package com.amnix.adblockwebview.util;

import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
