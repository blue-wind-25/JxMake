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

## Dogfood Output Validation

**`js_ts_content_diff.js`** — content-preservation checker for JS/TS,
modeled on `java_content_diff.java`/`kotlin_content_diff.java` but via the
TypeScript compiler API instead of javac/PSI. One script handles both `.js`
and `.ts` (`ts.createSourceFile` parses plain JS fine; same idiom as
`STATE_DATA_FORMATS.md`'s `css_content_diff.py`/`xml_content_diff.py`).
Parses original and formatted files to a real AST and compares: top-level
imports as an order-tolerant MULTISET (`js-import-order` legitimately
reorders them); every other top-level statement/declaration in original
relative order via leaf-token canonicalization (terminal tokens joined with
single spaces, whitespace collapsed, so alignment padding/reindent are never
flagged); comments as a MULTISET, whitespace-normalized and lowercased
(case-only diff is expected `normalize-comment-start-case` behavior, not a
bug).

**Gotcha (same shape as `kotlin_content_diff.java`'s PSI one):** TS's AST
doesn't attach comments as tree nodes, so they're recovered separately from
raw source via `ts.getLeadingCommentRanges`, scanned at every node's
`getFullStart()` (plus position 0 and EOF, for a comment before the first
token or trailing with nothing after), deduplicated by `[pos, end)` since
the same range can be reached from multiple scan points.

**`typescript` package version gotcha:** an unpinned `npm install --prefix
~/mynpm typescript` installed **`typescript@7.0.2`** (the native tsgo
rewrite), which exports only `version`/`versionMajorMinor` — no
`createSourceFile`/`getLeadingCommentRanges`. Re-installed pinned to
`typescript@5` (landed `5.9.3`), which has the full classic API. Verify
`typeof ts.createSourceFile === 'function'` before trusting a reinstall.

Exit 0 if content is preserved, 1 with a description of each mismatch
otherwise, 2 on usage error. No build step — plain `.js`, run directly:

```bash
export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules:~/mynpm/node_modules
export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
node tools/verifiers/js_ts_content_diff.js <original.(js|ts)> <formatted.(js|ts)>
```

Verified against hand-crafted pairs for both extensions: a good pair
(import-sort + reindent + comment recapitalization) passed clean for both
`.js`/`.ts`; a bad pair with a dropped statement flagged `non-import
top-level statement count changed` for both; a bad pair with a corrupted
(recapitalized *and* reworded) comment flagged the present/missing mismatch
for both. All six cases (3 pairs × 2 extensions) behaved as expected. 208
lines.

**Two further tolerances added, post-`lodash/lodash` dogfood (see that
pass's write-up below for the false-positive classes that motivated them):**

1. **`normalize-comment-end-period` tolerance** — `stripCommentDelims` now
   calls a new `stripSoleTrailingPeriod(t)` (strips one trailing `.` before
   trailing whitespace/the closing delimiter, leaving `..`/`...` or any
   non-trailing `.` alone) before the existing normalize+lowercase,
   mirroring `MiscRuleCore`'s own `stripSoleTrailingPeriod`/
   `stripSoleTrailingPeriodAcrossLines` formatter behavior.
2. **Single-statement block unwrapping (STYLE.md §10)** — `canonicalize`'s
   `walk` special-cases `ts.isBlock(n) && n.statements.length === 1`:
   recurses directly into that one statement, skipping `{`/`}`, so
   `if (x) foo();` and `if (x) { foo(); }` (generic over any `Block`, not
   `if`-specific) canonicalize identically. Blocks with 0 or 2+ statements
   are walked normally (braces included), so a real added/removed/changed
   statement is still caught.

**Third false-positive class**, found re-verifying against `lodash/lodash`
post-fix: `canonicalize`'s child-walk also pulled in `/** ... */` JSDoc text
directly (TS parses a JSDoc block comment as a real AST child, `ts.isJSDoc`,
even with no `@` tags), bypassing `collectComments`'s period-stripping/
lowercasing and still getting flagged. Fixed by skipping `ts.isJSDoc(n)`
nodes in `canonicalize`'s walk — already covered by `collectComments`.

Re-verified against all six original hand-crafted pairs plus two new pairs
per extension (trailing-period tolerance; brace-omission tolerance) — all
pass, both `.js` and `.ts`.

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
- **Real-code testing pass** — DONE for `expressjs/express`, `nestjs/nest`,
  `vuejs/core`, and `lodash/lodash`. See the "Resolved this session"/dogfood
  narrative sections below for each repo's detail.

### Resolved this session (js_combined/js_comments activation)

- **Destructuring-pattern-with-internal-comment collapse bug** — a comment
  inside a destructuring pattern (`{ id, // note\n name }`) was dropped on a
  second pass: `significantOnly()` strips comment tokens, so
  `parseDestructuringDeclaration`'s pattern scan never saw it. Fixed by
  scanning raw tokens for an interior comment and bailing (leaving
  multi-line form untouched) if found.
- **ASI-vs-declaration-alignment-grid phase-ordering bug** — `FormatterCurly
  .formatOne`'s Phase 0 ran the alignment-grid pass before
  `enforceSemicolonInsertion`, so an ASI-reliant declaration (no explicit
  `;`) was invisible to `parseDeclaration`'s hard `;` requirement and its
  whole alignment group fell back to raw input. Fixed by moving
  `enforceSemicolonInsertion` before `ScopePipelineCurly.process()`.
- **Array-destructuring `,`→`...` missing space** — `[first, second,
  ...others]` rendered as `[first, second,... others]`. Root cause:
  `MiscRuleCore.parseAssignment` (JS/TS-unaware) misparsed the destructuring
  declaration as a plain assignment and re-spliced it via its own
  `...`-tight-both-sides rule (object destructuring unaffected). Fixed by
  adding a `const`/`let`/`var` bail-out, mirroring C++'s `auto [a, b] =
  expr;` bail-out.
- **`js_combined_out.js` fixture regenerated** — it wrongly expected a
  multi-line arrow-function initializer to join the alignment grid;
  `JsTsDeclarationAlignmentRule` deliberately excludes multi-line block/
  lambda initializers (same precedent as Kotlin). Design kept, fixture
  regenerated.

### Resolved this session (ts_combined/ts_comments activation)

- **`Map<string,number>` ASI bug** — `GENERIC_SAFE_KEYWORDS` didn't include
  TS primitive type keywords (`string`/`number`/`boolean`/`any`/`unknown`/
  `never`/`object`/`undefined`/`null`, tokenized KEYWORD not IDENTIFIER), so
  a second type argument invalidated `<...>` tracking before the matching
  `>`, breaking downstream ASI logic. Fixed by extending
  `GENERIC_SAFE_KEYWORDS`.
- **Enum last-member (no trailing comma) bug** — `parseEnumMembers`'s
  value-scan loop bailed the whole enum on any depth-0 NEWLINE (the common
  last-member case). Changed to `break` instead.
- **Generic-argument comma spacing** — added
  `enforceGenericArgumentCommaSpacing` (flat scan tracking `angleDepth`);
  no such pass existed before.
- **Union-type continuation indent — new pass**, `enforceUnionType
  ContinuationIndent`: column-aligns continuation lines of a multi-line
  `type X = A | B | C;` alias (`parseTypeAlias` bails on multi-line
  initializers) under the RHS's first token. Bug fixed during triage: the
  RHS depth-scan bailed on any comment anywhere in the span, including a
  legitimate trailing comment on an interior operand line — narrowed the
  bailout to frozen tokens only.
- **Class-field `:`/`=` alignment grid — new feature**, previously
  unimplemented (neither `JsTsDeclarationAlignmentRule` nor `enforce
  InterfaceTypeAliasMemberColonAlignment` touched class fields). Added
  `enforceClassFieldAlignmentGrid`, parsing runs of simple typed class
  fields into alignment groups. Two bugs fixed in `rewriteClassFieldGroups`:
  double-indented first field of each group (raw-copy loop copied
  WHITESPACE tokens too — fixed to copy NEWLINE only); duplicate blank line
  before a group's first field with a leading comment (fixed via
  `leadingCommentsStartIdx` tracking).
- **Fixture-authoring corrections (not code bugs):** `ts_combined_out.ts` —
  an empty `{}` class body wrongly got a `// class Container` closing
  comment (removed); a 95-char decorator+class line was wrongly split
  under the 100-char limit (merged back); `Widget`'s field block aligned to
  match sibling `Config`. `ts_comments_out.ts` — confirmed against
  pre-existing `rewriteEnumBody` behavior that double-space-before-trailing-
  comment and group-wide name-padding are established behavior; fixture's
  single-space/missing-padding instances were mistakes, corrected; same
  `Widget` alignment fix applied.

### Resolved this session (expressjs/express real-code testing)

First real-code-testing pass for this job. Repo: `expressjs/express`
(`/tmp/express`, HEAD `ae6dd37`, 141 `.js` files: `lib/` 6, `test/` 91,
`examples/`; 0 `.jsx`/`.tsx`). All 141 processed in full.

Two bugs found via `node --check`, both fixed, combined into fixture
`test/real_code_regressions_77_inp/out.js`:

- **ASI leading-continuation-operator/comma bug** — `maybeInsertSemicolon`
  only checked the *previous* line's trailing token, never the *next*
  line's leading token. Method-chaining with the operator leading the
  continuation line (`request(app)\n.get('/')\n.expect(...)`, common in
  Express's Mocha suite) or a comma-first multi-declarator list both got a
  bogus `;` inserted mid-chain/mid-list. Fixed via a `LEADING_CONTINUATION_
  OPS` set (narrower than `CONTINUATION_OPS`, excludes unary-legal `+`/`-`)
  plus a leading-`,` check, both via next-significant-token lookahead.
- **No JS/TS regex-literal tokenizing at all** — every `/` not `//`/`/*`
  fell through to the division-operator scan; a regex containing `"` inside
  a bracketed character class (`/^(?:W\/)?"[^"]+"$/`, `test/res.sendFile.js`)
  had its `"` mistaken for a string start, corrupting brace/paren tracking
  for the rest of the statement. Fixed via `TokenizerCurly.emitRegexLiteral`
  (opaque STRING token, same posture as `emitTemplateLiteral`) +
  `isRegexLiteralAllowedHere` (regex-vs-division disambiguation).

Final (141-file corpus, both fixed): zero crashes; round1→round2 `diff -r`
empty; `node --check` 141/141 (was 93/141 failing pre-fix). Semantic check
beyond syntax: `require('./index.js')` on formatted `lib/express.js`
returned a working `express()` app with a real `app.get`/`app.listen`/HTTP
GET round-trip; formatted `test/` files under `mocha`: a dependency-clean
subset passed 35/35; `test/res.sendFile.js` post-fix loads and runs, 42
assertion failures confirmed an environment limitation (unpinned
`send`/`serve-static`, reproduces identically unformatted), not
formatter-induced.

### Resolved this session (nestjs/nest real-code testing, in progress)

Repo: `nestjs/nest` (`/tmp/nest`, HEAD `7e6e313`). Five bugs found and fixed:

1. **`/**`-style JSDoc opener corruption** (`MiscRuleCore.
   reformatMultiLineBlockComment`, universal across all curly-family
   languages) — `firstContent = rawLines[0].substring(2).trim()` assumed a
   2-char opening marker (`/*`), but JSDoc/Javadoc `/**` is 3 chars, so the
   stray `*` became a fake content line (`/**` rendered as `/*` + spurious
   `* *` line). Fixed by scanning forward from index 2 while char is `*` to
   capture the whole marker and reuse it verbatim. Confirmed fixed for Java
   too; Kotlin fixture `real_code_regressions_38_out.kt` updated. `make
   test`: 128/128 forward + idempotency.
2. **Dot+space corruption in `MiscRuleCurly.renderCallCandidate`'s
   `sigForRender` selection** — `options.provideInjectionTokensFrom` became
   `options. provideInjectionTokensFrom` when a multi-arg call whose every
   argument is a bare dotted member-access got rejoined/rewrapped. Root
   cause: the second `enforceCallLineBreaking` pass's `parseSignature`
   misparsed the arg list as a C/C++/Java forward-declaration parameter
   list (type `options.`, name `provideInjectionTokensFrom`) — same
   misparse class already guarded for Kotlin but not JS/TS. Fixed by
   forcing `sigForRender` to `null` for JS/TS too; also hardened
   `collapseTokensToOneLine` to never space around a tight `.`/`?.`.
   Fixture `real_code_regressions_81_{inp,out}.ts`. `make test`: 130/130.
   Confirmed against all 5 originally-reported files; `node --check`
   (Node 24) passes on all 5.
3. **Content duplication in `enforceClassFieldAlignmentGrid` — nested
   `CLASS` braces** — `packages/core/injector/module.ts` (nested `return
   class extends ModuleRef {...}`) and `.../middleware/builder.ts` had
   whole method/constructor blocks duplicated in round2. Root cause: a flat
   `classOpens` list swept with one linear `cursor` assumes disjoint spans;
   a nested class breaks that (outer rewrite already copies the inner span,
   the later top-level entry for the inner class re-appends it and walks
   `cursor` backward, causing the final raw-copy-to-EOF loop to double-emit).
   Fixed by filtering `classOpens` to only the outermost brace per nesting
   level (nested class fields get no alignment-grid treatment). Fixture
   `real_code_regressions_82_{inp,out}.ts`. `make test`: 131/131. Confirmed
   both files; `node --check` passes both round1 outputs.
4. **Comment-continuation-indent drift on an object-shaped `type X = {...}
   & Y;` intersection alias** — `packages/core/inspector/interfaces/
   edge.interface.ts`. Root cause: `enforceUnionTypeContinuationIndent`
   re-indented *every* NEWLINE in a multi-line `type NAME = ...;` RHS with
   no bracket-depth tracking — harmless for a plain union, but an
   intersection whose left operand is a multi-line object literal has
   NEWLINEs already correctly indented inside the object body, which got
   force-reindented too, ballooning to an arbitrary depth (compounding
   across passes). Fixed by tracking bracket depth, only reindenting a
   NEWLINE at the union/intersection's own top level (depth 0). Fixture
   `real_code_regressions_84_{inp,out}.ts`. `make test`: 133/133. Confirmed
   against the file; `node --check` passes.
5. **`join(...)` call-wrap/collapse non-idempotency** —
   `integration/repl/e2e/repl-process.spec.ts`'s `const localPackageResolver
   = join(workspaceRoot, '...')` (exactly 100 chars, right at
   `lineLengthLimit`). Root cause: `renderCallCandidate`'s multi-line-source
   branch always preserved original per-line argument grouping with no
   fits-check of its own (unlike its single-line sibling) — a once-wrapped
   call stayed wrapped even after it would fit on one line, flip-flopping
   at the boundary. Fixed by adding the same fits-check, JS/TS-scoped only
   (widening to all languages regressed `real_code_regressions_1`/C++):
   measures the tight single-line candidate directly and collapses when it
   fits, dropping any dangling trailing empty argument group first.
   Fixture `real_code_regressions_85_{inp,out}.ts`; also updated
   `real_code_regressions_81_out.ts` (its call now correctly collapses).
   `make test`: 134/134. Confirmed against the file; `node --check` passes
   round1 output.

### Resolved this session (vuejs/core real-code testing, in progress)

Repo: `vuejs/core` (`/tmp/vue-core`, HEAD `b5f8518`). 514 `.ts`/`.js` files
under `packages/`, `packages-private/`, `scripts/` (5 `.tsx` files under
`packages-private/dts-test/` correctly excluded, out of scope). Round1
forward pass: zero crashes/errors, 514/514 formatted. Round1→round2
idempotency found 20 files differing; one root cause (below) fixed 15 of
them, 5 remained non-idempotent for other reasons (see below). **Narrative
documents the pass in chronological order; see "`vuejs/core` dogfood pass —
DONE" further down for the final outcome.**

- **Leading multi-line block comment reindent non-idempotency** — FIXED.
  `JsTsSpecificRule`'s class-field alignment grid (`flushClassFieldGroup`),
  enum-member formatting (`rewriteEnumBody`), and interface/type-alias
  member alignment (`enforceInterfaceTypeAliasMemberColonAlignment`) all
  re-emitted a member's captured leading `/** ... */` comment verbatim at
  its *original* indentation (e.g. this repo's 2-space convention) instead
  of the member's re-rendered depth (formatter's 4-space default) — round1
  left it misaligned, a later general block-comment reindent caught up on
  round2, producing different stable outputs each round. Fixed via
  `reindentLeadingComment` (strips each continuation line's leading
  whitespace, reconstructs `indentPrefix + " " + strippedLine`) called at
  all three sites instead of emitting captured text raw. Fixture
  `real_code_regressions_87_{inp,out}.ts`. `make test`: 136/136. Confirmed
  against 15 of the originally-affected files (`dep.ts`, `computed.ts`,
  `effect.ts`, `effectScope.ts`, `collectionHandlers.ts` [comment part],
  `parser.ts`, `transformElement.ts`, `vModel.ts`, `vSlot.ts`,
  `resolveType.ts`, `componentEmits.ts`, `componentPublicInstance.ts`,
  `rendererTemplateRef.ts`, `renderer.ts`, `vOn.ts`) via targeted re-run.

**4 of the original 5 non-idempotent files fully resolved** (3 new bugs
found/fixed, all with fixtures):

- `packages/reactivity/src/collectionHandlers.ts` — FIXED (not the
  general-reindent gap as first guessed). `TokenizerCurly.GENERIC_SAFE_
  KEYWORDS` was missing TS primitives `symbol`/`bigint`, and
  `isGenericSafeToken`'s OP case had no entry for `|` — both needed for a
  union type inside a generic argument (`Record<string | symbol, Function |
  number>`) to keep the enclosing `<...>` reclassified as angle brackets.
  Fixture `real_code_regressions_88`, commit 189118e.
- `packages/runtime-core/src/componentOptions.ts` — three distinct root
  causes: (1) same symbol/bigint/`|` bug as above; (2)
  `JsTsDeclarationAlignmentRule.parseTypeAlias`'s generic-clause skip loop
  (`type MergedHook<T = () => void> = ...`) advanced past a type-parameter-
  default clause without capturing tokens, silently deleting the whole
  `<...>` clause; (3) `TokenizerCurly`'s dispatch loop had an unconditional
  `]`-followed-by-`]` branch meant only for C++11 attribute closes, which
  fired for a TS mapped type (`{ [K in T[number]]?: unknown }`) and emitted
  an OP token instead of `emitCloseBracket()`, desyncing
  `enforceSemicolonInsertion`'s `[`/`]` depth counter for the rest of the
  file. Fixture `real_code_regressions_89`, commit 453deef. **One narrower
  residual bug in this file found during full-corpus re-verification** —
  see "Still open" below.
- `packages-private/dts-test/ref.test-d.ts` / `watch.test-d.ts` — FIXED.
  `JsTsSpecificRule.classifyBraces`'s `isValue` prev-token list had no
  entry for TS union/intersection ops `|`/`&`; an inline object type
  following one (`type Steps = { step: '1' } | { step: '2' }`) fell through
  to "default not a value", misclassifying its `{` as a statement-body
  brace and resetting the ASI depth counter at a false boundary —
  corrupting indentation for the rest of the enclosing scope, not just a
  semicolon defect. Fixture `real_code_regressions_90`, commit a6edd22.

**Full-corpus re-verification** after the above 3 fixes: round1 clean
(514/514); round1→round2 found **12 files still differing** (down from 20).
`scripts/release.js` (the 5th originally-listed file) is one; the other 11
had diffs only *partially* resolved by the comment-reindent fix and this
session's 3 fixes.

**Still open — 2 distinct bug shapes, NOT the general-reindent architectural
gap, found via this full-corpus re-check, not yet fixed (root-caused but
budget-exhausted this session):**

- **`if( ... )` nested-call paren-padding order-dependency** — affects 11
  files: `parser.ts`, `transformElement.ts`, `vModel.ts`, `vSlot.ts`,
  `resolveType.ts`, `componentEmits.ts`, `componentOptions.ts`,
  `componentPublicInstance.ts`, `rendererTemplateRef.ts`, `renderer.ts`,
  `vOn.ts`. Shape: `if( !isReservedPrefix(key[0]) ) Object.defineProperty(\n
  ...\n);`. Per RDD_KEY_62: a nested `(`/`[` anywhere inside a paren pair
  makes it "loose" (STYLE.md §3.1), applied universally — so
  `isReservedPrefix(key[0])`'s parens should be loose,
  `isReservedPrefix( key[0] )`. **Round2 is spec-correct; round1 under-pads
  (the actual bug).** Reproducible only in file context (not a minimal
  single-line repro) — appears when the `if`'s consequent is itself a
  multi-line call, suggesting `enforceComplexityPadding`'s scan misses the
  inner call's parens in that context on round1. Root cause not yet
  isolated; masked earlier since the comment-reindent fix's affected-files
  re-run didn't exercise this shape.
- **`scripts/release.js` — call-wrap/collapse boundary miscalculation.**
  `enforceCallLineBreaking`'s JS/TS-only single-argument fits-check
  (`MiscRuleCurly.java`, the nestjs/nest `join(...)` fix,
  `real_code_regressions_85`) measures the candidate line length *before*
  `JsTsDeclarationAlignmentRule`'s later column-alignment pass runs. For
  `const res = await fetch(\`...\`)`: round1 measures the unaligned prefix
  (fits at exactly 100 chars, collapses); round2 measures after sibling
  declarations in its group have their `=` padded, pushing past 100 (no
  longer fits) — the fits-check doesn't account for padding a sibling will
  add. Real fix needs the fits-check to run after declaration-alignment, or
  alignment to account for fits-check-collapsed calls — cross-pass-ordering
  fix, out of scope without dedicated follow-up.

### `vuejs/core` dogfood pass — DONE

Continuing from the 12-file idempotency gap above, a later leg of this
session (driven primarily by the `tsc` typecheck pass — sharper signal than
byte diffing for locating generic-clause/statement-boundary corruption)
fixed the `if( ... )` nested-call paren-padding order-dependency and the
`scripts/release.js` call-wrap/collapse boundary bug above, plus nine
further bugs surfaced by running `tsc --noEmit` against the full
round1-formatted 514-file tree vs. the unmodified tree (0 errors) — every
new error line was root-caused as a real formatter defect and fixed:

1. `TokenizerCurly.GENERIC_SAFE_KEYWORDS` missing `true`/`false` (TS
   boolean-literal type keywords, tokenized as KEYWORD not IDENTIFIER) —
   `real_code_regressions_102`.
2. The `;`/`{`/`}` open-stack clear-all guard (added for the symbol/bigint
   fix, see `nestedBraceDepth`) firing on a legitimate nested `{}`
   object-type argument inside an already-tracked generic clause — same
   fix as item 1, `real_code_regressions_102`.
3. `GENERIC_SAFE_KEYWORDS` missing `keyof`/`is`/`infer`/`asserts`/
   `readonly`/`unique`/`as`/`satisfies` (TS type-operator keywords legal
   directly inside a generic argument list / conditional type) —
   `real_code_regressions_101`.
4. A parenthesized ternary sub-expression's `:` misclassified as a
   return-type colon since the preceding `)` closes a plain grouping paren,
   not a real signature — new `isGroupingExpressionParen` helper in
   `JsTsSpecificRule.isTypeColonAt` — `real_code_regressions_105`.
5. `typeof`/`keyof` as `prevPrev` not recognized by
   `enforceArrowFunctionParameterParens`'s bail-out check (only `is` was),
   wrongly wrapping a type-predicate identifier (`key is keyof typeof val
   => ...`) — `real_code_regressions_105`.
6. A trailing type-annotation `:` wrapping to the next line got a bogus `;`
   inserted after it — `needsSemicolonAfter`'s intended `isPunct(t, ":")`
   guard never matched (`:` tokenizes as OP); fixed by adding `":"` to
   `CONTINUATION_OPS` — `real_code_regressions_105`.
7. `isGenericSafeToken`'s TS-safe OP list missing `=>`/`...` (a function-type
   generic argument like `Map<(...args: any[]) => void, Handler>` lost its
   outer `<...>` tracking); `...` gated `lang.isTs`-only after it regressed
   C++ test 53's variadic-template spacing (`Args...`) when added
   unconditionally — `real_code_regressions_105`.
8. A standalone TS function-type parameter list (`(...args: any[]) => void`)
   got padded/tightened like an arbitrary grouping paren by `MiscRuleCore.
   enforceComplexityPadding`'s generic non-identifier-preceded `(` branch;
   fixed with a `lang.isTs`-gated exception recognizing a matching `)`
   immediately followed by `=>` — `real_code_regressions_105`.
9. `nestedBraceDepth`'s guard (item 2) only covered the nested `{`/`}`
   delimiters, not tokens inside them — `Record<string, { local: string;
   default?: Expression }>`'s member name `default` (a KEYWORD not in
   `GENERIC_SAFE_KEYWORDS`) and its `;` separator still reached the outer
   `<...>`'s invalidation checks and wiped it; also needed
   `ANGLE_BRACKET_OPEN` added to `classifyBraces`'s `isValue` whitelist for
   a mapped-type object as a generic argument — `real_code_regressions_105`.
10. `GENERIC_SAFE_KEYWORDS` missing `typeof` — found on the final full-corpus
    tsc rerun. `Record<(typeof identityMethods)[number], any>` produced a
    bogus `;` before the closing `>`; worse, `let server:
    ReturnType<typeof createServer>` lost `<...>` tracking entirely (`>`
    became a plain OP token), defeating `enforceSemicolonInsertion`'s
    statement-boundary detection and merging the following
    `beforeAll(...)` onto the same line — `real_code_regressions_107`.

**Final verification** (clean build, `make test`: 156/156 forward + 156/156
idempotency): full 514-file round1 forward pass — zero crashes/errors.
Round1→round2 idempotency — **only one file still differs**,
`packages/compiler-sfc/src/script/utils.ts`, a switch-case fallthrough
(consecutive `case` labels sharing one body) non-idempotency **confirmed
pre-existing on the unmodified codebase** (verified via `git stash`/
rebuild/retest/pop — original code produces a different but equally-broken
symptom, `case 'StringLiteral':;` with a bogus semicolon and lost
blank-line collapsing; this session's code instead shrinks one space per
round-trip). **Not fixed** as part of this pass — tracked below. `tsc
--noEmit` on the round1-formatted tree: **1 error** (same file/line, same
pre-existing switch-case bug; 0 on the unmodified tree). No other file
shows a new formatter-induced tsc error.

**Dogfood pass verdict: DONE.** All formatter bugs found this session (13
total: 2 original comment-reindent-era fixes + 2 `if`-padding/`release.js`
fixes + the 9-item list above) are fixed and covered by permanent fixtures.
The one remaining idempotency/tsc gap (`utils.ts` switch-case fallthrough)
is confirmed pre-existing, not a regression, out of scope — see "Known open
issues" below, plus a second, separately-discovered spacing-only gap in the
single-declarator colon path.

### Known open issues (pre-existing, deferred — not part of `vuejs/core` DONE scope)

- **`utils.ts` switch-case fallthrough non-idempotency** (see above) —
  root-caused (case-label-fallthrough one-liner-collapse/alignment feature
  interacting badly with consecutive `case` labels sharing one body) but
  confirmed pre-existing via `git stash` comparison, not introduced this
  session. Needs its own dedicated fix + fixture. **Second confirming
  recurrence in the `lodash/lodash` dogfood pass** (below): a related
  symptom in `fp/_baseConvert.js`'s `initCloneByTag` typed-array fallthrough
  case body (196 chars) — round1 leaves it unwrapped past
  `line-length-limit`, round2 wraps the trailing call's arguments — a
  `SwitchRule` case-grid vs. generic call-wrap-fits-check ordering gap.
  Still not fixed (shared C/C++/Java-owned `SwitchRule` logic, deliberately
  left to a future session).

  **2026-07-28 cleanup-pass re-assessment:** re-checked against
  `STATE_C_CPP_JAVA.md`'s "Known Gaps" section — no fix landed there since
  either, and the root cause (case-label-fallthrough collapse/alignment vs.
  a generic call-wrap fits-check ordering gap) is a `SwitchRule` change,
  same risk class as the reindentation architectural gap this cleanup pass
  is scoped to avoid touching. Not cheaper now; still deferred.
- **Single-declarator colon spacing**: `const x: number = 1;` renders as
  `const x : number = 1;` (space inserted before the colon) at plain top
  level with no function-type involved. Root cause:
  `JsTsDeclarationAlignmentRule`'s single-declarator grid-alignment handling
  — `classifyTypeColons` deliberately suppresses its own colon-spacing pass
  for a single-declarator statement, deferring to the alignment rule, whose
  spacing doesn't fully match `classifyTypeColons`'s for the ungridded
  case. Spacing-only, no tsc error; confirmed widespread via `grep -rn
  "const [a-zA-Z_]* : "`. Left unaddressed; not blocking `vuejs/core` DONE
  status (pre-existing gap, not a regression from this session's fixes).

  **2026-07-28 cleanup-pass re-assessment:** looked cheap at first glance
  (spacing-only, one rule), but the actual fix requires reconciling two
  independent spacing decisions (`classifyTypeColons`'s vs. the
  declaration-alignment rule's) for the single-declarator case without
  disturbing the multi-declarator grid path that already depends on the
  alignment rule's spacing — not the "clearly low-risk, well-scoped" bar
  this pass requires. Left as a re-assessment note, not attempted.

### `lodash/lodash` dogfood pass — DONE

Repo: `lodash/lodash` (`/tmp/lodash`, HEAD `a666ba5`, v4.18.1). Tree is a
single large `lodash.js` (17259 lines) plus build tooling/tests, not the
per-function-file layout originally anticipated. In-scope corpus: 27 real
`.js` files (`lodash.js`, `fp/*.js` [4], `lib/{common,fp,main}/*.js` [11
build-tooling], `test/*.js`+assets, `perf/*.js`+assets, doctest/playwright
configs); `dist/*`/`vendor/*` excluded per file-exclusion convention. 50983
total lines.

**Baseline:** `node --check` 27/27 pass; parse-only script also 27/27 clean.
**Round1:** zero crashes, 27/27 formatted; `node --check` 27/27 pass — no
new syntax errors.

**Idempotency:** 26/27 byte-identical; `lodash.js` differs — same
"switch-case fallthrough one-liner" shape as the already-tracked,
pre-existing, deferred `vuejs/core` `utils.ts` issue: a long collapsed
`case A: /* FALL-THROUGH */ ... case Z: return foo(a, b);` line
(`initCloneByTag`'s typed-array branch, 196 chars, over limit) is left
unwrapped by `SwitchRule`'s fallthrough render on round1, then wrapped on
round2 — `SwitchRule`/case-grid vs. call-wrap-fits-check ordering, shared
C/C++/Java-owned logic. Not re-root-caused/fixed here (second confirming
data point, not a new bug).

**Content-preservation** (`js_ts_content_diff.js`, original vs. round1):
17/27 "MISMATCH", all decomposing into two already-understood, intentional,
non-lossy transformations the checker didn't yet tolerate:
- **Comment trailing-period stripping** (`normalize-comment-end-period=on`)
  — every flagged pair differs only by a trailing `.` present in original,
  absent in formatted.
- **§10 single-expression-block brace omission** (pre-existing behavior) —
  every flagged diff traces to a single-statement `if`/`while` rendered
  brace-less (confirmed in `fp/_baseConvert.js`, `fp/_mapping.js`); no
  tokens added/removed/reordered beyond the brace omission.

No other content-preservation category appeared anywhere in the corpus.
Both are candidates for a future checker tolerance update — NOT formatter
bugs.

**Verdict: DONE.** Zero new formatter bugs. The one idempotency diff is a
confirming recurrence of the already-tracked `SwitchRule` issue, left
unfixed for the same reason as `vuejs/core`. No further full-corpus re-run
needed.

**Checker subsequently improved, corpus re-verified (follow-up session):**
`js_ts_content_diff.js` gained tolerance for both classes above plus a third
(JSDoc-as-AST-child double-counting raw comment text). Re-run: **22/27
clean** (up from 10/27). Remaining 5 files are two further false-positive
classes, confirmed intentional/non-lossy, left unfixed (out of scope):
- **3 files** (`lib/fp/build-doc.js`, `lib/fp/build-modules.js`,
  `lib/main/build-modules.js`) — bare single-param arrows gaining parens
  (`chunk => ...` → `(chunk) => ...`, documented §6 behavior).
- **2 files** (`perf/perf.js`, `test/test.js`) — **not a formatter bug**:
  STYLE.md §4 mandates pre-increment except when post-form is required; a
  standalone/unused for-loop increment correctly becomes `++index`/
  `++count` (confirmed `perf/perf.js:213`).

Verification pairs (6 hand-crafted `.js`/`.ts` pairs) live in scratchpad
only, not promoted to `test/` fixtures (checker is a dogfood tool, not part
of `make test`).

**TODO (low priority):** teach `js_ts_content_diff.js` to tolerate the two
remaining classes above (arrow-param parens; post-to-pre increment
rewrite) — cosmetic checker gaps, not formatter defects; do whenever
convenient.

### `angular/angular` dogfood pass — categorized; clusters 1-3 FIXED, cluster 4 PARTIALLY FIXED (3 of 4 known root causes; #3 attempted and reverted, see below), cluster 5 NOT YET FIXED (accepted gap, no action planned)

Repo: `/tmp/angular`, shallow clone (`--depth 1`), HEAD `5ad8231`
(2026-07-24). Scope: 5394 `.ts` files (`.d.ts`/`.tsx` excluded, no
`node_modules`/vendored code present in the shallow clone) across
`packages/`, `adev/`, `devtools/`, `integration/`, `modules/`,
`vscode-ng-language-service/`, `dev-app/`, `tools/`. Formatted in 8
batches (one per top-level dir) via `--preserve-tree --root /tmp/angular
--out <scratch>/round1`, then round2 the same way. Syntax-validity check:
TS compiler-API parse-only (no type-check).

Stats: 0 crashes / 5394 files; 29 idempotency mismatches; parse-check
baseline (unformatted originals) 0/5394 errors, round1 output **46/5394**
files with parse errors (339 diagnostic lines) — i.e. 46 formatter-induced
syntax corruptions, a real Category-1 finding despite zero hard crashes.

No fixes attempted yet — clusters below are triage only, sorted
**most-valuable-to-fix first** (value = criticality weighed against
estimated difficulty):

1. **[CRITICAL] [FIXED] Dotted/qualified type-predicate or return-type before
   `=>` gets its last segment wrapped in a spurious paren pair** — dominant
   corruption cluster, **~40 of 46 broken files**. E.g.
   `packages/language-service/override_rename_ts_plugin.ts:29`: `(): ts
   .server.PluginModule =>` → `(): ts.server.(PluginModule) =>`;
   `packages/language-service/src/template_target.ts:429`: `node is tss
   .Node =>` → `node is tss.(Node) =>`;
   `packages/compiler-cli/src/ngtsc/typecheck/src/checker.ts:690,724`:
   `diag is ts.Diagnostic =>` → `diag is ts.(Diagnostic) =>`; also
   `inheritance_graph.ts`, `util.ts`, `navigations.ts`, ~35 more. Root
   cause: `JsTsSpecificRule.enforceArrowFunctionParameterParens` (§6's
   "wrap bare single arrow param" rule) scans backward from `=>` for a
   single bare identifier and wraps it, without checking whether that
   identifier is the tail of a preceding **dotted** type-predicate/
   return-type annotation. The existing bail-out special-cased `is`/
   `typeof`/`keyof` as `prevPrev` (from `vuejs/core`), but a multi-segment
   dotted path (`ts.server.X`, `tss.X`) wasn't covered (immediate `prevPrev`
   is `.`, not a whitelisted keyword). Common real-TS shape, produces
   silently-invalid output that still looks plausible — only caught via
   compiler parse-check.

   **FIXED**: `enforceArrowFunctionParameterParens` now walks backward over
   any number of `IDENTIFIER '.'` pairs before `prevIdx` to find the dotted
   chain's first segment (`anchorIdx`), then applies the existing bail-out
   check against what precedes THAT. Verified against a hand-written repro
   (all three shapes + a bare single-param arrow, still correctly wrapped)
   and directly against the three real files cited above — no corruption
   remains. Fixture `real_code_regressions_134`. `make test`: 183/183.
2. **[CRITICAL] [FIXED] Old-style angle-bracket cast (`<Type>{...}`)
   misparsed as a generic, injecting a bogus `;` inside the following
   object literal** — 1 file: `packages/core/src/testability/
   testability.ts:229`: `push(<WaitCallback>{doneCb: cb, timeoutId:
   timeoutId, updateCb: updateCb});` → `...updateCb: updateCb;});` (spurious
   `;` before closing `}`, breaks the call).

   **FIXED**: root cause one level downstream of the initial
   `TokenizerCurly.reclassifyAngleBrackets` guess — that pass correctly
   leaves a cast's `<`/`>` as plain OP tokens (it only tracks a `<`
   preceded by IDENTIFIER, i.e. a generic clause; a cast's `<` follows an
   expression-start token like `(`), but nothing downstream recognized this
   plain-OP `<Type>` shape as a cast, so the following object literal fell
   through `JsTsSpecificRule.classifyBraces`'s default-false `isValue` case
   and got depth-reset as a statement body, triggering
   `enforceSemicolonInsertion` to inject a bogus `;` before its closing `}`.
   Fixed with `isLegacyCastBrace`: a `{` preceded by a plain `>` whose
   matching plain `<` sits before a (optionally dotted) type name following
   a value-starting token (`(`, `,`, `=`, `return`, ...) is now treated as a
   value/pattern brace. Verified against real `testability.ts:243` (no
   corruption) plus a minimal repro. Fixture `real_code_regressions_135`.
   `make test`: 184/184.
3. **[CRITICAL] [FIXED] Multi-line generic return-type clause loses its
   closing `>`, spilling a bogus `;` into the type** — 1 file:
   `packages/private/testing/src/utils.ts:103-105`: `async function
   loadDominoOrNull(): Promise<\n  (typeof import(...))['default'] |
   null\n> {` produces a `'>' expected` parse error on round1 output.

   **FIXED**: bisected to TS's dynamic-import type-query operand
   (`import(...)` as a type operand inside the generic clause): `import` is
   a KEYWORD not present in `TokenizerCurly.GENERIC_SAFE_KEYWORDS`, so it
   invalidated the whole enclosing `<...>` tracking before the matching `>`
   — same gap class as the existing `keyof`/`is`/`infer`/`asserts`/
   `readonly`/`unique`/`as`/`satisfies`/`typeof` entries. Fixed by adding
   `"import"` to `GENERIC_SAFE_KEYWORDS`. Verified against real
   `utils.ts:102-105` (no corruption) plus a minimal repro, idempotent.
   Fixture `real_code_regressions_136`. `make test`: 185/185.
4. **[PARTIALLY FIXED] Call-wrap/collapse vs. alignment-padding fits-check
   ordering** — dominant idempotency cluster, **~23 of 29 files**:
   `packages/router/src/create_router_state.ts:27`,
   `packages/core/src/render3/node_selector_matcher.ts:155`,
   `packages/compiler/src/template/pipeline/src/ingest.ts:814`,
   `packages/localize/tools/src/translate/source_files/locale_plugin.ts:42`,
   `vscode-ng-language-service/server/src/handlers/hover.ts:58`, plus ~18
   more (`compiler-cli/src/ngtsc/core/{compiler,host}.ts`, `schematics/*`,
   `common/src/i18n/format_{date,number}.ts`,
   `common/upgrade/src/location_shim.ts`,
   `devtools/.../split.component.ts`,
   `animations/browser/test/.../web_animations_player_spec.ts`,
   `adev/.../app.ts`). A call's argument list wraps multi-line on one
   round and collapses on the other (or a class-field/const-declaration
   initializer wraps, disturbing its alignment-grid siblings' column
   widths) — the already-documented "Known open issues" `scripts/
   release.js`/`real_code_regressions_85`/`_102` fits-check family above:
   `enforceCallLineBreaking`'s single-argument fits-check measures the
   candidate line's length **before** declaration-alignment/complexity-
   padding finish adjusting column widths, flip-flopping every round. This
   run **confirms it's broad and high-frequency**, not a rare edge case —
   23/29 idempotency files is the majority. `indent-size=2`: tested 2
   files — `create_router_state.ts` avoids the flip at indent-size=2
   (boundary shifted), `node_selector_matcher.ts` reproduces identically
   (config-insensitive, confirming it's a pure ordering bug not a
   config-boundary artifact).

   Two of the (at least three) distinct root causes behind this cluster
   are now **FIXED**:

   - **Root cause #1 — trailing-comma dangling-empty-group** (confirmed via
     `create_router_state.ts:27`): `renderCallDropped`/`renderCallOnePerLine`
     measured a call's argument list via `splitTopLevelCommas`, which —
     unlike sibling `groupByOriginalLine` — doesn't drop a dangling trailing
     empty group from a trailing comma before `)` (this codebase's style,
     `foo(\n  a,\n  b,\n);`). A fresh format measured 2 chars wider than a
     reformat of already-formatted output (formatter renders never emit a
     trailing comma), flipping the fits-check at the boundary. Fixed by
     adding the same dangling-empty-group drop to both methods. Fixture
     `real_code_regressions_140`.
   - **Root cause #2 — `if (`/`if(` keyword-spacing pipeline ordering**
     (confirmed via `node_selector_matcher.ts:155`, `locale_plugin.ts:42`):
     `enforceCallLineBreaking`'s fits-check for a call embedded in an
     `if (...)` condition measured the line including the keyword-to-paren
     gap, since `enforceKeywordSpacing` (collapses `if (` to `if(`)
     originally ran only in Phase 4, after this fits-check — one char
     narrower on reformat than on a fresh format, flipping the fits-check
     at the boundary (confirmed exact 101-vs-100-char split). Fixed by
     pulling `enforceKeywordSpacing` forward to run immediately before the
     first `enforceCallLineBreaking` call in `FormatterCurly.formatOne`
     (same "measurement must see final width" pattern as
     `enforceComplexityPadding`/`enforceAttributeAndSpliceBracketPadding`/
     `enforceInitializerBraceSpacing`); original Phase 4 call left in place.
     Applies to all curly-brace languages (shared `MiscRuleCore` method) —
     full `make test` re-run confirmed no regressions. Fixture
     `real_code_regressions_141`. `make test` after both fixes: 190/190.

   A spot-check re-run against 8 of the originally-cited files after both
   fixes found 6 now idempotent (`create_router_state.ts`,
   `node_selector_matcher.ts`, `ingest.ts`, `locale_plugin.ts`,
   `hover.ts`, `format_number.ts`) and 2 **still broken**, each via its
   own further distinct root cause (now diagnosed, not yet fixed):

   - **Root cause #3 (attempted, REVERTED — too many regressions) —
     braceless-else body never re-validated after brace-collapse/
     alignment** (`common/src/i18n/format_date.ts:519`,
     `if(offset === 0) return 'Z'; else return (...)`):
     `collapseSingleExpressionBlocks` strips the `if`/`else` braces (Phase
     0) before `enforceCallLineBreaking` runs (Phase 1) — but the braced
     source form uses a `+`-chain complexity-wrap to fit, which doesn't get
     applied to a now-braceless `else` body, leaving the joined line over
     the limit. `alignBracelessElseIfChain` (last pass) then pads the
     branch to align regardless — an intentional escape hatch per its own
     comment (`BlockStructureRule.java` ~line 2801-2819), not a bug there. A
     fresh format thus commits a too-long braceless-else line unvalidated;
     reformatting re-runs `enforceCallLineBreaking` on the joined line and
     wraps it — the two rounds diverge.

     **Tried**: refuse to collapse (`tryCollapse`) whenever the joined
     one-line result exceeds `lineLengthLimit`, leaving braces in place.
     **DO NOT retry this naive approach** — reverted because it has no way
     to know `enforceCallLineBreaking` will still wrap a call *inside* the
     joined body and make it fit, so it wrongly re-braced every braceless
     if/else whose body merely contains a wrappable call — broke 5 existing
     fixtures/dogfood cases (`java_combined`, `real_code_regressions_57`,
     `_81`, `_93`, `_141`). Real fix needs the guard to account for
     downstream wrapping potential (run the same wrap logic against the
     joined candidate before deciding it can't fit) — bigger lift, deferred.

     **2026-07-28 cleanup-pass re-assessment:** still the same bigger lift —
     the real fix needs `tryCollapse` to simulate `enforceCallLineBreaking`'s
     wrap decision on the joined candidate before deciding collapsibility,
     the kind of two-pass lookahead that caused the naive attempt's 5-fixture
     regression. Not attempted again this pass.
   - **Root cause #4 [FIXED] — trailing same-line comment inconsistently
     counted in the collapse fits-check** (`common/upgrade/src/
     location_shim.ts:461`, `this.$$absUrl = this.getServerBase() +
     this.$$url.slice(1); // remove '/' from front of URL`): on a fresh
     format, the call is still on its original line with the trailing
     comment, and the fits-check counts the comment's width (104 chars incl.
     comment > 100-limit once `=`-alignment padding widens the assignment
     column) — wraps. Once wrapped, the comment moves after the call's `)`
     on its own line; reformatting re-measures without the comment (72
     chars, fits) — collapses back. Whether the comment counted depended on
     whether the call was already wrapped, not actual final width — non-
     idempotent. **Fixed** via `appendRangeCollapsingTrailingCommentGap`
     (`MiscRuleCurly.java`): a whitespace run immediately before a trailing
     line comment collapses to one space for measurement only (never
     rendered) — comment-column padding width shouldn't count as structural
     width. Used only in the JS/TS tight-candidate fits-check's `suffix`
     computation. Verified against `location_shim.ts` and a minimal repro;
     full `make test` re-run, no regressions. Fixture
     `real_code_regressions_142`. `make test`: 191/191.

     **Separate, still-open observation found while building this
     fixture**: a 3-sibling variant (all three `this.$$foo = ...`
     assignments in one `=`-alignment group, as in the real file) still
     isn't self-stable on the very first format — `format(inp)` commits the
     group's wide comment-column padding onto the newly-wrapped call's
     comment line, but `format(format(inp))` re-collapses it to one space.
     Did **not** reproduce against the actual `location_shim.ts` (confirmed
     idempotent), so root cause #4's fix is complete for that file; fixture
     142 deliberately kept single-statement to avoid this separate quirk.
     Not investigated further — flag if a future dogfood run hits it.

   `compiler-cli/src/ngtsc/core/{compiler,host}.ts` and
   `devtools/.../split.component.ts` were not re-checked (missing from
   this `/tmp/angular` checkout at re-check time). This entry stays
   open until root cause #3 is fixed and a full re-run across all ~23
   originally-cited files confirms full resolution, or further causes
   are found.
5. **[IDEMPOTENCY] Reindentation on internally-inconsistent source
   (accepted gap, third confirming recurrence, no new action)** — 3
   files: `packages/benchpress/test/metric/user_metric_spec.ts:88`,
   `packages/compiler/src/template/pipeline/src/emit.ts:104`,
   `packages/core/src/render3/i18n/i18n_parse.ts:520` — a lone closing
   `}`'s indent (2 vs 4 spaces) differs between rounds because the
   *original* source itself has genuinely inconsistent brace indentation
   (mixed 2-/4-space blocks in the same function) and this formatter's
   indentation model is relative-delta-from-one-reference-line, not
   absolute-depth-derived — the exact, already-ACCEPTED-not-fixed
   architectural gap documented in `STATE_COMMON.md`'s "General
   scope-depth reindentation" section (prior confirmed instances:
   `javaparser`'s `ASTParser.java`, local `JSONEncoderLite.java`). Lowest
   priority: low criticality (only affects already-inconsistently-indented
   source), architecturally hard (explicitly scoped as its own future
   dedicated high-risk job in `STATE_COMMON.md` — do not attempt
   piecemeal).

   **2026-07-28 cleanup-pass re-assessment:** unchanged — still the same
   cross-job architectural gap tracked centrally in `STATE_COMMON.md`,
   explicitly out of scope for this housekeeping pass.

Next free fixture number unaffected (no fixtures added yet — none of the
five clusters above has been fixed). Full corpus re-run deferred until
fixes land, same pattern as `vuejs/core`/`lodash/lodash` above.

## Dogfood: microsoft/TypeScript (categorization pass, not yet fixed)

Repo: `microsoft/TypeScript`, shallow clone (`--depth 1`), HEAD `cc5c6e2`
(2026-07-28), `/tmp/ts-dogfood/TypeScript`. Full tree has 20798 `.ts` +
333 `.tsx` (excluded, out of scope) + 1095 `.d.ts` files, but
`tests/cases/**` (20089 files) is deliberately excluded — it's hand-authored
*compiler test fixtures*, many containing intentionally-invalid syntax
(`// @noEmit`-style directive comments, deliberate parse-error cases used to
test the compiler's own diagnostics), the same "test fixtures/generated
code" shape `STATE_COMMON.md` says to skip, not real source. `tests/
baselines/**` (auto-generated expectation data) is excluded for the same
reason the task called out. Corpus actually used: **`src/` only, 601 real
`.ts` files (108 `.d.ts` excluded), 379045 lines** — this is the same
compiler/services/server/harness/testRunner source tree previously listed
as "huge, ~1490 kLOC" (that estimate presumably included `tests/`); `src/`
alone is still a large, heavily-typed, real-world corpus.

Batched via `--preserve-tree --root ... --out ...` (one invocation for
round1, one for round2, ~601 files each, ~1m15s / ~52s wall-clock). Round1:
**zero crashes, 601/601 files produced.** Syntax-checked via a throwaway
TS-compiler-API parse-only script (no `js_ts_syntax_check.js` exists yet in
`tools/verifiers`; modeled on the parse step already used for `angular/
angular`'s dogfood pass) — baseline (unformatted) 0/601 parse errors.
Round1→round2 idempotency: `diff -rq` found **30/601 files differing**.

### Category 1 — Critical (round1 itself is corrupt/unsafe)

Round1 parse-check: **8/601 files, 85 diagnostics** — all traceable to 3
distinct root causes:

1. **`||=`/`&&=` compound-assignment operators not tokenized as a single
   token** — 1 file (`src/compiler/checker.ts`), 3 occurrences. Root cause
   precisely identified (no subagent needed): `TokenizerCurly.
   MULTI_CHAR_OPS` (`src/com/jxmake/formatter/tokenizer/TokenizerCurly.java`
   ~line 188) lists `??=` but is missing both `||=` and `&&=`, even though
   `JsTsSpecificRule.java` (lines ~72, ~93) already references `"&&="`/
   `"||="` in spacing/precedence tables as if they tokenize as one token.
   Without a `MULTI_CHAR_OPS` entry, `||=` splits into `||` then a separate
   `=` token, which downstream spacing/padding logic then renders with a
   spurious space and stray parens:
   ```
   // original
   const cache = (links.accessibleChainCache ||= new Map());
   // round1 output
   const cache = ( links.accessibleChainCache || = new Map() );
   ```
   **Value: HIGH** (silent semantic corruption — `||=`/`&&=` are common
   modern JS/TS idioms, e.g. lazy-init caches; this exact shape appears
   3x in one file of a 601-file corpus, likely widespread elsewhere).
   **Difficulty: TRIVIAL** — two-string addition to an existing array,
   same shape as the already-landed `??=` entry right next to it.
   **Ranks #1** — textbook high-value/near-zero-difficulty fix.

   **FIXED (2026-07-28):** added `"&&="`/`"||="` to `MULTI_CHAR_OPS`
   immediately before the pre-existing `"&&"`/`"||"` entries (longest-match-
   first ordering, `emitOperator`'s loop tries entries in array order and
   returns on first `startsWith` match). Fixture:
   `test/real_code_regressions_143_{inp,out}.ts`. `make test`: 192/192
   forward + 192/192 idempotency, zero regressions.

2. **Union-type return-type/type-predicate before `=>` gets its last
   segment wrapped in a spurious paren pair** — 6 files (`checker.ts` x4,
   `services/mapCode.ts`, `services/symbolDisplay.ts`, `services/
   refactors/moveToFile.ts`, `services/codefixes/
   addConvertToUnknownForNonOverlappingTypes.ts`, `testRunner/unittests/
   helpers/virtualFileSystemWithWatch.ts`), ~9 occurrences. **Same root
   cause FAMILY, same function (`enforceArrowFunctionParameterParens`
   in `JsTsSpecificRule.java`), as the already-fixed `angular/angular`
   cluster 1 ("dotted/qualified type-predicate... gets its last segment
   wrapped") — but a DIFFERENT shape that fix didn't cover.** The angular
   fix (commit landing `real_code_regressions_134`) walks back over
   `IDENTIFIER '.'` chains to find a dotted return-type's anchor before
   checking the `:`/`is`/`typeof`/`keyof` bail-out list; it does not walk
   back over a union `|` operator, so a union type whose last operand is a
   bare identifier still gets wrapped:
   ```
   // original (services/symbolDisplay.ts:296)
   (declaration): declaration is AccessorDeclaration | PropertyDeclaration =>
   // round1 output
   (declaration): declaration is AccessorDeclaration | (PropertyDeclaration) =>
   ```
   also reproduces for a plain (non-predicate) union return type
   (`services/mapCode.ts`: `(block): block is Block | SourceFile =>` →
   `... | (SourceFile) =>`) and inside a callback argument
   (`checker.ts:39179`: `(p): p is PropertyAssignment | ShorthandPropertyAssignment =>`).
   **Value: HIGH** (real parse errors, common shape — union return
   types/predicates are idiomatic TS, hits 6/601 files here alone).
   **Difficulty: LOW-MEDIUM** — same walk-back idea as the already-shipped
   dotted-chain fix, just extended to also walk back over a leading `|`
   (and its left operand) before the bail-out check; the existing fix's
   code/tests are a direct template. **Ranks #2** — high value, low
   difficulty, and a near-identical precedent already exists in the same
   function.

   **FIXED (2026-07-28):** `enforceArrowFunctionParameterParens`'s existing
   dotted-chain walk-back is now a loop that alternates the `IDENTIFIER '.'`
   walk-back with a new `IDENTIFIER '|'` (union type operator) walk-back
   until neither makes further progress, so `anchorIdx` lands on the true
   first anchor of the return-type/type-predicate expression before the
   `:`/`is`/`typeof`/`keyof` bail-out check — covering chained unions
   (`A | B | C`) and dotted union members (`A | ts.B`), not just a single
   `|`. Verified directly against the three real trigger files
   (`services/symbolDisplay.ts`, `services/mapCode.ts`,
   `compiler/checker.ts` under `/tmp/ts-dogfood/TypeScript`) — no spurious
   parens remain around any union's last segment. Fixture:
   `test/real_code_regressions_145_{inp,out}.ts` (type-predicate union,
   three-member union type-predicate, and plain union return type, all in
   one file). `make test`: 194/194 forward + 194/194 idempotency, zero
   regressions.

3. **Backslash-newline continuation inside a plain (non-template) string
   literal not honored, corrupting the rest of the string/statement** — 2
   files (`testRunner/unittests/incrementalParser.ts`, `testRunner/
   unittests/services/colorization.ts`). Legal (if archaic) JS: a
   double-quoted string literal spanning multiple source lines via a
   trailing `\` before each newline:
   ```
   // original
   const source = "class C {\
       set Bar(bar:string) {}\
   }\
   var o2 = { set Foo(val:number) { } };";
   ```
   Round1 mangles this into multiple broken statements with unterminated
   strings and stray tokens (`Unterminated string literal`, `Invalid
   character`, `Declaration or statement expected`) — the tokenizer
   appears to treat the trailing `\` + newline as ending the string rather
   than escaping the newline. Not root-caused down to the exact
   tokenizer line (would need a short subagent/deeper dive to pin the
   exact `readToken` string-scanning branch) — categorization only per
   task scope. **Value: MEDIUM** (real corruption, but this exact
   multi-line-backslash-continuation-in-a-plain-string shape is rare in
   idiomatic modern code — modern style uses template literals or
   `+`-concatenation for multi-line strings; both hits here are decades-old
   test-harness code, not typical application code). **Difficulty: MEDIUM**
   (tokenizer string-scanning change, needs care not to regress ordinary
   escape-sequence handling for `\n`/`\t`/`\\` etc.). **Ranks #5** — real
   bug, but lower frequency/idiomaticity than #1/#2 and non-trivial to fix
   safely.

### Category 2 — Idempotency-only (round1 valid, round2 differs)

**28 of the 30 idempotent-diff files are a single cluster, already tracked
— NOT a new bug.** Spot-checked ~10 of the 28 (`compiler/builder.ts`,
`compiler/commandLineParser.ts`, `compiler/emitter.ts`, `compiler/factory/
nodeFactory.ts`, `testRunner/parallel/host.ts`, `typingsInstallerCore/
typingsInstaller.ts`, `server/session.ts`, `services/completions.ts`,
`services/jsDoc.ts`, `harness/client.ts`) — every diff is a call
wrapped-multi-line on one round and collapsed-to-one-line on the other (or
an object/call closer `}`/`)` moving to its own line), exactly the
already-documented **"Call-wrap/collapse vs. alignment-padding fits-check
ordering"** cluster from the `angular/angular` dogfood pass (this file's
cluster 4, `enforceCallLineBreaking`'s single-argument fits-check measuring
candidate width before declaration-alignment/keyword-spacing/complexity-
padding finish adjusting column widths). Confirmed config-insensitive here
too: `commandLineParser.ts` retested with `.jxmake-code-formatter`
(`indent-size = 2`) reproduces the identical diff shape (`) };` → `)\n};`
plus a re-collapsed `const optionMap = ...` alignment column), same as
angular's `node_selector_matcher.ts` confirmation. **Cross-reference: this
is the SAME root cause as `angular/angular`'s open cluster 4, not a new
bug** — a third confirming recurrence at even larger scale (28/601 files
here vs. 23/5394 in angular, proportionally ~30x denser, likely because
this repo's line-length/wrapping style sits close to the 100-char boundary
very often). Two of angular cluster 4's ≥4 root causes are already fixed
(dangling-empty-group measurement, `if(`/`if (` keyword-spacing ordering);
this run doesn't newly isolate which of the remaining root causes (#3
braceless-else-body, or others) apply to these 28 files — out of scope to
re-diagnose here, same underlying architectural fix needed either way.
**Value: HIGH** (highest file-count impact of anything found this pass).
**Difficulty:** inherits whatever the angular cluster 4 remaining work
already estimated (medium-high — cross-pass-ordering fix, the naive
`tryCollapse` guard attempt already reverted once for 5-fixture
regressions). **Ranks #3** (tied consideration with #2, see ranked list
below) — very high file-count value, but explicitly already scoped as a
bigger, riskier lift than #1/#2.

The remaining **2 of 30** idempotency files (`checker.ts`, `incrementalParser.ts`)
are already accounted for under Category 1 above (their round1 output is
actively corrupt, so their round1→round2 diff is a symptom of the same
Category-1 bug, not an independent idempotency-only finding).

### Ranked list (all clusters, most-valuable-to-fix first)

1. **`||=`/`&&=` tokenizer gap** (Category 1) — trivial 2-string fix,
   silent semantic corruption on a common modern-JS idiom. Highest
   value/difficulty ratio of anything found.
2. **Union-type-before-`=>` arrow-param spurious wrap** (Category 1) —
   real parse errors on an idiomatic TS shape (union return
   types/predicates), and the fix is a direct, low-risk extension of an
   already-shipped near-identical fix (dotted-chain walk-back) in the same
   function.
3. **Call-wrap/collapse vs. alignment-padding fits-check ordering**
   (Category 2) — by far the highest file-count (28/601 here, plus
   23/5394 in angular, plus smaller recurrences elsewhere), but same
   already-scoped medium-high-difficulty cross-pass-ordering fix as
   angular's still-open cluster 4; ranked below #1/#2 purely on
   difficulty, not value — whoever picks up angular cluster 4's remaining
   work should treat this TS corpus as further confirming evidence, not a
   separate task.
4. **Backslash-newline-continued plain-string-literal corruption**
   (Category 1) — real corruption, but affects only 2 files in an
   old-style test-harness idiom rarely seen in modern code; needs its own
   root-cause dive (not yet pinned to an exact tokenizer line) before a
   fix estimate firms up.

No fixture-only false positives were found this pass (unlike `lodash/
lodash`'s comment-period/brace-omission content-diff false positives) —
this pass used direct TS-compiler-API parse-checking and raw `diff`, not
`js_ts_content_diff.js`, so that checker's known tolerances weren't
exercised here.

**Status: categorized, not fixed** (per this pass's explicit scope — see
`STATE_DOGFOOD.md`'s row for the status-legend caveat). No source files
under `src/` were modified.

### Known false positives (no source change needed, fixture-only)

- A spurious-looking blank line after a class's opening `{` in older `.js`
  fixture drafts, and a doubled trailing space before a one-liner getter
  body's closing `}` — both confirmed correct, existing behavior
  (STYLE.md §7 named-construct blank line; `GetterSetterRuleCurly`'s
  group-width body padding), matching passing C++/Java/Kotlin fixtures
  byte-for-byte. Only the stale hand-authored `.js` draft fixtures were
  wrong — resolve by regenerating them during Next Steps 1.
