<p align="center">
  <img src="photo/1.png" width="45%" />
  <img src="photo/2.png" width="45%" />
</p>

# AekioriEats

## Spring Boot 기반 배달 마이크로서비스 프로젝트
시장에서 점점 더 요구하는 Spring 생태계와 분산 시스템 역량을 제대로 쌓아보고 싶어서 시작한 프로젝트다.

처음엔 “가볍게 CRUD나 찍먹해보자” 하며 작게 시작한 프로젝트지만 
결국 “이왕 하는 김에 공부도 할 겸 가보자”가 되어버린 구현 기록이다.


주문, 유저, 인증, 결제, 포인트, 상점 등등.. 앞으로 계속 확장할 계획이고, 
솔직히 어디까지 갈지는 나도 잘 모르겠다.

`AekioriEats`는 단순한 기능 구현을 넘어, 도메인 모델링과 서비스 간 연결, 그리고 이를 뒷받침하는 인프라 구성까지 함께 다루기 위해 만든 프로젝트다.

### 구현된 주제들
- **DDD** 기반 도메인 경계 설계와 응집도 강화
- **Kafka + Debezium** 기반 Event-Driven Architecture와 Transactional Outbox Pattern
- **Saga Pattern** 기반 분산 트랜잭션 관리
- **Idempotency(멱등성)** 보장 및 서비스 간 결합도 최소화
- **Redis** 기반 분산락 및 캐싱 전략
- **API Gateway** (Spring Cloud Gateway) 기반 공통 인증/인가/라우팅
- **Bloom Filter** 기반 이메일 중복 확인 성능 최적화 PoC
- **PortOne** 연동 결제/환불 처리 (카카오페이 테스트 결제 및 취소)
- **CQRS** - Store 서비스 조회/명령 분리

### 추후 추가 예정
- **Distributed Tracing** (Micrometer + Zipkin)
- **Circuit Breaker** (Resilience4j)
- **Elasticsearch** 기반 메뉴/상점 통합 검색

### 관측(Observability) 고도화 예정
- 지금은 시스템 메트릭(RPS/Latency/Error/JVM/GC) 중심 대시보드를 우선 운영한다.
- 다음 단계로 비즈니스 메트릭(주문 생성/실패, Outbox 미처리 건수, 인증 실패율)을 추가할 예정이다.
- 알림(Alert)도 붙일 예정이다. 예: Outbox backlog 증가, 5xx 비율 급증, p95 지연 시간 임계치 초과.
- 방향은 `지표 수집 -> 대시보드 시각화 -> 임계치 알림 -> 장애 대응` 흐름 완성이다.

<p align="center">
  <img src="photo/3.png" width="45%" />
  <img src="photo/4.png" width="45%" />
</p>

## Domains

### 구현 완료
- [`Auth`](Auth/README.md) - JWT 인증, 리프레시 토큰 로테이션, Rate Limit
- [`User`](User/README.md) - 사용자 프로필 Projection, 이벤트 Dedup 처리
- [`Order`](Order/README.md) - 주문 생성/상태 관리, Redis 멱등성, Outbox
- [`Store`](Store/README.md) - 상점/메뉴 관리, 주문 검증 이벤트 컨슈머
- [`Gateway`](Gateway/README.md) - JWT 검증, 라우팅, 헤더 인젝션/위조 방지
- [`Payment`](Payment/README.md) - 결제 대기 생성, PortOne 연동 결제 확정/환불
- [`Point`](Point/README.md) - 포인트 잔액/원장, 차감 이벤트 처리

### 추후 확장 예정
- `Promotion / Coupon`
- `Rider Delivery`
- `Notification`
- `Review`



## 인프라 구성

현재 프로젝트는 Docker Compose 기준으로 아래 구성 요소를 함께 사용한다.

- MySQL
- Redis
- Kafka
- Kafka Connect + Debezium
- Kafka UI
- Prometheus
- Grafana



## 로컬 실행

처음 환경을 띄울 때는 아래 문서를 기준으로 진행한다.

- [로컬 시작 가이드](docs/start/README.md)

요약 명령:

```cmd
docker compose --env-file infra/docker/infra/.env.infra -f infra/docker/infra/compose.infra.yml up -d --build
cd infra\terraform\kafka
terraform init
terraform plan
terraform apply
cd ..\..\..
infra\debezium\register-all-outbox-connectors.cmd
docker compose --env-file infra/docker/app/.env.app -f infra/docker/app/compose.app.yml up -d --build
```

