#!/usr/bin/env bash
#
# C++20-until-C++23 content-preservation check via GCC's
# -fdump-tree-original -std=c++20 -- matches the task description's example
# exactly.
#
PROGRAM=cpp20_content_diff
EXT=cpp
MODE=DIFF
CD_COMPILER_VAR=GXX
CD_DIFF_MODE=gxx_dump
CD_STD=c++20
CD_LANG=c++
. "$(dirname "$0")/_exec_c_cpp.sh" "$@"
