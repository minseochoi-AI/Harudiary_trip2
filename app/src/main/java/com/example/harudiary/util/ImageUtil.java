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
            com.bumptech.glide.Glide.with(context)
                    .load(uriStr)
                    .error(android.R.color.transparent)
                    .into(iv);
        } catch (Exception e) {
            iv.setImageURI(null);
        }
    }
}
