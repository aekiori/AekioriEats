@echo off
setlocal

powershell -ExecutionPolicy Bypass -File "%~dp0test_gateway_order_create.ps1" %*

endlocal
