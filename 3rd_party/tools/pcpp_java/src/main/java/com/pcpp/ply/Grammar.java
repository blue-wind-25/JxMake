// Translated by Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Represents a context-free grammar for LALR table construction.
 *
 * Mirrors PLY's Grammar class from yacc.py.
 *
 * Key data structures:
 *   Productions  - List of all productions (index 0 is the augmented start rule)
 *   Prodnames    - Map: non-terminal name → list of productions for that name
 *   Terminals    - Map: terminal name → list of production numbers using it
 *   Nonterminals - Map: non-terminal name → list of production numbers using it
 *   Precedence   - Map: terminal → [assoc, level]
 *   First        - Precomputed FIRST sets
 *   Follow       - Precomputed FOLLOW sets
 *   Start        - Start symbol
 */
public class Grammar {

    /** All productions; index 0 is reserved for the augmented S' rule. */
    public final List<Production> Productions;

    /** non-terminal name → list of productions for that name */
    public final Map<String, List<Production>> Prodnames;

    /** Used to detect duplicate productions. Key is "name -> syms". */
    public final Map<String, Production> Prodmap;

    /** terminal → list of production numbers where it is used */
    public final Map<String, List<Integer>> Terminals;

    /** non-terminal → list of production numbers where it is used */
    public final Map<String, List<Integer>> Nonterminals;

    /** Precomputed FIRST sets */
    public final Map<String, List<String>> First;

    /** Precomputed FOLLOW sets */
    public final Map<String, List<String>> Follow;

    /**
     * Precedence rules: terminal → [assoc, level_string]
     * assoc is "left", "right", or "nonassoc".
     */
    public final Map<String, String[]> Precedence;

    /** Terminals for which precedence was actually used (for warnings). */
    public final Set<String> UsedPrecedence;

    /** The start symbol (set by {@link #set_start}). */
    public String Start;

    private static final Pattern IS_IDENTIFIER = Pattern.compile("^[a-zA-Z0-9_-]+$");

    public Grammar(List<String> terminals) {
        Productions  = new ArrayList<>();
        Productions.add(null); // index 0 reserved

        Prodnames    = new LinkedHashMap<>();
        Prodmap      = new HashMap<>();
        Terminals    = new LinkedHashMap<>();
        Nonterminals = new LinkedHashMap<>();
        First        = new HashMap<>();
        Follow       = new HashMap<>();
        Precedence   = new LinkedHashMap<>();
        UsedPrecedence = new HashSet<>();

        for (String t : terminals) {
            Terminals.put(t, new ArrayList<>());
        }
        Terminals.put("error", new ArrayList<>());
    }

    public int size() {
        return Productions.size();
    }

    public Production get(int index) {
        return Productions.get(index);
    }

    // -------------------------------------------------------------------------
    // set_precedence()
    // -------------------------------------------------------------------------

    /**
     * Set the precedence for a terminal.
     * Must be called before any {@link #add_production} call.
     *
     * @param term   terminal name
     * @param assoc  "left", "right", or "nonassoc"
     * @param level  numeric precedence level (higher = tighter binding)
     */
    public void set_precedence(String term, String assoc, int level) {
        if (Productions.size() > 1) {
            throw new GrammarError("Must call set_precedence() before add_production()");
        }
        if (Precedence.containsKey(term)) {
            throw new GrammarError("Precedence already specified for terminal '" + term + "'");
        }
        if (!assoc.equals("left") && !assoc.equals("right") && !assoc.equals("nonassoc")) {
            throw new GrammarError("Associativity must be one of 'left', 'right', or 'nonassoc'");
        }
        Precedence.put(term, new String[]{assoc, String.valueOf(level)});
    }

    // -------------------------------------------------------------------------
    // add_production()
    // -------------------------------------------------------------------------

