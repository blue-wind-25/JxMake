#!/usr/bin/env bash
# build-server.sh
# Compiles CompileServer.java into compile-server.jar
# Run this once with any JDK >= 8.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$SCRIPT_DIR/CompileServer.java"
JAR="$SCRIPT_DIR/compile-server.jar"
TMP="$(mktemp -d)"

trap 'rm -rf "$TMP"' EXIT

echo "Compiling CompileServer.java..."
# Use --release 8 on JDK 9+ (stricter cross-compilation, also checks bootstrap API).
# Fall back to -source/-target for JDK 8 which predates --release.
_JAVAC_VER=$(javac -version 2>&1)
if echo "$_JAVAC_VER" | grep -qE '^javac 1\.'; then
    javac -source 8 -target 8 -d "$TMP" "$SRC"
else
    javac --release 8 -d "$TMP" "$SRC"
fi
unset _JAVAC_VER

echo "Packaging compile-server.jar..."
# -J-XX:-UsePerfData: jar is itself a JVM process, and by default writes its own
# hsperfdata (perf counter) file into the OS temp dir (GetTempPath on Windows --
# NOT controlled by the java.io.tmpdir system property) while it runs. If that
# is the same root "$TMP" was created under, jar can race with its own live pid
# file while recursively copying "$TMP" -- observed on Windows runners as:
#     FileNotFoundException: ...\hsperfdata_<user>\<pid> (The process cannot
#     access the file because it is being used by another process)
# Disabling perf-data output entirely avoids the file ever existing.
jar -J-XX:-UsePerfData cfe "$JAR" CompileServer -C "$TMP" .

echo "Done: $JAR"
