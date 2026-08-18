/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.MakefileSpecificRule;

/**
 * STYLE_TOOLING.md §1 (Makefile) dispatch. Not part of any existing family (curly/indent/tags/
 * simpleBraced all don't fit -- Makefile is neither brace-delimited nor indentation-significant in
 * the sense those families are, and recipe lines are whitespace-sensitive in a way none of them
 * are), so this extends {@link FormatterCore} directly, the same pattern as {@link FormatterToml}.
 */
public final class FormatterMakefile extends FormatterCore {

    public FormatterMakefile(final Lang lang)
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
        final MakefileSpecificRule rule = new MakefileSpecificRule(
            config.indentSize(),
            config.isNormalizeCommentStartCase(),
            config.isNormalizeCommentEndPeriod()
        );
        rule.setNormalizeCommentMultiSentenceCase( config.isNormalizeCommentMultiSentenceCase() );

        return rule.format(content);
    }

} // class FormatterMakefile
