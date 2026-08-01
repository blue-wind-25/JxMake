# STATE_KOTLIN.md — Kotlin JAR Implementation Tracker

Read `STATE_COMMON.md` first — it has the shared commit/ambiguity/testing
conventions this file assumes. `STATE_C_CPP_JAVA.md` (the other job's file)
is NOT required reading for this one — only `STATE_COMMON.md` is.
Dogfood corpus status: see `STATE_DOGFOOD.md`.

---

## Purpose

Tracks implementation of Kotlin support in the deterministic JAR formatter
(`util/CodingStyle.md/formatter/`), per `STYLE_KOTLIN.md` / `STYLE_KOTLIN2.md`.

**Overall status: implementation complete (Steps 0-4 all DONE, `make test`
green). Step 5 dogfood: core goal (dogfood tree compiles clean) met. Bucket
D3 (see "Dogfood: JetBrains/kotlin" below) — root cause confirmed, two fix
attempts tried and reverted — was, as of 2026-08-02, folded into the new
`STATE_CURLY_GDR.md` "General scope-depth reindentation" (GDR) job; it is
no longer tracked as an independently open item in this file. The Kotlin
job itself now has no independently-open item.**

---

## Hard Constraint — Shared Classes

Shared across all languages (C, C++, Java, Kotlin), not per-language files:

```
tokenizer/TokenizerCore.java (TokenizerCore + TokenizerCurly)
grid/ColumnGrid.java
grid/ModifierPriority.java
evaluator/ComplexityPaddingEvaluator.java
rules/DeclarationAlignmentRule.java (DeclarationAlignmentRuleCore + DeclarationAlignmentRuleCurly)
rules/BlockStructureRule.java
rules/SwitchRule.java
rules/GetterSetterRule.java (GetterSetterRuleCore + GetterSetterRuleCurly)
rules/MiscRule.java (MiscRuleCore + MiscRuleCurly)
ScopePipeline.java (ScopePipelineCore + ScopePipelineCurly)
Formatter.java (FormatterCore + FormatterCurly)
```

**Any change to one of these for Kotlin's benefit must not change behavior
for C/C++/Java.** Before/after every such change, re-run `make test` (all
C/C++/Java fixtures) and confirm zero regressions — shared-class edits are
this job's biggest risk. Record the before/after test count in the commit
message.

Kotlin-only work belongs in new files (see Project Layout below), alongside
`JavaSpecificRule.java`/`CppSpecificRule.java`, not folded into them.

**Before modifying a shared class, grep first — do not read
`STATE_C_CPP_JAVA.md` in full.** Run `grep -Fm1 'ClassName' RDD_LOG.md`
(substitute the class/method) to surface existing `RDD_KEY_n` decisions
already explaining its shape — e.g. `TokenizerCore`'s multi-char operator
table (RDD_KEY_69), or why a rule class re-derives named-construct-ness from
raw tokens instead of trusting one flag (RDD_KEY_84/85). Usually sufficient.
Only read `STATE_C_CPP_JAVA.md`'s Project Layout section (never Checklist or
full history) if the grep hits don't explain what you're looking at.

---

## Project Layout (new files only)

```
util/CodingStyle.md/formatter/
  src/
    com/jxmake/formatter/
      grid/
        KotlinModifierPriority.java
      rules/
        KotlinSpecificRule.java
  test/
    kt_combined_inp.kt / kt_combined_out.kt
    kt_comments_inp.kt / kt_comments_out.kt
```

Existing shared files listed under Hard Constraint above are modified
in-place, additively, when Kotlin needs a shared capability they don't yet
have (e.g. a new operator token) — they are not duplicated per-language.

New Kotlin-only rule classes that grew large enough to warrant their own
file (beyond `KotlinSpecificRule.java`): `KotlinDeclarationAlignmentRule`
(extends `DeclarationAlignmentRule`), `KotlinSignatureRule` (extends
`MiscRule`), `KotlinGetterSetterRule` (extends `GetterSetterRule`).

---

## Resolved Design Decisions

Full text of each decision lives in `RDD_LOG.md` (shared with
`STATE_C_CPP_JAVA.md` — continue its existing `RDD_KEY_n` numbering, do not
restart). See STATE_COMMON.md's lookup convention (`grep -Fm1`, no `-A`).

