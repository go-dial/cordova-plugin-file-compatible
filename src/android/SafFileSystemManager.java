/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */
package org.apache.cordova.file;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * SAF File System Manager for Storage Access Framework integration
 * Provides secure, permission-less access to media files and documents
 * Complies with Google Play policies by using scoped storage
 */
public class SafFileSystemManager {
    private static final String LOG_TAG = "SafFileSystemManager";
    
    // Request codes for SAF activities
    public static final int REQUEST_CODE_OPEN_DOCUMENT = 100;
    public static final int REQUEST_CODE_OPEN_DOCUMENT_TREE = 101;
    public static final int REQUEST_CODE_CREATE_DOCUMENT = 102;
    
    private Context context;
    private CordovaInterface cordova;
    private CallbackContext currentCallback;
    
    public SafFileSystemManager(Context context, CordovaInterface cordova) {
        this.context = context;
        this.cordova = cordova;
    }
    
    /**
     * Open document picker for file selection
     * Uses SAF to allow users to pick files without requiring storage permissions
     */
    public void openDocumentPicker(String[] mimeTypes, boolean allowMultiple, CallbackContext callbackContext) {
        this.currentCallback = callbackContext;
        
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        if (mimeTypes != null && mimeTypes.length > 0) {
            if (mimeTypes.length == 1) {
                intent.setType(mimeTypes[0]);
            } else {
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }
        } else {
            intent.setType("*/*");
        }
        
        if (allowMultiple) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        
        cordova.startActivityForResult(null, intent, REQUEST_CODE_OPEN_DOCUMENT);
    }
    
    /**
     * Open directory picker for directory access
     * Uses SAF to allow users to select directories for persistent access
     */
    public void openDirectoryPicker(CallbackContext callbackContext) {
        this.currentCallback = callbackContext;
        
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | 
                       Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                       Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        
        cordova.startActivityForResult(null, intent, REQUEST_CODE_OPEN_DOCUMENT_TREE);
    }
    
