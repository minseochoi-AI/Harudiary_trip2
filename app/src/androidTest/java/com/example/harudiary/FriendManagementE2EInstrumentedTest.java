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
public class FriendManagementE2EInstrumentedTest {

    private static final String BASE_URL = "http://133.186.143.108:8083/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client;
    private String uA;
    private String uB;
    private String uC;

    @Before
    public void setUp() throws Exception {
        long ts = System.currentTimeMillis();
        uA = "p_userA_" + ts;
        uB = "p_userB_" + ts;
        uC = "p_userC_" + ts;
        
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
                
        // Register users
        registerUser(uA);
        registerUser(uB);
        registerUser(uC);
    }

    private void registerUser(String id) throws Exception {
        JSONObject reg = new JSONObject();
        reg.put("id", id);
        reg.put("nickname", "User_" + id);
        postRequest("/user/register", reg);
    }

    @Test
    public void runFriendManagementTests() throws Exception {
        // API #7
        String searchResp = getRequest("/friend/search?userId=" + uA + "&query=B");
        assertNotNull(searchResp);
        
        // API #8
        String browseResp = getRequest("/friend/browse/" + uA);
        assertNotNull(browseResp);
        
        // API #9
        JSONObject reqPayload = new JSONObject();
        reqPayload.put("fromUserId", uC);
        reqPayload.put("toUserId", uA);
        postRequest("/friend/request", reqPayload);
        
        // API #10
        String statusResp = getRequest("/friend/status?userId=" + uC + "&friendId=" + uA);
        assertNotNull(statusResp);
        
        // API #11
        String pendingResp = getRequest("/friend/pending-count/" + uA);
        assertNotNull(pendingResp);
        
        // API #12
        String reqsResp = getRequest("/friend/requests/" + uA);
        JSONArray reqs = new JSONArray(reqsResp);
        assertTrue(reqs.length() > 0);
        long reqId = reqs.getJSONObject(0).getLong("id");
        
        // API #13
        postRequest("/friend/reject/" + reqId, new JSONObject());
        
        // API #14
        postRequest("/friend/request", reqPayload);
        
        // API #15
        String reqsResp2 = getRequest("/friend/requests/" + uA);
        JSONArray reqs2 = new JSONArray(reqsResp2);
        assertTrue(reqs2.length() > 0);
        long reqId2 = reqs2.getJSONObject(0).getLong("id");
        
        // API #16
        postRequest("/friend/accept/" + reqId2, new JSONObject());
        
        // API #17
        String listResp = getRequest("/friend/list/" + uA);
        assertNotNull(listResp);
        
        // API #18
        JSONObject diaryPayload = new JSONObject();
        diaryPayload.put("userId", uC);
        diaryPayload.put("date", "2026-06-25");
        diaryPayload.put("timeSlot", "morning");
        diaryPayload.put("content", "Friend Test");
        diaryPayload.put("rating", 4.0);
        postRequest("/diary", diaryPayload);
        
        // API #19
        JSONObject planPayload = new JSONObject();
        planPayload.put("trip_title", "Friend Plan");
        planPayload.put("days", new JSONArray());
        postRequest("/travel/plan/save?userId=" + uC + "&date=2026-06-26", planPayload);
        
        // API #20
        String datesResp = getRequest("/friend/dates/" + uC);
        assertNotNull(datesResp);
        
        // API #21
        deleteRequest("/friend/delete?userId=" + uA + "&friendId=" + uC);
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
