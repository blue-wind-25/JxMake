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
C-family brace/paren/statement shape). §1–15 rules are implemented in
`JsTsSpecificRule.java` (+ `JsTsDeclarationAlignmentRule.java` for the
declaration-alignment grid), wired into `FormatterCurly`'s phase pipeline.

## Status Summary

Baseline work (§1-15) is **DONE**. Real-code dogfood completed for
`expressjs/express`, `nestjs/nest`, `vuejs/core`, `lodash/lodash`,
`angular/angular`, `microsoft/TypeScript` — all findings fixed (see RDD
index and Checklist). JSX/TSX support: Step 1 (opaque boundary-finding
pre-pass, all 11 detection contexts incl. template-literal holes) and Step 2
(generic attribute-list wrapping of a `JSX_SPAN`'s opening tag, all 5
increments) are both **DONE** — see Scope/JSX section below. `make test`:
312/312 forward + idempotency as of the last landed change.

---

## Scope

`STYLE_JS_TS.md` covers latest ECMAScript (ES2024+) and TypeScript 5.x, one
shared file for both. JS/TS are curly-family (`Lang.isCurly()` covers
`isJs`/`isTs` alongside C/C++/Java/Kotlin, RDD_KEY_187): no separate
tokenizer/formatter classes — JS/TS share `TokenizerCurly`/`FormatterCurly`/
`ScopePipelineCurly`/`MiscRuleCurly`, gated internally on `lang.isJs`/
`lang.isTs`. Concrete rule logic lives in `JsTsSpecificRule.java` +
`JsTsDeclarationAlignmentRule.java`, TS-only additions gated on `lang.isTs`.

**JSX/TSX** (originally out of scope entirely; superseded by a two-step plan,
both steps now DONE — full design/increment history below under "JSX/TSX
implementation"):
- **Step 1 — DONE.** `TokenizerCurly.findJsxSpans` detects a JSX tree at any
  of 11 expression-start contexts (return, `=>`, ternary `?`/`:`, call-arg/
  array-element start, assignment/logical-RHS, grouping-paren start, bare
  `{`-hole start, template-literal `${}` holes, spread) and collapses it
  into one opaque, frozen `JSX_SPAN` token — raw source preserved
  byte-for-byte, treated as one atomic expression token by every
  surrounding pass. Content-preservation only, not JSX-aware reformatting.
- **Step 2 — DONE.** Generic long-expression-style wrap of a `JSX_SPAN`'s
  own *opening tag* attribute list when it exceeds `line-length` (self-
  closing and children-bearing tags, all attribute kinds). Never touches
  anything from the tag's closing `>`/`/>` onward (children/closing tag
  copied through byte-identical) — the JSX-whitespace-is-significant hazard
  makes reformatting children unsafe.
- Detection scope: `.jsx`/`.tsx` always on; `.js`/`.mjs`/`.cjs` detect JSX
  unconditionally (mirrors Babel/Prettier); plain `.ts` stays gated off by
  default (mirrors tsc/Prettier's own split, avoids the `<Type>expr` legacy-
  cast ambiguity) behind the `jsx-in-ts` config key (ordinary `Config` key,
  works via CLI/env/file/server/`JXM_CFMT_CFG` for free).
- A full JSX-aware embedding dispatcher (real grammar parsing, attribute-
  specific alignment, children-specific indentation semantics) remains a
  distinct, larger future job — not started, not designed beyond what's
  below.

**HTML5 `<style>`/other embedded-format dispatchers** beyond `<script>`
belong to the Data Formats job (`STATE_DATA_FORMATS.md`), not this one —
`<script>` splicing is done, see `XmlSpecificRule.renderScriptOrStyle`.

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
| RDD_KEY_195 | §15 local-import classification — only `./`/`../`-prefixed specifiers are "local", everything else non-built-in is "third-party" |
| RDD_KEY_196 | Closing comments on modifier-prefixed methods (`async`/`static`/`get`/`set`) use the bare name only; object-shaped `type X = {...}` aliases get closing comments like `interface`/`class`/`enum` |
| RDD_KEY_197 | Import-ordering: trailing same-line comment travels with its import; a standalone comment segments the import list (grouped/sorted independently per segment) instead of bailing the whole pass |
| RDD_KEY_245 | (No fix.) First dive into `commandLineParser.ts` decl-alignment idempotency bug; ruled out `spansMultipleLines` bracket-depth hypothesis; superseded by RDD_KEY_246 |
| RDD_KEY_246 | (No fix, 2 attempts reverted.) Root cause: `applyOversizedAggregateInitClosingBracePass` decides `}` placement from whether the initializer already has an embedded NEWLINE — stale round1 vs round2. Narrow re-run fixed symptom but exposed a sibling divergence; full `ScopePipelineCurly.process` re-run fixed repro but regressed other fixtures — both reverted. Superseded by RDD_KEY_248 |
| RDD_KEY_248 | Call-wrap/collapse vs. declaration-alignment idempotency bug, FIXED: `ScopePipelineCurly.reapplyClosingBraceAndDeclarationsPass` re-runs closing-brace + declarations passes a second time (JS/TS only), skipping the trailing-gap force-reindent step on the re-run. Fixture `real_code_regressions_179` |
| RDD_KEY_249 | (No fix, reverted.) `enforceCallLineBreaking` rejoin fits-check for multi-candidate lines; a blanket width-widening fix regressed other fixtures. Real mechanism found later: `alignBracelessElseIfChain` pads a short `else` after the fits-check already measured it unpadded — fixed by RDD_KEY_250 |
| RDD_KEY_250 | Braceless if/else rejoin-fits-check vs. `alignBracelessElseIfChain` ordering bug, FIXED: `FormatterCurly.format` re-runs `enforceCallLineBreaking` (twice) + `enforceComplexityPadding` right after `alignBracelessElseIfChain`. Fixture `real_code_regressions_180` |
| RDD_KEY_263 | switch-case fallthrough non-idempotency, FIXED: `FormatterCurly.format` re-runs `switchRule.formatNonInlineSwitches` a second time near the end of Phase 4, after case-grid collapse and call-wrap passes settle. Shared curly-family code, cross-ref `STATE_C_CPP_JAVA.md` |
| RDD_KEY_269 | angular cluster 4 residue (`shared.ts`/`directive_outputs.ts`), FIXED: widened `alignBracelessElseIfChain`'s chain-recovery to also tolerate a bare `else` indented deeper than its `if` (opposite of the pre-existing recovery). Fixture `real_code_regressions_184` |
| RDD_KEY_270 | `microsoft/TypeScript` `harness/collectionsImpl.ts`, FIXED: `applyAssignmentsPass` added as a 3rd pass inside `closingBraceAndDeclarationsOnly` narrow re-run mode (extends RDD_KEY_248). Fixture `real_code_regressions_185` |
| RDD_KEY_271 | angular cluster 4 residue #3 (`web_animations_player_spec.ts`/`input_transform.ts`), FIXED: (a) `tryParseClassField` collapses a multi-line field initializer's NEWLINE into a soft space instead of bailing; (b) `enforceUnionIntersectionSpacing`/`enforceTypeColonSpacing` pulled forward before `enforceDecoratorOverflowCascade`. Fixture `real_code_regressions_186` |
| RDD_KEY_273 | `microsoft/TypeScript` Tier-3 shape #1 (braceless if/else body-column padding stale on round2), FIXED: `alignBracelessElseIfChain` split on `"\n"` only, leaving a trailing `'\r'` on CRLF source that skewed length math across the `lineLengthLimit` boundary between rounds. Fixed by stripping trailing `'\r'` per split line. Fixture `real_code_regressions_195` (CRLF, `.gitattributes -text`) |
| RDD_KEY_274 | Tier-3 shape #2 (class-field `:`-alignment group splitting on round2), FIXED: a same-line leading comment forces its own line in `flushClassFieldGroup`, adding a NEWLINE the source never had; old `blankLineBetween` miscounted it as a real blank line. Fixed by requiring the two NEWLINEs be strictly back-to-back. Fixture `real_code_regressions_196` |
| RDD_KEY_275 | Tier-3 shape #4 (closing `}` non-idempotently gains a stale `// if` comment), FIXED as a verified side effect of RDD_KEY_273 (no separate code change; A/B bisection proved it). Fixture `real_code_regressions_197` (CRLF) |
| RDD_KEY_276 | Tier-3 shape #3 (interface intersection-type closing brace shifts column on round2), FIXED: a field literally named `class` made `classBraceKind`'s backward KEYWORD scan misclassify its own nested object-type-literal brace as a class body. Fixed via `isFieldNameKeywordUsage` (skip a `class`/`interface` token immediately followed by `:`/`?`). Fixture `real_code_regressions_198` |
| RDD_KEY_294 | `hasBreakableCall` under-approximation (XL.txt Tier 9, RDD_KEY_245/246/248/249/250 family), FIXED: `refuseUnrescuableCollapse` now also refuses a braceless-if/else collapse when the one rescuable call's own best-case arg-removal still can't clear `lineLengthLimit` (`maxRescueSavings`), gated off whenever an array/object literal is present in the scanned span (`containsListLiteral`) to preserve `real_code_regressions_81`/`_93`'s already-accepted "over limit is fine, rejoin anyway" behavior (2nd attempt; 1st, an ungated width gate, regressed `_81`). Fixture `real_code_regressions_209` |

