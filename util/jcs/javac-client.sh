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
#    javac-client.sh 0      ...             # port auto-derived from active JDK
#
# Protocol: US-wrapped (\x1FTAG\x1F) sentinel lines separate sections.
# awk matches \x1FTAG\x1F directly; no tr stripping needed.
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

if ! (echo > /dev/tcp/localhost/"$PORT") 2>/dev/null; then
    echo "ERROR: javac daemon is not running on port $PORT." >&2
    echo "       Start it with: start-compile-server.sh $PORT" >&2
    exit 1
fi

# ── Send request, receive framed response ─────────────────────────────────────

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

# Construct the payload
payload_func() {
    printf '%s\n' "$@"
    printf '\x1FENDINP\x1F\n'
}

# Determine correct netcat timeout flags based on the OS
NC_ARGS=("-w" "30")
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS Netcat requires -G for connection timeout
    NC_ARGS+=("-G" "1")
fi

# Send/Receive logic optimized for cross-platform compatibility
if command -v nc >/dev/null 2>&1; then
    # Pass dynamically assigned platform flags.
    # Standard input redirection (< /dev/null) is avoided here since payload_func
    # explicitly feeds stdin via the pipe and terminates with EOF.
    payload_func "$@" | nc "${NC_ARGS[@]}" localhost "$PORT" > "$WORK/response"
else
    # Fallback: Pure Bash network redirection if 'nc' isn't available at all
    exec 3<>/dev/tcp/localhost/"$PORT"
    payload_func "$@" >&3
    cat <&3 > "$WORK/response"
    exec 3>&-
fi

# awk matches the US-wrapped sentinel lines directly and routes each
# section to the appropriate temp file.
awk '
    /\x1FSTDOUT\x1F/ { mode="stdout"; next }
    /\x1FSTDERR\x1F/ { mode="stderr"; next }
    /\x1FEXTCOD\x1F/ { mode="extcod"; next }
    mode == "stdout" { print > (wdir "/stdout") }
    mode == "stderr" { print > (wdir "/stderr") }
    mode == "extcod" { print > (wdir "/exitcode") }
' wdir="$WORK" "$WORK/response"

# Emit stdout to stdout and stderr to stderr, preserving javac's separation.
cat "$WORK/stdout"  2>/dev/null || true
cat "$WORK/stderr" >&2 2>/dev/null || true

EXIT_CODE=$(cat "$WORK/exitcode" 2>/dev/null | head -1 | tr -d '[:space:]')
exit "${EXIT_CODE:-1}"
