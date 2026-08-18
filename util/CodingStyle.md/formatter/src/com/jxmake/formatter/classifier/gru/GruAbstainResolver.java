/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.classifier.gru;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.jxmake.formatter.Config;
import com.jxmake.formatter.classifier.CommentClassifier;
import com.jxmake.formatter.classifier.CommentDecision;
import com.jxmake.formatter.classifier.CommentFeatureVector;

/**
 * Integration point for the "Rules, then GRU on abstain" pipeline documented in STATE_AI.md's
 *  "GRU implementation design" ({@code Rules -> high confidence / abstain -> bidirectional GRU
 *  classifier -> final decision}). This is purely additive plumbing -- it does not change
 *  {@link CommentClassifier#classify}'s pure rule-based signature/contract (that contract is a
 *  hard architectural constraint per that class's own javadoc).
 *
 *  <p><b>Wired into {@code MiscRuleCore.classifyComment}</b> (both its capitalize-first-letter and
 *  strip-trailing-period funnel points), gated behind the same {@code gru-classifier}/
 *  {@code gru-weights-path} config keys this class already read before the wiring landed. If no
 *  weights file is deployed ({@link GruClassifier#classify} fails safe per its own javadoc), this
 *  resolver produces exactly the same result {@link CommentClassifier#classify} alone would --
 *  matching STATE_AI.md's hard constraint that this work is purely additive, no existing
 *  Tier-1/Tier-2 rule behavior may change when the feature is off or the weights file is absent.
 */
public final class GruAbstainResolver {

    /**
     * Cache of loaded {@link GruClassifier} instances keyed by resolved weights-file path, so the
     *  weights JSON is parsed at most once per distinct path for the lifetime of the process --
     *  {@link #resolve} previously called {@link GruClassifier#load} on every single ABSTAIN
     *  comment, re-reading and re-parsing the same file over and over in both multi-file batch runs
     *  and server mode. An absent {@link Optional} value (from a failed load) is cached too, so a
     *  missing/corrupt weights file is only attempted once, not retried per comment -- consistent
     *  with RDD_EXT_9's "skip for process lifetime after first failure" fail-safe pattern used
     *  elsewhere in this codebase (see STATE_AI.md). {@link ConcurrentHashMap#computeIfAbsent} makes the single
     *  load thread-safe without a separate lock, so a single shared {@link GruClassifier} per
     *  weights path is also safe to hand out to concurrent server-mode requests.
     */
    private static final ConcurrentHashMap<Path, Optional<GruClassifier>> CLASSIFIER_CACHE = new ConcurrentHashMap<>();

    /**
     * Filename of the GRU weights file expected in the "program directory" (see
     *  {@link #programDirectory()}) when {@code gru-weights-path} is left at its default (empty)
     *  -- i.e. not explicitly configured. Matches the name the top-level distribution build
     *  (see {@code ../../../dist_build/jxmake_dist/apps/code-formatter/}) copies alongside the
     *  packaged jar, and that {@code make gru-train} also copies into {@code $(CLASS_DIR)} for
     *  dev/test runs.
     */
    public static final String WEIGHTS_FILENAME = "code-formatter-ai-assist-weights.json";

    private GruAbstainResolver()
    {
    }

