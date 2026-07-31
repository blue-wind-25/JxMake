#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.

# This script shells out to Node.js (uses parse5 via an embedded helper), so
# run it with the following env vars set (Node.js + its packages are installed
# under these non-default locations, not the system paths):
#
#     export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
#     export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules:~/mynpm/node_modules
#     export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH

"""
html_content_diff.py -- content-preservation checker for HTML5, modeled on
xml_content_diff.py / css_content_diff.py / toml_content_diff.py.

Complements html_syntax_check.js (parse5-based syntax check: "does it still parse
per the HTML5 spec's error-tolerant grammar?"). This script instead asks
"did the formatter change any *meaning*?" by parsing both the original and
formatted HTML5 file with parse5 (via a small inline Node.js helper, since
this is Python-side tooling comparing output of a Node parser -- same
shell-out pattern as toml_content_diff.py uses for smol-toml) and walking
both trees in parallel, comparing:

    1. Element tag names and tree structure (tree walk, skipping pure-
       whitespace-only text nodes so re-indentation is never flagged).
    2. Attribute name+value pairs per element, IN ORDER.
    3. Whitespace-normalized text content (words/characters must survive;
       reflow/re-indentation is expected).
    4. Comment content, whitespace-normalized -- this is the check that
       would catch a CSS/XML-style comment-corruption bug in HTML too, since
       a corrupted comment usually stays syntactically valid.
    5. The DOCTYPE declaration (name/publicId/systemId).

IMPORTANTLY: the RAW, byte-identical text content of <script>/<style>
element bodies is NOT compared directly -- the formatter legitimately
dispatches that content to the JS/TS and CSS pipelines respectively, which
may reformat it (whitespace, quote style, etc.). Instead, for script/style
elements this script only checks that the element itself still exists at
the same tree position with the same attributes; the body's *meaning* is
presumed covered by html_syntax_check.js (which re-parses the whole file, including
tokenizing script/style raw text) and, for style bodies, by CSS's own
content-preservation guarantees (css_content_diff.py) when applicable.
Node-type mismatches at the same tree position are reported as a
structural mismatch.

Usage:
    python3 html_content_diff.py <original.html> <formatted.html>

Requires the same LD_LIBRARY_PATH/NODE_PATH/PATH env as html_syntax_check.js (parse5
must be installed via `npm install --prefix ~/mynpm parse5`).

Exit 0 if all checks pass (content preserved), 1 otherwise with a
description of every mismatch, 2 if either file fails to parse via parse5
at all (parse5 practically never hard-fails given HTML5's error-tolerant
grammar, but the case is handled defensively).
"""
import json
import os
import re
import subprocess
import sys

NODE_HELPER = r"""
const parse5 = require('parse5');
const fs = require('fs');
const path = process.argv[1];

function isRawTextElement(tagName) {
    return tagName === 'script' || tagName === 'style';
}

function simplify(node) {
    if (node.nodeName === '#text') {
        return { type: 'text', value: node.value };
    }
    if (node.nodeName === '#comment') {
        return { type: 'comment', value: node.data };
    }
    if (node.nodeName === '#documentType') {
        return {
            type: 'doctype',
            name: node.name || '',
            publicId: node.publicId || '',
            systemId: node.systemId || ''
        };
    }
    if (node.nodeName === '#document' || node.nodeName === '#document-fragment') {
        return {
            type: 'root',
            children: (node.childNodes || []).map(simplify)
        };
    }
    // Regular element.
    const attrs = (node.attrs || []).map(a => [a.name, a.value]);
    const tagName = node.tagName;
    const out = { type: 'element', tagName: tagName, attrs: attrs };
    if (isRawTextElement(tagName)) {
        // Script/style bodies are dispatched to another pipeline and may
        // legitimately be reformatted -- don't compare their raw content,
        // only that the element itself is present with a raw-text child
        // marker (proves the body wasn't silently dropped entirely).
        const hasBody = (node.childNodes || []).some(
            c => c.nodeName === '#text' && c.value.trim() !== ''
        );
        out.children = [{ type: 'opaque-body', present: hasBody }];
    } else {
        out.children = (node.childNodes || []).map(simplify);
    }
    return out;
}

try {
    const text = fs.readFileSync(path, 'utf8');
    const document = parse5.parse(text, { sourceCodeLocationInfo: false });
    process.stdout.write(JSON.stringify(simplify(document)));
} catch (e) {
    process.stderr.write(String(e && e.message ? e.message : e));
    process.exit(1);
}
"""
NODE = os.environ.get("NODE", "node")

WS_RE = re.compile(r"\s+")


def norm_ws(s):
    return WS_RE.sub(" ", s).strip()


def parse_html_via_node(path):
    proc = subprocess.run(
        [NODE, "-e", NODE_HELPER, "--", path],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True
    )
    if proc.returncode != 0: return None, proc.stderr.strip()
    try:
        return json.loads(proc.stdout), None
    except json.JSONDecodeError as e:
        return None, "failed to decode JSON from node helper: %s" % e


