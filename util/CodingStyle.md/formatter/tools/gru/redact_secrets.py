#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.

"""Redacts likely API keys/tokens from an extract_comments.py-format corpus
file in place, before any Pool A/Pool B extraction or auto-labeling sees it
(STATE_AI.md's "acquire_corpus.sh should filter/redact likely API keys" item)
-- a scraped repo's leaked secret must never reach sample_default.txt or any
committed corpus.

Input/output format is unchanged: "<lang>\\t<escaped comment text>" per line
(same escaping as extract_comments.py's escape()). Only the comment-text
column is scanned; matches are replaced with "[REDACTED]" in place.

Covers named-provider formats (Google, AWS, GitHub, Stripe, OpenAI/Anthropic,
Slack) plus a narrow generic high-entropy fallback for key/secret/token-
looking assignments that don't match a named provider format. The generic
fallback is intentionally conservative (long mixed-case-and-digit run near a
key-ish word) to avoid mass-redacting ordinary hashes/identifiers in comments.

Usage:
    python3 redact_secrets.py <file> [<file> ...]
"""

import argparse
import math
import re
import sys

_REDACTED = "[REDACTED]"

# Named-provider patterns: matched and replaced wholesale, no surrounding
# context needed -- these prefixes/shapes are distinctive enough on their own.
_NAMED_PATTERNS = [
    re.compile(r"AIza[0-9A-Za-z\-_]{35}"),                       # Google API key
    re.compile(r"A(?:KIA|SIA|GPA|IDA|ROA|IPA|NPA|NVA|SCA)[0-9A-Z]{16}"),  # AWS access key id
    re.compile(r"gh[pousr]_[A-Za-z0-9]{36,255}"),                 # GitHub token
    re.compile(r"github_pat_[A-Za-z0-9_]{22,255}"),               # GitHub fine-grained PAT
    re.compile(r"(?:sk|pk|rk)_(?:live|test)_[0-9a-zA-Z]{16,}"),    # Stripe
    re.compile(r"sk-ant-[A-Za-z0-9\-_]{20,}"),                     # Anthropic (before generic sk-)
    re.compile(r"sk-(?:proj-)?[A-Za-z0-9_\-]{20,}"),               # OpenAI
    re.compile(r"xox[baprs]-[0-9A-Za-z\-]{10,}"),                  # Slack
]

# Generic fallback: an assignment-shaped "<key-ish word> [:=] <token>" where
# the token itself looks high-entropy (mixed upper/lower/digit, length >= 20).
# Anchored on a key-ish word to avoid flagging ordinary hashes/identifiers
# that happen to appear bare in a comment.
_KEYish_ASSIGNMENT_RE = re.compile(
    r"(?i)\b(api[_-]?key|secret|token|password|access[_-]?key|auth)\b\s*[:=]\s*"
    r"['\"]?([A-Za-z0-9_\-/+=]{20,})"
)


def _shannon_entropy(s):
    if not s:
        return 0.0
    freq = {}
    for ch in s:
        freq[ch] = freq.get(ch, 0) + 1
    length = len(s)
    return -sum((c / length) * math.log2(c / length) for c in freq.values())


def _looks_high_entropy(token):
    has_upper = any(c.isupper() for c in token)
    has_lower = any(c.islower() for c in token)
    has_digit = any(c.isdigit() for c in token)
    if sum([has_upper, has_lower, has_digit]) < 2:
        return False
    return _shannon_entropy(token) >= 3.5


def redact(comment_text):
    redacted_count = 0

    def _sub_named(m):
        nonlocal redacted_count
        redacted_count += 1
        return _REDACTED

    text = comment_text
    for pattern in _NAMED_PATTERNS:
        text = pattern.sub(_sub_named, text)

    def _sub_generic(m):
        nonlocal redacted_count
        token = m.group(2)
        if _looks_high_entropy(token):
            redacted_count += 1
            return m.group(1) + "=" + _REDACTED
        return m.group(0)

    text = _KEYish_ASSIGNMENT_RE.sub(_sub_generic, text)
    return text, redacted_count


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("files", nargs="+", help="Corpus file(s) to redact in place")
    args = parser.parse_args()

    total_redacted = 0
    for path in args.files:
        with open(path, "r", encoding="utf-8") as f:
            lines = f.readlines()

        out_lines = []
        for line in lines:
            line = line.rstrip("\n")
            parts = line.split("\t", 1)
            if len(parts) != 2:
                out_lines.append(line)
                continue
            lang, comment_text = parts
            redacted_text, count = redact(comment_text)
            total_redacted += count
            out_lines.append(f"{lang}\t{redacted_text}")

        with open(path, "w", encoding="utf-8") as f:
            f.write("\n".join(out_lines) + ("\n" if out_lines else ""))

    print(f"redact_secrets: redacted {total_redacted} likely secret(s) across {len(args.files)} file(s)", file=sys.stderr)


if __name__ == "__main__":
    main()
