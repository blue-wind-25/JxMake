/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.classifier.CommentClassifier;
import com.jxmake.formatter.classifier.CommentDecision;
import com.jxmake.formatter.classifier.CommentFeatureExtractor;
import com.jxmake.formatter.classifier.CommentFeatureVector;

/**
 * Measures the real rule-based {@link CommentClassifier}'s ABSTAIN rate over a real comment
 *  corpus -- STATE_AI.md's Step 3 open item 9, the sole genuinely blocking item before any GRU
 *  training-data acquisition can start. Deliberately lives outside {@code src/}, same as
 *  {@code GruTrainer.java} -- this is a one-off measurement tool, not runtime or shipped code.
 *
 *  <p>Reads records written by {@code tools/gru/extract_comments.py}: one comment per line, format
 *  {@code "<lang>\t<escaped comment text>"}, where the comment text has its own literal newlines/
 *  tabs escaped as {@code \n}/{@code \t}. Feeds each one through the real
 *  {@link CommentFeatureExtractor#extract(String, Lang)} / {@link CommentClassifier#classify}
 *  pipeline -- the same pipeline production code uses -- and tallies {@link CommentDecision}
 *  counts overall and per language. Does not attempt to reimplement any classifier logic itself.
 */
public final class CommentAbstainTally {

    private CommentAbstainTally()
    {
    }

    private static final String USAGE = "Usage: CommentAbstainTally <extracted-comments-path>";

    public static void main(String[] args) throws IOException
    {
        if(args.length != 1) {
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        final java.io.File input = new java.io.File( args[0] );
        if( !input.isFile() || !input.canRead() ) {
            System.err.println( "CommentAbstainTally: input file not readable: " + args[0] );
            System.exit(2);
            return;
        }

        final Map<String, Lang>  langCache     = new LinkedHashMap<String, Lang>();
        final Map<String, int[]> perLangCounts = new LinkedHashMap<String, int[]>(); // [YES, NO, ABSTAIN]
              int                total         = 0;
              int                malformed     = 0;

        try ( BufferedReader reader = new BufferedReader(
            new InputStreamReader( Files.newInputStream( input.toPath() ), StandardCharsets.UTF_8 )
        ) ) {
            String line;
            while( ( line = reader.readLine() ) != null ) {
                if( line.isEmpty() ) continue;
                final int tab = line.indexOf('\t');
                if(tab < 0) {
                    ++malformed;
                    continue;
                }
                final String langName    = line.substring(0, tab);
                final String commentText = unescape( line.substring(tab + 1) );

                final Lang                 lang     = langCache.computeIfAbsent(
                    langName, Lang::new
                );
                final CommentFeatureVector features = CommentFeatureExtractor.extract(
                    commentText, lang
                );
                final CommentDecision      decision = CommentClassifier.classify(features);

                final int[] counts = perLangCounts.computeIfAbsent( langName, k->new int[3] );
                counts[ indexOf(decision) ]++;
                ++total;
            } // while
        }

        System.out.println("CommentAbstainTally: " + total + " comment(s) classified ("
                + malformed + " malformed line(s) skipped)");
        System.out.println();
        System.out.printf("%-10s %8s %8s %8s %10s%n", "lang", "YES", "NO", "ABSTAIN", "abstain%");
        final int[] grandTotal = new int[3];
        for( final Map.Entry<String, int[]> entry : perLangCounts.entrySet() ) {
            final int[] counts    = entry.getValue();
            final int   langTotal = counts[0] + counts[1] + counts[2];
            System.out.printf(
                "%-10s %8d %8d %8d %9.1f%%%n", entry.getKey(), counts[0], counts[1], counts[2],
                langTotal == 0 ? 0.0 : 100.0 * counts[2] / langTotal
            );
            grandTotal[0] += counts[0];
            grandTotal[1] += counts[1];
            grandTotal[2] += counts[2];
        } // for
        final int grandSum = grandTotal[0] + grandTotal[1] + grandTotal[2];
        System.out.printf(
            "%-10s %8d %8d %8d %9.1f%%%n", "TOTAL", grandTotal[0], grandTotal[1], grandTotal[2],
            grandSum == 0 ? 0.0 : 100.0 * grandTotal[2] / grandSum
        );
    }

    private static int indexOf(final CommentDecision decision)
    {
        switch(decision) {
            case YES : return 0;
            case NO  : return 1;
            default  : return 2;
        }
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

} // class CommentAbstainTally
