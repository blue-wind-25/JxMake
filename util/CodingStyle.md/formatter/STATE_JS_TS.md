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

Additional bugs found in the same dogfood pass, **not yet fixed**:
- Content duplication — `packages/core/injector/module.ts` and
  `packages/core/middleware/builder.ts` had entire method/constructor blocks
  duplicated in round2 that weren't present in round1.
- Comment-continuation-indent drift still reproduces in at least one file
  (`edge.interface.ts`) even at `indent-size=2`.
- `join(...)` call-wrap/collapse non-idempotency: a multi-line call wraps
  once, then a second pass collapses it back to one line (fits under 100
  chars) rather than settling into a stable form.

### Known false positives (no source change needed, fixture-only)

- A spurious-looking blank line after a class's opening `{` in older `.js`
  fixture drafts, and a doubled trailing space before a one-liner getter
  body's closing `}` — both confirmed correct, existing behavior
  (STYLE.md §7 named-construct blank line; `GetterSetterRuleCurly`'s
  group-width body padding), matching passing C++/Java/Kotlin fixtures
  byte-for-byte. Only the stale hand-authored `.js` draft fixtures were
  wrong — resolve by regenerating them during Next Steps 1.
