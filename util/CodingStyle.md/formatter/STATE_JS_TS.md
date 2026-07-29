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
`normalize-comment-start-case` behavior, not a bug). Comments are recovered
separately from raw source via `ts.getLeadingCommentRanges` (TS's AST doesn't
attach them as tree nodes), scanned at every node's `getFullStart()` plus
position 0/EOF, deduplicated by `[pos, end)`.

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

**js_combined/js_comments activation:**
- Comment inside a destructuring pattern (`{ id, // note\n name }`) dropped
  on reformat — `significantOnly()` hid it from `parseDestructuringDeclaration`'s
  pattern scan; fixed by scanning raw tokens and bailing (leaving multi-line
  form untouched) when an interior comment is found.
- ASI vs. declaration-alignment-grid phase ordering — alignment-grid pass ran
  before `enforceSemicolonInsertion`, so ASI-reliant declarations (no
  explicit `;`) fell back to raw input; fixed by reordering.
- Array-destructuring `,`→`...` missing space (`[a, b,... c]`) —
  `MiscRuleCore.parseAssignment` (JS/TS-unaware) misparsed destructuring as a
  plain assignment; fixed with a `const`/`let`/`var` bail-out (mirrors C++'s
  `auto [a, b] = expr;` bail-out).

**ts_combined/ts_comments activation:**
- `Map<string,number>` ASI bug — `GENERIC_SAFE_KEYWORDS` missing TS
  primitive type keywords (`string`/`number`/`boolean`/`any`/`unknown`/
  `never`/`object`/`undefined`/`null`); fixed by extending the set.
- Enum last-member (no trailing comma) bug — `parseEnumMembers` bailed the
  whole enum on any depth-0 NEWLINE; changed to `break`.
- Generic-argument comma spacing — new pass `enforceGenericArgumentCommaSpacing`.
- New pass `enforceUnionTypeContinuationIndent` for multi-line
  `type X = A | B | C;` alias continuation indent; bug fixed during triage:
  RHS depth-scan bailed on any comment in the span (should only bail on
  frozen tokens).
- New feature `enforceClassFieldAlignmentGrid` (class-field `:`/`=`
  alignment, previously unimplemented). Two bugs fixed in
  `rewriteClassFieldGroups`: double-indented first field (raw-copy loop
  copied WHITESPACE tokens, fixed to NEWLINE only); duplicate blank line
  before a group's first field with a leading comment.

**expressjs/express (first real-code pass, 141 `.js` files, HEAD `ae6dd37`):**
two bugs found via `node --check`, both fixed (fixture
`real_code_regressions_77`):
- ASI leading-continuation-operator/comma bug — `maybeInsertSemicolon` only
  checked the previous line's trailing token, not the next line's leading
  token; method-chaining with a leading operator or comma-first
  multi-declarator list got a bogus `;`. Fixed via `LEADING_CONTINUATION_OPS`
  (narrower than `CONTINUATION_OPS`) + leading-`,` check.
- No JS/TS regex-literal tokenizing — `/.../ ` fell through to the division
  scan, corrupting brace/paren tracking when a regex character class
  contained `"`. Fixed via `TokenizerCurly.emitRegexLiteral` +
  `isRegexLiteralAllowedHere`.
Final: zero crashes, round-trip idempotent, `node --check` 141/141 (was
93/141 pre-fix); semantic smoke test (`require('./index.js')`, live HTTP
round-trip, Mocha subset 35/35) all passed; one unrelated pre-existing
environment failure (unpinned deps) confirmed not formatter-induced.

**nestjs/nest (HEAD `7e6e313`):** five bugs found and fixed:
1. `/**` JSDoc opener corruption (universal curly-family bug in
   `MiscRuleCore.reformatMultiLineBlockComment`) — assumed a 2-char opening
   marker; fixed to scan forward while char is `*`. Also fixed for Java;
   Kotlin fixture updated.
2. Dot+space corruption in `MiscRuleCurly.renderCallCandidate`'s
   `sigForRender` (`options. provideInjectionTokensFrom`) — a multi-arg call
   whose every arg is a bare dotted member-access got misparsed as a C-style
   parameter list; fixed by forcing `sigForRender` to `null` for JS/TS too
   (fixture `real_code_regressions_81`).
3. Content duplication in `enforceClassFieldAlignmentGrid` on nested `class`
   braces — flat `classOpens` list with one linear cursor assumed disjoint
   spans; fixed by only alignment-gridding the outermost brace per nesting
   level (fixture `real_code_regressions_82`).
4. Comment-continuation-indent drift on an object-shaped intersection alias
   (`type X = {...} & Y;`) — `enforceUnionTypeContinuationIndent` reindented
   every NEWLINE with no bracket-depth tracking; fixed to only reindent at
   depth 0 (fixture `real_code_regressions_84`).
