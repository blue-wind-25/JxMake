#!/usr/bin/env bash
# javac-daemon-wrapper.sh
#
# Transparent javac replacement that auto-starts the compile daemon
# for the current JDK on demand, then routes the compile request to it.
#
# Port is derived from the Java major version (major * 1000), so each
# JDK major version gets its own daemon automatically:
#    JDK 8 -> 8000, JDK 11 -> 11000, JDK 17 -> 17000, JDK 21 -> 21000.
#
# Installation as a Makefile JAVAC override (no JDK surgery needed):
#    JAVAC     = /path/to/javac-daemon-wrapper.sh
#    JAVA_HOME = /usr/lib/jvm/java-21
#
# Installation as a drop-in javac (rename the real javac first):
#    mv $JAVA_HOME/bin/javac $JAVA_HOME/bin/javac.real
#    cp javac-daemon-wrapper.sh $JAVA_HOME/bin/javac
#    chmod +x $JAVA_HOME/bin/javac
#    # compile-server.jar must be in the same directory
#    cp compile-server.jar $JAVA_HOME/bin/
#
# Protocol: US-wrapped (\u001FTAG\u001F) sentinel lines separate sections.
# awk matches \u001FTAG\u001F directly; no tr stripping needed.

set -euo pipefail

# Editable: directory used for PID and log files (also passed to JVM)
TMPDIR_JVM=/tmp

# ── Includes ──────────────────────────────────────────────────────────────────

_JCS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$_JCS_DIR/_port.sh"
. "$_JCS_DIR/_javac_args.sh"

# ── Locate the JDK ───────────────────────────────────────────────────────────

SCRIPT_DIR="$_JCS_DIR"
if [[ -x "$SCRIPT_DIR/java" ]]; then
    JDK_BIN="$SCRIPT_DIR"
elif [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    JDK_BIN="$JAVA_HOME/bin"
else
    JAVA_PATH="$(command -v java 2>/dev/null || true)"
    if [[ -z "$JAVA_PATH" ]]; then
        echo "ERROR: Cannot locate JDK. Set JAVA_HOME or install a JDK." >&2
        exit 1
    fi
    JDK_BIN="$(dirname "$JAVA_PATH")"
fi

JAVA_BIN="$JDK_BIN/java"
JAVAC_REAL="$JDK_BIN/javac.real"   # real javac after rename
JAR_DIR="$SCRIPT_DIR"
JAR="$JAR_DIR/compile-server.jar"

# ── Derive port from Java major version ──────────────────────────────────────

PORT=$(derive_port "$JAVA_BIN")

# ── Ensure daemon is running ─────────────────────────────────────────────────

start_daemon() {
    if [[ ! -f "$JAR" ]]; then
        echo "ERROR: compile-server.jar not found at $JAR" >&2
        echo "       Run build-server.sh first." >&2
        return 1
    fi

    local PID_FILE="${TMPDIR_JVM}/javac-daemon-${PORT}.pid"

    if [[ -f "$PID_FILE" ]]; then
        local PID
        PID=$(cat "$PID_FILE")
        if [[ "$PID" =~ ^[0-9]+$ ]] && kill -0 "$PID" 2>/dev/null; then
            return 0
        fi
        rm -f "$PID_FILE"
    fi

    if (echo > /dev/tcp/localhost/"$PORT") 2>/dev/null; then
        return 0
    fi

    local LOG="${TMPDIR_JVM}/javac-daemon-${PORT}.log"
    nohup "$JAVA_BIN" -Djava.io.tmpdir="$TMPDIR_JVM" -jar "$JAR" "$PORT" \
        > "$LOG" 2>&1 &
    local JAVA_PID=$!

    for i in {1..10}; do
        sleep 0.5
        if (echo > /dev/tcp/localhost/"$PORT") 2>/dev/null; then
            local PID
            PID=$(cat "$PID_FILE" 2>/dev/null || echo "-1")
            if [[ "$PID" == "-1" ]] || ! [[ "$PID" =~ ^[0-9]+$ ]]; then
                echo "$JAVA_PID" > "$PID_FILE"
            fi
            return 0
        fi
    done

    echo "WARNING: javac daemon failed to start on port $PORT." >&2
    echo "         Check ${TMPDIR_JVM}/javac-daemon-${PORT}.log" >&2
    return 1
}

# ── Absolutize path arguments relative to client's working directory ──────────
# The server daemon runs in a fixed CWD; convert all relative paths to absolute
# so the server can locate source files, output dirs, and classpath entries.

CLIENT_CWD="$(pwd)"

# ── Compilation helpers ───────────────────────────────────────────────────────

# WORK dir is created at script scope so the EXIT trap always cleans it up,
# even when run_via_daemon calls 'exit' directly.
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

run_via_daemon() {
    # Dynamically build the arguments for netcat based on the operating system
    local NC_ARGS=("-w" "30")
    if [[ "$OSTYPE" == "darwin"* ]]; then
        NC_ARGS+=("-G" "1") # Apply macOS-only connection timeout flag safely
    fi

    # Route content via native network descriptors or dynamic netcat flags
    if command -v nc >/dev/null 2>&1; then
        { process_javac_args "$@"; printf '\x1FENDINP\x1F\n'; } | nc "${NC_ARGS[@]}" localhost "$PORT" > "$WORK/response"
    else
        # Native safe fallback if netcat behaves erratically or isn't installed
        exec 3<>/dev/tcp/localhost/"$PORT"
        { process_javac_args "$@"; printf '\x1FENDINP\x1F\n'; } >&3
        cat <&3 > "$WORK/response"
        exec 3>&-
    fi

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

    cat "$WORK/stdout"  2>/dev/null || true
    cat "$WORK/stderr" >&2 2>/dev/null || true

    local EXIT_CODE
    read -r EXIT_CODE < "$WORK/exitcode" 2>/dev/null || true
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

# ── Main ─────────────────────────────────────────────────────────────────────

if start_daemon; then
    run_via_daemon "$@"
else
    echo "Falling back to real javac..." >&2
    fallback_to_real "$@"
fi
