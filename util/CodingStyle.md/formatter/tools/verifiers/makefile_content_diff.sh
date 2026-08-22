#!/usr/bin/env bash
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is distributed under the Apache License, Version 2.0.
# See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
#
# makefile_content_diff.sh - content-preservation checker for Makefile
# dogfood testing.
#
# Make has no in-process parser available here (unlike Python's `ast` or
# Java's real parser), and unlike C/C++ there's also no compiler AST dump
# to shell out to. Instead, this uses `make`'s own `-p` flag, which prints
# its fully-expanded internal database (every variable's expanded value,
# every target's expanded prerequisites and recipe lines) for a dry run
# (`-n`, so no recipe is ever actually executed) with built-in implicit
# rules and variables suppressed (`-r -R`, so the dump reflects only what
# the input file itself defines, not GNU Make's stock rule set). Two such
# dumps -- one for the original file, one for the formatted file -- are
# compared textually, the same "shell out to a real tool, diff its
# structural output" precedent `_exec_c_cpp.py`'s gxx_dump/clang_ast modes
# and toml_content_diff.py use.
#
# Both dumps are produced from the same throwaway working directory
# (one per pair, holding the input under a fixed name, "Included.mk",
# included by a fixed no-op wrapper Makefile), so path-dependent database
# entries (CURDIR, MAKEFILE_LIST, etc.) come out byte-identical whether or
# not the file's actual content changed -- only the database's own header/
# footer timestamp lines and each file's "Last modified" mtime line vary
# run to run, and those are stripped before comparing.
#
# This proves the file's expanded variables, targets, prerequisites, and
# recipe lines are unchanged. Like the YAML/TOML content-diff scripts, it
# does NOT prove comment-only changes are inert by inspecting comments
# specifically -- make itself discards comments while building this
# database, so a comment-only formatting change simply produces no diff
# either way (the case this check exists for), and no separate
# comment-preservation side-channel is offered.
#
# Usage (two modes -- see also print_usage() below):
#     Single pair:
#         makefile_content_diff.sh <original.mk> <formatted.mk>
#     Batch (one invocation over a whole corpus -- rel-path list, one path
#     per line, relative to both base dirs alike):
#         makefile_content_diff.sh <original_base_dir> <formatted_base_dir> <mk_rel_path_file_list.txt>
#
# Before each pair's diff, a "[yyyy-MM-dd HH:mm:ss.SSS] <relative path>"
# line is printed, matching yaml_content_diff.py's/toml_content_diff.py's
# precedent. In batch mode, a rel-path missing from either base dir is a
# warning, not a crash -- the file is skipped and the run continues; the
# final SUMMARY line and process exit code still reflect it.
#
# Exit codes:
#     0 - every pair's database dump matched
#     1 - a dump mismatch was found (or, in batch mode, any pair is
#         missing/errors)
#     2 - (single-pair mode only) `make -p` itself failed for one of the
#         two files -- not expected for an already syntax-checked file;
#         also used for a usage error

print_usage() {
    echo "Usage: makefile_content_diff.sh <original.mk> <formatted.mk>" >&2
    echo "       makefile_content_diff.sh <original_base_dir> <formatted_base_dir> <mk_rel_path_file_list.txt>" >&2
}

timestamp_now() {
    date '+%Y-%m-%d %H:%M:%S.%3N'
}

# Prints the filtered `make -p` database dump for $1 (the file to check)
# to stdout. $2 is a scratch working directory reused across both sides of
# one pair's comparison, so path-dependent database entries line up.
_mk_dump() {
    local target="$1" workdir="$2" wrapper="$workdir/__jxmake_wrapper.mk"

    printf 'include ./Included.mk\n__jxmake_noop_target__:\n\t@:\n' > "$wrapper"
    cp "$target" "$workdir/Included.mk"

    ( cd "$workdir" && make -p -n -r -R -f "$wrapper" __jxmake_noop_target__ 2>&1 ) \
        | grep -v '^# Make data base, printed on\|^# Finished Make data base\|^#  Last modified '
}

