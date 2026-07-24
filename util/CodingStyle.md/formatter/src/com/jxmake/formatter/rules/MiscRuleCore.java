/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.classifier.CommentClassifier;
import com.jxmake.formatter.classifier.CommentDecision;
import com.jxmake.formatter.classifier.CommentFeatureExtractor;
import com.jxmake.formatter.evaluator.ComplexityPaddingEvaluator;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Family-agnostic base for {@link MiscRuleCurly} (and, in the future, {@code MiscRuleIndent}/
 * {@code MiscRuleTags}) -- everything in this file used to live directly in {@code MiscRule}
 * before the curly/indent/tags class-refactor. No behavior change, mechanical move only (see
 * STATE_COMMON.md's Class Refactor section).
 */
public abstract class MiscRuleCore {

    protected final Lang lang;
    protected final boolean normalizeCommentStartCase;
    protected final boolean normalizeCommentEndPeriod;
    protected final boolean commentNormalizationClassifier;
    public final int indentWidth;
    public final int lineLengthLimit;
    protected final String indentUnit;

    protected MiscRuleCore(final Lang lang, final boolean normalizeCommentStartCase,
            final boolean normalizeCommentEndPeriod, final boolean commentNormalizationClassifier,
            final int indentWidth, final int lineLengthLimit) {
        this.lang = lang;
        this.normalizeCommentStartCase = normalizeCommentStartCase;
        this.normalizeCommentEndPeriod = normalizeCommentEndPeriod;
        this.commentNormalizationClassifier = commentNormalizationClassifier;
        this.indentWidth = indentWidth;
        this.lineLengthLimit = lineLengthLimit;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentWidth; i++) {
            sb.append(' ');
        }
        this.indentUnit = sb.toString();
    }

    protected static Set<String> setOf(final String... words) {
        return new HashSet<>(Arrays.asList(words));
    }

    // ── §1 Indentation ───────────────────────────────────────────────────────────
    /** Tab display size and spaces-per-level default, per STYLE.md §1 -- overridable via the
     *  `indent-size` config key (see {@link #indentWidth}). Shared by any rule (this one or a
     *  future one, e.g. §8's signature wrapping) that needs to *generate* new indentation. */
    public static final int DEFAULT_INDENT_WIDTH = 4;

    public static final int DEFAULT_LINE_LENGTH_LIMIT = 100;

protected static final Set<String> COMMENT_NO_CAPITALIZE_C = setOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
            "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long",
            "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct",
            "switch", "typedef", "union", "unsigned", "void", "volatile", "while");
protected static final Set<String> COMMENT_NO_CAPITALIZE_CPP = setOf(
            "alignas", "alignof", "asm", "bool", "catch", "char16_t", "char32_t", "class",
            "co_await", "co_return", "co_yield", "concept", "consteval", "constexpr", "constinit",
            "const_cast", "decltype", "delete", "dynamic_cast", "explicit", "export", "false",
            "final", "friend", "mutable", "namespace", "new", "noexcept", "nullptr", "operator",
            "override", "private", "protected", "public", "reinterpret_cast", "requires",
            "static_assert", "static_cast", "template", "this", "thread_local", "throw", "true",
            "try", "typeid", "typename", "using", "virtual", "wchar_t");
protected static final Set<String> COMMENT_NO_CAPITALIZE_JAVA = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "else", "enum", "extends", "final", "finally",
            "for", "goto", "if", "implements", "import", "instanceof", "interface", "native",
            "new", "package", "permits", "private", "protected", "public", "record", "return",
            "sealed", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
            "throws", "transient", "try", "var", "void", "volatile", "while", "yield", "null",
            "true", "false");

protected static final Set<String> TIGHT_PAREN_KEYWORDS =
            setOf("if", "while", "for", "switch", "catch", "when");
    /**
     * Converts every line's leading indentation run to the requested style, per STYLE.md §1's
     * `indent-style = spaces | tabs` modes (resolved -- see "§1 indentation scope" in Resolved
     * Design Decisions). `indent-style = auto` is deliberately not handled here: it requires
     * whole-project context to determine the dominant style, which is a `Main.java`/
     * `Config.java`-orchestration-time decision made by a separate, not-yet-built detector class
     * -- that class is expected to resolve "auto" down to a concrete `spaces`/`tabs` choice and
     * call this method with that choice, so this method itself never has to interpret "auto".
     * Only the whitespace run at the very start of each line is touched; whitespace elsewhere
     * (mid-line alignment padding, trailing whitespace) is never indentation. A line whose
     * indentation width (tabs expanded at {@link #indentWidth}) is not an exact multiple of
     * {@link #indentWidth} is irregular/malformed indentation and is left completely untouched
     * rather than guessed at.
     */
    public String convertIndentation(final List<Token> tokens, final String indentStyle) {
        if (!"spaces".equals(indentStyle) && !"tabs".equals(indentStyle)) {
            throw new IllegalArgumentException("convertIndentation only handles spaces|tabs, got: " + indentStyle);
        }
        final StringBuilder out = new StringBuilder();
        boolean atLineStart = true;
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (atLineStart && t.type == TokenType.WHITESPACE && !t.frozen) {
                out.append(renderIndent(t.text, indentStyle));
                atLineStart = false;
                i++;
                continue;
            }
            out.append(t.text);
            atLineStart = (t.type == TokenType.NEWLINE);
            i++;
        }
        return out.toString();
    }
protected String renderIndent(final String original, final String indentStyle) {
        final int width = expandedIndentWidth(original);
        if (width % indentWidth != 0) {
            return original;
        }
        return indentText(width / indentWidth, indentStyle);
    }
    /** Column width of a raw (not-yet-{@link #convertIndentation}-normalized) leading-whitespace
     *  run, expanding each {@code '\t'} to the next {@link #indentWidth} tab stop the same way
     *  {@link #renderIndent} does -- unlike {@code original.length()}, which undercounts a tab as
     *  a single character. Any line-length fits-check that measures a physical line still
     *  containing raw source indentation (i.e. anything that runs before {@code
     *  convertIndentation}, which is always the pipeline's last phase) must use this instead of
     *  {@code String.length()} on that indent, or a tab-indented line whose true post-conversion
     *  width would exceed {@code lineLengthLimit} can wrongly measure as fitting -- stable only on
     *  a second format pass, once the indent is already spaces from the start (found via real-code
     *  testing, local `src/com`/`src/org` dogfood -- `enforceCallLineBreaking`'s whole-line/
     *  candidate fits-checks). */
    protected int expandedIndentWidth(final String original) {
        int width = 0;
        for (int i = 0; i < original.length(); i++) {
            width += (original.charAt(i) == '\t') ? (indentWidth - (width % indentWidth)) : 1;
        }
        return width;
    }
    /** Renders `level` indent levels in the requested style -- shared by §1's line converter
     *  above and §8's signature-wrapping below, which both need to *generate* brand-new
     *  indentation (as opposed to converting indentation that already exists in source). */
    protected String indentText(final int level, final String indentStyle) {
        final boolean tabs = "tabs".equals(indentStyle);
        final char unit = tabs ? '\t' : ' ';
        final int count = tabs ? level : level * indentWidth;
        final StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(unit);
        }
        return sb.toString();
    }
    /**
     * Collapses any whitespace-only gap between a control-flow keyword (`if`/`while`/`for`/
     * `switch`/`catch` -- STYLE.md §3.2's keywords, plus Kotlin's `when` per STYLE_KOTLIN.md
     * §3.2, harmless for C/C++/Java since they have no `when` keyword at all) and its following
     * `(` down to zero width. A comment or a `NEWLINE` in the gap blocks the rewrite for that
     * occurrence, same
     * conservative posture as `BlockStructureRule`'s brace-style passes -- relocating a comment
     * unambiguously is out of scope. This method never touches what's *inside* the `(...)`;
     * deciding whether the contents are padded is STYLE.md §3.1, already implemented as the
     * `isLoose` evaluation in `ComplexityPaddingEvaluator` -- wiring that evaluation into an
     * actual rewrite pass is separate, not-yet-assigned work.
     */
    public String enforceKeywordSpacing(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        Token lastSignificant = null;
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                i++;
                continue;
            }

            final boolean collapse = isPunct(t, "(") && lastSignificant != null
                    && lastSignificant.type == TokenType.KEYWORD
                    && TIGHT_PAREN_KEYWORDS.contains(lastSignificant.text)
                    && gap.stream().noneMatch(this::isCommentOrNewline)
                    && !t.frozen && !lastSignificant.frozen && gap.stream().noneMatch(g -> g.frozen);
            if (!collapse) {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            }
            gap.clear();
            out.append(t.text);
            lastSignificant = t;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }
