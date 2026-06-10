# Harudiary - 세부 사용자 목적 기반 기능 및 API 오케스트레이션 명세서

본 문서는 추상적인 개념(Bio-Weather 등)을 모두 배제하고, `Harudiary`와 `travel` 프로젝트가 실제로 결합되었을 때 **프론트엔드 UI 상호작용부터 백엔드의 엔드포인트별 속성 처리까지의 파이프라인**을 원자적(Atomic) 수준으로 분해한 명세입니다.

---

## 1. 목적: 다이어리 작성 시 환경 정보(주소/날씨) 자동 기록

**[상황 및 프론트엔드 상호작용]**
- 사용자가 메인 화면에서 플로팅 액션 버튼(FAB, ➕)을 눌러 `RecordActivity` (일기 작성 화면)에 진입함.
- 프론트엔드는 즉시 디바이스의 GPS 센서를 가동하여 위경도(`x`, `y`) 좌표를 획득.

| 단계 | 주체 | 엔드포인트 / 시스템 | 전달/요청 속성(Request) | 반환/처리 속성(Response) |
|---|---|---|---|---|
| 1 | Client | `GET /api/env/current` | `lat={y}`, `lng={x}` | 백엔드로 좌표 전송 (화면에는 비동기 로딩 스피너 작동) |
| 2 | Backend | Kakao `geo/coord2address` | `x={x}`, `y={y}`, Header: `Authorization` | `documents[0].road_address.building_name` 등 파싱 |
| 3 | Backend | Weather `getUltraSrtNcst` | 격자변환 알고리즘 적용 ➔ `nx`, `ny`, `base_time` 등 | `category=PTY, T1H`의 `obsrValue` (강수/기온) 파싱 |
| 4 | Backend | 내부 통합 컨트롤러 | Kakao + Weather 원시 파싱 데이터 | `{ "address": "...", "weather_status": 1, "temp": 27.5 }` 형태의 단일 JSON 반환 |
| 5 | Client | UI 렌더링 로직 | 수신된 단일 JSON 객체 | 텍스트뷰에 주소 자동 기입, `weather_status` 값에 따라 ☀️, 🌧️ 이모지로 변환하여 렌더링 |
| 6 | Client | DB 영구 저장 트리거 | 사용자의 **[저장] 버튼** 클릭 | 사용자가 작성한 일기 본문 + 자동 생성된 주소/날씨를 묶어 `POST /api/diary`로 백엔드 전송 |

---

## 2. 목적: 내 위치 기반(GPS) 실시간 날씨 맞춤 주변 여행지 즉석 추천

**[상황 및 프론트엔드 상호작용]**
- 당일 치기나 즉흥 여행 중, 사용자가 "현재 위치에서 비를 피해 갈 수 있는 실내 코스"를 찾고자 할 때, 홈 화면의 **[내 주변 즉석 추천] 버튼** 클릭.
- (선택) 반경(예: 1km) 설정 팝업 제공 후 현재 GPS와 함께 전송.

| 단계 | 주체 | 엔드포인트 / 시스템 | 전달/요청 속성(Request) | 반환/처리 속성(Response) |
|---|---|---|---|---|
| 1 | Client | `GET /api/travel/recommend/nearby`| `lat={y}`, `lng={x}`, `radius=1000` | 추천 대기 상태 (스켈레톤 UI 표출) |
| 2 | Backend | Weather `getUltraSrtNcst` | `nx`, `ny` | 현재 비가 오는지, 더운지(`PTY`, `T1H`) 기상 상태 파악 |
| 3 | Backend | Kakao `search/category` | `category_group_code=FD6,CE7,AT4` (음식, 카페, 명소), `x`, `y`, `radius` | 주변 장소의 `place_name`, `distance`, `category_name` 등 리스트업 |
| 4 | Backend | Gemini API `generateContent` | **Prompt:** `[현재날씨: 비, 27도] + [반경 내 장소배열 20개] 기반으로, 실내 위주 최단 동선 3곳 추천. json.md 규격 엄수.` | 구조화된 JSON (`trip_title`, `days`, `places[ travel_time, transport_mode ]`) 도출 |
| 5 | Client | UI 렌더링 로직 | `TravelPlanResponse` DTO 매핑 | `DailyTimelineFragment`의 `RecyclerView`에 각 장소 카드 렌더링. 장소 간 연결선(점선) 위에 `travel_time` 텍스트 매핑. |

