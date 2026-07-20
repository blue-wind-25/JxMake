# STATE_JS_TS.md — JavaScript / TypeScript JAR Support Tracker

Read `STATE_COMMON.md` first — it has the shared commit/ambiguity/testing
conventions this file assumes. `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` (the
other jobs' files) are NOT required reading for this one — only
`STATE_COMMON.md` is.

---

## Purpose

Tracks implementation of JavaScript/TypeScript support in the deterministic
JAR formatter (`util/CodingStyle.md/formatter/`), per `STYLE_JS_TS.md`
(which derives most rules from `STYLE_JAVA.md`/`STYLE_KOTLIN.md` given
JS/TS's C-family brace/paren/statement shape). **Current status is
scaffold-only: dispatch exists only as a "not yet implemented" error thrown
for JS/TS constructs, no real formatting logic exists yet.**

---

## Next Steps (work ordering, set by the user)

Scaffold gate for js/ts has already been flipped by the user (outside a
checkpoint commit — `Lang.isScaffoldOnly` no longer includes js/ts). The
Makefile's local js/ts fixture lines (`INP_FILES += js_combined_inp.js`
etc.) are still commented out as of this note. Agreed order for remaining
work, before moving on to any other language job:

1. ~~Unblock JS from HTML5~~ — **DONE.** `XmlSpecificRule.renderScriptOrStyle`
   dispatches real `<script>` content to `FormatterCore.forLanguage("js")`,
   mirroring `<style>`'s CSS splice. Both sub-bugs found while finishing
   this step are fixed:
   1a. **CDATA unwrap/rewrap** — `renderScriptOrStyle` now detects a
       dedented/trimmed raw body bounded by `<![CDATA[`/`]]>`, strips both
       markers before dispatching the inner text to the JS formatter, and
       re-wraps the formatted result in `<![CDATA[\n...\n]]>\n` before the
       HTML-level `reindent`. Without this, the literal `<![CDATA[` text
       was fed straight into the JS tokenizer as if it were source — it
       didn't crash (unrecognized tokens pass through largely unchanged),
       but it silently broke the declaration-alignment grid for `var`/`let`/
       `const` groups following it (confirmed via a standalone `.js`
       repro with a literal `<![CDATA[` header line: `=` alignment across
       `var now = ...; var elapsed = ...;` was lost, while brace placement,
       semicolon insertion, and the closing-comment pass were unaffected).
       No remaining `//% JXM_CFMT_DIS`/`ENA` freeze wrapper needed —
       confirmed removed from both HTML fixtures.
   1b. **Spliced JS indentation doubling on idempotency round-trip** — the
       original "body flush against the margin" symptom this item was filed
       under turned out to be the same, already-documented general
       limitation as `STATE_COMMON.md`'s "General scope-depth
       reindentation" gap (this formatter preserves original relative
       indentation, it doesn't reindent flush-left bodies from scratch) —
       not something to special-case in the splice path. The actual,
       splice-specific bug was different: `renderScriptOrStyle` fed the JS
       formatter the raw `<script>` body without removing its existing
       baked-in absolute indentation, then unconditionally added another
       `reindent(..., depth + 1)` layer on top — round1 was fine, but
       reformatting round1's own already-spliced output (idempotency
       round2) doubled the indentation every round, since the previous
       round's baked whitespace survived untouched through the JS formatter
       and got a second prefix stacked on it. Fixed via a new `dedent()`
       helper (strips the common leading whitespace shared by every
       non-blank line of the raw body) applied before dispatch, so each
       round starts from the same flush-left baseline regardless of what
       absolute depth the previous round's `reindent` left behind.
   The `Config`-threading TODO (the throwaway `Config.resolve(null,
   overrides)` synthesis from 4 primitive fields, instead of the real
   resolved `Config`) is **still open, not addressed by this step** — see
   Open Design Questions below; it doesn't block correctness for any
   currently-exercised JS/TS config key, only for someone relying on a
   JS/TS-specific key (`js-import-order`, etc.) inside spliced `<script>`
   content specifically.
   Both HTML fixtures (`test/html_combined_inp/out.html`,
   `test/html_comments_inp/out.html`) are updated and green: `make test`
   106/106 forward + 106/106 idempotency.

   Beyond steps 2-3 below, "JS/TS implemented, just needs dogfooding" is
   NOT yet accurate — these are real implementation gaps, not just
   untested code, and should be closed before treating the job as feature-
   complete:
   - **RDD_KEY_182/183**: destructuring-pattern LHS, multi-declarator
     statements, and `type X = ...` alias groups aren't joined to the
     declaration-alignment grid yet — explicitly "not yet implemented",
     not just untested.
   - **`static get`/`set` vs. plain accessor padding inconsistency** —
     confirmed real bug (Still Open #5's second half), no fix attempted.
   - **Import-ordering comment/blank-line group-break rework**
     (RDD_KEY_197) — design decided, code change not written (segment at
     standalone comments instead of bailing the whole pass).
   - **Nested template-literal interpolation** — `` `${`inner ${x}`}` ``
     isn't recursively reformatted (known low-priority gap, §4).
   - The `Config`-threading TODO above (HTML `<script>` splice path).

   Only once those are closed does it become "implemented, just dogfood"
   — i.e. the real-code testing pass against `nodejs/node`, `express`,
   `lodash`, `microsoft/TypeScript`, `angular`, `nestjs`, `vue` already
   listed in the Test-Fixture Repos section but not yet run.
2. Activate `test/js_combined_inp/out.js` and `test/js_comments_inp/out.js`
   in the Makefile, run `make test`, and bug-fix whatever surfaces.
3. Activate `test/ts_combined_inp/out.ts` and `test/ts_comments_inp/out.ts`
   in the Makefile, run `make test`, and bug-fix whatever surfaces.

Rationale (user's own words): the JS/TS basics should be solid before
dogfooding — get this job to a genuinely stable baseline first, then move
on to Python3 (next language job in the rotation) rather than continuing to
add JS/TS scope indefinitely. The remaining "Still Open"/"Open Design
Questions" items below (import-ordering RDD_KEY_197 rework, `static`
get/set padding inconsistency, real-code testing pass against external
repos) stay tracked but are lower priority than steps 1-3 above.

---

## Scope

`STYLE_JS_TS.md` covers latest ECMAScript (ES2024+) and latest TypeScript
(5.x), one shared file for both (TS is a syntactic superset of JS). **Out of
scope entirely** (not just deferred): JSX/TSX — see Open Design Questions
below.

Sections:

1. Baseline directly inherited unchanged from `STYLE.md`/`STYLE_JAVA.md`:
   bracket/paren complexity padding, keyword spacing, `{}` spacing, closing
   comments on blocks, blank line before `return`, `else`/`else if`
   placement, `switch` formatting.
2. Statement termination — always insert explicit semicolons, never rely on
   ASI (opposite of Kotlin's clean no-semicolon default, since JS's ASI is
   an error-recovery mechanism with real hazards).
3. Destructuring and spread (`{...rest}`, `[...items]`) — no space after
   `...`, bracket padding per §1.
4. Template literals — content preserved exactly as written (never reflowed
   /reindented), `${...}` interpolation gets normal expression spacing.
5. Function/method brace style — Allman for named functions/class methods
   (mirrors Java), with K&R/one-liner exceptions for arrow function bodies,
   getter/setter one-liner groups, and empty bodies.
6. Arrow functions — spaced `=>`, same-line no-brace for single-expression
   body, K&R brace for block body; parameter parens always kept even for a
   single untyped parameter.
7. Optional chaining/nullish coalescing (`?.` tight, `??` spaced) — direct
   analog to Kotlin's `?.`/`?:`.
8. Getter/setter accessors — direct application of the existing
   getter/setter one-liner group alignment (STYLE.md §14), since JS/TS
   accessors always have a real block body.
9. Decorators — `@Name`/`@Name(args)`, tight `@`, placement (own-line vs.
   inline) preserved as written, overflow cascade (drop to own line, then
   wrap args).
10. `async`/`await` — `await` as unary prefix, tight against operand.
11. TypeScript type annotations — `: type` colon spacing (Kotlin `: type`
    analog); union/intersection (`|`/`&`) ordinary binary-operator spacing,
    break-style preserved as written on overflow; class field modifier
    priority table (own ordering: `declare`, visibility, `static`,
    `abstract`, `override`, `readonly` — TS-only modifiers slotted relative
    to Java's existing order, cannot reuse Java's table wholesale).
12. Enums — always one-member-per-line (unlike Java's packing convention),
    `=` column-aligned when explicit values are present, closing comment,
    no trailing `;`.
13. Generics (`<T>`) — same tight/loose bracket-complexity approach as
    C++/Java generics.
14. `interface`/`type` alias declarations — member `:` alignment like §11;
    brace style is K&R (container construct, not function/method).
15. Import ordering — group/blank-line shape like Java's import ordering,
    but JS/TS-specific groups (built-in/node:, third-party, local);
    configurable `js-import-order`/`-sort`/`-blank-lines`. Local-import
    path classification logic is explicitly not yet designed (see Open
    Design Questions below — this is the "Import-path classification" open
    item from the style doc, separate from the HTML5/JSX dispatcher
    questions).

Scaffold dispatch lives in the shared `Lang.java`/`Main.java`/
`ServerMode.java`/`Config.java`, described in the routing `CLAUDE.md`
table. This job's own rule class, `rules/JsTsSpecificRule.java`, exists
only as a boilerplate stub (constructor throws
`UnsupportedOperationException`) — no real logic yet.

---

## Resolved Design Decisions

Full text of each decision lives in `RDD_LOG.md` (shared with
`STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` — continue the existing `RDD_KEY_n`
numbering, do not restart). See `STATE_COMMON.md`'s lookup convention
(`grep -Fm1`, no `-A`).

| Key | Topic |
|---|---|
| RDD_KEY_182 | §3/§6 destructuring-pattern LHS joins the const/let alignment grid like any ordinary declaration |
| RDD_KEY_183 | §11.1 consecutive `type X = ...` aliases form their own `=`-aligned group, same as const/let |
| RDD_KEY_187 | Class Scoping — no separate JsTokenizer/TsTokenizer etc.; shared curly classes gated internally on `isJs`/`isTs`, concrete rules in one `JsTsSpecificRule.java` |
| RDD_KEY_195 | §15 local-import classification — drop the source-root disjunct entirely; only `./`/`../`-prefixed specifiers are "local", everything else non-built-in is "third-party" (accepted misclassification of bundler/tsconfig `baseUrl`/`paths` absolute imports) |

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

---

## Tools/compiler used

Compiler for dogfood test `node` and `tsc` needs:

```bash
export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules
export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
```

---

## Test Fixtures (Local)

Local dogfood pairs (distinct from the external-repo list above, which is
for corpus-scale validation) have been authored and registered in
`formatter/test/` — see `test/README.txt` for the pair list and what each
covers. The pairs are split by extension (`.js` vs. `.ts`), not shared —
same separation C/C++ already have across their own fixture families,
since TS-only constructs (decorators, enums, generics, interfaces) can't
live in a valid `.js` file.

---

## Class Scoping (post Core/Curly/Indent/Tags refactor)

JS/TS are curly-family (`Lang.isCurly()` covers `isJs`/`isTs` alongside
C/C++/Java/Kotlin). **Resolved (RDD_KEY_187):** no separate `JsTokenizer`/
`TsTokenizer` (or `JsTsTokenizerCurly` intermediate) — JS and TS share
`TokenizerCurly`/`FormatterCurly`/`ScopePipelineCurly`/`MiscRuleCurly`
directly, gated internally on `lang.isJs`/`lang.isTs`, the same way Kotlin
is gated inside those same classes today rather than getting its own
`KotlinTokenizer`/`KotlinFormatter`. Concrete JS/TS-only rule logic (§2–15)
lands in a single `JsTsSpecificRule.java` (boilerplate stub created, throws
`UnsupportedOperationException` until real logic lands), mirroring
`KotlinSpecificRule.java`'s role, gating TS-only additions (type
annotations/interfaces/enums, §11–14) internally on `lang.isTs` rather than
splitting into `JsSpecificRule`/`TsSpecificRule`.

The existing HTML5-dispatcher and JSX/TSX-out-of-scope open questions
below are unaffected by the refactor.

## Open Questions

### Scaffold-gate flip — blocking bug found and since fixed; flip not re-attempted

An earlier attempt to flip `Lang.isScaffoldOnly` for `js` (smoke-testing via
the real `Main.java` CLI path for the first time) found that a plain
unspaced declaration (`let x=1;`, `const x=5;`, `var z=7;`) was left
completely untouched end-to-end: `DeclarationAlignmentRuleCurly`'s
constructor had no `isJs`/`isTs` branch, so JS/TS fell into the C/C++
modifier-priority arm and `let`/`var` were never recognized as declaration
keywords — `parseDeclaration` returned `null` and no `=`-spacing was ever
applied. The `Lang.java` gate-flip edit from that session was reverted (no
code change kept); the bug itself is **now fixed** by the later
`JsTsDeclarationAlignmentRule` work (see Checklist below), verified via
harness for plain single-declarator statements.

**Before re-attempting the scaffold-gate flip**, a future session should:
rebuild the CLI smoke-test (including a deliberately unspaced plain
declaration, not just destructuring/already-correct examples), confirm it's
clean, then also wire `XmlSpecificRule.renderScriptOrStyle`'s `isJsType &&
!frozen` branch to dispatch to the real JS formatter and update the two
blocked HTML fixtures (see the follow-up note at the bottom of the
Checklist). Real-code testing pass and the currently-open Checkpoint-21
bugs (below) don't strictly block the flip, but flipping while they're open
means marking JS "done" while known bugs remain — judgment call for that
session.

### Open Design Questions (not attempted, future work)

- **`XmlSpecificRule`'s `<script>` dispatch doesn't thread the real resolved
  `Config`.** `renderScriptOrStyle` synthesizes a throwaway `Config` via
  `Config.resolve(null, overrides)` from just 4 primitive fields
  (`lineLengthLimit`, `indentWidth`, `useTabs`, `normalizeCommentStartCase`)
  it happens to already carry, silently defaulting every other JS/TS-
  specific knob (`js-import-order`, `js-import-sort`, `js-import-blank-
  lines`, etc.) instead of inheriting the enclosing HTML file's actually-
  resolved `Config`. Should be refactored to thread the real `Config`
  through from `FormatterXml.formatOne` (which already has it as a
  parameter) into `XmlSpecificRule`'s constructor. Not blocking — no
  currently-exercised fixture relies on a non-default JS/TS config key
  inside spliced `<script>` content — but should land before relying on one.
- **HTML5 needs its own dispatcher.** A `.html` file can embed inline
  `<script>`/`<style>` blocks that need to be spliced out, formatted by the
  appropriate sub-formatter (JS/TS for `<script>`, CSS for `<style>`, per
  `STYLE_DATA_FORMATS.md` §4.2), and spliced back with correct
  reindentation. Unsolved — belongs at the intersection of this job and the
  Data Formats job (tracked in `STATE_DATA_FORMATS.md`).
- **JSX/TSX will need their own future dispatcher**, different from HTML5's
  case — JSX embeds tag syntax directly inside JS/TS expression position, a
  compound-language situation, not a same-file extension. `STYLE_JS_TS.md`
  puts JSX/TSX **out of scope entirely** (not merely deferred).
- **Cross-job follow-up (from `STATE_DATA_FORMATS.md`'s HTML5 §4 work):**
  `XmlSpecificRule.renderScriptOrStyle` currently throws for any real
  (non-frozen) `<script>` content since JS/TS has no dispatch target yet.
  Two local fixtures (`test/html_combined_inp/out.html`,
  `test/html_comments_inp/out.html`) work around this with a temporary
  `//% JXM_CFMT_DIS`/`//% JXM_CFMT_ENA` wrapper forcing their `<script>`
  bodies to stay opaque. Once real JS/TS logic is live and the scaffold gate
  flips: (1) wire an actual dispatch call into `renderScriptOrStyle`
  (reformat spliced-out content, reindent back in, matching `<style>`'s
  existing CSS-splice shape); (2) remove the DIS/ENA wrapper from both HTML
  fixtures; (3) regenerate both `_out.html` files and re-run `make test`.

---

## Checklist

### Status by style-doc section

All items below are implemented in `JsTsSpecificRule.java` unless noted, and
wired into `FormatterCurly`'s phase pipeline (Phase 1 for structural/brace
passes, Phase 4 for flat spacing passes, Phase 5 for import ordering).
`make test`: 106/106 forward + 106/106 idempotency, zero regressions,
current as of the latest checkpoint (Checkpoint 23) — true after every
entry below unless noted otherwise.

