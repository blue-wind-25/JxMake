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
 * Java-specific STYLE_JAVA.md sections not owned by another rule class: §2 (method-definition
 * Allman conversion) and §7 (Import Ordering). (§1/§4/§5/§6/§8 are already fully covered by
 * already-COMPLETE general/shared rule files -- see STATE.md's "`JavaSpecificRule.java` scoping"
 * Resolved Design Decision for the full cross-check; §3 needs zero code in this file either, since
 * it is satisfied by {@code MiscRule.enforceConditionComplexityPadding}, STYLE.md §3.1.)
 */
public class JavaSpecificRule {

    private final String language;

    public JavaSpecificRule(final String language) {
        this.language = language;
    }

    /**
     * STYLE_JAVA.md §2: a recognized Java <b>method definition</b>'s own brace moves to its own
     * line (Allman) whenever it is currently K&amp;R/same-line. Class/interface/enum body braces
     * and every control-flow block already correctly stay K&amp;R via the shared, language-general
     * {@code BlockStructureRule} (`Token.name`/keyword-based classification), and lambda bodies
     * already correctly stay K&amp;R via {@code BlockStructureRule.isLambdaBrace}'s Java branch
     * (preceding token is `->`) -- none of those need touching here. One-liner exception:
     * brace-placement only, same resolution as {@code CppSpecificRule}'s §2 (see STATE.md "§2
     * one-liner scope") -- this method never inspects or changes how many physical lines the body
     * itself spans, it only ever relocates the opening `{`.
     *
     * <p>Candidate signal mirrors {@code CppSpecificRule.isCandidateSignatureName}: the `{` is
     * directly preceded (no comment, no newline in the gap) by a `)` whose matching `(` is itself
     * preceded by a candidate method name (IDENTIFIER, not itself preceded by `new`) -- this
     * naturally excludes every control-flow brace (`if`/`while`/`for`/`switch`/`catch`/`try` precede
     * their `(` with a KEYWORD, never an IDENTIFIER, or have no `(` at all) and every lambda (a
     * lambda's `{` is preceded directly by `->`, never by a bare `)`-after-identifier). A `throws`
     * clause between `)` and `{` (`void foo() throws IOException { ... }`) is a documented,
     * deliberate gap, identical in spirit to C++'s trailing-qualifier gap in its own §2 -- the
     * immediate-predecessor check (`{` directly preceded by `)`) excludes it, and there is no
     * STYLE_JAVA.md worked example to justify guessing past that signal.
     *
     * <p>One Java-specific false positive the C++ version of this signal never has to consider:
     * an enum constant's anonymous constant-body (`RED("red") { ... }`) is structurally identical
     * to a method-definition signature (`)` preceded by a matching `(` preceded by an IDENTIFIER
     * not preceded by `new`) -- Java enum constants never use `new` the way anonymous classes do.
     * Left unguarded, this would wrongly Allman-convert a constant body, violating STYLE_JAVA.md
     * §2's "method definitions only" scope. Guarded via {@code isEnumConstantBody}: a candidate is
     * excluded if its matching `}` is immediately followed (skipping whitespace/comments/newlines)
     * by `,` or `;` -- the universal separator/terminator of an enum constant list, and a shape a
     * real method body's closing `}` can never be followed by in Java (unlike C/C++, a Java method
     * body brace is never followed by a bare `;`). This leaves one documented residual gap: the
     * <i>last</i> constant in an enum with no trailing members and no trailing `;` (legal Java) has
     * its body's `}` followed directly by the enum's own closing `}`, indistinguishable from an
     * ordinary last-member-in-a-body shape without enum-body-context tracking this codebase doesn't
     * do elsewhere either -- left untouched-but-possibly-misconverted in that narrow case, same
     * "no AST, bounded-effort" posture as the rest of this codebase's documented gaps.
     *
     * <p>A `{` already on its own line (gap already contains a NEWLINE) is left untouched --
     * idempotent. The per-occurrence indentation target reuses the closing `)`'s own
     * line-leading indentation, same as {@code CppSpecificRule}'s identical method.
     */
    public String enforceMethodDefinitionAllmanBraceStyle(final List<Token> tokens) {
        final Map<Integer, Integer> gapToBrace = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (!isPunct(tokens.get(i), "{")) {
                continue;
            }
            final int closeParenIdx = prevSignificantIndex(tokens, i);
            if (closeParenIdx < 0 || !isPunct(tokens.get(closeParenIdx), ")")) {
                continue;
            }
            if (!isMethodDefinitionCloseParen(tokens, closeParenIdx)) {
                continue;
            }
            if (hasNewlineOrCommentBetween(tokens, closeParenIdx, i)) {
                continue;
            }
            if (isEnumConstantBody(tokens, i)) {
                continue;
            }
            gapToBrace.put(closeParenIdx + 1, i);
        }

