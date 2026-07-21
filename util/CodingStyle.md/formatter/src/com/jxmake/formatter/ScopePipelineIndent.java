/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;
import com.jxmake.formatter.tokenizer.TokenizerIndent;

import java.util.List;

/**
 * Indentation-block-family scope pipeline (Python3 -- see STATE_PYTHON3.md). Statement/
 * indentation skeleton only: tokenizes via {@link TokenizerIndent} and renders the token stream
 * straight back to source text with no transformation yet -- a deliberate identity pass, not a
 * placeholder. Its purpose is to prove the tokenize/render round-trip is lossless (byte-identical
 * output for byte-identical input, for every token kind including the synthesized zero-text
 * {@code INDENT}/{@code DEDENT} markers) *before* any cosmetic rule (STYLE_PYTHON3.md §1-9) is
 * layered on top -- since Python's indentation is semantically load-bearing, any bug in this
 * base layer would corrupt block membership, not just cosmetics. Rule passes land as later calls
 * inside {@link #process}, each re-tokenizing/re-rendering in turn, mirroring
 * {@link ScopePipelineCurly}'s own multi-pass shape.
 */
public final class ScopePipelineIndent extends ScopePipelineCore {

    private final Lang lang;

    public ScopePipelineIndent(final Lang lang, final int indentWidth) {
        super(indentWidth);
        this.lang = lang;
    }

    @Override
    public String process(final String source) {
        final List<Token> tokens = new TokenizerIndent(lang).tokenize(source);
        return render(tokens);
    }

    /** Reassembles {@code tokens}' source text verbatim. Every token kind's {@code text} is its
     *  exact original source span, EXCEPT the synthesized {@code INDENT}/{@code DEDENT} markers
     *  (see {@link TokenizerIndent#synthesizeIndentation}), which carry no source text of their
     *  own (their {@code text} field instead holds the new indent width, for a later rule pass's
     *  use) and so must be skipped here rather than appended. */
    private String render(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        for (final Token t : tokens) {
            if (t.type == TokenType.INDENT || t.type == TokenType.DEDENT) {
                continue;
            }
            out.append(t.text);
        }
        return out.toString();
    }
}
