#!/usr/bin/env bash
#
# Common launcher for Python helper applications.
#
# Required:
#     PROGRAM=...
#
# Optional:
#     NODE_ENV=1
#     PYTHON_MODULES="yaml requests"

if [ -z "$PROGRAM" ]; then
    echo "PROGRAM is not set." >&2
    exit 1
fi

: "${PYTHON:=python3.12}"

DIR="$(dirname "$0")"

if [ -n "$NODE_ENV" ]; then
    . "$DIR/_exec_node_env.sh"
fi

if [ -n "$PYTHON_MODULES" ]; then
    for m in $PYTHON_MODULES; do
        if ! "$PYTHON" -c "import $m" >/dev/null 2>&1; then
            echo "Required Python module '$m' is not installed." >&2
            echo >&2
            echo "Install it with:" >&2
            echo "    \"$PYTHON\" -m pip install --user \"$m\"" >&2
            exit 1
        fi
    done
fi

exec "$PYTHON" "$DIR/${PROGRAM}.py" "$@"