- **§1 Baseline-inherited rules** (bracket/paren complexity padding, keyword
  spacing, `{}` spacing, closing comments, blank line before `return`,
  `else`/`else if`, `switch`) — DONE, free once `lang.isJs`/`isTs` are
  recognized in the curly-family classes. Confirmed narrowly gated:
  Java's array-decl/double-brace-init tight exception stays Java-only;
  Kotlin's unconditional-blank-line closing-comment override does not leak
  onto JS/TS (JS/TS uses the plain threshold-gated behavior); Java's
  no-closing-comment-on-`else` suppression keys off general, language-
  agnostic behavior so it also covers JS/TS. Fixed (Still-Open bug 2):
  `BlockStructureRule.classifyBrace` only ever recognized if/for/while/
  switch control-flow bodies and tokenizer-tagged named constructs — a
  plain function/method body brace fell through to `Frame.other` (no
  closing comment, ever, regardless of length), even though §1's own
  `} // function foo` worked example requires one past the threshold.
  Added a new `Kind.FUNCTION` case, scoped to `lang.isJs`/`isTs` only (no
  effect on C/C++/Java/Kotlin): a `)`-then-`{` body whose matching `(` is
  preceded by an IDENTIFIER (not a control keyword) is labeled with that
  bare identifier — already excludes modifiers for free, since `async`/
  `static`/a generator's `*` all sit further back, never directly before
  `(`. Threshold-gated exactly like FOR/WHILE/SWITCH, not unconditional
  like NAMED. RDD_KEY_196 confirms this bare-name-only labeling (no
  modifiers) is the intended, final design for every modifier-prefixed
  method shape (`async`/`static`/`get`/`set`/etc.), verified directly
  against `static get instance()` → `} // instance` in addition to the
  originally-named `async load(...)` → `} // load` case.
