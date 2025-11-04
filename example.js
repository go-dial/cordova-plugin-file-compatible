/*
 * Example usage of cordova-plugin-file-compatible
 * Demonstrates SAF integration and backward compatibility
 */

// Example 1: Pick an image using SAF (recommended for external files)
function pickImageWithSAF() {
    if (SafManager.isSupported()) {
        SafManager.pickImages(false, function(uri) {
            console.log('Selected image URI:', uri);
            
            // Get file information
            SafManager.getFileInfo(uri, function(info) {
                console.log('File name:', info.name);
                console.log('File size:', info.size);
                console.log('MIME type:', info.mimeType);
                
                // Copy to app storage if needed
                var localPath = cordova.file.dataDirectory + 'selected_image.jpg';
                SafManager.copyFromSaf(uri, localPath, function(success) {
                    console.log('File copied to app storage');
                    displayImage(localPath);
                }, function(error) {
                    console.error('Copy failed:', error);
                });
            }, function(error) {
                console.error('Failed to get file info:', error);
            });
        }, function(error) {
            console.error('Image selection cancelled or failed:', error);
        });
    } else {
        alert('SAF not supported on this platform');
    }
}

// Example 2: Access media files using scoped storage
function getMediaImages() {
    SafManager.getMediaFiles('image', function(images) {
        console.log('Found', images.length, 'images');
        images.forEach(function(image, index) {
            console.log(index + ':', image.name, '(' + image.size + ' bytes)');
        });
    }, function(error) {
        console.error('Failed to get media images:', error);
    });
}

// Example 2a: Access audio files using scoped storage
function getAudioFiles() {
    SafManager.getMediaFiles('audio', function(audioFiles) {
        console.log('Found', audioFiles.length, 'audio files');
        audioFiles.forEach(function(audio, index) {
            console.log(index + ':', audio.name);
            console.log('  Size:', (audio.size / 1024 / 1024).toFixed(2) + ' MB');
            console.log('  Type:', audio.mimeType);
            console.log('  URI:', audio.uri);
        });
    }, function(error) {
        console.error('Failed to get audio files:', error);
    });
}

// Example 2b: Pick and upload audio file
function pickAndUploadAudio() {
    SafManager.pickAudio(false, function(audioUri) {
        console.log('Selected audio URI:', audioUri);
        
        // Get audio file information
        SafManager.getFileInfo(audioUri, function(info) {
            console.log('Audio file details:');
            console.log('  Name:', info.name);
            console.log('  Size:', (info.size / 1024 / 1024).toFixed(2) + ' MB');
            console.log('  MIME type:', info.mimeType);
            
            // Copy to app storage for processing/upload
            var localPath = cordova.file.dataDirectory + 'uploaded_audio.mp3';
            SafManager.copyFromSaf(audioUri, localPath, function(success) {
                if (success) {
                    console.log('Audio file copied to app storage');
                    // Now you can upload the file from local storage
                    uploadAudioFile(localPath, info);
                } else {
                    console.error('Failed to copy audio file');
                }
            }, function(error) {
                console.error('Copy operation failed:', error);
            });
        }, function(error) {
            console.error('Failed to get audio file info:', error);
        });
    }, function(error) {
        console.error('Audio selection cancelled or failed:', error);
    });
}

// Example 2c: Multiple audio file selection
function pickMultipleAudioFiles() {
    SafManager.pickAudio(true, function(audioUris) {
        console.log('Selected', audioUris.length, 'audio files');
        
        audioUris.forEach(function(uri, index) {
            SafManager.getFileInfo(uri, function(info) {
                console.log('Audio', index + 1 + ':', info.name, info.mimeType);
            });
        });
    }, function(error) {
        console.error('Multiple audio selection failed:', error);
    });
}

// Helper function to upload audio file
function uploadAudioFile(localPath, fileInfo) {
    console.log('Uploading audio file:', fileInfo.name);
    
    // Example upload using FileTransfer plugin or fetch API
    var uploadUrl = 'https://your-server.com/upload/audio';
    
    // Using fetch API (modern approach)
    window.resolveLocalFileSystemURL(localPath, function(fileEntry) {
        fileEntry.file(function(file) {
            var formData = new FormData();
            formData.append('audio', file, fileInfo.name);
            formData.append('mimeType', fileInfo.mimeType);
            formData.append('size', fileInfo.size);
            
            fetch(uploadUrl, {
                method: 'POST',
                body: formData,
                headers: {
                    'Authorization': 'Bearer your-token-here'
                }
            })
            .then(response => response.json())
            .then(data => {
                console.log('Audio upload successful:', data);
            })
            .catch(error => {
                console.error('Audio upload failed:', error);
            });
        });
    });
}

