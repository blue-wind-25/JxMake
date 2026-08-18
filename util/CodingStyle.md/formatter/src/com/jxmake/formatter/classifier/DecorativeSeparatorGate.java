/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.classifier;

/**
 * First real {@link CommentClassifier} gate that can return {@link CommentDecision#NO} (see
 *  RDD_KEY_96's note that {@code classify} previously had no NO-producing path at all). Presence-
 *  based, like {@link NonLatinScriptGate}: a comment with no letter or digit anywhere -- just
 *  punctuation/symbol runs such as {@code ****...****} or {@code #####...#####} -- cannot be prose
 *  by construction, so this is decided as confidently as the non-Latin-script gate rather than via
 *  {@link CommentClassifierWeights}'s scored path. Deliberately narrow: does not attempt to catch
 *  commented-out code or license blocks (those still need real word content to distinguish from
 *  prose, so they stay on the scored path / a future gate, not this one).
 */
public final class DecorativeSeparatorGate {

    private DecorativeSeparatorGate()
    {
    }

    public static boolean isDecorativeOnly(final String commentText)
    {
        boolean hasNonWhitespace = false;
        int     i                = 0;
        while( i < commentText.length() ) {
            final int codepoint = commentText.codePointAt(i);
            i += Character.charCount(codepoint);
            if( Character.isWhitespace(codepoint) ) continue;
            hasNonWhitespace = true;
            if( Character.isLetterOrDigit(codepoint) ) return false;
        } // while

        return hasNonWhitespace;
    }

} // class DecorativeSeparatorGate
