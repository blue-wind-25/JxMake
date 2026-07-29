/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.classifier.gru;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loader/schema for the external, trained weights file consumed by {@link GruClassifier}.
 *  Per STATE_NEXT_AI.md's "GRU implementation design", the trainer (a separate, non-shipped
 *  {@code tools/gru} entry point) writes this file; {@code GruClassifier} never bakes weight
 *  arrays into source the way {@link com.jxmake.formatter.classifier.CommentClassifierWeights}
 *  does, since a neural net's weight count isn't hand-editable the same way and retraining
 *  shouldn't require a JAR rebuild. JSON is used (not a flat binary tensor dump) so a trained
 *  file stays diffable/inspectable in v1 (RDD_EXT_14).
 */
public final class GruWeights {

    /**
     * Current schema version this loader understands. Bump alongside any incompatible change
     *  to this class's field layout.
     */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public final int schemaVersion;

    // Vocabulary / embedding.
    public final int vocabSize;
    public final int hashBuckets;
    public final int embeddingDim;

    // Bidirectional GRU
    public final int hiddenSize;
    public final int sequenceCap;

    // Classification head
    public final int numClasses;

    /**
     * Softmax confidence cutoff below which the classifier abstains (RDD_EXT_11). Lives here,
     *  not hardcoded, so a retrain can ship a new threshold alongside new weights in one file.
     */
    public final double abstainThreshold;

    // The fields below represent the actual trained numbers (embedding table, per-direction GRU
    // gate matrices/biases, dense head, output layer). They are all nullable together: a weights
    // file written before this schema extension (or any file that only carries the flat scalar
    // architecture constants above) simply omits them, and GruClassifier treats that the same as
    // "no trained weights yet" -- ABSTAIN, same fail-safe posture as a missing/unreadable file.
    // See hasTrainedWeights().

    /**
     * Explicit vocab words, in embedding-row order; row {@code explicitVocab.length + hashBucket}
     *  is an OOV row. Null iff no trained weights are present.
     */
    public final String[] explicitVocab;

    /**
     * Embedding table: {@code embeddings.length == explicitVocab.length + hashBuckets} rows,
     *  each {@code embeddingDim} wide. Null iff no trained weights are present.
     */
    public final double[][] embeddings;

    /** Forward-direction GRU gate weights. Null iff no trained weights are present. */
    public final DirectionWeights forward;

    /** Backward-direction GRU gate weights. Null iff no trained weights are present. */
    public final DirectionWeights backward;

    /**
     * Dense head: {@code denseW} is 64 x (2*hiddenSize), {@code denseB} is length 64. Null iff no
     *  trained weights are present.
     */
    public final double[][] denseW;
    public final double[]   denseB;

    /**
     * Output layer: {@code outW} is numClasses x 64, {@code outB} is length numClasses. Null iff
     *  no trained weights are present.
     */
    public final double[][] outW;
    public final double[]   outB;

    /**
     * One GRU direction's gate weights: update ({@code z}), reset ({@code r}), and candidate
     *  ({@code h}) gates. {@code Wz}/{@code Wr}/{@code Wh} are hiddenSize x embeddingDim (applied
     *  to the input); {@code Uz}/{@code Ur}/{@code Uh} are hiddenSize x hiddenSize (applied to the
     *  previous hidden state); {@code bz}/{@code br}/{@code bh} are length hiddenSize. Plain data
     *  holder, mutable, shared by both the trained-weights side (here) and gradient accumulation
     *  during training (same shape, see {@code tools/gru/GruTrainer.java}).
     */
    public static final class DirectionWeights {

        public double[][] Wz, Wr, Wh;
        public double[][] Uz, Ur, Uh;
        public double[] bz, br,   bh;

        public DirectionWeights()
        {
        }

        public DirectionWeights(
            double[][] Wz,
            double[][] Wr,
            double[][] Wh,
            double[][] Uz,
            double[][] Ur,
            double[][] Uh,
            double[]   bz,
            double[]   br,
            double[]   bh
        )
        {
            this.Wz = Wz;
            this.Wr = Wr;
            this.Wh = Wh;
            this.Uz = Uz;
            this.Ur = Ur;
            this.Uh = Uh;
            this.bz = bz;
            this.br = br;
            this.bh = bh;
        }

