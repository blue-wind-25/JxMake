/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * C/C++-specific STYLE_C_CPP.md sections not owned by another rule class: §1, §2 (Allman
 * conversion), §3, §4, §9, §10, §11. (§5/§6 are already handled by {@code MiscRule}/
 * {@code DeclarationAlignmentRule}; §7's additional C/C++ closing-comment cases and the lambda
 * part of §2 are already handled by {@code BlockStructureRule}; §8 is explicitly
 * preserve-as-is.)
 */
public class CppSpecificRule {

    private final String language;

    public CppSpecificRule(final String language) {
        this.language = language;
    }

    /**
     * STYLE_C_CPP.md §1: C always writes an empty parameter list as `(void)`; C++ always omits
     * it (`()`). Resolved -- see STATE.md "§1 empty parameter list": rather than build general
     * declaration-vs-call detection (no STYLE.md worked example exercises that distinction, and
     * the cost of guessing wrong here is corrupting working code, not just a missed format), the
     * two directions are handled with two different, independently-safe signals:
     * <ul>
     * <li>C, `()` → `(void)`: only rewritten when the matching `)` is directly followed by `{`
     * (skipping whitespace/comments) -- i.e. only recognized function <b>definitions</b>, the
     * same "identifier before `(`, body brace after `)`" signal already used elsewhere in this
     * codebase (see {@code BlockStructureRule}'s function-body brace detection) to distinguish a
     * definition from a call without an AST, since a call is never followed by `{`. A bare
     * prototype (`void foo();`, no body) is a documented, deliberate gap -- it is structurally
     * identical to a call (`foo();`) without a body-brace signal to anchor on, so it is left
     * untouched rather than guessed at.</li>
     * <li>C++, `(void)` → `()`: rewritten wherever it occurs, body or no body, with no extra
     * signal needed -- `IDENTIFIER(void)` is never valid call syntax (`void` cannot be passed as
     * an argument), so every occurrence directly after a candidate function name is a signature,
     * by construction of the language itself.</li>
     * </ul>
     * Both directions require the identifier immediately before `(` to not itself be preceded by
     * `new` (excludes a constructor call via `new Identifier()`, same guard already used by
     * {@code BlockStructureRule.isFunctionBodyBrace}), and both skip the rewrite entirely if a
     * comment sits between the parens (nothing to guess at structurally, but consistent with this
     * file's existing "a comment in the gap blocks the rewrite" posture).
     */
    public String enforceEmptyParameterList(final List<Token> tokens) {
        final boolean isC = "c".equals(language);
        final Map<Integer, Integer> spans = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (!isPunct(tokens.get(i), "(") || !isCandidateSignatureName(tokens, i)) {
                continue;
            }
            final int closeIdx = matchParenForward(tokens, i);
            if (closeIdx < 0 || hasCommentBetween(tokens, i, closeIdx)) {
                continue;
            }
            if (isC) {
                if (isEmptyBetween(tokens, i, closeIdx) && isFollowedByFunctionBody(tokens, closeIdx)) {
                    spans.put(i, closeIdx);
                }
            } else if (isOnlyVoidBetween(tokens, i, closeIdx)) {
                spans.put(i, closeIdx);
            }
        }

        final StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < tokens.size()) {
            final Integer closeIdx = spans.get(i);
            if (closeIdx != null) {
                out.append(isC ? "(void)" : "()");
                i = closeIdx + 1;
            } else {
                out.append(tokens.get(i).text);
                i++;
            }
        }
        return out.toString();
    }

    /** True iff the token immediately before {@code openIdx} is an IDENTIFIER not itself preceded
     *  by `new` -- the candidate-function-name signal shared by both rewrite directions. */
    private boolean isCandidateSignatureName(final List<Token> tokens, final int openIdx) {
        final int nameIdx = prevSignificantIndex(tokens, openIdx);
        if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
            return false;
        }
        final int beforeName = prevSignificantIndex(tokens, nameIdx);
        return beforeName < 0 || tokens.get(beforeName).type != TokenType.KEYWORD
                || !"new".equals(tokens.get(beforeName).text);
    }

    /** True iff {@code closeIdx} (a `)`) is directly followed by `{` -- the function-definition
     *  signal, never true for a call. */
    private boolean isFollowedByFunctionBody(final List<Token> tokens, final int closeIdx) {
        final int next = nextSignificantIndex(tokens, closeIdx);
        return next >= 0 && isPunct(tokens.get(next), "{");
    }

    private boolean isEmptyBetween(final List<Token> tokens, final int openIdx, final int closeIdx) {
        for (int i = openIdx + 1; i < closeIdx; i++) {
            if (!isGapToken(tokens.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isOnlyVoidBetween(final List<Token> tokens, final int openIdx, final int closeIdx) {
        int sigIdx = -1;
        for (int i = openIdx + 1; i < closeIdx; i++) {
            if (isGapToken(tokens.get(i))) {
                continue;
            }
            if (sigIdx != -1) {
                return false;
            }
            sigIdx = i;
        }
        return sigIdx != -1 && tokens.get(sigIdx).type == TokenType.KEYWORD
                && "void".equals(tokens.get(sigIdx).text);
    }

    private boolean hasCommentBetween(final List<Token> tokens, final int fromExclusive, final int toExclusive) {
        for (int i = fromExclusive + 1; i < toExclusive; i++) {
            final TokenType type = tokens.get(i).type;
            if (type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) {
                return true;
            }
        }
        return false;
    }

    private int matchParenForward(final List<Token> tokens, final int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < tokens.size(); i++) {
            if (isPunct(tokens.get(i), "(")) {
                depth++;
            } else if (isPunct(tokens.get(i), ")")) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int prevSignificantIndex(final List<Token> tokens, final int from) {
        for (int i = from - 1; i >= 0; i--) {
            if (!isGapToken(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int nextSignificantIndex(final List<Token> tokens, final int from) {
        for (int i = from + 1; i < tokens.size(); i++) {
            if (!isGapToken(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isGapToken(final Token t) {
        return t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK;
    }

    private boolean isPunct(final Token t, final String text) {
        return t != null && t.type == TokenType.PUNCT && text.equals(t.text);
    }
}
