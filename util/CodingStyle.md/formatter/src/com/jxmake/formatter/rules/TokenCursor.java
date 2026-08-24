/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.rules;

import java.util.List;

import com.jxmake.formatter.tokenizer.TokenizerCore.Token;

/**
 * Minimal mutable token-list cursor (a token list plus a current index), promoted from
 * byte-identical private {@code Cursor} copies previously duplicated in {@link CssSpecificRule}
 * and {@link JsonSpecificRule}. Fields are accessed directly (not just via {@link #cur()}) by
 * both callers' own parse loops, same as before this pull-up.
 */
final class TokenCursor {

    final List<Token> toks;
          int         i;

    TokenCursor(final List<Token> toks)
    {
        this.toks = toks;
    }

    Token cur()
    {
        return i < toks.size() ? toks.get(i) : null;
    }

} // class TokenCursor
