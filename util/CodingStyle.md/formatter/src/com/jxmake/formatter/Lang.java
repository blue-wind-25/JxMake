/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter;

/**
 * Precomputes the `"c"`/`"cpp"`/`"java"`/`"kotlin"` language identity of the file being formatted
 * exactly once per {@link Formatter#formatOne}, so rule classes read {@link #isC}/{@link #isCpp}/
 * {@link #isJava}/{@link #isKotlin} instead of each re-comparing the raw {@link #language} string.
 */
public final class Lang {
    public final String language;
    public final boolean isC;
    public final boolean isCpp;
    public final boolean isJava;
    public final boolean isKotlin;

    public Lang(final String language) {
        this.language = language;
        this.isC = "c".equals(language);
        this.isCpp = "cpp".equals(language);
        this.isJava = "java".equals(language);
        this.isKotlin = "kotlin".equals(language);
    }
}
