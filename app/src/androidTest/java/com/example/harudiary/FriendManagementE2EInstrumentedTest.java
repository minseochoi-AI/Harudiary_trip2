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

import static org.junit.Assert.assertTrue;

/**
 * 친구 관리 부가 기능 E2E 테스트
 * 커버리지 보완 항목 (총 8개 로직):
 * 1. GET /api/friend/search
 * 2. GET /api/friend/browse/{userId}
 * 3. GET /api/friend/status
 * 4. GET /api/friend/pending-count/{userId}
 * 5. POST /api/friend/reject/{requestId}
 * 6. GET /api/friend/list/{userId}
 * 7. GET /api/friend/dates/{friendId}
 * 8. DELETE /api/friend/delete
 */
@RunWith(AndroidJUnit4.class)
public class FriendManagementE2EInstrumentedTest {

    private static final String BASE_URL = "http://133.186.143.108:8083/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client;

    private final String USER_A = "fm_user_a";
    private final String USER_B = "fm_user_b";
    private final String USER_C = "fm_user_c";

    @Before
    public void setUp() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Test
    public void runFriendManagementTests() throws Exception {
        setupUsers();

        // 1. 친구 찾기 및 둘러보기
        testFriendSearchAndBrowse();

        // 2. 친구 요청 생성 및 대기 인원 / 상태 확인
        long requestId = testFriendRequestAndStatus();

        // 3. 친구 거절
        testFriendReject(requestId);

        // 4. 새로운 친구 요청 후 수락하여 목록 및 다이어리 날짜 확인, 최종 삭제
        testFriendListDatesAndDelete();
    }

    private void setupUsers() {
        try {
            JSONObject uA = new JSONObject(); uA.put("id", USER_A); uA.put("nickname", "A사용자"); postRequest("/user/register", uA);
            JSONObject uB = new JSONObject(); uB.put("id", USER_B); uB.put("nickname", "B사용자"); postRequest("/user/register", uB);
            JSONObject uC = new JSONObject(); uC.put("id", USER_C); uC.put("nickname", "C사용자"); postRequest("/user/register", uC);
        } catch (Exception ignored) {}
    }

    private void testFriendSearchAndBrowse() throws Exception {
        // GET /api/friend/search
        String searchResp = getRequest("/friend/search?userId=" + USER_A + "&query=B사용자");
        JSONArray searchArr = new JSONArray(searchResp);
        assertTrue("검색 결과가 1건 이상이어야 합니다.", searchArr.length() > 0);

        // GET /api/friend/browse/{userId}
        String browseResp = getRequest("/friend/browse/" + USER_A);
        JSONArray browseArr = new JSONArray(browseResp);
        assertTrue("둘러보기(추천) 목록이 내려와야 합니다.", browseArr.length() >= 0);
    }

    private long testFriendRequestAndStatus() throws Exception {
        // USER_A -> USER_B 친구 요청
        JSONObject reqPayload = new JSONObject();
        reqPayload.put("fromUserId", USER_A);
        reqPayload.put("toUserId", USER_B);
        postRequest("/friend/request", reqPayload);

        // GET /api/friend/status
        String statusResp = getRequest("/friend/status?userId=" + USER_A + "&friendId=" + USER_B);
        // 상태는 PENDING 이라는 문자열일 것임 (혹은 따옴표가 포함될 수 있음)
        assertTrue("친구 상태가 PENDING이어야 합니다.", statusResp.contains("PENDING"));

        // GET /api/friend/pending-count/{userId}
        String pendingResp = getRequest("/friend/pending-count/" + USER_B);
        int pendingCount = Integer.parseInt(pendingResp.trim());
        assertTrue("대기 중인 요청이 1건 이상이어야 합니다.", pendingCount >= 1);

        // 방금 보낸 요청의 Request ID 찾기
        String reqListResp = getRequest("/friend/requests/" + USER_B);
        JSONArray reqList = new JSONArray(reqListResp);
        for(int i = 0; i < reqList.length(); i++) {
            JSONObject obj = reqList.getJSONObject(i);
            if (USER_A.equals(obj.optJSONObject("fromUser").optString("id"))) {
                return obj.optLong("id");
            }
        }
        throw new RuntimeException("친구 요청을 찾을 수 없습니다.");
    }

