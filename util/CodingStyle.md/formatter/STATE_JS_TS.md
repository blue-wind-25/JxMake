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
pipeline. Current `make test`: 196/196 forward + 196/196 idempotency (grows
as fixtures are added; see dogfood sections below for latest count history).

---

## Status Summary

All planned baseline work is **DONE**: §1–15 implemented, JS and TS local
fixtures active, and real-code dogfood passes completed for
`expressjs/express`, `nestjs/nest`, `vuejs/core`, `lodash/lodash`,
`angular/angular` (categorized, most clusters fixed), and
`microsoft/TypeScript` (categorized, 3/4 clusters fixed). Two dogfood
findings remain open by design — see "Active work" below. JS/TS basics were
deliberately hardened to a stable baseline before Python3 (next job in
rotation) per user direction.

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
| RDD_KEY_248 | Call-wrap/collapse vs. declaration-alignment/padding idempotency bug (Tier-4, see Open Questions), 3rd session, FIXED: `ScopePipelineCurly.reapplyClosingBraceAndDeclarationsPass` re-runs just the closing-brace + declarations passes a second time (JS/TS only), with the shared trailing-gap force-reindent step skipped on that re-run |

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
`normalize-comment-start-case` behavior, not a bug). Comments recovered separately via `ts.getLeadingCommentRanges` (TS AST
doesn't attach them as tree nodes), scanned at every node's `getFullStart()`
plus position 0/EOF, deduplicated by `[pos, end)`.

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
- **Cluster #3 sibling ("declaration/class-field-alignment-grid vs. call-wrap
  ordering") — RESOLVED 2026-08-06 (RDD_KEY_248), see that dated subsection
  below for the landed fix. History below kept for context (2026-08-05
  investigation session, no code change landed, new architectural finding
  recorded).** Attempted to root-cause and fix the
  `microsoft/TypeScript` `commandLineParser.ts` `pathOptions`/`optionMap`/
  `watchOptionMap` declaration-group shape (minimal repro: `/tmp/mini.ts`,
  see below) directly named in cluster #3's write-up above as "not a
  braceless if/else collapse... a sibling root cause in the same 'call-wrap
  vs. column-width-adjusting-pass ordering' family."

  **First hypothesis (WRONG, but instructive):** `JsTsDeclarationAlignmentRule
  .spansMultipleLines`'s flat `parenDepth`/`braceDepth` counters bail
  (exclude the row from its alignment group) on any `NEWLINE` inside a
  brace (`braceDepth > 0`), even when that newline is really just a nested
  call's own wrapped argument list (`{ key: someCall(\n arg\n), ... }`) --
  narrower than the existing paren-only carve-out documented in that
  method's own javadoc. Replacing the two flat counters with an actual
  bracket-kind stack (bail only when a newline's innermost enclosing
  bracket is `{` or the stack is empty; tolerate `(`/`[`) is a real,
  narrowly-scoped improvement to that one method and **did not regress
  `make test` (244/244 forward + idempotency, unchanged)** -- but **did NOT
  fix the `commandLineParser.ts` repro**, so it was reverted rather than
  landed as a partial/silent change. Root cause is one level up.

  **Actual finding, via debug instrumentation (removed before revert):**
  `ScopePipelineCurly.processScope` runs its five per-scope passes
  (`applyDeclarationsPass` first) **outer-first over the literal flat
  token list passed to it**, then separately recurses into each child
  `{...}` span and reruns the same five passes again on that child's own
  (already outer-pass-touched) slice -- confirmed via instrumented entry
  dumps at both the depth-0 (whole-file) and depth-1 (function-body) calls
  for the same statements. For fresh (never-before-formatted) input this
  is a no-op at depth 0 for nested declarations (round1's depth-0 entry
  dump showed the statements untouched, only depth-1's own call actually
  grouped/rendered them) -- but **for already-formatted input whose
  initializer already contains an embedded call-wrap newline (i.e. round2,
  reformatting round1's own output), the depth-0 pass's own
  grouping/statement-splitting no longer treats the region as opaque the
  same way**, so depth-0 now *also* renders (or partially renders) these
  declarations before depth-1 gets to reprocess the same span a second
  time -- two grid-alignment computations of the same statements inside one
  single `format()` call, seeded from two different intermediate states,
  is what actually produces the round1-vs-round2 divergence (confirmed via
  `System.identityHashCode`-tagged entry dumps showing the depth-1 call's
  *input* already differing between round1 and round2's runs, before
  depth-1's own logic even executes). This is a different, and likely
  larger, architectural issue than a single method's bail condition: it
  means `applyDeclarationsPass`/`groupAlignableDeclarations`'s statement-
  splitting is not reliably scope-opaque at the outer recursion level once
  a nested declaration's initializer already contains a previous-round's
  call-wrap artifact.

  **Why no fix was attempted for that deeper issue this session:** the
  double-processing (outer-pass-then-inner-pass-reprocess) appears
  deliberate, load-bearing infrastructure per `processScope`'s own javadoc
  ("recurses outer-first... splicing each child's processed text back in
  place") and is shared by every curly-family language's declaration/
  assignment/signature/getter-setter passes, not just JS/TS -- changing
  when/whether the outer pass is allowed to touch nested-scope content
  risks the same class of broad regression the `STATE_CURLY_GDR.md`/
  `RDD_KEY_229` pre-pass-vs-post-pass GDR investigation hit (a genuine
  circular dependency between an outer pass's decisions and an inner
  pass's re-derivation of the same span from different intermediate
  text), and this session had no time budget left to design, prototype,
  and real-corpus-validate a fix at that scope. Matches this cluster's
  existing "would need its own root-cause identification pass" framing
  above -- now with an actual root cause identified, but still unscoped
  for a fix. Left OPEN, same as before this session; no fixture added, no
  `RDD_LOG.md` key added (no design was actually landed to record).
  **Next session:** start from the `processScope` outer/inner double-pass
  finding above rather than re-deriving it; the `spansMultipleLines`
  bracket-stack refinement above is a real, still-available, no-regression
  incremental improvement if anyone wants it landed on its own merits
  (independent of this bigger issue) -- it was only reverted because it
  didn't resolve the cited bug alone and this session preferred not to
  land an unverified partial change silently.

  **2026-08-06 follow-up session (RDD_KEY_245), no code change landed,
  narrows but does not resolve the above:** re-investigated using a fresh
  minimal repro (`/tmp/mini.ts`, the `commandLineParser.ts`-derived
  `pathOptions`/`optionMap`/`watchOptionMap` shape) with direct
  `ScopePipelineCurly.processScope` entry-point instrumentation. **For this
  specific repro shape, the instrumented output shows only ONE recursion
  level touches these declarations** -- the object-literal initializer's own
  `{...}` is never found as its own `splitTopLevelSpans` "Span" at all
  (value-position braces nested inside a declaration initializer aren't
  span-recursed the way a function/class/control-flow body is), contradicting
  this file's own prior write-up's implication that the outer/inner
  double-pass is this repro's cause. Independently re-implemented and
  re-tested the `spansMultipleLines` bracket-kind-stack refinement described
  above (tolerate a `NEWLINE` whose innermost enclosing bracket is `(`/`[`
  even when that pair is itself nested inside the row's own `{...}`
  initializer) -- **confirmed again it does not fix `/tmp/mini.ts`** (same
  negative result, now independently reconfirmed). Further instrumentation
  narrowed the real divergence to something else entirely: round2 renders the
  object literal's closing `}` onto its own line where round1 keeps it
  trailing inline -- neither `processScope`'s span recursion (ruled out, see
  above) nor `JsTsDeclarationAlignmentRule`'s grouping (ruled out, the
  bracket-stack fix didn't change the symptom) is responsible; most likely
  locus is `MiscRuleCurly.enforceCallLineBreaking`/`renderCallCandidate`'s
  own multi-line-source closing-bracket placement (~line 1250-1330, the same
  region the braceless-if/else Open Questions entry below already names as a
  "which pass gets to see the final stable per-line width" locus) -- not
  traced further, out of this session's reasoning-effort budget. Full
  finding, including the exact instrumentation approach and next-session
  pointer, recorded in `RDD_LOG.md`'s `RDD_KEY_245`. No fixture added, no
  dogfood corpus re-run performed (repro-level investigation only this
  session). `make test` unaffected (244/244, no source change committed).

  **2026-08-06 second follow-up session (RDD_KEY_246), two fix attempts
  tried, both reverted, no code change landed.** Picked up directly from
  `RDD_KEY_245`'s pointer. Reproduced `/tmp/mini.ts` (same repro), added
  `DBG_FC`-gated instrumentation in `FormatterCurly.format` at "after
  `ScopePipelineCurly.process`" and "before/after the first
  `enforceCallLineBreaking` call" checkpoints. **This localizes the
  divergence to inside `ScopePipelineCurly.process` itself, before Phase
  1's `enforceCallLineBreaking` ever runs** -- contradicting `RDD_KEY_245`'s
  own tentative locus (`enforceCallLineBreaking`/`renderCallCandidate`
  ~line 1250-1330). Read that method's code directly and confirmed
  structurally it cannot be responsible: the span it replaces is strictly
  `[openParenIdx, closeIdx+1)`, never touching text after the call's own
  closing `)`, so it cannot be what moves a `}` that comes after the call.

  **Actual root cause (new finding, more precise than `RDD_KEY_245`'s):**
  `ScopePipelineCurly.applyOversizedAggregateInitClosingBracePass` --
  called once, early, inside `processScope`, well before Phase 1's
  `enforceCallLineBreaking` -- decides whether to move a dangling `}` onto
  its own line by checking whether the aggregate initializer's `{...}`
  *already* contains an embedded `NEWLINE` token. On a fresh format
  (round1) no such newline exists yet (the nested call hasn't been
  wrapped by `enforceCallLineBreaking` yet), so the pass is a no-op and
  `}` stays fused inline. On a reformat (round2) the previous round's
  call-wrap newline is already present in the input, so this time the
  pass fires and splits `}` onto its own line -- round1 != round2. Same
  "a pass's decision depends on a later pass's not-yet-produced newline"
  family as the precedent fixes already named in this file
  (`enforceComplexityPadding`, `enforceAttributeAndSpliceBracketPadding`,
  `enforceInitializerBraceSpacing`), just in a pass not previously
  suspected.

  **Attempt 1 (narrow re-run, reverted -- fixed the named symptom but
  revealed a second, same-family divergence):** added
  `ScopePipelineCurly.applyOversizedAggregateInitClosingBraceFixup`, a
  public wrapper re-running only `applyOversizedAggregateInitClosingBracePass`,
  called once more from `FormatterCurly.format` right after the first
  `enforceCallLineBreaking` call. This corrected the `}` placement on
  round1 to match round2 for `/tmp/mini.ts`, but exposed that
  `JsTsDeclarationAlignmentRule.spansMultipleLines`/`parseDeclaration`'s
  grouping decision for the same `pathOptions` row is *also* made against
  the stale (pre-call-wrap) shape on round1 vs. the post-call-wrap shape
  on round2 -- round1 keeps the row in its alignment group (wider column
  padding), round2 excludes it (narrower padding) -- same underlying
  "decision made before the shape is final" bug, manifesting a second
  time in declaration-alignment padding, not just brace placement. Narrow
  re-run insufficient; reverted rather than land a partial fix.

  **Attempt 2 (full `ScopePipelineCurly.process(text)` re-run, reverted --
  fixed `/tmp/mini.ts` completely but caused a real regression on the
  fixture corpus):** replaced the narrow fixup with a second, full
  `scopePipeline.process(text)` call in `FormatterCurly.format`'s Phase 1,
  right after the first `enforceCallLineBreaking` call, so every
  per-scope pass (closing-brace, declarations/alignment, assignments,
  signature, getter-setter) re-derives its decision against the
  post-call-wrap shape. This made `/tmp/mini.ts` fully idempotent
  (`diff round1 round2` empty, matching the previously-established stable
  fixed point) -- but `make test` (built via the proper `make jar`/`make
  test` path, not an ad-hoc `javac`) showed **real, new forward-pass
  regressions** on fixtures unrelated to the repro: `real_code_regressions_100.ts`
  collapsed an already-correct `} // interface ParserOptions` (closing
  brace + trailing comment, previously on its own line) back onto the
  same line as other content; `real_code_regressions_144.kt` and several
  Java/`cpp26` fixtures also failed. Re-running the whole pipeline a
  second time is evidently not safe to do unconditionally -- some pass in
  the five-pass sequence treats a second same-round invocation as "this
  span was already finalized, re-collapse/re-merge it" rather than as a
  true no-op refinement, at least for trailing-comment-after-`}` shapes.
  Reverted; both source files (`FormatterCurly.java`,
  `ScopePipelineCurly.java`) restored to their pre-session `HEAD` content
  and the `.jar` rebuilt from clean `HEAD` via `make jar`; `make test`
  reconfirmed clean at baseline (**244/244 forward + idempotency, zero
  regressions** from the revert). No dogfood corpus (TS repo/Angular
  repo/lodash) run this session -- both attempts were disqualified by the
  local fixture suite before reaching that stage.

  **Note on a testing pitfall hit this session:** a single very long
  `make _test_serial JAR_FILE=...` invocation's terminal output can be
  silently truncated by the calling tool without any visible marker,
  hiding real `FAIL` lines among hundreds of `PASS` lines -- always
  redirect `make test`/`make _test_serial` output to a log file and
  `grep -n "^FAIL"` it directly rather than trusting a live/streamed
  terminal capture for a suite this size.

  **Next session:** the root cause (`applyOversizedAggregateInitClosingBracePass`'s
  stale-newline check) is now precisely identified and is a strictly
  better starting point than `RDD_KEY_245`'s. A viable fix likely needs to
  re-run *only* the closing-brace pass plus the declaration-alignment
  grouping/padding pass a second time (the two passes shown to actually
  need the post-call-wrap shape) while leaving the signature/getter-setter/
  assignment passes single-pass, to avoid Attempt 2's `real_code_regressions_100`-
  style trailing-comment regression -- this narrower combination was not
  tried this session (budget spent confirming the narrowest (Attempt 1)
  and widest (Attempt 2) ends of the spectrum both fail, in different
  ways). Full finding recorded in `RDD_LOG.md`'s `RDD_KEY_246`.

  **2026-08-06 third follow-up session (RDD_KEY_248), FIXED -- landed.**
  Picked up `RDD_KEY_246`'s untried "narrower middle ground" pointer
  directly: re-run only `applyOversizedAggregateInitClosingBracePass` +
  `applyDeclarationsPass` (closing-brace first) a second time, via a new
  `processScope(..., closingBraceAndDeclarationsOnly)` overload and public
  `ScopePipelineCurly.reapplyClosingBraceAndDeclarationsPass(String)` entry
  point, called from `FormatterCurly.format` (JS/TS only) right after the
  first `enforceCallLineBreaking`. **First cut reproduced Attempt 2's exact
  `real_code_regressions_100.ts` regression again** (`} // interface
  ParserOptions` losing its 2sp indent) -- tracing it down showed the true
  cause is NOT the two token-level passes at all, but `processScope`'s
  shared trailing-gap force-reindent step (the block right after the
  span-recursion loop that snaps a child scope's closing-brace gap to a
  freshly-recomputed `effectiveSpanIndent`): on this second, narrower
  re-run, that step re-derives indentation from the round's ALREADY-
  reformatted physical text (other Phase 1 passes' blank-line-insertion/
  Allman-conversion already baked in by this point), a different shape
  than what `findParentIndent` saw during the original `process()` call,
  so it can silently flip an already-correct closing-brace indent. **Fix:**
  skip that force-reindent step entirely when
  `closingBraceAndDeclarationsOnly` is true (the first `process()` call
  already got it right once; the narrower re-run only needs its own two
  passes' own splices). With that one gate, `real_code_regressions_100.ts`
  matches expected output again. Validated: `make test-quiet` 492/492
  (245 fixture pairs incl. new `real_code_regressions_179`, forward +
  idempotency, zero FAIL); `/tmp/mini.ts` fully idempotent;
  `microsoft/TypeScript` dogfood corpus (`/tmp/ts-dogfood/TypeScript`,
  `src/` only, 601 `.ts` files) round1/round2 mismatches 31/601 (freshly
  reconfirmed baseline) -> 20/601 with the fix, a strict subset (zero new
  regressions, 11 files newly idempotent incl. `commandLineParser.ts`
  itself); `angular/angular` dogfood corpus (`/tmp/angular`, same 5394-file
  `.ts` scope as the existing entry below) 17/5394 (freshly reconfirmed
  baseline) -> 15/5394, again a strict subset (zero new regressions, 2
  files newly idempotent). `lodash/lodash`'s cached checkout was found
  empty/stale this session (not re-cloned given the already-large
  corpus-validation cost already spent on the other two, much larger,
  corpora -- the fix is JS/TS-gated, not lodash-specific, and the two
  re-run corpora already give strong consistent evidence; a future session
  can re-verify lodash if convenient, not treated as a blocking gap). New
  fixture `test/real_code_regressions_179_{inp,out}.ts` (the
  `commandLineParser.ts`-derived minimal repro) registered in the
  Makefile's `INP_FILES` and `test/README.txt`. Full finding recorded in
  `RDD_LOG.md`'s `RDD_KEY_248`. **This item is now closed** -- see the
  Resolved Design Decisions index above.

- **README "braceless if/else collapse can still be non-idempotent" bullet
  (`hasBreakableCall`/`refuseUnrescuableCollapse`, `BlockStructureRule.java`)
  — 2026-08-05 investigation session, no code change landed, the documented
  fix direction was found to not address the actual mechanism.** Task: make
  the collapse decision simulate the later call-wrap pass's actual output
  width instead of only asking "does a rescuable call exist." Minimal repro
  built from the bullet's own cited files (`format_date.ts:519`): `/tmp`
  scratch file with `if (offset === 0) { return 'Z'; } else { return (
  (zone >= 0 ? '+' : '') + padNumber(hours, 2, minusSign) + ':' +
  padNumber(Math.abs(zone % 60), 2, minusSign) ); }` inside a plain function
  (no switch needed) reproduces round1≠round2 exactly as described.

  **First attempt (reverted, did not land):** widened `hasBreakableCall`
  (conceptually renamed to a "would wrapping actually fit" check) to render
  the prefix-through-`(` and `)`-through-end spans at the call's own base
  indent and refuse collapse unless *both* fit `lineLengthLimit`, mirroring
  `MiscRuleCurly.renderCallDropped`/`renderCallOnePerLine`'s actual output
  shape. **Broke `real_code_regressions_81`** (`make test` regression,
  confirmed then reverted): that fixture's own accepted, committed,
  genuinely-idempotent output (`this.createAsyncOptionsProvider(...)`/
  `getInjectionProviders(...)` case) has a post-wrap prefix line at **102
  chars — already over `lineLengthLimit=100`** — yet reformatting that exact
  output a second time reproduces it byte-for-byte (verified directly:
  round-tripping `test/real_code_regressions_81_out.ts` through the JAR is
  a no-op). So "does the post-wrap line fit under the limit" is **not** the
  right idempotency criterion -- a wrap that leaves a line over-limit can
  still be perfectly stable, and a width-based gate refuses legitimate,
  already-tested-correct collapses.

  **Actual root cause, found via debug instrumentation (removed before
  revert), does not live in `BlockStructureRule` at all:** built a minimal
  repro with the call *already braceless in the source* (no `{`/`}` to
  strip, so `collapseSingleExpressionBlocks`/`tryCollapse`/
  `refuseUnrescuableCollapse` never run at all) --
  `if (offset === 0) return 'Z'; else return ( (zone >= 0 ? '+' : '') +
  padNumber(hours, 2, minusSign) + ':' + padNumber( Math.abs(zone % 60), 2,
  minusSign ) );` -- and it **still reproduces round1≠round2 with zero
  braceless-collapse logic involved**, proving this specific bug's
  mechanism is independent of the collapse-decision heuristic the README
  bullet and the original design blame it on. Traced to
  `FormatterCurly.format`'s own two same-round calls to
  `MiscRuleCurly.enforceCallLineBreaking` (lines ~246 and ~271): the first
  call correctly wraps *both* `padNumber(...)` calls (each measured against
  the true whole-line width via `wholeLineRest`, both over limit, both
  wrapped) -- confirmed via `System.err` dumps of `enforceCallLineBreaking`'s
  own return value showing both calls multi-line immediately after the
  first call. But the *second* call re-examines each now-already-wrapped
  call independently via the `containsNewline(paramsSlice)` branch's own
  "would this rejoin fit on one line" fits-check (`MiscRuleCurly.java`
  ~line 1250-1330) -- which measures **only that one call's own candidate
  line in isolation**, not the full original combined statement width, so
  each of the two wrapped calls individually "fits" once rejoined and both
  get silently rejoined back onto one line, reproducing the exact original
  over-limit one-liner as round1's *final* on-disk output. Round2 (fed that
  same one-liner as fresh input) exhibits an indent/context-dependent
  asymmetry in the same rejoin logic (confirmed only on the real
  `format_date.ts` file, not fully isolated in the minimal repro) that
  rejoins the *first* call but not the *second* -- producing the partially-
  wrapped, differing output. Root cause is therefore an
  `enforceCallLineBreaking`-internal self-interaction across its own two
  per-round invocations (specifically the rejoin fits-check's blindness to
  sibling call/text width on the same combined statement line), structurally
  the same "call-wrap vs. pass-ordering" family STATE_JS_TS.md's cluster #3
  sibling entry above already named but did not root-cause -- not a gap in
  `hasBreakableCall`'s collapse-time approximation at all. Fixing it
  correctly requires `enforceCallLineBreaking`'s rejoin check to measure the
  *whole* combined statement line (accounting for other still-wrapped or
  already-rejoined sibling calls on the same original line), which is
  `MiscRuleCurly`/cross-invocation-shared-state work, not
  `BlockStructureRule`-local, and out of this task's scoped file.

  **Disposition:** no code change landed (both the reverted width-based
  gate and leaving `hasBreakableCall` as-is are confirmed-safe no-regression
  states; `make test` reconfirmed at 244/244 forward + idempotency after the
  revert). The README "Known Limitations" bullet's wording is accurate as
  currently written (still describes a real, unfixed gap) and was **not**
  narrowed or removed -- the investigation changed *where* the eventual fix
  belongs (an `enforceCallLineBreaking` rejoin-check fix, not a
  `BlockStructureRule` collapse-time gate) but did not change what's
  actually broken or unbroken. No `RDD_LOG.md` key added (no design was
  landed). **Next session attempting this:** do not retry the width-based
  `hasBreakableCall` gate shape without also handling
  `real_code_regressions_81`'s "post-wrap-still-over-limit-but-idempotent"
  case; the real fix needs to live in `MiscRuleCurly.enforceCallLineBreaking`
  around the `containsNewline` rejoin branch (~line 1250-1330), likely
  requiring the rejoin fits-check to see the full original line's other
  candidates' current (wrapped-or-not) state rather than judging each call
  in isolation -- consider whether this converges with cluster #3 sibling's
  already-identified `ScopePipelineCurly.processScope` double-pass finding
  into one shared "which pass gets to see the final, stable per-line width"
  architectural problem before attempting either in isolation.

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
exactly `lineLengthLimit` (fixed: fits-check on multi-line-source branch
of `renderCallCandidate`, JS/TS-scoped, fixture `_85`). `make test`
reached 134/134 by end of pass.

### `vuejs/core` dogfood pass — DONE

Repo: `vuejs/core` (`/tmp/vue-core`, HEAD `b5f8518`), 514 `.ts`/`.js` files
under `packages/`, `packages-private/`, `scripts/` (5 `.tsx` files under
`packages-private/dts-test/` correctly excluded, out of scope). Round1: zero
crashes, 514/514 formatted. Round1→round2 idempotency initially found 20
files differing.

**Bugs found and fixed (13 total across the pass):**
1. Leading multi-line block-comment reindent non-idempotency — class-field
   grid, enum formatting, interface/type-alias member alignment all
   re-emitted a captured leading `/** ... */` at its *original* indentation.
   Fixed via `reindentLeadingComment` at all three sites (fixture `_87`).
   Resolved 15/20 files.
2. `collectionHandlers.ts` — `GENERIC_SAFE_KEYWORDS` missing `symbol`/
   `bigint`, `isGenericSafeToken`'s OP case missing `|` (fixture `_88`).
3. `componentOptions.ts` — same symbol/bigint/`|` bug, plus type-parameter-
   default clause silently dropped by `parseTypeAlias`'s generic-clause skip
   loop, plus unconditional `]`-followed-by-`]` branch (meant for C++11
   attributes) misfiring on a TS mapped type, desyncing bracket-depth
   tracking (fixture `_89`).
4. `ref.test-d.ts`/`watch.test-d.ts` — `classifyBraces`'s `isValue`
   prev-token list missing `|`/`&`, misclassifying an inline object type's
   `{` after a union/intersection op as a statement-body brace (fixture `_90`).
5. `if( ... )` nested-call paren-padding order-dependency (11 files) — per
   RDD_KEY_62 a nested `(`/`[` anywhere inside a paren pair makes it
   "loose"; round1 under-padded when the `if`'s consequent was itself a
   multi-line call. Root-caused and fixed.
6. `scripts/release.js` call-wrap/collapse boundary bug — single-argument
   fits-check measured candidate width *before* `JsTsDeclarationAlignmentRule`'s
   column-alignment pass ran. Fixed (ancestor of the still-open angular/
   TypeScript "cluster 4"/"cluster 3" ordering bug — see those sections).
7–15. Nine further bugs via `tsc --noEmit` diff (0 vs. new errors), each
   root-caused/fixed (fixtures `_101`, `_102`, `_105` [5 sub-bugs], `_107`):
   `GENERIC_SAFE_KEYWORDS` missing `true`/`false`, then `keyof`/`is`/
   `infer`/`asserts`/`readonly`/`unique`/`as`/`satisfies`, then `typeof`;
   nested-brace-depth clear-all guard over-firing on a legit nested
   object-type arg, and separately not covering tokens inside nested
   braces; parenthesized-ternary `:` misclassified as return-type colon
   (new `isGroupingExpressionParen` helper); `typeof`/`keyof` not
   recognized as `prevPrev` by arrow-param-paren bail-out; trailing
   type-annotation `:` wrapped to next line got a bogus `;` (added `":"`
   to `CONTINUATION_OPS`); `isGenericSafeToken` OP list missing `=>`/`...`
   (latter `lang.isTs`-gated); TS function-type parameter list wrongly
   padded like a grouping paren.

**Final verification:** `make test` 156/156 forward + 156/156 idempotency.
Full 514-file round1 — zero crashes. Round1→round2 — **only one file still
differs**: `packages/compiler-sfc/src/script/utils.ts`, a switch-case
fallthrough (consecutive `case` labels sharing one body) non-idempotency,
**confirmed pre-existing on the unmodified codebase** (verified via `git
stash`/rebuild/retest/pop — original produces a different but equally-broken
symptom). Not fixed as part of this pass — see "Known open issues" below.

**Verdict: DONE.** All 13 formatter bugs found this session fixed and
covered by permanent fixtures. The `utils.ts` switch-case gap is confirmed
pre-existing, out of scope, tracked below.

### Known open issues (pre-existing, deferred — not part of `vuejs/core` DONE scope)

- **`utils.ts` switch-case fallthrough non-idempotency** — root cause:
  case-label-fallthrough one-liner-collapse/alignment feature interacts
  badly with consecutive `case` labels sharing one body. Confirmed
  pre-existing via `git stash` comparison, not introduced by this job.
  **Second confirming recurrence in `lodash/lodash`** (below):
  `fp/_baseConvert.js`'s `initCloneByTag` typed-array fallthrough case body
  (196 chars) — round1 leaves it unwrapped past `line-length-limit`, round2
  wraps the trailing call's arguments — a `SwitchRule` case-grid vs. generic
  call-wrap-fits-check ordering gap. Shared C/C++/Java-owned `SwitchRule`
  logic, deliberately left to a future session. **2026-07-28 re-assessment**
  (against `STATE_C_CPP_JAVA.md`'s "Known Gaps"): no fix landed there since;
  same risk class as the reindentation architectural gap this job is scoped
  to avoid touching piecemeal. Still deferred, not cheap.
- **Single-declarator colon spacing** — **FIXED 2026-08-04.** `const x:
  number = 1;` rendered as `const x : number = 1;` (space inserted before
  the colon) whenever the declaration had no alignment-group neighbors.
  Root cause: `JsTsDeclarationAlignmentRule.renderAlignedGroup` always put
  the `: type` text in its own `ColumnGrid` cell; `ColumnGrid.flush()`
  always joins adjacent cells with a single space, even for a one-row
  "group" of size 1 — so `x`/`: number` joined as `x : number` even though
  there was nothing to actually align a column against. STYLE.md §5's "a
  lone variable with no group neighbors...align trivially with itself, do
  not leave it awkwardly padded" rule means a singleton declaration should
  never pay the grid-join space. **Fix:** `renderAlignedGroup` now checks
  `group.size() == 1`; for that case the name and `: type` text are merged
  into one cell before being added to the row (no separate type-column cell
  at all), so the join space lands after the identifier as normal TS
  spacing instead of before the colon. A real (`size() > 1`) alignment
  group is untouched — it keeps the separate-cell/grid-padding path, whose
  space-before-`:` is the deliberately documented alignment look
  (STYLE_JS_TS.md §11.2's `DEFAULT : string` example) and must stay as-is.
  The `=` init column and trailing-comment handling were not touched (no
  reported bug there — a lone variable's `=` spacing was already correct).

  One pre-existing fixture had baked in the old buggy spacing:
  `test/real_code_regressions_107_out.ts`'s `let server : ReturnType<typeof
  createServer>;` (a genuine singleton, no group neighbors) was updated to
  `let server: ReturnType<typeof createServer>;` to match the now-correct
  behavior — this is the fixture's own file, not a hand-rolled special
  case, so no separate `RDD_KEY_*` was needed for it.

  New permanent fixture `test/real_code_regressions_177_{inp,out}.ts`
  (registered in the Makefile's `INP_FILES` and `test/README.txt`) covers
  both paths side by side: two singleton declarations (`const x: number =
  1;`, `let y: string;`, each isolated by a blank line) prove the fix, and
  a real 3-row `const` group inside a function body proves the alignment
  grid path (space before `:`) is unchanged.

  **Test result:** `make test` 239/239 forward + 239/239 idempotency (was
  238/238 before this fixture was added) — zero regressions, fix kept (not
  reverted).

### `lodash/lodash` dogfood pass — DONE

Repo: `lodash/lodash` (`/tmp/lodash`, HEAD `a666ba5`, v4.18.1). Single large
`lodash.js` (17259 lines) plus build tooling/tests. In-scope corpus: 27 real
`.js` files, 50983 total lines (`dist/*`/`vendor/*` excluded per file-
exclusion convention).

**Baseline:** `node --check` 27/27 pass. **Round1:** zero crashes, 27/27
formatted, `node --check` 27/27 pass. **Idempotency:** 26/27 byte-identical;
`lodash.js` differs — the same switch-case-fallthrough shape as the
`vuejs/core` `utils.ts` issue above (second confirming data point, not
re-fixed here — see "Known open issues").

**Content-preservation** (`js_ts_content_diff.js`, original vs. round1):
initially 17/27 "MISMATCH", all decomposing into two intentional, non-lossy
transformations the checker didn't yet tolerate (comment trailing-period
stripping; §10 single-expression-block brace omission) — both became checker
tolerances (see Dogfood Output Validation above). After checker improvement
(follow-up session): **22/27 clean** (up from 10/27 pre-first-round of
tolerances). Remaining 5 files are two further confirmed-intentional,
non-lossy classes, left unfixed (checker gap, not formatter bug, low
priority TODO): bare single-param arrows gaining parens (3 files, documented
§6 behavior); STYLE.md §4 pre-increment-except-when-post-required correctly
rewriting a standalone/unused for-loop increment (2 files,
`perf/perf.js`/`test/test.js`).

**Verdict: DONE.** Zero new formatter bugs found. The one idempotency diff
is a confirming recurrence of the already-tracked `SwitchRule` issue.

### `angular/angular` dogfood pass — clusters 1-3 FIXED, cluster 4 PARTIALLY FIXED (all 4 named root causes now landed; residual files exist outside the 4 named causes — see 2026-07-31 session below), cluster 5 RESOLVED (2026-08-05 — all 3/3 files now idempotent: `emit.ts` via single-pass GDR (2026-08-02), `user_metric_spec.ts`/`i18n_parse.ts` via `curly-general-scope-reindent-multipass` (landed 2026-08-03 in `STATE_CURLY_GDR.md` for `RDD_KEY_229`, re-validated fresh against the live corpus this session); see below — this is a per-corpus dogfood-recommendation note, `curly-general-scope-reindent`/`-multipass` both stay `off` by default project-wide)

Repo: `/tmp/angular`, shallow clone (`--depth 1`), HEAD `5ad8231`
(2026-07-24). Scope: 5394 `.ts` files (`.d.ts`/`.tsx` excluded) across
`packages/`, `adev/`, `devtools/`, `integration/`, `modules/`,
`vscode-ng-language-service/`, `dev-app/`, `tools/`. Formatted in 8 batches
(one per top-level dir), round1 then round2. Syntax check: TS compiler-API
parse-only (no type-check).

Stats: 0 crashes/5394 files; 29 idempotency mismatches; parse-check baseline
(unformatted) 0/5394 errors; round1 output **46/5394** files with parse
errors (339 diagnostic lines) — 46 real formatter-induced syntax
corruptions despite zero hard crashes. Clusters ranked most-valuable-to-fix
first (value = criticality weighed against difficulty):

1. **[CRITICAL] [FIXED]** Dotted/qualified type-predicate or return-type
   before `=>` gets its last segment wrapped in a spurious paren pair —
   dominant corruption cluster, **~40 of 46 broken files** (e.g. `node is
   tss.Node =>` → `node is tss.(Node) =>`). Root cause:
   `enforceArrowFunctionParameterParens`'s backward scan for a bare
   single-identifier param didn't check whether that identifier is the tail
   of a preceding **dotted** type-predicate/return-type (existing bail-out
   only special-cased `is`/`typeof`/`keyof` as immediate `prevPrev`, not a
   multi-segment dotted path). **Fix:** walk backward over any number of
   `IDENTIFIER '.'` pairs before `prevIdx`, then apply existing bail-out
   against what precedes the chain's first segment. Fixture
   `real_code_regressions_134`. `make test`: 183/183.
2. **[CRITICAL] [FIXED]** Old-style angle-bracket cast (`<Type>{...}`)
   misparsed as a generic, injecting a bogus `;` inside the following object
   literal — 1 file (`testability.ts:229`). Root cause one level downstream
   of `reclassifyAngleBrackets` (which correctly leaves a cast's `<`/`>` as
   plain OP): nothing downstream recognized the plain-OP `<Type>{` shape as
   a cast, so the object literal fell through `classifyBraces`'s
   default-false `isValue` case and got depth-reset as a statement body.
   **Fix:** new `isLegacyCastBrace` — a `{` preceded by a plain `>` whose
   matching plain `<` sits before a (optionally dotted) type name following
   a value-starting token is now treated as a value/pattern brace. Fixture
   `real_code_regressions_135`. `make test`: 184/184.
3. **[CRITICAL] [FIXED]** Multi-line generic return-type clause loses its
   closing `>`, spilling a bogus `;` into the type — 1 file
   (`utils.ts:103-105`, `Promise<\n (typeof import(...))['default'] |
   null\n>`). Root cause: TS dynamic-import type-query operand (`import` as
   a type-operand keyword) wasn't in `GENERIC_SAFE_KEYWORDS`, invalidating
   the enclosing `<...>` tracking — same gap class as existing
   `keyof`/`is`/`infer`/etc. entries. **Fix:** add `"import"` to
   `GENERIC_SAFE_KEYWORDS`. Fixture `real_code_regressions_136`. `make
   test`: 185/185.
4. **[PARTIALLY FIXED — ACTIVE WORK] Call-wrap/collapse vs. alignment-padding
   fits-check ordering** — dominant idempotency cluster, **~23 of 29 files**
   (`create_router_state.ts:27`, `node_selector_matcher.ts:155`,
   `ingest.ts:814`, `locale_plugin.ts:42`, `hover.ts:58`, plus ~18 more
   across `compiler-cli`, `schematics`, `common/i18n`, `location_shim.ts`,
   `split.component.ts`, `web_animations_player_spec.ts`, `adev/.../app.ts`).
   Root cause family: `enforceCallLineBreaking`'s single-argument fits-check
   measures candidate line length **before** declaration-alignment/
   complexity-padding finish adjusting column widths, flip-flopping every
   round. Confirmed broad (23/29, majority) and config-insensitive at
   `indent-size=2`.

   **Two of at least three/four distinct root causes are FIXED:**
   - **Root cause #1 — trailing-comma dangling-empty-group** (confirmed via
     `create_router_state.ts:27`): `renderCallDropped`/`renderCallOnePerLine`
     measured via `splitTopLevelCommas`, which — unlike `groupByOriginalLine`
     — doesn't drop a dangling trailing empty group from a trailing comma
     before `)`. Fixed by adding the same drop to both methods. Fixture
     `real_code_regressions_140`.
   - **Root cause #2 — `if (`/`if(` keyword-spacing pipeline ordering**
     (confirmed via `node_selector_matcher.ts:155`, `locale_plugin.ts:42`):
     the fits-check for a call inside `if (...)` measured the line before
     `enforceKeywordSpacing` (collapses `if (` → `if(`) had run (it ran in
     Phase 4, after the fits-check) — one char narrower on reformat,
     flipping the boundary (confirmed exact 101-vs-100). Fixed by pulling
     `enforceKeywordSpacing` forward to run immediately before the first
     `enforceCallLineBreaking` call. Applies to all curly-brace languages
     (shared `MiscRuleCore`), full `make test` re-run confirmed no
     regressions. Fixture `real_code_regressions_141`. `make test` after
     both fixes: 190/190.
   - Spot-check of 8 originally-cited files after both fixes: 6 now
     idempotent, 2 still broken via further distinct root causes:

   - **Root cause #3 — ATTEMPTED AND REVERTED (too many regressions), then
     redesigned and landed** — **braceless-else body never re-validated
     after brace-collapse/alignment** (`format_date.ts:519`):
     `collapseSingleExpressionBlocks` strips `if`/`else` braces in Phase 0,
     before `enforceCallLineBreaking` (Phase 1) — the braced source used a
     `+`-chain complexity-wrap to fit, which doesn't apply to the
     now-braceless body, leaving the joined line over the limit;
     `alignBracelessElseIfChain` pads it anyway (intentional escape hatch,
     `BlockStructureRule.java`, not a bug there). Rounds diverge. **First
     attempt (reverted):** refuse to collapse (`tryCollapse`) whenever the
     joined one-liner exceeds `lineLengthLimit` — **DO NOT retry this naive
     approach**: no way to know `enforceCallLineBreaking` will still wrap an
     inner call and make it fit, so it wrongly re-braced every braceless
     if/else with a wrappable-call body — broke 5 fixtures (`java_combined`,
     `real_code_regressions_57`/`_81`/`_93`/`_141`). Real fix needs the
     guard to simulate `enforceCallLineBreaking`'s wrap decision on the
     joined candidate first (two-pass lookahead) — bigger lift.

     **Design (2026-07-30, landed 2026-07-31 — tracker item 12):** cheaper
     than a true two-pass simulation; reuses existing precedent
     (`JavaSpecificRule.isSingleLineBody`, `KotlinSpecificRule`'s analogous
     method, `GetterSetterRuleCurly.parseOneLinerMember`'s length pre-check
     — all solve the same "will `enforceCallLineBreaking` still wrap this
     later" problem via a cheap heuristic, not a real simulation): (1)
     `hasBreakableCall(tokens, from, to)` — true iff the span contains a
     `name(args)` call with a non-empty argument list (the only shape ever
     wrapped); (2) a raw-width estimate
     (`expandedIndentWidth(lineIndent(...))` + collapsed-whitespace text
     length, matching `enforceCallLineBreaking`'s own measurement) compared
     against `lineLengthLimit`. Key insight: refuse collapse only when
     over-limit **and** `hasBreakableCall` is false — if a breakable call
     exists, collapsing is still safe since `enforceCallLineBreaking` will
     wrap later and both rounds predict the same outcome.

     **Implemented** in `BlockStructureRule.java`: private
     `refuseUnrescuableCollapse` (alongside local
     `expandedIndentWidth`/`hasBreakableCall`/`nextSignificantIndexLocal`/
     `matchParenForwardLocal` — this class has no shared ancestor with the
     `*Curly` hierarchy, so all four are duplicated copies per the
     established per-class convention, not new shared extractions). Called
     from `tryCollapse` (after existing brace-content guards, right before
     its final `return`) and from `collapseBracelessBody` (shared core both
     `tryCollapseBraceless` and the bare-`else` collapse path route through
     — gained a new leading `indentAnchorIdx` parameter, the keyword token
     index, threaded from both call sites). Gate: `(lang.isJs ||
     lang.isTs)` only; computes the joined candidate's true rendered width;
     if under `lineLengthLimit`, no gate. If over, refuses (returns `null`,
     leaving the braced/multi-line form untouched) only when
     `hasBreakableCall` finds no rescuable call.

     **Deviation from original wording, found necessary:** the design said
     scan "the candidate's body span"; the actual scan covers the *whole*
     candidate (condition/prefix AND body) — a body-only scan broke the
     already-passing `real_code_regressions_141` fixture, where the
     zero-arg body call has nothing to wrap but the *condition*'s own call
     is what `enforceCallLineBreaking` wraps to rescue the line (root
     cause #2's own fix). Widening the scan doesn't reopen the reverted
     attempt's failure mode, since that attempt had no `hasBreakableCall`
     gate at all.

     **Follow-up: third insertion point found and fixed while building the
     permanent fixture.** The design named two insertion points
     (`tryCollapse`/`tryCollapseBraceless`), but a third call site — the
     bare-terminal `else { ... }` chain-collapse path inside
     `collapseSingleExpressionBlocks` itself (gated by
     `chainAllBranchesCollapsible`) — builds its collapsed candidate inline
     and routed through neither, so it had no gate until this follow-up.
     Found while constructing a minimal "unrescuable, should refuse" repro:
     refusal never took effect for a plain `if (...) { ... } else {
     longNoCallBody }` shape. Fixed by adding the same
     `refuseUnrescuableCollapse` call at that site too (falls through to
     the untouched default when refused, same posture as every other
     guard in this class).

     **Test results:** `make test` 221/221 forward + 221/221 idempotency
     (grew from 196/196 at the top of this file partly from intervening
     sessions/other jobs' fixtures, partly this session's new fixture
     `real_code_regressions_172` — see below).

     **Real-corpus validation:** all 5 originally-cited `angular/angular`
     files (`create_router_state.ts`, `node_selector_matcher.ts`,
     `ingest.ts`, `locale_plugin.ts`, `hover.ts`) confirmed individually
     idempotent now (were not, before this fix). `location_shim.ts` (root
     cause #4's file) and `split.component.ts` spot-checked, still
     idempotent (no regression). Full `packages/` re-scan (3900
     `.ts`/non-`.d.ts`/non-`.tsx` files, round1→round2): **12 files still
     differ**, down from the ~23 originally cited across the whole
     5394-file corpus (packages/ is the majority of that count, not a
     strictly apples-to-apples full re-run, but the direction/magnitude
     confirm real reduction). Of the 12: 3 are the already-catalogued,
     unrelated, accepted cluster 5 architectural gap (`user_metric_spec.ts`,
     `emit.ts`, `i18n_parse.ts` — pre-existing inconsistent-source
     reindentation, untouched by this session). The remaining 9
     (`format_date.ts`, `web_animations_player_spec.ts`, `parser.ts`,
     `jit_compiler_facade.ts`, `r3_template_transform.ts`, `util.ts`,
     `node_js_file_system.ts`, `input_transform.ts`, `migration.ts`) are
     **not fixed** by this session — see "known residual limitation"
     immediately below.

     **Known residual limitation (not a regression, a real gap in the
     heuristic's coverage, confirmed via `format_date.ts:519` and
     `checker.ts:16487` in the TypeScript corpus below):** `hasBreakableCall`
     only asks "does a rescuable call exist", not "will wrapping it actually
     bring the line under `lineLengthLimit`". When a collapsed candidate is
     long enough that wrapping the one breakable call's arguments doesn't
     shrink the joined line far enough (e.g. a long `+`-concatenation chain
     with two `padNumber(...)` calls, only one of which — or neither, if the
     other operands alone already exceed the limit — would be wrapped), the
     gate still allows the collapse (a rescuable call exists), but the real
     `enforceCallLineBreaking` pass either doesn't wrap it (still doesn't fit
     enough to trigger) or wraps it and still leaves the line long — round1
     and round2 can then genuinely disagree the same way they did before this
     fix. This is the gap the original design already flagged as the reason a
     true two-pass simulation would be needed for full coverage ("bigger
     lift, deferred") — `hasBreakableCall` was always a cheap approximation,
     not a guarantee, and this session's validation is the first real
     evidence of where the approximation's coverage ends. No fixture was
     added for this specific residual case (the existing behavior pre- and
     post-fix is identical for these specific files -- the fix is a strict
     improvement, never a regression, so there is no *new* bug shape to pin
     with a fixture). The fix's own correctness (both the "rescuable,
     collapse proceeds" and "unrescuable, collapse refused" branches of
     `refuseUnrescuableCollapse`) is now covered by the new permanent
     fixture `test/real_code_regressions_172_{inp,out}.ts` (registered in
     the Makefile's `INP_FILES` and `test/README.txt`, modeled on
     `checkAttrs`/`checkFlag` shapes derived from the angular real-code
     validation above), plus the pre-existing `real_code_regressions_57`/
     `_81`/`_93`/`_141` fixtures (regression coverage for the reverted
     naive attempt).

     **TypeScript corpus (`/tmp/ts-dogfood/TypeScript`, 601 files) re-run:**
     round1→round2 now shows 29 differing files (previously 28/601 for
     cluster #3 before this fix). Not a regression — spot-checking
     `commandLineParser.ts` (named in the original cluster #3 write-up as
     reproducing config-insensitively) shows its diff is an
     object-literal/declaration-alignment column-width shift, not a
     braceless if/else collapse at all — a sibling root cause in the same
     "call-wrap vs. column-width-adjusting-pass ordering" family this
     session's fix was never scoped to touch. `checker.ts` in this same
     corpus *does* show the exact root-cause-#3 shape (a braceless
     `if(...)  <huge-gap>  lateBindMember(...)`, same as `format_date.ts`)
     and is the other confirmed instance of the residual limitation above.
     **Conclusion:** this session's fix is confirmed correctly targeted and
     effective for its specific root cause, but the TypeScript corpus's
     cluster #3 count barely moved because most of its 28 files are a
     different, not-yet-root-caused sibling issue (declaration/class-field-
     alignment-grid vs. call-wrap ordering) rather than the
     braceless-collapse shape — narrower than the original "same root cause
     family" framing assumed. Left open, out of scope for tracker item 12
     as scoped — would need its own root-cause identification pass.

   - **Root cause #4 [FIXED] — trailing same-line comment inconsistently
     counted in the collapse fits-check** (`location_shim.ts:461`): fresh
     format counts the trailing comment's width in the fits-check (over
     limit once `=`-alignment padding widens the column) → wraps; once
     wrapped the comment moves past the call's `)`, so reformat measures
     without it → collapses back. **Fixed** via
     `appendRangeCollapsingTrailingCommentGap` (`MiscRuleCurly.java`):
     whitespace before a trailing line comment collapses to one space for
     measurement only (never rendered), used only in the JS/TS
     tight-candidate fits-check's `suffix`. Fixture
     `real_code_regressions_142`. `make test`: 191/191. A related 3-sibling
     `=`-alignment-group non-self-stability quirk was seen while building
     the fixture but did NOT reproduce against real `location_shim.ts`
     (confirmed idempotent there) — not investigated further, flag if a
     future dogfood run hits it.

   `compiler-cli/src/ngtsc/core/{compiler,host}.ts` and
   `devtools/.../split.component.ts` were not re-checked (missing from the
   checkout at re-check time). **This cluster stays OPEN** until root cause
   #3 is fixed and a full re-run across all ~23 originally-cited files
   confirms full resolution, or further causes are found. **This is the
   same root-cause family as `microsoft/TypeScript`'s open "Category 2
   cluster #3" below — treat any future work on either as the same task.**
5. **[IDEMPOTENCY] Reindentation on internally-inconsistent source —
   RESOLVED 2026-08-05, opt-in dogfood recommendation for this corpus** — 3
   files (`user_metric_spec.ts:88`, `emit.ts:104`, `i18n_parse.ts:520`): a lone
   closing `}`'s indent (2 vs 4 spaces) differs between rounds because the
   *original* source itself has genuinely inconsistent brace indentation
   (mixed 2-/4-space blocks in the same function), and this formatter's
   indentation model is relative-delta-from-one-reference-line, not
   absolute-depth-derived — the exact architectural gap documented in
   `STATE_COMMON.md`'s "General scope-depth reindentation" section (prior
   instances: `javaparser`'s `ASTParser.java`, local `JSONEncoderLite.java`).
   Lowest priority: low criticality, architecturally hard, explicitly scoped
   as its own future dedicated high-risk job in `STATE_COMMON.md` — do not
   attempt piecemeal. **2026-07-28 re-assessment:** unchanged, still out of
   scope for any housekeeping pass.

   **2026-08-02 re-assessment (`RDD_KEY_229`), after the GDR job
   (`STATE_CURLY_GDR.md`) landed its opt-in pre-pass and expanded scope to
   JS/TS (`RDD_KEY_228`):** re-ran all 3 files with
   `curly-general-scope-reindent = on`. `emit.ts` is now idempotent —
   GDR's absolute structural-depth indent correctly overrides the
   inconsistent source indentation for that file's shape. `user_metric_spec.ts`
   and `i18n_parse.ts` are still NOT idempotent even with GDR on, but for a
   different reason than the original architectural gap: both contain
   one-true-brace-style joins (`} else if (...) {`) that this formatter's
   own brace-placement pass splits into separate `}`/`else if (...) {`
   lines — GDR computes its indent target before that split happens, so
   the split-out line has no GDR target of its own and can render wrong. A
   candidate fix (run GDR after brace-placement instead of before) was
   confirmed to fix this specific case but introduces a different
   non-idempotency (indentation change flips the pipeline's own line-wrap
   decisions on the next round) — see `RDD_KEY_229` and
   `STATE_CURLY_GDR.md`'s checklist for the full investigation. **User
   judged both remediation paths too risky to attempt this session — no
   code change landed.** No new fixture added. (Historical note — see the
   2026-08-05 update below: this was superseded by a different, safer fix
   landed in the GDR job, not either of the two paths flagged risky here.)

   **2026-08-05 update (this session):** `STATE_CURLY_GDR.md` records that
   on 2026-08-03 (separate GDR-job session, `curly-general-scope-reindent-
   multipass` design, `RDD_KEY_233`/`RDD_KEY_234`) a **third**, safer
   remediation path was designed and landed — a bounded 4-stage
   GDR/pipeline/GDR/pipeline sequence, opt-in behind a second flag
   (`curly-general-scope-reindent-multipass`, only takes effect when
   `curly-general-scope-reindent` is also on) — distinct from the two paths
   (bounded fixpoint iteration; feeding GDR's indent into the wrap
   fits-check) the user judged too risky above. That session's own
   validation already re-tested `user_metric_spec.ts`/`i18n_parse.ts`
   against this exact corpus and reported both fully idempotent under
   multipass. This session re-confirmed it independently, fresh against the
   live `/tmp/angular` checkout (all three cluster-5 files, in-file
   `JXM_CFMT_CFG curly-general-scope-reindent=on;
   curly-general-scope-reindent-multipass=on`, round1→round2): **all 3 of 3
   files now produce a zero-line diff** (`user_metric_spec.ts`,
   `i18n_parse.ts`, and `emit.ts`, which was already passing under
   single-pass GDR and stays passing under multipass — no regression). All
   3 round1 outputs pass `tools/verifiers/js_ts_syntax_check.sh` (exit 0).
   **Cluster 5 is now closed: 3 of 3 files fixable, opt-in, via existing
   flags — no new source code changed this session.** Per
   `STATE_CURLY_GDR.md`'s own scoping notes, `curly-general-scope-reindent`/
   `curly-general-scope-reindent-multipass` remain `off` by default
   project-wide (the `on` path is still explicitly flagged there as a hard,
   multi-session, not-fully-mature problem in general — this is a
   per-corpus dogfood recommendation for `angular/angular`-shaped
   one-true-brace-style source, not a project-wide default change). No new
   permanent fixture added for this specific finding — the multipass
   mechanism itself is already covered by the existing
   `test/curly_gdr_multipass_inp.java`/`_out.java` fixture (Java, but
   exercises the same shared `GdrPipelineGate` 4-stage code path used for
   `.ts`/`.js`) plus `RDD_KEY_229`'s and this note's own real-corpus
   evidence; a `.ts`-specific duplicate of the same mechanism was judged
   redundant. `make test`: 244/244 forward + idempotency (unaffected,
   doc-only change this session).

Next free fixture number unaffected by cluster 4/5 doc update (no new
fixtures this session). Full corpus re-run deferred until cluster 4 root
cause #3 lands, same pattern as `vuejs/core`/`lodash/lodash`.

## `microsoft/TypeScript` dogfood pass — 3 of 4 clusters FIXED; cluster #3 PARTIALLY addressed 2026-07-31 (see angular cluster 4's root-cause-#3 session write-up — the braceless-collapse shape is fixed, but most of this corpus's 28 files turned out to be a different, not-yet-root-caused sibling issue in the same family; count barely moved, 28→29)

Checkout: `/tmp/ts-dogfood/TypeScript`, shallow clone (`--depth 1`), HEAD
`cc5c6e2` (2026-07-28) — reuse, do not re-clone. Scope: `src/` only, 601 real
`.ts` files (108 `.d.ts` excluded), 379045 lines; `.tsx` excluded (JSX/TSX
out of scope); `tests/cases/**`/`tests/baselines/**` excluded (hand-authored/
generated compiler test fixtures, not real source).

Round1: zero crashes, 601/601 produced. Syntax-checked via a throwaway
TS-compiler-API parse-only script (no `js_ts_syntax_check.js` exists in
`tools/verifiers` yet). Baseline (unformatted) 0/601 parse errors.
Round1→round2 idempotency: 30/601 differing (28 are cluster #3 below; 2 are
symptoms of Category 1 bugs, not independent findings).

### Category 1 — Critical (round1 corrupt/unsafe): 8/601 files, 85 diagnostics, 3 root causes — ALL FIXED (2026-07-28)

1. **`||=`/`&&=` not tokenized as a single token** — 1 file (`checker.ts`),
   3 occurrences. `MULTI_CHAR_OPS` had `??=` but was missing `||=`/`&&=`
   even though `JsTsSpecificRule.java` already referenced them elsewhere.
   Fix: added both, ordered before `&&`/`||` (longest-match-first). Fixture
   `real_code_regressions_143`. `make test`: 192/192.
2. **Union-type return-type/type-predicate before `=>` gets its last segment
   wrapped in a spurious paren pair** — 6 files, ~9 occurrences (e.g.
   `declaration is AccessorDeclaration | PropertyDeclaration =>` →
   `... | (PropertyDeclaration) =>`). Same function as angular cluster 1's
   fix (dotted-chain walk-back), but that fix didn't walk back over a union
   `|` operator. Fix: walk-back loop now alternates an `IDENTIFIER '.'`
   walk-back with an `IDENTIFIER '|'` walk-back until neither makes
   progress, covering chained and dotted unions. Fixture
   `real_code_regressions_145`. `make test`: 194/194.
3. **Backslash-newline continuation inside a plain string literal,
   CRLF-specific, corrupts the rest of the string/statement** — 2 files
   (both CRLF). `TokenizerCurly.emitString`'s backslash-escape handling only
   consumed the backslash + the `\r` half of `\r\n`, leaving `\n` next, which
   the unescaped-newline check mistook for string termination. LF-only
   continuation already worked. Fix: special-case `\` + `\r\n` to advance 3
   chars. Fixture `real_code_regressions_147` — **`.gitattributes` marks
   `test/real_code_regressions_147_inp.ts` `-text`** so git preserves its
   deliberate CRLF bytes; a future session touching this fixture must
   preserve that. `make test`: 196/196.

### Category 2 — Idempotency-only: 28/601 files (2 more are Category-1 symptoms)

**Cluster #3 — call-wrap/collapse vs. alignment-padding fits-check ordering.
NOT FIXED — deliberately deferred by user decision, ACTIVE / OPEN WORK:**
**SAME root cause as the still-open `angular/angular` cluster 4** above
(`enforceCallLineBreaking`'s single-argument fits-check measuring candidate
width before declaration-alignment/keyword-spacing/complexity-padding finish
adjusting column widths) — a third confirming recurrence, proportionally
~30x denser here (28/601 vs 23/5394 in angular), likely because this repo's
line lengths sit close to the 100-char boundary often. Confirmed
config-insensitive (`indent-size=2` on `commandLineParser.ts` reproduces
identically). Spot-checked ~10/28: every diff is a call wrapped/collapsed
between rounds, or a closer moving to its own line. Angular cluster 4's
root causes #1/#2 (dangling-empty-group measurement, `if(`/`if (` spacing
ordering) are already fixed here too; this run doesn't isolate which
remaining cause (#3 braceless-else, or others) applies — same underlying
architectural fix needed either way; angular's naive `tryCollapse` guard
attempt was reverted (5-fixture regressions). **Treat this TS corpus as
further confirming evidence for angular cluster 4, not a separate task.**
No fixture registered (not fixed).

### Ranked list (most-valuable-first)

1. `||=`/`&&=` tokenizer gap — FIXED. Trivial fix, highest value/difficulty.
2. Union-type-before-`=>` spurious wrap — FIXED. Direct low-risk extension
   of an existing precedent.
3. Call-wrap/collapse vs. alignment-padding ordering — NOT FIXED, deferred
   (see above). Highest file-count value (28/601) but same medium-high-
   difficulty cross-pass-ordering fix already scoped under angular cluster 4.
4. Backslash-newline CRLF string corruption — FIXED. Real corruption but
   narrow (2 files, old test-harness idiom).

No fixture-only false positives found this pass (used direct TS-compiler-API
parse-checking + raw `diff`, not `js_ts_content_diff.js`).

### Known false positives (no source change needed, fixture-only)

- Spurious-looking blank line after a class's opening `{` in older `.js`
  fixture drafts, and doubled trailing space before a one-liner getter
  body's closing `}` — both confirmed correct, existing behavior (STYLE.md
  §7 named-construct blank line; `GetterSetterRuleCurly`'s group-width body
  padding), matching passing C++/Java/Kotlin fixtures byte-for-byte. Only
  the stale hand-authored `.js` draft fixtures were wrong; resolved by
  regenerating them.
</content>
