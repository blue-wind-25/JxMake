/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.tokenizer;

public class TokenizerCore {

    public enum TokenType {
        KEYWORD,
        IDENTIFIER,
        NUMBER,
        STRING,
        CHAR,
        OP,
        PUNCT,
        COMMENT_LINE,
        COMMENT_BLOCK,
        WHITESPACE,
        NEWLINE,
        PREPROCESSOR,        // C/C++ only — opaque single-line #-directive
        MACRO_DEF,           // C/C++ only — opaque multiline #define with \ continuations
        ANGLE_BRACKET_OPEN,  // generic/template context
        ANGLE_BRACKET_CLOSE  // generic/template context
    }

    public static final class Token {
        public final TokenType type;
        public final String text;

        public Token(TokenType type, String text) {
            this.type = type;
            this.text = text;
        }
    }
}
