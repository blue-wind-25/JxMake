// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

/**
 * Exception thrown when an invalid token is encountered and no default
 * error handler is defined. Mirrors PLY's LexError from lex.py.
 */
public class LexError extends RuntimeException {
    public final String text;

    public              LexError( String message, String text ) {
        super(message);
        this.text = text;
    }
} // class LexError
