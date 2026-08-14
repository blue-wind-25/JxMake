#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.

"""
_exec_c_cpp.py - shared C/C++ content-preservation compare engine used by
c_content_diff.sh, c20_content_diff.sh, cpp_content_diff.sh,
cpp20_content_diff.sh, and cpp26_content_diff.sh (via _exec_c_cpp.sh).

Not meant to be invoked directly by a human -- always dispatched to by one
of those wrapper scripts, which fill in the compiler path, dump mode, and
-std/-x for their own language/standard scope.

Unlike Python (python_content_diff.py, real `ast` module) or Java
(java_content_diff.java, real parser), C/C++ has no in-process parser
available here, so this shells out to GCC/Clang per file and compares their
dump/AST output textually instead -- modeled on toml_content_diff.py's
"shell out, then compare" precedent rather than the AST-object-equality
precedent.

Modes (matching XL.txt's task description):
    gxx_dump   -- $GXX  -x <lang> -std=<std> -fdump-tree-original=- -fsyntax-only <file>
    clang_ast  -- $CLANGXX -x <lang> -std=<std> -Xclang -ast-dump -fsyntax-only <file>
    clang_json -- $CLANGXX -x <lang> -std=<std> -Xclang -ast-dump=json -fsyntax-only <file>

Compiler output is filtered exactly like the *_syntax_check.sh scripts
(drop the harmless libstdc++ "no version information available" warning,
strip generated hex addresses, strip "<...>" angle-bracket noise outside
#include lines, strip ANSI colour codes) before comparison.

The task description's content-diff examples pipe the gxx_dump/clang_ast
outputs through `diff -u`, and the clang_json output through
`git diff --no-index`. Both are unified diffs; this script uses Python's
own difflib.unified_diff for both, uniformly, to avoid a hard dependency on
a git binary/repo context for the json mode (git diff --no-index normally
works outside a repo too, but relying on the platform-provided diff library
instead of shelling out again is simpler and equally informative here).

Usage (two modes -- see also print_usage() below):
    Single pair:
        _exec_c_cpp.py <compiler> <mode> <std> <lang> <original> <formatted> [-- extra-opts...]
    Batch (one Python process invocation over a whole corpus -- rel-path
    list, one path per line, relative to both base dirs alike; STILL one
    compiler subprocess per file per pair, since GCC/Clang has no
    persistent-process mode here, but avoids re-spawning this Python driver
    itself per file):
        _exec_c_cpp.py <compiler> <mode> <std> <lang> --batch <orig_base_dir> <fmt_base_dir> <rel_path_file_list.txt> [-- extra-opts...]

Before each pair's compare, a "[yyyy-MM-dd HH:mm:ss.SSS] <relative path>"
line is printed, matching python_content_diff.py's/java_content_diff.java's
precedent. In batch mode, a rel-path missing from either base dir is a
warning, not a crash -- skipped, run continues. A compile failure for one
pair in batch mode is caught and counted as MISMATCH/ERROR rather than
aborting the whole batch. The batch loop below is a single sequential
for-loop, deliberately -- see the STATE_COMMON.md batch-mode convention and
the sibling investigation of python_content_diff.py's batch mode, which
found it to be sequential/single-process (no threading), not a
concurrency-bug candidate; this driver follows the same sequential shape on
purpose.

Exit 0 if all pairs identical, 1 if any differ or are missing (batch) /
differ (single), 2 if a file fails to compile in single-pair mode, or on a
usage error.
"""
import datetime
import difflib
import os
import re
import subprocess
import sys


class DumpError(Exception):
    pass


