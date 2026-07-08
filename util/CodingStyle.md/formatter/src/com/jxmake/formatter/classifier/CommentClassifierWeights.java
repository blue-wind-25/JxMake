package com.jxmake.formatter.classifier;

/** Weight constants for {@link CommentClassifier}'s scoring formula ({@code score = w*x + bias},
 *  threshold compare). Placeholder/zero until RDD_KEY_97's frontier-model-assisted weight
 *  generation happens -- with all weights zero, every score falls below any positive threshold,
 *  so the classifier safely no-ops (always {@link CommentDecision#ABSTAIN}) until real values
 *  are supplied here. Do not hand-tune or corpus-train these; see RDD_KEY_97. */
public final class CommentClassifierWeights {

    // TODO(comment-grammar): replace with real weights per RDD_KEY_97 (frontier-model-assisted,
    // offline, one-time, baked into JAR as constants) once a per-language example set exists.
    public static final double BIAS = 0.0;

    // TODO(comment-grammar): threshold value from RDD_KEY_98's 99%-precision target, set from
    // data once real weights exist. 0.0 with all-zero weights means every score sits exactly at
    // threshold, so CommentClassifier must resolve ties to ABSTAIN, never YES.
    public static final double THRESHOLD = 0.0;

    private CommentClassifierWeights() {
    }
}
