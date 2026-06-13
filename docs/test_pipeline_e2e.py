import requests
import json
import datetime
import sys
import time

LOG_FILE = '/home/ubuntu/root/base/log/pipeline_test.log'
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
        pass # Ignore already registered

def create_dummy_diary(user_id, date_str):
    d = {
        "userId": user_id, "date": date_str, "timeSlot": "morning",
        "content": f"더미 다이어리 {date_str}", "rating": 5.0
    }
    r = requests.post(f"{BASE}/diary", json=d, timeout=5)
    r.raise_for_status()
    data = r.json()
    return data.get("id") or data.get("activityId")

# =======================================================
# Part A: PipelineE2E (소셜/CRUD)
# =======================================================
def test_pipeline_crud():
    print("\n[Part A: PipelineE2E CRUD]")
    results = {}
    register_user("test_user_a", "유저A")
    register_user("test_user_b", "유저B")
    
    # D1: Login
    try:
        r = requests.post(f"{BASE}/user/login", json={"id": "test_user_a"}, timeout=5)
        r.raise_for_status()
        write_json_log("A1_login", {"id": "test_user_a"}, r.json())
        print("  ✅ A1 Login OK")
        results["A1"] = True
    except Exception as e:
        write_json_log("A1_login", {}, None, error=str(e))
        print(f"  ❌ A1 Failed: {e}")
        results["A1"] = False

    # D2: Diary Create/Read
    try:
        activity_id = create_dummy_diary("test_user_b", "2026-06-07")
        r = requests.get(f"{BASE}/diary/test_user_b", timeout=5)
        write_json_log("A2_diary_read", {}, r.json())
        check(len(r.json()) > 0, "No diaries found")
        print("  ✅ A2 Diary Create/Read OK")
        results["A2"] = True
    except Exception as e:
        write_json_log("A2_diary_read", {}, None, error=str(e))
        print(f"  ❌ A2 Failed: {e}")
        results["A2"] = False

    # D3: Diary Delete
    try:
        target_id = create_dummy_diary("test_user_b", "2026-06-08")
        r = requests.delete(f"{BASE}/diary/{target_id}", timeout=5)
        r.raise_for_status()
        r_read = requests.get(f"{BASE}/diary/test_user_b", timeout=5)
        check(str(target_id) not in r_read.text, "Diary still exists")
        write_json_log("A3_diary_delete", {"id": target_id}, {"status":"DELETED"})
        print("  ✅ A3 Diary Delete OK")
        results["A3"] = True
    except Exception as e:
        write_json_log("A3_diary_delete", {}, None, error=str(e))
        print(f"  ❌ A3 Failed: {e}")
        results["A3"] = False

    # D5: Timeline
    try:
        target_id = create_dummy_diary("test_user_b", "2026-06-09")
        r = requests.get(f"{BASE}/friend/timeline?myUserId=test_user_a&friendId=test_user_b&date=2026-06-09", timeout=5)
        r.raise_for_status()
        data = r.json()
        write_json_log("A5_timeline", {}, data)
        check(any(str(target_id) in str(item) for item in data), "Diary not in timeline")
        check("heartCount" in data[0], "No heartCount field")
        print("  ✅ A5 Timeline OK")
        results["A5"] = True
    except Exception as e:
        write_json_log("A5_timeline", {}, None, error=str(e))
        print(f"  ❌ A5 Failed: {e}")
        results["A5"] = False

    return results

