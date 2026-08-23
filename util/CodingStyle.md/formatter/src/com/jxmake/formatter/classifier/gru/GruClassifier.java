/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.classifier.gru;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jxmake.formatter.classifier.CommentDecision;

/**
 * Runtime and shared forward/backward math for the Step 3 comment-classifier abstain-case
 * resolution, per STATE_NEXT_AI.md's "GRU implementation design" -- a purpose-trained
 * ~500k-parameter bidirectional GRU, the only feasible Step 3 approach (small instruction-tuned
 * LLMs were tested and confirmed NOT FEASIBLE at this task). Loads a trained {@link GruWeights}
 * file at startup; never contains literal weight arrays in source, unlike
 * {@link com.jxmake.formatter.classifier.CommentClassifierWeights}'s baked-in linear-model
 * constants -- a neural net's weight count isn't hand-editable the same way, and retraining
 * shouldn't require a JAR rebuild.
 *
 *  <p>{@link #forward} and {@link #backward} are {@code public static} (not just used internally
 *  by {@link #classify}) so {@code tools/gru/GruTrainer.java} -- outside {@code src/}, a different
 *  package, since the runtime JAR must never bundle training code -- can run the exact same
 *  forward pass and backpropagate through it during training, per the same bit-for-bit-identical
 *  requirement RDD_EXT_13 states for {@link #tokenize}/{@link #hashBucket}.
 */
public final class GruClassifier {

    /**
     * Number of OOV hash buckets (RDD_EXT_13): FNV-1a (32-bit) mod this value. Deterministic,
     * no external dependency, trivially identical to reimplement on the training and runtime
     * sides. Public so {@code tools/gru/GruTrainer.java} (outside {@code src/}, a different
     * package) can call the exact same {@link #tokenize}/{@link #hashBucket} the runtime uses --
     * RDD_EXT_13 requires these stay bit-for-bit identical between training and runtime.
     */
    public static final int HASH_BUCKETS = 1024;

    /**
     * Per-comment token cap (truncate/pad), per the finalized architecture. Public for the same
     * cross-package reason as {@link #HASH_BUCKETS} -- the trainer must cap sequences the same
     * way the runtime does.
     */
    public static final int SEQUENCE_CAP = 64;

    /**
     * Fixed softmax output class order this codebase uses -- an encoding convention (like
     * {@link #HASH_BUCKETS}'s hash choice), not one of STATE_NEXT_AI.md's open items. Whatever
     * training pipeline produces the weights file must emit its 3-way softmax output in this
     * same order, since {@link #decide} maps output index -> class positionally.
     */
    public static final CommentDecision[] CLASS_ORDER = { CommentDecision.YES, CommentDecision.NO, CommentDecision.ABSTAIN };

    private final GruWeights weights;
    private final Vocabulary vocabulary;

    private GruClassifier(GruWeights weights)
    {
        this.weights    = weights;
        this.vocabulary = weights.hasTrainedWeights()
                ? new Vocabulary( java.util.Arrays.asList(weights.explicitVocab) )
                : null;
    }

    /**
     * Loads a trained weights file and returns a ready-to-use classifier. Per the fail-safe
     * posture documented in STATE_NEXT_AI.md, a missing or unreadable weights file must make
     * the caller behave as {@link CommentDecision#ABSTAIN} for every comment -- callers should
     * catch {@link IOException} here and fall back accordingly rather than aborting formatting;
     * this method itself only reports the failure, it doesn't apply the fallback.
     */
    public static GruClassifier load(Path weightsFile) throws IOException
    {
        return new GruClassifier( GruWeights.load(weightsFile) );
    }

    /**
     * Classifies a single comment's ambiguous target word in context, returning the same
     * {@code YES}/{@code NO}/{@code ABSTAIN} classes as the existing rule-based classifier
     * (RDD_EXT_10 -- no more granular intermediate class). Abstains when the top softmax class
     * doesn't clear {@link GruWeights#abstainThreshold} (RDD_EXT_11), same posture as the
     * missing-weights-file/untrained-weights fail-safe.
     */
    public CommentDecision classify(String commentText, int targetWordIndex)
    {
        double[] probabilities = probabilities(commentText, targetWordIndex);
        if(probabilities == null) return CommentDecision.ABSTAIN;

        return decide(probabilities, weights.abstainThreshold);
    }

    /** The trained weights file's own {@code abstainThreshold} -- {@link #classify}'s default */
    public double abstainThreshold()
    {
        return weights.abstainThreshold;
    }

