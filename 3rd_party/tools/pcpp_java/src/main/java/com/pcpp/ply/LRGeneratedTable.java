// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.*;
import java.util.function.Function;

/**
 * Generates LALR(1) or SLR parsing tables from a {@link Grammar}.
 *
 * Mirrors PLY's LRGeneratedTable from yacc.py.
 *
 * The public-facing result is stored in the superclass fields:
 *   {@link LRTable#lr_action}
 *   {@link LRTable#lr_goto}
 *   {@link LRTable#lr_productions}
 *
 * Conflict information is available via:
 *   {@link #sr_conflicts}  - list of shift/reduce conflicts
 *   {@link #rr_conflicts}  - list of reduce/reduce conflicts
 *
 * Usage:
 * <pre>
 *   Grammar g = ...;
 *   LRGeneratedTable lrt = new LRGeneratedTable(g, "LALR", log);
 *   // lrt.lr_action, lrt.lr_goto, lrt.lr_productions are ready
 * </pre>
 */
public class LRGeneratedTable extends LRTable {

    private final Grammar   grammar;
    private final PlyLogger log;

    // Goto cache keyed by (System.identityHashCode(I), symbol)
    private final Map<Long, List<LRItem>> lr_goto_cache;
    // Hash of each closed item set by identity
    private final Map<Integer, Integer> lr0_cidhash;

    // Internal counter for closure computation
    private int _add_count;

    // Conflict tracking
    public       int            sr_conflict  = 0;
    public       int            rr_conflict  = 0;
    public final List<Object[]> conflicts    = new ArrayList<>();
    public final List<Object[]> sr_conflicts = new ArrayList<>(); // [state, token, "shift"|"reduce"]
    public final List<Object[]> rr_conflicts = new ArrayList<>(); // [state, chosenProd, rejectedProd]

    private static final int MAXINT = Integer.MAX_VALUE;

    public LRGeneratedTable(Grammar grammar, String method, PlyLogger log)
    {
        if( !method.equals(
            "SLR"
        ) && !method.equals(
            "LALR"
        ) ) throw new LALRError(
            "Unsupported method: " + method
        );
        this.grammar   = grammar;
        this.lr_method = method;
        this.log       = log != null ? log : PlyLogger.NULL;

        this.lr_action     = new LinkedHashMap<>();
        this.lr_goto       = new LinkedHashMap<>();
        this.lr_goto_cache = new HashMap<>();
        this.lr0_cidhash   = new HashMap<>();

        // Convert grammar productions to MiniProduction list for the runtime
        this.lr_productions = new ArrayList<>();
        for(Production p : grammar.Productions) {
            if(p == null) this.lr_productions.add(null);
            else          this.lr_productions.add(
                new MiniProduction(p.str, p.name, p.len, p.func, p.file, p.line)
            );
        }

        // Build tables
        grammar.build_lritems();
        grammar.compute_first();
        grammar.compute_follow(null);
        lr_parse_table();
    }

    // =========================================================================
    // LR(0) closure
    // =========================================================================

    /**
     * Compute the LR(0) closure of an item set I.
     * Mirrors LRGeneratedTable.lr0_closure in yacc.py.
     */
    public List<LRItem> lr0_closure(List<LRItem> I)
    {
        ++_add_count;
        List<LRItem> J      = new ArrayList<>(I);
        boolean      didadd = true;
        while(didadd) {
            didadd = false;
            for( int ji = 0; ji < J.size(); ++ji ) {
                LRItem j = J.get(ji);
                for(Production x : j.lr_after)
                    if(x.lr_next != null && x.lr_next.lr0_added != _add_count) {
                        J.add(x.lr_next);
                        x.lr_next.lr0_added = _add_count;
                        didadd              = true;
                }
            } // for
        } // while

        return J;
    }

    // =========================================================================
    // LR(0) goto
    // =========================================================================

    /**
     * Compute goto(I, x) — the LR(0) goto function.
     * Returns a unique (identity-equal) list object for the same set of items.
     * Mirrors LRGeneratedTable.lr0_goto in yacc.py.
     */
    public List<LRItem> lr0_goto(List<LRItem> I, String x)
    {
        long         cacheKey = cacheKey( System.identityHashCode(I), x );
        List<LRItem> cached   = lr_goto_cache.get(cacheKey);
        if(cached != null) return cached;

        // Build the goto set
        List<LRItem> gs = new ArrayList<>();
        for(LRItem p : I) {
            LRItem n = p.lr_next;
            if( n != null && x.equals(n.lr_before) ) gs.add(n);
        }

        List<LRItem> g;
        if( !gs.isEmpty() ) g = lr0_closure(gs);
        else                g = gs; // Empty list

        lr_goto_cache.put(cacheKey, g);

        return g;
    }

