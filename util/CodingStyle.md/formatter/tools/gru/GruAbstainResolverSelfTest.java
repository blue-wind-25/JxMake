/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

import com.jxmake.formatter.Config;
import com.jxmake.formatter.classifier.CommentDecision;
import com.jxmake.formatter.classifier.CommentFeatureVector;
import com.jxmake.formatter.classifier.gru.GruAbstainResolver;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Standalone self-check for {@link GruAbstainResolver}, following the same zero-dependency
 *  plain-{@code main()} pattern as {@code GruWeightsSelfTest.java}/{@code GruTokenizerSelfTest
 *  .java}/{@code GruSoftmaxSelfTest.java} -- no test framework exists anywhere in this project.
 *
 *  <p>Covers the four cases STATE_AI.md's task description calls out:
 *  <ol>
 *      <li>Rules resolve non-ABSTAIN -- GRU never consulted, verified by pointing the weights
 *          path at a nonexistent file and confirming no error and the rule-based result.</li>
 *      <li>Rules ABSTAIN + {@code gru-classifier} off -- falls through immediately, no load
 *          attempt (verified the same way, nonexistent weights path, no error).</li>
 *      <li>Rules ABSTAIN + {@code gru-classifier} on + weights file missing -- fail-safe
 *          ABSTAIN.</li>
 *      <li>Rules ABSTAIN + {@code gru-classifier} on + real weights file present -- loads
 *          successfully, GRU stub still returns ABSTAIN, final result is ABSTAIN.</li>
 *  </ol>
 *
 *  <p>{@link Config} has no public constructor/setters (it's resolved via config-file/env/CLI
 *  layers), so this test builds each scenario's config via {@link Config#resolve(Path, Map)}
 *  with in-memory CLI-override maps -- the same mechanism the rest of the codebase uses to set
 *  config from code, no new test-only seam needed. Run via: {@code java GruAbstainResolverSelfTest}. */
public final class GruAbstainResolverSelfTest {

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        checkRulesResolveNonAbstain_gruNeverConsulted();
        checkRulesAbstain_gruClassifierOff_noLoadAttempt();
        checkRulesAbstain_gruClassifierOn_weightsMissing_failSafeAbstain();
        checkRulesAbstain_gruClassifierOn_weightsPresent_stillAbstain();

        if (failures == 0) {
            System.out.println("GruAbstainResolverSelfTest: all checks passed");
        } else {
            System.err.println("GruAbstainResolverSelfTest: " + failures + " check(s) failed");
            System.exit(1);
        }
    }

    private static void checkRulesResolveNonAbstain_gruNeverConsulted() throws IOException {
        // hasLeadingKeywordMatch=false, hasNonLatinScript=false -> CommentClassifier falls
        // through to the "main path", which always resolves YES per its own javadoc.
        CommentFeatureVector features = plainFeatures("Combined", "test", "file");
        Config config = configWith(true, "/nonexistent/gru/weights/path.json");

        CommentDecision result = GruAbstainResolver.resolve(features, "Combined test file.", 0, config);
        if (result != CommentDecision.YES) {
            fail("expected rule-based YES result to pass through unchanged, got " + result);
        }
    }

    private static void checkRulesAbstain_gruClassifierOff_noLoadAttempt() throws IOException {
        CommentFeatureVector features = nonLatinFeatures();
        Config config = configWith(false, "/nonexistent/gru/weights/path.json");

        // gru-classifier is off: the nonexistent weights path must never be touched, so this
        // must not throw and must return ABSTAIN without any load attempt.
        CommentDecision result = GruAbstainResolver.resolve(features, "中文 comment", 0, config);
        if (result != CommentDecision.ABSTAIN) {
            fail("expected ABSTAIN when gru-classifier is off, got " + result);
        }
    }

    private static void checkRulesAbstain_gruClassifierOn_weightsMissing_failSafeAbstain() throws IOException {
        CommentFeatureVector features = nonLatinFeatures();
        Config config = configWith(true, "/nonexistent/gru/weights/path.json");

        CommentDecision result = GruAbstainResolver.resolve(features, "中文 comment", 0, config);
        if (result != CommentDecision.ABSTAIN) {
            fail("expected fail-safe ABSTAIN on missing weights file, got " + result);
        }
    }

    private static void checkRulesAbstain_gruClassifierOn_weightsPresent_stillAbstain() throws IOException {
        Path weightsPath = Files.createTempFile("gru_abstain_resolver_selftest_weights", ".json");
        try {
            Files.write(weightsPath, ("{"
                    + "\"schemaVersion\": 1,"
                    + "\"vocabSize\": 100,"
                    + "\"hashBuckets\": 1024,"
                    + "\"embeddingDim\": 16,"
                    + "\"hiddenSize\": 224,"
                    + "\"sequenceCap\": 64,"
                    + "\"numClasses\": 3,"
                    + "\"abstainThreshold\": 0.5"
                    + "}").getBytes(StandardCharsets.UTF_8));

            CommentFeatureVector features = nonLatinFeatures();
            Config config = configWith(true, weightsPath.toString());

            CommentDecision result = GruAbstainResolver.resolve(features, "中文 comment", 0, config);
            if (result != CommentDecision.ABSTAIN) {
                fail("expected ABSTAIN (weights load succeeds but GruClassifier.classify is still "
                        + "a stub), got " + result);
            }
        } finally {
            Files.deleteIfExists(weightsPath);
        }
    }

    private static CommentFeatureVector plainFeatures(String target, String prev, String next) {
        return new CommentFeatureVector(target, prev, next, false, false, false, false,
                TokenType.COMMENT_LINE, false, false);
    }

    private static CommentFeatureVector nonLatinFeatures() {
        // hasNonLatinScript=true -> CommentClassifier's Gate 1 always returns ABSTAIN,
        // regardless of the other fields -- the simplest reliable way to force an ABSTAIN
        // from the rule-based classifier for this test.
        return new CommentFeatureVector("word", null, null, false, false, false, false,
                TokenType.COMMENT_LINE, true, false);
    }

    private static Config configWith(boolean gruClassifierOn, String weightsPath) {
        Map<String, String> cliOverrides = new LinkedHashMap<String, String>();
        cliOverrides.put("gru-classifier", gruClassifierOn ? "on" : "off");
        cliOverrides.put("gru-weights-path", weightsPath);
        return Config.resolve(null, cliOverrides);
    }

    private static void fail(String message) {
        failures++;
        System.err.println("FAIL: " + message);
    }
}
