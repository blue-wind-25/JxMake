/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.jxmake.formatter.classifier.CommentDecision;
import com.jxmake.formatter.classifier.gru.GruClassifier;

/**
 * Measures precision of a trained {@code GruClassifier} against an RDD_EXT_21-schema
 *  labeled-examples file: loads the weights file, classifies every example, and reports
 *  correct/decided/abstained counts overall and per YES/NO class. Deliberately lives outside
 *  {@code src/} alongside {@link GruTrainer}, since it's evaluation tooling for real corpora, not
 *  shipped runtime code -- same category as {@code CommentAbstainTally.java}. {@link
 *  #tools/gru/cross_validate.py} shells out to this class once per fold.
 */
public final class GruEval {

    private GruEval()
    {
    }

    private static final String USAGE =
        "Usage: GruEval <weights-path> <rdd-ext-21-examples-path> [threshold ...]";

    /**
     * A single labeled example plus its pre-computed softmax probabilities, cached so a
     *  --threshold sweep (below) evaluates every candidate threshold against the exact same
     *  forward-pass outputs instead of recomputing them once per threshold.
     */
    private static final class Scored {
        final String   label;
        final double[] probabilities; // null => classifier.probabilities() returned null (ABSTAIN
                                       // unconditionally, e.g. out-of-range target index)

        Scored(String label, double[] probabilities)
        {
            this.label         = label;
            this.probabilities = probabilities;
        }
    } // class Scored

    public static void main(String[] args) throws IOException
    {
        if(args.length < 2) {
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        Path          weightsPath  = Paths.get( args[0] );
        Path          examplesPath = Paths.get( args[1] );
        GruClassifier classifier   = GruClassifier.load(weightsPath);

        // args[2..]: optional threshold sweep (see GruClassifier.probabilities/decide) -- if none
        // given, evaluate at the weights file's own trained abstainThreshold only, matching this
        // tool's original (pre-sweep) single-line-output behavior exactly.
        double[] thresholds;
        if(args.length > 2) {
            thresholds = new double[args.length - 2];
            for(int i = 2; i < args.length; ++i) {
                double t = Double.parseDouble( args[i] );
                if(t < 0.0 || t > 1.0) {
                    System.err.println("GruEval: threshold must be within [0.0, 1.0], got " + t);
                    System.exit(2);
                    return;
                }
                thresholds[i - 2] = t;
            } // for
        } // if
        else {
            thresholds = new double[] { classifier.abstainThreshold() };
        }

        List<Scored> scored = new java.util.ArrayList<>();
        for( String line : Files.readAllLines(examplesPath, StandardCharsets.UTF_8) ) {
            if( line.isEmpty() ) continue;
            String[] parts = line.split("\t", 4);
            if(parts.length != 4) continue;
            String label       = parts[1];
            int    targetIndex = Integer.parseInt( parts[2] );
            String text        = unescape( parts[3] );
            scored.add( new Scored( label, classifier.probabilities(text, targetIndex) ) );
        } // for

        for(double threshold : thresholds) {
            evaluateAt(scored, threshold);
        }
    }

    private static void evaluateAt(List<Scored> scored, double threshold)
    {
        int yesCorrect = 0, yesIncorrect = 0, noCorrect = 0, noIncorrect = 0, abstain = 0;
        int total = scored.size();
        for(Scored s : scored) {
            CommentDecision verdict = s.probabilities == null
                    ? CommentDecision.ABSTAIN : GruClassifier.decide(s.probabilities, threshold);
            if(verdict == CommentDecision.ABSTAIN) {
                ++abstain;
                continue;
            }
            boolean predictedYes = verdict == CommentDecision.YES;
            boolean actualYes    = s.label.equals("YES");
            if(actualYes) {
                if(predictedYes) yesCorrect++;
                else             yesIncorrect++;
            }
            else {
                if(predictedYes) noIncorrect++;
                else             noCorrect++;
            }
        } // for

        int decided = total - abstain;
        int correct = yesCorrect + noCorrect;
        double precision = decided == 0 ? 0.0 : (double) correct / decided;
        System.out.println("threshold=" + threshold
                + " total=" + total + " abstain=" + abstain + " decided=" + decided
                + " correct=" + correct + " precision=" + precision
                + " yesCorrect=" + yesCorrect + " yesIncorrect=" + yesIncorrect
                + " noCorrect=" + noCorrect + " noIncorrect=" + noIncorrect);
    }

    private static String unescape(String escaped)
    {
        StringBuilder out = new StringBuilder();
        for( int i = 0; i < escaped.length(); ++i ) {
            char c = escaped.charAt(i);
            if( c == '\\' && i + 1 < escaped.length() ) {
                char nxt = escaped.charAt(i + 1);
                if(nxt == 'n') {
                    out.append('\n');
                    ++i;
                    continue;
                }
                if(nxt == 't') {
                    out.append('\t');
                    ++i;
                    continue;
                }
                if(nxt == '\\') {
                    out.append('\\');
                    ++i;
                    continue;
                }
            } // if
            out.append(c);
        } // for

        return out.toString();
    }

} // class GruEval
