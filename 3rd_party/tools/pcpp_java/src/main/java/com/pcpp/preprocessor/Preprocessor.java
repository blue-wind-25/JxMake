// Translated using Claude Sonnet 4.6
package com.pcpp.preprocessor;

import com.pcpp.evaluator.Evaluator;
import com.pcpp.evaluator.Value;
import com.pcpp.parser.*;
import com.pcpp.ply.LexToken;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * C preprocessor implementation.
 * Mirrors the Preprocessor class in preprocessor.py.
 *
 * Usage:
 *   Preprocessor pp = new Preprocessor();
 *   pp.parse(sourceText, "myfile.c");
 *   pp.write(System.out);
 */
public class Preprocessor extends PreprocessorHooks {

    // -----------------------------------------------------------------------
    // Instance variables (mirrors preprocessor.py __init__)
    // -----------------------------------------------------------------------
    protected CppLexer             lexer;
    protected Evaluator            evaluator;
    public Map<String, Macro>      macros;
    public List<String>            path;
    public List<String>            temp_path;
    protected List<Object[]>       rewrite_paths; // List of [Pattern, String replacement]
    protected Map<String, String>  _file_cache;
    public Pattern                 passthru_includes;
    public Map<String, Object>     include_once; // path -> null or include guard macro name
    public int                     include_depth;
    public List<FileInclusionTime> include_times;
    public int                     return_code;
    public PrintWriter             debugout;
    public boolean                 auto_pragma_once_enabled;
    public String                  line_directive;
    public int                     compress; // 0 = no, 1 = some, 2 = maximum
    public String                  assume_encoding;

    // Token type probed from lexer
    protected String               t_ID;
    protected String               t_INTEGER;
    protected Class<?>             t_INTEGER_TYPE;
    protected String               t_CHAR;
    protected String               t_STRING;
    protected String               t_SPACE;
    protected String               t_NEWLINE;
    protected String               t_LINECONT;
    protected Set<String>          t_WS;
    protected String               t_DPOUND;
    protected String               t_TERNARY;
    protected String               t_COLON;
    protected String               t_COMMENT1;
    protected String               t_COMMENT2;
    protected Set<String>          t_COMMENT;

    // Expansion tracking
    protected boolean              expand_linemacro;
    protected boolean              expand_filemacro;
    protected boolean              expand_countermacro;
    protected int                  linemacro;
    protected int                  linemacrodepth;
    protected int                  countermacro;
    protected String               source;

    // Parse output iterator
    protected Iterator<LexToken>   parser;
    protected Set<String>          ignore;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    public Preprocessor() {
        this(null);
    }

    public Preprocessor( CppLexer lexer ) {
        super();
        if( lexer == null ) lexer = CppParser.defaultLexer();
        this.lexer                    = lexer;
        this.evaluator                = new Evaluator();
        this.macros                   = new LinkedHashMap<>();
        this.path                     = new ArrayList<>();
        this.temp_path                = new ArrayList<>();
        this.rewrite_paths            = new ArrayList<>();
        this._file_cache              = new HashMap<>();
        this.passthru_includes        = null;
        this.include_once             = new LinkedHashMap<>();
        this.include_depth            = 0;
        this.include_times            = new ArrayList<>();
        this.return_code              = 0;
        this.debugout                 = null;
        this.auto_pragma_once_enabled = true;
        this.line_directive           = "#line";
        this.compress                 = 0;
        this.assume_encoding          = null;

        // Add a rewrite to relativise the current working directory
        String cwd = Paths.get( "" ).toAbsolutePath().toString() + File.separator;
        rewrite_paths.add( new Object[] {
            Pattern.compile( Pattern.quote( cwd ) + "(.*)" ),
            "$1"
        } );

        lexprobe();

        LocalDateTime     now     = LocalDateTime.now();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern( "MMM dd yyyy" );
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern( "HH:mm:ss" );
        define( "__DATE__ \"" + now.format( dateFmt ) + "\"" );
        define( "__TIME__ \"" + now.format( timeFmt ) + "\"" );
        define( "__PCPP__ 1" );

        expand_linemacro    = true;
        expand_filemacro    = true;
        expand_countermacro = true;
        linemacro           = 0;
        linemacrodepth      = 0;
        countermacro        = 0;
    }

    // -----------------------------------------------------------------------
    // tokenize() – mirrors preprocessor.py tokenize()
    // -----------------------------------------------------------------------

    public List<LexToken> tokenize( String text )
    {
        List<LexToken> tokens = new ArrayList<>();
        lexer.input( text );
        while( true ) {
            LexToken tok = lexer.token();
            if( tok == null ) break;
            tok.source = "";
            tokens.add( tok );
        }
        return tokens;
    }

    // -----------------------------------------------------------------------
    // lexprobe() – mirrors preprocessor.py __lexprobe()
    // -----------------------------------------------------------------------

    private void lexprobe()
    {
        lexer.input( "identifier" );
        LexToken tok = lexer.token();
        if( tok == null || !"identifier".equals( tok.value ) ) {
            System.err.println( "Couldn't determine identifier type" );
        }
        else {
            t_ID = tok.type;
        }

        lexer.input( "12345" );
        tok = lexer.token();
        if( tok == null ) {
            System.err.println( "Couldn't determine integer type" );
        }
        else {
            t_INTEGER      = tok.type;
            t_INTEGER_TYPE = tok.value.getClass();
        }

        lexer.input( "'a'" );
        tok = lexer.token();
        if( tok == null || !"'a'".equals( tok.value ) ) {
            System.err.println( "Couldn't determine character type" );
        }
        else {
            t_CHAR = tok.type;
        }

        lexer.input( "\"filename\"" );
        tok = lexer.token();
        if( tok == null || !"\"filename\"".equals( tok.value ) ) {
            System.err.println( "Couldn't determine string type" );
        }
        else {
            t_STRING = tok.type;
        }

        lexer.input( "  " );
        tok = lexer.token();
        if( tok == null || !"  ".equals( tok.value ) ) {
            t_SPACE = null;
        }
        else {
            t_SPACE = tok.type;
        }

        lexer.input( "\n" );
        tok = lexer.token();
        if( tok == null || !"\n".equals( tok.value ) ) {
            t_NEWLINE = null;
            System.err.println( "Couldn't determine token for newlines" );
        }
        else {
            t_NEWLINE = tok.type;
        }

        lexer.input( "\\     \n" );
        tok = lexer.token();
        if( tok == null || !"     ".equals( tok.value ) ) {
            t_LINECONT = null;
            System.err.println( "Couldn't determine token for line continuations" );
        }
        else {
            t_LINECONT = tok.type;
        }

        t_WS = new HashSet<>( Arrays.asList( t_SPACE, t_NEWLINE, t_LINECONT ) );

        lexer.input( "##" );
        tok  = lexer.token();
        if( tok == null || !"##".equals( tok.value ) ) {
            System.err.println( "Couldn't determine token for token pasting operator" );
        }
        else {
            t_DPOUND = tok.type;
        }

        lexer.input( "?" );
        tok = lexer.token();
        if( tok == null || !"?".equals( tok.value ) ) {
            System.err.println( "Couldn't determine token for ternary operator" );
        }
        else {
            t_TERNARY = tok.type;
        }

        lexer.input( ":" );
        tok = lexer.token();
        if( tok == null || !":".equals( tok.value ) ) {
            System.err.println( "Couldn't determine token for colon" );
        }
        else {
            t_COLON = tok.type;
        }

        lexer.input( "/* comment */" );
        tok = lexer.token();
        if( tok == null || !"/* comment */".equals( tok.value ) ) {
            System.err.println( "Couldn't determine comment type" );
        }
        else {
            t_COMMENT1 = tok.type;
        }

        lexer.input( "// comment" );
        tok = lexer.token();
        if( tok == null || !"// comment".equals( tok.value ) ) {
            System.err.println( "Couldn't determine comment type" );
        }
        else {
            t_COMMENT2 = tok.type;
        }

        t_COMMENT = new HashSet<>( Arrays.asList( t_COMMENT1, t_COMMENT2 ) );
    }

