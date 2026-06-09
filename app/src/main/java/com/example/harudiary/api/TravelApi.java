package com.example.harudiary.api;

import com.example.harudiary.model.TravelPlanResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface TravelApi {
    
    @GET("api/env/current")
    Call<Map<String, Object>> getEnvCurrent(
            @Query("lat") double lat, 
            @Query("lng") double lng
    );

    @GET("api/travel/recommend/nearby")
    Call<TravelPlanResponse> recommendNearby(
            @Query("lat") double lat, 
            @Query("lng") double lng, 
            @Query("radius") int radius,
            @Query("days") int days
    );

    @POST("api/travel/recommend/diary")
    Call<TravelPlanResponse> recommendByDiary(@Body DiaryRecommendRequest request);

    @POST("api/travel/plan/save")
    Call<Void> savePlan(
            @Body TravelPlanResponse request, 
            @Query("userId") String userId, 
            @Query("diaryId") Integer diaryId
    );

    class DiaryRecommendRequest {
        public String targetDate;
        public int days;
        public double lat;
        public double lng;
        public String diaryText;

        public DiaryRecommendRequest(String targetDate, int days, double lat, double lng, String diaryText) {
            this.targetDate = targetDate;
            this.days = days;
            this.lat = lat;
            this.lng = lng;
            this.diaryText = diaryText;
        }
    }
}
