#!/usr/bin/env bash
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
#
# bash_syntax_check.sh - syntax checker for Bash source files.
#
# Uses `bash -n` (parse-only, no execution) on each input file.
#
# Usage:
#     bash_syntax_check.sh <file.sh> [file2.sh ...]
#
# Exit 0 if all files parse successfully, 1 if one or more files contain
# syntax errors, 2 if the command-line usage is invalid.

if [ "$#" -lt 1 ]; then
    echo "Usage: bash_syntax_check.sh <file.sh> [file2.sh ...]" >&2
    exit 2
fi

status=0

for f in "$@"; do
    err=$(bash -n "$f" 2>&1 1>/dev/null)
    if [ -n "$err" ]; then
        status=1
        printf '%s\n' "$err" >&2
    fi
done

exit "$status"
