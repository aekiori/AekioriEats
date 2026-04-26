# Kubernetes 배포 전략

## 목적

이 문서는 AekioriEats를 Kubernetes에 배포할 때 API, Kafka consumer, scheduler 역할을 어떻게 분리하고 운영할지 정리한다.

현재 `infra/k8s/order/order.yaml`은 `order-service`를 단일 Deployment 예시로 배포한다. 각 서비스 애플리케이션은 필요에 따라 HTTP API, Kafka consumer, scheduler 역할을 함께 가질 수 있다.

## 현재 구조

```text
{service} Deployment
  - HTTP API
  - Kafka consumers, optional
  - scheduler, optional
```

현재 manifest 기준:

```yaml
kind: Deployment
metadata:
  name: {service}
spec:
  replicas: 1
```

로컬/포트폴리오 환경에서는 이 구조가 가장 단순하다. Deployment 수가 적고, 하나의 애플리케이션 설정으로 API, consumer, scheduler를 함께 확인할 수 있다.

다만 운영 관점에서는 세 역할의 확장 기준이 다르다.

| 역할 | 확장 기준 |
|---|---|
| API | HTTP 요청량, latency, CPU 사용량 |
| Consumer | Kafka lag, 처리량, partition 수 |
| Scheduler | job 실행 주기, 실행 시간, 중복 실행 방지 |

그래서 최종적으로는 API, consumer, scheduler를 분리할 수 있는 구조를 목표로 둔다.

## Scheduler 중복 실행 문제

Spring `@Scheduled`는 애플리케이션 인스턴스마다 실행된다.

따라서 scheduler가 포함된 Deployment의 replicas를 2개 이상으로 늘리면 모든 Pod가 같은 job을 실행하려고 한다.

중복 실행이 문제가 되는 scheduler는 Redisson `RLock` 같은 분산락으로 보호한다.

```text
Pod A -> lock 획득 -> job 실행
Pod B -> lock 획득 실패 -> skip
Pod C -> lock 획득 실패 -> skip
```

즉, 분산락은 Pod가 여러 개 떠도 한 인스턴스만 scheduler job을 수행하도록 막는 장치다. 현재 Order scheduler는 Redisson lock을 사용한다.

현재 manifest는 `replicas: 1`이라 Redisson lock이 필수는 아니다.
다만 추후 scheduler가 포함된 Deployment의 replicas를 늘리거나 API/consumer/scheduler를 역할별로 분리할 때, 같은 job이 중복 실행되지 않도록 미리 안전장치를 둔다.  

## 역할별 Deployment 분리안

운영 규모가 커지면 하나의 애플리케이션 이미지를 역할별 Deployment로 나눌 수 있다.

```text
{service}-api Deployment
  - HTTP API only

{service}-consumer Deployment
  - Kafka consumers only

{service}-scheduler Deployment
  - scheduler only
```

이 방식의 목적은 역할별 확장 기준을 분리하는 것이다. API는 HTTP 트래픽에 따라 replicas를 늘리고, consumer는 Kafka lag에 따라 replicas를 조정하며, scheduler는 job 중복 실행과 실행 주기를 기준으로 운영한다.

## replicas 전략

| 구조 | replicas | 장점 | 단점 |
|---|---:|---|---|
| 단일 Deployment | 1 | 단순함, 로컬/포트폴리오 운영 쉬움 | 역할별 확장 불가 |
| 단일 Deployment + 분산락 | 2 이상 | API/consumer HA와 scheduler 중복 방지 | 모든 Pod에 scheduler 코드가 함께 올라감 |
| 역할별 Deployment | API/consumer만 2 이상, scheduler 1 | 역할별 확장 기준 분리 | Deployment와 설정 관리 증가 |
| scheduler 전용 Deployment + 분산락 | 2 이상 | scheduler HA 확보 | scheduler 전용 Pod 대부분이 대기 상태라 과할 수 있음 |

대부분의 scheduler job은 실시간 요청 경로가 아니다. 따라서 scheduler 전용 Deployment를 `replicas: 1`로 두고 짧은 재시작 공백을 허용하는 전략도 가능하다.

