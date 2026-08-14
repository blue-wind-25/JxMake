#!/usr/bin/env bash
#
# C++26 syntax check (STYLE_CPP26.md scope). GCC 12.2.0 has no literal
# "-std=c++26" (max C++ mode is c++20/c++2b); Clang 22.1.8 does accept
# "-std=c++26" literally. Clang is therefore the accurate C++26 check here;
# GCC's c++2b is run too as a best-effort secondary (it is NOT truly
# C++26 -- it's GCC's closest pre-C++26 draft-standard mode).
#
PROGRAM=cpp26_syntax_check
EXT=cpp
MODE=SYNTAX
SC_RUNS=("CLANGXX:c++26:c++:clang++ -std=c++26" "GXX:c++2b:c++:g++ -std=c++2b (best-effort, not literal C++26)")
. "$(dirname "$0")/_exec_c_cpp.sh" "$@"
