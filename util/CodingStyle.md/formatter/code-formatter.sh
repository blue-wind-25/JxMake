#!/usr/bin/env bash
# code-formatter.sh — wrapper for code-formatter.jar / code-formatter-1.00.jar
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VERSIONED_JAR="code-formatter-1.00.jar"

if [ -f "$SCRIPT_DIR/code-formatter.jar" ]; then
    exec java -jar "$SCRIPT_DIR/code-formatter.jar" "$@"
elif [ -f "$SCRIPT_DIR/$VERSIONED_JAR" ]; then
    exec java -jar "$SCRIPT_DIR/$VERSIONED_JAR" "$@"
else
    echo "error: neither code-formatter.jar nor $VERSIONED_JAR found in $SCRIPT_DIR" >&2
    exit 1
fi
