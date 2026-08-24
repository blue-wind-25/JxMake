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
C (external corpora, `../../JCS`/`../../MDXplorer`/
`../../../3rd_party/tools/pcpp_java`/`colordiff.py`) — every formatted file
byte-identical to committed originals, still at the prior fixed point,
nothing to adopt; see `STATE_DOGFOOD.md`.

Pass 2 (flag forced ON via `JXMAKE_CODE_FORMATTER_LINE_SPLIT_OPERATOR_
PRIORITY=on`, scratch copy, read-only): corpus
`../../../3rd_party/tools/pcpp_java` (43 `.java` files, ~10.7K LOC) —
chosen over `../../JCS` (only 1 real `.java` file, rest shell/PowerShell
scripts) as the smallest genuinely curly-family corpus among the four
Pass-1 external candidates. round1/round2 `diff -ru` empty (idempotency,
exercises `hasNewlineBetween`'s guard). `java_syntax_check.sh` clean on
all 43 files. Only 3 files actually changed vs. the unformatted original
(`LexTab.java`, `ParseTab.java`, `Preprocessor.java`) — small corpus, few
lines cross the length threshold. Hand-eyeballed: `LexTab.java`/
`ParseTab.java` show tier-1 `+` splits on long string-concatenation
assignment RHS (operator-leading, one `indentWidth` continuation indent,
correctly leaving `?`/`:` substrings *inside* string literals like
`"...CPP_QUESTION..."` untouched since those are literal text, not real
ternary tokens); `Preprocessor.java` shows a tier-1 `&&` split on a long
`if` condition. All three match the designed shape exactly, no false
positives. No bug found — Pass 2 fully clean, no new fixture needed. No
files copied back anywhere (read-only per plan).

Pass 3 (flag forced ON via `.jxmake-code-formatter`, scratch copy,
read-only): corpus `google/guava` (`git clone --depth 1`, 1655 `.java`
files excluding `android/`, ~404.7K LOC) — a much larger corpus than Pass
2, chosen after every other `/tmp` checkout available at session start
turned out corrupted/unsuitable (0 real files, or `node_modules` stub-only
trees). round1/round2 `diff -ru`: 100 of 1655 files differ, all
attributable to two categories, both confirmed pre-existing (reproduced
identically with the flag forced OFF at default config, i.e. present on
unmodified `main` before this pass):
  1. The already-documented closing-comment-min-lines flap (see "Known
     Out-of-Scope Finding" above) — a handful of files.
  2. A newly-observed variant of the same broad "line-count-crosses-a-
     later-pass's-threshold" class: when the original source already has
     an `if`/`while` condition hand-split across multiple lines (e.g.
     `Sets.java`'s `subSet`, `&&`-chained across 4 source lines) and the
     single-statement body collapses onto the condition's closing line
     (`BlockStructureRule.tryCollapse`), the resulting over-length single
     physical line is not always re-detected as a fresh operator-split
     candidate in round 1 (root cause not fully isolated — reproducing it
     requires the exact real-file context; a reduced standalone repro with
     the same shape formats correctly in round 1). Round 2, working from
     round 1's already-collapsed-and-rejoined text, does split it
     correctly, self-correcting on the second pass. Output is valid Java
     both rounds — a non-idempotent flap, not a correctness bug — left
     out of scope for this pass per the same reasoning as finding 1.
  `java_syntax_check.sh` (batch via `xargs`, all 1655 files): 1647 clean,
  8 "variable declaration not allowed here" errors — all 8 confirmed
  **pre-existing** (reproduced identically with the flag OFF), root-caused
  to an unrelated, separate bug in `BlockStructureRule.tryCollapse`: it
  strips `{ }` from a single-statement loop/if body even when that
  statement is a local variable declaration, which is illegal Java without
  braces (e.g. `SuppliersTest.java`'s
  `for(...) Object unused = Suppliers.synchronizedSupplier(...).get();`).
  Flagged prominently as a real, pre-existing bug worth a future session's
  attention, but unrelated to `line-split-operator-priority` and out of
  scope here.
  One genuine in-scope bug found and fixed: `findTernarySplits` mistook a
  generic wildcard type argument's `?` (`Optional<?>`, `Collection<?
  extends E>`, `Map<K, ?>` — found via `MoreObjects.java`/`Sets.java`) for
  a ternary conditional operator, producing an incorrect split inside the
  type argument. Fixed via a new `isGenericWildcardQuestion` helper in
  `MiscRuleCurly.java` that recognizes the shape (immediately bounded by
  `<`/`,` on one side, `>`/`,`/`extends`/`super` on the other) and skips
  it. Note: the bounding `<`/`>` tokens are tagged
  `TokenType.ANGLE_BRACKET_OPEN`/`ANGLE_BRACKET_CLOSE` by a later
  re-tagging pass, not plain `OP`/`PUNCT` as first assumed — the fix
  checks both forms, matching the existing pattern used elsewhere in this
  file (`isPunct(t, "<") || t.type == TokenType.ANGLE_BRACKET_OPEN`). New
  fixture: `test/real_code_regressions_231_{inp,out}.java`. `make test`
  351/351 forward + idempotency after the fix. Re-verified post-fix: the
  100-file round1/round2 diff count and 8-file syntax-check count are
  both unchanged (the fix only removes a false-positive split, it doesn't
  touch either pre-existing flap class). 73 of the 1655 files show the
  feature actually firing vs. a flag-off baseline; spot-checked
  `CharMatcher.java`, `Graphs.java`, `Maps.java`, `TreeRangeSet.java` —
  all show correct tier-1 (`&&`/`+`) and tier-2 (ternary) operator-leading
  splits, no false positives on TS-equivalent shapes (n/a for this
  Java-only corpus) or Kotlin elvis (n/a). No files copied back anywhere
  (read-only per plan).

Pass 4 (follow-up fix for Pass 3 finding 2, the collapse-onto-condition-
line non-idempotency flap): root-caused via `google/guava`'s
`Suppliers.java` `memoize` method (`if (delegate instanceof
NonSerializableMemoizingSupplier || delegate instanceof
MemoizingSupplier) return delegate;` — 118 chars once
`BlockStructureRule.tryCollapse` joins the single-statement body onto the
condition's closing-paren line — confirmed to stay unsplit and identical
across round 1 AND round 2 pre-fix, i.e. a persistent miss, not a
self-correcting flap as Pass 3 first assumed for that specific line; the
"self-correcting on round 2" behavior Pass 3 observed happens for *other*
lines in the same corpus once round 1's own edits shift what round 2 sees.
Root cause: `MiscRuleCurly.tryOperatorSplit`'s fits-check measured only
`[lineStartIndex(from), to]` (the condition's own span, stopping at the
closing paren) rather than the true rendered physical line, so it never
saw the collapsed body text that `BlockStructureRule.tryCollapse` (running
earlier in the pipeline, well before `enforceOperatorLineBreaking`) had
already joined onto that same line, and undercounted the width. Fix:
extend the measured span through `effectiveLineEndIndex(tokens, to)` (the
existing helper that walks forward to the true end of the physical output
line, tracking bracket depth). Extending the fits-check window uncovered a
second, narrower issue: it now unconditionally counted a trailing same-
line comment against the plain 100-char limit, over-splitting lines that
legitimately fit under the wider 120-char `line-length-with-comment`
limit (found via `Streams.java`'s alignment-grouped `isParallel`
declaration). Fixed by mirroring `enforceCallLineBreaking`'s own existing
comment-aware-limit pattern (`effectiveLimit = hasCommentBetween(tokens,
to, lineEnd) ? lineLengthWithCommentLimit : lineLengthLimit`). New
fixture: `test/real_code_regressions_235_{inp,out}.java` (adapted from the
real `Suppliers.java` `memoize` shape), round1/round2 empty diff confirmed
for it specifically. Re-verified against a fresh `google/guava` clone
(1655 files, same methodology as Pass 3, flag forced on for both rounds
via a copied `.jxmake-code-formatter`): round1/round2 diff dropped from
100 to 47 files, all 47 confirmed (by sampling) to be the pre-existing
closing-comment-min-lines flap (finding 1) only — finding 2's non-
idempotency, including the exact `Suppliers.java:129` line, is now stable
across both rounds. `java_syntax_check.sh` on all 1655 round1 files: zero
errors. `make test`: 355/355 forward + idempotency. Full text:
`RDD_KEY_342`.

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
- **D5 — fits-check must measure the true physical line, comment-aware.**
  Follow-up fix (Pass 4, `RDD_KEY_342`). `tryOperatorSplit`'s fits-check
  originally measured only the condition span itself, missing width
  contributed by a `BlockStructureRule.tryCollapse`-joined single-statement
  body sharing the same physical line — a persistent (not self-correcting)
  non-idempotency. Fixed by measuring through `effectiveLineEndIndex`, and,
  to avoid a resulting over-split regression on trailing same-line
  comments, by reusing `enforceCallLineBreaking`'s existing comment-aware
  `effectiveLimit` pattern.

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
- `test/real_code_regressions_235_{inp,out}.java` — Pass 4 idempotency
  fix: an `if` condition whose collapsed single-statement body pushes the
  physical line over the length limit, adapted from real
  `Suppliers.java`'s `memoize` shape.
- `make test`: 347/347 -> 350/350 -> 351/351 -> 355/355 forward +
  idempotency, zero regressions (355 after Pass 4's
  `real_code_regressions_235` fixture).

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