5. `join(...)` call-wrap/collapse non-idempotency at exactly
   `lineLengthLimit` — multi-line-source branch of `renderCallCandidate`
   preserved original per-line grouping with no fits-check of its own; fixed
   by adding the same fits-check, JS/TS-scoped only (widening regressed C++)
   (fixture `real_code_regressions_85`).
All confirmed against real files; `node --check`/`make test` green after
each (`make test` reached 134/134 by the end of this pass).

### `vuejs/core` dogfood pass — DONE

Repo: `vuejs/core` (`/tmp/vue-core`, HEAD `b5f8518`), 514 `.ts`/`.js` files
under `packages/`, `packages-private/`, `scripts/` (5 `.tsx` files under
`packages-private/dts-test/` correctly excluded, out of scope). Round1: zero
crashes, 514/514 formatted. Round1→round2 idempotency initially found 20
files differing.

**Bugs found and fixed (13 total across the pass):**
1. Leading multi-line block-comment reindent non-idempotency — class-field
   grid, enum formatting, and interface/type-alias member alignment all
   re-emitted a captured leading `/** ... */` comment at its *original*
   indentation instead of the member's re-rendered depth. Fixed via
   `reindentLeadingComment`, called at all three sites (fixture
   `real_code_regressions_87`). Resolved 15 of the 20 files.
2. `collectionHandlers.ts` — `GENERIC_SAFE_KEYWORDS` missing TS primitives
   `symbol`/`bigint`, and `isGenericSafeToken`'s OP case missing `|` (needed
   for `Record<string | symbol, Function | number>`) (fixture `_88`).
3. `componentOptions.ts` — three causes: same symbol/bigint/`|` bug; a
   type-parameter-default clause (`type MergedHook<T = () => void> = ...`)
   silently dropped by `parseTypeAlias`'s generic-clause skip loop; an
   unconditional `]`-followed-by-`]` branch (meant for C++11 attributes)
   misfiring on a TS mapped type (`{ [K in T[number]]?: unknown }`),
   desyncing bracket-depth tracking (fixture `_89`).
4. `ref.test-d.ts`/`watch.test-d.ts` — `classifyBraces`'s `isValue`
   prev-token list missing `|`/`&`, so an inline object type following a
   union/intersection op misclassified its `{` as a statement-body brace,
   corrupting indentation for the rest of the scope (fixture `_90`).
5. `if( ... )` nested-call paren-padding order-dependency (11 files) — per
   RDD_KEY_62 a nested `(`/`[` anywhere inside a paren pair makes it
   "loose"; round1 under-padded when the `if`'s consequent was itself a
   multi-line call. Root-caused and fixed.
6. `scripts/release.js` call-wrap/collapse boundary bug — the JS/TS
   single-argument fits-check measured candidate width *before*
   `JsTsDeclarationAlignmentRule`'s later column-alignment pass ran, so
   sibling `=`-padding could push a collapsed call back over the limit on
   round2. Fixed (this is the ancestor of the still-open angular/TypeScript
   "cluster 4"/"cluster 3" ordering bug — see those sections; the specific
   `release.js` instance itself is resolved here).
