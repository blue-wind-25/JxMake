#!/usr/bin/env bash
# javac-client.sh
# Drop-in javac replacement for Makefiles that routes compilation
# through a running compile-server daemon.
#
# The daemon must already be running (use start-compile-server.sh).
# Does NOT auto-start the server -- fails clearly if daemon is down.
#
# Usage in Makefile:
#    JAVAC = /path/to/javac-client.sh <port>
#    $(JAVAC) -cp ... -d build/classes src/Foo.java
#
# Usage from shell:
#    javac-client.sh 62650 -cp libs/*.jar -d out src/Main.java
#    javac-client.sh 0     ...   # port auto-derived from active JDK
#
# Protocol: US-wrapped (\u001FTAG\u001F) sentinel lines separate sections.
# awk matches \u001FTAG\u001F directly; no tr stripping needed.
set -euo pipefail

# ── Includes ──────────────────────────────────────────────────────────────────

_JCS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$_JCS_DIR/_port.sh"
. "$_JCS_DIR/_javac_args.sh"

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

if ! (echo > /dev/tcp/localhost/"$PORT") 2>/dev/null; then
    echo "ERROR: javac daemon is not running on port $PORT." >&2
    echo "       Start it with: start-compile-server.sh $PORT" >&2
    exit 1
fi

# ── Absolutize path arguments relative to client's working directory ──────────
# The server daemon runs in a fixed CWD; convert all relative paths to absolute
# so the server can locate source files, output dirs, and classpath entries.

CLIENT_CWD="$(pwd)"

# ── Send request, receive framed response ─────────────────────────────────────

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

# Send absolutized javac args line-by-line, then the US-wrapped ENDINP sentinel.
# Receive the full framed response into a temp file (binary-safe).
{ process_javac_args "$@"; printf '\x1FENDINP\x1F\n'; } \
    | nc -w 30 localhost "$PORT" > "$WORK/response"

# awk matches the US-wrapped sentinel lines directly and routes each
# section to the appropriate temp file.
awk '
    # Match and consume sentinel lines (Unit Separator = \037)
    /^\037STDOUT\037$/ { mode="stdout"; next }
    /^\037STDERR\037$/ { mode="stderr"; next }
    /^\037EXTCOD\037$/ { mode="extcod"; next }

    # Route all subsequent lines until the next sentinel
    mode == "stdout" { print > (wdir "/stdout"); next }
    mode == "stderr" { print > (wdir "/stderr"); next }
    mode == "extcod" { print > (wdir "/exitcode"); next }
' wdir="$WORK" "$WORK/response"

# Emit stdout to stdout and stderr to stderr, preserving javac's separation.
cat "$WORK/stdout"  2>/dev/null || true
cat "$WORK/stderr" >&2 2>/dev/null || true

EXIT_CODE=$(cat "$WORK/exitcode" 2>/dev/null | head -1 | tr -d '[:space:]')
exit "${EXIT_CODE:-1}"
