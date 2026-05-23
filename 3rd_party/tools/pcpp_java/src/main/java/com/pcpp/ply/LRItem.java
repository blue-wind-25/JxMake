// Translated by Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a specific stage of parsing a production rule (an LR item).
 *
 * Mirrors PLY's LRItem class from yacc.py.
 *
 * For example, given:
 *   expr : expr PLUS term
 *
 * The LR items are:
 *   expr -> . expr PLUS term   (lr_index=0)
 *   expr -> expr . PLUS term   (lr_index=1)
 *   expr -> expr PLUS . term   (lr_index=2)
 *   expr -> expr PLUS term .   (lr_index=3)
 *
 * The "." is inserted into the prod list at lr_index.
 *
 * Fields:
 *   name       - Name of the production (LHS), e.g. "expr"
 *   prod       - RHS with "." inserted, e.g. ["expr", ".", "PLUS", "term"]
 *   number     - Production number
 *   lr_index   - Position of the dot in prod
 *   lookaheads - LALR lookahead symbols, keyed by state number
 *   len        - Length of prod (including the ".")
 *   usyms      - Unique symbols from the original production
 *   lr_next    - Next LRItem (dot moved one position right), or null at end
 *   lr_after   - Productions that immediately follow the dot symbol (for closure)
 *   lr_before  - The symbol immediately before the dot
 *
 *   lr0_added  - Internal counter used during LR(0) closure computation
 */
public class LRItem {
    public final String name;
    public final List<String> prod;   // includes "." at lr_index
    public final int number;
    public final int lr_index;
    public final int len;
    public final List<String> usyms;

    /** LALR lookahead sets: state → list of lookahead terminal symbols. */
    public final Map<Integer, List<String>> lookaheads;

    /** Next LR item (dot shifted right by one). Null at the end of the rule. */
    public LRItem lr_next;

    /**
     * List of all productions whose LHS immediately follows the dot.
     * Populated during {@link Grammar#build_lritems}.
     */
    public List<Production> lr_after;

    /**
     * The grammar symbol immediately before the dot.
     * Populated during {@link Grammar#build_lritems}.
     */
    public String lr_before;

    /** Internal counter used during LR(0) closure; mirrors Python's lr0_added attribute. */
    public int lr0_added;

    public LRItem(Production p, int n) {
        this.name      = p.name;
        this.number    = p.number;
        this.lr_index  = n;
        this.usyms     = p.usyms;
        this.lookaheads = new HashMap<>();
        this.lr_after   = new ArrayList<>();

        // Build prod with "." inserted at position n
        List<String> withDot = new ArrayList<>(p.prod);
        withDot.add(n, ".");
        this.prod = withDot;
        this.len  = withDot.size();
    }

    @Override
    public String toString() {
        if (prod.isEmpty()) {
            return name + " -> <empty>";
        }
        return name + " -> " + String.join(" ", prod);
    }
}