7–15. Nine further bugs surfaced by running `tsc --noEmit` against the full
   round1-formatted tree vs. unmodified (0 vs. new errors), each root-caused
   and fixed (fixtures `_101`, `_102`, `_105` [5 distinct sub-bugs], `_107`):
   `GENERIC_SAFE_KEYWORDS` missing `true`/`false`; a nested-brace-depth clear
   -all guard over-firing on a legitimate nested object-type argument;
   `GENERIC_SAFE_KEYWORDS` missing `keyof`/`is`/`infer`/`asserts`/`readonly`/
   `unique`/`as`/`satisfies`; a parenthesized-ternary `:` misclassified as a
   return-type colon (new `isGroupingExpressionParen` helper); `typeof`/
   `keyof` not recognized as `prevPrev` by the arrow-param-paren bail-out;
   a trailing type-annotation `:` wrapped to the next line got a bogus `;`
   (added `":"` to `CONTINUATION_OPS`); `isGenericSafeToken`'s OP list
   missing `=>`/`...` (the latter `lang.isTs`-gated to avoid regressing C++
   variadic-template spacing); a TS function-type parameter list
   (`(...args: any[]) => void`) wrongly padded like a grouping paren; the
   nested-brace-depth guard not covering tokens *inside* the nested braces
   (`Record<string, { local: string; default?: Expression }>`); and finally
   `GENERIC_SAFE_KEYWORDS` missing `typeof` (`Record<(typeof
   identityMethods)[number], any>` / `ReturnType<typeof createServer>`).

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
- **Single-declarator colon spacing**: `const x: number = 1;` renders as
  `const x : number = 1;` (space inserted before the colon) at plain top
  level with no function-type involved. Root cause:
  `JsTsDeclarationAlignmentRule`'s single-declarator grid-alignment handling
  — `classifyTypeColons` deliberately suppresses its own colon-spacing pass
  for a single-declarator statement, deferring to the alignment rule, whose
  spacing doesn't fully match `classifyTypeColons`'s for the ungridded case.
  Spacing-only, no tsc error; confirmed widespread via `grep -rn "const
  [a-zA-Z_]* : "`. **2026-07-28 re-assessment:** looked cheap at first
  glance, but the real fix requires reconciling two independent spacing
  decisions without disturbing the multi-declarator grid path that already
  depends on the alignment rule's spacing — not low-risk enough for a
  cleanup pass. Left as a re-assessment note, not attempted.

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
tolerances (see Dogfood Output Validation above). After the checker was
improved (follow-up session): **22/27 clean** (up from 10/27 pre-first-round
of tolerances). Remaining 5 files are two further confirmed-intentional,
non-lossy classes, left unfixed (checker gap, not formatter bug, low
priority TODO): bare single-param arrows gaining parens (3 files, documented
§6 behavior); STYLE.md §4 pre-increment-except-when-post-required correctly
rewriting a standalone/unused for-loop increment (2 files,
`perf/perf.js`/`test/test.js`).

**Verdict: DONE.** Zero new formatter bugs found. The one idempotency diff
is a confirming recurrence of the already-tracked `SwitchRule` issue.

### `angular/angular` dogfood pass — clusters 1-3 FIXED, cluster 4 PARTIALLY FIXED (3 of 4 root causes; #3 attempted and reverted), cluster 5 NOT FIXED (accepted gap, no action planned)

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
   `IDENTIFIER '.'` pairs before `prevIdx` to find the dotted chain's first
   segment, then apply the existing bail-out against what precedes THAT.
   Fixture `real_code_regressions_134`. `make test`: 183/183.
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
   the enclosing `<...>` tracking — same gap class as the existing
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

   - **Root cause #3 — ATTEMPTED AND REVERTED (too many regressions) —
     braceless-else body never re-validated after brace-collapse/alignment**
     (`format_date.ts:519`): `collapseSingleExpressionBlocks` strips
     `if`/`else` braces in Phase 0, before `enforceCallLineBreaking`
     (Phase 1) — the braced source used a `+`-chain complexity-wrap to fit,
     which doesn't apply to the now-braceless body, leaving the joined line
     over the limit; `alignBracelessElseIfChain` then pads it anyway
     (intentional escape hatch, `BlockStructureRule.java` ~line 2801-2819,
     not a bug there). Rounds diverge. **Tried:** refuse to collapse
     (`tryCollapse`) whenever the joined one-liner exceeds
     `lineLengthLimit`. **DO NOT retry this naive approach** — reverted: no
     way to know `enforceCallLineBreaking` will still wrap an inner call and
     make it fit, so it wrongly re-braced every braceless if/else with a
     wrappable-call body — broke 5 fixtures (`java_combined`,
     `real_code_regressions_57`/`_81`/`_93`/`_141`). **Real fix needed:**
     guard must simulate `enforceCallLineBreaking`'s wrap decision on the
     joined candidate first (two-pass lookahead) — bigger lift, deferred.
     2026-07-28 re-assessment: unchanged, not reattempted.
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
   ACCEPTED GAP, third confirming recurrence, NO ACTION PLANNED** — 3 files
   (`user_metric_spec.ts:88`, `emit.ts:104`, `i18n_parse.ts:520`): a lone
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

Next free fixture number unaffected by cluster 4/5 (still open, no new
fixtures). Full corpus re-run deferred until cluster 4 root cause #3 lands,
same pattern as `vuejs/core`/`lodash/lodash`.

## `microsoft/TypeScript` dogfood pass — 3 of 4 clusters FIXED; cluster #3 deliberately deferred (ACTIVE WORK, same root cause as angular cluster 4 above)

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

- A spurious-looking blank line after a class's opening `{` in older `.js`
  fixture drafts, and a doubled trailing space before a one-liner getter
  body's closing `}` — both confirmed correct, existing behavior (STYLE.md
  §7 named-construct blank line; `GetterSetterRuleCurly`'s group-width body
  padding), matching passing C++/Java/Kotlin fixtures byte-for-byte. Only
  the stale hand-authored `.js` draft fixtures were wrong; resolved by
  regenerating them.
</content>
