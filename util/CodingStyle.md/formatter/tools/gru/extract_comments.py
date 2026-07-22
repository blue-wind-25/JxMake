#!/usr/bin/env python3
"""Extracts raw comment text from source files into a flat file for
tools/gru/CommentAbstainTally.java to feed through the real CommentClassifier.

This is extraction only -- it does not decide anything about ambiguity itself
(see STATE_AI.md, open item 9: the real ABSTAIN-rate measurement must run the
actual rule-based CommentClassifier, not a reimplementation of its logic in
Python). Output line format: "<lang>\t<comment text>", where the comment
text has its own literal newlines/tabs escaped as "\n"/"\t" so the whole
record stays on one line. <lang> is one of the language strings CommentAbstainTally
passes to `new com.jxmake.formatter.Lang(lang)`.

Usage:
    python3 extract_comments.py <root-dir> [<root-dir> ...] --out <output-file>
"""

import argparse
import os
import re
import sys

# Extension -> language string, mirroring com.jxmake.formatter.Lang.infer's
# recognized SUPPORTED_LANGUAGES set. Only languages with a comment syntax
# worth extracting are listed; unrecognized extensions are skipped.
EXTENSION_TO_LANG = {
    ".c": "c", ".h": "c",
    ".cpp": "cpp", ".cc": "cpp", ".cxx": "cpp", ".hpp": "cpp", ".hh": "cpp", ".hxx": "cpp",
    ".java": "java",
    ".kt": "kotlin", ".kts": "kotlin",
    ".json5": "json5",
    ".yaml": "yaml", ".yml": "yaml",
    ".toml": "toml",
    ".xml": "xml",
    ".css": "css",
    ".html": "html5", ".htm": "html5",
    ".js": "js", ".mjs": "js", ".cjs": "js",
    ".ts": "ts",
    ".py": "python3",
}

# Languages whose comment syntax is "//" line + "/* */" block.
C_STYLE_LANGS = {"c", "cpp", "java", "kotlin", "json5", "css", "js", "ts"}
# Languages whose comment syntax is "#" line only.
HASH_STYLE_LANGS = {"yaml", "toml", "python3"}
# Languages whose comment syntax is "<!-- -->" block only.
XML_STYLE_LANGS = {"xml", "html5"}

LINE_COMMENT_RE = re.compile(r"//(.*)$", re.MULTILINE)
BLOCK_COMMENT_RE = re.compile(r"/\*(.*?)\*/", re.DOTALL)
HASH_COMMENT_RE = re.compile(r"#(.*)$", re.MULTILINE)
XML_COMMENT_RE = re.compile(r"<!--(.*?)-->", re.DOTALL)

# "3rd_party" excluded per STATE_AI.md's item-9 lesson: a single vendored
# non-code-comment data file (a bitmap-font glyph table) dominated a whole
# language's ABSTAIN count in an earlier run and wasn't representative of
# ordinary hand-written comment style.
SKIP_DIR_NAMES = {".git", "target", "node_modules", "__pycache__", "3rd_party"}


def extract_from_text(text, lang):
    comments = []
    if lang in C_STYLE_LANGS:
        for m in BLOCK_COMMENT_RE.finditer(text):
            comments.append(m.group(1))
        for m in LINE_COMMENT_RE.finditer(text):
            comments.append(m.group(1))
    elif lang in HASH_STYLE_LANGS:
        for m in HASH_COMMENT_RE.finditer(text):
            comments.append(m.group(1))
    elif lang in XML_STYLE_LANGS:
        for m in XML_COMMENT_RE.finditer(text):
            comments.append(m.group(1))
    return comments


def escape(comment_text):
    return comment_text.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("roots", nargs="+", help="Root directories to scan")
    parser.add_argument("--out", required=True, help="Output file path")
    args = parser.parse_args()

    total = 0
    with open(args.out, "w", encoding="utf-8") as out:
        for root in args.roots:
            for dirpath, dirnames, filenames in os.walk(root):
                dirnames[:] = [d for d in dirnames if d not in SKIP_DIR_NAMES]
                for filename in filenames:
                    ext = os.path.splitext(filename)[1].lower()
                    lang = EXTENSION_TO_LANG.get(ext)
                    if lang is None:
                        continue
                    path = os.path.join(dirpath, filename)
                    try:
                        with open(path, "r", encoding="utf-8", errors="ignore") as f:
                            text = f.read()
                    except OSError as e:
                        print(f"extract_comments: skipping unreadable file {path}: {e}", file=sys.stderr)
                        continue
                    for comment_text in extract_from_text(text, lang):
                        if not comment_text.strip():
                            continue
                        out.write(f"{lang}\t{escape(comment_text)}\n")
                        total += 1

    print(f"extract_comments: wrote {total} comment(s) to {args.out}", file=sys.stderr)


if __name__ == "__main__":
    main()
