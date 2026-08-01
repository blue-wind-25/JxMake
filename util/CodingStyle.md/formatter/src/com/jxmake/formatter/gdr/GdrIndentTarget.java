package com.jxmake.formatter.gdr;

/**
 * The GDR pre-pass's computed absolute indent target for one source
 * line, as produced by {@link GdrReindenter}. {@code touchable} false
 * means this line must not be reindented at all (interior continuation
 * of a multi-line string/comment/preprocessor token, see
 * {@link GdrLineTouchability}) -- {@code level}/{@code columns} are
 * meaningless in that case.
 */
public final class GdrIndentTarget {
    public final int line;
    public final boolean touchable;
    public final int level;
    public final int columns;

    public GdrIndentTarget(int line, boolean touchable, int level, int columns) {
        this.line = line;
        this.touchable = touchable;
        this.level = level;
        this.columns = columns;
    }

    @Override
    public String toString() {
        return "line=" + line + " touchable=" + touchable + " level=" + level + " columns=" + columns;
    }
}
