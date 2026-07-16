# FUTURE_TEST_FIXTURES.md — Planned Local Test Fixture Pairs (Not Authored)

This file is a staging area for **local dogfood test-fixture pairs** (`<name>_inp/out.<ext>`,
same convention as `formatter/test/README.txt`) for languages that don't have a JAR
implementation yet: C++26 extensions, JSON/JSON5, XML, CSS, HTML5, JavaScript,
TypeScript, and Python3. None of the pairs below are authored — this is a planning
list only, same "notes for later, not a task list" spirit as
FUTURE_FEATURE_DISCUSSION.md.

**Sections are named, not numbered**, and never will be — this file is meant to be
referenced from multiple `STYLE_*.md` files by section name, and numbering would go
stale every time a section is added, removed, or reordered.

**Distinct from external Test-Fixture Repos.** Each relevant `STYLE_*.md` also lists
external GitHub repos (for corpus-scale validation once a language is actually
implemented) — those lists are unaffected by this file. This file covers only local,
committed-to-the-repo dogfood pairs, same role `cpp_modern_inp/out.cpp` and
`cpp_comments_inp/out.cpp` already play for CPP20.

When a pair is actually authored, move its entry out of this file and into
`formatter/test/README.txt` alongside the existing entries, same as any other
fixture pair.

---

## CPP26

Moved here from `STYLE_CPP26.md`'s former local "Test Fixtures" section — originally
drafted there before this staging file existed.

- **cpp_26ext_inp/out.cpp** — pack indexing (`T...[i]`), `= delete("reason")`,
  placeholder `_`, contracts (`pre`/`post`/`contract_assert`).
- **cpp_26_comments_inp/out.cpp** — uncommon comment placement around the above
  constructs, same purpose as `cpp_comments_inp/out.cpp` for CPP20.
- **cpp_26_reflection_inp/out.cpp** — reflection (`^^`, `[:`, `:]` splicing).
  Authored **after**, not alongside, the other two pairs above: §5's tokenizer pass
  is validated against the external corpus (`bloomberg/clang-p2996`,
  `wrocpp/cpp26-reflection-examples`, `simdjson/experimental_json_builder`,
  `stephenberry/glaze` — see `STYLE_CPP26.md` §5) first, since that's real-world
  code exercising the new tokens the tokenizer doesn't support yet. Only once that
  pass confirms the tokens are handled correctly does it make sense to write a
  fixed expected-output pair here — a local fixture locks in behavior that's
  already been validated, it isn't how that behavior gets validated in the first
  place. Until then this pair does not exist and §5's rules stay provisional.

Referenced from: `STYLE_CPP26.md`.

---

## JSON

Plain RFC 8259 JSON only — kept separate from JSON5 specifically to catch the
formatter accidentally emitting JSON5-only syntax (trailing commas, unquoted keys,
comments) into a `.json` file. No `_comments` pair — plain JSON has no comment
syntax, so there's nothing to test placement of.

- **json_core_inp/out.json** — strict RFC 8259 grammar: quoted keys only, no trailing
  commas, no comments; key/value colon alignment (§1.1), array bracket complexity
  (§1.2).

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## JSON5

- **json5_combined_inp/out.json5** — JSON5-specific syntax (unquoted keys, trailing
  commas), key/value colon alignment with mixed group/group-break cases, nested
  object/array bracket complexity.
- **json5_comments_inp/out.json5** — uncommon `//` and `/* */` placement (JSON5
  supports both).

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## XML

- **xml_combined_inp/out.xml** — tag/attribute formatting and wrapping, attribute
  order preservation, CDATA (including the `<script>`/`<style>` CDATA-unwrap
  exception), DOCTYPE and processing instructions, at least one non-plain-XML
  dialect (e.g. SVG or Android XML) to exercise namespace-bearing attributes.
- **xml_comments_inp/out.xml** — uncommon `<!-- -->` placement, plus a
  `JXM_CFMT_DIS`/`ENA` directive pair using XML's single block-comment directive
  syntax.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## CSS

