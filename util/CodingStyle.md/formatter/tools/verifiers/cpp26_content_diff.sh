#!/usr/bin/env bash
#
# C++26 content-preservation check via Clang's -Xclang -ast-dump=json
# -std=c++26 -- matches the task description's example (the JSON AST-dump
# example, originally piped to `git diff --no-index`; _exec_c_cpp.py
# compares/diffs with Python's difflib instead -- see its module docstring
# for why). GCC has no literal C++26 mode to use here.
#
# KNOWN LIMITATION (verified this session): unlike Python's
# ast.dump(include_attributes=False), Clang's JSON AST dump has no
# "strip source-location attributes" mode -- "loc"/"range"/"file"/"line"/
# "col" fields are emitted unconditionally and legitimately change with
# pure whitespace/brace-placement reformatting (e.g. moving a `{` to its
# own line shifts every subsequent line number), and the absolute file
# path differs between the orig/fmt base dirs by construction. So this
# mode will report a MISMATCH for some purely-cosmetic reformatting that
# did not corrupt program structure -- it's a stronger (line/col-sensitive)
# check than the other content-diff scripts, not a false positive in the
# hex-address/hidden-noise sense. Use c_content_diff.sh's/cpp20_content_diff.sh's
# gxx_dump mode (or c20_content_diff.sh's clang_ast text mode, which is
# comparatively terser) if a location-insensitive check is wanted instead.
#
PROGRAM=cpp26_content_diff
EXT=cpp
MODE=DIFF
CD_COMPILER_VAR=CLANGXX
CD_DIFF_MODE=clang_json
CD_STD=c++26
CD_LANG=c++
. "$(dirname "$0")/_exec_c_cpp.sh" "$@"
