#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.

"""Curates the ~3.5k-word explicit vocab Vocabulary.java's constructor needs (per STATE_AI.md's
"GRU implementation design"): every keyword across every supported/planned language gets a
guaranteed slot, plus common English comment-corpus words filling the rest.

This is a ONE-OFF generator, not something GruTrainer runs per training pass -- its output,
explicit_vocab.txt, is meant to be curated once and checked in (see that file's own header),
then stay fixed across retrains (changing it would shift every word's embedding-row index,
invalidating any previously trained weights file).

Copyright note: nothing here reproduces any corpus text. The keyword section is our own
per-language reserved-word lists (facts about each language's grammar, not copyrightable
expression). The common-word section is derived by counting single-word frequencies across a
real extracted-comment corpus and keeping only the words themselves (never sentences/phrases) --
individual common words are not copyrightable subject matter (Feist), so this carries no
licensing risk regardless of the source corpus's own license.

Common-word ranking is by CROSS-SOURCE DOCUMENT FREQUENCY, not raw token count (scoped in
STATE_AI.md's "TODO -- explicit_vocab.txt contamination filter" section, 2026-07-31): a word
must appear in at least --min-sources of the per-source comments_<name>.txt files
acquire_corpus.sh produces (ties broken by raw count) to be eligible. Raw-count-only ranking let
words that are merely LOCALLY frequent -- concentrated in one or two sources, e.g. a copyright-
header name repeated across one person's own local dogfood repos, or one hobby project's
hardware jargon -- outrank words that are genuinely common across the whole corpus but happen to
occur once per line rather than piling up in a handful of files. Requiring the word to recur
across multiple independent sources is a much better proxy for "ordinary English comment
vocabulary" than raw frequency alone.

Usage:
    python3 build_vocab.py <dir-of-comments_NAME.txt-files> --out <output-file>
        [--target-size 3500] [--min-sources 2] [--pattern 'comments_*.txt']
"""

import argparse
import glob
import os
import re
import sys
from collections import Counter

# Mirrors com.jxmake.formatter.classifier.KeywordAmbiguityGate's own per-language lists exactly
# (kept in sync by hand, same as that class's own header note about MiscRuleCore) -- these are
# the languages with C-family-style reserved words already gated in the rule-based classifier.
KEYWORDS_C = [
    "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
    "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long",
    "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct",
    "switch", "typedef", "union", "unsigned", "void", "volatile", "while",
]
KEYWORDS_CPP = [
    "alignas", "alignof", "asm", "bool", "catch", "char16_t", "char32_t", "class",
    "co_await", "co_return", "co_yield", "concept", "consteval", "constexpr", "constinit",
    "const_cast", "decltype", "delete", "dynamic_cast", "explicit", "export", "false",
    "final", "friend", "mutable", "namespace", "new", "noexcept", "nullptr", "operator",
    "override", "private", "protected", "public", "reinterpret_cast", "requires",
    "static_assert", "static_cast", "template", "this", "thread_local", "throw", "true",
    "try", "typeid", "typename", "using", "virtual", "wchar_t",
]
KEYWORDS_JAVA = [
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
    "const", "continue", "default", "do", "else", "enum", "extends", "final", "finally",
    "for", "goto", "if", "implements", "import", "instanceof", "interface", "native",
    "new", "package", "permits", "private", "protected", "public", "record", "return",
    "sealed", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
    "throws", "transient", "try", "var", "void", "volatile", "while", "yield", "null",
    "true", "false",
]
KEYWORDS_KOTLIN = [
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
    "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
    "true", "try", "typealias", "typeof", "val", "var", "when", "while",
]
# JS/TS reserved words + TS-only additions (STATE_JS_TS.md: implemented, no keyword-ambiguity
# gate of its own yet, but still one of the "every supported/planned language" this vocab
# promises coverage for).
KEYWORDS_JS_TS = [
    "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete",
    "do", "else", "export", "extends", "false", "finally", "for", "function", "if", "import",
    "in", "instanceof", "new", "null", "return", "super", "switch", "this", "throw", "true",
    "try", "typeof", "var", "void", "while", "with", "yield", "let", "static", "async", "await",
    "of", "get", "set", "interface", "type", "enum", "implements", "private", "protected",
    "public", "readonly", "abstract", "as", "asserts", "is", "keyof", "infer", "module",
    "namespace", "declare", "never", "unknown", "satisfies",
]
# Python3 reserved words (STATE_PYTHON3.md: scaffold-only in the formatter itself, but a
# "planned language" per that file, so it still gets guaranteed vocab slots now).
KEYWORDS_PYTHON3 = [
    "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class",
    "continue", "def", "del", "elif", "else", "except", "finally", "for", "from", "global",
    "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return",
    "try", "while", "with", "yield",
]
# JSON5/YAML/TOML/XML/CSS don't have "reserved words" in the same programming-language sense
# (CSS property/selector names aren't a fixed keyword set; XML has no keywords at all) -- only
# their literal-value tokens are relevant here
KEYWORDS_DATA_FORMATS = ["true", "false", "null", "yes", "no", "nan", "inf"]

ALL_KEYWORD_GROUPS = [
    KEYWORDS_C, KEYWORDS_CPP, KEYWORDS_JAVA, KEYWORDS_KOTLIN,
    KEYWORDS_JS_TS, KEYWORDS_PYTHON3, KEYWORDS_DATA_FORMATS,
]