현재 프로젝트에서는 `단일 Deployment replicas 1`을 기본으로 두고, 중복 실행이 문제가 되는 scheduler에는 분산락을 둔다. 나중에 운영 환경에서 API, consumer, scheduler를 분리하면 API/consumer는 각자의 부하 기준으로 확장하고 scheduler는 별도 Deployment에서 운영할 수 있다.

## 환경 변수 운영

역할별 Deployment로 분리할 때는 같은 이미지를 쓰더라도 환경 변수로 API, consumer, scheduler 실행 여부를 조정할 수 있다.

Consumer는 Spring Kafka의 listener auto-startup 옵션으로 끄고 켠다.

```yaml
env:
  - name: SPRING_KAFKA_LISTENER_AUTO_STARTUP
    value: "false"
```

Scheduler는 서비스별 scheduler enable 옵션으로 끄고 켠다.

| 환경 변수 | 기본값 | 설명 |
|---|---:|---|
| `SPRING_KAFKA_LISTENER_AUTO_STARTUP` | `true` | Kafka listener 자동 시작 여부 |

서비스별 scheduler 환경 변수는 다음과 같다.

| 서비스 | 환경 변수 | 설명 |
|---|---|---|
| Auth | `AUTH_TOKEN_CLEANUP_ENABLED` | 만료/폐기 refresh token cleanup scheduler 활성화 여부 |
| Order | `ORDER_TIMEOUT_COMPENSATION_ENABLED` | 주문 timeout 보상 scheduler 활성화 여부 |

Order scheduler는 Redisson lock과 batch 관련 설정도 가진다.

| 환경 변수 | 기본값 | 설명 |
|---|---:|---|
| `ORDER_TIMEOUT_COMPENSATION_FIXED_DELAY_MS` | `60000` | job 실행 간격 |
| `ORDER_TIMEOUT_COMPENSATION_INITIAL_DELAY_MS` | `30000` | 최초 실행 지연 |
| `ORDER_TIMEOUT_COMPENSATION_BATCH_SIZE` | `100` | 상태별 처리 batch 크기 |
| `ORDER_TIMEOUT_COMPENSATION_LOCK_NAME` | `order:timeout-compensation` | Redisson lock 이름 |
| `ORDER_TIMEOUT_COMPENSATION_LOCK_WAIT_MS` | `0` | lock 획득 대기 시간 |
| `ORDER_TIMEOUT_COMPENSATION_LOCK_LEASE_MS` | `60000` | lock lease 시간 |

역할별 Deployment로 분리한다면 API/consumer Pod에서는 scheduler를 끄고, scheduler Pod에서만 켜는 방식으로 운영할 수 있다.

```yaml
env:
  - name: ORDER_TIMEOUT_COMPENSATION_ENABLED
    value: "false"
```

예를 들면 역할별 설정은 다음처럼 나눌 수 있다.

| Deployment | `SPRING_KAFKA_LISTENER_AUTO_STARTUP` | Scheduler enabled |
|---|---|---|
| `{service}-api` | `false` | `false` |
| `{service}-consumer` | `true` | `false` |
| `{service}-scheduler` | `false` | `true` |

## 선택 기준

현재 포트폴리오/로컬 운영 기준:

- Deployment는 단순하게 유지한다.
- 중복 실행이 문제가 되는 scheduler는 분산락으로 보호한다.
- scheduler job은 실시간 요청 경로가 아니므로 replicas 1의 짧은 공백은 허용한다.

운영 규모가 커진 뒤:

- API 트래픽이 늘면 API Deployment replicas를 늘린다.
- consumer 처리량이 병목이면 consumer 전용 Deployment 분리를 검토한다.
- scheduler 실행 시간이 길어지거나 운영 모니터링 요구가 커지면 scheduler 전용 Deployment 또는 Quartz Cluster 같은 전용 스케줄링 도구를 검토한다.

## 관련 문서

- [Order Timeout Compensation 설계](../order-timeout-compensation.md)
- [Kafka / Debezium 운영 정리](kafka-debezium.md)
- [Prometheus / Grafana 로컬 구성](prometheus-grafana.md)
