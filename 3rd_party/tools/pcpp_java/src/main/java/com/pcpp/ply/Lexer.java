// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.*;
import java.util.regex.*;

/**
 * Java implementation of a PLY-style regex-based lexer.
 *
 * <p>In Python PLY, rules are discovered from {@code t_XXX} function
 * docstrings/strings on a module object.  In this Java translation, rules are
 * supplied via a {@link LexerSpec} (a list of {@link LexRule} objects),
 * preserving the same priority ordering as PLY:
 * <ol>
 *   <li>Function rules in declaration order.</li>
 *   <li>String-only rules in order of decreasing pattern length.</li>
 * </ol>
 *
 * <p>Public API matches Python PLY's {@code Lexer} class from {@code lex.py}:
 * <ul>
 *   <li>{@link #input(String)} — feed input text</li>
 *   <li>{@link #token()} — get next token (null at EOF)</li>
 *   <li>{@link #begin(String)} — change lexer state</li>
 *   <li>{@link #pushState(String)} / {@link #popState()} — state stack</li>
 *   <li>{@link #currentState()} — query state name</li>
 *   <li>{@link #skip(int)} — advance position</li>
 *   <li>{@link #clone()} — clone the lexer</li>
 *   <li>{@link #lineno} — current line number (1-based)</li>
 *   <li>{@link #lexpos} — current position in input</li>
 * </ul>
 *
 * <p>Mirrors PLY's {@code Lexer} class from {@code lex.py}.
 */
public class Lexer {

    // -----------------------------------------------------------------------
    // Internal: one compiled master-regex chunk with its index table.
    // Mirrors the (cre, findex) tuple from Python PLY.
    // -----------------------------------------------------------------------

    /**
     * A compiled master regex together with an index that maps regex group
     * numbers to (callback, tokenType) pairs.
     */
    public static final class CompiledLexRe {
        /** The compiled pattern. */
        public final Pattern pattern;
        /**
         * Group-index → {@link IndexEntry} table.
         * {@code null} entries mean the group exists but was not registered.
         * Index 0 is unused (full-match group); meaningful entries start at 1.
         */
        public final IndexEntry[]         indexFunc;
        /** Group name → group number map (from the pattern). */
        public final Map<String, Integer> groupIndex;

        public CompiledLexRe( Pattern pattern, IndexEntry[] indexFunc,
            Map<String, Integer> groupIndex )
        {
            this.pattern    = pattern;
            this.indexFunc  = indexFunc;
            this.groupIndex = groupIndex;
        }
    } // class CompiledLexRe

    /**
     * One entry in the group-to-function index.
     * Mirrors the {@code (func, toktype)} tuple in PLY's findex list.
     * Both fields may be {@code null}: callback==null for string rules;
     * tokenType==null for ignore rules.
     */
    public static final class IndexEntry {
        public final TokenCallback callback;  // null for string/ignore rules
        public final String        tokenType; // null for ignore rules

        public IndexEntry( TokenCallback callback, String tokenType )
        {
            this.callback  = callback;
            this.tokenType = tokenType;
        }
    } // class IndexEntry

    // -----------------------------------------------------------------------
    // Per-state tables (populated by LexerBuilder)
    // -----------------------------------------------------------------------

    /** Compiled master regexes per state name. */
    public Map<String, List<CompiledLexRe> > lexstatere     = new HashMap<>();
    /** Raw regex strings per state (for debugging). */
    public Map<String, List<String> >        lexstateretext = new HashMap<>();
    /** State type map: name → "inclusive" | "exclusive". */
    public Map<String, String>               lexstateinfo   = new HashMap<>();
    /** Ignore character strings per state. */
    public Map<String, String>               lexstateignore = new HashMap<>();
    /** Error callbacks per state. */
    public Map<String, TokenCallback>        lexstateerrorf = new HashMap<>();
    /** EOF callbacks per state. */
    public Map<String, TokenCallback>        lexstateeoff   = new HashMap<>();

    // -----------------------------------------------------------------------
    // Currently active state
    // -----------------------------------------------------------------------