        /**
         * Allocates a zero-filled instance of the given shape -- used both as an initial-weights
         *  starting point (before random init) and as a gradient accumulator during training
         */
        public static DirectionWeights zeros(int hiddenSize, int embeddingDim)
        {
            return new DirectionWeights(
                new double[hiddenSize][embeddingDim], new double[hiddenSize][embeddingDim], new double[hiddenSize][embeddingDim],
                new double[hiddenSize][hiddenSize], new double[hiddenSize][hiddenSize], new double[hiddenSize][hiddenSize],
                new double[hiddenSize], new double[hiddenSize], new double[hiddenSize]
            );
        }

    } // class DirectionWeights

    GruWeights(
        int              schemaVersion,
        int              vocabSize,
        int              hashBuckets,
        int              embeddingDim,
        int              hiddenSize,
        int              sequenceCap,
        int              numClasses,
        double           abstainThreshold,
        String[]         explicitVocab,
        double[][]       embeddings,
        DirectionWeights forward,
        DirectionWeights backward,
        double[][]       denseW,
        double[]         denseB,
        double[][]       outW,
        double[]         outB
    )
    {
        this.schemaVersion    = schemaVersion;
        this.vocabSize        = vocabSize;
        this.hashBuckets      = hashBuckets;
        this.embeddingDim     = embeddingDim;
        this.hiddenSize       = hiddenSize;
        this.sequenceCap      = sequenceCap;
        this.numClasses       = numClasses;
        this.abstainThreshold = abstainThreshold;
        this.explicitVocab    = explicitVocab;
        this.embeddings       = embeddings;
        this.forward          = forward;
        this.backward         = backward;
        this.denseW           = denseW;
        this.denseB           = denseB;
        this.outW             = outW;
        this.outB             = outB;
    }

    /**
     * True iff this file carries real trained numbers (embedding table, both GRU directions,
     *  dense head, output layer, and the explicit vocab word list all present together) rather
     *  than just the flat architecture-constant scalars. {@link GruClassifier#classify} checks
     *  this and abstains when false, per the project's fail-safe posture (missing/unusable signal
     *  -> ABSTAIN, never blocks formatting). Partial files (some but not all arrays present) are
     *  treated as untrained too -- a real trainer run always writes the full bundle at once.
     */
    public boolean hasTrainedWeights()
    {
        return explicitVocab != null && embeddings != null && forward != null && backward != null
                && denseW != null && denseB != null && outW != null && outB != null;
    }

    /**
     * Loads and validates a weights file from {@code path}. Throws a clear, explicit error
     *  naming the expected vs. found schema version on any mismatch, rather than attempting to
     *  parse a shape the loader wasn't written for -- per RDD_EXT_14, this is a hard error, not
     *  a silent-misparse risk.
     *
     *  <p>The flat scalar fields (hand-rolled regex extraction, no external JSON library -- the
     *  project has none) are always required. The trained-weight arrays (embedding table, GRU
     *  gate matrices, dense head) are parsed via a small recursive-descent value parser (also
     *  hand-rolled, no external library) when present, and left null when absent -- see
     *  {@link #hasTrainedWeights()}.
     */
    public static GruWeights load(Path path) throws IOException
    {
        if( !Files.isReadable(
            path
        ) ) throw new IOException(
            "GRU weights file not readable: " + path
        );
        String json = new String( Files.readAllBytes(path), StandardCharsets.UTF_8 );

        int schemaVersion = requireIntField(json, "schemaVersion", path);
        if(schemaVersion != CURRENT_SCHEMA_VERSION) throw new IOException(
            "GRU weights file " + path + " has schemaVersion " + schemaVersion + ", expected " + CURRENT_SCHEMA_VERSION
        );

        int    vocabSize        = requirePositiveIntField(json, "vocabSize", path);
        int    hashBuckets      = requirePositiveIntField(json, "hashBuckets", path);
        int    embeddingDim     = requirePositiveIntField(json, "embeddingDim", path);
        int    hiddenSize       = requirePositiveIntField(json, "hiddenSize", path);
        int    sequenceCap      = requirePositiveIntField(json, "sequenceCap", path);
        int    numClasses       = requirePositiveIntField(json, "numClasses", path);
        double abstainThreshold = requireDoubleField(json, "abstainThreshold", path);
        if(abstainThreshold < 0.0 || abstainThreshold > 1.0) throw new IOException(
            "GRU weights file " + path + " field \"abstainThreshold\" must " + "be within [0.0, 1.0], got " + abstainThreshold
        );

        String[]         explicitVocab = optionalStringArray(json, "explicitVocab");
        double[][]       embeddings    = optionalDoubleArray2D(json, "embeddings");
        DirectionWeights forward       = optionalDirectionWeights(json, "forward");
        DirectionWeights backward      = optionalDirectionWeights(json, "backward");
        double[][]       denseW        = optionalDoubleArray2D(json, "denseW");
        double[]         denseB        = optionalDoubleArray1D(json, "denseB");
        double[][]       outW          = optionalDoubleArray2D(json, "outW");
        double[]         outB          = optionalDoubleArray1D(json, "outB");

        return new GruWeights(
            schemaVersion, vocabSize, hashBuckets, embeddingDim, hiddenSize,
            sequenceCap, numClasses, abstainThreshold, explicitVocab, embeddings, forward,
            backward, denseW, denseB, outW, outB
        );
    }

