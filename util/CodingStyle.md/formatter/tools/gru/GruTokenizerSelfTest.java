/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

import java.util.List;

import com.jxmake.formatter.classifier.gru.GruClassifier;

/**
 * Standalone self-check for {@code GruClassifier}'s tokenizer and OOV hash bucket, per
 * RDD_EXT_12/13. No test framework exists anywhere in this project (the formatter's own
 * testing methodology is the {@code _inp}/{@code _out} fixture-diffing in {@code test/}, which
 * doesn't apply to internal classifier logic), so this follows the project's existing
 * zero-dependency style: a plain {@code main()} that asserts and exits non-zero on failure.
 * Non-shipped, like {@code GruTrainer.java}. Run via: {@code java GruTokenizerSelfTest}.
 */
public final class GruTokenizerSelfTest {

    private static int failures = 0;

    public static void main(final String[] args)
    {
        checkTokenize( "matrix.", asList("matrix", ".") );
        checkTokenize( "// for i", asList("/", "/", "for", "i") );
        checkTokenize( "camelCase snake_case", asList("camelCase", "snake_case") );
        checkTokenize( "e.g. done", asList("e", ".", "g", ".", "done") );
        checkTokenize( "", asList() );
        checkTokenize( "   ", asList() );

        checkHashDeterministic("matrix");
        checkHashDeterministic("");
        checkHashInRange("some_long_identifier_name_that_is_definitely_oov");
        checkHashDistinct("foo", "bar");

        if(failures == 0) {
            System.out.println("GruTokenizerSelfTest: all checks passed");
        }
        else {
            System.err.println("GruTokenizerSelfTest: " + failures + " check(s) failed");
            System.exit(1);
        }
    }

    private static void checkTokenize(final String input, final List<String> expected)
    {
        final List<String> actual = GruClassifier.tokenize(input);
        if( !actual.equals(
            expected
        ) ) fail(
            "tokenize(" + quote(input) + ") = " + actual + ", expected " + expected
        );
    }

    private static void checkHashDeterministic(final String token)
    {
        final int a = GruClassifier.hashBucket(token);
        final int b = GruClassifier.hashBucket(token);
        if(a != b) fail( "hashBucket(" + quote(token) + ") not deterministic: " + a + " vs " + b );
    }

    private static void checkHashInRange(final String token)
    {
        final int h = GruClassifier.hashBucket(token);
        if(h < 0 || h >= GruClassifier.HASH_BUCKETS) fail(
            "hashBucket(" + quote(token) + ") = " + h + " out of range [0, " + GruClassifier.HASH_BUCKETS + ")"
        );
    }

    private static void checkHashDistinct(final String a, final String b)
    {
        if( GruClassifier.hashBucket(
            a
        ) == GruClassifier.hashBucket(
            b
        ) ) System.out.println(
            "GruTokenizerSelfTest: note -- hashBucket(" + quote(a) + ") collides " + "with hashBucket(" + quote(b) + ") (possible for a 1024-bucket hash, not a failure)"
        );
    }

    private static void fail(final String message)
    {
        ++failures;
        System.err.println("FAIL: " + message);
    }

    private static String quote(final String s)
    {
        return "\"" + s + "\"";
    }

    private static List<String> asList(final String... items)
    {
        return java.util.Arrays.asList(items);
    }

} // class GruTokenizerSelfTest