    /** Active compiled regex list for the current state. */
    public List<CompiledLexRe> lexre;
    /** Characters to skip in the current state. */
    public String              lexignore = "";
    /** Error callback for the current state. */
    public TokenCallback       lexerrorf;
    /** EOF callback for the current state. */
    public TokenCallback       lexeoff;
    /** Current state name. */
    public String              lexstate      = "INITIAL";
    /** State stack for push/pop operations. */
    public List<String>        lexstatestack = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Token sets and literals
    // -----------------------------------------------------------------------

    /** Valid token type names (excluding single-char literals). */
    public Set<String> lextokens    = new HashSet<>();
    /** Valid token type names including single-char literals. */
    public Set<String> lextokensAll = new HashSet<>();
    /** Single-character literal tokens. */
    public String      lexliterals  = "";

    // -----------------------------------------------------------------------
    // Input / position
    // -----------------------------------------------------------------------

    /** The current input string. */
    public String  lexdata;
    /** Current position within {@link #lexdata}. */
    public int     lexpos = 0;
    /** Length of {@link #lexdata}. */
    public int     lexlen = 0;
    /** Current line number (1-based). */
    public int     lineno = 1;
    /** Last {@link Matcher} produced during a rule match (set before callbacks). */
    public Matcher lexmatch;
    /** When {@code true} token-type validation is skipped (optimize mode). */
    public boolean lexoptimize = false;

    // -----------------------------------------------------------------------
    // input()
    // -----------------------------------------------------------------------

    /**
     * Feed a new input string to the lexer.
     * Mirrors {@code Lexer.input()} from {@code lex.py}.
     *
     * @param s the input text
     */
    public void input( String s )
    {
        if( s == null ) throw new IllegalArgumentException( "Expected a non-null String" );
        this.lexdata = s;
        this.lexpos  = 0;
        this.lexlen  = s.length();
    }

    // -----------------------------------------------------------------------
    // begin() / pushState() / popState() / currentState()
    // -----------------------------------------------------------------------

    /**
     * Change the current lexer state.
     * Mirrors {@code Lexer.begin()} from {@code lex.py}.
     *
     * @param state name of the state to switch to
     * @throws IllegalArgumentException if the state is not defined
     */
    public void begin( String state )
    {
        if( !lexstatere.containsKey( state ) ) throw new IllegalArgumentException( "Undefined lexer state: " + state );
        this.lexre     = lexstatere.get( state );
        this.lexignore = lexstateignore.getOrDefault( state, "" );
        this.lexerrorf = lexstateerrorf.get( state );
        this.lexeoff   = lexstateeoff.get( state );
        this.lexstate  = state;
    }

    /**
     * Push the current state onto the stack and switch to {@code state}.
     * Mirrors {@code Lexer.push_state()} from {@code lex.py}.
     */
    public void pushState( String state )
    {
        lexstatestack.add( lexstate );
        begin( state );
    }

    /**
     * Pop the most recently pushed state and switch back to it.
     * Mirrors {@code Lexer.pop_state()} from {@code lex.py}.
     */
    public void popState()
    {
        begin( lexstatestack.remove( lexstatestack.size() - 1 ) );
    }

    /**
     * Returns the name of the current lexer state.
     * Mirrors {@code Lexer.current_state()} from {@code lex.py}.
     */
    public String currentState()
    {
        return lexstate;
    }

    // -----------------------------------------------------------------------
    // skip()
    // -----------------------------------------------------------------------

    /**
     * Advance the lexer position by {@code n} characters.
     * Mirrors {@code Lexer.skip()} from {@code lex.py}.
     */
    public void skip( int n )
    {
        lexpos += n;
    }

    // -----------------------------------------------------------------------
    // token()
    // -----------------------------------------------------------------------

