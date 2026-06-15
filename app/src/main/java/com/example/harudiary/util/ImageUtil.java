package com.example.harudiary.util;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

public class ImageUtil {
    public static void setSafeImageURI(Context context, ImageView iv, String uriStr) {
        if (uriStr == null || uriStr.isEmpty()) {
            iv.setImageURI(null);
            return;
        }
        try {
            Uri uri = Uri.parse(uriStr);
            if (uriStr.startsWith("content://")) {
                context.getContentResolver().openInputStream(uri).close();
            }
            iv.setImageURI(uri);
        } catch (Exception e) {
            iv.setImageURI(null);
        }
    }
}