    // -----------------------------------------------------------------------
    // add_path() – mirrors preprocessor.py add_path()
    // -----------------------------------------------------------------------

    public void add_path( String p )
    {
        path.add( p );
        try {
            Path    rel    = Paths.get( p );
            String  relStr = rel.toString();
            Pattern pat    = Pattern.compile( Pattern.quote(
                Paths.get( p ).toAbsolutePath().toString() + File.separator ) + "(.*)" );
            rewrite_paths.add( new Object[] {pat, relStr + File.separator + "$1"} );
        } catch( Exception ignored ) {}
    }

    // -----------------------------------------------------------------------
    // group_lines() – mirrors preprocessor.py group_lines()
    // -----------------------------------------------------------------------

    protected Iterable<List<LexToken> > group_lines( String input, String abssource )
    {
        CppLexer      lex      = lexer.clone();
        // Strip trailing whitespace from each line (mirrors [x.rstrip() for x in input.splitlines()])
        String[]      rawLines = input.split( "\n", -1 );
        StringBuilder sb       = new StringBuilder();
        for( String line : rawLines ) {
            // rstrip
            int end = line.length();
            while( end > 0 && (line.charAt( end - 1 ) == ' ' || line.charAt( end - 1 ) == '\t') ) end--;
            sb.append( line, 0, end ).append( '\n' );
        }
        // Remove trailing \n added above
        if( sb.length() > 0 && sb.charAt( sb.length() - 1 ) == '\n' ) sb.setLength( sb.length() - 1 );

        lex.input( sb.toString() );
        lex.lineno = 1;

        List<List<LexToken> > result      = new ArrayList<>();
        List<LexToken>        currentLine = new ArrayList<>();

        while( true ) {
            LexToken tok = lex.token();
            if( tok == null ) break;
            tok.source = abssource;
            currentLine.add( tok );
            if( t_WS.contains( tok.type ) && "\n".equals( tok.value ) ) {
                result.add( currentLine );
                currentLine = new ArrayList<>();
            }
        }

        if( !currentLine.isEmpty() ) {
            LexToken last  = currentLine.get( currentLine.size() - 1 );
            LexToken nltok = last.copy();
            nltok.type  = t_NEWLINE;
            nltok.value = "\n";
            currentLine.add( nltok );
            result.add( currentLine );
        }

        return result;
    }

    // -----------------------------------------------------------------------
    // tokenstrip() – mirrors preprocessor.py tokenstrip()
    // -----------------------------------------------------------------------

    protected List<LexToken> tokenstrip( List<LexToken> tokens )
    {
        int i = 0;
        while( i < tokens.size() && t_WS.contains( tokens.get( i ).type ) ) i++;
        if( i > 0 ) tokens = new ArrayList<>( tokens.subList( i, tokens.size() ) );
        int j = tokens.size() - 1;
        while( j >= 0 && t_WS.contains( tokens.get( j ).type ) ) j--;
        if( j < tokens.size() - 1 ) tokens = new ArrayList<>( tokens.subList( 0, j + 1 ) );
        return tokens;
    }

    // -----------------------------------------------------------------------
    // collect_args() – mirrors preprocessor.py collect_args()
    // Returns [tokencount, args, positions] packed as Object[]
    // -----------------------------------------------------------------------

    protected Object[] collect_args( List<LexToken> tokenlist, boolean ignore_errors )
    {
        List<List<LexToken> > args        = new ArrayList<>();
        List<Integer>         positions   = new ArrayList<>();
        List<LexToken>        current_arg = new ArrayList<>();
        int                   nesting     = 1;
        int                   tokenlen    = tokenlist.size();
        int                   i           = 0;

        while( i < tokenlen && t_WS.contains( tokenlist.get( i ).type ) ) i++;

        if( i < tokenlen && "(".equals( tokenlist.get( i ).value ) ) {
            positions.add( i + 1 );
        }
        else {
            if( !ignore_errors ) {
                LexToken first = tokenlist.isEmpty() ? null : tokenlist.get( 0 );
                if( first != null ) on_error( (String) first.source, first.lineno, "Missing '(' in macro arguments" );
            }
            return new Object[] {
                       0, Collections.emptyList(), Collections.emptyList()
            };
        }
        i++;

        while( i < tokenlen ) {
            LexToken t = tokenlist.get( i );
            if( "(".equals( t.value ) ) {
                current_arg.add( t );
                nesting++;
            }
            else if( ")".equals( t.value ) ) {
                nesting--;
                if( nesting == 0 ) {
                    args.add( tokenstrip( current_arg ) );
                    positions.add( i );
                    return new Object[] {
                               i + 1, args, positions
                    };
                }
                current_arg.add( t );
            }
            else if( ",".equals( t.value ) && nesting == 1 ) {
                args.add( tokenstrip( current_arg ) );
                positions.add( i + 1 );
                current_arg = new ArrayList<>();
            }
            else {
                current_arg.add( t );
            }
            i++;
        }

        if( !ignore_errors ) {
            LexToken last = tokenlist.isEmpty() ? null : tokenlist.get( tokenlist.size() - 1 );
            if( last != null ) on_error( (String) last.source, last.lineno, "Missing ')' in macro arguments" );
        }
        return new Object[] {
                   0, Collections.emptyList(), Collections.emptyList()
        };
    }

    // -----------------------------------------------------------------------
    // macro_prescan() – mirrors preprocessor.py macro_prescan()
    // -----------------------------------------------------------------------

    protected void macro_prescan( Macro macro )
    {
        macro.patch           = new ArrayList<>();
        macro.str_patch       = new ArrayList<>();
        macro.var_comma_patch = new ArrayList<>();

        Map<String, Integer> arglist_index = new LinkedHashMap<>();
        if( macro.arglist != null )
            for( int idx = 0; idx < macro.arglist.size(); idx++ ) arglist_index.put( macro.arglist.get( idx ), idx );

        int i = 0;
        while( i < macro.value.size() ) {
            LexToken tok = macro.value.get( i );
            if( t_ID.equals( tok.type ) && arglist_index.containsKey( tok.value ) ) {
                int argnum = arglist_index.get( (String) tok.value );
                // Check for # stringification
                int j      = i - 1;
                while( j >= 0 && t_WS.contains( macro.value.get( j ).type ) ) j--;
                if( j >= 0 && "#".equals( macro.value.get( j ).value ) ) {
                    LexToken copy = macro.value.get( i ).copy();
                    copy.type = t_STRING;
                    macro.value.set( i, copy );
                    while( i > j ) {
                        macro.value.remove( j );
                        i--;
                    }
                    macro.str_patch.add( new int[] {argnum, i} );
                }
                else if( i > 0 && "##".equals( macro.value.get( i - 1 ).value ) ) {
                    macro.patch.add( new Object[] {"t", argnum, i} );
                    i++;
                    continue;
                }
                else if( (i + 1) < macro.value.size() && "##".equals( macro.value.get( i + 1 ).value ) ) {
                    macro.patch.add( new Object[] {"t", argnum, i} );
                    i++;
                    continue;
                }
                else {
                    macro.patch.add( new Object[] {"e", argnum, i} );
                }
            }
            else if( "##".equals( tok.value ) ) {
                if( macro.variadic && i > 0 && ",".equals( macro.value.get( i - 1 ).value ) &&
                    (i + 1) < macro.value.size() && t_ID.equals( macro.value.get( i + 1 ).type ) &&
                    macro.vararg != null && macro.vararg.equals( macro.value.get( i + 1 ).value ) ) macro.var_comma_patch.add( i - 1 );
            }
            i++;
        }
        // Sort patch list in reverse order of position
        macro.patch.sort( (a, b) -> Integer.compare( (int) b[2], (int) a[2] ) );
    }

