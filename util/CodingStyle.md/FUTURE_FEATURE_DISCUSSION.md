# FUTURE_FEATURE_DISCUSSION.md — Future Language/Version Support (Not Scoped)

This file is a standalone discussion note for language or version support that is
**not currently scoped, not started, and not part of any implementation plan.**
It is deliberately **not referenced from README.md, STATE.md, or any other file** —
there is no path by which a normal session would be pointed here, and that is
intentional: this is a place for the user to jot down "worth remembering later"
notes, not a queue for the CLI to discover and act on. If any entry below is ever
actually picked up, its contents should be moved into the normal flow at that
time (a real STATE_NEXT_*.md design file, or checklist items in STATE.md) rather
than treating this file itself as a task list.

No checklist syntax (`[ ]`/`[~]`) is used anywhere in this file, on purpose — an
entry here is a discussion point, not a tracked, scoped piece of work.

---

## Implementation Priority

If any of this is ever picked up, in this order:

1. **C++26** — closest to existing work (STYLE_CPP20.md), mostly additive.
2. **JSON / JSON5, XML** — data/markup formats, grouped together since both are
   small additive rule sets layered on existing bracket/complexity logic, with
   no imperative-language constructs (no functions, no control flow) to design
   around.
3. **JavaScript, TypeScript, CSS, HTML5** — the "web" group. Grouped together
   because HTML5 literally embeds CSS and JavaScript (its formatter dispatches
   to both), so the three are easiest to design as one connected effort rather
   than sequentially.
4. **Python3** — last: its complexity detector and alignment rules are the
   most novel piece of new design work here (no existing non-brace-delimited
   language in the formatter today), so it benefits from having the other six
   done first as reference points.

Nothing in this file is a commitment to implement any of it — this is notes for
later, not a plan.

---

## C++26

**Status:** exploratory discussion only, nothing scoped, nothing started.
**Priority:** 1 (highest).

C++26 brings several new constructs. Most look tractable as additive work,
following the same pattern that added C++17/20/23 support in `STYLE_CPP20.md`
(new keyword columns, new operator spacing rules, or — for several C++20
constructs — no new code at all once checked against actual JAR behavior).
One area looks meaningfully harder:

- **Pack indexing (`T...[i]`)** — likely falls under the existing array-index
  bracket rules (STYLE.md §3.1) with little or no new code.
- **`= delete("reason")`** — trivial; a string literal inside an existing
  construct.
- **Placeholder `_`** — ordinary identifier, no new rule anticipated.
- **Contracts (`pre`/`post`/`contract_assert` attached to a signature)** —
  comparable in shape to STYLE_CPP20.md §2.2's trailing-`requires`-clause
  handling (a clause trailing the `)`, wrapping to its own line if the
  combined line overflows). Expected to be tractable as an additive section.
- **Reflection (`^^`, `[:`, `:]` splicing)** — the one genuinely harder piece.
  These are new tokens the tokenizer does not recognize at all, not new
  keywords added to an existing grammar shape — comparable in kind to the
  Kotlin Step 0 tokenizer work (new `MULTI_CHAR_OPS` entries, longest-prefix-
  first ordering, and a real risk of surfacing latent tokenizer bugs the way
  that session found one in number-literal lexing). `[:`/`:]` also look
  bracket-shaped, so a deliberate decision is needed on whether they interact
  with STYLE.md §3.1's complexity-based bracket-padding rule or stay fully
  opaque. This should get its own dedicated tokenizer-support pass, evidence-
  tested against real reflection code, before any style rule for it is
  trusted — not inferred from the standard's grammar alone.

**Update (Q&A, drafted):** `STYLE_CPP26.md` has been drafted covering all five
constructs above (pack indexing, `= delete("reason")`, placeholder `_`, contracts,
reflection). C++26 itself shipped/finalized 28 March 2026 — confirmed via web search.
Two things worth recording from that check:
- **Trivial relocatability was removed from C++26** during finalization (implementation
  bugs) and deferred to a later standard — deliberately *not* included in
  `STYLE_CPP26.md`.
