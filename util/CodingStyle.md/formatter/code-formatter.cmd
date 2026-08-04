@echo off
:: code-formatter.cmd — wrapper for code-formatter.jar / code-formatter-<version>.jar
setlocal

set "SCRIPT_DIR=%~dp0"

if exist "%SCRIPT_DIR%code-formatter.jar" (
    java -jar "%SCRIPT_DIR%code-formatter.jar" %*
    exit /b %ERRORLEVEL%
)

for %%F in ("%SCRIPT_DIR%code-formatter-*.jar") do (
    java -jar "%%~fF" %*
    exit /b %ERRORLEVEL%
)

for %%F in ("%SCRIPT_DIR%target\code-formatter-*.jar") do (
    java -jar "%%~fF" %*
    exit /b %ERRORLEVEL%
)

echo error: neither code-formatter.jar nor code-formatter-^<version^>.jar found in %SCRIPT_DIR%
exit /b 1
