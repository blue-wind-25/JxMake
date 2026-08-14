#!/usr/bin/env bash
#
# C++20-until-C++23 syntax check (STYLE_CPP20.md scope). Both GCC and Clang
# accept "-std=c++20" literally, so both are run.
#
PROGRAM=cpp20_syntax_check
EXT=cpp
MODE=SYNTAX
SC_RUNS=("GXX:c++20:c++:g++ -std=c++20" "CLANGXX:c++20:c++:clang++ -std=c++20")
. "$(dirname "$0")/_exec_c_cpp.sh" "$@"
