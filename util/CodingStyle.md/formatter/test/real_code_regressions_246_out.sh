#!/usr/bin/env bash
#
# Copyright (C) 2024 Example Corp.
# SPDX-License-Identifier: MIT
#

for ARG in "$@"; do
    case "$STATE" in
    pre)
        if [ "$ARG" = "-cp" ]; then STATE=cp; else A+=("$ARG"); fi
        ;;
    post)
        B+=("$ARG")
        ;;
    esac
done