    /**
     * Create a new document
     * Uses SAF to allow users to create files in their chosen location
     */
    public void createDocument(String fileName, String mimeType, CallbackContext callbackContext) {
        this.currentCallback = callbackContext;
        
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType != null ? mimeType : "*/*");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        
        cordova.startActivityForResult(null, intent, REQUEST_CODE_CREATE_DOCUMENT);
    }
    
    /**
     * Handle activity result from SAF operations
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (currentCallback == null) {
            return;
        }
        
        if (resultCode != Activity.RESULT_OK) {
            currentCallback.error("User cancelled or operation failed");
            currentCallback = null;
            return;
        }
        
        try {
            switch (requestCode) {
                case REQUEST_CODE_OPEN_DOCUMENT:
                    handleOpenDocumentResult(data);
                    break;
                case REQUEST_CODE_OPEN_DOCUMENT_TREE:
                    handleOpenDocumentTreeResult(data);
                    break;
                case REQUEST_CODE_CREATE_DOCUMENT:
                    handleCreateDocumentResult(data);
                    break;
                default:
                    currentCallback.error("Unknown request code: " + requestCode);
                    break;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error handling SAF result", e);
            currentCallback.error("Error handling result: " + e.getMessage());
        } finally {
            currentCallback = null;
        }
    }
    
    /**
     * Handle result from document picker
     */
    private void handleOpenDocumentResult(Intent data) {
        if (data.getClipData() != null) {
            // Multiple files selected
            int count = data.getClipData().getItemCount();
            List<String> uris = new ArrayList<>();
            
            for (int i = 0; i < count; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                takePersistableUriPermission(uri);
                uris.add(uri.toString());
            }
            
            JSONArray jsonArray = new JSONArray(uris);
            currentCallback.success(jsonArray);
        } else if (data.getData() != null) {
            // Single file selected
            Uri uri = data.getData();
            takePersistableUriPermission(uri);
            currentCallback.success(uri.toString());
        } else {
            currentCallback.error("No document selected");
        }
    }
    
    /**
     * Handle result from directory picker
     */
    private void handleOpenDocumentTreeResult(Intent data) {
        Uri uri = data.getData();
        if (uri != null) {
            takePersistableUriPermission(uri);
            currentCallback.success(uri.toString());
        } else {
            currentCallback.error("No directory selected");
        }
    }
    
    /**
     * Handle result from document creation
     */
    private void handleCreateDocumentResult(Intent data) {
        Uri uri = data.getData();
        if (uri != null) {
            takePersistableUriPermission(uri);
            currentCallback.success(uri.toString());
        } else {
            currentCallback.error("Failed to create document");
        }
    }
    
    /**
     * Take persistable URI permission for long-term access
     */
    private void takePersistableUriPermission(Uri uri) {
        try {
            context.getContentResolver().takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException e) {
            Log.w(LOG_TAG, "Could not take persistable permission for " + uri, e);
        }
    }
    
    /**
     * Get file information from SAF URI
     */
    public FileInfo getFileInfo(Uri uri) {
        FileInfo info = new FileInfo();
        
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                
                if (nameIndex != -1) {
                    info.name = cursor.getString(nameIndex);
                }
                
                if (sizeIndex != -1) {
                    info.size = cursor.getLong(sizeIndex);
                }
                
                info.mimeType = context.getContentResolver().getType(uri);
                info.uri = uri;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error getting file info for " + uri, e);
        }
        
        return info;
    }
    
    /**
     * Copy file from SAF URI to local storage
     */
    public boolean copyFromSaf(Uri sourceUri, File destinationFile) {
        try (InputStream input = context.getContentResolver().openInputStream(sourceUri);
             OutputStream output = new FileOutputStream(destinationFile)) {
            
            if (input == null) {
                return false;
            }
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            
            return true;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error copying file from SAF", e);
            return false;
        }
    }
    
    /**
     * Copy file from local storage to SAF URI
     */
    public boolean copyToSaf(File sourceFile, Uri destinationUri) {
        try (InputStream input = new FileInputStream(sourceFile);
             OutputStream output = context.getContentResolver().openOutputStream(destinationUri)) {
            
            if (output == null) {
                return false;
            }
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            
            return true;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error copying file to SAF", e);
            return false;
        }
    }
    
    /**
     * Get all persistent URI permissions
     */
    public List<UriPermission> getPersistentUriPermissions() {
        return context.getContentResolver().getPersistedUriPermissions();
    }
    
    /**
     * Release persistent URI permission
     */
    public void releasePersistentUriPermission(Uri uri) {
        try {
            context.getContentResolver().releasePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException e) {
            Log.w(LOG_TAG, "Could not release persistable permission for " + uri, e);
        }
    }
    
    /**
     * Check if app has access to specific media directories through scoped storage
     */
    public boolean hasMediaAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10+, apps have scoped access to media files by default
            // No special permissions needed for reading media files created by the app
            return true;
        }
        return false;
    }
    
    /**
     * Get media files using MediaStore (scoped storage)
     */
    public List<FileInfo> getMediaFiles(String mediaType) {
        List<FileInfo> mediaFiles = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return mediaFiles; // Fallback to traditional file access
        }
        
        Uri collection;
        String[] projection = {
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        };
        
        switch (mediaType.toLowerCase()) {
            case "image":
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                break;
            case "video":
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                break;
            case "audio":
                collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                break;
            default:
                return mediaFiles;
        }
        
        try (Cursor cursor = context.getContentResolver().query(
                collection, projection, null, null, null)) {
            
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
                int mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
                
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    long size = cursor.getLong(sizeColumn);
                    String mimeType = cursor.getString(mimeColumn);
                    
                    Uri contentUri = Uri.withAppendedPath(collection, String.valueOf(id));
                    
                    FileInfo info = new FileInfo();
                    info.name = name;
                    info.size = size;
                    info.mimeType = mimeType;
                    info.uri = contentUri;
                    
                    mediaFiles.add(info);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error querying media files", e);
        }
        
        return mediaFiles;
    }
    
    /**
     * File information structure
     */
    public static class FileInfo {
        public String name;
        public long size;
        public String mimeType;
        public Uri uri;
        
        public FileInfo() {}
        
        public FileInfo(String name, long size, String mimeType, Uri uri) {
            this.name = name;
            this.size = size;
            this.mimeType = mimeType;
            this.uri = uri;
        }
    }
}