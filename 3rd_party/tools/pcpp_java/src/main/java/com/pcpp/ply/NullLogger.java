// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

/**
 * A logger that silently discards all output. Mirrors PLY's NullLogger from
 * lex.py. Useful when no diagnostic output is desired.
 */
public class NullLogger extends PlyLogger {

    public NullLogger()
    {
        super( new java.io.PrintWriter( new java.io.Writer()
        {
            @Override public void write( char[] buf, int off, int len )
            { // Discard
            }
            @Override public void flush()
            {                               // Discard
            }
            @Override public void close()
            {                               // Discard
            }
        } ) );
    }

    @Override
    public void critical(String msg, Object... args) { /* Discard */ }

    @Override
    public void warning(String msg, Object... args) { /* Discard */ }

    @Override
    public void error(String msg, Object... args) { /* Discard */ }

    @Override
    public void info(String msg, Object... args) { /* Discard */ }

    @Override
    public void debug(String msg, Object... args) { /* Discard */ }

} // class NullLogger
