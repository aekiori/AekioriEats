@echo off
setlocal EnableExtensions
set "ROOT=%~dp0..\.."
call "%ROOT%\infra\debezium\register-all-outbox-connectors.cmd" %*
exit /b %ERRORLEVEL%
