/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.grid.ColumnGrid;
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

    /** Fallback one-indent-level unit when it can't be derived from the class/interface's own
     *  body indentation -- same precedent as {@code SwitchRule.DEFAULT_INDENT_UNIT}. */
    private static final String DEFAULT_INDENT_UNIT = "    ";

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
     *
     * <p>RDD_KEY_75/RDD_KEY_89: a one-liner method whose entire `{ ... }` body sits on one
     * physical line is never converted to Allman -- confirmed via `make test` against
     * java_modern_out.java/combined_out.java, where standalone one-liners (e.g. `distance()`,
     * `hasError()`, `isActive()`, each the only one-liner in its enclosing scope) stay K&R just
     * as reliably as ones adjacent to another one-liner (STYLE.md §14 groups).
     */
    public String enforceMethodDefinitionAllmanBraceStyle(final List<Token> tokens) {
        final Map<Integer, Integer> gapToBrace = new HashMap<>();
        final List<OneLinerCandidate> oneLiners = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (!isPunct(tokens.get(i), "{") || tokens.get(i).name != null) {
                // `name != null` means the tokenizer already tagged this as a named-construct
                // body (class/interface/enum/record) -- always K&R, handled by
                // `BlockStructureRule.qualifiesForKAndR`, never Allman. Without this guard a
                // record's own body brace (`record Point(...) {`, or with a trailing
                // `implements` clause) is structurally indistinguishable from a method
                // definition / compact-constructor shape and would be wrongly re-broken here.
                continue;
            }
            final int prevIdx = prevSignificantIndex(tokens, i);
            if (prevIdx < 0) {
                continue;
            }
            if (!isPunct(tokens.get(prevIdx), ")")) {
                // Check for Java `throws` clause: `void foo() throws IOException {`
                final int throwsCloseParen = findCloseParenBeforeThrows(tokens, prevIdx);
                if (throwsCloseParen >= 0 && !hasNewlineOrCommentBetween(tokens, prevIdx, i)
                        && isMethodDefinitionCloseParen(tokens, throwsCloseParen)
                        && !isEnumConstantBody(tokens, i)) {
                    final int closeBraceIdx = matchBraceForward(tokens, i);
                    if (closeBraceIdx >= 0 && isSingleLineBody(tokens, i, closeBraceIdx)) {
                        final int openParenIdx = matchParenBackward(tokens, throwsCloseParen);
                        final int nameIdx = prevSignificantIndex(tokens, openParenIdx);
                        oneLiners.add(new OneLinerCandidate(nameIdx, throwsCloseParen, i, closeBraceIdx));
                    } else {
                        // Keep `throws IOException` in output; only move `{` to its own line.
                        gapToBrace.put(prevIdx + 1, i);
                    }
                } else if (isCompactConstructorBrace(tokens, prevIdx, i)
                        && !hasNewlineOrCommentBetween(tokens, prevIdx, i)) {
                    gapToBrace.put(prevIdx + 1, i);
                }
                continue;
            }
            final int closeParenIdx = prevIdx;
            if (!isMethodDefinitionCloseParen(tokens, closeParenIdx)) {
                continue;
            }
            if (hasNewlineOrCommentBetween(tokens, closeParenIdx, i)) {
                continue;
            }
            if (isEnumConstantBody(tokens, i)) {
                continue;
            }
            final int closeBraceIdx = matchBraceForward(tokens, i);
            if (closeBraceIdx >= 0 && isSingleLineBody(tokens, i, closeBraceIdx)) {
                final int openParenIdx = matchParenBackward(tokens, closeParenIdx);
                final int nameIdx = prevSignificantIndex(tokens, openParenIdx);
                oneLiners.add(new OneLinerCandidate(nameIdx, closeParenIdx, i, closeBraceIdx));
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
                out.append(tokens.get(braceIdx).text);
                i = braceIdx + 1;
            } else {
                out.append(tokens.get(i).text);
                i++;
            }
        }
        return out.toString();
    }

    /** One method-definition `{ ... }` whose body sits entirely on one physical line --
     *  always stays K&amp;R (RDD_KEY_75/RDD_KEY_89), never converted to Allman. */
    private static final class OneLinerCandidate {
        final int nameIdx;
        final int closeParenIdx;
        final int braceIdx;
        final int closeBraceIdx;

        OneLinerCandidate(final int nameIdx, final int closeParenIdx, final int braceIdx, final int closeBraceIdx) {
            this.nameIdx = nameIdx;
            this.closeParenIdx = closeParenIdx;
            this.braceIdx = braceIdx;
            this.closeBraceIdx = closeBraceIdx;
        }
    }

    /** True iff no {@code NEWLINE} token appears between {@code braceIdx} and {@code closeBraceIdx}
     *  inclusive -- the whole `{ ... }` span is one physical line. */
    private boolean isSingleLineBody(final List<Token> tokens, final int braceIdx, final int closeBraceIdx) {
        for (int i = braceIdx; i <= closeBraceIdx; i++) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                return false;
            }
        }
        return true;
    }

    private boolean isMethodDefinitionCloseParen(final List<Token> tokens, final int closeParenIdx) {
        final int openParenIdx = matchParenBackward(tokens, closeParenIdx);
        return openParenIdx >= 0 && isCandidateMethodName(tokens, openParenIdx);
    }

    /**
     * For Java {@code throws} clauses: given the token at {@code fromIdx} (the significant token
     * immediately before {@code {}) that is NOT {@code )}, checks if it is the last exception
     * class name in a {@code throws} clause. Scans backward through comma-separated IDENTIFIERs
     * to the {@code throws} keyword, then expects {@code )} immediately before it.
     * Returns the index of the {@code )} of the method parameter list, or -1 if no such pattern.
     */
    private int findCloseParenBeforeThrows(final List<Token> tokens, final int fromIdx) {
        int i = fromIdx;
        if (i < 0 || tokens.get(i).type != TokenType.IDENTIFIER) {
            return -1;
        }
        while (i >= 0) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.IDENTIFIER) {
                i = prevSignificantIndex(tokens, i - 1);
            } else if (isPunct(t, ",")) {
                i = prevSignificantIndex(tokens, i - 1);
            } else {
                break;
            }
        }
        if (i < 0 || tokens.get(i).type != TokenType.KEYWORD || !"throws".equals(tokens.get(i).text)) {
            return -1;
        }
        final int closeParen = prevSignificantIndex(tokens, i - 1);
        return (closeParen >= 0 && isPunct(tokens.get(closeParen), ")")) ? closeParen : -1;
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

    /** Access modifiers legal on a compact canonical constructor -- the only keywords that may
     *  sit between a member boundary and the constructor's own name. */
    private static final Set<String> COMPACT_CTOR_MODIFIERS = new HashSet<>(
            Arrays.asList("public", "private", "protected"));

    /**
     * True iff the `{` at {@code braceIdx} (whose immediately preceding significant token,
     * already confirmed not a `)`, sits at {@code identIdx}) is a record's compact canonical
     * constructor -- `public Point { ... }`, no parameter list at all (STYLE_JAVA17.md §1).
     * A bare IDENTIFIER directly before `{` is structurally ambiguous: besides a compact
     * constructor, it's also produced by an enum constant body omitting constructor args
     * (`RED { ... }`, excluded via {@code isEnumConstantBody}) and -- the case that bit the
     * first version of this method -- the <i>last type in an enclosing class/interface's own
     * `implements`/`extends` clause</i> (`class Foo implements Bar {`), since `Bar` is just as
     * bare an IDENTIFIER as a constructor name. Distinguished by walking back from the name,
     * over any access modifiers (the only thing legal before a real constructor name), and
     * requiring what's left to be a member/scope boundary (`}`, `;`, `{`, or start of file) --
     * `implements`/`extends`/`,` never satisfy that, so they correctly fail this check instead
     * of needing `Token.name` (which can't see back across an arbitrarily long `implements`
     * list either, so it's null for both the true positive and the false positive alike here).
     */
    private boolean isCompactConstructorBrace(final List<Token> tokens, final int identIdx, final int braceIdx) {
        if (tokens.get(identIdx).type != TokenType.IDENTIFIER) {
            return false;
        }
        if (isEnumConstantBody(tokens, braceIdx)) {
            return false;
        }
        int i = prevSignificantIndex(tokens, identIdx);
        while (i >= 0 && tokens.get(i).type == TokenType.KEYWORD
                && COMPACT_CTOR_MODIFIERS.contains(tokens.get(i).text)) {
            i = prevSignificantIndex(tokens, i - 1);
        }
        return i < 0 || isPunct(tokens.get(i), "}") || isPunct(tokens.get(i), ";")
                || isPunct(tokens.get(i), "{");
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

    /**
     * STYLE_JAVA17.md §2: line-breaks a {@code permits} clause -- inline if the full
     * class/interface declaration line (from the {@code class}/{@code interface} keyword's own
     * line start through the body's opening {@code {}) fits within {@code
     * MiscRule.LINE_LENGTH_LIMIT}, otherwise one permitted type per line, column-aligned under the
     * first type, with the body's {@code {} trailing the last type. Always re-decides from
     * scratch regardless of the file's current wrapped/unwrapped shape -- same "regenerate, don't
     * preserve" posture as {@link #enforceImportOrdering}'s static reordering, so the pass is
     * idempotent. Bails per occurrence (leaves that one untouched) if a comment is found anywhere
     * between the declaration's line start and the body's {@code {}, or if the permitted-type list
     * can't be parsed -- never guesses past an unrecognized shape, same posture as {@link
     * #enforceImportOrdering}.
     */
    public String enforcePermitsClauseLineBreaking(final List<Token> tokens) {
        final List<int[]> spans = new ArrayList<>();
        final List<String> renders = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.type != TokenType.KEYWORD || !"permits".equals(t.text)) {
                continue;
            }
            final int classIdx = prevClassOrInterfaceKeyword(tokens, i);
            if (classIdx < 0) {
                continue;
            }
            final int openBraceIdx = nextPunct(tokens, i, "{");
            if (openBraceIdx < 0) {
                continue;
            }
            final int declStart = lineStartIndex(tokens, classIdx);
            if (hasCommentBetween(tokens, declStart, openBraceIdx + 1)) {
                continue;
            }
            final int prevSigIdx = prevSignificantIndex(tokens, i);
            if (prevSigIdx < 0) {
                continue;
            }
            final List<String> types = parsePermittedTypes(tokens, i + 1, openBraceIdx);
            if (types == null) {
                continue;
            }

            final String baseIndent = lineIndent(tokens, declStart);
            final String collapsedFull = baseIndent + collapseToOneLine(tokens, declStart, openBraceIdx);
            final String rendered = collapsedFull.length() <= MiscRule.LINE_LENGTH_LIMIT
                    ? renderPermitsInline(types)
                    : renderPermitsWrapped(tokens, baseIndent, openBraceIdx, types);

            spans.add(new int[] { prevSigIdx + 1, openBraceIdx + 1 });
            renders.add(rendered);
        }

        if (spans.isEmpty()) {
            return joinVerbatim(tokens);
        }

        final StringBuilder out = new StringBuilder();
        int cursor = 0;
        for (int s = 0; s < spans.size(); s++) {
            final int[] span = spans.get(s);
            appendRange(out, tokens, cursor, span[0]);
            out.append(renders.get(s));
            cursor = span[1];
        }
        appendRange(out, tokens, cursor, tokens.size());
        return out.toString();
    }

    private String renderPermitsInline(final List<String> types) {
        return " permits " + String.join(", ", types) + " {";
    }

    /** Derives the one-indent-level unit from the gap between the declaration's own indent and its
     *  body's first member's indent (same precedent as {@code SwitchRule.deriveUnit}), falling back
     *  to {@link #DEFAULT_INDENT_UNIT}. */
    private String renderPermitsWrapped(final List<Token> tokens, final String baseIndent,
            final int openBraceIdx, final List<String> types) {
        final String unit = deriveIndentUnit(tokens, baseIndent, openBraceIdx);
        final String align = repeatChar(' ', baseIndent.length() + unit.length() + "permits ".length());

        final StringBuilder sb = new StringBuilder();
        sb.append('\n').append(baseIndent).append(unit).append("permits ");
        for (int idx = 0; idx < types.size(); idx++) {
            if (idx > 0) {
                sb.append('\n').append(align);
            }
            sb.append(types.get(idx));
            if (idx < types.size() - 1) {
                sb.append(',');
            }
        }
        sb.append(" {");
        return sb.toString();
    }

    private String deriveIndentUnit(final List<Token> tokens, final String baseIndent, final int openBraceIdx) {
        final int firstBodySig = nextSignificantIndex(tokens, openBraceIdx);
        if (firstBodySig < 0) {
            return DEFAULT_INDENT_UNIT;
        }
        final String bodyIndent = lineIndent(tokens, firstBodySig);
        if (bodyIndent.length() > baseIndent.length() && bodyIndent.startsWith(baseIndent)) {
            return bodyIndent.substring(baseIndent.length());
        }
        return DEFAULT_INDENT_UNIT;
    }

    private String repeatChar(final char c, final int count) {
        final StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    /** Splits {@code tokens[fromInclusive, toExclusive)} on top-level commas (depth-tracked across
     *  `&lt;...&gt;` generic argument lists) into one collapsed-to-one-line, trimmed string per
     *  permitted type. Returns {@code null} if the list is empty -- an unparseable/empty shape this
     *  method never guesses past. Multi-char OPs like `&gt;&gt;` that close two nested generic levels
     *  at once are a known, accepted residual gap (permitted-type lists are realistically always
     *  simple class names, never deeply nested generics). */
    private List<String> parsePermittedTypes(final List<Token> tokens, final int fromInclusive, final int toExclusive) {
        final List<String> types = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        int angleDepth = 0;
        for (int i = fromInclusive; i < toExclusive; i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                return null;
            }
            if (t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                if (current.length() > 0 && current.charAt(current.length() - 1) != ' ') {
                    current.append(' ');
                }
                continue;
            }
            if (isPunct(t, "<")) {
                angleDepth++;
            } else if (isPunct(t, ">")) {
                angleDepth--;
            } else if (angleDepth == 0 && isPunct(t, ",")) {
                types.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(t.text);
        }
        final String last = current.toString().trim();
        if (last.isEmpty()) {
            return null;
        }
        types.add(last);
        return types;
    }

    /** Collapses {@code tokens[fromInclusive, toInclusive]} to one physical line by replacing every
     *  WHITESPACE/NEWLINE run between significant tokens with a single space -- used only to measure
     *  the would-be-inline rendering length, never emitted verbatim. */
    private String collapseToOneLine(final List<Token> tokens, final int fromInclusive, final int toInclusive) {
        final StringBuilder sb = new StringBuilder();
        for (int i = fromInclusive; i <= toInclusive; i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
                continue;
            }
            sb.append(t.text);
        }
        return sb.toString().trim();
    }

    /** Sentinel returned by {@link #findCaseArrowOrColon} when a label's top-level `:` is found
     *  instead of `->` -- the switch is colon-form, owned by {@code SwitchRule}, never touched here. */
    private static final int COLON_FOUND = -2;

    /** Sentinel returned by {@link #findCaseArrowOrColon} when neither terminator is found before
     *  hitting a depth-0 `;`/`{` boundary first -- a malformed/unrecognized label shape. */
    private static final int ARROW_NOT_FOUND = -1;

    /**
     * STYLE_JAVA17.md §3: column-aligns the `->` across every case of an arrow-labeled switch
     * (a switch *expression*, or the same arrow syntax used as a statement -- the rule doesn't
     * distinguish, since the token shape is identical either way). This is a distinct construct
     * from the `:`-labeled switch statement already fully handled by {@code SwitchRule}'s
     * STYLE.md §13 passes, which this method never touches: arrow-labeled and colon-labeled cases
     * never co-exist in one switch per the JLS, so the two rules' switch discovery is naturally
     * disjoint -- {@link #findArrowCases} returns {@code null} (skip, untouched) the instant it
     * finds a label terminated by `:` instead of `->`.
     *
     * <p>All-or-nothing per switch (§3.1, §7 resolved decision): if any case in a given switch has
     * a block body (`-> {`), that whole switch is left untouched -- no alignment at all, same
     * conservative posture as STYLE.md §13's own inline-alignment bail-out. Nothing else is needed
     * for a block-body case: its `{` already renders K&amp;R via {@code
     * BlockStructureRule.isLambdaBrace}'s existing `->`-preceded-brace branch (lambdas and arrow-form
     * switch cases are structurally identical at that brace), and already gets no closing comment
     * via the same not-`)`-preceded shape in {@code BlockStructureRule.classifyBrace} -- both already
     * correct, pre-existing, general-purpose behavior, not modified here.
     *
     * <p>A switch with a malformed/unrecognized label shape is also left completely untouched, same
     * "never guess past an unrecognized shape" posture used throughout this codebase. Only the label
     * span -- from the `case`/`default` keyword through the `->` and the single space after it -- is
     * ever rewritten; body content (everything from the first significant token after `->` onward)
     * is never touched, so a block body's internal formatting, a multi-line expression, or a
     * `throw` statement survives exactly as written.
     */
    public String enforceSwitchExpressionArrowAlignment(final List<Token> tokens) {
        final Map<Integer, String> overrides = new HashMap<>();

        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.type != TokenType.KEYWORD || !"switch".equals(t.text)) {
                continue;
            }
            final int openParenIdx = nextSignificantIndex(tokens, i);
            if (openParenIdx < 0 || !isPunct(tokens.get(openParenIdx), "(")) {
                continue;
            }
            final int closeParenIdx = matchParenForward(tokens, openParenIdx);
            if (closeParenIdx < 0) {
                continue;
            }
            final int openBraceIdx = nextSignificantIndex(tokens, closeParenIdx);
            if (openBraceIdx < 0 || !isPunct(tokens.get(openBraceIdx), "{")) {
                continue;
            }
            final int closeBraceIdx = matchBraceForward(tokens, openBraceIdx);
            if (closeBraceIdx < 0) {
                continue;
            }

            final List<ArrowCase> cases = findArrowCases(tokens, openBraceIdx, closeBraceIdx);
            if (cases == null || cases.isEmpty()) {
                continue; // colon-form (SwitchRule's), or malformed -- never ours
            }

            boolean anyBlockBody = false;
            for (final ArrowCase c : cases) {
                if (c.blockBody) {
                    anyBlockBody = true;
                    break;
                }
            }
            if (!anyBlockBody) {
                applyArrowAlignment(cases, overrides);
            }
        }

        return render(tokens, overrides);
    }

    /** One `case <label> ->` / `default ->` arrow-form case found directly (brace depth 0 relative
     *  to the switch's own `{`) inside an arrow-labeled switch body. */
    private static final class ArrowCase {
        final int kwIdx;
        final int bodyStartIdx; // first significant token after `->`
        final String label; // raw "case ..." / "default" text, whitespace-collapsed and trimmed
        final boolean blockBody; // body starts with `{`

        ArrowCase(final int kwIdx, final int bodyStartIdx, final String label, final boolean blockBody) {
            this.kwIdx = kwIdx;
            this.bodyStartIdx = bodyStartIdx;
            this.label = label;
            this.blockBody = blockBody;
        }
    }

    /**
     * Finds every direct (brace-depth-0) `case`/`default` label inside [openBraceIdx,
     * closeBraceIdx) and classifies it as arrow-form. Returns {@code null} the instant any label's
     * terminator turns out to be a top-level `:` ({@link #COLON_FOUND} -- the switch belongs to
     * {@code SwitchRule} instead) or can't be found at all ({@link #ARROW_NOT_FOUND} -- malformed).
     */
    private List<ArrowCase> findArrowCases(final List<Token> tokens, final int openBraceIdx, final int closeBraceIdx) {
        final List<ArrowCase> cases = new ArrayList<>();
        int depth = 0;
        for (int i = openBraceIdx + 1; i < closeBraceIdx; i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "{")) {
                depth++;
            } else if (isPunct(t, "}")) {
                depth--;
            } else if (depth == 0 && t.type == TokenType.KEYWORD
                    && ("case".equals(t.text) || "default".equals(t.text))) {
                final int arrowIdx = findCaseArrowOrColon(tokens, i, closeBraceIdx);
                if (arrowIdx == COLON_FOUND || arrowIdx == ARROW_NOT_FOUND) {
                    return null;
                }
                final int bodyStartIdx = nextSignificantIndex(tokens, arrowIdx);
                if (bodyStartIdx < 0) {
                    return null;
                }
                final String label = collapseToOneLine(tokens, i, arrowIdx - 1);
                cases.add(new ArrowCase(i, bodyStartIdx, label, isPunct(tokens.get(bodyStartIdx), "{")));
            }
        }
        return cases;
    }

    /** The top-level (paren/bracket-depth 0) `->` terminating a `case`/`default` label starting at
     *  {@code kwIdx}, or {@link #COLON_FOUND}/{@link #ARROW_NOT_FOUND} -- see {@link #findArrowCases}. */
    private int findCaseArrowOrColon(final List<Token> tokens, final int kwIdx, final int limit) {
        int depth = 0;
        for (int i = kwIdx + 1; i < limit; i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "(") || isPunct(t, "[")) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]")) {
                depth--;
            } else if (depth == 0 && isOp(t, "->")) {
                return i;
            } else if (depth == 0 && isOp(t, ":")) {
                return COLON_FOUND;
            } else if (depth == 0 && (isPunct(t, ";") || isPunct(t, "{"))) {
                return ARROW_NOT_FOUND;
            }
        }
        return ARROW_NOT_FOUND;
    }

    /** Pads every case's label to the widest in {@code cases} (trailing-cell trick, same precedent
     *  as {@code SwitchRule.applyInlineAlignment}'s label cell) and rewrites only the label span --
     *  body content from {@code bodyStartIdx} onward is left completely untouched. */
    private void applyArrowAlignment(final List<ArrowCase> cases, final Map<Integer, String> overrides) {
        final ColumnGrid grid = new ColumnGrid();
        for (final ArrowCase c : cases) {
            grid.addRow(new String[] {c.label + " ", ""});
        }
        final List<String[]> padded = grid.flush();

        for (int i = 0; i < cases.size(); i++) {
            final ArrowCase c = cases.get(i);
            overrides.put(c.kwIdx, padded.get(i)[0] + "-> ");
            for (int k = c.kwIdx + 1; k < c.bodyStartIdx; k++) {
                overrides.put(k, "");
            }
        }
    }

    /** Renders {@code tokens} with each entry in {@code overrides} substituted for that token's
     *  own text -- same minimal-touch rendering precedent as {@code SwitchRule.render}. */
    private String render(final List<Token> tokens, final Map<Integer, String> overrides) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            final String override = overrides.get(i);
            out.append(override != null ? override : tokens.get(i).text);
        }
        return out.toString();
    }

    private int matchParenForward(final List<Token> tokens, final int openIdx) {
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

    private boolean isOp(final Token t, final String text) {
        return t != null && t.type == TokenType.OP && text.equals(t.text);
    }

    /** Nearest preceding {@code class}/{@code interface} KEYWORD token, or -1 -- bounded-effort,
     *  no depth tracking, same posture as the rest of this codebase's non-AST heuristics. */
    private int prevClassOrInterfaceKeyword(final List<Token> tokens, final int fromExclusive) {
        for (int i = fromExclusive - 1; i >= 0; i--) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.KEYWORD && ("class".equals(t.text) || "interface".equals(t.text))) {
                return i;
            }
        }
        return -1;
    }

    /** Nearest following PUNCT token matching {@code text}, or -1. */
    private int nextPunct(final List<Token> tokens, final int fromExclusive, final String text) {
        for (int i = fromExclusive + 1; i < tokens.size(); i++) {
            if (isPunct(tokens.get(i), text)) {
                return i;
            }
        }
        return -1;
    }

    /** Index of the first significant token on the same physical line as {@code idx}. */
    private int lineStartIndex(final List<Token> tokens, final int idx) {
        int newlineIdx = -1;
        for (int i = idx; i >= 0; i--) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                newlineIdx = i;
                break;
            }
        }
        final int firstSig = nextSignificantIndex(tokens, newlineIdx);
        return firstSig < 0 ? idx : firstSig;
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
