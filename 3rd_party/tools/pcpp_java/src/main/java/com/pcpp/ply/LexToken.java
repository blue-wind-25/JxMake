// Translated by Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single token produced by the lexer.
 * Mirrors PLY's LexToken class from lex.py.
 */
public class LexToken {
    public String type;
    public Object value;
    public int lineno;
    public int lexpos;

    // pcpp extension fields
    public String source;
    public List<String> expanded_from;

    /**
     * Back-reference to the lexer that produced this token.
     * Set during function-rule processing so callbacks can call
     * lexer.skip(), lexer.begin(), etc. Mirrors Python PLY's
     * {@code tok.lexer = self} assignment in Lexer.token().
     */
    public Lexer lexer;

    public LexToken() {
        this.expanded_from = null;
    }

    public LexToken(String type, Object value, int lineno, int lexpos) {
        this.type = type;
        this.value = value;
        this.lineno = lineno;
        this.lexpos = lexpos;
        this.expanded_from = null;
    }

    /** Deep-copy constructor (mirrors copy.copy behaviour used throughout pcpp). */
    public LexToken copy() {
        LexToken t = new LexToken();
        t.type = this.type;
        t.value = this.value;
        t.lineno = this.lineno;
        t.lexpos = this.lexpos;
        t.source = this.source;
        t.lexer = this.lexer;
        t.expanded_from = this.expanded_from != null ? new ArrayList<>(this.expanded_from) : null;
        return t;
    }

    @Override
    public String toString() {
        return String.format("LexToken(%s,%s,%d,%d)", type, value, lineno, lexpos);
    }
}
