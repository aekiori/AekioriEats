#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

curl -X POST -H "Content-Type: application/json" \
  --data-binary @Order/infra/debezium/order-outbox-connector-smt.json \
  http://localhost:8083/connectors
