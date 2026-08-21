package com.jxmake.formatter.gdr;

import java.util.ArrayList;
import java.util.List;

/**
 * The GDR pre-pass's own {@code {}}-nesting depth counter -- see
 * {@code STATE_CURLY_GDR.md}. Deliberately independent of
 * {@code ScopePipelineCurly}'s depth tracking: this pre-pass runs before
 * the existing formatter pipeline and must not share code or state with
 * it.
 *
 * <p>Consumes the token stream from {@link GdrTokenizer} (which has
 * already excluded string/char/comment/raw-string/preprocessor interiors
 * from bracket recognition, so every {@link GdrTokenType#BRACE_OPEN}/
 * {@link GdrTokenType#BRACE_CLOSE} token here is real structural code)
 * and produces one {@link GdrLineBraceDepth} per source line. Scoped only
 * to brace depth -- paren/bracket depth (needed for the
 * continuation-vs-block second axis) is deferred to the reindenter
 * (checklist item 4), not this counter.
 */
public final class GdrBraceDepthCounter {

    private GdrBraceDepthCounter()
    {
    }

    public static List<GdrLineBraceDepth> compute(List<GdrToken> tokens)
    {
        List<GdrLineBraceDepth> result           = new ArrayList<>();
        int                     depth            = 0;
        int                     line             = 0;
        int                     startOfLineDepth = 0;

        for(GdrToken t : tokens) {
            // Defensive: normally every '\n' is either its own NEWLINE
            // token or embedded in a multi-line token's text (both
            // handled below), so t.line should never outrun the local
            // `line` cursor -- but don't silently miscount if it does.
            while(line < t.line) {
                result.add( new GdrLineBraceDepth(line, startOfLineDepth, depth) );
                ++line;
                startOfLineDepth = depth;
            }

                 if(t.type == GdrTokenType.BRACE_OPEN)  depth++;
            else if(t.type == GdrTokenType.BRACE_CLOSE) depth--;

            int embeddedNewlines = GdrTextUtil.countNewlines(t.text);
            for(int k = 0; k < embeddedNewlines; ++k) {
                result.add( new GdrLineBraceDepth(line, startOfLineDepth, depth) );
                ++line;
                startOfLineDepth = depth;
            }
        } // for t
        // Trailing line after the last '\n' (or the sole line, if the
        // source has none) -- not otherwise recorded by the loop above
        result.add( new GdrLineBraceDepth(line, startOfLineDepth, depth) );

        return result;
    }

} // class GdrBraceDepthCounter
