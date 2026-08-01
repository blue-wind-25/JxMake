package com.jxmake.formatter.gdr;

/**
 * Combined {@code ()}/{@code []} nesting depth for one source line, as
 * computed by {@link GdrParenBracketDepthCounter}. This is the
 * "continuation" axis referenced by {@code STATE_CURLY_GDR.md}'s
 * background section (wrapped call args, multi-line initializers) --
 * kept as its own counter, separate from {@link GdrLineBraceDepth}'s
 * block/scope axis, so the reindenter can combine the two explicitly
 * rather than the counters conflating them.
 */
public final class GdrLineParenBracketDepth {
    public final int line;
    public final int depthAtStart;
    public final int depthAtEnd;

    public GdrLineParenBracketDepth(int line, int depthAtStart, int depthAtEnd) {
        this.line = line;
        this.depthAtStart = depthAtStart;
        this.depthAtEnd = depthAtEnd;
    }

    @Override
    public String toString() {
        return "line=" + line + " start=" + depthAtStart + " end=" + depthAtEnd;
    }
}