- **C++29** work only began June 2026 (first WG21 meeting, Brno); a few items are
  adopted into the *draft* (UB catalog — no syntax impact; contract pre/post for
  virtual functions — semantic extension of already-covered contracts syntax, no new
  tokens; `=default` for postfix increment/decrement — the one real syntax addition,
  trivial in shape). Decided **not** to include any C++29 content — "adopted into
  draft" this early isn't the same as frozen, and the trivial-relocatability removal
  above is a live example of C++26 losing a feature even at its finalization stage.
  Revisit only once C++29 itself ships.

**If this is ever picked up — file structure recommendation:** create a new
`STYLE_CPP26.md`, rather than extending `STYLE_CPP20.md`. Two reasons:
1. Java and Kotlin both already split "baseline" from "newer constructs"
   (`STYLE_JAVA.md`→`STYLE_JAVA17.md`, `STYLE_KOTLIN.md`→`STYLE_KOTLIN2.md`,
   each explicitly "read after" its baseline file). `STYLE_CPP20.md` breaks
   that convention by lumping three revisions (17/20/23) into one file, but
   those were uniformly small, additive features. Reflection and contracts
   are a larger, more structurally novel feature surface — closer to "new
   language subset" than "a few new keywords" — and fit the split-file
   pattern better than the lumped one.
2. `STYLE_CPP20.md`'s constructs are fully implemented and verified (STATE.md
   Task G cross-checked them against actual JAR behavior). Keeping it frozen
   and putting C++26 in its own file avoids mixing the riskier, unverified
   reflection/contracts work into a file that is otherwise done — same
   reasoning STATE_KOTLIN.md gives for staying self-contained rather than
   folded into STATE.md.

**File plan:** new `STYLE_CPP26.md`, reads after `STYLE_CPP20.md` (decided above).

**Test-fixture repos** (real reflection code to validate the tokenizer pass
against, since this is the one piece that can't be trusted from the standard's
grammar alone):
- `bloomberg/clang-p2996` — Bloomberg's experimental Clang fork implementing
  P2996 reflection; the most complete open-source implementation, includes
  its own test suite under the compiler's test tree.
- `wrocpp/cpp26-reflection-examples` — blog-series source, small runnable
  `.cpp` files per post, each independently verified to compile against a
  pinned `clang-p2996` build; good bite-sized fixtures.
- `simdjson/experimental_json_builder` — a real library experimenting with
  P2996-based reflection for JSON (de)serialization, non-trivial real usage
  rather than toy examples.
- `stephenberry/glaze` — production JSON/BEVE serialization library with an
  opt-in P2996 reflection backend; larger, more idiomatic real-world usage.

Nothing above should be read as a commitment to implement C++26 support —
this is a note for later, not a plan.

---

## JSON / JSON5

**Status:** exploratory discussion only, nothing scoped, nothing started.
**Priority:** 2.

Supports:
- JSON (RFC 8259)
- JSON5

**Alignment (new, decided via Q&A):** align the `:` between key and value
within an object, the same way STYLE.md §5 column-aligns adjacent simple
variable declarations. Adjacent same-level keys form a group; the group
breaks at a blank line or a comment line (mirrors §5's own group-break
behavior), and also breaks across a nesting-depth change (entering/leaving a
nested object or array). JSON5-specific syntax (unquoted keys, trailing
commas, inline/block comments) doesn't change the alignment logic itself —
comments simply break groups, same as blank lines — but does mean the
tokenizer needs to recognize those productions before any alignment pass can
run over them.

**Complexity/bracket padding:** reuse STYLE.md §3.1's existing tight/loose
bracket rule for arrays and nested objects — an array of atoms stays tight
(`[1, 2, 3]`), an array containing objects/nested arrays goes loose, mirroring
the existing "contains a call" vs. "atoms only" distinction (a JSON object/
array has no function calls, but the same nesting-complexity signal applies).

**File plan:** no new standalone `STYLE_JSON.md`. Folds into a shared
`STYLE_DATA_FORMATS.md` (see XML below) as its own subsection, explicitly
citing which STYLE.md sections it borrows (§3.1, §5-style alignment) rather
than inheriting the whole file — STYLE.md's other ~13 sections (function
signatures, switch formatting, getter/setter grouping, etc.) don't apply to a
data format with no functions or control flow, and a reader skimming
STYLE.md's "applies to all languages" framing shouldn't be misled into
assuming they do.

