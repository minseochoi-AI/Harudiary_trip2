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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 여행 파이프라인 전용 통합 E2E 테스트
 * 1. AI 여행 계획 검색 Fallback 검증
 * 2. 동일 날짜 여행 계획 저장/수정 검증
 * 3. 여행 완료 기록(역방향 흐름) 검증
 * 4. 친구의 계획 및 완료 기록 공유/조회 검증
 * 5. 여행 완료 기록에 대한 좋아요 공유(반응) 검증
 */
@RunWith(AndroidJUnit4.class)
public class TravelFlowE2EInstrumentedTest {

    private static final String BASE_URL = "http://133.186.143.108:8083/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private OkHttpClient client;
    private static Integer createdDiaryId = null;

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
    public void runAllTravelFlowTests() throws Exception {
        // 테스트 선행조건: 기본 계정 등록 (에러 발생해도 무시, 이미 있으면 통과)
        setupTestUsers();

        // [테스트 1] AI 여행 계획 검색 Fallback 검증
        JSONObject planResp = testFallbackRecommend();

        // [테스트 2] 동일 날짜 여행 계획 저장/수정 검증
        testSavePlanDuplicateUpdate(planResp);

        // [테스트 3] 여행 완료 기록(역방향 흐름) 검증
        testTravelCompletionRecord();

        // [테스트 4] 친구의 계획 및 완료 기록 공유/조회 검증
        testFriendTimelineShare();

        // [테스트 5] 여행 완료 기록에 대한 좋아요 공유(반응) 검증
        testReactionShare();
    }

    private void setupTestUsers() {
        // Seed 데이터가 존재하므로 유저 생성(register) 단계를 생략합니다.
    }

    private JSONObject testFallbackRecommend() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("targetDate", "2026-06-20");
        payload.put("days", 1);
        // 고의로 lat/lng을 0으로 설정하거나 위치 정보가 부족한 상황 모사
        payload.put("lat", 0.0);
        payload.put("lng", 0.0);
        // GPS 없이 텍스트 내 키워드로 백엔드 Fallback 동작 유도
        payload.put("diaryText", "도쿄 맛집 가고 싶다");

        String resp = postRequest("/travel/recommend/diary", payload);
        JSONObject respObj = new JSONObject(resp);
        