---

## Test-Fixture Repos

- `nodejs/node`, `expressjs/express`, `lodash/lodash`, `microsoft/TypeScript`
  (also a JS fixture), `angular/angular`, `nestjs/nest`, `vuejs/core` — see
  `STATE_DOGFOOD.md` for run status.
- JSX/TSX dogfood repos (all DONE, see "JSX/TSX implementation" below):
  `taniarascia/react-tutorial`, `ruanyf/react-demos`, `reactstrap/reactstrap`
  (JSX); `microsoft/TypeScript-React-Starter`, `Lemoncode/
  react-typescript-samples`, `excalidraw/excalidraw` (TSX).

## Tools/compiler used

`node`/`tsc` env setup: see `STATE_COMMON.md`'s Verifier toolchain section.

**`typescript` package version gotcha:** an unpinned install may resolve to
`typescript@7.0.2` (native tsgo rewrite — no `createSourceFile`/
`getLeadingCommentRanges`). Must pin `typescript@5` (landed `5.9.3`). Verify
`typeof ts.createSourceFile === 'function'` after any reinstall.

---

## Dogfood Output Validation

**`js_ts_content_diff.js`** — content-preservation checker (TS compiler API,
same idiom as `java_content_diff.java`/`css_content_diff.py`). Compares
top-level imports as an order-tolerant multiset, every other top-level
statement via leaf-token canonicalization (alignment/reindent never
flagged), comments as a whitespace/case-normalized multiset. Comments
recovered via `ts.getLeadingCommentRanges`. `node
tools/verifiers/js_ts_content_diff.js <original> <formatted>` — exit 0/1/2.
JSX/TSX are out of scope for this checker (its own docstring says so;
`scriptKindFor` never maps `.jsx`/`.tsx` to a JSX-aware `ScriptKind` — a
documented checker limitation, not a formatter bug when it flags a JSX file
as MISMATCH). This gap was independently fixed for the *syntax*-check
sibling tool (`js_ts_syntax_check.js`, see "JSX/TSX implementation" below);
`js_ts_content_diff.js` itself was never updated for JSX/TSX `ScriptKind`.

**Tolerances** (added post-`lodash/lodash` dogfood, all false-positive
classes found there, all confirmed non-lossy):
1. `normalize-comment-end-period` — strips one trailing `.` before compare.
2. Single-statement block unwrapping (STYLE.md §10) — recurses into a
   single-statement `Block` so `if (x) foo();` == `if (x) { foo(); }`.
3. JSDoc-as-AST-child double-counting — skips `ts.isJSDoc` nodes during the
   statement walk (already covered by `collectComments`).
4. Bare single-param arrow parens (STYLE.md §6) — skips an arrow's `(`/`)`
   when its one parameter is a plain identifier with no type/default/rest.
5. For-loop incrementor `++`/`--` position (STYLE.md §4) — a `for(...)`
   incrementor's pre/post form is canonicalized to a fixed prefix token,
   deliberately scoped to that one slot only (elsewhere a pre/post swap is
   still a real semantic bug and must still be flagged).

---

## Test Fixtures (Local)

Local dogfood pairs live in `formatter/test/` — see `test/README.txt`.
Pairs are split by extension (`.js` vs `.ts`; TS-only constructs like
decorators/enums/generics/interfaces can't live in `.js`).
`js_combined`/`js_comments`/`ts_combined`/`ts_comments` plus all other local
JS/TS fixtures are active in the Makefile and passing.

---

## Open Questions / Known Limitations

- **Watch-list, not a job:** `=`-alignment-group non-self-stability quirk,
  seen once while building the RDD_KEY_142 fixture near `location_shim.ts`,
  never reproduced against the real file. If a future dogfood run ever
  produces this again, check it against this note before treating it as new.

- **Template-literal-hole JSX (item 10) real-corpus validation** was folded
  into Step 2 Increment 5's 6-corpus dogfood pass (`excalidraw` specifically
  hit and fixed a template-hole semicolon-insertion bug — see below) — no
  longer a separate open item.

- **JSX detection widening to `.js`/`.ts`** — IMPLEMENTED 2026-08-13 (see
  "JSX/TSX implementation" below for the research + landed design). `.ts`
  intentionally stays gated behind `jsx-in-ts` rather than default-on.

- **`findJsxSpanEnd`/`parseJsxTag` tag-name-identity hardening** —
  IMPLEMENTED as part of the same 2026-08-13 session: a closing tag now
  must name-match its opener (tracked via a `Deque<String>` of open names,
  not a bare integer depth); a mismatch safely returns -1 (same fallback
  contract as every other rejection), never throws/silently accepts.

- **JSX full embedding-aware dispatcher** (real grammar parsing of children/
  attribute values, not just opaque preservation + opening-tag-attribute
  wrap) remains explicitly out of scope — a distinct, larger future job. See
  "JSX/TSX implementation" below for the rejected 3-step alternative design
  and why (unbounded recursion into `{}` holes, HTML5-tree-construction
  pass not verified for JSX's HTML-divergent grammar, no grammar-position-
  aware parser in this codebase to inherit `<` disambiguation from for free).

- **Unrelated bug found, not fixed (out of this job's scope):**
  `ruanyf/react-demos`'s `demo13/app.js` (compiled, non-JSX, minified
  one-liner function bodies) is not idempotent — a general curly-brace-
  family reindent pass re-breaks an already-reformatted one-liner
  differently on a second pass. Belongs to `STATE_C_CPP_JAVA.md`'s
  curly-family work or a new dedicated investigation.

---

## JSX/TSX implementation

