package com.amnix.adblockwebview.util;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class PermissionUtils {

    public static int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0;

    public static boolean hasPermission(Activity activity, String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(activity, permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);

            return false;
        }
        return true;
    }
}
