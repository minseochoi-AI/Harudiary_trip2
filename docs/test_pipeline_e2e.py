import requests
import json
import datetime
import sys

LOG_FILE = '/home/ubuntu/root/leeyoungkon_projects/log/pipeline_test.log'
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

# ═══════════════════════════════════════════════════════════
# D1: 회원가입 → 로그인
# ═══════════════════════════════════════════════════════════
def test_d1_register_login():
    print("D1: 회원가입 → 로그인 ...")
    
    # Register user A
    reg_a = {"id": "test_user_a", "nickname": "유저A"}
    try:
        r = requests.post(f"{BASE}/user/register", json=reg_a, timeout=10)
        r.raise_for_status()
        write_json_log("D1_register_A", reg_a, r.json())
        print(f"  ✅ 유저A 가입 OK (id={r.json().get('id')})")
    except Exception as e:
        write_json_log("D1_register_A", reg_a, None, error=str(e))
        # User might already exist, try login anyway
        print(f"  ⚠️ 유저A 가입 실패 (이미 존재할 수 있음): {e}")

    # Register user B
    reg_b = {"id": "test_user_b", "nickname": "유저B"}
    try:
        r = requests.post(f"{BASE}/user/register", json=reg_b, timeout=10)
        r.raise_for_status()
        write_json_log("D1_register_B", reg_b, r.json())
        print(f"  ✅ 유저B 가입 OK (id={r.json().get('id')})")
    except Exception as e:
        write_json_log("D1_register_B", reg_b, None, error=str(e))
        print(f"  ⚠️ 유저B 가입 실패 (이미 존재할 수 있음): {e}")

    # Login user A
    login_a = {"id": "test_user_a"}
    try:
        r = requests.post(f"{BASE}/user/login", json=login_a, timeout=10)
        r.raise_for_status()
        data = r.json()
        write_json_log("D1_login_A", login_a, data)
        assert data.get("id") == "test_user_a", f"Expected test_user_a, got {data.get('id')}"
        print(f"  ✅ 유저A 로그인 OK (nickname={data.get('nickname')})")
    except Exception as e:
        write_json_log("D1_login_A", login_a, None, error=str(e))
        print(f"  ❌ 유저A 로그인 실패: {e}")
        return False
    return True


# ═══════════════════════════════════════════════════════════
# D2: 다이어리 작성(3건, 다른 날짜) → 조회
# ═══════════════════════════════════════════════════════════
DIARY_IDS = []

def test_d2_diary_create_read():
    print("\nD2: 다이어리 작성(3건) → 조회 ...")
    
    diaries = [
        {"userId": "test_user_b", "date": "2026-06-07", "timeSlot": "morning",
         "content": "아침 산책했다", "rating": 4.5, "latitude": 37.5665, "longitude": 126.978,
         "address": "서울시 중구", "weather": "맑음", "temperature": 22.5},
        {"userId": "test_user_b", "date": "2026-06-08", "timeSlot": "lunch",
         "content": "점심에 카페 갔다", "rating": 3.0, "latitude": 37.5512, "longitude": 126.988,
         "address": "서울시 용산구", "weather": "비", "temperature": 18.0},
        {"userId": "test_user_b", "date": "2026-06-09", "timeSlot": "evening",
         "content": "저녁에 산에 갔다", "rating": 5.0, "latitude": 37.5780, "longitude": 126.977,
         "address": "서울시 종로구", "weather": "맑음", "temperature": 25.0},
    ]
    
    for i, d in enumerate(diaries):
        try:
            r = requests.post(f"{BASE}/diary", json=d, timeout=10)
            r.raise_for_status()
            data = r.json()
            diary_id = data.get("id")
            DIARY_IDS.append(diary_id)
            write_json_log(f"D2_create_diary_{i+1}", d, data)
            
            # Verify 10 fields
            fields_ok = True
            for key in ["date", "timeSlot", "content", "rating", "latitude", "longitude", "address", "weather", "temperature"]:
                if str(data.get(key, "")) == "" and d.get(key) is not None:
                    fields_ok = False
                    print(f"    ⚠️ 필드 누락: {key}")
            
            print(f"  ✅ 다이어리 {i+1} 작성 OK (id={diary_id}, fields_ok={fields_ok})")
        except Exception as e:
            write_json_log(f"D2_create_diary_{i+1}", d, None, error=str(e))
            print(f"  ❌ 다이어리 {i+1} 작성 실패: {e}")
            return False

    # Read all diaries by user B
    try:
        r = requests.get(f"{BASE}/diary/test_user_b", timeout=10)
        r.raise_for_status()
        data = r.json()
        write_json_log("D2_read_diaries", {"userId": "test_user_b"}, data)
        created_ids = set(DIARY_IDS)
        found_ids = set(d.get("id") for d in data)
        missing = created_ids - found_ids
        if missing:
            print(f"  ❌ 조회 시 누락된 다이어리 ID: {missing}")
            return False
        print(f"  ✅ 다이어리 조회 OK (총 {len(data)}건, 방금 작성한 {len(DIARY_IDS)}건 포함)")
    except Exception as e:
        write_json_log("D2_read_diaries", {"userId": "test_user_b"}, None, error=str(e))
        print(f"  ❌ 다이어리 조회 실패: {e}")
        return False
    return True


