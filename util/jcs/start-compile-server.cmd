@echo off
:: start-compile-server.cmd
:: Thin launcher that delegates to start-compile-server.ps1.
::
:: Usage:
::   start-compile-server.cmd <port>
::   start-compile-server.cmd 0
::   start-compile-server.cmd 0 "C:\jdk21\bin\java.exe"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass ^
    -File "%~dp0start-compile-server.ps1" %*
exit /b %ERRORLEVEL%
