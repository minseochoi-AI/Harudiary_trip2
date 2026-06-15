# 🗄️ Harudiary 통합 ERD (Entity-Relationship Diagram)

백엔드 데이터베이스(`harudiary_db`)의 MySQL Entity 구조와 각 객체 간의 연관 관계를 나타낸 ERD 및 스키마 명세입니다.

```mermaid
erDiagram
    USER ||--o{ DIARY : "작성 (writes)"
    USER ||--o{ FRIEND : "user1_id"
    USER ||--o{ FRIEND : "user2_id"
    USER ||--o{ FRIEND_REQUEST : "from_user_id"
    USER ||--o{ FRIEND_REQUEST : "to_user_id"
    USER ||--o{ REACTION : "from_user_id"
    USER ||--o{ COMMENT : "from_user_id"

    DIARY ||--o{ DAY_PLAN : "포함 (contains)"
    DIARY ||--o{ REACTION : "받음 (receives)"
    DIARY ||--o{ COMMENT : "받음 (receives)"

    DAY_PLAN ||--o{ PLACE : "가짐 (has)"

    USER {
        String id PK "고유 ID 매핑"
        String nickname "사용자 식별 닉네임"
    }

    DIARY {
        Long id PK "자동 생성 식별자"
        String user_id FK "작성자 고유 ID"
        String title "다이어리 제목"
        String content "다이어리 내용"
        String date "기록 일자 (YYYY-MM-DD)"
        boolean is_plan "여행 계획 여부 플래그"
    }

    DAY_PLAN {
        Long id PK "일차 식별자"
        Long diary_id FK "어느 다이어리/계획에 속하는지"
        Integer day_number "N일차"
    }

    PLACE {
        Long id PK "장소 식별자"
        Long day_plan_id FK "속한 N일차 정보"
        String place_name "장소명"
        Integer travel_time "이동 소요 시간"
        Integer sequence "배치 순서"
    }

    FRIEND {
        Long id PK
        String user_id_1 FK
        String user_id_2 FK
    }

    FRIEND_REQUEST {
        Long id PK
        String from_user_id FK
        String to_user_id FK
        String status "PENDING, ACCEPTED, REJECTED"
    }
```

## 핵심 설계 고려사항
1. **데이터 분리 무결성**: Diary 테이블 내 `is_plan` 플래그를 통해, 통계(스트릭)용 과거 데이터와 미래의 '여행 계획' 데이터가 섞이지 않도록 방어 로직을 구성.
2. **연관성 삭제 처리 (JPA)**: DayPlan과 Place 간의 순서(`sequence`) 조정 및 삭제 시 고아 객체 누수를 방지하기 위해 `orphanRemoval = true`가 적용된 1:N 구조 설계.
3. **사용자 단순화**: 별도의 Password 인증 대신 `String id`를 Primary Key로 두어 복잡성을 최소화하고 소셜 관계를 직관적으로 구성.
