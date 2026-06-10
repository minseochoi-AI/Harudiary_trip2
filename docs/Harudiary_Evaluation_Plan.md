# Harudiary 프로젝트 평가 및 기능 증강 계획 (Term Project 평가 기준)

본 문서는 `Harudiary` 프론트엔드 앱의 현재 구현 수준을 평가 기준(기능 30%, UI 30%, 완성도 20%, 활용가능성 20%)에 따라 비판적으로 검증하고, `App0523` 및 `travel` 프로젝트의 우위 사항을 결합한 **기능적 증강(Augmentation) 계획**입니다. 기존 논의된 평가 내용은 철회 없이 유지하며, 실제 구현을 위한 파일 수정 사항을 3가지 영역으로 명확히 3분할하여 정리했습니다.

---

## 1. [백엔드] Docker 기반 travel 서버 수정 및 DB 구현 방안
기존 `travel` 프로젝트에 `Harudiary`의 데이터(사용자, 다이어리, 친구 등)를 영구 저장할 수 있는 DB(MySQL의 `harudiary_db`)를 연동하고, 수정된 코드를 Docker 컨테이너에 재적용(중단 후 재실행)합니다.

| 수정 위치 (경로) | 대상 파일 | 예상 수정 개수 | 상세 변경 및 구현 내용 |
| --- | --- | --- | --- |
| `/home/ubuntu/root/leeyoungkon_projects/travel/src/main/resources/` | `application.properties` | 2곳 | MySQL `harudiary_db` 데이터베이스 연결 정보(`spring.datasource.url` 등), 환경 변수 기반 Gemini API 키 설정(`gemini.api.key=${GEMINI_API_KEY}`), 및 JPA 설정 추가 |
| `/home/ubuntu/root/leeyoungkon_projects/travel/` | `pom.xml` | 1곳 | `mysql-connector-java` 및 `spring-boot-starter-data-jpa` 라이브러리 추가 |
| `/home/ubuntu/root/leeyoungkon_projects/travel/src/main/java/com/example/travel/model/` | `User.java`, `Activity.java` 등 | 신규 생성 (약 3~4개) | `Harudiary`의 `DBHelper.java`에 있던 테이블(User, Activity 등)을 JPA Entity로 변환 |
| `/home/ubuntu/root/leeyoungkon_projects/travel/src/main/java/com/example/travel/controller/` | `DiaryController.java` 등 | 신규 생성 (약 2~3개) | 프론트엔드의 다이어리 저장/조회/동기화 요청을 처리할 REST API 구현 |
| **터미널 (명령어 실행 위치: travel 디렉토리)** | CLI 명령어 | - | 1. `docker stop travel-server`<br>2. `docker rm travel-server`<br>3. `docker build -t travel-backend .`<br>4. `docker run -d -p 8083:8083 -e GEMINI_API_KEY=your_api_key_here --name travel-server travel-backend` |

---

## 2. [프론트엔드] App0523 기반 Harudiary 연동 및 UI 수정 방안
`Harudiary` 앱의 로컬 DB (SQLite) 의존성을 끊고, `App0523`의 검증된 네트워크(Retrofit) 및 이미지 최적화(Glide) 패턴을 이식합니다.

| 수정 위치 (경로) | 대상 파일 | 예상 수정 개수 | 상세 변경 및 구현 내용 |
| --- | --- | --- | --- |
| `/home/ubuntu/root/leeyoungkon_projects/Harudiary/app/` | `build.gradle.kts` | 2곳 | `Retrofit2`, `Gson Converter`, `Glide` 의존성 추가 및 Sync |
| `/home/ubuntu/root/leeyoungkon_projects/Harudiary/app/src/main/java/com/example/harudiary/api/` | `RetrofitClient.java`, `TravelApi.java` | 신규 생성 (2개) | `App0523` 코드를 복사 및 응용하여 백엔드(`travel`)와 통신할 인터페이스/클라이언트 설정 |
| `/home/ubuntu/root/leeyoungkon_projects/Harudiary/app/src/main/java/com/example/harudiary/db/` | `DBHelper.java` | 1곳 (대규모) | 기존 로컬 DB CRUD 로직을 Retrofit 비동기 네트워크 호출(서버 연동) 방식으로 교체 |
| `/home/ubuntu/root/leeyoungkon_projects/Harudiary/app/src/main/java/com/example/harudiary/fragment/` | `DailyTimelineFragment.java` | 3곳 | **(상세화)** 백엔드를 거쳐 Gemini API가 최적화하여 반환한 계층형 JSON(Day별, Place별 동선 및 시간 정보) 결과를 비동기로 수신한 뒤, 각 방문 장소를 RecyclerView 컴포넌트로 순서에 따라 렌더링합니다.<br>또한, 생성된 타임라인 일정 내에서 사용자가 특정 장소 아이템을 **'삭제(스와이프)'**, **'위치 이동(ItemTouchHelper를 통한 드래그 앤 드롭)'**, 그리고 최종적으로 **'결정(Save/Commit)'** 할 수 있는 UI 제어 로직을 추가하며, 각 장소의 이미지는 `Glide`를 사용하여 최적화 로드합니다. |
| `/home/ubuntu/root/leeyoungkon_projects/Harudiary/app/src/main/java/com/example/harudiary/model/` | `TravelPlanResponse.java` 등 | 신규 생성 (약 3개) | 서버가 반환하는 JSON 형태와 일치하는 계층형 데이터 모델(DTO) 추가 |

