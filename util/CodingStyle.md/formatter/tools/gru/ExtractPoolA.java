/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
 * Extracts Pool A (keyword-ambiguity) candidates for Step 3's GRU training set --
 *  STATE_AI.md's "Training-set acquisition" section. Reads the same corpus format
 *  {@code tools/gru/extract_comments.py} writes and {@code CommentAbstainTally} reads
 *  ({@code "<lang>\t<escaped comment text>"}), runs each comment through the real
 *  {@link CommentFeatureExtractor#extract(String, Lang)} / {@link CommentClassifier#classify}
 *  pipeline (not a reimplementation), and writes out only the comments that ABSTAIN
 *  *because of* keyword ambiguity ({@link CommentFeatureVector#hasLeadingKeywordMatch}) --
 *  i.e. this is exactly the "Pool A" definition from STATE_AI.md.
 *
 *  <p>Deliberately excludes ABSTAINs where {@code hasNonLatinScript} is set instead: those are
 *  {@code NonLatinScriptGate} hits (non-English script comments, or non-comment vendored data
 *  like bitmap-font glyph tables -- see item 9's TTGO_VGA32_Lite/RobotCoding investigation in
 *  STATE_AI.md), not keyword-ambiguity cases, and would pollute Pool A with off-topic examples.
 *
 *  <p>Output is the same {@code "<lang>\t<escaped text>"} format as the input, so it can be fed
 *  straight into the frontier-model labeling step described in STATE_AI.md without further
 *  reformatting. Lives outside {@code src/}, same as {@code GruTrainer.java}/
 *  {@code CommentAbstainTally.java} -- a one-off data-preparation tool, not runtime or shipped
 *  code.
 */
public final class ExtractPoolA {

    private ExtractPoolA()
    {
    }

    private static final String USAGE = "Usage: ExtractPoolA <extracted-comments-path> <output-path>";

    public static void main(final String[] args) throws IOException
    {
        if(args.length != 2) {
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        final java.io.File input = new java.io.File( args[0] );
        if( !input.isFile() || !input.canRead() ) {
            System.err.println( "ExtractPoolA: input file not readable: " + args[0] );
            System.exit(2);
            return;
        }

        final Map<String, Lang> langCache = new LinkedHashMap<String, Lang>();
              int               total     = 0;
              int               malformed = 0;
              int               kept      = 0;

        try ( BufferedReader reader = new BufferedReader(
            new InputStreamReader( Files.newInputStream( input.toPath() ), StandardCharsets.UTF_8 )
        );
             BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter( Files.newOutputStream( new java.io.File( args[1] ).toPath() ),
                        StandardCharsets.UTF_8 ) ) ) {
            String line;
            while( ( line = reader.readLine() ) != null ) {
                if( line.isEmpty() ) continue;
                final int tab = line.indexOf('\t');
                if(tab < 0) {
                    ++malformed;
                    continue;
                }
                ++total;
                final String langName    = line.substring(0, tab);
                final String escapedText = line.substring(tab + 1);
                final String commentText = unescape(escapedText);

                final Lang                 lang     = langCache.computeIfAbsent(
                    langName, Lang::new
                );
                final CommentFeatureVector features = CommentFeatureExtractor.extract(
                    commentText, lang
                );
                final CommentDecision      decision = CommentClassifier.classify(features);

                if(decision == CommentDecision.ABSTAIN && features.hasLeadingKeywordMatch) {
                    writer.write(langName);
                    writer.write('\t');
                    writer.write(escapedText);
                    writer.newLine();
                    ++kept;
                } // if
            } // while
        }

        System.out.println("ExtractPoolA: " + total + " comment(s) read (" + malformed
                + " malformed line(s) skipped), " + kept + " kept as Pool A (keyword-ambiguity) candidate(s)");
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

} // class ExtractPoolA
