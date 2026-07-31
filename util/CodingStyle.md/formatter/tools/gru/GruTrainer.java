/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.jxmake.formatter.classifier.gru.GruClassifier;
import com.jxmake.formatter.classifier.gru.GruWeights;
import com.jxmake.formatter.classifier.gru.Vocabulary;

/**
 * Training entry point for the Step 3 GRU comment-classifier, per STATE_AI.md's "GRU
 *  implementation design". Deliberately lives outside {@code src/} -- the runtime JAR must never
 *  bundle training code; this writes a weights file for
 *  {@code com.jxmake.formatter.classifier.gru.GruClassifier}/{@code GruWeights} to read at
 *  runtime, it does not generate or overwrite any {@code .java} source.
 *
 *  <p><b>Current behavior:</b> a real training loop -- random (Xavier/Glorot-style) weight
 *  initialization, per-example forward pass via {@link GruClassifier#forward}, backprop-through-
 *  time via {@link GruClassifier#backward}, gradients averaged (not summed) across a mini-batch of
 *  {@code --batch-size} examples (default 16, per RDD_EXT_18's batch-size-32 starting default --
 *  16 chosen as this codebase's own smaller default, see STATE_AI.md's mini-batch session), and a
 *  single Adam optimizer step applied per batch (the Adam {@code step} counter increments once per
 *  batch, not once per example -- see {@code --threads} below for how within-batch parallelism
 *  composes with this). Runs for up to
 *  {@code --epochs} (default from RDD_EXT_18: 30) with early stopping on a held-out validation
 *  split's cross-entropy loss (patience-based: stop once validation loss hasn't improved for
 *  {@code --patience} epochs, default 5).
 *
 *  <p><b>Learning-rate schedule (opt-in, default off):</b> {@code --warmup-steps=N} (default 0)
 *  ramps the LR linearly from 0 up to {@code --lr} over the first N Adam steps, then follows a
 *  cosine decay down to {@code --lr-min} (default 0.0) over the remaining steps up to the decay
 *  horizon ({@code stepsPerEpoch * --epochs}). {@code --warmup-steps=0} (the default) disables the
 *  schedule entirely -- {@code --lr} is used flat throughout, exactly as before this feature
 *  existed. See {@link #computeScheduledLr}.
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
 *  useful only for quick local smoke tests against tiny placeholder data.
 */
public final class GruTrainer {

    private GruTrainer()
    {
    }

    private static final String USAGE = "Usage: GruTrainer <labeled-examples-path> <output-weights-path> [--key=value ...]";

    private static final int    EMBEDDING_DIM     = 16;
    private static final int    HIDDEN_SIZE       = 224;
    private static final int    DENSE_SIZE        = 64;
    private static final double ABSTAIN_THRESHOLD = 0.5;
    /**
     * Global L2-norm gradient clipping threshold. Prevents exploding gradients in the
     *  recurrent layers while leaving ordinary updates unchanged. A value around 5 is a
     *  conventional starting point for GRU/LSTM training and can later become a
     *  --clip=<value> hyperparameter if needed.
     */
    private static final double GRADIENT_CLIP_NORM = 5.0;
    private static final String DEFAULT_VOCAB_PATH = "tools/gru/explicit_vocab.txt";

    // ── Checkpointing (break/resume support) ─────────────────────────────────────────────────────
    //
    // Two binary checkpoint files, both derived from --out's path and living right next to it:
    //   <out>.ckpt-current.bin -- overwritten every epoch. Full resumable state: weights, vocab,
    //     Adam optimizer moment arrays, and scalar run state (epoch, epochsSinceImprovement,
    //     bestValidationLoss, learningRate/maxEpochs/patience/seed, Adam step counter). This is the
    //     file --resume=<path> expects.
    //   <out>.ckpt-best.bin -- overwritten only when validation loss improves. Weights + vocab only
    //     (no optimizer/run state) -- "give me the best model so far", not a resume target on its
    //     own, though loadCurrentCheckpoint's caller does read it as a *sibling* of a resume target
    //     to recover the true best-so-far weight arrays (the current-weights checkpoint alone only
    //     ever holds the latest epoch's weights, not the best one).
    // Both are plain java.io.DataOutputStream/DataInputStream over Buffered*Stream -- no external
    // library, consistent with this codebase's zero-third-party-dependency convention -- chosen over
    // JSON purely for I/O speed on frequent (every-epoch) writes of a several-MB weights blob.
    // Written via a temp-file-then-atomic-rename (Files.move + REPLACE_EXISTING) so a process killed
    // mid-write never leaves a half-written, corrupt checkpoint behind -- exactly the crash scenario
    // this feature exists to protect against. The temp file is fsync'd before the rename and the
    // containing directory is fsync'd after it (see fsyncFile/fsyncParentDirectory), so a real power
    // failure can lose at most the in-flight checkpoint attempt, never corrupt or silently roll back
    // a previously-completed one. Both files are deleted on normal successful completion
    // (see the end of main) -- they are a resume/recovery safety net, never a persistent artifact,
    // same posture as every other real per-run output this job never commits (RDD_EXT_19-style).
    private static final int    CHECKPOINT_MAGIC           = 0x47525543; // "GRUC"
    // Bumped 1 -> 2 when mini-batch training added a new persisted scalar (batchSize), then 2 -> 3
    // when the LR warmup+cosine-decay schedule added two more (warmupSteps, lrMin) to the
    // current-weights checkpoint's run-state block -- see writeCurrentCheckpoint/loadCurrentCheckpoint.
    // A checkpoint from an older format version is simply rejected by the version check below rather
    // than silently misread; checkpoints are an ephemeral resume/recovery safety net (never
    // committed, deleted on normal completion), so this is a safe, low-cost break.
    private static final int    CHECKPOINT_FORMAT_VERSION  = 3;
    private static final String CHECKPOINT_CURRENT_SUFFIX  = ".ckpt-current.bin";
    private static final String CHECKPOINT_BEST_SUFFIX     = ".ckpt-best.bin";

