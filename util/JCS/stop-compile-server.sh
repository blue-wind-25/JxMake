#!/usr/bin/env bash
# stop-compile-server.sh
# Stops a running javac daemon on the given port.
#
# Usage:
#    stop-compile-server.sh <port>             # stop daemon on this port
#    stop-compile-server.sh 0 [/path/to/java]  # port derived from JDK version
#    stop-compile-server.sh                    # stop ALL running javac daemons
set -euo pipefail

# Editable: directory where PID files are stored (must match start-compile-server.sh)
TMPDIR_JVM=/tmp

# ── Port resolution ───────────────────────────────────────────────────────────

resolve_port_from_java() {
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

# ── Stop one daemon ───────────────────────────────────────────────────────────

stop_one() {
    local PORT="$1"
    local PID_FILE="${TMPDIR_JVM}/javac-daemon-${PORT}.pid"

    if [[ ! -f "$PID_FILE" ]]; then
        echo "No PID file found for port $PORT ($PID_FILE). Daemon may not be running."
        return 1
    fi

    local PID
    PID=$(cat "$PID_FILE")

    if ! [[ "$PID" =~ ^[0-9]+$ ]]; then
        echo "Invalid PID '$PID' in $PID_FILE for port $PORT. Removing stale PID file."
        rm -f "$PID_FILE"
        return 1
    fi

    if kill -0 "$PID" 2>/dev/null; then
        echo "Stopping javac daemon on port $PORT (PID $PID)..."
        kill "$PID"
        for i in {1..6}; do
            sleep 0.5
            kill -0 "$PID" 2>/dev/null || { echo "Stopped."; return 0; }
        done
        echo "Daemon did not exit cleanly, sending SIGKILL..."
        kill -9 "$PID" 2>/dev/null || true
        rm -f "$PID_FILE"
        echo "Killed."
    else
        echo "Daemon for port $PORT (PID $PID) is not running. Cleaning up stale PID file."
        rm -f "$PID_FILE"
    fi
}

# ── Main ──────────────────────────────────────────────────────────────────────

if [[ $# -eq 0 ]]; then
    FOUND=0

    # Enable nullglob so empty wildcard arrays collapse cleanly
    shopt -s nullglob
    PID_FILES=("${TMPDIR_JVM}/javac-daemon-"*.pid)
    shopt -u nullglob

    # Guard against set -u by checking if the array size is greater than 0
    if [[ ${#PID_FILES[@]} -gt 0 ]]; then
        for f in "${PID_FILES[@]}"; do
            PORT="${f#${TMPDIR_JVM}/javac-daemon-}"
            PORT="${PORT%.pid}"
            stop_one "$PORT" && FOUND=$((FOUND + 1)) || true
        done
    fi

    if [[ $FOUND -eq 0 ]]; then
        echo "No running javac daemons found."
    fi
elif [[ "$1" == "0" ]]; then
    JAVA_BIN="${2:-java}"
    PORT=$(resolve_port_from_java "$JAVA_BIN")
    stop_one "$PORT" || true
else
    stop_one "$1" || true
fi
