// Translated using Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The LR parsing engine.
 *
 * Mirrors PLY's LRParser class from yacc.py.
 *
 * Construct via {@link YaccBuilder#build(GrammarSpec)} (the yacc() factory),
 * or directly for testing:
 * <pre>
 *   LRTable table = ...;
 *   LRParser parser = new LRParser(table, errorFunc);
 *   Object result = parser.parse(tokenSupplier, lexer);
 * </pre>
 *
 * The {@link #parse} method accepts a {@link Supplier}&lt;{@link YaccSymbol}&gt; as
 * the token source.  Callers typically wrap a {@link Lexer} or a pre-built
 * {@link java.util.List} of tokens.
 */
public class LRParser {
    private static final int ERROR_COUNT = 3; // symbols that must shift to leave error-recovery

    /** Production list; index 0 is augmented S' rule. */
    private final List<MiniProduction> productions;

    /** Action table: state → (token → action). */
    private final Map<Integer, Map<String, Integer>> action;

    /** Goto table: state → (non-terminal → next state). */
    private final Map<Integer, Map<String, Integer>> gotoTable;

    /** User-supplied error callback; may be null. */
    private final Consumer<YaccSymbol> errorfunc;

    /**
     * Defaulted states: states where every possible action is the same
     * reduction.  In such states the parser can reduce without consuming
     * the next lookahead token.
     */
    private final Map<Integer, Integer> defaulted_states;

    // -------------------------------------------------------------------------
    // State exposed to grammar actions and the error callback
    // -------------------------------------------------------------------------

    /** Current parser state (set before each grammar action call). */
    public int state;

    /**
     * The integer state stack (accessible to error callbacks via parser.statestack).
     * Mirrors Python's self.statestack.
     */
    public List<Integer>    statestack;
    /**
     * The grammar symbol stack (accessible to error callbacks via parser.symstack).
     * Mirrors Python's self.symstack.
     */
    public List<YaccSymbol> symstack;

    /** True if error-recovery mode is OK (reset by errok()). */
    public boolean errorok;

    /** The token supplier in use during the current parse call. */
    private Supplier<YaccSymbol> tokenSupplier;

    /** The lexer associated with the current parse call (may be null). */
    private Object lexer;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public LRParser(LRTable lrtab, Consumer<YaccSymbol> errorf) {
        this.productions   = lrtab.lr_productions;
        this.action        = lrtab.lr_action;
        this.gotoTable     = lrtab.lr_goto;
        this.errorfunc     = errorf;
        this.defaulted_states = new HashMap<>();
        this.errorok       = true;
        computeDefaultedStates();
    }

    // -------------------------------------------------------------------------
    // Defaulted-state support
    // -------------------------------------------------------------------------

    /**
     * Pre-compute states where only one action (a reduction) is possible.
     * Mirrors LRParser.set_defaulted_states in yacc.py.
     */
    private void computeDefaultedStates() {
        for (Map.Entry<Integer, Map<String, Integer>> e : action.entrySet()) {
            Map<String, Integer> actions = e.getValue();
            // Collect non-null values
            List<Integer> rules = new ArrayList<>();
            for (Integer v : actions.values()) {
                if (v != null) rules.add(v);
            }
            if (rules.size() == 1 && rules.get(0) < 0) {
                defaulted_states.put(e.getKey(), rules.get(0));
            }
        }
    }

    /** Disable defaulted-state optimization (forces lookahead before every reduce). */
    public void disableDefaultedStates() {
        defaulted_states.clear();
    }

    // -------------------------------------------------------------------------
    // Error-recovery helpers (available to grammar error actions)
    // -------------------------------------------------------------------------

    /** Signal that error recovery is complete; re-enable normal parsing. */
    public void errok() {
        this.errorok = true;
    }

    /** Restart the parser from scratch (clears stacks). Mirrors Python LRParser.restart(). */
    public void restart() {
        if (statestack != null) statestack.clear();
        if (symstack != null)   symstack.clear();
        if (symstack != null)   symstack.add(new YaccSymbol("$end", null));
        if (statestack != null) statestack.add(0);
    }

    /** Retrieve the next token from the current token source. */
    public YaccSymbol token() {
        return tokenSupplier != null ? tokenSupplier.get() : null;
    }

    // -------------------------------------------------------------------------
    // parse() — main entry point
    // -------------------------------------------------------------------------

    /**
     * Parse a sequence of tokens supplied by {@code tokenSupplier}.
     *
     * This is the primary entry point, analogous to Python's
     * {@code LRParser.parse(input, lexer, debug=False, tracking=False)}.
     *
     * @param tokenSupplier a function that returns the next {@link YaccSymbol}
     *                      on each call; returns null (or a $end symbol) at EOF
     * @param lexer         the lexer object (stored for use in grammar actions;
     *                      may be null)
     * @return the value of the top-most grammar symbol on acceptance
     */
    public Object parse(Supplier<YaccSymbol> tokenSupplier, Object lexer) {
        return parseopt_notrack(tokenSupplier, lexer);
    }

    /**
     * Convenience overload: parse a pre-built list of {@link LexToken} tokens.
     *
     * The list is converted to a {@link YaccSymbol} supplier internally.
     *
     * @param tokens the complete token list (must not be null)
     * @param lexer  the originating lexer (may be null)
     * @return the parse result
     */
    public Object parse(List<LexToken> tokens, Object lexer) {
        Iterator<LexToken> it = tokens.iterator();
        Supplier<YaccSymbol> sup = () -> {
            if (!it.hasNext()) return null;
            LexToken tok = it.next();
            YaccSymbol sym = new YaccSymbol();
            sym.type  = tok.type;
            sym.value = tok.value;
            sym.lineno = tok.lineno;
            sym.hasLineno = true;
            sym.lexpos = tok.lexpos;
            sym.hasLexpos = true;
            return sym;
        };
        return parse(sup, lexer);
    }

    // -------------------------------------------------------------------------
    // parseopt_notrack — optimised parser loop (no line-number tracking)
    // -------------------------------------------------------------------------

    /**
     * The core parse loop — mirrors PLY's parseopt_notrack from yacc.py.
     *
     * This is the fastest variant: no debug output, no position tracking.
     */
    private Object parseopt_notrack(Supplier<YaccSymbol> get_token, Object lexer) {
        this.tokenSupplier = get_token;
        this.lexer = lexer;

        YaccSymbol lookahead = null;
        Deque<YaccSymbol> lookaheadstack = new ArrayDeque<>();

        List<Integer>      stateStack = new ArrayList<>();
        List<YaccSymbol>   symStack   = new ArrayList<>();

        // Expose stacks for error recovery (mirrors Python's self.statestack / self.symstack)
        this.statestack = stateStack;
        this.symstack   = symStack;

        // Re-use a single YaccProduction object per parse; swap slice each time
        YaccProduction pslice = new YaccProduction(null, symStack);
        pslice.lexer  = lexer;
        pslice.parser = this;

        YaccSymbol errtoken  = null;
        int        errorcount = 0;
        this.errorok = true;

        // Initial state
        stateStack.add(0);
        YaccSymbol endSym = new YaccSymbol("$end", null);
        symStack.add(endSym);
        int currentState = 0;

        parseLoop:
        while (true) {
            // -----------------------------------------------------------------------
            // Determine the next action
            // -----------------------------------------------------------------------
            Integer t;

            if (!defaulted_states.containsKey(currentState)) {
                // Need to look at the lookahead
                if (lookahead == null) {
                    if (lookaheadstack.isEmpty()) {
                        lookahead = get_token.get();
                    } else {
                        lookahead = lookaheadstack.pop();
                    }
                    if (lookahead == null) {
                        lookahead = new YaccSymbol("$end", null);
                    }
                }
                String ltype = lookahead.type;
                Map<String, Integer> stateActions = action.get(currentState);
                t = stateActions != null ? stateActions.get(ltype) : null;
            } else {
                t = defaulted_states.get(currentState);
            }

            // -----------------------------------------------------------------------
            // Process the action
            // -----------------------------------------------------------------------
            if (t != null) {
                if (t > 0) {
                    // SHIFT
                    stateStack.add(t);
                    currentState = t;
                    symStack.add(lookahead);
                    lookahead = null;
                    if (errorcount > 0) errorcount--;
                    continue;
                }

                if (t < 0) {
                    // REDUCE
                    MiniProduction p = productions.get(-t);
                    String pname = p.name;
                    int    plen  = p.len;

                    // Create the new LHS symbol
                    YaccSymbol sym = new YaccSymbol(pname, null);

                    if (plen > 0) {
                        // Build the production slice: [sym, rhs1, rhs2, ..., rhsN]
                        // The slice is symStack[-plen-1 .. end] with [0] replaced by sym
                        int start = symStack.size() - plen;
                        List<YaccSymbol> targ = new ArrayList<>(plen + 1);
                        targ.add(sym);
                        for (int i = start; i < symStack.size(); i++) {
                            targ.add(symStack.get(i));
                        }

                        pslice.slice = targ;

                        try {
                            // Pop RHS symbols from stacks
                            removeLastN(symStack,   plen);
                            this.state = currentState;
                            if (p.callable != null) {
                                p.callable.accept(pslice);
                            }
                            removeLastN(stateStack, plen);
                            symStack.add(sym);
                            Map<String, Integer> gotoRow = gotoTable.get(stateStack.get(stateStack.size() - 1));
                            currentState = gotoRow != null ? gotoRow.getOrDefault(pname, 0) : 0;
                            stateStack.add(currentState);
                        } catch (YaccProduction.ParseSyntaxError e) {
                            // Grammar action signalled a syntax error
                            if (lookahead != null) lookaheadstack.push(lookahead);
                            // Put the production slice items back (except [0])
                            for (int i = 1; i < targ.size() - 1; i++) {
                                symStack.add(targ.get(i));
                            }
                            stateStack.remove(stateStack.size() - 1);
                            currentState = stateStack.get(stateStack.size() - 1);
                            sym.type  = "error";
                            sym.value = "error";
                            lookahead = sym;
                            errorcount = ERROR_COUNT;
                            this.errorok = false;
                        }
                        continue;

                    } else {
                        // Empty production
                        List<YaccSymbol> targ = new ArrayList<>(1);
                        targ.add(sym);
                        pslice.slice = targ;

                        try {
                            this.state = currentState;
                            if (p.callable != null) {
                                p.callable.accept(pslice);
                            }
                            symStack.add(sym);
                            Map<String, Integer> gotoRow = gotoTable.get(stateStack.get(stateStack.size() - 1));
                            currentState = gotoRow != null ? gotoRow.getOrDefault(pname, 0) : 0;
                            stateStack.add(currentState);
                        } catch (YaccProduction.ParseSyntaxError e) {
                            if (lookahead != null) lookaheadstack.push(lookahead);
                            stateStack.remove(stateStack.size() - 1);
                            currentState = stateStack.get(stateStack.size() - 1);
                            sym.type  = "error";
                            sym.value = "error";
                            lookahead = sym;
                            errorcount = ERROR_COUNT;
                            this.errorok = false;
                        }
                        continue;
                    }
                }

                if (t == 0) {
                    // ACCEPT
                    YaccSymbol n = symStack.get(symStack.size() - 1);
                    return n.value;
                }
            }

            // -----------------------------------------------------------------------
            // t is null → syntax error
            // -----------------------------------------------------------------------
            if (t == null) {
                if (errorcount == 0 || this.errorok) {
                    errorcount = ERROR_COUNT;
                    this.errorok = false;
                    errtoken = lookahead;
                    if (errtoken != null && "$end".equals(errtoken.type)) {
                        errtoken = null; // end-of-file error
                    }

                    if (errorfunc != null) {
                        if (errtoken != null && !errtoken.hasLexpos) {
                            // Attach lexer so error handler can query it
                        }
                        this.state = currentState;
                        errorfunc.accept(errtoken);
                        if (this.errorok) {
                            // User performed panic-mode recovery and set errorok.
                            // The error function may return a replacement token
                            // (we do not support that variant here — caller must push
                            // back via the token supplier).
                            errtoken = null;
                            continue;
                        }
                    } else {
                        if (errtoken != null) {
                            if (errtoken.hasLineno) {
                                System.err.printf("yacc: Syntax error at line %d, token=%s%n",
                                                  errtoken.lineno, errtoken.type);
                            } else {
                                System.err.printf("yacc: Syntax error, token=%s%n", errtoken.type);
                            }
                        } else {
                            System.err.println("yacc: Parse error in input. EOF");
                            return null;
                        }
                    }
                } else {
                    errorcount = ERROR_COUNT;
                }

                // Case 1: stack has only the initial entry → discard token and keep going
                if (stateStack.size() <= 1 && lookahead != null && !"$end".equals(lookahead.type)) {
                    lookahead = null;
                    errtoken  = null;
                    currentState = 0;
                    lookaheadstack.clear();
                    continue;
                }

                // Case 2: at end-of-file with non-trivial stack → bail out
                if (lookahead != null && "$end".equals(lookahead.type)) {
                    return null;
                }

                if (lookahead != null && !"error".equals(lookahead.type)) {
                    YaccSymbol topSym = symStack.get(symStack.size() - 1);
                    if ("error".equals(topSym.type)) {
                        // Error already on top — just discard the lookahead
                        lookahead = null;
                        continue;
                    }

                    // Create an error symbol wrapping the bad token
                    YaccSymbol errSym = new YaccSymbol("error", lookahead);
                    if (lookahead.hasLineno) {
                        errSym.lineno    = lookahead.lineno;
                        errSym.endlineno = lookahead.lineno;
                        errSym.hasLineno = true;
                    }
                    if (lookahead.hasLexpos) {
                        errSym.lexpos    = lookahead.lexpos;
                        errSym.endlexpos = lookahead.lexpos;
                        errSym.hasLexpos = true;
                    }
                    lookaheadstack.push(lookahead);
                    lookahead = errSym;
                } else {
                    // Pop the stack
                    symStack.remove(symStack.size() - 1);
                    stateStack.remove(stateStack.size() - 1);
                    currentState = stateStack.get(stateStack.size() - 1);
                }
                continue;
            }

            throw new RuntimeException("yacc: internal parser error");
        }
    }

    // =========================================================================
    // parseopt (with position tracking)
    // =========================================================================

    /**
     * Parse with position tracking enabled.
     * Mirrors PLY's parseopt() from yacc.py.
     *
     * Same logic as {@link #parseopt_notrack} but also sets
     * {@code sym.lineno} / {@code sym.lexpos} from lexer state on empty
     * productions, and copies start/end positions for non-empty ones.
     */
    public Object parseopt(Supplier<YaccSymbol> get_token, Object lexer) {
        this.tokenSupplier = get_token;
        this.lexer = lexer;

        YaccSymbol lookahead = null;
        Deque<YaccSymbol> lookaheadstack = new ArrayDeque<>();

        List<Integer>    stateStack = new ArrayList<>();
        List<YaccSymbol> symStack   = new ArrayList<>();

        // Expose stacks for error recovery
        this.statestack = stateStack;
        this.symstack   = symStack;

        YaccProduction pslice = new YaccProduction(null, symStack);
        pslice.lexer  = lexer;
        pslice.parser = this;

        YaccSymbol errtoken  = null;
        int        errorcount = 0;
        this.errorok = true;

        stateStack.add(0);
        symStack.add(new YaccSymbol("$end", null));
        int currentState = 0;

        while (true) {
            Integer t;
            if (!defaulted_states.containsKey(currentState)) {
                if (lookahead == null) {
                    lookahead = lookaheadstack.isEmpty() ? get_token.get() : lookaheadstack.pop();
                    if (lookahead == null) lookahead = new YaccSymbol("$end", null);
                }
                Map<String, Integer> stateActions = action.get(currentState);
                t = stateActions != null ? stateActions.get(lookahead.type) : null;
            } else {
                t = defaulted_states.get(currentState);
            }

            if (t != null) {
                if (t > 0) {
                    stateStack.add(t);
                    currentState = t;
                    symStack.add(lookahead);
                    lookahead = null;
                    if (errorcount > 0) errorcount--;
                    continue;
                }

                if (t < 0) {
                    MiniProduction p = productions.get(-t);
                    String pname = p.name;
                    int    plen  = p.len;
                    YaccSymbol sym = new YaccSymbol(pname, null);

                    if (plen > 0) {
                        int start = symStack.size() - plen;
                        List<YaccSymbol> targ = new ArrayList<>(plen + 1);
                        targ.add(sym);
                        for (int i = start; i < symStack.size(); i++) targ.add(symStack.get(i));

                        // Tracking: copy position from first/last RHS symbols
                        YaccSymbol t1 = targ.get(1);
                        sym.lineno    = t1.lineno;
                        sym.lexpos    = t1.lexpos;
                        sym.hasLineno = t1.hasLineno;
                        sym.hasLexpos = t1.hasLexpos;
                        YaccSymbol tLast = targ.get(targ.size() - 1);
                        sym.endlineno = tLast.hasLineno ? tLast.endlineno : tLast.lineno;
                        sym.endlexpos = tLast.hasLexpos ? tLast.endlexpos : tLast.lexpos;

                        pslice.slice = targ;
                        try {
                            removeLastN(symStack, plen);
                            this.state = currentState;
                            if (p.callable != null) p.callable.accept(pslice);
                            removeLastN(stateStack, plen);
                            symStack.add(sym);
                            Map<String, Integer> gotoRow = gotoTable.get(stateStack.get(stateStack.size() - 1));
                            currentState = gotoRow != null ? gotoRow.getOrDefault(pname, 0) : 0;
                            stateStack.add(currentState);
                        } catch (YaccProduction.ParseSyntaxError e) {
                            if (lookahead != null) lookaheadstack.push(lookahead);
                            for (int i = 1; i < targ.size() - 1; i++) symStack.add(targ.get(i));
                            stateStack.remove(stateStack.size() - 1);
                            currentState = stateStack.get(stateStack.size() - 1);
                            sym.type  = "error";
                            sym.value = "error";
                            lookahead = sym;
                            errorcount = ERROR_COUNT;
                            this.errorok = false;
                        }
                        continue;
                    } else {
                        // Empty production — take position from lexer if available
                        if (lexer instanceof Lexer) {
                            Lexer lx = (Lexer) lexer;
                            sym.lineno    = lx.lineno;
                            sym.hasLineno = true;
                        }
                        List<YaccSymbol> targ = new ArrayList<>();
                        targ.add(sym);
                        pslice.slice = targ;
                        try {
                            this.state = currentState;
                            if (p.callable != null) p.callable.accept(pslice);
                            symStack.add(sym);
                            Map<String, Integer> gotoRow = gotoTable.get(stateStack.get(stateStack.size() - 1));
                            currentState = gotoRow != null ? gotoRow.getOrDefault(pname, 0) : 0;
                            stateStack.add(currentState);
                        } catch (YaccProduction.ParseSyntaxError e) {
                            if (lookahead != null) lookaheadstack.push(lookahead);
                            stateStack.remove(stateStack.size() - 1);
                            currentState = stateStack.get(stateStack.size() - 1);
                            sym.type  = "error";
                            sym.value = "error";
                            lookahead = sym;
                            errorcount = ERROR_COUNT;
                            this.errorok = false;
                        }
                        continue;
                    }
                }

                if (t == 0) {
                    return symStack.get(symStack.size() - 1).value;
                }
            }

            // Syntax error handling (same logic as parseopt_notrack)
            if (t == null) {
                if (errorcount == 0 || this.errorok) {
                    errorcount = ERROR_COUNT;
                    this.errorok = false;
                    errtoken = lookahead;
                    if (errtoken != null && "$end".equals(errtoken.type)) errtoken = null;

                    if (errorfunc != null) {
                        this.state = currentState;
                        errorfunc.accept(errtoken);
                        if (this.errorok) { errtoken = null; continue; }
                    } else {
                        if (errtoken != null) {
                            if (errtoken.hasLineno) {
                                System.err.printf("yacc: Syntax error at line %d, token=%s%n",
                                                  errtoken.lineno, errtoken.type);
                            } else {
                                System.err.printf("yacc: Syntax error, token=%s%n", errtoken.type);
                            }
                        } else {
                            System.err.println("yacc: Parse error in input. EOF");
                            return null;
                        }
                    }
                } else {
                    errorcount = ERROR_COUNT;
                }

                if (stateStack.size() <= 1 && lookahead != null && !"$end".equals(lookahead.type)) {
                    lookahead = null; errtoken = null; currentState = 0;
                    lookaheadstack.clear(); continue;
                }
                if (lookahead != null && "$end".equals(lookahead.type)) return null;

                if (lookahead != null && !"error".equals(lookahead.type)) {
                    YaccSymbol topSym = symStack.get(symStack.size() - 1);
                    if ("error".equals(topSym.type)) {
                        // Update end position to cover the bad token
                        if (lookahead.hasLineno) { topSym.endlineno = lookahead.lineno; }
                        if (lookahead.hasLexpos) { topSym.endlexpos = lookahead.lexpos; }
                        lookahead = null; continue;
                    }
                    YaccSymbol errSym = new YaccSymbol("error", lookahead);
                    if (lookahead.hasLineno) {
                        errSym.lineno = errSym.endlineno = lookahead.lineno;
                        errSym.hasLineno = true;
                    }
                    if (lookahead.hasLexpos) {
                        errSym.lexpos = errSym.endlexpos = lookahead.lexpos;
                        errSym.hasLexpos = true;
                    }
                    lookaheadstack.push(lookahead);
                    lookahead = errSym;
                } else {
                    YaccSymbol popped = symStack.remove(symStack.size() - 1);
                    if (lookahead != null && lookahead.hasLineno && popped.hasLineno) {
                        lookahead.lineno = popped.lineno;
                    }
                    if (lookahead != null && lookahead.hasLexpos && popped.hasLexpos) {
                        lookahead.lexpos = popped.lexpos;
                    }
                    stateStack.remove(stateStack.size() - 1);
                    currentState = stateStack.get(stateStack.size() - 1);
                }
                continue;
            }

            throw new RuntimeException("yacc: internal parser error");
        }
    }

    // =========================================================================
    // Utility
    // =========================================================================

    /** Remove the last n elements from a list. */
    private static <T> void removeLastN(List<T> list, int n) {
        int size = list.size();
        list.subList(size - n, size).clear();
    }
}
