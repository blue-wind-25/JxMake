/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.classifier.gru;

import com.jxmake.formatter.Config;
import com.jxmake.formatter.classifier.CommentClassifier;
import com.jxmake.formatter.classifier.CommentDecision;
import com.jxmake.formatter.classifier.CommentFeatureVector;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Integration point for the "Rules, then GRU on abstain" pipeline documented in STATE_AI.md's
 *  "GRU implementation design" ({@code Rules -> high confidence / abstain -> bidirectional GRU
 *  classifier -> final decision}). This is purely additive plumbing -- it does not change
 *  {@link CommentClassifier#classify}'s pure rule-based signature/contract (that contract is a
 *  hard architectural constraint per that class's own javadoc), and it is not wired into
 *  {@code MiscRuleCore} (a separate, out-of-scope follow-up task).
 *
 *  <p>Since {@link GruClassifier#classify} is currently a stub that unconditionally returns
 *  {@link CommentDecision#ABSTAIN} (see its javadoc), this resolver cannot yet produce any
 *  different final decision than calling {@link CommentClassifier#classify} alone would --
 *  matching STATE_AI.md's hard constraint that this work is purely additive, no existing
 *  Tier-1/Tier-2 rule behavior may change. Once a real trained weights file and forward pass
 *  land, this is the one call site that will start seeing the GRU actually resolve some abstains,
 *  with no further plumbing changes needed. */
public final class GruAbstainResolver {

    private GruAbstainResolver() {
    }

    /** Runs the full "Rules, then GRU on abstain" pipeline for one comment's target word, using
     *  {@link Config#isGruClassifier()}/{@link Config#gruWeightsPath()} to decide whether the GRU
     *  stage is even attempted.
     *
     *  <ol>
     *      <li>Calls {@link CommentClassifier#classify(CommentFeatureVector)}.</li>
     *      <li>If the result is not {@link CommentDecision#ABSTAIN}, returns it immediately -- the
     *          GRU stage is never consulted and no filesystem access happens.</li>
     *      <li>If the result is {@link CommentDecision#ABSTAIN} but {@code config.isGruClassifier()}
     *          is {@code false}, returns {@code ABSTAIN} immediately -- again, no filesystem
     *          access happens, since the feature is opt-in per STATE_AI.md.</li>
     *      <li>Otherwise, attempts to {@link GruClassifier#load} the weights file at
     *          {@code config.gruWeightsPath()}. A missing/unreadable file is caught as an
     *          {@link IOException} and treated as {@code ABSTAIN} (the same fail-safe posture
     *          documented on {@link GruClassifier#load}'s javadoc and in STATE_AI.md's "Fail-safe"
     *          note) -- this never blocks formatting. On success, delegates to
     *          {@link GruClassifier#classify(String, int)} and returns whatever
     *          {@link CommentDecision} results (currently always {@code ABSTAIN}, per that
     *          method's stub).</li>
     *  </ol>
     *
     * @param features the rule-based classifier's feature vector for this comment/target word
     * @param commentText the raw comment text (what {@link GruClassifier#classify} tokenizes)
     * @param targetWordIndex the target word's token index within {@code commentText}, per
     *      {@link GruClassifier#classify}'s contract
     * @param config resolved config, supplying the {@code gru-classifier}/{@code gru-weights-path}
     *      keys
     */
    public static CommentDecision resolve(final CommentFeatureVector features, final String commentText,
            final int targetWordIndex, final Config config) {
        final CommentDecision ruleResult = CommentClassifier.classify(features);
        if (ruleResult != CommentDecision.ABSTAIN) {
            return ruleResult;
        }
        if (!config.isGruClassifier()) {
            return CommentDecision.ABSTAIN;
        }

        final Path weightsPath = Paths.get(config.gruWeightsPath());
        final GruClassifier gru;
        try {
            gru = GruClassifier.load(weightsPath);
        } catch (final IOException e) {
            // Fail-safe: missing/unreadable weights file -> ABSTAIN, never blocks formatting.
            return CommentDecision.ABSTAIN;
        }
        return gru.classify(commentText, targetWordIndex);
    }
}
