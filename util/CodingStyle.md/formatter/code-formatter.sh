#!/usr/bin/env bash
# code-formatter.sh — wrapper for code-formatter.jar / code-formatter-<version>.jar
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

find_versioned_jar() {
    local dir="$1"
    local jars=("$dir"/code-formatter-*.jar)
    if [ -e "${jars[0]}" ]; then
        printf '%s\n' "${jars[0]}"
    fi
}

if [ -f "$SCRIPT_DIR/code-formatter.jar" ]; then
    exec java -jar "$SCRIPT_DIR/code-formatter.jar" "$@"
fi

VERSIONED_JAR="$(find_versioned_jar "$SCRIPT_DIR")"
if [ -n "$VERSIONED_JAR" ]; then
    exec java -jar "$VERSIONED_JAR" "$@"
fi

VERSIONED_JAR="$(find_versioned_jar "$SCRIPT_DIR/target")"
if [ -n "$VERSIONED_JAR" ]; then
    exec java -jar "$VERSIONED_JAR" "$@"
fi

echo "error: neither code-formatter.jar nor code-formatter-<version>.jar found in $SCRIPT_DIR" >&2
exit 1
