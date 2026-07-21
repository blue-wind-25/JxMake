/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

/** Training entry point for the Step 3 GRU comment-classifier, per STATE_NEXT_AI.md's "GRU
 *  implementation design". Deliberately lives outside {@code src/} -- the runtime JAR must never
 *  bundle training code or a training-only ML dependency; this writes a weights file for
 *  {@code com.jxmake.formatter.classifier.gru.GruClassifier}/{@code GruWeights} to read at
 *  runtime, it does not generate or overwrite any {@code .java} source.
 *
 *  <p>Skeleton only -- actual training (forward/backward pass, optimizer, weights-file writer)
 *  is blocked on STATE_NEXT_AI.md's still-open items: hyperparameters (item 3), evaluation target
 *  (item 4), a measured ABSTAIN rate (item 9), and the licensing/provenance check for bulk-sourced
 *  comment data (item 10) -- none of those have been resolved yet, so this stub does not guess at
 *  them. Do not implement the training loop or hardcode hyperparameters until those are resolved
 *  per STATE_COMMON.md's ambiguity-handling protocol. */
public final class GruTrainer {

    private GruTrainer() {
    }

    /** Expected usage: {@code GruTrainer <labeled-examples-path> <output-weights-path> [hyperparameters...]}.
     *  Not yet implemented -- see class-level note. */
    public static void main(String[] args) {
        throw new UnsupportedOperationException(
                "GruTrainer: training pipeline not yet implemented -- blocked on STATE_NEXT_AI.md "
                        + "open items 3/4/9/10 (hyperparameters, evaluation target, measured "
                        + "ABSTAIN rate, licensing check), per STATE_COMMON.md's ambiguity-handling "
                        + "protocol");
    }
}
