/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.classifier.gru;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Loader/schema for the external, trained weights file consumed by {@link GruClassifier}.
 *  Per STATE_NEXT_AI.md's "GRU implementation design", the trainer (a separate, non-shipped
 *  {@code tools/gru} entry point) writes this file; {@code GruClassifier} never bakes weight
 *  arrays into source the way {@link com.jxmake.formatter.classifier.CommentClassifierWeights}
 *  does, since a neural net's weight count isn't hand-editable the same way and retraining
 *  shouldn't require a JAR rebuild. JSON is used (not a flat binary tensor dump) so a trained
 *  file stays diffable/inspectable in v1 (RDD_EXT_14). */
public final class GruWeights {

    /** Current schema version this loader understands. Bump alongside any incompatible change
     *  to this class's field layout. */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public final int schemaVersion;

    // Vocabulary / embedding.
    public final int vocabSize;
    public final int hashBuckets;
    public final int embeddingDim;

    // Bidirectional GRU.
    public final int hiddenSize;
    public final int sequenceCap;

    // Classification head.
    public final int numClasses;

    /** Softmax confidence cutoff below which the classifier abstains (RDD_EXT_11). Lives here,
     *  not hardcoded, so a retrain can ship a new threshold alongside new weights in one file. */
    public final double abstainThreshold;

    GruWeights(int schemaVersion, int vocabSize, int hashBuckets, int embeddingDim,
            int hiddenSize, int sequenceCap, int numClasses, double abstainThreshold) {
        this.schemaVersion = schemaVersion;
        this.vocabSize = vocabSize;
        this.hashBuckets = hashBuckets;
        this.embeddingDim = embeddingDim;
        this.hiddenSize = hiddenSize;
        this.sequenceCap = sequenceCap;
        this.numClasses = numClasses;
        this.abstainThreshold = abstainThreshold;
    }

    /** Loads and validates a weights file from {@code path}. Throws a clear, explicit error
     *  naming the expected vs. found schema version on any mismatch, rather than attempting to
     *  parse a shape the loader wasn't written for -- per RDD_EXT_14, this is a hard error, not
     *  a silent-misparse risk.
     *
     *  <p>Parses only the flat scalar fields declared above (hand-rolled regex extraction, no
     *  external JSON library -- the project has none and the schema is currently flat key/value
     *  pairs, not nested arrays). The actual embedding table, GRU weight matrices, and dense-head
     *  weights are not represented by this class yet -- those land once the training pipeline
     *  writes real numbers to parse (see {@code tools/gru/GruTrainer.java}). */
    public static GruWeights load(Path path) throws IOException {
        if (!Files.isReadable(path)) {
            throw new IOException("GRU weights file not readable: " + path);
        }
        String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        int schemaVersion = requireIntField(json, "schemaVersion", path);
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IOException("GRU weights file " + path + " has schemaVersion "
                    + schemaVersion + ", expected " + CURRENT_SCHEMA_VERSION);
        }

        return new GruWeights(
                schemaVersion,
                requireIntField(json, "vocabSize", path),
                requireIntField(json, "hashBuckets", path),
                requireIntField(json, "embeddingDim", path),
                requireIntField(json, "hiddenSize", path),
                requireIntField(json, "sequenceCap", path),
                requireIntField(json, "numClasses", path),
                requireDoubleField(json, "abstainThreshold", path));
    }

    private static final Pattern NUMBER_FIELD = Pattern.compile(
            "\"(\\w+)\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");

    private static String findFieldValue(String json, String key, Path path) throws IOException {
        Matcher m = NUMBER_FIELD.matcher(json);
        while (m.find()) {
            if (m.group(1).equals(key)) {
                return m.group(2);
            }
        }
        throw new IOException("GRU weights file " + path + " is missing required field \"" + key + "\"");
    }

    private static int requireIntField(String json, String key, Path path) throws IOException {
        String value = findFieldValue(json, key, path);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IOException("GRU weights file " + path + " field \"" + key
                    + "\" is not an integer: " + value);
        }
    }

    private static double requireDoubleField(String json, String key, Path path) throws IOException {
        String value = findFieldValue(json, key, path);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IOException("GRU weights file " + path + " field \"" + key
                    + "\" is not a number: " + value);
        }
    }
}
