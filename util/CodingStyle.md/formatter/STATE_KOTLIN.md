# STATE_KOTLIN.md — Kotlin JAR Implementation Tracker

Read `STATE_COMMON.md` first — it has the shared commit/ambiguity/testing
conventions this file assumes. `STATE_C_CPP_JAVA.md` (the other job's file)
is NOT required reading for this one — only `STATE_COMMON.md` is.

---

## Purpose

Tracks implementation of Kotlin support in the deterministic JAR formatter
(`util/CodingStyle.md/formatter/`), per `STYLE_KOTLIN.md` / `STYLE_KOTLIN2.md`.
Kotlin currently has **no** JAR support — `AI_PREAMBLE_FULL.md`'s full-file AI
pass is the only existing workflow for Kotlin files (see `README.txt`). This
file tracks the work to close that gap.

---

## Hard Constraint — Shared Classes

The formatter's tokenizer and several rule classes are **shared across all
languages** (C, C++, Java, and now Kotlin) — they are not per-language files:

```
tokenizer/TokenizerCore.java
grid/ColumnGrid.java
grid/ModifierPriority.java
evaluator/ComplexityPaddingEvaluator.java
rules/DeclarationAlignmentRule.java
rules/BlockStructureRule.java
rules/SwitchRule.java
rules/GetterSetterRule.java
rules/MiscRule.java
ScopePipeline.java
Formatter.java
```

**Any change to one of these files for Kotlin's benefit must not change
behavior for C/C++/Java.** Before and after every such change, re-run the
formatter's full existing test suite (`make test` — all C/C++/Java fixtures
under `test/`) and confirm zero regressions — same discipline STATE_COMMON.md
requires generally, called out here explicitly because shared-class edits are
this job's biggest risk. Record the before/after test count in the commit
message.

Kotlin-only work belongs in new files (see Project Layout below), added
alongside the existing per-language files (`JavaSpecificRule.java`,
`CppSpecificRule.java`) rather than folded into them.

