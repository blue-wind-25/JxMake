# STATE_JS_TS.md — JavaScript / TypeScript JAR Support Tracker

Read `STATE_COMMON.md` first (shared commit/ambiguity/testing conventions).
`STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` are not required reading for this job.

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
pipeline. `make test`: 126/126 forward + 126/126 idempotency.

---

## Next Steps (work ordering, set by the user)

All previously-tracked implementation gaps (RDD_KEY_182/183, the
`GetterSetterRuleCurly` static/plain accessor padding bug, RDD_KEY_197
import-ordering rework, nested template-literal interpolation, the
`XmlSpecificRule` Config-threading TODO) are **resolved** — see Resolved
Design Decisions and Checklist below. Remaining work, in order:

1. ~~Activate `test/js_combined_inp/out.js` and `test/js_comments_inp/out.js`~~
   **DONE.** Both active in Makefile, `make test` green. Three real bugs
   found/fixed — see "Resolved this session (js_combined/js_comments
   activation)" below.
2. ~~Activate `test/ts_combined_inp/out.ts` and `test/ts_comments_inp/out.ts`~~
   **DONE.** Both active in Makefile, `make test` green (114/114 forward +
   idempotency). Real bugs found/fixed: `GENERIC_SAFE_KEYWORDS` missing TS
   primitive type keywords (ASI-breaking angle-bracket misdetection),
   `parseEnumMembers` bailing instead of ending the value on a last-member
   depth-0 NEWLINE, no comma-spacing pass for generic type arguments, no
   continuation-indent pass for multi-line union/intersection `type` aliases
   (including one that unconditionally bailed on any comment in the RHS span
   rather than just frozen tokens), and no `:`/`=` alignment-grid pass for
   class fields at all. See "Resolved this session (ts_combined/ts_comments
   activation)" below for full detail.
3. ~~Real-code testing pass (see Test-Fixture Repos below)~~ **DONE** —
   `expressjs/express` dogfood run. See "Resolved this session
   (expressjs/express real-code testing)" below for full detail.