    // -----------------------------------------------------------------------
    // macro_expand_args() – mirrors preprocessor.py macro_expand_args()
    // -----------------------------------------------------------------------

    protected List<LexToken> macro_expand_args( Macro macro, List<List<LexToken> > args )
    {
        // Deep copy of macro value
        List<LexToken> rep = new ArrayList<>();
        for( LexToken t : macro.value ) rep.add( t.copy() );

        // String expansion patches
        Map<Integer, String> str_expansion = new LinkedHashMap<>();
        for( int[] strp : macro.str_patch ) {
            int argnum = strp[0], idx = strp[1];
            if( !str_expansion.containsKey( argnum ) ) {
                List<LexToken> tokens2 = new ArrayList<>();
                for( LexToken t : args.get( argnum ) ) tokens2.add( t.copy() );
                // Collapse non-space whitespace to space
                for( LexToken t : tokens2 )
                    if( t_WS.contains( t.type ) && !t_LINECONT.equals( t.type ) ) t.value = " ";
                // Collapse multiple consecutive whitespace
                int j = 0;
                while( j < tokens2.size() - 1 ) {
                    if( t_WS.contains( tokens2.get( j ).type ) && t_WS.contains( tokens2.get( j + 1 ).type ) ) {
                        tokens2.remove( j + 1 );
                    }
                    else { j++; }
                }
                StringBuilder strb = new StringBuilder();
                for( LexToken t : tokens2 ) strb.append( t.value );
                String        str  = strb.toString().replace( "\\", "\\\\" ).replace( "\"", "\\\"" );
                str_expansion.put( argnum, "\"" + str + "\"" );
            }
            LexToken copy = rep.get( idx ).copy();
            copy.value = str_expansion.get( argnum );
            rep.set( idx, copy );
        }

        // Variadic comma patch
        boolean comma_patch = false;
        if( macro.variadic && args.get( args.size() - 1 ).isEmpty() ) {
            for( int varIdx : macro.var_comma_patch ) {
                rep.set( varIdx, null );
                comma_patch = true;
            }
        }

        // Apply other patches in reverse order
        Map<Integer, List<LexToken> > expanded = new LinkedHashMap<>();
        for( Object[] patch : macro.patch ) {
            String ptype  = (String) patch[0];
            int    argnum = (int) patch[1];
            int    pIdx   = (int) patch[2];
            if( "t".equals( ptype ) ) {
                rep.subList( pIdx, pIdx + 1 ).clear();
                rep.addAll( pIdx, args.get( argnum ) );
            }
            else if( "e".equals( ptype ) ) {
                if( !expanded.containsKey( argnum ) ) expanded.put( argnum, expand_macros( new ArrayList<>( args.get( argnum ) ), new ArrayList<>() ) );
                rep.subList( pIdx, pIdx + 1 ).clear();
                rep.addAll( pIdx, expanded.get( argnum ) );
            }
        }

        // Remove null entries from comma patch
        if( comma_patch ) rep.removeIf( Objects::isNull );

        // Token concatenation pass (## stitching)
        while( !rep.isEmpty() && t_DPOUND.equals( rep.get( 0 ).type ) ) rep.remove( 0 );
        while( !rep.isEmpty() && t_DPOUND.equals( rep.get( rep.size() - 1 ).type ) ) rep.remove( rep.size() - 1 );

        boolean stitched = false;
        int     k        = 1;
        while( k < rep.size() - 1 ) {
            if( rep.get( k ) != null && t_DPOUND.equals( rep.get( k ).type ) ) {
                int j = k + 1;
                while( j < rep.size() && rep.get( j ) != null && t_DPOUND.equals( rep.get( j ).type ) ) j++;
                while( j < rep.size() && rep.get( j ) != null && t_WS.contains( rep.get( j ).type ) ) j++;
                if( j < rep.size() && rep.get( j ) != null ) {
                    LexToken prev = rep.get( k - 1 ).copy();
                    prev.type  = null;
                    prev.value = prev.value.toString() + rep.get( j ).value.toString();
                    rep.set( k - 1, prev );
                    while( j >= k ) {
                        rep.remove( k );
                        j--;
                    }
                    stitched = true;
                }
            }
            else {
                k++;
            }
        }

        if( stitched ) {
            // Re-lex any stitched tokens
            CppLexer lex2 = lexer.clone();
            int      ki   = 0;
            while( ki < rep.size() ) {
                LexToken t = rep.get( ki );
                if( t != null && t.type == null ) {
                    lex2.input( t.value.toString() );
                    List<LexToken> toks2 = new ArrayList<>();
                    LexToken       tt;
                    while( (tt = lex2.token() ) != null ) toks2.add( tt );
                    if( !toks2.isEmpty() ) {
                        rep.set( ki, toks2.get( 0 ) );
                        for( int m = 1; m < toks2.size(); m++ ) rep.add( ki + m, toks2.get( m ) );
                        ki += toks2.size();
                    }
                    else {
                        rep.remove( ki );
                    }
                }
                else {
                    ki++;
                }
            }
        }

        return rep;
    }

    // -----------------------------------------------------------------------
    // expand_macros() – mirrors preprocessor.py expand_macros()
    // -----------------------------------------------------------------------

