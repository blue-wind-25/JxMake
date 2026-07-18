# FUTURE_TEST_FIXTURES.md — Planned Local Test Fixture Pairs

> **⚠️ Note on fixture quality:** the `inp`/`out` pairs in this file are
> hand-crafted (by an AI pass reasoning against the relevant `STYLE_*.md` rules,
> not generated or verified by a JAR), and may still contain formatting errors —
> several have already been caught and corrected through review, and more may
> remain. Check each pair against the style docs before relying on it, same
> "review every diff carefully" scrutiny `README.txt` asks of any AI-pass output.

This file is a staging area for **local dogfood test-fixture pairs** (`<name>_inp/out.<ext>`,
same convention as `formatter/test/README.txt`) for languages that don't have a JAR
implementation yet: C++26 extensions, JSON/JSON5, XML, CSS, HTML5, JavaScript,
TypeScript, and Python3. Same "notes for later, not a task list" spirit as
FUTURE_FEATURE_DISCUSSION.md — a pair listed here is not a commitment to implement
the language.

**Draft content vs. authored/registered.** Some entries below carry a pre-drafted
`inp`/`out` pair inline, hand-reasoned against the relevant `STYLE_*.md` rules by a
capable general-purpose model (the same job `AI_PREAMBLE_FULL.md` describes) since
there's no JAR to generate or verify it. This is **not** the same as "authored" in
the sense the rest of this file uses that word — draft content here has not been
run through a formatter or cross-checked, and needs the same "review every diff
carefully" scrutiny `README.txt` asks of any AI-pass output. A pair is only
"authored" once it's been reviewed, moved to `formatter/test/`, and registered in
`formatter/test/README.txt` per that file's own instructions — draft content
staying here, even pre-written, doesn't skip that step.

**PROMOTION GATE — flagged assumptions/contradictions.** Several draft pairs below
carry a **Flagged assumption** or **Flagged contradiction** note — a spot where the
draft had to guess at an unstated rule, or where two parts of the relevant
`STYLE_*.md`/`STYLE.md` file disagree with each other. These are not just FYI: **a
pair carrying one of these notes must not be moved to `formatter/test/` and
registered in `README.txt` until the flag is resolved.** Resolving means an actual
discussion with Aloysius about what the rule *should* be, followed by a fix to the
corresponding `STYLE_*.md` section — not a unilateral pick, even if the fixture's
own draft content already leans one way. If Claude (in Claude Code or any other
session) is asked to promote a fixture pair and its entry still carries a Flagged
note, stop and raise the flag for discussion before touching `formatter/test/` or
`README.txt`, same as any other unresolved open item in this project.

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


`cpp_26ext_inp/out.cpp` and `cpp_26_comments_inp/out.cpp` have been extracted to
`formatter/test/` (registered commented-out in the Makefile pending real §1-4 rule
coverage -- see `STATE_CPP26.md`). Their draft content formerly staged here has been
removed; see `formatter/test/README.txt` for a description of what each covers.

`cpp_26_reflection_inp/out.cpp` has also been extracted to `formatter/test/`
(registered commented-out in the Makefile pending real §5 rule coverage -- see
`STATE_CPP26.md`), promoted ahead of its original promotion gate (external-corpus
cross-check still pending) to seed the initial tokenizer test, per explicit
instruction. Its draft content formerly staged here has been removed; see
`formatter/test/README.txt` for a description of what it covers.

Referenced from: `STYLE_CPP26.md`.

---

## JSON

Plain RFC 8259 JSON only — kept separate from JSON5 specifically to catch the
formatter accidentally emitting JSON5-only syntax (trailing commas, unquoted keys,
comments) into a `.json` file. No `_comments` pair — plain JSON has no comment
syntax, so there's nothing to test placement of.

The entry formerly staged here (`json_core_inp/out.json`) has been extracted,
reviewed, and registered as a real fixture in `formatter/test/` — see
`formatter/test/README.txt` and `formatter/STATE_DATA_FORMATS.md`.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## JSON5

Both entries formerly staged here (`json5_core_inp/out.json5`,
`json5_comments_inp/out.json5`) have been extracted, reviewed, and registered as
real fixtures in `formatter/test/` — see `formatter/test/README.txt` and
`formatter/STATE_DATA_FORMATS.md`.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## XML

`xml_combined_inp/out.xml` and `xml_comments_inp/out.xml` have been extracted to
`formatter/test/` and verified against the real JAR (registered active in the
Makefile -- XML has real logic; see `STATE_DATA_FORMATS.md`). Their draft content
formerly staged here has been removed; see `formatter/test/README.txt` for a
description of what each covers, including two corrections found once verified
against the real JAR: self-closing tags never wrap regardless of length (a known
gap, not a bug to fix here), and `<script>`/`<style>` CDATA splicing is HTML5-only
(§4.2), not implemented for plain XML, so it stays fully opaque.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## CSS

Both entries formerly staged here (`css_combined_inp/out.css`,
`css_comments_inp/out.css`) have been extracted, reviewed, and registered as real
fixtures in `formatter/test/` — see `formatter/test/README.txt` and
`formatter/STATE_DATA_FORMATS.md`.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## HTML5

`html_combined_inp/out.html` and `html_comments_inp/out.html` have been extracted
to `formatter/test/` (registered commented-out in the Makefile pending real HTML5
dispatch/formatting logic -- see `STATE_DATA_FORMATS.md`). Their draft content
formerly staged here has been removed; see `formatter/test/README.txt` for a
description of what each covers.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## YAML

Both entries (`yaml_core_inp/out.yaml`, `yaml_comments_inp/out.yaml`) were authored
directly in `formatter/test/` rather than staged here first, since there was no
pre-existing draft to extract — see `formatter/test/README.txt` and
`formatter/STATE_DATA_FORMATS.md`. They are commented out of the Makefile's
`INP_FILES` (YAML support is scaffold-only, RDD_KEY_191) until real formatting logic
lands.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## TOML

Both entries (`toml_core_inp/out.toml`, `toml_comments_inp/out.toml`) were authored
directly in `formatter/test/` rather than staged here first, same as YAML above —
see `formatter/test/README.txt` and `formatter/STATE_DATA_FORMATS.md`. They are
commented out of the Makefile's `INP_FILES` (TOML support is scaffold-only,
RDD_KEY_191) until real formatting logic lands.

Referenced from: `STYLE_DATA_FORMATS.md`.

---

## JavaScript

`js_combined_inp/out.js` and `js_comments_inp/out.js` have been extracted to
`formatter/test/` (registered commented-out in the Makefile pending real JS
formatting logic -- see `STATE_JS_TS.md`). Their draft content formerly staged
here has been removed; see `formatter/test/README.txt` for a description of what
each covers.

Referenced from: `STYLE_JS_TS.md`.

---

## TypeScript

`ts_combined_inp/out.ts` and `ts_comments_inp/out.ts` have been extracted to
`formatter/test/` (registered commented-out in the Makefile pending real TS
formatting logic -- see `STATE_JS_TS.md`). Their draft content formerly staged
here has been removed; see `formatter/test/README.txt` for a description of what
each covers.

Referenced from: `STYLE_JS_TS.md`.

---

## Python3

`py_combined_inp/out.py` and `py_comments_inp/out.py` have been extracted to
`formatter/test/` (registered commented-out in the Makefile pending real Python3
formatting logic -- see `STATE_PYTHON3.md`). Their draft content formerly staged
here has been removed; see `formatter/test/README.txt` for a description of what
each covers.

Referenced from: `STYLE_PYTHON3.md`.