        // Fallback 로직을 타서 정상적으로 플랜을 반환하는지 검증
        assertTrue("Fallback 결과로 trip_title이 존재해야 합니다.", respObj.has("trip_title"));
        return respObj;
    }

    private void testSavePlanDuplicateUpdate(JSONObject planResp) throws Exception {
        String testDate = "2026-06-20";
        // 첫 번째 저장 (date: 2026-06-20)
        String url = "/travel/plan/save?userId=seed_user_A&date=" + testDate;
        postRequest(url, planResp);

        // 동일 날짜에 실제 기록(Record: isPlan=false) 저장 (Double Storage 모사)
        JSONObject recordPayload = new JSONObject();
        recordPayload.put("userId", "seed_user_A");
        recordPayload.put("date", testDate);
        recordPayload.put("timeSlot", "evening");
        recordPayload.put("content", "도쿄 도착해서 숙소 체크인 완료!");
        recordPayload.put("rating", 4.0);
        postRequest("/diary", recordPayload);

        // 두 번째 저장 (date: 2026-06-20, 제목 변경 모사하여 덮어쓰기 검증)
        // 이때 기록(isPlan=false)이 있어도 충돌 없이 계획(isPlan=true)만 수정되어야 함
        planResp.put("trip_title", "업데이트된 도쿄 여행 일정");
        postRequest(url, planResp);

        // GET /diary/seed_user_A 를 호출해서 날짜 2026-06-20가 2개인지, 
        // 계획 제목이 업데이트된 내용인지 검증
        String readResp = getRequest("/diary/seed_user_A");
        JSONArray diaries = new JSONArray(readResp);
        
        int count = 0;
        boolean titleUpdated = false;
        boolean hasRecord = false;
        for (int i = 0; i < diaries.length(); i++) {
            JSONObject d = diaries.getJSONObject(i);
            if (testDate.equals(d.optString("date"))) {
                count++;
                if (d.optBoolean("isPlan", false)) {
                    if ("업데이트된 도쿄 여행 일정".equals(d.optString("title"))) {
                        titleUpdated = true;
                    }
                } else {
                    hasRecord = true;
                }
            }
        }
        
        assertEquals("동일 날짜에 계획 1개, 기록 1개로 총 2개가 공존해야 합니다.", 2, count);
        assertTrue("기존 계획 데이터가 새로운 계획으로 덮어씌워져야(Update) 합니다.", titleUpdated);
        assertTrue("실제 일기(Record) 데이터도 정상 보존되어야 합니다.", hasRecord);
    }

    private void testTravelCompletionRecord() throws Exception {
        // 역방향: PlaceDto 정보를 기반으로 실제 Diary(방문 완료 기록) 생성
        JSONObject diaryPayload = new JSONObject();
        diaryPayload.put("userId", "seed_user_A");
        diaryPayload.put("date", "2026-06-21"); // 완료 기록은 다음 날로 모사
        diaryPayload.put("timeSlot", "lunch");
        diaryPayload.put("content", "도쿄 맛집 추천받아서 직접 왔는데 최고였다!");
        diaryPayload.put("rating", 5.0);
        
        // PlaceDto 로부터 pre-fill 된 좌표/주소 데이터
        diaryPayload.put("address", "도쿄도 시부야구 스시집");
        diaryPayload.put("latitude", 35.6580);
        diaryPayload.put("longitude", 139.7016);

        String resp = postRequest("/diary", diaryPayload);
        JSONObject respObj = new JSONObject(resp);
        
        // 생성 성공 및 ID 확보
        assertTrue("방문 완료 기록이 정상 생성되어 id가 반환되어야 합니다.", respObj.has("id"));
        createdDiaryId = respObj.getInt("id"); // 다음 테스트(좋아요 등)를 위해 ID 보존
    }

    private void testFriendTimelineShare() throws Exception {
        assertNotNull("이전 단계에서 생성된 기록(다이어리) ID가 있어야 합니다.", createdDiaryId);

        // 1. 친구 관계 성립 모사 (이미 되어있을 수도 있으니 에러 무시)
        JSONObject req = new JSONObject();
        req.put("fromUserId", "seed_user_B");
        req.put("toUserId", "seed_user_A");
        try { postRequest("/friend/request", req); } catch(Exception ignored) {}

        // 2. 친구(seed_user_B)가 유저(seed_user_A)의 타임라인을 조회
        String url = "/friend/timeline?myUserId=seed_user_B&friendId=seed_user_A&date=2026-06-21";
        String resp = getRequest(url);
        JSONArray arr = new JSONArray(resp);
        
        assertTrue("친구 타임라인 조회가 성공해야 합니다.", arr.length() > 0);
        
        boolean found = false;
        for (int i=0; i<arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            if (obj.optInt("id") == createdDiaryId) {
                found = true;
                break;
            }
        }
        assertTrue("친구 타임라인에서 방금 생성한 여행 완료 기록(플랜)을 볼 수 있어야 합니다.", found);
    }

    private void testReactionShare() throws Exception {
        assertNotNull("이전 단계에서 생성된 다이어리가 있어야 합니다.", createdDiaryId);

        JSONObject payload = new JSONObject();
        payload.put("userId", "seed_user_B");
        payload.put("diaryId", createdDiaryId);
        
        // 좋아요(Heart) Toggle ON
        postRequest("/reaction/toggle", payload);

        // 타임라인 재조회를 통해 하트(좋아요) 개수가 반영되었는지 검증
        String url = "/friend/timeline?myUserId=seed_user_B&friendId=seed_user_A&date=2026-06-21";
        String resp = getRequest(url);
        JSONArray arr = new JSONArray(resp);
        
        boolean hasHeart = false;
        for (int i=0; i<arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            if (obj.optInt("id") == createdDiaryId) {
                int heartCount = obj.optInt("heartCount", 0);
                if (heartCount > 0) hasHeart = true;
            }
        }
        assertTrue("좋아요를 누른 후 heartCount가 정상적으로 반영 및 공유되어야 합니다.", hasHeart);
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
}
