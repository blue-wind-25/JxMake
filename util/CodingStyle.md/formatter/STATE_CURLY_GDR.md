# STATE_CURLY_GDR.md — General Scope-Depth Reindentation (curly reindent job)

Read `STATE_COMMON.md` first — shared commit workflow, ambiguity-handling
protocol, file-exclusion rules, and real-code-testing methodology. This
file holds only what is specific to this job.

---

## Purpose

Tracks the "General scope-depth reindentation" (GDR) job: reindent ordinary
body statements to an absolute target derived from structural (brace/scope)
depth, rather than preserving original whitespace except where a specific
recognized rewrite (brace placement, spacing, alignment) requires touching it.

**Overall status: pre-pass architecture landed and wired up behind
`curly-general-scope-reindent = on` (default off); real-code validation
against its originally-scoped corpora not yet done, and a real
pass-ordering bug was found during a first, differently-scoped real-code
test (`RDD_KEY_229`) — see the Checklist's last two items below.** Split
out of `STATE_COMMON.md`'s old "Architectural TODOs" section (risk-analysis
writeup only, no implementation) on 2026-08-02.

---

## Background: why this is its own dedicated job, not a quick fix

**Current state** (confirmed by direct testing, C++26 session): the
formatter does not reindent ordinary body statements from scratch —
original whitespace is preserved except for specific recognized rewrites.
Only `SwitchRule.applyNonInlineCaseIndent` and
`ScopePipeline.applyDeclarationsPass` reindent anything, and both apply one
**relative delta** from a single reference line, not an absolute target from
brace-nesting depth. `STATE_C_CPP_JAVA.md`'s "Known Gaps — Open" documents
two real bugs from this shape (`ASTParser.java` in `javaparser/javaparser`;
local `tool/JSONEncoderLite.java`) — non-idempotent reindentation on
internally-inconsistent source, both ACCEPTED-not-fixed: the real fix
(absolute target from structural depth) is nontrivial with real regression
risk for a narrow shape.

**Why a *general* version is much harder/riskier than those two narrow passes:**
- **Blast radius inversion.** Current invariant: don't touch indentation
  unless a specific construct requires it — every real-code bug found so
  far (~20+ external repos) has been narrow/isolated. A general pass makes
  every line in every file a candidate for a wrong result (currently
  ~1/2000 files in `javaparser`). (See "Proposed pre-pass architecture" —
  now scoped to only the `curly-general-scope-reindent = on` case, not the
  default-off path.)
- **Continuation vs. block depth is a second axis, not a free extension.**
  Brace/paren/bracket depth alone isn't enough — wrapped expressions,
  chained calls, multi-line initializers have continuation-indent
  conventions (STYLE.md §2) that don't reduce to "one level per `{`". Must
  merge two indent models without them fighting.
- **Content that must never be touched.** Raw string literals, block-comment
  interior lines, preprocessor directives (column-0 regardless of depth,
  own continuation rules), and `frozen` spans all need exclusion — each
  already a real bug source under narrower passes.
- **Ordering interacts with every other pass.** Brace-placement (Allman),
  line-wrapping (`enforceCallLineBreaking`), switch-case handling run at
  specific `FormatterCurly` phase points because their output affects what
  "correct" indentation is afterward (see
  `formatNonInlineSwitches`/`enforceCallLineBreaking` ordering bug, fixture
  `_56`). A general reindent pass needs to run after every line-count/brace
  decision is final; an ordering bug produces plausible-looking-wrong
  output, not a crash.

**If ever attempted (general cautions, still apply on top of the pre-pass
architecture below):**
- `make test`'s fixture corpus is a floor, not a substitute — fixtures were
  tuned under the current indentation-preserving model. Re-run real-code
  testing against at least `javaparser/javaparser`, local
  `tool/JSONEncoderLite.java`, `serge-sans-paille/frozen` (where existing
  indent bugs surfaced), plus a fresh untested large corpus (full-tree
  idempotency, not `--out DIR`) — neither open gap was caught by
  `make test` alone.
- Expect this to be the single riskiest change ever made to this
  formatter's overall system; budget accordingly, not as an incremental fix.

---

## In-file directive requirement (JXM_CFMT_GDR)

Needed once GDR is actually implemented, so mixed/inconsistent indentation
can be deliberately introduced and tested per-region (e.g. a nested `if`
block hand-indented at a shallower depth than its ancestors) without a
whole-file config flip.

**New, GDR-specific directive pair**, distinct from existing
`JXM_CFMT_CFG key=value` (see `STATE_COMMON.md`'s "In-file Config Support"
— `InFileConfig.parse`, top-of-file preamble, `//%`/`/*% ... */`
comment-based directives). GDR needs point-in-file 0/1 toggles, not a
single top-of-file key=value block, so it does not reuse `JXM_CFMT_CFG`'s
syntax or its single-preamble-only placement rule:

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

