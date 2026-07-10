# STATE_KOTLIN.md — Kotlin JAR Implementation Tracker

**This file is self-contained. Do not assume `STATE.md` has been read in this
session.** If you have not read `STATE.md`, that is fine — every convention this
file depends on is restated below. This file is routed to from `CLAUDE.md`'s
job table (Kotlin JAR support → this file), and, since Kotlin implementation
work has now started, also from a redirect at the top of `STATE.md` itself
(see "Handoff Note" below for the history of that link).

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
under `test/`) and confirm zero regressions. This is the same discipline
`STATE.md` already applies to its own commits; it is restated here because a
session working from this file alone must not skip it for lack of having read
`STATE.md`.

Kotlin-only work belongs in new files (see Project Layout below), added
alongside the existing per-language files (`JavaSpecificRule.java`,
`CppSpecificRule.java`) rather than folded into them.

**Before modifying a shared class, grep first — do not read `STATE.md` in
full.** Run `grep -Fm1 'ClassName' RDD_LOG.md` (substitute the class or
method you're about to touch) to surface any existing `RDD_KEY_n` decisions
that already explain its shape — e.g. why `TokenizerCore`'s multi-char
operator table is structured the way it is (RDD_KEY_69), or why a rule class
re-derives named-construct-ness from raw tokens instead of trusting one flag
(RDD_KEY_84/85). This is almost always sufficient. Only read `STATE.md`'s
Project Layout section specifically (never its Checklist or full history) if
the grep hits don't explain what you're looking at.

---

### During implementation
- Implement one checklist section at a time
- After completing a section (or when the cumulative diff across all changed files
  exceeds ~50 lines, whichever comes first), do a checkpoint commit:
  1. Update STATE_KOTLIN.md — check off completed items and update the active checklist.
  2. `git add util/CodingStyle.md/formatter/` (the entire formatter directory)
  3. `git reset util/CodingStyle.md/formatter/target/` (exclude build output)
  4. `git commit -m "<message>"` — short descriptive message, no strict format required,
     trailer ending with `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`
- Small related items within a section may be grouped into one commit if they
  are trivially connected — use judgment based on line count (~50 lines threshold)
- Never let implemented files and STATE_KOTLIN.md drift out of sync — STATE_KOTLIN.md must
  always reflect the true current state at every commit
- Never modify the files `util/CodingStyle.md/formatter/test/*_inp.*` unless they contain
  syntax errors (they are the test input files).
- Never modify the files `util/CodingStyle.md/formatter/test/*_out.*` unless explicitly
  asked (they are the reference output files that show the expected results).
- Ignore `XL.txt`, that is the user tracker file.
- Use `/tmp` for temporary smoke-test and mini-test files.
- NEVER perform filesystem-wide find; search first in `/tmp/claude-1000` or the project root.
  If still not found, ask me.
- Do not use static analysis as the primary method of bug diagnosis or regression checking.
  Prefer evidence over reasoning (using debug prints). Keep static analysis minimal—only
  enough to identify where to insert debug prints.

## Commit Workflow

Same discipline as `STATE.md`'s own (restated, not cross-referenced, per the
self-contained requirement above):

- Implement one checklist section at a time.
- Checkpoint commit after each section or when the cumulative diff exceeds
  ~50 lines, whichever comes first: update this file's checklist, then
  `git add`/commit the formatter directory (excluding `target/`).
- Trailer: `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`.
- **On any ambiguity:** stop, add the question to Open Questions below, mark
  the checklist item `[~]`, commit this file only, and wait for an answer.
  Once resolved: append the full decision to `RDD_LOG.md` (next
  `RDD_KEY_n`, continuing the shared sequence — do not restart numbering for
  Kotlin), add the key + topic to this file's own Resolved Design Decisions
  index below, then continue.
- **On any shared-class change:** re-run the full existing C/C++/Java test
  suite before committing, per the Hard Constraint above. Record the
  before/after test count in the commit message.

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
`STATE.md` — continue its existing `RDD_KEY_n` numbering, do not restart).
Look up one key at a time via `grep -Fm1 'RDD_KEY_n' RDD_LOG.md`
(no `-A`, its lines are long).

