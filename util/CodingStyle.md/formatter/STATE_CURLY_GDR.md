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

**Overall status: pre-pass architecture landed and wired up behind
`curly-general-scope-reindent = on` (default off); real-code validation
against its originally-scoped corpora not yet done, and a real
pass-ordering bug was found during a first, differently-scoped real-code
test (`RDD_KEY_229`) — see the Checklist's last two items below.** This
file was split out of `STATE_COMMON.md`'s old "Architectural TODOs"
section (which held only a risk-analysis writeup, no implementation) on
2026-08-02, with the design expanded per the discussion below.

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

Build GDR as a separate pre-pass that runs BEFORE the source ever reaches
the existing formatter pipeline, entirely gated behind
`curly-general-scope-reindent = on` (default `off`, in `STATE_COMMON.md`'s
Config Keys and Defaults table). When on, the pre-pass runs first with its
own minimal tokenizer (not `TokenizerCore`/`TokenizerCurly`), its own
brace-depth counter (independent of `ScopePipelineCurly`'s), and its own
reindenter (absolute target from structural depth) — entirely independent
of the existing `ScopePipelineCurly`/`FormatterCurly` machinery. Only after
the pre-pass runs does the (possibly rewritten) source proceed into the
normal, unmodified formatter pipeline exactly as it does today.

**Rationale / risk-profile change:** when off (the default), the existing
formatter code path is completely untouched — the pre-pass never runs, so
it can't share a bug with or regress the existing pipeline. This changes
the risk profile from "modifies shared core pipeline code" (very high risk,
per the Background analysis above — every line in every file becomes a
candidate for a wrong result) to "isolated additive pre-pass, only active
opt-in" — zero blast radius on the default-off path, which stays the entire
~20+ external-repo-validated regression surface as-is.

**This does not make the pre-pass's own correctness free** — all the hard
sub-problems in the Background section (continuation-vs-block depth as a
second axis, raw-string/comment/preprocessor-directive/`frozen`-span
exclusion, ordering entirely before every other pass) still apply in full
to the pre-pass's own implementation. The architecture change only removes
regression risk to the *existing* pipeline when the feature is off.

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

## Open design proposal: bounded multi-pass remediation for RDD_KEY_229 (discussion only, NOT decided/implemented)

**User proposal (2026-08-03):** since GDR is a pre-pass, it can't see brace-
placement/line-wrap decisions the normal pipeline hasn't made yet, and a
post-pass ordering trades that bug for a different one (indentation-width
changes flipping wrap fits-checks) — see `RDD_KEY_229`'s full writeup for
both failure modes. Proposed remediation: a new config key,
**`curly-gs-reindent-multipass`** (`off` default, `on`), that — only when
`curly-general-scope-reindent` is also `on` — runs a fixed 4-stage sequence
instead of GDR's current single pre-pass-then-pipeline order:

1. GDR (pre-pass, as today)
2. Normal formatting pass (pipeline, as today)
3. GDR again
4. Normal formatting pass again

This is a **concrete instantiation of one of the two remediation options
`RDD_KEY_229` already named but explicitly did not attempt** ("iterate
pipeline+GDR to a bounded fixpoint") — this section is not a new idea from
scratch, it's picking that option back up with a specific, boundable shape
(fixed 4 stages, not an open-ended loop-until-stable) instead of the
unbounded fixpoint iteration `RDD_KEY_229` left unscoped.

### Why this plausibly resolves the circular dependency

Stage 1 (GDR-1) computes depth from the *original* source's brace/paren
nesting — wrong for any line the pipeline is about to split or join (the
`RDD_KEY_229`/javaparser dominant-failure-mode case: a joined `} else if
(...) {` later Allman-split into two lines, where the newly split-out line
never got its own GDR target). Stage 2 (pipeline-1) makes all of its
brace-placement/line-wrap decisions using GDR-1's already-mostly-correct
indentation as input — closer to final width than an unindented or
relative-delta-indented source, so fewer wrap-decision errors than today's
plain post-pass ordering (which starts pipeline from arbitrary/original
indentation, not GDR-adjusted). Stage 3 (GDR-2) now runs on already-
finalized brace placement and line splits/joins (pipeline-1's output), so
every line — including ones newly created by stage 2's own Allman-splitting
— gets a correct absolute depth-based target this time, since the structure
it's measuring is the *actual final* structure, not a pre-reflow guess.
Stage 4 (pipeline-2) exists because stage 3's reindentation can still change
line widths enough to flip a wrap fits-check that stage 2 decided under
stage-1's slightly-different widths (this is the exact circular-dependency
mechanism `RDD_KEY_229`'s post-pass-ordering experiment found) — one more
pipeline pass lets those decisions re-settle against the now-correct
widths.

Because GDR only ever rewrites leading whitespace (never moves, splits, or
joins tokens/lines — confirmed by `GdrRewriter.rewrite`'s existing
implementation, "replaces each touchable line's leading whitespace ...
leaves the rest of the line byte-for-byte untouched"), and the pipeline's
own passes are what own all structural reflow, the two alternate cleanly
without either one undoing the other's *kind* of edit — GDR's edits can't
un-split what the pipeline joined or vice versa, they only ever adjust the
number learned from whatever structure currently exists.

### Whether this achieves true idempotency (not just "closer")

This is a heuristic, not a proof. Assuming stage 4 reaches a stable
width/wrap-decision state (no residual oscillation), a *second* full
4-stage application of the same source should be a no-op end to end: GDR is
a pure function of current brace/paren structure (independent of whatever
indentation was already there), so GDR-1 of round 2 recomputes the same
targets stage 3 already wrote; pipeline-1 of round 2 re-decides the same
wraps stage 4 already settled on (same widths in, same decisions out);
GDR-2/pipeline-2 of round 2 are then no-ops on an already-fixed-point
source. This composes correctly with `make test`'s existing round1/round2
idempotency check — no special-casing needed there, a file that reaches a
true fixed point by stage 4 will simply pass idempotency as-is, and one
that doesn't will fail it exactly the way any other idempotency bug does
today.

**What is NOT proven, and would need real-code validation before trusting
this generally:** whether 4 stages is *always* enough. The known failure
mode this is meant to fix is a **first-order** effect (one missed target on
a newly-split line); the residual risk is a **second-order** oscillation —
a wrap decision whose own flip (stage 4) changes width in a way that would
still disagree with a hypothetical stage-5 GDR pass. `RDD_KEY_229`'s own
post-pass-ordering experiment found real flapping of exactly this shape
between GDR and the pipeline, so it is not purely theoretical; the open
question is whether it damps out after one extra round (this proposal) or
needs more. This can only be answered by re-running the same real-code
corpora `RDD_KEY_229` already exposed the bug against
(`javaparser/javaparser`'s `javaparser-core-generators` 13/43 non-idempotent
files, plus the `angular/angular` TS cluster-5 files) with multipass wired
up — a residual non-idempotent file after 4 stages, if any remain, would at
least narrow from "the dominant failure mode across ordinary code" (today)
to "a smaller residual set", which is progress either way, not a
prerequisite this proposal needs to fully close to be worth landing.

### Other open questions this proposal surfaces (not yet answered)

- **Interaction with the pipeline's OWN relative-delta reindenters**
  (`SwitchRule.applyNonInlineCaseIndent`, `ScopePipeline.
  applyDeclarationsPass`, and STYLE.md §8's multi-line param-list/
  declaration continuation-indent renderer in `MiscRuleCurly`) — these
  already run inside "normal formatting pass" (stages 2/4) and use their
  own indent models, not GDR's absolute-depth-plus-paren-axis model
  directly. `GdrReindenter`'s Background section already claims its
  continuation-vs-block axis matches STYLE.md §8's convention (see the
  "Implement the pre-pass's own reindenter" checklist item), but that
  claim was validated via smoke tests, not against these specific passes'
  actual output shapes. Stage 3 (GDR-2) running on the pipeline's already-
  rendered switch-case bodies / wrapped declarations needs to *agree* with
  what those passes just wrote, or it will silently overwrite
  correctly-STYLE.md-compliant indentation with GDR's own (possibly
  differently-shaped) target on the second pass — this is a real,
  unverified risk, not a formality.
- **Whether `curly-gs-reindent-multipass = on` while `curly-general-scope-
  reindent = off` is a no-op, a usage error, or silently ignored** — same
  category of question `RDD_KEY_227` already resolved for the `JXM_CFMT_GDR`
  directive's interaction with the master flag; needs the same kind of
  explicit resolution before implementation, not an assumption.
- **Naming.** The user's suggested `curly-gs-reindent-multipass` uses a
  different abbreviation style (`gs-reindent`) than the existing
  `curly-general-scope-reindent` key it depends on (spelled out in full).
  Worth deciding at implementation time whether to match the existing key's
  full-word style (e.g. `curly-general-scope-reindent-multipass`) for
  config-key consistency, or keep the shorter suggested form — flagged
  here, not decided.
- **Cost.** Opt-in only, so zero cost on the default-off path (same
  guarantee as the base `curly-general-scope-reindent` flag), but doubles
  wall-clock cost of the already-expensive GDR-pre-pass-plus-full-pipeline
  combination for any file that does enable it — worth a one-line
  `README.md` mention once/if implemented, not a blocker.

### Verdict

**Plausible and worth prototyping** — it directly targets the confirmed
root cause (`RDD_KEY_229`) with a bounded, cheap-to-reason-about shape
(GdrRewriter's whitespace-only edits mean the two pass kinds can't
structurally fight each other), and composes with the existing idempotency
test with no special-casing. It is not a proven fix (see "second-order
oscillation" above) and has at least one real unverified risk (the
relative-delta-reindenter interaction). **Per `RDD_KEY_229`'s own note**
("both remediation paths too risky to attempt this session... a future
session should ask before attempting either remediation path"), this
write-up is the "ask" — implementation should wait for an explicit go-ahead
in a future session, at which point it should be scoped as its own
checklist item here (new `RDD_KEY_n` once real design decisions are made,
e.g. the no-op/error question above), validated first via the same
`javaparser-core-generators`/`angular` cluster-5 files `RDD_KEY_229` already
has failure data for before calling it done.

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
- `RDD_KEY_233` — `curly-general-scope-reindent-multipass` naming: full-word
  style (matching the base `curly-general-scope-reindent` key), not the
  originally-suggested `curly-gs-reindent-multipass` abbreviation. Full
  text: `RDD_KEY_233` in `RDD_LOG.md`.
- `RDD_KEY_234` — `curly-general-scope-reindent-multipass = on` while
  `curly-general-scope-reindent = off`: **silent no-op**, same resolution
  category as `RDD_KEY_227`. Full text: `RDD_KEY_234` in `RDD_LOG.md`.
- `RDD_KEY_228` — Scope expanded to include JS/TS (user-directed): both
  plain `.js`/`.ts` files and embedded HTML `<script>` content are now
  reindented by GDR when on. Also fixed an independent HTML-formatter bug
  found while testing this: a `%`-prefixed marker/directive HTML comment
  (e.g. `<!--% JXM_CFMT_CFG ... -->`) was being corrupted into
  `<!-- % ... -->` by ordinary comment rendering, permanently breaking the
  marker's required exact prefix on any subsequent parse. Full text:
  `RDD_KEY_228` in `RDD_LOG.md`.

## Checklist

Status: **directive semantics resolved (RDD_KEY_227); no code yet.** No
implementation item below is checked off — this is the initial concrete
plan, not a placeholder.

- [x] Design/finalize `JXM_CFMT_GDR 0`/`1` directive semantics — resolved,
      see Resolved Design Decisions above (`RDD_KEY_227`).
- [x] Implement the pre-pass's own minimal tokenizer — independent of
      `TokenizerCore`/`TokenizerCurly`, scoped to what the reindenter needs
      (bracket tokens, string/char/comment/raw-string/preprocessor
      recognition for exclusion). Landed as new isolated package
      `com.jxmake.formatter.gdr`: `GdrTokenType.java`, `GdrToken.java`,
      `GdrTokenizer.java` (`tokenize(String) -> List<GdrToken>`).
      Recognizes single-char bracket tokens (`{}()[]`, depth counting
      deferred to the next item), `//`/`/* */` comments, `"..."`/`'...'`
      literals (backslash-escape aware), C++11 `R"delim(...)delim"` raw
      strings (falls through to ordinary handling if not followed by `(`),
      Kotlin `"""..."""` triple-quoted strings, and preprocessor
      directives (leading-whitespace-then-`#`, backslash-newline
      continuation); everything else is opaque `TEXT`. `make` builds
      clean, zero existing-file changes. Smoke-tested via a `/tmp` harness
      (preprocessor continuation, comments, escapes, char literal, C++ raw
      string, nested brackets) — all boundaries/line numbers correct,
      brace depth balanced to 0. **Known gap, acceptable at this stage:**
      no handling for languages/literal shapes outside C/C++/Java/Kotlin,
      or malformed/unterminated literals beyond falling back to
      end-of-line/file.
- [x] Implement the pre-pass's own brace-depth counter, independent of
      `ScopePipelineCurly`'s. Landed `GdrLineBraceDepth.java` (per-line
      `depthAtStart`/`depthAtEnd`) + `GdrBraceDepthCounter.java`
      (`compute(List<GdrToken>) -> List<GdrLineBraceDepth>`). Consumes
      `GdrTokenizer`'s output directly (strings/comments/preprocessor
      interiors already excluded), so it's a simple running
      increment/decrement. Brace depth only — paren/bracket depth is the
      next item's job. Deliberately does not decide the closing-brace-
      dedent question (that's the reindenter's job); just exposes both
      numbers per line. `make` builds clean. Smoke-tested (nested `if`
      inside `main`, embedded multi-line block comment) — depth tracked
      correctly, no spurious change across the comment's embedded
      newlines.
- [x] Implement the pre-pass's own reindenter: derive each line's absolute
      indent target from structural depth, merging in a continuation-vs-
      block second axis (STYLE.md §8's wrapped-call/declaration
      convention — "one level in from the opening line, closer dedents
      back to the opening line's own indent"; §2 is "Line Length" and has
      no continuation rule) rather than a naive one-level-per-`{` model.
      Landed as `GdrIndentTarget`/`GdrReindenter`, computing per-line
      absolute levels/columns only (does not yet rewrite source text —
      that's the pipeline-integration item below). Sub-steps:
      - [x] Paren/bracket depth counter (continuation axis) —
            `GdrLineParenBracketDepth.java`/`GdrParenBracketDepthCounter.java`,
            same shape as the brace counter, `(`/`[` counted together per
            §8. Smoke-tested: wrapped call args show depth 1 on interior
            lines / 0 once closed; same-line `arr[3]`/`a[0]` pairs net to
            0. `make` builds clean.
      - [x] Line-touchability classifier (skip lines that are interior
            continuation of a multi-line STRING/BLOCK_COMMENT/
            PREPROCESSOR token — content that must never be reindented,
            an inherent correctness requirement distinct from the later
            opt-in exclusion-zone item below). `GdrLineTouchability.java`:
            `computeUntouchableLines`/`computeTouchableByLine`, derived
            from `GdrToken`s spanning `>0` embedded newlines — every line
            strictly after such a token's start line through its end line
            is untouchable; the start line itself stays touchable.
            Smoke-tested against block-comment/preprocessor-continuation/
            single-line-string/C++-raw-string cases (9 lines), all
            matched expectations.
      - [x] Combine brace depth + paren/bracket depth into a single
            per-line absolute indent target, with the leading-closer
            dedent rule (a line whose first significant token is a
            closing bracket reindents to the depth *after* that close,
            matching the opening line). `GdrIndentTarget.java` +
            `GdrReindenter.java` (`compute(String, int indentSize) ->
            List<GdrIndentTarget>`, self-contained). Per line, each axis
            independently uses its own `depthAtEnd` instead of
            `depthAtStart` only when that axis's own closing token leads
            the line (via new `computeLeadingTokenTypes`); `level =
            braceLevel + pbLevel`, `columns = level * indentSize`.
            Untouchable lines get a `touchable=false` placeholder.
            Smoke-tested a wrapped signature containing a nested `if`
            block: every line's level matched hand-derived expectations,
            including both paren-led-closer and brace-led-closer cases.
- [x] Implement content exclusions: raw string literals, block-comment
      interior lines, preprocessor directives, `frozen`/JXM_CFMT_DIS-ENA
      spans, and any region bracketed by `JXM_CFMT_GDR 0`/`1`. The first
      three were already covered by the touchability sub-step above
      (inherent, not opt-in). Remaining scope: the two opt-in
      marker-comment families — new `GdrExclusionZones.
      computeExcludedByLine(List<GdrToken>) -> List<Boolean>`,
      independently reimplementing (not sharing code with) the existing
      pipeline's line-anchored `//% JXM_CFMT_DIS`/`ENA` (+ block-comment
      form) regex convention (confirmed exact pattern by reading
      `TokenizerCore.FORMAT_DIS_MARKER`/`FORMAT_ENA_MARKER`, read-only),
      plus the new `//% JXM_CFMT_GDR 0`/`1` directive per `RDD_KEY_227`'s
      flat-toggle semantics. Both families independently toggle (OR'd
      together); the marker comment's own line is always excluded, same
      as the existing frozen-span convention. Wired into
      `GdrReindenter.compute`. Smoke-tested standalone (DIS/ENA and GDR
      0/1 marker lines each correctly excluding only their own span) and
      end-to-end (a DIS/ENA-bracketed span inside a function body
      correctly excluded, levels resuming correctly on both sides).
- [x] Wire `curly-general-scope-reindent = on` to actually invoke the
      pre-pass ahead of the existing formatter pipeline (`Main`/
      `ServerMode` entry points) — confirm the off/default path is
      byte-for-byte unchanged, not just by reasoning about the gate.
      Added `curly-general-scope-reindent` to `Config.java` (field, getter
      `isCurlyGeneralScopeReindent()`, `ALL_KEYS`, `parseBoolean` — default
      `false`, same convention as every other boolean key) and
      `com.jxmake.formatter.gdr.GdrRewriter.rewrite(source, indentSize)`,
      the first class in this package to actually rewrite source: calls
      `GdrReindenter.compute`, replaces each touchable line's leading
      whitespace with `target.columns` spaces, leaves the rest of the line
      byte-for-byte untouched; untouchable/blank lines copied verbatim.
      Smoke-tested a nested-brace + wrapped-call-args snippet — output
      matched hand-derived expected indentation exactly. Then added
      `GdrPipelineGate.apply(source, language, config)` (off or
      non-curly-family → unchanged; on + curly-family → `GdrRewriter.
      rewrite`), called from both entry points right before
      `FormatterCore.forLanguage(language).formatOne`
      (`Main.formatStandalone`, `ServerMode`'s request handler). Verified
      the off-path is byte-for-byte unchanged by asserting `GdrPipelineGate
      .apply` returns the exact same `String` reference (`==`) when the key
      is unset; verified the on-path via a direct harness against
      `Config.resolve`. Also confirmed via `--standalone --diff` that
      toggling `JXMAKE_CODE_FORMATTER_CURLY_GENERAL_SCOPE_REINDENT`
      produces identical CLI output for already-correctly-indented input
      (expected — the direct-`==` harness is the real proof of the
      off-path guarantee, not this CLI diff). `make` builds clean, zero
      changes to any existing pipeline class's own logic (only the two
      call sites plus the new `Config.java` key).
- [x] Author the "New test fixtures needed" pair(s) above (config-acceptance
      only if the reindenter isn't ready) — or the real reindent-shape
      fixtures once the pre-pass is implemented. Added
      `test/curly_general_scope_reindent_inp.hpp`/`_out.hpp` (registered in
      `Makefile`/`test/README.txt` right after the `in_file_config_error_*`
      neighbor), using `JXM_CFMT_CFG curly-general-scope-reindent=on;
      indent-size=2` on a badly-indented `struct` body. Chose
      config-acceptance framing over a reindent-isolating one: empirically
      confirmed (via `--standalone --diff`) that the existing pipeline
      already fully reformats braces/call-args regardless of GDR, so final
      CLI output is pipeline-dominated either way — GDR's own effect is
      proven instead by the direct-harness `==`/rewrite assertions in the
      wiring item above. Expected output generated via
      `--standalone --in-place`, confirmed idempotent via
      `--standalone --check` (exit 0). `make test`: 226/226
      forward+idempotency including the new fixture.

      **Follow-up:** added `test/java_flush_left_inp.java`/`_out.java` —
      `curly-general-scope-reindent=on` where every input line is flushed
      to column 0. This one *does* isolate GDR's own contribution
      end-to-end: without the directive, the base pipeline leaves the body
      completely unindented (relative-delta reindentation has nothing to
      anchor to with no indentation anywhere), while GDR-on produces fully
      correct nested indentation — a concrete demonstration of the bug
      this job exists to fix. Registered after `java_preprocessor_method_
      inp.java`. Also added `test/html_js_flush_left_inp.html`/`_out.html`
      — NOT a GDR fixture (GDR excludes HTML/JS, see Scoping) but a
      real-code-regression-style fixture: an HTML document flushed to
      column 0 including a flushed-left `<script>` block, exercising the
      existing HTML5/JS dispatch pipeline. Idempotent, but surfaces the
      same relative-delta limitation inside the dispatched JS body (out of
      scope here, documented in `test/README.txt` so it isn't mistaken for
      a GDR gap). Registered after `html_comments_inp.html`. `make test`:
      228/228 forward+idempotency.
- [x] Update `README.md` (and re-verify whether `../README.txt` needs an
      edit — see "When implemented" section) once the above lands. Done
      2026-08-02: rewrote the stale "Known Limitations" bullet (previously
      said GDR "is not supported"; now describes it as opt-in via
      `curly-general-scope-reindent = on`, including the js/ts + embedded
      HTML5 `<script>` scope and the flush-left case it fixes). Added a new
      "GDR in-file directive" subsection right after "In-file config
      overrides" documenting `JXM_CFMT_GDR 0`/`1` (line/block forms,
      anywhere-in-file placement, flat-toggle semantics, unmatched-trailing-
      `0`-at-EOF is a no-op, silent no-op when the feature is globally off)
      per RDD_KEY_227. Checked `../README.txt`: still has no formatter
      config-keys section, confirmed the "When implemented" note's
      assumption still held, so no edit needed there.
- [ ] Real-code test the pre-pass, `curly-general-scope-reindent = on`,
      against at least `javaparser/javaparser`, local
      `tool/JSONEncoderLite.java`, and `serge-sans-paille/frozen` (the three
      corpora where the current narrower relative-delta reindent bugs
      surfaced) — full-tree idempotency, not `--out DIR` sampling. **Not yet
      done against these three.** A first real-code test WAS run instead
      (2026-08-02, `RDD_KEY_229`) against `angular/angular`'s TS dogfood
      cluster-5 accepted-gap files (`STATE_JS_TS.md`), since GDR's scope
      expanded to JS/TS (`RDD_KEY_228`) after this item was originally
      written. Result: 1 of 3 files (`emit.ts`) fixed cleanly; 2 of 3
      (`user_metric_spec.ts`, `i18n_parse.ts`) exposed a real, confirmed
      **pass-ordering bug**, not yet fixed — see `RDD_KEY_229` for full
      detail. Summary: GDR's pre-pass computes indent depth before
      brace-placement runs, so source using joined one-true-brace style
      (`} else if (...) {`) gets a wrong/non-idempotent indent once
      brace-placement later splits that line into `}` / `else if (...) {`
      to match this formatter's Allman style — the split-out line never
      gets its own GDR target. A post-pass ordering (GDR after the
      pipeline instead of before) was tried as a candidate fix and DOES
      resolve this specific case, but was confirmed to introduce a
      different non-idempotency: GDR's indentation change alters line
      width, which flips the pipeline's own line-wrap fits-check decision
      on the next round — a genuine circular dependency between GDR's
      depth-based indent and the pipeline's width-based wrap decisions.
      **User explicitly judged both remediation paths (bounded fixpoint
      iteration; feeding GDR's indent into the wrap fits-check) too risky
      to attempt this session — no source code changed.** `README.md`'s
      Known Limitations GDR bullet now documents the joined-brace-style
      gap for users. This item stays open/unchecked; a future session
      picking it up should read `RDD_KEY_229` in full before attempting
      either remediation path, and should still run the original
      `javaparser`/`JSONEncoderLite`/`frozen` real-code pass this item was
      written for.

      **2026-08-02 session, partial progress:** ran the originally-scoped
      real-code pass against `javaparser/javaparser` (fresh clone,
      `/tmp/javaparser_gdr`), starting with its smallest module
      (`javaparser-core-generators`, 43 files) per this file's "work bit by
      bit" convention, `curly-general-scope-reindent=on`, full-tree
      idempotency (round1 vs round2, not `--out DIR` sampling). Result: 13
      of 43 files non-idempotent. Inspected every failing file's diff (not
      just a sample) — **all 13 are the same root cause as `RDD_KEY_229`**,
      not 13 distinct bugs: a closing `}`/`)` on a line the pipeline's own
      brace-placement/line-wrap passes later re-split or re-joined loses
      its GDR-computed indent target, landing at the pre-split line's depth
      instead of the post-split one (e.g. `GrammarLetterGenerator.java`'s
      `else {` block closer measured at the wrong depth after the
      pipeline's wrap decision moved it). This confirms `RDD_KEY_229`'s
      pass-ordering bug is not TS-specific — it reproduces identically in
      plain Java under normal Allman-reflow/call-wrap activity, so it is
      the dominant (likely majority) failure mode across ordinary curly-
      family code, not a rare edge case. No independently-fixable bug was
      found in this batch; every failure traces to the one already-deferred
      design issue, so per `RDD_KEY_229`'s note ("both remediation paths
      too risky to attempt this session") and `STATE_COMMON.md`'s
      ambiguity-handling protocol, **no source code was changed this
      session either** — attempting either remediation path is a new
      design judgment call, not a bug fix, and the user's prior explicit
      risk judgment on it stands until revisited directly. `JSONEncoderLite.
      java` and `serge-sans-paille/frozen` were not yet reached this
      session (stopped after the first module to avoid spending the rest of
      the corpus pass on restating the same finding).

      **Left off here:** the rest of `javaparser/javaparser` (`javaparser-
      core` is the large main module, not yet run), local `tool/
      JSONEncoderLite.java`, and `serge-sans-paille/frozen` are still
      untested this cycle — but given 13/13 failures in the first module
      trace to one root cause, expect the same dominant failure mode there
      too rather than new bug categories. **Before spending more real-code-
      testing cycles on this corpus, the higher-leverage next step is
      revisiting `RDD_KEY_229`'s remediation options directly** (bounded
      fixpoint iteration between GDR and the pipeline; or feeding GDR's
      precomputed indent into `MiscRuleCurly.renderCallCandidate`'s wrap
      fits-check instead of the line's raw current indentation) — continuing
      to run more corpora without addressing the root cause will keep
      surfacing the same already-documented bug rather than new, actionable
      ones. That said, this is a real design decision with prior explicit
      user risk judgment against attempting it, so a future session should
      ask before attempting either remediation path rather than proceeding
      unilaterally. `/tmp/javaparser_gdr` (fresh clone) and `/tmp/gdr_r1`/
      `/tmp/gdr_r2` (round1/round2 output for the tested module) were left
      in place for reuse by the next session per the "search `/tmp` for an
      existing checkout" convention.
- [x] **Prototype bounded multi-pass remediation for `RDD_KEY_229`** (user
      go-ahead given 2026-08-03, see the "Open design proposal" section
      above). Resolved the two flagged open design questions first, as new
      `RDD_LOG.md` entries: naming (`RDD_KEY_233`, full-word style —
      `curly-general-scope-reindent-multipass`, not the shorter
      `curly-gs-reindent-multipass`) and the multipass-on/base-off
      interaction (`RDD_KEY_234`, silent no-op, same resolution category as
      `RDD_KEY_227`). Implemented: `Config.java` gained the new key (field
      `curlyGeneralScopeReindentMultipass`, getter
      `isCurlyGeneralScopeReindentMultipass()`, `ALL_KEYS`, `parseBoolean`,
      default `false`). `GdrPipelineGate` gained `applyAndFormat(source,
      language, config, filePath, formatOff)`, the new single entry point
      both `Main.formatStandalone` and `ServerMode`'s request handler now
      call instead of separately calling `apply` then
      `FormatterCore.forLanguage(...).formatOne(...)`: runs GDR once then
      `formatOne` once (unchanged behavior) when multipass is off or the
      base flag is off; when both flags are on and the language is
      curly-family, runs the full 4-stage sequence (GDR, pipeline, GDR,
      pipeline) by calling `apply`/`formatOne` a second time on the first
      pipeline pass's own output. `make` builds clean; `make test`:
      237/237 forward + idempotency, zero regressions on the existing
      (default-off) suite — the new path is exercised only when both flags
      are explicitly turned on. No new local fixture added yet (deferred
      to the next checklist sub-item); `STATE_COMMON.md`'s Config Keys
      table and `README.md`'s Configuration section both updated with the
      new key. **Next sub-items (not yet done this pass):** a small smoke
      fixture proving the 4-stage sequence actually runs with both flags
      on, and real-code validation against the `angular/angular` TS
      cluster-5 files and `javaparser-core-generators`
      (`/tmp/javaparser_gdr`) that originally exposed `RDD_KEY_229`.
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
- **Primary implementation surface is the new isolated `com.jxmake.
  formatter.gdr` pre-pass package** (already landed, see checklist above)
  — **NOT** `ScopePipelineCurly.java`. This supersedes old
  (pre-2026-08-02) speculation that a general reindent pass would touch
  `ScopePipelineCurly.java`/`SwitchRule.applyNonInlineCaseIndent` directly;
  that assumption predates the pre-pass proposal. The two existing narrow
  relative-delta reindenters (`SwitchRule.applyNonInlineCaseIndent`,
  `ScopePipeline.applyDeclarationsPass`) are left untouched by this job;
  whether they're ever retired in favor of the pre-pass's absolute-target
  model is an open question for whenever the pre-pass is mature.
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
- **Scope expanded 2026-08-02 per `RDD_KEY_228` (user-directed): JS/TS are
  in scope.** JS/TS are curly-brace-family too, same reindentation problem
  as C/C++/Java/Kotlin, and `GdrPipelineGate.isCurlyFamily` now includes
  `"js"`/`"ts"` — both plain `.js`/`.ts` files AND embedded HTML
  `<script>` content (via `XmlSpecificRule.renderScriptOrStyle`'s own
  `GdrPipelineGate.apply` call) are reindented when
  `curly-general-scope-reindent` is on. `GdrTokenizer` gained
  `scanTemplateLiteral` (backtick `` ` `` template literals) to avoid
  misreading `${...}` interpolation content as real bracket depth.
  **Known gap, not yet fixed:** a JS/TS regex literal (e.g. `/[{]/`) is
  still tokenized as ordinary `TEXT`, not a string-like unit, so a
  bracket-family character inside one can still miscount depth — see
  `RDD_KEY_228` for detail.
- Out of scope entirely for this job: any change to data-format
  (JSON/YAML/etc.) or Python3 indentation logic, and HTML/XML's own
  element-nesting indentation (structurally indent-based already, not a
  brace-depth problem) — GDR as scoped here is the curly-brace-family
  (C/C++/Java/Kotlin, plus JS/TS per the expansion above)
  reindentation problem, matching where the old `STATE_COMMON.md` TODO
  lived before this split.
