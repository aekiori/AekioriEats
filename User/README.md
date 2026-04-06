# User 서비스
사용자 생명주기와 상태를 관리하는 도메인 서비스다.

## 주요 기능
- 회원가입 (`이메일 + 비밀번호`)
- 이메일 중복 확인
- 회원 정보 조회
- 회원 상태 변경 (`ACTIVE`, `LOCKED`, `DEACTIVATED`)

## API

### 1) 이메일 중복 확인
`GET /api/v1/users/email/exists?email={email}`

응답 예시:
```json
{
  "email": "hello@example.com",
  "exists": true
}
```

### 2) 회원가입
`POST /api/v1/users`

요청 예시:
```json
{
  "email": "hello@example.com",
  "password": "password1234"
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

### 3) 회원 조회
`GET /api/v1/users/{userId}`

### 4) 회원 상태 변경
`PATCH /api/v1/users/{userId}/status`

요청 예시:
```json
{
  "status": "LOCKED"
}
```

## Bloom Filter
이메일 중복 확인 API에서 DB 부하를 줄이기 위한 학습용 PoC다.

- 위치: `UserEmailBloomFilter`
- 전략:
  - `mightContain=false`면 DB 조회 없이 `exists=false` 바로 반환
  - `mightContain=true`면 DB `existsByEmail`로 최종 확인
  - 회원가입 성공 시 Bloom Filter에 이메일 추가
  - 최종 정합성은 DB `UNIQUE(email)` 제약으로 보장

### 워밍업 동작
- 앱 시작 후 `ApplicationReadyEvent`에서 비동기 워밍업 수행
- 워밍업 완료 전에는 자동으로 DB 조회 fallback
- 워밍업 실패해도 서비스는 정상 기동되고 DB fallback으로 동작
- 워밍업 실패 시 재시도는 넣지 않음
  - 블룸 필터는 “있으면 좋고 없으면 그만”인 **아님말고** 식 성능 최적화 계층이다
  - 실패해도 정합성/기능에는 영향 없고, 그냥 DB 조회 경로로 동작한다
  - 재시도 로직까지 넣으면 오히려 오버엔지니어링인 것 같아서 현재는 제외했다

### 설정값 (`application.yaml`)
```yaml
user:
  bloom:
    enabled: true
    expected-insertions: 5000000
    fpp: 0.01
    warmup-batch-size: 10000
```

### 주의사항
- Bloom Filter는 삭제를 지원하지 않아서 탈퇴/삭제된 이메일이 필터에 남을 수 있다
- 이 경우 false positive로 DB 확인 경로를 타고, 정합성에는 영향 없다

## 운영 메모
- 블룸 필터는 최적화 계층이고 정답 소스는 DB다
- 고QPS 환경 아니면 과한 튜닝보다 DB 인덱스/쿼리 플랜 유지가 우선이다
- `expected-insertions`, `fpp`는 트래픽/회원 수 늘면 주기적으로 다시 맞춰야함

## 데이터베이스
- 기본 스키마: `delivery_user`
- 테이블: `users`
- Flyway 마이그레이션: `User/src/main/resources/db/migration/V1__init_user_schema.sql`
- Flyway 히스토리 테이블: `flyway_schema_history_user`

## 테스트 실행
```cmd
.\gradlew.bat :User:test
```
