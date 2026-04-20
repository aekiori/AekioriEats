# Payment Service

Payment 서비스는 주문 결제 상태, 포트원 결제 검증/취소, 결제 결과 이벤트 발행을 담당한다.

## 역할

- `PaymentRequested` 이벤트를 consume해서 결제 대기 데이터를 생성한다.
- 클라이언트가 포트원 결제를 완료한 뒤 `POST /api/v1/payments/confirm`을 호출하면 결제를 확정한다.
- 결제 확정 성공 시 `payment_outbox`에 `PaymentSucceeded` 이벤트를 저장한다.
- 결제 실패 처리 시 `payment_outbox`에 `PaymentFailed` 이벤트를 저장할 수 있다.
- 주문이 `REFUND_PENDING` 상태로 전환되면 포트원 결제 취소 API를 호출한다.
- 포트원 결제 취소 성공 시 `payment.status=REFUNDED`로 변경하고 `PaymentRefunded` 이벤트를 저장한다.

## 결제 흐름

```text
order-service
  -> payment.requested outbox
  -> Kafka
  -> payment-service consume
  -> payment row 생성, status=PENDING

client
  -> PortOne KakaoPay 결제
  -> POST /api/v1/payments/confirm

payment-service
  -> payment.status=SUCCESS
  -> payment_outbox PaymentSucceeded 저장
  -> Debezium/Kafka
  -> order-service consume
```

## 환불 흐름

가게가 결제 완료 주문을 거절하면 Order 서비스가 주문을 `REFUND_PENDING`으로 바꾸고 `OrderStatusChanged` 이벤트를 발행한다.
Payment 서비스는 이 이벤트를 consume해서 포트원 결제 취소 API를 호출한다.

```text
store-service
  -> StoreOrderRejected

order-service
  -> order.status=REFUND_PENDING
  -> outbox.event.OrderStatusChanged(targetStatus=REFUND_PENDING)

payment-service
  -> PortOne POST /payments/{paymentId}/cancel
  -> payment.status=REFUNDED
  -> payment_outbox PaymentRefunded 저장

order-service
  -> PaymentRefunded consume
  -> order.status=REFUNDED
```

포트원 검증/취소 모드가 꺼져 있으면 로컬 개발 편의를 위해 mock 취소로 처리한다.
실제 카카오페이 취소까지 검증하려면 `PAYMENT_PORTONE_VERIFY_ENABLED=true`로 실행해야 한다.

## DDL

```sql
CREATE TABLE payment (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id          BIGINT NOT NULL,
    user_id           BIGINT NOT NULL,
    amount            INT NOT NULL,
    status            ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    pg_type           VARCHAR(20) NOT NULL DEFAULT 'KAKAOPAY',
    pg_transaction_id VARCHAR(100),
    failed_reason     VARCHAR(200),
    created_at        DATETIME NOT NULL,
    updated_at        DATETIME NOT NULL,
    UNIQUE KEY uq_payment_order_id (order_id)
);

CREATE TABLE payment_outbox (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id       VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id   BIGINT NOT NULL,
    event_type     VARCHAR(50) NOT NULL,
    payload        TEXT NOT NULL,
    status         ENUM('INIT','PUBLISHED') NOT NULL DEFAULT 'INIT',
    partition_key  VARCHAR(100),
    created_at     DATETIME NOT NULL
);
```

## 상태

```text
PENDING -> SUCCESS
PENDING -> FAILED
SUCCESS -> REFUNDED
```

## API

### 결제 확정

```http
POST /api/v1/payments/confirm
Content-Type: application/json

{
  "orderId": 10,
  "paymentId": "paymentorder10...",
  "amount": 24400
}
```

응답 예시:

```json
{
  "orderId": 10,
  "paymentId": 1,
  "providerPaymentId": "paymentorder10...",
  "status": "SUCCESS",
  "amount": 24400,
  "providerVerified": false
}
```

## 포트원 검증

현재 로컬 개발 기본값은 `PAYMENT_PORTONE_VERIFY_ENABLED=false`다.
이 경우 Payment DB의 `amount`와 클라이언트 요청 `amount`를 비교해서 confirm 한다.

실제 포트원 API 검증을 붙일 때는 아래 환경변수를 설정한다.

```env
PAYMENT_PORTONE_VERIFY_ENABLED=true
PAYMENT_PORTONE_API_BASE_URL=https://api.portone.io
PAYMENT_PORTONE_API_SECRET=...
PAYMENT_PORTONE_STORE_ID=store-f68c0d7a-c89c-4e1b-b3fd-7655a1d8899e
```

API Secret은 서버 전용 값이다. 프론트엔드에 노출하면 안 된다.

## 포트원 환불

실제 환불은 포트원 V2 결제 취소 API를 호출한다.

```text
POST /payments/{paymentId}/cancel
```

현재 구현은 전액 취소 기준이다.
포트원 취소 API가 성공해야만 Payment 상태를 `REFUNDED`로 바꾸고 `PaymentRefunded` 이벤트를 발행한다.

로컬에서 `.env`를 사용할 경우 IntelliJ 실행 설정에 아래 값을 환경변수로 주입해야 한다.

```env
PAYMENT_PORTONE_VERIFY_ENABLED=true
PAYMENT_PORTONE_API_SECRET=...
PAYMENT_PORTONE_STORE_ID=...
```