# =======================================================
# Part B: TravelFlowE2E (여행 파이프라인 & 더블 스토리지)
# =======================================================
def test_travel_flow():
    print("\n[Part B: TravelFlowE2E]")
    results = {}
    ts = int(time.time())
    user_flow = f"test_user_flow_{ts}"
    friend_flow = f"test_friend_flow_{ts}"
    register_user(user_flow, "FlowUser")
    register_user(friend_flow, "FlowFriend")

    # C1: Fallback Recommend
    payload = {"targetDate":"2026-06-20", "days":1, "lat":0.0, "lng":0.0, "diaryText":"도쿄 가고싶다"}
    try:
        r = requests.post(f"{BASE}/travel/recommend/diary", json=payload, timeout=30)
        r.raise_for_status()
        plan_resp = r.json()
        write_json_log("B1_fallback_recommend", payload, plan_resp)
        check("trip_title" in plan_resp, "No trip_title")
        print("  ✅ B1 Fallback Recommend OK")
        results["B1"] = True
    except Exception as e:
        write_json_log("B1_fallback_recommend", payload, None, error=str(e))
        print(f"  ❌ B1 Failed: {e}")
        return results

    # C2: Save Plan Duplicate Update (Double Storage)
    try:
        # 1. Save plan
        r1 = requests.post(f"{BASE}/travel/plan/save?userId={user_flow}&date=2026-06-20", json=plan_resp, timeout=10)
        r1.raise_for_status()
        
        # 2. Save record on same date
        rec_payload = {"userId": user_flow, "date":"2026-06-20", "timeSlot":"evening", "content":"도쿄 도착!", "rating":4.0}
        requests.post(f"{BASE}/diary", json=rec_payload, timeout=5)
        
        # 3. Update plan
        plan_resp["trip_title"] = "업데이트된 도쿄 여행 일정"
        r3 = requests.post(f"{BASE}/travel/plan/save?userId={user_flow}&date=2026-06-20", json=plan_resp, timeout=10)
        r3.raise_for_status()

        # 4. Verify
        r4 = requests.get(f"{BASE}/diary/test_user_flow_{ts}", timeout=5)
        try:
            data = r4.json()
        except Exception as e:
            write_json_log("B2_double_storage_error", {}, r4.text, error=str(e))
            raise e
        write_json_log("B2_double_storage", {}, data)
        
        items = [d for d in data if d.get("date") == "2026-06-20"]
        check(len(items) >= 2, "Should have both plan and record")
        has_plan = any(d.get("isPlan") and d.get("title") == "업데이트된 도쿄 여행 일정" for d in items)
        has_record = any(not d.get("isPlan") for d in items)
        check(has_plan and has_record, "Double storage update failed")
        print("  ✅ B2 Double Storage Update OK")
        results["B2"] = True
    except Exception as e:
        write_json_log("B2_double_storage", {}, None, error=str(e))
        print(f"  ❌ B2 Failed: {e}")
        results["B2"] = False

    return results

# =======================================================
# Part C: FriendManagementE2E
# =======================================================
def test_friend_management():
    print("\n[Part C: FriendManagementE2E]")
    results = {}
    ts = int(time.time())
    ua = f"fm_user_a_{ts}"
    ub = f"fm_user_b_{ts}"
    uc = f"fm_user_c_{ts}"
    register_user(ua, "A사용자")
    register_user(ub, "B사용자")
    register_user(uc, "C사용자")

    # Friend Browse & Search
    try:
        r1 = requests.get(f"{BASE}/friend/search?userId={ua}&query=B사용자", timeout=5)
        r1.raise_for_status()
        check(len(r1.json()) > 0, "No search result")
        write_json_log("C1_friend_search", {}, r1.json())
        print("  ✅ C1 Search OK")
        results["C1"] = True
    except Exception as e:
        write_json_log("C1_friend_search", {}, None, str(e))
        results["C1"] = False

    # Friend Request & Status
    try:
        req_payload = {"fromUserId":ua, "toUserId":uc}
        requests.post(f"{BASE}/friend/request", json=req_payload, timeout=5)
        
        r_status = requests.get(f"{BASE}/friend/status?userId={ua}&friendId={uc}", timeout=5)
        check("PENDING" in r_status.text, "Status not PENDING")
        
        r_reqs = requests.get(f"{BASE}/friend/requests/{uc}", timeout=5)
        reqs = r_reqs.json()
        req_id = next((r["id"] for r in reqs if r["fromUser"]["id"] == ua), None)
        check(req_id is not None, "Request ID not found")
        
        # Accept
        requests.post(f"{BASE}/friend/accept/{req_id}", timeout=5)
        
        # Calendar Dates check (isPlan)
        requests.post(f"{BASE}/diary", json={"userId":uc, "date":"2026-06-25", "content":"C일기", "rating":4.0}, timeout=5)
        requests.post(f"{BASE}/travel/plan/save?userId={uc}&date=2026-06-26", json={"tripTitle":"C계획", "days":[]}, timeout=5)
        
        r_dates = requests.get(f"{BASE}/friend/dates/{uc}", timeout=5)
        dates = r_dates.json()
        write_json_log("C2_friend_dates", {}, dates)
        has_plan = any(d.get("isPlan") and d.get("date") == "2026-06-26" for d in dates)
        has_record = any(not d.get("isPlan") and d.get("date") == "2026-06-25" for d in dates)
        check(has_plan and has_record, "Dates marker separation failed")
        
        print("  ✅ C2 Request, Accept, Dates Marker OK")
        results["C2"] = True
    except Exception as e:
        write_json_log("C2_friend_flow", {}, None, str(e))
        print(f"  ❌ C2 Failed: {e}")
        results["C2"] = False

    return results

