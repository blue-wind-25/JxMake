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

    private static final String USAGE = "Usage: GruEval <weights-path> <rdd-ext-21-examples-path>";

    public static void main(String[] args) throws IOException
    {
        if(args.length != 2) {
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        Path          weightsPath  = Paths.get( args[0] );
        Path          examplesPath = Paths.get( args[1] );
        GruClassifier classifier   = GruClassifier.load(weightsPath);

        int yesCorrect = 0, yesIncorrect = 0, noCorrect = 0, noIncorrect = 0, abstain = 0, total = 0;
        for( String line : Files.readAllLines(examplesPath, StandardCharsets.UTF_8) ) {
            if( line.isEmpty() ) continue;
            String[] parts = line.split("\t", 4);
            if(parts.length != 4) continue;
            String label       = parts[1];
            int    targetIndex = Integer.parseInt( parts[2] );
            String text        = unescape( parts[3] );
            ++total;

            CommentDecision verdict = classifier.classify(text, targetIndex);
            if(verdict == CommentDecision.ABSTAIN) {
                ++abstain;
                continue;
            }
            boolean predictedYes = verdict == CommentDecision.YES;
            boolean actualYes    = label.equals("YES");
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
        System.out.println("total=" + total + " abstain=" + abstain + " decided=" + decided
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