    /**
     * Return the next token from the lexer, or {@code null} at end of input.
     *
     * <p>This is the hot path; the logic mirrors {@code Lexer.token()} from
     * {@code lex.py} as closely as possible.
     *
     * @return the next {@link LexToken}, or {@code null}
     * @throws LexError if an unrecognised character is encountered and no
     *                  error rule handles it
     */
    public LexToken token()
    {
        int    pos    = this.lexpos;
        int    len    = this.lexlen;
        String ignore = this.lexignore;
        String data   = this.lexdata;

        while( pos < len ) {
            // Skip ignored characters (short-circuit path)
            if( ignore.indexOf( data.charAt( pos ) ) >= 0 ) {
                pos++;
                continue;
            }

            // Try each compiled master regex in turn
            boolean             matchedIgnore = false;
            List<CompiledLexRe> activeLexRe   = this.lexre;
            if( activeLexRe == null ) activeLexRe = java.util.Collections.emptyList();
            for( CompiledLexRe clr : activeLexRe ) {
                Matcher m = clr.pattern.matcher( data );
                m.region( pos, len );
                m.useTransparentBounds( true );
                m.useAnchoringBounds( false );
                if( !m.lookingAt() ) continue;

                // Find which named group matched (the first one at pos)
                int matchedGroup = -1;
                for( Map.Entry<String, Integer> e : clr.groupIndex.entrySet() ) {
                    int g = e.getValue();
                    if( g < clr.indexFunc.length && m.start( g ) == pos && m.end( g ) > pos ) {
                        matchedGroup = g;
                        break;
                    }
                }
                if( matchedGroup < 0 || matchedGroup >= clr.indexFunc.length ) continue;

                IndexEntry entry = clr.indexFunc[matchedGroup];
                if( entry == null ) continue;

                // Build token
                LexToken tok = new LexToken();
                tok.value  = m.group();
                tok.lineno = this.lineno;
                tok.lexpos = pos;
                tok.type   = entry.tokenType;

                if( entry.callback == null ) {
                    // Simple string rule
                    if( tok.type != null ) {
                        // Emit token
                        this.lexpos = m.end();
                        return tok;
                    }
                    else {
                        // Ignore rule — consume and move on
                        pos           = m.end();
                        matchedIgnore = true;
                        break;
                    }
                }

                // Function rule: set lexer reference, call callback
                pos           = m.end();
                tok.lexer     = this;
                this.lexmatch = m;
                this.lexpos   = pos;

                LexToken newtok = entry.callback.apply( tok );

                if( newtok == null ) {
                    // Callback discarded the token — update positions and continue
                    pos           = this.lexpos;
                    ignore        = this.lexignore;
                    matchedIgnore = true;
                    break;
                }

                // Validate token type (unless in optimize mode)
                if( !lexoptimize && !lextokensAll.contains( newtok.type ) )
                    throw new LexError(
                        "Rule returned an unknown token type '" + newtok.type + "'",
                        data.substring( pos ) );
                return newtok;
            }

            if( matchedIgnore ) continue;

            // No regex matched — check literals
            char ch = data.charAt( pos );
            if( lexliterals.indexOf( ch ) >= 0 ) {
                LexToken tok = new LexToken();
                tok.value   = String.valueOf( ch );
                tok.lineno  = this.lineno;
                tok.type    = (String) tok.value;
                tok.lexpos  = pos;
                this.lexpos = pos + 1;
                return tok;
            }

            // No match — call error handler
            if( lexerrorf != null ) {
                LexToken tok = new LexToken();
                tok.value   = data.substring( pos );
                tok.lineno  = this.lineno;
                tok.type    = "error";
                tok.lexer   = this;
                tok.lexpos  = pos;
                this.lexpos = pos;
                LexToken newtok = lexerrorf.apply( tok );
                if( this.lexpos == pos )
                    throw new LexError(
                        "Scanning error. Illegal character '" + ch + "'",
                        data.substring( pos ) );
                pos = this.lexpos;
                if( newtok != null ) return newtok;
                continue;
            }

            this.lexpos = pos;
            throw new LexError(
                "Illegal character '" + ch + "' at index " + pos,
                data.substring( pos ) );
        }

        // End of input
        if( lexeoff != null ) {
            LexToken tok = new LexToken();
            tok.type    = "eof";
            tok.value   = "";
            tok.lineno  = this.lineno;
            tok.lexpos  = pos;
            tok.lexer   = this;
            this.lexpos = pos;
            return lexeoff.apply( tok );
        }

        this.lexpos = pos + 1;
        if( lexdata == null ) throw new RuntimeException( "No input string given with input()" );
        return null;
    }

    // -----------------------------------------------------------------------
    // Iterable support
    // -----------------------------------------------------------------------

