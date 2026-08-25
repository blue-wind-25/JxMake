/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter;

import java.util.List;

import com.jxmake.formatter.rules.BlockStructureRule;
import com.jxmake.formatter.rules.CppSpecificRule;
import com.jxmake.formatter.rules.JavaSpecificRule;
import com.jxmake.formatter.rules.JsTsSpecificRule;
import com.jxmake.formatter.rules.KotlinSpecificRule;
import com.jxmake.formatter.rules.MiscRuleCurly;
import com.jxmake.formatter.rules.SwitchRule;
import com.jxmake.formatter.tokenizer.TokenizerCore;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCurly;

/**
 * Curly-brace-family formatter (C/C++/Java/Kotlin) -- everything in this file used to live
 * directly in {@code Formatter.formatOne} before the curly/indent/tags class-refactor
 * (STATE_COMMON.md's "Class Refactor" section); no behavior change, mechanical move only.
 */
public final class FormatterCurly extends FormatterCore {

    public FormatterCurly(final Lang lang)
    {
        super(lang);
    }

    /**
     * D3 fix attempt (2026-08-16, `STATE_CURLY_GDR.md`'s D3 tracking): `enforceCallLineBreaking`'s
     * no-newline fits-check measures a candidate against its enclosing physical source line, which
     * is volatile round-to-round for Kotlin -- an earlier sibling call/condition on the same
     * original source line getting wrapped by an EARLIER `formatOnePass` invocation permanently
     * shortens the physical-line prefix a LATER candidate on that same original line is measured
     * against on the next invocation, flipping its own wrap decision (confirmed via
     * `EqualityAndComparisonCallsTransformer.kt`'s `Name.identifier("compareTo") -> if
     * (doNotIntrinsify) call else transformCompareToMethodCall(call)` -- wraps both calls on a
     * fresh format since the whole original line is over limit, but only the first call once
     * re-tokenized from its own wrapped output, since removing the first call's own text from the
     * physical line now leaves the second comfortably under limit). Every prior fix attempt
     * (RDD_KEY_221/226/252/253) tried to re-derive a stable statement-start boundary via a backward
     * token scan -- all six reverted, each regressing a different Kotlin shape (no reliable `;`
     * statement terminator in Kotlin to anchor on). This attempt sidesteps needing a stable boundary
     * at all: mirrors `GdrPipelineGate.applyAndFormat`'s already-proven convergence-loop precedent
     * (`RDD_KEY_241`) by running the whole one-pass pipeline (renamed to {@link #formatOnePass})
     * TWICE for Kotlin only, so an external caller's first (and only) invocation already sees the
     * fixed point a second external round would have produced -- no heuristic, no statement-
     * boundary classification, same "just run the pipeline again" idea GDR itself uses for the
     * structurally identical circular-dependency shape. Bounded to `lang.isKotlin` (the only family
     * D3 was ever confirmed in) to keep the extra pass's cost off every other language's default
     * path. `MAX_SETTLE_PASSES` mirrors GDR's own `MAX_MULTIPASS_CYCLES` safety-cap precedent,
     * much smaller since the confirmed oscillation is 2-cycle, not deeper.
     */
    private static final int MAX_SETTLE_PASSES = 5;

    @Override
    public String formatOne(
        final String  content,
        final String  filePath,
        final Config  config,
        final boolean formatOff
    )
    {
        String prev = formatOnePass(content, filePath, config, formatOff);
        if(!lang.isKotlin) return prev;

        for(int i = 0; i < MAX_SETTLE_PASSES; ++i) {
            final String next = formatOnePass(prev, filePath, config, formatOff);
            if( next.equals(prev) ) return prev;
            prev = next;
        } // for

        return prev; // Did not converge within the cap -- return the last computed pass rather than looping forever
    }

