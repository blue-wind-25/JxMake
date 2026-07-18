/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.XmlSpecificRule;

public final class FormatterXml extends FormatterCore {

    public FormatterXml(final Lang lang) {
        super(lang);
    }

    @Override
    public String formatOne(final String content, final String filePath, final Config config,
            final boolean formatOff) {
        if (formatOff) {
            return content;
        }
        final XmlSpecificRule rule = new XmlSpecificRule(lang, config.lineLength(), config.indentSize(),
                config.indentStyle(), config.isNormalizeCommentStartCase());
        return rule.format(content);
    }
}
