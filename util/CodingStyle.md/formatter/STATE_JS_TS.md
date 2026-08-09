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
| RDD_KEY_274 | 2026-08-09 `microsoft/TypeScript` dogfood-reconfirmation Tier-3 shape #2 (class-field `:`-type-annotation column-alignment group splitting apart on round2) — `server/editorServices.ts`/`server/project.ts`, FIXED: a same-line leading comment (`/** @internal */ readonly x: T;`) forces its own line when `JsTsSpecificRule.flushClassFieldGroup` renders a group, adding a NEWLINE the source never had; `blankLineBetween`'s old "count total NEWLINEs in the gap, blank if >= 2" logic then miscounted that forced line break as a genuine blank line on round2 (fed round1's own already-comment-on-its-own-line output), splitting a group round1 had correctly kept joined. Fixed by requiring the two NEWLINEs be strictly back-to-back (only WHITESPACE allowed between, a COMMENT resets the count) to count as a real blank line. New fixture: `test/real_code_regressions_196_{inp,out}.ts` — not CRLF-specific, a distinct root cause from RDD_KEY_273 despite sharing the same flagged-file corpus |
| RDD_KEY_275 | 2026-08-09 `microsoft/TypeScript` dogfood-reconfirmation Tier-3 shape #4 (closing `}` non-idempotently gaining a stale `// if` trailing comment on round2) — `services/codefixes/fixMissingTypeAnnotationOnExports.ts`, FIXED as a verified side effect of RDD_KEY_273 (no separate code change): A/B bisection on a 100-line real-file excerpt (lines 1050-1150) proved reverting only RDD_KEY_273's `alignBracelessElseIfChain` fix reproduces this bug and restoring it resolves it, though the full causal chain into `BlockStructureRule.decideComment`/`countContentLines`'s NEWLINE-count threshold was not separately hand-traced. New fixture: `test/real_code_regressions_197_{inp,out}.ts` (CRLF-encoded excerpt, `.gitattributes`-marked `-text`) — exists purely to lock in this second symptom's coverage |
| RDD_KEY_276 | 2026-08-09 `microsoft/TypeScript` dogfood-reconfirmation Tier-3 shape #3 (an interface's intersection-type closing brace `};` shifting from column 0 to indented on round2) — `compiler/types.ts`, FIXED: a field literally named `class` (`readonly class: ExpressionWithTypeArguments & { readonly expression: ...; };`, a legal TS property name) made `JsTsSpecificRule.classBraceKind`'s backward KEYWORD-text scan misclassify the field's own nested inline object-type-literal `{...}` brace as a class-declaration body, feeding it into `enforceClassFieldAlignmentGrid`'s class-field grid rewrite and corrupting/destabilizing the nested brace's formatting. Fixed by adding `isFieldNameKeywordUsage` — `classBraceKind` now skips (keeps walking backward) a candidate `class`/`interface` KEYWORD token immediately followed by `:`/`?` (field-name usage) instead of an identifier (declared name). New fixture: `test/real_code_regressions_198_{inp,out}.ts` (plain LF, distinct root cause from RDD_KEY_273/274/275's cluster) |

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

## Related investigation history — same architectural family as bugs #1-#3

RDD_KEY_245/246/249 (all rejected/no-fix, superseded by later fixes) probed
the same architectural family as bugs #1-#3 below: "which pass gets to see
the final, stable per-line/per-column width." Kept as a compact locus/
hypothesis index — do not re-derive from scratch.