- **Tokenizer support** — DONE, additive only, in `TokenizerCurly.java`
  (`TokenizerCore.java` untouched): `KEYWORDS_JS`/`_TS`,
  `NAMED_CONSTRUCT_JS`/`_TS`, `=>`/`??=`/`??` multi-char ops, a dedicated
  `emitTemplateLiteral()` emitting the whole backtick literal as one opaque
  STRING token. Decorators/TS `:`/`|`/`&`/generics `<>` needed no new
  lexing. `${...}` interior re-tokenizing for spacing was deferred to §4's
  own rule (now done, see below).
- **§2 Semicolon insertion** — DONE (`enforceSemicolonInsertion`),
  depth-stack scan gated on statement-body vs. value braces
  (`classifyBraces`). Must run *before*
  `BlockStructureRule.collapseSingleExpressionBlocks` in Phase 1 (an
  idempotency bug appeared when ordered after). `classifyBraces` has since
  grown special cases for object-destructuring LHS, import named-list
  braces (incl. `import type {`), and enum bodies — each found via a
  distinct "value vs. statement-list brace" misclassification bug (fixed).
- **§3 Destructuring/spread** — DONE: `enforceSpreadRestSpacing` (no space
  after `...`); bracket padding on destructuring patterns already free via
  the existing complexity-padding pass; destructuring-pattern LHS join to
  the declaration-alignment grid is RDD_KEY_182 (design decided, **not yet
  implemented** — see grid status below). Fixed (Still-Open bug 4):
  `{ id, name, ...rest }` object-destructuring patterns stayed comma-tight
  since `MiscRuleCore.enforceInitializerBraceSpacing`'s brace-initializer
  detection only recognized a `{` preceded by `=`/import/call-paren, never
  a `{` preceded directly by `const`/`let`/`var` (the destructuring LHS
  shape) — added that case. Array-destructuring commas (`[first, second]`)
  and arrow-parameter commas (`(a, b)`) were already fixed for free by the
  existing bracket-generic/declaration-grid passes; only the object-pattern
  `{}` case needed this fix.
