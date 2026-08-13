// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

/**
 * Exception raised for LALR table-generation errors.
 * Mirrors PLY's LALRError from yacc.py.
 */
public class LALRError extends YaccError {

    public LALRError(String message)
    {
        super(message);
    }

} // class LALRError
