#!/usr/bin/env bash
# start-compile-server.sh
# Starts the javac daemon for a given JDK and port.
# Does nothing if a daemon for that port is already running.
#
# Usage:
#   start-compile-server.sh <port> [/path/to/java]
#
# Examples:
#   start-compile-server.sh 62650
#   start-compile-server.sh 62651 /usr/lib/jvm/java-11/bin/java
set -euo pipefail

PORT="${1:-}"
JAVA_BIN="${2:-java}"

if [[ -z "$PORT" ]]; then
    echo "Usage: $0 <port> [/path/to/java]" >&2
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/compile-server.jar"
PID_FILE="/tmp/javac-daemon-${PORT}.pid"
LOG_FILE="/tmp/javac-daemon-${PORT}.log"

if [[ ! -f "$JAR" ]]; then
    echo "ERROR: $JAR not found. Run build-server.sh first." >&2
    exit 1
fi

# Check if already running via PID file
if [[ -f "$PID_FILE" ]]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        echo "Daemon already running on port $PORT (PID $PID)"
        exit 0
    else
        echo "Stale PID file found, removing."
        rm -f "$PID_FILE"
    fi
fi

# Also check via nc in case PID file is missing
if nc -z localhost "$PORT" 2>/dev/null; then
    echo "Something is already listening on port $PORT (not our daemon?)" >&2
    exit 1
fi

echo "Starting javac daemon on port $PORT using $("$JAVA_BIN" -version 2>&1 | head -1)..."

nohup "$JAVA_BIN" -jar "$JAR" "$PORT" > "$LOG_FILE" 2>&1 &

# Wait up to 5 seconds for it to become ready
for i in $(seq 1 10); do
    sleep 0.5
    if nc -z localhost "$PORT" 2>/dev/null; then
        PID=$(cat "$PID_FILE" 2>/dev/null || echo "?")
        echo "Daemon started (PID $PID), listening on port $PORT"
        echo "Log: $LOG_FILE"
        exit 0
    fi
done

echo "ERROR: Daemon did not start within 5 seconds. Check $LOG_FILE" >&2
exit 1