    /**
     * Iterate over all tokens produced from the current input.
     * Mirrors Python PLY's {@code __iter__} / {@code __next__} support.
     */
    public Iterable<LexToken> tokens()
    {
        return () -> new java.util.Iterator<LexToken>() {
                   private LexToken next = advance();

                   private LexToken advance()
                   {
                       try {
                           return token();
                       }
                       catch( LexError e ) {
                           throw new RuntimeException( e );
                       }
                   }

                   @Override public boolean hasNext() { return next != null; }

                   @Override
                   public LexToken next()
                   {
                       if( next == null ) throw new java.util.NoSuchElementException();
                       LexToken cur = next;
                       next = advance();
                       return cur;
                   }
        };
    }

    // -----------------------------------------------------------------------
    // clone()
    // -----------------------------------------------------------------------

    /**
     * Create a shallow copy of this lexer.
     *
     * <p>The clone shares all compiled regex structures (they are immutable)
     * but gets its own mutable position / state / stack.
     * Mirrors {@code Lexer.clone()} from {@code lex.py}.
     *
     * @return a new {@code Lexer} that is a shallow copy of this one
     */
    public Lexer clone()
    {
        Lexer c = new Lexer();
        // Share immutable compiled structures
        c.lexstatere     = this.lexstatere;
        c.lexstateretext = this.lexstateretext;
        c.lexstateinfo   = this.lexstateinfo;
        c.lexstateignore = this.lexstateignore;
        c.lexstateerrorf = this.lexstateerrorf;
        c.lexstateeoff   = this.lexstateeoff;
        c.lextokens      = this.lextokens;
        c.lextokensAll   = this.lextokensAll;
        c.lexliterals    = this.lexliterals;
        c.lexoptimize    = this.lexoptimize;
        // Copy mutable per-instance fields
        c.lexdata        = this.lexdata;
        c.lexpos         = this.lexpos;
        c.lexlen         = this.lexlen;
        c.lineno         = this.lineno;
        c.lexstate       = this.lexstate;
        c.lexstatestack  = new ArrayList<>( this.lexstatestack );
        // Activate the cloned state
        c.lexre          = c.lexstatere.get( c.lexstate );
        c.lexignore      = c.lexstateignore.getOrDefault( c.lexstate, "" );
        c.lexerrorf      = c.lexstateerrorf.get( c.lexstate );
        c.lexeoff        = c.lexstateeoff.get( c.lexstate );
        return c;
    }

    // -----------------------------------------------------------------------
    // Builder (convenience wrapper around LexerBuilder)
    // -----------------------------------------------------------------------

    /**
     * Fluent builder for constructing a {@code Lexer} directly without going
     * through a full {@link LexerSpec}.  Mirrors the {@code Lexer.Builder}
     * pattern and the PLY {@code lex()} convenience function.
     *
     * <p>Example:
     * <pre>{@code
     * Lexer lex = new Lexer.Builder()
     *     .addRule(new LexRule("t_ID",     "[A-Za-z_]\\w*"))
     *     .addRule(new LexRule("t_NUMBER", "\\d+"))
     *     .literals("+-*{@literal /}")
     *     .build();
     * lex.input("x + 42");
     * }</pre>
     */
    public static class Builder {
        private final List<LexRule> rules    = new ArrayList<>();
        private String              literals = "";
        private TokenCallback       errorCallback;

        /** Add a lexer rule. */
        public Builder addRule( LexRule rule )
        {
            rules.add( rule );
            return this;
        }

        /** Add a simple string rule (no callback). */
        public Builder addRule( String name, String pattern )
        {
            rules.add( new LexRule( name, pattern ) );
            return this;
        }

        /** Add a function rule with a callback. */
        public Builder addRule( String name, String pattern, TokenCallback cb )
        {
            rules.add( new LexRule( name, pattern, cb ) );
            return this;
        }

        /** Set the literal characters string. */
        public Builder literals( String lit )
        {
            this.literals = lit;
            return this;
        }

        /** Set the error callback (mirrors {@code t_error}). */
        public Builder errorCallback( TokenCallback cb )
        {
            this.errorCallback = cb;
            return this;
        }

        /**
         * Build a {@link Lexer} from the accumulated rules.
         * Delegates to {@link LexerBuilder}.
         */
        public Lexer build()
        {
            LexerSpec spec = new LexerSpec();
            spec.addLiterals( literals );
            for( LexRule r : rules ) spec.addRule( r );
            if( errorCallback != null ) spec.setErrorCallback( errorCallback );
            return LexerBuilder.build( spec );
        }
    } // class Builder
}     // class Lexer
