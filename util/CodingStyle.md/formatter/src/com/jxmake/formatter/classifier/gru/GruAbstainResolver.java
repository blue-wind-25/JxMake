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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

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

    /** Filename of the GRU weights file expected in the "program directory" (see
     *  {@link #programDirectory()}) when {@code gru-weights-path} is left at its default (empty)
     *  -- i.e. not explicitly configured. Matches the name the top-level distribution build
     *  (see {@code ../../../dist_build/jxmake_dist/apps/code-formatter/}) copies alongside the
     *  packaged jar, and that {@code make gru-train} also copies into {@code $(CLASS_DIR)} for
     *  dev/test runs. */
    public static final String WEIGHTS_FILENAME = "code-formatter-ai-assist-weights.json";

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
     *      <li>Otherwise, resolves the weights-file path (see {@link #resolveWeightsPath(Config)}
     *          -- {@code config.gruWeightsPath()} if explicitly set, else derived from the
     *          program directory) and attempts to {@link GruClassifier#load} it. A missing/
     *          unreadable/corrupt file, or an inability to even determine the program directory,
     *          is caught/treated as {@code ABSTAIN} (the same fail-safe posture documented on
     *          {@link GruClassifier#load}'s javadoc and in STATE_AI.md's "Fail-safe" note) --
     *          this never blocks formatting. On success, delegates to
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

        final Path weightsPath = resolveWeightsPath(config);
        if (weightsPath == null) {
            // Fail-safe: no explicit path configured and the program directory couldn't be
            // determined -- ABSTAIN, never blocks formatting.
            return CommentDecision.ABSTAIN;
        }
        final GruClassifier gru;
        try {
            gru = GruClassifier.load(weightsPath);
        } catch (final IOException e) {
            // Fail-safe: missing/unreadable/corrupt weights file -> ABSTAIN, never blocks
            // formatting.
            return CommentDecision.ABSTAIN;
        }
        return gru.classify(commentText, targetWordIndex);
    }

    /** Resolves the weights-file path to attempt loading: {@code config.gruWeightsPath()} if it
     *  is explicitly set (non-empty), else derived as {@code programDirectory()/WEIGHTS_FILENAME}
     *  when {@code gru-weights-path} is left at its default empty value. Returns {@code null} (a
     *  fail-safe "no path" result, handled by the caller as {@code ABSTAIN}) if no explicit path
     *  is configured and the program directory can't be determined either. */
    private static Path resolveWeightsPath(final Config config) {
        final String configured = config.gruWeightsPath();
        if (configured != null && !configured.trim().isEmpty()) {
            return Paths.get(configured);
        }
        final Path programDir = programDirectory();
        if (programDir == null) {
            return null;
        }
        return programDir.resolve(WEIGHTS_FILENAME);
    }

    /** Resolves the directory the running program lives in, so the GRU weights file can be found
     *  next to it without a hardcoded path: the jar's parent directory when run via {@code -jar}
     *  (packaged/distributed layout, e.g. {@code apps/code-formatter/} in the distribution tree),
     *  or the classes directory itself for a dev/test run against {@code $(CLASS_DIR)} (there is
     *  no jar to take a parent of in that case -- the classes directory already is the "program
     *  directory" analog for that mode). Returns {@code null} (fail-safe, treated as "can't
     *  resolve a default path" by {@link #resolveWeightsPath(Config)}) if the code source location
     *  is unavailable or malformed -- this is not expected in normal operation, but must never
     *  throw and block formatting. */
    private static Path programDirectory() {
        try {
            final CodeSource codeSource = GruAbstainResolver.class.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }
            final URL location = codeSource.getLocation();
            if (location == null) {
                return null;
            }
            final Path path = Paths.get(location.toURI());
            if (Files.isDirectory(path)) {
                return path;
            }
            return path.getParent();
        } catch (final URISyntaxException | RuntimeException e) {
            return null;
        }
    }
}