protected static final ComplexityPaddingEvaluator COMPLEXITY_EVALUATOR = new ComplexityPaddingEvaluator();
    /**
     * Pads or tightens every `(...)` and `[...]` in the token stream per STYLE.md §3.1: exactly
     * one space just inside both delimiters when {@link ComplexityPaddingEvaluator#isLoose} reports
     * the content "loose" (a nested `(`/`[` is present anywhere in it), or zero width (tight)
     * otherwise. Applies universally -- not limited to control-flow conditions. One exclusion:
     * a `(` whose immediately preceding significant token is an IDENTIFIER and whose matching `)`
     * is followed by `{`, `->`, or a trailing qualifier (`const`/`override`/`noexcept`/`throws`/
     * `final`) is a function/method definition parameter list and is left untouched. A comment or
     * `NEWLINE` in a given side's own gap blocks the rewrite for that side only.
     */
    public String enforceComplexityPadding(final List<Token> tokens) {
        final Map<Integer, Boolean> looseByOpenIdx = new HashMap<>();
        final Map<Integer, Boolean> looseByCloseIdx = new HashMap<>();
        final int n = tokens.size();

        for (int i = 0; i < n; i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "(")) {
                final int prevSigIdx = prevSignificantIndex(tokens, i - 1);
                if (prevSigIdx >= 0 && tokens.get(prevSigIdx).type == TokenType.IDENTIFIER) {
                    final int closeIdx = matchParenForward(tokens, i);
                    if (closeIdx < 0) {
                        continue;
                    }
                    final int afterCloseIdx = nextSignificantIndex(tokens, closeIdx + 1);
                    if (afterCloseIdx >= 0 && isFunctionDefFollower(tokens.get(afterCloseIdx))) {
                        continue;
                    }
                    final boolean loose = COMPLEXITY_EVALUATOR.isLoose(tokens.subList(i + 1, closeIdx));
                    looseByOpenIdx.put(i, loose);
                    looseByCloseIdx.put(closeIdx, loose);
                } else {
                    final int closeIdx = matchParenForward(tokens, i);
                    if (closeIdx < 0) {
                        continue;
                    }
                    // TS function-type parameter list (`type Foo = (...args: any[]) => void`,
                    // `const x: (...args: any[]) => void`, vuejs/core dogfood
                    // `e2eBrowserUtils.ts`): a standalone function type's own `(...)` is a
                    // parameter list exactly like a named function's, but it sits in type
                    // position (preceded by `=`/`:`/`|`/`&`/`(`/`,`/`<`, never an IDENTIFIER),
                    // so the IDENTIFIER-preceded branch above never recognizes it and this
                    // generic-expression branch was padding/tightening it like an arbitrary
                    // parenthesized value instead of leaving a real parameter list untouched.
                    // Recognized the same way `isFunctionDefFollower` recognizes a real
                    // signature: the matching `)` is immediately followed by `=>`.
                    final int afterCloseIdx = nextSignificantIndex(tokens, closeIdx + 1);
                    if (lang.isTs && afterCloseIdx >= 0 && isOp(tokens.get(afterCloseIdx), "=>")) {
                        continue;
                    }
                    final boolean loose = COMPLEXITY_EVALUATOR.isLoose(tokens.subList(i + 1, closeIdx));
                    looseByOpenIdx.put(i, loose);
                    looseByCloseIdx.put(closeIdx, loose);
                }
            } else if (isPunct(t, "[")) {
                final int closeIdx = matchBracketForward(tokens, i);
                if (closeIdx < 0) {
                    continue;
                }
                final boolean loose = COMPLEXITY_EVALUATOR.isLoose(tokens.subList(i + 1, closeIdx));
                looseByOpenIdx.put(i, loose);
                looseByCloseIdx.put(closeIdx, loose);
            }
        }

        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        Token lastSignificant = null;
        int lastSignificantIdx = -1;
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                i++;
                continue;
            }

            final boolean lastIsOpen = isPunct(lastSignificant, "(") || isPunct(lastSignificant, "[");
            final boolean curIsClose = isPunct(t, ")") || isPunct(t, "]");
            final Boolean afterOpen = lastIsOpen ? looseByOpenIdx.get(lastSignificantIdx) : null;
            final Boolean beforeClose = curIsClose ? looseByCloseIdx.get(i) : null;
            final boolean gapHasBlocker = gap.stream().anyMatch(this::isCommentOrNewline)
                    || t.frozen || (lastSignificant != null && lastSignificant.frozen)
                    || gap.stream().anyMatch(g -> g.frozen);

            if (!gapHasBlocker && (Boolean.TRUE.equals(afterOpen) || Boolean.TRUE.equals(beforeClose))) {
                out.append(' ');
                gap.clear();
            } else if (!gapHasBlocker && (Boolean.FALSE.equals(afterOpen) || Boolean.FALSE.equals(beforeClose))) {
                gap.clear();
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
                gap.clear();
            }

            out.append(t.text);
            lastSignificant = t;
            lastSignificantIdx = i;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }
protected boolean isFunctionDefFollower(final Token t) {
        if (isPunct(t, "{") || isOp(t, "->")) {
            return true;
        }
        if (t.type == TokenType.KEYWORD) {
            final String s = t.text;
            return "const".equals(s) || "override".equals(s) || "noexcept".equals(s)
                    || "throws".equals(s) || "final".equals(s);
        }
        return false;
    }
protected int matchBracketForward(final List<Token> tokens, final int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < tokens.size(); i++) {
            if (isPunct(tokens.get(i), "[")) {
                depth++;
            } else if (isPunct(tokens.get(i), "]")) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    /**
     * Normalizes spacing inside brace-initializer lists (array/struct initializers, `= { ... }`
     * contexts) per STYLE.md §3.3: empty `{}` is always tight; non-empty `{ ... }` gets exactly
     * one space just inside both the opening and closing brace, at every nesting level. Also
     * normalizes comma spacing within the same initializer context (no space before `,`, exactly
     * one space after) -- needed because a brace-initializer whose contents span nested `{...}`
     * groups (e.g. `{ {1,2}, {3,4} }`) is deliberately rejected by
     * `DeclarationAlignmentRule.parseDeclaration` (initializer ending in `}` is left untouched, see
     * its own doc comment) and so never gets comma spacing from that rule's `renderInitTokens`.
     * Control-flow and function/class body braces (STYLE.md §11/§7, already handled by
     * `BlockStructureRule`) are never touched -- those braces are never directly preceded by
     * `=`, `{`, or `,` while nested inside an already-recognized initializer, which is the
     * structural signal this method uses to recognize an initializer brace (no AST available;
     * confirmed against `BlockStructureRule.qualifiesForKAndR` that an initializer brace, whose
     * preceding token is `=`/`{`/`,`, never matches that method's K&R/lambda criteria, so the
     * two rules never fight over the same brace). A comment or `NEWLINE` immediately inside a
     * brace blocks the rewrite for that side, same conservative posture as the rest of this
     * file -- a genuinely multi-line initializer is left untouched.
     */
    public String enforceInitializerBraceSpacing(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        final Deque<Boolean> initStack = new ArrayDeque<>();
        // Parallel stack: true only for the frame directly opened by `=` (a fresh top-level
        // initializer), false for every other frame -- including a nested initializer frame
        // continuing an already-active parent initializer, and any non-initializer scope brace
        // (function/control-flow body) that happens to enclose an initializer. `initStack` alone
        // can't distinguish "outermost" this way since a non-initializer enclosing scope brace
        // (e.g. a function body) also occupies a stack slot, offsetting a naive depth count.
        final Deque<Boolean> outermostStack = new ArrayDeque<>();
        Token lastSignificant = null;
        Token secondLastSignificant = null;
        Token thirdLastSignificant = null;
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                i++;
                continue;
            }

            final boolean inInit = !initStack.isEmpty() && initStack.peek();
            // Inside-brace padding applies only at the outermost initializer level -- STYLE.md
            // §3.3's worked example pads only the outer pair of a nested brace-initializer
            // (`{ {1, 2}, {3, 4} }`), leaving inner element braces tight. Comma spacing, by
            // contrast, applies at every nesting level.
            final boolean atOutermostInit = inInit && !outermostStack.isEmpty() && outermostStack.peek();
            final boolean afterInitOpen = isPunct(lastSignificant, "{") && atOutermostInit;
            final boolean beforeInitClose = isPunct(t, "}") && atOutermostInit;
            final boolean beforeComma = isPunct(t, ",") && inInit;
            final boolean afterComma = isPunct(lastSignificant, ",") && inInit;
            final boolean gapHasBlocker = gap.stream().anyMatch(this::isCommentOrNewline)
                    || t.frozen || (lastSignificant != null && lastSignificant.frozen)
                    || gap.stream().anyMatch(g -> g.frozen);

            if (((afterInitOpen && isPunct(t, "}")) || beforeComma) && !gapHasBlocker) {
                gap.clear();
            } else if ((afterInitOpen || beforeInitClose || afterComma) && !gapHasBlocker) {
                out.append(' ');
                gap.clear();
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
                gap.clear();
            }

            if (isPunct(t, "{")) {
                final boolean startsNewInit = isOp(lastSignificant, "=")
                        || ((lang.isJs || lang.isTs) && isImportBraceHeaderKeyword(lastSignificant, secondLastSignificant))
                        || ((lang.isJs || lang.isTs) && isPunct(lastSignificant, ",")
                                && secondLastSignificant != null && secondLastSignificant.type == TokenType.IDENTIFIER
                                && thirdLastSignificant != null && thirdLastSignificant.type == TokenType.KEYWORD
                                && "import".equals(thirdLastSignificant.text))
                        || ((lang.isJs || lang.isTs) && isPunct(lastSignificant, "("))
                        // JS/TS object-destructuring declaration LHS (`const {id, name} = ...`):
                        // this `{` directly follows a `const`/`let`/`var` keyword, not `=`/`(`/
                        // import-brace like every other case above. STYLE_JS_TS.md §3 requires the
                        // same `{ ... }` padding and comma spacing as any other brace-initializer,
                        // but `JsTsDeclarationAlignmentRule.parseDeclaration` deliberately leaves a
                        // destructuring-pattern LHS unparsed (RDD_KEY_182), so it never gets comma
                        // spacing from that rule's own renderer either -- without this case, a
                        // destructuring `{id,name,...rest}` pattern stayed comma-tight (found via
                        // Checkpoint 21's local-fixture harness testing). Scoped to the keyword
                        // immediately before `{` so it can't affect an ordinary block/control-flow
                        // brace, which is never itself directly preceded by `const`/`let`/`var`.
                        || ((lang.isJs || lang.isTs) && lastSignificant != null
                                && lastSignificant.type == TokenType.KEYWORD
                                && ("const".equals(lastSignificant.text) || "let".equals(lastSignificant.text)
                                        || "var".equals(lastSignificant.text)));
                final boolean isInit = startsNewInit
                        || ((isPunct(lastSignificant, "{") || isPunct(lastSignificant, ","))
                                && !initStack.isEmpty() && initStack.peek());
                initStack.push(isInit);
                outermostStack.push(startsNewInit);
            } else if (isPunct(t, "}") && !initStack.isEmpty()) {
                initStack.pop();
                outermostStack.pop();
            }

            out.append(t.text);
            thirdLastSignificant = secondLastSignificant;
            secondLastSignificant = lastSignificant;
            lastSignificant = t;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }
    /**
     * JS/TS-only (checked by the caller): a named-import list's `{` -- immediately preceded by
     * the {@code import} keyword itself, or by {@code type} where the token before that is
     * {@code import} (TS's {@code import type { Foo } from "...";}) -- is treated as an
     * initializer-shaped brace for §3.3 padding purposes the same as a `= { ... }` initializer,
     * even though it has no preceding `=`. Mirrors {@code JsTsSpecificRule.classifyBraces}'s own
     * `isImportBraceHeader` lookback (same two-token shape), kept as a separate, narrower copy
     * here rather than shared/extracted -- that method also decides semicolon-insertion depth
     * behavior, a different concern from this class's brace-content spacing, and this base class
     * has no dependency on `JsTsSpecificRule`. No effect on C/C++/Java/Kotlin -- `import` is not
     * even a keyword in those languages' `KEYWORDS_*` sets, so `lastSig.type == KEYWORD &&
     * "import".equals(...)` can never match there regardless of this method's own `lang.isJs ||
     * lang.isTs` gate at the call site. A combined default+named import (`import Widget, {a, b}
     * from "...";`) is handled by a separate disjunct at the call site (not this helper) that
     * additionally recognizes `{` preceded by `,` preceded by an IDENTIFIER preceded by the
     * `import` keyword -- `classifyBraces`'s own `isImportBraceHeader` doesn't need this shape
     * since §2's semicolon-insertion concern only cares about the *first* `{` on a line for depth
     * purposes there, but §3.3 padding must recognize every named-list brace shape.
     */
    private boolean isImportBraceHeaderKeyword(final Token lastSig, final Token secondLastSig) {
        if (lastSig == null || lastSig.type != TokenType.KEYWORD) {
            return false;
        }
        if ("import".equals(lastSig.text)) {
            return true;
        }
        return "type".equals(lastSig.text) && secondLastSig != null
                && secondLastSig.type == TokenType.KEYWORD && "import".equals(secondLastSig.text);
    }
