package com.jxmake.formatter.gdr;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.jxmake.formatter.tokenizer.TokenizerCore;

/**
 * Opt-in exclusion-zone detection for the GDR pre-pass -- see
 * {@code STATE_CURLY_GDR.md}'s "content exclusions" checklist item.
 * Distinct from {@link GdrLineTouchability}: that class covers content
 * the reindenter could never safely touch (interior of a multi-line
 * literal/comment/preprocessor token); this class covers regions a
 * user has explicitly asked to exclude via marker comments, even though
 * the content itself would otherwise be ordinary, reindentable code.
 *
 * <p>Two independent, orthogonal marker families, both line-anchored
 * (the marker comment must be the sole content of its line, matching
 * this codebase's existing frozen-span convention exactly):
 * <ul>
 *   <li>{@code //% JXM_CFMT_DIS} / {@code //% JXM_CFMT_ENA} (and their
 *       {@code /*% ... *}{@code /} block-comment equivalents) -- the
 *       existing whole-pipeline frozen-span markers, same regex shape as
 *       {@code TokenizerCore.FORMAT_DIS_MARKER}/{@code FORMAT_ENA_MARKER}
 *       (reimplemented independently here, not shared code, per this
 *       pre-pass's isolation requirement -- see {@link GdrTokenizer}'s
 *       javadoc for why). A frozen span is excluded from GDR regardless
 *       of the {@code curly-general-scope-reindent}/{@code JXM_CFMT_GDR}
 *       state -- the existing pipeline already guarantees frozen content
 *       is never touched by anything, and GDR must honor that same
 *       guarantee independently since it runs before that pipeline ever
 *       sees the source.</li>
 *   <li>{@code //% JXM_CFMT_GDR 0} / {@code //% JXM_CFMT_GDR 1} (and
 *       block-comment equivalents) -- the new GDR-specific directive,
 *       flat-toggle semantics per {@code RDD_KEY_227}: a single
 *       {@code 1} always re-enables, a redundant {@code 0} is a no-op,
 *       and an unmatched trailing {@code 0} at EOF is not an error (see
 *       that RDD entry).</li>
 * </ul>
 *
 * <p>Per the existing frozen-span convention (mirrored here for the new
 * directive too): the marker comment's own line is always excluded,
 * regardless of which state it's toggling into -- an {@code ENA}/{@code
 * JXM_CFMT_GDR 1} line is itself still excluded even though it
 * re-enables GDR for the lines *after* it.
 */
public final class GdrExclusionZones {

    private static final Pattern DIS     = Pattern.compile(
        "^//%\\s*" + TokenizerCore.JXM_CFMT_DIS + "\\s*$|^/\\*%\\s*" + TokenizerCore.JXM_CFMT_DIS + "\\s*\\*/$"
    );
    private static final Pattern ENA     = Pattern.compile(
        "^//%\\s*" + TokenizerCore.JXM_CFMT_ENA + "\\s*$|^/\\*%\\s*" + TokenizerCore.JXM_CFMT_ENA + "\\s*\\*/$"
    );
    private static final Pattern GDR_OFF = Pattern.compile(
        "^//%\\s*" + TokenizerCore.JXM_CFMT_GDR + "\\s+0\\s*$|^/\\*%\\s*" + TokenizerCore.JXM_CFMT_GDR + "\\s+0\\s*\\*/$"
    );
    private static final Pattern GDR_ON  = Pattern.compile(
        "^//%\\s*" + TokenizerCore.JXM_CFMT_GDR + "\\s+1\\s*$|^/\\*%\\s*" + TokenizerCore.JXM_CFMT_GDR + "\\s+1\\s*\\*/$"
    );

    private GdrExclusionZones()
    {
    }

    /** Per-line: true if the line falls inside an exclusion zone (or is a marker line itself) */
    public static List<Boolean> computeExcludedByLine(List<GdrToken> tokens)
    {
        List<Boolean> result        = new ArrayList<>();
        boolean       frozen        = false;
        boolean       gdrOff        = false;
        boolean       lineHasMarker = false;
        int           line          = 0;

        for(GdrToken t : tokens) {
            while(line < t.line) {
                result.add(frozen || gdrOff || lineHasMarker);
                ++line;
                lineHasMarker = false;
            }

            if(t.type == GdrTokenType.LINE_COMMENT || t.type == GdrTokenType.BLOCK_COMMENT) {
                String trimmed = t.text.trim();
                if( DIS.matcher(trimmed).matches() ) {
                    frozen        = true;
                    lineHasMarker = true;
                }
                else if( ENA.matcher(trimmed).matches() ) {
                    frozen        = false;
                    lineHasMarker = true;
                }
                else if( GDR_OFF.matcher(trimmed).matches() ) {
                    gdrOff        = true;
                    lineHasMarker = true;
                }
                else if( GDR_ON.matcher(trimmed).matches() ) {
                    gdrOff        = false;
                    lineHasMarker = true;
                }
            } // if

            int embeddedNewlines = countNewlines(t.text);
            for(int k = 0; k < embeddedNewlines; ++k) {
                result.add(frozen || gdrOff || lineHasMarker);
                ++line;
                lineHasMarker = false;
            }
        } // for t
        result.add(frozen || gdrOff || lineHasMarker);

        return result;
    }

    private static int countNewlines(String text)
    {
        int count = 0;
        for( int i = 0; i < text.length(); ++i ) {
            if( text.charAt(i) == '\n' ) count++;
        }

        return count;
    }

} // class GdrExclusionZones