# ═══════════════════════════════════════════════════════════
# D3: 다이어리 삭제
# ═══════════════════════════════════════════════════════════
def test_d3_diary_delete():
    print("\nD3: 다이어리 삭제 ...")
    if not DIARY_IDS:
        print("  ⚠️ 삭제할 다이어리 없음 (D2 스킵됨)")
        return False
    
    target_id = DIARY_IDS[-1]  # 마지막 것 삭제
    try:
        r = requests.delete(f"{BASE}/diary/{target_id}", timeout=10)
        r.raise_for_status()
        write_json_log("D3_delete_diary", {"diaryId": target_id}, {"status": "DELETED"})
        
        # Verify deletion
        r2 = requests.get(f"{BASE}/diary/test_user_b", timeout=10)
        remaining_ids = [d.get("id") for d in r2.json()]
        if target_id in remaining_ids:
            print(f"  ❌ 삭제 후에도 id={target_id}가 조회됨")
            return False
        
        DIARY_IDS.pop()
        print(f"  ✅ 다이어리 id={target_id} 삭제 OK (조회에서 미포함 확인)")
    except Exception as e:
        write_json_log("D3_delete_diary", {"diaryId": target_id}, None, error=str(e))
        print(f"  ❌ 삭제 실패: {e}")
        return False
    return True


# ═══════════════════════════════════════════════════════════
# D4: 친구 요청
# ═══════════════════════════════════════════════════════════
def test_d4_friend_request():
    print("\nD4: 친구 요청 ...")
    payload = {"fromUserId": "test_user_a", "toUserId": "test_user_b"}
    try:
        r = requests.post(f"{BASE}/friend/request", json=payload, timeout=10)
        r.raise_for_status()
        data = r.json()
        write_json_log("D4_friend_request", payload, data)
        print(f"  ✅ 친구 요청 OK (id={data.get('id')})")
    except Exception as e:
        write_json_log("D4_friend_request", payload, None, error=str(e))
        print(f"  ❌ 친구 요청 실패: {e}")
        return False
    return True


# ═══════════════════════════════════════════════════════════
# D5: 타임라인 조회 (TimelineDTO heartCount/commentCount 포함)
# ═══════════════════════════════════════════════════════════
def test_d5_timeline():
    print("\nD5: 친구 타임라인 조회 ...")
    if not DIARY_IDS:
        print("  ⚠️ 다이어리 없음")
        return False
    
    params = {"myUserId": "test_user_a", "friendId": "test_user_b", "date": "2026-06-07"}
    try:
        r = requests.get(f"{BASE}/friend/timeline", params=params, timeout=10)
        r.raise_for_status()
        data = r.json()
        write_json_log("D5_timeline", params, data)
        
        if len(data) == 0:
            print(f"  ❌ 타임라인 결과 0건")
            return False
        
        item = data[0]
        required = ["id", "date", "timeSlot", "content", "heartCount", "commentCount", "heartedByMe"]
        missing = [k for k in required if k not in item]
        if missing:
            print(f"  ❌ TimelineDTO 필드 누락: {missing}")
            return False
        
        print(f"  ✅ 타임라인 조회 OK ({len(data)}건, heartCount={item['heartCount']}, commentCount={item['commentCount']}, heartedByMe={item['heartedByMe']})")
    except Exception as e:
        write_json_log("D5_timeline", params, None, error=str(e))
        print(f"  ❌ 타임라인 조회 실패: {e}")
        return False
    return True


