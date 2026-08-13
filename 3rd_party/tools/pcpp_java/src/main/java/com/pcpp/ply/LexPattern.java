// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that supplies the regex pattern for a lexer rule method.
 *
 * <p>In PLY Python, the regex pattern is stored as the docstring of a
 * {@code t_XXX} function. In Java (where methods have no docstrings), this
 * annotation fills the same role.
 *
 * <p>Example:
 * <pre>{@code
 * @LexPattern("[A-Za-z_][A-Za-z0-9_]*")
 * public LexToken t_ID(LexToken tok) {
 *     return tok;
 * }
 * }</pre>
 *
 * <p>Alternatively, a static String field with the same name as the method
 * may be used (see {@link LexerReflect}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LexPattern {

    /** The regular expression pattern for this token rule */
    String value();

} // interface LexPattern
