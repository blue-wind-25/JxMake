#!/usr/bin/env bash
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is distributed under the Apache License, Version 2.0.
# See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
#
# jxmake_syntax_check.sh - syntax checker for JxMakeFile (.jxm) source files.
#
# Runs the real JxMake compiler in --__compile__ mode against each input
# file, which is the only way to validate JxMake syntax (there is no
# separate "parse-only" mode). That mode always writes a sibling *.bin
# file next to the source, as a side effect this checker must not leak:
#
#   - If a *.bin file already exists for a given input, it is backed up
#     before compiling and restored afterward (the pre-existing file is
#     the user's, not this script's, to touch).
#   - If no *.bin file existed beforehand, the one produced by the
#     compile is deleted afterward, so a syntax check leaves no trace.
#
# The jxmake binary is expected at a fixed path relative to this script,
# not an absolute path: "<this script's dir>/../../../../../dist_build/jxmake".
# If that dist_build directory (or the jxmake binary inside it) doesn't
# exist, this script exits with an error telling the user to build it
# first, rather than guessing at another location.
#
# Usage:
#     jxmake_syntax_check.sh <file.jxm> [file2.jxm ...]
#
# Exit 0 if all files compile successfully, 1 if one or more files
# contain syntax errors, 2 if the command-line usage is invalid or the
# dist_build/jxmake prerequisite is missing.

if [ "$#" -lt 1 ]; then
    echo "Usage: jxmake_syntax_check.sh <file.jxm> [file2.jxm ...]" >&2
    exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_BUILD="$SCRIPT_DIR/../../../../../dist_build"
JXMAKE_BIN="$DIST_BUILD/jxmake"

if [ ! -d "$DIST_BUILD" ] || [ ! -x "$JXMAKE_BIN" ]; then
    echo "ERROR: $JXMAKE_BIN not found." >&2
    echo "Build dist_build first (see the JxMake build instructions), then re-run this check." >&2
    exit 2
fi

# Pending "<real bin path>:<backup path>" entries for *.bin files that
# existed before we ran and must be restored, tracked so a trap on exit
# (including on Ctrl-C mid-run) still restores whatever was backed up so
# far, instead of leaving a stray .jxmake_syntax_check_backup file behind.
restores=()

restore_all() {
    local entry orig backup
    for entry in "${restores[@]}"; do
        [ -n "$entry" ] || continue
        orig="${entry%%:*}"
        backup="${entry#*:}"
        mv -f "$backup" "$orig"
    done
}
trap restore_all EXIT

status=0

for f in "$@"; do
    bin="${f}.bin"
    backup=""

    if [ -e "$bin" ]; then
        backup="${bin}.jxmake_syntax_check_backup"
        mv -f "$bin" "$backup"
        restores+=("$bin:$backup")
    fi

    err=$("$JXMAKE_BIN" --__compile__ -f "$f" 2>&1 1>/dev/null)
    rc=$?
    if [ "$rc" -ne 0 ] || [ -n "$err" ]; then
        status=1
        printf '%s\n' "$err" >&2
    fi

    if [ -n "$backup" ]; then
        # A *.bin existed before -- restore it now (also removes the
        # entry from `restores` so the exit trap doesn't redo the mv).
        mv -f "$backup" "$bin"
        restores=("${restores[@]/$bin:$backup/}")
    elif [ -e "$bin" ]; then
        # No *.bin existed before -- this run's own output, discard it.
        rm -f "$bin"
    fi
done

exit "$status"
