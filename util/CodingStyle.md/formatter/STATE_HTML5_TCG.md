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

**Overall status: not started (scoping/planning only, 2026-08-02).** This
job was split out of `STATE_DATA_FORMATS.md`'s "HTML5 deep tree-
construction edge cases" section (item 1: the still-open
`web-platform-tests/wpt` residual gaps) on 2026-08-02. No implementation
has landed — this file now has a full **Scoping** section (grounded in the
actual current `XmlSpecificRule.java` code shape) and a **Checklist** a
future session can execute from directly, starting at item 1.

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

---

## Scoping

### Current code shape (as of 2026-08-02, `src/com/jxmake/formatter/rules/XmlSpecificRule.java`, 1364 lines)

All XML/HTML5 tree-walking lives in one file, gated internally on
`lang.isHtml5`. The pieces most relevant to this job:

- `parseNodes(boolean stopAtCloseTag, Set<String> impliedCloseTriggers)`
  (~line 398) — the children-collecting loop. Already has three HTML5-
  specific tolerant-parsing branches bolted on: (a) `stopAtCloseTag` +
  `openTagStack.contains(...)` ancestor check (RDD_KEY_223) to distinguish
  a legitimate cascade-close from an orphan close tag; (b)
  `impliedCloseTriggers`/`startsWithTriggerTag` (RDD_KEY_200) for sibling-
  start-tag-implies-close (`option`/`optgroup` etc., see
  `IMPLIED_CLOSE_TRIGGERS`); (c) a document-root-level stray-closing-tag
  discard when `!stopAtCloseTag`. None of these are insertion-mode-aware —
  they're all name-matching heuristics against a single flat
  `openTagStack`.
- `parseElement(...)` (~line 639) pushes/pops `openTagStack` (a
  `Deque<String>` of lowercased currently-open tag names — name-only, no
  associated insertion-mode or "which table/template this is inside"
  context) around child parsing via `try`/`finally`.
- `openTagStack` (private field, ~line 260) is the *only* explicit tree-
  shape state that exists today. It answers "is tag X open somewhere
  above me" — nothing else. There is no insertion-mode variable, no
  distinct "list of active formatting elements" (needed for adoption
  agency), no notion of foster-parenting's table/template boundaries.
- `OPAQUE_IMPLIED_END_TAG_ELEMENTS` (~line 147) and
  `IMPLIED_CLOSE_TRIGGERS` (~line 192) are element-name lookup tables for
  two narrow, already-solved sub-problems (verbatim-capture elements like
  `<ruby>`, and sibling-implies-close pairs) — precedent for "table-driven,
  not spec-transcribing" fixes, but neither generalizes to this job's four
  gaps, which are behavior-shape changes, not table entries.
- The overall parse is classic recursive descent: `parseElement` calls
  `parseNodes` which calls `parseSingleNode`/`parseElement` again for each
  child. "What's currently open" beyond `openTagStack`'s tag names (e.g.
  "am I inside a `<table>`," "am I inside a `<template>`," "what's the
  active insertion mode") is implicit in which Java stack frame is
  executing — there is no reified state object a mode-dependent decision
  could consult or mutate mid-parse.

### The four gaps, what each needs, and why the prerequisite is shared

