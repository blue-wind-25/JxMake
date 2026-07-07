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
full.** Run `grep -Fm1 'ClassName' STATE_rdd_log.md` (substitute the class or
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
  Once resolved: append the full decision to `STATE_rdd_log.md` (next
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
        KotlinModifierPriority.java     ← NOT STARTED
      rules/
        KotlinSpecificRule.java         ← NOT STARTED
  test/
    kt_combined_inp.kt / kt_combined_out.kt   ← NOT STARTED
    kt_comments_inp.kt / kt_comments_out.kt   ← NOT STARTED
```

Existing shared files listed under Hard Constraint above are modified
in-place, additively, when Kotlin needs a shared capability they don't yet
have (e.g. a new operator token) — they are not duplicated per-language.

---

## Resolved Design Decisions

Full text of each decision lives in `STATE_rdd_log.md` (shared with
`STATE.md` — continue its existing `RDD_KEY_n` numbering, do not restart).
Look up one key at a time via `grep -Fm1 'RDD_KEY_n' STATE_rdd_log.md`
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
| RDD_KEY_107 | Kotlin destructuring declarations — §12; new `DestructuringDecl`/`groupDestructuringDeclarations`/`parseDestructuringDeclaration`/`renderDestructuringGroup` in `KotlinDeclarationAlignmentRule.java` (not a new file — reuses that class's existing §6/RDD_KEY_103 infrastructure); single pre-rendered `lhsText` cell, no per-component type grid, since §12 has no type annotations to anchor one; own group stream, never merged with §6's |
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

---

## Open Questions

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
| 4 | `when` expression (arrow alignment, closing comment, blank lines) | (c), **done** | `SwitchRule.java` turned out to be colon-form-statement-only (STYLE.md §13), unrelated; the real arrow-form logic is `JavaSpecificRule.enforceSwitchExpressionArrowAlignment`, but its `case`/`default`-keyword label scan and all-or-nothing block-body bailout both don't fit Kotlin's keyword-less, non-all-or-nothing `when` — implemented as new `KotlinSpecificRule.formatWhenExpressions` instead. RDD_KEY_101. |
| 5 | Null-safety operators (`?.`/`!!` tight, `?:` spaced) | (c), **done** | New `KotlinSpecificRule.enforceNullSafetyOperatorSpacing` — a single flat whole-file whitespace-collapsing pass, not scoped to any one construct, since no shared class does general expression-level operator re-spacing today. RDD_KEY_102. |
| 6 | Variable/property declaration alignment | (c), **done** | New `KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule` (user-directed: loosen shared-class visibility, then extend, rather than an independent parser in `KotlinSpecificRule.java`). Reuses `splitStatements`/`hasBlankLineBefore`/`hasCommentBefore`/`significantOnly`/`renderTokens`/`findTrailingComment` (raised private → protected, no behavior change) plus `ColumnGrid`/`KotlinModifierPriority`; writes its own `KotlinDecl` model, `splitKotlinStatements` (newline-terminated statement splitting — Kotlin has no `;`), `parseKotlinDeclaration`, and `renderPropertyGroup` (per-column `ColumnGrid`, not `Declaration`/`render()`). RDD_KEY_103. |
| 7 | Constructor/function parameter lists | (c), **done** | Same reversed-grammar issue as §6, in `MiscRule.Param`/`Signature` instead of `DeclarationAlignmentRule.Declaration`. Fixed as `RDD_KEY_104` — new `KotlinSignatureRule extends MiscRule`, same visibility-loosen-then-extend pattern as §6. |
| 7.1 | Named/default arguments (`=` spacing/alignment) | (c), **done for declarations** | Folded into §7's `KotlinSignatureRule.parseKotlinParam`/`render` — a default value is just one more optional trailing part of a single parameter's grammar, so no separate method was needed. **Not covered:** the call-site named-argument shape (`foo(x = 1, y = 2)`) shown in STYLE_KOTLIN.md §7.1's own worked example is a function *call*, not a declaration — no type column, different token shape (`name = value` only) — genuinely out of `KotlinSignature`'s scope as parsed here; would need its own small parser/renderer analogous to `MiscRule`'s `renderCallOnePerLine`/`renderCallPreserveGroups` family if picked up later. |
| 7.2 | Trailing comma (preserved as-is) | (a), **verified** | No existing pass adds or strips a trailing comma in any parameter/argument list for any language — trivially satisfied by doing nothing. Confirmed via harness: `KotlinSignature.trailingComma` round-trips correctly through `KotlinSignatureRule.render` for `fun foo(x: Int,)`. |
| 8 | Property accessors (`get`/`set`, preserve expression/block form) | (a), **verified** | "Preserve as-is" is satisfied by not writing code that touches it. One risk checked: `BlockStructureRule.collapseSingleExpressionBlocks`'s `SINGLE_EXPR_KEYWORDS` is `{if, while, for}` only — an accessor's `set(v) { field = v }` block body is never a match, so it won't get wrongly collapsed to bare-statement form. Confirmed via harness for both a block-bodied `set(v) { field = v }` (left completely untouched, including its own Allman `{`, since §3's Allman-conversion gap applies here too but the *block form itself* isn't collapsed) and an expression-bodied `get() = computeY()` (untouched). |
| 9 | Expression-bodied functions | (a)/(c), **done** | "Preserve as-is" part is free (same reasoning as §8). The "wrap `= expr` onto its own line if signature-breaking alone isn't enough" part implemented as new `KotlinSignatureRule.FunctionTail`/`parseFunctionTail`/`renderWithTail`, a three-tier fallback delegating to §7's existing `render` for the middle tier. Also fixed a **shared-class bug** this work surfaced: `MiscRule.isTightToken` was collapsing Kotlin multiplication spacing (`x* x`), gated off for Kotlin. See RDD_KEY_112. |
| 10 | `for` loops and ranges | (a)/(c), **done** | Tight/loose paren-padding itself is already generic (`ComplexityPaddingEvaluator`, STYLE.md §3.1) — `in`/`until`/`downTo`/`step` turned out to already be inert w.r.t. its nested-bracket detection with zero code changes (`in` is `TokenType.KEYWORD`, the other three are plain `TokenType.IDENTIFIER`, confirmed via harness — reclassified (b)→(a)). The `..`/`..<` range operator's own *tight* spacing needed new code, same kind of gap as §5 — new `KotlinSpecificRule.enforceRangeOperatorSpacing`. RDD_KEY_110. |
| 11 | Labeled jumps (`@label` spacing) | (c), **done** | New `KotlinSpecificRule.enforceLabeledJumpSpacing` — a small left-to-right state machine over a flat whole-file token pass (same shape as §5/RDD_KEY_102), telling a jump's `@label` (tight both sides) apart from a declaration's `label@` (tight before, spaced after) apart from an unrelated annotation `@Foo` (untouched). RDD_KEY_105. |
| 12 | Destructuring declarations | (c), **done** | LHS is a parenthesized name list (`(a, b) = pair`), not `MiscRule.Assignment`'s assumed single `target` token — implemented directly in `KotlinDeclarationAlignmentRule.java` (reuses its existing §6 infrastructure) as new `DestructuringDecl`/`groupDestructuringDeclarations`/`parseDestructuringDeclaration`/`renderDestructuringGroup`, a separate group stream from §6's own. Comma spacing is normalized for free as a side effect of rebuilding `lhsText` from the parsed component list, not a passive default. RDD_KEY_107. |
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

- [ ] Implement each section flagged "(c)" in Step 1's scoping table, one
      section at a time, each as its own checkpoint commit.
Full implementation/verification narratives for every checked item below have been
compacted out of this file — each is still fully recorded, in the same level of
detail, in its `RDD_KEY_n` entry in `STATE_rdd_log.md` (`grep -Fm1 'RDD_KEY_n'`).
- [x] **§1 Semicolons.** `KotlinSpecificRule.stripOptionalSemicolons` strips every
      optional statement-terminating `;`, keeping only an armed `enum class` body's
      entries/members separator when member declarations actually follow it. New
      file, no shared-class change. `make test` 25/25.
- [x] **§3.1/§3.4 Class/Object/Companion Object/`init` bodies.** `RDD_KEY_99` —
      shared-class extension (`Lang.isKotlin`,
      `BlockStructureRule.classifyKotlinHeadlessNamed`), plus a related tokenizer
      fix (`:` wrongly arming a supertype identifier as the construct's own name
      for anonymous `object : Super {}`). `make test` 25/25.
- [x] **§3.2 `when` no space before `(`.** `RDD_KEY_100` — added `"when"` to
      `MiscRule.TIGHT_PAREN_KEYWORDS`, a one-line shared-class change (pure no-op
      for C/C++/Java, no such keyword in their keyword sets). `make test` 25/25.
- [x] **§4 `when` expression (arrow alignment, closing comment, blank lines).**
      `RDD_KEY_101` — new `KotlinSpecificRule.formatWhenExpressions`, not a
      `SwitchRule`/`JavaSpecificRule` extension (keyword-less branch labels,
      non-all-or-nothing block-body alignment, and forced rather than merely
      preserved blank lines all differ from the Java arrow-switch precedent).
      Not wired into `Formatter.formatOne` yet. `make test` 25/25.
- [x] **§5 Null-safety operators (`?.`/`!!` tight, `?:` spaced).** `RDD_KEY_102` —
      new `KotlinSpecificRule.enforceNullSafetyOperatorSpacing`, a flat whole-file
      pass (no shared class does general expression-level operator re-spacing).
      `make test` 25/25.
- [x] **§6 Variable/property declaration alignment.** `RDD_KEY_103` — raised six
      `DeclarationAlignmentRule` helpers `private`→`protected` (additive, no
      behavior change), then `KotlinDeclarationAlignmentRule extends
      DeclarationAlignmentRule` with its own `KotlinDecl` model,
      `splitKotlinStatements` (newline-terminated, not `;`-terminated),
      `parseKotlinDeclaration`, and `ColumnGrid`-based `renderPropertyGroup`.
      User-directed: loosen shared-class visibility, then extend, rather than an
      independent parser. `make test` 25/25.
- [x] **§7/§7.1 Constructor/function parameter lists, named/default arguments.**
      `RDD_KEY_104` — same visibility-loosen-then-extend pattern as §6, six
      `MiscRule` helpers promoted, new `KotlinSignatureRule extends MiscRule` with
      its own `KotlinParam`/`KotlinSignature` model and `ColumnGrid`-based broken
      form; also covers §7.2 trailing-comma preservation. **Not covered**:
      call-site named arguments (`foo(x = 1, y = 2)`), a structurally different
      type-less shape. `make test` 32/32.
- [x] **§11 Labeled jumps (`@label` spacing).** `RDD_KEY_105` — new, fully
      self-contained `KotlinSpecificRule.enforceLabeledJumpSpacing` (no shared-class
      change needed at all), a flat whole-file pass with a small `JumpState`
      machine distinguishing a jump's `@label` (tight both sides) from a label
      declaration's `label@` (tight before, one space after) from an unrelated
      `@Annotation`. `make test` 32/32.
- [x] **§14 Generic `where` clause.** `RDD_KEY_106` — new, fully self-contained
      `KotlinSpecificRule.enforceWhereClausePlacement`, structurally mirroring
      `CppSpecificRule.enforceRequiresClausePlacement` (fits-inline vs. wraps to
      its own indented line, based on `lineLengthLimit`), but always breaking
      every bound one-per-line at the top-level comma (never within a bound)
      once wrapped, column-aligned under the first bound's start column, per
      STYLE_KOTLIN.md §14's own worked examples. Added a new
      `KotlinSpecificRule(Lang, int lineLengthLimit, int indentWidth)`
      constructor (this class's first method needing indent width, not just
      line length). No shared-class change. `make test` 32/32.
- [x] **§12 Destructuring declarations.** `RDD_KEY_107` — new `DestructuringDecl`
      model, `groupDestructuringDeclarations`/`parseDestructuringDeclaration`/
      `renderDestructuringGroup` added directly to `KotlinDeclarationAlignmentRule.java`
      (reuses that class's §6 infrastructure rather than a new file). Single
      pre-rendered `lhsText` cell (no per-component type grid, per §12's own
      "no type annotations to anchor a column grid" text) rendered via a
      two-column `ColumnGrid`, same shape as `MiscRule.Assignment`'s render.
      Own group stream, never merged with §6's property-declaration groups.
      No shared-class change. `make test` 32/32.
- [x] **§16 Annotation use-site targets.** `RDD_KEY_108` — new
      `KotlinSpecificRule.enforceAnnotationUseSiteTargetSpacing`, a flat
      whole-file state-machine pass (`@` → target → `:` → name) matching
      target keywords by token text (not `TokenType.KEYWORD`, since
      `delegate` isn't tokenizer-lexed as one). Tightens only the `:` on
      both sides; `@`-to-target spacing left unenforced (no textual
      backing, no codebase precedent for reformatting plain annotation
      spacing). No shared-class change. `make test` 32/32.
- [x] **§17/§17.1 Lambda-with-receiver / function-type nesting exemption +
      arrow spacing.** `RDD_KEY_109` — **shared-class change**:
      `ComplexityPaddingEvaluator.isLoose` extended to skip a `.`-preceded/
      `->`-followed `(...)` span (a lambda-with-receiver's own invocation
      parens) rather than counting it as nesting, so an enclosing
      parenthesized type annotation stays tight (`(StringBuilder.() -> Unit)`)
      instead of incorrectly loose-padding. Pure no-op for C/C++/Java
      (neither has this token shape); `make test` 32/32 before and after.
      Known Gap (function type nested as a parameter of another) deliberately
      left unhandled per the style doc. Also new Kotlin-only
      `KotlinSpecificRule.enforceArrowSpacing` + `collectWhenBranchArrowIndices`
      — flat whole-file single-space arrow pass covering both constructs,
      explicitly excluding `when`-branch arrows (owned by §4's column
      alignment) by index. `make test` 32/32.
- [x] **§10 `for` loops and ranges.** `RDD_KEY_110` — `in`/`until`/`downTo`/
      `step` reclassified (b)→(a): already inert w.r.t.
      `ComplexityPaddingEvaluator.isLoose` with zero code changes (`in` is
      `TokenType.KEYWORD`, the rest are plain `TokenType.IDENTIFIER`,
      confirmed via harness). New `KotlinSpecificRule.enforceRangeOperatorSpacing`
      tightens `..`/`..<` on both sides (simpler one-sided sibling of §5's
      state machine, no spaced variant to handle). No shared-class change.
      `make test` 32/32.
- [x] **§2 `enum class` with members.** `RDD_KEY_111` — closing-comment
      label and body-open/close blank lines already free (Step 0 +
      `insertNamedConstructBlankLines`, verified via harness, zero changes).
      New `KotlinSpecificRule.separateEnumConstantListTerminator` (+
      `findEnumConstantListTerminators`/`isEnumBodyBrace`/`prevSignificantIndex`)
      adds the blank-line emphasis around the mandatory entry-list-terminating
      `;`, mirroring `JavaSpecificRule.separateEnumConstantListTerminator`
      (per-language precedent, not a shared-class reuse — same reasoning as
      §14/RDD_KEY_106). Reuses this file's own existing `lineIndent` helper.
      Verified via harness: reproduces the style doc's worked example
      byte-for-byte; no-trailing-members, trailing-`;`-with-no-members-after,
      and trailing-comma-preservation cases all correctly left untouched. No
      shared-class change. `make test` 32/32.
- [x] **§9 Expression-bodied functions.** `RDD_KEY_112` — "preserve as-is"
      part free (same reasoning as §8). New `KotlinSignatureRule.FunctionTail`
      (parses `: ReturnType`/`= expr` after a signature's `)`) +
      `parseFunctionTail` + `renderWithTail`, a three-tier fallback: fits
      fully inline as written; else break params first (delegates straight
      to §7's existing `render`, unchanged) and append the tail if that now
      fits; else, only if expression-bodied, wrap `= expr` onto its own line
      indented one level (mirrors §7.1's named-argument `=`-wrap). An explicit
      return type with no `=` and still too long after breaking params is
      left as the combined line — nothing documented left to wrap for that
      shape. **Shared-class change**: `MiscRule.isTightToken`'s `*`/`&`
      tight-token treatment (a C/C++ pointer/reference-declarator convention)
      gated off for Kotlin via `!lang.isKotlin` — unconditionally applied, it
      was collapsing ordinary Kotlin multiplication spacing (`x * x` →
      `x* x`) in any expression rendered through the shared `renderTokens`,
      caught via a harness reproducing the style doc's own `x * x + y * y`
      worked example byte-for-byte. `make test` 32/32 before and after.
- [x] **§3/§3.3 Function/secondary-constructor body Allman-brace
      conversion.** `RDD_KEY_114` — new
      `KotlinSpecificRule.enforceFunctionDefinitionAllmanBraceStyle` (+
      `isFunctionOrConstructorCloseParen`/`findSignatureCloseParenBeforeBrace`/
      `isAngleOpen`/`isAngleClose`/`skipAngleBracketsBackward`), mirroring
      `JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle`/
      `CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle`'s overall
      shape but with a far more conservative candidate signal: Kotlin's
      trailing-lambda call syntax (`someCall(args) { ... }`) is token-shape-
      identical to a function definition's body brace, and Kotlin has no
      `new` keyword to rule ordinary calls out the way Java/C++ do, so a
      candidate is only accepted if a backward scan from the name (through
      an optional extension-receiver chain and/or `<T>` clause) lands
      exactly on `fun`, or the token before `(` is `constructor` itself —
      anything else bails, same "give up rather than guess" posture as
      `KotlinSignatureRule.parseKotlinSignature`. Also handles a
      `: ReturnType` sitting between `)` and `{`, and tolerates the
      tokenizer's non-reclassified plain-`OP` `<T>` right after `fun`
      (`fun <T> ...` doesn't get `ANGLE_BRACKET_OPEN`/`_CLOSE` the way
      `List<T>` does) — both discovered only via harness, not anticipated
      up front. One-liner bodies stay K&R (RDD_KEY_75/RDD_KEY_89 exception).
      Verified via an 11-case harness: plain function, secondary
      constructor, extension function, generic function, generic extension
      function (all K&R→Allman); already-Allman (idempotent); one-liner
      (stays K&R); trailing-lambda call (untouched, enclosing real function
      still converts); enum-entry anonymous body (untouched); control-flow
      block (untouched); plain call with no body (untouched). No shared-
      class change. `make test` 32/32.
- [x] **§1 Semicolon stripping.** `RDD_KEY_115` — re-examined the
      pre-existing `stripOptionalSemicolons` (committed earlier, `b0e778f`,
      before this session's own RDD-log/scoping-table-marker convention
      existed, hence the row still read plain "(c)" with no "**done**")
      rather than assuming it was already correct, and found a real bug: it
      only ever protected the enum-with-members mandatory `;` (§2) and
      stripped every other `;` unconditionally — including a deliberate
      same-line multi-statement `;` (`val a = 1; val b = 2`), which would
      have silently merged the two statements into one invalid line, not
      just a style nit. Rewrote around a single positive-evidence rule,
      `isTrailingSemicolon`: only strip a `;` that is the last significant
      thing on its physical line (next non-gap token, skipping whitespace/
      comments, either starts a new line or none remain) — this naturally
      leaves the multi-statement-same-line case untouched with no special-
      casing needed. Reuses §2/RDD_KEY_111's `findEnumConstantListTerminators`
      directly for the enum-mandatory-`;` exclusion rather than re-deriving
      a separate enum/class/brace-tracking state machine. Also fixed a
      stray-trailing-space gap the old version had (`foo() ;` → `foo() `
      instead of `foo()`) by dropping any whitespace immediately preceding a
      stripped `;` too. Verified via an 8-case harness: plain flat
      declarations (stripped); space-padded `;` (no stray trailing space);
      multi-statement-same-line (now correctly kept — the bug this re-check
      caught); trailing line comment after `;` (still stripped); enum with
      members after its mandatory `;` (kept); enum with a trailing `;` but
      no members after (stripped, optional); no semicolons at all
      (untouched); `;` at literal end-of-file (stripped). No shared-class
      change. `make test` 32/32.
- [x] **§19 String templates — tokenizer-level fix.** `RDD_KEY_116` —
      **shared-class change.** Confirmed the flagged risk was real: a nested
      string inside a `${...}` interpolation (`"${foo("x")}"`) terminated
      `TokenizerCore.emitString()`'s naive scan-to-next-`"` early, splitting
      the literal into three tokens instead of one. Fixed with a Kotlin-only
      `skipKotlinString`/`skipKotlinInterpolationBlock`/`skipKotlinChar`
      path, gated behind `lang.isKotlin`, that depth-tracks `${...}`'s own
      `{`/`}` nesting (so a lambda literal inside the interpolation doesn't
      break early either) and recurses for any nested string/char literal,
      arbitrarily deep. Non-Kotlin scan is byte-for-byte the original.
      Verified via an 11-case harness (bare `$x`; braced `${x}`; the
      original failing nested-string case; a lambda literal inside
      interpolation; two adjacent interpolation blocks; a doubly-nested
      string-inside-interpolation-inside-string; an unterminated string;
      plain string with no interpolation; escaped `\$`; a char literal with
      `\"` immediately before an interpolation containing its own char
      literal with a `"`; and a plain C string through the non-Kotlin path
      as a sanity check) — all round-tripped byte-for-byte and tokenized as
      expected. `make test` 32/32 before and after. Surfaces triple-quoted
      raw strings as a separate, undocumented, out-of-scope gap (row 19.1).

### Step 3.5 — Configuration Property Wiring

Found during a cross-check requested by the user: none of the properties below
are actually reachable for `.kt` files today, for one shared root cause —
`Main.java` has no Kotlin language dispatch yet (see Explicit Non-Goals: no
`Main.java` changes until Steps 0–4 are complete). `Config.java` already
parses all of them (`line-length`, `indent-size`, `indent-style`,
`closing-comment-min-lines`, `format-macros`, `line-endings`,
`normalize-comment-start-case`, `normalize-comment-end-period`), and the
existing Kotlin rule classes (`KotlinSpecificRule`, `KotlinSignatureRule`,
`KotlinDeclarationAlignmentRule`) already *accept* `lineLengthLimit`/
`indentWidth` as constructor params — but nothing in `Formatter.java`/
`ScopePipeline.java` constructs those rule classes with Kotlin's `Config`
values yet, since no pipeline path exists for the language at all.

- [ ] `line-length` / `indent-size` / `indent-style`: wire from `Config` into
      Kotlin rule construction once a Kotlin pipeline path exists (Step 4/5
      territory — depends on the `Main.java` wiring currently deferred).
- [ ] `closing-comment-min-lines`: confirm Kotlin's named-construct closing
      comments (§2/§3.1, `BlockStructureRule`-derived) respect this once wired
      — untested for Kotlin specifically.
- [ ] `format-macros`: likely a no-op for Kotlin (no preprocessor macros in the
      language) — confirm and document as intentionally inert rather than
      silently ignored.
- [ ] `line-endings`: shared/global concern (`Formatter.java` line-ending
      normalization) — confirm it already applies file-type-agnostically once
      Kotlin files flow through `Formatter.java` at all.
- [ ] `normalize-comment-start-case` / `normalize-comment-end-period`: confirm
      these apply to Kotlin's `//`/`/* */` comments unchanged (comment syntax
      itself is identical to Java) once wired.
- [ ] New Kotlin import-ordering properties, parallel to
      `java-import-order`/`java-import-sort`/`java-import-depth`/
      `java-import-blank-lines` (today handled inline in `Formatter.java`, not
      a separate rule class): add `kotlin-import-order`, `kotlin-import-sort`,
      `kotlin-import-depth`, `kotlin-import-blank-lines` to `Config.java`'s
      known-keys list and parsing, then implement the actual Kotlin `import`
      statement ordering/sorting/grouping logic (not started — no Kotlin
      import handling exists anywhere yet, this is genuinely new work, not
      just config plumbing).
- [ ] JXM_CFMT_DIS/JXM_CFMT_ENA marker-comment disabling and `--format-off`:
      the underlying implementation (`TokenizerCore`'s marker regexes,
      `ScopePipeline`'s frozen-region handling) is already shared and
      language-generic — no Kotlin-specific token shape breaks it. Currently
      unreachable for `.kt` files for the same `Main.java`-wiring reason as
      above. Confirm behavior with a real Kotlin fixture once wired; only then
      update `README.md`'s "Disabling formatting for part or all of a file"
      section to mention Kotlin (per the existing Explicit Non-Goal: no
      `README.md` update until Step 5's dogfood pass is clean).
- [ ] Update `README.md` for the new `kotlin-import-*` keys.

### Step 4 — Test Fixtures
- [ ] `test/kt_combined_inp.kt` / `kt_combined_out.kt` — first fixture pair,
      covering STYLE_KOTLIN.md's and STYLE_KOTLIN2.md's sections end to end,
      same methodology as the existing `*_inp/out` pairs for other languages.
- [ ] `test/kt_comments_inp.kt` / `kt_comments_inp.kt` — second fixture pair,
      for uncommon comment locations (including JXM_CFMT_DIS/JXM_CFMT_ENA),
      same methodology as the existing `*_inp/out` pairs for other languages.
- [ ] Additional fixture pairs as needed for KOTLIN2-specific constructs
      (guard conditions, `data object`).
- [ ] After every fixture addition or shared-class change: full existing
      C/C++/Java suite + new Kotlin fixtures, zero regressions.

### Step 5 — Dogfood / Real-Code Testing

- [ ] Once Steps 0–4 are complete, apply the same real-code-testing
      methodology `STATE.md` used for C/C++/Java (clone a real, compiling
      Kotlin project → format → idempotency check round1 vs round2 → compile
      with `kotlinc`) — deferred until the core checklist above is done, not
      started speculatively.

---

## Explicit Non-Goals (for now)

- No `Main.java` changes (`.kt`/`.kts` extension → language detection) until
  Steps 0–4 are complete.
- No `README.md`/`README.txt` update advertising Kotlin JAR support until
  Step 5's dogfood pass is clean — premature otherwise, same reasoning
  already applied to this session's own README.md/README.txt review.
- No link from `STATE.md`'s own Project Layout or checklist — explicit
  instruction, revisit only when told to.

---

## Handoff Note — When Linking This File From `STATE.md`

When the user tells you to link this file (i.e. Kotlin JAR implementation
work is actually starting), do both of the following as one checkpoint
commit — this section is instruction for that moment, not just a reminder:

1. **In `STATE.md`:** add this paragraph as the very first thing after the
   title line, before the existing "Do NOT read `README.md`..." note, so it
   is seen before any other instruction in that file:

   ```
   If the current task concerns Kotlin JAR support, stop here and read
   STATE_KOTLIN.md instead — it is self-contained and does not require the
   rest of this file.
   ```

2. **In this file:** remove (or reword) the "Guard — Unexpected Read of This
   File" section near the top. Its premise — "nothing routes here
   automatically" — stops being true the moment step 1 lands; left as-is, it
   would tell every legitimately-routed session to stop and ask the user,
   defeating the redirect you just added.

Do not perform either edit before the user explicitly says Kotlin
implementation work is starting — both remain deferred until then, per the
Explicit Non-Goals above.
