# Terraform 기반 Kafka 토픽 관리

## 운영 원칙
- Kafka 토픽을 수동 스크립트 대신 Terraform으로 IaC 하여 관리한다.
- 변경 이력을 리뷰 가능한 형태로 남기고, 환경 간 설정 차이를 줄이기 위함이다.
  - 수동 스크립트로 관리하기 번거로웠던 것도 있긴 하다.
- 운영 환경에서는 변경 시 `terraform plan`을 반드시 확인한 후 `terraform apply`수행 필요

## 파일 구성
- `versions.tf`: Terraform 및 Provider 버전
- `variables.tf`: 입력 변수 정의
- `main.tf`: `kafka_topic` 리소스 정의
- `outputs.tf`: 관리 대상 토픽 출력값
- `terraform.tfvars.example`: 로컬/개발 환경 기본 토픽 세트


## 트러블슈팅
- `terraform apply` 중 `client has run out of available brokers to talk to: EOF`가 발생하고 Kafka 로그에 `InvalidReceiveException`이 보이면 Provider TLS 설정 확인 필요
- 이 프로젝트의 로컬 브로커(`localhost:9092`)는 PLAINTEXT이므로 Provider에서 `tls_enabled = false`를 사용해야 함
- 호스트 도구(Terraform, 로컬 스크립트) 접속 주소: `localhost:9092`
- Docker 내부 클라이언트 접속 주소: `kafka:29092`

## Kafka Connect 내부 토픽 정책
`connect-configs`, `connect-offsets`, `connect-status`는 Kafka Connect 내부 토픽이므로 이 프로젝트에서는 Terraform 관리 대상에서 제외한다.

## Debezium Schema History 토픽 정책
대상 토픽:
- `schemahistory.delivery.order`
- `schemahistory.delivery.user`
- `schemahistory.delivery.auth`
- `schemahistory.delivery.store`

관리 원칙:
- 위 `schemahistory.*` 토픽은 Terraform 관리 대상에서 제외한다.
- 생성/수정/삭제는 Debezium/Kafka 운영 절차에서 관리한다.

제외 이유:
- Debezium 스키마 이력 쓰기 안정성 보장을 위해 운영 중 설정 충돌을 최소화한다.
- 잘못된 설정(예: `cleanup.policy=compact`)이 적용되면 `Compacted topic cannot accept message without key` 오류로 커넥터가 중단될 수 있습니다.

운영 체크:
1. `schemahistory.*`를 Terraform `topics`에 선언하지 않습니다.
2. 과거에 state에 편입했다면 `terraform state rm`으로 분리한다.
3. 변경 후 `terraform plan`에서 `schemahistory.*` 관련 생성/수정/삭제가 없는지 확인한다.

## Outbox 이벤트 토픽 라우팅
Outbox 이벤트 토픽 naming, Debezium SMT 라우팅 정책, aggregate -> event_type 전환 배경은 아래 문서를 기준으로 본다.

- [Kafka / Debezium 운영 정리](./kafka-debezium.md)

이 문서에서는 Terraform이 그 토픽들을 어떻게 선언하고 관리하는지만 다룬다.

## 기존 클러스터 편입(Import)
이미 존재하는 토픽을 Terraform으로 관리하려면 apply 전에 import를 수행한다.

예시:

```powershell
cd .\infra\terraform\kafka
terraform import 'kafka_topic.topics["outbox.event.ORDER"]' outbox.event.ORDER
terraform import 'kafka_topic.topics["outbox.event.USER"]' outbox.event.USER
terraform import 'kafka_topic.topics["outbox.event.PAYMENT"]' outbox.event.PAYMENT
```

`topics`에 선언된 토픽은 동일한 방식으로 모두 import하면 된다.

## 관리 경계(Ownership Boundary)
- Terraform이 관리:
  - 토픽 이름
  - 파티션 수
  - 복제 계수(replication factor)
  - 토픽 설정(config)
- 애플리케이션 런타임이 관리:
  - 컨슈머 그룹 조인 및 생명주기
  - 리스너 런타임 동작

요약: 토픽은 IaC(Terraform), 컨슈머 그룹은 런타임에서 생성/관리된다.
