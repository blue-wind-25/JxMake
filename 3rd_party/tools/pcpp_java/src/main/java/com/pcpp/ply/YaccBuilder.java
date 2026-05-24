// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factory that constructs an {@link LRParser} from a {@link GrammarSpec}.
 *
 * Mirrors PLY's {@code yacc()} function from yacc.py.
 *
 * <p>Typical usage:
 * <pre>
 *   GrammarSpec spec = GrammarSpec.builder()
 *       .tokens("NUMBER", "PLUS", "MINUS", "TIMES", "DIVIDE")
 *       .prec("left", "PLUS", "MINUS")
 *       .prec("left", "TIMES", "DIVIDE")
 *       .rule("p_expr_binop",
 *             "expr : expr PLUS  term\n" +
 *             "     | expr MINUS term",
 *             p -&gt; p.set(0, (int) p.get(1) + (int) p.get(3)))
 *       .rule("p_expr_term", "expr : term", p -&gt; p.set(0, p.get(1)))
 *       .rule("p_term_num",  "term : NUMBER", p -&gt; p.set(0, p.get(1)))
 *       .errorFunc(tok -&gt; System.err.println("Syntax error: " + tok))
 *       .build();
 *
 *   LRParser parser = YaccBuilder.build(spec);
 *   Object result = parser.parse(tokenSupplier, lexer);
 * </pre>
 *
 * <p>Alternatively, if pre-generated parse tables are available:
 * <pre>
 *   LRParser parser = YaccBuilder.buildFromTable(parseTabData, spec);
 * </pre>
 */
public class YaccBuilder {

    /** Whether to check for unreachable / infinite-cycle symbols. */
    public static boolean CHECK_RECURSION = true;

    // =========================================================================
    // build() — construct parser from GrammarSpec (full table generation)
    // =========================================================================

    /**
     * Build a parser by generating LALR(1) parse tables from the given spec.
     *
     * @param spec  the grammar specification
     * @return a ready-to-use {@link LRParser}
     * @throws YaccError if the grammar contains errors
     */
    public static LRParser build( GrammarSpec spec )
    {
        return build( spec, "LALR", PlyLogger.NULL, PlyLogger.NULL );
    }

    /**
     * Build a parser with explicit logging.
     *
     * @param spec      grammar specification
     * @param method    "LALR" or "SLR"
     * @param errorlog  logger for user-facing error/warning messages
     * @param debuglog  logger for detailed table-construction debug output
     * @return a ready-to-use {@link LRParser}
     * @throws YaccError if the grammar contains errors
     */
    public static LRParser build( GrammarSpec spec, String method,
        PlyLogger errorlog, PlyLogger debuglog )
    {
        boolean errors = false;

        // 1. Validate tokens
        if( spec.tokens == null || spec.tokens.isEmpty() ) throw new YaccError( "No token list is defined" );
        for( String t : spec.tokens )
            if( t.equals( "error" ) ) throw new YaccError( "Illegal token name 'error'. It is a reserved word" );

        // 2. Build Grammar object
        Grammar grammar = new Grammar( spec.tokens );

        // 3. Set precedence
        for( int level = 0; level < spec.precedence.size(); level++ ) {
            String[] entry = spec.precedence.get( level );
            String   assoc = entry[0];
            for( int i = 1; i < entry.length; i++ ) {
                try {
                    grammar.set_precedence( entry[i], assoc, level + 1 );
                } catch( GrammarError e ) {
                    errorlog.warning( "%s", e.getMessage() );
                }
            }
        }

        // 4. Build action map (function name → Consumer<YaccProduction>)
        Map<String, Consumer<YaccProduction> > actionMap = new LinkedHashMap<>();
        for( GrammarRule rule : spec.rules )
            if( rule.action != null ) actionMap.put( rule.name, rule.action );

        // 5. Parse grammar rule strings and add productions
        for( GrammarRule rule : spec.rules ) {
            List<Object[]> parsed = parseGrammarString( rule.pattern, rule.file, rule.line );
            for( Object[] g : parsed ) {
                // g = [file, line, prodname, syms]
                String file       = (String) g[0];
                int    line       = (Integer) g[1];
                String prodname   = (String) g[2];
                @SuppressWarnings("unchecked")
                List<String> syms = (List<String>) g[3];
                try {
                    grammar.add_production( prodname, syms, rule.name, file, line );
                } catch( GrammarError e ) {
                    errorlog.error( "%s", e.getMessage() );
                    errors = true;
                }
            }
        }

        // 6. Set start symbol
        try {
            grammar.set_start( spec.start );
        } catch( GrammarError e ) {
            errorlog.error( "%s", e.getMessage() );
            errors = true;
        }

        if( errors ) throw new YaccError( "Unable to build parser" );

        // 7. Validate the grammar
        for( Object[] sym_prod : grammar.undefined_symbols() ) {
            Production prod = (Production) sym_prod[1];
            errorlog.error( "%s:%d: Symbol '%s' used, but not defined as a token or a rule",
                prod.file, prod.line, sym_prod[0] );
            errors = true;
        }

        for( String term : grammar.unused_terminals() ) errorlog.warning( "Token '%s' defined, but not used", term );

        for( Production prod : grammar.unused_rules() ) errorlog.warning( "%s:%d: Rule '%s' defined, but not used",
            prod.file, prod.line, prod.name );

        for( String[] prec : grammar.unused_precedence() ) {
            errorlog.error( "Precedence rule '%s' defined for unknown symbol '%s'",
                prec[1], prec[0] );
            errors = true;
        }

        if( CHECK_RECURSION ) {
            for( String u : grammar.find_unreachable() ) errorlog.warning( "Symbol '%s' is unreachable", u );
            for( String inf : grammar.infinite_cycles() ) {
                errorlog.error( "Infinite recursion detected for symbol '%s'", inf );
                errors = true;
            }
        }

        if( errors ) throw new YaccError( "Unable to build parser" );

        // 8. Generate parse tables
        LRGeneratedTable lr    = new LRGeneratedTable( grammar, method, debuglog );

        int              numSR = lr.sr_conflicts.size();
        int              numRR = lr.rr_conflicts.size();
        if( numSR == 1 ) {errorlog.warning( "1 shift/reduce conflict" );}
        else if( numSR > 1 ) {errorlog.warning( "%d shift/reduce conflicts", numSR );}
        if( numRR == 1 ) {errorlog.warning( "1 reduce/reduce conflict" );}
        else if( numRR > 1 ) {errorlog.warning( "%d reduce/reduce conflicts", numRR );}

        // 9. Bind callables
        lr.bindCallables( actionMap );

        // 10. Wrap error function
        Consumer<YaccSymbol> errFunc = wrapErrorFunc( spec.errorFunc );

        return new LRParser( lr, errFunc );
    }

