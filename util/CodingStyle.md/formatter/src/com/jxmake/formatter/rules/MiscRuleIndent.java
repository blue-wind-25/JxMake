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
 * `STATE_PYTHON3.md`. Not yet implemented.
 */
public final class MiscRuleIndent extends MiscRuleCore {

    public MiscRuleIndent(final Lang lang, final boolean normalizeCommentStartCase,
            final boolean normalizeCommentEndPeriod, final boolean commentNormalizationClassifier,
            final int indentWidth, final int lineLengthLimit) {
        super(lang, normalizeCommentStartCase, normalizeCommentEndPeriod, commentNormalizationClassifier,
                indentWidth, lineLengthLimit);
        throw new UnsupportedOperationException(
                "MiscRuleIndent (Python3) is not yet implemented -- see STATE_PYTHON3.md");
    }
}
