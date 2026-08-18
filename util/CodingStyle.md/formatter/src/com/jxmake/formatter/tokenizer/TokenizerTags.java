/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.tokenizer;

import com.jxmake.formatter.Lang;

/**
 * Skeleton landing spot for the tag-nested language family (XML/HTML5) -- see
 * `STATE_DATA_FORMATS.md`. Not yet implemented.
 */
public class TokenizerTags extends TokenizerCore {

    public TokenizerTags(final Lang lang)
    {
        throw new UnsupportedOperationException(
                "'" + lang.language + "' formatting is not yet implemented (scaffold only -- see "
                        + "STATE_DATA_FORMATS.md)");
    }

} // class TokenizerTags
