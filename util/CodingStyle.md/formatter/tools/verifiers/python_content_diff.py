#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.

"""
python_content_diff.py - content-preservation checker for Python, modeled on
toml_content_diff.py / xml_content_diff.py / yaml_content_diff.py.

Unlike those, Python has a real parser in its own stdlib (`ast`), so this
script uses it directly rather than shelling out to a Node.js helper.

Parses BOTH the original and formatted file with `ast.parse`, then compares
`ast.dump(tree, include_attributes=False)` (position/line/col attributes
stripped, since formatting legitimately changes those) for structural
equality. If formatting changed only whitespace/style, the two dumps must be
byte-identical -- any difference means a statement's nesting, a token's
value, or a node's shape changed, i.e. real corruption (most critically:
an indentation-based scoping change, since Python's indentation IS its
scoping, per STATE_PYTHON3.md).

Usage:
    python3 python_content_diff.py <original.py> <formatted.py>

Exit 0 if the two ASTs are structurally identical (content preserved), 1 if
they differ (prints a highlighted description of the first mismatch), 2 if
either file fails to parse as Python at all.
"""
import ast
import sys


def parse_file(path):
    with open(path, "r", encoding="utf-8") as f:
        source = f.read()
    try:
        return ast.parse(source, filename=path), None
    except SyntaxError as e:
        return None, str(e)


def dump(tree):
    # include_attributes=False strips lineno/col_offset/end_lineno/end_col_offset --
    # exactly the fields formatting is expected to change; every other field
    # (node type, field values, child ordering/nesting) must match exactly.
    return ast.dump(tree, include_attributes=False, indent=2)


def main():
    if len(sys.argv) != 3:
        sys.stderr.write("usage: python_content_diff.py <original.py> <formatted.py>\n")
        sys.exit(2)

    orig_path, fmt_path = sys.argv[1], sys.argv[2]

    orig_tree, orig_err = parse_file(orig_path)
    if orig_err is not None:
        sys.stderr.write("ERROR: original file failed to parse as Python: %s: %s\n" % (orig_path, orig_err))
        sys.exit(2)

    fmt_tree, fmt_err = parse_file(fmt_path)
    if fmt_err is not None:
        sys.stderr.write("ERROR: formatted file failed to parse as Python: %s: %s\n" % (fmt_path, fmt_err))
        sys.exit(2)

    orig_dump = dump(orig_tree)
    fmt_dump  = dump(fmt_tree)

    if orig_dump == fmt_dump:
        print("OK: AST structurally identical (%s == %s)" % (orig_path, fmt_path))
        sys.exit(0)
    else:
        sys.stderr.write("MISMATCH: AST structure differs between %s and %s\n" % (orig_path, fmt_path))
        orig_lines = orig_dump.splitlines()
        fmt_lines  = fmt_dump.splitlines()
        for i, (a, b) in enumerate(zip(orig_lines, fmt_lines)):
            if a != b:
                sys.stderr.write("first differing line (%d):\n" % (i + 1))
                sys.stderr.write("  original : %s\n" % a)
                sys.stderr.write("  formatted: %s\n" % b)
                break

        else:
            sys.stderr.write("(one AST dump is a strict prefix of the other -- "
                              "orig %d lines, formatted %d lines)\n" % (len(orig_lines), len(fmt_lines)))
        sys.exit(1)


if __name__ == "__main__": main()
