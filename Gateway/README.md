# Gateway 서비스
도메인 서비스 앞단에서 공통 인증/라우팅을 담당하는 API Gateway다.

## 핵심 역할
- `/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/orders/**` 라우팅
- JWT 검증 (Auth와 동일한 `AUTH_JWT_SECRET` 사용)
- 검증 성공 시 내부 헤더 `X-User-Id` 주입

## 기본 포트
- `8088`

## 기본 라우팅 대상
- Auth: `http://localhost:8084`
- User: `http://localhost:8082`
- Order: `http://localhost:8081`

환경변수로 변경 가능:
- `GATEWAY_AUTH_SERVICE_URI`
- `GATEWAY_USER_SERVICE_URI`
- `GATEWAY_ORDER_SERVICE_URI`

## 실행
```cmd
.\gradlew.bat bootRun
```

## 보안 하드닝 메모
- `Authorization` 헤더는 `Bearer ` prefix 제거 후 `trim()` 처리한다.
- 그래서 `Bearer {token}` 뿐 아니라 `Bearer  {token}`(공백 2개 이상)도 정상 파싱된다.
- 요청 초입에서 외부 입력 `X-User-Id`, `X-User-Role` 헤더는 제거한다.
- JWT 검증 성공 후에만 Gateway가 `X-User-Id`/`X-User-Role`를 다시 주입한다.
- 목적은 클라이언트가 헤더를 위조해서 권한을 우회하는 케이스를 막는 것이다.
- JWT 검증 실패는 예외 타입별로 분기해서 401 JSON(`timestamp`, `path`, `code`, `message`, `errors`)으로 내려준다.
- `AUTH_MISSING_TOKEN`: Authorization 헤더 누락/형식 오류
- `AUTH_TOKEN_EXPIRED`: 만료 토큰
- `AUTH_INVALID_TOKEN`: 변조/형식 불량/빈 토큰
- `AUTH_UNSUPPORTED_TOKEN`: 미지원 토큰 형식
- `AUTH_TOKEN_VALIDATION_FAILED`: 기타 검증 실패

## 하위 서비스 계약
- Gateway는 인증 성공 후에만 `X-User-Id`, `X-User-Role`를 주입한다.
- `Order`, `User` 서비스는 이 헤더를 인증 주체로 강제해서 인가를 수행한다.
- 즉 하위 서비스는 body/path/query의 `userId` 단독 값을 신뢰하지 않는다.
- 외부 트래픽은 반드시 Gateway 경유로 들어오게 운영해야 한다.