    /**
     * Returns the raw softmax class-probability distribution for a comment's target word, or
     * {@code null} if classification cannot proceed at all (untrained weights, or an
     * out-of-range target index) -- the same fail-safe conditions {@link #classify} treats as an
     * unconditional ABSTAIN before even reaching {@link #decide}. Exposed so eval/tuning tooling
     * (e.g. a --threshold sweep) can try multiple {@code abstainThreshold} values against the
     * same forward pass without recomputing it or retraining -- {@link #classify} itself still
     * only ever uses {@link GruWeights#abstainThreshold}.
     */
    public double[] probabilities(String commentText, int targetWordIndex)
    {
        List<String> tokens = tokenize(commentText);
        if( !weights.hasTrainedWeights() ) return null;
        if( tokens.size() > SEQUENCE_CAP ) tokens = tokens.subList(0, SEQUENCE_CAP);
        if( targetWordIndex < 0 || targetWordIndex >= tokens.size() ) return null;
        ForwardCache cache = forward(weights, vocabulary, tokens, targetWordIndex);

        return softmax(cache.logits);
    }

    /**
     * Word-level tokenization per RDD_EXT_12: trailing/attached punctuation splits into its own
     * token ({@code matrix.} -> {@code matrix} + {@code .}), consistent with the existing
     * rule-based classifier's own dot-count reasoning. camelCase/snake_case identifiers stay
     * whole -- not sub-tokenized, since the classification signal comes from surrounding context
     * words, not from decomposing the identifier itself.
     */
    public static List<String> tokenize(String commentText)
    {
        List<String> tokens = new ArrayList<>();
        int          i      = 0;
        int          n      = commentText.length();
        while(i < n) {
            char c = commentText.charAt(i);
            if( Character.isWhitespace(c) ) {
                ++i;
                continue;
            }
            if( isWordChar(c) ) {
                int start = i;
                while( i < n && isWordChar( commentText.charAt(i) ) ) i++;
                tokens.add( commentText.substring(start, i) );
            }
            else {
                tokens.add( String.valueOf(c) );
                ++i;
            }
        } // while

        return tokens;
    }

    /**
     * Numerically-stable softmax: converts raw class scores (logits) into a probability
     * distribution that sums to 1. Subtracts the max logit before exponentiating to avoid
     * overflow -- this doesn't change the result ({@code softmax(x) == softmax(x - c)} for any
     * constant {@code c}), only its numerical stability for large logit magnitudes.
     */
    public static double[] softmax(double[] logits)
    {
        if(logits.length == 0) return new double[0];
        double max = logits[0];
        for(double v : logits) {
            if(v > max) max = v;
        }
        double[] exp = new double[logits.length];
        double   sum = 0.0;
        for(int i = 0; i < logits.length; ++i) {
            exp[i]  = Math.exp( logits[i] - max );
            sum    += exp[i];
        }
        for(int i = 0; i < exp.length; ++i) exp[i] /= sum;

        return exp;
    }

    /**
     * Maps a softmax probability distribution to a {@link CommentDecision} per RDD_EXT_11: the
     * top class must clear {@code abstainThreshold} (not just be the argmax) to be returned as
     * that class; otherwise this abstains, same posture as the missing-weights-file fail-safe.
     * {@code probabilities[i]} corresponds to {@link #CLASS_ORDER}{@code [i]} -- callers must
     * pass a distribution produced in that same class order.
     */
    public static CommentDecision decide(double[] probabilities, double abstainThreshold)
    {
        if(probabilities.length != CLASS_ORDER.length) throw new IllegalArgumentException(
            "expected " + CLASS_ORDER.length + " probabilities (one per CLASS_ORDER entry), got " + probabilities.length
        );
        int argmax = 0;
        for(int i = 1; i < probabilities.length; ++i) {
            if( probabilities[i] > probabilities[argmax] ) argmax = i;
        }
        if( probabilities[argmax] > abstainThreshold ) return CLASS_ORDER[argmax];

        return CommentDecision.ABSTAIN;
    }

