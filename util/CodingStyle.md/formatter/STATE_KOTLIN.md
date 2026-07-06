# STATE_KOTLIN.md — Kotlin JAR Implementation Tracker

**This file is self-contained. Do not assume `STATE.md` has been read in this
session.** If you have not read `STATE.md`, that is fine — every convention this
file depends on is restated below. This file is routed to from `CLAUDE.md`'s
job table (Kotlin JAR support → this file) — it is still **not** linked from
`STATE.md`'s own index; do not add a cross-reference there until explicitly
told to (see "Handoff Note" below for when that happens).

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

---

## Open Questions

*(none)*

---

## Checklist

### Step 0 — Tokenizer Support (shared file, additive only)

**Critical rule for this step:** `TokenizerCore.java` is shared with C/C++/Java.
Every addition here must be additive (new keyword/operator recognition) and
must not change how any existing C/C++/Java token is lexed. Re-run the full
existing test suite after this step, before moving to Step 1.

- [ ] Survey `STYLE_KOTLIN.md`/`STYLE_KOTLIN2.md` for every token not already
      lexed correctly by `TokenizerCore.java`: `?.`, `!!`, `?:`, `->` (already
      exists for Java lambdas — confirm Kotlin's lambda/function-type/`when`
      arrow reuses the same token type), `..`, `..<` (KOTLIN2), `@` in labeled
      jumps (`return@label`, `outer@`), and confirm no collision with existing
      `MULTI_CHAR_OPS` entries (e.g. the `.*` collision already documented in
      RDD_KEY_69 for a different reason — check every new Kotlin operator
      against that table before adding).
- [ ] Add a Kotlin keyword set (`KEYWORDS_KOTLIN`), parallel to
      `KEYWORDS_JAVA`/`KEYWORDS_CPP`, covering at minimum: `val`, `var`, `fun`,
      `when`, `is`, `as`, `in`, `out`, `object`, `companion`, `init`,
      `constructor`, `data`, `sealed`, `infix`, `suspend`, `vararg`, `where`,
      `by`.
- [ ] Add Kotlin named-construct detection (`NAMED_CONSTRUCT_KOTLIN` or reuse
      the existing mechanism) for `class`, `object`, `companion object`,
      `interface`, `enum class`, `init` — confirm `computeConstructName()`'s
      lookback window is sufficient for each shape, extending it the same way
      `record`/`concept` needed extending (RDD_KEY_84/RDD_KEY_85), not by
      guessing ahead of an actual failing case.
- [ ] Re-run full existing C/C++/Java test suite. Zero regressions required
      before proceeding to Step 1.

### Step 1 — Scoping Pass (mirrors `JavaSpecificRule.java`'s own scoping, RDD_KEY_59)

- [ ] Cross-check every section of `STYLE_KOTLIN.md` and `STYLE_KOTLIN2.md`
      against the already-COMPLETE shared rule classes (`DeclarationAlignmentRule`,
      `BlockStructureRule`, `SwitchRule`, `GetterSetterRule`, `MiscRule`) to
      determine, per section: (a) already satisfied as-is by shared logic once
      Step 0's tokenizer work lands, (b) satisfied by a small additive
      extension to a shared class, or (c) needs a new method in
      `KotlinSpecificRule.java`. Record the outcome as a table appended to
      this file (same pattern as `RDD_KEY_59`'s Java scoping breakdown), not
      just as an implicit assumption.
- [ ] Flag anything found during scoping that would require changing
      already-COMPLETE shared-class *behavior* (not just adding to it) — per
      the Hard Constraint, that requires stopping and asking before proceeding,
      same as `STATE.md`'s own posture toward already-COMPLETE files.

### Step 2 — `KotlinModifierPriority.java`

- [ ] Column order for Kotlin's modifier set (`public/private/protected/
      internal`, `open/final/abstract/sealed`, `override`, `const`,
      `lateinit`, `val`/`var` sharing one slot per STYLE_KOTLIN.md §6) —
      confirm no cross-declaration-kind conflict analogous to the one resolved
      for Java in RDD_KEY_83 before assuming a single flat map suffices.

### Step 3 — `KotlinSpecificRule.java`

- [ ] Implement each section flagged "(c)" in Step 1's scoping table, one
      section at a time, each as its own checkpoint commit.

### Step 4 — Test Fixtures

- [ ] `test/kotlin_core_inp.kt` / `kotlin_core_out.kt` — first fixture pair,
      covering STYLE_KOTLIN.md's baseline sections end to end, same
      methodology as the existing `*_core_inp/out` pairs for other languages.
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
