// Translated by Claude Sonnet 4.6
package com.pcpp.parser;

/**
 * Raise this exception to abort processing of a preprocessor directive and
 * to instead output it as is into the output.
 * Mirrors OutputDirective exception in parser.py.
 */
public class OutputDirective extends RuntimeException {
    public final int action;

    public OutputDirective(int action) {
        super("OutputDirective: " + action);
        this.action = action;
    }
}
