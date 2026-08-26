/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.classifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.jxmake.formatter.Lang;

/**
 * RDD_KEY_96: per-language keyword lists (no shared list across C/C++/Java/Kotlin) + two-stage
 * check -- a cheap membership test first, then contextual scoring only on actual keyword
 * matches. Keeps the common case (no keyword present) O(1)-ish and avoids scoring cost on
 * comments that can't be ambiguous in the first place.
 */
public final class KeywordAmbiguityGate {

    // Mirrors MiscRuleCore's COMMENT_NO_CAPITALIZE_* precedent, but kept as this package's own copy
    // per RDD_KEY_96 -- no shared list across languages, and this gate's purpose (feeding the
    // classifier) is independent of MiscRuleCore's deterministic capitalization skip
    private static final Set<String> KEYWORDS_C = setOf(
        "auto",
        "break",
        "case",
        "char",
        "const",
        "continue",
        "default",
        "do",
        "double",
        "else",
        "enum",
        "extern",
        "float",
        "for",
        "goto",
        "if",
        "inline",
        "int",
        "long",
        "register",
        "restrict",
        "return",
        "short",
        "signed",
        "sizeof",
        "static",
        "struct",
        "switch",
        "typedef",
        "union",
        "unsigned",
        "void",
        "volatile",
        "while"
    );

    private static final Set<String> KEYWORDS_CPP = setOf(
        "alignas",
        "alignof",
        "asm",
        "bool",
        "catch",
        "char16_t",
        "char32_t",
        "class",
        "co_await",
        "co_return",
        "co_yield",
        "concept",
        "consteval",
        "constexpr",
        "constinit",
        "const_cast",
        "decltype",
        "delete",
        "dynamic_cast",
        "explicit",
        "export",
        "false",
        "final",
        "friend",
        "mutable",
        "namespace",
        "new",
        "noexcept",
        "nullptr",
        "operator",
        "override",
        "private",
        "protected",
        "public",
        "reinterpret_cast",
        "requires",
        "static_assert",
        "static_cast",
        "template",
        "this",
        "thread_local",
        "throw",
        "true",
        "try",
        "typeid",
        "typename",
        "using",
        "virtual",
        "wchar_t"
    );

    private static final Set<String> KEYWORDS_JAVA = setOf(
        "abstract",
        "assert",
        "boolean",
        "break",
        "byte",
        "case",
        "catch",
        "char",
        "class",
        "const",
        "continue",
        "default",
        "do",
        "else",
        "enum",
        "extends",
        "final",
        "finally",
        "for",
        "goto",
        "if",
        "implements",
        "import",
        "instanceof",
        "interface",
        "native",
        "new",
        "package",
        "permits",
        "private",
        "protected",
        "public",
        "record",
        "return",
        "sealed",
        "static",
        "strictfp",
        "super",
        "switch",
        "synchronized",
        "this",
        "throw",
        "throws",
        "transient",
        "try",
        "var",
        "void",
        "volatile",
        "while",
        "yield",
        "null",
        "true",
        "false"
    );

    private static final Set<String> KEYWORDS_KOTLIN = setOf(
        "as",
        "break",
        "class",
        "continue",
        "do",
        "else",
        "false",
        "for",
        "fun",
        "if",
        "in",
        "interface",
        "is",
        "null",
        "object",
        "package",
        "return",
        "super",
        "this",
        "throw",
        "true",
        "try",
        "typealias",
        "typeof",
        "val",
        "var",
        "when",
        "while"
    );

    // RDD_KEY_96 precedent extended (STATE_AI.md 2026-07-31 "extend classifier_weights"
    // session): js/ts route through the same curly-brace `MiscRuleCurly.enforceCommentStyle`
    // call path as c/cpp/java/kotlin (see Lang.isCurly), so they need their own real keyword
    // sets too -- previously silently fell through to KEYWORDS_C, which is wrong (JS/TS share
    // almost no keywords with C). json/json5/css/yaml/toml/xml/html5 never reach this gate at
    // all (their comment normalization has no classifier/GRU dependency at all -- see
    // ToolingCommentNormalizer's own doc comment); python3 was in that same "never reaches"
    // category until MiscRuleIndent wired `#`-comment normalization through the same
    // classifyComment path 2026-08-08 (STATE_PYTHON3.md), which silently fell through to this
    // same wrong KEYWORDS_C default until the KEYWORDS_PYTHON branch below was added -- see
    // STATE_AI.md for the full investigation.
    private static final Set<String> KEYWORDS_JS = setOf(
        "async",
        "await",
        "break",
        "case",
        "catch",
        "class",
        "const",
        "continue",
        "debugger",
        "default",
        "delete",
        "do",
        "else",
        "export",
        "extends",
        "false",
        "finally",
        "for",
        "function",
        "if",
        "import",
        "in",
        "instanceof",
        "let",
        "new",
        "null",
        "return",
        "static",
        "super",
        "switch",
        "this",
        "throw",
        "true",
        "try",
        "typeof",
        "var",
        "void",
        "while",
        "with",
        "yield"
    );

