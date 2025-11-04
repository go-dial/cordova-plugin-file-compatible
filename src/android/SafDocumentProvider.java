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

import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * SAF Document Provider for Cordova File Plugin
 * Provides secure access to files through Storage Access Framework
 * Compatible with Google Play policies by avoiding sensitive permissions
 */
public class SafDocumentProvider extends DocumentsProvider {
    private static final String LOG_TAG = "SafDocumentProvider";
    
    // Authority must match the one declared in AndroidManifest.xml
    public static final String AUTHORITY = "org.apache.cordova.file.saf.provider";
    
    // Document types
    private static final String TYPE_DIRECTORY = DocumentsContract.Document.MIME_TYPE_DIR;
    
    // Root IDs
    private static final String ROOT_ID_INTERNAL = "internal";
    private static final String ROOT_ID_EXTERNAL = "external";
    
    // URI matching
    private static final int MATCH_ROOTS = 1;
    private static final int MATCH_ROOT = 2;
    private static final int MATCH_DOCUMENT = 3;
    private static final int MATCH_CHILDREN = 4;
    private static final int MATCH_SEARCH = 5;
    
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    
    static {
        uriMatcher.addURI(AUTHORITY, "root", MATCH_ROOTS);
        uriMatcher.addURI(AUTHORITY, "root/*", MATCH_ROOT);
        uriMatcher.addURI(AUTHORITY, "document/*", MATCH_DOCUMENT);
        uriMatcher.addURI(AUTHORITY, "document/*/children", MATCH_CHILDREN);
        uriMatcher.addURI(AUTHORITY, "search", MATCH_SEARCH);
    }
    
