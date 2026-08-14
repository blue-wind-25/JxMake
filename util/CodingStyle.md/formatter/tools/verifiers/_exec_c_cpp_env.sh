#!/usr/bin/env bash
#
# Compiler-location env vars shared by all c*/cpp*_syntax_check.sh and
# c*/cpp*_content_diff.sh scripts. Sourced (not executed) by _exec_c_cpp.sh.
#
# Override any of these in your shell/CI env if your sandbox has GCC/Clang
# installed elsewhere (e.g. the Claude Code web/Android remote sandbox does
# not have these exact paths -- only this dev PC does, verified 2026-08-15).
#

: "${GXX:=/opt/gcc-12.2.0/bin/g++}"
: "${CLANGXX:=$HOME/xsdk/clang22/LLVM-22.1.8-Linux-X64/bin/clang++}"
: "${USER_LD_LIBRARY_PATH:=/opt/isl-0.16.1/lib}"

# $GXX's cc1/cc1plus need libisl at runtime (fails with "libisl.so.15: cannot
# open shared object file" otherwise) -- must actually export, not just define.
export LD_LIBRARY_PATH="$USER_LD_LIBRARY_PATH:$LD_LIBRARY_PATH"
