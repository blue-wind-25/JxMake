#!/usr/bin/env bash
# build-server.sh
# Compiles CompileServer.java into compile-server.jar
# Run this once with any JDK >= 11.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$SCRIPT_DIR/CompileServer.java"
JAR="$SCRIPT_DIR/compile-server.jar"
TMP="$(mktemp -d)"

trap 'rm -rf "$TMP"' EXIT

echo "Compiling CompileServer.java..."
javac -source 11 -target 11 -d "$TMP" "$SRC"

echo "Packaging compile-server.jar..."
jar cfe "$JAR" CompileServer -C "$TMP" .

echo "Done: $JAR"
