/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.JxMakeSpecificRule;

/**
 * STYLE_JXMAKE.md (JxMakeFile scripting language) dispatch. Not part of any existing family (same
 * reasoning as {@link FormatterEini}/{@link FormatterMakefile}/{@link FormatterBash}/
 * {@link FormatterPowerShell} -- neither brace-delimited, indentation-significant, nor tag-nested
 * in the sense those families are), so this extends {@link FormatterCore} directly.
 */
public final class FormatterJxMake extends FormatterCore {

    public FormatterJxMake(final Lang lang)
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
        final JxMakeSpecificRule rule = new JxMakeSpecificRule(
            config.indentSize(),
            config.isNormalizeCommentStartCase(),
            config.isNormalizeCommentEndPeriod()
        );
        rule.setNormalizeCommentMultiSentenceCase( config.isNormalizeCommentMultiSentenceCase() );

        return rule.format(content);
    }

} // class FormatterJxMake
