/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.classifier.gru;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Word-to-embedding-index lookup for {@link GruClassifier}, per STATE_NEXT_AI.md's "GRU
 *  implementation design": an explicit vocab (~3.5k words -- every keyword across every
 *  supported/planned language gets a guaranteed slot, plus common English comment-corpus words
 *  filling the rest) with a hashed-bucket fallback for anything out of vocab (RDD_EXT_13's
 *  FNV-1a mod {@link GruClassifier#HASH_BUCKETS}, via {@link GruClassifier#hashBucket}).
 *
 *  <p>This class is only the lookup mechanism -- the actual ~3.5k-word explicit vocab content is
 *  training-data curation work (which keywords, which common words, how they're ordered), not an
 *  architectural decision, so it isn't hardcoded here. The explicit word list is a constructor
 *  argument, expected to eventually come from the trained weights file/training pipeline. Case is
 *  preserved (not lowercased) per the finalized architecture -- case is a real signal (e.g.
 *  {@code Return} reads less like a keyword than {@code return}), so two differently-cased forms
 *  of the same word are deliberately distinct vocab entries. */
public final class Vocabulary {

    private final Map<String, Integer> explicitIndex;
    private final int explicitVocabSize;

    /** Builds a vocabulary from an explicit, ordered word list -- each word's position is its
     *  embedding index (0-based, case-preserved, no deduplication check beyond what the caller
     *  provides). */
    public Vocabulary(List<String> explicitWords) {
        this.explicitVocabSize = explicitWords.size();
        Map<String, Integer> index = new HashMap<>(explicitWords.size() * 2);
        for (int i = 0; i < explicitWords.size(); i++) {
            index.putIfAbsent(explicitWords.get(i), i);
        }
        this.explicitIndex = Collections.unmodifiableMap(index);
    }

    /** Number of explicit vocab slots (index range {@code [0, explicitVocabSize())}). */
    public int explicitVocabSize() {
        return explicitVocabSize;
    }

    /** Resolves {@code word} to an embedding-table row index: an explicit vocab index if present
     *  (case-sensitive match), otherwise {@code explicitVocabSize + hashBucket(word)} -- so the
     *  full embedding table has {@code explicitVocabSize + HASH_BUCKETS} rows, explicit slots
     *  first, OOV hash buckets after. */
    public int lookup(String word) {
        Integer explicit = explicitIndex.get(word);
        if (explicit != null) {
            return explicit;
        }
        return explicitVocabSize + GruClassifier.hashBucket(word);
    }

    /** Total embedding table row count this vocabulary implies: explicit slots plus OOV hash
     *  buckets. */
    public int totalEmbeddingRows() {
        return explicitVocabSize + GruClassifier.HASH_BUCKETS;
    }
}
