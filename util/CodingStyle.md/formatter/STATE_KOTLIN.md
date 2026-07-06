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
  **Resolved:** user chose option 2 — `KotlinSpecificRule.java` will implement
  its own independent declaration/parameter parser and renderer for §6/§7,
  reusing only `ColumnGrid`/`ModifierPriority`-level primitives (including the
  new `KotlinModifierPriority`, Step 2). `DeclarationAlignmentRule` itself is
  not touched. This is Step 3 scope, not yet implemented.
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
| 1 | Semicolons (strip optional `;`) | (c) | No shared class strips statement-terminating `;` for any language today (C/Java require it) — wholly new `KotlinSpecificRule` pass. Must special-case enum-with-members' mandatory `;` (kept) and deliberate same-line multi-statement `;` (kept). |
| 2 | `enum class` with members | (a)/(c) | The `"enum class " + name` closing-comment label already falls out of `BlockStructureRule.classifyNamed`'s existing "keyword before `class` is `enum`" check (originally written for C++) — works for free once `enum`/`class` are both Kotlin keywords (Step 0, done). The mandatory `;` itself is just §1's stripper *not* stripping this one case — no separate logic. **Not yet verified:** the blank-line "emphasis" spacing around the entry-list/`;`/members shown in the style doc's example — needs a real fixture to confirm `insertNamedConstructBlankLines` produces it as-is or needs extension. |
| 3 | Brace style (Allman fn bodies / K&R everything else) | (a) | `BlockStructureRule.qualifiesForKAndR`'s `PAREN_KR_KEYWORDS`/`BARE_KR_KEYWORDS` sets already cover Kotlin's exact same control-flow keyword vocabulary (`if/while/for/switch/catch`, `else/do/try/finally`); function bodies default to Allman the same way C/Java ones do (a brace after `)` with no named-construct/control-flow keyword before it). `isLambdaBrace`'s K&R lambda exception is already language-general. |
| 3.1 | Class/Object/Companion Object bodies | (b), **done** | Named `class Foo {`/`object Foo {` already worked. Headless gap (anonymous `companion object {}`, anonymous `object : Interface {}`, `init {}` never arming `pendingNamedConstructName`) fixed via `RDD_KEY_99`: additive `BlockStructureRule.classifyKotlinHeadlessNamed`, gated by new `Lang.isKotlin`, parallel to the existing `isAnonymousClassBrace` precedent. Also fixed a related tokenizer bug found during verification (see RDD_KEY_99): `:` was wrongly arming a following supertype identifier as the construct's own name. |
| 3.2 | `catch`/`for`/`while`/`when` no space before `(` | (b), **done** | Added `"when"` to `MiscRule.TIGHT_PAREN_KEYWORDS` — RDD_KEY_100. Pure no-op for C/C++/Java (no `when` keyword/token in any of their keyword sets). |
| 3.3 | Secondary constructors (Allman body) | (a) | A constructor body's `{` follows `)` with no named-construct/control keyword before it — same generic "default to Allman" path as any other function body, no special-case needed. |
| 3.4 | `init` blocks | (b), **done** | Same headless-named-construct fix as §3.1 — `init {}` now returns `"init"` from `classifyKotlinHeadlessNamed`, grouped in the same `RDD_KEY_99` commit. |
| 4 | `when` expression (arrow alignment, closing comment, blank lines) | (c), **done** | `SwitchRule.java` turned out to be colon-form-statement-only (STYLE.md §13), unrelated; the real arrow-form logic is `JavaSpecificRule.enforceSwitchExpressionArrowAlignment`, but its `case`/`default`-keyword label scan and all-or-nothing block-body bailout both don't fit Kotlin's keyword-less, non-all-or-nothing `when` — implemented as new `KotlinSpecificRule.formatWhenExpressions` instead. RDD_KEY_101. |
| 5 | Null-safety operators (`?.`/`!!` tight, `?:` spaced) | (c) | No shared class does general expression-level operator re-spacing today — `MiscRule.isTightToken`/`needsSpaceBetween` only fire inside signature/param rendering, and assignment RHS values are joined **verbatim** (`MiscRule.joinVerbatim`, no re-spacing at all). Enforcing this for arbitrary expressions (not just declarations) is wholly new `KotlinSpecificRule` scope. |
| 6 | Variable/property declaration alignment | (c) — **major, see Open Questions** | `DeclarationAlignmentRule.Declaration` is `[modifiers] [typeTokens] [name] [= initTokens]` — C/Java's type-before-name grammar. Kotlin's `val name : Type = init` is the reverse order. Column-grid reuse (§6's `:`-alignment, `=`-alignment) needs either a shared-model extension (behavior change — stop-and-ask territory per Hard Constraint) or an independent Kotlin-only declaration parser/renderer in `KotlinSpecificRule.java` that only reuses `ColumnGrid`/`ModifierPriority`-level primitives, not `Declaration` itself. |
| 7 | Constructor/function parameter lists | (c) | Same reversed-grammar issue as §6 applies to `MiscRule.Param`/`Signature` (also assumes type-then-name); tied to §6's resolution. |
| 7.1 | Named/default arguments (`=` spacing/alignment) | (c) | Depends on §6/§7's resolution — reuses whatever declaration/assignment grid ends up handling Kotlin's reversed grammar. |
| 7.2 | Trailing comma (preserved as-is) | (a) | No existing pass adds or strips a trailing comma in any parameter/argument list for any language — trivially satisfied by doing nothing. |
| 8 | Property accessors (`get`/`set`, preserve expression/block form) | (a) | "Preserve as-is" is satisfied by not writing code that touches it. One risk checked: `BlockStructureRule.collapseSingleExpressionBlocks`'s `SINGLE_EXPR_KEYWORDS` is `{if, while, for}` only — an accessor's `set(v) { field = v }` block body is never a match, so it won't get wrongly collapsed to bare-statement form. |
| 9 | Expression-bodied functions | (a)/(c) | "Preserve as-is" part is free (same reasoning as §8). The "wrap `= expr` onto its own line if signature-breaking alone isn't enough" part is new behavior, tied to §6/§7's signature-wrapping work. |
| 10 | `for` loops and ranges | (b)/(c) | Tight/loose paren-padding itself is already generic (`ComplexityPaddingEvaluator`, STYLE.md §3.1) — needs `in`/`until`/`downTo`/`step` recognized as ordinary word-operator tokens for its nested-bracket detection to see through them correctly (additive keyword-set entries, (b)). The `..`/`..<` range operator's own *tight* spacing is the same kind of gap as §5 (c). |
| 11 | Labeled jumps (`@label` spacing) | (c) | No existing mechanism recognizes this token shape (keyword/identifier followed by `@identifier`) — new, scoped, `KotlinSpecificRule` logic. |
| 12 | Destructuring declarations | (c) | LHS is a parenthesized name list (`(a, b) = pair`), not `MiscRule.Assignment`'s assumed single `target` token — needs its own parsing, though it can likely still feed the existing `=`-alignment renderer once parsed. Comma spacing itself needs no new code (general commas aren't respaced by anything today, same reasoning as §7.2). |
| 13 | Generics variance (`in`/`out`) | (b) | `TokenizerCore.GENERIC_SAFE_KEYWORDS` doesn't yet include `"in"`/`"out"` — without it, `reclassifyAngleBrackets` may fail to recognize `Box<out T>`'s `<`/`>` as a generic pair rather than comparison operators. Small additive fix (belongs with Step 0 in spirit, catalogued here since it surfaced during this section's cross-check). |
| 14 | Generic `where` clause | (c) | Structural analog exists in `CppSpecificRule.java`'s trailing-`requires`-clause handling, but that's a per-language file, not shared — needs its own `KotlinSpecificRule` method (can use the C++ one as a reference pattern during Step 3). |
| 15 | Infix functions (modifier slot; call-site spacing) | (a) | Modifier slot itself is Step 2 (`KotlinModifierPriority`) scope, not Step 1. Call-site word-operator spacing (`3 times "abc"`) is ordinary expression spacing, already left alone by every shared class (same reasoning as §5's baseline, no active interference to worry about). |
| 16 | Annotation use-site targets (`@field:` tight `:`) | (c) | No existing annotation-colon handling (Java annotations have no use-site-target shape) — small new `KotlinSpecificRule` logic. |
| 17 | Lambda-with-receiver / function types (exempt from nesting detector) | (c) | Needs `Type.(...) -> Ret` recognized as one atomic function-type token by whatever handles nested-paren/bracket detection (`ComplexityPaddingEvaluator` or a Kotlin-specific pre-pass) — new. |
| 17.1 | Lambda parameter arrow spacing | (c) | Same category as §5 — active operator spacing outside declarations, nothing shared does this today. |
| 18 | `vararg` | (a) | Modifier-slot handling is Step 2 scope; no general spacing concern beyond that. |
| 19 | String templates (preserve `"$x"`/`"${x}"` exactly) | (c) — **tokenizer-level, feeds back into Step 0** | Not yet verified whether `TokenizerCore.emitString()` can correctly find a Kotlin string's closing `"` when a `${...}` interpolation contains its own nested `"..."` (e.g. `"${foo("x")}"`). `emitString()` was written for C/Java string literals, which have no such nesting. This is a real risk, not a style question — needs a dedicated Step 0 follow-up before any Kotlin fixture with string templates can be trusted. Flagged, not yet investigated in depth (avoiding guessing ahead of an actual failing case, per this step's own discipline; a concrete failing fixture should drive the actual fix). |
| 20 | Sealed classes/interfaces | (a) | Normal `class`/`object` K&R rules apply unchanged, no special layout. |
| 21 | Type aliases | (a) | Single-line `=`-spaced statement, no new behavior. |
| 22 | Extension functions | (a) | `fun` behaves like any other modifier/keyword token for spacing purposes. |
| 23 | Known Gaps | (a), excluded | Explicitly out of scope, same posture as STYLE_JAVA.md's own excluded "unresolved" section (RDD_KEY_59). |
| K2.1 | Guard conditions in `when` | (b)/(c) | Extends §4's arrow-alignment logic as-is per the style doc — tied to §4's `SwitchRule` generalization question. |
| K2.2 | `data object` | (a), once §3.1 lands | Formatted exactly like `object` — `"data"` just needs to be treated as a skippable modifier prefix by whatever §3.1 fix recognizes anonymous/headless `object` (small addendum to that fix, not separate new work). |
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
- [x] **§1 Semicolons.** `KotlinSpecificRule.stripOptionalSemicolons(List<Token>)`
      strips every optional statement-terminating `;`, keeping only an `enum
      class` body's entries/members separator, and only when member
      declarations actually follow it (an entries-only enum body's trailing
      `;` is optional too and gets stripped, matching §1's own stated
      rationale). Implemented as a token-list state machine: tracks a
      brace-depth stack of `EnumBodyState` (armed when the immediately-open
      `{` was preceded by `enum` then `class`, tolerating any
      generics/constructor-args/supertype-clause tokens in between since
      Kotlin's grammar has `enum`/`class` strictly adjacent modulo
      whitespace); the first top-level `;` inside an armed body is kept iff a
      non-`}` token follows before that body's own closing `}`. Verified with
      a standalone tokenize-then-strip harness (not committed, scratchpad
      only, per this file's own "prefer evidence" rule): flat declarations,
      an enum with no trailing members, an enum with trailing members, a
      nested enum-with-constructor-args-and-supertype-clause inside an outer
      class, and a same-line entries-only enum — all stripped/kept correctly.
      One real bug caught and fixed during this verification: the initial
      version reset its `enum`/`class`-pending flags on the class *name*
      token itself, before ever reaching the body's `{`, so no enum body was
      ever armed — fixed by only resetting those flags on `;`/`{`/`}`, not on
      arbitrary header tokens. Full C/C++/Java suite still 25/25 (this file
      is new, so no shared-class change to worry about here).
- [x] **§3.1/§3.4 Class/Object/Companion Object/`init` bodies.** Fixed as
      `RDD_KEY_99` — a shared-class extension (`Lang.isKotlin`,
      `BlockStructureRule.classifyKotlinHeadlessNamed`), not a
      `KotlinSpecificRule.java` method, since the fix belongs in the same
      brace-classification machinery that already handles every other
      named-construct shape. Also fixed a related tokenizer bug found during
      verification (`:` wrongly arming a supertype identifier as the
      construct's own name for anonymous `object : Super {}`). Full
      C/C++/Java suite 25/25 before and after each of the three shared-class
      edits. See `RDD_KEY_99` for full detail.
- [x] **§3.2 `when` no space before `(`.** Fixed as `RDD_KEY_100` — added
      `"when"` to `MiscRule.TIGHT_PAREN_KEYWORDS`, a shared-class one-line
      change, not a `KotlinSpecificRule.java` method, since the set is
      already unpartitioned by language and `when` simply never matches for
      C/C++/Java (no such keyword in their keyword sets). Verified via a
      standalone harness calling `enforceKeywordSpacing` directly on
      tokenized `when (x) { 1 -> "a" }`, confirming the collapse to
      `when(x) { ... }`. Full C/C++/Java suite 25/25 before and after.
- [x] **§4 `when` expression (arrow alignment, closing comment, blank lines).**
      Fixed as `RDD_KEY_101` — new `KotlinSpecificRule.formatWhenExpressions`,
      not a shared-class extension. `SwitchRule.java` (read in full, 896
      lines) turned out to be entirely colon-form switch STATEMENT handling
      (STYLE.md §13) with zero `"->"` logic; the real arrow-form
      switch-EXPRESSION logic is
      `JavaSpecificRule.enforceSwitchExpressionArrowAlignment`/`findArrowCases`,
      but two things block reusing it: (1) it anchors each label's start on a
      `case`/`default` KEYWORD token, which Kotlin `when` branches don't have
      (just a bare condition expression); (2) it bails out of alignment
      entirely for the whole `switch` if any case has a block body, but
      STYLE_KOTLIN.md §4's own worked example keeps `->` aligned even with
      one block-body branch present — the opposite rule. New method finds
      branch boundaries by requiring one branch per physical line (a depth-0
      `->` starts a body; a depth-0 NEWLINE after a non-block body, or a
      block body's own matching `}`, ends it) and bails (leaves that whole
      `when` untouched) if this shape isn't met. Also forces a blank line
      after `{`/before `}` (control-flow blocks like `if`/`for`/`while`/
      `switch` only ever *preserve* existing blank lines via
      `BlockStructureRule.insertNamedConstructBlankLines` — never force them
      — so this needed its own logic here too) and an unconditional
      `// when subject` closing comment (bare `// when` for a subject-less
      `when { ... }`), with no length gating, unlike FOR/WHILE/SWITCH's
      closing comments in `BlockStructureRule.addClosingComments`. Verified
      via a standalone harness covering a simple `when(x) { ... }`, a mixed
      simple/block-body `when`, and a subject-less `when { ... }` — all three
      matched STYLE_KOTLIN.md §4's exact expected output, including the
      block-body-mixed alignment case. Full C/C++/Java suite 25/25 (this
      method isn't wired into any shared class or `Formatter.formatOne` yet —
      that's deferred to whenever `KotlinSpecificRule` itself gets wired in).

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
