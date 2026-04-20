# Frontend

`Auth + User + Store + Order + Payment` 흐름을 확인하기 위한 Next.js 클라이언트다.
유저 관점의 가게 탐색/장바구니/결제 화면과 사장 주문 처리 화면을 포함한다.

## 기능
- 회원가입: `POST /api/v1/auth/signup`
- 로그인: `POST /api/v1/auth/login`
- 이메일 중복 확인: `GET /api/v1/auth/email/exists`
- 토큰 재발급: `POST /api/v1/auth/refresh`
- 로그아웃: `POST /api/v1/auth/logout`
- 내 프로필 조회: `GET /api/v1/users/{userId}`
- 카테고리/가게 목록 조회
- 가게 검색: `GET /api/v1/stores/search`
- 가게 상세/메뉴 조회: `GET /api/v1/stores/{storeId}`
- 장바구니 담기/수량 변경
- 주문 생성: `POST /api/v1/orders` (`X-Idempotency-Key` 필요)
- 주문 상세 조회: `GET /api/v1/orders/{orderId}`
- 포트원 카카오페이 결제창 호출
- 결제 confirm: `POST /api/v1/payments/confirm`
- 사장 주문 수락/거절 처리

## 구조
- Browser -> Next route handler proxy (`/api/backend/...`) -> API Gateway (`8088`)
- 브라우저에서 바로 테스트할 수 있게 Next route handler가 백엔드 프록시 역할을 한다.
- 그래서 백엔드 서비스마다 CORS 설정을 따로 먼저 붙이지 않아도 된다.

## 실행
1. `frontend/.env.local.example`을 `.env.local`로 복사한다.
2. 필요하면 `BACKEND_BASE_URL`을 수정한다. 기본값은 `http://localhost:8088`이다.
3. 프론트를 실행한다.

```bash
cd frontend
npm install
npm run dev
```

## Docker Compose 실행
```bash
docker compose --env-file infra/docker/app/.env.app -f infra/docker/app/compose.app.yml up -d frontend-service
```

접속:
- http://localhost:3001

컨테이너 내부 기본 백엔드 대상:
- `http://host.docker.internal:8088` (Gateway)

## 페이지
- `/`: 메인/가게 목록/세션 상태
- `/auth/signup`: 회원가입 + 자동 로그인
- `/auth/login`: 로그인
- `/me`: JWT payload에서 userId를 읽고 프로필 조회
- `/store/detail`: 배달앱 스타일 가게 상세/메뉴 화면
- `/cart`: 장바구니/주문 생성
- `/payment/checkout`: 포트원 카카오페이 결제/confirm/주문 상태 확인
- `/store/owner`: 사장 주문 수락/거절 화면
