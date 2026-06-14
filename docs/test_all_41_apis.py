import requests
import json
import datetime
import sys
import time
import random

LOG_FILE = '/home/ubuntu/root/base/log/api_41_test.log'
BASE = "http://localhost:8083/api"

def write_json_log(scenario, request_data, response_data, error=None):
    log_entry = {
        "timestamp": datetime.datetime.now().isoformat(),
        "scenario": scenario,
        "request": request_data,
        "response": response_data
    }
    if error:
        log_entry["error"] = str(error)
        
    with open(LOG_FILE, "a", encoding="utf-8") as f:
        f.write(json.dumps(log_entry, ensure_ascii=False) + "\n")

def check(condition, message):
    if not condition:
        raise AssertionError(message)

# =======================================================
# Helpers
# =======================================================
def register_user(user_id, nickname):
    payload = {"id": user_id, "nickname": nickname}
    try:
        r = requests.post(f"{BASE}/user/register", json=payload, timeout=5)
    except:
        pass

def test_api(method, url, scenario, expected_status=200, **kwargs):
    print(f"  [Testing] {scenario}: {method} {url}")
    timeout = kwargs.pop('timeout', 10)
    try:
        r = requests.request(method, url, timeout=timeout, **kwargs)
        r.raise_for_status()
        write_json_log(scenario, kwargs, r.text)
        print(f"  ✅ {scenario} OK")
        return True, r
    except Exception as e:
        write_json_log(scenario, kwargs, None, error=str(e))
        print(f"  ❌ {scenario} Failed: {e}")
        return False, None

# =======================================================
# Test Suites
# =======================================================

def test_diary_stats_e2e(user_a):
    print("\n[Part 1: DiaryStatsE2E (6 APIs)]")
    results = {}
    
    # 1. POST /diary
    payload = {"userId": user_a, "date": "2026-06-25", "timeSlot": "morning", "content": "Test Content", "rating": 4.0}
    res, _ = test_api("POST", f"{BASE}/diary", "1_setupUserAndDiary_POST_diary", json=payload)
    results["1"] = res
    
    # 2. POST /travel/plan/save
    plan_payload = {"trip_title": "Test Trip", "days": []}
    res, _ = test_api("POST", f"{BASE}/travel/plan/save?userId={user_a}&date=2026-06-26", "2_setupUserAndDiary_POST_travel_plan", json=plan_payload)
    results["2"] = res
    
    # 3. GET /env/current
    res, _ = test_api("GET", f"{BASE}/env/current?lat=37.5&lng=127.0", "3_testEnvCurrent_GET_env")
    results["3"] = res
    
    # 4. GET /diary/{userId}/count
    res, _ = test_api("GET", f"{BASE}/diary/{user_a}/count?yearMonth=2026-06", "4_testDiaryCount_GET_count")
    results["4"] = res
    
    # 5. GET /diary/{userId}/streak
    res, _ = test_api("GET", f"{BASE}/diary/{user_a}/streak", "5_testDiaryStreak_GET_streak")
    results["5"] = res
    
    # 6. GET /diary/{userId}/date/{date}
    res, _ = test_api("GET", f"{BASE}/diary/{user_a}/date/2026-06-25", "6_testDiaryByDate_GET_date")
    results["6"] = res
    
    return results

