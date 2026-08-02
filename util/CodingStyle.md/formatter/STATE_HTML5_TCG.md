# STATE_HTML5_TCG.md — HTML5 Deep Tree-Construction Gaps (tc gap job)

Read `STATE_COMMON.md` first — it has the shared commit workflow, ambiguity-
handling protocol, file-exclusion rules, and real-code-testing methodology
used by every job. This file assumes all of that and only contains what's
specific to this job.

---

## Purpose

Tracks the "HTML5 deep tree-construction gaps" (tc gap) job: closing the
remaining `web-platform-tests/wpt` HTML5 parsing conformance gaps that
require actual HTML5 insertion-mode semantics — foster-parenting-driven
tree reshaping, misnested `<form>` reconstruction inside `<template>`,
implicit `<body>` start-tag insertion, and the adoption agency algorithm —
rather than the ad hoc, locally-scoped fixes that have closed every other
HTML5 parsing bug found so far.

**Overall status: not started.** This job was split out of
`STATE_DATA_FORMATS.md`'s "HTML5 deep tree-construction edge cases"
section (item 1: the still-open `web-platform-tests/wpt` residual gaps) on
2026-08-02. No design work, scoping, or checklist exists yet — this file
currently holds only the background/risk writeup carried over from
`STATE_DATA_FORMATS.md`.

---

## Background: why this is its own dedicated job, not a quick fix

**Current state**: the formatter's HTML5 handling (`XmlSpecificRule` /
`parseNodes`/`parseElement`) walks markup with an implicit tree model —
nesting and "what's currently open" are represented by the Java call
stack's own recursion, not by an explicit HTML5 open-elements stack or
insertion-mode state variable. This has been sufficient for every real
HTML5 bug found in dogfood corpora so far, including some that look
tree-construction-shaped at first glance (see "Contrast: fixes that did
NOT need this" below) — but four residual `web-platform-tests/wpt`
conformance gaps do not close under this model:

1. **Foster-parenting-driven tree reshaping**
   (`foreign_content_009/010.html` and similar WPT fixtures) — content that
   the HTML5 spec requires to be relocated out of a `<table>` and inserted
   *before* the table in the tree, rather than nested inside it where the
   source text placed it.
2. **Misnested `<form>` reconstruction inside `<template>`** — spec-defined
   recovery behavior when a `<form>` start tag appears somewhere the
   current insertion mode doesn't allow it directly.
3. **Implicit `<body>` start-tag insertion** — documents that never write
   an explicit `<body>` tag still get spec-defined implicit element
   insertion at a specific point in the tree.
4. **Adoption agency algorithm** — the spec's misnested-formatting-element
   (`<b>`/`<i>`/etc.) recovery algorithm; generally regarded as the most
   fiddly of the four and the one to tackle last if this job proceeds.

**Why these four need the same prerequisite, and why that prerequisite is
large:** all four are defined in the HTML5 spec in terms of an explicit
open-elements stack plus a current insertion-mode value that switches
behavior contextually (`"in table"`, `"in template"`, `"in body"`, etc.).
The formatter has neither — "what's open" is implicit in Java call-stack
recursion, and there is no insertion-mode concept at all. Building both is
a structural change to the traversal at the core of `parseNodes`/
`parseElement`, touching every HTML5/XML document processed, not a narrow
addition. **Comparable in size/risk to the "General scope-depth
reindentation" (GDR) job** (`STATE_CURLY_GDR.md`) — both require
introducing an explicit state-tracking structure to replace something
currently implicit in ordinary recursive/sequential code, and both carry
blast-radius risk proportional to "every document/line becomes a candidate
for a different result," not a narrow recognized-construct risk surface.

**Real-world impact: low.** All four gaps are WPT's own deliberately
pathological conformance fixtures, designed to probe spec corner cases
most real-world HTML doesn't exercise. Every dogfood corpus checked so far
(`apache/ant manual/`, `WordPress/wordpress-develop`,
`alexandersandberg/html5-elements-tester`) has formatted cleanly with
respect to these four gaps — no real-world regression has ever been
attributed to them. 2026-07-28 re-assessment (carried over from
`STATE_DATA_FORMATS.md`): unchanged, nothing landed, no corpus has hit
this in practice.

**Contrast: fixes that did NOT need this prerequisite.** Not every
tree-shaped-looking HTML5 bug requires the full insertion-mode-state
machine — two precedents show narrower fixes sufficed:
- `apache/ant manual/running.html` (`RDD_KEY_223`, 2026-08-01, FIXED) — an
  orphan `</p>` cascading all the way to the document root. Looked
  tree-construction-shaped, but only needed a lightweight
  name-only `Deque<String> openTagStack` in `XmlSpecificRule` (tracking
  just "what tag names are currently open," no insertion-mode value) to
  find the correct matching open element instead of over-closing. This
  job's four gaps are different in kind, not just degree — they need
  mode-dependent *behavior* switches (e.g. "relocate this node before the
  table" or "implicitly insert a `<body>`"), which a name-only stack
  cannot express.
- Tag-name case-folding (`real_code_regressions_112`, commit `10b20cf`,
  2026-07-25, DONE) — a self-contained lookup-table fixup
  (`XmlSpecificRule.SVG_TAG_NAME_CASE_FIXUP`), unrelated to tree shape at
  all.

See `STATE_DATA_FORMATS.md`'s "HTML5 deep tree-construction edge cases"
section for the original combined writeup (items 1-3) this job was split
out of; items 2 and 3 above (the FIXED precedents) remain documented there
as they are closed and out of this job's scope. `RDD_LOG.md` has no key
yet for this job specifically — the relevant history is under the keys
cited above.

**Comparative risk note (2026-08-02, from direct discussion):** of the two
"comparable in size" jobs, GDR is currently the more dangerous of the two
in practice — it has a demonstrated architectural collision with
already-shipped pipeline logic (`RDD_KEY_229`, a real circular dependency
between GDR's depth-based indent and the pipeline's width-based wrap
decisions). This tc gap job has no such demonstrated collision; it is an
inert, unbuilt feature with explicitly low real-world impact, whose main
cost is the size of the prerequisite state-machine work, not a known
collision with anything already shipped.
