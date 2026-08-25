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

**2026-08-25 follow-up (closing-comment-min-lines flap fix):** the
remaining Known Out-of-Scope Finding from the section above — splitting a
condition can push an enclosing `for`/`while`/`if`/`switch` block's line
count across the `closing-comment-min-lines` threshold, but
`addClosingComments` ran before `enforceOperatorLineBreaking` in the
pipeline, so a fresh format's decision didn't see the post-split line
count while a reformat of already-split output did — root-caused and
fixed via `RDD_KEY_343` (see D6 in Resolved Design Decisions below): the
`addClosingComments`/`enforceSwitchExpressionArrowAlignment` pair now runs
after `enforceOperatorLineBreaking` when the flag is on, gated so the
flag-off pipeline is byte-for-byte unchanged. New fixture
`test/real_code_regressions_236_{inp,out}.java`. `make test`: 356/356
forward + idempotency. Spot-checked against a fresh `google/guava` clone
(flag forced on, same methodology as Pass 3/4): confirmed zero of the
remaining round1-vs-round2 differing files are this flap class anymore.
That spot-check surfaced a different, still-unfixed, pre-existing flap —
see the new entry in "Known Out-of-Scope Finding" below.

**2026-08-25 follow-up (`indent-size=2` spot-check on the two Known
Out-of-Scope flaps, low-priority per the tracker's standing "indent-size
fallback" practice):** reused the existing `google/guava` clone (fresh
`git clone --depth 1`, re-verified at exactly 1655 `.java` files excluding
`android/`, same selection as Pass 3/`RDD_KEY_342`/`RDD_KEY_343`) and the
same methodology (`.jxmake-code-formatter` at the corpus root, manually
copied into round 1's output before round 2 since `--preserve-tree` doesn't
copy dotfiles, batch `xargs` invocation). `make test` confirmed green
(356/356) before starting; no `src/` changes made this pass.

Four conditions run: flag on/off crossed with `indent-size` default(4)/2.
Baseline reproduction first surfaced an unexpected result: at default
`indent-size`, flag-on and flag-off produced **byte-identical differing-file
lists** (42/1655 each) -- i.e. **flap (a) (the declaration/condition
collapse-on-round2 flap) did not reproduce at all in this fresh clone**, at
0/1655, not the documented 38/1655. Individually re-checked all four named
example files (`MinMaxPriorityQueue.java`, `CharMatcher.java`,
`TreeRangeSet.java`, `Streams.java`) both in the batch run and via an
isolated single-file solo re-format (ruling out a batch/script artifact):
all four are round1-byte-identical-to-round2 at default `indent-size`,
flag forced on -- `src/` is unchanged since `RDD_KEY_343`'s fix commit
(confirmed via `git log`, no commits touching `src/` since), so this is a
guava-corpus-content difference between today's fresh clone and whatever
commit the original Pass 3/`RDD_KEY_343` spot-checks cloned, not a src
regression or a re-fix -- not chased further (out of scope; flagged below).
Flap (b) (the flag-independent baseline) reproduced exactly as documented:
42/1655, identical file list flag-on vs. flag-off, `Functions.java` included.

