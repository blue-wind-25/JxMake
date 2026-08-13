# STATE_HTML5_TCG.md — HTML5 Deep Tree-Construction Gaps (tc gap job)

Read `STATE_COMMON.md` first — shared commit workflow, ambiguity-handling
protocol, file-exclusion rules, real-code-testing methodology. This file
only contains what's specific to this job.

---

## Status: COMPLETE (2026-08-03), readability refactor 2026-08-05

All four gaps (levels 1-4) are landed behind the cumulative
`html5-tc-gap-level` config key (default `0` — **intentional by design,
user-confirmed 2026-08-10, not a leftover TODO**; behavior unchanged unless
a caller opts in). Full-suite dogfood re-validation across all three
corpora (`apache/ant manual/`, `WordPress/wordpress-develop`,
`alexandersandberg/html5-elements-tester`) at levels `0` and `4` came back
clean — no regression attributable to any of the four gaps. `make test`:
236/236 forward + idempotency (244/244 after the 2026-08-05 refactor,
zero-behavior-change). Split out of `STATE_DATA_FORMATS.md`'s "HTML5 deep
tree-construction edge cases" item 1 (2026-08-02); insertion-mode design
settled same day as `RDD_KEY_230`.

**Real-world impact: low.** All four gaps are WPT's own deliberately
pathological conformance fixtures; every dogfood corpus formats cleanly
against them, and no real-world regression has ever been attributed to
them — this job exists for spec-conformance completeness, not because a
real corpus hit a bug.

---

## The four gaps

1. **Foster-parenting-driven tree reshaping** (`foreign_content_009/010`-
   style WPT fixtures) — content the spec requires relocated out of a
   `<table>` and inserted *before* it, not nested where source text placed
   it. Genuine tree mutation.
2. **Misnested `<form>` reconstruction inside `<template>`** — spec-defined
   recovery via a "form element pointer," scoped per `<template>` boundary.
3. **Implicit `<body>` start-tag insertion** — the first fabricated-node
   path in an otherwise strictly preserve-as-written formatter.
4. **Adoption agency algorithm** (misnested `<b>`/`<i>`/formatting-element
   recovery) — fiddliest of the four; done last.

**Design decision (`RDD_KEY_230`, 2026-08-02, `RDD_LOG.md`):** rejected one
large shared insertion-mode state machine for three independent, narrow,
config-gated state pieces: `bodyInserted` (gap 3); `isInTableInsertionMode()`
+ `FosterBuffer`/`fosterBufferStack` (gap 1); `currentFormElementPointer`
(gap 2) — no generic insertion-mode enum/frame stack, no attempt to model
the full ~23-state HTML5 insertion-mode list. Narrow scope per gap does
**not** mean low cumulative risk: gap 1 needs genuine tree mutation, gap 3
sets a fabricated-node precedent, and the four gated behaviors stack behind
one config axis, each dogfooded before the next lands.

**Contrast — fixes that did NOT need this design:** `RDD_KEY_223`
(`apache/ant manual/running.html`, orphan `</p>`) only needed a name-only
`Deque<String> openTagStack` — name matching, no insertion-mode value.
Tag-name case-folding (`real_code_regressions_112`) was a self-contained
lookup table, unrelated to tree shape.

**Comparative risk note (2026-08-02):** GDR (`STATE_CURLY_GDR.md`) remains
the more dangerous job in practice, with a demonstrated architectural
collision with shipped pipeline logic (`RDD_KEY_229`). This tc gap job has
no such collision; before any level ships it's an inert, unbuilt,
low-real-world-impact feature.

---

## Config: `html5-tc-gap-level`

Integer, default **`0`** (today's behavior, RDD_KEY_223-style heuristics
only). Resolved via the same precedence chain as any other config value —
no extra `lang.isHtml5 &&` guard needed, since the key only takes effect
where `lang.isHtml5` is already true elsewhere in the pipeline. Levels are
cumulative, strictly ordered simplest-to-most-complex:

| Level | Gap enabled | What it adds |
|---|---|---|
| 0 | none | Current behavior only. |
| 1 | Gap 3 — implicit `<body>` insertion | First fabricated-node path; `bodyInserted` guard flag. |
| 2 | + Gap 1 — foster-parenting | `isInTableInsertionMode()` + `FosterBuffer`/`fosterBufferStack`. |
| 3 | + Gap 2 — misnested `<form>` in `<template>` | `currentFormElementPointer`, scoped per `<template>`. |
| 4 | + Gap 4 — adoption agency | Active-formatting-elements reparenting (narrowed subset, see below). |

Each gap's code path is guarded by a single `config.html5TcGapLevel() >= N`
check; levels don't otherwise interact (each gap's state is independent
per `RDD_KEY_230`). `README.md` and `STATE_COMMON.md` were updated once
level 1 landed (config-key index, Known Limitations section).

---

## Implementation summary by level

**Level 1 — implicit `<body>` insertion (`>= 1`).** `Config.java`: new
`html5TcGapLevel` int field/getter, `"html5-tc-gap-level"` in `ALL_KEYS`.
`XmlSpecificRule.java`: `bodyInserted` boolean field; `format()` calls
`insertImplicitBodyIfNeeded(nodes)` after `parseNodes(false)`, before
`renderNodes` — confirms no explicit `<body>`, treats the first
non-whitespace/non-comment/non-DOCTYPE/non-`<head>` sibling as the
synthesis point, and wraps it plus all following siblings in one
synthesized `<body>`. **Known residual gap FIXED (2026-08-11):** when
source had no explicit `<head>` tag either, the old sibling heuristic
wrapped `<meta>`/`<title>`/`<script>` into `<body>` immediately instead of
implicitly opening `<head>` first. Root cause: the synthesis point was
picked by a sibling-shape heuristic, not a real tracked "head insertion
mode closed" transition, so a leading `<meta>`/`<title>`/`<script>` run
with no `<head>` wrapper had nothing distinguishing it from real body
content. Fixed by adding a `headInsertionModeClosed` boolean field (reset
at the top of each `insertImplicitBodyIfNeeded` call) plus a
`HEAD_ELIGIBLE_ELEMENTS` lookup set (`title`, `script`, `style`, `meta`,
`link`, `base`, `noscript` — the spec's own "in head" vocabulary): while
scanning for the synthesis point, an explicit `<head>` sets the flag
`true` and is skipped as before; while the flag is `false`, a head-eligible
sibling is also skipped (it belongs to an implicit head, not body); the
first sibling that is neither closes the flag and becomes the synthesis
point. Verified via a minimal repro (`<html>` with `<meta>`/`<title>`/
`<script>` then `<h1>`/`<p>`, no `<head>` tag at all, `html5-tc-gap-level=1`):
before the fix all five siblings landed inside the synthesized `<body>`;
after, `<meta>`/`<title>`/`<script>` stay outside `<body>` as direct
`<html>` children, while `<h1>`/`<p>` still get wrapped correctly. The
explicit-`<head>` case (the common case, e.g.
`html_tc_gap_level1_body_insertion_{inp,out}.html`) is unchanged, verified
byte-identical via `make test`. `README.md`'s Level-1 known-gap bullet
updated to drop this sub-gap (the bare-fragment-wrapping sub-gap is
unaffected, still documented).
Fixtures: `test/html_tc_gap_level1_body_insertion_{inp,out}.html`,
`test/html_tc_gap_level0_body_unchanged_{inp,out}.html`,
`test/html_tc_gap_level1_no_head_{inp,out}.html` (new, 2026-08-11 — no
explicit `<head>` at all, proves the fix).

**Level 2 — foster-parenting (`>= 2`).** New `TABLE_STRUCTURE_CHILDREN`
lookup set (spec's "in table" vocabulary + `td`/`th` defensively);
`FosterBuffer` static nested class; `fosterBufferStack` field (push/pop in
`parseElement` on `<table>` open/close); `pendingFosterBuffer` side-channel
(RDD_KEY_230 Option B), consumed by the immediate caller in `parseNodes`
right before adding the returned `<table>` node, splicing buffered content
in immediately before it; `isInTableInsertionMode()`/`shouldFosterParent
(Node)`; leak-guard `assert fosterBufferStack.isEmpty()` at end of
`format()`. **Deviation from RDD_KEY_230:** `isInTableInsertionMode()`
implemented as a single-level check (`"table".equals(openTagStack.peek())`),
not a full ancestor scan — the ancestor-scan version incorrectly fostered
descendants of an already-fostered element and legitimate `<tr>`-child
`<td>`s. Verified via manual smoke tests. Fixtures:
`test/html_tc_gap_level2_foster_parenting_{inp,out}.html`,
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
dropping content). **Tested and confirmed sufficient:** a single field
(not a `Deque`) — stress-tested nested `<template>`-in-`<form>`-in-`<form>`
shape; the `<template>` boundary's save/restore rides the Java call stack,
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
`parseNodes` right after adding that ancestor node, reconstructing a clone
of the orphaned element as its next sibling via `reconstructFormattingElement`
(mirrors `parseElement`'s tail logic but for a synthesized open tag).
**Deliberately narrowed subset of the spec algorithm** (documented
deviation, same pattern as levels 2-3): no reified "list of active
formatting elements," no bounded-iteration furthest-block search, no
bookmark-based re-insertion — full generality judged too large/risky for
one checkpoint. Only the single most-recently-orphaned formatting element
is tracked (plain field, not a stack), detected only for the narrow "next
token closes an ancestor" case, reconstructed as a plain next-sibling
clone. **Known limitation:** a second, simultaneous misnesting (two
formatting elements orphaned by the same ancestor's close) only
reconstructs the innermost one — an outer one is silently dropped (field
overwritten, not queued). Not fixed, logged for a future session.
**Disposition (2026-08-10):** judged plausible to fix (upgrade the
single-slot field to a small stack/list) — added to `XL.txt` TIER 9 rather
than left permanent. **Real bug found and fixed pre-fixture:** level 2's
foster-parenting branch used an early `continue` that bypassed the
level-4 reconstruction check entirely, silently dropping a formatting
element that should have been both reconstructed AND foster-parented (e.g.
inside a `<table>`). Fixed by turning the early `continue` into a
`fostered` boolean so the level-4 check always runs afterward, routing its
result into whichever destination the triggering ancestor landed in.
Fixtures: `test/html_tc_gap_level4_adoption_agency_{inp,out}.html`,
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
  behavior) — none attributable to level 4, confirming real-world safety at
  max level.
- **2026-08-11 (level-1 no-`<head>` fix):** `make -k test` after the fix:
  552 PASS, one pre-existing unrelated failure
  (`test/real_code_regressions_148_inp.kt`, a Kotlin comment-normalization
  drift, confirmed present identically when built against the unmodified
  `HEAD` version of `XmlSpecificRule.java` — not caused by this change).
  Every HTML5 fixture, including all four tc-gap levels' existing fixtures,
  passed unchanged; new fixture `html_tc_gap_level1_no_head_{inp,out}.html`
  passes.
- `CLAUDE.md`'s routing-table row was kept in sync at every checkpoint
  (design-landed → level 1 → levels 1-2 → levels 1-3 → all four levels +
  full-suite re-validated), per the rule that a resolved design decision is
  real progress and belongs in the row even before code ships.
- `README.md` gained an `# ── HTML5 ──` config-key group
  (`html5-tc-gap-level = 0`) and a Known Limitations paragraph describing
  all four levels' mechanisms/limitations (later moved to a Configuration
  subsection — see `STATE_COMMON.md`'s Architectural TODOs section,
  2026-08-03 doc-cleanup entry). `STATE_COMMON.md`'s Config Keys and
  Defaults gained the matching entry.

---

## 2026-08-05 follow-up (pure readability refactor, zero behavior change)

Every `html5TcGapLevel >= N` raw-integer-literal comparison site in
`XmlSpecificRule.java` now compares against named `private static final int`
constants declared alongside the `html5TcGapLevel` field:
`LEVEL_BODY_SYNTHESIS = 1`, `LEVEL_TABLE_FOSTER = 2`,
`LEVEL_TEMPLATE_FORM = 3`, `LEVEL_FORMATTING_RECONSTRUCT = 4`. An `enum`
was considered and rejected (discussed with the user) — the semantics are
a genuinely cumulative ordered threshold, so `>=` against a plain `int` is
correct and idiomatic; an enum would only relocate the same comparisons
behind `.ordinal()` with no real gain. `Config.java`'s own `html5TcGapLevel`
field has no comparison sites, so needed no constants. Verified zero
behavior change: `make test` unchanged at 244/244 forward + idempotency,
plus a real-corpus spot check (`/tmp/ant/manual`, all `.html` files, at
`html5-tc-gap-level=4` via env var) round1/round2 byte-identical.

---

## Open Questions

**Level 4 two-simultaneous-misnesting fix (`XL.txt` TIER 9 item) — blocked,
2026-08-11, needs user decision.** Investigated upgrading
`pendingAdoptionNode`/`pendingAdoptionOuterTagLower` (single field pair) to
a small stack/list, so a second simultaneous misnesting under the same
ancestor is queued instead of overwriting the first (see Level 4's "Known
limitation" note above).

Root cause confirmed by tracing `parseElement`/`parseNodes`: for
`<b>1<i>2<u>3</b>4</i>5</u>6</p>` (or without the `<p>` wrapper), `<u>` is
orphaned first (`pendingAdoptionNode = u`, ancestor `b`); before `b`
actually closes, `<i>` is *also* orphaned by the same eventual `</b>`
(`pendingAdoptionNode = i`, overwriting `u`) — confirmed via a Java debug
print that `u`'s entry is silently dropped exactly as documented.

Built a minimal repro and checked real spec-correct ground truth using
`parse5` (a real HTML5-tree-construction-spec implementation, already
installed under `~/mynpm/node_modules`, invoked via
`tools/verifiers/_exec_node_env.sh`) rather than guessing:

```
input:  <b>1<i>2<u>3</b>4</i>5</u>6
spec output:
  <b>1<i>2<u>3</u></i></b><i><u>4</u></i><u>5</u>6
```

Materially more complex than "queue both orphaned elements, replay each
once": the spec's real output needs **three separate reconstruction
events**, not one — (1) `<i>` wrapping a nested `<u>` reconstructed right
after `<b>`'s real close, holding `"4"`; (2) a **second, independent** `<u>`
reconstruction after *that* reconstructed `<i>`'s own matching close,
holding `"5"`; (3) plain trailing text `"6"` with no further
reconstruction. This is the spec's "reconstruct the active formatting
elements" step, which re-fires again after each subsequent close/text-insert
— not a one-shot replay of a queued list, but a genuinely bigger mechanism
(closer to the full active-formatting-elements list the existing design
deliberately rejected for level 4, see "Deliberately narrowed subset"
above) than the "small, contained upgrade... not a rewrite of the
adoption-agency algorithm" scope this fix was proposed under.

**Not implemented.** Per STATE_COMMON.md's ambiguity-handling protocol,
stopping here rather than shipping a queue-based fix that would produce an
order different from real spec behavior (silently wrong in a new way,
instead of the current honestly-documented single-slot limitation). No
source file touched this session. Options for a future session, needing a
user decision on which to pursue:

1. Leave the known limitation as-is (already accepted/documented, low
   real-world impact per this file's own top-of-file dogfood findings —
   needs two *simultaneous* misnestings, which real-world corpora have
   never hit).
2. Implement the fuller "reconstruct active formatting elements on next
   text/element insert" mechanism needed for spec-correct multi-misnesting
   output — a materially bigger, riskier change than originally scoped,
   effectively expanding level 4 toward the full spec algorithm the design
   doc (`RDD_KEY_230`) deliberately rejected.
3. Implement a queue-based fix that is a deliberate, documented
   approximation (not spec-exact) or an incremental risk assessment.

---

## Resolved Design Decisions index

Full text in `RDD_LOG.md` (grep-only lookup, see `STATE_COMMON.md`):

| Key | Topic |
|---|---|
| RDD_KEY_230 | Insertion-mode state design: no generic enum/frame stack — three independent narrow state pieces (`bodyInserted`; `isInTableInsertionMode()` + `FosterBuffer`/`fosterBufferStack`; `currentFormElementPointer`), plus the `html5-tc-gap-level` config key (default `0`, levels `1`-`4`). |
