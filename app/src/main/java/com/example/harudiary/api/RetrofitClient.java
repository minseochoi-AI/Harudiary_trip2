package com.example.harudiary.api;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // 백엔드 컨테이너 포트가 8083 이므로, 에뮬레이터 환경(10.0.2.2) 또는 실제 디바이스 IP 주소 설정 필요
    // 우분투 서버의 외부 공인 IP 적용
    private static final String BASE_URL = "http://133.186.143.108:8083/";
    
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(1, TimeUnit.MINUTES) // 연결 타임아웃 1분
                    .readTimeout(1, TimeUnit.MINUTES)    // 제미나이 응답 대기를 위한 읽기 타임아웃 1분
                    .writeTimeout(1, TimeUnit.MINUTES)   // 쓰기 타임아웃 1분
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static Retrofit getInstance() {
        return getClient();
    }
}
