// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.List;

/**
 * Complete specification for a grammar to be compiled into an LALR parser.
 *
 * In Python PLY this information is scattered across module-level variables
 * and p_xxx functions.  In Java everything is collected into a
 * {@link GrammarSpec} and passed to {@link YaccBuilder#build}.
 *
 * Mandatory fields:
 *   tokens     - the list of terminal token names (mirrors Python's {@code tokens})
 *   rules      - ordered list of {@link GrammarRule} objects (mirrors p_xxx functions)
 *
 * Optional fields:
 *   start      - start symbol (defaults to the LHS of the first rule)
 *   precedence - list of precedence / associativity entries
 *   errorFunc  - the error callback (mirrors Python's {@code p_error(p)})
 */
public class GrammarSpec {

    /** Terminal token names (must not contain "error"). */
    public final List<String>      tokens;

    /** Grammar rules in declaration order. */
    public final List<GrammarRule> rules;

    /**
     * Precedence / associativity declarations, in order from lowest to highest.
     *
     * Each entry is a string array: {"left"|"right"|"nonassoc", "TERM1", "TERM2", ...}
     *
     * Mirrors the Python {@code precedence} variable:
     * <pre>
     *   precedence = (
     *       ('left', 'PLUS', 'MINUS'),
     *       ('left', 'TIMES', 'DIVIDE'),
     *   )
     * </pre>
     */
    public final List<String[]> precedence;

    /**
     * The start symbol.  If null, the LHS of the first rule is used.
     * Mirrors Python's optional {@code start} variable.
     */
    public final String start;

    /**
     * Error callback.  Called with the offending {@link YaccSymbol} when a
     * syntax error occurs.  May be null (no error handler).
     * Mirrors Python's {@code p_error(p)} function.
     */
    public final Consumer<YaccSymbol> errorFunc;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    private GrammarSpec( Builder b )
    {
        this.tokens     = Collections.unmodifiableList( new ArrayList<>( b.tokens ) );
        this.rules      = Collections.unmodifiableList( new ArrayList<>( b.rules ) );
        this.precedence = Collections.unmodifiableList( new ArrayList<>( b.precedence ) );
        this.start      = b.start;
        this.errorFunc  = b.errorFunc;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder {
        private List<String>         tokens     = new ArrayList<>();
        private List<GrammarRule>    rules      = new ArrayList<>();
        private List<String[]>       precedence = new ArrayList<>();
        private String               start      = null;
        private Consumer<YaccSymbol> errorFunc  = null;

        public Builder tokens( String... names )
        {
            tokens.addAll( Arrays.asList( names ) );
            return this;
        }

        public Builder tokens( List<String> names )
        {
            tokens.addAll( names );
            return this;
        }

        public Builder rule( GrammarRule rule )
        {
            rules.add( rule );
            return this;
        }

        public Builder rule( String name, String pattern, Consumer<YaccProduction> action )
        {
            rules.add( new GrammarRule( name, pattern, action ) );
            return this;
        }

        public Builder rule( String name, String pattern )
        {
            rules.add( new GrammarRule( name, pattern, null ) );
            return this;
        }

        /**
         * Add a precedence entry.
         * @param assoc  "left", "right", or "nonassoc"
         * @param terms  terminal names at this precedence level
         */
        public Builder prec( String assoc, String... terms )
        {
            String[] entry = new String[terms.length + 1];
            entry[0] = assoc;
            System.arraycopy( terms, 0, entry, 1, terms.length );
            precedence.add( entry );
            return this;
        }

        public Builder start( String startSymbol )
        {
            this.start = startSymbol;
            return this;
        }

        public Builder errorFunc( Consumer<YaccSymbol> errorHandler )
        {
            this.errorFunc = errorHandler;
            return this;
        }

        public GrammarSpec build()
        {
            if( tokens.isEmpty() ) throw new YaccError( "GrammarSpec requires at least one token" );
            if( rules.isEmpty() ) throw new YaccError( "GrammarSpec requires at least one grammar rule" );
            return new GrammarSpec( this );
        }
    } // class Builder
}     // class GrammarSpec
