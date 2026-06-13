# 🚀 Harudiary 통합 구현 명세서 (Implementation Plans)

본 문서는 프론트엔드, 백엔드 및 테스트 환경에 걸친 구조 개편과 개선 작업을 총망라한 통합 구현 명세서입니다.

---

## 1. 프론트엔드 수정 작업 명세서

### 📋 프론트엔드 작업 목록

| 번호 | 파일 수정형식 | 목적 (기대 효과) | 단위 컴포넌트 (클래스, 함수, 레이아웃 등) |
|:---:|:---|:---|:---|
| **1** | **[수정]** | **[긴급 NPE 해결 및 에디터 고도화]**<br>삭제된 UI 요소(별점, 시간)의 참조를 완벽히 걷어내어 Crash를 방지. `et_content` 배경 제거, 줄간격 최적화 등 노션(Notion) 스타일의 깔끔한 에디터 뷰로 개편. | `RecordActivity.java`<br>`activity_record.xml`<br>- `rbRating`, `btnMorning` 참조 삭제<br>- `et_content` 스타일 속성 조정 |
| **2** | **[수정]** | **[사진 첨부 및 취소 UX 보완]**<br>선택된 사진이 있을 때만 컨테이너를 노출하고, 맘에 들지 않을 경우 삭제 버튼(`btn_remove_photo`)을 눌러 롤백할 수 있는 토글 로직 추가. | `RecordActivity.java`<br>- `selectedPhotoUri` 분기 노출<br>- `btn_remove_photo` 리스너 추가 |
| **3** | **[추가/수정]** | **[기록과 계획 화면 분리 및 GPS 의존 해결]**<br>`EXTRA_MODE="plan"`일 경우 현재 GPS 환경 정보 로딩을 생략하여 불필요한 대기를 없애고, 목적지 입력 뷰를 추가하여 진정한 여행 계획이 가능하도록 물리적 분리. | `PlanInputActivity.java` (신설)<br>`RecordActivity.java`<br>- `requestLocationAndFetchData()` 분기 처리 |
| **4** | **[수정]** | **[일정 저장 후 피드백 및 스위칭]**<br>AI 계획을 저장한 뒤 화면이 먹통이 되지 않고, 저장 신호(`RESULT_OK`)를 보내 곧바로 해당 날짜의 메인 타임라인으로 이동시켜 저장 내역을 확인. | `TravelPlanActivity.java` (`setResult`)<br>`MainActivity.java` (`onActivityResult` 또는 `DailyFragment` 렌더링 호출) |
| **5** | **[수정]** | **[시각적 테마 분리 및 캘린더 연동]**<br>이미 수행한 기록(Record)과 미래의 계획(Plan)을 타임라인 카드 색상이나 D-Day 뱃지로 명확히 구분. 달력 마커도 서버 데이터에 따라 두 가지 색상으로 표출. | `DailyTimelineFragment.java`<br>`HomeFragment.java` |
| **5.1** | **[수정]** | **[방문 완료 피드백 보강]**<br>계획된 장소 방문 시 '방문 완료' 상태 동기화 및 기록 연동 자동화. | `TravelPlanAdapter.java`<br>`TravelPlanActivity.java`<br>- 방문 버튼 클릭 시 `RecordActivity` 연결<br>- `onActivityResult` 결과 처리 및 `isVisited` 상태 갱신 |

### 🚨 프론트엔드 추가 결정 필요 사항 및 솔루션

#### 1. [UX 단절] 사진 첨부 취소 로직 부재
* **문제**: 사용자가 사진을 첨부했다가 마음이 바뀌었을 때 되돌릴 방법(버튼)이 없음.
* **솔루션**: 레이아웃에 `btn_remove_photo`를 추가하고, 클릭 시 `selectedPhotoUri = null` 할당 및 `layout_photo_container`를 `GONE`, 다시 사진 추가 버튼을 `VISIBLE` 상태로 전환.

#### 2. [모호함] 맹목적 GPS 의존과 불필요한 로딩
* **문제**: 계획을 세우러 들어갔는데 현재 위치의 날씨를 찾기 위해 로딩 스피너가 돌고, 제주도/해외 목적지 검색 입력 창이 없어 UX가 비상식적임.
* **솔루션**: `EXTRA_MODE="plan"`일 때는 `requestLocationAndFetchData()`를 즉시 패스. 신규 `PlanInputActivity` 최상단에 "어디로 떠나시나요?" 텍스트 입력창 배치.

