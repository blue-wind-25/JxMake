# STATE_HTML5_TCG.md — HTML5 Deep Tree-Construction Gaps (tc gap job)

Read `STATE_COMMON.md` first — shared commit workflow, ambiguity-handling
protocol, file-exclusion rules, real-code-testing methodology. This file
only contains what's specific to this job.

---

## Status: COMPLETE (2026-08-03), readability refactor 2026-08-05

All four gaps (levels 1-4) are landed behind the cumulative
`html5-tc-gap-level` config key (default `0`, still off by default —
**intentional by design, user-confirmed 2026-08-10, not a leftover TODO** —
current behavior unchanged unless a caller opts in). Full-suite dogfood
re-validation across all three corpora (`apache/ant manual/`,
`WordPress/wordpress-develop`, `alexandersandberg/html5-elements-tester`) at
both level `0` and level `4` came back clean — no regression attributable to
any of the four gaps. `make test`: 236/236 forward + idempotency (244/244
after the 2026-08-05 refactor, zero-behavior-change). Originally split out
of `STATE_DATA_FORMATS.md`'s "HTML5 deep tree-construction edge cases" item
1 (2026-08-02); insertion-mode design settled the same day as `RDD_KEY_230`.

**Real-world impact: low.** All four gaps are WPT's own deliberately
pathological conformance fixtures; every dogfood corpus checked formats
cleanly with respect to them — no real-world regression has ever been
attributed to them. This job exists for spec-conformance completeness, not
because a real corpus hit a bug.

---

## The four gaps

1. **Foster-parenting-driven tree reshaping** (`foreign_content_009/010`-
   style WPT fixtures) — content the spec requires relocated out of a
   `<table>` and inserted *before* the table, not nested where source text
   placed it. Genuine tree mutation.
2. **Misnested `<form>` reconstruction inside `<template>`** — spec-defined
   recovery via a "form element pointer," scoped per `<template>` boundary.
3. **Implicit `<body>` start-tag insertion** — the first fabricated-node
   path in an otherwise strictly preserve-as-written formatter.
4. **Adoption agency algorithm** (misnested `<b>`/`<i>`/formatting-element
   recovery) — fiddliest of the four; done last.

**Design decision (`RDD_KEY_230`, 2026-08-02, in `RDD_LOG.md`):** rejected
one large shared insertion-mode state machine. Actual design is three
independent, narrow, config-gated state pieces: `bodyInserted` (gap 3);
`isInTableInsertionMode()` + `FosterBuffer`/`fosterBufferStack` (gap 1);
`currentFormElementPointer` (gap 2). No generic insertion-mode enum/frame
stack; no attempt to model the full ~23-state HTML5 insertion-mode list.
Narrow scope per gap does **not** mean low cumulative risk: gap 1 needs
genuine tree mutation, gap 3 sets a fabricated-node precedent, and four
gated behaviors stack behind one config axis, each dogfooded before the
next lands.

**Contrast — fixes that did NOT need this design:** `RDD_KEY_223`
(`apache/ant manual/running.html`, orphan `</p>`) only needed a name-only
`Deque<String> openTagStack`, no insertion-mode value — name matching, not a
mode-dependent behavior switch. Tag-name case-folding
(`real_code_regressions_112`) was a self-contained lookup table, unrelated
to tree shape.

**Comparative risk note (2026-08-02):** GDR (`STATE_CURLY_GDR.md`) remains
the more dangerous job in practice — demonstrated architectural collision
with shipped pipeline logic (`RDD_KEY_229`). This tc gap job has no such
demonstrated collision; before any level ships it's an inert, unbuilt,
low-real-world-impact feature.

---

## Config: `html5-tc-gap-level`

Integer, default **`0`** (today's behavior, RDD_KEY_223-style heuristics
only). Resolved exactly like any other config value (same precedence chain
as every other key) — no extra `lang.isHtml5 &&` guard needed, since
`html5-tc-gap-level` only has effect when `lang.isHtml5` is already true
elsewhere in the pipeline. Levels cumulative, strictly ordered
simplest-to-most-complex:

| Level | Gap enabled | What it adds |
|---|---|---|
| 0 | none | Current behavior only. |
| 1 | Gap 3 — implicit `<body>` insertion | First fabricated-node path; `bodyInserted` guard flag. |
| 2 | + Gap 1 — foster-parenting | `isInTableInsertionMode()` + `FosterBuffer`/`fosterBufferStack`. |
| 3 | + Gap 2 — misnested `<form>` in `<template>` | `currentFormElementPointer`, scoped per `<template>`. |
| 4 | + Gap 4 — adoption agency | Active-formatting-elements reparenting (narrowed subset, see below). |

