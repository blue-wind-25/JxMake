#!/usr/bin/env bash
# start-compile-server.sh
# Starts the javac daemon for a given JDK and port.
# Does nothing if a daemon for that port is already running.
#
# Usage:
#    start-compile-server.sh <port> [/path/to/java]
#    start-compile-server.sh 0      [/path/to/java]   # port derived from JDK version
#
# Examples:
#    start-compile-server.sh 21000
#    start-compile-server.sh 0 /usr/lib/jvm/java-21/bin/java
#    start-compile-server.sh 0                        # uses java from PATH
set -euo pipefail

# Editable: directory used for PID and log files (also passed to JVM as java.io.tmpdir)
TMPDIR_JVM=/tmp

# ── Includes ──────────────────────────────────────────────────────────────────

_JCS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$_JCS_DIR/_port.sh"

# ── Argument parsing ──────────────────────────────────────────────────────────

PORT="${1:-}"
JAVA_BIN="${2:-java}"

if [[ -z "$PORT" ]]; then
    echo "Usage: $0 <port> [/path/to/java]" >&2
    exit 1
fi

# ── Validate java binary ──────────────────────────────────────────────────────

if ! command -v "$JAVA_BIN" >/dev/null 2>&1; then
    echo "ERROR: java binary not found or not executable: $JAVA_BIN" >&2
    exit 1
fi

JAVAC_BIN="$(dirname -- "$(command -v "$JAVA_BIN")")/javac"
if [[ "$(basename -- "$JAVA_BIN")" == "javac" ]] || [[ ! -x "$JAVAC_BIN" ]]; then
    echo "ERROR: $JAVA_BIN does not appear to come from a JDK (no javac found alongside it). A JRE-only install cannot run the compile server." >&2
    exit 1
fi

# ── Port resolution ───────────────────────────────────────────────────────────

JAVA_VER=$("$JAVA_BIN" -version 2>&1 | head -1)

if [[ "$PORT" == "0" ]]; then
    PORT=$(derive_port "$JAVA_BIN")
    echo "Auto-derived port $PORT from Java version $JAVA_VER."
fi

# ── Main ──────────────────────────────────────────────────────────────────────

SCRIPT_DIR="$_JCS_DIR"
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
    fi
    # If a socket connection works here, the process is active even if the PID is dead/stale
    if (echo > /dev/tcp/localhost/"$PORT") 2>/dev/null; then
        echo "Daemon already running on port $PORT (recovered from stale PID reference)"
        exit 0
    fi
    echo "Stale PID file found, removing."
    rm -f "$PID_FILE"
fi

if (echo > /dev/tcp/localhost/"$PORT") 2>/dev/null; then
    echo "Something else is already listening on port $PORT (not our daemon?)" >&2
    exit 1
fi

echo "Starting javac daemon on port $PORT using $JAVA_VER..."

nohup "$JAVA_BIN" -Djava.io.tmpdir="$TMPDIR_JVM" -jar "$JAR" "$PORT" \
    > "$LOG_FILE" 2>&1 &
JAVA_PID=$!

for i in {1..10}; do
    sleep 0.5
    if (echo > /dev/tcp/localhost/"$PORT") 2>/dev/null; then
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
