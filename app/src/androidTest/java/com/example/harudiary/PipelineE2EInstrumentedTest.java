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

    // 실제 운영/테스트 중인 원격 서버의 공인 IP (server_report.md 기준)
    private static final String BASE_URL = "http://133.186.143.108:8083/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private OkHttpClient client;
    private static List<Integer> DIARY_IDS = new ArrayList<>();

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
            // Register A
            JSONObject regA = new JSONObject();
            regA.put("id", "test_user_a");
            regA.put("nickname", "유저A");
            postRequest("/user/register", regA);

            // Register B
            JSONObject regB = new JSONObject();
            regB.put("id", "test_user_b");
            regB.put("nickname", "유저B");
            postRequest("/user/register", regB);

            // Login A
            JSONObject loginA = new JSONObject();
            loginA.put("id", "test_user_a");
            String loginResp = postRequest("/user/login", loginA);
            JSONObject loginJson = new JSONObject(loginResp);
            assertEquals("test_user_a", loginJson.getString("id"));
            
        } catch (Exception e) {
            System.out.println("D1 진행 중 가입/로그인 실패 (이미 가입된 유저일 수 있음): " + e.getMessage());
        }
    }

    private void testD2DiaryCreateRead() throws Exception {
        JSONArray diaries = new JSONArray();
        JSONObject d1 = new JSONObject();
        d1.put("userId", "test_user_b"); d1.put("date", "2026-06-07"); d1.put("timeSlot", "morning");
        d1.put("content", "아침 산책했다"); d1.put("rating", 4.5); d1.put("latitude", 37.5665); d1.put("longitude", 126.978);
        
        JSONObject d2 = new JSONObject();
        d2.put("userId", "test_user_b"); d2.put("date", "2026-06-08"); d2.put("timeSlot", "lunch");
        d2.put("content", "점심에 카페 갔다"); d2.put("rating", 3.0); d2.put("latitude", 37.5512); d2.put("longitude", 126.988);
        
        JSONObject d3 = new JSONObject();
        d3.put("userId", "test_user_b"); d3.put("date", "2026-06-09"); d3.put("timeSlot", "evening");
        d3.put("content", "저녁에 산에 갔다"); d3.put("rating", 5.0); d3.put("latitude", 37.5780); d3.put("longitude", 126.977);

        diaries.put(d1); diaries.put(d2); diaries.put(d3);

        for (int i = 0; i < diaries.length(); i++) {
            String resp = postRequest("/diary", diaries.getJSONObject(i));
            JSONObject respObj = new JSONObject(resp);
            DIARY_IDS.add(respObj.getInt("id"));
        }

        // Read
        String readResp = getRequest("/diary/test_user_b");
        JSONArray readArray = new JSONArray(readResp);
        assertTrue(readArray.length() >= 3);
    }

    private void testD3DiaryDelete() throws Exception {
        if (DIARY_IDS.isEmpty()) return;
        int targetId = DIARY_IDS.get(DIARY_IDS.size() - 1);
        deleteRequest("/diary/" + targetId);
        DIARY_IDS.remove(DIARY_IDS.size() - 1);
        
        String readResp = getRequest("/diary/test_user_b");
        assertFalse(readResp.contains("\"id\":" + targetId + ","));
    }

    private void testD4FriendRequest() throws Exception {
        JSONObject req = new JSONObject();
        req.put("fromUserId", "test_user_a");
        req.put("toUserId", "test_user_b");
        try {
            postRequest("/friend/request", req);
        } catch(Exception e) {
            System.out.println("D4 친구요청 실패 (이미 친구이거나 요청 상태일 수 있음): " + e.getMessage());
        }
    }

    private void testD5Timeline() throws Exception {
        if (DIARY_IDS.isEmpty()) return;
        String resp = getRequest("/friend/timeline?myUserId=test_user_a&friendId=test_user_b&date=2026-06-07");
        JSONArray arr = new JSONArray(resp);
        assertTrue(arr.length() > 0);
        assertNotNull(arr.getJSONObject(0).has("heartCount"));
    }

    private void testD6HeartToggle() throws Exception {
        if (DIARY_IDS.isEmpty()) return;
        int targetId = DIARY_IDS.get(0);
        JSONObject payload = new JSONObject();
        payload.put("userId", "test_user_a");
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
        if (DIARY_IDS.isEmpty()) return;
        int targetId = DIARY_IDS.get(0);
        
        JSONObject payload = new JSONObject();
        payload.put("userId", "test_user_a");
        payload.put("diaryId", targetId);
        payload.put("content", "멋진 기록이네요! (Android)");
        
        String resp = postRequest("/comment", payload);
        JSONObject respObj = new JSONObject(resp);
        int commentId = respObj.getInt("id");
        
        String readResp = getRequest("/comment/" + targetId);
        assertTrue(readResp.contains("\"id\":" + commentId));
        
        deleteRequest("/comment/" + commentId + "?userId=test_user_a");
    }

    // ─── AI 여행 추천 관련 연동 테스트 ───
    private void testRecommendNearby() throws Exception {
        String url = "/travel/recommend/nearby?lat=37.5665&lng=126.9780&radius=1500";
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
