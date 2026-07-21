/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Training entry point for the Step 3 GRU comment-classifier, per STATE_AI.md's "GRU
 *  implementation design". Deliberately lives outside {@code src/} -- the runtime JAR must never
 *  bundle training code or a training-only ML dependency; this writes a weights file for
 *  {@code com.jxmake.formatter.classifier.gru.GruClassifier}/{@code GruWeights} to read at
 *  runtime, it does not generate or overwrite any {@code .java} source.
 *
 *  <p><b>Current behavior:</b> writes a weights file containing only the flat scalar fields
 *  {@code GruWeights.load} currently knows how to parse -- {@code schemaVersion=1} and the five
 *  finalized architecture constants from STATE_AI.md's "Architecture" table
 *  ({@code hashBuckets}, {@code embeddingDim}, {@code hiddenSize}, {@code sequenceCap},
 *  {@code numClasses}), plus {@code abstainThreshold} (RDD_EXT_11's stated default, 0.5) and a
 *  {@code vocabSize} derived by counting the distinct whitespace-split tokens in the labeled-
 *  examples file (a natural, non-guessy count from real input, not an invented placeholder). This
 *  is legitimate to write now because it is exactly the schema {@code GruWeights.load} already
 *  parses -- it is NOT a trained model: no embedding table or GRU weight matrices are written,
 *  because {@link com.jxmake.formatter.classifier.gru.GruWeights} has no fields for them yet.
 *  The {@code --key=value} hyperparameters collected below are still not consumed by anything --
 *  there is no training loop to feed them to.
 *
 *  <p>The actual training loop (forward/backward pass, optimizer) remains blocked on
 *  STATE_AI.md's still-open items: hyperparameters (item 3), evaluation target (item 4), a
 *  measured ABSTAIN rate (item 9), and the licensing/provenance check for bulk-sourced comment
 *  data (item 10) -- none of those have been resolved yet, so this class does not guess at them;
 *  do not implement the training loop itself or hardcode specific hyperparameter names/defaults
 *  until those items are resolved per STATE_COMMON.md's ambiguity-handling protocol. */
public final class GruTrainer {

    private GruTrainer() {
    }

    private static final String USAGE =
            "Usage: GruTrainer <labeled-examples-path> <output-weights-path> [--key=value ...]";

    /** Parses and validates CLI args, then hands off to the (not yet implemented) training loop.
     *  Positional args: an existing, readable labeled-examples file, and an output-weights-file
     *  path whose parent directory must exist. Any further {@code --key=value} args are collected
     *  generically -- their names/meaning are exactly STATE_NEXT_AI.md's open item 3
     *  (hyperparameters), not decided here, so nothing about a specific key is validated or
     *  interpreted, only the {@code --key=value} shape itself. */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println(USAGE);
            System.exit(2);
        }

        File examplesFile = new File(args[0]);
        if (!examplesFile.isFile() || !examplesFile.canRead()) {
            System.err.println("GruTrainer: labeled-examples file not readable: " + args[0]);
            System.exit(2);
        }

        File weightsOut = new File(args[1]);
        File weightsOutParent = weightsOut.getAbsoluteFile().getParentFile();
        if (weightsOutParent == null || !weightsOutParent.isDirectory()) {
            System.err.println("GruTrainer: output-weights-path parent directory does not exist: "
                    + args[1]);
            System.exit(2);
        }

        Map<String, String> hyperparameters = new LinkedHashMap<>();
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--") || arg.indexOf('=') < 0) {
                System.err.println("GruTrainer: malformed hyperparameter arg (expected --key=value): "
                        + arg);
                System.err.println(USAGE);
                System.exit(2);
            }
            int eq = arg.indexOf('=');
            String key = arg.substring(2, eq);
            String value = arg.substring(eq + 1);
            if (key.isEmpty()) {
                System.err.println("GruTrainer: malformed hyperparameter arg (empty key): " + arg);
                System.exit(2);
            }
            hyperparameters.put(key, value);
        }

        // hyperparameters is intentionally unused past this point -- see class javadoc: there is
        // no training loop yet to feed it to (blocked on STATE_AI.md open items 3/4/9/10).

        final int vocabSize;
        try {
            vocabSize = countDistinctTokens(examplesFile);
        } catch (final IOException e) {
            System.err.println("GruTrainer: could not read labeled-examples file " + args[0] + ": "
                    + e.getMessage());
            System.exit(2);
            return;
        }

        final String json = "{\n"
                + "  \"schemaVersion\": " + com.jxmake.formatter.classifier.gru.GruWeights.CURRENT_SCHEMA_VERSION + ",\n"
                + "  \"vocabSize\": " + vocabSize + ",\n"
                + "  \"hashBuckets\": " + com.jxmake.formatter.classifier.gru.GruClassifier.HASH_BUCKETS + ",\n"
                + "  \"embeddingDim\": 16,\n"
                + "  \"hiddenSize\": 224,\n"
                // GruClassifier.SEQUENCE_CAP is package-private (not accessible from here, a
                // different package outside src/) -- 64 is that same finalized architecture
                // constant, per STATE_AI.md's Architecture table ("Sequence cap"), not a guess.
                + "  \"sequenceCap\": 64,\n"
                + "  \"numClasses\": " + com.jxmake.formatter.classifier.gru.GruClassifier.CLASS_ORDER.length + ",\n"
                + "  \"abstainThreshold\": 0.5\n"
                + "}\n";

        try {
            Files.write(weightsOut.toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            System.err.println("GruTrainer: could not write output weights file " + args[1] + ": "
                    + e.getMessage());
            System.exit(2);
            return;
        }

        System.out.println("GruTrainer: wrote placeholder/architecture-constants-only weights file to "
                + weightsOut + " (vocabSize=" + vocabSize + ", derived from " + examplesFile
                + "). No real trained numbers -- embedding table and GRU weight matrices are not yet "
                + "represented in the schema at all. The real training loop remains blocked on "
                + "STATE_AI.md open items 3/4/9/10 (hyperparameters, evaluation target, measured "
                + "ABSTAIN rate, licensing check). Collected hyperparameters (not yet consumed by "
                + "anything): " + hyperparameters);
    }

    /** Counts the number of distinct whitespace-separated tokens across the labeled-examples
     *  file. This is a natural, non-guessy stand-in for {@code vocabSize} derivable from real
     *  input -- NOT a claim about the eventual real vocabulary (that is Vocabulary.java's
     *  ~3.5k-word explicit vocab, still unseeded per STATE_AI.md), just an honest placeholder
     *  count from whatever labeled-examples file is actually passed in. */
    private static int countDistinctTokens(final File examplesFile) throws IOException {
        final Set<String> distinct = new LinkedHashSet<String>();
        for (final String line : Files.readAllLines(examplesFile.toPath(), StandardCharsets.UTF_8)) {
            for (final String token : line.trim().split("\\s+")) {
                if (!token.isEmpty()) {
                    distinct.add(token);
                }
            }
        }
        return Math.max(1, distinct.size());
    }
}