    public static void main(String[] args)
    {
        if(args.length < 2) {
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        File examplesFile = new File( args[0] );
        if( !examplesFile.isFile() || !examplesFile.canRead() ) {
            System.err.println( "GruTrainer: labeled-examples file not readable: " + args[0] );
            System.exit(2);
            return;
        }

        File weightsOut       = new File( args[1] );
        File weightsOutParent = weightsOut.getAbsoluteFile().getParentFile();
        if( weightsOutParent == null || !weightsOutParent.isDirectory() ) {
            System.err.println( "GruTrainer: output-weights-path parent directory does not exist: "
                    + args[1] );
            System.exit(2);
            return;
        }

        Map<String, String> hyperparameters = new LinkedHashMap<>();
        for(int i = 2; i < args.length; ++i) {
            String arg = args[i];
            if( !arg.startsWith("--") || arg.indexOf('=') < 0 ) {
                System.err.println("GruTrainer: malformed hyperparameter arg (expected --key=value): "
                        + arg);
                System.err.println(USAGE);
                System.exit(2);
                return;
            } // if
            int    eq    = arg.indexOf('=');
            String key   = arg.substring(2, eq);
            String value = arg.substring(eq + 1);
            if( key.isEmpty() ) {
                System.err.println("GruTrainer: malformed hyperparameter arg (empty key): " + arg);
                System.exit(2);
                return;
            }
            hyperparameters.put(key, value);
        } // for

        // --resume=<checkpoint-path>: loads a previously-written current-weights checkpoint (see
        // CHECKPOINT_CURRENT_SUFFIX below) and continues training from where it left off, instead of
        // randomInit. See ResumeState/loadCurrentCheckpoint's javadoc for exactly what is and isn't
        // faithfully restored. Loaded here (before the RDD_EXT_18 hyperparameter defaults just below)
        // so lr/epochs/patience/seed can fall back to the checkpoint's own recorded values instead of
        // the hardcoded starting defaults when resuming and the CLI doesn't explicitly override them.
        String      resumePath = hyperparameters.get("resume");
        ResumeState resumed    = null;
        if(resumePath != null) {
            File resumeFile = new File(resumePath);
            if( !resumeFile.isFile() || !resumeFile.canRead() ) {
                System.err.println( "GruTrainer: --resume checkpoint file not readable: " + resumePath );
                System.exit(2);
                return;
            }
            try {
                resumed = loadCurrentCheckpoint(resumeFile);
            }
            catch(IOException e) {
                System.err.println( "GruTrainer: could not load --resume checkpoint " + resumePath
                        + ": " + e.getMessage() );
                System.exit(2);
                return;
            }
            System.out.println( String.format(
                    "GruTrainer: resuming from '%s' (epoch=%2d, epochsSinceImprovement=%2d,"
                            + " bestValidationLoss=%9.7f)",
                    resumePath, resumed.epoch, resumed.epochsSinceImprovement, resumed.bestValidationLoss ) );
        } // if

        // RDD_EXT_18 starting defaults, overridable via --key=value; fall back to the resumed
        // checkpoint's own recorded values (not the hardcoded RDD_EXT_18 defaults) when resuming.
        double learningRate = Double.parseDouble( hyperparameters.getOrDefault(
            "lr", resumed != null ? String.valueOf(resumed.learningRate) : "0.001"
        ) );
        int maxEpochs = Integer.parseInt( hyperparameters.getOrDefault(
            "epochs", resumed != null ? String.valueOf(resumed.maxEpochs) : "30"
        ) );
        int patience = Integer.parseInt( hyperparameters.getOrDefault(
            "patience", resumed != null ? String.valueOf(resumed.patience) : "5"
        ) );
        long seed = Long.parseLong( hyperparameters.getOrDefault(
            "seed", resumed != null ? String.valueOf(resumed.seed) : "42"
        ) );
        // Progress reporting (2026-07-29): large auto-labeled corpora (100k+ examples) take long
        // enough per epoch that a human watching the process needs mid-epoch feedback, not just a
        // once-per-epoch line. --progress-every=N prints a running line every N training examples
        // within the epoch (0 disables); default chosen so it fires a handful of times per epoch on
        // a ~100k-example corpus without flooding the console on small corpora.
        int progressEvery = Integer.parseInt(
            hyperparameters.getOrDefault("progress-every", "1000")
        );
        // --threads=N (default 1, i.e. plain sequential per-batch computation): computes forward/
        // backward for every example in a mini-batch (see --batch-size below) in parallel across N
        // worker threads, all against the same pre-batch weights snapshot (safe -- forward/backward
        // only read weights, never mutate it), before the batch's gradients are averaged and a
        // single Adam update applied. This means threads controls *within-batch* parallelism only --
        // it composes orthogonally with --batch-size (which controls how many examples' gradients get
        // averaged per Adam step), not a separate/competing mechanism. Left opt-in (default 1) rather
        // than defaulting to all cores, so a real training run doesn't unexpectedly saturate the
        // machine.
        int threads = Integer.parseInt( hyperparameters.getOrDefault("threads", "1") );
        if(threads < 1) {
            System.err.println("GruTrainer: --threads must be >= 1, got " + threads);
            System.exit(2);
            return;
        }
        // --batch-size=N (default 16): number of examples whose gradients are averaged (not summed --
        // averaging keeps the effective step size comparable across different batch sizes, matching
        // standard mini-batch SGD/Adam) before one Adam optimizer step is applied. The Adam `step`
        // counter (used for bias-correction) increments once per batch, not once per example -- see
        // AdamState.apply. A resumable hyperparameter, same override-if-specified-else-checkpoint-
        // value pattern as --lr/--epochs/--patience below. The last partial batch of an epoch (when
        // trainExamples % batchSize != 0) naturally averages over however many examples it actually
        // has, since averageGradients divides by the batch's real non-skipped example count, not a
        // hardcoded batchSize.
        int batchSize = Integer.parseInt( hyperparameters.getOrDefault(
            "batch-size", resumed != null ? String.valueOf(resumed.batchSize) : "16"
        ) );
        if(batchSize < 1) {
            System.err.println("GruTrainer: --batch-size must be >= 1, got " + batchSize);
            System.exit(2);
            return;
        }
        // --warmup-steps=N (default 0, resumable like --batch-size): learning-rate warmup + cosine
        // decay, opt-in via this one flag -- 0 (default) means the schedule is disabled entirely and
        // learningRate is used flat throughout, exactly matching pre-schedule behavior (backward
        // compatible). N > 0 ramps the LR linearly from 0 up to --lr over the first N Adam steps
        // (step-granular, not epoch-granular, since training became per-batch-step in the mini-batch
        // session -- warming up per-step is smoother than per-epoch under mini-batching), then follows
        // a cosine decay from --lr down to --lr-min over the remaining steps up to the decay horizon
        // (stepsPerEpoch * --epochs -- reuses --epochs as the horizon rather than inventing a third
        // duration concept alongside epochs/patience; early stopping via --patience simply cuts the
        // schedule off early, same as it already does for maxEpochs itself). See
        // computeScheduledLr/the epoch-loop call site below.
        int warmupSteps = Integer.parseInt( hyperparameters.getOrDefault(
            "warmup-steps", resumed != null ? String.valueOf(resumed.warmupSteps) : "0"
        ) );
        if(warmupSteps < 0) {
            System.err.println("GruTrainer: --warmup-steps must be >= 0, got " + warmupSteps);
            System.exit(2);
            return;
        }
        // --lr-min=N (default 0.0, resumable): the cosine decay's floor. Only meaningful when
        // --warmup-steps > 0; ignored (schedule disabled) otherwise.
        double lrMin = Double.parseDouble( hyperparameters.getOrDefault(
            "lr-min", resumed != null ? String.valueOf(resumed.lrMin) : "0.0"
        ) );
        if(lrMin < 0) {
            System.err.println("GruTrainer: --lr-min must be >= 0, got " + lrMin);
            System.exit(2);
            return;
        }
        // --check-gradients=N (absent by default): diagnostic-only mode, does not train. Picks one
        // random example and N random entries from a representative sample of the weight arrays,
        // compares GruClassifier.backward()'s analytic gradient for each against a numeric
        // finite-difference estimate, and exits. Use this to sanity-check backward() before relying
        // on it for further changes -- it never runs during normal training.
        boolean checkGradients = hyperparameters.containsKey("check-gradients");
        int checkGradientSamples = checkGradients
                ? Integer.parseInt( hyperparameters.get("check-gradients") ) : 0;

        List<Example> examples;
        try {
            examples = readExamples(examplesFile);
        }
        catch(IOException e) {
            System.err.println( "GruTrainer: could not read labeled-examples file " + args[0] + ": "
                    + e.getMessage() );
            System.exit(2);
            return;
        }
        if( examples.isEmpty() ) {
            System.err.println(
                "GruTrainer: labeled-examples file " + args[0] + " has no examples"
            );
            System.exit(2);
            return;
        } // if

        List<String> explicitVocab;
        if(resumed != null) {
            // Resuming: the vocab is whatever embedding-row layout the checkpoint's own weights were
            // trained against (embedded in the checkpoint itself, mirroring how GruWeights embeds its
            // own explicitVocab snapshot in the JSON weights file) -- never re-derived from --vocab/
            // the examples file, which could silently shift embedding-row indices out from under the
            // resumed weight arrays.
            explicitVocab = resumed.explicitVocab;
        } // if
        else {
            String vocabPath = hyperparameters.getOrDefault("vocab", DEFAULT_VOCAB_PATH);
            File   vocabFile = new File(vocabPath);
            if( !vocabPath.isEmpty() && vocabFile.isFile() && vocabFile.canRead() ) {
                try {
                    explicitVocab = readVocab(vocabFile);
                }
                catch(IOException e) {
                    System.err.println(
                        "GruTrainer: could not read vocab file " + vocabPath + ": " + e.getMessage()
                    );
                    System.exit(2);
                    return;
                }
            } // if
            else {
                explicitVocab = buildVocab(examples);
            }
        } // else
        Vocabulary vocabulary = new Vocabulary(explicitVocab);

        Random     random  = new Random(seed);
        GruWeights weights = resumed != null ? resumed.weights : randomInit(explicitVocab, vocabulary, random);

        if(checkGradients) {
            checkGradients(weights, vocabulary, examples, random, checkGradientSamples);
            return;
        }

        // NOTE on resume fidelity: `random` is re-seeded from `seed` (the checkpoint's own recorded
        // seed when resuming) and immediately used for this exact same shuffle+split below, so the
        // train/validation split itself IS reproduced exactly on resume (deterministic given the same
        // seed and the same examples file/order). What is NOT reproduced is the *epoch-by-epoch*
        // shuffle sequence beyond this point: java.util.Random's internal state past this call isn't
        // itself serialized, only the seed is -- so a resumed run's per-epoch example order diverges
        // from what an uninterrupted run would have done from this point on. This is a documented,
        // accepted limitation (STATE_AI.md), not a bug: resume continues training validly, it just
        // doesn't bit-reproduce a non-interrupted run.
        Collections.shuffle(examples, random);
        int           validationCount = Math.max( 1, examples.size() / 5 );
        List<Example> validation      = examples.subList(0, validationCount);
        List<Example> train           = examples.subList( validationCount, examples.size() );
        if( train.isEmpty() ) train = examples;

        // Decay horizon (total scheduled steps): stepsPerEpoch * maxEpochs, reusing --epochs rather
        // than a separate duration flag (see the --warmup-steps javadoc above). Computed here since
        // it needs train.size() (fixed at this point) and batchSize; recomputes identically on resume
        // from the same (resumable) train.size()/batchSize/maxEpochs, so it needs no persisted state
        // of its own.
        int stepsPerEpoch      = (train.size() + batchSize - 1) / batchSize;
        int totalScheduleSteps = stepsPerEpoch * maxEpochs;

        System.out.println( String.format(
                "GruTrainer: starting -- vocabSize=%d, trainExamples=%d, validationExamples=%d,"
                        + " maxEpochs=%d, patience=%d, lr=%9.7f, batchSize=%d, threads=%d,"
                        + " warmupSteps=%d, lrMin=%9.7f",
                explicitVocab.size(), train.size(), validation.size(), maxEpochs, patience,
                learningRate, batchSize, threads, warmupSteps, lrMin ) );

        AdamState adam               = resumed != null ? resumed.adam : new AdamState(weights);
        double    bestValidationLoss = resumed != null ? resumed.bestValidationLoss : Double.POSITIVE_INFINITY;
        // `weights` is mutated in place by `adam.apply` every step, so a plain `bestWeights =
        // weights` reference assignment would silently drift to whatever `weights` is by the end
        // of training instead of actually preserving the best-validation-loss epoch's numbers.
        // Snapshotting the serialized JSON immediately on improvement sidesteps needing a real
        // deep-copy of GruWeights' nested arrays.
        //
        // On resume, prefer the sibling best-weights checkpoint (see writeBestCheckpoint) if it's
        // readable -- it holds the actual best-validation-loss weight arrays, which the current-
        // weights checkpoint alone does not (that one only ever holds the LATEST epoch's weights).
        // If the sibling file is missing/unreadable, fall back to the resumed (latest-epoch, not
        // necessarily best) weights and loudly say so -- an accepted, documented gap rather than a
        // silent wrong result.
        String bestWeightsJson;
        if(resumed != null) {
            File bestCheckpointFile = deriveBestCheckpointFile( new File(resumePath) );
            String loadedBestJson = null;
            if( bestCheckpointFile.isFile() && bestCheckpointFile.canRead() ) {
                try {
                    LoadedWeights bestLoaded = readBestCheckpoint(bestCheckpointFile);
                    loadedBestJson = toJson(bestLoaded.weights, bestLoaded.explicitVocab);
                }
                catch(IOException e) {
                    System.err.println( "GruTrainer: could not read sibling best-checkpoint "
                            + bestCheckpointFile + ": " + e.getMessage()
                            + " -- falling back to the resumed (latest-epoch) weights as the interim"
                            + " best-so-far snapshot." );
                }
            } // if
            if(loadedBestJson != null) {
                bestWeightsJson = loadedBestJson;
            }
            else {
                System.err.println( "GruTrainer: no readable sibling best-checkpoint found at "
                        + bestCheckpointFile + " -- using the resumed (latest-epoch, not necessarily"
                        + " best) weights as the interim best-so-far snapshot until the next real"
                        + " validation improvement." );
                bestWeightsJson = toJson(weights, explicitVocab);
            }
        } // if
        else {
            bestWeightsJson = toJson(weights, explicitVocab);
        }
        int  epochsSinceImprovement = resumed != null ? resumed.epochsSinceImprovement : 0;
        int  step                   = resumed != null ? resumed.step : 0;
        int  startEpoch             = resumed != null ? resumed.epoch + 1 : 1;
        long trainingStartNanos     = System.nanoTime();

        File currentCheckpointFile = new File( weightsOut.getAbsolutePath() + CHECKPOINT_CURRENT_SUFFIX );
        File bestCheckpointFile    = deriveBestCheckpointFile(currentCheckpointFile);

        ExecutorService executor = threads > 1 ? Executors.newFixedThreadPool(threads) : null;
        try {
            for(int epoch = startEpoch; epoch <= maxEpochs; ++epoch) {
                Collections.shuffle(train, random);
                double trainLoss       = 0.0;
                long   epochStartNanos = System.nanoTime();
                int    examplesSeen    = 0;
                int    i               = 0;
                // Last scheduled LR actually applied this epoch (stays == learningRate flat when
                // warmupSteps == 0, i.e. schedule disabled) -- reported on the epoch summary line.
                double lrThisEpoch     = learningRate;
                while( i < train.size() ) {
                    // batchSize controls averaging granularity (one Adam step per this many
                    // examples); threads (passed to computeBatch's executor) controls how many of
                    // this batch's forward/backward computations run in parallel -- an orthogonal,
                    // composing axis, not a competing one. See the --threads/--batch-size javadoc
                    // above.
                    int           batchEnd = Math.min( i + batchSize, train.size() );
                    List<Example> batch    = train.subList(i, batchEnd);
                    i = batchEnd;
                    List<ComputedGradient> computed = computeBatch(
                        executor, weights, vocabulary, batch
                    );
                    GruClassifier.Gradients averaged = averageGradients(computed);
                    if(averaged != null) {
                        ++step;
                        lrThisEpoch = computeScheduledLr(
                            learningRate, lrMin, step, warmupSteps, totalScheduleSteps
                        );
                        adam.apply(weights, averaged, lrThisEpoch, step);
                    }
                    for( ComputedGradient result : computed ) {
                        ++examplesSeen;
                        if(result != null) trainLoss += result.loss;
                        if(progressEvery > 0 && examplesSeen % progressEvery == 0) printProgress(
                            epoch,
                            examplesSeen,
                            train.size(),
                            trainLoss,
                            epochStartNanos,
                            trainingStartNanos
                        );
                    } // for
                } // while
                trainLoss /= train.size();

                // Validation never mutates `weights`, so parallelizing it (unlike training) carries
                // no staleness tradeoff at all -- every example is scored against the exact same
                // frozen snapshot regardless of thread count, and losses are summed back in
                // original list order, so the result is identical to running it sequentially
                double validationLoss = 0.0;
                int    vi             = 0;
                while( vi < validation.size() ) {
                    int           vEnd  = Math.min( vi + threads, validation.size() );
                    List<Example> batch = validation.subList(vi, vEnd);
                    vi = vEnd;
                    List<Double> losses = computeValidationBatch(
                        executor, weights, vocabulary, batch
                    );
                    for(Double loss : losses) {
                        if(loss != null) validationLoss += loss;
                    }
                } // while
                validationLoss /= validation.size();

                double epochSeconds = ( System.nanoTime() - epochStartNanos ) / 1e9;
                double totalSeconds = ( System.nanoTime() - trainingStartNanos ) / 1e9;
                System.out.println( String.format(
                        "GruTrainer: epoch %2d, trainLoss=%9.7f, validationLoss=%9.7f, lr=%9.7f, "
                                + "epochSeconds=%6.1f, totalElapsedSeconds=%8.1f",
                        epoch, trainLoss, validationLoss, lrThisEpoch, epochSeconds, totalSeconds) );

                if(validationLoss < bestValidationLoss) {
                    bestValidationLoss     = validationLoss;
                    bestWeightsJson        = toJson(weights, explicitVocab);
                    epochsSinceImprovement = 0;
                    try {
                        writeBestCheckpoint(bestCheckpointFile, weights, explicitVocab, bestValidationLoss);
                    }
                    catch(IOException e) {
                        // A failed checkpoint write must never abort a real training run -- it's a
                        // safety net, not the run's actual deliverable (the final JSON weights file
                        // still gets written normally at the end). Warn and keep training.
                        System.err.println( "GruTrainer: warning -- could not write best-weights"
                                + " checkpoint to " + bestCheckpointFile + ": " + e.getMessage() );
                    }
                }
                else {
                    ++epochsSinceImprovement;
                    if(epochsSinceImprovement >= patience) {
                        System.out.println("GruTrainer: early stopping at epoch " + epoch
                                + " (no validation improvement for " + patience + " epochs)");
                        // Persist the final epoch/patience state to the current-weights checkpoint too,
                        // same as every other epoch below -- otherwise a resume from right after an
                        // early stop would see stale epochsSinceImprovement from the second-to-last
                        // epoch's checkpoint write.
                        writeCurrentCheckpointQuietly(
                            currentCheckpointFile, weights, explicitVocab, adam, epoch,
                            epochsSinceImprovement, bestValidationLoss, learningRate, maxEpochs,
                            patience, seed, step, batchSize, warmupSteps, lrMin
                        );
                        break;
                    }
                }

                // Current-weights checkpoint: overwritten every epoch (win or not), holding full
                // resumable state -- see writeCurrentCheckpoint's javadoc for the exact binary layout.
                writeCurrentCheckpointQuietly(
                    currentCheckpointFile, weights, explicitVocab, adam, epoch, epochsSinceImprovement,
                    bestValidationLoss, learningRate, maxEpochs, patience, seed, step, batchSize,
                    warmupSteps, lrMin
                );
            } // for epoch
        }
        finally {
            if(executor != null) executor.shutdown();
        }

        byte[] weightsBytes     = bestWeightsJson.getBytes(StandardCharsets.UTF_8);
        File   actualWeightsOut = weightsOut;
        try {
            Files.write( weightsOut.toPath(), weightsBytes );
        }
        catch(IOException e) {
            System.err.println( "GruTrainer: could not write output weights file " + args[1] + ": "
                    + e.getMessage() );
            // Training can take minutes to hours; losing the trained weights entirely because the
            // final write failed (disk full, bad path, permissions) is far worse than writing them
            // somewhere unintended, so fall back to a /tmp path and loudly announce it rather than
            // exiting empty-handed
            File fallback = new File(
                System.getProperty("java.io.tmpdir"),
                "gru-weights-fallback-" + System.currentTimeMillis() + ".json"
            );
            try {
                Files.write( fallback.toPath(), weightsBytes );
                System.err.println("GruTrainer: wrote fallback copy of trained weights to " + fallback
                        + " -- move it to a permanent location yourself, it will not be cleaned up"
                        + " automatically, and rerun with a valid output path next time.");
                actualWeightsOut = fallback;
            }
            catch(IOException fallbackError) {
                System.err.println( "GruTrainer: fallback write to " + fallback + " also failed: "
                        + fallbackError.getMessage() + " -- trained weights lost." );
                System.exit(2);
                return;
            }
            System.exit(1);
            return;
        }

        // Normal successful completion: the final JSON weights file is now safely on disk, so the
        // binary checkpoints (a resume/recovery safety net, never a persistent artifact -- see
        // STATE_AI.md) are no longer needed. Best-effort delete -- a leftover checkpoint file after a
        // successful run is harmless clutter, not worth failing the run over.
        try {
            Files.deleteIfExists( currentCheckpointFile.toPath() );
            Files.deleteIfExists( bestCheckpointFile.toPath() );
        }
        catch(IOException e) {
            System.err.println( "GruTrainer: warning -- could not delete checkpoint file(s) after"
                    + " successful completion: " + e.getMessage() );
        }

        System.out.println( String.format(
                "GruTrainer: wrote trained weights file to '%s' (vocabSize=%d, trainExamples=%d,"
                        + " validationExamples=%d, bestValidationLoss=%9.7f)",
                actualWeightsOut, explicitVocab.size(), train.size(), validation.size(), bestValidationLoss ) );

        printConfusionMatrix(actualWeightsOut, vocabulary, validation);
    }

    /**
     * Reloads the just-written best-validation-loss weights file and reports a binary confusion
     *  matrix (positive class = YES) plus precision/recall/F1 against the held-out validation split.
     *  Ground truth is always YES or NO (ABSTAIN is never a training label -- see the class javadoc),
     *  but the trained softmax has a third ABSTAIN output slot per {@link GruClassifier#CLASS_ORDER},
     *  so a prediction landing on ABSTAIN is counted here as simply "not predicted YES".
     */
    private static void printConfusionMatrix(
        File          weightsFile,
        Vocabulary    vocabulary,
        List<Example> validation
    )
    {
        GruWeights bestWeights;
        try {
            bestWeights = GruWeights.load( weightsFile.toPath() );
        }
        catch(IOException e) {
            System.err.println( "GruTrainer: could not reload " + weightsFile
                    + " to report confusion matrix: " + e.getMessage() );
            return;
        }

        int truePositive = 0, falsePositive = 0, trueNegative = 0, falseNegative = 0;
        for(Example example : validation) {
            if( example.targetWordIndex < 0 || example.targetWordIndex >= example.tokens.size() ) continue;
            GruClassifier.ForwardCache cache =
                    GruClassifier.forward(
                        bestWeights, vocabulary, example.tokens, example.targetWordIndex
                    );
            boolean predictedYes = argmax(cache.logits) == 0;
            boolean actualYes    = example.classIndex == 0;
            if(predictedYes && actualYes) truePositive++;
            else if(predictedYes)         falsePositive++;
            else if(actualYes)            falseNegative++;
            else                          trueNegative++;
        } // for

        double precision = truePositive + falsePositive == 0 ? 0.0
                : (double) truePositive / (truePositive + falsePositive);
        double recall = truePositive + falseNegative == 0 ? 0.0
                : (double) truePositive / (truePositive + falseNegative);
        double f1 = precision + recall == 0 ? 0.0 : 2 * precision * recall / (precision + recall);

        System.out.println( String.format(
                "GruTrainer: validation confusion matrix (positive=YES): tp=%d, fp=%d, tn=%d, fn=%d,"
                        + " precision=%7.5f, recall=%7.5f, f1=%7.5f",
                truePositive, falsePositive, trueNegative, falseNegative, precision, recall, f1) );
    }

    private static int argmax(double[] values)
    {
        int best = 0;
        for(int i = 1; i < values.length; ++i) {
            if( values[i] > values[best] ) best = i;
        }

        return best;
    }

    /**
     * Diagnostic-only gradient check (see {@code --check-gradients} in {@link #main}): picks one
     *  random labeled example, runs forward+backward once to get {@link GruClassifier.Gradients},
     *  then for a representative sample of weight arrays (dense layer, output layer, one direction's
     *  Wz, and the embedding rows the example actually touches) perturbs each sampled entry by
     *  +/-epsilon, recomputes the loss, and compares the resulting numeric derivative against
     *  backward()'s analytic one. Never used during normal training -- exists purely to build
     *  confidence in backward() before further changes rely on it.
     */
    private static void checkGradients(
        GruWeights    weights,
        Vocabulary    vocabulary,
        List<Example> examples,
        Random        random,
        int           samplesPerArray
    )
    {
        Example example  = examples.get( random.nextInt( examples.size() ) );
        int     attempts = 0;
        while( ( example.targetWordIndex < 0 || example.targetWordIndex >= example.tokens.size() )
                && attempts < examples.size() ) {
            example = examples.get( random.nextInt( examples.size() ) );
            ++attempts;
        }
        if( example.targetWordIndex < 0 || example.targetWordIndex >= example.tokens.size() ) {
            System.err.println(
                "GruTrainer: no example with an in-range targetWordIndex found for gradient check"
            );
            System.exit(2);
            return;
        } // if

        List<String> tokens          = example.tokens;
        int          targetWordIndex = example.targetWordIndex;
        int          classIndex      = example.classIndex;
        GruClassifier.ForwardCache cache = GruClassifier.forward(
            weights, vocabulary, tokens, targetWordIndex
        );
        GruClassifier.Gradients gradients = GruClassifier.backward(weights, cache, classIndex);

        double epsilon = 1e-5;
        System.out.println( String.format(
                "GruTrainer: gradient check against example label=%s -- epsilon=%9.7f,"
                        + " samplesPerArray=%d",
                example.label, epsilon, samplesPerArray ) );
        double maxRelError = 0.0;
        maxRelError = Math.max(
            maxRelError, checkArray2D("denseW", weights.denseW, gradients.denseW, weights, vocabulary, tokens, targetWordIndex, classIndex, random, epsilon, samplesPerArray)
        );
        maxRelError = Math.max(
            maxRelError, checkArray1D("denseB", weights.denseB, gradients.denseB, weights, vocabulary, tokens, targetWordIndex, classIndex, random, epsilon, samplesPerArray)
        );
        maxRelError = Math.max(
            maxRelError, checkArray2D("outW", weights.outW, gradients.outW, weights, vocabulary, tokens, targetWordIndex, classIndex, random, epsilon, samplesPerArray)
        );
        maxRelError = Math.max(
            maxRelError, checkArray1D("outB", weights.outB, gradients.outB, weights, vocabulary, tokens, targetWordIndex, classIndex, random, epsilon, samplesPerArray)
        );
        maxRelError = Math.max(
            maxRelError, checkArray2D("forward.Wz", weights.forward.Wz, gradients.forward.Wz, weights, vocabulary, tokens, targetWordIndex, classIndex, random, epsilon, samplesPerArray)
        );
        for( Map.Entry<Integer, double[]> entry : gradients.embeddingGrad.entrySet() ) {
            int row = entry.getKey();
            maxRelError = Math.max(
                maxRelError, checkArray1D( "embeddings[" + row + "]", weights.embeddings[row], entry.getValue(), weights, vocabulary, tokens, targetWordIndex, classIndex, random, epsilon, samplesPerArray )
            );
        }

        System.out.println( String.format( "GruTrainer: gradient check complete -- maxRelativeError=%9.6f",
                maxRelError ) + (maxRelError < 1e-2 ? " (PASS)" : " (FAIL)") );
        System.exit(maxRelError < 1e-2 ? 0 : 1);
    }

    private static double checkArray2D(
        String       name,
        double[][]   param,
        double[][]   grad,
        GruWeights   weights,
        Vocabulary   vocabulary,
        List<String> tokens,
        int          targetWordIndex,
        int          classIndex,
        Random       random,
        double       epsilon,
        int          samples
    )
    {
        double maxRelError = 0.0;
        for(int s = 0; s < samples; ++s) {
            int    i        = random.nextInt(param.length);
            int    j        = random.nextInt( param[i].length );
            double relError = checkOneEntry(
                name + "[" + i + "][" + j + "]",
                param[i],
                j,
                grad[i][j],
                weights,
                vocabulary,
                tokens,
                targetWordIndex,
                classIndex,
                epsilon
            );
            maxRelError = Math.max(maxRelError, relError);
        } // for

        return maxRelError;
    }

    private static double checkArray1D(
        String       name,
        double[]     param,
        double[]     grad,
        GruWeights   weights,
        Vocabulary   vocabulary,
        List<String> tokens,
        int          targetWordIndex,
        int          classIndex,
        Random       random,
        double       epsilon,
        int          samples
    )
    {
        double maxRelError = 0.0;
        for(int s = 0; s < samples; ++s) {
            int    i        = random.nextInt(param.length);
            double relError = checkOneEntry(
                name + "[" + i + "]",
                param,
                i,
                grad[i],
                weights,
                vocabulary,
                tokens,
                targetWordIndex,
                classIndex,
                epsilon
            );
            maxRelError = Math.max(maxRelError, relError);
        } // for

        return maxRelError;
    }

    private static double checkOneEntry(
        String       label,
        double[]     param,
        int          index,
        double       analytic,
        GruWeights   weights,
        Vocabulary   vocabulary,
        List<String> tokens,
        int          targetWordIndex,
        int          classIndex,
        double       epsilon
    )
    {
        double original = param[index];
        param[index] = original + epsilon;
        double lossPlus = crossEntropyLoss(
            GruClassifier.forward(weights, vocabulary, tokens, targetWordIndex).logits, classIndex
        );
        param[index] = original - epsilon;
        double lossMinus = crossEntropyLoss(
            GruClassifier.forward(weights, vocabulary, tokens, targetWordIndex).logits, classIndex
        );
        param[index] = original;

        double numeric  = (lossPlus - lossMinus) / (2* epsilon);
        double relError = Math.abs(
            numeric - analytic
        ) / Math.max(
            1e-8, Math.abs(numeric) + Math.abs(analytic)
        );
        System.out.println( String.format(
                "GruTrainer:   %s analytic=%9.6f, numeric=%9.6f, relError=%9.6f",
                label, analytic, numeric, relError ) );

        return relError;
    }

    /**
     * One example's computed loss + gradients, or the outcome of skipping an out-of-range
     *  {@code targetWordIndex} (never constructed for those -- see {@link #computeBatch})
     */
    private static final class ComputedGradient {

        final double loss;
        final GruClassifier.Gradients gradients;

        ComputedGradient(double loss, GruClassifier.Gradients gradients)
        {
            this.loss      = loss;
            this.gradients = gradients;
        }

    } // class ComputedGradient

    private static ComputedGradient computeGradient(
        GruWeights weights,
        Vocabulary vocabulary,
        Example    example
    )
    {
        GruClassifier.ForwardCache cache = GruClassifier.forward(
            weights, vocabulary, example.tokens, example.targetWordIndex
        );
        double loss = crossEntropyLoss(cache.logits, example.classIndex);
        GruClassifier.Gradients gradients = GruClassifier.backward(
            weights, cache, example.classIndex
        );
        clipGradients(gradients, GRADIENT_CLIP_NORM);

        return new ComputedGradient(loss, gradients);
    }

    /**
     * Computes forward+backward for one batch of examples, in parallel across {@code executor}'s
     *  worker threads when non-null (all against the same {@code weights} snapshot -- safe since
     *  {@link GruClassifier#forward}/{@link GruClassifier#backward} only read {@code weights}, never
     *  mutate it), sequentially otherwise. Result order matches {@code batch}'s order; entries for
     *  examples with an out-of-range {@code targetWordIndex} are {@code null} (skipped, matching the
     *  pre-existing single-threaded behavior of not computing anything for them).
     */
    private static List<ComputedGradient> computeBatch(
        ExecutorService executor,
        GruWeights      weights,
        Vocabulary      vocabulary,
        List<Example>   batch
    )
    {
        List<ComputedGradient> results = new ArrayList<>( batch.size() );
        if(executor == null) {
            for(Example example : batch) {
                if( example.targetWordIndex < 0 || example.targetWordIndex >= example.tokens.size() ) results.add(
                    null
                );
                else results.add( computeGradient(weights, vocabulary, example) );
            }
            return results;
        } // if
        List<Future<ComputedGradient>> futures = new ArrayList<>( batch.size() );
        for(final Example example : batch) {
            if( example.targetWordIndex < 0 || example.targetWordIndex >= example.tokens.size() ) {
                futures.add(null);
            }
            else {
                futures.add( executor.submit( new Callable<ComputedGradient>() {
                    @Override
                    public ComputedGradient call()
                    {
                        return computeGradient(weights, vocabulary, example);
                    }
                } ) );
            }
        } // for
        for(Future<ComputedGradient> future : futures) {
            if(future == null) {
                results.add(null);
                continue;
            }
            try {
                results.add( future.get() );
            }
            catch(InterruptedException | ExecutionException e) {
                throw new RuntimeException("GruTrainer: parallel gradient computation failed", e);
            }
        } // for

        return results;
    }

    /**
     * Same parallelization strategy as {@link #computeBatch}, but for validation (loss only, no
     *  gradients/Adam application) -- see the call site's comment on why this carries no staleness
     *  tradeoff, unlike training
     */
    private static List<Double> computeValidationBatch(
        ExecutorService executor,
        GruWeights      weights,
        Vocabulary      vocabulary,
        List<Example>   batch
    )
    {
        List<Double> results = new ArrayList<>( batch.size() );
        if(executor == null) {
            for(Example example : batch) {
                if( example.targetWordIndex < 0 || example.targetWordIndex >= example.tokens.size() ) {
                    results.add(null);
                }
                else {
                    GruClassifier.ForwardCache cache = GruClassifier.forward(
                        weights, vocabulary, example.tokens, example.targetWordIndex
                    );
                    results.add( crossEntropyLoss(cache.logits, example.classIndex) );
                }
            } // for
            return results;
        } // if
        List<Future<Double>> futures = new ArrayList<>( batch.size() );
        for(final Example example : batch) {
            if( example.targetWordIndex < 0 || example.targetWordIndex >= example.tokens.size() ) {
                futures.add(null);
            }
            else {
                futures.add( executor.submit( new Callable<Double>() {
                    @Override
                    public Double call()
                    {
                        GruClassifier.ForwardCache cache = GruClassifier.forward(weights, vocabulary, example.tokens, example.targetWordIndex);

                        return crossEntropyLoss(cache.logits, example.classIndex);
                    }
                } ) );
            }
        } // for
        for(Future<Double> future : futures) {
            if(future == null) {
                results.add(null);
                continue;
            }
            try {
                results.add( future.get() );
            }
            catch(InterruptedException | ExecutionException e) {
                throw new RuntimeException("GruTrainer: parallel validation computation failed", e);
            }
        } // for

        return results;
    }

    /**
     * Averages (not sums) the non-null {@link ComputedGradient}s of one mini-batch into a single
     *  {@link GruClassifier.Gradients}, mutating and returning the first non-null entry's own
     *  gradients object as the accumulator (cheaper than allocating a fresh one -- {@code
     *  GruClassifier.Gradients}'s constructor is package-private and not callable from here anyway).
     *  Entries skipped by {@link #computeBatch} (out-of-range {@code targetWordIndex}) are excluded
     *  from both the sum and the divisor, so a mini-batch containing one or more skipped examples
     *  still averages correctly over its real example count -- the same logic naturally handles the
     *  last, possibly-partial batch of an epoch (divides by however many examples it actually
     *  contains, never a hardcoded {@code batchSize}). Returns {@code null} if every entry in the
     *  batch was skipped, matching the pre-mini-batch behavior of applying no Adam step for an
     *  all-skipped batch.
     */
    private static GruClassifier.Gradients averageGradients(List<ComputedGradient> computed)
    {
        GruClassifier.Gradients sum   = null;
        int                     count = 0;
        for(ComputedGradient result : computed) {
            if(result == null) continue;
            if(sum == null) sum = result.gradients;
            else addGradientsInto(sum, result.gradients);
            ++count;
        } // for
        if(sum == null) return null;
        scaleGradients( sum, 1.0 / count );

        return sum;
    }

    private static void addGradientsInto(GruClassifier.Gradients dst, GruClassifier.Gradients src)
    {
        for( Map.Entry<Integer, double[]> entry : src.embeddingGrad.entrySet() ) {
            double[] existing = dst.embeddingGrad.get( entry.getKey() );
            if(existing == null) dst.embeddingGrad.put( entry.getKey(), entry.getValue().clone() );
            else addInto( existing, entry.getValue() );
        }
        addInto(dst.forward, src.forward);
        addInto(dst.backward, src.backward);
        addInto(dst.denseW, src.denseW);
        addInto(dst.denseB, src.denseB);
        addInto(dst.outW, src.outW);
        addInto(dst.outB, src.outB);
    }

    private static void addInto(GruWeights.DirectionWeights dst, GruWeights.DirectionWeights src)
    {
        addInto(dst.Wz, src.Wz);
        addInto(dst.Wr, src.Wr);
        addInto(dst.Wh, src.Wh);
        addInto(dst.Uz, src.Uz);
        addInto(dst.Ur, src.Ur);
        addInto(dst.Uh, src.Uh);
        addInto(dst.bz, src.bz);
        addInto(dst.br, src.br);
        addInto(dst.bh, src.bh);
    }

    private static void addInto(double[][] dst, double[][] src)
    {
        for(int i = 0; i < dst.length; ++i) addInto( dst[i], src[i] );
    }

    private static void addInto(double[] dst, double[] src)
    {
        for(int i = 0; i < dst.length; ++i) dst[i] += src[i];
    }

    /** Same field walk as {@link #clipGradients}'s scaling half, reused here to divide a summed
     *  mini-batch gradient by its example count.
     */
    private static void scaleGradients(GruClassifier.Gradients g, double factor)
    {
        for( double[] row : g.embeddingGrad.values() ) scale(row, factor);

        scale(g.forward, factor);
        scale(g.backward, factor);
        scale(g.denseW, factor);
        scale(g.denseB, factor);
        scale(g.outW, factor);
        scale(g.outB, factor);
    }

    /**
     * Learning-rate warmup + cosine decay schedule (see {@code --warmup-steps}/{@code --lr-min} in
     *  {@link #main}). {@code warmupSteps <= 0} disables the schedule entirely -- returns {@code
     *  baseLr} unconditionally, byte-for-byte the pre-schedule flat-lr behavior, so an unmodified
     *  invocation (default {@code --warmup-steps=0}) is unaffected. Otherwise: linear ramp from 0 to
     *  {@code baseLr} over {@code [1, warmupSteps]}, then a cosine decay from {@code baseLr} down to
     *  {@code lrMin} over the remaining steps up to {@code totalSteps} (clamped to {@code lrMin} past
     *  {@code totalSteps}, e.g. if early stopping never reaches it). {@code step} is the 1-based Adam
     *  step counter (same counter used for bias-correction), so this is step-granular, matching
     *  mini-batch training's one-Adam-step-per-batch granularity rather than per-epoch.
     */
    private static double computeScheduledLr(
        double baseLr, double lrMin, int step, int warmupSteps, int totalSteps
    )
    {
        if(warmupSteps <= 0) return baseLr;
        if(step <= warmupSteps) return baseLr * step / (double) warmupSteps;

        int    decaySteps = Math.max( 1, totalSteps - warmupSteps );
        double progress    = Math.min( 1.0, (step - warmupSteps) / (double) decaySteps );

        return lrMin + 0.5 * (baseLr - lrMin) * ( 1 + Math.cos(Math.PI * progress) );
    }

    /**
     * Mid-epoch progress line: examples-seen/total, running average train loss so far this epoch,
     *  elapsed time this epoch, and a rough estimated-time-remaining for the epoch based on the
     *  average per-example rate observed so far. Printed via {@code System.out.println}, which
     *  auto-flushes on newline for the console/redirected-file stdout case this is meant for (a
     *  human tailing the process's own manual invocation, per the 2026-07-29 request), so no
     *  explicit {@code System.out.flush()} is needed.
     */
    private static void printProgress(
        int    epoch,
        int    examplesSeen,
        int    totalExamples,
        double lossSoFar,
        long   epochStartNanos,
        long   trainingStartNanos
    )
    {
        double elapsedSeconds      = ( System.nanoTime() - epochStartNanos ) / 1e9;
        double perExampleSeconds   = elapsedSeconds / examplesSeen;
        double etaSeconds          = perExampleSeconds* (totalExamples - examplesSeen);
        double totalElapsedSeconds = ( System.nanoTime() - trainingStartNanos ) / 1e9;
        System.out.println( String.format(
                "GruTrainer: epoch %2d, progress %6d/%6d (%4.1f%%), avgTrainLoss=%7.5f, "
                        + "epochElapsedSeconds=%6.1f, epochEtaSeconds=%6.1f, totalElapsedSeconds=%8.1f",
                epoch, examplesSeen, totalExamples, 100.0 * examplesSeen / totalExamples,
                lossSoFar / examplesSeen, elapsedSeconds, etaSeconds, totalElapsedSeconds) );
    }

    private static final class Example {

        final String label;
        final int    classIndex;
        final int    targetWordIndex;
        final String text;
        /**
         * Tokenized once at load time (and truncated to {@code SEQUENCE_CAP}, matching what the
         *  per-epoch code previously recomputed on every pass) rather than re-tokenized on every
         *  epoch -- the text/tokenization never changes across epochs, only the weights do
         */
        final List<String> tokens;

        Example(String label, int classIndex, int targetWordIndex, String text)
        {
            this.label           = label;
            this.classIndex      = classIndex;
            this.targetWordIndex = targetWordIndex;
            this.text            = text;
            List<String> tokenized = GruClassifier.tokenize(text);
            if( tokenized.size() > GruClassifier.SEQUENCE_CAP ) tokenized = tokenized.subList(
                0, GruClassifier.SEQUENCE_CAP
            );
            this.tokens = tokenized;
        }

    } // class Example

    /**
     * Parses RDD_EXT_20/RDD_EXT_21's labeled-examples schema:
     *  {@code <lang>\t<label:YES|NO>\t<targetWordIndex>\t<escaped-text>}. Lines starting with
     *  {@code #} (comments) and blank lines are skipped, matching {@code sample_examples.txt}'s
     *  own illustrative-shape convention.
     */
    private static List<Example> readExamples(File examplesFile) throws IOException
    {
        List<Example> examples = new ArrayList<>();
        for( String line : Files.readAllLines( examplesFile.toPath(), StandardCharsets.UTF_8 ) ) {
            if( line.isEmpty() || line.startsWith("#") ) continue;
            String[] parts = line.split("\t", 4);
            if(parts.length != 4) continue;
            String label = parts[1];
            if( !label.equals("YES") && !label.equals("NO") ) continue;
            int classIndex = label.equals("YES") ? 0 : 1; // GruClassifier.CLASS_ORDER: YES=0, NO=1
            int targetWordIndex;
            try {
                targetWordIndex = Integer.parseInt( parts[2] );
            }
            catch(NumberFormatException e) {
                continue;
            }
            String text = unescape( parts[3] );
            examples.add( new Example(label, classIndex, targetWordIndex, text) );
        } // for

        return examples;
    }

    private static String unescape(String s)
    {
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < s.length(); ++i ) {
            char c = s.charAt(i);
            if( c == '\\' && i + 1 < s.length() ) {
                char next = s.charAt(i + 1);
                if(next == 'n') {
                    sb.append('\n');
                    ++i;
                    continue;
                }
                else if(next == 't') {
                    sb.append('\t');
                    ++i;
                    continue;
                }
                else if(next == '\\') {
                    sb.append('\\');
                    ++i;
                    continue;
                }
            } // if
            sb.append(c);
        } // for

        return sb.toString();
    }

