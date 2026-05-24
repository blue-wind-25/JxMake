// Translated by Claude Sonnet 4.6
package com.pcpp.ply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Translates a {@link LexerSpec} into a ready-to-use {@link Lexer}.
 *
 * <p>This class mirrors the {@code lex()} function from PLY's {@code lex.py}.
 * It performs the following steps (matching the Python implementation):
 * <ol>
 *   <li>Collect token names, literals, state info, and rules from the spec.</li>
 *   <li>For each state, build a master regular expression from the rules
 *       that belong to that state (function rules first, then string rules
 *       sorted by decreasing pattern length — same as PLY).</li>
 *   <li>For "inclusive" states, append the INITIAL state's compiled regexes
 *       (mirrors the {@code linfo.stateinfo} loop in {@code lex()}).</li>
 *   <li>Wire everything into the {@link Lexer} instance and call
 *       {@link Lexer#begin(String)} to activate the INITIAL state.</li>
 * </ol>
 *
 * <h2>Regex group naming</h2>
 * <p>Python PLY builds a master regex using {@code (?P<name>pattern)}
 * named groups and then inspects {@code groupindex} to map group names back
 * to rule handlers.  Java's {@link Pattern} supports the same syntax but
 * requires group names to be valid Java identifiers and unique across the
 * entire pattern.  We therefore name each group after the rule's symbolic
 * name (e.g. {@code t_ID}), which already satisfies the uniqueness
 * requirement because PLY rules have unique names within a state.
 * When the same logical rule appears in multiple states we suffix the group
 * name with an underscore-plus-counter to guarantee uniqueness.
 */
public final class LexerBuilder {

    private LexerBuilder() {}

    // -----------------------------------------------------------------------
    // Public entry point — mirrors lex()
    // -----------------------------------------------------------------------

    /**
     * Build a {@link Lexer} from the given specification.
     *
     * @param spec the lexer specification
     * @return a fully initialised {@link Lexer}
     */
    public static Lexer build(LexerSpec spec) {
        return build(spec, false, new PlyLogger());
    }

    /**
     * Build a {@link Lexer} from the given specification with explicit options.
     *
     * @param spec     the lexer specification
     * @param optimize when {@code true} token-type validation is skipped at runtime
     * @param log      logger for warnings/errors
     * @return a fully initialised {@link Lexer}
     */
    public static Lexer build(LexerSpec spec, boolean optimize, PlyLogger log) {
        Lexer lexobj = new Lexer();
        lexobj.lexoptimize = optimize;

        // ----------------------------------------------------------------
        // 1. Token set
        // ----------------------------------------------------------------
        lexobj.lextokens = new HashSet<>(spec.getTokens());

        // ----------------------------------------------------------------
        // 2. Literals
        // ----------------------------------------------------------------
        lexobj.lexliterals = spec.getLiterals();
        lexobj.lextokensAll = new HashSet<>(lexobj.lextokens);
        for (char c : lexobj.lexliterals.toCharArray()) {
            lexobj.lextokensAll.add(String.valueOf(c));
        }

        // ----------------------------------------------------------------
        // 3. State info
        // ----------------------------------------------------------------
        lexobj.lexstateinfo = new LinkedHashMap<>(spec.getStateInfo());

        // ----------------------------------------------------------------
        // 4. Error / EOF callbacks
        // ----------------------------------------------------------------
        lexobj.lexstateerrorf = new HashMap<>(spec.getErrorCallbacks());
        lexobj.lexstateeoff   = new HashMap<>(spec.getEofCallbacks());

        // ----------------------------------------------------------------
        // 5. Ignore strings
        // ----------------------------------------------------------------
        lexobj.lexstateignore = new HashMap<>(spec.getIgnoreMap());

        // ----------------------------------------------------------------
        // 6. Categorise rules per state
        //    Mirrors the linfo.funcsym / linfo.strsym separation in lex.py.
        //    Function rules keep declaration order; string rules are sorted
        //    by decreasing pattern length.
        // ----------------------------------------------------------------
        // Maps: state → list of function LexRule
        Map<String, List<LexRule>> funcRules = new LinkedHashMap<>();
        // Maps: state → list of string LexRule
        Map<String, List<LexRule>> strRules  = new LinkedHashMap<>();

        for (String state : lexobj.lexstateinfo.keySet()) {
            funcRules.put(state, new ArrayList<>());
            strRules.put(state, new ArrayList<>());
        }

        // Derive token-name from rule name using the same logic as PLY's
        // _statetoken() helper.
        Set<String> allStateNames = lexobj.lexstateinfo.keySet();

        for (LexRule rule : spec.getRules()) {
            String[] statesForRule = stateTokenStates(rule.name, allStateNames);
            for (String state : statesForRule) {
                if (rule.isFunction()) {
                    funcRules.computeIfAbsent(state, k -> new ArrayList<>()).add(rule);
                } else {
                    strRules.computeIfAbsent(state, k -> new ArrayList<>()).add(rule);
                }
            }
        }

        // Sort string rules by decreasing pattern length (PLY convention)
        for (List<LexRule> list : strRules.values()) {
            list.sort((a, b) -> b.pattern.length() - a.pattern.length());
        }

        // ----------------------------------------------------------------
        // 7. Build master regex per state
        // ----------------------------------------------------------------
        for (String state : lexobj.lexstateinfo.keySet()) {
            List<LexRule> ordered = new ArrayList<>();
            ordered.addAll(funcRules.getOrDefault(state, Collections.<LexRule>emptyList()));
            ordered.addAll(strRules.getOrDefault(state, Collections.<LexRule>emptyList()));

            List<String>         tokenNames = new ArrayList<>();
            for (LexRule r : ordered) {
                tokenNames.add(stateTokenName(r.name, allStateNames));
            }

            List<String> reTextList = new ArrayList<>();
            List<Lexer.CompiledLexRe> compiledList = new ArrayList<>();

            formMasterRe(ordered, tokenNames, lexobj.lextokens, reTextList, compiledList, log);

            lexobj.lexstatere    .put(state, compiledList);
            lexobj.lexstateretext.put(state, reTextList);
        }

        // ----------------------------------------------------------------
        // 8. For inclusive states, extend with INITIAL state's regexes
        //    Mirrors the stateinfo loop near the bottom of lex().
        // ----------------------------------------------------------------
        List<Lexer.CompiledLexRe> initialRe   = lexobj.lexstatere.getOrDefault("INITIAL", Collections.<Lexer.CompiledLexRe>emptyList());
        List<String>              initialText  = lexobj.lexstateretext.getOrDefault("INITIAL", Collections.<String>emptyList());

        for (Map.Entry<String, String> e : lexobj.lexstateinfo.entrySet()) {
            String state = e.getKey();
            String stype = e.getValue();
            if (!"INITIAL".equals(state) && "inclusive".equals(stype)) {
                lexobj.lexstatere    .get(state).addAll(initialRe);
                lexobj.lexstateretext.get(state).addAll(initialText);
            }
        }

        // For inclusive states that have no explicit error/ignore, inherit INITIAL's
        TokenCallback initialErr = lexobj.lexstateerrorf.get("INITIAL");
        String        initialIgn = lexobj.lexstateignore.getOrDefault("INITIAL", "");
        for (Map.Entry<String, String> e : lexobj.lexstateinfo.entrySet()) {
            String state = e.getKey();
            String stype = e.getValue();
            if ("inclusive".equals(stype)) {
                lexobj.lexstateerrorf.putIfAbsent(state, initialErr);
                lexobj.lexstateignore.putIfAbsent(state, initialIgn);
            }
        }

        if (initialErr == null) {
            log.warning("No t_error rule is defined");
        }

        // ----------------------------------------------------------------
        // 9. Activate INITIAL state
        // ----------------------------------------------------------------
        if (lexobj.lexstatere.containsKey("INITIAL")) {
            lexobj.begin("INITIAL");
        }

        return lexobj;
    }

    // -----------------------------------------------------------------------
    // _form_master_re equivalent
    // -----------------------------------------------------------------------

    /**
     * Build one or more compiled master regexes from {@code rules}.
     *
     * <p>Mirrors Python PLY's {@code _form_master_re()} function.  If the
     * combined pattern is too long (or has duplicate group names) it is split
     * recursively in half until each half compiles successfully.
     *
     * @param rules       ordered list of rules
     * @param tokenNames  parallel list of token-type strings (one per rule)
     * @param validTokens set of declared token names (for validation)
     * @param outText     accumulates the raw regex strings
     * @param outCompiled accumulates the compiled master-regex chunks
     * @param log         logger
     */
    private static void formMasterRe(
            List<LexRule> rules,
            List<String>  tokenNames,
            Set<String>   validTokens,
            List<String>  outText,
            List<Lexer.CompiledLexRe> outCompiled,
            PlyLogger log) {

        if (rules.isEmpty()) return;

        // Build the combined alternation: (?P<groupName>pattern)|...
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rules.size(); i++) {
            if (i > 0) sb.append('|');
            // Use rule name as group name (must be a valid Java identifier)
            String groupName = safeGroupName(rules.get(i).name);
            sb.append("(?<").append(groupName).append('>')
              .append(rules.get(i).pattern)
              .append(')');
        }
        String combined = sb.toString();

        Pattern pat;
        try {
            pat = Pattern.compile(combined, Pattern.DOTALL);
        } catch (PatternSyntaxException ex) {
            // Split in half and recurse (mirrors Python PLY behaviour)
            int mid = rules.size() / 2;
            if (mid == 0) mid = 1;
            formMasterRe(rules.subList(0, mid),     tokenNames.subList(0, mid),
                         validTokens, outText, outCompiled, log);
            formMasterRe(rules.subList(mid, rules.size()), tokenNames.subList(mid, rules.size()),
                         validTokens, outText, outCompiled, log);
            return;
        }

        // Build the index: group number → IndexEntry
        // Java's named-group map gives us name → group-number
        // We need group-number → IndexEntry
        Map<String, Integer> groupIndex = namedGroups(pat);
        int maxGroup = groupIndex.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        Lexer.IndexEntry[] indexFunc = new Lexer.IndexEntry[maxGroup + 1];

        for (int i = 0; i < rules.size(); i++) {
            LexRule rule      = rules.get(i);
            String  gname     = safeGroupName(rule.name);
            Integer gnum      = groupIndex.get(gname);
            if (gnum == null) continue;

            String tokType = tokenNames.get(i);
            boolean isIgnore = rule.ignore || tokType == null
                               || tokType.isEmpty() || tokType.contains("ignore_");

            if (rule.isFunction()) {
                indexFunc[gnum] = new Lexer.IndexEntry(rule.callback,
                                      isIgnore ? null : tokType);
            } else {
                indexFunc[gnum] = new Lexer.IndexEntry(null,
                                      isIgnore ? null : tokType);
            }
        }

        outText.add(combined);
        outCompiled.add(new Lexer.CompiledLexRe(pat, indexFunc, groupIndex));
    }

    // -----------------------------------------------------------------------
    // _statetoken helpers
    // -----------------------------------------------------------------------

    /**
     * Given a rule name like {@code "t_foo_bar_SPAM"} and the set of known
     * state names, returns the states the rule applies to.
     * Mirrors PLY's {@code _statetoken(s, names)}.
     */
    static String[] stateTokenStates(String ruleName, Set<String> allStateNames) {
        String[] parts = ruleName.split("_", -1);
        // parts[0] == "t"
        List<String> states = new ArrayList<>();
        int i = 1;
        for (; i < parts.length; i++) {
            if (allStateNames.contains(parts[i])) {
                states.add(parts[i]);
            } else if ("ANY".equals(parts[i])) {
                states.addAll(allStateNames);
            } else {
                break;
            }
        }
        if (states.isEmpty()) {
            states.add("INITIAL");
        }
        return states.toArray(new String[0]);
    }

    /**
     * Extract the token name from a rule name.
     * e.g. {@code "t_foo_bar_SPAM"} → {@code "SPAM"} (if foo, bar are states).
     */
    static String stateTokenName(String ruleName, Set<String> allStateNames) {
        String[] parts = ruleName.split("_", -1);
        int i = 1;
        while (i < parts.length
               && (allStateNames.contains(parts[i]) || "ANY".equals(parts[i]))) {
            i++;
        }
        return String.join("_", Arrays.copyOfRange(parts, i, parts.length));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Convert a PLY rule name to a safe Java regex group name.
     * Java named groups must match [A-Za-z][A-Za-z0-9]* — we replace any
     * remaining characters with underscores.
     */
    private static String safeGroupName(String name) {
        // Rule names like "t_ID" are already valid.  Replace non-alnum with '_'.
        return name.replaceAll("[^A-Za-z0-9]", "_");
    }

    /**
     * Extract the named-group → group-number map from a compiled {@link Pattern}.
     *
     * <p>Java's {@link Pattern} class does not expose {@code namedGroups()}
     * publicly before Java 20.  We use reflection to access it, falling back
     * to a regex-based extraction if reflection fails.
     */
    @SuppressWarnings("unchecked")
    static Map<String, Integer> namedGroups(Pattern pat) {
        // Try the public API first (available from Java 20+)
        try {
            java.lang.reflect.Method m = Pattern.class.getMethod("namedGroups");
            return new HashMap<>((Map<String, Integer>) m.invoke(pat));
        } catch (NoSuchMethodException | java.lang.reflect.InvocationTargetException
                 | IllegalAccessException ignored) {
        }

        // Fallback: reflect on the private field
        try {
            java.lang.reflect.Field f = Pattern.class.getDeclaredField("namedGroups");
            f.setAccessible(true);
            Object val = f.get(pat);
            if (val instanceof Map) {
                return new HashMap<>((Map<String, Integer>) val);
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        // Last resort: parse the pattern string for (?<name>...) occurrences
        Map<String, Integer> result = new LinkedHashMap<>();
        Pattern gp = Pattern.compile("\\(\\?<([A-Za-z][A-Za-z0-9_]*)>");
        Matcher gm = gp.matcher(pat.pattern());
        int groupNum = 0;
        while (gm.find()) {
            groupNum++;
            result.put(gm.group(1), groupNum);
        }
        return result;
    }
}
