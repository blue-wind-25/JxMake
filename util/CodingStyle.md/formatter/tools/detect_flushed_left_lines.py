#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is distributed under the Apache License, Version 2.0.
# See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.

"""Scans curly-brace-family source files for an "accidentally flushed-left line"
formatter bug shape: a body line dedented all the way to column 0 immediately
after an indented `{`-opening line, e.g.

    void method() {
    doSomething();   // <- should be indented one level, not flush left
    }

This is the exact strict heuristic used during real-code dogfood hunts for this
bug class (see formatter/STATE_COMMON.md's "Formatter self-formatting
(dogfood-and-adopt) process" section, 2026-08-16 entry): the collapsed line
must sit right after an INDENTED `{`-opening line (an opener already flush at
column 0, e.g. a top-level `namespace X {` or `class Y {` in a language that
doesn't indent those, is not itself suspicious), and must not be a comment,
a lone closing brace, or a preprocessor directive -- those are legitimate
flush-left shapes, not the dedented-body-line bug. A deliberately narrow
heuristic on purpose: an earlier, broader "any dedented line after any `{`"
version produced dozens of false positives on ordinary wrapped-signature-
plus-body shapes (see the same STATE_COMMON.md entry).

This is a standalone static check over a file's own text -- it does not need
(and does not take) a before/after diff. Run it over a formatter's own output
after a self-formatting/dogfood pass, or over any source tree, to catch this
specific formatter-bug shape.

Usage:
    detect_flushed_left_lines.py <file-or-dir> [<file-or-dir> ...]

Exit status: 0 if no suspicious lines were found, 1 if any were found, 2 on
a usage error (no arguments).
"""

import sys
from pathlib import Path

# Extensions scanned when a directory is given. Explicit file arguments are
# always scanned regardless of extension.
DEFAULT_EXTENSIONS = (
    ".c", ".h", ".cpp", ".hpp", ".cc", ".cxx",
    ".java", ".kt",
    ".js", ".jsx", ".ts", ".tsx",
    ".cs", ".go", ".rs",
)

COMMENT_PREFIXES = ("//", "/*", "*", "#")


def is_comment_line(stripped):
    return stripped.startswith(COMMENT_PREFIXES)


def is_closing_brace_line(stripped):
    # A lone `}`, or `} else {`/`} // comment`/`});` etc. -- anything that
    # itself starts with `}` is a block-closer, not a suspicious body line.
    return stripped.startswith("}")


def find_flushed_left_lines(path):
    """Yields (line_number, opener_line, offending_line) for each hit in path."""

    try:
        text = path.read_text(encoding="utf-8", errors="replace")
    except OSError as e:
        print(f"detect_flushed_left_lines: skipping {path}: {e}", file=sys.stderr)
        return

    lines = text.splitlines()

    prev_nonblank_idx = None
    for i, raw_line in enumerate(lines):
        stripped = raw_line.strip()
        if not stripped:
            continue

        if prev_nonblank_idx is not None:
            opener = lines[prev_nonblank_idx]
            opener_stripped = opener.rstrip()
            opener_indent = len(opener) - len(opener.lstrip(" \t"))

            opener_is_indented_block_start = (
                opener_indent > 0 and opener_stripped.endswith("{")
            )

            current_indent = len(raw_line) - len(raw_line.lstrip(" \t"))
            current_is_flush_left = current_indent == 0

            if (
                opener_is_indented_block_start
                and current_is_flush_left
                and not is_comment_line(stripped)
                and not is_closing_brace_line(stripped)
            ):
                yield (i + 1, opener, raw_line)

        prev_nonblank_idx = i


def iter_target_files(args):
    for arg in args:
        p = Path(arg)
        if p.is_dir():
            for ext in DEFAULT_EXTENSIONS:
                yield from sorted(p.rglob(f"*{ext}"))
        elif p.is_file():
            yield p
        else:
            print(f"detect_flushed_left_lines: not a file or directory: {arg}", file=sys.stderr)


def main():
    if len(sys.argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2

    hit_count = 0
    file_count = 0

    for path in iter_target_files(sys.argv[1:]):
        file_count += 1
        for line_number, opener, offending in find_flushed_left_lines(path):
            hit_count += 1
            print(f"{path}:{line_number}:")
            print(f"    opener  : {opener.rstrip()}")
            print(f"    flushed : {offending.rstrip()}")

    if hit_count == 0:
        print(f"detect_flushed_left_lines: {file_count} file(s) scanned, no suspicious lines found")
        return 0

    print(f"\ndetect_flushed_left_lines: {file_count} file(s) scanned, {hit_count} suspicious line(s) found", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
