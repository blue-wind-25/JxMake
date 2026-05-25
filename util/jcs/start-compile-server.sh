#!/usr/bin/env bash
# start-compile-server.sh
# Starts the javac daemon for a given JDK and port.
# Does nothing if a daemon for that port is already running.
#
# Usage:
#   start-compile-server.sh <port> [/path/to/java]
#   start-compile-server.sh 0      [/path/to/java]   # port derived from JDK version
#
# Examples:
#   start-compile-server.sh 21000
#   start-compile-server.sh 0 /usr/lib/jvm/java-21/bin/java
#   start-compile-server.sh 0                         # uses java from PATH
set -euo pipefail

# Editable: directory used for PID and log files (also passed to JVM as java.io.tmpdir)
TMPDIR_JVM=/tmp

PORT="${1:-}"
JAVA_BIN="${2:-java}"

if [[ -z "$PORT" ]]; then
    echo "Usage: $0 <port> [/path/to/java]" >&2
    exit 1
fi

# ── Port resolution ───────────────────────────────────────────────────────────

if [[ "$PORT" == "0" ]]; then
    VER_STR=$("$JAVA_BIN" -version 2>&1 | head -1)
    if [[ "$VER_STR" =~ \"1\.([0-9]+) ]]; then
        JAVA_MAJOR="${BASH_REMATCH[1]}"
    elif [[ "$VER_STR" =~ \"([0-9]+) ]]; then
        JAVA_MAJOR="${BASH_REMATCH[1]}"
    else
        echo "ERROR: Cannot parse Java version from: $VER_STR" >&2
        exit 1
    fi
    PORT=$((JAVA_MAJOR * 1000))
    echo "Auto-derived port $PORT from Java major version $JAVA_MAJOR."
fi

# ── Main ──────────────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/compile-server.jar"
PID_FILE="${TMPDIR_JVM}/javac-daemon-${PORT}.pid"
LOG_FILE="${TMPDIR_JVM}/javac-daemon-${PORT}.log"

if [[ ! -f "$JAR" ]]; then
    echo "ERROR: $JAR not found. Run build-server.sh first." >&2
    exit 1
fi

if [[ -f "$PID_FILE" ]]; then
    PID=$(cat "$PID_FILE")
    if [[ "$PID" =~ ^[0-9]+$ ]] && kill -0 "$PID" 2>/dev/null; then
        echo "Daemon already running on port $PORT (PID $PID)"
        exit 0
    else
        echo "Stale PID file found, removing."
        rm -f "$PID_FILE"
    fi
fi

if nc -z localhost "$PORT" 2>/dev/null; then
    echo "Something is already listening on port $PORT (not our daemon?)" >&2
    exit 1
fi

echo "Starting javac daemon on port $PORT using $("$JAVA_BIN" -version 2>&1 | head -1)..."

nohup "$JAVA_BIN" -Djava.io.tmpdir="$TMPDIR_JVM" -jar "$JAR" "$PORT" \
    > "$LOG_FILE" 2>&1 &
JAVA_PID=$!

for i in $(seq 1 10); do
    sleep 0.5
    if nc -z localhost "$PORT" 2>/dev/null; then
        PID=$(cat "$PID_FILE" 2>/dev/null || echo "-1")
        if [[ "$PID" == "-1" ]] || ! [[ "$PID" =~ ^[0-9]+$ ]]; then
            echo "$JAVA_PID" > "$PID_FILE"
            PID="$JAVA_PID"
        fi
        echo "Daemon started (PID $PID), listening on port $PORT"
        echo "Log: $LOG_FILE"
        exit 0
    fi
done

echo "ERROR: Daemon did not start within 5 seconds. Check $LOG_FILE" >&2
exit 1