1. **Foster-parenting-driven tree reshaping** (`foreign_content_009/010.html`
   and similar WPT fixtures). Spec requirement: certain content
   encountered while the insertion mode is `"in table"` (etc.) must be
   inserted into the tree *before* the table, not as the table's child,
   even though the source text places it between `<table>` and `</table>`.
   This is a genuine tree-shape rewrite — the naive recursive-descent
   model builds `Node` children in source order as it recurses, so
   "insert this node into an ancestor's child list at a position already
   fixed by the time we're three frames deep inside `<table>`" cannot be
   expressed without either (a) a mutable, explicit node-tree structure
   built bottom-up with post-hoc relocation, or (b) a two-pass approach
   (build tree normally, then a foster-parenting relocation pass keyed off
   an explicit "was this text/node encountered while insertion mode was
   in-table" marker). Needs: insertion-mode tracking at minimum; likely
   also a reification of "the tree built so far" as a mutable structure
   parseElement can reach past its own immediate parent.
2. **Misnested `<form>` reconstruction inside `<template>`.** Spec
   requirement: a `<form>` start tag encountered where the current
   insertion mode doesn't allow it directly triggers specific recovery
   (roughly: track a single "form element pointer," suppress a second one
   inside a `<template>` context, insert according to a mode-specific
   rule). Needs: insertion-mode tracking plus a new single-slot "form
   element pointer" piece of state, scoped per `<template>` boundary.
3. **Implicit `<body>` start-tag insertion.** Spec requirement: a document
   with no explicit `<body>` start tag anywhere still gets one implicitly
   inserted at a specific point (when the first "in body"-eligible content
   is seen after `<head>` closes). This is the narrowest of the four —
   confirmed in the 2026-07-26 investigation (see Background above) it
   cannot be peeled off standalone: it requires (a) the first tag-
   synthesis path in an otherwise strictly preserve-as-written formatter
   (fabricating a `<body>` node absent from source text), and (b)
   threading a "have I already inserted the implicit `<body>`" flag across
   recursive `parseNodes`/`parseElement` calls to avoid double-insertion —
   which is already a lightweight version of the insertion-mode state the
   other three gaps need in full.
4. **Adoption agency algorithm** (misnested `<b>`/`<i>`/formatting-element
   recovery). The most complex of the four — the spec algorithm
   maintains an explicit "list of active formatting elements" (distinct
   from the open-elements stack) and a bounded-iteration reparenting loop.
   Widely regarded (including by the WHATWG spec's own prose) as the
   fiddliest part of HTML5 tree construction to implement correctly.
   Confirmed lowest priority — do last, only after the shared prerequisite
   has already been built and proven on gaps 1-3.

**Why one shared prerequisite, not four independent fixes:** all four are
defined by the HTML5 spec in terms of (a) an explicit open-elements stack
(exists today, but name-only — `openTagStack` would need to carry more
than a tag name, e.g. a per-frame insertion-mode-relevant marker or a
reference to the actual constructed `Node`, not just its name), and (b) a
current insertion-mode value (`"initial"`, `"before html"`, `"before
head"`, `"in head"`, `"after head"`, `"in body"`, `"in table"`, `"in
template"`, `"after body"`, `"after after body"`, etc. — the full HTML5
insertion-mode list is ~23 states, though this job almost certainly does
not need all 23 modeled explicitly; only the subset that actually
distinguishes gap 1-4 behavior needs real states, with everything else
collapsible into a generic "in body"-like default). Building the mode
concept from scratch is the large, shared, structural part of the work;
each individual gap's own behavior on top of that state machine is
comparatively small.

### What real-world HTML this affects

Confirmed **low real-world impact** — every dogfood corpus checked so far
(`apache/ant manual/`, `WordPress/wordpress-develop`,
`alexandersandberg/html5-elements-tester`) formats cleanly with respect to
all four gaps. These are WPT's own deliberately pathological conformance
fixtures (foreign-content/table foster-parenting edge cases, misnested
`<form>`-in-`<template>` edge cases, documents that never write `<body>`
at all, deliberately-misnested `<b>`/`<i>` chains) — real-world authored
HTML essentially never exercises them because browsers' own error recovery
already normalizes the common cases before anyone hand-writes malformed
markup like this. This job exists for spec conformance completeness, not
because a real corpus has hit a bug — re-confirm this is still true (rerun
the dogfood corpora already on hand, see checklist) before sinking large
effort into it, in case priorities have shifted.

### Non-goals

- No change to `openTagStack`'s existing consumers (RDD_KEY_223's
  ancestor-cascade-close logic) unless a specific gap's fix requires it —
  extend, don't replace, unless proven necessary.
- No attempt to model the full ~23-state HTML5 insertion-mode list
  up front. Model only the modes gaps 1-4 actually need; treat everything
  else as the current implicit default behavior.
- No changes to XML/XHTML (non-`lang.isHtml5`) parsing behavior — every
  new code path must stay `lang.isHtml5`-gated, matching every other
  HTML5-tolerant-parsing addition so far.
- Do not attempt all four gaps in one pass — land the prerequisite plus
  gap 3 (narrowest) first, checkpoint, then proceed one gap at a time.

---

## Checklist

Work top to bottom; each numbered item is expected to be its own
checkpoint commit (or a small cluster if trivially connected, per
`STATE_COMMON.md`'s ~50-line-diff guidance). Do not start item *N* before
item *N-1* is committed and `make test` is green.

- [x] 1. **Re-confirm real-world impact is still low** before investing
      further: re-run the three dogfood corpora already on hand
      (`apache/ant manual/`, `WordPress/wordpress-develop`,
      `alexandersandberg/html5-elements-tester` — reuse existing `/tmp`
      checkouts per `STATE_COMMON.md`'s methodology, re-clone only if not
      found) against the current build. If a new real-world regression
      attributable to one of the four gaps has appeared, re-prioritize
      that gap first regardless of the "recommended order" below.

      **2026-08-02 re-run (verification only, no code changes).** All three
      existing `/tmp` checkouts found and reused (`/tmp/ant`,
      `/tmp/wordpress-develop`, `/tmp/html5-elements-tester`); current build
      (`target/code-formatter-1.00.jar`, already up to date, `git log -1` =
      `98ce069`) used directly.
      - `apache/ant manual/` (226 files): forward + round2 + idempotency
        226/226 clean; `html_syntax_check.sh` 226/226 clean; content-diff
        223/226 clean, 3 mismatches — all 3 already documented/accepted in
        `STATE_DATA_FORMATS.md` (`running.html`'s known discard-vs-synthesize
        `<p>` gap, RDD_KEY_223; `Tasks/imageio.html`/`Tasks/image.html`'s
        known lowercase-prose-comment non-bug). No new mismatch, no gap-1/2/
        3/4-attributable regression.
      - `WordPress/wordpress-develop` (303 `.html` files found under the
        checkout, superset of the 263/73 "real markup" counts previously
        recorded — ran all 303 as a superset check rather than
        re-deriving the exact prior filter): forward + round2 + idempotency
        303/303 clean. `html_syntax_check.sh`: 2/303 clean full documents
        (`src/readme.html`, `tests/qunit/index.html`, both OK) plus 301
        `missing-doctype` results on the remaining files — spot-checked
        (`tests/phpunit/data/blocks/do-blocks-original.html`) and confirmed
        these are bare markup *fragments* in the original source too (no
        `<!DOCTYPE>` in the un-formatted input either), i.e. an inherent
        property of these PHPUnit block/template fixture files, not
        something the formatter introduced and not tree-construction-gap-
        related — consistent with this corpus's prior characterization as
        "mostly thin Gutenberg block-theme templates."
      - `alexandersandberg/html5-elements-tester` (`index.html`, 42KB):
        forward + round2 + idempotency clean; `html_syntax_check.sh` OK;
        `html_content_diff.sh` OK (content preserved). Still fully clean
        end-to-end, matching its prior DONE status.

      **Conclusion: still low impact, no reprioritization needed.** No new
      real-world regression attributable to any of the four gaps (foster-
      parenting, misnested `<form>`-in-`<template>`, implicit `<body>`
      insertion, adoption agency) surfaced in any of the three corpora.
      Every residual mismatch found traces to an already-documented,
      already-accepted, unrelated cause. Proceed to item 2 in the existing
      recommended order (no reprioritization).
- [ ] 2. **Design the insertion-mode state object** (spike/design-only, no
      behavior change yet): decide its shape (a small enum plus a
      per-`parseElement`-frame or explicit-stack-of-frames representation
      — needs a concrete decision, not just "some state"), decide how it
      threads through `parseNodes`/`parseElement` (extra parameter vs.
      instance field vs. a small explicit `Deque<Frame>` mirroring
      `openTagStack`), and decide the minimal mode subset needed to serve
      gaps 3, 1, 2 (in that recommended order) without modeling all ~23
      spec modes. Record the decision, once settled, as a new `RDD_KEY_n`
      entry in `RDD_LOG.md` plus this file's own Resolved Design Decisions
      index (add that index section once the first decision lands) —
      treat this as an ambiguity/design decision under `STATE_COMMON.md`'s
      protocol, not something to just start coding.
- [ ] 3. **Implicit `<body>` start-tag insertion** (narrowest gap, do
      first — per the 2026-07-26 investigation cited in Background). Land
      the tag-synthesis path (first fabricated-node case in an otherwise
      preserve-as-written formatter — needs its own explicit design
      sign-off given how large a posture change that is) plus the
      double-insertion guard flag. New fixture(s) under
      `test/real_code_regressions_N_{inp,out}.html` reproducing a
      no-explicit-`<body>` document. `make test` green, zero regressions
      on the existing 223+ suite.
- [ ] 4. **Re-run the WPT residual gap catalogue for gap 3** specifically
      (the relevant `html/syntax/` WPT fixtures cited in
      `STATE_DATA_FORMATS.md`'s item 1, if still reachable — no network
      access was available as of the last check, confirm current
      availability first) to verify the fix is spec-accurate, not just
      "doesn't crash."
- [ ] 5. **Foster-parenting-driven tree reshaping** (`foreign_content_009/
      010.html` and similar). Extend the insertion-mode state from item 2
      to recognize `"in table"`-family modes; implement the relocation
      behavior (decide bottom-up-mutable-tree vs. two-pass approach per
      the Scoping section's item-1 writeup — this choice has real blast-
      radius implications for every HTML5 document processed, treat as an
      ambiguity needing sign-off if it's not obviously forced by the
      state-object design from item 2). New fixture(s) reproducing the
      minimal foster-parenting case. `make test` green.
- [ ] 6. **Misnested `<form>` reconstruction inside `<template>`.** Add
      the single-slot form-element-pointer state, scoped per `<template>`
      boundary; implement the mode-specific insertion rule. New
      fixture(s). `make test` green.
- [ ] 7. **Adoption agency algorithm** (do last — most fiddly, per
      Background). Add the "list of active formatting elements" state
      (distinct from `openTagStack`); implement the spec's bounded-
      iteration reparenting loop for misnested `<b>`/`<i>`/etc. New
      fixture(s) covering at least one classic misnested-formatting-
      element WPT case. `make test` green.
- [ ] 8. **Full-suite real-code re-validation** once all four gaps are
      landed: re-run all three dogfood corpora from item 1 end-to-end
      (forward, round2, idempotency diff, `html_syntax_check.sh`,
      `html_content_diff.py`) plus the full local `make test` suite. Fix
      any regression before considering the job complete.
- [ ] 9. **Update `CLAUDE.md`'s top-level routing table** row for this job
      from "not started — high risk" to whatever the true landed state is
      (only once real progress exists — do not touch it during pure
      planning work).

---
