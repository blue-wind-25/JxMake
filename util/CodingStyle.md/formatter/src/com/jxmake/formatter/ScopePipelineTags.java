/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter;

/**
 * Skeleton landing spot for the tag-nested language family (XML/HTML5) -- see
 * `STATE_DATA_FORMATS.md`. Not yet implemented.
 */
public final class ScopePipelineTags extends ScopePipelineCore {

    public ScopePipelineTags(final int indentWidth)
    {
        super(indentWidth);
    }

    @Override
    public String process(final String source)
    {
        throw new UnsupportedOperationException(
                "tag-based scope pipeline is not yet implemented (scaffold only -- see "
                        + "STATE_DATA_FORMATS.md)");
    }

} // class ScopePipelineTags