With `indent-size=2` added: flag-off dropped from 42 to **2**/1655
differing files (`CollectionToArrayTester.java`,
`WriteReplaceOverridesTest.java` -- both already part of the 42-file
default-indent baseline, same flap-(b) class, diff shape unchanged in kind
just relocated to different lines/columns) -- i.e. flap (b) **mostly
self-resolves under `indent-size=2`** (40 of 42 files become idempotent)
but does **not** fully resolve (2 residual files). Flag-on at
`indent-size=2` showed **3**/1655 differing files: the same 2 flap-(b)
residuals (byte-identical diff shape to the flag-off run) plus **one new
file**, `guava/src/com/google/common/cache/LocalCache.java`, not present in
either default-indent run nor in the flag-off-indent2 run -- i.e. a
flag-*dependent* (only appears with `line-split-operator-priority=on`),
`indent-size=2`-*dependent* (only appears at indent-size 2) non-idempotency,
newly discovered by this pass. Diffed: an operator-split assignment RHS's
continuation line (`|| ticker == NULL_TICKER ) ? null : ticker;`, tier-1
`||`-leading per this feature's own output shape) sits at column 8 on round
1 but gets re-indented to column 34 (aligned under the assignment's `=`
column, matching the surrounding declaration-alignment group's padding) on
round 2 -- a third, distinct flap shape from both (a) (whole-expression
collapse-to-one-line) and (b) (`Functions.java`'s anonymous-class-brace
shape): a continuation-line *alignment-padding* drift, not a
line-count/collapse issue. Root cause not investigated (per this task's
explicit low-priority/no-rabbit-hole scope) -- flagged as a third Known
Out-of-Scope Finding below for a future session.

`java_syntax_check.sh` (batch, all 1655 round1 files at flag-on +
`indent-size=2`): 1655/1655 clean, zero errors -- confirmed individually for
all five named example files (`MinMaxPriorityQueue.java`, `CharMatcher.java`,
`TreeRangeSet.java`, `Streams.java`, `Functions.java`) plus the newly-found
`LocalCache.java`. Consistent with the standing "neither flap causes
corruption" framing -- unchanged.

**Net finding:** `indent-size=2` does **not** cleanly resolve either
documented flap. Flap (a) could not be meaningfully re-tested against this
corpus draw at all (it wasn't present at the default-indent baseline either,
for reasons not investigated) -- no conclusion possible from this corpus for
flap (a) specifically. Flap (b) partially self-resolves (40/42, ~95%) but
leaves 2 residual files, and additionally surfaces a brand-new,
flag-and-indent-size-dependent alignment-padding flap not previously seen at
default `indent-size`. No fix attempted; both existing flaps remain
undocumented-root-cause/not-fixed, and the new alignment-padding flap is
added as a third Known Out-of-Scope Finding rather than chased, per this
task's explicit scope. `make test` re-confirmed 356/356 at the end (no
`src/` changes made this pass). Full text: `RDD_KEY_344`.

---

## Purpose

Tracks the curly-family (C/C++/Java/Kotlin/JS/TS) `line-split-operator-
priority` config key (default off): when an `if`/`while`/`switch`
condition, a `for(...)` header, or a bare `return`/assignment-RHS
expression with no enclosing call parens is too long, splits it at
operator boundaries instead of leaving it long or falling straight to
today's call-argument-paren wrapping. **Fully implemented, `make test`
green (356/356).**

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
- **D6 — closing-comment-min-lines flap fixed via flag-gated reordering,
  not an unconditional move.** Follow-up fix, `RDD_KEY_343`. Root cause
  matched `RDD_KEY_174`'s precedent exactly: `enforceOperatorLineBreaking`
  can expand a block's content-line count, but
  `BlockStructureRule.addClosingComments` (STYLE.md §7 threshold decision)
  ran BEFORE it in the pipeline, so a fresh format's decision saw the
  pre-split line count while a reformat of already-split output saw the
  post-split count. Unlike `RDD_KEY_174` (where the earlier pass could
  simply move later unconditionally), `addClosingComments` could not move
  unconditionally here: everything between its old position and
  `enforceOperatorLineBreaking`'s position (Phase 4 cosmetic spacing,
  `alignBracelessElseIfChain`) runs regardless of the
  `line-split-operator-priority` flag, and `alignBracelessElseIfChain`
  itself can change a block's line count (collapsing a braceless `if`/body
  onto one line) — an unconditional move would have changed
  `addClosingComments`' decisions, and thus output, even with the flag
  off. Fixed by gating instead: `addClosingComments` (+
  `enforceSwitchExpressionArrowAlignment`, kept paired immediately after
  it, matching its pre-fix relative order) runs at its original Phase 3
  position only when the flag is off (byte-for-byte identical to every
  pre-fix flag-off pipeline), and is skipped there + re-run immediately
  after `enforceOperatorLineBreaking` when the flag is on. New fixture:
  `test/real_code_regressions_236_{inp,out}.java`.
- **D7 — `indent-size=2` does not resolve either Known Out-of-Scope
  flap.** Spot-check, `RDD_KEY_344`. Flap (a) didn't reproduce at all in a
  fresh guava clone even at default `indent-size` (corpus-content-sensitive,
  not investigated), so no conclusion for it specifically. Flap (b) mostly
  self-resolves at `indent-size=2` (40/42) but not fully (2 residual files),
  and `indent-size=2` + flag-on additionally surfaces a third, previously
  unseen flap (continuation-line alignment-padding drift on an
  operator-split assignment RHS) not chased further per this task's scope.

---

## Known Out-of-Scope Finding (not fixed, discovered incidentally)

