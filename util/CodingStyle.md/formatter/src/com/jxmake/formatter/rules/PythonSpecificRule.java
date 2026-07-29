/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;

/**
 * Landing spot for Python 3-only STYLE_PYTHON3.md rule logic (bracket-complexity detector,
 * assignment alignment, import ordering, decorators, f-strings, signature wrapping, structural
 * pattern matching, single-statement bodies, control-flow blank lines) -- mirrors
 * {@link KotlinSpecificRule}'s role for Kotlin, landing in the indent-based family instead of
 * curly. Not yet implemented -- see STATE_PYTHON3.md.
 */
public final class PythonSpecificRule {

    private final Lang lang;
    private final int  lineLengthLimit;

    public PythonSpecificRule(final Lang lang)
    {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public PythonSpecificRule(final Lang lang, final int lineLengthLimit)
    {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public PythonSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth)
    {
        this.lang            = lang;
        this.lineLengthLimit = lineLengthLimit;
        throw new UnsupportedOperationException(
                "PythonSpecificRule is not yet implemented -- see STATE_PYTHON3.md");
    }

} // class PythonSpecificRule