int matchParenForward(final List<Token> tokens, final int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < tokens.size(); i++) {
            if (isPunct(tokens.get(i), "(")) {
                depth++;
            } else if (isPunct(tokens.get(i), ")")) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
protected boolean isStatementBoundary(final Token t) {
        return t == null || isPunct(t, ";") || isPunct(t, "{") || isPunct(t, "}");
    }
protected boolean isIncrementOp(final Token t) {
        return t.type == TokenType.OP && ("++".equals(t.text) || "--".equals(t.text));
    }
protected boolean noBlockerBetween(final List<Token> tokens, final int fromExclusive, final int toExclusive) {
        for (int i = fromExclusive + 1; i < toExclusive; i++) {
            if (isCommentOrNewline(tokens.get(i))) {
                return false;
            }
        }
        return true;
    }
protected int nextSignificantIndex(final List<Token> tokens, final int from) {
        int i = from;
        while (i < tokens.size() && isGapToken(tokens.get(i))) {
            i++;
        }
        return i < tokens.size() ? i : -1;
    }
protected static final Set<String> ASSIGNMENT_OPS = setOf(
            "=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=");
public static final class Assignment {
        public final Token target;
        public final String lhsText; // full rendered LHS (may span multiple tokens)
        public final Token operator;
        public final List<Token> valueTokens;
        public final Token trailingComment; // nullable
        public final boolean blankLineBefore;
        public final boolean multiLine;
        public final boolean breakBeforeOperator; // only meaningful when multiLine
        public final List<Token> firstLineValueTokens; // only set when multiLine
        public final List<Token> secondLineValueTokens; // only set when multiLine

        private Assignment(final Token target, final String lhsText, final Token operator,
                final List<Token> valueTokens, final Token trailingComment,
                final boolean blankLineBefore, final boolean multiLine,
                final boolean breakBeforeOperator, final List<Token> firstLineValueTokens,
                final List<Token> secondLineValueTokens) {
            this.target = target;
            this.lhsText = lhsText;
            this.operator = operator;
            this.valueTokens = valueTokens;
            this.trailingComment = trailingComment;
            this.blankLineBefore = blankLineBefore;
            this.multiLine = multiLine;
            this.breakBeforeOperator = breakBeforeOperator;
            this.firstLineValueTokens = firstLineValueTokens;
            this.secondLineValueTokens = secondLineValueTokens;
        }

        static Assignment singleLine(final Token target, final String lhsText, final Token operator,
                final List<Token> valueTokens, final Token trailingComment,
                final boolean blankLineBefore) {
            return new Assignment(target, lhsText, operator, valueTokens, trailingComment,
                    blankLineBefore, false, false, null, null);
        }

        static Assignment multiLine(final Token target, final String lhsText, final Token operator,
                final boolean breakBeforeOperator, final List<Token> firstLineValueTokens,
                final List<Token> secondLineValueTokens, final Token trailingComment,
                final boolean blankLineBefore) {
            return new Assignment(target, lhsText, operator, null, trailingComment,
                    blankLineBefore, true, breakBeforeOperator, firstLineValueTokens,
                    secondLineValueTokens);
        }
    }
    /**
     * Splits one scope's direct-content tokens (caller-extracted, no deeper-nested tokens --
     * same scoping contract as `DeclarationAlignmentRule.groupDeclarations`) into maximal runs of
     * textually-adjacent bare assignment statements (STYLE.md §6, resolved -- see "§6 grouping
     * and rendering" in Resolved Design Decisions: same textually-adjacent-run signal as §14). A
     * blank line, a comment-only gap, or any statement not recognized as a bare assignment breaks
     * the current run. Unlike `GetterSetterRule.groupOneLiners`'s 2+ minimum, a run of length 1 is
     * still returned here -- STYLE.md §6 explicitly wants a lone variable to "align trivially with
     * itself," which {@link #render} achieves for free (group size 1 means both padding widths
     * just equal that one row's own widths).
     */
    public List<List<Assignment>> groupAssignments(final List<Token> scopeTokens) {
        final List<List<Token>> statements = splitAssignmentStatements(scopeTokens);
        final List<List<Assignment>> groups = new ArrayList<>();
        List<Assignment> current = new ArrayList<>();

        for (final List<Token> stmt : statements) {
            final boolean blankBefore = hasBlankLineBeforeStmt(stmt);
            final Assignment a = parseAssignment(stmt, blankBefore);
            if (a == null) {
                if (!current.isEmpty()) {
                    groups.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            if (blankBefore && !current.isEmpty()) {
                groups.add(current);
                current = new ArrayList<>();
            }
            current.add(a);
        }
        if (!current.isEmpty()) {
            groups.add(current);
        }
        return groups;
    }
    /**
     * Renders one alignment group (STYLE.md §6) as two independently fixed-width columns per
     * row -- `maxNameLen` (the widest target name) and `maxPrefixLen` (the widest operator text
     * minus its trailing `=`) -- so that every row's `=` lands on the same column regardless of
     * which compound operator it uses (resolved -- see "§6 grouping and rendering": a single
     * `ColumnGrid` left-pad column on the concatenated name+operator text does NOT reproduce
     * STYLE.md's worked example; this manual two-field padding does). The right-hand side is
     * never reformatted -- its original token text (including internal spacing) is reproduced
     * verbatim, since STYLE.md describes alignment of the `=` column only, not a rewrite of
     * arbitrary expression spacing. An optional trailing-comment column reuses `ColumnGrid` to
     * align comments across the group, same precedent as `DeclarationAlignmentRule`/
     * `GetterSetterRule`. A {@code multiLine} row (STYLE.md §6's "Multi-line right-hand sides")
     * cannot participate in that `ColumnGrid` pass -- its single `value+";"` cell would only ever
     * hold the first physical line, so any later comment-column padding computed from it would be
     * wrong -- so such rows are rendered separately by {@link #renderMultiLine} and spliced back
     * into the group's line order afterward; their own trailing comment (rare, undocumented by any
     * worked example) is appended directly after the second line's `;` rather than column-aligned.
     */
    public List<String> render(final List<Assignment> group) {
        int maxNameLen = 0;
        int maxPrefixLen = 0;
        for (final Assignment a : group) {
            maxNameLen = Math.max(maxNameLen, a.lhsText.length());
            maxPrefixLen = Math.max(maxPrefixLen, assignOpPrefix(a.operator).length());
        }
        // +1 unconditionally -- even the group's widest operator still needs its own leading
        // space (verified against STYLE.md's worked example: maxPrefixLen=2 from ">>=" there,
        // but every row's rendered gap is 3, i.e. naturalMax+1, not naturalMax)
        maxPrefixLen++;
        final int lhsWidth = maxNameLen + maxPrefixLen + 1; // +1 for "="

        final ColumnGrid grid = new ColumnGrid();
        for (final Assignment a : group) {
            if (a.multiLine) {
                continue;
            }
            final String lhs = padRight(a.lhsText, maxNameLen)
                    + padLeft(assignOpPrefix(a.operator), maxPrefixLen) + "=";
            final List<String> cells = new ArrayList<>();
            cells.add(lhs);
            cells.add(joinVerbatim(a.valueTokens) + ";");
            if (a.trailingComment != null) {
                cells.add(a.trailingComment.text);
            }
            grid.addRow(cells.toArray(new String[0]));
        }
        final List<String[]> flushed = grid.flush();

        final List<String> lines = new ArrayList<>();
        int flushIdx = 0;
        for (final Assignment a : group) {
            if (a.multiLine) {
                lines.addAll(renderMultiLine(a, maxNameLen, maxPrefixLen, lhsWidth));
            } else {
                lines.add(String.join(" ", flushed.get(flushIdx)));
                flushIdx++;
            }
        }
        return lines;
    }
    /**
     * Renders one multi-line-right-hand-side row (STYLE.md §6) as exactly two lines: line 1 is
     * `lhs + " " + firstLineValueTokens` (identical shape to the single-line case); line 2 is
     * indentation alone, sized to land the continuation at the documented target column, followed
     * by `secondLineValueTokens` and the terminating `;`. Breaking *before* an operator (the
     * operator is the first token of {@code secondLineValueTokens}) targets the `=` column itself
     * -- index `lhsWidth - 1`, since `lhs` is exactly `lhsWidth` characters wide and ends in `=`.
     * Breaking *after* an operator (the operator is the last token of {@code
     * firstLineValueTokens}) targets the column immediately after `=`, i.e. where the first
     * operand began on line 1 -- index `lhsWidth + 1` (`lhs` then one space then the operand).
     * Both target columns are computed from the whole group's `lhsWidth`, not this row's own
     * unpadded name/operator length, so a multi-line row's continuation lines up correctly even
     * when other rows in the same group have longer names/operators.
     */
    protected List<String> renderMultiLine(final Assignment a, final int maxNameLen,
            final int maxPrefixLen, final int lhsWidth) {
        final String lhs = padRight(a.lhsText, maxNameLen)
                + padLeft(assignOpPrefix(a.operator), maxPrefixLen) + "=";
        final String line1 = lhs + " " + joinVerbatim(a.firstLineValueTokens);

        final int indentLen = a.breakBeforeOperator ? lhsWidth - 1 : lhsWidth + 1;
        final StringBuilder line2 = new StringBuilder(padRight("", indentLen));
        line2.append(joinVerbatim(a.secondLineValueTokens)).append(';');
        if (a.trailingComment != null) {
            line2.append(' ').append(a.trailingComment.text);
        }
        return Arrays.asList(line1, line2.toString());
    }
protected String assignOpPrefix(final Token operator) {
        return operator.text.substring(0, operator.text.length() - 1);
    }
protected String joinVerbatim(final List<Token> tokens) {
        final StringBuilder sb = new StringBuilder();
        for (final Token t : tokens) {
            sb.append(t.text);
        }
        return sb.toString();
    }
protected static String padRight(final String s, final int width) {
        final StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }
protected static String padLeft(final String s, final int width) {
        final StringBuilder sb = new StringBuilder();
        while (sb.length() + s.length() < width) {
            sb.append(' ');
        }
        sb.append(s);
        return sb.toString();
    }
    /**
     * Parses the shape `target op value ;` (STYLE.md §6), either entirely on one source line, or
     * spanning exactly two (STYLE.md §6's "Multi-line right-hand sides", see {@link
     * #classifyMultiLineBreak}). `target` must be a single bare `IDENTIFIER` -- a member access
     * (`obj.field`), an array element (`arr[i]`), or a pointer deref (`*ptr`) has no STYLE.md
     * worked example to justify guessing at, so any of those leave this method returning null (the
     * statement is left untouched and breaks the current alignment run, same as any other
     * unrecognized statement). A comment or `NEWLINE` between the target and the first value token
     * (i.e. the `target op` portion must itself be on one line) blocks recognition entirely, as
     * does any comment anywhere in the value -- only a single `NEWLINE` inside the value, at a
     * point `classifyMultiLineBreak` can classify as breaking directly before or after an
     * operator, is accepted; two or more `NEWLINE`s, or a break point unrelated to an operator
     * (e.g. mid-operand), have no STYLE.md worked example and are left untouched.
     */
    protected Assignment parseAssignment(final List<Token> stmt, final boolean blankBefore) {
        final int targetIdx = nextSignificantIndex(stmt, 0);
        if (targetIdx < 0) {
            return null;
        }
        final TokenType targetType = stmt.get(targetIdx).type;
        if (targetType != TokenType.IDENTIFIER && targetType != TokenType.KEYWORD) {
            return null;
        }
        // `auto [a, b] = expr;` (a C++17 structured binding) starts with the `auto` keyword
        // followed by a `[` -- that's DeclarationAlignmentRule's shape, not an assignment to a
        // subscript of a variable literally named "auto". Left unrecognized here so the
        // declaration pass's rendering isn't re-parsed and re-collapsed by this pass.
        if (targetType == TokenType.KEYWORD && "auto".equals(stmt.get(targetIdx).text)) {
            return null;
        }
        // Same shape, JS/TS side: `const [a, b, ...c] = expr;` (array-destructuring
        // declaration) starts with `const`/`let`/`var` followed by `[` -- that's
        // JsTsDeclarationAlignmentRule's shape, not an assignment to a subscript of a variable
        // literally named "const"/"let"/"var". Without this bail-out, this method's generic
        // `[`-as-subscript scan below still matched it, and this class's own JS/TS-unaware
        // `renderTokens`/`isTightToken` (which treats `...` as tight on BOTH sides, unlike
        // JsTsDeclarationAlignmentRule's JS/TS-aware "space before, tight after" rule) re-
        // rendered and re-spliced the already-correctly-rendered pattern, corrupting `[first,
        // second, ...others]` into `[first, second,... others]` (object-destructuring `{...}`
        // patterns were never affected -- `{` isn't one of this scan's recognized LHS shapes).
        if ((lang.isJs || lang.isTs) && targetType == TokenType.KEYWORD
                && ("const".equals(stmt.get(targetIdx).text) || "let".equals(stmt.get(targetIdx).text)
                        || "var".equals(stmt.get(targetIdx).text))) {
            return null;
        }
        // Scan forward to find the assignment operator, allowing member-access chains
        // (obj.field, ptr->field) and subscript expressions (arr[i]) in the LHS.
        // State 0: after target/field, expecting op or member-access operator.
        // State 1: after . or ->, expecting the field-name IDENTIFIER.
        // State 2: inside [...] subscript (depth-tracked).
        int opIdx = -1;
        int scanState = 0;
        int scanDepth = 0;
        for (int k = targetIdx + 1; k < stmt.size(); k++) {
            final Token t = stmt.get(k);
            if (isGapToken(t)) { continue; }
            if (scanState == 2) {
                if (isPunct(t, "[")) { scanDepth++; }
                else if (isPunct(t, "]")) { scanDepth--; if (scanDepth == 0) { scanState = 0; } }
                else if (isPunct(t, ";")) { return null; }
                continue;
            }
            if (scanState == 1) {
                if (t.type != TokenType.IDENTIFIER) { return null; }
                scanState = 0;
                continue;
            }
            if (t.type == TokenType.OP && ASSIGNMENT_OPS.contains(t.text)) {
                opIdx = k;
                break;
            }
            if (isOp(t, ".") || isOp(t, "->")) { scanState = 1; continue; }
            if (isPunct(t, "[")) { scanState = 2; scanDepth = 1; continue; }
            return null;
        }
        if (opIdx < 0) { return null; }

        // Render the full LHS for alignment (handles single- and multi-token LHS).
        final List<Token> lhsSigTokens = new ArrayList<>();
        for (int k = targetIdx; k < opIdx; k++) {
            final Token t = stmt.get(k);
            if (!isGapToken(t)) { lhsSigTokens.add(t); }
        }
        final String lhsText = renderTokens(lhsSigTokens);

        final int valueFrom = nextSignificantIndex(stmt, opIdx + 1);
        if (valueFrom < 0 || !noBlockerBetween(stmt, targetIdx, valueFrom)) {
            return null;
        }
        final int semiIdx = findTopLevelSemicolon(stmt, valueFrom);
        if (semiIdx < 0) {
            return null;
        }
        int valueTo = semiIdx;
        while (valueTo > valueFrom && isGapToken(stmt.get(valueTo - 1))) {
            valueTo--;
        }
        if (valueTo <= valueFrom) {
            return null;
        }
        for (int i = semiIdx + 1; i < stmt.size(); i++) {
            final Token t = stmt.get(i);
            if (t.type != TokenType.WHITESPACE && t.type != TokenType.COMMENT_LINE
                    && t.type != TokenType.COMMENT_BLOCK) {
                return null; // stray tokens after `;` -- not a clean single statement
            }
        }

        int newlineCount = 0;
        int newlineIdx = -1;
        for (int i = valueFrom; i < valueTo; i++) {
            final Token t = stmt.get(i);
            if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                return null;
            }
            if (t.type == TokenType.NEWLINE) {
                newlineCount++;
                newlineIdx = i;
            }
        }
        final Token trailingComment = findTrailingAssignComment(stmt);
        if (newlineCount == 0) {
            final List<Token> value = new ArrayList<>(stmt.subList(valueFrom, valueTo));
            return Assignment.singleLine(stmt.get(targetIdx), lhsText, stmt.get(opIdx), value,
                    trailingComment, blankBefore);
        }

        // STYLE.md §6's own "multi-line right-hand side" shape is exactly one newline, split at
        // an operator. Anything else here (more than one newline, or a single newline that isn't
        // an operator break) is most likely a call whose arguments `MiscRule.enforceCallLineBreaking`
        // already wrapped across lines on a prior pass, once fed back in as this pass's input --
        // reject re-deriving that shape and fall through to the verbatim branch below instead of
        // returning null, so this row still participates in the group's LHS/`=` alignment (using
        // its own `lhsText` for width) rather than splitting the run into smaller subgroups, which
        // would otherwise make a fresh format and a reformat of that format's own already-wrapped
        // output disagree on group membership/padding (non-idempotent).
        if (newlineCount == 1) {
            int line1End = newlineIdx;
            while (line1End > valueFrom && isGapToken(stmt.get(line1End - 1))) {
                line1End--;
            }
            int line2Start = newlineIdx + 1;
            while (line2Start < valueTo && isGapToken(stmt.get(line2Start))) {
                line2Start++;
            }
            if (line1End > valueFrom && line2Start < valueTo) {
                final List<Token> line1 = new ArrayList<>(stmt.subList(valueFrom, line1End));
                final List<Token> line2 = new ArrayList<>(stmt.subList(line2Start, valueTo));
                final Boolean breakBeforeOperator = classifyMultiLineBreak(line1, line2);
                if (breakBeforeOperator != null) {
                    return Assignment.multiLine(stmt.get(targetIdx), lhsText, stmt.get(opIdx),
                            breakBeforeOperator, line1, line2, trailingComment, blankBefore);
                }
            }
        }
        final List<Token> value = new ArrayList<>(stmt.subList(valueFrom, valueTo));
        return Assignment.singleLine(stmt.get(targetIdx), lhsText, stmt.get(opIdx), value,
                trailingComment, blankBefore);
    }
    /**
     * Classifies a multi-line right-hand side's break point per STYLE.md §6: {@code true} if
     * {@code line2}'s first token is an operator ("breaking before an operator"), {@code false} if
     * {@code line1}'s last token is an operator ("breaking after an operator"), {@code null} if
     * neither holds -- no STYLE.md worked example covers a break unrelated to an operator (e.g.
     * mid-operand), so that shape is left unrecognized rather than guessed at. Checked in this
     * order so a (rare, ambiguous) break where both sides touch an operator resolves to the
     * "before" reading.
     */
    protected Boolean classifyMultiLineBreak(final List<Token> line1, final List<Token> line2) {
        if (line2.get(0).type == TokenType.OP) {
            return Boolean.TRUE;
        }
        if (line1.get(line1.size() - 1).type == TokenType.OP) {
            return Boolean.FALSE;
        }
        return null;
    }
protected int findTopLevelSemicolon(final List<Token> tokens, final int from) {
        int depth = 0;
        for (int i = from; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")) {
                depth--;
            } else if (depth == 0 && isPunct(t, ";")) {
                return i;
            }
        }
        return -1;
    }
protected Token findTrailingAssignComment(final List<Token> stmt) {
        for (int k = stmt.size() - 1; k >= 0; k--) {
            final Token t = stmt.get(k);
            if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                return t;
            }
            if (t.type != TokenType.WHITESPACE) {
                break;
            }
        }
        return null;
    }
    /**
     * Splits scope tokens into statement-or-block spans, depth-tracked across `(`/`[`/`{` (and
     * their closes) so that neither a parenthesized sub-expression's internal punctuation (e.g. a
     * `for(...; ...; ...)` header's own `;`s, or a lambda body's own `;`) nor a nested `{ }` block
     * ends the span early -- only a `;` or a balancing `}` at combined depth 0 does. A balancing
     * `}` produces an opaque span (e.g. an `if`/`for`/method-body block that leaked into this
     * scope) that `parseAssignment` will always reject, same as any other unrecognized statement.
     * A same-line trailing comment is pulled into the span it follows, same precedent as
     * `DeclarationAlignmentRule.splitStatements`.
     */
    protected List<List<Token>> splitAssignmentStatements(final List<Token> scopeTokens) {
        final List<List<Token>> statements = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        final int n = scopeTokens.size();
        int depth = 0;
        int idx = 0;

        while (idx < n) {
            final Token t = scopeTokens.get(idx);
            current.add(t);
            idx++;

            if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                depth++;
                continue;
            }
            if (isPunct(t, ")") || isPunct(t, "]")) {
                depth--;
                continue;
            }
            if (isPunct(t, "}")) {
                depth--;
                if (depth == 0) {
                    idx = pullTrailingSameLine(scopeTokens, current, idx);
                    statements.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            if (depth == 0 && isPunct(t, ";")) {
                idx = pullTrailingSameLine(scopeTokens, current, idx);
                statements.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            statements.add(current);
        }
        return statements;
    }
protected int pullTrailingSameLine(final List<Token> tokens, final List<Token> current, final int from) {
        int idx = from;
        final int n = tokens.size();
        while (idx < n) {
            final Token next = tokens.get(idx);
            if (next.type == TokenType.WHITESPACE || next.type == TokenType.COMMENT_LINE
                    || next.type == TokenType.COMMENT_BLOCK) {
                current.add(next);
                idx++;
            } else {
                break;
            }
        }
        return idx;
    }
    /** Same blank-line-before detection as `DeclarationAlignmentRule.hasBlankLineBefore`. */
    protected boolean hasBlankLineBeforeStmt(final List<Token> stmt) {
        int newlineRun = 0;
        for (final Token t : stmt) {
            if (t.type == TokenType.NEWLINE) {
                newlineRun++;
                if (newlineRun >= 2) {
                    return true;
                }
            } else if (t.type == TokenType.WHITESPACE) {
                // ignore -- doesn't break or extend the newline run
            } else if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                newlineRun = 0;
            } else {
                break;
            }
        }
        return false;
    }
    /** Joins tokens into canonical spaced text -- exact copy of
     *  `DeclarationAlignmentRule.renderTokens`'s tight-attachment rules (`*`/`&`/`::`/generics/
     *  `[`/`]`/`,`), duplicated here rather than shared since neither class currently exposes
     *  these as a shared utility (each rule class keeps its own small token-joining helpers). */
    protected String renderTokens(final List<Token> tokens) {
        final Set<Token> templateOpens = new HashSet<>();
        final Set<Token> templateCloses = new HashSet<>();
        templateAngleTokens(tokens, templateOpens, templateCloses);
        final StringBuilder sb = new StringBuilder();
        Token prev = null;
        for (final Token t : tokens) {
            if (prev != null && needsSpaceBetween(prev, t, templateOpens, templateCloses)) {
                sb.append(' ');
            }
            sb.append(t.text);
            prev = t;
        }
        return sb.toString();
    }
    /** A leading `template<...>` clause's `<`/`>` tokens are never reclassified to
     *  {@code ANGLE_BRACKET_OPEN}/{@code _CLOSE} by the tokenizer (it only arms on an
     *  identifier/cast-keyword before `<`, not the `template` keyword -- see
     *  `DeclarationAlignmentRule`'s own template-prefix handling for the same precedent), so
     *  {@link #needsSpaceBetween}/{@link #isTightToken} would otherwise space them like a
     *  comparison operator. Populates the identity sets of every open/close `<`/`>` token
     *  belonging to such a clause (depth-matched on the raw `<`/`>` OP tokens themselves) so the
     *  caller can treat them as tight without touching the tokens' actual type. */
protected boolean needsSpaceBetween(final Token prev, final Token cur, final Set<Token> templateOpens,
            final Set<Token> templateCloses) {
        // Kotlin's `fun <T> foo(...)` generic-function type-parameter clause is the one shape
        // where an ANGLE_BRACKET_OPEN is *not* tight against what precedes it -- every other
        // opener (`Foo<T>`, `foo<T>(...)`) directly follows the identifier it qualifies, but this
        // one follows the `fun` keyword itself and needs the normal keyword-then-clause space.
        if (lang.isKotlin && cur.type == TokenType.ANGLE_BRACKET_OPEN
                && prev.type == TokenType.KEYWORD && "fun".equals(prev.text)) {
            return true;
        }
        // JS/TS destructuring-declaration LHS (`const [first, second] = items`, `let { a, b } =
        // obj`): the opening `[`/`{` of the destructuring pattern directly follows a `const`/
        // `let`/`var` keyword. `isTightToken`'s `[`-is-always-tight rule below exists for
        // C/C++/Java array-declarator/subscript shapes (`int arr[5]`, `a[i]`), where `[` always
        // follows an identifier/closing-bracket, never a keyword -- so it wrongly collapses this
        // JS/TS shape's required keyword-then-pattern space too (`const[first, second]`). Scoped
        // narrowly to "keyword immediately before `[`" so it can't affect any other `[` use.
        if ((lang.isJs || lang.isTs) && isPunct(cur, "[") && prev.type == TokenType.KEYWORD) {
            return true;
        }
        if (isTightToken(cur) || templateCloses.contains(cur) || templateOpens.contains(cur)) {
            return false;
        }
        // A type keyword (`void`, `int`, ...) directly followed by `(` is a function-type's
        // return type inside a template argument (`std::function<void(int)>`) -- keywords can
        // never be called, so this is never a call-site space, only a tight function-type join.
        if (isPunct(cur, "(") && (prev.type == TokenType.IDENTIFIER
                || prev.type == TokenType.ANGLE_BRACKET_CLOSE || prev.type == TokenType.KEYWORD)) {
            return false;
        }
        if (prev.type == TokenType.ANGLE_BRACKET_OPEN || isOp(prev, "::") || isOp(prev, ".") || isOp(prev, "->") || isPunct(prev, "[") || isPunct(prev, "(")
                || templateOpens.contains(prev)) {
            return false;
        }
        // A Kotlin annotation's `@` (e.g. `@RaiseDSL public inline fun ...`) is tight against the
        // identifier that follows it -- without this, an annotation that shares its source line
        // with the function signature (so it becomes part of `sig.leadTokens` and gets rendered
        // through this same join point) comes out as `@ RaiseDSL`, which is not valid Kotlin (the
        // parser requires the identifier immediately after `@`). Kotlin's other `@`-uses
        // (`return@label`, `label@`, `this@Label`) are handled separately by
        // `KotlinSpecificRule.enforceLabeledJumpSpacing` over the whole token stream, not through
        // this lead-token join, so no annotation-vs-jump disambiguation is needed here. Found via
        // arrow-kt/arrow real-code testing (`RaiseAccumulateContext.kt`'s `@RaiseDSL public inline
        // fun ... mapOrAccumulate`, confirmed a genuine kotlin_sc parse error, not just cosmetic).
        if (lang.isKotlin && isOp(prev, "@")) {
            return false;
        }
        return true;
    }
protected boolean isTightToken(final Token t) {
        if (t.type == TokenType.ANGLE_BRACKET_OPEN || t.type == TokenType.ANGLE_BRACKET_CLOSE) {
            return true;
        }
        if (isPunct(t, ",") || isPunct(t, "[") || isPunct(t, "]") || isPunct(t, ")")) {
            return true;
        }
        // `*`/`&` are C/C++ pointer/reference declarator sigils here (tight against the type
        // they modify, `int* p`) -- Kotlin has no such use of either symbol (its `*` is always
        // multiplication or the spread operator, its expressions never use bare `&`), so treating
        // them as tight there would wrongly collapse ordinary arithmetic spacing in any Kotlin
        // expression rendered through this shared join point (STYLE_KOTLIN.md §9's `= expr`
        // rendering surfaced this: `x * x` was joining as `x* x`).
        if (!lang.isKotlin && (Token.isRepOp(t, '*') || Token.isRepOp(t, '&'))) {
            return true;
        }
        // Kotlin's bare `?` only ever appears as a type's nullability suffix (`Type?`) -- its
        // other two `?`-led operators (`?.` safe call, `?:` elvis) are each their own multi-char
        // token, never plain `?`, and Kotlin has no C-style ternary `?` to confuse this with -- so
        // it is always tight against the preceding type token, unlike C/Java's ternary `?`.
        if (lang.isKotlin && isOp(t, "?")) {
            return true;
        }
        return isOp(t, "...") || isOp(t, "::") || isOp(t, ".") || isOp(t, "->");
    }

    protected void templateAngleTokens(final List<Token> tokens, final Set<Token> opens, final Set<Token> closes) {
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.KEYWORD && "template".equals(t.text)
                    && i + 1 < tokens.size() && isOp(tokens.get(i + 1), "<")) {
                int depth = 0;
                for (int j = i + 1; j < tokens.size(); j++) {
                    final Token u = tokens.get(j);
                    if (isOp(u, "<")) {
                        depth++;
                        opens.add(u);
                    } else if (isOp(u, ">")) {
                        depth--;
                        closes.add(u);
                        if (depth == 0) {
                            break;
                        }
                    }
                }
            }
        }
    }
