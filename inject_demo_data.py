import requests
import json
import time
import datetime

BASE_URL = "http://localhost:8083/api"
HEADERS = {'Content-Type': 'application/json'}

def post_req(path, payload):
    url = f"{BASE_URL}{path}"
    print(f"POST {url}")
    try:
        response = requests.post(url, json=payload, headers=HEADERS, timeout=10)
        print(f"Status: {response.status_code}, Body: {response.text}")
        return response.json() if response.text else {}
    except Exception as e:
        print(f"Error: {e}")
        return None

def get_req(path):
    url = f"{BASE_URL}{path}"
    print(f"GET {url}")
    try:
        response = requests.get(url, headers=HEADERS, timeout=10)
        print(f"Status: {response.status_code}")
        return response.json() if response.text else {}
    except Exception as e:
        print(f"Error: {e}")
        return None

def main():
    print("=== Harudiary 10단계 시연용 데이터 자동 주입 시작 ===")
    
    # User IDs
    demo_user = "msc93913@gmail.com"
    demo_friend = "demo_friend"
    pending_user = "pending_user"
    search_user = "search_user"
    
    # 1. 계정 4개 일괄 생성
    print("\n[Step 1] 계정 생성")
    for u in [demo_user, demo_friend, pending_user, search_user]:
        post_req("/user/register", {"id": u, "nickname": f"Name_{u}"})
    time.sleep(0.5)

    # 2. 친구 맺기 (자동 수락) : demo_friend -> demo_user
    print("\n[Step 2] 친구 맺기 (demo_friend <-> demo_user)")
    status = get_req(f"/friend/status?userId={demo_friend}&friendId={demo_user}")
    if status != "friend":
        post_req("/friend/request", {"fromUserId": demo_friend, "toUserId": demo_user})
        reqs = get_req(f"/friend/requests/{demo_user}")
        if reqs:
            for req in reqs:
                from_user = req.get("fromUser", {})
                if from_user.get("id") == demo_friend:
                    post_req(f"/friend/accept/{req.get('id')}", {})
    time.sleep(0.5)

    # 3. 대기 중인 친구 요청 : pending_user -> demo_user
    print("\n[Step 3] 대기 중인 친구 요청 (pending_user -> demo_user)")
    p_status = get_req(f"/friend/status?userId={pending_user}&friendId={demo_user}")
    if p_status == "none":
        post_req("/friend/request", {"fromUserId": pending_user, "toUserId": demo_user})
    time.sleep(0.5)

    # 4. 일상 기록 (스트릭용)
    print("\n[Step 4] 일상 기록 주입 (스트릭용)")
    post_req("/diary", {"userId": demo_user, "date": "2026-06-10", "timeSlot": "morning", "content": "기말고사 화이팅!", "rating": 4.0})
    post_req("/diary", {"userId": demo_user, "date": "2026-06-11", "timeSlot": "evening", "content": "친구랑 한강공원 산책함", "rating": 5.0})

    # 5. AI 생성용 시드 일기
    print("\n[Step 5] AI 생성용 시드 일기 주입")
    seed_res = post_req("/diary", {"userId": demo_user, "date": "2026-06-13", "timeSlot": "evening", "content": "이번 여름 휴가 때는 제주도에 가서 흑돼지도 먹고 오션뷰 카페에서 푹 쉬고 싶다.", "rating": 5.0})
    
    seed_diary_id = 1
    if seed_res:
        seed_diary_id = seed_res.get("activityId") or seed_res.get("id") or 1

    # 6. 미리 세워둔 여행 계획
    print("\n[Step 6] 미래의 여행 계획 주입")
    plan_payload = {
        "tripTitle": "미리 세워둔 부산 일정",
        "days": [
            {
                "day_number": 1,
                "places": [
                    {
                        "place_name": "해운대 해수욕장", 
                        "travel_time_minutes_to_next": 0, 
                        "transport_mode": "도보", 
                        "address_name": "부산 해운대구",
                        "place_category": "관광지",
                        "time_spent_hours": 2.0,
                        "x": "129.1586",
                        "y": "35.1587",
                        "is_visited": False
                    }
                ]
            }
        ]
    }
    post_req(f"/travel/plan/save?userId={demo_user}&date=2026-06-25", plan_payload)

    # 7. 친구의 오늘자 기록
    print("\n[Step 7] 친구의 오늘자 기록 주입")
    today_str = datetime.datetime.now().strftime("%Y-%m-%d")
    post_req("/diary", {"userId": demo_friend, "date": today_str, "timeSlot": "lunch", "content": "오늘 날씨 너무 좋다! 카페 옴.", "rating": 5.0})

    # 8. 친구가 내 일기에 리액션 및 댓글
    print("\n[Step 8] 친구 리액션 및 댓글 (to Seed Diary)")
    post_req("/reaction/toggle", {"userId": demo_friend, "diaryId": seed_diary_id})
    post_req("/comment", {"userId": demo_friend, "diaryId": seed_diary_id, "content": "나도 갈래!"})

    # 9. 휴지통 삭제 전용 일기
    print("\n[Step 9] 삭제 시연용 더미 일기 주입")
    post_req("/diary", {"userId": demo_user, "date": "2026-06-14", "timeSlot": "night", "content": "이건 지울 일기", "rating": 1.0})

    # 10. 낯선 사람 검색 시연
    print("\n[Step 10] 검색 대상자 계정 확인 완료")

    print("\n=== 데이터 주입 완료 ===")
    print("앱을 실행하고 'demo_user' 계정으로 로그인하여 시연을 시작하세요!")

if __name__ == "__main__":
    main()
