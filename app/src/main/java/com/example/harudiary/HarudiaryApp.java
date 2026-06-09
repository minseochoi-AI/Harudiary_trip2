package com.example.harudiary;

import android.app.Application;

import com.kakao.vectormap.KakaoMapSdk;

/**
 * Application 클래스 — 앱 시작 시 Kakao Map SDK 초기화
 *
 * ★ KakaoMapSdk.init() 없이는 MapView.start()가 무음으로 실패한다.
 *   SDK 2.x 이상에서는 반드시 명시적으로 초기화해야 한다.
 */
public class HarudiaryApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // AndroidManifest.xml의 meta-data APP_KEY와 동일한 키
        KakaoMapSdk.init(this, "36721e8c1c608dfe646c736c675b117d");
    }
}
