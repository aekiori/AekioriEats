# Order Timeout Compensation 설계

## 목적

주문 Saga는 여러 서비스의 이벤트를 거치기 때문에 특정 단계에서 이벤트가 유실되거나 외부 처리가 멈추면 주문이 중간 상태에 오래 머무를 수 있다.

이 문서는 `order-service`가 그런 주문을 주기적으로 찾아 보상 상태로 전이시키는 timeout compensation 설계를 정리한다.

## 대상 상태

| 현재 상태 | 의미 | timeout 이후 상태 | 이유 |
|---|---|---|---|
| `PENDING` | Store 검증 이벤트 대기 | `FAILED` | Store 검증 결과가 오지 않음 |
| `PAYMENT_PENDING` | 결제 결과 이벤트 대기 | `FAILED` | 결제 성공/실패 결과가 오지 않음 |
| `PAID` | Store 최종 수락/거절 대기 | `REFUND_PENDING` | 결제 완료 후 매장 응답이 오지 않음 |

`PAID`는 이미 결제가 완료된 상태라 바로 `FAILED`나 `CANCELLED`로 끝내지 않는다. `REFUND_PENDING`으로 전이시키면 기존 Payment 서비스의 refund pending consumer가 환불 플로우를 이어받는다.

## 처리 방식

현재 구현은 DB polling 기반이다.

1. Scheduler가 일정 주기로 실행된다.
2. Redisson `RLock`을 획득한 인스턴스만 보상 작업을 수행한다.
3. `orders.status`, `orders.updated_at` 기준으로 timeout 대상 주문을 조회한다.
4. 대상 주문을 비관적 락으로 잡고 상태를 변경한다.
5. 상태 변경은 기존 Order aggregate의 `updateStatus`를 사용한다.
6. 상태 변경 history를 `TIMEOUT_COMPENSATION` source type으로 기록한다.
7. 상태 변경 outbox가 저장되고 Debezium을 통해 Kafka로 발행된다.

## 분산 실행 제어

멀티 인스턴스 환경에서는 모든 인스턴스의 `@Scheduled` 메서드가 동시에 실행될 수 있다.

이를 막기 위해 ShedLock 대신 Redisson 분산락을 사용한다. 이 프로젝트는 이미 Order 멱등성 처리에 Redis를 사용하므로 Redis 기반 분산락이 더 자연스럽다.

```java
RLock lock = redissonClient.getLock(lockName);
boolean acquired = lock.tryLock(lockWaitMs, lockLeaseMs, TimeUnit.MILLISECONDS);
```

락 획득에 실패하면 해당 인스턴스는 작업을 skip한다. 락을 획득한 인스턴스만 timeout compensation을 실행한다.

## DB 조회와 인덱스

timeout 대상 조회 조건은 다음과 같다.

```sql
where status = ?
  and updated_at < ?
order by updated_at asc
limit ?
```

이를 위해 `orders(status, updated_at)` 인덱스를 둔다.

```sql
CREATE INDEX idx_status_updatedAt ON orders (status, updated_at);
```

DB polling은 단순하지만 주문량이 늘면 조회 비용이 커질 수 있다. 그래서 반드시 상태와 시간 기준 인덱스, batch size 제한을 같이 둔다.

## 설정

```yaml
order:
  timeout-compensation:
    enabled: true
    fixed-delay-ms: 60000
    initial-delay-ms: 30000
    batch-size: 100
    store-validation-timeout-minutes: 10
    payment-result-timeout-minutes: 10
    store-decision-timeout-minutes: 15
    lock-name: order:timeout-compensation
    lock-wait-ms: 0
    lock-lease-ms: 60000
```

| 설정 | 설명 |
|---|---|
| `enabled` | timeout compensation 활성화 여부 |
| `fixed-delay-ms` | 작업 완료 후 다음 실행까지 대기 시간 |
| `initial-delay-ms` | 애플리케이션 시작 후 첫 실행 지연 시간 |
| `batch-size` | 상태별 한 번에 처리할 최대 주문 수 |
| `store-validation-timeout-minutes` | `PENDING` timeout 기준 |
| `payment-result-timeout-minutes` | `PAYMENT_PENDING` timeout 기준 |
| `store-decision-timeout-minutes` | `PAID` timeout 기준 |
| `lock-name` | Redisson 분산락 이름 |
| `lock-wait-ms` | 락 획득 대기 시간. 기본값은 0으로 즉시 실패 |
| `lock-lease-ms` | 락 lease 시간. 기본값은 60초이며 작업 최대 예상 시간보다 약간 길게 둔다 |

## 운영 관점

- `lock-lease-ms`는 작업이 정상적으로 끝나는 시간보다 약간 길어야 한다. 너무 길면 락을 가진 인스턴스가 죽었을 때 다른 인스턴스가 보상 작업을 재개하기까지 오래 기다린다.
- Redis 장애가 발생하면 timeout compensation이 멈출 수 있다. 이 작업은 실시간 주문 생성 경로가 아니라 보상 작업이므로 Redis 기반 분산락을 선택했다.
- 상태 변경은 기존 outbox 흐름을 타므로 `PAID -> REFUND_PENDING` 같은 보상 전이도 다른 서비스에 이벤트로 전달된다.
- batch size를 너무 크게 잡으면 한 번의 트랜잭션이 길어진다.
- timeout 기준은 운영 데이터와 장애 대응 정책에 맞춰 조정해야 한다.

## 대안

| 방식 | 장점 | 단점 |
|---|---|---|
| DB polling + Redisson lock | 단순하고 현재 인프라와 잘 맞음 | DB를 주기적으로 조회함 |
| ShedLock | 구현이 쉽고 DB 기반으로 동작 | 별도 lock table 필요, 현재 Redis 인프라와는 덜 자연스러움 |
| Quartz Cluster | 잡 이력/재시도/모니터링이 강함 | 현재 규모에서는 무거움 |
| Kafka delay topic | timeout을 이벤트 흐름으로 처리 가능 | delay topic 설계와 운영 복잡도 증가 |
| Redis ZSET delay queue | 빠르고 timeout 대상 조회가 효율적 | Redis 유실/복구와 DB 정합성 설계 필요 |

현재 프로젝트에서는 `DB polling + Redisson lock`을 기본 구현으로 선택한다. 추후 주문량이 커지면 Kafka delay topic 또는 별도 timeout worker로 분리할 수 있다.
