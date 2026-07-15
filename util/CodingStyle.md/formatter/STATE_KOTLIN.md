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
4. **`github.com/square/kotlinpoet`** — fully closed, all 4 idempotency
   shapes resolved, config: `indent_size=2` (matches kotlinpoet's own
   `.editorconfig`, same convention as okio). Initial pass fixed 2 compile/
   idempotency bugs (RDD_KEY_152 stale when-branch brace-indent anchor,
   RDD_KEY_153 Allman-conversion misfiring on an expr-bodied function's own
   trailing-lambda body). A later 125-file re-run surfaced a residual
   10-file idempotency gap in 4 distinct shapes, resolved across three
   follow-up sessions: Shape 1 (6 files: `CodeWriter.kt`, `LambdaTypeName.kt`,
   `MemberSpecHolder.kt`, `ParameterizedTypeName.kt`, `TypeVariableName.kt`,
   `WildcardTypeName.kt`) via RDD_KEY_163 (also resolved RDD_KEY_149's
   deferred `okio` bug); Shape 2 (`AbstractTypesTest.kt`) via RDD_KEY_164;
   Shape 3 (`ReflectiveClassInspector.kt`/`kmAnnotations.kt`) via RDD_KEY_165;
   Shape 4 (`KotlinPoetMetadataSpecsTest.kt`) via RDD_KEY_166 (which also
   surfaced/fixed two masked bugs in `signatureLineIndent` via a from-clean
   rebuild). Fixtures `_46`–`_49`. `make test`: 68/68 clean-rebuild final.
   **Confirmed via a fresh full-125-file re-run, `diff -rq round1 round2`
   empty — fully round1-vs-round2 idempotent.**

**Not started dogfood / real-code testing**
1. **`github.com/JetBrains/kotlin`** (NOT STARTED) — the Kotlin compiler's
   own source tree; large, likely the most demanding candidate for grammar
   coverage (compiler-internal code tends to use every language feature,
   including obscure/edge-case syntax). Last-resort/stress candidate,
   similar posture to `microsoft/STL`/`llvm-project` in the C++ list.

**In progress dogfood / real-code testing details**

1. **`github.com/arrow-kt/arrow`** (IN PROGRESS) — functional-programming
   library (typed errors, optics, effects). Scoped to `arrow-core`'s and
   `arrow-optics`'s `commonMain/kotlin` source sets (63 `.kt` files total),
   using tool (3) `kotlin_sc` for compile-checking per explicit user
   instruction instead of the Gradle-copy dance (tool (2)) — no Gradle
   wrapper build or persistent dogfood copy needed. Config: `indent-size=2`
   override (matches arrow's own `.editorconfig`, same convention as
   okio/kotlinpoet). Three bugs found and fixed this session, all confirmed
   as genuine `kotlin_sc`-rejected (compiler-invalid) output, not just
   idempotency-diff artifacts — see RDD_KEY_171 (generic type-parameter
   bound `:` corrupting angle-bracket tracking), RDD_KEY_172 (`val`/`var`
   declaration wrongly collapsed into a braceless `if`), RDD_KEY_173
   (annotation `@` sharing its line with a function signature rendering as
   `@ Foo`). Fixtures `test/real_code_regressions_59`–`_61`. `make test`:
   85/85 clean, zero regressions.
   **Two bugs remain found but NOT fixed, deferred (same posture as
   RDD_KEY_149's earlier `okio` deferral):**
   - `MiscRule.enforceCallLineBreaking`'s width measurement for nested call
     candidates (e.g. `leq(a, b)` inside an expression-bodied function tail)
     uses the full original physical line's width even when an
     enclosing/preceding candidate on the same line has already been
     claimed for multi-line rewriting, causing spurious redundant wrapping.
     Confirmed as a genuine `kotlin_sc` compile error (not just cosmetic)
     in `arrow-core`'s `Either.kt` (a `buildList(10) { if (a is Left)
     add(a.value) ... }` block with 10 sequential `if`/`add` statements gets
     its statements fused with no separator between them, since Kotlin has
     no mandatory `;`) — this is the ONLY remaining `kotlin_sc`-flagged
     syntax error in the 63-file scope as of RDD_KEY_173. Also produces a
     pure idempotency (non-compile-breaking) flap in `Comparison.kt`'s
     `sort2`. A fix was attempted (threading a `lastClaimedEnd`/`floor`
     value into `renderCallCandidate`) but caused 3 regressions in existing
     fixtures (`_33`, `_46`, `_55`) because `enforceCallLineBreaking` runs
     twice in `Formatter`'s pipeline and the floor logic incorrectly
     suppressed a legitimately-needed wrap on the second pass — reverted via
     `git checkout --`, not committed. Needs a more careful fix that
     accounts for both pipeline passes before attempting again.
   - `raise/context/RaiseContext.kt` shows a pure idempotency (non-compile-
     breaking) wrapping-shape flap for `ensureNotNull`'s params between
     round1 (collapsed/fits) and round2 (re-wrapped with column padding),
     and `Iterable.kt` shows a `for`-loop closing-comment threshold flap
     (round1: bare `}`; round2: `} // for`). Neither investigated/root-
     caused yet.
   Verification method: `kotlin_sc` run against a fresh round1 build of the
   full 63-file scope (baseline: 0 errors on unmodified `orig/`; before this
   session's fixes: 5 files with genuine syntax errors; after: 1 file,
   `Either.kt`, the deferred `enforceCallLineBreaking` bug above).

**When a test completes:** move/compact its entry from "Not started" (or its
"In progress" detail) into "Finished dogfood / real-code testing", and add a
new numbered entry to "Tools/compiler used" if a genuinely new tool is
introduced.
