# STATE_JS_TS.md — JavaScript / TypeScript JAR Support Tracker

Read `STATE_COMMON.md` first — it has the shared commit/ambiguity/testing
conventions this file assumes. `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` (the
other jobs' files) are NOT required reading for this one — only
`STATE_COMMON.md` is.

Full historical narrative (root-cause writeups for each resolved bug) lives
in `git log` for this directory, not duplicated here — this file tracks
current state only.

---

## Purpose

Real formatting logic for JavaScript/TypeScript, per `STYLE_JS_TS.md` (which
derives most rules from `STYLE_JAVA.md`/`STYLE_KOTLIN.md` given JS/TS's
C-family brace/paren/statement shape). Scaffold gate is flipped
(`Lang.isScaffoldOnly` no longer includes js/ts) and all §1–15 rules are
implemented in `JsTsSpecificRule.java` (+ `JsTsDeclarationAlignmentRule.java`
for the declaration-alignment grid), wired into `FormatterCurly`'s phase
pipeline. `make test`: 110/110 forward + 110/110 idempotency.

---

## Next Steps (work ordering, set by the user)

All previously-tracked implementation gaps (RDD_KEY_182/183, the
`GetterSetterRuleCurly` static/plain accessor padding bug, RDD_KEY_197
import-ordering rework, nested template-literal interpolation, the
`XmlSpecificRule` Config-threading TODO) are **resolved** — see Resolved
Design Decisions and Checklist below. Remaining work, in order:

1. ~~Activate `test/js_combined_inp/out.js` and `test/js_comments_inp/out.js`~~
   **DONE.** Both active in the Makefile, `make test` green. Three real bugs
   found and fixed along the way — see "Resolved this session" below.
2. Activate `test/ts_combined_inp/out.ts` and `test/ts_comments_inp/out.ts`
   in the Makefile, run `make test`, bug-fix whatever surfaces. **IN
   PROGRESS** — Makefile activation attempted, `make test` currently FAILS
   with a large diff. Root cause identified but not yet fixed: TS
   `interface`/`type`-literal member lists and class-field declarations are
   ASI-reliant in the fixture's input (no explicit `;`), same shape as the
   JS declaration bug just fixed, but `enforceSemicolonInsertion` only
   inserts semicolons for ordinary statements — it does not cover
   interface/type-literal property-signature members or bare class-field
   declarations, so the `;`-requiring passes downstream
   (`enforceInterfaceTypeAliasMemberColonAlignment`, class field colon
   spacing) bail out on the whole containing body. A second, narrower bug
   found in the same pass: `void` is listed in `CONTINUATION_KEYWORDS`
   (correct for the expression operator `void 0`) but wrongly also blocks
   semicolon insertion when `void` is a TS *type* keyword ending a
   function-type return position (`(id: string) => void`) — these are two
   different grammatical roles colliding on one token spelling. Additional
   surfaced (not yet triaged) diffs: multi-line union-type continuation
   indent, decorator+class-declaration same-line splitting, an oversized
   class name's closing `}` comment. The Makefile's TS activation lines are
   currently reverted back to commented-out pending this fix — re-activate
   as part of resolving this item.
3. Real-code testing pass (see Test-Fixture Repos below) — not started.

Rationale (user's own words): the JS/TS basics should be solid before
dogfooding — get this job to a genuinely stable baseline first, then move on
to Python3 (next language job in the rotation).

---

## Scope

`STYLE_JS_TS.md` covers latest ECMAScript (ES2024+) and latest TypeScript
(5.x), one shared file for both (TS is a syntactic superset of JS). **Out of
scope entirely** (not just deferred): JSX/TSX — see Open Questions below.

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

Compiler for dogfood test `node` and `tsc` needs:

```bash
export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules
export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
```

---

## Test Fixtures (Local)

Local dogfood pairs (distinct from the external-repo list above, which is
for corpus-scale validation) live in `formatter/test/` — see
`test/README.txt` for the pair list and what each covers. Pairs are split by
extension (`.js` vs. `.ts`), not shared, since TS-only constructs
(decorators, enums, generics, interfaces) can't live in a valid `.js` file.

`js_combined`/`js_comments`/`ts_combined`/`ts_comments` are authored but
**not yet activated** in the Makefile — see Next Steps 1–2. All other local
JS/TS fixtures (`ts_decl_grid_ext`, `js_getter_setter_asi`,
`js_import_ordering_comments`, `js_nested_template_literal`, etc.) are
active and passing.

---

## Open Questions

- **HTML5 needs its own dispatcher for `<style>`/other embedded formats
  beyond `<script>`.** `<script>` splicing (JS/TS dispatch, CDATA unwrap/
  rewrap, Config-threading) is done — see `XmlSpecificRule.
  renderScriptOrStyle`. Any further HTML5/embedded-format dispatcher work
  belongs to the Data Formats job (`STATE_DATA_FORMATS.md`), not this one.
- **JSX/TSX will need their own future dispatcher**, different from HTML5's
  case — JSX embeds tag syntax directly inside JS/TS expression position, a
  compound-language situation, not a same-file extension. `STYLE_JS_TS.md`
  puts JSX/TSX **out of scope entirely** (not merely deferred).

---

## Checklist

### Status by style-doc section

All items below are implemented in `JsTsSpecificRule.java` unless noted, and
wired into `FormatterCurly`'s phase pipeline (Phase 1 structural/brace,
Phase 4 flat spacing, Phase 5 import ordering). `make test`: 110/110 forward
+ 110/110 idempotency, zero regressions.

- **§1 Baseline-inherited rules** — DONE.
- **Tokenizer support** — DONE (`TokenizerCurly.java`: `KEYWORDS_JS`/`_TS`,
  `NAMED_CONSTRUCT_JS`/`_TS`, `=>`/`??=`/`??`, `emitTemplateLiteral()`).
- **§2 Semicolon insertion** — DONE (`enforceSemicolonInsertion`).
- **§3 Destructuring/spread** — DONE (`enforceSpreadRestSpacing` +
  declaration-grid join, RDD_KEY_182).
- **§4 Template literals** — DONE (`enforceTemplateLiteralInterpolationSpacing`,
  now recursive into nested backtick literals inside a `${...}` interior).
- **§5 Function/method Allman brace style** — DONE
  (`enforceMethodDefinitionAllmanBraceStyle`).
- **§6 Arrow functions** — DONE (`enforceArrowSpacing`,
  `enforceArrowFunctionParameterParens`).
- **§7 Optional chaining / nullish coalescing** — DONE
  (`enforceOptionalChainingSpacing`).
- **§8 Getter/setter accessors** — DONE, reuses `GetterSetterRuleCurly`
  (static/plain accessor-group padding bug fixed — see Checklist below).
  **Known gap:** a plain method with no return-type token (e.g.
  `isValid() {...}`) cannot join the same aligned group as `get`/`set`
  siblings (`mergeReturnTypeIntoCall` would need a redesign for this shape)
  — left ungrouped (correct, just unaligned), not attempted.
- **§9 Decorators** — DONE (`enforceDecoratorTightAtSpacing`,
  `enforceDecoratorOverflowCascade`).
- **§10 `async`/`await` spacing** — DONE, free (required syntax spacing).
- **§11 Type annotations** — DONE as flat passes (`enforceTypeColonSpacing`,
  `enforceUnionIntersectionSpacing`, `reorderClassFieldModifiers`); grid
  column integration folded into the declaration-alignment grid entry below.
- **§12 Enums** — DONE (`enforceEnumMemberFormatting`).
- **§13 Generics (`<T>`)** — assumed free via reused C++/Java
  generic-bracket-complexity handling; not independently re-verified.
- **§14 Interface / object-shaped `type`-alias member `:` alignment** —
  DONE (`enforceInterfaceTypeAliasMemberColonAlignment`, RDD_KEY_196).
- **§15 Import ordering** — DONE (`enforceImportOrdering`, RDD_KEY_195,
  RDD_KEY_197).
- **Declaration-alignment grid (`let`/`const`/`var`/`type`)** — DONE,
  including destructuring-pattern LHS (RDD_KEY_182) and `type X = ...` alias
  groups (RDD_KEY_183) via `JsTsDeclarationAlignmentRule`. Multi-declarator
  statements (`let a = 1, b = 2;`) deliberately stay unaligned — matches
  C++/Java's own existing behavior for the same shape, confirmed not a gap.
- **`GetterSetterRuleCurly` static-vs-plain accessor padding** — RESOLVED.
  JS/TS-only ASI-aware statement boundary added to `splitMembers` (gated on
  `lang.isJs || lang.isTs`, zero effect on other languages). Fixture:
  `test/js_getter_setter_asi_inp/out.js`.
- **Import-ordering comment handling (RDD_KEY_197)** — RESOLVED.
  `enforceImportOrdering` now folds a trailing same-line comment into its
  import's span and segments (rather than bails) at a standalone comment.
  Fixture: `test/js_import_ordering_comments_inp/out.js`.
- **`XmlSpecificRule` Config-threading** — RESOLVED. New 6-arg constructor
  threads the enclosing HTML file's real resolved `Config` into the spliced
  `<script>` path instead of a throwaway 4-field synthesis.
- **Real-code testing pass** — NOT started (see Test-Fixture Repos).

### Resolved this session (js_combined/js_comments activation)

- **Destructuring-pattern-with-internal-comment collapse bug** — a comment
  embedded inside a destructuring pattern (e.g. `{ id, // note\n name }`)
  was silently dropped on a second format pass, because `significantOnly()`
  strips comment tokens the same as whitespace, so
  `JsTsDeclarationAlignmentRule.parseDestructuringDeclaration`'s pattern scan
  never saw it. Fixed by scanning the raw (comment-bearing) statement
  tokens for a comment between the pattern's first/last tokens and bailing
  (leaving the statement's own multi-line form untouched) if found.
- **ASI-vs-declaration-alignment-grid phase-ordering bug** — a significant
  pipeline defect: `FormatterCurly.formatOne`'s Phase 0 ran the
  declaration-alignment grid pass *before* `enforceSemicolonInsertion`, so
  any ASI-reliant declaration (no explicit `;` in source) was invisible to
  `JsTsDeclarationAlignmentRule.parseDeclaration`'s hard requirement for a
  literal `;` token — it (and every row in its alignment group) silently
  fell back to raw, unformatted input. Fixed by moving
  `enforceSemicolonInsertion` to run before `ScopePipelineCurly.process()`
  instead of after it.
- **Array-destructuring `,`→`...` missing space** — `[first, second,
  ...others]` was rendering as `[first, second,... others]` (space
  misplaced from before `...` to after it), while the equivalent
  object-destructuring `{ ...rest }` rendered correctly. Root cause:
  `MiscRuleCore.parseAssignment` (the older, JS/TS-unaware §6 bare-
  assignment grouping pass) misparsed `const [first, second, ...others] =
  expr;` as a plain assignment — `const` (a KEYWORD) was accepted as an
  assignment target, and the following `[...]` was scanned as a subscript
  expression, not recognized as `JsTsDeclarationAlignmentRule`'s own
  destructuring shape. This let `applyAssignmentsPass` re-parse and re-
  splice a statement the declaration-alignment pass had *already* rendered
  correctly, using this class's own separate `renderTokens`/`isTightToken`
  (which treats `...` as tight on both sides, unlike
  `JsTsDeclarationAlignmentRule`'s JS/TS-aware "space before, tight after"
  rule). Object-destructuring was never affected — `{` isn't one of
  `parseAssignment`'s recognized LHS shapes, only `[` is. Fixed by adding a
  `const`/`let`/`var` bail-out to `parseAssignment`, mirroring the existing
  C++ `auto [a, b] = expr;` structured-binding bail-out.
- **`js_combined_out.js` fixture regenerated** — the fixture expected
  `const process = (data) => {...}` (a multi-line arrow-function
  initializer) to join the alignment grid with a padded `=`, but
  `JsTsDeclarationAlignmentRule`'s documented design deliberately excludes
  multi-line block/lambda initializers from the grid (same precedent as
  Kotlin). Confirmed with the user: keep the design, regenerate the
  fixture — done.

### Known false positives (no source change needed, fixture-only)

- A spurious-looking blank line after a class's opening `{` in older
  `.js` fixture drafts, and a doubled trailing space before a one-liner
  getter body's closing `}` — both confirmed correct, existing behavior
  (STYLE.md §7 named-construct blank line; `GetterSetterRuleCurly`'s
  group-width body padding), matching passing C++/Java/Kotlin fixtures
  byte-for-byte. Only the stale hand-authored `.js` draft fixtures were
  wrong — resolve by regenerating them during Next Steps 1.