    /**
     * Add a production rule to the grammar.
     *
     * @param prodname  LHS non-terminal name
     * @param syms      RHS symbol list (may include "%prec TERM" at the end)
     * @param func      name of the action function (may be null)
     * @param file      source file (for error messages)
     * @param line      source line (for error messages)
     */
    public void add_production(String prodname, List<String> syms,
                               String func, String file, int line) {
        if (Terminals.containsKey(prodname)) {
            throw new GrammarError(file + ":" + line +
                ": Illegal rule name '" + prodname + "'. Already defined as a token");
        }
        if (prodname.equals("error")) {
            throw new GrammarError(file + ":" + line +
                ": Illegal rule name 'error'. error is a reserved word");
        }
        if (!IS_IDENTIFIER.matcher(prodname).matches()) {
            throw new GrammarError(file + ":" + line +
                ": Illegal rule name '" + prodname + "'");
        }

        // Validate and expand literal tokens
        List<String> expandedSyms = new ArrayList<>(syms);
        for (int n = 0; n < expandedSyms.size(); n++) {
            String s = expandedSyms.get(n);
            if (s.length() > 0 && (s.charAt(0) == '\'' || s.charAt(0) == '"')) {
                // Literal character token
                String c = s.substring(1, s.length() - 1);
                if (c.length() > 1) {
                    throw new GrammarError(file + ":" + line +
                        ": Literal token " + s + " in rule '" + prodname +
                        "' may only be a single character");
                }
                if (!Terminals.containsKey(c)) {
                    Terminals.put(c, new ArrayList<>());
                }
                expandedSyms.set(n, c);
            } else if (!s.equals("%prec") && !IS_IDENTIFIER.matcher(s).matches()) {
                throw new GrammarError(file + ":" + line +
                    ": Illegal name '" + s + "' in rule '" + prodname + "'");
            }
        }

        // Determine precedence
        String[] prodprec;
        if (expandedSyms.contains("%prec")) {
            int precIdx = expandedSyms.indexOf("%prec");
            if (precIdx == expandedSyms.size() - 1) {
                throw new GrammarError(file + ":" + line +
                    ": Syntax error. Nothing follows %prec");
            }
            if (precIdx != expandedSyms.size() - 2) {
                throw new GrammarError(file + ":" + line +
                    ": Syntax error. %prec can only appear at the end of a grammar rule");
            }
            String precname = expandedSyms.get(expandedSyms.size() - 1);
            prodprec = Precedence.get(precname);
            if (prodprec == null) {
                throw new GrammarError(file + ":" + line +
                    ": Nothing known about the precedence of '" + precname + "'");
            }
            UsedPrecedence.add(precname);
            // Remove the last two elements (%prec TERMINAL)
            expandedSyms.remove(expandedSyms.size() - 1);
            expandedSyms.remove(expandedSyms.size() - 1);
        } else {
            String precname = rightmostTerminal(expandedSyms);
            if (precname != null && Precedence.containsKey(precname)) {
                prodprec = Precedence.get(precname);
            } else {
                prodprec = new String[]{"right", "0"};
            }
        }

        // Check for duplicate rule
        String mapKey = prodname + " -> " + expandedSyms;
        if (Prodmap.containsKey(mapKey)) {
            Production m = Prodmap.get(mapKey);
            throw new GrammarError(file + ":" + line + ": Duplicate rule " + m +
                ". Previous definition at " + m.file + ":" + m.line);
        }

        int pnumber = Productions.size();
        if (!Nonterminals.containsKey(prodname)) {
            Nonterminals.put(prodname, new ArrayList<>());
        }

        // Register symbol usage
        for (String t : expandedSyms) {
            if (Terminals.containsKey(t)) {
                Terminals.get(t).add(pnumber);
            } else {
                if (!Nonterminals.containsKey(t)) {
                    Nonterminals.put(t, new ArrayList<>());
                }
                Nonterminals.get(t).add(pnumber);
            }
        }

        Production p = new Production(pnumber, prodname, expandedSyms, prodprec, func, file, line);
        Productions.add(p);
        Prodmap.put(mapKey, p);

        if (!Prodnames.containsKey(prodname)) {
            Prodnames.put(prodname, new ArrayList<>());
        }
        Prodnames.get(prodname).add(p);
    }

    // -------------------------------------------------------------------------
    // set_start()
    // -------------------------------------------------------------------------

    /**
     * Set the start symbol and create the augmented grammar rule S' -> start.
     * Production 0 is S' -> start.
     */
    public void set_start(String start) {
        if (start == null) {
            start = Productions.get(1).name;
        }
        if (!Nonterminals.containsKey(start)) {
            throw new GrammarError("start symbol '" + start + "' undefined");
        }
        List<String> startProd = new ArrayList<>();
        startProd.add(start);
        Productions.set(0, new Production(0, "S'", startProd));
        Nonterminals.get(start).add(0);
        Start = start;
    }