- **RDD_KEY_245** — first pass at the `commandLineParser.ts` decl-alignment
  idempotency bug; ruled out `JsTsDeclarationAlignmentRule.spansMultipleLines`'s
  bracket-depth bail; wrongly suspected `MiscRuleCurly.enforceCallLineBreaking`/
  `renderCallCandidate`'s multi-line closing-bracket placement (RDD_KEY_246
  later proved that locus structurally impossible). Real loci found since:
  `applyOversizedAggregateInitClosingBracePass` (RDD_KEY_246/248) and
  `applyAssignmentsPass` (bug #2).
- **RDD_KEY_246** (2 attempts reverted) — pinned the root cause:
  `applyOversizedAggregateInitClosingBracePass` decides `}` placement from
  whether the aggregate initializer already contains an embedded NEWLINE,
  which is stale pre- vs. post-call-wrap. Attempt 1 (re-run the pass alone)
  fixed the symptom but exposed the same staleness in
  `JsTsDeclarationAlignmentRule.spansMultipleLines`'s grouping decision.
  Attempt 2 (full `ScopePipelineCurly.process` re-run) fixed the repro but
  regressed `real_code_regressions_100.ts` (collapsed an already-correct
  `} // interface ParserOptions`) plus several Java/Kotlin/cpp26 fixtures —
  reverted. Lesson carried into RDD_KEY_248: an unconditional full
  `processScope` re-run is unsafe; the eventual fix re-ran only two of the
  five passes. (Also flagged a general pitfall: redirect long `make test`
  runs to a log and `grep -n "^FAIL"` it — live terminal capture can
  silently truncate output on a suite this size.)
- **RDD_KEY_248 (FIXED)** — landed the untried narrow middle ground:
  `ScopePipelineCurly.reapplyClosingBraceAndDeclarationsPass` re-runs
  `applyOversizedAggregateInitClosingBracePass` + `applyDeclarationsPass` a
  second time. First cut reproduced Attempt 2's `real_code_regressions_100.ts`
  regression via `processScope`'s shared trailing-gap force-reindent step
  (re-derives indentation from already-reformatted text on the narrow
  re-run); fixed by skipping that step in narrow-rerun mode.
- **RDD_KEY_249** (reverted, later refuted/redirected) — repro
  `/tmp/mini2.ts` (two `padNumber(...)` calls): round1's rejoin fits-check
  measures only the local physical line, producing an inconsistent partial
  rejoin on round2. A blanket statement-wide fits-check fixed `mini2.ts` but
  regressed `real_code_regressions_81`/`_93` (whose over-limit rejoins are
  legitimately stable) — reverted; same trap that later sank the first
  `hasBreakableCall` width-gate attempt for angular cluster 4 root cause #3.
  Follow-up instrumentation refuted a "sibling-wrap-visibility" hypothesis
  and found the real mechanism: `BlockStructureRule.alignBracelessElseIfChain`
  runs after `enforceCallLineBreaking` and pads a short `else` to align with
  its `if`; round1's rejoin-check measures the `else` prefix before that
  padding lands, so a candidate wrongly rejoins. This is a distinct
  interaction from bug #1's `alignBracelessElseIfChain` issue, and is what
  RDD_KEY_250 fixed.
- **RDD_KEY_250 (FIXED)** — right after `alignBracelessElseIfChain` in
  `FormatterCurly.format`, re-run `enforceCallLineBreaking` twice + one
  `enforceComplexityPadding` (same fix shape as RDD_KEY_248; a single
  re-run call was insufficient to reach a fixed point). `real_code_regressions_81`/
  `_93` re-verified unaffected (no braceless-else chain in either).

---

## Active work — all 3 originally-tracked bugs FIXED (RDD_KEY_269/270/271)

No open bug remains from this investigation. Full session-by-session
narrative (every dead end) lives in `git log` for this file; kept here as a
compact per-bug record (symptom/root cause/fix/verification).

### 1. `angular/angular` cluster 4 residue — `shared.ts`/`directive_outputs.ts` (FIXED, RDD_KEY_269)

Symptom: a bare `if(cond) stmt; else stmt;` (`packages/core/.../shared.ts:793-796`,
`directive_outputs.ts`) round-trips with `else` correctly 2sp-aligned on
round1 but re-indented to 4sp and unaligned on round2. Root cause:
`alignBracelessElseIfChain` detects an if/else chain by requiring the
`if`/`else` lines' raw indent to match exactly, with a recovery case only
for "the `if` line was left-padded wider than `else`" — on round2, some
earlier pass (never individually identified) leaves the standalone `else`
line indented one level *deeper* than its `if` instead, the opposite
direction, which the existing recovery didn't handle, so the chain-scan
breaks and no alignment is applied. Confirmed distinct from the
`processScope` double-pass family (#2/#3): this is a single indentation
value, not a grouping/padding decision, and occurs before
`alignBracelessElseIfChain` runs (a single-pass method). Fix: widened the
chain-recovery case to also strip excess indent when `jIndent > indentLen`
for a bare-`else` member (chain size 1 only), mirroring the existing
narrower-`if` recovery in the opposite direction. Verified: both files
individually idempotent; full `angular/angular` corpus (5394 `.ts`)
idempotency-violation count went 9→7, the 2 newly-fixed files exactly
`shared.ts`/`directive_outputs.ts`, zero regressions elsewhere (an initial
raw round1-output diff showed 178 differing files, traced to stale GRU
classifier weights in the comparison jar, not this fix, ruled out via 3
independent checks). `make test` 258/258 → 259/259. New fixture:
`test/real_code_regressions_184_{inp,out}.ts`. Kotlin (shares
`BlockStructureRule`/`KotlinSpecificRule.alignBracelessElseIfChain`)
re-verified only via `make test`, no dedicated Kotlin corpus re-run.

Status: **FIXED.**

### 2. `microsoft/TypeScript` cluster #3 — `harness/collectionsImpl.ts` (FIXED, RDD_KEY_270)

Symptom: `harness/collectionsImpl.ts:276` (`this._size = -1;`) is
wide-padded on round1, unpadded on round2 — same "call-wrap vs.
alignment-padding decided too early" shape RDD_KEY_248 already fixed for
`applyDeclarationsPass`, but this time in `ScopePipelineCurly.applyAssignmentsPass`,
a pass RDD_KEY_248's own javadoc had explicitly excluded as "not shown to
depend on post-call-wrap shape" — this investigation disproved that for at
least this shape. Debug prints confirmed: round1 groups the wide
map-assignment line with the narrow `this._size` line (padding it); round2
(fed round1's own wrapped output) drops the map row from that group, so
`_size` gets no padding (why the row drops out on round2 specifically
wasn't separately traced). Fix (direct extension of RDD_KEY_248):
`applyAssignmentsPass` added as a third pass inside `processScope`'s
`closingBraceAndDeclarationsOnly` re-run mode, after the closing-brace and
declarations passes. The RDD_KEY_246 failure mode (shared trailing-gap
force-reindent step) was checked by inspection — that step is gated by one
boolean untied to which passes run inside the branch, so it's unaffected.
Validated via A/B rebuild (repro obtained via direct `curl` of the file
from GitHub, no full clone) rather than corpus dogfood. `make test`
259/259 → 260/260. New fixture: `test/real_code_regressions_185_{inp,out}.ts`.
Known unrelated pre-existing artifact: `js_ts_content_diff.js` flags this
file's top-level `interface`/`class` headers as MISMATCH both before and
after the fix — a content-diff tool gap, not this bug.

Status: **FIXED.**

### 3. `angular/angular` cluster 4 residue group #3 — `web_animations_player_spec.ts`, `input_transform.ts` (FIXED, RDD_KEY_271)

Same family as #1/#2 but two distinct mechanisms, both outside
`ScopePipelineCurly.processScope` (confirmed via `grep`, zero hits in the
diff — lower risk than #2 by inspection, not assumed):

- `web_animations_player_spec.ts:177-193` — a class-field alignment grid
  disagreed on grouping/column-width between rounds because
  `JsTsSpecificRule.tryParseClassField` bailed to "unrecognized field" on
  any embedded NEWLINE; round1 parses the field single-line, then a later
  call-wrap pass adds a NEWLINE inside its initializer, so round2 hits the
  bail and misclassifies it. Fix: collapse a multi-line initializer's
  NEWLINE + continuation indent into a single soft space while scanning,
  instead of bailing (the sibling multi-line-type-expression bail a few
  lines above was left alone, not shown broken).
- `input_transform.ts:16-19` — a decorator-call-plus-declaration line fit
  under `lineLengthLimit` on round1 but wrapped on round2, because
  `enforceDecoratorOverflowCascade` (Phase 1) measured the line's fit
  *before* `enforceUnionIntersectionSpacing`/`enforceTypeColonSpacing`
  (Phase 4) later widened e.g. `string|number` inside the decorator's own
  arguments — a self-violating round1. Fix: pulled those two TS-only
  passes forward to run immediately before `enforceDecoratorOverflowCascade`
  (same pulled-forward precedent as `enforceComplexityPadding`/etc.); the
  original Phase 4 calls stay as idempotent no-ops.

Repro: direct `curl` fetch of both files from a pinned angular commit (no
full clone — prior checkout gone, this system's git rejects
`--filter=blob:none --sparse`). Validated via A/B rebuild on both real
files (byte-for-byte repro pre-fix, full idempotency + `js_ts_syntax_check.sh`
clean post-fix). `make test` 260/260 → 261/261. New fixture:
`test/real_code_regressions_186_{inp,out}.ts` (combines both bugs in one
minimized file). No fresh full-corpus re-run performed (checkout
unavailable) — validation scope is the two real files plus the fixture
suite, same precedent as RDD_KEY_270.

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

`new Map([[undefined, undefined]])` (a nested/double-bracketed array
literal argument) got a stray `;` inserted inside the call's parens on a
single pass — real corruption, not just non-idempotency. Root cause:
`TokenizerCurly`'s C++11 `[[attribute]]`-open detection was missing the
`&& lang.isCpp` guard its sibling `]]`-close/`[:` branches already have, so
a TS nested `[[` matched the C++ heuristic and tokenized as an OP
"attribute open" token while its matching `]]` fell through to ordinary
PUNCT — an asymmetric OP/PUNCT pair that undercounted the array's open
relative to its close in every `isPunct(t, "[")`-based bracket-depth
tracker, including `enforceCallLineBreaking`'s `matchParenForward`, which
then read the call's argument slice one token too far (through to the
statement's own `;`). Fixed by adding the missing `&& lang.isCpp` guard
(one line). Verified: isolated repro, full `watchPublic.ts` (syntax-clean +
idempotent), a genuine C++ `[[nodiscard]]` attribute still tokenizes
correctly (no regression), `make test` 271/271. New fixture:
`real_code_regressions_194_inp/out.ts`.

### Braceless if/else-if body-column-alignment padding, CRLF-staleness — FIXED (2026-08-09, RDD_KEY_273)

Found re-confirming 3 of the 14 previously-flagged `microsoft/TypeScript`
dogfood files (`compiler/builder.ts`, `compiler/moduleNameResolver.ts`,
`services/findAllReferences.ts`, all CRLF-original): `BlockStructureRule.
alignBracelessElseIfChain`'s body-column padding for a braceless `if`/
`else if` chain went stale across a second pass — round1 leaves a short
`if` branch unpadded, round2 pads it to align with its wider `else if`
sibling. Root cause: the method splits on `"\n"` only, so every line of
CRLF-original source keeps a trailing `'\r'` (CRLF/LF normalization
happens once, at the very end, in `Main.applyLineEndings`) — that stray
byte skews the method's length math, landing on different sides of the
`lineLengthLimit` padding-guard boundary between round1 (still-CRLF) and
round2 (already-LF-only, one byte narrower). Fixed by stripping any
trailing `'\r'` from each split line immediately after the split, before
any measurement (safe — the final output's line-ending style is
independently re-derived from the *original* file by `Main.applyLineEndings`).
Verified via A/B rebuild + all 3 files re-confirmed idempotent against a
fresh clone. `make test` 271/271 → 272/272. New fixture:
`real_code_regressions_195_inp/out.ts` (CRLF-encoded, `.gitattributes`-marked
`-text`, same precedent as `real_code_regressions_147_inp.ts`). This same
mechanism also turned out to be the root cause of Tier-3 shape #4 below,
and was ruled out as the cause of shape #2 below.

### Class-field alignment-group splitting on a same-line leading comment — FIXED (2026-08-09, RDD_KEY_274)

Found in the same reconfirmation: `server/editorServices.ts`'s
`readonly throttledOperations : ThrottledOperations;` (and its alignment-
group sibling) round-trips at its group's wide padded column on round1 but
collapses to its own narrow width on round2, silently splitting the group
— same symptom in `server/project.ts`. Both files are CRLF-original like
shape #1's, but instrumentation showed a genuine extra NEWLINE token, not
a stray `'\r'`: the field's leading comment sits on the same physical line
as the field in the source, so round1 has exactly 1 NEWLINE in the gap
since the previous field; `JsTsSpecificRule.flushClassFieldGroup`
unconditionally renders every leading comment on its own line regardless
of source shape, adding a NEWLINE the original never had. Round2 (fed
round1's own output) then sees 2 NEWLINEs in that gap, and the old
`blankLineBetween` helper (counts total NEWLINEs, blank if >= 2) wrongly
judged it a genuine blank line, splitting the group. Fixed by requiring the
two NEWLINEs be strictly back-to-back (only WHITESPACE allowed between; a
COMMENT resets the count) to count as a real blank line. Verified via A/B
rebuild + both files re-confirmed idempotent against a fresh clone. `make
test` 272/272 → 273/273. New fixture: `real_code_regressions_196_inp/out.ts`
(a minimized `Foo` class; plain LF, confirming a distinct root cause from
shape #1 despite sharing the same flagged-file corpus).

### Closing brace non-idempotently gains a `// if` trailing comment — FIXED as side effect (2026-08-09, RDD_KEY_275)

Found in the same reconfirmation: `services/codefixes/
fixMissingTypeAnnotationOnExports.ts` had a closing `}` that round-tripped
bare on round1 but gained a trailing `// if` comment on round2 — a
different mechanism from RDD_KEY_273's string-length guard
(`BlockStructureRule.decideComment`/`countContentLines` counts NEWLINE
tokens between a block's braces against a threshold). Hard to minimize —
only reproduced on a self-contained 100-line excerpt of the real
CRLF-original file (lines 1050-1150), not smaller hand-crafted snippets.
A/B rebuild on that excerpt isolated the cause without a full trace:
reverting `BlockStructureRule.java` to its pre-RDD_KEY_273 state reproduces
the bug; the RDD_KEY_273-fixed state (already committed) is idempotent on
the same excerpt — no separate code change needed, RDD_KEY_273's CRLF
`'\r'`-strip fixes this as a side effect (plausibly an earlier
width-sensitive pass inside the same enclosing block was itself
CRLF-skewed, shifting its NEWLINE count across the `closingCommentMinLines`
boundary). `make test` 273/273 → 274/274. New fixture:
`real_code_regressions_197_inp/out.ts` (100-line CRLF excerpt, exists to
lock in coverage of the already-landed RDD_KEY_273 fix).

### Interface intersection-type field named `class`/`interface` misclassifies its own nested object-type brace — FIXED (2026-08-09, RDD_KEY_276)

Found in the same reconfirmation: `compiler/types.ts`'s
`JSDocAugmentsTag`/`JSDocImplementsTag` interfaces each have a field
literally named `class` (a legal TS property name); the nested inline
object-type literal's closing `};` shifted from column 0 (round1, already
mildly corrupted) to indented (round2, differently corrupted). Root cause,
isolated via a 3-line minimal repro: `JsTsSpecificRule.classBraceKind`
walks backward from a `{` for a `class`/`interface` KEYWORD token to
classify the enclosing construct, with no regard for whether that token is
a declaration keyword or a property name — so walking back from the
field's own nested brace, it hit the field's own name token `class` first
and misclassified the nested brace as a class body, which
`enforceClassFieldAlignmentGrid` then corrupted by running its class-field
grid rewrite on the object-type-literal's interior. Fixed by adding
`isFieldNameKeywordUsage` — true when the token after a candidate
`class`/`interface` KEYWORD is `:`/`?` (field-name usage) rather than an
identifier; `classBraceKind` now keeps walking backward instead of
returning a kind in that case. (First attempt used `isPunct(t, ":")` and
silently never matched — `:` tokenizes as OP, not PUNCT, in this codebase;
corrected to `isOp`.) Verified: minimal repro + the full real ~9800-line
`compiler/types.ts` (CRLF-original) both idempotent post-fix. `make test`
274/274 → 275/275. New fixture: `real_code_regressions_198_inp/out.ts`
(plain LF, confirming a distinct root cause from RDD_KEY_273/274/275's
cluster).

### Known false positives (no source change needed, fixture-only)

- Spurious-looking blank line after a class's opening `{` in older `.js`
  fixture drafts, and doubled trailing space before a one-liner getter
  body's closing `}` — both confirmed correct, existing behavior (STYLE.md
  §7 named-construct blank line; `GetterSetterRuleCurly`'s group-width body
  padding), matching passing C++/Java/Kotlin fixtures byte-for-byte. Only
  the stale hand-authored `.js` draft fixtures were wrong; resolved by
  regenerating them.
