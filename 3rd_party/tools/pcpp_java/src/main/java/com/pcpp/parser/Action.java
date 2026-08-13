// Translated using Claude Sonnet 4.6
package com.pcpp.parser;

/**
 * What kind of abort processing to do in OutputDirective.
 * Mirrors Action class in parser.py.
 */
public final class Action {

    private Action() {}

    /** Abort processing (don't execute), but pass the directive through to output */
    public static final int IgnoreAndPassThrough = 0;

    /** Abort processing (don't execute), and remove from output */
    public static final int IgnoreAndRemove = 1;

} // class Action
