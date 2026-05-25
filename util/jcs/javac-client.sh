#!/usr/bin/env bash
# javac-client.sh
# Drop-in javac replacement for Makefiles that routes compilation
# through a running compile-server daemon.
#
# The daemon must already be running (use start-compile-server.sh).
# Does NOT auto-start the server — fails clearly if daemon is down.
#
# Usage in Makefile:
#   JAVAC = /path/to/javac-client.sh <port>
#   ...
#   $(JAVAC) -cp ... -d build/classes src/Foo.java
#
# Or from shell:
#   javac-client.sh 62650 -cp libs/*.jar -d out src/Main.java
set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "Usage: $0 <port> [javac args...]" >&2
    exit 1
fi

PORT="$1"
shift

if ! nc -z localhost "$PORT" 2>/dev/null; then
    echo "ERROR: javac daemon is not running on port $PORT." >&2
    echo "       Start it with: start-compile-server.sh $PORT" >&2
    exit 1
fi

# Send args line by line, terminated by END
RESPONSE=$(printf '%s\n' "$@" 'END' | nc localhost "$PORT")

# Print all output except the EXIT: line to stderr (matches javac behaviour)
echo "$RESPONSE" | grep -v '^EXIT:' >&2 || true

# Extract and return the exit code
EXIT_CODE=$(echo "$RESPONSE" | grep '^EXIT:' | tail -1 | cut -d: -f2)
exit "${EXIT_CODE:-1}"
