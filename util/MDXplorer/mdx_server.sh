#!/usr/bin/env bash

: "${PYTHON:=python3.12}"

DIR="$(dirname "$0")"

exec "$PYTHON" "$DIR/mdx_server.py" "$@"
