#!/usr/bin/env bash
#
# Legacy C (until C17/18) syntax check. GCC only -- matches the task
# description's literal example ($GXX -std=gnu99 -fsyntax-only). No
# STYLE_C23.md-equivalent exists for this scope; STYLE_C_CPP.md is the
# closest style-guide match (see c20_syntax_check.sh for the C23 scope).
#
PROGRAM=c_syntax_check
EXT=c
MODE=SYNTAX
SC_RUNS=("GXX:gnu99:c:g++ -std=gnu99")
. "$(dirname "$0")/_exec_c_cpp.sh" "$@"
