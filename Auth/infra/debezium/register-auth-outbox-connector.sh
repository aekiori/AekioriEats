curl -X POST -H "Content-Type: application/json" \
  --data-binary @Auth/infra/debezium/auth-outbox-connector-smt.json \
  http://localhost:8083/connectors
