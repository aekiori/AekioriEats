@echo off
setlocal

powershell -ExecutionPolicy Bypass -File "%~dp0hit-domain-error-metrics.ps1" %*

endlocal
