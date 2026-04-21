# Kafka Consumer 멱등 처리 설계

## 개요

Kafka consumer 멱등성은 HTTP 멱등성과 결이 다르다.

- HTTP 멱등성은 "같은 요청을 여러 번 보내도 주문이 한 번만 생성되게" 막는 문제
- Kafka consumer 멱등성은 "같은 이벤트를 여러 번 consume해도 side effect가 한 번만 나가게" 막는 문제

Kafka는 at-least-once(최소 한번)를 기본 가정으로 본다.  
즉 중복 consume는 이상 상황이 아니라 반드시 온다는 전제로 consumer를 짜는 게 맞다.

---

## HTTP 멱등성과 차이

| 구분 | HTTP 멱등성 | Kafka Consumer 멱등성 |
|---|---|---|
| 중복 원인 | 재클릭, 타임아웃, 클라 재시도 | 재배포, rebalance, commit 실패, 중복 발행 |
| 기준 키 | `Idempotency-Key` | `eventId`, aggregate id, 현재 상태 |
| 방어 위치 | API 진입점 | consumer 핸들러 / DB |
| 핵심 목표 | 같은 요청 중복 생성 방지 | 같은 이벤트 중복 부작용 방지 |

---

## 이 프로젝트에서 쓰는 방식

consumer 멱등을 한 가지 방식으로 통일하지 않는다.
도메인 성격에 따라 두 패턴을 섞어 쓴다.

### 1. processed_events 테이블 기록 방식

`User` 도메인 projection consumer에서 사용한다.

흐름은 아래와 같다.

1. eventId를 `processed_events`에 insert 시도
2. unique 충돌이면 이미 처리한 이벤트로 보고 skip
3. insert 성공한 경우에만 실제 projection/upsert 진행

이 방식이 잘 맞는 경우:

- projection 적재
- 읽기 모델 동기화
- 통계/집계 테이블 적재
- "이 이벤트를 처리했는가?" 자체가 핵심인 consumer

### 2. 도메인 상태 전이 검증 방식

`Order`, `Store`, `Payment` 상태 변경 consumer에서 사용한다.

예를 들면:

- 주문이 이미 `PAID`면 `payment.succeeded`를 다시 받아도 skip
- 주문이 이미 `REFUNDED`면 `payment.refunded`를 다시 받아도 skip
- `StoreOrder`가 이미 있으면 `order.paid`를 다시 받아도 생성 안 함

이 방식의 핵심은 "지금 상태에서 이 event를 다시 적용해도 변화가 없어야 한다"에 초점을 둔다.

이 방식이 잘 맞는 경우:

- 주문 / 결제 / 가게 주문 상태 변경처럼 상태 머신이 명확한 도메인

---

## 외부 부작용이 있을 때

consumer가 DB만 바꾸는 게 아니라 외부 API를 호출하면 난이도가 올라간다.

- 포인트 차감/복구
- 알림 발송

이 경우 도메인 상태가 맞아도 외부 호출이 두 번 나가면 사고가 난다.
그래서 아래 중 하나가 필요하다.

- 외부 호출 전에 processed_event 기록
- 외부 시스템도 idempotency key를 지원하게 맞춤
- 도메인 상태 + unique key + 재호출 방지 조건을 같이 둠

이 프로젝트는 클라이언트가 PortOne SDK로 결제창을 띄우고, 서버는 /confirm에서 검증만 한다.
즉 서버가 PG를 직접 호출하지 않으므로 PG 중복 호출 자체가 서버 레벨 문제가 아니다.
/confirm 중복 호출은 payment 도메인 상태 전이 검증으로 막는다.

---

## 최후 방어선은 DB다

Redis나 메모리 캐시로 consumer 멱등을 처리할 수도 있지만,
재기동/유실/멀티 인스턴스까지 생각하면 DB 기준 방어선이 훨씬 믿을 만하다.

- `processed_events.event_id` unique constraint
- `store_order.order_id` unique constraint
- `payment.order_id` unique constraint
- 상태 전이 조건을 만족하지 않으면 update하지 않는 로직

---

## 운영 관점 메모

- skip 로그에는 `eventId`, `aggregateId`, 현재 상태를 남길 것
- 재처리 기준이 필요하면 `processed_events` 방식이 더 명확함
- 결제/환불 쪽은 "중복 이벤트 빈도"보다 "한 번 잘못 중복 실행됐을 때의 사고 비용"이 훨씬 크다. 보수적으로 설계하는 게 맞다.

---

## 이 프로젝트의 결론

- HTTP 멱등성과는 별도 문제다.
- Kafka는 중복 consume를 기본 가정으로 본다.
- projection류는 `processed_events` 방식이 잘 맞는다.
- 상태 변경류는 상태 전이 검증 + unique constraint 방식이 잘 맞는다.
- 외부 부작용이 있으면 DB 상태만 맞다고 끝이 아니라 외부 호출 중복까지 막아야 한다.

**Kafka consumer 멱등의 본질은 "같은 event를 두 번 받아도 결과와 부작용이 한 번만 발생하게 만드는 것"이다.**
