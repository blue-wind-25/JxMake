// Translated by Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a single production (grammar rule) in its full form.
 *
 * Mirrors PLY's Production class from yacc.py.
 *
 * A grammar rule such as:
 *   expr : expr PLUS term
 *
 * is represented as:
 *   name     = "expr"
 *   prod     = ["expr", "PLUS", "term"]
 *   number   = &lt;assigned by Grammar&gt;
 *   prec     = ("right", 0)  or similar precedence pair
 *   func     = name of the action function (string key into the action map)
 *   callable = the bound Consumer&lt;YaccProduction&gt; action
 *   file     = source file where rule was declared
 *   line     = line number where rule was declared
 */
public class Production {
    /** Count of how many times this production has been reduced. */
    public int reduced = 0;

    public final int number;
    public final String name;
    public final List<String> prod;
    public final String func;
    public final String file;
    public final int line;

    /** Precedence pair: ("left"|"right"|"nonassoc", level). */
    public String[] prec;

    /** Number of RHS symbols. */
    public final int len;

    /** Unique symbols appearing on the RHS. */
    public final List<String> usyms;

    /**
     * Human-readable rule string, e.g. "expr -> expr PLUS term"
     * or "epsilon -> &lt;empty&gt;" for empty productions.
     */
    public final String str;

    /** The bound action callable. Null means no-op. */
    public Consumer<YaccProduction> callable;

    /** Linked list of LR items (used during table construction). */
    public LRItem lr_next;
    public List<LRItem> lr_items;

    public Production(int number, String name, List<String> prod,
                      String[] precedence, String func, String file, int line) {
        this.number = number;
        this.name   = name;
        this.prod   = new ArrayList<>(prod);
        this.prec   = precedence != null ? precedence : new String[]{"right", "0"};
        this.func   = func;
        this.file   = file;
        this.line   = line;
        this.len    = prod.size();
        this.lr_items = new ArrayList<>();

        // Build usyms (unique symbols in RHS order of first occurrence)
        this.usyms = new ArrayList<>();
        for (String s : prod) {
            if (!usyms.contains(s)) {
                usyms.add(s);
            }
        }

        // Build string representation
        if (!this.prod.isEmpty()) {
            this.str = name + " -> " + String.join(" ", prod);
        } else {
            this.str = name + " -> <empty>";
        }
    }

    /** Convenience constructor with no precedence (defaults to right, 0). */
    public Production(int number, String name, List<String> prod) {
        this(number, name, prod, new String[]{"right", "0"}, null, "", 0);
    }

    /** Return the symbol at position index on the RHS. */
    public String get(int index) {
        return prod.get(index);
    }

    /** Bind the action by name from the spec's action map. */
    public void bind(java.util.Map<String, Consumer<YaccProduction>> pdict) {
        if (func != null) {
            this.callable = pdict.get(func);
        }
    }

    /**
     * Create the nth LRItem for this production (dot at position n).
     * The resulting item's lr_after and lr_before fields are populated
     * by the caller ({@link Grammar#build_lritems}).
     */
    public LRItem lr_item(int n) {
        if (n > len) return null;
        return new LRItem(this, n);
    }

    @Override
    public String toString() {
        return str;
    }
}
