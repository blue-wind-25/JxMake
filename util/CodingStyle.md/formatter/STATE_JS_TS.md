# STATE_JS_TS.md — JavaScript / TypeScript JAR Support Tracker

Read `STATE_COMMON.md` first (shared commit/ambiguity/testing conventions).
`STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` are not required reading for this job.
Dogfood corpus status: see `STATE_DOGFOOD.md`.

Full historical narrative (root-cause writeups) lives in `git log` for this
directory, not duplicated here — this file tracks current state only.

---

## Purpose

Real formatting logic for JavaScript/TypeScript, per `STYLE_JS_TS.md` (which
derives most rules from `STYLE_JAVA.md`/`STYLE_KOTLIN.md` given JS/TS's
C-family brace/paren/statement shape). Scaffold gate is flipped
(`Lang.isScaffoldOnly` no longer includes js/ts) and all §1–15 rules are
implemented in `JsTsSpecificRule.java` (+ `JsTsDeclarationAlignmentRule.java`
for the declaration-alignment grid), wired into `FormatterCurly`'s phase
pipeline. Current `make test`: 261/261 forward + idempotency (grows as
fixtures are added; see dogfood sections below for count history).

---

## Status Summary

All planned baseline work is **DONE**: §1–15 implemented, JS and TS local
fixtures active, and real-code dogfood passes completed for
`expressjs/express`, `nestjs/nest`, `vuejs/core`, `lodash/lodash`,
`angular/angular` (categorized, all clusters fixed — cluster 4's residue
was itself split into 3 findings across two sessions, all 3 now fixed, see
RDD_KEY_269/RDD_KEY_271), and `microsoft/TypeScript` (categorized, all
clusters fixed, see RDD_KEY_270). No dogfood finding remains open as of
RDD_KEY_271 — see "Active work" below. JS/TS basics were deliberately
hardened to a stable baseline before Python3 (next job in rotation) per
user direction.

---

## Scope

`STYLE_JS_TS.md` covers latest ECMAScript (ES2024+) and latest TypeScript
(5.x), one shared file for both (TS is a syntactic superset of JS). **Out of
scope entirely** (not just deferred): **JSX/TSX** — they will need their own
future embedding-aware dispatcher (JSX embeds tag syntax directly inside JS/TS
expression position, a compound-language situation, not a same-file
extension like HTML5's `<script>` splicing). `STYLE_JS_TS.md` puts JSX/TSX
out of scope entirely, not merely deferred.

Sections 1–15 (baseline-inherited rules, semicolon insertion, destructuring/
spread, template literals, function/arrow brace style, optional chaining,
getter/setter, decorators, async/await, type annotations, enums, generics,
interface/type-alias member alignment, import ordering) are all DONE — see
Checklist for per-section notes and known gaps.

JS/TS are curly-family (`Lang.isCurly()` covers `isJs`/`isTs` alongside
C/C++/Java/Kotlin, RDD_KEY_187): no separate `JsTokenizer`/`TsTokenizer` —
JS and TS share `TokenizerCurly`/`FormatterCurly`/`ScopePipelineCurly`/
`MiscRuleCurly` directly, gated internally on `lang.isJs`/`lang.isTs`.
Concrete JS/TS-only rule logic lands in `JsTsSpecificRule.java` +
`JsTsDeclarationAlignmentRule.java`, TS-only additions gated on `lang.isTs`.

---

## Resolved Design Decisions

Full text of each decision lives in `RDD_LOG.md` (shared with
`STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md`). See `STATE_COMMON.md`'s lookup
convention (`grep -Fm1`, no `-A`).

| Key | Topic |
|---|---|
| RDD_KEY_182 | §3/§6 destructuring-pattern LHS joins the const/let alignment grid like any ordinary declaration |
| RDD_KEY_183 | §11.1 consecutive `type X = ...` aliases form their own `=`-aligned group, same as const/let |
| RDD_KEY_187 | Class Scoping — no separate JsTokenizer/TsTokenizer etc.; shared curly classes gated internally on `isJs`/`isTs`, concrete rules in one `JsTsSpecificRule.java` |
| RDD_KEY_195 | §15 local-import classification — drop the source-root disjunct entirely; only `./`/`../`-prefixed specifiers are "local", everything else non-built-in is "third-party" |
| RDD_KEY_196 | Closing comments on modifier-prefixed methods (`async`/`static`/`get`/`set`) use the bare name only, no modifiers; object-shaped `type X = {...}` aliases get closing comments like `interface`/`class`/`enum` |
| RDD_KEY_197 | Import-ordering: trailing same-line comment travels with its import; a standalone comment segments the import list (grouped/sorted independently per segment) instead of bailing the whole pass |
| RDD_KEY_245 | (No fix landed.) First deep-dive into the `commandLineParser.ts` decl-alignment idempotency bug; ruled out the initial `spansMultipleLines` bracket-depth hypothesis, narrowed toward `enforceCallLineBreaking`/`renderCallCandidate`'s multi-line closing-bracket placement — later superseded by RDD_KEY_246's more precise locus |
| RDD_KEY_246 | (No fix landed, 2 attempts reverted.) Root cause precisely identified: `applyOversizedAggregateInitClosingBracePass` decides `}` placement from whether the aggregate initializer already contains an embedded `NEWLINE` — stale on round1 vs. present on round2. Narrow re-run (Attempt 1) fixed the symptom but exposed a same-family declaration-alignment divergence; full `ScopePipelineCurly.process` re-run (Attempt 2) fixed the repro but regressed `real_code_regressions_100.ts` and Java/Kotlin/cpp fixtures — reverted both |
| RDD_KEY_248 | Call-wrap/collapse vs. declaration-alignment/padding idempotency bug (Tier-4), 3rd session, FIXED: `ScopePipelineCurly.reapplyClosingBraceAndDeclarationsPass` re-runs just the closing-brace + declarations passes a second time (JS/TS only), with the shared trailing-gap force-reindent step skipped on that re-run (that step's re-derivation from already-reformatted text was what caused Attempt 2's regression). New fixture: `test/real_code_regressions_179_{inp,out}.ts` (the `commandLineParser.ts`-derived minimal repro) |
| RDD_KEY_249 | (No fix landed, reverted.) Investigation into `enforceCallLineBreaking`'s rejoin fits-check for `formatOffset`-style multi-candidate lines (`/tmp/mini2.ts`); a blanket statement-wide width-widening fix was tried and reverted (regressed `real_code_regressions_81`/`_93`, whose legitimately-stable over-limit rejoins the blanket check couldn't distinguish from genuinely unstable ones) |
| RDD_KEY_250 | Braceless if/else rejoin-fits-check-vs-`alignBracelessElseIfChain` pass-ordering idempotency bug, 6th session, FIXED: `FormatterCurly.format` re-runs `enforceCallLineBreaking` (twice) + `enforceComplexityPadding` right after `alignBracelessElseIfChain`, same fix shape as RDD_KEY_248. New fixture: `test/real_code_regressions_180_{inp,out}.ts` (the `formatOffset` repro) |
| RDD_KEY_263 | `utils.ts`/`lodash.js` switch-case fallthrough non-idempotency (long-deferred, see former "Known open issues" entry below), FIXED: `FormatterCurly.format` re-runs `switchRule.formatNonInlineSwitches` a second time near the end of Phase 4, after `alignInlineSwitches`'s case-grid collapse and the call-wrap passes have settled — shared `SwitchRule`/curly-family code, cross-referenced in `STATE_C_CPP_JAVA.md` |
| RDD_KEY_269 | `angular/angular` cluster 4 residue — `shared.ts`/`directive_outputs.ts`, FIXED: widened `BlockStructureRule.alignBracelessElseIfChain`'s chain-recovery to also tolerate a bare `else` re-indented one level deeper than its paired `if` (opposite direction from the pre-existing narrower-`if`-line recovery), stripping the excess back to the `if`'s own indent. New fixture: `test/real_code_regressions_184_{inp,out}.ts` |
| RDD_KEY_270 | `microsoft/TypeScript` cluster #3 — `harness/collectionsImpl.ts`, FIXED: `applyAssignmentsPass` added as a third pass inside `ScopePipelineCurly.processScope`'s existing `closingBraceAndDeclarationsOnly` narrow re-run mode (direct extension of RDD_KEY_248), after the closing-brace and declarations passes. New fixture: `test/real_code_regressions_185_{inp,out}.ts` |
| RDD_KEY_271 | `angular/angular` cluster 4 residue group #3 — `web_animations_player_spec.ts`/`input_transform.ts`, FIXED: (a) `JsTsSpecificRule.tryParseClassField` now collapses a multi-line class-field initializer's embedded NEWLINE into a soft space instead of bailing to "unrecognized member"; (b) `enforceUnionIntersectionSpacing`/`enforceTypeColonSpacing` pulled forward to run before `enforceDecoratorOverflowCascade` in `FormatterCurly.format`, so its inline-decorator-fits measurement sees the final post-spacing width. Neither touches `ScopePipelineCurly`/`closingBraceAndDeclarationsOnly` at all. New fixture: `test/real_code_regressions_186_{inp,out}.ts` |
| RDD_KEY_273 | 2026-08-09 `microsoft/TypeScript` dogfood-reconfirmation Tier-3 shape #1 (braceless if/else-if body-column-alignment padding going stale on round2) — `compiler/builder.ts`/`compiler/moduleNameResolver.ts`/`services/findAllReferences.ts`, FIXED: `BlockStructureRule.alignBracelessElseIfChain` splits on `"\n"` only, leaving a trailing `'\r'` on every line of CRLF-original source (CRLF/LF normalization happens once, at the very end, in `Main.applyLineEndings`) — that stray byte skews the `lineLengthLimit` padding guard's length math, landing on different sides of the boundary between round1 (still-CRLF) and round2 (already-LF-only). Fixed by stripping any trailing `'\r'` from each split line up front. New fixture: `test/real_code_regressions_195_{inp,out}.ts` (deliberately CRLF-encoded, `.gitattributes`-marked `-text`) |

