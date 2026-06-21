/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java-specific STYLE_JAVA.md sections not owned by another rule class: §2 (method-definition
 * Allman conversion) and §7 (Import Ordering). (§1/§4/§5/§6/§8 are already fully covered by
 * already-COMPLETE general/shared rule files -- see STATE.md's "`JavaSpecificRule.java` scoping"
 * Resolved Design Decision for the full cross-check; §3 needs zero code in this file either, since
 * it is satisfied by {@code MiscRule.enforceConditionComplexityPadding}, STYLE.md §3.1.)
 */
public class JavaSpecificRule {

    private final String language;

    /** The six fixed classification buckets every import is sorted into, per the resolved
     *  STYLE_JAVA.md §7 reading (see STATE.md "§7 import group order/count contradiction" --
     *  trust the worked example). {@code groupOrder} passed into {@link #enforceImportOrdering}
     *  configures only the *emission order* of these always-the-same six buckets, never which
     *  buckets exist -- so it must be a permutation of exactly this set. */
    private static final Set<String> IMPORT_GROUP_KEYS = new HashSet<>(
            Arrays.asList("java", "com", "org", "other", "local", "static"));

    public JavaSpecificRule(final String language) {
        this.language = language;
    }

    /**
     * STYLE_JAVA.md §2: a recognized Java <b>method definition</b>'s own brace moves to its own
     * line (Allman) whenever it is currently K&amp;R/same-line. Class/interface/enum body braces
     * and every control-flow block already correctly stay K&amp;R via the shared, language-general
     * {@code BlockStructureRule} (`Token.name`/keyword-based classification), and lambda bodies
     * already correctly stay K&amp;R via {@code BlockStructureRule.isLambdaBrace}'s Java branch
     * (preceding token is `->`) -- none of those need touching here. One-liner exception:
     * brace-placement only, same resolution as {@code CppSpecificRule}'s §2 (see STATE.md "§2
     * one-liner scope") -- this method never inspects or changes how many physical lines the body
     * itself spans, it only ever relocates the opening `{`.
     *
     * <p>Candidate signal mirrors {@code CppSpecificRule.isCandidateSignatureName}: the `{` is
     * directly preceded (no comment, no newline in the gap) by a `)` whose matching `(` is itself
     * preceded by a candidate method name (IDENTIFIER, not itself preceded by `new`) -- this
     * naturally excludes every control-flow brace (`if`/`while`/`for`/`switch`/`catch`/`try` precede
     * their `(` with a KEYWORD, never an IDENTIFIER, or have no `(` at all) and every lambda (a
     * lambda's `{` is preceded directly by `->`, never by a bare `)`-after-identifier). A `throws`
     * clause between `)` and `{` (`void foo() throws IOException { ... }`) is a documented,
     * deliberate gap, identical in spirit to C++'s trailing-qualifier gap in its own §2 -- the
     * immediate-predecessor check (`{` directly preceded by `)`) excludes it, and there is no
     * STYLE_JAVA.md worked example to justify guessing past that signal.
     *
     * <p>One Java-specific false positive the C++ version of this signal never has to consider:
     * an enum constant's anonymous constant-body (`RED("red") { ... }`) is structurally identical
     * to a method-definition signature (`)` preceded by a matching `(` preceded by an IDENTIFIER
     * not preceded by `new`) -- Java enum constants never use `new` the way anonymous classes do.
     * Left unguarded, this would wrongly Allman-convert a constant body, violating STYLE_JAVA.md
     * §2's "method definitions only" scope. Guarded via {@code isEnumConstantBody}: a candidate is
     * excluded if its matching `}` is immediately followed (skipping whitespace/comments/newlines)
     * by `,` or `;` -- the universal separator/terminator of an enum constant list, and a shape a
     * real method body's closing `}` can never be followed by in Java (unlike C/C++, a Java method
     * body brace is never followed by a bare `;`). This leaves one documented residual gap: the
     * <i>last</i> constant in an enum with no trailing members and no trailing `;` (legal Java) has
     * its body's `}` followed directly by the enum's own closing `}`, indistinguishable from an
     * ordinary last-member-in-a-body shape without enum-body-context tracking this codebase doesn't
     * do elsewhere either -- left untouched-but-possibly-misconverted in that narrow case, same
     * "no AST, bounded-effort" posture as the rest of this codebase's documented gaps.
     *
     * <p>A `{` already on its own line (gap already contains a NEWLINE) is left untouched --
     * idempotent. The per-occurrence indentation target reuses the closing `)`'s own
     * line-leading indentation, same as {@code CppSpecificRule}'s identical method.
     */
    public String enforceMethodDefinitionAllmanBraceStyle(final List<Token> tokens) {
        final Map<Integer, Integer> gapToBrace = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (!isPunct(tokens.get(i), "{")) {
                continue;
            }
            final int closeParenIdx = prevSignificantIndex(tokens, i);
            if (closeParenIdx < 0 || !isPunct(tokens.get(closeParenIdx), ")")) {
                continue;
            }
            if (!isMethodDefinitionCloseParen(tokens, closeParenIdx)) {
                continue;
            }
            if (hasNewlineOrCommentBetween(tokens, closeParenIdx, i)) {
                continue;
            }
            if (isEnumConstantBody(tokens, i)) {
                continue;
            }
            gapToBrace.put(closeParenIdx + 1, i);
        }

