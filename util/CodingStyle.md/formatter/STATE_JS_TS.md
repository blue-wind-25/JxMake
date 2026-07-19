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

### Open Design Questions

Documented explicitly here per the style doc's own flagged gaps — not
attempted this session, future work:

- **HTML5 needs its own dispatcher.** A `.html` file can embed inline
  `<script>`/`<style>` blocks that need to be spliced out and handed to
  different sub-formatters (JS/TS for `<script>`, CSS for `<style>`, per
  `STYLE_DATA_FORMATS.md` §4.2). This dispatcher — the splice-out/format/
  splice-back mechanism and its re-indentation handling — is unsolved this
  session and belongs conceptually at the intersection of this job and the
  Data Formats job (tracked separately in `STATE_DATA_FORMATS.md`).
- **JSX/TSX will need their own future dispatcher for the same embedding
  problem**, but with different syntax than HTML5's `<script>`/`<style>`
  case — JSX embeds XML/HTML-like tag syntax directly inside JS/TS
  expression position, a compound-language situation, not a same-file
  JS/TS extension. `STYLE_JS_TS.md` explicitly puts JSX/TSX **out of
  scope entirely** (not merely deferred). Unsolved this session, not
  attempted.
- **Import-path built-in/third-party/local classification (§15)** — the
  resolution logic for classifying an import path into one of the three
  default groups is not yet designed, per the style doc's own "Known Open
  Items" section. **Re-confirmed blocking, checked in detail this session
  (not force-implemented):** before starting real §15 work, checked whether
  a purely mechanical three-way split (relative/absolute path prefix =
  local; `node:`-prefixed or a known Node built-in name = built-in;
  everything else = third-party) could sidestep the "not yet designed" flag,
  since two of those three legs (built-in list/`node:` prefix, and
  `./`/`../` relative-path local) are unambiguous ecosystem conventions with
  no judgment call involved. **Re-reading `STYLE_JS_TS.md` §15's actual text
  directly (not from memory) shows this is not just silence — the doc
  itself states the open status explicitly, and folds a second, undefined
  criterion into "local":**
  > "**Local import detection:** an import path is "local" if it's relative
  > (starts with `./` or `../`) or resolves within the project's configured
  > source root; everything else resolvable from `node_modules` is
  > third-party; anything matching Node's built-in module list (or prefixed
  > `node:`) is built-in. **Not yet in the real config schema — the
  > resolution logic for this classification is still an open item (§15).**"
  >
  > (Known Open Items, restated:) "Import-path built-in/third-party/local
  > classification's resolution logic (§15) — not yet designed."
  Two blockers, not one: (1) the doc's own text says outright, twice, that
  the resolution logic is still open — this isn't an inference gap the
  mechanical split can quietly fill, it's the doc declaring itself
  unresolved; (2) "local" is defined as relative-path **or** "resolves
  within the project's configured source root" — that second disjunct
  requires a project-source-root concept (e.g. a `tsconfig.json`
  `baseUrl`/`paths`-style resolution, or a new config key naming the root)
  that doesn't exist anywhere in this formatter's config schema today, so
  even the "local" leg alone isn't fully mechanical — a bare specifier like
  `import { Widget } from "components/Widget"` (no `./` prefix) could be
  either a first-party absolute-from-source-root import or a genuine
  third-party package, and nothing in the doc or the current config schema
  says how to tell them apart without inventing a new, undesigned config
  key. Per `STATE_COMMON.md`'s ambiguity protocol, this is exactly the kind
  of open question to stop for rather than force through with an invented
  default. **Not implemented this session; no code changes made to
  `JsTsSpecificRule.java`, `Config.java`, or `README.md` for §15.**

---

## Checklist

- [x] Diff `STYLE_JS_TS.md` against `STYLE_JAVA.md`/`STYLE_KOTLIN.md`
      section-by-section to confirm which "Baseline — Directly Inherited"
      (§1) items are already free once shared-class wiring recognizes a JS/
      TS language tag, vs. which need a small additive extension (mirrors
      the Kotlin job's Step 1 scoping-table approach). **Findings:** all 7
      §1 items (bracket/paren complexity padding, keyword spacing, `{}`
      spacing, closing comments on blocks, blank line before `return`,
      `else`/`else if` placement, `switch` formatting) cite plain
      `STYLE.md` base sections, not a Java- or Kotlin-specific override —
      they come free once `lang.isJs`/`isTs` is recognized alongside
      `lang.isKotlin` in the curly-family classes, **provided** existing
      Java/Kotlin-only override branches stay narrowly gated and don't leak
      onto JS/TS: (1) Java's array-decl (`int[] x`)/double-brace-init
      tight-exception (§4) has no JS/TS analog, must stay Java-only; (2)
      Kotlin's unconditional-blank-line-always closing-comment override
      (§3.1/§3.4) must NOT apply to JS/TS — JS/TS uses the plain
      threshold-gated `STYLE.md` §7 behavior instead; (3) Java's
      never-add-closing-comment-to-`else` suppression (§8) should be
      verified to key off the general §7/§12 interaction (language-agnostic)
      rather than a Java-only flag, so it also covers JS/TS's `else`. §2-15
      classification (JS+TS vs TS-only, reuse-vs-new-logic) verified during
      this pass; folded directly into the per-section notes already present
      above in this file's Scope section — no separate table needed, they
      matched what's already documented there (§7 reuses Kotlin `?.`/`?:`,
      §8 reuses STYLE.md §14 group-alignment, §11 reuses Kotlin `: type`
      tail spacing + declaration-alignment grid, §13 reuses Java/C++ generic
      bracket-complexity, §14 reuses Kotlin/Java K&R container brace style +
      member `:` alignment; §2/§3/§4/§9/§10/§12/§15's group-contents need
      new JS/TS-specific logic as already scoped).
- [x] Tokenizer support pass: survey `STYLE_JS_TS.md` for every token not
      already lexed (template literals, `?.`/`??`, `=>`, decorators `@`,
      spread/rest `...`, TS type-annotation `:`/`|`/`&`/generics `<>`), done
      once for both languages rather than revisited later for TS's
      additions. `TokenizerCore.java` is shared with C/C++/Java/Kotlin —
      every addition here must be purely additive and must not change how
      any existing token in those languages is lexed. Re-run the full
      existing regression suite and confirm zero regressions before any
      rule-level work begins.
      **Done, in `TokenizerCurly.java` only** (`TokenizerCore.java` itself
      untouched): added `KEYWORDS_JS`/`KEYWORDS_TS` (TS = JS ∪ its own
      vocabulary) and `NAMED_CONSTRUCT_JS`/`NAMED_CONSTRUCT_TS` keyword sets,
      wired via new `case "js"`/`case "ts"` arms in the constructor switch;
      added `"=>"`, `"??="`, `"??"` to `MULTI_CHAR_OPS` (`?.`/`...`/`!` were
      already present, shared with Kotlin/C-family — no new entry needed for
      those); added a new `emitTemplateLiteral()` for backtick literals,
      dispatch-gated on `c == '\`' && (lang.isJs || lang.isTs)` so it's a
      no-op for every other language. Decorators (`@`) and TS `:`/`|`/`&`/
      generics `<>` needed **no new lexing** — they already fall through to
      the existing single-char `emitOperator()` fallback / existing
      `<`/`>` angle-bracket reclassification, which is language-agnostic.
      Template-literal scope note: the whole literal (incl. any `${...}`
      interpolations) is emitted as **one opaque STRING token**, content
      preserved byte-for-byte — satisfies §4's "preserved exactly as
      written" half now; §4's other half ("`${...}` gets normal expression
      spacing") needs interpolation-interior re-tokenizing and is deferred
      to §4's own future rule-implementation checkpoint, not attempted here.
      `make` compiles clean; `make test`: 106/106 forward + 106/106
      idempotency, zero regressions (existing C/C++/Java/Kotlin fixture
      corpus unaffected — confirms the additions are purely additive).
      `JsTsSpecificRule` still throws `UnsupportedOperationException`
      unconditionally (rule layer untouched this checkpoint), so no JS/TS
      fixture could yet be un-commented in the Makefile even though the
      tokenizer can now lex the input without erroring.
