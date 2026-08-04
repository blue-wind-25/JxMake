#!/usr/bin/env python3
"""
regroup_synthetic.py -- Python 3.6+

Takes one pasted-in file where Pool A and Pool B lines (and possibly output
from several chat responses/models) have been dumped together, and splits
them back into pool_a.tsv / pool_b.tsv, per RDD_EXT_20/21 schema. Malformed
or ambiguous lines go to unresolved.tsv for manual review instead of being
silently dropped.

Whitespace between fields is tolerant: any run of spaces/tabs counts as one
separator (maxsplit=3), so inconsistent copy/paste spacing from chat UIs is
fine as long as each line has at least 3 whitespace-separated fields before
the comment text.

Classification rule:
    targetWordIndex == 0                       -> Pool A
    targetWordIndex == index of the LAST token -> Pool B
    (single-token comment satisfies both       -> Pool A, noted in unresolved
     only if it ALSO fails other checks)
    anything else / malformed                  -> unresolved

Pool A candidates (targetWordIndex == 0) also get an optional leading-word
cross-check against --expected-words (preferred) and/or --vocab:
  --expected-words: the persistent, append-only sidecar
    gen_synthetic_prompt.py writes each run's actual requested word slice
    to (default .gen_synthetic_requested_words.txt next to that script).
    This is the tight check -- catches a chat LLM drifting into unprompted
    filler once it runs past the words it was actually asked for, since
    such filler words were never appended to this file by any run.
  --vocab: the full explicit_vocab.txt embedding vocabulary. Looser than
    --expected-words (it includes every word ever embedded, including
    ordinary English words like "the"/"a"/"to" that are valid vocab
    entries but may not have been requested in any specific batch) --
    kept mainly to catch outright garbage (e.g. a leading token like
    "sizeof(int)" with code glued onto it, not a real word at all).
Either or both may be omitted (pass empty string) to disable that check;
if both are given, expected-words is checked first, then vocab.

Usage:
    python3 regroup_synthetic.py --input pasted.txt --outdir ./out
    python3 regroup_synthetic.py --input pasted.txt --outdir ./out \\
        --expected-words ../.gen_synthetic_requested_words.txt --vocab explicit_vocab.txt
"""

import argparse
import os
import re
import sys

VALID_LABELS            = {"YES", "NO"}
FIELD_SPLIT             = re.compile(r"[ \t]+")
DEFAULT_VOCAB           = os.path.join(os.path.dirname(__file__), "explicit_vocab.txt")
DEFAULT_REQUESTED_WORDS = os.path.join(os.path.dirname(__file__), ".gen_synthetic_requested_words.txt")


def load_word_set(path):
    """Returns a lowercased set of words from a one-word-per-line file
    (blank lines and '#'-comment lines skipped), or None if the path is
    empty/missing -- fail open, not closed, so a missing/misnamed file
    never causes silent data loss by rejecting everything."""
    if not path or not os.path.exists(path): return None
    words = set()
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"): continue
            words.add(line.lower())

    return words or None


def classify(lang, label, idx_str, text, expected_words=None, vocab=None):
    """Returns ('A'|'B'|None, reason_if_none)."""
    if lang.strip() == "": return None, "empty lang"
    if label not in VALID_LABELS: return None, "label not YES/NO: {!r}".format(label)
    try:
        idx = int(idx_str)
    except ValueError:
        return None, "targetWordIndex not an int: {!r}".format(idx_str)
    if idx < 0: return None, "negative targetWordIndex"
    if text.strip() == "":
        return None, "empty comment text"

    tokens   = text.strip().split()
    last_idx = len(tokens) - 1

    if idx == 0:
        leading = re.sub(r"[^\w]+$", "", tokens[0]).lower()
        if expected_words is not None and leading not in expected_words:
            return None, "leading word {!r} was never requested by gen_synthetic_prompt.py (idx==0)".format(tokens[0])
        if vocab is not None and leading not in vocab:
            return None, "leading word {!r} not in vocab (idx==0)".format(tokens[0])
        return "A", None
    if idx == last_idx: return "B", None

    return None, "targetWordIndex {} matches neither first (0) nor last ({}) token".format(idx, last_idx)