    private static long cacheKey(int identityHash, String symbol)
    {
        return ( (long) identityHash << 32 ) ^ symbol.hashCode();
    }

    // =========================================================================
    // LR(0) items
    // =========================================================================

    /**
     * Compute the canonical collection of LR(0) item sets.
     * Mirrors LRGeneratedTable.lr0_items in yacc.py.
     */
    public List<List<LRItem>> lr0_items()
    {
        Production   p0      = grammar.Productions.get(0);
        List<LRItem> initial = new ArrayList<>();
        initial.add(p0.lr_next);
        List<List<LRItem>> C = new ArrayList<>();
        C.add( lr0_closure(initial) );
        lr0_cidhash.put( System.identityHashCode( C.get(0) ), 0 );

        int i = 0;
        while( i < C.size() ) {
            List<LRItem> I = C.get(i);
            ++i;

            // Collect all symbols that could appear in goto(I, x)
            Set<String> asyms = new LinkedHashSet<>();
            for(LRItem ii : I)
                for(String s : ii.usyms) asyms.add(s);

            for(String x : asyms) {
                List<LRItem> g = lr0_goto(I, x);
                if( g.isEmpty() || lr0_cidhash.containsKey( System.identityHashCode(g) ) ) continue;
                lr0_cidhash.put( System.identityHashCode(g), C.size() );
                C.add(g);
            }
        } // while

        return C;
    }

    // =========================================================================
    // LALR(1) lookahead computation (DeRemer & Pennello algorithm)
    // =========================================================================

    /** Compute nullable non-terminals */
    public Set<String> compute_nullable_nonterminals()
    {
        Set<String> nullable    = new HashSet<>();
        int         numNullable = 0;
        while(true) {
            for( int i = 1; i < grammar.Productions.size(); ++i ) {
                Production p = grammar.Productions.get(i);
                if(p.len == 0) {
                    nullable.add(p.name);
                    continue;
                }
                boolean allNullable = true;
                for(String t : p.prod)
                    if( !nullable.contains(t) ) { allNullable = false; break; }
                if(allNullable) nullable.add(p.name);
            } // for
            if( nullable.size() == numNullable ) break;
            numNullable = nullable.size();
        } // while

        return nullable;
    }

    /**
     * Find all non-terminal transitions (state, symbol) in C.
     * Returns a list of state indices; call {@link #getNttransSyms()} to get
     * the parallel list of symbol names after this method returns.
     *
     * Mirrors LRGeneratedTable.find_nonterminal_transitions in yacc.py.
     */
    public List<int[]> find_nonterminal_transitions(List<List<LRItem>> C)
    {
        List<int[]> trans = new ArrayList<>();
        _nttrans_syms = new ArrayList<>();
        for( int stateno = 0; stateno < C.size(); ++stateno )
            for( LRItem p : C.get(stateno) )
                if(p.lr_index < p.len - 1) {
                    String sym = p.prod.get(p.lr_index + 1);
                    if( grammar.Nonterminals.containsKey(sym) ) {
                        boolean found = false;
                        for( int i = 0; i < trans.size(); ++i )
                            if( trans.get(i)[0] == stateno && _nttrans_syms.get(i).equals(sym) ) {
                                found = true; break;
                        }
                        if(!found) {
                            trans.add( new int[] {stateno} );
                            _nttrans_syms.add(sym);
                        }
                    } // if
        } // if

        return trans;
    }

    /** Parallel symbol list populated by {@link #find_nonterminal_transitions} */
    private List<String> _nttrans_syms = new ArrayList<>();

    /** Get the non-terminal symbol name for the ith transition entry */
    public String getNttransSyms(int i) { return _nttrans_syms.get(i); }

    // =========================================================================
    // dr_relation, reads_relation, lookback/includes, read sets, follow sets
    // =========================================================================

    // TODO: Complete translation of LALR(1) lookahead computation.
    // The following methods need a full implementation of the DeRemer-Pennello
    // digraph algorithm.  The skeleton below compiles but falls back to the SLR
    // FOLLOW sets if LALR lookaheads are unavailable.

