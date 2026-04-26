<p align="center">
  <img src="photo/5.png"/>
</p>

# AekioriEats 문서 목차

## 시작 / 운영

| 문서 | 설명 |
|------|------|
| [로컬 시작 가이드](start/README.md) | 인프라 기동 → Terraform → Debezium → 앱 전체 실행 순서 |
| [Kafka / Debezium 운영 정리](infra/kafka-debezium.md) | Outbox-Debezium-Kafka 흐름, connector 등록/재등록, 장애 확인 순서 |
| [Kafka 토픽 Terraform 관리](infra/kafka-terraform.md) | 토픽 IaC 운영 원칙, tfvars 관리, import 절차 |
| [Kafka 토픽 관리 정책](infra/topic-management.md) | 토픽 단일 기준, 관리 경계, 현재 원칙 |
| [Prometheus / Grafana 로컬 구성](infra/prometheus-grafana.md) | 스크랩 대상, 대시보드, 보관 정책 |
| [PromQL 대시보드 템플릿](infra/promql-dashboard-template.md) | 주요 PromQL 쿼리 모음 |
| [Kubernetes 배포 전략](infra/kubernetes-deployment-strategy.md) | API/Consumer/Scheduler 배포 분리 기준과 Redisson lock 운영 원칙 |

## 설계

| 문서 | 설명 |
|------|------|
| [이벤트 설계 (Saga 흐름)](event-design.md) | 주문 생성 → 가게 검증 → 결제 → 포인트 전체 Saga 흐름 |
| [Outbox 설계 메모](outbox-design-note.md) | 서비스별 Outbox 분리 이유, 운영 원칙 |
| [HTTP 멱등 처리 설계](idempotency-http.md) | 주문 API idempotency-key 처리 흐름 |
| [Kafka Consumer 멱등 처리 설계](idempotency-kafka-consumer.md) | 컨슈머 중복 이벤트 처리 전략 |
| [Order Timeout Compensation](order-timeout-compensation.md) | 주문 timeout 보상 처리와 Redisson 분산락 설계 |

## 서비스

| 서비스 | 설명 | 문서 |
|--------|------|------|
| [Auth](../Auth/README.md) | JWT 인증, 리프레시 토큰 로테이션, Rate Limit | - |
| [User](../User/README.md) | 사용자 프로필 Projection, 이벤트 Dedup 처리 | - |
| [Order](../Order/README.md) | 주문 생성/상태 관리, Redis 멱등성, Outbox | - |
| [Store](../Store/README.md) | 상점/메뉴 관리, 주문 검증 이벤트 컨슈머 | [도메인 설계](../Store/store-domain-design.md) |
| [Gateway](../Gateway/README.md) | JWT 검증, 라우팅, 헤더 인젝션/위조 방지 | - |
| [Payment](../Payment/README.md) | 결제 대기 생성, PortOne 연동 결제 확정 | - |
| [Point](../Point/README.md) | 포인트 잔액/원장, 차감 이벤트 처리 | - |
