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
public class DiaryStatsE2EInstrumentedTest {

    private static final String BASE_URL = "http://133.186.143.108:8083/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client;
    private String uA;

    @Before
    public void setUp() throws Exception {
        long ts = System.currentTimeMillis();
        uA = "p_userA_" + ts;
        
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
                
        // Register user
        JSONObject reg = new JSONObject();
        reg.put("id", uA);
        reg.put("nickname", "User_" + uA);
        postRequest("/user/register", reg);
    }

    @Test
    public void runDiaryStatsTests() throws Exception {
        // API #1
        JSONObject d = new JSONObject();
        d.put("userId", uA);
        d.put("date", "2026-06-25");
        d.put("timeSlot", "morning");
        d.put("content", "Test Content");
        d.put("rating", 4.0);
        postRequest("/diary", d);
        
        // API #2
        JSONObject plan = new JSONObject();
        plan.put("trip_title", "Test Trip");
        plan.put("days", new JSONArray());
        postRequest("/travel/plan/save?userId=" + uA + "&date=2026-06-26", plan);
        
        // API #3
        String envResp = getRequest("/env/current?lat=37.5&lng=127.0");
        assertNotNull(envResp);
        
        // API #4
        String countResp = getRequest("/diary/" + uA + "/count?yearMonth=2026-06");
        assertTrue(Integer.parseInt(countResp.trim()) >= 0);
        
        // API #5
        String streakResp = getRequest("/diary/" + uA + "/streak");
        assertTrue(Integer.parseInt(streakResp.trim()) >= 0);
        
        // API #6
        String dateResp = getRequest("/diary/" + uA + "/date/2026-06-25");
        assertTrue(new JSONArray(dateResp).length() > 0);
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
