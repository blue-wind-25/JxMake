package com.jxmake.formatter.gdr;

import java.util.List;

/**
 * Applies {@link GdrReindenter}'s computed per-line indent targets to actually rewrite source
 * text -- see {@code STATE_CURLY_GDR.md}'s checklist item 6. {@link GdrReindenter} itself only
 * computes targets; this class is the one place that turns them into a new source string.
 *
 * <p>For each touchable line, the existing leading whitespace run is replaced with
 * {@code target.columns} spaces; everything from the first non-whitespace character onward is
 * left byte-for-byte unchanged. Untouchable lines (per {@link GdrIndentTarget#touchable}) are
 * copied through verbatim, including their original leading whitespace. Line endings are not
 * touched here -- this class splits/rejoins on {@code \n} only and preserves any trailing
 * {@code \r} as part of that line's unchanged tail content.
 */
public final class GdrRewriter {

    private GdrRewriter()
    {
    }

    public static String rewrite(
        String  source,
        int     indentSize,
        boolean postMode,
        boolean kotlinNestedBlockComments
    )
    {
        List<GdrIndentTarget> targets = GdrReindenter.compute(
            source, indentSize, postMode, kotlinNestedBlockComments
        );
        String[]              lines   = source.split("\n", -1);

        StringBuilder result = new StringBuilder( source.length() );
        for(int i = 0; i < lines.length; ++i) {
            String          line   = lines[i];
            GdrIndentTarget target = ( i < targets.size() ) ? targets.get(i) : null;

            if(target != null && target.touchable) {
                int leadingEnd = leadingWhitespaceEnd(line);
                if( leadingEnd < line.length() ) result.append(
                    spaces(target.columns)
                ).append(
                    line.substring(leadingEnd)
                );
                else result.append(line);
            } // if
            else {
                result.append(line);
            }

            if(i < lines.length - 1) result.append('\n');
        } // for

        return result.toString();
    }

    private static int leadingWhitespaceEnd(String line)
    {
        int i = 0;
        while( i < line.length() && ( line.charAt(i) == ' ' || line.charAt(i) == '\t' ) ) i++;

        return i;
    }

    private static String spaces(int count)
    {
        // Defensive: GdrReindenter.compute already clamps each depth axis to
        // >= 0 (RDD_KEY_242), so `count` should never be negative in
        // practice -- this is a last-resort backstop against
        // NegativeArraySizeException, not the primary fix
        if(count < 0) count = 0;
        StringBuilder sb = new StringBuilder(count);
        for(int i = 0; i < count; ++i) sb.append(' ');

        return sb.toString();
    }

} // class GdrRewriter
