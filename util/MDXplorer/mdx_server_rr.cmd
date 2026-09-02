@echo off
setlocal

set "DIR=%~dp0"

call "%DIR%mdx_server.cmd" %* -C "%DIR%..\.."
exit /b %errorlevel%
