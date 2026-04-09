@echo off
setlocal

pushd "%~dp0.."

curl -X POST http://localhost:8080/api/v1/orders -H "Content-Type: application/json" -H "X-Trace-Id: trace-order-001" --data-binary @"%~dp0..\sample-order-create.json"
echo.
echo.
curl -X POST http://localhost:8080/api/v1/orders -H "Content-Type: application/json" -H "X-Trace-Id: trace-order-002" --data-binary @"%~dp0..\sample-order-create.json"
echo.
echo.
docker compose -f docker/compose.yml exec mysql mysql -uroot -proot -D delivery -e "select count(*) as order_count from orders; select count(*) as outbox_count from outbox; select id, event_id, event_type, status, created_at from outbox order by created_at desc;"
echo.
echo.
docker compose -f docker/compose.yml exec kafka kafka-console-consumer --bootstrap-server kafka:29092 --topic outbox.event.ORDER --max-messages 1

popd

endlocal