protected List<Token> significantOnly(final List<Token> stmt) {
        final List<Token> sig = new ArrayList<>();
        for (final Token t : stmt) {
            if (!isGapToken(t)) {
                sig.add(t);
            }
        }
        return sig;
    }
    /** Like {@link #significantOnly}, but keeps comment tokens -- used by {@link #parseSignature}
     *  so a parameter's inline block comment survives parsing instead of being silently dropped;
     *  only whitespace/newlines are gap tokens here. */
    protected List<Token> significantWithComments(final List<Token> stmt) {
        final List<Token> sig = new ArrayList<>();
        for (final Token t : stmt) {
            if (t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) {
                sig.add(t);
            }
        }
        return sig;
    }
protected int prevSignificantIndex(final List<Token> tokens, final int from) {
        int i = from;
        while (i >= 0 && isGapToken(tokens.get(i))) {
            i--;
        }
        return i;
    }
protected int matchParenBackward(final List<Token> tokens, final int closeIdx) {
        int depth = 0;
        for (int i = closeIdx; i >= 0; i--) {
            if (isPunct(tokens.get(i), ")")) {
                depth++;
            } else if (isPunct(tokens.get(i), "(")) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    /**
     * Capitalizes the first letter and strips a sole trailing period (STYLE.md §15) on every
     * `//` comment, on every `/* ... *&#47;` block comment that is already a single line, and on
     * a multi-line `/* ... *&#47;` block comment that already follows the conventional ` * `
     * continuation-marker banner shape (see {@link #reformatMultiLineBlockComment}) -- a
     * multi-line block comment that does *not* already use that marker convention (raw wrapped
     * prose, commented-out code, ASCII art) is left completely untouched, per the user's resolved
     * scope decision: only normalize within an already-recognizable shape, never restructure
     * arbitrary content. STYLE.md's separator-alignment rule remains a separate, deferred item.
     * <p>Per the Resolved Design Decision ("§15 comment scope and sentence detection"), this
     * method applies unconditionally to every comment token it sees, <i>except</i> for the
     * STYLE.md-documented exemption for labels/markers/closing-comments (`// for i`,
     * `/* FALL-THROUGH *&#47;`), which is now detected structurally (RDD_KEY_75 follow-up; see
     * {@link #isClosingBraceLabelComment}) rather than relied upon via pipeline ordering alone --
     * ordering by itself (this pass must run before `BlockStructureRule`'s §7 and `SwitchRule`'s
     * §13 passes that create those comments) only holds the first time a file is formatted; a
     * re-format of already-formatted output sees the generated comments as ordinary input and,
     * without this detection, would wrongly capitalize a lowercase label like `// switch`.
     * <p>{@code indentStyle} (the same {@code "spaces"|"tabs"} value later passed to
     * {@link #convertIndentation}, which always runs after this pass) is used to normalize the
     * reference indentation baked into a reformatted multi-line block comment's continuation
     * lines (see {@link #reformatMultiLineBlockComment}) -- this pass runs *before*
     * {@code convertIndentation}, so the raw indentation captured from the source
     * ({@link #indentBefore}) has not been converted to the target style yet; feeding that raw,
     * unconverted indent straight into the continuation lines' own text (which
     * {@code convertIndentation} never revisits, since by then it is embedded inside a single
     * multi-line {@code COMMENT_BLOCK} token rather than a separate leading {@code WHITESPACE}
     * token) left tab-indented continuation lines under a `spaces`-converted opening line on a
     * fresh format, self-correcting only on a second pass once the opening line's own indent was
     * already converted -- an idempotency bug found via real-code testing (`org.itadaki.bzip2`
     * et al., local `src/com`/`src/org` dogfood). Normalizing through {@link #renderIndent} here
     * up front makes the very first pass already correct.
     */
    public String enforceCommentStyle(final List<Token> tokens, final String indentStyle) {
        final Map<Integer, String> lineCommentContent = computeLineCommentGroups(tokens);
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.frozen) {
                out.append(t.text);
            } else if (t.type == TokenType.COMMENT_LINE) {
                final String content = lineCommentContent.get(i);
                if (content == null) {
                    out.append(t.text);
                } else {
                    out.append("//").append(content);
                }
            } else if (t.type == TokenType.COMMENT_BLOCK && !t.text.contains("\n") && !t.text.contains("\r")) {
                final String inner = t.text.substring(2, t.text.length() - 2);
                if ("FALL-THROUGH".equals(inner.trim())) {
                    out.append(t.text);
                } else {
                    out.append("/*").append(applyCommentTextRules(inner)).append("*/");
                }
            } else if (t.type == TokenType.COMMENT_BLOCK) {
                out.append(reformatMultiLineBlockComment(t.text, renderIndent(indentBefore(tokens, i), indentStyle)));
            } else if (t.type == TokenType.PREPROCESSOR) {
                out.append(capitalizePreprocessorTrailingComment(t.text));
            } else {
                out.append(t.text);
            }
        }
        return out.toString();
    }
    /** A `#define NAME VALUE // comment` line is lexed as one opaque {@code PREPROCESSOR} token
     *  (see {@link com.jxmake.formatter.tokenizer.TokenizerCore.TokenType#PREPROCESSOR}'s own
     *  doc), so its trailing `//` comment never becomes a separate {@code COMMENT_LINE} token and
     *  is skipped by the loop above. Applies the same capitalization rule directly to the text
     *  portion after a top-level (not inside a string/char literal) `//`, if any. */
    protected String capitalizePreprocessorTrailingComment(final String text) {
        final int idx = findTopLevelLineCommentStart(text);
        if (idx < 0) {
            return text;
        }
        final String before = text.substring(0, idx);
        final String inner = text.substring(idx + 2);
        return before + "//" + capitalizeFirstLetter(inner);
    }
    /** Index of a `//` sequence not nested inside a `"..."` or `'...'` literal, or -1 if none. */
    protected int findTopLevelLineCommentStart(final String text) {
        boolean inString = false;
        boolean inChar = false;
        for (int i = 0; i < text.length() - 1; i++) {
            final char c = text.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (inChar) {
                if (c == '\\') {
                    i++;
                } else if (c == '\'') {
                    inChar = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '\'') {
                inChar = true;
            } else if (c == '/' && text.charAt(i + 1) == '/') {
                return i;
            }
        }
        return -1;
    }
    /** Groups consecutive `//` line comments -- back to back with no blank line between -- into
     *  one §15 sentence-detection unit, the same way a multi-line `/* ... *&#47;` block comment
     *  already is: the trailing period is stripped only when it is the sole `.` across every line
     *  of the group, not just the last line alone. A closing-brace-label comment
     *  ({@link #isClosingBraceLabelComment}) breaks the chain entirely (never a link). A
     *  separator-alignment comment ({@link #parseSeparatorComment}) still counts as a chain link
     *  for dot-counting purposes (an ordinary prose sentence can coincidentally look like one, see
     *  RDD_KEY_47 follow-up) but is never itself rewritten -- rendered verbatim, exactly as before.
     *  Returns each rewritable group member's already-capitalized, period-decided replacement
     *  content, keyed by its token index; a token absent from the map must be rendered verbatim by
     *  the caller. */
    protected Map<Integer, String> computeLineCommentGroups(final List<Token> tokens) {
        final Map<Integer, String> result = new HashMap<>();
        int i = 0;
        while (i < tokens.size()) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.COMMENT_LINE && isCommentChainLink(tokens, i)) {
                final List<Integer> group = new ArrayList<>();
                group.add(i);
                int j = i;
                int next;
                while ((next = nextCommentChainLinkIfAdjacent(tokens, j)) >= 0) {
                    group.add(next);
                    j = next;
                }
                final List<String> contents = new ArrayList<>();
                for (final int idx : group) {
                    contents.add(tokens.get(idx).text.substring(2));
                }
                final int lastIdx = group.size() - 1;
                if (isCommentRewritable(tokens, group.get(lastIdx))) {
                    stripSoleTrailingPeriodAcrossLines(contents);
                }
                for (int k = 0; k < group.size(); k++) {
                    if (isCommentRewritable(tokens, group.get(k))) {
                        result.put(group.get(k), capitalizeFirstLetter(contents.get(k)));
                    }
                }
                i = j + 1;
            } else {
                i++;
            }
        }
        return result;
    }
    /** True iff the {@code COMMENT_LINE} token at {@code idx} extends a §15 sentence-detection
     *  chain: any plain `//` comment that is not a closing-brace label. */
    protected boolean isCommentChainLink(final List<Token> tokens, final int idx) {
        return !isClosingBraceLabelComment(tokens, idx);
    }
    /** True iff the {@code COMMENT_LINE} token at {@code idx} may actually be rewritten (not a
     *  separator-alignment label, handled instead by {@link #alignCommentSeparators}). */
    protected boolean isCommentRewritable(final List<Token> tokens, final int idx) {
        final Token t = tokens.get(idx);
        return parseSeparatorComment(t.text, idx) == null;
    }
    /** If the token at {@code idx} is a `//` chain-link comment, and it is followed -- after
     *  exactly one {@code NEWLINE} and only {@code WHITESPACE} otherwise (no blank line, no other
     *  token) -- by another chain-link `//` comment, returns that next comment's token index;
     *  otherwise returns -1. */
    protected int nextCommentChainLinkIfAdjacent(final List<Token> tokens, final int idx) {
        int p = idx + 1;
        int newlineCount = 0;
        while (p < tokens.size()) {
            final TokenType type = tokens.get(p).type;
            if (type == TokenType.WHITESPACE) {
                p++;
            } else if (type == TokenType.NEWLINE) {
                newlineCount++;
                if (newlineCount > 1) {
                    return -1;
                }
                p++;
            } else {
                break;
            }
        }
        if (p >= tokens.size() || newlineCount != 1) {
            return -1;
        }
        if (tokens.get(p).type == TokenType.COMMENT_LINE && isCommentChainLink(tokens, p)) {
            return p;
        }
        return -1;
    }
    /** True iff the {@code COMMENT_LINE} token at {@code idx} is immediately preceded, on the
     *  same physical line (only {@code WHITESPACE} in between, no {@code NEWLINE}), by a `}` --
     *  optionally followed by a `;` (C/C++ `struct`/`class`/`enum`/`union` definitions) -- the
     *  exact shape {@code BlockStructureRule.addClosingComments} generates (STYLE.md §7's
     *  `// label` closing comments). Catches both freshly-generated and user-written instances of
     *  this shape alike, consistent with STYLE.md's own "labels/markers/closing-comments" framing
     *  not distinguishing the two. */
    protected boolean isClosingBraceLabelComment(final List<Token> tokens, final int idx) {
        int p = idx - 1;
        while (p >= 0 && tokens.get(p).type == TokenType.WHITESPACE) {
            p--;
        }
        if (p < 0) {
            return false;
        }
        if (isPunct(tokens.get(p), ";")) {
            p--;
            while (p >= 0 && tokens.get(p).type == TokenType.WHITESPACE) {
                p--;
            }
            if (p < 0) {
                return false;
            }
        }
        if (!isPunct(tokens.get(p), "}")) {
            return false;
        }
        // A generated/genuine closing-comment label only ever follows a `}` that sits alone on
        // its own line (STYLE.md §7's rendering); a `}` sharing its line with the rest of a
        // one-liner body (`{ return v_; }`) can never carry one, so a trailing comment there
        // (e.g. a one-liner getter's `// getter`) is just an ordinary comment.
        int q = p - 1;
        while (q >= 0 && tokens.get(q).type == TokenType.WHITESPACE) {
            q--;
        }
        return q < 0 || tokens.get(q).type == TokenType.NEWLINE;
    }
