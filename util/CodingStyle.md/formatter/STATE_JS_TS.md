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

### Blocking issue found while attempting to flip the JS scaffold gate (this session)

**Session task was: (1) flip `Lang.isScaffoldOnly` for `js` (not `ts`), smoke-test via the real
`Main.java` CLI path for the first time, then (2) wire `XmlSpecificRule.renderScriptOrStyle`'s
`isJsType && !frozen` branch to dispatch to the real JS formatter, then (3)/(4) update the two
blocked HTML fixtures and re-run `make test`.** Step 1's own mandated pre-flip sanity check
(`make test` baseline, then a CLI smoke-test after flipping) surfaced a real, non-trivial,
pre-existing bug that blocks safely flipping the gate — documented here per `STATE_COMMON.md`'s
ambiguity protocol; the `Lang.java` edit that flipped the gate has been **reverted** (working tree
is back to `js`/`ts` both scaffold-only, matching commit `4fff96f`), no code change from this
session is retained.

**The bug:** a plain, unspaced declaration statement — `let x=1;`, `const x=5;`, `var z=7;` — is
left **completely untouched** end-to-end through the real JAR (`code-formatter.sh`), no `=`
spacing inserted, unlike every other curly-family language (verified `int x=1;` → `int x = 1;` in
Java through the same JAR run for comparison). Root cause, confirmed by reading
`DeclarationAlignmentRuleCurly`'s constructor (`rules/DeclarationAlignmentRuleCurly.java`, ~line
48): its type-keyword/modifier-priority selection is only `lang.isJava ? JavaModifierPriority :
CppModifierPriority` — no `isJs`/`isTs` branch exists — so for JS, `TYPE_KEYWORDS_C` (C's tiny
built-in-type set) and `CppModifierPriority` are used to try to parse the statement as a
declaration. `let`/`var` are neither a recognized C type keyword nor a recognized C/C++ modifier
(only `const` happens to be, per Checkpoint 4's note), so `parseDeclaration` returns `null` for any
`let`/`var` declaration (and even for many `const` ones, confirmed: `const x=5;` also stayed
untouched) — the statement never enters the declaration-rendering path at all, so no `=`-spacing
(or any other declaration-grid normalization) is ever applied. This reproduces even for the
simplest single, non-destructured declarator, not just destructuring patterns.

**Why this blocks the flip, not just a "small/obvious fix":** this is not a narrow edge case — a
`let`/`const`/`var` declaration with tight `=` spacing is one of the single most common statement
shapes in real-world JS, so flipping the scaffold gate today would mark JS "done" for real files
while silently leaving a large fraction of ordinary declaration statements completely unformatted.
It is, however, **not a new/surprise bug** in the sense of being undocumented: it is the direct,
predictable consequence of an already-known, already-deferred gap — the checklist item below ("When
implementing §11 below (declaration/parameter alignment), start from
`KotlinDeclarationAlignmentRule.java`/`KotlinSignatureRule.java`...") was never actually done.
Checkpoints 7 and 9 (§11's colon-spacing / union-intersection-spacing / modifier-reordering flat
passes) each explicitly recorded that the declaration-alignment-**grid** integration itself
(`Declaration`/`ColumnGrid` parser modeled on Kotlin's, which is what would give JS/TS declarations
a real parse path with `=`-spacing) was deliberately left undone, out of scope for those
checkpoints. This session is the first time that gap's real-world consequence was actually
observed end-to-end (all prior verification was via hand-picked already-correctly-spaced harness
examples, e.g. `const x = 5;`, per Checkpoint 4's own text — never a deliberately-unspaced input).

**Per `STATE_COMMON.md`'s hard-rule guidance in this session's own task brief** ("if flipping the
JS scaffold gate surfaces real, unexpected bugs... that aren't small/obvious fixes — don't attempt
a large unplanned redesign, document and stop"): stopping here rather than attempting the
`DeclarationAlignmentRuleCurly` JS/TS integration as an improvised addition to this session's scope.
That integration is exactly the still-unchecked checklist item below and deserves its own dedicated
checkpoint(s) (parser + rendering + fixture verification), not a rushed fix bolted onto the
scaffold-gate/HTML5-dispatcher task.

**Not done this session as a result:** the JS scaffold-gate flip (reverted), the
`XmlSpecificRule.renderScriptOrStyle` HTML5 `<script>` dispatch wiring, and the two HTML fixture
updates (`test/html_combined_inp/out.html`, `test/html_comments_inp/out.html`) — all three are
downstream of the gate flip and were not attempted once the blocker was found, per the "don't push
through degraded" guidance. `make test` is unchanged from this session's own confirmed baseline:
106/106 forward + 106/106 idempotency (no code changes were kept).

**Suggested next step for a future session:** implement real JS/TS awareness in
`DeclarationAlignmentRuleCurly` (its own checklist item below), verify a plain unspaced
`let`/`const`/`var` declaration gets correctly `=`-spaced (and grid-aligned where applicable) via a
standalone harness first, *then* re-attempt this session's scaffold-gate-flip task from scratch —
the smoke-test step should include at least one deliberately unspaced plain declaration statement
(not just destructuring or already-correctly-spaced examples) before considering the flip safe.

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
- [x] **RESOLVED — declaration-alignment-grid support for `let`/`const`/
      `var` implemented, mirroring `KotlinDeclarationAlignmentRule.java` as
      a structural template.** New `JsTsDeclarationAlignmentRule.java`
      (`extends DeclarationAlignmentRuleCurly`, mirrors
      `KotlinDeclarationAlignmentRule`'s shape: its own `Row`
      record/`parseDeclaration`/`groupAlignableDeclarations`/
      `renderAlignedGroup`, not the base class's C/Java-grammar `Declaration`
      parser -- same "no seam to inject a reversed-grammar branch, base
      `parseDeclaration` is `private`" finding a research pass confirmed
      before writing any code). Unlike Kotlin, JS/TS statements are always
      `;`-terminated (§2), so this class reuses the base class's own
      `splitStatements` directly instead of Kotlin's newline-based
      `splitKotlinStatements`; JS/TS also has no modifier table for local
      declarations, so the grid has no modifier columns, just `keyword name
      [: type] [= init] ;` with the trailing `;` appended directly onto
      each row's own last non-empty cell (not its own separately-joined
      `ColumnGrid` cell, which would otherwise leave a stray space before it
      once cells are joined with `" "`). Wired into `ScopePipelineCurly` as
      a new `jsTsDeclarationRule` field (`(lang.isJs || lang.isTs) ? new
      JsTsDeclarationAlignmentRule(...) : null`, mirroring
      `kotlinDeclarationRule`'s construction) and a new
      `applyJsTsDeclarationsPass` method, dispatched from
      `applyDeclarationsPass` alongside the existing Kotlin branch; reuses
      `addKotlinDeclReplacement`'s identity-based splice-back helper as-is
      (works unchanged for any `firstAnchor`/`lastAnchor` token pair,
      Kotlin- or JS/TS-specific).
      **Deliberately out of scope this checkpoint, left completely
      untouched by this class (documented in the class's own doc comment,
      not a regression -- these statements render exactly as they did
      before this checkpoint, i.e. still unspaced for `=`, no worse):**
      destructuring-pattern LHS (`const { a, b } = obj;`, `const [x, y] =
      arr;`, RDD_KEY_182) and multi-declarator statements (`let a = 1, b =
      2;`, `let a: number, b: string;`) both make `parseDeclaration` return
      null, so the whole statement is left out of any group and rendered
      verbatim -- same "never guess past an unrecognized shape" posture as
      Kotlin's own class. `type X = ...` alias groups (RDD_KEY_183) are a
      separate future extension too -- `type` is not recognized as a
      declaration keyword anywhere in this class.
      **Two real bugs found and fixed as prerequisites during verification,
      not scoped to this checkpoint originally:**
      (1) Checkpoint 7's `JsTsSpecificRule.enforceTypeColonSpacing` flat
      pass runs in `FormatterCurly` Phase 4, strictly after
      `ScopePipelineCurly`'s Phase 0 (which houses this new grid pass) --
      without any fix, the flat pass's "tight before / one space after"
      normalization always ran on top of the grid's own column-padded `:`,
      collapsing the alignment back down to a single space on every format
      (confirmed via harness: `let a: number = 5;` grid-aligned correctly
      alongside siblings by `JsTsDeclarationAlignmentRule.renderAlignedGroup`
      directly, but the same statement through the *full* `FormatterCore
      .forLanguage("ts")` pipeline lost its padding). Fixed with a narrow
      change to `JsTsSpecificRule.classifyTypeColons`/`isTypeColonAt`: the
      `declaratorCtx` disjunct for a colon immediately following the
      `let`/`const`/`var` keyword itself (a statement's *first* declarator)
      is now additionally gated on a new depth-aware forward scan,
      `hasTopLevelCommaBeforeSemicolon`, so it only still counts as a "type
      colon to flat-normalize" when the statement is a genuine
      multi-declarator one (which the grid above explicitly does not
      handle, so still needs this flat pass) -- a single-declarator
      statement's colon is now correctly left alone by the flat pass,
      since the grid already owns its spacing/alignment entirely. A
      subsequent declarator's own colon (`ctx` is `,`, not the keyword) is
      unaffected by this change -- always was, and remains, flat-normalized,
      since every multi-declarator declarator past the first is never
      handled by the grid either way.
      (2) `DeclarationAlignmentRuleCore.isTightToken`'s `Token.isRepOp(t,
      '*')`/`'&'` tight-punctuation exception (written for C/C++'s repeated
      pointer/reference operators, `**`/`&&`-as-rvalue-reference-declarator)
      was already gated `!lang.isKotlin` but not `!lang.isJs`/`!lang.isTs`
      -- found via harness testing an arrow-function initializer
      (`const list = items.map(x => x * 2);`) rendered through this new
      class's `renderTokens(initTokens)` call: `*` was wrongly treated as
      tight-against-preceding-operand, corrupting `x * 2` to `x* 2` (JS has
      no pointer/reference `*`/`&`/`**`/`&&`-declarator construct either --
      `*` is always multiplication, `&`/`&&` always bitwise-/logical-AND).
      Fixed by adding `&& !lang.isJs && !lang.isTs` to the same exclusion
      already carved out for Kotlin -- a shared base-class method used by
      every curly-family declaration/initializer renderer, so re-verified
      the full `make test` suite (Kotlin/Java/C/C++ fixtures) is
      byte-for-byte unaffected by this addition.
      Verified via a standalone harness (`FormatterCore.forLanguage("ts")`/
      `"js")`): the original blocking-bug repro (`let x=1;`, `const x=5;`,
      `var z=7;`) now all correctly render with `=` spacing (`let x = 1;`
      etc.), individually and as a column-aligned group (keyword-column and
      `=`-column both aligned across `let x = 1; / const y = 2; / var z =
      7;`); a TS-typed declaration mixed into the same group (`let a:
      number = 5;`) correctly gets both its own `:`/`=` columns aligned
      against its untyped siblings (blank `:` column shown for the
      untyped rows); a group of all-typed declarations (`let name: string
      = "hi"; / let count: number = 1; / const w = 1; / const wideName =
      2;`) aligns correctly too; a multi-declarator statement (`let a:
      number=1, b: string="x";`) is left completely untouched (documented
      gap, not a regression -- confirmed identical to this same input's
      pre-checkpoint behavior via `git stash`); destructuring declarations
      (`const { p, q } = obj;`, `const [m, n] = arr;`) are unaffected,
      confirming no regression on Checkpoints 5/6's fixes; arrow-function
      initializers (`const add = (a, b) => a + b;`), template-literal
      initializers (`` let greeting = `Hello, ${name}!`; ``), a call-
      expression initializer with an arrow-function argument (`const list
      = items.map(x => x * 2);`, exercising both bug fixes above together),
      a TS function-type-annotated declaration (`let arrow: () => void =
      () => {};`), a class field (`private static readonly count: number =
      0;`, confirming §11.2's class-field modifier-reordering pass and this
      new declaration-local-variable class don't interfere -- class fields
      are a different statement shape entirely, never matched by this
      class's `parseDeclaration`), a trailing end-of-line comment (`let x =
      1; // trailing comment`, correctly preserved and aligned in its own
      column), and a multi-line call-argument initializer (`let y =
      foo(\n  1,\n  2\n);`, correctly collapsed to one line, matching the
      pre-existing `enforceCallLineBreaking`/complexity-padding precedent)
      all render correctly. Round-trip (harness round1 → round2) confirmed
      idempotent on every case above. `make` compiles clean; `make test`:
      106/106 forward + 106/106 idempotency, zero regressions in the
      existing C/C++/Java/Kotlin fixture corpus (both shared-class changes
      above re-verified against the full suite, not just spot-checked).
      **Not done this checkpoint, left for a future one (context-budget
      stop, per this task's own explicit guidance -- this class is judged
      solid and well-tested as far as it goes, but the remaining scope is
      substantial enough to deserve its own dedicated session rather than a
      rushed extension here):** destructuring-pattern LHS joining the grid
      (RDD_KEY_182's full integration -- currently these statements are
      simply excluded from the grid entirely, never corrupted, just not
      aligned), multi-declarator statement support (`let a = 1, b = 2;`
      getting real `=`/`: ` spacing and grid alignment, not just being left
      verbatim), and `type X = ...` alias-group alignment (RDD_KEY_183).
      No local `.js`/`.ts` fixture pair was registered/verified via `make
      test` this checkpoint either (still blocked on the broader §2-15
      real-code-testing-pass checklist item below, unchanged from before
      this checkpoint). The original blocking bug from the "Blocking issue
      found while attempting to flip the JS scaffold gate" Open Question
      above is now fixed and verified (see that section's own text for the
      repro) -- that Open Question entry is left in place as a historical
      record of what blocked the scaffold-gate-flip attempt, but its root
      cause is resolved; a future session re-attempting the scaffold-gate
      flip should re-run that session's own smoke-test procedure (including
      a deliberately unspaced plain declaration) to confirm before
      proceeding, and should also decide whether the remaining declared-out-
      of-scope gaps above (destructuring/multi-declarator/type-alias grid
      integration) need to land first or can follow separately.
- [x] Implement §2–15 rule-by-rule, each its own checkpoint commit, per
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
      **§15 ambiguity resolved (RDD_KEY_195) — user asked and answered.**
      Decision: drop the source-root disjunct entirely; local-import
      detection is `./`/`../`-prefix only, everything non-built-in and
      non-relative is third-party (including bundler/tsconfig
      `baseUrl`/`paths` absolute first-party imports, an accepted
      misclassification). §15 implementation (config keys, classification,
      grouping/sort/blank-line rendering) unblocked, continues next.
      **Checkpoint 16 done -- §15 (import ordering) fully implemented, all
      §2-15 sections now complete.** New `Config.java` keys `js-import-order`
      (default `builtin, third-party, local`), `js-import-sort` (default
      `on`), `js-import-blank-lines` (default `1`) -- no `js-import-depth`
      (JS/TS has no `package`-declaration-derived local prefix concept to
      read a depth from, unlike Java/Kotlin). New
      `JsTsSpecificRule.enforceImportOrdering` (`isJs || isTs`-gated),
      structurally modeled on `JavaSpecificRule.enforceImportOrdering`
      (fixed 3-key bucket set `{builtin, third-party, local}`, `groupOrder`
      permutation validation, alphabetical-within-group sort, `blankLines +
      1` newline separation between non-empty groups) but re-emits each
      import declaration's **original token span verbatim** rather than
      canonically regenerating its text -- JS/TS import clauses (named-list
      braces, `type` modifier, default+namespace combinations) have enough
      internal shape variety that preserving original spacing was judged
      safer than reconstructing it, unlike Java's simpler dotted-path
      canonical-regen. Classification (RDD_KEY_195's resolved priority
      order): `node:`-prefixed or leading path segment matches a
      41-entry `NODE_BUILTIN_MODULES` list -> `builtin`; `./`/`../`-prefixed
      -> `local`; everything else -> `third-party` (including
      bundler/tsconfig-`baseUrl`/`paths`-style absolute first-party imports
      such as `"components/Widget"`, the accepted misclassification
      RDD_KEY_195 documents). Only real top-level `import` declarations are
      recognized -- a dynamic `import(...)` call or `import.meta` is
      detected via a same-line lookahead (`(` or `.` immediately after
      `import`) and left completely untouched, never even inspected for
      reordering. `export ... from "...";` re-export statements are out of
      this pass's scope entirely (STYLE_JS_TS.md §15's own worked example
      only shows `import`). Wired into `FormatterCurly` Phase 5 (file-header
      -level structure), as a new `else if (lang.isJs || lang.isTs)` branch
      alongside Java's/Kotlin's own import-ordering calls.
      **Real bug found and fixed as a prerequisite, not scoped to §15
      originally:** `JsTsSpecificRule.classifyBraces` (§2's semicolon-
      insertion brace classifier) had no case for a named-import list's `{
      ... }` (e.g. `import { Widget } from "...";`) -- its `{` follows the
      `import` KEYWORD, which wasn't in the existing value/pattern-brace
      KEYWORD disjunct (`return`/`yield`/`throw`/`typeof`/`const`/`let`/
      `var`), so it fell through to the "statement body" default: depth
      reset to 0 inside the braces, and the last specifier before `}`
      wrongly got a bogus `;` inserted (`import { Widget; } from "...";`),
      which then corrupted `parseJsImportStatement`'s scan (the injected `;`
      terminated the statement early, before the `from "path"` clause was
      ever reached, so no path `STRING` token was found and the entire
      ordering pass silently bailed on any file containing a named-import
      list). Fixed with a new `isImportBraceHeader` special case in
      `classifyBraces`, checked before the generic `isValue` logic: `{`
      immediately preceded by the `import` keyword (or by `type` where the
      token before `type` is `import`, covering TS's `import type { Foo }
      from "...";`) gets `resetDepth = false` (its comma-separated specifier
      interior is never a statement list) and `needsSemicolon = false` (its
      own closing `}` is never a statement's semicolon-needing tail -- the
      real terminator is after the following `from "path"` clause). New
      helper `isKeywordAt` (bounds-checked KEYWORD-at-index test) supports
      the `import type {` two-token lookback. Verified this fix doesn't
      regress §2's own destructuring-pattern coverage (Checkpoints 5/6) --
      `const { a, b } = obj;` still renders correctly, since its `{` is
      preceded by `const`/`let`/`var`, a completely different branch than
      the new `import`/`import type` case.
      Verified via a standalone harness (`FormatterCore.forLanguage("js")`/
      `"ts"`): a scrambled mix of built-in (`fs`, `node:path`), third-party
      (`express`, `lodash`), and local (`../components`, `./helper`)
      imports correctly groups into the default `builtin, third-party,
      local` order with one blank line between each non-empty group and
      alphabetical sort within each group; every statement-shape variant
      (default import, namespace `import * as ns`, named-list, combined
      default+named, side-effect-only `import "./polyfill";`, TS `import
      type { Foo } from "./types";`) round-trips correctly and participates
      in grouping/sorting; `fs/promises` (built-in submodule, no `node:`
      prefix) correctly classifies `builtin` via its leading `fs` segment;
      a bare absolute-style specifier `"components/Widget"` correctly
      classifies `third-party`, confirming the accepted RDD_KEY_195
      limitation; a dynamic `import("./dynamic")` call and `import.meta.url`
      inside a function body are both left completely untouched and don't
      interfere with the two real static imports above them being grouped
      correctly; a trailing line comment immediately after one import
      statement (`import fs from "fs"; // comment`) correctly blocks the
      entire pass (order left unchanged, comment never dropped), matching
      the "never guess past an unrecognized shape" posture used throughout
      this codebase. Round-trip (harness round1 -> round2) confirmed
      idempotent on every case above. `make` compiles clean; `make test`:
      106/106 forward + 106/106 idempotency, zero regressions.
      `STATE_COMMON.md`'s Config Keys and Defaults table and `README.md`'s
      config table both updated with the new JS/TS section; `README.md`
      also gained a new "JS/TS import groups and local-import
      classification" subsection (no `js-import-depth` key, unlike
      Java/Kotlin) and a new Known Limitations entry documenting the
      RDD_KEY_195 bundler/tsconfig-path-mapping misclassification.
      **§2-15 rule-by-rule checklist item is now fully complete** -- every
      section from §2 through §15 has landed across Checkpoints 3-16. No
      local `.js`/`.ts` fixture pair was registered live in the Makefile
      this checkpoint (out of scope -- verification was via the standalone
      harness only, per the task's own instructions); the next checklist
      item below (real-code testing pass) and un-commenting/verifying the
      existing local fixture pairs remain open follow-up work, along with
      the separate later task of flipping `Lang.isScaffoldOnly` for `js`/
      `ts` once that follow-up lands.
- [x] Author local test fixture pairs per `FUTURE_TEST_FIXTURES.md`'s
      "JavaScript"/"TypeScript" sections (split by extension since TS-only
      constructs can't live in `.js`). Done: `js_combined_inp/out.js`,
      `js_comments_inp/out.js`, `ts_combined_inp/out.ts`,
      `ts_comments_inp/out.ts` extracted to `test/`, registered
      commented-out in the Makefile (real logic not yet implemented),
      documented in `test/README.txt`.
- [~] **BLOCKED (superseded — see Checkpoint 21 below) — attempted to
      verify/activate the four existing local `.js`/`.ts` fixture pairs
      against the real JAR this session (via the
      same standalone-harness technique used throughout this file's
      checkpoints), found real formatter bugs, not just stale hand-authored
      `_out` expectations.** All four pairs (`js_combined`, `js_comments`,
      `ts_combined`, `ts_comments`) produce output that diverges from their
      recorded `_out` files. Some divergences are indeed just stale
      expectations (e.g. `_out` files assume this formatter reindents
      continuation lines/unindented bodies from scratch — it deliberately
      does not, per this file's own "General scope-depth reindentation"
      architectural note in `STATE_COMMON.md`; the flush-left input in
      `ts_combined_inp.ts`'s `LongUnion`/`AnotherLongUnion` cases is
      correctly left untouched by the real JAR, but the `_out` file wrongly
      expects it reindented). But at least three genuine, previously
      unverified bugs were found via isolated minimal repros (not yet fixed,
      not scoped to this checkpoint):
      1. **Import named-list braces never get `{ }` padding.**
         `import {readFile} from "node:fs/promises";` stays tight, while an
         ordinary object literal (`const obj = {a: 1};` → `{ a : 1 }`) does
         get padded. Likely because Checkpoint 16's `isImportBraceHeader`
         special-case in `JsTsSpecificRule.classifyBraces` (added for §2's
         semicolon-insertion depth logic) has an unintended side effect on
         the shared complexity-padding pass, or the padding pass never
         visits an import-list brace at all — root cause not investigated.
      2. **Allman/K&R conversion is inconsistent depending on generics.**
         Minimal repro:
         ```
         function identity<T>(value: T): T {
             return value;
         }

         function plain(value) {
             return value;
         }
         ```
         formats to `identity` correctly staying K&R... no — actually
         **`identity<T>` stays K&R (wrong, should convert to Allman per §5,
         since it's a plain named function) while `plain(value)` DOES
         convert to Allman.** I.e. the presence of a generic type parameter
         list before the params `(` appears to suppress §5's Allman
         candidate-signal detection (`findHeaderCloseParen`'s backward walk
         likely doesn't expect a `<T>` between the function name and `(`).
         This is a real, previously-unverified gap in
         `JsTsSpecificRule.enforceMethodDefinitionAllmanBraceStyle` — every
         prior Checkpoint-11 verification case was non-generic.
      3. **Decorator-argument object literals don't get the same `{ }`
         padding as plain object literals.** `@Component({ selector: "..."
         })` stays tight (`@Component({selector: "..."})`) in the real JAR
         output, unlike a bare `const obj = {a: 1};`.
      A fourth oddity, not yet classified as bug-or-intentional: plain
      object-literal padding renders `{ a : 1, b : 2 }` (space *before* the
      colon too, `a :` not `a:`) — unclear if this is existing
      language-agnostic complexity-padding behavior shared with other
      curly languages (not JS/TS-specific, out of this job's scope to
      "fix" unilaterally) or itself a bug; not investigated this
      checkpoint.
      **Per `STATE_COMMON.md`'s ambiguity protocol, stopping here rather
      than attempting fixes for all of the above as an unplanned addition
      to a "verify existing fixtures" checkpoint** — each of the three
      numbered bugs above plausibly needs its own dedicated
      investigation+fix+regression-verification checkpoint (mirroring the
      granularity every other bug fix in this file's history got), and the
      `_out` files likely need a mix of "regenerate to match now-correct
      behavior" (for the reindentation-assumption cases) and "leave
      unregistered until the underlying bug is fixed" (for the three bugs
      above) — that classification work itself was not completed this
      checkpoint. No fixture was activated in the Makefile, no `_out` file
      was regenerated, no source file was modified this checkpoint. Next
      session should: (a) decide whether to fix bugs 1-3 above first (each
      probably small, isolated, single-checkpoint-sized fixes per this
      file's usual pattern) or accept/document current behavior and
      regenerate `_out` files to match instead, then (b) redo this
      fixture-verification pass once behavior is settled.
      **Bug 1 fixed — see Checkpoint 17. Bug 2 (generics suppressing Allman
      conversion) fixed — see Checkpoint 18. Bug 3 (decorator-argument,
      and more generally any call-argument, object-literal padding) fixed
      — see Checkpoint 19. The colon-padding oddity (`{ a : 1 }` — space
      before `:` too) fixed — see Checkpoint 20.** All four items are now
      resolved; the four local `.js`/`.ts` fixture pairs (`js_combined`,
      `js_comments`, `ts_combined`, `ts_comments`) are judged ready to be
      revisited for activation/uncommenting in the Makefile as the next
      checkpoint (see Checkpoint 20's own closing note).
- [x] **Checkpoint 17 done — bug 1 fixed: import named-list braces now get
      `{ }` padding, matching plain object-literal initializers.** Root
      cause was NOT in `JsTsSpecificRule.classifyBraces` (that method only
      feeds §2's semicolon-insertion depth logic, a separate concern) and
      NOT in the shared `ComplexityPaddingEvaluator`/`enforceComplexityPadding`
      pass (`MiscRuleCore.java`, STYLE.md §3.1) either — that pass only ever
      handles `(...)`/`[...]`, never `{...}`, confirmed by reading it in
      full. The actual mechanism that pads `const obj = {a: 1};` into
      `const obj = { a : 1 };` is a **different, STYLE.md §3.3** pass,
      `MiscRuleCore.enforceInitializerBraceSpacing` (wired into
      `FormatterCurly` Phase 4, called unconditionally for every curly
      language, confirmed via a standalone harness that a plain `.js` file
      exhibits the identical padding — this is not new JS/TS-specific logic
      today, it's the pre-existing brace-initializer-list padding pass
      C/C++/Java/Kotlin already had). Its `startsNewInit` signal is
      `isOp(lastSignificant, "=")` only — a brace-initializer must be
      directly preceded by `=`. An import's named-list `{` is preceded by
      the `import` keyword (or `import`/`,`-separated default-import name
      for the combined default+named shape), never `=`, so it never
      qualified as an initializer and was left completely untouched (the
      §15 checkpoint's own text already independently confirms imports are
      re-emitted "verbatim", consistent with this finding). Fixed with two
      additive `startsNewInit` disjuncts, both gated `lang.isJs ||
      lang.isTs` (zero effect on C/C++/Java/Kotlin — `import`/`type` aren't
      even lexed as JS/TS-flavored `KEYWORD` tokens for those languages,
      the tokenizer's per-language `KEYWORDS_*` sets are disjoint): (1) new
      helper `isImportBraceHeaderKeyword(lastSig, secondLastSig)` — true
      when `lastSig` is the `import` keyword itself, or `lastSig` is `type`
      whose own preceding token is `import` (TS's `import type { Foo } from
      "...";`); (2) a second disjunct at the call site (not folded into the
      helper, since it needs a third token of lookback) for the combined
      default+named shape (`import Widget, { a, b } from "...";`) — `{`
      preceded by `,` preceded by an IDENTIFIER preceded by the `import`
      keyword. `enforceInitializerBraceSpacing` previously only tracked a
      single `lastSignificant` token; added `secondLastSignificant`/
      `thirdLastSignificant` (updated in lockstep each iteration) to support
      both lookback depths. No change to `initStack`/`outermostStack`
      mechanics themselves — once `startsNewInit` is true for an import
      brace, the existing padding/comma-spacing logic applies unchanged and
      correctly (single specifier, multiple specifiers, comma spacing all
      "just work" once the brace is recognized as an initializer-shaped
      brace).
      **Verified Checkpoint 16's own fix stays intact (explicit regression
      check, not assumed):** `import { Widget } from "...";` still gets no
      bogus semicolon inserted before its `}` — confirmed via the
      standalone harness, since `classifyBraces`'s `isImportBraceHeader`
      (a completely separate method, untouched this checkpoint) still
      independently governs that.
      Verified via a standalone harness (`FormatterCore.forLanguage("js")`/
      `"ts"`): `import {readFile} from "node:fs/promises";` →
      `import { readFile } from "node:fs/promises";`; multi-specifier
      (`import {readFile, writeFile} from "...";`) → both specifiers padded
      with correct comma spacing; namespace import (`import * as ns from
      "./ns";`) and plain default import (`import Widget from "./widget";`)
      both correctly untouched (no brace at all to pad); combined
      default+named (`import Widget, {a, b} from "./widget";`) → `import
      Widget, { a, b } from "./widget";`; TS `import type {Foo} from
      "./types";` → `import type { Foo } from "./types";`; an
      already-correctly-padded import (`import { Widget } from
      "./widget";`) round-trips byte-for-byte unchanged (idempotency);
      side-effect-only `import "./polyfill";` (no braces) untouched. Bug 3
      (decorator-argument object-literal padding, `@Component({selector:
      "x"})`) explicitly re-confirmed **still broken** (out of this
      checkpoint's scope, not touched) — the decorator call's own `(...)`
      argument, whose interior `{...}` is preceded by `(`, not `=` or
      `import`, doesn't match either new disjunct, so it's unaffected by
      this fix either way, exactly as planned. Round-trip (harness round1
      → round2) confirmed idempotent on every case above. `make` compiles
      clean; `make test`: 106/106 forward + 106/106 idempotency, zero
      regressions in the existing C/C++/Java/Kotlin fixture corpus.
      **Not done this checkpoint, still open:** bug 2 (generics suppressing
      §5 Allman conversion) and bug 3 (decorator-argument object-literal
      padding) from the BLOCKED entry above, and the colon-padding oddity —
      each still needs its own dedicated checkpoint. No local `.js`/`.ts`
      fixture pair was activated/uncommented in the Makefile this
      checkpoint (explicitly out of scope per this session's own brief —
      that follow-up waits until all three bugs, and the colon-padding
      oddity, are resolved).
- [x] **Checkpoint 18 done -- bug 2 fixed: a generic type-parameter list
      (`<T>`) between a function/method name and its params `(` no longer
      suppresses §5's Allman candidate-signal detection.** Root cause,
      confirmed via a standalone harness before any code change: in
      `enforceMethodDefinitionAllmanBraceStyle`, the candidate check
      required the token immediately before the header's opening `(` to be
      an IDENTIFIER (the function/method name). For `identity<T>(value: T):
      T {`, the token immediately before `(` is `>` (the generic list's own
      closing bracket, tokenized as `TokenType.ANGLE_BRACKET_CLOSE` per the
      tokenizer's existing language-agnostic angle-bracket reclassification
      -- confirmed this is not a raw comparison-operator `>`), not an
      IDENTIFIER, so the whole candidate check failed and the header's `{`
      was left K&R untouched. Fixed narrowly in
      `enforceMethodDefinitionAllmanBraceStyle`: when the token immediately
      before `(` is `ANGLE_BRACKET_CLOSE`, a new local helper
      `matchAngleOpenBackward` (a direct duplicate of
      `BlockStructureRule.matchAngleOpenBackward`'s own local
      depth-counting scan over `ANGLE_BRACKET_OPEN`/`_CLOSE` tokens only --
      not shared/extracted, since that method is private to its own class,
      same "duplicate rather than risk touching a shared method" posture
      Checkpoint 7 used for `isValuePrecededBrace`) finds the matching `<`,
      then the name-identifier check is re-applied to whatever
      significant token precedes that `<` instead of the `>` itself. No
      change to how `<`/`>` are tokenized or reclassified anywhere --
      purely consumes the existing `ANGLE_BRACKET_OPEN`/`_CLOSE` token
      types, zero new lexing. Scoped entirely to
      `JsTsSpecificRule.enforceMethodDefinitionAllmanBraceStyle`'s own
      candidate-signal check; no other language's Allman/brace-style logic
      touched.
      Verified via a standalone harness (`FormatterCore.forLanguage("ts")`):
      the exact BLOCKED-entry repro (`identity<T>` + `plain`) -- both now
      correctly convert to Allman (previously `identity<T>` wrongly stayed
      K&R while `plain` correctly converted -- confirmed reproduced first,
      before the fix, via the same harness); a generic method inside a
      class (`render<T>(data: T): void {`) converts to Allman correctly
      inside a `class Box { ... } // class Box` wrapper; multiple generic
      type params (`function pair<A, B>(a: A, b: B): void {`) converts
      correctly (multi-param generic list, not just single-param); a plain
      non-generic function (`function plain2(value) {`) still converts
      correctly (regression check, unaffected by the new
      `ANGLE_BRACKET_CLOSE` branch since it never matches for a
      non-generic header); an already-Allman-formatted generic function
      (`function already<T>(value: T): T\n{ ... }`) round-trips unchanged
      (idempotent, no double-move). Bug 1's fix (Checkpoint 17, import
      named-list brace padding) reconfirmed untouched in the same harness
      run (`import {readFile} from "node:fs/promises";` still correctly
      pads to `import { readFile } from "node:fs/promises";` alongside the
      generic-function cases above, in one combined fixture). Round-trip
      (harness round1 -> round2) confirmed idempotent across the whole
      combined fixture, not just the single-case repros. `make` compiles
      clean; `make test`: 106/106 forward + 106/106 idempotency, zero
      regressions in the existing C/C++/Java/Kotlin/JS/TS-tokenizer-shared
      corpus.
      **Not done this checkpoint, still open:** bug 3 (decorator-argument
      object-literal padding) and the colon-padding oddity from the
      BLOCKED entry above -- each still needs its own dedicated checkpoint.
      No local `.js`/`.ts` fixture pair was activated/uncommented in the
      Makefile this checkpoint (still waiting on bug 3 and the
      colon-padding oddity, per the existing plan).
- [x] **Checkpoint 19 done -- bug 3 fixed: decorator-argument (and, more
      generally, any call-argument) object-literal padding now matches
      plain object-literal initializers.** Confirmed via a standalone
      harness before any code change that the gap is **not**
      decorator-specific: a plain call-argument object literal in ordinary
      JS, `foo({a: 1});`, also stayed tight (`foo({a: 1});`, no padding),
      identical to the decorator repro (`@Component({selector:
      "app-widget"})` stayed tight too) -- both fail for the same reason.
      Root cause, in `MiscRuleCore.enforceInitializerBraceSpacing` (the
      same §3.3 pass Checkpoint 17 fixed for import braces): its
      `startsNewInit` signal only recognized `{` preceded by `=`, by an
      import-header keyword, or by the combined-default-import `,` shape
      (both added in Checkpoint 17) -- `(` was never in the disjunct list
      at all, for any curly language, so **no** call-argument object
      literal (decorator argument or otherwise) was ever recognized as an
      initializer-shaped brace; the decorator case is just the single most
      common real-world instance of this generic gap, not a distinct
      mechanism.
      **Fix, scoped JS/TS-only (mirrors Checkpoint 17's own scoping
      choice, not language-agnostic):** added one more `startsNewInit`
      disjunct, `(lang.isJs || lang.isTs) && isPunct(lastSignificant,
      "(")` -- a `{` directly following `(` (no other significant token
      between them) now starts a fresh top-level initializer frame for
      JS/TS only. Chose the narrower, language-gated form over a
      language-agnostic one specifically because `enforceInitializerBraceSpacing`
      is a **shared base-class method** used unconditionally by every
      curly language: in Kotlin, a lambda passed as an explicit
      parenthesized call argument (`foo({ x -> x + 1 })`, valid though
      not the idiomatic trailing-lambda form) would have the exact same
      `(` immediately before `{` shape, and there is no existing
      precedent in this codebase for treating a Kotlin lambda body as an
      "initializer" for §3.3 padding purposes -- a language-agnostic
      fix risked silently changing Kotlin lambda-argument spacing with no
      test coverage backing that decision. C/C++/Java have no valid
      construct where `(` is directly followed by `{` as a call argument
      (an anonymous-class/lambda argument is always preceded by `new
      Type(...)`'s closing `)`, never bare `(`), so they were never at
      risk either way, but the Kotlin case alone was enough to prefer the
      narrower gate.
      Verified via a standalone harness (`FormatterCore.forLanguage("ts")`/
      `"js"`): the exact BLOCKED-entry repro, `@Component({selector:
      "app-widget"})` -> `@Component({ selector: "app-widget" })`; a
      multi-property decorator argument, `@Component({selector: "app-x",
      template: "y"})` -> both properties padded and comma-spaced
      correctly; a plain call-argument object literal in `.js`, `foo({a:
      1});` -> `foo({ a: 1 });`, confirming the fix is not
      decorator-specific; an already-correctly-padded decorator argument,
      `@Component({ selector: "already-ok" })`, round-trips byte-for-byte
      unchanged (idempotency). Also re-verified in the same harness run,
      combined into one fixture: Checkpoint 17's import-brace padding
      (`import {readFile} from "node:fs/promises";`, combined
      default+named `import Widget, {a, b} from "./widget";`) and
      Checkpoint 18's generics-suppressed-Allman fix (`identity<T>(value:
      T): T {` and a plain non-generic function) both still correct and
      unaffected by this checkpoint's change. A destructured function
      parameter, `function f({a, b}) {`, was also found to now pick up
      `{ }` padding (`function f({ a, b })`) as a side effect of the same
      new disjunct -- since `{` there is also directly preceded by `(` --
      judged a consistent, desirable side effect (not a regression) rather
      than a narrower shape to special-case out, since destructuring
      brace/bracket padding was already noted elsewhere in this file as
      "already free via STYLE.md §3.1's existing complexity-padding pass"
      for other destructuring positions, so a destructured *parameter*
      picking up the same padding-family treatment is consistent, not a
      new inconsistency. Round-trip (harness round1 -> round2) confirmed
      idempotent across the whole combined fixture. `make` compiles
      clean; `make test`: 106/106 forward + 106/106 idempotency, zero
      regressions in the existing C/C++/Java/Kotlin fixture corpus (the
      new disjunct is `lang.isJs || lang.isTs`-gated, so it cannot affect
      any other language's behavior -- re-verified by the full suite
      passing unchanged, not just reasoned about).
      **Still open after this checkpoint:** only the colon-padding oddity
      (`{ a : 1 }` -- space before `:` too) from the original BLOCKED
      entry remains unresolved; all three numbered bugs found while
      verifying the four local `.js`/`.ts` fixture pairs are now fixed.
      Per the existing plan, the four fixture pairs (`js_combined`,
      `js_comments`, `ts_combined`, `ts_comments`) remain **not**
      activated/uncommented in the Makefile this checkpoint -- that
      follow-up is still explicitly gated on the colon-padding oddity
      being resolved or accepted first, not attempted here.
- [x] **Checkpoint 20 done -- the colon-padding oddity fixed: a plain
      object-literal property colon (`{a: 1, b: 2}`) no longer picks up a
      space before the colon.** Reproduced first via a standalone harness
      before any code change: `const obj = {a: 1, b: 2};` rendered as
      `const obj = { a : 1, b : 2 };` (space before **and** after the colon)
      through the full `FormatterCore.forLanguage("js")` pipeline.
      **Root cause, confirmed by tracing rather than guessed:** NOT
      `JsTsSpecificRule.enforceTypeColonSpacing` (that pass is
      `lang.isTs`-gated and is a no-op for `.js`; the bug reproduces
      identically in a plain `.js` file, proving it independently) and NOT
      shared cross-language colon-handling in the sense the task worried
      about (Java/C++/Kotlin ternary `a ? b : c`, `case 1 :` labels, and
      `label:` statements were all directly tested via the same harness and
      found unaffected/pre-existing-as-is -- this is not a shared quirky
      convention this codebase already has for other languages). The actual
      mechanism: `DeclarationAlignmentRuleCore.renderTokens` (a
      family-agnostic base method used by every curly language, including
      `JsTsDeclarationAlignmentRule`'s own `renderTokens(r.initTokens)` call
      for a declaration's `= expr` initializer, Checkpoint 5's own
      machinery) has a completely generic `needsSpaceBetween`/`isTightToken`
      pair with **no colon-specific case at all** for any language -- a
      bare `:` simply isn't in `isTightToken`'s set, so the default fallback
      (space on both sides) applies universally. That default is *correct*
      for a ternary (`cond ? 1 : 2`, verified via harness to render
      correctly both before and after this fix) but *wrong* for an
      object-literal property colon, which needs tight-before/spaced-after.
      The bug only manifests when an object literal is rendered as a
      declaration's initializer (`const obj = {a: 1};`, going through this
      shared `renderTokens` join point) -- a call-argument or decorator-
      argument object literal (`foo({a: 1})`, `@Component({selector: "x"})`)
      is a completely different code path (`MiscRuleCore
      .enforceInitializerBraceSpacing`, Checkpoints 17/19's own pass, which
      only pads around `{`/`}`/`,` and never re-renders interior tokens
      pairwise) and was already confirmed correct (verified via harness
      before touching any code: `@Component({selector: "app-x", template:
      "y"})` already rendered `selector: "app-x"` correctly, tight-before,
      through that separate mechanism) -- the bug's blast radius is narrowly
      "object literal used as a declaration initializer" only, not "object
      literals in general".
      **Fix, scoped to `DeclarationAlignmentRuleCore.renderTokens`,
      `lang.isJs || lang.isTs`-gated (zero effect on any other language):**
      new private helper `computeJsObjectPropertyColons` -- a lightweight
      bracket-stack scan (`{`/`(`/`[` push a frame, matching close pops it)
      that classifies a `{` as an object-literal frame unless its
      immediately preceding significant token is an IDENTIFIER, `=>`, or `)`
      (a block body, not a literal) -- the very first token of an
      initializer (`prevSig == null`) is treated as an object literal,
      matching `= {...}`'s own shape. Within an object-literal frame, an
      `expectingKey` flag starts `true` right after `{` or a top-level `,`
      at that frame's own depth, and clears the first time a `:` is seen at
      that depth -- so only that first per-property colon is classified as
      tight-before; a ternary `:` inside the same value position (`{a: cond
      ? 1 : 2}`) is correctly left unclassified (`expectingKey` already
      `false` by the time it's reached) and keeps the default symmetric
      spacing. `renderTokens` itself now precomputes this identity-based
      `Set<Token>` once per call (empty/zero-cost for every non-JS/TS
      language) and skips the space-before check for any token in the set.
      **Explicitly confirmed NOT needed / correctly left alone:** a colon
      nested inside a paren/bracket frame within the initializer (e.g. a TS
      arrow-parameter type annotation, `{a: (x: number) => x}`) is
      classified `objFrame.peek() == false` (paren frame, not object-literal
      frame) so is left with default spacing by this pass -- verified this
      is not a regression, since `JsTsSpecificRule.enforceTypeColonSpacing`
      (Checkpoint 7's own TS-only flat pass, runs later in `FormatterCurly`
      Phase 4) already unconditionally normalizes any parameter-position
      colon to tight-before regardless of what this earlier grid-rendering
      phase produced, so the two passes compose correctly without needing
      this new logic to also handle that shape.
      Verified via a standalone harness (`FormatterCore.forLanguage("js")`/
      `"ts"`): the exact BLOCKED-entry repro (`const obj = {a: 1, b: 2};`)
      now renders `const obj = { a: 1, b: 2 };` (brace padding from the
      pre-existing §3.3 pass retained, colon spacing now correct); a single
      property (`{a: 1}`), a nested object-literal value (`{a: {b: 1}}` ->
      `{ a: { b: 1 } }`, both levels' colons correct), a shorthand property
      (`{a}`, no colon at all, untouched) all render correctly. Explicitly
      re-verified unaffected (byte-for-byte, per the task's own hard
      requirement): a top-level ternary in a declaration initializer
      (`let x = cond ? 1 : 2;`, both an already-correctly-spaced and a
      deliberately mis-spaced `cond?1:2` input, both correctly normalize to
      `cond ? 1 : 2`); a ternary *inside* an object-literal property value
      (`{a: cond ? 1 : 2, b: y ? 3 : 4}` -> both property colons tight-
      before, both ternary colons spaced-both-sides, correctly
      disambiguated in the same statement); destructuring rename
      (`const { a: renamed } = obj;`, unaffected -- this statement is
      excluded from the grid entirely per Checkpoints 5/6, never reaches
      this method); a decorator-argument / call-argument object literal
      (`@Component({selector: "app-x", template: "y"})`,
      `foo({a: 1})`, verified already-correct through the separate
      `enforceInitializerBraceSpacing` mechanism, confirmed still correct
      and untouched by this checkpoint's change since that's a different
      code path); a TS arrow-parameter-typed object-literal value
      (`{a: (x: number) => x}`, both the outer property colon and the inner
      parameter-type colon end up correctly spaced, composing with
      Checkpoint 7's later pass as expected); a multi-declarator group with
      object-literal initializers (`const a = {x: 1}; let bb = {y: 2};`,
      both declarators correctly grid-aligned with correct colon spacing in
      each initializer). Round-trip (harness round1 -> round2) confirmed
      idempotent on every case above. `make` compiles clean; `make test`:
      106/106 forward + 106/106 idempotency, zero regressions in the
      existing C/C++/Java/Kotlin fixture corpus (the new helper and its
      call site are both `lang.isJs || lang.isTs`-gated, so no other
      language's declaration/initializer rendering is affected -- reconfirmed
      by the full suite passing unchanged, not just reasoned about).
      **All four items from the original BLOCKED entry (bugs 1-3, plus this
      colon-padding oddity) are now resolved.** Per the existing plan, the
      four local `.js`/`.ts` fixture pairs (`js_combined`, `js_comments`,
      `ts_combined`, `ts_comments`) are judged **ready to be revisited for
      Makefile activation** as the next checkpoint -- that re-verification
      pass (re-running each fixture's actual JAR output against its
      recorded `_out` file, reconciling the already-known stale-expectation
      divergences the BLOCKED entry separately noted, e.g. the
      reindentation-assumption cases in `ts_combined_inp.ts`) was
      **explicitly not attempted this session** per this session's own
      scope (item 6 in the task brief) and remains the next session's first
      task.
- [~] **BLOCKED (Checkpoint 21) — re-attempted the four-fixture
      verification/activation pass, found the fixtures are still not ready
      to activate: a large set of additional real, previously-unverified
      bugs, well beyond Checkpoints 17-20's scope.** Built the standalone
      harness this task specified (`Harness.java`, default package,
      compiled against `target/classes`, calling `Config.resolve(path,
      emptyMap, InFileConfig.parse(original))` then
      `FormatterCore.forLanguage(lang).formatOne(...)` — same pattern
      `Main.formatStandalone` uses internally, `Lang.isScaffoldOnly` never
      touched) and ran all four fixture `_inp` files through it, diffing
      against their `_out` files. (Note: partway into this session the user
      manually corrected indentation mistakes in `test/js_combined_inp.js`,
      `test/js_comments_inp.js`, `test/ts_combined_inp.ts`,
      `test/ts_combined_out.ts`, `test/ts_comments_inp.ts`,
      `test/ts_comments_out.ts` that predated this checkpoint — the harness
      was re-run against the corrected files before any analysis below.)
      Every one of the four pairs still diverges from its `_out` file.
      **Confirmed as genuine stale-expectation cases (category (a), safe to
      regenerate once the real bugs below are fixed):** the
      `LongUnion`/`AnotherLongUnion` break-style continuation lines and
      `js_comments_inp.js`'s deliberately-over-indented `return merged;`
      line are correctly left byte-for-byte as originally written by the
      real JAR (this formatter's documented no-reindent-from-scratch
      behavior) — the `_out` files wrongly assume reindentation. **Not
      attempted as a fix or regeneration this checkpoint, because the
      divergences are dominated by genuine new bugs interleaved with these
      stale-expectation lines in the same files**, making a clean
      "just regenerate" pass unsafe without fixing the bugs first (doing so
      would bake incorrect behavior into the reference `_out` files).
      **New bugs found, cataloged, NOT fixed (each plausibly its own future
      checkpoint):**
      1. **Generator method `*` gets a wrongly-inserted space:**
         `*iterate() {` renders as `* iterate() {` end-to-end (both `.js`
         and `.ts`). No generator-method spacing rule exists in
         `JsTsSpecificRule.java` at all — likely a plain `*` (multiplication)
         binary-operator-style default-spacing rule from a shared
         token-joining pass firing on the leading `*` of a generator method
         header, which has no left operand. Reproduces in both fixture
         files.
      2. **Closing comments (`STYLE.md` §7/§12, `closing-comment-min-lines`)
         are missing from JS/TS method/class bodies that clearly exceed the
         default 5-line threshold.** `async load(...) { ... }` (an
         ~11-line body in `js_combined_inp.js`) gets no `// async load`
         trailing comment though `_out` expects one, and even the
         already-short `iterate()`/generator body's surrounding `class
         Widget { ... }` itself loses its own `// class Widget` closing
         comment in one of the two `.js` fixtures. Root cause not
         investigated — plausibly an ordering interaction between one of
         the many new Phase-1/Phase-4 JS/TS passes and the closing-comment
         pass, or a `classifyBraces`-style brace-kind misclassification for
         a class/method body that also participates in one of §2's newer
         special cases.
      3. **A spurious blank line is inserted immediately after a class's
         opening `{` (and, in one fixture, immediately before its closing
         `}`), not present in the input.** Reproduces in both `.js`
         fixtures. Not investigated — not a general blank-line-preservation
         bug, since interior blank lines the user actually wrote are
         preserved correctly elsewhere in the same files.
      4. **Comma spacing is not applied inside destructuring patterns or
         arrow-function parameter lists.** `const {id,name,...rest} = ...`
         stays comma-tight (expected `{ id, name, ...rest }`); `(a,b) =>
         a + b` and `(a,b=10) => a + b` both stay comma-tight too (expected
         `(a, b)`/`(a, b = 10)`). Plain call-argument commas elsewhere in
         the same corpus are not exhaustively re-checked this checkpoint, so
         it's not yet confirmed whether this is comma-spacing-in-general
         missing for JS/TS, or narrowly scoped to destructuring-LHS/arrow-
         parameter-list token runs (both of which bypass the ordinary
         declaration-alignment grid per Checkpoint 5's own documented
         gap, per RDD_KEY_182) — not investigated further.
      5. **`GetterSetterRuleCurly`'s empty-parens padding is inconsistent
         between a `static get`/`static set` pair and a plain (non-static)
         `get`/`set` pair in the same file.** `static get instanceCount()`
         renders with tight `()` (no padding at all), while the very next
         `get x()`/`set x(value)` pair in the same class correctly pads the
         empty-parens getter to match its sibling's width
         (`get x(     )`), matching Checkpoint 13/14's own documented,
         accepted padding convention. The `static` modifier prefix appears
         to be breaking the grouping/width-matching logic for that pair
         specifically. Not investigated further.
      6. **A one-liner getter body can render with a doubled trailing space
         before its closing `}`:** `get x(     ) { return this._x;  }`
         (two spaces before `}`) vs. the correct single space seen
         elsewhere in the same file. Not investigated — possibly related to
         bug 5 above (same accessor group), possibly independent.
      7. **TypeScript `interface`/`type`-alias/class-field member `:`
         colons are never spaced at all** (`id:string;`, `label:string;`,
         `private locale:string;`, `protected count:number;`,
         `cache:Map<string,number>`) — this is not merely "no grid alignment
         column" (already a documented, accepted gap per Checkpoint 7's own
         scope note), it is the complete absence of even flat tight-before/
         space-after normalization for these colon positions. Re-reading
         this file's own Scope section (line ~271-274) shows §14's "member
         `:` alignment" and the interface/class-field-colon cases were
         *assumed* free by analogy during the original §1 gap-survey pass,
         but no checkpoint from 3 through 20 ever actually implemented or
         verified them — `enforceTypeColonSpacing` (Checkpoint 7) is
         explicitly scoped to declarator/parameter/return-type colons only,
         and Checkpoint 7's own text explicitly says class-field colons were
         "deliberately NOT touched." Interface member colons and the class
         field colons in `ts_combined_inp.ts`/`ts_comments_inp.ts` are the
         same untouched shape. **This means §11.2 (class-field modifiers)
         and §14 (interface/type-alias member colon alignment) are each
         only partially implemented, not fully complete as the "§2-15
         checklist item is now fully complete" note under Checkpoint 16
         claimed** — that claim should be treated as inaccurate for these
         two sections specifically until a future checkpoint actually adds
         member-colon spacing/alignment for interface bodies, type-alias
         object shapes, and class fields.
      8. **Enum member formatting (§12) is substantially broken, not just
         unaligned:** `Pending  = 3,` renders as `Pending=3;` (no `=`
         spacing at all, AND the trailing separator is wrongly a semicolon
         instead of a comma — §12's own text is explicit: "no trailing
         `;`"), `Blue,` (last member before `}`) renders as `Blue;` (comma
         wrongly replaced by a semicolon), and there is no `=`-column
         alignment across `Active`/`Inactive`/`Pending` at all (each stays
         at its own original, unaligned spacing once the `=` bug above is
         also fixed). This strongly suggests §2's generic JS/TS
         semicolon-insertion pass (`enforceSemicolonInsertion`) is
         mis-firing inside an enum body (probably not recognizing an enum
         body as a comma-separated member list, the same class of
         "value vs. statement-list brace" classification bug Checkpoints
         5/6/16 each found and fixed for a different brace shape) and no
         `=`-alignment-grid pass for enum members was ever written (§12 was
         never given its own checkpoint anywhere in this file's history —
         confirmed by the checkpoint-by-checkpoint list above never
         mentioning "enum" as its own topic; it was only ever assumed
         "free" during the original gap-survey pass, same as bug 7's §14
         finding).
      9. **Import ordering/blank-line placement diverges from `_out` in
         `js_comments_inp.js`** in a way not yet root-caused: with `fs`
         (builtin), `debounce`-from-`lodash` (third-party, has a trailing
         comment), and `express` (third-party) as the three imports, the
         real JAR's output re-orders/re-blank-lines them differently than
         `_out` expects. Not investigated to determine whether this is a
         genuine ordering-pass bug or (per Checkpoint 16's own documented
         "a trailing line comment on an import blocks the entire pass"
         behavior) a correct, intentional bail-out whose `_out` file simply
         encodes a stale assumption — not disentangled this checkpoint.
      **Per `STATE_COMMON.md`'s ambiguity protocol, stopping here rather
      than attempting fixes for any of the nine items above.** This is
      substantially more than "small/obvious fixes" — items 7 and 8 in
      particular indicate two entire style-doc sections (§12, §14) were
      never actually implemented despite being checked off as complete, and
      items 1-6 and 9 are six more independent, previously-unverified bugs
      spread across brace-classification, comma-spacing, blank-line
      handling, closing comments, and getter/setter grouping. **No fixture
      was activated/uncommented in the Makefile this checkpoint, no `_out`
      file was regenerated, no source file was modified.** A future session
      should: (a) decide whether §12/§14 need to be implemented as their own
      dedicated checkpoints first (they are more "not yet done" than "buggy"
      given they were never actually built), (b) triage bugs 1-6 and 9 as
      their own small, isolated fix checkpoints in whatever order is
      convenient, then (c) redo this fixture-verification/activation pass
      once all of the above (plus the already-known stale reindentation
      expectations) are settled — at that point the `_out` files can likely
      be regenerated wholesale from the real JAR's now-correct output rather
      than hand-patched. The standalone harness written this session
      (`Harness.java`, not committed — lives only in the session's scratch
      directory — trivial to recreate: default-package class, `Config
      .resolve(path, Collections.emptyMap(), InFileConfig.parse(original))`
      then `FormatterCore.forLanguage(lang).formatOne(original,
      path.toString(), config, false)`, compiled with `javac -cp
      target/classes`) is the fastest way to re-verify each fix without
      needing the Makefile/CLI gate touched.
- [ ] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      `STYLE_JS_TS.md`'s listed test-fixture repos (`nodejs/node`,
      `expressjs/express`, `lodash/lodash`, `microsoft/TypeScript`,
      `angular/angular`, `nestjs/nest`, `vuejs/core`).
- [~] **BLOCKED — see "Blocking issue found while attempting to flip the JS
      scaffold gate" under Open Questions above.** An attempt this session to
      flip `Lang.isScaffoldOnly` for `js` and wire this follow-up's dispatch
      call was stopped before any of items (1)-(3) below were done, because
      the mandated pre-flip CLI smoke-test surfaced the declaration-alignment
      gap documented above; the `Lang.java` gate-flip edit was reverted, no
      code change was kept. **Follow-up once real JS/TS logic lands
      (cross-job note from
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
