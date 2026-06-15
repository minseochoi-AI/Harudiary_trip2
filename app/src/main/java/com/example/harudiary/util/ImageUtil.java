package com.example.harudiary.util;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

public class ImageUtil {
    public static void setSafeImageURI(Context context, ImageView iv, String uriStr) {
        if (uriStr == null || uriStr.isEmpty()) {
            iv.setImageURI(null);
            iv.setVisibility(android.view.View.GONE);
            return;
        }

        try {
            com.bumptech.glide.Glide.with(context)
                    .load(uriStr)
                    .dontAnimate()
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(
                                @androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e,
                                Object model,
                                com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                boolean isFirstResource) {
                            // 로드 실패 시 강제로 숨김 처리 (핑크색 엑스박스 방지)
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                                iv.setVisibility(android.view.View.GONE));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(
                                android.graphics.drawable.Drawable resource,
                                Object model,
                                com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                com.bumptech.glide.load.DataSource dataSource,
                                boolean isFirstResource) {
                            // 로드 성공 시 정상적으로 보여줌
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                                iv.setVisibility(android.view.View.VISIBLE));
                            return false;
                        }
                    })
                    .into(iv);
        } catch (Exception e) {
            iv.setVisibility(android.view.View.GONE);
        }
    }
}
