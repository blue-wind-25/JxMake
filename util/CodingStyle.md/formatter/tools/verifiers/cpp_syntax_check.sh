#!/usr/bin/env bash
#
# Legacy C++ (until C++17) syntax check. GCC only, -std=gnu++17, mirroring
# c_syntax_check.sh's legacy-C choice (STYLE_C_CPP.md scope).
#
PROGRAM=cpp_syntax_check
EXT=cpp
MODE=SYNTAX
SC_RUNS=("GXX:gnu++17:c++:g++ -std=gnu++17")
. "$(dirname "$0")/_exec_c_cpp.sh" "$@"
