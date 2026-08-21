// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds all the information needed to build a {@link Lexer}.
 *
 * <p>In PLY Python the lexer specification lives in a Python module as
 * {@code tokens}, {@code literals}, {@code states}, and {@code t_XXX}
 * symbols.  In Java we collect equivalent data in this class.
 *
 * <p>Typical usage:
 * <pre>{@code
 * LexerSpec spec = new LexerSpec();
 * spec.addTokens("ID", "NUMBER", "PLUS");
 * spec.addLiteral('(');
 * spec.addState("COMMENT", "exclusive");
 * spec.addRule(new LexRule("t_ID",     "[A-Za-z_][A-Za-z0-9_]*"));
 * spec.addRule(new LexRule("t_NUMBER", "\\d+"));
 * spec.addIgnore("INITIAL", " \t");
 * spec.setErrorCallback("INITIAL", tok -> { /* ... *<U+200C>/ return tok; });
 * }</pre>
 *
 * <p>After populating the spec call {@link LexerBuilder#build(LexerSpec)} to
 * obtain a ready-to-use {@link Lexer}.
 */
public class LexerSpec {

    // -----------------------------------------------------------------------
    // Tokens
    // -----------------------------------------------------------------------

    /** All valid token names (populated via {@link #addTokens}) */
    private final List<String> tokens = new ArrayList<>();

    /** Add one or more token names */
    public LexerSpec addTokens(String... names)
    {
        for(String n : names) tokens.add(n);

        return this;
    }

    /** Returns an unmodifiable view of the token list */
    public List<String> getTokens()
    {
        return Collections.unmodifiableList(tokens);
    }

    // -----------------------------------------------------------------------
    // Literals
    // -----------------------------------------------------------------------

    /** Single-character literal tokens that pass through unchanged */
    private final StringBuilder literals = new StringBuilder();

    /** Add a single literal character */
    public LexerSpec addLiteral(char c)
    {
        literals.append(c);

        return this;
    }

    /** Add multiple literal characters from a string */
    public LexerSpec addLiterals(String chars)
    {
        literals.append(chars);

        return this;
    }

    /** Returns the literals as a String */
    public String getLiterals()
    {
        return literals.toString();
    }

    // -----------------------------------------------------------------------
    // States
    // -----------------------------------------------------------------------

    /**
     * State map: state-name → "inclusive" | "exclusive".
     * INITIAL is always present and is "inclusive".
     */
    private final Map<String, String> stateInfo = new LinkedHashMap<>();

    {
        stateInfo.put("INITIAL", "inclusive");
    }

    /**
     * Add a lexer state.
     *
     * @param name      state name
     * @param stateType {@code "inclusive"} or {@code "exclusive"}
     */
    public LexerSpec addState(String name, String stateType)
    {
        stateInfo.put(name, stateType);

        return this;
    }

    /** Returns an unmodifiable view of the state info map */
    public Map<String, String> getStateInfo()
    {
        return Collections.unmodifiableMap(stateInfo);
    }

    // -----------------------------------------------------------------------
    // Rules
    // -----------------------------------------------------------------------

    /**
     * All lexer rules in declaration order.  Function rules are sorted by
     * insertion order; string rules are sorted by pattern length (longest
     * first) during build.
     */
    private final List<LexRule> rules = new ArrayList<>();

    /**
     * Add a lexer rule.  The rule name must follow PLY conventions:
     * {@code t_[state_]tokenname} – e.g. {@code t_ID}, {@code t_COMMENT_end}.
     */
    public LexerSpec addRule(LexRule rule)
    {
        rules.add(rule);

        return this;
    }

    /** Returns an unmodifiable view of the rule list */
    public List<LexRule> getRules()
    {
        return Collections.unmodifiableList(rules);
    }

    // -----------------------------------------------------------------------
    // Ignore strings per state
    // -----------------------------------------------------------------------

    /** Maps state name to the string of characters to ignore */
    private final Map<String, String> ignoreMap = new LinkedHashMap<>();

    /**
     * Set the ignore string for a state.  Characters in {@code chars} are
     * skipped without producing a token (equivalent to {@code t_ignore} in
     * Python PLY).
     */
    public LexerSpec setIgnore(String state, String chars)
    {
        ignoreMap.put(state, chars);

        return this;
    }

    /** Shorthand: set ignore for INITIAL state */
    public LexerSpec setIgnore(String chars)
    {
        return setIgnore("INITIAL", chars);
    }

    /** Returns the ignore map (state → chars) */
    public Map<String, String> getIgnoreMap()
    {
        return Collections.unmodifiableMap(ignoreMap);
    }

    // -----------------------------------------------------------------------
    // Error callbacks per state
    // -----------------------------------------------------------------------

    /** Maps state name to its error callback */
    private final Map<String, TokenCallback> errorCallbacks = new LinkedHashMap<>();

    /**
     * Set the error callback for a state.  Mirrors {@code t_error} in Python.
     * The callback receives a token whose {@code value} is the remaining input,
     * {@code type} is {@code "error"}.  It may advance the lexer position via
     * {@link Lexer#skip(int)}.
     */
    public LexerSpec setErrorCallback(String state, TokenCallback cb)
    {
        errorCallbacks.put(state, cb);

        return this;
    }

    /** Shorthand: set error callback for INITIAL state */
    public LexerSpec setErrorCallback(TokenCallback cb)
    {
        return setErrorCallback("INITIAL", cb);
    }

    /** Returns the error callback map */
    public Map<String, TokenCallback> getErrorCallbacks()
    {
        return Collections.unmodifiableMap(errorCallbacks);
    }

    // -----------------------------------------------------------------------
    // EOF callbacks per state
    // -----------------------------------------------------------------------

    /** Maps state name to its EOF callback */
    private final Map<String, TokenCallback> eofCallbacks = new LinkedHashMap<>();

    /**
     * Set the EOF callback for a state.  Mirrors {@code t_eof} in Python.
     */
    public LexerSpec setEofCallback(String state, TokenCallback cb)
    {
        eofCallbacks.put(state, cb);

        return this;
    }

    /** Shorthand: set EOF callback for INITIAL state */
    public LexerSpec setEofCallback(TokenCallback cb)
    {
        return setEofCallback("INITIAL", cb);
    }

    /** Returns the EOF callback map */
    public Map<String, TokenCallback> getEofCallbacks()
    {
        return Collections.unmodifiableMap(eofCallbacks);
    }

    // -----------------------------------------------------------------------
    // Reflection support: populate spec from an annotated Java object
    // -----------------------------------------------------------------------

    /**
     * Populates this spec by reflecting on {@code obj} using
     * {@link LexerReflect}.  This mirrors how the Python {@code lex()}
     * function accepts a {@code module} argument and extracts
     * {@code tokens / literals / states / t_XXX} attributes from it via
     * {@code dir(module)}.
     *
     * <p>See {@link LexerReflect} for the annotation / naming conventions
     * expected on {@code obj}.
     *
     * @param obj    the object to reflect on
     * @param log    logger for warnings / errors (may be {@code null} to use
     *               {@link PlyLogger#NULL})
     * @return {@code this} for chaining
     * @throws IllegalArgumentException if the reflected spec is invalid
     */
    public LexerSpec fromObject(Object obj, PlyLogger log)
    {
        LexerReflect reflect = new LexerReflect(obj, log != null ? log : PlyLogger.NULL);
        reflect.populate(this);

        return this;
    }

    /** Shorthand with a stderr logger */
    public LexerSpec fromObject(Object obj)
    {
        return fromObject( obj, new PlyLogger() );
    }

} // class LexerSpec