    /**
     * Runs the full "Rules, then GRU on abstain" pipeline for one comment's target word, using
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
    public static CommentDecision resolve(
        final CommentFeatureVector features,
        final String               commentText,
        final int                  targetWordIndex,
        final Config               config
    )
    {
        return resolve(
            features,
            commentText,
            targetWordIndex,
            config.isGruClassifier(),
            config.gruWeightsPath()
        );
    }

    /**
     * {@code Config}-free overload of {@link #resolve(CommentFeatureVector, String, int, Config)} for
     *  callers (e.g. {@code MiscRuleCore}) that don't hold a full {@link Config} instance -- takes the
     *  same two config values ({@code gru-classifier}, {@code gru-weights-path}) directly. Identical
     *  behavior/fail-safe posture to the {@code Config}-based overload, which now just forwards here.
     */
    public static CommentDecision resolve(
        final CommentFeatureVector features,
        final String               commentText,
        final int                  targetWordIndex,
        final boolean              gruClassifierEnabled,
        final String               gruWeightsPathConfig
    )
    {
        final CommentDecision ruleResult = CommentClassifier.classify(features);
        if(ruleResult != CommentDecision.ABSTAIN) return ruleResult;
        if(!gruClassifierEnabled) return CommentDecision.ABSTAIN;

        final Path weightsPath = resolveWeightsPath(gruWeightsPathConfig);
        if(weightsPath == null) {
            // Fail-safe: no explicit path configured and the program directory couldn't be
            // determined -- ABSTAIN, never blocks formatting
            return CommentDecision.ABSTAIN;
        }
        final GruClassifier gru = loadCached(weightsPath).orElse(null);
        if(gru == null) {
            // Fail-safe: missing/unreadable/corrupt weights file -> ABSTAIN, never blocks
            // formatting
            return CommentDecision.ABSTAIN;
        }

        return gru.classify(commentText, targetWordIndex);
    }

    /**
     * Returns the cached {@link GruClassifier} for {@code weightsPath}, loading and caching it on
     *  first use ({@link #CLASSIFIER_CACHE}) -- at most one {@link GruClassifier#load} call (and
     *  thus one weights-file read/parse) per distinct path for the process's lifetime, shared
     *  safely across every caller including concurrent server-mode requests. An empty
     *  {@link Optional} means loading failed (and is cached too, so it isn't retried) --
     *  {@link IOException} is the only checked exception {@link GruClassifier#load} declares, so it
     *  is caught here and folded into that empty result.
     */
    private static Optional<GruClassifier> loadCached(final Path weightsPath)
    {
        return CLASSIFIER_CACHE.computeIfAbsent(
            weightsPath,
            path-> {
                try {
                    return Optional.of( GruClassifier.load(path) );
                }
                catch(final IOException e) {
                    return Optional.empty();
                }
            }
        );
    }

    /**
     * Resolves the weights-file path to attempt loading: {@code gruWeightsPathConfig} if it is
     *  explicitly set (non-empty), else derived as {@code programDirectory()/WEIGHTS_FILENAME} when
     *  {@code gru-weights-path} is left at its default empty value. Returns {@code null} (a
     *  fail-safe "no path" result, handled by the caller as {@code ABSTAIN}) if no explicit path
     *  is configured and the program directory can't be determined either.
     */
    private static Path resolveWeightsPath(final String gruWeightsPathConfig)
    {
        if( gruWeightsPathConfig != null && !gruWeightsPathConfig.trim().isEmpty() ) return Paths.get(
            gruWeightsPathConfig
        );
        final Path programDir = programDirectory();
        if(programDir == null) return null;

        return programDir.resolve(WEIGHTS_FILENAME);
    }

    /**
     * Resolves the directory the running program lives in, so the GRU weights file can be found
     *  next to it without a hardcoded path: the jar's parent directory when run via {@code -jar}
     *  (packaged/distributed layout, e.g. {@code apps/code-formatter/} in the distribution tree),
     *  or the classes directory itself for a dev/test run against {@code $(CLASS_DIR)} (there is
     *  no jar to take a parent of in that case -- the classes directory already is the "program
     *  directory" analog for that mode). Returns {@code null} (fail-safe, treated as "can't
     *  resolve a default path" by {@link #resolveWeightsPath(Config)}) if the code source location
     *  is unavailable or malformed -- this is not expected in normal operation, but must never
     *  throw and block formatting.
     */
    private static Path programDirectory()
    {
        try {
            final CodeSource codeSource = GruAbstainResolver.class.getProtectionDomain().getCodeSource();
            if(codeSource == null) return null;
            final URL location = codeSource.getLocation();
            if(location == null) return null;
            final Path path = Paths.get( location.toURI() );
            if( Files.isDirectory(path) ) return path;
            return path.getParent();
        }
        catch(final URISyntaxException | RuntimeException e) {
            return null;
        }
    }

} // class GruAbstainResolver
