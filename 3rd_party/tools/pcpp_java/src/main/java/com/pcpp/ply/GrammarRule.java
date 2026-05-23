// Translated by Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.function.Consumer;

/**
 * Represents a single grammar rule with its action function.
 *
 * In Python PLY, grammar rules are expressed as functions with docstrings:
 * <pre>
 *   def p_expr(p):
 *       '''expr : expr PLUS term'''
 *       p[0] = p[1] + p[3]
 * </pre>
 *
 * In Java, each such rule is expressed as a {@link GrammarRule}:
 * <pre>
 *   GrammarRule.of("expr : expr PLUS term", p -&gt; p.set(0, (int)p.get(1) + (int)p.get(3)))
 * </pre>
 *
 * A single rule object may specify multiple alternatives by using multiple
 * lines in the pattern (with "|" continuation), mirroring the Python convention
 * of a multi-line docstring in one p_ function.
 *
 * The {@code name} field is used as the key when binding callables;
 * it should be unique within a {@link GrammarSpec}.
 */
public class GrammarRule {
    /**
     * A unique name for this rule (analogous to the Python function name, e.g. "p_expr").
     * Used as the key for action binding.
     */
    public final String name;

    /**
     * The grammar rule text, in BNF form.
     * May contain multiple lines/alternatives:
     * <pre>
     *   expr : expr PLUS term
     *        | expr MINUS term
     * </pre>
     */
    public final String pattern;

    /**
     * The action to execute on reduction.
     * Receives a {@link YaccProduction} where:
     *   p.get(0)  is the LHS result (set by the action)
     *   p.get(1)  is the value of the first RHS symbol
     *   ...etc.
     *
     * May be null if the rule has no semantic action (p[0] stays null).
     */
    public final Consumer<YaccProduction> action;

    /**
     * Source file name (for error messages). Optional; may be null.
     */
    public final String file;

    /**
     * Source line number (for error messages). Optional; may be 0.
     */
    public final int line;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public GrammarRule(String name, String pattern, Consumer<YaccProduction> action,
                       String file, int line) {
        this.name    = name;
        this.pattern = pattern;
        this.action  = action;
        this.file    = file;
        this.line    = line;
    }

    public GrammarRule(String name, String pattern, Consumer<YaccProduction> action) {
        this(name, pattern, action, "<unknown>", 0);
    }

    /** No-op rule (no semantic action). */
    public GrammarRule(String name, String pattern) {
        this(name, pattern, null, "<unknown>", 0);
    }

    // -------------------------------------------------------------------------
    // Factory helpers (for more fluent usage at call sites)
    // -------------------------------------------------------------------------

    /** Create a rule with the given pattern and action. */
    public static GrammarRule of(String name, String pattern, Consumer<YaccProduction> action) {
        return new GrammarRule(name, pattern, action);
    }

    /** Create a no-op rule (action leaves p[0] = null). */
    public static GrammarRule of(String name, String pattern) {
        return new GrammarRule(name, pattern, null);
    }

    @Override
    public String toString() {
        return "GrammarRule(" + name + ": " + pattern.trim() + ")";
    }
}
