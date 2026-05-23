// Translated by Claude Sonnet 4.6
package com.pcpp.ply;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Collects lexer specification from an annotated Java object and populates a
 * {@link LexerSpec}.
 *
 * <p>This mirrors the {@code LexerReflect} class from PLY's {@code lex.py},
 * which uses Python reflection to discover {@code tokens}, {@code literals},
 * {@code states}, and {@code t_XXX} attributes on a module object.
 *
 * <h2>Conventions for the host object</h2>
 * <ul>
 *   <li>{@code public String[] tokens} or {@code public static String[] tokens}
 *       — list of token names.</li>
 *   <li>{@code public String literals} or {@code public static String literals}
 *       — string of single-character literal tokens (optional).</li>
 *   <li>{@code public String[][] states} or {@code public static String[][] states}
 *       — array of {@code {"stateName","inclusive|exclusive"}} pairs (optional).</li>
 *   <li>Methods named {@code t_TOKENNAME(LexToken)} with an optional
 *       {@link LexPattern} annotation supplying the regex, or whose Javadoc
 *       comment is used via the annotation.  If no {@link LexPattern} is
 *       present the method's first string constant field or the
 *       {@link LexPattern} annotation on the method is used.</li>
 *   <li>Fields named {@code t_TOKENNAME} of type {@code String} — simple
 *       string rules.</li>
 *   <li>Methods or fields named {@code t_error} — error handler.</li>
 *   <li>Methods or fields named {@code t_eof} — EOF handler.</li>
 *   <li>Fields named {@code t_ignore} — ignore string.</li>
 * </ul>
 *
 * <p>The state-prefix convention matches PLY:
 * {@code t_[state1_state2_...]tokenname}.  {@code ANY} expands to all states.
 */
public class LexerReflect {

    private static final Pattern IS_IDENTIFIER = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final Object spec;
    private final PlyLogger log;
    private boolean hasError;

    /**
     * @param spec the object to reflect (has {@code tokens}, {@code t_XXX}
     *             fields/methods, etc.)
     * @param log  logger for warnings / errors
     */
    public LexerReflect(Object spec, PlyLogger log) {
        this.spec = spec;
        this.log = log;
        this.hasError = false;
    }

    /** Returns {@code true} if any validation errors were encountered. */
    public boolean hasError() {
        return hasError;
    }

    // -----------------------------------------------------------------------
    // Public entry point
    // -----------------------------------------------------------------------

    /**
     * Populate {@code target} by reflecting on the host object supplied at
     * construction time.
     *
     * @param target the {@link LexerSpec} to populate
     */
    public void populate(LexerSpec target) {
        Map<String, String> stateInfo = new HashMap<>();
        stateInfo.put("INITIAL", "inclusive");

        getTokens(target);
        getLiterals(target);
        getStates(target, stateInfo);
        getRules(target, stateInfo);
    }

    // -----------------------------------------------------------------------
    // tokens
    // -----------------------------------------------------------------------

    private void getTokens(LexerSpec target) {
        Object val = getFieldValue("tokens");
        if (val == null) {
            log.error("No token list is defined");
            hasError = true;
            return;
        }
        String[] arr = toStringArray(val);
        if (arr == null || arr.length == 0) {
            log.error("tokens must be a non-empty array of strings");
            hasError = true;
            return;
        }
        // Validate
        Set<String> seen = new HashSet<>();
        for (String n : arr) {
            if (!IS_IDENTIFIER.matcher(n).matches()) {
                log.error("Bad token name '%s'", n);
                hasError = true;
            }
            if (!seen.add(n)) {
                log.warning("Token '%s' multiply defined", n);
            }
        }
        target.addTokens(arr);
    }

    // -----------------------------------------------------------------------
    // literals
    // -----------------------------------------------------------------------