Each gap's code path is guarded by a single `config.html5TcGapLevel() >= N`
check; no interaction between levels beyond that (each gap's state is
independent per `RDD_KEY_230`). `README.md` and `STATE_COMMON.md` were
updated once level 1 landed (config-key index, Known Limitations section).

---

## Implementation summary by level

**Level 1 — implicit `<body>` insertion (`>= 1`).** `Config.java`: new
`html5TcGapLevel` int field/getter, `"html5-tc-gap-level"` in `ALL_KEYS`.
`XmlSpecificRule.java`: `bodyInserted` boolean field; `format()` calls
`insertImplicitBodyIfNeeded(nodes)` after `parseNodes(false)`, before
`renderNodes`. **Simplification:** rather than a real "head closed"
insertion-mode transition, finds `<html>`'s children (or top-level node
list), confirms no explicit `<body>` present, treats the first
non-whitespace/non-comment/non-DOCTYPE/non-`<head>` sibling as the
synthesis point, wraps it + all following siblings in one synthesized
`<body>` element. **Known residual gap** (found via WPT `no-doctype-name.html`
re-run, item 4): when source has no explicit `<head>` tag either, the
heuristic wraps `<meta>`/`<title>`/`<script>` into `<body>` immediately
instead of implicitly opening `<head>` first — spec-accuracy gap only, no
crash/malformed output, logged as a known limitation, not fixed.
**Disposition (2026-08-10):** judged plausible to fix (track a real "head
insertion mode closed" transition instead of the sibling heuristic) —
added to `XL.txt` TIER 9 rather than left as permanent.
Fixtures: `test/html_tc_gap_level1_body_insertion_{inp,out}.html`,
`test/html_tc_gap_level0_body_unchanged_{inp,out}.html`.

**Level 2 — foster-parenting (`>= 2`).** New `TABLE_STRUCTURE_CHILDREN`
lookup set (spec's "in table" vocabulary + `td`/`th` defensively);
`FosterBuffer` static nested class; `fosterBufferStack` field
(push/pop in `parseElement` on `<table>` open/close); `pendingFosterBuffer`
side-channel (RDD_KEY_230 Option B), consumed by the immediate caller in
`parseNodes` right before adding the returned `<table>` node, splicing
buffered content in immediately before it; `isInTableInsertionMode()` /
`shouldFosterParent(Node)`; leak-guard `assert fosterBufferStack.isEmpty()`
at end of `format()`. **Deviation from RDD_KEY_230:**
`isInTableInsertionMode()` implemented as a single-level check
(`"table".equals(openTagStack.peek())`), not a full ancestor scan — the
ancestor-scan version incorrectly fostered descendants of an already-
fostered element and legitimate `<tr>`-child `<td>`s. Verified via manual
smoke tests. Fixtures: `test/html_tc_gap_level2_foster_parenting_{inp,out}.html`,
`test/html_tc_gap_level1_foster_unchanged_{inp,out}.html`.

**Level 3 — misnested `<form>` in `<template>` (`>= 3`).**
`currentFormElementPointer` field (active `<form>` node or `null`);
`pendingSuppressedFormNode` side-channel (same Option-B shape). In
`parseElement`: `<template>` open saves/resets the pointer (restored in
`finally`, same call-stack-local pattern as `isSvg`/`svgDepth`); `<form>`
open sets the pointer only if none already active; a suppressed form still
parses normally but is recorded via `pendingSuppressedFormNode`, and
`parseNodes` splices its children into its own children list instead of
adding the form node itself (spec's "ignore the start tag" recovery, minus
dropping content). **Tested and confirmed sufficient:** single field
(not a `Deque`) — stress-tested nested `<template>`-in-`<form>`-in-`<form>`
shape; the `<template>` boundary's save/restore rides the Java call stack
so no cross-boundary state store beyond one field is needed. Fixtures:
`test/html_tc_gap_level3_form_template_{inp,out}.html`,
`test/html_tc_gap_level2_form_unchanged_{inp,out}.html`.

**Level 4 — adoption agency algorithm (`>= 4`).** New `FORMATTING_ELEMENTS`
lookup set (`a`, `b`, `big`, `code`, `em`, `font`, `i`, `nobr`, `s`, `small`,
`strike`, `strong`, `tt`, `u`); `pendingAdoptionNode`/
`pendingAdoptionOuterTagLower` pair set when a formatting element is
implicitly closed because the next token is a real closing tag belonging to
one of its ancestors (classic `<b>1<i>2</b>3</i>`); `pendingReconstruct
FormattingTemplate` side channel (same Option-B shape), consumed by
`parseNodes` right after adding that ancestor node, reconstructing a clone of
the orphaned element as its next sibling via `reconstructFormattingElement`
(mirrors `parseElement`'s tail logic but for a synthesized open tag).
**Deliberately narrowed subset of the spec algorithm** (documented deviation,
same pattern as levels 2-3): no reified "list of active formatting
elements," no bounded-iteration furthest-block search, no bookmark-based
re-insertion — full generality judged too large/risky for one checkpoint.
Implemented: only the single most-recently-orphaned formatting element is
tracked (plain field, not a stack), detected only for the narrow "next token
closes an ancestor" case, reconstructed as a plain next-sibling clone.
**Known limitation:** a second, simultaneous misnesting (two formatting
elements orphaned by the same ancestor's close) only reconstructs the
innermost one — an outer one is silently dropped (field overwritten, not
queued). Not fixed, logged for a future session. **Disposition (2026-08-10):**
judged plausible to fix (upgrade the single-slot field to a small
stack/list) — added to `XL.txt` TIER 9 rather than left as permanent.
**Real bug found and fixed pre-fixture:** level 2's foster-parenting branch
used an early `continue` that bypassed the level-4 reconstruction check
entirely, silently dropping a formatting element that should have been both
reconstructed AND foster-parented (e.g. inside a `<table>`). Fixed by turning
the early `continue` into a `fostered` boolean so the level-4 check always
runs afterward, routing its result into whichever destination the triggering
ancestor landed in. Fixtures:
`test/html_tc_gap_level4_adoption_agency_{inp,out}.html`,
`test/html_tc_gap_level3_adoption_unchanged_{inp,out}.html`.

---

## Validation

- Each level: its own fixture pair(s) plus a combined smoke test proving
  all levels landed so far fire correctly together, in order, without
  disturbing earlier levels' guards.
- Item 9 (final full-suite re-validation, 2026-08-03): re-ran all three
  dogfood corpora at level `0` and level `4`, plus full `make test`
  (236/236 forward + idempotency). Every mismatch traced to an
  already-documented, unrelated cause (RDD_KEY_223's gap, lowercase-prose-
  comment non-bug, level-1's own pre-existing Gutenberg-fragment wrapping
  behavior) — none attributable to level 4. Confirms real-world safety at
  max level.
- `CLAUDE.md`'s routing-table row was kept in sync at every checkpoint
  (design-landed → level 1 → levels 1-2 → levels 1-3 → all four levels +
  full-suite re-validated), per the rule that a resolved design decision is
  real progress and belongs in the row even before code ships.
- `README.md` gained an `# ── HTML5 ──` config-key group
  (`html5-tc-gap-level = 0`) and a Known Limitations paragraph describing
  all four levels' mechanisms/limitations (later moved to a Configuration
  subsection — see `STATE_COMMON.md`'s Architectural TODOs section, 2026-08-03
  doc-cleanup entry). `STATE_COMMON.md`'s Config Keys and Defaults gained
  the matching entry.

---

## 2026-08-05 follow-up (pure readability refactor, zero behavior change)

Every `html5TcGapLevel >= N` raw-integer-literal comparison site in
`XmlSpecificRule.java` now compares against named `private static final int`
constants declared alongside the `html5TcGapLevel` field:
`LEVEL_BODY_SYNTHESIS = 1`, `LEVEL_TABLE_FOSTER = 2`,
`LEVEL_TEMPLATE_FORM = 3`, `LEVEL_FORMATTING_RECONSTRUCT = 4`. An `enum` was
considered and rejected (discussed with the user) — the semantics are a
genuinely cumulative ordered threshold, so `>=` against a plain `int` is
correct and idiomatic; an enum would only relocate the same comparisons
behind `.ordinal()` with no real gain. `Config.java`'s own `html5TcGapLevel`
field has no comparison sites, so it needed no constants. Verified zero
behavior change: `make test` unchanged at 244/244 forward + 244/244
idempotency, plus a real-corpus spot check (`/tmp/ant/manual`, all `.html`
files, at `html5-tc-gap-level=4` via env var) round1/round2 byte-identical.

---

## Resolved Design Decisions index

Full text in `RDD_LOG.md` (grep-only lookup, see `STATE_COMMON.md`):

| Key | Topic |
|---|---|
| RDD_KEY_230 | Insertion-mode state design: no generic enum/frame stack — three independent narrow state pieces (`bodyInserted`; `isInTableInsertionMode()` + `FosterBuffer`/`fosterBufferStack`; `currentFormElementPointer`), plus the `html5-tc-gap-level` config key (default `0`, levels `1`-`4`). |
