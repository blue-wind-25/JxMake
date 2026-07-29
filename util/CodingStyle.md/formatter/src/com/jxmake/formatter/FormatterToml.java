/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.TomlSpecificRule;

/**
 * TOML formatter (STYLE_DATA_FORMATS.md §6). Not a "SimpleBraced" family member (RDD_KEY_189/191)
 * -- extends {@link FormatterCore} directly. {@code --format-off}/{@code --format-on} at the
 * whole-file level is honored directly; per-region frozen spans (`#% JXM_CFMT_DIS`/`ENA`) are
 * handled by {@link TomlSpecificRule} itself.
 */
public final class FormatterToml extends FormatterCore {

    public FormatterToml(final Lang lang)
    {
        super(lang);
    }

    @Override
    public String formatOne(
        final String  content,
        final String  filePath,
        final Config  config,
        final boolean formatOff
    )
    {
        if(formatOff) return content;
        final TomlSpecificRule rule = new TomlSpecificRule(
            lang, config.lineLength(), config.indentSize(), config.isNormalizeCommentStartCase()
        );

        return rule.format(content);
    }

} // class FormatterToml
