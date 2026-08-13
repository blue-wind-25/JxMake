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
(`Lang.isScaffoldOnly` no longer includes js/ts); all §1–15 rules are
implemented in `JsTsSpecificRule.java` (+ `JsTsDeclarationAlignmentRule.java`
for the declaration-alignment grid), wired into `FormatterCurly`'s phase
pipeline. Current `make test`: 297/297 forward + idempotency (grows as
fixtures are added; see dogfood sections below for count history).

---

## Status Summary

All planned baseline work is **DONE**: §1–15 implemented, JS and TS local
fixtures active, and real-code dogfood passes completed for
`expressjs/express`, `nestjs/nest`, `vuejs/core`, `lodash/lodash`,
`angular/angular` (categorized, all clusters fixed — cluster 4's residue was
itself split into 3 findings across two sessions, all 3 now fixed, see
RDD_KEY_269/RDD_KEY_271), and `microsoft/TypeScript` (categorized, all
clusters fixed, see RDD_KEY_270). No dogfood finding remains open as of
RDD_KEY_271 — see "Active work" below. JS/TS basics were deliberately
hardened to a stable baseline before Python3 (next job in rotation), per
user direction.

---

## Scope

`STYLE_JS_TS.md` covers latest ECMAScript (ES2024+) and latest TypeScript
(5.x), one shared file for both (TS is a syntactic superset of JS). Per
`STYLE_JS_TS.md`, JSX/TSX were originally **out of scope entirely, not
merely deferred** — that blanket statement is now superseded by a two-step
plan (2026-08-13), Step 1 of which has landed:

- **Step 1 (DONE — the boundary-finding pre-pass, 10/11 design-list
  contexts, Increments 1-6).** `TokenizerCurly.findJsxSpans` detects a JSX
  tree at any of the enumerated expression-start contexts and collapses it
  into one opaque, frozen `JSX_SPAN` token — raw source preserved
  byte-for-byte, never reformatted internally. Critically, the frozen token
  sits in the significant-token stream like any other atomic expression
  token, so surrounding formatting (call-argument wrapping, assignment-RHS
  line-length decisions, array-element spacing) already treats a JSX tag
  *as an expression* from the outside. This is content-preservation, not
  JSX-aware reformatting.
