# 이벤트 설계 문서

## 목적
- 주문 생성부터 가게 검증, 결제, 포인트, 최종 수락/거절, 환불까지를 이벤트 기반 Saga로 연결한다.
- 각 서비스는 자기 로컬 트랜잭션만 책임지고, 다음 단계는 Kafka 이벤트로 이어간다.
- 서비스 간 직접 호출을 줄이고, Outbox + Debezium CDC로 이벤트 발행 안정성을 확보한다.

## 공통 원칙
- 이벤트 발행은 Transactional Outbox 패턴을 사용한다.
- 각 서비스는 DB 저장과 outbox 저장을 같은 트랜잭션에서 처리한다.
- Kafka 발행은 Debezium Outbox EventRouter SMT가 담당한다.
- 컨슈머는 중복 메시지를 전제로 멱등하게 처리한다.
- 실패 처리는 재시도, DLQ, 운영자 재처리 경로를 전제로 둔다.
- 이벤트 payload에는 가능한 한 `eventId`, `eventType`, `schemaVersion`, `occurredAt`, 주요 aggregate id를 포함한다.

## 전체 주문 흐름

```text
[1] order-service
주문 생성
-> order outbox: OrderCreated
-> Kafka: outbox.event.OrderCreated

[2] store-service consume OrderCreated
가게 1차 검증
- 가게 존재 여부
- 가게 영업 상태
- 최소 주문 금액
-> 성공: OrderValidated
-> 실패: OrderRejected

[3] order-service consume OrderValidated
주문 상태 = PAYMENT_PENDING
-> order outbox: PaymentRequested
-> Kafka: outbox.event.PaymentRequested

[4] payment-service consume PaymentRequested
결제 대기 데이터 생성
포인트 사용 금액이 있으면
-> payment outbox: PointDeductionRequested
포인트 사용 금액이 없으면
-> 클라이언트 결제 confirm 대기 또는 PG 결제 진행

[5] point-service consume PointDeductionRequested
포인트 차감
-> 성공: PointDeducted
-> 실패: PointDeductionFailed

[6] payment-service consume PointDeducted
PG 결제 진행 또는 결제 confirm 처리
-> 성공: PaymentSucceeded
-> 실패: PaymentFailed + PointRefundRequested

[7] payment-service consume PointDeductionFailed
결제 실패 처리
-> PaymentFailed

[8] order-service consume PaymentSucceeded
주문 상태 = PAID
-> order outbox: OrderPaid

[9] store-service consume OrderPaid
가게 최종 수락/거절
-> 수락: StoreOrderAccepted
-> 거절: StoreOrderRejected

[10] order-service consume StoreOrderAccepted
주문 상태 = ACCEPTED
-> order outbox: OrderAccepted

[11] order-service consume StoreOrderRejected
주문 상태 = REFUND_PENDING
-> order outbox: OrderStatusChanged(targetStatus=REFUND_PENDING)

[12] payment-service consume OrderStatusChanged(targetStatus=REFUND_PENDING)
포트원 결제 취소 API 호출
-> 성공 시 payment.status = REFUNDED
-> PaymentRefunded
-> 포인트 사용분이 있으면 PointRefundRequested

[13] order-service consume PaymentRefunded
주문 상태 = REFUNDED
-> order outbox: OrderStatusChanged(targetStatus=REFUNDED)
```

## 현재 구현된 주요 구간

```text
[1] OrderCreated
-> store-service consume
-> 가게 검증 (존재/영업상태/최소주문금액)
-> OrderValidated 또는 OrderRejected

[2] OrderValidated
-> order-service consume
-> 주문 상태 = PAYMENT_PENDING
-> PaymentRequested outbox 저장

[3] PaymentRequested
-> payment-service consume
-> payment row 생성 (status=PENDING)
-> 포인트 사용액 > 0이면 PointDeductionRequested outbox 저장

[4] PointDeductionRequested
-> point-service consume
-> point_balance 차감 + point_ledger 기록
-> PointDeducted 또는 PointDeductionFailed

[5] 클라이언트 PortOne 결제 완료
-> POST /api/v1/payments/confirm
-> payment-service가 PortOne 검증 (verifyEnabled=true 시)
-> payment.status = SUCCESS
-> PaymentSucceeded outbox 저장

[6] StoreOrderRejected
-> order-service consume
-> 주문 상태 = REFUND_PENDING
-> OrderStatusChanged(targetStatus=REFUND_PENDING) outbox 저장

[7] OrderStatusChanged(targetStatus=REFUND_PENDING)
-> payment-service consume
-> PortOne 결제 취소 API 호출
-> payment.status = REFUNDED
-> PaymentRefunded outbox 저장

[8] PaymentRefunded
-> order-service consume
-> 주문 상태 = REFUNDED
```

## 이벤트 타입과 토픽

Outbox 이벤트 토픽은 Debezium Outbox SMT의 `event_type` 기준으로 분리한다.

Debezium 설정 기준:

```json
{
  "transforms.outbox.route.by.field": "event_type",
  "transforms.outbox.route.topic.replacement": "outbox.event.${routedByValue}"
}
```

주요 토픽:

```text
outbox.event.OrderCreated
outbox.event.OrderValidated
outbox.event.OrderRejected
outbox.event.OrderStatusChanged
outbox.event.PaymentRequested
outbox.event.PaymentSucceeded
outbox.event.PaymentFailed
outbox.event.PaymentRefunded
outbox.event.StoreOrderAccepted
outbox.event.StoreOrderRejected
outbox.event.PointDeductionRequested
outbox.event.PointDeducted
outbox.event.PointDeductionFailed
```

레거시/전환용 aggregate 토픽:

```text
outbox.event.ORDER
outbox.event.USER
outbox.event.PAYMENT
```

신규 주문-결제 흐름 컨슈머는 가능한 한 이벤트 타입별 토픽만 구독한다.

## 서비스별 책임

### Order Service
- 주문 생성과 주문 상태 변경을 담당한다.
- `OrderCreated`를 발행한다.
- `OrderValidated`를 받으면 주문 상태를 `PAYMENT_PENDING`으로 변경하고 `PaymentRequested`를 발행한다.
- `OrderRejected`를 받으면 주문을 거절 상태로 전환한다.
- 이후 `PaymentSucceeded`, `PaymentFailed`, `PaymentRefunded` 등을 consume해서 주문 상태를 갱신한다.
- `StoreOrderRejected`를 받으면 `REFUND_PENDING`으로 바꾸고, 환불 완료 이벤트를 받으면 `REFUNDED`로 마감한다.

### Store Service
- `OrderCreated`를 consume해서 가게 1차 검증을 수행한다.
- 검증 결과를 `store_order_validation`에 저장한다.
- 성공 시 `OrderValidated`, 실패 시 `OrderRejected`를 발행한다.
- 결제 이후 최종 수락/거절 단계에서는 `OrderStatusChanged(targetStatus=PAID)`를 consume해서 사장 수락/거절 대기 데이터를 만든다.
- 사장 수락/거절 API 결과에 따라 `StoreOrderAccepted` 또는 `StoreOrderRejected`를 발행한다.

### Payment Service
- `PaymentRequested`를 consume해서 결제 대기 데이터를 생성한다.
- 포인트 사용 금액이 있으면 `PointDeductionRequested`를 발행한다.
- 클라이언트가 `/api/v1/payments/confirm`을 호출하면 포트원 결제 결과를 검증하고 `PaymentSucceeded`를 발행한다.
- 실패 시 `PaymentFailed`를 발행한다.
- 주문이 `REFUND_PENDING`으로 바뀐 `OrderStatusChanged` 이벤트를 consume해서 포트원 결제 취소 API를 호출한다.
- 포트원 결제 취소가 성공한 뒤에만 `payment.status=REFUNDED`로 바꾸고 `PaymentRefunded`를 발행한다.
- 추후 포인트 차감 성공/실패 이벤트를 consume해서 PG 결제 흐름을 이어가도록 확장한다.

### Point Service
- `PointDeductionRequested`를 consume한다.
- 포인트 잔액을 차감하고 원장을 남긴다.
- 성공 시 `PointDeducted`, 실패 시 `PointDeductionFailed`를 발행한다.
- 현재 MVP는 즉시 차감 후 실패 시 보상 환불하는 모델이다.
- 운영 수준에서는 `RESERVE -> CAPTURE / RELEASE` 방식으로 확장할 수 있다.

## 포인트 처리 정책

현재 MVP 방식:

```text
PointDeductionRequested
-> point_balance 즉시 차감
-> PointDeducted

PG 실패 또는 주문 거절
-> PointRefundRequested
-> point_balance 복구
```

운영 확장 방식:

```text
RESERVE
-> available balance 감소
-> reserved balance 증가

CAPTURE
-> reserved balance 감소
-> 실제 사용 확정

RELEASE
-> reserved balance 감소
-> available balance 복구
```

지금은 MVP 속도를 위해 `DEDUCT -> REFUND` 모델을 사용하고, 추후 정산/환불 복잡도가 커지면 예약/확정/해제 모델로 바꾼다.

## 멱등성 기준

각 컨슈머는 Kafka 메시지가 중복 전달될 수 있다고 가정한다.

권장 멱등키:
- 주문 생성: `idempotencyKey`
- Store 검증: `eventId` 또는 `orderId + validation step`
- Payment 생성: `orderId`
- Point 차감: `paymentId + DEDUCT` 또는 `eventId`
- 환불: `paymentId + REFUND`

현재 Point MVP는 `point_ledger.idempotency_key`에 unique 제약을 둬서 중복 차감을 막는다.

## Kafka 메시지 형식 예시

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
    "schemaVersion": 1,
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

## 토픽 관리 정책

- Kafka 토픽은 Terraform으로 관리한다.
- 토픽 자동 생성은 운영 기준에서 사용하지 않는다.
- Kafka Connect 내부 토픽은 Kafka Connect가 관리한다.
- Debezium schema history 토픽은 Terraform 대상에서 제외한다.
- 컨슈머 그룹은 Terraform으로 만들지 않고, 애플리케이션이 실제로 consume을 시작할 때 생성된다.

## 남은 작업

우선순위 높음:
- `payment-service`가 `PointDeducted`를 consume → PG 결제 이어가기
- `payment-service`가 `PointDeductionFailed`를 consume → `PaymentFailed` 발행
- 포인트 사용 주문의 환불 이벤트 연계 (`PointRefundRequested`)

이후 확장:
- Saga timeout 처리 (결제 응답 없을 때 Order 자동 FAILED 전환)
- DLQ 컨슈머 및 재처리 운영 API 정리