def significant_children(node):
    out = []
    for child in node.get("children", []):
        if child["type"] == "text" and child["value"].strip() == "": continue
        out.append(child)

    return out


def describe(node):
    if node is None: return "<nothing>"
    if node["type"] == "element": return "<%s>" % node["tagName"]
    if node["type"] == "text": return "text %r" % node["value"]
    if node["type"] == "comment": return "comment %r" % node["value"]
    if node["type"] == "doctype": return "doctype %r" % node["name"]
    if node["type"] == "opaque-body": return "opaque-body(present=%r)" % node["present"]

    return "node type %r" % node["type"]


def walk(a, b, path, errors):
    if a["type"] != b["type"]:
        errors.append("%s: node type mismatch: %s vs %s" % (path, describe(a), describe(b)))
        return

    if a["type"] in ("root",):
        a_kids = significant_children(a)
        b_kids = significant_children(b)
        if len(a_kids) != len(b_kids):
            errors.append(
                "%s: child count mismatch: %d vs %d\n    ORIGINAL : %s\n    FORMATTED: %s"
                % (path, len(a_kids), len(b_kids),
                   [describe(k) for k in a_kids], [describe(k) for k in b_kids])
            )
            return
        for i, (ak, bk) in enumerate(zip(a_kids, b_kids)):
            walk(ak, bk, "%s[%d]" % (path, i), errors)

    elif a["type"] == "element":
        if a["tagName"] != b["tagName"]:
            errors.append("%s: element name mismatch: %r vs %r" % (path, a["tagName"], b["tagName"]))
        if a["attrs"] != b["attrs"]:
            errors.append(
                "%s <%s>: attribute mismatch (order/name/value):\n    ORIGINAL : %s\n    FORMATTED: %s"
                % (path, a["tagName"], a["attrs"], b["attrs"])
            )
        a_kids = significant_children(a)
        b_kids = significant_children(b)
        if len(a_kids) != len(b_kids):
            errors.append(
                "%s <%s>: child count mismatch: %d vs %d\n    ORIGINAL : %s\n    FORMATTED: %s"
                % (path, a["tagName"], len(a_kids), len(b_kids),
                   [describe(k) for k in a_kids], [describe(k) for k in b_kids])
            )
            return
        for i, (ak, bk) in enumerate(zip(a_kids, b_kids)):
            walk(ak, bk, "%s/<%s>[%d]" % (path, a["tagName"], i), errors)

    elif a["type"] == "text":
        na, nb = norm_ws(a["value"]), norm_ws(b["value"])
        if na != nb:
            errors.append("%s: text content mismatch:\n    ORIGINAL : %r\n    FORMATTED: %r" % (path, na, nb))

    elif a["type"] == "comment":
        na, nb = norm_ws(a["value"]), norm_ws(b["value"])
        if na != nb:
            errors.append("%s: comment text mismatch:\n    ORIGINAL : %r\n    FORMATTED: %r" % (path, na, nb))

    elif a["type"] == "doctype":
        if (a["name"], a["publicId"], a["systemId"]) != (b["name"], b["publicId"], b["systemId"]):
            errors.append(
                "%s: doctype mismatch:\n    ORIGINAL : %s\n    FORMATTED: %s"
                % (path, (a["name"], a["publicId"], a["systemId"]), (b["name"], b["publicId"], b["systemId"]))
            )

    elif a["type"] == "opaque-body":
        if a["present"] != b["present"]:
            errors.append(
                "%s: script/style body presence mismatch (content dropped entirely):\n"
                "    ORIGINAL present=%r, FORMATTED present=%r" % (path, a["present"], b["present"])
            )


def main():
    if len(sys.argv) != 3:
        sys.stderr.write("Usage: html_content_diff.sh <original.html> <formatted.html>\n")
        sys.exit(2)

    orig_path, fmt_path = sys.argv[1], sys.argv[2]

    orig_tree, orig_err = parse_html_via_node(orig_path)
    if orig_err is not None:
        sys.stderr.write("ERROR: original file failed to parse via parse5: %s: %s\n" % (orig_path, orig_err))
        sys.exit(2)

    fmt_tree, fmt_err = parse_html_via_node(fmt_path)
    if fmt_err is not None:
        sys.stderr.write("ERROR: formatted file failed to parse via parse5: %s: %s\n" % (fmt_path, fmt_err))
        sys.exit(2)

    errors = []
    walk(orig_tree, fmt_tree, "root", errors)

    if errors:
        print("CONTENT MISMATCH (%d issue(s)) between %s and %s:" % (len(errors), orig_path, fmt_path))
        for e in errors: print("  - %s" % e)
        sys.exit(1)

    print("OK: content preserved between %s and %s" % (orig_path, fmt_path))
    sys.exit(0)


if __name__ == "__main__": main()