- **css_combined_inp/out.css** — property/value colon alignment with mixed group/
  group-break cases, at-rules (`@media`, `@supports`, `@keyframes`, `@font-face`)
  showing the header-vs-declaration distinction, nested rule blocks.
- **css_comments_inp/out.css** — uncommon `/* */` placement, plus a
  `JXM_CFMT_DIS`/`ENA` directive pair using CSS's single block-comment directive
  syntax.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## HTML5

Must include a small embedded `<style>` block and a small embedded `<script>` block
in both pairs (not just the combined one) — the dispatcher to CSS/JS formatting is
the main design point for this language and needs coverage in both the construct
pass and the comment-placement pass, not just one.

- **html_combined_inp/out.html** — void element normalization (`<br>`, `<img>`, no
  self-closing slash), tag/attribute wrapping, a small embedded `<style>` block
  (dispatches to CSS combined fixture's constructs at small scale) and a small
  embedded `<script>` block (dispatches to JS combined fixture's constructs at small
  scale), re-indentation after splice-back.
- **html_comments_inp/out.html** — uncommon `<!-- -->` placement, the
  `<script><![CDATA[ ... ]]></script>` CDATA-wrapped script idiom (§2.3 exception),
  a `<script type="application/json">` block that must stay opaque (not dispatched).

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## JavaScript

Plain `.js` only — no TypeScript-only constructs (those belong in the TypeScript
section below), same separation C/C++ already have across `.c`/`.cpp`/`.hpp`.

- **js_combined_inp/out.js** — destructuring/spread, template literals, arrow
  functions, optional chaining/nullish coalescing, `async`/`await`, decorators
  (stage-3 JS decorators, not TS-only usage), getter/setter accessors (`get`/`set` —
  plain ES6, not TS-only, so it belongs here rather than in the TypeScript pair),
  always-explicit semicolon insertion, import ordering/grouping.
- **js_comments_inp/out.js** — uncommon `//`/`/* */` placement around the above
  constructs.

Referenced from: `STYLE_JS_TS.md`.

---

## TypeScript

Plain `.ts` only — constructs with no valid JS equivalent, so they can't share the
JavaScript pair above.

- **ts_combined_inp/out.ts** — type annotations, union/intersection type wrapping
  (both break-before and break-after styles), generics, `interface`/`type` alias
  declarations, enums (both value-less and explicit-value forms), class field
  modifiers (all six priority-table slots exercised, including a mixed-modifier-
  length alignment group), decorators (own-line and inline placement, plus the
  two-step overflow cascade).
- **ts_comments_inp/out.ts** — uncommon comment placement around the above
  constructs.

Referenced from: `STYLE_JS_TS.md`.

---

## Python3

- **py_combined_inp/out.py** — bracket complexity categories (comprehensions,
  slicing, star-unpacking, dict-vs-set disambiguation), assignment alignment
  (including augmented-assignment and both continuation-break styles), import
  ordering/grouping (including the non-import-statement group-split rule and
  `from __future__ import` promotion), decorators (including the two-step-absent
  overflow case), f-strings (expression spacing vs. opaque format-spec), function
  signature wrapping with type hints (`:`/`=` alignment, a bare-no-hint parameter
  in the same group), structural pattern matching (`match`/`case` — type/sequence/
  mapping/class-deconstruction patterns, or-patterns, guard clauses, wildcard `_`),
  single-statement compound bodies (compact `if x: return y` form for `if`/`elif`/
  `else`/`while`/`for`/`case`, the overflow-triggered expansion to block form, and
  `:`-column alignment across a run of compact `case` lines), control-flow blank
  lines (function-scope-only blank line before `return`, blank line before `elif`/
  `else` triggered by a preceding nested `return`/`break`/`continue`), and a
  `@property`/`@x.setter` pair (to confirm it's just two ordinary decorated methods
  with no special alignment, per §4's note).
- **py_comments_inp/out.py** — uncommon `#` comment placement around the above
  constructs.

Referenced from: `STYLE_PYTHON3.md`.