WORD_RE = re.compile(r"^[A-Za-z]+$")


def unescape(escaped):
    out = []
    i   = 0
    while i < len(escaped):
        c = escaped[i]
        if c == "\\" and i + 1 < len(escaped):
            nxt = escaped[i + 1]
            if nxt == "n":
                out.append("\n")
                i += 2
                continue
            if nxt == "t":
                out.append("\t")
                i += 2
                continue
            if nxt == "\\":
                out.append("\\")
                i += 2
                continue
        out.append(c)
        i += 1

    return "".join(out)


def is_word_char(c):
    return c.isalnum() or c == "_"


def tokenize(text):
    # Mirrors com.jxmake.formatter.classifier.gru.GruClassifier.tokenize exactly, same reasoning
    # as add_target_index.py's own copy: keeps token boundaries (and therefore which strings show
    # up as candidate vocab words) consistent with what the runtime/trainer actually see.
    tokens = []
    i      = 0
    n      = len(text)
    while i < n:
        c = text[i]
        if c.isspace():
            i += 1
            continue
        if is_word_char(c):
            start = i
            while i < n and is_word_char(text[i]): i += 1
            tokens.append(text[start:i])
        else:
            tokens.append(c)
            i += 1

    return tokens


def words_in_source(path, seen_keywords):
    """Returns (set of distinct eligible words in this one source file, Counter of their raw
    counts within it) -- the set is what feeds document frequency, the Counter is the raw-count
    tiebreaker."""
    words_here = set()
    raw_here   = Counter()
    with open(path, "r", encoding="utf-8") as inp:
        for line in inp:
            line = line.rstrip("\n")
            if not line: continue
            tab = line.find("\t")
            if tab < 0: continue
            comment_text = unescape(line[tab + 1:])
            for token in tokenize(comment_text):
                if WORD_RE.match(token) and token not in seen_keywords:
                    # Case-preserved per the finalized architecture (RDD_EXT_12 note on casing
                    # being a real signal) -- "Return" and "return" are counted/ranked separately
                    words_here.add(token)
                    raw_here[token] += 1

    return words_here, raw_here


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("corpus_dir", help="directory of per-source comments_<name>.txt files (acquire_corpus.sh output)")
    parser.add_argument("--out", required=True)
    parser.add_argument("--target-size", type=int, default=3500)
    parser.add_argument("--min-sources", type=int, default=2,
                         help="minimum number of distinct source files a word must appear in to be eligible")
    parser.add_argument("--pattern", default="comments_*.txt",
                         help="glob pattern (within corpus_dir) selecting per-source files")
    args = parser.parse_args()

    explicit_words = []
    seen           = set()
    for group in ALL_KEYWORD_GROUPS:
        for word in group:
            if word not in seen:
                seen.add(word)
                explicit_words.append(word)
    keyword_count = len(explicit_words)

    source_paths = sorted(glob.glob(os.path.join(args.corpus_dir, args.pattern)))
    if not source_paths:
        sys.exit(f"build_vocab: no files matching {args.pattern!r} in {args.corpus_dir}")

    doc_freq  = Counter()
    raw_count = Counter()
    for path in source_paths:
        words_here, raw_here = words_in_source(path, seen)
        for word in words_here: doc_freq[word] += 1
        raw_count.update(raw_here)

    eligible = [w for w in doc_freq if doc_freq[w] >= args.min_sources]
    eligible.sort(key=lambda w: (-doc_freq[w], -raw_count[w], w))

    remaining = max(0, args.target_size - keyword_count)
    taken     = eligible[:remaining]
    for word in taken:
        seen.add(word)
        explicit_words.append(word)

    with open(args.out, "w", encoding="utf-8") as out:
        out.write("# Explicit vocab for the Step 3 GRU (STATE_AI.md's \"GRU implementation\n")
        out.write("# design\"). One word per line, in embedding-row order -- DO NOT REORDER OR\n")
        out.write("# REMOVE LINES once a weights file has been trained against this list, since\n")
        out.write("# that would shift every word's embedding row index. Append-only if this ever\n")
        out.write("# needs to grow. Generated by tools/gru/build_vocab.py -- see that script's\n")
        out.write("# header for the copyright rationale (no corpus text is reproduced here, only\n")
        out.write("# individual common words and language keywords, neither of which is\n")
        out.write(f"# copyrightable subject matter). Common words ranked by cross-source document\n")
        out.write(f"# frequency across {len(source_paths)} source(s), min {args.min_sources} source(s) required.\n")
        for word in explicit_words:
            out.write(word)
            out.write("\n")

    print(f"build_vocab: {keyword_count} keyword(s) + {len(taken)} document-frequency-ranked "
          f"common word(s) (from {len(eligible)} eligible across {len(source_paths)} source(s), "
          f"min {args.min_sources} source(s)) = {len(explicit_words)} total, written to {args.out}",
          file=sys.stderr)
    if len(taken) < remaining:
        print(f"build_vocab: WARNING -- only {len(taken)}/{remaining} common-word slots filled; "
              f"not enough words met the --min-sources={args.min_sources} threshold. Consider "
              f"lowering --min-sources or adding more sources.", file=sys.stderr)


if __name__ == "__main__": main()