| Key | Topic |
|---|---|
| RDD_KEY_91 | `STATE_KOTLIN.md` — self-contained tracker, not linked from `STATE.md` yet |
| RDD_KEY_92 | Shared-tokenizer approach — extend `TokenizerCore.java` in place, no separate Kotlin tokenizer |
| RDD_KEY_93 | Checklist ordering — tokenizer support first, then scoping pass, before `KotlinSpecificRule.java` |
| RDD_KEY_99 | §3.1/§3.4 headless named-construct classification (`companion object {}`, anon `object [:Super] {}`, `init {}`) — `BlockStructureRule.classifyKotlinHeadlessNamed`, + tokenizer `:` supertype-name bug |
| RDD_KEY_100 | §3.2 `when` no-space-before-`(` — added to `MiscRule.TIGHT_PAREN_KEYWORDS` |
| RDD_KEY_101 | §4 `when` expression arrow alignment/closing comment/blank lines — new `KotlinSpecificRule.formatWhenExpressions` |
| RDD_KEY_102 | §5 null-safety operator spacing (`?.`/`!!` tight, `?:` spaced) — `KotlinSpecificRule.enforceNullSafetyOperatorSpacing` |
| RDD_KEY_103 | §6 var/property declaration alignment — new `KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule`, name-before-type grammar |
| RDD_KEY_104 | §7/§7.1 constructor/function param line-breaking + alignment — new `KotlinSignatureRule extends MiscRule`, own param/signature model |
| RDD_KEY_105 | §11 labeled jump/label spacing (`return@label`, `label@`) — `KotlinSpecificRule.enforceLabeledJumpSpacing` state machine |
| RDD_KEY_106 | §14 generic `where` clause line-breaking — `KotlinSpecificRule.enforceWhereClausePlacement`, mirrors C++ `requires` handling |
| RDD_KEY_107 | §12 destructuring declarations — `KotlinDeclarationAlignmentRule` group/parse/render helpers; **REVISED under RDD_KEY_126** |
| RDD_KEY_126 | **REVISES RDD_KEY_107** — merges §12 destructuring into the same column-aligned group stream as an adjacent §6 declaration (C++ structured-bindings precedent) |
| RDD_KEY_127 | Bare Kotlin `else\n stmt` collapse gap (distinct from RDD_KEY_124's `if`/`while`/`for` case) — `collapseBracelessBody` helper; column-padding half **resolved under RDD_KEY_128** |
| RDD_KEY_128 | **RESOLVES RDD_KEY_127** — collapsed single-line `else` body column-pads to match its `if(...)` sibling; new `KotlinSpecificRule.alignBracelessElseWithIf`, last-running line-based pass |
| RDD_KEY_131 | `kt_comments_inp/out.kt` — fixed 4 remaining bugs (comment-led `when`-branch/`return` blank lines, leading-blank stripping, outer-class closing comment suppressed by nested frozen region), fixture enabled |
| RDD_KEY_108 | §16 annotation use-site target `:` spacing — `KotlinSpecificRule.enforceAnnotationUseSiteTargetSpacing` |
| RDD_KEY_109 | §17/§17.1 lambda-with-receiver nesting exemption + arrow spacing — `ComplexityPaddingEvaluator.isLoose` extended (shared, no-op C/C++/Java) + `KotlinSpecificRule.enforceArrowSpacing` |
| RDD_KEY_110 | §10 `for` loops/ranges — `in`/`until`/`downTo`/`step` already inert; new `KotlinSpecificRule.enforceRangeOperatorSpacing` for `..`/`..<` |
| RDD_KEY_111 | §2 `enum class` blank-line emphasis around mandatory `;` — `KotlinSpecificRule.separateEnumConstantListTerminator` |
| RDD_KEY_112 | §9 expression-bodied functions — `KotlinSignatureRule.FunctionTail`/`parseFunctionTail`/`renderWithTail`; shared fix: `MiscRule.isTightToken` `*`/`&` gated off for Kotlin |
| RDD_KEY_113 | §13 generic variance (`in`/`out`) — `TokenizerCore.GENERIC_SAFE_KEYWORDS` extended, tokenizer-level, no-op C/C++/Java |
| RDD_KEY_114 | §3/§3.3 function/secondary-constructor Allman-brace conversion — `KotlinSpecificRule.enforceFunctionDefinitionAllmanBraceStyle`, conservative `fun`/`constructor`-anchored scan |
| RDD_KEY_115 | §1 semicolon stripping — rewrote `stripOptionalSemicolons` around positive-evidence `isTrailingSemicolon` (old version silently merged same-line multi-statement `;`) |
| RDD_KEY_116 | §19 string template tokenizer risk — `TokenizerCore.emitString` misread nested string inside `${...}`; Kotlin-only `skipKotlinString`/interpolation-aware scan |
| RDD_KEY_117 | Row 19.1 triple-quoted raw string tokenizer support — badly broken before; Kotlin-only `emitKotlinRawString`/`skipKotlinRawString` |
| RDD_KEY_132 | §8/§9 one-liner getter/setter grouping — new `KotlinGetterSetterRule extends GetterSetterRule`; scoped to expression-bodied one-liners (§9), `get()`/`set()` (§8) left to RDD_KEY_133 |
| RDD_KEY_133 | §8 property-accessor one-liner grouping (gap RDD_KEY_132 left open) — `parseKotlinAccessorMember`/`renderAccessorGroup`; 2 idempotency bugs fixed (`needsSpaceBetween` `get`/`set` tightness, `parseKotlinDeclaration` re-parse bail) |
| RDD_KEY_134 | dogfood compile-break: `MiscRule.renderCallCandidate` collapsed a multi-line trailing-lambda call arg onto one line, no separators — Kotlin-gated `containsBrace` bail (gate required, non-gated broke C++ fixture). Fixture `_17` |
| RDD_KEY_135 | dogfood idempotency (`PlayMusicBlock.kt` §6): `spansMultipleLines` misclassified a call-wrapped single-statement initializer as multi-line block — depth-aware newline classification fix. Fixture `_18` |
| RDD_KEY_136 | dogfood (`MainActivity.kt`): trailing-lambda `{` on a fluent-chain continuation line got wrong closing-brace indent — new `ScopePipeline.braceLineIndent`/`effectiveSpanIndent`, Kotlin-gated. Fixture `_19` |
| RDD_KEY_137 | dogfood (`MainViewModel.kt`/`ToolbarActions.kt`): parenthesized if/else-expression initializer fused onto next statement with no separator — `collapseSingleExpressionBlocks` gains unmatched-paren-depth guard. Fixture `_20` |
| RDD_KEY_138 | dogfood (`BlockCanvasView.kt`): missing space before `&&` — `DeclarationAlignmentRule.isTightToken`'s `&`-repeat check was ungated by language (the one copy missing the Kotlin gate `MiscRule.isTightToken` already had). Fixture `_21` |
| RDD_KEY_139 | dogfood (`BlockPalette.kt` §9 column-width flap): `parseKotlinOneLinerMember` missing the length pre-check `GetterSetterRule.parseOneLinerMember` has. Fixture `_22` |
| RDD_KEY_140 | dogfood (`BlockPalette.kt` Allman idempotency): `KotlinSpecificRule.isSingleLineBody` missing the width-prediction pre-check `JavaSpecificRule` has; ported + corrected width formula (indent + per-token space). Fixture `_23` |
| RDD_KEY_141 | `./gradlew compileDebugKotlin` ~50 errors: Kotlin `: ReturnType` tail detection matched any top-level `)` with no check a real `:` followed, deleting `.IDENTIFIER2` off a fluent-chain call. Fix requires a top-level `:` before accepting the tail. Fixture `_24` |
| RDD_KEY_142 | `BlockCanvasView.kt`/`ToolbarActions.kt` compile corruption: `label@` state machine misfired on `@Annotation` after a class name/trailing identifier — new `isLoopLabelTarget` lookahead. Fixture `_25` |
| RDD_KEY_143 | `Optimizer.kt` compile error: bare-`else` braceless-collapse matched a `when` arm's `else ->` label — new `isWhenArrow` bail. Fixture `_26` |
| RDD_KEY_144 | `ProgramBuilder.kt` — 2 unrelated bugs: (A) `!is`/`!in` negated operators corrupted to `! is`/`! in` by generic KEYWORD spacing, Kotlin-gated fix; (B) `parseSignature`'s C-style parser misapplied to Kotlin call args (`it.func.funcName` misparsed), split into a separate `sigForRender`. Fixture `_27` |
| RDD_KEY_118 | §24 import-ordering implementation — `KotlinSpecificRule.enforceKotlinImportOrdering`, no `static` bucket, new `kotlin-import-*` config keys; verified standalone, not yet wired into `Formatter.formatOne` |
| RDD_KEY_145 | `square/okio` testing: initial diff was a test-methodology mistake (default `indent-size=4` vs. okio's own `2`), not a bug; no code change |
| RDD_KEY_146 | `square/okio` bug #1: unary minus/plus mis-spacing in declaration initializers — new `renderTokens` override + `isUnaryMinusOperand` lookback |
| RDD_KEY_147 | `square/okio` bug #2: `: ReturnType` tail detection merged an unrelated `val ... by lazy {}` across a blank line — new `hasTopLevelBlankLine` guard |
| RDD_KEY_148 | `square/okio` bug #3: braceless `if {...}` round1-vs-round2 wrap flap from an untightened `keyword (` prefix width mismatch — new `tightenParenPrefix` helper. Combined fixture `_30` (with 146/147) |
| RDD_KEY_149 | `square/okio` bug #4 — **found, not fixed, deferred** (later **resolved by RDD_KEY_163**): multi-line signature's column padding/trailing comma silently stripped downstream. |
| RDD_KEY_150 | `square/okio` compile bug #1: `TokenizerCore.MULTI_CHAR_OPS` missing `===`/`!==`, mis-tokenized and re-spaced invalid |
| RDD_KEY_151 | `square/okio` compile bug #2: do-while's trailing `while (cond)` misread as loop-start, fusing the next statement — new `isDoWhileTailKeyword` lookback. Combined fixture `_31` (with 150) |
| RDD_KEY_152 | `square/kotlinpoet` idempotency: nested `when {}` closing-brace indent flap from a pre-merge brace-line anchor — new `findMergingWhenBranchLineStart` lookahead. Fixture `_32` |
| RDD_KEY_153 | `square/kotlinpoet` compile bug: expression-bodied function's own trailing-lambda body wrongly Allman-converted as the function's body brace — new depth-0-`=` bail-out. Fixture `_33` |
| RDD_KEY_154 | `kotlinx.coroutines` idempotency: `renderWithTail` baked a trailing space onto bare `"="` for an empty-tail lambda body, unbounded growth each pass. Fixture `_37` |
| RDD_KEY_155 | `kotlinx.coroutines` compile bug #1: nested `/* */` inside a KDoc comment closed the outer doc-comment early (Kotlin nests block comments) — Kotlin-gated nesting-depth tracking. Fixture `_38` |
| RDD_KEY_156 | `kotlinx.coroutines` compile bug #2: `this@Label` got a spurious space before `@` — new `AFTER_THIS_KEYWORD`/`AFTER_THIS_AT` states. Fixture `_39` |
| RDD_KEY_157 | `kotlinx.coroutines` compile bug #3 (`LimitedDispatcher.kt`): a `synchronized {}` block nested inside a collapsed single-statement body had its own statements fused with no separators — new `containsMultilineNestedBrace` bail. Fixture `_40` |
| RDD_KEY_158 | `kotlinx.coroutines` idempotency (`SystemProps.kt`): signature-tail merge re-derived the `try` span's indent but left a chained `catch`'s stale pre-merge indent — span loop now inherits the preceding span's resolved indent for a chained `catch`/`finally`. Fixture `_41` |
| RDD_KEY_159 | `kotlinx.coroutines` idempotency category 1 (4 files, brace/comment indent drift): `effectiveSpanIndent` preferred `braceIndent` over `spanIndent` even for a NAMED construct with a wrapped header — now forces `spanIndent` when `isNamedScope`. Fixture `_42` |
| RDD_KEY_160 | `kotlinx.coroutines` idempotency (`JobSupport.kt`): call-argument continuation indent read a stale pre-merge indent for a `when`-branch call body — new `MiscRule.effectiveCallBaseIndent`. Fixture `_43` |
| RDD_KEY_161 | `kotlinx.coroutines` idempotency (`BufferedChannel.kt`): dangling braceless `else expr` after span extraction mis-anchored `findParentIndent`'s backward scan — skip forward past the dangling keyword's body first. Fixture `_44` |
| RDD_KEY_163 | **RESOLVES `kotlinpoet` Shape 1 (6 files) + RDD_KEY_149's deferred `okio` bug** — two `enforceCallLineBreaking` bugs: undercounted nested-call width on reformat (new `effectiveLineEndIndex`); re-wrapping an already-correct `: ReturnType {` signature as a plain call (new `isKotlinReturnTypeThenBlockBody`). Fixture `_46` |
| RDD_KEY_164 | **RESOLVES `kotlinpoet` Shape 2** (`AbstractTypesTest.kt`): `enforceWhereClausePlacement` derived base indent from its own already-wrapped line, compounding indent each round — new `signatureLineIndent` true-statement-start scan. Fixture `_47` |
| RDD_KEY_165 | **RESOLVES `kotlinpoet` Shape 3** (2 files): `findMergingWhenBranchLineStart` only recognized bare `when {`, not a subject-form `when(x) {` or a plain trailing-lambda call — lookahead generalized. Fixture `_48` |
| RDD_KEY_166 | **RESOLVES `kotlinpoet` Shape 4 — all 4 shapes resolved, full tree idempotent** (`KotlinPoetMetadataSpecsTest.kt`): `Foo::class` class-literal wrongly armed `namedConstructKeywordSeen`, corrupting a later scope's name — new `isPrecededByDoubleColon()` guard; also fixed 2 masked bugs found via a from-clean rebuild in `signatureLineIndent` (a `-1` crash, a Shape-2-fix regression). Fixture `_49` |
| RDD_KEY_162 | **RESOLVES RDD_KEY_161's last open question**, `ChannelFlow.kt` declaration-alignment padding flap (final file of the `kotlinx.coroutines` investigation) — early alignment phase widened a sibling line enough to trigger a later wrap, which then un-widened it next pass; fix plumbs indent-width/line-length awareness into `renderAlignedGroup` with a brace-bodied-initializer-only exclusion. Fixture `_45`. Closes the entire investigation. |
| RDD_KEY_174 | `arrow-kt/arrow` dogfood, idempotency bug 1 (`RaiseContext.kt`'s `ensureNotNull`, deferred by RDD_KEY_173): `KotlinSignatureRule.parseKotlinSignature`'s first-`IDENTIFIER (` scan mistook a leading `context(raise: Raise<Error>)` clause's own paren for the real signature's param list when both shared one physical line — fixed by skipping a non-matching candidate paren and continuing the scan. Combined fixture `_62` (with RDD_KEY_175). |
| RDD_KEY_175 | `arrow-kt/arrow` dogfood, idempotency bug 2 (`Iterable.kt`'s `separateEither`, deferred by RDD_KEY_173): `Formatter.java` ran `formatWhenExpressions` (can insert `when{}` blank lines) after `addClosingComments` had already counted the enclosing `for` loop's line total against `closing-comment-min-lines` — fixed by moving `formatWhenExpressions` ahead of `addClosingComments`, same precedent as `alignInlineSwitches`/`markFallthrough`. Combined fixture `_62` (with RDD_KEY_174). |
| RDD_KEY_176 | `arrow-kt/arrow` dogfood, bug 4 (`Either.kt`'s `zipOrAccumulate`, deferred by RDD_KEY_173): `BlockStructureRule.collapseBracelessBody` (bare-`else`/braceless-`if` body collapse) never checked whether its found body was actually a single statement once that body could itself own a multi-line `{...}` block (a trailing-lambda call like `buildList(10) { ... }`) — `renderInline` fused the block's internal statements with no `;` separator, a genuine compile error. Fixed by reusing `containsMultilineNestedBrace` as a bail-out guard, mirroring `tryCollapse`'s existing protection. Fixture `_63`. |
| RDD_KEY_207 | `JetBrains/kotlin` dogfood cluster C6b (12 files) — multi-dollar string prefix (`$$`/`$$$`) spurious space fix, `isDollarRun` carve-out in `MiscRuleCore`/`DeclarationAlignmentRuleCore` |
| RDD_KEY_208 | `JetBrains/kotlin` dogfood cluster C6e (6 files) — trailing-lambda multi-statement body fused as boolean sub-expression inside `if(...)`/`&&`; `containsMultilineNestedBrace` condition guard added to `BlockStructureRule.tryCollapse`/`tryCollapseBraceless` |
| RDD_KEY_177 | **RESOLVES the last open arrow-kt/arrow item, closes the investigation** — `Comparison.kt`'s `sort2` idempotency flap: `collapseSingleExpressionBlocks`'s `isKotlinExpressionIf` exemption only covered a parenthesized (`kotlinParenDepth > 0`) expression-position `if`, not a depth-0 if-expression used as an entire expression-bodied function's whole body — new `isFunctionExprBodyEquals` backward-scan exempts the `if` when preceded by a function's (not `val`/`var`'s) `=` tail; a second, structurally separate bug in the paired bare `else` branch (no condition of its own to run the same check against) needed its own fix, a `pendingKotlinExprBodyElse` flag carried from the exempted `if` to its paired `else`. Fixture `_64`. |
| RDD_KEY_209 | `JetBrains/kotlin` dogfood C6k Shape C6k-3 (`ConeTypeRenderer.kt`) — **FIXED**: `!is`/`!in` corrupted to `! is`/`! in` inside a parameter's default value; RDD_KEY_144(A)'s carve-out existed only in `DeclarationAlignmentRuleCore.needsSpaceBetween`, missing from its duplicate `MiscRuleCore.needsSpaceBetween` (the join point a default value actually renders through). Fixture `_160`. |
| RDD_KEY_210 | `JetBrains/kotlin` dogfood C6k Shape C6k-4 (`ClientUtils.kt`) — **FIXED**: `KotlinSpecificRule.enforceNullSafetyOperatorSpacing` treated `!!` as unconditionally tight on both sides; a following keyword (`port!! in range`) needs its source-preserved space, not tightening — new `isPostfixNullOpContinuation` narrows `!!`'s right-side tightness to `.`/`[`/`(`/`?.`/`!!` only. Also incidentally fixes `x!! ?: y`. Fixture `_161`. |
| RDD_KEY_211 | `JetBrains/kotlin` dogfood C6k Shape C6k-5 (`CompositionTests.kt`) — **FIXED**: `isAnnotationFunctionTypeParen`'s lookahead past the matching close-paren only recognized `->` as confirming the paren belongs to an `@Annotation`-preceding function type; a *nullable* function-type default (`@Composable( () -> Unit )?`) has its outer close-paren followed by `?` instead (the `->` is inside, before the close), so the space-before-paren fix never fired. Widened lookahead to `isOp(t, "->") \|\| isOp(t, "?")` in both `MiscRuleCore`/`DeclarationAlignmentRuleCore` copies (Kotlin's `?` tokenizes as `OP`, not `PUNCT`). Fixture `_162`. |
| RDD_KEY_212 | `JetBrains/kotlin` dogfood C6k Shape C6k-1 (`Number2String.kt`, `TypeExpansionUtils.kt`, `TypeCommonizerTest.kt`) — **FIXED**: two independent root causes. (a) `BlockStructureRule.isSingleStatementBody` only routed Kotlin through the newline-aware `isKotlinSingleStatementBody` helper when `semiCount != 1` — Kotlin's optional-semicolon grammar means one `;` doesn't imply single-statement, so a body with several newline-separated statements plus one trailing `;`-terminated statement got wrongly collapsed. Fixed by always routing Kotlin through `isKotlinSingleStatementBody`, widened to also treat a depth-0 `;` as a statement boundary. (b) `KotlinSignatureRule.parseKotlinSignature` stripped NEWLINE tokens via `significantWithComments` before detecting a parameter's multi-line multi-statement lambda default value, letting `renderWithTail`'s inline-fits shortcut fuse the lambda body onto one line. Fixed by a new `containsMultilineNestedBrace(sigTokens)` bail at the top of `parseKotlinSignature`, operating on raw pre-strip tokens (mirrors `BlockStructureRule`'s identically-named helper). Fixture `_163`. |
| RDD_KEY_213 | `JetBrains/kotlin` dogfood C6k Shape C6k-2 (`KtVisitorTest.kt`, `BenchmarksReport.kt`) — **FIXED**: a raw string ending in its own literal `"` right before the closing `"""` produces a 4+-quote trailing run in the source; `skipKotlinRawString` (RDD_KEY_117) closed at the *first* `"""` of that run instead of the last three, leaving a stray `"` token that a later spacing pass then space-separated from the following call, corrupting `""""` into `""" "`. Confirmed via `kotlin_syntax_check` that real `kotlinc` accepts `"""abc"""".trimMargin()`/5-quote variants cleanly, proving the closing delimiter is always the run's last three quotes. Fixed by scanning through the whole quote run before closing. Fixture `_164`. |
| RDD_KEY_214 | `JetBrains/kotlin` dogfood Category 2 cluster **D2a** (chained-fluent-call closing-brace drift, 328 of 334 known idempotency-flap files) — **FIXED**: `ScopePipelineCurly`'s existing `isChainedCatchFinally` carve-out (RDD_KEY_158, inherit the preceding span's own already-resolved `effectiveSpanIndent` instead of re-deriving from volatile physical text) generalized from `catch`/`finally` keywords to any Kotlin `.`/`?.` fluent-chain continuation directly following a preceding span's own `}` (`}.apply {`, `}.also {`, etc) — new `isChainedFluentCall`. Fixture `_165`. |
| RDD_KEY_215 | `JetBrains/kotlin` dogfood D2a residual, where-clause-shaped share (`TestStepBuilder.kt`/`common.kt`/`KaBaseSymbolRelationProvider.kt`/`TopLevelPhases.kt`) — **FIXED**: two causes. (a) `ScopePipelineCurly`'s `isNamedScope` carve-out (RDD_KEY_159) never covers `fun` — added `headerHasTopLevelWhereClause` + `hasWhereClauseHeader` condition to force `spanIndent` for a `fun` with its own wrapped `where` clause too. (b) `KotlinSpecificRule.signatureLineIndent` (RDD_KEY_164) anchored a where-clause's base indent on the volatile physical line of the immediately preceding boundary token — non-idempotent when that boundary is itself the tail of an ANCESTOR's own wrapped `where` clause; fixed by deriving base indent from the true statement's own stable header-line indent minus one `indentUnit`. Fixture `_166`. |
| RDD_KEY_216 | `JetBrains/kotlin` dogfood D2a residual, `TopLevelPhases.kt`'s remaining line — **FIXED**: `isChainedFluentCall` (RDD_KEY_214) only recognized `.`/`?.` continuation after the preceding span's own `}`, not a boolean-infix-operator-joined chain (`} \|\| declarations.any { ... }`) — widened with new `isChainedBooleanOp` (`\|\|`/`&&`). Fixture `_167`. Closes 5 of RDD_KEY_214's 6-file D2a residual (all but `GenerateReleaseNotes.kt`/`TypeBridging.kt`, which direct diffing shows are ordinary D3 wrap-decision-flap instances, not a distinct D2a shape — misclassified by RDD_KEY_214's own text, left open under the separate D3 bucket). |
| RDD_KEY_218 | `JetBrains/kotlin` dogfood cluster **D4** (minor adjacent-closing-brace spacing flap, sample hit `JsArgumentsImpl.kt`) — **FIXED, closes D4**: `BlockStructureRule.collapseBracelessBody` (the already-braceless multi-line-body collapse path exercised only on a reformat, once a prior pass had already stripped an enclosing `if`'s own braces) correctly excluded its enclosing scope's own terminating `}` from the rendered body but left the WHITESPACE token immediately preceding it inside `contents`, which `renderInline` then silently dropped (never emits trailing whitespace) — losing a source-preserved single space (`) }` → `)}`) on the second format pass; `tryCollapse`'s sibling braced-body path never has this loss since it lets the surrounding loop re-append a *different* piece of untouched whitespace verbatim instead of folding it into a render. Fixed by appending a trailing space when the token right before the enclosing `}` was WHITESPACE/NEWLINE. Also required correcting `real_code_regressions_33_out.kt` (a pre-existing fixture that had, by coincidence, already baked this same bug's buggy `)} as T` into its own recorded "expected" output — corrected to `) } as T`). Fixture `_168`. `make test` 216/216 forward + idempotency, zero regressions. |
| RDD_KEY_219 | `JetBrains/kotlin` dogfood cluster **D1** (declaration/accessor column-alignment padding flap) — **PARTIALLY FIXED**: two independent group-width-recompute-instability root causes (RDD_KEY_139/140/162 family). (1) `KotlinDeclarationAlignmentRule.renderAlignedGroup` rendered surviving (non-excluded) rows as one flat shared-width grid even when an excluded, overflowing, brace-bodied-init row sat in the MIDDLE of the group — fixed by rendering surviving rows as maximal contiguous runs instead. (2) The analogous bug in shared `ScopePipelineCurly.applyGetterSetterPass` (Kotlin's one-liner expression-bodied function/accessor grouping) — fixed with new Kotlin-gated `renderKotlinFilteredRuns` helper, C/C++/Java untouched. **Known remaining gap, left open**: a third sub-shape where the offending member's own solo/raw width fits under the limit (so the raw-length parse-time pre-check never excludes it) but the group's own shared-column padding alone pushes it over — `KotlinGetterSetterRule` has no RDD_KEY_162-style budget-aware exclusion mechanism at all. D1's ~100-file estimate likely contains an unknown mix of all three sub-shapes; closure is partial. Fixture `_169`. `make test`: 217/217 forward + idempotency, zero regressions. |
| RDD_KEY_220 | `JetBrains/kotlin` dogfood cluster **D1**, third sub-shape left open by RDD_KEY_219 — **FIXED, closes D1 fully**: group-padding-induced overflow in `KotlinGetterSetterRule`'s one-liner grouping (member's own raw width fits under `lineLengthLimit`, but shared-column padding alone pushes it over; pre-fix this was silently absorbed by a later `MiscRule.enforceCallLineBreaking` call-argument wrap). Fixed by porting RDD_KEY_162's fixed-point budget-exclusion loop into a new depth-aware 3-arg `GetterSetterRuleCurly.render`/`KotlinGetterSetterRule.render` override, gated on `GetterSetterRuleCore.hasBreakableCall`, reusing RDD_KEY_219's contiguous-run rendering for survivors; `depth` threaded through `ScopePipelineCurly.applyGetterSetterPass`/`renderKotlinFilteredRuns`. Fixture `_170`. `make test`: 218/218 → 219/219 forward + idempotency, zero regressions. |
| RDD_KEY_221 | `JetBrains/kotlin` dogfood cluster **D3** — root cause confirmed, fix **NOT landed**: `MiscRuleCurly.renderCallCandidate`'s no-newline-branch fits-check measures a candidate against its entire enclosing physical source line (`lineStartIndex(tokens, nameIdx)`) instead of a stable position tied to the candidate itself, causing the wrap decision to flap across rounds as the enclosing line's own length changes. Candidate fix (anchor measurement at `nameIdx`) regressed 28 fixtures across C/C++/Java/TS/Kotlin at `make test` — reverted, not committed. Documented as a `README.md` Known Limitations bullet; D3 remains open. |
| RDD_KEY_226 | `JetBrains/kotlin` dogfood cluster **D3**, 2026-07-31 design session's `statementStartIndex` (depth-0 `;`/`{`/`}` backward-scan) fix **implemented, validated, and REVERTED** — the design's own documented "Known open risk" (Kotlin statements are usually NEWLINE-, not `;`-separated) materialized: 16 Kotlin fixtures regressed (e.g. `real_code_regressions_20_inp.kt`'s `val display = ...` followed by `showMessage(context, display)` — the backward scan walks past the current statement into the entire preceding sibling statement, inflating the measured width, false-positive wrap), zero regressions in C/C++/Java/TS (gate itself worked). Reverted in full; `make test` back to 225/225. D3 remains open; needs real statement-boundary tracking (depth-0 NEWLINE as a statement end vs. mid-wrap), closer to the "General scope-depth reindentation" architectural TODO than a self-contained fix. |

---

## Checklist

### Step 0 — Tokenizer Support (shared file, additive only) — DONE

`TokenizerCore.java` is shared with C/C++/Java; every addition below was
additive, verified against the full C/C++/Java suite before/after each
change (32/32, zero regressions throughout).

- [x] `MULTI_CHAR_OPS`: `?.`, `?:`, `!!`, `..<`, `..` added (longest-prefix
      rule). Bug found/fixed: `emitNumber()` consumed every `.`
      unconditionally, misreading `1..10` as one bogus `NUMBER` — fixed by
      stopping consumption when a `.` is followed by another `.`.
- [x] `KEYWORDS_KOTLIN` keyword set added (hard + modifier/soft keywords).
- [x] `NAMED_CONSTRUCT_KOTLIN` = `class`, `object`, `interface`, `enum`,
      `init` (special-casing deferred to Step 1).
- [x] §13: `GENERIC_SAFE_KEYWORDS` extended with `in`/`out` so
      `reclassifyAngleBrackets` treats declaration-site variance
      (`Box<out T>`) as a generic pair, not a comparison. RDD_KEY_113.
- [x] §19: Kotlin-only interpolation-aware string scan
      (`skipKotlinString`/`skipKotlinInterpolationBlock`/`skipKotlinChar`)
      inside `emitString()` — the shared naive scan misread a nested string
      inside `${...}` as three tokens instead of one. RDD_KEY_116.
- [x] §19.1: Kotlin-only raw-string support (`isKotlinRawStringOpener`/
      `emitKotlinRawString`/`skipKotlinRawString`) for `"""..."""`, checked
      ahead of the plain-`"` and C/C++ raw-string branches. No
      backslash-escape processing by design; terminates greedily at first
      `"""` (later found insufficient for a 4+-quote trailing run — see
      RDD_KEY_213). RDD_KEY_117.

### Step 1 — Scoping Pass (mirrors `JavaSpecificRule.java`, RDD_KEY_59) — DONE

Every section of `STYLE_KOTLIN.md`/`STYLE_KOTLIN2.md` was cross-checked
against the already-complete shared rule classes and classified: (a)
already satisfied as-is, (b) small additive shared-class extension, or (c)
new method in `KotlinSpecificRule.java`. `DeclarationAlignmentRule`'s
`Declaration` model assumes C/Java's `[modifiers] Type name [= init]`
order, structurally reversed from Kotlin's `[modifiers] val/var name :
Type [= init]` — resolved by writing `KotlinDeclarationAlignmentRule` as
its own model rather than forcing the shared one (RDD_KEY_103).

**Scoping table** (section numbers match `STYLE_KOTLIN.md`; `K2.N` = `STYLE_KOTLIN2.md` §N). All DONE/verified; RDD_KEY column is authoritative detail.

| § | Topic | Outcome | RDD_KEY |
|---|---|---|---|
| 1 | Semicolons (strip optional `;`) | (c) done, Kotlin-only `stripOptionalSemicolons` | 115 |
| 2 | `enum class` with members | (a)/(c) done — closing comment free from `classifyNamed`; blank-line emphasis around terminating `;` new | 111 |
| 3 | Brace style (Allman fn bodies / K&R else) | (a) K&R verified; (c) Allman-conversion done, conservative `fun`/`constructor`-anchored scan | 114 |
| 3.1 | Class/Object/Companion Object bodies | (b) done — headless gap (`companion object {}`, `object : I {}`, `init {}`) + a `:` supertype tokenizer bug | 99 |
| 3.2 | `catch`/`for`/`while`/`when` no space before `(` | (b) done — `"when"` added to `TIGHT_PAREN_KEYWORDS` | 100 |
| 3.3 | Secondary constructors (Allman body) | (c) done — same method as §3, keyed off `constructor` | 114 |
| 3.4 | `init` blocks | (b) done — same headless fix as §3.1 | 99 |
| 4 | `when` expression (arrow alignment, closing comment, blank lines) | (c) done, new `KotlinSpecificRule.formatWhenExpressions` (idempotency fix RDD_KEY_121) | 101 |
| 5 | Null-safety operators (`?.`/`!!` tight, `?:` spaced) | (c) done, flat whole-file pass | 102 |
| 6 | Variable/property declaration alignment | (c) done, `KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule` (loosen-then-extend) | 103 |
| 7 | Constructor/function parameter lists | (c) done, `KotlinSignatureRule extends MiscRule`, same pattern as §6 | 104 |
| 7.1 | Named/default arguments (`=` spacing/alignment) | (c) done for declarations only; call-site named args (`foo(x = 1)`) out of scope, different token shape | 104 |
| 7.2 | Trailing comma (preserved as-is) | (a) verified — no pass adds/strips one | — |
| 8 | Property accessors (`get`/`set`) | (a)/(c) — preserve-as-is default; plain expression-bodied-getter grouping done (RDD_KEY_133); block-bodied/setter/initializer shapes remain preserve-as-written-only (open gap) | 132/133 |
| 9 | Expression-bodied functions | (a)/(c) done incl. grouping — `FunctionTail`/`parseFunctionTail`/`renderWithTail`; shared fix `MiscRule.isTightToken` gated off for Kotlin `*`/`&` | 112, grouping 132 |
| 10 | `for` loops and ranges | (a)/(c) done — `in`/`until`/`downTo`/`step` already inert; `..`/`..<` tight spacing new | 110 |
| 11 | Labeled jumps (`@label` spacing) | (c) done, state machine distinguishing jump-`@label`/decl-`label@`/`@Annotation` | 105 |
| 12 | Destructuring declarations | (c) done in `KotlinDeclarationAlignmentRule`, merged into §6's group stream | 107, revised 126 |
| 13 | Generics variance (`in`/`out`) | (b) done, tokenizer-level only | 113 |
| 14 | Generic `where` clause | (c) done, mirrors C++'s trailing-`requires` handling | 106 |
| 15 | Infix functions (call-site spacing) | (a) verified, ordinary expression spacing | — |
| 16 | Annotation use-site targets (`@field:` tight `:`) | (c) done | 108 |
| 17 | Lambda-with-receiver / function types (nesting exemption) | (b) done, `ComplexityPaddingEvaluator.isLoose` extended, no-op C/C++/Java | 109 |
| 17.1 | Lambda parameter arrow spacing | (c) done, flat whole-file pass, excludes `when`-arrow via §4's helper | 109 |
| 18 | `vararg` | (a) verified, modifier-slot only | — |
| 19 | String templates (preserve `"$x"`/`"${x}"`) | (c) tokenizer-level done | 116 |
| 19.1 | Triple-quoted raw strings | (c) tokenizer-level done, verified via 14-case harness | 117 |
| 20 | Sealed classes/interfaces | (a) verified, ordinary K&R rules apply | — |
| 21 | Type aliases | (a) verified | — |
| 22 | Extension functions | (a) verified — `fun String.reverseWords()` receiver-type shape needs no special-casing | — |
| 23 | Known Gaps | (a) excluded, same posture as STYLE_JAVA.md's own excluded section | — |
| K2.1 | Guard conditions in `when` | (a) verified, extends §4 as-is, matches style doc's worked example byte-for-byte | — |
| K2.2 | `data object` | (a) verified, formatted exactly like `object` | — |
| K2.3 | Other 2.0/2.1 features | (a) excluded, no new formatting rules per style doc | — |

### Step 2 — `KotlinModifierPriority.java` — DONE

- [x] Column order for Kotlin's modifier set (visibility, modality,
      `override`, `const`, `lateinit`, `val`/`var`). Confirmed no
      cross-declaration-kind conflict analogous to Java's `abstract`/
      `volatile` case (RDD_KEY_83) — `const`/`lateinit`/`override`/modality
      never fight over column order across declaration kinds. Implemented
      as `grid/KotlinModifierPriority.java`: columns 0 (visibility) / 1
      (modality) / 2 (`override`) / 3 (`const`) / 4 (`lateinit`) / 5
      (`val`/`var`).

### Step 3 — `KotlinSpecificRule.java` — DONE

Every "(c)"-flagged section from Step 1 implemented, one per checkpoint
commit; per-section RDD_KEY mapping is the Step 1 scoping table above. Only
remaining documented gap: §8 block-bodied accessors, getter+setter pairs,
and initializer+accessor properties are preserved-as-written but not
grouped (see Open Questions).

### Step 3.5 — Configuration Property Wiring — DONE

Pipeline fully wired and verified live — `Main.java` `inferLanguage`
auto-detects `.kt`/`.kts`, `Formatter.java` runs every Kotlin rule class
through the same pipeline as Java/C++. Config uses `.jxmake-code-formatter`
`key=value` format; boolean keys accept `on`/`off` only.

- [x] `line-length`/`indent-size`/`indent-style` — same behavior as
      Java/C++, see `STATE_COMMON.md` → Config Keys and Defaults.
- [x] `closing-comment-min-lines` — wired via shared `BlockStructureRule`.
- [x] `format-macros` — permanent no-op for Kotlin (no preprocessor).
- [x] `line-endings` — language-agnostic, applied post-format in `Main.java`.
- [x] `normalize-comment-start-case`/`normalize-comment-end-period` — wired,
      same cross-language behavior as Java.
- [x] Kotlin import ordering (§24): `kotlin-import-order`/`-sort`/`-depth`/
      `-blank-lines` config keys, `KotlinSpecificRule.enforceKotlinImportOrdering`
      (mirrors Java's; no `static` bucket, replaced by a `kotlin` group;
      aliased/wildcard imports sort by original qualified name). RDD_KEY_118.
      Verified standalone; not yet wired into `Formatter.formatOne`.
- [x] JXM_CFMT_DIS/ENA + `--format-off` — confirmed working, language-generic.
- [x] README.md/README.txt updated for Kotlin support.

### Step 4 — Test Fixtures — DONE

Both `kt_combined_inp.kt`/`kt_combined_out.kt` and `kt_comments_inp.kt`/
`kt_comments_out.kt` fully pass, both enabled in the `Makefile`.
**Fixtures are handwritten and may have syntax errors — confirm with the
user as needed.** Methodology: format the input, diff against the
reference to find bugs, plus an idempotency check (output re-run through
the formatter should be unchanged).

- [x] `kt_combined_inp/out.kt` — STYLE_KOTLIN.md + STYLE_KOTLIN2.md
      end-to-end coverage. Round-trips clean, forward + idempotency. 9
      punch-list bugs found, all resolved (see below); 2 non-bugs (fixture
      naming ambiguity, intentional trailing whitespace on EOF blank line)
      left as-is.
- [x] `kt_comments_inp/out.kt` — uncommon comment locations +
      JXM_CFMT_DIS/ENA. 4 bugs found, all resolved (RDD_KEY_131).
- [x] **`make test` 36/36** (34 prior C/C++/Java + real_code_regressions
      fixtures, plus both Kotlin fixtures, forward and idempotency both
      green).

**`kt_combined` punch list** (all resolved, one-line summary each):
1. Missing blank line/closing comment on `class`/`enum class` with primary
   constructor — RDD_KEY_119 (tokenizer, Kotlin-gated).
2. `for(...) { stmt }` not collapsing (no `;` to count) — RDD_KEY_120.
3. `when(status) { ... }` squished/mis-indented — `parseKotlinDeclaration`
   stripped newlines from a multi-line block-expression initializer —
   RDD_KEY_121.
4. Fixture typo (`int`→`Int`), fixed by user, not a formatter bug.
5. `result1`/`result2` run-together — fixture ambiguity, fixed by user.
6. Apparent double-indentation — stray test-harness `indent-size=8`
   artifact, not a bug. RDD_KEY_122 fixed a real separate bug in the same
   area (`set(value) { ... }` accessor closing-brace indent).
7. `val safe = ...` spacing/alignment near the `when` fix — two parts:
   missing space in `.let{ }` (RDD_KEY_123) and column-alignment with a
   following destructuring line (RDD_KEY_126, reverses RDD_KEY_107).
8. `if(...) return@X` / `if(...) expr` braceless collapse not firing —
   RDD_KEY_124 (main case), RDD_KEY_127 (bare `else`), RDD_KEY_128
   (collapsed-`else` column padding).
9. Explicit-return-type functions missing blank-line-before-`return` —
   RDD_KEY_125 (`isFunctionBodyBrace` didn't recognize `: ReturnType`;
   also surfaced an unrelated C++ `->`-scan ordering bug).

**`kt_comments` punch list** (all resolved, RDD_KEY_131):
1. Missing blank line before a comment-led `when` branch —
   `ensureBlankLineInGap` replaced with `SwitchRule`'s comment-anchored version.
2. Missing blank line before a comment-led `return` — Kotlin-only carve-out
   in `MiscRule.insertBlankLineBeforeReturn`.
3. Leading blank line inside a body not stripped when first statement
   isn't a declaration — new `stripLeadingBlankBeforeNonDeclarationStatement`.
4. Outermost class missing closing blank line + comment — an unrelated
   frozen (JXM_CFMT_DIS/ENA) region nested inside suppressed the outer
   class's own blank line/comment; shared-class fix, also fixed a latent
   identical bug in `java_format_toggle_out.java`.

---

## Step 5 — Dogfood / Real-Code Testing

**Status: dogfood tree now compiles clean end-to-end — Step 5's core goal
is met.** Any future session picking this up should treat it as
regression-watching / further polish, not a known-broken state. Bucket D3
(see "Dogfood: JetBrains/kotlin" below) is **folded into `STATE_CURLY_GDR.md`
as of 2026-08-02** — see that section for the pointer; this job has no
independently-open item of its own.

Applied STATE_COMMON.md's real-code-testing methodology (clone a real,
compiling Kotlin project → format → idempotency round1 vs round2 →
compile-check) once Steps 0-4 were complete.

**Standalone `K2JVMCompiler` classpath — rejected, do not use.** A bare
`kotlin-compiler-embeddable` + `kotlin-stdlib` classpath cannot
syntax-check an Android/AndroidX candidate — every file under
`gui_frontend_android/app/src/main/java/*.kt` imports `android.*`/AndroidX
APIs, only resolvable via the project's own Gradle build. Use the Gradle
wrapper (tool (2) below) for any Android/Gradle candidate.

For `gui_frontend_android`: copy once into a **persistent** location —
`~/Projects/Shadow/rc_gui_frontend_android_DOGFOOD` — not `/tmp`, so it
survives reboots. Never write to `~/Projects/RobotCoding/gui_frontend_android`
itself (read-only); always re-verify a fix against the true pristine
originals there (a stale dogfood copy can hold already-formatted output and
falsely look fixed/unfixed).

**One-time setup after the copy:** edit `gradle.properties` in the dogfood
copy so `project.buildDir=build` (plain relative value — the original
points at the real project's own external build dir, which would still
collide even from a copy). Redo if the dogfood dir is ever deleted and
recopied.

```bash
cp -r ~/Projects/RobotCoding/gui_frontend_android ~/Projects/Shadow/rc_gui_frontend_android_DOGFOOD
cd ~/Projects/Shadow/rc_gui_frontend_android_DOGFOOD
# one-time only: edit gradle.properties, set project.buildDir=build
export ANDROID_HOME=~/android_devel
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/gradle-8.9/bin
export PATH=/opt/openjdk-21_linux-x64_bin/jdk-21/bin:$PATH
./gradlew compileDebugKotlin
```

(`gui_frontend_android/env.sh` sets the same `ANDROID_HOME`/`PATH`/JDK but
ends by `exec bash`-ing into an interactive session — source only its
`export`/`cd` lines for a scripted run.)

**Lightweight PSI-based syntax-only checker (`kotlin_syntax_check`) — viable, distinct
from the rejected full-compilation recipe above.** The rejected K2JVMCompiler
note is about a bare classpath doing a *full compile*, which can't resolve
`android.*`/AndroidX imports without Gradle's dependency graph.
`kotlin_syntax_check` is lighter: parses a `.kt` file to a PSI/AST via
`KotlinCoreEnvironment`/`KtPsiFactory` and reports `PsiErrorElement` nodes
(parse errors only, no semantic/type checking) — never resolves `android.*`
imports, so the AndroidX objection doesn't apply. Plain classpath-based
standalone Java program; every needed class is bundled in the single
shaded `~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib/kotlin-compiler.jar`
(confirmed via `unzip -l`, no separate intellij-core/trove4j jars needed).

Tool location: `util/CodingStyle.md/formatter/tools/verifiers/`
(`kotlin_syntax_check.java` + compiled `.class`; committed, licensed
project tooling, alongside the other jobs' syntax checkers).

Build (JDK 21, matches this compiler's class file version 52 = Java 8
target, runs fine on 21; see STATE_COMMON.md's "Verifier toolchain paths"
for the `$JDK`/`$KLIB` env setup and why each is needed):

```bash
"$JDK/bin/java" -cp ".:$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_syntax_check <file.kt> [file2.kt ...]
```

Exits 0 and prints "OK: no syntax errors" when clean; exits 1 and prints
each `PsiErrorElement`'s description + text range when a parse error is
found. Verified against this project's own `test/kt_combined_out.kt`
(passes clean) and a deliberately corrupted copy with injected stray `}}}`
(correctly reports the right errors at the right offsets).

**Recommended use going forward:** run `kotlin_syntax_check` first as a
near-instant pre-filter. It does NOT replace `./gradlew compileDebugKotlin`
for real compile-check testing (no semantic/unresolved-reference checking)
— keep using the Gradle recipe for that.

Follow STATE_COMMON.md's fixture-registration convention when a bug is
found and fixed here (`test/real_code_regressions_N_{inp,out}.kt`,
registered in `Makefile`'s `INP_FILES`, documented in `test/README.txt`,
standard copyright header) — same precedent as the `indent-size = 2`
config-wiring no-op exception noted there.

**Dogfood Output Validation — `kotlin_content_diff`.** A content-preservation
checker for Kotlin, complementing `kotlin_syntax_check` (which only proves
"still parses", same `java_content_diff`/`css_content_diff.py`/
`xml_content_diff.py` precedent). Reuses `kotlin_syntax_check`'s
`KotlinCoreEnvironment`/`KtPsiFactory` infrastructure, modeled on
`java_content_diff`'s design split by content family (import sorting,
declaration-alignment whitespace, comment-case normalization are all
legitimate transforms, not corruption):
- **imports** — compared as a multiset (`getImportedFqName()` +
  `isAllUnder()`/alias, sorted) since reordering is legitimate.
- **every top-level declaration** — compared in original relative order,
  each canonicalized by a hand-rolled leaf-token walk (whitespace/comment/
  KDoc skipped, remaining leaf text joined with single spaces) rather than
  a pretty-printer, since IntelliJ PSI is a lossless CST with no built-in
  canonical form to lean on.
- **comments** (line/block/KDoc) — extracted separately, compared as a
  multiset, whitespace-normalized and lowercased, so a case-only change
  isn't flagged but a dropped/corrupted comment still is.

**Gotcha hit and fixed during verification:** the leaf-token walk and
comment extraction MUST use `ASTNode.getChildren(null)` (via
`PsiElement.getNode()`), not `PsiElement.getChildren()` or
`PsiTreeUtil.findChildrenOfType()`. For stub-based elements (`KtClass`,
`KtProperty`, `KtNamedFunction`), `PsiElement.getChildren()` silently omits
plain leaf tokens — identifiers, keywords, and critically comments
(confirmed via an ASTNode-level dump showing `BLOCK_COMMENT`/`EOL_COMMENT`
reachable only through `ASTNode.getChildren(null)`). Switching both the
canonicalization walk and comment collection to ASTNode traversal fixed it.

Run (same classpath/env as `kotlin_syntax_check` — see STATE_COMMON.md's
"Verifier toolchain paths"):
```bash
"$JDK/bin/java" -cp ".:$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_content_diff <original.kt> <formatted.kt>
```
Verified against a hand-crafted good pair (reindentation + import sort +
one comment recapitalization — passes clean) and two bad pairs, a dropped
statement and a corrupted comment (both correctly flagged), after the
ASTNode-traversal fix above. Test fixtures kept in `/tmp` only (hand-crafted
verification pairs, not registered as permanent `test/` fixtures).

**Tools/compiler used**

(1) `kotlinc` — bare standalone compiler, e.g.:
```bash
PATH=/opt/openjdk-21_linux-x64_bin/jdk-21/bin:$PATH \
  ~/xsdk/kotlin-compiler-2.4.0/kotlinc/bin/kotlinc <file(s)-or-source-set>
```
Sufficient for a non-Android/non-Gradle candidate's own source set (e.g.
okio's `commonMain`, kotlinpoet's `jvmMain`) since it needs no external
dependency graph beyond the stdlib; `expect`/`actual`/unresolved-reference
noise from checking one source set in isolation is expected and ignored.
Cannot resolve AndroidX or a multi-module Gradle project's own dependencies
— see the rejected K2JVMCompiler note above and use (2) instead for those.

(2) `./gradlew compileDebugKotlin` — full command block above (with
`ANDROID_HOME`/JDK 21 on `PATH`, run against a persistent dogfood copy,
never the original checkout). Used for Android/Gradle candidates needing
the real SDK/AndroidX dependency graph (`gui_frontend_android`).

(3) `kotlin_syntax_check` — PSI-based syntax-only checker, build/run
commands above. Used when a full Gradle build is not wanted/needed
(`kotlinx.coroutines`, per explicit user request) — catches parse errors
only, weaker confidence than (2) (no semantic/type checking).

**Finished dogfood / real-code testing** (full bug detail lives in the
Resolved Design Decisions table above — this list is the per-project
index: scope, config, which RDD_KEYs, fixtures, verification result).

1. **RobotCoding `gui_frontend_android`** (Android/Gradle app, 46 `.kt`
   files) — complete, config: default. 9 idempotency bugs (RDD_KEY_134–140)
   then ~50 compile errors across 9 files found via `./gradlew
   compileDebugKotlin` (RDD_KEY_141–144). Final: `BUILD SUCCESSFUL`, zero
   errors (2 pre-existing unrelated deprecation warnings). Verified via
   tool (2).
2. **`github.com/square/okio`** — core bugs fixed, config `indent-size=2`
   (matches okio's own `.editorconfig`; RDD_KEY_145 confirmed default
   `indent-size=4` produces spurious diffs, not a bug). Fixed RDD_KEY_146–148
   (fixture `_30`), RDD_KEY_150–151 (fixture `_31`). Verified via
   round1/round2 idempotency diffing + tool (1) against `commonMain` only.
   One bug found but not fixed at the time — RDD_KEY_149 (later resolved by
   RDD_KEY_163).
3. **`github.com/Kotlin/kotlinx.coroutines`** — fully closed, config:
   default. Scoped to `kotlinx-coroutines-core`'s `common`+`jvm` source
   sets (163 `.kt` files), tool (3) per user instruction instead of a
   Gradle build. Idempotency: 11 non-idempotent files, resolved across
   RDD_KEY_154 and RDD_KEY_158–162. Compile-check: 9 of 163 files had
   genuine syntax errors, resolved via RDD_KEY_155–157. Verified via
   round1/round2 diffing + tool (3) only (weaker compile-check confidence
   than `gui_frontend_android`).
4. **`github.com/square/kotlinpoet`** — fully closed, all 4 idempotency
   shapes resolved, config `indent-size=2` (matches kotlinpoet's own
   `.editorconfig`). Initial pass: RDD_KEY_152–153. A later 125-file re-run
   surfaced a residual 10-file idempotency gap in 4 shapes: RDD_KEY_163
   (also resolved RDD_KEY_149's deferred `okio` bug), RDD_KEY_164,
   RDD_KEY_165, RDD_KEY_166. Fixtures `_46`–`_49`. `make test`: 68/68.
   Confirmed via a fresh full-125-file re-run, `diff -rq round1 round2`
   empty.
5. **`github.com/arrow-kt/arrow`** — fully closed, all bugs resolved.
   Functional-programming library, scoped to `arrow-core`'s and
   `arrow-optics`'s `commonMain/kotlin` source sets (63 `.kt` files),
   config `indent-size=2`, compile-checked via tool (3) per explicit user
   instruction instead of the Gradle-copy dance. Compile-breaking:
   RDD_KEY_171–173, RDD_KEY_176. Idempotency-only: RDD_KEY_174, RDD_KEY_175,
   RDD_KEY_177 (investigation's last open item). Fixtures `_59`–`_64`.
   Verified: a fresh full-scope 63-file reformat is round1/round2
   byte-identical; `kotlin_syntax_check` 0 errors across all 63 files
   (started at 5); `make test` 88/88 clean, zero regressions throughout.

**Not started / in progress**

1. **`github.com/JetBrains/kotlin`** — IN PROGRESS. The Kotlin compiler's
   own source tree (~16k `.kt` files after filters), originally queued as
   a last-resort/stress candidate (similar posture to `microsoft/STL`/
   `llvm-project` in the C++ list) but substantial work has landed.
   Category 1 (parse errors/corruption) fully closed. Category 2
   (idempotency): D2a (332/334 known-flap files) and D4 (~8 files) both
   fully closed; D1 fully closed (all three group-width-recompute-
   instability sub-shapes fixed); **D3's root cause is confirmed
   (RDD_KEY_221) but no fix has landed** — a first candidate fix was tried
   and reverted after regressing 28 fixtures across C/C++/Java/TS/Kotlin at
   `make test` (RDD_KEY_221); a second, more careful candidate design (the
   2026-07-31 `statementStartIndex` scoping session) was implemented and
   validated in a follow-up session, but its own documented "Known open
   risk" materialized for real — 16 Kotlin fixtures regressed because
   Kotlin statements are usually NEWLINE-, not `;`-separated, so the
   backward scan merges an unrelated preceding sibling statement into the
   measurement — reverted again, not committed (RDD_KEY_226). See
   `README.md`'s Known Limitations section and RDD_KEY_221/RDD_KEY_226. D3
   remains open (~34 files total, including 2 files reclassified out of
   D2a's own former residual: `GenerateReleaseNotes.kt`/`TypeBridging.kt`).
   A real fix needs actual Kotlin statement-boundary tracking (a depth-0
   NEWLINE that ends a statement, distinguished from one that's mid-wrap
   inside the current candidate's own already-broken rendering).
   **As of 2026-08-02, D3 is folded into `STATE_CURLY_GDR.md`'s "General
   scope-depth reindentation" (GDR) job rather than tracked as a
   standalone open item here** — both investigation sessions below already
   concluded this is closer to that job's territory than a self-contained
   fix; see `STATE_CURLY_GDR.md`'s "D3 fold" section for the summary and
   next steps. Full diagnosis, tool commands, and per-cluster fix history
   remain below in "Dogfood: JetBrains/kotlin" for reference.

**When a test completes:** move/compact its entry from "Not started" into
"Finished dogfood / real-code testing", and add a new numbered entry to
"Tools/compiler used" if a genuinely new tool is introduced.

---

## Dogfood: JetBrains/kotlin (categorization pass)

**Current status:** Category 1 (parse errors/corruption) **fully closed**
— every C1-C6k cluster/shape fixed, 0 genuine formatter-caused failures
remain (2 pre-existing BOM syntax-checker artifacts, unrelated to the
formatter, are the only remaining `kotlin_syntax_check` hits). Category 2
(idempotency-only): D2a **fully closed** (332 of 334 known-flap files —
the remaining 2, `GenerateReleaseNotes.kt`/`TypeBridging.kt`, re-triaged as
ordinary D3 instances, not a distinct D2a shape); D4 **fully closed** (~8
files, RDD_KEY_218); D1 **fully closed** (RDD_KEY_219 fixture `_169` +
RDD_KEY_220 fixture `_170`); **D3 still fully open — folded into
`STATE_CURLY_GDR.md` as of 2026-08-02, no longer independently open here
(see that file's "D3 fold" section).**

Checkout: `/tmp/jb_kotlin_kt/kotlin-master` (fresh `.kt`-only tarball
extraction from `codeload.github.com/JetBrains/kotlin/tar.gz/refs/heads/master`
— `tar --wildcards '*.kt'` used instead of a full git clone to avoid the
non-Kotlin majority of this huge repo) — reuse this checkout, do not
re-clone. Scope: all `*.kt` excluding `*/testData/*` (compiler test
fixtures), `*/build/*`, `*/resources/*`, anything with `generated` in its
name — ~16k files (`/tmp/kt_filelist.txt`).

**Tools/compiler used:** same as this file's general "Tools/compiler used"
section above — JDK `/opt/openjdk-21_linux-x64_bin/jdk-21`, Kotlin compiler
`~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib`, `kotlin_syntax_check` (compile
once with javac against `kotlin-compiler.jar:kotlin-stdlib.jar`, then run
against files). Formatted via `--preserve-tree --root <src> --out <dir>`
single-JAR-invocation batching per STATE_COMMON's methodology (round1 then
round2 off round1's output), verified via `kotlin_syntax_check` (round1)
and `diff -rq round1 round2` (idempotency). No `indent-size` override
needed — all findings reproduce at default config.

### 2026-07-28 Re-triage — fresh full-corpus numbers

Reused the same checkout (not re-cloned). True denominator: **16078**
files (earlier 16268 figure was a stale recount). Batched `--preserve-tree`
reformat, one JAR invocation per top-level subdirectory. No `indent-size`
override — default config.

- **Category 1 (parse errors), fresh baseline: 10 raw failures on round1**,
  of which 2 are a pre-existing BOM syntax-checker artifact
  (`JavaScriptLexerBase.kt`/`JavaScriptParserBase.kt` — confirmed via
  `xxd` the `EF BB BF` UTF-8 BOM is identical in original and formatted
  output; the formatter preserves it correctly, the syntax checker's own
  BOM handling is at fault — permanent, unrelated to any cluster below),
  leaving **8 genuine formatter-caused failures, all now FIXED** (C6k
  shapes 1-5). Category 1 is now 0/8 genuine failures.
- **Category 2 (idempotency-only), fresh baseline: 334/16078 (2.08%)** —
  essentially flat vs. the stale 344/16268 count despite the intervening
  C1-C6k fixes (those targeted Category 1 crash/corruption, not the D1-D4
  idempotency-flap families, so no overlap expected).

Working files (scratch, not committed): round1/round2 output trees and
syntax/idempotency logs under `/tmp/kt_retriage_*`, `/tmp/round1_*`,
`/tmp/c6k_fresh_errors.txt`, `/tmp/idem_*` — reused by later sections below
(e.g. the D3 design session), not re-generated each time.

### Category 1 — Critical (crash/corruption) — ALL CLOSED (C1-C6k)

| Cluster | Files | Root cause / Fix | Fixture / RDD_KEY |
|---|---|---|---|
| **C1** — own-line comment before a constructor param fused onto the param, deleting it | 1+ | `parseKotlinSignature`'s comma-split moved a comment starting the next param's slice onto the previous param. Fixed via `findLineStartComments`/`findStandaloneComments` + `leadingCommentOwnLine`. | `_144` |
| **C2** — `@Annotation` at expression position gets spurious space after `@` | ~22 | `DeclarationAlignmentRuleCore.needsSpaceBetween` lacked the `@`-tight case `MiscRuleCore` already had. | `_149` |
| **C3** — multi-statement named-argument lambda body fused, no separators (largest crash cluster) | ~70+ | `renderCallCandidate`'s bail loop used `splitTopLevelCommas` (no brace-depth tracking), misreading a lambda's own param commas as call-arg separators. New brace-depth-aware `splitTopLevelCommasBraceAware`. | `_146` |
| **C4** — CLOSED, not a real bug: miscategorized instance of C5, folds into its count. | — | — | — |
| **C5** — multi-line `when (...)` subject's embedded newlines leaked into the `// when <subject>` closing comment, orphaning trailing tokens | ~15-20 | `formatWhenExpressions`'s subject capture preserved raw newlines; fixed by collapsing whitespace runs to a single space. Also resolved C4. | `_148` |
| **C6a** — typed `by`-delegate's type-scan swallowed the whole delegate expression (incl. multi-statement trailing lambda) | 42 | `parseKotlinDeclaration`'s type-token scan only terminated on `=`; a `by`-delegate has none — bail added on any top-level `by`. | `_150` |
| **C6b** — multi-dollar string prefix (`$$`/`$$$`) gets spurious space before the string | 12 | New `isDollarRun` carve-out in both `needsSpaceBetween` duplicates. | `_158`; RDD_KEY_207 |
| **C6c** — nullable callable ref `T?::member` corrupted to `T ?: :member` | 2 files, high density | `MULTI_CHAR_OPS`'s `"?:"` match had no lookahead for a following `::` — added `source.startsWith("?::", pos)` bail. | `_151` |
| **C6d** — `@Composable (Params) -> Type` function-type parens lose required leading space | 6 | The general "`IDENTIFIER` before `(` is tight" call rule fired before either duplicate's `@`-tight case could. New lookback/lookahead carve-out in both Core duplicates plus a third overlooked copy in `KotlinDeclarationAlignmentRule.renderTokens`. | `_153` |
| **C6e** — trailing-lambda multi-statement body fused when used as boolean sub-expression inside `if(...)`/`&&`/`\|\|` | 6 | `tryCollapse`/`tryCollapseBraceless` guarded the collapse *body* against embedded multi-line braces but not the *condition* — reused `containsMultilineNestedBrace` as a condition bail. | `_159`; RDD_KEY_208 |
| **C6f** — multi-line collapse swallows an embedded `//` comment, corrupting everything after it (largest sub-cluster, C1-grade severity) | ~20, overlapped C6h/C6k | Three independent comment-unaware flattening call sites, each fixed with a comment-aware bail returning `null` (leave span untouched): (1) `parseKotlinParam`'s default-value grid rendering, (2) `tryCollapseBraceless`'s condition render, (3) `parseFunctionTail`'s expression-body slice. | `_156` (1-2), `_157` (3) |
| **C6g** — backtick identifier containing literal `(` breaks paren-depth tracking downstream | 4 | Kotlin backtick spans weren't recognized in the tokenizer's dispatch loop at all — new `TokenizerCurly.emitKotlinBacktickIdentifier`, opaque-span treatment mirroring JS/TS template literals. | `_154` |
| **C6h** — `Missing '}'` at EOF in Gradle-plugin tests — folded into C6f, no independent cause | 8 | Downstream symptom of C6f; no separate fix needed. | covered by `_156`/`_154` |
| **C6i** — multiple one-line interface member declarations fused without separators | 1 | `applySignaturePass`'s `: ReturnType` tail detection only bailed on a blank-line run, not an ordinary single newline — new `hasTopLevelNewline` bail, gated off when a depth-0 `=` is present. | `_155`; RDD_KEY_206 |
| **C6j** — square-bracket destructuring lambda params `[x, y] ->` lost the space after `{` | 1 | `[` was always-tight (C/C++/Java indexing rule) — narrow `lang.isKotlin && "[" after "{"` carve-out forcing the space. | `_152` |
| **C6k** — 5/5 shapes fixed, cluster closed (8 genuine failures) | 8 | **Shape 1** (multi-statement fusion): two causes — `isSingleStatementBody` only routed Kotlin through the newline-aware helper when `semiCount != 1`; `parseKotlinSignature` stripped NEWLINEs before detecting a param's multi-line lambda default. **Shape 2**: a raw string ending in its own `"` before closing `"""` produces a 4+-quote run; `skipKotlinRawString` closed at the run's first `"""` instead of last three. **Shape 3**: RDD_KEY_144(A)'s `!is`/`!in` carve-out missing from `MiscRuleCore` (param-default path). **Shape 4**: `enforceNullSafetyOperatorSpacing` treated `!!` unconditionally tight on the right; new `isPostfixNullOpContinuation`. **Shape 5**: C6d's fix didn't cover a nullable function-type default (`@Composable( () -> Unit )?`) since lookahead only recognized `->`, not a following `?`. | `_163` (Shape 1), `_164` (Shape 2), `_160`/`_161`/`_162` (Shapes 3-5); RDD_KEY_212, 213, 209, 210, 211 |

**Baseline check:** the 70-file C6b-C6k total was cross-checked against the
raw `kotlin_syntax_check` failure list — 70/72 confirmed genuine formatter
bugs, the other 2/72 are the pre-existing BOM artifact (excluded from 70).

Working files retained in `/tmp` (not committed): earlier round1/round2
trees, syntax logs, and C6 triage lists (`/tmp/c6_remaining.txt`,
`/tmp/c6_70.txt`, `/tmp/round1c_syntax.log`, etc.) — superseded by the
2026-07-28 re-triage's fresh files above but left in place.

### Category 2 — Idempotency-only, fresh count 334/16078 files

**2026-07-28 re-triage — fresh per-bucket counts.** Sampling methodology:
334 total differing files (`diff -rq round1 round2`), a uniform random
40-file sample (`shuf -n 40`) manually diffed and bucketed by primary diff
shape. Estimates below are the sample's bucket proportion scaled to the
full 334, **not** exhaustive — treat as directional.

| Cluster | Sample hits (of 40) | Est. files | Status |
|---|---|---|---|
| **D2a.** Chained-fluent-call closing-brace drift (`}.apply {`, `}?.let {`, etc — a span's own `braceIndent`/`spanIndent` read off the volatile physical text of the PRECEDING span's own `}`) | 22 | 332 of 334 known flap files | **FIXED** (RDD_KEY_214/215/216, fixtures `_165`/`_166`/`_167`). Fix: generalized `isChainedCatchFinally` (RDD_KEY_158) into `isChainedFluentCall`, inherits the preceding span's already-resolved `prevEffectiveSpanIndent`. Root-caused against `declarationBuilders.kt`. RDD_KEY_214's own 6-file residual: 5 fixed by a `fun`-with-wrapped-`where`-clause gap (RDD_KEY_215) + a boolean-operator-chained continuation (RDD_KEY_216); the last 2 (`GenerateReleaseNotes.kt`/`TypeBridging.kt`) were misclassified as D2a-adjacent — direct diffing shows they're ordinary D3 wrap-decision-flap instances, left open under D3. `make test`: 214/214 → 215/215 → 216/216, zero regressions. |
| **D1.** Declaration/accessor column-alignment padding flap (round1 vs round2 disagree on padding width) | 12 | ~100 | **FULLY FIXED** (RDD_KEY_219 fixture `_169`, RDD_KEY_220 fixture `_170`). Same family as RDD_KEY_139/162 (group-width recompute instability). Three independent root causes, all fixed: (1) `renderAlignedGroup` rendered surviving rows as one flat shared-width grid even with an excluded overflowing row mid-group — fixed via maximal contiguous runs. (2) Same bug in `applyGetterSetterPass` — new Kotlin-gated `renderKotlinFilteredRuns`. (3) A member whose own solo width fits but whose group's shared-column padding alone pushes it over (silently absorbed pre-fix by a later `enforceCallLineBreaking` wrap) — fixed by porting RDD_KEY_162's fixed-point budget-exclusion loop into a depth-aware `GetterSetterRuleCurly.render`/`KotlinGetterSetterRule.render` override. |
| **D3.** Multi-line-call/condition wrap-decision flap (one line in round1, exploded across multiple lines in round2, or vice versa) | 4 | ~33 (broader than the original ~15-20 "lambda header" estimate — includes plain call-argument and `if(...)` condition wraps too) | **FOLDED INTO `STATE_CURLY_GDR.md` as of 2026-08-02 — no longer independently open here.** Root cause confirmed (RDD_KEY_221), TWO candidate fixes tried and reverted, neither landed. `MiscRuleCurly.renderCallCandidate`'s no-newline-branch fits-check measures a candidate against its entire enclosing physical source line (`lineStartIndex(tokens, nameIdx)`) instead of a stable position tied to the candidate itself, so the wrap decision flaps across rounds as the enclosing line's own length changes. Fix attempt 1 (anchor measurement at `nameIdx`) regressed 28 fixtures across C/C++/Java/TS/Kotlin at `make test` (RDD_KEY_221) — dropped legitimate same-statement prefix. Fix attempt 2 (2026-07-31 design session's `statementStartIndex`, a depth-0 `;`/`{`/`}` backward-scan, Kotlin-gated) was implemented and validated against the grounded `when`-arm example plus a synthetic multi-arm `when` stress fixture (both passed, round1==round2) — but regressed 16 Kotlin fixtures at the full `make test` because Kotlin statements are usually NEWLINE-, not `;`-separated, so the scan merges an unrelated preceding sibling statement into the measurement (RDD_KEY_226; concrete repro `real_code_regressions_20_inp.kt`). Both reverted, not committed. Documented in `README.md`'s Known Limitations. Landing a real fix needs actual Kotlin statement-boundary tracking (recognizing a depth-0 NEWLINE that ends a statement vs. one mid-wrap inside the current candidate's own already-broken rendering) — closer to `STATE_COMMON.md`'s "General scope-depth reindentation" architectural TODO's territory than a self-contained fix. Includes 2 files reclassified out of D2a's former residual (`GenerateReleaseNotes.kt`/`TypeBridging.kt`). **Folded into `STATE_CURLY_GDR.md` as of 2026-08-02 — the Kotlin job itself now has no independently-open bucket.** |
| **D4.** Minor adjacent-closing-brace spacing flap (`) }` vs `)}`) | 1 | ~8 | **FIXED** (RDD_KEY_218, fixture `_168`). `collapseBracelessBody` dropped a source-preserved trailing space before an already-braceless body's enclosing `}` via `renderInline`'s no-trailing-whitespace behavior. Reproduced against `JsArgumentsImpl.kt`; also fixed a latent instance already baked into fixture `real_code_regressions_33_out.kt`. |

Sample total: 22+12+4+1 = 39 of 40 (`org.w3c.dom.kt` showed both a D1 and a
D3 flap in the same diff — counted once under its dominant D1 shape). No
5th bucket shape needed — all 40 sampled files fit one of D1-D4.

**Recommended next step (not done yet):** run `kotlin_content_diff` across
the full 16078-file corpus before further Category-1-family work — it
would surface silent content loss undetectable by the syntax checker and
give a truer denominator. Otherwise, D3 is the next-highest-value open item.

## D3 investigation history (2026-07-31 design session + 2026-08-01 implementation attempt, RDD_KEY_226)

**Folded into `STATE_CURLY_GDR.md` as of 2026-08-02 — D3 is no longer
tracked as an independently open item in this file; see that file's "D3
fold" section for the current pointer.** Kept below for reference.

**Grounded example** (`EqualityAndComparisonCallsTransformer.kt`, a
`when`-arm, 109 chars, over the 100-char limit):
```kotlin
Name.identifier("compareTo") -> if (doNotIntrinsify) call else transformCompareToMethodCall(call)
```
Round1 correctly wraps both call candidates (`Name.identifier(...)` and
`transformCompareToMethodCall(...)`) since both are measured against the
same still-unmodified original line. Round2 (reformatting round1's own
output, and also within a single `formatOne` pass — `enforceCallLineBreaking`
runs twice) re-measures `transformCompareToMethodCall` against
`lineStartIndex(tokens, nameIdx)`, which now walks back only to the nearest
physical `NEWLINE` — the one baked in by `Name.identifier(...)`'s own prior
wrap — so the "enclosing line" it sees has shrunk to fit, and it wrongly
un-wraps. Root cause (RDD_KEY_221): one candidate's own wrap decision
changes the measured line boundary a *sibling* candidate on the same
logical statement sees; `lineStartIndex` tracks volatile physical layout,
not the stable logical statement.

**Fix attempt 1 (`nameIdx`-anchor) — reverted, regressed 28 fixtures.**
Anchoring at `nameIdx` discards legitimate same-statement prefix (`return
`, `if (`, `val x = `, etc), so lines that should wrap no longer do — an
underestimate failure mode, disjoint from the original bug, not a fix.

**Fix attempt 2 (`statementStartIndex`, 2026-07-31 design → 2026-08-01
implementation) — reverted, regressed 16 Kotlin fixtures.** Design:
replace `lineStartIndex(tokens, nameIdx)` at its one load-bearing call site
(`MiscRuleCurly.renderCallCandidate`'s no-newline fits-check) with a new
backward scan (mirroring `splitTopLevelCommasBraceAware`'s depth-tracking,
and `KotlinSpecificRule.signatureLineIndent`'s RDD_KEY_164/215 precedent)
that tracks paren/bracket/angle-bracket depth and stops at the nearest
depth-0 `;`, `{`, or `}` — never a bare `NEWLINE`. Gated `lang.isKotlin`,
scoped to the one call site. The design's own documented open risk: Kotlin
`when` arms (and most Kotlin statements generally, RDD_KEY_115) are
NEWLINE-separated, not `;`-separated, so a preceding sibling with no
depth-0 `;` could get merged into the backward scan.

Implemented and validated: the grounded repro and a dedicated synthetic
multi-arm `when{}` stress fixture (targeting the open risk directly) both
passed, round1==round2, zero regressions in C/C++/Java/TS. But the full
`make test` run still regressed 16 Kotlin fixtures — the risk materialized
in an *ordinary two-statement sequence*, not the `when`-arm shape the
stress test checked. Minimal repro (`real_code_regressions_20_inp.kt`):
```kotlin
val display = (if (warning != null) "$warning\n\n" else "") + "Done"
showMessage(context, display)
```
`showMessage(context, display)` already fits and should render unchanged,
but the backward scan from `showMessage`'s `nameIdx` finds no depth-0
`;`/`{`/`}` before it, walks past its own statement start through the
NEWLINE, and merges the entire preceding `val display = ...` statement
into the width measurement — producing a false-positive wrap. Reverted in
full (`git checkout -- src/com/jxmake/formatter/rules/MiscRuleCurly.java`,
confirmed clean); `make test` back to 225/225, zero net change.

**Conclusion:** the `;`/`{`/`}` depth-0 boundary rule alone can't handle
Kotlin's mostly-NEWLINE-delimited statement grammar. A real fix needs
actual Kotlin statement-boundary tracking (a depth-0 NEWLINE that ends a
statement vs. one mid-wrap inside the current candidate's own
already-broken rendering) — closer to the "General scope-depth
reindentation" architectural TODO's territory than a self-contained fix.
No fixture was added (fix never landed). A future attempt should start
from real statement/expression-boundary tracking, not another
token-depth-only backward scan.

## Open Questions

- **C4 — closed, no open question remains.** Was a miscategorized instance
  of C5, not a real bug: both collapse paths always join condition/body
  with a literal `" "`, so a missing separator is impossible by
  construction. Re-verified clean against the fixed JAR once C5's actual
  bug was patched. See Category 1 table's C4/C5 rows for detail; C4's
  ~44-file estimate folds into C5's count.
