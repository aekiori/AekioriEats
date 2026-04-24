# Auth 서비스

토큰 기반 인증을 담당하는 도메인 서비스다.

## 주요 기능
- 로그인 (`이메일 + 비밀번호`)
- 액세스 토큰 발급 (JWT)
- 리프레시 토큰 발급/로테이션
- 리프레시 토큰 재사용 탐지 (reuse detection)
- 로그아웃 (리프레시 토큰 폐기)
- 로그인/회원가입 Rate Limit (IP + 계정)

## API

### Swagger
- UI: `http://localhost:8084/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8084/v3/api-docs`

### 1) 회원가입
`POST /api/v1/auth/signup`

요청 예시:
```json
{
  "email": "user@example.com",
  "password": "password1234",
  "nickname": "애기오리"
}
```

응답 예시:
```json
{
  "userId": 1,
  "email": "user@example.com",
  "nickname": "애기오리"
}
```

### 2) 로그인
`POST /api/v1/auth/login`

요청 예시:
```json
{
  "email": "auth-user@example.com",
  "password": "password1234"
}
```

응답 예시:
```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 1800
}
```

### 3) 토큰 재발급
`POST /api/v1/auth/refresh`

요청 예시:
```json
{
  "refreshToken": "..."
}
```

### 4) 로그아웃
`POST /api/v1/auth/logout`

요청 예시:
```json
{
  "refreshToken": "..."
}
```

## 토큰 정책
- 액세스 토큰: JWT (`auth.jwt.access-token-expiration-seconds`)
- 리프레시 토큰: DB 저장 (`auth_refresh_tokens` 테이블)
- 리프레시 로테이션: 재발급 시 기존 리프레시 토큰 폐기
- 재사용 탐지: 이미 폐기된 리프레시 토큰 재사용 시 `AUTH_REFRESH_TOKEN_REUSE_DETECTED` 반환 + 해당 사용자 활성 리프레시 토큰 전부 폐기

## Rate Limit 정책
- 로그인/회원가입 모두 `IP`와 `account(email)` 버킷을 동시에 검사한다.
- 제한 초과 시 `429 TOO_MANY_REQUESTS`, `AUTH_RATE_LIMITED`를 반환한다.
- 설정 키:
  - `auth.rate-limit.signup.ip.*`
  - `auth.rate-limit.signup.account.*`
  - `auth.rate-limit.login.ip.*`
  - `auth.rate-limit.login.account.*`
## 로그인 시 User 정보 연동 전략

| 방식 | 설명 | 적합한 상황 | 추천도           |
| --- | --- | --- |---------------|
| API Composition | Auth가 User Service에 직접 API 호출 | User 데이터가 자주 필요할 때 | ★★★☆☆ (보통)    |
| 이벤트 기반 (CQRS) | User 변경 이벤트 발행 → Auth가 구독해서 Auth 전용 DB/Read Model 유지 | 읽기 성능이 중요하고 최신성 허용 가능할 때 | ★★★★★ (좋음)    |
| 직접 DB 조회 | Auth가 User DB를 직접 조회/JOIN | 사용 금지 | ★☆☆☆☆ (최악) 🤯 |

- 도메인 분리 원칙상 `Auth -> User DB` 직접 접근은 지양한다.
- 현재 로그인 조회 원본은 `delivery_auth.auth_users`다.


## 설정
- 메인 설정 파일: `Auth/src/main/resources/application.yaml`
- 필수 시크릿:
  - `AUTH_JWT_SECRET`

## 데이터베이스
- 테이블: `auth_refresh_tokens`
- Flyway 마이그레이션: `Auth/src/main/resources/db/migration/V1__init_auth_schema.sql`

## 테스트 실행
```cmd
.\gradlew.bat test
```

## Auth ↔ User 이벤트 설계 (2026-04)
- 회원가입 책임은 `Auth`가 가진다.
- `Auth`는 회원가입 성공 시 Outbox에 `UserCreated` 이벤트를 적재한다.
- `User`는 이 이벤트를 소비해서 로컬 `users` projection row를 만든다.

### 이벤트 Envelope
```json
{
  "eventId": "uuid",
  "eventType": "UserCreated",
  "schemaVersion": 1,
  "occurredAt": "2026-04-07T08:00:00",
  "userId": 1,
  "email": "user@example.com",
  "status": "ACTIVE"
}
```

### 처리 원칙
- `eventId`: 중복 소비 추적/디버깅용으로 필수
- `schemaVersion`: 이벤트 포맷 진화 대비용
- `occurredAt`: 비즈니스 이벤트 발생 시점 추적용
- 생성 이벤트(`UserCreated`)는 생성 통로로만 사용하고, 데이터 정정은 별도 이벤트로 분리한다.
