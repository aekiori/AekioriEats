# 멱등 처리 설계

## 개요

주문 생성 API는 네트워크 타임아웃, 클라이언트 재시도, 중복 클릭 같은 상황에서도 같은 요청이 여러 번 처리되지 않도록 멱등성을 보장한다.  
이 프로젝트에서는 `Idempotency-Key`와 `requestHash`, Redis, DB unique constraint를 함께 사용해서 중복 주문 생성을 막는다.

## Idempotency-Key

클라이언트는 주문 생성 시 `Idempotency-Key` 헤더를 반드시 보낸다.

```http
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

정리 기준은 아래와 같다.

- 멱등 키는 비즈니스 데이터가 아니라 요청 제어용 메타데이터
- 바디는 비즈니스 로직 데이터만 담는 게 깔끔
- 그래서 body보다 HTTP 헤더에 두는 방식을 택했다.
  - Stripe가 이 방식을 대중화시켰고, 이후 결제/금융 API들이 따라가면서 사실상 업계 표준처럼 굳어졌다 카더라
  - HTTP 표준이라기보다는 업계 관행?

## requestHash

같은 `Idempotency-Key`로 다른 요청 본문이 들어오는 경우를 막기 위해 `requestHash`를 같이 저장한다.

`requestHash`에는 아래 값들이 반영된다.

- `userId`
- `storeId`
- `deliveryAddress`
- `usedPointAmount`
- 주문 아이템 목록

같은 키라도 해시가 다르면 같은 요청으로 보지 않고 `409 CONFLICT`를 반환한다.

## Idempotency-Key Normalize

```java
private String normalizeIdempotencyKey(String idempotencyKey) {
    return idempotencyKey.trim();
}
```

`@NotBlank` 검증을 통과하더라도 `" Aekiori "` 처럼 공백이 포함된 경우 DB 유니크 제약 조건 및 멱등성 비교 로직에서 의도치 않은 중복 처리가 발생할 수 있다. 따라서 수신 즉시 `trim()` 정규화한다.


## 현재 처리 순서

현재 `createOrder`의 멱등 처리 순서는 아래와 같다.

1. `idempotencyKey` 정규화
2. `requestHash` 생성
3. Redis 결과 캐시 조회
4. Redis lock 획득 시도
5. 주문 생성
6. 트랜잭션 커밋 후 Redis 결과 캐시 저장
7. 실패 시 lock 해제

키 구조는 아래처럼 사용한다.

| 키 | 용도 | 기본 TTL |
|---|---|---|
| `order:idempotency:lock:{idempotencyKey}` | 처리 중 중복 요청 차단 | 30초 |
| `order:idempotency:result:{idempotencyKey}` | 완료 결과 캐시 | 600초 |

## 응답 케이스

| 케이스 | 응답 |
|---|---|
| 같은 키 + 같은 요청 본문 + 기존 결과 존재 | 기존 주문 결과 반환 |
| 같은 키 + 다른 요청 본문 | `409 CONFLICT` |
| 같은 키 + 동일 요청이 현재 처리 중 | `409 CONFLICT` |
| 새로운 요청 | `201 CREATED` |

## 요청 분기 흐름
1. Redis Lock 확인
    - Lock 있음 → 409 (처리 중)


2. Redis Result 캐시 확인
    -  캐시 있음 → requestHash 비교
    - 같음 → 캐시 결과 반환
    - 다름 → 409


3. DB 조회
    - DB에 있음 → requestHash 비교
    - 같음 → 반환 + 캐시 저장
    - 다름 → 409


4. 신규 주문 생성

## 멱등 처리 설계 메모

createOrder 멱등 처리에서 DB 먼저 조회할지 Redis 먼저 조회할지는 정답보다 트레이드오프 문제로 봤다.

DB 먼저: 캐시 유실에도 방어적으로 중복 생성을 막을 수 있다.  
Redis 먼저: 재시도 요청을 빠르게 털어내 성능상 유리하다. 정상 요청 기준으로도 DB hit이 없어서 유리하다.

단, "앞단에서 뭘 쓰든 상관없다"는 결론은  
**idempotency_key unique constraint 같은 DB 레벨 최후 방어선이 있어야만 성립한다.**

- Redis 유실 시에도 DB가 중복 insert를 막아줘야 한다.

correctness 관점에서는 둘 다 안전하고, 실제 차이는 성능과 운영 철학의 차이에 가깝다.  
실제 주문 트래픽에서는 중복 주문 자체가 정상은 아닐거라, 극소수 케이스일 것이라고 생각했다.

- 얼마나 될지 ㄹㅇ 궁금하긴하다 한 0.05퍼 되려나?

그래서 최후 방어선이 갖춰져 있다면 DB 먼저든 Redis 먼저든 실질적인 체감 차이는 거의 없다고 판단했다.

### 실무 관점 메모

이 문제는 대부분의 상황에서 심각한 성능 병목이라기보다 아래에 더 가깝다.

- 설계 취향
- 운영 철학
- 장애 설명 가능성
- 팀 컨벤션

결제나 금융처럼 사고 비용이 큰 도메인에서는 개인 취향보다 팀 컨벤션이 더 중요하다.  
예를 들어 팀이 아래처럼 정해놨다면:

- 우리는 DB 먼저 조회한다
- 우리는 Redis 먼저 본다

그 규칙을 따르는 게 맞다. 이 부분은 미세한 성능 차이보다 일관성과 운영 안정성이 더 중요하다.

### 이 프로젝트의 결론

이 프로젝트는 아래 순서를 사용한다.  
`Redis 결과 캐시 조회 -> Redis lock 획득 -> 주문 생성`

정상 요청 기준으로 DB hit을 줄이고, 재시도 요청도 Redis에서 빠르게 털어내기 위해 Redis 먼저 구조를 선택했다.  
캐시 유실 시에는 idempotency_key unique constraint가 최후 방어선 역할을 한다.