    private static DirectionWeights optionalDirectionWeights(
        String json, String prefix
    ) throws IOException
    {
        double[][] Wz = optionalDoubleArray2D(json, prefix + "Wz");
        double[][] Wr = optionalDoubleArray2D(json, prefix + "Wr");
        double[][] Wh = optionalDoubleArray2D(json, prefix + "Wh");
        double[][] Uz = optionalDoubleArray2D(json, prefix + "Uz");
        double[][] Ur = optionalDoubleArray2D(json, prefix + "Ur");
        double[][] Uh = optionalDoubleArray2D(json, prefix + "Uh");
        double[]   bz = optionalDoubleArray1D(json, prefix + "bz");
        double[]   br = optionalDoubleArray1D(json, prefix + "br");
        double[]   bh = optionalDoubleArray1D(json, prefix + "bh");
        if(Wz == null && Wr == null && Wh == null && Uz == null && Ur == null && Uh == null && bz == null && br == null && bh == null) return null;

        return new DirectionWeights(Wz, Wr, Wh, Uz, Ur, Uh, bz, br, bh);
    }

    // ── Flat scalar field parsing (existing hand-rolled regex approach) ─────────────────────────

    private static final Pattern NUMBER_FIELD = Pattern.compile(
        "\"(\\w+)\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)"
    );

    private static String findFieldValue(String json, String key, Path path) throws IOException
    {
        Matcher m = NUMBER_FIELD.matcher(json);
        while( m.find() ) {
            if( m.group(1).equals(key) ) return m.group(2);
        }
        throw new IOException(
            "GRU weights file " + path + " is missing required field \"" + key + "\""
        );
    }

    private static int requireIntField(String json, String key, Path path) throws IOException
    {
        String value = findFieldValue(json, key, path);
        try {
            return Integer.parseInt(value);
        }
        catch(NumberFormatException e) {
            throw new IOException("GRU weights file " + path + " field \"" + key
                    + "\" is not an integer: " + value);
        }
    }

    private static int requirePositiveIntField(
        String json, String key, Path path
    ) throws IOException
    {
        int value = requireIntField(json, key, path);
        if(value <= 0) throw new IOException(
            "GRU weights file " + path + " field \"" + key + "\" must be positive, got " + value
        );

        return value;
    }

    private static double requireDoubleField(String json, String key, Path path) throws IOException
    {
        String value = findFieldValue(json, key, path);
        try {
            return Double.parseDouble(value);
        }
        catch(NumberFormatException e) {
            throw new IOException("GRU weights file " + path + " field \"" + key
                    + "\" is not a number: " + value);
        }
    }

