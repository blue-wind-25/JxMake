# STATE_CURLY_GDR.md — General Scope-Depth Reindentation (curly reindent job)

Read `STATE_COMMON.md` first — it has the shared commit workflow, ambiguity-
handling protocol, file-exclusion rules, and real-code-testing methodology
used by every job. This file assumes all of that and only contains what's
specific to this job.

---

## Purpose

Tracks the "General scope-depth reindentation" (GDR) job: reindenting
ordinary body statements to an absolute target derived from structural
(brace/scope) depth, rather than the current model of preserving original
whitespace except where a specific recognized rewrite (brace placement,
spacing, alignment) requires touching it.

**Overall status: NOT STARTED.** This file was split out of
`STATE_COMMON.md`'s old "Architectural TODOs" section (which held only a
risk-analysis writeup, no implementation) on 2026-08-02, with the design
expanded per the discussion below. No `src/` code exists for this job yet.

---

## Background: why this is its own dedicated job, not a quick fix

**Current state** (confirmed by direct testing, C++26 session): the
formatter does not reindent ordinary body statements from scratch —
original whitespace is preserved except for specific recognized rewrites
(brace placement, spacing, alignment). Only
`SwitchRule.applyNonInlineCaseIndent` and
`ScopePipeline.applyDeclarationsPass` reindent anything, and both apply one
**relative delta** from a single reference line, not an absolute target
derived from brace-nesting depth. `STATE_C_CPP_JAVA.md`'s "Known Gaps —
Open" documents two real bugs from this shape (`ASTParser.java` in
`javaparser/javaparser`; local `tool/JSONEncoderLite.java`) —
non-idempotent reindentation on internally-inconsistent source, both
ACCEPTED-not-fixed: the real fix (derive each line's absolute target from
structural depth, not a raw-source delta) is nontrivial with real
regression risk for a narrow shape.

**Why a *general* version is much harder/riskier than those two narrow passes:**
- **Blast radius inversion.** Current invariant: don't touch indentation
  unless a specific construct requires it — why every real-code bug found
  so far (~20+ external repos) has been narrow/isolated. A general pass
  makes every line in every file a candidate for a wrong result (currently
  ~1/2000 files in `javaparser`) — would become the default risk surface
  for the whole corpus. (See "Proposed pre-pass architecture" below —
  this is now scoped down to only the `curly-general-scope-reindent = on`
  case, not the default-off path.)
- **Continuation vs. block depth is a second axis, not a free extension.**
  Brace/paren/bracket depth alone isn't enough — wrapped expressions,
  chained calls, multi-line initializers each have their own
  continuation-indent conventions (STYLE.md §2) that don't reduce to "one
  level per `{`". Any real implementation must merge two indent models
  without them fighting.
- **Content that must never be touched.** Raw string literals, block-comment
  interior lines, preprocessor directives (column-0 regardless of depth,
  own continuation rules), and `frozen` spans all need exclusion — each has
  already been a real bug source under the current narrower passes; a
  general pass multiplies where these exclusions must be reapplied.
- **Ordering interacts with every other pass.** Brace-placement (Allman),
  line-wrapping (`enforceCallLineBreaking`), switch-case handling all run at
  specific `FormatterCurly` phase points because their output affects what
  "correct" indentation even is afterward (see the
  `formatNonInlineSwitches`/`enforceCallLineBreaking` ordering bug, fixture
  `_56`). A general reindent pass needs to run after every line-count/brace
  decision is final; an ordering bug here produces plausible-looking-wrong
  output, not a crash.

**If ever attempted (general cautions, still apply on top of the pre-pass
architecture below):**
- `make test`'s fixture corpus is a floor, not a substitute, for validation
  — fixtures were tuned under the current indentation-preserving model.
  Re-run real-code testing against at least `javaparser/javaparser`, local
  `tool/JSONEncoderLite.java`, `serge-sans-paille/frozen` (where the
  existing indent bugs surfaced), plus a fresh untested large corpus
  (full-tree idempotency, not `--out DIR`) — neither open gap was caught by
  `make test` alone, both came from one-off real-code-testing sessions.