---

## 3. 데이터 연동, API 검증 및 AI 라우팅 설계 (고려사항)

**1. 다이어리 앱에서 '여행 일정' 데이터를 정확히 받아 출력하기 위한 구성 방안 및 고려사항**
기존 Harudiary의 일상 기록 기능에 travel 백엔드의 "여행 일정(Day-by-Day)" 데이터를 결합하여 화면에 출력하려면 다음 사항들을 고려하여 프론트엔드를 재구성해야 합니다.

- **계층형 데이터 모델(DTO) 동기화**: 백엔드에서 반환하는 JSON 구조(TravelPlanResponse ➔ DayPlanDto ➔ PlaceDto)와 정확히 일치하는 모델 클래스를 Android 앱에 생성해야 합니다. 특히 장소의 위도/경도, 방문 순서, 예상 소요 시간 등의 필드가 누락 없이 매핑되어야 합니다.
- **타임라인(Timeline) 기반 UI/UX 설계**: 여행 일정은 시간의 흐름에 따라 진행됩니다. Harudiary에 이미 있는 DailyTimelineFragment를 확장하거나 RecyclerView를 중첩(Day 리스트 안에 Place 리스트)하여, 타임라인 형태(예: 점선으로 장소와 장소가 이어지는 UI)로 시각화하는 것이 직관적입니다.
- **비동기 처리 및 상태 관리 (ViewModel + LiveData/StateFlow)**: AI가 일정을 생성하는 데는 몇 초의 지연 시간(Latency)이 발생할 수 있습니다. 사용자가 지루함을 느끼지 않도록 프로그레스 바(로딩 바)를 띄워두고, Retrofit을 통한 백엔드 통신이 완료되면 ViewModel을 통해 화면이 자연스럽게 갱신되도록 구성해야 합니다.
- **사용자 편집(Customization) 고려**: AI가 짜준 일정이 완벽하지 않을 수 있습니다. 앱에서 특정 장소를 삭제하거나 순서를 드래그 앤 드롭(ItemTouchHelper)으로 변경할 수 있는 기능을 고려해야 실사용성이 크게 올라갑니다.

**2. Kakao API의 맛집/여행 정보 지원 여부 비판적 검증**
현재 Harudiary가 사용 중인 Kakao API(카카오 로컬 API 및 지도 API)에 대한 정확한 한계와 지원 범위는 다음과 같습니다.

- **맛집 및 장소 정보 (지원 O, 매우 우수함)**: 카카오 로컬 API의 키워드 검색(v2/local/search/keyword.json)은 카테고리 그룹 코드(예: FD6 - 음식점, CE7 - 카페, AT4 - 관광명소)를 통해 매우 정확한 국내 맛집 및 명소 데이터를 제공합니다. 별점은 직접 제공하지 않으나, 카카오맵 상세 URL을 제공하여 즉시 연동 가능합니다.
- **여행 계획 생성 및 최단 경로 도출 (지원 X, 직접 구현 불가)**: 카카오 로컬 실시간 API 자체에는 "여행 계획을 짜주는 기능"이나 "여러 장소를 경유하는 최단 경로(TSP 문제) 알고리즘"이 포함되어 있지 않습니다.
길찾기(내비) API를 쓰면 A에서 B까지의 시간은 알 수 있으나, 여러 장소를 묶어 최적의 동선을 도출하는 것은 API 호출 횟수 낭비와 복잡한 알고리즘(프론트엔드 연산 부하)을 초래합니다.

**3. Gemini API를 활용한 최단 경로 및 시간 도출 (내재적 기능 활용)**
카카오 API의 '경로 최적화' 한계를 극복하기 위해, 백엔드의 Gemini API가 가진 "지리적 추론 및 공간 인지 능력"을 활용해야 합니다. Gemini는 단순한 텍스트 봇이 아니라 내부 학습 데이터에 기반해 각 장소 간의 물리적 거리와 이동 수단별 예상 시간을 꽤 정확히 알고 있습니다.

