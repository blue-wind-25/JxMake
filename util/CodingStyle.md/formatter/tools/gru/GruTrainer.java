/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
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
 * implementation design". Deliberately lives outside {@code src/} -- the runtime JAR must never
 * bundle training code; this writes a weights file for
 * {@code com.jxmake.formatter.classifier.gru.GruClassifier}/{@code GruWeights} to read at
 * runtime, it does not generate or overwrite any {@code .java} source.
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
    private static final double ABSTAIN_THRESHOLD = 0.76;
    /**
     * Global L2-norm gradient clipping threshold. Prevents exploding gradients in the
     * recurrent layers while leaving ordinary updates unchanged. A value around 5 is a
     * conventional starting point for GRU/LSTM training and can later become a
     * --clip=<value> hyperparameter if needed.
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
    private static final int CHECKPOINT_MAGIC = 0x47525543; // "GRUC"
    // Bumped 1 -> 2 when mini-batch training added a new persisted scalar (batchSize), then 2 -> 3
    // when the LR warmup+cosine-decay schedule added two more (warmupSteps, lrMin) to the
    // current-weights checkpoint's run-state block -- see writeCurrentCheckpoint/loadCurrentCheckpoint.
    // A checkpoint from an older format version is simply rejected by the version check below rather
    // than silently misread; checkpoints are an ephemeral resume/recovery safety net (never
    // committed, deleted on normal completion), so this is a safe, low-cost break.
    private static final int    CHECKPOINT_FORMAT_VERSION = 3;
    private static final String CHECKPOINT_CURRENT_SUFFIX = ".ckpt-current.bin";
    private static final String CHECKPOINT_BEST_SUFFIX    = ".ckpt-best.bin";

    public static void main(final String[] args)
    {
        if(args.length < 2) {
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        final File examplesFile = new File( args[0] );
        if( !examplesFile.isFile() || !examplesFile.canRead() ) {
            System.err.println( "GruTrainer: labeled-examples file not readable: " + args[0] );
            System.exit(2);
            return;
        }

        final File weightsOut       = new File( args[1] );
        final File weightsOutParent = weightsOut.getAbsoluteFile().getParentFile();
        if( weightsOutParent == null || !weightsOutParent.isDirectory() ) {
            System.err.println( "GruTrainer: output-weights-path parent directory does not exist: "
                    + args[1] );
            System.exit(2);
            return;
        }

        final Map<String, String> hyperparameters = new LinkedHashMap<>();
        for(int i = 2; i < args.length; ++i) {
            final String arg = args[i];
            if( !arg.startsWith("--") || arg.indexOf('=') < 0 ) {
                System.err.println("GruTrainer: malformed hyperparameter arg (expected --key=value): "
                        + arg);
                System.err.println(USAGE);
                System.exit(2);
                return;
            } // if
            final int    eq    = arg.indexOf('=');
            final String key   = arg.substring(2, eq);
            final String value = arg.substring(eq + 1);
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
        final String      resumePath = hyperparameters.get("resume");
              ResumeState resumed    = null;
        if(resumePath != null) {
            final File resumeFile = new File(resumePath);
            if( !resumeFile.isFile() || !resumeFile.canRead() ) {
                System.err.println(
                    "GruTrainer: --resume checkpoint file not readable: " + resumePath
                );
                System.exit(2);
                return;
            } // if
            try {
                resumed = loadCurrentCheckpoint(resumeFile);
            }
            catch(final IOException e) {
                System.err.println( "GruTrainer: could not load --resume checkpoint " + resumePath
                        + ": " + e.getMessage() );
                System.exit(2);
                return;
            }
            System.out.println( String.format(
                    "GruTrainer: resuming from '%s' (epoch=%2d, epochsSinceImprovement=%2d,"
                            + " bestValidationLoss=%9.7f)",
                    resumePath, resumed.epoch, resumed.epochsSinceImprovement, resumed.bestValidationLoss) );
        } // if

        // RDD_EXT_18 starting defaults, overridable via --key=value; fall back to the resumed
        // checkpoint's own recorded values (not the hardcoded RDD_EXT_18 defaults) when resuming
        final double learningRate = Double.parseDouble(
            hyperparameters.getOrDefault( "lr", resumed != null ? String.valueOf(resumed.learningRate) : "0.001" )
        );
        final int    maxEpochs    = Integer.parseInt(
            hyperparameters.getOrDefault( "epochs", resumed != null ? String.valueOf(resumed.maxEpochs) : "30" )
        );
        final int    patience     = Integer.parseInt(
            hyperparameters.getOrDefault( "patience", resumed != null ? String.valueOf(resumed.patience) : "5" )
        );
        final long   seed         = Long.parseLong(
            hyperparameters.getOrDefault( "seed", resumed != null ? String.valueOf(resumed.seed) : "42" )
        );
        // Progress reporting (2026-07-29): large auto-labeled corpora (100k+ examples) take long
        // enough per epoch that a human watching the process needs mid-epoch feedback, not just a
        // once-per-epoch line. --progress-every=N prints a running line every N training examples
        // within the epoch (0 disables); default chosen so it fires a handful of times per epoch on
        // a ~100k-example corpus without flooding the console on small corpora.
        final int progressEvery = Integer.parseInt(
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
        final int threads = Integer.parseInt( hyperparameters.getOrDefault("threads", "1") );
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
        final int batchSize = Integer.parseInt(
            hyperparameters.getOrDefault( "batch-size", resumed != null ? String.valueOf(resumed.batchSize) : "16" )
        );
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
        final int warmupSteps = Integer.parseInt(
            hyperparameters.getOrDefault( "warmup-steps", resumed != null ? String.valueOf(resumed.warmupSteps) : "0" )
        );
        if(warmupSteps < 0) {
            System.err.println("GruTrainer: --warmup-steps must be >= 0, got " + warmupSteps);
            System.exit(2);
            return;
        }
        // --lr-min=N (default 0.0, resumable): the cosine decay's floor. Only meaningful when
        // --warmup-steps > 0; ignored (schedule disabled) otherwise.
        final double lrMin = Double.parseDouble(
            hyperparameters.getOrDefault( "lr-min", resumed != null ? String.valueOf(resumed.lrMin) : "0.0" )
        );
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
        final boolean checkGradients       = hyperparameters.containsKey("check-gradients");
        final int     checkGradientSamples = checkGradients ? Integer.parseInt(
            hyperparameters.get("check-gradients")
        ) : 0;

        final List<Example> examples;
        try {
            examples = readExamples(examplesFile);
        }
        catch(final IOException e) {
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

        // --hand-labeled=<path> + --hand-labeled-repeat=N (both opt-in, default disabled):
        // oversamples a small set of hand-labeled hard cases against the bulk auto-labeled corpus.
        // Per STATE_AI.md's 2026-08-02 GRU-improvement session -- the hand-labeled
        // classifier_weights_examples rows are already folded into the main labeled-examples file
        // (sample_default.txt, via `make gru-acquire-corpus`) but are a ~0.3% minority there, so
        // online SGD/Adam sees each one once per epoch, easily swamped by the majority-YES
        // auto-labeled gradient. This adds N *extra* copies of every example in --hand-labeled's file
        // into the training split only (never validation, so the held-out validation-loss numbers
        // stay comparable to a non-oversampled run) -- the file is expected to be in the same
        // RDD_EXT_20/21 schema as the main labeled-examples file (e.g. the output of
        // convert_classifier_weights_examples.py), typically the same rows already present once in
        // the main file. Default 0 means fully disabled (identical behavior to before this flag
        // existed, even if --hand-labeled is passed without a repeat count).
        final String handLabeledPath   = hyperparameters.get("hand-labeled");
        final int    handLabeledRepeat = Integer.parseInt(
            hyperparameters.getOrDefault("hand-labeled-repeat", "0")
        );
        if(handLabeledRepeat < 0) {
            System.err.println(
                "GruTrainer: --hand-labeled-repeat must be >= 0, got " + handLabeledRepeat
            );
            System.exit(2);
            return;
        } // if
        if(handLabeledPath != null && handLabeledRepeat == 0) System.err.println(
            "GruTrainer: --hand-labeled given without --hand-labeled-repeat > 0 -- this is a" + " no-op; did you mean to also pass --hand-labeled-repeat=N?"
        );
        List<Example> handLabeledExamples = Collections.emptyList();
        if(handLabeledRepeat > 0) {
            if(handLabeledPath == null) {
                System.err.println(
                    "GruTrainer: --hand-labeled-repeat > 0 requires --hand-labeled=<path>"
                );
                System.exit(2);
                return;
            } // if
            final File handLabeledFile = new File(handLabeledPath);
            if( !handLabeledFile.isFile() || !handLabeledFile.canRead() ) {
                System.err.println(
                    "GruTrainer: --hand-labeled file not readable: " + handLabeledPath
                );
                System.exit(2);
                return;
            } // if
            try {
                handLabeledExamples = readExamples(handLabeledFile);
            }
            catch(final IOException e) {
                System.err.println( "GruTrainer: could not read --hand-labeled file "
                        + handLabeledPath + ": " + e.getMessage() );
                System.exit(2);
                return;
            }
            if( handLabeledExamples.isEmpty() ) {
                System.err.println(
                    "GruTrainer: --hand-labeled file " + handLabeledPath + " has no examples"
                );
                System.exit(2);
                return;
            } // if
        } // if

        final List<String> explicitVocab;
        if(resumed != null) {
            // Resuming: the vocab is whatever embedding-row layout the checkpoint's own weights were
            // trained against (embedded in the checkpoint itself, mirroring how GruWeights embeds its
            // own explicitVocab snapshot in the JSON weights file) -- never re-derived from --vocab/
            // the examples file, which could silently shift embedding-row indices out from under the
            // resumed weight arrays
            explicitVocab = resumed.explicitVocab;
        } // if
        else {
            final String vocabPath = hyperparameters.getOrDefault("vocab", DEFAULT_VOCAB_PATH);
            final File   vocabFile = new File(vocabPath);
            if( !vocabPath.isEmpty() && vocabFile.isFile() && vocabFile.canRead() ) {
                try {
                    explicitVocab = readVocab(vocabFile);
                }
                catch(final IOException e) {
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
        final Vocabulary vocabulary = new Vocabulary(explicitVocab);

        final Random     random  = new Random(seed);
        final GruWeights weights = resumed != null ? resumed.weights : randomInit(
            explicitVocab, vocabulary, random
        );

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
        final int           validationCount = Math.max( 1, examples.size() / 5 );
        final List<Example> validation      = new ArrayList<>( examples.subList(0, validationCount) );
              List<Example> train           = new ArrayList<>( examples.subList(
            validationCount, examples.size()
        ) );
        if( train.isEmpty() ) train = new ArrayList<>(examples);

        // Hand-labeled oversampling (see the --hand-labeled/--hand-labeled-repeat javadoc above):
        // added to `train` only, after the split, so `validation`'s held-out set is unaffected and
        // its loss stays comparable across oversampled vs. non-oversampled runs.
        if( !handLabeledExamples.isEmpty() ) {
            for(int r = 0; r < handLabeledRepeat; ++r) train.addAll(handLabeledExamples);
            System.out.println( String.format(
                    "GruTrainer: oversampled %d hand-labeled example(s) x%d repeat(s) = %d extra"
                            + " training row(s) from '%s' (trainExamples now %d)",
                    handLabeledExamples.size(), handLabeledRepeat,
                    handLabeledExamples.size() * handLabeledRepeat, handLabeledPath, train.size() ) );
        } // if

        // Decay horizon (total scheduled steps): stepsPerEpoch * maxEpochs, reusing --epochs rather
        // than a separate duration flag (see the --warmup-steps javadoc above). Computed here since
        // it needs train.size() (fixed at this point) and batchSize; recomputes identically on resume
        // from the same (resumable) train.size()/batchSize/maxEpochs, so it needs no persisted state
        // of its own.
        final int stepsPerEpoch      = ( train.size() + batchSize - 1 ) / batchSize;
        final int totalScheduleSteps = stepsPerEpoch * maxEpochs;

        System.out.println( String.format(
                "GruTrainer: starting -- vocabSize=%d, trainExamples=%d, validationExamples=%d,"
                        + " maxEpochs=%d, patience=%d, lr=%9.7f, batchSize=%d, threads=%d,"
                        + " warmupSteps=%d, lrMin=%9.7f",
                explicitVocab.size(), train.size(), validation.size(), maxEpochs, patience,
                learningRate, batchSize, threads, warmupSteps, lrMin ) );

        final AdamState adam               = resumed != null ? resumed.adam : new AdamState(weights);
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
            final File   bestCheckpointFile = deriveBestCheckpointFile( new File(resumePath) );
                  String loadedBestJson     = null;
            if( bestCheckpointFile.isFile() && bestCheckpointFile.canRead() ) {
                try {
                    final LoadedWeights bestLoaded = readBestCheckpoint(bestCheckpointFile);
                    loadedBestJson = toJson(bestLoaded.weights, bestLoaded.explicitVocab);
                }
                catch(final IOException e) {
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
                System.err.println("GruTrainer: no readable sibling best-checkpoint found at "
                        + bestCheckpointFile + " -- using the resumed (latest-epoch, not necessarily"
                        + " best) weights as the interim best-so-far snapshot until the next real"
                        + " validation improvement.");
                bestWeightsJson = toJson(weights, explicitVocab);
            }
        } // if
        else {
            bestWeightsJson = toJson(weights, explicitVocab);
        }
              int  epochsSinceImprovement = resumed != null ? resumed.epochsSinceImprovement : 0;
              int  step                   = resumed != null ? resumed.step : 0;
        final int  startEpoch             = resumed != null ? resumed.epoch + 1 : 1;
        final long trainingStartNanos     = System.nanoTime();

        final File currentCheckpointFile = new File(
            weightsOut.getAbsolutePath() + CHECKPOINT_CURRENT_SUFFIX
        );
        final File bestCheckpointFile    = deriveBestCheckpointFile(currentCheckpointFile);

        final ExecutorService executor = threads > 1 ? Executors.newFixedThreadPool(threads) : null;
        try {
            for(int epoch = startEpoch; epoch <= maxEpochs; ++epoch) {
                Collections.shuffle(train, random);
                      double trainLoss       = 0.0;
                final long   epochStartNanos = System.nanoTime();
                      int    examplesSeen    = 0;
                      int    i               = 0;
                // Last scheduled LR actually applied this epoch (stays == learningRate flat when
                // warmupSteps == 0, i.e. schedule disabled) -- reported on the epoch summary line
                double lrThisEpoch = learningRate;
                while( i < train.size() ) {
                    // BatchSize controls averaging granularity (one Adam step per this many
                    // examples); threads (passed to computeBatch's executor) controls how many of
                    // this batch's forward/backward computations run in parallel -- an orthogonal,
                    // composing axis, not a competing one. See the --threads/--batch-size javadoc
                    // above.
                    final int           batchEnd = Math.min( i + batchSize, train.size() );
                    final List<Example> batch    = train.subList(i, batchEnd);
                    i = batchEnd;
                    final List<ComputedGradient> computed = computeBatch(
                        executor, weights, vocabulary, batch
                    );
                    final GruClassifier.Gradients averaged = averageGradients(computed);
                    if(averaged != null) {
                        ++step;
                        lrThisEpoch = computeScheduledLr(
                            learningRate, lrMin, step, warmupSteps, totalScheduleSteps
                        );
                        adam.apply(weights, averaged, lrThisEpoch, step);
                    } // if
                    for(final ComputedGradient result : computed) {
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
                    } // for result
                } // while
                trainLoss /= train.size();

                // Validation never mutates `weights`, so parallelizing it (unlike training) carries
                // no staleness tradeoff at all -- every example is scored against the exact same
                // frozen snapshot regardless of thread count, and losses are summed back in
                // original list order, so the result is identical to running it sequentially
                double validationLoss = 0.0;
                int    vi             = 0;
                while( vi < validation.size() ) {
                    final int           vEnd  = Math.min( vi + threads, validation.size() );
                    final List<Example> batch = validation.subList(vi, vEnd);
                    vi = vEnd;
                    final List<Double> losses = computeValidationBatch(
                        executor, weights, vocabulary, batch
                    );
                    for(final Double loss : losses) {
                        if(loss != null) validationLoss += loss;
                    }
                } // while
                validationLoss /= validation.size();

                final double epochSeconds = ( System.nanoTime() - epochStartNanos ) / 1e9;
                final double totalSeconds = ( System.nanoTime() - trainingStartNanos ) / 1e9;
                System.out.println( String.format(
                        "GruTrainer: epoch %2d, trainLoss=%9.7f, validationLoss=%9.7f, lr=%9.7f, "
                                + "epochSeconds=%6.1f, totalElapsedSeconds=%8.1f",
                        epoch, trainLoss, validationLoss, lrThisEpoch, epochSeconds, totalSeconds) );

                if(validationLoss < bestValidationLoss) {
                    bestValidationLoss     = validationLoss;
                    bestWeightsJson        = toJson(weights, explicitVocab);
                    epochsSinceImprovement = 0;
                    try {
                        writeBestCheckpoint(
                            bestCheckpointFile, weights, explicitVocab, bestValidationLoss
                        );
                    }
                    catch(final IOException e) {
                        // A failed checkpoint write must never abort a real training run -- it's a
                        // safety net, not the run's actual deliverable (the final JSON weights file
                        // still gets written normally at the end). Warn and keep training.
                        System.err.println( "GruTrainer: warning -- could not write best-weights"
                                + " checkpoint to " + bestCheckpointFile + ": " + e.getMessage() );
                    }
                } // if
                else {
                    ++epochsSinceImprovement;
                    if(epochsSinceImprovement >= patience) {
                        System.out.println("GruTrainer: early stopping at epoch " + epoch
                                + " (no validation improvement for " + patience + " epochs)");
                        // Persist the final epoch/patience state to the current-weights checkpoint too,
                        // same as every other epoch below -- otherwise a resume from right after an
                        // early stop would see stale epochsSinceImprovement from the second-to-last
                        // epoch's checkpoint write
                        writeCurrentCheckpointQuietly(
                            currentCheckpointFile, weights, explicitVocab, adam, epoch,
                            epochsSinceImprovement, bestValidationLoss, learningRate, maxEpochs,
                            patience, seed, step, batchSize, warmupSteps, lrMin
                        );
                        break;
                    } // if
                }

                // Current-weights checkpoint: overwritten every epoch (win or not), holding full
                // resumable state -- see writeCurrentCheckpoint's javadoc for the exact binary layout
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

        final byte[] weightsBytes     = bestWeightsJson.getBytes(StandardCharsets.UTF_8);
              File   actualWeightsOut = weightsOut;
        try {
            Files.write( weightsOut.toPath(), weightsBytes );
        }
        catch(final IOException e) {
            System.err.println( "GruTrainer: could not write output weights file " + args[1] + ": "
                    + e.getMessage() );
            // Training can take minutes to hours; losing the trained weights entirely because the
            // final write failed (disk full, bad path, permissions) is far worse than writing them
            // somewhere unintended, so fall back to a /tmp path and loudly announce it rather than
            // exiting empty-handed
            final File fallback = new File(
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
            catch(final IOException fallbackError) {
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
        catch(final IOException e) {
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
     * matrix (positive class = YES) plus precision/recall/F1 against the held-out validation split.
     * Ground truth is always YES or NO (ABSTAIN is never a training label -- see the class javadoc),
     * but the trained softmax has a third ABSTAIN output slot per {@link GruClassifier#CLASS_ORDER},
     * so a prediction landing on ABSTAIN is counted here as simply "not predicted YES".
     */
    private static void printConfusionMatrix(
        final File          weightsFile,
        final Vocabulary    vocabulary,
        final List<Example> validation
    )
    {
        final GruWeights bestWeights;
        try {
            bestWeights = GruWeights.load( weightsFile.toPath() );
        }
        catch(final IOException e) {
            System.err.println( "GruTrainer: could not reload " + weightsFile
                    + " to report confusion matrix: " + e.getMessage() );
            return;
        }

        int truePositive = 0, falsePositive = 0, trueNegative = 0, falseNegative = 0;
        for(final Example example : validation) {
            if( example.targetWordIndex < 0 || example.targetWordIndex >= example.tokens.size() ) continue;
            final GruClassifier.ForwardCache cache =
                    GruClassifier.forward(
                        bestWeights, vocabulary, example.tokens, example.targetWordIndex
                    );
            final boolean predictedYes = argmax(cache.logits) == 0;
            final boolean actualYes    = example.classIndex == 0;
                 if(predictedYes && actualYes) truePositive++;
            else if(predictedYes)              falsePositive++;
            else if(actualYes)                 falseNegative++;
            else                               trueNegative++;
        } // for

        final double precision = truePositive + falsePositive == 0 ? 0.0 : (double)truePositive / (truePositive + falsePositive);
        final double recall    = truePositive + falseNegative == 0 ? 0.0 : (double)truePositive / (truePositive + falseNegative);
        final double f1        = precision + recall == 0 ? 0.0 : 2* precision * recall / (precision + recall);

        System.out.println( String.format(
                "GruTrainer: validation confusion matrix (positive=YES): tp=%d, fp=%d, tn=%d, fn=%d,"
                        + " precision=%7.5f, recall=%7.5f, f1=%7.5f",
                truePositive, falsePositive, trueNegative, falseNegative, precision, recall, f1) );
    }

    private static int argmax(final double[] values)
    {
        int best = 0;
        for(int i = 1; i < values.length; ++i) {
            if( values[i] > values[best] ) best = i;
        }

        return best;
    }

    /**
     * Diagnostic-only gradient check (see {@code --check-gradients} in {@link #main}): picks one
     * random labeled example, runs forward+backward once to get {@link GruClassifier.Gradients},
     * then for a representative sample of weight arrays (dense layer, output layer, one direction's
     * Wz, and the embedding rows the example actually touches) perturbs each sampled entry by
     * +/-epsilon, recomputes the loss, and compares the resulting numeric derivative against
     * backward()'s analytic one. Never used during normal training -- exists purely to build
     * confidence in backward() before further changes rely on it.
     */
    private static void checkGradients(
        final GruWeights    weights,
        final Vocabulary    vocabulary,
        final List<Example> examples,
        final Random        random,
        final int           samplesPerArray
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

        final List<String> tokens          = example.tokens;
        final int          targetWordIndex = example.targetWordIndex;
        final int          classIndex      = example.classIndex;
        final GruClassifier.ForwardCache cache = GruClassifier.forward(
            weights, vocabulary, tokens, targetWordIndex
        );
        final GruClassifier.Gradients gradients = GruClassifier.backward(weights, cache, classIndex);

        final double epsilon = 1e-5;
        System.out.println( String.format(
                "GruTrainer: gradient check against example label=%s -- epsilon=%9.7f,"
                        + " samplesPerArray=%d",
                example.label, epsilon, samplesPerArray) );
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
        for( final Map.Entry<Integer, double[]> entry : gradients.embeddingGrad.entrySet() ) {
            final int row = entry.getKey();
            maxRelError = Math.max(
                maxRelError, checkArray1D( "embeddings[" + row + "]", weights.embeddings[row], entry.getValue(), weights, vocabulary, tokens, targetWordIndex, classIndex, random, epsilon, samplesPerArray )
            );
        }

        System.out.println( String.format("GruTrainer: gradient check complete -- maxRelativeError=%9.6f",
                maxRelError) + (maxRelError < 1e-2 ? " (PASS)" : " (FAIL)") );
        System.exit(maxRelError < 1e-2 ? 0 : 1);
    }

    private static double checkArray2D(
        final String       name,
        final double[][]   param,
        final double[][]   grad,
        final GruWeights   weights,
        final Vocabulary   vocabulary,
        final List<String> tokens,
        final int          targetWordIndex,
        final int          classIndex,
        final Random       random,
        final double       epsilon,
        final int          samples
    )
    {
        double maxRelError = 0.0;
        for(int s = 0; s < samples; ++s) {
            final int    i        = random.nextInt(param.length);
            final int    j        = random.nextInt( param[i].length );
            final double relError = checkOneEntry(
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
        final String       name,
        final double[]     param,
        final double[]     grad,
        final GruWeights   weights,
        final Vocabulary   vocabulary,
        final List<String> tokens,
        final int          targetWordIndex,
        final int          classIndex,
        final Random       random,
        final double       epsilon,
        final int          samples
    )
    {
        double maxRelError = 0.0;
        for(int s = 0; s < samples; ++s) {
            final int    i        = random.nextInt(param.length);
            final double relError = checkOneEntry(
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
        final String       label,
        final double[]     param,
        final int          index,
        final double       analytic,
        final GruWeights   weights,
        final Vocabulary   vocabulary,
        final List<String> tokens,
        final int          targetWordIndex,
        final int          classIndex,
        final double       epsilon
    )
    {
        final double original = param[index];
        param[index] = original + epsilon;
        final double lossPlus = crossEntropyLoss(
            GruClassifier.forward(weights, vocabulary, tokens, targetWordIndex).logits, classIndex
        );
        param[index] = original - epsilon;
        final double lossMinus = crossEntropyLoss(
            GruClassifier.forward(weights, vocabulary, tokens, targetWordIndex).logits, classIndex
        );
        param[index] = original;

        final double numeric  = (lossPlus - lossMinus) / (2* epsilon);
        final double relError = Math.abs(
            numeric - analytic
        ) / Math.max(
            1e-8, Math.abs(numeric) + Math.abs(analytic)
        );
        System.out.println( String.format(
                "GruTrainer:   %s analytic=%9.6f, numeric=%9.6f, relError=%9.6f",
                label, analytic, numeric, relError) );

        return relError;
    }

    /**
     * One example's computed loss + gradients, or the outcome of skipping an out-of-range
     * {@code targetWordIndex} (never constructed for those -- see {@link #computeBatch})
     */
    private static final class ComputedGradient {

        final double loss;
        final GruClassifier.Gradients gradients;

        ComputedGradient(final double loss, final GruClassifier.Gradients gradients)
        {
            this.loss      = loss;
            this.gradients = gradients;
        }

    } // class ComputedGradient

    private static ComputedGradient computeGradient(
        final GruWeights weights,
        final Vocabulary vocabulary,
        final Example    example
    )
    {
        final GruClassifier.ForwardCache cache = GruClassifier.forward(
            weights, vocabulary, example.tokens, example.targetWordIndex
        );
        final double loss = crossEntropyLoss(cache.logits, example.classIndex);
        final GruClassifier.Gradients gradients = GruClassifier.backward(
            weights, cache, example.classIndex
        );
        clipGradients(gradients, GRADIENT_CLIP_NORM);

        return new ComputedGradient(loss, gradients);
    }

    /**
     * Computes forward+backward for one batch of examples, in parallel across {@code executor}'s
     * worker threads when non-null (all against the same {@code weights} snapshot -- safe since
     * {@link GruClassifier#forward}/{@link GruClassifier#backward} only read {@code weights}, never
     * mutate it), sequentially otherwise. Result order matches {@code batch}'s order; entries for
     * examples with an out-of-range {@code targetWordIndex} are {@code null} (skipped, matching the
     * pre-existing single-threaded behavior of not computing anything for them).
     */
    private static List<ComputedGradient> computeBatch(
        final ExecutorService executor,
        final GruWeights      weights,
        final Vocabulary      vocabulary,
        final List<Example>   batch
    )
    {
        final List<ComputedGradient> results = new ArrayList<>( batch.size() );
        if(executor == null) {
            for(final Example example : batch) {
                if( example.targetWordIndex < 0 || example.targetWordIndex >= example.tokens.size() ) results.add(
                    null
                );
                else results.add( computeGradient(weights, vocabulary, example) );
            }
            return results;
        } // if
        final List<Future<ComputedGradient>> futures = new ArrayList<>( batch.size() );
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
        for(final Future<ComputedGradient> future : futures) {
            if(future == null) {
                results.add(null);
                continue;
            }
            try {
                results.add( future.get() );
            }
            catch(final InterruptedException | ExecutionException e) {
                throw new RuntimeException("GruTrainer: parallel gradient computation failed", e);
            }
        } // for

        return results;
    }

    /**
     * Same parallelization strategy as {@link #computeBatch}, but for validation (loss only, no
     * gradients/Adam application) -- see the call site's comment on why this carries no staleness
     * tradeoff, unlike training
     */
    private static List<Double> computeValidationBatch(
        final ExecutorService executor,
        final GruWeights      weights,
        final Vocabulary      vocabulary,
        final List<Example>   batch
    )
    {
        final List<Double> results = new ArrayList<>( batch.size() );
        if(executor == null) {
            for(final Example example : batch) {
                if( example.targetWordIndex < 0 || example.targetWordIndex >= example.tokens.size() ) {
                    results.add(null);
                }
                else {
                    final GruClassifier.ForwardCache cache = GruClassifier.forward(
                        weights, vocabulary, example.tokens, example.targetWordIndex
                    );
                    results.add( crossEntropyLoss(cache.logits, example.classIndex) );
                }
            } // for
            return results;
        } // if
        final List<Future<Double>> futures = new ArrayList<>( batch.size() );
        for(final Example example : batch) {
            if( example.targetWordIndex < 0 || example.targetWordIndex >= example.tokens.size() ) {
                futures.add(null);
            }
            else {
                futures.add( executor.submit( new Callable<Double>() {
                    @Override
                    public Double call()
                    {
                        final GruClassifier.ForwardCache cache = GruClassifier.forward(weights, vocabulary, example.tokens, example.targetWordIndex);

                        return crossEntropyLoss(cache.logits, example.classIndex);
                    }
                } ) );
            }
        } // for
        for(final Future<Double> future : futures) {
            if(future == null) {
                results.add(null);
                continue;
            }
            try {
                results.add( future.get() );
            }
            catch(final InterruptedException | ExecutionException e) {
                throw new RuntimeException("GruTrainer: parallel validation computation failed", e);
            }
        } // for

        return results;
    }

    /**
     * Averages (not sums) the non-null {@link ComputedGradient}s of one mini-batch into a single
     * {@link GruClassifier.Gradients}, mutating and returning the first non-null entry's own
     * gradients object as the accumulator (cheaper than allocating a fresh one -- {@code
     * GruClassifier.Gradients}'s constructor is package-private and not callable from here anyway).
     * Entries skipped by {@link #computeBatch} (out-of-range {@code targetWordIndex}) are excluded
     * from both the sum and the divisor, so a mini-batch containing one or more skipped examples
     * still averages correctly over its real example count -- the same logic naturally handles the
     * last, possibly-partial batch of an epoch (divides by however many examples it actually
     * contains, never a hardcoded {@code batchSize}). Returns {@code null} if every entry in the
     * batch was skipped, matching the pre-mini-batch behavior of applying no Adam step for an
     * all-skipped batch.
     */
    private static GruClassifier.Gradients averageGradients(final List<ComputedGradient> computed)
    {
        GruClassifier.Gradients sum   = null;
        int count = 0;
        for(final ComputedGradient result : computed) {
            if(result == null) continue;
            if(sum == null) sum = result.gradients;
            else            addGradientsInto(sum, result.gradients);
            ++count;
        } // for
        if(sum == null) return null;
        scaleGradients(sum, 1.0 / count);

        return sum;
    }

    private static void addGradientsInto(final GruClassifier.Gradients dst, final GruClassifier.Gradients src)
    {
        for( final Map.Entry<Integer, double[]> entry : src.embeddingGrad.entrySet() ) {
            final double[] existing = dst.embeddingGrad.get( entry.getKey() );
            if(existing == null) dst.embeddingGrad.put( entry.getKey(), entry.getValue().clone() );
            else                 addInto( existing, entry.getValue() );
        }
        addInto(dst.forward, src.forward);
        addInto(dst.backward, src.backward);
        addInto(dst.denseW, src.denseW);
        addInto(dst.denseB, src.denseB);
        addInto(dst.outW, src.outW);
        addInto(dst.outB, src.outB);
    }

    private static void addInto(final GruWeights.DirectionWeights dst, final GruWeights.DirectionWeights src)
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

    private static void addInto(final double[][] dst, final double[][] src)
    {
        for(int i = 0; i < dst.length; ++i) addInto( dst[i], src[i] );
    }

    private static void addInto(final double[] dst, final double[] src)
    {
        for(int i = 0; i < dst.length; ++i) dst[i] += src[i];
    }

    /**
     * Same field walk as {@link #clipGradients}'s scaling half, reused here to divide a summed
     * mini-batch gradient by its example count
     */
    private static void scaleGradients(final GruClassifier.Gradients g, final double factor)
    {
        for( final double[] row : g.embeddingGrad.values() ) scale(row, factor);

        scale(g.forward, factor);
        scale(g.backward, factor);
        scale(g.denseW, factor);
        scale(g.denseB, factor);
        scale(g.outW, factor);
        scale(g.outB, factor);
    }

    /**
     * Learning-rate warmup + cosine decay schedule (see {@code --warmup-steps}/{@code --lr-min} in
     * {@link #main}). {@code warmupSteps <= 0} disables the schedule entirely -- returns {@code
     * baseLr} unconditionally, byte-for-byte the pre-schedule flat-lr behavior, so an unmodified
     * invocation (default {@code --warmup-steps=0}) is unaffected. Otherwise: linear ramp from 0 to
     * {@code baseLr} over {@code [1, warmupSteps]}, then a cosine decay from {@code baseLr} down to
     * {@code lrMin} over the remaining steps up to {@code totalSteps} (clamped to {@code lrMin} past
     * {@code totalSteps}, e.g. if early stopping never reaches it). {@code step} is the 1-based Adam
     * step counter (same counter used for bias-correction), so this is step-granular, matching
     * mini-batch training's one-Adam-step-per-batch granularity rather than per-epoch.
     */
    private static double computeScheduledLr(
        final double baseLr,
        final double lrMin,
        final int    step,
        final int    warmupSteps,
        final int    totalSteps
    )
    {
        if(warmupSteps <= 0) return baseLr;
        if(step <= warmupSteps) return baseLr * step / (double) warmupSteps;

        final int    decaySteps = Math.max(1, totalSteps - warmupSteps);
        final double progress   = Math.min( 1.0, (step - warmupSteps) / (double)decaySteps );

        return lrMin + 0.5 * (baseLr - lrMin) * ( 1 + Math.cos(Math.PI * progress) );
    }

    /**
     * Mid-epoch progress line: examples-seen/total, running average train loss so far this epoch,
     * elapsed time this epoch, and a rough estimated-time-remaining for the epoch based on the
     * average per-example rate observed so far. Printed via {@code System.out.println}, which
     * auto-flushes on newline for the console/redirected-file stdout case this is meant for (a
     * human tailing the process's own manual invocation, per the 2026-07-29 request), so no
     * explicit {@code System.out.flush()} is needed.
     */
    private static void printProgress(
        final int    epoch,
        final int    examplesSeen,
        final int    totalExamples,
        final double lossSoFar,
        final long   epochStartNanos,
        final long   trainingStartNanos
    )
    {
        final double elapsedSeconds      = ( System.nanoTime() - epochStartNanos ) / 1e9;
        final double perExampleSeconds   = elapsedSeconds / examplesSeen;
        final double etaSeconds          = perExampleSeconds* (totalExamples - examplesSeen);
        final double totalElapsedSeconds = ( System.nanoTime() - trainingStartNanos ) / 1e9;
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
         * per-epoch code previously recomputed on every pass) rather than re-tokenized on every
         * epoch -- the text/tokenization never changes across epochs, only the weights do
         */
        final List<String> tokens;

        Example(final String label, final int classIndex, final int targetWordIndex, final String text)
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
     * {@code <lang>\t<label:YES|NO>\t<targetWordIndex>\t<escaped-text>}. Lines starting with
     * {@code #} (comments) and blank lines are skipped, matching {@code sample_examples.txt}'s
     * own illustrative-shape convention.
     */
    private static List<Example> readExamples(final File examplesFile) throws IOException
    {
        final List<Example> examples = new ArrayList<>();
        for( final String line : Files.readAllLines( examplesFile.toPath(), StandardCharsets.UTF_8 ) ) {
            if( line.isEmpty() || line.startsWith("#") ) continue;
            final String[] parts = line.split("\t", 4);
            if(parts.length != 4) continue;
            final String label = parts[1];
            if( !label.equals("YES") && !label.equals("NO") ) continue;
            final int classIndex = label.equals("YES") ? 0 : 1; // GruClassifier.CLASS_ORDER: YES=0, NO=1
            final int targetWordIndex;
            try {
                targetWordIndex = Integer.parseInt( parts[2] );
            }
            catch(final NumberFormatException e) {
                continue;
            }
            final String text = unescape( parts[3] );
            examples.add( new Example(label, classIndex, targetWordIndex, text) );
        } // for

        return examples;
    }

    private static String unescape(final String s)
    {
        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < s.length(); ++i ) {
            final char c = s.charAt(i);
            if( c == '\\' && i + 1 < s.length() ) {
                final char next = s.charAt(i + 1);
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

    private static List<String> readVocab(final File vocabFile) throws IOException
    {
        final List<String> words = new ArrayList<>();
        for( final String line : Files.readAllLines( vocabFile.toPath(), StandardCharsets.UTF_8 ) ) {
            final String trimmed = line.trim();
            if( trimmed.isEmpty() || trimmed.startsWith("#") ) continue;
            words.add(trimmed);
        }

        return words;
    }

    private static List<String> buildVocab(final List<Example> examples)
    {
        final Set<String> seen = new LinkedHashSet<>();
        for(final Example example : examples) {
            for( final String token : GruClassifier.tokenize(example.text) ) seen.add(token);
        }

        return new ArrayList<>(seen);
    }

    private static double crossEntropyLoss(final double[] logits, final int trueClassIndex)
    {
        final double[] probabilities = GruClassifier.softmax(logits);

        return -Math.log( Math.max( probabilities[trueClassIndex], 1e-12 ) );
    }

    private static GruWeights randomInit(
        final List<String> explicitVocab,
        final Vocabulary   vocabulary,
        final Random       random
    )
    {
        final int        embeddingRows = vocabulary.totalEmbeddingRows();
        final double[][] embeddings    = new double[embeddingRows][EMBEDDING_DIM];
        for(int i = 0; i < embeddingRows; ++i) {
            for(int j = 0; j < EMBEDDING_DIM; ++j) embeddings[i][j] = glorot(
                random, EMBEDDING_DIM, EMBEDDING_DIM
            );
        }

        final GruWeights.DirectionWeights forward = randomDirectionWeights(random);
        final GruWeights.DirectionWeights backward = randomDirectionWeights(random);

        final double[][] denseW = new double[DENSE_SIZE][2* HIDDEN_SIZE];
        for(int i = 0; i < DENSE_SIZE; ++i) {
            for(int j = 0; j < 2 * HIDDEN_SIZE; ++j) denseW[i][j] = glorot(
                random, 2 * HIDDEN_SIZE, DENSE_SIZE
            );
        }
        final double[] denseB = new double[DENSE_SIZE];

        final double[][] outW = new double[GruClassifier.CLASS_ORDER.length][DENSE_SIZE];
        for(int i = 0; i < outW.length; ++i) {
            for(int j = 0; j < DENSE_SIZE; ++j) outW[i][j] = glorot(
                random, DENSE_SIZE, outW.length
            );
        }
        final double[] outB = new double[GruClassifier.CLASS_ORDER.length];

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

    private static GruWeights.DirectionWeights randomDirectionWeights(final Random random)
    {
        final GruWeights.DirectionWeights weights = GruWeights.DirectionWeights.zeros(
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

    private static void fillGlorot(final double[][] matrix, final Random random, final int fanIn, final int fanOut)
    {
        for( final double[] row : matrix ) {
            for(int j = 0; j < row.length; ++j) row[j] = glorot(random, fanIn, fanOut);
        }
    }

    private static double glorot(final Random random, final int fanIn, final int fanOut)
    {
        final double limit = Math.sqrt( 6.0 / (fanIn + fanOut) );

        return ( random.nextDouble() * 2 - 1 ) * limit;
    }

    /**
     * Clips the entire gradient set to the specified global L2 norm. This is the
     * standard "global norm" clipping used by most GRU/LSTM implementations rather
     * than clipping each tensor independently, since it preserves the direction of
     * the update while only reducing its magnitude when necessary.
     */
    private static void clipGradients(final GruClassifier.Gradients gradients, final double maxNorm)
    {
        double sumSquares = 0.0;

        for( final double[] row : gradients.embeddingGrad.values() ) {
            for(final double v : row) sumSquares += v * v;
        }

        sumSquares += sumSquares(gradients.forward);
        sumSquares += sumSquares(gradients.backward);
        sumSquares += sumSquares(gradients.denseW);
        sumSquares += sumSquares(gradients.denseB);
        sumSquares += sumSquares(gradients.outW);
        sumSquares += sumSquares(gradients.outB);

        final double norm = Math.sqrt(sumSquares);
        if(norm <= maxNorm || norm == 0.0) return;

        final double scale = maxNorm / norm;

        for( final double[] row : gradients.embeddingGrad.values() ) scale(row, scale);

        scale(gradients.forward, scale);
        scale(gradients.backward, scale);
        scale(gradients.denseW, scale);
        scale(gradients.denseB, scale);
        scale(gradients.outW, scale);
        scale(gradients.outB, scale);
    }

    private static double sumSquares(final GruWeights.DirectionWeights w)
    {
        return sumSquares(w.Wz) + sumSquares(w.Wr) + sumSquares(w.Wh)
                + sumSquares(w.Uz) + sumSquares(w.Ur) + sumSquares(w.Uh)
                + sumSquares(w.bz) + sumSquares(w.br) + sumSquares(w.bh);
    }

    private static double sumSquares(final double[][] m)
    {
        double s = 0.0;
        for( final double[] row : m ) s += sumSquares(row);

        return s;
    }

    private static double sumSquares(final double[] v)
    {
        double s = 0.0;
        for(final double x : v) s += x * x;

        return s;
    }

    private static void scale(final GruWeights.DirectionWeights w, final double factor)
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

    private static void scale(final double[][] m, final double factor)
    {
        for( final double[] row : m ) scale(row, factor);
    }

    private static void scale(final double[] v, final double factor)
    {
        for(int i = 0; i < v.length; ++i) v[i] *= factor;
    }

    /**
     * Adam optimizer state (first/second moment estimates), one pair of accumulator arrays per
     * trained-weight array, mirroring {@link GruWeights}'s field layout exactly so each weight
     * array's update can be applied in lockstep with its gradient. Embedding-row moments are kept
     * densely (one row per vocab+hash-bucket slot) even though any single example's gradient only
     * touches a handful of rows -- the corpora here are small enough that this is simpler than a
     * sparse moment table, and moment state must persist across examples/epochs regardless.
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

        AdamState(final GruWeights weights)
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

            DirectionMoments(final int hiddenSize, final int embeddingDim)
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

        void apply(final GruWeights weights, final GruClassifier.Gradients gradients, final double lr, final int step)
        {
            final double biasCorrection1 = 1.0 - Math.pow(BETA1, step);
            final double biasCorrection2 = 1.0 - Math.pow(BETA2, step);

            for( final Map.Entry<Integer, double[]> entry : gradients.embeddingGrad.entrySet() ) {
                final int row = entry.getKey();
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
            final GruWeights.DirectionWeights weights,
            final GruWeights.DirectionWeights gradients,
            final DirectionMoments            moments,
            final double                      lr,
            final double                      biasCorrection1,
            final double                      biasCorrection2
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
            final double[] param,
            final double[] grad,
            final double[] m,
            final double[] v,
            final double   lr,
            final double   biasCorrection1,
            final double   biasCorrection2
        )
        {
            for(int i = 0; i < param.length; ++i) {
                m[i] = BETA1 * m[i] + (1 - BETA1) * grad[i];
                v[i] = BETA2 * v[i] + (1 - BETA2) * grad[i] * grad[i];
                final double mHat = m[i] / biasCorrection1;
                final double vHat = v[i] / biasCorrection2;
                param[i] -= lr * mHat / ( Math.sqrt(vHat) + EPSILON );
            } // for
        }

        private static void update2D(
            final double[][] param,
            final double[][] grad,
            final double[][] m,
            final double[][] v,
            final double     lr,
            final double     biasCorrection1,
            final double     biasCorrection2
        )
        {
            for(int i = 0; i < param.length; ++i) update1D(
                param[i], grad[i], m[i], v[i], lr, biasCorrection1, biasCorrection2
            );
        }

    } // class AdamState

    // ── Checkpoint binary I/O ────────────────────────────────────────────────────────────────────

    /**
     * {@code weightsOut}-relative path where the current-weights checkpoint's sibling best-weights
     * checkpoint lives, given the current-weights checkpoint's own {@code File}. Both are always
     * written together from the same {@code weightsOut} base path (see the constant block above), so
     * this is a plain suffix swap, not a search.
     */
    private static File deriveBestCheckpointFile(final File currentCheckpointFile)
    {
        final String path = currentCheckpointFile.getPath();
        if( path.endsWith(CHECKPOINT_CURRENT_SUFFIX) ) return new File(
            path.substring( 0, path.length() - CHECKPOINT_CURRENT_SUFFIX.length() ) + CHECKPOINT_BEST_SUFFIX
        );

        // Best-effort fallback for a --resume path that doesn't end in the expected suffix (e.g. a
        // user manually renamed the file) -- still deterministic, just less likely to find a real
        // sibling; readBestCheckpoint's caller already treats "no sibling found" as a handled,
        // documented case, not a hard error
        return new File(path + CHECKPOINT_BEST_SUFFIX);
    }

    private static void writeDoubleArray(final DataOutputStream out, final double[] values) throws IOException
    {
        out.writeInt(values.length);
        for(final double v : values) out.writeDouble(v);
    }

    private static double[] readDoubleArray(final DataInputStream in) throws IOException
    {
        final int      n      = in.readInt();
        final double[] values = new double[n];
        for(int i = 0; i < n; ++i) values[i] = in.readDouble();

        return values;
    }

    private static void readDoubleArrayInto(final DataInputStream in, final double[] target) throws IOException
    {
        final int n = in.readInt();
        if(n != target.length) throw new IOException(
            "checkpoint shape mismatch: expected length " + target.length + ", found " + n
        );
        for(int i = 0; i < n; ++i) target[i] = in.readDouble();
    }

    private static void writeDoubleArray2D(final DataOutputStream out, final double[][] rows) throws IOException
    {
        out.writeInt(rows.length);
        for( final double[] row : rows ) writeDoubleArray(out, row);
    }

    private static double[][] readDoubleArray2D(final DataInputStream in) throws IOException
    {
        final int        n    = in.readInt();
        final double[][] rows = new double[n][];
        for(int i = 0; i < n; ++i) rows[i] = readDoubleArray(in);

        return rows;
    }

    private static void readDoubleArray2DInto(
        final DataInputStream in, final double[][] target
    ) throws IOException
    {
        final int n = in.readInt();
        if(n != target.length) throw new IOException(
            "checkpoint shape mismatch: expected " + target.length + " rows, found " + n
        );
        for(int i = 0; i < n; ++i) readDoubleArrayInto( in, target[i] );
    }

    private static void writeDirectionWeights(
        final DataOutputStream out, final GruWeights.DirectionWeights w
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

    private static GruWeights.DirectionWeights readDirectionWeights(
        final DataInputStream in
    ) throws IOException
    {
        final double[][] Wz = readDoubleArray2D(in);
        final double[][] Wr = readDoubleArray2D(in);
        final double[][] Wh = readDoubleArray2D(in);
        final double[][] Uz = readDoubleArray2D(in);
        final double[][] Ur = readDoubleArray2D(in);
        final double[][] Uh = readDoubleArray2D(in);
        final double[]   bz = readDoubleArray(in);
        final double[]   br = readDoubleArray(in);
        final double[]   bh = readDoubleArray(in);

        return new GruWeights.DirectionWeights(Wz, Wr, Wh, Uz, Ur, Uh, bz, br, bh);
    }

    /**
     * Both checkpoint kinds share this block: schema/architecture scalars, the vocab (needed to
     * reconstruct a {@link Vocabulary}), and every {@link GruWeights} array {@code toJsonFields}
     * already enumerates -- same field list, not a separately-invented one
     */
    private static void writeWeightsBlock(
        final DataOutputStream out,
        final GruWeights       weights,
        final List<String>     explicitVocab
    ) throws IOException
    {
        out.writeInt(weights.schemaVersion);
        out.writeInt( explicitVocab.size() );
        out.writeInt(weights.hashBuckets);
        out.writeInt(weights.embeddingDim);
        out.writeInt(weights.hiddenSize);
        out.writeInt(weights.sequenceCap);
        out.writeInt(weights.numClasses);
        out.writeDouble(weights.abstainThreshold);
        for(final String word : explicitVocab) out.writeUTF(word);
        writeDoubleArray2D(out, weights.embeddings);
        writeDirectionWeights(out, weights.forward);
        writeDirectionWeights(out, weights.backward);
        writeDoubleArray2D(out, weights.denseW);
        writeDoubleArray(out, weights.denseB);
        writeDoubleArray2D(out, weights.outW);
        writeDoubleArray(out, weights.outB);
    }

    /**
     * Loaded {@link GruWeights} plus the {@code explicitVocab} list it was built from -- {@code
     * GruWeights} itself only exposes the vocab as a {@code String[]}; callers here generally want
     * the {@code List<String>} shape {@link #toJson} etc. already take
     */
    private static final class LoadedWeights {

        final List<String> explicitVocab;
        final GruWeights   weights;

        LoadedWeights(final List<String> explicitVocab, final GruWeights weights)
        {
            this.explicitVocab = explicitVocab;
            this.weights       = weights;
        }

    } // class LoadedWeights

    private static LoadedWeights readWeightsBlock(final DataInputStream in) throws IOException
    {
        final int          schemaVersion    = in.readInt();
        final int          vocabSize        = in.readInt();
        final int          hashBuckets      = in.readInt();
        final int          embeddingDim     = in.readInt();
        final int          hiddenSize       = in.readInt();
        final int          sequenceCap      = in.readInt();
        final int          numClasses       = in.readInt();
        final double       abstainThreshold = in.readDouble();
        final List<String> explicitVocab    = new ArrayList<>(vocabSize);
        for(int i = 0; i < vocabSize; ++i) explicitVocab.add( in.readUTF() );
        final double[][] embeddings = readDoubleArray2D(in);
        final GruWeights.DirectionWeights forward  = readDirectionWeights(in);
        final GruWeights.DirectionWeights backward = readDirectionWeights(in);
        final double[][] denseW = readDoubleArray2D(in);
        final double[]   denseB = readDoubleArray(in);
        final double[][] outW   = readDoubleArray2D(in);
        final double[]   outB   = readDoubleArray(in);

        final GruWeights weights = new GruWeightsBuilder().schemaVersion(
            schemaVersion
        ).vocabSize(
            vocabSize
        ).hashBuckets(
            hashBuckets
        ).embeddingDim(
            embeddingDim
        ).hiddenSize(
            hiddenSize
        ).sequenceCap(
            sequenceCap
        ).numClasses(
            numClasses
        ).abstainThreshold(
            abstainThreshold
        ).explicitVocab(
            explicitVocab.toArray( new String[0] )
        ).embeddings(
            embeddings
        ).forward(
            forward
        ).backward(
            backward
        ).denseW(
            denseW
        ).denseB(
            denseB
        ).outW(
            outW
        ).outB(
            outB
        ).build();

        return new LoadedWeights(explicitVocab, weights);
    }

    /**
     * Writes the best-weights checkpoint (weights + vocab only, plus the validation loss that earned
     * it, for human inspection -- no Adam/run state, this is "give me the best model so far", not a
     * resume target on its own). Overwritten only when validation loss improves. Temp-file-then-
     * atomic-rename, see the constant block's javadoc.
     */
    private static void writeBestCheckpoint(
        final File         file,
        final GruWeights   weights,
        final List<String> explicitVocab,
        final double       bestValidationLoss
    ) throws IOException
    {
        final File tmp = new File( file.getPath() + ".tmp" );
        try( final DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream( new FileOutputStream(tmp) )
        ) ) {
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
     * this, the temp file's bytes (and, separately, the rename itself -- see
     * {@link #fsyncParentDirectory}) can still be sitting in the OS page cache and lost on a real
     * power failure, even though a plain process kill can never corrupt the file (temp-file-then-
     * atomic-rename, see the constant block's javadoc)
     */
    private static void fsyncFile(final File f) throws IOException
    {
        try( final FileChannel ch = FileChannel.open( f.toPath(), StandardOpenOption.WRITE ) ) {
            ch.force(true);
        }
    }

    /**
     * Forces the directory entry (the rename itself) to durable storage -- Linux-only (opening a
     * directory as a {@link FileChannel} is not portable to Windows), acceptable here since this
     * tool only runs on this project's own Linux dev/build hosts
     */
    private static void fsyncParentDirectory(final File file) throws IOException
    {
        final File parent = file.getAbsoluteFile().getParentFile();
        if(parent == null) return;
        try( final FileChannel ch = FileChannel.open( parent.toPath(), StandardOpenOption.READ ) ) {
            ch.force(true);
        }
    }

    private static LoadedWeights readBestCheckpoint(final File file) throws IOException
    {
        try( final DataInputStream in = new DataInputStream(
            new BufferedInputStream( new FileInputStream(file) )
        ) ) {
            final int magic = in.readInt();
            if(magic != CHECKPOINT_MAGIC) throw new IOException(
                "not a GruTrainer checkpoint file (bad magic): " + file
            );
            final int formatVersion = in.readInt();
            if(formatVersion != CHECKPOINT_FORMAT_VERSION) throw new IOException(
                "unsupported checkpoint format version " + formatVersion + " in " + file
                        + " (expected " + CHECKPOINT_FORMAT_VERSION + ")"
            );
            final int kind = in.readInt();
            if(kind != 0) throw new IOException(
                "expected a best-weights checkpoint (kind=0) but found kind=" + kind + " in " + file
            );
            final LoadedWeights loaded = readWeightsBlock(in);
            in.readDouble(); // BestValidationLoss -- informational only, caller doesn't need it back

            return loaded;
        }
    }

    private static void writeDirectionMoments(
        final DataOutputStream out, final AdamState.DirectionMoments m
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
        final DataInputStream in, final AdamState.DirectionMoments m
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

    private static void writeAdamState(final DataOutputStream out, final AdamState adam) throws IOException
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

    /**
     * Allocates a fresh {@link AdamState} sized to {@code weights} (all-zero moments, same as
     * {@code new AdamState(weights)} at the start of a fresh run), then overwrites its arrays'
     * contents in place from {@code in} -- the moment arrays are {@code final} references, so this
     * fills them rather than reassigning
     */
    private static AdamState readAdamStateInto(
        final DataInputStream in, final GruWeights weights
    ) throws IOException
    {
        final AdamState adam = new AdamState(weights);
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
     * moment arrays -- not just the raw weight arrays, since resuming without them would restart the
     * optimizer's momentum from scratch and defeat the point of a faithful resume -- and scalar run
     * state). Overwritten once per epoch, after that epoch's Adam updates + validation-loss
     * computation. Temp-file-then-atomic-rename, see the constant block's javadoc.
     *
     *  <p><b>Resume fidelity caveat</b> (documented here and at the {@code --resume} call site): the
     *  RNG seed is persisted, not {@code java.util.Random}'s internal state -- resuming reproduces the
     *  exact original train/validation split (deterministic given the same seed) but not the exact
     *  per-epoch shuffle sequence beyond that point. Accepted limitation, not a bug.
     */
    private static void writeCurrentCheckpoint(
        final File         file,
        final GruWeights   weights,
        final List<String> explicitVocab,
        final AdamState    adam,
        final int          epoch,
        final int          epochsSinceImprovement,
        final double       bestValidationLoss,
        final double       learningRate,
        final int          maxEpochs,
        final int          patience,
        final long         seed,
        final int          step,
        final int          batchSize,
        final int          warmupSteps,
        final double       lrMin
    ) throws IOException
    {
        final File tmp = new File( file.getPath() + ".tmp" );
        try( final DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream( new FileOutputStream(tmp) )
        ) ) {
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

    /**
     * {@link #writeCurrentCheckpoint} wrapper for the per-epoch call sites in {@code main}: a failed
     * checkpoint write must never abort a real training run (it's a safety net, not the run's actual
     * deliverable), so this catches and warns instead of propagating
     */
    private static void writeCurrentCheckpointQuietly(
        final File         file,
        final GruWeights   weights,
        final List<String> explicitVocab,
        final AdamState    adam,
        final int          epoch,
        final int          epochsSinceImprovement,
        final double       bestValidationLoss,
        final double       learningRate,
        final int          maxEpochs,
        final int          patience,
        final long         seed,
        final int          step,
        final int          batchSize,
        final int          warmupSteps,
        final double       lrMin
    )
    {
        try {
            writeCurrentCheckpoint(
                file, weights, explicitVocab, adam, epoch, epochsSinceImprovement, bestValidationLoss,
                learningRate, maxEpochs, patience, seed, step, batchSize, warmupSteps, lrMin
            );
        }
        catch(final IOException e) {
            System.err.println( "GruTrainer: warning -- could not write current-weights checkpoint to "
                    + file + ": " + e.getMessage() );
        }
    }

    /**
     * Full state restored by {@code --resume=<path>}: weights, vocab, Adam optimizer moments, and
     * every scalar the epoch loop needs to continue faithfully (epoch/patience bookkeeping, the
     * hyperparameters that were in effect, and the Adam step counter -- needed for bias-correction
     * continuity, not just cosmetic)
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
            final List<String> explicitVocab,
            final GruWeights   weights,
            final AdamState    adam,
            final int          epoch,
            final int          epochsSinceImprovement,
            final double       bestValidationLoss,
            final double       learningRate,
            final int          maxEpochs,
            final int          patience,
            final long         seed,
            final int          step,
            final int          batchSize,
            final int          warmupSteps,
            final double       lrMin
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

    private static ResumeState loadCurrentCheckpoint(final File file) throws IOException
    {
        try( final DataInputStream in = new DataInputStream(
            new BufferedInputStream( new FileInputStream(file) )
        ) ) {
            final int magic = in.readInt();
            if(magic != CHECKPOINT_MAGIC) throw new IOException(
                "not a GruTrainer checkpoint file (bad magic): " + file
            );
            final int formatVersion = in.readInt();
            if(formatVersion != CHECKPOINT_FORMAT_VERSION) throw new IOException(
                "unsupported checkpoint format version " + formatVersion + " in " + file
                        + " (expected " + CHECKPOINT_FORMAT_VERSION + ")"
            );
            final int kind = in.readInt();
            if(kind != 1) throw new IOException(
                "expected a current-weights checkpoint (kind=1) but found kind=" + kind + " in " + file
            );
            final LoadedWeights loaded                 = readWeightsBlock(in);
            final int           epoch                  = in.readInt();
            final int           epochsSinceImprovement = in.readInt();
            final double        bestValidationLoss     = in.readDouble();
            final double        learningRate           = in.readDouble();
            final int           maxEpochs              = in.readInt();
            final int           patience               = in.readInt();
            final long          seed                   = in.readLong();
            final int           step                   = in.readInt();
            final int           batchSize              = in.readInt();
            final int           warmupSteps            = in.readInt();
            final double        lrMin                  = in.readDouble();
            final AdamState     adam                   = readAdamStateInto(in, loaded.weights);

            return new ResumeState(
                loaded.explicitVocab, loaded.weights, adam, epoch, epochsSinceImprovement,
                bestValidationLoss, learningRate, maxEpochs, patience, seed, step, batchSize,
                warmupSteps, lrMin
            );
        }
    }

    /**
     * Builder for {@link GruWeights} -- its own constructor is package-private (only
     * {@code GruWeights.load} normally builds instances), so this class hand-assembles the same
     * JSON {@code GruWeights.load} parses instead of trying to call that constructor directly.
     * Kept as a builder (rather than one giant constructor call) only to keep {@link #randomInit}
     * readable given how many fields {@link GruWeights} now has.
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

        GruWeightsBuilder schemaVersion   (final int                         v) { this.schemaVersion = v; return this;    }
        GruWeightsBuilder vocabSize       (final int                         v) { this.vocabSize = v; return this;        }
        GruWeightsBuilder hashBuckets     (final int                         v) { this.hashBuckets = v; return this;      }
        GruWeightsBuilder embeddingDim    (final int                         v) { this.embeddingDim = v; return this;     }
        GruWeightsBuilder hiddenSize      (final int                         v) { this.hiddenSize = v; return this;       }
        GruWeightsBuilder sequenceCap     (final int                         v) { this.sequenceCap = v; return this;      }
        GruWeightsBuilder numClasses      (final int                         v) { this.numClasses = v; return this;       }
        GruWeightsBuilder abstainThreshold(final double                      v) { this.abstainThreshold = v; return this; }
        GruWeightsBuilder explicitVocab   (final String[]                    v) { this.explicitVocab = v; return this;    }
        GruWeightsBuilder embeddings      (final double[][]                  v) { this.embeddings = v; return this;       }
        GruWeightsBuilder forward         (final GruWeights.DirectionWeights v) { this.forward = v; return this;          }
        GruWeightsBuilder backward        (final GruWeights.DirectionWeights v) { this.backward = v; return this;         }
        GruWeightsBuilder denseW          (final double[][]                  v) { this.denseW = v; return this;           }
        GruWeightsBuilder denseB          (final double[]                    v) { this.denseB = v; return this;           }
        GruWeightsBuilder outW            (final double[][]                  v) { this.outW = v; return this;             }
        GruWeightsBuilder outB            (final double[]                    v) { this.outB = v; return this;             }

        GruWeights build()
        {
            // GruWeights has no public constructor (see class javadoc above) -- round-trip through
            // its own JSON schema instead, which is public API (GruWeights.load)
            final String json = toJson(this);
            try {
                final java.nio.file.Path tmp = Files.createTempFile("gru_trainer_init", ".json");
                Files.write( tmp, json.getBytes(StandardCharsets.UTF_8) );
                final GruWeights result = GruWeights.load(tmp);
                Files.deleteIfExists(tmp);
                return result;
            }
            catch(final IOException e) {
                throw new IllegalStateException(
                    "GruTrainer: failed to round-trip initial weights: " + e.getMessage(), e
                );
            }
        }

        private static String toJson(final GruWeightsBuilder b)
        {
            return GruTrainer.toJsonFields(
                b.schemaVersion, b.vocabSize, b.hashBuckets, b.embeddingDim,
                b.hiddenSize, b.sequenceCap, b.numClasses, b.abstainThreshold, b.explicitVocab,
                b.embeddings, b.forward, b.backward, b.denseW, b.denseB, b.outW, b.outB
            );
        }

    } // class GruWeightsBuilder

    private static String toJson(final GruWeights weights, final List<String> explicitVocab)
    {
        return toJsonFields(
            weights.schemaVersion, explicitVocab.size(), weights.hashBuckets, weights.embeddingDim,
            weights.hiddenSize, weights.sequenceCap, weights.numClasses, weights.abstainThreshold,
            explicitVocab.toArray( new String[0] ), weights.embeddings, weights.forward, weights.backward,
            weights.denseW, weights.denseB, weights.outW, weights.outB
        );
    }

    private static String toJsonFields(
        final int                         schemaVersion,
        final int                         vocabSize,
        final int                         hashBuckets,
        final int                         embeddingDim,
        final int                         hiddenSize,
        final int                         sequenceCap,
        final int                         numClasses,
        final double                      abstainThreshold,
        final String[]                    explicitVocab,
        final double[][]                  embeddings,
        final GruWeights.DirectionWeights forward,
        final GruWeights.DirectionWeights backward,
        final double[][]                  denseW,
        final double[]                    denseB,
        final double[][]                  outW,
        final double[]                    outB
    )
    {
        final StringBuilder sb = new StringBuilder();
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
        final StringBuilder               sb,
        final String                      prefix,
        final GruWeights.DirectionWeights weights
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

    private static String jsonArray1D(final double[] values)
    {
        final StringBuilder sb = new StringBuilder("[");
        for(int i = 0; i < values.length; ++i) {
            if(i > 0) sb.append(",");
            sb.append( values[i] );
        }

        return sb.append("]").toString();
    }

    private static String jsonArray2D(final double[][] rows)
    {
        final StringBuilder sb = new StringBuilder("[");
        for(int i = 0; i < rows.length; ++i) {
            if(i > 0) sb.append(",");
            sb.append( jsonArray1D( rows[i] ) );
        }

        return sb.append("]").toString();
    }

    private static String jsonStringArray(final String[] values)
    {
        final StringBuilder sb = new StringBuilder("[");
        for(int i = 0; i < values.length; ++i) {
            if(i > 0) sb.append(",");
            sb.append("\"").append( escapeJsonString( values[i] ) ).append("\"");
        }

        return sb.append("]").toString();
    }

    private static String escapeJsonString(final String s)
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
