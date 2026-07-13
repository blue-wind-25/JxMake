@echo off
:: code-formatter.cmd — wrapper for code-formatter.jar / code-formatter-1.00.jar
setlocal

set "SCRIPT_DIR=%~dp0"
set "VERSIONED_JAR=code-formatter-1.00.jar"

if exist "%SCRIPT_DIR%code-formatter.jar" (
    java -jar "%SCRIPT_DIR%code-formatter.jar" %*
    exit /b %ERRORLEVEL%
)
if exist "%SCRIPT_DIR%%VERSIONED_JAR%" (
    java -jar "%SCRIPT_DIR%%VERSIONED_JAR%" %*
    exit /b %ERRORLEVEL%
)
if exist "%SCRIPT_DIR%\target\%VERSIONED_JAR%" (
    java -jar "%SCRIPT_DIR%\target\%VERSIONED_JAR%" %*
    exit /b %ERRORLEVEL%
)
echo error: neither code-formatter.jar nor %VERSIONED_JAR% found in %SCRIPT_DIR%
exit /b 1
