# Kafka Consumer Retry / DLQ

## 적용 범위

- User, Order, Store, Payment, Point 서비스의 Kafka consumer는 Spring Kafka `DefaultErrorHandler`를 사용한다.
- Auth, Gateway는 현재 Kafka consumer가 없어서 적용 대상이 아니다.
- 실패 메시지는 원본 토픽명에 `.DLT`를 붙인 토픽으로 보낸다.
  - 예: `outbox.event.PaymentRequested` -> `outbox.event.PaymentRequested.DLT`

## 기본 정책

| 항목 | 기본값 | 설정 |
|------|--------|------|
| 재시도 횟수 | 3 | `{SERVICE}_EVENT_CONSUMER_RETRY_ATTEMPTS` |
| 재시도 간격 | 1000ms | `{SERVICE}_EVENT_CONSUMER_RETRY_INTERVAL_MS` |
| DLQ 토픽 | `{originalTopic}.DLT` | 코드에서 자동 결정 |

`retry-attempts=3`이면 최초 처리 1회 실패 후 3회 재시도하고, 그래도 실패하면 DLT로 보낸다.

## Consumer 구현 규칙

- 처리 실패를 `catch`에서 로그만 남기고 삼키면 안 된다.
- 재시도/DLQ 처리가 동작하려면 예외를 다시 던져야 한다.
- 처리할 이벤트가 아니거나 필수 필드가 부족해 무시 가능한 메시지는 `return`한다.
- 같은 이벤트가 재전달될 수 있으므로 handler/service 레벨의 멱등 처리를 유지한다.

## Topic 관리

Kafka auto-create가 꺼져 있으므로 DLT 토픽도 Terraform으로 명시한다.

- 토픽 목록: `infra/terraform/kafka/terraform.tfvars.example`
- 운영 반영: `infra/terraform/kafka`에서 `terraform plan`, `terraform apply`

## 수동 조회 / 재처리 API

Kafka consumer가 있는 서비스는 동일한 내부 운영 API를 제공한다.

| Method | Path | 설명 |
|--------|------|------|
| GET | `/internal/kafka-dlt/messages` | DLT 토픽 메시지 조회 |
| POST | `/internal/kafka-dlt/replay` | DLT 메시지를 원본 토픽으로 재발행 |

공통 헤더:

| Header | 설명 |
|--------|------|
| `X-Internal-Api-Key` | 서비스별 내부 API key |

조회 예시:

```http
GET /internal/kafka-dlt/messages?topic=outbox.event.PaymentRequested.DLT&partition=0&offset=0&limit=20
X-Internal-Api-Key: payment-internal-dev-key
```

재처리 예시:

```http
POST /internal/kafka-dlt/replay
X-Internal-Api-Key: payment-internal-dev-key
Content-Type: application/json

{
  "topic": "outbox.event.PaymentRequested.DLT",
  "partition": 0,
  "offset": 12
}
```

`targetTopic`을 생략하면 `.DLT` suffix를 제거한 원본 토픽으로 재발행한다.

```json
{
  "topic": "outbox.event.PaymentRequested.DLT",
  "partition": 0,
  "offset": 12,
  "targetTopic": "outbox.event.PaymentRequested"
}
```

## 남은 운영 과제

- DLT 적재량 알림
- 반복 실패 이벤트의 원인 분류 및 영구 실패 처리 정책
