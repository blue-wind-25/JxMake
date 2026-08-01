package com.jxmake.formatter.gdr;

import java.util.ArrayList;
import java.util.List;

/**
 * The GDR pre-pass's combined {@code ()}/{@code []} depth counter --
 * the continuation axis, separate from {@link GdrBraceDepthCounter}'s
 * block/scope axis. Same structure/algorithm as
 * {@link GdrBraceDepthCounter}, deliberately not shared code with it (or
 * with any existing pipeline class) -- see that class's javadoc and
 * {@code STATE_CURLY_GDR.md} for why.
 *
 * <p>Parens and brackets are counted together (not as two separate
 * numbers) because {@code STYLE.md} §8 applies the same "one level per
 * unclosed opener, closer dedents to the opening line" rule to both
 * wrapped call/declaration parens and, by the same convention, bracketed
 * initializers -- there is no observed case in this codebase's own style
 * rules where a paren-continuation and a bracket-continuation need
 * independent depth tracking.
 */
public final class GdrParenBracketDepthCounter {

    private GdrParenBracketDepthCounter() {
    }

    public static List<GdrLineParenBracketDepth> compute(List<GdrToken> tokens) {
        List<GdrLineParenBracketDepth> result = new ArrayList<>();
        int depth = 0;
        int line = 0;
        int startOfLineDepth = 0;

        for (GdrToken t : tokens) {
            while (line < t.line) {
                result.add(new GdrLineParenBracketDepth(line, startOfLineDepth, depth));
                line++;
                startOfLineDepth = depth;
            }

            if (t.type == GdrTokenType.PAREN_OPEN || t.type == GdrTokenType.BRACKET_OPEN) {
                depth++;
            } else if (t.type == GdrTokenType.PAREN_CLOSE || t.type == GdrTokenType.BRACKET_CLOSE) {
                depth--;
            }

            int embeddedNewlines = countNewlines(t.text);
            for (int k = 0; k < embeddedNewlines; k++) {
                result.add(new GdrLineParenBracketDepth(line, startOfLineDepth, depth));
                line++;
                startOfLineDepth = depth;
            }
        }
        result.add(new GdrLineParenBracketDepth(line, startOfLineDepth, depth));
        return result;
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