    private static List<String> readVocab(File vocabFile) throws IOException
    {
        List<String> words = new ArrayList<>();
        for( String line : Files.readAllLines( vocabFile.toPath(), StandardCharsets.UTF_8 ) ) {
            String trimmed = line.trim();
            if( trimmed.isEmpty() || trimmed.startsWith("#") ) continue;
            words.add(trimmed);
        }

        return words;
    }

    private static List<String> buildVocab(List<Example> examples)
    {
        Set<String> seen = new LinkedHashSet<>();
        for(Example example : examples) {
            for( String token : GruClassifier.tokenize(example.text) ) seen.add(token);
        }

        return new ArrayList<>(seen);
    }

    private static double crossEntropyLoss(double[] logits, int trueClassIndex)
    {
        double[] probabilities = GruClassifier.softmax(logits);

        return -Math.log( Math.max( probabilities[trueClassIndex], 1e-12 ) );
    }

    private static GruWeights randomInit(
        List<String> explicitVocab,
        Vocabulary   vocabulary,
        Random       random
    )
    {
        int        embeddingRows = vocabulary.totalEmbeddingRows();
        double[][] embeddings    = new double[embeddingRows][EMBEDDING_DIM];
        for(int i = 0; i < embeddingRows; ++i) {
            for(int j = 0; j < EMBEDDING_DIM; ++j) embeddings[i][j] = glorot(
                random, EMBEDDING_DIM, EMBEDDING_DIM
            );
        }

        GruWeights.DirectionWeights forward = randomDirectionWeights(random);
        GruWeights.DirectionWeights backward = randomDirectionWeights(random);

        double[][] denseW = new double[DENSE_SIZE][2* HIDDEN_SIZE];
        for(int i = 0; i < DENSE_SIZE; ++i) {
            for(int j = 0; j < 2 * HIDDEN_SIZE; ++j) denseW[i][j] = glorot(
                random, 2 * HIDDEN_SIZE, DENSE_SIZE
            );
        }
        double[] denseB = new double[DENSE_SIZE];

        double[][] outW = new double[GruClassifier.CLASS_ORDER.length][DENSE_SIZE];
        for(int i = 0; i < outW.length; ++i) {
            for(int j = 0; j < DENSE_SIZE; ++j) outW[i][j] = glorot(
                random, DENSE_SIZE, outW.length
            );
        }
        double[] outB = new double[GruClassifier.CLASS_ORDER.length];

        return new GruWeightsBuilder()
                .schemaVersion(GruWeights.CURRENT_SCHEMA_VERSION)
                .vocabSize( explicitVocab.size() )
                .hashBuckets(GruClassifier.HASH_BUCKETS)
                .embeddingDim(EMBEDDING_DIM)
                .hiddenSize(HIDDEN_SIZE)
                .sequenceCap(GruClassifier.SEQUENCE_CAP)
                .numClasses(GruClassifier.CLASS_ORDER.length)
                .abstainThreshold(ABSTAIN_THRESHOLD)
                .explicitVocab( explicitVocab.toArray( new String[0] ) )
                .embeddings(embeddings)
                .forward(forward)
                .backward(backward)
                .denseW(denseW)
                .denseB(denseB)
                .outW(outW)
                .outB(outB)
                .build();
    }

