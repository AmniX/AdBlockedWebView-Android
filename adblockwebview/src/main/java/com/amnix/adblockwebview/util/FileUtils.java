package com.amnix.adblockwebview.util;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.amnix.adblockwebview.R;

import java.io.File;

public class FileUtils {
    public static long downloadFile(Context context, String url, String mimeType) {
        try {
            String[] fnm = url.split("/");
            String fileName = fnm[fnm.length - 1];
            fileName = getFileName(fileName);
            String host = fnm[2];
            String extension = MimeTypeMap.getFileExtensionFromUrl(url);
            if(extension.equals(""))
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            String fileNameWithExtension = fileName + "." + extension;

            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(fileNameWithExtension);
            request.setMimeType(mimeType);
            request.setDescription(host);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileNameWithExtension);
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            if (!downloadsDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                downloadsDir.mkdirs();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            } else {
                //noinspection deprecation
                request.setShowRunningNotification(true);
            }
            request.setVisibleInDownloadsUi(true);
            long downloadId = dm.enqueue(request);

            Toast.makeText(context, context.getString(R.string.message_download_started), Toast.LENGTH_SHORT).show();

            return downloadId;
        } catch (SecurityException e) {
            throw new SecurityException("No permission allowed: android.permission.WRITE_EXTERNAL_STORAGE");
        }
    }

    private static String getFileName(String fileName) {
        if (!TextUtils.isEmpty(fileName)) {
            int fragment = fileName.lastIndexOf('#');
            if (fragment > 0) {
                fileName = fileName.substring(0, fragment);
            }

            int query = fileName.lastIndexOf('?');
            if (query > 0) {
                fileName = fileName.substring(0, query);
            }

            return fileName;
        } else {
            return "";
        }
    }
}