- **§4 Template literals** — DONE (`enforceTemplateLiteralInterpolationSpacing`):
  finds top-level `${...}` spans (nesting-aware, quote-span-skipping),
  re-tokenizes/re-renders each interior in isolation. Bails out (leaves
  untouched) on a NEWLINE/comment/frozen token inside a span. **Known gap:**
  a nested template literal inside an interpolation (`` `${`inner ${x}`}` ``)
  is treated as one opaque quoted span — its own interior `${...}` is not
  recursively reformatted (documented, low-priority, not yet attempted).
- **§5 Function/method Allman brace style** — DONE
  (`enforceMethodDefinitionAllmanBraceStyle`), candidate signal excludes
  control-flow/anonymous-function/arrow-body braces by construction; K&R
  exceptions for empty bodies and one-liners. Fixed: a generic
  type-parameter list (`identity<T>(...)`) was suppressing the candidate
  signal because the token before `(` was `>` not an IDENTIFIER — fixed via
  `matchAngleOpenBackward` walking back over the `<...>` pair before
  re-checking for the name identifier. Fixed (Still-Open bug 1): a
  generator method's `*` marker got a wrongly-inserted space
  (`*iterate()` → `* iterate()`) — `MiscRuleCurly.render`'s lead-token
  join used `MiscRuleCore.needsSpaceBetween`'s C/C++-pointer-declarator
  default (space before the name); JS/TS `*` here is never a pointer
  sigil, only ever the generator marker, so it's now forced tight
  whenever it's the signature's last lead token and `lang.isJs`/`isTs`.