    private static GruWeights.DirectionWeights randomDirectionWeights(Random random)
    {
        GruWeights.DirectionWeights weights = GruWeights.DirectionWeights.zeros(
            HIDDEN_SIZE, EMBEDDING_DIM
        );
        fillGlorot(weights.Wz, random, EMBEDDING_DIM, HIDDEN_SIZE);
        fillGlorot(weights.Wr, random, EMBEDDING_DIM, HIDDEN_SIZE);
        fillGlorot(weights.Wh, random, EMBEDDING_DIM, HIDDEN_SIZE);
        fillGlorot(weights.Uz, random, HIDDEN_SIZE, HIDDEN_SIZE);
        fillGlorot(weights.Ur, random, HIDDEN_SIZE, HIDDEN_SIZE);
        fillGlorot(weights.Uh, random, HIDDEN_SIZE, HIDDEN_SIZE);

        return weights;
    }

    private static void fillGlorot(double[][] matrix, Random random, int fanIn, int fanOut)
    {
        for( double[] row : matrix ) {
            for(int j = 0; j < row.length; ++j) row[j] = glorot(random, fanIn, fanOut);
        }
    }

    private static double glorot(Random random, int fanIn, int fanOut)
    {
        double limit = Math.sqrt( 6.0 / (fanIn + fanOut) );

        return ( random.nextDouble() * 2 - 1 ) * limit;
    }

