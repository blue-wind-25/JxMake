package com.jxmake.formatter.gdr;

/**
 * One token produced by {@link GdrTokenizer}. {@code line} is the
 * 0-based source line the token starts on; a token spanning multiple
 * physical lines (a block comment, a raw string, a continued
 * preprocessor directive) carries only its start line -- callers that
 * need per-line depth must count embedded {@code '\n'} occurrences in
 * {@code text} themselves.
 */
public final class GdrToken {

    public final GdrTokenType type;
    public final String       text;
    public final int          line;

    public GdrToken(GdrTokenType type, String text, int line)
    {
        this.type = type;
        this.text = text;
        this.line = line;
    }

    @Override
    public String toString()
    {
        return type + "@" + line + "(" + text.length() + " chars)";
    }

} // class GdrToken
