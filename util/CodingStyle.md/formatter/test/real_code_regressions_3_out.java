/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
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
