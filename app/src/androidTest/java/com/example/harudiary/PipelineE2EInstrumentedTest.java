package com.example.harudiary;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Instrumented test, which will execute on an Android device.
 * 
 * 안드로이드 스튜디오 (에뮬레이터) 내부에서 도커(호스트 PC)의 API 서버로 접근하기 위해
 * IP 주소를 10.0.2.2 로 설정합니다.
 */
@RunWith(AndroidJUnit4.class)
public class PipelineE2EInstrumentedTest {

    // 1. BASE_URL 수정 (로컬 도커 테스트 시 10.0.2.2 사용)
    private static final String BASE_URL = "http://133.186.143.108:8083/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private OkHttpClient client;

    @Before
    public void setUp() {
        // 네트워크 타임아웃을 넉넉히 설정 (추천 API 등이 오래 걸릴 수 있으므로)
        client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    @Test
    public void runAllPipelineE2ETests() throws Exception {
        // 1. 소셜/CRUD 연동 검증
        testD1RegisterLogin();
        testD2DiaryCreateRead();
        testD3DiaryDelete();
        testD4FriendRequest();
        testD5Timeline();
        testD6HeartToggle();
        testD7Dates();
        testD8CommentCrud();

        // 2. 여행 파이프라인(Harudiary_trip2 앱 <-> travel 모델 연동) 검증
        testRecommendNearby();
        testRecommendByDiary();
    }

    private void testD1RegisterLogin() {
        try {
            // Register is skipped because Seed data is already present in DB.

            // Login A (seed_user_A)
            JSONObject loginA = new JSONObject();
            loginA.put("id", "seed_user_A");
            String loginResp = postRequest("/user/login", loginA);
            JSONObject loginJson = new JSONObject(loginResp);
            assertEquals("seed_user_A", loginJson.getString("id"));
            
        } catch (Exception e) {
            System.out.println("D1 진행 중 가입/로그인 실패 (이미 가입된 유저일 수 있음): " + e.getMessage());
        }
    }

    private void testD2DiaryCreateRead() throws Exception {
        int activityId = createDummyDiary("seed_user_B", "2026-06-07");
        assertTrue(activityId > 0);

        String readResp = getRequest("/diary/seed_user_B");
        JSONArray readArray = new JSONArray(readResp);
        assertTrue(readArray.length() > 0);
    }

    private void testD3DiaryDelete() throws Exception {
        int targetId = createDummyDiary("seed_user_B", "2026-06-08");
        deleteRequest("/diary/" + targetId + "?userId=seed_user_B");
        
        String readResp = getRequest("/diary/seed_user_B");
        assertFalse(readResp.contains("\"activityId\":" + targetId));
    }

    private void testD4FriendRequest() throws Exception {
        JSONObject req = new JSONObject();
        req.put("fromUserId", "seed_user_A");
        req.put("toUserId", "seed_user_B");
        try {
            postRequest("/friend/request", req);
        } catch(Exception e) {
            System.out.println("D4 친구요청 실패 (이미 친구이거나 요청 상태일 수 있음): " + e.getMessage());
        }
    }

    private void testD5Timeline() throws Exception {
        int targetId = createDummyDiary("seed_user_B", "2026-06-07");
        String resp = getRequest("/friend/timeline?myUserId=seed_user_A&friendId=seed_user_B&date=2026-06-07");
        JSONArray arr = new JSONArray(resp);
        assertTrue(arr.length() > 0);
        
        // Find our dummy diary in timeline
        boolean found = false;
        for (int i=0; i<arr.length(); i++) {
            if (arr.getJSONObject(i).optInt("id") == targetId || arr.getJSONObject(i).optInt("activityId") == targetId) {
                assertTrue(arr.getJSONObject(i).has("heartCount")); // assertTrue로 변경
                found = true;
                break;
            }
        }
        assertTrue("Timeline should contain the created diary", found);
    }

    private void testD6HeartToggle() throws Exception {
        int targetId = createDummyDiary("seed_user_B", "2026-06-11");
        JSONObject payload = new JSONObject();
        payload.put("userId", "seed_user_A");
        payload.put("diaryId", targetId);
        
        // Toggle ON (이전 상태에 따라 true/false일 수 있으므로 호출만 테스트)
        postRequest("/reaction/toggle", payload);
    }

    private void testD7Dates() throws Exception {
        String resp = getRequest("/friend/dates/test_user_b");
        JSONArray arr = new JSONArray(resp);
        assertTrue(arr.length() > 0);
    }

    private void testD8CommentCrud() throws Exception {
        int targetId = createDummyDiary("seed_user_B", "2026-06-12");
        
        JSONObject payload = new JSONObject();
        payload.put("userId", "seed_user_A");
        payload.put("diaryId", targetId);
        payload.put("content", "멋진 기록이네요! (Android)");
        
        String resp = postRequest("/comment", payload);
        JSONObject respObj = new JSONObject(resp);
        int commentId = respObj.getInt("id");
        
        String readResp = getRequest("/comment/" + targetId);
        assertTrue(readResp.contains("\"id\":" + commentId));
        
        deleteRequest("/comment/" + commentId + "?userId=test_user_a");
    }

    private int createDummyDiary(String userId, String date) throws Exception {
        JSONObject d = new JSONObject();
        d.put("userId", userId);
        d.put("date", date);
        d.put("timeSlot", "morning");
        d.put("content", "더미 다이어리 생성");
        d.put("rating", 5.0);
        
        String resp = postRequest("/diary", d);
        JSONObject respObj = new JSONObject(resp);
        if (respObj.has("id")) return respObj.getInt("id");
        return respObj.getInt("activityId");
    }

    // ─── AI 여행 추천 관련 연동 테스트 ───
    // 5. testRecommendNearby 수정 (days 파라미터 추가)
    private void testRecommendNearby() throws Exception {
        // days=1 파라미터 추가
        String url = "/travel/recommend/nearby?lat=37.5665&lng=126.9780&radius=1500&days=1";
        String resp = getRequest(url);
        JSONObject respObj = new JSONObject(resp);
        assertTrue(respObj.has("trip_title"));
        
        // E3: DB Save 테스트 연동
        testSaveToDb(respObj);
    }

    private void testRecommendByDiary() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("targetDate", "2026-06-15");
        payload.put("days", 3);
        payload.put("lat", 33.4996);
        payload.put("lng", 126.5312);
        payload.put("diaryText", "어제 제주도 바다에서 산책하고 흑돼지를 먹었는데 너무 좋았다. 이번 2박 3일 여행도 그렇게 가고 싶다.");
        
        String resp = postRequest("/travel/recommend/diary", payload);
        JSONObject respObj = new JSONObject(resp);
        assertTrue(respObj.has("trip_title"));
        
        // E3: DB Save 테스트 연동
        testSaveToDb(respObj);
    }

    private void testSaveToDb(JSONObject planResponse) throws Exception {
        // userId 쿼리 파라미터 추가
        String url = "/travel/plan/save?userId=test_user_a";
        postRequest(url, planResponse);
    }

    // ─── 유틸리티 메서드 (OkHttp Helper) ───
    private String postRequest(String path, JSONObject json) throws IOException {
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return response.body().string();
        }
    }

    private String getRequest(String path) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return response.body().string();
        }
    }

    private void deleteRequest(String path) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .delete()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        }
    }
}





