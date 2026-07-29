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

Usage:
    python3 xml_content_diff.py <original.xml> <formatted.xml>

Exit code 0 if all five checks pass (content preserved), 1 otherwise, with a
description of every mismatch printed to stdout.

Used for real-code dogfood testing per STATE_DATA_FORMATS.md's "Dogfood
Output Validation" section -- written during the apache/maven XML dogfood
session (first XML dogfood run). Reusable as-is for the other three XML
test-fixture repos (apache/ant, jenkinsci/jenkins, w3c/svgwg) still pending.
"""
import re
import sys
from xml.dom.minidom import parse
from xml.parsers.expat import ExpatError

WS_RE = re.compile(r"\s+")


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
        if a_attrs != b_attrs:
            errors.append(
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


def main():
    if len(sys.argv) != 3:
        print("usage: xml_content_diff.py <original.xml> <formatted.xml>")
        sys.exit(2)

    orig_path, fmt_path = sys.argv[1], sys.argv[2]

    try:
        orig_doc = parse(orig_path)
    except ExpatError as e:
        print(f"SKIP: original does not parse as XML ({e}) -- content-diff not applicable")
        sys.exit(2)
    try:
        fmt_doc = parse(fmt_path)
    except ExpatError as e:
        print(f"FORMATTED FILE FAILS TO PARSE: {e}")
        sys.exit(1)

    errors = []
    walk(orig_doc.documentElement, fmt_doc.documentElement, "root", errors)

    if errors:
        print(f"CONTENT MISMATCH ({len(errors)} issue(s)) between {orig_path} and {fmt_path}:")
        for e in errors: print(f"  - {e}")
        sys.exit(1)

    print(f"OK: content preserved between {orig_path} and {fmt_path}")
    sys.exit(0)


if __name__ == "__main__": main()