    // Root projection
    private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
        DocumentsContract.Root.COLUMN_ROOT_ID,
        DocumentsContract.Root.COLUMN_MIME_TYPES,
        DocumentsContract.Root.COLUMN_FLAGS,
        DocumentsContract.Root.COLUMN_ICON,
        DocumentsContract.Root.COLUMN_TITLE,
        DocumentsContract.Root.COLUMN_SUMMARY,
        DocumentsContract.Root.COLUMN_DOCUMENT_ID,
        DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,
    };
    
    // Document projection
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        DocumentsContract.Document.COLUMN_FLAGS,
        DocumentsContract.Document.COLUMN_SIZE,
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
        
        // Add internal storage root
        final MatrixCursor.RowBuilder internalRow = result.newRow();
        internalRow.add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID_INTERNAL);
        internalRow.add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*");
        internalRow.add(DocumentsContract.Root.COLUMN_FLAGS, 
            DocumentsContract.Root.FLAG_LOCAL_ONLY | 
            DocumentsContract.Root.FLAG_SUPPORTS_CREATE);
        internalRow.add(DocumentsContract.Root.COLUMN_ICON, android.R.drawable.ic_menu_save);
        internalRow.add(DocumentsContract.Root.COLUMN_TITLE, "Internal Storage");
        internalRow.add(DocumentsContract.Root.COLUMN_SUMMARY, "Application internal storage");
        internalRow.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, getDocumentId(getInternalStorageDirectory()));
        internalRow.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, getInternalStorageDirectory().getFreeSpace());
        
        // Add external storage root if available
        File externalDir = getExternalStorageDirectory();
        if (externalDir != null && externalDir.exists()) {
            final MatrixCursor.RowBuilder externalRow = result.newRow();
            externalRow.add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID_EXTERNAL);
            externalRow.add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*");
            externalRow.add(DocumentsContract.Root.COLUMN_FLAGS, 
                DocumentsContract.Root.FLAG_LOCAL_ONLY | 
                DocumentsContract.Root.FLAG_SUPPORTS_CREATE);
            externalRow.add(DocumentsContract.Root.COLUMN_ICON, android.R.drawable.ic_menu_save);
            externalRow.add(DocumentsContract.Root.COLUMN_TITLE, "External Storage");
            externalRow.add(DocumentsContract.Root.COLUMN_SUMMARY, "Application external storage");
            externalRow.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, getDocumentId(externalDir));
            externalRow.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, externalDir.getFreeSpace());
        }
        
        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        includeFile(result, documentId, null);
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) 
            throws FileNotFoundException {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        final File parent = getFileForDocId(parentDocumentId);
        
        if (!parent.exists()) {
            throw new FileNotFoundException("Parent document not found: " + parentDocumentId);
        }
        
        if (!parent.isDirectory()) {
            throw new FileNotFoundException("Parent is not a directory: " + parentDocumentId);
        }
        
        File[] children = parent.listFiles();
        if (children != null) {
            for (File child : children) {
                includeFile(result, null, child);
            }
        }
        
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) 
            throws FileNotFoundException {
        final File file = getFileForDocId(documentId);
        
        if (!file.exists()) {
            throw new FileNotFoundException("Document not found: " + documentId);
        }
        
        final int accessMode = ParcelFileDescriptor.parseMode(mode);
        return ParcelFileDescriptor.open(file, accessMode);
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName) 
            throws FileNotFoundException {
        final File parent = getFileForDocId(parentDocumentId);
        
        if (!parent.exists() || !parent.isDirectory()) {
            throw new FileNotFoundException("Parent directory not found: " + parentDocumentId);
        }
        
        File newFile = new File(parent, displayName);
        
        try {
            if (TYPE_DIRECTORY.equals(mimeType)) {
                if (!newFile.mkdir()) {
                    throw new FileNotFoundException("Failed to create directory: " + displayName);
                }
            } else {
                if (!newFile.createNewFile()) {
                    throw new FileNotFoundException("Failed to create file: " + displayName);
                }
            }
        } catch (Exception e) {
            throw new FileNotFoundException("Failed to create document: " + e.getMessage());
        }
        
        return getDocumentId(newFile);
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        final File file = getFileForDocId(documentId);
        
        if (!file.exists()) {
            throw new FileNotFoundException("Document not found: " + documentId);
        }
        
        if (!file.delete()) {
            throw new FileNotFoundException("Failed to delete document: " + documentId);
        }
    }

    @Override
    public String renameDocument(String documentId, String displayName) throws FileNotFoundException {
        final File file = getFileForDocId(documentId);
        
        if (!file.exists()) {
            throw new FileNotFoundException("Document not found: " + documentId);
        }
        
        final File newFile = new File(file.getParentFile(), displayName);
        
        if (!file.renameTo(newFile)) {
            throw new FileNotFoundException("Failed to rename document: " + documentId);
        }
        
        return getDocumentId(newFile);
    }

    /**
     * Get the File object for a document ID
     */
    private File getFileForDocId(String documentId) throws FileNotFoundException {
        final File target = new File(documentId);
        if (!target.exists()) {
            throw new FileNotFoundException("Document not found: " + documentId);
        }
        return target;
    }
    
    /**
     * Get document ID from File
     */
    private String getDocumentId(File file) {
        return file.getAbsolutePath();
    }
    
    /**
     * Get internal storage directory
     */
    private File getInternalStorageDirectory() {
        return getContext().getFilesDir();
    }
    
    /**
     * Get external storage directory
     */
    private File getExternalStorageDirectory() {
        return getContext().getExternalFilesDir(null);
    }
    
    /**
     * Add a file to the cursor
     */
    private void includeFile(MatrixCursor result, String docId, File file) {
        if (docId == null) {
            docId = getDocumentId(file);
        } else {
            file = new File(docId);
        }
        
        if (!file.exists()) {
            return;
        }
        
        int flags = 0;
        
        if (file.canWrite()) {
            if (file.isDirectory()) {
                flags |= DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
            } else {
                flags |= DocumentsContract.Document.FLAG_SUPPORTS_WRITE;
            }
            flags |= DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
            flags |= DocumentsContract.Document.FLAG_SUPPORTS_RENAME;
        }
        
        final String mimeType = file.isDirectory() ? TYPE_DIRECTORY : getMimeType(file);
        
        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, docId);
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType);
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.getName());
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified());
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags);
        row.add(DocumentsContract.Document.COLUMN_SIZE, file.length());
    }
    
    /**
     * Get MIME type for file
     */
    private String getMimeType(File file) {
        final String name = file.getName();
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1);
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }
    
    /**
     * Resolve root projection
     */
    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }
    
    /**
     * Resolve document projection
     */
    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }
}