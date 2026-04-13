@echo off
setlocal EnableExtensions
set "ROOT=%~dp0..\.."
pushd "%ROOT%" >NUL || exit /b 1
curl -X POST -H "Content-Type: application/json" --data-binary @Store\infra\debezium\store-outbox-connector-smt.json http://localhost:8083/connectors
set "CODE=%ERRORLEVEL%"
popd >NUL
exit /b %CODE%
