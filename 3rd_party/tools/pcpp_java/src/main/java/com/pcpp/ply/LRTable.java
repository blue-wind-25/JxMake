// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base class holding precomputed LR parsing tables.
 *
 * Mirrors PLY's LRTable from yacc.py.
 *
 * The three core tables are:
 *   lr_action      - action[state][token] → positive=shift target state,
 *                    negative=reduce by production (-n), 0=accept, null=error
 *   lr_goto        - goto[state][non-terminal] → next state after a reduction
 *   lr_productions - list of MiniProduction objects (index 0 is the augmented rule)
 *   lr_method      - "LALR" or "SLR"
 *
 * Tables are either:
 *   (a) read from a pre-generated parsetab (see {@link #readTable}), or
 *   (b) computed by {@link LRGeneratedTable}.
 */
public class LRTable {

    /**
     * Action table: state (int) → (token → action).
     * action > 0  → shift to that state
     * action < 0  → reduce using production number (-action)
     * action == 0 → accept
     * no entry     → syntax error
     */
    public Map<Integer, Map<String, Integer>> lr_action;

    /**
     * Goto table: state (int) → (non-terminal → next state)
     */
    public Map<Integer, Map<String, Integer>> lr_goto;

    /**
     * Production list.  Index 0 is the augmented start rule S' -> start.
     * Indices are the production numbers (used as -action for reductions).
     */
    public List<MiniProduction> lr_productions;

    /** "LALR" or "SLR" */
    public String lr_method;

    public LRTable() {}

    // -------------------------------------------------------------------------
    // Programmatic table population (used by YaccBuilder / LRGeneratedTable)
    // -------------------------------------------------------------------------

    /**
     * Populate from raw data structures produced by a table generator.
     * This is the direct-assignment path (no file I/O).
     */
    public void setTables(
        Map<Integer, Map<String, Integer>> action,
        Map<Integer, Map<String, Integer>> gotoTable,
        List<MiniProduction>               productions,
        String                             method
    )
    {
        this.lr_action      = action;
        this.lr_goto        = gotoTable;
        this.lr_productions = productions;
        this.lr_method      = method;
    }

    // -------------------------------------------------------------------------
    // readTable() – populate from a ParseTabData object (analogous to parsetab.py)
    // -------------------------------------------------------------------------

    /**
     * Load pre-generated tables from a {@link ParseTabData} object.
     *
     * In Python PLY the tables come from a generated parsetab.py module.
     * In Java they come from a {@link ParseTabData} implementation, which is
     * typically a generated class that simply fills in the three fields below.
     *
     * @param tab the parse table data
     * @return the signature stored in the table data
     */
    public String readTable(ParseTabData tab)
    {
        this.lr_action = tab.getAction();
        this.lr_goto   = tab.getGoto();
        this.lr_method = tab.getMethod();

        this.lr_productions = new ArrayList<>();
        for( Object[] row : tab.getProductions() ) {
            // row = [str, name, len, func, file, line]  (same layout as Python tuple)
            String str  = (String)row[0];
            String name = (String)row[1];
            int    len  = (Integer)row[2];
            String func = row[3] != null ? (String)row[3] : null;
            String file = row[4] != null ? (String)row[4] : null;
            int    line = row[5] != null ? (Integer)row[5] : 0;
            this.lr_productions.add( new MiniProduction(str, name, len, func, file, line) );
        } // for

        return tab.getSignature();
    }

    // -------------------------------------------------------------------------
    // bindCallables() – attach action functions to MiniProduction entries
    // -------------------------------------------------------------------------

    /**
     * Bind grammar action functions to each MiniProduction.
     *
     * @param pdict map from function name to Consumer&lt;YaccProduction&gt; action
     */
    public void bindCallables(Map<String, Consumer<YaccProduction>> pdict)
    {
        for(MiniProduction p : lr_productions) p.bind(pdict);
    }

} // class LRTable
