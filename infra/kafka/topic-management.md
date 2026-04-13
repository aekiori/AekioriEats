# Kafka Topic Management

## Source of Truth
- Primary: Terraform (`infra/terraform/kafka`)
- Secondary (local convenience): scripts under `infra/scripts`

## Why
- Avoid dependency on broker auto topic creation
- Keep topic changes reviewable and reproducible
- Detect and reduce config drift

## Ownership Boundary
- Terraform manages:
  - topic name
  - partitions
  - replication factor
  - topic configs
- Application runtime manages:
  - consumer group join/lifecycle
  - listener concurrency and offset behavior

## Terraform Workflow
1. Update `infra/terraform/kafka/terraform.tfvars`
2. Run `terraform init`
3. Run `terraform plan`
4. Run `terraform apply`
5. If cluster already has topics, run `terraform import` first

See `infra/terraform/kafka/README.md` for details.

## Visibility
- Kafka UI: `http://localhost:8989`
- Use UI for topic/group overview and quick checks