def timestamp_now():
    now = datetime.datetime.now()

    return now.strftime("%Y-%m-%d %H:%M:%S.") + "%03d" % (now.microsecond // 1000)


def print_timestamped_header(rel_path):
    print("[%s] %s" % (timestamp_now(), rel_path))


def filter_output(text):
    # 1) drop the harmless libstdc++ symbol-versioning warning line(s)
    lines = [l for l in text.splitlines() if "no version information available" not in l]

    out_lines = []
    for l in lines:
        # 2) strip generated hex addresses ("... 0x7f3a1b2c ...", including
        #    with no leading space, e.g. clang_json's `"id": "0x7f3a1b2c"`)
        l = re.sub(r"\s?0x[0-9a-fA-F]+", "", l)
        # 3) strip "<...>" angle-bracket noise, except on #include lines
        #    (where "<...>" is a real system-header name, not AST/template
        #    punctuation)
        if "#include" not in l:
            l = re.sub(r"<[^>]*>", "", l)
        out_lines.append(l)

    text = "\n".join(out_lines)
    # 4) strip ANSI colour escape sequences
    text = re.sub(r"\x1b\[[0-9;]*[mK]", "", text)

    return text


def dump_one(compiler, mode, std, lang, path, extra_opts):
    if mode == "gxx_dump":
        cmd = [compiler, "-x", lang, "-std=" + std, "-fdump-tree-original=-", "-fsyntax-only"]
    elif mode == "clang_ast":
        cmd = [compiler, "-x", lang, "-std=" + std, "-Xclang", "-ast-dump", "-fsyntax-only"]
    elif mode == "clang_json":
        cmd = [compiler, "-x", lang, "-std=" + std, "-Xclang", "-ast-dump=json", "-fsyntax-only"]
    else:
        raise ValueError("unknown mode: %s" % mode)

    cmd += list(extra_opts) + [path]

    proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    out = filter_output(proc.stdout.decode("utf-8", errors="replace"))

    if proc.returncode != 0:
        raise DumpError("compiler invocation failed (exit %d) for %s:\n%s" % (proc.returncode, path, out))

    return out


def compare_one(compiler, mode, std, lang, orig_path, fmt_path, orig_label, fmt_label, extra_opts):
    """The full single-pair dump-diff check, shared by both modes. Raises
    DumpError if either file fails to compile. Returns True if the filtered
    dumps are identical, False on a mismatch (mismatch details printed to
    stderr either way)."""
    try:
        orig_dump = dump_one(compiler, mode, std, lang, orig_path, extra_opts)
    except DumpError as e:
        raise DumpError("original file failed to compile: %s" % e)

    try:
        fmt_dump = dump_one(compiler, mode, std, lang, fmt_path, extra_opts)
    except DumpError as e:
        raise DumpError("formatted file failed to compile: %s" % e)

    if orig_dump == fmt_dump:
        print("OK: dump structurally identical (%s == %s)" % (orig_label, fmt_label))

        return True

    sys.stderr.write("MISMATCH: dump differs between %s and %s\n" % (orig_label, fmt_label))
    diff = difflib.unified_diff(
        orig_dump.splitlines(keepends=True),
        fmt_dump.splitlines(keepends=True),
        fromfile=orig_label,
        tofile=fmt_label,
    )
    sys.stderr.writelines(diff)

    return False


def print_usage():
    sys.stderr.write("Usage: _exec_c_cpp.py <compiler> <mode> <std> <lang> <original> <formatted> [-- extra-opts...]\n")
    sys.stderr.write("       _exec_c_cpp.py <compiler> <mode> <std> <lang> --batch <orig_base_dir> <fmt_base_dir> <rel_path_file_list.txt> [-- extra-opts...]\n")


def run_single(compiler, mode, std, lang, orig_arg, fmt_arg, extra_opts):
    print_timestamped_header(orig_arg)

    orig_exists, fmt_exists = os.path.exists(orig_arg), os.path.exists(fmt_arg)
    if not orig_exists or not fmt_exists:
        if not orig_exists and not fmt_exists:
            sys.stderr.write("WARNING: both %s and %s are missing\n" % (orig_arg, fmt_arg))
        elif not orig_exists: sys.stderr.write("WARNING: %s is missing\n" % orig_arg)
        else: sys.stderr.write("WARNING: %s is missing\n" % fmt_arg)
        sys.exit(1)

    try:
        sys.exit(0 if compare_one(compiler, mode, std, lang, orig_arg, fmt_arg, orig_arg, fmt_arg, extra_opts) else 1)
    except DumpError as e:
        sys.stderr.write("ERROR: %s\n" % e)
        sys.exit(2)


def run_batch(compiler, mode, std, lang, orig_base_dir, fmt_base_dir, file_list_path, extra_opts):
    with open(file_list_path, "r", encoding="utf-8") as f:
        rel_paths = [line.strip() for line in f if line.strip()]

    ok_count = mismatch_count = missing_count = 0

    # Deliberately a single sequential loop -- one compiler subprocess pair
    # per file, one after another, no threads/processes fanned out. See the
    # module docstring: this mirrors python_content_diff.py's own batch
    # loop shape, which was found (this session) to be sequential too, i.e.
    # not a concurrency-bug candidate.
    for rel in rel_paths:
        orig_path = os.path.join(orig_base_dir, rel)
        fmt_path = os.path.join(fmt_base_dir, rel)

        print_timestamped_header(rel)

        orig_exists, fmt_exists = os.path.exists(orig_path), os.path.exists(fmt_path)
        if not orig_exists and not fmt_exists:
            print("  WARNING: missing from both %s and %s -- skipping" % (orig_base_dir, fmt_base_dir))
            missing_count += 1
            continue
        if not orig_exists:
            print("  WARNING: missing from %s -- skipping" % orig_base_dir)
            missing_count += 1
            continue
        if not fmt_exists:
            print("  WARNING: missing from %s -- skipping" % fmt_base_dir)
            missing_count += 1
            continue

        try:
            if compare_one(compiler, mode, std, lang, orig_path, fmt_path, rel, rel, extra_opts): ok_count += 1
            else: mismatch_count += 1
        except Exception as e:
            print("  ERROR: %s" % e)
            mismatch_count += 1

    print("")
    print("SUMMARY: %d OK, %d MISMATCH/ERROR, %d MISSING (of %d files checked)" % (
        ok_count, mismatch_count, missing_count, ok_count + mismatch_count + missing_count))

    if mismatch_count > 0 or missing_count > 0: sys.exit(1)


def split_extra(args):
    if "--" in args:
        i = args.index("--")

        return args[:i], args[i + 1:]

    return args, []


def main():
    args, extra_opts = split_extra(sys.argv[1:])

    if len(args) < 4:
        print_usage()
        sys.exit(2)

    compiler, mode, std, lang = args[0], args[1], args[2], args[3]
    rest = args[4:]

    if len(rest) == 2:
        run_single(compiler, mode, std, lang, rest[0], rest[1], extra_opts)
    elif len(rest) == 4 and rest[0] == "--batch":
        run_batch(compiler, mode, std, lang, rest[1], rest[2], rest[3], extra_opts)
    else:
        print_usage()
        sys.exit(2)


if __name__ == "__main__": main()
