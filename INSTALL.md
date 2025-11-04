# Installation and Usage Guide

## Quick Start

### 1. Install the Plugin

```bash
# Remove old file plugins first (if installed)
cordova plugin remove cordova-plugin-file
cordova plugin remove cordova-plugin-filepath

# Install the compatible version
cordova plugin add cordova-plugin-file-compatible
```

### 2. Update Permissions (Remove Old Ones)

Remove these permissions from your `config.xml` as they are no longer needed:

```xml
<!-- Remove these deprecated permissions -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### 3. Start Using SAF APIs

```javascript
document.addEventListener('deviceready', function() {
    // Modern approach - no permissions required
    SafManager.pickImages(false, function(uri) {
        console.log('Selected image:', uri);
    }, function(error) {
        console.log('Selection cancelled');
    });
}, false);
```

## Migration Examples

### Before (Deprecated)
```javascript
// This may fail on Android 13+ due to permission restrictions
function oldWay() {
    navigator.camera.getPicture(function(imageURI) {
        // Limited access, requires permissions
        window.resolveLocalFileSystemURL(imageURI, function(fileEntry) {
            // May not work due to scoped storage
        });
    }, function(error) {
        console.error('Camera error:', error);
    }, {
        quality: 50,
        sourceType: Camera.PictureSourceType.PHOTOLIBRARY
    });
}
```

### After (SAF)
```javascript
// This always works and is Google Play compliant
function newWay() {
    SafManager.pickImages(false, function(uri) {
        console.log('User selected:', uri);
        
        // Get file info
        SafManager.getFileInfo(uri, function(info) {
            console.log('File:', info.name, info.size, info.mimeType);
        });
        
        // Copy to app storage if needed
        SafManager.copyFromSaf(uri, cordova.file.dataDirectory + 'image.jpg', 
            function(success) {
                console.log('File copied successfully');
            }
        );
    }, function(error) {
        console.log('User cancelled selection');
    });
}
```

## Platform Support

| Platform | Status | Notes |
|----------|--------|-------|
| Android 13+ | ✅ Full SAF support | Recommended approach |
| Android 10-12 | ✅ SAF + Legacy fallback | Both methods work |
| Android <10 | ✅ Legacy APIs | Traditional file access |
| iOS | ✅ Traditional APIs | No changes needed |
| Browser | ✅ Traditional APIs | No changes needed |

## Advanced Usage

### Directory Access
```javascript
SafManager.openDirectoryPicker(function(dirUri) {
    console.log('Directory selected:', dirUri);
    // User has granted persistent access to this directory
});
```

### File Creation
```javascript
SafManager.createDocument('report.pdf', 'application/pdf', function(uri) {
    console.log('Created file location:', uri);
    // Copy your generated content to this location
});
```

### Media Files
```javascript
SafManager.getMediaFiles('image', function(images) {
    images.forEach(function(img) {
        console.log('Found:', img.name, img.uri);
    });
});

// Audio files access
SafManager.getMediaFiles('audio', function(audioFiles) {
    audioFiles.forEach(function(audio) {
        console.log('Audio:', audio.name, audio.mimeType, audio.size);
    });
});

// Video files access  
SafManager.getMediaFiles('video', function(videos) {
    videos.forEach(function(video) {
        console.log('Video:', video.name, video.mimeType, video.size);
    });
});
```

### Audio File Upload Workflow
```javascript
function uploadAudioFile() {
    // Step 1: Let user pick audio file
    SafManager.pickAudio(false, function(audioUri) {
        console.log('User selected audio:', audioUri);
        
        // Step 2: Get file information
        SafManager.getFileInfo(audioUri, function(info) {
            console.log('Audio details:', info.name, info.size, info.mimeType);
            
            // Step 3: Copy to app storage for upload
            var localPath = cordova.file.dataDirectory + 'temp_audio.mp3';
            SafManager.copyFromSaf(audioUri, localPath, function(success) {
                if (success) {
                    // Step 4: Upload the file
                    uploadToServer(localPath, info);
                }
            });
        });
    }, function(error) {
        console.log('User cancelled audio selection');
    });
}

function uploadToServer(localPath, fileInfo) {
    var uploadUrl = 'https://your-server.com/upload';
    
    // Using fetch API
    window.resolveLocalFileSystemURL(localPath, function(fileEntry) {
        fileEntry.file(function(file) {
            var formData = new FormData();
            formData.append('audio', file, fileInfo.name);
            
            fetch(uploadUrl, {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => console.log('Upload success:', data))
            .catch(error => console.error('Upload failed:', error));
        });
    });
}
```

## Best Practices

1. **Always handle user cancellation** - SAF operations can be cancelled
2. **Store SAF URIs** - They provide persistent access
3. **Use traditional APIs for internal files** - They remain unchanged
4. **Educate users** - Explain why file selection is needed
5. **Test on different Android versions** - Ensure compatibility

## Troubleshooting

### "Permission denied" errors
- Use SAF APIs instead of direct file access
- Remove deprecated permissions from config.xml

### "File not found" errors  
- Ensure you're using the correct URIs from SAF
- Check if the user has revoked access

### Google Play rejection
- This plugin is specifically designed to avoid rejections
- No sensitive permissions are requested

## Support

For issues, feature requests, or questions:
- GitHub Issues: https://github.com/go-dial/cordova-plugin-file-compatible/issues
- Migration Help: See README.md for detailed migration guide