- Expect this to be the single riskiest change ever made to this
  formatter's overall system; budget accordingly, not as an incremental fix.

---

## In-file directive requirement (JXM_CFMT_GDR)

Needed once GDR is actually implemented, so mixed/inconsistent indentation
can be deliberately introduced and tested per-region (e.g. a nested `if`
block hand-indented at a shallower depth than its ancestors, on purpose, in
test fixtures or real source) without needing a whole-file config flip.

This is a **new, GDR-specific directive pair**, distinct from the existing
`JXM_CFMT_CFG key=value` in-file config mechanism (see `STATE_COMMON.md`'s
"In-file Config Support" section for the existing directive's parsing
precedent — `InFileConfig.parse`, top-of-file preamble, `//%`/`/*% ... */`
comment-based directives). GDR needs point-in-file 0/1 toggles, not a
single top-of-file key=value block, so it does not reuse
`JXM_CFMT_CFG`'s syntax or its single-preamble-only placement rule:

```
//% JXM_CFMT_GDR 0
//% JXM_CFMT_GDR 1
```

and the block-comment variant, matching however `JXM_CFMT_CFG` supports
both line and block comment forms:

```
/*% JXM_CFMT_GDR 0 */
/*% JXM_CFMT_GDR 1 */
```

`0` disables GDR reindentation for the region following the directive; `1`
re-enables it. Exact semantics still to be designed at implementation time
(e.g. whether it nests, whether an unmatched trailing `0` needs an implicit
end-of-file `1` restore, whether it's an error to mix with
`curly-general-scope-reindent = off`) — captured here only as a **design
requirement**, not yet implemented.

---

## Proposed pre-pass architecture (reduces default-off regression risk to zero)