    public List<LexToken> expand_macros( List<LexToken> tokens, List<String> expanding_from )
    {
        for( LexToken tok : tokens )
            if( tok.expanded_from == null ) tok.expanded_from = new ArrayList<>();
        int i = 0;
        while( i < tokens.size() ) {
            LexToken t = tokens.get( i );
            if( linemacrodepth == 0 ) linemacro = t.lineno;
            linemacrodepth++;
            if( t_ID.equals( t.type ) ) {
                String name = (String) t.value;
                if( macros.containsKey( name ) && !t.expanded_from.contains( name ) && !expanding_from.contains( name ) ) {
                    Macro m = macros.get( name );
                    if( m.arglist == null ) {
                        // Simple macro
                        List<LexToken> rep              = new ArrayList<>();
                        for( LexToken x : m.value ) rep.add( x.copy() );
                        List<String>   newExpandingFrom = new ArrayList<>( expanding_from );
                        newExpandingFrom.add( name );
                        List<LexToken> ex               = expand_macros( rep, newExpandingFrom );
                        for( LexToken e : ex ) {
                            e.source = t.source;
                            e.lineno = t.lineno;
                            if( e.expanded_from == null ) e.expanded_from = new ArrayList<>();
                            e.expanded_from.add( name );
                        }
                        tokens.subList( i, i + 1 ).clear();
                        tokens.addAll( i, ex );
                        linemacrodepth--;
                        if( linemacrodepth == 0 ) linemacro = 0;
                        continue;
                    }
                    else {
                        // Macro with arguments
                        int j = i + 1;
                        while( j < tokens.size() && (t_WS.contains( tokens.get( j ).type ) || t_COMMENT.contains( tokens.get( j ).type ) ) ) j++;
                        if( j == tokens.size() || !"(".equals( tokens.get( j ).value ) ) {
                            i = j;
                        }
                        else {
                            Object[] result            = collect_args( tokens.subList( j, tokens.size() ), true );
                            int      tokcount          = (int) result[0];
                            @SuppressWarnings("unchecked")
                            List<List<LexToken> > args = (List<List<LexToken> >) result[1];
                            if( tokcount == 0 ) break;

                            boolean argsOk             = true;
                            if( !m.variadic && (args.size() != 1 || !args.get( 0 ).isEmpty() || m.arglist.size() > 1) &&
                                args.size() != m.arglist.size() ) {
                                on_error( (String) t.source, t.lineno,
                                    String.format( "Macro %s requires %d arguments but was passed %d",
                                        t.value, m.arglist.size(), args.size() ) );
                                i      = j + tokcount;
                                argsOk = false;
                            }
                            else if( m.variadic && args.size() < m.arglist.size() - 1 ) {
                                on_error( (String) t.source, t.lineno,
                                    String.format( "Macro %s must have at least %d argument(s)", t.value, m.arglist.size() - 1 ) );
                                i      = j + tokcount;
                                argsOk = false;
                            }

                            if( argsOk ) {
                                if( m.variadic ) {
                                    if( args.size() == m.arglist.size() - 1 ) {
                                        args.add( new ArrayList<>() );
                                    }
                                    else {
                                        @SuppressWarnings("unchecked")
                                        List<Integer> positions2 = (List<Integer>) result[2];
                                        int            startIdx  = positions2.get( m.arglist.size() - 1 );
                                        List<LexToken> varArgs   = new ArrayList<>(
                                            tokens.subList( j + startIdx, j + tokcount - 1 ) );
                                        while( args.size() > m.arglist.size() - 1 ) args.remove( args.size() - 1 );
                                        args.add( varArgs );
                                    }
                                }
                                else {
                                    while( args.size() < m.arglist.size() ) args.add( new ArrayList<>() );
                                }

                                List<LexToken> rep              = macro_expand_args( m, args );
                                List<String>   newExpandingFrom = new ArrayList<>( expanding_from );
                                newExpandingFrom.add( name );
                                List<LexToken> ex               = expand_macros( rep, newExpandingFrom );
                                for( LexToken e : ex ) {
                                    e.source = t.source;
                                    e.lineno = t.lineno;
                                    if( e.expanded_from == null ) e.expanded_from = new ArrayList<>();
                                    e.expanded_from.add( name );
                                }
                                // Non-conforming extension: insert space if next token is an identifier
                                if( tokens.size() > j + tokcount && t_ID.equals( tokens.get( j + tokcount ).type ) ) {
                                    LexToken spTok = tokens.get( j + tokcount ).copy();
                                    spTok.type  = t_SPACE;
                                    spTok.value = " ";
                                    ex.add( spTok );
                                }
                                tokens.subList( i, j + tokcount ).clear();
                                tokens.addAll( i, ex );
                            }
                        }
                        linemacrodepth--;
                        if( linemacrodepth == 0 ) linemacro = 0;
                        continue;
                    }
                }
                else if( expand_linemacro && "__LINE__".equals( t.value ) ) {
                    t.type  = t_INTEGER;
                    t.value = linemacro;
                }
                else if( expand_countermacro && "__COUNTER__".equals( t.value ) ) {
                    t.type  = t_INTEGER;
                    t.value = countermacro++;
                }
            }
            i++;
            linemacrodepth--;
            if( linemacrodepth == 0 ) linemacro = 0;
        }
        return tokens;
    }

    // -----------------------------------------------------------------------
    // evalexpr() – mirrors preprocessor.py evalexpr()
    // Returns [result (long), rewritten tokens or null]
    // -----------------------------------------------------------------------

    protected Object[] evalexpr( List<LexToken> tokens )
    {
        if( tokens.isEmpty() ) {
            on_error( "unknown", 0, "Empty expression" );
            return new Object[] {
                       0L, null
            };
        }

        final boolean[] partial_expansion = {
            false
        };

        // Replace defined(macro) occurrences
        Function<List<LexToken>, List<LexToken> > replace_defined = (toks) -> {
            int i = 0;
            while( i < toks.size() ) {
                if( t_ID.equals( toks.get( i ).type ) && "defined".equals( toks.get( i ).value ) ) {
                    int     j         = i + 1;
                    boolean needparen = false;
                    String  result2   = "0L";
                    while( j < toks.size() ) {
                        if( t_WS.contains( toks.get( j ).type ) ) { j++; continue; }
                        if( t_ID.equals( toks.get( j ).type ) ) {
                            if( macros.containsKey( toks.get( j ).value ) ) {
                                result2 = "1L";
                            }
                            else {
                                Boolean repl = on_unknown_macro_in_defined_expr( toks.get( j ) );
                                if( repl == null ) {
                                    partial_expansion[0] = true;
                                    result2              = "defined(" + toks.get( j ).value + ")";
                                }
                                else {
                                    result2 = repl ? "1L" : "0L";
                                }
                            }
                            if( !needparen ) break;
                        }
                        else if( "(".equals( toks.get( j ).value ) ) {
                            needparen = true;
                        }
                        else if( ")".equals( toks.get( j ).value ) ) {
                            break;
                        }
                        else {
                            on_error( (String) toks.get( i ).source, toks.get( i ).lineno, "Malformed defined()" );
                        }
                        j++;
                    }
                    if( result2.startsWith( "defined" ) ) {
                        toks.get( i ).type  = t_ID;
                        toks.get( i ).value = result2;
                    }
                    else {
                        toks.get( i ).type  = t_INTEGER;
                        toks.get( i ).value = result2;
                    }
                    // Remove tokens i+1..j
                    if( j + 1 <= toks.size() ) toks.subList( i + 1, Math.min( j + 1, toks.size() ) ).clear();
                }
                i++;
            }
            return toks;
        };

        tokens = replace_defined.apply( tokens );
        tokens = expand_macros( tokens, new ArrayList<>() );
        tokens = replace_defined.apply( tokens );

        if( tokens.isEmpty() ) return new Object[] {
                       0L, null
            };

        // Build hooks for the evaluator
        Map<String, Function<Value, Value> > evalfuncts = new HashMap<String, Function<Value, Value> >() {
            final boolean[] pe = {
                false
            };
            @Override
            public boolean containsKey( Object key ) { return true; }
            @Override
            public Function<Value, Value> get( Object key )
            {
                String k = (String) key;
                if( k.startsWith( "defined(" ) ) {
                    pe[0] = true;
                    return x -> new Value( 0L );
                }
                Function<Value, Value> repl = on_unknown_macro_function_in_expr( k );
                if( repl == null ) {
                    pe[0] = true;
                    return x -> new Value( 0L );
                }
                return repl;
            }
        };
        final boolean[]     evalFunctPartial = {
            false
        };
        final boolean[]     evalVarPartial   = {
            false
        };

        Map<String, Object> evalvars = new HashMap<String, Object>() {
            @Override
            public boolean containsKey( Object key ) { return true; }
            @Override
            public Object get( Object key )
            {
                String k = (String) key;
                if( k.startsWith( "defined(" ) ) {
                    evalVarPartial[0] = true;
                    return 0L;
                }
                Object repl = on_unknown_macro_in_expr( k );
                if( repl == null ) {
                    evalVarPartial[0] = true;
                    return k; // pass through as identifier name
                }
                return repl;
            }
        };

        long result = 0;
        try {
            Value val = evaluator.evaluate( tokens, evalfuncts, evalvars );
            partial_expansion[0] = partial_expansion[0] || evalFunctPartial[0] || evalVarPartial[0];
            result               = val.value();
        } catch( OutputDirective e ) {
            throw e;
        }
        catch( Exception e ) {
            partial_expansion[0] = partial_expansion[0] || evalFunctPartial[0] || evalVarPartial[0];
            if( !partial_expansion[0] ) {
                StringBuilder sb = new StringBuilder();
                for( LexToken t : tokens ) sb.append( t.value );
                on_error( (String) tokens.get( 0 ).source, tokens.get( 0 ).lineno,
                    "Could not evaluate expression due to " + e + " (passed to evaluator: '" + sb + "')" );
            }
            result = 0;
        }

        return new Object[] {
                   result, partial_expansion[0] ? tokens : null
        };
    }

