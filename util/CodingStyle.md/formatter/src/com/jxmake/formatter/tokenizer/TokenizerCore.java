/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.tokenizer;

import com.jxmake.formatter.Lang;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TokenizerCore {

    public enum TokenType {
        KEYWORD,
        IDENTIFIER,
        NUMBER,
        STRING,
        CHAR,
        OP,
        PUNCT,
        COMMENT_LINE,
        COMMENT_BLOCK,
        WHITESPACE,
        NEWLINE,
        PREPROCESSOR,        // C/C++ only — opaque single-line #-directive
        MACRO_DEF,           // C/C++ only — opaque multiline #define with \ continuations
        ANGLE_BRACKET_OPEN,  // generic/template context
        ANGLE_BRACKET_CLOSE  // generic/template context
    }

    public static final class Token {
        public final TokenType type;
        public String text;
        public final int braceDepth;
        public final int parenDepth;
        public final String name; // for `{`/`}` only: pushed/popped construct name, else null
        public boolean frozen; // set by markFrozenSpans; true = opaque pass-through, never transformed

        public Token(final TokenType type, final String text, final int braceDepth,
                final int parenDepth, final String name) {
            this.type = type;
            this.text = text;
            this.braceDepth = braceDepth;
            this.parenDepth = parenDepth;
            this.name = name;
        }

        /** Null-safe: {@code t} is a `PUNCT` token whose text equals {@code text}. Centralizes
         *  what used to be a byte-for-byte-identical private helper duplicated in nearly every
         *  rule class. */
        public static boolean isPunct(final Token t, final String text) {
            return t != null && t.type == TokenType.PUNCT && text.equals(t.text);
        }

        /** Null-safe: {@code t} is an `OP` token whose text equals {@code text}. */
        public static boolean isOp(final Token t, final String text) {
            return t != null && t.type == TokenType.OP && text.equals(t.text);
        }

        /** Null-safe: {@code t} is an `OP` token whose text equals repeated {@code ch}. */
        public static boolean isRepOp(final Token t, final char ch) {
            if (t == null || t.type != TokenType.OP || t.text.isEmpty()) {
                return false;
            }
            for (int i = 0; i < t.text.length(); i++) {
                if (t.text.charAt(i) != ch) {
                    return false;
                }
            }
            return true;
        }

        /** Null-safe: {@code t} is a `KEYWORD` token whose text equals {@code text}. */
        public static boolean isKeyword(final Token t, final String text) {
            return t != null && t.type == TokenType.KEYWORD && text.equals(t.text);
        }

        /** Null-safe: {@code t} is a line or block comment token. */
        public static boolean isComment(final Token t) {
            return t != null && (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK);
        }

        /** Null-safe: {@code t} is whitespace, a newline, or a comment -- a token every rule
         *  class's rendering passes skip over when scanning for the next significant token. */
        public static boolean isGapToken(final Token t) {
            return t != null && (t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                    || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK);
        }
    }

    private static final java.util.regex.Pattern FORMAT_DIS_MARKER = java.util.regex.Pattern.compile(
            "^//%\\s*JXM_CFMT_DIS\\s*$|^/\\*%\\s*JXM_CFMT_DIS\\s*\\*/$");
    private static final java.util.regex.Pattern FORMAT_ENA_MARKER = java.util.regex.Pattern.compile(
            "^//%\\s*JXM_CFMT_ENA\\s*$|^/\\*%\\s*JXM_CFMT_ENA\\s*\\*/$");

    /** Scans {@code tokens} in order, toggling a frozen/unfrozen state on
     *  {@code //% JXM_CFMT_DIS}/{@code ENA} (and block-comment equivalent) marker comments, and
     *  stamps {@link Token#frozen} on every token accordingly. A marker token itself is always
     *  stamped frozen (never reformatted/removed), regardless of whether it disables or
     *  re-enables. {@code startFrozen} seeds the initial state (set from {@code --format-off}). */
    public static void markFrozenSpans(final List<Token> tokens, final boolean startFrozen) {
        boolean frozen = startFrozen;
        for (final Token t : tokens) {
            if (Token.isComment(t)) {
                final String trimmed = t.text.trim();
                if (FORMAT_DIS_MARKER.matcher(trimmed).matches()) {
                    frozen = true;
                } else if (FORMAT_ENA_MARKER.matcher(trimmed).matches()) {
                    t.frozen = true;
                    frozen = false;
                    continue;
                }
            }
            t.frozen = frozen;
        }
    }

    private static Set<String> setOf(final String... words) {
        return new HashSet<>(Arrays.asList(words));
    }

    private static final Set<String> KEYWORDS_C = setOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
            "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long",
            "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct",
            "switch", "typedef", "union", "unsigned", "void", "volatile", "while");

    private static final Set<String> KEYWORDS_CPP = setOf(
            "alignas", "alignof", "asm", "auto", "bool", "break", "case", "catch", "char",
            "char16_t", "char32_t", "class", "co_await", "co_return", "co_yield", "concept",
            "const", "constexpr", "consteval", "constinit", "const_cast", "continue",
            "decltype", "default", "delete", "do", "double", "dynamic_cast", "else", "enum",
            "explicit", "export", "extern", "false", "final", "float", "for", "friend", "goto",
            "if", "inline", "int", "long", "mutable", "namespace", "new", "noexcept", "nullptr",
            "operator", "override", "private", "protected", "public", "register",
            "reinterpret_cast", "requires", "return", "short", "signed", "sizeof", "static",
            "static_assert", "static_cast", "struct", "switch", "template", "this",
            "thread_local", "throw", "true", "try", "typedef", "typeid", "typename", "union",
            "unsigned", "using", "virtual", "void", "volatile", "wchar_t", "while");

    private static final Set<String> KEYWORDS_JAVA = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "permits", "private", "protected",
            "public", "record", "return", "sealed", "short", "static", "strictfp", "super", "switch",
            "synchronized", "this", "throw", "throws", "transient", "true", "false", "null", "try",
            "var", "void", "volatile", "while");

    // Kotlin 1.0-1.9 hard + soft/modifier keywords (STYLE_KOTLIN.md/STYLE_KOTLIN2.md). Soft
    // keywords (e.g. `by`, `data`, `get`/`set`) are only reserved in specific positions in real
    // Kotlin, but the formatter has no need to allow them as plain identifiers elsewhere -- same
    // simplification already made for Java's `var`/`record` (both contextual in real Java, both
    // listed unconditionally in KEYWORDS_JAVA above).
    private static final Set<String> KEYWORDS_KOTLIN = setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
            "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
            "true", "try", "typealias", "typeof", "val", "var", "when", "while",
            "by", "catch", "companion", "const", "constructor", "crossinline", "data",
            "dynamic", "enum", "external", "field", "file", "final", "finally", "get",
            "import", "infix", "init", "inline", "inner", "internal", "lateinit", "noinline",
            "open", "operator", "out", "override", "param", "private", "property", "protected",
            "public", "receiver", "reified", "sealed", "set", "setparam", "suspend", "tailrec",
            "vararg", "where");

    private static final Set<String> NAMED_CONSTRUCT_C = setOf("struct", "enum");
    private static final Set<String> NAMED_CONSTRUCT_CPP =
            setOf("class", "struct", "enum", "namespace", "concept");
    private static final Set<String> NAMED_CONSTRUCT_JAVA =
            setOf("class", "interface", "enum", "record");
    private static final Set<String> NAMED_CONSTRUCT_KOTLIN =
            setOf("class", "object", "interface", "enum", "init");

    // Keywords that may legally appear inside a generic/template argument list without
    // invalidating the candidate `<>` pair -- e.g. `vector<int>`, `array<unsigned char, 4>`,
    // `<? extends T>`. Builtin C/C++ type keywords were missing here originally, which silently
    // blocked reclassification of the single most common template shape (`vector<int>`).
    // "in"/"out" (STYLE_KOTLIN.md §13, declaration-site use-site/generic variance -- `Box<out T>`,
    // `Comparable<in T>`) are Kotlin-only keywords, absent from every other language's keyword set,
    // so this is a pure no-op for C/C++/Java.
    private static final Set<String> GENERIC_SAFE_KEYWORDS = setOf(
            "extends", "super", "const", "typename", "class",
            "bool", "char", "char16_t", "char32_t", "double", "float", "int", "long",
            "short", "signed", "unsigned", "void", "wchar_t", "in", "out");

    // C++ cast keywords: `static_cast<T>(...)` etc. are tokenized as KEYWORD (not IDENTIFIER),
    // so the generic `<` after an IDENTIFIER check in reclassifyAngleBrackets() misses them
    // unless checked separately.
    private static final Set<String> CAST_KEYWORDS = setOf(
            "static_cast", "dynamic_cast", "reinterpret_cast", "const_cast");

    // Longest-prefix-first order matters: emitOperator() matches the first entry whose text the
    // source starts with, so "<=>" must precede "<=" (a strict prefix of it) or the spaceship
    // operator would be split into "<=" + ">".
    // Kotlin operators added here (`?.`, `!!`, `?:`, `..<`, `..`) are new entries, not shared with
    // C/C++/Java -- "..<" must precede ".." (a strict prefix of it), same longest-prefix-first
    // requirement already noted above for "..." vs "->"/"<=".
    private static final String[] MULTI_CHAR_OPS = {
            "<<=", ">>>=", ">>=", "...", "->*", "..<",
            "<=>", "::", "<<", ">>>", ">>", "<=", ">=", "==", "!=", "&&", "||",
            "++", "--", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "->", ".*",
            "?.", "?:", "!!", "..",
            "[[", "]]"
    };

    private final Lang lang;
    private final String language;
    private final Set<String> keywords;
    private final Set<String> namedConstructKeywords;

    private String source;
    private int pos;
    private int length;
    private boolean atLineStart;

    private int braceDepth;
    private int parenDepth;
    private int preprocessorDepth;
    private boolean syntaxError;
    private final Deque<String> nameStack = new LinkedList<>();
    private final Deque<Token> recentSignificant = new ArrayDeque<>();
    // record header tracking (`record Name(` ... `)` -- the component list sits between the
    // keyword+name and the body brace, so the simple 2-token `computeConstructName` lookback
    // can't see across it). `bracketNameStack` mirrors every `(`/`[` open/close 1:1 so it stays
    // balanced regardless of nested unrelated brackets (e.g. an annotation argument list inside
    // the component list); `pendingRecordName` is the most recently closed record component
    // list's name, consumed by the very next `{` (the record's own body -- nothing else can
    // legally emit a `{` between the component list and the body, since `implements`/generic
    // clauses there contain no brace-bearing expressions).
    private final Deque<String> bracketNameStack = new LinkedList<>(); // LinkedList allows null pushes
    private String pendingRecordName;
    // concept body tracking (`concept Name = requires(...) { ... }` -- the requires-expression's
    // own parameter list sits between the keyword+name and the body brace, same gap problem as
    // `record`'s component list, but simpler: nothing between `concept Name =` and that first `{`
    // can itself open a brace, so a single pending flag (armed on `=`, consumed by the very next
    // `{`) suffices with no paren-balancing needed. Cleared without effect on `;` first -- a
    // concept with no requires-expression body at all (`concept Integral = std::is_integral_v<T>;`).
    private String pendingConceptName;
    // Named-construct body tracking: armed when IDENTIFIER follows a named-construct keyword
    // (class/struct/enum/namespace), persists across inheritance clauses and base-type specifiers
    // so the `{` at the end of `class Foo : public Bar {` or `enum class E : uint32_t {` still
    // carries the correct construct name.  Cleared on `;`, on entering a paren group at depth 1
    // (which signals a function/method parameter list, not a construct header), and when consumed.
    private String pendingNamedConstructName;
    // Sticky flag set whenever a named-construct keyword (class/struct/enum/namespace) is seen.
    // Allows the IDENTIFIER lookup to skip over attribute-specifiers like `alignas(16)` that
    // sit between the keyword and the construct name.  Cleared when pendingNamedConstructName is
    // armed, on `;`, or on `{`/`}` (scope transition).
    private boolean namedConstructKeywordSeen;

    public boolean hasSyntaxError() {
        return syntaxError;
    }

    public TokenizerCore(final Lang lang) {
        this.lang = lang;
        this.language = lang.language;
        switch (language) {
            case "c":
                this.keywords = KEYWORDS_C;
                this.namedConstructKeywords = NAMED_CONSTRUCT_C;
                break;
            case "cpp":
                this.keywords = KEYWORDS_CPP;
                this.namedConstructKeywords = NAMED_CONSTRUCT_CPP;
                break;
            case "java":
                this.keywords = KEYWORDS_JAVA;
                this.namedConstructKeywords = NAMED_CONSTRUCT_JAVA;
                break;
            case "kotlin":
                this.keywords = KEYWORDS_KOTLIN;
                this.namedConstructKeywords = NAMED_CONSTRUCT_KOTLIN;
                break;
            default:
                throw new IllegalArgumentException("Unknown language: " + language);
        }
    }

    public List<Token> tokenize(final String source) {
        this.source = source;
        this.pos = 0;
        this.length = source.length();
        this.atLineStart = true;
        this.braceDepth = 0;
        this.parenDepth = 0;
        this.preprocessorDepth = 0;
        this.syntaxError = false;
        this.nameStack.clear();
        this.recentSignificant.clear();
        this.bracketNameStack.clear();
        this.pendingRecordName = null;
        this.pendingConceptName = null;
        this.pendingNamedConstructName = null;

        final List<Token> tokens = new ArrayList<>();

        while (pos < length) {
            final char c = source.charAt(pos);

            if (c == '\r' || c == '\n') {
                addToken(tokens, emitNewline());
                atLineStart = true;
                continue;
            }
            if (c == ' ' || c == '\t') {
                addToken(tokens, emitWhitespace());
                continue;
            }

            final Token t;
            if (isPreprocessorLanguage() && c == '#' && atLineStart) {
                t = emitPreprocessorOrDefine();
            } else if (c == '/' && peek(1) == '/') {
                t = emitLineComment();
            } else if (c == '/' && peek(1) == '*') {
                t = emitBlockComment();
            } else if (c == '"' && isTextBlockOpener()) {
                t = emitTextBlock();
            } else if (c == '"' && isKotlinRawStringOpener()) {
                t = emitKotlinRawString();
            } else if ((lang.isC || lang.isCpp) && rawStringPrefixLength() >= 0) {
                t = emitRawString(rawStringPrefixLength());
            } else if (c == '"') {
                t = emitString();
            } else if (c == '\'') {
                t = emitChar();
            } else if (Character.isDigit(c) || (c == '.' && Character.isDigit(peek(1)))) {
                t = emitNumber();
            } else if (isIdentifierStart(c)) {
                t = emitIdentifierOrKeyword();
            } else if (c == '[' && peek(1) == '[' && looksLikeAttributeOpen()) {
                t = emitOperator();
            } else if (c == ']' && peek(1) == ']') {
                t = emitOperator();
            } else if (c == '{') {
                t = emitOpenBrace();
            } else if (c == '}') {
                t = emitCloseBrace();
            } else if (c == '(' || c == '[') {
                t = emitOpenBracket(c);
            } else if (c == ')' || c == ']') {
                t = emitCloseBracket(c);
            } else if (c == ';' || c == ',') {
                t = emitPunct(c);
            } else {
                t = emitOperator();
            }

            addToken(tokens, t);
            atLineStart = false;
            if (syntaxError) {
                break;
            }
        }

        if (!syntaxError && !lang.isC) {
            reclassifyAngleBrackets(tokens);
        }

        return tokens;
    }

    private void addToken(final List<Token> tokens, final Token t) {
        tokens.add(t);
        trackSignificant(t);
    }

    private void trackSignificant(final Token t) {
        switch (t.type) {
            case WHITESPACE:
            case NEWLINE:
            case COMMENT_LINE:
            case COMMENT_BLOCK:
            case PREPROCESSOR:
                return;
            default:
                if (t.type == TokenType.OP && "=".equals(t.text)) {
                    final String conceptName = computeConceptHeaderName();
                    if (conceptName != null) {
                        pendingConceptName = conceptName;
                    }
                } else if (t.type == TokenType.PUNCT && ";".equals(t.text)) {
                    pendingConceptName = null;
                    pendingNamedConstructName = null;
                    namedConstructKeywordSeen = false;
                } else if (t.type == TokenType.PUNCT && "{".equals(t.text)) {
                    namedConstructKeywordSeen = false;
                } else if (t.type == TokenType.PUNCT && "}".equals(t.text)) {
                    namedConstructKeywordSeen = false;
                } else if (t.type == TokenType.OP && ":".equals(t.text) && namedConstructKeywordSeen) {
                    // A named-construct keyword directly followed by `:` with no identifier in
                    // between (Kotlin's anonymous `object : Comparable<Int> {`) has no name to
                    // arm -- without this, the next IDENTIFIER found (the supertype name,
                    // `Comparable`) would be wrongly captured as the construct's own name. Never
                    // fires for C/C++/Java: their named constructs always require a name token
                    // before any inheritance-clause `:`, so namedConstructKeywordSeen is already
                    // false (cleared by the arm-on-IDENTIFIER branch below) by the time a `:` is
                    // reached in those languages.
                    namedConstructKeywordSeen = false;
                } else if (t.type == TokenType.PUNCT && "(".equals(t.text) && t.parenDepth == 1) {
                    // Entering the outermost paren group (a function's parameter list) -- clear
                    // pendingNamedConstructName so the function body's `{` doesn't pick up the
                    // surrounding class/struct name.
                    pendingNamedConstructName = null;
                } else if (t.type == TokenType.KEYWORD && namedConstructKeywords.contains(t.text)
                        && !"concept".equals(t.text)) {
                    namedConstructKeywordSeen = true;
                }
                recentSignificant.addLast(t);
                if (recentSignificant.size() > 3) {
                    recentSignificant.removeFirst();
                }
                // Arm pendingNamedConstructName when IDENTIFIER follows a named-construct keyword.
                // namedConstructKeywordSeen survives attribute-specifiers like `alignas(16)` that
                // sit between the keyword and the name; the old arr[n-2] check only caught the
                // direct keyword→identifier case.
                if (t.type == TokenType.IDENTIFIER && t.parenDepth == 0 && namedConstructKeywordSeen) {
                    pendingNamedConstructName = t.text;
                    namedConstructKeywordSeen = false;
                } else if (t.type == TokenType.IDENTIFIER && t.parenDepth == 0
                        && pendingNamedConstructName != null && recentSignificant.size() >= 2) {
                    // Qualified namespace name (`namespace alpha::beta::gamma {`): each further
                    // `::identifier` segment after the first extends the already-armed name.
                    final Token[] arr = recentSignificant.toArray(new Token[0]);
                    final Token prev = arr[arr.length - 2];
                    if (prev.type == TokenType.OP && "::".equals(prev.text)) {
                        pendingNamedConstructName = pendingNamedConstructName + "::" + t.text;
                    } else if (prev.type == TokenType.IDENTIFIER) {
                        // Elaborated-type variable declaration (`struct sigaction sa = { };`):
                        // the first identifier after `struct` is a previously-declared type tag,
                        // not a new construct name, and this second identifier is the variable
                        // being declared -- the `{` that may follow is that variable's aggregate
                        // initializer, not a construct body.
                        pendingNamedConstructName = null;
                    }
                }
        }
    }

    /** True iff the last two significant tokens (before the `=` currently being tracked) are the
     *  `concept` keyword followed by its declared name -- arms {@code pendingConceptName}, the
     *  `concept` analog of {@link #computeRecordHeaderName}. */
    private String computeConceptHeaderName() {
        final Token[] arr = recentSignificant.toArray(new Token[0]); // oldest..newest
        final int n = arr.length;
        if (n < 2 || !namedConstructKeywords.contains("concept")) {
            return null;
        }
        final Token kw = arr[n - 2];
        final Token name = arr[n - 1];
        if (kw.type == TokenType.KEYWORD && "concept".equals(kw.text) && name.type == TokenType.IDENTIFIER) {
            return name.text;
        }
        return null;
    }

    // ── Named construct detection (for the `{` name stack) ─────────────────────────
    private String computeConstructName() {
        final Token[] arr = recentSignificant.toArray(new Token[0]); // oldest..newest
        final int n = arr.length;
        if (n >= 2) {
            final Token kw = arr[n - 2];
            final Token name = arr[n - 1];
            if ("extern".equals(kw.text) && kw.type == TokenType.KEYWORD
                    && name.type == TokenType.STRING && "\"C\"".equals(name.text)) {
                return kw.text + " " + name.text;
            }
            if (kw.type == TokenType.KEYWORD && namedConstructKeywords.contains(kw.text)
                    && name.type == TokenType.IDENTIFIER) {
                return name.text;
            }
        }
        if (n >= 3) {
            final Token enumKw = arr[n - 3];
            final Token classKw = arr[n - 2];
            final Token name = arr[n - 1];
            if ("enum".equals(enumKw.text) && "class".equals(classKw.text)
                    && namedConstructKeywords.contains("enum")
                    && name.type == TokenType.IDENTIFIER) {
                return name.text;
            }
        }
        return null;
    }

    /**
     * True if the last two significant tokens are the `record` keyword followed by its
     * declared name -- called when a `(` is emitted, to flag that paren group as a record's
     * component list (see {@code bracketNameStack}/{@code pendingRecordName} above). Generic
     * record names (`record Box<T>(...)`) are not recognized -- out of scope, same
     * bounded-effort spirit as {@code isAnonymousClassBrace}'s qualified-name limitation in
     * `BlockStructureRule`.
     */
    private String computeRecordHeaderName() {
        final Token[] arr = recentSignificant.toArray(new Token[0]); // oldest..newest
        final int n = arr.length;
        if (n < 2 || !namedConstructKeywords.contains("record")) {
            return null;
        }
        final Token kw = arr[n - 2];
        final Token name = arr[n - 1];
        if (kw.type == TokenType.KEYWORD && "record".equals(kw.text) && name.type == TokenType.IDENTIFIER) {
            return name.text;
        }
        return null;
    }

    // ── Per-construct emit helpers ──────────────────────────────────────────────────
    private Token emitOpenBrace() {
        final String name;
        if (pendingRecordName != null) {
            name = pendingRecordName;
            pendingRecordName = null;
        } else if (pendingConceptName != null) {
            name = pendingConceptName;
            pendingConceptName = null;
        } else if (pendingNamedConstructName != null) {
            name = pendingNamedConstructName;
            pendingNamedConstructName = null;
        } else {
            name = computeConstructName();
        }
        braceDepth++;
        nameStack.push(name);
        pos++;
        return new Token(TokenType.PUNCT, "{", braceDepth, parenDepth, name);
    }

    private Token emitCloseBrace() {
        if (braceDepth == 0) {
            syntaxError = true;
        }
        final String name = nameStack.isEmpty() ? null : nameStack.pop();
        braceDepth--;
        pos++;
        return new Token(TokenType.PUNCT, "}", braceDepth, parenDepth, name);
    }

    private Token emitOpenBracket(final char c) {
        bracketNameStack.push(c == '(' ? computeRecordHeaderName() : null);
        parenDepth++;
        pos++;
        return new Token(TokenType.PUNCT, String.valueOf(c), braceDepth, parenDepth, null);
    }

    private Token emitCloseBracket(final char c) {
        final String recordName = bracketNameStack.isEmpty() ? null : bracketNameStack.pop();
        if (c == ')' && recordName != null) {
            pendingRecordName = recordName;
        }
        parenDepth--;
        pos++;
        return new Token(TokenType.PUNCT, String.valueOf(c), braceDepth, parenDepth, null);
    }

    private Token emitPunct(final char c) {
        pos++;
        return new Token(TokenType.PUNCT, String.valueOf(c), braceDepth, parenDepth, null);
    }

    private Token emitPreprocessorOrDefine() {
        int p = pos + 1; // skip '#'
        while (p < length && (source.charAt(p) == ' ' || source.charAt(p) == '\t')) {
            p++;
        }
        final int wordStart = p;
        while (p < length && isIdentifierPart(source.charAt(p))) {
            p++;
        }
        final String directive = source.substring(wordStart, p);

        if ("define".equals(directive)) {
            return isMultilineDirective(p) ? emitMacroDef() : emitSingleLineDefine();
        }
        return emitPreprocessor();
    }

    private boolean isMultilineDirective(final int from) {
        int p = from;
        while (p < length && source.charAt(p) != '\n' && source.charAt(p) != '\r') {
            p++;
        }
        int q = p - 1;
        while (q >= from && (source.charAt(q) == ' ' || source.charAt(q) == '\t')) {
            q--;
        }
        return q >= from && source.charAt(q) == '\\';
    }

    private Token emitSingleLineDefine() {
        pos++; // '#'
        while (pos < length && (source.charAt(pos) == ' ' || source.charAt(pos) == '\t')) {
            pos++;
        }
        pos += "define".length();
        while (pos < length && (source.charAt(pos) == ' ' || source.charAt(pos) == '\t')) {
            pos++;
        }
        final int nameStart = pos;
        while (pos < length && isIdentifierPart(source.charAt(pos))) {
            pos++;
        }
        final String name = source.substring(nameStart, pos);

        String paramList = "";
        if (pos < length && source.charAt(pos) == '(') {
            final int paramStart = pos;
            int depth = 0;
            do {
                final char c = source.charAt(pos);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }
                pos++;
            } while (pos < length && depth > 0);
            paramList = source.substring(paramStart, pos);
        }

        final int restStart = pos;
        while (pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r') {
            pos++;
        }
        final String rest = source.substring(restStart, pos);

        final String text = "#define " + name + paramList + rest;
        return new Token(TokenType.PREPROCESSOR, text, braceDepth, parenDepth, null);
    }

    private Token emitMacroDef() {
        final int start = pos;
        while (true) {
            while (pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r') {
                pos++;
            }
            int q = pos - 1;
            while (q >= start && (source.charAt(q) == ' ' || source.charAt(q) == '\t')) {
                q--;
            }
            final boolean continues = q >= start && source.charAt(q) == '\\';
            if (!continues || pos >= length) {
                break;
            }
            if (source.charAt(pos) == '\r') {
                pos++;
                if (pos < length && source.charAt(pos) == '\n') {
                    pos++;
                }
            } else {
                pos++;
            }
        }
        return new Token(TokenType.MACRO_DEF, source.substring(start, pos), braceDepth,
                parenDepth, null);
    }

    /** Like {@link #emitMacroDef}, a {@code #if}/{@code #elif}/etc. directive can itself span
     *  multiple physical lines via a trailing {@code \} continuation (e.g. a long boolean
     *  condition) -- failing to consume those continuation lines here left their real `(`/`)`
     *  tokens to be lexed as ordinary PUNCT by the caller, permanently desyncing every
     *  brace/paren-depth counter for the remainder of the file from that point on. */
    private Token emitPreprocessor() {
        final int start = pos;
        while (true) {
            while (pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r') {
                pos++;
            }
            int q = pos - 1;
            while (q >= start && (source.charAt(q) == ' ' || source.charAt(q) == '\t')) {
                q--;
            }
            final boolean continues = q >= start && source.charAt(q) == '\\';
            if (!continues || pos >= length) {
                break;
            }
            if (source.charAt(pos) == '\r') {
                pos++;
                if (pos < length && source.charAt(pos) == '\n') {
                    pos++;
                }
            } else {
                pos++;
            }
        }
        return new Token(TokenType.PREPROCESSOR, source.substring(start, pos), braceDepth, parenDepth, null);
    }

    private Token emitLineComment() {
        final int start = pos;
        pos += 2;
        while (pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r') {
            pos++;
        }
        return new Token(TokenType.COMMENT_LINE, source.substring(start, pos), braceDepth,
                parenDepth, null);
    }

    private Token emitBlockComment() {
        final int start = pos;
        pos += 2;
        while (pos < length && !(source.charAt(pos) == '*' && peek(1) == '/')) {
            pos++;
        }
        if (pos < length) {
            pos += 2;
        }
        return new Token(TokenType.COMMENT_BLOCK, source.substring(start, pos), braceDepth,
                parenDepth, null);
    }

    /** True iff {@code pos} sits on the opening `"""` of a Java text block (STYLE_JAVA17.md §4) --
     *  three consecutive `"` characters, Java only ({@code emitString}'s plain-string path already
     *  bails on a bare `"` followed by a newline before finding its own closing quote, which is
     *  exactly what would otherwise happen here: without this check, a text block's opening `"""`
     *  mis-lexes as an empty string token followed by a single stray-quote token, exposing the
     *  block's entire multi-line content -- braces, indentation, everything -- to every other rule
     *  in the pipeline). */
    private boolean isTextBlockOpener() {
        return lang.isJava && peek(1) == '"' && peek(2) == '"';
    }

    /**
     * Lexes a Java text block (`"""..."""`) as a single opaque STRING token spanning every
     * physical line it covers -- same "one token, internal newlines embedded in its own text,
     * never split into separate NEWLINE tokens" precedent already established by {@link
     * #emitBlockComment} for `/<i></i>* ... *<i></i>/`. This is what makes STYLE_JAVA17.md §4's "preserved
     * exactly as written" requirement fall out for free: nothing downstream that scans for
     * NEWLINE tokens to detect "spans multiple physical lines" ever sees inside a text block's
     * content, and Phase 6's indentation-conversion pass (which only ever rewrites line-start
     * WHITESPACE tokens, found by walking from one NEWLINE to the next) never reaches in to
     * rewrite it either -- both already true for block comments, now true here too, without any
     * special-casing needed in either of those passes.
     *
     * <p>A `\`-escaped quote is skipped as a pair (mirroring {@code emitString}/{@code emitChar}'s
     * own escape handling) so an escaped `\"""` inside the content is never mistaken for the
     * closing delimiter. An unterminated text block (no closing `"""` before EOF) is consumed to
     * the end of the source -- same graceful-degradation posture as {@code emitBlockComment}'s own
     * unterminated-comment handling, never a crash.
     */
    private Token emitTextBlock() {
        final int start = pos;
        pos += 3;
        while (pos < length) {
            final char c = source.charAt(pos);
            if (c == '\\' && pos + 1 < length) {
                pos += 2;
                continue;
            }
            if (c == '"' && peek(1) == '"' && peek(2) == '"') {
                pos += 3;
                break;
            }
            pos++;
        }
        return new Token(TokenType.STRING, source.substring(start, pos), braceDepth, parenDepth,
                null);
    }

    /** True iff {@code pos} sits on the opening `"""` of a Kotlin raw string. Neither
     *  STYLE_KOTLIN.md nor STYLE_KOTLIN2.md mentions raw strings at all -- surfaced as a
     *  side-finding while fixing §19's interpolation-nesting risk (RDD_KEY_116) and confirmed via
     *  harness to be badly broken: without this check, `"""hello "world" end"""` mis-lexed as five
     *  tokens (`""` / `"hello "` / `world` (a bare `IDENTIFIER`!) / `" end"` / `""`) instead of one,
     *  and a multi-line raw string mis-lexed a spurious `NEWLINE` token into the middle of what
     *  should be one opaque string -- exposing the content's real newlines to every indentation/
     *  scope pass in the pipeline exactly the way an unrecognized text block would. */
    private boolean isKotlinRawStringOpener() {
        return lang.isKotlin && peek(1) == '"' && peek(2) == '"';
    }

    /**
     * Lexes a Kotlin raw string (`"""..."""`) as a single opaque STRING token, same "one token,
     * internal newlines embedded in its own text" precedent as {@link #emitTextBlock}/{@link
     * #emitBlockComment}. Two rules make this a different scan from every other string helper in
     * this file:
     * <ul>
     *   <li>No backslash-escape processing at all -- unlike every other Kotlin/C/Java string or
     *       char literal, `\` is a plain literal character inside a raw string (this is the whole
     *       point of the construct), so a lone trailing `\` right before the closing `"""` must
     *       not swallow the first delimiter quote the way an escape pair would elsewhere.</li>
     *   <li>Termination is greedy on the *first* `"""` encountered, exactly matching the real
     *       Kotlin compiler: a raw string is free to contain runs of one or two unescaped `"`
     *       characters as plain content (e.g. `"""hello "world" end"""`), but the moment three
     *       land in a row, that's the close -- even if a fourth immediately follows (a literal
     *       trailing `"` right at the end needs `${'"'}` in real Kotlin source; this tokenizer
     *       doesn't need to solve that, it only needs to match where the closing delimiter falls).</li>
     * </ul>
     * `${...}` interpolation is still recognized and depth-tracked via {@link
     * #skipKotlinInterpolationBlock} exactly as in {@link #skipKotlinString} -- interpolation
     * works identically inside a raw string, including a nested raw string inside the
     * interpolation expression itself ({@code "${"""nested"""}"}), which {@code
     * skipKotlinInterpolationBlock} now dispatches back into this same method for.
     */
    private int skipKotlinRawString(final int openIdx) {
        int p = openIdx + 3;
        while (p < length) {
            final char c = source.charAt(p);
            if (c == '"' && p + 2 < length && source.charAt(p + 1) == '"' && source.charAt(p + 2) == '"') {
                return p + 3;
            }
            if (c == '$' && p + 1 < length && source.charAt(p + 1) == '{') {
                p = skipKotlinInterpolationBlock(p + 2);
                continue;
            }
            p++;
        }
        return p;
    }

    private Token emitKotlinRawString() {
        final int start = pos;
        pos = skipKotlinRawString(pos);
        return new Token(TokenType.STRING, source.substring(start, pos), braceDepth, parenDepth,
                null);
    }

    private static final String[] RAW_STRING_PREFIXES = { "u8R", "uR", "UR", "LR", "R" };

    /** C++11 raw string literals (`R"delim(...)delim"`, optionally prefixed by an encoding
     *  prefix `u8`/`u`/`U`/`L`) can contain arbitrary characters -- including `{`/`}` -- as plain
     *  content (nanobench's mustache HTML templates are stored this way). Without recognizing the
     *  whole thing as one opaque token, {@code emitIdentifierOrKeyword} + {@code emitString} would
     *  lex the prefix, the raw delimiter, and the literal's contents as ordinary source, exposing
     *  any brace characters inside to the brace-depth tracker that every scope-splitting pass
     *  relies on -- silently corrupting nesting depth for the rest of the file. Returns the
     *  prefix length (`"R"` = 1, `"u8R"` = 3, ...) if {@code pos} sits on a genuine raw string
     *  opener (prefix + `"` + a valid delimiter of at most 16 chars with no whitespace/paren/
     *  backslash + `(`), else -1. */
    private int rawStringPrefixLength() {
        for (final String prefix : RAW_STRING_PREFIXES) {
            if (!source.startsWith(prefix, pos)) {
                continue;
            }
            int p = pos + prefix.length();
            if (p >= length || source.charAt(p) != '"') {
                continue;
            }
            p++;
            final int delimStart = p;
            while (p < length && p - delimStart <= 16 && source.charAt(p) != '(') {
                final char dc = source.charAt(p);
                if (dc == ' ' || dc == '\t' || dc == '\n' || dc == '\r' || dc == '\\' || dc == ')') {
                    break;
                }
                p++;
            }
            if (p < length && source.charAt(p) == '(') {
                return prefix.length();
            }
        }
        return -1;
    }

    /** Lexes a raw string literal as a single opaque STRING token, from the encoding/`R` prefix
     *  through the closing `)delim"` -- content in between (including any `{`/`}`/`"` chars) is
     *  never re-examined by the brace-depth tracker or any other rule (same "opaque, own text,
     *  never split" precedent as {@link #emitBlockComment}/{@link #emitTextBlock}). An unterminated
     *  raw string (no matching `)delim"` before EOF) is consumed to the end of the source. */
    private Token emitRawString(final int prefixLen) {
        final int start = pos;
        pos += prefixLen + 1; // prefix + opening `"`
        final int delimStart = pos;
        while (pos < length && source.charAt(pos) != '(') {
            pos++;
        }
        final String delim = source.substring(delimStart, pos);
        final String closer = ")" + delim + "\"";
        if (pos < length) {
            pos++; // `(`
        }
        final int closerIdx = source.indexOf(closer, pos);
        if (closerIdx < 0) {
            pos = length;
        } else {
            pos = closerIdx + closer.length();
        }
        return new Token(TokenType.STRING, source.substring(start, pos), braceDepth, parenDepth,
                null);
    }

    private Token emitString() {
        final int start = pos;
        if (lang.isKotlin) {
            pos = skipKotlinString(pos);
        } else {
            pos++;
            while (pos < length) {
                final char c = source.charAt(pos);
                if (c == '\\' && pos + 1 < length) {
                    pos += 2;
                    continue;
                }
                if (c == '"') {
                    pos++;
                    break;
                }
                if (c == '\n' || c == '\r') {
                    break;
                }
                pos++;
            }
        }
        return new Token(TokenType.STRING, source.substring(start, pos), braceDepth, parenDepth,
                null);
    }

    /**
     * Kotlin-only string scan (STYLE_KOTLIN.md §19): unlike the plain scan-to-next-quote above
     * (correct for C/Java, which have no string interpolation), a Kotlin `"..."` string can
     * contain a `${...}` interpolation block whose expression may itself contain nested string
     * literals -- each with its own `${...}` -- nested `{ }` (e.g. a lambda argument passed inside
     * the interpolation), and nested char literals, any of which can contain a `"` that must not
     * be mistaken for this string's own closing quote. Confirmed via harness that the naive scan
     * misreads `"${foo("x")}"` as three tokens (`"${foo("` / `x` / `")}"`) rather than one,
     * corrupting the token stream (a later spacing pass could then insert whitespace *inside* the
     * string literal, since it no longer looks like one token). {@code $x} (bare interpolation, no
     * braces) needs no special handling here -- the `$` itself introduces no nesting risk, so it's
     * simply ordinary string content as far as quote-matching is concerned.
     */
    private int skipKotlinString(final int openQuoteIdx) {
        int p = openQuoteIdx + 1;
        while (p < length) {
            final char c = source.charAt(p);
            if (c == '\\' && p + 1 < length) {
                p += 2;
                continue;
            }
            if (c == '"') {
                return p + 1;
            }
            if (c == '\n' || c == '\r') {
                return p;
            }
            if (c == '$' && p + 1 < length && source.charAt(p + 1) == '{') {
                p = skipKotlinInterpolationBlock(p + 2);
                continue;
            }
            p++;
        }
        return p;
    }

    /** Scans from just after a `${`'s opening `{` to just past its matching `}`, skipping over any
     *  nested `{`/`}` (e.g. a lambda literal passed inside the interpolation expression), string
     *  literals (recursively, via {@link #skipKotlinString}), and char literals encountered along
     *  the way -- so a `"`/`{`/`}` inside any of those never desynchronizes the depth count or gets
     *  mistaken for this block's own closing `}`. */
    private int skipKotlinInterpolationBlock(final int startIdx) {
        int p = startIdx;
        int depth = 1;
        while (p < length && depth > 0) {
            final char c = source.charAt(p);
            if (c == '"' && p + 2 < length && source.charAt(p + 1) == '"' && source.charAt(p + 2) == '"') {
                p = skipKotlinRawString(p);
                continue;
            }
            if (c == '"') {
                p = skipKotlinString(p);
                continue;
            }
            if (c == '\'') {
                p = skipKotlinChar(p);
                continue;
            }
            if (c == '{') {
                depth++;
                p++;
                continue;
            }
            if (c == '}') {
                depth--;
                p++;
                continue;
            }
            p++;
        }
        return p;
    }

    /** Same scan as {@link #emitChar} but returning the end index rather than allocating a
     *  {@code Token} -- used by {@link #skipKotlinInterpolationBlock} to skip a char literal
     *  without misreading its own quote as structurally significant. */
    private int skipKotlinChar(final int openQuoteIdx) {
        int p = openQuoteIdx + 1;
        while (p < length) {
            final char c = source.charAt(p);
            if (c == '\\' && p + 1 < length) {
                p += 2;
                continue;
            }
            if (c == '\'') {
                return p + 1;
            }
            if (c == '\n' || c == '\r') {
                return p;
            }
            p++;
        }
        return p;
    }

    private Token emitChar() {
        final int start = pos;
        pos++;
        while (pos < length) {
            final char c = source.charAt(pos);
            if (c == '\\' && pos + 1 < length) {
                pos += 2;
                continue;
            }
            if (c == '\'') {
                pos++;
                break;
            }
            if (c == '\n' || c == '\r') {
                break;
            }
            pos++;
        }
        return new Token(TokenType.CHAR, source.substring(start, pos), braceDepth, parenDepth,
                null);
    }

    private Token emitNumber() {
        final int start = pos;
        while (pos < length) {
            final char c = source.charAt(pos);
            // A `.` followed by another `.` is never a decimal point -- it's the start of
            // Kotlin's `..`/`..<` range operator (e.g. `1..10`), which must not be swallowed
            // into the number literal.
            if (c == '.' && peek(1) == '.') {
                break;
            }
            if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '\'') {
                pos++;
                continue;
            }
            if ((c == '+' || c == '-') && pos > start) {
                final char prev = source.charAt(pos - 1);
                if (prev == 'e' || prev == 'E' || prev == 'p' || prev == 'P') {
                    pos++;
                    continue;
                }
            }
            break;
        }
        return new Token(TokenType.NUMBER, source.substring(start, pos), braceDepth, parenDepth,
                null);
    }

    private Token emitIdentifierOrKeyword() {
        final int start = pos;
        while (pos < length && isIdentifierPart(source.charAt(pos))) {
            pos++;
        }
        final String text = source.substring(start, pos);
        final TokenType type = keywords.contains(text) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
        return new Token(type, text, braceDepth, parenDepth, null);
    }

    // Guards the "[[" -> single-OP-token merge (C++17 attribute syntax, e.g. `[[noreturn]]`)
    // against Objective-C nested message sends like `[[NSString alloc] initWithFormat:...]`,
    // which also start with two adjacent '[' but whose brackets close separately (`] ... ]`,
    // never `]]`). Real attribute-lists are short and self-contained: scans forward from just
    // after the "[[" for a '[' at paren-depth 0 that is immediately followed by another ']'
    // (i.e. a genuine "]]" close with nothing but identifiers/`::`/parenthesized args in
    // between). Bails (returns false) on anything that couldn't appear in attribute syntax --
    // a bare single ']', a string/char literal, a statement terminator, or a brace -- which is
    // exactly what a message-send receiver/argument list looks like.
    private boolean looksLikeAttributeOpen() {
        int depth = 0;
        final int limit = Math.min(length, pos + 200);
        for (int i = pos + 2; i < limit; i++) {
            final char c = source.charAt(i);
            if (c == '\n' || c == '\r' || c == ';' || c == '{' || c == '}'
                    || c == '"' || c == '\'') {
                return false;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ']' && depth == 0) {
                return i + 1 < length && source.charAt(i + 1) == ']';
            }
        }
        return false;
    }

    private Token emitOperator() {
        // Consume a maximal run of '*'
        if (source.charAt(pos) == '*') {
            final int start = pos;
            while (pos < source.length() && source.charAt(pos) == '*') {
                pos++;
            }
            return new Token(TokenType.OP, source.substring(start, pos), braceDepth, parenDepth, null);
        }

        for (final String op : MULTI_CHAR_OPS) {
            if (source.startsWith(op, pos)) {
                pos += op.length();
                return new Token(TokenType.OP, op, braceDepth, parenDepth, null);
            }
        }

        final char c = source.charAt(pos);
        pos++;
        return new Token(TokenType.OP, String.valueOf(c), braceDepth, parenDepth, null);
    }

    private Token emitWhitespace() {
        final int start = pos;
        while (pos < length && (source.charAt(pos) == ' ' || source.charAt(pos) == '\t')) {
            pos++;
        }
        return new Token(TokenType.WHITESPACE, source.substring(start, pos), braceDepth,
                parenDepth, null);
    }

    private Token emitNewline() {
        final int start = pos;
        if (source.charAt(pos) == '\r') {
            pos++;
            if (pos < length && source.charAt(pos) == '\n') {
                pos++;
            }
        } else {
            pos++;
        }
        return new Token(TokenType.NEWLINE, source.substring(start, pos), braceDepth, parenDepth,
                null);
    }

    private static boolean isCastKeyword(final Token t) {
        return t.type == TokenType.KEYWORD && CAST_KEYWORDS.contains(t.text);
    }

    // ── Generic/template angle bracket disambiguation ───────────────────────────────
    private void reclassifyAngleBrackets(final List<Token> tokens) {
        final List<Integer> sig = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            final TokenType ty = tokens.get(i).type;
            if (ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE
                    || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK
                    || ty == TokenType.PREPROCESSOR) {
                continue;
            }
            sig.add(i);
        }

        final Deque<int[]> openStack = new ArrayDeque<>(); // each entry: {tokenIndex, validFlag}

        for (int s = 0; s < sig.size(); s++) {
            final int idx = sig.get(s);
            final Token cur = tokens.get(idx);

            if (cur.type == TokenType.PUNCT
                    && (";".equals(cur.text) || "{".equals(cur.text) || "}".equals(cur.text))) {
                openStack.clear();
                continue;
            }

            if (cur.type == TokenType.OP && "<".equals(cur.text)) {
                final Token prev = s > 0 ? tokens.get(sig.get(s - 1)) : null;
                if (prev != null && (prev.type == TokenType.IDENTIFIER || isCastKeyword(prev))) {
                    openStack.push(new int[] {idx, 1});
                } else if (!openStack.isEmpty()) {
                    invalidateAll(openStack);
                }
                continue;
            }

            if (cur.type == TokenType.OP && ">".equals(cur.text)) {
                if (!openStack.isEmpty()) {
                    final int[] open = openStack.pop();
                    if (open[1] == 1) {
                        tokens.set(open[0], retype(tokens.get(open[0]), TokenType.ANGLE_BRACKET_OPEN));
                        tokens.set(idx, retype(cur, TokenType.ANGLE_BRACKET_CLOSE));
                    }
                }
                continue;
            }

            if (cur.type == TokenType.OP && ">>".equals(cur.text)) {
                if (openStack.size() >= 2) {
                    final int[] inner = openStack.pop();
                    final int[] outer = openStack.pop();
                    if (inner[1] == 1 && outer[1] == 1) {
                        tokens.set(inner[0], retype(tokens.get(inner[0]), TokenType.ANGLE_BRACKET_OPEN));
                        tokens.set(outer[0], retype(tokens.get(outer[0]), TokenType.ANGLE_BRACKET_OPEN));
                        tokens.set(idx, retype(cur, TokenType.ANGLE_BRACKET_CLOSE));
                        tokens.add(idx + 1, new Token(TokenType.ANGLE_BRACKET_CLOSE, "",
                                cur.braceDepth, cur.parenDepth, null));
                        shiftSigAfter(sig, s, idx + 1);
                    }
                } else if (openStack.size() == 1) {
                    final int[] open = openStack.pop();
                    if (open[1] == 1) {
                        tokens.set(open[0], retype(tokens.get(open[0]), TokenType.ANGLE_BRACKET_OPEN));
                        tokens.set(idx, retype(cur, TokenType.ANGLE_BRACKET_CLOSE));
                        tokens.add(idx + 1,
                                new Token(TokenType.OP, ">", cur.braceDepth, cur.parenDepth, null));
                        shiftSigAfter(sig, s, idx + 1);
                    }
                }
                continue;
            }

            if (!isGenericSafeToken(cur) && !openStack.isEmpty()) {
                invalidateAll(openStack);
            }
        }
    }

    private void shiftSigAfter(final List<Integer> sig, final int afterPos, final int newIndex) {
        for (int j = afterPos + 1; j < sig.size(); j++) {
            sig.set(j, sig.get(j) + 1);
        }
        sig.add(afterPos + 1, newIndex);
    }

    private void invalidateAll(final Deque<int[]> stack) {
        for (final int[] entry : stack) {
            entry[1] = 0;
        }
    }

    private boolean isGenericSafeToken(final Token t) {
        switch (t.type) {
            case IDENTIFIER:
            case NUMBER:
            case STRING:
            case CHAR:
            case ANGLE_BRACKET_OPEN:
            case ANGLE_BRACKET_CLOSE:
                return true;
            case KEYWORD:
                return GENERIC_SAFE_KEYWORDS.contains(t.text);
            case PUNCT:
                // `(`/`)` allowed too: a function-type template argument (`std::function<void(int)>`)
                // has a balanced parameter list nested directly inside the `<>`.
                return ",".equals(t.text) || "[".equals(t.text) || "]".equals(t.text)
                        || "(".equals(t.text) || ")".equals(t.text);
            case OP:
                return ".".equals(t.text) || "::".equals(t.text) || "?".equals(t.text)
                        || "*".equals(t.text) || "&".equals(t.text);
            default:
                return false;
        }
    }

    private Token retype(final Token t, final TokenType newType) {
        return new Token(newType, t.text, t.braceDepth, t.parenDepth, t.name);
    }

    private boolean isPreprocessorLanguage() {
        // Java source files sometimes carry PCPP-style C-preprocessor directives
        // (`#define`/`#ifdef`/etc.) as a poor man's template mechanism ahead of a separate
        // preprocessing step. Lexing them as opaque PREPROCESSOR/MACRO_DEF tokens (same as
        // C/C++) means every rule already passes them through untouched -- no per-rule
        // Java-specific handling needed.
        return true;
    }

    private boolean isIdentifierStart(final char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private boolean isIdentifierPart(final char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private char peek(final int offset) {
        final int p = pos + offset;
        return p < length ? source.charAt(p) : '\0';
    }
}
