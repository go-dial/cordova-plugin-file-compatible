/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Storage Access Framework (SAF) APIs for Cordova File Plugin
 * Provides modern, privacy-compliant file access methods
 */
var exec = require('cordova/exec');

var SafManager = {
    /**
     * Open document picker for file selection
     * Uses Storage Access Framework to allow users to pick files
     * 
     * @param {String} mimeType - MIME type filter (default: "*/*")
     * @param {Boolean} allowMultiple - Allow multiple file selection (default: false)
     * @param {Function} successCallback - Success callback with selected file URI(s)
     * @param {Function} errorCallback - Error callback
     */
    openDocumentPicker: function(mimeType, allowMultiple, successCallback, errorCallback) {
        // Handle parameter variations for backward compatibility
        if (typeof mimeType === 'function') {
            errorCallback = allowMultiple;
            successCallback = mimeType;
            allowMultiple = false;
            mimeType = "*/*";
        } else if (typeof allowMultiple === 'function') {
            errorCallback = successCallback;
            successCallback = allowMultiple;
            allowMultiple = false;
        }
        
        // Set defaults
        mimeType = mimeType || "*/*";
        allowMultiple = allowMultiple || false;
        
        exec(successCallback, errorCallback, "File", "openDocumentPicker", [mimeType, allowMultiple]);
    },

    /**
     * Create new document using Storage Access Framework
     * Uses Storage Access Framework to allow users to create files
     * 
     * @param {String} fileName - Name of the file to create
     * @param {String} mimeType - MIME type of the file (default: "*/*")
     * @param {Function} successCallback - Success callback with created file URI
     * @param {Function} errorCallback - Error callback
     */
    createDocument: function(fileName, mimeType, successCallback, errorCallback) {
        // Handle parameter variations for backward compatibility
        if (typeof mimeType === 'function') {
            errorCallback = successCallback;
            successCallback = mimeType;
            mimeType = "*/*";
        }
        
        // Set defaults
        mimeType = mimeType || "*/*";
        
        exec(successCallback, errorCallback, "File", "createDocument", [fileName, mimeType]);
    },

    /**
     * Get media files from MediaStore (images, videos, audio)
     * Uses scoped storage APIs to access media without permissions
     * 
     * @param {String} mediaType - Type of media: "image", "video", or "audio"
     * @param {Number} limit - Maximum number of files to return (default: 50)
     * @param {Function} successCallback - Success callback with file information array
     * @param {Function} errorCallback - Error callback
     */
    getMediaFiles: function(mediaType, limit, successCallback, errorCallback) {
        // Handle parameter variations
        if (typeof limit === 'function') {
            errorCallback = successCallback;
            successCallback = limit;
            limit = 50;
        }
        
        // Validate mediaType
        var validTypes = ['image', 'video', 'audio'];
        if (!validTypes.includes(mediaType)) {
            if (errorCallback) {
                errorCallback('Invalid media type. Must be: image, video, or audio');
            }
            return;
        }
        
        // Set defaults
        limit = limit || 50;
        
        exec(successCallback, errorCallback, "File", "getMediaFiles", [mediaType, limit]);
    },

    /**
     * Get file information from SAF URI
     * Retrieves metadata about a file using its SAF content URI
     * 
     * @param {String} safUri - SAF content URI (content://)
     * @param {Function} successCallback - Success callback with file info object
     * @param {Function} errorCallback - Error callback
     */
    getFileInfo: function(safUri, successCallback, errorCallback) {
        if (!safUri || !safUri.startsWith('content://')) {
            if (errorCallback) {
                errorCallback('Invalid SAF URI. Must start with content://');
            }
            return;
        }
        
        exec(successCallback, errorCallback, "File", "getFileInfo", [safUri]);
    },

    /**
     * Copy file from SAF URI to app's private storage
     * Useful for working with files selected via SAF in app's sandbox
     * 
     * @param {String} safUri - Source SAF content URI
     * @param {String} targetPath - Target file path in app storage
     * @param {Function} successCallback - Success callback with boolean result
     * @param {Function} errorCallback - Error callback
     */
    copyFromSaf: function(safUri, targetPath, successCallback, errorCallback) {
        if (!safUri || !safUri.startsWith('content://')) {
            if (errorCallback) {
                errorCallback('Invalid SAF URI. Must start with content://');
            }
            return;
        }
        
        if (!targetPath) {
            if (errorCallback) {
                errorCallback('Target path is required');
            }
            return;
        }
        
        exec(successCallback, errorCallback, "File", "copyFromSaf", [safUri, targetPath]);
    },

    /**
     * Copy file from app storage to SAF URI
     * Useful for saving app-generated files to user-selected locations
     * 
     * @param {String} sourcePath - Source file path in app storage
     * @param {String} safUri - Target SAF content URI
     * @param {Function} successCallback - Success callback with boolean result
     * @param {Function} errorCallback - Error callback
     */
    copyToSaf: function(sourcePath, safUri, successCallback, errorCallback) {
        if (!sourcePath) {
            if (errorCallback) {
                errorCallback('Source path is required');
            }
            return;
        }
        
        if (!safUri || !safUri.startsWith('content://')) {
            if (errorCallback) {
                errorCallback('Invalid SAF URI. Must start with content://');
            }
            return;
        }
        
        exec(successCallback, errorCallback, "File", "copyToSaf", [sourcePath, safUri]);
    },

    /**
     * Delete file via SAF URI
     * Removes a file using Storage Access Framework
     * 
     * @param {String} safUri - SAF content URI of file to delete
     * @param {Function} successCallback - Success callback with boolean result
     * @param {Function} errorCallback - Error callback
     */
    deleteDocument: function(safUri, successCallback, errorCallback) {
        if (!safUri || !safUri.startsWith('content://')) {
            if (errorCallback) {
                errorCallback('Invalid SAF URI. Must start with content://');
            }
            return;
        }
        
        exec(successCallback, errorCallback, "File", "deleteDocument", [safUri]);
    },

    /**
     * Check if a SAF URI is valid and accessible
     * Tests if the app still has permission to access the URI
     * 
     * @param {String} safUri - SAF content URI to test
     * @param {Function} successCallback - Success callback with boolean result
     * @param {Function} errorCallback - Error callback
     */
    isUriAccessible: function(safUri, successCallback, errorCallback) {
        if (!safUri || !safUri.startsWith('content://')) {
            if (successCallback) {
                successCallback(false);
            }
            return;
        }
        
        exec(successCallback, errorCallback, "File", "isUriAccessible", [safUri]);
    },

    /**
     * Migrate traditional file path to SAF-compatible approach
     * Helper method for apps transitioning from file:// to content:// URIs
     * Note: This doesn't automatically migrate files, but provides guidance
     * 
     * @param {String} filePath - Traditional file path
     * @param {Function} successCallback - Success callback with migration info
     * @param {Function} errorCallback - Error callback
     */
    migrateToSaf: function(filePath, successCallback, errorCallback) {
        // For now, return null as migration requires user interaction
        // Apps should guide users to re-select files using SAF
        if (successCallback) {
            successCallback(null);
        }
    }
};

module.exports = SafManager;