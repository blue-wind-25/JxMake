/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

import com.jxmake.formatter.classifier.gru.GruClassifier;
import com.jxmake.formatter.classifier.gru.GruWeights;
import com.jxmake.formatter.classifier.gru.Vocabulary;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Training entry point for the Step 3 GRU comment-classifier, per STATE_AI.md's "GRU
 *  implementation design". Deliberately lives outside {@code src/} -- the runtime JAR must never
 *  bundle training code; this writes a weights file for
 *  {@code com.jxmake.formatter.classifier.gru.GruClassifier}/{@code GruWeights} to read at
 *  runtime, it does not generate or overwrite any {@code .java} source.
 *
 *  <p><b>Current behavior:</b> a real training loop -- random (Xavier/Glorot-style) weight
 *  initialization, per-example forward pass via {@link GruClassifier#forward}, backprop-through-
 *  time via {@link GruClassifier#backward}, and an Adam optimizer step applied immediately per
 *  example (batch size 1, a deliberate simplification of RDD_EXT_18's batch-size-32 starting
 *  default -- revisit once real production training runs show it matters; per-example updates are
 *  simpler to implement correctly and are still a valid, if noisier, SGD variant). Runs for up to
 *  {@code --epochs} (default from RDD_EXT_18: 30) with early stopping on a held-out validation
 *  split's cross-entropy loss (patience-based: stop once validation loss hasn't improved for
 *  {@code --patience} epochs, default 5).
 *
 *  <p>Prints a start-of-run summary line, a mid-epoch progress line every {@code --progress-every}
 *  training examples (default 1000, 0 disables -- added 2026-07-29 once a large, 90k+-example
 *  auto-labeled corpus made a once-per-epoch-only log impractical to watch live), and a per-epoch
 *  summary line (loss + wall-clock timing). All via plain {@code System.out.println}, which
 *  auto-flushes on newline.
 *
 *  <p>Labeled-examples file format (RDD_EXT_20/RDD_EXT_21): one example per line, tab-separated:
 *  {@code <lang>\t<label:YES|NO>\t<targetWordIndex>\t<escaped-comment-text>}. {@code label} is
 *  binary ground truth (ABSTAIN is the GRU's own below-threshold runtime behavior, never a
 *  training class, per RDD_EXT_20). {@code targetWordIndex} is the 0-based index, after
 *  {@link GruClassifier#tokenize}, of the ambiguous target word the label is about -- the leading
 *  keyword for Pool A (keyword-ambiguity) examples, the last token for Pool B (period-ambiguity)
 *  examples, per RDD_EXT_21. The comment text has literal newlines/tabs escaped as {@code \n}/
 *  {@code \t}, mirroring {@code extract_comments.py}'s own escaping.
 *
 *  <p>The explicit vocab is loaded from {@code tools/gru/explicit_vocab.txt} by default (override
 *  with {@code --vocab=<path>}) -- see that file's own header and RDD_EXT_22 in STATE_AI.md. It is
 *  a permanent, checked-in, one-time-curated resource (every supported/planned language's
 *  keywords, plus ~3.3k common words frequency-derived from a real extracted-comments corpus): its
 *  row order fixes every trained weights file's embedding-row layout, so it must never be
 *  reordered/shrunk once anything has been trained against it. If the default file is missing (or
 *  {@code --vocab=} is explicitly empty), falls back to the old behavior of deriving a vocab from
 *  every distinct token seen in the labeled-examples file itself (order of first appearance) --
 *  useful only for quick local smoke tests against tiny placeholder data. */
public final class GruTrainer {

    private GruTrainer() {
    }

    private static final String USAGE =
            "Usage: GruTrainer <labeled-examples-path> <output-weights-path> [--key=value ...]";

    private static final int EMBEDDING_DIM = 16;
    private static final int HIDDEN_SIZE = 224;
    private static final int DENSE_SIZE = 64;
    private static final double ABSTAIN_THRESHOLD = 0.5;
    /** Global L2-norm gradient clipping threshold. Prevents exploding gradients in the
     *  recurrent layers while leaving ordinary updates unchanged. A value around 5 is a
     *  conventional starting point for GRU/LSTM training and can later become a
     *  --clip=<value> hyperparameter if needed. */
    private static final double GRADIENT_CLIP_NORM = 5.0;
    private static final String DEFAULT_VOCAB_PATH = "tools/gru/explicit_vocab.txt";

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        File examplesFile = new File(args[0]);
        if (!examplesFile.isFile() || !examplesFile.canRead()) {
            System.err.println("GruTrainer: labeled-examples file not readable: " + args[0]);
            System.exit(2);
            return;
        }

        File weightsOut = new File(args[1]);
        File weightsOutParent = weightsOut.getAbsoluteFile().getParentFile();
        if (weightsOutParent == null || !weightsOutParent.isDirectory()) {
            System.err.println("GruTrainer: output-weights-path parent directory does not exist: "
                    + args[1]);
            System.exit(2);
            return;
        }

        Map<String, String> hyperparameters = new LinkedHashMap<>();
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--") || arg.indexOf('=') < 0) {
                System.err.println("GruTrainer: malformed hyperparameter arg (expected --key=value): "
                        + arg);
                System.err.println(USAGE);
                System.exit(2);
                return;
            }
            int eq = arg.indexOf('=');
            String key = arg.substring(2, eq);
            String value = arg.substring(eq + 1);
            if (key.isEmpty()) {
                System.err.println("GruTrainer: malformed hyperparameter arg (empty key): " + arg);
                System.exit(2);
                return;
            }
            hyperparameters.put(key, value);
        }

        // RDD_EXT_18 starting defaults, overridable via --key=value.
        double learningRate = Double.parseDouble(hyperparameters.getOrDefault("lr", "0.001"));
        int maxEpochs = Integer.parseInt(hyperparameters.getOrDefault("epochs", "30"));
        int patience = Integer.parseInt(hyperparameters.getOrDefault("patience", "5"));
        long seed = Long.parseLong(hyperparameters.getOrDefault("seed", "42"));
        // Progress reporting (2026-07-29): large auto-labeled corpora (100k+ examples) take long
        // enough per epoch that a human watching the process needs mid-epoch feedback, not just a
        // once-per-epoch line. --progress-every=N prints a running line every N training examples
        // within the epoch (0 disables); default chosen so it fires a handful of times per epoch on
        // a ~100k-example corpus without flooding the console on small corpora.
        int progressEvery = Integer.parseInt(hyperparameters.getOrDefault("progress-every", "1000"));

        List<Example> examples;
        try {
            examples = readExamples(examplesFile);
        } catch (IOException e) {
            System.err.println("GruTrainer: could not read labeled-examples file " + args[0] + ": "
                    + e.getMessage());
            System.exit(2);
            return;
        }
        if (examples.isEmpty()) {
            System.err.println("GruTrainer: labeled-examples file " + args[0] + " has no examples");
            System.exit(2);
            return;
        }

        String vocabPath = hyperparameters.getOrDefault("vocab", DEFAULT_VOCAB_PATH);
        List<String> explicitVocab;
        File vocabFile = new File(vocabPath);
        if (!vocabPath.isEmpty() && vocabFile.isFile() && vocabFile.canRead()) {
            try {
                explicitVocab = readVocab(vocabFile);
            } catch (IOException e) {
                System.err.println("GruTrainer: could not read vocab file " + vocabPath + ": " + e.getMessage());
                System.exit(2);
                return;
            }
        } else {
            explicitVocab = buildVocab(examples);
        }
        Vocabulary vocabulary = new Vocabulary(explicitVocab);

        Random random = new Random(seed);
        GruWeights weights = randomInit(explicitVocab, vocabulary, random);

        Collections.shuffle(examples, random);
        int validationCount = Math.max(1, examples.size() / 5);
        List<Example> validation = examples.subList(0, validationCount);
        List<Example> train = examples.subList(validationCount, examples.size());
        if (train.isEmpty()) {
            train = examples;
        }

        System.out.println("GruTrainer: starting -- vocabSize=" + explicitVocab.size()
                + ", trainExamples=" + train.size() + ", validationExamples=" + validation.size()
                + ", maxEpochs=" + maxEpochs + ", patience=" + patience + ", lr=" + learningRate);

        AdamState adam = new AdamState(weights);
        double bestValidationLoss = Double.POSITIVE_INFINITY;
        GruWeights bestWeights = weights;
        int epochsSinceImprovement = 0;
        int step = 0;
        long trainingStartNanos = System.nanoTime();

        for (int epoch = 1; epoch <= maxEpochs; epoch++) {
            Collections.shuffle(train, random);
            double trainLoss = 0.0;
            long epochStartNanos = System.nanoTime();
            int examplesSeen = 0;
            for (Example example : train) {
                List<String> tokens = GruClassifier.tokenize(example.text);
                if (tokens.size() > GruClassifier.SEQUENCE_CAP) {
                    tokens = tokens.subList(0, GruClassifier.SEQUENCE_CAP);
                }
                examplesSeen++;
                if (example.targetWordIndex < 0 || example.targetWordIndex >= tokens.size()) {
                    if (progressEvery > 0 && examplesSeen % progressEvery == 0) {
                        printProgress(epoch, examplesSeen, train.size(), trainLoss, epochStartNanos, trainingStartNanos);
                    }
                    continue;
                }
                GruClassifier.ForwardCache cache = GruClassifier.forward(weights, vocabulary, tokens, example.targetWordIndex);
                trainLoss += crossEntropyLoss(cache.logits, example.classIndex);
                GruClassifier.Gradients gradients = GruClassifier.backward(weights, cache, example.classIndex);
                clipGradients(gradients, GRADIENT_CLIP_NORM);
                step++;
                adam.apply(weights, gradients, learningRate, step);
                if (progressEvery > 0 && examplesSeen % progressEvery == 0) {
                    printProgress(epoch, examplesSeen, train.size(), trainLoss, epochStartNanos, trainingStartNanos);
                }
            }
            trainLoss /= train.size();

            double validationLoss = 0.0;
            for (Example example : validation) {
                List<String> tokens = GruClassifier.tokenize(example.text);
                if (tokens.size() > GruClassifier.SEQUENCE_CAP) {
                    tokens = tokens.subList(0, GruClassifier.SEQUENCE_CAP);
                }
                if (example.targetWordIndex < 0 || example.targetWordIndex >= tokens.size()) {
                    continue;
                }
                GruClassifier.ForwardCache cache = GruClassifier.forward(weights, vocabulary, tokens, example.targetWordIndex);
                validationLoss += crossEntropyLoss(cache.logits, example.classIndex);
            }
            validationLoss /= validation.size();

            double epochSeconds = (System.nanoTime() - epochStartNanos) / 1e9;
            double totalSeconds = (System.nanoTime() - trainingStartNanos) / 1e9;
            System.out.println("GruTrainer: epoch " + epoch + " trainLoss=" + trainLoss
                    + " validationLoss=" + validationLoss + " epochSeconds=" + String.format("%.1f", epochSeconds)
                    + " totalElapsedSeconds=" + String.format("%.1f", totalSeconds));

            if (validationLoss < bestValidationLoss) {
                bestValidationLoss = validationLoss;
                bestWeights = weights;
                epochsSinceImprovement = 0;
            } else {
                epochsSinceImprovement++;
                if (epochsSinceImprovement >= patience) {
                    System.out.println("GruTrainer: early stopping at epoch " + epoch
                            + " (no validation improvement for " + patience + " epochs)");
                    break;
                }
            }
        }

        String json = toJson(bestWeights, explicitVocab);
        try {
            Files.write(weightsOut.toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("GruTrainer: could not write output weights file " + args[1] + ": "
                    + e.getMessage());
            System.exit(2);
            return;
        }

        System.out.println("GruTrainer: wrote trained weights file to " + weightsOut
                + " (vocabSize=" + explicitVocab.size() + ", trainExamples=" + train.size()
                + ", validationExamples=" + validation.size() + ", bestValidationLoss=" + bestValidationLoss + ")");
    }

    /** Mid-epoch progress line: examples-seen/total, running average train loss so far this epoch,
     *  elapsed time this epoch, and a rough estimated-time-remaining for the epoch based on the
     *  average per-example rate observed so far. Printed via {@code System.out.println}, which
     *  auto-flushes on newline for the console/redirected-file stdout case this is meant for (a
     *  human tailing the process's own manual invocation, per the 2026-07-29 request), so no
     *  explicit {@code System.out.flush()} is needed. */
    private static void printProgress(int epoch, int examplesSeen, int totalExamples, double lossSoFar,
            long epochStartNanos, long trainingStartNanos) {
        double elapsedSeconds = (System.nanoTime() - epochStartNanos) / 1e9;
        double perExampleSeconds = elapsedSeconds / examplesSeen;
        double etaSeconds = perExampleSeconds * (totalExamples - examplesSeen);
        double totalElapsedSeconds = (System.nanoTime() - trainingStartNanos) / 1e9;
        System.out.println("GruTrainer: epoch " + epoch + " progress " + examplesSeen + "/" + totalExamples
                + " (" + String.format("%.1f", 100.0 * examplesSeen / totalExamples) + "%)"
                + " avgTrainLoss=" + String.format("%.4f", lossSoFar / examplesSeen)
                + " epochElapsedSeconds=" + String.format("%.1f", elapsedSeconds)
                + " epochEtaSeconds=" + String.format("%.1f", etaSeconds)
                + " totalElapsedSeconds=" + String.format("%.1f", totalElapsedSeconds));
    }

    private static final class Example {
        final String label;
        final int classIndex;
        final int targetWordIndex;
        final String text;

        Example(String label, int classIndex, int targetWordIndex, String text) {
            this.label = label;
            this.classIndex = classIndex;
            this.targetWordIndex = targetWordIndex;
            this.text = text;
        }
    }

    /** Parses RDD_EXT_20/RDD_EXT_21's labeled-examples schema:
     *  {@code <lang>\t<label:YES|NO>\t<targetWordIndex>\t<escaped-text>}. Lines starting with
     *  {@code #} (comments) and blank lines are skipped, matching {@code sample_examples.txt}'s
     *  own illustrative-shape convention. */
    private static List<Example> readExamples(File examplesFile) throws IOException {
        List<Example> examples = new ArrayList<>();
        for (String line : Files.readAllLines(examplesFile.toPath(), StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\t", 4);
            if (parts.length != 4) {
                continue;
            }
            String label = parts[1];
            if (!label.equals("YES") && !label.equals("NO")) {
                continue;
            }
            int classIndex = label.equals("YES") ? 0 : 1; // GruClassifier.CLASS_ORDER: YES=0, NO=1
            int targetWordIndex;
            try {
                targetWordIndex = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                continue;
            }
            String text = unescape(parts[3]);
            examples.add(new Example(label, classIndex, targetWordIndex, text));
        }
        return examples;
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                if (next == 'n') {
                    sb.append('\n');
                    i++;
                    continue;
                } else if (next == 't') {
                    sb.append('\t');
                    i++;
                    continue;
                } else if (next == '\\') {
                    sb.append('\\');
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static List<String> readVocab(File vocabFile) throws IOException {
        List<String> words = new ArrayList<>();
        for (String line : Files.readAllLines(vocabFile.toPath(), StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            words.add(trimmed);
        }
        return words;
    }

    private static List<String> buildVocab(List<Example> examples) {
        Set<String> seen = new LinkedHashSet<>();
        for (Example example : examples) {
            for (String token : GruClassifier.tokenize(example.text)) {
                seen.add(token);
            }
        }
        return new ArrayList<>(seen);
    }

    private static double crossEntropyLoss(double[] logits, int trueClassIndex) {
        double[] probabilities = GruClassifier.softmax(logits);
        return -Math.log(Math.max(probabilities[trueClassIndex], 1e-12));
    }

    private static GruWeights randomInit(List<String> explicitVocab, Vocabulary vocabulary, Random random) {
        int embeddingRows = vocabulary.totalEmbeddingRows();
        double[][] embeddings = new double[embeddingRows][EMBEDDING_DIM];
        for (int i = 0; i < embeddingRows; i++) {
            for (int j = 0; j < EMBEDDING_DIM; j++) {
                embeddings[i][j] = glorot(random, EMBEDDING_DIM, EMBEDDING_DIM);
            }
        }

        GruWeights.DirectionWeights forward = randomDirectionWeights(random);
        GruWeights.DirectionWeights backward = randomDirectionWeights(random);

        double[][] denseW = new double[DENSE_SIZE][2 * HIDDEN_SIZE];
        for (int i = 0; i < DENSE_SIZE; i++) {
            for (int j = 0; j < 2 * HIDDEN_SIZE; j++) {
                denseW[i][j] = glorot(random, 2 * HIDDEN_SIZE, DENSE_SIZE);
            }
        }
        double[] denseB = new double[DENSE_SIZE];

        double[][] outW = new double[GruClassifier.CLASS_ORDER.length][DENSE_SIZE];
        for (int i = 0; i < outW.length; i++) {
            for (int j = 0; j < DENSE_SIZE; j++) {
                outW[i][j] = glorot(random, DENSE_SIZE, outW.length);
            }
        }
        double[] outB = new double[GruClassifier.CLASS_ORDER.length];

        return new GruWeightsBuilder()
                .schemaVersion(GruWeights.CURRENT_SCHEMA_VERSION)
                .vocabSize(explicitVocab.size())
                .hashBuckets(GruClassifier.HASH_BUCKETS)
                .embeddingDim(EMBEDDING_DIM)
                .hiddenSize(HIDDEN_SIZE)
                .sequenceCap(GruClassifier.SEQUENCE_CAP)
                .numClasses(GruClassifier.CLASS_ORDER.length)
                .abstainThreshold(ABSTAIN_THRESHOLD)
                .explicitVocab(explicitVocab.toArray(new String[0]))
                .embeddings(embeddings)
                .forward(forward)
                .backward(backward)
                .denseW(denseW)
                .denseB(denseB)
                .outW(outW)
                .outB(outB)
                .build();
    }

    private static GruWeights.DirectionWeights randomDirectionWeights(Random random) {
        GruWeights.DirectionWeights weights = GruWeights.DirectionWeights.zeros(HIDDEN_SIZE, EMBEDDING_DIM);
        fillGlorot(weights.Wz, random, EMBEDDING_DIM, HIDDEN_SIZE);
        fillGlorot(weights.Wr, random, EMBEDDING_DIM, HIDDEN_SIZE);
        fillGlorot(weights.Wh, random, EMBEDDING_DIM, HIDDEN_SIZE);
        fillGlorot(weights.Uz, random, HIDDEN_SIZE, HIDDEN_SIZE);
        fillGlorot(weights.Ur, random, HIDDEN_SIZE, HIDDEN_SIZE);
        fillGlorot(weights.Uh, random, HIDDEN_SIZE, HIDDEN_SIZE);
        return weights;
    }

    private static void fillGlorot(double[][] matrix, Random random, int fanIn, int fanOut) {
        for (double[] row : matrix) {
            for (int j = 0; j < row.length; j++) {
                row[j] = glorot(random, fanIn, fanOut);
            }
        }
    }

    private static double glorot(Random random, int fanIn, int fanOut) {
        double limit = Math.sqrt(6.0 / (fanIn + fanOut));
        return (random.nextDouble() * 2 - 1) * limit;
    }

    /** Clips the entire gradient set to the specified global L2 norm. This is the
     *  standard "global norm" clipping used by most GRU/LSTM implementations rather
     *  than clipping each tensor independently, since it preserves the direction of
     *  the update while only reducing its magnitude when necessary. */
    private static void clipGradients(GruClassifier.Gradients gradients, double maxNorm) {
        double sumSquares = 0.0;

        for (double[] row : gradients.embeddingGrad.values()) {
            for (double v : row) {
                sumSquares += v * v;
            }
        }

        sumSquares += sumSquares(gradients.forward);
        sumSquares += sumSquares(gradients.backward);
        sumSquares += sumSquares(gradients.denseW);
        sumSquares += sumSquares(gradients.denseB);
        sumSquares += sumSquares(gradients.outW);
        sumSquares += sumSquares(gradients.outB);

        double norm = Math.sqrt(sumSquares);
        if (norm <= maxNorm || norm == 0.0) {
            return;
        }

        double scale = maxNorm / norm;

        for (double[] row : gradients.embeddingGrad.values()) {
            scale(row, scale);
        }

        scale(gradients.forward, scale);
        scale(gradients.backward, scale);
        scale(gradients.denseW, scale);
        scale(gradients.denseB, scale);
        scale(gradients.outW, scale);
        scale(gradients.outB, scale);
    }

    private static double sumSquares(GruWeights.DirectionWeights w) {
        return sumSquares(w.Wz) + sumSquares(w.Wr) + sumSquares(w.Wh)
                + sumSquares(w.Uz) + sumSquares(w.Ur) + sumSquares(w.Uh)
                + sumSquares(w.bz) + sumSquares(w.br) + sumSquares(w.bh);
    }

    private static double sumSquares(double[][] m) {
        double s = 0.0;
        for (double[] row : m) {
            s += sumSquares(row);
        }
        return s;
    }

    private static double sumSquares(double[] v) {
        double s = 0.0;
        for (double x : v) {
            s += x * x;
        }
        return s;
    }

    private static void scale(GruWeights.DirectionWeights w, double factor) {
        scale(w.Wz, factor);
        scale(w.Wr, factor);
        scale(w.Wh, factor);
        scale(w.Uz, factor);
        scale(w.Ur, factor);
        scale(w.Uh, factor);
        scale(w.bz, factor);
        scale(w.br, factor);
        scale(w.bh, factor);
   }

    private static void scale(double[][] m, double factor) {
        for (double[] row : m) {
            scale(row, factor);
        }
    }

    private static void scale(double[] v, double factor) {
        for (int i = 0; i < v.length; i++) {
            v[i] *= factor;
        }
    }

    /** Adam optimizer state (first/second moment estimates), one pair of accumulator arrays per
     *  trained-weight array, mirroring {@link GruWeights}'s field layout exactly so each weight
     *  array's update can be applied in lockstep with its gradient. Embedding-row moments are kept
     *  densely (one row per vocab+hash-bucket slot) even though any single example's gradient only
     *  touches a handful of rows -- the corpora here are small enough that this is simpler than a
     *  sparse moment table, and moment state must persist across examples/epochs regardless. */
    private static final class AdamState {
        private static final double BETA1 = 0.9;
        private static final double BETA2 = 0.999;
        private static final double EPSILON = 1e-8;

        final double[][] embeddingM, embeddingV;
        final DirectionMoments forward, backward;
        final double[][] denseWM, denseWV;
        final double[] denseBM, denseBV;
        final double[][] outWM, outWV;
        final double[] outBM, outBV;

        AdamState(GruWeights weights) {
            embeddingM = new double[weights.embeddings.length][weights.embeddingDim];
            embeddingV = new double[weights.embeddings.length][weights.embeddingDim];
            forward = new DirectionMoments(weights.hiddenSize, weights.embeddingDim);
            backward = new DirectionMoments(weights.hiddenSize, weights.embeddingDim);
            denseWM = new double[weights.denseW.length][weights.denseW[0].length];
            denseWV = new double[weights.denseW.length][weights.denseW[0].length];
            denseBM = new double[weights.denseB.length];
            denseBV = new double[weights.denseB.length];
            outWM = new double[weights.outW.length][weights.outW[0].length];
            outWV = new double[weights.outW.length][weights.outW[0].length];
            outBM = new double[weights.outB.length];
            outBV = new double[weights.outB.length];
        }

        static final class DirectionMoments {
            final double[][] WzM, WzV, WrM, WrV, WhM, WhV;
            final double[][] UzM, UzV, UrM, UrV, UhM, UhV;
            final double[] bzM, bzV, brM, brV, bhM, bhV;

            DirectionMoments(int hiddenSize, int embeddingDim) {
                WzM = new double[hiddenSize][embeddingDim];
                WzV = new double[hiddenSize][embeddingDim];
                WrM = new double[hiddenSize][embeddingDim];
                WrV = new double[hiddenSize][embeddingDim];
                WhM = new double[hiddenSize][embeddingDim];
                WhV = new double[hiddenSize][embeddingDim];
                UzM = new double[hiddenSize][hiddenSize];
                UzV = new double[hiddenSize][hiddenSize];
                UrM = new double[hiddenSize][hiddenSize];
                UrV = new double[hiddenSize][hiddenSize];
                UhM = new double[hiddenSize][hiddenSize];
                UhV = new double[hiddenSize][hiddenSize];
                bzM = new double[hiddenSize];
                bzV = new double[hiddenSize];
                brM = new double[hiddenSize];
                brV = new double[hiddenSize];
                bhM = new double[hiddenSize];
                bhV = new double[hiddenSize];
            }
        }

        void apply(GruWeights weights, GruClassifier.Gradients gradients, double lr, int step) {
            double biasCorrection1 = 1.0 - Math.pow(BETA1, step);
            double biasCorrection2 = 1.0 - Math.pow(BETA2, step);

            for (Map.Entry<Integer, double[]> entry : gradients.embeddingGrad.entrySet()) {
                int row = entry.getKey();
                update1D(weights.embeddings[row], entry.getValue(), embeddingM[row],
                        embeddingV[row], lr, biasCorrection1, biasCorrection2);
            }
            applyDirection(weights.forward, gradients.forward, forward, lr,
                    biasCorrection1, biasCorrection2);
            applyDirection(weights.backward, gradients.backward, backward, lr,
                    biasCorrection1, biasCorrection2);
            update2D(weights.denseW, gradients.denseW, denseWM, denseWV, lr,
                    biasCorrection1, biasCorrection2);
            update1D(weights.denseB, gradients.denseB, denseBM, denseBV, lr,
                    biasCorrection1, biasCorrection2);
            update2D(weights.outW, gradients.outW, outWM, outWV, lr,
                    biasCorrection1, biasCorrection2);
            update1D(weights.outB, gradients.outB, outBM, outBV, lr,
                    biasCorrection1, biasCorrection2);
        }

        private void applyDirection(GruWeights.DirectionWeights weights, GruWeights.DirectionWeights gradients,
                DirectionMoments moments, double lr,
                double biasCorrection1, double biasCorrection2) {
            update2D(weights.Wz, gradients.Wz, moments.WzM, moments.WzV, lr,
                    biasCorrection1, biasCorrection2);
            update2D(weights.Wr, gradients.Wr, moments.WrM, moments.WrV, lr,
                    biasCorrection1, biasCorrection2);
            update2D(weights.Wh, gradients.Wh, moments.WhM, moments.WhV, lr,
                    biasCorrection1, biasCorrection2);
            update2D(weights.Uz, gradients.Uz, moments.UzM, moments.UzV, lr,
                    biasCorrection1, biasCorrection2);
            update2D(weights.Ur, gradients.Ur, moments.UrM, moments.UrV, lr,
                    biasCorrection1, biasCorrection2);
            update2D(weights.Uh, gradients.Uh, moments.UhM, moments.UhV, lr,
                    biasCorrection1, biasCorrection2);
            update1D(weights.bz, gradients.bz, moments.bzM, moments.bzV, lr,
                    biasCorrection1, biasCorrection2);
            update1D(weights.br, gradients.br, moments.brM, moments.brV, lr,
                    biasCorrection1, biasCorrection2);
            update1D(weights.bh, gradients.bh, moments.bhM, moments.bhV, lr,
                    biasCorrection1, biasCorrection2);
        }

        private static void update1D(double[] param, double[] grad,
                double[] m, double[] v, double lr,
                double biasCorrection1, double biasCorrection2) {
            for (int i = 0; i < param.length; i++) {
                m[i] = BETA1 * m[i] + (1 - BETA1) * grad[i];
                v[i] = BETA2 * v[i] + (1 - BETA2) * grad[i] * grad[i];
                double mHat = m[i] / biasCorrection1;
                double vHat = v[i] / biasCorrection2;
                param[i] -= lr * mHat / (Math.sqrt(vHat) + EPSILON);
            }
        }

        private static void update2D(double[][] param, double[][] grad,
                double[][] m, double[][] v, double lr,
                double biasCorrection1, double biasCorrection2) {
            for (int i = 0; i < param.length; i++) {
                update1D(param[i], grad[i], m[i], v[i], lr,
                        biasCorrection1, biasCorrection2);
            }
        }
    }

    /** Builder for {@link GruWeights} -- its own constructor is package-private (only
     *  {@code GruWeights.load} normally builds instances), so this class hand-assembles the same
     *  JSON {@code GruWeights.load} parses instead of trying to call that constructor directly.
     *  Kept as a builder (rather than one giant constructor call) only to keep {@link #randomInit}
     *  readable given how many fields {@link GruWeights} now has. */
    private static final class GruWeightsBuilder {
        private int schemaVersion, vocabSize, hashBuckets, embeddingDim, hiddenSize, sequenceCap, numClasses;
        private double abstainThreshold;
        private String[] explicitVocab;
        private double[][] embeddings;
        private GruWeights.DirectionWeights forward, backward;
        private double[][] denseW;
        private double[] denseB;
        private double[][] outW;
        private double[] outB;

        GruWeightsBuilder schemaVersion(int v) { this.schemaVersion = v; return this; }
        GruWeightsBuilder vocabSize(int v) { this.vocabSize = v; return this; }
        GruWeightsBuilder hashBuckets(int v) { this.hashBuckets = v; return this; }
        GruWeightsBuilder embeddingDim(int v) { this.embeddingDim = v; return this; }
        GruWeightsBuilder hiddenSize(int v) { this.hiddenSize = v; return this; }
        GruWeightsBuilder sequenceCap(int v) { this.sequenceCap = v; return this; }
        GruWeightsBuilder numClasses(int v) { this.numClasses = v; return this; }
        GruWeightsBuilder abstainThreshold(double v) { this.abstainThreshold = v; return this; }
        GruWeightsBuilder explicitVocab(String[] v) { this.explicitVocab = v; return this; }
        GruWeightsBuilder embeddings(double[][] v) { this.embeddings = v; return this; }
        GruWeightsBuilder forward(GruWeights.DirectionWeights v) { this.forward = v; return this; }
        GruWeightsBuilder backward(GruWeights.DirectionWeights v) { this.backward = v; return this; }
        GruWeightsBuilder denseW(double[][] v) { this.denseW = v; return this; }
        GruWeightsBuilder denseB(double[] v) { this.denseB = v; return this; }
        GruWeightsBuilder outW(double[][] v) { this.outW = v; return this; }
        GruWeightsBuilder outB(double[] v) { this.outB = v; return this; }

        GruWeights build() {
            // GruWeights has no public constructor (see class javadoc above) -- round-trip through
            // its own JSON schema instead, which is public API (GruWeights.load).
            String json = toJson(this);
            try {
                java.nio.file.Path tmp = Files.createTempFile("gru_trainer_init", ".json");
                Files.write(tmp, json.getBytes(StandardCharsets.UTF_8));
                GruWeights result = GruWeights.load(tmp);
                Files.deleteIfExists(tmp);
                return result;
            } catch (IOException e) {
                throw new IllegalStateException("GruTrainer: failed to round-trip initial weights: " + e.getMessage(), e);
            }
        }

        private static String toJson(GruWeightsBuilder b) {
            return GruTrainer.toJsonFields(b.schemaVersion, b.vocabSize, b.hashBuckets, b.embeddingDim,
                    b.hiddenSize, b.sequenceCap, b.numClasses, b.abstainThreshold, b.explicitVocab,
                    b.embeddings, b.forward, b.backward, b.denseW, b.denseB, b.outW, b.outB);
        }
    }

    private static String toJson(GruWeights weights, List<String> explicitVocab) {
        return toJsonFields(weights.schemaVersion, explicitVocab.size(), weights.hashBuckets, weights.embeddingDim,
                weights.hiddenSize, weights.sequenceCap, weights.numClasses, weights.abstainThreshold,
                explicitVocab.toArray(new String[0]), weights.embeddings, weights.forward, weights.backward,
                weights.denseW, weights.denseB, weights.outW, weights.outB);
    }

    private static String toJsonFields(int schemaVersion, int vocabSize, int hashBuckets, int embeddingDim,
            int hiddenSize, int sequenceCap, int numClasses, double abstainThreshold, String[] explicitVocab,
            double[][] embeddings, GruWeights.DirectionWeights forward, GruWeights.DirectionWeights backward,
            double[][] denseW, double[] denseB, double[][] outW, double[] outB) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"schemaVersion\": ").append(schemaVersion).append(",\n");
        sb.append("  \"vocabSize\": ").append(vocabSize).append(",\n");
        sb.append("  \"hashBuckets\": ").append(hashBuckets).append(",\n");
        sb.append("  \"embeddingDim\": ").append(embeddingDim).append(",\n");
        sb.append("  \"hiddenSize\": ").append(hiddenSize).append(",\n");
        sb.append("  \"sequenceCap\": ").append(sequenceCap).append(",\n");
        sb.append("  \"numClasses\": ").append(numClasses).append(",\n");
        sb.append("  \"abstainThreshold\": ").append(abstainThreshold).append(",\n");
        sb.append("  \"explicitVocab\": ").append(jsonStringArray(explicitVocab)).append(",\n");
        sb.append("  \"embeddings\": ").append(jsonArray2D(embeddings)).append(",\n");
        appendDirection(sb, "forward", forward);
        appendDirection(sb, "backward", backward);
        sb.append("  \"denseW\": ").append(jsonArray2D(denseW)).append(",\n");
        sb.append("  \"denseB\": ").append(jsonArray1D(denseB)).append(",\n");
        sb.append("  \"outW\": ").append(jsonArray2D(outW)).append(",\n");
        sb.append("  \"outB\": ").append(jsonArray1D(outB)).append("\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static void appendDirection(StringBuilder sb, String prefix, GruWeights.DirectionWeights weights) {
        sb.append("  \"").append(prefix).append("Wz\": ").append(jsonArray2D(weights.Wz)).append(",\n");
        sb.append("  \"").append(prefix).append("Wr\": ").append(jsonArray2D(weights.Wr)).append(",\n");
        sb.append("  \"").append(prefix).append("Wh\": ").append(jsonArray2D(weights.Wh)).append(",\n");
        sb.append("  \"").append(prefix).append("Uz\": ").append(jsonArray2D(weights.Uz)).append(",\n");
        sb.append("  \"").append(prefix).append("Ur\": ").append(jsonArray2D(weights.Ur)).append(",\n");
        sb.append("  \"").append(prefix).append("Uh\": ").append(jsonArray2D(weights.Uh)).append(",\n");
        sb.append("  \"").append(prefix).append("bz\": ").append(jsonArray1D(weights.bz)).append(",\n");
        sb.append("  \"").append(prefix).append("br\": ").append(jsonArray1D(weights.br)).append(",\n");
        sb.append("  \"").append(prefix).append("bh\": ").append(jsonArray1D(weights.bh)).append(",\n");
    }

    private static String jsonArray1D(double[] values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(values[i]);
        }
        return sb.append("]").toString();
    }

    private static String jsonArray2D(double[][] rows) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(jsonArray1D(rows[i]));
        }
        return sb.append("]").toString();
    }

    private static String jsonStringArray(String[] values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(escapeJsonString(values[i])).append("\"");
        }
        return sb.append("]").toString();
    }

    private static String escapeJsonString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t");
    }
}
