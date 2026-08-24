# STATE_LINE_SPLIT_OP.md — Operator-Priority Line Splitting Tracker

Read `STATE_COMMON.md` first — shared commit/ambiguity/testing conventions
this file assumes; no other job's `STATE_*.md` is required. Dogfood corpus
status: see `STATE_DOGFOOD.md`.

**2026-08-25 dogfood/validation (two-pass):** Pass 1 (flag off, routine
self-adopt regression confirm) — Leg A (`tools/*`, 82 files) idempotent,
zero content-changed files, nothing to adopt. Leg B (`src/**/*.java`, 100
files) idempotent; 3 files differed from committed `src/`
(`Config.java`/`FormatterCurly.java`/`MiscRuleCurly.java`, exactly this
session's own new-feature files, not yet self-formatted) — all reviewed by
hand, ordinary cosmetic re-style (call-wrap width, spacing normalization,
declaration-alignment column width), confirming the feature is a true
no-op with the flag off; zero column-0-flush lines, zero content/comment
loss. Trial JAR: `_test_serial` 348/350 — the 2 failures
(`cpp_comments_inp.cpp`, `real_code_regressions_217_inp.java`) are the
documented `gru-sync-weights` trial-JAR drift class, not chased. Adopted;
`make clean && make test` 350/350 forward + idempotency, fully green. Leg
C (external corpora) — see below.

Pass 2 (flag forced on, stress test): corpus `../../../3rd_party/tools/pcpp_java`
(small, curly-family). Idempotent (`hasNewlineBetween` guard held under
round2). `java_syntax_check.sh` clean on all changed files. Hand-eyeballed
diffs confirm operator-LEADING tier-1 (`&&`/`||`/`+`/`-`) splits firing
correctly on long `if`/assignment conditions; no false positives. No bug
found — Pass 2 fully clean, no new fixture needed.

---

## Purpose

Tracks the curly-family (C/C++/Java/Kotlin/JS/TS) `line-split-operator-
priority` config key (default off): when an `if`/`while`/`switch`
condition, a `for(...)` header, or a bare `return`/assignment-RHS
expression with no enclosing call parens is too long, splits it at
operator boundaries instead of leaving it long or falling straight to
today's call-argument-paren wrapping. **Fully implemented, `make test`
green (350/350).**

Output shape: unpadded, operator-LEADING (the operator token leads each
continuation line), each continuation line at `baseIndent + one
indentWidth`.

---

## Project Layout

- `src/com/jxmake/formatter/rules/MiscRuleCurly.java` — all logic.
  - `enforceOperatorLineBreaking(List<Token>)` — main scan loop, recognizes
    `if`/`while`/`switch` (condition span), `for` (delegates to
    `tryForHeaderSplit`), `return` (expression span to `;`), and top-level
    `=` assignment (`parenDepth == 0`, RHS span to `;`).
  - `tryOperatorSplit` — entry point for if/while/switch/return/assignment
    candidates; guarded by `hasNewlineBetween` (idempotency, see below).
  - `splitTiered(tokens, from, to, baseIndent, tier)` — tier 1 -> 2 -> 3
    dispatcher; Kotlin skips tier 2 (no C-style ternary — `?:` there is
    elvis) straight to tier 3.
  - `findBinaryOpSplits(tokens, from, to, opTexts...)` — shared depth-
    tracking scanner; `findOperatorSplits` (tier 1: `&&`,`||`,`+`,`-`) and
    `findMulDivSplits` (tier 3: `*`,`/`) both delegate to it.
  - `findTernarySplits` (tier 2: `?`/`:` depth-0 scan) + `isBinaryOperatorContext`
    (disambiguates unary vs binary via preceding-token check, e.g. unary
    `*ptr` deref never mistaken for a multiplication split point).
  - `renderTieredSplit` / `renderFragment` — render operator-leading
    continuation lines; a fragment still too long recurses into the next
    tier.
  - `tryForHeaderSplit` / `renderForClause` — `for(...)` headers split on
    their own two top-level `;` clause boundaries onto three lines (init/
    cond/incr), closing `)` staying attached to whatever followed it in
    source; a clause still too long recurses into the same tier-1..3
    ladder.
  - `hasNewlineBetween(tokens, from, to)` — `true` if any token in the
    inclusive range is a NEWLINE. Idempotency guard at the top of
    `tryOperatorSplit`, `renderFragment`, `tryForHeaderSplit`, and
    `renderForClause`: an already-multi-line candidate (round 2+ of a
    repeated format, or any other pre-existing multi-line shape) is left
    untouched unconditionally. Mirrors this class's standing precedent
    (`enforceCallLineBreaking`'s own per-side gap check) of a NEWLINE in a
    gap suppressing a rewrite for that side.
- `src/com/jxmake/formatter/FormatterCurly.java` — exactly ONE gated call
  site: `if( config.lineSplitOperatorPriority() ) { text =
  miscRule.enforceOperatorLineBreaking( tokenizer.apply(text) ); }`,
  positioned immediately after `blockRule.alignBracelessElseIfChain(...)`
  runs and before the `enforceCallLineBreaking` calls that follow it. See
  Resolved Design Decisions D1 below for why this deviates from inserting
  before all four existing `enforceCallLineBreaking` call sites.
- `src/com/jxmake/formatter/Config.java` — `line-split-operator-priority`
  key fully wired (`ALL_KEYS`, field, accessor, `GROUPS`, `describeAll`
  case, `fromRawMap` parsing).

---

## Resolved Design Decisions

Full text: `RDD_KEY_340`.

- **D1 — single pipeline call site, not four.** The task's original
  mechanical instruction was to insert a gated
  `enforceOperatorLineBreaking` call before each of the four pre-existing
  `enforceCallLineBreaking` call sites in `FormatterCurly.java`. Real
  testing found that running the split before `blockRule.
  alignBracelessElseIfChain` sees the text feeds that pass an already-
  split multi-line if-condition, corrupting its braceless-collapse output
  with spurious blank lines. Fix: only ONE gated call remains, positioned
  after `alignBracelessElseIfChain` runs.
- **D2 — idempotency via `hasNewlineBetween`.** Re-running the formatter
  on its own already-split output re-derived a split on round 2 (the
  whitespace-collapsed-for-measurement span still measured "too long"),
  and because rendering copies original WHITESPACE/NEWLINE tokens
  verbatim for already-fitting fragments, produced a duplicate newline
  (a blank line). Fixed by unconditionally skipping any candidate that
  already spans a NEWLINE.
- **D3 — three-tier ladder, `for` in scope, `*`/`/` tier.** Amended
  mid-implementation from an initial single-tier `&&`/`||`/`+`/`-`-only
  design. Tier 1 (`&&`,`||`,`+`,`-`, equal priority) always tried first;
  tier 2 (`?:` ternary, skipped for Kotlin) only engaged when tier 1 found
  nothing or left a fragment still too long; tier 3 (`*`,`/`) only engaged
  when tiers 1-2 leave a fragment still too long. `for(...)` headers split
  on their two top-level `;` clause boundaries; an individual clause still
  too long recurses into the same ladder.
- **D4 — landmines confirmed non-issues.** Kotlin elvis `?:` never
  mistaken for ternary; JS/TS optional chaining `?.` and nullish
  coalescing `??` never mistaken for ternary `?`/`:`; C/C++ unary
  pointer-dereference `*ptr` never mistaken for a binary-multiplication
  split point. Each has a dedicated fixture case.

---

## Known Out-of-Scope Finding (not fixed, discovered incidentally)

A pre-existing closing-comment-min-lines round1-vs-round2 flap (same
category as `RDD_KEY_174`): splitting a condition can change the enclosing
function's line count across the threshold that decides whether a
trailing `// functionName` comment is emitted, so round 1 and round 2
output can genuinely differ for a function shaped that way. Hit while
authoring the `.ts` fixture's would-be `primaryTierIf` case; worked around
by omitting that case from the `.ts` fixture (already covered by `.cpp`/
`.kt`) rather than fixing — unrelated to this feature, out of scope.

---

## Testing

- `test/line_split_operator_priority_{inp,out}.cpp` — primary/ternary/
  mul-div tiers across if/while/switch/assignment/return, `for` (plain and
  with a clause still too long after the top-level split), unary-`*`
  landmine.
- `test/line_split_operator_priority_{inp,out}.kt` — primary tier if,
  elvis-vs-ternary landmine.
- `test/line_split_operator_priority_{inp,out}.ts` — optional-chaining/
  nullish-vs-ternary landmine, genuine ternary still splits.
- All three registered in `Makefile`'s `INP_FILES` (grouped ahead of the
  `real_code_regressions_*` block, per this job's own naming).
- `make test`: 347/347 -> 350/350 forward + idempotency, zero regressions.

---

## Checklist

- [x] Resolve structural/design questions (RDD_KEY_340).
- [x] Implement `enforceOperatorLineBreaking` and the tier-1..3 ladder.
- [x] Implement `for(...)` header splitting.
- [x] Wire config key end-to-end (`Config.java`).
- [x] Wire single pipeline call site (`FormatterCurly.java`).
- [x] Fix fresh-format and idempotency-round blank-line bugs.
- [x] Author and verify fixtures (`.cpp`/`.kt`/`.ts`), including landmines.
- [x] Register fixtures in `Makefile`; `make test` green.
- [x] `CLAUDE.md` job table row; this state file.
- [x] `README.md` sync (user-facing only, no internal vocabulary).
