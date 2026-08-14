#!/usr/bin/env bash
#
# C23-scope content-preservation check (see c20_syntax_check.sh for the
# naming note). Uses Clang's -Xclang -ast-dump with -std=c23, since GCC
# 12.2.0 has no literal C23 mode to dump against.
#
PROGRAM=c20_content_diff
EXT=c
MODE=DIFF
CD_COMPILER_VAR=CLANGXX
CD_DIFF_MODE=clang_ast
CD_STD=c23
CD_LANG=c
. "$(dirname "$0")/_exec_c_cpp.sh" "$@"
