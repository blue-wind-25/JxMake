// Translated by Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.function.Consumer;

/**
 * Minimal stand-in for a Production object used at parse time.
 *
 * When parse tables are loaded from a pre-generated source (analogous to
 * parsetab.py in Python PLY), only the fields used by the LR parsing engine
 * need to be present.  This class holds exactly those fields.
 *
 * Mirrors PLY's MiniProduction from yacc.py.
 *
 * Fields:
 *   str      - Human-readable form of the rule, e.g. "expr -> expr PLUS term"
 *   name     - Name of the production (the LHS non-terminal), e.g. "expr"
 *   len      - Number of symbols on the RHS
 *   func     - Name of the action function (may be null for empty actions)
 *   file     - Source file where the production was defined
 *   line     - Line number where the production was defined
 *   callable - The actual Java action, bound by YaccBuilder.bindCallables()
 */
public class MiniProduction {
    public final String str;
    public final String name;
    public final int len;
    public final String func;
    public final String file;
    public final int line;

    /**
     * The grammar action to call on reduction.
     * Null means "no-op" (leaves p[0] as null).
     * Bound by {@link YaccBuilder#bindCallables}.
     */
    public Consumer<YaccProduction> callable;

    public MiniProduction(String str, String name, int len, String func, String file, int line) {
        this.str  = str;
        this.name = name;
        this.len  = len;
        this.func = func;
        this.file = file;
        this.line = line;
    }

    /** Bind the named action to its callable from the spec's action map. */
    public void bind(java.util.Map<String, Consumer<YaccProduction>> pdict) {
        if (func != null) {
            this.callable = pdict.get(func);
        }
    }

    @Override
    public String toString() {
        return str;
    }
}
