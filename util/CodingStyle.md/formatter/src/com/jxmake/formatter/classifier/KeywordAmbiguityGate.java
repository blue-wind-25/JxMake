/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.classifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.jxmake.formatter.Lang;

/** RDD_KEY_96: per-language keyword lists (no shared list across C/C++/Java/Kotlin) + two-stage
 *  check -- a cheap membership test first, then contextual scoring only on actual keyword
 *  matches. Keeps the common case (no keyword present) O(1)-ish and avoids scoring cost on
 *  comments that can't be ambiguous in the first place. */
public final class KeywordAmbiguityGate {

    // Mirrors MiscRule's COMMENT_NO_CAPITALIZE_* precedent, but kept as this package's own copy
    // per RDD_KEY_96 -- no shared list across languages, and this gate's purpose (feeding the
    // classifier) is independent of MiscRule's deterministic capitalization skip.
    private static final Set<String> KEYWORDS_C = setOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
            "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long",
            "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct",
            "switch", "typedef", "union", "unsigned", "void", "volatile", "while");

    private static final Set<String> KEYWORDS_CPP = setOf(
            "alignas", "alignof", "asm", "bool", "catch", "char16_t", "char32_t", "class",
            "co_await", "co_return", "co_yield", "concept", "consteval", "constexpr", "constinit",
            "const_cast", "decltype", "delete", "dynamic_cast", "explicit", "export", "false",
            "final", "friend", "mutable", "namespace", "new", "noexcept", "nullptr", "operator",
            "override", "private", "protected", "public", "reinterpret_cast", "requires",
            "static_assert", "static_cast", "template", "this", "thread_local", "throw", "true",
            "try", "typeid", "typename", "using", "virtual", "wchar_t");

    private static final Set<String> KEYWORDS_JAVA = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "else", "enum", "extends", "final", "finally",
            "for", "goto", "if", "implements", "import", "instanceof", "interface", "native",
            "new", "package", "permits", "private", "protected", "public", "record", "return",
            "sealed", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
            "throws", "transient", "try", "var", "void", "volatile", "while", "yield", "null",
            "true", "false");

    private static final Set<String> KEYWORDS_KOTLIN = setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
            "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
            "true", "try", "typealias", "typeof", "val", "var", "when", "while");

    private KeywordAmbiguityGate() {
    }

    private static Set<String> setOf(final String... words) {
        return new HashSet<>(Arrays.asList(words));
    }

    public static boolean hasLeadingKeywordMatch(final String commentText, final Lang lang) {
        final String leadingWord = leadingWord(commentText);
        if (leadingWord.isEmpty()) {
            return false;
        }
        if (lang.isJava) {
            return KEYWORDS_JAVA.contains(leadingWord);
        }
        if (lang.isCpp) {
            return KEYWORDS_C.contains(leadingWord) || KEYWORDS_CPP.contains(leadingWord);
        }
        if (lang.isKotlin) {
            return KEYWORDS_KOTLIN.contains(leadingWord);
        }
        return KEYWORDS_C.contains(leadingWord);
    }

    /** First contiguous run of letters/digits/underscore after skipping leading whitespace, or
     *  "" if commentText has no such leading word. Mirrors
     *  MiscRule.capitalizeFirstLetter's leading-word extraction. */
    private static String leadingWord(final String commentText) {
        int i = 0;
        while (i < commentText.length() && Character.isWhitespace(commentText.charAt(i))) {
            i++;
        }
        int end = i;
        while (end < commentText.length()
                && (Character.isLetterOrDigit(commentText.charAt(end)) || commentText.charAt(end) == '_')) {
            end++;
        }
        return commentText.substring(i, end);
    }

    /** Stage 2 -- contextual scoring, only invoked when stage 1 ({@link #hasLeadingKeywordMatch})
     *  returns true. Resolves the ambiguity a bare keyword-membership test can't (e.g. "static"
     *  as an English adjective vs. the language keyword). Weights and derivation: see
     *  {@code cwg/weights.md}. Returns {@code true} only when the comment reads as ordinary
     *  prose (safe to normalize); any of {@code nextCharIsOpenParen}, {@code nextTokenIsArrow},
     *  {@code containsSemicolon}, or {@code containsUrlOrFilenameOrNumber} is enough on its own
     *  to push the score below threshold, per that file's "asymmetric risk" rationale -- a false
     *  skip is zero-cost, a false positive is a visible bug. */
    public static boolean resolveAmbiguousKeyword(final CommentFeatureVector features) {
        double score = CommentClassifierWeights.KEYWORD_BIAS;
        if (features.nextCharIsOpenParen) {
            score += CommentClassifierWeights.KEYWORD_WEIGHT_PAREN;
        }
        if (features.nextTokenIsArrow) {
            score += CommentClassifierWeights.KEYWORD_WEIGHT_ARROW;
        }
        if (features.containsSemicolon) {
            score += CommentClassifierWeights.KEYWORD_WEIGHT_SEMICOLON;
        }
        if (features.containsUrlOrFilenameOrNumber) {
            score += CommentClassifierWeights.KEYWORD_WEIGHT_URL_OR_NUMBER;
        }
        return score > CommentClassifierWeights.KEYWORD_THRESHOLD;
    }
}