    // =========================================================================
    // buildFromTable() — construct parser from pre-generated tables
    // =========================================================================

    /**
     * Build a parser from pre-generated parse table data.
     *
     * This is the fast path: no table generation happens at runtime.
     * Analogous to PLY loading a pre-built parsetab.py.
     *
     * @param tab   the pre-generated table data
     * @param spec  grammar spec — used only to bind action functions
     * @return a ready-to-use {@link LRParser}
     */
    public static LRParser buildFromTable( ParseTabData tab, GrammarSpec spec )
    {
        LRTable lrtab = new LRTable();
        lrtab.readTable( tab );

        // Build action map from spec
        Map<String, Consumer<YaccProduction> > actionMap = new LinkedHashMap<>();
        for( GrammarRule rule : spec.rules )
            if( rule.action != null ) actionMap.put( rule.name, rule.action );
        lrtab.bindCallables( actionMap );

        Consumer<YaccSymbol> errFunc = wrapErrorFunc( spec.errorFunc );
        return new LRParser( lrtab, errFunc );
    }

    // =========================================================================
    // Grammar string parsing
    // =========================================================================

    /**
     * Parse a BNF-style grammar rule string into a list of production tuples.
     *
     * Mirrors PLY's {@code parse_grammar()} from yacc.py.
     *
     * Each element of the returned list is an Object[] with four elements:
     *   [0] String       - source file
     *   [1] Integer      - line number
     *   [2] String       - production name (LHS)
     *   [3] List<String> - RHS symbols
     *
     * @param doc   the rule string (may contain newlines / "|" continuations)
     * @param file  source file name (for error messages)
     * @param line  starting line number
     * @return list of production tuples
     * @throws YaccError on parse error
     */
    public static List<Object[]> parseGrammarString( String doc, String file, int line )
    {
        List<Object[]> result = new ArrayList<>();
        if( doc == null || doc.trim().isEmpty() ) return result;

        String lastp          = null;
        int    dline          = line;

        for( String ps : doc.split( "\n", -1 ) ) {
            dline++;
            String trimmed     = ps.trim();
            if( trimmed.isEmpty() ) continue;

            String[]     parts = trimmed.split( "\\s+" );
            String       prodname;
            List<String> syms  = new ArrayList<>();

            if( parts[0].equals( "|" ) ) {
                // Continuation of previous rule
                if( lastp == null ) throw new YaccError( file + ":" + dline + ": Misplaced '|'" );
                prodname = lastp;
                for( int i = 1; i < parts.length; i++ ) syms.add( parts[i] );
            }
            else {
                prodname = parts[0];
                lastp    = prodname;
                if( parts.length < 2 ) throw new YaccError( file + ":" + dline +
                    ": Syntax error in rule '" + trimmed + "'" );
                String assign = parts[1];
                if( !assign.equals( ":" ) && !assign.equals( "::=" ) ) throw new YaccError( file + ":" + dline +
                    ": Syntax error. Expected ':' in rule '" + trimmed + "'" );
                for( int i = 2; i < parts.length; i++ ) syms.add( parts[i] );
            }

            result.add( new Object[] {file, dline, prodname, syms} );
        }
        return result;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /** Wrap the user error function into the format expected by LRParser. */
    private static Consumer<YaccSymbol> wrapErrorFunc( Consumer<YaccSymbol> userFunc )
    {
        return userFunc; // already the right type; null is handled inside LRParser
    }
} // class YaccBuilder
