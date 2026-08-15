#!/usr/bin/env bash
#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

check() {
    if [ "$1" = "-x" ]; then
        for a in "$@"; do
            if [ "$a" = "$1" ]; then
                echo "match"
            fi
        done
    fi
}

case "$1" in
run)
    for f in "$@"; do
        if [ -f "$f" ]; then
            echo "$f"
        fi
    done
    ;;
esac
