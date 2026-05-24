// Translated using Claude Sonnet 4.6
package com.pcpp.parser;

/**
 * Token type constants used throughout the C preprocessor.
 * Mirrors the tokens tuple in parser.py.
 */
public final class CppTokens {
    private CppTokens() {}

    public static final String CPP_ID         = "CPP_ID";
    public static final String CPP_INTEGER    = "CPP_INTEGER";
    public static final String CPP_FLOAT      = "CPP_FLOAT";
    public static final String CPP_STRING     = "CPP_STRING";
    public static final String CPP_CHAR       = "CPP_CHAR";
    public static final String CPP_WS         = "CPP_WS";
    public static final String CPP_LINECONT   = "CPP_LINECONT";
    public static final String CPP_COMMENT1   = "CPP_COMMENT1";
    public static final String CPP_COMMENT2   = "CPP_COMMENT2";
    public static final String CPP_POUND      = "CPP_POUND";
    public static final String CPP_DPOUND     = "CPP_DPOUND";
    public static final String CPP_PLUS       = "CPP_PLUS";
    public static final String CPP_MINUS      = "CPP_MINUS";
    public static final String CPP_STAR       = "CPP_STAR";
    public static final String CPP_FSLASH     = "CPP_FSLASH";
    public static final String CPP_PERCENT    = "CPP_PERCENT";
    public static final String CPP_BAR        = "CPP_BAR";
    public static final String CPP_AMPERSAND  = "CPP_AMPERSAND";
    public static final String CPP_TILDE      = "CPP_TILDE";
    public static final String CPP_HAT        = "CPP_HAT";
    public static final String CPP_LESS       = "CPP_LESS";
    public static final String CPP_GREATER    = "CPP_GREATER";
    public static final String CPP_EQUAL      = "CPP_EQUAL";
    public static final String CPP_EXCLAMATION= "CPP_EXCLAMATION";
    public static final String CPP_QUESTION   = "CPP_QUESTION";
    public static final String CPP_LPAREN     = "CPP_LPAREN";
    public static final String CPP_RPAREN     = "CPP_RPAREN";
    public static final String CPP_LBRACKET   = "CPP_LBRACKET";
    public static final String CPP_RBRACKET   = "CPP_RBRACKET";
    public static final String CPP_LCURLY     = "CPP_LCURLY";
    public static final String CPP_RCURLY     = "CPP_RCURLY";
    public static final String CPP_DOT        = "CPP_DOT";
    public static final String CPP_COMMA      = "CPP_COMMA";
    public static final String CPP_SEMICOLON  = "CPP_SEMICOLON";
    public static final String CPP_COLON      = "CPP_COLON";
    public static final String CPP_BSLASH     = "CPP_BSLASH";
    public static final String CPP_SQUOTE     = "CPP_SQUOTE";
    public static final String CPP_DQUOTE     = "CPP_DQUOTE";

    public static final String CPP_DEREFERENCE    = "CPP_DEREFERENCE";
    public static final String CPP_MINUSEQUAL     = "CPP_MINUSEQUAL";
    public static final String CPP_MINUSMINUS     = "CPP_MINUSMINUS";
    public static final String CPP_LSHIFT         = "CPP_LSHIFT";
    public static final String CPP_LESSEQUAL      = "CPP_LESSEQUAL";
    public static final String CPP_RSHIFT         = "CPP_RSHIFT";
    public static final String CPP_GREATEREQUAL   = "CPP_GREATEREQUAL";
    public static final String CPP_LOGICALOR      = "CPP_LOGICALOR";
    public static final String CPP_OREQUAL        = "CPP_OREQUAL";
    public static final String CPP_LOGICALAND     = "CPP_LOGICALAND";
    public static final String CPP_ANDEQUAL       = "CPP_ANDEQUAL";
    public static final String CPP_EQUALITY       = "CPP_EQUALITY";
    public static final String CPP_INEQUALITY     = "CPP_INEQUALITY";
    public static final String CPP_XOREQUAL       = "CPP_XOREQUAL";
    public static final String CPP_MULTIPLYEQUAL  = "CPP_MULTIPLYEQUAL";
    public static final String CPP_DIVIDEEQUAL    = "CPP_DIVIDEEQUAL";
    public static final String CPP_PLUSEQUAL      = "CPP_PLUSEQUAL";
    public static final String CPP_PLUSPLUS       = "CPP_PLUSPLUS";
    public static final String CPP_PERCENTEQUAL   = "CPP_PERCENTEQUAL";
    public static final String CPP_LSHIFTEQUAL    = "CPP_LSHIFTEQUAL";
    public static final String CPP_RSHIFTEQUAL    = "CPP_RSHIFTEQUAL";

    public static final String LITERALS = "+-*/%|&~^<>=!?()[]{}.,;:\\'\"";
}
