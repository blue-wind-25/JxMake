// Translated using Claude Sonnet 4.6
package com.pcpp.parser;

import com.pcpp.ply.LexToken;

import java.util.*;
import java.util.regex.*;

/**
 * C preprocessor lexer.  Mirrors the token rules defined in parser.py.
 *
 * In Python PLY the rules are functions/strings named t_XXX; here they are
 * implemented as an ordered list of regex rules fed into the Lexer engine.
 * Rule priority follows PLY conventions: multi-character compound operators
 * come before single-character ones.
 */
public class CppLexer {

    // -----------------------------------------------------------------------
    // Regex patterns (from parser.py t_XXX definitions)
    // Multi-char compound operators MUST come before single-char ones.
    // -----------------------------------------------------------------------

    // Whitespace (not past end of line)
    private static final String PAT_WS            = "[ \\t]+|\\n";
    // Line continuation  \\[ \t]*\n
    private static final String PAT_LINECONT      = "\\\\[ \\t]*\\n";
    // ##
    private static final String PAT_DPOUND        = "##";
    // Compound operators (longest first)
    private static final String PAT_LSHIFTEQUAL   = "<<=";
    private static final String PAT_RSHIFTEQUAL   = ">>=";
    private static final String PAT_LSHIFT        = "<<";
    private static final String PAT_RSHIFT        = ">>";
    private static final String PAT_LESSEQUAL     = "<=";
    private static final String PAT_GREATEREQUAL  = ">=";
    private static final String PAT_LOGICALAND    = "&&";
    private static final String PAT_LOGICALOR     = "\\|\\|";
    private static final String PAT_ANDEQUAL      = "&=";
    private static final String PAT_OREQUAL       = "\\|=";
    private static final String PAT_XOREQUAL      = "\\^=";
    private static final String PAT_EQUALITY      = "==";
    private static final String PAT_INEQUALITY    = "!=";
    private static final String PAT_MULTIPLYEQUAL = "\\*=";
    private static final String PAT_DIVIDEEQUAL   = "/=";
    private static final String PAT_PLUSEQUAL     = "\\+=";
    private static final String PAT_PLUSPLUS      = "\\+\\+";
    private static final String PAT_MINUSEQUAL    = "-=";
    private static final String PAT_MINUSMINUS    = "--";
    private static final String PAT_PERCENTEQUAL  = "%=";
    private static final String PAT_DEREFERENCE   = "->";
    // Single-char operators
    private static final String PAT_POUND         = "#";
    private static final String PAT_PLUS          = "\\+";
    private static final String PAT_MINUS         = "-";
    private static final String PAT_STAR          = "\\*";
    private static final String PAT_FSLASH        = "/";
    private static final String PAT_PERCENT       = "%";
    private static final String PAT_BAR           = "\\|";
    private static final String PAT_AMPERSAND     = "&";
    private static final String PAT_TILDE         = "~";
    private static final String PAT_HAT           = "\\^";
    private static final String PAT_LESS          = "<";
    private static final String PAT_GREATER       = ">";
    private static final String PAT_EQUAL         = "=";
    private static final String PAT_EXCL          = "!";
    private static final String PAT_QUESTION      = "\\?";
    private static final String PAT_LPAREN        = "\\(";
    private static final String PAT_RPAREN        = "\\)";
    private static final String PAT_LBRACKET      = "\\[";
    private static final String PAT_RBRACKET      = "\\]";
    private static final String PAT_LCURLY        = "\\{";
    private static final String PAT_RCURLY        = "\\}";
    private static final String PAT_DOT           = "\\.";
    private static final String PAT_COMMA         = ",";
    private static final String PAT_SEMICOLON     = ";";
    private static final String PAT_COLON         = ":";
    private static final String PAT_BSLASH        = "\\\\";
    private static final String PAT_SQUOTE        = "'";
    private static final String PAT_DQUOTE        = "\"";
    // Identifiers
    private static final String PAT_ID            = "[A-Za-z_][\\w_]*";
    // Integer literal
    private static final String PAT_INTEGER       =
        "(((0x|0X)[0-9a-fA-F]+)|\\d+)([uU][lL]|[lL][uU]|[uU]|[lL])?";
    // Float literal
    private static final String PAT_FLOAT         =
        "((\\d+)(\\.\\d+)(e(\\+|-)?\\d+)?|(\\d+)e(\\+|-)?\\d+)([lLfF])?";
    // String literal  "..." (may contain escaped chars or line continuations)
    // *+ (possessive) uses Curly (iterative) instead of Loop (recursive) in Java's
    // regex engine, preventing StackOverflowError on long string literals.
    private static final String PAT_STRING   = "\"(?:[^\"\\\\\\n]|\\\\.)*+\"";
    // Char literal  'c' or L'c'
    private static final String PAT_CHAR     = "(?:L)?'(?:[^'\\\\\\n]|\\\\.)*+'";
    // Block comment — possessive *+ avoids Java regex stack overflow on long comments.
    private static final String PAT_COMMENT1 = "/\\*(?:[^*]|\\*+(?!/))*+\\*/";
    // Line comment
    private static final String PAT_COMMENT2 = "//[^\\n]*";