## DB/Debezium 커넥터 운영 메모
- 실무 기준은 서비스별로 DB 인스턴스를 분리하고, 커넥터도 서비스별로 분리하는 게 원칙인 것 으로 알고있다. 
(MSA/DDD 관점에서의 가장 이상적인 형태)
  - 정확히 모름.  네카오급 덩치 아니면 현실적인 문제(비용, 운영) 등등.. 땜에 꼭 그렇지만은 않다 카더라
- 이 프로젝트 로컬 환경은 작성자 노트북 사양 이슈로 DB/커넥터를 최소 구성으로 합쳐서 돌렸다.
- 로컬 목적은 운영 토폴로지 재현보다 기능 검증 우선이다.
- `schema.history.internal.kafka.topic`(예: `schemahistory.delivery.order`)은 Debezium 내부 메타 토픽이다.
- 브로커 `auto.create.topics.enable=true` + 토픽 생성 권한(ACL)이 있으면 자동 생성될 수 있지만, 환경에 따라 자동 생성이 안 될 수 있다.
- 이 토픽이 없거나 삭제되면 connector task가 `The db history topic is missing`로 `FAILED` 난다.
- 실무/로컬 모두 history 토픽은 삭제하지 않는 걸 기본으로 두고, 필요하면 미리 수동 생성(pre-create)해두는 게 안전하다.
- 복구할 땐 커넥터 `snapshot.mode=schema_only_recovery`로 재설정 후, history 토픽 생성/확인하고 connector를 restart 한다.


Order 내부 운영 API는 제거했고, 현재는 사용자/사장 플로우 중심으로만 구성했다.

## 인증/인가 경계 (2026-04)
- 외부 요청은 Gateway를 통해서만 들어온다고 가정한다.
- Gateway가 JWT 검증 후 주입한 `X-User-Id`, `X-User-Role`만 신뢰한다.
- `Order`, `User` 도메인은 클라이언트가 보낸 `userId`만 보고 처리하지 않는다.
- 본인 리소스 규칙 위반 시 `403 FORBIDDEN` + `FORBIDDEN_RESOURCE_ACCESS`를 반환한다.
- 인증 주체 헤더가 없거나 비정상이면 `401 UNAUTHORIZED` + `UNAUTHORIZED_PRINCIPAL`을 반환한다.
- 관리자 예외 권한 없이 사용자/사장 본인 기준으로만 인가한다.

## 문서

### 시작 / 운영
- [로컬 시작 가이드](docs/start/README.md)
- [Kafka / Debezium 운영 정리](docs/infra/kafka-debezium.md)
- [Kafka 토픽 Terraform 관리](docs/infra/kafka-terraform.md)
- [Prometheus / Grafana 로컬 구성](docs/infra/prometheus-grafana.md)
- [도메인 대시보드 PromQL 템플릿](docs/infra/promql-dashboard-template.md)

### 설계
- [이벤트 설계 (Saga 흐름)](docs/event-design.md)
- [Outbox 설계 메모](docs/outbox-design-note.md)
- [멱등성 처리 설계](docs/Idempotency.md)
- [Store 도메인 설계](Store/store-domain-design.md)

### 서비스
- [Auth](Auth/README.md) · [User](User/README.md) · [Order](Order/README.md) · [Store](Store/README.md) · [Gateway](Gateway/README.md) · [Payment](Payment/README.md) · [Point](Point/README.md)

## Kafka Message Format

Outbox 이벤트는 Debezium EventRouter SMT를 통해 Kafka로 발행된다.

예시 토픽:
- `outbox.event.OrderCreated`
- `outbox.event.OrderValidated`
- `outbox.event.OrderRejected`
- `outbox.event.PaymentRequested`
- `outbox.event.PaymentSucceeded`
- `outbox.event.PaymentFailed`
- `outbox.event.PaymentRefunded`
- `outbox.event.StoreOrderAccepted`
- `outbox.event.StoreOrderRejected`

메시지 구조 예시:

```json
{
  "topic": "outbox.event.OrderCreated",
  "partition": 0,
  "offset": 0,
  "key": {
    "payload": "1"
  },
  "headers": {
    "id": "1a2cfb6d-979e-4166-b9fb-a35f84207e96",
    "eventType": "OrderCreated",
    "aggregateId": "1"
  },
  "value": {
    "eventId": "1a2cfb6d-979e-4166-b9fb-a35f84207e96",
    "eventType": "OrderCreated",
    "orderId": 1,
    "storeId": 3,
    "userId": 1,
    "totalAmount": 19900,
    "usedPointAmount": 0,
    "finalAmount": 19900,
    "occurredAt": "2026-04-15T21:16:04.9481829"
  }
}
```

토픽은 Terraform에서 명시적으로 관리하고, Kafka Connect 내부 토픽과 Debezium schema history 토픽은 각 컴포넌트가 관리한다.
