package com.example.harudiary.api;

import com.example.harudiary.model.TimelineDTO;
import com.example.harudiary.model.User;
import com.example.harudiary.model.FriendRequest;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.DELETE;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Body;

public interface FriendApi {
    @GET("/api/friend/dates/{friendId}")
    Call<List<String>> getFriendDates(@Path("friendId") String friendId);

    @GET("/api/friend/timeline")
    Call<List<TimelineDTO>> getFriendTimeline(
            @Query("myUserId") String myUserId,
            @Query("friendId") String friendId,
            @Query("date") String date
    );

    @GET("/api/friend/pending-count/{userId}")
    Call<Integer> getPendingRequestCount(@Path("userId") String userId);

    @GET("/api/friend/browse/{userId}")
    Call<List<Map<String, Object>>> getFriendBrowseList(@Path("userId") String userId);

    @POST("/api/friend/request")
    Call<Map<String, Object>> requestFriend(@Body Map<String, String> payload);

    @POST("/api/friend/request")
    Call<FriendRequest> sendRequest(@Body Map<String, String> payload);

    @GET("/api/friend/search")
    Call<List<Map<String, Object>>> searchUsers(@Query("userId") String userId, @Query("query") String query);

    @GET("/api/friend/status")
    Call<String> getRelationshipStatus(@Query("userId") String userId, @Query("friendId") String friendId);

    @POST("/api/friend/accept/{requestId}")
    Call<Void> acceptFriendRequest(@Path("requestId") long requestId);

    @POST("/api/friend/reject/{requestId}")
    Call<Void> rejectFriendRequest(@Path("requestId") long requestId);

    @DELETE("/api/friend/delete")
    Call<Void> deleteFriend(@Query("userId") String userId, @Query("friendId") String friendId);

    @GET("/api/friend/requests/{userId}")
    Call<List<FriendRequest>> getPendingRequests(@Path("userId") String userId);

    @GET("/api/friend/list/{userId}")
    Call<List<User>> getFriends(@Path("userId") String userId);
}
