@echo off
setlocal

powershell -ExecutionPolicy Bypass -File "%~dp0validate_kafka_topics.ps1" %*

endlocal