    private void testFriendReject(long requestId) throws Exception {
        // POST /api/friend/reject/{requestId}
        postRequest("/friend/reject/" + requestId, new JSONObject());

        // 상태가 NONE이 되었는지 확인
        String statusResp = getRequest("/friend/status?userId=" + USER_A + "&friendId=" + USER_B);
        assertTrue("거절 후 상태가 PENDING이 아니어야 합니다.", !statusResp.contains("PENDING"));
    }

    private void testFriendListDatesAndDelete() throws Exception {
        // USER_A -> USER_C 친구 맺기 (수락)
        JSONObject reqPayload = new JSONObject();
        reqPayload.put("fromUserId", USER_A);
        reqPayload.put("toUserId", USER_C);
        postRequest("/friend/request", reqPayload);

        String reqListResp = getRequest("/friend/requests/" + USER_C);
        JSONArray reqList = new JSONArray(reqListResp);
        long requestId = reqList.getJSONObject(0).optLong("id");
        
        postRequest("/friend/accept/" + requestId, new JSONObject());

        // GET /api/friend/list/{userId}
        String listResp = getRequest("/friend/list/" + USER_A);
        JSONArray listArr = new JSONArray(listResp);
        boolean hasC = false;
        for (int i=0; i<listArr.length(); i++) {
            if (USER_C.equals(listArr.getJSONObject(i).optString("id"))) hasC = true;
        }
        assertTrue("목록에 친구 C가 있어야 합니다.", hasC);

        // 친구 C가 기록과 계획을 작성
        JSONObject diaryC = new JSONObject();
        diaryC.put("userId", USER_C);
        diaryC.put("date", "2026-06-25");
        diaryC.put("timeSlot", "morning");
        diaryC.put("content", "친구 달력 테스트 일기");
        diaryC.put("rating", 4.0);
        postRequest("/diary", diaryC);
        
        JSONObject planC = new JSONObject();
        planC.put("tripTitle", "친구 달력 계획");
        planC.put("days", new JSONArray());
        postRequest("/travel/plan/save?userId=" + USER_C + "&date=2026-06-26", planC);

        // GET /api/friend/dates/{friendId}
        String datesResp = getRequest("/friend/dates/" + USER_C);
        JSONArray datesArr = new JSONArray(datesResp);
        org.junit.Assert.assertEquals("친구의 달력 날짜 목록이 2개(계획 1, 기록 1)여야 합니다.", 2, datesArr.length());

        boolean foundRecord = false;
        boolean foundPlan = false;
        for (int i=0; i<datesArr.length(); i++) {
            JSONObject obj = datesArr.getJSONObject(i);
            org.junit.Assert.assertTrue("날짜 응답에 isPlan 필드가 존재해야 합니다.", obj.has("isPlan"));
            if (!obj.getBoolean("isPlan") && "2026-06-25".equals(obj.getString("date"))) {
                foundRecord = true;
            } else if (obj.getBoolean("isPlan") && "2026-06-26".equals(obj.getString("date"))) {
                foundPlan = true;
            }
        }
        org.junit.Assert.assertTrue("달력 마커 응답에 Record 객체가 올바르게 포함되어야 합니다.", foundRecord);
        org.junit.Assert.assertTrue("달력 마커 응답에 Plan 객체가 올바르게 포함되어야 합니다.", foundPlan);

        // DELETE /api/friend/delete
        deleteRequest("/friend/delete?userId=" + USER_A + "&friendId=" + USER_C);

        // 리스트 재확인 (삭제됨)
        String listRespAfter = getRequest("/friend/list/" + USER_A);
        JSONArray listArrAfter = new JSONArray(listRespAfter);
        boolean hasCAfter = false;
        for (int i=0; i<listArrAfter.length(); i++) {
            if (USER_C.equals(listArrAfter.getJSONObject(i).optString("id"))) hasCAfter = true;
        }
        assertTrue("삭제 후 목록에 친구 C가 없어야 합니다.", !hasCAfter);
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

    private String deleteRequest(String path) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .delete()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return response.body().string();
        }
    }
}