    public List<String> dr_relation(
        List<List<LRItem>> C,
        int[]              trans,
        String             N,
        Set<String>        nullable
    )
    {
        List<String> terms = new ArrayList<>();
        List<LRItem> g     = lr0_goto( C.get( trans[0] ), N );
        for(LRItem p : g)
            if(p.lr_index < p.len - 1) {
                String a = p.prod.get(p.lr_index + 1);
                if( grammar.Terminals.containsKey(a) && !terms.contains(a) ) terms.add(a);
        }
        if( trans[0] == 0 && N.equals( grammar.Productions.get(0).prod.get(0) ) ) terms.add("$end");

        return terms;
    }

    // =========================================================================
    // lr_parse_table() — core table construction
    // =========================================================================

    /**
     * Build the SLR or LALR(1) parse table.
     * Mirrors LRGeneratedTable.lr_parse_table in yacc.py.
     */
    public void lr_parse_table()
    {
        List<Production>                   Productions = grammar.Productions;
        Map<String, String[]>              Precedence  = grammar.Precedence;
        Map<Integer, Map<String, Integer>> action      = this.lr_action;
        Map<Integer, Map<String, Integer>> gotoTable   = this.lr_goto;

        log.info("Parsing method: %s", lr_method);

        // Step 1: Compute LR(0) item sets
        List<List<LRItem>> C = lr0_items();

        // Step 2: For LALR, add lookaheads
        if( lr_method.equals("LALR") ) add_lalr_lookaheads(C);

        // Step 3: Build action and goto tables state by state
        int st = 0;
        for(List<LRItem> I : C) {
            Map<String, Integer>    st_action         = new LinkedHashMap<>();
            Map<LRItem, LRItem>     st_actionp        = new IdentityHashMap<>(); // Item → item for logging
            Map<LRItem, Production> st_actionpProd    = new IdentityHashMap<>(); // Token → prod for conflict resolution
            Map<String, Production> st_actionpByToken = new LinkedHashMap<>();
            Map<String, Integer>    st_goto           = new LinkedHashMap<>();

            log.info("");
            log.info("state %d", st);
            log.info("");
            for(LRItem p : I) log.info("    (%d) %s", p.number, p);
            log.info("");

            for(LRItem p : I) {
                if(p.lr_index == p.len - 1) {
                    // Dot is at the end → reduce or accept
                    if( p.name.equals("S'") ) {
                        st_action.put("$end", 0);
                    }
                    else {
                        // Determine lookaheads
                        List<String> laheads;
                        if( lr_method.equals("LALR") ) {
                            laheads = p.lookaheads.getOrDefault( st, new ArrayList<>() );
                        }
                        else {
                            // SLR: use FOLLOW sets
                            laheads = grammar.Follow.getOrDefault( p.name, new ArrayList<>() );
                        }

                        Production prod = Productions.get(p.number);
                        for(String a : laheads) {
                            Integer r = st_action.get(a);
                            if(r != null) {
                                // Conflict
                                if(r > 0) {
                                    // Shift/reduce conflict
                                    String[] sprec_pair = Precedence.getOrDefault(
                                        a, new String[] { "right", "0" }
                                    );
                                    String   sprec      = sprec_pair[0];
                                    int      slevel     = Integer.parseInt( sprec_pair[1] );

                                    String[] rprec_pair = prod.prec;
                                    String   rprec      = rprec_pair[0];
                                    int      rlevel     = Integer.parseInt( rprec_pair[1] );

                                    if( slevel < rlevel || ( slevel == rlevel && rprec.equals(
                                        "left"
                                    ) ) ) {
                                        // Reduce wins
                                        st_action.put(a, -p.number);
                                        st_actionpByToken.put(a, prod);
                                        if(slevel == 0 && rlevel == 0) {
                                            log.info(
                                                "  ! shift/reduce conflict for %s resolved as reduce",
                                                a
                                            );
                                            sr_conflicts.add( new Object[] {st, a, "reduce"} );
                                        } // if
                                        prod.reduced++;
                                    } // if
                                    else if( slevel == rlevel && rprec.equals("nonassoc") ) {
                                        st_action.put(a, null);
                                    }
                                    else
                                    // Shift wins
                                    if(rlevel == 0) {
                                        log.info(
                                            "  ! shift/reduce conflict for %s resolved as shift", a
                                        );
                                        sr_conflicts.add( new Object[] {st, a, "shift"} );
                                    }
                                } // if
                                else if(r < 0) {
                                    // Reduce/reduce conflict — prefer earlier rule
                                    Production oldp = Productions.get(-r);
                                    if(oldp.line > prod.line) {
                                        st_action.put(a, -p.number);
                                        st_actionpByToken.put(a, prod);
                                        prod.reduced++;
                                        oldp.reduced--;
                                        rr_conflicts.add( new Object[] {st, prod, oldp} );
                                    } // if
                                    else {
                                        rr_conflicts.add( new Object[] {st, oldp, prod} );
                                    }
                                    log.info(
                                        "  ! reduce/reduce conflict for %s resolved using rule %d (%s)",
                                        a, st_action.get(a) != null ? -st_action.get(a) : -1,
                                        st_actionpByToken.getOrDefault(a, oldp)
                                    );
                                }
                                else {
                                    throw new LALRError("Unknown conflict in state " + st);
                                }
                            } // if
                            else {
                                st_action.put(a, -p.number);
                                st_actionpByToken.put(a, prod);
                                prod.reduced++;
                            }
                        } // for a
                    }
                } // if
                else {
                    // Dot is not at end → shift or goto
                    int    i = p.lr_index;
                    String a = p.prod.get(i + 1); // Symbol after the dot
                    if( grammar.Terminals.containsKey(a) ) {
                        List<LRItem> g = lr0_goto(I, a);
                        Integer      j = lr0_cidhash.get( System.identityHashCode(g) );
                        if(j != null && j >= 0) {
                            Integer r = st_action.get(a);
                            if(r != null) {
                                if(r > 0) {
                                    if( !r.equals(
                                        j
                                    ) ) throw new LALRError(
                                        "Shift/shift conflict in state " + st
                                    );
                                } // if
                                else if(r < 0) {
                                    // Shift vs existing reduce — apply precedence
                                    String[] sprec_pair = Precedence.getOrDefault(
                                        a, new String[] { "right", "0" }
                                    );
                                    String   sprec      = sprec_pair[0];
                                    int      slevel     = Integer.parseInt( sprec_pair[1] );

                                    Production rProd = st_actionpByToken.get(a);
                                    String[]   rprec_pair = rProd != null ? rProd.prec : new String[] {
                                        "right", "0"
                                    };
                                    String rprec  = rprec_pair[0];
                                    int    rlevel = Integer.parseInt( rprec_pair[1] );

                                    if( slevel > rlevel || ( slevel == rlevel && rprec.equals(
                                        "right"
                                    ) ) ) {
                                        // Shift wins
                                        if(rProd != null) rProd.reduced--;
                                        st_action.put(a, j);
                                        st_actionpByToken.put(a, null);
                                        if(rlevel == 0) {
                                            log.info(
                                                "  ! shift/reduce conflict for %s resolved as shift",
                                                a
                                            );
                                            sr_conflicts.add( new Object[] {st, a, "shift"} );
                                        } // if
                                    } // if
                                    else if( slevel == rlevel && rprec.equals("nonassoc") ) {
                                        st_action.put(a, null);
                                    }
                                    else
                                    // Reduce wins
                                    if(slevel == 0 && rlevel == 0) {
                                        log.info(
                                            "  ! shift/reduce conflict for %s resolved as reduce", a
                                        );
                                        sr_conflicts.add( new Object[] {st, a, "reduce"} );
                                    }
                                }
                                else {
                                    throw new LALRError("Unknown conflict in state " + st);
                                }
                            } // if
                            else {
                                st_action.put(a, j);
                                st_actionpByToken.put(a, null);
                            }
                        } // if
                    } // if
                }
            } // for p

            // Build the goto table for this state
            Set<String> nkeys = new LinkedHashSet<>();
            for(LRItem ii : I)
                for(String s : ii.usyms)
                    if( grammar.Nonterminals.containsKey(s) ) nkeys.add(s);
            for(String n : nkeys) {
                List<LRItem> g = lr0_goto(I, n);
                Integer      j = lr0_cidhash.get( System.identityHashCode(g) );
                if(j != null && j >= 0) {
                    st_goto.put(n, j);
                    log.info("    %-30s shift and go to state %d", n, j);
                }
            } // for n

            action.put(st, st_action);
            gotoTable.put(st, st_goto);
            ++st;
        } // for I
    }

