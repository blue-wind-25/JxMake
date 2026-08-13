#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.

"""
xml_content_diff.py -- XML content-preservation checker.

Complements xml_syntax_check.js (@xmldom/xmldom-based syntax check: "does it still
parse?"). This script instead asks "did the formatter change any *meaning*?"
by comparing an original XML file against a formatted version of it, walking
both DOMs in parallel (stdlib xml.dom.minidom -- no extra package
dependency, unlike the Node-based *_syntax_check.js scripts) and comparing:

    1. Element names, in document order (tree shape/tag names unchanged).
    2. Attribute name+value pairs per element, IN ORDER (XML attribute order
       is spec-preserved per STYLE_DATA_FORMATS.md SS2.2 -- reordering, even if
       harmless to most consumers, is a real content-preservation bug here).
    3. Text-node content, whitespace-normalized (collapse runs of whitespace
       to a single space, strip leading/trailing) -- re-indentation/re-wrap is
       an expected style change, but the actual words/characters must survive.
    4. Comment text, whitespace-normalized same as text nodes -- this is the
       check that would have caught CSS's twbs/bootstrap rtlcss-directive
       comment-corruption bug (fixture real_code_regressions_69) had it been
       an XML/HTML bug instead: a corrupted comment is often still perfectly
       valid XML syntax, so a pure syntax check alone would not catch it.
    5. CDATA section content, byte-identical (no whitespace normalization --
       CDATA is defined to be opaque/verbatim).

Node-type mismatches (e.g. an element where the original had a comment) at
the same tree position are reported as a structural mismatch.

Usage (two modes -- see also print_usage() below):
    Single pair:
        python3 xml_content_diff.py <original.xml> <formatted.xml>
    Batch (one Python process invocation over a whole corpus, avoiding a
    process restart per file -- rel-path list, one path per line, relative
    to both base dirs alike):
        python3 xml_content_diff.py <original_base_dir> <formatted_base_dir> <xml_rel_path_file_list.txt>

Before each pair's diff, a "[yyyy-MM-dd HH:mm:ss.SSS] <relative path>" line is
printed -- lets a hang/slow file in a large batch run be pinpointed, matching
js_ts_content_diff.js's/java_content_diff.java's/kotlin_content_diff.java's
own precedent. In batch mode, a rel-path missing from either base dir (or
both) is a warning, not a crash -- the file is skipped and the run continues;
the final SUMMARY line and process exit code still reflect it. A parse
failure for one pair in batch mode is also caught and counted as a
MISMATCH/ERROR rather than aborting the whole batch.

Used for real-code dogfood testing per STATE_DATA_FORMATS.md's "Dogfood
Output Validation" section -- written during the apache/maven XML dogfood
session (first XML dogfood run). Reusable as-is for the other three XML
test-fixture repos (apache/ant, jenkinsci/jenkins, w3c/svgwg) still pending.

Exit code 0 if all five checks pass (content preserved, all pairs in batch
mode), 1 otherwise (or if any pair is missing/errors in batch mode), 2 if
either file fails to parse as XML at all in single-pair mode, or on a usage
error.
"""
import datetime
import os
import re
import sys
from xml.dom.minidom import parse
from xml.parsers.expat import ExpatError

WS_RE = re.compile(r"\s+")


class ParseError(Exception):
    pass


def norm_ws(s):
    return WS_RE.sub(" ", s).strip()


def significant_children(node):
    """Element/Text/Comment/CDATA children, skipping pure-whitespace text nodes."""
    out = []
    for child in node.childNodes:
        if child.nodeType == child.TEXT_NODE:
            if child.data.strip() == "": continue
            out.append(child)
        elif child.nodeType in (child.ELEMENT_NODE, child.COMMENT_NODE, child.CDATA_SECTION_NODE):
            out.append(child)

    return out


def describe(node):
    if node is None: return "<nothing>"
    if node.nodeType == node.ELEMENT_NODE: return f"<{node.tagName}>"
    if node.nodeType == node.TEXT_NODE: return f"text {node.data!r}"
    if node.nodeType == node.COMMENT_NODE: return f"comment {node.data!r}"
    if node.nodeType == node.CDATA_SECTION_NODE: return f"CDATA {node.data!r}"

    return f"node type {node.nodeType}"


