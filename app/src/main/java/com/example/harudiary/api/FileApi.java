package com.example.harudiary.api;

import java.util.Map;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface FileApi {
    @Multipart
    @POST("/api/upload")
    Call<Map<String, String>> uploadFile(@Part MultipartBody.Part file);
}
