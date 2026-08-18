/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.tokenizer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Increment-1 scaffolding for STATE_JS_TS.md's Step 2 ("context 11" scoping session) -- NOT
 *  user-facing, NOT part of any formatting output. This is the "detect-and-measure-only slice"
 *  called for by that scoping session's suggested 5-increment breakdown, step (1): confirm a
 *  {@code JSX_SPAN}'s opening tag is over-width, with no line-breaking emitted yet. Consulted only
 *  by {@code FormatterCurly.formatOne} (records a measurement) and this job's own test fixtures/
 *  future increments (read the counters back). No other pass reads or writes these counters, and
 *  nothing here ever mutates a token's {@code text} -- purely observational.
 *
 * <p>Increment-1 approximation, documented rather than silently assumed: {@link
 *  #recordOpeningTagMeasurement} measures only the opening tag's own raw width (
 *  {@code Token#jsxOpeningTagEndOffset}), not the tag's actual rendered column position (current
 *  indentation plus any preceding same-line tokens) -- a real column-aware fits-check is exactly
 *  the kind of rendering-time machinery a future wrap-implementing increment needs, and is
 *  deliberately out of scope for this detect-only slice.
 */
public final class JsxWrapDiagnostics {

    private JsxWrapDiagnostics() {}

    private static final AtomicInteger measuredCount  = new AtomicInteger(0);
    private static final AtomicInteger overWidthCount = new AtomicInteger(0);

    /** Test-only reset, so one fixture's counts don't leak into the next test run's assertions */
    public static void reset()
    {
        measuredCount.set(0);
        overWidthCount.set(0);
    }

    /** Total number of {@code JSX_SPAN} opening tags measured so far (over-width or not) */
    public static int measuredCount()
    {
        return measuredCount.get();
    }

    /** Number of measured opening tags whose raw width exceeded {@code lineLengthLimit} */
    public static int overWidthCount()
    {
        return overWidthCount.get();
    }

    /**
     * Records one opening-tag width measurement. {@code openingTagWidth} is expected to be a
     *  {@code JSX_SPAN} token's {@code jsxOpeningTagEndOffset} (the opening tag's own raw
     *  character width, `<` through `>`/`/>` inclusive) -- see this class's own doc comment for
     *  the documented column-position approximation this increment accepts.
     */
    public static void recordOpeningTagMeasurement(
        final int openingTagWidth,
        final int lineLengthLimit
    )
    {
        measuredCount.incrementAndGet();
        if(openingTagWidth > lineLengthLimit) overWidthCount.incrementAndGet();
    }

} // class JsxWrapDiagnostics