    // -----------------------------------------------------------------------
    // Master pattern
    // Each alternative is a named group G<n>.  Order matters for priority.
    // -----------------------------------------------------------------------

    private static final String[] RULE_NAMES = {
        CppTokens.CPP_COMMENT1, // longest, must come first
        CppTokens.CPP_COMMENT2,
        CppTokens.CPP_LINECONT,
        CppTokens.CPP_WS,
        CppTokens.CPP_STRING,
        CppTokens.CPP_CHAR,
        CppTokens.CPP_FLOAT,
        CppTokens.CPP_INTEGER,
        CppTokens.CPP_ID,
        CppTokens.CPP_DPOUND,
        CppTokens.CPP_LSHIFTEQUAL,
        CppTokens.CPP_RSHIFTEQUAL,
        CppTokens.CPP_LSHIFT,
        CppTokens.CPP_RSHIFT,
        CppTokens.CPP_LESSEQUAL,
        CppTokens.CPP_GREATEREQUAL,
        CppTokens.CPP_LOGICALAND,
        CppTokens.CPP_LOGICALOR,
        CppTokens.CPP_ANDEQUAL,
        CppTokens.CPP_OREQUAL,
        CppTokens.CPP_XOREQUAL,
        CppTokens.CPP_EQUALITY,
        CppTokens.CPP_INEQUALITY,
        CppTokens.CPP_MULTIPLYEQUAL,
        CppTokens.CPP_DIVIDEEQUAL,
        CppTokens.CPP_PLUSEQUAL,
        CppTokens.CPP_PLUSPLUS,
        CppTokens.CPP_MINUSEQUAL,
        CppTokens.CPP_MINUSMINUS,
        CppTokens.CPP_PERCENTEQUAL,
        CppTokens.CPP_DEREFERENCE,
        CppTokens.CPP_POUND,
        CppTokens.CPP_PLUS,
        CppTokens.CPP_MINUS,
        CppTokens.CPP_STAR,
        CppTokens.CPP_FSLASH,
        CppTokens.CPP_PERCENT,
        CppTokens.CPP_BAR,
        CppTokens.CPP_AMPERSAND,
        CppTokens.CPP_TILDE,
        CppTokens.CPP_HAT,
        CppTokens.CPP_LESS,
        CppTokens.CPP_GREATER,
        CppTokens.CPP_EQUAL,
        CppTokens.CPP_EXCLAMATION,
        CppTokens.CPP_QUESTION,
        CppTokens.CPP_LPAREN,
        CppTokens.CPP_RPAREN,
        CppTokens.CPP_LBRACKET,
        CppTokens.CPP_RBRACKET,
        CppTokens.CPP_LCURLY,
        CppTokens.CPP_RCURLY,
        CppTokens.CPP_DOT,
        CppTokens.CPP_COMMA,
        CppTokens.CPP_SEMICOLON,
        CppTokens.CPP_COLON,
        CppTokens.CPP_BSLASH,
        CppTokens.CPP_SQUOTE,
        CppTokens.CPP_DQUOTE,
    };

    private static final String[] RULE_PATTERNS = {
        PAT_COMMENT1,
        PAT_COMMENT2,
        PAT_LINECONT,
        PAT_WS,
        PAT_STRING,
        PAT_CHAR,
        PAT_FLOAT,
        PAT_INTEGER,
        PAT_ID,
        PAT_DPOUND,
        PAT_LSHIFTEQUAL,
        PAT_RSHIFTEQUAL,
        PAT_LSHIFT,
        PAT_RSHIFT,
        PAT_LESSEQUAL,
        PAT_GREATEREQUAL,
        PAT_LOGICALAND,
        PAT_LOGICALOR,
        PAT_ANDEQUAL,
        PAT_OREQUAL,
        PAT_XOREQUAL,
        PAT_EQUALITY,
        PAT_INEQUALITY,
        PAT_MULTIPLYEQUAL,
        PAT_DIVIDEEQUAL,
        PAT_PLUSEQUAL,
        PAT_PLUSPLUS,
        PAT_MINUSEQUAL,
        PAT_MINUSMINUS,
        PAT_PERCENTEQUAL,
        PAT_DEREFERENCE,
        PAT_POUND,
        PAT_PLUS,
        PAT_MINUS,
        PAT_STAR,
        PAT_FSLASH,
        PAT_PERCENT,
        PAT_BAR,
        PAT_AMPERSAND,
        PAT_TILDE,
        PAT_HAT,
        PAT_LESS,
        PAT_GREATER,
        PAT_EQUAL,
        PAT_EXCL,
        PAT_QUESTION,
        PAT_LPAREN,
        PAT_RPAREN,
        PAT_LBRACKET,
        PAT_RBRACKET,
        PAT_LCURLY,
        PAT_RCURLY,
        PAT_DOT,
        PAT_COMMA,
        PAT_SEMICOLON,
        PAT_COLON,
        PAT_BSLASH,
        PAT_SQUOTE,
        PAT_DQUOTE,
    };

