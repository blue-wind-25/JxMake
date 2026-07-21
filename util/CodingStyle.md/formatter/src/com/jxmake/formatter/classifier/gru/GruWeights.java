/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.classifier.gru;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
     *  a silent-misparse risk. */
    public static GruWeights load(Path path) throws IOException {
        if (!Files.isReadable(path)) {
            throw new IOException("GRU weights file not readable: " + path);
        }
        throw new UnsupportedOperationException(
                "GruWeights.load: JSON parsing not yet implemented -- design-only scaffold per "
                        + "STATE_NEXT_AI.md, package com.jxmake.formatter.classifier.gru is NOT STARTED");
    }
}
