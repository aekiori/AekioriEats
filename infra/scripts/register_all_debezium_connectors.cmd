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

call :register_connector "order-outbox-connector" "Order\infra\debezium\order-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :register_connector "user-outbox-connector" "User\infra\debezium\user-outbox-connector-smt.json"
if errorlevel 1 set "FAILED=1"

call :register_connector "auth-outbox-connector" "Auth\infra\debezium\auth-outbox-connector-smt.json"
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

:register_connector
set "NAME=%~1"
set "FILE=%~2"

if not exist "%FILE%" (
    echo [FAIL] missing file: %FILE%
    exit /b 1
)

for /f %%H in ('curl -s -o NUL -w "%%{http_code}" "%CONNECT_URL%/connectors/%NAME%"') do set "EXISTS_CODE=%%H"
if "!EXISTS_CODE!"=="200" (
    echo [INFO] connector exists. delete first: %NAME%
    for /f %%D in ('curl -s -o NUL -w "%%{http_code}" -X DELETE "%CONNECT_URL%/connectors/%NAME%"') do set "DELETE_CODE=%%D"
    if not "!DELETE_CODE!"=="204" if not "!DELETE_CODE!"=="404" (
        echo [FAIL] delete failed. connector=%NAME% status=!DELETE_CODE!
        exit /b 1
    )
)

echo [INFO] register connector: %NAME%
for /f %%R in ('curl -s -o NUL -w "%%{http_code}" -X POST -H "Content-Type: application/json" --data-binary "@%FILE%" "%CONNECT_URL%/connectors"') do set "REGISTER_CODE=%%R"
if "!REGISTER_CODE!"=="201" (
    echo [OK] registered: %NAME%
    exit /b 0
)

echo [FAIL] register failed. connector=%NAME% status=!REGISTER_CODE!
exit /b 1