    // Pattern to strip line-continuations from string literals
    private static final Pattern  STRING_LINECONT_PAT = Pattern.compile( "\\\\[ \\t]*\\n" );

    private static final Pattern  MASTER;
    private static final String[] GROUP_NAMES         = new String[RULE_NAMES.length];
    static {
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < RULE_NAMES.length; i++ ) {
            if( i > 0 ) sb.append( '|' );
            String gname = "G" + i;
            GROUP_NAMES[i] = gname;
            sb.append( "(?<" ).append( gname ).append( '>' );
            sb.append( RULE_PATTERNS[i] );
            sb.append( ')' );
        }
        MASTER = Pattern.compile( sb.toString(), Pattern.DOTALL );
    }

    // -----------------------------------------------------------------------
    // Instance state
    // -----------------------------------------------------------------------
    private String  inputData;
    private int     lexpos;
    private int     lexlen;
    public int      lineno;
    private Matcher masterMatcher;

    public CppLexer()
    {
        lineno = 1;
    }

    public void input( String text )
    {
        this.inputData     = text;
        this.lexpos        = 0;
        this.lexlen        = text.length();
        this.lineno        = 1;
        this.masterMatcher = MASTER.matcher( text );
    }

    /**
     * Return the next token, or null at EOF.
     * Mirrors PLY Lexer.token() with the custom t_XXX functions from parser.py.
     */
    public LexToken token()
    {
        if( inputData == null || lexpos >= lexlen ) return null;

        Matcher m = masterMatcher;
        while( lexpos < lexlen ) {
            m.region( lexpos, lexlen );
            if( !m.lookingAt() ) {
                // Error: consume one character and return it as a token of its own type
                char     c   = inputData.charAt( lexpos );
                LexToken tok = new LexToken();
                tok.type   = String.valueOf( c );
                tok.value  = String.valueOf( c );
                tok.lineno = lineno;
                tok.lexpos = lexpos;
                lexpos++;
                return tok;
            }

            // Find which group matched
            for( int i = 0; i < RULE_NAMES.length; i++ ) {
                String matched   = m.group( GROUP_NAMES[i] );
                if( matched == null ) continue;

                String   tokType = RULE_NAMES[i];
                LexToken tok     = new LexToken();
                tok.type   = tokType;
                tok.value  = matched;
                tok.lineno = lineno;
                tok.lexpos = lexpos;
                lexpos    += matched.length();

                // Apply per-token actions (mirrors the t_XXX function bodies in parser.py)
                switch( tokType ) {
                    case CppTokens.CPP_WS:
                        lineno += countNewlines( matched );
                        return tok;

                    case CppTokens.CPP_LINECONT:
                        // Strip the leading backslash and trailing newline; keep only middle whitespace
                        tok.value = matched.substring( 1, matched.length() - 1 );
                        lineno   += 1;
                        return tok;

                    case CppTokens.CPP_STRING:
                        // Collapse line continuations inside the string literal
                        Matcher      lc   = STRING_LINECONT_PAT.matcher( matched );
                        StringBuffer sb2  = new StringBuffer();
                        int          subs = 0;
                        while( lc.find() ) { lc.appendReplacement( sb2, "" ); subs++; }
                        lc.appendTail( sb2 );
                        tok.value = sb2.toString();
                        lineno   += subs + countNewlines( tok.value.toString() );
                        return tok;

                    case CppTokens.CPP_CHAR:
                        lineno += countNewlines( matched );
                        return tok;

                    case CppTokens.CPP_COMMENT1:
                        lineno += countNewlines( matched );
                        return tok;

                    case CppTokens.CPP_COMMENT2:
                        return tok;

                    default:
                        return tok;
                } // switch
            }
        }
        return null;
    }

    /** Return a clone with independent cursor but sharing no mutable state. */
    public CppLexer clone()
    {
        CppLexer c = new CppLexer();
        c.inputData     = this.inputData;
        c.lexpos        = this.lexpos;
        c.lexlen        = this.lexlen;
        c.lineno        = this.lineno;
        c.masterMatcher = this.inputData != null ? MASTER.matcher( this.inputData ) : null;
        return c;
    }

    /** Skip n characters (used in error handlers). */
    public void skip( int n )
    {
        lexpos = Math.min( lexpos + n, lexlen );
    }

    private static int countNewlines( String s )
    {
        int count = 0;
        for( int i = 0; i < s.length(); i++ )
            if( s.charAt( i ) == '\n' ) count++;
        return count;
    }
} // class CppLexer
