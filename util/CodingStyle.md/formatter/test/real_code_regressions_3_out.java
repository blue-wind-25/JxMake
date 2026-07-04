/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package test;

public class RealCodeRegressions3 {

    void m()
    {
        config.lineLength             = parseInt(raw, "line-length", config.lineLength);
        config.indentSize             = parseInt(raw, "indent-size", config.indentSize);
        config.closingCommentMinLines = parseInt(
            raw, "closing-comment-min-lines", config.closingCommentMinLines
        );
        config.formatMacros           = parseBoolean(raw, "format-macros", config.formatMacros);
    }

} // class RealCodeRegressions3
