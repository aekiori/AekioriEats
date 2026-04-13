# Event Design (Draft v1)

## 목적
- 주문 생성부터 결제/가게 수락/환불까지를 이벤트 기반 Saga로 처리한다.
- 각 서비스는 자신의 로컬 트랜잭션만 커밋하고, 후속 처리는 이벤트로 연결한다.

## 공통 원칙
- 발행 방식: Outbox 패턴 사용
- 공통 필드: `eventId`, `eventType`, `occurredAt`, `aggregateId(orderId)`, `correlationId(sagaId)`, `schemaVersion`
- 멱등성: 각 컨슈머는 `processed_events`(또는 동등 저장소)로 중복 처리 방지
- 실패 처리: 재시도 + DLQ + 운영자 재처리 경로 제공

## 전체 플로우
1. `order-service`: 주문 생성  
   - 상태: `PENDING`
   - 이벤트: `order.created`

2. `store-service` consume `order.created`: 1차 가게 검증(오픈/최소금액/기본 제약)  
   - 성공: `order.validated`
   - 실패: `order.rejected`

3. `order-service` consume `order.validated`  
   - 상태: `PAYMENT_PENDING`
   - 이벤트: `payment.requested`

4. `pay-service` consume `payment.requested`  
   - 포인트 필요 시: `point.deduction.requested`
   - 포인트 불필요 시: PG 결제 바로 진행

5. `point-service` consume `point.deduction.requested`  
   - 성공: `point.deducted`
   - 실패: `point.deduction.failed`

6. `pay-service` consume `point.deducted` 후 PG 결제  
   - 성공: `payment.succeeded`
   - 실패: `payment.failed` + `point.refund.requested`(선차감 포인트가 있으면)

7. `pay-service` consume `point.deduction.failed`  
   - 이벤트: `payment.failed`

8. `order-service` consume `payment.succeeded`  
   - 상태: `PAID`
   - 이벤트: `order.paid`

9. `store-service` consume `order.paid`: 최종 수락/거절  
   - 수락: `store.order.accepted`
   - 거절: `store.order.rejected`

10. `order-service` consume `store.order.accepted`  
   - 상태: `ACCEPTED`
   - 이벤트: `order.accepted`

11. `order-service` consume `store.order.rejected`  
   - 상태: `REFUND_PENDING`

12. `pay-service` consume `store.order.rejected`  
   - 이벤트: `payment.refunded` (+ 필요 시 `point.refund.requested`)

13. `order-service` consume `payment.refunded`  
   - 상태: `REFUNDED`
   - 이벤트: `order.refunded`

## 반드시 포함할 보완 분기
- `order-service` consume `order.rejected`  
  - 상태: `REJECTED`(또는 `CANCELLED`)로 종료
- `order-service` consume `payment.failed`  
  - 상태: `PAYMENT_FAILED`(또는 정책 상태) 전이

## 적립(earn) 이벤트 권장 시점
- `point.earn.requested`는 `payment.succeeded` 직후보다 `store.order.accepted` 이후가 안전하다.
- 이유: 가게 최종 거절/환불 시 적립 취소 보상 플로우를 줄일 수 있음.

## 타임아웃/보상 정책
- 대기 구간 타임아웃 정의:
  - `order.created -> order.validated`
  - `payment.requested -> payment.succeeded | payment.failed`
  - `order.paid -> store.order.accepted | store.order.rejected`
- 타임아웃 시 `*.timeout` 이벤트 또는 명시적 실패 상태 전이
- 보상 이벤트는 재처리 가능해야 하며 멱등하게 설계

## 상태 전이 요약 (Order)
- `PENDING` -> `PAYMENT_PENDING` -> `PAID` -> `ACCEPTED`
- 예외 분기:
  - `PENDING` -> `REJECTED` (`order.rejected`)
  - `PAYMENT_PENDING` -> `PAYMENT_FAILED` (`payment.failed`)
  - `PAID` -> `REFUND_PENDING` -> `REFUNDED` (`store.order.rejected` 이후 환불 완료)

## 토픽/컨슈머 그룹 네이밍 가이드
- 토픽 예: `order.created`, `order.validated`, `payment.requested`, `payment.succeeded`
- 컨슈머 그룹은 도메인 책임 기준으로 분리:
  - `store-order-created-validation`
  - `order-payment-state-transition`
  - `pay-payment-requested`
  - `point-deduction`

## 운영 체크리스트
- 이벤트 스키마 버전 관리(`schemaVersion`)
- DLQ 모니터링 대시보드
- 리플레이/재처리 툴 준비
- 핵심 이벤트 end-to-end 추적용 `correlationId` 로깅