---

## Test-Fixture Repos

- `nodejs/node` — large, real, mixed-style JS codebase (core + tooling).
- `expressjs/express` — smaller, idiomatic, widely-read real-world JS.
- `lodash/lodash` — dense functional-style JS, good stress test for
  complexity-based bracket padding (STYLE.md §3.1) on chained calls.
- `microsoft/TypeScript` — the compiler itself; canonical, heavily-typed
  real-world TS at scale; also doubles as a JS fixture.
- `angular/angular` — large, idiomatic, decorator-heavy real TS.
- `nestjs/nest` — decorator- and generic-heavy backend TS, good coverage of
  the type-annotation-alignment cases (§11, §14).
- `vuejs/core` — modern TS with heavy generics and type-level code.

## Tools/compiler used

Compiler for dogfood test `node` and `tsc` needs: see STATE_COMMON.md's
"Verifier toolchain paths" for the `node` env setup (`LD_LIBRARY_PATH`/
`NODE_PATH`/`PATH`) and why each variable is needed — canonical copy lives
there now, shared with `tools/verifiers/*.js` verification for every job,
not just this one.

**`typescript` package version gotcha:** an unpinned `npm install --prefix
~/mynpm typescript` installs **`typescript@7.0.2`** (the native tsgo
rewrite), which exports only `version`/`versionMajorMinor` — no
`createSourceFile`/`getLeadingCommentRanges`. Must pin to `typescript@5`
(landed `5.9.3`), which has the full classic API. Verify
`typeof ts.createSourceFile === 'function'` before trusting a reinstall.

---

## Dogfood Output Validation

**`js_ts_content_diff.js`** — content-preservation checker for JS/TS,
modeled on `java_content_diff.java`/`kotlin_content_diff.java` but via the
TypeScript compiler API instead of javac/PSI (same idiom as
`STATE_DATA_FORMATS.md`'s `css_content_diff.py`/`xml_content_diff.py`). One
script handles both `.js` and `.ts`. Parses original and formatted files to a
real AST and compares: top-level imports as an order-tolerant MULTISET
(`js-import-order` legitimately reorders them); every other top-level
statement/declaration in original relative order via leaf-token
canonicalization (terminal tokens joined with single spaces, whitespace
collapsed, so alignment padding/reindent are never flagged); comments as a
MULTISET, whitespace-normalized and lowercased (case-only diff is expected
`normalize-comment-start-case` behavior, not a bug). Comments recovered
separately via `ts.getLeadingCommentRanges` (TS AST doesn't attach them as
tree nodes), scanned at every node's `getFullStart()` plus position 0/EOF,
deduplicated by `[pos, end)`.

Exit 0 if content is preserved, 1 with a description of each mismatch
otherwise, 2 on usage error. No build step — plain `.js`, run directly:

```bash
node tools/verifiers/js_ts_content_diff.js <original.(js|ts)> <formatted.(js|ts)>
```
(needs the same `LD_LIBRARY_PATH`/`NODE_PATH`/`PATH` exports as above). 208
lines. Verified against hand-crafted pairs for both extensions (good pair:
import-sort+reindent+comment recapitalization passes clean; bad pairs:
dropped statement and corrupted comment both correctly flagged) — all 6
cases (3 pairs × 2 extensions) behaved as expected.

**Tolerances added post-`lodash/lodash` dogfood** (false-positive classes
found there):
1. `normalize-comment-end-period` — strips one trailing `.` before
   comparison (mirrors `MiscRuleCore.stripSoleTrailingPeriod`).
2. Single-statement block unwrapping (STYLE.md §10) — `canonicalize`
   recurses directly into a `Block`'s one statement when
   `statements.length === 1`, so `if (x) foo();` vs `if (x) { foo(); }`
   canonicalize identically. Blocks with 0 or 2+ statements walked normally.
3. JSDoc-as-AST-child double-counting — TS parses `/** ... */` as a real
   `ts.isJSDoc` AST child even with no `@` tags; `canonicalize`'s walk now
   skips these nodes (already covered separately by `collectComments`).

Re-verified against all hand-crafted pairs plus new pairs per tolerance —
all pass, both `.js` and `.ts`.

---

## Test Fixtures (Local)

Local dogfood pairs (distinct from the external-repo list above, which is
for corpus-scale validation) live in `formatter/test/` — see
`test/README.txt` for the pair list and what each covers. Pairs are split by
extension (`.js` vs. `.ts`), not shared, since TS-only constructs
(decorators, enums, generics, interfaces) can't live in a valid `.js` file.

`js_combined`/`js_comments`/`ts_combined`/`ts_comments` plus all other local
JS/TS fixtures (`ts_decl_grid_ext`, `js_getter_setter_asi`,
`js_import_ordering_comments`, `js_nested_template_literal`, etc.) are all
active in the Makefile and passing.

---

## Open Questions

- **HTML5 needs its own dispatcher for `<style>`/other embedded formats
  beyond `<script>`.** `<script>` splicing (JS/TS dispatch, CDATA unwrap/
  rewrap, Config-threading) is done — see `XmlSpecificRule.
  renderScriptOrStyle`. Any further HTML5/embedded-format dispatcher work
  belongs to the Data Formats job (`STATE_DATA_FORMATS.md`), not this one.

- **JSX/TSX out-of-scope statement** — see Scope section above (exact
  wording preserved there per policy: out of scope entirely, not deferred).

  **2026-08-07 discussion session (no code, no fixtures, no RDD_LOG key —
  interactive discussion only, findings recorded for a future implementation
  session):** user proposed a 3-step staged approach as a simpler
  alternative to a full embedding-aware dispatcher: (1) tokenizer marks a
  whole JSX tree as one opaque `IDENTIFIER` token, throws on tag imbalance;
  (2) `{...}` expression holes inside JSX become `__JSn__` placeholders,
  outer markup handed to the existing HTML formatter; (3) each placeholder's
  content sent through the JS/TS formatter as an independent small program.
  Assessed in depth, **not adopted as a design**:
  - Step 1's flaw: packing the discovered JSX span into a plain
    `IDENTIFIER` token doesn't avoid the hard problem (finding tag
    boundaries against `<`'s three-way ambiguity with less-than/generics,
    recursively through nested `{}` holes) — it still requires that walk,
    then discards the structure. Reusing `IDENTIFIER` as a multi-line
    opaque-blob vehicle would also hit every downstream pass that assumes
    realistic single-token width (declaration/class-field alignment grids,
    `enforceCallLineBreaking`'s `candidateLen` checks — the same width/
    pass-ordering fragility class as RDD_KEY_248/249/250). A dedicated
    opaque/frozen token kind would be needed instead.
  - Step 2's HTML-formatter-reuse instinct is sound for splice mechanics
    (real precedent: `XmlSpecificRule.renderScriptOrStyle`), but JSX's
    grammar diverges from real HTML5 in load-bearing ways the existing
    HTML5 tree-construction pass isn't verified to tolerate: arbitrary
    self-closing tags, case-sensitive component names (`<MyComponent>`),
    fragments (`<>...</>`), and expression-valued attributes
    (`onClick={handler}`, a 4th embedding site not covered by child-content
    substitution).
  - Step 3 is the most underspecified: the proposal's own example
    (`items.map(x => <li>{x}</li>)`) shows a `{...}` hole containing more
    JSX with its own `{...}` holes — unbounded-depth recursion, not the
    single flat step described. Written correctly it becomes the same
    embedding-aware dispatcher this scope note already says is needed.
  - **How real JSX parsers actually solve the `<` ambiguity, and why it
    doesn't port directly:** they switch lexer modes based on grammar
    position (a parser always knows when it's at expression-start, where
    `<` is unambiguously JSX-open), not lookahead heuristics. This codebase
    has no grammar-position-aware parser (flat tokenizer + local-lookback
    passes), so this can't be inherited for free — `<`/`>` disambiguation
    for generics alone already needed a dedicated mechanism
    (`TokenizerCurly.reclassifyAngleBrackets`/`isGenericSafeToken`).
    **Portable idea identified:** run boundary-finding as its own dedicated
    pre-pass checking for `<` at a short enumerable list of expression-start
    token-adjacency contexts (after `return`, arrow body, ternary branches,
    call-argument start, etc.), recursing into `{}` holes — positional
    rather than lexical disambiguation, without requiring a real AST.
  - **Verifier tooling:** `@babel/parser` was proposed for a JSX-aware
    `js_ts_content_diff.js`; recommended against — stick with the
    already-in-use TS compiler API (`ts.createSourceFile` with
    `ts.ScriptKind.TSX`/`.JSX`), avoiding a second parser dependency and the
    same npm-pin gotcha already hit for `typescript` itself.

  **Not yet designed, left for a future session:** the concrete enumerable
  list of expression-start contexts for the boundary-finding pre-pass, and
  the concrete opaque-span token representation. Nothing here supersedes
  the Scope section's "out of scope entirely" statement.

---

## Related investigation history — same architectural family as bugs #2/#3

