# Point 서비스

Point 서비스는 사용자 포인트 잔액과 포인트 원장을 관리한다.

## 현재 범위
- 사용자 포인트 잔액 조회
- 로컬/개발용 포인트 충전 API
- `PointDeductionRequested` 이벤트 소비
- 포인트 차감 멱등 처리
- 포인트 차감 결과 outbox 이벤트 저장

## 이벤트 흐름

```text
Payment 서비스
-> outbox.event.PointDeductionRequested
-> Point 서비스 consume
-> point_balance / point_ledger 갱신
-> outbox.event.PointDeducted 또는 outbox.event.PointDeductionFailed
```

## API

```http
GET /api/v1/points/users/{userId}/balance
POST /api/v1/points/users/{userId}/charge
```

포인트 충전 요청 예시:

```json
{
  "amount": 10000,
  "reason": "local test seed"
}
```

## 테이블
- `point_balance`: 사용자별 현재 포인트 잔액
- `point_ledger`: 포인트 충전/차감/실패 이력
- `point_outbox`: Point 서비스가 발행할 이벤트 저장소

## 로컬 실행

```cmd
.\gradlew.bat bootRun
```

## 다음 단계
- `payment-service`가 `PointDeducted`를 consume해서 PG 결제를 이어간다.
- `payment-service`가 `PointDeductionFailed`를 consume해서 `PaymentFailed`를 발행한다.
- 결제 실패/가게 거절 시 `PointRefundRequested` 흐름을 추가한다.