def test_friend_management_e2e(user_a, user_b, user_c):
    print("\n[Part 2: FriendManagementE2E (15 APIs)]")
    results = {}
    
    # 7. GET /friend/search
    res, _ = test_api("GET", f"{BASE}/friend/search?userId={user_a}&query=B", "7_testFriendSearchAndBrowse_GET_search")
    results["7"] = res
    
    # 8. GET /friend/browse/{userId}
    res, _ = test_api("GET", f"{BASE}/friend/browse/{user_a}", "8_testFriendSearchAndBrowse_GET_browse")
    results["8"] = res
    
    # 9. POST /friend/request
    res, _ = test_api("POST", f"{BASE}/friend/request", "9_testFriendRequestAndStatus_POST_request", json={"fromUserId": user_c, "toUserId": user_a})
    results["9"] = res
    
    # 10. GET /friend/status
    res, _ = test_api("GET", f"{BASE}/friend/status?userId={user_c}&friendId={user_a}", "10_testFriendRequestAndStatus_GET_status")
    results["10"] = res
    
    # 11. GET /friend/pending-count/{userId}
    res, _ = test_api("GET", f"{BASE}/friend/pending-count/{user_a}", "11_testFriendRequestAndStatus_GET_pending")
    results["11"] = res
    
    # 12. GET /friend/requests/{userId}
    res, r = test_api("GET", f"{BASE}/friend/requests/{user_a}", "12_testFriendRequestAndStatus_GET_requests")
    results["12"] = res
    
    req_id = None
    if r and r.json():
        req_id = r.json()[0].get("id")
    
    # 13. POST /friend/reject/{requestId}
    if req_id:
        res, _ = test_api("POST", f"{BASE}/friend/reject/{req_id}", "13_testFriendReject_POST_reject")
        results["13"] = res
    else:
        print("  ⚠️ Skipped 13 due to missing req_id")
        results["13"] = False

    # 14. POST /friend/request (again)
    res, _ = test_api("POST", f"{BASE}/friend/request", "14_testFriendListDatesAndDelete_POST_request", json={"fromUserId": user_c, "toUserId": user_a})
    results["14"] = res
    
    # 15. GET /friend/requests/{userId} (again)
    res, r = test_api("GET", f"{BASE}/friend/requests/{user_a}", "15_testFriendListDatesAndDelete_GET_requests")
    results["15"] = res
    
    if r and r.json():
        req_id = r.json()[0].get("id")
        
    # 16. POST /friend/accept/{requestId}
    if req_id:
        res, _ = test_api("POST", f"{BASE}/friend/accept/{req_id}", "16_testFriendListDatesAndDelete_POST_accept")
        results["16"] = res
    else:
        print("  ⚠️ Skipped 16 due to missing req_id")
        results["16"] = False
        
    # 17. GET /friend/list/{userId}
    res, _ = test_api("GET", f"{BASE}/friend/list/{user_a}", "17_testFriendListDatesAndDelete_GET_list")
    results["17"] = res
    
    # 18. POST /diary (Duplicate logic test)
    res, _ = test_api("POST", f"{BASE}/diary", "18_testFriendListDatesAndDelete_POST_diary", json={"userId": user_c, "date": "2026-06-25", "timeSlot": "morning", "content": "Friend Test", "rating": 4.0})
    results["18"] = res
    
    # 19. POST /travel/plan/save (Duplicate logic test)
    res, _ = test_api("POST", f"{BASE}/travel/plan/save?userId={user_c}&date=2026-06-26", "19_testFriendListDatesAndDelete_POST_plan", json={"tripTitle": "Friend Plan", "days": []})
    results["19"] = res
    
    # 20. GET /friend/dates/{friendId}
    res, _ = test_api("GET", f"{BASE}/friend/dates/{user_c}", "20_testFriendListDatesAndDelete_GET_dates")
    results["20"] = res
    
    # 21. DELETE /friend/delete
    res, _ = test_api("DELETE", f"{BASE}/friend/delete?userId={user_a}&friendId={user_c}", "21_testFriendListDatesAndDelete_DELETE_friend")
    results["21"] = res
    
    return results

