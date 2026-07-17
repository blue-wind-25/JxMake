/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter;

/**
 * Skeleton landing spot for the tag-nested language family (XML/HTML5) -- see
 * `STATE_DATA_FORMATS.md`. Not yet implemented.
 */
public final class ScopePipelineTags extends ScopePipelineCore {

    public ScopePipelineTags(final int indentWidth) {
        super(indentWidth);
    }

    @Override
    public String process(final String source) {
        throw new UnsupportedOperationException(
                "tag-based scope pipeline is not yet implemented (scaffold only -- see "
                        + "STATE_DATA_FORMATS.md)");
    }
}
