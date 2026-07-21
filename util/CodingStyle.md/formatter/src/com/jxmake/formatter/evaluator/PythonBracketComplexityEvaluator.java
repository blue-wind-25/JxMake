/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.evaluator;

import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Python3 bracket-complexity detector (STYLE_PYTHON3.md §1). An extension of, not a straight
 * port of, {@link ComplexityPaddingEvaluator}'s C-family tight/loose heuristic -- deliberately
 * self-contained rather than delegating to it, since two of its assumptions don't carry over:
 * it never treats a nested `{` as a complexity signal (C-family `{}` is only ever an initializer,
 * essentially never appearing as call-argument/index content, unlike Python's dict/set literals),
 * and its Kotlin receiver-function-type carve-out (`Type.() -> Ret`) has no Python analog.
 *
 * <p>Callers pass the token slice strictly between a bracket pair's open/close punctuation (not
 * including the brackets themselves) -- same contract as {@link ComplexityPaddingEvaluator#isLoose}.
 */
public class PythonBracketComplexityEvaluator {

    /** Result of §1.5's dict-vs-set disambiguation for a `{}` literal's content. */
    public enum BraceKind {
        DICT,
        SET
    }

    /** §1.1 baseline + §1.2 comprehensions, for `()` content (call arguments, parenthesized
     *  expressions/tuples, and generator expressions -- a bare `for` at the content's own top
     *  level makes any of these loose, same as a `[]`/`{}` comprehension). */
    public boolean isLooseParen(final List<Token> contentTokens) {
        return containsTopLevelComprehension(contentTokens) || containsNestedBracket(contentTokens);
    }

    /** §1.1/§1.2/§1.3/§1.4, for `[]` content (subscripts, slices, and list literals). A top-level
     *  comprehension makes the bracket unconditionally loose (§1.2). Otherwise, per §1.3, the
     *  content is split on top-level `:` (a slice's own separators, never a complexity signal by
     *  themselves) into independent sub-expressions -- the bracket goes loose if any one segment
     *  does (§1.1's nesting-propagation principle applied per-segment). A plain index or list
     *  literal (no top-level `:`) degrades to a single segment, i.e. the §1.1 baseline unchanged.
     *  §1.4 star-unpacking needs no special case here -- {@code *}/{@code **} are `OP` tokens, not
     *  bracket punctuation, so they never trip {@link #containsNestedBracket} on their own. */
    public boolean isLooseBracket(final List<Token> contentTokens) {
        if (containsTopLevelComprehension(contentTokens)) {
            return true;
        }
        for (final List<Token> segment : splitOnTopLevelColon(contentTokens)) {
            if (containsNestedBracket(segment)) {
                return true;
            }
        }
        return false;
    }

    /** §1.5: `{}` padding does not use the §1.1 complexity heuristic at all -- every non-empty
     *  `{}` (dict or set, comprehension or literal, with or without `*`/`**` unpacking) is
     *  unconditionally loose, and an empty `{}` is unconditionally tight, per STYLE.md §3.3's
     *  "always pad non-empty `{}`" rule applied uniformly (RDD_KEY_184: no unpacking-only tight
     *  carve-out). */
    public boolean isLooseBrace(final List<Token> contentTokens) {
        return !isEmptyContent(contentTokens);
    }

    /** §1.5's dict-vs-set disambiguation: presence of a top-level `:` means dict; none means set;
     *  empty `{}` is defined as dict (Python has no empty-set literal -- `set()` is a call, not a
     *  literal). A comprehension's own leading `key: value` (dict comprehension) vs. bare
     *  expression (set comprehension) is distinguished the same way, since that `:` is also at
     *  the content's own top level. */
    public BraceKind classifyBrace(final List<Token> contentTokens) {
        if (isEmptyContent(contentTokens)) {
            return BraceKind.DICT;
        }
        return containsTopLevelColon(contentTokens) ? BraceKind.DICT : BraceKind.SET;
    }

    /** True if {@code tokens} contains a `for` KEYWORD at its own top level (depth 0 relative to
     *  the content itself -- a `for` nested inside a deeper bracket within the content, e.g. an
     *  inner call's own comprehension argument, does not count for the outer content). */
    private boolean containsTopLevelComprehension(final List<Token> tokens) {
        int depth = 0;
        for (final Token t : tokens) {
            if (isOpenBracket(t)) {
                depth++;
            } else if (isCloseBracket(t)) {
                depth--;
            } else if (depth == 0 && Token.isKeyword(t, "for")) {
                return true;
            }
        }
        return false;
    }

    /** True if {@code tokens} contains any `(`/`[`/`{` at any depth -- a function call or any
     *  nested bracket, at any nesting level, signals complexity per STYLE.md §3.1's outward-
     *  propagation rule (an inner loose bracket makes every enclosing bracket loose too). */
    private boolean containsNestedBracket(final List<Token> tokens) {
        for (final Token t : tokens) {
            if (isOpenBracket(t)) {
                return true;
            }
        }
        return false;
    }

    /** True if {@code tokens} contains a `:` PUNCT at its own top level (depth 0). Used both by
     *  {@link #classifyBrace} (dict-vs-set) and {@link #isLooseBracket} (slice-segment splitting,
     *  via {@link #splitOnTopLevelColon}). */
    private boolean containsTopLevelColon(final List<Token> tokens) {
        int depth = 0;
        for (final Token t : tokens) {
            if (isOpenBracket(t)) {
                depth++;
            } else if (isCloseBracket(t)) {
                depth--;
            } else if (depth == 0 && Token.isPunct(t, ":")) {
                return true;
            }
        }
        return false;
    }

    /** Splits {@code tokens} into segments on every top-level (depth 0) `:` PUNCT, the `:` itself
     *  dropped from every segment (§1.3: slice colons are pure separators, never part of either
     *  side's own sub-expression). A nested bracket's own internal `:` (e.g. a dict literal or a
     *  nested slice) is correctly excluded from splitting since it's below depth 0. No top-level
     *  `:` at all yields a single segment equal to the full input. */
    private List<List<Token>> splitOnTopLevelColon(final List<Token> tokens) {
        final List<List<Token>> segments = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        int depth = 0;
        for (final Token t : tokens) {
            if (isOpenBracket(t)) {
                depth++;
                current.add(t);
            } else if (isCloseBracket(t)) {
                depth--;
                current.add(t);
            } else if (depth == 0 && Token.isPunct(t, ":")) {
                segments.add(current);
                current = new ArrayList<>();
            } else {
                current.add(t);
            }
        }
        segments.add(current);
        return segments;
    }

    /** True if {@code tokens} has no real content -- every token is a gap token (whitespace/
     *  newline/comment) or the list is empty. Used for §1.5's empty-`{}` special case. */
    private boolean isEmptyContent(final List<Token> tokens) {
        for (final Token t : tokens) {
            if (!Token.isGapToken(t)) {
                return false;
            }
        }
        return true;
    }

    private boolean isOpenBracket(final Token t) {
        return t.type == TokenType.PUNCT
                && ("(".equals(t.text) || "[".equals(t.text) || "{".equals(t.text));
    }

    private boolean isCloseBracket(final Token t) {
        return t.type == TokenType.PUNCT
                && (")".equals(t.text) || "]".equals(t.text) || "}".equals(t.text));
    }
}
