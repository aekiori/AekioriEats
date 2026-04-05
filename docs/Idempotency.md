# 멱등성 (Idempotency)
## 개요
주문 생성 API는 네트워크 오류나 클라이언트 재시도로 인한 중복 처리를 방지하기 위해 멱등성을 보장한다.
동일한 요청이 여러 번 들어와도 결과는 항상 동일하게 유지된다.
------------------------------------------------------------
## Idempotency-Key
### 헤더 방식 채택
```
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```
- 멱등키는 비즈니스 데이터가 아니라 **전송 제어 메타데이터**
- 바디는 비즈니스 로직 데이터만 담는 것이 원칙
- HTTP 표준도 헤더에 넣는 방식을 권장
### 정규화 처리
`@NotBlank` 검증을 통과하더라도 `" Aekiori "` 처럼 공백이 포함된 경우,
DB 유니크 제약 조건 및 멱등성 비교 로직에서 의도치 않은 중복 처리가 발생할 수 있다.
따라서 `trim()`을 통해 수신 즉시 정규화한다.
------------------------------------------------------------
## 처리 흐름
```
요청 수신
    │
    ▼
Redis Lock 확인 (order:idempotency:lock:{idempotencyKey})
    │
    ├── Lock 존재 → 처리 중 → 409 CONFLICT
    │
    ▼
Redis Result 캐시 확인 (order:idempotency:result:{idempotencyKey})
    │
    ├── 캐시 존재 → 캐시된 결과 즉시 반환
    │
    ▼
DB 조회 (idempotencyKey 기준)
    │
    ├── 존재 → requestHash 비교
    │       ├── 같음 → 기존 주문 반환 + Redis 캐시 저장
    │       └── 다름 → 409 CONFLICT (동일 키, 다른 요청)
    │
    ▼
신규 주문 처리
    │
    ▼
결과 반환 + Redis 캐시 저장
```

---

## Redis 키 구조

| 키 | 용도 | TTL |
|---|---|---|
| `order:idempotency:lock:{idempotencyKey}` | 처리 중 중복 요청 차단 | 단기 |
| `order:idempotency:result:{idempotencyKey}` | 완료된 결과 캐싱 | 장기 |

---

## 응답 케이스

| 케이스 | 응답 |
|---|---|
| 동일 키 + 동일 요청 본문 | `200 OK` — 기존 주문 결과 반환 |
| 동일 키 + 다른 요청 본문 | `409 CONFLICT` |
| 처리 중인 동일 키 요청 | `409 CONFLICT` |
| 신규 요청 | `201 CREATED` |

---

## requestHash

동일한 `idempotencyKey`로 다른 내용의 요청이 들어왔을 때를 감지하기 위해
요청 본문을 해싱한 값을 함께 저장한다.

- 최초 요청 시 `requestHash` 생성 및 저장
- 재시도 요청 시 `requestHash` 비교
- 불일치 시 `409 CONFLICT` 반환
