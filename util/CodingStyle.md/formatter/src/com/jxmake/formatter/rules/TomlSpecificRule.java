/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;

/**
 * Landing spot for TOML STYLE_DATA_FORMATS.md rule logic. Neither curly, indent-based, nor
 * tag-based per `Lang.java`'s family predicates, so this rule (and its eventual tokenizer/
 * formatter) is not a `*Curly`/`*Indent`/`*Tags` sibling -- see RDD_KEY_189. Not yet
 * implemented -- see STATE_DATA_FORMATS.md.
 */
public final class TomlSpecificRule {

    private final Lang lang;
    private final int lineLengthLimit;

    public TomlSpecificRule(final Lang lang) {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public TomlSpecificRule(final Lang lang, final int lineLengthLimit) {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public TomlSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth) {
        this.lang = lang;
        this.lineLengthLimit = lineLengthLimit;
        throw new UnsupportedOperationException(
                "TomlSpecificRule is not yet implemented -- see STATE_DATA_FORMATS.md");
    }
}