---

## 3. 목적: 내 일기(과거 기록 텍스트) 기반 맞춤형 여행 일정 딥-생성

**[상황 및 프론트엔드 상호작용]**
- 사용자가 과거에 쓴 *"제주도 바다를 걷고 흑돼지를 먹고싶다"*라는 다이어리의 상세 페이지(`RecordActivity` 조회 모드)에서 **[이 기록 기반으로 여행 일정 생성] 버튼** 클릭.

| 단계 | 주체 | 엔드포인트 / 시스템 | 전달/요청 속성(Request) | 반환/처리 속성(Response) |
|---|---|---|---|---|
| 1 | Client | `POST /api/travel/recommend/diary`| `diaryId={id}` 전송 | (일정 생성 Lottie 애니메이션 표출) |
| 2 | Backend | DB 조회 로직 (MySQL) | `SELECT * FROM Diary WHERE id = {id}` | 해당 다이어리의 본문 내용(Text) 획득 |
| 3 | Backend | Gemini API (1차 추출) | **Prompt:** `본문에서 방문 희망 지역과 키워드를 배열로 추출.` | `["제주도", "흑돼지", "바다"]` |
| 4 | Backend | Kakao `search/keyword` | `query="제주도 흑돼지"`, `query="제주도 바다"` 등 다중 호출 | 키워드에 매칭되는 유명 장소(POI) `documents` 배열 획득 |
| 5 | Backend | Weather `getVilageFcst` | 제주도 좌표(`nx`, `ny`), `base_date=여행예정일` | 여행 예정일의 `POP`(강수확률), `SKY`(하늘상태) 파악 |
| 6 | Backend | Gemini API (2차 조합) | **Prompt:** `[추출된 키워드 장소목록] + [예정일 날씨] 기반 2박 3일 최단 동선 배정. json.md 준수.` | 계층형(`day_number`, `places`) JSON 반환 |
| 7 | Client | UI 렌더링 로직 | `TravelPlanResponse` DTO 수신 | `TabLayout`(Day 1, Day 2)으로 일자별 타임라인 분리 렌더링 |

---

## 4. 목적: 추천된 타임라인 일정의 개인화(커스텀 편집) 및 최종 DB 확정

**[상황 및 프론트엔드 상호작용]**
- 생성된 추천 일정 화면에서, 사용자가 "이 카페는 빼고 다른 식당을 먼저 가야지"라고 판단.
- 프론트엔드 `RecyclerView`의 아이템을 **좌우 스와이프하여 특정 장소 삭제**, 아이템 우측 핸들을 **위아래로 롱클릭 드래그하여 방문 순서 변경**.
- 편집을 모두 마친 후 하단의 **[일정 확정 및 저장] 버튼** 클릭.

| 단계 | 주체 | 엔드포인트 / 시스템 | 전달/요청 속성(Request) | 반환/처리 속성(Response) |
|---|---|---|---|---|
| 1 | Client | 안드로이드 `ItemTouchHelper` | Swipe / Drag & Drop 네이티브 터치 이벤트 | 앱 메모리 상의 `List<PlaceDto>` 인덱스 즉각 재배치 및 UI 갱신 |
| 2 | Client | `POST /api/travel/plan/save` | 편집이 완료된 최종 `TravelPlanRequest` 전체 JSON 바디 | 서버로 편집된 데이터 전송 |
| 3 | Backend | JPA 영속성 저장 로직 | 전달받은 JSON 파싱 ➔ `DayPlan`, `Place` 엔티티 매핑 | 전달받은 `List`의 순서(인덱스)를 데이터베이스 `Place` 테이블의 `sequence` 컬럼에 매핑하여 `INSERT/UPDATE` 처리 |
| 4 | Client | 화면 전환 제어 | HTTP 200 OK 수신 대기 | 성공 모달 표출 후 메인 화면 혹은 마이페이지의 '내 여행 일정' 탭으로 화면 전환 제어 |
