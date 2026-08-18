#!/usr/bin/env python3
#
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is distributed under the Apache License, Version 2.0.
# See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
#

import argparse
import difflib
import re

from pathlib import Path


JAVADOC_LINE_RE = re.compile(r"^([ \t]*)\*(.*?)(\r?\n)?$")


def parse_javadoc_line(line):
    match = JAVADOC_LINE_RE.match(line)
    if not match:
        return None

    prefix  = match.group(1)
    content = match.group(2)
    newline = match.group(3) or ""

    return prefix, content, newline


def normalize_paragraph(lines):
    """
    Normalize one Javadoc paragraph if it has exactly this form:

        * First line
        *  continuation
        *  continuation

    Do not touch paragraphs containing deeper indentation such as:

        *     - item
        *       continuation
    """
    if len(lines) < 2:
        return lines

    parsed = [parse_javadoc_line(line) for line in lines]

    if any(item is None for item in parsed):
        return lines

    # First line must have exactly one space after '*'
    first_content = parsed[0][1]
    if not first_content.startswith(" ") or first_content.startswith("  "):
        return lines

    # Every continuation line must have exactly two spaces after '*'
    for _, content, _ in parsed[1:]:
        if not content.startswith("  ") or content.startswith("   "):
            return lines

    # Remove exactly one of those two spaces
    result = [lines[0]]

    for prefix, content, newline in parsed[1:]:
        result.append(prefix + "*" + content[1:] + newline)

    return result


def normalize_javadoc(text):
    lines  = text.splitlines(keepends=True)
    result = []

    in_javadoc = False
    paragraph  = []

    def flush():
        nonlocal paragraph

        if paragraph:
            result.extend(normalize_paragraph(paragraph))
            paragraph = []

    for line in lines:
        if not in_javadoc:
            result.append(line)

            start = line.find("/**")
            if start != -1:
                # Single-line Javadoc: /** ... */
                if "*/" not in line[start + 3:]:
                    in_javadoc = True

            continue

        parsed = parse_javadoc_line(line)

        # Something unexpected inside the Javadoc block
        if parsed is None:
            flush()
            result.append(line)

            if "*/" in line:
                in_javadoc = False

            continue

        _, content, _ = parsed

        # Closing Javadoc line
        if content.strip() == "/":
            flush()
            result.append(line)
            in_javadoc = False
            continue

        # Blank Javadoc line separates paragraphs
        if content.strip() == "":
            flush()
            result.append(line)
            continue

        paragraph.append(line)

    flush()

    return "".join(result)


def process_file(path, show_diff):
    original   = path.read_text(encoding="utf-8")
    normalized = normalize_javadoc(original)

    if original == normalized:
        return False

    if show_diff:
        diff = difflib.unified_diff(
            original.splitlines(keepends=True),
            normalized.splitlines(keepends=True),
            fromfile=str(path),
            tofile=str(path),
        )
        print("".join(diff))
    else:
        path.write_text(normalized, encoding="utf-8")

    return True


def main():
    parser = argparse.ArgumentParser(
        description=(
            "Normalize Claude-style extra spaces in JavaDoc "
            "continuation lines."
        )
    )

    parser.add_argument(
        "directory",
        type=Path,
        nargs="?",
        help="directory containing Java source files",
    )

    parser.add_argument(
        "--diff",
        action="store_true",
        help="show changes without modifying files",
    )

    args = parser.parse_args()

    if args.directory is None:
        parser.print_help()
        return 0

    if not args.directory.is_dir():
        parser.error(f"not a directory: {args.directory}")

    count = 0

    for path in args.directory.rglob("*.java"):
        if process_file(path, args.diff):
            count += 1

    if args.diff: print(f"{count} Java file(s) would change.")
    else:
        print(f"{count} Java file(s) changed.")


if __name__ == "__main__": raise SystemExit(main())