    // -----------------------------------------------------------------------
    // parsegen() – mirrors preprocessor.py parsegen()
    // Returns an iterable of LexToken
    // -----------------------------------------------------------------------

    public Iterable<LexToken> parsegen( String input, String source, String abssource )
    {
        List<LexToken> output           = new ArrayList<>();

        String         rewritten_source = source;
        if( abssource != null ) {
            rewritten_source = abssource;
            for( Object[] rewrite : rewrite_paths ) {
                Pattern pat  = (Pattern) rewrite[0];
                String  repl = (String) rewrite[1];
                Matcher m    = pat.matcher( rewritten_source );
                if( m.find() ) {
                    rewritten_source = m.replaceFirst( repl );
                    if( !File.separator.equals( "/" ) ) rewritten_source = rewritten_source.replace( File.separator, "/" );
                    break;
                }
            }
        }

        // Replace trigraph sequences
        String                    t     = CppParser.trigraph( input );
        Iterable<List<LexToken> > lines = group_lines( t, rewritten_source );

        if( source == null ) source = "";
        if( rewritten_source == null ) rewritten_source = "";

        int    my_include_times_idx = include_times.size();
        String including_file       = macros.containsKey( "__FILE__" ) ? (String) macros.get( "__FILE__" ).value.get( 0 ).value : null;
        include_times.add( new FileInclusionTime( including_file, source, abssource, include_depth ) );
        include_depth++;
        long my_include_time_begin  = System.nanoTime();

        if( expand_filemacro ) define( "__FILE__ \"" + rewritten_source + "\"" );

        this.source = abssource;
        List<LexToken> chunk      = new ArrayList<>();
        boolean        enable     = true;
        boolean        iftrigger  = false;
        boolean        ifpassthru = false;

        class IfStackEntry {
            boolean        enable, iftrigger, ifpassthru, rewritten;
            List<LexToken> startlinetoks;
            IfStackEntry( boolean enable, boolean iftrigger, boolean ifpassthru, List<LexToken> startlinetoks )
            {
                this.enable     = enable; this.iftrigger = iftrigger;
                this.ifpassthru = ifpassthru; this.startlinetoks = startlinetoks;
            }
        } // class IfStackEntry

        Deque<IfStackEntry> ifstack                   = new ArrayDeque<>();
        boolean             at_front_of_file          = true;
        boolean             auto_pragma_once_possible = auto_pragma_once_enabled;
        String[]            include_guard             = {
            null, null
        }; // [macro, "0"/"1"]

        on_potential_include_guard( null );

        for( List<LexToken> x : lines ) {
            boolean all_whitespace                       = true;
            boolean skip_auto_pragma_once_possible_check = false;

            // Handle comments
            for( LexToken tok2 : x )
                if( t_COMMENT.contains( tok2.type ) ) {
                    if( !Boolean.TRUE.equals( on_comment( tok2 ) ) ) {
                        if( t_COMMENT1.equals( tok2.type ) ) {
                            tok2.value = " ";
                        }
                        else if( t_COMMENT2.equals( tok2.type ) ) {
                            tok2.value = "\n";
                        }
                        tok2.type = CppTokens.CPP_WS;
                    }
                }

            // Skip over whitespace
            LexToken tok = null;
            int      xi  = 0;
            for(; xi < x.size(); xi++ ) {
                tok = x.get( xi );
                if( !t_WS.contains( tok.type ) && !t_COMMENT.contains( tok.type ) ) {
                    all_whitespace = false;
                    break;
                }
            }

            boolean output_and_expand_line = true;
            boolean output_unexpanded_line = false;

            if( tok != null && "#".equals( tok.value ) ) {
                List<LexToken> precedingtoks = new ArrayList<>();
                precedingtoks.add( tok );
                output_and_expand_line = false;

                try {
                    xi++;
                    while( xi < x.size() && t_WS.contains( x.get( xi ).type ) ) {
                        precedingtoks.add( x.get( xi ) );
                        xi++;
                    }
                    List<LexToken> dirtokens = tokenstrip( x.subList( xi, x.size() ) );
                    String         name      = "";
                    List<LexToken> args      = new ArrayList<>();

                    Boolean handling = null;
                    if( !dirtokens.isEmpty() ) {
                        name = (String) dirtokens.get( 0 ).value;
                        args = tokenstrip( new ArrayList<>( dirtokens.subList( 1, dirtokens.size() ) ) );

                        if( debugout != null ) {
                            StringBuilder sb = new StringBuilder();
                            for( LexToken argt : args ) sb.append( argt.value );
                            debugout.printf( "%b:%b:%b %s:%d #%s %s%n",
                                enable, iftrigger, ifpassthru,
                                dirtokens.get( 0 ).source, dirtokens.get( 0 ).lineno,
                                dirtokens.get( 0 ).value, sb );
                        }

                        handling = on_directive_handle( dirtokens.get( 0 ), args, ifpassthru, precedingtoks );
                        assert handling == null || handling;
                    }
                    else {
                        name = "";
                        args = new ArrayList<>();
                        throw new OutputDirective( Action.IgnoreAndRemove );
                    }

                    switch( name ) {
                        case "define":
                            at_front_of_file = false;
                            if( enable ) {
                                for( LexToken emtok : expand_macros( chunk, new ArrayList<>() ) ) output.add( emtok );
                                chunk = new ArrayList<>();
                                if( include_guard[0] != null && "0".equals( include_guard[1] ) ) {
                                    if( !args.isEmpty() && include_guard[0].equals( args.get( 0 ).value ) && args.size() == 1 ) {
                                        include_guard[1] = "1";
                                        if( ifpassthru && !ifstack.isEmpty() && !ifstack.peek().ifpassthru ) ifpassthru = false;
                                    }
                                }
                                define( args );
                                if( debugout != null )
                                    debugout.printf( "%b:%b:%b %s:%d      %s%n",
                                        enable, iftrigger, ifpassthru,
                                        dirtokens.get( 0 ).source, dirtokens.get( 0 ).lineno,
                                        macros.get( args.get( 0 ).value ) );
                                if( handling == null ) output.addAll( x );
                            }
                            break;

                        case "include":
                            if( enable ) {
                                for( LexToken emtok : expand_macros( chunk, new ArrayList<>() ) ) output.add( emtok );
                                chunk = new ArrayList<>();
                                Macro oldfile = macros.get( "__FILE__" );
                                if( !args.isEmpty() && !"<".equals( args.get( 0 ).value ) && !t_STRING.equals( args.get( 0 ).type ) ) args = tokenstrip( expand_macros( args, new ArrayList<>() ) );
                                for( LexToken emtok : include( args, x ) ) output.add( emtok );
                                if( oldfile != null ) macros.put( "__FILE__", oldfile );
                                this.source = abssource;
                            }
                            break;

                        case "undef":
                            at_front_of_file = false;
                            if( enable ) {
                                for( LexToken emtok : expand_macros( chunk, new ArrayList<>() ) ) output.add( emtok );
                                chunk = new ArrayList<>();
                                undef( args );
                                if( handling == null ) output.addAll( x );
                            }
                            break;

                        case "ifdef":
                            at_front_of_file = false;
                            ifstack.push( new IfStackEntry( enable, iftrigger, ifpassthru, new ArrayList<>( x ) ) );
                            if( enable ) {
                                ifpassthru = false;
                                String macname = (String) args.get( 0 ).value;
                                if( !macros.containsKey( macname ) ) {
                                    Boolean res = on_unknown_macro_in_defined_expr( args.get( 0 ) );
                                    if( res == null ) {
                                        ifpassthru = true;
                                        ifstack.peek().rewritten = true;
                                        throw new OutputDirective( Action.IgnoreAndPassThrough );
                                    }
                                    else if( res ) {
                                        iftrigger = true;
                                    }
                                    else {
                                        enable = false; iftrigger = false;
                                    }
                                }
                                else {
                                    iftrigger = true;
                                }
                            }
                            break;

                        case "ifndef":
                            if( ifstack.isEmpty() && at_front_of_file ) {
                                on_potential_include_guard( (String) args.get( 0 ).value );
                                include_guard[0] = (String) args.get( 0 ).value;
                                include_guard[1] = "0";
                            }
                            at_front_of_file = false;
                            ifstack.push( new IfStackEntry( enable, iftrigger, ifpassthru, new ArrayList<>( x ) ) );
                            if( enable ) {
                                ifpassthru = false;
                                String macname = (String) args.get( 0 ).value;
                                if( macros.containsKey( macname ) ) {
                                    enable = false; iftrigger = false;
                                }
                                else {
                                    Boolean res = on_unknown_macro_in_defined_expr( args.get( 0 ) );
                                    if( res == null ) {
                                        ifpassthru = true;
                                        ifstack.peek().rewritten = true;
                                        throw new OutputDirective( Action.IgnoreAndPassThrough );
                                    }
                                    else if( res ) {
                                        enable = false; iftrigger = false;
                                    }
                                    else {
                                        iftrigger = true;
                                    }
                                }
                            }
                            break;

                        case "if":
                            if( ifstack.isEmpty() && at_front_of_file && args.size() >= 3 &&
                                "!".equals( args.get( 0 ).value ) && "defined".equals( args.get( 1 ).value ) ) {
                                int n = 2;
                                if( "(".equals( args.get( n ).value ) ) n++;
                                on_potential_include_guard( (String) args.get( n ).value );
                                include_guard[0] = (String) args.get( n ).value;
                                include_guard[1] = "0";
                            }
                            at_front_of_file = false;
                            ifstack.push( new IfStackEntry( enable, iftrigger, ifpassthru, new ArrayList<>( x ) ) );
                            if( enable ) {
                                iftrigger = false; ifpassthru = false;
                                Object[] exprResult      = evalexpr( args );
                                long     result          = (long) exprResult[0];
                                @SuppressWarnings("unchecked")
                                List<LexToken> rewritten = (List<LexToken>) exprResult[1];
                                if( rewritten != null ) {
                                    // Rebuild x with rewritten expression
                                    ifpassthru = true;
                                    ifstack.peek().rewritten = true;
                                    throw new OutputDirective( Action.IgnoreAndPassThrough );
                                }
                                if( result == 0 ) {enable = false;}
                                else {iftrigger = true;}
                            }
                            break;

                        case "elif":
                            at_front_of_file = false;
                            if( !ifstack.isEmpty() ) {
                                IfStackEntry top = ifstack.peek();
                                if( top.enable ) {
                                    if( enable && !ifpassthru ) {
                                        enable = false;
                                    }
                                    else if( !iftrigger ) {
                                        Object[] exprResult      = evalexpr( args );
                                        long     result          = (long) exprResult[0];
                                        @SuppressWarnings("unchecked")
                                        List<LexToken> rewritten = (List<LexToken>) exprResult[1];
                                        if( rewritten != null ) {
                                            enable     = true;
                                            ifpassthru = true;
                                            ifstack.peek().rewritten = true;
                                            throw new OutputDirective( Action.IgnoreAndPassThrough );
                                        }
                                        if( ifpassthru ) {
                                            if( result != 0 ) throw new OutputDirective( Action.IgnoreAndPassThrough );
                                            enable = false;
                                        }
                                        else if( result != 0 ) {
                                            enable = true; iftrigger = true;
                                        }
                                    }
                                }
                            }
                            else if( !dirtokens.isEmpty() ) {on_error( (String) dirtokens.get( 0 ).source, dirtokens.get( 0 ).lineno, "Misplaced #elif" );}
                            break;

                        case "else":
                            at_front_of_file = false;
                            if( !ifstack.isEmpty() ) {
                                IfStackEntry top = ifstack.peek();
                                if( top.enable ) {
                                    if( ifpassthru ) {
                                        enable = true;
                                        throw new OutputDirective( Action.IgnoreAndPassThrough );
                                    }
                                    if( enable ) {enable = false;}
                                    else if( !iftrigger ) { enable = true; iftrigger = true; }
                                }
                            }
                            else if( !dirtokens.isEmpty() ) {on_error( (String) dirtokens.get( 0 ).source, dirtokens.get( 0 ).lineno, "Misplaced #else" );}
                            break;

                        case "endif":
                            at_front_of_file = false;
                            if( !ifstack.isEmpty() ) {
                                IfStackEntry oldEntry = ifstack.pop();
                                enable                               = oldEntry.enable;
                                iftrigger                            = oldEntry.iftrigger;
                                ifpassthru                           = oldEntry.ifpassthru;
                                skip_auto_pragma_once_possible_check = true;
                                if( oldEntry.rewritten ) throw new OutputDirective( Action.IgnoreAndPassThrough );
                            }
                            else if( !dirtokens.isEmpty() ) {on_error( (String) dirtokens.get( 0 ).source, dirtokens.get( 0 ).lineno, "Misplaced #endif" );}
                            break;

                        case "pragma":
                            if( !args.isEmpty() && "once".equals( args.get( 0 ).value ) )
                                if( enable ) include_once.put( this.source, null );
                            break;

                        default:
                            if( enable ) {
                                Boolean result2 = on_directive_unknown( dirtokens.isEmpty() ? null : dirtokens.get( 0 ),
                                    args, ifpassthru, precedingtoks );
                                output_unexpanded_line = (result2 == null);
                            }
                            break;
                    } // switch

                } catch( OutputDirective e ) {
                    if( e.action == Action.IgnoreAndPassThrough ) {
                        output_unexpanded_line = true;
                    }
                    else if( e.action == Action.IgnoreAndRemove ) {
                        // nothing
                    }
                }
            }

            // Auto pragma once check
            if( !skip_auto_pragma_once_possible_check && auto_pragma_once_possible &&
                ifstack.isEmpty() && !all_whitespace ) auto_pragma_once_possible = false;

            if( output_and_expand_line || output_unexpanded_line ) {
                if( !all_whitespace ) at_front_of_file = false;
                if( enable ) {
                    if( output_and_expand_line ) {
                        chunk.addAll( x );
                    }
                    else {
                        for( LexToken emtok : expand_macros( chunk, new ArrayList<>() ) ) output.add( emtok );
                        chunk = new ArrayList<>();
                        output.addAll( x );
                    }
                }
                else {
                    // Need to keep blank lines
                    int ki = 0;
                    while( ki < x.size() ) {
                        if( !t_WS.contains( x.get( ki ).type ) ) {x.remove( ki );}
                        else {ki++;}
                    }
                    chunk.addAll( x );
                }
            }
        }

        for( LexToken emtok : expand_macros( chunk, new ArrayList<>() ) ) output.add( emtok );

        for( IfStackEntry entry : ifstack )
            if( !entry.startlinetoks.isEmpty() ) {
                StringBuilder sb = new StringBuilder();
                for( LexToken nt : entry.startlinetoks ) sb.append( nt.value );
                on_error( (String) entry.startlinetoks.get( 0 ).source,
                    entry.startlinetoks.get( 0 ).lineno, "Unterminated " + sb );
            }

        if( auto_pragma_once_possible && include_guard[0] != null && "1".equals( include_guard[1] ) ) include_once.put( this.source, include_guard[0] );

        double elapsed = (System.nanoTime() - my_include_time_begin) / 1e9;
        include_times.get( my_include_times_idx ).elapsed = elapsed;
        include_depth--;

        return output;
    }