// Example 3: Create a new document
function createTextFile() {
    SafManager.createDocument('my-notes.txt', 'text/plain', function(uri) {
        console.log('Created document URI:', uri);
        
        // Write content to the created file
        var content = 'Hello from cordova-plugin-file-compatible!\nThis file was created using SAF.';
        // Note: Writing to SAF URIs requires additional implementation
        // For now, you can copy content from a local file
        
    }, function(error) {
        console.error('Document creation failed:', error);
    });
}

// Example 4: Traditional file operations (unchanged - backward compatible)
function useTraditionalFileAPI() {
    window.requestFileSystem(LocalFileSystem.PERSISTENT, 0, function(fs) {
        console.log('File system opened:', fs.name);
        
        fs.root.getFile('test.txt', {create: true, exclusive: false}, function(fileEntry) {
            console.log('File created:', fileEntry.fullPath);
            
            // Write to file
            fileEntry.createWriter(function(fileWriter) {
                fileWriter.onwrite = function() {
                    console.log('File write completed');
                    readFile(fileEntry);
                };
                
                var blob = new Blob(['Hello Traditional API!'], {type: 'text/plain'});
                fileWriter.write(blob);
            }, function(error) {
                console.error('Failed to create writer:', error);
            });
        }, function(error) {
            console.error('Failed to get file:', error);
        });
    }, function(error) {
        console.error('Failed to request file system:', error);
    });
}

function readFile(fileEntry) {
    fileEntry.file(function(file) {
        var reader = new FileReader();
        reader.onload = function() {
            console.log('File content:', this.result);
        };
        reader.readAsText(file);
    }, function(error) {
        console.error('Failed to read file:', error);
    });
}

// Example 5: Robust file access with fallback
function robustFileAccess() {
    if (SafManager.isSupported() && device.platform === 'Android') {
        // Use SAF for external files on Android
        SafManager.openDocumentPicker(['image/*', 'video/*'], false, function(uri) {
            console.log('SAF selected:', uri);
            handleSelectedFile(uri);
        }, function(error) {
            console.log('SAF selection cancelled, falling back to app storage');
            useAppStorage();
        });
    } else {
        // Use traditional methods for iOS/other platforms or as fallback
        useAppStorage();
    }
}

function useAppStorage() {
    window.resolveLocalFileSystemURL(cordova.file.documentsDirectory, function(dirEntry) {
        console.log('Using app documents directory:', dirEntry.toURL());
    }, function(error) {
        console.error('Failed to access app storage:', error);
    });
}

function handleSelectedFile(uri) {
    SafManager.getFileInfo(uri, function(info) {
        console.log('Selected file:', info.name, info.mimeType);
        
        if (info.mimeType.startsWith('image/')) {
            displayImageFromSAF(uri);
        } else if (info.mimeType.startsWith('video/')) {
            playVideoFromSAF(uri);
        }
    }, function(error) {
        console.error('Failed to get file info:', error);
    });
}

function displayImageFromSAF(uri) {
    // Note: Direct display of SAF URIs in HTML may not work
    // Copy to local storage first for display
    var localPath = cordova.file.cacheDirectory + 'temp_image.jpg';
    SafManager.copyFromSaf(uri, localPath, function(success) {
        if (success) {
            displayImage('file://' + localPath);
        }
    }, function(error) {
        console.error('Failed to copy image:', error);
    });
}

function displayImage(src) {
    var img = document.createElement('img');
    img.src = src;
    img.style.maxWidth = '100%';
    document.body.appendChild(img);
}

// Initialize on device ready
document.addEventListener('deviceready', function() {
    console.log('cordova-plugin-file-compatible ready');
    console.log('SAF supported:', SafManager.isSupported());
    console.log('Platform:', device.platform);
    
    // Example usage
    // pickImageWithSAF();
    // getMediaImages();
    // useTraditionalFileAPI();
}, false);