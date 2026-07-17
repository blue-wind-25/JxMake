/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter;

/**
 * Skeleton landing spot for the indent-based language family (Python3) -- see
 * `STATE_PYTHON3.md`. Not yet implemented.
 */
public final class ScopePipelineIndent extends ScopePipelineCore {

    public ScopePipelineIndent(final int indentWidth) {
        super(indentWidth);
    }

    @Override
    public String process(final String source) {
        throw new UnsupportedOperationException(
                "indent-based scope pipeline is not yet implemented (scaffold only -- see "
                        + "STATE_PYTHON3.md)");
    }
}
