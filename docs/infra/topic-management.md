# Kafka 토픽 관리

## 단일 기준
- Kafka 토픽의 단일 기준은 Terraform 설정(`infra/terraform/kafka`)입니다.
- 예전에 사용하던 `topics.yml` 매니페스트나 수동 생성/검증 스크립트는 중복 관리를 막기 위해 제거했습니다.

## 목적
- 브로커의 자동 토픽 생성에 의존하지 않습니다.
- 토픽 변경 사항을 코드 리뷰 가능한 형태로 남깁니다.
- 로컬/개발/운영 환경 사이의 설정 차이를 줄입니다.

## 관리 경계
Terraform이 관리하는 것:
- 토픽 이름
- 파티션 수
- 복제 계수(replication factor)
- 토픽 config

애플리케이션 런타임이 관리하는 것:
- 컨슈머 그룹 생성/조인/생명주기
- Kafka listener 동작
- offset 처리 방식

## Terraform 작업 흐름
1. `infra/terraform/kafka/terraform.tfvars`를 수정합니다.
2. `terraform init`을 실행합니다.
3. `terraform plan`으로 변경 내용을 확인합니다.
4. 문제가 없으면 `terraform apply`를 실행합니다.
5. 이미 클러스터에 존재하는 토픽을 Terraform으로 관리하려면 먼저 `terraform import`를 수행합니다.

자세한 내용은 [Kafka 토픽 Terraform 관리](./kafka-terraform.md)를 참고합니다.

## 확인 방법
- Kafka UI: `http://localhost:8989`
- Kafka UI에서 토픽, 컨슈머 그룹, 메시지 흐름을 빠르게 확인합니다.

## 현재 원칙
- 신규 토픽은 Terraform에 추가합니다.
- Kafka Connect 내부 토픽과 Debezium schema history 토픽은 Terraform 관리 대상에서 제외합니다.
- Outbox 이벤트 토픽은 `event_type` 기준으로 분리합니다.