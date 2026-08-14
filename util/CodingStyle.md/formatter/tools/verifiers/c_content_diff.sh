#!/usr/bin/env bash
#
# Legacy C (until C17/18) content-preservation check via GCC's
# -fdump-tree-original AST-ish dump, compared textually. Matches the task
# description's gxx_dump example pattern, using -std=gnu99 to match
# c_syntax_check.sh's scope.
#
PROGRAM=c_content_diff
EXT=c
MODE=DIFF
CD_COMPILER_VAR=GXX
CD_DIFF_MODE=gxx_dump
CD_STD=gnu99
CD_LANG=c
. "$(dirname "$0")/_exec_c_cpp.sh" "$@"