# =======================================================
# Part D: DiaryStatsE2E
# =======================================================
def test_diary_stats():
    print("\n[Part D: DiaryStatsE2E]")
    results = {}
    ts = int(time.time())
    TU = f"test_stats_user_{ts}"
    register_user(TU, "StatsUser")

    try:
        # Create record vs plan
        requests.post(f"{BASE}/diary", json={"userId":TU, "date":"2026-06-25", "content":"통계일기", "rating":4.0}, timeout=5)
        requests.post(f"{BASE}/travel/plan/save?userId={TU}&date=2026-06-26", json={"tripTitle":"통계계획", "days":[]}, timeout=5)
        
        # Env
        r_env = requests.get(f"{BASE}/env/current?lat=37.5665&lng=126.9780", timeout=5)
        check(r_env.status_code == 200, "Env endpoint failed")
        
        # Count (Must provide yearMonth)
        r_count = requests.get(f"{BASE}/diary/{TU}/count?yearMonth=2026-06", timeout=5)
        write_json_log("D1_stats_count", {}, r_count.text)
        count = int(r_count.text.strip())
        check(count >= 1, f"Count should be >=1, got {count}")
        
        # Streak
        r_streak = requests.get(f"{BASE}/diary/{TU}/streak", timeout=5)
        streak = int(r_streak.text.strip())
        check(streak >= 1, "Streak should be >= 1")
        
        print("  ✅ D1 Stats (Count, Streak, Env) OK")
        results["D1"] = True
    except Exception as e:
        write_json_log("D1_stats", {}, None, str(e))
        print(f"  ❌ D1 Failed: {e}")
        results["D1"] = False

    return results

if __name__ == "__main__":
    print("=" * 60)
    print("  Harudiary MEGA E2E Pipeline Test Suite (Python Version)")
    print(f"  Log File: {LOG_FILE}")
    print("=" * 60)
    
    # Initialize Log
    with open(LOG_FILE, "w", encoding="utf-8") as f:
        f.write("")
        
    rA = test_pipeline_crud()
    rB = test_travel_flow()
    rC = test_friend_management()
    rD = test_diary_stats()
    
    all_res = {**rA, **rB, **rC, **rD}
    passed = sum(1 for v in all_res.values() if v)
    total = len(all_res)
    
    print("\n" + "=" * 60)
    print("  RESULTS SUMMARY")
    print("=" * 60)
    for k, v in all_res.items():
        print(f"  {k}: {'✅ PASS' if v else '❌ FAIL'}")
    print(f"\n  Total: {passed}/{total} passed")
    print("=" * 60)
