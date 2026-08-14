#!/usr/bin/env bash
#
# Legacy C++ (until C++17) content-preservation check via GCC's
# -fdump-tree-original, -std=gnu++17.
#
PROGRAM=cpp_content_diff
EXT=cpp
MODE=DIFF
CD_COMPILER_VAR=GXX
CD_DIFF_MODE=gxx_dump
CD_STD=gnu++17
CD_LANG=c++
. "$(dirname "$0")/_exec_c_cpp.sh" "$@"
