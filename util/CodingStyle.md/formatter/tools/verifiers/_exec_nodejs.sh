#!/usr/bin/env bash
#
# Common launcher for NodeJS helper applications.
#
# Required:
#     PROGRAM=...
#
# Optional:
#     REQUIRE_MODULES="..."

if [ -z "$PROGRAM" ]; then
    echo "PROGRAM is not set." >&2
    exit 1
fi

DIR="$(dirname "$0")"

. "$DIR/_exec_node_env.sh"

if [ -n "$REQUIRE_MODULES" ]; then
    for m in $REQUIRE_MODULES; do
        if ! "$NODE" -e "require('$m')" >/dev/null 2>&1; then
            echo "Required npm package '$m' is not installed." >&2
            echo >&2
            echo "Install it with:" >&2
            echo "    LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH\" \"$NPM\" install --prefix \"$USER_NPM_HOME\" \"$m\"" >&2
            exit 1
        fi
    done
fi

exec "$NODE" "$DIR/${PROGRAM}.js" "$@"
