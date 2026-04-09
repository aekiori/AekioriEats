@echo off
setlocal

echo [k6] sending traffic to 4 domains (default: 30 rps each)
echo [k6] default mode: chaos
echo [k6] mixed mode example: run-domain-30rps.cmd -e MODE=mixed -e ERROR_4XX_RATIO=0.05 -e ERROR_5XX_RATIO=0.05
echo [k6] press Ctrl+C to stop
echo.

k6 run "%~dp0k6_domains_30rps_forever.js" %*

endlocal
