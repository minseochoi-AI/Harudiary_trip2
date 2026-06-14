package com.example.harudiary;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PipelineE2EInstrumentedTest {

    private static final String BASE_URL = "http://133.186.143.108:8083/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client;
    private String uA;
    private String uB;

    @Before
    public void setUp() throws Exception {
        long ts = System.currentTimeMillis();
        uA = "p_userA_" + ts;
        uB = "p_userB_" + ts;
        
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
                
        // Register users
        registerUser(uA);
        registerUser(uB);
    }

    private void registerUser(String id) throws Exception {
        JSONObject reg = new JSONObject();
        reg.put("id", id);
        reg.put("nickname", "User_" + id);
        postRequest("/user/register", reg);
    }

    @Test
    public void runPipelineTests() throws Exception {
        // API #22
        JSONObject loginPayload = new JSONObject();
        loginPayload.put("id", uA);
        loginPayload.put("nickname", "User_" + uA);
        String loginResp = postRequest("/user/login", loginPayload);
        assertNotNull(loginResp);

        // API #23
        String diaryResp = getRequest("/diary/" + uB);
        assertNotNull(diaryResp);

        // Prep: create diary to delete
        JSONObject createDelete = new JSONObject();
        createDelete.put("userId", uB);
        createDelete.put("date", "2026-06-08");
        createDelete.put("content", "Delete me");
        createDelete.put("rating", 5.0);
        String createdDeleteResp = postRequest("/diary", createDelete);
        JSONObject createdDeleteObj = new JSONObject(createdDeleteResp);
        long deleteDiaryId = createdDeleteObj.has("id") ? createdDeleteObj.getLong("id") : createdDeleteObj.getLong("activityId");

        // API #24
        deleteRequest("/diary/" + deleteDiaryId + "?userId=" + uB);

        // API #25
        JSONObject friendReq = new JSONObject();
        friendReq.put("fromUserId", uA);
        friendReq.put("toUserId", uB);
        postRequest("/friend/request", friendReq);

        // Prep: Auto accept friend request
        String reqsResp = getRequest("/friend/requests/" + uB);
        JSONArray reqs = new JSONArray(reqsResp);
        if (reqs.length() > 0) {
            long reqId = reqs.getJSONObject(0).getLong("id");
            postRequest("/friend/accept/" + reqId, new JSONObject());
        }

        // API #26
        String timelineResp = getRequest("/friend/timeline?myUserId=" + uA + "&friendId=" + uB + "&date=2026-06-07");
        assertNotNull(timelineResp);

        // Prep: Create NEW diary for reaction/comment
        JSONObject createActive = new JSONObject();
        createActive.put("userId", uB);
        createActive.put("date", "2026-06-09");
        createActive.put("content", "Keep me");
        createActive.put("rating", 5.0);
        String createdActiveResp = postRequest("/diary", createActive);
        JSONObject createdActiveObj = new JSONObject(createdActiveResp);
        long activeDiaryId = createdActiveObj.has("id") ? createdActiveObj.getLong("id") : createdActiveObj.getLong("activityId");

        // API #27
        JSONObject reactionReq = new JSONObject();
        reactionReq.put("userId", uA);
        reactionReq.put("diaryId", activeDiaryId);
        postRequest("/reaction/toggle", reactionReq);

        // API #28
        String datesResp = getRequest("/friend/dates/" + uB);
        assertNotNull(datesResp);

        // API #29
        JSONObject commentReq = new JSONObject();
        commentReq.put("userId", uA);
        commentReq.put("diaryId", activeDiaryId);
        commentReq.put("content", "hello");
        String commentResp = postRequest("/comment", commentReq);
        JSONObject commentObj = new JSONObject(commentResp);
        long commentId = commentObj.has("id") ? commentObj.getLong("id") : commentObj.getLong("commentId");

        // API #30
        String getCommentResp = getRequest("/comment/" + activeDiaryId);
        assertNotNull(getCommentResp);

        // API #31
        deleteRequest("/comment/" + commentId + "?userId=" + uA);

        // API #32 & #33
        JSONObject recommendReq = new JSONObject();
        recommendReq.put("targetDate", "2026-06-20");
        recommendReq.put("days", 1);
        recommendReq.put("lat", 0.0);
        recommendReq.put("lng", 0.0);
        recommendReq.put("diaryText", "Test");

        String rec1 = postRequest("/travel/recommend/diary", recommendReq);
        assertNotNull(rec1);

        String rec2 = postRequest("/travel/recommend/diary", recommendReq);
        assertNotNull(rec2);
    }

    private String postRequest(String path, JSONObject json) throws IOException {
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder().url(BASE_URL + path).post(body).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return response.body() != null ? response.body().string() : "";
        }
    }

    private String getRequest(String path) throws IOException {
        Request request = new Request.Builder().url(BASE_URL + path).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return response.body() != null ? response.body().string() : "";
        }
    }

    private void deleteRequest(String path) throws IOException {
        Request request = new Request.Builder().url(BASE_URL + path).delete().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        }
    }
}
