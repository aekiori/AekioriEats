# Auth 서비스

토큰 기반 인증을 담당하는 도메인 서비스입니다.

## 주요 기능
- 로그인 (`이메일 + 비밀번호`)
- 액세스 토큰 발급 (JWT)
- 리프레시 토큰 발급/로테이션
- 로그아웃 (리프레시 토큰 폐기)

## API

### 1) 로그인
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

### 2) 토큰 재발급
`POST /api/v1/auth/refresh`

요청 예시:
```json
{
  "refreshToken": "..."
}
```

### 3) 로그아웃
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
## 로그인 시 User 정보 연동 전략

| 방식 | 설명 | 적합한 상황 | 추천도           |
| --- | --- | --- |---------------|
| API Composition | Auth가 User Service에 직접 API 호출 | User 데이터가 자주 필요할 때 | ★★★☆☆ (보통)    |
| 이벤트 기반 (CQRS) | User 변경 이벤트 발행 → Auth가 구독해서 Auth 전용 DB/Read Model 유지 | 읽기 성능이 중요하고 최신성 허용 가능할 때 | ★★★★★ (좋음)    |
| 직접 DB 조회 | Auth가 User DB를 직접 조회/JOIN | 사용 금지 | ★☆☆☆☆ (최악) 🤯 |

- 도메인 분리 원칙상 `Auth -> User DB` 직접 접근은 지양한다.
- 현재 로그인 조회 원본은 `delivery_auth.auth_users` 이다.


## 설정
- 메인 설정 파일: `Auth/src/main/resources/application.yaml`
- 필수 시크릿:
  - `AUTH_JWT_SECRET`

## 데이터베이스
- 테이블: `auth_refresh_tokens`
- Flyway 마이그레이션: `Auth/src/main/resources/db/migration/V1__init_auth_schema.sql`

## 테스트 실행
```cmd
.\gradlew.bat :Auth:test
```