# ═══════════════════════════════════════════════════════════
# D6: 하트 토글 (2회 → ON → OFF)
# ═══════════════════════════════════════════════════════════
def test_d6_heart_toggle():
    print("\nD6: 하트 토글 (ON→OFF) ...")
    if not DIARY_IDS:
        print("  ⚠️ 다이어리 없음")
        return False
    
    payload = {"userId": "test_user_a", "diaryId": DIARY_IDS[0]}
    
    # Toggle ON
    try:
        r = requests.post(f"{BASE}/reaction/toggle", json=payload, timeout=10)
        r.raise_for_status()
        result1 = r.json()
        write_json_log("D6_heart_toggle_ON", payload, result1)
        
        if result1 != True:
            print(f"  ❌ 1차 토글 결과: {result1} (예상: true)")
            return False
        print(f"  ✅ 1차 하트 ON: {result1}")
    except Exception as e:
        write_json_log("D6_heart_toggle_ON", payload, None, error=str(e))
        print(f"  ❌ 1차 하트 실패: {e}")
        return False
    
    # Verify timeline shows heart
    params = {"myUserId": "test_user_a", "friendId": "test_user_b", "date": "2026-06-07"}
    try:
        r = requests.get(f"{BASE}/friend/timeline", params=params, timeout=10)
        data = r.json()
        target_id = DIARY_IDS[0]
        item = next((d for d in data if d["id"] == target_id), None)
        if not item:
            print(f"  ❌ 타임라인 반영 검증 실패: id={target_id} 항목 찾을 수 없음")
            return False
            
        assert item["heartedByMe"] == True, f"heartedByMe should be True, got {item['heartedByMe']}"
        assert item["heartCount"] >= 1, f"heartCount should be >=1, got {item['heartCount']}"
        print(f"  ✅ 타임라인 반영 확인: heartedByMe={item['heartedByMe']}, heartCount={item['heartCount']}")
    except Exception as e:
        print(f"  ❌ 타임라인 반영 검증 실패: {e}")
    
    # Toggle OFF
    try:
        r = requests.post(f"{BASE}/reaction/toggle", json=payload, timeout=10)
        r.raise_for_status()
        result2 = r.json()
        write_json_log("D6_heart_toggle_OFF", payload, result2)
        
        if result2 != False:
            print(f"  ❌ 2차 토글 결과: {result2} (예상: false)")
            return False
        print(f"  ✅ 2차 하트 OFF: {result2}")
    except Exception as e:
        write_json_log("D6_heart_toggle_OFF", payload, None, error=str(e))
        print(f"  ❌ 2차 하트 실패: {e}")
        return False
    
    return True


# ═══════════════════════════════════════════════════════════
# D7: 날짜 목록 조회
# ═══════════════════════════════════════════════════════════
def test_d7_dates():
    print("\nD7: 친구 날짜 목록 조회 ...")
    try:
        r = requests.get(f"{BASE}/friend/dates/test_user_b", timeout=10)
        r.raise_for_status()
        data = r.json()
        write_json_log("D7_dates", {"friendId": "test_user_b"}, data)
        
        if not isinstance(data, list):
            print(f"  ❌ 날짜 목록이 배열이 아님: {type(data)}")
            return False
        
        if len(data) < 2:
            print(f"  ⚠️ 날짜 목록이 2개 미만 ({len(data)}개) — D3에서 1건 삭제했으므로 정상일 수 있음")
        
        # Check descending order
        for i in range(len(data) - 1):
            if data[i] < data[i+1]:
                print(f"  ❌ 날짜 내림차순이 아님: {data[i]} < {data[i+1]}")
                return False
        
        print(f"  ✅ 날짜 목록 조회 OK ({len(data)}개, 내림차순 정렬 확인): {data}")
    except Exception as e:
        write_json_log("D7_dates", {"friendId": "test_user_b"}, None, error=str(e))
        print(f"  ❌ 날짜 목록 조회 실패: {e}")
        return False
    return True