**Config properties (not yet scoped):** `json-colon-align` (on/off) — toggle
for the §5-style key/value alignment above. Tracked here, same reasoning as
the Python3 config-properties note below — not yet in the real config schema
or a `STATE.md` task.

**Update (Q&A, drafted):** folded into `STYLE_DATA_FORMATS.md` §1 as planned, alongside
XML/CSS/HTML5.

**Test-fixture repos:**
- `json5/json5` — the reference JSON5 implementation; ships its own
  `json5-tests` fixture suite of edge-case `.json5` files.
- `microsoft/vscode` — huge volume of real hand-authored JSON (settings,
  launch configs, extension manifests), heavily nested, good density of
  mixed simple/complex values for the alignment+bracket rules.
- `babel/babel` — real `.babelrc`/JSON5 config usage in a major, actively
  maintained project (one of the JSON5 project's own cited "in the wild"
  adopters).
- `eslint/eslint` — large real-world JSON config surface (rule configs,
  package metadata) with plenty of nested-object/array combinations.

---

## XML

**Status:** exploratory discussion only, nothing scoped, nothing started.
**Priority:** 2 (alongside JSON/JSON5).

Supports:
- XML 1.0
- XHTML
- SVG
- MathML
- RSS
- Atom
- Android XML
- Maven POM
- Ant build.xml
- IntelliJ XML
- etc.

**Formatting:** standard formatting only — no alignment or other new rule
class needed here (no Q&A required for this section, as originally noted).
Indentation, attribute wrapping, and self-closing-tag rules follow the same
shape as an unadorned application of STYLE.md's general bracket/indent rules;
nothing XML-specific needs inventing beyond tag/attribute tokenizing.

**File plan:** no new standalone `STYLE_XML.md`. Folds into the same shared
`STYLE_DATA_FORMATS.md` as JSON/JSON5, as its own (short) subsection, since it
needs essentially no bespoke rules beyond what the tokenizer already has to
do to recognize tags/attributes.

**Test-fixture repos:**
- `apache/maven` — POM-heavy real project, exercises the "Maven POM" variant
  called out above directly.
- `apache/ant` — build.xml-heavy, exercises the "Ant build.xml" variant.
- `jenkinsci/jenkins` — large mixed XML surface (plugin configs, job
  definitions), good density and variety of real-world nesting depth.
- `w3c/svgwg` — SVG working-group repo; real, spec-adjacent SVG markup for
  the "SVG" variant rather than hand-wavy samples.