    // -------------------------------------------------------------------------
    // Validation helpers
    // -------------------------------------------------------------------------

    /** Return non-terminals that cannot be reached from the start symbol. */
    public List<String> find_unreachable() {
        Set<String> reachable = new HashSet<>();
        markReachableFrom(Productions.get(0).prod.get(0), reachable);
        List<String> unreachable = new ArrayList<>();
        for (String s : Nonterminals.keySet()) {
            if (!reachable.contains(s)) {
                unreachable.add(s);
            }
        }
        return unreachable;
    }

    private void markReachableFrom(String s, Set<String> reachable) {
        if (reachable.contains(s)) return;
        reachable.add(s);
        List<Production> prods = Prodnames.get(s);
        if (prods != null) {
            for (Production p : prods) {
                for (String r : p.prod) {
                    markReachableFrom(r, reachable);
                }
            }
        }
    }

    /** Return non-terminals involved in infinite recursion (no terminal derivation). */
    public List<String> infinite_cycles() {
        Map<String, Boolean> terminates = new HashMap<>();
        for (String t : Terminals.keySet()) terminates.put(t, true);
        terminates.put("$end", true);
        for (String n : Nonterminals.keySet()) terminates.put(n, false);

        boolean someChange = true;
        while (someChange) {
            someChange = false;
            for (Map.Entry<String, List<Production>> e : Prodnames.entrySet()) {
                String n = e.getKey();
                for (Production p : e.getValue()) {
                    boolean pTerminates = true;
                    for (String s : p.prod) {
                        if (!Boolean.TRUE.equals(terminates.get(s))) {
                            pTerminates = false;
                            break;
                        }
                    }
                    if (pTerminates && !Boolean.TRUE.equals(terminates.get(n))) {
                        terminates.put(n, true);
                        someChange = true;
                        break;
                    }
                }
            }
        }

        List<String> infinite = new ArrayList<>();
        for (Map.Entry<String, Boolean> e : terminates.entrySet()) {
            if (!e.getValue()) {
                String s = e.getKey();
                if (Prodnames.containsKey(s) || Terminals.containsKey(s) || s.equals("error")) {
                    infinite.add(s);
                }
            }
        }
        return infinite;
    }

    /** Return [(symbol, production)] for undefined symbols used in rules. */
    public List<Object[]> undefined_symbols() {
        List<Object[]> result = new ArrayList<>();
        for (Production p : Productions) {
            if (p == null) continue;
            for (String s : p.prod) {
                if (!Prodnames.containsKey(s) && !Terminals.containsKey(s) && !s.equals("error")) {
                    result.add(new Object[]{s, p});
                }
            }
        }
        return result;
    }

