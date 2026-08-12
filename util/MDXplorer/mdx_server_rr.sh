#!/usr/bin/env bash

DIR="$(dirname "$0")"

exec "$DIR/mdx_server.sh" "$@" -C "$DIR/../.."
