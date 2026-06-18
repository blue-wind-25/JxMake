/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.evaluator;

import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;
import java.util.List;

public class ComplexityPaddingEvaluator {

    public boolean isLoose(final List<Token> contentTokens) {
        for (final Token t : contentTokens) {
            if (t.type == TokenType.PUNCT && ("(".equals(t.text) || "[".equals(t.text))) {
                return true;
            }
        }
        return false;
    }
}
