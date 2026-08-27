# STATE_LINE_SPLIT_OP.md — Operator-Priority Line Splitting Tracker

Read `STATE_COMMON.md` first — shared commit/ambiguity/testing conventions
this file assumes; no other job's `STATE_*.md` is required. Dogfood corpus
status: see `STATE_DOGFOOD.md`.

**2026-08-26 follow-up fix (single-line function-body closing-brace
placement, `RDD_KEY_353`, D14):** fixed the closing-`}` half of the flap D10
(`RDD_KEY_348`) left open — see D14 below and the dated follow-up appended to
the "Single-line function-body brace placement..." Known Out-of-Scope
Finding further down. New fixture `test/real_code_regressions_244_{inp,out}.cpp`
(the exact D10/`RDD_KEY_348` manual repro, now fully idempotent at the tool's
actual default config). `make test`: 363/363 -> 364/364.

**2026-08-25 follow-up investigation + partial fix (declaration/condition
collapse-on-round2 flap, `RDD_KEY_351`, D13):** re-investigated the
"Declaration/condition collapse-on-round2 flap" Known Out-of-Scope Finding
below. First tried hard to reproduce the ORIGINAL Java/`google/guava`
manifestation: (a) re-ran the exact four named files
(`MinMaxPriorityQueue.java`/`CharMatcher.java`/`TreeRangeSet.java`/
`Streams.java`) against the scratchpad's existing fresh guava clone (reused,
not re-cloned) at default config, flag on — 0/4 differ, matching
`RDD_KEY_344`'s prior non-repro; (b) ran the full 1655-file corpus at a
lowered `line-length=60` (an untried combination — `RDD_KEY_344` only tried
non-default `indent-size`) — 44/1655 files differ flag-on, all confirmed by
a line-count-based hunk scan to be either the pre-existing flag-independent
baseline (40 files, unrelated) or a `for(...)`-header-clause indentation
drift (4 files, a distinct flap shape, not chased), zero files showing the
specific "many lines collapse to one" shape; (c) a hand-written minimal
Java repro of the exact `int result = (cond) ? A : B;` declaration shape,
and the real `MinMaxPriorityQueue.java` file run in isolation, both
round-trip clean against current `src/` — confirms `RDD_KEY_344`'s guess
that the original guava commit's specific trigger is gone from this corpus
draw, not a src regression. **Net: the original Java-specific manifestation
still does not reproduce anywhere, same conclusion as `RDD_KEY_344`.**

However, `RDD_KEY_349`'s TS dogfood had already flagged one live recurrence
of this same finding (`angular/angular`'s `create_application.ts`) without
chasing it. Re-investigating found a real, different, fully root-caused bug
(not D12's `parseAssignment` mechanism, which always bails for JS/TS
`const`/`let`/`var` targets so was never in play here) — see D13 below for
the full root-cause/fix writeup
(`DeclarationAlignmentRuleCurly.spansMultipleLines`'s paren/bracket-depth
blind spot). Verified against the real, unmodified `create_application.ts`:
previously 26/27 sampled files were round1-byte-identical-to-round2 (this
file the sole exception, per `RDD_KEY_349`); now 27/27, rendering correctly
instead of collapsing with mangled ternary spacing. `js_ts_syntax_check.sh`
re-run: 26/27 clean, the 1 remaining flagged file (`structure.ts`) confirmed
the same pre-existing, unrelated, flag-independent bug `RDD_KEY_349` already
flagged. Zero regressions re-verified on `square/okio` (313 Kotlin files)
and `google/guava` (same 44-file diff count before/after — this fix doesn't
touch the Java/C/C++ path). New fixture
`test/real_code_regressions_243_{inp,out}.ts`. `make test`: 362/362 ->
363/363. **Status: the JS/TS manifestation is now fixed; the ORIGINAL
Java/guava manifestation this finding was first found from remains
genuinely unreproduced — this finding is only PARTIALLY closed.** Full
text: `RDD_KEY_351`.

**2026-08-25 follow-up fix (continuation-line alignment-padding drift,
`RDD_KEY_350`, D12):** root-caused and fixed the "Continuation-line
alignment-padding drift on operator-split RHS" Known Out-of-Scope Finding
(`RDD_KEY_344`/`RDD_KEY_346`). Reproduced minimally (adapted from the real
`google/guava` `LocalCache.java` trigger) rather than re-cloning guava. Root
cause: not declaration-alignment padding as originally theorized, but
`MiscRuleCore.parseAssignment`'s pre-existing STYLE.md §6 "multi-line
right-hand side" feature, which runs before `enforceOperatorLineBreaking`
ever executes and misclassifies that pass's own already-split output (on a
reformat) as a hand-authored §6 example, re-indenting it to the alignment
group's `=` column. Fixed via a narrow, flag-gated guard in
`parseAssignment` (`isOperatorSplitContinuationIndent`) that recognizes and
leaves alone a continuation line sitting at exactly
`enforceOperatorLineBreaking`'s own documented indent convention. Verified
against the real, unmodified `google/guava` `LocalCache.java` file that
surfaced the finding: round1 now byte-identical to round2. New fixture
`test/real_code_regressions_242_{inp,out}.java`. `make test`: 361/361 ->
362/362. Full text: `RDD_KEY_350`.