**Proposal, not yet implemented:** build GDR as a separate pre-pass that
runs BEFORE the source ever reaches the existing formatter pipeline,
entirely gated behind `curly-general-scope-reindent = on` (default `off`,
already present in `STATE_COMMON.md`'s Config Keys and Defaults table).

When `curly-general-scope-reindent = on`, the pre-pass runs first, with:
- its own minimal tokenizer (does not reuse `TokenizerCore`/`TokenizerCurly`),
- its own brace-depth counter (independent of `ScopePipelineCurly`'s),
- its own reindenter (derives each line's absolute target from structural
  depth, per this job's whole purpose),

entirely independent of the existing `ScopePipelineCurly`/`FormatterCurly`
machinery. Only after this pre-pass runs does the (possibly rewritten)
source proceed into the normal, unmodified formatter pipeline exactly as it
does today.

**Rationale / risk-profile change:** when `curly-general-scope-reindent` is
off (the default), the existing formatter code path is completely
untouched — the pre-pass never runs, so it can't share a bug with, or
regress, the existing pipeline. This changes the risk profile from
"modifies shared core pipeline code" (very high risk, per the original risk
analysis above — every line in every file becomes a candidate for a wrong
result) to "isolated additive pre-pass, only active opt-in" — zero blast
radius on the default-off path, which stays the entire ~20+
external-repo-validated regression surface as-is.

**This does not make the pre-pass's own correctness free.** All of the
hard sub-problems listed in the Background section above — continuation-vs-
block depth as a second axis, raw-string/comment/preprocessor-directive/
`frozen`-span exclusion, ordering relative to (in this case, entirely
before) every other pass — still apply in full to the pre-pass's own
implementation. The architecture change only removes the risk of
regressing the *existing* pipeline when the feature is off; it does not
reduce the inherent difficulty of getting the pre-pass itself right when
the feature is on.

---

## D3 fold (Kotlin dogfood cluster D3 → this job)

**As of 2026-08-02, Kotlin dogfood cluster D3 (multi-line-call/condition
wrap-decision flap) is folded into this job.** See `STATE_KOTLIN.md`'s D3
entries (RDD_KEY_221, RDD_KEY_226, the "2026-07-31 D3 scoping session", and
the "2026-08-01 D3 implementation attempt" section) for the full
investigation history — not restated in full here, only summarized as this
job's motivating real-world case.

**Confirmed root cause:** `MiscRuleCurly.renderCallCandidate`'s no-newline
fits-check measures a wrap candidate against its enclosing physical source
line (`lineStartIndex(tokens, nameIdx)`), which is volatile — it shifts
depending on what a *sibling* candidate on the same logical statement did
to the line in an earlier phase of the same pass, or in the previous
round. Two candidate fixes were tried:
1. Anchor measurement at `nameIdx` (RDD_KEY_221) — regressed 28 fixtures
   across C/C++/Java/TS/Kotlin at `make test` (dropped legitimate
   same-statement prefix, an underestimate failure mode). Reverted.
2. `statementStartIndex`, a depth-0 `;`/`{`/`}` backward scan (RDD_KEY_226)
   — passed its own targeted validation but regressed 16 Kotlin fixtures at
   full `make test`, because Kotlin statements are ordinarily
   NEWLINE-separated, not `;`-separated, so the scan walks past the current
   statement into an unrelated preceding sibling statement. Reverted.

Both investigation sessions independently, explicitly concluded (in their
own write-ups, before this fold decision was made) that a real fix needs
actual Kotlin statement-boundary/structural-depth tracking — not a local
token-scan patch — and that this is "closer to `STATE_COMMON.md`'s
'General scope-depth reindentation' architectural TODO's territory than a
self-contained fix." This fold executes that already-self-documented
conclusion; it is not a new judgment call made here.

**Scoped as a sub-goal of this job:** once GDR's own structural-depth
infrastructure (the pre-pass's brace-depth counter / statement-boundary
tracking) lands, D3's fix should be revisited using that infrastructure —
deriving a stable statement-start boundary the same way the pre-pass
derives a stable reindent target — rather than as a standalone patch to
`MiscRuleCurly`. D3 is not blocking GDR's own architecture work; it's a
concrete validation case to revisit once the infrastructure exists.

`STATE_KOTLIN.md` has been updated to point here instead of tracking D3 as
an independently open Kotlin item — see that file's D3 sections and its
Step-5/dogfood summary lines for the pointer back to this file.

---

## When implemented: documentation to update

A future implementer landing real GDR pre-pass logic (or the D3 revisit
built on top of it) must:

- Update `README.md`'s Config Keys / Configuration section to describe
  `curly-general-scope-reindent` as an implemented (not just declared)
  key, and add a "GDR in-file directive" subsection (matching the existing
  "In-file config overrides" section's style) documenting `JXM_CFMT_GDR 0`/
  `JXM_CFMT_GDR 1` (line and block comment forms), placement rules, and
  interaction with `curly-general-scope-reindent`.
- Check `../README.txt` for a formatter config-keys section before editing
  it — as of this writing it has none (no `curly-general-scope-reindent`
  or other formatter config key is documented there), so unless that
  changes in the interim, no `../README.txt` edit should be needed; verify
  this assumption still holds at implementation time rather than trusting
  this note blindly.
- Update `STATE_KOTLIN.md`'s D3 entries (its Category-2/D3 table row and the
  "2026-07-31"/"2026-08-01" D3 session sections) to point at the actual fix
  commit and new `RDD_KEY_n` once the D3 revisit lands, rather than leaving
  them pointing at this file's now-superseded "folded, not yet fixed"
  state.

---

## New test fixtures needed (instructions only — do NOT create these now)

**Out of scope for the current task:** do not implement any GDR
reindentation logic, do not create these fixture files, do not edit the
Makefile or `test/README.txt` yet. This section is instructions for a
**future** session to follow once it's ready to wire up the fixture
skeleton.

Goal: prove a file **can** turn `curly-general-scope-reindent` on via
in-file config (`JXM_CFMT_CFG curly-general-scope-reindent=on`), independent
of whether the reindent logic itself is implemented yet. If the reindent
pre-pass isn't implemented yet when this fixture is authored, the expected
output must be **identical to the input** — i.e. the config key parses and
is accepted without erroring, but has (as yet) no observable effect. Do not
hand-craft an expected output that assumes reindentation happened before
the logic exists.

Language for the fixture doesn't matter — pick whichever is convenient,
e.g. `.hpp` to match the existing `in_file_config_error_inp.hpp` neighbor
fixture's naming/family.

Placement instructions for whoever implements this later:
- **`Makefile`**: register the new fixture(s) in `INP_FILES`, immediately
  AFTER the existing (currently commented-out) line
  `#INP_FILES += in_file_config_error_inp.hpp` — new line(s) go right after
  that, in the same `INP_FILES` block.
- **`test/README.txt`**: document the new fixture(s) immediately after the
  existing `in_file_config_error_inp/out.hpp` entry, in the same list style
  as that entry and its neighbors (one-line name + short description of
  what it proves, matching the surrounding entries' phrasing).

One or two fixture pairs is enough — the goal is proving config
acceptance/no-op behavior, not exercising reindentation shapes (there is no
reindentation logic yet to exercise).

---

## Resolved Design Decisions

- `RDD_KEY_227` — `JXM_CFMT_GDR 0`/`1` directive semantics: **flat toggle**
  (a single `1` always re-enables, redundant `0`s are no-ops, no nesting
  counter); an unmatched trailing `0` at EOF is **neither an error nor an
  implicit restore** — it's moot, since nothing remains to format past EOF
  and the next file starts fresh from its own config regardless; using the
  directive while `curly-general-scope-reindent` is globally `off` is a
  **silent no-op** (parses fine, lets a file be prepared for GDR ahead of a
  project-wide flag flip). Full text: `RDD_KEY_227` in `RDD_LOG.md`.

## Checklist

Status: **directive semantics resolved (RDD_KEY_227); no code yet.** No
implementation item below is checked off — this is the initial concrete
plan, not a placeholder.

- [x] Design/finalize `JXM_CFMT_GDR 0`/`1` directive semantics — resolved,
      see Resolved Design Decisions above (`RDD_KEY_227`).
- [x] Implement the pre-pass's own minimal tokenizer — independent of
      `TokenizerCore`/`TokenizerCurly`, scoped only to what the reindenter
      needs (brace/paren/bracket depth, string/char/comment/raw-string/
      preprocessor-directive recognition for exclusion purposes). Landed as
      new isolated package `com.jxmake.formatter.gdr`: `GdrTokenType.java`
      (enum), `GdrToken.java` (type/text/start-line), `GdrTokenizer.java`
      (`GdrTokenizer.tokenize(String) -> List<GdrToken>`). Recognizes
      single-char bracket tokens (`{}()[]`, depth counting itself deferred
      to the next checklist item's dedicated counter), `//` line comments,
      `/* */` block comments (multi-line), `"..."`/`'...'` literals with
      backslash-escape handling, C++11 `R"delim(...)delim"` raw strings
      (up to 16-char delimiter, falls through to ordinary text/string
      handling if not actually followed by `(` so a bare `R`/`r` identifier
      isn't misdetected), Kotlin `"""..."""` triple-quoted strings, and
      preprocessor directives (line starting with `#` after only
      leading whitespace, backslash-newline continuation). Everything else
      is opaque `TEXT` runs — no identifier/keyword/operator recognition,
      not needed for this job's scope. `make` (full-project `javac` via
      the `SOURCES` glob) builds clean with the new files added — zero
      changes to any existing file. Manually smoke-tested (preprocessor
      continuation, comments, escaped quotes, char literal, C++ raw
      string, nested `(){}[]`) via a throwaway `/tmp` harness against
      `target/classes`; all token boundaries and line numbers correct,
      brace depth balanced to 0 on a nested sample. Known gap, acceptable
      at this stage per the checklist's own scoping (exclusion zones like
      `frozen`/`JXM_CFMT_GDR 0`/`1` spans are a later checklist item, not
      this one): no handling yet for languages/literal shapes outside
      C/C++/Java/Kotlin (this job's explicit scope, see "Scoping" section
      below) or for malformed/unterminated literals beyond falling back to
      end-of-line/end-of-file.
- [x] Implement the pre-pass's own brace-depth counter, independent of
      `ScopePipelineCurly`'s. Landed `GdrLineBraceDepth.java` (per-line
      `depthAtStart`/`depthAtEnd`) and `GdrBraceDepthCounter.java`
      (`compute(List<GdrToken>) -> List<GdrLineBraceDepth>`) in the same
      `com.jxmake.formatter.gdr` package. Consumes `GdrTokenizer`'s output
      directly — every `BRACE_OPEN`/`BRACE_CLOSE` token it sees is already
      real structural code (strings/comments/preprocessor interiors were
      excluded by the tokenizer), so the counter itself is a simple
      running increment/decrement, with `depthAtStart` recorded before a
      line's own brace tokens are applied and `depthAtEnd` after. Scoped
      to brace depth only, matching the checklist item's own wording —
      paren/bracket depth for the continuation-vs-block second axis is
      deferred to the reindenter (next item), not duplicated here.
      Deliberately does not attempt the closing-brace-dedent decision
      (whether a line led by `}` should reindent to `depthAtStart` or
      `depthAtEnd`) — that's the reindenter's job per this job's own
      "Continuation vs. block depth is a second axis" background note;
      this counter just exposes both numbers per line. `make` builds
      clean, zero changes to any existing file. Manually smoke-tested
      (nested `if` inside `main`, embedded multi-line block comment) via
      a throwaway `/tmp` harness — depth tracked correctly across the
      comment's embedded newlines (no spurious depth change) and matched
      expected nesting at every line.
- [~] Implement the pre-pass's own reindenter: derive each line's absolute
      indent target from structural depth, merging in a continuation-vs-
      block second axis (STYLE.md §8's wrapped-call/declaration
      convention — checked directly, since STYLE.md §2 is "Line Length"
      and has no continuation-indent rule of its own; §8 is the actual
      source of the "one level in from the opening line, closer dedents
      back to the opening line's own indent" convention this reindenter
      follows) rather than a naive one-level-per-`{` model. In progress,
      sub-steps:
      - [x] Paren/bracket depth counter (the continuation axis) —
            `GdrLineParenBracketDepth.java`/`GdrParenBracketDepthCounter.java`,
            same structure as the brace counter, `(`/`[` counted together
            per §8's identical treatment of both. Smoke-tested: a wrapped
            `foo(\n    arg1,\n    arg2\n);` call correctly shows depth 1
            on its interior lines and 0 once closed; same-line
            `arr[3]`/`a[0]` pairs correctly net to 0 (not a continuation,
            handled by the brace counter instead since that example's
            actual continuation was via `{`). `make` builds clean, zero
            changes to any existing file.
      - [ ] Line-touchability classifier (skip lines that are interior
            continuation of a multi-line STRING/BLOCK_COMMENT/
            PREPROCESSOR token — content that must never be reindented).
      - [ ] Combine brace depth + paren/bracket depth into a single
            per-line absolute indent target, with the leading-closer
            dedent rule (a line whose first significant token is a
            closing bracket reindents to the depth *after* that close,
            matching the opening line, not the body depth).
- [ ] Implement content exclusions: raw string literals, block-comment
      interior lines, preprocessor directives (column-0, own continuation
      rules), `frozen`/JXM_CFMT_DIS-ENA spans, and any region bracketed by
      `JXM_CFMT_GDR 0`/`1`.
- [ ] Wire `curly-general-scope-reindent = on` to actually invoke the
      pre-pass ahead of the existing formatter pipeline (`Main`/
      `ServerMode` entry points) — confirm the off/default path is
      byte-for-byte unchanged by diffing pre-pass-on-vs-off code paths, not
      just by reasoning about the gate.
- [ ] Author the "New test fixtures needed" pair(s) above (config-acceptance
      only, no-op expected output if the reindenter isn't ready) — or the
      real reindent-shape fixtures once the pre-pass itself is implemented.
- [ ] Update `README.md` (and re-verify whether `../README.txt` needs an
      edit — see "When implemented" section) once the above lands.
- [ ] Real-code test the pre-pass, `curly-general-scope-reindent = on`,
      against at least `javaparser/javaparser`, local
      `tool/JSONEncoderLite.java`, and `serge-sans-paille/frozen` (the three
      corpora where the current narrower relative-delta reindent bugs
      surfaced) — full-tree idempotency, not `--out DIR` sampling.
- [ ] Revisit Kotlin dogfood cluster D3 (see "D3 fold" section above) using
      the pre-pass's statement-boundary/structural-depth infrastructure;
      land a real fix in `MiscRuleCurly`/wherever the fix ends up living,
      record a new `RDD_KEY_n`, update `STATE_KOTLIN.md`'s D3 entries to
      point at it.

Do the above checklist one by one. Test, commit, and ask me whether to continue or pause.

## Scoping

This job's scope is **the GDR pre-pass architecture plus D3's eventual
fix** — nothing else. Concretely:

- **Does not touch the existing formatter pipeline code paths at all**
  while `curly-general-scope-reindent` stays at its default (`off`). No
  change to `ScopePipelineCurly.java`, `FormatterCurly.java`, `MiscRule*`,
  `TokenizerCore`/`TokenizerCurly`, or any other shared class is expected
  or permitted as part of the default-off path — the whole point of the
  pre-pass architecture (see above) is that the on/off gate lives entirely
  outside those classes, at the point where a source file first enters the
  pipeline.
- **Likely primary implementation surface is a brand-new, isolated pre-pass
  module** (e.g. something like a new top-level package/class such as
  `com.jxmake.formatter.gdr.GeneralScopeDepthReindenter` or similar — exact
  naming is an implementation-time decision, not fixed here) — **NOT**
  `ScopePipelineCurly.java`. This explicitly supersedes the old
  (pre-2026-08-02) `STATE_COMMON.md` text's speculation that a general
  reindent pass would "likely touch `ScopePipelineCurly.java` primarily,
  potentially subsuming/replacing `SwitchRule.applyNonInlineCaseIndent`'s
  relative-delta logic" — that assumption predates the pre-pass proposal
  and no longer holds. The two existing narrow relative-delta reindenters
  (`SwitchRule.applyNonInlineCaseIndent`, `ScopePipeline.applyDeclarationsPass`)
  are left untouched by this job; whether they're ever retired in favor of
  the pre-pass's absolute-target model is an open question for whenever
  the pre-pass is mature, not part of this job's initial scope.
- **D3's eventual fix is in scope**, but only once the pre-pass's
  statement-boundary/structural-depth infrastructure exists to build it on
  — it is not a standalone task to attempt in isolation again (both prior
  standalone attempts, RDD_KEY_221 and RDD_KEY_226, were tried and
  reverted; re-attempting without the underlying infrastructure would just
  repeat that history).
- **The `curly-general-scope-reindent = on` path's own correctness is a
  hard, multi-session problem** (continuation-vs-block depth, exclusion
  zones, ordering-before-everything-else) — it is explicitly not a
  small/incremental task even though the default-off path is now
  zero-risk. Treat any single session's progress on the `on` path as
  partial by default; do not assume a quick win.
- Out of scope entirely for this job: any change to data-format
  (JSON/YAML/etc.), JS/TS, or Python3 indentation logic — GDR as scoped
  here is specifically the curly-brace-family (C/C++/Java/Kotlin)
  `"cpp"`-pipeline-adjacent reindentation problem, matching where the old
  `STATE_COMMON.md` TODO lived before this split.
