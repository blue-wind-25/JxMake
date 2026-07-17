/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.tokenizer;

import com.jxmake.formatter.Lang;

/**
 * Skeleton landing spot for the indentation-block language family (Python3) -- see
 * `STATE_PYTHON3.md`. Not yet implemented.
 */
public class TokenizerIndent extends TokenizerCore {

    public TokenizerIndent(final Lang lang) {
        throw new UnsupportedOperationException(
                "'" + lang.language + "' formatting is not yet implemented (scaffold only -- see "
                        + "STATE_PYTHON3.md)");
    }
}
