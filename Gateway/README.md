# Gateway 서비스
도메인 서비스 앞단에서 공통 인증/라우팅을 담당하는 API Gateway다.

## 핵심 역할
- `/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/orders/**` 라우팅
- JWT 검증 (Auth와 동일한 `AUTH_JWT_SECRET` 사용)
- 검증 성공 시 내부 헤더 `X-User-Id` 주입

## 기본 포트
- `8088`

## 기본 라우팅 대상
- Auth: `http://localhost:8083`
- User: `http://localhost:8082`
- Order: `http://localhost:8081`

환경변수로 변경 가능:
- `GATEWAY_AUTH_SERVICE_URI`
- `GATEWAY_USER_SERVICE_URI`
- `GATEWAY_ORDER_SERVICE_URI`

## 실행
```cmd
.\gradlew.bat :Gateway:bootRun
```