**Rejected alternative design (2026-08-07 discussion, no code/RDD key).** A
user-proposed 3-step approach (tokenizer marks a whole JSX tree as one plain
`IDENTIFIER`; `{...}` holes become `__JSn__` placeholders handed to the HTML
formatter; each placeholder's content re-sent through the JS/TS formatter)
was assessed and **not adopted**: Step 1 discards structure needed by
width/alignment passes (same fragility class as RDD_KEY_248/249/250) without
a dedicated opaque/frozen token; Step 2's HTML-formatter reuse is sound for
splice mechanics but JSX diverges from real HTML5 (self-closing tags,
case-sensitive component names, fragments, `onClick={handler}`-style
attribute-valued embeds) in ways the HTML5 tree-construction pass isn't
verified to tolerate; Step 3 is unbounded-depth recursion
(`items.map(x => <li>{x}</li>)`), not the flat step described. Real JSX
parsers resolve the `<` ambiguity via grammar-position lexer modes, which
this codebase's flat-tokenizer architecture has no equivalent of — the
portable idea extracted instead: a dedicated pre-pass testing `<` at a short
enumerable list of expression-start token-adjacency contexts, recursing into
`{}` holes, without needing a real AST. `@babel/parser` was considered and
rejected for a JSX-aware `js_ts_content_diff.js` (avoid a second parser
dependency/npm-pin gotcha) — stick with `ts.createSourceFile`'s
`TSX`/`JSX` `ScriptKind`s if that checker is ever extended.

**Design session (2026-08-12, no code, no RDD key).** Fleshed out the
portable idea into a concrete design, later implemented essentially as
written:
- **Enumerable expression-start context list** (11 items): after `return`;
  after `=>`; after ternary `?`/`:` (both branches); call-argument start
  (`(`/top-level `,`); array-element start (`[`/top-level `,`); assignment-
  RHS (`=` and compound assignment ops); logical/nullish RHS (`&&`/`||`/
  `??`); grouping-paren start (a `(` that is NOT a call-open); recursively
  inside a JSX `{...}` hole; recursively inside a template-literal `${}`
  hole; after `...` (spread). Explicitly NOT a context: after IDENTIFIER/`)`/
  `]`/NUMBER/STRING — those are exactly the shapes `reclassifyAngleBrackets`
  already claims as generic-safe, never JSX-open.
- **Token representation**: new `TokenType.JSX_SPAN`, raw `text` (incl.
  embedded newlines) from opening `<` through the matching close/self-close,
  `frozen = true` unconditionally, no new position/width field (follows the
  existing convention for other multi-line-text tokens like `COMMENT_BLOCK`
  — line-length passes just need a `\n`-containing-token guard, same as they
  already need for those). No depth field either — nesting is internal to
  the pre-pass's own recursive walk, never externally visible once a span
  token is emitted (exactly one `JSX_SPAN` per top-level JSX tree).
- **Ordering**: the JSX pre-pass must run BEFORE `reclassifyAngleBrackets`
  (generics disambiguation), so generics-disambiguation never sees an
  already-consumed JSX span's interior `<`/`>`.
- **The genuine `<Type>expr`-cast-vs-JSX ambiguity**: both start identically
  (`=`, `<`, IDENTIFIER, `>`) and land in the same expression-start context
  — resolved the same way real tooling resolves it, by file-extension-scoped
  dispatch (`.tsx`/`.jsx` files always treat expression-start `<` as a
  JSX-open candidate first, falling back to plain relational-`<` if no
  matching close is found; plain `.ts`/`.js` files' pre-pass doesn't run at
  all under this design — later widened, see below).
- One flagged gap not resolved by this session: the context list was
  assembled from static reasoning, not yet validated against a real JSX
  corpus — flagged as required validation before trusting it complete (later
  done via the Step-2-Increment-5 6-corpus dogfood pass, see below).

**Step 1 implementation (Increments 1-6, 2026-08-12 → 2026-08-13, all
LANDED).** Each increment added one or two contexts to
`TokenizerCurly.findJsxSpans`'s `isJsxContext`, each independently fixture-
verified and `make test`-clean with zero regressions on non-JSX fixtures
(confirming the `.jsx`/`.tsx`-only gate is a true no-op elsewhere), each
relying on the same self-correcting safety net: `findJsxSpanEnd`/
`parseJsxTag` return -1 on anything that doesn't parse as a balanced JSX
tree, leaving tokens untouched.
- **Increment 1** (`return` context): added `Lang.isJsxSyntax` (new
  `Lang(language, filePath)` overload, true only for `.jsx`/`.tsx`) and
  `FormatterCore.forLanguage`'s matching overload. **Real bug found and
  fixed as a prerequisite**: `isRegexLiteralAllowedHere`'s character-level
  lexer misread the `/` of a JSX closing tag (`</Foo>`, preceded by `<`) as
  regex-literal start, corrupting tokenization before the post-tokenize JSX
  pass could even run. Fixed with a narrow `lang.isJsxSyntax`-gated special
  case (`OP "<"` immediately before `/` disallows regex-start). Fixture
  `jsx_tsx_return_context`.
- **Increment 2** (`=>`, ternary `?`/`:`): plain single-token-lookback
  checks; `?.`/`??`/`?:` multi-char ops already consumed earlier in the
  lexer so a bare `?`/`:` OP token can't misfire. Fixtures
  `jsx_tsx_arrow_ternary_context`, `jsx_tsx_combined_sanity`.
- **Increment 3** (call-argument/array-element start): new
  `isCallArgumentOrArrayElementStart` (+`isCallOpenParen`,
  `findEnclosingOpenBracket`) distinguishes a call-open `(` from a grouping
  `(`, and walks a `,`'s enclosing bracket via a small stack-based matcher.
  Fixture `jsx_tsx_call_array_context`.
- **Increment 4** (assignment-RHS incl. compound, logical/nullish-RHS): new
  local `JSX_ASSIGNMENT_OPS`/`JSX_LOGICAL_OPS` sets (kept tokenizer-local
  rather than reusing `MiscRuleCore.ASSIGNMENT_OPS`, which is
  cross-package-`protected` and missing `&&=`/`||=`/`??=`/`<<=`/`>>>=`).
  Attribute-`=`-vs-context-`=` ambiguity checked and confirmed a non-issue
  (bare-JSX attribute values aren't valid JSX syntax). Fixtures
  `jsx_tsx_assign_logical_context`, `jsx_tsx_assign_logical_sanity`.
- **Increment 5** (grouping-paren start): new `isGroupingParenStart` (mirror
  of `isCallOpenParen`). Wider blast radius than prior increments (fires on
  every control-flow `(` too, e.g. `if (...)`) but the same -1 fallback
  covers it; flagged as worth watching at real-corpus scale (no issue
  actually found). Fixture `jsx_tsx_grouping_paren_context`.
- **Increment 6** (bare `{`-hole start; spread): item 9 re-scoped from its
  literal "recurse into every hole" wording — traced that `findJsxSpans`'s
  outer loop already scans every significant token regardless of nesting
  depth, so the only genuine gap was a `<` immediately after a bare `{`
  (`{<Bar/>}`); added as a plain `isPunct(prev, "{")` check, no recursive
  machinery needed. New `isSpreadContext` for item 11 (`...` then the same
  shape `isCallArgumentOrArrayElementStart` already tests). **Item 10
  (template-literal `${}` holes) found structurally unreachable at this
  pass's level, not merely unimplemented** — a whole template literal is
  already swallowed into one opaque STRING token by the character-level
  lexer before `findJsxSpans` (a post-tokenize pass) ever runs; needs a
  tokenizer-level change, tracked separately (see below). Fixture
  `jsx_tsx_hole_spread_context`.

**Item 10 scoping session (2026-08-13, no code, no RDD key).** Broke the
tokenizer-level change into sub-contexts, since — unlike Increments 1-6 —
`emitTemplateLiteral`/`skipTemplateInterpolation` run for every JS/TS file
regardless of extension (template literals aren't JSX-only syntax), so this
can't be scoped as narrowly by construction.
- **Sub-context 0**: decide the opaque-vs-transparent boundary —
  `skipTemplateInterpolation` must tokenize (not just skip) each `${...}`
  interior; `emitTemplateLiteral`'s one-`Token` return becomes a sequence of
  tokens spliced into the stream.
- **Sub-context 1**: new `TokenType`s for the hole boundary — two options
  considered, (a) reuse plain `PUNCT` tokens for `${`/`}` (cheaper) vs. (b) a
  dedicated `TEMPLATE_HOLE_OPEN`/`CLOSE` pair (safer, more invasive).
  Recommended starting with (a).
- **Sub-context 2**: nested template literals inside a hole — traced that
  the existing recursive skip-structure already handles arbitrary nesting;
  the re-entry point must route back through `emitTemplateLiteral` itself
  (not a simplified path) to preserve that.
- **Sub-context 3**: scope decision — strongly recommended gating the new
  behavior on `lang.isJsxSyntax` (narrow, mirrors Increment 1's regex-lexer
  carve-out) rather than changing template-literal tokenization
  unconditionally for every JS/TS file (categorically larger regression
  surface — template literals are ubiquitous, unlike any JSX-only context).
- **Sub-context 4**: regression-test plan sized to the risk — every existing
  fixture with a backtick needs byte-identical re-verification, plus new
  fixtures for a bare JSX-in-hole case, a non-JSX-interpolation case (the
  single most likely regression class), the nested-template case, and an
  `if (x < 1)` safety-net case.
- Suggested 5-step increment breakdown recorded for implementers (structural
  refactor first with no new tokenization; then real re-entry tokenization,
  verified against non-JSX holes first; then JSX-detection wiring; then
  nested-template fixture; then real-corpus validation) — followed as
  written when implemented (below).

**Item 10 (sub-contexts 0-3) implementation — LANDED 2026-08-13.**
Implemented as one combined change (narrow `.jsx`/`.tsx`-only gate applied
from the first line, not bolted on after).
- **Sub-context 0**: `TokenizerCurly.tokenize`'s dispatch chain extracted
  into `tokenizeOneUnit(List<Token>)` so hole interiors can re-enter real
  tokenization. New `emitTemplateLiteral(List<Token>)` (`void`-returning,
  pushes tokens directly — matches how every other multi-token emission in
  this tokenizer already works). Old single-opaque-token body preserved as
  `emitTemplateLiteralOpaque()`, still the only path for plain `.js`/`.ts`.
- **Sub-context 1**: option (a) (`PUNCT` `"${"`/`"}"`) was tried first per
  the scoping session's preference but **broke real code within minutes**:
  several existing passes check `isPunct(t, "}")` for statement/ASI-boundary
  purposes without verifying a matching real `{` precedes it, and a hole's
  closing `"}"` is textually indistinguishable from a real block/object
  close — corrupted `${x+1}` (spurious `;`, mangled newlines). Fell back to
  option (b): dedicated `TokenType.TEMPLATE_HOLE_OPEN`/`TEMPLATE_HOLE_CLOSE`
  (new methods `emitTemplateLiteralSegmented`/`emitTemplateHoleInterior`),
  the hole's own closing `}` consumed directly (never through
  `emitCloseBrace`, so global `braceDepth` is untouched by it) while a real
  nested `{`/`}` inside the hole still participates in `braceDepth`
  normally. `findJsxSpans.isJsxContext` gained a disjunct: JSX may start
  right after `TEMPLATE_HOLE_OPEN`. **Discovered along the way**: the
  pre-existing `enforceTemplateLiteralInterpolationSpacing` is text-based
  and only matches one opaque backtick-STRING token — silently stopped
  firing for `.tsx` once templates became segmented there. Fixed with a
  token-based parallel path (`findMatchingTemplateHoleClose`/
  `renderTemplateHoleInterior` in `JsTsSpecificRule`). Fixture
  `jsx_tsx_template_hole_context`.
- **Sub-context 2**: nesting held for *tokenization* for free as predicted,
  but **not** for the new *rendering* path: `renderTemplateHoleInterior`'s
  first version folded a nested hole correctly but still pushed the nested
  literal's own STRING segments into the significant-token list separately,
  and `renderTokens` spaced adjacent STRING tokens apart as unrelated
  values — a real, cumulative, non-idempotent corruption
  (`` `a ${`b ${x+1}`} d` `` grew a space wider every pass), invisible to
  `make test`'s aggregate count since no prior fixture exercised it. Fixed
  by folding a nested literal's entire segment chain into one synthetic
  STRING token before it reaches the significant-token list. Fixture
  `jsx_tsx_template_hole_nested`.
- **Sub-context 3**: confirmed true no-op outside `.jsx`/`.tsx` by
  individually sweeping every existing fixture containing a backtick.
- `make test`: 299/299 → 301/301 forward + idempotency (2 new fixture
  pairs), pre-existing 299 unaffected.

**Step 2 scoping session (2026-08-13, no code, no RDD key).** Framed
explicitly as NOT a 12th detection context — a *rendering* concern applying
uniformly to every already-detected `JSX_SPAN`, regardless of which
detection context caught it.
- **Sub-context 1 (minimal structure)**: rather than tokenizing a JSX tree's
  children/attribute values into real sub-tokens, extend `parseJsxTag` to
  additionally record the opening tag's attribute-boundary offsets it
  already walks past (currently discarded) as byte offsets into the span's
  `text`. Everything from the tag's `>`/`/>` onward stays exactly as opaque
  as before.
- **Sub-context 2 (the JSX-whitespace-is-significant hazard — the single
  biggest risk specific to Step 2)**: JSX children whitespace/newlines can
  be semantically meaningful under JSX's own whitespace-collapsing rules,
  unlike ordinary JS/TS cosmetic whitespace — reformatting across that
  boundary risks an actual behavior change, not just a cosmetic diff.
  Recommendation (followed): Step 2 may ONLY ever touch the opening tag's
  attribute list, never reflow/re-emit a byte from the closing `>`/`/>`
  onward — falls out of sub-context 1's own scoping.
- **Sub-context 3 (wrap-decision machinery)**: `enforceCallLineBreaking`/
  `renderCallCandidate` assume comma-separated, typed-declaration-shaped
  arguments — wrong fit for JSX's whitespace-separated, comma-less
  attributes (bare boolean, spread `{...props}`, expression-valued).
  Recommendation (followed): a dedicated but still generic wrap function
  reusing only the *shape* of the fits-check/wrap-ladder and its
  width-measuring helpers, not `renderCallCandidate`/`parseSignature`
  itself.
- **Sub-context 4**: a `JSX_SPAN` inside a template-literal hole needs no
  special-casing — it's just one more token to `renderTemplateHoleInterior`,
  same as every other Step-1 context.
- **Sub-context 5**: no separate scope gate needed — `JSX_SPAN` only ever
  exists under `lang.isJsxSyntax`, so Step 2 inherits Step 1's scope by
  construction.
- **Sub-context 6**: regression-test plan — long-attribute-list-wraps,
  short-list-stays-one-line, spread, bare-boolean, expression-valued (nested
  `()`/`.`), and an explicit byte-identical-children assertion on every
  fixture with children.
- Suggested 5-increment breakdown (structure-only first; self-closing-tag
  wrap only; extend to children-bearing tags; attribute-kind fixtures;
  real-corpus validation) — followed as written (below).

**Step 2 implementation (Increments 1-5, 2026-08-13/14, all LANDED — Step 2
complete).**
- **Increment 1 (detect-and-measure-only)**: `Token` gained
  `jsxOpeningTagEndOffset`/`jsxAttrBoundaries` (populated only for
  `JSX_SPAN`); `parseJsxTag` records each top-level attribute's start
  position; new `JsxWrapDiagnostics` (two `AtomicInteger` counters,
  test/debug scaffolding, never mutates output) wired into
  `FormatterCurly.formatOne`, gated on `lang.isJsxSyntax`. Documented
  approximation: measures only the opening tag's own raw width, not its
  rendered column position (indentation/preceding tokens) — real
  column-aware measurement deferred to the actual wrap increment. Verified
  via a scratch `Verify.java` harness (no JUnit in this repo) — exact
  expected measured/over-width counts on a 2-tag fixture. Fixture
  `jsx_tsx_wrap_detect_context`. No behavior change — output byte-identical.
- **Increment 2 (real wrap, self-closing tags only)**: new
  `JsTsSpecificRule.enforceJsxSelfClosingAttributeWrap` (+
  `renderJsxSelfClosingWrapCandidate`), mirroring
  `enforceCallLineBreaking`'s fits/wrap decision shape without its
  comma-split/typed-signature machinery. A span is "self-closing, no
  children" purely via `jsxOpeningTagEndOffset == text.length()`. Wired
  into `FormatterCurly.format` Phase 4, after `formatNonInlineSwitches`.
  **Real bug found and fixed as a prerequisite**: `attrRawTokenIndices`
  recorded a `{` at `localBrace == 0` as a fresh attribute boundary
  unconditionally — correct for spread (`{...props}`) but wrong for an
  ordinary `name={value}` attribute's own value-hole open brace (same
  attribute, not a second one), splitting `attr={x}` into two wrapped
  lines. Fixed with `isValueHoleOpenBrace` (a `{` immediately preceded by
  `=` at `localBrace == 0` is skipped, not recorded). Fixture
  `jsx_tsx_self_closing_wrap`.
- **Increment 3 (extend to children-bearing tags)**: generalized the render
  method to accept any root span with `jsxOpeningTagEndOffset >= 0` and
  non-empty `jsxAttrBoundaries` (tokenizer already populated both fields for
  open tags too — only the render method's gate was narrow). **Width-
  measurement fix found while designing this increment** (not a
  regression): Increment 2's check measured the *entire* span's raw text
  length, harmless for self-closing (no child text) but wrong for
  children-bearing tags (a short opening tag with huge children would
  spuriously "overflow"); fixed to measure `jsxOpeningTagEndOffset` alone,
  matching `JsxWrapDiagnostics`'s own established approximation. Splice
  mechanism: `text` split at `jsxOpeningTagEndOffset` into a rewritten
  opening-tag segment and an untouched `tail` (children + closing tag) —
  `tail` is appended completely unmodified, no re-tokenizing/trimming —
  this IS the byte-identical-children guarantee, enforced structurally.
  Fixture `jsx_tsx_self_closing_wrap` extended (3 new cases incl. a
  multi-line-children case verified byte-identical via `--diff`).
- **Increment 4 (attribute-kind fixtures)**: no source changes — confirmed
  the existing brace-only balance tracking in `parseJsxTag` already
  generically handles spread, bare boolean, and expression-valued
  (incl. nested `()`/`.`) attributes with no JSX-grammar-specific logic
  needed. Fixture `jsx_tsx_attr_kinds_wrap` (4 cases).
- **Increment 5 (real-corpus validation, all 6 corpora, Step 2 complete)**:
  - `taniarascia/react-tutorial` (JSX, re-run), `microsoft/
    TypeScript-React-Starter` (TSX, re-run): idempotent, syntax-clean, no
    line naturally triggers the wrap in either corpus (still validates no
    regression on non-triggering real code).
  - `ruanyf/react-demos` (JSX): real JSX here lives almost entirely inside
    `.html` `<script type="text/babel">` blocks (out of `JS_SCRIPT_TYPES`'s
    allowlist — a separate HTML5 script-dispatch concern, not a gap here).
    Of the few standalone `.js` files, 2 have real JSX, both idempotent/
    clean. Found the unrelated `demo13/app.js` non-idempotency bug noted
    under Open Questions above (filed, not fixed — out of scope).
  - `Lemoncode/react-typescript-samples` (TSX, 329 files, sampled 15): all
    idempotent/clean; 2 files' pre-existing multi-line attribute formatting
    confirmed author's own style, not wrap-logic output (no line naturally
    overflows in the sample).
  - `reactstrap/reactstrap` (JSX, full 108-file set): **found a real,
    reproducible content-corruption bug** (a Step 1 detection gap, not
    wrap-logic): `parseJsxTag` required a tag-name `IDENTIFIER`
    unconditionally, so JSX fragment shorthand (`<>...</>`) was never
    recognized as JSX at all — its `{...}` content fell through to ordinary
    JS statement formatting, which wrongly inserted a `;` *inside* the
    expression hole (`DropdownToggle.js`: `{returnFunction(...)}` →
    `{returnFunction(...);}`). **Fixed**: when the token after `<`/`</` is
    `>` (not an IDENTIFIER), treat it as a fragment with an empty-string
    tag-name sentinel — the existing name-match check in `findJsxSpanEnd`
    then pairs `<>`...`</>` correctly with no other changes (fragments never
    have attributes, so the wrap logic never engages on one). Fixture
    `jsx_tsx_fragment_shorthand`. Full 108-file re-sweep post-fix: clean.
  - `excalidraw/excalidraw` (TSX, sampled 17 incl. all 7 real wrap-trigger
    candidates): idempotent, zero formatter errors. **Found a second real,
    reproducible content-corruption bug**, unrelated to JSX detection or
    wrapping: `enforceSemicolonInsertion`'s depth counter tracked `(`/`[`/
    expression-`{` but never `TEMPLATE_HOLE_OPEN`/`CLOSE` — a `NEWLINE`
    immediately after `${` inside a multi-line hole (e.g. a wrapped
    ternary) was evaluated at depth 0 as a real statement boundary, so a
    stray `;` got appended directly onto the `${` token
    (`SearchMenu.tsx`: `` `${searchMatches.items.length} ${\n cond\n ? a\n : b\n}` ``
    → `` `${...} ${;\n...` ``, parse-breaking). **Fixed**: `TEMPLATE_HOLE_OPEN`/
    `CLOSE` now push/pop the depth counter exactly like `(`/`)`;
    `needsSemicolonAfter` also explicitly returns `false` for
    `TEMPLATE_HOLE_OPEN` as a defensive belt-and-suspenders. Fixture
    `jsx_tsx_template_hole_wrap`. Full 17-file re-sample post-fix: clean.
  - **Step 2 verdict**: all 5 increments landed; validated idempotent and
    syntax-clean across 6 real-world corpora (2 re-runs + 4 new), with 2
    genuine content-corruption bugs found and fixed (fragment-shorthand
    detection gap; template-hole semicolon-insertion gap) — both unrelated
    to the wrap logic itself, exactly the class of defect real-corpus
    validation exists to catch.
  - `make test`: 306/306 → 307 → 308 → 309 → 310 → 311 across the 5
    increments (forward + idempotency each step), zero regressions.

**Checker-tooling follow-ons (2026-08-14, LANDED, not formatter source).**
- `js_ts_syntax_check.js`'s `scriptKindFor` only special-cased `.ts` and
  fell back to plain `ts.ScriptKind.JS` for `.tsx`/`.jsx` too — neither JS
  nor TS `ScriptKind` does JSX/generic disambiguation, so every real
  TypeScript generic in a `.tsx` file was a false parse-error (the "6
  pre-existing checker-limitation failures" seen across the corpora above).
  Fixed: `.tsx` → `ts.ScriptKind.TSX`, `.jsx` → `ts.ScriptKind.JSX`
  explicitly. Verified against all 6 previously-failing excalidraw files
  (now 0 failures) without masking `reactstrap`'s genuine unrelated
  `export X from 'Y'` failure (still correctly caught).
- `reactstrap/reactstrap`'s `src/index.js` used the never-standardized
  Babel-only `export Foo from './Bar';` re-export shorthand (~100 times),
  which `ts.createSourceFile` has no grammar for under any `ScriptKind`.
  Fixed by having the checker line-rewrite that exact shape to the
  standard-equivalent `export { default as Foo } from './Bar';` before
  parsing (checker-only, line-preserving, never touches formatter output).
  Verified: `index.js` now exits 0, a real syntax error is still caught,
  full 197-file reactstrap `src/` sweep now 0 failures. This closed the
  last remaining accepted checker-tool gap across all 6 Step 2 corpora.
- Neither change touched formatter source; `make test` (312/312) unaffected
  both times.

**Research session (2026-08-13, no code/RDD key) — how real tooling handles
JSX-in-`.js`/`.ts` detection, informing the widening below.** Confirmed via
direct research: Babel/`@babel/parser` requires an explicit `jsx` plugin
opt-in at the parser-options level (no extension-sniffing in the bare
parser); Prettier's default parser for the whole `.js`/`.mjs`/`.cjs`/`.jsx`
family always enables JSX, but for `.ts` uses a separate TS-scanner-based
parser with JSX off, only enabling it for `.tsx` — TS and Prettier both
treat `.ts`/`.tsx` as fundamentally different because of the real
`<Type>value`-cast-vs-JSX-open-tag ambiguity (a well-known, deliberate TS
restriction — `<Type>` casts are disallowed in `.tsx`, `as Type` required
instead). ESLint/`@babel/eslint-parser` require explicit project-config
opt-in, not per-file sniffing. No mainstream tool content-sniffs (e.g. for
`import React`) to decide JSX-ness — confirmed unreliable independently: the
"automatic" JSX runtime (React 17+) doesn't require importing React at all,
so an import-based heuristic would miss exactly the modern case while
over-triggering on hooks-only non-JSX files that happen to import React.

Re-reading this codebase's own `isJsxContext`'s 11 `||`-clauses confirmed
every one requires `<` to be the first token of a brand-new expression — an
ordinary comparison's `<` is never in that position (the left operand
always precedes it), so comparisons are structurally excluded by
construction, not merely caught by the -1 fallback. The one real residual
ambiguity is the `<Type>expr` legacy cast, which is rare-to-nonexistent in
true `.js` (that syntax isn't even valid JS) but real in `.ts`. A second,
genuine, previously-unguarded gap was found in the same re-read: tag
matching only tracked nesting *depth*, not tag *name* — `<a>...</b>` would
balance as if valid — flagged as worth hardening regardless, and load-
bearing once detection widens past `.jsx`/`.tsx` (closes off the residual
cast-collision risk for `.js`/`.ts`).

**Recommendation (implemented as written): hybrid.** Extend detection
unconditionally to `.js`/`.mjs`/`.cjs` (mirrors Babel/Prettier, justified by
the structural-exclusion finding above). Do NOT extend to plain `.ts` by
default (mirrors tsc/Prettier's own deliberate split) — instead expose a
`.ts`-scoped opt-in. Land the tag-name-identity hardening either way.

**Implementation — LANDED 2026-08-13.**
1. `Lang.isJsxSyntaxPath` widened unconditionally to `.js`/`.mjs`/`.cjs` (in
   addition to pre-existing `.jsx`/`.tsx`, unchanged).
2. `.ts` stays gated off by default.
3. **Design decision**: a plain new boolean `Config` key rather than a
   bespoke CLI-only flag — checked that `InFileConfig.isEligible` admits any
   ordinary known `Config` key automatically (CLI/env/file/server/
   `JXM_CFMT_CFG` all work for free), unlike `--lang` (RDD_KEY_286) which
   needed hand-written special-casing for a real reason (not an ordinary
   `Config` key) that doesn't apply here. Landed as `Config.ALL_KEYS`/
   `GROUPS` (`"JS/TS"`)/`describeOne`/`fromRawMap` entries, a `jsxInTs`
   field, threaded through `FormatterCore`'s new 3-arg `forLanguage`
   overload and `GdrPipelineGate.applyAndFormat` (the single real call
   site). **Renamed `jsx-in-js` → `jsx-in-ts`** same day, on user review of
   README.md (the original name misleadingly read as ".js files" though it
   only ever affects `.ts`) — pure rename across `Config`/`Lang`/
   `FormatterCore`/`GdrPipelineGate`/the fixture/docs, no behavior change.
4. `parseJsxTag` returns a small `JsxTagResult` (incl. `tagName`) instead of
   a bare `int[]`; `findJsxSpanEnd` tracks a `Deque<String>` of open tag
   names instead of an integer depth — a closing tag only reduces depth on
   a name match; any mismatch safely returns -1.

**Two latent bugs found and fixed along the way** (invisible before this
session because widening JSX detection to `.js` is what first activated the
`.jsx`/`.tsx`-only segmented-template-literal tokenizer mode for plain `.js`
files containing real template interpolations):
- `DeclarationAlignmentRuleCore.needsSpaceBetween` (a duplicate of
  `MiscRuleCore`'s adjacency-spacing method) had no `TEMPLATE_HOLE_OPEN`/
  `CLOSE` awareness, inserting spurious spaces around a template literal's
  `${`/`}` boundaries in declaration-grouped renders. Found via
  `DEBUG_PHASES`-gated phase tracing, isolated to `ScopePipelineCurly
  .process`. Fixed with the same always-tight guard `MiscRuleCore` uses.
- `ComplexityPaddingEvaluator.isLoose` counted a nested call inside a
  template hole's interior toward the *enclosing* call's own looseness
  decision, wrongly padding `console.log(...)` when a hole contained a
  nested call. Found via the pre-existing `real_code_regressions_94`
  fixture. Fixed by having `isLoose` skip over `TEMPLATE_HOLE_OPEN`...
  `TEMPLATE_HOLE_CLOSE` spans (nesting-depth-aware) rather than recursing
  into them.

Fixtures added: `jsx_in_plain_js` (real JSX in plain `.js`, preserved),
`ts_jsx_default_off` (`.ts`'s legacy `<Type>` cast untouched by default),
`ts_jsx_optin` (`.ts` + `JXM_CFMT_CFG jsx-in-ts=on` — JSX now detected),
`jsx_mismatched_tag`/`js_mismatched_tag` (`<a>text</b>` bails to plain
formatting in both `.jsx` and the newly-widened `.js`). `make test`:
306/306 forward + idempotency.

**`taniarascia/react-tutorial` re-dogfooded against the real `.js` corpus
that motivated this whole effort**: all 5 files now round-trip clean,
`js_ts_syntax_check.sh` 5/5 (previously 4/5 failed, including the
`Api.js` truncation/`{entry}`→`{entry;}` corruption that originally
motivated this widening). Idempotent. Content-diff still flags 4/5 as
MISMATCH — confirmed by manual whitespace-stripped diff as entirely
legitimate style transforms (arrow-parens, semicolons, closing comments),
no lost/garbled JSX, a documented `js_ts_content_diff.js` JSX-non-awareness
limitation, not a formatter bug. `STATE_DOGFOOD.md` row updated.

---

## Related investigation history — pass-ordering / stale-width bug family

RDD_KEY_245/246/249 (rejected/no-fix, superseded) all probed the same
architectural question as the three "Active work" bugs below: "which pass
gets to see the final, stable per-line/per-column width." Compact locus
index, not re-derived from scratch by future investigations:

- **RDD_KEY_245**: first pass at `commandLineParser.ts`'s idempotency bug;
  ruled out `spansMultipleLines`'s bracket-depth bail; wrongly suspected
  `enforceCallLineBreaking`/`renderCallCandidate`'s bracket placement (later
  proven structurally impossible by RDD_KEY_246). Real loci found since:
  `applyOversizedAggregateInitClosingBracePass` (RDD_KEY_246/248) and
  `applyAssignmentsPass` (RDD_KEY_270 below).
- **RDD_KEY_246**: pinned the root cause (`}`-placement decided from stale
  embedded-NEWLINE state) but both fix attempts regressed other fixtures —
  reverted. Lesson carried into RDD_KEY_248: an unconditional full
  `processScope` re-run is unsafe; the eventual fix re-ran only two of the
  five passes. (General pitfall noted: redirect long `make test` runs to a
  log and `grep -n "^FAIL"` — live terminal capture can silently truncate.)
- **RDD_KEY_248 (FIXED)**: the untried narrow middle ground — re-run just
  `applyOversizedAggregateInitClosingBracePass` + `applyDeclarationsPass` a
  second time, skipping the shared trailing-gap force-reindent step on the
  re-run (that step re-derives indentation from already-reformatted text,
  which is what broke the earlier full re-run attempt).
- **RDD_KEY_249** (reverted, later redirected): a blanket statement-wide
  fits-check fix regressed other fixtures (same trap that later sank the
  first `hasBreakableCall` width-gate attempt for angular cluster 4 root
  cause #3, below). The real mechanism was `alignBracelessElseIfChain`
  padding a short `else` *after* the fits-check already measured it
  unpadded — fixed by RDD_KEY_250.
- **RDD_KEY_250 (FIXED)**: re-run `enforceCallLineBreaking` twice +
  `enforceComplexityPadding` right after `alignBracelessElseIfChain` (same
  fix shape as RDD_KEY_248; a single re-run call wasn't enough to reach a
  fixed point).

---

## Active work — all 3 originally-tracked bugs FIXED (RDD_KEY_269/270/271)

No open bug remains from this investigation. Full session narrative in
`git log`; compact per-bug record kept here (symptom/root cause/fix/
verification) since these are the kind of pass-ordering bug a future
session must not accidentally reintroduce or rediscover from scratch.

### 1. angular `shared.ts`/`directive_outputs.ts` (FIXED, RDD_KEY_269)

A bare `if(cond) stmt; else stmt;` round-trips correctly aligned on round1
but re-indented and unaligned on round2. `alignBracelessElseIfChain`'s
chain-recovery only handled "the `if` line was left-padded wider than
`else`"; round2 sometimes leaves the standalone `else` indented one level
*deeper* than its `if` instead (opposite direction), breaking the chain
scan. Fix: widened the recovery to also strip excess indent in that
direction (chain size 1 only). Verified: both files individually
idempotent; full angular corpus (5394 `.ts`) idempotency-violation count
went 9→7, the 2 newly-fixed exactly these files, zero regressions
elsewhere. `make test` 258/258 → 259/259. Fixture
`real_code_regressions_184`. Kotlin shares the same
`BlockStructureRule`/`KotlinSpecificRule.alignBracelessElseIfChain` code —
re-verified only via `make test`, no dedicated Kotlin corpus re-run.

### 2. `microsoft/TypeScript` `harness/collectionsImpl.ts` (FIXED, RDD_KEY_270)

Same "call-wrap vs. alignment-padding decided too early" shape RDD_KEY_248
fixed for `applyDeclarationsPass`, but in `applyAssignmentsPass` — a pass
RDD_KEY_248's own javadoc had explicitly excluded as unaffected; this
investigation disproved that for at least this shape (a wide map-assignment
line's grouping with a narrow sibling line differs between rounds, so the
sibling gets padding on round1 but not round2). Fix: `applyAssignmentsPass`
added as a 3rd pass inside `processScope`'s narrow re-run mode, after the
closing-brace and declarations passes (direct extension of RDD_KEY_248).
`make test` 259/259 → 260/260. Fixture `real_code_regressions_185`. Known
unrelated pre-existing artifact: `js_ts_content_diff.js` flags this file's
top-level `interface`/`class` headers as MISMATCH both before and after —
a content-diff tool gap, not this bug.

### 3. angular `web_animations_player_spec.ts`/`input_transform.ts` (FIXED, RDD_KEY_271)

Same family as #1/#2 but two distinct mechanisms, both confirmed outside
`ScopePipelineCurly.processScope`:
- `tryParseClassField` bailed to "unrecognized field" on any embedded
  NEWLINE, so a field whose initializer gained a NEWLINE from a later
  call-wrap pass got misclassified on round2, disagreeing with round1's
  single-line parse on alignment-grid grouping/column-width. Fix: collapse
  a multi-line initializer's NEWLINE + continuation indent into a soft
  space while scanning, instead of bailing.
- `enforceDecoratorOverflowCascade` (Phase 1) measured a decorator-plus-
  declaration line's fit *before* `enforceUnionIntersectionSpacing`/
  `enforceTypeColonSpacing` (Phase 4) later widened e.g. `string|number`
  inside the decorator's own arguments — a self-violating round1. Fix:
  pulled those two TS-only passes forward to run immediately before
  `enforceDecoratorOverflowCascade` (Phase 4's original calls stay as
  idempotent no-ops).

`make test` 260/260 → 261/261. Fixture `real_code_regressions_186`
(combines both bugs in one minimized file).

---

## Checklist

### Status by style-doc section

All implemented in `JsTsSpecificRule.java` unless noted, wired into
`FormatterCurly`'s phase pipeline (Phase 1 structural/brace, Phase 4 flat
spacing, Phase 5 import ordering). All DONE:

- §1 Baseline-inherited rules.
- Tokenizer support (`TokenizerCurly.java`: `KEYWORDS_JS`/`_TS`,
  `NAMED_CONSTRUCT_JS`/`_TS`, `=>`/`??=`/`??`, `emitTemplateLiteral()`).
- §2 Semicolon insertion (`enforceSemicolonInsertion`).
- §3 Destructuring/spread (`enforceSpreadRestSpacing` + declaration-grid
  join, RDD_KEY_182).
- §4 Template literals (`enforceTemplateLiteralInterpolationSpacing`,
  recursive into nested backtick literals).
- §5 Function/method Allman brace style.
- §6 Arrow functions (`enforceArrowSpacing`,
  `enforceArrowFunctionParameterParens`).
- §7 Optional chaining / nullish coalescing.
- §8 Getter/setter accessors, reuses `GetterSetterRuleCurly`. **Fixed
  2026-08-11**: a JS/TS plain block-bodied method with no return-type token
  now joins adjacent `get`/`set` siblings via an empty return-type grid
  cell without changing C++ constructor grouping;
  `ScopePipelineCurly` normalizes the generated padding back on reformat
  (idempotent). Fixture `real_code_regressions_200`.
- §9 Decorators (`enforceDecoratorTightAtSpacing`,
  `enforceDecoratorOverflowCascade`).
- §10 `async`/`await` spacing — free (required syntax spacing).
- §11 Type annotations (`enforceTypeColonSpacing`,
  `enforceUnionIntersectionSpacing`, `reorderClassFieldModifiers`,
  `enforceClassFieldAlignmentGrid` for §11.2,
  `enforceUnionTypeContinuationIndent` for §11.1).
- §12 Enums (`enforceEnumMemberFormatting`).
- §13 Generics (`<T>`) — bracket-complexity detection reused from C++/Java
  (`reclassifyAngleBrackets`/`isGenericSafeToken`, extended with TS
  keywords per dogfood findings below); comma-spacing via
  `enforceGenericArgumentCommaSpacing`.
- §14 Interface/object-shaped `type`-alias member `:` alignment
  (`enforceInterfaceTypeAliasMemberColonAlignment`, RDD_KEY_196).
- §15 Import ordering (`enforceImportOrdering`, RDD_KEY_195, RDD_KEY_197).
- Declaration-alignment grid (`let`/`const`/`var`/`type`), incl.
  destructuring-pattern LHS (RDD_KEY_182) and `type X = ...` groups
  (RDD_KEY_183) via `JsTsDeclarationAlignmentRule`. Multi-declarator
  statements (`let a = 1, b = 2;`) deliberately stay unaligned, matching
  C++/Java's existing behavior — confirmed not a gap.
- Single-declarator colon spacing — **FIXED 2026-08-04**: `const x: number
  = 1;` rendered with a spurious space before `:` for a singleton (no
  alignment-group neighbors), because `ColumnGrid.flush()` always joins
  adjacent cells with a space even for one. Fix: merge name and `: type`
  into one cell when `group.size() == 1`; real (`> 1`) groups keep their
  deliberate §11.2 space-before-`:` look. Fixture `real_code_regressions_177`;
  `real_code_regressions_107_out.ts` updated to match. `make test` 239/239.
- Real-code testing — DONE for `expressjs/express`, `nestjs/nest`,
  `vuejs/core`, `lodash/lodash`, `angular/angular`, `microsoft/TypeScript`.

### Fixed implementation-gap bugs (settled, historical — conclusions only, full narrative in `git log`)

**js_combined/js_comments activation**: comment inside a destructuring
pattern dropped on reformat (fixed: scan raw tokens, bail multi-line on
interior comment); ASI-vs-alignment-grid ordering (fixed: ASI runs first);
array-destructuring `,`→`...` missing space (fixed: `const`/`let`/`var`
bail-out in `parseAssignment`, mirrors C++'s `auto [a, b]` bail-out).

**ts_combined/ts_comments activation**: `Map<string,number>` ASI bug
(`GENERIC_SAFE_KEYWORDS` missing TS primitives, extended); enum
last-member no-trailing-comma bug (`break` instead of bailing whole enum);
generic-argument comma spacing (new `enforceGenericArgumentCommaSpacing`);
new `enforceUnionTypeContinuationIndent` (bug: bailed on any comment, fixed
to only bail on frozen tokens); new `enforceClassFieldAlignmentGrid` with
two bugs fixed in `rewriteClassFieldGroups` (double-indented first field;
duplicate blank line before a leading-commented group).

**expressjs/express (141 `.js`, HEAD `ae6dd37`)**: two bugs, fixture
`real_code_regressions_77`: ASI leading-continuation-operator/comma bug
(`maybeInsertSemicolon` only checked the previous line's trailing token;
fixed via `LEADING_CONTINUATION_OPS` + leading-`,` check); no regex-literal
tokenizing (fixed via `emitRegexLiteral`/`isRegexLiteralAllowedHere`).
Final: 141/141 `node --check`, idempotent, semantic smoke test passed.

**nestjs/nest (HEAD `7e6e313`)**: five bugs fixed: (1) `/**` JSDoc opener
corruption, universal curly-family bug in `reformatMultiLineBlockComment`
(assumed 2-char opener; fixed to scan forward while char is `*`; also fixed
Java/Kotlin); (2) dot+space corruption in `renderCallCandidate`'s
`sigForRender` for multi-arg calls whose every arg is a bare dotted
member-access (fixed: force `sigForRender` to `null` for JS/TS, fixture
`_81`); (3) content duplication in `enforceClassFieldAlignmentGrid` on
nested `class` braces (fixed: only grid outermost brace per nesting level,
fixture `_82`); (4) comment-continuation-indent drift on an object-shaped
intersection alias (fixed: only reindent at depth 0, fixture `_84`); (5)
`join(...)` call-wrap/collapse non-idempotency at exactly `lineLengthLimit`
(fixed: fits-check on multi-line-source branch, fixture `_93`).

### `vuejs/core` dogfood — DONE

`utils.ts` switch-case fallthrough non-idempotency found here first
(confirmed again in `lodash/lodash`, fixed project-wide as `RDD_KEY_263`).
No other bugs; all other files clean/idempotent.

### `lodash/lodash` dogfood — DONE

27 in-scope `.js` files (`dist`/`vendor` excluded), 50983 lines. Baseline
`node --check` 27/27. Round1: zero crashes, 27/27 formatted, `node --check`
27/27. Idempotency: 26/27; `lodash.js` was the switch-case-fallthrough issue
above (later fixed). Content-preservation: initial 17/27 MISMATCH all
decomposed into intentional non-lossy transforms → became `js_ts_content_diff.js`
tolerances 1-3 (see Dogfood Output Validation) → 22/27 clean. Remaining 5
were two more confirmed-intentional classes (bare-arrow-parens, 3 files;
pre-increment-except-when-post-required for-loop rewrite, 2 files) → became
tolerances 4/5 (2026-08-10, verified via 4 synthetic pairs incl. 2 negative
controls since the original checkout's trigger files no longer existed on
disk). **Verdict: DONE**, zero new formatter bugs — checker now reports
27/27 clean.

### `angular/angular` dogfood

Clusters 1-3 FIXED, cluster 4 FIXED (all named root causes + the final
`alignBracelessElseIfChain`/`processScope`-family residue, see "Active
work" above), cluster 5 RESOLVED. Repo: `/tmp/angular`, HEAD `5ad8231`,
5394 `.ts` files across `packages/`/`adev/`/`devtools/`/etc. Syntax check:
TS compiler-API parse-only.

**Initial**: 0 crashes; 29 idempotency mismatches; round1 had **46/5394**
files with real parse errors (339 diagnostics) despite zero hard crashes.

**Clusters 1-3, all FIXED:**
1. **[CRITICAL]** Dotted/qualified type-predicate before `=>` wrapped its
   last segment in a spurious paren pair (~40/46 broken files, e.g.
   `node is tss.Node =>` → `node is tss.(Node) =>`). Fix:
   `enforceArrowFunctionParameterParens`'s backward scan now walks back
   over any number of `IDENTIFIER '.'` pairs before its `is`/`typeof`/
   `keyof` bail. Fixture `real_code_regressions_134`.
2. **[CRITICAL]** `<Type>{...}` legacy cast misparsed as a generic,
   injecting a bogus `;` into the object literal (`testability.ts:229`).
   Fix: new `isLegacyCastBrace`. Fixture `real_code_regressions_135`.
3. **[CRITICAL]** Multi-line generic return-type clause lost its closing
   `>` (`utils.ts:103-105`, dynamic-import type-query operand). Fix: add
   `"import"` to `GENERIC_SAFE_KEYWORDS`. Fixture `real_code_regressions_136`.

`make test` after clusters 1-3: 185/185.

**Cluster 4 — call-wrap/collapse vs. alignment-padding fits-check
ordering** (dominant, ~23/29 idempotency-mismatch files): fits-checks
measuring a candidate line before declaration-alignment/complexity-padding
finish adjusting column widths, flip-flopping every round.
- **Root cause #1** (trailing-comma dangling-empty-group,
  `create_router_state.ts:27`): `renderCallDropped`/`renderCallOnePerLine`
  measured via `splitTopLevelCommas`, which unlike `groupByOriginalLine`
  didn't drop a dangling trailing empty group from a trailing comma before
  `)`. Fixed by adding the same drop to both. Fixture
  `real_code_regressions_140`.
- **Root cause #2** (`if (`/`if(` keyword-spacing ordering,
  `node_selector_matcher.ts:155`/`locale_plugin.ts:42`): the fits-check for
  a call inside `if (...)` measured before `enforceKeywordSpacing`
  collapses `if (` → `if(`, flipping a boundary case by exactly one char.
  Fixed by pulling `enforceKeywordSpacing` forward before the first
  `enforceCallLineBreaking` call (applies to all curly-brace languages).
  Fixture `real_code_regressions_141`. `make test` after both: 190/190.
- **Root cause #3** (braceless-else body never re-validated after
  brace-collapse, `format_date.ts:519`): `collapseSingleExpressionBlocks`
  strips braces before `enforceCallLineBreaking` runs, so a body that used
  to fit via a complexity-wrap that doesn't apply braceless stays
  over-limit, and `alignBracelessElseIfChain` pads it anyway (intentional
  escape hatch there, not the bug). **First attempt reverted**: refusing
  collapse whenever the joined one-liner exceeds `lineLengthLimit` — do NOT
  retry, it wrongly re-braced every wrappable-call-body braceless if/else
  (broke 5 fixtures incl. `java_combined`). **Real fix (landed
  2026-07-31)**: `BlockStructureRule.refuseUnrescuableCollapse` — refuse
  collapse only when over-limit AND `hasBreakableCall` finds no rescuable
  call (a `name(args)`-with-nonempty-args span), a cheap-heuristic
  precedent shared with `JavaSpecificRule.isSingleLineBody`. Called from 3
  sites incl. a previously-ungated bare-terminal `else{...}` chain-collapse
  path found while building the fixture. Scans the *whole* candidate
  (condition + body), not body-only (a body-only scan broke
  `real_code_regressions_141`, whose rescuable call is in the condition).
  Fixture `real_code_regressions_172`. `make test`: 221/221. Full
  `packages/` re-scan (3900 files): 12 differ (down from ~23; 3 are the
  separate cluster 5 gap, 9 not fixed this session — see below).
- **Root cause #4 [FIXED]** (trailing same-line comment inconsistently
  counted in the collapse fits-check, `location_shim.ts:461`): fresh format
  counts the comment's width (over-limit once alignment padding widens the
  column) → wraps; reformat measures without it (comment now past the
  call's `)`) → collapses back. Fixed via
  `appendRangeCollapsingTrailingCommentGap` (measurement-only, never
  rendered). Fixture `real_code_regressions_142`. `make test`: 191/191. A
  related `=`-alignment-group instability was seen while building the
  fixture but did NOT reproduce against real `location_shim.ts` — flagged
  under Open Questions above, not investigated further.

  **Known residual limitation — FIXED (RDD_KEY_294).** `hasBreakableCall`
  only asked "does a rescuable call exist," not "will wrapping it actually
  bring the line under the limit" — when the one breakable call's wrap
  didn't shrink the line far enough (e.g. dominated by a long `+`-chain),
  collapse still proceeded and the line stayed over-limit post-wrap. Fixed
  via a narrower post-wrap-estimate comparison (`maxRescueSavings`,
  optimistic upper-bound), gated off whenever an array/object literal is in
  the scanned span (`containsListLiteral`) — an ungated first attempt
  regressed `real_code_regressions_81` for the same reason RDD_KEY_249
  already documented for that fixture ("over limit is fine, rejoin anyway,
  as long as it's round-stable"). See RDD_KEY_294 for the full writeup.
  Fixture `real_code_regressions_209`. `make test`: 318/318.

**2026-08-07 re-scan (doc-only, no fix)**: full `packages/`+`devtools/`
re-scan, 3906 files. 7 of the 9 previously-"not fixed" root-cause-#3 files
are now idempotent as a side effect of RDD_KEY_248. Full re-scan: only
7/3906 still non-idempotent — 3 cluster 5 (below), 2 the `processScope`
family (RDD_KEY_270), 2 the `alignBracelessElseIfChain` cause (RDD_KEY_269)
— both fixed since, see "Active work" above.

**Cluster 5 — RESOLVED 2026-08-05.** `user_metric_spec.ts`/`i18n_parse.ts`/
`emit.ts` — a pre-existing inconsistent-source-reindentation architectural
gap (GDR pipeline interaction, `RDD_KEY_229`, full investigation in
`STATE_CURLY_GDR.md`). A safer multipass GDR sequence
(`curly-general-scope-reindent-multipass`, opt-in, `RDD_KEY_233`/
`RDD_KEY_234`, landed in the GDR job) resolves all 3/3 files to a
zero-line diff, syntax-clean. Both flags remain `off` by default
project-wide — a per-corpus recommendation, not a default change.

### `microsoft/TypeScript` dogfood

3 of 4 clusters FIXED outright; cluster #3 substantially reduced by
RDD_KEY_248. Checkout `/tmp/ts-dogfood/TypeScript`, HEAD `cc5c6e2`, `src/`
only, 601 `.ts` files, 379045 lines (`.d.ts`/`.tsx`/`tests/**` excluded).
Baseline 0/601 parse errors; round1 zero crashes, 601/601 produced.

**Category 1 — Critical, 8/601 files, 3 root causes, ALL FIXED
(2026-07-28):**
1. `||=`/`&&=` not tokenized as one token (1 file, `checker.ts`) —
   `MULTI_CHAR_OPS` had `??=` but not `||=`/`&&=`. Fixture
   `real_code_regressions_143`.
2. Union-type return-type/predicate before `=>` gets its last segment
   spuriously wrapped in parens (6 files; same function as angular cluster
   1 but needed a union-`|` walk-back too). Fix: walk-back loop alternates
   `IDENTIFIER '.'` and `IDENTIFIER '|'` until neither makes progress.
   Fixture `real_code_regressions_145`.
3. Backslash-newline string continuation, CRLF-specific corruption (2
   files): `emitString`'s escape handling only consumed `\` + the `\r` half
   of `\r\n`. Fixed to advance 3 chars for `\` + `\r\n`. Fixture
   `real_code_regressions_147` — `.gitattributes -text` marked (CRLF).
   `make test`: 196/196.

**Category 2 — Idempotency-only, cluster #3** (originally 28/601 files):
same root cause as angular cluster 4, third confirming recurrence,
proportionally ~30x denser. `commandLineParser.ts`'s exact shape is fixed
by RDD_KEY_248 (reconfirmed empty diff, plain and at `indent-size=2`).

**2026-08-07 re-scan**: fresh full 601-file round1→round2 shows only
14/601 still differing. `checker.ts` still shows the known
`hasBreakableCall` residual (left alone, documented above).
`harness/collectionsImpl.ts` was the `applyAssignmentsPass` sibling bug,
fixed as RDD_KEY_270.

**Ranked bug-hunt summary (most-valuable-first)**: (1) `||=`/`&&=`
tokenizer gap; (2) union-type-before-`=>` spurious wrap; (3) call-wrap vs.
alignment-padding ordering (substantially fixed, residue under "Active
work"); (4) backslash-newline CRLF string corruption (narrow, 2 files). All
FIXED.

### Other real-code-found bugs (fixed)

- **`compiler/watchPublic.ts` nested-array-literal corruption — FIXED
  2026-08-09.** `new Map([[undefined, undefined]])` got a stray `;`
  inserted inside the call's parens on a single pass (real corruption, not
  just non-idempotency). Root cause: C++11 `[[attribute]]`-open detection
  was missing the `&& lang.isCpp` guard its sibling branches already had,
  so a TS nested `[[` matched the C++ heuristic and tokenized asymmetrically
  (OP open / PUNCT close), undercounting bracket depth in every
  `isPunct(t, "[")`-based tracker including `enforceCallLineBreaking`'s
  `matchParenForward`. Fixed by adding the missing guard (one line). `make
  test` 271/271. Fixture `real_code_regressions_194`.

- **Braceless if/else CRLF-staleness — FIXED 2026-08-09, RDD_KEY_273** (see
  RDD index above for full detail).

- **Class-field alignment-group splitting on a same-line leading comment —
  FIXED 2026-08-09, RDD_KEY_274** (see RDD index above).

- **Closing brace non-idempotently gains a `// if` comment — FIXED as side
  effect, RDD_KEY_275** (see RDD index above).

- **Interface field named `class` misclassifies its own nested brace —
  FIXED 2026-08-09, RDD_KEY_276** (see RDD index above). Note: first fix
  attempt used `isPunct(t, ":")` and silently never matched — `:` tokenizes
  as OP, not PUNCT, in this codebase; corrected to `isOp`.

### JSX/TSX dogfood — see "JSX/TSX implementation" above

`taniarascia/react-tutorial`, `microsoft/TypeScript-React-Starter`,
`ruanyf/react-demos`, `Lemoncode/react-typescript-samples`,
`reactstrap/reactstrap`, `excalidraw/excalidraw` — all 6 corpora run and
folded into the Step 1/Step 2 write-ups above (fixture names, bugs found,
verdicts). Not duplicated here.

### Known false positives (no source change needed, fixture-only)

Spurious-looking blank line after a class's opening `{` in older `.js`
fixture drafts, and doubled trailing space before a one-liner getter body's
closing `}` — both confirmed correct, existing behavior (STYLE.md §7
named-construct blank line; `GetterSetterRuleCurly`'s group-width body
padding), matching passing C++/Java/Kotlin fixtures byte-for-byte. Only the
stale hand-authored `.js` draft fixtures were wrong; resolved by
regenerating them.