def main():
    ap = argparse.ArgumentParser(description="Regroup scattered Pool A/B lines back into separate files.")
    ap.add_argument("--input", required=True, help="pasted combined file")
    ap.add_argument("--outdir", default=".", help="directory to write pool_a.tsv / pool_b.tsv / unresolved.tsv")
    ap.add_argument("--expected-words", default=DEFAULT_REQUESTED_WORDS,
                     help="gen_synthetic_prompt.py's append-only requested-words sidecar; Pool A (idx==0) "
                          "lines whose leading word was never requested go to unresolved.tsv instead of "
                          "pool_a.tsv. Pass an empty string to disable this check.")
    ap.add_argument("--vocab", default=DEFAULT_VOCAB,
                     help="explicit_vocab.txt path, a looser fallback check on top of --expected-words "
                          "(catches garbage leading tokens, e.g. code glued onto a keyword with no space). "
                          "Pass an empty string to disable this check.")
    args = ap.parse_args()

    os.makedirs(args.outdir, exist_ok=True)

    expected_words = load_word_set(args.expected_words)
    if expected_words is None:
        print("# note: --expected-words check disabled (no file at {!r})".format(args.expected_words),
              file=sys.stderr)
    vocab = load_word_set(args.vocab)
    if vocab is None:
        print("# note: --vocab check disabled (no file at {!r})".format(args.vocab), file=sys.stderr)

    pool_a, pool_b, unresolved = [], [], []
    total_lines              = 0
    skipped_blank_or_comment = 0

    with open(args.input, "r", encoding="utf-8") as f:
        for raw_line in f:
            line     = raw_line.rstrip("\n").rstrip("\r")
            stripped = line.strip()
            if not stripped or stripped.startswith("#"):
                skipped_blank_or_comment += 1
                continue

            total_lines += 1
            parts        = FIELD_SPLIT.split(stripped, maxsplit=3)
            if len(parts) < 4:
                unresolved.append((line, "fewer than 4 fields"))
                continue

            lang, label, idx_str, text = parts
            pool, reason = classify(lang, label, idx_str, text,
                                     expected_words=expected_words, vocab=vocab)
            # normalize the output line to real tabs regardless of input spacing
            norm_line = "\t".join([lang, label, idx_str, text])

            if pool == "A": pool_a.append(norm_line)
            elif pool == "B": pool_b.append(norm_line)
            else:
                unresolved.append((line, reason))

    a_path = os.path.join(args.outdir, "pool_a.tsv")
    b_path = os.path.join(args.outdir, "pool_b.tsv")
    u_path = os.path.join(args.outdir, "unresolved.tsv")

    with open(a_path, "w", encoding="utf-8") as f:
        f.write("\n".join(pool_a) + ("\n" if pool_a else ""))
    with open(b_path, "w", encoding="utf-8") as f:
        f.write("\n".join(pool_b) + ("\n" if pool_b else ""))
    with open(u_path, "w", encoding="utf-8") as f:
        for line, reason in unresolved:
            f.write("{}\t# {}\n".format(line, reason))

    print("processed {} data lines ({} blank/comment lines skipped)".format(
        total_lines, skipped_blank_or_comment))
    print("  Pool A: {} -> {}".format(len(pool_a), a_path))
    print("  Pool B: {} -> {}".format(len(pool_b), b_path))
    print("  unresolved: {} -> {}".format(len(unresolved), u_path))
    if unresolved:
        print("  (review unresolved.tsv by hand -- these were not discarded, just not auto-classified)",
              file=sys.stderr)


if __name__ == "__main__": main()