    /**
     * Clips the entire gradient set to the specified global L2 norm. This is the
     *  standard "global norm" clipping used by most GRU/LSTM implementations rather
     *  than clipping each tensor independently, since it preserves the direction of
     *  the update while only reducing its magnitude when necessary.
     */
    private static void clipGradients(GruClassifier.Gradients gradients, double maxNorm)
    {
        double sumSquares = 0.0;

        for( double[] row : gradients.embeddingGrad.values() ) {
            for(double v : row) sumSquares += v * v;
        }

        sumSquares += sumSquares(gradients.forward);
        sumSquares += sumSquares(gradients.backward);
        sumSquares += sumSquares(gradients.denseW);
        sumSquares += sumSquares(gradients.denseB);
        sumSquares += sumSquares(gradients.outW);
        sumSquares += sumSquares(gradients.outB);

        double norm = Math.sqrt(sumSquares);
        if(norm <= maxNorm || norm == 0.0) return;

        double scale = maxNorm / norm;

        for( double[] row : gradients.embeddingGrad.values() ) scale(row, scale);

        scale(gradients.forward, scale);
        scale(gradients.backward, scale);
        scale(gradients.denseW, scale);
        scale(gradients.denseB, scale);
        scale(gradients.outW, scale);
        scale(gradients.outB, scale);
    }

    private static double sumSquares(GruWeights.DirectionWeights w)
    {
        return sumSquares(w.Wz) + sumSquares(w.Wr) + sumSquares(w.Wh)
                + sumSquares(w.Uz) + sumSquares(w.Ur) + sumSquares(w.Uh)
                + sumSquares(w.bz) + sumSquares(w.br) + sumSquares(w.bh);
    }

    private static double sumSquares(double[][] m)
    {
        double s = 0.0;
        for( double[] row : m ) s += sumSquares(row);

        return s;
    }

    private static double sumSquares(double[] v)
    {
        double s = 0.0;
        for(double x : v) s += x * x;

        return s;
    }

