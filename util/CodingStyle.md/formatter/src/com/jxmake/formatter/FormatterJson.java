/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.JsonSpecificRule;

/**
 * JSON/JSON5 formatter (RDD_KEY_190) -- a "SimpleBraced" family member alongside the future CSS
 * formatter. `--format-off`/`--format-on` at the whole-file level is honored directly (JSON/JSON5
 * have no per-region opaque-passthrough mechanism the way `//%`/`/*%` frozen spans work for the
 * curly family): a fully format-off file is returned unchanged.
 */
public final class FormatterJson extends FormatterSimpleBraced {

    public FormatterJson(final Lang lang) {
        super(lang);
    }

    @Override
    public String formatOne(final String content, final String filePath, final Config config,
            final boolean formatOff) {
        if (formatOff) {
            return content;
        }
        final JsonSpecificRule rule = new JsonSpecificRule(lang, config.lineLength(),
                config.indentSize(), config.indentStyle());
        return rule.format(content);
    }
}
