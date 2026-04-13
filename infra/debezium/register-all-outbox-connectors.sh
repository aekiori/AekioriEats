#!/usr/bin/env bash
set -euo pipefail

CONNECT_URL="${1:-http://localhost:8083}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

if ! command -v jq >/dev/null 2>&1; then
  echo "[FAIL] jq is required for PUT payload extraction (.config)"
  exit 1
fi

upsert_connector() {
  local name="$1"
  local file="$2"

  if [[ ! -f "${file}" ]]; then
    echo "[FAIL] missing file: ${file}"
    return 1
  fi

  local config_payload
  config_payload="$(jq -c '.config' "${file}")"

  echo "[INFO] upsert connector: ${name}"
  local upsert_code
  upsert_code="$(curl -s -o /dev/null -w "%{http_code}" -X PUT -H "Content-Type: application/json" --data-raw "${config_payload}" "${CONNECT_URL}/connectors/${name}/config")"
  if [[ "${upsert_code}" == "200" || "${upsert_code}" == "201" ]]; then
    echo "[OK] upserted: ${name}"
    return 0
  fi

  echo "[FAIL] upsert failed. connector=${name} status=${upsert_code}"
  return 1
}

FAILED=0

upsert_connector "order-outbox-connector" "Order/infra/debezium/order-outbox-connector-smt.json" || FAILED=1
upsert_connector "user-outbox-connector" "User/infra/debezium/user-outbox-connector-smt.json" || FAILED=1
upsert_connector "auth-outbox-connector" "Auth/infra/debezium/auth-outbox-connector-smt.json" || FAILED=1
upsert_connector "store-outbox-connector" "Store/infra/debezium/store-outbox-connector-smt.json" || FAILED=1

if [[ "${FAILED}" -ne 0 ]]; then
  echo
  echo "[FAIL] one or more connectors failed"
  exit 1
fi

echo
echo "[OK] all connectors are registered"