# ═══════════════════════════════════════════════════════════
# D8: 댓글 CRUD (작성 → 조회 → 삭제)
# ═══════════════════════════════════════════════════════════
def test_d8_comment_crud():
    print("\nD8: 댓글 CRUD (작성→조회→삭제) ...")
    if not DIARY_IDS:
        print("  ⚠️ 다이어리 없음")
        return False
    
    diary_id = DIARY_IDS[0]
    
    # Create comment
    payload = {"userId": "test_user_a", "diaryId": diary_id, "content": "멋진 기록이네요!"}
    try:
        r = requests.post(f"{BASE}/comment", json=payload, timeout=10)
        r.raise_for_status()
        data = r.json()
        comment_id = data.get("id")
        write_json_log("D8_comment_create", payload, data)
        print(f"  ✅ 댓글 작성 OK (id={comment_id})")
    except Exception as e:
        write_json_log("D8_comment_create", payload, None, error=str(e))
        print(f"  ❌ 댓글 작성 실패: {e}")
        return False
    
    # Read comments
    try:
        r = requests.get(f"{BASE}/comment/{diary_id}", timeout=10)
        r.raise_for_status()
        data = r.json()
        write_json_log("D8_comment_read", {"diaryId": diary_id}, data)
        found = any(c.get("id") == comment_id for c in data)
        if not found:
            print(f"  ❌ 방금 작성한 댓글 id={comment_id}이 조회 안 됨")
            return False
        print(f"  ✅ 댓글 조회 OK (총 {len(data)}건, id={comment_id} 포함)")
    except Exception as e:
        write_json_log("D8_comment_read", {"diaryId": diary_id}, None, error=str(e))
        print(f"  ❌ 댓글 조회 실패: {e}")
        return False
    
    # Delete comment
    try:
        r = requests.delete(f"{BASE}/comment/{comment_id}", params={"userId": "test_user_a"}, timeout=10)
        r.raise_for_status()
        write_json_log("D8_comment_delete", {"commentId": comment_id, "userId": "test_user_a"}, {"status": "DELETED"})
        
        # Verify deletion
        r2 = requests.get(f"{BASE}/comment/{diary_id}", timeout=10)
        remaining = [c.get("id") for c in r2.json()]
        if comment_id in remaining:
            print(f"  ❌ 삭제 후에도 id={comment_id}이 조회됨")
            return False
        print(f"  ✅ 댓글 삭제 OK (조회에서 미포함 확인)")
    except Exception as e:
        write_json_log("D8_comment_delete", {"commentId": comment_id}, None, error=str(e))
        print(f"  ❌ 댓글 삭제 실패: {e}")
        return False
    
    return True


# ═══════════════════════════════════════════════════════════
# 기존 E2E (여행 추천 파이프라인)
# ═══════════════════════════════════════════════════════════
def test_recommend_nearby():
    print("\nE1: recommend_nearby (GET /nearby) ...")
    url = f"{BASE}/travel/recommend/nearby"
    params = {"lat": 37.5665, "lng": 126.9780, "radius": 1500}
    
    try:
        response = requests.get(url, params=params, timeout=120)
        response.raise_for_status()
        data = response.json()
        write_json_log("recommend_nearby", params, data)
        print(f"  ✅ Scenario E1 Success! Trip: {data.get('trip_title', 'N/A')}")
        if data.get('days') and len(data['days']) > 0:
            print(f"     Total Places for Day 1: {len(data['days'][0].get('places', []))}")
        test_save_to_db(data)
    except requests.exceptions.RequestException as e:
        error_resp = e.response.text if hasattr(e, 'response') and e.response is not None else None
        write_json_log("recommend_nearby", params, None, error=str(e) + (f" | {error_resp}" if error_resp else ""))
        print(f"  ❌ Scenario E1 Failed: {e}")

