/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.PowerShellSpecificRule;

/**
 * STYLE_TOOLING.md §3 (PowerShell) dispatch. Standalone (not part of any existing family), same
 * pattern as {@link FormatterBash}/{@link FormatterMakefile}. Delegates all six §3.x rules to
 * {@link PowerShellSpecificRule}.
 */
public final class FormatterPowerShell extends FormatterCore {

    public FormatterPowerShell(final Lang lang)
    {
        super(lang);
    }

    @Override
    public String formatOne(
        final String content, final String filePath, final Config config, final boolean formatOff
    )
    {
        if(formatOff) return content;
        final PowerShellSpecificRule rule = new PowerShellSpecificRule(
            config.indentSize(), config.isNormalizeCommentStartCase(), config.isNormalizeCommentEndPeriod()
        );

        return rule.format(content);
    }

} // class FormatterPowerShell