protected static final class SepMatch {
        final int tokenIndex;
        final String label;
        final char sep;
        final String rest;

        SepMatch(final int tokenIndex, final String label, final char sep, final String rest) {
            this.tokenIndex = tokenIndex;
            this.label = label;
            this.sep = sep;
            this.rest = rest;
        }
    }
    /**
     * Resolved -- see STATE.md "§15 separator alignment": a trailing `//` comment qualifies as a
     * separator-alignment label iff its text (after `//`) contains exactly one character that is
     * (a) not a Unicode letter or digit ({@code Character.isLetterOrDigit}, so accented letters
     * like `ü` count as alphanumeric and can never be a separator) and (b) flanked by a literal
     * space on both sides. That single character is the separator; everything before it (trimmed)
     * is the label, everything after it (trimmed) is the rest. A comment with zero or 2+ such
     * candidates, or where label/rest would be empty, does not qualify and returns {@code null}.
     */
    protected SepMatch parseSeparatorComment(final String commentText, final int tokenIndex) {
        final String content = commentText.substring(2);
        int sepPos = -1;
        for (int i = 1; i < content.length() - 1; i++) {
            final char c = content.charAt(i);
            if (Character.isWhitespace(c) || Character.isLetterOrDigit(c)) {
                continue;
            }
            if (content.charAt(i - 1) == ' ' && content.charAt(i + 1) == ' ') {
                if (sepPos != -1) {
                    return null;
                }
                sepPos = i;
            }
        }
        if (sepPos == -1) {
            return null;
        }
        final String label = content.substring(0, sepPos).trim();
        final String rest = content.substring(sepPos + 1).trim();
        if (label.isEmpty() || rest.isEmpty()) {
            return null;
        }
        return new SepMatch(tokenIndex, label, content.charAt(sepPos), rest);
    }
    /**
     * STYLE.md §15 separator alignment (resolved -- see STATE.md "§15 separator alignment"):
     * pads the label portion of trailing `//` comments so a shared separator character lines up
     * vertically across a run of physically-adjacent lines, independent of what kind of statement
     * (if any) precedes the comment on each line -- this rule looks only at comment text, never
     * at another rule's alignment-group structure. A "line" is the token span between two
     * `NEWLINE` tokens (or list start/end); it qualifies only if its last significant token is a
     * `COMMENT_LINE` that {@link #parseSeparatorComment} recognizes. A blank line, a line with no
     * trailing comment, or a comment that doesn't match the separator shape naturally breaks the
     * run (it simply doesn't qualify) -- same "doesn't match, breaks the group" posture used by
     * every other grouping rule in this file. A run must also share the same separator character
     * to stay together, and must have at least 2 qualifying lines before anything is rewritten --
     * a lone qualifying line is left byte-for-byte untouched, same minimum-group-size precedent as
     * §14's getter/setter grouping.
     */
    public String alignCommentSeparators(final List<Token> tokens) {
        final List<SepMatch> perLine = new ArrayList<>();
        int lineStart = 0;
        for (int i = 0; i <= tokens.size(); i++) {
            if (i == tokens.size() || tokens.get(i).type == TokenType.NEWLINE) {
                perLine.add(findTrailingSeparatorComment(tokens, lineStart, i));
                lineStart = i + 1;
            }
        }

        final Map<Integer, String> rewrites = new HashMap<>();
        int runStart = 0;
        while (runStart < perLine.size()) {
            if (perLine.get(runStart) == null) {
                runStart++;
                continue;
            }
            int runEnd = runStart + 1;
            while (runEnd < perLine.size() && perLine.get(runEnd) != null
                    && perLine.get(runEnd).sep == perLine.get(runStart).sep) {
                runEnd++;
            }
            if (runEnd - runStart >= 2) {
                int maxLabelLen = 0;
                for (int i = runStart; i < runEnd; i++) {
                    maxLabelLen = Math.max(maxLabelLen, perLine.get(i).label.length());
                }
                for (int i = runStart; i < runEnd; i++) {
                    final SepMatch m = perLine.get(i);
                    final String newText = "// " + padRight(m.label, maxLabelLen) + " " + m.sep + " " + m.rest;
                    rewrites.put(m.tokenIndex, newText);
                }
            }
            runStart = runEnd;
        }

        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            final String rewritten = rewrites.get(i);
            out.append(rewritten != null ? rewritten : tokens.get(i).text);
        }
        return out.toString();
    }
    /** The {@link SepMatch} for the line spanning {@code [from, to)}, or {@code null} if that
     *  line's last significant token isn't a qualifying separator-alignment `//` comment. */
    protected SepMatch findTrailingSeparatorComment(final List<Token> tokens, final int from, final int to) {
        for (int i = to - 1; i >= from; i--) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.WHITESPACE) {
                continue;
            }
            if (t.type != TokenType.COMMENT_LINE || t.frozen) {
                return null;
            }
            return parseSeparatorComment(t.text, i);
        }
        return null;
    }
    /**
     * Normalizes a multi-line `/* ... *&#47;` block comment into STYLE.md §15's banner shape
     * (`/*` alone on its own line, each content line as ` * &lt;content&gt;`, `*&#47;` alone on
     * its own line, all indented to match {@code indent} -- the original comment's own line
     * indentation) -- but only when every physical line is already recognizable as using the
     * conventional `*`-per-line continuation-marker convention (resolved -- see "§15 multi-line
     * block comment banner reformatting" in STATE.md). If any continuation line, once its
     * leading whitespace is stripped, does not start with `*`, the whole comment is returned
     * unchanged -- this is what correctly skips raw wrapped prose and commented-out code, neither
     * of which has a STYLE.md worked example sanctioning a rewrite.
     * <p>Once recognized, each line's content is extracted by stripping its leading whitespace,
     * its leading `*`, and at most one following space (the closing line additionally has its
     * trailing `*&#47;` stripped first). The first and last physical lines (which carry `/*` and
     * `*&#47;` respectively) only contribute a content line if what remains is non-empty; a
     * genuinely blank *middle* line (a bare `*` with nothing else) is preserved as an intentional
     * blank paragraph separator. Text rules then apply across the whole extracted content exactly
     * like the single-line case, generalized over multiple lines: the first content line's
     * leading letter is always capitalized; the trailing period on the very last content line is
     * stripped only if it is the sole `.` across all content lines (an ellipsis, or any
     * abbreviation followed by more sentence text, is never touched), matching STYLE.md's "single
     * sentence never ends in a period, even one that merely got wrapped onto multiple physical
     * lines" reading already established for `//` comments.
     */
    protected String reformatMultiLineBlockComment(final String text, final String indent) {
        final String[] rawLines = text.split("\r\n|\r|\n", -1);
        final int n = rawLines.length;
        for (int i = 1; i < n; i++) {
            if (!stripLeadingWhitespace(rawLines[i]).startsWith("*")) {
                return text;
            }
        }

        int openMarkerEnd = 2;
        while (openMarkerEnd < rawLines[0].length() && rawLines[0].charAt(openMarkerEnd) == '*') {
            openMarkerEnd++;
        }
        final String openMarker = rawLines[0].substring(0, openMarkerEnd);
        final String firstContent = rawLines[0].substring(openMarkerEnd).trim();

        final String lastStripped = stripLeadingWhitespace(rawLines[n - 1]);
        final String lastContent;
        if ("*/".equals(lastStripped)) {
            lastContent = "";
        } else {
            final String afterMarker = afterLeadingStarMarker(lastStripped);
            if (!afterMarker.endsWith("*/")) {
                return text;
            }
            lastContent = trimTrailing(afterMarker.substring(0, afterMarker.length() - 2));
        }

        final List<String> contentLines = new ArrayList<>();
        if (!firstContent.isEmpty()) {
            contentLines.add(firstContent);
        }
        for (int i = 1; i < n - 1; i++) {
            contentLines.add(trimTrailing(afterLeadingStarMarker(stripLeadingWhitespace(rawLines[i]))));
        }
        if (!lastContent.isEmpty()) {
            contentLines.add(lastContent);
        }

        if (!contentLines.isEmpty()) {
            contentLines.set(0, capitalizeFirstLetter(contentLines.get(0)));
        }
        stripSoleTrailingPeriodAcrossLines(contentLines);

        final StringBuilder out = new StringBuilder(openMarker);
        for (final String line : contentLines) {
            out.append('\n').append(indent).append(" *");
            if (!line.isEmpty()) {
                out.append(' ').append(line);
            }
        }
        out.append('\n').append(indent).append(" */");
        return out.toString();
    }
    /** Drops a line's leading whitespace, returning the remainder unchanged. */
    protected String stripLeadingWhitespace(final String line) {
        int i = 0;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        return line.substring(i);
    }
    /** Drops a leading `*` and at most one space immediately after it. Caller must have already
     *  verified {@code wsStrippedLine} starts with `*`. */
    protected String afterLeadingStarMarker(final String wsStrippedLine) {
        String rest = wsStrippedLine.substring(1);
        if (rest.startsWith(" ")) {
            rest = rest.substring(1);
        }
        return rest;
    }
