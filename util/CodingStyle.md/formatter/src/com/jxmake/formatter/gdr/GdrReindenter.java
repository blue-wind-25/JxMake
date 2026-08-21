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
 *
 * <p><b>{@code postMode} (RDD_KEY_331):</b> when the GDR pre-pass logic is reused as a genuine
 * POST-pass ({@code curly-general-scope-reindent-postpass}, see {@link GdrPipelineGate}) it runs
 * directly on the already-fully-formatted pipeline output rather than on raw source ahead of the
 * pipeline. The paren/bracket continuation axis ({@code pbLevel}) uses a naive "one indent level
 * per open paren/bracket" model, which does not always match the pipeline's own, more nuanced
 * {@code STYLE.md} §8 continuation-indent convention for a wrapped call/condition -- re-deriving a
 * wrapped-call continuation line's indentation from {@code pbLevel} on top of output the pipeline
 * already committed to can therefore land at a different (wrong) depth than what the pipeline
 * itself decided. Passing {@code postMode = true} makes every line with a nonzero {@code pbLevel}
 * (any line that is itself inside an open paren/bracket wrap, or that closes one) untouchable, so
 * the postpass never re-derives such a line's indentation -- it only re-targets plain
 * {@code pbLevel == 0} lines by brace depth alone, which is exactly the kind of line the postpass's
 * original motivating fix (a plain sibling declaration left at the wrong depth by an unrelated
 * pre-existing bug) needed. Every other caller (the pre-pass proper, including every multipass
 * cycle) always passes {@code postMode = false} and is unaffected -- see the two-arg overload
 * below.
 *
 * <p><b>RDD_KEY_332 refinement:</b> the "nonzero" check that gates {@code postMode}'s untouchable
 * rule is based on each line's raw paren/bracket {@code depthAtStart}, not the leader-adjusted
 * {@code pbLevel} used for actually retargeting a non-exempt line -- a mixed closer line (e.g.
 * {@code ) }} that closes a call's paren and a lambda's brace on the same line) has {@code
 * depthAtStart != 0} (the wrap was still open when the line began) even though its leader-adjusted
 * {@code pbLevel} collapses to {@code 0} once the closer is accounted for. Using {@code pbLevel} for
 * the gate (as originally landed by RDD_KEY_331) let exactly this closer shape slip through and get
 * re-targeted from brace depth alone -- found via the 188-file real-Kotlin-corpus re-validation
 * RDD_KEY_331 itself could not run. See {@code STATE_CURLY_GDR.md}'s RDD_KEY_332 entry.
 */
public final class GdrReindenter {

    private GdrReindenter()
    {
    }

    /**
     * Equivalent to {@code compute(source, indentSize, false)} -- the ordinary pre-pass behavior
     * (full {@code braceLevel + pbLevel}, no postMode restriction). Kept as the default entry point
     * so every existing caller (smoke tests, direct callers) is unaffected by the new
     * {@code postMode} parameter.
     */
    public static List<GdrIndentTarget> compute(String source, int indentSize)
    {
        return compute(source, indentSize, false);
    }

    /**
     * Equivalent to {@code compute(source, indentSize, postMode, false)} -- {@code
     * kotlinNestedBlockComments} off. Kept so every caller besides {@link
     * com.jxmake.formatter.gdr.GdrPipelineGate} is unaffected by the new parameter (RDD_KEY_333).
     */
    public static List<GdrIndentTarget> compute(String source, int indentSize, boolean postMode)
    {
        return compute(source, indentSize, postMode, false);
    }