**Gemini 로직 구성 및 이해:**

- **프롬프트 엔지니어링 (Prompting)**: Spring Boot 백엔드에서 Gemini에게 요청을 보낼 때, 단순히 "제주도 2박 3일 일정 짜줘"가 아니라 다음과 같이 프롬프트를 시스템적으로 강제해야 합니다.
*"제주도 2박 3일 일정을 작성하되, 반드시 **최단 이동 동선(동선 낭비 최소화)**을 고려하여 장소를 배치해 줘. 그리고 각 장소에서 다음 장소까지의 **예상 이동 시간(분 단위)과 이동 수단(도보/차량)**을 JSON 필드로 정확히 포함해."*

- **내재적 라우팅(Routing)**: Gemini는 자체 지식 그래프를 활용해 "성산 일출봉"을 본 후에는 동쪽의 "섭지코지"로 가는 것이 합리적이라는 것을 추론합니다. 이를 통해 외부의 무거운 길찾기 API(Kakao Navi API 등)를 일일이 호출할 필요 없이, AI 단일 호출만으로 완성된 '최적 동선'과 '이동 시간 데이터'를 한 번에 프론트엔드로 전달할 수 있습니다.
- **앱에서의 표현**: 앱은 백엔드(Gemini)가 내려준 JSON 데이터 속 `estimated_travel_time: "15분"` 값을 그대로 타임라인 UI의 장소와 장소 사이 연결선에 텍스트로 뿌려주기만 하면 됩니다.

**4. 추가 결정 및 반영 사항 (2026-06-09)**
- **DB 환경 설정:** 백엔드에서 사용할 MySQL 데이터베이스는 `/home/ubuntu/root/leeyoungkon_projects/travel` 경로 내에 (Docker Compose 등을 활용하여) 직접 구성하며, 스키마명은 **`harudiary_db`**로 지정합니다. 현재 개발 위치가 서버 환경이므로 로컬에서 서버와 DB를 함께 구동하며, 이 공간에서 Gemini API 연동과 DB 관리를 통합하여 진행합니다.
- **Gemini API Key 관리 방식:** 소스 코드에 API 키를 하드코딩하지 않고, `application.properties`에 변수로 선언한 뒤 Docker 컨테이너 실행 시 환경 변수(`GEMINI_API_KEY`)를 통해 주입하는 방식으로 관리합니다.
- **사용자 인증(Auth) 처리:** 복잡한 보안 인증 로직(JWT 등)은 배제하고, 프론트엔드와 백엔드 간에 간단한 고유 ID 매핑 방식을 적용하여 인증을 단순하고 빠르게 처리합니다.
- **응답 대기 중 UI:** Gemini API가 일정을 생성하는 등 네트워크 지연(Latency)이 발생할 때, 사용자 대기 화면으로 직관적인 **프로그레스 바(로딩 바)**를 띄워 상태를 명확히 안내합니다.

**5. 작업 우선순위 및 검증 절차 (Phased Approach)**
두 프로젝트를 동시에 수정하면 테스트가 어려울 수 있습니다.
- **Phase 1**: 먼저 `travel` 백엔드 서버에 Entity/Repository를 세팅하고, Gemini 연동 및 DB 연동을 마친 뒤 Docker로 다시 띄워서 API 응답(JSON)이 잘 나오는지 Postman/curl로 검증합니다.
- **Phase 2**: 그 후 `Harudiary` 프론트엔드로 넘어가서 Retrofit을 붙이고 기존 SQLite 코드를 들어내는 순서로 작업하는 것이 안전합니다.


# Harudiary 프로젝트 평가 및 기능 증강 계획 (Term Project 평가 기준)

본 문서는 `Harudiary` 프론트엔드 앱의 현재 구현 수준을 평가 기준(기능 30%, UI 30%, 완성도 20%, 활용가능성 20%)에 따라 비판적으로 검증하고, `App0523` 및 `travel` 프로젝트의 우위 사항을 결합한 **기능적 증강(Augmentation) 계획**입니다. 기존 논의된 평가 내용은 철회 없이 유지하며, 실제 구현을 위한 파일 수정 사항을 3가지 영역으로 명확히 3분할하여 정리했습니다.

---

## 1. [백엔드] Docker 기반 travel 서버 수정 및 DB 구현 방안
기존 `travel` 프로젝트에 `Harudiary`의 데이터(사용자, 다이어리, 친구 등)를 영구 저장할 수 있는 DB(MySQL 등)를 연동하고, 수정된 코드를 Docker 컨테이너에 재적용(중단 후 재실행)합니다.