    private static boolean isWordChar(char c)
    {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * FNV-1a (32-bit) hash mod {@link #HASH_BUCKETS}, per RDD_EXT_13. Must stay bit-for-bit
     * identical between the training side and this runtime side.
     */
    public static int hashBucket(String token)
    {
        int    hash  = 0x811C9DC5;
        byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
        for(byte b : bytes) {
            hash ^= (b & 0xFF);
            hash *= 0x01000193;
        }

        return Math.floorMod(hash, HASH_BUCKETS);
    }

    // ── Forward pass + cached activations (shared by inference and training) ────────────────────

    /**
     * Every intermediate activation the forward pass produces, kept around so {@link #backward}
     * can backpropagate through it without recomputing. Plain data holder (flat public fields,
     * no getters), consistent with {@link GruWeights}'s own style. Only populated across the
     * ranges actually computed: forward-direction state for token indices {@code [0, targetIndex]},
     * backward-direction state for {@code [targetIndex, tokens.size())} -- per the recurrence's
     * causality, hidden state at any other index can't affect the target position's output, so
     * computing (and later backpropagating through) it would be wasted work.
     */
    public static final class ForwardCache {

        public final List<String>                    tokens;
        public final int                             targetIndex;
        public final int[]                           tokenRow;
        public final double[][]                      x;
        public final double[][] fZ, fR, fHTilde, fH, fRH;
        public final double[][] bZ, bR, bHTilde, bH, bRH;
        public final double[]                        denseInput;
        public final double[]                        densePre;
        public final double[]                        denseHidden;
        public final double[]                        logits;

        ForwardCache(
            List<String> tokens,
            int          targetIndex,
            int[]        tokenRow,
            double[][]   x,
            double[][]   fZ,
            double[][]   fR,
            double[][]   fHTilde,
            double[][]   fH,
            double[][]   fRH,
            double[][]   bZ,
            double[][]   bR,
            double[][]   bHTilde,
            double[][]   bH,
            double[][]   bRH,
            double[]     denseInput,
            double[]     densePre,
            double[]     denseHidden,
            double[]     logits
        )
        {
            this.tokens      = tokens;
            this.targetIndex = targetIndex;
            this.tokenRow    = tokenRow;
            this.x           = x;
            this.fZ          = fZ;
            this.fR          = fR;
            this.fHTilde     = fHTilde;
            this.fH          = fH;
            this.fRH         = fRH;
            this.bZ          = bZ;
            this.bR          = bR;
            this.bHTilde     = bHTilde;
            this.bH          = bH;
            this.bRH         = bRH;
            this.denseInput  = denseInput;
            this.densePre    = densePre;
            this.denseHidden = denseHidden;
            this.logits      = logits;
        }

    } // class ForwardCache

    /**
     * Runs the bidirectional-GRU + dense-head forward pass for one comment's tokens, targeting
     * {@code targetIndex} (per the finalized architecture: classification indexes into the
     * target word's own biGRU output, concat forward+backward, no marker token)
     */
    public static ForwardCache forward(
        GruWeights   weights,
        Vocabulary   vocabulary,
        List<String> tokens,
        int          targetIndex
    )
    {
        int t = tokens.size();
        int e = weights.embeddingDim;
        int h = weights.hiddenSize;

        double[][] x        = new double[t][];
        int[]      tokenRow = new int[t];
        for(int i = 0; i < t; ++i) {
            tokenRow[i] = vocabulary.lookup( tokens.get(i) );
            x[i]        = weights.embeddings[ tokenRow[i] ];
        }

        // z/r/hTilde are computed by gateInto -- a single fused, flat, straight-line loop per gate
        // (dot-product-with-x, dot-product-with-h, +bias, activation, all in one pass over the
        // hidden dimension) replacing the previous matVecInto+matVecInto+addVecInto+sigmoidVec/
        // tanhVec four-call chain. Same operation order per output element (Wx-row-dot-product,
        // then Uh-row-dot-product, then +bias, then activation) as the code it replaces, so results
        // are bit-identical -- purely fewer intermediate array allocations/passes, not a
        // reassociation. rh (r-hadamard-hPrev/hNext) is still materialized separately since it
        // feeds into a *different* matrix (Wh/Uh) than r itself.

        double[][] fZ = new double[t][], fR = new double[t][], fHTilde = new double[t][], fH = new double[t][], fRH = new double[t][];
        runGruDirection(weights.forward, x, h, 0, targetIndex, 1, fZ, fR, fHTilde, fRH, fH);

        double[][] bZ = new double[t][], bR = new double[t][], bHTilde = new double[t][], bH = new double[t][], bRH = new double[t][];
        runGruDirection(weights.backward, x, h, t - 1, targetIndex, -1, bZ, bR, bHTilde, bRH, bH);

        double[] denseInput = new double[2* h];
        System.arraycopy( fH[targetIndex], 0, denseInput, 0, h );
        System.arraycopy( bH[targetIndex], 0, denseInput, h, h );

        double[] densePre    = addVec( matVec(weights.denseW, denseInput), weights.denseB );
        double[] denseHidden = new double[densePre.length];
        for(int i = 0; i < densePre.length; ++i) denseHidden[i] = Math.max( 0.0, densePre[i] );
        double[] logits = addVec( matVec(weights.outW, denseHidden), weights.outB );

        return new ForwardCache(
            tokens, targetIndex, tokenRow, x, fZ, fR, fHTilde, fH, fRH,
            bZ, bR, bHTilde, bH, bRH, denseInput, densePre, denseHidden, logits
        );
    }

    /**
     * Runs one direction's (forward or backward) per-token GRU recurrence from {@code startIdx} to
     * {@code endIdx} (inclusive) stepping by {@code step}, writing each token's gate/hidden-state
     * arrays into the caller-supplied {@code outZ}/{@code outR}/{@code outHTilde}/{@code outRH}/
     * {@code outH} at that token's own index -- the direction-agnostic half of {@link #forward},
     * mirroring how {@link #backpropDirection} already factors the same forward/backward symmetry
     * for backprop. Same per-token operation order as before extraction, so results stay
     * bit-identical.
     */
    private static void runGruDirection(
        GruWeights.DirectionWeights dirWeights,
        double[][]                  x,
        int                         h,
        int                         startIdx,
        int                         endIdx,
        int                         step,
        double[][]                  outZ,
        double[][]                  outR,
        double[][]                  outHTilde,
        double[][]                  outRH,
        double[][]                  outH
    )
    {
        double[] hPrev = new double[h];
        for(int i = startIdx; step > 0 ? i <= endIdx : i >= endIdx; i += step) {
            double[] z = new double[h];
            gateInto( dirWeights.Wz, x[i], dirWeights.Uz, hPrev, dirWeights.bz, z, true );
            double[] r = new double[h];
            gateInto( dirWeights.Wr, x[i], dirWeights.Ur, hPrev, dirWeights.br, r, true );
            double[] rh     = hadamard(r, hPrev);
            double[] hTilde = new double[h];
            gateInto( dirWeights.Wh, x[i], dirWeights.Uh, rh, dirWeights.bh, hTilde, false );
            double[] hNew = new double[h];
            for(int k = 0; k < h; ++k) hNew[k] = ( 1 - z[k] ) * hPrev[k] + z[k] * hTilde[k];
            outZ[i]      = z;
            outR[i]      = r;
            outHTilde[i] = hTilde;
            outRH[i]     = rh;
            outH[i]      = hNew;
            hPrev        = hNew;
        } // for
    }

    /**
     * Accumulated gradients, one field per {@link GruWeights} trained-weight field, plus a sparse
     * per-row embedding gradient (most embedding rows are untouched by any single example, so a
     * map avoids allocating a full-size dense gradient table per example). Mutable accumulator,
     * built fresh per example by {@link #backward} -- callers (the trainer) apply it to their own
     * running weights via their own optimizer (Adam), then discard it.
     */
    public static final class Gradients {

        public final Map<Integer, double[]> embeddingGrad = new HashMap<>();
        public final GruWeights.DirectionWeights forward;
        public final GruWeights.DirectionWeights backward;
        public final double[][] denseW;
        public final double[]   denseB;
        public final double[][] outW;
        public final double[]   outB;

        Gradients(int hiddenSize, int embeddingDim, int denseSize, int numClasses)
        {
            forward  = GruWeights.DirectionWeights.zeros(hiddenSize, embeddingDim);
            backward = GruWeights.DirectionWeights.zeros(hiddenSize, embeddingDim);
            denseW   = new double[denseSize][2 * hiddenSize];
            denseB   = new double[denseSize];
            outW     = new double[numClasses][denseSize];
            outB     = new double[numClasses];
        }

        private void addEmbeddingGrad(int row, double[] delta)
        {
            double[] existing = embeddingGrad.get(row);
            if(existing == null) {
                embeddingGrad.put( row, delta.clone() );
            }
            else {
                for(int i = 0; i < existing.length; ++i) existing[i] += delta[i];
            }
        }

    } // class Gradients

    /**
     * Backpropagates the cross-entropy loss for {@code trueClassIndex} (an index into
     * {@link #CLASS_ORDER}) through {@code cache}, returning per-parameter gradients. Standard
     * GRU backprop-through-time equations, run only across the ranges {@link #forward} actually
     * computed (see {@link ForwardCache}'s javadoc on why that's sufficient).
     */
    public static Gradients backward(GruWeights weights, ForwardCache cache, int trueClassIndex)
    {
        int       h         = weights.hiddenSize;
        int       denseSize = weights.denseW.length;
        Gradients grad      = new Gradients(h, weights.embeddingDim, denseSize, weights.numClasses);

        double[] probabilities = softmax(cache.logits);
        double[] dLogits       = probabilities.clone();
        dLogits[trueClassIndex] -= 1.0;

        for(int c = 0; c < weights.numClasses; ++c) {
            for(int j = 0; j < denseSize; ++j) grad.outW[c][j] += dLogits[c] * cache.denseHidden[j];
            grad.outB[c] += dLogits[c];
        }
        double[] dDenseHidden = matTVec(weights.outW, dLogits, denseSize);
        double[] dDensePre    = new double[denseSize];
        for(int j = 0; j < denseSize; ++j) dDensePre[j] = cache.densePre[j] > 0 ? dDenseHidden[j] : 0.0;
        for(int j = 0; j < denseSize; ++j) {
            for(int k = 0; k < cache.denseInput.length; ++k) grad.denseW[j][k] += dDensePre[j] * cache.denseInput[k];
            grad.denseB[j] += dDensePre[j];
        }
        double[] dDenseInput = matTVec(weights.denseW, dDensePre, cache.denseInput.length);
        double[] dhFwdTarget = new double[h];
        double[] dhBwdTarget = new double[h];
        System.arraycopy(dDenseInput, 0, dhFwdTarget, 0, h);
        System.arraycopy(dDenseInput, h, dhBwdTarget, 0, h);

        backpropDirection(weights.forward, grad.forward, cache, grad, true, dhFwdTarget);
        backpropDirection(weights.backward, grad.backward, cache, grad, false, dhBwdTarget);

        return grad;
    }

    private static void backpropDirection(
        GruWeights.DirectionWeights weights,
        GruWeights.DirectionWeights gradWeights,
        ForwardCache                cache,
        Gradients                   grad,
        boolean                     isForward,
        double[]                    dhAtTarget
    )
    {
        int      h  = weights.bz.length;
        double[] dh = dhAtTarget;

        int start = isForward ? cache.targetIndex : cache.targetIndex;
        int end   = isForward ? 0 : cache.tokens.size() - 1;
        int step  = isForward ? -1 : 1;

        for(int t = start; isForward ? t >= end : t <= end; t += step) {
            double[] z      = isForward ? cache.fZ[t] : cache.bZ[t];
            double[] r      = isForward ? cache.fR[t] : cache.bR[t];
            double[] hTilde = isForward ? cache.fHTilde[t] : cache.bHTilde[t];
            double[] hPrev  = prevHidden(cache, t, isForward);
            double[] x      = cache.x[t];

            double[] dz           = new double[h];
            double[] dhTilde      = new double[h];
            double[] dhPrevDirect = new double[h];
            for(int k = 0; k < h; ++k) {
                dz[k]           = dh[k] * ( hTilde[k] - hPrev[k] );
                dhTilde[k]      = dh[k] * z[k];
                dhPrevDirect[k] = dh[k] * ( 1 - z[k] );
            }

            double[] dAh = new double[h];
            double[] dAz = new double[h];
            for(int k = 0; k < h; ++k) {
                dAh[k] = dhTilde[k] * ( 1 - hTilde[k] * hTilde[k] );
                dAz[k] = dz[k] * z[k] * ( 1 - z[k] );
            }

            double[] dRh        = matTVec(weights.Uh, dAh, h);
            double[] dr         = new double[h];
            double[] dhPrevViaR = new double[h];
            for(int k = 0; k < h; ++k) {
                dr[k]         = dRh[k] * hPrev[k];
                dhPrevViaR[k] = dRh[k] * r[k];
            }
            double[] dAr = new double[h];
            for(int k = 0; k < h; ++k) dAr[k] = dr[k] * r[k] * ( 1 - r[k] );

            for(int i = 0; i < h; ++i) {
                for(int j = 0; j < x.length; ++j) {
                    gradWeights.Wz[i][j] += dAz[i] * x[j];
                    gradWeights.Wr[i][j] += dAr[i] * x[j];
                    gradWeights.Wh[i][j] += dAh[i] * x[j];
                }
                for(int j = 0; j < h; ++j) {
                    gradWeights.Uz[i][j] += dAz[i] * hPrev[j];
                    gradWeights.Ur[i][j] += dAr[i] * hPrev[j];
                    double rh = r[j]* hPrev[j];
                    gradWeights.Uh[i][j] += dAh[i] * rh;
                }
                gradWeights.bz[i] += dAz[i];
                gradWeights.br[i] += dAr[i];
                gradWeights.bh[i] += dAh[i];
            } // for i

            double[] dx     = new double[x.length];
            double[] wzTdaz = matTVec(weights.Wz, dAz, x.length);
            double[] wrTdar = matTVec(weights.Wr, dAr, x.length);
            double[] whTdah = matTVec(weights.Wh, dAh, x.length);
            for(int j = 0; j < x.length; ++j) dx[j] = wzTdaz[j] + wrTdar[j] + whTdah[j];
            grad.addEmbeddingGrad( cache.tokenRow[t], dx );

            double[] uzTdaz = matTVec(weights.Uz, dAz, h);
            double[] urTdar = matTVec(weights.Ur, dAr, h);
            double[] dhPrev = new double[h];
            for(int k = 0; k < h; ++k) dhPrev[k] = dhPrevDirect[k] + dhPrevViaR[k] + uzTdaz[k] + urTdar[k];
            dh = dhPrev;
        } // for t
    }

    private static double[] prevHidden(ForwardCache cache, int t, boolean isForward)
    {
        int h = isForward ? cache.fH[0].length : cache.bH[ cache.tokens.size() - 1 ].length;
        if(isForward) return t == 0 ? new double[h] : cache.fH[t - 1];

        return t == cache.tokens.size() - 1 ? new double[h] : cache.bH[t + 1];
    }

    // ── Small linear-algebra helpers (dense, no external math library) ──────────────────────────

    private static double[] matVec(double[][] w, double[] x)
    {
        double[] y = new double[w.length];
        for(int i = 0; i < w.length; ++i) {
            double sum = 0.0;
            for(int j = 0; j < x.length; ++j) sum += w[i][j] * x[j];
            y[i] = sum;
        }

        return y;
    }

    /**
     * Fused GRU gate computation: {@code out[i] = activation( dot(W[i], x) + dot(U[i], hPrev) +
     * b[i] )} for every {@code i}, {@code activation} = sigmoid when {@code useSigmoid}, tanh
     * otherwise. Replaces the previous {@code matVecInto}+{@code matVecInto}+{@code addVecInto}+
     * {@code sigmoidVec}/{@code tanhVec} four-call chain with a single flat, straight-line,
     * non-aliased pass per output row (no intermediate {@code wx}/{@code uh}/pre-activation
     * arrays) -- same per-element operation order as the code it replaces (W-row dot product,
     * then U-row dot product, then +bias, then activation), so results are bit-identical, not a
     * reassociation. {@code out} must not alias {@code x} or {@code hPrev}.
     */
    private static void gateInto(
        double[][] w,
        double[]   x,
        double[][] u,
        double[]   hPrev,
        double[]   b,
        double[]   out,
        boolean    useSigmoid
    )
    {
        for(int i = 0; i < out.length; ++i) {
            double[] wRow = w[i];
            double   wx   = 0.0;
            for(int j = 0; j < x.length; ++j) wx += wRow[j] * x[j];
            double[] uRow = u[i];
            double   uh   = 0.0;
            for(int j = 0; j < hPrev.length; ++j) uh += uRow[j] * hPrev[j];
            double pre = wx + uh + b[i];
            out[i] = useSigmoid ? 1.0 / ( 1.0 + Math.exp(-pre) ) : Math.tanh(pre);
        } // for
    }

    /**
     * {@code W}-transpose times {@code v}: {@code result[j] = sum_i W[i][j] * v[i]}, where
     * {@code W} is {@code v.length} rows by {@code resultLength} columns.
     */
    private static double[] matTVec(double[][] w, double[] v, int resultLength)
    {
        double[] result = new double[resultLength];
        for(int i = 0; i < w.length; ++i) {
            for(int j = 0; j < resultLength; ++j) result[j] += w[i][j] * v[i];
        }

        return result;
    }

    private static double[] addVec(double[] a, double[] b)
    {
        double[] r = new double[a.length];
        for(int i = 0; i < a.length; ++i) r[i] = a[i] + b[i];

        return r;
    }

    private static double[] hadamard(double[] a, double[] b)
    {
        double[] r = new double[a.length];
        for(int i = 0; i < a.length; ++i) r[i] = a[i] * b[i];

        return r;
    }


} // class GruClassifier
