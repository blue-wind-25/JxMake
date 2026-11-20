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

##### Collect the plugin classpath (if any), resolving relative entries against the caller's
##### directory (this must be done before changing the directory below)
PLUGIN_CP=""

if [ -n "${JXMAKE_PLUGIN_CLASSPATH:-}" ]; then
    IFS=':' read -r -a PLUGIN_ENTRIES <<< "$JXMAKE_PLUGIN_CLASSPATH"
    for entry in "${PLUGIN_ENTRIES[@]}"; do
        [ -z "$entry" ] && continue
        [[ "$entry" == /* ]] || entry="$PWD/$entry"
        PLUGIN_CP="$PLUGIN_CP:$entry"
    done
fi

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

# Append the plugin classpath last so plugins cannot shadow JxMake or 3rd party classes
ALL_CP="$ALL_CP$PLUGIN_CP"

##### Define the flags
FLAGS=("-Xms512m" "-Xmx2048m" "-Xss2m" "-XX:+UseG1GC" "-XX:MaxGCPauseMillis=200" "-XX:+ParallelRefProcEnabled" "-XX:+AlwaysPreTouch")

# NOTE : All JARs are loaded via '-cp', so they all belong to the unnamed module; only
#        'ALL-UNNAMED' is effective here (listing module names would only produce "Unknown module"
#        warnings). If the JARs are ever loaded via '--module-path' instead, list their module
#        names here (see 'src/UPDATE_3RD_PARTY_JARS.md').
if [ "$NEEDS_NATIVE_ACCESS" == "true" ]; then
    FLAGS=("--enable-native-access=ALL-UNNAMED" "${FLAGS[@]}")
fi

##### Run the JxMake JAR file with the appropriate flag based on the major version
exec "$JAVA_CMD" "${FLAGS[@]}" -cp "$ALL_CP" JXM_Main "$@"
