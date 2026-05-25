#!/usr/bin/env bash
# javac-client.sh
# Drop-in javac replacement for Makefiles that routes compilation
# through a running compile-server daemon.
#
# The daemon must already be running (use start-compile-server.sh).
# Does NOT auto-start the server -- fails clearly if daemon is down.
#
# Usage in Makefile:
#   JAVAC = /path/to/javac-client.sh <port>
#   $(JAVAC) -cp ... -d build/classes src/Foo.java
#
# Usage from shell:
#   javac-client.sh 62650 -cp libs/*.jar -d out src/Main.java
#   javac-client.sh 0      ...           # port auto-derived from active JDK
#
# Protocol: NUL-wrapped (\x00TAG\x00) sentinel lines separate sections.
# tr strips NUL bytes before awk parses, so grep -E / awk can match
# plain keywords after stripping.
set -euo pipefail

# ── Helpers ──────────────────────────────────────────────────────────────────

# Derive port from Java major version: major * 1000
# Usage: derive_port <java-binary>
derive_port() {
    local java_bin="$1"
    local ver_str major
    ver_str=$("$java_bin" -version 2>&1 | head -1)
    if [[ "$ver_str" =~ \"1\.([0-9]+) ]]; then
        major="${BASH_REMATCH[1]}"
    elif [[ "$ver_str" =~ \"([0-9]+) ]]; then
        major="${BASH_REMATCH[1]}"
    else
        echo "ERROR: Cannot parse Java version from: $ver_str" >&2
        return 1
    fi
    echo $((major * 1000))
}

# ── Argument parsing ──────────────────────────────────────────────────────────

if [[ $# -lt 1 ]]; then
    echo "Usage: $0 <port> [javac args...]" >&2
    exit 1
fi

PORT="$1"
shift

if [[ "$PORT" == "0" ]]; then
    JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
    JAVA_BIN="${JAVA_BIN:-java}"
    PORT=$(derive_port "$JAVA_BIN")
fi

# ── Connectivity check ────────────────────────────────────────────────────────

if ! nc -z localhost "$PORT" 2>/dev/null; then
    echo "ERROR: javac daemon is not running on port $PORT." >&2
    echo "       Start it with: start-compile-server.sh $PORT" >&2
    exit 1
fi

# ── Send request, receive framed response ─────────────────────────────────────

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

# Send javac args line-by-line, then the NUL-wrapped ENDINP sentinel.
# Receive the full framed response into a temp file (binary-safe).
{ printf '%s\n' "$@"; printf '\x00ENDINP\x00\n'; } \
    | nc -w 30 localhost "$PORT" > "$WORK/response"

# Strip NUL bytes (they only appear in sentinel lines; javac never outputs NUL).
# Sentinel lines then become plain keywords: STDOUT, STDERR, EXTCOD.
# awk routes each section to the appropriate temp file.
tr -d '\000' < "$WORK/response" | awk '
    /^STDOUT$/ { mode="stdout"; next }
    /^STDERR$/ { mode="stderr"; next }
    /^EXTCOD$/ { mode="extcod"; next }
    mode == "stdout" { print > (wdir "/stdout") }
    mode == "stderr" { print > (wdir "/stderr") }
    mode == "extcod" { print > (wdir "/exitcode") }
' wdir="$WORK"

# Emit stdout to stdout and stderr to stderr, preserving javac's separation.
cat "$WORK/stdout"  2>/dev/null || true
cat "$WORK/stderr" >&2 2>/dev/null || true

EXIT_CODE=$(cat "$WORK/exitcode" 2>/dev/null | head -1 | tr -d '[:space:]')
exit "${EXIT_CODE:-1}"