protected String trimTrailing(final String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end);
    }
    /** Cross-line generalization of {@link #stripSoleTrailingPeriod}: strips the trailing `.` on
     *  the last entry only when it is the sole `.` across every entry. */
    protected void stripSoleTrailingPeriodAcrossLines(final List<String> lines) {
        if (!normalizeCommentEndPeriod || lines.isEmpty()) {
            return;
        }
        if (commentNormalizationClassifier
                && classifyComment(String.join("\n", lines)) != CommentDecision.YES) {
            return;
        }
        int dotCount = 0;
        for (final String l : lines) {
            for (int i = 0; i < l.length(); i++) {
                if (l.charAt(i) == '.') {
                    dotCount++;
                }
            }
        }
        if (dotCount != 1) {
            return;
        }
        final int lastIdx = lines.size() - 1;
        final String last = lines.get(lastIdx);
        if (last.isEmpty() || last.charAt(last.length() - 1) != '.') {
            return;
        }
        // Same trailing-whitespace-before-the-period fix as `stripSoleTrailingPeriod`.
        lines.set(lastIdx, trimTrailing(last.substring(0, last.length() - 1)));
    }
    /** The leading whitespace of the line containing the token at idx, or "" if it isn't first on
     *  its line -- same precedent as `BlockStructureRule.indentBefore`. */
    protected String indentBefore(final List<Token> tokens, final int idx) {
        final StringBuilder indent = new StringBuilder();
        int i = idx - 1;
        while (i >= 0 && tokens.get(i).type == TokenType.WHITESPACE) {
            indent.insert(0, tokens.get(i).text);
            i--;
        }
        return (i < 0 || tokens.get(i).type == TokenType.NEWLINE) ? indent.toString() : "";
    }