#### 3. [피드백 부재] 저장 후 화면 단절
* **문제**: `TravelPlanActivity`에서 [저장]을 눌러도 어디서 내 일정을 봐야 할지 알 수 없음.
* **솔루션**: 저장 성공 시 `setResult(RESULT_OK)` 후 `finish()`. 부모인 `MainActivity`에서 이를 캐치하여 저장된 날짜에 해당하는 `DailyFragment`로 화면을 즉시 스위칭해 시각적 피드백 제공.

#### 4. [디자인] 에디터 고도화 (Notion 스타일)
* **문제**: 기본 EditText 박스 형태가 세련되지 못함.
* **솔루션**: `et_content`의 `android:background="@null"`, `android:lineSpacingExtra="6dp"` 적용. 사진이 텍스트 바로 아래에 자연스럽게 붙도록 LinearLayout 구조 최적화.

---

## 2. 백엔드 수정 작업 명세서

### 📋 백엔드 작업 목록

| 번호 | 파일 수정형식 | 목적 (기대 효과) | 단위 컴포넌트 (클래스, 함수 등) |
|:---:|:---|:---|:---|
| **1** | **[수정]** | **[MVC 더티(Dirty) 상태 정리 및 빌드 확정]**<br>개편 중이던 폴더 이동에 따른 `package`, `import` 에러를 최종적으로 해결하여 빌드 무결성을 확보하고 커밋. | `travel/src/main/java/com/example/travel/`<br>- `controller`, `service`, `dto` 등 전 영역 |
| **2** | **[수정]** | **[모델 확장 및 반환 형식 개편]**<br>일기 저장 시 여행 계획 여부를 표기하는 `is_plan` 추가 및 `Place` 엔티티 내에 `is_visited` 방문 여부 상태 기록. 달력 마커 조회를 위해 `HomeFragment`가 사용할 `DiaryDateDto` 신규 규격 제공. | `Diary.java`, `Place.java`<br>`TravelController.java` (`recommendNearby`, `recommendByDiary`, `getMonthlyDates` 등 반환 모델 일괄 수정) |
| **3** | **[수정]** | **[고아 객체(Orphan) 찌꺼기 방치 해결]**<br>기존 일정을 재수정/초기화 시 DB에 자식 레코드들이 고아로 남지 않도록 JPA 설정을 정비. | `Diary.java` (`DayPlan` 매핑 부분)<br>`TravelPlanService.java` (`clear` 전파) |
| **4** | **[추가]** | **[이슈 #10: 소통 로직 집계 완성]**<br>프론트엔드의 N+1 우려 주석(하드코딩 0)을 철거할 수 있도록, 해당 다이어리의 좋아요/댓글 총합을 반환하는 API 제공. | `SocialController.java` (신설 또는 연장)<br>`GET /api/reaction/counts/{diaryId}` |
| **5** | **[추가/수정]** | **[이슈 #11: 달력 마커 시각적 구분 반환]**<br>기록(Record)과 계획(Plan) 날짜들을 단일 리스트가 아닌 식별 가능한 구조체 혹은 타입형태로 반환하여 프론트가 시각적으로 분기할 수 있게 지원. | `TravelController.java` (또는 `DiaryController.java`)<br>`GET /api/travel/plan/dates/{userId}` |

### 🚨 백엔드 추가 결정 필요 사항 및 솔루션

#### 1. [데이터 오염] 과거 기록과 미래 계획의 DB 혼재
* **문제**: 백엔드는 아직 발생하지 않은 "미래의 여행 계획"을 과거의 기억인 `Diary` 테이블에 우겨넣고 있음. 이 때문에 앱의 "올해 달성한 스트릭(작성 연속일)"이나 "이번 달 기록 개수"에 무작위로 미래의 날짜가 카운트되는 데이터 오염이 생김.
* **솔루션**: `Diary` 엔티티에 `is_plan(boolean)` 혹은 `type(enum)` 컬럼을 명시적으로 추가. 이후, 유저의 스트릭(Streak)이나 통계 정보를 반환하는 API(`DiaryRepository` 쿼리)에는 `WHERE is_plan = false` 조건을 의무화하여 집계에서 필터링.

#### 2. [용량 누수] 덮어쓰기 시 고아 객체 찌꺼기 방치
* **문제**: 사용자가 동일한 날짜의 여행 계획을 여러 번 [저장]하면, 기존의 자식 객체(`DayPlan`, `Place`)가 삭제되지 않고 DB에 쌓여 연결 고리가 끊긴 채 용량을 잡아먹게 됨.
* **솔루션**: `Diary` 엔티티의 `dayPlans` 리스트 매핑 시 `@OneToMany(mappedBy="diary", cascade=CascadeType.ALL, orphanRemoval=true)` 속성이 완벽히 활성화되어 있는지 점검. 필요하다면 Service에서 `clear()` 호출 전 강제 영속성 전이 수행.

#### 3. [구분 불가] 달력 마커의 시각적 구분 부재
* **문제**: 달력에 점을 찍기 위한 API가 단순히 날짜 문자열 배열 `["2026-06-25", "2026-06-28"]` 형태로만 반환되면, 프론트에서는 이것이 이미 다녀온 일기인지 앞으로 갈 계획인지 알 도리가 없음.
* **솔루션**: 반환 모델을 `List<DiaryDateDto>` 형태로 업그레이드하고, 내부에 `{"date": "2026-06-25", "type": "RECORD"}` 와 같이 구분자를 넣어 프론트로 전송.

---

## 3. 테스트 코드 수정 작업 명세서

### 📋 테스트 작업 목록

| 번호 | 대상 테스트 파일 | 담당 검증 내역 및 추가할 어설션(Assertion) |
|:---:|:---|:---|
| **1** | `PipelineE2EInstrumentedTest` | **[이슈 10, 12 검증: 강결합 제거 및 카운트 연동]**<br>• 전역변수 `DIARY_IDS`를 제거하고 각 테스트를 독립적으로 실행하도록 리팩토링.<br>• `testD5Timeline`, `testD8CommentCrud` 등에서 실제 하트/댓글 카운트가 0이 아닌 유의미한 값인지 응답 포맷 검증. |
| **2** | `TravelFlowE2EInstrumentedTest` | **[이슈 3 검증: 날짜 증발 방지]**<br>• `testSavePlanDuplicateUpdate`에서 프론트가 전송한 `date` 값이 백엔드 DB에 영구 저장되고, GET으로 재조회 시 해당 날짜 데이터가 정확히 일치하는지 검증. |
| **3** | `DiaryStatsE2EInstrumentedTest` | **[백엔드 데이터 오염 방지 검증]**<br>• 사용자가 "미래의 여행 계획(is_plan=true)"을 세웠을 때, 이 데이터가 "오늘의 일기 작성 스트릭(연속 기록)"이나 "이달의 기록 개수" 반환 API 호출 시 카운트를 부풀리지 않고 철저히 무시되는지 수학적으로 검증. |
| **4** | `FriendManagementE2EInstrumentedTest` | **[이슈 11 검증: 달력 반환 마커 분리]**<br>• `testFriendListDatesAndDelete`에서 친구의 달력 날짜 목록을 조회할 때, `Record` 속성과 `Plan` 속성이 분리된 형태의 DTO 배열(JSON)로 제대로 반환되는지 필드명 단위 검증. |

### 🚨 테스트 추가 결정 필요 사항 및 솔루션

#### 1. Spaghetti Test (테스트 순서 의존성) 철폐
* **문제**: 현재 E2E 테스트들은 `testD1`이 성공해야 얻은 ID를 `testD2`, `testD3`가 재사용하는 전역 리스트(`DIARY_IDS`) 구조를 가지고 있습니다. 이는 CI/CD에서 단일 테스트만 돌리거나 병렬 수행 시 무조건 실패를 유발합니다.
* **솔루션**: 각 `@Test` 내부에서 필요한 더미(Dummy) 데이터를 직접 생성(Setup)하고, 검증 후 롤백/삭제(Teardown)하는 완벽한 독립 테스트 모델로 리팩토링해야 합니다.

#### 2. 새롭게 반영되는 비즈니스 로직(is_plan, reaction counts)에 대한 엄격한 어설션
* **문제**: 프론트/백엔드 코드를 분리 및 고도화하는 과정에서 기존에 듬성듬성하게 짰던 `assertTrue` 검사가 아무 효과가 없을 수 있습니다.
* **솔루션**: 새로 추가되는 `is_plan` 필터링 검증(통계 카운트의 덧셈/뺄셈 여부)과 `heartCount` 같은 집계 값이 의도한 정확한 정수 값을 반환하는지 `assertEquals` 등을 통해 타이트하게 검사하도록 엣지 케이스 로직을 추가합니다.
