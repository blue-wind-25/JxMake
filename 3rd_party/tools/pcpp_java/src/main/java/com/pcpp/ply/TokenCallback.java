// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

/**
 * Functional interface for a token-rule callback. In PLY Python the callback
 * is a function taking a LexToken and returning a LexToken (or null to discard
 * the token). This Java equivalent uses the same contract.
 *
 * Mirrors the callable convention of "t_XXX" functions in lex.py.
 */
@FunctionalInterface
public interface TokenCallback {
    /**
     * Process a matched token.
     *
     * @param tok the token produced from the regex match; its {@code value} is
     *            already set to the matched text, {@code lineno} and
     *            {@code lexpos} are also set.
     * @return the token to emit (may be {@code tok} itself, a different token,
     *         or {@code null} to silently discard the token).
     */
    LexToken apply( LexToken tok );
}