- **Step 2 (Increment 1/5 LANDED — detect-and-measure-only; increments 2-5
  NOT STARTED — generic grouped-expression-style wrap of the tag's own
  interior).** Format a JSX_SPAN's own contents using the same generic
  "long expression exceeding the line-length threshold gets broken across
  lines" machinery already used for calls/object literals/array literals —
  treating the tag as if it were a long grouped expression — WITHOUT
  parsing real JSX grammar (no attribute-specific alignment, no
  children-specific indentation semantics). Scoped into sub-contexts by the
  "2026-08-13 scoping session — context 11" section below (right after the
  item-10 implementation write-up) — that section is the source of truth
  for Step 2's design. Its own suggested 5-increment breakdown's first
  increment (sub-context 1's minimal structure, detect-and-measure-only, no
  behavior change) landed 2026-08-13 — see the "2026-08-13 implementation
  session — Step 2, Increment 1 of 5 (detect-and-measure-only) (LANDED)"
  section further below. Increments 2-5 (the actual wrap-decision function,
  self-closing-tag wrapping, children-bearing-tag wrapping, and real-corpus
  validation) remain **NOT STARTED**. This is a deliberately bounded middle
  ground between "frozen opaque blob" (Step 1) and a full JSX-aware
  embedding dispatcher (still a distinct, larger future job — JSX embeds
  tag syntax directly inside JS/TS expression position, a compound-language
  situation, not a same-file extension like HTML5's `<script>` splicing).

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
4. **2026-08-10.** Bare single-param arrow parens (STYLE.md §6) —
   `canonicalize` skips an `ArrowFunction`'s `(`/`)` tokens when its one
   parameter is a plain identifier with no type/default/rest/optional
   marker (`isBareableArrowParam`), so `x => x+1` and `(x) => x+1`
   canonicalize identically.
5. **2026-08-10.** For-loop incrementor
   `++`/`--` position (STYLE.md §4 pre-increment-except-when-post-required)
   — `canonicalize` special-cases a `ForStatement`'s `incrementor` slot:
   when it's a bare `PostfixUnaryExpression`/`PrefixUnaryExpression` with a
   `++`/`--` operator, it's rendered as a fixed prefix-position canonical
   token (`++`/`--` then the operand) regardless of original position, so
   `i++` and `++i` canonicalize identically **only inside a `for(...)`
   incrementor clause** — deliberately not applied to `++`/`--` anywhere
   else in a file, since outside that one slot a pre/post swap can still be
   a real, catchable semantic bug (e.g. `y = x++` vs `y = ++x`).

Re-verified against all hand-crafted pairs plus new pairs per tolerance —
all pass, both `.js` and `.ts`. Tolerances 4/5 verified via 4 synthetic
pairs (arrow-param OK, for-loop-incrementor OK, dropped-statement still
MISMATCH, non-for-loop `x++`/`++x` swap still MISMATCH) — the original
lodash/lodash checkout's real trigger files no longer exist on disk to
re-run directly (the `/tmp/lodash` checkout's `.js` files outside
`dist/`/`vendor/` are all gone; only the excluded `dist`/`vendor` trees
remain), so synthetic snippets isolating the exact two transformation
shapes were used instead, per `STATE_COMMON.md`'s real-code-testing
fallback guidance.

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

- **IMPLEMENTED (2026-08-13) — see the "2026-08-13 implementation session —
  JSX-in-`.js`/`.ts` detection (LANDED)" section above for the full
  writeup.** `.js`/`.mjs`/`.cjs` now get the JSX pre-pass unconditionally;
  `.ts` stays gated off by default with a new `jsx-in-ts` Config-key
  opt-in; `TokenizerCurly`'s tag-matching was hardened with tag-name
  identity tracking; two latent template-literal-spacing bugs the
  widening exposed were found and fixed;
  `taniarascia/react-tutorial` re-dogfooded clean.

- **Should JSX detection ever extend to plain `.js`/`.ts` files (not just
  `.jsx`/`.tsx`)?** (Original open question, now resolved above.) Raised by
  the `taniarascia/react-tutorial` dogfood pass
  (2026-08-13, see that section under "Checklist" above) — that repo's real
  corpus ships JSX embedded directly in `.js` files (an older-CRA
  convention), which the current strictly extension-gated
  `Lang.isJsxSyntax` never detects, causing real content corruption when
  formatted as-shipped (not just a missed opportunity — the plain-JS
  tokenizer actively misparses the embedded JSX). Not acted on this pass:
  extending detection risks false-positive JSX parsing colliding with
  legitimate `<`/`>` comparison operators in ordinary `.js`/`.ts` code that
  never contains JSX — a real design tradeoff, not a narrow safe fix.
  Deferred to the user; candidate outcomes include a documented Known
  Limitation (do nothing, require `.jsx`/`.tsx` renaming as a precondition)
  or a future opt-in heuristic-detection mode.

  **Researched 2026-08-13 — see the "2026-08-13 research session —
  JSX-in-`.js`/`.ts` detection" section under Checklist item 11 (context
  11) above for the full writeup.** Outcome: not fully settled, but
  research produced a concrete lean rather than an even split. Real tooling
  (Babel/Prettier) attempts JSX parsing unconditionally for the whole
  JS-family bucket (`.js`/`.mjs`/`.cjs`) but deliberately does NOT do the
  same for `.ts` (TS's own compiler gates JSX strictly by
  `.ts`-vs-`.tsx` `ScriptKind`, because of a real, industry-recognized
  ambiguity with the legacy `<Type>expr` angle-bracket cast syntax).
  Re-reading this formatter's own `TokenizerCurly.isJsxContext`/
  `findJsxSpanEnd`/`parseJsxTag` found that all eleven detection contexts
  already structurally exclude ordinary `<`/`>` comparisons (a comparison's
  `<` is never expression-start, so it can never reach any of the eleven
  trigger clauses) — the real residual risk is narrowly the `<Type>expr`
  cast collision, which is rare in true `.js` (that syntax doesn't parse as
  JS at all) but real in `.ts`. Recommendation (not yet implemented, not
  yet validated against a widened-default real corpus): extend detection
  unconditionally to `.js`/`.mjs`/`.cjs`, keep `.ts` extension-gated by
  default (matching Prettier/tsc's own split), and offer a `.ts`-scoped
  config/CLI opt-in (e.g. `--jsx-in-ts`) for the legacy-`.ts`-with-JSX case
  instead. A tag-name-identity hardening of `parseJsxTag`/`findJsxSpanEnd`
  (currently only tracks nesting depth, not name — `<a>...</b>` balances
  today) is called out as worth doing before or alongside any widening.
  Still requires user sign-off before implementation.

- **HTML5 needs its own dispatcher for `<style>`/other embedded formats
  beyond `<script>`.** `<script>` splicing (JS/TS dispatch, CDATA unwrap/
  rewrap, Config-threading) is done — see `XmlSpecificRule.
  renderScriptOrStyle`. Any further HTML5/embedded-format dispatcher work
  belongs to the Data Formats job (`STATE_DATA_FORMATS.md`), not this one.

- **Watch-list, not a job (moved from `XL.txt` TIER 8 2026-08-12):
  `=`-alignment-group non-self-stability quirk.** Seen once while building
  the RDD_KEY_142 fixture near `location_shim.ts`, never reproduced against
  the real file — no repro exists to act on. Not tracked as backlog work;
  if a future JS/TS dogfood run ever produces an `=`-alignment-group
  instability, check whether it matches this quirk before treating it as a
  new bug.

- **JSX/TSX scope statement** — superseded 2026-08-13; see Scope section
  above for the current two-step plan (Step 1 DONE, Step 2 NOT STARTED).
  The old "out of scope entirely, not deferred" wording no longer applies
  as a blanket statement — Step 1's boundary-finding pre-pass is real,
  landed JSX/TSX support (content-preservation + outer-expression
  treatment), even though full JSX-aware reformatting (Step 2 and beyond)
  remains future work.

  **2026-08-07 discussion session (no code, no fixtures, no RDD_LOG key —
  interactive discussion only, findings recorded for a future implementation
  session):** user proposed a 3-step staged approach as a simpler
  alternative to a full embedding-aware dispatcher: (1) tokenizer marks a
  whole JSX tree as one opaque `IDENTIFIER` token, throws on tag imbalance;
  (2) `{...}` expression holes inside JSX become `__JSn__` placeholders,
  outer markup handed to the existing HTML formatter; (3) each placeholder's
  content sent through the JS/TS formatter as an independent small program.
  Assessed in depth, **not adopted as a design**:
  - Step 1 doesn't avoid the hard problem — finding tag boundaries against
    `<`'s three-way ambiguity with less-than/generics, recursively through
    nested `{}` holes, still requires that walk; packing the result into a
    plain `IDENTIFIER` just discards the structure and breaks every
    downstream pass that assumes realistic single-token width (declaration/
    class-field alignment grids, `enforceCallLineBreaking`'s `candidateLen`
    checks — same width/pass-ordering fragility class as
    RDD_KEY_248/249/250). A dedicated opaque/frozen token kind would be
    needed instead.
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
  - **How real JSX parsers solve the `<` ambiguity, and why it doesn't port
    directly:** they switch lexer modes based on grammar position (a parser
    always knows when it's at expression-start, where `<` is unambiguously
    JSX-open), not lookahead heuristics. This codebase has no
    grammar-position-aware parser (flat tokenizer + local-lookback passes),
    so this can't be inherited for free — `<`/`>` disambiguation for
    generics alone already needed a dedicated mechanism
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
    npm-pin gotcha already hit for `typescript` itself.

  **2026-08-12 design session (still no code, no fixtures, no RDD_LOG
  key — design-only, prep for a future embedding-aware-dispatcher
  implementation session). Fills in the two items the entry above left
  blank. Nothing here supersedes the Scope section's "out of scope
  entirely" statement — JSX/TSX remain unimplemented; this is design
  only.**

  **1. Enumerable list of expression-start contexts** (where a pre-pass
  should test a `<` as a possible JSX-open, given this codebase's flat
  tokenizer + local-lookback architecture — no grammar-position awareness,
  so "expression-start" has to be approximated as a fixed list of
  token-adjacency shapes, derived from walking `TokenizerCurly`/
  `JsTsSpecificRule`'s own coverage of JS/TS expression grammar):

  - After `return` (KEYWORD).
  - After `=>` (arrow-function body start; the arrow's own parameter list
    is a separate, unambiguous paren-delimited construct, not itself a
    boundary candidate).
  - After `?` and after `:` inside a ternary (`cond ? <A/> : <B/>`) — both
    branches independently, not just the first.
  - Call-argument start: immediately after `(` or after a top-level `,`
    inside an already-open `(` (mirrors `splitTopLevelCommas`'s own
    argument-boundary notion).
  - Array-literal element start: immediately after `[` or after a
    top-level `,` inside an already-open `[`.
  - Assignment RHS: after `=` (also covers `+=`/`-=`/etc. compound
    assignment operators — same RHS-start shape).
  - Logical/nullish short-circuit RHS: after `&&`, `||`, `??`.
  - Parenthesized-expression start: after a bare `(` that is not itself a
    call/argument-list open (i.e. not immediately preceded by an
    IDENTIFIER/`)`/`]` — distinguishing a grouping paren from a call paren
    matters here because both route through the same `(`-adjacency check
    but a call's `(` is already covered by the call-argument-start rule
    above; a grouping `(` needs its own entry since its "owner" isn't a
    call).
  - Recursively inside a JSX expression hole's own `{...}` — once inside a
    found JSX span, any `{` opens a hole, and every context above applies
    again to the token stream starting just after that `{` (this is what
    makes it a pre-pass with recursion, not a single flat scan).
  - Template-literal `${}` holes: same recursion trigger as a JSX
    `{...}` hole — a `${` inside a backtick string is already a distinct
    tokenizer state (`emitTemplateLiteral`) that re-enters ordinary token
    scanning for its interior, so the same expression-start list must
    apply there too (`` `text ${<Foo/>} more` `` is legal JSX-in-template
    just as `{<Foo/>}` is legal JSX-in-JSX-hole).
  - After `...` (spread) wherever spread is legal in expression position
    (array-literal element, call argument) — covered by the call-argument/
    array-element entries above as long as `...` itself doesn't need to be
    skipped over before testing the following `<`; call out explicitly
    since it's easy to miss in a token-adjacency table (the token
    immediately before `<` is `...`, not `(`/`[`/`,`).
  - Decorator-call argument position (`@Component({ template: <Foo/> })`)
    — not a new context, already covered by the object-literal-value /
    call-argument entries once `@Foo(...)`'s own `(` is recognized as an
    ordinary call open; called out because decorators are JS/TS-specific
    and easy to overlook when porting a "generic expression-start" list
    from a general-purpose JS reference.
  - **Explicitly NOT a context:** after an IDENTIFIER, after `)`, after
    `]`, after a NUMBER/STRING/CHAR — these are exactly the shapes
    `reclassifyAngleBrackets`'s existing generic-open check already claims
    (`prev.type == IDENTIFIER || isCastKeyword(prev)`) or are otherwise
    non-expression-start (postfix positions). A `<` here is unambiguously
    a relational/generic operator, never JSX-open, and must be left alone
    by the pre-pass — this is the two mechanisms' actual division of
    labor (see point 3 below).

  Risk if incomplete: a `<` in an unlisted expression-start context is
  invisible to the pre-pass and falls through to ordinary `<`/`>`
  operator/generic handling, silently misparsing the JSX open as a
  less-than comparison or spurious generic — exactly the failure mode
  this design has to avoid, so this list should be treated as the
  starting draft to validate against a real JSX/TSX corpus (react itself,
  or a `create-react-app` output tree) before implementation, not as
  provably complete from static reasoning alone.

  **2. Concrete opaque-span token representation.** Proposed new
  `TokenType.JSX_SPAN` (alongside the existing `ANGLE_BRACKET_OPEN`/
  `ANGLE_BRACKET_CLOSE`/`FSTRING_*` precedent of dedicated non-generic
  token kinds for a structurally special span). `Token` currently has no
  position/offset field at all (`type`, `text`, `braceDepth`,
  `parenDepth`, `name`, `frozen` — see `TokenizerCore.Token`); width
  everywhere downstream is computed as `text.length()` directly off the
  token's raw text (`MiscRuleCurly`'s `candidateLen`/`paramsLine.length()`/
  etc. all do this, no separate width field exists for any token kind
  today). A `JSX_SPAN` token should follow that same precedent rather than
  inventing a new position-tracking mechanism the rest of the codebase
  doesn't have:
  - `text`: the full raw source span from the opening `<` through the
    matching closing tag's `>` (or self-closing `/>`), byte-for-byte,
    **including embedded newlines** for a multi-line JSX tree. This is
    what makes it "opaque" — nothing downstream reformats its interior
    directly.
  - Width for single-line fits-checks (`candidateLen`-style call sites):
    since `text` can itself contain `\n`, a naive `text.length()` would
    overstate width for a multi-line span and understate it for
    "still fits on this physical line" checks that only care about the
    *last* line's contribution when the token trails onto a following
    line, or the *first* line's contribution when a caller is measuring
    up to where the span starts. Rather than adding a new field, reuse
    the existing pattern this codebase already has for other tokens whose
    raw text can span multiple lines (`COMMENT_BLOCK`, template-literal
    `STRING` tokens via `emitTemplateLiteral`) — those are simply excluded
    from the single-line fits-check call sites that would be misled by a
    `\n`-containing `text.length()`, not given a separate width field.
    `JSX_SPAN` should follow the same convention: line-length/alignment
    passes that iterate raw `text.length()` need one additional guard
    (skip/bail on a token containing `\n`, same as they'd need to already
    for a multi-line block comment or template literal, if any currently
    don't — this is worth auditing, not assuming pre-solved) rather than
    a bespoke computed-width field only `JSX_SPAN` gets.
  - `frozen`: set `true` unconditionally — a `JSX_SPAN` must never be
    touched by any transformation pass (spacing, alignment, keyword
    rewriting), consistent with how `frozen` is already used for
    pass-through opaque spans elsewhere in the tokenizer.
  - `name`: unused for this token kind (`Token`'s `name` field is
    documented as "for `{`/`}` only"); leave `null`.
  - **Nesting depth for `{}` holes**, needed for the previously-assessed
    3-step approach's placeholder-substitution step to still work on top
    of this representation: do NOT add a depth field to `Token` itself.
    Depth is a property of the *pre-pass's own recursive walk*, not of
    the emitted token — by the time a `JSX_SPAN` token exists in the
    output stream, its interior holes have *either* already been
    recursively resolved (each hole's `{...}` interior independently
    re-tokenized/reformatted and re-spliced back into the span's `text`
    before the span token is emitted) *or* the span is fully opaque with
    holes left as raw unformatted text inside it (a scope decision for
    the implementation session, not this design). Either way there is
    exactly one `JSX_SPAN` token per top-level JSX tree in the surrounding
    JS/TS token stream — nesting is internal to how that one token's
    `text` was assembled, never externally visible as sibling tokens the
    rest of the pipeline needs to know the depth of. This is a deliberate
    simplification versus the 3-step approach's original `__JSn__`
    placeholder idea (which needed depth to know which placeholder
    resolves into which other placeholder's context) — here, the
    recursive resolution happens entirely *before* span-token emission,
    so no placeholder bookkeeping survives into the main token stream at
    all.
  - Integration point: `reclassifyAngleBrackets` already runs as a
    post-tokenize pass over the flat token list (`TokenizerCurly`, called
    conditionally per-language). A `findJsxSpans`-shaped pre-pass would
    need to run as another post-tokenize pass over the same flat list,
    producing `JSX_SPAN` tokens that replace the raw `<...>...</...>`
    token run in place — same shape of pass as `reclassifyAngleBrackets`
    itself (mutate the token list in place, walk a `sig` index list of
    significant tokens), not a new tokenizer entry point.

  **3. Interaction with `<`/`>` generic disambiguation.** Ordering: the
  JSX boundary-finding pre-pass must run **before**
  `reclassifyAngleBrackets`, not after or interleaved — `reclassifyAngleBrackets`
  needs the token stream already free of any `<`/`>` that belong to a
  resolved `JSX_SPAN` (a `JSX_SPAN` is one opaque token, so by definition
  its interior `<`/`>` characters are gone from the significant-token list
  `reclassifyAngleBrackets` walks; there is nothing left for it to
  misinterpret there), and conversely `reclassifyAngleBrackets`'s own
  generic-safe-token machinery has no way to special-case "this `<` is
  actually a JSX open" — it only ever asks "does the token before this
  `<` make it generic-safe," never "is this `<` JSX." Running JSX-finding
  first cleanly removes JSX spans from view before generics disambiguation
  ever sees the token stream, avoiding a joint disambiguation problem.

  **Ambiguous case, confirmed real:** old-style TS generic type-assertion
  cast (`const x = <T>foo;`) and a JSX element (`const x = <div>foo</div>;`)
  both start identically — `=` then `<` then IDENTIFIER then `>` — and
  both land in the *same* expression-start context from list item 1
  above (assignment RHS). This is a genuine, not merely apparent,
  ambiguity: nothing in the immediate token-adjacency shape distinguishes
  them; real JSX tooling resolves this exact case by parser mode
  (`.tsx` files disable the legacy cast syntax entirely and always treat
  `<T>` as JSX — TypeScript's own documented reason `<T>` casts are
  banned in `.tsx`), not by grammar disambiguation. **This is important
  context for the future implementation session's scope**: since this
  codebase dispatches by file extension already (`Lang.isTs`/`.tsx`
  presumably would be a new extension, not yet added — see Scope section,
  "JSX/TSX ... own future embedding-aware dispatcher"), the same
  `.tsx`-disables-legacy-cast convention is directly reusable — a
  `.tsx`/`.jsx` file's pre-pass can safely treat every expression-start
  `<` as a JSX-open candidate first (falling back to plain relational-`<`
  only if no matching closing tag/self-close is found), while a plain
  `.ts`/`.js` file's pre-pass should not run at all (no JSX possible
  there, `<T>foo` stays a legacy cast, handled by the existing
  `reclassifyAngleBrackets`/cast-keyword machinery unchanged). This
  resolves the ambiguity by construction (file-extension-scoped dispatch,
  matching real tooling's own convention) rather than needing a token-level
  heuristic to guess between the two meanings within one file.

  **Does the portable idea hold up?** Yes, with the file-extension-scoped
  caveat above made explicit (it was implicit, not stated, in the
  2026-08-07 assessment). The core positional-disambiguation insight
  survives concrete design: a short enumerable expression-start-context
  list is buildable without a real AST (list above), a token representation
  fits the existing `Token`/`TokenType` model without new fields (`JSX_SPAN`
  + text-length-based width + the existing multi-line-token width-guard
  convention), and the generic-vs-JSX ambiguity is resolved cleanly by
  running JSX-finding first and scoping it to `.tsx`/`.jsx` files only —
  not by trying to disambiguate `<T>` inline within an ordinary `.ts`
  file, which would have been genuinely unsound (no reliable token-level
  signal distinguishes the two there). One real gap surfaced by this
  design pass and **not** resolved: the expression-start-context list
  above is a draft assembled from static reasoning over this codebase's
  own JS/TS grammar coverage, not validated against a real JSX corpus —
  flagged above as required validation work before an implementation
  session trusts it as complete.

  **2026-08-12 implementation session, Increment 1 — LANDED, still
  unfinished (1/11 contexts, no real corpus validation yet).** First real
  code toward this design, per the parent task's incremental-checkpoint
  instructions. What's implemented and fixture-verified:

  - `TokenType.JSX_SPAN` added to `TokenizerCore` exactly per the design
    above (raw `text` including embedded newlines, `frozen = true`
    unconditionally, no new `Token` fields).
  - `Lang.isJsxSyntax` (new field, new `Lang(String language, String
    filePath)` constructor overload) — true only when `filePath` ends in
    `.jsx`/`.tsx`. `.jsx`/`.tsx` already inferred to `"js"`/`"ts"` at the
    `Lang.infer` level (pre-existing, unrelated to this session), so this
    is the *only* signal distinguishing a real JSX/TSX file from a plain
    `.js`/`.ts` one — needed because `Lang` previously carried no per-file
    path at all. `FormatterCore.forLanguage` gained a `(language,
    filePath)` overload (old one-arg form still exists, delegates with a
    `null` path, used by every caller with no real file — e.g.
    `XmlSpecificRule`'s forced `"js"`/`"css"` `<script>`/`<style>`
    dispatch); `GdrPipelineGate.applyAndFormat` (the one production call
    site that has a real `filePath`) updated to use it.
  - `TokenizerCurly.findJsxSpans` (+ helpers `findJsxSpanEnd`,
    `parseJsxTag`, `skipBalancedBraceHole`) — a post-tokenize pass run
    only when `lang.isJsxSyntax`, immediately before
    `reclassifyAngleBrackets` per the design's ordering requirement.
    **Only the "after `return`" context is implemented** (`Token.
    isKeyword(prev, "return")` immediately before the candidate `<`) — the
    other 10 contexts from the design's enumerated list are NOT yet
    implemented; a `<` in any of those positions still falls through
    unchanged to whatever `reclassifyAngleBrackets`/relational-operator
    handling already did before this session (i.e. usually misparsed, if
    it's actually JSX — this is expected/known, not a bug, until a future
    increment adds that context). The span-finder itself already handles,
    within its one implemented context: nested same- and different-name
    elements at arbitrary depth, a self-closing tag (`<br />`) both as the
    sole root element and as a nested child, one HTML attribute, and one
    `{...}` expression hole (balance-skipped, not recursed into — the
    design's recursive-hole context is explicitly future work, tracked
    separately from this "after return" context).
  - **Real bug found and fixed as a prerequisite, not anticipated by the
    design docs**: `TokenizerCurly.isRegexLiteralAllowedHere` returned
    `true` (regex-literal start allowed) whenever the previous significant
    token was a plain `OP` (with only `++`/`--` excluded) — so in a
    `.tsx`/`.jsx` file, the very first `/` of a JSX closing tag (`</Foo>`,
    previous token is `<`) was misread by the *character-level* lexer
    (which runs before `findJsxSpans`'s post-tokenize pass ever sees the
    token stream) as the start of a regex literal, scanning for a second
    unescaped `/` and usually overrunning to end-of-line/EOF with
    `syntaxError = true`, corrupting the whole file's tokenization before
    the JSX pre-pass could run at all. Fixed with a narrow, `lang.
    isJsxSyntax`-gated special case: `OP "<"` immediately before `/`
    disallows regex-start. Scoped to JSX/TSX files only, so `a < /re/`
    (division-after-less-than, technically legal JS) is unaffected in
    plain `.js`/`.ts` files — the narrow tradeoff is accepted only in
    files that opted into JSX syntax via their extension, where a regex
    immediately after `<` is vanishingly rare next to `</Foo>`. This means
    the design's assumption that the pre-pass could be a clean,
    fully-separate post-tokenize step (point 2/3 above) was slightly
    optimistic — one character-level lexer decision upstream of
    tokenization needed a JSX-aware carve-out too, not just the
    post-tokenize pass itself. Worth flagging for whoever implements
    further contexts: watch for other character-level lexer decisions
    (e.g. template-literal `` ` `` detection, cast-keyword detection) that
    might have similar latent JSX-unaware assumptions once more contexts
    exercise more of the token stream.
  - **Fixture-verified**: `test/jsx_tsx_return_context_{inp,out}.tsx`
    (nested elements + one attribute + one `{}` hole + a self-closing
    root return + a same-file `if (x < 1)` comparison confirmed
    untouched, since it's not immediately after `return`). `make test`:
    291/291 → 292/292 forward + idempotency, zero regressions on any
    existing `.js`/`.ts` fixture (confirms the file-extension gate is a
    true zero-behavior-change no-op for non-`.jsx`/`.tsx` files, per the
    parent task's hard stop-condition). Manually re-verified round1/round2
    byte-identical on a hand-written multi-function `.tsx` smoke file
    beyond just the one registered fixture.
  - **NOT done**: no real-JSX-corpus validation (react itself,
    `create-react-app` output, or similar) — the parent task explicitly
    flagged the 11-context list as unvalidated draft reasoning and
    required corpus-checking before trusting it complete; that validation
    did not happen this session (only the one narrowest context was
    attempted, so there was no multi-context list to cross-check yet
    regardless). `js_ts_content_diff.js` (the TS-compiler-API-based
    dogfood content-preservation checker) was NOT updated for `.tsx`/JSX
    syntax — per the 2026-08-07 discussion above it should eventually use
    `ts.ScriptKind.TSX`/`.JSX`, but that's still future work, untouched
    this session.
  - **Where to resume (superseded by Increment 2 below — kept for
    history).**

  **2026-08-12 implementation session, Increment 2 — LANDED (3/11
  contexts, still no real corpus validation).** Same session pattern as
  Increment 1, extending `TokenizerCurly.findJsxSpans` with two more
  expression-start contexts from the design's enumerated list:

  - **"After `=>`"** (arrow-function body start) and **"after `?`"/"after
    `:`"** (both branches of a ternary conditional expression, counted as
    one context per the design's own list item 3 — see the design list
    above) are now recognized alongside the existing "after `return`"
    check. All three checks are simple single-token-lookback tests
    (`Token.isKeyword(prev, "return")`, `Token.isOp(prev, "=>")`,
    `Token.isOp(prev, "?")`, `Token.isOp(prev, ":")`), combined into one
    `isJsxContext` boolean — no comma/bracket-depth tracking needed for
    any of these three, matching the parent task's "natural next pair"
    grouping advice.
  - **Ambiguity safety confirmed, not just assumed**: a bare `?`/`:` OP
    token unambiguously means ternary here, since the character-level
    lexer's `MULTI_CHAR_OPS` already matches `?.` (optional chaining), `??`
    (nullish coalescing), and `?:` before ever falling through to a
    single-char `?` token, so this check can't misfire on those other
    `?`-shaped operators. A context check firing on a *non*-JSX `<` (e.g. a
    real less-than comparison, or a legacy `<T>` cast in a ternary
    else-branch) is still safe regardless: `findJsxSpanEnd`/`parseJsxTag`
    returns -1 for anything that doesn't parse as a balanced JSX tree,
    leaving tokens untouched — the same self-correcting property Increment
    1 relied on for the `return`-context `<T>`-cast ambiguity.
  - **Fixture-verified**: `test/jsx_tsx_arrow_ternary_context_{inp,out}.tsx`
    (bare-arrow-body JSX return; a ternary with two simple-element
    branches; a ternary whose truthy branch is a nested JSX tree with an
    attribute and a `{...}` hole and whose falsy branch is a self-closing
    `<br />`; an `if (x < 1)` comparison confirmed untouched) and
    `test/jsx_tsx_combined_sanity_{inp,out}.tsx` (all 3 contexts landed so
    far combined in one small component — an arrow-body ternary, a
    `return`-context JSX tree containing a `{...}` hole whose interior
    itself contains a ternary of two JSX elements — verifying context
    interaction, not just isolated context recognition). `make test`:
    292/292 → 294/294 forward + idempotency, zero regressions on any
    existing `.js`/`.ts`/`.tsx` fixture, including Increment 1's own
    `jsx_tsx_return_context` fixture (hand-diffed unchanged as an extra
    check beyond the Makefile-driven `make test` run).
  - **Nothing from the design broke on contact this increment**: unlike
    Increment 1's `isRegexLiteralAllowedHere` surprise, no new
    character-level-lexer carve-out was needed for `=>`/`?`/`:` (already
    ordinary multi-char/single-char OP tokens with no JSX-unaware lexer
    assumption to fix).
  - **NOT done**: still no real-JSX-corpus validation (react itself,
    `create-react-app` output, or similar); `js_ts_content_diff.js` still
    not updated for `.tsx`/JSX `ts.ScriptKind` — both carried over from
    Increment 1's "NOT done" list.
  - **Where to resume**: superseded by Increment 3 below (kept for history).

  **2026-08-12 implementation session, Increment 3 — LANDED (5/11
  contexts, still no real corpus validation).** Adds call-argument-start
  and array-literal-element-start (design list items 4 and 5) to
  `TokenizerCurly.findJsxSpans`'s `isJsxContext` check.

  - **New helper `isCallArgumentOrArrayElementStart`** (+ two small
    supporting helpers, `isCallOpenParen` and `findEnclosingOpenBracket`):
    unlike Increments 1/2's plain single-token-lookback checks, these two
    contexts need to distinguish a call-open `(` from a bare grouping `(`
    (the design explicitly calls out grouping-paren-start as its own,
    separate, not-yet-implemented context — over-claiming it here would
    blur that boundary) and, for a `,`-adjacent candidate, need to know
    what bracket the comma is actually inside of. Handled with three
    shapes, all in one method: immediately after `[` (always
    array-element-start, no ambiguity); immediately after `(` (only
    counted when `isCallOpenParen` confirms the `(` is itself preceded by
    an IDENTIFIER/`)`/`],` matching `reclassifyAngleBrackets`'s own
    call-shaped notion); immediately after a top-level `,` (walks backward
    via `findEnclosingOpenBracket`, a small stack-based bracket matcher
    scanning `(`/`)`, `[`/`]`, `{`/`}` back to the nearest unmatched
    opener, then applies the same `[`-always/`(`-only-if-call-open test to
    that enclosing bracket; an enclosing `{` (object literal) or no
    enclosing bracket at all is correctly NOT recognized this increment).
  - **Ambiguity safety unchanged from Increments 1/2** — same -1-on-
    unbalanced-JSX self-correcting property, harmless on a real less-than
    comparison or index expression.
  - **Fixture-verified**: `test/jsx_tsx_call_array_context_{inp,out}.tsx`
    — a call with two JSX arguments (first immediately after `(`, second
    after a top-level `,`) and an array literal of two JSX elements (first
    immediately after `[`, second after a top-level `,`); an `if (x < 1)`
    comparison confirmed untouched. `make test`: 294/294 → 295/295 forward
    + idempotency, zero regressions on any existing `.js`/`.ts`/`.tsx`
    fixture, manually re-verified round1/round2 byte-identical outside the
    Makefile-driven run as well.
  - **Nothing from the design broke on contact** — no new
    character-level-lexer carve-out needed, same as Increment 2.
  - **NOT done**: still no real-JSX-corpus validation; `js_ts_content_diff.js`
    still not updated for `.tsx`/JSX `ts.ScriptKind` — both carried over
    from Increments 1/2's "NOT done" lists.
  - **Where to resume**: superseded by Increment 4 below (kept for history).

  **2026-08-12 implementation session, Increment 4 — LANDED (7/11
  contexts, still no real corpus validation).** Adds assignment-RHS (incl.
  compound assignment) and logical/nullish-RHS (design list items 6 and 7)
  to `TokenizerCurly.findJsxSpans`'s `isJsxContext` check.

  - **New helper `isAssignmentOrLogicalRhsStart`** (+ two small local sets,
    `JSX_ASSIGNMENT_OPS` and `JSX_LOGICAL_OPS`): a plain single-token-lookback
    check, same shape as Increment 2's `=>`/`?`/`:` checks — no comma/
    bracket-depth tracking needed, matching the parent task's own prediction.
    `JSX_ASSIGNMENT_OPS` = `=`, `+=`, `-=`, `*=`, `/=`, `%=`, `&=`, `|=`,
    `^=`, `<<=`, `>>=`, `>>>=`, `&&=`, `||=`, `??=` — the exact set of
    assignment-shaped entries this tokenizer's own `MULTI_CHAR_OPS` lexer
    table emits, plus the plain single-char `=`. Deliberately **not** reused
    from `MiscRuleCore.ASSIGNMENT_OPS` (rules package): that field is
    `protected` (cross-package inaccessible from `tokenizer`) and is missing
    `&&=`/`||=`/`??=`/`<<=`/`>>>=`, which this tokenizer's own character-level
    lexer does emit — kept local and tokenizer-scoped instead, to avoid
    silently under-covering compound-assignment RHS starts. `JSX_LOGICAL_OPS`
    = `&&`, `||`, `??`.
  - **Attribute-`=` ambiguity (flagged by the parent task) checked, confirmed
    a non-issue**: `findJsxSpans`'s outer scan only re-examines a `<`
    immediately after a recognized-context token; a JSX tag's own attribute
    `=` (e.g. `bar={x}` inside `<Foo bar={x} />`) is only followed by `<` if
    the attribute value were bare JSX with no braces/quotes at all (e.g.
    `bar=<Bar/>`), which isn't valid JSX syntax in the first place (attribute
    values must be a string literal or a `{...}` expression) — so this
    shape essentially cannot occur in real JSX, and even if it did, the same
    `findJsxSpanEnd`/`parseJsxTag` returns -1 safety net every prior
    increment relies on would leave it untouched. No special-casing needed
    in `isAssignmentOrLogicalRhsStart` beyond the plain lookback.
  - **Ambiguity safety unchanged from prior increments** — same -1-on-
    unbalanced-JSX self-correcting property: a `<` after `=`/`&&`/etc.
    that's actually a real comparison (`x = y < z`) or a legacy `<T>` cast
    falls through untouched.
  - **Fixture-verified**: `test/jsx_tsx_assign_logical_context_{inp,out}.tsx`
    (a plain `=` assignment, a `+=` compound assignment, and each of
    `&&`/`||`/`??` immediately preceding a JSX open; an `if (x < 1)`
    comparison confirmed untouched) and
    `test/jsx_tsx_assign_logical_sanity_{inp,out}.tsx` (Increment 4's two
    new contexts combined with previously-landed contexts — plain `=`,
    `&&`-RHS, `??`-RHS, `return`-context [implicit via nested function],
    call-argument-start, array-element-start, both ternary branches,
    arrow-body — in one small component, to catch context-interaction
    bugs). `make test`: 295/295 → 297/297 forward + idempotency, zero
    regressions on any existing `.js`/`.ts`/`.tsx` fixture, manually
    re-verified round1/round2 byte-identical on both new fixtures outside
    the Makefile-driven run as well.
  - **Nothing from the design broke on contact** — no new
    character-level-lexer carve-out needed, same as Increments 2/3.
  - **NOT done**: still no real-JSX-corpus validation; `js_ts_content_diff.js`
    still not updated for `.tsx`/JSX `ts.ScriptKind` — both carried over
    from Increments 1/2/3's "NOT done" lists.
  - **Where to resume**: superseded by Increment 5 below (kept for history).

  **2026-08-13 implementation session, Increment 5 — LANDED (8/11
  contexts, still no real corpus validation).** Adds grouping-paren-start
  (design list item 8) to `TokenizerCurly.findJsxSpans`'s `isJsxContext`
  check.

  - **New helper `isGroupingParenStart`**: the mirror image of
    `isCallOpenParen` — recognized when `<` is immediately after a `(` that
    is NOT itself a call-open (i.e. `isCallOpenParen` on that `(` returns
    `false`). Deliberately does not also handle the `,`-inside-a-grouping-
    paren shape (`isCallArgumentOrArrayElementStart`'s enclosing-bracket
    walk already explicitly declines to recognize a `,` enclosed by a
    grouping `(` — grouping parens don't have comma-separated "elements" in
    JS/TS expression grammar the way call args/array elements do, so
    there's no such shape to add).
  - **Ambiguity safety confirmed, but with a wider blast radius than prior
    increments**: same -1-on-unbalanced-JSX self-correcting property, but
    this increment's context check fires much more broadly than Increments
    1-4 — *any* non-call `(` (control-flow parens like `if (...)`/
    `while (...)`/`for (...)`, any bare grouping expression) now gets
    tested. Verified via the fixture's `if (x < 1)` case: `if`'s `(` is not
    preceded by an IDENTIFIER/`)`/`]` (preceded by the `if` KEYWORD), so
    `isCallOpenParen` returns `false` and `isGroupingParenStart` returns
    `true` — `x < 1` is then offered to `findJsxSpanEnd`/`parseJsxTag`,
    which correctly returns -1 (not a balanced JSX tree), leaving the
    tokens untouched. Same safety net every prior increment relies on, just
    exercised on a much higher volume of real-world `(` shapes (nearly
    every control-flow statement in a `.tsx` file) — worth flagging for a
    future corpus-validation pass as the context most likely to reveal a
    performance or correctness edge case at scale, even though none was
    found in this session's own testing.
  - **Fixture-verified**: `test/jsx_tsx_grouping_paren_context_{inp,out}.tsx`
    (a bare `const a = (<span>...</span>);` and a
    `const b = (<div className="b">{a}</div>);` with a `{}` hole; an `if
    (x < 1)` comparison confirmed untouched, doubling as the fallback-safety
    proof above). `make test`: 297/297 → 298/298 forward + idempotency, zero
    regressions on any existing `.js`/`.ts`/`.tsx` fixture, manually
    re-verified round1/round2 byte-identical on the new fixture outside the
    Makefile-driven run as well.
  - **Nothing from the design broke on contact** — no new
    character-level-lexer carve-out needed, same as Increments 2/3/4.
  - **NOT done**: still no real-JSX-corpus validation; `js_ts_content_diff.js`
    still not updated for `.tsx`/JSX `ts.ScriptKind` — both carried over
    from Increments 1-4's "NOT done" lists. The `<T>`-cast-vs-JSX
    ambiguity's `.tsx`-only resolution is still not stress-tested against
    an actual `const x = <T>foo;` cast-shaped `.tsx` input in any of the 8
    now-landed contexts (carried over from Increment 4, not this
    increment's own gap).
  - **Where to resume**: superseded by Increment 6 below (kept for history).

  **2026-08-13 implementation session, Increment 6 — LANDED (10/11
  contexts, item 10 found structurally unreachable at this pass's level,
  still no real corpus validation).** Adds bare `{`-hole-start (design list
  item 9) and spread (design list item 11) to `TokenizerCurly.findJsxSpans`.

  - **Item 9 re-scoped from its literal wording, with the reasoning
    recorded here so a future session doesn't re-litigate it.** The design
    text says "once inside a found JSX span, any `{` opens a hole, and
    every context above applies again to the token stream starting just
    after that `{`" — read literally, this asks for `findJsxSpanEnd`/
    `skipBalancedBraceHole` to recurse *while walking an already-matched
    outer span* and independently re-run `isJsxContext` inside each hole.
    Traced through concretely before implementing: `findJsxSpans`'s outer
    loop already scans **every** significant token in the whole file for a
    `<` satisfying `isJsxContext`, regardless of `{}`/`()`/`[]` nesting
    depth — nesting only matters for a `<` that has *already* been consumed
    into an earlier-collapsed `JSX_SPAN` token (removed from `sig`), and a
    consumed span's raw `text` already preserves everything inside it
    (including any nested hole's JSX) byte-for-byte, since the whole
    span is frozen/opaque and never reformatted. So a `<` inside a hole
    that is itself inside an already-matched outer span needs no separate
    detection — it's already correctly preserved either way, and
    recursing there would be a costly no-op. The one genuine gap: a `<`
    immediately after a bare `{` was not in any existing check at all —
    `{<Bar/>}` (a JSX element as the sole content of a hole, nothing else
    preceding it) previously fell through unmatched. Implemented as a
    plain `Token.isPunct(prev, "{")` addition to `isJsxContext`, same shape
    as the existing `(`/`[` checks, rather than the heavier recursive-walk
    machinery the literal wording implies — same self-correcting -1
    fallback safety net covers the (very common) non-JSX case of `{` opening
    an ordinary block statement or object literal.
  - **New helper `isSpreadContext`** (item 11): token immediately before
    `<` must be `...`, and the token before *that* must satisfy the exact
    same shape `isCallArgumentOrArrayElementStart` already tests (`(`/`[`/
    a top-level `,` whose enclosing bracket is a call-open `(` or any `[`)
    — reused one token further back rather than duplicated, confirming the
    design's own "likely close to free" prediction.
  - **Item 10 (template-literal `${}` holes) found structurally
    unreachable at this pre-pass's level, not merely unimplemented.**
    Traced `emitTemplateLiteral`/`skipTemplateInterpolation`
    (character-level lexer, runs before tokenization ever produces a flat
    token list): a whole template literal, including every `${...}`
    interpolation inside it, is already swallowed into one opaque STRING
    token before `findJsxSpans` (a post-tokenize pass) ever sees the token
    stream — there is no separate token for `${`/`}` or the interpolation's
    interior for this pass to recurse into. Recognizing JSX inside a
    template hole would require the character-level lexer itself to stop
    treating `${...}` as fully opaque (emit separate tokens for the
    interpolation interior, re-entering ordinary character-level scanning)
    — the same class of upstream-lexer surprise flagged in Increment 1's
    `isRegexLiteralAllowedHere` fix, but larger in scope: that fix was one
    narrow special case, this would change how template literals tokenize
    generally. Left unimplemented as out of scope for this increment;
    tracked as the one remaining item for a future session willing to take
    on a tokenizer-level (not `findJsxSpans`-level) change.
  - **Ambiguity safety unchanged from every prior increment**: same
    -1-on-unbalanced-JSX self-correcting property throughout.
  - **Fixture-verified**: `test/jsx_tsx_hole_spread_context_{inp,out}.tsx`
    (a `return`-context `<div>` whose child hole's sole content is another
    JSX element, `<div>{<span>nested</span>}</div>`; a spread call argument
    `foo(...items, <span>tail</span>)`; a spread array element
    `[...items, <span>tail</span>]`; an `if (x < 1)` comparison confirmed
    untouched). `make test`: 298/298 → 299/299 forward + idempotency, zero
    regressions on any existing `.js`/`.ts`/`.tsx` fixture, manually
    re-verified round1/round2 byte-identical on the new fixture outside
    the Makefile-driven run as well.
  - **NOT done**: still no real-JSX-corpus validation (react itself,
    `create-react-app` output, or similar); `js_ts_content_diff.js` still
    not updated for `.tsx`/JSX `ts.ScriptKind`; the `<T>`-cast-vs-JSX
    ambiguity's `.tsx`-only resolution still not stress-tested against an
    actual `const x = <T>foo;` cast-shaped `.tsx` input — all three carried
    over from prior increments' "NOT done" lists.
  - **Where to resume (superseded by the 2026-08-13 scoping session below
    for item 10's own breakdown — kept for history; still accurate on the
    real-JSX-corpus-validation point, which the scoping session below does
    not touch).** 10/11 design-list contexts now landed. Only item
    10 (template-literal `${}` holes) remains, and it needs a
    tokenizer-level change (see above), not another `isJsxContext` clause
    — treat it as its own, larger-scoped future task rather than a natural
    "next increment" in this series. Absent that, the highest-value
    remaining work on this whole sub-job is the long-deferred
    real-JSX-corpus validation pass (react itself or a `create-react-app`
    output tree) against all 10 landed contexts together, flagged as
    required-but-not-done since the original 2026-08-12 design session and
    repeated unchanged in every increment since.

  **2026-08-13 scoping session — item 10 (template-literal `${}` holes)
  broken into sub-contexts (design/scoping only, no code, no fixtures, no
  RDD_LOG key).** Increment 6 above left item 10 as one deferred blob
  ("needs a tokenizer-level change, not another `isJsxContext` clause").
  This section breaks that down into a concrete, incremental checklist a
  future implementation session can pick up, following the same
  small-individually-testable-individually-committed-increment pattern
  Increments 1-6 already established. Grounded in a re-read of
  `TokenizerCurly.emitTemplateLiteral` (character-level lexer, ~line 1354),
  `skipTemplateInterpolation`/`skipNestedTemplateLiteral`/
  `skipQuotedForTemplate` (~line 1540 onward), and the dispatch-loop call
  site (`c == '`' && (lang.isJs || lang.isTs)`, ~line 603) — not
  speculation.

  **Why this is structurally different from Increments 1-6, and riskier.**
  Every one of Increments 1-6 added a new *condition* to
  `findJsxSpans.isJsxContext`, a post-tokenize pass that only ever runs when
  `lang.isJsxSyntax` — zero blast radius on plain `.js`/`.ts` files by
  construction (the pass itself never runs there), confirmed empirically
  every increment via the unchanged `make test` count on non-`.tsx`
  fixtures. Item 10 cannot be scoped that narrowly at the same layer:
  `emitTemplateLiteral`/`skipTemplateInterpolation` run in the
  character-level lexer, for **every** JS/TS file regardless of extension
  (template literals are ordinary, common JS/TS syntax, not a JSX-only
  construct) — see the dispatch-loop condition above, gated only on
  `lang.isJs || lang.isTs`, never on `lang.isJsxSyntax`. Any change to how
  `${...}` is scanned therefore touches the tokenization of every `.js`/
  `.ts`/`.jsx`/`.tsx` file with a template literal in it, not just the
  `.jsx`/`.tsx` subset — a categorically larger surface than any prior
  increment, and the reason this needs its own carefully-planned session
  rather than a "Increment 7" drop-in.

  **Sub-context 0 (prerequisite, blocks everything below): decide the
  opaque-vs-transparent boundary.** Before any tokenizer change, decide
  exactly what stays opaque and what doesn't:
  - Today, `emitTemplateLiteral` emits ONE `STRING` token for the entire
    literal, backticks and all interpolations included, and
    `skipTemplateInterpolation` never emits any token for a `${...}`
    interior — it only advances `pos` past it (brace/quote/nested-template
    balanced skip, no re-entry into `emitIdentifierOrKeyword`/
    `emitOperator`/etc.).
  - The minimal change that unblocks JSX detection inside a hole:
    `skipTemplateInterpolation` must instead **tokenize** each `${...}`
    interior (re-enter the normal per-character dispatch used by the main
    scan loop, the same set of `emit*` branches at ~line 570-650) rather
    than just skipping raw characters, while the backtick-delimited
    non-interpolation text on either side of each hole stays exactly as
    opaque as it is today.
  - This means a single template literal with N interpolations would need
    to become a **sequence** of tokens instead of one `STRING` token: text
    segment, hole-open marker, the hole's own real tokens, hole-close
    marker, text segment, ... — a structural change to what
    `emitTemplateLiteral` returns (today: exactly one `Token`; after: an
    unknown-in-advance number of tokens spliced into the main stream at the
    call site, ~line 604). `emitTemplateLiteral`'s signature/return type
    and its one call site both need to change together — decide up front
    whether it becomes `void` (pushing tokens directly onto the caller's
    list) or returns a `List<Token>` the caller splices in, matching
    whichever existing multi-token-emission precedent (if any) this
    tokenizer already has, rather than inventing a new return-shape
    convention.

  **Sub-context 1: new `TokenType`s for the hole boundary.** The design's
  original list-item-1 wording ("recursively inside a JSX expression hole's
  own `{...}` ... same recursion trigger as a JSX `{...}` hole") implies the
  post-tokenize `findJsxSpans` pass needs to recognize a template-hole's
  `${`/`}` the same way it already recognizes an ordinary `{`/`}` PUNCT
  pair for a JSX-hole's own bare-brace context (item 9, landed in Increment
  6 as `Token.isPunct(prev, "{")`). Two representational options, to be
  decided by the implementation session rather than assumed here:
  - (a) Emit the `${` and the hole's closing `}` as ordinary `PUNCT`
    tokens with distinguishable `text` (`"${"` / `"}"`), letting
    `findJsxSpans`'s existing `isJsxContext`/`isPunct` machinery treat a
    `${`-opened hole exactly like a `{`-opened JSX hole (item 9) with no
    new `TokenType` needed at all — cheapest option, but conflates a
    template hole's `${` with a bare `{`'s different surrounding-text
    semantics (a template hole is never a block statement or object
    literal, so nothing downstream should ever need to tell them apart) —
    verify no existing pass relies on `${` vs `{` being distinguishable
    before picking this option.
  - (b) A dedicated `TokenType.TEMPLATE_HOLE_OPEN`/`TEMPLATE_HOLE_CLOSE`
    pair, mirroring the existing `JSX_SPAN`/`ANGLE_BRACKET_OPEN`/
    `ANGLE_BRACKET_CLOSE` precedent of dedicated non-generic token kinds —
    safer (no ambiguity with plain `{`/`}`), more invasive (every pass that
    already switches on `TokenType`/`Token.isPunct` for brace matching
    needs an audit for whether it now also needs to handle this new kind,
    the same class of "audit every fits-check call site" work the design's
    original `JSX_SPAN` write-up already flagged for multi-line-token
    width guards).
  - Recommendation for the implementation session to validate, not a
    settled decision: start with (a) (reuse `PUNCT`) since it is the
    smaller, more mechanically-checkable change, and only fall back to (b)
    if (a) is found to break an existing brace-matching assumption during
    implementation — mirrors this job's own established "smallest safe
    change first" pattern from Increments 1-6.

  **Sub-context 2: nested template literals inside a hole.** The design
  session flagged `` `a ${ `b ${c}` } d` `` (a nested template literal
  inside an outer hole, legal JS) as a real question. Traced against the
  current code: `skipTemplateInterpolation` already calls
  `skipNestedTemplateLiteral` on an inner `` ` ``, which itself recurses
  back into `skipTemplateInterpolation` for the inner template's own
  `${...}` holes — the *skip*-only version already handles arbitrary
  nesting depth correctly today (it just never tokenizes any of it). Once
  sub-context 0's re-entry-into-real-tokenization change lands, this
  recursive structure is naturally preserved AS LONG AS the re-entry point
  itself calls back into `emitTemplateLiteral` (not a separate, simpler
  path) when it encounters a nested `` ` `` while tokenizing a hole's
  interior — i.e. the character-level dispatch loop's own `` c == '`' ``
  branch (~line 603) must be reachable from inside hole-tokenization, not
  bypassed. Concretely: verify (with a dedicated fixture,
  `` `a ${ `b ${c}` } d` `` plus a JSX-bearing variant `` `a ${ `b ${<X/>}` }` ``)
  that a JSX span found inside a doubly-nested hole round-trips correctly
  before considering nesting "done" — this is exactly the kind of case
  static reasoning alone could get wrong.

  **Sub-context 3: scope to `.jsx`/`.tsx` only, or all JS/TS?** This is the
  single highest-leverage risk-reduction decision available, and should be
  made explicit and deliberate rather than defaulted:
  - **Narrow-but-honest option**: gate the new tokenize-instead-of-skip
    behavior in `skipTemplateInterpolation` on `lang.isJsxSyntax`, exactly
    like Increment 1's `isRegexLiteralAllowedHere` carve-out — a plain
    `.js`/`.ts` file's template-literal tokenization stays byte-for-byte
    unchanged (still one opaque `STRING` token, `skipTemplateInterpolation`
    keeps its current skip-only behavior), and only `.jsx`/`.tsx` files pay
    the cost/risk of the new tokenize-the-hole behavior. This directly
    mirrors this job's own established "narrow, gated" precedent and
    contains the regression surface to the same file-extension boundary
    every other increment already relies on — **strongly preferred**
    unless a concrete reason surfaces during implementation that the two
    code paths can't be cleanly forked.
  - **Wide option (NOT recommended without much heavier validation)**:
    change `skipTemplateInterpolation`'s tokenization behavior
    unconditionally for every JS/TS file. Rejected as the default plan
    here specifically because template literals are ubiquitous in ordinary
    `.js`/`.ts` code — unlike every Increment 1-6 context (each scoped to
    JSX-only files by construction), a bug introduced here could regress
    any of the 200+ non-JSX `.js`/`.ts` fixtures and any real-code dogfood
    corpus previously validated, not just the handful of `.tsx` fixtures.
    Only worth considering if a future session finds the gated/ungated
    code paths are so awkward to maintain in parallel that unifying them
    is worth the much larger validation burden described below.

  **Sub-context 4: regression-test plan, sized to the risk.** Whichever
  option sub-context 3 lands on, this change touches template-literal
  tokenization directly, so the validation bar must be higher than any
  prior increment's:
  - Every existing `.ts`/`.js` fixture containing a template literal must
    be re-verified byte-identical (forward + idempotency), not just
    `make test`'s aggregate pass count — a per-fixture `grep -l '\`'
    test/*_inp.{js,ts}` sweep first, to know which fixtures actually
    exercise this code path before trusting a green `make test` as
    sufficient evidence.
  - If the narrow (`.jsx`/`.tsx`-gated) option from sub-context 3 is taken,
    confirm via the same method Increment 1 used (`make test`'s count
    unchanged on every non-`.tsx` fixture) that the gate is a true
    zero-behavior-change no-op outside JSX/TSX files — the load-bearing
    claim the narrow option's whole risk argument rests on.
  - New fixtures needed, mirroring Increments 1-6's per-context fixture
    pattern: a template literal with a JSX-bearing hole in each of the
    (already-landed) 10 expression-start contexts is not required — item
    10 is its own context, not a cross-product with the other 10 — but at
    minimum: a bare `` `text ${<Foo/>} more` ``, a hole containing plain
    non-JSX JS (confirming the re-entered tokenization doesn't corrupt
    ordinary interpolation expressions — the single most likely regression
    class, since sub-context 0's change touches how *every* hole is
    tokenized, not just JSX-bearing ones), the nested-template case from
    sub-context 2, and an `if (x < 1)` (or similar) comparison inside a
    hole confirmed untouched (the same self-correcting-fallback sanity
    check every prior increment's fixture includes).
  - Real-corpus validation (react/create-react-app or similar) remains
    required before trusting this complete, same carried-over gap every
    increment since the 2026-08-12 design session has flagged — item 10
    adds one more reason it's needed: template literals with JSX holes are
    common in real React code (e.g. styled-components-style tagged
    templates, conditional JSX-in-template patterns) in a way none of the
    10 already-landed contexts' own fixtures can stand in for.

  **Suggested increment breakdown for a future session** (each its own
  commit per this job's checkpoint-commit convention, not a prescription to
  follow rigidly): (1) sub-context 0's return-shape decision + the
  mechanical `emitTemplateLiteral`/call-site restructuring with NO new
  tokenization yet (interpolation interior still just skipped, but now via
  explicit segment tokens instead of one opaque `STRING` — a
  behavior-preserving refactor, verifiable by itself via unchanged `make
  test`); (2) re-entry into real per-character tokenization for a hole's
  interior, gated per sub-context 3's narrow option, verified against the
  non-JSX-hole fixture from sub-context 4 first (prove ordinary
  interpolation expressions still round-trip before ever testing JSX
  inside one); (3) the actual JSX-detection wiring (`findJsxSpans`
  extended to recognize a template hole's boundary per sub-context 1),
  verified against the JSX-in-template fixtures; (4) the nested-template
  fixture from sub-context 2; (5) the real-corpus validation pass, ideally
  covering all 11 contexts together at that point, not item 10 in
  isolation. Steps (1)-(2) carry essentially all of this task's risk and
  should not be combined with step (3) in one commit, even though step (3)
  is the one that actually delivers item 10's user-visible behavior — this
  mirrors the parent task's own instruction to keep each increment small
  and individually testable.

  **Dogfood candidates registered 2026-08-13 (NOT STARTED — for the boundary-
  finding pre-pass as a whole, not item 10 specifically; full row detail in
  STATE_DOGFOOD.md).** Real, verified-to-exist repos, one small / one small-
  but-more-complex-embedded-tag / one larger, per language:
  - JSX: `taniarascia/react-tutorial` (small), `ruanyf/react-demos` (small,
    many demo dirs with heavier conditional-rendering/spread JSX — stresses
    Increments 5/6's grouping-paren and spread contexts specifically),
    `reactstrap/reactstrap` (larger, plain-JS component library, no
    TypeScript).
  - TSX: `microsoft/TypeScript-React-Starter` (small, official minimal
    starter), `Lemoncode/react-typescript-samples` (small, many samples with
    generics/HOCs — heavier embedded TSX), `excalidraw/excalidraw` (larger,
    popular production TSX codebase).
  These exercise the already-landed 10/11 contexts (opaque JSX-span
  preservation); they do NOT exercise item 10 (template-literal `${}`
  holes) until that sub-context lands, since template-literal-embedded JSX
  is currently just swallowed whole as an opaque `STRING` along with the
  rest of the literal — no crash, but no boundary-finding inside it either.

  **Sub-contexts 0-3 LANDED (2026-08-13, later same day).** Implemented as
  one combined change rather than sub-context 0/1 as two separate commits —
  the narrow `.jsx`/`.tsx`-only gate from sub-context 3 was applied from the
  very first line of code (not bolted on afterward), since the scoping
  session already called it "strongly preferred" and this minimizes risk
  the same way doing it later would have, with less churn.

  - **Sub-context 0.** `TokenizerCurly.tokenize`'s per-character dispatch
    chain was extracted out of its `while` loop into a new
    `tokenizeOneUnit(List<Token>)` method, so a hole's interior can re-enter
    real tokenization by simply calling it in a loop. The backtick dispatch
    branch now calls a new `emitTemplateLiteral(List<Token>)` that pushes
    tokens directly (`void`-returning, splicing at the call site was
    rejected — pushing straight into the shared `tokens` list matches how
    every other multi-token emission in this tokenizer already works, e.g.
    comments/PREPROCESSOR runs) instead of returning one opaque token. The
    old single-opaque-token body survives unchanged as
    `emitTemplateLiteralOpaque()`, still the only path for plain `.js`/`.ts`.
  - **Sub-context 1.** Option (a) (`PUNCT` tokens with `"${"`/`"}"` text)
    was tried first per the scoping session's stated preference, but broke
    real code within minutes of smoke-testing: several existing passes
    (`JsTsSpecificRule`, `MiscRuleCore`) check `isPunct(t, "}")` for
    statement/ASI-boundary purposes without verifying a matching real `{`
    precedes it, and a hole's closing `"}"` is textually indistinguishable
    from a real block/object close, corrupting output around
    `${x+1}` (spurious inserted `;` and mangled newlines). Fell back to
    option (b) immediately: dedicated `TokenType.TEMPLATE_HOLE_OPEN`/
    `TEMPLATE_HOLE_CLOSE`, emitted by `emitTemplateLiteralSegmented`/
    `emitTemplateHoleInterior` (new methods in `TokenizerCurly`), with the
    hole's own terminating `}` consumed directly (never routed through
    `emitCloseBrace`, so the tokenizer's global `braceDepth` is untouched by
    it) while any real nested `{`/`}` inside the hole falls through to
    ordinary dispatch and does participate in `braceDepth` normally.
    `findJsxSpans`'s `isJsxContext` gained one new disjunct — JSX is allowed
    to start right after a `TEMPLATE_HOLE_OPEN` — verified against the new
    `jsx_tsx_template_hole_context_inp/out.tsx` fixture (bare JSX as a
    hole's sole content, a plain non-JSX interpolation confirming the
    pre-existing `${a+b}`-style spacing-normalization feature keeps firing,
    and a ternary mixing a real `<` comparison with JSX branches).
    Discovered along the way: that pre-existing spacing-normalization
    feature (`JsTsSpecificRule.enforceTemplateLiteralInterpolationSpacing`)
    is text-based and only matches a single opaque STRING token starting
    with a backtick, so it silently stopped firing for `.tsx` once template
    literals became segmented there. Fixed with a token-based parallel path
    (`findMatchingTemplateHoleClose`/`renderTemplateHoleInterior` in
    `JsTsSpecificRule`) that reformats the real interior tokens between a
    `TEMPLATE_HOLE_OPEN`/`CLOSE` pair via the existing `renderTokens` helper.
  - **Sub-context 2.** The scoping session predicted nested template
    literals would "fall out of this structure for free" since
    `skipTemplateInterpolation`'s pre-existing recursive structure already
    handles arbitrary nesting depth. That held for *tokenization* (a nested
    backtick naturally re-enters `emitTemplateLiteral` via
    `tokenizeOneUnit`'s own dispatch, no special-casing needed) but **not**
    for the new *rendering* path added to satisfy sub-context 1's
    `.tsx`-regression fix above: `renderTemplateHoleInterior`'s first
    version folded a nested hole into one synthetic token correctly, but
    still pushed the nested literal's own surrounding STRING segments into
    the significant-token list as separate entries; `renderTokens` then
    spaced adjacent STRING tokens apart as if they were two unrelated value
    expressions (not literal text meant to be glued together verbatim),
    corrupting `` `a ${ `b ${x+1}` } d` `` into
    `` `a ${`b  ${x + 1} `} d` `` on the first pass and growing by one more
    space on every subsequent pass (a real, cumulative, non-idempotent
    bug — `make test`'s aggregate count stayed green throughout since no
    prior fixture exercised this scenario; it was caught only by the
    manual nested-literal smoke test sub-context 2 itself required). Fixed
    by folding a nested literal's *entire* segment chain — every one of its
    own STRING pieces and holes, recursively — into a single synthetic
    STRING token before it ever reaches the significant-token list, so it
    participates in adjacency decisions exactly as it would have if the
    tokenizer had never segmented it. Verified clean and idempotent via the
    new `jsx_tsx_template_hole_nested_inp/out.tsx` fixture (plain nested
    template, JSX-bearing nested template, `if (x < 1)` safety net).
  - **Sub-context 3.** Confirmed as a true no-op outside `.jsx`/`.tsx`:
    `emitTemplateLiteral` dispatches to the untouched
    `emitTemplateLiteralOpaque()` single-token path whenever
    `!lang.isJsxSyntax`. Verified explicitly (not just via the aggregate
    `make test` count) by sweeping every existing fixture containing a
    backtick — `js_combined_inp.js`, `js_nested_template_literal_inp.js`,
    `real_code_regressions_94_inp.js`, `real_code_regressions_177_inp.ts`,
    `js_comments_inp.js`, `real_code_regressions_93_inp.ts` — all `.js`/
    `.ts`, all individually confirmed PASS (byte-identical) after this
    change.

  `make test`: 299/299 forward + 299/299 idempotency before this work;
  301/301 forward + 301/301 idempotency after (the +2 new fixture pairs),
  with the pre-existing 299 unaffected. Sub-context 5 (real-corpus/dogfood
  validation) and the wide/ungated option remain explicitly out of scope,
  per the parent task.

  **2026-08-13 scoping session — context 11 (Step 2: generic
  grouped-expression-style wrap of a `JSX_SPAN`'s interior) (design/scoping
  only, no code, no fixtures, no RDD_LOG key).** See the Scope section above
  for the two-step framing; this section is Step 2's own breakdown, written
  in the same style as item 10's scoping session above, grounded in a
  re-read of the real `findJsxSpans`/`parseJsxTag`/`findJsxSpanEnd` code in
  `TokenizerCurly.java` (around line 1974 onward) and the real
  `enforceCallLineBreaking`/`renderCallCandidate` code in
  `MiscRuleCurly.java` (around line 1174 onward) — not speculation.

  **This is NOT a 12th detection context — state this explicitly up
  front.** Items 1-10 in the original design list are all *detection*
  contexts: places a `<` can start a JSX tree, each checked by
  `findJsxSpans.isJsxContext`. "Context 11" here is a fundamentally
  different kind of list item — a *rendering* concern that applies
  uniformly to every already-detected `JSX_SPAN` token, regardless of which
  of the 10 detection contexts caught it. There is no `isJsxContext` clause
  for context 11 and there never will be one, because it is not a question
  of "where can a JSX tree start" — that question is already fully
  answered by items 1-10. A future reader should not go looking for one.

  **Sub-context 1: what minimal internal structure does a `JSX_SPAN` need
  to expose before any generic wrap logic can apply to it?** Today
  `findJsxSpans` collapses the entire matched token range into one opaque
  `Token(TokenType.JSX_SPAN, text, ...)` with `frozen = true` — the
  constituent tokens are gone; only the raw concatenated `text` survives
  (see `TokenizerCurly.java` lines 2014-2024). `enforceCallLineBreaking`/
  `renderCallCandidate` operate on a `List<Token>` slice between a `(` and
  its matching `)`, splitting on top-level commas
  (`splitTopLevelCommas`) to get "the breakable units." A `JSX_SPAN` has no
  such comma-separated argument list and no `List<Token>` interior at all
  to hand to that machinery. The minimal viable structure proposed: do NOT
  attempt to tokenize a JSX tree's children or attribute *values* into real
  sub-tokens. Instead, teach `findJsxSpans`/`parseJsxTag` to additionally
  record, alongside the frozen span, the *opening tag's* attribute-boundary
  offsets it already walks past today (the `while(s < n)` attribute-scanning
  loop in `parseJsxTag`, lines 2146-2160, already visits each attribute in
  sequence via its own `localBrace`-aware scan — it just currently only
  measures balance to find the tag's closing `>`/`/>`, discarding the
  boundary positions it passes through). Emitting those boundaries as byte
  offsets into the span's `text` (or as a parallel list of substrings) is
  additive to the existing frozen-token shape — the `JSX_SPAN` token itself
  can stay exactly as it is for every consumer that doesn't care about
  wrapping (assignment-RHS width checks, call-argument placement, etc. all
  keep treating it as one atomic unit). Everything from the tag's `>` (or
  `/>`) onward — all children, all text, all nested JSX — stays exactly as
  opaque as it is today; only the opening tag's attribute list gets any new
  structure at all. This mirrors item 10 sub-context 0's opaque-vs-
  transparent boundary decision: decide the absolute minimum that must stop
  being opaque, and freeze everything else exactly as hard as before.

  **Sub-context 2: the JSX-whitespace-is-significant hazard — the single
  biggest risk specific to Step 2.** Item 10 never had to deal with this.
  Inside JSX children (anything between a tag's `>` and its matching
  `</Tag>`), whitespace — including newlines — can be semantically
  meaningful under JSX's own whitespace-collapsing rules (leading/trailing
  whitespace on a line is stripped, but whitespace between elements on the
  same line is preserved as a single space, and these rules differ from
  ordinary JS/TS cosmetic-formatting whitespace in ways this formatter's
  existing rendering machinery has no concept of). Reformatting text that
  crosses this boundary risks changing rendered output — a categorically
  worse failure mode than a cosmetic diff, because it is a behavior change
  the formatter is supposed to never produce for any language. Recommendation:
  scope Step 2 so it can **only ever touch the opening tag's attribute
  list** — never reflow, re-wrap, or re-emit a single byte of anything from
  the tag's own closing `>`/`/>` onward. This is a strict subset of the
  already-frozen span (sub-context 1's proposal already only exposes
  attribute-boundary offsets, nothing about children), so this constraint
  falls out of sub-context 1's own scoping rather than needing separate
  enforcement — but it must be stated as an explicit, deliberate policy
  here so a future implementation session doesn't feel invited to extend
  "just a bit further" into self-closing-tag children or nested elements
  once the attribute-wrap machinery exists and looks reusable. A
  self-closing tag (`<Foo attr1={x} attr2={y} />`, `kind == 2` in
  `parseJsxTag`) has no children at all and is the safest, most complete
  case Step 2 can fully handle; a tag with real children
  (`<Foo attr1={x}>...</Foo>`) can still have its opening tag's attribute
  list wrapped under this scope, with the children segment (from `>` to
  `</Foo>`) copied through verbatim, untouched.

  **Sub-context 3: reuse vs. new machinery for the actual wrap decision.**
  `enforceCallLineBreaking`/`renderCallCandidate` are built around
  `parseSignature`/`Param`'s C/C++/Java-style typed "[type] name [size]"
  shape (for telling a declaration from a call) and, once past that,
  comma-separated argument rendering (Options 1-3: drop, preserve, one-per-
  line). JSX attribute syntax is comma-*less* (attributes are separated by
  bare whitespace) and has three shapes with no call-argument analogue:
  bare boolean attributes (`disabled`), spread attributes (`{...props}`),
  and expression-valued attributes (`attr={expr}`, itself an arbitrary,
  possibly-multi-token expression rather than a single value token). Trying
  to force these through `parseSignature`'s typed-declaration path would be
  actively wrong (there is no "type"/"name" pair to extract, and the
  existing Kotlin/JS-TS carve-out in `renderCallCandidate`'s own doc
  comment already establishes the precedent that a language without a
  prototype-only declaration shape must never be routed through that typed
  path). Recommendation: write a dedicated but still generic (non-JSX-
  grammar-aware) wrap function specifically for a whitespace-separated
  attribute-candidate list, reusing only the *shape* of
  `enforceCallLineBreaking`'s decision ladder (single-line-fits → no
  change; else drop; else one-attribute-per-line) and its general
  line-length/fits-check helpers, not `renderCallCandidate` itself or
  `parseSignature`/`Param`. This keeps the "generic long-expression wrap"
  spirit the Scope section calls for (reusing the *machinery*, i.e. the
  wrap-decision shape and its width-measuring helpers) without forcing
  JSX's syntactically different attribute list through comma-splitting
  logic it doesn't have.

  **Sub-context 4: interaction with item 10 / template-literal-embedded
  JSX.** Since item 10's sub-contexts 0-3 landed earlier the same day, a
  `JSX_SPAN` can now appear inside a template-literal hole (e.g.
  `` `text ${<Foo attr1={x} attr2={y} attr3={z} />} more` ``, tokenized via
  `TEMPLATE_HOLE_OPEN`/`renderTemplateHoleInterior` in `JsTsSpecificRule`).
  Traced against the current code: `renderTemplateHoleInterior` reformats a
  hole's *interior tokens* via the existing `renderTokens` helper, and a
  `JSX_SPAN` sitting inside that interior is just one more token in the
  list to that helper — exactly as it already is for every other Step-1
  detection context (call-argument position, assignment-RHS, etc.), all of
  which also just place the frozen span into an existing token-rendering
  path with no special-casing. Step 2's future wrap logic, once it exists,
  would be invoked wherever any `JSX_SPAN` is found in the token stream
  regardless of which container (top-level statement, call argument,
  template hole) it sits inside — it doesn't need any different treatment
  for the template-hole case specifically. Finding: no special-casing
  needed, already falls out — same pattern as item 10 sub-context 2's own
  nested-template finding.

  **Sub-context 5: scope gate.** Restated explicitly for symmetry with item
  10 sub-context 3, though this one is close to a non-decision: Step 2's
  wrap logic by construction only ever touches an already-detected
  `JSX_SPAN` token, and a `JSX_SPAN` token only ever exists when
  `lang.isJsxSyntax` is true (`findJsxSpans` is only called from `tokenize`
  under that guard — see line 572). There is no separate gate to add;
  Step 2 inherits Step 1's `.jsx`/`.tsx`-only scope automatically and
  cannot regress a plain `.js`/`.ts` file no matter how it's implemented,
  the same zero-blast-radius-by-construction property every Step 1
  increment already relies on.

  **Sub-context 6: regression-test plan, sized to the risk.** New fixtures
  needed once implementation starts, at minimum: a long attribute list that
  should wrap (exceeds `line-length`), a short attribute list that should
  NOT wrap (stays on one line even though the tag itself is "long" in some
  naive sense), a spread-attribute case (`{...props}` mixed with ordinary
  attributes), a bare boolean-attribute case (`disabled`, no `=`), an
  expression-valued attribute case (`attr={a.b.c(x, y)}`, an arbitrary
  nested expression, to confirm the wrap logic doesn't need to understand
  what's inside `{...}`, only where it balances), and — direct consequence
  of sub-context 2's hazard — an explicit "children text must be provably
  byte-identical before and after" check on every fixture that has
  children at all (not just an incidental byte-for-byte pass, but a named,
  deliberate assertion in the fixture's own documentation that this is
  what's being verified). Real-corpus validation remains required before
  trusting this complete — the same carried-over gap every scoping session
  in this job has flagged: the JSX/TSX dogfood repos already registered in
  `STATE_DOGFOOD.md` (`taniarascia/react-tutorial`, `ruanyf/react-demos`,
  `reactstrap/reactstrap`, `microsoft/TypeScript-React-Starter`,
  `Lemoncode/react-typescript-samples`, `excalidraw/excalidraw`).

  **Suggested increment breakdown for a future implementation session**
  (each step individually testable/committable, same spirit as item 10's
  breakdown, starting with the narrowest possible slice): (1) sub-context
  1's minimal structure — extend `parseJsxTag` to also record attribute-
  boundary offsets alongside the frozen `JSX_SPAN`, with NO wrap logic and
  NO behavior change yet (a detect-and-measure-only step: can the formatter
  tell a `JSX_SPAN`'s opening tag is over-width, verified via a debug
  assertion or test, before ever emitting a single new line break) — the
  direct analogue of item 10's own step (1) (structural change first,
  behavior-preserving, verified by unchanged `make test`); (2) the actual
  wrap-decision function from sub-context 3, applied only to the
  self-closing-tag case (no children at all) first — the strictly safer
  half of sub-context 2's scope; (3) extend to tags with children, with the
  byte-identical-children assertion from sub-context 6 as the gating test
  for this step specifically; (4) the spread/boolean/expression-attribute
  fixtures from sub-context 6, one at a time; (5) the real-corpus
  validation pass. Steps (1)-(2) carry the readiness-proving risk and
  should not be combined with step (3) in one commit, since step (3) is the
  one that actually exercises the whitespace-significance hazard
  sub-context 2 flags as the whole risk profile's crux.

  **2026-08-13 implementation session — Step 2, Increment 1 of 5
  (detect-and-measure-only) (LANDED).** Implements exactly step (1) of the
  suggested increment breakdown immediately above: sub-context 1's minimal
  structure, with **no wrap logic and no behavior change** — increments 2-5
  (the actual wrap-decision function, self-closing-tag wrapping,
  children-bearing-tag wrapping, and real-corpus validation) remain **NOT
  STARTED**, do not read this section as Step 2 being complete.

  - **`Token` gained two new nullable/default-`-1` fields**,
    `jsxOpeningTagEndOffset` (int) and `jsxAttrBoundaries`
    (`List<Integer>`), populated only for `JSX_SPAN` tokens
    (`TokenizerCore.java`) — additive to the existing frozen/opaque `text`
    shape per sub-context 1's own recommendation; every other token kind is
    unaffected (`-1`/`null` defaults, never read by any pre-existing pass).
  - **`TokenizerCurly.parseJsxTag`** now also records each top-level
    (`localBrace == 0`) attribute's raw-token-index start position for an
    open/self-close tag — a plain `IDENTIFIER` (bare boolean attribute, or
    the name half of `name=value`) or a `{` at `localBrace == 0` (a spread
    attribute, `{...props}`) — returned via a new `attrRawTokenIndices`
    field on `JsxTagResult`. Not consulted by anything inside
    `parseJsxTag`/`findJsxSpanEnd` itself — purely additional data for the
    caller.
  - **`TokenizerCurly.findJsxSpans`** re-parses the already-confirmed-valid
    root tag (a second, side-effect-free `parseJsxTag` call on the same
    short token range findJsxSpanEnd already walked) once a span is about
    to be emitted, and converts `attrRawTokenIndices` into offsets relative
    to the span's own `text` (0 == the leading `<`), storing them on the
    new `Token` fields: `jsxOpeningTagEndOffset` = offset of the character
    immediately after the opening tag's closing `>`/`/>`;
    `jsxAttrBoundaries` = parallel offset list, one per attribute, in
    source order.
  - **New class `com.jxmake.formatter.tokenizer.JsxWrapDiagnostics`**
    (`JsxWrapDiagnostics.java`) — the "narrow, clearly-internal mechanism"
    called for by the parent task, explicitly documented as increment-1
    scaffolding, not user-facing. Two `AtomicInteger` counters
    (`measuredCount`, `overWidthCount`), a `reset()` for test isolation, and
    `recordOpeningTagMeasurement(int openingTagWidth, int lineLengthLimit)`
    which increments `measuredCount` always and `overWidthCount` when
    `openingTagWidth > lineLengthLimit`. Nothing in this class ever mutates
    a token or any formatter output.
  - **`FormatterCurly.formatOne`** calls
    `JsxWrapDiagnostics.recordOpeningTagMeasurement` once per `JSX_SPAN`
    token with a valid `jsxOpeningTagEndOffset`, gated on
    `lang.isJsxSyntax`, inside the existing `tokenizer` lambda (so it fires
    on every re-tokenize pass, same as every other pass in that lambda —
    harmless for a purely observational counter). `lineLengthLimit` was
    hoisted a few lines earlier in the method (was previously computed
    after the lambda) so the lambda's closure can see it.
  - **Documented increment-1 approximation** (stated explicitly in
    `JsxWrapDiagnostics`'s own doc comment, not silently assumed): the
    measurement compares only the opening tag's own raw width
    (`jsxOpeningTagEndOffset`) against `line-length`, not the tag's actual
    rendered column position (current indentation + any preceding same-line
    tokens). A real column-aware fits-check is rendering-time machinery
    that belongs to a future wrap-implementing increment, not this
    detect-only slice.
  - **Verification method** (no JUnit harness in this repo — matches
    `STATE_COMMON.md`'s "prefer evidence over reasoning" debug-print
    convention): a small standalone `Verify.java` (not committed —
    scratch-only, per `STATE_COMMON.md`'s `/tmp` guidance) compiled against
    `target/classes` and run directly, calling
    `new FormatterCurly(lang).formatOne(...)` after `JsxWrapDiagnostics.reset()`
    and printing `measuredCount()`/`overWidthCount()`. Against the new
    fixture below (one over-width tag, one under-width tag): `measured=100
    overWidth=50` — the exact 2:1 ratio expected (the file is re-tokenized
    50 times across the format pipeline, and of the file's 2 `JSX_SPAN`
    tokens exactly 1 is over-width every time). Against a pre-existing
    fixture with no over-width tags
    (`jsx_tsx_grouping_paren_context_inp.tsx`): `measured=100 overWidth=0`
    — confirms no false positives.
  - **Fixture-verified**: `test/jsx_tsx_wrap_detect_context_{inp,out}.tsx`
    (`<VeryLongComponentName attributeOne={valueOne} attributeTwo={valueTwo}
    attributeThree={valueThree} />` — an opening tag whose attribute list
    exceeds the default 100-char `line-length`; `<Small a={1} />` — a short
    tag that doesn't; an `if (x < 1)` comparison confirmed untouched).
    `make test`: 306/306 → 307/307 forward + idempotency — the fixture's
    own `_out.tsx` is byte-identical to what the pre-existing formatter
    already produced for that input (verified by generating it fresh with
    the new code and confirming no JSX content was altered), proving the
    zero-output-visible-change guarantee this increment requires. Zero
    regressions on any other existing fixture.
  - **NOT done** (explicitly, so a future session doesn't overclaim): no
    actual line-breaking/wrapping of any kind (increments 2-5 of Step 2's
    breakdown); no column-aware (indentation + preceding-token) width
    measurement, only the opening tag's own raw width; no real-JSX-corpus
    validation of this new measurement against react/create-react-app-scale
    input (carried over from every prior Step 1 increment's own "NOT done"
    list, unchanged); `JsxWrapDiagnostics` has no wired-in CLI/server-visible
    reporting — it is purely an internal counter read back by hand or by a
    future test harness, not a feature.
  - **Where to resume (superseded by Increment 2 below — kept for
    history).**

  **2026-08-14 implementation session — Step 2, Increment 2 of 5 (real
  wrap, self-closing tags only) (LANDED).** Implements step (2) of the
  suggested increment breakdown: the actual wrap-decision function from
  sub-context 3, applied only to a self-closing `JSX_SPAN` with no children
  at all (sub-context 2's "strictly safer half") — increments 3-5
  (children-bearing tags, spread/boolean/expression-attribute fixtures,
  real-corpus validation) remain **NOT STARTED**.

  - **`JsTsSpecificRule.enforceJsxSelfClosingAttributeWrap`** (+ private
    helper `renderJsxSelfClosingWrapCandidate`) — mirrors
    `MiscRuleCurly.enforceCallLineBreaking`'s decision-ladder shape (fits on
    one line → no change; else wrap) without routing through that method's
    comma-split/typed-signature machinery, per sub-context 3's own
    recommendation (JSX attributes are whitespace-separated, not
    comma-separated, and have no `Type name` shape to parse). A `JSX_SPAN`
    is recognized as "self-closing, no children" purely via
    `jsxOpeningTagEndOffset == text.length()` (the whole span text IS the
    opening tag — a tag with real children always has trailing
    child/closing-tag text past that offset, so this check alone correctly
    excludes them without any separate "has children" flag). Width uses the
    existing `lineColumnOf` helper (rendered column up to the span) plus the
    span's own raw text length — same increment-1-approximation posture
    documented on `JsxWrapDiagnostics` (no indentation-of-continuation-lines
    complexity needed for the decision itself, only for the replacement
    text). Wired into `FormatterCurly.format`'s Phase 4, immediately after
    `switchRule.formatNonInlineSwitches`, gated on `lang.isJsxSyntax`.
  - **Real bug found and fixed as a prerequisite**:
    `TokenizerCurly.parseJsxTag`'s existing `attrRawTokenIndices` collection
    (built by Increment 1 of Step 2, unconsumed until now) recorded a `{`
    at `localBrace == 0` as a fresh attribute boundary unconditionally —
    correct for a spread attribute (`{...props}`) but wrong for an ordinary
    `name={value}` attribute's own value-hole open brace, which is the SAME
    attribute as the identifier immediately before it, not a second one.
    Left unnoticed by Increment 1 since detect-only mode never actually
    consumed the boundary list for rendering. Fixed with
    `isValueHoleOpenBrace` (a `{` immediately preceded by `=` at
    `localBrace == 0` is skipped, not recorded) — found immediately via this
    increment's own real-code smoke test (`attr={x}` wrongly split into
    `attr=` and `{x}` as two separate wrapped lines before the fix).
  - **Fixture-verified**: `jsx_tsx_wrap_detect_context_out.tsx` (Increment
    1's own fixture) updated — its wide tag now genuinely wraps
    one-attribute-per-line instead of staying byte-identical to the input,
    the intended, expected effect of landing real wrap behavior on top of
    detect-only measurement; its narrow tag is unchanged. New fixture pair
    `test/jsx_tsx_self_closing_wrap_{inp,out}.tsx`: a single over-width
    attribute wraps with `/>` on its own closing line; a zero-attribute
    over-width tag stays on one line (nothing to wrap); an over-width tag
    WITH children is left completely untouched (still out of scope).
    `make test`: 307/307 → 308/308 forward + idempotency, zero regressions
    on any other existing fixture. Manually verified round1/round2
    byte-identical on both fixtures outside the Makefile-driven run too.
  - **NOT done**: children-bearing tags (Increment 3); spread/boolean/
    expression-attribute-specific fixtures (Increment 4 — this increment's
    fixtures only exercise plain `name={expr}` attributes, since the
    boundary-detection bug fix above was itself found via that shape); no
    real-JSX-corpus validation (Increment 5) — all carried over unchanged
    from every prior increment's own "NOT done" list.
  - **Where to resume (superseded by Increment 3 below — kept for
    history).**

  **2026-08-14 implementation session — Step 2, Increment 3 of 5 (extend to
  children-bearing tags) (LANDED).** Implements step (3) of the suggested
  increment breakdown: extends the same wrap-decision function to a root
  `JSX_SPAN` that has real children (an opening tag, not just a
  self-closing one) — increments 4-5 (spread/boolean/expression-attribute
  fixtures, real-corpus validation) remain **NOT STARTED**.

  - **`JsTsSpecificRule.renderJsxSelfClosingWrapCandidate`** (method name
    kept for continuity with the Increment 2 pipeline wiring, despite now
    handling both shapes) — generalized from Increment 2's self-closing-only
    check (`jsxOpeningTagEndOffset == text.length()`) to accept ANY root
    `JSX_SPAN` with `jsxOpeningTagEndOffset >= 0` and a non-empty
    `jsxAttrBoundaries` list. No tokenizer change was needed:
    `TokenizerCurly.findJsxSpans` already populated both fields for an
    opening tag (`rootTag.kind == 0`) as well as a self-closing one
    (`kind == 2`) back in Increment 1 — only the render method itself had
    the artificially narrow self-closing-only gate.
  - **Width measurement fix (found while designing this increment, not a
    regression)**: Increment 2's width check used
    `lineColumnOf(...) + t.text.length()`, i.e. the ENTIRE span's raw text
    including any children. For a self-closing tag this is identical to the
    opening tag's own width (there is no child text), so Increment 2 never
    noticed. For a children-bearing tag this is wrong — measuring the whole
    subtree means a short opening tag with huge/multi-line children would
    spuriously "overflow" (or a real multi-line child block's own newlines
    would corrupt the column arithmetic entirely). Fixed by measuring
    `openingTagWidth = t.jsxOpeningTagEndOffset` (an int offset, not the
    full text length) — this is the same approximation
    `JsxWrapDiagnostics` (Increment 1) already uses for its own
    over-width counter, so this fix brings the real wrap decision back in
    line with that established precedent rather than introducing a new one.
  - **Splice mechanism**: `text` is split into `openingTagText =
    text.substring(0, jsxOpeningTagEndOffset)` (rewritten, one attribute
    per line, same as Increment 2) and `tail = text.substring(
    jsxOpeningTagEndOffset)` (children + closing tag, when present — empty
    string for a self-closing tag). `tail` is appended to the wrapped
    output completely unmodified — no re-tokenizing, no trimming, no
    whitespace normalization of any kind — this IS sub-context 2's
    byte-identical-children guarantee, enforced structurally (the code
    physically cannot touch those bytes, not just "chooses" not to).
    The opening tag's own closing marker is now detected per-tag
    (`openingTagText.endsWith("/>")` → `/>` on its own closing line;
    otherwise a bare `>` on its own closing line, with `tail` — starting
    with the children — appended directly after with no extra newline
    inserted, since `tail`'s own leading whitespace/newlines, if any, are
    already whatever the source author wrote and must not be touched).
  - **Fixture-verified**: `test/jsx_tsx_self_closing_wrap_{inp,out}.tsx`
    extended with three new cases (kept the two Increment 2 cases plus the
    old "children untouched" case, renamed `WithChildrenShort` since it's
    now a genuine "opening tag alone still fits, so no wrap" case rather
    than an "out of scope" one): `WithChildrenWrapped` (over-width opening
    tag, short single-expression child `{child}`) wraps the opening tag
    with a bare `>` on its own line, `{child}...` spliced back on
    immediately after; `WithMultilineChildrenPreservedVerbatim` (over-width
    opening tag, real multi-line JSX children — nested `<span>`,
    deliberately irregular internal whitespace, an embedded `.map()`
    expression) wraps the opening tag while every byte of the children
    section comes through unchanged — verified directly via `--diff`
    showing the children/closing-tag lines untouched, and round-trip
    (`--in-place` twice) byte-identical output, in addition to the
    Makefile-driven `make test` run: 308/308 forward + 308/308
    idempotency, zero regressions on any other existing fixture (no new
    fixture files needed registering — the pair was already registered by
    Increment 2, only its content changed).
  - **NOT done**: spread/boolean/expression-attribute-specific fixtures
    (Increment 4 — every fixture through this increment only exercises
    plain `name={expr}` attributes); no real-JSX-corpus validation
    (Increment 5) — both carried over unchanged from every prior
    increment's own "NOT done" list.
  - **Where to resume (superseded by Increment 4 below — kept for
    history).**

  **2026-08-14 implementation session — Step 2, Increment 4 of 5 (spread/
  boolean/expression-attribute fixtures) (LANDED).** Implements step (4) of
  the suggested increment breakdown: fixture-only, no source changes —
  confirms the wrap logic landed in Increments 2-3 needs no real
  JSX-grammar understanding for any attribute shape, only the existing
  brace-only balance-tracking already in `parseJsxTag` — Increment 5
  (real-corpus validation) remains **NOT STARTED**.

  - **No source changes.** `parseJsxTag`'s existing boundary-detection
    logic (an IDENTIFIER or a non-value-hole `{` at `localBrace == 0`
    starts a new attribute; only `{`/`}` are tracked for nesting, not
    `()`/`.`) already generically covers spread attributes, bare boolean
    attributes, and expression-valued attributes with nested calls/member
    access inside their value hole — this increment's job was proving that
    via real fixtures, not writing new handling.
  - **Fixture-verified**: new pair `test/jsx_tsx_attr_kinds_wrap_{inp,
    out}.tsx`, four self-closing cases (self-closing chosen deliberately to
    isolate attribute-kind handling from the children-splice mechanism
    Increment 3 already covers): a spread attribute
    (`{...somePropsObjectThatIsQuiteLong}`) wraps as one segment; a bare
    boolean attribute (`disabledBecauseOfSomeReason`, no `=`) wraps as a
    plain identifier alone on its line; an expression-valued attribute
    whose value contains nested `()`/`.`
    (`onClick={handlers.click.bind(this, item.id)}`) wraps as one segment
    without the inner parens confusing the brace-only balance tracker; a
    mixed tag combining spread + boolean + expression + plain attributes in
    one tag wraps each onto its own line in source order. Verified
    round1/round2 byte-identical outside the Makefile-driven run too.
    Registered in `Makefile`'s `INP_FILES` and described in
    `test/README.txt`. `make test`: 308/308 → 309/309 forward + 309/309
    idempotency, zero regressions on any other existing fixture.
  - **NOT done**: real-JSX-corpus validation (Increment 5) — carried over
    unchanged from every prior increment's own "NOT done" list.
  - **Where to resume (superseded by Increment 5 below — kept for
    history).**

  **2026-08-14 implementation session — Step 2, Increment 5 of 5 (real-corpus
  validation) (LANDED — all 6 corpora done; Step 2 complete).** Implements
  step (5), the final increment of the suggested breakdown: dogfood every
  JSX/TSX corpus already registered in
  `STATE_DOGFOOD.md`, alternating JSX/TSX repo-by-repo per user direction,
  re-running the two corpora already marked DONE there (that DONE status
  predates all of Step 2 — it only ever validated Step 1's boundary
  detection, never the wrap logic Increments 2-4 landed on top of it).

  - **`taniarascia/react-tutorial` (JSX, re-run)**: reused the cached
    `/tmp/dogfood_react_tutorial` checkout. All 5 `.js` files round1/round2
    idempotent, `js_ts_syntax_check.sh` 5/5 clean. No line in this corpus
    naturally exceeds `line-length` with a JSX opening tag, so the wrap
    logic itself never engaged here — the pass still validates that
    Increments 2-4 introduced no regression on real code that doesn't
    happen to trigger them.
  - **`microsoft/TypeScript-React-Starter` (TSX, re-run)**: fresh clone
    (`/tmp/ts_react_starter_dogfood`, prior checkout no longer cached). Same
    10 `.tsx` files as the original pass, `--preserve-tree`. Round1/round2
    idempotent, `js_ts_syntax_check.sh` 10/10 clean. Same finding as
    react-tutorial: no wrap-triggering line in this corpus either.
  - **`ruanyf/react-demos` (JSX)**: fresh clone (`/tmp/react_demos_dogfood`).
    Real JSX in this repo lives almost entirely inside `.html` files'
    `<script type="text/babel">` blocks, which `XmlSpecificRule.
    JS_SCRIPT_TYPES` deliberately doesn't recognize as JS (`text/babel`
    isn't in that allowlist) — out of this job's scope, a separate
    HTML5 script-dispatch concern, left untouched by design, not a gap.
    Of the repo's few standalone `.js` files, only `demo13/src/browser.js`
    and `demo13/src/app.js` contain real JSX (`demo13/{app,server,
    browser}.js` at the repo root are already-compiled babel OUTPUT with no
    JSX at all, just `React.createElement` calls). Both real-JSX files are
    round1/round2 idempotent and `js_ts_syntax_check.sh`-clean. Found one
    **unrelated pre-existing bug**, not caused by any JSX/Step-2 code:
    `demo13/app.js` (compiled, non-JSX, minified one-liner function bodies)
    is NOT idempotent — a general curly-brace-family reindent pass
    re-breaks an already-reformatted `function f(...) { ... }` one-liner
    differently on a second pass. Filed as a known finding, not fixed here
    (general curly-family issue, unrelated to any JSX content, out of this
    job's scope — would belong to STATE_C_CPP_JAVA.md's curly-family work
    or a new dedicated investigation).
  - **`Lemoncode/react-typescript-samples` (TSX)**: fresh clone
    (`/tmp/lemoncode_dogfood`, 329 `.tsx` files total). Sampled 15 files
    across `hooks/` and `old_class_components_samples/` (seeded `shuf`,
    excluding `.spec.`/`.test.` files) rather than the full set, matching
    this corpus's much larger size relative to the others. All 15
    round1/round2 idempotent, `js_ts_syntax_check.sh` 15/15 clean. Two files
    had multi-line, one-attribute-per-line JSX matching the wrap output
    shape (`hooks/07_ColorPicker/src/app.tsx`,
    `old_class_components_samples/03 Navigation/src/components/header.tsx`)
    — confirmed via diff against the original that this was the AUTHOR'S
    own pre-existing formatting, not something the wrap logic newly
    produced (no line in this sample naturally overflows `line-length`).
  - **`reactstrap/reactstrap` (JSX)**: fresh clone (`/tmp/reactstrap_dogfood`,
    108 `.js` files under `src/`, excluding `__tests__/`). Ran the FULL set
    (small enough not to need sampling), `--preserve-tree`. **Found a real,
    reproducible content-corruption bug** (not a wrap-logic issue — a Step 1
    detection gap): `parseJsxTag` required a tag-name `IDENTIFIER`
    unconditionally, so JSX fragment shorthand (`<>...</>`, no tag name at
    all) was never recognized as JSX by `findJsxSpans` in the first place.
    `DropdownToggle.js` had `return <>{returnFunction({ ref: this.context.
    onToggleRef })}</>;` — with the fragment invisible to JSX detection,
    its `{...}` content fell through to ordinary JS statement-level
    formatting, which wrongly inserted a semicolon INSIDE the expression
    hole: `{returnFunction(...)}}` → `{returnFunction(...);}` — an actual
    behavior change, exactly the class of bug real-corpus validation exists
    to catch. **Fixed** in `parseJsxTag`: when the token immediately after
    `<`/`</` is `>` (not an `IDENTIFIER`), it's a fragment — `tagNameStr` is
    given an empty-string sentinel (`""`) instead of returning `null`, so
    `findJsxSpanEnd`'s existing tag-identity check (`expected.equals(
    r.tagName)`) already pairs `<>`...`</>` correctly with no other logic
    changes anywhere (fragments never have attributes, so the wrap logic in
    `JsTsSpecificRule` never engages on one either — `jsxAttrBoundaries` is
    always empty for a fragment root). New regression fixture
    `test/jsx_tsx_fragment_shorthand_{inp,out}.tsx`: the exact corrupted
    shape (bare-expression fragment child), a multi-child fragment, and a
    fragment nested inside a normal element's children — all three
    round-trip byte-identical. `make test`: 309/309 → 310/310 forward +
    idempotency, zero regressions. Re-ran the full 108-file reactstrap sweep
    against the fixed jar afterward: fully idempotent, zero errors,
    `DropdownToggle.js` now syntax-clean (only pre-existing false-fail
    remaining is `index.js`, a `js_ts_syntax_check.sh` limitation on legacy
    `export X from 'Y'` syntax — confirmed present on the ORIGINAL
    unformatted file too, not a formatter bug).
  - **`excalidraw/excalidraw` (TSX)**: fresh clone (`/tmp/excalidraw_dogfood`,
    303 `.tsx` files under `packages/`/`examples/`, excluding
    `node_modules/`). Sampled 17 files: the 7 files whose lines naturally
    exceed `line-length` inside a JSX opening tag (real wrap-trigger
    candidates: `CustomFooter.tsx`, `AI.tsx`, `BraveMeasureTextError.tsx`,
    `EyeDropper.tsx`, `icons.tsx`, `DefaultItems.tsx`,
    `WelcomeScreen.Hints.tsx`) plus 10 randomly sampled others (seeded
    `shuf`, excluding `examples/` and `.test.` files), matching the
    sampling approach used for the similarly large Lemoncode corpus.
    Round1/round2 fully idempotent (`diff -rq` empty, confirmed twice), zero
    formatter errors. **Found a second real, reproducible
    content-corruption bug**, unrelated to JSX detection or wrapping at
    all: `enforceSemicolonInsertion`'s depth counter (which decides whether
    a `NEWLINE` is a genuine statement boundary) tracked `(`/`[`/expression-
    `{` but never `TEMPLATE_HOLE_OPEN`/`TEMPLATE_HOLE_CLOSE` (a template
    literal's `` ${...} `` hole boundary tokens). A `NEWLINE` immediately
    after `` ${ `` inside a multi-line hole (e.g. a wrapped ternary) was
    therefore evaluated at depth 0 as if it were a real statement boundary,
    and `needsSemicolonAfter` had no exclusion for `TEMPLATE_HOLE_OPEN`
    either, so a stray `;` got appended directly onto the `` ${ `` token.
    Real code: `packages/excalidraw/components/SearchMenu.tsx` had
    `` const matchCount = `${searchMatches.items.length} ${\n  cond\n    ? a\n    : b\n}`; ``
    — round1 output corrupted the second hole into
    `` `${searchMatches.items.length} ${;\n...` ``, a genuine parse-breaking
    change (`js_ts_syntax_check.sh` caught it as "Expression expected" /
    "Unterminated template literal", the only one of the 7 syntax-check
    failures in this sample whose ORIGINAL file passed clean — the other 6
    fail identically on their originals too, the same
    `js_ts_syntax_check.sh` legacy-syntax limitation seen in prior corpora,
    confirmed via direct per-file original-vs-formatted comparison and left
    as-is). **Fixed** in `JsTsSpecificRule.enforceSemicolonInsertion`:
    `TEMPLATE_HOLE_OPEN` now pushes/increments depth exactly like `(`, and
    `TEMPLATE_HOLE_CLOSE` pops it exactly like `)`; `needsSemicolonAfter`
    also now explicitly returns `false` for `TEMPLATE_HOLE_OPEN` as a
    defensive belt-and-suspenders in case `lastSigIdx` ever lands there via
    another path. New regression fixture
    `test/jsx_tsx_template_hole_wrap_{inp,out}.tsx`: the exact corrupted
    shape (multi-line ternary in a `` ${} `` hole with a sibling `` ${} ``
    before it) plus a simpler single-expression multi-line hole — both
    idempotent, no semicolon ever inserted inside either hole. `make test`:
    310/310 → 311/311 forward + idempotency, zero regressions. Re-ran the
    full 17-file excalidraw sample against the fixed jar afterward: still
    fully idempotent, and `SearchMenu.tsx` now syntax-clean — only the same
    6 pre-existing (original-file-also-fails) checker-tool-limitation
    failures remain.
  - **Step 2 status: all 5 increments landed.** Generic grouped-expression
    wrapping of an overlong `JSX_SPAN` opening tag's attribute list is
    complete: self-closing and children-bearing tags, all attribute kinds
    (plain, spread, boolean, expression-valued), validated idempotent and
    syntax-clean across 6 real-world JSX/TSX corpora (2 re-runs + 4 new),
    with two genuine content-corruption bugs found and fixed along the way
    (JSX fragment-shorthand detection gap; template-literal-hole semicolon-
    insertion gap) — exactly the class of defect real-corpus validation
    exists to catch, on both occasions unrelated to the wrap logic itself.

  **2026-08-13 research session — JSX-in-`.js`/`.ts` detection (open
  question from the react-tutorial dogfood finding) (design/research only,
  no code, no fixtures, no RDD_LOG key).** Follows on from the
  `taniarascia/react-tutorial` dogfood pass above (see "Checklist" ->
  `taniarascia/react-tutorial` and `microsoft/TypeScript-React-Starter`
  dogfood pass" section, and the Open Questions entry "Should JSX detection
  ever extend to plain `.js`/`.ts` files") — that pass found a real repo
  shipping JSX embedded directly in `.js` files with no `.jsx` extension,
  which corrupts under the current strictly-extension-gated
  `Lang.isJsxSyntax`. This session researches how established tooling
  handles the same ambiguity and proposes (but does not implement) a
  strategy.

  **1. How real tooling handles it.**
  - **Babel / `@babel/parser`**: JSX parsing is opt-in at the parser-options
    level, not extension-sniffed by the bare parser API itself —
    `@babel/parser` only attempts JSX grammar when the caller passes
    `plugins: ['jsx']` (or `sourceType`/preset config that implies it);
    without that plugin a bare `<` in expression position is a syntax
    error. The bare parser has no notion of file extension at all (it
    receives a string, not a path) — extension-based decisions are made by
    the *caller* (Babel's own `babel-preset-react`/`.babelrc` resolution,
    or downstream tools), not by `@babel/parser` itself.
  - **Prettier**: confirmed via Prettier's documented parser-selection
    behavior — Prettier's default parser for `.js`/`.mjs`/`.cjs`/`.jsx` is
    `babel`, and Prettier always passes the `jsx` plugin when invoking
    `@babel/parser` for that parser, regardless of extension (there is no
    "JS-only, non-JSX-capable" mode of the `babel` parser in Prettier's own
    parser table). For `.ts` Prettier instead uses the `typescript`
    parser (via `@typescript-eslint`/TS's own scanner shape) with JSX
    disabled, and only enables JSX parsing for `.tsx`. This confirms the
    task's framing: Prettier's JS-family handling is "attempt JSX
    unconditionally," but its TS-family handling is extension-gated exactly
    like `tsc` itself (see next point) — Prettier does not treat `.js` and
    `.ts` uniformly.
  - **TypeScript compiler (`ts.createSourceFile`)**: confirmed — TS's
    `ScriptKind` enum has distinct `JS`/`JSX`/`TS`/`TSX` values, and JSX
    element syntax (`<div>`) is a parse error under `ScriptKind.TS`
    (reported by `tsc` as "JSX element ... has no corresponding closing
    tag" or similar, in a `.ts` file that isn't `.tsx`), while the
    angle-bracket type-assertion syntax `<Foo>expr` is itself *rejected* in
    `ScriptKind.TSX` for the opposite reason (ambiguity with a JSX open
    tag) — this is a well-known, deliberate TypeScript restriction, not an
    oversight: the TS team's own public position (TS handbook / release
    notes for the `.tsx` extension) is that `<Type>value`-style casts are
    disallowed in `.tsx` specifically because they're syntactically
    indistinguishable from a JSX open tag, and users must use `as Type`
    instead in that extension. TS resolves the ambiguity purely by
    extension/`ScriptKind`, never by content-sniffing — this project's own
    `js_ts_content_diff.js` wrapper already reflects this (its docstring:
    "JSX/TSX are explicitly out of scope... this tool only targets plain
    .js/.ts", and `scriptKindFor` never maps `.jsx`/`.tsx` to a JSX-aware
    `ScriptKind` — see the dogfood section above). Since this formatter's
    own pipeline isn't bound by TS's own restriction (it isn't `tsc`, it's
    a text-preserving reformatter), TS's behavior is useful precedent but
    not a hard constraint here — it does, however, confirm that the
    `<Type>expr` vs. `<Tag>` ambiguity is a real, industry-recognized
    conflict, not a hypothetical.
  - **ESLint / `@babel/eslint-parser`**: no per-file content-sniffing
    either. `eslint-plugin-react` and `@babel/eslint-parser`-based configs
    enable JSX via explicit `parserOptions.ecmaFeatures.jsx = true` (or an
    `overrides` block scoped to a glob such as `**/*.js`) at the
    project-config level — an explicit, author-controlled opt-in matching
    this task's strategy (c), not automatic per-file detection.
  - **Summary**: no mainstream tool researched does runtime content-sniffing
    (scanning file text for `import React`/a pragma comment) to decide
    JSX-ness per file. The two real patterns in use are (i) "attempt JSX
    unconditionally for the whole JS-family bucket, since plain JS has no
    competing ambiguous syntax" (Babel/Prettier's `.js`/`.jsx`/`.mjs`/`.cjs`
    handling), and (ii) "gate strictly on extension/config, because this
    specific language variant (TS) has a real competing syntax" (TS
    compiler's `.ts` vs `.tsx`, Prettier's mirroring of that split, ESLint's
    project-config opt-in).

  **2. False-positive risk in this formatter's own architecture.** Re-read
  `TokenizerCurly.findJsxSpans`/`isJsxContext` (~line 1974),
  `findJsxSpanEnd` (~line 2079), and `parseJsxTag` (~line 2127) for this
  session (not the STATE_JS_TS.md description). Key structural fact,
  confirmed by re-reading `isJsxContext`'s eleven `||`-clauses (return,
  `=>`, `?`, `:`, call-arg/array-element-start, assignment-RHS,
  logical-RHS, grouping-paren-start, JSX-hole `{`, template-hole-open,
  spread): every one of them requires the `<` token to be the **first
  token of a brand-new expression** — immediately after `return`, `=>`,
  `?`, `:`, `(`, `[`, `,` inside a call/array, `=`/compound-assignment,
  `&&`/`||`/`??`, `{`, `${`, or `...`. A genuine relational/comparison `<`
  (`a < b`) or a chained comparison (`a < b > c`, legal JS since each
  comparison yields a boolean re-compared) **never** has its `<` in
  expression-start position — the left operand (`a`) always precedes it,
  so the operand token, not `<` itself, is what sits in the trigger
  position, and the operand is virtually never itself one of the eleven
  trigger tokens. Traced concretely for `a < b > c`: for this to misfire,
  `<` would have to be the token immediately after e.g. `return` — but
  `return a < b > c` has `<` preceded by `a`, not `return`, so
  `isJsxContext` never even fires for the `<`. **This means ordinary
  comparison operators are structurally excluded from all eleven contexts
  by construction, not merely handled by the `-1` fallback** — the fallback
  is a second line of defense, not the first.
  - The **real remaining ambiguity** is TypeScript's own legacy
    angle-bracket type assertion, `<Type>expr` — e.g. `x = <Foo>y` (RHS of
    `=`), `return <Foo>y`, `foo(<Foo>y, z)` (call-arg start) — because a
    cast's `<Type>` is *also* expression-start-shaped and syntactically
    identical to a JSX open tag with no attributes. `parseJsxTag` parses
    `<Foo>` as a well-formed **open** tag (kind 0: tag name found, no `/`
    self-close, immediate `>`) exactly as it would for real JSX, and
    `findJsxSpanEnd` then requires a subsequent *closing* tag (`</Foo>` or
    another self-close bringing depth back to 0) before it will accept the
    span — a cast expression essentially never contains a `</`-shaped token
    sequence afterward, so in real-world non-JSX code this returns `-1`
    (safe fallback) almost every time. This matches TS's own recognition of
    the identical ambiguity (point 1 above) — it isn't a corner case unique
    to this formatter.
  - **A genuine, currently-unguarded gap found this session**:
    `findJsxSpanEnd`/`parseJsxTag` track only tag-nesting **depth**, not
    tag-name identity — `parseJsxTag`'s `kind == 1` (closing tag) branch
    decrements `depth` and `kind == 2`/`depth == 0` returns, but nothing
    anywhere compares the closing tag's name against the opening tag's
    dotted name (confirmed by grep: no `tagName`/`nameEquals`/
    `matchesOpen`-shaped helper exists in `TokenizerCurly.java`). This means
    `<a>...</b>` — a *mismatched* open/close pair — is currently accepted as
    a balanced tree by this pass, exactly as `<a>...</a>` would be. This is
    already true today, gated behind `.jsx`/`.tsx` only, and is presumably
    low-risk there since a `.jsx` file's `<a>...</b>` is already broken JSX
    the author would need to fix regardless. It becomes materially more
    relevant if detection is ever widened to plain `.js`/`.ts`, where the
    thing being (mis)balanced is a legacy cast plus unrelated later code,
    not intentional JSX — see point 4 below for the concrete guard this
    implies.

  **3. Proposed detection strategies.**
  - **(a) Extend unconditionally to `.js`/`.ts` (mirror Babel/Prettier's
    default-on approach).** Per point 1, real tooling does NOT do this
    uniformly — it does it for the JS family only, and deliberately does
    NOT do it for `.ts` (Prettier/tsc both gate `.ts` separately from
    `.tsx` specifically because of the type-assertion collision). Applying
    (a) to `.js`/`.mjs`/`.cjs` is well-supported by precedent and, per
    point 2, the eleven contexts' structural expression-start requirement
    already excludes ordinary comparisons; the residual risk is narrowly
    the `<Type>expr` legacy-cast collision, which is rare in plain
    `.js`/`.mjs`/`.cjs` (that cast syntax doesn't even exist in a
    strictly-JS file — it's TS-only syntax that would already be a syntax
    error there under a JS grammar, so a `.js` file containing `<Foo>y`
    almost certainly *is* JSX, not a cast, unlike `.ts`). Applying (a) to
    `.ts` is NOT well-supported — that's exactly the file type where the
    cast ambiguity is real (legacy TS code does use `<Type>value` casts in
    plain `.ts` files today, `as Type` being the modern preferred form but
    not universally adopted), and both Prettier and `tsc` deliberately
    decline to extend JSX parsing to `.ts` for this reason.
  - **(b) Content-sniff for `import React`/`from 'react'`/a `@jsx` pragma
    comment.** Rejected as unreliable, and confirmed unreliable by a
    verifiable landscape shift: the "new JSX transform" (React 17+,
    `automatic` runtime, the default in `create-react-app`/Next.js since
    their React-17-era releases) explicitly removes the requirement to
    import React at all for JSX to work — the transform injects the
    `jsx`/`jsxs` runtime helper imports automatically. A real, modern
    `.js`/`.tsx` file using JSX under the automatic runtime can have zero
    `React`-related import, so an import-based heuristic would silently
    fail to detect exactly the modern-tooling case, while also
    over-triggering on any file that merely imports `react` for a
    non-JSX reason (e.g. `useContext`/hooks-only files with no JSX at
    all). No mainstream tool researched in point 1 uses this heuristic —
    consistent with rejecting it here too.
  - **(c) Config/CLI opt-in only** (a `--jsx-in-ts` flag, or a
    per-file `JXM_CFMT_CFG --jsx-in-ts=on` directive mirroring the existing
    `--lang` in-file-override precedent, `RDD_KEY_286`). Matches ESLint's
    real-world pattern (project-level explicit opt-in, not per-file
    sniffing) and gives the safest possible blast radius — zero risk to any
    file the user doesn't explicitly mark. Downside: requires the user (or
    a build-tooling integration) to know and act on this per legacy-`.js`
    file or per project, which the react-tutorial-shaped repo's own author
    never would have done (they wrote plain `.js` under an older CRA
    convention with no formatter-specific awareness at all) — so pure (c)
    does not actually fix the dogfood finding's real-world scenario unless
    paired with a broader default.
  - **Recommendation: a hybrid of (a) and (c).** Extend
    `Lang.isJsxSyntax`-equivalent detection (i.e., wire
    `TokenizerCurly.findJsxSpans` to also run) unconditionally for
    `.js`/`.mjs`/`.cjs` — mirroring Babel/Prettier's own JS-family default
    and justified by point 2's finding that the eleven contexts already
    structurally exclude ordinary comparisons, leaving only the
    already-rare (in real `.js`) legacy-cast shape as residual risk. Do
    **not** extend to plain `.ts` by default — mirror TS's/Prettier's own
    deliberate `.ts`/`.tsx` split, since `.ts` is exactly where the
    `<Type>expr` cast collision is real and non-rare; instead expose (c)
    (a `--jsx-in-ts`/`JXM_CFMT_CFG` opt-in) for the `.ts` case, so a user
    with a legacy `.ts`-with-embedded-JSX file (same convention as the
    dogfood repo but TS-flavored) has an explicit, safe path without
    changing the `.ts` default. This is not a full-confidence
    recommendation — it is not yet validated against a real `.js`-with-JSX
    corpus reformatted under the widened default (the react-tutorial `.jsx`
    supplementary check in the dogfood section above validates the
    pre-pass's correctness once engaged, but not this specific
    default-widening decision's real-world false-positive rate at scale) —
    a future implementation session should treat this as a strong starting
    point, not a settled decision, and validate against
    `ruanyf/react-demos` and any other still-`NOT STARTED` dogfood repo
    that ships `.js`-with-JSX before shipping the default change.

  **4. Code-shape decision for a future implementation session, described
  precisely enough to act on directly (not implemented here):**
  - Before widening `findJsxSpans` to run on `.js`/`.mjs`/`.cjs`, harden
    `parseJsxTag`/`findJsxSpanEnd` to check **tag-name identity** between
    an open tag and the closing tag that reduces its depth to 0, not just
    depth balance. Concretely: `parseJsxTag` already walks the dotted tag
    name (`s` advances past `IDENTIFIER (. IDENTIFIER)*` at ~line
    2139-2144) for both open and close tags — capture that name (as a
    `String`, e.g. the concatenated raw text of the identifier/dot tokens)
    into the returned `int[]`-equivalent result (would need to widen the
    return shape, e.g. to a small result object or an `Object[]`/parallel
    `String[]` out-param, since `int[]` can't carry a `String`), and have
    `findJsxSpanEnd` track a stack of open tag names (not just an integer
    `depth`) so a `kind == 1` closing tag is only accepted when its name
    equals the top of that stack; a name mismatch should return `-1` (same
    safe-fallback contract every other rejection already uses), not throw
    or otherwise change error-handling shape. This directly closes the gap
    found in point 2 (`<a>...</b>` currently balances) and specifically
    reduces the residual `.js` cast/JSX-collision risk described in point 3
    — a coincidental later `</SomeUnrelatedName>`-shaped token sequence
    would no longer be enough to wrongly close a misdetected `<Type>`-cast
    span; the closing tag's name would additionally have to coincidentally
    match the cast's type name, which is materially rarer. This hardening
    is worth doing regardless of which strategy from point 3 is chosen,
    since it strictly tightens an existing gap in the already-shipped
    `.jsx`/`.tsx` pass too — but it becomes load-bearing (not just
    nice-to-have) if detection is ever widened past the current
    `.jsx`/`.tsx` gate, since the residual risk in `.js`/`.ts` is
    specifically the shape this closes.

---

  **2026-08-13 implementation session — JSX-in-`.js`/`.ts` detection
  (LANDED).** Implemented the hybrid recommendation from the research
  session directly above, plus recommendation 4's tag-name hardening.

  1. `Lang.isJsxSyntaxPath` widened unconditionally to `.js`/`.mjs`/`.cjs`
     (mirrors Babel/Prettier's own default) in addition to the pre-existing
     `.jsx`/`.tsx`. `.jsx`/`.tsx` behavior is unchanged.
  2. `.ts` deliberately stays gated off by default — same reasoning `tsc`/
     Prettier use (the legacy `<Type>expr` angle-bracket cast collides with
     a JSX open tag).
  3. **Design decision for the `.ts`-scoped opt-in:** a plain new `Config`
     key, `jsx-in-ts`, rather than a special-cased CLI-only flag. Checked
     precedent first — `InFileConfig.isEligible` admits any key satisfying
     `Config.isKnownKey(key) && !SERVER_SCOPED_KEYS.contains(key)`, so a
     new boolean `Config` key automatically works everywhere (CLI flag,
     `JXMAKE_CODE_FORMATTER_*` env var, config file, server query param,
     and `JXM_CFMT_CFG` in-file directive) with no extra plumbing — unlike
     `--lang` (RDD_KEY_286), which needed hand-written special-casing
     specifically because it is *not* an ordinary `Config` key. Since
     `jsx-in-ts` has no such reason to be special, the generic mechanism
     was the right fit; a bespoke `--jsx-in-ts`-only flag would have
     needlessly reinvented what `Config`/`InFileConfig` already provide for
     free. Landed as: `Config.ALL_KEYS`/`GROUPS` (`"JS/TS"` group)/
     `describeOne`/`fromRawMap` entries, a `jsxInTs` field +
     `isJsxInTs()` getter, threaded through `FormatterCore`'s new 3-arg
     `forLanguage(language, filePath, jsxInTsOptIn)` overload and
     `GdrPipelineGate.applyAndFormat` (the single call site both `Main` and
     `ServerMode` route through).
  4. `TokenizerCurly.parseJsxTag` now returns a small `JsxTagResult`
     (`newSigPos`/`kind`/`tagName`) instead of an `int[]`, and
     `findJsxSpanEnd` tracks a `Deque<String>` of open tag names instead of
     a bare integer depth — a closing tag only reduces depth when its name
     matches the innermost open name; any mismatch (`<a>...</b>`, or a
     close with no open at all) returns `-1`, the same safe-fallback every
     other rejection in this pass already uses (never throws, never
     silently accepts).

  **Renamed `jsx-in-js` → `jsx-in-ts` (2026-08-13, same day, user request).**
  The key originally landed as `jsx-in-js` (point 3 above); a user review of
  README.md flagged that name as misleading -- the "js" reads as ".js
  files" even though the key applies exclusively to `.ts` files and has
  zero effect on `.js`/`.jsx`/`.tsx`/`.mjs`/`.cjs`, which detect JSX
  unconditionally regardless of this key. Renamed throughout: `Config`'s
  `ALL_KEYS`/`GROUPS`/`describeOne`/`fromRawMap` entries, the `jsxInTs`
  field + `isJsxInTs()` getter, `Lang`'s/`FormatterCore`'s
  `jsxInTsOptIn` parameter, `GdrPipelineGate`'s call site, the
  `ts_jsx_optin_inp/out.ts` fixture's `JXM_CFMT_CFG` directive, and every
  doc reference (README.md, this file, `test/README.txt`). Pure rename,
  no behavior change -- `make test` still 307/307 forward + idempotency
  after the rename.

  **Two latent bugs found and fixed along the way** (both invisible before
  this session because widening JSX detection to plain `.js` is what first
  activated the `.jsx`/`.tsx`-only segmented template-literal tokenizer
  mode for ordinary `.js` files containing real template-literal
  interpolations — the prior `.jsx`/`.tsx` dogfood/fixture corpus had zero
  such content):
  - `DeclarationAlignmentRuleCore.needsSpaceBetween` (a duplicate of
    `MiscRuleCore`'s own adjacency-spacing method, exercised whenever a
    declaration statement is grouped/rendered by
    `DeclarationAlignmentRuleCurly`/`JsTsDeclarationAlignmentRule`) had no
    `TEMPLATE_HOLE_OPEN`/`TEMPLATE_HOLE_CLOSE` awareness and inserted
    spurious spaces around a template literal's `${`/`}` boundaries (e.g.
    `` `User: ${name}` `` → `` `User:   ${name}  ` ``). Found via
    `DEBUG_PHASES`-gated phase-by-phase tracing (temporary `System.err`
    prints after each `FormatterCurly.formatOne` phase, removed once
    diagnosed) that isolated the corruption to `ScopePipelineCurly.process`
    specifically. Fixed with the same guard shape `MiscRuleCore` already
    uses: treat either side of a template hole boundary as always-tight.
  - `ComplexityPaddingEvaluator.isLoose` counted a real nested call inside
    a template hole's interior (e.g. `` `${pico.green(`x`)}` ``'s inner
    `pico.green(...)` call) toward the *enclosing* call's own looseness
    decision, wrongly padding `console.log(...)` to
    `console.log( ... )` — found via `real_code_regressions_94`, a
    pre-existing fixture with exactly this nested-template-in-call shape.
    Fixed by having `isLoose` skip over a `TEMPLATE_HOLE_OPEN`...
    `TEMPLATE_HOLE_CLOSE` span (nesting-depth-aware) rather than recursing
    into it — the template literal is still one argument value from the
    enclosing call's point of view, exactly as it was before segmentation
    when it was one opaque `STRING` token.

  **Fixtures added** (`test/README.txt` has the full per-fixture
  rationale): `jsx_in_plain_js_inp/out.js` (real JSX in plain `.js`,
  preserved), `ts_jsx_default_off_inp/out.ts` (`.ts`'s legacy `<Type>` cast
  left untouched by default), `ts_jsx_optin_inp/out.ts` (`.ts` + `` /*%
  JXM_CFMT_CFG jsx-in-ts=on */ `` — JSX now detected/preserved),
  `jsx_mismatched_tag_inp/out.jsx` and `js_mismatched_tag_inp/out.js`
  (`<a>text</b>` bails out to plain, non-corrupted formatting in both the
  pre-existing `.jsx` gate and the newly-widened `.js` context). `make
  test`: 306/306 forward, 306/306 idempotency.

  **`taniarascia/react-tutorial` re-dogfooded against the real `.js`
  corpus that motivated this whole effort** (re-cloned fresh to
  `/tmp/dogfood_react_tutorial`, prior cached clone from the original
  finding no longer present): all 5 `.js` files now round-trip clean.
  `js_ts_syntax_check.sh` 5/5 clean (previously 4/5 failed, including the
  `Api.js` truncation/`{entry}`→`{entry;}` corruption that motivated this
  whole effort). Format→format-again diffs empty (idempotent) for all 5
  files. `tools/verifiers/js_ts_content_diff.js`/`.sh` flags 4/5 as
  MISMATCH as expected (documented JSX-non-awareness limitation, not a
  formatter bug) — manually cross-checked via whitespace-stripped diff:
  every reported difference is a legitimate, expected style transform
  (arrow-parameter parens added, semicolons inserted, `// methodName`/`//
  ClassName` closing comments added), not corruption — no lost/garbled
  JSX content, no truncation, no stray tokens. `STATE_DOGFOOD.md`'s
  react-tutorial row updated accordingly.

  **Open Questions entry above (`Should JSX detection ever extend to plain
  .js/.ts files?`) is now IMPLEMENTED** — see that entry's own note.

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
  stale pre- vs. post-call-wrap. Attempt 1 (re-run the pass alone) fixed the
  symptom but exposed the same staleness in `JsTsDeclarationAlignmentRule.
  spansMultipleLines`'s grouping decision. Attempt 2 (full
  `ScopePipelineCurly.process` re-run) fixed the repro but regressed
  `real_code_regressions_100.ts` (collapsed an already-correct `} //
  interface ParserOptions`) plus several Java/Kotlin/cpp26 fixtures —
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
  padding lands, so a candidate wrongly rejoins. Distinct from bug #1's
  `alignBracelessElseIfChain` issue; fixed by RDD_KEY_250.
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
line indented one level *deeper* than its `if` instead (the opposite
direction), which the existing recovery didn't handle, so the chain-scan
breaks and no alignment is applied. Confirmed distinct from the
`processScope` double-pass family (#2/#3): a single indentation value, not
a grouping/padding decision, occurring before `alignBracelessElseIfChain`
runs (a single-pass method). Fix: widened the chain-recovery case to also
strip excess indent when `jIndent > indentLen` for a bare-`else` member
(chain size 1 only), mirroring the existing narrower-`if` recovery in the
opposite direction. Verified: both files individually idempotent; full
`angular/angular` corpus (5394 `.ts`) idempotency-violation count went
9→7, the 2 newly-fixed files exactly `shared.ts`/`directive_outputs.ts`,
zero regressions elsewhere (an initial raw round1-output diff showed 178
differing files, traced to stale GRU classifier weights in the comparison
jar, not this fix, ruled out via 3 independent checks). `make test`
258/258 → 259/259. New fixture: `test/real_code_regressions_184_{inp,out}.ts`.
Kotlin (shares `BlockStructureRule`/`KotlinSpecificRule.alignBracelessElseIfChain`)
re-verified only via `make test`, no dedicated Kotlin corpus re-run.

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
force-reindent step) was checked by inspection — gated by one boolean
untied to which passes run inside the branch, so unaffected. Validated via
A/B rebuild (repro via direct `curl` of the file from GitHub, no full
clone) rather than corpus dogfood. `make test` 259/259 → 260/260. New
fixture: `test/real_code_regressions_185_{inp,out}.ts`. Known unrelated
pre-existing artifact: `js_ts_content_diff.js` flags this file's top-level
`interface`/`class` headers as MISMATCH both before and after the fix — a
content-diff tool gap, not this bug.

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
unavailable); validation scope is the two real files plus the fixture
suite, same precedent as RDD_KEY_270.

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
  **Fixed 2026-08-11:** a JS/TS plain block-bodied method with no return-type
  token (e.g. `isValid() {...}`) now joins adjacent `get`/`set` siblings;
  its empty return-type grid cell pads the name into the shared column without
  changing C++ constructor grouping. `ScopePipelineCurly` normalizes that
  generated leading padding back to the group's source indent on reformat, so
  the result is idempotent. Fixture: `test/real_code_regressions_200_{inp,out}.js`.
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
**22/27 clean**. Remaining 5 files were two further confirmed-intentional,
non-lossy classes: bare single-param arrows gaining parens (3 files,
documented §6 behavior); STYLE.md §4 pre-increment-except-when-post-required
correctly rewriting a standalone/unused for-loop increment (2 files,
`perf/perf.js`/`test/test.js`).

**2026-08-10: FIXED.** Both remaining classes were checker gaps, not
formatter bugs (per the original assessment) — added as two more
`canonicalize` tolerances in `js_ts_content_diff.js` (see "Dogfood Output
Validation" above, tolerances 4/5). The original `/tmp/lodash` checkout no
longer has the triggering `.js` files on disk (only excluded `dist`/
`vendor` trees remain), so verification used 4 synthetic pairs isolating
the exact two shapes plus two negative controls (a dropped statement; a
non-for-loop `x++`/`++x` swap, a genuine semantic difference) — all 4
behaved correctly (tolerated/still-flagged as expected). Net effect: the
checker would now report **27/27 clean** against the original corpus, up
from 22/27 — no change to the formatter itself, `make test` unaffected.

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

1. `||=`/`&&=` tokenizer gap — trivial fix, highest value/difficulty.
2. Union-type-before-`=>` spurious wrap — direct low-risk extension of an
   existing precedent.
3. Call-wrap/collapse vs. alignment-padding ordering — substantially fixed
   (`RDD_KEY_248`/`RDD_KEY_250`); residue tracked under "Active work" above.
4. Backslash-newline CRLF string corruption — real corruption but narrow
   (2 files, old test-harness idiom).

All FIXED (see Category 1/2 above for detail). No fixture-only false
positives found in this pass (direct TS-compiler-API parse-checking + raw
`diff`, not `js_ts_content_diff.js`).

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

### `taniarascia/react-tutorial` and `microsoft/TypeScript-React-Starter` dogfood pass — smallest JSX/TSX boundary-finding pre-pass candidates, DONE (2026-08-13)

First two of the six JSX/TSX candidates registered in the "Dogfood
candidates registered 2026-08-13" note above (see `STATE_DOGFOOD.md`). Run
by explicit instruction as a small first pass before touching the other
four (`ruanyf/react-demos`, `reactstrap/reactstrap`,
`Lemoncode/react-typescript-samples`, `excalidraw/excalidraw`), which remain
`NOT STARTED`, deferred to a future session. No formatter source change
made this pass — both findings below are checker-tool/corpus-shape
observations, not formatter bugs requiring a fix.

**`taniarascia/react-tutorial`** (`/tmp/dogfood_react_tutorial`, shallow
clone). In-scope corpus: 5 files under `src/` (`Api.js`, `App.js`,
`Form.js`, `index.js`, `Table.js`), all `.js`, none `.jsx` — the repo ships
JSX embedded directly inside `.js` files (a common older-CRA convention),
so it does not actually contain any real `.jsx`-extension file despite
being registered as a "JSX candidate."

This is itself the headline finding: the boundary-finding pre-pass is
strictly extension-gated (`Lang.isJsxSyntax` checks `.jsx`/`.tsx` only, by
design — see Scope section above and `RDD_KEY_187`/the class-scoping note).
Running the real corpus exactly as shipped (`.js` extension, untouched)
means the pre-pass never engages at all, and the plain-JS tokenizer has no
JSX awareness. Result: **real content corruption**, not just cosmetic
diffs. Round1 on the as-shipped `.js` files is idempotent (round1/round2
diff empty — the corruption itself is stable, not the freshly-discovered
kind of bug this job usually chases) but `js_ts_syntax_check.sh` fails on
4/5 files (`Table.js`, `Form.js`, `App.js`, `Api.js` — only `index.js`,
which contains no JSX, passes) with real parse errors (`'}' expected`,
`JSX element '...' has no corresponding closing tag`). Manual diff of
`Api.js` shows the file is truncated mid-statement and a spurious `;` is
inserted inside a JSX expression container (`{entry}` → `{entry;}`) — the
`<`/`>`/`{`/`}` of the embedded JSX tags are being misparsed as ordinary
JS operators/braces once the tokenizer runs past the point where it can
recover.

As a supplementary check (not part of the real corpus, but needed to
actually exercise the feature under test), the same 5 files were copied to
`.jsx` extensions and re-run: round1/round2 idempotent (empty diff),
`js_ts_syntax_check.sh` 5/5 clean, and manual diff of all 5 files confirmed
every reformatting is cosmetic-only (brace-on-own-line style, `//
componentDidMount`/`// render`/`// class App` closing-brace comments, bare
single-param arrow parenthesization) with the JSX itself preserved
byte-for-byte in every case, exactly as Step 1's guarantee promises.
`js_ts_content_diff.js`'s batch mode flagged all 4 non-trivial `.jsx` files
as MISMATCH ("non-import top-level statement structure differs") — cross-
checked by hand against the diff and confirmed a **checker limitation, not
a formatter bug**: the tool's own docstring states "JSX/TSX are explicitly
out of scope... this tool only targets plain .js/.ts" and its
`scriptKindFor` never maps `.jsx`/`.tsx` to a JSX-aware `ts.ScriptKind`, so
its own AST parse of a `JSX_SPAN`-bearing file diverges from a plain
canonicalization — this is the tool's documented gap, not evidence of
content loss (confirmed via direct diff instead). No template literals
present anywhere in this corpus (item 10 / `TEMPLATE_HOLE_OPEN` path
untested by this repo).

No source fix attempted for the `.js`-with-embedded-JSX corruption — per
the task's explicit scope, this is exactly the class of finding to
document and leave open rather than chase with a blind fix: extending JSX
detection to plain `.js`/`.ts` files is a real design question (risk of
false-positive JSX detection colliding with legitimate `<`/`>` comparison
operators in ordinary `.js` code that never contains JSX), not a narrow
safe patch. Recorded as `STATE_DOGFOOD.md`'s `DONE - OPEN Q` status.

**`microsoft/TypeScript-React-Starter`** (`/tmp/dogfood_ts_react_starter`,
shallow clone). In-scope corpus: 10 real `.tsx` files under `src/`
(`index.tsx`, `App.tsx`, `App.test.tsx`, `components/Hello.tsx`,
`components/Hello.test.tsx`, `constants/index.tsx`, `reducers/index.tsx`,
`containers/Hello.tsx`, `types/index.tsx`, `actions/index.tsx` —
`--preserve-tree --root` used to avoid `Hello.tsx`/`index.tsx` basename
collisions across subdirectories). Unlike the JSX repo above, every file
here already has the real `.tsx` extension, so the boundary-finding
pre-pass engages exactly as intended with no supplementary rename needed.

Round1/round2 idempotency: empty diff, 10/10. `js_ts_content_diff.js`
batch mode: 10/10 `OK: content preserved` (no checker-tool limitation hit
here, unlike the `.jsx` case above). `js_ts_syntax_check.sh`: 10/10 clean.
Manual spot-check of `components/Hello.tsx` (an interface plus a function
component with a JSX return) confirms all diffs are ordinary cosmetic
reformatting (interface member `:`-column alignment grid, brace-on-own-line
style, `// Hello`/`// interface Props` closing-brace comments,
`if(x) throw ...;` single-statement collapse) with JSX content untouched.
No template literals present anywhere in this corpus either (item 10
untested by this repo too — neither of these two smallest candidates
exercises the template-literal-with-JSX path; a future session's dogfood
pass against the remaining 4 registered repos, especially the "small-but-
complex" ones, should specifically watch for it, per the parent task's own
instruction).

**Verdict:** `microsoft/TypeScript-React-Starter` — clean `DONE`, zero
issues, real corpus. `taniarascia/react-tutorial` — `DONE - OPEN Q`: the
Step 1 pre-pass itself is confirmed correct and content-preserving when it
engages (`.jsx` supplementary check), but the real, as-shipped corpus never
triggers it at all and suffers real content corruption as plain `.js` —
worth a documented Known Limitation and a design discussion (own tracked
question, not blocking Step 1/Step 2 work) about whether `.js`/`.ts` files
containing embedded JSX should ever be detected heuristically, deferred to
the user.

### Known false positives (no source change needed, fixture-only)

- Spurious-looking blank line after a class's opening `{` in older `.js`
  fixture drafts, and doubled trailing space before a one-liner getter
  body's closing `}` — both confirmed correct, existing behavior (STYLE.md
  §7 named-construct blank line; `GetterSetterRuleCurly`'s group-width body
  padding), matching passing C++/Java/Kotlin fixtures byte-for-byte. Only
  the stale hand-authored `.js` draft fixtures were wrong; resolved by
  regenerating them.
