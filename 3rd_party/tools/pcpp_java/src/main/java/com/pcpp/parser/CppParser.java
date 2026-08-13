// Translated using Claude Sonnet 4.6
package com.pcpp.parser;

import java.util.regex.*;

/**
 * Static helpers from parser.py: trigraph replacement and utility functions.
 * Mirrors the module-level code in parser.py.
 */
public final class CppParser {

    private CppParser() {}

    // Trigraph replacement (mirrors trigraph() in parser.py)
    private static final Pattern TRIGRAPH_PAT = Pattern.compile("\\?\\?[=/\\'()!<>\\-]");
    private static java.util.Map<Character, Character> TRIGRAPH_REP = new java.util.HashMap<>();
    static {
        TRIGRAPH_REP.put('=',  '#');
        TRIGRAPH_REP.put('/',  '\\');
        TRIGRAPH_REP.put('\'', '^');
        TRIGRAPH_REP.put('(',  '[');
        TRIGRAPH_REP.put(')',  ']');
        TRIGRAPH_REP.put('!',  '|');
        TRIGRAPH_REP.put('<',  '{');
        TRIGRAPH_REP.put('>',  '}');
        TRIGRAPH_REP.put('-',  '~');
    }

    /**
     * Replace trigraph sequences (??=, ??/, etc.) in the input string.
     * Mirrors trigraph() from parser.py.
     */
    public static String trigraph(String input)
    {
        Matcher      m  = TRIGRAPH_PAT.matcher(input);
        StringBuffer sb = new StringBuffer();
        while( m.find() ) {
            char last = m.group().charAt(2);
            char rep  = TRIGRAPH_REP.getOrDefault(last, last);
            m.appendReplacement( sb, Matcher.quoteReplacement( String.valueOf(rep) ) );
        }
        m.appendTail(sb);

        return sb.toString();
    }

    /** Create the default C preprocessor lexer. Mirrors default_lexer() from parser.py. */
    public static CppLexer defaultLexer()
    {
        return new CppLexer();
    }

} // class CppParser
