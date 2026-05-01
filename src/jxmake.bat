@echo off
setlocal enabledelayedexpansion

::::: Get the directory where the batch file is located
set "DIR=%~dp0"

::::: Move into that directory so the JAR can be found
cd /d "%DIR%"

::::: Use alternate Java if JXMAKE_JAVA is defined
if defined JXMAKE_JAVA (
    set "JAVA_CMD=%JXMAKE_JAVA%"
) else (
    set "JAVA_CMD=java"
)

::::: Get Java version using "java.specification.version"
set "JAVA_VER="
for /f "tokens=2 delims==" %%V in ('%JAVA_CMD% -XshowSettings:properties -version 2^>^&1 ^| findstr "java.specification.version"') do (
    for /f "tokens=* delims= " %%A in ("%%V") do set "JAVA_VER=%%A"
)

if "!JAVA_VER!"=="" (
    echo [ERROR] Unable to determine Java version.
    exit /b 1
)

::::: Extract the major version from JAVA_VER
set "JAVA_MAJOR=0"
for /f "tokens=1 delims=." %%V in ("!JAVA_VER!") do set "JAVA_MAJOR=%%V"

::::: Check if "--enable-native-access" flag is required
set "NATIVE_ACCESS_FLAG="
if !JAVA_MAJOR! GEQ 22 (
    set "NATIVE_ACCESS_FLAG=--enable-native-access=org.fusesource.jansi,com.fazecast.jSerialComm,net.codecrete.usb,com.sun.jna,com.sun.jna.platform,ALL-UNNAMED"
)

::::: Build the full classpath
set "DEP_CP=jansi.jar jSerialComm.jar spellchecker.jar rstaui.jar rsyntaxtextarea.jar autocomplete.jar java-does-usb.jar pty4j.jar annotations.jar slf4j-api.jar kotlin-stdlib.jar jna.jar jna-platform.jar"

set "ALL_CP=%DIR%\jxmake_dist\jxmake.jar"

for %%J in (%DEP_CP%) do (
    set "ALL_CP=!ALL_CP!;%DIR%\jxmake_dist\libs\%%J"
)

::::: Run the application JAR file with the appropriate flags based on the Java major version
"%JAVA_CMD%" !NATIVE_ACCESS_FLAG!    ^
    -Xms512m                         ^
    -Xmx2048m                        ^
    -Xss2m                           ^
    -XX:+UseG1GC                     ^
    -XX:MaxGCPauseMillis=200         ^
    -XX:+ParallelRefProcEnabled      ^
    -XX:+AlwaysPreTouch              ^
    -cp "%ALL_CP%" JXM_Main %*

endlocal
