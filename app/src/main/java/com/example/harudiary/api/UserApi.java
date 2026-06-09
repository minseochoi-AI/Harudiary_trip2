package com.example.harudiary.api;

import com.example.harudiary.model.User;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UserApi {
    @POST("/api/user/register")
    Call<User> register(@Body Map<String, String> payload);

    @POST("/api/user/login")
    Call<User> login(@Body Map<String, String> payload);
}