| 수정 위치 (경로) | 대상 파일 | 예상 수정 개수 | 상세 변경 및 구현 내용 |
| --- | --- | --- | --- |
| `/home/ubuntu/root/leeyoungkon_projects/travel/src/main/resources/` | `application.properties` | 2곳 | MySQL 데이터베이스 연결 정보(`spring.datasource.url` 등) 및 JPA 설정 추가 |
| `/home/ubuntu/root/leeyoungkon_projects/travel/` | `pom.xml` | 1곳 | `mysql-connector-java` 및 `spring-boot-starter-data-jpa` 라이브러리 추가 |
| `/home/ubuntu/root/leeyoungkon_projects/travel/src/main/java/com/example/travel/model/` | `User.java`, `Activity.java` 등 | 신규 생성 (약 3~4개) | `Harudiary`의 `DBHelper.java`에 있던 테이블(User, Activity 등)을 JPA Entity로 변환 |
| `/home/ubuntu/root/leeyoungkon_projects/travel/src/main/java/com/example/travel/controller/` | `DiaryController.java` 등 | 신규 생성 (약 2~3개) | 프론트엔드의 다이어리 저장/조회/동기화 요청을 처리할 REST API 구현 |
| **터미널 (명령어 실행 위치: travel 디렉토리)** | CLI 명령어 | - | 1. `docker stop travel-server`<br>2. `docker rm travel-server`<br>3. `docker build -t travel-backend .`<br>4. `docker run -d -p 8083:8083 --name travel-server travel-backend` |

---

## 2. [프론트엔드] App0523 기반 Harudiary 연동 및 UI 수정 방안
`Harudiary` 앱의 로컬 DB (SQLite) 의존성을 끊고, `App0523`의 검증된 네트워크(Retrofit) 및 이미지 최적화(Glide) 패턴을 이식합니다.

| 수정 위치 (경로) | 대상 파일 | 예상 수정 개수 | 상세 변경 및 구현 내용 |
| --- | --- | --- | --- |
| `/home/ubuntu/root/leeyoungkon_projects/Harudiary/app/` | `build.gradle.kts` | 2곳 | `Retrofit2`, `Gson Converter`, `Glide` 의존성 추가 및 Sync |
| `/home/ubuntu/root/leeyoungkon_projects/Harudiary/app/src/main/java/com/example/harudiary/api/` | `RetrofitClient.java`, `TravelApi.java` | 신규 생성 (2개) | `App0523` 코드를 복사 및 응용하여 백엔드(`travel`)와 통신할 인터페이스/클라이언트 설정 |
| `/home/ubuntu/root/leeyoungkon_projects/Harudiary/app/src/main/java/com/example/harudiary/db/` | `DBHelper.java` | 1곳 (대규모) | 기존 로컬 DB CRUD 로직을 Retrofit 비동기 네트워크 호출(서버 연동) 방식으로 교체 |
| `/home/ubuntu/root/leeyoungkon_projects/Harudiary/app/src/main/java/com/example/harudiary/fragment/` | `DailyTimelineFragment.java` | 3곳 | **(상세화)** 백엔드를 거쳐 Gemini API가 최적화하여 반환한 계층형 JSON(Day별, Place별 동선 및 시간 정보) 결과를 비동기로 수신한 뒤, 각 방문 장소를 RecyclerView 컴포넌트로 순서에 따라 렌더링합니다.<br>또한, 생성된 타임라인 일정 내에서 사용자가 특정 장소 아이템을 **'삭제(스와이프)'**, **'위치 이동(ItemTouchHelper를 통한 드래그 앤 드롭)'**, 그리고 최종적으로 **'결정(Save/Commit)'** 할 수 있는 UI 제어 로직을 추가하며, 각 장소의 이미지는 `Glide`를 사용하여 최적화 로드합니다. |
| `/home/ubuntu/root/leeyoungkon_projects/Harudiary/app/src/main/java/com/example/harudiary/model/` | `TravelPlanResponse.java` 등 | 신규 생성 (약 3개) | 서버가 반환하는 JSON 형태와 일치하는 계층형 데이터 모델(DTO) 추가 |

---

## 3. 데이터 연동, API 검증 및 AI 라우팅 설계 (고려사항)

**1. 다이어리 앱에서 '여행 일정' 데이터를 정확히 받아 출력하기 위한 구성 방안 및 고려사항**
기존 Harudiary의 일상 기록 기능에 travel 백엔드의 "여행 일정(Day-by-Day)" 데이터를 결합하여 화면에 출력하려면 다음 사항들을 고려하여 프론트엔드를 재구성해야 합니다.

