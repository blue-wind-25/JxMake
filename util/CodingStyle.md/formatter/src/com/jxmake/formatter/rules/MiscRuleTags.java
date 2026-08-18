/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;

/**
 * Skeleton landing spot for the tag-based language family (XML/HTML5) -- see
 * `STATE_DATA_FORMATS.md`. Not yet implemented.
 */
public final class MiscRuleTags extends MiscRuleCore {

    public MiscRuleTags(
        final Lang    lang,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod,
        final boolean commentNormalizationClassifier,
        final int     indentWidth,
        final int     lineLengthLimit
    )
    {
        super(lang, normalizeCommentStartCase, normalizeCommentEndPeriod, commentNormalizationClassifier,
                indentWidth, lineLengthLimit);
        throw new UnsupportedOperationException(
            "MiscRuleTags (XML/HTML5) is not yet implemented -- see STATE_DATA_FORMATS.md"
        );
    }

} // class MiscRuleTags
