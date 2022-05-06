package com.amnix.adblockwebview.util;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;

public class ClipboardUtils {

    private ClipboardUtils() {
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void copyText(Context context, String text) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clip = ClipData.newPlainText("", text);
        cm.setPrimaryClip(clip);
    }

    public static boolean hasText(Context context) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        ClipDescription description = cm.getPrimaryClipDescription();
        ClipData clipData = cm.getPrimaryClip();
        return clipData != null
                && description != null
                && (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN));
    }

    public static CharSequence getText(Context context) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        ClipDescription description = cm.getPrimaryClipDescription();
        ClipData clipData = cm.getPrimaryClip();
        if (clipData != null && description != null && description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
            return clipData.getItemAt(0).getText();
        } else {
            return "";
        }
    }
}
