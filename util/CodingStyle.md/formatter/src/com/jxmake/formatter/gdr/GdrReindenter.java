package com.jxmake.formatter.gdr;

import java.util.ArrayList;
import java.util.List;

/**
 * The GDR pre-pass's own reindenter -- see {@code STATE_CURLY_GDR.md}'s
 * checklist item 4. Combines {@link GdrBraceDepthCounter}'s block/scope
 * axis and {@link GdrParenBracketDepthCounter}'s continuation axis into
 * one absolute per-line indent target, honoring
 * {@link GdrLineTouchability}'s untouchable lines. This class only
 * *computes* targets -- it does not rewrite source text; wiring the
 * result into an actual rewrite is a later checklist item.
 *
 * <p>Per-axis level for a line is normally that axis's {@code
 * depthAtStart} (the depth in effect before the line's own content). The
 * one exception, matching {@code STYLE.md} §8's "the closing `)` goes on
 * its own line, indented to match ... the call's indentation level" (and
 * the same convention this codebase applies to a block-closing `}`): a
 * line whose first non-whitespace token is a closing bracket uses that
 * bracket's own axis {@code depthAtEnd} instead of {@code depthAtStart},
 * so the closer dedents to match its opening line rather than the body
 * it closes. Only the axis matching the actual leading closer is
 * affected -- e.g. a line leading with {@code )} adjusts only the
 * paren/bracket axis; the brace axis for that same line still uses its
 * ordinary {@code depthAtStart}.
 *
 * <p>A line's final {@code touchable} flag is false if EITHER
 * {@link GdrLineTouchability} (content that can never safely be touched)
 * OR {@link GdrExclusionZones} (opt-in frozen/{@code JXM_CFMT_GDR}
 * exclusion zones) says so.
 */
public final class GdrReindenter {

    private GdrReindenter() {
    }

    public static List<GdrIndentTarget> compute(String source, int indentSize) {
        List<GdrToken> tokens = GdrTokenizer.tokenize(source);
        List<GdrLineBraceDepth> braceDepths = GdrBraceDepthCounter.compute(tokens);
        List<GdrLineParenBracketDepth> pbDepths = GdrParenBracketDepthCounter.compute(tokens);
        int totalLines = braceDepths.size();

        List<Boolean> touchable = GdrLineTouchability.computeTouchableByLine(tokens, totalLines);
        List<Boolean> excluded = GdrExclusionZones.computeExcludedByLine(tokens);
        GdrTokenType[] leadingType = computeLeadingTokenTypes(tokens, totalLines);

        List<GdrIndentTarget> result = new ArrayList<>(totalLines);
        for (int line = 0; line < totalLines; line++) {
            boolean lineExcluded = line < excluded.size() && excluded.get(line);
            if (!touchable.get(line) || lineExcluded) {
                result.add(new GdrIndentTarget(line, false, 0, 0));
                continue;
            }
            GdrLineBraceDepth brace = braceDepths.get(line);
            GdrLineParenBracketDepth pb = pbDepths.get(line);
            GdrTokenType leader = leadingType[line];

            int braceLevel = (leader == GdrTokenType.BRACE_CLOSE) ? brace.depthAtEnd : brace.depthAtStart;
            int pbLevel = (leader == GdrTokenType.PAREN_CLOSE || leader == GdrTokenType.BRACKET_CLOSE)
                    ? pb.depthAtEnd : pb.depthAtStart;

            int level = braceLevel + pbLevel;
            result.add(new GdrIndentTarget(line, true, level, level * indentSize));
        }
        return result;
    }

    /**
     * First non-whitespace token type on each line, or {@code null} for a
     * line with no non-whitespace content (blank line, or a line entirely
     * consumed by an untouchable multi-line-token continuation).
     */
    private static GdrTokenType[] computeLeadingTokenTypes(List<GdrToken> tokens, int totalLines) {
        GdrTokenType[] leading = new GdrTokenType[totalLines];
        for (GdrToken t : tokens) {
            if (t.line < 0 || t.line >= totalLines || leading[t.line] != null) {
                continue;
            }
            if (t.type == GdrTokenType.NEWLINE) {
                continue;
            }
            if (t.type == GdrTokenType.TEXT && isAllWhitespace(t.text)) {
                continue;
            }
            leading[t.line] = t.type;
        }
        return leading;
    }

    private static boolean isAllWhitespace(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != ' ' && c != '\t' && c != '\r') {
                return false;
            }
        }
        return true;
    }
}
