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

call :put_connector "Auth\infra\debezium\auth-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :put_connector "User\infra\debezium\user-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :put_connector "Order\infra\debezium\order-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :put_connector "Store\infra\debezium\store-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :put_connector "Payment\infra\debezium\payment-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :put_connector "Point\infra\debezium\point-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

if defined FAILED (
    echo.
    echo [FAIL] one or more connectors failed
    echo [HINT] This script uses PUT upsert. Check connector config JSON and Kafka Connect health.
    popd >NUL
    exit /b 1
)

echo.
echo [OK] all connectors are registered
popd >NUL
exit /b 0

:put_connector
set "FILE=%~1"
set "NAME="
set "TEMP_FILE="

if not exist "%FILE%" (
    echo [FAIL] missing file: %FILE%
    exit /b 1
)

for /f "usebackq delims=" %%N in (`powershell -NoProfile -Command "$json = Get-Content -LiteralPath '%FILE%' -Raw | ConvertFrom-Json; if (-not $json.name) { exit 1 }; [Console]::Out.Write($json.name)"`) do set "NAME=%%N"
if not defined NAME (
    echo [FAIL] cannot extract connector name: %FILE%
    exit /b 1
)

set "TEMP_FILE=%TEMP%\%NAME%-config.json"

echo [INFO] upsert connector: %NAME%
powershell -NoProfile -Command ^
    "$json = Get-Content -LiteralPath '%FILE%' -Raw | ConvertFrom-Json; $json.config | ConvertTo-Json -Depth 100 | Set-Content -LiteralPath '%TEMP_FILE%' -Encoding UTF8"
if errorlevel 1 (
    echo [FAIL] cannot extract config body: %FILE%
    if exist "%TEMP_FILE%" del /q "%TEMP_FILE%" >NUL 2>&1
    exit /b 1
)

for /f %%R in ('curl -s -o NUL -w "%%{http_code}" -X PUT -H "Content-Type: application/json" --data-binary "@%TEMP_FILE%" "%CONNECT_URL%/connectors/%NAME%/config"') do set "PUT_CODE=%%R"
if exist "%TEMP_FILE%" del /q "%TEMP_FILE%" >NUL 2>&1

if "!PUT_CODE!"=="200" (
    echo [OK] upserted: %NAME%
    exit /b 0
)

echo [FAIL] upsert failed. connector=%NAME% status=!PUT_CODE!
exit /b 1