    // =========================================================================
    // LALR lookahead computation
    // =========================================================================

    /**
     * Attach LALR(1) lookaheads to all LR items that require them.
     * Mirrors LRGeneratedTable.add_lalr_lookaheads in yacc.py.
     *
     * Uses the DeRemer & Pennello digraph algorithm via helper methods.
     */
    public void add_lalr_lookaheads(List<List<LRItem>> C)
    {
        Set<String> nullable = compute_nullable_nonterminals();

        // Find all non-terminal transitions as (state, symbol) pairs
        List<int[]>  transStates = new ArrayList<>();
        List<String> transSyms   = new ArrayList<>();
        for( int stateno = 0; stateno < C.size(); ++stateno )
            for( LRItem p : C.get(stateno) )
                if(p.lr_index < p.len - 1) {
                    String sym = p.prod.get(p.lr_index + 1);
                    if( grammar.Nonterminals.containsKey(sym) ) {
                        boolean found = false;
                        for( int k = 0; k < transStates.size(); ++k )
                            if( transStates.get(k)[0] == stateno && transSyms.get(k).equals(sym) ) {
                                found = true; break;
                        }
                        if(!found) {
                            transStates.add( new int[] {stateno} );
                            transSyms.add(sym);
                        }
                    } // if
        } // if
        int numTrans = transStates.size();

        // Compute DR (directly reads) relation
        List<List<String>> drSets = new ArrayList<>();
        for(int i = 0; i < numTrans; ++i) drSets.add(
            dr_relation_full( C, transStates.get(i)[0], transSyms.get(i), nullable )
        );

        // Compute READS relation: (p,A) reads (q,B) if goto(p,A) -> . B ...  and B is nullable
        List<List<Integer>> readsRel = new ArrayList<>();
        for(int i = 0; i < numTrans; ++i) {
            List<Integer> rel   = new ArrayList<>();
            int           state = transStates.get(i)[0];
            String        N     = transSyms.get(i);
            List<LRItem>  g     = lr0_goto( C.get(state), N );
            int           j     = lr0_cidhash.getOrDefault( System.identityHashCode(g), -1 );
            for(LRItem p : g)
                if(p.lr_index < p.len - 1) {
                    String a = p.prod.get(p.lr_index + 1);
                    if( nullable.contains(a) )
                        // Find index of (j, a) in trans list
                        for(int k = 0; k < numTrans; ++k)
                            if( transStates.get(k)[0] == j && transSyms.get(k).equals(a) ) {
                                rel.add(k);
                                break;
                    }
            } // if
            readsRel.add(rel);
        } // for

        // Compute read sets via digraph
        List<List<String>> readSets = digraph(numTrans, readsRel, drSets);

        // Compute lookback and includes
        // lookback: transition-index → list of (stateIdx, LRItem)
        List<List<int[]>>   lookback    = new ArrayList<>();
        List<List<Integer>> includesRel = new ArrayList<>();
        for(int i = 0; i < numTrans; ++i) {
            lookback.add( new ArrayList<>() );
            includesRel.add( new ArrayList<>() );
        }
        // dtrans: (state, sym) → index
        Map<Long, Integer> dtrans = new HashMap<>();
        for(int i = 0; i < numTrans; ++i) {
            long key = (long)transStates.get(
                i
            )[0] << 32 | ( transSyms.get(
                i
            ).hashCode() & 0xFFFFFFFFL );
            dtrans.put(key, i);
        } // for

        for(int ti = 0; ti < numTrans; ++ti) {
            int    state = transStates.get(ti)[0];
            String N     = transSyms.get(ti);
            for( LRItem p : C.get(state) ) {
                if( !p.name.equals(N) ) continue;

                int lr_index = p.lr_index;
                int j        = state;
                while(lr_index < p.len - 1) {
                    ++lr_index;
                    String t = p.prod.get(lr_index);

                    long tkey = (long)j << 32 | ( t.hashCode() & 0xFFFFFFFFL );
                    if( dtrans.containsKey(tkey) ) {
                        // Check if suffix derives empty
                        boolean suffixNullable = true;
                        for(int li = lr_index + 1; li < p.len; ++li) {
                            if( grammar.Terminals.containsKey( p.prod.get(li) ) ) {
                                suffixNullable = false; break;
                            }
                            if( !nullable.contains( p.prod.get(li) ) ) {
                                suffixNullable = false; break;
                            }
                        } // for li
                        if(suffixNullable) {
                            // (j,t) includes (state, N)  →  ti includes dtrans[(j,t)]
                            int includedIdx = dtrans.get(tkey);
                            if( !includesRel.get(
                                includedIdx
                            ).contains(
                                ti
                            ) ) includesRel.get(
                                includedIdx
                            ).add(
                                ti
                            );
                        } // if
                    } // if

                    List<LRItem> gNext = lr0_goto( C.get(j), t );
                    j = lr0_cidhash.getOrDefault( System.identityHashCode(gNext), -1 );
                } // while

                // J is the final state — find matching completed item
                if(j >= 0) {
                    for( LRItem r : C.get(j) ) {
                        if( !r.name.equals(p.name) ) continue;
                        if(r.lr_index != r.len - 1) continue;
                        // Compare RHS
                        boolean match = true;
                        for(int k = 0; k < r.lr_index; ++k)
                            // R.prod has "." at index r.lr_index, items before that are the actual syms
                            // r.prod[0..lr_index-1] should match p.prod[1..lr_index]
                            if( !r.prod.get(k).equals( p.prod.get(k + 1) ) ) {
                                match = false; break;
                        }
                        if(match) {
                            lookback.get(ti).add( new int[] {j, r.number} ); // Store (state, production index)
                        }
                    } // for r
                } // if
            } // for p
        } // for ti

        // Compute follow sets via digraph
        List<List<String>> followSets = digraph(numTrans, includesRel, readSets);

        // Attach lookaheads to LR items
        for(int ti = 0; ti < numTrans; ++ti) {
            List<String> fs = followSets.get(ti);
            for( int[] lb : lookback.get(ti) ) {
                int lbState   = lb[0];
                int lbProdNum = lb[1];
                // Find the completed LR item in C[lbState]
                for( LRItem r : C.get(lbState) )
                    if(r.number == lbProdNum && r.lr_index == r.len - 1) {
                        List<String> laheads = r.lookaheads.computeIfAbsent(
                            lbState, k->new ArrayList<>()
                        );
                        for(String a : fs)
                            if( !laheads.contains(a) ) laheads.add(a);
                } // if
            } // for lb
        } // for ti
    }

