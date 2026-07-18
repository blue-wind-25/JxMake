/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

/**
 * Thin dispatcher/contract shared by every language-family formatter sibling
 * ({@link FormatterCurly} for C/C++/Java/Kotlin, and future {@code FormatterIndent}/
 * {@code FormatterTags} for Python3/XML-HTML5). {@link #forLanguage} is the single place that
 * picks the right sibling by {@link Lang} family, so callers (`Main.java`/`ServerMode.java`) never
 * need their own if/else on language.
 */
public abstract class FormatterCore {
    protected final Lang lang;

    protected FormatterCore(final Lang lang) {
        this.lang = lang;
    }

    public abstract String formatOne(String content, String filePath, Config config, boolean formatOff);

    public static FormatterCore forLanguage(final String language) {
        final Lang lang = new Lang(language);
        if (lang.isJson || lang.isJson5) {
            return new FormatterJson(lang);
        }
        if (lang.isCss) {
            return new FormatterCss(lang);
        }
        if (lang.isCurly) {
            return new FormatterCurly(lang);
        }
        if (lang.isIndentBased) {
            return new FormatterIndent(lang);
        }
        if (lang.isTagBased) {
            return new FormatterTags(lang);
        }
        throw new UnsupportedOperationException("'" + language + "' has no formatter dispatch yet");
    }
}
