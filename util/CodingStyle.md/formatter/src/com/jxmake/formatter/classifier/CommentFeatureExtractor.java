/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.classifier;

import com.jxmake.formatter.Lang;

/** Builds a {@link CommentFeatureVector} from raw comment text. Pure function, no formatter
 *  mutation -- see STATE_COMMENT_GRAMMAR.md's hard architectural constraint. Not yet wired into
 *  {@code MiscRule}; implementation is step 1 of that file's "Handoff note" suggested order. */
public final class CommentFeatureExtractor {

    private CommentFeatureExtractor() {
    }

    // TODO(comment-grammar): implement per STATE_COMMENT_GRAMMAR.md "Suggested order" step 1 --
    // pure-function feature extraction, testable with zero weights. Takes comment text + lang so
    // callers can produce a CommentFeatureVector without touching MiscRule's funnel points yet.
    public static CommentFeatureVector extract(final String commentText, final Lang lang) {
        throw new UnsupportedOperationException("CommentFeatureExtractor not yet implemented");
    }
}
