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
# -J-Djava.io.tmpdir: jar is itself a JVM process, and by default writes its own
# hsperfdata (perf counter) file into java.io.tmpdir while it runs. If that
# defaults to the same OS temp root $TMP was created under, jar can race with
# its own live pid file while recursively copying "$TMP" (observed as
# FileNotFoundException: ...\hsperfdata_<user>\<pid> on Windows runners).
# Point it at its own scratch dir so it never writes into what it's packaging.
JAR_TMP="$(mktemp -d)"
trap 'rm -rf "$TMP" "$JAR_TMP"' EXIT
jar "-J-Djava.io.tmpdir=$JAR_TMP" cfe "$JAR" CompileServer -C "$TMP" .

echo "Done: $JAR"
