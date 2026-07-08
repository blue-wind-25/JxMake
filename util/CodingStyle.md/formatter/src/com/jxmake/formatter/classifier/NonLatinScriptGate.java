/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.classifier;

/** RDD_KEY_95: presence-based gate, not ratio-based -- any non-Latin codepoint anywhere in the
 *  comment skips the classifier entirely for that comment (mixed-token comments defeat
 *  whole-string language-ID; this is permanently out of scope, not a threshold to tune). */
public final class NonLatinScriptGate {

    private NonLatinScriptGate() {
    }

    // TODO(comment-grammar): implement per RDD_KEY_95 / STATE_COMMENT_GRAMMAR.md "Mechanical"
    // scope. Returns true iff any codepoint in commentText falls outside Latin script, meaning
    // the caller must treat the comment as ABSTAIN-equivalent without ever reaching scoring.
    public static boolean containsNonLatinScript(final String commentText) {
        throw new UnsupportedOperationException("NonLatinScriptGate not yet implemented");
    }
}
