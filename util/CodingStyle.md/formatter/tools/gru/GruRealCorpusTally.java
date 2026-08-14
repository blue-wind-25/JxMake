/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.classifier.CommentClassifier;
import com.jxmake.formatter.classifier.CommentDecision;
import com.jxmake.formatter.classifier.CommentFeatureExtractor;
import com.jxmake.formatter.classifier.CommentFeatureVector;
import com.jxmake.formatter.classifier.gru.GruClassifier;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

/**
 * Runs the real rules-then-GRU-on-abstain pipeline (mirroring
 *  {@link com.jxmake.formatter.classifier.gru.GruAbstainResolver#resolve} /
 *  {@link com.jxmake.formatter.rules.MiscRuleCore#classifyComment(String)}'s leading-token,
 *  {@code targetWordIndex=0} call site) over an {@code extract_comments.py} corpus, at a swept
 *  set of {@code abstainThreshold} values, and tallies the GRU stage's YES/NO/ABSTAIN decision
 *  distribution -- separately from {@link GruEval}, which measures precision against a *labeled*
 *  set. This tool needs no ground truth, so it's the one to reach for on a real, unrelated
 *  codebase where hand-labeling isn't practical: a low abstain rate and/or a heavy single-class
 *  skew on truly out-of-distribution text is itself a signal worth checking (was the reported
 *  precision figure training-fit, not genuinely held-out? -- see {@code STATE_AI.md}'s repeated
 *  training-fit-vs-held-out gap, e.g. the 98.7%-vs-86.3% 2026-08-02 entry). Comments the rule-based
 *  classifier already decides are skipped entirely (never reach the GRU in production either) --
 *  run the corpus through {@link FilterAbstain} first so every line actually exercises the GRU
 *  stage, rather than sampling from a raw {@code extract_comments.py} dump where most lines never
 *  would.
 */
public final class GruRealCorpusTally {

    private GruRealCorpusTally()
    {
    }

    private static final String USAGE = "Usage: GruRealCorpusTally <weights-path> <extracted-comments-path> <threshold> [threshold ...]";

    public static void main(String[] args) throws Exception
    {
        if(args.length < 3) {
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        final Path     weightsPath = Paths.get( args[0] );
        final Path     corpusPath  = Paths.get( args[1] );
        final double[] thresholds  = new double[args.length - 2];
        for(int i = 2; i < args.length; ++i) {
            final double t = Double.parseDouble( args[i] );
            if(t < 0.0 || t > 1.0) {
                System.err.println(
                    "GruRealCorpusTally: threshold must be within [0.0, 1.0], got " + t
                );
                System.exit(2);
                return;
            } // if
            thresholds[i - 2] = t;
        } // for

        final GruClassifier classifier = GruClassifier.load(weightsPath);

              int                total        = 0, ruleDecided = 0, ruleAbstain = 0;
        final Map<Double, int[]> perThreshold = new LinkedHashMap<>();               // [YES, NO, ABSTAIN]
        for(double t : thresholds) perThreshold.put( t, new int[3] );

        try ( BufferedReader reader = new BufferedReader(
            new InputStreamReader( Files.newInputStream(corpusPath), StandardCharsets.UTF_8 )
        ) ) {
            String line;
            while( ( line = reader.readLine() ) != null ) {
                if( line.isEmpty() ) continue;
                final int tab = line.indexOf('\t');
                if(tab < 0) continue;
                final String langName = line.substring(0, tab);
                final String text     = unescape( line.substring(tab + 1) );
                final Lang   lang;
                try { lang = new Lang(langName); }
catch(IllegalArgumentException e) { continue; }

                ++total;
                final CommentFeatureVector features   = CommentFeatureExtractor.extract(
                    text, lang, TokenType.COMMENT_LINE, 0
                );
                final CommentDecision      ruleResult = CommentClassifier.classify(features);
                if(ruleResult != CommentDecision.ABSTAIN) {
                    ++ruleDecided;
                    continue;
                }
                ++ruleAbstain;

                final double[] probs = classifier.probabilities(text, 0);
                for(double t : thresholds) {
                    final CommentDecision verdict = probs == null ? CommentDecision.ABSTAIN : GruClassifier.decide(
                        probs, t
                    );
                    final int[]           counts  = perThreshold.get(t);
                    switch(verdict) {
                        case YES : counts[0]++; break;
                        case NO  : counts[1]++; break;
                        default  : counts[2]++; break;
                    } // switch
                } // for
            } // while
        }

        System.out.println(
            "GruRealCorpusTally: " + total + " comment(s) read, " + ruleDecided
                + " rule-decided (skipped), " + ruleAbstain + " rule-ABSTAIN'd (GRU-eligible)"
        );
        for(double t : thresholds) {
            final int[] c   = perThreshold.get(t);
            final int   sum = c[0] + c[1] + c[2];
            System.out.printf(
                "threshold=%.2f  GRU-YES=%d GRU-NO=%d GRU-ABSTAIN=%d  gru-decide-rate=%.1f%%%n",
                t, c[0], c[1], c[2], sum == 0 ? 0.0 : 100.0 * ( c[0] + c[1] ) / sum
            );
        } // for
    }

    private static String unescape(final String escaped)
    {
        final StringBuilder sb = new StringBuilder( escaped.length() );
        for( int i = 0; i < escaped.length(); ++i ) {
            final char c = escaped.charAt(i);
            if( c == '\\' && i + 1 < escaped.length() ) {
                final char next = escaped.charAt(i + 1);
                if(next == 'n') {
                    sb.append('\n');
                    ++i;
                    continue;
                }
                if(next == 't') {
                    sb.append('\t');
                    ++i;
                    continue;
                }
                if(next == '\\') {
                    sb.append('\\');
                    ++i;
                    continue;
                }
            } // if
            sb.append(c);
        } // for
        return sb.toString();
    }

} // class GruRealCorpusTally