- **계층형 데이터 모델(DTO) 동기화**: 백엔드에서 반환하는 JSON 구조(TravelPlanResponse ➔ DayPlanDto ➔ PlaceDto)와 정확히 일치하는 모델 클래스를 Android 앱에 생성해야 합니다. 특히 장소의 위도/경도, 방문 순서, 예상 소요 시간 등의 필드가 누락 없이 매핑되어야 합니다.
- **타임라인(Timeline) 기반 UI/UX 설계**: 여행 일정은 시간의 흐름에 따라 진행됩니다. Harudiary에 이미 있는 DailyTimelineFragment를 확장하거나 RecyclerView를 중첩(Day 리스트 안에 Place 리스트)하여, 타임라인 형태(예: 점선으로 장소와 장소가 이어지는 UI)로 시각화하는 것이 직관적입니다.
- **비동기 처리 및 상태 관리 (ViewModel + LiveData/StateFlow)**: AI가 일정을 생성하는 데는 몇 초의 지연 시간(Latency)이 발생할 수 있습니다. 사용자가 지루함을 느끼지 않도록 스켈레톤 UI(로딩 애니메이션)를 띄워두고, Retrofit을 통한 백엔드 통신이 완료되면 ViewModel을 통해 화면이 자연스럽게 갱신되도록 구성해야 합니다.
- **사용자 편집(Customization) 고려**: AI가 짜준 일정이 완벽하지 않을 수 있습니다. 앱에서 특정 장소를 삭제하거나 순서를 드래그 앤 드롭(ItemTouchHelper)으로 변경할 수 있는 기능을 고려해야 실사용성이 크게 올라갑니다.

**2. Kakao API의 맛집/여행 정보 지원 여부 비판적 검증**
현재 Harudiary가 사용 중인 Kakao API(카카오 로컬 API 및 지도 API)에 대한 정확한 한계와 지원 범위는 다음과 같습니다.

- **맛집 및 장소 정보 (지원 O, 매우 우수함)**: 카카오 로컬 API의 키워드 검색(v2/local/search/keyword.json)은 카테고리 그룹 코드(예: FD6 - 음식점, CE7 - 카페, AT4 - 관광명소)를 통해 매우 정확한 국내 맛집 및 명소 데이터를 제공합니다. 별점은 직접 제공하지 않으나, 카카오맵 상세 URL을 제공하여 즉시 연동 가능합니다.
- **여행 계획 생성 및 최단 경로 도출 (지원 X, 직접 구현 불가)**: 카카오 로컬 API 자체에는 "여행 계획을 짜주는 기능"이나 "여러 장소를 경유하는 최단 경로(TSP 문제) 알고리즘"이 포함되어 있지 않습니다.
길찾기(내비) API를 쓰면 A에서 B까지의 시간은 알 수 있으나, 여러 장소를 묶어 최적의 동선을 도출하는 것은 API 호출 횟수 낭비와 복잡한 알고리즘(프론트엔드 연산 부하)을 초래합니다.

**3. Gemini API를 활용한 최단 경로 및 시간 도출 (내재적 기능 활용)**
카카오 API의 '경로 최적화' 한계를 극복하기 위해, 백엔드의 Gemini API가 가진 "지리적 추론 및 공간 인지 능력"을 활용해야 합니다. Gemini는 단순한 텍스트 봇이 아니라 내부 학습 데이터에 기반해 각 장소 간의 물리적 거리와 이동 수단별 예상 시간을 꽤 정확히 알고 있습니다.

**Gemini 로직 구성 및 이해:**

- **프롬프트 엔지니어링 (Prompting)**: Spring Boot 백엔드에서 Gemini에게 요청을 보낼 때, 단순히 "제주도 2박 3일 일정 짜줘"가 아니라 다음과 같이 프롬프트를 시스템적으로 강제해야 합니다.
*"제주도 2박 3일 일정을 작성하되, 반드시 **최단 이동 동선(동선 낭비 최소화)**을 고려하여 장소를 배치해 줘. 그리고 각 장소에서 다음 장소까지의 **예상 이동 시간(분 단위)과 이동 수단(도보/차량)**을 JSON 필드로 정확히 포함해."*

