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
     *  sides. Public so {@code tools/gru/GruTrainer.java} (outside {@code src/}, a different
     *  package) can call the exact same {@link #tokenize}/{@link #hashBucket} the runtime uses --
     *  RDD_EXT_13 requires these stay bit-for-bit identical between training and runtime. */
    public static final int HASH_BUCKETS = 1024;

    /** Per-comment token cap (truncate/pad), per the finalized architecture. */
    static final int SEQUENCE_CAP = 64;

    /** Fixed softmax output class order this codebase uses -- an encoding convention (like
     *  {@link #HASH_BUCKETS}'s hash choice), not one of STATE_NEXT_AI.md's open items. Whatever
     *  training pipeline produces the weights file must emit its 3-way softmax output in this
     *  same order, since {@link #decide} maps output index -> class positionally. */
    public static final CommentDecision[] CLASS_ORDER = {
        CommentDecision.YES, CommentDecision.NO, CommentDecision.ABSTAIN
    };

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
     *  missing-weights-file fail-safe.
     *
     *  <p><b>Stub behavior (intentional, not a bug):</b> the actual forward pass (embedding
     *  lookup, bidirectional GRU recurrence, dense head) is not yet implemented -- it is blocked
     *  on a real trained weights file with an embedding table and GRU weight matrices, neither of
     *  which {@link GruWeights} represents yet (see its javadoc; only the flat scalar
     *  architecture-constant fields exist so far). Until that lands, this method unconditionally
     *  returns {@link CommentDecision#ABSTAIN} for every call, matching this project's existing
     *  fail-safe posture everywhere else in this design: missing/unusable signal -> ABSTAIN ->
     *  mechanical fallback, never blocks formatting. {@code tokenize} is still called so the
     *  tokenization path is exercised and ready once a real forward pass lands. */
    public CommentDecision classify(String commentText, int targetWordIndex) {
        tokenize(commentText);
        return CommentDecision.ABSTAIN;
    }

    /** Word-level tokenization per RDD_EXT_12: trailing/attached punctuation splits into its own
     *  token ({@code matrix.} -> {@code matrix} + {@code .}), consistent with the existing
     *  rule-based classifier's own dot-count reasoning. camelCase/snake_case identifiers stay
     *  whole -- not sub-tokenized, since the classification signal comes from surrounding context
     *  words, not from decomposing the identifier itself. */
    public static List<String> tokenize(String commentText) {
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

    /** Numerically-stable softmax: converts raw class scores (logits) into a probability
     *  distribution that sums to 1. Subtracts the max logit before exponentiating to avoid
     *  overflow -- this doesn't change the result ({@code softmax(x) == softmax(x - c)} for any
     *  constant {@code c}), only its numerical stability for large logit magnitudes. Pure math,
     *  independent of any trained weights -- usable now even though the forward pass that
     *  produces real logits isn't implemented yet. */
    public static double[] softmax(double[] logits) {
        if (logits.length == 0) {
            return new double[0];
        }
        double max = logits[0];
        for (double v : logits) {
            if (v > max) {
                max = v;
            }
        }
        double[] exp = new double[logits.length];
        double sum = 0.0;
        for (int i = 0; i < logits.length; i++) {
            exp[i] = Math.exp(logits[i] - max);
            sum += exp[i];
        }
        for (int i = 0; i < exp.length; i++) {
            exp[i] /= sum;
        }
        return exp;
    }

    /** Maps a softmax probability distribution to a {@link CommentDecision} per RDD_EXT_11: the
     *  top class must clear {@code abstainThreshold} (not just be the argmax) to be returned as
     *  that class; otherwise this abstains, same posture as the missing-weights-file fail-safe.
     *  {@code probabilities[i]} corresponds to {@link #CLASS_ORDER}{@code [i]} -- callers must
     *  pass a distribution produced in that same class order. */
    public static CommentDecision decide(double[] probabilities, double abstainThreshold) {
        if (probabilities.length != CLASS_ORDER.length) {
            throw new IllegalArgumentException("expected " + CLASS_ORDER.length
                    + " probabilities (one per CLASS_ORDER entry), got " + probabilities.length);
        }
        int argmax = 0;
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > probabilities[argmax]) {
                argmax = i;
            }
        }
        if (probabilities[argmax] > abstainThreshold) {
            return CLASS_ORDER[argmax];
        }
        return CommentDecision.ABSTAIN;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** FNV-1a (32-bit) hash mod {@link #HASH_BUCKETS}, per RDD_EXT_13. Must stay bit-for-bit
     *  identical between the training side and this runtime side. */
    public static int hashBucket(String token) {
        int hash = 0x811C9DC5;
        byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            hash ^= (b & 0xFF);
            hash *= 0x01000193;
        }
        return Math.floorMod(hash, HASH_BUCKETS);
    }
}
