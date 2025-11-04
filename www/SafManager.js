var exec = require('cordova/exec');

var SafManager = {
    openDocumentPicker: function(mimeType, allowMultiple, successCallback, errorCallback) {
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
        
        mimeType = mimeType || "*/*";
        allowMultiple = allowMultiple || false;
        
        exec(successCallback, errorCallback, "File", "openDocumentPicker", [mimeType, allowMultiple]);
    },

    getFileInfo: function(safUri, successCallback, errorCallback) {
        if (!safUri || !safUri.startsWith('content://')) {
            if (errorCallback) {
                errorCallback('Invalid SAF URI');
            }
            return;
        }
        
        exec(successCallback, errorCallback, "File", "getFileInfo", [safUri]);
    },

    copyFromSaf: function(safUri, targetPath, successCallback, errorCallback) {
        if (!safUri || !safUri.startsWith('content://')) {
            if (errorCallback) {
                errorCallback('Invalid SAF URI');
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
    }
};

module.exports = SafManager;