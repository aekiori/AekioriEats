@echo off
setlocal

powershell -ExecutionPolicy Bypass -File "%~dp0apply_kafka_topics.ps1" %*

endlocal
