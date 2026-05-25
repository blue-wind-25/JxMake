@echo off
:: javac-client.cmd
:: Thin launcher that delegates to javac-client.ps1.
:: Place this file alongside javac-client.ps1.
::
:: Usage:
::   javac-client.cmd <port> [javac args...]
::   javac-client.cmd 0      [javac args...]
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass ^
    -File "%~dp0javac-client.ps1" %*
exit /b %ERRORLEVEL%
