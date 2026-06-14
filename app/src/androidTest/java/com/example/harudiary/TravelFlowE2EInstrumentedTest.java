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
public class TravelFlowE2EInstrumentedTest {

    private static final String BASE_URL = "http://133.186.143.108:8083/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client;
    private String uF;
    private String uFF;

    @Before
    public void setUp() throws Exception {
        long ts = System.currentTimeMillis();
        uF = "p_userF_" + ts;
        uFF = "p_userFF_" + ts;
        
        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
                
        // Register users
        registerUser(uF);
        registerUser(uFF);
    }

    private void registerUser(String id) throws Exception {
        JSONObject reg = new JSONObject();
        reg.put("id", id);
        reg.put("nickname", "User_" + id);
        postRequest("/user/register", reg);
    }

    @Test
    public void runTravelFlowTests() throws Exception {
        // API #34
        JSONObject recommendReq = new JSONObject();
        recommendReq.put("targetDate", "2026-06-20");
        recommendReq.put("days", 1);
        recommendReq.put("lat", 0.0);
        recommendReq.put("lng", 0.0);
        recommendReq.put("diaryText", "도쿄");

        String recRespStr = postRequest("/travel/recommend/diary", recommendReq);
        JSONObject planResp;
        try {
            planResp = new JSONObject(recRespStr);
        } catch (Exception e) {
            planResp = new JSONObject();
            planResp.put("trip_title", "Test");
            planResp.put("days", new JSONArray());
        }

        // API #35
        postRequest("/travel/plan/save?userId=" + uF + "&date=2026-06-20", planResp);

        // API #36
        JSONObject diaryReq = new JSONObject();
        diaryReq.put("userId", uF);
        diaryReq.put("date", "2026-06-20");
        diaryReq.put("timeSlot", "evening");
        diaryReq.put("content", "도쿄 도착");
        diaryReq.put("rating", 4.0);
        postRequest("/diary", diaryReq);

        // API #37
        String getDiaryResp = getRequest("/diary/" + uF);
        assertNotNull(getDiaryResp);

        // API #38
        JSONObject diaryWithTitle = new JSONObject();
        diaryWithTitle.put("userId", uF);
        diaryWithTitle.put("date", "2026-06-21");
        diaryWithTitle.put("timeSlot", "lunch");
        diaryWithTitle.put("content", "완료");
        diaryWithTitle.put("rating", 5.0);
        diaryWithTitle.put("trip_title", "도쿄 여행");
        
        String createdDiaryStr = postRequest("/diary", diaryWithTitle);
        JSONObject createdDiaryObj = new JSONObject(createdDiaryStr);
        long diaryId = 1;
        if (createdDiaryObj.has("activityId")) {
            diaryId = createdDiaryObj.getLong("activityId");
        } else if (createdDiaryObj.has("id")) {
            diaryId = createdDiaryObj.getLong("id");
        }

        // API #39
        JSONObject friendReq = new JSONObject();
        friendReq.put("fromUserId", uFF);
        friendReq.put("toUserId", uF);
        postRequest("/friend/request", friendReq);

        // Prep: Auto accept friend request
        String reqsResp = getRequest("/friend/requests/" + uF);
        JSONArray reqs = new JSONArray(reqsResp);
        if (reqs.length() > 0) {
            long reqId = reqs.getJSONObject(0).getLong("id");
            postRequest("/friend/accept/" + reqId, new JSONObject());
        }

        // API #40
        String timelineResp = getRequest("/friend/timeline?myUserId=" + uFF + "&friendId=" + uF + "&date=2026-06-21");
        assertNotNull(timelineResp);

        // API #41
        JSONObject reactionReq = new JSONObject();
        reactionReq.put("userId", uFF);
        reactionReq.put("diaryId", diaryId);
        postRequest("/reaction/toggle", reactionReq);
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
}