- **내재적 라우팅(Routing)**: Gemini는 자체 지식 그래프를 활용해 "성산 일출봉"을 본 후에는 동쪽의 "섭지코지"로 가는 것이 합리적이라는 것을 추론합니다. 이를 통해 외부의 무거운 길찾기 API(Kakao Navi API 등)를 일일이 호출할 필요 없이, AI 단일 호출만으로 완성된 '최적 동선'과 '이동 시간 데이터'를 한 번에 프론트엔드로 전달할 수 있습니다.
- **앱에서의 표현**: 앱은 백엔드(Gemini)가 내려준 JSON 데이터 속 `estimated_travel_time: "15분"` 값을 그대로 타임라인 UI의 장소와 장소 사이 연결선에 텍스트로 뿌려주기만 하면 됩니다.


Phase 1: Backend (travel) 및 DB 구현 체크리스트
[x] 1. 데이터베이스(MySQL) 셋업
[x] mysql 컨테이너에 harudiary_db 스키마 생성
[x] 2. 의존성 및 설정 추가
[x] pom.xml에 MySQL Connector, Spring Data JPA 추가
[x] application.properties에 DB 접속 정보 및 GEMINI_API_KEY 환경 변수 설정
[x] 3. 백엔드 모델(JPA Entity) 및 DTO 구현
[x] User, Diary, DayPlan, Place 엔티티 클래스 생성
[x] 각 엔티티의 Repository 인터페이스 생성
[x] TravelPlanResponse, DayPlanDto, PlaceDto 생성
[x] 4. Gemini API 서비스 및 컨트롤러 구현
[x] Gemini 구조화된 출력(JSON Schema)을 호출하는 GeminiClientService 구현
[x] 응답을 파싱하고 DB에 저장하는 TravelPlanService 구현
[x] 프론트엔드와 통신할 TravelController 구현
[x] 5. 백엔드 Docker 재실행 및 검증
[x] travel 프로젝트 빌드 및 Docker 이미지 생성
[x] docker run으로 컨테이너 실행 (환경변수 주입)
[x] 내부 테스트(curl/Postman)로 응답 검증
Phase 2: 백엔드 API 오케스트레이션 및 파이프라인 구현 (travel)
[ ] 1. 백엔드 외부 API 통신 기반 마련
[ ] RestTemplate 또는 WebClient 빈(Bean) 등록
[ ] application.properties에 Kakao/Weather API 키 설정
[ ] 2. 카카오 & 기상청 API 클라이언트 서비스 구현
[ ] KakaoLocalService (키워드/카테고리 검색, 역지오코딩 구현)
[ ] WeatherApiService (기상청 단기/초단기 호출 및 nx, ny 격자 변환 수학 로직 포팅)
[ ] 3. 컨트롤러 및 핵심 비즈니스 로직(Service) 분기 구현
[ ] EnvController: /api/env/current (위경도 ➔ 주소+기상실황)
[ ] TravelRecommendController: /api/travel/recommend/nearby 및 /api/travel/recommend/diary
[ ] GeminiOrchestrationService: 1차 호출(키워드 추출) 및 2차 호출(일정 생성) 로직 작성. 카카오 결과 상위 15개 Truncate 로직 적용.
[ ] 수학적 Date 분기(UltraSrtNcst vs VilageFcst) 로직 적용
[ ] 4. TravelPlanController 및 DB 동기화 구현
[ ] POST /api/travel/plan/save 엔드포인트 생성
[ ] 수신된 List 순서대로 sequence를 부여하여 MySQL에 영속화(JPA saveAll)
Phase 3: 프론트엔드 Retrofit 이관 및 UI 렌더링 (Harudiary)
[ ] 1. 의존성 및 불필요 클래스 정리
[ ] build.gradle.kts에 Retrofit2, Gson, Glide 추가
[ ] 기존 KakaoLocalAPI.java, WeatherAPI.java 삭제
[ ] 2. 네트워크 통신 클라이언트(Retrofit) 세팅
[ ] RetrofitClient.java 및 TravelApi.java 명세서 작성
[ ] 응답 DTO (TravelPlanResponse, DayPlanDto, PlaceDto) 작성
[ ] 3. RecordActivity 환경 정보 통신 이관 및 버튼 추가
[ ] 기존 카카오/날씨 직접 호출 삭제 ➔ TravelApi.getEnvCurrent() 교체
[ ] 조회 모드 시 하단에 [이 기록 기반으로 여행 일정 생성] 버튼 추가
[ ] 4. HomeFragment 및 DailyTimelineFragment UI 보강
[ ] 홈에 [내 주변 즉석 추천] 플로팅 버튼 추가
[ ] 타임라인 렌더링(RecyclerView)에 서버로부터 받은 travel_time 표시
[ ] ItemTouchHelper 부착 (Swipe 삭제, Drag 순서 변경)
[ ] 하단에 [일정 확정 및 저장] 버튼 추가 및 savePlan() 통신 연동
[ ] 5. SQLite 로컬 DB 완전 탈피
[ ] DBHelper.java와 관련 DAO의 CRUD를 Retrofit 비동기 호출로 교체