def walk(a, b, path, errors):
    if a.nodeType != b.nodeType:
        errors.append(f"{path}: node type mismatch: {describe(a)} vs {describe(b)}")
        return

    if a.nodeType == a.ELEMENT_NODE:
        if a.tagName != b.tagName:
            errors.append(f"{path}: element name mismatch: {a.tagName!r} vs {b.tagName!r}")
        a_attrs = list(a.attributes.items()) if a.attributes else []
        b_attrs = list(b.attributes.items()) if b.attributes else []
        if a_attrs != b_attrs: errors.append(
                f"{path} <{a.tagName}>: attribute mismatch (order/name/value):\n"
                f"    ORIGINAL : {a_attrs}\n"
                f"    FORMATTED: {b_attrs}"
            )
        a_kids = significant_children(a)
        b_kids = significant_children(b)
        if len(a_kids) != len(b_kids):
            errors.append(
                f"{path} <{a.tagName}>: child count mismatch: "
                f"{len(a_kids)} vs {len(b_kids)}\n"
                f"    ORIGINAL : {[describe(k) for k in a_kids]}\n"
                f"    FORMATTED: {[describe(k) for k in b_kids]}"
            )
            return
        for i, (ak, bk) in enumerate(zip(a_kids, b_kids)):
            walk(ak, bk, f"{path}/<{a.tagName}>[{i}]", errors)

    elif a.nodeType == a.TEXT_NODE:
        na, nb = norm_ws(a.data), norm_ws(b.data)
        if na != nb:
            errors.append(f"{path}: text content mismatch:\n    ORIGINAL : {na!r}\n    FORMATTED: {nb!r}")

    elif a.nodeType == a.COMMENT_NODE:
        na, nb = norm_ws(a.data), norm_ws(b.data)
        if na != nb:
            errors.append(f"{path}: comment text mismatch:\n    ORIGINAL : {na!r}\n    FORMATTED: {nb!r}")

    elif a.nodeType == a.CDATA_SECTION_NODE:
        if a.data != b.data:
            errors.append(f"{path}: CDATA content mismatch (must be byte-identical):\n    ORIGINAL : {a.data!r}\n    FORMATTED: {b.data!r}")


def timestamp_now():
    now = datetime.datetime.now()

    return now.strftime("%Y-%m-%d %H:%M:%S.") + "%03d" % (now.microsecond // 1000)


def print_timestamped_header(rel_path):
    print("[%s] %s" % (timestamp_now(), rel_path))


def compare_one(orig_path, fmt_path, orig_label, fmt_label):
    """The full single-pair tree-walk check, shared by both modes. Raises
    ParseError if either file fails to parse. Returns True if content is
    preserved, False on a mismatch (mismatch details printed to stdout)."""
    try:
        orig_doc = parse(orig_path)
    except ExpatError as e:
        raise ParseError(f"original does not parse as XML ({e})")
    try:
        fmt_doc = parse(fmt_path)
    except ExpatError as e:
        raise ParseError(f"formatted file fails to parse as XML ({e})")

    errors = []
    walk(orig_doc.documentElement, fmt_doc.documentElement, "root", errors)

    if errors:
        print(f"CONTENT MISMATCH ({len(errors)} issue(s)) between {orig_label} and {fmt_label}:")
        for e in errors: print(f"  - {e}")

        return False

    print(f"OK: content preserved between {orig_label} and {fmt_label}")

    return True


def print_usage():
    sys.stderr.write("Usage: xml_content_diff.sh <original.xml> <formatted.xml>\n")
    sys.stderr.write("       xml_content_diff.sh <original_base_dir> <formatted_base_dir> <xml_rel_path_file_list.txt>\n")


def run_single(orig_arg, fmt_arg):
    print_timestamped_header(orig_arg)

    orig_exists, fmt_exists = os.path.exists(orig_arg), os.path.exists(fmt_arg)
    if not orig_exists or not fmt_exists:
        if not orig_exists and not fmt_exists: print(f"WARNING: both {orig_arg} and {fmt_arg} are missing")
        elif not orig_exists:                  print(f"WARNING: {orig_arg} is missing")
        else:                                  print(f"WARNING: {fmt_arg} is missing")
        sys.exit(1)

    try:
        sys.exit(0 if compare_one(orig_arg, fmt_arg, orig_arg, fmt_arg) else 1)
    except ParseError as e:
        print(f"SKIP: {e} -- content-diff not applicable")
        sys.exit(2)


def run_batch(orig_base_dir, fmt_base_dir, file_list_path):
    with open(file_list_path, "r", encoding="utf-8") as f:
        rel_paths = [line.strip() for line in f if line.strip()]

    ok_count = mismatch_count = missing_count = 0

    for rel in rel_paths:
        orig_path = os.path.join(orig_base_dir, rel)
        fmt_path  = os.path.join(fmt_base_dir, rel)

        print_timestamped_header(rel)

        orig_exists, fmt_exists = os.path.exists(orig_path), os.path.exists(fmt_path)
        if not orig_exists and not fmt_exists:
            print(f"  WARNING: missing from both {orig_base_dir} and {fmt_base_dir} -- skipping")
            missing_count += 1
            continue
        if not orig_exists:
            print(f"  WARNING: missing from {orig_base_dir} -- skipping")
            missing_count += 1
            continue
        if not fmt_exists:
            print(f"  WARNING: missing from {fmt_base_dir} -- skipping")
            missing_count += 1
            continue

        try:
            if compare_one(orig_path, fmt_path, rel, rel): ok_count += 1
            else:                                          mismatch_count += 1
        except Exception as e:
            print(f"  ERROR: {e}")
            mismatch_count += 1

    print("")
    print(f"SUMMARY: {ok_count} OK, {mismatch_count} MISMATCH/ERROR, {missing_count} MISSING "
          f"(of {ok_count + mismatch_count + missing_count} files checked)")

    if mismatch_count > 0 or missing_count > 0: sys.exit(1)


def main():
    args = sys.argv[1:]
    if len(args) == 2:   run_single(args[0], args[1])
    elif len(args) == 3: run_batch(args[0], args[1], args[2])
    else:
        print_usage()
        sys.exit(2)


if __name__ == "__main__": main()
