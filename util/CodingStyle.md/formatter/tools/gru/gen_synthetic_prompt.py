#!/usr/bin/env python3
"""
gen_synthetic_prompt.py -- Python 3.6+

Generates a self-contained copy/paste prompt (Pool A + Pool B, RDD_EXT_20/21
schema) for a chat LLM (Gemini/Grok/etc.), pulling the next unused slice of
words from tools/gru/explicit_vocab.txt as the Pool A leading-keyword list.

Persists which word index to start from next time in a small JSON state
file, so consecutive daily runs walk forward through the vocab instead of
repeating the same words. Wraps back to the start once the vocab is
exhausted.

This tool is for SYNTHETIC augmentation data only -- see STATE_AI.md's
RDD_EXT_23 note. It is separate from, and not a substitute for, the real
acquire_corpus.sh + hand-labeling pipeline (RDD_EXT_19/20).

Usage:
    python3 gen_synthetic_prompt.py
    python3 gen_synthetic_prompt.py --words-per-batch 30
    python3 gen_synthetic_prompt.py --reset
    python3 gen_synthetic_prompt.py --vocab path/to/explicit_vocab.txt --state path/to/state.json
"""

import argparse
import json
import os
import sys

DEFAULT_VOCAB = os.path.join(os.path.dirname(__file__), "explicit_vocab.txt")
DEFAULT_STATE = os.path.join(os.path.dirname(__file__), ".gen_synthetic_state.json")
DEFAULT_LANGS = ["c", "cpp", "java", "python", "js"]

PROMPT_TEMPLATE = """Generate exactly {total} lines of training data. Output ONLY the lines, no header, no explanation, no markdown formatting/backticks.

Each line: 4 fields separated by a single space or tab (either is fine):
  <lang> <label> <targetWordIndex> <comment-text>

  lang = one of: {langs}
  label = YES or NO
  targetWordIndex = 0 for Pool A lines, or the 0-based index of the final token for Pool B lines (see below)
  comment-text = a short realistic code comment, plain words separated by single spaces, no literal tabs

Pool A ({n} lines) -- leading-keyword ambiguity, targetWordIndex=0:
  Use ONLY these leading words, one line per word below and IN THIS ORDER, alternating YES/NO down the list:
  {word_slice}
  YES = word is prose language in that spot. NO = word is code syntax (e.g. "for (int i...").

Pool B ({n} lines) -- trailing-period ambiguity, targetWordIndex = index of the final token:
  Alternate YES (period ends a sentence) / NO (period is part of an abbreviation/token like "etc." or "vs.").

Vary sentence length (3-12 words). No two lines may share the same sentence structure.
Print Pool A lines first, then Pool B lines, in the order described above.
"""


def load_vocab(path):
    words = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            words.append(line)
    if not words:
        raise ValueError("no words found in vocab file: {}".format(path))
    return words


def load_state(path):
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
        return data.get("next_index", 0)
    return 0


def save_state(path, next_index):
    with open(path, "w", encoding="utf-8") as f:
        json.dump({"next_index": next_index}, f)
        f.write("\n")


def take_slice(words, start, count):
    """Return `count` words starting at `start`, wrapping around the list.
    Returns (slice_words, wrapped_bool, new_next_index)."""
    n = len(words)
    if count >= n:
        # asking for more words than exist -- just return the whole vocab once
        return list(words), True, 0

    end = start + count
    if end <= n:
        sl = words[start:end]
        wrapped = False
        new_next = end % n
    else:
        sl = words[start:n] + words[0:end - n]
        wrapped = True
        new_next = end - n
    return sl, wrapped, new_next


def main():
    ap = argparse.ArgumentParser(description="Generate the rotating synthetic-data prompt.")
    ap.add_argument("--vocab", default=DEFAULT_VOCAB, help="path to explicit_vocab.txt")
    ap.add_argument("--state", default=DEFAULT_STATE, help="path to state json file")
    ap.add_argument("--words-per-batch", type=int, default=20,
                     help="number of Pool A words to pull this run (also used as Pool B line count)")
    ap.add_argument("--langs", nargs="+", default=DEFAULT_LANGS, help="languages to rotate across")
    ap.add_argument("--reset", action="store_true", help="reset state back to index 0 before running")
    ap.add_argument("--out", default=None, help="optional file to also save the prompt to")
    args = ap.parse_args()

    if args.words_per_batch <= 0:
        print("error: --words-per-batch must be positive", file=sys.stderr)
        sys.exit(1)

    words = load_vocab(args.vocab)

    if args.reset:
        start = 0
    else:
        start = load_state(args.state)
        start = start % len(words)  # defensive, in case vocab shrank

    word_slice, wrapped, new_next = take_slice(words, start, args.words_per_batch)
    save_state(args.state, new_next)

    prompt = PROMPT_TEMPLATE.format(
        total=2 * len(word_slice),
        langs=", ".join(args.langs),
        n=len(word_slice),
        word_slice=", ".join(word_slice),
    )

    # status info goes to stderr so stdout stays paste-clean
    print("# vocab words {}-{} of {} (wrapped: {})".format(
        start, start + len(word_slice) - 1, len(words), wrapped), file=sys.stderr)
    print("# next run will start at index {}".format(new_next), file=sys.stderr)

    print(prompt)

    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            f.write(prompt)
        print("# also saved to {}".format(args.out), file=sys.stderr)


if __name__ == "__main__":
    main()