def test_pipeline_e2e(user_a, user_b):
    print("\n[Part 3: PipelineE2E (12 APIs)]")
    results = {}
    
    # 22. POST /user/login (Register skipped as logic, but test login)
    res, _ = test_api("POST", f"{BASE}/user/login", "22_testD1RegisterLogin_POST_login", json={"id": user_a, "nickname": "TestA"})
    results["22"] = res
    
    # 23. GET /diary/{userId}
    res, _ = test_api("GET", f"{BASE}/diary/{user_b}", "23_testD2DiaryCreateRead_GET_diary")
    results["23"] = res
    
    # Prep diary for delete
    r = requests.post(f"{BASE}/diary", json={"userId": user_b, "date": "2026-06-08", "content": "Delete me", "rating": 5.0})
    diary_id = r.json().get("id") or r.json().get("activityId") if r and r.json() else 1
    
    # 24. DELETE /diary/{diaryId}
    res, _ = test_api("DELETE", f"{BASE}/diary/{diary_id}?userId={user_b}", "24_testD3DiaryDelete_DELETE_diary")
    results["24"] = res
    
    # 25. POST /friend/request
    res, _ = test_api("POST", f"{BASE}/friend/request", "25_testD4FriendRequest_POST_request", json={"fromUserId": user_a, "toUserId": user_b})
    results["25"] = res
    
    # Auto-accept friend request for subsequent reaction/comment tests
    r_reqs = requests.get(f"{BASE}/friend/requests/{user_b}")
    if r_reqs.json():
        req_id = r_reqs.json()[0].get("id")
        requests.post(f"{BASE}/friend/accept/{req_id}")
    
    # 26. GET /friend/timeline
    res, _ = test_api("GET", f"{BASE}/friend/timeline?myUserId={user_a}&friendId={user_b}&date=2026-06-07", "26_testD5Timeline_GET_timeline")
    results["26"] = res
    
    # Create a NEW diary for reaction and comment tests (since the previous one was deleted)
    r2 = requests.post(f"{BASE}/diary", json={"userId": user_b, "date": "2026-06-09", "content": "Keep me", "rating": 5.0})
    active_diary_id = r2.json().get("activityId") or r2.json().get("id") if r2 and r2.status_code == 200 else 1
    
    # 27. POST /reaction/toggle
    res, _ = test_api("POST", f"{BASE}/reaction/toggle", "27_testD6HeartToggle_POST_reaction", json={"userId": user_a, "diaryId": active_diary_id})
    results["27"] = res
    
    # 28. GET /friend/dates/{friendId}
    res, _ = test_api("GET", f"{BASE}/friend/dates/{user_b}", "28_testD7Dates_GET_dates")
    results["28"] = res
    
    # 29. POST /comment
    res, r = test_api("POST", f"{BASE}/comment", "29_testD8CommentCrud_POST_comment", json={"userId": user_a, "diaryId": active_diary_id, "content": "hello"})
    results["29"] = res
    comment_id = None
    if r:
        try:
            data = r.json()
            comment_id = data.get("id") or data.get("commentId")
        except:
            pass
    
    # 30. GET /comment/{diaryId}
    res, _ = test_api("GET", f"{BASE}/comment/{active_diary_id}", "30_testD8CommentCrud_GET_comment")
    results["30"] = res
    
    # 31. DELETE /comment/{commentId}
    if comment_id:
        res, _ = test_api("DELETE", f"{BASE}/comment/{comment_id}?userId={user_a}", "31_testD8CommentCrud_DELETE_comment")
        results["31"] = res
    else:
        print("  ⚠️ Skipped 31 due to missing comment_id")
        results["31"] = False
        
    # 32. POST /travel/recommend/nearby (Notice: table listed diary, but keeping nearby if there)
    # The table listed POST /travel/recommend/diary twice. We'll just test diary recommend.
    payload = {"targetDate": "2026-06-20", "days": 1, "lat": 0.0, "lng": 0.0, "diaryText": "Test"}
    res, _ = test_api("POST", f"{BASE}/travel/recommend/diary", "32_testRecommendNearby_POST_recommend", json=payload, timeout=30)
    results["32"] = res
    
    # 33. POST /travel/recommend/diary
    res, _ = test_api("POST", f"{BASE}/travel/recommend/diary", "33_testRecommendByDiary_POST_recommend", json=payload, timeout=30)
    results["33"] = res
    
    return results