    // TS-only additions layered on top of KEYWORDS_JS, mirroring the isCpp branch's
    // KEYWORDS_C+KEYWORDS_CPP additive pattern below
    private static final Set<String> KEYWORDS_TS = setOf(
        "abstract",
        "any",
        "as",
        "boolean",
        "declare",
        "enum",
        "implements",
        "interface",
        "is",
        "keyof",
        "namespace",
        "never",
        "number",
        "private",
        "protected",
        "public",
        "readonly",
        "string",
        "type",
        "unknown"
    );

    // Full CPython keyword.kwlist (Python 3), plus the two soft keywords `match`/`case` used by
    // structural pattern matching -- included since they're the exact kind of "reads like an
    // ordinary English word" false-friend this gate exists for
    private static final Set<String> KEYWORDS_PYTHON = setOf(
        "False",
        "None",
        "True",
        "and",
        "as",
        "assert",
        "async",
        "await",
        "break",
        "case",
        "class",
        "continue",
        "def",
        "del",
        "elif",
        "else",
        "except",
        "finally",
        "for",
        "from",
        "global",
        "if",
        "import",
        "in",
        "is",
        "lambda",
        "match",
        "nonlocal",
        "not",
        "or",
        "pass",
        "raise",
        "return",
        "try",
        "while",
        "with",
        "yield"
    );

    private KeywordAmbiguityGate()
    {
    }

    private static Set<String> setOf(final String... words)
    {
        return new HashSet<>( Arrays.asList(words) );
    }

    /**
     * Takes the already-extracted leading word directly, rather than re-scanning
     * {@code commentText} -- callers such as {@link CommentFeatureExtractor} already have this
     * word from their own leading-word extraction and can pass it straight through
     */
    public static boolean hasLeadingKeywordMatch(final String leadingWord, final Lang lang)
    {
        if( leadingWord.isEmpty() ) return false;
        if(lang.isJava) return KEYWORDS_JAVA.contains(leadingWord);
        if(lang.isCpp) return KEYWORDS_C.contains(
            leadingWord
        ) || KEYWORDS_CPP.contains(
            leadingWord
        );
        if(lang.isKotlin) return KEYWORDS_KOTLIN.contains(leadingWord);
        if(lang.isTs) return KEYWORDS_JS.contains(
            leadingWord
        ) || KEYWORDS_TS.contains(
            leadingWord
        );
        if(lang.isJs) return KEYWORDS_JS.contains(leadingWord);
        if(lang.isPython3) return KEYWORDS_PYTHON.contains(leadingWord);

        return KEYWORDS_C.contains(leadingWord);
    }

    /**
     * Stage 2 -- contextual scoring, only invoked when stage 1 ({@link #hasLeadingKeywordMatch})
     * returns true. Resolves the ambiguity a bare keyword-membership test can't (e.g. "static"
     * as an English adjective vs. the language keyword). Weights and derivation: see
     * {@code tools/classifier_weights/weights.md}. Returns {@code true} only when the comment reads as ordinary
     * prose (safe to normalize); any of {@code nextCharIsOpenParen}, {@code nextTokenIsArrow},
     * {@code containsSemicolon}, or {@code containsUrlOrFilenameOrNumber} is enough on its own
     * to push the score below threshold, per that file's "asymmetric risk" rationale -- a false
     * skip is zero-cost, a false positive is a visible bug.
     */
    public static boolean resolveAmbiguousKeyword(final CommentFeatureVector features)
    {
        double score = CommentClassifierWeights.KEYWORD_BIAS;
        if(features.nextCharIsOpenParen) score += CommentClassifierWeights.KEYWORD_WEIGHT_PAREN;
        if(features.nextTokenIsArrow) score += CommentClassifierWeights.KEYWORD_WEIGHT_ARROW;
        if(features.containsSemicolon) score += CommentClassifierWeights.KEYWORD_WEIGHT_SEMICOLON;
        if(features.containsUrlOrFilenameOrNumber) score += CommentClassifierWeights.KEYWORD_WEIGHT_URL_OR_NUMBER;

        return score > CommentClassifierWeights.KEYWORD_THRESHOLD;
    }

} // class KeywordAmbiguityGate
