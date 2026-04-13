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

call :upsert_connector "order-outbox-connector" "Order\infra\debezium\order-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :upsert_connector "user-outbox-connector" "User\infra\debezium\user-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :upsert_connector "auth-outbox-connector" "Auth\infra\debezium\auth-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :upsert_connector "store-outbox-connector" "Store\infra\debezium\store-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :upsert_connector "payment-outbox-connector" "Payment\infra\debezium\payment-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

if defined FAILED (
    echo.
    echo [FAIL] one or more connectors failed
    popd >NUL
    exit /b 1
)

echo.
echo [OK] all connectors are registered
popd >NUL
exit /b 0

:upsert_connector
set "NAME=%~1"
set "FILE=%~2"

if not exist "%FILE%" (
    echo [FAIL] missing file: %FILE%
    exit /b 1
)

set "TMP_CONFIG=%TEMP%\%NAME%-config-%RANDOM%%RANDOM%.json"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$obj = Get-Content -LiteralPath '%FILE%' -Raw | ConvertFrom-Json; if ($null -eq $obj.config) { exit 2 }; $obj.config | ConvertTo-Json -Depth 100 | Set-Content -LiteralPath '%TMP_CONFIG%' -Encoding Ascii"
if errorlevel 1 (
    echo [FAIL] cannot build config payload from file: %FILE%
    if exist "%TMP_CONFIG%" del /q "%TMP_CONFIG%" >NUL 2>NUL
    exit /b 1
)

echo [INFO] upsert connector: %NAME%
for /f %%R in ('curl -s -o NUL -w "%%{http_code}" -X PUT -H "Content-Type: application/json" --data-binary "@%TMP_CONFIG%" "%CONNECT_URL%/connectors/%NAME%/config"') do set "PUT_CODE=%%R"
if exist "%TMP_CONFIG%" del /q "%TMP_CONFIG%" >NUL 2>NUL

if "!PUT_CODE!"=="200" (
    echo [OK] upserted: %NAME%
    exit /b 0
)
if "!PUT_CODE!"=="201" (
    echo [OK] upserted: %NAME%
    exit /b 0
)

echo [FAIL] upsert failed. connector=%NAME% status=!PUT_CODE!
exit /b 1
