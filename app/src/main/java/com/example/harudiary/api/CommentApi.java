package com.example.harudiary.api;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CommentApi {
    @GET("/api/comment/{diaryId}")
    Call<List<Map<String, Object>>> getComments(@Path("diaryId") long diaryId);

    @POST("/api/comment")
    Call<Map<String, Object>> addComment(@Body Map<String, Object> payload);

    @DELETE("/api/comment/{commentId}")
    Call<Void> deleteComment(@Path("commentId") long commentId, @Query("userId") String userId);
}