    private void getLiterals(LexerSpec target) {
        Object val = getFieldValue("literals");
        if (val == null) return;
        if (val instanceof String) {
            String s = (String) val;
            for (char c : s.toCharArray()) {
                target.addLiteral(c);
            }
        } else if (val instanceof char[]) {
            for (char c : (char[]) val) {
                target.addLiteral(c);
            }
        } else if (val instanceof String[]) {
            for (String s : (String[]) val) {
                if (s.length() != 1) {
                    log.error("Invalid literal '%s'. Must be a single character", s);
                    hasError = true;
                } else {
                    target.addLiteral(s.charAt(0));
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // states
    // -----------------------------------------------------------------------

    private void getStates(LexerSpec target, Map<String, String> stateInfo) {
        Object val = getFieldValue("states");
        if (val == null) return;
        if (!(val instanceof String[][])) {
            log.error("states must be a String[][] of {\"name\",\"inclusive|exclusive\"} pairs");
            hasError = true;
            return;
        }
        String[][] arr = (String[][]) val;
        for (String[] pair : arr) {
            if (pair.length != 2) {
                log.error("Invalid state specifier. Each entry must be {\"name\",\"inclusive|exclusive\"}");
                hasError = true;
                continue;
            }
            String name = pair[0];
            String type = pair[1];
            if (!("inclusive".equals(type) || "exclusive".equals(type))) {
                log.error("State type for state '%s' must be 'inclusive' or 'exclusive'", name);
                hasError = true;
                continue;
            }
            if (stateInfo.containsKey(name)) {
                log.error("State '%s' already defined", name);
                hasError = true;
                continue;
            }
            stateInfo.put(name, type);
            target.addState(name, type);
        }
    }

    // -----------------------------------------------------------------------
    // rules (t_XXX)
    // -----------------------------------------------------------------------

    /**
     * Discovers all {@code t_XXX} methods and fields on the host object and
     * adds corresponding {@link LexRule}s to {@code target}.
     */
    private void getRules(LexerSpec target, Map<String, String> stateInfo) {
        Class<?> cls = spec.getClass();

        // Collect t_ fields (string rules, ignore, etc.)
        List<Field> tFields = new ArrayList<>();
        for (Field f : cls.getFields()) {
            if (f.getName().startsWith("t_")) {
                tFields.add(f);
            }
        }
        // Also declared (including inherited protected/package)
        for (Field f : cls.getDeclaredFields()) {
            if (f.getName().startsWith("t_") && !tFields.contains(f)) {
                f.setAccessible(true);
                tFields.add(f);
            }
        }

        // Collect t_ methods (function rules)
        List<Method> tMethods = new ArrayList<>();
        for (Method m : cls.getMethods()) {
            if (m.getName().startsWith("t_")) {
                tMethods.add(m);
            }
        }
        for (Method m : cls.getDeclaredMethods()) {
            if (m.getName().startsWith("t_") && !tMethods.contains(m)) {
                m.setAccessible(true);
                tMethods.add(m);
            }
        }

        Set<String> allStateNames = stateInfo.keySet();

        // Process string rules from fields
        for (Field f : tFields) {
            String fname = f.getName();
            Object val;
            try {
                val = f.get(spec);
            } catch (IllegalAccessException e) {
                continue;
            }
            if (!(val instanceof String)) continue;
            String pattern = (String) val;

            String[] stateToken = stateToken(fname, allStateNames);
            String[] states = stateTokenStates(fname, allStateNames);
            String tokenName = stateToken[1];

            if ("ignore".equals(tokenName)) {
                for (String s : states) {
                    target.setIgnore(s, pattern);
                }
                continue;
            }
            if ("error".equals(tokenName)) {
                log.error("Rule '%s' must be defined as a method (function)", fname);
                hasError = true;
                continue;
            }

            // Validate pattern
            if (!validatePattern(fname, pattern, stateInfo.keySet())) continue;

            boolean isIgnoreRule = tokenName.startsWith("ignore_");
            for (String s : states) {
                String ruleName = stateSpecificRuleName(fname, s, states, allStateNames);
                target.addRule(new LexRule(ruleName, pattern, isIgnoreRule));
            }
        }

        // Process function rules from methods
        for (Method m : tMethods) {
            String mname = m.getName();

            // Get the pattern from @LexPattern annotation or method's static field
            String pattern = getMethodPattern(m);

            String[] states = stateTokenStates(mname, allStateNames);
            String[] stateToken = stateToken(mname, allStateNames);
            String tokenName = stateToken[1];

            if ("error".equals(tokenName)) {
                // Error handler
                final Method errMethod = m;
                TokenCallback cb = tok -> {
                    try {
                        Object result = errMethod.invoke(spec, tok);
                        return (result instanceof LexToken) ? (LexToken) result : null;
                    } catch (Exception ex) {
                        throw new RuntimeException("Error in t_error handler", ex);
                    }
                };
                for (String s : states) {
                    target.setErrorCallback(s, cb);
                }
                continue;
            }

            if ("eof".equals(tokenName)) {
                // EOF handler
                final Method eofMethod = m;
                TokenCallback cb = tok -> {
                    try {
                        Object result = eofMethod.invoke(spec, tok);
                        return (result instanceof LexToken) ? (LexToken) result : null;
                    } catch (Exception ex) {
                        throw new RuntimeException("Error in t_eof handler", ex);
                    }
                };
                for (String s : states) {
                    target.setEofCallback(s, cb);
                }
                continue;
            }

            if ("ignore".equals(tokenName)) {
                log.error("Rule '%s' must be defined as a String field, not a method", mname);
                hasError = true;
                continue;
            }

            if (pattern == null || pattern.isEmpty()) {
                log.error("No regular expression defined for rule '%s'", mname);
                hasError = true;
                continue;
            }

            // Validate
            if (!validatePattern(mname, pattern, stateInfo.keySet())) continue;

            // Build callback
            final Method ruleMethod = m;
            final String finalTokenName = tokenName;
            TokenCallback cb = tok -> {
                try {
                    Object result = ruleMethod.invoke(spec, tok);
                    return (result instanceof LexToken) ? (LexToken) result : null;
                } catch (Exception ex) {
                    throw new RuntimeException("Error in rule handler " + ruleMethod.getName(), ex);
                }
            };

            for (String s : states) {
                String ruleName = stateSpecificRuleName(mname, s, states, allStateNames);
                target.addRule(new LexRule(ruleName, pattern, cb));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Mirrors PLY's {@code _statetoken(s, names)} function.
     *
     * <p>Given a rule name like {@code "t_foo_bar_SPAM"} and the set of known
     * state names, returns {@code {states[], tokenName}}.
     */
    private String[] stateToken(String s, Set<String> names) {
        String[] parts = s.split("_", -1);
        // parts[0] == "t"
        int i = 1;
        for (; i < parts.length; i++) {
            if (!names.contains(parts[i]) && !"ANY".equals(parts[i])) {
                break;
            }
        }
        // states are parts[1..i-1], token is parts[i..]
        String tokenName = String.join("_", Arrays.copyOfRange(parts, i, parts.length));
        return new String[]{s, tokenName};
    }

    private String[] stateTokenStates(String s, Set<String> allNames) {
        String[] parts = s.split("_", -1);
        int i = 1;
        List<String> stateList = new ArrayList<>();
        for (; i < parts.length; i++) {
            if (allNames.contains(parts[i])) {
                stateList.add(parts[i]);
            } else if ("ANY".equals(parts[i])) {
                stateList.addAll(allNames);
            } else {
                break;
            }
        }
        if (stateList.isEmpty()) {
            stateList.add("INITIAL");
        }
        return stateList.toArray(new String[0]);
    }

    /**
     * Generates a per-state rule name that is unique within the master regex.
     * For multi-state rules we embed the state to avoid group-name collisions.
     */
    private String stateSpecificRuleName(String originalName, String state,
                                          String[] allStates, Set<String> allStateNames) {
        if (allStates.length == 1 && "INITIAL".equals(state)) {
            return originalName;
        }
        // Replace state prefix in name with the specific state
        // e.g. "t_ID" used in state "COMMENT" becomes "t_COMMENT_ID"
        String[] parts = originalName.split("_", -1);
        int i = 1;
        while (i < parts.length && (allStateNames.contains(parts[i]) || "ANY".equals(parts[i]))) {
            i++;
        }
        String tokenPart = String.join("_", Arrays.copyOfRange(parts, i, parts.length));
        return "t_" + state + "_" + tokenPart;
    }

    /**
     * Extract a regex pattern for a method. Looks for a {@link LexPattern}
     * annotation first, then falls back to a static String field with the
     * same name on the declaring class.
     */
    private String getMethodPattern(Method m) {
        LexPattern ann = m.getAnnotation(LexPattern.class);
        if (ann != null) {
            return ann.value();
        }
        // Fallback: look for a static String field with the same name
        try {
            Field f = m.getDeclaringClass().getDeclaredField(m.getName());
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                f.setAccessible(true);
                return (String) f.get(null);
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return null;
    }

    private boolean validatePattern(String name, String pattern, Set<String> stateNames) {
        try {
            Pattern p = Pattern.compile("(?<" + name + ">" + pattern + ")",
                    Pattern.COMMENTS);
            if (p.matcher("").matches()) {
                log.error("Regular expression for rule '%s' matches empty string", name);
                hasError = true;
                return false;
            }
        } catch (PatternSyntaxException e) {
            log.error("Invalid regular expression for rule '%s': %s", name, e.getMessage());
            hasError = true;
            return false;
        }
        return true;
    }

    /** Retrieve a field value (static or instance) by name, or {@code null}. */
    private Object getFieldValue(String name) {
        Class<?> cls = spec.getClass();
        try {
            Field f = cls.getField(name);
            return f.get(spec);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(spec);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return null;
    }

    private static String[] toStringArray(Object val) {
        if (val instanceof String[]) return (String[]) val;
        if (val instanceof List) {
            List<?> list = (List<?>) val;
            String[] arr = new String[list.size()];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = list.get(i).toString();
            }
            return arr;
        }
        return null;
    }
}
