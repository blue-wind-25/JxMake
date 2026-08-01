package com.jxmake.formatter.gdr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Determines which source lines the GDR reindenter is allowed to touch.
 * Content that must never be reindented -- interior lines of a multi-line
 * string/comment/preprocessor-directive token -- per
 * {@code STATE_CURLY_GDR.md}'s "Content that must never be touched"
 * background note. This is an inherent correctness requirement of the
 * reindenter itself (touching a raw string or comment continuation line
 * would corrupt content), distinct from the later, separate checklist
 * item for *opt-in* exclusion zones (`frozen`/`JXM_CFMT_GDR 0`/`1`
 * spans) -- those are additional regions a user asks to exclude; this is
 * content the reindenter could never safely touch in the first place.
 *
 * <p>A line is untouchable if any part of it falls inside a
 * {@link GdrTokenType#STRING}, {@link GdrTokenType#CHAR},
 * {@link GdrTokenType#BLOCK_COMMENT}, or
 * {@link GdrTokenType#PREPROCESSOR} token that spans multiple physical
 * lines (a single-line token, e.g. an ordinary {@code "foo"} string, does
 * not make its own line untouchable on its own -- leading whitespace on
 * that line is still a normal reindent target; it's only the token's own
 * *interior continuation lines*, which have no leading whitespace of
 * their own to reindent, that must be skipped). A single-line
 * {@link GdrTokenType#LINE_COMMENT} never spans multiple lines by
 * construction, so it never contributes here.
 */
public final class GdrLineTouchability {

    private GdrLineTouchability() {
    }

    /**
     * Returns the set of 0-based line numbers that must NOT be reindented:
     * every line strictly after a multi-line token's start line, up to and
     * including its end line.
     */
    public static Set<Integer> computeUntouchableLines(List<GdrToken> tokens) {
        Set<Integer> untouchable = new HashSet<>();
        for (GdrToken t : tokens) {
            if (!isPotentiallyMultiline(t.type)) {
                continue;
            }
            int newlineCount = countNewlines(t.text);
            if (newlineCount == 0) {
                continue;
            }
            for (int k = 1; k <= newlineCount; k++) {
                untouchable.add(t.line + k);
            }
        }
        return untouchable;
    }

    /** Convenience: same result as a line-indexed boolean list, 0=touchable. */
    public static List<Boolean> computeTouchableByLine(List<GdrToken> tokens, int totalLines) {
        Set<Integer> untouchable = computeUntouchableLines(tokens);
        List<Boolean> result = new ArrayList<>(totalLines);
        for (int line = 0; line < totalLines; line++) {
            result.add(!untouchable.contains(line));
        }
        return result;
    }

    private static boolean isPotentiallyMultiline(GdrTokenType type) {
        return type == GdrTokenType.STRING
                || type == GdrTokenType.CHAR
                || type == GdrTokenType.BLOCK_COMMENT
                || type == GdrTokenType.PREPROCESSOR;
    }

    private static int countNewlines(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }
}
