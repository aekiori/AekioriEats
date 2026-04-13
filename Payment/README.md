# Payment Service

`payment.requested` 이벤트를 소비해서 결제 처리 상태를 저장하고,
결과를 outbox(`PaymentSucceeded` / `PaymentFailed`)로 발행하는 서비스입니다.

## 주요 기능
- `outbox.event.ORDER` / `delivery.delivery_order.outbox`에서 `PaymentRequested` 소비
- 결제 요청 상태 저장(`payment` 테이블)
- 결제 결과 outbox 저장(`outbox` 테이블)

## 실행
```cmd
.\gradlew.bat bootRun
```

## 테스트
```cmd
.\gradlew.bat test
```