**Update (Q&A, drafted):** folded into `STYLE_DATA_FORMATS.md` §2, as planned. Two
things resolved during Q&A that weren't obvious from "standard formatting":
- **Indentation** uses the formatter's existing global `indent-size`/`indent-style`
  config keys (already in README.md's config table) — no XML-specific indent config,
  no special tabs-vs-spaces handling.
- **CDATA** is opaque/preserved verbatim by default, *except* when it's the direct
  content of a `<script>`/`<style>` tag (the old XHTML `<script><![CDATA[...]]>` idiom),
  in which case it's unwrapped, dispatched through the CSS/JS formatter same as plain
  `<script>`/`<style>` content, then re-wrapped on output. No separate CDATA formatter
  class needed — it's a check inside the existing script/style dispatcher.

---

## JavaScript

**Status:** exploratory discussion only, nothing scoped, nothing started.
**Priority:** 3 (web group).

Supports:
- Latest supported ECMAScript (currently ES2024+)

**Derivation:** close to Java/Kotlin in shape (C-family brace/paren/statement
structure), so — per the decision to keep this Q&A minimal — this should
derive its rules directly from STYLE_JAVA.md and STYLE_KOTLIN.md the same way
STYLE_KOTLIN.md derives from STYLE_JAVA.md today ("same as STYLE_JAVA.md §N"
where a construct has a direct analog), calling out deltas only:
destructuring/spread, template literals, arrow functions vs. Kotlin lambdas,
optional chaining/nullish coalescing (direct analog to Kotlin §5's null-safety
operators), and `async`/`await`. No fundamentally new rule class expected.

**File plan:** shared `STYLE_JS_TS.md` (decided below, alongside TypeScript)
rather than a separate file per language — the two are close enough in
practice (TypeScript is a syntactic superset) that splitting them would mean
mostly duplicated content, the same reasoning that keeps STYLE_C_CPP.md as one
file for C and C++ rather than two.

**Test-fixture repos:**
- `nodejs/node` — large, real, mixed-style JS codebase (core + tooling).
- `expressjs/express` — smaller, idiomatic, widely-read real-world JS.
- `lodash/lodash` — dense functional-style JS, good stress test for
  complexity-based bracket padding (STYLE.md §3.1) on chained calls.
- `microsoft/TypeScript` — the compiler itself is written in a large,
  disciplined JS/TS-adjacent style; also doubles as a TypeScript fixture.

**Update (Q&A, drafted):** drafted as `STYLE_JS_TS.md`, deriving from STYLE_JAVA.md /
STYLE_KOTLIN.md by section-number citation (not copied content — those files are
stable, so a reference stays correct if they're ever revised, whereas a copy would
silently drift). Cross-references from `STYLE_DATA_FORMATS.md` to this file use the
filename only, not a section number, so they can't go stale if this file's numbering
changes. One item was **not** resolvable from the existing doc and needed new design
during Q&A: import grouping/classification (built-in vs. third-party vs. local) has no
Java/Kotlin analog to derive from — flagged as an open item in `STYLE_JS_TS.md` §13,
same unresolved-tier-classification shape as the Python3 import-sorting question
below (though Python3's ended up resolved much more simply — see that section).

**Update (gap-review pass):** a later pass found and resolved several additional
gaps not covered above: statement termination/semicolons (JS's ASI is an
error-recovery hazard, not a clean design like Kotlin's — resolved as always-explicit,
`STYLE_JS_TS.md` §2), decorators (§7, including the own-line/inline placement and
two-step overflow cascade), TS enums (§10, derived from C++ `enum class` rather than
Java's plain-enum packing), TS union/intersection type wrapping (§9.1, preserves
author's break-before/break-after choice), and TS class field modifier priority/
alignment (§9.2, a new `TSModifierPriority`-equivalent table since TS has modifiers
Java doesn't — `readonly`/`override`/`declare`). JSX/TSX was also raised and
deliberately scoped **out** of this file entirely — see the JSX/TSX entry below.

---

## TypeScript

**Status:** exploratory discussion only, nothing scoped, nothing started.
**Priority:** 3 (web group, alongside JavaScript).

Supports:
- Latest supported TypeScript (currently 5.x)

**Derivation:** same minimal-Q&A reasoning as JavaScript above, plus explicit
type-annotation formatting (colon spacing after parameter/return types is a
direct analog to STYLE_KOTLIN.md's `: ReturnType` tail handling, §9 in
STYLE_KOTLIN.md), generics (`<T>`) reusing the same bracket-complexity
approach as C++/Java generics, and `interface`/`type` alias declarations
(structurally close to a Kotlin `data class`/Java record for alignment
purposes).

**File plan:** same shared `STYLE_JS_TS.md` as JavaScript — this file would
read as "baseline JS rules, then a TypeScript-specific section for type
annotations/generics/interfaces," similar in spirit to how STYLE_KOTLIN.md
sits on top of STYLE_JAVA.md, but kept in one file since unlike Java→Kotlin
there's no meaningful "someone only needs the baseline" audience (nobody
formats plain JS files inside a TS-only project's formatter config in a way
that would need the split).

**Test-fixture repos:**
- `microsoft/TypeScript` — the compiler itself; canonical, heavily-typed
  real-world TS at scale.
- `angular/angular` — large, idiomatic, decorator-heavy real TS.
- `nestjs/nest` — decorator- and generic-heavy backend TS, good coverage of
  the type-annotation-alignment cases.
- `vuejs/core` — modern TS with heavy generics and type-level code.

---

## CSS

**Status:** exploratory discussion only, nothing scoped, nothing started.
**Priority:** 3 (web group).

Supports:
- Modern CSS (currently CSS 3+ modules)

**Alignment (new, decided via Q&A):** align the `:` between property and
value within a rule block, same grouping/group-break shape as JSON's colon
alignment above (adjacent declarations in the same rule form a group; a blank
line, a comment, or a nested at-rule/selector boundary breaks the group).

**File plan:** no new standalone `STYLE_CSS.md`. Folds into the shared
`STYLE_DATA_FORMATS.md` as its own subsection, same reasoning as JSON/XML —
CSS has no functions, no control flow, and only needs the one borrowed
alignment rule plus standard bracket/indent handling for rule blocks.

**Config properties (not yet scoped):** `css-colon-align` (on/off) — toggle
for the property/value alignment above. Same "tracked here, not yet in the
real schema" reasoning as JSON's and Python3's config-properties notes.

**Test-fixture repos:**
- `twbs/bootstrap` — large, hand-authored, real-world source Sass/CSS with
  heavy declaration density per rule block.
- `necolas/normalize.css` — small, extremely well-known, densely-commented
  real CSS — good edge cases for the comment-breaks-group rule.
- `foundation/foundation-sites` — another large real framework, different
  authorship conventions than Bootstrap for cross-checking the rule isn't
  overfit to one project's style.
- `primer/css` — GitHub's own production design-system CSS; real, actively
  maintained, different declaration-density patterns again.

**Update (Q&A, drafted):** folded into `STYLE_DATA_FORMATS.md` §3, as planned.

---

## HTML5

**Status:** exploratory discussion only, nothing scoped, nothing started.
**Priority:** 3 (web group).

Uses:
- HTML formatter
- CSS formatter (for `<style>`)
- JavaScript formatter (for `<script>`)

**Formatting:** standard formatting only — no alignment or other new rule
class needed here (no Q&A required for this section, as originally noted).
The main design work is dispatch: routing `<style>`/`<script>` block contents
to the CSS/JS formatters above and splicing the result back in with correct
re-indentation, not new formatting rules for HTML markup itself.

**File plan:** no new standalone `STYLE_HTML.md`. Folds into the shared
`STYLE_DATA_FORMATS.md` as its own (short) subsection — mostly a description
of the dispatch behavior, since the tag/attribute formatting itself reuses
the same minimal rules as the XML subsection.

**Test-fixture repos:**
- `h5bp/html5-boilerplate` — canonical real-world HTML5 template, exercises
  the "plain markup" path directly.
- `twbs/bootstrap` — its docs site source has substantial real HTML5 with
  embedded `<style>`/`<script>`, good for the dispatch path.
- `mdn/content` — MDN's own content repo, huge volume of real, embedded
  HTML/CSS/JS example snippets, useful both as fixtures and as a style
  reference for "how would a well-formatted example actually look."
- `whatwg/html` — the HTML Living Standard's own source, real large-scale
  HTML5 authored under the standard itself.

**Update (Q&A, drafted):** folded into `STYLE_DATA_FORMATS.md` §4, as planned —
confirmed this is the correct file-plan grouping despite CSS/HTML5 sitting in the
"web group" for *priority* purposes alongside JS/TS; the file-plan grouping (which
file the rules live in) is separate from the priority grouping (implementation
order), and CSS/HTML5's lack of functions/control-flow is what puts them in
`STYLE_DATA_FORMATS.md` rather than `STYLE_JS_TS.md`.

---

## JSX / TSX

**Status:** exploratory discussion only, nothing scoped, nothing started.
**Priority:** not assigned — raised during the STYLE_JS_TS.md gap-review pass,
not part of the original seven-language plan above.

Neither JSX nor TSX ever reaches a browser directly — both require a build step
(Babel/`tsc`/esbuild/swc) that strips types and/or transforms JSX tag syntax
into plain JS function calls (`React.createElement(...)` or the newer `jsx()`
runtime) before execution. This distinguishes JSX from TypeScript's
relationship to JavaScript: TS adds a type-annotation layer on top of the same
runtime grammar (which is why `STYLE_JS_TS.md` treats JS/TS as one file today),
but JSX embeds XML/HTML-like tag syntax directly inside expression position
(`<div className="x">{content}</div>` as a value) — a structurally different
grammar the tokenizer has no support for at all, not a small addition to
existing rules.

**Compound-language framing:** JSX/TSX is closer in kind to HTML5 (which
dispatches embedded `<script>`/`<style>` content to separate formatters) than
to a same-file JS/TS extension — it's markup embedded inside a host language's
expression grammar, not an additive JS/TS construct. For that reason it does
not belong in `STYLE_JS_TS.md` even once scoped; it would need its own file
plan (likely a `STYLE_JSX.md` dispatching to `STYLE_JS_TS.md` for expression
content and something XML/tag-shaped for the markup itself, mirroring HTML5's
dispatch design) rather than a new section in the existing JS/TS file.

**Tokenizer risk:** comparable in kind to C++26's reflection tokens
(`STYLE_CPP26.md` §5) — new tokens the existing tokenizer doesn't recognize,
not new keywords slotted into an existing grammar shape. Any rule set drafted
before that tokenizer work would carry the same "provisional, unvalidated"
caveat §5 already carries for reflection.

No test-fixture repos recorded yet — not scoped enough to pick a corpus.

---

## Python3

**Status:** exploratory discussion only, nothing scoped, nothing started.
**Priority:** 4 (last — most novel design work).

Supports:
- Latest supported Python 3 (currently 3.15+)
- Python 2 not supported

**Complexity detector (new, decided via Q&A — corrected):** the baseline case
reuses STYLE.md §3.1's tight/loose heuristic (atoms/simple ops tight, a
call or nested bracket loose, nesting propagates outward), but this is
**extended, not a straight port** — Python has constructs the C-family
heuristic has no bucket for at all:
- **Comprehensions** (`[x for x in y if cond]`) — a fifth content category
  (a `for` clause plus optional `if` filters, possibly nested), not "atom,"
  "call," or "nested bracket." Needs its own rule, not an adapted old one.
- **Slicing** (`a[1:2:3]`, `a[::2]`) — `:` inside `[]` has no analog in
  C/Java/Kotlin indexing; whether an expression-bearing slice
  (`a[i+1:j-1]`) counts as "simple ops" (tight) or forces loose is an open
  design question, not inherited from §3.1.
  **Resolved (Q&A):** the `:` is punctuation, never spaced, regardless of
  looseness — `a[i+1:j-1]` stays tight; `a[ i+1:(j*k)-1 ]` goes loose because of
  the nested `()`, but the colon itself stays tight either way (`(j*k)-1`, not
  `(j*k) -1` or spaced around `:`). Looseness is decided per side of each `:`
  using the normal §3.1 call/nested-bracket signal, same as any other `[]`.
- **Star-unpacking** (`*args`, `**kwargs`, `[*a, *b]`) — no direct analog in
  the bracket contents of any currently-supported language.
- **Dict vs. set literal ambiguity** — `{}` is an empty dict, `{1, 2}` a
  set, `{1: 2}` a dict; none of the brace-delimited languages have this
  dual meaning to disambiguate before applying padding rules.

**Alignment (new, decided via Q&A):** align `=` across adjacent simple
assignment statements, mirroring STYLE.md §6's compound-operator alignment
for C/Java/Kotlin. Group/break rules should mirror §6 directly (blank line or
comment breaks the group; an augmented-assignment operator like `+=` aligns
in the same group as `=`, same as §6 already does for C-family languages).

**Import sorting — RESOLVED (Q&A), simpler than originally scoped:** the
original framing below (a PEP 8/`isort`-style four-tier grouping requiring
stdlib-vs-third-party classification) was **not adopted**. Decided instead:
Python import order has no runtime-correctness requirement (imports execute
top-to-bottom like any statement), so this is purely a formatting convention.
Sort key: `import` before `from`; alphabetical by module name within each
keyword; for `from X import a, b, c`, tie-break by first name, then second,
etc. Relative imports (`from .`/`from ..`) need no special-casing — `.` sorts
before letters in plain ASCII, so they land first naturally. **No stdlib
list or first-party config is needed at all** — the classification problem
below is avoided entirely by not grouping by tier in the first place.

Critical grouping caveat added during Q&A: a contiguous run of imports at the
same block level is one sortable group; **any non-import statement breaks the
group**, including a statement that itself contains nested imports (an `if`,
`try`, function body). Imports nested inside such a block form their own
group, sorted only among themselves, never merged with imports outside the
block — reordering across that boundary is a real behavioral risk (changes
*when* an import executes), not just cosmetic. `from __future__ import ...`
is moved to the top of its own group (not necessarily the top of the file)
since it's already required to be first — promoting it within its existing
group doesn't cross a group boundary, so it's safe.

Drafted as `STYLE_PYTHON3.md` §3. The paragraph below is preserved for
context on what was originally considered and rejected.

<details>
<summary>Original (rejected) framing — four-tier PEP 8/isort grouping</summary>

Also not a straight port of Java's import-sorting rule (STYLE_JAVA.md, and
`java-import-order` in `STATE_C_CPP_JAVA.md`'s config table). Java sorts a
flat list by package prefix. Python's de facto convention (PEP 8, codified by
`isort`) groups into four tiers instead — `__future__` first, then
standard-library, then third-party, then local/first-party — each tier
blank-line-separated and alphabetized within itself, with its own `import x`
vs. `from x import y` ordering. The hard part is the **stdlib-vs-third-party
classification** itself: unlike Java/Kotlin (where sorting never needs to
know whether a package is "yours" or a dependency), Python's convention
requires knowing that, which means either bundling a stdlib module list to
check against, or requiring the user to configure their own project's
first-party package names.

</details>

**Indentation note:** Python's significant whitespace means this formatter
would, for the first time, need to treat indentation itself as semantically
load-bearing rather than purely cosmetic — worth flagging now as the one
structural difference from every currently-supported language, even though it
doesn't block the complexity-detector or alignment work above.

**File plan:** new standalone `STYLE_PYTHON3.md` — unlike JSON/XML/CSS/HTML5,
Python has real imperative-language surface (functions, control flow,
classes) different enough from the brace-delimited languages that a short
"borrowed sections" subsection wouldn't be enough; it needs the same kind of
dedicated file C, Java, and Kotlin each got.

**Config properties (not yet scoped — tracked here so they aren't lost before
a config schema / `STATE.md` task exists for them):**
- `python-complexity-detector` (on/off) — toggle for the extended §3.1-style
  bracket/comprehension/slice heuristic above.
- `python-assignment-align` (on/off) — toggle for the `=`-alignment rule.
- `python-import-sort` (on/off), `python-import-blank-lines` — resolved and
  drafted in `STYLE_PYTHON3.md` §3.4. The `python-import-stdlib-list` /
  `python-import-first-party-packages` keys originally anticipated here are
  **no longer needed** — the simplified sort rule decided via Q&A has no tier
  classification to configure.

The complexity-detector and assignment-alignment config keys above, plus the
import-sort keys, are drafted in `STYLE_PYTHON3.md` — still not yet in the
real config schema or a `STATE.md` task, but no longer just a placeholder
list; see that file for the resolved shape.

**Test-fixture repos:**
- `python/cpython` — the reference implementation's own standard library;
  large, disciplined, real-world Python at scale.
- `pallets/flask` — small-to-medium, idiomatic, widely-read real Python.
- `django/django` — large real-world Python with heavy use of decorators,
  class-based patterns, and dict/list literal density (good complexity-
  detector stress test).
- `psf/black` — worth including specifically *because* it's a formatter
  itself: its own source is real Python, and its test-fixture corpus
  (`tests/data`) is itself a curated set of formatting edge cases that may be
  directly reusable rather than just inspirational.
- `pallets/click` — dense use of decorators and nested call arguments, good
  additional stress test for the tight/loose bracket heuristic on call sites.

**Update (gap-review pass):** the three items originally left in `STYLE_PYTHON3.md`'s
Known Open Items were resolved via Q&A and drafted into the file: decorator
placement/overflow (§4 — decorators are always their own statement-level line by
grammar, so only the call-overflow cascade needed defining), f-string internal
expression formatting (§5 — braces hold an expression so normal spacing applies, but
the `!conversion`/`:format_spec` tail is a literal spec string and stays opaque), and
type-hint-heavy signature wrapping (§6 — same inline/one-per-line rule as STYLE.md
§8, but the alignment target is `:`/`=` rather than the type column, since Python's
declaration order is `name: type = default` rather than `type name`). Known Open
Items (§7) is now empty, kept for future use.
