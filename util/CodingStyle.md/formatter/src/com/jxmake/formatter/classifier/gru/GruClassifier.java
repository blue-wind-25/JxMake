/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.classifier.gru;

import com.jxmake.formatter.classifier.CommentDecision;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Inference-only runtime for the Step 3 comment-classifier abstain-case resolution, per
 *  STATE_NEXT_AI.md's "GRU implementation design" -- a purpose-trained ~500k-parameter
 *  bidirectional GRU, the only feasible Step 3 approach (small instruction-tuned LLMs were
 *  tested and confirmed NOT FEASIBLE at this task). Loads a trained {@link GruWeights} file at
 *  startup; never contains literal weight arrays in source, unlike
 *  {@link com.jxmake.formatter.classifier.CommentClassifierWeights}'s baked-in linear-model
 *  constants -- a neural net's weight count isn't hand-editable the same way, and retraining
 *  shouldn't require a JAR rebuild.
 *
 *  <p>Design-only scaffold: package {@code com.jxmake.formatter.classifier.gru} is NOT STARTED
 *  per STATE_NEXT_AI.md's checklist. Method bodies below follow the finalized architecture
 *  (word-level tokens, ~3.5k explicit vocab + 1024 FNV-1a hash buckets for OOV, 16-dim
 *  embeddings, single-layer bidirectional GRU hidden=224, target-word biGRU-output indexing,
 *  dense(64, ReLU) -> softmax) but are not yet implemented. */
public final class GruClassifier {

    /** Number of OOV hash buckets (RDD_EXT_13): FNV-1a (32-bit) mod this value. Deterministic,
     *  no external dependency, trivially identical to reimplement on the training and runtime
     *  sides. */
    static final int HASH_BUCKETS = 1024;

    /** Per-comment token cap (truncate/pad), per the finalized architecture. */
    static final int SEQUENCE_CAP = 64;

    private final GruWeights weights;

    private GruClassifier(GruWeights weights) {
        this.weights = weights;
    }

    /** Loads a trained weights file and returns a ready-to-use classifier. Per the fail-safe
     *  posture documented in STATE_NEXT_AI.md, a missing or unreadable weights file must make
     *  the caller behave as {@link CommentDecision#ABSTAIN} for every comment -- callers should
     *  catch {@link IOException} here and fall back accordingly rather than aborting formatting;
     *  this method itself only reports the failure, it doesn't apply the fallback. */
    public static GruClassifier load(Path weightsFile) throws IOException {
        return new GruClassifier(GruWeights.load(weightsFile));
    }

    /** Classifies a single comment's ambiguous target word in context, returning the same
     *  {@code YES}/{@code NO}/{@code ABSTAIN} classes as the existing rule-based classifier
     *  (RDD_EXT_10 -- no more granular intermediate class). Abstains when the top softmax class
     *  doesn't clear {@link GruWeights#abstainThreshold} (RDD_EXT_11), same posture as the
     *  missing-weights-file fail-safe. */
    public CommentDecision classify(String commentText, int targetWordIndex) {
        List<String> tokens = tokenize(commentText);
        throw new UnsupportedOperationException(
                "GruClassifier.classify: inference not yet implemented -- design-only scaffold "
                        + "per STATE_NEXT_AI.md, package com.jxmake.formatter.classifier.gru is NOT STARTED");
    }

    /** Word-level tokenization per RDD_EXT_12: trailing/attached punctuation splits into its own
     *  token ({@code matrix.} -> {@code matrix} + {@code .}), consistent with the existing
     *  rule-based classifier's own dot-count reasoning. camelCase/snake_case identifiers stay
     *  whole -- not sub-tokenized, since the classification signal comes from surrounding context
     *  words, not from decomposing the identifier itself. */
    static List<String> tokenize(String commentText) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        int n = commentText.length();
        while (i < n) {
            char c = commentText.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (isWordChar(c)) {
                int start = i;
                while (i < n && isWordChar(commentText.charAt(i))) {
                    i++;
                }
                tokens.add(commentText.substring(start, i));
            } else {
                tokens.add(String.valueOf(c));
                i++;
            }
        }
        return tokens;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** FNV-1a (32-bit) hash mod {@link #HASH_BUCKETS}, per RDD_EXT_13. Must stay bit-for-bit
     *  identical between the training side and this runtime side. */
    static int hashBucket(String token) {
        int hash = 0x811C9DC5;
        byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            hash ^= (b & 0xFF);
            hash *= 0x01000193;
        }
        return Math.floorMod(hash, HASH_BUCKETS);
    }
}
