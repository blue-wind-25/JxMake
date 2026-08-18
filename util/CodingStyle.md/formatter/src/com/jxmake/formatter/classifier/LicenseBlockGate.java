/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.classifier;

import java.util.regex.Pattern;

/**
 * Detects a comment that reads as a multi-line license/copyright block -- the informal rule
 * {@code tools/gru/README.txt}'s "Hand labeling" section already uses for Pool B: "spans 2+
 * newlines, a license block not a single sentence -&gt; NO". Per STATE_AI.md's "further
 * CommentClassifier NO-producing gates" TODO, that hand-labeling shortcut is too blunt to use
 * verbatim -- a real multi-line prose paragraph (a comment split across lines purely for line-
 * width reasons, common throughout this codebase's own Javadoc/block-comment style) must not
 * misfire. Mirrors {@link CommentedOutCodeGate}'s two-independent-signal shape: a primary
 * newline-count-plus-terminal-punctuation signal, confirmed by a second, independent
 * license/copyright-specific signal, rather than either alone.
 *
 *  <p><b>Primary signal</b> -- spans 2+ newlines <i>and</i> the comment's last non-whitespace
 *  character is not sentence-ending punctuation ({@code .}/{@code !}/{@code ?}). Genuine
 *  multi-sentence prose (the false-positive risk this gate exists to avoid) overwhelmingly ends
 *  its final sentence with terminal punctuation; a license/copyright block frequently does not
 *  (e.g. this project's own {@code STATE_COMMON.md} standard test-fixture header, ending in
 *  {@code "SPDX-License-Identifier: MIT"}, no trailing period). This alone is not used as the
 *  gate, since some real license headers (including this project's own {@code Copyright (C) ...}
 *  file header, ending "... Apache License, Version 2.0 text.") do end in a period and would be
 *  missed -- an accepted false-skip (asymmetric-risk design used throughout this classifier), not
 *  a correctness problem.
 *
 *  <p><b>Confirming signal</b> -- explicit copyright/license vocabulary anywhere in the text
 *  (e.g. {@code Copyright}, {@code SPDX-License-Identifier}, {@code Licensed under}, {@code All
 *  rights reserved}). Requiring this confirming signal keeps an ordinary multi-line prose
 *  paragraph that simply lacks a final period (rare, but possible -- e.g. a trailing fragment or
 *  a list intro line) from misfiring. A decorative-border-line confirming signal (the shape
 *  {@link DecorativeSeparatorGate} recognizes at the whole-comment level) was tried and rejected:
 *  real-corpus testing (`tools/gru/sample_default.txt`) found hundreds of ordinary decorative
 *  section-banner comments (e.g. XML build-file section headers framed by
 *  {@code =====...=====} border lines, no copyright/license content at all) that share the
 *  border-line shape without being license blocks -- the same over-broad-single-signal failure
 *  mode as the rejected leading-hyphen gate (see STATE_AI.md's 2026-07-30 "Bug 2" note).
 *  License-specific vocabulary alone is narrow enough to avoid that false-positive class while
 *  still catching every real license/copyright block found in this codebase's own corpus.
 */
public final class LicenseBlockGate {

    // Copyright/license-specific vocabulary -- deliberately narrow (named terms that essentially
    // never appear in ordinary code-comment prose outside an actual license/copyright notice),
    // mirroring the same "false match only costs an unnecessary NO" asymmetric-risk acceptance
    // used by every other gate in this classifier
    private static final Pattern LICENSE_VOCABULARY = Pattern.compile(
        "\\bCopyright\\b|\\(C\\)|\\bSPDX-License-Identifier\\b|\\bLicensed under\\b" + "|\\bAll rights reserved\\b|\\bRedistribution and use\\b" + "|\\bPermission is hereby granted\\b|\\bWITHOUT WARRANTIES?\\b"
    );

    private LicenseBlockGate()
    {
    }

    public static boolean looksLikeLicenseBlock(final String commentText)
    {
        if( countNewlines(commentText) < 2 ) return false;
        if( endsWithSentenceEndingPunctuation(commentText) ) return false;

        return LICENSE_VOCABULARY.matcher(commentText).find();
    }

    private static int countNewlines(final String commentText)
    {
        int count = 0;
        for( int i = 0; i < commentText.length(); ++i ) if( commentText.charAt(i) == '\n' ) count++;

        return count;
    }

    private static boolean endsWithSentenceEndingPunctuation(final String commentText)
    {
        int i = commentText.length() - 1;
        while( i >= 0 && Character.isWhitespace( commentText.charAt(i) ) ) i--;
        if(i < 0) return false;
        final char last = commentText.charAt(i);

        return last == '.' || last == '!' || last == '?';
    }

} // class LicenseBlockGate
