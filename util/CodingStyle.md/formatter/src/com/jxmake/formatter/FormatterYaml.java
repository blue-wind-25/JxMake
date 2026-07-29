/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.YamlSpecificRule;

/**
 * YAML formatter (STYLE_DATA_FORMATS.md §5). Not a "SimpleBraced" family member (RDD_KEY_189/191)
 * -- extends {@link FormatterCore} directly. Per §5.1, {@code indent-style} is ignored (YAML output
 * is always space-indented); only {@code config.indentSize()} is passed through.
 * {@code --format-off}/{@code --format-on} at the whole-file level is honored directly; per-region
 * frozen spans (`#% JXM_CFMT_DIS`/`ENA`) are handled by {@link YamlSpecificRule} itself.
 */
public final class FormatterYaml extends FormatterCore {

    public FormatterYaml(final Lang lang)
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
        final YamlSpecificRule rule = new YamlSpecificRule(
            lang, config.lineLength(), config.indentSize(), config.isNormalizeCommentStartCase()
        );

        return rule.format(content);
    }

} // class FormatterYaml