    /** Full DR relation for LALR */
    private List<String> dr_relation_full(
        List<List<LRItem>> C,
        int                state,
        String             N,
        Set<String>        nullable
    )
    {
        List<String> terms = new ArrayList<>();
        List<LRItem> g     = lr0_goto( C.get(state), N );
        for(LRItem p : g)
            if(p.lr_index < p.len - 1) {
                String a = p.prod.get(p.lr_index + 1);
                if( grammar.Terminals.containsKey(a) && !terms.contains(a) ) terms.add(a);
        }
        if( state == 0 && N.equals( grammar.Productions.get(0).prod.get(0) ) )
            if( !terms.contains("$end") ) terms.add("$end");

        return terms;
    }

    // =========================================================================
    // Digraph algorithm (DeRemer & Pennello)
    // =========================================================================

    /**
     * Compute set-valued function F via the digraph algorithm.
     *
     * F(x) = FP(x) ∪ ∪{ F(y) | x R y }
     *
     * @param n         number of nodes (0..n-1)
     * @param R         adjacency list: R.get(i) is list of j where i R j
     * @param FP        initial set values (modified in place and returned)
     * @return          computed F values, indexed same as FP
     */
    private List<List<String>> digraph(int n, List<List<Integer>> R, List<List<String>> FP)
    {
        int[]              N = new int[n];
        List<List<String>> F = new ArrayList<>(FP);
        // Make mutable copies
        for(int i = 0; i < n; ++i) F.set( i, new ArrayList<>( F.get(i) ) );
        Deque<Integer> stack = new ArrayDeque<>();
        for(int x = 0; x < n; ++x)
            if( N[x] == 0 ) traverse(x, N, stack, F, R);

        return F;
    }

    private void traverse(
        int                 x,
        int[]               N,
        Deque<Integer>      stack,
        List<List<String>>  F,
        List<List<Integer>> R
    )
    {
        stack.push(x);
        int d = stack.size();
        N[x] = d;
        // F[x] already initialized to FP(x)

        for( int y : R.get(x) ) {
            if( N[y] == 0 ) traverse(y, N, stack, F, R);
            N[x] = Math.min( N[x], N[y] );
            for( String a : F.get(y) )
                if( !F.get(x).contains(a) ) F.get(x).add(a);
        }

        if( N[x] == d ) {
            // Pop elements until x, assign them F[x]
            List<String> fxCopy = new ArrayList<>( F.get(x) );
            int          top    = stack.peek();
            N[top] = MAXINT;
            F.set(top, fxCopy);
            int popped = stack.pop();
            while(popped != x) {
                top    = stack.peek();
                N[top] = MAXINT;
                F.set(top, fxCopy);
                popped = stack.pop();
            }
        } // if
    }

} // class LRGeneratedTable