    // -----------------------------------------------------------------------
    // include() – mirrors preprocessor.py include()
    // -----------------------------------------------------------------------

    public Iterable<LexToken> include( List<LexToken> tokens, List<LexToken> original_line )
    {
        List<LexToken> output = new ArrayList<>();
        if( tokens.isEmpty() ) return output;

        if( !"<".equals( tokens.get( 0 ).value ) && !t_STRING.equals( tokens.get( 0 ).type ) ) tokens = tokenstrip( expand_macros( tokens, new ArrayList<>() ) );

        boolean      is_system_include = false;
        String       filename;
        List<String> searchPath;

        if( "<".equals( tokens.get( 0 ).value ) ) {
            is_system_include = true;
            int i = 1;
            while( i < tokens.size() && !">".equals( tokens.get( i ).value ) ) i++;
            if( i >= tokens.size() ) {
                on_error( (String) tokens.get( 0 ).source, tokens.get( 0 ).lineno, "Malformed #include <...>" );
                return output;
            }
            StringBuilder sb = new StringBuilder();
            for( LexToken t : tokens.subList( 1, i ) ) sb.append( t.value );
            filename   = sb.toString();
            searchPath = new ArrayList<>( path );
        }
        else if( t_STRING.equals( tokens.get( 0 ).type ) ) {
            String raw = (String) tokens.get( 0 ).value;
            filename   = raw.substring( 1, raw.length() - 1 );
            searchPath = new ArrayList<>( temp_path );
            searchPath.addAll( path );
        }
        else {
            String incPath = (String) tokens.get( 0 ).value;
            String curdir  = temp_path.isEmpty() ? "" : temp_path.get( 0 );
            on_include_not_found( true, false, curdir, incPath );
            return output;
        }

        if( searchPath.isEmpty() ) searchPath.add( "" );

        while( true ) {
            boolean found = false;
            for( String p : searchPath ) {
                String iname     = p.isEmpty() ? filename : Paths.get( p, filename ).toString();
                String fulliname = Paths.get( iname ).toAbsolutePath().toString();
                if( include_once.containsKey( fulliname ) ) {
                    if( passthru_includes != null ) {
                        StringBuilder sb = new StringBuilder();
                        for( LexToken t : tokens ) sb.append( t.value );
                        if( passthru_includes.matcher( sb.toString() ).find() ) output.addAll( original_line );
                    }
                    return output;
                }
                try {
                    if( !_file_cache.containsKey( fulliname ) ) {
                        try (Reader rdr = on_file_open( is_system_include, fulliname ) ) {
                            StringBuilder sb  = new StringBuilder();
                            char[]        buf = new char[8192];
                            int           n;
                            while( (n = rdr.read( buf ) ) != -1 ) sb.append( buf, 0, n );
                            _file_cache.put( fulliname, sb.toString() );
                        }
                    }
                    String data  = _file_cache.get( fulliname );
                    String dname = Paths.get( fulliname ).getParent() != null ?
                                   Paths.get( fulliname ).getParent().toString() : "";

                    if( !dname.isEmpty() ) temp_path.add( 0, dname );

                    StringBuilder tokStr = new StringBuilder();
                    for( LexToken t : tokens ) tokStr.append( t.value );

                    if( passthru_includes != null && passthru_includes.matcher( tokStr.toString() ).find() ) {
                        output.addAll( original_line );
                        for( LexToken ignored2 : parsegen( data, filename, fulliname ) ) {} // parse but discard
                    }
                    else {
                        for( LexToken t : parsegen( data, filename, fulliname ) ) output.add( t );
                    }

                    if( !dname.isEmpty() ) temp_path.remove( 0 );
                    return output;
                } catch( IOException e2 ) {
                    // Try next path
                }
            }

            // Not found
            String curdir  = temp_path.isEmpty() ? "" : temp_path.get( 0 );
            String newPath = on_include_not_found( false, is_system_include, curdir, filename );
            assert newPath != null;
            searchPath.add( newPath );
        }
    }

