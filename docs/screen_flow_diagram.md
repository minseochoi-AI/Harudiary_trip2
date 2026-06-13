# 🗺 Harudiary - 사용자 화면 이동 명세서 (User Journey Map)

현재 애플리케이션의 화면 이동 흐름(Screen Flow)과 사용자가 누르는 버튼/액션을 시각적으로 정리한 다이어그램입니다. `test_implementation_plan.md` 적용 전, 현재 프론트엔드의 구조를 파악하기 위한 명세서입니다.

```mermaid
flowchart TD
    %% ==== 정의 ====
    classDef activity fill:#e1f5fe,stroke:#03a9f4,stroke-width:2px;
    classDef fragment fill:#f3e5f5,stroke:#9c27b0,stroke-width:2px;
    classDef action fill:#fff3e0,stroke:#ff9800,stroke-width:1px,stroke-dasharray: 5 5;
    classDef popup fill:#e8f5e9,stroke:#4caf50,stroke-width:2px;

    %% ==== 노드 ====
    MAIN[MainActivity\n(Bottom Navigation)]:::activity
    HOME[HomeFragment\n(홈 캘린더)]:::fragment
    DAILY[DailyTimelineFragment\n(하루 타임라인)]:::fragment
    
    ADD_POPUP[팝업: 작성 유형 선택\n'기록 작성' or '여행 계획']:::popup

    PLAN_INPUT[PlanInputActivity\n(여행 계획 입력)]:::activity
    TRAVEL_PLAN[TravelPlanActivity\n(추천 일정 리뷰/수정)]:::activity
    RECORD[RecordActivity\n(다이어리 기록 작성)]:::activity

    %% ==== 메인 / 홈 흐름 ====
    MAIN -- "Bottom Nav [홈] 탭" --> HOME
    MAIN -- "Bottom Nav [+] 버튼" --> ADD_POPUP

    HOME -- "달력 날짜(Date) 클릭" --> DAILY
    HOME -- "오늘의 질문 [+] 클릭" --> RECORD

    %% ==== [+] 버튼 팝업 흐름 ====
    ADD_POPUP -- "✈️ 여행 계획 클릭" --> PLAN_INPUT
    ADD_POPUP -- "📝 기록 작성 클릭" --> RECORD

    %% ==== 여행 계획 (Plan) 흐름 ====
    PLAN_INPUT -- "날짜/제목 입력 후\n[추천받기] API 호출" --> TRAVEL_PLAN
    
    TRAVEL_PLAN -- "[일정 확정하기] 클릭\n(저장 후 종료)" --> PLAN_INPUT_FINISH((종료 후\nHome으로 복귀))
    TRAVEL_PLAN -- "각 장소 카드의\n[✅ 방문 인증] 클릭" --> RECORD
    
    %% ==== 일기 기록 (Record) 흐름 ====
    RECORD -- "내용/사진/위치 작성 후\n[저장] 클릭" --> RECORD_FINISH((저장 후\n이전 화면 복귀))
    
    RECORD_FINISH -. "방문 인증으로 진입 시\nPlan 업데이트" .-> TRAVEL_PLAN
    RECORD_FINISH -. "단순 기록 시" .-> HOME

    %% ==== 데일리 타임라인 흐름 ====
    DAILY -- "[+ 아침/점심/저녁/기타 추가] 클릭" --> RECORD
    DAILY -- "계획(주황색) / 기록(기본색)\n카드 토글" --> REACTION[리액션 바 노출\n(하트/댓글)]:::action
```

---

## 📌 주요 화면별 상세 기능 및 흐름

### 1. **MainActivity (하단 네비게이션 바)**
- **`[홈 탭]`**: `HomeFragment`를 호출하여 달력 및 스트릭을 보여줍니다.
- **`[+] 버튼 (nav_add)`**: "📝 기록 작성"과 "✈️ 여행 계획" 중 하나를 선택하는 바텀 팝업을 띄웁니다.

### 2. **HomeFragment (홈 화면)**
- **`달력 날짜 셀` 클릭**: 해당 날짜의 `DailyTimelineFragment`로 이동하여 그날의 타임라인(일정/기록)을 조회합니다.
- **`오늘의 질문 +` 버튼 클릭**: 즉시 `RecordActivity`를 호출하여 오늘 날짜로 일기를 씁니다.

### 3. **PlanInputActivity & TravelPlanActivity (계획 모드)**
- **진입**: 메인 화면의 [+] 팝업에서 "여행 계획" 선택 시 진입.
- **`PlanInputActivity`**: 날짜와 장소를 텍스트로 입력하고 추천 API를 백엔드로 보냅니다.
- **`TravelPlanActivity`**: 백엔드에서 AI가 추천해준 일정을 리스트업합니다.
  - **`순서 변경/삭제`**: 드래그 앤 드롭으로 일정을 마음대로 수정합니다.
  - **`✅ 방문 인증` 버튼 클릭**: 해당 계획 카드(장소)의 정보를 들고 `RecordActivity`로 진입합니다. 기록을 마치고 돌아오면 해당 장소는 "방문 완료(isVisited=true)"로 업데이트되며 조용히 백엔드에 저장됩니다.
  - **`일정 확정하기` 버튼 클릭**: 최종적으로 계획을 DB에 저장하고 화면을 종료하여 메인(Home)으로 돌아갑니다.

### 4. **DailyTimelineFragment (하루 타임라인 모드)**
- **진입**: 홈 달력에서 특정 날짜를 클릭 시 진입.
- 오전에 생성한 **'계획(Plan - 주황색 마커)'**과 방금 쓴 **'기록(Record - 기본색 마커)'**이 섞여서 타임라인에 그려집니다.
- **`[+ (시간대) 기록 추가]` 버튼 클릭**: 선택한 시간대 정보를 들고 `RecordActivity`로 수동 진입하여 빈 일기를 작성합니다.
- **카드 클릭**: 일기 카드 하단의 좋아요/댓글 리액션 바를 슬라이딩하여 열고 닫습니다.

### 5. **RecordActivity (기록 작성 뷰)**
- **진입**: [+] 팝업의 "기록 작성", 타임라인의 "+ 추가", 계획 리뷰의 "방문 인증" 등 다방면에서 진입합니다.
- (이번 작업으로 계획 UI가 완전 분리되어 오직 '일기 쓰기' 역할만 수행합니다.)
- 사진 첨부, 텍스트 입력 후 **[저장]** 시 현재 위치(GPS) 및 날씨를 백엔드로 보내 새 `Diary` 엔티티를 생성하고 이전 화면으로 돌아갑니다.