        final StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < tokens.size()) {
            final Integer braceIdx = gapToBrace.get(i);
            if (braceIdx != null) {
                out.append('\n').append(lineIndent(tokens, i - 1));
                i = braceIdx;
            } else {
                out.append(tokens.get(i).text);
                i++;
            }
        }
        return out.toString();
    }

    private boolean isMethodDefinitionCloseParen(final List<Token> tokens, final int closeParenIdx) {
        final int openParenIdx = matchParenBackward(tokens, closeParenIdx);
        return openParenIdx >= 0 && isCandidateMethodName(tokens, openParenIdx);
    }

    /** True iff the token immediately before {@code openIdx} is an IDENTIFIER not itself preceded
     *  by `new` -- the candidate-method-name signal, mirroring
     *  {@code CppSpecificRule.isCandidateSignatureName}. */
    private boolean isCandidateMethodName(final List<Token> tokens, final int openIdx) {
        final int nameIdx = prevSignificantIndex(tokens, openIdx);
        if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
            return false;
        }
        final int beforeName = prevSignificantIndex(tokens, nameIdx);
        return beforeName < 0 || tokens.get(beforeName).type != TokenType.KEYWORD
                || !"new".equals(tokens.get(beforeName).text);
    }

    /** True iff the `{` at {@code braceIdx} is an enum constant's anonymous constant-body --
     *  detected via its matching `}` being immediately followed by `,` or `;`, the universal
     *  enum-constant-list separator/terminator (see this method's caller's doc comment for the
     *  residual gap this heuristic doesn't cover). */
    private boolean isEnumConstantBody(final List<Token> tokens, final int braceIdx) {
        final int closeBraceIdx = matchBraceForward(tokens, braceIdx);
        if (closeBraceIdx < 0) {
            return false;
        }
        final int next = nextSignificantIndex(tokens, closeBraceIdx);
        return next >= 0 && (isPunct(tokens.get(next), ",") || isPunct(tokens.get(next), ";"));
    }

    private boolean hasNewlineOrCommentBetween(final List<Token> tokens, final int fromExclusive, final int toExclusive) {
        for (int i = fromExclusive + 1; i < toExclusive; i++) {
            final TokenType type = tokens.get(i).type;
            if (type == TokenType.NEWLINE || type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) {
                return true;
            }
        }
        return false;
    }

    /** Line-leading whitespace of the physical line containing token {@code idx} -- "" if that
     *  line has no leading whitespace (column-0 start). */
    private String lineIndent(final List<Token> tokens, final int idx) {
        int newlineIdx = -1;
        for (int i = idx; i >= 0; i--) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                newlineIdx = i;
                break;
            }
        }
        final int afterNewline = newlineIdx + 1;
        if (afterNewline < tokens.size() && tokens.get(afterNewline).type == TokenType.WHITESPACE) {
            return tokens.get(afterNewline).text;
        }
        return "";
    }

    private int matchParenBackward(final List<Token> tokens, final int closeIdx) {
        int depth = 0;
        for (int i = closeIdx; i >= 0; i--) {
            if (isPunct(tokens.get(i), ")")) {
                depth++;
            } else if (isPunct(tokens.get(i), "(")) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int matchBraceForward(final List<Token> tokens, final int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < tokens.size(); i++) {
            if (isPunct(tokens.get(i), "{")) {
                depth++;
            } else if (isPunct(tokens.get(i), "}")) {
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
