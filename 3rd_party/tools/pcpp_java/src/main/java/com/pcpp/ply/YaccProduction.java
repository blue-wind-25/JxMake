// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper passed to each grammar action function during LALR parsing.
 *
 * Mirrors PLY's YaccProduction from yacc.py.
 *
 * In Python, grammar actions index into the production with p[0], p[1], ...,
 * where p[0] is the left-hand side (the result) and p[1..n] are the matched
 * symbols.  Negative indices look into the parser's symbol stack.
 *
 * In Java:
 *   - Use {@link #get(int)} / {@link #set(int, Object)} for Python-style p[n] access.
 *   - Grammar action lambdas receive a YaccProduction and call p.set(0, result)
 *     to set the left-hand side value.
 *   - The underlying slice and stack contain {@link YaccSymbol} entries.
 */
public class YaccProduction {
    /**
     * The production slice: slice.get(0) is the LHS symbol (value written by action),
     * slice.get(1..n) are the matched RHS symbols.
     * Package-private so LRParser can swap it each reduction.
     */
    List<YaccSymbol> slice;

    /**
     * The full parser symbol stack. Used for negative-index lookups (p[-1], etc.).
     * Package-private so LRParser can set it.
     */
    List<YaccSymbol> stack;

    /** The lexer that drove the parse (may be null). Set by LRParser. */
    public Object    lexer;

    /** Back-reference to the LRParser instance. Set by LRParser. */
    public LRParser  parser;

    /** Construct with an empty slice; slice/stack set later by the parser. */
    public YaccProduction( List<YaccSymbol> slice )
    {
        this.slice = slice;
    }

    public YaccProduction( List<YaccSymbol> slice, List<YaccSymbol> stack )
    {
        this.slice = slice;
        this.stack = stack;
    }

    // -------------------------------------------------------------------------
    // Core index-based access (mirrors Python p[n])
    // -------------------------------------------------------------------------

    /**
     * Return the value of the nth symbol in the production.
     *
     * <ul>
     *   <li>n == 0  → LHS value (initially null; the action sets this)</li>
     *   <li>n > 0   → nth RHS symbol value</li>
     *   <li>n < 0   → look into the symbol stack (Python negative indexing)</li>
     * </ul>
     */
    public Object get( int n )
    {
        if( n >= 0 ) {
            return slice.get( n ).value;
        }
        else {
            int idx = stack.size() + n;
            return stack.get( idx ).value;
        }
    }

    /**
     * Set the value of the nth symbol in the production.
     * Typically only n == 0 is used (to set the result).
     */
    public void set( int n, Object value )
    {
        slice.get( n ).value = value;
    }

    // -------------------------------------------------------------------------
    // Convenience helpers
    // -------------------------------------------------------------------------

    /** Convenience: set the result (p[0]). */
    public void setResult( Object value )
    {
        slice.get( 0 ).value = value;
    }

    /** Convenience: get the result (p[0]). */
    public Object getResult()
    {
        return slice.get( 0 ).value;
    }

    /** Return the token at position index as a {@link LexToken} (null if not a token). */
    public LexToken getToken( int index )
    {
        Object v = get( index );
        return v instanceof LexToken ? (LexToken) v : null;
    }

    /**
     * Return the number of symbols in the slice (including the LHS at index 0).
     * Mirrors Python len(p).
     */
    public int size()
    {
        return slice.size();
    }

    /**
     * Get a sublist of values for the slice indices i (inclusive) .. j (exclusive).
     * Mirrors Python p[i:j].
     */
    public List<Object> getSlice( int i, int j )
    {
        List<Object> result = new ArrayList<>();
        int          end    = Math.min( j, slice.size() );
        for( int k = i; k < end; k++ ) result.add( slice.get( k ).value );
        return result;
    }

    // -------------------------------------------------------------------------
    // Position / span helpers
    // -------------------------------------------------------------------------

    /**
     * Return the line number of the nth symbol (0 if not set).
     * Mirrors Python p.lineno(n).
     */
    public int lineno( int n )
    {
        YaccSymbol s = slice.get( n );
        return s.hasLineno ? s.lineno : 0;
    }

    /**
     * Set the line number of the nth symbol.
     * Mirrors Python p.set_lineno(n, lineno).
     */
    public void setLineno( int n, int lineno )
    {
        YaccSymbol s = slice.get( n );
        s.lineno    = lineno;
        s.hasLineno = true;
    }

    /**
     * Return the (startLine, endLine) span for the nth symbol.
     * Mirrors Python p.linespan(n).
     */
    public int[] linespan( int n )
    {
        YaccSymbol s         = slice.get( n );
        int        startLine = s.hasLineno ? s.lineno : 0;
        int        endLine   = s.hasLineno ? s.endlineno : startLine;
        return new int[] {
                   startLine, endLine
        };
    }

    /**
     * Return the lex position of the nth symbol (0 if not set).
     * Mirrors Python p.lexpos(n).
     */
    public int lexpos( int n )
    {
        YaccSymbol s = slice.get( n );
        return s.hasLexpos ? s.lexpos : 0;
    }

    /**
     * Set the lex position of the nth symbol.
     * Mirrors Python p.set_lexpos(n, lexpos).
     */
    public void setLexpos( int n, int lexpos )
    {
        YaccSymbol s = slice.get( n );
        s.lexpos    = lexpos;
        s.hasLexpos = true;
    }

    /**
     * Return the (startLexpos, endLexpos) span for the nth symbol.
     * Mirrors Python p.lexspan(n).
     */
    public int[] lexspan( int n )
    {
        YaccSymbol s        = slice.get( n );
        int        startPos = s.hasLexpos ? s.lexpos : 0;
        int        endPos   = s.hasLexpos ? s.endlexpos : startPos;
        return new int[] {
                   startPos, endPos
        };
    }

    // -------------------------------------------------------------------------
    // Error signalling
    // -------------------------------------------------------------------------

    /**
     * Signal a syntax error from within a grammar action.
     * Mirrors the Python p.error() method which raises SyntaxError.
     * Caught by the LRParser parse loop.
     */
    public void error()
    {
        throw new ParseSyntaxError();
    }

    /**
     * Internal exception used to signal a syntax error from a grammar action.
     * Caught by the LRParser parse loop (mirrors Python's SyntaxError).
     */
    static class ParseSyntaxError extends RuntimeException {
        ParseSyntaxError() {
            // Suppress stack trace for performance — this is a control-flow exception.
            super(null, null, true, false);
        }
    } // class ParseSyntaxError

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder( "[" );
        if( slice != null ) {
            for( int i = 0; i < slice.size(); i++ ) {
                if( i > 0 ) sb.append( ", " );
                sb.append( slice.get( i ).value );
            }
        }
        sb.append( "]" );
        return sb.toString();
    }
} // class YaccProduction
