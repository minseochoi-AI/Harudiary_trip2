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

/**
 * 다이어리 통계 및 부가 기능 (Env) E2E 테스트
 * 커버리지 보완 항목:
 * 1. GET /api/env/current
 * 2. GET /api/diary/{userId}/count
 * 3. GET /api/diary/{userId}/streak
 * 4. GET /api/diary/{userId}/date/{date}
 */
@RunWith(AndroidJUnit4.class)
public class DiaryStatsE2EInstrumentedTest {

    private static final String BASE_URL = "http://133.186.143.108:8083/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client;
    private String TEST_USER;

    @Before
    public void setUp() {
        TEST_USER = "test_stats_" + System.currentTimeMillis();
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Test
    public void runDiaryStatsTests() throws Exception {
        setupUserAndDiary();

        testEnvCurrent();
        testDiaryCount();
        testDiaryStreak();
        testDiaryByDate();
    }

    private void setupUserAndDiary() {
        try {
            JSONObject reg = new JSONObject();
            reg.put("id", TEST_USER);
            reg.put("nickname", "StatsUser");
            postRequest("/user/register", reg);
            
            // 테스트용 일기(Record, isPlan=false) 하나 작성
            JSONObject diary = new JSONObject();
            diary.put("userId", TEST_USER);
            diary.put("date", "2026-06-25");
            diary.put("timeSlot", "morning");
            diary.put("content", "통계 테스트용 일기");
            diary.put("rating", 4.0);
            postRequest("/diary", diary);

            // 계획(Plan, isPlan=true) 하나 작성 - 이것이 통계에 영향을 주지 않아야 함
            JSONObject travelPlan = new JSONObject();
            travelPlan.put("tripTitle", "통계 오염 테스트 여행");
            travelPlan.put("days", new JSONArray()); // 빈 일정
            
            // TravelPlan 저장 API 호출 (date=2026-06-26)
            postRequest("/travel/plan/save?userId=" + TEST_USER + "&date=2026-06-26", travelPlan);
        } catch (Exception ignored) {}
    }

    private void testEnvCurrent() throws Exception {
        String resp = getRequest("/env/current");
        // 환경 변수 응답이 정상적으로 반환되는지 (에러 없이 문자열이 오면 통과)
        assertNotNull("환경 변수 결과가 null이 아니어야 합니다.", resp);
    }

    private void testDiaryCount() throws Exception {
        String resp = getRequest("/diary/" + TEST_USER + "/count?yearMonth=2026-06");
        // count는 정수값이 반환되어야 함
        int count = Integer.parseInt(resp.trim());
        org.junit.Assert.assertEquals("다이어리 개수가 1개여야 합니다. (계획은 제외되어야 함)", 1, count);
    }

    private void testDiaryStreak() throws Exception {
        String resp = getRequest("/diary/" + TEST_USER + "/streak");
        int streak = Integer.parseInt(resp.trim());
        org.junit.Assert.assertEquals("연속 작성일이 1이어야 합니다. (계획은 제외)", 1, streak);
    }

    private void testDiaryByDate() throws Exception {
        String resp = getRequest("/diary/" + TEST_USER + "/date/2026-06-25");
        JSONArray arr = new JSONArray(resp);
        // 특정 날짜(2026-06-25)의 다이어리 배열이 제대로 내려오는지 확인
        assertTrue("해당 날짜에 작성한 다이어리가 있어야 합니다.", arr.length() > 0);
        assertTrue("다이어리 내용이 일치해야 합니다.", arr.getJSONObject(0).getString("content").contains("통계 테스트"));
    }

    // ─── 유틸리티 메서드 ───
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
}
