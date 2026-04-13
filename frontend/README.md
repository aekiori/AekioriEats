# frontend (Auth/User/Store/Order MVP)

Quick Next.js client focused on `Auth + User + Store + Order` flow.

## Features
- Signup: `POST /api/v1/auth/signup`
- Login: `POST /api/v1/auth/login`
- Check email duplicate: `GET /api/v1/auth/email/exists`
- Refresh token: `POST /api/v1/auth/refresh`
- Logout: `POST /api/v1/auth/logout`
- Read my profile: `GET /api/v1/users/{userId}`
- Owner store create: `POST /api/v1/owner/stores`
- Category list: `GET /api/v1/stores/categories`
- Owner menu group create: `POST /api/v1/owner/stores/{storeId}/menu-groups`
- Owner menu create: `POST /api/v1/owner/stores/{storeId}/menus`
- Store search: `GET /api/v1/stores/search`
- Order create: `POST /api/v1/orders` (`X-Idempotency-Key` required)
- Order detail: `GET /api/v1/orders/{orderId}`
- Store detail (menu page): `GET /api/v1/stores/{storeId}`

## Architecture
- Browser -> Next route handler proxy (`/api/backend/...`) -> API Gateway (`8088`)
- You can test in browser without adding separate CORS config first.

## Run
1. Copy `frontend/.env.local.example` to `.env.local`
2. Edit `BACKEND_BASE_URL` if needed (default: `http://localhost:8088`)
3. Start

```bash
cd frontend
npm install
npm run dev
```

## Run with Docker Compose
```bash
docker compose --env-file docker/app/.env.app -f docker/app/compose.app.yml up -d frontend-service
```

Open:
- http://localhost:3001

Default backend target in container:
- `http://host.docker.internal:8088` (Gateway)

## Pages
- `/` : entry + session status
- `/auth/signup` : signup + auto login
- `/auth/login` : login
- `/me` : decode JWT payload to userId, then request profile
- `/store/owner` : owner store register + menu group/menu create
- `/store/detail` : Coupang Eats style menu detail page (group/item/options)
- `/order/create` : order create + order detail + store search
