@echo off
setlocal

powershell -ExecutionPolicy Bypass -File "%~dp0list_kafka_runtime.ps1" %*

endlocal