System_Architecture_and_Usecase_Explanation.md, app_usecase_orchestration.md)의 API 명세와 실제 안드로이드 프로젝트(Harudiary), 백엔드 프로젝트(travel)의 로직 코드를 비교 분석하여, 실제 안드로이드 어플리케이션 사용자가 어떻게 도커 서버(Spring Boot)에 접속하여 통신하는지 구조와 흐름을 설명해 드리겠습니다.

1. 백엔드 도커 서버의 포트 개방 (travel 프로젝트)
먼저 travel 백엔드 서버가 도커 컨테이너로 어떻게 띄워져 있고, 외부(안드로이드)의 접속을 어떻게 기다리는지 확인해야 합니다.

travel 프로젝트의 

docker-compose.yml
 파일을 살펴보면, travel-server라는 이름의 스프링 부트 컨테이너가 8083 포트를 호스트(사용자의 PC)와 연결(포트 포워딩)하고 있습니다.

yaml
# /home/ubuntu/root/leeyoungkon_projects/travel/docker-compose.yml
services:
  travel-server:
    build: .
    container_name: travel-server
    ports:
      - "8083:8083" # 호스트의 8083 포트로 들어오는 요청을 컨테이너의 8083 포트로 전달
    networks:
      - resttemplateserver-latlang_default
2. 안드로이드 클라이언트의 서버 접속 설정 (Harudiary 프로젝트)
안드로이드 앱 사용자는 REST API 통신을 위해 Retrofit2 라이브러리를 사용하고 있습니다. 안드로이드 에뮬레이터에서 실행 중인 앱이 내 PC(호스트)에서 도커로 돌고 있는 서버에 접속하기 위해 사용하는 특수한 IP가 바로 10.0.2.2 입니다.

Harudiary 프로젝트의 RetrofitClient.java 파일에서 이 핵심 로직을 확인할 수 있습니다.

java
// /home/ubuntu/root/leeyoungkon_projects/Harudiary/app/src/main/java/com/example/harudiary/api/RetrofitClient.java
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class RetrofitClient {
    // 백엔드 컨테이너 포트가 8083 이므로, 에뮬레이터 환경(10.0.2.2) 또는 실제 디바이스 IP 주소 설정 필요
    private static final String BASE_URL = "http://10.0.2.2:8083/";
    private static Retrofit retrofit = null;
    public static Retrofit getClient() {
        if (retrofit == null) {
            // 안드로이드 앱이 통신할 기본 서버 주소와 JSON 변환기(Gson)를 설정합니다.
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL) 
                    .addConverterFactory(GsonConverterFactory.create())
                    // ... (기타 설정)
                    .build();
        }
        return retrofit;
    }
}
💡 10.0.2.2란? 안드로이드 에뮬레이터 자체도 하나의 가상 머신(VM)이기 때문에, 에뮬레이터 안에서 localhost나 127.0.0.1을 호출하면 에뮬레이터 자신을 가리키게 됩니다. 따라서 에뮬레이터를 띄워준 개발자 PC(호스트 시스템)의 로컬호스트에 접근하기 위해 안드로이드가 예약해둔 특수 IP가 10.0.2.2 입니다.

3. 유스케이스 기반의 실제 API 호출 로직
app_usecase_orchestration.md 문서에 명세된 내용 중 [목적 3: 내 일기 기반 맞춤형 여행 일정 딥-생성] (엔드포인트: POST /api/travel/recommend/diary)가 안드로이드 실제 코드에서는 어떻게 도커 서버로 호출되는지 확인해 보겠습니다.

안드로이드 앱의 RecordActivity.java 에서는 앞서 만든 RetrofitClient를 가져와서 비동기 통신(enqueue)으로 도커 서버에 요청을 보냅니다.

