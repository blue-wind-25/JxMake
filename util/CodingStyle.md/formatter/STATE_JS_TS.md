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
(`/tmp/express`, HEAD `ae6dd37`, 1.9M, 141 `.js` files: `lib/` 6, `test/`
91, plus `examples/`; 0 `.jsx`/`.tsx`). All 141 files processed in full.

Two real bugs found via `node --check` (output stayed syntactically
plausible enough that only Node's parser caught it), both fixed, combined
into one fixture (`test/real_code_regressions_77_inp/out.js`):

- **ASI leading-continuation-operator/comma bug** — `maybeInsertSemicolon`
  only looked at the *previous* line's trailing token, never the *next*
  line's leading token. Method-chaining with the operator leading the
  continuation line (`request(app)\n.get('/')\n.expect(...)`, ubiquitous in
  Express's Mocha suite) or a comma-first multi-declarator list both got a
  bogus `;` inserted mid-chain/mid-list. Fixed by adding a
  `LEADING_CONTINUATION_OPS` set (narrower than `CONTINUATION_OPS` —
  excludes unary-legal `+`/`-`/etc.) plus a leading-`,` check, both via
  next-significant-token lookahead.
- **No JS/TS regex-literal tokenizing at all** — every `/` that wasn't
  `//`/`/*` fell through to the generic division-operator scan. A regex
  containing a `"` inside a bracketed character class
  (`/^(?:W\/)?"[^"]+"$/`, from `test/res.sendFile.js`) had its `"` mistaken
  for a string-literal start, corrupting brace/paren/statement tracking for
  the rest of the statement. Fixed by adding `TokenizerCurly.emitRegex
  Literal` (opaque STRING-typed token, same posture as `emitTemplate
  Literal`) and `isRegexLiteralAllowedHere` (regex-vs-division
  disambiguation).

Final numbers (full 141-file corpus, both bugs fixed): forward pass zero
crashes; round1→round2 `diff -r` empty; `node --check` 141/141 pass (was
93/141 failing before the fix). Semantic check went further than syntax:
`npm install --prefix <scratch> express` supplied a dependency tree;
`require('./index.js')` on formatted `lib/express.js` returned the expected
function, `express()` produced a working `app`, and a real `app.get`/
`app.listen`/HTTP GET round-tripped end-to-end. Formatted `test/` files run
directly under `mocha`: a dependency-clean file (`test/req.host.js`+
`req.hostname.js`+`Route.js`) passed 35/35. `test/res.sendFile.js`
(post-fix) also loads and runs under Mocha — 42 assertions fail, confirmed
an environment limitation (static-file 404s from an unpinned `send`/
`serve-static` version, reproduces identically on the unformatted
checkout), not formatter-induced.

### Resolved this session (nestjs/nest real-code testing, in progress)

Repo: `nestjs/nest` (`/tmp/nest`, HEAD `7e6e313`). Five bugs found and
fixed:

1. **`/**`-style JSDoc opener corruption in `MiscRuleCore.
   reformatMultiLineBlockComment`** — universal bug across every curly-family
   language sharing this class (JS/TS, Java, Kotlin, C/C++). `firstContent =
   rawLines[0].substring(2).trim()` assumed the opening marker is always 2
   chars (`/*`), but a JSDoc/Javadoc `/**` opener is 3 — the stray `*` got
   promoted to a fake first content line, rendering `/**` as `/*` plus a
   spurious `* *` line on nearly every multi-line comment reformatted by
   this method. Fixed by scanning forward from index 2 while the char is
   `*` to find the true marker end, capturing the whole marker (`/*`,
   `/**`, `/***`, ...) and reusing it verbatim instead of a hardcoded `/*`.
   Confirmed fixed for Java too. Existing Kotlin fixture
   `test/real_code_regressions_38_out.kt` updated to corrected output.
   `make test`: 128/128 forward + idempotency, zero regressions.
2. **Dot+space corruption in `MiscRuleCurly.renderCallCandidate`'s
   `sigForRender` typed/untyped selection** —
   `options.provideInjectionTokensFrom` became `options.
   provideInjectionTokensFrom` when a multi-arg call whose every argument
   is a bare dotted member-access expression got rejoined/rewrapped.
   Root-caused via debug prints at phase boundaries: `FormatterCurly`'s
   *second* `enforceCallLineBreaking` pass took a different render path —
   `parseSignature` misparsed the argument list as a C/C++/Java-style
   forward-declaration parameter list (type `options.`, name
   `provideInjectionTokensFrom`), the same misparse class already guarded
   for Kotlin only but never extended to JS/TS. Fixed by forcing
   `sigForRender` to `null` for JS/TS too (neither language has a
   prototype-only forward-declaration shape); also hardened
   `collapseTokensToOneLine` to never insert a space immediately before/
   after a tight `.`/`?.` token. New fixture
   `test/real_code_regressions_81_{inp,out}.ts`. `make test`: 130/130
   forward + idempotency. Confirmed against all 5 originally-reported
   files; `node --check` (Node 24) passes on all 5 round1 outputs.
3. **Content duplication in `enforceClassFieldAlignmentGrid` — nested
   `CLASS` braces not handled** — `packages/core/injector/module.ts` (a
   `return class extends ModuleRef {...}` nested inside an outer class
   method) and `packages/core/middleware/builder.ts` had entire method/
   constructor blocks duplicated in round2. Root cause: the method collects
   every `CLASS`-classified brace into a flat `classOpens` list and sweeps
   with a single linear `cursor`, assuming disjoint spans — a nested class
   breaks that: the outer class's rewrite already copies the inner class
   span byte-for-byte, so the later top-level entry for the inner class
   re-appended its content and walked `cursor` backward, causing the final
   raw-copy-to-EOF loop to re-emit everything twice. Fixed by filtering
   `classOpens` to only the outermost class brace at each nesting level
   (nested class fields simply don't get alignment-grid treatment). New
   fixture `test/real_code_regressions_82_{inp,out}.ts`. `make test`:
   131/131 forward + idempotency. Confirmed against both files; `node
   --check` passes on both round1 outputs.
4. **Comment-continuation-indent drift / arbitrary-deep-indent corruption on
   an object-shaped `type X = { ... } & Y;` intersection alias** —
   `packages/core/inspector/interfaces/edge.interface.ts`. Root cause:
   `enforceUnionTypeContinuationIndent` re-indented *every* `NEWLINE` from a
   multi-line `type NAME = ...;` RHS through the terminating `;` to the
   RHS's own column, with no bracket-depth tracking — harmless for a plain
   union list, but an intersection whose left operand is a multi-line
   object-type literal has `NEWLINE`s nested inside the object body's
   already-correct indentation, which got force-reindented too, blowing
   members out to an arbitrarily deep column (compounding across passes
   since a leading JSDoc comment shielded interior lines on round1 only).
   Fixed by tracking bracket depth and only re-indenting a `NEWLINE` at the
   union/intersection's own top level (depth 0). New fixture
   `test/real_code_regressions_84_{inp,out}.ts`. `make test`: 133/133
   forward + idempotency. Confirmed against the file; `node --check`
   passes. (Debug-print methodology: dumped `text` after `ScopePipeline
   Curly.process` and after `enforceInterfaceTypeAliasMemberColonAlignment`
   to localize the corruption.)
5. **`join(...)` call-wrap/collapse non-idempotency** (last of the
   originally-reported bugs) —
   `integration/repl/e2e/repl-process.spec.ts`'s `const localPackageResolver
   = join(workspaceRoot, '...')` (exactly 100 chars collapsed, right at
   `lineLengthLimit`). Root cause: `renderCallCandidate`'s multi-line-source
   branch always preserved the call's original per-line argument grouping,
   with no fits-check of its own (unlike the sibling single-line branch) —
   a call that had ever been wrapped stayed wrapped even once it fit on one
   line, flipping between forms across repeated passes right at the
   boundary. Fixed by adding the same fits-check, scoped to JS/TS only
   (widening to every language regressed `real_code_regressions_1`/C++):
   measures the actual tight single-line candidate directly (rather than
   `collapseToOneLine`, which overestimates length via a phantom space for
   the newline) and collapses whenever it fits, dropping any dangling
   trailing empty argument group first (`splitTopLevelCommas` doesn't drop
   a trailing comma's empty tail itself). New fixture
   `test/real_code_regressions_85_{inp,out}.ts`; also updated
   `real_code_regressions_81_out.ts`'s expected output (its call now
   correctly collapses too). `make test`: 134/134 forward + idempotency,
   zero regressions beyond the intentional fixture-81 update. Confirmed
   against the file; `node --check` passes on the round1 output.

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
  its *original* source indentation (e.g. this repo's 2-space convention)
  instead of reindenting to the member's re-rendered depth (this
  formatter's 4-space default) — round1 left it misaligned, and a later
  general block-comment reindent caught up on round2, producing different
  stable outputs each round. Fixed by adding `reindentLeadingComment`
  (strips each continuation line's leading whitespace, reconstructs
  `indentPrefix + " " + strippedLine`) and calling it at all three sites
  instead of emitting the captured text raw. New fixture
  `test/real_code_regressions_87_{inp,out}.ts`. `make test`: 136/136
  forward + idempotency. Confirmed fixed against 15 of the originally-
  affected files (`dep.ts`, `computed.ts`, `effect.ts`, `effectScope.ts`,
  `collectionHandlers.ts` [comment part only], `parser.ts`,
  `transformElement.ts`, `vModel.ts`, `vSlot.ts`, `resolveType.ts`,
  `componentEmits.ts`, `componentPublicInstance.ts`,
  `rendererTemplateRef.ts`, `renderer.ts`, `vOn.ts`) via a targeted
  affected-files-only re-run.

**4 of the original 5 non-idempotent files fully resolved** (3 new bugs
found/fixed, all with fixtures):

- `packages/reactivity/src/collectionHandlers.ts` — FIXED (not the
  general-reindent gap, as first guessed). `TokenizerCurly.GENERIC_SAFE_
  KEYWORDS` was missing TS primitive keywords `symbol`/`bigint`, and
  `isGenericSafeToken`'s OP case had no entry for `|`, both needed for a
  union type inside a generic argument list (`Record<string | symbol,
  Function | number>`) to keep the enclosing `<...>` reclassified as angle
  brackets. `real_code_regressions_88` fixture, commit 189118e.
- `packages/runtime-core/src/componentOptions.ts` — three distinct root
  causes chased and fixed: (1) the same symbol/bigint/`|` bug as above; (2)
  `JsTsDeclarationAlignmentRule.parseTypeAlias`'s generic-clause skip loop
  (`type MergedHook<T = () => void> = ...`) advanced past a type-parameter-
  default clause without capturing its tokens, silently deleting the whole
  `<...>` clause; (3) `TokenizerCurly`'s dispatch loop had an unconditional
  `]`-immediately-followed-by-`]` branch meant only for C++11 attribute
  closes, which fired for a TS mapped type's `{ [K in T[number]]?: unknown
  }` and emitted an OP token instead of the ordinary PUNCT
  `emitCloseBracket()` path, desyncing `enforceSemicolonInsertion`'s `[`/`]`
  depth counter for the rest of the file. `real_code_regressions_89`
  fixture, commit 453deef. **One narrower residual bug in this same file
  found during full-corpus re-verification** — see "Still open" below.
- `packages-private/dts-test/ref.test-d.ts` /
  `watch.test-d.ts` — FIXED. `JsTsSpecificRule.classifyBraces`'s `isValue`
  prev-token list had no entry for TS's union/intersection continuation
  operators `|`/`&`; an inline object type following one in a union type
  alias (`type Steps = { step: '1' } | { step: '2' }`) fell through to
  "default to not a value", misclassifying its `{` as a statement-body
  brace and resetting the ASI depth counter at a false statement boundary —
  corrupting indentation for the rest of the enclosing scope, not just a
  semicolon defect. `real_code_regressions_90` fixture, commit a6edd22.

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
  ...\n);`. Per `ComplexityPaddingEvaluator.isLoose`/`enforceComplexity
  Padding`'s contract and RDD_KEY_62: a nested `(` or `[` anywhere inside a
  paren pair's content makes it "loose" per STYLE.md §3.1, applied
  universally (not just to keyword-anchored condition parens) — so
  `isReservedPrefix(key[0])`'s own parens are correctly loose,
  `isReservedPrefix( key[0] )`. **Round2's output is spec-correct; round1
  under-pads (the actual bug).** Reproducible only in file context, not a
  minimal single-line repro — appears specifically when the `if`'s
  consequent is itself a multi-line call spanning several lines (`warn(...)`
  / `Object.defineProperty(...)` / `queuePostRenderEffect(...)`), suggesting
  `enforceComplexityPadding`'s scan misses this inner call's parens in that
  context on round1. Root cause (which phase/exclusion) not yet isolated —
  masked earlier because the comment-reindent fix's affected-files-only
  re-run didn't exercise this shape; the full-corpus re-check surfaced it.
- **`scripts/release.js` — call-wrap/collapse boundary miscalculation.**
  `enforceCallLineBreaking`'s JS/TS-only single-argument fits-check
  (`MiscRuleCurly.java`, the nestjs/nest `join(...)` fix,
  `real_code_regressions_85`) measures the candidate collapsed line's length
  *before* `JsTsDeclarationAlignmentRule`'s later column-alignment pass
  runs. For `const res = await fetch(\`...\`)`: round1 measures against the
  unaligned prefix (fits at exactly 100 chars, collapses); round2 measures
  against the same line after sibling declarations in its alignment group
  (`const branch = ...` / `const data = ...`) have had their `=` padded
  (`res    =` instead of `res =`), pushing it past 100 chars (no longer
  fits). The fits-check runs before alignment padding, so its estimate
  doesn't account for padding a sibling declaration will add. Real fix
  needs either running the fits-check after declaration-alignment, or
  having declaration-alignment account for calls fits-check-collapsed right
  at the boundary — cross-pass-ordering fix, out of scope without dedicated
  follow-up.

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

### `lodash/lodash` dogfood pass — DONE

Repo: `lodash/lodash` (`/tmp/lodash`, HEAD `a666ba5`, package.json version
`4.18.1`). This checkout's tree is a single large `lodash.js` (17259
lines) plus build tooling/tests, **not** the per-function-file layout the
Test-Fixture Repos note originally anticipated (that split only exists in
older tags/derived packages). In-scope corpus: 27 real `.js` files
(`lodash.js`, `fp/*.js` [4], `lib/common/*.js`/`lib/fp/*.js`/
`lib/main/*.js` [11 build-tooling files], `test/test.js`/`test-fp.js`/
`remove.js`/`asset/*.js`/`playwright-runner.spec.js`, `perf/*.js`/
`perf/asset/perf-ui.js`, `.markdown-doctest-setup.js`,
`playwright.config.js`) — `dist/*` and `vendor/*` (third-party vendor code)
excluded per `STATE_COMMON.md`'s file-exclusion conventions. 50983 total
lines across the 27 files.

**Baseline:** `node --check` 27/27 pass; a throwaway TS-compiler-API
parse-only script (same idiom as `js_ts_content_diff.js`) also confirmed
27/27 zero pre-existing parse errors.

**Round1** (all 27 files): zero crashes, 27/27 formatted; `node --check`
27/27 pass on round1 output; same parse script 27/27 clean — no new syntax
errors introduced.

**Idempotency (round1→round2):** 26/27 byte-identical; **1 file differs**,
`lodash.js`, in the same "switch-case fallthrough one-liner" shape already
tracked as a known, pre-existing, deferred issue (see "Known open issues" —
`utils.ts` entry from the `vuejs/core` pass): a long collapsed `case A: /*
FALL-THROUGH */ ... case Z: return foo(a, b);` line (`initCloneByTag`'s
typed-array branch, 196 chars, over `line-length-limit`) is left unwrapped
by `SwitchRule`'s fallthrough render on round1, then `enforceCallLine
Breaking`/complexity-padding wraps the trailing call's arguments on round2
— the same `SwitchRule`/case-grid vs. call-wrap-fits-check ordering class
of bug. `SwitchRule` is shared, core C/C++/Java-owned logic (not
JS/TS-specific) — not re-root-caused or fixed here, consistent with the
`vuejs/core` pass leaving its analogous issue open. Recorded as a second
confirming data point, not a new bug entry.

**Content-preservation** (`js_ts_content_diff.js`, original vs. round1):
17/27 files reported "MISMATCH", but every flagged line across all 17
decomposes into two already-understood, intentional, non-lossy
transformations the checker's canonicalization doesn't yet tolerate
(confirmed by manual inspection, not real content loss):

- **Comment trailing-period stripping** (`normalize-comment-end-period=on`'s
  documented `stripSoleTrailingPeriod`/`stripSoleTrailingPeriodAcrossLines`
  behavior) — every flagged comment pair differs only by a trailing `.`
  present in the original, absent in the formatted text. The checker
  already tolerates a lowercase-only diff but was never extended to this
  transformation.
- **§10 single-expression-block brace omission** (existing behavior, not
  JS/TS-specific) — every flagged "non-import top-level statement" diff
  traces to a single-statement `if (...) { body; }`/`while (...) { body; }`
  rendered brace-less. Confirmed directly for `fp/_baseConvert.js`
  (`cloneArray`'s `while (length--) { result[length] = array[length]; }` →
  `while (length--) result[length] = array[length];`; `wrapImmutable`'s `if
  (!length) { return; }` → `if (!length) return;`) and `fp/_mapping.js` (an
  `if/else` pair in its IIFE) — no tokens added/removed/reordered beyond the
  brace omission. (JSDoc tags parse as real AST nodes even for `.js` files,
  which is why some canonicalized text includes JSDoc tag words — a
  pre-existing checker characteristic, not a false positive since both
  sides include the same tokens.)

No other content-preservation category (dropped statement, reordered code,
corrupted comment wording) appeared anywhere in the corpus. Both
transformations are candidates for a future checker tolerance update
(documented, not changed, since this session's task was dogfooding the
formatter) — explicitly NOT formatter bugs.

**Verdict: DONE.** Zero new formatter bugs found. The one idempotency diff
is a confirming recurrence of the already-tracked `SwitchRule`
case-fallthrough issue (not new), left unfixed for the same reason as the
`vuejs/core` pass (shared C/C++/Java-owned logic, out of scope for a
targeted JS/TS fix). Baseline, round1, round1↔round2 idempotency (26/27
clean, 1 known-issue recurrence), and content-preservation are all
accounted for; no further full corpus re-run needed.

**Checker subsequently improved, corpus re-verified (follow-up session):**
`js_ts_content_diff.js` gained tolerance for both false-positive classes
above (trailing-period stripping; single-statement brace omission for any
control-flow construct) — see "Dogfood Output Validation" above — plus a
third class (JSDoc-as-AST-child double-counting raw un-stripped comment
text) found/fixed during re-verification. Re-running across all 27 files
after all three fixes: **22/27 clean** (up from 10/27). Remaining 5 files
decompose into two further false-positive classes, confirmed by direct
token-level inspection as intentional, non-lossy formatter behavior — left
unfixed as out of scope for this task's two specified tolerances:

- **3 files** (`lib/fp/build-doc.js`, `lib/fp/build-modules.js`,
  `lib/main/build-modules.js`) — bare single-param arrow functions gaining
  parens (`chunk => ...` becomes `(chunk) => ...`, `enforceArrowFunction
  ParameterParens`, STYLE_JS_TS.md §6, documented behavior). Confirmed via
  opcode diff of canonicalized token streams — only change is the added
  `(`/`)`.
- **2 files** (`perf/perf.js`, `test/test.js`) — **not a formatter bug**:
  `STYLE.md` §4 mandates pre-increment (`++i`) except when post-form is
  actually required; a standalone expression statement or unused for-loop
  increment is exactly the case where post-form isn't required, so
  `index++`/`count++` correctly becomes `++index`/`++count` (confirmed at
  `perf/perf.js:213`). Same shape as the arrow-param class — checker
  tolerance gap, not a new bug. (Accompanying lost space after `for`/`if`
  in the same statements is a separate, unrelated cosmetic detail not
  investigated here.)

Verification pairs for the checker fix (six hand-crafted good/bad pairs,
`.js`+`.ts`) are in this session's scratchpad, not promoted to permanent
`test/` fixtures since `js_ts_content_diff.js` is a dogfood-validation
tool, not part of `make test`.

**TODO (low priority, not currently queued):** teach `js_ts_content_diff.js`
to tolerate the two remaining false-positive classes above — (a)
`enforceArrowFunctionParameterParens`-added parens on a bare single arrow
param, (b) `STYLE.md` §4-mandated post-to-pre increment/decrement rewrite on
a standalone/unused-value expression. Both are cosmetic gaps in the
checker's tolerance list, not formatter defects, so this only reduces
future dogfood-session triage noise — not urgent, do whenever convenient.

### Known false positives (no source change needed, fixture-only)

- A spurious-looking blank line after a class's opening `{` in older `.js`
  fixture drafts, and a doubled trailing space before a one-liner getter
  body's closing `}` — both confirmed correct, existing behavior
  (STYLE.md §7 named-construct blank line; `GetterSetterRuleCurly`'s
  group-width body padding), matching passing C++/Java/Kotlin fixtures
  byte-for-byte. Only the stale hand-authored `.js` draft fixtures were
  wrong — resolve by regenerating them during Next Steps 1.
