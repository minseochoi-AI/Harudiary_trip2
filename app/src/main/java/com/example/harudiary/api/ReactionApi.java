package com.example.harudiary.api;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ReactionApi {
    @POST("/api/reaction/toggle")
    Call<Boolean> toggleHeart(@Body Map<String, Object> payload);

    @retrofit2.http.GET("/api/reaction/counts/{diaryId}")
    Call<Map<String, Integer>> getReactionCounts(@retrofit2.http.Path("diaryId") long diaryId);
}
