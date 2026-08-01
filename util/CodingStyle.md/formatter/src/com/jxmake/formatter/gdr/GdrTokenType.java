package com.jxmake.formatter.gdr;

/**
 * Token kinds produced by {@link GdrTokenizer}. Deliberately narrow --
 * scoped only to what the GDR pre-pass reindenter needs (structural
 * bracket depth plus exclusion-zone recognition), not a general-purpose
 * lexer.
 */
public enum GdrTokenType {
    BRACE_OPEN,
    BRACE_CLOSE,
    PAREN_OPEN,
    PAREN_CLOSE,
    BRACKET_OPEN,
    BRACKET_CLOSE,
    STRING,
    CHAR,
    LINE_COMMENT,
    BLOCK_COMMENT,
    PREPROCESSOR,
    NEWLINE,
    TEXT
}
