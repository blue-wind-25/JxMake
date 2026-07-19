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
  Items" section.

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
- [ ] Implement §2–15 rule-by-rule, each its own checkpoint commit, per
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
