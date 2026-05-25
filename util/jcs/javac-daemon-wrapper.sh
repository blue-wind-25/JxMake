#!/usr/bin/env bash
# javac-daemon-wrapper.sh
#
# Transparent javac replacement that auto-starts the compile daemon
# for the current JDK on demand, then routes the compile request to it.
#
# Intended to be placed alongside (or symlinked from) the JDK bin dir,
# with the real javac renamed to javac.real in the same directory.
# Can also be used standalone via JAVAC= in your Makefile.
#
# Port is derived deterministically from the JDK bin path so each JDK
# version gets its own daemon automatically — no manual port management.
#
# Usage as Makefile JAVAC override (no JDK surgery needed):
#   JAVAC      = /path/to/javac-daemon-wrapper.sh
#   JAVA_HOME  = /usr/lib/jvm/java-21    # tells wrapper which JDK to use
#
# Usage as drop-in javac (rename real javac first):
#   mv $JAVA_HOME/bin/javac $JAVA_HOME/bin/javac.real
#   cp javac-daemon-wrapper.sh $JAVA_HOME/bin/javac
#   chmod +x $JAVA_HOME/bin/javac

set -euo pipefail

# ── Locate the JDK ──────────────────────────────────────────────────────────

# If we are installed inside a JDK bin dir, use that.
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [[ -x "$SCRIPT_DIR/java" ]]; then
    JDK_BIN="$SCRIPT_DIR"
elif [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    JDK_BIN="$JAVA_HOME/bin"
else
    # Fall back to whatever java is on PATH
    JAVA_PATH="$(command -v java 2>/dev/null || true)"
    if [[ -z "$JAVA_PATH" ]]; then
        echo "ERROR: Cannot locate JDK. Set JAVA_HOME or install a JDK." >&2
        exit 1
    fi
    JDK_BIN="$(dirname "$JAVA_PATH")"
fi

JAVA_BIN="$JDK_BIN/java"
JAVAC_REAL="$JDK_BIN/javac.real"   # real javac after rename
JAR_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$JAR_DIR/compile-server.jar"

# ── Derive a stable port from JDK bin path ──────────────────────────────────
# cksum gives a stable 32-bit hash; map into ephemeral port range 49152-65535
PORT=$(echo "$JDK_BIN" | cksum | awk '{print 49152 + ($1 % 16383)}')

# ── Ensure daemon is running ─────────────────────────────────────────────────

start_daemon() {
    if [[ ! -f "$JAR" ]]; then
        echo "ERROR: compile-server.jar not found at $JAR" >&2
        echo "       Run build-server.sh first." >&2
        return 1
    fi

    local PID_FILE="/tmp/javac-daemon-${PORT}.pid"

    # Already running?
    if [[ -f "$PID_FILE" ]]; then
        local PID
        PID=$(cat "$PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            return 0
        fi
        rm -f "$PID_FILE"
    fi

    if nc -z localhost "$PORT" 2>/dev/null; then
        return 0  # something is already listening
    fi

    local LOG="/tmp/javac-daemon-${PORT}.log"
    nohup "$JAVA_BIN" -jar "$JAR" "$PORT" > "$LOG" 2>&1 &

    # Wait up to 5 seconds
    for i in $(seq 1 10); do
        sleep 0.5
        nc -z localhost "$PORT" 2>/dev/null && return 0
    done

    echo "WARNING: javac daemon failed to start on port $PORT." >&2
    echo "         Check $LOG" >&2
    return 1
}

# ── Run compilation ──────────────────────────────────────────────────────────

run_via_daemon() {
    local RESPONSE
    RESPONSE=$(printf '%s\n' "$@" 'END' | nc localhost "$PORT")
    echo "$RESPONSE" | grep -v '^EXIT:' >&2 || true
    local EXIT_CODE
    EXIT_CODE=$(echo "$RESPONSE" | grep '^EXIT:' | tail -1 | cut -d: -f2)
    exit "${EXIT_CODE:-1}"
}

fallback_to_real() {
    if [[ -x "$JAVAC_REAL" ]]; then
        exec "$JAVAC_REAL" "$@"
    else
        echo "ERROR: Daemon unavailable and $JAVAC_REAL not found." >&2
        exit 1
    fi
}

# Try to ensure daemon is up; fall back to real javac if we can't
if start_daemon; then
    run_via_daemon "$@"
else
    echo "Falling back to real javac..." >&2
    fallback_to_real "$@"
fi