Build GDR as a separate pre-pass that runs BEFORE the source reaches the
existing formatter pipeline, entirely gated behind
`curly-general-scope-reindent = on` (default `off`, in `STATE_COMMON.md`'s
Config Keys and Defaults table). When on, the pre-pass runs first with its
own minimal tokenizer (not `TokenizerCore`/`TokenizerCurly`), its own
brace-depth counter (independent of `ScopePipelineCurly`'s), and its own
reindenter (absolute target from structural depth) — entirely independent
of existing `ScopePipelineCurly`/`FormatterCurly` machinery. Only after the
pre-pass does the (possibly rewritten) source proceed into the normal,
unmodified formatter pipeline exactly as today.

**Rationale / risk-profile change:** when off (default), the existing
formatter code path is completely untouched — zero blast radius on the
default-off path, which stays the entire ~20+ external-repo-validated
regression surface as-is. Changes risk from "modifies shared core pipeline
code" to "isolated additive pre-pass, only active opt-in".

**This does not make the pre-pass's own correctness free** — all hard
sub-problems in Background (continuation-vs-block depth, raw-string/
comment/preprocessor-directive/`frozen`-span exclusion, ordering entirely
before every other pass) still apply in full to the pre-pass. Architecture
change only removes regression risk to the *existing* pipeline when off.

---

## D3 fold (Kotlin dogfood cluster D3 → this job)

**As of 2026-08-02, Kotlin dogfood cluster D3 (multi-line-call/condition
wrap-decision flap) is folded into this job.** See `STATE_KOTLIN.md`'s D3
entries (RDD_KEY_221, RDD_KEY_226, the "2026-07-31 D3 scoping session", and
the "2026-08-01 D3 implementation attempt" section) for full investigation
history — not restated in full here, only summarized as this job's
motivating real-world case.

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

Both investigation sessions independently concluded that a real fix needs
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
instead of GDR's current single pre-pass-then-pipeline order: (1) GDR
pre-pass as today, (2) normal formatting pass as today, (3) GDR again, (4)
normal formatting pass again. This is a **concrete instantiation of one of
the two remediation options `RDD_KEY_229` already named but explicitly did
not attempt** ("iterate pipeline+GDR to a bounded fixpoint") — fixed 4
stages, not an open-ended loop-until-stable.

### Why this plausibly resolves the circular dependency

Stage 1 (GDR-1) computes depth from the *original* source's brace/paren
nesting — wrong for any line the pipeline is about to split or join (the
`RDD_KEY_229`/javaparser dominant-failure-mode case: a joined `} else if
(...) {` later Allman-split into two lines, where the newly split-out line
never got its own GDR target). Stage 2 (pipeline-1) makes brace-placement/
line-wrap decisions using GDR-1's already-mostly-correct indentation, so
fewer wrap-decision errors than plain post-pass ordering. Stage 3 (GDR-2)
runs on pipeline-1's finalized brace placement and line splits/joins, so
every line — including ones newly created by stage 2's Allman-splitting —
gets a correct absolute depth-based target from the *actual final*
structure. Stage 4 (pipeline-2) exists because stage 3's reindentation can
still change line widths enough to flip a wrap fits-check that stage 2
decided under stage-1's slightly-different widths (the exact
circular-dependency mechanism `RDD_KEY_229`'s post-pass-ordering experiment
found) — one more pipeline pass lets those decisions re-settle against
now-correct widths.

Because GDR only ever rewrites leading whitespace (never moves, splits, or
joins tokens/lines — confirmed by `GdrRewriter.rewrite`: "replaces each
touchable line's leading whitespace ... leaves the rest of the line
byte-for-byte untouched"), and the pipeline owns all structural reflow, the
two alternate cleanly without either undoing the other's *kind* of edit.

### Whether this achieves true idempotency (not just "closer")

This is a heuristic, not a proof. Assuming stage 4 reaches a stable
width/wrap-decision state, a *second* full 4-stage application should be a
no-op end to end: GDR is a pure function of current brace/paren structure,
so GDR-1 of round 2 recomputes the same targets stage 3 already wrote;
pipeline-1 of round 2 re-decides the same wraps stage 4 already settled on;
GDR-2/pipeline-2 of round 2 are then no-ops. Composes with `make test`'s
existing round1/round2 idempotency check — no special-casing needed.

**What is NOT proven:** whether 4 stages is *always* enough. Known failure
mode is a **first-order** effect (one missed target on a newly-split
line); residual risk is a **second-order** oscillation — a wrap decision
whose own flip (stage 4) changes width enough to still disagree with a
hypothetical stage-5 GDR pass. `RDD_KEY_229`'s own post-pass-ordering
experiment found real flapping of exactly this shape, so it's not purely
theoretical; whether it damps out after one extra round (this proposal) or
needs more can only be answered by re-running the same real-code corpora
`RDD_KEY_229` already exposed the bug against (`javaparser-core-generators`
13/43 non-idempotent files, `angular/angular` TS cluster-5) with multipass
wired up.

### Other open questions this proposal surfaces (not yet answered)

- **Interaction with the pipeline's OWN relative-delta reindenters**
  (`SwitchRule.applyNonInlineCaseIndent`, `ScopePipeline.
  applyDeclarationsPass`, STYLE.md §8's multi-line param-list/declaration
  continuation-indent renderer in `MiscRuleCurly`) — these run inside
  "normal formatting pass" (stages 2/4) using their own indent models, not
  GDR's absolute-depth-plus-paren-axis model directly. `GdrReindenter`'s
  Background section claims its continuation-vs-block axis matches
  STYLE.md §8's convention, but that was validated via smoke tests only,
  not against these passes' actual output shapes. Stage 3 (GDR-2) running
  on already-rendered switch-case bodies/wrapped declarations needs to
  *agree* with what those passes just wrote, or it will silently overwrite
  correctly-STYLE.md-compliant indentation on the second pass — a real,
  unverified risk.
- **Whether `curly-gs-reindent-multipass = on` while `curly-general-scope-
  reindent = off` is a no-op, a usage error, or silently ignored** — same
  category of question `RDD_KEY_227` already resolved for `JXM_CFMT_GDR`'s
  interaction with the master flag; needs the same explicit resolution
  before implementation, not an assumption.
- **Naming.** The suggested `curly-gs-reindent-multipass` uses a different
  abbreviation style than the existing `curly-general-scope-reindent` key
  it depends on (spelled out in full). Worth deciding at implementation
  time whether to match the existing key's full-word style (e.g.
  `curly-general-scope-reindent-multipass`) or keep the shorter form —
  flagged, not decided.
