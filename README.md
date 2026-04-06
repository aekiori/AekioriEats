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

### 해당 프로젝트에서 다루게 될 주제들
- **DDD**를 통한 명확한 도메인 경계 설계와 응집도 강화
- **Kafka + Debezium** 기반 Event-Driven Architecture와 Transactional Outbox Pattern
- **Saga Pattern**을 활용한 분산 트랜잭션 관리
- **Idempotency(멱등성)** 보장 및 서비스 간 결합도 최소화
- **Redis** 기반 분산락 및 캐싱 전략
- **API Gateway** (Spring Cloud Gateway) 기반 공통 인증/인가/라우팅
- **Bloom Filter** 기반 이메일 중복 확인 성능 최적화 PoC
- **Distributed Tracing** (Zipkin)으로 서비스 간 요청 흐름 추적
- **Circuit Breaker** (Resilience4j)로 서비스 간 장애 전파 방지
- **CQRS** - 조회/명령 분리
- **Elasticsearch** 기반 메뉴/상점 검색

<p align="center">
  <img src="photo/3.png" width="45%" />
  <img src="photo/4.png" width="45%" />
</p>

## Domains

### 우선 구현
- `Order` - **개발 중**
- `User`- **개발 중**
- `Auth`- **개발 중**
- `Payment`
- `Point`
- `Store`

이후에는 도메인 간 연결과 운영 복잡도가 더 커지는 만큼, 공부도 더 하면서 확장 방향을 신중하게 가져갈 예정이다.

다만 그 시점에는 AI가 상당 부분을 대신하고 있을지도 모르겠다.

### 추후 확장 예정
- `Promotion / Coupon`
- `Rider Delivery`
- `Notification`
- `Inventory`
- `Review`
- `...`



## 인프라 구성

현재 프로젝트는 Docker Compose 기준으로 아래 구성 요소를 함께 사용한다.

- MySQL
- Redis
- Kafka
- Kafka Connect + Debezium
- Kafka UI



## 로컬 실행

```cmd
docker compose up -d --build
```

Kafka Connect가 준비된 뒤 connector 등록:

```cmd
curl -X POST -H "Content-Type: application/json" --data-binary @infra\debezium\order-outbox-connector-smt.json http://localhost:8083/connectors
```

connector 상태 확인:

```cmd
curl http://localhost:8083/connectors/order-outbox-connector/status
```

내부 API 호출 시에는 `X-Internal-Api-Key` 헤더가 필요하다.

## 문서

- [Kafka / Debezium 운영 정리](docs/kafka-debezium.md)
- [Order 서비스 문서](Order/README.md)
