# 로컬 시작 가이드

이 문서는 AekioriEats를 로컬에서 처음 띄울 때 필요한 필수 절차를 한 곳에 모아둔 실행 순서표다.

## 전체 순서

1. 인프라 컨테이너 기동
2. Kafka 토픽 Terraform 적용
3. Debezium connector 등록
4. 애플리케이션 컨테이너 기동
5. 상태 확인

## 0. 전제 조건

로컬에 아래 도구가 필요하다.

- Docker Desktop
- Terraform
- Java 17 이상

Windows 기준으로 명령은 프로젝트 루트에서 실행한다.

```cmd
cd C:\Users\wildphs\Desktop\AekioriEats
```

## 1. 인프라 컨테이너 기동

MySQL, Redis, Kafka, Kafka Connect, Kafka UI, Prometheus, Grafana를 먼저 띄운다.

```cmd
docker compose --env-file infra/docker/infra/.env.infra -f infra/docker/infra/compose.infra.yml up -d --build
```

상태 확인:

```cmd
docker ps
```

Kafka Connect가 준비됐는지 확인:

```cmd
curl http://localhost:8083/connectors
```

## 2. Kafka 토픽 Terraform 적용

이 프로젝트는 Kafka 토픽을 수동 생성하지 않고 Terraform으로 관리한다.

```cmd
cd infra\terraform\kafka
terraform init
terraform plan
terraform apply
cd ..\..\..
```

운영 원칙:

- 변경 전 `terraform plan`을 먼저 확인한다.
- `terraform apply`는 plan 결과가 의도한 토픽 변경인지 확인한 뒤 실행한다.
- Kafka Connect 내부 토픽과 Debezium schema history 토픽은 Terraform 관리 대상에서 제외한다.

자세한 내용:

- [Kafka 토픽 Terraform 관리](../infra/kafka-terraform.md)
- [Kafka / Debezium 운영 정리](../infra/kafka-debezium.md)

## 3. Debezium connector 등록

Kafka Connect가 떠 있고, Kafka 토픽이 준비된 뒤 connector를 등록한다.

```cmd
infra\debezium\register-all-outbox-connectors.cmd
```

기본 Kafka Connect 주소는 `http://localhost:8083`이다.

다른 주소로 등록해야 하면 첫 번째 인자로 넘긴다.

```cmd
infra\debezium\register-all-outbox-connectors.cmd http://localhost:8083
```

등록 대상:

- Auth outbox connector
- User outbox connector
- Order outbox connector
- Store outbox connector
- Payment outbox connector
- Point outbox connector

주의:

- 현재 스크립트는 `POST /connectors` 방식이다.
- 이미 connector가 있으면 Kafka Connect가 `409 Conflict`를 반환한다.
- 기존 connector 설정을 바꾸려면 기존 connector를 삭제한 뒤 다시 등록하거나, 필요 시 `PUT /connectors/{name}/config` 방식으로 별도 갱신한다.

상태 확인:

```cmd
curl http://localhost:8083/connectors
curl http://localhost:8083/connectors/order-outbox-connector/status
curl http://localhost:8083/connectors/store-outbox-connector/status
curl http://localhost:8083/connectors/payment-outbox-connector/status
curl http://localhost:8083/connectors/point-outbox-connector/status
```

## 4. 애플리케이션 컨테이너 기동

전체 서비스를 Docker로 띄울 때 사용한다.

```cmd
docker compose --env-file infra/docker/app/.env.app -f infra/docker/app/compose.app.yml up -d --build
```

포함 서비스:

- auth-service
- user-service
- order-service
- store-service
- payment-service
- point-service
- gateway-service
- frontend-service

주의:

- Docker 내부 서비스는 Kafka를 `kafka:29092`로 바라본다.
- 호스트에서 실행하는 도구나 로컬 IDE 실행 앱은 Kafka를 보통 `localhost:9092`로 바라본다.
- 로컬 IDE로 서비스를 직접 띄우는 경우에는 `infra/docker/app/.env.app` 값과 애플리케이션 로컬 설정의 Kafka 주소를 혼동하지 않는다.

## 5. 상태 확인

컨테이너 상태:

```cmd
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

Kafka 토픽/컨슈머 확인:

```cmd
infra\scripts\list_kafka_runtime.cmd
```

주요 UI:

- Kafka UI: `http://localhost:8088`
- Kafka Connect: `http://localhost:8083`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Frontend: `http://localhost:3001`
- Gateway: `http://localhost:8080`

## 자주 하는 작업

인프라만 다시 빌드:

```cmd
docker compose --env-file infra/docker/infra/.env.infra -f infra/docker/infra/compose.infra.yml up -d --build
```

앱만 다시 빌드:

```cmd
docker compose --env-file infra/docker/app/.env.app -f infra/docker/app/compose.app.yml up -d --build
```

connector 목록 확인:

```cmd
curl http://localhost:8083/connectors
```

connector 삭제 후 재등록 예시:

```cmd
curl -X DELETE http://localhost:8083/connectors/order-outbox-connector
infra\debezium\register-all-outbox-connectors.cmd
```

## 문서 위치 기준

- 실행 순서: `docs/start/README.md`
- Kafka 토픽 관리: `docs/infra/kafka-terraform.md`
- Kafka/Debezium 운영 메모: `docs/infra/kafka-debezium.md`
- 이벤트 흐름 설계: `docs/event-design.md`
- 관측 구성: `docs/infra/prometheus-grafana.md`
