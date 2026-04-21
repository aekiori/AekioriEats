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
- `...`



## 인프라 구성

현재 프로젝트는 Docker Compose 기준으로 아래 구성 요소를 함께 사용한다.

- MySQL
- Redis
- Kafka
- Kafka Connect + Debezium
- Kafka UI
- Prometheus
- Grafana

# AekioriEats의 문서 목차는 아래를 참고
[문서 목차](docs/readme.md)