    public static List<GdrIndentTarget> compute(
        String source, int indentSize, boolean postMode, boolean kotlinNestedBlockComments
    )
    {
        List<GdrToken>                 tokens      = GdrTokenizer.tokenize(source, kotlinNestedBlockComments);
        List<GdrLineBraceDepth>        braceDepths = GdrBraceDepthCounter.compute(tokens);
        List<GdrLineParenBracketDepth> pbDepths    = GdrParenBracketDepthCounter.compute(tokens);
        int                            totalLines  = braceDepths.size();

        List<Boolean>  touchable    = GdrLineTouchability.computeTouchableByLine(tokens, totalLines);
        List<Boolean>  excluded     = GdrExclusionZones.computeExcludedByLine(tokens);
        GdrTokenType[] leadingType  = computeLeadingTokenTypes(tokens, totalLines);
        String[]       trailingSig  = postMode ? computeTrailingSignificantText(tokens, totalLines) : null;
        String[]       precedingSig = postMode ? computePrecedingSignificantText(trailingSig, totalLines) : null;

        List<GdrIndentTarget> result   = new ArrayList<>(totalLines);
        boolean               zoneOpen = false;
        int                   zoneFloor = 0;
        for(int line = 0; line < totalLines; ++line) {
            boolean lineExcluded = line < excluded.size() && excluded.get(line);
            if( !touchable.get(line) || lineExcluded ) {
                result.add( new GdrIndentTarget(line, false, 0, 0) );
                continue;
            }
            GdrLineBraceDepth        brace  = braceDepths.get(line);
            GdrLineParenBracketDepth pb     = pbDepths.get(line);
            GdrTokenType             leader = leadingType[line];

            // postMode (RDD_KEY_332, fourth refinement -- "zone" propagation): once a line is
            // exempted (below) because it directly continues a wrapped expression body, every
            // line NESTED inside whatever it opens is, structurally, still part of that same
            // continuation the pipeline already placed -- including that nested content's own
            // closing brace, which GDR's plain brace-count model would otherwise re-derive
            // relative to the WRONG reference point (the depth before the continuation's own
            // brace opened, not the depth the continuation line itself visually sits at -- see
            // STATE_CURLY_GDR.md's RDD_KEY_332 entry for the worked example). A zone starts the
            // moment a line is exempted for any reason at brace depth `zoneFloor` and stays open
            // for every subsequent line whose OWN depthAtStart is deeper than that floor; it
            // closes itself as soon as depth returns to (or below) the floor, so a sibling
            // statement immediately after the zone is unaffected.
            if(postMode && zoneOpen && brace.depthAtStart <= zoneFloor) zoneOpen = false;
            if(postMode && zoneOpen) {
                result.add( new GdrIndentTarget(line, false, 0, 0) );
                continue;
            }

            int braceLevel = (leader == GdrTokenType.BRACE_CLOSE) ? brace.depthAtEnd : brace.depthAtStart;
            int pbLevel    = (leader == GdrTokenType.PAREN_CLOSE || leader == GdrTokenType.BRACKET_CLOSE) ? pb.depthAtEnd : pb.depthAtStart;

            // Clamp: a genuinely well-formed (balanced) file never has either
            // axis go negative -- depth only decrements below zero when the
            // source has more closers than openers up to this point, i.e.
            // malformed/unbalanced input. Clamp per-axis rather than after
            // summing so one axis's legitimate positive level isn't
            // cancelled out by the other axis's bogus negative one. See
            // RDD_KEY_242 / GdrRewriter.spaces's NegativeArraySizeException.
            if(braceLevel < 0) braceLevel = 0;
            if(pbLevel    < 0) pbLevel    = 0;

            // postMode (RDD_KEY_331/RDD_KEY_332): leave any line inside (or closing) an open
            // paren/bracket wrap completely untouched -- see the class Javadoc above for why
            // pbLevel's naive model doesn't match the pipeline's own STYLE.md §8
            // continuation-indent convention once this runs as a postpass on already-finished
            // pipeline output. Deliberately checks the RAW pb.depthAtStart here, not the
            // leader-adjusted pbLevel above: pbLevel swaps to depthAtEnd for a line whose
            // leading token is itself a paren/bracket closer (the "closer dedents to match its
            // opening line" rule), which for a closer that fully closes the wrap on the same
            // line reports depthAtEnd == 0 -- wrongly signaling "no wrap" for exactly the
            // wrap-continuation closer line postMode most needs to leave alone (RDD_KEY_332:
            // found via the 188-file JetBrains/kotlin corpus re-validation RDD_KEY_331 could not
            // run; a mixed closer like `) }` -- paren-close then brace-close on one line -- was
            // still being re-targeted from brace depth alone, over-indenting it). pb.depthAtStart
            // reflects whether the wrap was already open when this line began, which is true for
            // every continuation line AND every closer line of a multi-line wrap regardless of
            // where the wrap's depth ends up by the line's own end.
            if(postMode && pb.depthAtStart != 0) {
                result.add( new GdrIndentTarget(line, false, 0, 0) );
                continue;
            }

            // postMode (RDD_KEY_332, second refinement): leave a "compound" brace-closer line
            // untouched too -- one whose leading `}` is immediately followed by more significant
            // content that reopens a brace on the SAME line (Kotlin scope-function chains like
            // `}.apply {`/`}.also {`/`}.let {`). depthAtEnd for a plain, bare closer equals
            // depthAtStart - 1 exactly; anything else (a same-line reopen nets depthAtEnd back up
            // toward depthAtStart, a double-close nets it down further) means this line isn't the
            // simple "sibling declaration/closing-brace-only mis-indent" shape the postpass's
            // brace-depth-alone retarget is meant for -- re-deriving it collides with the same
            // wrap-continuation problem pb.depthAtStart already guards above, just on the brace
            // axis instead of the paren/bracket axis. A bare `}` (optionally with a trailing
            // comment) is unaffected -- depthAtEnd == depthAtStart - 1 there, so it's still
            // retargeted, preserving the postpass's original motivating fix.
            if(postMode && leader == GdrTokenType.BRACE_CLOSE && brace.depthAtEnd != brace.depthAtStart - 1) {
                result.add( new GdrIndentTarget(line, false, 0, 0) );
                continue;
            }

            // postMode (RDD_KEY_332, third refinement): leave a line untouched if the nearest
            // preceding non-blank line ends (ignoring a trailing line/block comment) with `=` --
            // an expression-body arrow or plain assignment continuation per STYLE.md §8's
            // continuation-indent convention. GDR's brace/paren-bracket depth model has no
            // visibility into this axis at all (both levels are legitimately 0 for, e.g., a
            // top-level expression-bodied function's wrapped multi-line signature): the pipeline
            // already indents such a body one extra level beyond raw brace depth, which
            // brace-depth-alone retargeting would wrongly collapse back down. Matches trailing
            // `==`/`!=`/`>=`/`<=`/`+=` etc. too (all end in the same character) -- deliberately
            // over-inclusive/conservative, since exempting a line only means leaving it exactly as
            // the pipeline already placed it, never mis-deriving a new value for it.
            if(postMode && endsWithEquals( precedingSig[line] )) {
                if(!zoneOpen) {
                    zoneOpen  = true;
                    zoneFloor = brace.depthAtStart;
                }
                result.add( new GdrIndentTarget(line, false, 0, 0) );
                continue;
            }

            int level = braceLevel + pbLevel;
            result.add( new GdrIndentTarget(line, true, level, level * indentSize) );
        } // for

        return result;
    }

