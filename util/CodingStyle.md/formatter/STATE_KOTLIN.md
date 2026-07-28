# STATE_KOTLIN.md — Kotlin JAR Implementation Tracker

Read `STATE_COMMON.md` first — it has the shared commit/ambiguity/testing
conventions this file assumes. `STATE_C_CPP_JAVA.md` (the other job's file)
is NOT required reading for this one — only `STATE_COMMON.md` is.
Dogfood corpus status: see `STATE_DOGFOOD.md`.

---

## Purpose

Tracks implementation of Kotlin support in the deterministic JAR formatter
(`util/CodingStyle.md/formatter/`), per `STYLE_KOTLIN.md` / `STYLE_KOTLIN2.md`.

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
| RDD_KEY_149 | `square/okio` bug #4 — **found, not fixed, deferred** (later **resolved by RDD_KEY_163**): multi-line signature's column padding/trailing comma silently stripped downstream. See Open Questions. |
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
| RDD_KEY_216 | `JetBrains/kotlin` dogfood D2a residual, `TopLevelPhases.kt`'s remaining line — **FIXED**: `isChainedFluentCall` (RDD_KEY_214) only recognized `.`/`?.` continuation after the preceding span's own `}`, not a boolean-infix-operator-joined chain (`} || declarations.any { ... }`) — widened with new `isChainedBooleanOp` (`||`/`&&`). Fixture `_167`. Closes 5 of RDD_KEY_214's 6-file D2a residual (all but `GenerateReleaseNotes.kt`/`TypeBridging.kt`, which direct diffing shows are ordinary D3 wrap-decision-flap instances, not a distinct D2a shape — misclassified by RDD_KEY_214's own text, left open under the separate D3 bucket). |
| RDD_KEY_218 | `JetBrains/kotlin` dogfood cluster **D4** (minor adjacent-closing-brace spacing flap, sample hit `JsArgumentsImpl.kt`) — **FIXED, closes D4**: `BlockStructureRule.collapseBracelessBody` (the already-braceless multi-line-body collapse path exercised only on a reformat, once a prior pass had already stripped an enclosing `if`'s own braces) correctly excluded its enclosing scope's own terminating `}` from the rendered body but left the WHITESPACE token immediately preceding it inside `contents`, which `renderInline` then silently dropped (never emits trailing whitespace) — losing a source-preserved single space (`) }` → `)}`) on the second format pass; `tryCollapse`'s sibling braced-body path never has this loss since it lets the surrounding loop re-append a *different* piece of untouched whitespace verbatim instead of folding it into a render. Fixed by appending a trailing space when the token right before the enclosing `}` was WHITESPACE/NEWLINE. Also required correcting `real_code_regressions_33_out.kt` (a pre-existing fixture that had, by coincidence, already baked this same bug's buggy `)} as T` into its own recorded "expected" output — corrected to `) } as T`). Fixture `_168`. `make test` 216/216 forward + idempotency, zero regressions. |
| RDD_KEY_219 | `JetBrains/kotlin` dogfood cluster **D1** (declaration/accessor column-alignment padding flap) — **PARTIALLY FIXED**: two independent group-width-recompute-instability root causes (RDD_KEY_139/140/162 family). (1) `KotlinDeclarationAlignmentRule.renderAlignedGroup` rendered surviving (non-excluded) rows as one flat shared-width grid even when an excluded, overflowing, brace-bodied-init row sat in the MIDDLE of the group — fixed by rendering surviving rows as maximal contiguous runs instead. (2) The analogous bug in shared `ScopePipelineCurly.applyGetterSetterPass` (Kotlin's one-liner expression-bodied function/accessor grouping) — fixed with new Kotlin-gated `renderKotlinFilteredRuns` helper, C/C++/Java untouched. **Known remaining gap, left open**: a third sub-shape where the offending member's own solo/raw width fits under the limit (so the raw-length parse-time pre-check never excludes it) but the group's own shared-column padding alone pushes it over — `KotlinGetterSetterRule` has no RDD_KEY_162-style budget-aware exclusion mechanism at all. D1's ~100-file estimate likely contains an unknown mix of all three sub-shapes; closure is partial. Fixture `_169`. `make test`: 217/217 forward + idempotency, zero regressions. |

---

## Checklist

### Step 0 — Tokenizer Support (shared file, additive only)

**Critical rule:** `TokenizerCore.java` is shared with C/C++/Java. Every
addition must be additive and must not change existing token lexing.
Re-run the full test suite after this step, before Step 1.

- [x] Added to `MULTI_CHAR_OPS`: `?.`, `?:`, `!!`, `..<`, `..`
      (longest-prefix-first, same rule as `...`/`->*`). `->` reused as-is;
      `@` in labeled jumps needs no new entry (falls to `emitOperator`'s
      fallback, spacing is Step 3's concern). **Bug found/fixed:**
      `emitNumber()` consumed every `.` unconditionally, so `1..10` lexed as
      one bogus `NUMBER`; fixed by stopping consumption when a `.` is
      followed by another `.`. Verified via `TokenizerCore` dump, `make test`
      25/25 unaffected.
- [x] Added Kotlin keyword set `KEYWORDS_KOTLIN` (hard + modifier/soft
      keywords, unconditionally reserved, same simplification as Java's
      `var`/`record`).
- [x] Added `NAMED_CONSTRUCT_KOTLIN` = `class`, `object`, `interface`,
      `enum`, `init`. `companion object`/`enum class` special-casing and
      `computeConstructName()` lookback per shape deferred to Step 1.
- [x] Re-ran full C/C++/Java suite: **25/25 pass, zero regressions**.
- [x] **Follow-up (§13 cross-check):** added `"in"`/`"out"` to
      `GENERIC_SAFE_KEYWORDS` so `reclassifyAngleBrackets` treats
      declaration-site variance (`Box<out T>`, `Comparable<in T>`) as a
      generic pair not a comparison. No-op for C/C++/Java. `make test` 32/32
      before/after. RDD_KEY_113.
- [x] **Follow-up (§19 cross-check):** added Kotlin-only interpolation-aware
      string scan (`skipKotlinString`/`skipKotlinInterpolationBlock`/
      `skipKotlinChar`) inside `emitString()`, gated `lang.isKotlin` — the
      shared naive scan-to-next-`"` misread a nested string inside `${...}`
      (`"${foo("x")}"`) as three tokens instead of one. Depth-tracks
      `${...}`'s own `{`/`}` nesting, recurses for nested literals
      arbitrarily deep. Non-Kotlin scan untouched. `make test` 32/32
      before/after. RDD_KEY_116.
- [x] **Follow-up (row 19.1, investigated on request):** added Kotlin-only
      raw-string support (`isKotlinRawStringOpener`/`emitKotlinRawString`/
      `skipKotlinRawString`), checked before the plain-`"` and C/C++
      raw-string-prefix branches. Naive path mis-lexed `"""..."""` into
      multiple STRING/IDENTIFIER tokens and leaked a spurious `NEWLINE` on
      multi-line ones. No backslash-escape processing (literal `\` by
      design); terminates greedily at first `"""`, matching the real
      compiler. `skipKotlinInterpolationBlock` extended for nested raw
      strings inside interpolation. Non-Kotlin paths untouched. `make test`
      32/32 before/after. RDD_KEY_117.

### Step 1 — Scoping Pass (mirrors `JavaSpecificRule.java`'s own scoping, RDD_KEY_59)

- [x] Cross-checked every section of `STYLE_KOTLIN.md`/`STYLE_KOTLIN2.md`
      against the already-COMPLETE shared rule classes
      (`DeclarationAlignmentRule`, `BlockStructureRule`, `SwitchRule`,
      `GetterSetterRule`, `MiscRule`) to determine, per section: (a) already
      satisfied as-is once Step 0 lands, (b) satisfied by a small additive
      shared-class extension, or (c) needs a new method in
      `KotlinSpecificRule.java`. Table below.
- [x] Flagged shared-class *behavior* changes (not just additions) — see
      **Open Questions**: `DeclarationAlignmentRule`'s `Declaration` model
      assumes C/Java's `[modifiers] Type name [= init]` order, structurally
      reversed from Kotlin's `[modifiers] val/var name : Type [= init]`.
      Stopped here rather than guessing a direction.

**Scoping table** (section numbers match `STYLE_KOTLIN.md`; `K2.N` = `STYLE_KOTLIN2.md` §N):

| § | Topic | Outcome | Notes |
|---|---|---|---|
| 1 | Semicolons (strip optional `;`) | (c), **done** | No shared class strips statement-terminating `;` for any language — Kotlin-only `KotlinSpecificRule.stripOptionalSemicolons`. See RDD_KEY_115 for the rewrite from an earlier flawed version. |
| 2 | `enum class` with members | (a)/(c), **done** | `BlockStructureRule.classifyNamed`'s "keyword before `class` is `enum`" check already labels the closing comment for free once `enum`/`class` are Kotlin keywords. Body-open/close blank lines already free via `insertNamedConstructBlankLines`. Blank-line emphasis around the entry-list-terminating `;` needed new `KotlinSpecificRule.separateEnumConstantListTerminator`, mirroring the Java equivalent. RDD_KEY_111. |
| 3 | Brace style (Allman fn bodies / K&R everything else) | (a) K&R direction **verified**; (c) Allman-conversion direction **done** | `BlockStructureRule.qualifiesForKAndR`'s keyword sets already cover Kotlin's vocabulary. Function-body K&R→Allman needed new `KotlinSpecificRule.enforceFunctionDefinitionAllmanBraceStyle` (backward scan to `fun`/`constructor`, more conservative than Java/C++ since a trailing-lambda call is token-shape-identical to a function body brace and there's no `new` to rule calls out). Also handles `: ReturnType` between `)` and `{`, and a one-liner body staying K&R (RDD_KEY_75/RDD_KEY_89 exception). RDD_KEY_114. |
| 3.1 | Class/Object/Companion Object bodies | (b), **done** | Named constructs already worked; the headless gap (anonymous `companion object {}`, anonymous `object : Interface {}`, `init {}`) fixed via RDD_KEY_99 (`BlockStructureRule.classifyKotlinHeadlessNamed`, gated by `Lang.isKotlin`), which also fixed a related tokenizer bug (`:` wrongly arming a supertype identifier as the construct name). |
| 3.2 | `catch`/`for`/`while`/`when` no space before `(` | (b), **done** | Added `"when"` to `MiscRule.TIGHT_PAREN_KEYWORDS`. No-op for C/C++/Java. RDD_KEY_100. |
| 3.3 | Secondary constructors (Allman body) | (c), **done** | Covered by the same §3 method — a secondary constructor is recognized by the token before `(` being `constructor` itself. Verified via harness. RDD_KEY_114. |
| 3.4 | `init` blocks | (b), **done** | Same headless-named-construct fix as §3.1 — `init {}` returns `"init"` from `classifyKotlinHeadlessNamed`. RDD_KEY_99. |
| 4 | `when` expression (arrow alignment, closing comment, blank lines) | (c) | `SwitchRule.java` is colon-form-statement-only (STYLE.md §13), unrelated; `JavaSpecificRule.enforceSwitchExpressionArrowAlignment`'s `case`/`default`-keyword scan and all-or-nothing bailout don't fit Kotlin's keyword-less, non-all-or-nothing `when` — new `KotlinSpecificRule.formatWhenExpressions`. RDD_KEY_101. Idempotency bug fixed under RDD_KEY_121 (root cause was `KotlinDeclarationAlignmentRule`, not this method). |
| 5 | Null-safety operators (`?.`/`!!` tight, `?:` spaced) | (c), **done** | New `KotlinSpecificRule.enforceNullSafetyOperatorSpacing`, flat whole-file pass. RDD_KEY_102. |
| 6 | Variable/property declaration alignment | (c), **done** | New `KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule` (loosen-then-extend, not an independent parser). Reuses several protected-raised helpers plus `ColumnGrid`/`KotlinModifierPriority`; writes its own `KotlinDecl` model, statement splitter, parser, group renderer. RDD_KEY_103. |
| 7 | Constructor/function parameter lists | (c), **done** | Same reversed-grammar issue as §6, in `MiscRule.Param`/`Signature`. New `KotlinSignatureRule extends MiscRule`, same pattern as §6. RDD_KEY_104. |
| 7.1 | Named/default arguments (`=` spacing/alignment) | (c), **done for declarations** | Folded into §7's parser/renderer — a default value is one more optional trailing part. **Not covered:** the call-site named-argument shape (`foo(x = 1, y = 2)`) is a function *call*, different token shape — out of `KotlinSignature`'s scope; would need its own small parser/renderer if picked up later. |
| 7.2 | Trailing comma (preserved as-is) | (a), **verified** | No pass adds/strips a trailing comma for any language — trivially satisfied. Confirmed via harness: `fun foo(x: Int,)` round-trips correctly. |
| 8 | Property accessors (`get`/`set`, preserve expression/block form) | (a)/(c), **done for the plain expression-bodied getter shape (RDD_KEY_133); block-bodied/setter/initializer shapes remain preserve-as-written-only** | "Preserve as-is" satisfied by not touching it. `BlockStructureRule.collapseSingleExpressionBlocks`'s `SINGLE_EXPR_KEYWORDS` (`{if, while, for}`) never matches an accessor's block body — confirmed via harness both a block-bodied `set(v) { field = v }` and expression-bodied `get() = computeY()` stay untouched. **Grouping fixed under RDD_KEY_133** for the plain getter-only shape: `parseKotlinAccessorMember`/`renderAccessorGroup` in `KotlinGetterSetterRule.java` merges an adjacent group of `val x: Int` / `get() = expr` two-line units onto one line each, column-aligned. Block-bodied accessors, getter+setter pairs, and initializer+accessor properties remain unhandled but correctly preserved-as-written. See Open Questions. |
| 9 | Expression-bodied functions | (a)/(c), **done, including grouping** | "Preserve as-is" free (same reasoning as §8). Wrap-`= expr`-onto-own-line implemented as `KotlinSignatureRule.FunctionTail`/`parseFunctionTail`/`renderWithTail`, three-tier fallback delegating to §7's `render` for the middle tier. Also fixed a shared-class bug: `MiscRule.isTightToken` was collapsing Kotlin multiplication spacing (`x* x`), gated off for Kotlin. RDD_KEY_112. **Grouping fixed under RDD_KEY_132**: `KotlinGetterSetterRule` column-aligns adjacent one-liner expression-bodied functions. §8's accessor one-liners were a separate gap, resolved by RDD_KEY_133. |
| 10 | `for` loops and ranges | (a)/(c), **done** | Tight/loose paren-padding already generic (`ComplexityPaddingEvaluator`); `in`/`until`/`downTo`/`step` turned out already inert w.r.t. nested-bracket detection with zero code changes (confirmed via harness, reclassified (b)→(a)). `..`/`..<`'s own tight spacing needed new `KotlinSpecificRule.enforceRangeOperatorSpacing`. RDD_KEY_110. |
| 11 | Labeled jumps (`@label` spacing) | (c), **done** | New `KotlinSpecificRule.enforceLabeledJumpSpacing` — a left-to-right state machine telling a jump's `@label` (tight both sides) apart from a declaration's `label@` (tight before, spaced after) apart from an unrelated `@Foo` annotation. RDD_KEY_105. |
| 12 | Destructuring declarations | (c), **done** | LHS is a parenthesized name list (`(a, b) = pair`), not `MiscRule.Assignment`'s single-`target` assumption — implemented in `KotlinDeclarationAlignmentRule.java` (reuses §6 infrastructure). Comma spacing normalized for free as a side effect of rebuilding `lhsText`. RDD_KEY_107; group-stream merge revised under RDD_KEY_126 (merges into the same column-aligned group as an adjacent §6 declaration, C++ structured-bindings precedent). |
| 13 | Generics variance (`in`/`out`) | (b), **done** | `TokenizerCore.GENERIC_SAFE_KEYWORDS` extended — confirmed via harness these were previously misread as comparison `OP` tokens, now correctly angle-bracket tokens; plain comparisons unaffected. No-op for C/C++/Java. Tokenizer-level, no rendering pass needed. RDD_KEY_113. |
| 14 | Generic `where` clause | (c), **done** | Structural analog exists in `CppSpecificRule.java`'s trailing-`requires`-clause handling (a per-language file, not shared) — new `KotlinSpecificRule.enforceWhereClausePlacement`, using the C++ method as a reference pattern. RDD_KEY_106. |
| 15 | Infix functions (modifier slot; call-site spacing) | (a), **verified** | Modifier slot is Step 2 scope. Call-site word-operator spacing is ordinary expression spacing, already left alone by every shared class. Confirmed via harness. |
| 16 | Annotation use-site targets (`@field:` tight `:`) | (c), **done** | No existing annotation-colon handling (Java has no use-site-target shape) — new `KotlinSpecificRule.enforceAnnotationUseSiteTargetSpacing`. RDD_KEY_108. |
| 17 | Lambda-with-receiver / function types (exempt from nesting detector) | (b), **done** | `ComplexityPaddingEvaluator.isLoose` extended to skip a `.`-preceded/`->`-followed `(...)` span rather than counting it as nesting — no-op for C/C++/Java, `make test` 32/32. Known Gap (function type nested as a parameter of another) deliberately unhandled per the style doc. RDD_KEY_109. |
| 17.1 | Lambda parameter arrow spacing | (c), **done** | New `KotlinSpecificRule.enforceArrowSpacing` — flat whole-file single-space-both-sides pass over every `->`, covering §17 and §17.1 together. Excludes `when`-branch arrows via `collectWhenBranchArrowIndices` (owned by §4). RDD_KEY_109. |
| 18 | `vararg` | (a), **verified** | Modifier-slot handling is Step 2 scope; no general spacing concern. Confirmed via harness inert to every brace-style pass. |
| 19 | String templates (preserve `"$x"`/`"${x}"` exactly) | (c) — **tokenizer-level, done** | Confirmed the flagged risk was real (see RDD_KEY_116). Bare `$x` needed no special handling — introduces no nesting risk. Triple-quoted raw strings flagged as a related but out-of-scope gap — see row 19.1. |
| 19.1 | Triple-quoted raw strings (`"""..."""`) | (c) — **tokenizer-level, done** | Investigated on explicit request; confirmed badly broken, not merely unhandled (see RDD_KEY_117). Verified via a 14-case harness (embedded quote runs, multi-line, literal backslash, plain/nested/doubly-nested interpolation, 4-trailing-quotes edge case, unterminated input, plus Java text block / C++ raw string / plain C string sanity checks all untouched). `make test` 32/32 before/after. |
| 20 | Sealed classes/interfaces | (a), **verified** | Normal `class`/`object` K&R rules apply unchanged. Confirmed via harness: `sealed class Result { ... }` gets the same K&R brace + closing comment as a plain `class`. |
| 21 | Type aliases | (a), **verified** | Single-line `=`-spaced statement, no new behavior. Confirmed via harness. |
| 22 | Extension functions | (a), **verified** | `fun` behaves like any other modifier/keyword for spacing. Confirmed via harness: `KotlinSignatureRule.parseKotlinSignature` correctly parses `fun String.reverseWords()` — the receiver-type-before-name shape needs no special-casing beyond §7's existing name-detection. |
| 23 | Known Gaps | (a), excluded | Explicitly out of scope, same posture as STYLE_JAVA.md's own excluded section (RDD_KEY_59). |
| K2.1 | Guard conditions in `when` | (a), **verified** | Extends §4's arrow-alignment logic as-is per the style doc — confirmed via harness `formatWhenExpressions` already handles a guarded branch (`is String if x.isNotEmpty() -> foo()`) correctly with zero code changes, since the guard text is scanned up to the first top-level `->` like any label. Output matched STYLE_KOTLIN2.md §1's worked example byte-for-byte, including multi-guarded-branch alignment. |
| K2.2 | `data object` | (a), **verified** | Formatted exactly like `object` — a *named* `data object Singleton { ... }` is an ordinary named construct with an extra leading modifier, not a headless shape. Confirmed via harness: gets the same blank lines + closing comment as any other named `object`. |
| K2.3 | Other 2.0/2.1 features | (a), excluded | Explicitly "no new formatting rules" in the style doc. |

### Step 2 — `KotlinModifierPriority.java`

- [x] Column order for Kotlin's modifier set (`public/private/protected/
      internal`, `open/final/abstract/sealed`, `override`, `const`,
      `lateinit`, `val`/`var` sharing one slot per STYLE_KOTLIN.md §6) —
      confirmed no cross-declaration-kind conflict analogous to Java's
      `abstract`/`volatile` case (RDD_KEY_83): `const` (properties only),
      `lateinit` (var properties only), `override` (members only), and
      `open`/`final`/`abstract`/`sealed` (mutually exclusive modality) never
      fight over column order across declaration kinds. Implemented as
      `grid/KotlinModifierPriority.java`: columns 0 (visibility) / 1
      (modality, shared) / 2 (`override`) / 3 (`const`) / 4 (`lateinit`) / 5
      (`val`/`var`, shared). Compiles clean standalone; wiring into rule
      classes is Step 3's job.

### Step 3 — `KotlinSpecificRule.java`

- [x] Implemented each "(c)"-flagged section from Step 1's scoping table,
      one per checkpoint commit. §8/§9 one-liner getter/setter grouping: §9
      fixed via `KotlinGetterSetterRule` (RDD_KEY_132), §8 fixed via the
      same class (RDD_KEY_133) — block-bodied/setter/initializer accessor
      shapes remain an open, documented gap (see Open Questions). Every
      other flagged section done.

Implementation/verification narratives in `RDD_LOG.md` (`grep -Fm1
'RDD_KEY_n'`), not duplicated here. Per-section RDD_KEY mapping (see Step 1
scoping table above for full detail): §1 RDD_KEY_115 (supersedes flawed
`b0e778f`); §3.1/§3.4 RDD_KEY_99; §3.2 RDD_KEY_100; §4 RDD_KEY_101
(idempotency fix RDD_KEY_121); §5 RDD_KEY_102; §6
`KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule` RDD_KEY_103;
§7/§7.1 `KotlinSignatureRule extends MiscRule` RDD_KEY_104 (call-site named
args `foo(x = 1, y = 2)` not covered — different shape); §11 RDD_KEY_105;
§14 RDD_KEY_106; §12 RDD_KEY_107, revised RDD_KEY_126 (merges into adjacent
§6 alignment group, C++ structured-bindings precedent, per user request);
§16 RDD_KEY_108; §17/§17.1 RDD_KEY_109 (shared-class change:
`ComplexityPaddingEvaluator.isLoose`); §10 RDD_KEY_110; §2 RDD_KEY_111; §9
RDD_KEY_112 (shared-class change: `MiscRule.isTightToken` gated off for
Kotlin `*`/`&`); §3/§3.3 RDD_KEY_114; §19/§19.1 tokenizer-level RDD_KEY_116,
RDD_KEY_117; §8/§9 one-liner getter/setter grouping — new
`KotlinGetterSetterRule extends GetterSetterRule`, §9 RDD_KEY_132, §8 (plain
expression-bodied `get()`, no initializer, no `set`) RDD_KEY_133.
Block-bodied accessors, getter+setter pairs, and initializer+accessor
properties remain unhandled (preserved as written, not grouped) — see Open
Questions.

### Step 3.5 — Configuration Property Wiring

Pipeline is fully wired (verified live) — `Main.java` `inferLanguage`
auto-detects `.kt`/`.kts`, `Formatter.java` constructs and runs every Kotlin
rule class through the same pipeline as Java/C++. Config uses
`.jxmake-code-formatter`, `key=value` format (not YAML); boolean keys accept
`on`/`off` only, not `true`/`false`.

- [x] `line-length`/`indent-size`/`indent-style`: wired, same behavior as
      Java/C++. See `STATE_COMMON.md` → **Config Keys and Defaults** for the
      full config table (shared across all four languages).
- [x] `closing-comment-min-lines`: wired via shared `BlockStructureRule`.
- [x] `format-macros`: permanent no-op for Kotlin (no preprocessor) — not a gap.
- [x] `line-endings`: language-agnostic, applied post-format in `Main.java`.
- [x] `normalize-comment-start-case`/`normalize-comment-end-period`: wired,
      same cross-language behavior as Java (line comments not end-period-normalized).
- [x] Kotlin import ordering — STYLE_KOTLIN.md §24 spec + implementation:
      `kotlin-import-order`/`-sort`/`-depth`/`-blank-lines` config keys,
      `KotlinSpecificRule.enforceKotlinImportOrdering` (mirrors
      `JavaSpecificRule.enforceImportOrdering`; no `static` bucket — replaced
      by a `kotlin` group, since Kotlin has no `import static` keyword;
      aliased/wildcard imports sort by original qualified name). RDD_KEY_118.
      Not yet wired into `Formatter.formatOne`.
- [x] JXM_CFMT_DIS/JXM_CFMT_ENA + `--format-off`: confirmed working for
      Kotlin, language-generic implementation, no gap.
- [x] README.md/README.txt updated for Kotlin support (config keys, import
      groups, `.kt`/`.kts` detection, stale "not implemented" notes corrected).

### Step 4 — Test Fixtures

**DONE — both `kt_combined_inp.kt`/`kt_combined_out.kt` and
`kt_comments_inp.kt`/`kt_comments_out.kt` fully pass, both enabled in the
`Makefile`.**

**Fixtures are handwritten and may have syntax errors — confirm with the user
as needed.**

`test/kt_combined_inp.kt`/`kt_combined_out.kt` capture STYLE_KOTLIN.md +
STYLE_KOTLIN2.md end-to-end coverage. `test/kt_comments_inp.kt`/
`kt_comments_out.kt` capture uncommon comment locations + JXM_CFMT_DIS/ENA.

`kt_*_inp.kt` = input files; `kt_*_out.kt` = reference output. Methodology:
run the formatter on the input, diff the `/tmp` output against the reference
to find bugs, and also run an idempotency test (output re-run through the
formatter should be unchanged).

- [x] `test/kt_combined_inp.kt` / `kt_combined_out.kt` — **DONE.** Enabled in
      the `Makefile`'s `INP_FILES`; round-trips clean, both forward
      (`inp`→`out`) and idempotency (`out`→`out`) passes. Every punch-list
      item below is resolved except item 2 (`result1`/`result2` fixture
      naming, resolved by the user directly in the fixture, not a formatter
      change) and item 4 (trailing whitespace on the EOF blank line,
      intentional test content, not a bug).
- [x] `test/kt_comments_inp.kt` / `test/kt_comments_out.kt` — **DONE.**
      Enabled in the `Makefile`'s `INP_FILES`. All four bugs found by the
      earlier manual check were root-caused and fixed; round-trips clean,
      both passes. See punch list below. RDD_KEY_131.
- [x] After every fixture addition or shared-class change: full existing
      C/C++/Java suite + new Kotlin fixtures, zero regressions. **`make test`
      36/36** (34 prior C/C++/Java + real_code_regressions fixtures, plus
      `kt_combined_inp.kt` and `kt_comments_inp.kt`, forward and idempotency
      passes both green).

**Step 4 known-bugs punch list** (against `test/kt_combined_inp.kt` /
`kt_combined_out.kt`) — all resolved. Full narratives in `RDD_LOG.md`;
one-line summary each:

1. [x] Missing blank line/closing comment on `class`/`enum class` with a
   primary constructor. RDD_KEY_119 (tokenizer, Kotlin-gated).
2. [x] `for(...) { stmt }` not collapsing (no `;` to count). RDD_KEY_120.
3. [x] `when(status) { ... }` squished/mis-indented — `KotlinDeclarationAlignmentRule.
   parseKotlinDeclaration` stripped newlines from a multi-line
   block-expression initializer. RDD_KEY_121.
4. [x] `fun test(): int` — fixture typo (`int`→`Int`), fixed by the user, not
   a formatter bug.
5. Lines 88-90 (`val result`/`val result` run-together) — fixture ambiguity,
   later fixed by the user (`result1`/`result2` rename), not touched here.
6. [x] Apparent double-indentation of a body's first `val`/`var` — stray
   leftover `/tmp/kt_test/.jxmake-code-formatter` (`indent-size=8`)
   test-harness artifact, not a bug. RDD_KEY_122 fixed a real separate bug
   in the same area (`set(value) { ... }` accessor closing-brace indent).
7. [x] `val safe = ...` spacing/alignment near the `when` fix — two parts:
   missing space in `.let{ }` (RDD_KEY_123, Kotlin-gated
   `DeclarationAlignmentRule.needsSpaceBetween`), and column-alignment with
   the following `val (a, b) = ...` destructuring line (RDD_KEY_126 —
   reverses RDD_KEY_107's "never merged" decision, C++ structured-bindings
   precedent).
8. [x] `if(...) return@X` / `if(...) expr` braceless collapse not firing.
   RDD_KEY_124 (main case), RDD_KEY_127 (bare `else` collapse), RDD_KEY_128
   (collapsed `else` body column-padding, new `KotlinSpecificRule.
   alignBracelessElseWithIf` pass).
9. [x] Explicit-return-type functions (`fun foo(...): Int { ... }`) missing
   STYLE.md §9's blank-line-before-`return`. RDD_KEY_125
   (`MiscRule.isFunctionBodyBrace` didn't recognize Kotlin's `: ReturnType`
   shape; also surfaced/fixed an unrelated C++ `->`-scan ordering bug).

**Step 4 known-bugs punch list** (against `test/kt_comments_inp.kt` /
`kt_comments_out.kt`) — all resolved. Full narrative in `RDD_LOG.md`
RDD_KEY_131; one-line summary each:

1. [x] Missing blank line before a `when` branch led by its own standalone
   comment (`// Success case` after `when(status) {`).
   `KotlinSpecificRule.ensureBlankLineInGap` bailed on any comment anywhere
   in the gap; replaced with `SwitchRule`'s comment-anchored version.
2. [x] Missing blank line before a `return` preceded by its own standalone
   leading comment. Kotlin-only carve-out in `MiscRule.
   insertBlankLineBeforeReturn` (`appendGapWithForcedBlankAfterLastComment`)
   — Java's accepted fixture wants the opposite, so gated `lang.isKotlin`,
   not a shared-class change.
3. [x] Leading blank line inside a function/lambda body not stripped when
   the first statement isn't a `val`/`var` declaration. New
   `KotlinSpecificRule.stripLeadingBlankBeforeNonDeclarationStatement`,
   wired into `Formatter.java`'s Kotlin-only Phase 1 block.
4. [x] Outermost `class Widget` missing its closing blank line + `} //
   class Widget` comment. Root cause: `BlockStructureRule.addClosingComments`
   and `insertNamedConstructBlankLines` both scanned the *entire* block span
   for any frozen (JXM_CFMT_DIS/ENA) token instead of just the boundary gap
   being rewritten — an unrelated frozen region nested inside `findFirstX`
   suppressed the outer class's own blank line/comment. Shared-class fix;
   also fixed a latent identical bug in `test/java_format_toggle_out.java`
   (added its own previously-missing blank line + `} // class FormatToggle`).

### Step 5 — Dogfood / Real-Code Testing

**Status: dogfood tree now compiles clean end-to-end — Step 5's core goal is
met.** Any future session picking this up should treat it as
regression-watching / further polish, not a known-broken state. See
**Finished dogfood / real-code testing** for completed candidates,
**Not started dogfood / real-code testing** for queued/deferred ones, and
**Tools/compiler used** for exact invocations.

- [x] Once Steps 0–4 are complete, apply STATE_COMMON.md's real-code-testing
      methodology (clone a real, compiling Kotlin project → format →
      idempotency check round1 vs round2 → compile-check) — deferred until
      the core checklist above was done, not started speculatively. See the
      candidate lists below for status of every candidate run so far.

**Standalone `K2JVMCompiler` classpath — rejected, do not use.** A bare
`kotlin-compiler-embeddable` + `kotlin-stdlib` classpath cannot syntax-check
an Android/AndroidX candidate: every file under
`gui_frontend_android/app/src/main/java/*.kt` imports `android.*`/AndroidX
APIs, only resolvable via the project's own Gradle build. Use the project's
Gradle wrapper (tool (2) below) for any Android/Gradle candidate.

For `gui_frontend_android`: copy once into a **persistent** location —
`~/Projects/Shadow/rc_gui_frontend_android_DOGFOOD` — not `/tmp`, so it
survives reboots. Never write to `~/Projects/RobotCoding/gui_frontend_android`
itself (read-only); always re-verify a fix against the true pristine
originals there, not just the dogfood copy (a stale copy can hold
already-formatted, pre-fix output and falsely look fixed/unfixed).

**One-time setup after the copy:** edit `gradle.properties` in the dogfood
copy so `project.buildDir=build` (plain relative value — the original points
at the real project's own external build dir, which would still collide even
from a copy). Redo if the dogfood dir is ever deleted and recopied.

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
`android.*`/AndroidX imports without Gradle's dependency graph. `kotlin_syntax_check`
is lighter: parses a `.kt` file to a PSI/AST via
`KotlinCoreEnvironment`/`KtPsiFactory` and reports `PsiErrorElement` nodes
(parse errors) — no semantic/type checking, never resolves `android.*`
imports, so the AndroidX objection doesn't apply. Plain classpath-based
standalone Java program; every needed class is bundled in the single shaded
`~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib/kotlin-compiler.jar` (confirmed via
`unzip -l`, no separate intellij-core/trove4j jars needed).

Tool location: `util/CodingStyle.md/formatter/tools/verifiers/`
(`kotlin_syntax_check.java` + compiled `kotlin_syntax_check.class`; committed, licensed project
tooling, alongside the other jobs' syntax checkers).

Build/run (JDK 21, matches this compiler's class file version 52 = Java 8
target, runs fine on 21):

```bash
JDK=/opt/openjdk-21_linux-x64_bin/jdk-21
KLIB=~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib
cd util/CodingStyle.md/formatter/tools/verifiers
"$JDK/bin/javac" -cp "$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_syntax_check.java
"$JDK/bin/java" -cp ".:$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_syntax_check <file.kt> [file2.kt ...]
```

Exits 0 and prints "OK: no syntax errors" when clean; exits 1 and prints each
`PsiErrorElement`'s description + text range when a parse error is found.
Verified against this project's own `test/kt_combined_out.kt` (passes clean)
and a deliberately corrupted copy with injected stray `}}}` (correctly
reports the right errors at the right offsets).

**Recommended use going forward:** for a quick syntax/parse sanity check on
formatter output, run `kotlin_syntax_check` first — near-instant versus a full Gradle
build. It does NOT replace `./gradlew compileDebugKotlin` for real
dogfood/compile-check testing (no semantic checking, no unresolved-reference
detection) — keep using the Gradle recipe for that. Treat it as a fast
pre-filter / supplement, not a substitute.

Follow STATE_COMMON.md's fixture-registration convention when a bug is found
and fixed here (`test/real_code_regressions_N_{inp,out}.kt`, registered in
`Makefile`'s `INP_FILES`, documented in `test/README.txt`, standard copyright
header) — same precedent as the `indent-size = 2` config-wiring no-op
exception noted there.

**Dogfood Output Validation — `kotlin_content_diff`.** A content-preservation
checker for Kotlin, complementing `kotlin_syntax_check` (which only proves "still
parses", same `java_content_diff`/`css_content_diff.py`/`xml_content_diff.py`
precedent). Reuses `kotlin_syntax_check`'s `KotlinCoreEnvironment`/`KtPsiFactory`
infrastructure (no new dependency), modeled directly on `java_content_diff`'s
design split by content family (`kotlin-import-order` sorting,
declaration-alignment whitespace, `normalize-comment-start-case` are all
legitimate transforms that must not be flagged as corruption):
- **imports** — compared as a multiset (`getImportedFqName()` +
  `isAllUnder()`/alias, sorted) since reordering here is legitimate.
- **every top-level declaration** (`KtFile.getDeclarations()`) — compared
  **in original relative order**, each canonicalized by a hand-rolled
  leaf-token walk (whitespace/comment/KDoc nodes skipped, remaining leaf
  text joined with single spaces) rather than a pretty-printer: unlike
  javac's `Tree.toString()` (which re-synthesizes text from the AST,
  losing original whitespace/comments for free), IntelliJ PSI is a
  lossless concrete syntax tree — `PsiElement.getText()` returns verbatim
  original source — so there is no built-in canonical form to lean on;
  this tool builds its own.
- **comments** (both line/block comments and KDoc blocks) — extracted
  separately, compared as a multiset, whitespace-normalized **and**
  lowercased, so a case-only change is not flagged but a dropped/corrupted
  comment still is.

**Gotcha hit and fixed during verification:** the leaf-token walk and
comment extraction MUST use `ASTNode.getChildren(null)` (via
`PsiElement.getNode()`), not `PsiElement.getChildren()` or
`PsiTreeUtil.findChildrenOfType()`. For stub-based elements (`KtClass`,
`KtProperty`, `KtNamedFunction`), `PsiElement.getChildren()` only returns
structurally significant composite children and silently omits every plain
leaf token — identifiers, keywords, and critically comments — so a
`PsiTreeUtil.findChildrenOfType(file, PsiComment.class)` scan over the
whole file found zero comments even with several clearly present (confirmed
via an ASTNode-level dump showing `BLOCK_COMMENT`/`EOL_COMMENT` nodes
reachable only through `ASTNode.getChildren(null)`, never through the
PSI-level children array). Switching both the canonicalization walk and
comment collection to ASTNode traversal fixed it.

Build/run (same classpath as `kotlin_syntax_check`):
```bash
JDK=/opt/openjdk-21_linux-x64_bin/jdk-21
KLIB=~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib
cd util/CodingStyle.md/formatter/tools/verifiers
"$JDK/bin/javac" -cp "$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_content_diff.java
"$JDK/bin/java" -cp ".:$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_content_diff <original.kt> <formatted.kt>
```
Verified against a hand-crafted good pair (reindentation + import sort +
one comment recapitalization — passes clean) and two bad pairs, a dropped
statement (correctly flagged as "top-level declaration #0 structure/content
differs") and a corrupted comment (correctly flagged as a comment present
in one file's set but not the other's) — all three cases caught correctly,
after the ASTNode-traversal fix above. Test fixtures kept in `/tmp` only
(hand-crafted verification pairs, not registered as permanent `test/`
fixtures).

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
`ANDROID_HOME`/JDK 21 on `PATH`, run against a persistent dogfood copy, never
the original checkout). Used for Android/Gradle candidates needing the real
SDK/AndroidX dependency graph (`gui_frontend_android`).

(3) `kotlin_syntax_check` — PSI-based syntax-only checker, build/run commands above.
Used when a full Gradle build is not wanted/needed (`kotlinx.coroutines`, per
explicit user request) — catches parse errors only, weaker confidence than (2)
(no semantic/type checking).

**Finished dogfood / real-code testing**
1. **RobotCoding `gui_frontend_android`** (Android/Gradle app, 46 `.kt`
   files) — complete, config: default. 9 idempotency bugs, RDD_KEY_134–140
   (trailing-lambda collapse, call-wrapped-initializer misclassification,
   lambda-brace indent anchor, if/else-as-value collapse, `&&`-vs-`&`
   confusion, §9 column-width flap, Allman width-prediction gap). Then
   `./gradlew compileDebugKotlin` found ~50 compile errors across 9 files,
   RDD_KEY_141–144 (return-type-tail misdetection eating a fluent-chain
   call, `@Annotation`-vs-label false positive, `when`-arm `else ->`
   mismatched by braceless-`else` collapse, two unrelated bugs in one
   statement). Final: `BUILD SUCCESSFUL`, zero errors (2 pre-existing
   unrelated deprecation warnings). Verified via tool (2).
2. **`github.com/square/okio`** — core bugs fixed, config `indent-size=2`
   (matches okio's own `.editorconfig`; RDD_KEY_145 confirmed default
   `indent-size=4` produces spurious diffs, not a bug). Fixed: RDD_KEY_146
   (unary minus mis-spacing), RDD_KEY_147 (blank-line signature-tail merge),
   RDD_KEY_148 (stale-prefix over-wrap of a braceless `if`) — fixture `_30`;
   RDD_KEY_150 (missing `===`/`!==` tokenizer entries), RDD_KEY_151
   (do-while trailing `while` misread as loop-start) — fixture `_31`.
   Verified via round1/round2 idempotency diffing + tool (1) against
   `commonMain` only. One bug found but **not** fixed at the time —
   RDD_KEY_149 (later resolved by RDD_KEY_163) — see **Not started** below.
3. **`github.com/Kotlin/kotlinx.coroutines`** — fully closed, config:
   default. Scoped to `kotlinx-coroutines-core`'s `common`+`jvm` source sets
   (163 `.kt` files), tool (3) per user instruction instead of a Gradle
   build. Idempotency: 11 non-idempotent files, resolved across RDD_KEY_154
   (baked trailing-space growth) and RDD_KEY_158–162 (try/catch-as-expression
   brace confusion; a shared named-scope-with-wrapped-header root cause
   across 4 files; a call-argument continuation-indent flap; a dangling
   braceless-`else` mis-anchoring bare `}` drift; a declaration-alignment
   padding-width flap — see RDD table for per-bug detail). Compile-check:
   9 of 163 files had genuine syntax errors, resolved via RDD_KEY_155
   (nesting-unaware block-comment truncation), RDD_KEY_156 (`this@Label`
   spacing), RDD_KEY_157 (a `synchronized(...)` block's statements fused
   with no separators). Verified via round1/round2 diffing + tool (3) only
   (weaker compile-check confidence than `gui_frontend_android`).
4. **`github.com/square/kotlinpoet`** — fully closed, all 4 idempotency
   shapes resolved, config `indent-size=2` (matches kotlinpoet's own
   `.editorconfig`). Initial pass fixed 2 bugs (RDD_KEY_152 stale
   when-branch brace-indent anchor, RDD_KEY_153 Allman-conversion misfiring
   on an expr-bodied function's own trailing-lambda body). A later 125-file
   re-run surfaced a residual 10-file idempotency gap in 4 shapes: Shape 1
   (6 files) via RDD_KEY_163 (also resolved RDD_KEY_149's deferred `okio`
   bug); Shape 2 (`AbstractTypesTest.kt`) via RDD_KEY_164; Shape 3
   (`ReflectiveClassInspector.kt`/`kmAnnotations.kt`) via RDD_KEY_165;
   Shape 4 (`KotlinPoetMetadataSpecsTest.kt`) via RDD_KEY_166 (also fixed
   two masked bugs in `signatureLineIndent` found via a from-clean rebuild).
   Fixtures `_46`–`_49`. `make test`: 68/68 clean-rebuild final. Confirmed
   via a fresh full-125-file re-run, `diff -rq round1 round2` empty.
5. **`github.com/arrow-kt/arrow`** — fully closed, all bugs resolved.
   Functional-programming library, scoped to `arrow-core`'s and
   `arrow-optics`'s `commonMain/kotlin` source sets (63 `.kt` files),
   config `indent-size=2`, compile-checked via tool (3) per explicit user
   instruction instead of the Gradle-copy dance. Compile-breaking bugs:
   RDD_KEY_171 (generic type-parameter bound `:` corrupting angle-bracket
   tracking), RDD_KEY_172 (`val`/`var` declaration wrongly collapsed into a
   braceless `if`), RDD_KEY_173 (annotation `@` sharing its line with a
   function signature rendering as `@ Foo`), RDD_KEY_176 (`Either.kt`'s
   `zipOrAccumulate`: `collapseBracelessBody` fused a trailing-lambda call's
   own multi-line block body with no `;` separators). Idempotency-only
   flaps: RDD_KEY_174 (`RaiseContext.kt`'s `ensureNotNull`, a
   `context(...)`-clause paren misdetected as the real signature's paren),
   RDD_KEY_175 (`Iterable.kt`'s `separateEither`, a `formatWhenExpressions`/
   `addClosingComments` pipeline-ordering bug), and RDD_KEY_177
   (`Comparison.kt`'s `sort2`, the investigation's last open item — an
   unparenthesized depth-0 if-expression used as an entire expression-bodied
   function's whole body wasn't exempted from `collapseSingleExpressionBlocks`,
   plus a sibling bug in its paired bare `else` arm). Fixtures
   `test/real_code_regressions_59`–`_64`. Verified: a fresh full-scope
   63-file reformat is round1/round2 byte-identical (`diff -rq` empty);
   `kotlin_syntax_check` reports 0 syntax errors across all 63 files (started at 5
   before RDD_KEY_171-173, 1 after RDD_KEY_173, 0 after RDD_KEY_176);
   `make test` 88/88 clean, zero regressions throughout.

**Not started dogfood / real-code testing**
1. **`github.com/JetBrains/kotlin`** — IN PROGRESS, not "not started." The
   Kotlin compiler's own source tree (~16k `.kt` files after filters),
   originally queued as a last-resort/stress candidate (similar posture to
   `microsoft/STL`/`llvm-project` in the C++ list) but substantial work has
   since landed. Category 1 (parse errors/corruption) is fully closed.
   Category 2 (idempotency) has buckets D2a (332/334 known-flap files —
   RDD_KEY_214/215/216; the remaining 2 are ordinary D3 instances, not a
   distinct D2a shape) and D4 (~8 files — RDD_KEY_218) both fully closed;
   D1 is now **partially closed** (RDD_KEY_219 — two of at least three
   group-width-recompute-instability sub-shapes fixed, a third explicitly
   left open); D3 still fully open (~334 files total across all buckets,
   with D3 now confirmed to include at least
   `GenerateReleaseNotes.kt`/`TypeBridging.kt` from D2a's own former
   residual). Full diagnosis, tool commands, and per-cluster fix history:
   see "Dogfood: JetBrains/kotlin" section below.

**When a test completes:** move/compact its entry from "Not started" (or its
"In progress" detail) into "Finished dogfood / real-code testing", and add a
new numbered entry to "Tools/compiler used" if a genuinely new tool is
introduced.

---

## Dogfood: JetBrains/kotlin (categorization pass, not yet fixed)

**Status (current, as of the D1 partial fix — RDD_KEY_219):** Category 1
(parse errors/corruption) is **fully closed** — every C1-C6k cluster/shape
fixed, 0 genuine formatter-caused failures remain (2 pre-existing BOM
syntax-checker artifacts, unrelated to the formatter, are the only
remaining `kotlin_syntax_check` hits). Category 2 (idempotency-only) is
**partially closed**: D2a is **fully closed** (332 of 334 known-flap
files — the remaining 2, `GenerateReleaseNotes.kt`/`TypeBridging.kt`, were
re-triaged and found to be ordinary D3 wrap-decision-flap instances, not a
distinct D2a shape); D4 is now also **fully closed** (~8 files,
RDD_KEY_218); D1 is now **partially closed** (RDD_KEY_219, fixture `_169`
— two of at least three group-width-recompute-instability sub-shapes
fixed, a third explicitly left open, see D1 row below); D3 still fully
open. See the "Category
1"/"Category 2" tables below for the current per-cluster/per-bucket
breakdown.
Checkout: `/tmp/jb_kotlin_kt/kotlin-master`
(fresh `.kt`-only tarball extraction from
`codeload.github.com/JetBrains/kotlin/tar.gz/refs/heads/master` — no prior
`/tmp` checkout existed; full git clone skipped in favor of
`tar --wildcards '*.kt'` to avoid the non-Kotlin majority of this huge repo)
— reuse this checkout, do not re-clone. Scope: all `*.kt` under the tarball
root excluding `*/testData/*` (compiler test fixtures, out of scope),
`*/build/*`, `*/resources/*`, anything with `generated` in its name —
16268 files, 128 MB, ~113.6k lines (`/tmp/kt_filelist.txt`).

**Tools/compiler used:** same as this file's general "Tools/compiler used"
section above — JDK `/opt/openjdk-21_linux-x64_bin/jdk-21`, Kotlin compiler
`~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib`, `kotlin_syntax_check` (compile
once with javac against `kotlin-compiler.jar:kotlin-stdlib.jar`, then run
against files — see that section for the exact commands). Formatted via
`--preserve-tree --root <src> --out <dir>` single-JAR-invocation batching
per STATE_COMMON's methodology (round1 then round2 off round1's output),
verified via `kotlin_syntax_check` (round1) and `diff -rq round1 round2`
(idempotency). No `indent-size` override needed — all findings reproduce at
default config.

**Original results (stale, fully superseded by the 2026-07-28 re-triage
below):** 220/16268 files failed `kotlin_syntax_check` (Category 1); 344/16268
were idempotency-only (Category 2), after de-duping 81 files that overlapped
both categories.

### 2026-07-28 Re-triage — fresh full-corpus numbers

Reused the same checkout (`/tmp/jb_kotlin_kt/kotlin-master`, not re-cloned).
True current denominator: **16078** files (the earlier 16268 figure was a
stale recount, exclude rules/corpus unchanged — not worth chasing further).
Batched `--preserve-tree --root <src> --out <dir>` reformat, one JAR
invocation per top-level subdirectory (16 subdirs, ~324s round1/~321s round2
wall-clock). No `indent-size` override — default config.

- **Category 1 (parse errors), fresh baseline: 10 raw `kotlin_syntax_check`
  failures on round1, of which 2 are a pre-existing BOM syntax-checker
  artifact** (`JavaScriptLexerBase.kt`/`JavaScriptParserBase.kt`, under
  `js/js.parser/.../antlr/` — confirmed via `xxd` the `EF BB BF` UTF-8 BOM is
  identical in original and formatted output; the formatter preserves it
  correctly, the syntax checker's own BOM handling is at fault, not the
  formatter — permanent, unrelated to any cluster below), leaving **8
  genuine formatter-caused failures, all now FIXED** (C6k shapes 1-5, see
  table below). Category 1 is now 0/8 genuine failures.
- **Category 2 (idempotency-only), fresh baseline: 334/16078 (2.08%)**
  (`diff -rq round1 round2`) — essentially flat vs. the stale 344/16268
  count despite the intervening C1-C6k fixes (those targeted Category 1
  crash/corruption, not the D1-D4 idempotency-flap families, so no overlap
  expected). See "Category 2" table below for current per-bucket status.

Working files (not committed, `/tmp` only): `/tmp/kt_retriage_round1`,
`/tmp/kt_retriage_round2`, `/tmp/round1_filelist.txt`,
`/tmp/round1_syntax_full.log`, `/tmp/round1_cat1_files.txt`,
`/tmp/c6k_fresh_errors.txt`, `/tmp/kt_retriage_idempotency_diff.txt`,
`/tmp/idem_r1_files.txt`, `/tmp/idem_sample40.txt`,
`/tmp/idem_sample40_diffs.txt`.

### Category 1 — Critical (crash/corruption) — ALL CLOSED (C1-C6k)

| Cluster | Files | Root cause / Fix | Fixture / RDD_KEY |
|---|---|---|---|
| **C1** — own-line comment before a constructor param fused onto the param, deleting it | 1+ | `parseKotlinSignature`'s comma-split moved any comment starting the next param's slice onto the previous param. Fixed via `findLineStartComments`/`findStandaloneComments` + `KotlinParam.leadingCommentOwnLine`. | `real_code_regressions_144.kt` |
| **C2** — `@Annotation` at expression position gets spurious space after `@` | ~22 | `DeclarationAlignmentRuleCore.needsSpaceBetween` lacked the `@`-tight case `MiscRuleCore` already had. Added it. | `real_code_regressions_149.kt` |
| **C3** — multi-statement named-argument lambda body fused, no separators (largest crash cluster) | ~70+ | `MiscRuleCurly.renderCallCandidate`'s bail loop used `splitTopLevelCommas` (no brace-depth tracking), misreading a lambda's own param commas as call-arg separators. New brace-depth-aware `splitTopLevelCommasBraceAware`. | `real_code_regressions_146.kt` |
| **C4** — CLOSED, not a real bug: miscategorized instance of C5, folds into its count (see Open Questions). | — | — | — |
| **C5** — multi-line `when (...)` subject's embedded newlines leaked into the `// when <subject>` closing comment, orphaning trailing tokens | ~15-20 | `formatWhenExpressions`'s subject capture preserved raw newlines. Fixed by collapsing whitespace runs to a single space. Also resolved C4. | `real_code_regressions_148.kt` |
| **C6a** — typed `by`-delegate's type-scan swallowed the whole delegate expression (incl. multi-statement trailing lambda) | 42 | `parseKotlinDeclaration`'s type-token scan only terminated on `=`; a `by`-delegate has none. Fixed with a bail on any top-level `by` keyword. | `real_code_regressions_150.kt` |
| **C6b** — multi-dollar string prefix (`$$`/`$$$`) gets spurious space before the string | 12 | No tight-token rule recognized the `$$`/`$$$`-then-STRING join. New `isDollarRun` carve-out in both `needsSpaceBetween` duplicates. | `real_code_regressions_158.kt`; RDD_KEY_207 |
| **C6c** — nullable callable ref `T?::member` corrupted to `T ?: :member` | 2 files, high density | `MULTI_CHAR_OPS`' `"?:"` match had no lookahead for a following `::`. Added `source.startsWith("?::", pos)` bail. | `real_code_regressions_151.kt` |
| **C6d** — `@Composable (Params) -> Type` function-type parens lose required leading space | 6 | The general "`IDENTIFIER` before `(` is tight" call rule fired before either duplicate's `@`-tight case could. New lookback/lookahead carve-out (confirms annotation name + `->` inside the parens) in both Core duplicates plus a third overlooked copy in `KotlinDeclarationAlignmentRule.renderTokens`. | `real_code_regressions_153.kt` |
| **C6e** — trailing-lambda multi-statement body fused when used as boolean sub-expression inside `if(...)`/`&&`/`\|\|` | 6 | `BlockStructureRule.tryCollapse`/`tryCollapseBraceless` guarded the collapse *body* against embedded multi-line braces but not the *condition*. Reused `containsMultilineNestedBrace` as a condition bail. | `real_code_regressions_159.kt`; RDD_KEY_208 |
| **C6f** — multi-line collapse swallows an embedded `//` comment, corrupting everything after it (largest sub-cluster, C1-grade severity) | ~20, overlapped C6h/C6k | Three independent comment-unaware flattening call sites, each fixed with a `containsLineComment`/`hasLineComment`-style bail returning `null` (leave span untouched) instead of flattening: (1) `parseKotlinParam`'s default-value grid rendering, (2) `tryCollapseBraceless`'s condition render (had no guard, unlike sibling `tryCollapse`), (3) `parseFunctionTail`'s expression-body slice (comment-only pre-check missed `tail.exprTokens`). | `real_code_regressions_156.kt` (fixes 1-2), `real_code_regressions_157.kt` (fix 3) |
| **C6g** — backtick identifier containing literal `(` breaks paren-depth tracking downstream | 4 | Kotlin backtick spans weren't recognized at all in the tokenizer's dispatch loop, so interior `(`/`)` chars were mis-tokenized as real brackets. New `TokenizerCurly.emitKotlinBacktickIdentifier`, opaque-span treatment mirroring JS/TS template literals. | `real_code_regressions_154.kt` |
| **C6h** — `Missing '}'` at EOF in Gradle-plugin tests — folded into C6f, no independent cause | 8 | Downstream symptom of C6f's comment-swallow mechanism; all originally-affected files clean once C6f/C6g landed, no separate fix needed. | covered by `_156`/`_154` |
| **C6i** — multiple one-line interface member declarations fused without separators | 1 | `ScopePipelineCurly.applySignaturePass`'s `: ReturnType` tail detection only bailed on a blank-line run before the next brace, not an ordinary single newline, so a headerless one-liner got swallowed into a later construct's span. New `hasTopLevelNewline` bail, gated off when a depth-0 `=` is present (preserves the legitimate expr-body-with-trailing-lambda shape). | `real_code_regressions_155.kt`; RDD_KEY_206 |
| **C6j** — square-bracket destructuring lambda params `[x, y] ->` lost the space after `{` | 1 | `[` was always-tight (C/C++/Java indexing-shape rule) in both `needsSpaceBetween` duplicates. Narrow `lang.isKotlin && "[" after "{"` carve-out forcing the space (safe — Kotlin has no bracket array-literal syntax). | `real_code_regressions_152.kt` |
| **C6k** — 5/5 shapes fixed, cluster closed. 8 genuine failures (2 remaining from the original 10 + 6 surfacing in the fresh re-triage: `ConeTypeRenderer.kt`, `ClientUtils.kt`, `TypeExpansionUtils.kt`, `KtVisitorTest.kt`, `BenchmarksReport.kt`, `Number2String.kt`, `CompositionTests.kt`, `TypeCommonizerTest.kt`). | 8 | **Shape 1** (multi-statement fusion, `TypeExpansionUtils.kt`/`Number2String.kt`/`TypeCommonizerTest.kt`): two causes — (a) `isSingleStatementBody` only routed Kotlin through the newline-aware helper when `semiCount != 1`, wrongly collapsing multi-statement bodies with one trailing `;`; now always routes Kotlin through it. (b) `parseKotlinSignature` stripped NEWLINEs before detecting a param's multi-line lambda default, letting the inline-fits shortcut fuse it; new pre-strip `containsMultilineNestedBrace` bail. **Shape 2** (`KtVisitorTest.kt`/`BenchmarksReport.kt`): a raw string ending in its own `"` before the closing `"""` produces a 4+-quote run; `skipKotlinRawString` closed at the run's first `"""` instead of its last three. Fixed to scan through the whole run. **Shape 3** (`ConeTypeRenderer.kt`): RDD_KEY_144(A)'s `!is`/`!in` carve-out existed only in `DeclarationAlignmentRuleCore`, missing from `MiscRuleCore` (the path a param default value renders through). Added there too. **Shape 4** (`ClientUtils.kt`): `enforceNullSafetyOperatorSpacing` treated `!!` as unconditionally tight on the right; new `isPostfixNullOpContinuation` narrows it to genuine postfix continuations. **Shape 5** (`CompositionTests.kt`): C6d's fix didn't cover a *nullable* function-type default (`@Composable( () -> Unit )?`) since its lookahead only recognized `->`, not a following `?`. Widened to accept both. | `_163` (Shape 1); `_164` (Shape 2); RDD_KEY_212, RDD_KEY_213, RDD_KEY_209, RDD_KEY_210, RDD_KEY_211; `_160`/`_161`/`_162` (Shapes 3-5) |

**Baseline check:** the 70-file C6b-C6k total was cross-checked against the
raw `kotlin_syntax_check` failure list — 70/72 are confirmed genuine
formatter bugs, the other 2/72 are the pre-existing BOM syntax-checker
artifact (see above), excluded from the 70.

Working files retained in `/tmp` (not committed): `/tmp/c6_remaining.txt`,
`/tmp/round1c_syntax.log`, `/tmp/c6_70.txt`, `/tmp/all_ctx.txt`,
`/tmp/REP2.txt`, `/tmp/round1`, `/tmp/round2`, `/tmp/round1_syntax.log`,
`/tmp/idempotency_diff.txt`, `/tmp/kt_filelist.txt`.

### Category 2 — Idempotency-only, fresh count 334/16078 files (D2a now FIXED — see below)

**2026-07-28 re-triage — fresh per-bucket counts.** Sampling methodology: 334
total differing files (`diff -rq round1 round2` on the fresh full-corpus
round1/round2 pair — see "2026-07-28 Re-triage" above), a uniform random
40-file sample (`shuf -n 40`) manually diffed and bucketed by primary diff
shape (a few files show more than one shape; classified by whichever
dominates that file's diff). Estimates below are the sample's bucket
proportion scaled to the full 334, **not** an exhaustive per-file
classification of all 334 — treat as directional, not exact.

| Cluster | Sample hits (of 40) | Est. files (scaled from sample) | Notes |
|---|---|---|---|
| **D2a.** Chained-fluent-call closing-brace drift (`}.apply {`, `}?.let {`, etc — a span's own `braceIndent`/`spanIndent` read off the volatile physical text of the PRECEDING span's own `}`) | 22 (of 40 D2 hits) | **332 of 334 known idempotency-flap files — FIXED** (RDD_KEY_214/215/216, fixtures `_165`/`_166`/`_167`) | Fix: generalized the existing `isChainedCatchFinally` carve-out (RDD_KEY_158) to any Kotlin `.`/`?.` fluent-chain continuation following a preceding span's own `}` — new `ScopePipelineCurly.isChainedFluentCall`, inherits the preceding span's already-resolved `prevEffectiveSpanIndent` instead of re-deriving from its volatile physical closing text. Root-caused against real corpus file `declarationBuilders.kt`. RDD_KEY_214's own 6-file residual has been fully triaged and closed for 5 of the 6 files: `TestStepBuilder.kt`/`common.kt`/`KaBaseSymbolRelationProvider.kt`/`TopLevelPhases.kt` were a `fun`-with-wrapped-`where`-clause `effectiveSpanIndent`/`signatureLineIndent` gap, not the "isNamedScope excluding fun" guess alone — fixed by RDD_KEY_215 (two causes: `ScopePipelineCurly.headerHasTopLevelWhereClause` + `KotlinSpecificRule.signatureLineIndent`'s boundary-line-anchoring rewrite); `TopLevelPhases.kt`'s one remaining line (a boolean-operator-chained `.any { } || .any { }`, not "unexplained brace drift") was fixed by RDD_KEY_216's `isChainedBooleanOp`. The last 2 files, `GenerateReleaseNotes.kt`/`TypeBridging.kt`, were misclassified by RDD_KEY_214's own text as "D2a-adjacent" — direct diffing shows they are ordinary instances of the separate, already-tracked D3 wrap-decision-flap bucket (a `joinToString`/`.also` call-argument wrap decision flapping between one-line and multi-line across rounds), not a distinct D2a shape at all; left open under D3 below, out of scope for D2a's own closure. `make test`: 214/214 → 215/215 → 216/216 forward + idempotency across both fixes, zero regressions. |
| **D1.** Declaration/accessor column-alignment padding flap (round1 vs round2 disagree on padding width) | 12 | **~100, PARTIALLY FIXED** (RDD_KEY_219, fixture `_169`) | Same family as fixed RDD_KEY_139/162 (group-width recompute instability). Two independent root causes fixed: (1) `KotlinDeclarationAlignmentRule.renderAlignedGroup` rendered surviving (non-excluded) rows as one flat shared-width grid even when an excluded, overflowing, brace-bodied-init row sat in the MIDDLE of the group — round1 aligned across the gap, round2 (once the excluded row hard-breaks parsing as genuine multi-line) split into narrower groups; fixed by rendering surviving rows as maximal contiguous runs. (2) The analogous bug in the shared `ScopePipelineCurly.applyGetterSetterPass` driver for Kotlin's one-liner expression-bodied function/accessor grouping; fixed with a new Kotlin-gated `renderKotlinFilteredRuns` helper, C/C++/Java's flat-group behavior left untouched. **Known remaining gap** (explicitly not fixed, still open within D1): a THIRD sub-shape where the offending member's own solo/raw width already fits under `lineLengthLimit` (so it's never excluded via the raw-length parse-time pre-check that catches (1)/(2)) but the group's own shared-column padding alone pushes it over the limit — `KotlinGetterSetterRule`'s one-liner grouping has no RDD_KEY_162-style budget-aware exclusion mechanism at all. Since D1's ~100-file estimate almost certainly contains an unknown mix of all three sub-shapes, this bucket is PARTIALLY closed, not fully. |
| **D3.** Multi-line-call/condition wrap-decision flap (one line in round1, exploded across multiple lines in round2, or vice versa) | 4 | **~33** (previous ~15-20 estimate for a narrower "multi-param lambda header" sub-shape; fresh sampling shows the wrap-flap bucket is broader than originally scoped — includes plain call-argument and `if(...)` condition wraps, not just lambda headers) | Wrap decision (`enforceCallLineBreaking`-family) isn't stable across passes — round1 and round2 make different one-line-vs-wrapped calls for the same logical content. |
| **D4.** Minor adjacent-closing-brace spacing flap (`) }` vs `)}`) | 1 | **~8 — FIXED** (RDD_KEY_218, fixture `_168`) | Root cause: `BlockStructureRule.collapseBracelessBody` dropped a source-preserved trailing space before an already-braceless body's enclosing `}` via `renderInline`'s no-trailing-whitespace behavior. Reproduced directly against `JsArgumentsImpl.kt`; also surfaced a latent instance of the same bug already baked into pre-existing fixture `real_code_regressions_33_out.kt`, corrected alongside. |

Sample total: 22+12+4+1 = 39 of 40 (the 40th file, `org.w3c.dom.kt`, showed
both a D1 padding flap and a D3 wrap flap in the same diff — counted once
under its dominant D1 shape above, so cluster hit-counts sum to 39/40 files
but the file itself touches 2 buckets). No new bucket shape needed — all 40
sampled files fit one of the 4 existing D1-D4 categories, none required a
5th bucket.

**Current standing (see this file's top "Status" line for the summary):**
Category 1 fully closed (all C1-C6k). Category 2: D2a fully closed
(332/334, RDD_KEY_214/215/216 — see D2a row above); D4 now also fully
closed (RDD_KEY_218, fixture `_168`, ~8 files); D1 now PARTIALLY closed
(RDD_KEY_219, fixture `_169` — two of an unknown-mix of at least three
group-width-recompute-instability sub-shapes fixed, a third explicitly
left open, see D1 row above); D3 remains fully untouched (D3's known count
now includes 2 files reclassified out of D2a's own former residual,
`GenerateReleaseNotes.kt`/`TypeBridging.kt`) — good candidates for a
dedicated future fix session: D1's remaining third sub-shape (needs a
`KotlinGetterSetterRule`-side RDD_KEY_162-style budget-aware exclusion
mechanism) and D3 itself, in roughly that priority order per estimated
file counts above.

**Recommended next step (not done yet):** run `kotlin_content_diff` across
the full 16078-file corpus before further Category-1-family work — it would
surface silent content loss undetectable by the syntax checker and give a
truer denominator. Otherwise, D1's remaining third sub-shape or D3 are the
next-highest-value open items.

## Open Questions

- **C4 — closed, no open question remains.** Was a miscategorized instance
  of C5, not a real bug: both collapse paths always join condition/body with
  a literal `" "`, so a missing separator is impossible by construction.
  Re-verified clean against the fixed JAR once C5's actual bug was patched.
  See the "Dogfood: JetBrains/kotlin" → Category 1 table's C4/C5 rows for
  detail; C4's ~44-file estimate folds into C5's count.