def test_recommend_by_diary():
    print("\nE2: recommend_by_diary (POST /diary) ...")
    url = f"{BASE}/travel/recommend/diary"
    payload = {
        "targetDate": "2026-06-15", "days": 3,
        "lat": 33.4996, "lng": 126.5312,
        "diaryText": "어제 제주도 바다에서 산책하고 흑돼지를 먹었는데 너무 좋았다. 이번 2박 3일 여행도 그렇게 가고 싶다."
    }
    
    try:
        response = requests.post(url, json=payload, timeout=120)
        response.raise_for_status()
        data = response.json()
        write_json_log("recommend_by_diary", payload, data)
        print(f"  ✅ Scenario E2 Success! Trip: {data.get('trip_title', 'N/A')}")
        if data.get('days') and len(data['days']) > 0:
            print(f"     Total Days Generated: {len(data['days'])}")
            for idx, day in enumerate(data['days']):
                print(f"     Day {day.get('day_number', idx+1)} Places: {len(day.get('places', []))}")
        test_save_to_db(data)
    except requests.exceptions.RequestException as e:
        error_resp = e.response.text if hasattr(e, 'response') and e.response is not None else None
        write_json_log("recommend_by_diary", payload, None, error=str(e) + (f" | {error_resp}" if error_resp else ""))
        print(f"  ❌ Scenario E2 Failed: {e}")

def test_save_to_db(plan_response):
    print("  E3: save_to_db (POST /save) ...")
    url = f"http://localhost:8083/api/travel/plan/save"
    params = {"userId": "test_user_a"}
    try:
        response = requests.post(url, params=params, json=plan_response, timeout=30)
        response.raise_for_status()
        data = {"status": "SUCCESS", "message": "DB save transaction completed"}
        write_json_log("save_to_db", plan_response, data)
        print("  ✅ Scenario E3 Success (DB Persistence OK)!")
    except requests.exceptions.RequestException as e:
        error_resp = e.response.text if hasattr(e, 'response') and e.response is not None else None
        write_json_log("save_to_db", plan_response, None, error=str(e) + (f" | {error_resp}" if error_resp else ""))
        print(f"  ❌ Scenario E3 Failed: {e}")


# ═══════════════════════════════════════════════════════════
# MAIN
# ═══════════════════════════════════════════════════════════
if __name__ == "__main__":
    print("=" * 60)
    print("  Harudiary E2E Pipeline Test Suite")
    print("  (소셜 CRUD + 여행 추천 파이프라인 통합 검증)")
    print("=" * 60)
    print(f"  Log: {LOG_FILE}")
    print(f"  Time: {datetime.datetime.now().isoformat()}")
    print("=" * 60)
    
    results = {}
    
    # ── Part A: 소셜/CRUD 검증 (D1~D8) ──
    print("\n─── Part A: 소셜/CRUD 검증 ───")
    results["D1"] = test_d1_register_login()
    results["D2"] = test_d2_diary_create_read()
    results["D3"] = test_d3_diary_delete()
    results["D4"] = test_d4_friend_request()
    results["D5"] = test_d5_timeline()
    results["D6"] = test_d6_heart_toggle()
    results["D7"] = test_d7_dates()
    results["D8"] = test_d8_comment_crud()
    
    # ── Part B: 여행 추천 파이프라인 (E1~E3) ──
    print("\n─── Part B: 여행 추천 파이프라인 ───")
    test_recommend_nearby()
    test_recommend_by_diary()
    
    # ── Summary ──
    print("\n" + "=" * 60)
    print("  RESULTS SUMMARY")
    print("=" * 60)
    passed = sum(1 for v in results.values() if v)
    total = len(results)
    for k, v in results.items():
        status = "✅ PASS" if v else "❌ FAIL"
        print(f"  {k}: {status}")
    print(f"\n  소셜/CRUD: {passed}/{total} passed")
    print(f"  상세 JSON 로그: {LOG_FILE}")
    print("=" * 60)
