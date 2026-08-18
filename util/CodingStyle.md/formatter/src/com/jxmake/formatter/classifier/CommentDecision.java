/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.classifier;

/**
 * Output of {@link CommentClassifier}. Per STATE_COMMENT_GRAMMAR.md's hard architectural
 *  constraint, the classifier only decides -- it never mutates comment text. {@link #ABSTAIN}
 *  must be treated by the caller exactly as if the relevant {@code normalize-comment-*} config
 *  key were {@code off} for that one comment.
 */
public enum CommentDecision {

    YES, NO, ABSTAIN;

} // enum CommentDecision