java
// /home/ubuntu/root/leeyoungkon_projects/Harudiary/app/src/main/java/com/example/harudiary/activity/RecordActivity.java
import com.example.harudiary.api.RetrofitClient;
import com.example.harudiary.api.TravelApi;
// ...
// 1. Retrofit 클라이언트를 통해 TravelApi 인터페이스의 구현체를 생성
TravelApi api = RetrofitClient.getClient().create(TravelApi.class);
// 2. 도커 서버의 `POST /api/travel/recommend/diary` 엔드포인트로 데이터를 담아 요청 (비동기)
api.recommendByDiary(req).enqueue(new retrofit2.Callback<TravelPlanResponse>() {
    
    @Override // 서버로부터 200 OK 응답(JSON)이 성공적으로 돌아왔을 때
    public void onResponse(@NonNull Call<TravelPlanResponse> call, @NonNull Response<TravelPlanResponse> response) {
        // 백엔드(도커) ➔ 카카오/제미나이 ➔ 백엔드(도커)를 거쳐서 돌아온 JSON 데이터를 안드로이드 UI에 렌더링
    }
    @Override // 통신 실패, 타임아웃, 또는 도커 서버가 꺼져있을 때
    public void onFailure(@NonNull Call<TravelPlanResponse> call, @NonNull Throwable t) {
        // 에러 처리 로직
    }
});
🚀 요약: 전체 접속 파이프라인 (Network Flow)
사용자가 안드로이드 앱에서 버튼을 누를 때부터 도커 서버까지 도달하는 네트워크 접속 흐름은 다음과 같습니다.

[Android UI] 사용자가 일정 생성 등의 동작을 수행합니다. (RecordActivity.java)
[Retrofit HTTP Client] 안드로이드 앱은 목적지 주소를 http://10.0.2.2:8083/api/... 로 설정하여 HTTP 요청을 쏩니다. (RetrofitClient.java)
[에뮬레이터 네트워크 브릿지] 안드로이드 에뮬레이터는 10.0.2.2로 향하는 요청을 호스트 PC(Ubuntu)의 127.0.0.1 (localhost) 로 변환하여 전달합니다.
[Docker Port Mapping] 호스트 PC의 8083 포트로 요청이 도착하면, docker-compose.yml의 포트 매핑 규칙(8083:8083)에 의해 백엔드 컨테이너의 8083 포트로 패킷이 토스됩니다.
[Spring Boot Server] 도커 안에서 돌고 있는 스프링 부트 서버의 컨트롤러(Controller)가 해당 API 요청을 받아 처리하고 응답을 반환합니다.
3:41 PM
그러니까, 지금 내 도커에 실제 다른 사용자가 사용할 수 있는지 묻는거여
3:43 PM
결론부터 말씀드리면 "현재 상태로는 실제 다른 사용자(외부 사용자)가 접속할 수 없습니다."

그 이유는 앞서 보았던 안드로이드 앱의 코드 단 한 줄 때문입니다.

🚫 왜 외부 사용자는 접속하지 못할까요?
안드로이드 프로젝트(Harudiary)의 RetrofitClient.java 파일에 다음과 같이 주소가 고정(하드코딩)되어 있습니다.

java
private static final String BASE_URL = "http://10.0.2.2:8083/";
10.0.2.2는 오직 개발자 PC에 띄워둔 안드로이드 에뮬레이터(가상 스마트폰) 안에서만 작동하는 특수한 가짜 IP입니다.

만약 이 상태로 앱을 빌드해서 친구의 진짜 스마트폰(갤럭시 등)에 깔아주게 되면, 앱은 외부 인터넷을 타고 사용자님의 우분투 서버로 찾아오는 것이 아니라, 친구 스마트폰 기기 내부에서 서버를 찾으려고 시도하다가 연결 실패(Timeout) 에러를 뿜게 됩니다.

🟢 실제 다른 사용자가 접속하게 만들려면 어떻게 해야 하나요?
외부 사용자가 사용자님의 도커 서버를 쓰게 하려면 다음 두 가지 조치가 필요합니다.

1. 안드로이드 앱 코드 수정 (IP 변경)
안드로이드 RetrofitClient.java의 BASE_URL을 사용자님이 도커를 띄워둔 **우분투 서버의 '진짜 공인 IP(Public IP)' 또는 '도메인'**으로 변경한 뒤 앱(APK)을 다시 빌드해야 합니다.

java
// 예시: 우분투 서버의 공인 IP가 123.45.67.89 라면
private static final String BASE_URL = "http://123.45.67.89:8083/";
2. 우분투 서버의 방화벽(포트) 개방
앱이 서버의 IP를 찾아오더라도, 서버의 문이 닫혀있으면 튕겨납니다. 도커는 이미 8083 포트를 열어두고 기다리고 있지만, 우분투 OS 자체의 방화벽(ufw)이나, 만약 AWS/GCP 같은 클라우드를 쓰신다면 클라우드의 '보안 그룹(Security Group)' 설정에서 8083 포트로 외부 접속(Inbound)이 들어올 수 있도록 허용(Open)해 주셔야 합니다.

요약하자면: 현재 도커 자체는 정상적으로 요청을 받을 준비가 되어 있지만, 앱이 찾아가는 목적지 주소(10.0.2.2)가 개발용으로만 설정되어 있기 때문에 외부 사용자는 접속할 수 없는 상태입니다.