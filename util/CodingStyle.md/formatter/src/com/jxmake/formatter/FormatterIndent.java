/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

/**
 * Skeleton landing spot for the indentation-block language family (Python3) -- see
 * `STATE_PYTHON3.md`. Not yet implemented.
 */
public final class FormatterIndent extends FormatterCore {

    public FormatterIndent(final Lang lang) {
        super(lang);
    }

    @Override
    public String formatOne(final String content, final String filePath, final Config config,
            final boolean formatOff) {
        throw new UnsupportedOperationException(
                "'" + lang.language + "' formatting is not yet implemented (scaffold only -- see "
                        + "STATE_PYTHON3.md)");
    }
}
