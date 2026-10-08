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

## 운영 / 보안 메모

### Trace ID 전파

Gateway는 모든 외부 요청에 `X-Trace-Id`를 보장한다.
클라이언트 또는 상위 프록시가 유효한 trace id를 보냈으면 그대로 downstream 서비스로 전달하고, 없거나 형식이 이상하면 Gateway가 UUID를 새로 만든다.

Gateway에서 직접 차단한 요청도 추적할 수 있도록 `401`, `429` 응답 JSON에는 `traceId`가 포함된다.
하위 서비스는 전달받은 `X-Trace-Id`를 로그 MDC와 에러 응답에 사용해서 Gateway 로그와 도메인 서비스 로그를 같은 요청 단위로 묶는다.

### Gateway / Auth Rate Limit 역할 분리

Gateway rate limit은 입구 방어막이다.
IP 기준으로 인증 API와 공개 API 유입량을 먼저 제한해서 Auth, DB, downstream 서비스로 불필요한 요청이 내려가지 않게 막는다.

Auth rate limit은 인증 도메인 방어다.
로그인/회원가입 시도는 IP만으로 판단하면 부족하므로 email/account 기준 제한을 함께 둔다.
공격자가 IP를 바꿔가며 같은 계정을 계속 시도하는 경우 Gateway IP 제한은 우회될 수 있지만, Auth의 account 기준 제한은 계속 적용된다.

따라서 두 제한은 중복이 아니라 계층형 방어로 본다.
Gateway는 넓고 가볍게, Auth는 도메인 맥락을 반영해서 더 세밀하게 제한한다.