protected String capitalizeFirstLetter(final String content) {
        if (!normalizeCommentStartCase) {
            return content;
        }
        for (int i = 0; i < content.length(); i++) {
            final char c = content.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (Character.isLetter(c) && Character.isLowerCase(c)) {
                if (commentNormalizationClassifier) {
                    if (classifyComment(content) != CommentDecision.YES) {
                        return content;
                    }
                    return content.substring(0, i) + Character.toUpperCase(c) + content.substring(i + 1);
                }
                // Extract the first word to check whether it is a keyword.
                int end = i;
                while (end < content.length()
                        && (Character.isLetterOrDigit(content.charAt(end))
                                || content.charAt(end) == '_')) {
                    end++;
                }
                if (isCommentNoCapitalizeWord(content.substring(i, end))) {
                    return content;
                }
                return content.substring(0, i) + Character.toUpperCase(c) + content.substring(i + 1);
            }
            break;
        }
        return content;
    }
    /** True iff `word` is a keyword in the current file's language ({@link #lang}) that must
     *  never be titlecased when it starts a comment sentence -- checked against the
     *  language-specific set only, so a C/C++-only keyword like `inline` never suppresses
     *  capitalization in a Java comment, and vice versa. */
    protected boolean isCommentNoCapitalizeWord(final String word) {
        if (lang.isJava) {
            return COMMENT_NO_CAPITALIZE_JAVA.contains(word);
        }
        if (lang.isCpp) {
            return COMMENT_NO_CAPITALIZE_C.contains(word) || COMMENT_NO_CAPITALIZE_CPP.contains(word);
        }
        return COMMENT_NO_CAPITALIZE_C.contains(word);
    }

