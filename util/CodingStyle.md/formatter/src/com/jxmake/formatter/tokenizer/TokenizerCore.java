/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
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
        public final String text;
        public final int braceDepth;
        public final int parenDepth;
        public final String name; // for `{`/`}` only: pushed/popped construct name, else null

        public Token(TokenType type, String text, int braceDepth, int parenDepth, String name) {
            this.type = type;
            this.text = text;
            this.braceDepth = braceDepth;
            this.parenDepth = parenDepth;
            this.name = name;
        }
    }

    private static Set<String> setOf(String... words) {
        return new HashSet<>(Arrays.asList(words));
    }

    private static final Set<String> KEYWORDS_C = setOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
            "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long",
            "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct",
            "switch", "typedef", "union", "unsigned", "void", "volatile", "while");

    private static final Set<String> KEYWORDS_CPP = setOf(
            "alignas", "alignof", "asm", "auto", "bool", "break", "case", "catch", "char",
            "char16_t", "char32_t", "class", "const", "constexpr", "const_cast", "continue",
            "decltype", "default", "delete", "do", "double", "dynamic_cast", "else", "enum",
            "explicit", "export", "extern", "false", "final", "float", "for", "friend", "goto",
            "if", "inline", "int", "long", "mutable", "namespace", "new", "noexcept", "nullptr",
            "operator", "override", "private", "protected", "public", "register",
            "reinterpret_cast", "return", "short", "signed", "sizeof", "static", "static_assert",
            "static_cast", "struct", "switch", "template", "this", "thread_local", "throw",
            "true", "try", "typedef", "typeid", "typename", "union", "unsigned", "using",
            "virtual", "void", "volatile", "wchar_t", "while");

    private static final Set<String> KEYWORDS_JAVA = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "true", "false", "null", "try", "var", "void",
            "volatile", "while");

    private static final Set<String> NAMED_CONSTRUCT_C = setOf("struct", "enum");
    private static final Set<String> NAMED_CONSTRUCT_CPP =
            setOf("class", "struct", "enum", "namespace");
    private static final Set<String> NAMED_CONSTRUCT_JAVA = setOf("class", "interface", "enum");

    private static final String[] MULTI_CHAR_OPS = {
            "<<=", ">>=", "...", "->*",
            "::", "<<", ">>", "<=", ">=", "==", "!=", "&&", "||",
            "++", "--", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "->", ".*"
    };

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

    public boolean hasSyntaxError() {
        return syntaxError;
    }

    public TokenizerCore(String language) {
        this.language = language;
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
            default:
                throw new IllegalArgumentException("Unknown language: " + language);
        }
    }

    public List<Token> tokenize(String source) {
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

        List<Token> tokens = new ArrayList<>();

        while (pos < length) {
            char c = source.charAt(pos);

            if (c == '\r' || c == '\n') {
                addToken(tokens, emitNewline());
                atLineStart = true;
                continue;
            }
            if (c == ' ' || c == '\t') {
                addToken(tokens, emitWhitespace());
                continue;
            }

            Token t;
            if (isPreprocessorLanguage() && c == '#' && atLineStart) {
                t = emitPreprocessorOrDefine();
            } else if (c == '/' && peek(1) == '/') {
                t = emitLineComment();
            } else if (c == '/' && peek(1) == '*') {
                t = emitBlockComment();
            } else if (c == '"') {
                t = emitString();
            } else if (c == '\'') {
                t = emitChar();
            } else if (Character.isDigit(c) || (c == '.' && Character.isDigit(peek(1)))) {
                t = emitNumber();
            } else if (isIdentifierStart(c)) {
                t = emitIdentifierOrKeyword();
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

        if (!syntaxError && !"c".equals(language)) {
            reclassifyAngleBrackets(tokens);
        }

        return tokens;
    }

    private void addToken(List<Token> tokens, Token t) {
        tokens.add(t);
        trackSignificant(t);
    }

    private void trackSignificant(Token t) {
        switch (t.type) {
            case WHITESPACE:
            case NEWLINE:
            case COMMENT_LINE:
            case COMMENT_BLOCK:
            case PREPROCESSOR:
                return;
            default:
                recentSignificant.addLast(t);
                if (recentSignificant.size() > 3) {
                    recentSignificant.removeFirst();
                }
        }
    }

    // ── Named construct detection (for the `{` name stack) ─────────────────────────
    private String computeConstructName() {
        Token[] arr = recentSignificant.toArray(new Token[0]); // oldest..newest
        int n = arr.length;
        if (n >= 2) {
            Token kw = arr[n - 2];
            Token name = arr[n - 1];
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
            Token enumKw = arr[n - 3];
            Token classKw = arr[n - 2];
            Token name = arr[n - 1];
            if ("enum".equals(enumKw.text) && "class".equals(classKw.text)
                    && namedConstructKeywords.contains("enum")
                    && name.type == TokenType.IDENTIFIER) {
                return name.text;
            }
        }
        return null;
    }

    // ── Per-construct emit helpers ──────────────────────────────────────────────────
    private Token emitOpenBrace() {
        String name = null;
        if (preprocessorDepth == 0) {
            name = computeConstructName();
            braceDepth++;
            nameStack.push(name);
        }
        pos++;
        return new Token(TokenType.PUNCT, "{", braceDepth, parenDepth, name);
    }

    private Token emitCloseBrace() {
        String name = null;
        if (preprocessorDepth == 0) {
            if (braceDepth == 0) {
                syntaxError = true;
            }
            name = nameStack.isEmpty() ? null : nameStack.pop();
            braceDepth--;
        }
        pos++;
        return new Token(TokenType.PUNCT, "}", braceDepth, parenDepth, name);
    }

    private Token emitOpenBracket(char c) {
        if (preprocessorDepth == 0) {
            parenDepth++;
        }
        pos++;
        return new Token(TokenType.PUNCT, String.valueOf(c), braceDepth, parenDepth, null);
    }

    private Token emitCloseBracket(char c) {
        if (preprocessorDepth == 0) {
            parenDepth--;
        }
        pos++;
        return new Token(TokenType.PUNCT, String.valueOf(c), braceDepth, parenDepth, null);
    }

    private Token emitPunct(char c) {
        pos++;
        return new Token(TokenType.PUNCT, String.valueOf(c), braceDepth, parenDepth, null);
    }

    private Token emitPreprocessorOrDefine() {
        int p = pos + 1; // skip '#'
        while (p < length && (source.charAt(p) == ' ' || source.charAt(p) == '\t')) {
            p++;
        }
        int wordStart = p;
        while (p < length && isIdentifierPart(source.charAt(p))) {
            p++;
        }
        String directive = source.substring(wordStart, p);

        if ("define".equals(directive)) {
            return isMultilineDirective(p) ? emitMacroDef() : emitSingleLineDefine();
        }
        return emitPreprocessor();
    }

    private boolean isMultilineDirective(int from) {
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
        int nameStart = pos;
        while (pos < length && isIdentifierPart(source.charAt(pos))) {
            pos++;
        }
        String name = source.substring(nameStart, pos);

        String paramList = "";
        if (pos < length && source.charAt(pos) == '(') {
            int paramStart = pos;
            int depth = 0;
            do {
                char c = source.charAt(pos);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }
                pos++;
            } while (pos < length && depth > 0);
            paramList = source.substring(paramStart, pos);
        }

        int restStart = pos;
        while (pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r') {
            pos++;
        }
        String rest = source.substring(restStart, pos);

        String text = "#define " + name + paramList + rest;
        return new Token(TokenType.PREPROCESSOR, text, braceDepth, parenDepth, null);
    }

    private Token emitMacroDef() {
        int start = pos;
        while (true) {
            while (pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r') {
                pos++;
            }
            int q = pos - 1;
            while (q >= start && (source.charAt(q) == ' ' || source.charAt(q) == '\t')) {
                q--;
            }
            boolean continues = q >= start && source.charAt(q) == '\\';
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

    private Token emitPreprocessor() {
        int start = pos;
        pos++; // consume '#'
        while (pos < length && (source.charAt(pos) == ' ' || source.charAt(pos) == '\t')) {
            pos++;
        }
        int wordStart = pos;
        while (pos < length && isIdentifierPart(source.charAt(pos))) {
            pos++;
        }
        String directive = source.substring(wordStart, pos);
        while (pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r') {
            pos++;
        }
        String text = source.substring(start, pos);

        if ("if".equals(directive) || "ifdef".equals(directive) || "ifndef".equals(directive)) {
            preprocessorDepth++;
        } else if ("endif".equals(directive)) {
            preprocessorDepth = Math.max(0, preprocessorDepth - 1);
        }

        return new Token(TokenType.PREPROCESSOR, text, braceDepth, parenDepth, null);
    }

    private Token emitLineComment() {
        int start = pos;
        pos += 2;
        while (pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r') {
            pos++;
        }
        return new Token(TokenType.COMMENT_LINE, source.substring(start, pos), braceDepth,
                parenDepth, null);
    }

    private Token emitBlockComment() {
        int start = pos;
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

    private Token emitString() {
        int start = pos;
        pos++;
        while (pos < length) {
            char c = source.charAt(pos);
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
        return new Token(TokenType.STRING, source.substring(start, pos), braceDepth, parenDepth,
                null);
    }

    private Token emitChar() {
        int start = pos;
        pos++;
        while (pos < length) {
            char c = source.charAt(pos);
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
        int start = pos;
        while (pos < length) {
            char c = source.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '\'') {
                pos++;
                continue;
            }
            if ((c == '+' || c == '-') && pos > start) {
                char prev = source.charAt(pos - 1);
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
        int start = pos;
        while (pos < length && isIdentifierPart(source.charAt(pos))) {
            pos++;
        }
        String text = source.substring(start, pos);
        TokenType type = keywords.contains(text) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
        return new Token(type, text, braceDepth, parenDepth, null);
    }

    private Token emitOperator() {
        for (String op : MULTI_CHAR_OPS) {
            if (source.startsWith(op, pos)) {
                pos += op.length();
                return new Token(TokenType.OP, op, braceDepth, parenDepth, null);
            }
        }
        char c = source.charAt(pos);
        pos++;
        return new Token(TokenType.OP, String.valueOf(c), braceDepth, parenDepth, null);
    }

    private Token emitWhitespace() {
        int start = pos;
        while (pos < length && (source.charAt(pos) == ' ' || source.charAt(pos) == '\t')) {
            pos++;
        }
        return new Token(TokenType.WHITESPACE, source.substring(start, pos), braceDepth,
                parenDepth, null);
    }

    private Token emitNewline() {
        int start = pos;
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

    // ── Generic/template angle bracket disambiguation ───────────────────────────────
    private void reclassifyAngleBrackets(List<Token> tokens) {
        List<Integer> sig = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            TokenType ty = tokens.get(i).type;
            if (ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE
                    || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK
                    || ty == TokenType.PREPROCESSOR) {
                continue;
            }
            sig.add(i);
        }

        Deque<int[]> openStack = new ArrayDeque<>(); // each entry: {tokenIndex, validFlag}

        for (int s = 0; s < sig.size(); s++) {
            int idx = sig.get(s);
            Token cur = tokens.get(idx);

            if (cur.type == TokenType.PUNCT
                    && (";".equals(cur.text) || "{".equals(cur.text) || "}".equals(cur.text))) {
                openStack.clear();
                continue;
            }

            if (cur.type == TokenType.OP && "<".equals(cur.text)) {
                Token prev = s > 0 ? tokens.get(sig.get(s - 1)) : null;
                if (prev != null && prev.type == TokenType.IDENTIFIER) {
                    openStack.push(new int[] {idx, 1});
                } else if (!openStack.isEmpty()) {
                    invalidateAll(openStack);
                }
                continue;
            }

            if (cur.type == TokenType.OP && ">".equals(cur.text)) {
                if (!openStack.isEmpty()) {
                    int[] open = openStack.pop();
                    if (open[1] == 1) {
                        tokens.set(open[0], retype(tokens.get(open[0]), TokenType.ANGLE_BRACKET_OPEN));
                        tokens.set(idx, retype(cur, TokenType.ANGLE_BRACKET_CLOSE));
                    }
                }
                continue;
            }

            if (cur.type == TokenType.OP && ">>".equals(cur.text)) {
                if (openStack.size() >= 2) {
                    int[] inner = openStack.pop();
                    int[] outer = openStack.pop();
                    if (inner[1] == 1 && outer[1] == 1) {
                        tokens.set(inner[0], retype(tokens.get(inner[0]), TokenType.ANGLE_BRACKET_OPEN));
                        tokens.set(outer[0], retype(tokens.get(outer[0]), TokenType.ANGLE_BRACKET_OPEN));
                        tokens.set(idx, retype(cur, TokenType.ANGLE_BRACKET_CLOSE));
                        tokens.add(idx + 1, new Token(TokenType.ANGLE_BRACKET_CLOSE, "",
                                cur.braceDepth, cur.parenDepth, null));
                        shiftSigAfter(sig, s, idx + 1);
                    }
                } else if (openStack.size() == 1) {
                    int[] open = openStack.pop();
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

    private void shiftSigAfter(List<Integer> sig, int afterPos, int newIndex) {
        for (int j = afterPos + 1; j < sig.size(); j++) {
            sig.set(j, sig.get(j) + 1);
        }
        sig.add(afterPos + 1, newIndex);
    }

    private void invalidateAll(Deque<int[]> stack) {
        for (int[] entry : stack) {
            entry[1] = 0;
        }
    }

    private boolean isGenericSafeToken(Token t) {
        switch (t.type) {
            case IDENTIFIER:
            case NUMBER:
            case STRING:
            case CHAR:
            case ANGLE_BRACKET_OPEN:
            case ANGLE_BRACKET_CLOSE:
                return true;
            case KEYWORD:
                return "extends".equals(t.text) || "super".equals(t.text)
                        || "const".equals(t.text) || "typename".equals(t.text)
                        || "class".equals(t.text);
            case PUNCT:
                return ",".equals(t.text) || "[".equals(t.text) || "]".equals(t.text);
            case OP:
                return ".".equals(t.text) || "::".equals(t.text) || "?".equals(t.text)
                        || "*".equals(t.text) || "&".equals(t.text);
            default:
                return false;
        }
    }

    private Token retype(Token t, TokenType newType) {
        return new Token(newType, t.text, t.braceDepth, t.parenDepth, t.name);
    }

    private boolean isPreprocessorLanguage() {
        return !"java".equals(language);
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private char peek(int offset) {
        int p = pos + offset;
        return p < length ? source.charAt(p) : '\0';
    }
}
