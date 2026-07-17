/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;

/**
 * Skeleton landing spot for the indent-based language family (Python3) -- see
 * `STATE_PYTHON3.md`. Not yet implemented. Optional reuse only, not required --
 * Python3's `@property`/`@x.setter` pair handling may end up entirely bespoke instead.
 */
public final class GetterSetterRuleIndent extends GetterSetterRuleCore {

    public GetterSetterRuleIndent(final Lang lang, final int indentWidth, final int lineLengthLimit) {
        super(lang, indentWidth, lineLengthLimit);
    }
}