    private String formatOnePass(
        final String  content,
        final String  filePath,
        final Config  config,
        final boolean formatOff
    )
    {
        final TokenizerCurly tokenizerCore = new TokenizerCurly(lang);
        final boolean        isCOrCpp      = lang.isCpp || lang.isC;

        final int indentWidth     = config.indentSize();
        final int lineLengthLimit = config.lineLength();

        final java.util.function.Function<String, List<Token>> tokenizer = (final String s) -> {
            final List<Token> tokens = tokenizerCore.tokenize(s);
            TokenizerCore.markFrozenSpans(tokens, formatOff);
            // STATE_JS_TS.md's Step 2 "context 11" scoping session, Increment 1
            // (detect-and-measure-only) -- purely observational, see JsxWrapDiagnostics's own doc
            // comment. Zero effect on `tokens`/output; records nothing when the file has no
            // JSX_SPAN tokens at all (every non-.jsx/.tsx file, by construction).
            if(lang.isJsxSyntax) {
                for(final Token t : tokens) {
                    if(t.type == com.jxmake.formatter.tokenizer.TokenizerCore.TokenType.JSX_SPAN && t.jsxOpeningTagEndOffset >= 0) com.jxmake.formatter.tokenizer.JsxWrapDiagnostics.recordOpeningTagMeasurement(
                        t.jsxOpeningTagEndOffset, lineLengthLimit
                    );
                } // for
            } // if
            return tokens;
        };

        final int                lineLengthWithCommentLimit = config.lineLengthWithComment();
        final BlockStructureRule blockRule                  = new BlockStructureRule(
            lang, config.closingCommentMinLines(), indentWidth, lineLengthLimit
        );
        final SwitchRule         switchRule                 = new SwitchRule(
            lang, lineLengthLimit, indentWidth
        );
        final MiscRuleCurly      miscRule                   = new MiscRuleCurly(
            lang,
            config.isNormalizeCommentStartCase(),
            config.isNormalizeCommentEndPeriod(),
            config.isCommentNormalizationClassifier(),
            config.isGruClassifier(),
            config.gruWeightsPath(),
            indentWidth,
            lineLengthLimit,
            lineLengthWithCommentLimit
        );
        miscRule.setNormalizeCommentMultiSentenceCase(
            config.isNormalizeCommentMultiSentenceCase()
        );
        final CppSpecificRule    cppRule    = isCOrCpp ? new CppSpecificRule(
            lang, lineLengthLimit, indentWidth
        ) : null;
        final JavaSpecificRule   javaRule   = lang.isJava ? new JavaSpecificRule(
            lang, lineLengthLimit, indentWidth, lineLengthWithCommentLimit
        ) : null;
        final KotlinSpecificRule kotlinRule = lang.isKotlin ? new KotlinSpecificRule(
            lang, lineLengthLimit, indentWidth
        ) : null;
        final JsTsSpecificRule   jsTsRule   = (lang.isJs || lang.isTs) ? new JsTsSpecificRule(
            lang, lineLengthLimit, indentWidth
        ) : null;

        // Phase 0: §5/§6/§8/§14 grouping rules, recursive.
        // Pre-pad complexity spacing (§3.1) before grouping/column-width computation -- otherwise
        // GetterSetterRule's body-column width can be measured against a body's pre-padding text
        // (e.g. `Math.max( 0, lvl )`) which Phase 1's own enforceComplexityPadding call later
        // shrinks (e.g. to `Math.max(0, lvl)`), leaving a sibling member's trailing padding stale
        // by the amount stripped -- stable only on a second format pass, when the input is already
        // post-padding. enforceComplexityPadding is idempotent, so re-running it in Phase 1 after
        // Phase 0's own transformations (which can introduce new one-liner bodies) is still needed
        // and safe.
        String text = miscRule.enforceComplexityPadding( tokenizer.apply(content) );
        // Same reasoning applies to template angle-bracket spacing (e.g. `static_cast<T>` ->
        // `static_cast< T >` for a "loose" nested template arg): Phase 4's own
        // enforceTemplateAngleBracketSpacing call runs long after this Phase 0 column-width
        // measurement, so a getter/setter body containing an unpadded `static_cast<...>` measures
        // narrower here than its final rendered width -- a sibling's alignment padding then goes
        // stale by exactly the padding later added, stable only on a second format pass (see
        // STATE.md's `frozen`/`map.hpp` `key_comp()`/`value_comp()` bug). Pulled forward here,
        // same as enforceComplexityPadding above; the Phase 4 call stays too (idempotent, and
        // still needed for any angle brackets introduced by Phase 0-3's own transformations).
        if(lang.isCpp) text = cppRule.enforceTemplateAngleBracketSpacing( tokenizer.apply(text) );
        if(lang.isJs || lang.isTs) {
            // STYLE_JS_TS.md §2: always insert explicit semicolons. Must run before
            // ScopePipelineCurly.process below -- its own §11 declaration-alignment-grid pass
            // (JsTsDeclarationAlignmentRule.parseDeclaration) requires a literal `;` token to
            // recognize a statement's end at all; an ASI-reliant statement with no explicit `;`
            // yet (e.g. `const calc = (a, b) => a + b`) was silently bailing out of grid-grouping
            // entirely -- not just missing alignment, but every flat cosmetic-spacing pass inside
            // that dropped group (comma spacing among them) never ran either, since none of them
            // ever got the chance to re-tokenize that span. Confirmed via a standalone harness:
            // the same statement list aligned/spaced correctly once already `;`-terminated, but
            // silently fell back to raw input when relying on ASI. Also still runs before
            // collapseSingleExpressionBlocks (now further down, inside Phase 1) for the original
            // reason recorded below.
            text = jsTsRule.enforceSemicolonInsertion( tokenizer.apply(text) );
        } // if
        final ScopePipelineCurly scopePipeline = new ScopePipelineCurly(
            lang,
            config.indentStyle(),
            config.isNormalizeCommentStartCase(),
            config.isNormalizeCommentEndPeriod(),
            config.isCommentNormalizationClassifier(),
            config.isGruClassifier(),
            config.gruWeightsPath(),
            formatOff,
            indentWidth,
            lineLengthLimit,
            lineLengthWithCommentLimit
        );
        // RDD_KEY_350: guards `parseAssignment`'s STYLE.md §6 multi-line-RHS recognition against
        // misclassifying `enforceOperatorLineBreaking`'s own continuation-line output shape (see
        // STATE_LINE_SPLIT_OP.md's Known Out-of-Scope Finding). No-op when the flag is off
        // (default), matching every other gated call in this method.
        scopePipeline.setLineSplitOperatorPriority( config.lineSplitOperatorPriority() );
        text = scopePipeline.process(text);

        // Phase 1: structural/brace passes.
        // (STYLE_JS_TS.md §2 semicolon insertion itself now runs earlier, right before
        // ScopePipelineCurly.process above -- see the comment there. It must still run before
        // collapseSingleExpressionBlocks below: that pass's braceless-collapse eligibility check
        // reads the block body's own trailing token, so if semicolon insertion ran later, round1
        // (no semicolon yet) and round2 (semicolon already present from round1's own output)
        // would feed collapseSingleExpressionBlocks two different token shapes for the same
        // logical statement, breaking idempotency (confirmed via a standalone harness: `if (x) {
        // doThing() }` stayed braced on round1 but collapsed to `if(x) doThing();` on round2 when
        // this ran after the collapse pass instead).
        if(lang.isTs) {
            // STYLE_JS_TS.md §12: TS enum bodies are always reflowed to one member per line
            // (regardless of original layout) with `=` column-alignment when explicit values are
            // present. Must run before collapseSingleExpressionBlocks/the general brace-style
            // passes below so those passes see the final, reflowed multi-line member list rather
            // than an original same-line member list.
            text = jsTsRule.enforceEnumMemberFormatting( tokenizer.apply(text) );
            text = jsTsRule.enforceInterfaceTypeAliasMemberColonAlignment( tokenizer.apply(text) );
        } // if
        text = blockRule.collapseSingleExpressionBlocks( tokenizer.apply(text) );
        text = blockRule.enforceKAndRBraceStyle( tokenizer.apply(text) );
        text = blockRule.enforceNamedConstructHeaderSpacing( tokenizer.apply(text) );
        text = blockRule.placeElseOnOwnLine( tokenizer.apply(text) );
        text = blockRule.placeCatchFinallyOnOwnLine( tokenizer.apply(text) );
        text = blockRule.insertNamedConstructBlankLines( tokenizer.apply(text) );
        if(isCOrCpp) {
            text = cppRule.enforceFunctionDefinitionAllmanBraceStyle( tokenizer.apply(text) );
            text = cppRule.enforceEmptyParameterList( tokenizer.apply(text) );
            if(lang.isCpp) {
                text = cppRule.enforceRequiresClausePlacement( tokenizer.apply(text) );
                text = cppRule.enforceContractClausePlacement( tokenizer.apply(text) );
            }
        } // if
        else if(lang.isJava) {
            text = javaRule.enforceMethodDefinitionAllmanBraceStyle( tokenizer.apply(text) );
            text = javaRule.enforcePermitsClauseLineBreaking( tokenizer.apply(text) );
            text = javaRule.separateEnumConstantListTerminator( tokenizer.apply(text) );
        }
        else if(lang.isKotlin) {
            text = kotlinRule.enforceFunctionDefinitionAllmanBraceStyle( tokenizer.apply(text) );
            text = kotlinRule.separateEnumConstantListTerminator( tokenizer.apply(text) );
            text = kotlinRule.stripOptionalSemicolons( tokenizer.apply(text) );
            text = kotlinRule.stripLeadingBlankBeforeNonDeclarationStatement(
                tokenizer.apply(text)
            );
        }
        else if(lang.isJs || lang.isTs) {
            // STYLE_JS_TS.md §5: named function declarations/class methods move their `{` to its
            // own line (Allman) -- arrow-function block bodies, empty bodies, and one-liner
            // (getter/setter-style) bodies stay K&R, excluded internally by the method itself.
            text = jsTsRule.enforceMethodDefinitionAllmanBraceStyle( tokenizer.apply(text) );
        }
        if(lang.isTs) {
            // Same "measurement must see the final width" reasoning as the comment on
            // enforceDecoratorOverflowCascade right below (this must run BEFORE that pass, not
            // merely before enforceCallLineBreaking): STYLE_JS_TS.md #11's union/intersection
            // `|`/`&` spacing (`string|number` -> `string | number`) and type-annotation colon
            // spacing are both ordinarily Phase 4 passes (further down), but
            // enforceDecoratorOverflowCascade's own "does the whole inline decorator+target line
            // fit" measurement runs first of all the width-driven passes -- a decorator argument
            // whose type text sits right at lineLengthLimit measures as "fits" (stays inline) using
            // the pre-spacing width, then grows past the limit once this spacing is added later
            // with no further re-check -- a fresh format stays inline (self-violating over-limit)
            // while a reformat of already-formatted output (spacing already baked in) sees the
            // wider, over-limit shape from the start and drops the decorator to its own line --
            // round1 != round2. Pulled forward from Phase 4 to run right before
            // enforceDecoratorOverflowCascade, same fix shape as
            // enforceComplexityPadding/enforceAttributeAndSpliceBracketPadding/
            // enforceInitializerBraceSpacing further down; the original Phase 4 calls are left in
            // place too (idempotent no-ops on anything this early call already normalized -- they
            // only additionally cover type shapes introduced/exposed by later passes). Found via
            // real-world testing (angular/angular's `input_transform.ts`
            // `@Input({transform: (value: string|number, _: any) => value ? 1 : 0})` decorator).
            text = jsTsRule.enforceUnionIntersectionSpacing( tokenizer.apply(text) );
            text = jsTsRule.enforceTypeColonSpacing( tokenizer.apply(text) );
        } // if
        if(lang.isJs || lang.isTs) {
            // STYLE_JS_TS.md §9's overflow cascade: drop an overlong inline decorator to its own
            // line before any width-driven pass below runs, so their "does it fit" checks see the
            // post-split shape on the very first format pass (same ordering reasoning as
            // enforceArrowFunctionParameterParens right below). The second cascade step (wrapping
            // the decorator's own overlong argument list) needs no call here -- it's already free
            // via enforceCallLineBreaking's generic "IDENTIFIER (" scan further down.
            text = jsTsRule.enforceDecoratorOverflowCascade( tokenizer.apply(text) );
        } // if
        if(lang.isJs || lang.isTs) {
            // STYLE_JS_TS.md §6: a bare single-parameter arrow (`n => ...`) always gets its
            // parameter wrapped in parens (`(n) => ...`). This must run before the
            // enforceComplexityPadding call right below (not down in Phase 4 with the rest of
            // this file's JS/TS cosmetic passes) for the same reason enforceComplexityPadding
            // itself was pulled forward in the comment below: inserting `(x)` introduces a new
            // nested-paren shape inside any enclosing call's argument list, and
            // enforceComplexityPadding's tight-vs-loose decision for that enclosing call has to
            // see the post-insertion shape on the very first format pass, or a fresh format
            // (pre-insertion, stays tight) and a reformat of already-formatted output
            // (post-insertion, goes loose) disagree -- not idempotent. Found via real-world
            // testing (`arr.map(x => x * 2)` staying tight on format #1 but going loose on #2).
            text = jsTsRule.enforceArrowFunctionParameterParens( tokenizer.apply(text) );
        } // if
        // EnforceCallLineBreaking's "does it fit in LINE_LENGTH_LIMIT" measurement must see
        // enforceComplexityPadding's loose `( x )` spacing already applied -- otherwise a line
        // right at the boundary can measure as "fits" here, then grow past the limit once padding
        // is added with no further re-check, only to (correctly, but inconsistently) get broken
        // the next time the file is formatted. So enforceComplexityPadding is pulled forward from
        // Phase 4 to run right before it, rather than moving enforceCallLineBreaking itself back
        // -- it must stay ahead of Phase 3's addClosingComments (same reasoning as the
        // alignInlineSwitches-before-addClosingComments comment below: a pass that can expand a
        // block's line count has to run before any line-count-threshold decision reads it, or that
        // decision sees a pre-expansion count on a fresh format and a post-expansion count on a
        // reformat of already-formatted output). Found via real-world testing (tinyexpr-plusplus's
        // bitwise_rotate_right/left overloads and te_parser::list's while loop,
        // github.com/blake-madden/tinyexpr-plusplus).
        text = miscRule.enforceComplexityPadding( tokenizer.apply(text) );
        // Same "measurement must see the final width" reasoning as enforceComplexityPadding just
        // above: enforceAttributeAndSpliceBracketPadding's loose `[: expr :]`/`[[ expr ]]` padding
        // can push a line right at the boundary past LINE_LENGTH_LIMIT with no further re-check if
        // it ran later (originally Phase 4, after enforceCallLineBreaking already measured/decided
        // not to wrap) -- a fresh format sees the pre-padding width and stays one line, while a
        // reformat of already-formatted output (padding already baked in) sees the post-padding
        // width and wraps, a genuine idempotency bug (found via real-world testing,
        // simdjson/experimental_json_builder's `from_json.hpp`
        // `[:simdjson::json_builder::expand(std::meta::nonstatic_data_members_of(^T)):] >>`
        // line). Pulled forward from Phase 4 to run right before enforceCallLineBreaking, same fix
        // shape as enforceComplexityPadding above; enforcePackIndexingSpacing/
        // enforceReflectionOperatorSpacing stay in Phase 4 since they only ever tighten (remove)
        // spacing, never grow a line's width, so they can't trigger this class of bug.
        if(lang.isCpp) text = cppRule.enforceAttributeAndSpliceBracketPadding(
            tokenizer.apply(text)
        );
        // Same "measurement must see the final width" reasoning as enforceComplexityPadding/
        // enforceAttributeAndSpliceBracketPadding above: enforceInitializerBraceSpacing's loose
        // `{ x }` initializer-brace padding (STYLE.md §3.3) can push a line right at the boundary
        // past LINE_LENGTH_LIMIT with no further re-check if it only ran later (originally Phase 4,
        // after enforceCallLineBreaking already measured/decided not to wrap) -- a fresh format
        // sees the pre-padding width and stays one line, while a reformat of already-formatted
        // output (padding already baked in) sees the post-padding width and wraps, a genuine
        // idempotency bug (found via real-world testing, openrewrite/rewrite's `J.java`
        // `@RequiredArgsConstructor(onConstructor_ = {@JsonCreator(...)})` annotation argument,
        // whose `{@JsonCreator(...)}` grows past the limit once padded to `{ @JsonCreator(...) }`).
        // Pulled forward from Phase 4 to run right before enforceCallLineBreaking, same fix shape
        // as enforceComplexityPadding/enforceAttributeAndSpliceBracketPadding above; the original
        // Phase 4 call further down is left in place too (idempotent, still needed for braces
        // introduced/exposed by later passes).
        text = miscRule.enforceInitializerBraceSpacing( tokenizer.apply(text) );
        text = miscRule.enforceKeywordSpacing( tokenizer.apply(text) );
        text = miscRule.enforceCallLineBreaking( tokenizer.apply(text) );
        if(lang.isJs || lang.isTs) {
            // RDD_KEY_246/RDD_KEY_248: ScopePipelineCurly.applyOversizedAggregateInitClosingBracePass's
            // dangling-`}` decision, and JsTsDeclarationAlignmentRule's grouping/padding decision,
            // are both made above (inside scopePipeline.process) BEFORE the call-wrap just above
            // has run -- so both can be computed against a stale, pre-wrap `{...}` shape on a fresh
            // format, only becoming stable once a prior round's own wrap-newline is already present
            // in the input (round1 != round2). Re-run just those two passes now, against the
            // post-wrap shape, closing-brace first so the declarations pass sees the corrected
            // shape too. Deliberately narrower than re-running the whole `process()` a second time
            // (that regressed unrelated already-correct constructs, e.g. a trailing-comment-after-
            // `}` interface body -- see RDD_KEY_246) and narrower than re-running just the
            // closing-brace pass alone (left the declarations pass working off stale data).
            text = scopePipeline.reapplyClosingBraceAndDeclarationsPass(text);
        } // if
        else if(lang.isJava) {
            // RDD_KEY_270 Java-family follow-up (see STATE_C_CPP_JAVA.md's Open Questions entry,
            // "4th idea"): an `applyAssignmentsPass` alignment group's trailing-comment-column
            // padding is computed above (inside scopePipeline.process) BEFORE the call-wrap just
            // above has run -- if one sibling's call gets wrapped by enforceCallLineBreaking, every
            // OTHER sibling's padding stays stale (computed against the pre-wrap width), and only
            // collapses once a later round's input already has the wrap baked in -- round1 !=
            // round2. Widening the JS/TS `reapplyClosingBraceAndDeclarationsPass` bundle above to
            // Java was tried and reverted twice (re-running `applyOversizedAggregateInitClosingBracePass`/
            // `applyDeclarationsPass` a second time re-collapsed already-correct Java aggregate-init
            // closing braces and enum-constant-list `;`-separation respectively). Re-run ONLY
            // `applyAssignmentsPass` instead -- it exclusively touches genuine assignment-statement
            // groups, so it cannot re-collapse either of those unrelated constructs.
            text = scopePipeline.reapplyAssignmentsPassOnly(text);
        } // if
        // EnforceCallLineBreaking can turn a one-liner function body into a multi-line one (an
        // overlong call inside it gets wrapped across lines) -- but enforceFunctionDefinitionAllman
        // BraceStyle already ran earlier, above, back when the body still looked like a one-liner,
        // and deliberately left its brace K&R per RDD_KEY_75 ("never Allman-convert a one-liner").
        // That decision is now stale: the body is no longer a one-liner, so it should have been
        // Allman-converted. Stable only on a second format pass, once the input already has the
        // call pre-wrapped, so the earlier call sees a genuinely-multi-line body from the start
        // (see STATE.md's `frozen`/`set.hpp` `operator==`/`operator<`/`operator>` bug). Re-running
        // it here (idempotent: a no-op on anything already Allman, still-one-line, or otherwise
        // untouched) re-derives the brace placement against the now-final line count.
        if(isCOrCpp) text = cppRule.enforceFunctionDefinitionAllmanBraceStyle(
            tokenizer.apply(text)
        );
        // EnforceCallLineBreaking can join a call whose args originally spanned multiple lines
        // (each side's own gap blocked the pass above from touching its spacing, since a NEWLINE
        // in the gap suppresses the rewrite for that side) onto a single line, replacing the
        // newline with a plain single space with no complexity-padding awareness of its own. On a
        // fresh format that plain-space join is exactly what "loose" padding looks like, even for
        // an argument list with no nested `(`/`[` that should render tight -- stable only on a
        // second pass, once there's no longer a NEWLINE in the gap to block the rewrite. Re-running
        // enforceComplexityPadding here (idempotent, purely paren-local) re-tightens/loosens any
        // call whose layout enforceCallLineBreaking just finalized.
        text = miscRule.enforceComplexityPadding( tokenizer.apply(text) );
        text = switchRule.formatNonInlineSwitches( tokenizer.apply(text) );
        text = miscRule.enforceCallLineBreaking( tokenizer.apply(text) );
        text = miscRule.enforceComplexityPadding( tokenizer.apply(text) );
        text = miscRule.insertBlankLineBeforeReturn( tokenizer.apply(text) );

        // Phase 2: comment-style normalization -- must precede Phase 3 (RDD_KEY_47)
        text = miscRule.enforceCommentStyle( tokenizer.apply(text), config.indentStyle() );
        text = miscRule.alignCommentSeparators( tokenizer.apply(text) );

        // Phase 3: comment/marker-generating passes.
        // `alignInlineSwitches`/`markFallthrough` run before `addClosingComments` so the SWITCH
        // closing-comment line-count decision (STYLE.md §7) sees the switch body's final,
        // fully-compacted line count -- not its pre-alignment shape -- keeping the decision
        // (and thus idempotency) stable across repeated format passes.
        text = switchRule.markFallthrough( tokenizer.apply(text) );
        text = switchRule.alignInlineSwitches( tokenizer.apply(text) );
        // Same reasoning as alignInlineSwitches/markFallthrough above, extended to Kotlin's own
        // `when` expression formatting: formatWhenExpressions can insert blank lines inside a
        // `when {}` body (STYLE_KOTLIN.md §4), expanding an enclosing `for`/`while` block's own
        // line count -- if it ran later (Phase 4, as it originally did), a fresh format sees the
        // pre-expansion line count here while a reformat of already-formatted output (which
        // already has those blank lines baked in) sees the post-expansion count, so a `for`/
        // `while` block straddling the closing-comment-min-lines threshold gets a `// for`/`//
        // while` comment on round2 but not round1 -- a genuine idempotency bug, not just
        // cosmetic (found via arrow-kt/arrow's `Iterable.kt` `separateEither`, RDD_KEY_174). Moved
        // ahead of addClosingComments so its line-count decisions always see the final line count.
        if(lang.isKotlin) text = kotlinRule.formatWhenExpressions( tokenizer.apply(text) );
        // addClosingComments (+ enforceSwitchExpressionArrowAlignment, which stays paired
        // immediately after it) is only run HERE when line-split-operator-priority is off. When
        // that flag is on, enforceOperatorLineBreaking (further below, gated the same way) can
        // still EXPAND a block's line count after this point -- splitting a long if/while/for
        // condition or return/assignment RHS across multiple lines -- so running
        // addClosingComments here would let its STYLE.md §7 line-count threshold decision see a
        // fresh format's pre-split (shorter) line count while a reformat of already-split output
        // sees the post-split (longer) count, a round1-vs-round2 flap identical in shape to
        // RDD_KEY_174 above. Fixed (RDD_KEY_343) by deferring both calls, flag-on only, to run
        // after enforceOperatorLineBreaking instead (see that later call site) -- keeping this
        // flag-off branch byte-for-byte identical to the pre-fix, single unconditional call so
        // every existing flag-off fixture/pipeline stays unaffected.
        if( !config.lineSplitOperatorPriority() ) {
            text = blockRule.addClosingComments( tokenizer.apply(text) );
            if(lang.isJava) text = javaRule.enforceSwitchExpressionArrowAlignment(
                tokenizer.apply(text)
            );
        }

        // Phase 4: cosmetic spacing
        text = miscRule.enforceKeywordSpacing( tokenizer.apply(text) );
        text = miscRule.enforceInitializerBraceSpacing( tokenizer.apply(text) );
        text = miscRule.enforcePreIncrement( tokenizer.apply(text) );
        if(lang.isCpp) {
            text = cppRule.enforceTemplateAngleBracketSpacing( tokenizer.apply(text) );
            text = cppRule.enforcePackIndexingSpacing( tokenizer.apply(text) );
            text = cppRule.enforceContractAssertSpacing( tokenizer.apply(text) );
            text = cppRule.enforceReflectionOperatorSpacing( tokenizer.apply(text) );
        }
        if( isCOrCpp && config.isFormatMacros() ) text = cppRule.alignMacroDefinitions(
            tokenizer.apply(text)
        );
        if(lang.isKotlin) {
            text = kotlinRule.enforceNullSafetyOperatorSpacing( tokenizer.apply(text) );
            text = kotlinRule.enforceRangeOperatorSpacing( tokenizer.apply(text) );
            text = kotlinRule.enforceLabeledJumpSpacing( tokenizer.apply(text) );
            text = kotlinRule.enforceArrowSpacing( tokenizer.apply(text) );
            text = kotlinRule.enforceAnnotationUseSiteTargetSpacing( tokenizer.apply(text) );
            text = kotlinRule.enforceWhereClausePlacement( tokenizer.apply(text) );
        } // if
        if(lang.isJs || lang.isTs) {
            // STYLE_JS_TS.md §3/§7: spread/rest `...` tight, `?.` tight, `??`/`??=` spaced --
            // same flat cosmetic-spacing treatment as Kotlin's analogous operators above.
            text = jsTsRule.enforceSpreadRestSpacing( tokenizer.apply(text) );
            text = jsTsRule.enforceOptionalChainingSpacing( tokenizer.apply(text) );
            text = jsTsRule.enforceTemplateLiteralInterpolationSpacing( tokenizer.apply(text) );
            text = jsTsRule.enforceArrowSpacing( tokenizer.apply(text) );
            text = jsTsRule.enforceDecoratorTightAtSpacing( tokenizer.apply(text) );
        } // if
        if(lang.isTs) {
            // STYLE_JS_TS.md §11: `: type` colon spacing (declarator/parameter/return-type),
            // TS-only -- JS has no type annotations.
            text = jsTsRule.enforceTypeColonSpacing( tokenizer.apply(text) );
            text = jsTsRule.enforceUnionIntersectionSpacing( tokenizer.apply(text) );
            text = jsTsRule.enforceUnionTypeContinuationIndent( tokenizer.apply(text) );
            text = jsTsRule.reorderClassFieldModifiers( tokenizer.apply(text) );
            text = jsTsRule.enforceClassFieldAlignmentGrid( tokenizer.apply(text) );
            text = jsTsRule.enforceGenericArgumentCommaSpacing( tokenizer.apply(text) );
        } // if
        // Must run after every earlier paren-tightening/spacing pass has settled a braceless
        // collapsed `if(...) body` line's final rendered width -- see the method's own javadoc
        // for why an earlier collapse-time attempt was stale. Shared across all languages: C/C++/
        // Java grew their own opt-in braceless chain-collapse alongside Kotlin's.
        text = blockRule.alignBracelessElseIfChain( tokenizer.apply(text) );
        if( config.lineSplitOperatorPriority() ) {
            text = miscRule.enforceOperatorLineBreaking( tokenizer.apply(text) );
            // enforceOperatorLineBreaking can turn a single-line function body's `return`
            // expression multi-line (e.g. `constexpr auto count() -> int { return B ? 1 : 0; }`'s
            // ternary) -- but enforceFunctionDefinitionAllmanBraceStyle already ran twice above
            // (Phase 1, and again after enforceCallLineBreaking) while the body still looked like
            // a one-liner, and deliberately left its `{` K&R per RDD_KEY_75 ("never Allman-convert
            // a one-liner"). That decision is now stale, same "widened by a later pass" shape as
            // the enforceCallLineBreaking-triggered re-run above (see that call site's own
            // comment) -- re-run it here, flag-on only, and ahead of addClosingComments below for
            // the same reason as that pass's own reordering (RDD_KEY_343): the `{`-onto-its-own-
            // line newline this can introduce must be visible to any enclosing block's own
            // closing-comment line-count threshold decision. C/C++ only -- the identical one-liner-
            // defer behavior in JavaSpecificRule/KotlinSpecificRule/JsTsSpecificRule's own Allman-
            // brace passes was never given this re-run treatment either (not yet confirmed to need
            // it by real-code dogfood), so left as-is rather than speculatively widened.
            if(isCOrCpp) text = cppRule.enforceFunctionDefinitionAllmanBraceStyle(
                tokenizer.apply(text)
            );
            // See the flag-off addClosingComments call site above (Phase 3) for why this pair is
            // deferred to here, flag-on only: enforceOperatorLineBreaking can expand a block's
            // line count, and addClosingComments' STYLE.md §7 threshold decision must see that
            // final, post-split line count to stay round1/round2 idempotent (RDD_KEY_343).
            text = blockRule.addClosingComments( tokenizer.apply(text) );
            if(lang.isJava) text = javaRule.enforceSwitchExpressionArrowAlignment(
                tokenizer.apply(text)
            );
        }
        text = miscRule.enforceCallLineBreaking( tokenizer.apply(text) );
        text = miscRule.enforceCallLineBreaking( tokenizer.apply(text) );
        text = miscRule.enforceComplexityPadding( tokenizer.apply(text) );
        text = switchRule.formatNonInlineSwitches( tokenizer.apply(text) );
        if(lang.isJsxSyntax) {
            // "JSX full embedding-aware dispatcher" recursive `{}`-hole-parsing job
            // (STATE_JS_TS.md) -- must run BEFORE enforceJsxSelfClosingAttributeWrap: every hole
            // it splices lives at an offset >= jsxOpeningTagEndOffset (children-position only),
            // so it never disturbs the opening-tag region the wrap pass below measures/rewrites,
            // and can safely run first.
            text = jsTsRule.spliceJsxExpressionHoles( tokenizer.apply(text) );
            // STATE_JS_TS.md's narrow JSX-scoped child parser (2026-08-20 approach) -- re-derives
            // each direct child tag/fragment boundary line's own indentation from unambiguous JSX
            // nesting depth. Must run after the hole splice above (hole interiors need to already
            // be in final form) and is safe on either side of the wrap pass below (that pass never
            // touches anything at/after jsxOpeningTagEndOffset).
            text = jsTsRule.reindentJsxChildren( tokenizer.apply(text) );
            // STATE_JS_TS.md's Step 2 "context 11" scoping session, Increment 2 -- self-closing-tag
            // attribute wrap only (tags with children remain out of scope, see that method's own
            // doc comment). Runs after every earlier width-affecting pass has settled, same
            // ordering rationale as enforceCallLineBreaking immediately above.
            text = jsTsRule.enforceJsxSelfClosingAttributeWrap( tokenizer.apply(text) );
        } // if

        // Phase 5: file-header-level structure
        if(isCOrCpp) {
            text = cppRule.enforceHeaderFileStructure(
                tokenizer.apply(text), filePath, config.isHeaderGuardRename()
            );
        }
        else if(lang.isJava) {
            text = javaRule.enforceImportOrdering(
                tokenizer.apply(text), config.javaImportOrder(),
                config.isJavaImportSort(), config.javaImportDepth(), config.javaImportBlankLines()
            );
        }
        else if(lang.isKotlin) {
            text = kotlinRule.enforceKotlinImportOrdering(
                tokenizer.apply(text), config.kotlinImportOrder(),
                config.isKotlinImportSort(), config.kotlinImportDepth(), config.kotlinImportBlankLines()
            );
        }
        else if(lang.isJs || lang.isTs) {
            text = jsTsRule.enforceImportOrdering(
                tokenizer.apply(text), config.jsImportOrder(),
                config.isJsImportSort(), config.jsImportBlankLines()
            );
        }

        // Phase 6: final whitespace normalization, last
        text = miscRule.convertIndentation( tokenizer.apply(text), config.indentStyle() );

        return text;
    }

} // class FormatterCurly
