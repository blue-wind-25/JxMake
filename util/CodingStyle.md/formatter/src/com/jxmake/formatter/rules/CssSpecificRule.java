/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;

/**
 * Landing spot for CSS STYLE_DATA_FORMATS.md rule logic (property/value colon alignment,
 * at-rule and native-nesting header/group recursion). Also the formatter an HTML5 `<style>`
 * splice-out dispatches to once both this and {@link XmlSpecificRule} (HTML5's landing spot)
 * are implemented. Neither curly, indent-based, nor tag-based per `Lang.java`'s family
 * predicates, so this rule (and its eventual tokenizer/formatter) is not a `*Curly`/`*Indent`/
 * `*Tags` sibling -- see RDD_KEY_189. Not yet implemented -- see STATE_DATA_FORMATS.md.
 */
public final class CssSpecificRule {

    private final Lang lang;
    private final int lineLengthLimit;

    public CssSpecificRule(final Lang lang) {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public CssSpecificRule(final Lang lang, final int lineLengthLimit) {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public CssSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth) {
        this.lang = lang;
        this.lineLengthLimit = lineLengthLimit;
        throw new UnsupportedOperationException(
                "CssSpecificRule is not yet implemented -- see STATE_DATA_FORMATS.md");
    }
}
