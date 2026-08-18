/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.EiniSpecificRule;

/**
 * STYLE_TOOLING.md §4 (E-INI / Extended INI) dispatch. Not part of any existing family (same
 * reasoning as {@link FormatterMakefile}/{@link FormatterBash}/{@link FormatterPowerShell} --
 * neither brace-delimited, indentation-significant, nor tag-nested in the sense those families
 * are), so this extends {@link FormatterCore} directly.
 */
public final class FormatterEini extends FormatterCore {

    public FormatterEini(final Lang lang)
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
        final EiniSpecificRule rule = new EiniSpecificRule(
            config.indentSize(),
            config.isNormalizeCommentStartCase(),
            config.isNormalizeCommentEndPeriod()
        );
        rule.setNormalizeCommentMultiSentenceCase( config.isNormalizeCommentMultiSentenceCase() );

        return rule.format(content);
    }

} // class FormatterEini
