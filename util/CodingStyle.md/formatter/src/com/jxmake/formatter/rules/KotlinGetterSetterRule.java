/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * STYLE_KOTLIN.md §8/§9: extends {@link GetterSetterRuleCurly} for Kotlin expression-bodied
 * one-liner functions (e.g. {@code fun getX(): Int = 1}), fixing the RDD_KEY_132 gap where
 * {@code GetterSetterRuleCurly.groupOneLiners} never grouped Kotlin one-liners at all --
 * {@code isClassScope}'s gate (Java, or a C++-style {@code public:}/{@code private:} label)
 * never recognizes Kotlin, and {@code parseOneLinerMember}'s modifier-consuming loop plus
 * name/return-type scanning assume C/Java's {@code [modifiers] ReturnType name(...)} token
 * order, the reverse of Kotlin's {@code [modifiers] fun name(...): ReturnType = expr}.
 *
 * Same "loosen shared-class visibility, then extend" pattern as
 * {@link KotlinDeclarationAlignmentRule} (RDD_KEY_103) and {@link KotlinSignatureRule}
 * (RDD_KEY_104): the base class's C/C++/Java-agnostic private helpers were raised to
 * {@code protected} (no behavior change), and this subclass supplies its own newline-terminated
 * member splitter, its own name-before-type one-liner parser, and its own column-grid renderer,
 * reusing {@link GetterSetterRuleCore.Member} purely as an index-range container (its
 * {@code bodyFrom}/{@code bodyTo} fields are repurposed here to hold the expression-bodied
 * function's {@code = expr} span rather than a brace-delimited block body, so
 * {@code excludeOutliers}'s width-based outlier exclusion still works unmodified).
 *
 * <p>Scope: expression-bodied one-liner functions (§9) -- the shape harness-confirmed broken in
 * STATE_KOTLIN.md's Open Questions (`fun getX(): Int = 1` / `getY` / `getZ` failing to
 * column-align) -- fixed under RDD_KEY_132.
 *
 * <p><b>§8 property accessors (RDD_KEY_133):</b> also handles the structurally different shape
 * of a {@code val}/{@code var} property declaration immediately followed by its own
 * expression-bodied {@code get() = expr} accessor line (e.g. {@code val x: Int} /
 * {@code get() = 1}), grouping adjacent instances of that two-line unit and rendering each as a
 * single merged, column-aligned line ({@code val x    : Int    get() = 1}), the same "collapse
 * one-liner shapes into a grid" posture already used for §9 -- the two-line source form is a
 * legal Kotlin *default* rendering for a standalone accessor (STYLE_KOTLIN.md §8's own worked
 * example), not a hard requirement preserved even when the accessor is one of 2+ adjacent
 * one-liner siblings sharing a group, mirroring §9's already-established willingness to
 * reformat/collapse an otherwise-legal multi-token shape for the sake of group alignment.
 * Deliberately scoped to the plain getter-only shape: a bare {@code get() = expr} with no
 * modifiers of its own, no accompanying {@code set(...)}, and no initializer on the property
 * line -- block-bodied accessors ({@code get() { ... }} / {@code set(v) { ... }}), any property
 * that pairs a getter with a setter, and any property with both an initializer and a custom
 * accessor are all left completely untouched (still "preserved as written" per §8's own
 * requirement, same posture §9's own scoping note took for block-bodied functions). See
 * STATE_KOTLIN.md's Open Questions for the full narrative and remaining gaps.
 */
public class KotlinGetterSetterRule extends GetterSetterRuleCurly {

    private static final List<String> FUN_MODIFIERS = Arrays.asList(
            "public", "private", "protected", "internal", "override", "open", "final",
            "abstract", "inline", "suspend", "operator", "infix", "tailrec", "external");

    /** Modifiers that can precede a `val`/`var` property declaration (STYLE_KOTLIN.md §6/§8). */
    private static final List<String> PROPERTY_MODIFIERS = Arrays.asList(
            "public", "private", "protected", "internal", "override", "open", "final",
            "abstract", "const", "lateinit");

    /** {@link Member#pureSpecifier} marker distinguishing a §8 property-accessor member (this
     *  field is otherwise unused/null for Kotlin, so it is repurposed here as a shape tag rather
     *  than adding a new field to the shared {@code Member} class). */
    private static final String ACCESSOR_MARKER = "ACCESSOR";

    public KotlinGetterSetterRule(final Lang lang) {
        super(lang);
    }

    public KotlinGetterSetterRule(final Lang lang, final int indentWidth, final int lineLengthLimit) {
        super(lang, indentWidth, lineLengthLimit);
    }

    /**
     * Kotlin has no C/Java-style class-scope/`{ }`-nesting-vs-file-scope distinction that would
     * change this rule's behavior (no access-specifier labels, and top-level file-scope
     * expression-bodied functions are just as groupable as class members) -- always true, same
     * unconditional posture as the base class's own {@code lang.isJava} branch.
     */
    @Override
    public List<List<Member>> groupOneLiners(final List<Token> scopeTokens, final int depth) {
        final List<int[]> spans = splitKotlinMemberSpans(scopeTokens);
        final List<List<Member>> groups = new ArrayList<>();
        List<Member> current = new ArrayList<>();

        int i = 0;
        while (i < spans.size()) {
            final int[] span = spans.get(i);
            Member m = parseKotlinOneLinerMember(scopeTokens, span[0], span[1], depth);
            int consumed = 1;
            if (m == null && i + 1 < spans.size()) {
                final int[] accSpan = spans.get(i + 1);
                m = parseKotlinAccessorMember(scopeTokens, span, accSpan);
                if (m != null) {
                    consumed = 2;
                }
            }
            if (m == null) {
                if (current.size() >= 2) {
                    groups.add(current);
                }
                current = new ArrayList<>();
                i += 1;
                continue;
            }
            final boolean shapeChanged = !current.isEmpty()
                    && isAccessorMember(current.get(current.size() - 1)) != isAccessorMember(m);
            if ((m.blankLineBefore || shapeChanged) && !current.isEmpty()) {
                if (current.size() >= 2) {
                    groups.add(current);
                }
                current = new ArrayList<>();
            }
            current.add(m);
            i += consumed;
        }
        if (current.size() >= 2) {
            groups.add(current);
        }
        return groups;
    }

    private boolean isAccessorMember(final Member m) {
        return ACCESSOR_MARKER.equals(m.pureSpecifier);
    }

    /**
     * Recognises a two-line STYLE_KOTLIN.md §8 unit: a {@code [modifiers] val/var name [: Type]}
     * property declaration (no initializer) immediately followed -- no blank line, no comment
     * gap -- by a bare {@code get() = expr} expression-bodied accessor line. Returns null for
     * anything else (initializer present, `set` present, block-bodied accessor, multi-line
     * expression, or any other unrecognised shape) -- same "never guess past an unrecognized
     * shape" posture as {@link #parseKotlinOneLinerMember}.
     */
    private Member parseKotlinAccessorMember(final List<Token> tokens, final int[] declSpan, final int[] accSpan) {
        final int declFrom = declSpan[0];
        final int declTo = declSpan[1];
        final int firstSig = firstSignificantIndex(tokens, declFrom, declTo);
        if (firstSig < 0) {
            return null;
        }
        final boolean blankBefore = hasBlankLineRun(tokens, declFrom, firstSig);

        int pos = firstSig;
        while (pos < declTo) {
            final Token t = tokens.get(pos);
            if (isInsignificant(t)) {
                pos++;
                continue;
            }
            if (t.type == TokenType.KEYWORD && PROPERTY_MODIFIERS.contains(t.text)) {
                pos++;
                continue;
            }
            break;
        }
        final int valVarIdx = nextSignificant(tokens, pos, declTo);
        if (valVarIdx < 0 || valVarIdx != pos || tokens.get(valVarIdx).type != TokenType.KEYWORD
                || !("val".equals(tokens.get(valVarIdx).text) || "var".equals(tokens.get(valVarIdx).text))) {
            return null;
        }
        final int declLeadFrom = firstSig;
        final int declLeadTo = valVarIdx + 1;

        final int nameIdx = nextSignificant(tokens, valVarIdx + 1, declTo);
        if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
            return null;
        }

        String typeText = "";
        int scanFrom = nameIdx + 1;
        final int colonIdx = nextSignificant(tokens, scanFrom, declTo);
        if (colonIdx >= 0 && isOp(tokens.get(colonIdx), ":")) {
            final int typeStart = nextSignificant(tokens, colonIdx + 1, declTo);
            if (typeStart < 0) {
                return null;
            }
            final int typeEnd = trimTrailingWs(tokens, typeStart, declTo);
            if (typeEnd <= typeStart) {
                return null;
            }
            typeText = cellText(tokens, typeStart, typeEnd);
            scanFrom = typeEnd;
        }
        // No `=` initializer allowed on the declaration line itself -- out of scope (a property
        // with both an initializer and a custom accessor is a different, more complex shape).
        if (nextSignificant(tokens, scanFrom, declTo) >= 0) {
            return null;
        }

        // Accessor span: must start (after any leading gap) directly at `get` -- no blank line,
        // no leading comment -- and be a bare `get() = expr` with nothing else in the span.
        final int accFrom = accSpan[0];
        final int accTo = accSpan[1];
        final int accFirstSig = firstSignificantIndex(tokens, accFrom, accTo);
        if (accFirstSig < 0 || hasBlankLineRun(tokens, accFrom, accFirstSig)) {
            return null;
        }
        // Reject a leading comment on the accessor line -- keeps the merge conservative (a
        // comment documenting the accessor stays attached to the two-line form, untouched).
        for (int k = accFrom; k < accFirstSig; k++) {
            final TokenType kt = tokens.get(k).type;
            if (kt == TokenType.COMMENT_LINE || kt == TokenType.COMMENT_BLOCK) {
                return null;
            }
        }
        if (tokens.get(accFirstSig).type != TokenType.KEYWORD || !"get".equals(tokens.get(accFirstSig).text)) {
            return null;
        }
        final int parenOpenIdx = nextSignificant(tokens, accFirstSig + 1, accTo);
        if (parenOpenIdx < 0 || !isPunct(tokens.get(parenOpenIdx), "(")) {
            return null;
        }
        final int parenCloseIdx = nextSignificant(tokens, parenOpenIdx + 1, accTo);
        if (parenCloseIdx < 0 || !isPunct(tokens.get(parenCloseIdx), ")")) {
            return null; // a parameterized/non-empty accessor parens -- not a bare getter
        }
        final int eqIdx = nextSignificant(tokens, parenCloseIdx + 1, accTo);
        if (eqIdx < 0 || !isOp(tokens.get(eqIdx), "=")) {
            return null; // block-bodied getter, or something else entirely
        }
        if (hasNewlineBetween(tokens, eqIdx + 1, accTo)) {
            return null; // multi-line expression body -- not a one-liner
        }

        int lastIdx = accTo - 1;
        while (lastIdx > eqIdx && (tokens.get(lastIdx).type == TokenType.WHITESPACE
                || tokens.get(lastIdx).type == TokenType.NEWLINE)) {
            lastIdx--;
        }
        if (lastIdx <= eqIdx) {
            return null; // no expression body
        }
        Token trailingComment = null;
        int bodyTo;
        if (tokens.get(lastIdx).type == TokenType.COMMENT_LINE || tokens.get(lastIdx).type == TokenType.COMMENT_BLOCK) {
            trailingComment = tokens.get(lastIdx);
            bodyTo = trimTrailingWs(tokens, eqIdx + 1, lastIdx);
        } else {
            bodyTo = lastIdx + 1;
        }
        final int bodyFrom = trimLeadingWs(tokens, eqIdx + 1, bodyTo);
        if (bodyFrom >= bodyTo) {
            return null; // empty expression body
        }

        return new Member(new ArrayList<Token>(), declLeadFrom, declLeadFrom,
                declLeadFrom, declLeadTo,
                nameIdx, nameIdx, nameIdx, nameIdx,
                bodyFrom, bodyTo, declFrom, accTo,
                trailingComment, blankBefore,
                typeText, ACCESSOR_MARKER, false);
    }

    /**
     * Splits {@code scopeTokens} into newline-terminated statement spans (plus `;`-terminated
     * ones, in case a stray semicolon survives), mirroring
     * {@code KotlinDeclarationAlignmentRule.splitKotlinStatements}'s int-index-based sibling --
     * the base class's own {@code splitMembers} assumes every statement ends in a top-level `;`
     * or a `{ ... }` block, which is wrong for Kotlin's newline-terminated, semicolon-optional
     * grammar (an expression-bodied one-liner has neither).
     */
    private List<int[]> splitKotlinMemberSpans(final List<Token> scopeTokens) {
        final List<int[]> spans = new ArrayList<>();
        final int n = scopeTokens.size();
        int start = 0;
        int depth = 0;
        boolean sawSignificant = false;
        int i = 0;
        while (i < n) {
            final Token t = scopeTokens.get(i);
            if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                depth++;
                sawSignificant = true;
                i++;
                continue;
            }
            if (isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")) {
                depth--;
                sawSignificant = true;
                i++;
                continue;
            }
            if (depth == 0 && isPunct(t, ";")) {
                i++;
                spans.add(new int[] {start, i});
                start = i;
                sawSignificant = false;
                continue;
            }
            if (depth == 0 && t.type == TokenType.NEWLINE && sawSignificant) {
                spans.add(new int[] {start, i});
                start = i; // the newline itself becomes the next span's leading gap
                sawSignificant = false;
                continue;
            }
            if (!isInsignificant(t)) {
                sawSignificant = true;
            }
            i++;
        }
        if (start < n) {
            spans.add(new int[] {start, n});
        }
        return spans;
    }

    /**
     * Recognises a single-line {@code [modifiers] fun name(params) [: ReturnType] = expr}
     * expression-bodied function (STYLE_KOTLIN.md §9). Returns null for anything else --
     * block-bodied functions ({@code { ... }}), functions with no {@code =} at all, multi-line
     * members, and any unrecognised shape -- same "never guess past an unrecognized shape"
     * posture as every other Kotlin-aware rule in this codebase.
     */
    private Member parseKotlinOneLinerMember(final List<Token> tokens, final int from, final int to,
            final int nestDepth) {
        final int firstSig = firstSignificantIndex(tokens, from, to);
        if (firstSig < 0) {
            return null;
        }
        final boolean blankBefore = hasBlankLineRun(tokens, from, firstSig);
        if (hasNewlineBetween(tokens, firstSig, to)) {
            return null;
        }

        int pos = firstSig;
        while (pos < to) {
            final Token t = tokens.get(pos);
            if (isInsignificant(t)) {
                pos++;
                continue;
            }
            if (t.type == TokenType.KEYWORD && FUN_MODIFIERS.contains(t.text)) {
                pos++;
                continue;
            }
            break;
        }

        final int funIdx = nextSignificant(tokens, pos, to);
        if (funIdx < 0 || tokens.get(funIdx).type != TokenType.KEYWORD || !"fun".equals(tokens.get(funIdx).text)) {
            return null; // only plain functions (not extension receivers/generics) supported
        }
        final int returnTypeFrom = firstSig; // "[modifiers] fun" rendered verbatim as one lead cell
        final int returnTypeTo = funIdx + 1;

        final int nameIdx = nextSignificant(tokens, funIdx + 1, to);
        if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
            return null;
        }

        final int parenOpenIdx = nextSignificant(tokens, nameIdx + 1, to);
        if (parenOpenIdx < 0 || !isPunct(tokens.get(parenOpenIdx), "(")) {
            return null;
        }
        final int parenCloseIdx = matchBracket(tokens, parenOpenIdx, "(", ")");
        if (parenCloseIdx < 0) {
            return null;
        }
        final int paramsFrom = trimLeadingWs(tokens, parenOpenIdx + 1, parenCloseIdx);
        final int paramsTo = trimTrailingWs(tokens, paramsFrom, parenCloseIdx);

        int scanFrom = parenCloseIdx + 1;
        String postParenQualifier = "";
        final int colonIdx = nextSignificant(tokens, scanFrom, to);
        if (colonIdx >= 0 && isOp(tokens.get(colonIdx), ":")) {
            final int typeStart = nextSignificant(tokens, colonIdx + 1, to);
            if (typeStart < 0) {
                return null;
            }
            int angleDepth = 0;
            int typeEnd = -1;
            for (int k = typeStart; k < to; k++) {
                final Token tk = tokens.get(k);
                if (isPunct(tk, "(") || tk.type == TokenType.ANGLE_BRACKET_OPEN) {
                    angleDepth++;
                } else if (isPunct(tk, ")") || tk.type == TokenType.ANGLE_BRACKET_CLOSE) {
                    angleDepth--;
                } else if (angleDepth == 0 && isOp(tk, "=")) {
                    typeEnd = k;
                    break;
                }
            }
            if (typeEnd < 0) {
                return null; // no '=' after the return type -- not an expression body
            }
            final int typeTrimEnd = trimTrailingWs(tokens, typeStart, typeEnd);
            if (typeTrimEnd <= typeStart) {
                return null;
            }
            postParenQualifier = cellText(tokens, typeStart, typeTrimEnd);
            scanFrom = typeEnd;
        }

        final int eqIdx = nextSignificant(tokens, scanFrom, to);
        if (eqIdx < 0 || !isOp(tokens.get(eqIdx), "=")) {
            return null; // block-bodied, or an explicit-return-type function with no expr body
        }

        // Find the last non-whitespace/newline token in range -- distinguishing a genuine
        // trailing same-line comment (kept as its own cell) from the expression body itself,
        // since isInsignificant() treats comments the same as whitespace for ordinary trimming.
        int lastIdx = to - 1;
        while (lastIdx > eqIdx && (tokens.get(lastIdx).type == TokenType.WHITESPACE
                || tokens.get(lastIdx).type == TokenType.NEWLINE)) {
            lastIdx--;
        }
        if (lastIdx <= eqIdx) {
            return null; // no expression body
        }
        Token trailingComment = null;
        int bodyTo;
        if (tokens.get(lastIdx).type == TokenType.COMMENT_LINE || tokens.get(lastIdx).type == TokenType.COMMENT_BLOCK) {
            trailingComment = tokens.get(lastIdx);
            bodyTo = trimTrailingWs(tokens, eqIdx + 1, lastIdx);
        } else {
            bodyTo = lastIdx + 1;
        }
        final int bodyFrom = trimLeadingWs(tokens, eqIdx + 1, bodyTo);
        if (bodyFrom >= bodyTo) {
            return null; // empty expression body
        }

        // A body containing a non-empty-arg call is exactly the shape `enforceCallLineBreaking`
        // (later in the pipeline) may break across multiple lines if it doesn't fit -- check that
        // predicted width here so grouping/padding is decided consistently whether this is a
        // fresh format (body still on one physical line) or a reformat of output already broken
        // by that later phase. Without this, a fresh format groups/pads a too-long member using
        // its original single-line text (still short at this point), only for the later phase to
        // break it; reformatting that already-broken output then correctly excludes the
        // now-multi-line member via `hasNewlineBetween` above, splitting the run into different
        // subgroups with different padding on the second pass (RDD_KEY_138) -- same length
        // pre-check GetterSetterRuleCurly.parseOneLinerMember already uses for this exact reason (its
        // own doc comment explains the flip-flop), never previously ported to this Kotlin sibling.
        if (hasBreakableCall(tokens, bodyFrom, bodyTo)) {
            final int estimatedColumn = nestDepth * indentWidth;
            final int estimatedWidth = estimatedColumn + cellText(tokens, firstSig, to).length();
            if (estimatedWidth > lineLengthLimit) {
                return null;
            }
        }

        return new Member(new ArrayList<Token>(), returnTypeFrom, returnTypeFrom,
                returnTypeFrom, returnTypeTo,
                nameIdx, nameIdx, paramsFrom, paramsTo,
                bodyFrom, bodyTo, from, to,
                trailingComment, blankBefore,
                postParenQualifier, null, false);
    }

    /**
     * Renders one aligned group of expression-bodied one-liner functions (STYLE_KOTLIN.md §9):
     * {@code [modifiers] fun} / {@code name(params)[: ReturnType]} / {@code = expr} as three
     * grid-aligned columns, plus a trailing (ragged) comment column when present -- the base
     * class's own {@code render} cannot be reused as-is since it hard-codes a `{ body };`
     * definition/declaration shape that doesn't exist in Kotlin's postfix-type, no-semicolon
     * grammar. No width-budget awareness -- see {@link #render(List, List, int)} for that; this
     * overload is kept for the base-class override contract and any caller that doesn't have a
     * {@code depth} to pass (none currently outside this class).
     */
    @Override
    public List<String> render(final List<Token> tokens, final List<Member> group) {
        if (!group.isEmpty() && isAccessorMember(group.get(0))) {
            return renderAccessorGroupRaw(tokens, group);
        }
        return renderRaw(tokens, group);
    }

    /**
     * RDD_KEY_220 -- width-budget-aware entry point, mirroring {@code KotlinDeclarationAlignmentRule
     * .renderAlignedGroup(List, int)}'s own RDD_KEY_162 fix, ported to this class's one-liner
     * grouping. {@code depth} is the group's scope depth (as passed to {@link #groupOneLiners});
     * {@code depth * indentWidth} is the same estimated-column approximation
     * {@link #parseKotlinOneLinerMember}'s own raw-length pre-check already uses.
     *
     * <p>Root cause fixed here: {@link #parseKotlinOneLinerMember}'s raw-length pre-check (and
     * {@code excludeOutliers}'s width-ratio outlier test) only ever excludes a member whose OWN
     * solo/raw width already overflows {@code lineLengthLimit} before any group padding is
     * applied. A member whose own raw width fits comfortably but whose GROUP-PADDED width (once
     * every row's columns are stretched to the group's widest member) overflows the limit was
     * never excluded at all -- {@code render}/{@code renderAccessorGroupRaw} just flushed the
     * grid regardless, and a later phase ({@code enforceCallLineBreaking}) would then wrap that
     * member's now-too-long padded line, at which point a re-parse (via {@code splitKotlinMemberSpans}
     * failing to recognize the now-multi-line span as a single-liner) treats it as a hard group
     * boundary -- narrower, un-padded on round2 where round1 had padded it, an idempotency flap
     * structurally identical to RDD_KEY_162's own `ChannelFlow.kt` bug, just never ported to this
     * one-liner-function/accessor sibling. Fixed the same way: a fixed-point loop excluding any
     * row from the shared column grid (rendered solo instead) whenever its group-aligned rendered
     * width overflows the budget AND its body contains a breakable call ({@link #hasBreakableCall}
     * -- the same gate {@link #parseKotlinOneLinerMember}'s own raw-length pre-check already uses,
     * since only a body containing a call can ever be wrapped by {@code enforceCallLineBreaking}
     * in the first place; a body with no call at all can never become multi-line no matter how
     * long its padded line renders, so excluding it would only needlessly discard a sibling's
     * legitimate alignment). Surviving rows are then rendered as maximal CONTIGUOUS runs (not one
     * flat grid spanning the whole group), same RDD_KEY_219 rationale: a run adjacent to an
     * excluded row was already going to lose its shared padding with the row on the far side once
     * that excluded row wraps and hard-breaks the group on a re-format, so this pass must not
     * pretend otherwise from the very first pass.
     */
    @Override
    public List<String> render(final List<Token> tokens, final List<Member> group, final int depth) {
        if (group.size() <= 1 || indentWidth <= 0) {
            return render(tokens, group);
        }
        final boolean accessorShape = isAccessorMember(group.get(0));
        final int indent = depth * indentWidth;

        final boolean[] excluded = new boolean[group.size()];
        while (true) {
            final List<Integer> safeIndices = new ArrayList<>();
            for (int i = 0; i < group.size(); i++) {
                if (!excluded[i]) {
                    safeIndices.add(i);
                }
            }
            if (safeIndices.size() <= 1) {
                break; // nothing left to budget-check against
            }
            final List<Member> safeRows = new ArrayList<>();
            for (final int idx : safeIndices) {
                safeRows.add(group.get(idx));
            }
            final List<String> safeLines = accessorShape
                    ? renderAccessorGroupRaw(tokens, safeRows) : renderRaw(tokens, safeRows);

            int overflowAt = -1;
            for (int k = 0; k < safeRows.size(); k++) {
                final Member m = safeRows.get(k);
                if (!hasBreakableCall(tokens, m.bodyFrom, m.bodyTo)) {
                    continue;
                }
                if (indent + safeLines.get(k).length() > lineLengthLimit) {
                    overflowAt = safeIndices.get(k);
                    break;
                }
            }
            if (overflowAt < 0) {
                break; // fixed point reached -- no remaining row overflows its budget
            }
            excluded[overflowAt] = true;
        }

        final List<String> result = new ArrayList<>(java.util.Collections.nCopies(group.size(), null));
        int i = 0;
        while (i < group.size()) {
            if (excluded[i]) {
                final List<Member> solo = java.util.Collections.singletonList(group.get(i));
                result.set(i, (accessorShape ? renderAccessorGroupRaw(tokens, solo) : renderRaw(tokens, solo)).get(0));
                i++;
                continue;
            }
            int j = i;
            final List<Member> runRows = new ArrayList<>();
            while (j < group.size() && !excluded[j]) {
                runRows.add(group.get(j));
                j++;
            }
            final List<String> runLines = accessorShape
                    ? renderAccessorGroupRaw(tokens, runRows) : renderRaw(tokens, runRows);
            for (int k = 0; k < runLines.size(); k++) {
                result.set(i + k, runLines.get(k));
            }
            i = j;
        }
        return result;
    }

    private List<String> renderRaw(final List<Token> tokens, final List<Member> group) {
        final ColumnGrid grid = new ColumnGrid();
        for (final Member m : group) {
            final List<String> cells = new ArrayList<>();
            cells.add(cellText(tokens, m.returnTypeFrom, m.returnTypeTo));
            final String callCell = cellText(tokens, m.nameFrom, m.nameIdx + 1)
                    + "(" + cellText(tokens, m.paramsFrom, m.paramsTo) + ")";
            cells.add(callCell);
            cells.add(m.postParenQualifier.isEmpty() ? "" : ": " + m.postParenQualifier);
            cells.add("= " + cellText(tokens, m.bodyFrom, m.bodyTo));
            if (m.trailingComment != null) {
                cells.add(m.trailingComment.text);
            }
            grid.addRow(cells.toArray(new String[0]));
        }
        final List<String> lines = new ArrayList<>();
        for (final String[] row : grid.flush()) {
            lines.add(String.join(" ", row));
        }
        return lines;
    }

    /**
     * Renders one aligned group of §8 property-accessor one-liners as a single merged,
     * column-aligned line per member: {@code [modifiers] val/var} / {@code name} /
     * {@code [: Type]} / {@code get() = expr}, mirroring §9's {@code render}'s 4-column shape
     * (declaration lead / name-or-call / colon-qualifier / tail) with a Kotlin-legal single-line
     * property+accessor rendering (e.g. {@code val x    : Int    get() = 1}).
     */
    private List<String> renderAccessorGroupRaw(final List<Token> tokens, final List<Member> group) {
        final ColumnGrid grid = new ColumnGrid();
        for (final Member m : group) {
            final List<String> cells = new ArrayList<>();
            cells.add(cellText(tokens, m.returnTypeFrom, m.returnTypeTo));
            cells.add(cellText(tokens, m.nameFrom, m.nameIdx + 1));
            cells.add(m.postParenQualifier.isEmpty() ? "" : ": " + m.postParenQualifier);
            cells.add("get() = " + cellText(tokens, m.bodyFrom, m.bodyTo));
            if (m.trailingComment != null) {
                cells.add(m.trailingComment.text);
            }
            grid.addRow(cells.toArray(new String[0]));
        }
        final List<String> lines = new ArrayList<>();
        for (final String[] row : grid.flush()) {
            lines.add(String.join(" ", row));
        }
        return lines;
    }
}