    // ── Nested-array field parsing (embedding table, GRU matrices, dense/output layers) ─────────
    //
    // A small recursive-descent value parser, only as capable as this schema needs: numbers,
    // double-quoted strings (with the handful of escapes extract_comments.py-style tools emit),
    // and arrays of either (possibly nested, for 2D matrices). No object-value support beyond the
    // top level, since no field here needs it. Kept separate from the flat-scalar regex approach
    // above because that approach can't express nesting; this one isn't used for the flat fields
    // because they're required-and-simple enough that the original regex approach is clearer.

    private static final class Cursor {

        final String s;
              int    pos;

        Cursor(String s, int pos)
        {
            this.s   = s;
            this.pos = pos;
        }

    } // class Cursor

    private static Object findOptionalValue(String json, String key)
    {
        Matcher m = Pattern.compile( "\"" + Pattern.quote(key) + "\"\\s*:\\s*" ).matcher(json);
        if( !m.find() ) return null;
        Cursor c = new Cursor( json, m.end() );

        return parseValue(c);
    }

    private static Object parseValue(Cursor c)
    {
        skipWs(c);
        char ch = c.s.charAt(c.pos);
        if(ch == '[') return parseArray(c);
        if(ch == '"') return parseString(c);

        return parseNumber(c);
    }

    private static List<Object> parseArray(Cursor c)
    {
        c.pos++; // Consume '['
        List<Object> list = new ArrayList<>();
        skipWs(c);
        if( c.s.charAt(c.pos) == ']' ) {
            c.pos++;
            return list;
        }
        while(true) {
            list.add( parseValue(c) );
            skipWs(c);
            char ch = c.s.charAt(c.pos);
            if(ch == ',') {
                c.pos++;
                skipWs(c);
            }
            else if(ch == ']') {
                c.pos++;
                break;
            }
        } // while

        return list;
    }

    private static String parseString(Cursor c)
    {
        c.pos++; // Consume opening quote
        StringBuilder sb = new StringBuilder();
        while( c.s.charAt(c.pos) != '"' ) {
            char ch = c.s.charAt(c.pos);
            if(ch == '\\') {
                c.pos++;
                char esc = c.s.charAt(c.pos);
                switch(esc) {
                    case 'n'  : sb.append('\n'); break;
                    case 't'  : sb.append('\t'); break;
                    case '"'  : sb.append('"') ; break;
                    case '\\' : sb.append('\\'); break;
                    default   : sb.append(esc) ;
                } // switch
            } // if
            else {
                sb.append(ch);
            }
            c.pos++;
        } // while
        c.pos++; // Consume closing quote
        return sb.toString();
    }

    private static Double parseNumber(Cursor c)
    {
        int start = c.pos;
        while( c.pos < c.s.length() && ",]} \t\n\r".indexOf( c.s.charAt(c.pos) ) < 0 ) c.pos++;

        return Double.parseDouble( c.s.substring(start, c.pos) );
    }

    private static void skipWs(Cursor c)
    {
        while( Character.isWhitespace( c.s.charAt(c.pos) ) ) c.pos++;
    }

    @SuppressWarnings("unchecked")
    private static double[] optionalDoubleArray1D(String json, String key)
    {
        Object value = findOptionalValue(json, key);
        if(value == null) return null;
        List<Object> list   = (List<Object>) value;
        double[]     result = new double[ list.size() ];
        for( int i = 0; i < list.size(); ++i ) result[i] = (Double) list.get(i);

        return result;
    }

    @SuppressWarnings("unchecked")
    private static double[][] optionalDoubleArray2D(String json, String key)
    {
        Object value = findOptionalValue(json, key);
        if(value == null) return null;
        List<Object> rows   = (List<Object>) value;
        double[][]   result = new double[ rows.size() ][];
        for( int i = 0; i < rows.size(); ++i ) {
            List<Object> row = (List<Object>) rows.get(i);
            double[]     r   = new double[ row.size() ];
            for( int j = 0; j < row.size(); ++j ) r[j] = (Double) row.get(j);
            result[i] = r;
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static String[] optionalStringArray(String json, String key)
    {
        Object value = findOptionalValue(json, key);
        if(value == null) return null;
        List<Object> list   = (List<Object>) value;
        String[]     result = new String[ list.size() ];
        for( int i = 0; i < list.size(); ++i ) result[i] = (String) list.get(i);

        return result;
    }

} // class GruWeights
