package com.jxmake.formatter.gdr;

/**
 * Brace-nesting depth for one source line, as computed by
 * {@link GdrBraceDepthCounter}. {@code depthAtStart} is the depth in
 * effect before the line's first token (what a naive per-line reindent
 * target would use); {@code depthAtEnd} additionally reflects any
 * {@code {}} tokens that appear ON this line itself -- e.g. a line
 * consisting solely of a closing {@code }} has {@code depthAtStart} equal
 * to the body depth it closes and {@code depthAtEnd} one lower. Choosing
 * between the two (and handling a closing brace that leads the line vs.
 * one that trails other content) is the reindenter's job, not this
 * counter's -- see {@code STATE_CURLY_GDR.md}'s checklist item 4.
 */
public final class GdrLineBraceDepth {

    public final int line;
    public final int depthAtStart;
    public final int depthAtEnd;

    public GdrLineBraceDepth(int line, int depthAtStart, int depthAtEnd)
    {
        this.line         = line;
        this.depthAtStart = depthAtStart;
        this.depthAtEnd   = depthAtEnd;
    }

    @Override
    public String toString()
    {
        return "line=" + line + " start=" + depthAtStart + " end=" + depthAtEnd;
    }

} // class GdrLineBraceDepth