**Before modifying a shared class, grep first — do not read `STATE_C_CPP_JAVA.md`
in full.** Run `grep -Fm1 'ClassName' RDD_LOG.md` (substitute the class or
method you're about to touch) to surface any existing `RDD_KEY_n` decisions
that already explain its shape — e.g. why `TokenizerCore`'s multi-char
operator table is structured the way it is (RDD_KEY_69), or why a rule class
re-derives named-construct-ness from raw tokens instead of trusting one flag
(RDD_KEY_84/85). This is almost always sufficient. Only read
`STATE_C_CPP_JAVA.md`'s Project Layout section specifically (never its
Checklist or full history) if the grep hits don't explain what you're
looking at.

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
| RDD_KEY_93 | Checklist ordering — tokenizer support first, then a `JavaSpecificRule`-style scoping pass, before any `KotlinSpecificRule.java` code |
| RDD_KEY_99 | Kotlin headless named-construct classification (`companion object {}`, anonymous `object [: Super] {}`, `init {}`) — §3.1/§3.4; also fixed a tokenizer bug where `:` wrongly armed the supertype name as the construct name |
| RDD_KEY_100 | Kotlin `when` no-space-before-`(` — §3.2; added `"when"` to `MiscRule.TIGHT_PAREN_KEYWORDS`, no-op for C/C++/Java |
| RDD_KEY_101 | Kotlin `when` expression arrow alignment/closing comment/blank lines — §4; new `KotlinSpecificRule.formatWhenExpressions` (not a `JavaSpecificRule`/`BlockStructureRule` extension — keyword-less branches, non-all-or-nothing block-body alignment, forced blank lines) |
| RDD_KEY_102 | Kotlin null-safety operator spacing (`?.`/`!!` tight, `?:` spaced) — §5; new `KotlinSpecificRule.enforceNullSafetyOperatorSpacing`, flat whole-file pass (no shared class does general expression-level re-spacing) |
| RDD_KEY_103 | Kotlin variable/property declaration alignment — §6; new `KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule` (visibility-loosen-then-extend, superseding an earlier "independent parser" resolution), own statement splitter/parser/renderer for the name-before-type grammar |
| RDD_KEY_104 | Kotlin constructor/function parameter list line-breaking and column alignment — §7/§7.1; new `KotlinSignatureRule extends MiscRule` (same pattern as RDD_KEY_103), own `KotlinParam`/`KotlinSignature` model + `ColumnGrid`-based renderer; also covers §7.2 (trailing comma preservation) |
| RDD_KEY_105 | Kotlin labeled jump / label declaration spacing (`return@label`, `label@`) — §11; new `KotlinSpecificRule.enforceLabeledJumpSpacing`, flat whole-file state machine, same shape as RDD_KEY_102 |
| RDD_KEY_106 | Kotlin generic `where` clause line-breaking/alignment — §14; new `KotlinSpecificRule.enforceWhereClausePlacement`, mirrors `CppSpecificRule.enforceRequiresClausePlacement`; new `KotlinSpecificRule(Lang, int, int)` indent-width-aware constructor |
| RDD_KEY_107 | Kotlin destructuring declarations — §12; new `DestructuringDecl`/`groupDestructuringDeclarations`/`parseDestructuringDeclaration`/`renderDestructuringGroup` in `KotlinDeclarationAlignmentRule.java` (reuses §6/RDD_KEY_103 infrastructure); single pre-rendered `lhsText` cell, no per-component type grid; **originally its own group stream, REVISED under RDD_KEY_126** |
| RDD_KEY_126 | **REVISES RDD_KEY_107.** Merges §12 destructuring alignment into the same column-aligned group stream as an adjacent plain §6 `val`/`var` declaration, per user request citing this codebase's C++ structured-bindings precedent; new merged `Row`/`groupAlignableDeclarations`/`renderAlignedGroup`/`toRow` (old grouping/rendering methods kept, `@deprecated`, still used internally for parsing); `ScopePipeline.applyKotlinDeclarationsPass` simplified to one merged loop |
| RDD_KEY_127 | Bare Kotlin `else\n    stmt` (no condition of its own) never collapsed to one line — distinct gap from RDD_KEY_124 (which is keyed off `if`/`while`/`for`'s own `(...)` condition, never a standalone `else`); new shared `collapseBracelessBody` helper extracted from `tryCollapseBraceless`, plus a dedicated `else` branch in `collapseSingleExpressionBlocks`'s main loop; collapse-to-one-line half fixed, column-padding half left open **(resolved — see RDD_KEY_128)** |
| RDD_KEY_128 | **RESOLVES RDD_KEY_127's column-padding question**, user-confirmed via `kt_combined_inp.kt`: a collapsed single-line `else` body pads to the same column as its preceding single-line `if(...)` branch's body; new standalone, last-running `KotlinSpecificRule.alignBracelessElseWithIf` (line-based, on fully-formatted text) rather than computed at collapse time — an earlier collapse-time attempt was stale by one column since `MiscRule.enforceComplexityPadding`'s `if (`→`if(` tightening runs after the collapse pass |
| RDD_KEY_131 | `test/kt_comments_inp.kt`/`kt_comments_out.kt` — fixed the four remaining bugs (comment-led `when`-branch blank line, comment-led `return` blank line, leading-blank stripping in non-declaration-led bodies, outermost-class closing comment suppressed by an unrelated nested frozen region) and enabled the fixture in the `Makefile`. Full narrative in `RDD_LOG.md`; see Step 4 punch list. Note: RDD_KEY_129/130 belong to unrelated C/C++/Java work in `STATE_C_CPP_JAVA.md` — 131 is the correct next key here. |
| RDD_KEY_108 | Kotlin annotation use-site target `:` spacing — §16; new `KotlinSpecificRule.enforceAnnotationUseSiteTargetSpacing`, small state machine (same shape as §11/RDD_KEY_105); new `USE_SITE_TARGETS` set matched by token text since `delegate` is a soft keyword; `@`-to-target spacing deliberately left unenforced (no codebase precedent) |
| RDD_KEY_109 | Kotlin lambda-with-receiver nesting exemption + arrow spacing — §17/§17.1; **shared-class change** — `ComplexityPaddingEvaluator.isLoose` extended to skip a `.`-preceded/`->`-followed `(...)` span (lambda-with-receiver's invocation parens) instead of counting it as nesting, no-op for C/C++/Java (`make test` 32/32 before/after); new `KotlinSpecificRule.enforceArrowSpacing` + `collectWhenBranchArrowIndices`, flat single-space arrow pass excluding `when`-branch arrows (owned by §4) |
| RDD_KEY_110 | Kotlin `for` loops and ranges — §10; `in`/`until`/`downTo`/`step` reclassified (b)→(a), already inert w.r.t. `ComplexityPaddingEvaluator.isLoose` with zero code changes (verified via harness); new `KotlinSpecificRule.enforceRangeOperatorSpacing`, one-sided sibling of §5/RDD_KEY_102 tightening `..`/`..<` |
| RDD_KEY_111 | Kotlin `enum class` with members blank-line "emphasis" around the mandatory `;` — §2; shared `insertNamedConstructBlankLines` only handles body-open/close blank lines, not the `;` emphasis — new `KotlinSpecificRule.separateEnumConstantListTerminator`, mirroring `JavaSpecificRule.separateEnumConstantListTerminator` (per-language precedent, same as §14/RDD_KEY_106) |
| RDD_KEY_112 | Kotlin expression-bodied functions — §9; new `KotlinSignatureRule.FunctionTail`/`parseFunctionTail`/`renderWithTail`, three-tier inline/params-broken/wrap-`=` fallback delegating to §7's `render` for the middle tier — plus a **shared-class fix**: `MiscRule.isTightToken`'s `*`/`&` tight-token treatment gated off for Kotlin (was collapsing ordinary multiplication spacing, `x* x`) |
| RDD_KEY_113 | Kotlin generic variance (`in`/`out`) — §13; **shared-class fix** — `TokenizerCore.GENERIC_SAFE_KEYWORDS` extended with `"in"`/`"out"` so `reclassifyAngleBrackets` recognizes `Box<out T>`/`Comparable<in T>` as generic `<`/`>` pairs rather than comparisons; no-op for C/C++/Java; tokenizer-level, no rendering pass needed |
| RDD_KEY_114 | Kotlin function/secondary-constructor body Allman-brace conversion — §3/§3.3; new `KotlinSpecificRule.enforceFunctionDefinitionAllmanBraceStyle` (+ helpers), mirroring `JavaSpecificRule`/`CppSpecificRule`'s equivalents but with a much more conservative candidate signal (backward-scan must land on `fun`/`constructor`, since Kotlin has no `new` to rule out trailing-lambda calls); also handles `: ReturnType` between `)` and `{`, and tolerates the tokenizer's non-reclassified plain-`OP` `<T>` after `fun` |
| RDD_KEY_115 | Kotlin semicolon stripping — §1; fixed a real bug in the pre-existing `stripOptionalSemicolons` (`b0e778f`, predating the RDD-log convention) — it only protected the enum-with-members mandatory `;`, silently stripping a deliberate same-line multi-statement `;` too (would merge two statements into one invalid line); rewritten around a single positive-evidence `isTrailingSemicolon` rule (only strip a `;` last-significant on its line), reusing §2's `findEnumConstantListTerminators`; also fixed a stray-trailing-space gap |
| RDD_KEY_116 | Kotlin string template tokenizer risk — §19; **shared-class fix** — `TokenizerCore.emitString`'s naive scan-to-next-`"` misread a nested string inside a `${...}` interpolation (`"${foo("x")}"`) as three tokens instead of one — a real correctness risk (a later spacing pass could insert whitespace inside the literal's text); fixed with a Kotlin-only `skipKotlinString`/`skipKotlinInterpolationBlock`/`skipKotlinChar` path (depth-tracks `${...}` nesting, recurses for nested strings/chars), gated behind `lang.isKotlin`; surfaced triple-quoted raw strings as a related, explicitly out-of-scope gap (row 19.1) |
| RDD_KEY_117 | Kotlin triple-quoted raw string tokenizer support — row 19.1, **shared-class fix**; badly broken before (`"""hello "world" end"""` mis-lexed as five tokens; multi-line raw strings leaked a spurious `NEWLINE` into content); fixed with Kotlin-only `isKotlinRawStringOpener`/`emitKotlinRawString`/`skipKotlinRawString` — no backslash-escape processing (literal `\` by design), greedy termination at first `"""` (matches real Kotlin compiler); `${...}` interpolation extended to recognize a nested raw string too; non-Kotlin paths confirmed untouched |
| RDD_KEY_132 | Kotlin §8/§9 one-liner getter/setter grouping — new `KotlinGetterSetterRule extends GetterSetterRule` (same pattern as RDD_KEY_103/104), own member splitter/parser/3-column grid render; scope limited to expression-bodied one-liner functions (§9) — `get()`/`set()` accessors (§8) remain unhandled; new `test/kt_combined_inp.kt`/`kt_combined_out.kt` `class Accessors` case |
| RDD_KEY_133 | Kotlin §8 property-accessor (`get()`/`set()`) one-liner grouping, the gap RDD_KEY_132 left open — new `parseKotlinAccessorMember`/`isAccessorMember`/`renderAccessorGroup` in `KotlinGetterSetterRule.java`, scoped to a plain no-initializer `val`/`var` immediately followed by a bare `get() = expr`; merges into one line and column-aligns via `ColumnGrid`, mirroring §9's shape; two idempotency bugs found/fixed — a **shared-class fix** (`DeclarationAlignmentRule.needsSpaceBetween`, Kotlin-gated so `get`/`set` are tight against `(`) and a `KotlinDeclarationAlignmentRule.parseKotlinDeclaration` fix (bails rather than swallowing a re-parsed merged line's `get`/`set` into its type-token scan) |
| RDD_KEY_134 | Kotlin Step 5 dogfood testing found a compile-breaking bug: `MiscRule.renderCallCandidate`'s Option 2 (`renderCallPreserveGroups`) collapsed a multi-line trailing-lambda call argument (`Thread({ ...multi-statement body... }, "tcp-reader")`) onto one line with no statement separators — invalid Kotlin (no `;` fallback). **Shared-class fix**, Kotlin-gated: new bail in `renderCallCandidate` when any top-level call argument contains both a newline and a `{` (`containsBrace` helper); a non-gated version broke the pre-existing C++ `real_code_regressions_1` fixture, so the gate is required. New `test/real_code_regressions_17_inp.kt`/`_out.kt`. Fixes 2 of 3 known compile-breaking dogfood cases (`RobotTcpSession.kt`, `WifiStaDialog.kt`'s `postDelayed` callback); `BleDeviceSelectDialog.kt`'s similar callback still showed an idempotency diff, not yet root-caused. |
| RDD_KEY_135 | Follow-up dogfood idempotency bug (`PlayMusicBlock.kt`, §6): `KotlinDeclarationAlignmentRule.spansMultipleLines` treated a call-wrapped-but-single-statement initializer the same as a genuine multi-line block, wrongly bailing a declaration out of its alignment group on a second pass. Fixed with paren/brace-depth-aware newline classification (mirrors `ScopePipeline.hasTopLevelNewline`): bail only on a newline inside a real `{...}` body or at true top level, ignore one strictly inside a call's parens. First attempt (paren-depth only) was too permissive and regressed RDD_KEY_134's fixture, caught by `make test`. New `test/real_code_regressions_18_inp.kt`/`_out.kt`. Resolves 2 more of the 9 dogfood non-idempotent files (`PlayMusicBlock.kt`, `BleDeviceSelectDialog.kt`'s `val filter`); 5 remain. |
| RDD_KEY_136 | Follow-up dogfood investigation (`MainActivity.kt`'s `_checkRecovery()`): closing-brace indentation drift, broken even on a fresh format. Root cause: a trailing lambda argument's `{` opening on a continuation line of a multi-line fluent chain (`.setPositiveButton("Ok") {`), deeper than the chain statement's first line — `ScopePipeline.processScope` derived the lambda body's indent/closing-brace placement from `findParentIndent`'s whole-statement-first-line anchor instead of the brace's own physical line. **Shared-class fix**, Kotlin-gated: new `ScopePipeline.braceLineIndent` helper feeding a new `effectiveSpanIndent`; the named-scope one-liner pre-expansion path unaffected. New `test/real_code_regressions_19_inp.kt`/`_out.kt`. Fixes 2 more (`MainActivity.kt`, `BlePermissions.kt`), confirms `ToolbarActions.kt` shared this root cause — but re-checking against the true pristine original surfaced a different, previously-masked statement-joining-without-separator bug shared with `MainViewModel.kt`; 5 diffs remain. |
| RDD_KEY_137 | Follow-up (`MainViewModel.kt`/`ToolbarActions.kt`'s statement-joining-without-separator bug): a `val`/`var` initializer that's a parenthesized if/else expression (`val display = (if (cond) a else b)`), immediately followed by another statement, was fused onto that statement's line with no separator — compile-breaking. Minimized via six repros: both the wrapping parens AND the if/else together are required. Root cause: `BlockStructureRule.collapseSingleExpressionBlocks`'s main dispatch fires on every `if`/`else` with no notion of Kotlin's if-as-value-expression; a wrapped, braceless expression-position `if` fell into the Kotlin-only braceless-body collapse branch, consuming past the wrapping `)` and eating the following statement's newline. **Shared-class fix**, Kotlin-gated: a running unmatched-`(`/`[`-depth counter refuses to treat `if`/bare `else` as collapsible while depth > 0. Deliberately does not also gate on the preceding token — an early broader attempt regressed `real_code_regressions_18`, reverted to depth-only. New `test/real_code_regressions_20_inp.kt`/`_out.kt`. Fixes the last 2 of the 9 dogfood files (`MainViewModel.kt`, `ToolbarActions.kt`); 4 diffs remain. |
| RDD_KEY_138 | Follow-up (`BlockCanvasView.kt`'s missing-space-before-`&&`): a declaration initializer with a top-level `&&` lost its preceding space (`a > 1&& b`), reproduced on a fresh format. Root cause: `DeclarationAlignmentRule.isTightToken`'s `Token.isRepOp(t, '&')` check, ungated by language (meant for C/C++'s repeated pointer/reference sigils but matching Kotlin's `&&` too); `MiscRule.isTightToken` already carried the correct `!lang.isKotlin` gate — `DeclarationAlignmentRule.isTightToken` was the one copy that never got it. **Shared-class fix**: added the identical gate. New `test/real_code_regressions_21_inp.kt`/`_out.kt`. Fixes `BlockCanvasView.kt`'s last diff; 2 remain (`BlockPalette.kt`'s two diffs). |
| RDD_KEY_139 | Follow-up (`BlockPalette.kt`'s §9 one-liner column-width-flapping diff): adjacent §9 one-liners had different alignment widths round1 vs round2. Root cause: `KotlinGetterSetterRule.parseKotlinOneLinerMember` never got the length pre-check `GetterSetterRule.parseOneLinerMember` already has — a member whose body contains a call a later phase might wrap is width-estimated at grouping time; on a fresh format it's still single-line so it groups/pads with the rest, then the later phase wraps it, leaving stale padding; reformatting the already-wrapped output then excludes it, splitting the run differently. **Shared-class fix**: raised `GetterSetterRule.indentWidth`/`lineLengthLimit`/`hasBreakableCall` to `protected`, ported the length pre-check into `parseKotlinOneLinerMember`. New `test/real_code_regressions_22_inp.kt`/`_out.kt`. Fixes the §9 diff; 1 remains (Allman-brace idempotency gap). |
| RDD_KEY_140 | Follow-up (`BlockPalette.kt`'s last diff, Allman-brace idempotency): an `override fun draw(...) { _drawBlock(...) }` nested inside an anonymous `object : Block() { ... }` stayed K&R round1 (call wrapped internally by a later phase) but flipped to Allman round2. Root cause: `KotlinSpecificRule.isSingleLineBody` never got the width-prediction pre-check `JavaSpecificRule.isSingleLineBody` already has — same "decision made before a later call-wrapping phase runs" shape as RDD_KEY_139, this time for K&R-vs-Allman choice. **Fix**: ported `hasBreakableCall` + an estimated-width pre-check as a **duplicated** local helper (not inherited — sibling, not subclass, of `JavaSpecificRule`), adapted to this class's helper API. A first port compiled but had no effect — `JavaSpecificRule`'s width formula (summing only token text lengths, no allowance for indentation/inter-token spaces) undercounted too small to matter at its shallower call sites but not at this deeper object-nested indentation; corrected the formula (added indent length + 1 space per token). New `test/real_code_regressions_23_inp.kt`/`_out.kt`. `BlockPalette.kt` now fully round1-vs-round2 idempotent. |
| RDD_KEY_141 | `./gradlew compileDebugKotlin` against the fully-formatted dogfood tree failed with ~50 compiler errors across 9 files, contradicting the previously-recorded clean baseline. Root cause (via debug-instrumentation bisection): `ScopePipeline.applySignaturePass`'s Kotlin `: ReturnType` tail detection (`findLastTopLevelCloseParen`) matched any top-level `)` in a span's range with no check that a genuine `:` followed before the brace — so a fluent chain `IDENTIFIER1().IDENTIFIER2 { lambda }` (empty-parens call, then a bare/parenless trailing-lambda call) had its first call's `)` wrongly read as a signature close and the second call's name as a return-type tail, silently deleting `.IDENTIFIER2` on the first format pass (compile-breaking). Confirmed pre-existing (reproduces from commit ad4f746) and on real files `ConnectTypeDialog.kt`/`WifiApDialog.kt` (`.show().also { ... }` → `.show() { ... }`). **Fix**: require `nextSignificantIndex(tokens, realCloseParen)` to be a top-level `:` before accepting the Kotlin return-type-tail branch; else bail. New `test/real_code_regressions_24_inp.kt`/`_out.kt`. `make test`: 43/43 before, 43/43 after. Not yet confirmed whether this resolves all ~50 original compile errors. |
| RDD_KEY_142 | Follow-up (`BlockCanvasView.kt`'s compile errors, after RDD_KEY_141): `class BlockCanvasView @JvmOverloads constructor(...)` corrupted to `class BlockCanvasView@ JvmOverloads constructor(...)`, and `ToolbarActions.kt`'s second of two adjacent `@Volatile private var` declarations similarly corrupted — first-pass parse errors, not idempotency flaps. Root cause: `KotlinSpecificRule.enforceLabeledJumpSpacing`'s `label@` state machine (§11) transitioned into its force-space state for ANY identifier followed by `@`, with no check it was actually a label rather than `@Annotation` after a class name or a preceding statement's trailing identifier. **Fix**: added `isLoopLabelTarget` lookahead requiring `for`/`while`/`do`/`{` after the `@`; applied to both the state transition and the separate tight-spacing decision (fixing only one left the same-line case, e.g. `class Foo @JvmOverloads constructor(...)`, still broken). New `test/real_code_regressions_25_inp.kt`/`_out.kt`. Fixes both files' flagged corruption; `BlockCanvasView.kt`'s other, further-downstream errors not yet re-verified. |
| RDD_KEY_143 | Continued dogfood investigation (`Optimizer.kt`'s compile errors, after RDD_KEY_142): a `when` expression's `else -> { ... }` arm with a multi-statement block body was flattened onto one line with no `;` separators — a first-pass parse error. Root cause: `BlockStructureRule.collapseSingleExpressionBlocks`'s bare-`else` handling (meant only for a real `if`/`else` chain's braceless single-statement body) matched any `else` not immediately followed by `if`/`{`, wrongly matching a `when` arm's `else ->` label too. **Fix**: added an `isWhenArrow` check (token after `else` is `->`) to bail out of the braceless-collapse path, leaving `formatWhenExpressions` to handle it. New `test/real_code_regressions_26_inp.kt`/`_out.kt`. `make test`: 45/45 forward+idempotency. Post-RDD_KEY_142 dogfood compile showed 3 files remaining: `Optimizer.kt` (this bug), `ProgramBuilder.kt` (**correction, see RDD_KEY_144**: unrelated bugs), `XMLSaveLoad.kt` (distinct/unconfirmed shape, line 430) — not yet re-verified against a fresh compile. |
| RDD_KEY_144 | Continued dogfood investigation (`ProgramBuilder.kt`'s compile errors, after RDD_KEY_143): two separate, unrelated bugs in one statement (`it !is _FunctionItem || calledFunctions.contains(it.func.funcName)`). **Bug A**: `DeclarationAlignmentRule.needsSpaceBetween` had no case for Kotlin's `!is`/`!in` negated operators (one tight lexical unit), so the generic KEYWORD-space default corrupted them into `! is`/`! in`; fixed via a Kotlin-gated no-space check. **Bug B**: `MiscRule.enforceCallLineBreaking`'s `renderCallCandidate` used `parseSignature` (a C-style "type name" parser meant to detect real forward declarations) on Kotlin candidates too, so `parseParam`'s heuristic misparsed `it.func.funcName` as a `Type name` pair, inserting a spurious space once wrapped (`it.func. funcName`); fixed by splitting `sig` (drives the existing zero-param bail-out) from a new `sigForRender` (forced `null` for Kotlin, used only for render-path selection) — an initial attempt that nulled `sig` itself broke fixture 22's zero-param declaration, caught by `make test` and corrected. New `test/real_code_regressions_27_inp.kt`/`_out.kt`. `make test`: 47/47 forward+idempotency. |
| RDD_KEY_118 | Kotlin import-ordering implementation — §24 spec now implemented; new `KotlinSpecificRule.enforceKotlinImportOrdering` (+ parser/helpers), mirroring `JavaSpecificRule.enforceImportOrdering` but with no `static` bucket (priority local > kotlin > java/javax > org > com > other) and an import statement ending on optional `;` or NEWLINE/EOF rather than a required `;`; new `kotlin-import-order`/`-sort`/`-depth`/`-blank-lines` keys in `Config.java` mirroring `java-import-*` exactly; verified via a standalone 10-case harness, not yet wired into `Formatter.formatOne` |
| RDD_KEY_145 | Real-code testing against `square/okio` (Step 5's next queued candidate): initial ~43-file round1-vs-round2 diff was mostly a test-methodology mistake, not a bug — testing at the default `indent-size` (4) against a project whose `.editorconfig` specifies 2 produced spurious indentation "corruption"; re-testing with a `.jxmake-code-formatter` override matching okio's own convention dropped the diff to 7 files. No code change. |
| RDD_KEY_146 | `square/okio` real bug #1: unary minus/plus mis-spacing in Kotlin declaration initializers (`val x = -1` → `= - 1`) — `KotlinDeclarationAlignmentRule`'s base `needsSpaceBetween` is strictly pairwise, no notion of unary vs. binary; fixed with a new `KotlinDeclarationAlignmentRule.renderTokens` override + `isUnaryMinusOperand` lookback, plus loosening `DeclarationAlignmentRule.needsSpaceBetween` to `protected` (additive). `make test` 49/49 before/after. |
| RDD_KEY_147 | `square/okio` real bug #2: `ScopePipeline.applySignaturePass`'s Kotlin `: ReturnType` tail detection merged a headerless `expect fun` declaration with a later, unrelated `val ... by lazy { }` across a blank line into one bogus signature+tail. Fixed with a new `hasTopLevelBlankLine` guard bailing the tail match if a blank line separates the candidate `)` from the span's `{`. Shared-class change, not itself Kotlin-gated (surrounding branch already is). `make test` 49/49 before/after. |
| RDD_KEY_148 | `square/okio` real bug #3 (last of the original 7-file diff): a braceless `if (...) { throw ... }` round1-vs-round2 flapped between one line and multi-line-wrapped — `BlockStructureRule.tryCollapse`/`tryCollapseBraceless` rendered the `keyword (` prefix with the original space still present, one char wider than the tightened `keyword(` form a later pass produces, causing `MiscRule.enforceCallLineBreaking`'s length check to over-wrap a line that fits exactly at the 100-char limit in its true final width. Fixed with a new `tightenParenPrefix` helper (own `TIGHT_PAREN_KEYWORDS` copy) called from both collapse methods. Not Kotlin-gated — general cross-language correctness fix. New combined fixture `test/real_code_regressions_30_inp.kt`/`_out.kt` (covers RDD_KEY_146/147/148 together). `make test`: 49/49 before fixture, 50/50 after. |
| RDD_KEY_149 | `square/okio` real bug #4 — **found, not fixed, deferred.** A multi-line Kotlin signature's `ColumnGrid`-padded, trailing-comma-preserved param lines (confirmed correct at `KotlinSignatureRule.render`'s own return point via debug print) arrive in the final written file with padding collapsed to one space and the trailing comma stripped — some downstream re-tokenize/re-normalize step (mechanism not yet identified) mangles already-rendered replacement text. See Open Questions below and RDD_LOG.md for full repro/investigation detail. |
| RDD_KEY_150 | `square/okio` compile-check bug #1 (`kotlinc`, not caught by round1-vs-round2 diffing — broken consistently from the first pass): `TokenizerCore.MULTI_CHAR_OPS` had no `===`/`!==` entry at all, so `next !== this` lexed as two tokens (`!=`, `=`) and got re-spaced into the invalid `!= =`. Fixed by adding `===`/`!==` ahead of their 2-char prefixes. Shared-class change (tokenizer), not gated — inert for C/C++/Java. `make test` 50/50 before/after. |
| RDD_KEY_151 | `square/okio` compile-check bug #2: `BlockStructureRule`'s braceless-collapse dispatch treated a do-while's trailing `while (cond)` the same as a genuine loop-starting `while`, fusing the following unrelated statement onto the same line with no separator. Fixed with a new `isDoWhileTailKeyword` lookback gating the `while` dispatch entry. Not Kotlin-gated but confirmed inert for C/C++/Java (they always have a `;` right after a do-while's `)`, never reaching the Kotlin-only braceless branch). New combined fixture `test/real_code_regressions_31_inp.kt`/`_out.kt` (with RDD_KEY_150). `make test`: 50/50 before fixture, 51/51 after. |
| RDD_KEY_152 | `square/kotlinpoet` real-code testing (idempotency bug): a nested `when { ... }` used as a `when` branch's own body had its closing `}`'s indentation flap across rounds — `ScopePipeline.braceLineIndent` (RDD_KEY_136) anchored on the brace's physical line at Phase 0, before `KotlinSpecificRule.formatWhenExpressions`' Phase 4 arrow-alignment pass merges the branch label and the nested `when {` onto one line. Fixed with a new `findMergingWhenBranchLineStart` lookahead anchoring on the eventual post-merge line up front. Kotlin-gated shared-class change. New `test/real_code_regressions_32_inp.kt`/`_out.kt`. `make test`: 51/51 before, 52/52 after. |
| RDD_KEY_153 | `square/kotlinpoet` real-code testing (compile-check bug, `kotlinc` against `jvmMain`): `KotlinSpecificRule.findSignatureCloseParenBeforeBrace`'s backward scan for a `: ReturnType` clause had no bail-out on a depth-0 `=`, so an expression-bodied function whose body is itself a trailing-lambda call (`fun addTypes(...): T = apply { ... } as T`) had `apply`'s own `{` wrongly Allman-converted as the function's own body brace, splitting `apply` from `{ ... }` with no valid Kotlin grammar joining them. Fixed with a new depth-0-`=` bail-out. Kotlin-only file, no cross-language risk. New `test/real_code_regressions_33_inp.kt`/`_out.kt`. `make test`: 52/52 before, 53/53 after. |
| RDD_KEY_154 | `Kotlin/kotlinx.coroutines` idempotency bug: `KotlinSignatureRule.renderWithTail`'s `exprStr` baked a trailing space onto `"= "` even when `tail.exprTokens` was empty (a `{`-led lambda-literal expression body, left unconsumed by the tail parser by design) -- since the untouched remainder downstream keeps its own original leading whitespace, this stacked an extra space on every re-format (unbounded growth, non-idempotent). Fixed by rendering bare `"="` when `exprTokens` is empty. New `test/real_code_regressions_37_inp.kt`/`_out.kt`. `make test`: 56/56 before, 57/57 after. |
| RDD_KEY_155 | `Kotlin/kotlinx.coroutines` compile-check bug #1 (`kotlin_sc` syntax-only tool): a KDoc code example containing its own literal `/* ... */` snippet is valid Kotlin (block comments nest, unlike C/C++/Java) but `TokenizerCore.emitBlockComment` closed the outer doc-comment at that inner `*/`, corrupting/truncating everything after (`Guidance.kt`, ~330 lines dropped). Fixed with Kotlin-gated nesting-depth tracking; C/C++/Java unchanged. New `test/real_code_regressions_38_inp.kt`/`_out.kt`. `make test`: 57/57 before, 58/58 after. |
| RDD_KEY_156 | `Kotlin/kotlinx.coroutines` compile-check bug #2 (`kotlin_sc`): a qualified-this label reference (`this@Label`) had a space wrongly inserted before `@` since `KotlinSpecificRule.enforceLabeledJumpSpacing`'s §11 state machine had no case for the `this` keyword (only jump keywords/plain identifiers). Fixed with new `AFTER_THIS_KEYWORD`/`AFTER_THIS_AT` states, tight through the label with no forced trailing space (unlike a jump's `@label`). New `test/real_code_regressions_39_inp.kt`/`_out.kt`. `make test`: 58/58 before, 59/59 after. |
| RDD_KEY_157 | `Kotlin/kotlinx.coroutines` compile-check bug #3, `LimitedDispatcher.kt`'s last remaining `kotlin_sc` error (root-caused this session via a minimal repro extracted from the file itself + temporary phase-bracketing debug prints in `Formatter.formatOne`, not the previous session's static reasoning): `obtainTaskOrDeallocateWorker()`'s `while (true) { when (...) { null -> synchronized(lock) { stmt; stmt; stmt } ... } }` had the `synchronized` block's three statements fused onto one line with no separators. Root cause: `BlockStructureRule.isKotlinSingleStatementBody` correctly identifies the `while`'s sole body statement (the `when` expression) as syntactically one statement, but has no notion that statement can itself own a nested, genuinely multi-line `{...}` block -- `tryCollapse`'s `renderInline` flattens all whitespace/newlines in the approved span with no brace-depth awareness, silently fusing that unrelated nested block's own statements too. **Fix**: new `containsMultilineNestedBrace` helper (bails whenever a nested `{...}` contains a NEWLINE at brace-depth > 0), called from `isKotlinSingleStatementBody`; inert for C/C++/Java (only gates the Kotlin-only branch). New `test/real_code_regressions_40_inp.kt`/`_out.kt`. `make test`: 60/60 before and after. Narrow re-check: `kotlin_sc` against just the real `LimitedDispatcher.kt` confirmed clean post-fix. Closes the `kotlinx.coroutines` compile-breaking investigation; the 10-file idempotency-diff open question remains open (out of scope this session). |
| RDD_KEY_158 | Continued `Kotlin/kotlinx.coroutines` idempotency investigation, `SystemProps.kt`'s try/catch-as-expression brace-style bug (one of the 10 files RDD_KEY_157 left open): `KotlinSignatureRule`'s function-tail merge collapses a multi-line signature (`fun systemProp(\n  propertyName: String\n): String? =\n  try {`) onto one physical line inside `ScopePipeline.processScope`'s own pass, correctly re-deriving the `try` span's own indent against the new, shallower line -- but the following `catch (...) {` span's `{` still sits on its ORIGINAL pre-merge physical line, one level deeper, so `braceLineIndent` read a stale indent for `catch`'s scope with nothing to correct it, so the two rounds disagreed on its closing-brace placement. **Fix**: `ScopePipeline.processScope`'s span loop now tracks the immediately preceding span's resolved indent/close-brace index; a `catch`/`finally` span found directly chained onto that preceding span's own `}` (same adjacency `BlockStructureRule.placeCatchFinallyOnOwnLine` already tests for) inherits that indent instead of deriving its own. Kotlin-gated, inert for C/C++/Java. New `test/real_code_regressions_41_inp.kt`/`_out.kt`. `make test`: 60/60 before, 61/61 after. Narrow re-check confirmed `SystemProps.kt` itself is now clean, and incidentally so are `Select.kt`/`DebugProbesImpl.kt` (not independently investigated). 7 of the 10 files remain open -- see Open Questions. |
| RDD_KEY_159 | Continued `Kotlin/kotlinx.coroutines` idempotency investigation, category 1 (closing-brace/closing-comment indent drift, `AbstractSharedFlow.kt`/`ConcurrentLinkedList.kt`/`ThreadSafeHeap.kt`/`ExceptionsConstructor.kt`): root-caused via the standalone `ThreadSafeHeap.kt` repro left at the prior session's pause point -- `ScopePipeline.processScope`'s `effectiveSpanIndent` selection preferred `braceIndent` (the physical line the span's own `{` sits on) over `spanIndent` (the construct's own header/start-column indent) whenever `braceIndent` was deeper, correct for RDD_KEY_136's UNNAMED trailing-lambda-at-end-of-fluent-chain shape but wrong for a NAMED construct (class/fun/object) whose header wraps across multiple lines for unrelated reasons (here, a long generic `where` clause wrapped by `enforceWhereClausePlacement`/RDD_KEY_106) -- a named construct's body must always indent relative to its own header, never a wrapped continuation line. **Fix**: `effectiveSpanIndent` now forces `spanIndent` whenever `isNamedScope` is true, regardless of `braceIndent`. Kotlin-relevant in practice (`braceIndent` is `null` for non-Kotlin languages). New `test/real_code_regressions_42_inp.kt`/`_out.kt`. `make test`: 61/61 before, 62/62 after. Resolves 4 of the 7 remaining files (all of category 1). 3 files remain open, unrelated shapes: `BufferedChannel.kt` (bare `}` drift inside an unnamed nested lambda chain), `ChannelFlow.kt` (declaration-alignment padding-width flap), `JobSupport.kt` (call-argument continuation-line indent flap). |
| RDD_KEY_160 | Continued `Kotlin/kotlinx.coroutines` idempotency investigation, `JobSupport.kt`'s call-argument continuation-line indent flap (1 of the 3 files RDD_KEY_159 left open): root-caused via a temporary env-gated debug print in `MiscRule.renderCallCandidate` on the real file -- `enforceCallLineBreaking` (Phase 1) computes a call candidate's base indent from its own physical line before `KotlinSpecificRule.formatWhenExpressions` (Phase 4) unconditionally merges a keyword-less `when` branch's label/arrow/body onto one line (RDD_KEY_101/§4), so a call that IS such a branch body reads a stale, one-level-deeper pre-merge indent on a fresh format -- same "physical-line-anchored decision invalidated by a later merge" family as RDD_KEY_136/152/158/159, this time in `MiscRule` rather than `ScopePipeline`; directly relevant to (but not itself a fix for) the still-open `square/kotlinpoet` `CodeWriter.kt` continuation-indent question below, which theorized this same method/family as the likely culprit. **Fix**: new `MiscRule.effectiveCallBaseIndent`, mirroring `ScopePipeline.findMergingWhenBranchLineStart`'s detection shape -- uses the preceding line's indent instead of the candidate's own whenever that preceding line ends (modulo whitespace) with a top-level `->`; Kotlin-gated, no-op for C/C++/Java. New `test/real_code_regressions_43_inp.kt`/`_out.kt`. `make test`: 62/62 before, 63/63 after. Confirmed `JobSupport.kt` itself now round1-vs-round2 clean; the other 8 previously-tracked `kotlinx.coroutines` files re-checked and unaffected (7 still clean, `BufferedChannel.kt`/`ChannelFlow.kt` still diverge, unrelated shapes). |
| RDD_KEY_161 | Continued `Kotlin/kotlinx.coroutines` idempotency investigation, `BufferedChannel.kt`'s bare `}` closing-brace drift (1 of the 2 files RDD_KEY_160 left open), confirmed unrelated to RDD_KEY_159's named-scope fix since the surrounding scopes are all unnamed lambda bodies: root cause is in `ScopePipeline.findParentIndent`, not `processScope` -- `splitTopLevelSpans` extracts a braceless `if (...) { ... } else expr` statement's braced `if`-branch as its own top-level span, leaving the braceless `else expr` as dangling leading text at the START of the NEXT real span rather than a genuine statement; `findParentIndent`'s backward scan (bounded by that next span's own `span.start`) defaulted its anchor onto this dangling `else`, which only returned a real (wrong) indent once a LATER phase's re-alignment happened to put it alone on its own physical line (round2 only, explaining the idempotency-only nature of the bug). **Fix**: when the anchor found is itself a dangling `else`/`catch`/`finally` keyword, skip forward past its one-line body to the next real statement before deriving the indent; Kotlin-gated (`lang.isKotlin`). A broader statement-boundary heuristic (any depth-0 NEWLINE followed by a declaration-starter keyword) was tried first, also fixed this file, but regressed `real_code_regressions_33` (an unrelated, deliberately misindented nested `interface` header) -- reverted in favor of the narrower dangling-keyword-only fix. New `test/real_code_regressions_44_inp.kt`/`_out.kt`. `make test`: 63/63 before, 64/64 after. `ChannelFlow.kt` remains open, unrelated shape (declaration-alignment padding-width flap, not a closing-brace issue). |
| RDD_KEY_163 | **RESOLVES `square/kotlinpoet`'s Shape 1 idempotency gap (6 of the 10 open files)** -- two distinct `MiscRule.enforceCallLineBreaking` bugs: Bug A (`MemberSpecHolder.kt`) fixed via new depth-aware `effectiveLineEndIndex` (a nested already-wrapped call inside an expression-bodied signature's untouched tail text was undercounting the true line width on a reformat, flipping the wrap verdict); Bug B (`CodeWriter.kt`/`LambdaTypeName.kt`/`ParameterizedTypeName.kt`/`TypeVariableName.kt`/`WildcardTypeName.kt`, also **resolves RDD_KEY_149's originally-deferred `square/okio` bug**) fixed via new `isKotlinReturnTypeThenBlockBody` lookahead extending the true-signature exemption to a `: ReturnType {` tail (previously only a bare `{` was recognized), stopping this pass from re-wrapping an already-correctly-rendered signature as a plain untyped call and discarding its column padding/trailing comma. New `test/real_code_regressions_46_inp.kt`/`_out.kt`. `make test`: 65/65 before, 66/66 after. All 6 Shape-1 files + a fresh full-125-file re-run confirmed clean; 4 files remain open (Shapes 2/3/4, unrelated). |
| RDD_KEY_164 | **RESOLVES `square/kotlinpoet`'s Shape 2 idempotency gap** (`AbstractTypesTest.kt`'s `Parameterized<...> where ...` headerless generic class): `KotlinSpecificRule.enforceWhereClausePlacement` derived the wrapped `where` clause's base indent from `where`'s own current physical line, which on a reformat is already the previously-wrapped line -- one level deeper than the true signature indent, compounding an extra indent level per round. **Fix**: new `signatureLineIndent` helper mirroring `ScopePipeline.findParentIndent`'s "true statement start" posture (backward scan tracking paren/bracket/angle-bracket depth to the nearest depth-0 `;`/`}`/`{`) instead of `lineStartIndex`'s "back up one physical line", which lands on a generic parameter list's own continuation line once already wrapped. New `test/real_code_regressions_47_inp.kt`/`_out.kt`. `make test`: 66/66 before, 67/67 after. Confirmed clean against the real `AbstractTypesTest.kt` file. Shapes 3/4 remain open. |
| RDD_KEY_165 | **RESOLVES `square/kotlinpoet`'s Shape 3 idempotency gap** (2 files: `ReflectiveClassInspector.kt`'s subject-form `when(kotlin) { ... }`, `kmAnnotations.kt`'s plain trailing-lambda call `buildCodeBlock { ... }`, both used as a `when` branch's own multi-line body): `ScopePipeline.findMergingWhenBranchLineStart` (RDD_KEY_152) only recognized a bare `when {` line as the shape `KotlinSpecificRule.formatWhenExpressions`'s later arrow-merge pass folds onto the preceding branch-label line -- generalized the lookahead to accept an optional parenthesized subject after `when`, and a plain call-head identifier (not just the `when` keyword). New `test/real_code_regressions_48_inp.kt`/`_out.kt`. `make test`: 67/67 before, 68/68 after. Confirmed clean against both real files plus a fresh full-125-file re-run (only Shape 4 remains). |
| RDD_KEY_162 | **RESOLVES RDD_KEY_161's last open question**, `ChannelFlow.kt`'s declaration-alignment padding-width flap -- the final file of the 10-file `kotlinx.coroutines` idempotency investigation. `KotlinDeclarationAlignmentRule.renderAlignedGroup` padded typeless `countOrElement` out to match sibling `emitRef`'s type-column width, widening `emitRef`'s own line enough that a later pass wrapped its brace-bodied lambda initializer; re-parsing the resulting multi-line initializer next pass correctly bailed `emitRef` out of its group via `spansMultipleLines`'s brace-depth check, collapsing `countOrElement`'s padding -- an inverted instance of the "later phase invalidates an earlier phase's physical-shape decision" family (RDD_KEY_136/152/158/159/160/161), here the *early* alignment phase destabilizing a *later* wrap phase. **Fix**: indent-width/line-length-limit awareness plumbed into `renderAlignedGroup` (new indent-width-aware constructor, `DeclarationAlignmentRule.lineLengthLimit` loosened protected, `ScopePipeline` threads `depth` through the declarations pass); a fixed-point loop excludes a row from the shared column grid only when its group-aligned width overflows the budget AND its initializer is brace-bodied (new `hasBraceBodiedInit` -- the only shape `spansMultipleLines` can ever bail on; a plain call/expression initializer, no matter how long, is never excluded since any future wrap of it lands inside parens only, already handled idempotently by RDD_KEY_135's carve-out -- confirmed via `real_code_regressions_18`, which an overflow-only or solo-width-only first attempt wrongly regressed). New `test/real_code_regressions_45_inp.kt`/`_out.kt`. `make test`: 64/64 before, 65/65 after. All 9 previously-fixed files re-verified clean; `ChannelFlow.kt` itself now round1-vs-round2 clean. Closes the entire `kotlinx.coroutines` idempotency investigation. |

---

## Checklist

### Step 0 — Tokenizer Support (shared file, additive only)

**Critical rule for this step:** `TokenizerCore.java` is shared with C/C++/Java.
Every addition here must be additive (new keyword/operator recognition) and
must not change how any existing C/C++/Java token is lexed. Re-run the full
existing test suite after this step, before moving to Step 1.

- [x] Survey `STYLE_KOTLIN.md`/`STYLE_KOTLIN2.md` for every token not already
      lexed correctly by `TokenizerCore.java`. Added to `MULTI_CHAR_OPS`: `?.`,
      `?:`, `!!`, `..<`, `..` (longest-prefix-first: `..<` before `..`, same
      requirement as the existing `...`/`->*` ordering). `->` already existed
      and is reused as-is for Kotlin's lambda/function-type/`when` arrow. `@`
      in labeled jumps needs no new operator entry — it falls through to
      `emitOperator`'s single-char fallback, sufficient since the surrounding
      spacing rule is a Step 3 `KotlinSpecificRule` concern.
      **Found and fixed a real bug in the process:** `emitNumber()`
      unconditionally consumed every `.`, so `1..10` lexed as one bogus
      `NUMBER` token (and `1..<10` as `NUMBER "1.."` + `OP "<"` + `NUMBER
      "10"`) instead of `NUMBER "1"` + a range `OP`. Fixed by stopping number
      consumption when a `.` is followed by another `.` — safe for all four
      languages (a decimal point is never followed by a second `.`). Verified
      via `TokenizerCore` dump and `make test` (25/25 unaffected).
- [x] Add a Kotlin keyword set (`KEYWORDS_KOTLIN`), parallel to
      `KEYWORDS_JAVA`/`KEYWORDS_CPP` — includes all hard keywords plus the
      modifier/soft keywords in the checklist's original "at minimum" set
      (unconditionally reserved, same simplification as Java's `var`/`record`).
- [x] Add Kotlin named-construct detection (`NAMED_CONSTRUCT_KOTLIN` =
      `class`, `object`, `interface`, `enum`, `init`). Deliberately did not
      special-case `companion object`/`enum class` or verify
      `computeConstructName()`'s lookback window per shape yet — that
      cross-check is Step 1's job, not this one's.
- [x] Re-run full existing C/C++/Java test suite. **25/25 pass, zero
      regressions** (24 pre-existing + `real_code_regressions_13`, added the
      same session before this Kotlin work started).
- [x] **Follow-up (Step 1's §13 cross-check):** added `"in"`/`"out"` to
      `GENERIC_SAFE_KEYWORDS` so `reclassifyAngleBrackets` correctly
      recognizes declaration-site variance (`Box<out T>`, `Comparable<in T>`)
      as a generic `<`/`>` pair rather than a comparison. No-op for
      C/C++/Java. `make test` 32/32 before/after. RDD_KEY_113.
- [x] **Follow-up (Step 1's §19 cross-check):** added a Kotlin-only
      interpolation-aware string scan (`skipKotlinString`/
      `skipKotlinInterpolationBlock`/`skipKotlinChar`) inside `emitString()`,
      gated behind `lang.isKotlin` — the shared naive scan-to-next-`"`
      misread a nested string inside `${...}` (`"${foo("x")}"`) as three
      tokens instead of one, confirmed via harness. Depth-tracks `${...}`'s
      own `{`/`}` nesting and recurses for nested string/char literals,
      arbitrarily deep. Non-Kotlin scan untouched. `make test` 32/32
      before/after. RDD_KEY_116.
- [x] **Follow-up (row 19.1, investigated on explicit request):** added
      Kotlin-only raw-string support (`isKotlinRawStringOpener`/
      `emitKotlinRawString`/`skipKotlinRawString`), checked before the
      plain-`"` and C/C++ raw-string-prefix branches. Confirmed via harness
      the naive path was badly broken: `"""..."""` mis-lexed into multiple
      STRING/IDENTIFIER tokens, a multi-line one leaked a spurious `NEWLINE`
      into its content. No backslash-escape processing (literal `\` by
      design); terminates greedily at the first `"""`, matching the real
      Kotlin compiler. `skipKotlinInterpolationBlock` extended to recognize a
      nested raw string inside an interpolation. Non-Kotlin paths confirmed
      untouched. `make test` 32/32 before/after. RDD_KEY_117.

### Step 1 — Scoping Pass (mirrors `JavaSpecificRule.java`'s own scoping, RDD_KEY_59)

- [x] Cross-check every section of `STYLE_KOTLIN.md`/`STYLE_KOTLIN2.md`
      against the already-COMPLETE shared rule classes (`DeclarationAlignmentRule`,
      `BlockStructureRule`, `SwitchRule`, `GetterSetterRule`, `MiscRule`) to
      determine, per section: (a) already satisfied as-is once Step 0 lands,
      (b) satisfied by a small additive shared-class extension, or (c) needs
      a new method in `KotlinSpecificRule.java`. Table below.
- [x] Flag anything requiring a change to already-COMPLETE shared-class
      *behavior* (not just an addition) — see **Open Questions**:
      `DeclarationAlignmentRule`'s `Declaration` model assumes C/Java's
      `[modifiers] Type name [= init]` order, structurally reversed from
      Kotlin's `[modifiers] val/var name : Type [= init]`. Stopped here
      rather than guessing a direction.

**Scoping table** (section numbers match `STYLE_KOTLIN.md`; `K2.N` = `STYLE_KOTLIN2.md` §N):

| § | Topic | Outcome | Notes |
|---|---|---|---|
| 1 | Semicolons (strip optional `;`) | (c), **done** | No shared class strips statement-terminating `;` for any language — Kotlin-only `KotlinSpecificRule.stripOptionalSemicolons`. See RDD_KEY_115 for the rewrite from an earlier flawed version. |
| 2 | `enum class` with members | (a)/(c), **done** | `BlockStructureRule.classifyNamed`'s "keyword before `class` is `enum`" check already labels the closing comment for free once `enum`/`class` are Kotlin keywords. Body-open/close blank lines already free via `insertNamedConstructBlankLines`. Blank-line emphasis around the entry-list-terminating `;` needed new `KotlinSpecificRule.separateEnumConstantListTerminator`, mirroring the Java equivalent. RDD_KEY_111. |
| 3 | Brace style (Allman fn bodies / K&R everything else) | (a) K&R direction **verified**; (c) Allman-conversion direction **done** | `BlockStructureRule.qualifiesForKAndR`'s keyword sets already cover Kotlin's control-flow vocabulary — confirmed via harness. Function-body K&R→Allman conversion needed new `KotlinSpecificRule.enforceFunctionDefinitionAllmanBraceStyle`, with a more conservative candidate signal than Java/C++ since Kotlin's trailing-lambda call syntax is token-shape-identical to a function body brace and there's no `new` to rule ordinary calls out; requires a backward scan landing on `fun`/`constructor`. Also handles `: ReturnType` between `)` and `{`, and a one-liner body staying K&R (RDD_KEY_75/RDD_KEY_89 exception). RDD_KEY_114. |
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

- [x] Implement each section flagged "(c)" in Step 1's scoping table, one
      section at a time, each as its own checkpoint commit. §8/§9 one-liner
      getter/setter grouping: §9 fixed via `KotlinGetterSetterRule`
      (RDD_KEY_132), §8 fixed via the same class (RDD_KEY_133) —
      block-bodied/setter/initializer accessor shapes remain an open,
      documented gap (see Open Questions). Every other flagged section done.

Full implementation/verification narratives for every item below are recorded
in `RDD_LOG.md` (`grep -Fm1 'RDD_KEY_n'`), not duplicated here.

- [x] §1 Semicolons — `KotlinSpecificRule.stripOptionalSemicolons`. RDD_KEY_115
      (supersedes an earlier flawed version, `b0e778f`).
- [x] §3.1/§3.4 Class/Object/Companion Object/`init` bodies. RDD_KEY_99.
- [x] §3.2 `when` no space before `(`. RDD_KEY_100.
- [x] §4 `when` expression (arrow alignment, closing comment, blank lines).
      RDD_KEY_101; idempotency bug fixed under RDD_KEY_121.
- [x] §5 Null-safety operators (`?.`/`!!` tight, `?:` spaced). RDD_KEY_102.
- [x] §6 Variable/property declaration alignment — `KotlinDeclarationAlignmentRule
      extends DeclarationAlignmentRule`. RDD_KEY_103.
- [x] §7/§7.1 Constructor/function parameter lists, named/default arguments —
      `KotlinSignatureRule extends MiscRule`. RDD_KEY_104. Call-site named
      arguments (`foo(x = 1, y = 2)`) not covered — different shape.
- [x] §11 Labeled jumps (`@label` spacing). RDD_KEY_105.
- [x] §14 Generic `where` clause. RDD_KEY_106.
- [x] §12 Destructuring declarations. RDD_KEY_107, revised under RDD_KEY_126
      (now merges into the adjacent §6 alignment group, C++
      structured-bindings precedent, per user request).
- [x] §16 Annotation use-site targets. RDD_KEY_108.
- [x] §17/§17.1 Lambda-with-receiver/function-type nesting exemption + arrow
      spacing. RDD_KEY_109 (shared-class change: `ComplexityPaddingEvaluator.isLoose`).
- [x] §10 `for` loops and ranges. RDD_KEY_110.
- [x] §2 `enum class` with members. RDD_KEY_111.
- [x] §9 Expression-bodied functions. RDD_KEY_112 (shared-class change:
      `MiscRule.isTightToken` gated off for Kotlin `*`/`&`).
- [x] §3/§3.3 Function/secondary-constructor body Allman-brace conversion.
      RDD_KEY_114.
- [x] §19/§19.1 String templates + triple-quoted raw strings — tokenizer-level
      fix (shared-class change). RDD_KEY_116, RDD_KEY_117.
- [x] §8/§9 one-liner getter/setter grouping — new
      `KotlinGetterSetterRule extends GetterSetterRule`. §9 fixed under
      RDD_KEY_132. §8 (plain expression-bodied `get()` property accessors,
      no initializer, no `set`) fixed under RDD_KEY_133. Block-bodied
      accessors, getter+setter pairs, and initializer+accessor properties
      remain unhandled (preserved as written, not grouped) — see Open
      Questions.

### Step 3.5 — Configuration Property Wiring

**Correction (this session):** the pipeline is already fully wired (verified
live, not just re-read) — `Main.java` `inferLanguage` auto-detects `.kt`/`.kts`,
`Formatter.java` already constructs and runs every Kotlin rule class through
the same pipeline as Java/C++. An earlier stale framing implying otherwise
has been removed. Config uses `.jxmake-code-formatter`, `key=value` format
(not YAML); boolean keys accept `on`/`off` only, not `true`/`false`.

- [x] `line-length`/`indent-size`/`indent-style`: wired, same behavior as Java/C++.
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
`kt_comments_inp.kt`/`kt_comments_out.kt` fully pass, both now enabled in the
`Makefile`.**

**The test fixtures are handwritten and may have syntax error.
Confirm with the user as needed.***

`test/kt_combined_inp.kt` and `test/kt_combined_out.kt`
capture STYLE_KOTLIN.md + STYLE_KOTLIN2.md end-to-end coverage.

`test/kt_comments_inp.kt` and `test/kt_comments_out.kt`
capture uncommon comment locations + JXM_CFMT_DIS/ENA).

**The `test/kt_*_inp.kt` files are the input files.**
**The `test/kt_*_output.kt` files are the reference output files.**

Run the formatter to an input file and output the result in `/tmp`.

Perform `diff` between the output file in `/tmp` and the reference output file.
Use the result to fix the formatter.

Also perform idempotency test.

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
2. [x] `for(...) { stmt }` not collapsing to one line (no `;` to count).
   RDD_KEY_120.
3. [x] `when(status) { ... }` squished/mis-indented — root cause was
   `KotlinDeclarationAlignmentRule.parseKotlinDeclaration` stripping newlines
   from a multi-line block-expression initializer. RDD_KEY_121.
4. [x] `fun test(): int` — not a formatter bug, a fixture typo (`int`→`Int`),
   fixed directly by the user.
5. Lines 88-90 (`val result`/`val result` + run-together statements) —
   reported as fixture ambiguity, not touched; later fixed by the user
   directly (`result1`/`result2` rename).
6. [x] Apparent double-indentation of a body's first `val`/`var` — not a
   formatter bug, a stray leftover `/tmp/kt_test/.jxmake-code-formatter`
   (`indent-size=8`) test-harness artifact. RDD_KEY_122 fixed a real, separate
   bug found in the same area (`set(value) { ... }` accessor closing-brace
   indentation).
7. [x] `val safe = ...` spacing/alignment near the `when` fix — two parts:
   missing space in `.let{ }` (RDD_KEY_123, Kotlin-gated
   `DeclarationAlignmentRule.needsSpaceBetween`), and column-alignment with
   the following `val (a, b) = ...` destructuring line (RDD_KEY_126 —
   reverses RDD_KEY_107's "never merged" decision per user request, C++
   structured-bindings precedent).
8. [x] `if(...) return@X` / `if(...) expr` braceless collapse not firing.
   RDD_KEY_124 (main case), RDD_KEY_127 (bare `else` collapse),
   RDD_KEY_128 (collapsed `else` body column-padding, new standalone
   `KotlinSpecificRule.alignBracelessElseWithIf` pass).
9. [x] Explicit-return-type functions (`fun foo(...): Int { ... }`) missing
   STYLE.md §9's blank-line-before-`return`. RDD_KEY_125
   (`MiscRule.isFunctionBodyBrace` didn't recognize Kotlin's `: ReturnType`
   shape; also fixed an unrelated C++ `->`-scan ordering bug it surfaced).

**Step 4 known-bugs punch list** (against `test/kt_comments_inp.kt` /
`kt_comments_out.kt`) — all resolved. Full narrative in `RDD_LOG.md`
RDD_KEY_131; one-line summary each:

1. [x] Missing blank line before a `when` branch led by its own standalone
   comment (`// Success case` right after `when(status) {`).
   `KotlinSpecificRule.ensureBlankLineInGap` bailed out on any comment
   anywhere in the gap; replaced with `SwitchRule`'s comment-anchored version.
2. [x] Missing blank line before a `return` directly preceded by its own
   standalone leading comment. Kotlin-only carve-out in
   `MiscRule.insertBlankLineBeforeReturn` (`appendGapWithForcedBlankAfterLastComment`)
   — confirmed Java's own accepted fixture wants the opposite, so this is
   gated `lang.isKotlin`, not a shared-class change.
3. [x] Leading blank line inside a function/lambda body not stripped when
   the first statement isn't a `val`/`var` declaration. New
   `KotlinSpecificRule.stripLeadingBlankBeforeNonDeclarationStatement`,
   wired into `Formatter.java`'s Kotlin-only Phase 1 block.
4. [x] Outermost `class Widget` missing its closing blank line + `} //
   class Widget` comment. Root cause: `BlockStructureRule.addClosingComments`
   and `insertNamedConstructBlankLines` both scanned the *entire* block span
   for any frozen (JXM_CFMT_DIS/ENA) token instead of just the boundary gap
   being rewritten — an unrelated frozen region nested deep inside
   `findFirstX` suppressed the outer class's own blank line/comment. Shared-
   class fix; also fixed a latent identical bug in
   `test/java_format_toggle_out.java` (updated that reference fixture to add
   its own previously-missing blank line + `} // class FormatToggle`).

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
APIs, which only exist in the Android SDK jars pulled in by the project's own
Gradle build — a bare compiler classpath would fail on essentially every real
file, not just report genuine syntax errors. Use the project's own Gradle
wrapper instead (tool (2) below) for any Android/Gradle candidate.

For `gui_frontend_android`: copy it once into a **persistent** location —
`~/Projects/Shadow/rc_gui_frontend_android_DOGFOOD` — rather than `/tmp`, so
it survives reboots and doesn't need re-copying every session. Do not touch
`~/Projects/RobotCoding/gui_frontend_android` itself (read-only, never write
there) — only the dogfood copy, and always re-verify a fix against the true
pristine originals there, not just the dogfood copy (a stale dogfood copy can
hold already-formatted, pre-fix output from an earlier session and falsely
look fixed/unfixed).

**One-time setup after the copy:** edit `gradle.properties` in the dogfood
copy so `project.buildDir=build` (a plain relative value — the original
points at the real project's own external build dir, which would still
collide with the original project even from a copy). Do this once per copy —
redo it if the dogfood dir is ever deleted and recopied.

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

**Lightweight PSI-based syntax-only checker (`kotlin_sc`) — viable,
distinct from the rejected full-compilation recipe above.** The rejected
K2JVMCompiler note above is about a bare classpath doing a *full compile*,
which genuinely cannot resolve `android.*`/AndroidX imports without Gradle's
dependency graph. `kotlin_sc` is a much lighter tool: it parses a `.kt`
file to a PSI/AST via `KotlinCoreEnvironment`/`KtPsiFactory` and reports
`PsiErrorElement` nodes (parse errors) — no semantic/type checking, never
needs to resolve `android.*` imports, so the AndroidX objection doesn't
apply. Built as a plain classpath-based standalone Java program; every
needed class is bundled in the single shaded
`~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib/kotlin-compiler.jar` (confirmed via
`unzip -l`, no separate intellij-core/trove4j jars needed).

Tool location: `~/Projects/JxMake/0_excluded_directory/personal/SyntaxChecker/`
(`kotlin_sc.java` + compiled `kotlin_sc.class`; gitignored via the
top-level `.gitignore`'s `0_excluded_directory` entry).

Build/run (JDK 21, matches this compiler's class file version 52 = Java 8
target, runs fine on 21):

```bash
JDK=/opt/openjdk-21_linux-x64_bin/jdk-21
KLIB=~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib
cd ~/Projects/JxMake/0_excluded_directory/personal/SyntaxChecker
"$JDK/bin/javac" -cp "$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_sc.java
"$JDK/bin/java" -cp ".:$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar" kotlin_sc <file.kt>
```

Exits 0 and prints "OK: no syntax errors" when clean; exits 1 and prints each
`PsiErrorElement`'s description + text range when a parse error is found.
Verified against this project's own `test/kt_combined_out.kt` (passes clean)
and a deliberately corrupted copy with injected stray `}}}` (correctly
reports the right errors at the right offsets).

**Recommended use going forward:** for a quick syntax/parse sanity check on
formatter output, run `kotlin_sc` first — near-instant versus a full Gradle
build. It does NOT replace `./gradlew compileDebugKotlin` for real
dogfood/compile-check testing (no semantic checking, no unresolved-reference
detection) — keep using the Gradle recipe for that. Treat it as a fast
pre-filter / supplement, not a substitute.

Follow STATE_COMMON.md's fixture-registration convention when a bug is found
and fixed here (`test/real_code_regressions_N_{inp,out}.kt`, registered in
`Makefile`'s `INP_FILES`, documented in `test/README.txt`, standard copyright
header) — same precedent as the `indent-size = 2` config-wiring no-op
exception noted there.

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

(3) `kotlin_sc` — PSI-based syntax-only checker, build/run commands above.
Used when a full Gradle build is not wanted/needed (`kotlinx.coroutines`, per
explicit user request) — catches parse errors only, weaker confidence than (2)
(no semantic/type checking).

**Finished dogfood / real-code testing**
1. **RobotCoding `gui_frontend_android`** (Android/Gradle app, 46 `.kt`
   files) — complete, config: default (no override). 9 idempotency bugs
   found/fixed one root cause at a time (RDD_KEY_134–140: multi-line
   trailing-lambda call collapse, call-wrapped-initializer
   misclassification, lambda-brace indent anchor, if/else-as-value-expression
   collapse, `&&`-vs-C-style-`&` confusion, §9 column-width flapping, Allman
   width-prediction gap). Separately, `./gradlew compileDebugKotlin` then
   found ~50 first-pass compile errors across 9 files, resolved one file at a
   time (RDD_KEY_141–144: return-type-tail misdetection eating a fluent-chain
   call, `@Annotation`-vs-label false positive, `when`-arm `else ->`
   mismatched by braceless-`else` collapse, two unrelated bugs in one
   `ProgramBuilder.kt` statement). Final: `./gradlew compileDebugKotlin` →
   `BUILD SUCCESSFUL`, zero errors (2 pre-existing, unrelated deprecation
   warnings in `WifiStaDialog.kt` only). Verified via tool (2).
2. **`github.com/square/okio`** — core bugs fixed, config:
   `.jxmake-code-formatter` with `indent-size=2` (matches okio's own
   `.editorconfig`; RDD_KEY_145 confirmed the default `indent-size=4`
   produces spurious diffs against this candidate). Fixed: RDD_KEY_146
   (unary minus mis-spacing), RDD_KEY_147 (blank-line signature-tail merge),
   RDD_KEY_148 (stale-prefix over-wrap of a braceless `if`) — combined into
   `test/real_code_regressions_30_inp.kt`/`_out.kt`; RDD_KEY_150 (missing
   `===`/`!==` tokenizer entries), RDD_KEY_151 (do-while trailing `while`
   misread as loop-start) — combined into
   `test/real_code_regressions_31_inp.kt`/`_out.kt`. Verified via round1/
   round2 idempotency diffing + tool (1) against `commonMain` only
   (full multiplatform-aware build not run). One bug found but **not**
   fixed — RDD_KEY_149, affecting `RealBufferedSink.kt`/`FakeFileSystem.kt`
   — see **Not started** below (kept open, not deleted).
3. **`github.com/Kotlin/kotlinx.coroutines`** — fully closed, config:
   default (`indent_size=4` matches the project's own `.editorconfig`, no
   override needed). Scoped to `kotlinx-coroutines-core`'s `common`+`jvm`
   source sets (163 `.kt` files), per user instruction to use tool (3)
   instead of a Gradle build. Idempotency: 11 non-idempotent files found,
   all resolved across RDD_KEY_154 (baked trailing-space growth) and
   RDD_KEY_158–162 (try/catch-as-expression brace confusion; a shared
   named-scope-with-wrapped-header root cause across 4 files; a call-argument
   continuation-indent flap; a dangling braceless-`else` mis-anchoring bare
   `}` drift; a declaration-alignment padding-width flap — see RDD table for
   per-bug detail). Compile-check via tool (3): 9 of 163 files had genuine
   syntax errors, all resolved via RDD_KEY_155 (nesting-unaware block-comment
   truncation), RDD_KEY_156 (`this@Label` spacing), RDD_KEY_157 (a
   `synchronized(...)` block's statements fused with no separators). Verified
   via round1/round2 idempotency diffing + tool (3) only (weaker compile-check
   confidence than `gui_frontend_android` — no semantic/type checking).
4. **`github.com/square/kotlinpoet`** — 2 root-caused bugs fixed, config:
   `indent_size=2` (matches kotlinpoet's own `.editorconfig`, same convention
   as okio). Fixed: RDD_KEY_152 (stale when-branch nested-brace indent anchor
   after an arrow-merge pass) and RDD_KEY_153 (Allman-conversion misfiring on
   an expression-bodied function's own trailing-lambda body — first-pass
   compile-breaking, found via `kotlinc`). Verified via round1/round2
   idempotency diffing (131 `.kt` files) + tool (1) against `jvmMain` only
   (multiplatform-aware build not attempted; zero genuine syntax errors
   found beyond the one RDD_KEY_153 fixed).
   **Re-checked 2026-07-13** (fresh full-tree round1/round2 re-run, 125 `.kt`
   files, same `indent-size=2` config, against the source tree as it stood
   after RDD_KEY_162): confirmed `FileSpec.kt`, `FunSpec.kt`, `TaggableTest.kt`
   and one `interop/kotlin-metadata` file (`ReflectiveClassInspectorTest.kt`'s
   sibling shape — the interop diff count dropped from 4 to 3, exact
   4th-file identity from the original run was never recorded) are now
   round1-vs-round2 clean, most likely a side effect of RDD_KEY_159–162's
   "physical-line-anchored decision invalidated by a later pass" fixes (not
   individually bisected to a single key — no code was touched this session).
   10 files remain non-idempotent — 4 distinct shapes, none newly fixed this
   session; see **In progress dogfood / real-code testing details** below for
   the updated shape breakdown. Not deprioritized-and-untouched anymore in
   the sense of "unchecked" — this session re-verified the gap is real and
   current, just not closed.
   **Re-checked/fixed 2026-07-13 (later same day):** Shape 1 (6 files:
   `CodeWriter.kt`, `LambdaTypeName.kt`, `MemberSpecHolder.kt`,
   `ParameterizedTypeName.kt`, `TypeVariableName.kt`, `WildcardTypeName.kt`)
   fully root-caused and resolved — RDD_KEY_163, two distinct
   `MiscRule.enforceCallLineBreaking` bugs (also resolves RDD_KEY_149's
   originally-deferred `square/okio` bug, same mechanism as Bug B). New
   `test/real_code_regressions_46_inp.kt`/`_out.kt`. `make test`: 65/65
   before, 66/66 after. All 6 files individually re-verified clean against
   the real tree, plus a fresh full-125-file round1/round2 re-run confirms
   zero new regressions. **4 files remain non-idempotent** (Shapes 2/3/4 —
   `AbstractTypesTest.kt`, `ReflectiveClassInspector.kt`, `kmAnnotations.kt`,
   `KotlinPoetMetadataSpecsTest.kt`), not investigated this session.
   **Re-checked/fixed 2026-07-14:** Shape 2 (`AbstractTypesTest.kt`) fully
   root-caused and resolved -- RDD_KEY_164, a `KotlinSpecificRule.
   enforceWhereClausePlacement` base-indent bug. New
   `test/real_code_regressions_47_inp.kt`/`_out.kt`. `make test`: 66/66
   before, 67/67 after. Confirmed clean against the real file. Shape 3 (2
   files, `ReflectiveClassInspector.kt`/`kmAnnotations.kt`) also fully
   root-caused and resolved in the same session -- RDD_KEY_165, a
   `ScopePipeline.findMergingWhenBranchLineStart` gap (only recognized a
   bare `when {` line, not a subject-form `when(subject) {` or a plain
   call-head trailing lambda like `buildCodeBlock {`). New
   `test/real_code_regressions_48_inp.kt`/`_out.kt`. `make test`: 67/67
   before, 68/68 after. Confirmed clean against both real files plus a
   fresh full-125-file re-run. **1 file remains non-idempotent** (Shape 4 —
   `KotlinPoetMetadataSpecsTest.kt`).

**Not started dogfood / real-code testing**
1. **`github.com/arrow-kt/arrow`** (NOT STARTED) — functional-programming
   library (typed errors, optics, effects); multi-module Gradle structure
   similar in spirit to `kotlinx.coroutines`, will need the Gradle-copy dance
   (tool (2)). Expected to exercise heavy generics/variance (§13,
   RDD_KEY_113), extension-function-heavy DSLs (§22), and infix-function call
   sites (§15) more than any candidate tested so far.
2. **`github.com/JetBrains/kotlin`** (NOT STARTED) — the Kotlin compiler's
   own source tree; large, likely the most demanding candidate for grammar
   coverage (compiler-internal code tends to use every language feature,
   including obscure/edge-case syntax). Last-resort/stress candidate,
   similar posture to `microsoft/STL`/`llvm-project` in the C++ list.
3. **`square/okio`'s RDD_KEY_149 deferred bug** — **RESOLVED, same session as
   RDD_KEY_163** (2026-07-13). RDD_KEY_149's own symptom (`RealBufferedSink.kt`/
   `FakeFileSystem.kt`) was never independently re-verified this session (out
   of scope, `square/okio` re-clone not re-run), but the root cause found for
   `square/kotlinpoet`'s Shape 1 Bug B is the identical mechanism RDD_KEY_149
   described (signature param column-alignment/trailing-comma lost after
   splice-back) — see RDD_KEY_163.
4. **`square/kotlinpoet`'s remaining 1-file idempotency gap** (IN PROGRESS) —
   Shape 1 (6 files) resolved via RDD_KEY_163, Shape 2 (`AbstractTypesTest.kt`)
   resolved via RDD_KEY_164, Shape 3 (`ReflectiveClassInspector.kt`/
   `kmAnnotations.kt`) resolved via RDD_KEY_165; Shape 4 (1 file:
   `KotlinPoetMetadataSpecsTest.kt`) confirmed non-compile-breaking, still not
   root-caused. Full evidence in **In progress dogfood / real-code testing
   details** below.

**In progress dogfood / real-code testing details**

*`square/okio` — RDD_KEY_149 (signature param alignment/trailing-comma lost
after splice-back):* **RESOLVED via RDD_KEY_163** (2026-07-13, root-caused
while investigating `square/kotlinpoet`'s Shape 1 Bug B, not by re-testing
`square/okio` itself this session). The "something downstream re-normalizes
the already-rendered text" mystery below was actually
`MiscRule.enforceCallLineBreaking`: its "is this really a call, not a true
signature" exemption only recognized an immediate `{` right after the
candidate's closing `)`, missing a Kotlin signature whose `)` is instead
followed by a `: ReturnType {` tail — so a genuine, already-correctly-rendered
(column-padded, trailing-comma-preserved) multi-line signature like
`RealBufferedSink.kt`'s `commonWriteUtf8` got silently re-processed as an
ordinary untyped call by this same pass, discarding the padding and comma.
Fixed with a new `MiscRule.isKotlinReturnTypeThenBlockBody` lookahead
extending the exemption to that shape. See RDD_KEY_163 for full detail; the
original repro/investigation narrative below is kept for continuity but its
"still unidentified" conclusion is superseded.

Original (pre-RDD_KEY_163) investigation notes, kept for continuity: found
via `square/okio` real-code testing (`RealBufferedSink.kt`'s
`commonWriteUtf8`, also reproduces in `FakeFileSystem.kt`): a multi-line
Kotlin function parameter list that needs to break (doesn't fit inline) is
rendered correctly by `KotlinSignatureRule.render`'s `ColumnGrid`-based path —
confirmed via a temporary debug print directly at its own return point, e.g.
`"string     : String,"`/`"beginIndex : Int,"`/`"endIndex   : Int,"`, each
column padded to its sibling's width and the trailing comma preserved. But
the text that ends up in the final written file has the padding collapsed to
a single space and the trailing comma stripped — neither of which
`render()`/`renderWithTail()` produce. Something downstream of the
`Replacement` being spliced back into the token/text stream is re-normalizing
or re-tokenizing this already-rendered text; grepped for an explicit
trailing-comma-stripping pass and found none, so the exact mechanism is still
unidentified (leading theory: a later whole-file pass collapses literal
multi-space `WHITESPACE` runs down to one space without recognizing
intentional column padding inside already-rendered replacement text, though
this alone doesn't explain the comma loss). All debug instrumentation added
while investigating (`ColumnGrid.flush`'s `CG_DEBUG`-gated print, a per-line
print in `KotlinSignatureRule.render`) was reverted before committing — none
remains in the source. Next step is probably to trace exactly which later
pass's `Replacement`/text rewrite touches this span, e.g. by diffing the
token stream immediately before and after each pass in `ScopePipeline`'s
pipeline for this specific input.

*`square/kotlinpoet` — remaining 10-file idempotency gap:* after
RDD_KEY_152/153 fixed 2 of the original 13/14 tracked files, `CodeWriter.kt`/
`FileSpec.kt`/`FunSpec.kt`/`LambdaTypeName.kt`/`MemberSpecHolder.kt`/
`ParameterizedTypeName.kt`/`TypeVariableName.kt`/`WildcardTypeName.kt`/
`AbstractTypesTest.kt`/`TaggableTest.kt` and 4 files under
`interop/kotlin-metadata/` still round1-vs-round2 diffed as of the last
check.

**Re-checked 2026-07-13** (fresh full-tree round1/round2 re-run against the
`/tmp/kotlinpoet_work/kotlinpoet` clone, 125 `.kt` files, `indent-size=2`,
source tree as it stood after RDD_KEY_162 — no code changes made this
session): `FileSpec.kt`, `FunSpec.kt`, `TaggableTest.kt`, and one
`interop/kotlin-metadata` file are now confirmed clean. **10 files remain**,
sorted into four distinct shapes:

- **Shape 1 (6 files: `CodeWriter.kt`, `LambdaTypeName.kt`,
  `MemberSpecHolder.kt`, `ParameterizedTypeName.kt`, `TypeVariableName.kt`,
  `WildcardTypeName.kt`) — RESOLVED 2026-07-13, RDD_KEY_163.** Two distinct
  `MiscRule.enforceCallLineBreaking` bugs. Bug A (`MemberSpecHolder.kt`
  only): `lineEndIndex`'s "stop at the first NEWLINE" line-fits measurement
  undercounts the true rendered width whenever an unrelated nested call
  inside an expression-bodied signature's own untouched trailing tail text
  (e.g. `apply { x.map(\n  y\n) }`) happens to already be wrapped from the
  previous round — fixed with a new depth-aware `effectiveLineEndIndex`.
  Bug B (the other 5 files): the "is this really a call, not a true
  signature" exemption only recognized an immediate `{` right after the
  candidate's own `)`, missing a Kotlin signature whose tail is
  `: ReturnType {` (return type before the body brace) — so an
  already-correctly-rendered, column-padded, trailing-comma-preserved
  multi-line signature got silently re-wrapped as a plain untyped call,
  same mechanism RDD_KEY_149 (`square/okio`) left deferred/unidentified —
  fixed with a new `isKotlinReturnTypeThenBlockBody` lookahead. New
  `test/real_code_regressions_46_inp.kt`/`_out.kt`. `make test`: 65/65
  before, 66/66 after. All 6 files + a fresh full-125-file round1/round2
  re-run confirmed zero regressions.
- **Shape 2 (1 file: `AbstractTypesTest.kt`) — RESOLVED 2026-07-14,
  RDD_KEY_164.** A multi-line `where` clause's continuation lines were
  indented one level deeper on round2 than round1 (`where
  IntersectionOfInterfaces : Runnable,` at 4 spaces round1 vs 6 spaces
  round2, rest of the clause's lines shifting correspondingly). Root cause:
  `KotlinSpecificRule.enforceWhereClausePlacement`'s `baseIndent` was
  derived from `lineIndent(tokens, i)` -- the `where` token's own current
  physical line -- which on a reformat is already the previously-wrapped
  line, one level deeper than the true signature indent, compounding an
  extra indent level per round. Fixed with a new `signatureLineIndent`
  helper (backward scan tracking paren/bracket/angle-bracket depth to the
  nearest depth-0 `;`/`}`/`{`, mirroring `ScopePipeline.findParentIndent`'s
  "true statement start" posture) replacing the naive
  `lineIndent(tokens, i)`/`lineStartIndex` "back up one physical line"
  approach, which lands on a generic parameter list's own continuation
  line once already wrapped rather than the class declaration's true first
  line. New `test/real_code_regressions_47_inp.kt`/`_out.kt`. `make test`:
  66/66 before, 67/67 after. Confirmed clean against the real
  `AbstractTypesTest.kt` file.
- **Shape 3 (2 files: `ReflectiveClassInspector.kt`, `kmAnnotations.kt`) —
  RESOLVED 2026-07-14, RDD_KEY_165.** A lone closing `}` (one plain, one
  `} // when kotlin`) sat 2 spaces shallower on round2 than round1. Root
  cause: `ScopePipeline.findMergingWhenBranchLineStart` (RDD_KEY_152) only
  recognized a bare `when {` line as the shape
  `KotlinSpecificRule.formatWhenExpressions`'s later arrow-merge pass folds
  onto the preceding branch-label line -- `ReflectiveClassInspector.kt`'s
  subject-form `when(kotlin) { ... }` (extra `(...)` before the brace) and
  `kmAnnotations.kt`'s plain call head `buildCodeBlock { ... }` (no `when`
  keyword at all, just a trailing-lambda call as a `when` branch's whole
  body) both fell through unrecognized, so the nested scope's indent was
  computed against the pre-merge physical line on a fresh format but the
  post-merge line on a reformat. Fixed by generalizing the lookahead to
  accept an optional balanced `(...)` span after `when` (subject form), and
  to accept a plain `IDENTIFIER` as the line's head token (not just the
  `when` keyword), keeping the existing "immediately followed by the open
  brace" and "preceding line ends in a top-level `->`" guards unchanged.
  New `test/real_code_regressions_48_inp.kt`/`_out.kt`. `make test`: 67/67
  before, 68/68 after. Confirmed clean against both real files plus a
  fresh full-125-file re-run (only Shape 4 remains).
- **Shape 4 (1 file: `KotlinPoetMetadataSpecsTest.kt`)** — a `val`
  declaration-alignment padding flap: `val fooAliasData    = ...` is padded
  to match a sibling's column round1, unpadded round2 (`val fooAliasData =
  ...`). Same surface symptom as RDD_KEY_162's `ChannelFlow.kt` fix (the
  "early alignment phase destabilizes a later wrap phase" family), but
  RDD_KEY_162 is confirmed already in the tree for this re-check, so if
  this is the same root cause, RDD_KEY_162's fix has a gap for this
  specific case — not root-caused, just flagged.

Original (pre-2026-07-13) investigation notes on Shape 1, kept for
continuity: (1) `MemberSpecHolder.kt`'s
`addProperties`/`addFunctions` — an expression-bodied function's parameter
list breaks across lines on round1 (`fun addProperties(\n
propertySpecs: Iterable<PropertySpec>\n): T = apply { ... }`) but stays
inline on round2 (`fun addProperties(propertySpecs: ...): T = apply { ...
}`). Re-repro'd standalone against `orig.kt`/`MemberSpecHolder.kt` at the
same `indent-size=2` config used in the original test run (confirmed the
flap reproduces there; testing at the tool's bare default config additionally
surfaces an unrelated, seemingly-stray loose-paren spacing artifact —
`addProperty( x, y )` instead of `addProperty(x, y)` — not investigated
further and NOT part of this bug, most likely a leftover ad-hoc-testing
config artifact, same shape as the one RDD_KEY_122 found, but not confirmed).
A debug print placed at `KotlinSignatureRule.renderWithTail`'s tier-1
fits-check (right before the `startColumn + inline.length() - commentLen <=
lineLengthLimit` branch) shows **byte-identical** `inline`/`tailStr` values
on both round1 and round2 (`tailStr=[: T = apply]`, 74 chars, well under the
100-col limit) — `renderWithTail` takes the exact same tier-1 early-return
branch both times and is NOT where the two rounds diverge, contradicting the
earlier hypothesis that blamed this method's own fitting decision directly.
Also confirmed via `parseFunctionTail`'s tail-token slice that the tail is
deliberately cut short right after `apply` — the `{ propertySpecs.map(::add
Property) } as T` portion is NOT part of the rendered tail string at all;
it's left as trailing token text in the stream for a later pass to handle.
Since `renderWithTail`'s own decision is identical both rounds yet the actual
written signature differs (params wrapped on round1, not on round2), the
actual flap must be introduced by a **later pass** that re-measures the
merged line (signature text + the trailing `{ ... } as T` it doesn't own) and
independently chooses to re-wrap the signature's own `(...)` as if it were a
generic breakable call/paren group — almost certainly
`MiscRule.enforceCallLineBreaking`, not `KotlinSignatureRule`. Not yet traced
inside that method to find exactly which width comparison produces a
different verdict round1 vs round2 — next step is a debug print in
`MiscRule.enforceCallLineBreaking`'s wrap-candidate-selection/width-check
logic, dumping candidate span text + computed width for the
`addProperties(...)` span specifically, round1 vs round2, to find where the
numbers disagree. (2) `CodeWriter.kt`'s earlier-observed call-argument
continuation-line indent staleness (an `is FunSpec -> o.emit(\n  codeWriter =
this,\n  ...\n)` branch body, itself a variant of the same "Phase 4
arrow-merge changes the branch's physical line, invalidating an earlier
phase's physical-line-anchored indent decision" root cause RDD_KEY_152 fixed
for `ScopePipeline`, but here manifesting in a different rule — likely
`MiscRule.enforceCallLineBreaking`'s own continuation-indent logic, plausibly
the SAME method as (1) above, worth checking together) not yet traced to its
exact source. **Confirmed by RDD_KEY_160's `kotlinx.coroutines`
investigation** (`JobSupport.kt`) that `MiscRule.enforceCallLineBreaking`
does have exactly this "stale pre-merge indent" family of bug and fixed it
there via a new `effectiveCallBaseIndent` helper — directly relevant to (but
not itself re-verified against) `CodeWriter.kt`, which has never been
re-checked since. `kotlinc` syntax-checking `kotlinpoet/src/jvmMain` finds
**zero** genuine syntax errors (only expected unresolved-reference/
multiplatform noise from checking `jvmMain` in isolation) — none of these 12
remaining files are known to be compile-breaking, only idempotency-flapping.
No fixture added and no fix attempted as of the last session that touched
this; `make test` reconfirmed passing with the source tree back at its
RDD_KEY_153 state (no code changes that session). All debug instrumentation
added while investigating (a `KSR_DEBUG`-gated print in
`KotlinSignatureRule.renderWithTail`) was reverted before ending the session.

**When a test completes:** move/compact its entry from "Not started" (or its
"In progress" detail) into "Finished dogfood / real-code testing", and add a
new numbered entry to "Tools/compiler used" if a genuinely new tool is
introduced.
