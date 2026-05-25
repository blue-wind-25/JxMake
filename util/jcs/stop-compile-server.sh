#!/usr/bin/env bash
# stop-compile-server.sh
# Stops a running javac daemon on the given port.
#
# Usage:
#   stop-compile-server.sh <port>
#   stop-compile-server.sh         (stops all running javac daemons)
set -euo pipefail

# Editable: directory where PID files are stored (must match start-compile-server.sh)
TMPDIR_JVM=/tmp

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
        # Wait up to 3 seconds for clean exit
        for i in $(seq 1 6); do
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

if [[ $# -eq 0 ]]; then
    # Stop all
    FOUND=0
    for f in "${TMPDIR_JVM}/javac-daemon-"*.pid; do
        [[ -f "$f" ]] || continue
        PORT="${f#${TMPDIR_JVM}/javac-daemon-}"
        PORT="${PORT%.pid}"
        stop_one "$PORT" && FOUND=$((FOUND + 1))
    done
    if [[ $FOUND -eq 0 ]]; then
        echo "No running javac daemons found."
    fi
else
    stop_one "$1"
fi
