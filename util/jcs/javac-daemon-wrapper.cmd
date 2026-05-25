@echo off
:: javac-daemon-wrapper.cmd
:: Transparent javac replacement.  cmd.exe resolves .cmd before .exe, so
:: placing this file alongside javac.real.exe in the JDK bin directory
:: causes all "javac" invocations to be intercepted by this wrapper.
::
:: Setup:
::   1. Rename the real compiler:
::        rename "%JAVA_HOME%\bin\javac.exe" javac.real.exe
::   2. Copy this file and javac-daemon-wrapper.ps1 to the same directory.
::   3. Copy compile-server.jar to the same directory.
::
:: The wrapper delegates to the compile daemon and falls back to
:: javac.real.exe automatically if the daemon cannot start.
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass ^
    -File "%~dp0javac-daemon-wrapper.ps1" %*
exit /b %ERRORLEVEL%