def test_travel_flow_e2e(user_flow, friend_flow):
    print("\n[Part 4: TravelFlowE2E (8 APIs)]")
    results = {}
    
    payload = {"targetDate": "2026-06-20", "days": 1, "lat": 0.0, "lng": 0.0, "diaryText": "도쿄"}
    
    # 34. POST /travel/recommend/diary
    res, r = test_api("POST", f"{BASE}/travel/recommend/diary", "34_testFallbackRecommend_POST_recommend", json=payload, timeout=30)
    results["34"] = res
    plan_resp = {"trip_title": "Test", "days": []}
    if r and r.status_code == 200:
        try:
            plan_resp = r.json()
        except:
            pass
    
    # 35. POST /travel/plan/save
    res, _ = test_api("POST", f"{BASE}/travel/plan/save?userId={user_flow}&date=2026-06-20", "35_testSavePlanDuplicateUpdate_POST_plan", json=plan_resp)
    results["35"] = res
    
    # 36. POST /diary
    res, _ = test_api("POST", f"{BASE}/diary", "36_testSavePlanDuplicateUpdate_POST_diary", json={"userId": user_flow, "date": "2026-06-20", "timeSlot": "evening", "content": "도쿄 도착", "rating": 4.0})
    results["36"] = res
    
    # 37. GET /diary/{userId}
    res, _ = test_api("GET", f"{BASE}/diary/{user_flow}", "37_testSavePlanDuplicateUpdate_GET_diary")
    results["37"] = res
    
    # 38. POST /diary (with trip_title)
    res, r = test_api("POST", f"{BASE}/diary", "38_testTravelCompletionRecord_POST_diary", json={"userId": user_flow, "date": "2026-06-21", "timeSlot": "lunch", "content": "완료", "rating": 5.0, "trip_title": "도쿄 여행"})
    results["38"] = res
    diary_id = 1
    if r:
        try:
            data = r.json()
            diary_id = data.get("activityId") or data.get("id") or 1
        except:
            pass
    
    # 39. POST /friend/request
    res, _ = test_api("POST", f"{BASE}/friend/request", "39_testFriendTimelineShare_POST_request", json={"fromUserId": friend_flow, "toUserId": user_flow})
    results["39"] = res
    
    # Auto-accept friend request
    r_reqs = requests.get(f"{BASE}/friend/requests/{user_flow}")
    if r_reqs.json():
        req_id = r_reqs.json()[0].get("id")
        requests.post(f"{BASE}/friend/accept/{req_id}")
    
    # 40. GET /friend/timeline
    res, _ = test_api("GET", f"{BASE}/friend/timeline?myUserId={friend_flow}&friendId={user_flow}&date=2026-06-21", "40_testFriendTimelineShare_GET_timeline")
    results["40"] = res
    
    # 41. POST /reaction/toggle
    res, _ = test_api("POST", f"{BASE}/reaction/toggle", "41_testFriendTimelineShare_POST_reaction", json={"userId": friend_flow, "diaryId": diary_id})
    results["41"] = res
    
    return results

if __name__ == "__main__":
    print("=" * 60)
    print("  Harudiary 41 APIs Comprehensive Test Suite")
    print(f"  Log File: {LOG_FILE}")
    print("=" * 60)
    
    # Initialize Log
    import os
    os.makedirs(os.path.dirname(LOG_FILE), exist_ok=True)
    with open(LOG_FILE, "w", encoding="utf-8") as f:
        f.write("")
        
    ts = int(time.time())
    uA = f"p_userA_{ts}"
    uB = f"p_userB_{ts}"
    uC = f"p_userC_{ts}"
    uF = f"p_userF_{ts}"
    uFF = f"p_userFF_{ts}"
    
    # Pre-register users to avoid 404s
    for u in [uA, uB, uC, uF, uFF]:
        register_user(u, f"User_{u}")
        
    r1 = test_diary_stats_e2e(uA)
    r2 = test_friend_management_e2e(uA, uB, uC)
    r3 = test_pipeline_e2e(uA, uB)
    r4 = test_travel_flow_e2e(uF, uFF)
    
    all_res = {**r1, **r2, **r3, **r4}
    passed = sum(1 for v in all_res.values() if v)
    total = len(all_res)
    
    print("\n" + "=" * 60)
    print("  RESULTS SUMMARY (41 APIs)")
    print("=" * 60)
    for k, v in all_res.items():
        print(f"  API #{k}: {'✅ PASS' if v else '❌ FAIL'}")
    print(f"\n  Total: {passed}/{total} APIs passed")
    print("=" * 60)
