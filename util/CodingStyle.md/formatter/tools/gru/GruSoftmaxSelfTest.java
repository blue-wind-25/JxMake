/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

import com.jxmake.formatter.classifier.CommentDecision;
import com.jxmake.formatter.classifier.gru.GruClassifier;

/**
 * Standalone self-check for {@link GruClassifier#softmax} and {@link GruClassifier#decide},
 * hand-feeding fake logits/probabilities -- no trained weights or forward pass needed, since
 * both are pure math/logic. Same zero-dependency plain-{@code main()} pattern as the other
 * {@code Gru*SelfTest.java} files. Non-shipped. Run via: {@code java GruSoftmaxSelfTest}.
 */
public final class GruSoftmaxSelfTest {

    private static       int    failures = 0;
    private static final double EPSILON  = 1e-9;

    public static void main(String[] args)
    {
        checkSoftmaxSumsToOne( new double[] {1.0, 2.0, 3.0} );
        checkSoftmaxSumsToOne( new double[] {0.0, 0.0, 0.0} );
        checkSoftmaxUniformOnEqualLogits();
        checkSoftmaxNumericallyStableOnLargeLogits();
        checkSoftmaxHighestLogitHighestProbability();
        checkSoftmaxEmpty();

        checkDecideReturnsClassAboveThreshold();
        checkDecideAbstainsBelowThreshold();
        checkDecideAbstainsExactlyAtThreshold();
        checkDecideRejectsWrongLength();

        if(failures == 0) {
            System.out.println("GruSoftmaxSelfTest: all checks passed");
        }
        else {
            System.err.println("GruSoftmaxSelfTest: " + failures + " check(s) failed");
            System.exit(1);
        }
    }

    private static void checkSoftmaxSumsToOne(double[] logits)
    {
        double[] p   = GruClassifier.softmax(logits);
        double   sum = 0.0;
        for(double v : p) sum += v;
        if( Math.abs(
            sum - 1.0
        ) > EPSILON ) fail(
            "softmax(" + java.util.Arrays.toString(logits) + ") sums to " + sum + ", expected 1.0"
        );
    }

    private static void checkSoftmaxUniformOnEqualLogits()
    {
        double[] p = GruClassifier.softmax( new double[] { 5.0, 5.0, 5.0 } );
        for(double v : p) {
            if( Math.abs( v - (1.0 / 3.0) ) > EPSILON ) {
                fail( "softmax of equal logits not uniform: " + java.util.Arrays.toString(p) );
                return;
            }
        }
    }

    private static void checkSoftmaxNumericallyStableOnLargeLogits()
    {
        double[] p = GruClassifier.softmax( new double[] { 1000.0, 1000.0, 1000.0 } );
        for(double v : p) {
            if( Double.isNaN(v) || Double.isInfinite(v) ) {
                fail(
                    "softmax of large equal logits produced NaN/Infinity: " + java.util.Arrays.toString(p)
                );
                return;
            }
        } // for
    }

    private static void checkSoftmaxHighestLogitHighestProbability()
    {
        double[] p = GruClassifier.softmax( new double[] { 1.0, 5.0, 2.0 } );
        if( !( p[1] > p[0] && p[1] > p[2] ) ) fail(
            "softmax did not rank highest logit's probability highest: " + java.util.Arrays.toString(p)
        );
    }

    private static void checkSoftmaxEmpty()
    {
        double[] p = GruClassifier.softmax( new double[0] );
        if(p.length != 0) fail(
            "softmax of empty logits should return empty array, got " + java.util.Arrays.toString(p)
        );
    }

    private static void checkDecideReturnsClassAboveThreshold()
    {
        // CLASS_ORDER = {YES, NO, ABSTAIN}; index 1 (NO) clearly above threshold.
        CommentDecision d = GruClassifier.decide( new double[] { 0.1, 0.8, 0.1 }, 0.5 );
        if(d != CommentDecision.NO) fail("decide() expected NO for [0.1, 0.8, 0.1], got " + d);
    }

    private static void checkDecideAbstainsBelowThreshold()
    {
        // Top class (index 0, YES) at 0.4 does not clear a 0.5 threshold.
        CommentDecision d = GruClassifier.decide( new double[] { 0.4, 0.35, 0.25 }, 0.5 );
        if(d != CommentDecision.ABSTAIN) fail(
            "decide() expected ABSTAIN for a sub-threshold top class, got " + d
        );
    }

    private static void checkDecideAbstainsExactlyAtThreshold()
    {
        // "top class must clear" (strictly greater than) the threshold, not just meet it
        CommentDecision d = GruClassifier.decide( new double[] { 0.5, 0.3, 0.2 }, 0.5 );
        if(d != CommentDecision.ABSTAIN) fail(
            "decide() expected ABSTAIN when top class exactly equals threshold, got " + d
        );
    }

    private static void checkDecideRejectsWrongLength()
    {
        try {
            GruClassifier.decide( new double[] {0.5, 0.5}, 0.5 );
            fail("decide() should reject a probability array not matching CLASS_ORDER length");
        }
        catch(IllegalArgumentException expected) {
            // Expected
        }
    }

    private static void fail(String message)
    {
        ++failures;
        System.err.println("FAIL: " + message);
    }

} // class GruSoftmaxSelfTest
