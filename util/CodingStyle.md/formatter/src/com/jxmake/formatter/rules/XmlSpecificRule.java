/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;

/**
 * Landing spot for XML/HTML5 STYLE_DATA_FORMATS.md rule logic -- shared between the two (gate
 * internally on {@code lang.isHtml5} for HTML5-only additions such as void-element handling
 * and the `<script>`/`<style>` embedded-content dispatcher), per RDD_KEY_188, mirroring how
 * {@link KotlinSpecificRule}/{@link JsTsSpecificRule} share a curly-family base with their
 * siblings rather than each getting a wholly separate class. Tag-based per `Lang.java`'s family
 * predicates ({@code lang.isTagBased}) -- its eventual tokenizer/formatter extend
 * `TokenizerTags`/`FormatterTags`. Not yet implemented -- see STATE_DATA_FORMATS.md.
 */
public final class XmlSpecificRule {

    private final Lang lang;
    private final int lineLengthLimit;

    public XmlSpecificRule(final Lang lang) {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public XmlSpecificRule(final Lang lang, final int lineLengthLimit) {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public XmlSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth) {
        this.lang = lang;
        this.lineLengthLimit = lineLengthLimit;
        throw new UnsupportedOperationException(
                "XmlSpecificRule is not yet implemented -- see STATE_DATA_FORMATS.md");
    }
}
