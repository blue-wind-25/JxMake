/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.jxmake.formatter.classifier.gru.GruWeights;

/**
 * Standalone self-check for {@link GruWeights#load}, covering both the happy path and each error
 * path (missing field, bad schema version, malformed number, unreadable file). Same
 * zero-dependency plain-{@code main()} pattern as {@code GruTokenizerSelfTest.java} -- no test
 * framework exists anywhere in this project. Writes small temp JSON files under the system temp
 * directory and cleans them up. Non-shipped. Run via: {@code java GruWeightsSelfTest}.
 */
public final class GruWeightsSelfTest {

    private static int failures = 0;

    public static void main(String[] args) throws IOException
    {
        checkLoadsValidFile();
        checkMissingField();
        checkWrongSchemaVersion();
        checkMalformedNumber();
        checkUnreadableFile();
        checkNonPositiveDimensionRejected();
        checkAbstainThresholdOutOfRangeRejected();

        if(failures == 0) {
            System.out.println("GruWeightsSelfTest: all checks passed");
        }
        else {
            System.err.println("GruWeightsSelfTest: " + failures + " check(s) failed");
            System.exit(1);
        }
    }

    private static void checkLoadsValidFile() throws IOException
    {
        Path path = writeTemp(
            "{" + "\"schemaVersion\": 1," + "\"vocabSize\": 3500," + "\"hashBuckets\": 1024," + "\"embeddingDim\": 16," + "\"hiddenSize\": 224," + "\"sequenceCap\": 64," + "\"numClasses\": 3," + "\"abstainThreshold\": 0.5" + "}"
        );
        try {
            GruWeights w = GruWeights.load(path);
            expectEquals("schemaVersion", 1, w.schemaVersion);
            expectEquals("vocabSize", 3500, w.vocabSize);
            expectEquals("hashBuckets", 1024, w.hashBuckets);
            expectEquals("embeddingDim", 16, w.embeddingDim);
            expectEquals("hiddenSize", 224, w.hiddenSize);
            expectEquals("sequenceCap", 64, w.sequenceCap);
            expectEquals("numClasses", 3, w.numClasses);
            if(w.abstainThreshold != 0.5) fail(
                "abstainThreshold = " + w.abstainThreshold + ", expected 0.5"
            );
        }
        catch(IOException e) {
            fail( "valid file unexpectedly failed to load: " + e.getMessage() );
        }
        finally {
            Files.deleteIfExists(path);
        }
    }

    private static void checkMissingField() throws IOException
    {
        Path path = writeTemp("{\"schemaVersion\": 1, \"vocabSize\": 3500}");
        try {
            GruWeights.load(path);
            fail("missing-field file unexpectedly loaded without error");
        }
        catch(IOException e) {
            if( !e.getMessage().contains(
                "missing required field"
            ) ) fail(
                "missing-field error message unexpected: " + e.getMessage()
            );
        }
        finally {
            Files.deleteIfExists(path);
        }
    }

    private static void checkWrongSchemaVersion() throws IOException
    {
        Path path = writeTemp("{\"schemaVersion\": 99}");
        try {
            GruWeights.load(path);
            fail("wrong-schema-version file unexpectedly loaded without error");
        }
        catch(IOException e) {
            if( !e.getMessage().contains(
                "schemaVersion"
            ) ) fail(
                "wrong-schema-version error message unexpected: " + e.getMessage()
            );
        }
        finally {
            Files.deleteIfExists(path);
        }
    }

    private static void checkMalformedNumber() throws IOException
    {
        Path path = writeTemp("{\"schemaVersion\": \"one\"}");
        try {
            GruWeights.load(path);
            fail("malformed-number file unexpectedly loaded without error");
        }
        catch(IOException e) {
            // Expected: findFieldValue's regex only matches numeric values, so a quoted
            // "one" is treated the same as a missing field -- both are hard errors, which
            // is the important property (never silently misparses), not a specific message
        }
        finally {
            Files.deleteIfExists(path);
        }
    }

    private static void checkUnreadableFile()
    {
        Path path = Paths.get(
            System.getProperty("java.io.tmpdir"), "gru_weights_does_not_exist.json"
        );
        try {
            GruWeights.load(path);
            fail("nonexistent file unexpectedly loaded without error");
        }
        catch(IOException e) {
            if( !e.getMessage().contains(
                "not readable"
            ) ) fail(
                "unreadable-file error message unexpected: " + e.getMessage()
            );
        }
    }

    private static void checkNonPositiveDimensionRejected() throws IOException
    {
        Path path = writeTemp(
            "{" + "\"schemaVersion\": 1," + "\"vocabSize\": 3500," + "\"hashBuckets\": 1024," + "\"embeddingDim\": 16," + "\"hiddenSize\": 0," + "\"sequenceCap\": 64," + "\"numClasses\": 3," + "\"abstainThreshold\": 0.5" + "}"
        );
        try {
            GruWeights.load(path);
            fail("zero hiddenSize unexpectedly loaded without error");
        }
        catch(IOException e) {
            if( !e.getMessage().contains(
                "must be positive"
            ) ) fail(
                "non-positive-dimension error message unexpected: " + e.getMessage()
            );
        }
        finally {
            Files.deleteIfExists(path);
        }
    }

    private static void checkAbstainThresholdOutOfRangeRejected() throws IOException
    {
        Path path = writeTemp(
            "{" + "\"schemaVersion\": 1," + "\"vocabSize\": 3500," + "\"hashBuckets\": 1024," + "\"embeddingDim\": 16," + "\"hiddenSize\": 224," + "\"sequenceCap\": 64," + "\"numClasses\": 3," + "\"abstainThreshold\": 1.5" + "}"
        );
        try {
            GruWeights.load(path);
            fail("out-of-range abstainThreshold unexpectedly loaded without error");
        }
        catch(IOException e) {
            if( !e.getMessage().contains(
                "abstainThreshold"
            ) ) fail(
                "out-of-range abstainThreshold error message unexpected: " + e.getMessage()
            );
        }
        finally {
            Files.deleteIfExists(path);
        }
    }

    private static void expectEquals(String field, int expected, int actual)
    {
        if(expected != actual) fail(field + " = " + actual + ", expected " + expected);
    }

    private static Path writeTemp(String content) throws IOException
    {
        Path path = Files.createTempFile("gru_weights_selftest", ".json");
        Files.write( path, content.getBytes(StandardCharsets.UTF_8) );

        return path;
    }

    private static void fail(String message)
    {
        ++failures;
        System.err.println("FAIL: " + message);
    }

} // class GruWeightsSelfTest
