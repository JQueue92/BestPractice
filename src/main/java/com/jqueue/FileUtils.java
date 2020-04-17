package com.hqyatu.destination.utils;

/*
 * Copyright (C) 2018 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Comparator;

public class FileUtils {
    public static final String DOCUMENTS_DIR = "documents";
    // configured android:authorities in AndroidManifest (https://developer.android.com/reference/android/support/v4/content/FileProvider)
    public static final String AUTHORITY = "YOUR_AUTHORITY.provider";
    public static final String HIDDEN_PREFIX = ".";
    /**
     * TAG for log messages.
     */
    static final String TAG = "FileUtils";
    private static final boolean DEBUG = false; // Set to true to enable logging
    /**
     * File and folder comparator. TODO Expose sorting option method
     */
    public static Comparator<File> sComparator = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            // Sort alphabetically by lower case, which is much cleaner
            return f1.getName().toLowerCase().compareTo(
                    f2.getName().toLowerCase());
        }
    };
    /**
     * File (not directories) filter.
     */
    public static FileFilter sFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            final String fileName = file.getName();
            // Return files only (not directories) and skip hidden files
            return file.isFile() && !fileName.startsWith(HIDDEN_PREFIX);
        }
    };
    /**
     * Folder (directories) filter.
     */
    public static FileFilter sDirFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            final String fileName = file.getName();
            // Return directories only and skip hidden directories
            return file.isDirectory() && !fileName.startsWith(HIDDEN_PREFIX);
        }
    };

    private FileUtils() {
    } //private constructor to enforce Singleton pattern

    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     *
     * @param uri
     * @return Extension including the dot("."); "" if there is no extension;
     * null if uri was null.
     */
    public static String getExtension(String uri) {
        if (uri == null) {
            return null;
        }

        int dot = uri.lastIndexOf(".");
        if (dot >= 0) {
            return uri.substring(dot);
        } else {
            // No extension.
            return "";
        }
    }

    /**
     * @return Whether the URI is a local one.
     */
    public static boolean isLocal(String url) {
        return url != null && !url.startsWith("http://") && !url.startsWith("https://");
    }

    /**
     * @return True if Uri is a MediaStore Uri.
     * @author paulburke
     */
    public static boolean isMediaUri(Uri uri) {
        return "media".equalsIgnoreCase(uri.getAuthority());
    }

    /**
     * Convert File into Uri.
     *
     * @param file
     * @return uri
     */
    public static Uri getUri(File file) {
        return (file != null) ? Uri.fromFile(file) : null;
    }

    /**
     * Returns the path only (without file name).
     *
     * @param file
     * @return
     */
    public static File getPathWithoutFilename(File file) {
        if (file != null) {
            if (file.isDirectory()) {
                // no file to be split off. Return everything
                return file;
            } else {
                String filename = file.getName();
                String filepath = file.getAbsolutePath();

                // Construct path without file name.
                String pathwithoutname = filepath.substring(0,
                        filepath.length() - filename.length());
                if (pathwithoutname.endsWith("/")) {
                    pathwithoutname = pathwithoutname.substring(0, pathwithoutname.length() - 1);
                }
                return new File(pathwithoutname);
            }
        }
        return null;
    }

    /**
     * @return The MIME type for the given file.
     */
    public static String getMimeType(File file) {

        String extension = getExtension(file.getName());

        if (extension.length() > 0)
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.substring(1));

        return "application/octet-stream";
    }

    /**
     * @return The MIME type for the give Uri.
     */
    public static String getMimeType(Context context, Uri uri) {
        File file = new File(getPath(context, uri));
        return getMimeType(file);
    }

    /**
     * @return The MIME type for the give String Uri.
     */
    public static String getMimeType(Context context, String url) {
        String type = context.getContentResolver().getType(Uri.parse(url));
        if (type == null) {
            type = "application/octet-stream";
        }
        return type;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is local.
     */
    public static boolean isLocalStorageDocument(Uri uri) {
        return AUTHORITY.equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = MediaStore.Files.FileColumns.DATA;
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                if (DEBUG)
                    DatabaseUtils.dumpCursor(cursor);

                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br>
     * <br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @see #isLocal(String)
     * @see #getFile(Context, Uri)
     */
    public static String getPath(final Context context, final Uri uri) {
        String absolutePath = getLocalPath(context, uri);
        return absolutePath != null ? absolutePath : uri.toString();
    }

    private static String getLocalPath(final Context context, final Uri uri) {

        if (DEBUG)
            Log.d(TAG + " File -",
                    "Authority: " + uri.getAuthority() +
                            ", Fragment: " + uri.getFragment() +
                            ", Port: " + uri.getPort() +
                            ", Query: " + uri.getQuery() +
                            ", Scheme: " + uri.getScheme() +
                            ", Host: " + uri.getHost() +
                            ", Segments: " + uri.getPathSegments().toString()
            );

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // LocalStorageProvider
            if (isLocalStorageDocument(uri)) {
                // The path is the id
                return DocumentsContract.getDocumentId(uri);
            }
            // ExternalStorageProvider
            else if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                } else if ("home".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/documents/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);

                if (id != null && id.startsWith("raw:")) {
                    return id.substring(4);
                }
                // Android Q 似乎无效
                String[] contentUriPrefixesToTry = new String[]{
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads",
                        "content://downloads/all_downloads"
                };

                for (String contentUriPrefix : contentUriPrefixesToTry) {
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));
                    try {
                        String path = getDataColumn(context, contentUri, null, null);
                        if (path != null) {
                            return path;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // path could not be retrieved using ContentResolver, therefore copy file to accessible cache using streams
                String fileName = getFileName(context, uri);
                File cacheDir = getDocumentCacheDir(context);
                File file = generateFileName(fileName, cacheDir);
                String destinationPath = null;
                if (file != null) {
                    destinationPath = file.getAbsolutePath();
                    saveFileFromUri(context, uri, destinationPath);
                }

                return destinationPath;
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Convert Uri into File, if possible.
     *
     * @return file A local file that the Uri was pointing to, or null if the
     * Uri is unsupported or pointed to a remote resource.
     * @author paulburke
     * @see #getPath(Context, Uri)
     */
    public static File getFile(Context context, Uri uri) {
        if (uri != null) {
            String path = getPath(context, uri);
            if (path != null && isLocal(path)) {
                return new File(path);
            }
        }
        return null;
    }

    /**
     * Get the file size in a human-readable string.
     *
     * @param size
     * @return
     * @author paulburke
     */
    public static String getReadableFileSize(int size) {
        final int BYTES_IN_KILOBYTES = 1024;
        final DecimalFormat dec = new DecimalFormat("###.#");
        final String KILOBYTES = " KB";
        final String MEGABYTES = " MB";
        final String GIGABYTES = " GB";
        float fileSize = 0;
        String suffix = KILOBYTES;

        if (size > BYTES_IN_KILOBYTES) {
            fileSize = size / BYTES_IN_KILOBYTES;
            if (fileSize > BYTES_IN_KILOBYTES) {
                fileSize = fileSize / BYTES_IN_KILOBYTES;
                if (fileSize > BYTES_IN_KILOBYTES) {
                    fileSize = fileSize / BYTES_IN_KILOBYTES;
                    suffix = GIGABYTES;
                } else {
                    suffix = MEGABYTES;
                }
            }
        }
        return String.valueOf(dec.format(fileSize) + suffix);
    }

    /**
     * Get the Intent for selecting content to be used in an Intent Chooser.
     *
     * @return The intent for opening a file with Intent.createChooser()
     */
    public static Intent createGetContentIntent() {
        // Implicitly allow the user to select a particular kind of data
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // The MIME data type filter
        intent.setType("*/*");
        // Only return URIs that can be opened with ContentResolver
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        return intent;
    }


    /**
     * Creates View intent for given file
     *
     * @param file
     * @return The intent for viewing file
     */
    public static Intent getViewIntent(Context context, File file) {
        //Uri uri = Uri.fromFile(file);
        Uri uri = FileProvider.getUriForFile(context, AUTHORITY, file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String url = file.toString();
        if (url.contains(".doc") || url.contains(".docx")) {
            // Word document
            intent.setDataAndType(uri, "application/msword");
        } else if (url.contains(".pdf")) {
            // PDF file
            intent.setDataAndType(uri, "application/pdf");
        } else if (url.contains(".ppt") || url.contains(".pptx")) {
            // Powerpoint file
            intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
        } else if (url.contains(".xls") || url.contains(".xlsx")) {
            // Excel file
            intent.setDataAndType(uri, "application/vnd.ms-excel");
        } else if (url.contains(".zip") || url.contains(".rar")) {
            // WAV audio file
            intent.setDataAndType(uri, "application/x-wav");
        } else if (url.contains(".rtf")) {
            // RTF file
            intent.setDataAndType(uri, "application/rtf");
        } else if (url.contains(".wav") || url.contains(".mp3")) {
            // WAV audio file
            intent.setDataAndType(uri, "audio/x-wav");
        } else if (url.contains(".gif")) {
            // GIF file
            intent.setDataAndType(uri, "image/gif");
        } else if (url.contains(".jpg") || url.contains(".jpeg") || url.contains(".png")) {
            // JPG file
            intent.setDataAndType(uri, "image/jpeg");
        } else if (url.contains(".txt")) {
            // Text file
            intent.setDataAndType(uri, "text/plain");
        } else if (url.contains(".3gp") || url.contains(".mpg") || url.contains(".mpeg") ||
                url.contains(".mpe") || url.contains(".mp4") || url.contains(".avi")) {
            // Video files
            intent.setDataAndType(uri, "video/*");
        } else {
            intent.setDataAndType(uri, "*/*");
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return intent;
    }

    public static File getDownloadsDir() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    public static File getDocumentCacheDir(@NonNull Context context) {
        File dir = new File(context.getCacheDir(), DOCUMENTS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        logDir(context.getCacheDir());
        logDir(dir);

        return dir;
    }

    private static void logDir(File dir) {
        if (!DEBUG) return;
        Log.d(TAG, "Dir=" + dir);
        File[] files = dir.listFiles();
        for (File file : files) {
            Log.d(TAG, "File=" + file.getPath());
        }
    }

    private static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static final void copy(InputStream inputStream, OutputStream outputStream) {
        byte[] b = new byte[1024];
        try {
            while (inputStream.read(b) != -1) {
                outputStream.write(b, 0, b.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(inputStream);
            close(outputStream);
        }

    }


    public static File generateFileName(@Nullable String name, File directory) {
        if (name == null) {
            return null;
        }

        File file = new File(directory, name);

        if (file.exists()) {
            String fileName = name;
            String extension = "";
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                fileName = name.substring(0, dotIndex);
                extension = name.substring(dotIndex);
            }

            int index = 0;

            while (file.exists()) {
                index++;
                name = fileName + '(' + index + ')' + extension;
                file = new File(directory, name);
            }
        }

        try {
            if (!file.createNewFile()) {
                return null;
            }
        } catch (IOException e) {
            Log.w(TAG, e);
            return null;
        }

        logDir(directory);

        return file;
    }

    private static void saveFileFromUri(Context context, Uri uri, String destinationPath) {
        InputStream is = null;
        BufferedOutputStream bos = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            bos = new BufferedOutputStream(new FileOutputStream(destinationPath, false));
            byte[] buf = new byte[1024];
            is.read(buf);
            do {
                bos.write(buf);
            } while (is.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static byte[] readBytesFromFile(String filePath) {

        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;

        try {

            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];

            //read file into bytes[]
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return bytesArray;

    }

    public static File createTempImageFile(Context context, String fileName) throws IOException {
        // Create an image file name
        File storageDir = new File(context.getCacheDir(), DOCUMENTS_DIR);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    public static String getFileName(@NonNull Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        String filename = null;

        if (mimeType == null && context != null) {
            String path = getPath(context, uri);
            if (path == null) {
                filename = getName(uri.toString());
            } else {
                File file = new File(path);
                filename = file.getName();
            }
        } else {
            Cursor returnCursor = context.getContentResolver().query(uri, null,
                    null, null, null);
            if (returnCursor != null) {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                filename = returnCursor.getString(nameIndex);
                returnCursor.close();
            }
        }

        return filename;
    }

    public static String getName(String filename) {
        if (filename == null) {
            return null;
        }
        int index = filename.lastIndexOf('/');
        return filename.substring(index + 1);
    }

    public static String MimeType(String fileName) {
        String retval = "";
        if (fileName.contains(".")) {
            switch (fileName.substring(fileName.lastIndexOf('.'))) {
                case ".3dm":
                    retval = "x-world/x-3dmf";
                    break;
                case ".3dmf":
                    retval = "x-world/x-3dmf";
                    break;
                case ".a":
                    retval = "application/octet-stream";
                    break;
                case ".aab":
                    retval = "application/x-authorware-bin";
                    break;
                case ".aam":
                    retval = "application/x-authorware-map";
                    break;
                case ".aas":
                    retval = "application/x-authorware-seg";
                    break;
                case ".abc":
                    retval = "text/vnd.abc";
                    break;
                case ".acgi":
                    retval = "text/html";
                    break;
                case ".afl":
                    retval = "video/animaflex";
                    break;
                case ".ai":
                    retval = "application/postscript";
                    break;
                case ".aif":
                    retval = "audio/aiff";
                    break;
                case ".aifc":
                    retval = "audio/aiff";
                    break;
                case ".aiff":
                    retval = "audio/aiff";
                    break;
                case ".aim":
                    retval = "application/x-aim";
                    break;
                case ".aip":
                    retval = "text/x-audiosoft-intra";
                    break;
                case ".ani":
                    retval = "application/x-navi-animation";
                    break;
                case ".aos":
                    retval = "application/x-nokia-9000-communicator-add-on-software";
                    break;
                case ".aps":
                    retval = "application/mime";
                    break;
                case ".arc":
                    retval = "application/octet-stream";
                    break;
                case ".arj":
                    retval = "application/arj";
                    break;
                case ".art":
                    retval = "image/x-jg";
                    break;
                case ".asf":
                    retval = "video/x-ms-asf";
                    break;
                case ".asm":
                    retval = "text/x-asm";
                    break;
                case ".asp":
                    retval = "text/asp";
                    break;
                case ".asx":
                    retval = "video/x-ms-asf";
                    break;
                case ".au":
                    retval = "audio/basic";
                    break;
                case ".avi":
                    retval = "video/avi";
                    break;
                case ".avs":
                    retval = "video/avs-video";
                    break;
                case ".bcpio":
                    retval = "application/x-bcpio";
                    break;
                case ".bin":
                    retval = "application/octet-stream";
                    break;
                case ".bm":
                    retval = "image/bmp";
                    break;
                case ".bmp":
                    retval = "image/bmp";
                    break;
                case ".boo":
                    retval = "application/book";
                    break;
                case ".book":
                    retval = "application/book";
                    break;
                case ".boz":
                    retval = "application/x-bzip2";
                    break;
                case ".bsh":
                    retval = "application/x-bsh";
                    break;
                case ".bz":
                    retval = "application/x-bzip";
                    break;
                case ".bz2":
                    retval = "application/x-bzip2";
                    break;
                case ".c":
                    retval = "text/plain";
                    break;
                case ".c++":
                    retval = "text/plain";
                    break;
                case ".cat":
                    retval = "application/vnd.ms-pki.seccat";
                    break;
                case ".cc":
                    retval = "text/plain";
                    break;
                case ".ccad":
                    retval = "application/clariscad";
                    break;
                case ".cco":
                    retval = "application/x-cocoa";
                    break;
                case ".cdf":
                    retval = "application/cdf";
                    break;
                case ".cer":
                    retval = "application/pkix-cert";
                    break;
                case ".cha":
                    retval = "application/x-chat";
                    break;
                case ".chat":
                    retval = "application/x-chat";
                    break;
                case ".class":
                    retval = "application/java";
                    break;
                case ".com":
                    retval = "application/octet-stream";
                    break;
                case ".conf":
                    retval = "text/plain";
                    break;
                case ".cpio":
                    retval = "application/x-cpio";
                    break;
                case ".cpp":
                    retval = "text/x-c";
                    break;
                case ".cpt":
                    retval = "application/x-cpt";
                    break;
                case ".crl":
                    retval = "application/pkcs-crl";
                    break;
                case ".crt":
                    retval = "application/pkix-cert";
                    break;
                case ".csh":
                    retval = "application/x-csh";
                    break;
                case ".css":
                    retval = "text/css";
                    break;
                case ".cxx":
                    retval = "text/plain";
                    break;
                case ".dcr":
                    retval = "application/x-director";
                    break;
                case ".deepv":
                    retval = "application/x-deepv";
                    break;
                case ".def":
                    retval = "text/plain";
                    break;
                case ".der":
                    retval = "application/x-x509-ca-cert";
                    break;
                case ".dif":
                    retval = "video/x-dv";
                    break;
                case ".dir":
                    retval = "application/x-director";
                    break;
                case ".dl":
                    retval = "video/dl";
                    break;
                case ".doc":
                    retval = "application/msword";
                    break;
                case ".dot":
                    retval = "application/msword";
                    break;
                case ".dp":
                    retval = "application/commonground";
                    break;
                case ".drw":
                    retval = "application/drafting";
                    break;
                case ".dump":
                    retval = "application/octet-stream";
                    break;
                case ".dv":
                    retval = "video/x-dv";
                    break;
                case ".dvi":
                    retval = "application/x-dvi";
                    break;
                case ".dwf":
                    retval = "model/vnd.dwf";
                    break;
                case ".dwg":
                    retval = "image/vnd.dwg";
                    break;
                case ".dxf":
                    retval = "image/vnd.dwg";
                    break;
                case ".dxr":
                    retval = "application/x-director";
                    break;
                case ".el":
                    retval = "text/x-script.elisp";
                    break;
                case ".elc":
                    retval = "application/x-elc";
                    break;
                case ".env":
                    retval = "application/x-envoy";
                    break;
                case ".eps":
                    retval = "application/postscript";
                    break;
                case ".es":
                    retval = "application/x-esrehber";
                    break;
                case ".etx":
                    retval = "text/x-setext";
                    break;
                case ".evy":
                    retval = "application/envoy";
                    break;
                case ".exe":
                    retval = "application/octet-stream";
                    break;
                case ".f":
                    retval = "text/plain";
                    break;
                case ".f77":
                    retval = "text/x-fortran";
                    break;
                case ".f90":
                    retval = "text/plain";
                    break;
                case ".fdf":
                    retval = "application/vnd.fdf";
                    break;
                case ".fif":
                    retval = "image/fif";
                    break;
                case ".fli":
                    retval = "video/fli";
                    break;
                case ".flo":
                    retval = "image/florian";
                    break;
                case ".flx":
                    retval = "text/vnd.fmi.flexstor";
                    break;
                case ".fmf":
                    retval = "video/x-atomic3d-feature";
                    break;
                case ".for":
                    retval = "text/x-fortran";
                    break;
                case ".fpx":
                    retval = "image/vnd.fpx";
                    break;
                case ".webp":
                    retval = "image/webp";
                    break;
                case ".svg":
                    retval = "image/svg+xml";
                    break;
                case ".frl":
                    retval = "application/freeloader";
                    break;
                case ".funk":
                    retval = "audio/make";
                    break;
                case ".g":
                    retval = "text/plain";
                    break;
                case ".g3":
                    retval = "image/g3fax";
                    break;
                case ".gif":
                    retval = "image/gif";
                    break;
                case ".gl":
                    retval = "video/gl";
                    break;
                case ".gsd":
                    retval = "audio/x-gsm";
                    break;
                case ".gsm":
                    retval = "audio/x-gsm";
                    break;
                case ".gsp":
                    retval = "application/x-gsp";
                    break;
                case ".gss":
                    retval = "application/x-gss";
                    break;
                case ".gtar":
                    retval = "application/x-gtar";
                    break;
                case ".gz":
                    retval = "application/x-gzip";
                    break;
                case ".gzip":
                    retval = "application/x-gzip";
                    break;
                case ".h":
                    retval = "text/plain";
                    break;
                case ".hdf":
                    retval = "application/x-hdf";
                    break;
                case ".help":
                    retval = "application/x-helpfile";
                    break;
                case ".hgl":
                    retval = "application/vnd.hp-hpgl";
                    break;
                case ".hh":
                    retval = "text/plain";
                    break;
                case ".hlb":
                    retval = "text/x-script";
                    break;
                case ".hlp":
                    retval = "application/hlp";
                    break;
                case ".hpg":
                    retval = "application/vnd.hp-hpgl";
                    break;
                case ".hpgl":
                    retval = "application/vnd.hp-hpgl";
                    break;
                case ".hqx":
                    retval = "application/binhex";
                    break;
                case ".hta":
                    retval = "application/hta";
                    break;
                case ".htc":
                    retval = "text/x-component";
                    break;
                case ".htm":
                    retval = "text/html";
                    break;
                case ".html":
                    retval = "text/html";
                    break;
                case ".htmls":
                    retval = "text/html";
                    break;
                case ".htt":
                    retval = "text/webviewhtml";
                    break;
                case ".htx":
                    retval = "text/html";
                    break;
                case ".ice":
                    retval = "x-conference/x-cooltalk";
                    break;
                case ".ico":
                    retval = "image/x-icon";
                    break;
                case ".idc":
                    retval = "text/plain";
                    break;
                case ".ief":
                    retval = "image/ief";
                    break;
                case ".iefs":
                    retval = "image/ief";
                    break;
                case ".iges":
                    retval = "application/iges";
                    break;
                case ".igs":
                    retval = "application/iges";
                    break;
                case ".ima":
                    retval = "application/x-ima";
                    break;
                case ".imap":
                    retval = "application/x-httpd-imap";
                    break;
                case ".inf":
                    retval = "application/inf";
                    break;
                case ".ins":
                    retval = "application/x-internett-signup";
                    break;
                case ".ip":
                    retval = "application/x-ip2";
                    break;
                case ".isu":
                    retval = "video/x-isvideo";
                    break;
                case ".it":
                    retval = "audio/it";
                    break;
                case ".iv":
                    retval = "application/x-inventor";
                    break;
                case ".ivr":
                    retval = "i-world/i-vrml";
                    break;
                case ".ivy":
                    retval = "application/x-livescreen";
                    break;
                case ".jam":
                    retval = "audio/x-jam";
                    break;
                case ".jav":
                    retval = "text/plain";
                    break;
                case ".java":
                    retval = "text/plain";
                    break;
                case ".jcm":
                    retval = "application/x-java-commerce";
                    break;
                case ".jfif":
                    retval = "image/jpeg";
                    break;
                case ".jfif-tbnl":
                    retval = "image/jpeg";
                    break;
                case ".jpe":
                    retval = "image/jpeg";
                    break;
                case ".jpeg":
                    retval = "image/jpeg";
                    break;
                case ".jpg":
                    retval = "image/jpeg";
                    break;
                case ".jps":
                    retval = "image/x-jps";
                    break;
                case ".js":
                    retval = "application/x-javascript";
                    break;
                case ".jut":
                    retval = "image/jutvision";
                    break;
                case ".kar":
                    retval = "audio/midi";
                    break;
                case ".ksh":
                    retval = "application/x-ksh";
                    break;
                case ".la":
                    retval = "audio/nspaudio";
                    break;
                case ".lam":
                    retval = "audio/x-liveaudio";
                    break;
                case ".latex":
                    retval = "application/x-latex";
                    break;
                case ".lha":
                    retval = "application/octet-stream";
                    break;
                case ".lhx":
                    retval = "application/octet-stream";
                    break;
                case ".list":
                    retval = "text/plain";
                    break;
                case ".lma":
                    retval = "audio/nspaudio";
                    break;
                case ".log":
                    retval = "text/plain";
                    break;
                case ".lsp":
                    retval = "application/x-lisp";
                    break;
                case ".lst":
                    retval = "text/plain";
                    break;
                case ".lsx":
                    retval = "text/x-la-asf";
                    break;
                case ".ltx":
                    retval = "application/x-latex";
                    break;
                case ".lzh":
                    retval = "application/octet-stream";
                    break;
                case ".lzx":
                    retval = "application/octet-stream";
                    break;
                case ".m":
                    retval = "text/plain";
                    break;
                case ".m1v":
                    retval = "video/mpeg";
                    break;
                case ".m2a":
                    retval = "audio/mpeg";
                    break;
                case ".m2v":
                    retval = "video/mpeg";
                    break;
                case ".m3u":
                    retval = "audio/x-mpequrl";
                    break;
                case ".man":
                    retval = "application/x-troff-man";
                    break;
                case ".map":
                    retval = "application/x-navimap";
                    break;
                case ".mar":
                    retval = "text/plain";
                    break;
                case ".mbd":
                    retval = "application/mbedlet";
                    break;
                case ".mcd":
                    retval = "application/mcad";
                    break;
                case ".mcf":
                    retval = "text/mcf";
                    break;
                case ".mcp":
                    retval = "application/netmc";
                    break;
                case ".me":
                    retval = "application/x-troff-me";
                    break;
                case ".mht":
                    retval = "message/rfc822";
                    break;
                case ".mhtml":
                    retval = "message/rfc822";
                    break;
                case ".mid":
                    retval = "audio/midi";
                    break;
                case ".midi":
                    retval = "audio/midi";
                    break;
                case ".mif":
                    retval = "application/x-mif";
                    break;
                case ".mime":
                    retval = "message/rfc822";
                    break;
                case ".mjf":
                    retval = "audio/x-vnd.audioexplosion.mjuicemediafile";
                    break;
                case ".mjpg":
                    retval = "video/x-motion-jpeg";
                    break;
                case ".mm":
                    retval = "application/base64";
                    break;
                case ".mme":
                    retval = "application/base64";
                    break;
                case ".mod":
                    retval = "audio/mod";
                    break;
                case ".moov":
                    retval = "video/quicktime";
                    break;
                case ".mov":
                    retval = "video/quicktime";
                    break;
                case ".movie":
                    retval = "video/x-sgi-movie";
                    break;
                case ".mp2":
                    retval = "audio/mpeg";
                    break;
                case ".mp3":
                    retval = "audio/mpeg";
                    break;
                case ".mpa":
                    retval = "audio/mpeg";
                    break;
                case ".mpc":
                    retval = "application/x-project";
                    break;
                case ".mpe":
                    retval = "video/mpeg";
                    break;
                case ".mpeg":
                    retval = "video/mpeg";
                    break;
                case ".mpg":
                    retval = "video/mpeg";
                    break;
                case ".mpga":
                    retval = "audio/mpeg";
                    break;
                case ".mpp":
                    retval = "application/vnd.ms-project";
                    break;
                case ".mpt":
                    retval = "application/vnd.ms-project";
                    break;
                case ".mpv":
                    retval = "application/vnd.ms-project";
                    break;
                case ".mpx":
                    retval = "application/vnd.ms-project";
                    break;
                case ".mrc":
                    retval = "application/marc";
                    break;
                case ".ms":
                    retval = "application/x-troff-ms";
                    break;
                case ".mv":
                    retval = "video/x-sgi-movie";
                    break;
                case ".my":
                    retval = "audio/make";
                    break;
                case ".mzz":
                    retval = "application/x-vnd.audioexplosion.mzz";
                    break;
                case ".nap":
                    retval = "image/naplps";
                    break;
                case ".naplps":
                    retval = "image/naplps";
                    break;
                case ".nc":
                    retval = "application/x-netcdf";
                    break;
                case ".ncm":
                    retval = "application/vnd.nokia.configuration-message";
                    break;
                case ".nif":
                    retval = "image/x-niff";
                    break;
                case ".niff":
                    retval = "image/x-niff";
                    break;
                case ".nix":
                    retval = "application/x-mix-transfer";
                    break;
                case ".nsc":
                    retval = "application/x-conference";
                    break;
                case ".nvd":
                    retval = "application/x-navidoc";
                    break;
                case ".o":
                    retval = "application/octet-stream";
                    break;
                case ".oda":
                    retval = "application/oda";
                    break;
                case ".omc":
                    retval = "application/x-omc";
                    break;
                case ".omcd":
                    retval = "application/x-omcdatamaker";
                    break;
                case ".omcr":
                    retval = "application/x-omcregerator";
                    break;
                case ".p":
                    retval = "text/x-pascal";
                    break;
                case ".p10":
                    retval = "application/pkcs10";
                    break;
                case ".p12":
                    retval = "application/pkcs-12";
                    break;
                case ".p7a":
                    retval = "application/x-pkcs7-signature";
                    break;
                case ".p7c":
                    retval = "application/pkcs7-mime";
                    break;
                case ".p7m":
                    retval = "application/pkcs7-mime";
                    break;
                case ".p7r":
                    retval = "application/x-pkcs7-certreqresp";
                    break;
                case ".p7s":
                    retval = "application/pkcs7-signature";
                    break;
                case ".part":
                    retval = "application/pro_eng";
                    break;
                case ".pas":
                    retval = "text/pascal";
                    break;
                case ".pbm":
                    retval = "image/x-portable-bitmap";
                    break;
                case ".pcl":
                    retval = "application/vnd.hp-pcl";
                    break;
                case ".pct":
                    retval = "image/x-pict";
                    break;
                case ".pcx":
                    retval = "image/x-pcx";
                    break;
                case ".pdb":
                    retval = "chemical/x-pdb";
                    break;
                case ".pdf":
                    retval = "application/pdf";
                    break;
                case ".pfunk":
                    retval = "audio/make";
                    break;
                case ".pgm":
                    retval = "image/x-portable-greymap";
                    break;
                case ".pic":
                    retval = "image/pict";
                    break;
                case ".pict":
                    retval = "image/pict";
                    break;
                case ".pkg":
                    retval = "application/x-newton-compatible-pkg";
                    break;
                case ".pko":
                    retval = "application/vnd.ms-pki.pko";
                    break;
                case ".pl":
                    retval = "text/plain";
                    break;
                case ".plx":
                    retval = "application/x-pixclscript";
                    break;
                case ".pm":
                    retval = "image/x-xpixmap";
                    break;
                case ".pm4":
                    retval = "application/x-pagemaker";
                    break;
                case ".pm5":
                    retval = "application/x-pagemaker";
                    break;
                case ".png":
                    retval = "image/png";
                    break;
                case ".pnm":
                    retval = "application/x-portable-anymap";
                    break;
                case ".pot":
                    retval = "application/vnd.ms-powerpoint";
                    break;
                case ".pov":
                    retval = "model/x-pov";
                    break;
                case ".ppa":
                    retval = "application/vnd.ms-powerpoint";
                    break;
                case ".ppm":
                    retval = "image/x-portable-pixmap";
                    break;
                case ".pps":
                    retval = "application/vnd.ms-powerpoint";
                    break;
                case ".ppt":
                    retval = "application/vnd.ms-powerpoint";
                    break;
                case ".ppz":
                    retval = "application/vnd.ms-powerpoint";
                    break;
                case ".pre":
                    retval = "application/x-freelance";
                    break;
                case ".prt":
                    retval = "application/pro_eng";
                    break;
                case ".ps":
                    retval = "application/postscript";
                    break;
                case ".psd":
                    retval = "application/octet-stream";
                    break;
                case ".pvu":
                    retval = "paleovu/x-pv";
                    break;
                case ".pwz":
                    retval = "application/vnd.ms-powerpoint";
                    break;
                case ".py":
                    retval = "text/x-script.phyton";
                    break;
                case ".pyc":
                    retval = "applicaiton/x-bytecode.python";
                    break;
                case ".qcp":
                    retval = "audio/vnd.qcelp";
                    break;
                case ".qd3":
                    retval = "x-world/x-3dmf";
                    break;
                case ".qd3d":
                    retval = "x-world/x-3dmf";
                    break;
                case ".qif":
                    retval = "image/x-quicktime";
                    break;
                case ".qt":
                    retval = "video/quicktime";
                    break;
                case ".qtc":
                    retval = "video/x-qtc";
                    break;
                case ".qti":
                    retval = "image/x-quicktime";
                    break;
                case ".qtif":
                    retval = "image/x-quicktime";
                    break;
                case ".ra":
                    retval = "audio/x-pn-realaudio";
                    break;
                case ".ram":
                    retval = "audio/x-pn-realaudio";
                    break;
                case ".ras":
                    retval = "application/x-cmu-raster";
                    break;
                case ".rast":
                    retval = "image/cmu-raster";
                    break;
                case ".rexx":
                    retval = "text/x-script.rexx";
                    break;
                case ".rf":
                    retval = "image/vnd.rn-realflash";
                    break;
                case ".rgb":
                    retval = "image/x-rgb";
                    break;
                case ".rm":
                    retval = "application/vnd.rn-realmedia";
                    break;
                case ".rmi":
                    retval = "audio/mid";
                    break;
                case ".rmm":
                    retval = "audio/x-pn-realaudio";
                    break;
                case ".rmp":
                    retval = "audio/x-pn-realaudio";
                    break;
                case ".rng":
                    retval = "application/ringing-tones";
                    break;
                case ".rnx":
                    retval = "application/vnd.rn-realplayer";
                    break;
                case ".roff":
                    retval = "application/x-troff";
                    break;
                case ".rp":
                    retval = "image/vnd.rn-realpix";
                    break;
                case ".rpm":
                    retval = "audio/x-pn-realaudio-plugin";
                    break;
                case ".rt":
                    retval = "text/richtext";
                    break;
                case ".rtf":
                    retval = "text/richtext";
                    break;
                case ".rtx":
                    retval = "text/richtext";
                    break;
                case ".rv":
                    retval = "video/vnd.rn-realvideo";
                    break;
                case ".s":
                    retval = "text/x-asm";
                    break;
                case ".s3m":
                    retval = "audio/s3m";
                    break;
                case ".saveme":
                    retval = "application/octet-stream";
                    break;
                case ".sbk":
                    retval = "application/x-tbook";
                    break;
                case ".scm":
                    retval = "application/x-lotusscreencam";
                    break;
                case ".sdml":
                    retval = "text/plain";
                    break;
                case ".sdp":
                    retval = "application/sdp";
                    break;
                case ".sdr":
                    retval = "application/sounder";
                    break;
                case ".sea":
                    retval = "application/sea";
                    break;
                case ".set":
                    retval = "application/set";
                    break;
                case ".sgm":
                    retval = "text/sgml";
                    break;
                case ".sgml":
                    retval = "text/sgml";
                    break;
                case ".sh":
                    retval = "application/x-sh";
                    break;
                case ".shar":
                    retval = "application/x-shar";
                    break;
                case ".shtml":
                    retval = "text/html";
                    break;
                case ".sid":
                    retval = "audio/x-psid";
                    break;
                case ".sit":
                    retval = "application/x-sit";
                    break;
                case ".skd":
                    retval = "application/x-koan";
                    break;
                case ".skm":
                    retval = "application/x-koan";
                    break;
                case ".skp":
                    retval = "application/x-koan";
                    break;
                case ".skt":
                    retval = "application/x-koan";
                    break;
                case ".sl":
                    retval = "application/x-seelogo";
                    break;
                case ".smi":
                    retval = "application/smil";
                    break;
                case ".smil":
                    retval = "application/smil";
                    break;
                case ".snd":
                    retval = "audio/basic";
                    break;
                case ".sol":
                    retval = "application/solids";
                    break;
                case ".spc":
                    retval = "text/x-speech";
                    break;
                case ".spl":
                    retval = "application/futuresplash";
                    break;
                case ".spr":
                    retval = "application/x-sprite";
                    break;
                case ".sprite":
                    retval = "application/x-sprite";
                    break;
                case ".src":
                    retval = "application/x-wais-source";
                    break;
                case ".ssi":
                    retval = "text/x-server-parsed-html";
                    break;
                case ".ssm":
                    retval = "application/streamingmedia";
                    break;
                case ".sst":
                    retval = "application/vnd.ms-pki.certstore";
                    break;
                case ".step":
                    retval = "application/step";
                    break;
                case ".stl":
                    retval = "application/sla";
                    break;
                case ".stp":
                    retval = "application/step";
                    break;
                case ".sv4cpio":
                    retval = "application/x-sv4cpio";
                    break;
                case ".sv4crc":
                    retval = "application/x-sv4crc";
                    break;
                case ".svf":
                    retval = "image/vnd.dwg";
                    break;
                case ".svr":
                    retval = "application/x-world";
                    break;
                case ".swf":
                    retval = "application/x-shockwave-flash";
                    break;
                case ".t":
                    retval = "application/x-troff";
                    break;
                case ".talk":
                    retval = "text/x-speech";
                    break;
                case ".tar":
                    retval = "application/x-tar";
                    break;
                case ".tbk":
                    retval = "application/toolbook";
                    break;
                case ".tcl":
                    retval = "application/x-tcl";
                    break;
                case ".tcsh":
                    retval = "text/x-script.tcsh";
                    break;
                case ".tex":
                    retval = "application/x-tex";
                    break;
                case ".texi":
                    retval = "application/x-texinfo";
                    break;
                case ".texinfo":
                    retval = "application/x-texinfo";
                    break;
                case ".text":
                    retval = "text/plain";
                    break;
                case ".tgz":
                    retval = "application/x-compressed";
                    break;
                case ".tif":
                    retval = "image/tiff";
                    break;
                case ".tiff":
                    retval = "image/tiff";
                    break;
                case ".tr":
                    retval = "application/x-troff";
                    break;
                case ".tsi":
                    retval = "audio/tsp-audio";
                    break;
                case ".tsp":
                    retval = "application/dsptype";
                    break;
                case ".tsv":
                    retval = "text/tab-separated-values";
                    break;
                case ".turbot":
                    retval = "image/florian";
                    break;
                case ".txt":
                    retval = "text/plain";
                    break;
                case ".uil":
                    retval = "text/x-uil";
                    break;
                case ".uni":
                    retval = "text/uri-list";
                    break;
                case ".unis":
                    retval = "text/uri-list";
                    break;
                case ".unv":
                    retval = "application/i-deas";
                    break;
                case ".uri":
                    retval = "text/uri-list";
                    break;
                case ".uris":
                    retval = "text/uri-list";
                    break;
                case ".ustar":
                    retval = "application/x-ustar";
                    break;
                case ".uu":
                    retval = "application/octet-stream";
                    break;
                case ".uue":
                    retval = "text/x-uuencode";
                    break;
                case ".vcd":
                    retval = "application/x-cdlink";
                    break;
                case ".vcs":
                    retval = "text/x-vcalendar";
                    break;
                case ".vda":
                    retval = "application/vda";
                    break;
                case ".vdo":
                    retval = "video/vdo";
                    break;
                case ".vew":
                    retval = "application/groupwise";
                    break;
                case ".viv":
                    retval = "video/vivo";
                    break;
                case ".vivo":
                    retval = "video/vivo";
                    break;
                case ".vmd":
                    retval = "application/vocaltec-media-desc";
                    break;
                case ".vmf":
                    retval = "application/vocaltec-media-file";
                    break;
                case ".voc":
                    retval = "audio/voc";
                    break;
                case ".vos":
                    retval = "video/vosaic";
                    break;
                case ".vox":
                    retval = "audio/voxware";
                    break;
                case ".vqe":
                    retval = "audio/x-twinvq-plugin";
                    break;
                case ".vqf":
                    retval = "audio/x-twinvq";
                    break;
                case ".vql":
                    retval = "audio/x-twinvq-plugin";
                    break;
                case ".vrml":
                    retval = "application/x-vrml";
                    break;
                case ".vrt":
                    retval = "x-world/x-vrt";
                    break;
                case ".vsd":
                    retval = "application/x-visio";
                    break;
                case ".vst":
                    retval = "application/x-visio";
                    break;
                case ".vsw":
                    retval = "application/x-visio";
                    break;
                case ".w60":
                    retval = "application/wordperfect6.0";
                    break;
                case ".w61":
                    retval = "application/wordperfect6.1";
                    break;
                case ".w6w":
                    retval = "application/msword";
                    break;
                case ".wav":
                    retval = "audio/wav";
                    break;
                case ".wb1":
                    retval = "application/x-qpro";
                    break;
                case ".wbmp":
                    retval = "image/vnd.wap.wbmp";
                    break;
                case ".web":
                    retval = "application/vnd.xara";
                    break;
                case ".wiz":
                    retval = "application/msword";
                    break;
                case ".wk1":
                    retval = "application/x-123";
                    break;
                case ".wmf":
                    retval = "windows/metafile";
                    break;
                case ".wml":
                    retval = "text/vnd.wap.wml";
                    break;
                case ".wmlc":
                    retval = "application/vnd.wap.wmlc";
                    break;
                case ".wmls":
                    retval = "text/vnd.wap.wmlscript";
                    break;
                case ".wmlsc":
                    retval = "application/vnd.wap.wmlscriptc";
                    break;
                case ".word":
                    retval = "application/msword";
                    break;
                case ".wp":
                    retval = "application/wordperfect";
                    break;
                case ".wp5":
                    retval = "application/wordperfect";
                    break;
                case ".wp6":
                    retval = "application/wordperfect";
                    break;
                case ".wpd":
                    retval = "application/wordperfect";
                    break;
                case ".wq1":
                    retval = "application/x-lotus";
                    break;
                case ".wri":
                    retval = "application/mswrite";
                    break;
                case ".wrl":
                    retval = "application/x-world";
                    break;
                case ".wrz":
                    retval = "x-world/x-vrml";
                    break;
                case ".wsc":
                    retval = "text/scriplet";
                    break;
                case ".wsrc":
                    retval = "application/x-wais-source";
                    break;
                case ".wtk":
                    retval = "application/x-wintalk";
                    break;
                case ".xbm":
                    retval = "image/x-xbitmap";
                    break;
                case ".xdr":
                    retval = "video/x-amt-demorun";
                    break;
                case ".xgz":
                    retval = "xgl/drawing";
                    break;
                case ".xif":
                    retval = "image/vnd.xiff";
                    break;
                case ".xl":
                    retval = "application/excel";
                    break;
                case ".xla":
                    retval = "application/vnd.ms-excel";
                    break;
                case ".xlb":
                    retval = "application/vnd.ms-excel";
                    break;
                case ".xlc":
                    retval = "application/vnd.ms-excel";
                    break;
                case ".xld":
                    retval = "application/vnd.ms-excel";
                    break;
                case ".xlk":
                    retval = "application/vnd.ms-excel";
                    break;
                case ".xll":
                    retval = "application/vnd.ms-excel";
                    break;
                case ".xlm":
                    retval = "application/vnd.ms-excel";
                    break;
                case ".xls":
                    retval = "application/vnd.ms-excel";
                    break;
                case ".xlt":
                    retval = "application/vnd.ms-excel";
                    break;
                case ".xlv":
                    retval = "application/vnd.ms-excel";
                    break;
                case ".xlw":
                    retval = "application/vnd.ms-excel";
                    break;
                case ".xm":
                    retval = "audio/xm";
                    break;
                case ".xml":
                    retval = "application/xml";
                    break;
                case ".xmz":
                    retval = "xgl/movie";
                    break;
                case ".xpix":
                    retval = "application/x-vnd.ls-xpix";
                    break;
                case ".xpm":
                    retval = "image/xpm";
                    break;
                case ".x-png":
                    retval = "image/png";
                    break;
                case ".xsr":
                    retval = "video/x-amt-showrun";
                    break;
                case ".xwd":
                    retval = "image/x-xwd";
                    break;
                case ".xyz":
                    retval = "chemical/x-pdb";
                    break;
                case ".z":
                    retval = "application/x-compressed";
                    break;
                case ".zip":
                    retval = "application/zip";
                    break;
                case ".zoo":
                    retval = "application/octet-stream";
                    break;
                case ".zsh":
                    retval = "text/x-script.zsh";
                    break;
                default:
                    retval = "application/octet-stream";
                    break;
            }
        }
        return retval;
    }

}