@echo off
setlocal

powershell -ExecutionPolicy Bypass -File "%~dp0check-gateway-metrics-flow.ps1" %*

endlocal
