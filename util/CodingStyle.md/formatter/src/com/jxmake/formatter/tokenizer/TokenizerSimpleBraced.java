/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.tokenizer;

/**
 * Shared base for the "SimpleBraced" family: languages that are brace/bracket-delimited like the
 * curly-imperative family, but have no keywords, control flow, or preprocessor -- currently
 * JSON/JSON5 ({@link JsonTokenizer}), with CSS ({@code STYLE_DATA_FORMATS.md} §3) as the family's
 * other planned member. Holds only what both concretely share today: `/* *\/`-style block comment
 * scanning (JSON5 and CSS both use it; JSON5 additionally has `//` line comments, which stay in
 * {@link JsonTokenizer} since CSS has no line-comment syntax). Everything else
 * (string/number/selector/property grammar) is family-member-specific and lives in the concrete
 * sibling.
 */
public abstract class TokenizerSimpleBraced extends TokenizerCore {

    protected Token emitBlockComment() {
        final int start = pos;
        pos += 2;
        while (pos < length && !(source.charAt(pos) == '*' && peek(1) == '/')) {
            pos++;
        }
        if (pos < length) {
            pos += 2;
        }
        return new Token(TokenType.COMMENT_BLOCK, source.substring(start, pos), 0, 0, null);
    }
}
