/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.BashSpecificRule;

/**
 * STYLE_TOOLING.md §2 (Bash) dispatch. Standalone (not part of any existing family), same pattern
 * as {@link FormatterMakefile}/{@link FormatterToml}.
 */
public final class FormatterBash extends FormatterCore {

    public FormatterBash(final Lang lang)
    {
        super(lang);
    }

    @Override
    public String formatOne(
        final String content, final String filePath, final Config config, final boolean formatOff
    )
    {
        if(formatOff) return content;
        final BashSpecificRule rule = new BashSpecificRule( config.indentSize() );

        return rule.format(content);
    }

} // class FormatterBash
