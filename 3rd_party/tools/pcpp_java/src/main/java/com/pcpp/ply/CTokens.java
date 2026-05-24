// Translated using Claude Sonnet 4.6
// ----------------------------------------------------------------------
// CTokens.java
//
// Token specifications for symbols in ANSI C and C++. This file is
// meant to be used as a library in other tokenizers.
// Translated from ctokens.py (PLY project).
// ----------------------------------------------------------------------
package com.pcpp.ply;

public class CTokens {

    // All token names
    public static final String[] TOKENS = {
        // Literals (identifier, integer constant, float constant, string constant, char const)
        "ID", "TYPEID", "INTEGER", "FLOAT", "STRING", "CHARACTER",

        // Operators (+,-,*,/,%,|,&,~,^,<<,>>, ||, &&, !, <, <=, >, >=, ==, !=)
        "PLUS", "MINUS", "TIMES", "DIVIDE", "MODULO",
        "OR", "AND", "NOT", "XOR", "LSHIFT", "RSHIFT",
        "LOR", "LAND", "LNOT",
        "LT", "LE", "GT", "GE", "EQ", "NE",

        // Assignment (=, *=, /=, %=, +=, -=, <<=, >>=, &=, ^=, |=)
        "EQUALS", "TIMESEQUAL", "DIVEQUAL", "MODEQUAL", "PLUSEQUAL", "MINUSEQUAL",
        "LSHIFTEQUAL", "RSHIFTEQUAL", "ANDEQUAL", "XOREQUAL", "OREQUAL",

        // Increment/decrement (++,--)
        "INCREMENT", "DECREMENT",

        // Structure dereference (->)
        "ARROW",

        // Ternary operator (?)
        "TERNARY",

        // Delimiters ( ) [ ] { } , . ; :
        "LPAREN", "RPAREN",
        "LBRACKET", "RBRACKET",
        "LBRACE", "RBRACE",
        "COMMA", "PERIOD", "SEMI", "COLON",

        // Ellipsis (...)
        "ELLIPSIS",
    };

    // Operators
    public static final String T_PLUS             = "\\+";
    public static final String T_MINUS            = "-";
    public static final String T_TIMES            = "\\*";
    public static final String T_DIVIDE           = "/";
    public static final String T_MODULO           = "%";
    public static final String T_OR               = "\\|";
    public static final String T_AND              = "&";
    public static final String T_NOT              = "~";
    public static final String T_XOR              = "\\^";
    public static final String T_LSHIFT           = "<<";
    public static final String T_RSHIFT           = ">>";
    public static final String T_LOR              = "\\|\\|";
    public static final String T_LAND             = "&&";
    public static final String T_LNOT             = "!";
    public static final String T_LT               = "<";
    public static final String T_GT               = ">";
    public static final String T_LE               = "<=";
    public static final String T_GE               = ">=";
    public static final String T_EQ               = "==";
    public static final String T_NE               = "!=";

    // Assignment operators
    public static final String T_EQUALS           = "=";
    public static final String T_TIMESEQUAL       = "\\*=";
    public static final String T_DIVEQUAL         = "/=";
    public static final String T_MODEQUAL         = "%=";
    public static final String T_PLUSEQUAL        = "\\+=";
    public static final String T_MINUSEQUAL       = "-=";
    public static final String T_LSHIFTEQUAL      = "<<=";
    public static final String T_RSHIFTEQUAL      = ">>=";
    public static final String T_ANDEQUAL         = "&=";
    public static final String T_OREQUAL          = "\\|=";
    public static final String T_XOREQUAL         = "\\^=";

    // Increment/decrement
    public static final String T_INCREMENT        = "\\+\\+";
    public static final String T_DECREMENT        = "--";

    // ->
    public static final String T_ARROW            = "->";

    // ?
    public static final String T_TERNARY          = "\\?";

    // Delimiters
    public static final String T_LPAREN           = "\\(";
    public static final String T_RPAREN           = "\\)";
    public static final String T_LBRACKET         = "\\[";
    public static final String T_RBRACKET         = "\\]";
    public static final String T_LBRACE           = "\\{";
    public static final String T_RBRACE           = "\\}";
    public static final String T_COMMA            = ",";
    public static final String T_PERIOD           = "\\.";
    public static final String T_SEMI             = ";";
    public static final String T_COLON            = ":";
    public static final String T_ELLIPSIS         = "\\.\\.\\.";

    // Identifiers
    public static final String T_ID               = "[A-Za-z_][A-Za-z0-9_]*";

    // Integer literal
    public static final String T_INTEGER          = "\\d+([uU]|[lL]|[uU][lL]|[lL][uU])?";

    // Floating literal
    public static final String T_FLOAT            = "((\\d+)(\\.\\d+)(e(\\+|-)?(\\d+))? | (\\d+)e(\\+|-)?(\\d+))([lL]|[fF])?";

    // String literal
    public static final String T_STRING           = "\"([^\\\\\\n]|(\\\\.))*?\"";

    // Character constant 'c' or L'c'
    public static final String T_CHARACTER        = "(L)?'([^\\\\\\n]|(\\\\.))*?'";

    // Comment (C-Style) — note: in ply, this was a function; regex is the docstring
    public static final String T_COMMENT          = "/\\*(.|\\n)*?\\*/";

    // Comment (C++-Style)
    public static final String T_CPPCOMMENT       = "//.*\\n";
}
