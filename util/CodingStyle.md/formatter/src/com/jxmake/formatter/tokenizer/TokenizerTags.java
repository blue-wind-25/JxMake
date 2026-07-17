/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.tokenizer;

import com.jxmake.formatter.Lang;

/**
 * Skeleton landing spot for the tag-nested language family (XML/HTML5) -- see
 * `STATE_DATA_FORMATS.md`. Not yet implemented.
 */
public class TokenizerTags extends TokenizerCore {

    public TokenizerTags(final Lang lang) {
        throw new UnsupportedOperationException(
                "'" + lang.language + "' formatting is not yet implemented (scaffold only -- see "
                        + "STATE_DATA_FORMATS.md)");
    }
}