- [ ] When implementing §11 below (declaration/parameter alignment), start
      from `KotlinDeclarationAlignmentRule.java`/`KotlinSignatureRule.java`
      as a structural template, not from scratch. TS's `let x: Type =
      value` is the same name-before-type reversed grammar Kotlin's `val x:
      Type = value` already solved (RDD_KEY_103/104) — the shared
      `DeclarationAlignmentRuleCurly`'s `[modifiers] Type name` assumption
      doesn't fit either language, and Kotlin's `Declaration`-record/grid-
      rendering/group-break shape carries over directly. Copying the
      skeleton saves the design work, not the content: TS's actual type
      grammar (union/intersection `|`/`&`, generic constraints,
      function-type annotations, its own `declare`/`readonly`/`abstract`/
      `override` modifier-priority table, per §11's own note that it
      "cannot reuse Java's table wholesale") still has to be written and
      verified against TS-specific fixtures — the template only shortcuts
      the "how do you even structure this" question, not the TS-specific
      grammar or its own dogfooding surprises.
- [~] Implement §2–15 rule-by-rule, each its own checkpoint commit, per
      `STATE_COMMON.md`'s workflow. This is ordered by section, not by
      language: §2–10 apply to both JS and TS (verified entirely against
      `.js` fixtures as each lands), §11–14 are TS-only (verified against
      `.ts` fixtures once reached), §15 closes out both. JS support is
      therefore functionally complete once §2–10 land, without JS ever
      being a separate finished phase — TS support accretes the same way
      through §11–14, reusing every §2–10 commit rather than repeating
      them. Don't split this into a "JS pass" followed by a "TS pass"; the
      shared `JsTsSpecificRule.java` has no seam that would make that split
      meaningful.
      **Checkpoint 3 done — §2 (statement-termination semicolon insertion)
      only.** `JsTsSpecificRule.enforceSemicolonInsertion` is no longer a
      boilerplate stub (constructor no longer throws): a depth-stack-based
      statement-boundary scan, wired into `FormatterCurly.formatOne` gated
      on `lang.isJs || lang.isTs`. Depth increments across `(`/`[` and any
      `{` classified as a *value* brace (object literal/array pattern/
      arrow-function-assigned-value etc., via `classifyBraces`'s
      preceding-token heuristic) but resets to a fresh 0 inside a
      *statement-body* brace (function/method/class/interface/control-flow
      body, and — as its own special case — an arrow-function block body,
      which is a value on its own closing `}` but a statement list inside),
      so each statement is evaluated for its own semicolon at depth 0
      relative to its immediately enclosing block, not the file's absolute
      nesting. A statement boundary is a depth-0 NEWLINE or a depth-0
      statement-body brace about to close (covers one-liner bodies with no
      newline, e.g. `{ return x }`). Decorator applications (`@Name(...)`/
      `@Name` alone on their own line) are detected and excluded from
      semicolon insertion via `endsWithDecoratorApplication`. Control-flow/
      function/class headers are excluded via a "next significant token
      after the newline is `{`" lookahead. **Ordering finding:** the call
      had to be placed *before* `BlockStructureRule.collapseSingleExpression
      Blocks` in `FormatterCurly`'s Phase 1 (not after, alongside Kotlin's
      analogous semicolon-related calls, as originally placed) — placing it
      after caused a genuine idempotency bug (`if (x) { doThing() }` stayed
      braced on a fresh format but collapsed to `if(x) doThing();` on a
      second pass, because `collapseSingleExpressionBlocks`'s braceless-
      eligibility check saw a different token shape — no `;` yet vs. `;`
      already present from round1's own output — for the same logical
      statement across the two rounds). Found and fixed via a standalone
      harness (`FormatterCore.forLanguage("js")` invoked directly, bypassing
      `Main.java`'s `Lang.isScaffoldOnly` CLI gate, which still throws for
      `js`/`ts` and is intentionally NOT flipped yet — per the established
      JSON/CSS/YAML/TOML/XML/HTML5 precedent (RDD_KEY_190 and its
      successors), a language only leaves `SCAFFOLD_ONLY_LANGUAGES` once its
      job is functionally complete for real files, not after each
      individual section; for JS that milestone is "§2–10 all land," not
      "§2 alone"). Verified manually via the harness (not yet via
      `make test`, since no local `.js`/`.ts` fixture can be un-commented in
      the Makefile until enough sections land for `js_combined`/
      `js_comments` to pass whole — those fixtures exercise §2 together
      with §3–10/§15 in the same file): semicolons correctly inserted after
      plain statements, declarations, `return`/`yield` expressions,
      decorator-modified class fields, object-literal-assigned/arrow-
      function-assigned closing `}`, template-literal-assigned statements,
      optional-chaining/nullish-coalescing expressions, import/export
      statements; correctly withheld after function/class/control-flow
      Allman headers, own-line decorator applications, object-literal
      interior lines ending in a bare value (no trailing comma), and
      one-liner method/accessor bodies' own closing `}`. Round-trip
      (harness round1 → round2) confirmed idempotent on every manual
      fixture tried. `make` compiles clean; `make test`: 106/106 forward +
      106/106 idempotency, zero regressions in the existing C/C++/Java/
      Kotlin corpus. **Not yet done:** §3–15 (all remaining rows below),
      and no local `.js`/`.ts` fixture is registered/verified via
      `make test` yet (blocked on more sections landing, per above) — next
      session should continue with §3 (destructuring/spread spacing) next,
      per the suggested grouping in the original task brief (§3 pairs
      naturally with §2's mechanical-rule size).
      **Checkpoint 4 done — §3 (spread/rest tight spacing) + §7 (optional
      chaining/nullish coalescing spacing) only.** Two new flat single-pass
      scans in `JsTsSpecificRule.java`, `enforceSpreadRestSpacing`
      (one-sided: no space after `...`) and `enforceOptionalChainingSpacing`
      (`?.` tight both sides, `??`/`??=` spaced both sides), both directly
      modeled on `KotlinSpecificRule.enforceNullSafetyOperatorSpacing`'s
      flat-pass shape (same conservative gap-blocked-by-comment/NEWLINE/
      frozen bailout). Wired into `FormatterCurly` Phase 4, gated on
      `lang.isJs || lang.isTs`, right after Kotlin's analogous operator-
      spacing block. §3's other two named items need no new code: bracket
      padding on a destructuring pattern is already free via STYLE.md
      §3.1's existing complexity-padding pass (language-agnostic), and
      RDD_KEY_182 (declaration-alignment-grid membership) was a design
      decision already recorded, not new code owed this checkpoint. §10
      (`await`/`async` spacing) was surveyed and found already free: valid
      JS/TS source can never omit the space after these keywords (it's
      required syntax, not optional whitespace), and this codebase has no
      precedent anywhere of a pass that collapses/normalizes multiple
      spaces after an arbitrary keyword to exactly one (confirmed by
      grepping `MiscRuleCore`/`enforceKeywordSpacing`, which only
      tight-collapses specific `TIGHT_PAREN_KEYWORDS`-before-`(` cases) --
      so §10 needs no dedicated rule and was folded into this checkpoint's
      "already free" findings rather than getting its own commit.
      **Real bug found while smoke-testing §3 against destructuring
      declarations (`const [first, second] = items`), NOT caused by this
      checkpoint's own changes (confirmed via `git stash` against the prior
      commit -- same corruption reproduces there) -- documented, not fixed,
      here, because the actual fix is §11's scope, not §3's:**
      `DeclarationAlignmentRuleCurly`'s constructor
      (`rules/DeclarationAlignmentRuleCurly.java`, ~line 48) has no
      `lang.isJs`/`isTs` branch at all -- it falls into the same `else`
      arm as C/C++ (`CppModifierPriority` + `TYPE_KEYWORDS_C`/`_CPP`),
      because that constructor's condition is only `lang.isJava ? Java : Cpp`.
      `CppModifierPriority` happens to recognize `const` as a modifier
      (priority 4), so `const [first, second] = items;` gets parsed as a
      modifier-prefixed declaration whose "name" token is the structured-
      binding-style `[` -- but the actual C++ structured-binding render
      path (`isStructuredBinding`, ~line 291-303, including the extra-space
      insertion before the name column) is gated `if (lang.isCpp)`, false
      for JS, so the space-insertion never fires and the bracket renders
      tight against `const` (`const[first, second] = items;`, confirmed via
      the standalone harness, idempotent but wrong). Plain-identifier
      declarations (`const x = 5;`, `const add = (a, b) => a + b;`) and
      every spread/rest use *outside* declaration-LHS position (object/
      array literal spread, rest parameters, call-site spread) render
      correctly -- the corruption is narrowly scoped to destructuring-
      pattern-as-declaration-LHS specifically, confirmed via the same
      harness. Root cause and fix both belong to §11's already-planned
      dedicated checkpoint (wiring real JS/TS awareness into the shared
      declaration-alignment machinery, using `KotlinDeclarationAlignmentRule`/
      `KotlinSignatureRule` as a structural template per this file's own
      existing checklist note) -- not attempted here, since fixing it
      properly requires exactly the "how does JS/TS's untyped, no-modifier-
      table declaration grammar plug into this class" design work §11 is
      already scoped to do, not a narrow §3-sized patch. **This blocks**:
      registering any local `.js`/`.ts` fixture whose exercised surface
      includes a destructuring declaration until §11 lands (`js_combined`/
      `ts_combined` likely both need this per their real-code-derived
      content -- not yet checked line-by-line, next session verifying §3
      fixture-readiness should check first). Plain `const`/`let`/`var`
      declarations without destructuring are unaffected and safe to use in
      fixtures today.
      Verified via a standalone harness (`FormatterCore.forLanguage("js")`,
      same approach as Checkpoint 3, `Main.java`'s CLI gate still
      untouched): `...` correctly tightened after deliberately-wrong
      spacing in spread/rest/call-site positions; `?.`/`??`/`??=` correctly
      normalized from deliberately-wrong spacing (extra/missing spaces
      around all three). Round-trip (harness round1 -> round2) confirmed
      idempotent on every manual fixture tried. `make` compiles clean;
      `make test`: 106/106 forward + 106/106 idempotency, zero regressions.
      **Next up:** §4 (template literal `${...}` interior spacing) and/or
      §6 (arrow function brace style) per the suggested grouping, or §11
      (declaration alignment) sooner than originally planned if it starts
      blocking further fixture verification.
      **Checkpoint 5 done — §11 destructuring-declaration space bug fix
      only (root cause was NOT where Checkpoint 4 guessed).** Investigated
      via a standalone harness before touching any code: the Checkpoint 4
      note theorized the bug lived in `DeclarationAlignmentRuleCurly`'s
      constructor (no `isJs`/`isTs` branch, falls into the C/C++
      `CppModifierPriority` arm) plus its `isCpp`-gated structured-binding
      space fixup. Actual root cause, confirmed by tracing
      `parseDeclaration`: for `const [first, second] = items;`, `const` is
      consumed as a modifier, then the generic declarator path strips the
      entire `[first, second]` bracket run into `sizeTokens` (its
      trailing-`]`-stripping loop has no notion of "this bracket IS the
      whole declarator, not a suffix"), leaving `sizeEnd <= i` --
      `parseDeclaration` returns null, so `DeclarationAlignmentRuleCurly`
      never touches this statement at all (confirmed by inspection, not
      guessed: `isStructuredBinding`'s `isCpp`-only gate is real but dead
      code for this bug specifically, since `parseStructuredBinding` itself
      is also `isCpp`-gated at its call site and never reached for JS/TS
      either way). The actual space loss happens in the *shared, generic*
      `MiscRuleCore.isTightToken`/`needsSpaceBetween` token-joining pair
      (used by many rendering paths across every curly language, not
      declaration-specific): `isTightToken` unconditionally treats `[` as
      tight against whatever precedes it, correct for C/C++/Java array-
      declarator/subscript shapes (`int arr[5]`, `a[i]`, always preceded by
      an identifier/closing-bracket) but wrong for JS/TS's destructuring-
      declaration LHS, where `[`/`{` directly follows a `const`/`let`/`var`
      KEYWORD token. Fixed with a narrow, identity-scoped early-return in
      `MiscRuleCore.needsSpaceBetween` (mirrors the existing Kotlin
      `fun <T>` exception immediately above it in the same method): `if
      ((lang.isJs || lang.isTs) && isPunct(cur, "[") && prev.type ==
      TokenType.KEYWORD) return true;` -- forces a space only when `[`
      immediately follows a keyword, in JS/TS only, leaving every other
      language and every other `[` context (array literals, subscripts,
      generic-array-type contexts) completely untouched. **Not touched at
      all, contrary to the original plan:** `DeclarationAlignmentRuleCurly`'s
      constructor/`isStructuredBinding` gate -- Checkpoint 4's theory about
      where the bug lived was wrong; no `lang.isJs`/`isTs` branch was added
      there this checkpoint, since the actual fix required none. Verified
      via a standalone harness (`FormatterCore.forLanguage("ts")`):
      `const [first, second] = items;`, `let [a, b, c] = arr;`,
      `var [x] = single;` all now preserve the space and round-trip
      idempotent; unaffected/regression-checked in the same harness run:
      plain declarations (`const x = 5;`), arrow-function assignment,
      array-subscript reads (`items[0]`) staying tight, and a function-
      parameter array-subscript inside a body. `make` compiles clean;
      `make test`: 106/106 forward + 106/106 idempotency, zero regressions.
      **New bug found while smoke-testing (NOT fixed this checkpoint, out
      of this checkpoint's narrow scope -- documented here, not chased):**
      `const { a, b } = obj;` (object-destructuring, curly-brace form, as
      opposed to the array-bracket form this checkpoint fixed) renders as
      `const { a, b; } = obj;` -- a bogus `;` inserted before the closing
      `}`. This looks like a §2 (semicolon-insertion, `JsTsSpecificRule
      .enforceSemicolonInsertion`) interaction, not a §11/declaration-
      alignment issue: the object-pattern's `{` is presumably being
      misclassified as a statement-body brace (triggering the depth-reset-
      to-0 semicolon-insertion logic) rather than a value/pattern brace,
      the same classification distinction Checkpoint 3's `classifyBraces`
      heuristic already has to make for object-literal-as-value vs.
      function-body braces. Root cause not investigated further this
      checkpoint (scope was the documented array-destructuring bug only);
      flagged in Open Questions below for whichever future checkpoint picks
      up either §2's `classifyBraces` refinement or §3/§11's destructuring
      coverage next.
      **Checkpoint 6 done -- object-destructuring bogus-semicolon bug fixed
      (§2 `classifyBraces`, confirmed root cause as suspected).** For
      `const { a, b } = obj;`, `classifyBraces`'s `isValue` check on the
      object-pattern's `{` only recognized `=`/`(`/`[`/`,`/`:`/`??`/`||`/
      `&&`/`?`/`...`/`=>`/`return`/`yield`/`throw`/`typeof` as
      value-indicating predecessors -- a bare `const`/`let`/`var` KEYWORD
      immediately preceding `{` (destructuring-pattern-as-declaration-LHS)
      fell through to the default "not a value" branch, so `resetDepth` was
      wrongly `true`: the pattern's interior was treated as a statement list
      needing its own semicolon, and the depth-0-`}`-about-to-close boundary
      check inserted a bogus `;` after the last property name (`b`). Fixed
      by adding `const`/`let`/`var` to the same KEYWORD-predecessor
      disjunct already used for `return`/`yield`/`throw`/`typeof` in
      `classifyBraces` (`JsTsSpecificRule.java`) -- mirrors Checkpoint 5's
      analogous fix for the array-bracket form, but in the brace-
      classification axis instead of `MiscRuleCore.needsSpaceBetween`'s
      token-spacing axis. Nested patterns (`const { a: { b, c } } = obj;`)
      needed no additional change -- the inner `{` already follows `:`,
      already in the pre-existing `isValue` disjunct list. Verified via a
      standalone harness (`FormatterCore.forLanguage("ts")`):
      `const { a, b } = obj;`, `const { a: { b, c } } = obj;`,
      `const { a = 1 } = obj;` (default value inside pattern) all now render
      correctly with no bogus semicolon, and round-trip idempotent.
      Regression-checked in the same harness run: plain statement
      termination (`if (x) { doThing() }` -> braceless one-liner, still
      correct), arrow-function assignment, object-literal assignment,
      class/method Allman bodies with closing comments, and array-
      destructuring (`var [p, q] = list;`, Checkpoint 5's fix) all
      unaffected. `make` compiles clean; `make test`: 106/106 forward +
      106/106 idempotency, zero regressions.
      **Checkpoint 7 done -- §11 main-paragraph `: type` colon spacing only
      (declarator/parameter/return-type), flat spacing pass, NOT the
      declaration-alignment grid.** New `JsTsSpecificRule.enforceTypeColonSpacing`
      (TS-only, `lang.isTs`-gated, no-op passthrough for JS), wired into
      `FormatterCurly` Phase 4 right after the §3/§7 block, its own
      `if (lang.isTs)` guard (not folded into the shared `isJs || isTs`
      block above it, since this section is TS-only). Two-pass design: (1)
      `classifyTypeColons` -- a bracket-stack scan (`PAREN`/`BRACKET`/
      `OBJ`/`BLOCK` frames, `OBJ` vs `BLOCK` for `{` decided by the same
      "is the preceding token value-indicating" heuristic as §2's
      `classifyBraces`, duplicated locally as `isValuePrecededBrace` rather
      than shared/extracted -- lower risk than touching the existing method)
      producing the index set of every `:` classified as a type-annotation
      colon: preceding significant token is `)` (return type, unless that
      `)` closes a `case (...):` parenthesized label -- guarded via
      `isCaseLabelParen`), or preceding significant token is an IDENTIFIER
      (or `?` tight after one, TS's `name?: type` optional-marker shape)
      whose own preceding context is either `(`/`,` while the enclosing
      bracket frame is `PAREN` (parameter colon), or `,`/a `let`/`const`/
      `var` keyword while at statement level (`BLOCK` or no enclosing
      bracket) and an `inDeclarator` flag is set (declaration colon,
      supports multi-declarator `let a: number, b: string;`).
      (2) `enforceTypeColonSpacing` itself -- a flat gap-normalizing scan
      structurally identical to §3/§7's passes, tight before / one space
      after any index in the classified set, everything else left
      untouched. **Real tokenizer bug found and fixed as a prerequisite,
      NOT scoped to JS/TS originally:** `TokenizerCurly`'s `MULTI_CHAR_OPS`
      table has a `"?:"` entry for Kotlin's Elvis operator, matched
      unconditionally in the multi-char-op scan loop regardless of
      language -- so TS's `name?: type` (no space between `?` and `:`,
      the common/idiomatic form) was being lexed as one opaque `?:` token
      instead of two, silently defeating this checkpoint's own
      classification logic (confirmed via harness: `function f(a?:number)`
      round-tripped completely untouched before the fix). Fixed narrowly
      by skipping the `"?:"` entry in the multi-char-op loop when
      `!lang.isKotlin`, leaving Kotlin's Elvis-operator lexing byte-for-byte
      unchanged (confirmed: `make test` 106/106 both before and after,
      Kotlin corpus included) while JS/TS now lexes `?` and `:` as two
      separate tokens as intended. Verified via a standalone harness
      (`FormatterCore.forLanguage("ts")`): `let x:string;`/`let y :
      number;` → `let x: string;`/`let y: number;`; multi-declarator
      `let a:number, b:string;` → both colons fixed; function params +
      return type `function f(a: number, b:string):boolean {}` → all three
      colons fixed in one pass; optional parameter `function f(a?:number)`
      → fixed post-tokenizer-bugfix; arrow-typed declarator
      `const handler: (event: Event) => void = (event) => {...};` → both
      colons (outer declarator, inner arrow-param) correctly handled,
      inner one via the `PAREN`-frame path. Explicitly re-verified
      unaffected (byte-for-byte, per the task's own hard requirement):
      object-literal keys (`{ a: 1, b: 2 }`), destructuring rename
      (`const { a, b: renamed } = obj;`), ternary (`let x = a ? b : c;`),
      unparenthesized `case 1:`/`default:` labels (pre-existing quirky
      spacing -- `case 1 :` -- confirmed unchanged, not this checkpoint's
      concern), and the rare parenthesized `case (a + b):` label (excluded
      via `isCaseLabelParen`). Class-field colons (`private x:number;`)
      deliberately NOT touched -- out of this checkpoint's scope per the
      task brief (item 1 covers `let`/`const`/`var`/params/return types
      only, not class fields; §11.2's class-field modifier work is
      separate and still not started). Round-trip (harness round1 →
      round2) confirmed idempotent on every case above, including the
      pre-existing quirky `case 1 :` spacing (stable, not touched either
      way). `make` compiles clean; `make test`: 106/106 forward + 106/106
      idempotency, zero regressions.
      **Explicitly NOT done, deferred:** the declaration-alignment-grid
      column integration STYLE_JS_TS.md §11's own text calls for (`=`-
      column and `:`-column alignment across a group of consecutive
      declarations, RDD_KEY_183) -- this checkpoint is spacing-only, no
      `Declaration`/`ColumnGrid` parser modeled on
      `KotlinDeclarationAlignmentRule`/`KotlinSignatureRule` was written.
      A future checkpoint that wants true grid alignment will need that
      larger parser; this checkpoint's `classifyTypeColons` bracket-stack
      scan could plausibly seed its colon-position detection but was not
      designed with reuse in mind.
      **Checkpoint 8 done -- §11.1 union/intersection (`|`/`&`) spacing
      only.** New `JsTsSpecificRule.enforceUnionIntersectionSpacing`
      (TS-only, `lang.isTs`-gated), same flat gap-normalizing shape as
      §7/§11's other passes: any single-char `|`/`&` token forces one
      space on both sides. Compound tokens (`||`, `&&`, `|=`, `&=`, etc.)
      are already lexed as their own distinct multi-char ops, never match
      the single-char check, so they're untouched by construction.
      Deliberately scoped to TS-only rather than "type position only" --
      confirmed via harness that no existing pass spaces bitwise `|`/`&`
      in JS expression position either (`let x = a|b;` stays untouched in
      `.js`), so TS's own bitwise usages picking up the same spacing as
      its union/intersection ones is an accepted, documented side effect
      of the simpler scope choice, not a design gap. Wired into
      `FormatterCurly` Phase 4 directly after §11's colon-spacing call,
      inside the same `if (lang.isTs)` block. **Real bug found and fixed
      as part of this checkpoint's own verification (not pre-existing,
      not touched by Checkpoint 7):** break-before-operator union wrapping
      (`type Y = A\n | B\n | C;`, STYLE_JS_TS.md §11.1's own second worked
      example) was getting a bogus `;` inserted after `A` by §2's
      `JsTsSpecificRule.enforceSemicolonInsertion` -- that pass's NEWLINE-
      boundary check only ever looked at the *trailing* token before the
      newline (already handled via `CONTINUATION_OPS`, which is why the
      break-*after*-operator worked example `type X = A |\n B |\n C;`
      already rendered correctly) with no lookahead past the newline for
      a *leading* continuation operator on the next line. Fixed with a
      narrow, `|`/`&`-scoped addition to `maybeInsertSemicolon`'s existing
      NEWLINE-boundary lookahead (right next to the pre-existing "next
      token is `{`" Allman-header check): if the next significant token
      across a NEWLINE boundary is `|` or `&`, treat it as the same
      statement continuing and skip semicolon insertion. Scoped narrowly
      to just `|`/`&` (not every `CONTINUATION_OPS` entry) to minimize
      risk -- this task's own scope is union/intersection wrapping, not a
      general break-before-any-operator fix for arbitrary JS/TS
      expressions, which would need its own broader verification pass.
      Verified via a standalone harness (`FormatterCore.forLanguage("ts")`):
      `type Status = "active"|"inactive"|"pending";` →
      `"active" | "inactive" | "pending"`; `type Combined = Base&Extra;` →
      `Base & Extra`; mixed `type Z = A | B&C;` → `A | B & C` (confirmed
      not attempting precedence-aware reflow, just spacing, per the task's
      own explicit check); both break-before and break-after wrapped forms
      preserved exactly as written (including the semicolon-insertion fix
      above) with correct spacing and no reflow. Explicitly re-verified
      unaffected: JS bitwise `|`/`&` (`let x = a|b;` in a `.js` file stays
      untouched, confirming the TS-only gate). Round-trip (harness round1
      → round2) confirmed idempotent on every case above. `make` compiles
      clean; `make test`: 106/106 forward + 106/106 idempotency, zero
      regressions.
      **Checkpoint 9 done -- §11.2 class-field/method modifier-priority
      table reordering only.** New `JsTsSpecificRule.reorderClassFieldModifiers`
      (TS-only, `lang.isTs`-gated): scans for a maximal same-line run of
      2+ consecutive modifier keywords (`declare`, `public`/`private`/
      `protected`, `static`, `abstract`, `override`, `readonly` -- all
      already lexed as `TokenType.KEYWORD` via `KEYWORDS_TS`) and re-emits
      them in the fixed canonical order from §11.2's worked example
      (`declare` → visibility → `static` → `abstract` → `override` →
      `readonly`, confirmed by re-reading STYLE_JS_TS.md §11.2 directly
      this checkpoint, matching what Checkpoint 6 had already recorded).
      A run of exactly one modifier keyword is left completely untouched
      (nothing to reorder -- avoids needlessly re-deriving unchanged
      text). Deliberately scoped broader than "class field" literally --
      applies to any 2+-modifier run wherever found (also covers method
      modifiers, e.g. `private static foo()`), since these keywords never
      co-occur consecutively in valid TS/JS outside a class-member
      modifier list, so no narrower "must be directly inside a class body"
      gate was needed. Conservative bailout matching this file's other
      passes: a run whose internal gaps contain a NEWLINE, a comment, or a
      frozen token is left untouched rather than reordered. Wired into
      `FormatterCurly` Phase 4, directly after §11.1's union/intersection
      call, inside the same `if (lang.isTs)` block. Verified via a
      standalone harness (`FormatterCore.forLanguage("ts")`): the style
      doc's own worked examples (`declare public static readonly
      MAX_COUNT`, `protected override readonly cache`, `private static
      instance`) all round-trip unchanged (already-canonical order);
      scrambled orderings (`readonly static private x` →
      `private static readonly x`; a maximal 6-of-6-modifier scramble
      `override abstract static private declare readonly x` →
      `declare private static abstract override readonly x`) both
      normalize correctly; single-modifier (`private x: number;`) and
      zero-modifier (`x: number;`) fields both stay byte-for-byte
      untouched. Round-trip (harness round1 → round2) confirmed idempotent
      on every case above. `make` compiles clean; `make test`: 106/106
      forward + 106/106 idempotency, zero regressions.
      **§11 fully done as scoped by this multi-checkpoint task** (colon
      spacing, union/intersection spacing, modifier-table reordering --
      Checkpoints 7/8/9). **Explicitly NOT done, out of this task's scope,
      left for a future checkpoint:** the declaration-alignment-grid
      column integration §11's main paragraph and §11.2's own worked
      example both call for (RDD_KEY_183's `=`-aligned/`:`-aligned group
      behavior across consecutive declarations, and the padded-modifier-
      phrase grid alignment §11.2's own worked example shows) -- every
      checkpoint in this task was a flat spacing/reordering pass only, no
      `Declaration`/`ColumnGrid` parser modeled on
      `KotlinDeclarationAlignmentRule`/`KotlinSignatureRule` was written at
      any point this session.
      **Checkpoint 10 done -- §4 (template literal `${...}` interpolation
      spacing) only.** New `JsTsSpecificRule.enforceTemplateLiteralInterpolationSpacing`
      (`isJs || isTs`-gated): for every non-frozen backtick STRING token, a
      hand-rolled scanner (`findInterpolationSpans`, mirroring
      `TokenizerCurly.skipTemplateInterpolation`'s own nesting rules -- brace
      depth counting with `"`/`'`/`` ` ``-quoted spans skipped as opaque units
      via `skipQuotedSpan`, so a `}` inside a nested string/template doesn't
      corrupt depth counting) finds every top-level `${...}` span in the
      literal's raw text; each interior substring is re-tokenized in
      isolation via a fresh `TokenizerCurly` for the same language and
      re-joined via `MiscRuleCurly.renderTokens` (accessible directly --
      `protected` + same package, `com.jxmake.formatter.rules` -- no new
      spacing logic invented, reuses the same generic tight/loose
      token-adjacency rules every other rendering path already uses).
      Conservative bailout matching this file's other passes: a span
      containing a NEWLINE, a comment, or a frozen token is left
      byte-for-byte untouched (`reformatInterpolationInterior` returns
      `null`), as is an empty/blank interior. **Documented, deliberate scope
      limit:** a nested template literal inside an interpolation (`` `${`inner
      ${x}`}` ``) is treated as one opaque quoted span by `skipQuotedSpan`
      for span-finding purposes -- its own interior `${...}` is not
      recursively reformatted this pass (doubly-nested interpolation is rare
      enough in practice that recursion wasn't judged worth the added risk
      this session; a future checkpoint could recurse `rewriteTemplateLiteral`
      into any nested-backtick span found by `findInterpolationSpans` if this
      ever surfaces as a real gap). Wired into `FormatterCurly` Phase 4,
      directly after the §3/§7 spread/optional-chaining block, inside the
      same `if (lang.isJs || lang.isTs)` guard. Verified via a standalone
      harness (`FormatterCore.forLanguage("js")`/`"ts"`): `` `Hello,
      ${user.name}!` `` stays untouched (already-correct spacing);
      `` `Sum: ${a+b}` `` → `` `Sum: ${a + b}` ``; deliberately over-spaced
      `` `Val: ${  a  +  b  }` `` → `` `Val: ${a + b}` ``; a method-call
      interpolation (`` `Nested ${obj.get('x')}` ``) stays untouched (already
      correct, confirms call/dot-access tight-joining rules carry over
      correctly through `renderTokens`); a literal with no interpolation at
      all stays untouched; two adjacent interpolations in one literal
      (`` `${a}${b+c}` ``) both correctly spaced independently; a quoted `}`
      inside an interpolation's own string literal (`` `str with
      ${a==="}"?1:2}` ``) does not prematurely end the span -- correctly
      renders to `` `str with ${a === "}" ? 1 : 2}` ``; a multi-line
      interpolation (`` `multi ${\n  a + b\n}` ``) is correctly left
      byte-for-byte untouched (NEWLINE-inside-interpolation bailout).
      Round-trip (harness round1 → round2) confirmed idempotent on every
      case above. `make` compiles clean; `make test`: 106/106 forward +
      106/106 idempotency, zero regressions.
      **Checkpoint 11 done -- §5 (named function/class-method Allman brace
      style) only.** New `JsTsSpecificRule.enforceMethodDefinitionAllmanBraceStyle`
      (`isJs || isTs`-gated), structurally modeled on
      `JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle` (§5's own
      text says it "mirrors" Java) but substantially simplified -- JS/TS has
      no `throws` clause, no compact-constructor shape, and no
      enum-constant-body false positive to guard against. Candidate signal:
      the `{`'s header, walked backward via `findHeaderCloseParen`, must
      resolve to a `)` whose matching `(` is immediately preceded by an
      IDENTIFIER -- excludes every control-flow brace (keyword before `(`,
      never an identifier) and every anonymous function expression (`(`
      preceded directly by the `function` keyword, not a name) by
      construction. Two header shapes handled: direct `)` `{` adjacency
      (plain JS, or TS with no return type), or a TS return-type-annotation
      tail (`): Promise<Result> {` -- found via a backward walk for a `:`
      immediately preceded by `)`, the return type's own interior content is
      never inspected, only relocated along with the brace). An arrow
      function's block body (`=> {`) is excluded by construction too -- its
      `{` is directly preceded by `=>`, never matching either header shape,
      so §6's K&R-for-arrow-bodies needs no explicit check here. Two
      "stays K&R" exceptions per §5's own text: an empty body (`isEmptyBody`)
      and any one-liner whose whole `{ ... }` body sits on one physical line
      (`isSingleLineBraceBody` -- covers getter/setter one-liner groups and
      any other STYLE.md §14 squeeze-onto-one-line body without a
      getter/setter-specific check, since any one-liner method stays K&R the
      same way). Wired into `FormatterCurly` Phase 1, as its own `else if
      (lang.isJs || lang.isTs)` branch alongside the existing
      C/C++/Java/Kotlin Allman-conversion branches, right after the shared
      Phase-1 structural passes (mirrors where Java's own call sits).
      Verified via a standalone harness (`FormatterCore.forLanguage("js")`/
      `"ts"`): a plain named function (`function process(data, count) {...}`)
      converts to Allman; the same with a TS return-type annotation
      (`function process(data: string, count: number): Promise<Result> {`)
      converts to Allman with the return type preserved intact after the
      relocated `)`; a class method (`render() {...}`) converts; a
      constructor converts; `async`/`static` methods both convert
      (`async doThing(x) {...}`, `static make() {...}`); an empty body
      (`function empty() {}`) stays K&R untouched; getter/setter one-liners
      (`get x() { return this._x; }`) stay K&R untouched (one-liner
      exception); an arrow-function block body (`const add = (a, b) => {
      return a + b; };`) stays K&R untouched (never matched the candidate
      signal); a `{` already on its own line round-trips unchanged
      (idempotent by construction, confirmed). Round-trip (harness round1 →
      round2) confirmed idempotent on every case above. `make` compiles
      clean; `make test`: 106/106 forward + 106/106 idempotency, zero
      regressions.
      **Pre-existing bug found while smoke-testing (NOT caused by this
      checkpoint's own change -- confirmed by rebuilding Checkpoint 10's own
      commit, e73da4d, in isolation and reproducing the identical output
      there too; documented, not fixed, out of §4/§5/§6's scope):**
      `get x() { return this._x; }` / `set x(v) { this._x = v; }` inside a
      class body render with corrupted parameter-list spacing --
      `get x(  ) { return this._x; }` (double space inside an empty-looking
      paren pair -- actually the getter has no params, so `()` should stay
      tight) and `set x( v) { this._x = v;    }` (missing space after `(`
      before `v`, and stray trailing spaces before the closing `}`). This
      looks like a pre-existing STYLE.md §14 one-liner-group/getter-setter
      squeeze-and-align pass (`GetterSetterRuleCurly`, shared with
      C/C++/Java/Kotlin) mishandling JS/TS's getter/setter shape -- not
      investigated further this checkpoint (out of §4/§5/§6's scope; this
      task's own brief only covers §5's Allman/K&R brace-placement
      decision, not `GetterSetterRuleCurly`'s column/spacing rendering).
      Flagged here for whichever future checkpoint picks up §8 (Getter/
      Setter Accessors), which explicitly plans to reuse
      `GetterSetterRuleCurly`'s existing one-liner-group alignment machinery
      for JS/TS and will need to fix this as part of that wiring.
      **Checkpoint 12 done -- §6 (arrow function spacing/brace-style/
      parameter-parens) implemented via two new `JsTsSpecificRule.java`
      methods, `enforceArrowSpacing` and
      `enforceArrowFunctionParameterParens`.** Confirmed empirically (not
      assumed, per this checkpoint's own brief) via a standalone harness
      that `=>` spacing was NOT already free from any existing generic
      pass -- this codebase has no general from-scratch binary-operator
      respacing pass for any language, so `enforceArrowSpacing` (a flat
      gap-normalizing scan, structurally identical to
      `enforceOptionalChainingSpacing`/`enforceUnionIntersectionSpacing`,
      enforcing exactly one space on both sides of `=>` and bailing out on
      comments/NEWLINE/frozen tokens) was needed. `enforceArrowFunctionParameterParens`
      wraps a bare single-parameter arrow's parameter in parens
      (`n => ...` -> `(n) => ...`); STYLE_JS_TS.md §6's exact text was
      re-read to resolve what the task brief flagged as a plausible
      ambiguity ("preserve as written" vs. "always parenthesize") --
      it is unambiguous once read carefully: "Parameter parens: keep even
      for a single untyped parameter (`(n) => ...`, not `n => ...`) for
      alignment consistency with multi-parameter arrows in the same
      group." So the rule is always-parenthesize, not preserve; this was
      not a genuine blocking ambiguity and the STOP-CODING protocol was
      not invoked. The remaining two §6 items -- K&R (not Allman) braces
      for arrow block bodies, and same-line no-braces for single-
      expression bodies -- needed no new code: arrow bodies are excluded
      from §5's Allman candidate signal by construction (an arrow body's
      `{` is preceded by `=>`, never by `)` or a `)`-terminated TS return
      type, which is the only signal §5's Allman pass matches), and this
      codebase never auto-adds or strips braces around an expression body
      for any language, so a single-expression arrow body's existing
      brace-free form simply round-trips untouched.
      **Idempotency bug found and fixed before this checkpoint could be
      committed:** an initial implementation wired
      `enforceArrowFunctionParameterParens` into `FormatterCurly.java`'s
      Phase 4 (alongside the other JS/TS cosmetic spacing passes, right
      after §4's template-literal call). A 12-case smoke harness
      (formatting each case twice and diffing) caught one non-idempotent
      case: `arr.map(x => x * 2);` formatted (pass 1) to
      `arr.map((x) => x * 2);` -- staying *tight* around the outer
      `.map(...)` call's own parens -- but reformatting that same output
      (pass 2) produced `arr.map( (x) => x * 2 );`, now *loose*. Root
      cause: `enforceComplexityPadding` (the shared STYLE.md §3.1 tight-
      vs-loose call-padding pass) had already run and decided "tight"
      for the outer call *before* Phase 4's arrow-parens pass introduced
      the new nested `(x)` shape inside its argument list -- so a fresh
      format never saw the post-insertion shape in time to pad loosely,
      while a reformat of already-`(x)`-containing output did. This is
      the exact bug class `FormatterCurly.java`'s own comments already
      document and fix elsewhere in this file (re-running
      `enforceComplexityPadding` after any pass that can introduce a new
      nested-paren/bracket shape, so the padding decision always sees the
      final shape on the very first pass). Fix: moved
      `enforceArrowFunctionParameterParens`'s call site out of Phase 4
      and into Phase 1, immediately after the existing §5 Allman call and
      immediately before Phase 1's own `enforceComplexityPadding` re-run
      (line ~144) -- so that re-run's tight-vs-loose decision now always
      sees the post-parenthesization shape, on both a fresh format and a
      reformat. `enforceArrowSpacing` stayed in Phase 4 (it only adjusts
      spacing around an existing `=>` token, never adds/removes a
      paren/bracket, so it cannot affect any complexity-padding decision
      either way). Re-verified after the fix: all 12 smoke-harness cases
      -- including bare/parenthesized/typed single params, multi-param,
      no-param, async arrows, arrow-as-class-method-body, and a
      multi-line arrow body -- round-trip idempotently (round1 == round2
      in every case), and the previously-failing
      `arr.map(x => x * 2);` case now produces `arr.map( (x) => x * 2 );`
      identically on both the first format and every reformat after.
      `make` compiles clean; `make test`: 106/106 forward + 106/106
      idempotency, zero regressions.
      **Checkpoint 13 done -- pre-existing `GetterSetterRuleCurly` paren-
      spacing bug (Checkpoint 11 notes) fixed, as its own small commit ahead
      of §8's wiring.** Root cause, confirmed via a standalone harness
      (`FormatterCore.forLanguage("js")`) and a `git stash`-style before/
      after comparison against `make test`'s existing Java fixtures: the
      nested `callGrid`'s params-column split (`GetterSetterRuleCurly
      .render`, the type/name pre-padding block used when every member's
      params can be split into a type cell and a name cell) unconditionally
      inserted a `" "` separator between the (possibly empty) type cell and
      the name cell -- correct for Java/C++, where a real param always has a
      type token, but wrong for JS/TS's untyped single-param setters
      (`set x(value)`), where `typeTexts[i]` is always `""` for every member
      in the group (no type token exists at all), so the hardcoded separator
      leaked in as a literal leading space before the param name
      (`set x( value)`). Fixed with a new `noTypeColumn` flag (`maxTypeWidth
      == 0` across the whole group -- true only when no member in the group
      has any real type text): when set, the params cell is just the
      (width-padded) name text with no separator, skipping the type column
      entirely instead of padding it to zero width plus a stray space.
      **Explicitly confirmed NOT a bug, left untouched:** the empty-
      parameter-list padding-to-match-a-wider-sibling's-width behavior
      itself (e.g. `get x(     )` next to `set x(value)`) -- an initial fix
      attempt that forced empty parens to always render tight regardless of
      group padding broke 7 existing Java/C++ fixtures
      (`java_core`/`java_combined`/`cpp_core`/`cpp_modern`/`cpp_combined`/
      `cpp_comments`/`hpp_core`, confirmed via `make test`'s own reference
      `_out` files, e.g. `public int getCount(              ) { return
      count; }`), which prove this padding is Java's own long-established,
      intentional STYLE.md §14 behavior, not a bug -- reverted that part of
      the attempt; only the type/name separator-space fix (which has no
      Java/C++ analog, since their params are never typeless) was kept.
      Verified via a standalone harness (`FormatterCore.forLanguage("js")`):
      a `get x() {...}` / `set x(value) {...}` / `get y() {...}` group now
      renders as `get x(     ) { return this._x;  }` / `set x(value) { this
      ._x = value; }` / `get y(     ) { return this._y;  }` -- consistent
      empty-parens padding (matching Java's own precedent, not a bug) and no
      more leading space before `value`. Round-trip (harness round1 ->
      round2) confirmed idempotent. `make` compiles clean; `make test`:
      106/106 forward + 106/106 idempotency, zero regressions (Java/Kotlin/
      C/C++ getter-setter fixtures byte-for-byte unchanged after the revert
      of the empty-parens part).
      **Checkpoint 14 done -- §8 (getter/setter accessors) verified as
      already functional, no additional wiring code needed beyond
      Checkpoint 13's bug fix.** `ScopePipelineCurly`'s constructor already
      instantiates plain `GetterSetterRuleCurly` for any non-Kotlin language
      (`lang.isKotlin ? KotlinGetterSetterRule : GetterSetterRuleCurly`), so
      JS/TS already fall into that shared machinery with no per-language
      branch needed there. `get`/`set` are lexed as `KEYWORD` (already in
      `KEYWORDS_JS`/`_TS` from the tokenizer checkpoint), so
      `parseOneLinerMember` naturally parses the `get`/`set` keyword as a
      stand-in "return type" (the same generic mechanism that gives C++/Java
      their real return-type column) -- no JS/TS-specific parsing branch was
      needed for the accessor shape itself. Verified via a standalone
      harness (`FormatterCore.forLanguage("js")`/`"ts"`): a `get x()`/
      `set x(value)`/`get y()` group renders correctly column-aligned
      (`get x(     ) { return this._x;  }` / `set x(value) { this._x =
      value; }` / `get y(     ) { return this._y;  }`), matching STYLE.md
      §14's empty-parens-padded-to-widest-sibling convention now that
      Checkpoint 13's separator-space bug no longer corrupts the typed-
      params column; a mixed-name-width group (`x` / `horizontalPosition`)
      aligns correctly; the outlier/overflow-exclusion path works
      unmodified (a member whose body alone would overflow the line length
      is correctly excluded from the group, left as an ordinary standalone
      member); a TS getter/setter pair with explicit return-type/param-type
      annotations (`get x(): number { ... }` / `set x(value: number) { ...
      }`) round-trips correctly too, though **not through the grouped
      column-alignment path** -- see Known Gap below. Round-trip (harness
      round1 -> round2) confirmed idempotent on every case above. `make`
      compiles clean; `make test`: 106/106 forward + 106/106 idempotency,
      zero regressions.
      **Known gap, deliberately not attempted this checkpoint (scope
      discipline, not an oversight):** STYLE_JS_TS.md §8's own worked
      example groups a plain no-return-type-token method (`isValid() { ...
      }`) alongside `get`/`set` accessors in the same aligned group. An
      initial attempt to support this (relaxing `parseOneLinerMember`'s
      existing `noReturnType && pureSpecifier == null -> reject` guard for
      JS/TS, scoped narrowly to only accept when the member turns out to be
      `{`-terminated, so a bare call-expression statement like `foo(x);`
      stays correctly rejected) parsed successfully but produced a genuine
      column-misalignment bug: `render()`'s pre-existing
      `mergeReturnTypeIntoCall` special case (documented for C++ constructor
      groups, where every merged member independently owns its whole
      `name(params)` phrase) fires whenever ANY member in the group has an
      empty return type -- which then merges `isValid`'s name into the same
      cell axis as `get`/`set`'s prefix+name, but the *nested* callGrid that
      pads the name column had already run independently beforehand with no
      awareness of the merge, so the two paddings fight and produce
      misaligned parens (`isValid           (     )     {` next to `get x
      (     ) {`, columns off by the accessor-keyword's own width). Fixing
      this properly needs `mergeReturnTypeIntoCall`'s design reconsidered
      for a "some members have a real prefix, others don't, but all still
      need independently-aligned name/params" shape that never occurs in
      the pre-existing C/C++/Java corpus (a constructor's whole point is
      that its uniquely-named siblings don't share a name column to begin
      with) -- judged out of scope for a "reuse the existing machinery"
      task, reverted rather than pushed through. **Current behavior:** a
      plain method like `isValid()` next to `get`/`set` accessors is left
      ungrouped (rendered as an ordinary standalone member, not corrupted,
      just not column-aligned with its `get`/`set` siblings) -- correct and
      safe, just short of the style doc's full worked example. Flagged here
      for a future checkpoint that wants to extend `mergeReturnTypeIntoCall`
      properly, not attempted further this session.
      **Checkpoint 15 done -- §9 (decorators) implemented via three new
      `JsTsSpecificRule.java` methods.** `enforceDecoratorTightAtSpacing`
      (flat gap-normalizing pass, `isJs || isTs`-gated, wired into
      `FormatterCurly` Phase 4 right after §6's `enforceArrowSpacing`):
      confirmed empirically (harness) that `@` tight-binding is NOT already
      free -- a deliberately mis-spaced `@ Inject(TOKEN)` round-tripped
      untouched before this pass, since the codebase's generic token-join
      rules have no existing tight-unary-prefix exception for `@` (unlike
      `!`/`~`). `enforceDecoratorOverflowCascade` (structural, `isJs ||
      isTs`-gated, wired into `FormatterCurly` Phase 1 right before §6's
      `enforceArrowFunctionParameterParens`, same ordering reasoning: a
      pass that inserts a line break has to run before any later
      width-driven pass's "does it fit" check, or a fresh format and a
      reformat of already-split output disagree) implements only the
      cascade's first step -- dropping an inline decorator (one whose own
      line still holds more content after it) to its own line, at the same
      indentation, when the decorator + rest-of-line combined exceeds
      `lineLengthLimit`. The second cascade step (wrapping the decorator's
      own overlong argument list, once alone on its own line) needed **no
      new code** -- a decorator's `@Name(args)` call already matches
      `MiscRuleCurly.enforceCallLineBreaking`'s generic "IDENTIFIER (" scan,
      so it's wrapped by that existing pass the same as any other overlong
      call, confirmed via the harness (an already-own-line decorator with
      an overlong argument list wraps its args via the pre-existing
      dropped/one-per-line machinery, no decorator-specific code involved).
      A helper `findDecoratorEnd` locates the decorator application's own
      end (the name identifier for a bare `@Name`, the matching `)` for
      `@Name(args)`, walking forward over `.` for a qualified name like
      `@ns.Name` too). **Placement preservation** (§9's "never move a
      decorator from one placement to the other") needed no code at all --
      it's the default do-nothing behavior; `enforceDecoratorOverflowCascade`
      only ever acts on an inline decorator that doesn't fit (dropping it),
      never touches an already-own-line decorator, and never merges one back
      inline either way, confirmed via harness (an own-line `@Input()` /
      `name: string;` pair round-trips unchanged regardless of length).
      Verified via a standalone harness (`FormatterCore.forLanguage("ts")`):
      `@Component({...})` class decorator and `@Input()`/`@Output()`
      property decorators and `constructor(@Inject(TOKEN) private service:
      Service) {}` parameter decorator (the style doc's own worked example)
      all round-trip with correct tight `@` spacing; a deliberately
      mis-spaced `@ Component(...)` / `@   Input()` both get corrected to
      tight; an inline decorated field under the line-length limit stays
      inline (no cascade); an inline decorated field over the limit drops
      to its own line at the same indent, with the target now beginning the
      next line; a bare (no-args) decorator + long target combination also
      cascades correctly; an already-own-line decorator (with or without
      overflow) is left completely untouched either way. A combined
      decorated-class-with-getter/setter-accessors case (`@Component({...})
      export class Widget { @Input() name: string; get x() {...} set x(v)
      {...} }`) renders correctly end-to-end, confirming §8 and §9 compose
      without interference. Round-trip (harness round1 -> round2) confirmed
      idempotent on every case above. `make` compiles clean; `make test`:
      106/106 forward + 106/106 idempotency, zero regressions.
      **§8 and §9 both done as scoped this multi-checkpoint task**
      (Checkpoints 13/14/15). **§15 (import ordering) attempted next per the
      agreed ordering and found BLOCKED on a genuine, doc-confirmed open
      design question** -- see Open Design Questions above for the full
      detail (`STYLE_JS_TS.md` §15 explicitly states its own classification
      logic is unresolved, twice, and "local" detection's second disjunct
      needs an undesigned project-source-root config concept). No §15 code
      written this session (checklist item stays `[~]`, not advanced).
      **Next up:** either resolve the §15 ambiguity (ask the user how to
      define local-import detection and whether a source-root config key
      should be added) and then implement §15, or skip ahead to §12-14
      (enums, generics, interface/type-alias), which remain otherwise
      unstarted and have no known blocking ambiguity. Unblocking the HTML5
      `<script>` dispatcher (the cross-job follow-up note near the bottom of
      this file) remains a separate, later task either way.
- [x] Author local test fixture pairs per `FUTURE_TEST_FIXTURES.md`'s
      "JavaScript"/"TypeScript" sections (split by extension since TS-only
      constructs can't live in `.js`). Done: `js_combined_inp/out.js`,
      `js_comments_inp/out.js`, `ts_combined_inp/out.ts`,
      `ts_comments_inp/out.ts` extracted to `test/`, registered
      commented-out in the Makefile (real logic not yet implemented),
      documented in `test/README.txt`.
- [ ] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      `STYLE_JS_TS.md`'s listed test-fixture repos (`nodejs/node`,
      `expressjs/express`, `lodash/lodash`, `microsoft/TypeScript`,
      `angular/angular`, `nestjs/nest`, `vuejs/core`).
- [ ] **Follow-up once real JS/TS logic lands (cross-job note from
      `STATE_DATA_FORMATS.md`'s HTML5 §4 work):** `XmlSpecificRule
      .renderScriptOrStyle` dispatches HTML5 `<script>` content to a real
      JS formatter, but since JS/TS is still scaffold-only, any real
      (non-frozen) `<script>` content currently throws (no dispatch call
      exists yet). Two local fixtures (`test/html_combined_inp/out.html`,
      `test/html_comments_inp/out.html`) work around this by wrapping their
      real JS `<script>` bodies in a temporary `//% JXM_CFMT_DIS`/`//%
      JXM_CFMT_ENA` pair, which forces `renderScriptOrStyle` to treat the
      block as opaque/verbatim instead of attempting dispatch. Once this
      job lands a real JS formatter: (1) wire an actual dispatch call into
      `renderScriptOrStyle`'s `isJsType && !frozen` branch (currently just
      throws), reformatting the spliced-out content and reindenting it back
      in, matching `<style>`'s existing CSS-splice shape; (2) remove the
      `//% JXM_CFMT_DIS`/`ENA` pair from both HTML fixtures' `_inp.html` and
      `_out.html`; (3) regenerate both `_out.html` files from the real JAR
      and re-run `make test` to confirm the newly-real `<script>` output is
      correct before committing.
