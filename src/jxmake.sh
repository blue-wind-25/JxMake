#!/bin/bash
set -euo pipefail

##### This script cannot be used in these environments
KNM=`uname -s | tr '[:upper:]' '[:lower:]'`

if [[ "${KNM}" == *'cygwin'* ]] ||
   [[ "${KNM}" == *'mingw'*  ]] ||
   [[ "${KNM}" == *'msys'*   ]]; then
    echo $'\nPlease use \'jxmake.bat\' (instead of \'jxmake.sh\') in this environment.\n'
    exit 1
fi

##### Get the directory where the script file is located
SCRIPT_SOURCE="${BASH_SOURCE:-$0}"
DIR="$(dirname "$SCRIPT_SOURCE")"
DIR="$(cd "$DIR" && pwd)"

##### Move into that directory so the JAR can be found
cd "$DIR" || exit

##### Use alternate Java if JXMAKE_JAVA is defined
JAVA_CMD="${JXMAKE_JAVA:-java}"

##### Get Java version using "java.specification.version"
JAVA_VERSION=$("$JAVA_CMD" -XshowSettings:properties -version 2>&1 | awk -F '=' '/java.specification.version/ { print $2 }' | xargs)

if [ -z "$JAVA_VERSION" ]; then
    echo "[ERROR] Unable to determine Java version."
    exit 1
fi

##### Check if "--enable-native-access" flag is required
NEEDS_NATIVE_ACCESS=$(echo "$JAVA_VERSION" | awk -F. '{ if ($1 >= 22) print "true"; else print "false" }')

##### Build the full classpath
DEP_CP=(jansi.jar jSerialComm.jar zstd-jni.jar spellchecker.jar rstaui.jar rsyntaxtextarea.jar autocomplete.jar java-does-usb.jar pty4j.jar annotations.jar slf4j-api.jar kotlin-stdlib.jar jna.jar jna-platform.jar)

ALL_CP="$DIR/jxmake_dist/jxmake.jar"

for jar in "${DEP_CP[@]}"; do
    ALL_CP="$ALL_CP:$DIR/jxmake_dist/libs/$jar"
done

##### Define the flags
FLAGS=("-Xms512m" "-Xmx2048m" "-Xss2m" "-XX:+UseG1GC" "-XX:MaxGCPauseMillis=200" "-XX:+ParallelRefProcEnabled" "-XX:+AlwaysPreTouch")

if [ "$NEEDS_NATIVE_ACCESS" == "true" ]; then
    FLAGS=("--enable-native-access=org.fusesource.jansi,com.fazecast.jSerialComm,com.github.luben.zstd,net.codecrete.usb,com.sun.jna,com.sun.jna.platform,ALL-UNNAMED" "${FLAGS[@]}")
fi

##### Run the JxMake JAR file with the appropriate flag based on the major version
exec "$JAVA_CMD" "${FLAGS[@]}" -cp "$ALL_CP" JXM_Main "$@"
