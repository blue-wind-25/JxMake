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

**`js_ts_content_diff.js`** — a content-preservation checker for JS/TS,
modeled on `java_content_diff.java`/`kotlin_content_diff.java` (same
reasoning, applied via the TypeScript compiler API instead of javac/PSI).
One script handles both `.js` and `.ts` — `ts.createSourceFile` parses
plain JS fine, same idiom every other Node-based `*_sc.js`/`*_content_diff.py`
tool in this repo uses (see `STATE_DATA_FORMATS.md`'s `css_content_diff.py`/
`xml_content_diff.py` write-ups for the established pattern this follows).
Parses both original and formatted files to a real AST and compares:
top-level import statements (`ImportDeclaration`/`ImportEqualsDeclaration`)
as an order-tolerant MULTISET (`js-import-order` legitimately reorders/sorts
them); every other top-level statement/declaration IN ORIGINAL RELATIVE
ORDER, via a leaf-token canonicalization (every terminal token's text —
identifiers, keywords, literals, punctuation — joined with single spaces,
whitespace collapsed, so declaration-alignment column padding and pure
reindentation are never flagged); comments as a MULTISET, whitespace-
normalized AND lowercased (a case-only diff is expected
`normalize-comment-start-case` behavior per every other content-diff tool's
precedent, not a bug).

**Gotcha (same shape as `kotlin_content_diff.java`'s PSI one):** TypeScript's
AST does not attach comments as tree nodes — `node.getChildren()` never
yields them regardless of traversal strategy. Comments are recovered
separately from the raw source text via `ts.getLeadingCommentRanges`,
scanned at every node's `getFullStart()` position (plus position 0 and
end-of-file, to also catch a comment before the very first token or a
trailing comment with nothing after it), deduplicated by `[pos, end)` since
the same comment range can be reached from more than one scan point (e.g.
simultaneously as one statement's trailing comment and the next statement's
leading one).

**`typescript` package version gotcha:** `~/mynpm/node_modules` initially
had nothing installed under `typescript`; `npm install --prefix ~/mynpm
typescript` (no version pin) installed **`typescript@7.0.2`**, the new
native tsgo-based rewrite, which exports only `version`/`versionMajorMinor`
from its default entry point — no `createSourceFile`, no
`getLeadingCommentRanges`, none of the classic compiler API this tool needs.
Re-installed pinned to the classic 5.x line (`npm install --prefix ~/mynpm
typescript@5`, landed `5.9.3`) which has the full classic API. Anyone
reusing `~/mynpm/node_modules/typescript` for compiler-API work should
verify `typeof ts.createSourceFile === 'function'` before trusting it, in
case a future unpinned reinstall pulls `7.x` again.

Exit 0 if content is preserved, 1 with a description of each mismatch
otherwise, 2 on usage error. No build step — plain `.js`, run directly:

```bash
export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules:~/mynpm/node_modules
export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH
node tools/syntax_checker/js_ts_content_diff.js <original.(js|ts)> <formatted.(js|ts)>
```

Verified against hand-crafted pairs for both extensions before being
trusted: a good pair (import-sort + reindent + comment recapitalization) —
passed clean for both `.js` and `.ts`; a bad pair with a dropped statement
(dropped the final `const`/interface-consumer statement) — flagged
`non-import top-level statement count changed` for both; a bad pair with a
corrupted comment (recapitalized *and* reworded, not just cased) — flagged
`comments: present in original, missing from formatted` /
`present in formatted, missing from original` for both. All six cases (3
pairs × 2 extensions) behaved as expected. 208 lines.

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
  inside a destructuring pattern (`{ id, // note\n name }`) was silently
  dropped on a second pass because `significantOnly()` strips comment
  tokens, so `parseDestructuringDeclaration`'s pattern scan never saw it.
  Fixed by scanning raw (comment-bearing) tokens for an interior comment and
  bailing (leaving multi-line form untouched) if found.
