// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

/**
 * Represents a single lexer rule: a regex pattern and an optional callback.
 *
 * In PLY Python, rules are either:
 *   - A function whose docstring (or {@code .regex} attribute) is the pattern.
 *   - A plain string giving the pattern.
 *
 * In Java we unify both forms here. If {@code callback} is {@code null} the
 * rule is a simple string rule; if non-null it is a function rule. Setting
 * {@code ignore} to {@code true} makes the rule behave like a
 * {@code t_ignore_XXX} rule (token is consumed silently).
 *
 * The {@code name} is the symbolic name of the rule (e.g. {@code "t_ID"}).
 * The {@code tokenName} is the token type produced (e.g. {@code "ID"}).
 * Both are derived automatically by {@link LexerSpec} helpers.
 */
public class LexRule {
    /** Symbolic rule name, e.g. {@code "t_ID"} or {@code "t_INITIAL_error"}. */
    public final String name;
    /** Regex pattern string. */
    public final String pattern;
    /**
     * Optional callback. {@code null} for simple string rules.
     * When non-null the callback receives the matched {@link LexToken} and
     * may return a (possibly different) token or {@code null} to discard.
     */
    public final TokenCallback callback;
    /**
     * When {@code true} the matched text is silently consumed (ignore rule).
     * Only meaningful when {@code callback} is {@code null}.
     */
    public final boolean ignore;

    /**
     * Construct a function rule (callback is required to produce a token).
     *
     * @param name     symbolic name, e.g. {@code "t_ID"}
     * @param pattern  regex pattern
     * @param callback function invoked on match
     */
    public LexRule(String name, String pattern, TokenCallback callback) {
        this.name = name;
        this.pattern = pattern;
        this.callback = callback;
        this.ignore = false;
    }

    /**
     * Construct a simple string rule (no callback).
     *
     * @param name    symbolic name, e.g. {@code "t_PLUS"}
     * @param pattern regex pattern
     * @param ignore  if {@code true} matched text is discarded silently
     */
    public LexRule(String name, String pattern, boolean ignore) {
        this.name = name;
        this.pattern = pattern;
        this.callback = null;
        this.ignore = ignore;
    }

    /**
     * Convenience: simple non-ignore string rule.
     *
     * @param name    symbolic name
     * @param pattern regex pattern
     */
    public LexRule(String name, String pattern) {
        this(name, pattern, false);
    }

    /** Returns {@code true} if this is a function rule (has a callback). */
    public boolean isFunction() {
        return callback != null;
    }

    @Override
    public String toString() {
        return "LexRule{name='" + name + "', pattern='" + pattern + "'}";
    }
}