**2026-08-25 real-code TS dogfood (`angular/angular` sample, flag forced on,
`RDD_KEY_349`):** first real-code TS-specific validation of this feature's
`?.`/`??`/optional-param landmine safety at scale (prior TS coverage was only
the hand-written `test/line_split_operator_priority_{inp,out}.ts` fixture).
Cloned `angular/angular` fresh (shallow, 1788 `.ts` files under
`packages/*/src/**`) and hand-selected 27 files by grepping for real `?.`/
`??` usage, real optional-parameter signatures, long tier-1/tier-3 candidates,
and a genuine ternary, all filtered to lines near/over the tool's default
`line-length=100` (no adaptation needed this time — angular's own style
naturally crosses 100 in these shapes, unlike the tighter `fmtlib/fmt`/
`square/okio` corpora). A flag-off control at the same config isolated the
feature's own effect: 15/27 files differed flag-on-vs-off, the other 12
(`?.`/`??`-only/optional-param-only) were byte-identical — blast radius
matches the targeted shapes exactly. Confirmed correct on real code: tier-1
`&&`/`||`/`+` splits (`ingest.ts`, `service.ts`, `format_date.ts`,
`shadow_css.ts`, and 6 more); a genuine ternary still splits operator-leading
(`strip_nonrequired_parentheses.ts`'s `return requiredParens.has(expr) ?
expr : expr.expr;`); `?.`/`??` never mistaken for ternary
(`navigation_transition.ts`'s `currentNavigation?.targetBrowserUrl ??
currentNavigation?.extractedUrl` byte-identical flag-on-vs-off); a real
optional-parameter signature's `?` untouched even when its function got
reflowed for an unrelated reason (`url_tree.ts`'s `isActive(url, router,
matchOptions?: Partial<IsActiveMatchOptions>)`); tier-3 confirmed sound via a
synthetic repro (no real tier-3-in-condition candidates in this draw, same
gap as the `square/okio` pass). One genuine, real, in-scope bug found and
fixed — general to the whole curly family but a serious TS-specific
landmine: `findTernarySplits` mistook a bare `:` with no preceding real
ternary `?` for a split candidate (see D11 below for the full root-cause/
fix writeup). New fixture
`test/real_code_regressions_241_{inp,out}.ts`. `make test`: 360/360 ->
361/361. Idempotency: 26/27 sampled files byte-identical round1-vs-round2
post-fix; 1 known recurrence (`create_application.ts`, the documented
declaration/condition collapse-on-round2 flap, not chased — see Known
Out-of-Scope Finding below). `js_ts_syntax_check.sh`: 26/27 clean; 1 file
(`structure.ts`) flagged 7 pre-existing "',' expected" errors, confirmed
flag-independent and unrelated to this feature (reproduces identically with
the flag off and on the raw unformatted original) — a stray `;` inserted
inside an object-literal spread-with-type-cast expression
(`{...(x as T);}`), likely an `enforceSemicolonInsertion`-family ASI bug
akin to `RDD_KEY_339`'s but a different trigger shape; flagged for a future
JS/TS-job session, not fixed here (out of scope for this job). Full text:
`RDD_KEY_349`.

**2026-08-25 real-code Kotlin dogfood (`square/okio` sample, flag forced on,
`RDD_KEY_347`):** first real-code Kotlin-specific validation of this feature
(prior real-code passes were Java/C++ only, via `pcpp_java`/`google/guava`/
`fmtlib/fmt`). Cloned `square/okio` fresh (313 `.kt` files) and sampled 21
files by grepping for real elvis (`?:`), tier-1 (`&&`/`||`/`+`/`-`) in
conditions, tier-3 (`*`/`/`) in assignment/return/if shapes, and long
nullable-type (`Type?`) declarations. `okio`'s own ~100-column style rarely
crossed the tool's default `line-length=100`, so it was lowered to `70` for
this dogfood run only (disclosed adaptation, same spirit as `RDD_KEY_346`);
every finding re-confirmed at the tool's actual default too. Confirmed
correct on real code: tier-1 `&&`/`-` split with recursion
(`AsyncTimeout.kt`'s `awaitTimeout`); tier-3 mechanism confirmed sound via a
synthetic if-condition repro (real `okio` conditions happened to contain no
tier-3 operators in this draw); elvis (including chained `a ?: b ?: c`)
never split as ternary across 15+ real occurrences; nullable-type `?` in
long declarations never touched (`FsJs.kt`'s 103-char `readSync` signature,
`NonJvmPlatform.kt`'s `String?` params) — D4's landmine guard holds on real
Kotlin code. One genuine, real, in-scope bug found and fixed (general to the
whole curly family, not Kotlin-specific, D9 below): `findBinaryOpSplits`
mistook an arithmetic operator inside an array subscript (`arr[i - 1]`,
found via `Options.kt`) for a valid split-point candidate — see D9 for the
full root-cause/fix writeup. New fixture `test/real_code_regressions_240_{inp,out}.kt`.
`make test`: 359/359 -> 360/360. Idempotency: all 21 sampled files
byte-identical round1-vs-round2, both pre-fix and post-fix (this bug was a
fresh-format false-positive, not a flap). `kotlin_syntax_check.sh`: 21/21
clean both pre-fix and post-fix. A separate, real, root-caused gap was found
and documented as a new Known Out-of-Scope Finding (not fixed, per this
task's explicit time-box) rather than chased: Kotlin's `return`/top-level-`=`
operator-split branches never actually fire on real Kotlin source at all
(only `if`/`while`/`switch`/`for` work) — see that section below and
`RDD_KEY_347`'s full text for the root cause. Full text: `RDD_KEY_347`.


**2026-08-25 dogfood/validation (two-pass):** Pass 1 (flag off, routine
self-adopt regression confirm) — Leg A (`tools/*`, 82 files) idempotent,
nothing to adopt. Leg B (`src/**/*.java`, 100 files) idempotent; the only 3
files differing from committed `src/` were this session's own new-feature
files (`Config.java`/`FormatterCurly.java`/`MiscRuleCurly.java`, not yet
self-formatted) — hand-reviewed as ordinary cosmetic re-style, confirming
the feature is a true no-op with the flag off. Trial JAR: `_test_serial`
348/350 (2 pre-existing `gru-sync-weights` trial-JAR-drift failures, not
chased). Adopted; `make clean && make test` 350/350 forward + idempotency.
Leg C (external corpora, `../../JCS`/`../../MDXplorer`/
`../../../3rd_party/tools/pcpp_java`/`colordiff.py`) — byte-identical to
committed originals, nothing to adopt; see `STATE_DOGFOOD.md`.

Pass 2 (flag forced ON via `JXMAKE_CODE_FORMATTER_LINE_SPLIT_OPERATOR_
PRIORITY=on`, scratch copy, read-only): corpus
`../../../3rd_party/tools/pcpp_java` (43 `.java` files, ~10.7K LOC) —
chosen over `../../JCS` (only 1 real `.java` file, rest shell/PowerShell
scripts). round1/round2 `diff -ru` empty (idempotency, exercises
`hasNewlineBetween`'s guard). `java_syntax_check.sh` clean on all 43
files. Only 3 files actually changed vs. the unformatted original
(`LexTab.java`, `ParseTab.java`, `Preprocessor.java`): tier-1 `+` splits on
long string-concatenation assignment RHS (correctly leaving `?`/`:`
substrings *inside* string literals like `"...CPP_QUESTION..."`
untouched) and a tier-1 `&&` split on a long `if` condition — all three
match the designed shape exactly, no false positives. Pass 2 fully clean,
no new fixture needed, no files copied back (read-only per plan).

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
  attention, but unrelated to `line-split-by-operator-priority` and out of
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
condition's closing-paren line) — confirmed a persistent miss (identical
across round 1 AND round 2 pre-fix), not a self-correcting flap as Pass 3
first assumed for that specific line; see D5 below for the fits-check root
cause and fix (extend the measured span through
`effectiveLineEndIndex`, then guard the resulting over-split against a
trailing same-line comment via `enforceCallLineBreaking`'s existing
comment-aware-limit pattern, found via `Streams.java`'s `isParallel`
declaration). New fixture: `test/real_code_regressions_235_{inp,out}.java`
(adapted from the real `Suppliers.java` `memoize` shape), round1/round2
empty diff confirmed for it specifically. Re-verified against a fresh
`google/guava` clone (1655 files, same methodology as Pass 3): round1/
round2 diff dropped from 100 to 47 files, all 47 confirmed (by sampling)
to be the pre-existing closing-comment-min-lines flap (finding 1) only —
finding 2's non-idempotency, including the exact `Suppliers.java:129`
line, is now stable across both rounds. `java_syntax_check.sh` on all 1655
round1 files: zero errors. `make test`: 355/355 forward + idempotency.
Full text: `RDD_KEY_342`.

**2026-08-25 follow-up (closing-comment-min-lines flap fix, `RDD_KEY_343`,
D6):** the remaining Known Out-of-Scope Finding from the section above —
splitting a condition can push an enclosing block's line count across the
`closing-comment-min-lines` threshold, but `addClosingComments` ran before
`enforceOperatorLineBreaking`, so a fresh format's decision didn't see the
post-split count while a reformat of already-split output did. See D6
below for the full root-cause/fix writeup (flag-gated reordering of the
`addClosingComments`/`enforceSwitchExpressionArrowAlignment` pair). New
fixture `test/real_code_regressions_236_{inp,out}.java`. `make test`:
356/356 forward + idempotency. Spot-checked against a fresh `google/guava`
clone: confirmed zero of the remaining round1-vs-round2 differing files
are this flap class anymore. That spot-check surfaced a different,
still-unfixed, pre-existing flap — see the new entry in "Known
Out-of-Scope Finding" below.

**2026-08-25 follow-up (`indent-size=2` spot-check on the two Known
Out-of-Scope flaps, `RDD_KEY_344`, low-priority per the tracker's standing
"indent-size fallback" practice):** reused the existing `google/guava`
clone (1655 `.java` files, same selection as Pass 3/`RDD_KEY_342`/
`RDD_KEY_343`); `make test` green (356/356) before starting, no `src/`
changes made this pass. Four conditions run: flag on/off crossed with
`indent-size` default(4)/2. Full numeric breakdown and per-flap detail
lives in the "Known Out-of-Scope Finding" section below (search
`RDD_KEY_344`) — net result: flap (a) (declaration/condition collapse)
did not reproduce at all in this fresh clone (0/1655 vs. the documented
38/1655, a corpus-content difference between clones, not a src regression
or fix); flap (b) (flag-independent baseline, 42/1655 at default indent)
mostly self-resolves at `indent-size=2` (40/42 become idempotent) but
leaves 2 residual files; flag-on at `indent-size=2` additionally surfaces
one brand-new file (`LocalCache.java`) with a third, distinct
flag-and-indent-size-dependent flap shape (continuation-line
alignment-padding drift, not a collapse/brace issue) — added as a new
Known Out-of-Scope Finding. `java_syntax_check.sh`: 1655/1655 clean, zero
errors (neither flap causes corruption). **Net: `indent-size=2` does not
cleanly resolve either documented flap.** No fix attempted; `make test`
re-confirmed 356/356 at the end. Full text: `RDD_KEY_344`.

**2026-08-25 real-code C/C++ dogfood (`fmtlib/fmt` sample, flag forced on,
`RDD_KEY_346`):** first C/C++-specific real-code validation of this feature
(every prior real-code pass above was Java, via `pcpp_java`/`google/guava`).
Cloned `fmtlib/fmt` fresh and sampled 17 files (15 of 16 `include/fmt/*.h`
headers, `src/os.cc`, `src/fmt-c.cc` -- excluding two trivial C++20-module
shim files with no real content) by grepping for tier-1/2/3/`for`-header/
unary-`*`-near-arithmetic candidates; every sampled file had at least one
hit. Since `fmt`'s own source is already clang-formatted tight (observed
max line length 100 across the whole sampled tree), forcing the flag on at
the tool's own default `line-length=100` found almost nothing to split --
`line-length` was lowered to `60` via `.jxmake-code-formatter` (disclosed
methodology adaptation, same spirit as the `indent-size` fallback rule) to
get real signal; every finding below was independently confirmed to still
reproduce at the tool's own default `line-length=100` via a minimal
extracted repro. Confirmed on real code: tier-1 `&&` split (`base.h`'s
`is_constant_evaluated`), tier-3 `*`/`/` splits (`os.cc`'s `sizeof(DWORD) *
CHAR_BIT`, `format.h`'s bigint-multiply lines), a real `for(...)` header
split whose `sizeof(args) / sizeof(*args)` clause correctly stayed
un-split at the unary `*args` deref (`base.h`) -- D4's landmine guard holds
on real code, not just the hand-written fixture. Two genuine in-scope bugs
found and fixed (see D8) -- both narrowly scoped in `MiscRuleCurly.java`,
new fixture `test/real_code_regressions_239_{inp,out}.cpp`. `make test`:
358/358 -> 359/359. Two already-documented flap classes recurred in this
new corpus at the aggressive `line-length=60` (not re-chased, see the new
Known Out-of-Scope Finding entries below): a single-line function body's
opening brace not re-Allman-ed until round 2 once its return expression is
operator-split, and the continuation-line alignment-padding drift
(`RDD_KEY_344` Finding 3) now confirmed NOT `indent-size=2`/guava-specific.
Both confirmed flag-dependent via a targeted flag-off re-run (2 of the 6
affected files differ flag-off too -- confirmed flag-independent, general
curly-family, not investigated, out of scope). Full text: `RDD_KEY_346`.

---

## Purpose

Tracks the curly-family (C/C++/Java/Kotlin/JS/TS) `line-split-operator-
priority` config key (default off): when an `if`/`while`/`switch`
condition, a `for(...)` header, or a bare `return`/assignment-RHS
expression with no enclosing call parens is too long, splits it at
operator boundaries instead of leaving it long or falling straight to
today's call-argument-paren wrapping. **Fully implemented, `make test`
green (364/364).**

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
  - `reapplySingleLineFunctionBodyClosingBrace(tokens)` (D14, `RDD_KEY_353`)
    — re-derives the "should a non-named scope's closing `}` move onto its
    own line" decision for a body `enforceOperatorLineBreaking` just widened
    from one physical line to several, which
    `ScopePipelineCurly.processScope`'s own, much-earlier one-time decision
    never revisits. Language-general (all curly languages), gated only on
    the flag; identifies a candidate `{` via `Token.name == null` (the same
    named-vs-non-named-scope signal `processScope` itself uses), fires only
    when the pair now spans a genuine top-level newline and `}` is not
    already alone on its own line.
- `src/com/jxmake/formatter/FormatterCurly.java` — exactly ONE gated call
  site: `if( config.lineSplitByOperatorPriority() ) { text =
  miscRule.enforceOperatorLineBreaking( tokenizer.apply(text) ); }`,
  positioned immediately after `blockRule.alignBracelessElseIfChain(...)`
  runs and before the `enforceCallLineBreaking` calls that follow it. See
  Resolved Design Decisions D1 below for why this deviates from inserting
  before all four existing `enforceCallLineBreaking` call sites. D10's
  C/C++-only `enforceFunctionDefinitionAllmanBraceStyle` re-run and D14's
  language-general `reapplySingleLineFunctionBodyClosingBrace` call both
  live inside this same gated block, immediately after
  `enforceOperatorLineBreaking` and ahead of the flag-on `addClosingComments`
  call.
- `src/com/jxmake/formatter/Config.java` — `line-split-by-operator-priority`
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
  `line-split-by-operator-priority` flag, and `alignBracelessElseIfChain`
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
- **D8 — two real-code C/C++ split-point bugs, `fmtlib/fmt` dogfood.**
  `RDD_KEY_346`. (1) The top-level `=` assignment candidate's span (first
  `=` to the statement's own final `;`) had no awareness of a
  comma-separated multi-declarator list (`uint64_t ac = a * c, bc = b * c,
  ...;`), so `findBinaryOpSplits`'s depth-0 scan (tracks `(`/`[` nesting
  only, not statement-level commas) interleaved split points across
  unrelated declarators' own initializers. Fixed via a new
  `hasTopLevelComma` guard that declines the whole `=` candidate when a
  depth-0 comma exists in its RHS span -- conservative: only a trailing
  declarator with no comma before the final `;` (unambiguous on its own)
  still gets considered. (2) `isBinaryOperatorContext` only inspects the
  token PRECEDING a candidate `*`/`/`, so a pointer type closing a
  template argument list (`dynamic_cast<std::filebuf*>(...)`) was
  indistinguishable from real multiplication and got split mid-type. Fixed
  via a new `isPointerTypeBeforeAngleClose` helper (mirrors
  `isGenericWildcardQuestion`'s "also check the trailing bound" pattern)
  excluding a `*` immediately followed by `>`/`ANGLE_BRACKET_CLOSE` from
  tier-3's scan only (`/` has no equivalent meaning, left unguarded). New
  fixture `test/real_code_regressions_239_{inp,out}.cpp` (both bugs, one
  file). `make test`: 358/358 -> 359/359.
- **D9 — array-subscript operator never a valid split point.**
  `RDD_KEY_347`, `square/okio` Kotlin dogfood. `findBinaryOpSplits` picks
  occurrences at "the shallowest depth present" among matches actually
  found — not necessarily depth 0 — so a condition with no depth-0 tier-1/
  tier-3 operator of its own (only a non-tier relational op like `!=`) but a
  tier-1/tier-3 operator nested inside an array subscript (`arr[i - 1]`)
  picked that nested operator as its only, therefore "shallowest," match,
  splitting mid-subscript (`arr[i` / `- 1]...`) — valid Kotlin but never the
  intended shape. Fixed via a new, separately-tracked `bracketDepth`
  counter (`[`/`]` only) that excludes an occurrence outright whenever
  `bracketDepth > 0`, regardless of its `(`/`)`-based `depth` — narrower
  than a blanket depth-0-only restriction, which would also suppress the
  legitimate parenthesized-grouping case (`(a + b) * (c + d)`, no depth-0
  tier-1 op, but the nested `+`s are still the best available split point).
  New fixture `test/real_code_regressions_240_{inp,out}.kt`. `make test`:
  359/359 -> 360/360.
- **D10 — partial fix for the single-line function-body brace-placement
  flap; closing-brace half left open.** `RDD_KEY_348`. Root-caused as two
  independent stale decisions, both made before `enforceOperatorLineBreaking`
  ever runs: (1) `CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle`'s
  opening-`{` one-liner-defer decision (`RDD_KEY_75`); (2)
  `ScopePipelineCurly`'s foundational `scopePipeline.process()` scope-tree
  render (runs once, before Phase 1), whose closing-`}`-own-line decision is
  even earlier than (1). Fixed (1) only: a third re-run of
  `cppRule.enforceFunctionDefinitionAllmanBraceStyle`, gated on the flag and
  `isCOrCpp`, added immediately after `enforceOperatorLineBreaking` and ahead
  of the flag-on `addClosingComments` call (same D6 line-count-visibility
  reasoning). (2) left unfixed: re-running `scopePipeline.process()` wholesale
  is the same fix class already tried and reverted twice for an analogous
  JS/TS case (`RDD_KEY_246`) -- a safe fix needs a new, narrowly-scoped
  closing-brace-reapply helper that doesn't exist yet, not attempted blind
  under this follow-up's time-box. Verified at the tool's actual DEFAULT
  config (not a lowered dogfood `line-length`): round1's opening-`{`
  placement now matches round2 exactly; the closing-`}` placement stays
  non-idempotent. No fixture registered -- no accessible real trigger of this
  bug shape is fully idempotent yet (both stale decisions always co-occur),
  so `STATE_COMMON.md`'s round1==round2 fixture requirement can't be met;
  verified via a documented manual repro instead. `make test`: 360/360 before
  and after, zero regressions (flag- and C/C++-gated change).
- **D11 — a bare `:` needs a real preceding ternary `?` at the same depth;
  `?` immediately followed by `:` is never a ternary opener.** `RDD_KEY_349`,
  `angular/angular` TS dogfood. `findTernarySplits` previously treated EVERY
  depth-matching `:` OP token as a valid ternary else-branch split candidate
  unconditionally — harmless for C/C++/Java/Kotlin (a bare, unpaired `:`
  rarely appears inside an if/while/return/assignment span there) but a
  serious landmine for TypeScript, where a bare `:` is extremely common
  (function-type-alias/arrow-function parameter type annotations,
  object-type-literal and plain-object-literal property colons). Found via
  `url_tree.ts`'s `type PathCompareFn = (container: UrlSegmentGroup,
  containee: UrlSegmentGroup, matrixParams: ParamMatchOptions) => boolean;`
  (three parameter-type-annotation colons, zero real `?` anywhere in the
  RHS, all three wrongly split as if three ternary else-branches) and
  `resource.ts`'s `... ?? { isActive: false };` (the trailing object
  literal's own property colon — the `??` itself never matches the ternary
  `?` scan, since it's tokenized as its own distinct `"??"` OP text, but the
  old unconditional colon check still fired on the unrelated property colon
  that happened to be the sole depth-0 `:` present). Fixed via a new
  per-depth pending-`?`-count tracker (`pendingQuestionAtDepth`, a
  `Map<Integer,Integer>`): a genuine ternary `?` (past the existing
  `isGenericWildcardQuestion` exclusion) increments its depth's pending
  count and is still added to `occ` as before; a `:` is only added to `occ`
  — and only then decrements the count — when a pending `?` actually exists
  at its exact depth, otherwise it's left alone. A second, related shape
  also excluded via a new `isOptionalMarkerQuestion` helper (mirrors
  `isGenericWildcardQuestion`'s/`isPointerTypeBeforeAngleClose`'s
  "check the immediately-adjacent token" pattern): a `?` immediately
  followed by `:` (TS/Java's optional-parameter/-property marker, `x?: T`)
  can never be a genuine ternary opener, since a real ternary's true-branch
  expression is never empty — excluded from priming the pending-count, so
  its paired `:` also correctly finds no pending `?` and is left alone too.
  Chained ternaries (`a ? b : c ? d : e`) are unaffected — each `?`/`:` pair
  still resolves via the same per-depth counter. New fixture
  `test/real_code_regressions_241_{inp,out}.ts`. `make test`: 360/360 ->
  361/361.
- **D12 — continuation-line alignment-padding drift fixed via a narrow,
  flag-gated guard in `parseAssignment`, not a declaration-alignment
  padding-width fix.** `RDD_KEY_350`, fixing the "Continuation-line
  alignment-padding drift on operator-split RHS" Known Out-of-Scope Finding
  (`RDD_KEY_344`/`RDD_KEY_346`). Root cause was not declaration-alignment
  padding-width computation (that finding's original leading theory) but
  `MiscRuleCore.parseAssignment`'s pre-existing, unconditional STYLE.md §6
  "multi-line right-hand side" feature (`RDD_KEY_51`): it runs (via
  `ScopePipelineCurly.applyAssignmentsPass`) before `enforceOperatorLineBreaking`
  ever executes, so a fresh format never sees a pre-split RHS collide with
  it, but a reformat of already-split output does — the split RHS's
  operator-leading second line matches §6's own documented "breaking before
  an operator" shape exactly, so `parseAssignment` misclassifies it as a
  hand-authored §6 example and re-indents it to the alignment group's `=`
  column, a decision `enforceOperatorLineBreaking`'s own `hasNewlineBetween`
  guard then leaves stale forever after. Fixed via a new
  `lineSplitByOperatorPriority` flag (threaded into `MiscRuleCore`/
  `ScopePipelineCurly` post-construction, same "avoid threading a new param
  through every constructor overload" precedent as
  `normalizeCommentMultiSentenceCase`) gating a new
  `isOperatorSplitContinuationIndent` check: when the flag is on and a
  two-line RHS's second line sits at exactly the statement's own indent plus
  one `indentWidth` — `enforceOperatorLineBreaking`'s own documented
  continuation convention, essentially never coincident with a genuine §6
  example's `=`-column-derived indent — `parseAssignment` leaves it as the
  pre-existing verbatim multi-line fallback instead of reclassifying it.
  Flag-off pipeline byte-for-byte unchanged (every existing fixture,
  including every §6/declaration-alignment shape, unaffected). One narrow,
  deliberately accepted trade-off: a hand-authored §6 example whose
  continuation coincidentally sits at that exact indent, with the flag on,
  is now left verbatim instead of realigned to `=` — confirmed harmless
  (flag-off behavior for the same input is unaffected, and the coincidence
  requires both the niche opt-in flag and an unusual hand-typed indent
  choice). Verified against the real, unmodified `google/guava`
  `LocalCache.java` file that surfaced the finding (flag on,
  `indent-size=2`): round1 now byte-identical to round2. New fixture
  `test/real_code_regressions_242_{inp,out}.java`. `make test`: 361/361 ->
  362/362.
- **D13 — a paren/bracket-nested newline in a JS/TS/Kotlin declaration's
  initializer must also count as "spans multiple lines" once operator-split
  output can land one there; fixes one recurrence of the
  declaration/condition collapse-on-round2 finding, not the original
  Java/guava one.** `RDD_KEY_351`, re-investigating `RDD_KEY_349`'s
  `create_application.ts` recurrence. Root cause: unlike D12
  (`parseAssignment`, which always bails for JS/TS `const`/`let`/`var`
  targets and so was never in play here), this is
  `DeclarationAlignmentRuleCurly.spansMultipleLines` (shared by
  `JsTsDeclarationAlignmentRule`/`KotlinDeclarationAlignmentRule`, its only
  two callers) — a pre-existing, feature-independent design choice that only
  ever recognized a NEWLINE as "spans multiple lines" inside a `{`...`}`
  brace body or at full paren/bracket depth 0, deliberately treating a
  newline strictly inside a call/array's parens/brackets as safe to flatten
  (the assumption being only `enforceCallLineBreaking`'s own argument-wrap
  could have put it there, safely re-derivable next pass).
  `enforceOperatorLineBreaking` breaks that assumption: it can split an
  operator nested arbitrarily deep inside an array literal's spread argument
  (`[a, ...( cond ? b : c ), d]`), landing its own newline at paren/bracket
  depth > 0 with nothing left to re-derive it on a reformat — the old
  carve-out still calls it safe to flatten, and the generic row-rendering
  that follows doesn't reapply `enforceOperatorLineBreaking`'s own ternary
  spacing either, mangling `?`/`:` spacing on top of losing the split. Fixed
  narrowly and flag-gated (same precedent as D12): a new
  `lineSplitByOperatorPriority` field + setter on
  `DeclarationAlignmentRuleCurly` (consulted only by `spansMultipleLines`;
  harmless on the C/C++/Java subclass, which never calls that method), wired
  from `ScopePipelineCurly.setLineSplitByOperatorPriority` into both
  `jsTsDeclarationRule`/`kotlinDeclarationRule`; when on, a newline at
  paren/bracket depth > 0 (brace depth 0) also counts as multi-line. Flag-off
  pipeline byte-for-byte unchanged. Verified against the real, unmodified
  `angular/angular` `create_application.ts` file: the TS dogfood sample went
  from 26/27 to 27/27 round1-byte-identical-to-round2, with correct output
  (proper per-element array layout, correct ternary spacing) instead of a
  collapsed, mangled-spacing line. Zero regressions on `square/okio` (313
  files) or `google/guava` (1655 files, lowered `line-length`, unaffected —
  this fix doesn't touch the Java/C/C++ path). New fixture
  `test/real_code_regressions_243_{inp,out}.ts`. `make test`: 362/362 ->
  363/363. **Despite extensive re-investigation (fresh-clone re-check, a
  full-corpus lowered-`line-length` run, and an isolated single-file/hand-
  written repro), the ORIGINAL Java/`google/guava` manifestation this
  finding was first found from (`MinMaxPriorityQueue.java`/`CharMatcher.java`/
  `TreeRangeSet.java`/`Streams.java`) still does not reproduce anywhere —
  same conclusion as `RDD_KEY_344`. This finding is only PARTIALLY closed:
  the JS/TS manifestation is fixed, the original Java one remains an open,
  unreproducible question.**
- **D14 — closing-`}` half of the single-line function-body brace-placement
  flap fixed via a new, narrowly-scoped, language-general re-run — not a
  `scopePipeline.process()` re-run.** `RDD_KEY_353`, closing the gap D10
  (`RDD_KEY_348`) left open. Root cause fully isolated (via targeted debug
  prints removed before committing, per `STATE_COMMON.md`'s "prefer evidence
  over reasoning"): `ScopePipelineCurly.processScope`'s foundational
  scope-tree render (the one-time pass that runs once, before Phase 1, well
  before `enforceOperatorLineBreaking` ever executes) decides, for every
  non-named (function/loop/lambda) scope, whether its body is a genuine
  one-liner by checking for a paren/bracket-depth-0 NEWLINE at THAT point in
  the pipeline; when it still looks single-line, the body is spliced back
  verbatim (`childResult = childSource`), entirely skipping the "force the
  trailing gap before `}` onto its own line" reindent logic a body that is
  ALREADY multi-line at that same point receives. `enforceOperatorLineBreaking`
  (Phase 4) can turn exactly such a one-liner body multi-line via a
  return/condition/assignment operator split, but that earlier decision is
  never revisited, so the closing `}` stays glued to whatever fragment the
  split rendered last. Fixed via a brand-new helper,
  `MiscRuleCurly.reapplySingleLineFunctionBodyClosingBrace`, called once
  immediately after `enforceOperatorLineBreaking` (and after D10's C/C++-only
  `enforceFunctionDefinitionAllmanBraceStyle` re-run, same relative position),
  gated only on `config.lineSplitByOperatorPriority()` — no language
  restriction, since investigation showed the underlying stale decision lives
  in `ScopePipelineCurly`, shared by every curly-family language, not just
  C/C++. The new pass mirrors `ScopePipelineCurly.
  applyOversizedAggregateInitClosingBracePass`'s own "was this brace pair one
  physical line before, is it several now, is `}` still glued to the last
  one" shape, but keyed on a non-named scope's own body brace (`Token.name ==
  null`, the exact same signal `processScope` itself uses to distinguish a
  named vs. non-named scope) rather than an aggregate initializer's `= {
  ... };`. It only ever touches the single gap immediately before a
  qualifying `}` when the pair now spans a genuine top-level newline
  (paren/bracket-depth-aware, so an ordinary call-argument wrap strictly
  inside a nested call's own parens is correctly left alone, same carve-out
  precedent as the aggregate-init pass) — deliberately NOT a re-run of
  `scopePipeline.process()`/`processScope` itself, the exact fix class
  already tried and reverted twice for the analogous JS/TS closing-brace
  staleness (`RDD_KEY_246`, "regressed unrelated already-correct
  constructs"). Confirmed via manual repro that the identical closing-brace
  staleness also reproduces for Java and TypeScript, not just C/C++ (README's
  prior "most reliably observed in C/C++" language was investigated, not
  assumed) — but those two languages have a SEPARATE, pre-existing gap on the
  *opening*-`{` side of this same trigger (their own Allman-brace passes were
  never given D10's re-run treatment either, a distinct, still-open gap not
  fixed by this follow-up, out of scope here), so Java/TS remain
  non-idempotent overall even after this fix; only C/C++ — where D10 already
  fixed the opening-brace half — reaches full round1==round2 idempotency from
  this fix alone. Verified at the tool's actual DEFAULT config (`line-length=
  100`, no override), using the exact manual repro `RDD_KEY_348` built:
  `constexpr auto pick() -> int { return someVeryLongConditionExpressionName
  > anotherVeryLongThresholdName ? 1111111 : 2222222; }` — round1 now
  byte-identical to round2 (previously divergent: round1 kept `}` glued to
  `: 2222222;`, round2 alone). Also verified against the real, unmodified
  `fmtlib/fmt` `base.h` file (the same corpus D8/D10 used, `line-length=60`
  disclosed dogfood adaptation matching that prior session's own
  methodology): the real `count()`/`str()`/`size()` triggers named in D10's
  Known Out-of-Scope Finding are now round1-byte-identical-to-round2 across
  the whole file. New fixture `test/real_code_regressions_244_{inp,out}.cpp`
  (the D10/`RDD_KEY_348` manual repro, registered at the tool's actual
  default config — no in-file `line-length` override needed). `make test`:
  363/363 -> 364/364, zero regressions (confirmed the flag-off pipeline is
  untouched — the new pass is gated entirely behind
  `config.lineSplitByOperatorPriority()` — and that D10's own opening-brace
  fix is undisturbed).

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

**2026-08-25 follow-up, one recurrence FIXED, original manifestation still
unreproduced (`RDD_KEY_351`, D13).** Re-investigated after `RDD_KEY_349`'s
TS dogfood flagged `angular/angular`'s `create_application.ts` as a live
recurrence of this same finding without chasing it. Tried hard again to
reproduce the ORIGINAL Java/guava manifestation first: the four named files
against a fresh guava clone (0/4 differ, same as `RDD_KEY_344`), the full
1655-file corpus at a lowered `line-length=60` (an untried combination —
zero files showed a line-count-collapse shape), and an isolated
single-file/hand-written repro of the exact declaration shape (round-trips
clean) — **the original Java manifestation still does not reproduce
anywhere; still not confirmed fixed or broken.** But the flagged TS
recurrence turned out to be a real, different, fully root-caused bug (not
D12's `parseAssignment` mechanism, which always bails for JS/TS
`const`/`let`/`var`): `DeclarationAlignmentRuleCurly.spansMultipleLines`
(shared by `JsTsDeclarationAlignmentRule`/`KotlinDeclarationAlignmentRule`)
only ever recognized a NEWLINE as "spans multiple lines" inside a brace body
or at full paren/bracket depth 0, deliberately treating a newline strictly
inside a call/array's parens as safe to flatten (assuming only
`enforceCallLineBreaking`'s own argument-wrap could put it there) —
`enforceOperatorLineBreaking` breaks that assumption by splitting an
operator nested inside an array literal's spread argument, landing a
newline at paren/bracket depth > 0 that nothing re-derives on a reformat.
Fixed narrowly, flag-gated (see D13) — full text `RDD_KEY_351`. **This
finding is now only PARTIALLY closed**: the JS/TS manifestation is fixed;
the original Java manifestation (the rest of this section, below) remains
open.

A separate, pre-existing, flag-independent baseline of 42/1655 guava files
also differ round1-vs-round2 regardless of this feature (e.g.
`Functions.java`'s anonymous-class closing-brace indentation) — confirmed
unrelated to `line-split-by-operator-priority` (reproduces with the flag
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

**2026-08-25 follow-up (`fmtlib/fmt` C/C++ dogfood, `RDD_KEY_346`):**
confirmed this same flap class is NOT `indent-size=2`-specific or
guava/Java-specific — it reproduces at default `indent-size` in a wholly
different corpus (`fmtlib/fmt`) and language (C/C++) purely from a lower
`line-length` (60, this dogfood's own methodology adaptation), on tier-1
`+` rather than tier-1 `||` (`include/fmt/color.h`'s
`buffer[size++] = static_cast<Char>('0'\n + value / 10u);`, also seen in
`chrono.h`/`format.h`). Confirmed flag-dependent (does not reproduce with
the flag off at the same `line-length=60`). Root cause still not
investigated — broadens this finding's known scope but doesn't change its
status.

**2026-08-25 follow-up, FIXED (`RDD_KEY_350`, D12).** Root-caused: not a
declaration-alignment padding-width computation as this finding's original
leading theory guessed — `MiscRuleCore.parseAssignment`/`groupAssignments`
(STYLE.md §6's own pre-existing "multi-line right-hand side" feature,
`RDD_KEY_51`), which runs via `ScopePipelineCurly.applyAssignmentsPass` well
before `enforceOperatorLineBreaking` ever executes. On a fresh format the RHS
is still single-line at that point, so nothing collides; on a reformat, this
pass now sees the already-split two-physical-line RHS, and since its second
line happens to start with an operator, misclassifies it as a hand-authored
§6 example and re-indents it to the alignment group's `=` column — a stale
decision `enforceOperatorLineBreaking`'s own `hasNewlineBetween` idempotency
guard then leaves untouched forever after. Fixed via a new, flag-gated
`isOperatorSplitContinuationIndent` guard in `parseAssignment`: when the flag
is on and a two-line RHS's second line sits at exactly the statement's own
indent plus one `indentWidth` (this feature's own documented continuation
convention, essentially never coincident with a genuine §6 example's
`=`-column-derived indent), `parseAssignment` leaves it as the verbatim
multi-line fallback instead of reclassifying it — undisturbed,
byte-for-byte, on every subsequent round. Verified against the real,
unmodified `google/guava` `LocalCache.java` file (flag on, `indent-size=2`):
round1 now byte-identical to round2. New fixture
`test/real_code_regressions_242_{inp,out}.java`. `make test`: 361/361 ->
362/362. Full text: `RDD_KEY_350`.

**Single-line function-body brace placement not re-evaluated after a
return-expression operator split (found 2026-08-25, `fmtlib/fmt` dogfood,
`RDD_KEY_346`, not fixed).** A fourth, distinct flap: a single-line
function body whose `return` expression contains an operator eligible for
splitting (`template <bool B = false> constexpr auto count() -> int {
return B ? 1 : 0; }`-shaped, e.g. `include/fmt/base.h`'s `count()`,
`include/fmt/compile.h`'s `str()`, `include/fmt/format.h`'s `size()`) is
correctly operator-split on round 1, but keeps its opening `{` attached to
the signature line (`... -> int { return B` / `? 1` / `: 0; }`) instead of
being moved onto its own line by whatever later pass normally re-flows a
now-multi-line body to this codebase's Allman brace style — that move only
happens on round 2, once the body is already multi-line on input. Same
general "an earlier pipeline decision doesn't see
`enforceOperatorLineBreaking`'s post-split shape" family as `RDD_KEY_342`/
`RDD_KEY_343` (D5/D6), but for single-line-function-body brace placement
rather than a collapsed condition body or a closing-comment line-count
threshold — root cause not isolated to a specific method, not chased
(explicitly out of scope per this dogfood pass's time-box). Confirmed
flag-dependent (does not reproduce with the flag off at the same
`line-length=60`). `cpp_syntax_check.sh` confirms both rounds' output
stays valid C++; not a corruption/crash, same framing as the findings
above.

**2026-08-25 follow-up, root cause fully isolated, HALF FIXED
(`RDD_KEY_348`, D10).** The single flap above is actually two independent
stale-decision passes, both made before `enforceOperatorLineBreaking` runs:
(1) `CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle`'s opening-`{`
one-liner-defer decision (fixed: a third re-run was added immediately after
`enforceOperatorLineBreaking`, flag- and C/C++-gated); (2)
`ScopePipelineCurly.process()`'s foundational closing-`}`-own-line decision,
made even earlier (before Phase 1) — not fixed, since a safe fix needs a new
narrowly-scoped re-run helper that doesn't exist yet, and re-running
`process()` wholesale is the same fix class already reverted twice for an
analogous JS/TS bug (`RDD_KEY_246`). Confirmed via a repro built specifically
against the tool's actual DEFAULT config (not a lowered dogfood
`line-length`) that this is a real, in-scope bug. Post-fix, the opening-`{`
placement is now idempotent (matches what round2 already produced); the
closing-`}` placement is still not — the flap's diff surface is narrowed,
not eliminated. Still **not fully fixed** — the closing-brace half remains
open for a future session. No fixture: no real trigger of this bug shape is
fully idempotent yet with only (1) fixed, so no case exists that would pass
`make test`'s round1==round2 check.

**2026-08-26 follow-up, closing-`}` half FIXED (`RDD_KEY_353`, D14) — this
flap is now fully closed for C/C++.** Cause (2) above is fixed, not via a
`scopePipeline.process()` re-run (still the closed-off fix class per
`RDD_KEY_246`) but via a brand-new, narrowly-scoped helper,
`MiscRuleCurly.reapplySingleLineFunctionBodyClosingBrace`, run once
immediately after `enforceOperatorLineBreaking` (see D14 above for the full
root-cause/fix writeup). Verified at the tool's actual DEFAULT config using
the exact manual repro this row's own text names above
(`constexpr auto pick() -> int { return ... ? 1111111 : 2222222; }`): round1
is now byte-identical to round2. New fixture
`test/real_code_regressions_244_{inp,out}.cpp`. Also confirmed general to
the whole curly family, not C/C++-specific: the identical closing-brace
staleness reproduces for Java and TypeScript too, but those two languages
carry a SEPARATE, still-open, pre-existing gap on the *opening*-`{` side of
this same trigger (their own Allman-brace passes were never given D10's
C/C++-only re-run treatment) — not fixed by this follow-up, so Java/TS
remain non-idempotent overall for this trigger shape even now; only C/C++
reaches full round1==round2 idempotency from this fix alone. `make test`:
363/363 -> 364/364, zero regressions.

**Kotlin `return`/top-level-`=`-assignment operator-split branches never
actually fire on real source (found 2026-08-25, `square/okio` dogfood,
`RDD_KEY_347`, not fixed).** Unlike `if`/`while`/`switch`/`for` (which
correctly operator-split for Kotlin, confirmed on real code — see the
dogfood summary above), the `return`-expression and top-level `=`-assignment
candidate branches in `enforceOperatorLineBreaking` never actually split
anything for Kotlin, at any `line-length`, with or without an explicit
trailing `;` in the source. Confirmed via real code (`square/okio`'s
`Options.kt`: `val childNodesOffset = nodeOffset + node.intCount + 2 +
(selectChoiceCount * 2)`, 87 chars, stays on one line even down to a
synthetic `line-length=40`) and a minimal repro (`val x = a + b + (c * 2)`/
`return a + b + (c * 2)`, both `fun` bodies, tried with and without an
explicit `;`). Root cause: `KotlinSpecificRule.stripOptionalSemicolons` runs
in Phase 1, well before `enforceOperatorLineBreaking` (Phase 4);
`findStatementSemicolon` (shared by only the `return`/assignment candidate
branches — `if`/`while`/`switch`/`for` locate their span via paren-matching
instead, unaffected) looks solely for a literal `;` PUNCT token to find the
statement's end, which no longer exists for Kotlin's semicolon-free
statements by the time this pass runs — confirmed via the repro above
showing an explicit `;` makes no difference, since it's already stripped
upstream. Not fixed: a correct fix needs real Kotlin newline-sensitivity
(an ASI-equivalent) distinguishing a genuine end-of-statement NEWLINE from a
continuation NEWLINE across method-chaining (`.foo()` starting a new line),
multi-line if/when-used-as-expression bodies, elvis continuation, and
trailing lambdas — a materially bigger, separately-scoped feature with real
risk of corrupting valid Kotlin source if a naive heuristic got a common
shape wrong (e.g. reusing `GetterSetterRuleCore.isAsiContinuation`'s
existing JS/TS-only ASI heuristic verbatim would still mis-handle
method-chaining, since that heuristic only inspects the token BEFORE the
candidate NEWLINE, not what follows it). Time-boxed per this task's explicit
instructions rather than risked. Confirmed NOT a correctness/corruption bug
in itself — the gap is a silent no-op, byte-identical to the flag-off
baseline for these two statement shapes, and elvis-vs-ternary safety and
nullable-type safety are both independently unaffected (verified separately
in the dogfood summary above).

**2026-08-26 re-investigation, CONFIRMED NOT FEASIBLE as a narrow fix, closed
without implementing (no source change).** Re-opened this finding to
seriously assess a narrow, safe fix (mirroring `if`/`while`/`for`'s own
paren/depth-based span-finding instead of `findStatementSemicolon`) per this
follow-up task's explicit instructions, rather than leaving the original
assessment unchallenged. Read `KotlinSpecificRule.stripOptionalSemicolons`/
`isTrailingSemicolon`, `MiscRuleCurly.findStatementSemicolon`/
`enforceOperatorLineBreaking`'s return/assignment branches, and
`GetterSetterRuleCore.isAsiContinuation` end-to-end; confirmed no other
Kotlin-newline-sensitivity logic exists anywhere else in `src/` (repo-wide
grep). A depth-tracking, newline-based statement-end scan (bail outright,
rather than guess, whenever the token before or after the first depth-0
candidate NEWLINE is ambiguous — declining the whole candidate exactly like
today's silent no-op) looked initially promising for the four named
continuation shapes: method-chaining and elvis both lead with a token
(`.`/`?.`/`?:`) that can never legally start a new Kotlin statement, so
detecting them with full confidence and bailing is straightforward; a
brace-enclosed multi-line `if`/`when`-as-expression body is already handled
for free by the same bracket-depth tracking `findStatementSemicolon` already
does (a NEWLINE inside the body's own `{`...`}` is never at depth 0); a
braceless multi-line `if`/`when`/`try` expression body and a trailing lambda
starting on its own continuation line were judged too ambiguous for a cheap
confident check and would need to be declined outright (an acceptable,
narrow trade-off on their own).

However, empirical testing against a real Kotlin compiler (downloaded
`kotlin-compiler-2.4.0` fresh into this session, not present in the
container by default — a real, compiled-and-executed `.kt` snippet, not just
a syntax-only PSI parse) surfaced a second, independent, more fundamental
problem that the original finding didn't name: this feature's own
`renderTieredSplit`/`renderFragment` output shape is unconditionally
**operator-leading** (the split operator starts the continuation line) for
every language and every candidate kind, including `if`/`while`/`for`'s
existing, already-shipped Kotlin support — safe there only because that
split always happens strictly inside the condition's own `(...)`, where
Kotlin (like most languages) does not treat a NEWLINE as statement-ending at
all. A `return`/assignment RHS candidate is, by this whole candidate
category's own definition, NOT inside any enclosing call parens, so an
operator-leading split there would place a real top-level NEWLINE directly
before the operator for the first time. Tested directly (compiled and RUN,
not just parsed) with `kotlinc`: a value's initializer followed on the next
line by a leading `&&`/`||` correctly continues as one expression (matches
this feature's intended meaning) — but the same shape with a leading `+`/`-`
silently compiles as **two separate statements** instead (the original
initializer keeps its un-split value, and the leading `+`/`-` becomes a
separate, no-op unary-prefix expression statement — a real, silent
behavior-changing corruption, not caught by a syntax-only check), and the
same shape with a leading `*`/`/` fails to compile at all (a hard syntax
error). This means a safe implementation would need not only a real
Kotlin-ASI-equivalent for span-finding, but ALSO a second, independent
per-operator safety restriction on which operators this feature's existing,
cross-language operator-leading rendering convention may ever use for a
Kotlin return/assignment split specifically (in effect, a different render
style for this one case, or restricting Kotlin's top-level tier-1 splits to
`&&`/`||` only and refusing tier-3 `*`/`/` entirely) — confirming, with
concrete evidence rather than the original finding's reasoning alone, that
this is a materially bigger, separately-scoped feature, not a narrow fix.
Per this follow-up task's explicit instructions, decided NOT to implement
even the narrower span-finding-only piece on its own (it would still need
the render-side restriction to be safe, which is out of this narrow fix's
scope) — no `src/` change made. `XL.txt`'s `TIER 9 -- [Feature]` entry for
this item moved to `TIER X: Dead` with a short closed-reason annotation
pointing back here.

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
- `test/real_code_regressions_239_{inp,out}.cpp` — `RDD_KEY_346` fix (first
  C/C++-specific real-code fixture for this job): a comma-separated
  multi-declarator assignment statement (tier-3 split points interleaved
  across unrelated declarators) and a pointer type closing a template
  argument list (`Type*>` mistaken for tier-3 multiplication), combined in
  one file.
- `test/real_code_regressions_240_{inp,out}.kt` — `RDD_KEY_347` fix (first
  Kotlin-specific real-code fixture for this job): an array-subscript
  arithmetic operator (`arr[i - 1]`) mistaken for a valid tier-1 split
  point when no depth-0 tier-1/tier-3 operator exists elsewhere in the
  condition.
- `test/real_code_regressions_241_{inp,out}.ts` — `RDD_KEY_349` fix (first
  TS-specific real-code fixture for this job): a bare `:` with no preceding
  real ternary `?` (a function-type-alias parameter list's type
  annotations, and a trailing object-literal property colon after a
  nullish-coalescing fallback) mistaken for a ternary else-branch.
- `test/real_code_regressions_242_{inp,out}.java` — `RDD_KEY_350` fix
  (adapted from the real `google/guava` `LocalCache.java` trigger): an
  operator-split assignment RHS continuation line inside a multi-declaration
  alignment group, re-indented under the group's `=` column on a second
  format instead of keeping its first-format indentation.
- `test/real_code_regressions_243_{inp,out}.ts` — `RDD_KEY_351` fix
  (adapted from the real `angular/angular` `create_application.ts`
  trigger): a declaration's already-multi-line array-literal initializer,
  with a newline sitting inside a spread argument's parens rather than at
  the top level, mistaken for single-line and re-flattened with mangled
  ternary spacing.
- `test/real_code_regressions_244_{inp,out}.cpp` — `RDD_KEY_353` fix (the
  exact `RDD_KEY_348`/D10 manual repro, registered at the tool's actual
  default config): a single-line function body's closing `}` stayed glued to
  the last operator-split fragment instead of moving onto its own line once
  the `return` expression's ternary was split.
- `make test`: 347/347 -> 350/350 -> 351/351 -> 355/355 -> 356/356 ->
  359/359 -> 360/360 -> 361/361 -> 362/362 -> 363/363 -> 364/364 forward +
  idempotency, zero regressions (364 after `RDD_KEY_353`'s
  `real_code_regressions_244` fixture).
- **2026-08-25 real-code C/C++ dogfood (`RDD_KEY_346`):** `fmtlib/fmt`
  sample (17 files, flag forced on) — first C/C++-specific real-code
  validation of this feature. Confirmed correct tier-1/tier-3 splits, a
  real `for(...)` header split, and D4's unary-`*` landmine guard holding
  on real code. Found and fixed 2 genuine split-point bugs (D8). Found 2
  already-documented flap classes recurring in this new corpus (not
  chased, see Known Out-of-Scope Finding above). `cpp_syntax_check.sh`: 0
  errors on all 17 sampled files, both before and after the fix.
- **2026-08-25 real-code Kotlin dogfood (`RDD_KEY_347`):** `square/okio`
  sample (21 files, flag forced on) — first Kotlin-specific real-code
  validation of this feature. Confirmed correct tier-1 splits (with tier-1
  recursion), elvis-vs-ternary safety, and nullable-type safety on real
  code; tier-3 mechanism confirmed sound via a synthetic if-condition
  repro (no real tier-3-in-condition candidates existed in this corpus
  draw). Found and fixed 1 genuine split-point bug (D9, general to the
  curly family). Found and documented (not fixed, time-boxed) 1 new gap:
  Kotlin's `return`/assignment operator-split branches never fire on real
  source at all (see Known Out-of-Scope Finding above).
  `kotlin_syntax_check.sh`: 21/21 clean on all sampled files, both before
  and after the fix.
- **2026-08-25 follow-up fix (`RDD_KEY_348`, D10):** half-fixed the
  single-line function-body brace-placement flap (see Known Out-of-Scope
  Finding above) — opening-`{` staleness fixed, closing-`}` staleness left
  open (needs a new helper, out of scope). No fixture (no real trigger of
  this shape is fully idempotent with only half the bug fixed). `make test`:
  360/360 before and after, zero regressions.
- **2026-08-25 real-code TS dogfood (`RDD_KEY_349`):** `angular/angular`
  sample (27 files, flag forced on) — first TS-specific real-code
  validation of this feature's `?.`/`??`/optional-param landmine safety at
  scale. Confirmed correct tier-1/tier-2 (genuine ternary) splits, `?.`/`??`
  never mistaken for ternary, and a real optional-parameter `?` staying
  untouched, all on real code; tier-3 mechanism confirmed sound via a
  synthetic repro (no real tier-3-in-condition candidates in this corpus
  draw). Found and fixed 1 genuine split-point bug (D11, general to the
  curly family but a TS-specific landmine in practice: a bare `:` with no
  preceding real ternary `?`). `js_ts_syntax_check.sh`: 26/27 clean; the 1
  flagged file's errors confirmed pre-existing and flag-independent
  (unrelated bug, out of scope, flagged for a future JS/TS-job session).
- **2026-08-25 follow-up fix (`RDD_KEY_350`, D12):** fixed the
  continuation-line alignment-padding drift flap (see Known Out-of-Scope
  Finding above, now marked FIXED) — root-caused to `parseAssignment`'s
  STYLE.md §6 multi-line-RHS feature misclassifying operator-split output on
  a reformat; fixed via a narrow, flag-gated indent-shape guard. Verified
  against the real, unmodified `google/guava` `LocalCache.java` file that
  surfaced the finding. `make test`: 361/361 -> 362/362, zero regressions.
- **2026-08-26 follow-up fix (`RDD_KEY_353`, D14):** fixed the closing-`}`
  half of the single-line function-body brace-placement flap (see Known
  Out-of-Scope Finding above, now fully closed for C/C++) — root-caused to
  `ScopePipelineCurly.processScope`'s foundational scope-tree render leaving
  a still-one-liner-at-that-point non-named scope's body spliced back
  verbatim, never revisited once `enforceOperatorLineBreaking` later widens
  it; fixed via a new, narrowly-scoped, language-general re-run helper (not
  a `scopePipeline.process()` re-run). Verified at the tool's actual DEFAULT
  config against the exact `RDD_KEY_348` manual repro (now round1==round2)
  and against the real, unmodified `fmtlib/fmt` `base.h` file (`line-length=
  60`, matching D8/D10's own dogfood methodology). Confirmed general to the
  whole curly family (Java/TypeScript hit the same closing-brace staleness,
  though a separate, still-open, pre-existing opening-brace gap keeps those
  two non-idempotent overall). New fixture
  `test/real_code_regressions_244_{inp,out}.cpp`. `make test`: 363/363 ->
  364/364, zero regressions.

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
- [x] Real-code C/C++ dogfood (`fmtlib/fmt` sample, flag forced on,
      `RDD_KEY_346`) — 2 bugs found and fixed, 2 known flaps recurred
      (documented, not chased).
- [x] Real-code Kotlin dogfood (`square/okio` sample, flag forced on,
      `RDD_KEY_347`) — 1 bug found and fixed (D9, general curly-family),
      1 new gap found and documented (Kotlin return/assignment split
      never fires, time-boxed, not chased).
- [x] Single-line function-body brace-placement flap (`RDD_KEY_348`, D10;
      `RDD_KEY_353`, D14) — fully fixed for C/C++: opening-`{` staleness
      fixed by D10, closing-`}` staleness fixed by D14 via a new,
      narrowly-scoped, language-general re-run helper (not a
      `scopePipeline.process()` re-run). Java/TypeScript hit the same
      closing-brace staleness (also fixed by D14) but carry a SEPARATE,
      still-open, pre-existing opening-brace gap (their own Allman-brace
      passes never given D10's re-run treatment) — out of scope, not fixed,
      those two languages remain non-idempotent overall for this trigger.
- [x] Real-code TS dogfood (`angular/angular` sample, flag forced on,
      `RDD_KEY_349`) — 1 bug found and fixed (D11, a bare `:` mistaken for a
      ternary else-branch with no preceding real `?`, a TS-specific
      landmine); `?.`/`??`/optional-param safety and tier-1/tier-2
      correctness confirmed on real code at scale. 1 pre-existing,
      flag-independent, unrelated bug found and flagged (not fixed, out of
      scope for this job) via `js_ts_syntax_check.sh`.
- [x] Continuation-line alignment-padding drift flap fixed (`RDD_KEY_350`,
      D12) — root-caused to `parseAssignment`'s STYLE.md §6 multi-line-RHS
      feature misclassifying `enforceOperatorLineBreaking`'s own
      continuation-line output on a reformat; fixed via a narrow, flag-gated
      indent-shape guard. Verified against the real, unmodified
      `google/guava` `LocalCache.java` file that surfaced the finding.
- [~] Declaration/condition collapse-on-round2 flap (`RDD_KEY_351`, D13) —
      PARTIALLY fixed. The JS/TS manifestation `RDD_KEY_349` flagged
      (`angular/angular`'s `create_application.ts`) is root-caused and fixed
      (`spansMultipleLines`'s paren-depth blind spot, flag-gated). The
      ORIGINAL Java/`google/guava` manifestation this finding was first
      found from still does not reproduce anywhere despite a fresh-clone
      re-check, a full-corpus lowered-`line-length` run, and an isolated
      repro attempt — genuinely unconfirmed, left open for a future session
      with a different guava commit or a real novel trigger.
- [x] Kotlin `return`/plain-assignment operator-split gap (`RDD_KEY_347`) —
      re-investigated 2026-08-26 to seriously assess a narrow, safe fix; a
      real Kotlin compiler confirmed a second, independent corruption vector
      beyond the original ASI/statement-boundary problem (this feature's
      own cross-language operator-leading render convention itself silently
      changes program behavior for a Kotlin `+`/`-` split and hard-errors
      for a `*`/`/` split, outside the safety of an enclosing `if`/`while`/
      `for` condition's parens). CLOSED as not feasible without a
      separately-scoped Kotlin ASI subsystem; no `src/` change made. See the
      dated follow-up on the Known Out-of-Scope Finding above and `XL.txt`'s
      `TIER X: Dead` entry.
