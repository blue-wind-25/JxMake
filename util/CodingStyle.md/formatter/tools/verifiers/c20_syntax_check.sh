#!/usr/bin/env bash
#
# NOTE on naming: this script's scope is C23, not "C20" -- named c20_* to
# match this project's STYLE_*.md naming convention for the analogous C++
# pair (STYLE_CPP20.md = C++20-until-C++23, STYLE_CPP26.md = C++26). There
# is no STYLE_C20.md/STYLE_C23.md file in the project root as of this
# writing -- only STYLE_C_CPP.md (legacy C/C++) exists for C. This filename
# was specified explicitly in the task description ("c20_syntax_check.sh
# (C23)"), so it is used as given rather than guessed; flagging the
# mismatch here for visibility rather than silently assuming a convention.
#
# GCC 12.2.0 has no literal "-std=c23" (max C mode is c17/c2x); Clang
# 22.1.8 does accept "-std=c23" literally. So Clang is the accurate C23
# check here; GCC's c2x is run too as a best-effort secondary (it is NOT
# truly C23 -- it's GCC's closest pre-C23 draft-standard mode).
#
PROGRAM=c20_syntax_check
EXT=c
MODE=SYNTAX
SC_RUNS=("CLANGXX:c23:c:clang++ -std=c23" "GXX:c2x:c:g++ -std=c2x (best-effort, not literal C23)")
. "$(dirname "$0")/_exec_c_cpp.sh" "$@"
