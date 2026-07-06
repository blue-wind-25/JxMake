/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Kotlin-only STYLE_KOTLIN.md/STYLE_KOTLIN2.md sections flagged "(c)" in `STATE_KOTLIN.md`'s
 * Step 1 scoping table -- none of this is reusable from the shared rule classes. See that table
 * for why each section lands here rather than in a shared file.
 */
public class KotlinSpecificRule {

    private final Lang lang;

    public KotlinSpecificRule(final Lang lang) {
        this.lang = lang;
    }

    /** Tracks, for one open `{`, whether it is an `enum class` body and whether its
     *  mandatory entries/members `;` separator has already been located. */
    private static final class EnumBodyState {
        final boolean isEnumClass;
        boolean separatorFound;

        EnumBodyState(final boolean isEnumClass) {
            this.isEnumClass = isEnumClass;
        }
    }

    /**
     * STYLE_KOTLIN.md §1: strip all optional statement-terminating `;`. The only `;` kept is an
     * `enum class` body's entries/members separator (§2), and only when member declarations
     * actually follow it -- if nothing follows before the closing `}`, that `;` is optional too
     * and gets stripped like any other.
     */
    public List<Token> stripOptionalSemicolons(final List<Token> tokens) {
        final Deque<EnumBodyState> enumBodies = new ArrayDeque<>();
        boolean sawEnum = false;
        boolean sawEnumClass = false;

        // Pass 1: identify which `;` token indices are the mandatory enum separator.
        final boolean[] keep = new boolean[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                continue;
            }
            if (t.type == TokenType.KEYWORD && "enum".equals(t.text)) {
                sawEnum = true;
                continue;
            }
            if (t.type == TokenType.KEYWORD && "class".equals(t.text) && sawEnum) {
                sawEnumClass = true;
                sawEnum = false;
                continue;
            }
            if (isPunct(t, "{")) {
                enumBodies.push(new EnumBodyState(sawEnumClass));
                sawEnumClass = false;
                sawEnum = false;
                continue;
            }
            if (isPunct(t, "}")) {
                if (!enumBodies.isEmpty()) {
                    enumBodies.pop();
                }
                sawEnum = false;
                sawEnumClass = false;
                continue;
            }
            if (isPunct(t, ";")) {
                final EnumBodyState body = enumBodies.isEmpty() ? null : enumBodies.peek();
                if (body != null && body.isEnumClass && !body.separatorFound
                        && hasMoreContentBeforeClose(tokens, i)) {
                    body.separatorFound = true;
                    keep[i] = true;
                }
                sawEnum = false;
                sawEnumClass = false;
                continue;
            }
            // Any other token (the class name, generics, a supertype/constructor clause) is
            // part of the still-open `enum class ... {` header -- leave the pending flags alone
            // so they survive until the body's opening `{` is reached. `enum` on its own (with no
            // `class` yet observed) is cleared only by reaching another `enum`/`class`/`{`/`}`/`;`.
        }

        // Pass 2: rebuild the list, dropping every `;` not marked to keep.
        final List<Token> result = new ArrayList<>(tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, ";") && !keep[i]) {
                continue;
            }
            result.add(t);
        }
        return result;
    }

    /** True if there is at least one non-gap, non-`}` token between {@code semicolonIdx} and
     *  the `}` that closes the current brace depth -- i.e. member declarations actually follow. */
    private boolean hasMoreContentBeforeClose(final List<Token> tokens, final int semicolonIdx) {
        for (int j = semicolonIdx + 1; j < tokens.size(); j++) {
            final Token t = tokens.get(j);
            if (isGapToken(t)) {
                continue;
            }
            return !isPunct(t, "}");
        }
        return false;
    }
}