protected String applyCommentTextRules(final String content) {
        return stripSoleTrailingPeriod(capitalizeFirstLetter(content));
    }
    /** RDD_KEY_94/STATE_COMMENT_GRAMMAR.md's classifier-backed decision path -- only consulted
     *  when {@link #commentNormalizationClassifier} is on, replacing the purely-deterministic
     *  {@link #isCommentNoCapitalizeWord}/dot-count logic for that one comment. Per the hard
     *  architectural constraint, {@link CommentDecision#ABSTAIN} (and, symmetrically here,
     *  {@link CommentDecision#NO}) must behave exactly as if the relevant {@code
     *  normalize-comment-*} key were {@code off} for that one comment -- callers check
     *  {@code != CommentDecision.YES}, not {@code == CommentDecision.ABSTAIN}, so a future
     *  NO-capable classifier doesn't silently start normalizing on NO. */
    protected CommentDecision classifyComment(final String content) {
        return CommentClassifier.classify(CommentFeatureExtractor.extract(content, lang));
    }
    /** Strips the trailing `.` only when it is the sole `.` in `content` -- this also leaves an
     *  ellipsis (`...`) untouched for free, since an ellipsis's dot count is never exactly 1. */
    protected String stripSoleTrailingPeriod(final String content) {
        if (!normalizeCommentEndPeriod) {
            return content;
        }
        if (commentNormalizationClassifier && classifyComment(content) != CommentDecision.YES) {
            return content;
        }
        int end = content.length();
        while (end > 0 && Character.isWhitespace(content.charAt(end - 1))) {
            end--;
        }
        if (end == 0 || content.charAt(end - 1) != '.') {
            return content;
        }
        int dotCount = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '.') {
                dotCount++;
            }
        }
        if (dotCount != 1) {
            return content;
        }
        // Also trim any whitespace that was between the last word and the period being
        // stripped (e.g. "...specified type ." -> "...specified type", not "...specified
        // type " with a stray trailing space) -- otherwise the trailing space survives this
        // pass and only gets caught by a later, unrelated trailing-whitespace pass, making the
        // comment converge over two formatter passes instead of one (idempotency bug).
        return trimTrailing(content.substring(0, end - 1)) + content.substring(end);
    }
protected boolean isCommentOrNewline(final Token t) {
        return t.type == TokenType.NEWLINE || t.type == TokenType.COMMENT_LINE
                || t.type == TokenType.COMMENT_BLOCK;
    }
    /** {@code true} if any token in {@code [fromInclusive, toExclusive)} is frozen (RDD_KEY_90
     *  §A) -- used by structural/span-level passes to skip a whole candidate unit rather than try
     *  to partially rewrite it. */
    protected boolean anyFrozen(final List<Token> tokens, final int fromInclusive, final int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            if (tokens.get(i).frozen) {
                return true;
            }
        }
        return false;
    }
}
