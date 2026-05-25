@echo off
:: stop-compile-server.cmd
:: Thin launcher that delegates to stop-compile-server.ps1.
::
:: Usage:
::   stop-compile-server.cmd <port>
::   stop-compile-server.cmd 0
::   stop-compile-server.cmd          (stops all daemons)
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass ^
    -File "%~dp0stop-compile-server.ps1" %*
exit /b %ERRORLEVEL%