- **§6 Arrow functions** — DONE (`enforceArrowSpacing`,
  `enforceArrowFunctionParameterParens` — always parenthesizes a bare
  single param, confirmed unambiguous from the style doc's own text). K&R
  block-body braces and brace-free single-expression bodies need no code
  (excluded from §5's Allman signal by construction; this codebase never
  auto-adds/strips expression-body braces). §10 (`await`/`async` spacing)
  needs no code either — the space is required syntax, and no pass in this
  codebase normalizes multi-space-after-keyword. **Ordering fix:** the
  parens pass must run in Phase 1 immediately before the Phase-1
  `enforceComplexityPadding` re-run, not in Phase 4 — otherwise a fresh
  format and a reformat of already-parenthesized output disagreed on
  tight-vs-loose call padding (idempotency bug, fixed).
- **§7 Optional chaining / nullish coalescing** — DONE
  (`enforceOptionalChainingSpacing`: `?.` tight both sides, `??`/`??=`
  spaced both sides).
- **§8 Getter/setter accessors** — DONE, reuses `GetterSetterRuleCurly`
  unmodified (no per-language branch needed; `get`/`set` parse as a
  stand-in return-type column same as C++/Java). Fixed: the nested
  callGrid's params-column separator space leaked into typed-param-less
  JS/TS setters (`set x( value)`) — fixed via a `noTypeColumn` flag.
  Confirmed *not* a bug: empty-parens padding-to-match-sibling-width is
  Java/C++'s own established convention, not JS/TS-specific — left as is.
  **Known gap:** a plain method with no return-type token (e.g.
  `isValid() {...}`) cannot join the same aligned group as `get`/`set`
  siblings — `mergeReturnTypeIntoCall`'s "any member with empty return
  type merges" logic conflicts with the nested callGrid's independent name
  padding, producing column misalignment. Currently such a method is left
  ungrouped (correct, just not aligned) rather than corrupted. Needs
  `mergeReturnTypeIntoCall` redesigned for this shape — not attempted.
- **§9 Decorators** — DONE (`enforceDecoratorTightAtSpacing`,
  `enforceDecoratorOverflowCascade` for the drop-to-own-line step; the
  wrap-overlong-argument-list step needs no code, already covered by the
  existing generic call-line-breaking pass). Placement preservation
  (never move inline↔own-line) is the default do-nothing behavior.
- **§10 `async`/`await` spacing** — DONE, free (see §6 above).
- **§11 Type annotations** — Colon spacing (declarator/parameter/return
  type), union/intersection (`|`/`&`) spacing, and class-field/method
  modifier-priority reordering are all DONE as flat passes
  (`enforceTypeColonSpacing`, `enforceUnionIntersectionSpacing`,
  `reorderClassFieldModifiers`). Union/intersection spacing is
  deliberately TS-only rather than "type position only," so it also spaces
  JS-analogous bitwise `|`/`&` in TS (accepted side effect, not a gap).
  Prerequisite tokenizer fix: the Kotlin-only `"?:"` multi-char-op entry
  was matching TS's `name?: type` unconditionally — now skipped unless
  `lang.isKotlin`. Class-field colon flat spacing (as opposed to interface
  member colons, which get full grid alignment under §14) was added in the
  §14 checkpoint via a new `classBraceKind`/`BLOCK:CLASS` tag on
  `classifyTypeColons`. **NOT done:** the declaration-alignment-**grid**
  column integration §11's own text and worked example call for
  (RDD_KEY_183 — `=`-column and `:`-column alignment across a group of
  consecutive declarations/class fields) — every §11 checkpoint was a flat
  spacing/reordering pass only, no `Declaration`/`ColumnGrid` parser
  modeled on `KotlinDeclarationAlignmentRule`/`KotlinSignatureRule` was
  ever written for this.
- **§12 Enums** — DONE (`enforceEnumMemberFormatting`, `isEnumBodyBrace`
  fix to `classifyBraces`): always reflows to one-member-per-line (even
  from an originally-same-line body, per the style doc's explicit text),
  trailing separator always `,` never `;`, `=` column-aligned only among
  members that have an explicit value. This section was never given a
  dedicated checkpoint until late in the job's history despite being
  assumed "free" earlier — now fully implemented, no known gap.
- **§13 Generics (`<T>`)** — assumed free via the reused C++/Java
  generic-bracket-complexity handling; not given its own dedicated
  verification checkpoint (only touched indirectly via §5's
  generics-suppressed-Allman-conversion fix). Not currently flagged as
  broken, but not independently re-verified either.
- **§14 Interface / object-shaped `type`-alias member `:` alignment** —
  DONE (`enforceInterfaceTypeAliasMemberColonAlignment`,
  `parseInterfaceMembers`, `classBraceKind`/`isTypeAliasObjectBrace`):
  name-column padded, `pad(name) + " : " + type + ";"`. Unlike §12, does
  **not** force a same-line member list onto separate lines (left
  untouched if already one-liner — doc-supported choice, real-world bodies
  are overwhelmingly already one-per-line). Whole body is left
  byte-for-byte untouched (bailout, not partial/corrupted alignment) on
  any unrecognized member shape (method-signature member, index signature,
  a member type spanning a NEWLINE). K&R brace style for `interface`/
  `type` headers confirmed already handled by the pre-existing
  general-purpose non-function-brace-style pass — no new code needed;
  §5's Allman pass independently confirmed to never match these headers.
  RDD_KEY_196: an object-shaped `type X = { ... };` alias is a named
  construct like `interface`/`class`/`enum` per STYLE.md §7's universal
  rule — always gets a closing comment regardless of body length, placed
  after the trailing `;` (`}; // type Point`); non-object aliases with no
  brace body are unaffected. `BlockStructureRule.classifyBrace` gained a
  `lang.isTs && isOp(prev, "=")` branch (`typeAliasNameBeforeEquals`) that
  classifies the brace as `Frame.named(braceIdx, "type " + name)`, reusing
  the existing generic NAMED-construct closing-comment/semicolon-skipping
  machinery (`commentInsertionIndex` already walked past a trailing `;`).
  Verified: `type Point = {...}` (multi-line, >0 lines) now closes
  `}; // type Point`; a plain `const obj = {...}` initializer is
  unaffected (the depth-aware backward walk requires the enclosing
  statement to start with the `type` keyword).
  This section (along with §12) was previously and incorrectly marked
  "fully complete" under an earlier checkpoint before actually being
  built — now genuinely complete, no known remaining gap.
- **§15 Import ordering** — DONE (`enforceImportOrdering`, new config keys
  `js-import-order`/`-sort`/`-blank-lines`, no `-depth` key — JS/TS has no
  package-derived local-prefix concept). Classification (RDD_KEY_195):
  `node:`-prefixed or in the 41-entry `NODE_BUILTIN_MODULES` list →
  `builtin`; `./`/`../`-prefixed → `local`; everything else → `third-party`
  (including bundler/tsconfig `baseUrl`/`paths` absolute imports — accepted
  misclassification). Re-emits each import's original token span verbatim
  rather than canonically regenerating it. Dynamic `import(...)` and
  `import.meta` are detected and left completely untouched. `export ...
  from` re-exports are out of scope entirely. Prerequisite fix: `import`/
  `import type {` named-list braces were falling into §2's
  statement-body default and getting a bogus `;` inserted — fixed via
  `isImportBraceHeader` in `classifyBraces`.
- **Declaration-alignment grid (`let`/`const`/`var`)** — DONE for plain,
  single-declarator, non-destructured statements
  (`JsTsDeclarationAlignmentRule`, mirrors `KotlinDeclarationAlignmentRule`'s
  shape; reuses base `splitStatements` since JS/TS is always
  `;`-terminated). Object-literal-initializer colon spacing
  (tight-before-first-key-colon, distinguished from ternary `:`) also lives
  here (`computeJsObjectPropertyColons`). **NOT done, left verbatim (not
  corrupted, just unaligned):** destructuring-pattern LHS joining the grid
  (RDD_KEY_182), multi-declarator statements (`let a = 1, b = 2;`), and
  `type X = ...` alias groups (RDD_KEY_183) — each `parseDeclaration`
  returns `null` for these shapes today, same "never guess past an
  unrecognized shape" posture used throughout this file.
- **Local `.js`/`.ts` fixture pairs** — authored
  (`js_combined`, `js_comments`, `ts_combined`, `ts_comments`) but **still
  not activated/uncommented in the Makefile** — blocked on the Still Open
  bugs below plus the real-code testing pass (not started).
- **Real-code testing pass** — NOT started, against
  `STYLE_JS_TS.md`'s listed repos (`nodejs/node`, `expressjs/express`,
  `lodash/lodash`, `microsoft/TypeScript`, `angular/angular`, `nestjs/nest`,
  `vuejs/core`).

### Still Open (found during Checkpoint 21's fixture-verification pass)

1. ~~Generator method `*` gets a wrongly-inserted space~~ — **RESOLVED**
   (this checkpoint). See §5 above.
2. ~~Closing comments missing on JS/TS method bodies exceeding the
   threshold~~ — **RESOLVED** (this checkpoint) for function/method
   bodies, see §1 above. The "surrounding `class Widget {...}` itself"
   half of the original report was a **false positive**: named
   constructs always get a closing comment/blank-line pair regardless of
   length (STYLE.md §7) — confirmed already correct and covered by every
   passing C/C++/Java/Kotlin fixture; the local `.js` fixtures' `_out`
   files were simply hand-authored without that blank line and are now
   being corrected by a separate concurrent session.
3. **Spurious blank line after a class's opening `{`** — investigated
   this checkpoint and found to be a **false positive**, not a bug:
   STYLE.md §7 mandates a guaranteed blank line after `{`/before `}` for
   every named construct regardless of content length, and this is
   already exactly what every passing C++ fixture does (e.g.
   `test/real_code_regressions_1_out.cpp`'s `class Rotator {`). The local
   `.js` fixtures' `_out` files simply never had this blank line
   hand-authored in; no source fix needed, only fixture regeneration
   (stretch-goal territory, not a "still open" bug).
4. ~~Comma spacing missing inside destructuring patterns and arrow
   parameter lists~~ — **RESOLVED** (this checkpoint) for object-pattern
   `{ id, name }`. Arrow-parameter commas and array-pattern commas
   (`[first, second]`) were already correct for free (declaration-grid/
   bracket-generic passes) — only the object-destructuring `{}` case
   needed the fix. See §3 above.
5. ~~`GetterSetterRuleCurly` static accessor group not padding while a
   sibling plain accessor group in the same class does~~ — the doubled-`;`
   half **RESOLVED** this checkpoint. Root cause: `TokenizerCurly.
   isPreprocessorLanguage()` unconditionally returned `true` for every
   curly-family language, including JS/TS — so a line-leading `#` (JS/TS's
   real private-field/method sigil, e.g. `#cache = new Map()`) was lexed as
   a C-preprocessor directive, swallowing the *entire* statement (up to
   the next newline) into one opaque PREPROCESSOR token instead of real
   `#`/IDENTIFIER/... tokens. `JsTsSpecificRule.enforceSemicolonInsertion`
   then unconditionally appended `;` to that opaque token's text: harmless
   on a fresh format (no existing `;` to double up), but on a *second*
   format pass the opaque token's swallowed text already included the `;`
   from round1's own output, so a second `;` got appended on top —
   `#cache = new Map();` -> `#cache = new Map();;`, an idempotency bug, not
   visible on a single forward pass (which is why it wasn't caught by
   `make test`'s forward-only diffing until the idempotency check was run
   by hand). Fix: `isPreprocessorLanguage()` now returns
   `!(lang.isJs || lang.isTs)` — JS/TS never treats a line-leading `#` as a
   preprocessor sigil; C/C++/Java's PCPP-directive-passthrough behavior is
   unchanged. Verified via the standalone harness: `#cache = new Map();`
   round-trips byte-for-byte stable now (was `;;` on round2 before the
   fix). `make test`: 106/106 forward + 106/106 idempotency, zero
   regressions.
   **The static-vs-plain accessor-group padding-inconsistency half is
   NOT resolved as a side effect** — confirmed independent: with the `;;`
   bug fixed, `static get instanceCount()`/`static set instanceCount(value)`
   still render without the empty-parens padding that a plain
   `get x()`/`set x(value)` sibling group gets, on a fresh single-pass
   format of `test/js_combined_inp.js` (unchanged from before this fix).
   This is the pre-existing, already-documented `GetterSetterRuleCurly`
   empty-parens-padding-to-match-sibling-width convention question — left
   open, no further fix attempted this checkpoint (out of scope per the
   task instructions once confirmed independent).
6. **Doubled trailing space before a one-liner getter body's closing
   `}`** — investigated this checkpoint and found to be a **false
   positive**: `GetterSetterRuleCurly`'s one-liner accessor-group
   rendering pads each member's body to the group's widest body so every
   `}` aligns at the same column — confirmed by example against
   `test/cpp_comments_out.cpp`'s passing `getValue`/`setValue`/`isValid`
   group (`{ return v_;     }` etc., same mechanism, byte-for-byte). A
   `get`/`set` pair whose bodies differ in length by one character
   legitimately renders with 2 trailing spaces before the shorter body's
   `}` (1 alignment-padding space + 1 join space) — not a bug.
7. ~~TS interface/type-alias/class-field member `:` colons never spaced~~ —
   **RESOLVED** by Checkpoint 23 (§14 above and the class-field colon fix
   folded into §11).
8. ~~Enum member formatting substantially broken~~ — **RESOLVED** by
   Checkpoint 22 (§12 above).
9. **Import ordering/blank-line placement diverges from `_out` in
   `js_comments_inp.js`** — root-caused this checkpoint: `js_comments_inp.js`
   has a trailing end-of-line comment on its `lodash` import
   (`// utility for rate limiting`), and `enforceImportOrdering`
   deliberately bails its *entire* pass (no reordering, no blank-line
   normalization between groups) whenever a comment sits on or between
   two import declarations — a documented, deliberate "never risk
   silently dropping/misplacing a comment" design posture, not an
   oversight. The `_out` fixture's expectation (a blank line inserted
   between the `fs`/`lodash` groups despite that comment) contradicts
   this deliberate design decision. This is a genuine open design
   question, not a small bug: either the fixture's expectation is stale
   and should be regenerated to match the conservative bail-all
   behavior, or the bail-all posture should be relaxed to still allow
   blank-line normalization (but not reordering) around a commented
   import. Flagging per the ambiguity protocol rather than guessing which
   the user wants.

Bugs 1, 2, and 4 are resolved (source fixes, earlier checkpoint). Bug 5's
`#`-private-field doubled-`;` half is now also resolved (this checkpoint,
`TokenizerCurly.isPreprocessorLanguage`); its static-vs-plain accessor-
padding half is confirmed independent and stays open (no fix attempted).
Bugs 3 and 6 are false positives (no source change needed — the fixtures
are stale). Bug 9 is root-caused but is a genuine open design question,
not a bug fix. Bugs 7 and 8 were resolved in earlier checkpoints (23 and
22). The four local fixture pairs stay unactivated until bug 9's design
question is resolved, and the `_out` fixtures are regenerated for bugs
2/3's false-positive findings and bug 5's fix — then the fixture-
verification pass should be redone.
