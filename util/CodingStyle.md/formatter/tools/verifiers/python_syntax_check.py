#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is distributed under the Apache License, Version 2.0.
# See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.

"""
python_syntax_check.py - syntax checker for Python source files.

Uses Python's standard library (`ast`) to parse each input file. If
`ast.parse()` succeeds, the file is syntactically valid Python. No code is
executed and no bytecode (.pyc) files are generated.

Unlike `python -m py_compile`, this tool performs only parsing, making it
suitable for syntax verification during formatter or build-system validation.

Usage:
    python3 python_syntax_check.py <file.py> [file2.py ...]

Exit 0 if all files parse successfully, 1 if one or more files contain syntax
errors, 2 if the command-line usage is invalid.
"""
import ast
import sys


def check_file(path):
    with open(path, "r", encoding="utf-8") as f:
        source = f.read()

    try:
        ast.parse(source, filename=path)
        return None
    except SyntaxError as e:
        return e


def main():
    if len(sys.argv) < 2:
        sys.stderr.write("Usage: python_syntax_check.sh <file.py> [file2.py ...]\n")
        sys.exit(2)

    ok = True

    for path in sys.argv[1:]:
        err = check_file(path)
        if err is not None:
            ok = False
            sys.stderr.write("%s:%d:%d: %s\n" % (
                path,
                err.lineno or 0,
                err.offset or 0,
                err.msg))

    sys.exit(0 if ok else 1)


if __name__ == "__main__": main()
