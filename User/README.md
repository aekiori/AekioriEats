# User 서비스
사용자 프로필/상태를 관리하는 도메인 서비스다.

## 주요 기능
- 사용자 조회
- 사용자 상태 변경 (`ACTIVE`, `LOCKED`, `DEACTIVATED`)
- `AuthUserCreated` 이벤트 소비 후 로컬 `users` projection 생성
- 중복 이벤트 소비 차단(dedup)

## API

### Swagger
- UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

### 1) 사용자 생성 (주로 내부/운영 용도)
`POST /api/v1/users`

요청 예시:
```json
{
  "email": "hello@example.com"
}
```

응답 예시:
```json
{
  "userId": 1,
  "email": "hello@example.com",
  "status": "ACTIVE",
  "createdAt": "2026-04-06T00:00:00"
}
```

### 2) 사용자 조회
`GET /api/v1/users/{userId}`

### 3) 사용자 상태 변경
`PATCH /api/v1/users/{userId}/status`

요청 예시:
```json
{
  "status": "LOCKED"
}
```

## 인가 정책 (P0)
- 인증 주체는 Gateway 주입 헤더(`X-User-Id`, `X-User-Role`) 기준으로만 판단한다.
- `GET /api/v1/users/{userId}`: 본인만 조회 가능.
- `PATCH /api/v1/users/{userId}/status`: 본인만 변경 가능.
- 소유권 위반은 `403 FORBIDDEN` + `FORBIDDEN_RESOURCE_ACCESS`를 반환한다.
- 인증 주체 헤더 누락/비정상은 `401 UNAUTHORIZED` + `UNAUTHORIZED_PRINCIPAL`을 반환한다.

## Auth 이벤트 소비 정책 (2026-04)
- `User`는 `Auth`가 발행한 `AuthUserCreated`를 소비한다.
- 생성 이벤트는 **INSERT only + idempotent** 원칙으로 처리한다.
- 기존 `users` 데이터 overwrite는 금지한다.

### Dedup 전략
- `processed_events(event_id PK)` 테이블로 이벤트 중복 소비를 차단한다.
- 같은 `eventId`가 다시 오면 projection 반영 없이 skip 한다.
- dedup insert와 user insert는 같은 트랜잭션에서 처리한다.

### Projection SQL 원칙
```sql
INSERT INTO users (...)
VALUES (...)
ON DUPLICATE KEY UPDATE id = id; -- no-op
```

## 데이터베이스
- 기본 스키마: `delivery_user`
- 테이블: `users`, `processed_events`
- Flyway 마이그레이션: `User/src/main/resources/db/migration`
- Flyway 히스토리 테이블: `flyway_schema_history_user`

## 테스트 실행
```cmd
.\gradlew.bat test
```

## 참고
- Bloom Filter 이메일 중복 최적화는 현재 `Auth` 도메인에 있다.
- 로그인 식별자 정책(이메일/ID)은 `Auth` 도메인 문서를 기준으로 본다.
