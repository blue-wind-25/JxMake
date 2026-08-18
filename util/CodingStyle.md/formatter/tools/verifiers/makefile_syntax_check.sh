#!/usr/bin/env bash
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is distributed under the Apache License, Version 2.0.
# See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
#
# makefile_syntax_check.sh - syntax checker for Makefile source files.
#
# Uses `make -n` (dry run, no recipes executed) against a throwaway wrapper
# Makefile that `include`s each input file and defines its own no-op target,
# so a syntax check never fails merely because the input file itself defines
# no targets (a normal case for `.mk` fragments, unlike top-level `Makefile`s).
#
# Usage:
#     makefile_syntax_check.sh <Makefile> [file2.mk ...]
#
# Exit 0 if all files parse successfully, 1 if one or more files contain
# syntax errors, 2 if the command-line usage is invalid.

if [ "$#" -lt 1 ]; then
    echo "Usage: makefile_syntax_check.sh <Makefile> [file2.mk ...]" >&2
    exit 2
fi

status=0
wrapper="$(mktemp)"
trap 'rm -f "$wrapper"' EXIT

printf 'include $(JXMAKE_MKCHK_FILE)\n__jxmake_noop_target__:\n\t@:\n' > "$wrapper"

for f in "$@"; do
    err=$(JXMAKE_MKCHK_FILE="$f" make -n -f "$wrapper" __jxmake_noop_target__ 2>&1 1>/dev/null)
    if [ -n "$err" ]; then
        status=1
        printf '%s\n' "$err" >&2
    fi
done

exit "$status"