    // -----------------------------------------------------------------------
    // define() – mirrors preprocessor.py define()
    // -----------------------------------------------------------------------

    public void define( String tokens )
    {
        define( tokenize( tokens ) );
    }

    public void define( List<LexToken> tokens )
    {
        List<LexToken> linetok = new ArrayList<>();
        for( LexToken t : tokens ) linetok.add( t.copy() );

        if( linetok.isEmpty() ) return;
        LexToken nameToken = linetok.get( 0 );
        LexToken mtype     = linetok.size() > 1 ? linetok.get( 1 ) : null;

        Macro    m;
        if( mtype == null ) {
            m = new Macro( nameToken.value.toString(), new ArrayList<>(), null, false );
        }
        else if( t_WS.contains( mtype.type ) ) {
            m = new Macro( nameToken.value.toString(), tokenstrip( linetok.subList( 2, linetok.size() ) ), null, false );
        }
        else if( "(".equals( mtype.value ) ) {
            Object[] result            = collect_args( linetok.subList( 1, linetok.size() ), false );
            int      tokcount          = (int) result[0];
            @SuppressWarnings("unchecked")
            List<List<LexToken> > args = (List<List<LexToken> >) result[1];
            boolean      variadic      = false;
            List<String> argnames      = new ArrayList<>();
            boolean      parseOk       = true;
            for( List<LexToken> a : args ) {
                if( variadic ) {
                    on_error( (String) nameToken.source, nameToken.lineno,
                        "No more arguments may follow a variadic argument" );
                    parseOk = false; break;
                }
                StringBuilder astr    = new StringBuilder();
                for( LexToken at : a ) astr.append( at.value );
                String        astrStr = astr.toString();
                if( "...".equals( astrStr ) ) {
                    variadic         = true;
                    a.get( 0 ).type  = t_ID;
                    a.get( 0 ).value = "__VA_ARGS__";
                    argnames.add( "__VA_ARGS__" );
                    continue;
                }
                else if( astrStr.endsWith( "..." ) && !a.isEmpty() && t_ID.equals( a.get( 0 ).type ) ) {
                    variadic = true;
                    String argname = (String) a.get( 0 ).value;
                    if( argname.endsWith( "..." ) ) argname = argname.substring( 0, argname.length() - 3 );
                    argnames.add( argname );
                    continue;
                }
                if( a.isEmpty() && args.size() == 1 ) { argnames.add( "" ); continue; }
                if( a.size() > 1 || (a.size() == 1 && !t_ID.equals( a.get( 0 ).type ) ) ) {
                    on_error( (String) a.get( 0 ).source, a.get( 0 ).lineno, "Invalid macro argument" );
                    parseOk = false; break;
                }
                if( !a.isEmpty() ) argnames.add( (String) a.get( 0 ).value );
            }
            if( parseOk ) {
                List<LexToken> mvalue = tokenstrip( linetok.subList( 1 + tokcount, linetok.size() ) );
                // Strip whitespace adjacent to ##
                int            ki     = 0;
                while( ki < mvalue.size() ) {
                    if( ki + 1 < mvalue.size() ) {
                        if( t_WS.contains( mvalue.get( ki ).type ) && "##".equals( mvalue.get( ki + 1 ).value ) ) {
                            mvalue.remove( ki ); continue;
                        }
                        else if( "##".equals( mvalue.get( ki ).value ) && t_WS.contains( mvalue.get( ki + 1 ).type ) ) {
                            mvalue.remove( ki + 1 ); continue;
                        }
                    }
                    ki++;
                }
                m = new Macro( nameToken.value.toString(), mvalue, argnames, variadic );
                macro_prescan( m );
            }
            else {
                return;
            }
        }
        else {
            on_error( (String) nameToken.source, nameToken.lineno, "Bad macro definition" );
            return;
        }

        m.source = (String) nameToken.source;
        m.lineno = nameToken.lineno;
        macros.put( nameToken.value.toString(), m );
    }