Rationale (user's own words): the JS/TS basics should be solid before
dogfooding — get this job to a genuinely stable baseline first, then move on
to Python3 (next language job in rotation).

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
export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules:~/mynpm/node_modules
export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
```

`LD_LIBRARY_PATH` is required on this system's `node` binary (built against a
newer libstdc++/glibc than the system default) — without it, `node` fails
immediately with `libstdc++.so.6: cannot open shared object file` /
`GLIBCXX_*`/`GLIBC_*` version-not-found errors, even though the binary and
`PATH` are otherwise correct. `NODE_PATH`'s second entry
(`~/mynpm/node_modules`) is needed because `npm install --prefix ~/mynpm
<pkg>` installs into `~/mynpm/node_modules` directly, not
`~/mynpm/lib/node_modules` (the prior single-entry `NODE_PATH` was wrong for
locally-`npm install`ed packages, only correct for the two globally-shipped
scoped packages already under `~/mynpm/lib/node_modules`).

---

## Test Fixtures (Local)

Local dogfood pairs (distinct from the external-repo list above, which is
for corpus-scale validation) live in `formatter/test/` — see
`test/README.txt` for the pair list and what each covers. Pairs are split by
extension (`.js` vs. `.ts`), not shared, since TS-only constructs
(decorators, enums, generics, interfaces) can't live in a valid `.js` file.

`js_combined`/`js_comments`/`ts_combined`/`ts_comments` are all now active
in the Makefile and passing (see Next Steps 1–2, both DONE). All other
local JS/TS fixtures (`ts_decl_grid_ext`, `js_getter_setter_asi`,
`js_import_ordering_comments`, `js_nested_template_literal`, etc.) are also
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
Phase 4 flat spacing, Phase 5 import ordering). `make test`: 126/126 forward
+ 126/126 idempotency, zero regressions.

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
  `enforceUnionIntersectionSpacing`, `reorderClassFieldModifiers`,
  `enforceClassFieldAlignmentGrid` for §11.2 class-field `:`/`=` alignment,
  `enforceUnionTypeContinuationIndent` for §11.1 multi-line union/
  intersection `type` alias continuation indent); grid column integration
  folded into the declaration-alignment grid entry below.
- **§12 Enums** — DONE (`enforceEnumMemberFormatting`).
- **§13 Generics (`<T>`)** — DONE. Bracket-complexity detection reused from
  C++/Java (`TokenizerCurly.reclassifyAngleBrackets`/
  `isGenericSafeToken`, extended this session with TS primitive type
  keywords — see below). Comma-spacing inside generic argument lists is its
  own dedicated pass, `enforceGenericArgumentCommaSpacing`.
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
- **Real-code testing pass** — DONE, `expressjs/express`. See "Resolved this
  session (expressjs/express real-code testing)" below.

### Resolved this session (js_combined/js_comments activation)

- **Destructuring-pattern-with-internal-comment collapse bug** — a comment
  inside a destructuring pattern (e.g. `{ id, // note\n name }`) was
  silently dropped on a second format pass, because `significantOnly()`
  strips comment tokens like whitespace, so
  `JsTsDeclarationAlignmentRule.parseDestructuringDeclaration`'s pattern
  scan never saw it. Fixed by scanning the raw (comment-bearing) statement
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
  ...others]` rendered as `[first, second,... others]` (space misplaced
  from before `...` to after it), while equivalent object-destructuring
  `{ ...rest }` rendered correctly. Root cause: `MiscRuleCore.
  parseAssignment` (the older, JS/TS-unaware §6 bare-assignment grouping
  pass) misparsed `const [first, second, ...others] = expr;` as a plain
  assignment — `const` (a KEYWORD) was accepted as an assignment target,
  and the following `[...]` was scanned as a subscript expression, not
  recognized as `JsTsDeclarationAlignmentRule`'s own destructuring shape.
  This let `applyAssignmentsPass` re-parse and re-splice a statement the
  declaration-alignment pass had *already* rendered correctly, using this
  class's own separate `renderTokens`/`isTightToken` (which treats `...`
  as tight on both sides, unlike `JsTsDeclarationAlignmentRule`'s JS/TS-
  aware "space before, tight after" rule). Object-destructuring was never
  affected — `{` isn't a `parseAssignment`-recognized LHS shape, only `[`
  is. Fixed by adding a `const`/`let`/`var` bail-out to `parseAssignment`,
  mirroring the existing C++ `auto [a, b] = expr;` structured-binding
  bail-out.
- **`js_combined_out.js` fixture regenerated** — the fixture expected
  `const process = (data) => {...}` (a multi-line arrow-function
  initializer) to join the alignment grid with a padded `=`, but
  `JsTsDeclarationAlignmentRule`'s documented design deliberately excludes
  multi-line block/lambda initializers from the grid (same precedent as
  Kotlin). Confirmed with the user: keep the design, regenerate the
  fixture — done.

### Resolved this session (ts_combined/ts_comments activation)

- **`Map<string,number>` ASI bug** — `TokenizerCurly.isGenericSafeToken`'s
  `GENERIC_SAFE_KEYWORDS` set didn't include TS primitive type keywords
  (`string`, `number`, `boolean`, `any`, `unknown`, `never`, `object`,
  `undefined`, `null`), which are tokenized as `KEYWORD` not `IDENTIFIER`.
  A second type argument in a generic argument list (e.g. `Map<string,
  number>`) invalidated the whole `<...>` open/close tracking via
  `invalidateAll` before the matching `>` was reached, leaving it a plain
  `OP` token instead of `ANGLE_BRACKET_CLOSE` — this broke
  `CONTINUATION_OPS`-based ASI logic downstream, which then thought the
  statement wasn't complete. Fixed by extending `GENERIC_SAFE_KEYWORDS`.
- **Enum last-member (no trailing comma) bug** — `parseEnumMembers`'s
  value-scan loop `return null`'d (bailing the whole enum) on any depth-0
  NEWLINE, treating it as an unsupported multi-line value expression. This
  is overwhelmingly the common last-member case (`Pending = 3\n}`, no
  trailing comma). Changed to `break` instead, ending the value there.
- **Generic-argument comma spacing** — no pass existed for spacing after
  `,` inside a generic argument list (`Map<string,number>` never got a
  space after the comma). Added `enforceGenericArgumentCommaSpacing`: flat
  scan tracking `angleDepth` via `ANGLE_BRACKET_OPEN`/`_CLOSE`, inserts a
  single space after `,` when `angleDepth > 0`.
- **Union-type continuation indent — new pass** —
  `JsTsDeclarationAlignmentRule.parseTypeAlias` deliberately bails on any
  multi-line initializer, so no existing pass re-indented continuation
  lines of a multi-line `type X = A | B | C;` alias at all. Added
  `enforceUnionTypeContinuationIndent` (+ `tryRewriteUnionTypeAlias`,
  `lineColumnOf`) as an entirely separate pass — column-aligns continuation
  lines under the RHS's first token, preserving break-before-operator vs.
  break-after-operator style. Found and fixed one bug in the new pass
  itself during `ts_comments` triage: the RHS depth-scan bailed
  unconditionally on ANY comment token found anywhere in the span,
  including a legitimate same-line trailing comment on an interior operand
  line (`SecondOptionName | // middle option`). Narrowed the bailout to
  frozen tokens only — a trailing comment on an operand line is safe to
  carry through as part of that line's rendered content.
- **Class-field `:`/`=` alignment grid — new feature, entirely
  unimplemented before this session.** Confirmed via reading
  `JsTsDeclarationAlignmentRule` (only handles `let`/`const`/`var`/`type`)
  and `enforceInterfaceTypeAliasMemberColonAlignment` (only `IFACE`/type-
  alias-object braces, never `CLASS`) that no pass touched class field
  declarations at all. Added `enforceClassFieldAlignmentGrid` (+
  `ClassField`, `rewriteClassFieldGroups`, `flushClassFieldGroup`,
  `tryParseClassField`, `skipTopLevelMember`, `blankLineBetween`,
  `lastFieldEnd`, `skipOneNewline`): parses runs of simple typed class
  fields (`[modifiers]* name[?|!]? : type [= init]? ;`) between blank-line
  or unrecognized-member boundaries into alignment groups, padding
  modifier-phrase/name/type columns per group, matching the existing
  interface/enum alignment convention (double-space before trailing
  comment, name padded to the widest name in the group even across an
  interspersed leading comment on another member). Two bugs found/fixed
  during implementation, both in `rewriteClassFieldGroups`:
  - Double-indentation on the first field of each group — the group-start
    raw-copy loop was copying WHITESPACE tokens (indentation) as well as
    NEWLINE tokens, duplicating the indent `flushClassFieldGroup` supplies
    itself. Fixed by only copying NEWLINE tokens.
  - Duplicate blank line before a group's first field when that field has
    a leading comment — the same raw-copy loop counted the newline *after*
    the leading comment as part of the "blank line before group"
    preservation, but `flushClassFieldGroup` already renders that comment
    with its own trailing newline. Fixed by tracking
    `leadingCommentsStartIdx` and stopping the raw-copy loop there instead
    of at the field's own start index.
- **Fixture-authoring corrections (not code bugs), `ts_combined_out.ts`:**
  confirmed via reading `BlockStructureRule.decideComment`'s `NAMED` case
  (an empty `{}` body never gets a closing comment, by design, regardless
  of name length) that `class Container<...> {}` was wrongly given a
  `// class Container` fixture expectation — removed. Confirmed via
  STYLE_JS_TS.md §9 (decorator+declaration splitting is purely
  line-length-driven) that `@Injectable() export class
  UserAuthenticationAndAuditLoggingServiceForEnterpriseApplications {}`
  (95 chars, under the 100-char limit) was wrongly split onto two lines in
  the fixture — merged back to one line. The `Widget` class's field block
  was also inconsistently authored unaligned (copied verbatim from
  STYLE_JS_TS.md's illustrative §11.2 example) while the sibling `Config`
  block was aligned — both fall under the same general alignment-grid rule
  with no stated exception, so `Widget` was updated to match.
- **Fixture-authoring corrections (not code bugs), `ts_comments_out.ts`:**
  confirmed via reading the pre-existing (not modified this session)
  `rewriteEnumBody` that the double-space-before-trailing-comment
  convention, and name-padding computed across the full group (even past
  an interspersed leading comment on another member), are the established,
  already-tested behavior — the fixture's single-space instances
  (`Red, //`, `id : string; //`, `Pending  = 3, //`) and its missing
  `Active   = 1,` padding were fixture mistakes, not gaps. Corrected to
  match. `Widget`'s class-field block needed the same alignment-grid
  update as `ts_combined_out.ts`'s.

### Resolved this session (expressjs/express real-code testing)

First real-code-testing pass for this job. Repo: `expressjs/express`
(shallow clone already present under `/tmp/express`, HEAD `ae6dd37`),
confirmed genuinely small (1.9M, 141 `.js` files total: `lib/` 6, `test/` 91,
plus `examples/`; 0 `.jsx`/`.tsx` files present, so no exclusion was
necessary). All 141 files processed (both `lib/` and `test/` in full, no
sampling needed given the corpus size).

Two real bugs found, both via `node --check` (not baseline crash, not
idempotency — the corrupted output stayed syntactically plausible enough on
a first glance that only Node's own parser caught it), both fixed in the
same session, combined into one fixture (`test/real_code_regressions_77_
inp/out.js`):

- **ASI leading-continuation-operator/comma bug** — `JsTsSpecificRule.
  maybeInsertSemicolon` only ever looked at the *previous* line's own
  trailing token (`CONTINUATION_OPS`) to decide whether a statement was
  still open; it never looked ahead to the *next* line's leading token. A
  method-chaining style with the operator leading the continuation line
  (`request(app)\n.get('/')\n.expect(...)`, ubiquitous in Express's own
  Mocha test suite) or a comma-first multi-declarator list (`var a = ...\n
  , b = ...`) both got a bogus `;` inserted mid-chain/mid-list, corrupting
  valid JS into a syntax error. Fixed by adding a new `LEADING_CONTINUATION_
  OPS` set (deliberately narrower than `CONTINUATION_OPS` — excludes `+`/
  `-`/etc. that have a legitimate unary/statement-leading use) plus a
  leading-`,` check, both consulted via a next-significant-token lookahead
  alongside the existing `|`/`&` union-type lookahead already there for
  RDD_KEY handling of §11.1.
- **No JS/TS regex-literal tokenizing at all** — confirmed by reading
  `TokenizerCurly`'s dispatch loop: every `/` that wasn't `//`/`/*` fell
  through to the generic operator scan (division), with zero regex-literal
  recognition. Usually harmless by coincidence (a regex with no `"`/`'`
  inside just re-renders as itself), but a real-world regex containing a
  `"` inside a bracketed character class (`/^(?:W\/)?"[^"]+"$/`, from
  `test/res.sendFile.js`'s ETag assertions) had its `"` mistaken for the
  start of a string literal, corrupting brace/paren/statement tracking for
  the rest of the enclosing statement (observed as a mis-wrapped, mis-joined
  multi-statement mess extending several lines past the regex itself).
  Fixed by adding `TokenizerCurly.emitRegexLiteral` (opaque `STRING`-typed
  token, same posture as `emitTemplateLiteral`; correctly treats an
  unescaped `/` inside `[...]` as non-terminating) and `isRegexLiteralAllowedHere`
  (classic regex-vs-division disambiguation: regex unless the previous
  significant token already completed a value — identifier/number/string/
  char, a closing `)`/`]`/`}`, `this`/`super`, or postfix `++`/`--`).

Final numbers (full 141-file corpus, both bugs fixed): forward pass zero
crashes/exceptions; round1→round2 `diff -r` empty (idempotent); `node
--check` 141/141 pass (was 93/141 failing before the fix — effectively the
entire corpus, since the comma-first import-list idiom alone appears in
nearly every `test/*.js` file). `require()`-based semantic check: went
further than the syntax-only fallback — `npm install --prefix <scratch>
express` (network available) supplied a real (if not exactly version-pinned)
dependency tree; `require('./index.js')` on the formatted `lib/express.js`
tree returned the expected function, `express()` produced a working `app`
object, and a real `app.get('/hello', ...)` + `app.listen()` + an actual
HTTP GET round-tripped correctly end-to-end. Also ran the formatted `test/`
files directly under `mocha` (not just `node --check`): a clean file with no
static-file-serving dependency (`test/req.host.js` + `test/req.hostname.js`
+ `test/Route.js`) passed 35/35: confirms the formatted `require()` graph
and Mocha harness genuinely execute, not just parse. `test/res.sendFile.js`
itself (the regex-bug file, post-fix) also loads and runs under Mocha
without any crash — 42 of its assertions do fail, but confirmed as an
environment limitation, not formatter-induced corruption: they're all
static-file-serving 404s, and the identical failure pattern reproduces
against the *unformatted* checkout once dependencies are up (this repo's
exact `package-lock.json`-pinned dependency versions aren't installed
offline; the generic unpinned `npm install express` pulled a different
`send`/`serve-static` version than this exact commit expects). Documented
here rather than silently omitted, per the task's honesty requirement.

### Resolved this session (nestjs/nest real-code testing, in progress)

Repo: `nestjs/nest` (fresh shallow clone, `/tmp/nest`, HEAD `7e6e313`). First
bug found and fixed:

- **`/**`-style JSDoc opener corruption in `MiscRuleCore.
  reformatMultiLineBlockComment`** — near-universal bug affecting every
  language sharing this class (JS/TS, Java, Kotlin, C/C++), not just JS/TS.
  `firstContent = rawLines[0].substring(2).trim()` assumed the opening marker
  is always exactly 2 chars (`/*`), but a JSDoc/Javadoc-style `/**` opener is
  3 chars — `substring(2)` left a stray `*` that got promoted to a fake
  first content line, rendering `/**` as `/*` followed by a spurious `* *`
  line on **every** multi-line `/** ... */` comment reformatted by this
  method (i.e. nearly all of them, in any curly-family language). Fixed by
  scanning forward from index 2 while the character is `*` to find the true
  marker end, capturing that whole marker (`/*`, `/**`, `/***`, etc.) and
  reusing it verbatim as the output's opening token instead of a hardcoded
  `/*` literal, so the original marker style (plain vs. JSDoc/Javadoc) is
  preserved. Confirmed fixed for Java too via a standalone smoke test.
  Existing local fixture `test/real_code_regressions_38_out.kt` (Kotlin
  job's fixture, already exercising this exact shape) updated to the
  corrected expected output. `make test`: 128/128 forward + 128/128
  idempotency, zero regressions.

Second bug found and fixed (via debug-print root-causing, not static analysis
alone — see below):

- **Dot+space corruption in `MiscRuleCurly.renderCallCandidate`'s
  `sigForRender` typed/untyped selection** — `options.provideInjectionTokensFrom`
  became `options. provideInjectionTokensFrom` when a multi-arg call whose
  every argument is a bare dotted member-access expression (no top-level
  comma of its own) got rejoined/rewrapped. Root-caused with debug prints at
  each `FormatterCurly` phase boundary plus inside `collapseTokensToOneLine`
  (not static analysis alone, per this file's methodology): the corruption
  was **not** in `collapseTokensToOneLine` at all (that method was called
  with the right input and produced the right output on the first
  `enforceCallLineBreaking` pass) — it was introduced by the *second*
  `enforceCallLineBreaking` pass (`FormatterCurly` re-runs it twice, see that
  file's own comments for why), which took an entirely different render path
  the first pass didn't: `parseSignature` misparsed the multi-arg call's
  argument list as a real C/C++/Java-style "type name" forward-declaration
  parameter list (type `options.`, name `provideInjectionTokensFrom`) — the
  exact same misparse class already known and guarded for Kotlin only (see
  `sigForRender`'s existing doc comment, RobotCoding `gui_frontend_android`
  bug), but the guard was never extended to JS/TS. The typed dropped/
  one-per-line render path then inserted a column-separator space between
  the bogus "type" and "name" tokens. Fixed by forcing `sigForRender` to
  `null` for JS/TS too (`(lang.isKotlin || lang.isJs || lang.isTs) ? null :
  sig`), same reasoning as Kotlin: neither language has a prototype-only
  forward-declaration shape (a JS/TS function declaration always has an
  immediate `{` body, already exempted earlier in the same method). Also
  hardened `collapseTokensToOneLine` itself (defense-in-depth, not the
  actual root cause here but a related exposure) to never insert a space
  immediately before/after a tight `.`/`?.` token even when the original
  source's line break happened to fall right at that dot. New fixture
  `test/real_code_regressions_81_{inp,out}.ts`. `make test`: 130/130 forward
  + 130/130 idempotency, zero regressions. Confirmed against all 5
  originally-reported nestjs/nest files (`configurable-module.builder.ts`,
  `middleware-module.ts`, `router-execution-context.ts`, `client-rmq.ts`,
  `kafka-reply-partition-assigner.ts`): round1→round2 diff now empty for
  this bug's shape (a separate, pre-existing, unrelated general-reindent
  idempotency gap — see this file's "Architectural TODOs" in
  `STATE_COMMON.md` — still produces indentation-only diff noise across the
  whole nest tree when no per-directory `.jxmake-code-formatter` config
  round-trips with the output; not this bug). `node --check` (Node 24, which
  strips TS types natively) passes on all 5 round1 outputs.

Third bug found and fixed:

- **Content duplication in `JsTsSpecificRule.enforceClassFieldAlignmentGrid`
  — nested `CLASS` braces not handled** — `packages/core/injector/module.ts`
  (`Module.createModuleReferenceType`, `return class extends ModuleRef {
  ... };` nested inside an outer class method) and
  `packages/core/middleware/builder.ts` (a nested class similarly) had
  entire method/constructor blocks duplicated in round2 that weren't present
  in round1. Root cause: `enforceClassFieldAlignmentGrid` collects every
  brace classified `CLASS` by `classBraceKind` into a flat `classOpens` list
  and sweeps through them with a single linear `cursor`, assuming every
  selected class span is disjoint -- but a nested anonymous class (or any
  class nested inside another) breaks that assumption. The outer class's own
  `rewriteClassFieldGroups` call already copies the entire inner class span
  through byte-for-byte (as an ordinary unrecognized member, via
  `skipTopLevelMember`), so the loop's later top-level entry for the inner
  class re-appended its content a second time *and* walked `cursor` backward
  to the inner class's own (earlier) `closeIdx` -- so the method's final
  raw-copy-to-EOF loop re-emitted everything from there to the true end of
  file a second time too. Fixed by filtering `classOpens` down to only the
  outermost class brace at each nesting level (a nested class's own fields
  simply don't get the alignment-grid treatment -- consistent with this
  method's existing conservative "only rewrite what's fully understood"
  posture, not a regression). New fixture
  `test/real_code_regressions_82_{inp,out}.ts`. `make test`: 131/131 forward
  + 131/131 idempotency, zero regressions. Confirmed against both originally
  -reported nestjs/nest files: round1→round2 diff now empty. `node --check`
  (Node 24) passes on both round1 outputs.

Fourth bug found and fixed:

- **Comment-continuation-indent drift / arbitrary-deep-indent corruption on
  an object-shaped `type X = { ... } & Y;` intersection alias** —
  `packages/core/inspector/interfaces/edge.interface.ts`. Root cause:
  `JsTsSpecificRule.enforceUnionTypeContinuationIndent` (STYLE_JS_TS.md
  §11.1's union/intersection continuation-line re-alignment) treats a
  `type NAME = ...;` RHS as eligible whenever it contains a depth-0 `|`/`&`
  and spans multiple physical lines, then re-indents *every* `NEWLINE` from
  the RHS's start through the terminating `;` to the RHS's own column —
  with no bracket-depth tracking at all. That's harmless for a plain
  multi-line union list (every `NEWLINE` genuinely is a top-level break),
  but an intersection whose left operand is a multi-line object-type
  literal (`{ ... } & Y`) has `NEWLINE`s nested many bracket-levels deep
  that belong to the object body's own already-correct indentation — those
  were force-reindented to the alias's RHS column too, blowing every member
  out to an arbitrarily deep column matching the `type NAME = ` prefix's
  length. A leading `/** ... */` JSDoc comment on one member is a single
  token, so only the `NEWLINE` immediately before it got corrupted this way
  on a first pass; its own interior continuation lines (untouched by this
  pass) drifted further out of sync only once a second re-format pass
  reindented the token before it again, compounding the mismatch — matching
  the originally observed "grows between round1 and round2" symptom. Fixed
  by tracking bracket depth in the re-indent loop itself and only
  re-indenting a `NEWLINE` found at the union/intersection's own top level
  (depth 0), leaving any `NEWLINE` nested inside a bracketed sub-shape
  (object-type literal, tuple, generic argument list, ...) completely
  untouched. New fixture `test/real_code_regressions_84_{inp,out}.ts`.
  `make test`: 133/133 forward + 133/133 idempotency, zero regressions.
  Confirmed against the originally-reported nestjs/nest file: round1→round2
  diff now empty. `node --check` (Node 24) passes on the round1 output.
  (Debug-print methodology used: dumped `text` right after
  `ScopePipelineCurly.process` — already correct 2-space indent there —
  and right after `enforceInterfaceTypeAliasMemberColonAlignment` — the
  type-alias body was still untouched at that point too, since
  `parseInterfaceMembers` bails on the embedded JSDoc comment/union-typed
  member — which pointed at a later pass; grepping for `&`/union handling
  led directly to `enforceUnionTypeContinuationIndent`.)

Fifth bug found and fixed (all four originally-reported bugs now resolved):

- **`join(...)` call-wrap/collapse non-idempotency** —
  `integration/repl/e2e/repl-process.spec.ts`'s `const localPackageResolver
  = join(workspaceRoot, 'integration/_support/register-local-packages.ts')`
  (exactly 100 characters collapsed -- right at `lineLengthLimit`). Root
  cause: `MiscRuleCurly.renderCallCandidate`'s multi-line-source
  (`containsNewline(paramsSlice)`) branch always preserved the call's
  original per-line argument grouping (Option 2) unconditionally, with no
  fits-check of its own -- unlike the sibling single-line branch a few
  lines below, which correctly returns Option 0 (no change) whenever the
  call already fits. A call an author (or a previous format pass) had
  wrapped stayed wrapped forever even once it easily fit back onto one
  line, while the same call written fresh on one line collapsed correctly
  -- the same logical call could settle into two different stable shapes
  depending purely on incidental prior formatting, and one sitting right at
  the boundary flipped between forms across repeated passes. Fixed by
  adding the same fits-check to this branch, scoped to JS/TS only
  (`sigForRender == null && (lang.isJs || lang.isTs)` -- widening to every
  language unconditionally regressed `real_code_regressions_1`/C++, since a
  plain call's `sigForRender` is `null` there too for the ordinary "not a
  real signature" reason, not a JS/TS-specific misparse signal): measures
  the actual tight single-line candidate text directly (building the real
  `(args)` candidate and its own prefix/suffix token ranges via
  `appendRange`, rather than reusing the sibling branch's loose
  whitespace-collapsing `collapseToOneLine` helper, which turns the
  newline that originally followed the call's own `(` into a phantom
  single space and overestimates length by up to 2 characters, wrongly
  disqualifying a call that truly fits), and collapses to one line
  whenever it fits -- dropping any dangling trailing empty argument group
  first (`splitTopLevelCommas`, unlike its `groupByOriginalLine` sibling,
  doesn't drop a trailing comma's empty tail itself, so a trailing-comma
  multi-line source would otherwise gain a spurious trailing `, ` before
  `)` once collapsed -- found and fixed via an intermediate `make test`
  regression in `real_code_regressions_81`, since fixture 81's own
  `getInjectionProviders(...)` call has a trailing comma). New fixture
  `test/real_code_regressions_85_{inp,out}.ts`. Also required updating
  `real_code_regressions_81_out.ts`'s own expected output — its
  `getInjectionProviders(...)` call now correctly collapses to one line
  too (it fits); the old expected shape had itself been an artifact of
  this same bug, baked in as "correct" only because Bug 4 didn't have a
  fixture yet at the time Bug 1 was fixed. `make test`: 134/134 forward +
  134/134 idempotency, zero regressions beyond the intentional
  `real_code_regressions_81_out.ts` update. Confirmed against the
  originally-reported nestjs/nest file: round1→round2 diff now empty.
  `node --check` (Node 24) passes on the round1 output.

### Resolved this session (vuejs/core real-code testing, in progress)

Repo: `vuejs/core` (fresh shallow clone, `/tmp/vue-core`, HEAD `b5f8518`). 514
`.ts`/`.js` files under `packages/`, `packages-private/`, `scripts/` (5
`.tsx` files under `packages-private/dts-test/` correctly excluded, out of
scope per Open Questions). Full-corpus round1 (forward pass): zero
crashes/errors, 514/514 files formatted. Round1→round2 idempotency check
found 20 files differing; one root cause found and fixed so far (affecting
all 20 -- see below), 15 files' diffs resolved by the fix, 5 remaining files
still non-idempotent for other, separate, not-yet-root-caused reasons (see
Open Questions below) -- this dogfood pass is **not yet complete**.

- **Leading multi-line block comment reindent non-idempotency** — FIXED.
  `JsTsSpecificRule`'s class-field alignment grid (`flushClassFieldGroup`),
  enum-member formatting (`rewriteEnumBody`), and interface/type-alias
  member alignment (`enforceInterfaceTypeAliasMemberColonAlignment`) all
  re-emitted a member's captured leading `/** ... */` comment verbatim,
  including whatever absolute indentation its continuation lines happened to
  carry at the *original* source depth (e.g. this repo's own 2-space
  convention) — never reindented to match the member's own re-rendered
  indent depth (this formatter's 4-space default). A first pass left the
  comment's continuation lines visually misaligned under the member; a
  second pass' general block-comment reindent then caught up, producing a
  stable-but-misaligned round1 and a differently-aligned round2. Fixed by
  adding `reindentLeadingComment` (strips each continuation line's existing
  leading whitespace and reconstructs `indentPrefix + " " + strippedLine`,
  static helper local to `JsTsSpecificRule.java`) and calling it at all
  three sites instead of emitting the captured comment text raw. New fixture
  `test/real_code_regressions_87_{inp,out}.ts`. `make test`: 136/136 forward
  + 136/136 idempotency, zero regressions. Confirmed fixed against 15 of the
  originally-affected `vuejs/core` files (`dep.ts`, `computed.ts`,
  `effect.ts`, `effectScope.ts`, `collectionHandlers.ts` [comment part only,
  see below], `parser.ts`, `transformElement.ts`, `vModel.ts`, `vSlot.ts`,
  `resolveType.ts`, `componentEmits.ts`, `componentPublicInstance.ts`,
  `rendererTemplateRef.ts`, `renderer.ts`, `vOn.ts`) via a targeted
  affected-files-only re-run (not yet a full-tree re-run — see Next Steps).

**Of the original 5 non-idempotent files, 4 fully resolved this session** (3
distinct new bugs found and fixed, all committed with fixtures):

- `packages/reactivity/src/collectionHandlers.ts` — FIXED. Root cause was
  NOT the general-reindent architectural gap (initial guess above was
  wrong) — `TokenizerCurly.GENERIC_SAFE_KEYWORDS` was missing TS primitive
  keywords `symbol`/`bigint`, and `isGenericSafeToken`'s OP case had no
  entry for `|`, both needed for a union type directly inside a generic
  argument list (`Record<string | symbol, Function | number>`) to keep the
  enclosing `<...>` reclassified as angle brackets rather than plain `<`/`>`
  OP tokens — see `real_code_regressions_88` fixture and commit 189118e.
- `packages/runtime-core/src/componentOptions.ts` — the interface-member
  alignment-grid symptom above was itself a downstream corruption artifact,
  not the real bug. Three distinct root causes chased down and fixed in
  sequence: (1) the same symbol/bigint/`|` bug as collectionHandlers.ts,
  above; (2) `JsTsDeclarationAlignmentRule.parseTypeAlias`'s generic-clause
  skip loop (`type MergedHook<T = () => void> = ...`) advanced past a
  type-parameter-default clause without capturing its tokens, silently
  deleting the whole `<...>` clause from the output; (3) `TokenizerCurly`'s
  tokenizer dispatch loop had an unconditional (not language-gated) `]`
  immediately-followed-by-`]` branch meant only for C++11 attribute closes,
  which fired for a TS mapped type's `{ [K in T[number]]?: unknown }` (an
  indexed-access type's own close immediately followed by the mapped-type
  bracket's close) and emitted an OP token instead of the ordinary PUNCT
  `emitCloseBracket()` path, desyncing `enforceSemicolonInsertion`'s `[`/`]`
  depth counter for the rest of the file. See `real_code_regressions_89`
  fixture and commit 453deef. **One narrower, still-open residual bug found
  in this same file during the full-corpus re-verification below** (a
  different, unrelated symptom) — see "Still open" below.
- `packages-private/dts-test/ref.test-d.ts` /
  `packages-private/dts-test/watch.test-d.ts` — FIXED.
  `JsTsSpecificRule.classifyBraces`'s `isValue` prev-token list had no entry
  for TS's union/intersection continuation operators `|`/`&`; an inline
  object type directly following one of them in a union type alias (`type
  Steps = { step: '1' } | { step: '2' }`) fell through to the "default to
  not a value" fallback, misclassifying that object type's `{` as a
  statement-body brace and resetting the ASI depth counter to 0 at a false
  statement boundary — corrupting every subsequent line's indentation in
  the enclosing scope, not just a semicolon defect. See
  `real_code_regressions_90` fixture and commit a6edd22.

**Full-corpus re-verification (all 514 files, not just the previously-
affected subset) done after the above 3 fixes landed:** round1 forward pass
clean (zero crashes/errors, 514/514 formatted); round1→round2 idempotency
re-check found **12 files still differing** — down from the original 20, but
not zero. `scripts/release.js` (the 5th originally-listed file) is one of
them; the other 11 are files whose diffs the comment-reindent fix (previous
session) and this session's 3 fixes both happened to fully resolve for
*most* but not *all* of their original symptoms.

**Still open — 2 distinct bug shapes, NOT the general-reindent architectural
gap, found via this full-corpus re-check, not yet fixed (root-caused but
budget-exhausted this session):**

- **`if( ... )` nested-call paren-padding order-dependency** — affects 11
  files: `parser.ts`, `transformElement.ts`, `vModel.ts`, `vSlot.ts`,
  `resolveType.ts`, `componentEmits.ts`, `componentOptions.ts`,
  `componentPublicInstance.ts`, `rendererTemplateRef.ts`, `renderer.ts`,
  `vOn.ts`. Shape: `if( !isReservedPrefix(key[0]) ) Object.defineProperty(\n
  ...\n);`. **Correction from this section's first-pass framing** (found by
  checking `ComplexityPaddingEvaluator.isLoose`'s own contract, `MiscRuleCore
  .enforceComplexityPadding`'s doc comment, and RDD_KEY_62 directly): a
  nested `(` *or* `[` anywhere inside a paren pair's content makes that pair
  "loose" per STYLE.md §3.1, applied universally by
  `enforceComplexityPadding` (not just to `if`/`while`/`for`/`switch`'s own
  condition parens, which is the narrower, keyword-anchored
  `enforceKeywordSpacing`/RDD_KEY_62 pass) -- so `isReservedPrefix(key[0])`'s
  own parens, containing `key[0]`'s `[`, are correctly loose,
  `isReservedPrefix( key[0] )`. **Round2's output is therefore the
  spec-correct one; round1 is the actual bug** (a real formatter defect --
  under-padding, not round2 over-padding as first written here). Confirmed
  the discrepancy is real and reproducible only in the *file* context, not
  a minimal single-line repro (`if( !isReservedPrefix(key[0]) ) foo()`
  already fully/correctly pads on round1, round1==round2) -- it appears
  specifically when the `if`'s own **consequent** is itself a multi-line
  call spanning several following lines (`warn(\n ...\n)` /
  `Object.defineProperty(\n ...\n)` / `queuePostRenderEffect(\n ...\n)`,
  etc.), suggesting `enforceComplexityPadding`'s scan somehow misses or
  skips this inner call's parens specifically in that context on the first
  pass -- root cause (which phase/exclusion is responsible) not yet
  isolated. (This is the same symptom previously flagged as "not
  reproducing" in this section's earlier triage -- it was masked at the
  time because the comment-reindent fix's targeted affected-files-only
  re-run happened to not exercise this specific shape; the full-corpus
  re-check surfaces it directly.)
- **`scripts/release.js` — call-wrap/collapse boundary miscalculation.**
  `enforceCallLineBreaking`'s JS/TS-only single-argument fits-check
  (`MiscRuleCurly.java`, the nestjs/nest `join(...)` fix,
  `real_code_regressions_85`) measures the candidate collapsed line's length
  *before* `JsTsDeclarationAlignmentRule`'s later column-alignment pass runs
  — for `const res = await fetch(\`...\`)`, the unaligned prefix `const res
  = ` measures exactly 100 chars (the default `lineLengthLimit`), so the
  fits-check passes and round1 leaves it collapsed... no, precisely
  reversed: round1 (declaration NOT yet alignment-padded when this
  particular call is first measured relative to its own group) computes a
  shorter length and does not collapse/does collapse differently than
  round2, where the preceding `const branch = ...` / following `const data
  = ...` sibling declarations' alignment-grid padding (`res    =` instead of
  `res =`) has already been applied, pushing the same candidate line past
  100 chars. Confirmed via direct measurement: unaligned `    const res =
  await fetch(...)...);` is exactly 100 chars (fits, collapses); the
  post-alignment-padded form is longer (no longer fits, would need to stay
  wrapped) — but the fits-check runs *before* alignment padding is applied,
  so its estimate doesn't account for padding a sibling declaration in the
  same alignment group will add. Real fix needs either running this
  fits-check after declaration-alignment, or having declaration-alignment
  itself account for calls that were fits-check-collapsed right at the
  boundary — genuine cross-pass-ordering fix, out of scope to attempt
  without dedicated follow-up given this session's remaining budget.

**Next steps for this dogfood pass:** root-cause deeper and fix the two
bugs above (both confirmed real formatter bugs, not the architectural gap),
re-run the full 514-file round1/round2 check until clean, then do the `tsc`
typecheck pass per this job's "Real-code testing methodology" step 5, before
marking `vuejs/core` dogfood DONE. This session's own full-corpus check
(round1 clean, round2 diff count 20 → 12) already ran; the `tsc` typecheck
pass has NOT yet been attempted this session (deferred until the remaining
12-file idempotency gap is closed, per this job's methodology ordering).

### Known false positives (no source change needed, fixture-only)

- A spurious-looking blank line after a class's opening `{` in older `.js`
  fixture drafts, and a doubled trailing space before a one-liner getter
  body's closing `}` — both confirmed correct, existing behavior
  (STYLE.md §7 named-construct blank line; `GetterSetterRuleCurly`'s
  group-width body padding), matching passing C++/Java/Kotlin fixtures
  byte-for-byte. Only the stale hand-authored `.js` draft fixtures were
  wrong — resolve by regenerating them during Next Steps 1.
