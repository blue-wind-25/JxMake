// Translated by Claude Sonnet 4.6
package com.pcpp.ply;

/**
 * Exception raised for grammar-related errors.
 * Mirrors PLY's GrammarError from yacc.py.
 */
public class GrammarError extends YaccError {
    public GrammarError(String message) {
        super(message);
    }
}