    // -----------------------------------------------------------------------
    // undef() – mirrors preprocessor.py undef()
    // -----------------------------------------------------------------------

    public void undef( String tokens ) { undef( tokenize( tokens ) ); }

    public void undef( List<LexToken> tokens )
    {
        if( !tokens.isEmpty() ) macros.remove( tokens.get( 0 ).value.toString() );
    }

    // -----------------------------------------------------------------------
    // parse() – mirrors preprocessor.py parse()
    // -----------------------------------------------------------------------

    public void parse( String input, String source )
    {
        this.ignore = new HashSet<>();
        String absSource = source != null ? Paths.get( source ).toAbsolutePath().toString() : null;
        if( source != null ) {
            String dname = Paths.get( source ).getParent() != null ?
                           Paths.get( source ).getParent().toString() : "";
            temp_path.add( 0, dname );
        }
        this.parser = parsegen( input, source, absSource ).iterator();
    }

    public void parse( Reader input ) throws IOException
    {
        String        name = "<stdin>";
        StringBuilder sb   = new StringBuilder();
        char[]        buf  = new char[8192];
        int           n;
        while( (n = input.read( buf ) ) != -1 ) sb.append( buf, 0, n );
        parse( sb.toString(), name );
    }

    // -----------------------------------------------------------------------
    // token() – mirrors preprocessor.py token()
    // -----------------------------------------------------------------------

    public LexToken token()
    {
        if( parser == null ) return null;
        while( parser.hasNext() ) {
            LexToken tok = parser.next();
            if( ignore == null || !ignore.contains( tok.type ) ) return tok;
        }
        this.parser = null;
        return null;
    }

    // -----------------------------------------------------------------------
    // write() – mirrors preprocessor.py write()
    // -----------------------------------------------------------------------

    public void write( PrintWriter oh )
    {
        write( (Writer) oh );
    }

    public void write( Writer oh )
    {
        try {
            int     lastlineno = 0;
            String  lastsource = null;
            boolean done       = false;
            int     blanklines = 0;

            while( !done ) {
                boolean        emitlinedirective = false;
                List<LexToken> toks              = new ArrayList<>();
                boolean        all_ws            = true;

                // Accumulate a line
                while( !done ) {
                    LexToken tok2 = token();
                    if( tok2 == null ) { done = true; break; }
                    toks.add( tok2 );
                    String val    = tok2.value != null ? tok2.value.toString() : "";
                    if( !val.isEmpty() && val.charAt( 0 ) == '\n' ) break;
                    if( !t_WS.contains( tok2.type ) ) all_ws = false;
                }

                if( toks.isEmpty() ) break;

                if( all_ws ) {
                    if( toks.size() > 1 ) {
                        LexToken last = toks.get( toks.size() - 1 );
                        toks = new ArrayList<>();
                        toks.add( last );
                    }
                    String v = toks.get( 0 ).value != null ? toks.get( 0 ).value.toString() : "";
                    for( char c : v.toCharArray() ) if( c == '\n' ) blanklines++;
                    continue;
                }

                // Filter out line continuations
                for( int n = toks.size() - 1; n >= 0; n-- )
                    if( t_LINECONT != null && t_LINECONT.equals( toks.get( n ).type ) ) {
                        if( n > 0 && n < toks.size() - 2 &&
                            t_WS.contains( toks.get( n - 1 ).type ) &&
                            t_WS.contains( toks.get( n + 1 ).type ) ) {
                            if( !t_LINECONT.equals( toks.get( n - 1 ).type ) ) {
                                String prevVal = toks.get( n - 1 ).value.toString();
                                toks.get( n - 1 ).value = String.valueOf( prevVal.charAt( 0 ) );
                                toks.subList( n, n + 2 ).clear();
                            }
                        }
                        else {
                            toks.remove( n );
                        }
                    }

                emitlinedirective = (blanklines > 6) && line_directive != null;
                if( !toks.isEmpty() && toks.get( 0 ).source != null ) {
                    if( lastsource == null ) {
                        if( toks.get( 0 ).source != null ) emitlinedirective = true;
                        lastsource = toks.get( 0 ).source;
                    }
                    else if( !lastsource.equals( toks.get( 0 ).source ) ) {
                        emitlinedirective = true;
                        lastsource        = toks.get( 0 ).source;
                    }
                }

                // Replace consecutive whitespace with single space
                Integer first_ws = null;
                for( int n = toks.size() - 1; n >= 0; n-- ) {
                    LexToken tok3 = toks.get( n );
                    if( first_ws == null ) {
                        if( t_SPACE.equals( tok3.type ) || (tok3.value != null && tok3.value.toString().isEmpty() ) ) first_ws = n;
                    }
                    else if( !t_SPACE.equals( tok3.type ) && (tok3.value == null || !tok3.value.toString().isEmpty() ) ) {
                        int m = n + 1;
                        while( m != first_ws ) { toks.remove( m ); first_ws--; }
                        first_ws = null;
                        if( compress > 0 && toks.size() > m ) {
                            String v = toks.get( m ).value != null ? toks.get( m ).value.toString() : "";
                            if( !v.isEmpty() && v.charAt( 0 ) == ' ' ) toks.get( m ).value = " ";
                        }
                    }
                }

                if( compress <= 1 && !emitlinedirective && !toks.isEmpty() ) {
                    int newlinesneeded = toks.get( 0 ).lineno - lastlineno - 1;
                    if( newlinesneeded > 6 && line_directive != null ) {
                        emitlinedirective = true;
                    }
                    else if( newlinesneeded > 0 ) {
                        StringBuilder nlsb = new StringBuilder();
                        for( int nli = 0; nli < newlinesneeded; nli++ ) nlsb.append( '\n' );
                        oh.write( nlsb.toString() );
                    }
                }

                if( !toks.isEmpty() ) lastlineno = toks.get( 0 ).lineno;

                if( emitlinedirective && line_directive != null ) oh.write( line_directive + " " + lastlineno +
                    (lastsource == null ? "" : (" \"" + lastsource + "\"") ) + "\n" );

                for( LexToken tok3 : toks )
                    if( t_COMMENT1 != null && t_COMMENT1.equals( tok3.type ) ) {
                        String v = tok3.value != null ? tok3.value.toString() : "";
                        for( char c : v.toCharArray() ) if( c == '\n' ) lastlineno++;
                    }

                blanklines = 0;
                StringBuilder linesb = new StringBuilder();
                for( LexToken tok3 : toks )
                    if( tok3.value != null ) linesb.append( tok3.value );
                oh.write( linesb.toString() );
            }
            oh.flush();
        } catch( IOException e ) {
            System.err.println( "Write error: " + e.getMessage() );
        }
    }
} // class Preprocessor
