/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.tokenizer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.jxmake.formatter.Lang;

/**
 * Curly-brace-family tokenizer (C/C++/Java/Kotlin) -- everything in this file used to live
 * directly in {@link TokenizerCore} before the curly/indent/tags class-refactor
 * (STATE_COMMON.md's "Class Refactor" section); no behavior change, mechanical move only.
 */
public class TokenizerCurly extends TokenizerCore {

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
        "auto",
        "bool",
        "break",
        "case",
        "catch",
        "char",
        "char16_t",
        "char32_t",
        "class",
        "co_await",
        "co_return",
        "co_yield",
        "concept",
        "const",
        "constexpr",
        "consteval",
        "constinit",
        "const_cast",
        "continue",
        "decltype",
        "default",
        "delete",
        "do",
        "double",
        "dynamic_cast",
        "else",
        "enum",
        "explicit",
        "export",
        "extern",
        "false",
        "final",
        "float",
        "for",
        "friend",
        "goto",
        "if",
        "inline",
        "int",
        "long",
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
        "register",
        "reinterpret_cast",
        "requires",
        "return",
        "short",
        "signed",
        "sizeof",
        "static",
        "static_assert",
        "static_cast",
        "struct",
        "switch",
        "template",
        "this",
        "thread_local",
        "throw",
        "true",
        "try",
        "typedef",
        "typeid",
        "typename",
        "union",
        "unsigned",
        "using",
        "virtual",
        "void",
        "volatile",
        "wchar_t",
        "while"
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
        "double",
        "else",
        "enum",
        "extends",
        "final",
        "finally",
        "float",
        "for",
        "goto",
        "if",
        "implements",
        "import",
        "instanceof",
        "int",
        "interface",
        "long",
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
        "short",
        "static",
        "strictfp",
        "super",
        "switch",
        "synchronized",
        "this",
        "throw",
        "throws",
        "transient",
        "true",
        "false",
        "null",
        "try",
        "var",
        "void",
        "volatile",
        "while"
    );

    // Kotlin 1.0-1.9 hard + soft/modifier keywords (STYLE_KOTLIN.md/STYLE_KOTLIN2.md). Soft
    // keywords (e.g. `by`, `data`, `get`/`set`) are only reserved in specific positions in real
    // Kotlin, but the formatter has no need to allow them as plain identifiers elsewhere -- same
    // simplification already made for Java's `var`/`record` (both contextual in real Java, both
    // listed unconditionally in KEYWORDS_JAVA above).
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
        "while",
        "by",
        "catch",
        "companion",
        "const",
        "constructor",
        "crossinline",
        "data",
        "dynamic",
        "enum",
        "external",
        "field",
        "file",
        "final",
        "finally",
        "get",
        "import",
        "infix",
        "init",
        "inline",
        "inner",
        "internal",
        "lateinit",
        "noinline",
        "open",
        "operator",
        "out",
        "override",
        "param",
        "private",
        "property",
        "protected",
        "public",
        "receiver",
        "reified",
        "sealed",
        "set",
        "setparam",
        "suspend",
        "tailrec",
        "vararg",
        "where"
    );

    // JS keywords (ES2024+, STYLE_JS_TS.md). `class`/`function`/`interface` etc. share the
    // curly-family named-construct machinery below the same way Java/Kotlin's do.
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
        "get",
        "if",
        "import",
        "in",
        "instanceof",
        "let",
        "new",
        "null",
        "of",
        "return",
        "set",
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

    // TS adds its own keyword vocabulary on top of every JS keyword (RDD_KEY_187 -- shared
    // curly classes gated on lang.isJs/isTs, no separate JsTokenizer/TsTokenizer). STYLE_JS_TS.md
    // §11's own modifier-priority table (declare/visibility/static/abstract/override/readonly)
    // and §12/§14 (enum/interface/type) all need their keywords recognized here.
    private static final Set<String> KEYWORDS_TS = new HashSet<>(KEYWORDS_JS);
    static {
        KEYWORDS_TS.addAll( Arrays.asList(
                "abstract", "any", "as", "asserts", "bigint", "boolean", "declare", "enum",
                "implements", "infer", "interface", "is", "keyof", "namespace", "never", "number",
                "object", "override", "private", "protected", "public", "readonly", "satisfies",
                "string", "symbol", "type", "undefined", "unique", "unknown") );
    }

    private static final Set<String> NAMED_CONSTRUCT_C      = setOf("struct", "enum");
    private static final Set<String> NAMED_CONSTRUCT_CPP    = setOf(
        "class", "struct", "enum", "namespace", "concept"
    );
    private static final Set<String> NAMED_CONSTRUCT_JAVA   = setOf(
        "class", "interface", "enum", "record"
    );
    private static final Set<String> NAMED_CONSTRUCT_KOTLIN = setOf(
        "class", "object", "interface", "enum", "init"
    );
    private static final Set<String> NAMED_CONSTRUCT_JS     = setOf("class");
    private static final Set<String> NAMED_CONSTRUCT_TS     = setOf(
        "class", "interface", "enum", "namespace"
    );

    // Keywords that may legally appear inside a generic/template argument list without
    // invalidating the candidate `<>` pair -- e.g. `vector<int>`, `array<unsigned char, 4>`,
    // `<? extends T>`. Builtin C/C++ type keywords were missing here originally, which silently
    // blocked reclassification of the single most common template shape (`vector<int>`).
    // "in"/"out" (STYLE_KOTLIN.md §13, declaration-site use-site/generic variance -- `Box<out T>`,
    // `Comparable<in T>`) are Kotlin-only keywords, absent from every other language's keyword set,
    // so this is a pure no-op for C/C++/Java.
    private static final Set<String> GENERIC_SAFE_KEYWORDS = setOf(
        "extends",
        "super",
        "const",
        "typename",
        "class",
        "bool",
        "char",
        "char16_t",
        "char32_t",
        "double",
        "float",
        "int",
        "long",
        "short",
        "signed",
        "unsigned",
        "void",
        "wchar_t",
        "in",
        "out",
        "string",
        "number",
        "boolean",
        "any",
        "unknown",
        "never",
        "object",
        "undefined",
        "null",
        "symbol",
        "bigint",
        "true",
        "false",
        "keyof",
        "is",
        "infer",
        "asserts",
        "readonly",
        "unique",
        "as",
        "satisfies",
        "typeof",
        "import"
    );

    // C++ cast keywords: `static_cast<T>(...)` etc. are tokenized as KEYWORD (not IDENTIFIER),
    // so the generic `<` after an IDENTIFIER check in reclassifyAngleBrackets() misses them
    // unless checked separately.
    private static final Set<String> CAST_KEYWORDS = setOf(
        "static_cast", "dynamic_cast", "reinterpret_cast", "const_cast"
    );

    // Longest-prefix-first order matters: emitOperator() matches the first entry whose text the
    // source starts with, so "<=>" must precede "<=" (a strict prefix of it) or the spaceship
    // operator would be split into "<=" + ">".
    // Kotlin operators added here (`?.`, `!!`, `?:`, `..<`, `..`) are new entries, not shared with
    // C/C++/Java -- "..<" must precede ".." (a strict prefix of it), same longest-prefix-first
    // requirement already noted above for "..." vs "->"/"<=".
    // "===" / "!==" (Kotlin referential equality/inequality, STYLE_KOTLIN.md) are new entries
    // here too -- must precede their 2-char prefixes "==" / "!=" for the same longest-prefix-first
    // reason noted above ("..<" vs ".."). Found missing entirely via real-code compile-checking
    // against `square/okio`: `if (next !== this)` was lexed as the two tokens "!=" and "="
    // instead of one "!==" token, which a later paren-tightening pass then re-spaced into the
    // invalid `!= =` (RDD_KEY_150) -- a pure tokenizer gap, not a rendering-pass bug.
    // "^^" (C++26 reflection operator, STYLE_CPP26.md §5) and "[:"/":]" (C++26 splice brackets,
    // same section) are new entries for the §5 tokenizer-support pass -- none of the three is a
    // strict prefix of any other existing entry (or vice versa), so no ordering constraint among
    // them, but "^^" must still be reachable before emitOperator()'s single-char fallback would
    // otherwise split it into two "^" tokens. "[:" additionally needs a dispatch-loop branch (see
    // readToken's `c == '[' && peek(1) == ':'` case) since a leading `[` is otherwise intercepted
    // by the open-bracket branch before ever reaching emitOperator() -- unlike ":]", which starts
    // with `:` and already falls through to emitOperator() via the loop's default case.
    // JS/TS entries (STYLE_JS_TS.md §6/§7): "=>" (arrow), "??=" (nullish-coalescing assignment,
    // must precede its own "??" prefix and the plain "=" fallback), "??" (nullish coalescing --
    // "?." is already present above, shared with Kotlin's safe-call operator). None of these are a
    // prefix of any pre-existing C/C++/Java/Kotlin entry (or vice versa), so purely additive.
    private static final String[] MULTI_CHAR_OPS = {
            "<<=", ">>>=", ">>=", "...", "->*", "..<",
            "<=>", "::", "<<", ">>>", ">>", "<=", ">=", "===", "!==", "==", "!=", "&&=", "||=", "&&", "||",
            "++", "--", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "->", ".*",
            "?.", "?:", "!!", "..",
            "[[", "]]", "^^", "[:", ":]",
            "=>", "??=", "??"
    };

    private final Lang        lang;
    private final String      language;
    private final Set<String> keywords;
    private final Set<String> namedConstructKeywords;

    private boolean atLineStart;

    private       int           preprocessorDepth;
    private       boolean       syntaxError;
    private final Deque<String> nameStack         = new LinkedList<>();
    private final Deque<Token>  recentSignificant = new ArrayDeque<>();
    // Record header tracking (`record Name(` ... `)` -- the component list sits between the
    // keyword+name and the body brace, so the simple 2-token `computeConstructName` lookback
    // can't see across it). `bracketNameStack` mirrors every `(`/`[` open/close 1:1 so it stays
    // balanced regardless of nested unrelated brackets (e.g. an annotation argument list inside
    // the component list); `pendingRecordName` is the most recently closed record component
    // list's name, consumed by the very next `{` (the record's own body -- nothing else can
    // legally emit a `{` between the component list and the body, since `implements`/generic
    // clauses there contain no brace-bearing expressions).
    private final Deque<String> bracketNameStack = new LinkedList<>(); // LinkedList allows null pushes
    private       String        pendingRecordName;
    // Concept body tracking (`concept Name = requires(...) { ... }` -- the requires-expression's
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

    public boolean hasSyntaxError()
    {
        return syntaxError;
    }

    public TokenizerCurly(final Lang lang)
    {
        this.lang     = lang;
        this.language = lang.language;
        switch(language) {

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

            case "js":
                this.keywords = KEYWORDS_JS;
                this.namedConstructKeywords = NAMED_CONSTRUCT_JS;
                break;

            case "ts":
                this.keywords = KEYWORDS_TS;
                this.namedConstructKeywords = NAMED_CONSTRUCT_TS;
                break;

            default:
                throw new IllegalArgumentException("Unknown language: " + language);

        } // switch
    }

    public List<Token> tokenize(final String source)
    {
        this.source            = source;
        this.pos               = 0;
        this.length            = source.length();
        this.atLineStart       = true;
        this.braceDepth        = 0;
        this.parenDepth        = 0;
        this.preprocessorDepth = 0;
        this.syntaxError       = false;
        this.nameStack.clear();
        this.recentSignificant.clear();
        this.bracketNameStack.clear();
        this.pendingRecordName         = null;
        this.pendingConceptName        = null;
        this.pendingNamedConstructName = null;

        final List<Token> tokens = new ArrayList<>();

        while(pos < length) {
            tokenizeOneUnit(tokens);
            if(syntaxError) break;
        } // while

        // Must run BEFORE reclassifyAngleBrackets (STATE_JS_TS.md's 2026-08-12 design session,
        // point 3): once a JSX tree is collapsed into one opaque JSX_SPAN token, its interior
        // `<`/`>` characters are gone from the significant-token list reclassifyAngleBrackets
        // walks, so there is nothing left for it to misinterpret. Scoped to `.jsx`/`.tsx` files
        // only (`lang.isJsxSyntax`) -- a plain `.ts`/`.js` file must see zero behavior change.
        if(!syntaxError && lang.isJsxSyntax) findJsxSpans(tokens);

        if(!syntaxError && !lang.isC) reclassifyAngleBrackets(tokens);

        return tokens;
    }

    /**
     * Performs exactly one lexical step of the main character-level scan (dispatches on
     * {@code source.charAt(pos)}, emits the resulting token(s) via {@link #addToken}, advances
     * {@code pos}). Originally the direct body of {@link #tokenize}'s own {@code while} loop;
     * extracted (STATE_JS_TS.md's 2026-08-13 scoping session, sub-context 0) so a template-literal
     * hole's interior can re-enter the exact same dispatch the top-level scan uses (see
     * {@link #tokenizeTemplateHoleInterior}) instead of duplicating this chain. Pure refactor --
     * {@link #tokenize}'s own loop is now just {@code while(pos < length) tokenizeOneUnit(tokens);
     * if(syntaxError) break;}, byte-identical control flow to before this extraction.
     */
    private void tokenizeOneUnit(final List<Token> tokens)
    {
        final char c = source.charAt(pos);

        if(c == '\r' || c == '\n') {
            addToken( tokens, emitNewline() );
            atLineStart = true;
            return;
        }
        if(c == ' ' || c == '\t') {
            addToken( tokens, emitWhitespace() );
            return;
        }

        final Token t;
        if( pos == 0 && c == '#' && peek(1) == '!' ) {
            t = emitShebangLine();
        }
        else if( isPreprocessorLanguage() && c == '#' && atLineStart ) {
            t = emitPreprocessorOrDefine();
        }
        else if( c == '/' && peek(1) == '/' ) {
            t = emitLineComment();
        }
        else if( c == '/' && peek(1) == '*' ) {
            t = emitBlockComment();
        }
        else if( c == '/' && (lang.isJs || lang.isTs) && isRegexLiteralAllowedHere(tokens) ) {
            t = emitRegexLiteral();
        }
        else if( c == '"' && isTextBlockOpener() ) {
            t = emitTextBlock();
        }
        else if( c == '"' && isKotlinRawStringOpener() ) {
            t = emitKotlinRawString();
        }
        else if( (lang.isC || lang.isCpp) && rawStringPrefixLength() >= 0 ) {
            t = emitRawString( rawStringPrefixLength() );
        }
        else if(c == '"') {
            t = emitString();
        }
        else if( c == '`' && (lang.isJs || lang.isTs) ) {
            emitTemplateLiteral(tokens);
            atLineStart = false;
            return;
        }
        else if(c == '`' && lang.isKotlin) {
            t = emitKotlinBacktickIdentifier();
        }
        else if(c == '\'') {
            t = emitChar();
        }
        else if( Character.isDigit(c) || ( c == '.' && Character.isDigit( peek(1) ) ) ) {
            t = emitNumber();
        }
        else if( isIdentifierStart(c) ) {
            t = emitIdentifierOrKeyword();
        }
        else if( c == '[' && peek(1) == '[' && lang.isCpp && looksLikeAttributeOpen() ) {
            t = emitOperator();
        }
        else if( c == ']' && peek(1) == ']' && lang.isCpp ) {
            // C++11 attribute close (`]]` closing `[[nodiscard]]` etc, STYLE_CPP26.md §5).
            // Must stay C++-only like the "[[" branch just above it -- outside C++, two
            // adjacent `]` are unrelated closes (e.g. TS `{ [K in T[number]]?: unknown }`'s
            // indexed-access-type close immediately followed by the mapped-type bracket's own
            // close) and must each go through the ordinary emitCloseBracket() PUNCT path below,
            // not emitOperator() -- even after gating MULTI_CHAR_OPS's own "]]" entry to C++
            // only (see emitOperator()), reaching emitOperator() at all here still emits an OP
            // token instead of a PUNCT one, which defeats every isPunct(t, "]") check the same
            // way (vuejs/core dogfood, componentOptions.ts's InjectToObject mapped type).
            t = emitOperator();
        }
        else if( c == '[' && peek(1) == ':' && lang.isCpp ) {
            t = emitOperator();
        }
        else if(c == '{') {
            t = emitOpenBrace();
        }
        else if(c == '}') {
            t = emitCloseBrace();
        }
        else if(c == '(' || c == '[') {
            t = emitOpenBracket(c);
        }
        else if(c == ')' || c == ']') {
            t = emitCloseBracket(c);
        }
        else if(c == ';' || c == ',') {
            t = emitPunct(c);
        }
        else {
            t = emitOperator();
        }

        addToken(tokens, t);
        atLineStart = false;
    }

    private void addToken(final List<Token> tokens, final Token t)
    {
        tokens.add(t);
        trackSignificant(t);
    }

    private void trackSignificant(final Token t)
    {
        switch(t.type) {

            case WHITESPACE: /* FALL-THROUGH */
            case NEWLINE: /* FALL-THROUGH */
            case COMMENT_LINE: /* FALL-THROUGH */
            case COMMENT_BLOCK: /* FALL-THROUGH */
            case PREPROCESSOR:
                return;

            default:
                if( t.type == TokenType.OP && "=".equals(t.text) ) {
                    final String conceptName = computeConceptHeaderName();
                    if(conceptName != null) pendingConceptName = conceptName;
                }
                else if( t.type == TokenType.PUNCT && ";".equals(t.text) ) {
                    pendingConceptName        = null;
                    pendingNamedConstructName = null;
                    namedConstructKeywordSeen = false;
                }
                else if( t.type == TokenType.PUNCT && "{".equals(t.text) ) {
                    namedConstructKeywordSeen = false;
                }
                else if( t.type == TokenType.PUNCT && "}".equals(t.text) ) {
                    namedConstructKeywordSeen = false;
                }
                else if( t.type == TokenType.OP && ":".equals(
                    t.text
                ) && namedConstructKeywordSeen ) {
                    // A named-construct keyword directly followed by `:` with no identifier in
                    // between (Kotlin's anonymous `object : Comparable<Int> {`) has no name to
                    // arm -- without this, the next IDENTIFIER found (the supertype name,
                    // `Comparable`) would be wrongly captured as the construct's own name. Never
                    // fires for C/C++/Java: their named constructs always require a name token
                    // before any inheritance-clause `:`, so namedConstructKeywordSeen is already
                    // false (cleared by the arm-on-IDENTIFIER branch below) by the time a `:` is
                    // reached in those languages.
                    namedConstructKeywordSeen = false;
                }
                else if( t.type == TokenType.PUNCT && "(".equals(t.text) && t.parenDepth == 1
                        && !lang.isKotlin ) {
                    // Entering the outermost paren group (a function's parameter list) -- clear
                    // pendingNamedConstructName so the function body's `{` doesn't pick up the
                    // surrounding class/struct name.
                    // Gated off for Kotlin: a Kotlin class/enum-class's primary constructor
                    // parameter list (`class Foo(val x: Int) {`) has this exact same
                    // keyword-IDENTIFIER-`(` shape, but the `(` there sits between the just-armed
                    // construct name and its own body brace, not a separate function's parameter
                    // list -- clearing here would wrongly leave that class/enum-class body brace
                    // nameless (no closing comment, no name-driven blank lines). Never fires for
                    // C/C++/Java either way (this is a Kotlin-only case: their named constructs
                    // never have a parenthesized list directly after the name before `{`, aside
                    // from Java's `record`, which already survives via the separate
                    // `pendingRecordName` field, never touching `pendingNamedConstructName`).
                    pendingNamedConstructName = null;
                }
                else if( t.type == TokenType.KEYWORD && namedConstructKeywords.contains(t.text)
                        && !"concept".equals(t.text)
                        && !( lang.isKotlin && "class".equals(
                            t.text
                        ) && isPrecededByDoubleColon() ) ) {
                    // Kotlin class-literal reflection expressions (`Foo::class`) tokenize `class`
                    // as a KEYWORD, but it is not introducing a new construct here -- it's a
                    // property-like reference to the `Foo` class's KClass object. Arming
                    // namedConstructKeywordSeen here would let the *next* IDENTIFIER anywhere later
                    // in the file (there being no `{`/`}`/`;` in between for a bare expression like
                    // `TypeAliases::class` used as a whole statement's RHS) get wrongly captured as
                    // pendingNamedConstructName, corrupting an unrelated later scope's name (e.g. a
                    // following `.first { ... }` lambda wrongly treated as a named construct).
                    namedConstructKeywordSeen = true;
                }
                recentSignificant.addLast(t);
                if( recentSignificant.size() > 3 ) recentSignificant.removeFirst();
                // Arm pendingNamedConstructName when IDENTIFIER follows a named-construct keyword.
                // namedConstructKeywordSeen survives attribute-specifiers like `alignas(16)` that
                // sit between the keyword and the name; the old arr[n-2] check only caught the
                // direct keyword→identifier case.
                if(t.type == TokenType.IDENTIFIER && t.parenDepth == 0 && namedConstructKeywordSeen) {
                    pendingNamedConstructName = t.text;
                    namedConstructKeywordSeen = false;
                }
                else if( t.type == TokenType.IDENTIFIER && t.parenDepth == 0
                        && pendingNamedConstructName != null && recentSignificant.size() >= 2 ) {
                    // Qualified namespace name (`namespace alpha::beta::gamma {`): each further
                    // `::identifier` segment after the first extends the already-armed name
                    final Token[] arr  = recentSignificant.toArray( new Token[0] );
                    final Token   prev = arr[arr.length - 2];
                    if( prev.type == TokenType.OP && "::".equals(prev.text) ) {
                        pendingNamedConstructName = pendingNamedConstructName + "::" + t.text;
                    }
                    else if(prev.type == TokenType.IDENTIFIER) {
                        // Elaborated-type variable declaration (`struct sigaction sa = { };`):
                        // the first identifier after `struct` is a previously-declared type tag,
                        // not a new construct name, and this second identifier is the variable
                        // being declared -- the `{` that may follow is that variable's aggregate
                        // initializer, not a construct body
                        pendingNamedConstructName = null;
                    }
                }

        } // switch
    }

    /**
     * True iff the most recent significant token tracked so far is the {@code ::} operator --
     * used to recognize Kotlin class-literal expressions ({@code Foo::class}) so the {@code class}
     * keyword there is not mistaken for the start of an actual class declaration
     */
    private boolean isPrecededByDoubleColon()
    {
        if( recentSignificant.isEmpty() ) return false;
        final Token prev = recentSignificant.peekLast();

        return prev.type == TokenType.OP && "::".equals(prev.text);
    }

    /**
     * True iff the last two significant tokens (before the `=` currently being tracked) are the
     * `concept` keyword followed by its declared name -- arms {@code pendingConceptName}, the
     * `concept` analog of {@link #computeRecordHeaderName}
     */
    private String computeConceptHeaderName()
    {
        final Token[] arr = recentSignificant.toArray( new Token[0] ); // Oldest..newest
        final int     n   = arr.length;
        if( n < 2 || !namedConstructKeywords.contains("concept") ) return null;
        final Token kw   = arr[n - 2];
        final Token name = arr[n - 1];
        if( kw.type == TokenType.KEYWORD && "concept".equals(
            kw.text
        ) && name.type == TokenType.IDENTIFIER ) return name.text;

        return null;
    }

    // ── Named construct detection (for the `{` name stack) ─────────────────────────
    private String computeConstructName()
    {
        final Token[] arr = recentSignificant.toArray( new Token[0] ); // Oldest..newest
        final int     n   = arr.length;
        if(n >= 2) {
            final Token kw   = arr[n - 2];
            final Token name = arr[n - 1];
            if( "extern".equals(
                kw.text
            ) && kw.type == TokenType.KEYWORD && name.type == TokenType.STRING && "\"C\"".equals(
                name.text
            ) ) return kw.text + " " + name.text;
            if( kw.type == TokenType.KEYWORD && namedConstructKeywords.contains(
                kw.text
            ) && name.type == TokenType.IDENTIFIER ) return name.text;
        } // if
        if(n >= 3) {
            final Token enumKw  = arr[n - 3];
            final Token classKw = arr[n - 2];
            final Token name    = arr[n - 1];
            if( "enum".equals(
                enumKw.text
            ) && "class".equals(
                classKw.text
            ) && namedConstructKeywords.contains(
                "enum"
            ) && name.type == TokenType.IDENTIFIER ) return name.text;
        } // if

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
    private String computeRecordHeaderName()
    {
        final Token[] arr = recentSignificant.toArray( new Token[0] ); // Oldest..newest
        final int     n   = arr.length;
        if( n < 2 || !namedConstructKeywords.contains("record") ) return null;
        final Token kw   = arr[n - 2];
        final Token name = arr[n - 1];
        if( kw.type == TokenType.KEYWORD && "record".equals(
            kw.text
        ) && name.type == TokenType.IDENTIFIER ) return name.text;

        return null;
    }

    // ── Per-construct emit helpers ──────────────────────────────────────────────────
    private Token emitOpenBrace()
    {
        final String name;
        if(pendingRecordName != null) {
            name              = pendingRecordName;
            pendingRecordName = null;
        }
        else if(pendingConceptName != null) {
            name               = pendingConceptName;
            pendingConceptName = null;
        }
        else if(pendingNamedConstructName != null) {
            name                      = pendingNamedConstructName;
            pendingNamedConstructName = null;
        }
        else {
            name = computeConstructName();
        }
        ++braceDepth;
        nameStack.push(name);
        ++pos;

        return new Token(TokenType.PUNCT, "{", braceDepth, parenDepth, name);
    }

    private Token emitCloseBrace()
    {
        if(braceDepth == 0) syntaxError = true;
        final String name = nameStack.isEmpty() ? null : nameStack.pop();
        --braceDepth;
        ++pos;

        return new Token(TokenType.PUNCT, "}", braceDepth, parenDepth, name);
    }

    private Token emitOpenBracket(final char c)
    {
        bracketNameStack.push( c == '(' ? computeRecordHeaderName() : null );
        ++parenDepth;
        ++pos;

        return new Token( TokenType.PUNCT, String.valueOf(c), braceDepth, parenDepth, null );
    }

    private Token emitCloseBracket(final char c)
    {
        final String recordName = bracketNameStack.isEmpty() ? null : bracketNameStack.pop();
        if(c == ')' && recordName != null) pendingRecordName = recordName;
        --parenDepth;
        ++pos;

        return new Token( TokenType.PUNCT, String.valueOf(c), braceDepth, parenDepth, null );
    }

    private Token emitPreprocessorOrDefine()
    {
        int p = pos + 1; // Skip '#'
        while( p < length && ( source.charAt(p) == ' ' || source.charAt(p) == '\t' ) ) p++;
        final int wordStart = p;
        while( p < length && isIdentifierPart( source.charAt(p) ) ) p++;
        final String directive = source.substring(wordStart, p);

        if( "define".equals(
            directive
        ) ) return isMultilineDirective(
            p
        ) ? emitMacroDef() : emitSingleLineDefine();

        return emitPreprocessor();
    }

    private boolean isMultilineDirective(final int from)
    {
        int p = from;
        while( p < length && source.charAt(p) != '\n' && source.charAt(p) != '\r' ) p++;
        int q = p - 1;
        while( q >= from && ( source.charAt(q) == ' ' || source.charAt(q) == '\t' ) ) q--;

        return q >= from && source.charAt(q) == '\\';
    }

    private Token emitSingleLineDefine()
    {
        ++pos; // '#'
        while( pos < length && ( source.charAt(pos) == ' ' || source.charAt(pos) == '\t' ) ) pos++;
        pos += "define".length();
        while( pos < length && ( source.charAt(pos) == ' ' || source.charAt(pos) == '\t' ) ) pos++;
        final int nameStart = pos;
        while( pos < length && isIdentifierPart( source.charAt(pos) ) ) pos++;
        final String name = source.substring(nameStart, pos);

        String paramList = "";
        if( pos < length && source.charAt(pos) == '(' ) {
            final int paramStart = pos;
                  int depth      = 0;
            do {
                final char c = source.charAt(pos);
                     if(c == '(') depth++;
                else if(c == ')') depth--;
                ++pos;
            } while(pos < length && depth > 0);
            paramList = source.substring(paramStart, pos);
        } // if

        final int restStart = pos;
        while( pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r' ) pos++;
        final String rest = source.substring(restStart, pos);

        final String text = "#define " + name + paramList + rest;

        return new Token(TokenType.PREPROCESSOR, text, braceDepth, parenDepth, null);
    }

    private Token emitMacroDef()
    {
        final int start = pos;
        while(true) {
            while( pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r' ) pos++;
            int q = pos - 1;
            while( q >= start && ( source.charAt(q) == ' ' || source.charAt(q) == '\t' ) ) q--;
            final boolean continues = q >= start && source.charAt(q) == '\\';
            if(!continues || pos >= length) break;
            if( source.charAt(pos) == '\r' ) {
                ++pos;
                if( pos < length && source.charAt(pos) == '\n' ) pos++;
            }
            else {
                ++pos;
            }
        } // while

        return new Token(
            TokenType.MACRO_DEF, source.substring(start, pos), braceDepth,
            parenDepth, null
        );
    }

    /**
     * Like {@link #emitMacroDef}, a {@code #if}/{@code #elif}/etc. directive can itself span
     * multiple physical lines via a trailing {@code \} continuation (e.g. a long boolean
     * condition) -- failing to consume those continuation lines here left their real `(`/`)`
     * tokens to be lexed as ordinary PUNCT by the caller, permanently desyncing every
     * brace/paren-depth counter for the remainder of the file from that point on.
     */
    private Token emitPreprocessor()
    {
        final int start = pos;
        while(true) {
            while( pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r' ) pos++;
            int q = pos - 1;
            while( q >= start && ( source.charAt(q) == ' ' || source.charAt(q) == '\t' ) ) q--;
            final boolean continues = q >= start && source.charAt(q) == '\\';
            if(!continues || pos >= length) break;
            if( source.charAt(pos) == '\r' ) {
                ++pos;
                if( pos < length && source.charAt(pos) == '\n' ) pos++;
            }
            else {
                ++pos;
            }
        } // while

        return new Token(
            TokenType.PREPROCESSOR, source.substring(start, pos), braceDepth, parenDepth, null
        );
    }

    private Token emitLineComment()
    {
        final int start = pos;
        pos += 2;
        while( pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r' ) pos++;

        return new Token(
            TokenType.COMMENT_LINE, source.substring(start, pos), braceDepth,
            parenDepth, null
        );
    }

    // A shebang line (`#!...`) is only ever valid as the file's first two characters -- emitted
    // as its own opaque SHEBANG type (a gap token, per Token.isGapToken) rather than COMMENT_LINE
    // so no rule ever tokenizes its `/`-separated path segments as JS/TS division operators and
    // appends a stray statement-terminator semicolon, and so comment-normalization passes that
    // assume every COMMENT_LINE token's text starts with a literal `//` (e.g. MiscRuleCore
    // .enforceCommentStyle) never mangle it into `///usr/bin/env node` (found 2026-07-30
    // formatting tools/verifiers/*.js -- see STATE_AI.md's 2026-07-30 section).
    private Token emitShebangLine()
    {
        final int start = pos;
        while( pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r' ) pos++;

        return new Token(
            TokenType.SHEBANG, source.substring(start, pos), braceDepth,
            parenDepth, null
        );
    }

    private Token emitBlockComment()
    {
        final int start = pos;
        pos += 2;
        // Kotlin, unlike C/C++/Java, allows block comments to nest (`/* ... /* ... */ ... */`) --
        // a doc-comment code example containing its own literal `/* ... */` snippet (e.g.
        // kotlinx.coroutines's Guidance.kt KDoc) is valid Kotlin and must not have the outer
        // comment close at that inner `*/`. Track nesting depth for Kotlin only; C/C++/Java block
        // comments still close at the first `*/`, matching those languages' real grammar.
        if(lang.isKotlin) {
            int depth = 1;
            while(pos < length && depth > 0) {
                if( source.charAt(pos) == '/' && peek(1) == '*' ) {
                    ++depth;
                    pos += 2;
                }
                else if( source.charAt(pos) == '*' && peek(1) == '/' ) {
                    --depth;
                    pos += 2;
                }
                else {
                    ++pos;
                }
            } // while
        } // if
        else {
            while( pos < length && !( source.charAt(pos) == '*' && peek(1) == '/' ) ) pos++;
            if(pos < length) pos += 2;
        }

        return new Token(
            TokenType.COMMENT_BLOCK, source.substring(start, pos), braceDepth,
            parenDepth, null
        );
    }

    /**
     * True iff {@code pos} sits on the opening `"""` of a Java text block (STYLE_JAVA17.md §4) --
     * three consecutive `"` characters, Java only ({@code emitString}'s plain-string path already
     * bails on a bare `"` followed by a newline before finding its own closing quote, which is
     * exactly what would otherwise happen here: without this check, a text block's opening `"""`
     * mis-lexes as an empty string token followed by a single stray-quote token, exposing the
     * block's entire multi-line content -- braces, indentation, everything -- to every other rule
     * in the pipeline).
     */
    private boolean isTextBlockOpener()
    {
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
    private Token emitTextBlock()
    {
        final int start = pos;
        pos += 3;
        while(pos < length) {
            final char c = source.charAt(pos);
            if(c == '\\' && pos + 1 < length) {
                pos += 2;
                continue;
            }
            if( c == '"' && peek(1) == '"' && peek(2) == '"' ) {
                pos += 3;
                break;
            }
            ++pos;
        } // while

        return new Token(
            TokenType.STRING, source.substring(start, pos), braceDepth, parenDepth,
            null
        );
    }

    /**
     * True iff {@code pos} sits on the opening `"""` of a Kotlin raw string. Neither
     * STYLE_KOTLIN.md nor STYLE_KOTLIN2.md mentions raw strings at all -- surfaced as a
     * side-finding while fixing §19's interpolation-nesting risk (RDD_KEY_116) and confirmed via
     * harness to be badly broken: without this check, `"""hello "world" end"""` mis-lexed as five
     * tokens (`""` / `"hello "` / `world` (a bare `IDENTIFIER`!) / `" end"` / `""`) instead of one,
     * and a multi-line raw string mis-lexed a spurious `NEWLINE` token into the middle of what
     * should be one opaque string -- exposing the content's real newlines to every indentation/
     * scope pass in the pipeline exactly the way an unrecognized text block would.
     */
    private boolean isKotlinRawStringOpener()
    {
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
     *   <li>Termination matches the real Kotlin compiler's actual rule, confirmed against
     *       `kotlin_syntax_check` (RDD_KEY_212, C6k-2): a raw string is free to contain runs of one
     *       or two unescaped `"` characters as plain content (e.g. `"""hello "world" end"""`), and
     *       when a run of *three or more* quotes is encountered, the closing delimiter is the
     *       final three quotes of that whole run -- any earlier quotes in the same run are plain
     *       content, not a premature close. E.g. four trailing quotes (`"""abc""""`) closes after
     *       all four (content `abc"`, delimiter the last three), not after the first three the way
     *       an earlier version of this method assumed -- that earlier "greedy on the first `"""`"
     *       reading was simply wrong (verified: `kotlinc` accepts `"""abc"""".trimMargin()` with no
     *       syntax error, which is only possible if the closing three quotes are the *last* three of
     *       the run).</li>
     * </ul>
     * `${...}` interpolation is still recognized and depth-tracked via {@link
     * #skipKotlinInterpolationBlock} exactly as in {@link #skipKotlinString} -- interpolation
     * works identically inside a raw string, including a nested raw string inside the
     * interpolation expression itself ({@code "${"""nested"""}"}), which {@code
     * skipKotlinInterpolationBlock} now dispatches back into this same method for.
     */
    private int skipKotlinRawString(final int openIdx)
    {
        int p = openIdx + 3;
        while(p < length) {
            final char c = source.charAt(p);
            if( c == '"' && p + 2 < length && source.charAt(
                p + 1
            ) == '"' && source.charAt(
                p + 2
            ) == '"' ) {
                // Found a run of >= 3 quotes; extend through the whole run -- the closing
                // delimiter is the *last* three quotes of it, so any additional quotes beyond
                // the first three are still part of the string, not a premature close
                int q = p;
                while( q < length && source.charAt(q) == '"' ) q++;
                return q;
            } // if
            if( c == '$' && p + 1 < length && source.charAt(p + 1) == '{' ) {
                p = skipKotlinInterpolationBlock(p + 2);
                continue;
            }
            ++p;
        } // while

        return p;
    }

    private Token emitKotlinRawString()
    {
        final int start = pos;
        pos = skipKotlinRawString(pos);

        return new Token(
            TokenType.STRING, source.substring(start, pos), braceDepth, parenDepth,
            null
        );
    }

    private static final String[] RAW_STRING_PREFIXES = { "u8R", "uR", "UR", "LR", "R" };

    /**
     * C++11 raw string literals (`R"delim(...)delim"`, optionally prefixed by an encoding
     * prefix `u8`/`u`/`U`/`L`) can contain arbitrary characters -- including `{`/`}` -- as plain
     * content (nanobench's mustache HTML templates are stored this way). Without recognizing the
     * whole thing as one opaque token, {@code emitIdentifierOrKeyword} + {@code emitString} would
     * lex the prefix, the raw delimiter, and the literal's contents as ordinary source, exposing
     * any brace characters inside to the brace-depth tracker that every scope-splitting pass
     * relies on -- silently corrupting nesting depth for the rest of the file. Returns the
     * prefix length (`"R"` = 1, `"u8R"` = 3, ...) if {@code pos} sits on a genuine raw string
     * opener (prefix + `"` + a valid delimiter of at most 16 chars with no whitespace/paren/
     * backslash + `(`), else -1.
     */
    private int rawStringPrefixLength()
    {
        for(final String prefix : RAW_STRING_PREFIXES) {
            if( !source.startsWith(prefix, pos) ) continue;
            int p = pos + prefix.length();
            if( p >= length || source.charAt(p) != '"' ) continue;
            ++p;
            final int delimStart = p;
            while( p < length && p - delimStart <= 16 && source.charAt(p) != '(' ) {
                final char dc = source.charAt(p);
                if(dc == ' ' || dc == '\t' || dc == '\n' || dc == '\r' || dc == '\\' || dc == ')') break;
                ++p;
            }
            if( p < length && source.charAt(p) == '(' ) return prefix.length();
        } // for

        return -1;
    }

    /**
     * Lexes a raw string literal as a single opaque STRING token, from the encoding/`R` prefix
     * through the closing `)delim"` -- content in between (including any `{`/`}`/`"` chars) is
     * never re-examined by the brace-depth tracker or any other rule (same "opaque, own text,
     * never split" precedent as {@link #emitBlockComment}/{@link #emitTextBlock}). An unterminated
     * raw string (no matching `)delim"` before EOF) is consumed to the end of the source.
     */
    private Token emitRawString(final int prefixLen)
    {
        final int start = pos;
        pos += prefixLen + 1; // prefix + opening `"`
        final int delimStart = pos;
        while( pos < length && source.charAt(pos) != '(' ) pos++;
        final String delim  = source.substring(delimStart, pos);
        final String closer = ")" + delim + "\"";
        if(pos < length) pos++; // `(`
        final int closerIdx = source.indexOf(closer, pos);
        if(closerIdx < 0) pos = length;
        else              pos = closerIdx + closer.length();

        return new Token(
            TokenType.STRING, source.substring(start, pos), braceDepth, parenDepth,
            null
        );
    }

    private Token emitString()
    {
        final int start = pos;
        if(lang.isKotlin) {
            pos = skipKotlinString(pos);
        }
        else {
            ++pos;
            while(pos < length) {
                final char c = source.charAt(pos);
                if(c == '\\' && pos + 1 < length) {
                    // A backslash-escaped CRLF line continuation must consume both the `\r`
                    // and the following `\n` as one escaped unit -- consuming only the `\r`
                    // (as the generic 2-char skip below would) leaves the `\n` as the very
                    // next character examined, which the `c == '\n'` check right below
                    // mistakes for an unescaped newline terminating the string
                    if( source.charAt(
                        pos + 1
                    ) == '\r' && pos + 2 < length && source.charAt(
                        pos + 2
                    ) == '\n' ) pos += 3;
                    else pos += 2;
                    continue;
                } // if
                if(c == '"') {
                    ++pos;
                    break;
                }
                if(c == '\n' || c == '\r') break;
                ++pos;
            } // while
        }

        return new Token(
            TokenType.STRING, source.substring(start, pos), braceDepth, parenDepth,
            null
        );
    }

    /**
     * JS/TS template literal (STYLE_JS_TS.md §4, backtick-delimited, gated on lang.isJs/isTs in
     * the dispatch loop -- no other family uses `` ` `` at all, so this is purely additive).
     * Dispatches to one of two token shapes (STATE_JS_TS.md's 2026-08-13 scoping session,
     * sub-context 0/3): a plain {@code .js}/{@code .ts} file keeps the original single-opaque-
     * STRING-token shape ({@link #emitTemplateLiteralOpaque}, byte-for-byte unchanged from
     * before this session), while a {@code .jsx}/{@code .tsx} file ({@code lang.isJsxSyntax})
     * gets the new segmented shape ({@link #emitTemplateLiteralSegmented}) that exposes each
     * {@code ${...}} hole's boundary as its own token pair -- narrow/gated, mirroring the
     * {@code isRegexLiteralAllowedHere} `.jsx`/`.tsx`-only carve-out precedent, so this change
     * carries zero regression risk for the 200+ non-JSX `.js`/`.ts` fixtures and every real-code
     * dogfood corpus already validated against the opaque shape.
     */
    private void emitTemplateLiteral(final List<Token> tokens)
    {
        if(!lang.isJsxSyntax) {
            addToken( tokens, emitTemplateLiteralOpaque() );
            return;
        }
        emitTemplateLiteralSegmented(tokens);
    }

    /** Original single-opaque-STRING-token shape, unchanged. See {@link #emitTemplateLiteral}. */
    private Token emitTemplateLiteralOpaque()
    {
        final int start = pos;
        ++pos; // Consume opening `
        while(pos < length) {
            final char c = source.charAt(pos);
            if(c == '\\') {
                pos += 2;
                continue;
            }
            if(c == '`') {
                ++pos;
                return new Token(
                    TokenType.STRING, source.substring(start, pos), braceDepth,
                    parenDepth, null
                );
            } // if
            if( c == '$' && peek(1) == '{' ) {
                pos += 2;
                skipTemplateInterpolation();
                continue;
            }
            ++pos;
        } // while
        syntaxError = true;

        return new Token(
            TokenType.STRING, source.substring(start, pos), braceDepth, parenDepth,
            null
        );
    }

    /**
     * `.jsx`/`.tsx`-only segmented template-literal shape (STATE_JS_TS.md's 2026-08-13 scoping
     * session, sub-context 0/1). Instead of one opaque STRING token for the whole literal, emits a
     * sequence of tokens that reconstructs the exact same source text: a STRING segment for each
     * run of literal text, a {@code PUNCT "${"} / {@code PUNCT "}"} pair bracketing each
     * interpolation hole (STATE_JS_TS.md sub-context 1's chosen representation, option (a): reuse
     * plain PUNCT rather than a new dedicated TokenType), and the hole's own interior tokens in
     * between -- see {@link #emitTemplateHoleInterior}. No new return-shape convention needed
     * beyond what {@link #tokenizeOneUnit} already established for this call site: this method
     * pushes tokens directly via {@link #addToken} (the {@code void}, direct-push option
     * STATE_JS_TS.md's sub-context 0 write-up called out as the decision to make) rather than
     * returning a spliced {@code List<Token>} -- there is no existing multi-token-emission
     * precedent elsewhere in this tokenizer to match, so the simpler of the two options was taken.
     */
    private void emitTemplateLiteralSegmented(final List<Token> tokens)
    {
        int segStart = pos;
        ++pos; // Consume opening `
        while(pos < length) {
            final char c = source.charAt(pos);
            if(c == '\\') {
                pos += 2;
                continue;
            }
            if(c == '`') {
                ++pos;
                addToken(
                    tokens, new Token( TokenType.STRING, source.substring(segStart, pos), braceDepth, parenDepth, null )
                );
                return;
            } // if
            if( c == '$' && peek(1) == '{' ) {
                addToken(
                    tokens, new Token( TokenType.STRING, source.substring(segStart, pos), braceDepth, parenDepth, null )
                );
                pos += 2;
                addToken(
                    tokens,
                    new Token(TokenType.TEMPLATE_HOLE_OPEN, "${", braceDepth, parenDepth, null)
                );
                emitTemplateHoleInterior(tokens);
                segStart = pos;
                continue;
            } // if
            ++pos;
        } // while
        syntaxError = true;
        addToken(
            tokens, new Token( TokenType.STRING, source.substring(segStart, pos), braceDepth, parenDepth, null )
        );
    }

    /**
     * Tokenizes (or, until sub-context 1 lands, opaquely captures) a {@code ${...}} hole's
     * interior for {@link #emitTemplateLiteralSegmented}, then emits the closing {@code PUNCT "}"}
     * token. `pos` is positioned just after the opening `${` on entry.
     *
     * <p><b>Sub-context 1 (LANDED):</b> re-enters the exact same per-character dispatch the
     * top-level scan uses ({@link #tokenizeOneUnit}, called in a loop here) rather than merely
     * skipping raw characters -- this is what makes a JSX element inside a hole reachable by
     * {@link #findJsxSpans}'s post-tokenize pass at all. A local {@code holeDepth} counter (mirrors
     * {@link #skipTemplateInterpolation}'s own depth tracking) distinguishes the hole's own
     * terminating {@code }} from a nested one belonging to a real object-literal/block-statement
     * inside the interpolation expression -- the terminator is consumed directly here (never routed
     * through {@link #emitCloseBrace}, which would wrongly decrement the tokenizer's *global*
     * {@code braceDepth} for a scope that was never opened via {@link #emitOpenBrace} in the first
     * place); every other {@code {}}/`(`/`[`/string/nested-template construct inside the hole goes
     * through the ordinary dispatch and so participates in global brace/paren-depth tracking exactly
     * like the same construct anywhere else in the file. A nested backtick inside the hole re-enters
     * {@link #emitTemplateLiteral} itself via {@link #tokenizeOneUnit}'s own dispatch (not a
     * separate, simpler path), which is what makes nested template literals (sub-context 2) fall out
     * of this structure for free rather than needing their own special case.
     */
    private void emitTemplateHoleInterior(final List<Token> tokens)
    {
        int holeDepth = 1;
        while(pos < length) {
            final char c = source.charAt(pos);
            if(c == '}') {
                --holeDepth;
                if(holeDepth == 0) {
                    ++pos; // Consume the hole's own closing `}`
                    addToken(
                        tokens,
                        new Token(TokenType.TEMPLATE_HOLE_CLOSE, "}", braceDepth, parenDepth, null)
                    );
                    return;
                } // if
                // A genuine nested `}` (e.g. closing an object literal/block inside the
                // interpolation expression) -- fall through to ordinary dispatch below, which
                // correctly decrements the tokenizer's global braceDepth via emitCloseBrace.
            } // if
            else if(c == '{') {
                ++holeDepth;
                // Falls through to ordinary dispatch, which increments global braceDepth via
                // emitOpenBrace -- holeDepth tracks the same nesting independently, purely to
                // recognize this hole's own terminator
            }
            tokenizeOneUnit(tokens);
            if(syntaxError) return;
        } // while
        // Unterminated hole (EOF before the matching `}`) -- same posture as the rest of this
        // tokenizer's unterminated-construct handling
        syntaxError = true;
    }

    /**
     * Kotlin backtick-quoted identifier (STYLE_KOTLIN.md, e.g. the JetBrains test-name idiom
     * {@code fun `parses correctly (edge case)`() { ... }}). Gated on {@code lang.isKotlin} --
     * no other family uses backtick-escaped identifiers. C6g fix: prior to this, a Kotlin
     * backtick span wasn't recognized as opaque text at all (no {@code c == '`'} branch matched
     * for Kotlin in the dispatch loop above, unlike JS/TS's template-literal branch just above
     * this one), so the dispatch loop fell through to {@code emitOperator()} for the backtick
     * itself and then re-tokenized the identifier's *interior* character-by-character -- any
     * literal {@code (}/{@code )} embedded in the name (a common JetBrains test-name shape) was
     * emitted as a real {@code emitOpenBracket}/{@code emitCloseBracket} token, corrupting
     * every downstream paren-depth-tracking pass (signature-boundary detection, call/decl
     * rendering, etc.) the same way an un-masked string/comment span historically has. Fix:
     * treat the whole backtick span as a single opaque token here, the same way
     * {@code emitTemplateLiteral} already does for JS/TS template literals just above -- content
     * preserved byte-for-byte, no interior re-tokenization. Emitted as {@code IDENTIFIER} (not
     * {@code STRING}) since semantically this *is* an identifier -- e.g. a bare backtick-quoted
     * function name must still be recognized as the declaration's name token by callers that
     * check {@code TokenType.IDENTIFIER}. Kotlin backtick identifiers are single-line (a bare
     * newline is not legal inside one); an unterminated span (missing closing backtick before
     * EOF/newline) sets {@code syntaxError} and returns whatever was scanned, same posture as
     * {@code emitTemplateLiteral}'s own EOF fallback.
     */
    private Token emitKotlinBacktickIdentifier()
    {
        final int start = pos;
        ++pos; // Consume opening `
        while(pos < length) {
            final char c = source.charAt(pos);
            if(c == '`') {
                ++pos;
                return new Token(
                    TokenType.IDENTIFIER, source.substring(start, pos), braceDepth,
                    parenDepth, null
                );
            } // if
            if(c == '\n' || c == '\r') break;
            ++pos;
        } // while
        syntaxError = true;

        return new Token(
            TokenType.IDENTIFIER, source.substring(start, pos), braceDepth,
            parenDepth, null
        );
    }

    /**
     * JS/TS regex-literal vs. division-operator disambiguation at a bare {@code /}. The classic
     * heuristic: a `/` starts a regex literal unless the last significant (non-gap) token already
     * completed a value expression (identifier, number, string/template/regex literal, or a
     * closing `)`/`]`/`}` -- e.g. `a / b`, `arr[0] / 2`, `f() / 2`) or is a keyword that itself
     * denotes a value (`this`, `super`) rather than one that expects an operand to follow. Absent
     * any prior significant token (start of file/statement) or after any operator/punctuation/
     * keyword that expects an expression next (`return`, `=`, `(`, `,`, `&&`, etc.), `/` begins a
     * regex literal instead. Deliberately conservative: `++`/`--` (postfix on a value) are the
     * only OP tokens treated like a value-completing token, matching real JS semantics (`x++ / 2`
     * is division, not `x++` followed by a regex).
     */
    private boolean isRegexLiteralAllowedHere(final List<Token> tokens)
    {
        int i = tokens.size() - 1;
        while( i >= 0 && Token.isGapToken( tokens.get(i) ) ) i--;
        if(i < 0) return true;
        final Token last = tokens.get(i);
        switch(last.type) {

            case IDENTIFIER: /* FALL-THROUGH */
            case NUMBER: /* FALL-THROUGH */
            case STRING: /* FALL-THROUGH */
            case CHAR:
                return false;

            case KEYWORD:
                return !( "this".equals(last.text) || "super".equals(last.text) );

            case PUNCT:
                return !( ")".equals(last.text) || "]".equals(last.text) || "}".equals(last.text) );

            case OP:
                // `.jsx`/`.tsx` only: a `/` immediately after `<` is virtually always a JSX
                // closing tag (`</Foo>`) or the tail of a self-closing tag (`<Foo/>` -- the OP
                // token immediately before is `<`... actually the self-close case has an
                // IDENTIFIER/PUNCT before the `/`, already excluded above; this branch only fires
                // for the `</Foo>` shape), never a regex literal -- `a < /re/` (division-after-
                // less-than) is technically legal JS but vanishingly rare compared to a JSX
                // closing tag in a file that opted into JSX syntax via its extension. Without this,
                // the character-level lexer (which runs before findJsxSpans's post-tokenize pass
                // even sees the token stream) misreads `</span>`'s `/` as a regex-literal opener,
                // scanning for a second unescaped `/` and usually overrunning to end-of-line/EOF
                // with `syntaxError = true` -- corrupting the whole file's tokenization before the
                // JSX pre-pass gets a chance to run at all.
                if( lang.isJsxSyntax && "<".equals(last.text) ) return false;

                return !( "++".equals(last.text) || "--".equals(last.text) );

            default:
                return true;

        } // switch
    }

    /**
     * JS/TS regex literal (`/pattern/flags`), disambiguated from division by
     * {@link #isRegexLiteralAllowedHere}. Scans to the matching unescaped closing `/`, correctly
     * skipping over a bracketed character class (`[...]`) where an unescaped `/` does NOT
     * terminate the literal (e.g. the `/` has no special meaning inside `[...]`, though a literal
     * `/` there is rare -- the real motivating case is a `"`/`'` inside `[...]` that must NOT be
     * mistaken for the start of a string/char literal, which is exactly what happened before this
     * method existed: `/^(?:W\/)?"[^"]+"$/` in real-world test code). Emitted as an opaque
     * {@code STRING} token (same posture as {@link #emitTemplateLiteral}) -- content preserved
     * byte-for-byte, no rule currently needs to see inside a regex literal. Trailing flag letters
     * (`g`, `i`, `m`, `s`, `u`, `y`, `d`) are consumed as part of the same token. Unterminated
     * (reaches end of line without a closing `/`) is a syntax error, same posture as
     * {@link #emitTemplateLiteral} hitting EOF.
     */
    private Token emitRegexLiteral()
    {
        final int start = pos;
        ++pos; // Consume opening '/'
        boolean inCharClass = false;
        while(pos < length) {
            final char c = source.charAt(pos);
            if(c == '\\' && pos + 1 < length) {
                pos += 2;
                continue;
            }
            if(c == '\n' || c == '\r') break;
            if(c == '[') {
                inCharClass = true;
                ++pos;
                continue;
            }
            if(c == ']') {
                inCharClass = false;
                ++pos;
                continue;
            }
            if(c == '/' && !inCharClass) {
                ++pos;
                while( pos < length && Character.isLetter( source.charAt(pos) ) ) pos++;
                return new Token(
                    TokenType.STRING, source.substring(start, pos), braceDepth,
                    parenDepth, null
                );
            } // if
            ++pos;
        } // while
        syntaxError = true;

        return new Token(
            TokenType.STRING, source.substring(start, pos), braceDepth, parenDepth,
            null
        );
    }

    /**
     * Skips a `${...}` interpolation body (opening `${` already consumed), respecting nested
     * `{}` depth, nested quoted strings, and nested template literals so an interior `}`/`` ` ``/
     * quote char doesn't prematurely end the interpolation or the outer template literal.
     */
    private void skipTemplateInterpolation()
    {
        int depth = 1;
        while(pos < length && depth > 0) {
            final char c = source.charAt(pos);
            if(c == '\\') {
                pos += 2;
                continue;
            }
            if(c == '{') {
                ++depth;
                ++pos;
            }
            else if(c == '}') {
                --depth;
                ++pos;
            }
            else if(c == '"' || c == '\'') {
                skipQuotedForTemplate(c);
            }
            else if(c == '`') {
                ++pos;
                skipNestedTemplateLiteral();
            }
            else {
                ++pos;
            }
        } // while
    }

    private void skipQuotedForTemplate(final char quote)
    {
        ++pos; // Opening quote
        while(pos < length) {
            final char c = source.charAt(pos);
            if(c == '\\') {
                pos += 2;
                continue;
            }
            if(c == quote) {
                ++pos;
                return;
            }
            ++pos;
        } // while
    }

    private void skipNestedTemplateLiteral()
    {
        while(pos < length) {
            final char c = source.charAt(pos);
            if(c == '\\') {
                pos += 2;
                continue;
            }
            if(c == '`') {
                ++pos;
                return;
            }
            if( c == '$' && peek(1) == '{' ) {
                pos += 2;
                skipTemplateInterpolation();
                continue;
            }
            ++pos;
        } // while
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
    private int skipKotlinString(final int openQuoteIdx)
    {
        int p = openQuoteIdx + 1;
        while(p < length) {
            final char c = source.charAt(p);
            if(c == '\\' && p + 1 < length) {
                p += 2;
                continue;
            }
            if(c == '"') return p + 1;
            if(c == '\n' || c == '\r') return p;
            if( c == '$' && p + 1 < length && source.charAt(p + 1) == '{' ) {
                p = skipKotlinInterpolationBlock(p + 2);
                continue;
            }
            ++p;
        } // while

        return p;
    }

    /**
     * Scans from just after a `${`'s opening `{` to just past its matching `}`, skipping over any
     * nested `{`/`}` (e.g. a lambda literal passed inside the interpolation expression), string
     * literals (recursively, via {@link #skipKotlinString}), and char literals encountered along
     * the way -- so a `"`/`{`/`}` inside any of those never desynchronizes the depth count or gets
     * mistaken for this block's own closing `}`.
     */
    private int skipKotlinInterpolationBlock(final int startIdx)
    {
        int p     = startIdx;
        int depth = 1;
        while(p < length && depth > 0) {
            final char c = source.charAt(p);
            if( c == '"' && p + 2 < length && source.charAt(
                p + 1
            ) == '"' && source.charAt(
                p + 2
            ) == '"' ) {
                p = skipKotlinRawString(p);
                continue;
            }
            if(c == '"') {
                p = skipKotlinString(p);
                continue;
            }
            if(c == '\'') {
                p = skipKotlinChar(p);
                continue;
            }
            if(c == '{') {
                ++depth;
                ++p;
                continue;
            }
            if(c == '}') {
                --depth;
                ++p;
                continue;
            }
            ++p;
        } // while

        return p;
    }

    /**
     * Same scan as {@link #emitChar} but returning the end index rather than allocating a
     * {@code Token} -- used by {@link #skipKotlinInterpolationBlock} to skip a char literal
     * without misreading its own quote as structurally significant
     */
    private int skipKotlinChar(final int openQuoteIdx)
    {
        int p = openQuoteIdx + 1;
        while(p < length) {
            final char c = source.charAt(p);
            if(c == '\\' && p + 1 < length) {
                p += 2;
                continue;
            }
            if(c == '\'') return p + 1;
            if(c == '\n' || c == '\r') return p;
            ++p;
        } // while

        return p;
    }

    private Token emitChar()
    {
        final int start = pos;
        ++pos;
        while(pos < length) {
            final char c = source.charAt(pos);
            if(c == '\\' && pos + 1 < length) {
                pos += 2;
                continue;
            }
            if(c == '\'') {
                ++pos;
                break;
            }
            if(c == '\n' || c == '\r') break;
            ++pos;
        } // while

        return new Token(
            TokenType.CHAR, source.substring(start, pos), braceDepth, parenDepth,
            null
        );
    }

    private Token emitIdentifierOrKeyword()
    {
        final int start = pos;
        while( pos < length && isIdentifierPart( source.charAt(pos) ) ) pos++;
        final String    text = source.substring(start, pos);
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
    private boolean looksLikeAttributeOpen()
    {
          int depth = 0;
    final int limit = Math.min(length, pos + 200);
        for(int i = pos + 2; i < limit; ++i) {
            final char c = source.charAt(i);
            if(c == '\n' || c == '\r' || c == ';' || c == '{' || c == '}' || c == '"' || c == '\'') return false;
                 if(c == '(') depth++;
            else if(c == ')') depth--;
            else if(c == ']' && depth == 0) return i + 1 < length && source.charAt(i + 1) == ']';
        } // for

        return false;
    }

    private Token emitOperator()
    {
        // Consume a maximal run of '*'
        if( source.charAt(pos) == '*' ) {
            final int start = pos;
            while( pos < source.length() && source.charAt(pos) == '*' ) pos++;
            return new Token(
                TokenType.OP, source.substring(start, pos), braceDepth, parenDepth, null
            );
        } // if

        for(final String op : MULTI_CHAR_OPS) {
            // "?:" is Kotlin's Elvis operator (STYLE_KOTLIN.md §5) -- in JS/TS the same two
            // adjacent characters are an unrelated pair (TS's `name?: type` optional-marker `?`
            // immediately followed by an ordinary type-annotation `:`, no space between), so
            // this single MULTI_CHAR_OPS entry must not swallow them together outside Kotlin.
            if( "?:".equals(op) && !lang.isKotlin ) continue;
            // "T?::member" (nullable-type callable reference, e.g. `Array<*>?::contentEquals`)
            // is "?" (nullable-type marker) + "::" (callable reference), not elvis "?:" + ":".
            // Without this guard the greedy "?:" match above fires first (its prefix matches),
            // mis-splitting into elvis + a stray ":" token. Bail out of the "?:" match here so
            // "?" falls through to the single-char branch below, leaving "::" to be matched
            // whole on the next call.
            if( "?:".equals(op) && source.startsWith("?::", pos) ) continue;
            // "[[" / "]]" are C++11 attribute brackets (`[[nodiscard]]`, STYLE_CPP26.md §5 and
            // earlier) -- in TS a closing mapped-type indexed-access type immediately followed by
            // the mapped-type bracket's own close (`{ [K in T[number]]?: unknown }`) produces the
            // exact same two adjacent `]` characters with no C++-attribute meaning at all. Outside
            // C++, swallowing them into one "]]"/"[[" OP token desyncs every depth counter that
            // tracks `[`/`]` one-for-one (e.g. `JsTsSpecificRule.enforceSemicolonInsertion`'s
            // `depth`), which never recovers for the rest of the file (vuejs/core dogfood,
            // `componentOptions.ts`'s `InjectToObject` mapped type). Kept for C++ only, same as
            // the "?:" Kotlin-only guard immediately above.
            if( ( "[[".equals(op) || "]]".equals(op) ) && !lang.isCpp ) continue;
            if( source.startsWith(op, pos) ) {
                pos += op.length();
                return new Token(TokenType.OP, op, braceDepth, parenDepth, null);
            }
        } // for

        final char c = source.charAt(pos);
        ++pos;

        return new Token( TokenType.OP, String.valueOf(c), braceDepth, parenDepth, null );
    }

    private static boolean isCastKeyword(final Token t)
    {
        return t.type == TokenType.KEYWORD && CAST_KEYWORDS.contains(t.text);
    }

    // ── JSX/TSX boundary-finding pre-pass (XL.txt TIER 3, STATE_JS_TS.md) ───────────
    /**
     * Finds each top-level JSX/TSX tree in the flat token list and collapses it into one opaque
     * {@link TokenType#JSX_SPAN} token (raw source text preserved byte-for-byte, including
     * embedded newlines; {@code frozen = true}). Only called when {@link Lang#isJsxSyntax} is
     * true (`.jsx`/`.tsx` files only) -- see the call site in {@link #tokenize}.
     *
     * <p><b>Increment 1+2+3+4+5+6 scope</b> (see STATE_JS_TS.md's "2026-08-12 design session" for
     * the full 11-context list this will eventually cover, and the "2026-08-13 Increment 6" entry
     * for why items 9/10's literal recursive-walk design was NOT implemented as originally
     * specified): ten expression-start contexts are recognized here as a JSX-open candidate --
     * "after `return`" (Increment 1), "after `=>`" (arrow-function body start), "after `?`"/"after
     * `:`" (both branches of a ternary conditional expression) (Increment 2), call-argument-start /
     * array-literal-element-start (Increment 3, see {@link #isCallArgumentOrArrayElementStart}),
     * assignment-RHS (incl. compound assignment operators) / logical-nullish-RHS (Increment 4, see
     * {@link #isAssignmentOrLogicalRhsStart}), grouping-paren-start (Increment 5, see
     * {@link #isGroupingParenStart}), and bare `{`-hole-start / spread (Increment 6, see the plain
     * {@code Token.isPunct(prev, "{")} check and {@link #isSpreadContext}). Design list item 10
     * (template-literal `${}` holes) is NOT implemented -- see STATE_JS_TS.md, structurally
     * unreachable at this pre-pass's granularity because {@code emitTemplateLiteral} already
     * swallows an entire template literal (including every `${...}` interpolation) into one opaque
     * character-level STRING token before this post-tokenize pass ever runs; recognizing JSX inside
     * a template hole would require a tokenizer-level change, not an addition to this method. A `<`
     * in an unlisted expression-start context still falls through unchanged to the existing
     * `reclassifyAngleBrackets`/relational-operator handling, same as before this pre-pass existed.
     */
    private void findJsxSpans(final List<Token> tokens)
    {
        final List<Integer> sig = new ArrayList<>();
        for( int i = 0; i < tokens.size(); ++i ) {
            final TokenType ty = tokens.get(i).type;
            if(ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK || ty == TokenType.PREPROCESSOR) continue;
            sig.add(i);
        }

        for( int s = 0; s < sig.size(); ++s ) {
            final int   idx = sig.get(s);
            final Token cur = tokens.get(idx);
            if( !Token.isOp(cur, "<") ) continue;

            final Token prev = s > 0 ? tokens.get( sig.get(s - 1) ) : null;
            // Increment 1: "after return". Increment 2 adds "after =>" (arrow-function body start)
            // and "after ?"/"after :" (both branches of a ternary conditional expression) -- see
            // STATE_JS_TS.md's "2026-08-12 design session" context list, items 1-3. A bare "?" or
            // ":" OP token here is unambiguous: "?." (optional chaining), "??" (nullish coalescing),
            // and "?:" are all matched as their own multi-char ops by the character-level lexer
            // (MULTI_CHAR_OPS) before ever falling through to a single-char "?" token, so this can't
            // misfire on those. A wrongly-attempted context (e.g. a real less-than comparison, or a
            // legacy `<T>` cast) is still safe even if this check fires on it -- findJsxSpanEnd/
            // parseJsxTag returns -1 for anything that doesn't actually parse as a balanced JSX
            // tree, leaving the tokens untouched.
            final boolean isJsxContext = Token.isKeyword(
                prev, "return"
            ) || Token.isOp(
                prev, "=>"
            ) || Token.isOp(
                prev, "?"
            ) || Token.isOp(
                prev, ":"
            ) || isCallArgumentOrArrayElementStart(
                tokens, sig, s
            ) || isAssignmentOrLogicalRhsStart(
                prev
            ) || isGroupingParenStart(
                tokens, sig, s
            ) || Token.isPunct(
                prev, "{"
            ) || (prev != null && prev.type == TokenType.TEMPLATE_HOLE_OPEN) || isSpreadContext(
                tokens, sig, s
            );
            if(!isJsxContext) continue;

            final List<int[]> rawHoles     = new ArrayList<>();
            final List<int[]> rawAttrHoles = new ArrayList<>();
            final int         endTokenIdx  = findJsxSpanEnd(tokens, sig, s, rawHoles, rawAttrHoles);
            if(endTokenIdx < 0) continue; // Unbalanced/not real JSX here -- leave tokens untouched

            final StringBuilder text = new StringBuilder();
            for(int k = idx; k <= endTokenIdx; ++k) text.append( tokens.get(k).text );

            final Token span = new Token(
                TokenType.JSX_SPAN, text.toString(), cur.braceDepth, cur.parenDepth, null
            );
            span.frozen = true;

            // Convert both `rawHoles` (children-position holes) and `rawAttrHoles`
            // (`name={...}` attribute-value holes, per-tag, any nesting depth of this tree) raw
            // `tokens`-index ranges into offsets into the span's own `text` (same 0-based scheme
            // as `jsxOpeningTagEndOffset`), merged into one ascending-offset list -- both kinds are
            // spliced identically by `JsTsSpecificRule#spliceJsxExpressionHoles`, which only cares
            // about non-overlapping `[start, end)` ranges in source order, not which kind produced
            // each one. "JSX full embedding-aware dispatcher" recursive `{}`-hole-parsing job
            // (STATE_JS_TS.md).
            final List<int[]> allRawHoles = new ArrayList<>( rawHoles.size() + rawAttrHoles.size() );
            allRawHoles.addAll(rawHoles);
            allRawHoles.addAll(rawAttrHoles);
            allRawHoles.sort( (a, b) -> Integer.compare( a[0], b[0] ) );
            if( !allRawHoles.isEmpty() ) {
                final List<int[]> holeSpans = new ArrayList<>();
                for( final int[] raw : allRawHoles ) {
                    int off0 = 0;
                    for( int k = idx; k < raw[0]; ++k ) off0 += tokens.get(k).text.length();
                    int off1 = off0;
                    for( int k = raw[0]; k < raw[1]; ++k ) off1 += tokens.get(k).text.length();
                    holeSpans.add( new int[]{off0, off1} );
                } // for raw
                span.jsxHoleSpans = holeSpans;
            } // if

            // STATE_JS_TS.md's Step 2 "context 11" scoping session, sub-context 1 (Increment 1,
            // detect-and-measure-only) -- re-parse this same span's own root/opening tag (already
            // proven well-formed, since findJsxSpanEnd above only succeeds by walking through it)
            // purely to capture its attribute-boundary structure; a second, side-effect-free parse
            // of the same short token range, not a behavior change to anything findJsxSpanEnd
            // already did. Populates jsxOpeningTagEndOffset/jsxAttrBoundaries as offsets into the
            // span's own `text` (0 == the leading `<`); left at their `-1`/`null` defaults if the
            // root tag is somehow not open/self-close (structurally shouldn't happen here, since
            // findJsxSpanEnd's own first iteration requires kind 0 or 2 to proceed at all).
            final JsxTagResult rootTag = parseJsxTag(tokens, sig, s);
            if( rootTag != null && (rootTag.kind == 0 || rootTag.kind == 2) ) {
                final int           tagEndRawIdx   = sig.get(rootTag.newSigPos - 1); // Raw index of the tag's own '>'
                      int           offset         = 0;
                final List<Integer> attrBoundaries = new ArrayList<>();
                for(int k = idx; k <= tagEndRawIdx; ++k) {
                    if( rootTag.attrRawTokenIndices.contains(k) ) attrBoundaries.add(offset);
                    offset += tokens.get(k).text.length();
                }
                span.jsxOpeningTagEndOffset = offset;
                span.jsxAttrBoundaries      = attrBoundaries;
            } // if

            // Replace tokens[idx..endTokenIdx] (inclusive) with the single span token. No other
            // tokens are inserted/removed by this pass (unlike reclassifyAngleBrackets), so a
            // straightforward remove-then-set is sufficient.
            for(int k = endTokenIdx; k > idx; --k) tokens.remove(k);
            tokens.set(idx, span);

            // Re-derive `sig` for every position after `s` against the now-shorter token list --
            // entries inside the consumed span are gone, entries after it shift left by the
            // removed count. `sig.get(s)` itself (== idx) still correctly points at the new span
            // token, so it's left unchanged.
            final int removed = endTokenIdx - idx;
            for( int k = sig.size() - 1; k > s; --k ) {
                if( sig.get(k) > endTokenIdx ) sig.set( k, sig.get(k) - removed );
                else                           sig.remove(k);
            }
        } // for s
    }

    /**
     * Assignment operators (per STATE_JS_TS.md's design list item 6: "after `=`... also covers
     * `+=`/`-=`/etc. compound assignment operators -- same RHS-start shape"). Matches the exact set
     * of assignment-shaped entries this tokenizer's own {@link #MULTI_CHAR_OPS} emits, plus the
     * plain single-char `=`. Not reused from {@code MiscRuleCore.ASSIGNMENT_OPS} (rules package) --
     * that field is `protected` and cross-package, and is missing `&&=`/`||=`/`??=`/`<<=`/`>>>=`
     * which this tokenizer's own lexer does emit; kept local and tokenizer-scoped instead.
     */
    private static final Set<String> JSX_ASSIGNMENT_OPS = setOf(
        "=",
        "+=",
        "-=",
        "*=",
        "/=",
        "%=",
        "&=",
        "|=",
        "^=",
        "<<=",
        ">>=",
        ">>>=",
        "&&=",
        "||=",
        "??="
    );

    /** Logical/nullish short-circuit operators (design list item 7: "after `&&`, `||`, `??`") */
    private static final Set<String> JSX_LOGICAL_OPS = setOf("&&", "||", "??");

    /**
     * True when {@code prev} is an assignment operator (design list item 6, Increment 4) or a
     * logical/nullish short-circuit operator (design list item 7, Increment 4) -- both are
     * simple single-token-lookback checks, same shape as Increment 2's `=>`/`?`/`:` checks, no
     * comma/bracket-depth tracking needed. Compound assignment (`+=` etc.) and logical-assignment
     * (`&&=` etc.) share the same "RHS starts right after" shape as plain `=`, so both live in one
     * set/check rather than two. Safety is unchanged from every prior increment: a wrongly-attempted
     * context (e.g. `x = y < z` genuinely relational) is harmless because
     * {@link #findJsxSpanEnd}/{@link #parseJsxTag} returns -1 for anything that doesn't parse as
     * balanced JSX, leaving tokens untouched.
     */
    private boolean isAssignmentOrLogicalRhsStart(final Token prev)
    {
        if(prev == null || prev.type != TokenType.OP) return false;

        return JSX_ASSIGNMENT_OPS.contains(prev.text) || JSX_LOGICAL_OPS.contains(prev.text);
    }

    /**
     * Walks forward from {@code sig.get(s0)} (a `<` token, already confirmed to be at a
     * recognized expression-start context by the caller) tracking JSX tag-nesting depth, and
     * returns the raw token index of the final `>` (or self-closing tag's `>`) that closes the
     * whole top-level tree -- or {@code -1} if the token stream doesn't actually form a balanced
     * JSX tree from this point (e.g. a genuine less-than comparison that happened to match the
     * "after return" context; the caller must treat {@code -1} as "not JSX, leave alone", not an
     * error). A `{...}` expression hole's interior is balance-skipped without interpretation
     * (Increment 1 does not recurse into holes -- any `<`/`>` inside one is irrelevant to the
     * *outer* tree's own tag-nesting depth either way).
     */
    private int findJsxSpanEnd(final List<Token> tokens, final List<Integer> sig, final int s0)
    {
        return findJsxSpanEnd(tokens, sig, s0, null, null);
    }

    /**
     * Same as {@link #findJsxSpanEnd(List, List, int)}, additionally recording each top-level
     * children-position `{...}` hole's raw `[openIdx, closeIdxExclusive)` token-index range into
     * {@code holesOut} (raw indices into {@code tokens}, {@code closeIdxExclusive} one past the
     * hole's own closing `}`) when non-null -- for the "JSX full embedding-aware dispatcher"
     * recursive `{}`-hole-parsing job (STATE_JS_TS.md). A hole recorded here is, by construction,
     * always a children-position hole at ANY nesting depth of the tree being walked (an
     * attribute-value hole is consumed entirely inside {@link #parseJsxTag}'s own call, never seen
     * by this method's own loop) -- see that method's javadoc for why. Nested holes inside a
     * recorded hole's own interior are deliberately NOT walked into here (this method only
     * balance-skips a hole's interior, never interprets it) -- recursion into a hole's own nested
     * JSX/holes is instead handled by re-running the full formatting pipeline on that hole's
     * extracted interior text (see {@code JsTsSpecificRule#spliceJsxExpressionHoles}), which
     * naturally re-discovers any nested span the same way this pass does.
     */
    private int findJsxSpanEnd(
        final List<Token>   tokens,
        final List<Integer> sig,
        final int           s0,
        final List<int[]>   holesOut,
        final List<int[]>   attrHolesOut
    )
    {
        final java.util.Deque<String> openNames = new java.util.ArrayDeque<String>();
        int s = s0;
        while( s < sig.size() ) {
            final Token cur = tokens.get( sig.get(s) );
            if( !Token.isOp(cur, "<") ) {
                if( Token.isPunct(cur, "{") ) {
                    final int holeOpenRawIdx = sig.get(s);
                    final int newS           = skipBalancedBraceHole(tokens, sig, s);
                    if(newS < 0) return -1;
                    if(holesOut != null) {
                        final int holeCloseRawIdx = sig.get(newS - 1); // The hole's own closing `}`
                        holesOut.add( new int[]{holeOpenRawIdx, holeCloseRawIdx + 1} );
                    }
                    s = newS;
                    continue;
                } // if
                ++s;
                continue;
            } // if

            final JsxTagResult r = parseJsxTag(tokens, sig, s);
            if(r == null) return -1;
            if(attrHolesOut != null) attrHolesOut.addAll(r.attrValueHoleRawRanges);
            final int newS = r.newSigPos;
            final int kind = r.kind;      // 0 = open, 1 = close, 2 = self-close

            if(kind == 1) {
                // Tag-name identity check (STATE_JS_TS.md's 2026-08-13 hardening) -- a closing tag
                // only reduces depth when its name matches the innermost still-open tag's name;
                // any mismatch (e.g. `<a>...</b>`, or a close with no open at all) bails out (-1)
                // rather than silently accepting an unbalanced tree, same safe-fallback contract
                // every other rejection in this pass already uses.
                if( openNames.isEmpty() ) return -1;
                final String expected = openNames.pop();
                if( !expected.equals(r.tagName) ) return -1;
                if( openNames.isEmpty() ) return sig.get(newS - 1);
            } // if
            else if(kind == 2) {
                if( openNames.isEmpty() ) return sig.get(newS - 1);
            }
            else {
                openNames.push(r.tagName);
            }
            s = newS;
        } // while

        return -1; // Ran off the end without the tree ever closing
    }

    /**
     * Result of {@link #parseJsxTag}: {@code newSigPos} is the `sig` position immediately after
     * the consumed closing `>`; {@code kind} is 0 (open tag), 1 (closing tag, `</Name>`), or 2
     * (self-closing tag, `<Name .../>`); {@code tagName} is the tag's raw dotted name text (e.g.
     * `"Foo"` or `"React.Fragment"`), captured for the tag-name-identity check in
     * {@link #findJsxSpanEnd} (STATE_JS_TS.md's 2026-08-13 hardening -- previously only tag-nesting
     * *depth* was tracked, not name identity, so `<a>...</b>` silently balanced).
     */
    private static final class JsxTagResult {

        final int    newSigPos;
        final int    kind;
        final String tagName;

        final List<Integer> attrRawTokenIndices; // Raw `tokens` indices where each attribute in an
                                                    // open/self-close tag begins, in source order --
                                                    // empty for a closing tag (kind == 1). See
                                                    // STATE_JS_TS.md's Step 2 "context 11" scoping
                                                    // session, sub-context 1 -- consumed only by
                                                    // findJsxSpans to populate Token#jsxAttrBoundaries.

        final List<int[]> attrValueHoleRawRanges; // Raw `tokens` index [openIdx, closeIdxExclusive)
                                                    // Ranges of each top-level `name={...}` attribute
                                                    // VALUE hole (e.g. `onClick={handler}`) in this
                                                    // tag -- deliberately excludes spread attributes
                                                    // (`{...props}`, not a name=value shape; a spread
                                                    // expression isn't valid standalone dispatch
                                                    // content, see STATE_JS_TS.md's attribute-value
                                                    // hole recursion job). Empty for a closing tag.
                                                    // Consumed only by findJsxSpans to populate
                                                    // Token#jsxHoleSpans alongside children holes.

        JsxTagResult(
            final int           newSigPos,
            final int           kind,
            final String        tagName,
            final List<Integer> attrRawTokenIndices,
            final List<int[]>   attrValueHoleRawRanges
        )
        {
            this.newSigPos              = newSigPos;
            this.kind                   = kind;
            this.tagName                = tagName;
            this.attrRawTokenIndices    = attrRawTokenIndices;
            this.attrValueHoleRawRanges = attrValueHoleRawRanges;
        }

    } // class JsxTagResult

    /**
     * Parses one JSX tag (open, close, or self-closing) starting at {@code sig.get(s)} (a `<`
     * token). Returns a {@link JsxTagResult}, or {@code null} if this `<` does
     * not actually begin a well-formed tag (no tag-name IDENTIFIER following, or no matching `>`
     * found before the token stream ends). Attribute-value expression holes (`attr={...}`) are
     * balance-skipped via a local brace-depth counter so an embedded `>` (e.g. `attr={a > b}`)
     * isn't mistaken for the tag's own close.
     *
     * <p>Also records each top-level (localBrace == 0) attribute's start position, for an
     * open/self-close tag: either a plain `IDENTIFIER` (a bare boolean attribute like `disabled`,
     * or the name half of `name=value` -- either way the identifier itself is the attribute's own
     * boundary) or a `{` at localBrace == 0 (a spread attribute, `{...props}`, which has no
     * preceding name). Purely structural bookkeeping for STATE_JS_TS.md's Step 2 "context 11"
     * scoping session, sub-context 1 -- unused by anything in this method itself, consumed only
     * by {@link #findJsxSpans}.
     */
    private JsxTagResult parseJsxTag(
        final List<Token>   tokens,
        final List<Integer> sig,
        final int           s0
    )
    {
        final int n = sig.size();
              int s = s0 + 1;     // Skip the opening '<'
        if(s >= n) return null;

        final boolean closing = Token.isOp( tokens.get( sig.get(s) ), "/" );
        if(closing) {
            ++s;
            if(s >= n) return null;
        }

        // Fragment shorthand (`<>`/`</>`): the tag-name IDENTIFIER is entirely absent, `>` follows
        // the `<` (or `</`) immediately. Real-corpus dogfood (STATE_JS_TS.md's Step 2 Increment 5,
        // reactstrap's DropdownToggle.js) found this case wasn't recognized at all -- a bare `<>`
        // failed the old unconditional "tag name required" check below and returned null, so
        // findJsxSpans never even considered the fragment JSX, letting its `{...}` expression hole
        // fall through to ordinary JS statement-level formatting and get corrupted (a stray `;`
        // inserted inside the hole). Empty string is the fragment's `tagName` sentinel -- open/close
        // fragments both use "", so findJsxSpanEnd's existing tag-identity check (`expected.equals(
        // r.tagName)`) already pairs them correctly with zero changes needed there.
        final boolean isFragment = Token.isOp( tokens.get( sig.get(s) ), ">" );
        final String  tagNameStr;
        if(isFragment) {
            tagNameStr = "";
        }
        else {
            if( tokens.get( sig.get(s) ).type != TokenType.IDENTIFIER ) return null; // Tag name required
            final StringBuilder tagName = new StringBuilder( tokens.get( sig.get(s) ).text );
            ++s;
            while( s + 1 < n && Token.isOp( tokens.get( sig.get(s) ), "." )
                    && tokens.get( sig.get(s + 1) ).type == TokenType.IDENTIFIER ) {
                tagName.append('.').append( tokens.get( sig.get(s + 1) ).text );
                s += 2; // Dotted component name, e.g. `React.Fragment`
            }
            tagNameStr = tagName.toString();
        }

        final List<Integer> attrRawTokenIndices    = new ArrayList<>();
        final List<int[]>   attrValueHoleRawRanges = new ArrayList<>();
              int           localBrace             = 0;
              boolean       selfClosing            = false;
              int           valueHoleOpenRawIdx    = -1;                // Raw idx of the currently-open value
                                                          // Hole's own `{`, or -1 when the
                                                          // localBrace==0->1 transition in
                                                          // progress isn't a value hole (e.g. a
                                                          // spread attribute) -- see
                                                          // attrValueHoleRawRanges' own javadoc.
        while(s < n) {
            final Token t = tokens.get( sig.get(s) );
            if( localBrace == 0 && Token.isOp(t, "/") && s + 1 < n
                    && Token.isOp( tokens.get( sig.get(s + 1) ), ">" ) ) {
                selfClosing = true;
                ++s; // Leave the '>' itself for the shared consumption below
                break;
            }
            if( localBrace == 0 && Token.isOp(t, ">") ) break;
            // A `{` at localBrace == 0 only starts a new attribute (spread, `{...props}`) when
            // it's NOT the value half of a preceding `name={...}` -- otherwise it's the same
            // attribute's own value hole, not a fresh boundary (found via Increment 2's own
            // real-code smoke test: without this check, `attr={x}` wrongly split into two
            // "attributes", `attr=` and `{x}`).
            final boolean isValueHoleOpenBrace = Token.isPunct(
                t, "{"
            ) && s > 0 && Token.isOp(
                tokens.get( sig.get(s - 1) ), "="
            );
            // A hyphenated attribute name (`data-foo`, `aria-label`) tokenizes as
            // IDENTIFIER/`-`/IDENTIFIER -- a top-level `-` can never legally separate two real
            // JSX attributes any other way, so an IDENTIFIER immediately following one is always
            // a name continuation, not a fresh boundary (previously mis-split into two
            // "attributes", corrupting the wrap output -- see STATE_JS_TS.md).
            final boolean isHyphenatedNameContinuation = s > 0 && Token.isOp(
                tokens.get( sig.get(s - 1) ), "-"
            );
            if( localBrace == 0 && !closing && !isValueHoleOpenBrace && !isHyphenatedNameContinuation
                    && ( t.type == TokenType.IDENTIFIER || Token.isPunct(
                        t, "{"
                    ) ) ) attrRawTokenIndices.add(
                        sig.get(s)
                    );
            if( Token.isPunct(t, "{") ) {
                if(localBrace == 0 && isValueHoleOpenBrace) valueHoleOpenRawIdx = sig.get(s);
                ++localBrace;
                ++s;
                continue;
            }
            if( Token.isPunct(t, "}") ) {
                if(localBrace > 0) --localBrace;
                if(localBrace == 0 && valueHoleOpenRawIdx >= 0) {
                    attrValueHoleRawRanges.add( new int[]{valueHoleOpenRawIdx, sig.get(s) + 1} );
                    valueHoleOpenRawIdx = -1;
                }
                ++s;
                continue;
            } // if
            ++s;
        } // while

        if( s >= n || !Token.isOp( tokens.get( sig.get(s) ), ">" ) ) return null;
        ++s; // Consume '>'

        return new JsxTagResult(
            s, closing ? 1 : (selfClosing ? 2 : 0), tagNameStr, attrRawTokenIndices,
            attrValueHoleRawRanges
        );
    }

    /**
     * Balance-skips a `{...}` hole starting at {@code sig.get(s)} (a `{` token); returns the
     * `sig` position immediately after the matching `}`, or -1 if unbalanced.
     */
    private int skipBalancedBraceHole(
        final List<Token>   tokens,
        final List<Integer> sig,
        final int           s0
    )
    {
        int braceDepth = 0;
        int s          = s0;
        while( s < sig.size() ) {
            final Token t = tokens.get( sig.get(s) );
            if( Token.isPunct(t, "{") ) ++braceDepth;
            else if( Token.isPunct(t, "}") ) {
                --braceDepth;
                if(braceDepth == 0) return s + 1;
            }
            ++s;
        } // while

        return -1;
    }

    /**
     * Increment 3: recognizes a `<` at sig position {@code s} as a call-argument-start or
     * array-literal-element-start JSX context, per STATE_JS_TS.md's design list items
     * "Call-argument start" / "Array-literal element start". Two shapes each, both handled
     * uniformly here:
     * <ul>
     * <li>Immediately after `(` -- only when that `(` is itself a call-open (the token before it
     * is an IDENTIFIER, `)`, or `]`), matching {@code reclassifyAngleBrackets}'s own
     * generic-safe-token notion of "call-shaped". A bare grouping `(` (not preceded by a
     * call-owner) is deliberately NOT recognized here -- the design calls out grouping-paren-start
     * as its own separate, not-yet-implemented context; over-claiming it here would blur that
     * boundary.</li>
     * <li>Immediately after `[` -- always array-literal-element-start (no call/grouping ambiguity
     * exists for `[`; a computed member-access `[` starting with JSX is vanishingly rare and, even
     * if wrongly attempted, {@link #findJsxSpanEnd}/{@link #parseJsxTag} returns -1 for anything
     * that doesn't parse as balanced JSX, leaving tokens untouched).</li>
     * <li>Immediately after a top-level `,` -- walks backward to find the nearest unmatched
     * enclosing bracket ({@link #findEnclosingOpenBracket}); if it's a call-open `(` or any `[`,
     * the comma is a call-argument/array-element boundary by the same test as above. A `,` whose
     * enclosing bracket is a grouping `(`, a `{` (object literal), or nothing (top level) is not
     * recognized this increment.</li>
     * </ul>
     */
    private boolean isCallArgumentOrArrayElementStart(
        final List<Token>   tokens,
        final List<Integer> sig,
        final int           s
    )
    {
        if(s == 0) return false;
        final Token prev = tokens.get( sig.get(s - 1) );

        if( Token.isPunct(prev, "[") ) return true;
        if( Token.isPunct(prev, "(") ) return isCallOpenParen(tokens, sig, s - 1);

        if( Token.isPunct(prev, ",") ) {
            final int openIdx = findEnclosingOpenBracket(tokens, sig, s - 1);
            if(openIdx < 0) return false;
            final Token open = tokens.get( sig.get(openIdx) );
            if( Token.isPunct(open, "[") ) return true;
            if( Token.isPunct(open, "(") ) return isCallOpenParen(tokens, sig, openIdx);
            return false;
        } // if

        return false;
    }

    /**
     * True when the `(` token at {@code sig.get(parenS)} is a call-open -- immediately preceded
     * by an IDENTIFIER, `)`, or `]` (same notion `reclassifyAngleBrackets`'s generic-safe-token
     * check already uses to distinguish a call from a bare grouping paren).
     */
    private boolean isCallOpenParen(
        final List<Token>   tokens,
        final List<Integer> sig,
        final int           parenS
    )
    {
        if(parenS == 0) return false;
        final Token before = tokens.get( sig.get(parenS - 1) );

        return before.type == TokenType.IDENTIFIER || Token.isPunct(
            before, ")"
        ) || Token.isPunct(
            before, "]"
        );
    }

    /**
     * Increment 5: recognizes a `<` at sig position {@code s} as a grouping-paren-start JSX
     * context, per STATE_JS_TS.md's design list item "Parenthesized-expression start" (item 8) --
     * the mirror image of {@link #isCallOpenParen}: immediately after a `(` that is NOT itself a
     * call-open (i.e. not preceded by an IDENTIFIER/`)`/`]`). A call-open `(` is already covered
     * by {@link #isCallArgumentOrArrayElementStart} -- this check deliberately only fires on the
     * complementary case to avoid double-claiming the same `(` shape both ways.
     */
    private boolean isGroupingParenStart(
        final List<Token>   tokens,
        final List<Integer> sig,
        final int           s
    )
    {
        if(s == 0) return false;
        final int   parenS = s - 1;
        final Token prev   = tokens.get( sig.get(parenS) );
        if( !Token.isPunct(prev, "(") ) return false;

        return !isCallOpenParen(tokens, sig, parenS);
    }

    /**
     * Increment 6: recognizes a `<` at sig position {@code s} as a spread-element JSX context
     * (design list item 11: "after `...` wherever spread is legal in expression position
     * (array-literal element, call argument)"). The token immediately before `<` must be `...`;
     * the token before *that* is then tested against the exact same call/array-argument-start
     * shapes {@link #isCallArgumentOrArrayElementStart} already uses (immediately after `(`/`[`,
     * or after a top-level `,` whose enclosing bracket is a call-open `(` or any `[`) -- spread is
     * only legal in those two positions, so this reuses that logic one token further back rather
     * than duplicating the `(`/`[`/`,` shape table.
     */
    private boolean isSpreadContext(final List<Token> tokens, final List<Integer> sig, final int s)
    {
        if(s == 0) return false;
        final Token prev = tokens.get( sig.get(s - 1) );
        if( !Token.isOp(prev, "...") ) return false;

        return isCallArgumentOrArrayElementStart(tokens, sig, s - 1);
    }

    /**
     * Scans backward from (but not including) {@code sig.get(beforeS)} tracking bracket depth
     * across `(`/`)`, `[`/`]`, `{`/`}`, and returns the `sig` index of the nearest unmatched
     * opening bracket -- the bracket that directly encloses position {@code beforeS} -- or -1 if
     * none is found (top level). Used to classify a top-level `,` by what it's inside of.
     */
    private int findEnclosingOpenBracket(
        final List<Token>   tokens,
        final List<Integer> sig,
        final int           beforeS
    )
    {
        final java.util.Deque<String> stack = new java.util.ArrayDeque<>();
        for(int s = beforeS - 1; s >= 0; --s) {
            final Token t = tokens.get( sig.get(s) );
            if( Token.isPunct(t, ")") || Token.isPunct(t, "]") || Token.isPunct(t, "}") ) {
                stack.push(t.text);
            }
            else if( Token.isPunct(t, "(") || Token.isPunct(t, "[") || Token.isPunct(t, "{") ) {
                if( stack.isEmpty() ) return s;
                stack.pop();
            }
        } // for

        return -1;
    }

    // ── Generic/template angle bracket disambiguation ───────────────────────────────
    private void reclassifyAngleBrackets(final List<Token> tokens)
    {
        final List<Integer> sig = new ArrayList<>();
        for( int i = 0; i < tokens.size(); ++i ) {
            final TokenType ty = tokens.get(i).type;
            if(ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK || ty == TokenType.PREPROCESSOR) continue;
            sig.add(i);
        }

        final Deque<int[]> openStack        = new ArrayDeque<>(); // Each entry: {tokenIndex, validFlag}
              int          nestedBraceDepth = 0;                  // Balanced `{...}` seen while openStack is non-empty -- see below

        for( int s = 0; s < sig.size(); ++s ) {
            final int   idx = sig.get(s);
            final Token cur = tokens.get(idx);

            if( cur.type == TokenType.PUNCT && "{".equals(cur.text) && !openStack.isEmpty() ) {
                // TS object-type argument nested directly inside an active generic clause
                // (`ComponentPublicInstanceConstructor<Foo<..., {}, S, ...>>` -- an empty or
                // populated object-type literal used as one of several type arguments) is legal
                // and generic-safe, not a statement-body brace -- unlike the `{`-always-clears
                // rule below (which exists to bail out of a false-positive `<`/`>` guess once a
                // real code block is reached), a brace nested inside an *already-tracked* open
                // `<` never means the enclosing generic clause was a false guess. Without this,
                // the `{`/`}` clear-all below wiped the entire open stack -- including the outer
                // generic's own already-valid open `<` entries -- leaving the whole multi-line
                // clause's closing `>` a plain OP token instead of ANGLE_BRACKET_CLOSE (vuejs/core
                // dogfood, `apiDefineComponent.ts`'s `CreateComponentPublicInstanceWithMixins<...,
                // {}, S, ...>` type argument list).
                ++nestedBraceDepth;
                continue;
            } // if
            if( cur.type == TokenType.PUNCT && "}".equals(cur.text) && nestedBraceDepth > 0 ) {
                --nestedBraceDepth;
                continue;
            }

            if( nestedBraceDepth == 0 && cur.type == TokenType.PUNCT
                    && ( ";".equals(cur.text) || "{".equals(cur.text) || "}".equals(cur.text) ) ) {
                openStack.clear();
                continue;
            }

            if( cur.type == TokenType.OP && "<".equals(cur.text) ) {
                final Token prev = s > 0 ? tokens.get( sig.get(s - 1) ) : null;
                // Kotlin's `fun <T> foo(...)` generic-function type-parameter clause has no
                // identifier before its own `<` (it precedes the function name, not follows it,
                // unlike `class Foo<T>`/`foo<T>(...)` which are already covered by the IDENTIFIER
                // check above) -- recognize `fun` itself as a valid opener in that language only.
                final boolean kotlinGenericFunClause = lang.isKotlin && prev != null && prev.type == TokenType.KEYWORD && "fun".equals(
                    prev.text
                );
                if( prev != null && ( prev.type == TokenType.IDENTIFIER || isCastKeyword(prev)
                        || kotlinGenericFunClause ) ) {
                    openStack.push( new int[] {idx, 1} );
                }
                else if( !openStack.isEmpty() ) invalidateAll(openStack);
                continue;
            } // if

            if( cur.type == TokenType.OP && ">".equals(cur.text) ) {
                if( !openStack.isEmpty() ) {
                    final int[] open = openStack.pop();
                    if( open[1] == 1 ) {
                        tokens.set(
                            open[0], retype( tokens.get( open[0] ), TokenType.ANGLE_BRACKET_OPEN )
                        );
                        tokens.set( idx, retype(cur, TokenType.ANGLE_BRACKET_CLOSE) );
                    }
                } // if
                continue;
            } // if

            if( cur.type == TokenType.OP && ">>".equals(cur.text) ) {
                if( openStack.size() >= 2 ) {
                    final int[] inner = openStack.pop();
                    final int[] outer = openStack.pop();
                    if( inner[1] == 1 && outer[1] == 1 ) {
                        tokens.set(
                            inner[0], retype( tokens.get( inner[0] ), TokenType.ANGLE_BRACKET_OPEN )
                        );
                        tokens.set(
                            outer[0], retype( tokens.get( outer[0] ), TokenType.ANGLE_BRACKET_OPEN )
                        );
                        tokens.set( idx, retype(cur, TokenType.ANGLE_BRACKET_CLOSE) );
                        tokens.add(
                            idx + 1, new Token(TokenType.ANGLE_BRACKET_CLOSE, "", cur.braceDepth, cur.parenDepth, null)
                        );
                        shiftSigAfter(sig, s, idx + 1);
                    } // if
                } // if
                else if( openStack.size() == 1 ) {
                    final int[] open = openStack.pop();
                    if( open[1] == 1 ) {
                        // `cur` (the literal `>>` token) represents only ONE real angle-close here
                        // (the single tracked open `<`) plus one leftover literal `>` (e.g. an
                        // untracked `template<...>` outer bracket -- see the IDENTIFIER-only guard
                        // above). Splitting must not duplicate `cur`'s 2-char text onto both the
                        // retyped angle token and the new literal token, or the rendered output
                        // gains a spurious extra `>` (RDD_KEY_99-ish: was `retype(cur, ...)`,
                        // which preserved the full ">>" text on the angle token as well).
                        tokens.set(
                            open[0], retype( tokens.get( open[0] ), TokenType.ANGLE_BRACKET_OPEN )
                        );
                        tokens.set(
                            idx, new Token(TokenType.ANGLE_BRACKET_CLOSE, ">", cur.braceDepth, cur.parenDepth, null)
                        );
                        tokens.add(
                            idx + 1,
                            new Token(TokenType.OP, ">", cur.braceDepth, cur.parenDepth, null)
                        );
                        shiftSigAfter(sig, s, idx + 1);
                    } // if
                }
                continue;
            } // if

            if( cur.type == TokenType.OP && ">>>".equals(cur.text) ) {
                // Mirrors the `>>` handling above, generalized to a third nesting level (e.g.
                // `HashMap<String, HashMap<String, ArrayList<String>>>>`-style triple-nested
                // generics). Previously unhandled: `>>>` fell through to the generic-safe-token
                // fallback below, which is not generic-safe, so it invalidated the whole open
                // stack -- the outer `<`s never became ANGLE_BRACKET_OPEN and were then padded
                // as ordinary `<`/`>` relational operators on the next tokenize pass (idempotency
                // bug: round1's tightly-rendered `<...>` gets spaced out to `< ... >` on round2,
                // since round1's own output re-lexes the three adjacent `>` characters as a
                // single `>>>` token).
                if( openStack.size() >= 3 ) {
                    final int[] inner = openStack.pop();
                    final int[] mid   = openStack.pop();
                    final int[] outer = openStack.pop();
                    if( inner[1] == 1 && mid[1] == 1 && outer[1] == 1 ) {
                        tokens.set(
                            inner[0], retype( tokens.get( inner[0] ), TokenType.ANGLE_BRACKET_OPEN )
                        );
                        tokens.set(
                            mid[0], retype( tokens.get( mid[0] ), TokenType.ANGLE_BRACKET_OPEN )
                        );
                        tokens.set(
                            outer[0], retype( tokens.get( outer[0] ), TokenType.ANGLE_BRACKET_OPEN )
                        );
                        tokens.set( idx, retype(cur, TokenType.ANGLE_BRACKET_CLOSE) );
                        tokens.add(
                            idx + 1, new Token(TokenType.ANGLE_BRACKET_CLOSE, "", cur.braceDepth, cur.parenDepth, null)
                        );
                        shiftSigAfter(sig, s, idx + 1);
                        tokens.add(
                            idx + 2, new Token(TokenType.ANGLE_BRACKET_CLOSE, "", cur.braceDepth, cur.parenDepth, null)
                        );
                        shiftSigAfter(sig, s + 1, idx + 2);
                    } // if
                } // if
                else if( openStack.size() == 2 ) {
                    final int[] inner = openStack.pop();
                    final int[] outer = openStack.pop();
                    if( inner[1] == 1 && outer[1] == 1 ) {
                        // Two real closes plus one leftover literal `>` -- split `cur`'s 3-char
                        // text as 1+1+1 (see the `>>` size==1 branch's comment on why the text
                        // must be distributed across tokens, not duplicated)
                        tokens.set(
                            inner[0], retype( tokens.get( inner[0] ), TokenType.ANGLE_BRACKET_OPEN )
                        );
                        tokens.set(
                            outer[0], retype( tokens.get( outer[0] ), TokenType.ANGLE_BRACKET_OPEN )
                        );
                        tokens.set(
                            idx, new Token(TokenType.ANGLE_BRACKET_CLOSE, ">", cur.braceDepth, cur.parenDepth, null)
                        );
                        tokens.add(
                            idx + 1, new Token(TokenType.ANGLE_BRACKET_CLOSE, ">", cur.braceDepth, cur.parenDepth, null)
                        );
                        shiftSigAfter(sig, s, idx + 1);
                        tokens.add(
                            idx + 2,
                            new Token(TokenType.OP, ">", cur.braceDepth, cur.parenDepth, null)
                        );
                        shiftSigAfter(sig, s + 1, idx + 2);
                    } // if
                }
                else if( openStack.size() == 1 ) {
                    final int[] open = openStack.pop();
                    if( open[1] == 1 ) {
                        // One real close plus two leftover literal `>` characters (rendered back
                        // as a literal `>>` operator token)
                        tokens.set(
                            open[0], retype( tokens.get( open[0] ), TokenType.ANGLE_BRACKET_OPEN )
                        );
                        tokens.set(
                            idx, new Token(TokenType.ANGLE_BRACKET_CLOSE, ">", cur.braceDepth, cur.parenDepth, null)
                        );
                        tokens.add(
                            idx + 1,
                            new Token(TokenType.OP, ">>", cur.braceDepth, cur.parenDepth, null)
                        );
                        shiftSigAfter(sig, s, idx + 1);
                    } // if
                }
                continue;
            } // if

            // Any token inside an already-tracked nested `{...}` (see the nestedBraceDepth
            // block above) is an ordinary object-type member -- its own shape (property names,
            // `?`, `:`, keywords like `default`/`in`/`static` used as property names, etc.) has
            // nothing to do with the *outer* generic clause's own safety and must never
            // invalidate it (vuejs/core dogfood, `compiler-sfc/src/script/defineProps.ts`'s
            // `Record<string, { local: string; default?: Expression }>`: the member name
            // `default` is a KEYWORD not in `GENERIC_SAFE_KEYWORDS`, which invalidated the
            // enclosing `Record<...>` tracking before this exclusion existed).
            if( nestedBraceDepth == 0 && !isGenericSafeToken(
                cur
            ) && !openStack.isEmpty() ) invalidateAll(
                openStack
            );
        } // for
    }

    private void shiftSigAfter(final List<Integer> sig, final int afterPos, final int newIndex)
    {
        for( int j = afterPos + 1; j < sig.size(); ++j ) sig.set( j, sig.get(j) + 1 );
        sig.add(afterPos + 1, newIndex);
    }

    private void invalidateAll(final Deque<int[]> stack)
    {
        for( final int[] entry : stack ) entry[1] = 0;
    }

    private boolean isGenericSafeToken(final Token t)
    {
        switch(t.type) {

            case IDENTIFIER: /* FALL-THROUGH */
            case NUMBER: /* FALL-THROUGH */
            case STRING: /* FALL-THROUGH */
            case CHAR: /* FALL-THROUGH */
            case ANGLE_BRACKET_OPEN: /* FALL-THROUGH */
            case ANGLE_BRACKET_CLOSE:
                return true;

            case KEYWORD:
                return GENERIC_SAFE_KEYWORDS.contains(t.text);

            case PUNCT:
                // `(`/`)` allowed too: a function-type template argument (`std::function<void(int)>`)
                // has a balanced parameter list nested directly inside the `<>`
                return ",".equals(t.text) || "[".equals(t.text) || "]".equals(t.text)
                        || "(".equals(t.text) || ")".equals(t.text);

            case OP:
                // `:` (Kotlin-only) marks a type-parameter bound inside a generic clause
                // (`<A : Comparable<A>, B : Comparable<B>>`) -- without recognizing it as
                // generic-safe, the bound's own `:` invalidates the enclosing `<...>` tracking,
                // which then cascades: a second bound clause ending in `>>` finds its outer `<`
                // already marked invalid and leaves the whole `>>` unsplit, corrupting both
                // bound clauses' spacing (found via arrow-kt/arrow real-code testing,
                // Pair.kt's `compareTo` extension function). No-op for C/C++/Java, which never
                // use `:` inside a generic argument/parameter list.
                return ".".equals(t.text) || "::".equals(t.text) || "?".equals(t.text)
                        || "*".equals(t.text) || "&".equals(t.text)
                        || ( lang.isKotlin && ":".equals(t.text) )
                        // TS conditional types (`A extends B ? X : Y`) are legal directly inside
                        // a generic argument list (`Readonly<A extends B ? X : Y>`) -- the `:`
                        // there is the conditional type's own `?`/`:` branch separator, not a
                        // type annotation. Without recognizing it as generic-safe, `:` invalidates
                        // the enclosing `<...>` tracking the same way an unrecognized OP always
                        // does, leaving the whole clause's `<`/`>` as plain OP tokens instead of
                        // ANGLE_BRACKET_OPEN/CLOSE -- which in turn defeats `JsTsSpecificRule.
                        // enforceSemicolonInsertion`'s depth tracking, wrongly treating a NEWLINE
                        // inside the multi-line clause as a statement boundary (vuejs/core
                        // dogfood, `Readonly<\n  A extends B\n    ? C\n    : D\n>`).
                        || ( lang.isTs && ":".equals(t.text) )
                        // TS union types are legal directly inside a generic argument list
                        // (`Record<string | symbol, Function | number>`) -- without this, `|`
                        // invalidates the enclosing `<...>` tracking the same way an unrecognized
                        // OP always does, leaving the closing `>` a plain OP token instead of
                        // ANGLE_BRACKET_CLOSE; that in turn defeats `JsTsSpecificRule.
                        // enforceSemicolonInsertion`'s CONTINUATION_OPS check (a trailing `>` OP
                        // looks like an unfinished comparison), silently dropping the statement's
                        // semicolon and letting `JsTsDeclarationAlignmentRule.parseTypeAlias`'s
                        // depth-scan run away past the real statement boundary (vuejs/core
                        // dogfood, `collectionHandlers.ts`).
                        || ( lang.isTs && "|".equals(t.text) )
                        // TS function-type type arguments (`Map<(...args: any[]) => void,
                        // PageErrorHandler>`, vuejs/core dogfood
                        // `vue/__tests__/e2e/e2eBrowserUtils.ts`) are legal directly inside a
                        // generic argument list -- without recognizing the function type's own
                        // `=>` and rest-parameter `...` as generic-safe, either invalidates the
                        // enclosing `<...>` tracking the same way an unrecognized OP always does,
                        // leaving the closing `>` a plain OP token instead of
                        // ANGLE_BRACKET_CLOSE.
                        || ( lang.isTs && "=>".equals(t.text) )
                        || ( lang.isTs && "...".equals(t.text) );

            default:
                return false;

        } // switch
    }

    private Token retype(final Token t, final TokenType newType)
    {
        return new Token(newType, t.text, t.braceDepth, t.parenDepth, t.name);
    }

    private boolean isPreprocessorLanguage()
    {
        // Java source files sometimes carry PCPP-style C-preprocessor directives
        // (`#define`/`#ifdef`/etc.) as a poor man's template mechanism ahead of a separate
        // preprocessing step. Lexing them as opaque PREPROCESSOR/MACRO_DEF tokens (same as
        // C/C++) means every rule already passes them through untouched -- no per-rule
        // Java-specific handling needed.
        //
        // JS/TS is excluded: `#` there is real syntax (a private class field/method name, e.g.
        // `#cache = new Map()`), not a preprocessor sigil. Treating a line-leading `#` as a
        // preprocessor directive swallowed the entire private-field statement (including any
        // already-present trailing `;` on a reformat) into one opaque token, which
        // JsTsSpecificRule.enforceSemicolonInsertion then unconditionally appended a `;` to --
        // harmless-looking on a fresh format (no existing `;` to double up), but producing a
        // doubled `;;` the moment the same statement round-tripped through the formatter again
        // (found via the standalone harness's round1/round2 idempotency check).
        return !(lang.isJs || lang.isTs);
    }

} // class TokenizerCurly
