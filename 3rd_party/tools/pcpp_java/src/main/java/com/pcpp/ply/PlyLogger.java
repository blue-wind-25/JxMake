// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

import java.io.PrintWriter;

/**
 * A stand-in for a logging object. Mirrors PLY's PlyLogger from lex.py / yacc.py.
 * All log methods accept a printf-style format string and arguments.
 */
public class PlyLogger {
    private final PrintWriter writer;

    public PlyLogger( PrintWriter writer )
    {
        this.writer = writer;
    }

    /** Convenience constructor wrapping System.err. */
    public PlyLogger()
    {
        this(new PrintWriter( System.err, true ) );
    }

    public void critical( String msg, Object... args )
    {
        writer.println( args.length == 0 ? msg : String.format( msg, args ) );
    }

    public void warning( String msg, Object... args )
    {
        writer.println( "WARNING: " + (args.length == 0 ? msg : String.format( msg, args ) ) );
    }

    public void error( String msg, Object... args )
    {
        writer.println( "ERROR: " + (args.length == 0 ? msg : String.format( msg, args ) ) );
    }

    /** Alias for critical. */
    public void info( String msg, Object... args )
    {
        critical( msg, args );
    }

    /** Alias for critical. */
    public void debug( String msg, Object... args )
    {
        critical( msg, args );
    }

    /**
     * Null logger — mirrors PLY's NullLogger. Discards all output.
     */
    public static final PlyLogger NULL = new PlyLogger( new PrintWriter(System.err, false) )
    {
        @Override public void critical( String msg, Object... args ) {}
        @Override public void warning( String msg, Object... args ) {}
        @Override public void error( String msg, Object... args ) {}
        @Override public void info( String msg, Object... args ) {}
        @Override public void debug( String msg, Object... args ) {}
    };
} // class PlyLogger