- **ASI-vs-declaration-alignment-grid phase-ordering bug** — `FormatterCurly
  .formatOne`'s Phase 0 ran the declaration-alignment grid pass *before*
  `enforceSemicolonInsertion`, so any ASI-reliant declaration (no explicit
  `;`) was invisible to `parseDeclaration`'s hard requirement for a literal
  `;` — it and its whole alignment group silently fell back to raw input.
  Fixed by moving `enforceSemicolonInsertion` before
  `ScopePipelineCurly.process()`.
- **Array-destructuring `,`→`...` missing space** — `[first, second,
  ...others]` rendered as `[first, second,... others]`. Root cause:
  `MiscRuleCore.parseAssignment` (older, JS/TS-unaware) misparsed
  `const [first, second, ...others] = expr;` as a plain assignment, letting
  it re-splice a statement the declaration-alignment pass had already
  rendered correctly, via its own `...`-tight-both-sides rule (object
  destructuring unaffected — `{` isn't a `parseAssignment`-recognized LHS).
  Fixed by adding a `const`/`let`/`var` bail-out to `parseAssignment`,
  mirroring the existing C++ `auto [a, b] = expr;` bail-out.
- **`js_combined_out.js` fixture regenerated** — the fixture wrongly
  expected a multi-line arrow-function initializer to join the alignment
  grid; `JsTsDeclarationAlignmentRule` deliberately excludes multi-line
  block/lambda initializers (same precedent as Kotlin). Confirmed with user:
  kept the design, regenerated the fixture.

### Resolved this session (ts_combined/ts_comments activation)

- **`Map<string,number>` ASI bug** — `GENERIC_SAFE_KEYWORDS` didn't include
  TS primitive type keywords (`string`/`number`/`boolean`/`any`/`unknown`/
  `never`/`object`/`undefined`/`null`, tokenized as KEYWORD not
  IDENTIFIER). A second type argument in a generic list invalidated the
  whole `<...>` tracking before the matching `>`, breaking
  `CONTINUATION_OPS`-based ASI logic downstream. Fixed by extending
  `GENERIC_SAFE_KEYWORDS`.
- **Enum last-member (no trailing comma) bug** — `parseEnumMembers`'s
  value-scan loop bailed the whole enum on any depth-0 NEWLINE (the common
  last-member case, `Pending = 3\n}`). Changed to `break` instead.
- **Generic-argument comma spacing** — no pass existed for spacing after
  `,` inside a generic argument list. Added
  `enforceGenericArgumentCommaSpacing` (flat scan tracking `angleDepth`).
- **Union-type continuation indent — new pass** — no existing pass
  re-indented continuation lines of a multi-line `type X = A | B | C;`
  alias (`parseTypeAlias` bails on multi-line initializers). Added
  `enforceUnionTypeContinuationIndent`, column-aligning continuation lines
  under the RHS's first token. One bug fixed in the new pass during
  `ts_comments` triage: the RHS depth-scan bailed on ANY comment anywhere
  in the span, including a legitimate trailing comment on an interior
  operand line — narrowed the bailout to frozen tokens only.
- **Class-field `:`/`=` alignment grid — new feature, unimplemented
  before this session.** Neither `JsTsDeclarationAlignmentRule` nor
  `enforceInterfaceTypeAliasMemberColonAlignment` touched class fields.
  Added `enforceClassFieldAlignmentGrid`: parses runs of simple typed class
  fields into alignment groups matching the existing interface/enum
  convention. Two bugs fixed in `rewriteClassFieldGroups`: double-indented
  first field of each group (raw-copy loop copied WHITESPACE tokens too,
  fixed to only copy NEWLINE); duplicate blank line before a group's first
  field with a leading comment (fixed by tracking
  `leadingCommentsStartIdx`).
- **Fixture-authoring corrections (not code bugs), `ts_combined_out.ts`:**
  an empty `{}` class body was wrongly given a `// class Container` closing
  comment (never happens by design) — removed; a 95-char decorator+class
  line was wrongly split (under the 100-char limit) — merged back;
  `Widget`'s field block was inconsistently unaligned vs. sibling `Config`
  — aligned to match.
- **Fixture-authoring corrections (not code bugs), `ts_comments_out.ts`:**
  confirmed against pre-existing (unmodified) `rewriteEnumBody` behavior
  that double-space-before-trailing-comment and group-wide name-padding are
  established, already-tested behavior — the fixture's single-space
  instances and missing padding were fixture mistakes, corrected to match.
  `Widget`'s class-field block got the same alignment update as
  `ts_combined_out.ts`'s.

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

- **ASI leading-continuation-operator/comma bug** — `maybeInsertSemicolon`
  only looked at the *previous* line's trailing token, never the *next*
  line's leading token. Method-chaining with the operator leading the
  continuation line (`request(app)\n.get('/')\n.expect(...)`, ubiquitous in
  Express's Mocha suite) or a comma-first multi-declarator list both got a
  bogus `;` inserted mid-chain/mid-list, corrupting valid JS. Fixed by
  adding a `LEADING_CONTINUATION_OPS` set (narrower than
  `CONTINUATION_OPS` — excludes unary-legal `+`/`-`/etc.) plus a
  leading-`,` check, both via next-significant-token lookahead.
- **No JS/TS regex-literal tokenizing at all** — every `/` that wasn't
  `//`/`/*` fell through to the generic division-operator scan, zero regex
  recognition. Usually harmless by coincidence, but a real-world regex
  containing a `"` inside a bracketed character class
  (`/^(?:W\/)?"[^"]+"$/`, from `test/res.sendFile.js`'s ETag assertions)
  had its `"` mistaken for a string-literal start, corrupting brace/paren/
  statement tracking for the rest of the enclosing statement. Fixed by
  adding `TokenizerCurly.emitRegexLiteral` (opaque STRING-typed token, same
  posture as `emitTemplateLiteral`) and `isRegexLiteralAllowedHere`
  (classic regex-vs-division disambiguation).

Final numbers (full 141-file corpus, both bugs fixed): forward pass zero
crashes; round1→round2 `diff -r` empty; `node --check` 141/141 pass (was
93/141 failing before the fix — the comma-first import-list idiom alone
appears in nearly every `test/*.js` file). `require()`-based semantic check
went further than syntax-only: `npm install --prefix <scratch> express`
supplied a dependency tree; `require('./index.js')` on the formatted
`lib/express.js` tree returned the expected function, `express()` produced
a working `app`, and a real `app.get`/`app.listen`/HTTP GET round-tripped
end-to-end. Formatted `test/` files run directly under `mocha`: a clean
file with no static-file-serving dependency
(`test/req.host.js`+`req.hostname.js`+`Route.js`) passed 35/35, confirming
the formatted `require()` graph and Mocha harness genuinely execute.
`test/res.sendFile.js` (the regex-bug file, post-fix) also loads and runs
under Mocha — 42 assertions fail, but confirmed an environment limitation
(static-file 404s from an unpinned `send`/`serve-static` version, reproduces
identically against the unformatted checkout), not formatter-induced,
documented per the task's honesty requirement.

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
  every argument is a bare dotted member-access expression got
  rejoined/rewrapped. Root-caused with debug prints at each phase boundary:
  not in `collapseTokensToOneLine` itself, but the *second*
  `enforceCallLineBreaking` pass (`FormatterCurly` re-runs it twice) took a
  different render path — `parseSignature` misparsed the argument list as a
  C/C++/Java-style "type name" forward-declaration parameter list (type
  `options.`, name `provideInjectionTokensFrom`), the same misparse class
  already guarded for Kotlin only (`sigForRender`'s doc comment, RobotCoding
  `gui_frontend_android` bug) but never extended to JS/TS. Fixed by forcing
  `sigForRender` to `null` for JS/TS too, same reasoning as Kotlin (neither
  language has a prototype-only forward-declaration shape). Also hardened
  `collapseTokensToOneLine` (defense-in-depth) to never insert a space
  immediately before/after a tight `.`/`?.` token. New fixture
  `test/real_code_regressions_81_{inp,out}.ts`. `make test`: 130/130
  forward + idempotency, zero regressions. Confirmed against all 5
  originally-reported nestjs/nest files: round1→round2 diff now empty for
  this bug's shape (a separate, pre-existing, unrelated general-reindent
  idempotency gap — see `STATE_COMMON.md`'s Architectural TODOs — still
  produces indentation-only diff noise elsewhere; not this bug). `node
  --check` (Node 24) passes on all 5 round1 outputs.

Third bug found and fixed:

- **Content duplication in `enforceClassFieldAlignmentGrid` — nested
  `CLASS` braces not handled** — `packages/core/injector/module.ts` (a
  `return class extends ModuleRef {...}` nested inside an outer class
  method) and `packages/core/middleware/builder.ts` (similar) had entire
  method/constructor blocks duplicated in round2. Root cause: the method
  collects every `CLASS`-classified brace into a flat `classOpens` list and
  sweeps with a single linear `cursor`, assuming every selected class span
  is disjoint — a nested class breaks that assumption: the outer class's
  own rewrite already copies the inner class span byte-for-byte (as an
  unrecognized member), so the loop's later top-level entry for the inner
  class re-appended its content a second time and walked `cursor` backward,
  causing the final raw-copy-to-EOF loop to re-emit everything a second
  time too. Fixed by filtering `classOpens` to only the outermost class
  brace at each nesting level (nested class fields simply don't get
  alignment-grid treatment — consistent with this method's conservative
  posture, not a regression). New fixture
  `test/real_code_regressions_82_{inp,out}.ts`. `make test`: 131/131
  forward + idempotency, zero regressions. Confirmed against both
  originally-reported files: round1→round2 diff now empty. `node --check`
  (Node 24) passes on both round1 outputs.

Fourth bug found and fixed:

- **Comment-continuation-indent drift / arbitrary-deep-indent corruption on
  an object-shaped `type X = { ... } & Y;` intersection alias** —
  `packages/core/inspector/interfaces/edge.interface.ts`. Root cause:
  `enforceUnionTypeContinuationIndent` re-indents *every* `NEWLINE` from a
  multi-line `type NAME = ...;` RHS's start through the terminating `;` to
  the RHS's own column, with no bracket-depth tracking. Harmless for a
  plain union list, but an intersection whose left operand is a multi-line
  object-type literal (`{ ... } & Y`) has `NEWLINE`s nested deep inside the
  object body's own already-correct indentation — those got
  force-reindented too, blowing every member out to an arbitrarily deep
  column. Symptom compounded across passes ("grows between round1 and
  round2") because a leading JSDoc comment shielded interior lines on the
  first pass only. Fixed by tracking bracket depth and only re-indenting a
  `NEWLINE` at the union/intersection's own top level (depth 0). New
  fixture `test/real_code_regressions_84_{inp,out}.ts`. `make test`:
  133/133 forward + idempotency, zero regressions. Confirmed against the
  originally-reported file: round1→round2 diff now empty. `node --check`
  (Node 24) passes on the round1 output. (Debug-print methodology: dumped
  `text` after `ScopePipelineCurly.process` and after
  `enforceInterfaceTypeAliasMemberColonAlignment` to localize the
  corruption to `enforceUnionTypeContinuationIndent`.)

Fifth bug found and fixed (all four originally-reported bugs now resolved):

- **`join(...)` call-wrap/collapse non-idempotency** —
  `integration/repl/e2e/repl-process.spec.ts`'s `const localPackageResolver
  = join(workspaceRoot, '...')` (exactly 100 chars collapsed, right at
  `lineLengthLimit`). Root cause: `renderCallCandidate`'s multi-line-source
  branch always preserved the call's original per-line argument grouping
  unconditionally, with no fits-check of its own (unlike the sibling
  single-line branch) — a call that had ever been wrapped stayed wrapped
  forever even once it fit on one line, while the same call written fresh
  on one line collapsed correctly, so a call right at the boundary flipped
  between forms across repeated passes. Fixed by adding the same
  fits-check, scoped to JS/TS only (widening to every language
  unconditionally regressed `real_code_regressions_1`/C++): measures the
  actual tight single-line candidate directly (rather than the sibling
  branch's `collapseToOneLine`, which overestimates length by turning a
  newline into a phantom space) and collapses whenever it fits, dropping
  any dangling trailing empty argument group first (`splitTopLevelCommas`
  doesn't drop a trailing comma's empty tail itself — found via an
  intermediate regression in fixture 81, whose own trailing-comma call
  exposed it). New fixture `test/real_code_regressions_85_{inp,out}.ts`.
  Also required updating `real_code_regressions_81_out.ts`'s expected
  output (its call now correctly collapses too — the old shape had been an
  artifact of this same bug). `make test`: 134/134 forward + idempotency,
  zero regressions beyond the intentional fixture-81 update. Confirmed
  against the originally-reported file: round1→round2 diff now empty.
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
Open Questions below). **This narrative below documents the pass in
chronological, as-found order; see "`vuejs/core` dogfood pass — DONE" further
down for the final, completed outcome.**

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

### `vuejs/core` dogfood pass — DONE

Continuing from the 12-file idempotency gap above, a later leg of this same
overall `vuejs/core` session (driven primarily by the `tsc` typecheck pass,
methodology step 5, rather than round1/round2 diffing alone — `tsc` syntax
errors are a much sharper signal than a byte diff for exactly where a
generic-clause/statement-boundary tracking bug corrupts output) found and
fixed the `if( ... )` nested-call paren-padding order-dependency and the
`scripts/release.js` call-wrap/collapse boundary bug listed above, plus nine
further, previously-unknown bugs surfaced by running `tsc --noEmit` against
the full round1-formatted 514-file tree and comparing its error count/lines
against the unmodified original tree (0 errors) — every new error line was
root-caused as a real formatter defect (not a real vuejs/core type error)
and fixed:

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
4. A parenthesized ternary sub-expression's own `:` misclassified as a
   return-type colon because the preceding `)` closes a plain grouping
   paren, not a real function signature — new `isGroupingExpressionParen`
   helper in `JsTsSpecificRule.isTypeColonAt` — `real_code_regressions_105`.
5. `typeof`/`keyof` as `prevPrev` not recognized by
   `enforceArrowFunctionParameterParens`'s bail-out check (only `is` was),
   wrongly wrapping a type-predicate identifier in parens
   (`key is keyof typeof val => ...`) — `real_code_regressions_105`.
6. A trailing type-annotation `:` wrapping its type to the next line got a
   bogus `;` inserted right after it — `needsSemicolonAfter`'s intended
   `isPunct(t, ":")` guard never matched (`:` tokenizes as OP); fixed by
   adding `":"` to `CONTINUATION_OPS` instead — `real_code_regressions_105`.
7. `isGenericSafeToken`'s TS-safe OP list missing `=>`/`...` (a function-type
   generic argument like `Map<(...args: any[]) => void, Handler>` lost its
   outer `<...>` tracking); `...` had to be gated `lang.isTs`-only after it
   was found to regress C++ test 53's variadic-template spacing
   (`Args...`) when added unconditionally — `real_code_regressions_105`.
8. A standalone TS function-type parameter list (`(...args: any[]) => void`)
   got padded/tightened like an arbitrary grouping paren by
   `MiscRuleCore.enforceComplexityPadding`'s generic non-identifier-preceded
   `(` branch; fixed with a `lang.isTs`-gated exception recognizing a
   matching `)` immediately followed by `=>` — `real_code_regressions_105`.
9. `nestedBraceDepth`'s guard (item 2 above) only covered the nested `{`/`}`
   delimiters themselves, not tokens *inside* them —
   `Record<string, { local: string; default?: Expression }>`'s member name
   `default` (a KEYWORD not in `GENERIC_SAFE_KEYWORDS`) and its
   member-separator `;` both still reached the outer `<...>`'s invalidation
   checks and wiped it; a mapped-type object as a generic argument also
   needed `ANGLE_BRACKET_OPEN` added to `classifyBraces`'s `isValue`
   whitelist — `real_code_regressions_105`.
10. `GENERIC_SAFE_KEYWORDS` missing `typeof` — found on the final full-corpus
    tsc rerun after fixture 105 landed. `Record<(typeof identityMethods)
    [number], any>` produced a bogus `;` before the closing `>`; the
    single-line `let server: ReturnType<typeof createServer>` form was
    worse — losing `<...>` tracking left the `>` a plain OP token, which
    defeated `enforceSemicolonInsertion`'s statement-boundary detection
    entirely and merged the following `beforeAll(...)` statement onto the
    same line — `real_code_regressions_107`.

**Final verification** (clean build, `make test`: 156/156 forward + 156/156
idempotency): full 514-file `vuejs/core` round1 forward pass — zero
crashes/errors. Round1→round2 idempotency — **only one file still
differs**, `packages/compiler-sfc/src/script/utils.ts`, a switch-case
fallthrough (consecutive `case` labels sharing one body) non-idempotency
**confirmed pre-existing on the unmodified codebase** (verified via
`git stash`/rebuild/retest/`git stash pop` — the original code produces a
different but equally-broken symptom on this same file, `case
'StringLiteral':;` with a bogus semicolon and lost blank-line collapsing;
this session's code instead shrinks one space per round-trip pass). This is
explicitly **not fixed** as part of this dogfood pass — tracked below as a
known, separately-owned open issue. `tsc --noEmit` on the round1-formatted
tree: **1 error**, `packages/compiler-sfc/src/script/utils.ts(66,3): error
TS1128: Declaration or statement expected` — the same pre-existing
switch-case bug, and the *only* remaining error (0 on the unmodified
original tree, 1 here, both attributable to that one pre-existing,
out-of-scope defect). No other file shows any new formatter-induced tsc
error.

**Dogfood pass verdict: DONE.** All formatter bugs found via this
`vuejs/core` session (13 total: 2 original comment-reindent-era fixes + 2
`if`-padding/`release.js` fixes + the 9-item list above) are fixed and
covered by permanent fixtures. The one remaining idempotency/tsc gap
(`utils.ts` switch-case fallthrough) is confirmed pre-existing, not
formatter-regression, and out of scope for this pass — see "Known open
issues (pre-existing, deferred)" below for its tracking entry and a second,
separately-discovered spacing-only gap in the single-declarator colon path.

### Known open issues (pre-existing, deferred — not part of `vuejs/core` DONE scope)

- **`utils.ts` switch-case fallthrough non-idempotency** (see immediately
  above) — root-caused (case-label-fallthrough one-liner-collapse/alignment
  feature interacting badly with consecutive `case` labels sharing a single
  body) but confirmed pre-existing via `git stash` comparison against the
  unmodified codebase, not introduced by any fix in this session. Needs its
  own dedicated fix + fixture in a future session. **Second confirming
  recurrence found in the `lodash/lodash` dogfood pass** (see below): a
  different but related symptom — `fp/_baseConvert.js`'s
  `initCloneByTag`'s typed-array fallthrough case body (`case uint8Tag :
  /* FALL-THROUGH */ ... case uint32Tag : return cloneTypedArray(object,
  isDeep);`, 196 chars) round1 leaves unwrapped past `line-length-limit`,
  round2 wraps the trailing call's arguments — a `SwitchRule` case-grid vs.
  generic call-wrap-fits-check ordering gap. Still not fixed (shared
  C/C++/Java-owned `SwitchRule` logic, deliberately left to its own future
  session per both dogfood passes' scoping).
- **Single-declarator colon spacing**: `const x: number = 1;` renders as
  `const x : number = 1;` (space inserted before the colon) even at plain
  top level with no function-type involved. Confirmed to be
  `JsTsDeclarationAlignmentRule`'s single-declarator grid-alignment handling
  by its own design comment (`classifyTypeColons` deliberately suppresses
  its own colon-spacing pass for a single-declarator `let`/`const`/`var`
  statement, deferring to the alignment rule, which does not fully match
  `classifyTypeColons`'s spacing for the ungridded single-declarator case).
  Spacing-only — does not produce a tsc error — confirmed widespread across
  the round1 corpus output via `grep -rn "const [a-zA-Z_]* : "`. Left
  unaddressed pending a future session; not blocking `vuejs/core` DONE
  status since it produces no compile error and is a pre-existing gap in
  `JsTsDeclarationAlignmentRule`, not a regression from this session's
  generic-tracking fixes.

### `lodash/lodash` dogfood pass — DONE

Repo: `lodash/lodash` (fresh shallow clone, `/tmp/lodash`, HEAD `a666ba5`,
package.json version `4.18.1`). This checkout's current tree is a single
large `lodash.js` (17259 lines) plus build tooling/tests, **not** the
per-function-file layout the Test-Fixture Repos note originally anticipated
— that split lives only in older tags/derived packages, not this repo's
current `main`. In-scope corpus: 27 real `.js` files (`lodash.js`, `fp/*.js`
[4], `lib/common/*.js`/`lib/fp/*.js`/`lib/main/*.js` [11 build-tooling
files], `test/test.js`/`test-fp.js`/`remove.js`/`asset/*.js`/
`playwright-runner.spec.js`, `perf/*.js`/`perf/asset/perf-ui.js`,
`.markdown-doctest-setup.js`, `playwright.config.js`) — `dist/*` (generated
minified bundles) and `vendor/*` (backbone/underscore/firebug-lite/json-js,
third-party vendor code) excluded per `STATE_COMMON.md`'s file-exclusion
conventions. 50983 total lines across the 27 files.

**Baseline:** `node --check` 27/27 pass; a throwaway TS-compiler-API
parse-only script (`ts.createSourceFile` + checking `sf.parseDiagnostics`,
same idiom as `js_ts_content_diff.js`, not a permanent tool) also confirmed
27/27 zero pre-existing parse errors.

**Round1** (one batch invocation, `--preserve-tree --root /tmp/lodash --out
/tmp/round1-lodash`, all 27 files): zero crashes, 27/27 formatted. `node
--check` 27/27 pass on round1 output; the same TS-compiler-API parse script
also 27/27 clean on round1 output — no new syntax errors introduced.

**Idempotency (round1→round2):** 26/27 files byte-identical; **1 file
differs**, `lodash.js`, in exactly the same "switch-case fallthrough
one-liner" shape already tracked below as a known, pre-existing, deferred
issue (see "Known open issues" — `utils.ts` entry from the `vuejs/core`
pass): a long collapsed `case A: /* FALL-THROUGH */ ... case Z: return
foo(a, b);` line (`initCloneByTag`'s typed-array branch, 196 chars, well
over `line-length-limit`) is left unwrapped by `SwitchRule`'s fallthrough
render on round1, then `enforceCallLineBreaking`/complexity-padding wraps
the trailing call's arguments onto their own lines on round2 (a boundary/
ordering gap between the case-fallthrough grid render and the generic
call-wrap fits-check, structurally the same class of bug as the already-
recorded `SwitchRule`/case-grid interaction). `SwitchRule` is shared,
core C/C++/Java-owned logic (not JS/TS-specific) — not re-root-caused or
fixed in this session, consistent with the `vuejs/core` pass leaving its
own analogous `utils.ts` switch-case issue open rather than modifying
shared switch-case logic under this job. Not a new bug class; recorded here
as a second confirming data point for the existing open issue, not a new
entry.

**Content-preservation** (`js_ts_content_diff.js`, every file's original vs.
round1 output): 17/27 files reported as "MISMATCH", but every single flagged
line across all 17 decomposes into exactly two already-understood,
intentional, non-lossy formatting transformations that this checker's
current comment/statement canonicalization doesn't tolerate (not real
content loss, confirmed by manual inspection of every distinct flagged
statement index and every flagged comment):

- **Comment trailing-period stripping** (`normalize-comment-end-period=on`'s
  documented `stripSoleTrailingPeriod`/`stripSoleTrailingPeriodAcrossLines`
  behavior, `MiscRuleCore.java`) — every flagged comment pair in this run is
  identical except for a trailing `.` present in the original and absent in
  the formatted text (e.g. `"assign aliases."` vs `"assign aliases"`). The
  checker's comment multiset already tolerates a lowercase-only diff
  (`normalize-comment-start-case`) but was never extended to also tolerate
  this period-stripping transformation, so it flags an intentional,
  documented rule as a mismatch. Confirmed for every one of this run's
  flagged comments — no comment text was altered beyond the trailing period.
- **§10 single-expression-block brace omission** (`STYLE.md` §10, existing
  behavior, not JS/TS-specific) — every flagged "non-import top-level
  statement #N structure/content differs" traces to a single-statement
  `if (...) { body; }` / `while (...) { body; }` being rendered brace-less
  (`if (...) body;` / `while (...) body;`) per this pre-existing style rule.
  Confirmed directly for `fp/_baseConvert.js` (`cloneArray`'s `while
  (length--) { result[length] = array[length]; }` →  `while (length--)
  result[length] = array[length];`; `wrapImmutable`'s `if (!length) { return;
  } ` → `if (!length) return;`) and `fp/_mapping.js` (an `if/else` pair
  inside `_mapping.js`'s IIFE, same shape) by diffing each flagged
  statement's canonicalized text with its leading-JSDoc-comment span
  stripped — the only remaining difference in every case was exactly this
  brace-omission transformation, no tokens added/removed/reordered beyond
  it. (JSDoc `@private`/`@param`/`@returns` tags are parsed as real AST
  nodes by the TS compiler API even for `.js` files, which is why some
  flagged statements' canonicalized text includes JSDoc tag words — a
  pre-existing checker characteristic, not new, and not a false positive on
  its own since both sides include the same JSDoc tag tokens.)

No other content-preservation category (dropped statement, reordered
non-import code, corrupted comment wording) appeared anywhere in the 27-file
corpus. Both transformations above are candidates for a future
`js_ts_content_diff.js` tolerance update (documented here rather than
changed now, since the task for this session was dogfooding the formatter,
not the checker), but are explicitly NOT formatter bugs.

**Verdict: DONE.** Zero new formatter bugs found. The corpus turned out
clean per this task's "0-3 distinct bugs" branch — the one idempotency
diff is a confirming recurrence of the already-tracked, pre-existing,
deferred `SwitchRule` case-fallthrough issue (see "Known open issues"
below), not a new bug, and is left unfixed here for the same reason it was
left unfixed in the `vuejs/core` pass (shared C/C++/Java-owned logic, out of
scope for a targeted JS/TS-job fix without its own dedicated session). No
full corpus re-run is needed beyond what's documented above — baseline,
round1, round1↔round2 idempotency (26/27 clean, 1 known-issue recurrence),
and content-preservation are all accounted for.

### Known false positives (no source change needed, fixture-only)

- A spurious-looking blank line after a class's opening `{` in older `.js`
  fixture drafts, and a doubled trailing space before a one-liner getter
  body's closing `}` — both confirmed correct, existing behavior
  (STYLE.md §7 named-construct blank line; `GetterSetterRuleCurly`'s
  group-width body padding), matching passing C++/Java/Kotlin fixtures
  byte-for-byte. Only the stale hand-authored `.js` draft fixtures were
  wrong — resolve by regenerating them during Next Steps 1.