    /** Return terminals that were defined but never used in any rule. */
    public List<String> unused_terminals() {
        List<String> unused = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> e : Terminals.entrySet()) {
            if (!e.getKey().equals("error") && e.getValue().isEmpty()) {
                unused.add(e.getKey());
            }
        }
        return unused;
    }

    /** Return productions (one per non-terminal) for non-terminals that are never used. */
    public List<Production> unused_rules() {
        List<Production> unused = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> e : Nonterminals.entrySet()) {
            if (e.getValue().isEmpty()) {
                unused.add(Prodnames.get(e.getKey()).get(0));
            }
        }
        return unused;
    }

    /** Return [(term, assoc)] for precedence rules that were defined but never applied. */
    public List<String[]> unused_precedence() {
        List<String[]> unused = new ArrayList<>();
        for (Map.Entry<String, String[]> e : Precedence.entrySet()) {
            String termname = e.getKey();
            if (!Terminals.containsKey(termname) && !UsedPrecedence.contains(termname)) {
                unused.add(new String[]{termname, e.getValue()[0]});
            }
        }
        return unused;
    }

    // -------------------------------------------------------------------------
    // FIRST / FOLLOW
    // -------------------------------------------------------------------------

    /**
     * Compute FIRST(beta) for a list of symbols beta.
     * Returns a list of terminal symbols (may include "&lt;empty&gt;").
     */
    public List<String> _first(List<String> beta) {
        List<String> result = new ArrayList<>();
        boolean allProduceEmpty = true;
        for (String x : beta) {
            boolean xProducesEmpty = false;
            List<String> fx = First.get(x);
            if (fx != null) {
                for (String f : fx) {
                    if (f.equals("<empty>")) {
                        xProducesEmpty = true;
                    } else if (!result.contains(f)) {
                        result.add(f);
                    }
                }
            }
            if (!xProducesEmpty) {
                allProduceEmpty = false;
                break;
            }
        }
        if (allProduceEmpty && !beta.isEmpty()) {
            result.add("<empty>");
        }
        return result;
    }

    /**
     * Compute FIRST sets for all grammar symbols.
     */
    public Map<String, List<String>> compute_first() {
        if (!First.isEmpty()) return First;

        for (String t : Terminals.keySet()) {
            First.put(t, Collections.singletonList(t));
        }
        First.put("$end", Collections.singletonList("$end"));

        for (String n : Nonterminals.keySet()) {
            First.put(n, new ArrayList<>());
        }

        boolean someChange = true;
        while (someChange) {
            someChange = false;
            for (String n : Nonterminals.keySet()) {
                List<Production> prods = Prodnames.get(n);
                if (prods == null) continue;
                for (Production p : prods) {
                    for (String f : _first(p.prod)) {
                        if (!First.get(n).contains(f)) {
                            First.get(n).add(f);
                            someChange = true;
                        }
                    }
                }
            }
        }
        return First;
    }

    /**
     * Compute FOLLOW sets for all non-terminals.
     */
    public Map<String, List<String>> compute_follow(String start) {
        if (!Follow.isEmpty()) return Follow;
        if (First.isEmpty()) compute_first();

        for (String k : Nonterminals.keySet()) {
            Follow.put(k, new ArrayList<>());
        }

        if (start == null) start = Productions.get(1).name;
        Follow.get(start).add("$end");

        boolean didadd = true;
        while (didadd) {
            didadd = false;
            for (int i = 1; i < Productions.size(); i++) {
                Production p = Productions.get(i);
                for (int j = 0; j < p.prod.size(); j++) {
                    String B = p.prod.get(j);
                    if (!Nonterminals.containsKey(B)) continue;

                    List<String> suffix = p.prod.subList(j + 1, p.prod.size());
                    List<String> fst = _first(suffix);
                    boolean hasEmpty = false;
                    for (String f : fst) {
                        if (f.equals("<empty>")) {
                            hasEmpty = true;
                        } else if (!Follow.get(B).contains(f)) {
                            Follow.get(B).add(f);
                            didadd = true;
                        }
                    }
                    if (hasEmpty || j == p.prod.size() - 1) {
                        List<String> followP = Follow.get(p.name);
                        if (followP != null) {
                            for (String f : followP) {
                                if (!Follow.get(B).contains(f)) {
                                    Follow.get(B).add(f);
                                    didadd = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return Follow;
    }

    // -------------------------------------------------------------------------
    // build_lritems()
    // -------------------------------------------------------------------------

    /**
     * Build the complete set of LR items for every production.
     *
     * Each production gets a linked list of LR items from "A -> . B C" to
     * "A -> B C .".  The lr_after and lr_before fields on each LRItem are
     * also populated here.
     */
    public void build_lritems() {
        for (Production p : Productions) {
            if (p == null) continue;
            LRItem lastlri = null;
            List<LRItem> lr_items = new ArrayList<>();
            for (int i = 0; i <= p.len; i++) {
                LRItem lri;
                if (i > p.len) {
                    lri = null;
                } else {
                    lri = new LRItem(p, i);
                    // lr_after: productions whose LHS is the symbol right after the dot
                    if (i < p.prod.size()) {
                        String nextSym = p.prod.get(i);
                        List<Production> after = Prodnames.get(nextSym);
                        lri.lr_after = after != null ? after : new ArrayList<>();
                    }
                    // lr_before: symbol just before the dot
                    if (i > 0) {
                        lri.lr_before = p.prod.get(i - 1);
                    }
                }
                if (lastlri != null) {
                    lastlri.lr_next = lri;
                }
                if (lri == null) break;
                lr_items.add(lri);
                lastlri = lri;
            }
            p.lr_items = lr_items;
            p.lr_next = lr_items.isEmpty() ? null : lr_items.get(0);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Return the rightmost terminal in syms, or null if none. */
    private String rightmostTerminal(List<String> syms) {
        for (int i = syms.size() - 1; i >= 0; i--) {
            if (Terminals.containsKey(syms.get(i))) return syms.get(i);
        }
        return null;
    }
}
