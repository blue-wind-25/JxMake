// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

/**
 * Exception raised for yacc-related errors.
 * Mirrors PLY's YaccError from yacc.py.
 */
public class YaccError extends RuntimeException {
    public YaccError(String message) {
        super(message);
    }

    public YaccError(String message, Throwable cause) {
        super(message, cause);
    }
}
