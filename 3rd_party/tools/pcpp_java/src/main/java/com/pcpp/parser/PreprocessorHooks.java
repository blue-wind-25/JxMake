// Translated using Claude Sonnet 4.6
package com.pcpp.parser;

import com.pcpp.ply.LexToken;
import java.io.*;

/**
 * Override these in your subclass of Preprocessor to customise preprocessing.
 * Mirrors PreprocessorHooks class in parser.py.
 */
public class PreprocessorHooks {
    public LexToken lastdirective;
    public int      return_code;

    public PreprocessorHooks()
    {
        this.lastdirective = null;
        this.return_code   = 0;
    }

    /**
     * Called when the preprocessor has encountered an error.
     * The default prints to stderr and increments the return code.
     */
    public void on_error( String file, int line, String msg )
    {
        System.err.printf( "%s:%d error: %s%n", file, line, msg );
        return_code++;
    }

    /**
     * Called to open a file for reading.
     * The default opens with the system default encoding.
     * Override to use chardet or a custom encoding.
     */
    public Reader on_file_open( boolean is_system_include, String includepath ) throws IOException
    {
        return new FileReader( includepath );
    }

    /**
     * Called when a #include wasn't found.
     * Raise OutputDirective to pass through or remove.
     * Return a suitable path, or null to use the default behaviour.
     * The default prints an error and raises OutputDirective (pass through).
     */
    public String on_include_not_found( boolean is_malformed, boolean is_system_include,
        String curdir, String includepath )
    {
        if( is_malformed ) {
            on_error( lastdirective != null ? (String) lastdirective.source : "unknown",
                lastdirective != null ? lastdirective.lineno : 0,
                "Malformed #include statement: " + includepath );
        }
        else {
            on_error( lastdirective != null ? (String) lastdirective.source : "unknown",
                lastdirective != null ? lastdirective.lineno : 0,
                "Include file '" + includepath + "' not found" );
        }
        throw new OutputDirective( Action.IgnoreAndPassThrough );
    }

    /**
     * Called when an expression passed to an #if contained a defined operator
     * performed on something unknown.
     * Return Boolean.TRUE if defined, Boolean.FALSE if undefined,
     * throw OutputDirective to pass through, or return null to pass through mostly expanded.
     * The default returns Boolean.FALSE (as per the C standard).
     */
    public Boolean on_unknown_macro_in_defined_expr( LexToken tok )
    {
        return Boolean.FALSE;
    }

    /**
     * Called when an expression passed to an #if contained an unknown identifier.
     * Return what value to use, or null to pass through mostly expanded.
     * The default returns Integer 0 (as per the C standard).
     */
    public Object on_unknown_macro_in_expr( String ident )
    {
        return 0;
    }

    /**
     * Called when an expression passed to an #if contained an unknown function.
     * Return a lambda, or null to pass through mostly expanded.
     * The default returns a lambda returning 0 (as per the C standard).
     */
    public java.util.function.Function<Object, Integer> on_unknown_macro_function_in_expr( String ident )
    {
        return x -> 0;
    }

    /**
     * Called when there is a known directive: define, include, undef, ifdef, ifndef, if, elif, else, endif.
     * Return Boolean.TRUE to execute and remove from output,
     * throw OutputDirective to pass through or remove without execution,
     * return null to execute AND pass through (only works for #define, #undef).
     * The default returns Boolean.TRUE.
     */
    public Boolean on_directive_handle( LexToken directive, java.util.List<LexToken> toks,
        boolean ifpassthru, java.util.List<LexToken> precedingtoks )
    {
        this.lastdirective = directive;
        return Boolean.TRUE;
    }

    /**
     * Called when the preprocessor encounters a #directive it doesn't understand.
     * Return Boolean.TRUE to remove from output,
     * throw OutputDirective to pass through or remove,
     * return null to pass through.
     * The default handles #error and #warning, passes everything else through.
     */
    public Boolean on_directive_unknown( LexToken directive, java.util.List<LexToken> toks,
        boolean ifpassthru, java.util.List<LexToken> precedingtoks )
    {
        String val = (String) directive.value;
        if( "error".equals( val ) ) {
            StringBuilder sb = new StringBuilder();
            for( LexToken t : toks ) sb.append( t.value );
            System.err.printf( "%s:%d error: %s%n", directive.source, directive.lineno, sb );
            return_code++;
            return Boolean.TRUE;
        }
        else if( "warning".equals( val ) ) {
            StringBuilder sb = new StringBuilder();
            for( LexToken t : toks ) sb.append( t.value );
            System.err.printf( "%s:%d warning: %s%n", directive.source, directive.lineno, sb );
            return Boolean.TRUE;
        }
        return null;
    }

    /**
     * Called when the preprocessor encounters an #ifndef macro or #if !defined(macro)
     * as the first non-whitespace thing in a file.
     */
    public void on_potential_include_guard( String macro )
    {
        // Default: do nothing
    }

    /**
     * Called when the preprocessor encounters a comment token.
     * Modify the token in place if needed. Return true to let the comment pass through.
     * Returning false or null makes the token become whitespace.
     */
    public Boolean on_comment( LexToken tok )
    {
        return null;
    }
} // class PreprocessorHooks
