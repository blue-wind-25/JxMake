/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.CssSpecificRule;

/**
 * CSS formatter (RDD_KEY_190) -- a "SimpleBraced" family member alongside {@link FormatterJson}.
 * `--format-off`/`--format-on` at the whole-file level short-circuits to a raw passthrough; within
 * a formatted file, {@code /*% JXM_CFMT_DIS *}{@code /} .. {@code ENA} block-comment markers freeze
 * a per-region span verbatim (via {@code TokenizerCore.markFrozenSpans}), same convention as the
 * curly family's {@code //%}/{@code /*%} markers.
 */
public final class FormatterCss extends FormatterSimpleBraced {

    public FormatterCss(final Lang lang)
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
        final CssSpecificRule rule = new CssSpecificRule(
            lang,
            config.lineLength(),
            config.indentSize(),
            config.indentStyle(),
            config.isNormalizeCommentStartCase()
        );

        return rule.format(content);
    }

} // class FormatterCss
