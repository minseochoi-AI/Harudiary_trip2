# 🗺️ 증강된 화면 흐름도 (Augmented Screen Flow Diagram)

최신 구현 내역(`PlanInputActivity` 분리, `TravelPlanActivity` 신설, 방문 완료 파이프라인)을 반영하여 기존 흐름도를 증강했습니다.

```mermaid
flowchart TD
    %% ==== 노드 정의 ====
    classDef activity fill:#e1f5fe,stroke:#03a9f4,stroke-width:2px;
    classDef fragment fill:#f3e5f5,stroke:#9c27b0,stroke-width:2px;
    classDef action fill:#fff3e0,stroke:#ff9800,stroke-width:1px,stroke-dasharray: 5 5;

    MAIN[MainActivity\n(Bottom Navigation)]:::activity
    HOME[HomeFragment\n(홈 달력 / 통계)]:::fragment
    DAILY[DailyTimelineFragment\n(하루 타임라인)]:::fragment
    LIST[ListFragment\n(기록/계획 탭 분리)]:::fragment
    FRIEND[FriendListFragment\n(친구 검색 및 소식)]:::fragment
    
    RECORD[RecordActivity\n(다이어리 기록 작성)]:::activity
    PLAN_INPUT[PlanInputActivity\n(목적지 및 조건 입력)]:::activity
    TRAVEL_PLAN[TravelPlanActivity\n(N-Days 일정 리뷰/수정)]:::activity

    %% ==== 메인 흐름 ====
    MAIN -- "홈 탭" --> HOME
    HOME -- "날짜 클릭" --> DAILY
    MAIN -- "목록 탭" --> LIST
    MAIN -- "친구 탭" --> FRIEND
    
    %% ==== 작성 및 추천 분기 ====
    MAIN -- "FAB [+] 버튼" --> FAB_CHOICE{작성 유형 선택}
    FAB_CHOICE -- "📝 기록 작성" --> RECORD
    FAB_CHOICE -- "✈️ 여행 계획" --> PLAN_INPUT
    
    HOME -- "[내 주변 즉석 추천] 클릭" --> ENV_ACTION[Kakao/Weather API + Gemini]:::action
    ENV_ACTION --> DAILY
    
    %% ==== 여행 계획 파이프라인 ====
    PLAN_INPUT -- "추천받기 (Gemini API)" --> TRAVEL_PLAN
    TRAVEL_PLAN -- "Drag & Drop / Swipe 편집" --> EDIT_ACTION[UI 상호작용]:::action
    TRAVEL_PLAN -- "[일정 확정] (DB 저장)" --> HOME
    TRAVEL_PLAN -- "개별 장소 [✅ 방문 인증]" --> RECORD

    %% ==== 리스트 / 다이어리 조회 ====
    LIST -- "내 일기 탭 / 아이템 클릭" --> RECORD
    LIST -- "[휴지통] 클릭 (계획은 보호됨)" --> DELETE[DeleteRecordsActivity]:::activity
    
    %% ==== 딥-생성 파이프라인 ====
    RECORD -. "조회 모드에서\n[이 기록 기반 여행 생성] 클릭" .-> PLAN_INPUT
```