    private static void scale(GruWeights.DirectionWeights w, double factor)
    {
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

    private static void scale(double[][] m, double factor)
    {
        for( double[] row : m ) scale(row, factor);
    }

    private static void scale(double[] v, double factor)
    {
        for(int i = 0; i < v.length; ++i) v[i] *= factor;
    }

    /**
     * Adam optimizer state (first/second moment estimates), one pair of accumulator arrays per
     *  trained-weight array, mirroring {@link GruWeights}'s field layout exactly so each weight
     *  array's update can be applied in lockstep with its gradient. Embedding-row moments are kept
     *  densely (one row per vocab+hash-bucket slot) even though any single example's gradient only
     *  touches a handful of rows -- the corpora here are small enough that this is simpler than a
     *  sparse moment table, and moment state must persist across examples/epochs regardless.
     */
    private static final class AdamState {

        private static final double BETA1   = 0.9;
        private static final double BETA2   = 0.999;
        private static final double EPSILON = 1e-8;

        final double[][] embeddingM,    embeddingV;
        final DirectionMoments forward, backward;
        final double[][] denseWM,       denseWV;
        final double[] denseBM,         denseBV;
        final double[][] outWM,         outWV;
        final double[] outBM,           outBV;

        AdamState(GruWeights weights)
        {
            embeddingM = new double[weights.embeddings.length][weights.embeddingDim];
            embeddingV = new double[weights.embeddings.length][weights.embeddingDim];
            forward    = new DirectionMoments(weights.hiddenSize, weights.embeddingDim);
            backward   = new DirectionMoments(weights.hiddenSize, weights.embeddingDim);
            denseWM    = new double[weights.denseW.length][ weights.denseW[0].length ];
            denseWV    = new double[weights.denseW.length][ weights.denseW[0].length ];
            denseBM    = new double[weights.denseB.length];
            denseBV    = new double[weights.denseB.length];
            outWM      = new double[weights.outW.length][ weights.outW[0].length ];
            outWV      = new double[weights.outW.length][ weights.outW[0].length ];
            outBM      = new double[weights.outB.length];
            outBV      = new double[weights.outB.length];
        }

        static final class DirectionMoments {

            final double[][] WzM, WzV, WrM, WrV, WhM, WhV;
            final double[][] UzM, UzV, UrM, UrV, UhM, UhV;
            final double[] bzM, bzV, brM, brV, bhM,   bhV;

            DirectionMoments(int hiddenSize, int embeddingDim)
            {
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

        } // class DirectionMoments

        void apply(GruWeights weights, GruClassifier.Gradients gradients, double lr, int step)
        {
            double biasCorrection1 = 1.0 - Math.pow(BETA1, step);
            double biasCorrection2 = 1.0 - Math.pow(BETA2, step);

            for( Map.Entry<Integer, double[]> entry : gradients.embeddingGrad.entrySet() ) {
                int row = entry.getKey();
                update1D(
                    weights.embeddings[row], entry.getValue(), embeddingM[row],
                    embeddingV[row], lr, biasCorrection1, biasCorrection2
                );
            } // for
            applyDirection(
                weights.forward, gradients.forward, forward, lr,
                biasCorrection1, biasCorrection2
            );
            applyDirection(
                weights.backward, gradients.backward, backward, lr,
                biasCorrection1, biasCorrection2
            );
            update2D(
                weights.denseW, gradients.denseW, denseWM, denseWV, lr,
                biasCorrection1, biasCorrection2
            );
            update1D(
                weights.denseB, gradients.denseB, denseBM, denseBV, lr,
                biasCorrection1, biasCorrection2
            );
            update2D(
                weights.outW, gradients.outW, outWM, outWV, lr,
                biasCorrection1, biasCorrection2
            );
            update1D(
                weights.outB, gradients.outB, outBM, outBV, lr,
                biasCorrection1, biasCorrection2
            );
        }

        private void applyDirection(
            GruWeights.DirectionWeights weights,
            GruWeights.DirectionWeights gradients,
            DirectionMoments            moments,
            double                      lr,
            double                      biasCorrection1,
            double                      biasCorrection2
        )
        {
            update2D(
                weights.Wz, gradients.Wz, moments.WzM, moments.WzV, lr,
                biasCorrection1, biasCorrection2
            );
            update2D(
                weights.Wr, gradients.Wr, moments.WrM, moments.WrV, lr,
                biasCorrection1, biasCorrection2
            );
            update2D(
                weights.Wh, gradients.Wh, moments.WhM, moments.WhV, lr,
                biasCorrection1, biasCorrection2
            );
            update2D(
                weights.Uz, gradients.Uz, moments.UzM, moments.UzV, lr,
                biasCorrection1, biasCorrection2
            );
            update2D(
                weights.Ur, gradients.Ur, moments.UrM, moments.UrV, lr,
                biasCorrection1, biasCorrection2
            );
            update2D(
                weights.Uh, gradients.Uh, moments.UhM, moments.UhV, lr,
                biasCorrection1, biasCorrection2
            );
            update1D(
                weights.bz, gradients.bz, moments.bzM, moments.bzV, lr,
                biasCorrection1, biasCorrection2
            );
            update1D(
                weights.br, gradients.br, moments.brM, moments.brV, lr,
                biasCorrection1, biasCorrection2
            );
            update1D(
                weights.bh, gradients.bh, moments.bhM, moments.bhV, lr,
                biasCorrection1, biasCorrection2
            );
        }

        private static void update1D(
            double[] param,
            double[] grad,
            double[] m,
            double[] v,
            double   lr,
            double   biasCorrection1,
            double   biasCorrection2
        )
        {
            for(int i = 0; i < param.length; ++i) {
                m[i] = BETA1 * m[i] + (1 - BETA1) * grad[i];
                v[i] = BETA2 * v[i] + (1 - BETA2) * grad[i] * grad[i];
                double mHat = m[i] / biasCorrection1;
                double vHat = v[i] / biasCorrection2;
                param[i] -= lr * mHat / ( Math.sqrt(vHat) + EPSILON );
            } // for
        }

        private static void update2D(
            double[][] param,
            double[][] grad,
            double[][] m,
            double[][] v,
            double     lr,
            double     biasCorrection1,
            double     biasCorrection2
        )
        {
            for(int i = 0; i < param.length; ++i) update1D(
                param[i], grad[i], m[i], v[i], lr, biasCorrection1, biasCorrection2
            );
        }

    } // class AdamState

    // ── Checkpoint binary I/O ────────────────────────────────────────────────────────────────────

    /** {@code weightsOut}-relative path where the current-weights checkpoint's sibling best-weights
     *  checkpoint lives, given the current-weights checkpoint's own {@code File}. Both are always
     *  written together from the same {@code weightsOut} base path (see the constant block above), so
     *  this is a plain suffix swap, not a search.
     */
    private static File deriveBestCheckpointFile(File currentCheckpointFile)
    {
        String path = currentCheckpointFile.getPath();
        if( path.endsWith(CHECKPOINT_CURRENT_SUFFIX) ) return new File(
            path.substring( 0, path.length() - CHECKPOINT_CURRENT_SUFFIX.length() ) + CHECKPOINT_BEST_SUFFIX
        );

        // Best-effort fallback for a --resume path that doesn't end in the expected suffix (e.g. a
        // user manually renamed the file) -- still deterministic, just less likely to find a real
        // sibling; readBestCheckpoint's caller already treats "no sibling found" as a handled,
        // documented case, not a hard error.
        return new File(path + CHECKPOINT_BEST_SUFFIX);
    }

    private static void writeDoubleArray(DataOutputStream out, double[] values) throws IOException
    {
        out.writeInt( values.length );
        for(double v : values) out.writeDouble(v);
    }

    private static double[] readDoubleArray(DataInputStream in) throws IOException
    {
        int      n      = in.readInt();
        double[] values = new double[n];
        for(int i = 0; i < n; ++i) values[i] = in.readDouble();

        return values;
    }

    private static void readDoubleArrayInto(DataInputStream in, double[] target) throws IOException
    {
        int n = in.readInt();
        if(n != target.length) throw new IOException(
            "checkpoint shape mismatch: expected length " + target.length + ", found " + n
        );
        for(int i = 0; i < n; ++i) target[i] = in.readDouble();
    }

    private static void writeDoubleArray2D(DataOutputStream out, double[][] rows) throws IOException
    {
        out.writeInt( rows.length );
        for( double[] row : rows ) writeDoubleArray(out, row);
    }

    private static double[][] readDoubleArray2D(DataInputStream in) throws IOException
    {
        int        n    = in.readInt();
        double[][] rows = new double[n][];
        for(int i = 0; i < n; ++i) rows[i] = readDoubleArray(in);

        return rows;
    }

    private static void readDoubleArray2DInto(DataInputStream in, double[][] target) throws IOException
    {
        int n = in.readInt();
        if(n != target.length) throw new IOException(
            "checkpoint shape mismatch: expected " + target.length + " rows, found " + n
        );
        for(int i = 0; i < n; ++i) readDoubleArrayInto( in, target[i] );
    }

    private static void writeDirectionWeights(
        DataOutputStream out, GruWeights.DirectionWeights w
    ) throws IOException
    {
        writeDoubleArray2D(out, w.Wz);
        writeDoubleArray2D(out, w.Wr);
        writeDoubleArray2D(out, w.Wh);
        writeDoubleArray2D(out, w.Uz);
        writeDoubleArray2D(out, w.Ur);
        writeDoubleArray2D(out, w.Uh);
        writeDoubleArray(out, w.bz);
        writeDoubleArray(out, w.br);
        writeDoubleArray(out, w.bh);
    }

    private static GruWeights.DirectionWeights readDirectionWeights(DataInputStream in) throws IOException
    {
        double[][] Wz = readDoubleArray2D(in);
        double[][] Wr = readDoubleArray2D(in);
        double[][] Wh = readDoubleArray2D(in);
        double[][] Uz = readDoubleArray2D(in);
        double[][] Ur = readDoubleArray2D(in);
        double[][] Uh = readDoubleArray2D(in);
        double[]   bz = readDoubleArray(in);
        double[]   br = readDoubleArray(in);
        double[]   bh = readDoubleArray(in);

        return new GruWeights.DirectionWeights(Wz, Wr, Wh, Uz, Ur, Uh, bz, br, bh);
    }

    /** Both checkpoint kinds share this block: schema/architecture scalars, the vocab (needed to
     *  reconstruct a {@link Vocabulary}), and every {@link GruWeights} array {@code toJsonFields}
     *  already enumerates -- same field list, not a separately-invented one.
     */
    private static void writeWeightsBlock(
        DataOutputStream out, GruWeights weights, List<String> explicitVocab
    ) throws IOException
    {
        out.writeInt( weights.schemaVersion );
        out.writeInt( explicitVocab.size() );
        out.writeInt( weights.hashBuckets );
        out.writeInt( weights.embeddingDim );
        out.writeInt( weights.hiddenSize );
        out.writeInt( weights.sequenceCap );
        out.writeInt( weights.numClasses );
        out.writeDouble( weights.abstainThreshold );
        for( String word : explicitVocab ) out.writeUTF(word);
        writeDoubleArray2D(out, weights.embeddings);
        writeDirectionWeights(out, weights.forward);
        writeDirectionWeights(out, weights.backward);
        writeDoubleArray2D(out, weights.denseW);
        writeDoubleArray(out, weights.denseB);
        writeDoubleArray2D(out, weights.outW);
        writeDoubleArray(out, weights.outB);
    }

    /** Loaded {@link GruWeights} plus the {@code explicitVocab} list it was built from -- {@code
     *  GruWeights} itself only exposes the vocab as a {@code String[]}; callers here generally want
     *  the {@code List<String>} shape {@link #toJson} etc. already take.
     */
    private static final class LoadedWeights {

        final List<String> explicitVocab;
        final GruWeights    weights;

        LoadedWeights(List<String> explicitVocab, GruWeights weights)
        {
            this.explicitVocab = explicitVocab;
            this.weights        = weights;
        }

    } // class LoadedWeights

    private static LoadedWeights readWeightsBlock(DataInputStream in) throws IOException
    {
        int    schemaVersion    = in.readInt();
        int    vocabSize        = in.readInt();
        int    hashBuckets      = in.readInt();
        int    embeddingDim     = in.readInt();
        int    hiddenSize       = in.readInt();
        int    sequenceCap      = in.readInt();
        int    numClasses       = in.readInt();
        double abstainThreshold = in.readDouble();
        List<String> explicitVocab = new ArrayList<>(vocabSize);
        for(int i = 0; i < vocabSize; ++i) explicitVocab.add( in.readUTF() );
        double[][] embeddings = readDoubleArray2D(in);
        GruWeights.DirectionWeights forward  = readDirectionWeights(in);
        GruWeights.DirectionWeights backward = readDirectionWeights(in);
        double[][] denseW = readDoubleArray2D(in);
        double[]   denseB = readDoubleArray(in);
        double[][] outW   = readDoubleArray2D(in);
        double[]   outB   = readDoubleArray(in);

        GruWeights weights = new GruWeightsBuilder()
                .schemaVersion(schemaVersion)
                .vocabSize(vocabSize)
                .hashBuckets(hashBuckets)
                .embeddingDim(embeddingDim)
                .hiddenSize(hiddenSize)
                .sequenceCap(sequenceCap)
                .numClasses(numClasses)
                .abstainThreshold(abstainThreshold)
                .explicitVocab( explicitVocab.toArray( new String[0] ) )
                .embeddings(embeddings)
                .forward(forward)
                .backward(backward)
                .denseW(denseW)
                .denseB(denseB)
                .outW(outW)
                .outB(outB)
                .build();

        return new LoadedWeights(explicitVocab, weights);
    }

    /**
     * Writes the best-weights checkpoint (weights + vocab only, plus the validation loss that earned
     *  it, for human inspection -- no Adam/run state, this is "give me the best model so far", not a
     *  resume target on its own). Overwritten only when validation loss improves. Temp-file-then-
     *  atomic-rename, see the constant block's javadoc.
     */
    private static void writeBestCheckpoint(
        File file, GruWeights weights, List<String> explicitVocab, double bestValidationLoss
    ) throws IOException
    {
        File tmp = new File( file.getPath() + ".tmp" );
        try( DataOutputStream out = new DataOutputStream( new BufferedOutputStream( new FileOutputStream(tmp) ) ) ) {
            out.writeInt(CHECKPOINT_MAGIC);
            out.writeInt(CHECKPOINT_FORMAT_VERSION);
            out.writeInt(0); // kind = best
            writeWeightsBlock(out, weights, explicitVocab);
            out.writeDouble(bestValidationLoss);
        }
        fsyncFile(tmp);
        Files.move( tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING );
        fsyncParentDirectory(file);
    }

    /**
     * Forces {@code f}'s content to durable storage before the caller's atomic rename -- without
     *  this, the temp file's bytes (and, separately, the rename itself -- see
     *  {@link #fsyncParentDirectory}) can still be sitting in the OS page cache and lost on a real
     *  power failure, even though a plain process kill can never corrupt the file (temp-file-then-
     *  atomic-rename, see the constant block's javadoc).
     */
    private static void fsyncFile(File f) throws IOException
    {
        try( FileChannel ch = FileChannel.open( f.toPath(), StandardOpenOption.WRITE ) ) {
            ch.force(true);
        }
    }

    /**
     * Forces the directory entry (the rename itself) to durable storage -- Linux-only (opening a
     *  directory as a {@link FileChannel} is not portable to Windows), acceptable here since this
     *  tool only runs on this project's own Linux dev/build hosts.
     */
    private static void fsyncParentDirectory(File file) throws IOException
    {
        File parent = file.getAbsoluteFile().getParentFile();
        if(parent == null) return;
        try( FileChannel ch = FileChannel.open( parent.toPath(), StandardOpenOption.READ ) ) {
            ch.force(true);
        }
    }

    private static LoadedWeights readBestCheckpoint(File file) throws IOException
    {
        try( DataInputStream in = new DataInputStream( new BufferedInputStream( new FileInputStream(file) ) ) ) {
            int magic = in.readInt();
            if(magic != CHECKPOINT_MAGIC) throw new IOException(
                "not a GruTrainer checkpoint file (bad magic): " + file
            );
            int formatVersion = in.readInt();
            if(formatVersion != CHECKPOINT_FORMAT_VERSION) throw new IOException(
                "unsupported checkpoint format version " + formatVersion + " in " + file
                        + " (expected " + CHECKPOINT_FORMAT_VERSION + ")"
            );
            int kind = in.readInt();
            if(kind != 0) throw new IOException(
                "expected a best-weights checkpoint (kind=0) but found kind=" + kind + " in " + file
            );
            LoadedWeights loaded = readWeightsBlock(in);
            in.readDouble(); // bestValidationLoss -- informational only, caller doesn't need it back

            return loaded;
        }
    }

    private static void writeDirectionMoments(
        DataOutputStream out, AdamState.DirectionMoments m
    ) throws IOException
    {
        writeDoubleArray2D(out, m.WzM);
        writeDoubleArray2D(out, m.WzV);
        writeDoubleArray2D(out, m.WrM);
        writeDoubleArray2D(out, m.WrV);
        writeDoubleArray2D(out, m.WhM);
        writeDoubleArray2D(out, m.WhV);
        writeDoubleArray2D(out, m.UzM);
        writeDoubleArray2D(out, m.UzV);
        writeDoubleArray2D(out, m.UrM);
        writeDoubleArray2D(out, m.UrV);
        writeDoubleArray2D(out, m.UhM);
        writeDoubleArray2D(out, m.UhV);
        writeDoubleArray(out, m.bzM);
        writeDoubleArray(out, m.bzV);
        writeDoubleArray(out, m.brM);
        writeDoubleArray(out, m.brV);
        writeDoubleArray(out, m.bhM);
        writeDoubleArray(out, m.bhV);
    }

    private static void readDirectionMomentsInto(
        DataInputStream in, AdamState.DirectionMoments m
    ) throws IOException
    {
        readDoubleArray2DInto(in, m.WzM);
        readDoubleArray2DInto(in, m.WzV);
        readDoubleArray2DInto(in, m.WrM);
        readDoubleArray2DInto(in, m.WrV);
        readDoubleArray2DInto(in, m.WhM);
        readDoubleArray2DInto(in, m.WhV);
        readDoubleArray2DInto(in, m.UzM);
        readDoubleArray2DInto(in, m.UzV);
        readDoubleArray2DInto(in, m.UrM);
        readDoubleArray2DInto(in, m.UrV);
        readDoubleArray2DInto(in, m.UhM);
        readDoubleArray2DInto(in, m.UhV);
        readDoubleArrayInto(in, m.bzM);
        readDoubleArrayInto(in, m.bzV);
        readDoubleArrayInto(in, m.brM);
        readDoubleArrayInto(in, m.brV);
        readDoubleArrayInto(in, m.bhM);
        readDoubleArrayInto(in, m.bhV);
    }

    private static void writeAdamState(DataOutputStream out, AdamState adam) throws IOException
    {
        writeDoubleArray2D(out, adam.embeddingM);
        writeDoubleArray2D(out, adam.embeddingV);
        writeDirectionMoments(out, adam.forward);
        writeDirectionMoments(out, adam.backward);
        writeDoubleArray2D(out, adam.denseWM);
        writeDoubleArray2D(out, adam.denseWV);
        writeDoubleArray(out, adam.denseBM);
        writeDoubleArray(out, adam.denseBV);
        writeDoubleArray2D(out, adam.outWM);
        writeDoubleArray2D(out, adam.outWV);
        writeDoubleArray(out, adam.outBM);
        writeDoubleArray(out, adam.outBV);
    }

    /** Allocates a fresh {@link AdamState} sized to {@code weights} (all-zero moments, same as
     *  {@code new AdamState(weights)} at the start of a fresh run), then overwrites its arrays'
     *  contents in place from {@code in} -- the moment arrays are {@code final} references, so this
     *  fills them rather than reassigning.
     */
    private static AdamState readAdamStateInto(DataInputStream in, GruWeights weights) throws IOException
    {
        AdamState adam = new AdamState(weights);
        readDoubleArray2DInto(in, adam.embeddingM);
        readDoubleArray2DInto(in, adam.embeddingV);
        readDirectionMomentsInto(in, adam.forward);
        readDirectionMomentsInto(in, adam.backward);
        readDoubleArray2DInto(in, adam.denseWM);
        readDoubleArray2DInto(in, adam.denseWV);
        readDoubleArrayInto(in, adam.denseBM);
        readDoubleArrayInto(in, adam.denseBV);
        readDoubleArray2DInto(in, adam.outWM);
        readDoubleArray2DInto(in, adam.outWV);
        readDoubleArrayInto(in, adam.outBM);
        readDoubleArrayInto(in, adam.outBV);

        return adam;
    }

    /**
     * Writes the current-weights checkpoint: full resumable state (weights, vocab, Adam optimizer
     *  moment arrays -- not just the raw weight arrays, since resuming without them would restart the
     *  optimizer's momentum from scratch and defeat the point of a faithful resume -- and scalar run
     *  state). Overwritten once per epoch, after that epoch's Adam updates + validation-loss
     *  computation. Temp-file-then-atomic-rename, see the constant block's javadoc.
     *
     *  <p><b>Resume fidelity caveat</b> (documented here and at the {@code --resume} call site): the
     *  RNG seed is persisted, not {@code java.util.Random}'s internal state -- resuming reproduces the
     *  exact original train/validation split (deterministic given the same seed) but not the exact
     *  per-epoch shuffle sequence beyond that point. Accepted limitation, not a bug.
     */
    private static void writeCurrentCheckpoint(
        File       file,
        GruWeights weights,
        List<String> explicitVocab,
        AdamState  adam,
        int        epoch,
        int        epochsSinceImprovement,
        double     bestValidationLoss,
        double     learningRate,
        int        maxEpochs,
        int        patience,
        long       seed,
        int        step,
        int        batchSize,
        int        warmupSteps,
        double     lrMin
    ) throws IOException
    {
        File tmp = new File( file.getPath() + ".tmp" );
        try( DataOutputStream out = new DataOutputStream( new BufferedOutputStream( new FileOutputStream(tmp) ) ) ) {
            out.writeInt(CHECKPOINT_MAGIC);
            out.writeInt(CHECKPOINT_FORMAT_VERSION);
            out.writeInt(1); // kind = current
            writeWeightsBlock(out, weights, explicitVocab);
            out.writeInt(epoch);
            out.writeInt(epochsSinceImprovement);
            out.writeDouble(bestValidationLoss);
            out.writeDouble(learningRate);
            out.writeInt(maxEpochs);
            out.writeInt(patience);
            out.writeLong(seed);
            out.writeInt(step);
            out.writeInt(batchSize);
            out.writeInt(warmupSteps);
            out.writeDouble(lrMin);
            writeAdamState(out, adam);
        }
        fsyncFile(tmp);
        Files.move( tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING );
        fsyncParentDirectory(file);
    }

    /** {@link #writeCurrentCheckpoint} wrapper for the per-epoch call sites in {@code main}: a failed
     *  checkpoint write must never abort a real training run (it's a safety net, not the run's actual
     *  deliverable), so this catches and warns instead of propagating.
     */
    private static void writeCurrentCheckpointQuietly(
        File       file,
        GruWeights weights,
        List<String> explicitVocab,
        AdamState  adam,
        int        epoch,
        int        epochsSinceImprovement,
        double     bestValidationLoss,
        double     learningRate,
        int        maxEpochs,
        int        patience,
        long       seed,
        int        step,
        int        batchSize,
        int        warmupSteps,
        double     lrMin
    )
    {
        try {
            writeCurrentCheckpoint(
                file, weights, explicitVocab, adam, epoch, epochsSinceImprovement, bestValidationLoss,
                learningRate, maxEpochs, patience, seed, step, batchSize, warmupSteps, lrMin
            );
        }
        catch(IOException e) {
            System.err.println( "GruTrainer: warning -- could not write current-weights checkpoint to "
                    + file + ": " + e.getMessage() );
        }
    }

    /** Full state restored by {@code --resume=<path>}: weights, vocab, Adam optimizer moments, and
     *  every scalar the epoch loop needs to continue faithfully (epoch/patience bookkeeping, the
     *  hyperparameters that were in effect, and the Adam step counter -- needed for bias-correction
     *  continuity, not just cosmetic).
     */
    private static final class ResumeState {

        final List<String> explicitVocab;
        final GruWeights   weights;
        final AdamState    adam;
        final int          epoch;
        final int          epochsSinceImprovement;
        final double       bestValidationLoss;
        final double       learningRate;
        final int          maxEpochs;
        final int          patience;
        final long         seed;
        final int          step;
        final int          batchSize;
        final int          warmupSteps;
        final double       lrMin;

        ResumeState(
            List<String> explicitVocab,
            GruWeights   weights,
            AdamState    adam,
            int          epoch,
            int          epochsSinceImprovement,
            double       bestValidationLoss,
            double       learningRate,
            int          maxEpochs,
            int          patience,
            long         seed,
            int          step,
            int          batchSize,
            int          warmupSteps,
            double       lrMin
        )
        {
            this.explicitVocab          = explicitVocab;
            this.weights                = weights;
            this.adam                   = adam;
            this.epoch                  = epoch;
            this.epochsSinceImprovement = epochsSinceImprovement;
            this.bestValidationLoss     = bestValidationLoss;
            this.learningRate           = learningRate;
            this.maxEpochs              = maxEpochs;
            this.patience               = patience;
            this.seed                   = seed;
            this.step                   = step;
            this.batchSize              = batchSize;
            this.warmupSteps            = warmupSteps;
            this.lrMin                  = lrMin;
        }

    } // class ResumeState

    private static ResumeState loadCurrentCheckpoint(File file) throws IOException
    {
        try( DataInputStream in = new DataInputStream( new BufferedInputStream( new FileInputStream(file) ) ) ) {
            int magic = in.readInt();
            if(magic != CHECKPOINT_MAGIC) throw new IOException(
                "not a GruTrainer checkpoint file (bad magic): " + file
            );
            int formatVersion = in.readInt();
            if(formatVersion != CHECKPOINT_FORMAT_VERSION) throw new IOException(
                "unsupported checkpoint format version " + formatVersion + " in " + file
                        + " (expected " + CHECKPOINT_FORMAT_VERSION + ")"
            );
            int kind = in.readInt();
            if(kind != 1) throw new IOException(
                "expected a current-weights checkpoint (kind=1) but found kind=" + kind + " in " + file
            );
            LoadedWeights loaded                  = readWeightsBlock(in);
            int           epoch                   = in.readInt();
            int           epochsSinceImprovement  = in.readInt();
            double        bestValidationLoss      = in.readDouble();
            double        learningRate            = in.readDouble();
            int           maxEpochs               = in.readInt();
            int           patience                = in.readInt();
            long          seed                    = in.readLong();
            int           step                    = in.readInt();
            int           batchSize               = in.readInt();
            int           warmupSteps             = in.readInt();
            double        lrMin                   = in.readDouble();
            AdamState     adam                    = readAdamStateInto(in, loaded.weights);

            return new ResumeState(
                loaded.explicitVocab, loaded.weights, adam, epoch, epochsSinceImprovement,
                bestValidationLoss, learningRate, maxEpochs, patience, seed, step, batchSize,
                warmupSteps, lrMin
            );
        }
    }

    /**
     * Builder for {@link GruWeights} -- its own constructor is package-private (only
     *  {@code GruWeights.load} normally builds instances), so this class hand-assembles the same
     *  JSON {@code GruWeights.load} parses instead of trying to call that constructor directly.
     *  Kept as a builder (rather than one giant constructor call) only to keep {@link #randomInit}
     *  readable given how many fields {@link GruWeights} now has.
     */
    private static final class GruWeightsBuilder {

        private int schemaVersion, vocabSize, hashBuckets, embeddingDim, hiddenSize, sequenceCap, numClasses;
        private double                                                                            abstainThreshold;
        private String[]                                                                          explicitVocab;
        private double[][]                                                                        embeddings;
        private GruWeights.DirectionWeights forward, backward;
        private double[][] denseW;
        private double[]   denseB;
        private double[][] outW;
        private double[]   outB;

        GruWeightsBuilder schemaVersion   (int                         v) { this.schemaVersion = v; return this;    }
        GruWeightsBuilder vocabSize       (int                         v) { this.vocabSize = v; return this;        }
        GruWeightsBuilder hashBuckets     (int                         v) { this.hashBuckets = v; return this;      }
        GruWeightsBuilder embeddingDim    (int                         v) { this.embeddingDim = v; return this;     }
        GruWeightsBuilder hiddenSize      (int                         v) { this.hiddenSize = v; return this;       }
        GruWeightsBuilder sequenceCap     (int                         v) { this.sequenceCap = v; return this;      }
        GruWeightsBuilder numClasses      (int                         v) { this.numClasses = v; return this;       }
        GruWeightsBuilder abstainThreshold(double                      v) { this.abstainThreshold = v; return this; }
        GruWeightsBuilder explicitVocab   (String[]                    v) { this.explicitVocab = v; return this;    }
        GruWeightsBuilder embeddings      (double[][]                  v) { this.embeddings = v; return this;       }
        GruWeightsBuilder forward         (GruWeights.DirectionWeights v) { this.forward = v; return this;          }
        GruWeightsBuilder backward        (GruWeights.DirectionWeights v) { this.backward = v; return this;         }
        GruWeightsBuilder denseW          (double[][]                  v) { this.denseW = v; return this;           }
        GruWeightsBuilder denseB          (double[]                    v) { this.denseB = v; return this;           }
        GruWeightsBuilder outW            (double[][]                  v) { this.outW = v; return this;             }
        GruWeightsBuilder outB            (double[]                    v) { this.outB = v; return this;             }

        GruWeights build()
        {
            // GruWeights has no public constructor (see class javadoc above) -- round-trip through
            // its own JSON schema instead, which is public API (GruWeights.load).
            String json = toJson(this);
            try {
                java.nio.file.Path tmp = Files.createTempFile("gru_trainer_init", ".json");
                Files.write( tmp, json.getBytes(StandardCharsets.UTF_8) );
                GruWeights result = GruWeights.load(tmp);
                Files.deleteIfExists(tmp);
                return result;
            }
            catch(IOException e) {
                throw new IllegalStateException(
                    "GruTrainer: failed to round-trip initial weights: " + e.getMessage(), e
                );
            }
        }

        private static String toJson(GruWeightsBuilder b)
        {
            return GruTrainer.toJsonFields(
                b.schemaVersion, b.vocabSize, b.hashBuckets, b.embeddingDim,
                b.hiddenSize, b.sequenceCap, b.numClasses, b.abstainThreshold, b.explicitVocab,
                b.embeddings, b.forward, b.backward, b.denseW, b.denseB, b.outW, b.outB
            );
        }

    } // class GruWeightsBuilder

    private static String toJson(GruWeights weights, List<String> explicitVocab)
    {
        return toJsonFields(
            weights.schemaVersion, explicitVocab.size(), weights.hashBuckets, weights.embeddingDim,
            weights.hiddenSize, weights.sequenceCap, weights.numClasses, weights.abstainThreshold,
            explicitVocab.toArray( new String[0] ), weights.embeddings, weights.forward, weights.backward,
            weights.denseW, weights.denseB, weights.outW, weights.outB
        );
    }

    private static String toJsonFields(
        int                         schemaVersion,
        int                         vocabSize,
        int                         hashBuckets,
        int                         embeddingDim,
        int                         hiddenSize,
        int                         sequenceCap,
        int                         numClasses,
        double                      abstainThreshold,
        String[]                    explicitVocab,
        double[][]                  embeddings,
        GruWeights.DirectionWeights forward,
        GruWeights.DirectionWeights backward,
        double[][]                  denseW,
        double[]                    denseB,
        double[][]                  outW,
        double[]                    outB
    )
    {
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
        sb.append("  \"explicitVocab\": ").append( jsonStringArray(explicitVocab) ).append(",\n");
        sb.append("  \"embeddings\": ").append( jsonArray2D(embeddings) ).append(",\n");
        appendDirection(sb, "forward", forward);
        appendDirection(sb, "backward", backward);
        sb.append("  \"denseW\": ").append( jsonArray2D(denseW) ).append(",\n");
        sb.append("  \"denseB\": ").append( jsonArray1D(denseB) ).append(",\n");
        sb.append("  \"outW\": ").append( jsonArray2D(outW) ).append(",\n");
        sb.append("  \"outB\": ").append( jsonArray1D(outB) ).append("\n");
        sb.append("}\n");

        return sb.toString();
    }

    private static void appendDirection(
        StringBuilder               sb,
        String                      prefix,
        GruWeights.DirectionWeights weights
    )
    {
        sb.append(
            "  \""
        ).append(
            prefix
        ).append(
            "Wz\": "
        ).append(
            jsonArray2D(weights.Wz)
        ).append(
            ",\n"
        );
        sb.append(
            "  \""
        ).append(
            prefix
        ).append(
            "Wr\": "
        ).append(
            jsonArray2D(weights.Wr)
        ).append(
            ",\n"
        );
        sb.append(
            "  \""
        ).append(
            prefix
        ).append(
            "Wh\": "
        ).append(
            jsonArray2D(weights.Wh)
        ).append(
            ",\n"
        );
        sb.append(
            "  \""
        ).append(
            prefix
        ).append(
            "Uz\": "
        ).append(
            jsonArray2D(weights.Uz)
        ).append(
            ",\n"
        );
        sb.append(
            "  \""
        ).append(
            prefix
        ).append(
            "Ur\": "
        ).append(
            jsonArray2D(weights.Ur)
        ).append(
            ",\n"
        );
        sb.append(
            "  \""
        ).append(
            prefix
        ).append(
            "Uh\": "
        ).append(
            jsonArray2D(weights.Uh)
        ).append(
            ",\n"
        );
        sb.append(
            "  \""
        ).append(
            prefix
        ).append(
            "bz\": "
        ).append(
            jsonArray1D(weights.bz)
        ).append(
            ",\n"
        );
        sb.append(
            "  \""
        ).append(
            prefix
        ).append(
            "br\": "
        ).append(
            jsonArray1D(weights.br)
        ).append(
            ",\n"
        );
        sb.append(
            "  \""
        ).append(
            prefix
        ).append(
            "bh\": "
        ).append(
            jsonArray1D(weights.bh)
        ).append(
            ",\n"
        );
    }

    private static String jsonArray1D(double[] values)
    {
        StringBuilder sb = new StringBuilder("[");
        for(int i = 0; i < values.length; ++i) {
            if(i > 0) sb.append(",");
            sb.append( values[i] );
        }

        return sb.append("]").toString();
    }

    private static String jsonArray2D(double[][] rows)
    {
        StringBuilder sb = new StringBuilder("[");
        for(int i = 0; i < rows.length; ++i) {
            if(i > 0) sb.append(",");
            sb.append( jsonArray1D( rows[i] ) );
        }

        return sb.append("]").toString();
    }

    private static String jsonStringArray(String[] values)
    {
        StringBuilder sb = new StringBuilder("[");
        for(int i = 0; i < values.length; ++i) {
            if(i > 0) sb.append(",");
            sb.append("\"").append( escapeJsonString( values[i] ) ).append("\"");
        }

        return sb.append("]").toString();
    }

    private static String escapeJsonString(String s)
    {
        return s.replace(
            "\\", "\\\\"
        ).replace(
            "\"", "\\\""
        ).replace(
            "\n", "\\n"
        ).replace(
            "\t", "\\t"
        );
    }

} // class GruTrainer
