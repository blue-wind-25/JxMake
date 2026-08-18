/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.classifier.CommentClassifier;
import com.jxmake.formatter.classifier.CommentDecision;
import com.jxmake.formatter.classifier.CommentFeatureExtractor;
import com.jxmake.formatter.classifier.CommentFeatureVector;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

/**
 * Filters an {@code extract_comments.py} corpus file down to the lines the real rule-based
 *  {@link CommentClassifier} ABSTAINs on at {@code targetWordIndex=0} -- the leading-token
 *  (capitalize-decision) call site {@link com.jxmake.formatter.rules.MiscRuleCore#classifyComment}
 *  uses, and the only shape of line the GRU stage ever actually reaches in production. Complements
 *  {@link CommentAbstainTally}, which only tallies counts: this writes the ABSTAIN subset back out
 *  in the same {@code <lang>\t<escaped-comment-text>} format, so it can feed straight into
 *  {@link GruRealCorpusTally} (or manual hand-labeling) without wasting effort on lines the rules
 *  already resolve and the GRU never sees. Useful before running a held-out
 *  out-of-distribution GRU check against a real, unrelated codebase -- a raw
 *  {@code extract_comments.py} dump is mostly ordinary non-ambiguous prose/code that the rule-based
 *  classifier already decides on its own, so sampling from it directly under-exercises the GRU
 *  stage the same way a random sample from {@code sample_default.txt} would (see
 *  {@code STATE_AI.md}'s 2026-08-12 out-of-distribution investigation entry).
 */
public final class FilterAbstain {

    private FilterAbstain()
    {
    }

    private static final String USAGE = "Usage: FilterAbstain <extracted-comments-path> <out-path>";

    public static void main(String[] args) throws Exception
    {
        if(args.length != 2) {
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        int total = 0, kept = 0;
        try ( BufferedReader reader = new BufferedReader(
            new InputStreamReader( Files.newInputStream( Paths.get( args[0] ) ), StandardCharsets.UTF_8 )
        );
              PrintWriter out = new PrintWriter(
                  Files.newBufferedWriter( Paths.get( args[1] ), StandardCharsets.UTF_8 )
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
                final CommentFeatureVector features = CommentFeatureExtractor.extract(
                    text, lang, TokenType.COMMENT_LINE, 0
                );
                if( CommentClassifier.classify(features) != CommentDecision.ABSTAIN ) continue;
                out.println(line);
                ++kept;
            } // while
        }

        System.out.println(
            "FilterAbstain: " + total + " comment(s) read, " + kept + " rule-ABSTAIN'd (kept)"
        );
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

} // class FilterAbstain