# Compares one pair; prints MISMATCH/OK and a unified diff on mismatch.
# Returns 0 if the dumps matched, 1 on a mismatch, 2 if `make -p` itself
# failed for either side.
_mk_compare_one() {
    local orig="$1" fmt="$2" orig_label="$3" fmt_label="$4"
    local workdir dump_a dump_b rc=0

    workdir="$(mktemp -d)"

    dump_a="$workdir/dump_a.txt"
    dump_b="$workdir/dump_b.txt"

    _mk_dump "$orig" "$workdir" > "$dump_a"
    if [ ! -s "$dump_a" ]; then
        echo "ERROR: 'make -p' produced no output for original file: $orig_label" >&2
        rm -rf "$workdir"
        return 2
    fi

    _mk_dump "$fmt" "$workdir" > "$dump_b"
    if [ ! -s "$dump_b" ]; then
        echo "ERROR: 'make -p' produced no output for formatted file: $fmt_label" >&2
        rm -rf "$workdir"
        return 2
    fi

    if ! diff -q "$dump_a" "$dump_b" > /dev/null; then
        echo "MISMATCH: $orig_label vs $fmt_label"
        diff -u "$dump_a" "$dump_b" | sed 's/^/  /'
        rc=1
    else
        echo "OK: content preserved: $orig_label vs $fmt_label"
    fi

    rm -rf "$workdir"
    return "$rc"
}

_run_single() {
    local orig="$1" fmt="$2"

    echo "[$(timestamp_now)] $orig"

    if [ ! -e "$orig" ] || [ ! -e "$fmt" ]; then
        if [ ! -e "$orig" ] && [ ! -e "$fmt" ]; then
            echo "WARNING: both $orig and $fmt are missing" >&2
        elif [ ! -e "$orig" ]; then
            echo "WARNING: $orig is missing" >&2
        else
            echo "WARNING: $fmt is missing" >&2
        fi
        exit 1
    fi

    _mk_compare_one "$orig" "$fmt" "$orig" "$fmt"
    exit $?
}

_run_batch() {
    local orig_base="$1" fmt_base="$2" list="$3"
    local rel orig_path fmt_path
    local ok_count=0 mismatch_count=0 missing_count=0

    while IFS= read -r rel || [ -n "$rel" ]; do
        [ -n "$rel" ] || continue

        orig_path="$orig_base/$rel"
        fmt_path="$fmt_base/$rel"

        echo "[$(timestamp_now)] $rel"

        if [ ! -e "$orig_path" ] && [ ! -e "$fmt_path" ]; then
            echo "  WARNING: missing from both $orig_base and $fmt_base -- skipping"
            missing_count=$((missing_count + 1))
            continue
        fi
        if [ ! -e "$orig_path" ]; then
            echo "  WARNING: missing from $orig_base -- skipping"
            missing_count=$((missing_count + 1))
            continue
        fi
        if [ ! -e "$fmt_path" ]; then
            echo "  WARNING: missing from $fmt_base -- skipping"
            missing_count=$((missing_count + 1))
            continue
        fi

        if _mk_compare_one "$orig_path" "$fmt_path" "$rel" "$rel"; then
            ok_count=$((ok_count + 1))
        else
            mismatch_count=$((mismatch_count + 1))
        fi
    done < "$list"

    echo ""
    total=$((ok_count + mismatch_count + missing_count))
    echo "SUMMARY: $ok_count OK, $mismatch_count MISMATCH/ERROR, $missing_count MISSING (of $total files checked)"

    if [ "$mismatch_count" -gt 0 ] || [ "$missing_count" -gt 0 ]; then
        exit 1
    fi
    exit 0
}

case "$#" in
2) _run_single "$1" "$2" ;;
3) _run_batch "$1" "$2" "$3" ;;
*)
    print_usage
    exit 2
    ;;
esac