- **Cost.** Opt-in only, zero cost on the default-off path, but doubles
  wall-clock cost of the already-expensive GDR-pre-pass-plus-full-pipeline
  combination for any file that enables it — worth a one-line `README.md`
  mention once/if implemented, not a blocker.

### Verdict

**Plausible and worth prototyping** — it directly targets the confirmed
root cause (`RDD_KEY_229`) with a bounded, cheap-to-reason-about shape
(GdrRewriter's whitespace-only edits mean the two pass kinds can't
structurally fight each other), and composes with the existing idempotency
test with no special-casing. Not a proven fix (see "second-order
oscillation" above) and has at least one real unverified risk (the
relative-delta-reindenter interaction). **Per `RDD_KEY_229`'s own note**
("both remediation paths too risky to attempt this session... a future
session should ask before attempting either remediation path"), this
write-up is the "ask" — implementation should wait for an explicit
go-ahead, at which point it should be scoped as its own checklist item
here (new `RDD_KEY_n` once real design decisions are made, e.g. the
no-op/error question above), validated first via the same
`javaparser-core-generators`/`angular` cluster-5 files `RDD_KEY_229`
already has failure data for.

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

- `RDD_KEY_244` — Considered and rejected making `curly-general-scope-reindent`/
  `-multipass` `on` by default. Not just "not yet proven safe" — rejected on
  purpose-mismatch grounds too (GDR is a narrow fix-up tool for
  badly-indented/machine-generated source, not a general formatting rule;
  ordinary hand-written source has nothing for it to fix) and blast-radius
  grounds (default-on would invert this formatter's "never touch
  indentation unless a rule requires it" invariant project-wide). These
  keys are intended to remain permanently opt-in — not a deferred "flip the
  default later" task. Full text: `RDD_KEY_244` in `RDD_LOG.md`.
- `RDD_KEY_243` — Resolved the "How to fix the base single-pass `RDD_KEY_229` bug" Open
  Question: documentation-only resolution, not a code change. The already-shipped
  `curly-general-scope-reindent-multipass = on` is the fix/workaround (already empirically
  validated across every corpus this job tested against `RDD_KEY_229`'s failure mode); README.md
  restructured to state this plainly next to the gap's own explanation instead of leaving the two
  disconnected. Explicitly **not** option (A) as originally sketched (making the base
  single-flag path always iterate internally) — that remains rejected as redundant with the
  existing opt-in multipass key. Full text: `RDD_KEY_243` in `RDD_LOG.md`.
- `RDD_KEY_242` — **Fixed** the `GdrRewriter.spaces`
  `NegativeArraySizeException` crash flagged as a side-finding during the
  `RDD_KEY_240`/`RDD_KEY_241` session. Root cause confirmed by direct
  reconstruction (not assumed): `GdrReindenter.compute`'s per-axis brace/
  paren-bracket running depth goes negative only when the source is
  genuinely bracket-unbalanced (more closers than openers up to that
  point) — impossible for well-formed input, reproduced with a minimal
  Kotlin repro combining a one-true-brace `if`/`else` with a trailing-
  lambda fluent chain and one stray extra `}`. Fixed by clamping each of
  `braceLevel`/`pbLevel` to `>= 0` independently in `GdrReindenter.compute`
  before summing into `level` (no-op for well-formed input; malformed
  input now reindents an over-closed scope to column 0 instead of
  crashing), plus a defensive clamp in `GdrRewriter.spaces` itself. `make
  test`: 244/244 forward + idempotency, unaffected (no fixture — a
  malformed-input-only guard). Full text: `RDD_KEY_242` in `RDD_LOG.md`.
- `RDD_KEY_241` — **Fixed** `RDD_KEY_240`'s confirmed second-order-
  oscillation counterexample: `GdrPipelineGate.applyAndFormat`'s hardcoded
  4-call sequence replaced with an actual convergence loop (iterate GDR-pass
  + `formatOne` cycles, comparing each new cycle's output against the
  previous cycle's, stop on byte-identical match; `MAX_MULTIPASS_CYCLES =
  20` safety cap throws `IllegalStateException` if never reached). The
  RDD_KEY_240 TS/JS repro is now confirmed idempotent (round1 == round2).
  Base single-pre-pass path (multipass off or base flag off) unchanged;
  defaults remain `off`. Full text: `RDD_KEY_241` in `RDD_LOG.md`.
- `RDD_KEY_240` — Adversarial stress-testing (2026-08-05) of the bounded
  4-stage `curly-general-scope-reindent-multipass` loop **found a genuine,
  minimal counterexample to full idempotency**, confirming the
  "second-order oscillation" risk this file already flagged as unproven.
  **Fixed by `RDD_KEY_241`** (see above) — the hardcoded 4-stage bound was
  replaced with a real convergence loop.
  Minimal repro: a single-level (no extra nesting needed) TS/JS `if (...) {
  arr.filter(...).map(...).forEach(...); } else if (...) { ... }` with
  `curly-general-scope-reindent-multipass=on` — round1's output differs
  from round2 (reformatting round1's own output) on the wrapped `.map(...)`
  continuation's indent column, though round2 == round3 == round4 == round5
  (stabilizes only after one extra full formatter invocation, not within
  the single bounded-4-stage invocation the design assumed suffices). Also
  reproduces in Kotlin, but only at much greater nesting depth (30 levels)
  — did not reproduce in the equivalent Java shape at any depth tried. No
  source code changed (out of scope for this stress-testing task per
  explicit instruction — fixing the bounded-loop architecture is a real,
  separate follow-on). Full text: `RDD_KEY_240` in `RDD_LOG.md`.
- `RDD_KEY_227` — `JXM_CFMT_GDR 0`/`1` directive semantics: **flat toggle**
  (a single `1` always re-enables, redundant `0`s are no-ops, no nesting
  counter); an unmatched trailing `0` at EOF is **neither an error nor an
  implicit restore** — it's moot, since nothing remains to format past EOF
  and the next file starts fresh from its own config regardless; using the
  directive while `curly-general-scope-reindent` is globally `off` is a
  **silent no-op** (parses fine, lets a file be prepared for GDR ahead of a
  project-wide flag flip). Full text: `RDD_KEY_227` in `RDD_LOG.md`.
- `RDD_KEY_235` — Kotlin dogfood cluster D3 revisited: turning on
  `curly-general-scope-reindent`/`-multipass` does **NOT** resolve D3's
  wrap-decision flap (tested on the grounded repro and the real
  `EqualityAndComparisonCallsTransformer.kt` file) — negative result, no
  code changed. GDR only ever runs *between* whole pipeline passes and
  can't reach `MiscRuleCurly.renderCallCandidate`'s sibling-candidate
  volatility, which happens *inside* a single pipeline pass. A real D3 fix
  still needs a direct change to `renderCallCandidate`'s fits-check itself
  (consulting GDR's structural-depth infra as a library), not just
  flipping GDR on. Full text: `RDD_KEY_235` in `RDD_LOG.md`.
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

      **2026-08-03 follow-up (multipass-specific fixture):** added
      `test/curly_gdr_multipass_inp.java`/`_out.java` — a one-true-brace-
      style joined `} else if (...) {` / `} else {` chain with
      multi-statement bodies, `JXM_CFMT_CFG curly-general-scope-
      reindent=on; curly-general-scope-reindent-multipass=on` via in-file
      config. Minimal isolated repro of the confirmed `RDD_KEY_229` root
      cause: a single GDR pre-pass measures each line's depth against the
      joined source BEFORE brace-placement splits it into separate
      Allman-style lines, so a single GDR pass alone is non-idempotent on
      this exact shape (confirmed via `/tmp` dev harness: single-pass GDR
      round1 vs round2 differs on the `else`/`else if` lines' brace
      placement and indent); with both flags on, the 4-stage sequence
      resolves it cleanly. Expected output via `--standalone --in-place`,
      confirmed idempotent via `--standalone --check` (exit 0). Registered
      in `Makefile`'s `INP_FILES` right after `java_flush_left_inp.java`
      (before `html_js_flush_left_inp.html`) and in `test/README.txt`
      immediately after the `java_flush_left_inp/out.java` entry, under
      the same "General Scope-Depth Reindentation" heading, with wording
      distinguishing it from the base single-pass fixture and
      cross-referencing the real-code validation entries (angular,
      javaparser-core(-generators), JSONEncoderLite.java, frozen) that
      confirm the same fix at scale. `make test`: 238/238
      forward+idempotency, zero regressions.
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
- [x] Real-code test the pre-pass, `curly-general-scope-reindent = on`,
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
      (`javaparser-core-generators`, 43 files), `curly-general-scope-
      reindent=on`, full-tree idempotency. Result: 13 of 43 files
      non-idempotent. Inspected every failing diff — **all 13 are the same
      root cause as `RDD_KEY_229`**, not 13 distinct bugs: a closing
      `}`/`)` on a line the pipeline's own brace-placement/line-wrap passes
      later re-split or re-joined loses its GDR-computed indent target,
      landing at the pre-split line's depth instead of the post-split one
      (e.g. `GrammarLetterGenerator.java`'s `else {` block closer). Confirms
      `RDD_KEY_229`'s pass-ordering bug is not TS-specific — reproduces
      identically in plain Java under normal Allman-reflow/call-wrap
      activity, so it's the dominant (likely majority) failure mode across
      ordinary curly-family code, not a rare edge case. No independently-
      fixable bug found in this batch; every failure traces to the one
      already-deferred design issue, so per `RDD_KEY_229`'s note and
      `STATE_COMMON.md`'s ambiguity-handling protocol, **no source code was
      changed this session either**. `JSONEncoderLite.java` and
      `serge-sans-paille/frozen` not yet reached. **Left off here:** rest of
      `javaparser-core` main module, `JSONEncoderLite.java`, and `frozen`
      still untested — expect the same dominant failure mode. **Before
      spending more real-code-testing cycles on this corpus, the
      higher-leverage next step is revisiting `RDD_KEY_229`'s remediation
      options directly** (bounded fixpoint iteration; or feeding GDR's
      precomputed indent into `MiscRuleCurly.renderCallCandidate`'s wrap
      fits-check) — a real design decision with prior explicit user risk
      judgment against attempting it; future session should ask first.
      `/tmp/javaparser_gdr` and `/tmp/gdr_r1`/`/tmp/gdr_r2` left in place
      for reuse.
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
      are explicitly turned on. `STATE_COMMON.md`'s Config Keys table and
      `README.md`'s Configuration section both updated with the new key.

      **2026-08-03 session, smoke proof + real-code validation (PASS on
      both corpora RDD_KEY_229 already had failure data for):** used the
      real `RDD_KEY_229` failure cases themselves as proof the 4-stage path
      executes (`/tmp` harness only; no permanent fixture added that
      session).
      - **`angular/angular` TS cluster-5** (`/tmp/angular`, reused):
        `user_metric_spec.ts` and `i18n_parse.ts` — both confirmed
        non-idempotent under single-pass GDR (14 and 71 diff lines
        respectively between round1/round2, matching `RDD_KEY_229`) —
        **both become fully idempotent (zero-line round1/round2 diff) with
        `curly-general-scope-reindent-multipass=on`.** `emit.ts` (already
        passed under single-pass) stays idempotent under multipass — no
        new regression. All three multipass outputs pass
        `tools/verifiers/js_ts_syntax_check.sh` (exit 0).
      - **`javaparser-core-generators`** (`/tmp/javaparser_gdr`, reused,
        all 43 files, `--preserve-tree --root`): single-pass GDR
        reproduced original 13/43 non-idempotent; **multipass drops this
        to 0/43** (`diff -rq` round1 vs round2 empty). All 43 multipass
        outputs pass `tools/verifiers/java_syntax_check.sh` (exit 0).
      - **Result: PASS on every case `RDD_KEY_229` had failure data for.**
        Real evidence the bounded 4-stage design resolves the confirmed
        root cause on the actual corpora that exposed it. **What this does
        NOT prove:** the "second-order oscillation" risk (whether 4 stages
        is *always* enough) — evidence against it manifesting in practice
        so far, not proof it can never occur. Rest of `javaparser-core`,
        `tool/JSONEncoderLite.java`, and `serge-sans-paille/frozen` not run
        this sub-session. `make test`: 237/237 forward + idempotency
        (unaffected, `/tmp`-only).

      **2026-08-03 session (continued), remaining three originally-scoped
      corpora — all PASS under multipass:**
      - **`tool/JSONEncoderLite.java`**
        (`/home/aloysius/Projects/JxMake/src/jxm/tool/JSONEncoderLite.java`,
        one of two files `STATE_C_CPP_JAVA.md`'s "Known Gaps" cites as
        accepted-not-fixed relative-delta-reindent bug): single-pass
        `curly-general-scope-reindent=on` non-idempotent (112 diff lines
        round1/round2). **`curly-general-scope-reindent-multipass=on`
        fully fixes — zero-line round1/round2 diff.** Multipass output
        passes `tools/verifiers/java_syntax_check.sh` (exit 0). **PASS.**
      - **`serge-sans-paille/frozen`** (fresh clone `/tmp/frozen`, all 20
        `.hpp`/`.h` under `include/`, `--preserve-tree --root`):
        single-pass non-idempotent on **7 of 20 files**;
        **`curly-general-scope-reindent-multipass=on` → 0 of 20**. No
        C/C++ syntax-check wrapper in `tools/verifiers` (list only covers
        Java/Kotlin/JSON/JSON5/CSS/YAML/TOML/XML/HTML/JS-TS/Python), so
        used direct `g++ -std=c++17 -fsyntax-only` per file per
        `STATE_COMMON.md`'s "appropriate toolchain" methodology. Baseline
        (unmodified originals): 7 of 20 headers (`map.h`, `set.h`,
        `algorithm.h`, `string.h`, `random.h`, `unordered_set.h`,
        `unordered_map.h`) each have exactly 1 pre-existing syntax-only
        compile error (headers depend on other headers/template context
        not visible in single-TU syntax-only check — baseline limitation,
        not a real bug); other 13 compile clean. Compared error count per
        file baseline vs multipass round1 for **all 20 files**: every
        file's error count matches baseline exactly (7 still show exactly
        1 error each, same error text; 13 clean stay clean) — **zero new,
        formatter-induced errors**. **PASS.**
      - **`javaparser/javaparser` main `javaparser-core` module** (reused
        `/tmp/javaparser_gdr`, 576 `.java` files, large main module
        earlier `javaparser-core-generators` session deferred):
        single-pass non-idempotent on **93 of 576 files** (`diff -rq`
        round1 vs round2); **`curly-general-scope-reindent-multipass=on`
        → 0 of 576** (`diff -rq` empty). All 576 multipass outputs pass
        `tools/verifiers/java_syntax_check.sh` (batched single invocation
        per "invoke once per batch" convention — exit 0, all 576 "OK: no
        syntax errors"). **PASS.**

      **All three remaining originally-scoped real-code corpora for this
      checklist item are now done: `tool/JSONEncoderLite.java` (PASS),
      `serge-sans-paille/frozen` (PASS, 7/20 → 0/20), `javaparser-core`
      (PASS, 93/576 → 0/576) — combined with earlier `javaparser-core-
      generators` (13/43 → 0/43) and `angular/angular` TS cluster-5 (2/3
      files fixed, 1/3 already passing), `curly-general-scope-reindent-
      multipass=on` has now resolved every confirmed `RDD_KEY_229`-shape
      non-idempotency across every corpus this job has tested it against,
      zero newly-introduced syntax/compile errors in any of them.**
      Still-open, explicitly-flagged caveat remains: this is evidence the
      "second-order oscillation" risk hasn't manifested on any tested
      input, not proof it structurally cannot on some other input. `make
      test` throughout: 237/237 forward + idempotency (unaffected until
      multipass fixture added; see fixture item for 238/238).
- [~] Revisit Kotlin dogfood cluster D3 (see "D3 fold" section above) using
      the pre-pass's statement-boundary/structural-depth infrastructure;
      land a real fix in `MiscRuleCurly`/wherever the fix ends up living,
      record a new `RDD_KEY_n`, update `STATE_KOTLIN.md`'s D3 entries to
      point at it.

      **2026-08-03 session, negative result — tested the cheap hypothesis
      first, real fix still not attempted.** Per the coordinator's explicit
      request, tested whether simply turning on
      `curly-general-scope-reindent` (alone, then with
      `curly-general-scope-reindent-multipass` too) resolves D3 as a side
      effect of the just-landed multipass work, before attempting any new
      `MiscRuleCurly` code. Reused `/tmp/d3_test.kt` (the grounded repro
      from the original D3 sessions) and the real
      `EqualityAndComparisonCallsTransformer.kt` file (existing
      `/tmp/kotlin-master` clone). **Result: does NOT resolve — both files
      stay non-idempotent under all three configurations (GDR off, GDR-on
      single-pass, GDR-on multipass).** Under GDR off / single-pass the
      flap is the original full wrap/unwrap shape; under multipass the
      symptom narrows to a pure indentation-column drift on the same
      already-wrapped line (still non-idempotent, just a smaller diff) —
      notable but not a fix. Root cause of *why* GDR can't reach this:
      `MiscRuleCurly.renderCallCandidate`'s fits-check volatility
      (`RDD_KEY_221`) is driven by a sibling wrap candidate's effect on the
      physical line *within a single pipeline pass*; GDR only ever runs
      *between* whole pipeline passes (even doubled via multipass) and has
      no visibility into a decision made mid-pass against sibling
      candidates. Full writeup: `RDD_KEY_235`. **This confirms the "D3
      fold" section's original conclusion still holds — the only real fix
      path is a direct change to `renderCallCandidate`'s own fits-check
      (consulting GDR's structural-depth infra as a library, replacing the
      volatile `lineStartIndex` anchor), not achievable by toggling GDR on
      around the existing pipeline.** That implementation is a real
      design/code decision (two prior narrow-patch attempts,
      `RDD_KEY_221`/`RDD_KEY_226`, already regressed other fixtures when
      tried without this infra) — per `STATE_COMMON.md`'s ambiguity-
      handling protocol, deferred for an explicit go-ahead before
      attempting rather than started unilaterally this session. No source
      code changed. `make test` unaffected (237/237, no fixture/source
      changes this step).

- [x] **Adversarial stress-test of the bounded 4-stage multipass loop for
      the unproven "second-order oscillation" risk** (2026-08-05,
      dedicated hardening/validation task — not adding new GDR
      functionality, not flipping any default). Goal: hunt for a genuine
      counterexample where 4 stages isn't enough, using synthetic
      adversarial constructions shaped specifically to stress the
      mechanism, not just more real-world code (the corpora already tested
      — `javaparser-core(-generators)`, `angular` TS cluster-5,
      `JSONEncoderLite.java`, `serge-sans-paille/frozen` — all cleared with
      zero non-idempotency; this session deliberately targeted new shapes).

      **Mechanism confirmed first (`GdrPipelineGate.applyAndFormat`,
      `src/com/jxmake/formatter/gdr/GdrPipelineGate.java`):** the "4
      stages" is a hardcoded, unconditional 4-call sequence — `apply`
      (GDR) → `formatOne` (pipeline) → `apply` (GDR) → `formatOne`
      (pipeline), always exactly these 4 calls when both flags are on.
      **Not** a "stop when stable, else iterate up to N times" convergence
      loop — there is no comparison between any two stages' output, no
      iteration count beyond exactly 4, and no warning/error path at all if
      the sequence hasn't actually converged by the 4th call. This answers
      the task's step-1 question directly: the design cannot self-detect
      non-convergence, because it never checks for convergence in the
      first place.

      **Adversarial constructions tried** (all via
      `/*% JXM_CFMT_CFG curly-general-scope-reindent=on;
      curly-general-scope-reindent-multipass=on */`, syntax-validated
      before formatting, round1→round2→round3→round4→round5 reformatted
      and diffed at each step):
      - Deeply nested (10/20/30/50-level) one-true-brace `} else if (...)
        {` chains and real nested (not just chained) if-blocks, Java.
      - Dense combinations of one-true-brace joins + chained fluent calls
        (`.withA().withB()...`) + multi-line lambda wraps, all interleaved
        within the same nested structure, Java, at the same depths.
      - Deeply nested switch-in-if (stresses interaction with
        `SwitchRule.applyNonInlineCaseIndent`'s own relative-delta
        reindenter, an explicitly-flagged open question in this file's
        "Open design proposal" section), Java.
      - Wrapped ternary chains nested inside deep one-true-brace ifs, Java.
      - Randomized/garbage leading-whitespace input (random space/tab
        padding per line, no consistent original indentation to anchor
        anything), Java.
      - Deep nested templates/namespaces with a C++11 raw string literal,
        C++.
      - Deep nested one-true-brace `if`/`else if` combined with fluent
        `.filter().map().forEach()` arrow chains, template-literal
        interpolation, and nested arrow-lambda bodies, TS.
      - Deep nested one-true-brace `if`/`else if` combined with trailing-
        lambda chains (`.filter{}.map{}.forEach{}`), Kotlin.

      **Result: most constructions stayed fully idempotent across all 5
      rounds** — deep one-true-brace nesting/dense-combo/switch-nest/
      ternary-chain/garbage-indent (Java) and the C++ deep-template case
      found no bug at any depth tried (10/20/30/50).

      **But a genuine, minimal counterexample WAS found in TS/JS/Kotlin —
      see `RDD_KEY_240` for full detail, summarized here:** a single-level
      TS/JS `if (...) { arr.filter(x => x > 0).map(x => x*2).forEach(x =>
      {...}); } else if (...) { ... }` (depth 1, no extra nesting needed)
      with multipass on: round1's output differs from round2 (reformatting
      round1's own output) on the wrapped `.map(...)` continuation's indent
      column — a real idempotency failure from a single top-level
      invocation. round2 == round3 == round4 == round5 (confirmed stable
      after that point), so the true fixed point exists and is reached,
      just not within the one bounded-4-stage invocation the design
      assumed would suffice — it takes a second full formatter invocation
      (effectively 8 internal stages across two `applyAndFormat` calls) to
      settle. This **directly falsifies** this file's own "Open design
      proposal" section's claim under "Whether this achieves true
      idempotency" that "a *second* full 4-stage application should be a
      no-op end to end" — demonstrated NOT a no-op for this shape. Root
      cause is the same circular dependency `RDD_KEY_229` already
      diagnosed (GDR's reindentation changes a line's width, which can
      flip the pipeline's own wrap/continuation-indent fits-check), just
      manifesting one level deeper than the 4-stage bound accounts for.
      Reproduces in plain JS and TS at depth 1; reproduces in Kotlin too
      but needed much deeper nesting (30 levels of nested trailing-lambda
      `if`/`else if` chains) before the same one-level indent-column-drift
      pattern appeared; did **not** reproduce in the equivalent Java shape
      (`list.stream().filter().map().forEach()` with lambdas) at any depth
      tried.

      **No source code changed** — per this task's explicit constraint and
      `STATE_COMMON.md`'s ambiguity-handling protocol, a fix to the
      bounded-loop architecture itself (e.g. an actual iterate-until-stable
      loop, or feeding stage 3's width into stage 4's fits-check) is a
      real, separate follow-on task requiring its own design/go-ahead, not
      attempted here. `curly-general-scope-reindent`/
      `curly-general-scope-reindent-multipass` remain `off` by default,
      unchanged. `make test`: 244/244 forward + idempotency, unaffected —
      all adversarial work was scratchpad/`/tmp`-only, no fixture added (a
      fixture demonstrating this bug would need to encode a currently-known
      failure as expected output, which isn't right for a documented,
      not-yet-fixed gap — matching how `RDD_KEY_229`'s own finding was
      handled, state-file documentation only).

      **Honest confidence assessment:** this is now **evidence the 4-stage
      bound is insufficient for at least one real (if narrow/synthetic)
      shape**, not just "unproven" — the risk this file flagged as
      theoretical has now manifested concretely, in TS/JS at trivial depth
      and in Kotlin at real depth. It does NOT mean every real-world file
      is at risk: every real-code corpus this job has tested so far
      (700+ files, Java/C++/TS) still cleared cleanly, and the specific
      shape needed (a wrapped fluent/arrow chain immediately inside a
      one-true-brace-joined `if`/`else if`) is a fairly specific
      combination. What is now known: (1) the 4-stage bound is not a
      structural guarantee, confirmed by direct counterexample, not just
      argued from first principles; (2) at least for the cases found here,
      the oscillation is bounded and damps out after one additional full
      reformat (round2 was already stable) — nothing found in this session
      oscillates indefinitely or fails to converge at all; (3) this was not
      an exhaustive search — deeper/wider adversarial constructions in
      C/C++ (no minimal repro found there yet), other JS/TS shapes beyond
      fluent-chain wraps, and combinations with switch-case/ternary-wrap in
      TS/Kotlin specifically (only tried in Java) remain untried and could
      still surface further or worse cases.

- [x] **Fix `RDD_KEY_240`'s confirmed second-order-oscillation
      counterexample** (2026-08-05, follow-on to the adversarial
      stress-testing item above). `GdrPipelineGate.applyAndFormat`'s
      hardcoded, unconditional 4-call sequence (GDR, formatOne, GDR,
      formatOne) — which never compared any two stages' output — replaced
      with a real convergence loop: repeats the GDR-pass + `formatOne`
      cycle, comparing each new cycle's `formatOne` output against the
      immediately preceding cycle's, stopping as soon as two consecutive
      cycles are byte-identical (`String.equals`). New
      `MAX_MULTIPASS_CYCLES = 20` safety cap (~5x the ~4 cycles the
      confirmed `RDD_KEY_240` counterexample needed) throws
      `IllegalStateException` (file path + pointer to `RDD_KEY_241`) if
      reached without convergence, rather than silently returning a
      possibly-still-oscillating result. Base single-pre-pass path
      (multipass off, or base flag off) is byte-for-byte unchanged — still
      exactly one GDR call plus one `formatOne` call. `make test`:
      244/244 forward + idempotency throughout, unaffected (no fixture
      added — matches how `RDD_KEY_229`/`RDD_KEY_240` were documented,
      change only reachable via the explicitly-off-by-default multipass
      path). Re-ran the exact minimal `RDD_KEY_240` TS/JS repro (`if (...)
      { arr.filter().map().forEach(); } else if (...) { ... }` with both
      flags on via in-file config): confirmed fixed — round1 == round2
      (byte-identical, verified via `--standalone --in-place` + `diff`),
      previously round1 != round2. `curly-general-scope-reindent`/
      `curly-general-scope-reindent-multipass` remain `off` by default,
      unchanged in `Config.java`. Full text: `RDD_KEY_241` in
      `RDD_LOG.md`.

      **Separately investigated during this session, found to be an
      unrelated pre-existing bug — since fixed, see `RDD_KEY_242` below:**
      while constructing a deep-nesting Kotlin adversarial repro to
      exercise this same fix, `GdrRewriter.rewrite` /
      `GdrRewriter.spaces` threw `NegativeArraySizeException` on certain
      malformed (bracket-unbalanced) one-true-brace `} else if (...) {`
      Kotlin input combined with a trailing-lambda fluent chain
      (`listOf(...).filter { }.map { }.forEach { }`), reproducing
      identically under `curly-general-scope-reindent` alone (multipass
      off) — confirmed via a side-by-side build of the pre-`RDD_KEY_241`
      source, so this crash predates and was unaffected by this session's
      convergence-loop change.

- [x] **Fix the base single-pass `curly-general-scope-reindent` ordering
      bug** (`RDD_KEY_229`) directly, independent of the opt-in multipass
      workaround. Investigated 2026-08-05 while fixing
      `RDD_KEY_240`/`RDD_KEY_241`: the "single-pass bug" is unambiguously
      `RDD_KEY_229` — GDR's pre-pass computes each line's structural-depth
      indent target from the source's brace/paren nesting *before* the
      pipeline's brace-placement pass runs, so a line using joined
      one-true-brace style (`} else if (...) {`) that the pipeline later
      splits into separate Allman-style lines never gets a correct GDR
      target for the newly split-out line — confirmed non-idempotent on
      13/43 `javaparser-core-generators` files, 93/576 `javaparser-core`
      files, 7/20 `serge-sans-paille/frozen` files, 2/3 `angular` TS
      cluster-5 files (multipass is layered on top, not a fix to this base
      path — with multipass off, the base path still has this exact bug,
      unchanged). **Resolved 2026-08-06 as a documentation-only fix
      (`RDD_KEY_243`, coordinator go-ahead):** rather than either of
      `RDD_KEY_229`'s two named remediation options, the resolution is to
      document that the already-shipped, already-validated
      `curly-general-scope-reindent-multipass = on` is the fix for this gap
      — no source code changed. See `RDD_KEY_243` above and `README.md`'s
      "General scope-depth reindentation (GDR)" subsection.

Do the above checklist one by one. Test, commit, and ask me whether to continue or pause.

## Open Questions

None currently open. (The prior "how to fix the base single-pass `RDD_KEY_229`
bug" question was resolved 2026-08-06 as a documentation-only fix — see
`RDD_KEY_243` in Resolved Design Decisions above. Option (B), a direct change
to `MiscRuleCurly`'s wrap fits-check, remains un-attempted and would need its
own explicit go-ahead if ever revisited.)

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
- **`curly-general-scope-reindent`/`-multipass` are permanently opt-in by
  design, not "opt-in until proven safe enough to default on."** See
  `RDD_KEY_244` in Resolved Design Decisions above — considered and
  rejected on both purpose-mismatch grounds (a narrow fix-up tool has
  nothing to offer ordinary already-consistently-indented source) and
  blast-radius grounds (default-on would invert this formatter's
  touch-nothing-unless-required invariant project-wide), independent of
  whether the multipass second-order-oscillation risk is ever fully
  resolved. Do not track "flip the default" as a future goal for this job.