| Key | Topic |
|---|---|
| RDD_KEY_91 | `STATE_KOTLIN.md` — self-contained tracker, not linked from `STATE.md` yet |
| RDD_KEY_92 | Shared-tokenizer approach — extend `TokenizerCore.java` in place, no separate Kotlin tokenizer |
| RDD_KEY_93 | Checklist ordering — tokenizer support first, then a `JavaSpecificRule`-style scoping pass, before any `KotlinSpecificRule.java` code |
| RDD_KEY_99 | Kotlin headless named-construct classification (`companion object {}`, anonymous `object [: Super] {}`, `init {}`) — §3.1/§3.4; also fixed a related tokenizer bug (`:` wrongly arming the supertype name as the construct name) |
| RDD_KEY_100 | Kotlin `when` no-space-before-`(` — §3.2; added `"when"` to `MiscRule.TIGHT_PAREN_KEYWORDS`, a pure no-op for C/C++/Java |
| RDD_KEY_101 | Kotlin `when` expression arrow alignment/closing comment/blank lines — §4; new `KotlinSpecificRule.formatWhenExpressions`, not a `JavaSpecificRule`/`BlockStructureRule` extension (keyword-less branches, non-all-or-nothing block-body alignment, forced blank lines) |
| RDD_KEY_102 | Kotlin null-safety operator spacing (`?.`/`!!` tight, `?:` spaced) — §5; new `KotlinSpecificRule.enforceNullSafetyOperatorSpacing`, a flat whole-file pass since no shared class does general expression-level operator re-spacing |
| RDD_KEY_103 | Kotlin variable/property declaration alignment — §6; new `KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule` (visibility-loosen-then-extend, superseding the earlier "independent parser" resolution), own statement splitter/parser/renderer for the name-before-type parts |
| RDD_KEY_104 | Kotlin constructor/function parameter list line-breaking and column alignment — §7/§7.1; new `KotlinSignatureRule extends MiscRule` (same visibility-loosen-then-extend pattern as RDD_KEY_103, user-directed), own `KotlinParam`/`KotlinSignature` model, parser, and `ColumnGrid`-based renderer for the name-before-type parts; also covers §7.2 (trailing comma preservation) |
| RDD_KEY_105 | Kotlin labeled jump / label declaration spacing (`return@label`, `label@`) — §11; new `KotlinSpecificRule.enforceLabeledJumpSpacing`, a flat whole-file pass with a small state machine, same shape as RDD_KEY_102 |
| RDD_KEY_106 | Kotlin generic `where` clause line-breaking and bound alignment — §14; new `KotlinSpecificRule.enforceWhereClausePlacement`, structurally mirrors `CppSpecificRule.enforceRequiresClausePlacement` (per-language file precedent, not a shared-class extension); new `KotlinSpecificRule(Lang, int, int)` indent-width-aware constructor |
| RDD_KEY_107 | Kotlin destructuring declarations — §12; new `DestructuringDecl`/`groupDestructuringDeclarations`/`parseDestructuringDeclaration`/`renderDestructuringGroup` in `KotlinDeclarationAlignmentRule.java` (not a new file — reuses that class's existing §6/RDD_KEY_103 infrastructure); single pre-rendered `lhsText` cell, no per-component type grid, since §12 has no type annotations to anchor one; **originally its own group stream, never merged with §6's — REVISED under RDD_KEY_126, see below, per user request** |
| RDD_KEY_126 | **REVISES RDD_KEY_107.** Merges §12 destructuring-declaration alignment into the same column-aligned group stream as an adjacent plain §6 `val`/`var` declaration, per user request citing this codebase's own C++ structured-bindings precedent; new merged `Row`/`groupAlignableDeclarations`/`renderAlignedGroup`/`toRow` in `KotlinDeclarationAlignmentRule.java` (old `groupPropertyDeclarations`/`groupDestructuringDeclarations`/`renderPropertyGroup`/`renderDestructuringGroup` kept, `@deprecated`, still used internally for parsing); `ScopePipeline.applyKotlinDeclarationsPass` simplified to one merged loop |
| RDD_KEY_127 | Bare Kotlin `else\n    stmt` (no condition of its own) never collapsed to one line — distinct gap from RDD_KEY_124 (keyed off `if`/`while`/`for`'s own `(...)` condition, never a standalone `else`); new shared `collapseBracelessBody` helper extracted from `tryCollapseBraceless`, plus a dedicated `else`-keyword branch in `collapseSingleExpressionBlocks`'s main loop; collapse-to-one-line half fixed, column-padding-to-align-with-preceding-`if`-branch half left as an open question **(now resolved — see RDD_KEY_128)** |
| RDD_KEY_128 | **RESOLVES RDD_KEY_127's open column-padding question**, user-confirmed via the now-enabled `kt_combined_inp.kt` fixture: a collapsed single-line `else` body pads with spaces to start at the same column as its preceding single-line `if(...)` branch's own body; new standalone, last-running `KotlinSpecificRule.alignBracelessElseWithIf` (line-based, on fully-formatted text) rather than computed at collapse time — an earlier collapse-time attempt was root-caused stale by one column since `MiscRule.enforceComplexityPadding`'s `if (`→`if(` tightening still runs after the collapse pass |
| RDD_KEY_131 | `test/kt_comments_inp.kt`/`kt_comments_out.kt` — fixed all four remaining bugs (comment-led `when`-branch blank line, comment-led `return` blank line, leading-blank stripping in non-declaration-led bodies, outermost-class closing comment suppressed by an unrelated nested frozen region) and enabled the fixture in the `Makefile`. Full narrative in `RDD_LOG.md`; see Step 4 punch list below for the one-line-per-bug summary. Note: RDD_KEY_129/130 are used by unrelated C/C++/Java braceless-`if`/`else`-chain work tracked in `STATE.md`, not this file — 131 is the correct next-available key. |
| RDD_KEY_108 | Kotlin annotation use-site target `:` spacing — §16; new `KotlinSpecificRule.enforceAnnotationUseSiteTargetSpacing`, small state machine over a flat whole-file pass (same shape as §11/RDD_KEY_105); new `USE_SITE_TARGETS` set matched by token text (not `TokenType.KEYWORD`) since `delegate` is a soft keyword, not tokenizer-lexed; `@`-to-target spacing deliberately left unenforced (no textual backing, no codebase precedent for reformatting plain annotation spacing) |
| RDD_KEY_109 | Kotlin lambda-with-receiver nesting exemption + arrow spacing — §17/§17.1; **shared-class change** — `ComplexityPaddingEvaluator.isLoose` extended to skip a `.`-preceded/`->`-followed `(...)` span (a lambda-with-receiver's own invocation parens) rather than counting it as nesting, pure no-op for C/C++/Java (confirmed via harness, `make test` 32/32 before/after); new Kotlin-only `KotlinSpecificRule.enforceArrowSpacing` + `collectWhenBranchArrowIndices`, a flat whole-file single-space arrow pass that explicitly excludes `when`-branch arrows (owned by §4's column alignment) |
| RDD_KEY_110 | Kotlin `for` loops and ranges — §10; `in`/`until`/`downTo`/`step` reclassified (b)→(a), already inert w.r.t. `ComplexityPaddingEvaluator.isLoose` with zero code changes (verified via harness, not a keyword-set addition as originally guessed); new `KotlinSpecificRule.enforceRangeOperatorSpacing`, a simpler one-sided sibling of §5/RDD_KEY_102's state machine tightening `..`/`..<` on both sides (no spaced variant, unlike `?:`) |
| RDD_KEY_111 | Kotlin `enum class` with members blank-line "emphasis" around the mandatory `;` — §2; `insertNamedConstructBlankLines` (shared) confirmed to only handle the body-open/close blank lines, not the `;`-separator emphasis — new `KotlinSpecificRule.separateEnumConstantListTerminator` (+ helpers), structurally mirroring `JavaSpecificRule.separateEnumConstantListTerminator` (per-language precedent, same as §14/RDD_KEY_106) rather than reusing/relocating it, since that class isn't shared and its own helper names aren't present in any shared class either |
| RDD_KEY_112 | Kotlin expression-bodied functions — §9; new `KotlinSignatureRule.FunctionTail`/`parseFunctionTail`/`renderWithTail`, a three-tier inline/params-broken/wrap-`=` fallback delegating to §7's existing `render` for the middle tier — plus a **shared-class fix**, `MiscRule.isTightToken`'s `*`/`&` tight-token treatment gated off for Kotlin (was collapsing ordinary multiplication spacing, `x* x`, surfaced by this work's own harness reproducing the style doc's `x * x + y * y` worked example) |
| RDD_KEY_113 | Kotlin generic variance (`in`/`out`) — §13; **shared-class fix** — `TokenizerCore.GENERIC_SAFE_KEYWORDS` extended with `"in"`/`"out"` so `reclassifyAngleBrackets` recognizes `Box<out T>`/`Comparable<in T>` as generic `<`/`>` pairs rather than comparisons; pure no-op for C/C++/Java (neither keyword exists in their keyword sets); tokenizer-level fix, no rendering pass needed |
| RDD_KEY_114 | Kotlin function/secondary-constructor body Allman-brace conversion — §3/§3.3; new `KotlinSpecificRule.enforceFunctionDefinitionAllmanBraceStyle` (+ `isFunctionOrConstructorCloseParen`/`findSignatureCloseParenBeforeBrace`/`isAngleOpen`/`isAngleClose`/`skipAngleBracketsBackward`), structurally mirroring `JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle`/`CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle` but with a much more conservative candidate signal (backward-scan must land on `fun`/`constructor`, since Kotlin has no `new` keyword to rule out trailing-lambda calls the way Java/C++ rule out ordinary calls) — also handles `: ReturnType` sitting between `)` and `{`, and tolerates the tokenizer's non-reclassified plain-`OP` `<T>` after `fun` (both discovered only via harness) |
| RDD_KEY_115 | Kotlin semicolon stripping — §1; fixed a real bug in the pre-existing `stripOptionalSemicolons` (committed earlier, `b0e778f`, predating this session's RDD-log convention) — it only protected the enum-with-members mandatory `;`, silently stripping a deliberate same-line multi-statement `;` too (would have merged two statements into one invalid line); rewritten around a single positive-evidence `isTrailingSemicolon` rule (only strip a `;` that's the last significant thing on its physical line), reusing §2's `findEnumConstantListTerminators` for the enum exclusion; also fixed a stray-trailing-space gap the old version had |
| RDD_KEY_116 | Kotlin string template tokenizer risk — §19; **shared-class fix** — `TokenizerCore.emitString`'s naive scan-to-next-`"` misread a nested string inside a `${...}` interpolation (`"${foo("x")}"`) as three tokens instead of one, a genuine correctness risk (a later spacing pass could insert whitespace inside the literal's actual text); fixed with a Kotlin-only `skipKotlinString`/`skipKotlinInterpolationBlock`/`skipKotlinChar` path (depth-tracks `${...}`'s own `{`/`}` nesting, recurses for nested strings/chars, arbitrarily deep), gated behind `lang.isKotlin`, non-Kotlin scan untouched; surfaced triple-quoted raw strings as a related, explicitly out-of-scope gap (new row 19.1, not fixed — undocumented in either style doc) |
| RDD_KEY_117 | Kotlin triple-quoted raw string tokenizer support — row 19.1, **shared-class fix**; badly broken before this (`"""hello "world" end"""` mis-lexed as five tokens including a bare `IDENTIFIER`; multi-line raw strings leaked a spurious `NEWLINE` token into the content); fixed with Kotlin-only `isKotlinRawStringOpener`/`emitKotlinRawString`/`skipKotlinRawString` — no backslash-escape processing (literal `\` by design), greedy termination at the first `"""` (matches real Kotlin compiler semantics); `${...}` interpolation still recognized via `skipKotlinInterpolationBlock`, extended to also recognize a nested raw string inside an interpolation expression; non-Kotlin paths (Java text block, C++ raw string, plain C string) confirmed untouched |
| RDD_KEY_132 | Kotlin §8/§9 one-liner getter/setter grouping — new `KotlinGetterSetterRule extends GetterSetterRule` (same visibility-loosen-then-extend pattern as RDD_KEY_103/104), own newline-terminated member splitter, `[modifiers] fun name(params) [: ReturnType] = expr` parser, and 3-column grid render; scope limited to expression-bodied one-liner functions (§9) — `get()`/`set()` property accessors (§8) remain an unhandled, structurally different shape; new `test/kt_combined_inp.kt`/`kt_combined_out.kt` `class Accessors` case |
| RDD_KEY_133 | Kotlin §8 property-accessor (`get()`/`set()`) one-liner grouping, the remaining gap RDD_KEY_132 left open — new `parseKotlinAccessorMember`/`isAccessorMember`/`renderAccessorGroup` in `KotlinGetterSetterRule.java` (Kotlin-only file, no new shared-class methods), scoped to a plain no-initializer `val`/`var` property immediately followed by a bare `get() = expr` (no `set`, no block body); merges the two-line source into one Kotlin-legal line and column-aligns via `ColumnGrid`, mirroring §9's 4-cell shape; two idempotency bugs found and fixed — a **shared-class fix** (`DeclarationAlignmentRule.needsSpaceBetween`, Kotlin-gated carve-out so `get`/`set` keywords are tight against a following `(` like an ordinary call name) and a `KotlinDeclarationAlignmentRule.parseKotlinDeclaration` fix (bails rather than swallowing a re-parsed merged line's `get`/`set` into its type-token scan, which was also cross-contaminating an unrelated sibling's column width when wrongly grouped together) |
| RDD_KEY_134 | Kotlin Step 5 dogfood testing found a compile-breaking bug (not just idempotency): `MiscRule.renderCallCandidate`'s Option 2 (`renderCallPreserveGroups`, via `groupByOriginalLine`) collapsed a multi-line trailing-lambda call argument (`Thread({ ...multi-statement body... }, "tcp-reader")`) onto one line with no statement separators — invalid Kotlin, since (unlike C/C++/Java) Kotlin has no `;` to fall back on. **Shared-class fix**, Kotlin-gated (`lang.isKotlin`): new bail in `renderCallCandidate` when any top-level call argument contains both a newline and a `{` (new `containsBrace` helper); confirmed a non-gated version broke the pre-existing C++ `real_code_regressions_1` fixture, so the gate is required, not optional. New `test/real_code_regressions_17_inp.kt`/`_out.kt` fixture. Fixes 2 of 3 known compile-breaking dogfood cases (`RobotTcpSession.kt`, `WifiStaDialog.kt`'s `postDelayed` callback); `BleDeviceSelectDialog.kt`'s own `postDelayed` callback still shows an idempotency diff, not yet root-caused. |
| RDD_KEY_135 | Follow-up dogfood idempotency bug (`PlayMusicBlock.kt`, §6 declaration alignment): `KotlinDeclarationAlignmentRule.spansMultipleLines` treated a call-wrapped-but-single-statement initializer the same as a genuine multi-line block, wrongly bailing a declaration out of its alignment group on a second format pass. Fixed with paren/brace-depth-aware newline classification (mirrors `ScopePipeline.hasTopLevelNewline`'s "ignore newlines inside a call's parens" idiom): bail only on a newline inside a real `{`...`}` body or at true top level; ignore a newline strictly inside a call's parens with no enclosing brace. First attempt (paren-depth only, no brace-depth check) was too permissive and regressed RDD_KEY_134's own fixture — corrected once `make test` caught it. New `test/real_code_regressions_18_inp.kt`/`_out.kt`. Resolves 2 more of the original 9 dogfood non-idempotent files (`PlayMusicBlock.kt`, `BleDeviceSelectDialog.kt`'s `val filter`); 5 remain, see Step 5 below. |
| RDD_KEY_136 | Follow-up dogfood investigation (`MainActivity.kt`'s `_checkRecovery()`): a closing-brace indentation drift, confirmed broken even on a fresh format (not just round1-vs-round2). Root cause: a trailing lambda argument's `{` opening on a continuation line of a multi-line fluent chain (`.setPositiveButton("Ok") {`), deeper than the chain statement's own first line (`AlertDialog.Builder(this)`) — `ScopePipeline.processScope` derived the lambda body's indent, and its closing `}`'s placement, from `findParentIndent`'s whole-statement-first-line anchor (needed elsewhere for `case 1:` labels) instead of the brace's own physical line. **Shared-class fix**, Kotlin-gated (`lang.isKotlin`): new `ScopePipeline.braceLineIndent` helper (derives indent from the brace's own physical line, no statement-boundary reasoning) feeding a new `effectiveSpanIndent` (brace-line indent when deeper than/where `findParentIndent`'s `spanIndent` is null) used for the child body's inherited indent and closing-brace placement; the named-scope one-liner pre-expansion path is unaffected, still using `spanIndent`. New `test/real_code_regressions_19_inp.kt`/`_out.kt`. Fixes 2 more of the original 9 dogfood non-idempotent files (`MainActivity.kt`, `BlePermissions.kt`) plus confirms `ToolbarActions.kt`'s prior diff shared this root cause — but re-checking `ToolbarActions.kt` against the true pristine original (not the stale dogfood copy) surfaced a different, previously-masked statement-joining-without-separator bug, shared with `MainViewModel.kt`; 5 diffs remain, see Step 5 below. |
| RDD_KEY_137 | Follow-up dogfood investigation (`MainViewModel.kt`/`ToolbarActions.kt`'s statement-joining-without-separator bug RDD_KEY_136 flagged): a `val`/`var` declaration whose initializer is a parenthesized if/else expression (`val display = (if (cond) a else b)`), immediately followed by another statement, was fused onto that statement's line with no separator — compile-breaking. Minimized via six repros to the precise trigger: both the wrapping parens AND the if/else together are required. Root cause: `BlockStructureRule.collapseSingleExpressionBlocks`'s main dispatch fires on every `if`/`else` keyword with no notion of Kotlin's `if`-as-value-expression; a wrapped, braceless expression-position `if` fell into the Kotlin-only "braceless statement body" collapse branch (and the separate bare-`else` branch), which then consumed past the wrapping `)` and ate the following statement's separating newline. **Shared-class fix**, Kotlin-gated (`lang.isKotlin`): a running unmatched-`(`/`[`-depth counter in the main dispatch loop refuses to treat `if`/bare `else` as collapsible while depth > 0 — a statement-position `if`/`else` is never itself nested inside a paren this pass didn't open and fully consume via its own condition matching. Deliberately does NOT also gate on the preceding token (e.g. `=`) — an early broader attempt regressed `real_code_regressions_18` (an unparenthesized expression-position `if` that legitimately depends on this same collapse path per RDD_KEY_135); reverted to the depth-only signal. New `test/real_code_regressions_20_inp.kt`/`_out.kt`. Fixes the last 2 of the original 9 dogfood non-idempotent files (`MainViewModel.kt`, `ToolbarActions.kt`); 4 diffs remain, see Step 5 below. |
| RDD_KEY_138 | Follow-up dogfood investigation (`BlockCanvasView.kt`'s missing-space-before-`&&` diff): a `val`/`var` declaration whose initializer contains a top-level `&&` lost its preceding space (`a > 1&& b`) — reproduced on a fresh first-pass format, not just idempotency; scoped specifically to declaration initializers (plain assignments/returns/call arguments were unaffected). Root cause: `DeclarationAlignmentRule.isTightToken`'s `Token.isRepOp(t, '*') || Token.isRepOp(t, '&')` check, ungated by language — meant for C/C++'s repeated pointer/reference declarator sigils (`**`, `&&` as an rvalue-reference type), but `Token.isRepOp` matches ANY run of `&` characters including Kotlin's `&&` logical-AND operator, which Kotlin has no unary/repeated `*`/`&` construct to be confused with. `MiscRule.isTightToken` (the sibling shared-join-point method used by non-declaration expression rendering) already carried the correct `!lang.isKotlin &&` gate for this exact reason; `DeclarationAlignmentRule.isTightToken` was simply the one copy that had never received it. **Shared-class fix**: added the identical `!lang.isKotlin &&` gate, mirroring `MiscRule.isTightToken` verbatim. New `test/real_code_regressions_21_inp.kt`/`_out.kt`. Fixes `BlockCanvasView.kt`'s last remaining diff; 2 diffs remain (`BlockPalette.kt`'s two separate diffs), see Step 5 below. |
| RDD_KEY_139 | Follow-up dogfood investigation (`BlockPalette.kt`'s §9 one-liner column-width-flapping diff): a run of adjacent §9 expression-bodied one-liner functions had different column-alignment widths on round1 vs round2 — round1 padded all members uniformly, round2 split them into narrower subgroups. Root cause: `KotlinGetterSetterRule.parseKotlinOneLinerMember` never got the length pre-check `GetterSetterRule.parseOneLinerMember` (the C/C++/Java base rule) already has for this exact reason — a member whose body contains a call that a later phase (`MiscRule.enforceCallLineBreaking`) might wrap is estimated for width at grouping time; on a fresh format all members are still single-line, so the too-long one groups/pads with the rest, only for the later phase to wrap it and leave the padding stale; reformatting that already-wrapped output then correctly excludes it, splitting the run differently. **Shared-class fix**: raised `GetterSetterRule.indentWidth`/`lineLengthLimit`/`hasBreakableCall` from `private` to `protected` (same reuse pattern as RDD_KEY_103/104/133), then ported the identical length pre-check into `parseKotlinOneLinerMember`. New `test/real_code_regressions_22_inp.kt`/`_out.kt`. Fixes `BlockPalette.kt`'s §9 diff; 1 diff remains (the file's Allman-brace idempotency gap), see Step 5 below. |
| RDD_KEY_140 | Follow-up dogfood investigation (`BlockPalette.kt`'s last remaining diff, the Allman-brace idempotency gap): an `override fun draw(...) { _drawBlock(...) }` method body nested inside an anonymous `object : Block() { ... }` stayed K&R on round1 (call wrapped internally by a later phase) but flipped to Allman on round2. Root cause: `KotlinSpecificRule.isSingleLineBody` never got the width-prediction pre-check `JavaSpecificRule.isSingleLineBody` (the C/C++/Java sibling) already has for this exact bug class — same "decision made before a later call-wrapping phase runs" shape as RDD_KEY_139, this time for the K&R-vs-Allman brace choice. **Fix**: ported `hasBreakableCall` + an estimated-width pre-check into `KotlinSpecificRule.isSingleLineBody` as a **duplicated** local helper (not shared/inherited — `KotlinSpecificRule` is a sibling of, not a subclass of, `JavaSpecificRule`), adapted to this class's own helper API (`nextSignificantIndex`'s inclusive-of-`from` semantics; `matchParenForward`). A first port compiled but had no effect — traced to `JavaSpecificRule`'s width formula itself (summing only token text lengths, no allowance for indentation or inter-token spaces), an undercount too small to matter at `JavaSpecificRule`'s shallower call sites but large enough at this Kotlin method's deeper object-nested indentation to false-negative. Corrected the formula (added indent length + 1 space per token) rather than porting it unmodified. New `test/real_code_regressions_23_inp.kt`/`_out.kt`. Fixes `BlockPalette.kt`'s last remaining diff — the file is now fully round1-vs-round2 idempotent. |
| RDD_KEY_141 | `./gradlew compileDebugKotlin` against the fully-formatted dogfood tree (STATE_KOTLIN.md Step 5, run after all 9 originally-flagged idempotency diffs were resolved) failed with ~50 compiler errors across 9 files, contradicting the previously-recorded clean baseline compile. Root cause (one confirmed contributor, via debug-instrumentation pipeline bisection): `ScopePipeline.applySignaturePass`'s Kotlin `: ReturnType` tail detection (`findLastTopLevelCloseParen`) matched any top-level `)` in a span's range with no check that a genuine `:` actually followed it before the brace — so a fluent chain `IDENTIFIER1().IDENTIFIER2 { lambda }` (first call empty-parens, second call using Kotlin's bare/parenless trailing-lambda syntax) had its first call's `)` wrongly read as a signature's parameter-list close and the second call's name as a return-type tail, silently deleting `.IDENTIFIER2` from the output on the very first format pass (compile-breaking, not just an idempotency flap). Confirmed pre-existing (reproduces against a jar built from commit ad4f746, before RDD_KEY_140) and confirmed on real dogfood files `ConnectTypeDialog.kt`/`WifiApDialog.kt` (`.show().also { ... }` → `.show() { ... }`). **Fix**: require `nextSignificantIndex(tokens, realCloseParen)` to be a top-level `:` before accepting the Kotlin return-type-tail branch; otherwise bail. New `test/real_code_regressions_24_inp.kt`/`_out.kt`. `make test`: 43/43 before, 43/43 after. Not yet confirmed whether this resolves all ~50 originally-seen compile errors or whether others are separate bugs — re-format + re-compile not yet re-run. |
| RDD_KEY_142 | Follow-up dogfood investigation (`BlockCanvasView.kt`'s compile errors, following up on RDD_KEY_141): `class BlockCanvasView @JvmOverloads constructor(...)` was corrupted to `class BlockCanvasView@ JvmOverloads constructor(...)`, and `ToolbarActions.kt`'s second of two adjacent `@Volatile private var` declarations similarly corrupted -- both first-pass parse errors, not idempotency flaps. Root cause: `KotlinSpecificRule.enforceLabeledJumpSpacing`'s `label@` loop-label state machine (STYLE_KOTLIN.md §11) transitioned into its force-space `AFTER_DECL_AT` state for ANY identifier followed by `@`, with no check that it was actually a label (as opposed to an unrelated `@Annotation` after a class name or a preceding statement's trailing identifier). **Fix**: added `isLoopLabelTarget` lookahead requiring `for`/`while`/`do`/`{` after the `@` before accepting it as a label; applied to both the state transition AND the separate `tightBeforeAt` spacing decision (fixing only the former left the same-line tight-adjacency case, e.g. `class Foo @JvmOverloads constructor(...)`, still broken). New `test/real_code_regressions_25_inp.kt`/`_out.kt`. Fixes `BlockCanvasView.kt`'s class-header annotation corruption and `ToolbarActions.kt`'s second `@Volatile` corruption; `BlockCanvasView.kt`'s other errors (starting at line ~495, further downstream) not yet re-verified. |
| RDD_KEY_143 | Continued dogfood investigation (`Optimizer.kt`'s compile errors, following up on RDD_KEY_142): a `when` expression's `else -> { ... }` arm with a multi-statement block body was flattened onto one line with no `;` separators -- a first-pass parse error. Root cause: `BlockStructureRule.collapseSingleExpressionBlocks`'s bare-`else` handling (meant only for a real `if`/`else` chain's braceless single-statement body, STYLE.md §10) matched any `else` keyword not immediately followed by `if`/`{`, which also wrongly matched a `when` arm's `else ->` label. **Fix**: added an `isWhenArrow` check (token after `else` is `->`) to bail out of the braceless-collapse path for that shape, leaving `formatWhenExpressions` to handle it correctly on its own. New `test/real_code_regressions_26_inp.kt`/`_out.kt`. `make test`: 45/45 forward+idempotency. Post-RDD_KEY_142 dogfood compile showed 3 remaining files: `Optimizer.kt` (this bug), `ProgramBuilder.kt` (**correction, see RDD_KEY_144**: unrelated bugs, not this shape), `XMLSaveLoad.kt` (line 430, distinct/unconfirmed shape) -- not yet re-verified against a fresh compile after this fix. |
| RDD_KEY_144 | Continued dogfood investigation (`ProgramBuilder.kt`'s compile errors, following up on RDD_KEY_143): two separate, unrelated bugs in one statement (`it !is _FunctionItem || calledFunctions.contains(it.func.funcName)`). **Bug A**: `DeclarationAlignmentRule.needsSpaceBetween` had no case for Kotlin's `!is`/`!in` negated operators (a single tight lexical unit), so the generic KEYWORD-space default corrupted `!is`/`!in` into `! is`/`! in`. Fixed via a Kotlin-gated no-space check. **Bug B**: `MiscRule.enforceCallLineBreaking`'s `renderCallCandidate` used `parseSignature` (a C-style "type name" parser, meant to detect real forward declarations) on Kotlin candidates too -- Kotlin has no such shape (`fun ...` only) -- so `parseParam`'s heuristic misparsed the call argument `it.func.funcName` as a `Type name` pair, inserting a spurious space once wrapped: `it.func. funcName`. Fixed by splitting `sig` (drives the existing zero-param bail-out) from a new `sigForRender` (forced `null` for Kotlin, used only for render-path selection) -- an initial attempt that nulled `sig` itself broke fixture 22's zero-param declaration, caught by `make test` and corrected. New `test/real_code_regressions_27_inp.kt`/`_out.kt`. `make test`: 47/47 forward+idempotency. |
| RDD_KEY_118 | Kotlin import-ordering implementation — §24 spec now implemented; new `KotlinSpecificRule.enforceKotlinImportOrdering` (+ `ParsedKotlinImport`/`parseKotlinImportStatement`/`appendRange`/`joinVerbatim`/`isPathOp`/`findLocalPackagePrefix`/`classifyKotlinImportGroup`/`matchesPrefix`), mirroring `JavaSpecificRule.enforceImportOrdering` but with no `static` bucket (priority local > kotlin > java/javax > org > com > other) and an import statement ending on optional `;` or NEWLINE/EOF rather than a required `;`; new `kotlin-import-order`/`-sort`/`-depth`/`-blank-lines` keys added to `Config.java` mirroring `java-import-*` exactly; verified via a standalone 10-case harness, not yet wired into `Formatter.formatOne` |

---

## Open Questions

- ~~**First-statement double-indentation bug**~~ **RESOLVED — was a
  test-harness artifact, not a real formatter bug.** Root-caused with a
  debug-print harness (see RDD_KEY_122): a stray
  `/tmp/kt_test/.jxmake-code-formatter` config file left over from earlier
  ad-hoc testing this session had `indent-size=8`, silently picked up by
  every fixture copy formatted under that directory (config discovery walks
  up from the file being formatted). `normalizeIndent` was correctly
  rounding a real 4-space indent up to the nearest multiple of the
  wrongly-configured 8-width, doubling it. With that stray config deleted
  and a clean default `indent-size=4`, the symptom does not reproduce at
  all — confirmed against both the minimal `fun sumAll(...) { var total = 0
  ... }` repro and the full `kt_combined_inp.kt` fixture. The related item 4
  from the Step 4 punch list ("nested `for` gets a spurious `} // for`
  comment") also stopped reproducing once the same stray config
  (`closing-comment-min-lines=1`) was removed — also concluded to be the
  same artifact, not a real bug. **A different, real bug was found in the
  same area while re-testing with a clean config and fixed under
  RDD_KEY_122**: a Kotlin property's `set(value) { ... }` accessor body,
  immediately following a `;`-less `var`/`val` declaration line, had its
  closing `}` under-indented by one level — `ScopePipeline.findParentIndent`
  didn't recognize a Kotlin newline-terminated declaration as a statement
  boundary, only C/Java's `;`. Fixed with a narrowly-scoped Kotlin-only rule
  (depth-0 `NEWLINE` immediately followed by `get`/`set`). See RDD_LOG.md
  RDD_KEY_122 for full detail.
- **Reversed declaration grammar (§6/§7, found during Step 1).**
  `DeclarationAlignmentRule.Declaration` (and `MiscRule`'s parameter/signature
  model) assume C/Java's `[modifiers] Type name [= init]` token order.
  Kotlin's actual grammar is `[modifiers] val/var name : Type [= init]` —
  name comes first, type is optional and trails after `:`. This affects both
  variable/property declarations (§6) and function parameter lists (§7),
  which the style doc expects to align into the same kind of column grid C/Java
  declarations do (name column, `:`/type column, `=` column).
  Two ways forward:
  1. Extend `DeclarationAlignmentRule`'s shared `Declaration` model to support
     a name-before-type grammar mode — touches an already-COMPLETE shared
     class's *behavior*, not just additive keyword recognition, so per the
     Hard Constraint this needs to stop and ask before doing it.
  2. Give `KotlinSpecificRule.java` its own independent declaration/parameter
     parser and renderer, reusing only lower-level shared primitives
     (`ColumnGrid`, `ModifierPriority`) rather than `Declaration` itself —
     no shared-class behavior change, more duplicated logic.
  **Resolved for both §6 (RDD_KEY_103) and §7 (RDD_KEY_104):** originally the
  user chose option 2 for §6 (independent parser in `KotlinSpecificRule.java`),
  but reconsidered before implementing either and picked a third approach for
  both sections instead — loosen the relevant shared class's visibility on its
  C/C++/Java-agnostic private helpers (additive, behavior-neutral), then
  extend it (`KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule`
  for §6; `KotlinSignatureRule extends MiscRule` for §7), each with its own
  name-before-type model/parser/`ColumnGrid` renderer. See the Step 3
  checklist and `RDD_KEY_103`/`RDD_KEY_104` for full implementation detail.
- **String template tokenizing (§19, found during Step 1).** Not yet verified
  whether `TokenizerCore.emitString()` correctly closes a Kotlin string when a
  `${...}` interpolation contains its own nested `"..."` (e.g.
  `"${foo("x")}"`). `emitString()` was written for C/Java strings, which never
  nest quotes. This is a tokenizer-correctness risk, not a style question —
  should be resolved with a debug-print/dump harness against real nested-quote
  input (per this file's evidence-over-reasoning rule) before trusting any
  Kotlin fixture that uses string templates. Not yet investigated in depth.
- **§8/§9 one-liner getter/setter grouping never actually fires for Kotlin —
  RESOLVED for both §9 (expression-bodied functions, RDD_KEY_132) and §8
  (plain expression-bodied `get()` property accessors, RDD_KEY_133).** New
  `KotlinGetterSetterRule extends GetterSetterRule` (RDD_KEY_132) fixes the
  confirmed-broken case below for `fun name(params): ReturnType = expr`
  one-liners — verified via harness (`fun getX(): Int = 1` / `getLongName` /
  `getZ` now column-align, including correct outlier exclusion when one
  sibling's body is disproportionately wide) and a new `test/kt_combined_inp.kt`
  fixture case. **§8 fix (RDD_KEY_133):** a plain, no-initializer
  `val`/`var name: Type` property immediately followed by a bare
  `get() = expr` accessor (no `set`, no block body) is now merged onto one
  Kotlin-legal line and column-aligned across adjacent siblings, verified via
  harness (`val x: Int` / `get() = 1` and same-shape siblings with varying
  name widths now merge and align; outlier exclusion, mixed §8+§9-adjacent
  groups staying separate, block-bodied/setter/initializer cases staying
  untouched, and a trailing-comment case all confirmed) plus a new
  `class PropertyAccessors { ... }` case appended to `test/kt_combined_inp.kt`/
  `kt_combined_out.kt`. **Remaining documented gap:** block-bodied accessors
  (`get() { ... }`/`set(v) { ... }`), a property pairing a getter with a
  setter, and a property with both an initializer and a custom accessor are
  all still left "preserved as written, not grouped" — §8's "preserve as
  written" requirement is still met for them (nothing in this fix touches a
  shape outside its own narrow getter-only parser), only the alignment
  upgrade for those wider shapes remains out of scope. Original bug report
  preserved below for context. §8's/§9's scoping-table rows
  (RDD_KEY_112 and the §8 row above it) assert Kotlin one-liner
  accessors/expression-bodied functions "participate in the same §14/STYLE.md
  getter/setter-style aligned group" as a free consequence of the shared
  `GetterSetterRule` — but this was never actually harness-verified for the
  *grouping* behavior itself, only for the "preserve as written" /
  no-wrong-collapse behavior of a single standalone one-liner. Live-tested
  just now (standalone JAR, `.jxmake-code-formatter` config, scratch `.kt`
  fixture) and confirmed **broken**: three adjacent `fun getX(): Int = 1`
  /`getY`/`getZ` one-liners get zero column alignment, where the equivalent
  Java (`int getX() { return 1; }` etc.) correctly aligns. Reproduced with and
  without an explicit `public` modifier — no difference. Root cause (read,
  not yet fixed): `GetterSetterRule.groupOneLiners`'s `isClassScope` gate is
  `lang.isJava || hasAccessSpecifier(...)` — `hasAccessSpecifier` looks for
  C++-style `public:`/`private:` labels, which don't exist in Kotlin, so
  `isClassScope` is always `false` for Kotlin. More fundamentally,
  `parseOneLinerMember`'s modifier-consuming loop is gated `if (lang.isJava)`
  only, and the rest of that method assumes C/Java's
  `[modifiers] ReturnType name(...)` token order to find the member's name —
  the exact same reversed-grammar problem already resolved for §6/§7 above
  (RDD_KEY_103/104), just not yet extended to this shared class. Not fixed in
  this session (documentation-only session) — same "loosen shared-class
  visibility, then extend with a Kotlin-aware subclass/method" pattern as
  RDD_KEY_103/104 is the likely fix shape, but needs its own stop-and-think
  before touching `GetterSetterRule`'s behavior per the Hard Constraint. Until
  fixed, `AI_PREAMBLE_AESTHETIC.md`'s Rule 2 ("JAR aligns standard-prefix
  getter/setter groups automatically") is **not true for Kotlin** — flagged
  there with a caveat rather than silently relying on it.
- **Bare `else` single-statement collapse — fully RESOLVED (RDD_KEY_127 +
  RDD_KEY_128).** `kt_combined_out.kt` shows a bare `else` (no condition of
  its own, following an `if(...)  0`-shaped one-liner) collapsed onto one
  line and column-padded to align with the `if` branch above it:
  `else               it.toInt()`. RDD_KEY_124's fix only handled
  `if`/`while`/`for` (each keyed off its own `(...)` condition) —
  `collapseSingleExpressionBlocks`'s loop never triggered on a standalone
  `else` keyword at all. RDD_KEY_127 fixed the collapse-to-one-line half
  (new shared `collapseBracelessBody` helper + a dedicated `else`-keyword
  branch in the main loop). RDD_KEY_128 (this session, user-confirmed via
  the fixture once enabled in the `Makefile`) fixed the remaining column-
  padding half: a new standalone `KotlinSpecificRule.alignBracelessElseWithIf`
  pass, deliberately run *last* in the pipeline (after every paren-
  tightening/spacing pass has settled the `if` line's final rendered width)
  rather than folded into the early collapse pass — an earlier attempt at
  computing the padding at collapse time was root-caused to be stale by one
  column because `MiscRule.enforceComplexityPadding` still tightens `if (`
  to `if(` in a pass that runs *after* the collapse. No longer open.

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
      and is reused as-is for Kotlin's lambda/function-type/`when` arrow — no
      new token needed. `@` in labeled jumps (`return@label`, `outer@`) needs
      no new operator entry either: it already falls through to `emitOperator`'s
      single-char fallback as its own `OP` token, which is sufficient (the
      surrounding spacing rule is a Step 3 `KotlinSpecificRule` concern, not a
      tokenizer one).
      **Found and fixed a real bug in the process (not just additive):**
      `emitNumber()` unconditionally consumed every `.` character, so
      `1..10` lexed as one bogus `NUMBER` token `"1..10"` (and `1..<10` as
      `NUMBER "1.."` + `OP "<"` + `NUMBER "10"`) instead of `NUMBER "1"` + new
      range `OP`. Fixed by stopping number consumption when a `.` is followed
      by another `.` — a decimal point is never followed by a second `.` in
      any of C/C++/Java/Kotlin, so this is safe for all four languages.
      Verified via direct `TokenizerCore` dump (all Kotlin operators lex to
      the expected token stream) and `make test` (25/25 C/C++/Java fixtures
      unaffected, including the fixture with the most numeric-literal density).
- [x] Add a Kotlin keyword set (`KEYWORDS_KOTLIN`), parallel to
      `KEYWORDS_JAVA`/`KEYWORDS_CPP` — includes all hard keywords plus the
      modifier/soft keywords listed in the checklist's original "at minimum"
      set (unconditionally reserved, same simplification already made for
      Java's `var`/`record`, both contextual in real Java but listed
      unconditionally in `KEYWORDS_JAVA`).
- [x] Add Kotlin named-construct detection (`NAMED_CONSTRUCT_KOTLIN` =
      `class`, `object`, `interface`, `enum`, `init`). Deliberately did **not**
      special-case `companion object`, `enum class`, or verify
      `computeConstructName()`'s lookback window for each shape yet — that
      cross-check against actual formatter behavior is Step 1's job (it
      re-examines every named-construct shape against the already-COMPLETE
      shared rule classes); adding it here would be guessing ahead of an
      actual failing case, which this step's own instructions warn against.
- [x] Re-run full existing C/C++/Java test suite. **25/25 pass, zero
      regressions** (24 pre-existing + the unrelated `real_code_regressions_13`
      fixture added the same session, before this Kotlin work started).
- [x] **Follow-up (surfaced during Step 1's §13 cross-check):** added
      `"in"`/`"out"` to `GENERIC_SAFE_KEYWORDS` so `reclassifyAngleBrackets`
      correctly recognizes declaration-site variance (`Box<out T>`,
      `Comparable<in T>`) as a generic `<`/`>` pair rather than a comparison.
      Pure no-op for C/C++/Java (neither keyword exists in their keyword
      sets). `make test` 32/32 before and after. RDD_KEY_113.
- [x] **Follow-up (surfaced during Step 1's §19 cross-check):** added a
      Kotlin-only interpolation-aware string scan (`skipKotlinString` /
      `skipKotlinInterpolationBlock` / `skipKotlinChar`) inside
      `emitString()`, gated behind `lang.isKotlin` — the shared naive
      scan-to-next-`"` misread a nested string inside a `${...}`
      interpolation (`"${foo("x")}"`) as three tokens instead of one,
      confirmed via harness before writing the fix. Depth-tracks `${...}`'s
      own `{`/`}` nesting (so a lambda literal inside the interpolation
      doesn't break early either) and recurses for any nested string/char
      literal, arbitrarily deep. Non-Kotlin scan left byte-for-byte as the
      original. `make test` 32/32 before and after. RDD_KEY_116.
- [x] **Follow-up (row 19.1, investigated on explicit request):** added
      Kotlin-only raw-string support (`isKotlinRawStringOpener` /
      `emitKotlinRawString` / `skipKotlinRawString`), checked in the main
      dispatch before the plain-`"` and C/C++ raw-string-prefix branches.
      Confirmed via harness the naive path was badly broken: a `"""..."""`
      raw string mis-lexed into multiple STRING/IDENTIFIER tokens, and a
      multi-line one leaked a spurious `NEWLINE` token into its own content.
      No backslash-escape processing (literal `\` by design); terminates
      greedily at the first `"""` encountered, matching the real Kotlin
      compiler. `skipKotlinInterpolationBlock` (RDD_KEY_116) extended to
      recognize a nested raw string inside an interpolation expression too.
      Non-Kotlin paths (Java text block, C++ raw string, plain C string)
      confirmed untouched. `make test` 32/32 before and after. RDD_KEY_117.

### Step 1 — Scoping Pass (mirrors `JavaSpecificRule.java`'s own scoping, RDD_KEY_59)

- [x] Cross-check every section of `STYLE_KOTLIN.md` and `STYLE_KOTLIN2.md`
      against the already-COMPLETE shared rule classes (`DeclarationAlignmentRule`,
      `BlockStructureRule`, `SwitchRule`, `GetterSetterRule`, `MiscRule`) to
      determine, per section: (a) already satisfied as-is by shared logic once
      Step 0's tokenizer work lands, (b) satisfied by a small additive
      extension to a shared class, or (c) needs a new method in
      `KotlinSpecificRule.java`. Table below.
- [x] Flag anything found during scoping that would require changing
      already-COMPLETE shared-class *behavior* (not just adding to it) — see
      **Open Questions** below: `DeclarationAlignmentRule`'s `Declaration`
      model assumes C/Java's `[modifiers] Type name [= init]` token order,
      which is structurally reversed from Kotlin's `[modifiers] val/var name : Type
      [= init]`. Stopped here rather than guessing a direction.

**Scoping table** (section numbers match `STYLE_KOTLIN.md`; `K2.N` = `STYLE_KOTLIN2.md` §N):

| § | Topic | Outcome | Notes |
|---|---|---|---|
| 1 | Semicolons (strip optional `;`) | (c), **done** | No shared class strips statement-terminating `;` for any language today (C/Java require it) — Kotlin-only `KotlinSpecificRule.stripOptionalSemicolons` pass. An earlier-session version of this method (`b0e778f`) only protected the enum-with-members mandatory `;` and stripped every other `;` unconditionally, silently mis-handling the deliberate-same-line-multi-statement case — rewritten around a single positive-evidence rule (`isTrailingSemicolon`: only strip a `;` that's the last significant thing on its line), which naturally keeps a same-line multi-statement `;` untouched with no special-casing, plus reuses §2's `findEnumConstantListTerminators` for the enum-mandatory-`;` exclusion. RDD_KEY_115. |
| 2 | `enum class` with members | (a)/(c), **done** | The `"enum class " + name` closing-comment label already falls out of `BlockStructureRule.classifyNamed`'s existing "keyword before `class` is `enum`" check (originally written for C++) — works for free once `enum`/`class` are both Kotlin keywords (Step 0, done). The body-open/close blank lines are already produced for free by the shared `insertNamedConstructBlankLines` — verified via harness, zero changes. The blank-line emphasis around the entry-list-terminating `;` itself is a separate pass, not covered by that method; implemented as new `KotlinSpecificRule.separateEnumConstantListTerminator` (+ helpers), mirroring `JavaSpecificRule.separateEnumConstantListTerminator`. See RDD_KEY_111. |
| 3 | Brace style (Allman fn bodies / K&R everything else) | (a) for K&R-enforcement direction, **verified**; (c) for Allman-conversion direction, **done** | `BlockStructureRule.qualifiesForKAndR`'s `PAREN_KR_KEYWORDS`/`BARE_KR_KEYWORDS` sets already cover Kotlin's exact same control-flow keyword vocabulary (`if/while/for/switch/catch`, `else/do/try/finally`) — confirmed via harness, K&R already gets correctly enforced onto a same-line-with-`)`-before-K&R-construct brace. The *other* direction — converting a function body's brace from K&R to Allman — needed a new Kotlin-only method: `KotlinSpecificRule.enforceFunctionDefinitionAllmanBraceStyle`, mirroring `JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle`/`CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle` but with a much more conservative candidate signal, since Kotlin's trailing-lambda call syntax (`someCall(args) { ... }`) is token-shape-identical to a function definition's body brace and Kotlin has no `new` keyword to rule ordinary calls out the way Java/C++ do — requires a backward scan from the candidate name (through an optional extension-receiver chain and/or `<T>` clause) to land exactly on `fun`, or the token before `(` to be `constructor` itself; anything else bails, same posture as an ordinary call being left untouched. Also handles a `: ReturnType` sitting between `)` and `{`, and a one-liner body staying K&R (RDD_KEY_75/RDD_KEY_89 exception, same as Java/C++). RDD_KEY_114. |
| 3.1 | Class/Object/Companion Object bodies | (b), **done** | Named `class Foo {`/`object Foo {` already worked. Headless gap (anonymous `companion object {}`, anonymous `object : Interface {}`, `init {}` never arming `pendingNamedConstructName`) fixed via `RDD_KEY_99`: additive `BlockStructureRule.classifyKotlinHeadlessNamed`, gated by new `Lang.isKotlin`, parallel to the existing `isAnonymousClassBrace` precedent. Also fixed a related tokenizer bug found during verification (see RDD_KEY_99): `:` was wrongly arming a following supertype identifier as the construct's own name. |
| 3.2 | `catch`/`for`/`while`/`when` no space before `(` | (b), **done** | Added `"when"` to `MiscRule.TIGHT_PAREN_KEYWORDS` — RDD_KEY_100. Pure no-op for C/C++/Java (no `when` keyword/token in any of their keyword sets). |
| 3.3 | Secondary constructors (Allman body) | (c), **done** | Covered by the same §3 method (`KotlinSpecificRule.enforceFunctionDefinitionAllmanBraceStyle`) in one pass, as planned: a secondary constructor is recognized by the token immediately before `(` being the `constructor` keyword itself. Verified via harness: `class Foo { constructor(x: Int) { ... } }` correctly converts the constructor body to Allman. RDD_KEY_114. |
| 3.4 | `init` blocks | (b), **done** | Same headless-named-construct fix as §3.1 — `init {}` now returns `"init"` from `classifyKotlinHeadlessNamed`, grouped in the same `RDD_KEY_99` commit. |
| 4 | `when` expression (arrow alignment, closing comment, blank lines) | (c) | `SwitchRule.java` turned out to be colon-form-statement-only (STYLE.md §13), unrelated; the real arrow-form logic is `JavaSpecificRule.enforceSwitchExpressionArrowAlignment`, but its `case`/`default`-keyword label scan and all-or-nothing block-body bailout both don't fit Kotlin's keyword-less, non-all-or-nothing `when` — implemented as new `KotlinSpecificRule.formatWhenExpressions` instead. RDD_KEY_101. Body-squishing non-idempotency bug fixed under RDD_KEY_121 (root cause was `KotlinDeclarationAlignmentRule`, not this method). |
| 5 | Null-safety operators (`?.`/`!!` tight, `?:` spaced) | (c), **done** | New `KotlinSpecificRule.enforceNullSafetyOperatorSpacing` — a single flat whole-file whitespace-collapsing pass, not scoped to any one construct, since no shared class does general expression-level operator re-spacing today. RDD_KEY_102. |
| 6 | Variable/property declaration alignment | (c), **done** | New `KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule` (user-directed: loosen shared-class visibility, then extend, rather than an independent parser in `KotlinSpecificRule.java`). Reuses `splitStatements`/`hasBlankLineBefore`/`hasCommentBefore`/`significantOnly`/`renderTokens`/`findTrailingComment` (raised private → protected, no behavior change) plus `ColumnGrid`/`KotlinModifierPriority`; writes its own `KotlinDecl` model, `splitKotlinStatements` (newline-terminated statement splitting — Kotlin has no `;`), `parseKotlinDeclaration`, and `renderPropertyGroup` (per-column `ColumnGrid`, not `Declaration`/`render()`). RDD_KEY_103. |
| 7 | Constructor/function parameter lists | (c), **done** | Same reversed-grammar issue as §6, in `MiscRule.Param`/`Signature` instead of `DeclarationAlignmentRule.Declaration`. Fixed as `RDD_KEY_104` — new `KotlinSignatureRule extends MiscRule`, same visibility-loosen-then-extend pattern as §6. |
| 7.1 | Named/default arguments (`=` spacing/alignment) | (c), **done for declarations** | Folded into §7's `KotlinSignatureRule.parseKotlinParam`/`render` — a default value is just one more optional trailing part of a single parameter's grammar, so no separate method was needed. **Not covered:** the call-site named-argument shape (`foo(x = 1, y = 2)`) shown in STYLE_KOTLIN.md §7.1's own worked example is a function *call*, not a declaration — no type column, different token shape (`name = value` only) — genuinely out of `KotlinSignature`'s scope as parsed here; would need its own small parser/renderer analogous to `MiscRule`'s `renderCallOnePerLine`/`renderCallPreserveGroups` family if picked up later. |
| 7.2 | Trailing comma (preserved as-is) | (a), **verified** | No existing pass adds or strips a trailing comma in any parameter/argument list for any language — trivially satisfied by doing nothing. Confirmed via harness: `KotlinSignature.trailingComma` round-trips correctly through `KotlinSignatureRule.render` for `fun foo(x: Int,)`. |
| 8 | Property accessors (`get`/`set`, preserve expression/block form) | (a)/(c), **done for the plain expression-bodied getter shape (RDD_KEY_133); block-bodied/setter/initializer shapes remain preserve-as-written-only** | "Preserve as-is" is satisfied by not writing code that touches it. One risk checked: `BlockStructureRule.collapseSingleExpressionBlocks`'s `SINGLE_EXPR_KEYWORDS` is `{if, while, for}` only — an accessor's `set(v) { field = v }` block body is never a match, so it won't get wrongly collapsed to bare-statement form. Confirmed via harness for both a block-bodied `set(v) { field = v }` (left completely untouched, including its own Allman `{`, since §3's Allman-conversion gap applies here too but the *block form itself* isn't collapsed) and an expression-bodied `get() = computeY()` (untouched). **Grouping fixed under RDD_KEY_133** for the plain getter-only shape (no initializer, no `set`, no block body): new `parseKotlinAccessorMember`/`renderAccessorGroup` in `KotlinGetterSetterRule.java` merges an adjacent group of `val x: Int` / `get() = expr` two-line units onto one Kotlin-legal line each and column-aligns them, mirroring §9's grid shape. Block-bodied accessors, getter+setter pairs, and initializer+accessor properties remain unhandled — still correctly "preserved as written," only the alignment upgrade is out of scope for those wider shapes. See Open Questions for the full narrative. |
| 9 | Expression-bodied functions | (a)/(c), **done, including grouping** | "Preserve as-is" part is free (same reasoning as §8). The "wrap `= expr` onto its own line if signature-breaking alone isn't enough" part implemented as new `KotlinSignatureRule.FunctionTail`/`parseFunctionTail`/`renderWithTail`, a three-tier fallback delegating to §7's existing `render` for the middle tier. Also fixed a **shared-class bug** this work surfaced: `MiscRule.isTightToken` was collapsing Kotlin multiplication spacing (`x* x`), gated off for Kotlin. See RDD_KEY_112. **Grouping fixed under RDD_KEY_132**: new `KotlinGetterSetterRule extends GetterSetterRule` column-aligns adjacent one-liner expression-bodied functions, mirroring Java's equivalent behavior. §8 (`get()`/`set()` accessor one-liners) remains a separate, still-open gap — see that row. |
| 10 | `for` loops and ranges | (a)/(c), **done** | Tight/loose paren-padding itself is already generic (`ComplexityPaddingEvaluator`, STYLE.md §3.1) — `in`/`until`/`downTo`/`step` turned out to already be inert w.r.t. its nested-bracket detection with zero code changes (`in` is `TokenType.KEYWORD`, the other three are plain `TokenType.IDENTIFIER`, confirmed via harness — reclassified (b)→(a)). The `..`/`..<` range operator's own *tight* spacing needed new code, same kind of gap as §5 — new `KotlinSpecificRule.enforceRangeOperatorSpacing`. RDD_KEY_110. |
| 11 | Labeled jumps (`@label` spacing) | (c), **done** | New `KotlinSpecificRule.enforceLabeledJumpSpacing` — a small left-to-right state machine over a flat whole-file token pass (same shape as §5/RDD_KEY_102), telling a jump's `@label` (tight both sides) apart from a declaration's `label@` (tight before, spaced after) apart from an unrelated annotation `@Foo` (untouched). RDD_KEY_105. |
| 12 | Destructuring declarations | (c), **done** | LHS is a parenthesized name list (`(a, b) = pair`), not `MiscRule.Assignment`'s assumed single `target` token — implemented directly in `KotlinDeclarationAlignmentRule.java` (reuses its existing §6 infrastructure) as new `DestructuringDecl`/`groupDestructuringDeclarations`/`parseDestructuringDeclaration`/`renderDestructuringGroup`. Comma spacing is normalized for free as a side effect of rebuilding `lhsText` from the parsed component list, not a passive default. RDD_KEY_107. **Group-stream merge revised under RDD_KEY_126:** now merges into the same column-aligned group as an adjacent plain §6 declaration (per user request, C++ structured-bindings precedent) instead of staying in its own never-merged stream. |
| 13 | Generics variance (`in`/`out`) | (b), **done** | `TokenizerCore.GENERIC_SAFE_KEYWORDS` extended with `"in"`/`"out"` — confirmed via harness these were previously misread as comparison `OP` tokens, now correctly `ANGLE_BRACKET_OPEN`/`_CLOSE` for `Box<out T>`/`Comparable<in T>`/`Pair<in T, out U>`; plain comparisons unaffected. Pure no-op for C/C++/Java (neither keyword exists in their keyword sets). Tokenizer-level fix, no rendering/spacing pass needed beyond correct classification. RDD_KEY_113. |
| 14 | Generic `where` clause | (c), **done** | Structural analog exists in `CppSpecificRule.java`'s trailing-`requires`-clause handling, but that's a per-language file, not shared — implemented as new `KotlinSpecificRule.enforceWhereClausePlacement`, using the C++ method as a reference pattern per this row's own note. RDD_KEY_106. |
| 15 | Infix functions (modifier slot; call-site spacing) | (a), **verified** | Modifier slot itself is Step 2 (`KotlinModifierPriority`) scope, not Step 1. Call-site word-operator spacing (`3 times "abc"`) is ordinary expression spacing, already left alone by every shared class (same reasoning as §5's baseline, no active interference to worry about). Confirmed via harness. |
| 16 | Annotation use-site targets (`@field:` tight `:`) | (c), **done** | No existing annotation-colon handling (Java annotations have no use-site-target shape) — new `KotlinSpecificRule.enforceAnnotationUseSiteTargetSpacing`, a flat whole-file state-machine pass. RDD_KEY_108. |
| 17 | Lambda-with-receiver / function types (exempt from nesting detector) | (b), **done** | `ComplexityPaddingEvaluator.isLoose` (shared) extended to skip a `.`-preceded/`->`-followed `(...)` span rather than counting it as nesting — pure no-op for C/C++/Java, `make test` 32/32 before/after. Known Gap (function type nested as a parameter of another) deliberately left unhandled, per the style doc's own text. RDD_KEY_109. |
| 17.1 | Lambda parameter arrow spacing | (c), **done** | New `KotlinSpecificRule.enforceArrowSpacing` — flat whole-file single-space-both-sides pass over every `->`, covering the function-type arrow (§17) and lambda-parameter arrow (§17.1) together as "one consistent arrow-spacing rule." Excludes `when`-branch arrows via `collectWhenBranchArrowIndices` (owned by §4's alignment instead). RDD_KEY_109. |
| 18 | `vararg` | (a), **verified** | Modifier-slot handling is Step 2 scope; no general spacing concern beyond that. Confirmed via harness that a `vararg` param itself is inert to every brace-style pass tried. |
| 19 | String templates (preserve `"$x"`/`"${x}"` exactly) | (c) — **tokenizer-level, done** | Investigated and confirmed the flagged risk was real: `TokenizerCore.emitString()`'s naive scan-to-next-`"` misreads `"${foo("x")}"` as three tokens instead of one correctly-bounded `STRING` token, since a nested string inside a `${...}` interpolation terminates the scan early — confirmed via a token-dump harness before writing any fix. This is a genuine correctness risk (not cosmetic): with the string's own boundary lost, a later spacing pass could insert whitespace *inside* the literal's actual text. Fixed with a Kotlin-only path (`skipKotlinString`/`skipKotlinInterpolationBlock`/`skipKotlinChar`, gated behind `lang.isKotlin`, non-Kotlin scan left byte-for-byte as the original) that depth-tracks `${...}`'s own `{`/`}` nesting (so a lambda literal inside the interpolation, `"${list.map { it * 2 }}"`, doesn't break early either) and recurses back into itself for any nested string/char literal encountered along the way, arbitrarily deep. Bare `$x` needed no special handling — it introduces no nesting risk. RDD_KEY_116. Triple-quoted raw strings (`"""..."""`) are a related but explicitly out-of-scope risk surfaced during this investigation — see new row 19.1 below; neither style doc mentions them and they have entirely different lexical rules, so left as a flagged, not-yet-investigated gap rather than folded into this fix. |
| 19.1 | Triple-quoted raw strings (`"""..."""`) | (c) — **tokenizer-level, done** | Investigated on explicit request. Confirmed via harness this was badly broken, not merely unhandled: `"""hello "world" end"""` mis-lexed as five tokens (`""` / `"hello "` / a bare `IDENTIFIER world` / `" end"` / `""`), and a multi-line raw string mis-lexed a spurious `NEWLINE` token into the middle of what should be one opaque string. Fixed with `isKotlinRawStringOpener`/`emitKotlinRawString`/`skipKotlinRawString` (Kotlin-only, checked before the plain-`"` and C/C++ raw-string branches): no backslash-escape processing (`\` is literal, by design), and greedy termination at the first `"""` encountered (matches the real Kotlin compiler — four trailing quotes closes at the first three, leaving one stray `"` token, which is correct). `${...}` interpolation still recognized via the existing `skipKotlinInterpolationBlock` (RDD_KEY_116), extended with a `"""`-lookahead so a nested raw string inside an interpolation expression is also recognized rather than misread. Verified via a 14-case harness (embedded quote runs, multi-line, literal backslash, plain/nested/doubly-nested interpolation, the 4-trailing-quotes edge case, unterminated input, plus Java text block / C++ raw string / plain C string sanity checks all confirmed untouched). `make test` 32/32 before and after. RDD_KEY_117. |
| 20 | Sealed classes/interfaces | (a), **verified** | Normal `class`/`object` K&R rules apply unchanged, no special layout. Confirmed via harness: `sealed class Result { ... }` gets the same K&R brace + closing comment (`} // class Result`) as a plain `class`. |
| 21 | Type aliases | (a), **verified** | Single-line `=`-spaced statement, no new behavior. Confirmed via harness: `typealias Handler = (Int) -> Unit` passes through every brace-style pass untouched. |
| 22 | Extension functions | (a), **verified** | `fun` behaves like any other modifier/keyword token for spacing purposes. Confirmed via harness: `KotlinSignatureRule.parseKotlinSignature` correctly parses `fun String.reverseWords()`, placing `fun String .` in `leadTokens` and `reverseWords` as the signature name — the receiver-type-before-name shape needs no special-casing beyond what §7's existing name-detection (IDENTIFIER immediately before the first depth-0 `(`) already does. |
| 23 | Known Gaps | (a), excluded | Explicitly out of scope, same posture as STYLE_JAVA.md's own excluded "unresolved" section (RDD_KEY_59). |
| K2.1 | Guard conditions in `when` | (a), **verified** | Extends §4's arrow-alignment logic as-is per the style doc — no new rule. Confirmed by direct harness test (not committed): `KotlinSpecificRule.formatWhenExpressions` (§4/RDD_KEY_101) already handles a guarded branch (`is String if x.isNotEmpty() -> foo()`) correctly with zero code changes, since the guard's `if <condition>` is just ordinary label text scanned up to the first top-level `->` — `->` alignment, forced blank lines, and the closing comment all work identically whether or not a branch carries a guard. Output matched STYLE_KOTLIN2.md §1's own worked example byte-for-byte, including a second harness case with multiple guarded branches confirming the arrow column stays aligned across guarded and unguarded branches together. |
| K2.2 | `data object` | (a), **verified** | Formatted exactly like `object` — a *named* `data object Singleton { ... }` isn't even a headless-object shape (§3.1's fix was for anonymous/headless cases), it's an ordinary named construct with an extra leading modifier, already handled by the existing named-construct path. Confirmed via harness: `data object Singleton { val x = 1 }` gets the same blank lines + closing comment (`} // Singleton`) as any other named `object`. |
| K2.3 | Other 2.0/2.1 features | (a), excluded | Explicitly "no new formatting rules" in the style doc itself. |

### Step 2 — `KotlinModifierPriority.java`

- [x] Column order for Kotlin's modifier set (`public/private/protected/
      internal`, `open/final/abstract/sealed`, `override`, `const`,
      `lateinit`, `val`/`var` sharing one slot per STYLE_KOTLIN.md §6) —
      confirm no cross-declaration-kind conflict analogous to the one resolved
      for Java in RDD_KEY_83 before assuming a single flat map suffices.
      **No such conflict found**: unlike Java's `abstract`/`volatile` case
      (where a single rank for `abstract` forced an unwanted rank shift for
      `volatile` on fields), none of Kotlin's modifiers here need a *different*
      relative order depending on which declaration kind they appear on —
      `const` (properties only), `lateinit` (var properties only), `override`
      (members only), and `open`/`final`/`abstract`/`sealed` (mutually
      exclusive modality, one or none per declaration) never fight over
      column order across kinds. Implemented as
      `grid/KotlinModifierPriority.java`: columns 0 (visibility) / 1 (modality:
      `open`/`final`/`abstract`/`sealed`, shared) / 2 (`override`) / 3
      (`const`) / 4 (`lateinit`) / 5 (`val`/`var`, shared). Compiles clean
      standalone; not yet wired into any rule class (that's Step 3's job, once
      `KotlinSpecificRule.java` exists to use it).

### Step 3 — `KotlinSpecificRule.java`

- [x] Implement each section flagged "(c)" in Step 1's scoping table, one
      section at a time, each as its own checkpoint commit.
      §8/§9 one-liner getter/setter grouping: §9 (expression-bodied
      functions) fixed via `KotlinGetterSetterRule`, RDD_KEY_132. §8
      (plain expression-bodied `get()` accessors) fixed via the same class,
      RDD_KEY_133 — block-bodied/setter/initializer accessor shapes remain
      an open, documented gap (see Open Questions) — every other flagged
      section is done.
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
      `KotlinGetterSetterRule extends GetterSetterRule`. §9 (expression-bodied
      functions) fixed under RDD_KEY_132. §8 (plain expression-bodied
      `get()` property accessors, no initializer, no `set`) fixed under
      RDD_KEY_133. Block-bodied accessors, getter+setter pairs, and
      initializer+accessor properties remain unhandled (preserved as
      written, not grouped) — see Open Questions.

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
      the `Makefile`'s `INP_FILES` (no longer commented out); round-trips
      clean, both the forward (`inp`→`out`) and idempotency (`out`→`out`)
      passes. Every punch-list item below is resolved except item 2
      (`result1`/`result2` fixture naming, resolved by the user directly in
      the fixture, not a formatter change) and item 4 (trailing whitespace on
      the EOF blank line, already correctly flagged as intentional test
      content, not a bug).
- [x] `test/kt_comments_inp.kt` / `test/kt_comments_out.kt` — **DONE.**
      Enabled in the `Makefile`'s `INP_FILES`. All four bugs found by the
      earlier manual check were root-caused and fixed; round-trips clean,
      both the forward (`inp`→`out`) and idempotency (`out`→`out`) passes.
      See punch list below. RDD_KEY_131.
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

**Status: dogfood tree now compiles clean end-to-end (see below) — Step 5's
core goal is met.** Any future session picking this up should treat it as
regression-watching / further polish, not a known-broken state. The
formatting-and-idempotency-check pass ran through many rounds of fixes
(RDD_KEY_134 through RDD_KEY_144). All 9 originally-flagged non-idempotent
dogfood files, including both of `BlockPalette.kt`'s diffs, are resolved.
`./gradlew compileDebugKotlin` against the fully-formatted dogfood tree was
then attempted and **failed** with ~50 Kotlin compiler errors across 9
files (`BlockPalette.kt`, `ConnectTypeDialog.kt`, `MainToolbar.kt`,
`MainViewModel.kt`, `ToolbarActions.kt`, `WifiApDialog.kt`,
`WifiStaDialog.kt`, `BlePermissions.kt`, `XMLSaveLoad.kt`) — confirming the
formatter itself was introducing first-pass compile regressions, distinct
from (and more severe than) the idempotency-only bugs found so far.
**RDD_KEY_141 fixed one confirmed, severe root cause**: a fluent method
chain of the shape `IDENTIFIER1().IDENTIFIER2 { lambda }` (first call
genuinely empty-parens, second call using Kotlin's bare/parenless
trailing-lambda call syntax) was silently having its `.IDENTIFIER2` text
deleted on the very first format pass — `ScopePipeline.applySignaturePass`'s
Kotlin `: ReturnType` tail detection (`findLastTopLevelCloseParen`) matched
ANY top-level `)` in range with no check that a `:` actually followed it,
misreading the first call's `)` as a signature's parameter-list close and
the second call's name as a return-type tail. Confirmed on real dogfood
files `ConnectTypeDialog.kt` and `WifiApDialog.kt`
(`.show().also { ... }` → `.show() { ... }`, losing `.also`). Fixed by
requiring a genuine top-level `:` immediately after the found `)` before
accepting the Kotlin return-type-tail branch. **Re-formatted the dogfood
tree with the RDD_KEY_141-fixed jar and re-ran `./gradlew
compileDebugKotlin`**: confirmed RDD_KEY_141 fully resolved
`ConnectTypeDialog.kt`, `WifiApDialog.kt`, `WifiStaDialog.kt`,
`BlePermissions.kt`, and `MainViewModel.kt` (none appear in the error list
any more). **Errors were then root-caused and fixed one file at a time, per this
project's established protocol (minimal repro → fix → fixture → RDD_LOG
entry → commit) across several follow-up sessions**:
`BlockCanvasView.kt`/`ToolbarActions.kt` (RDD_KEY_142, `label@` state-machine
false-positive on `@Annotation`); `Optimizer.kt` (RDD_KEY_143, `when`
expression's `else -> { ... }` arm wrongly matched by
`BlockStructureRule`'s bare-`else` braceless-collapse logic, flattening a
multi-statement block onto one line); `ProgramBuilder.kt` (RDD_KEY_144, two
unrelated bugs: `!is`/`!in` getting a spurious inserted space, and a call
argument `it.func.funcName` getting misparsed as a C-style typed declaration
once its enclosing call needed line-wrapping, also inserting a spurious
space). `MainToolbar.kt`'s and `BlockPalette.kt`'s errors turned out to
already be resolved by that point (not independently investigated further —
see the fresh whole-tree recompile below). **Fresh whole-tree recompile
confirms all originally-flagged files, and the entire dogfood tree, now
compile clean**: recreated the dogfood copy from the pristine original,
reformatted all 46 `.kt` files with the RDD_KEY_144-fixed jar, and ran
`./gradlew compileDebugKotlin` — `BUILD SUCCESSFUL`, zero errors (only two
pre-existing, unrelated deprecation warnings in `WifiStaDialog.kt` remain).
Step 5's dogfood/real-code testing goal (a full, real Android app's Kotlin
sources reformatted end-to-end and still compiling) is now met. Do not
touch `~/Projects/RobotCoding/gui_frontend_android` itself — only the
dogfood copy. **Caution established this session**: the dogfood copy at
`~/Projects/Shadow/rc_gui_frontend_android_DOGFOOD` can itself hold
*already-formatted, pre-fix* output from an earlier session's round1/round2
runs — always re-verify a fix against the true pristine originals under
`~/Projects/RobotCoding/gui_frontend_android` (read-only, never write
there), not just the dogfood copy, or a stale idempotency check can falsely
look unfixed (or falsely look fixed).

**Idempotency findings (round1 vs round2 diff, 46 `.kt` files under
`app/src/main/java`):** 9 files originally non-idempotent.
**RDD_KEY_134 fixed the root cause for 2** (`RobotTcpSession.kt` and
`WifiStaDialog.kt`'s `postDelayed` scan-timeout callback — both the same
"multi-line trailing-lambda call argument gets collapsed with no statement
separator" shape, confirmed compile-breaking). **RDD_KEY_135 fixed the
root cause for 2 more** (`PlayMusicBlock.kt` and `BleDeviceSelectDialog.kt`'s
`val filter` declaration — both the same §6 declaration-alignment
call-wrapped-initializer idempotency shape). **RDD_KEY_136 fixed the root
cause for 2 more** (`MainActivity.kt` and `BlePermissions.kt`'s
closing-brace indentation drift — a trailing lambda's `{` opening on a
continuation line of a multi-line fluent chain, deeper than the chain
statement's own first line; `ScopePipeline.processScope` derived the
lambda body's indent from the whole statement's first line instead of the
brace's own physical line). **`ToolbarActions.kt`'s own closing-brace-drift
diff is also confirmed fixed by RDD_KEY_136** (same root cause) — but
re-checking it against the true pristine original (not the stale dogfood
copy) surfaced a **different, previously-masked bug**: a severe
statement-joining-without-separator diff (see below), present in the
original all along but hidden behind the closing-brace-drift diff that
obscured the file's other differences. **RDD_KEY_137 fixed the root cause
for this statement-joining-without-separator bug**, resolving both
`MainViewModel.kt` and `ToolbarActions.kt` — root cause was
`BlockStructureRule.collapseSingleExpressionBlocks` mishandling Kotlin's
`if` as a value expression (`val display = (if (cond) a else b)`): a
wrapped, braceless expression-position `if`/`else` was misread as a
braceless *statement* body, swallowing the newline that separated it from
the following statement. Fixed with a Kotlin-only unmatched-paren-depth
gate. `BleDeviceSelectDialog.kt`'s `postDelayed { ... }` callback diff was
re-checked after RDD_KEY_137 landed and no longer reproduces —
round1-vs-round2 is now byte-identical and the callback's rendered content
is well-formed; most likely resolved incidentally by RDD_KEY_137's fix,
since the file's surrounding code also relies on if/else-expression
collapse. Not confirmed which exact prior fix resolved it, and no RDD_LOG
entry was added for that finding since no code change was needed; treated
purely as re-verification. **RDD_KEY_138 fixed the root cause for
`BlockCanvasView.kt`'s missing-space-before-`&&` diff**: a `val`/`var`
declaration initializer containing a top-level `&&` lost its preceding
space — root cause was `DeclarationAlignmentRule.isTightToken`'s
`Token.isRepOp(t, '&')` check being ungated by language (meant for C/C++'s
repeated pointer/reference sigils, but matching Kotlin's `&&` too); fixed
by adding the same `!lang.isKotlin` gate `MiscRule.isTightToken` already
had. **RDD_KEY_139 fixed the root cause for `BlockPalette.kt`'s §9
one-liner-function column-width-flapping diff**: a run of adjacent §9
expression-bodied one-liners had different alignment widths on round1 vs
round2 because `KotlinGetterSetterRule.parseKotlinOneLinerMember` lacked
the length pre-check `GetterSetterRule.parseOneLinerMember` (the
C/C++/Java base rule) already has for this exact reason — a too-long
member's body (containing a call a later phase might line-wrap) needs to
be excluded from the group at grouping time itself, on the very first
pass, or the padding decision goes stale once that later phase wraps it.
Fixed by porting the identical `hasBreakableCall` + estimated-width
pre-check into the Kotlin sibling method. **RDD_KEY_140 fixed the root
cause for `BlockPalette.kt`'s last remaining diff**, an Allman-brace
idempotency gap (`override fun draw(...) { _drawBlock(` on round1 vs.
round1's own output re-split onto `override fun draw(...)` + `{
_drawBlock(` on round2): `KotlinSpecificRule.isSingleLineBody` lacked the
same width-prediction pre-check `JavaSpecificRule.isSingleLineBody` already
has; fixed by porting `hasBreakableCall` + an estimated-width pre-check as a
duplicated local helper (adapted to this class's own helper API), and
correcting the width formula itself (ported from `JavaSpecificRule`) to
include indentation and inter-token spacing, which it had omitted — a
shortfall that mattered at this method's deeper object-nested indentation
even though it apparently didn't at `JavaSpecificRule`'s shallower call
sites. **All 9 originally-flagged non-idempotent dogfood files are now
resolved**; `BlockPalette.kt` is fully round1-vs-round2 idempotent.

- [ ] Once Steps 0–4 are complete, apply the same real-code-testing
      methodology `STATE.md` used for C/C++/Java (clone a real, compiling
      Kotlin project → format → idempotency check round1 vs round2 → compile
      with `kotlinc`) — deferred until the core checklist above is done, not
      started speculatively.

      Candidate **RobotCoding `gui_frontend_android`**
      (`~/Projects/RobotCoding/gui_frontend_android/app/src/main/java/*.kt`,
      not actually reachable via the `../../../../` relative path originally
      written here — that project lives outside the `JxMake` tree entirely,
      under `~/Projects/RobotCoding/`, a sibling of `~/Projects/JxMake/`) -
      NOT STARTED

      **Preparation done (this session), formatting pass itself still NOT
      STARTED:** dogfood environment is now set up —
      `~/Projects/Shadow/rc_gui_frontend_android_DOGFOOD` created as a `cp -r`
      of the original `~/Projects/RobotCoding/gui_frontend_android` (original
      untouched), one-time `gradle.properties` edit
      (`project.buildDir=build`) applied, Gradle 8.9/JDK 21 env sourced per
      the recipe above, and `./gradlew compileDebugKotlin` run against the
      copy's current (unformatted) source as a baseline. **Baseline compile
      is clean**: `BUILD SUCCESSFUL`, only two pre-existing deprecation
      warnings (`WifiStaDialog.kt:170`/`177`, unrelated `NsdManager` API
      usage), zero errors. This confirms a future format-then-compile pass
      can attribute any new compile failure to the formatter itself.

**Standalone `K2JVMCompiler` classpath — rejected, do not use.** The recipe
that used to live here (a bare `kotlin-compiler-embeddable` +
`kotlin-stdlib` classpath) cannot syntax-check this candidate: every file
under `gui_frontend_android/app/src/main/java/*.kt` imports `android.*` /
AndroidX APIs, which only exist in the Android SDK jars pulled in by the
project's own Gradle build — a bare compiler classpath with just the Kotlin
stdlib on it has no way to resolve those symbols, so it would fail on
essentially every real file in this project, not just report genuine syntax
errors.

**Use instead:** the project's own Gradle wrapper, via its own env script —
but run it against a **copy** of `gui_frontend_android/`, not the original
checkout in `~/Projects/RobotCoding/`. The dogfood workflow here is
format-then-compile, which writes formatted `.kt` files back to disk; doing
that against the real, in-use RobotCoding working tree risks clobbering
uncommitted work there. `gui_frontend_android/` is a standalone Gradle/Android
module — the rest of RobotCoding (used by the project's other, non-Android
parts) builds via `make` or Arduino tooling, not Gradle, so only
`gui_frontend_android/` itself needs to be copied; it does not depend on
sibling directories.

Copy it once into a **persistent** location —
`~/Projects/Shadow/rc_gui_frontend_android_DOGFOOD` — rather than `/tmp`, so
it survives reboots and doesn't need re-copying (and re-editing, see below)
every session. If this directory already exists, skip the copy step and go
straight to the one-time `gradle.properties` edit check / compile.

**One-time setup after the copy:** the original `gradle.properties` points
its build output at the real project's own external build dir
(`project.buildDir=~/Projects/Shadow/rc_gui_frontend_android_build`)
so it does not collide with anything under `~/Projects/RobotCoding/`. Since
that path is external to `gui_frontend_android/` itself, a plain copy would
still write build output back into the *original* project's build dir —
edit `gradle.properties` in the dogfood copy so `project.buildDir` is a
plain relative value instead:

```
project.buildDir=build
```

(Gradle resolves a relative `project.buildDir` against the project directory
it's declared in, so this keeps build output fully inside the dogfood copy
with no absolute path needed.) This only needs doing once per copy — if the
dogfood dir is ever deleted and recopied, redo this edit before compiling.

`gui_frontend_android/env.sh` sets `ANDROID_HOME` and puts Gradle 8.9 and
JDK 21 on `PATH` — source it (or replicate just its `export` lines; it also
`cd`s and `exec bash`s into an interactive shell, which isn't wanted for a
scripted run) from within the dogfood copy, then run the copy's own
`./gradlew` with a compile-only task, e.g.:

```bash
cp -r ~/Projects/RobotCoding/gui_frontend_android ~/Projects/Shadow/rc_gui_frontend_android_DOGFOOD
cd ~/Projects/Shadow/rc_gui_frontend_android_DOGFOOD
# one-time only: edit gradle.properties, set project.buildDir=build
export ANDROID_HOME=~/android_devel
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/gradle-8.9/bin
export PATH=/opt/openjdk-21_linux-x64_bin/jdk-21/bin:$PATH
./gradlew compileDebugKotlin
```

(`env.sh` ends by `exec bash` into an interactive session — for a
non-interactive/scripted run, source only its `export`/`cd` lines instead of
running the whole script.) This gives a real syntax+type check against the
actual Android SDK/AndroidX dependency graph the source expects, which the
rejected standalone recipe could not.

**When a bug is found and fixed, add a new permanent fixture pair:**
`test/real_code_regressions_N_{inp,out}.<ext>` (next available `N`) reproducing it minimally,
then register it in `Makefile`'s `INP_FILES` and document it in `test/README.txt` — unless,
per the precedent set by the `indent-size = 2` config-wiring fixes, the bug is a no-op at the
test harness's own default config (in which case document the fix and its non-default-config
verification in this file instead, without adding a fixture that would be indistinguishable
from a no-op at default settings). Try to combine multiple bugs in the same text fixture if
possible. Use this standard copyright header on every new test fixture file:
```
/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
```
