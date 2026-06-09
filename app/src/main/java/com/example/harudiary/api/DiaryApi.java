package com.example.harudiary.api;

import com.example.harudiary.model.Record;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Path;
import java.util.List;

public interface DiaryApi {
    @POST("/api/diary")
    Call<Record> createDiary(@Body Map<String, Object> payload);

    @GET("/api/diary/{userId}")
    Call<List<Record>> getDiaries(@Path("userId") String userId);

    @GET("/api/diary/{userId}/dates")
    Call<List<String>> getRecordDates(@Path("userId") String userId, @retrofit2.http.Query("yearMonth") String yearMonth);

    @GET("/api/diary/{userId}/count")
    Call<Integer> getMonthlyCount(@Path("userId") String userId, @retrofit2.http.Query("yearMonth") String yearMonth);

    @GET("/api/diary/{userId}/streak")
    Call<Integer> getStreak(@Path("userId") String userId);

    @GET("/api/diary/{userId}/date/{date}")
    Call<List<Record>> getActivitiesByDate(@Path("userId") String userId, @Path("date") String date);

    @retrofit2.http.DELETE("/api/diary/{diaryId}")
    Call<Void> deleteDiary(@Path("diaryId") int diaryId);
}