        final StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < tokens.size()) {
            final Integer braceIdx = gapToBrace.get(i);
            if (braceIdx != null) {
                out.append('\n').append(lineIndent(tokens, i - 1));
                i = braceIdx;
            } else {
                out.append(tokens.get(i).text);
                i++;
            }
        }
        return out.toString();
    }

    private boolean isMethodDefinitionCloseParen(final List<Token> tokens, final int closeParenIdx) {
        final int openParenIdx = matchParenBackward(tokens, closeParenIdx);
        return openParenIdx >= 0 && isCandidateMethodName(tokens, openParenIdx);
    }

    /** True iff the token immediately before {@code openIdx} is an IDENTIFIER not itself preceded
     *  by `new` -- the candidate-method-name signal, mirroring
     *  {@code CppSpecificRule.isCandidateSignatureName}. */
    private boolean isCandidateMethodName(final List<Token> tokens, final int openIdx) {
        final int nameIdx = prevSignificantIndex(tokens, openIdx);
        if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
            return false;
        }
        final int beforeName = prevSignificantIndex(tokens, nameIdx);
        return beforeName < 0 || tokens.get(beforeName).type != TokenType.KEYWORD
                || !"new".equals(tokens.get(beforeName).text);
    }

    /** True iff the `{` at {@code braceIdx} is an enum constant's anonymous constant-body --
     *  detected via its matching `}` being immediately followed by `,` or `;`, the universal
     *  enum-constant-list separator/terminator (see this method's caller's doc comment for the
     *  residual gap this heuristic doesn't cover). */
    private boolean isEnumConstantBody(final List<Token> tokens, final int braceIdx) {
        final int closeBraceIdx = matchBraceForward(tokens, braceIdx);
        if (closeBraceIdx < 0) {
            return false;
        }
        final int next = nextSignificantIndex(tokens, closeBraceIdx);
        return next >= 0 && (isPunct(tokens.get(next), ",") || isPunct(tokens.get(next), ";"));
    }

    /**
     * STYLE_JAVA.md §7: groups every top-level {@code import} statement into six fixed buckets
     * (static, java/javax, org, com, local, other), sorts within each bucket, and re-renders the
     * whole import block in {@code groupOrder}'s order, separated by {@code blankLines} blank
     * line(s) between non-empty groups. Per the resolved "trust the worked example" reading (see
     * STATE.md), classification priority is static &gt; local &gt; java/javax &gt; org &gt; com
     * &gt; other, but {@code groupOrder} (typically {@code java, com, org, other, local, static})
     * controls only emission order, not classification.
     *
     * <p>Local-package detection: the first {@code package} declaration found in {@code tokens}
     * supplies the local prefix -- its top {@code importDepth} dot-separated components. An import
     * is "local" iff its own leading components match that prefix component-for-component (a
     * wildcard import like {@code com.mycompany.*} still matches on its non-wildcard prefix). If no
     * {@code package} declaration is found, the local bucket is simply never populated.
     *
     * <p>Each `import [static] a.b.c[.*];` statement is parsed token-by-token
     * ({@link #parseImportStatement}) by concatenating IDENTIFIER/`.`/`*` tokens directly (Java
     * import paths never contain meaningful internal whitespace) -- any comment found anywhere
     * inside one import statement, or floating in the gap between two import statements, aborts
     * the *entire* pass and returns {@code tokens} byte-for-byte unchanged, same "never guess past
     * an unrecognized shape" posture used throughout this codebase; losing a comment via silent
     * reordering is not an acceptable failure mode. A file with zero import statements is also a
     * no-op. Regenerated import lines are canonical text ({@code "import " ["static "] path ";"}),
     * discarding original internal spacing -- same "restructured content is regenerated, not
     * preserved verbatim" precedent as {@code DeclarationAlignmentRule}'s static reordering.
     * "Unused imports are not removed" (STYLE_JAVA.md's own words) is honored by construction: no
     * usage analysis is performed anywhere in this method.
     *
     * @param groupOrder must be a permutation of exactly the six fixed bucket names in
     *        {@link #IMPORT_GROUP_KEYS} -- a config-validation precondition, not a per-file
     *        content-shape judgment call, so an invalid value throws rather than silently dropping
     *        a bucket's imports
     */
    public String enforceImportOrdering(final List<Token> tokens, final List<String> groupOrder,
            final boolean sortAlphabetically, final int importDepth, final int blankLines) {
        if (!new HashSet<>(groupOrder).equals(IMPORT_GROUP_KEYS) || groupOrder.size() != IMPORT_GROUP_KEYS.size()) {
            throw new IllegalArgumentException(
                    "groupOrder must be a permutation of " + IMPORT_GROUP_KEYS + ", got: " + groupOrder);
        }

        final List<String> localPrefix = findLocalPrefix(tokens, importDepth);

        int depth = 0;
        int firstImportIdx = -1;
        int prevSemicolonIdx = -1;
        int lastSemicolonIdx = -1;
        boolean blocked = false;
        final List<ParsedImport> imports = new ArrayList<>();
        final int n = tokens.size();
        int i = 0;
        while (i < n) {
            final Token t = tokens.get(i);
            if (isPunct(t, "{")) {
                depth++;
                i++;
                continue;
            }
            if (isPunct(t, "}")) {
                depth--;
                i++;
                continue;
            }
            if (depth == 0 && t.type == TokenType.KEYWORD && "import".equals(t.text)) {
                if (firstImportIdx < 0) {
                    firstImportIdx = i;
                } else if (hasCommentBetween(tokens, prevSemicolonIdx + 1, i)) {
                    blocked = true;
                    break;
                }
                final ParsedImport parsed = parseImportStatement(tokens, i);
                if (parsed == null) {
                    blocked = true;
                    break;
                }
                imports.add(parsed);
                prevSemicolonIdx = parsed.semicolonIdx;
                lastSemicolonIdx = parsed.semicolonIdx;
                i = parsed.semicolonIdx + 1;
                continue;
            }
            i++;
        }

        if (blocked || imports.isEmpty()) {
            return joinVerbatim(tokens);
        }

        final Map<String, List<ParsedImport>> buckets = new HashMap<>();
        for (final String key : IMPORT_GROUP_KEYS) {
            buckets.put(key, new ArrayList<>());
        }
        for (final ParsedImport imp : imports) {
            buckets.get(classifyImportGroup(imp, localPrefix)).add(imp);
        }
        if (sortAlphabetically) {
            for (final List<ParsedImport> group : buckets.values()) {
                group.sort((a, b) -> a.path.compareTo(b.path));
            }
        }

        final StringBuilder body = new StringBuilder();
        boolean emittedAnyGroup = false;
        for (final String groupKey : groupOrder) {
            final List<ParsedImport> members = buckets.get(groupKey);
            if (members.isEmpty()) {
                continue;
            }
            if (emittedAnyGroup) {
                for (int b = 0; b < blankLines + 1; b++) {
                    body.append('\n');
                }
            }
            for (int m = 0; m < members.size(); m++) {
                if (m > 0) {
                    body.append('\n');
                }
                final ParsedImport imp = members.get(m);
                body.append("import ");
                if (imp.isStatic) {
                    body.append("static ");
                }
                body.append(imp.path).append(';');
            }
            emittedAnyGroup = true;
        }

        final StringBuilder out = new StringBuilder();
        appendRange(out, tokens, 0, firstImportIdx);
        out.append(body);
        appendRange(out, tokens, lastSemicolonIdx + 1, n);
        return out.toString();
    }

    /** One successfully-parsed `import [static] path;` statement. */
    private static final class ParsedImport {
        final boolean isStatic;
        final String path;
        final int semicolonIdx;

        ParsedImport(final boolean isStatic, final String path, final int semicolonIdx) {
            this.isStatic = isStatic;
            this.path = path;
            this.semicolonIdx = semicolonIdx;
        }
    }

    /**
     * Parses one `import [static] a.b.c[.*];` statement starting at the `import` keyword token at
     * {@code importIdx}. Returns {@code null} -- signaling "bail the entire pass" to the caller --
     * if a comment is found anywhere inside the statement, or if any token other than
     * WHITESPACE/NEWLINE, the `static` keyword (only before any path token), an IDENTIFIER, or a
     * dot/star OP token is encountered before the terminating `;`. Never guesses past an
     * unrecognized import shape.
     *
     * <p>Dot/star OP tokens need a dedicated check ({@link #isPathOp}) rather than a literal `"."`/
     * `"*"` text match: {@code TokenizerCore}'s {@code MULTI_CHAR_OPS} list includes C++'s
     * pointer-to-member operator `".*"`, shared across languages, so a wildcard import's trailing
     * `.{@literal *}` lexes as a single combined OP token with text {@code ".*"}, not two separate
     * single-char tokens -- discovered via a failing smoke-test case on `import pkg.*;`.</p>
     */
    private ParsedImport parseImportStatement(final List<Token> tokens, final int importIdx) {
        boolean isStatic = false;
        boolean sawPathToken = false;
        final StringBuilder path = new StringBuilder();
        final int n = tokens.size();
        int p = importIdx + 1;
        while (p < n) {
            final Token t = tokens.get(p);
            if (t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                p++;
                continue;
            }
            if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                return null;
            }
            if (isPunct(t, ";")) {
                return sawPathToken ? new ParsedImport(isStatic, path.toString(), p) : null;
            }
            if (!sawPathToken && !isStatic && t.type == TokenType.KEYWORD && "static".equals(t.text)) {
                isStatic = true;
                p++;
                continue;
            }
            if (t.type == TokenType.IDENTIFIER || isPathOp(t)) {
                path.append(t.text);
                sawPathToken = true;
                p++;
                continue;
            }
            return null;
        }
        return null;
    }

    /** True iff a {@code COMMENT_LINE}/{@code COMMENT_BLOCK} token exists anywhere in
     *  {@code tokens[fromInclusive, toExclusive)} -- used to detect a floating comment between two
     *  otherwise-clean import statements, which would otherwise be silently dropped by reordering. */
    private boolean hasCommentBetween(final List<Token> tokens, final int fromInclusive, final int toExclusive) {
        for (int i = Math.max(fromInclusive, 0); i < toExclusive; i++) {
            final TokenType type = tokens.get(i).type;
            if (type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) {
                return true;
            }
        }
        return false;
    }

    /** Reads the first {@code package a.b.c;} declaration in {@code tokens} (best-effort -- this
     *  is a non-destructive lookup, not a rewrite, so a malformed/commented package line just
     *  yields whatever IDENTIFIER tokens are found rather than bailing) and returns its top
     *  {@code importDepth} dot-components. Empty list if no `package` declaration exists. */
    private List<String> findLocalPrefix(final List<Token> tokens, final int importDepth) {
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.KEYWORD && "package".equals(t.text)) {
                final List<String> components = new ArrayList<>();
                int p = i + 1;
                while (p < tokens.size() && !isPunct(tokens.get(p), ";")) {
                    if (tokens.get(p).type == TokenType.IDENTIFIER) {
                        components.add(tokens.get(p).text);
                    }
                    p++;
                }
                return components.subList(0, Math.min(importDepth, components.size()));
            }
        }
        return Collections.emptyList();
    }

    /** Classification priority: static &gt; local &gt; java/javax &gt; org &gt; com &gt; other --
     *  see {@link #enforceImportOrdering}'s doc comment. */
    private String classifyImportGroup(final ParsedImport imp, final List<String> localPrefix) {
        if (imp.isStatic) {
            return "static";
        }
        final String[] parts = imp.path.split("\\.");
        if (!localPrefix.isEmpty() && matchesPrefix(parts, localPrefix)) {
            return "local";
        }
        final String first = parts.length > 0 ? parts[0] : "";
        if ("java".equals(first) || "javax".equals(first)) {
            return "java";
        }
        if ("org".equals(first)) {
            return "org";
        }
        if ("com".equals(first)) {
            return "com";
        }
        return "other";
    }

    private boolean matchesPrefix(final String[] parts, final List<String> prefix) {
        if (parts.length < prefix.size()) {
            return false;
        }
        for (int i = 0; i < prefix.size(); i++) {
            if (!parts[i].equals(prefix.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Appends the literal text of {@code tokens[fromInclusive, toExclusive)} verbatim. */
    private void appendRange(final StringBuilder out, final List<Token> tokens, final int fromInclusive, final int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            out.append(tokens.get(i).text);
        }
    }

    private String joinVerbatim(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        for (final Token t : tokens) {
            out.append(t.text);
        }
        return out.toString();
    }

    /** True iff {@code t} is an OP token consisting solely of `.`/`*` characters -- covers a plain
     *  `.` separator, a plain `*` wildcard, and {@code TokenizerCore}'s combined `.* ` multi-char
     *  pointer-to-member OP token (see {@link #parseImportStatement}'s doc comment). */
    private boolean isPathOp(final Token t) {
        if (t == null || t.type != TokenType.OP || t.text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < t.text.length(); i++) {
            final char c = t.text.charAt(i);
            if (c != '.' && c != '*') {
                return false;
            }
        }
        return true;
    }

    private boolean hasNewlineOrCommentBetween(final List<Token> tokens, final int fromExclusive, final int toExclusive) {
        for (int i = fromExclusive + 1; i < toExclusive; i++) {
            final TokenType type = tokens.get(i).type;
            if (type == TokenType.NEWLINE || type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) {
                return true;
            }
        }
        return false;
    }

    /** Line-leading whitespace of the physical line containing token {@code idx} -- "" if that
     *  line has no leading whitespace (column-0 start). */
    private String lineIndent(final List<Token> tokens, final int idx) {
        int newlineIdx = -1;
        for (int i = idx; i >= 0; i--) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                newlineIdx = i;
                break;
            }
        }
        final int afterNewline = newlineIdx + 1;
        if (afterNewline < tokens.size() && tokens.get(afterNewline).type == TokenType.WHITESPACE) {
            return tokens.get(afterNewline).text;
        }
        return "";
    }

    private int matchParenBackward(final List<Token> tokens, final int closeIdx) {
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

    private int matchBraceForward(final List<Token> tokens, final int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < tokens.size(); i++) {
            if (isPunct(tokens.get(i), "{")) {
                depth++;
            } else if (isPunct(tokens.get(i), "}")) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int prevSignificantIndex(final List<Token> tokens, final int from) {
        for (int i = from - 1; i >= 0; i--) {
            if (!isGapToken(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int nextSignificantIndex(final List<Token> tokens, final int from) {
        for (int i = from + 1; i < tokens.size(); i++) {
            if (!isGapToken(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isGapToken(final Token t) {
        return t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK;
    }

    private boolean isPunct(final Token t, final String text) {
        return t != null && t.type == TokenType.PUNCT && text.equals(t.text);
    }
}
