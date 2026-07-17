/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;

/**
 * Landing spot for JavaScript/TypeScript-only STYLE_JS_TS.md sections not reusable from the
 * shared curly-family rule classes -- mirrors {@link KotlinSpecificRule}'s role for Kotlin.
 * Shared between JS and TS (gate internally on {@code lang.isTs} for TS-only additions such as
 * type annotations/interfaces/enums, per RDD_KEY_187), rather than splitting into separate
 * per-language classes. Not yet implemented -- see STATE_JS_TS.md.
 */
public final class JsTsSpecificRule {

    private final Lang lang;
    private final int lineLengthLimit;

    public JsTsSpecificRule(final Lang lang) {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public JsTsSpecificRule(final Lang lang, final int lineLengthLimit) {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public JsTsSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth) {
        this.lang = lang;
        this.lineLengthLimit = lineLengthLimit;
        throw new UnsupportedOperationException(
                "JsTsSpecificRule is not yet implemented -- see STATE_JS_TS.md");
    }
}