    /**
     * First non-whitespace token type on each line, or {@code null} for a
     * line with no non-whitespace content (blank line, or a line entirely
     * consumed by an untouchable multi-line-token continuation)
     */
    private static GdrTokenType[] computeLeadingTokenTypes(List<GdrToken> tokens, int totalLines)
    {
        GdrTokenType[] leading = new GdrTokenType[totalLines];
        for(GdrToken t : tokens) {
            if( t.line < 0 || t.line >= totalLines || leading[t.line] != null ) continue;
            if(t.type == GdrTokenType.NEWLINE) continue;
            if( t.type == GdrTokenType.TEXT && isAllWhitespace(t.text) ) continue;
            leading[t.line] = t.type;
        }

        return leading;
    }

    /**
     * postMode helper (RDD_KEY_332): last significant (non-comment,
     * non-whitespace-only) token's raw text on each line, or {@code null}
     * for a line with no such content. Mirrors {@link
     * #computeLeadingTokenTypes} but keeps overwriting as it walks forward,
     * so the value left standing after the scan is the LAST token on that
     * line rather than the first.
     */
    private static String[] computeTrailingSignificantText(List<GdrToken> tokens, int totalLines)
    {
        String[] trailing = new String[totalLines];
        for(GdrToken t : tokens) {
            if( t.line < 0 || t.line >= totalLines ) continue;
            if( t.type == GdrTokenType.NEWLINE || t.type == GdrTokenType.LINE_COMMENT || t.type == GdrTokenType.BLOCK_COMMENT ) continue;
            if( t.type == GdrTokenType.TEXT && isAllWhitespace(t.text) ) continue;
            trailing[t.line] = t.text;
        }

        return trailing;
    }

    /**
     * postMode helper (RDD_KEY_332): precomputes, for every line index, the
     * nearest preceding line's trailing significant text (or {@code null}
     * if none exists, e.g. index 0). One forward pass over {@code
     * trailingSig} replaces what used to be an O(n) backward rescan per
     * call site -- {@code precedingSig[line]} is exactly what a
     * per-call backward scan from {@code line - 1} would have found.
     */
    private static String[] computePrecedingSignificantText(String[] trailingSig, int totalLines)
    {
        String[] preceding = new String[totalLines];
        String   lastSig   = null;
        for( int i = 0; i < totalLines; ++i ) {
            preceding[i] = lastSig;
            if(trailingSig[i] != null) lastSig = trailingSig[i];
        }

        return preceding;
    }

    private static boolean endsWithEquals(String trailingText)
    {
        if(trailingText == null) return false;
        int end = trailingText.length();
        while( end > 0 && Character.isWhitespace( trailingText.charAt(end - 1) ) ) --end;

        return end > 0 && trailingText.charAt(end - 1) == '=';
    }

    private static boolean isAllWhitespace(String text)
    {
        for( int i = 0; i < text.length(); ++i ) {
            char c = text.charAt(i);
            if(c != ' ' && c != '\t' && c != '\r') return false;
        }

        return true;
    }

} // class GdrReindenter