**Declaration/condition collapse-on-round2 flap (found 2026-08-25, during
`RDD_KEY_343`'s guava spot-check, not fixed).** A different, pre-existing
flap from the closing-comment one D6 fixed: an operator-split-eligible
declaration or condition that `enforceOperatorLineBreaking` correctly
renders multi-line (operator-leading) on round 1 collapses back onto one
physical line on round 2 — e.g. `google/guava`'s
`MinMaxPriorityQueue.java`: `int result = (configuredExpectedSize ==
Builder.UNSET_EXPECTED_SIZE) ? DEFAULT_CAPACITY : configuredExpectedSize;`
stays split round 1, joins to one line round 2. Also seen in
`CharMatcher.java`, `TreeRangeSet.java`, `Streams.java`, and 34 other files
in a fresh guava clone (38 of 1655 files total, flag forced on). Confirmed
pre-existing (reproduces identically against the pipeline from before
`RDD_KEY_343`'s fix) and unrelated to `addClosingComments`/closing
comments — likely a different later pass (declaration-alignment or
`enforceComplexityPadding` are the leading suspects) rejoining what
`enforceOperatorLineBreaking` split, but root cause not investigated.
**2026-08-25 `indent-size=2` follow-up (`RDD_KEY_344`):** tried per this
tracker's usual first move on an idempotency-only flap — did not resolve,
but also did not reproduce at all against a *fresh* guava re-clone even at
default `indent-size` (0/1655, not 38/1655, including all four named
example files individually re-checked and confirmed round1-byte-identical
to round2 both in the corpus batch run and via an isolated single-file
re-format). `src/` is unchanged since `RDD_KEY_343` landed (confirmed via
`git log`), so this is most likely guava-corpus-content drift between the
two clones' commits, not a fix or a regression — not investigated further
(out of scope). Net: no conclusion possible on whether `indent-size=2`
would resolve this flap, since this corpus draw no longer reproduces the
flap to test against; still **not fixed, root cause not investigated**.

A separate, pre-existing, flag-independent baseline of 42/1655 guava files
also differ round1-vs-round2 regardless of this feature (e.g.
`Functions.java`'s anonymous-class closing-brace indentation) — confirmed
unrelated to `line-split-operator-priority` (reproduces with the flag
off), not investigated, out of scope for this job. **2026-08-25
`indent-size=2` follow-up (`RDD_KEY_344`):** re-verified the 42-file
baseline reproduces unchanged at default `indent-size` on the fresh clone
(byte-identical file list flag-on vs. flag-off). At `indent-size=2` the
baseline **mostly self-resolves**: only 2 of the 42 files
(`CollectionToArrayTester.java`, `WriteReplaceOverridesTest.java`) remain
non-idempotent (same flap-(b) diff class, just relocated to different
lines), the other 40 become idempotent. Still **not fixed, root cause not
investigated** for the 2 residual files — `indent-size=2` is not a full
workaround, only a partial one, for this specific flap.

**2026-08-25 follow-up, the 2 residual files FIXED (`RDD_KEY_345`, owned by
the C/C++/Java job, not this one — see `STATE_C_CPP_JAVA.md`'s "Known Gaps
— Fixed" and its own `RDD_KEY_345` entry for the full writeup).** Root-
caused as two distinct, flag-independent, general curly-family bugs, not
specific to `indent-size=2` or to guava's own style: (1)
`CollectionToArrayTester.java`'s flap was `GetterSetterRuleCore.splitMembers`
mistaking a braceless single-line `for(init; cond; incr) body;` statement's
own header `;`s for member terminators (no `(`/`[` depth tracking outside
JS/TS), letting bogus header fragments pair up into a fake column-aligned
"member group"; (2) `WriteReplaceOverridesTest.java`'s flap was
`BlockStructureRule.alignBracelessElseIfChain` speculatively de-indenting an
unrelated `if(` line based on a numeric indent-delta coincidence, with no
rollback when the chain attempt was then rejected. Both fixed narrowly;
verified against the real guava files (indent-size 2 and default, flag on
and off) that round1 is now byte-identical to round2 in every combination.
`make test`: 356/356 -> 358/358, zero regressions. This job's own dogfood
does not need re-running for this — the fix lives entirely in shared
curly-family code owned by the other job.

**Continuation-line alignment-padding drift on operator-split RHS (found
2026-08-25, during the `indent-size=2` spot-check above, `RDD_KEY_344`, not
fixed).** A third, distinct flap from both (a) and (b), only observed at
`indent-size=2` with the flag on (does not reproduce at default
`indent-size`, and does not reproduce with the flag off at either indent
size): `google/guava`'s `LocalCache.java`, an operator-split (tier-1 `||`)
continuation line of an assignment RHS inside a declaration-alignment group
(`this.ticker = ( ticker == Ticker.systemTicker()\n || ticker ==
NULL_TICKER ) ? null : ticker;`) sits at column 8 on round 1 but is
re-indented to column 34 (aligned under the assignment group's `=` column)
on round 2 — an alignment-padding-width recomputation difference between
rounds, not a collapse-to-one-line or brace-indentation issue like (a)/(b).
Root cause not investigated (explicitly out of scope for the low-priority
`indent-size=2` spot-check task that found it) — flagged here for a future
session. `java_syntax_check.sh` confirms both rounds' output stays valid
Java; not a corruption/crash, same framing as (a) and (b).

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
- `test/real_code_regressions_236_{inp,out}.java` — `RDD_KEY_343` fix: a
  `while` loop whose nested braceless `if` condition split pushes the
  loop's own content-line count past `closing-comment-min-lines`, only
  visible to `addClosingComments` after the reordering fix.
- `make test`: 347/347 -> 350/350 -> 351/351 -> 355/355 -> 356/356 forward
  + idempotency, zero regressions (356 after `RDD_KEY_343`'s
  `real_code_regressions_236` fixture).

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
