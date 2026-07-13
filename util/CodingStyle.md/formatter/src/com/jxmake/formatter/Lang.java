/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter;

import java.util.Locale;

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

    /* When updating the supported language here, also update the language/extension list in:
     *    the {@link Lang} constructor above (isC/isCpp/isJava/isKotlin)
     *    the `--lang` validation in `Main.run()`
     *    `ServerMode.FormatHandler.handle()`
     */
    public static final String SUPPORTED_LANGUAGES = "c, cpp, java, kotlin";

    public static boolean isSupported(final String language) {
        return "c".equals(language) || "cpp".equals(language)
                || "java".equals(language) || "kotlin".equals(language);
    }

    public static String infer(final String path) {
        final String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".java")) {
            return "java";
        }
        if (lower.endsWith(".c") || lower.endsWith(".h")) {
            return "c";
        }
        if (lower.endsWith(".cc") || lower.endsWith(".cpp") || lower.endsWith(".cxx")
                || lower.endsWith(".hh") || lower.endsWith(".hpp") || lower.endsWith(".hxx")) {
            return "cpp";
        }
        if (lower.endsWith(".kt") || lower.endsWith(".kts")) {
            return "kotlin";
        }
        return null;
    }
}