Bugs #2 and #3 below belong to a broader, repeatedly-investigated
architectural family: **"which pass gets to see the final, stable per-line/
per-column width."** Several earlier sessions investigated *other* instances
of this family (not bugs #2/#3 themselves) and, even where no fix landed,
produced ruled-out hypotheses, precise code loci, and debugging methodology
that directly transfer to finishing #2/#3. Kept here in more detail than the
RDD table alone for that reason — do not re-derive these from scratch.

- **RDD_KEY_245 (no fix landed).** First deep-dive into the
  `commandLineParser.ts` declaration-alignment idempotency bug (the repro
  that `RDD_KEY_248` eventually fixed for `applyDeclarationsPass`, and whose
  sibling in `applyAssignmentsPass` is bug #2 below). Ruled out
  `JsTsDeclarationAlignmentRule.spansMultipleLines`'s bracket-depth bail
  condition as the cause (independently re-tested again in RDD_KEY_246,
  still negative). Tentatively located the divergence in
  `MiscRuleCurly.enforceCallLineBreaking`/`renderCallCandidate`'s multi-line
  closing-bracket placement (~line 1250-1330) — this locus was later shown
  by RDD_KEY_246 to be structurally impossible (that method's replaced span
  never extends past the call's own closing `)`), so **do not re-suspect
  `renderCallCandidate` itself** for this family; the real loci found since
  are `applyOversizedAggregateInitClosingBracePass` (RDD_KEY_246/248) and
  `applyAssignmentsPass` (bug #2).

- **RDD_KEY_246 (no fix landed, 2 attempts reverted).** Precisely located
  the `commandLineParser.ts` root cause: `ScopePipelineCurly.
  applyOversizedAggregateInitClosingBracePass` (called once, early, inside
  `processScope`, before Phase 1's `enforceCallLineBreaking`) decides
  whether to move a dangling `}` onto its own line by checking whether the
  aggregate initializer's `{...}` *already* contains an embedded `NEWLINE`
  — false on a fresh round1 (the nested call hasn't been wrapped yet), true
  on round2 (previous round's call-wrap newline already present) — a
  "pass's decision depends on a later pass's not-yet-produced newline" bug,
  same family as `enforceComplexityPadding`/
  `enforceAttributeAndSpliceBracketPadding`/`enforceInitializerBraceSpacing`.
  **Attempt 1 (reverted):** re-run only
  `applyOversizedAggregateInitClosingBracePass` a second time, right after
  the first `enforceCallLineBreaking` call. Fixed the named `}`-placement
  symptom, but exposed that `JsTsDeclarationAlignmentRule.
  spansMultipleLines`/`parseDeclaration`'s grouping decision for the same
  row is *also* made against the stale pre-call-wrap shape — a second,
  sibling manifestation of the same bug in declaration-alignment padding,
  not just brace placement (this is exactly the same shape bug #2 exhibits
  in `applyAssignmentsPass` — a third sibling). Reverted rather than land a
  partial fix.
  **Attempt 2 (reverted):** replaced the narrow fixup with a second, full
  `ScopePipelineCurly.process(text)` re-run in `FormatterCurly.format`'s
  Phase 1, so every per-scope pass re-derives its decision against the
  post-call-wrap shape. Fully fixed the repro (`diff round1 round2` empty)
  but caused **real forward-pass regressions** on unrelated fixtures:
  `real_code_regressions_100.ts` collapsed an already-correct `} //
  interface ParserOptions` (closing brace + trailing comment) back onto one
  line; several Java/Kotlin/`cpp26` fixtures also failed. **Lesson (directly
  applicable to any re-run-based fix for bug #2):** re-running the whole
  five-pass `processScope` sequence unconditionally is not safe — some pass
  treats a second same-round invocation as "already finalized, re-collapse/
  re-merge it," specifically for trailing-comment-after-`}` shapes. This is
  why `RDD_KEY_248`'s eventual fix (below) re-ran only two of the five
  passes, and why bug #2's candidate fix proposes adding a *third* narrowly-
  scoped pass to that same limited re-run rather than a full re-run.
  **Testing pitfall noted this session, applies to any future large-suite
  validation (including bug #2/#3 work):** a single very long `make
  _test_serial JAR_FILE=...` invocation's terminal output can be silently
  truncated by the calling tool with no visible marker, hiding real `FAIL`
  lines among hundreds of `PASS` lines — always redirect `make test`/`make
  _test_serial` output to a log file and `grep -n "^FAIL"` it directly
  rather than trusting a live/streamed terminal capture for a suite this
  size.

- **RDD_KEY_248 (FIXED — the landed fix bug #2's candidate extends).**
  Landed `RDD_KEY_246`'s untried "narrower middle ground": re-run only
  `applyOversizedAggregateInitClosingBracePass` + `applyDeclarationsPass`
  (closing-brace first) a second time, via
  `ScopePipelineCurly.reapplyClosingBraceAndDeclarationsPass`. **First cut
  reproduced Attempt 2's exact `real_code_regressions_100.ts` regression
  again** — traced to `processScope`'s *shared* trailing-gap force-reindent
  step (right after the span-recursion loop), which on this narrower re-run
  re-derives indentation from the round's ALREADY-reformatted physical text
  (other Phase 1 passes' blank-line-insertion/Allman-conversion already
  baked in), a different shape than what `findParentIndent` saw during the
  original `process()` call — silently flipping an already-correct
  closing-brace indent. **Fix:** skip that force-reindent step entirely
  when the narrow-rerun mode is active. This is the exact trap bug #2's
  candidate fix must also avoid if it adds a third pass to the same
  narrow-rerun mode.

- **RDD_KEY_249 (no fix landed, reverted) — investigates the sibling
  `enforceCallLineBreaking` rejoin-fits-check family, adjacent to but
  distinct from bugs #1/#2/#3.** Repro `/tmp/mini2.ts`: two `padNumber(...)`
  calls on one statement; round1's rejoin fits-check measures each
  candidate's own *local physical line* in isolation (`lineStartIndex`/
  `effectiveLineEndIndex`, which only look back/forward to the nearest
  `NEWLINE`) and both silently rejoin, producing an already-over-limit
  145-char line; round2 (fed that one-liner) measures the first candidate
  differently and only partially rejoins — an inconsistent partial rejoin.
  **Attempt (reverted):** widen the rejoin fits-check from physical-line-wide
  to statement-wide (`logicalStatementStart`/`logicalStatementEnd`, scanning
  to the nearest `;`/`{`/`}`/file bound). This **fully fixed `/tmp/mini2.ts`**
  but **regressed `real_code_regressions_81`/`_93`**, whose accepted,
  genuinely-idempotent output is a combined line already over
  `lineLengthLimit` that must still rejoin (the correct, stable behavior for
  those two fixtures is "over limit is fine, rejoin anyway, consistently").
  A blanket statement-wide over/under-limit gate cannot distinguish
  `_81`/`_93`'s legitimately-stable over-limit rejoins from `mini2.ts`'s
  genuinely-unstable ones — **this is the same trap that later sank the
  first `hasBreakableCall` width-gate attempt** for angular cluster 4 root
  cause #3, confirmed independently from the `enforceCallLineBreaking` side
  of the pipeline. **Next-session pointer that was later tested and
  refuted (RDD_KEY_249's own hypothesis, see below):** instrument whether
  `_81`/`_93`'s sibling candidates sit on their own original physical
  source line (pre-wrap) while `mini2.ts`'s do not — a fix scoped to "only
  widen measurement when the local line was itself produced by a still-open
  sibling wrap" was proposed as narrower than the blanket variant.
  **Refuted the following session:** instrumentation showed
  `localLineHasNewline` was `true` in *all four* runs (`mini2.ts` both
  rounds, `_81`/`_93` both rounds) — sibling-wrap-visibility does not
  distinguish the stable cases from the unstable one; both groups already
  answer "yes" under the original check. **Actual mechanism for
  `mini2.ts` (found via the same instrumentation, precise, and directly
  informed bug #1's methodology):** the divergence is a pass-ordering gap
  against `BlockStructureRule.alignBracelessElseIfChain` (~line 361), which
  runs *after* both `enforceCallLineBreaking` calls (~lines 247, 286) and
  pads a short `else` keyword to column-align with its paired `if`'s body.
  Round1's rejoin-check measures the `else` prefix *before* that padding is
  applied (13 chars narrower than the eventual final width), so a candidate
  that should stay wrapped instead fits and rejoins — an internal
  round1 self-violation (`candidateLen=92 <= limit=100` at check time, but
  the final padded line is 145 chars). Round2 (fed the already-padded
  one-liner) measures the real, wider prefix and gets a different verdict.
  `_81`/`_93` are unrelated to this mechanism entirely (no braceless-else
  chain in either fixture) — their stability comes from no later pass ever
  changing their prefix width after the fits-check runs. **This refutation
  and the `alignBracelessElseIfChain`-ordering mechanism is what RDD_KEY_250
  (below) fixed** — but note it is a *different* `alignBracelessElseIfChain`
  interaction than bug #1's (RDD_KEY_250 is about the rejoin fits-check
  running *before* the padding pass; bug #1 is about an *unidentified
  earlier indent pass* reacting to the padding pass's *leftover artifact* on
  a second round — same method, two distinct interactions with it).

- **RDD_KEY_250 (FIXED).** Landed the fix RDD_KEY_249's refutation pointed
  at: right after `alignBracelessElseIfChain` in `FormatterCurly.format`,
  re-run `enforceCallLineBreaking` twice + one `enforceComplexityPadding` —
  same fix shape as `RDD_KEY_248`, not a reorder of
  `alignBracelessElseIfChain` itself (which stays last on purpose, per its
  own comment, so its padding decision sees every earlier pass's final
  settled width). Verified a single re-run call was insuffient to reach a
  fixed point (only the first candidate re-wrapped) — the two-call-plus-
  repad shape was needed. `real_code_regressions_81`/`_93` re-verified
  individually still stable (neither has a braceless-else chain, so the new
  re-run block is a no-op for them) — confirms this fix is disjoint from
  what broke them in RDD_KEY_249's attempt.

---

## Active work — all 3 originally-tracked bugs now FIXED (`processScope`/
declaration-alignment/call-wrap-ordering family — see RDD_KEY_269/
RDD_KEY_270/RDD_KEY_271 and the "FIXED" notes under their own headings)

No open bug remains from this investigation as of RDD_KEY_271. Investigation
history is kept below in fuller detail since its debugging methodology and
the precise `RDD_KEY_246` gate reasoning directly transferred across all
three bugs (#3 in particular reused #1/#2's "verify the shared
trailing-gap force-reindent gate is untouched, don't just trust green
tests" discipline). See "Related investigation history" immediately above
for the broader family context, ruled-out hypotheses, and reusable
debugging loci. Full session-by-session narrative (including every dead
end not captured above) lives in `git log` for this file — not re-derived
here per this file's top-of-file convention.

### 1. `angular/angular` cluster 4 residue — `shared.ts`/`directive_outputs.ts` (FIXED, RDD_KEY_269)

`packages/core/src/render3/instructions/shared.ts:793-796` and
`packages/core/src/render3/view/directive_outputs.ts` (same code shape,
evidently copy-pasted between the two files) — a distinct, newly root-caused
bug in `BlockStructureRule.alignBracelessElseIfChain` (braceless if/else
chain alignment), confirmed via debug instrumentation to be **NOT** the
`processScope` double-pass family (#2/#3 below).

**Repro:** a bare `if(cond) stmt; else stmt;` with no wrappable call (both
branches short enough to fit). Round1: `collapseSingleExpressionBlocks`
strips braces (not gated by `refuseUnrescuableCollapse`, since both branches
fit) and `alignBracelessElseIfChain` column-aligns the `if`/`else` bodies,
producing e.g. `  if(...) hostIndex = data;` / `  else        [hostIndex,
...] = data;` (both indented 2sp, `else` padded to align). Round2
(reformatting that output): produces `  if(...) hostIndex = data;` /
`    else [hostIndex, ...] = data;` — `else` re-indented to 4sp instead of
2, and left unaligned.

**Root cause (confirmed via debug prints in `alignBracelessElseIfChain`,
since removed):** the pass detects an `if`/bare-`else` chain by requiring
the two lines' raw leading-whitespace lengths to match exactly (`jIndent !=
indentLen`), with one narrow recovery case for "the `if` line itself was
left-padded wider by a previous round" (`jIndent < indentLen`). On round1:
`if` `indentLen=2`, `else` `jIndent=2` — match, chain detected, alignment
applied. On round2 (round1's output as input): `if` still `indentLen=2`,
but `else` now measures `jIndent=4` — **some earlier pass in the pipeline
(not individually identified — not `collapseSingleExpressionBlocks`, a
no-op here since no braces remain, and not `alignBracelessElseIfChain`
itself) has already re-indented the standalone `else` line one level deeper
than its paired `if`, before `alignBracelessElseIfChain` sees it.** Since
`jIndent(4) > indentLen(2)`, the one existing recovery case (which only
handles the `if` line being padded wider) doesn't apply, the chain-scan
breaks at size 1, and the file falls through with no alignment. Most likely
a generic structural/statement indent-fixup pass earlier in
`FormatterCurly.format`'s Phase 0/1 that treats an already-braceless
standalone `else` (no adjacent brace pair) as an orphaned continuation
statement one level deeper than its paired `if`, rather than recognizing it
as a chain member — the determination differs depending on whether the
`else` line's physical shape already carries a previous round's
`alignBracelessElseIfChain` column-padding artifact.

**Is this the `processScope` double-pass mechanism?** No — confirmed via
evidence, not assumed. The `processScope` family's signature is a
declaration/assignment/signature *grouping* decision (column widths, which
rows share an alignment group) computed twice over overlapping ranges
within one `format()` call. This bug is a single *indentation* value for
one statement line disagreeing between rounds because an EARLIER pass's
indent computation reacts to the PRESENCE of a LATER pass's own
previous-round artifact — confirmed to occur before
`alignBracelessElseIfChain` runs, and that method is single-pass,
single-invocation (no outer/inner recursion). Closer in shape to the
`enforceComplexityPadding`/`enforceAttributeAndSpliceBracketPadding`/
`enforceInitializerBraceSpacing` precedent family ("a pass's decision
depends on a later pass's not-yet-produced or already-produced artifact")
— a genuinely distinct, fifth instance of that broader pattern.

**Fix landed (RDD_KEY_269):** widened `alignBracelessElseIfChain`'s
chain-recovery case to also tolerate `jIndent > indentLen` for a bare-`else`
member specifically (chain still size 1 — an `if`'s own bare else only,
never an `else if` member, which has no known/expected deeper-indent
shape), mirroring the existing `jIndent < indentLen` recovery for the `if`
line but in the opposite direction: strip the excess indentation off the
`else` line back down to the `if`'s own (already-canonical) indent instead
of re-anchoring `indentLen` itself. The earlier indent-fixup pass
responsible for the original deepening was never individually identified —
this fix works around its artifact at the point `alignBracelessElseIfChain`
observes it, rather than fixing that pass directly (the "more robustly
identify and fix the earlier pass" alternative floated below was not
pursued, given the narrower fix's clean validation result).

**Validation:** both cluster 4 files individually confirmed idempotent
(round1==round2) post-fix. Full `angular/angular` corpus (5394 `.ts` files,
reused `/tmp/angular` checkout) round1-vs-round2 idempotency-violation count
went from 9 (pre-fix) to 7 (post-fix) — the 2 newly-idempotent files are
exactly `shared.ts`/`directive_outputs.ts`; the remaining 7 (separate
`processScope`-family root cause, #2/#3 below) unchanged in identity and
count, i.e. **zero new regressions anywhere in the corpus**. (A raw
full-corpus round1-output byte diff between a pre-fix and post-fix jar
showed 178 differing files, but every one of those turned out to be
baseline contamination — the pre-fix comparison jar was built from a
separate source-tree copy lacking the real project's synced GRU
comment-classifier weights, producing spurious comment-capitalization
diffs unrelated to this fix; ruled out via (a) none of the 3
diffs-containing-the-word-"else" actually touching if/else alignment, (b)
the same pre-fix jar run twice on the full corpus producing byte-identical
output, and (c) the within-jar idempotency comparison above being immune to
any cross-jar weights-path discrepancy and giving the clean, decisive
signal instead.) `make test`: 258/258 → 259/259 forward + idempotency, zero
fixture regressions. New fixture: `test/real_code_regressions_184_{inp,out}.ts`.
Kotlin re-verified only via full `make test` (shared
`BlockStructureRule`/`KotlinSpecificRule.alignBracelessElseIfChain` code
path, no Kotlin-specific fixture added, no Kotlin real-code corpus re-run
performed this session — left as a future item if a similar bare-else
deeper-indent shape is ever found in Kotlin real code).

Status: **FIXED.**

### 2. `microsoft/TypeScript` cluster #3 — `harness/collectionsImpl.ts` (FIXED, RDD_KEY_270)

`ScopePipelineCurly.applyAssignmentsPass` (bare-assignment-statement
alignment, STYLE.md §6) needs the same re-run treatment `RDD_KEY_248`
already gave `applyDeclarationsPass`. Candidate fix identified but low
confidence — `RDD_KEY_246`'s two prior attempts in this exact family
(narrow re-run; full `processScope` re-run) both looked equally safe and
regressed via a shared `processScope` trailing-gap force-reindent step.
**MEDIUM-HIGH risk.**

**Finding:** `harness/collectionsImpl.ts` diffs at line 276
(`this._size = -1;`, wide-padded in round1, unpadded in round2) — same
"call-wrap vs. alignment-padding decided too early" shape as the
already-fixed `RDD_KEY_248` cause, but in `applyAssignmentsPass`, a code
path `RDD_KEY_248`'s own javadoc on the `closingBraceAndDeclarationsOnly`
re-run mode explicitly says was "not shown to depend on the post-call-wrap
shape" and therefore deliberately left out of the re-run — **this
investigation disproves that assumption for at least this one shape.**
Source (`src/harness/collectionsImpl.ts:272`):
`this._map[Metadata._escapeKey(key)] = value === undefined ?
Metadata._undefinedValue : value;` followed by `this._size = -1;` — round1
wraps the long line's `[...]` subscript (`enforceCallLineBreaking`, runs
after `processScope`), but `applyAssignmentsPass` (part of `processScope`,
runs *before* that wrap) already baked in `this._size`'s padding relative
to the map statement's full single-line LHS width. Confirmed via temporary
debug print (added/removed, no net diff) dumping each `Assignment` group's
members: **round1** groups the map assignment (lhsText len 35) with
`this._size` (len 10) — the wide member pads the narrow one. **round2**
(round1's own multi-line-subscript output) shows the map row **absent from
that group** — `this._size` groups only with an unrelated shorter
assignment, gets little/no padding. *Why* the row drops out of the group on
round2 specifically was **not pinned down this session** (budget spent
confirming the group-membership divergence itself, not tracing into
`parseAssignment`/`groupAssignments`/the two earlier `processScope` passes
that run first).

**Same family as #1/the RDD_KEY_248 finding, not a new third cause:** a
`processScope`-phase pass's decision (here, assignment-group membership/
padding) is computed once, before `enforceCallLineBreaking` (a later Phase
1 pass) finishes changing the column widths it depends on — structurally
identical to what `RDD_KEY_248` already fixed for `applyDeclarationsPass`,
just in a different one of the five `processScope` passes.

**Candidate fix (NOT ATTEMPTED — root-cause-only investigation; do not
implement without a fresh session's real-corpus validation):** direct
extension of `RDD_KEY_248`'s re-run — add `applyAssignmentsPass` as a third
pass inside `processScope`'s existing `closingBraceAndDeclarationsOnly`
mode (rename the flag if landed), run after the closing-brace and
declarations passes, same "closing-brace first" ordering rationale.
**Risk/blast radius:** narrowly scoped to the already-existing JS/TS-gated
re-run path in `FormatterCurly.format` (per `RDD_KEY_246`/`RDD_KEY_248`,
this second call site is JS/TS-only) — would not touch C/C++/Java/Kotlin's
single-pass behavior. **However, `RDD_KEY_246`'s own history is a direct
warning against assuming this is safe by inspection**: its Attempt 1
(re-running only the two originally-scoped passes) looked narrow and still
caused a real regression (`real_code_regressions_100.ts`'s `} // interface
ParserOptions` losing its indent) via the *shared* trailing-gap
force-reindent step inside `processScope` that both re-run modes pass
through — `RDD_KEY_248`'s eventual fix needed an extra gate to skip that
step entirely. Re-running `applyAssignmentsPass` a second time carries an
analogous unknown risk: an already-correctly-padded assignment group could
be re-parsed from its own already-padded text on the second re-run, and
`parseAssignment`/`renderTokens` were not audited this session for safety
under double-invocation (e.g. whether padding spaces before `=` could be
mistaken for part of a multi-token LHS). **Confidence this fixes the bug
without regression: LOW without a fresh session's full fixture-suite +
dogfood-corpus validation** — the direction is a well-precedented narrow
extension of an already-landed pattern, but every prior attempt in this
exact family underestimated a shared-step interaction on the first try.

**Fix landed (RDD_KEY_270):** a later session implemented the candidate fix
essentially as described above — `applyAssignmentsPass` added as a third
pass inside `processScope`'s `closingBraceAndDeclarationsOnly` re-run mode,
after `applyOversizedAggregateInitClosingBracePass` + `applyDeclarationsPass`
(closing-brace-first order preserved). The `RDD_KEY_246` failure mode (a
regression via the *shared* trailing-gap force-reindent step) was ruled out
by inspection rather than assumed safe: that step is skipped by a single
`if(closingBraceAndDeclarationsOnly)` boolean gate that is not tied to which
specific passes run inside the branch, so adding a third pass to the branch
does not touch that gate at all. Repro obtained via a direct `curl` fetch of
`harness/collectionsImpl.ts` from `raw.githubusercontent.com` (no full/
sparse clone — this system's old git rejects `--filter=blob:none
--sparse`, and the prior full checkout used for `RDD_KEY_248`'s corpus
validation was gone; a fresh full clone was explicitly ruled out for that
session). Validated via A/B rebuild (revert fix / rebuild / reproduce exact
symptom; restore fix / rebuild / confirm idempotent) rather than corpus
dogfood — `make test` 259/259 → 260/260 forward + idempotency, zero
regressions. New fixture `test/real_code_regressions_185_{inp,out}.ts` (a
`Metadata.set`-derived minimal repro). Full narrative: `RDD_KEY_270` in
`RDD_LOG.md`.

**Known follow-up not investigated:** `js_ts_content_diff.js` flags a
MISMATCH on `harness/collectionsImpl.ts`'s top-level statements #1/#2
(the `interface`/`class` headers) — confirmed via the same A/B rebuild to be
a pre-existing, unrelated content-diff-tool artifact present identically
both before and after this fix, not caused by it and not investigated
further (out of this bug's scope; a candidate future item for the checker
itself, same spirit as the `lodash/lodash` tolerance list above).

Status: **FIXED.**

### 3. `angular/angular` cluster 4 residue group #3 — `web_animations_player_spec.ts`, `input_transform.ts` (FIXED, RDD_KEY_271)

Same broad `processScope`/declaration-alignment/call-wrap family as #1/#2
above, but root-caused to **two distinct mechanisms**, both entirely
outside `ScopePipelineCurly.processScope`/`closingBraceAndDeclarationsOnly`
(unlike #2's `applyAssignmentsPass` fix) — confirmed lower-risk than #2 by
inspection (`grep` for `ScopePipelineCurly`/`closingBraceAndDeclarationsOnly`
in the eventual diff: zero hits), not merely assumed safe.

- `web_animations_player_spec.ts:177-193` — a class-field declaration-
  alignment grid (`effect : AnimationEffect | null = null;`, `finished:
  Promise<Animation> = ...`, etc.) where round1 and round2 disagreed on
  which rows are grouped and how wide the `:`/`=` columns are padded.
  **Root cause:** `JsTsSpecificRule.tryParseClassField`'s initializer-value
  scan bailed to `null` ("unrecognized field") on any embedded NEWLINE.
  round1 parses the field from original single-line source, grid-pads it,
  and a later Phase-4 `enforceCallLineBreaking` call wraps the now-overlong
  padded initializer (e.g. `Promise.resolve({} as any)`) across lines;
  round2 (fed that wrapped output) hits the NEWLINE bail and misclassifies
  the field, splitting/re-widening its group differently — non-idempotent.
  **Fix:** collapse the multi-line initializer's NEWLINE (+ continuation
  indentation) into a single soft space while scanning, instead of bailing
  — the field parses identically (as its logical single-line text) in both
  rounds, and the same downstream call-wrap pass re-wraps it the same way
  every round. Only the value-scan loop was touched; the sibling
  multi-line-*type*-expression bail a few lines above was left alone
  (undemonstrated as broken by this repro).
- `input_transform.ts:16-19` — a decorator-call-plus-declaration line
  (`@Input( {...} ) inlineFunctionInput: any;`) that fit under
  `lineLengthLimit` on round1 but got wrapped onto two lines on round2.
  **Root cause:** `enforceDecoratorOverflowCascade` (the first width-driven
  pass in `FormatterCurly.format`'s Phase 1) measures the whole inline
  decorator+target line's fit *before* `enforceUnionIntersectionSpacing`/
  `enforceTypeColonSpacing` (ordinarily Phase 4) widen e.g. `string|number`
  to `string | number` inside the decorator's own argument list — a line 2
  chars under the limit pre-spacing measures as "fits" and stays inline,
  then grows past the limit once that spacing lands later with no further
  re-check (a self-violating round1); round2 measures the already-widened
  text and drops the target to its own line. **Fix:** pulled
  `enforceUnionIntersectionSpacing`/`enforceTypeColonSpacing` (TS-only)
  forward to run immediately before `enforceDecoratorOverflowCascade` — same
  fix shape as the file's existing `enforceComplexityPadding`/
  `enforceAttributeAndSpliceBracketPadding`/`enforceInitializerBraceSpacing`
  pulled-forward precedent, just anchored one pass earlier since
  `enforceDecoratorOverflowCascade` runs before `enforceCallLineBreaking`,
  not after. The original Phase 4 calls of both passes stay in place too
  (idempotent no-ops on anything this earlier call already normalized).

**Repro:** direct `curl` fetch of both files from
`raw.githubusercontent.com/angular/angular/5ad8231/...` (paths resolved via
the GitHub REST `git/trees?recursive=1` API against the same pinned commit
— the bare basenames collided with a stale guessed path). No full/sparse
clone (prior `/tmp/angular` checkout gone; this system's old git rejects
`--filter=blob:none --sparse`, same finding as RDD_KEY_270).

**Validation:** A/B rebuild (revert both fixes via `git checkout` on the two
touched files / rebuild / reproduce both symptoms byte-for-byte on both real
files; restore / rebuild / confirm both files fully idempotent, `round1 ==
round2`, and `js_ts_syntax_check.sh` clean on both). `make test`: 260/260 →
261/261 forward + idempotency, zero regressions. New fixture
`test/real_code_regressions_186_{inp,out}.ts` (a minimized `Sample`/`Widget`
class pair combining both bugs in one file; A/B-confirmed to reproduce both
pre-fix and stay idempotent post-fix). No fresh full-corpus `angular/angular`
dogfood re-run performed (checkout unavailable, full re-clone out of scope
per this session's instructions) — validation scope is the two real repro
files plus the full existing fixture suite, same precedent as RDD_KEY_270.

Status: **FIXED.**

---

## Checklist

### Status by style-doc section

All items below are implemented in `JsTsSpecificRule.java` unless noted, and
wired into `FormatterCurly`'s phase pipeline (Phase 1 structural/brace,
Phase 4 flat spacing, Phase 5 import ordering).

- **§1 Baseline-inherited rules** — DONE.
- **Tokenizer support** — DONE (`TokenizerCurly.java`: `KEYWORDS_JS`/`_TS`,
  `NAMED_CONSTRUCT_JS`/`_TS`, `=>`/`??=`/`??`, `emitTemplateLiteral()`).
- **§2 Semicolon insertion** — DONE (`enforceSemicolonInsertion`).
- **§3 Destructuring/spread** — DONE (`enforceSpreadRestSpacing` +
  declaration-grid join, RDD_KEY_182).
- **§4 Template literals** — DONE (`enforceTemplateLiteralInterpolationSpacing`,
  recursive into nested backtick literals inside a `${...}` interior).
- **§5 Function/method Allman brace style** — DONE.
- **§6 Arrow functions** — DONE (`enforceArrowSpacing`,
  `enforceArrowFunctionParameterParens`).
- **§7 Optional chaining / nullish coalescing** — DONE.
- **§8 Getter/setter accessors** — DONE, reuses `GetterSetterRuleCurly`.
  **Known gap:** a plain method with no return-type token (e.g.
  `isValid() {...}`) cannot join the same aligned group as `get`/`set`
  siblings (`mergeReturnTypeIntoCall` would need a redesign) — left
  ungrouped (correct, just unaligned), not attempted.
- **§9 Decorators** — DONE (`enforceDecoratorTightAtSpacing`,
  `enforceDecoratorOverflowCascade`).
- **§10 `async`/`await` spacing** — DONE, free (required syntax spacing).
- **§11 Type annotations** — DONE as flat passes (`enforceTypeColonSpacing`,
  `enforceUnionIntersectionSpacing`, `reorderClassFieldModifiers`,
  `enforceClassFieldAlignmentGrid` for §11.2, `enforceUnionTypeContinuationIndent`
  for §11.1 multi-line union/intersection `type` alias continuation indent).
- **§12 Enums** — DONE (`enforceEnumMemberFormatting`).
- **§13 Generics (`<T>`)** — DONE. Bracket-complexity detection reused from
  C++/Java (`TokenizerCurly.reclassifyAngleBrackets`/`isGenericSafeToken`,
  extended repeatedly with TS-specific keywords, see dogfood sections
  below). Comma-spacing inside generic argument lists:
  `enforceGenericArgumentCommaSpacing`.
- **§14 Interface / object-shaped `type`-alias member `:` alignment** —
  DONE (`enforceInterfaceTypeAliasMemberColonAlignment`, RDD_KEY_196).
- **§15 Import ordering** — DONE (`enforceImportOrdering`, RDD_KEY_195,
  RDD_KEY_197).
- **Declaration-alignment grid (`let`/`const`/`var`/`type`)** — DONE,
  including destructuring-pattern LHS (RDD_KEY_182) and `type X = ...` alias
  groups (RDD_KEY_183) via `JsTsDeclarationAlignmentRule`. Multi-declarator
  statements (`let a = 1, b = 2;`) deliberately stay unaligned — matches
  C++/Java's own existing behavior for the same shape, confirmed not a gap.
- **Single-declarator colon spacing** — **FIXED 2026-08-04.** `const x:
  number = 1;` was rendering as `const x : number = 1;` whenever the
  declaration had no alignment-group neighbors, because
  `JsTsDeclarationAlignmentRule.renderAlignedGroup` always put `: type` in
  its own `ColumnGrid` cell, and `ColumnGrid.flush()` joins adjacent cells
  with a space even for a singleton "group." Fix: for `group.size() == 1`,
  merge name and `: type` into one cell (no separate type-column) so the
  join space lands after the identifier as normal TS spacing. Real
  (`size() > 1`) alignment groups are untouched (their space-before-`:` is
  the deliberate STYLE_JS_TS.md §11.2 look). Fixture
  `test/real_code_regressions_177_{inp,out}.ts`; a pre-existing fixture
  (`real_code_regressions_107_out.ts`) updated to match the corrected
  singleton spacing. `make test`: 239/239.
- **Real-code testing** — DONE for `expressjs/express`, `nestjs/nest`,
  `vuejs/core`, `lodash/lodash`, `angular/angular` (categorized), and
  `microsoft/TypeScript` (categorized). See dogfood sections below.

### Fixed implementation-gap bugs (settled, historical)

Compressed record of bugs found/fixed while activating local fixtures and
during the `expressjs/express`/`nestjs/nest` dogfood passes. Full narrative
in `git log`; conclusions only below.

**js_combined/js_comments activation:** comment inside a destructuring
pattern dropped on reformat (fixed: scan raw tokens, bail multi-line on
interior comment); ASI-vs-alignment-grid phase ordering (fixed: reordered
so ASI runs first); array-destructuring `,`→`...` missing space (fixed:
`const`/`let`/`var` bail-out in `MiscRuleCore.parseAssignment`, mirrors
C++'s `auto [a, b]` bail-out).

**ts_combined/ts_comments activation:** `Map<string,number>` ASI bug
(`GENERIC_SAFE_KEYWORDS` missing TS primitives, extended); enum last-member
no-trailing-comma bug (`parseEnumMembers` `break` instead of bailing whole
enum); generic-argument comma spacing (new pass
`enforceGenericArgumentCommaSpacing`); new pass
`enforceUnionTypeContinuationIndent` for multi-line `type X = A | B | C;`
continuation indent (bug: RHS depth-scan bailed on any comment, fixed to
only bail on frozen tokens); new feature `enforceClassFieldAlignmentGrid`
(class-field `:`/`=` alignment) with two bugs fixed in
`rewriteClassFieldGroups` (double-indented first field; duplicate blank
line before a leading-commented group).

**expressjs/express (141 `.js` files, HEAD `ae6dd37`):** two bugs via
`node --check`, both fixed (fixture `real_code_regressions_77`): ASI
leading-continuation-operator/comma bug (`maybeInsertSemicolon` only
checked previous line's trailing token; fixed via
`LEADING_CONTINUATION_OPS` + leading-`,` check); no JS/TS regex-literal
tokenizing (fixed via `TokenizerCurly.emitRegexLiteral` +
`isRegexLiteralAllowedHere`). Final: zero crashes, idempotent, `node
--check` 141/141 (was 93/141 pre-fix); semantic smoke test (require,
live HTTP round-trip, Mocha subset 35/35) passed; one unrelated
pre-existing environment failure confirmed not formatter-induced.

**nestjs/nest (HEAD `7e6e313`):** five bugs fixed: (1) `/**` JSDoc opener
corruption, universal curly-family bug in
`MiscRuleCore.reformatMultiLineBlockComment` (assumed 2-char opening
marker; fixed to scan forward while char is `*`; also fixed for Java,
Kotlin fixture updated); (2) dot+space corruption in
`MiscRuleCurly.renderCallCandidate`'s `sigForRender` for multi-arg calls
whose every arg is a bare dotted member-access (fixed: force
`sigForRender` to `null` for JS/TS, fixture `_81`); (3) content
duplication in `enforceClassFieldAlignmentGrid` on nested `class` braces
(fixed: only grid outermost brace per nesting level, fixture `_82`); (4)
comment-continuation-indent drift on an object-shaped intersection alias
(fixed: `enforceUnionTypeContinuationIndent` only reindents at depth 0,
fixture `_84`); (5) `join(...)` call-wrap/collapse non-idempotency at
exactly `lineLengthLimit` (fixed: fits-check on multi-line-source branch,
fixture `_93`).

### `vuejs/core` dogfood pass — DONE, one issue deferred

`utils.ts` switch-case fallthrough non-idempotency was found here first
(later confirmed a second time in `lodash/lodash` below, and eventually
fixed project-wide as `RDD_KEY_263`). No other bugs found; all other files
clean/idempotent.

### `lodash/lodash` dogfood pass — DONE

Repo: `lodash/lodash` (`/tmp/lodash`, HEAD `a666ba5`, v4.18.1). Single large
`lodash.js` (17259 lines) plus build tooling/tests. In-scope corpus: 27 real
`.js` files, 50983 total lines (`dist/*`/`vendor/*` excluded per file-
exclusion convention).

**Baseline:** `node --check` 27/27 pass. **Round1:** zero crashes, 27/27
formatted, `node --check` 27/27 pass. **Idempotency:** 26/27 byte-identical;
`lodash.js` differs — the same switch-case-fallthrough shape as the
`vuejs/core` `utils.ts` issue above (second confirming data point; later
fixed as `RDD_KEY_263`).

**Content-preservation** (`js_ts_content_diff.js`, original vs. round1):
initially 17/27 "MISMATCH", all decomposing into two intentional, non-lossy
transformations the checker didn't yet tolerate (comment trailing-period
stripping; §10 single-expression-block brace omission) — both became checker
tolerances (see Dogfood Output Validation above). After checker improvement:
**22/27 clean**. Remaining 5 files are two further confirmed-intentional,
non-lossy classes, left unfixed (checker gap, not formatter bug, low
priority TODO): bare single-param arrows gaining parens (3 files, documented
§6 behavior); STYLE.md §4 pre-increment-except-when-post-required correctly
rewriting a standalone/unused for-loop increment (2 files,
`perf/perf.js`/`test/test.js`).

**Verdict: DONE.** Zero new formatter bugs found. The one idempotency diff
was a confirming recurrence of the (now-fixed) `SwitchRule` issue.

### `angular/angular` dogfood pass

Clusters 1-3 FIXED, cluster 4 FIXED (all 4 named root causes landed,
including the `alignBracelessElseIfChain` cause — RDD_KEY_269 — which
fixed its 2 files, `shared.ts`/`directive_outputs.ts`; `RDD_KEY_248`
separately fixed most residual files outside the 4 named causes; the
final 2-file residual surface, `web_animations_player_spec.ts`/
`input_transform.ts`, is now also fixed — RDD_KEY_271, see "Active work"
above), cluster 5 RESOLVED
(2026-08-05 — all 3/3 files idempotent via existing opt-in flags, see
below; `curly-general-scope-reindent`/`-multipass` stay `off` by default
project-wide).

Repo: `/tmp/angular`, shallow clone (`--depth 1`), HEAD `5ad8231`
(2026-07-24). Scope: 5394 `.ts` files (`.d.ts`/`.tsx` excluded) across
`packages/`, `adev/`, `devtools/`, `integration/`, `modules/`,
`vscode-ng-language-service/`, `dev-app/`, `tools/`. Formatted in 8 batches
(one per top-level dir), round1 then round2. Syntax check: TS compiler-API
parse-only (no type-check).

**Initial stats:** 0 crashes/5394 files; 29 idempotency mismatches; parse-
check baseline (unformatted) 0/5394 errors; round1 output **46/5394** files
with parse errors (339 diagnostic lines) — 46 real formatter-induced syntax
corruptions despite zero hard crashes.

**Clusters 1-3 (all FIXED, most-valuable-first order):**
1. **[CRITICAL]** Dotted/qualified type-predicate or return-type before
   `=>` wrapped its last segment in a spurious paren pair — ~40/46 broken
   files (e.g. `node is tss.Node =>` → `node is tss.(Node) =>`).
   `enforceArrowFunctionParameterParens`'s backward scan didn't walk back
   over a multi-segment dotted path before its existing `is`/`typeof`/
   `keyof` bail-out. Fix: walk back over any number of `IDENTIFIER '.'`
   pairs first. Fixture `real_code_regressions_134`.
2. **[CRITICAL]** Old-style angle-bracket cast (`<Type>{...}`) misparsed
   as a generic, injecting a bogus `;` into the following object literal —
   1 file (`testability.ts:229`). Fix: new `isLegacyCastBrace` — a `{`
   preceded by a plain `>` whose matching `<` sits before a type name
   following a value-starting token is now treated as a value/pattern
   brace. Fixture `real_code_regressions_135`.
3. **[CRITICAL]** Multi-line generic return-type clause lost its closing
   `>`, spilling a bogus `;` into the type — 1 file (`utils.ts:103-105`,
   TS dynamic-import type-query operand). Fix: add `"import"` to
   `GENERIC_SAFE_KEYWORDS`. Fixture `real_code_regressions_136`.

`make test` after clusters 1-3: 185/185.

**Cluster 4 — call-wrap/collapse vs. alignment-padding fits-check ordering**
— dominant idempotency cluster, originally ~23/29 idempotency-mismatch
files. Root cause family: `enforceCallLineBreaking`'s single-argument
fits-check measures candidate line length **before** declaration-alignment/
complexity-padding finish adjusting column widths, flip-flopping every
round.

- **Root cause #1 — trailing-comma dangling-empty-group** (confirmed via
  `create_router_state.ts:27`): `renderCallDropped`/`renderCallOnePerLine`
  measured via `splitTopLevelCommas`, which — unlike `groupByOriginalLine`
  — didn't drop a dangling trailing empty group from a trailing comma
  before `)`. Fixed by adding the same drop to both methods. Fixture
  `real_code_regressions_140`.
- **Root cause #2 — `if (`/`if(` keyword-spacing pipeline ordering**
  (confirmed via `node_selector_matcher.ts:155`, `locale_plugin.ts:42`):
  the fits-check for a call inside `if (...)` measured the line before
  `enforceKeywordSpacing` (collapses `if (` → `if(`) had run — one char
  narrower on reformat, flipping the boundary (confirmed exact
  101-vs-100). Fixed by pulling `enforceKeywordSpacing` forward to run
  immediately before the first `enforceCallLineBreaking` call. Applies to
  all curly-brace languages (shared `MiscRuleCore`). Fixture
  `real_code_regressions_141`. `make test` after both: 190/190.
- **Root cause #3 — braceless-else body never re-validated after brace-
  collapse/alignment** (`format_date.ts:519`): `collapseSingleExpressionBlocks`
  strips `if`/`else` braces in Phase 0, before `enforceCallLineBreaking`
  (Phase 1) — a braced source used a `+`-chain complexity-wrap to fit,
  which doesn't apply to the now-braceless body, leaving the joined line
  over the limit; `alignBracelessElseIfChain` pads it anyway (intentional
  escape hatch, not a bug there). **First attempt (reverted):** refuse
  collapse whenever the joined one-liner exceeds `lineLengthLimit` — **do
  not retry**: with no way to know `enforceCallLineBreaking` will later
  wrap an inner call to make it fit, this wrongly re-braced every
  wrappable-call-body braceless if/else, breaking 5 fixtures
  (`java_combined`, `real_code_regressions_57`/`_81`/`_93`/`_141`). **Real
  fix (landed 2026-07-31):** `BlockStructureRule.refuseUnrescuableCollapse`
  — refuse collapse only when the joined candidate is over-limit **and**
  `hasBreakableCall(tokens, from, to)` finds no rescuable call (a
  `name(args)`-with-nonempty-args span), reusing the same cheap-heuristic
  precedent as `JavaSpecificRule.isSingleLineBody`/`GetterSetterRuleCurly
  .parseOneLinerMember`'s length pre-check rather than a true two-pass
  simulation. Called from `tryCollapse`, `collapseBracelessBody` (shared
  by both `tryCollapseBraceless` and the bare-`else` collapse path, gained
  a new `indentAnchorIdx` parameter), and (found while building the
  permanent fixture) a third site — the bare-terminal `else { ... }`
  chain-collapse path inside `collapseSingleExpressionBlocks`
  (`chainAllBranchesCollapsible`-gated) — which built its candidate inline
  and had no gate until this follow-up. Scan covers the *whole* candidate
  (condition + body), not body-only as originally worded (a body-only scan
  broke `real_code_regressions_141`, whose rescuable call is in the
  *condition*). Fixture `real_code_regressions_172`. `make test`: 221/221.
  All 5 originally-cited angular files confirmed individually idempotent.
  Full `packages/` re-scan (3900 files): 12 differ (down from ~23), 3 of
  which are the separate cluster 5 gap (untouched by this fix), 9 not
  fixed by this session — see "Known residual limitation" and the later
  2026-08-07 re-scan below.

- **Root cause #4 [FIXED] — trailing same-line comment inconsistently
  counted in the collapse fits-check** (`location_shim.ts:461`): fresh
  format counts the trailing comment's width in the fits-check (over limit
  once `=`-alignment padding widens the column) → wraps; once wrapped the
  comment moves past the call's `)`, so reformat measures without it →
  collapses back. Fixed via `appendRangeCollapsingTrailingCommentGap`
  (`MiscRuleCurly.java`): whitespace before a trailing line comment
  collapses to one space for measurement only (never rendered), used only
  in the JS/TS tight-candidate fits-check's `suffix`. Fixture
  `real_code_regressions_142`. `make test`: 191/191. A related 3-sibling
  `=`-alignment-group non-self-stability quirk was seen while building the
  fixture but did NOT reproduce against real `location_shim.ts` (confirmed
  idempotent there) — not investigated further, flag if a future dogfood
  run hits it.

  **Known residual limitation** (not a regression — confirmed via
  `format_date.ts:519` and `checker.ts:16487` in the TS corpus):
  `hasBreakableCall` only asks "does a rescuable call exist," not "will
  wrapping it actually bring the line under `lineLengthLimit`." When the
  one breakable call's wrap doesn't shrink the joined line far enough
  (e.g. dominated by a long `+`-concatenation chain rather than the call
  itself), collapse still proceeds and the line stays over-limit
  post-wrap — a narrower, more surgical measurement (comparing the
  post-wrap estimate against the limit, rather than blanket-widening as
  the earlier reverted attempt did) was identified as a possible fix
  direction but not attempted (budget spent on the two extremes already
  disproven — see cluster #3 sibling / `RDD_KEY_249` below for the
  parallel investigation and its own negative result).

**2026-08-07 root-cause-only re-scan** (no fix landed, doc-only): re-ran
the cluster-4 corpus (`packages/` 3900 files + `devtools/.../split/*.ts`,
3906 total) fresh. `compiler-cli/.../{compiler,host}.ts` and
`split.component.ts` (previously "not re-checked") are now idempotent. Of
the 9 files previously named "not fixed" under root cause #3, **7 are now
idempotent** (fixed as a side effect of `RDD_KEY_248`): `format_date.ts`,
`parser.ts`, `jit_compiler_facade.ts`, `r3_template_transform.ts`,
`util.ts`, `node_js_file_system.ts`, `migration.ts`. Only
`web_animations_player_spec.ts` and `input_transform.ts` remain broken.
**Full re-scan: only 7/3906 files still non-idempotent** — 3 are cluster 5
(unrelated, see below), 2 are the known `processScope` family (tracked as
"Active work" item #3 above), 2 are the newly-identified
`alignBracelessElseIfChain` cause (tracked as "Active work" item #1
above). No source change landed this session.

**Cluster 5 — RESOLVED 2026-08-05.** `user_metric_spec.ts`/`i18n_parse.ts`/
`emit.ts` — a pre-existing inconsistent-source-reindentation architectural
gap (GDR pipeline interaction, `RDD_KEY_229`, full investigation in
`STATE_CURLY_GDR.md`). A safer 4-stage GDR/pipeline/GDR/pipeline sequence
(`curly-general-scope-reindent-multipass`, opt-in, only active when
`curly-general-scope-reindent` is also on) was designed and landed in the
GDR job (`RDD_KEY_233`/`RDD_KEY_234`, 2026-08-03) and re-confirmed fresh
against the live `/tmp/angular` checkout this session: **all 3/3 files now
produce a zero-line diff**, all pass `js_ts_syntax_check.sh`. Both flags
remain `off` by default project-wide — this is a per-corpus dogfood
recommendation for one-true-brace-style source, not a default change. No
new fixture needed (existing `test/curly_gdr_multipass_inp.java`/`_out.java`
already covers the shared `GdrPipelineGate` mechanism).

### `microsoft/TypeScript` dogfood pass

3 of 4 clusters FIXED; cluster #3 substantially reduced by `RDD_KEY_248`.

Checkout: `/tmp/ts-dogfood/TypeScript`, shallow clone (`--depth 1`), HEAD
`cc5c6e2` (2026-07-28) — reuse, do not re-clone. Scope: `src/` only, 601 real
`.ts` files (108 `.d.ts` excluded), 379045 lines; `.tsx` excluded (JSX/TSX
out of scope); `tests/cases/**`/`tests/baselines/**` excluded.

Round1: zero crashes, 601/601 produced. Syntax-checked via a throwaway
TS-compiler-API parse-only script. Baseline (unformatted) 0/601 parse
errors.

**Category 1 — Critical (round1 corrupt/unsafe): 8/601 files, 3 root
causes — ALL FIXED (2026-07-28):**
1. **`||=`/`&&=` not tokenized as a single token** — 1 file (`checker.ts`).
   `MULTI_CHAR_OPS` had `??=` but was missing `||=`/`&&=`. Fixture
   `real_code_regressions_143`.
2. **Union-type return-type/type-predicate before `=>` gets its last
   segment wrapped in a spurious paren pair** — 6 files (same function as
   angular cluster 1's fix, but that fix didn't walk back over a union `|`
   operator too). Fix: walk-back loop alternates `IDENTIFIER '.'` and
   `IDENTIFIER '|'` walk-backs until neither makes progress. Fixture
   `real_code_regressions_145`.
3. **Backslash-newline continuation inside a string literal, CRLF-specific,
   corrupts the rest of the string/statement** — 2 files (both CRLF).
   `TokenizerCurly.emitString`'s backslash-escape handling only consumed
   the backslash + the `\r` half of `\r\n`. Fix: special-case `\` + `\r\n`
   to advance 3 chars. Fixture `real_code_regressions_147` —
   **`.gitattributes` marks `test/real_code_regressions_147_inp.ts`
   `-text`** so git preserves its deliberate CRLF bytes; preserve this if
   touching the fixture. `make test`: 196/196.

**Category 2 — Idempotency-only, cluster #3 (call-wrap/collapse vs.
alignment-padding fits-check ordering):** originally 28/601 files, **same
root cause as `angular/angular` cluster 4** — a third confirming
recurrence, proportionally ~30x denser (line lengths sit close to the
100-char boundary often). `commandLineParser.ts` was this cluster's
config-insensitive reference repro; its exact shape is now fixed by
`RDD_KEY_248` (`ScopePipelineCurly.reapplyClosingBraceAndDeclarationsPass`)
— reconfirmed empty round1↔round2 diff, plain and at `indent-size=2`.

**2026-08-07 re-scan:** fresh full-corpus round1→round2 (`src/`, 601
files) shows **only 14/601 still differing**: `compiler/builder.ts`,
`compiler/checker.ts`, `compiler/moduleNameResolver.ts`, `compiler/
program.ts`, `compiler/tsbuildPublic.ts`, `compiler/types.ts`, `compiler/
watchPublic.ts`, `harness/collectionsImpl.ts`, `harness/
incrementalUtils.ts`, `server/editorServices.ts`, `server/project.ts`,
`services/codefixes/fixMissingTypeAnnotationOnExports.ts`, `services/
codefixes/fixUnusedIdentifier.ts`, `services/findAllReferences.ts` — a
real reduction from `RDD_KEY_248`. `checker.ts` still shows the already-
explained root-cause-#3 residual (`hasBreakableCall` approximation gap,
above) — left alone. `harness/collectionsImpl.ts` is the newly-found
`applyAssignmentsPass` sibling bug — see "Active work" item #2 above.

### Ranked list (most-valuable-first, from the 2026-08 TypeScript-corpus
bug-hunt pass)

1. `||=`/`&&=` tokenizer gap — FIXED. Trivial fix, highest value/difficulty.
2. Union-type-before-`=>` spurious wrap — FIXED. Direct low-risk extension
   of an existing precedent.
3. Call-wrap/collapse vs. alignment-padding ordering — substantially fixed
   (`RDD_KEY_248`/`RDD_KEY_250`); residue tracked under "Active work" above.
4. Backslash-newline CRLF string corruption — FIXED. Real corruption but
   narrow (2 files, old test-harness idiom).

No fixture-only false positives found in that pass (used direct
TS-compiler-API parse-checking + raw `diff`, not `js_ts_content_diff.js`).

### `compiler/watchPublic.ts` nested-array-literal syntax corruption — FIXED (2026-08-09)

Found re-confirming the `microsoft/TypeScript` dogfood residual-files list
(see "Note on `microsoft/TypeScript`'s status" below): `new Map([[undefined,
undefined]])` (a call whose sole argument is a nested/double-bracketed
array literal `[[...]]`) got a stray `;` inserted inside the parens on a
single format pass — genuine output corruption, not merely an idempotency
gap. Reproduced independent of C/C++/Java scope (isolated with a plain
`.ts` repro, both as a single physical line and as a source-multi-line
call).

Root cause was shared-tokenizer, not JS/TS-specific:
`TokenizerCurly`'s C++11 `[[attribute]]`-open detection (`c == '[' &&
peek(1) == '[' && looksLikeAttributeOpen()`) was missing the `&&
lang.isCpp` guard its two sibling branches (`]]` attribute-close, `[:`)
both already have. A TS nested `[[` array-open matched the C++ heuristic
and got tokenized as an `OP` "attribute open" token, while its matching
`]]` close (correctly *not* gated to non-C++ languages) fell through to
the ordinary `PUNCT` bracket-close path — an asymmetric OP/PUNCT pair.
Every downstream `isPunct(t, "[")`-based bracket-depth tracker then
undercounted this array's open relative to its close, including
`MiscRuleCurly.enforceCallLineBreaking`'s own `matchParenForward` scan,
which read the call's argument slice as extending one token too far (past
the real `)`, through to the statement's own `;`) and rendered a spurious
statement terminator inside the call.

Fixed by adding the missing `&& lang.isCpp` guard to the `[[`-open branch
(one-line change, `TokenizerCurly.java`). Verified: the isolated repro, the
full `watchPublic.ts` file (both syntax-clean and idempotent
post-fix), a genuine C++ `[[nodiscard]]` attribute (still tokenizes
correctly, no regression), and `make test` (271/271 forward + idempotency,
zero regressions). New fixture: `real_code_regressions_194_inp/out.ts`.

### Braceless if/else-if body-column-alignment padding, CRLF-staleness — FIXED (2026-08-09, RDD_KEY_273)

Found in the same 2026-08-09 reconfirmation of the 14 previously-flagged
dogfood files (see "Note on `microsoft/TypeScript`'s status" below), Tier-3
shape #1: `BlockStructureRule.alignBracelessElseIfChain`'s single-statement
body-column padding for a braceless `if`/`else if` chain went stale across a
second format pass — round1 leaves a short `if` branch's body unpadded
(natural width), round2 (fed round1's own output) pads it to align with its
`else if` sibling's wider column. Example, `compiler/moduleNameResolver.ts`:
`if(options.resolvePackageJsonExports) features |= NodeResolutionFeatures.
Exports;` stays unpadded on round1 but gains extra spaces before `features`
on round2. Affected files: `compiler/builder.ts`, `compiler/
moduleNameResolver.ts`, `services/findAllReferences.ts` — all three are
CRLF-original source, which turned out to be the actual trigger.

Root-caused with `JXFMT_DEBUG_ELSEIF`-env-gated `System.err` instrumentation
added to the body-padding loop (same pattern as the `watchPublic.ts` fix
above): debug prints showed `end`/`target`/`spaces` identical between round1
and round2, but `body.length()` differing by exactly 1 (44 vs. 43). `od -c`
on the raw line traced the extra byte to a literal trailing `'\r'`.
`alignBracelessElseIfChain` splits its working text on `"\n"` only (not
`"\r\n"`), so every line retains a trailing `'\r'` while formatting
CRLF-original text — `Main.applyLineEndings`'s own comment already
documents that CRLF/LF normalization happens exactly once, at the very end,
precisely because "the internal formatting pipeline is not guaranteed to
have stripped every original `'\r'`". That stray character skews every
length computation this method makes, invisible almost everywhere except
the exact `lineLengthLimit`-boundary guard deciding whether to pad a
branch: on round1 (still-CRLF mid-pipeline) a branch's would-be-padded width
computes to just over the limit (guard refuses to pad); on round2 (fed
round1's own already-LF-only, one-byte-narrower-per-line output) the same
computation lands exactly at the limit (guard allows the pad) — non-
idempotent.

Fixed by stripping any trailing `'\r'` from each split line immediately
after the initial `split("\n", -1)`, before any measurement — safe to drop
rather than restore, since `Main.applyLineEndings` independently re-derives
the final output's line-ending style from the *original* file text (its
`"preserve"` branch calls `detectDominantLineEnding(original)`), never from
`'\r'` bytes surviving inside the internal pipeline. Verified: A/B rebuild
(reverted via `git checkout`, rebuilt, reproduced the exact stale-padding
symptom on both the new fixture and `compiler/moduleNameResolver.ts`
directly; restored, rebuilt, confirmed idempotent), all 3 originally-flagged
files re-verified idempotent against the fresh `microsoft/TypeScript` clone,
and `make test` (271/271 -> 272/272 forward + idempotency, zero
regressions). New fixture: `real_code_regressions_195_inp/out.ts`
(deliberately CRLF-encoded, `.gitattributes`-marked `-text`, same precedent
as `real_code_regressions_147_inp.ts`).

This same CRLF-trailing-`'\r'`-skews-a-length-guard mechanism was also the
root cause of Tier-3 shape #4 (see below) and turned out not to be the root
cause of shapes #2/#3 — see their own writeups for what those turned out to
be.

### Known false positives (no source change needed, fixture-only)

- Spurious-looking blank line after a class's opening `{` in older `.js`
  fixture drafts, and doubled trailing space before a one-liner getter
  body's closing `}` — both confirmed correct, existing behavior (STYLE.md
  §7 named-construct blank line; `GetterSetterRuleCurly`'s group-width body
  padding), matching passing C++/Java/Kotlin fixtures byte-for-byte. Only
  the stale hand-authored `.js` draft fixtures were wrong; resolved by
  regenerating them.
