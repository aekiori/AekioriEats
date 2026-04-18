@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "CONNECT_URL=%~1"
if "%CONNECT_URL%"=="" set "CONNECT_URL=http://localhost:8083"

set "ROOT=%~dp0..\.."
pushd "%ROOT%" >NUL || (
    echo [FAIL] cannot move to project root
    exit /b 1
)

set "FAILED="

call :post_connector "auth-outbox-connector" "Auth\infra\debezium\auth-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :post_connector "user-outbox-connector" "User\infra\debezium\user-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :post_connector "order-outbox-connector" "Order\infra\debezium\order-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :post_connector "store-outbox-connector" "Store\infra\debezium\store-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :post_connector "payment-outbox-connector" "Payment\infra\debezium\payment-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :post_connector "point-outbox-connector" "Point\infra\debezium\point-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

if defined FAILED (
    echo.
    echo [FAIL] one or more connectors failed
    echo [HINT] POST returns 409 if a connector already exists. Delete it first or switch this script back to PUT upsert.
    popd >NUL
    exit /b 1
)

echo.
echo [OK] all connectors are registered
popd >NUL
exit /b 0

:post_connector
set "NAME=%~1"
set "FILE=%~2"

if not exist "%FILE%" (
    echo [FAIL] missing file: %FILE%
    exit /b 1
)

echo [INFO] register connector: %NAME%
for /f %%R in ('curl -s -o NUL -w "%%{http_code}" -X POST -H "Content-Type: application/json" --data-binary "@%FILE%" "%CONNECT_URL%/connectors"') do set "POST_CODE=%%R"

if "!POST_CODE!"=="201" (
    echo [OK] registered: %NAME%
    exit /b 0
)

echo [FAIL] register failed. connector=%NAME% status=!POST_CODE!
exit /b 1
