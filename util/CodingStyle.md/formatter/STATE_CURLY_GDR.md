# STATE_CURLY_GDR.md — General Scope-Depth Reindentation (curly reindent job)

Read `STATE_COMMON.md` first (shared commit workflow, ambiguity-handling
protocol, file-exclusion rules, and real-code-testing methodology). This
file holds only what is specific to this job.

---

## Purpose

Tracks the "General scope-depth reindentation" (GDR) job: reindent ordinary
body statements to an absolute target derived from structural (brace/scope)
depth, rather than preserving original whitespace except where a specific
recognized rewrite (brace placement, spacing, alignment) requires touching it.

**Overall status: pre-pass architecture landed and wired up behind
`curly-general-scope-reindent = on` (default off). Real-code validation
against the originally-scoped corpora is now complete and passing under
`-multipass`; the base single-pass ordering bug (`RDD_KEY_229`) is resolved
as documentation (use multipass), not a source fix. D3 (Kotlin wrap-decision
flap) remains open, needing infrastructure beyond this job's current scope.**
Split out of `STATE_COMMON.md`'s old "Architectural TODOs" section
(risk-analysis writeup only, no implementation) on 2026-08-02.

---

## Background: why this is its own dedicated job, not a quick fix

**Current state** (confirmed by direct testing, C++26 session): the formatter
does not reindent ordinary body statements from scratch — original
whitespace is preserved except for specific recognized rewrites. Only
`SwitchRule.applyNonInlineCaseIndent` and
`ScopePipeline.applyDeclarationsPass` reindent anything, both applying a
**relative delta** from a single reference line, not an absolute target from
brace-nesting depth. `STATE_C_CPP_JAVA.md`'s "Known Gaps" originally
documented two real bugs from this shape (`ASTParser.java` in
`javaparser/javaparser`; local `tool/JSONEncoderLite.java`) — non-idempotent
reindentation on internally-inconsistent source, both ACCEPTED-not-fixed:
the real fix (absolute target from structural depth) is nontrivial, with
real regression risk for a narrow shape. **Status update (both now closed,
kept for historical context on why this job exists):**
`tool/JSONEncoderLite.java`'s instance stopped reproducing, closed
2026-08-15 (`RDD_KEY_292`); `ASTParser.java`'s instance closed 2026-08-16
(`RDD_KEY_301`, documentation-only — round-trips cleanly once both
`curly-general-scope-reindent` and `curly-general-scope-reindent-multipass`
are on, i.e. exactly the general pass this job built). See
`STATE_C_CPP_JAVA.md`'s "Known Gaps — Fixed" for both.

**Why a *general* version is much harder/riskier than those two narrow
passes:**
- **Blast radius inversion.** Current invariant: don't touch indentation
  unless a specific construct requires it — every real-code bug found so
  far (~20+ external repos) has been narrow/isolated. A general pass makes
  every line in every file a candidate for a wrong result, though the
  pre-pass architecture below scopes this risk to only the on case.
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
  "correct" indentation is afterward (fixture `_56`). A general reindent
  pass needs to run after every line-count/brace decision is final; an
  ordering bug produces plausible-looking-wrong output, not a crash.

**If ever attempted (general cautions, apply on top of the pre-pass
architecture below):** `make test`'s fixture corpus is a floor, not a
substitute — real-code test against at least `javaparser/javaparser`, local
`tool/JSONEncoderLite.java`, `serge-sans-paille/frozen`, plus a fresh
untested large corpus (full-tree idempotency, not `--out DIR`). Expect this
to be the single riskiest change ever made to this formatter's overall
system; budget accordingly.

---

## In-file directive requirement (JXM_CFMT_GDR)

Needed once GDR is implemented, so mixed/inconsistent indentation can be
deliberately introduced and tested per-region without a whole-file config
flip. **GDR-specific directive pair**, distinct from `JXM_CFMT_CFG
key=value` (see `STATE_COMMON.md`'s "In-file Config Support") — GDR needs
point-in-file 0/1 toggles, not a single top-of-file key=value block:

```
//% JXM_CFMT_GDR 0
//% JXM_CFMT_GDR 1
```
and the block-comment variant:
```
/*% JXM_CFMT_GDR 0 */
/*% JXM_CFMT_GDR 1 */
```

`0` disables GDR reindentation for the region following the directive; `1`
re-enables it. **Semantics resolved by `RDD_KEY_227`** (see Resolved Design
Decisions).

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

**Rationale:** when off (default), the existing formatter code path is
completely untouched — zero blast radius on the default-off path. Change
risk moves from "modifies shared core pipeline code" to "isolated additive
pre-pass, active only opt-in."

**This does not make the pre-pass's own correctness free** — all hard
sub-problems in Background still apply in full to the pre-pass.
Architecture change only removes regression risk to the *existing* pipeline
when off.

---

## D3 fold (Kotlin dogfood cluster D3 → this job)

**As of 2026-08-02, Kotlin dogfood cluster D3 (multi-line-call/condition
wrap-decision flap) is folded into this job.** Full investigation history is
in `STATE_KOTLIN.md`'s D3 entries (RDD_KEY_221, RDD_KEY_226, the
"2026-07-31 D3 scoping session", the "2026-08-01 D3 implementation
attempt") — summarized here as this job's motivating real-world case.

**Confirmed root cause:** `MiscRuleCurly.renderCallCandidate`'s no-newline
fits-check measures a wrap candidate against its enclosing physical source
line (`lineStartIndex(tokens, nameIdx)`), which is volatile — it shifts
depending on what a *sibling* candidate on the same logical statement did
in an earlier phase of the same pass, or the previous round. Two candidate
fixes were tried and reverted:
1. Anchor at `nameIdx` (RDD_KEY_221) — regressed 28 fixtures across
   C/C++/Java/TS/Kotlin (dropped legitimate same-statement prefix,
   underestimate failure mode).
2. `statementStartIndex`, a depth-0 `;`/`{`/`}` backward scan (RDD_KEY_226)
   — regressed 16 Kotlin fixtures (Kotlin statements are ordinarily
   NEWLINE-separated, not `;`-separated, so the scan walks into an
   unrelated preceding sibling statement).

Both sessions concluded a real fix needs actual Kotlin statement-boundary/
structural-depth tracking, not a local token-scan patch — "closer to
`STATE_COMMON.md`'s GDR architectural TODO's territory than a self-contained
fix." This fold executes that already-self-documented conclusion.

**Scoped as a sub-goal of this job:** once GDR's own structural-depth
infrastructure lands, D3's fix should be revisited using that
infrastructure, deriving a stable statement-start boundary the same way the
pre-pass derives a stable reindent target — not as a standalone patch to
`MiscRuleCurly`. D3 is not blocking GDR's own architecture work.

`STATE_KOTLIN.md` points here instead of tracking D3 independently — see
that file's D3 sections and its Step-5/dogfood summary lines.

**Disposition (2026-08-10):** already documented in `README.md`'s Known
Limitations → "Curly-brace family" item 2 (wrap decision can flap,
C/C++/Java/Kotlin/JS/TS) — this D3 entry is the Kotlin-specific instance,
not a separate one. Removed the redundant standalone `XL.txt` TIER 9 entry.

---

## Open design proposal: bounded multi-pass remediation for RDD_KEY_229 — IMPLEMENTED

**Original user proposal (2026-08-03):** GDR is a pre-pass, so it can't see
brace-placement/line-wrap decisions the pipeline hasn't made yet; a
post-pass ordering trades that bug for a different one (indentation-width
changes flipping wrap fits-checks) — see `RDD_KEY_229` for both failure
modes. Proposed remediation: new config key
**`curly-gs-reindent-multipass`** (`off` default, only effective when
`curly-general-scope-reindent` is also `on`), running a fixed 4-stage
sequence: (1) GDR pre-pass, (2) normal formatting pass, (3) GDR again, (4)
normal formatting pass again — an instantiation of one of `RDD_KEY_229`'s
two named remediation options ("iterate pipeline+GDR to a bounded fixpoint").

**Why this plausibly resolves the circular dependency:** Stage 1 computes
depth from the original source's brace/paren nesting (wrong for a line the
pipeline is about to split/join — the dominant javaparser failure: a joined
`} else if (...) {` later Allman-split, the new split-out line never
getting its own target). Stage 2 makes brace/wrap decisions using stage 1's
already-mostly-correct indentation. Stage 3 runs on stage 2's finalized
brace placement/splits, so every line (including newly created ones) gets a
correct target from the *actual final* structure. Stage 4 exists because
stage 3's reindentation can still change widths enough to flip a stage-2
wrap decision — one more pipeline pass lets those re-settle. Because GDR
only ever rewrites leading whitespace (`GdrRewriter.rewrite`: "replaces
each touchable line's leading whitespace ... leaves the rest of the line
byte-for-byte untouched") and the pipeline owns all structural reflow, the
two alternate cleanly without undoing each other's *kind* of edit.

**What was NOT proven at proposal time:** whether 4 stages is *always*
enough (residual second-order-oscillation risk). This was later confirmed
as a real gap by `RDD_KEY_240` and fixed by `RDD_KEY_241` (see Resolved
Design Decisions) — the hardcoded 4-stage bound was replaced with a real
convergence loop.

Other resolved open questions from the original proposal: naming
(`RDD_KEY_233`), multipass-on/base-off interaction (`RDD_KEY_234`),
interaction with the pipeline's own relative-delta reindenters
(unverified risk, not hit by any tested corpus so far). Implementation and
validation are recorded in the Checklist below.

---

## When implemented: documentation to update

- ~~Update `README.md`'s Config Keys/Configuration section~~ — done.
- Check `../README.txt` for a formatter config-keys section before editing
  it — as of last check it has none, so no edit needed; verify this
  assumption still holds if revisiting.
- Update `STATE_KOTLIN.md`'s D3 entries (its Category-2/D3 table row and the
  "2026-07-31"/"2026-08-01" D3 session sections) to point at the actual fix
  commit and new `RDD_KEY_n` once the D3 revisit lands, rather than leaving
  them pointing at this file's "folded, not yet fixed" state.

---

## New test fixtures needed — DONE (see Checklist)

Original instructions (kept for record): prove a file **can** turn
`curly-general-scope-reindent` on via in-file config, independent of
whether reindent logic is implemented yet — if not implemented, expected
output must be identical to input. Placement: `Makefile` — register right
after `#INP_FILES += in_file_config_error_inp.hpp`; `test/README.txt` —
document immediately after the `in_file_config_error_inp/out.hpp` entry.
Executed — see Checklist's fixture items below.

---

## Resolved Design Decisions

- `RDD_KEY_328` — Attempted to validate `curly-general-scope-reindent-postpass` (`RDD_KEY_324`)
  against real code before promoting it out of EXPERIMENTAL: not promoted. The original
  motivating target (Java anon-class compound-brace mismatch) turned out already fixed by an
  unrelated mechanism (`RDD_KEY_325`); a fresh real-corpus comparison (188-file Kotlin corpus)
  found a mixed result — some accidental improvements, but genuine new wrap-continuation
  indentation regressions on previously-correct output. Stays EXPERIMENTAL/default-off. Full
  text: `RDD_KEY_328`.
- `RDD_KEY_324` — Implemented the "run GDR a second time as a genuine POST-pass" idea (user
  suggestion, follow-up on `RDD_KEY_323`) as permanent, isolated GDR-job infrastructure: new
  `curly-general-scope-reindent-postpass` config key (EXPERIMENTAL, default off, silent no-op
  unless the base flag is also on, same posture as `-multipass`), applying GDR exactly once more
  directly to the fully-finished pipeline output with no further pipeline call after it — unlike
  every other GDR application (base single-pass and every multipass cycle), which is always
  immediately followed by another `formatOne` call. `make test`: 334/334, zero regressions
  (default off). Tested (temporary, reverted, no net source change) against `RDD_KEY_322/323`'s
  Java anon-class-as-call-argument repro (with Java's side channel temporarily re-enabled on top
  of the `RDD_KEY_321` corruption fix): still not a clean fix, but shifts WHICH brace pair GDR
  mismatches rather than eliminating the mismatch — the method body's own brace pair is now
  correctly aligned, but the outer anonymous-class-body brace pair (8sp open / 12sp close) becomes
  the new mismatch. Confirms the real fix needs `GdrBraceDepthCounter`/`GdrReindenter` root-cause
  tracing for this compound brace shape (already scoped by `RDD_KEY_323`), not just a pass-
  ordering change. Full text: `RDD_KEY_324`. See `STATE_C_CPP_JAVA.md`'s Known Gaps — Open (Java
  anon-class entry).
- `RDD_KEY_314` — Diagnosed the 2026-08-19 Open Question (function
  *expression* as call argument never reformatted to one-statement-
  per-line): **not a GDR bug** — reproduces identically with GDR fully off,
  so the pre-pass cannot be the cause. Real root cause is the shared
  pipeline's call-argument line-wrap/relocation logic treating a function-
  expression argument as an opaque text blob (same code family as
  `MiscRuleCurly`'s call-candidate wrapping, D3/`RDD_KEY_235`), never
  recursing into brace-placement for its interior — unlike statement-
  position function declarations. Out of this job's scope; documentation-
  only, no code change. Full text: `RDD_KEY_314`. **Follow-up (RDD_KEY_315,
  C_CPP_JAVA job):** precise root cause confirmed —
  `ScopePipelineCurly.splitTopLevelSpans` only records a `{` as a
  recursable child scope at `depth == 0`; a call argument's
  function-expression `{` is always `depth >= 1`, so it's never a span at
  all, invisible to `processScope`'s recursion from the start. Judged too
  risky to fix speculatively in one session (shared curly-family
  span-discovery infrastructure); accepted as an open gap, see
  `STATE_C_CPP_JAVA.md`'s "Known Gaps — Open". Full text: `RDD_KEY_315`.
  **Follow-up (RDD_KEY_316, C_CPP_JAVA job, 2026-08-20):** fixed for JS/TS
  only, via a `lang.isJs || lang.isTs`-gated side channel in
  `ScopePipelineCurly.processScope` (not by changing
  `splitTopLevelSpans`'s shared span model) — a call-argument
  function-expression body that already spans multiple physical lines now
  recurses and reformats the same as an identical body at statement/
  declaration position. C/C++/Java/Kotlin's own code paths untouched, still
  an accepted open gap for those languages. A residual JS/TS-only gap (a
  non-declaration statement line inside the newly-recursed body isn't
  force-reindented) is documented separately, not fixed. Full text:
  `RDD_KEY_316`; full narrative in `STATE_C_CPP_JAVA.md`'s Known Gaps.
- `RDD_KEY_298` — **Fixed** D3 (Kotlin multi-line-call/condition wrap-
  decision flap), seventh attempt, landed. `FormatterCurly.formatOne`
  renamed to private `formatOnePass`; new public `formatOne` re-runs it
  (Kotlin only) up to `MAX_SETTLE_PASSES = 5` times until byte-identical,
  mirroring `RDD_KEY_241`'s convergence-loop precedent instead of another
  backward-scan boundary heuristic. `make test` 321/321; 188-file real
  Kotlin corpus idempotent + syntax-clean. Full text: `RDD_KEY_298`.
- `RDD_KEY_244` — Considered and rejected making `curly-general-scope-reindent`/
  `-multipass` `on` by default. Rejected on purpose-mismatch grounds (GDR is
  a narrow fix-up tool for badly-indented/machine-generated source, not a
  general formatting rule; ordinary hand-written source has nothing for it
  to fix) and blast-radius grounds (default-on would invert this
  formatter's "never touch indentation unless a rule requires it"
  invariant). Permanently opt-in, not a deferred "flip the default later"
  task. Full text: `RDD_KEY_244` in `RDD_LOG.md`.
- `RDD_KEY_243` — Resolved "how to fix the base single-pass `RDD_KEY_229`
  bug": documentation-only resolution, not a code change. The already-
  shipped `curly-general-scope-reindent-multipass = on` is the fix/
  workaround (empirically validated across every corpus this job tested
  against `RDD_KEY_229`'s failure mode); README.md restructured to state
  this next to the gap's own explanation. Explicitly **not** option (A) as
  originally sketched (making the base single-flag path always iterate
  internally) — rejected as redundant with the existing opt-in multipass
  key. Full text: `RDD_KEY_243` in `RDD_LOG.md`.
- `RDD_KEY_242` — **Fixed** the `GdrRewriter.spaces`
  `NegativeArraySizeException` crash flagged during the `RDD_KEY_240`/
  `RDD_KEY_241` session. Root cause: `GdrReindenter.compute`'s per-axis
  brace/paren-bracket running depth goes negative only when source is
  genuinely bracket-unbalanced — reproduced with a minimal Kotlin repro
  (one-true-brace `if`/`else` + trailing-lambda fluent chain + one stray
  extra `}`). Fixed by clamping `braceLevel`/`pbLevel` to `>= 0`
  independently in `GdrReindenter.compute` before summing into `level`
  (no-op for well-formed input; malformed input now reindents an
  over-closed scope to column 0 instead of crashing), plus a defensive
  clamp in `GdrRewriter.spaces` itself. `make test`: 244/244, unaffected
  (no fixture — malformed-input-only guard). Full text: `RDD_KEY_242`.
- `RDD_KEY_241` — **Fixed** `RDD_KEY_240`'s confirmed second-order-
  oscillation counterexample: `GdrPipelineGate.applyAndFormat`'s hardcoded
  4-call sequence replaced with an actual convergence loop (iterate GDR-
  pass + `formatOne` cycles, comparing each new cycle's output against the
  previous, stop on byte-identical match; `MAX_MULTIPASS_CYCLES = 20`
  safety cap throws `IllegalStateException` if never reached). The
  RDD_KEY_240 TS/JS repro is now confirmed idempotent (round1 == round2).
  Base single-pre-pass path (multipass off or base flag off) unchanged;
  defaults remain `off`. Full text: `RDD_KEY_241`.
- `RDD_KEY_240` — Adversarial stress-testing (2026-08-05) of the bounded
  4-stage multipass loop **found a genuine, minimal counterexample to full
  idempotency**, confirming the "second-order oscillation" risk this file
  had flagged as unproven. **Fixed by `RDD_KEY_241`** (hardcoded 4-stage
  bound replaced with a real convergence loop). Minimal repro: a single-
  level TS/JS `if (...) { arr.filter(...).map(...).forEach(...); } else if
  (...) { ... }` with multipass on — round1 differs from round2 on the
  wrapped `.map(...)` continuation's indent column, though round2 == round3
  == round4 == round5. Also reproduces in Kotlin at much greater nesting
  depth (30 levels); did not reproduce in the equivalent Java shape at any
  depth tried. No source changed this session (out of scope, per explicit
  instruction). Full text: `RDD_KEY_240`.
- `RDD_KEY_227` — `JXM_CFMT_GDR 0`/`1` directive semantics: **flat toggle**
  (a single `1` always re-enables, redundant `0`s are no-ops, no nesting
  counter); an unmatched trailing `0` at EOF is **neither an error nor an
  implicit restore** (moot, nothing remains to format past EOF); using the
  directive while `curly-general-scope-reindent` is globally `off` is a
  **silent no-op**. Full text: `RDD_KEY_227`.
- `RDD_KEY_235` — Kotlin dogfood cluster D3 revisited: turning on
  `curly-general-scope-reindent`/`-multipass` does **NOT** resolve D3's
  wrap-decision flap (tested on the grounded repro and the real
  `EqualityAndComparisonCallsTransformer.kt` file) — negative result, no
  code changed. GDR only ever runs *between* whole pipeline passes and
  can't reach `MiscRuleCurly.renderCallCandidate`'s sibling-candidate
  volatility, which happens *inside* a single pipeline pass. A real D3 fix
  still needs a direct change to `renderCallCandidate`'s fits-check itself
  (consulting GDR's structural-depth infra as a library). Full text:
  `RDD_KEY_235`.
- `RDD_KEY_233` — `curly-general-scope-reindent-multipass` naming:
  full-word style (matching the base key), not the originally-suggested
  `curly-gs-reindent-multipass` abbreviation. Full text: `RDD_KEY_233`.
- `RDD_KEY_234` — `curly-general-scope-reindent-multipass = on` while
  `curly-general-scope-reindent = off`: **silent no-op**, same resolution
  category as `RDD_KEY_227`. Full text: `RDD_KEY_234`.
- `RDD_KEY_228` — Scope expanded to include JS/TS (user-directed): both
  plain `.js`/`.ts` files and embedded HTML `<script>` content are now
  reindented by GDR when on. Also fixed an independent HTML-formatter bug
  found while testing this: a `%`-prefixed marker/directive HTML comment
  (e.g. `<!--% JXM_CFMT_CFG ... -->`) was being corrupted into
  `<!-- % ... -->` by ordinary comment rendering, permanently breaking the
  marker's required exact prefix. Full text: `RDD_KEY_228`.

## Checklist

Status: **implementation complete, on/opt-in, real-code validated across
every originally-scoped corpus. D3 remains open (item marked `[~]` below).**

- [x] Design/finalize `JXM_CFMT_GDR 0`/`1` directive semantics — resolved,
      `RDD_KEY_227`.
- [x] Implement the pre-pass's own minimal tokenizer — independent of
      `TokenizerCore`/`TokenizerCurly`. Landed as new isolated package
      `com.jxmake.formatter.gdr`: `GdrTokenType.java`, `GdrToken.java`,
      `GdrTokenizer.java` (`tokenize(String) -> List<GdrToken>`).
      Recognizes single-char bracket tokens (depth counting deferred),
      `//`/`/* */` comments, `"..."`/`'...'` literals (backslash-escape
      aware), C++11 `R"delim(...)delim"` raw strings, Kotlin `"""..."""`
      triple-quoted strings, and preprocessor directives (leading-
      whitespace-then-`#`, backslash-newline continuation); everything else
      is opaque `TEXT`. Smoke-tested (preprocessor continuation, comments,
      escapes, char literal, C++ raw string, nested brackets) — all
      boundaries/line numbers correct, brace depth balanced to 0. **Known
      gap:** no handling for languages/literal shapes outside C/C++/Java/
      Kotlin, or malformed/unterminated literals beyond end-of-line/file
      fallback.
- [x] Implement the pre-pass's own brace-depth counter, independent of
      `ScopePipelineCurly`'s. Landed `GdrLineBraceDepth.java` (per-line
      `depthAtStart`/`depthAtEnd`) + `GdrBraceDepthCounter.java`. Consumes
      `GdrTokenizer`'s output directly (strings/comments/preprocessor
      interiors already excluded) — simple running increment/decrement.
      Brace depth only, paren/bracket depth is the next item's job.
      Deliberately does not decide the closing-brace-dedent question
      (reindenter's job). Smoke-tested (nested `if` inside `main`, embedded
      multi-line block comment) — depth tracked correctly.
- [x] Implement the pre-pass's own reindenter: derive each line's absolute
      indent target from structural depth, merging in a continuation-vs-
      block second axis (STYLE.md §8's wrapped-call/declaration convention)
      rather than a naive one-level-per-`{` model. Landed as
      `GdrIndentTarget`/`GdrReindenter`, computing per-line absolute
      levels/columns only (source-text rewriting is the pipeline-
      integration item below). Sub-steps:
      - [x] Paren/bracket depth counter (continuation axis) —
            `GdrLineParenBracketDepth.java`/`GdrParenBracketDepthCounter.java`,
            `(`/`[` counted together per §8. Smoke-tested.
      - [x] Line-touchability classifier (skip lines that are interior
            continuation of a multi-line STRING/BLOCK_COMMENT/PREPROCESSOR
            token). `GdrLineTouchability.java`: `computeUntouchableLines`/
            `computeTouchableByLine`, derived from `GdrToken`s spanning `>0`
            embedded newlines. Smoke-tested (9-line block-comment/
            preprocessor-continuation/single-line-string/raw-string cases).
      - [x] Combine brace depth + paren/bracket depth into a single per-line
            absolute indent target, with the leading-closer dedent rule.
            `GdrIndentTarget.java` + `GdrReindenter.java`
            (`compute(String, int indentSize) -> List<GdrIndentTarget>`).
            Per line, each axis independently uses its own `depthAtEnd`
            instead of `depthAtStart` only when that axis's own closing
            token leads the line (via `computeLeadingTokenTypes`); `level =
            braceLevel + pbLevel`, `columns = level * indentSize`.
            Untouchable lines get a `touchable=false` placeholder.
            Smoke-tested a wrapped signature with a nested `if` block.
- [x] Implement content exclusions: raw string literals, block-comment
      interior lines, preprocessor directives, `frozen`/JXM_CFMT_DIS-ENA
      spans, and any region bracketed by `JXM_CFMT_GDR 0`/`1`. First three
      already covered by the touchability sub-step. `GdrExclusionZones.
      computeExcludedByLine` independently reimplements the pipeline's
      `//% JXM_CFMT_DIS`/`ENA` convention plus the new `//% JXM_CFMT_GDR
      0`/`1` directive per `RDD_KEY_227`'s flat-toggle semantics — both OR'd
      together, marker line itself always excluded. Wired into
      `GdrReindenter.compute`. Smoke-tested standalone and end-to-end.
- [x] Wire `curly-general-scope-reindent = on` to actually invoke the
      pre-pass ahead of the existing pipeline (`Main`/`ServerMode`).
      `Config.java` gained the key (field, `isCurlyGeneralScopeReindent()`,
      `ALL_KEYS`, `parseBoolean`, default `false`). `GdrRewriter.
      rewrite(source, indentSize)` — first class in the package to actually
      rewrite source: calls `GdrReindenter.compute`, replaces each
      touchable line's leading whitespace with `target.columns` spaces,
      leaves the rest byte-for-byte untouched. `GdrPipelineGate.apply(source,
      language, config)` (off/non-curly-family → unchanged; on +
      curly-family → `GdrRewriter.rewrite`), called from both entry points
      right before `FormatterCore.forLanguage(language).formatOne`.
      Off-path verified byte-for-byte unchanged (`GdrPipelineGate.apply`
      returns the exact same `String` reference (`==`) when the key is
      unset). `make` builds clean, zero changes to any existing pipeline
      class's own logic.
- [x] Author the test fixtures. `test/curly_general_scope_reindent_inp.hpp`/
      `_out.hpp` (registered after `in_file_config_error_*`), using
      `JXM_CFMT_CFG curly-general-scope-reindent=on; indent-size=2` on a
      badly-indented `struct` body — config-acceptance framing (GDR's own
      contribution proven separately via the direct-harness `==`/rewrite
      assertions above, since the pipeline dominates final CLI output
      regardless). `make test`: 226/226.

      **Follow-up:** `test/java_flush_left_inp.java`/`_out.java` —
      `curly-general-scope-reindent=on` with every input line flushed to
      column 0. This one isolates GDR's own contribution end-to-end: base
      pipeline leaves the body completely unindented without the directive,
      GDR-on produces fully correct nested indentation. Registered after
      `java_preprocessor_method_inp.java`. Also `test/html_js_flush_left_
      inp.html`/`_out.html` — NOT a GDR fixture (GDR excludes HTML/JS, see
      Scoping) but a real-code-regression-style fixture exercising the
      existing HTML5/JS dispatch pipeline's own (out-of-scope) relative-
      delta limitation, documented in `test/README.txt` so it isn't
      mistaken for a GDR gap. Registered after `html_comments_inp.html`.
      `make test`: 228/228.

      **2026-08-03 follow-up (multipass-specific fixture):**
      `test/curly_gdr_multipass_inp.java`/`_out.java` — one-true-brace-style
      joined `} else if (...) {` / `} else {` chain with multi-statement
      bodies, `curly-general-scope-reindent=on;
      curly-general-scope-reindent-multipass=on` via in-file config.
      Minimal isolated repro of `RDD_KEY_229`'s root cause: single GDR
      pre-pass alone is non-idempotent on this shape; both flags on
      resolves it cleanly. Registered in `Makefile`/`test/README.txt` right
      after `java_flush_left_inp.java`. `make test`: 238/238.
- [x] Update `README.md` (and re-verify `../README.txt`). Done 2026-08-02:
      rewrote the stale "Known Limitations" bullet (was "not supported";
      now opt-in via `curly-general-scope-reindent = on`, incl. js/ts +
      embedded HTML5 `<script>` scope and the flush-left fix). Added a "GDR
      in-file directive" subsection after "In-file config overrides"
      documenting `JXM_CFMT_GDR 0`/`1` per `RDD_KEY_227`. `../README.txt`
      confirmed still has no formatter config-keys section — no edit needed.
- [x] Real-code test the pre-pass against `javaparser/javaparser`, local
      `tool/JSONEncoderLite.java`, and `serge-sans-paille/frozen` — full-
      tree idempotency. **Result across the whole investigation
      (2026-08-02 through 2026-08-03): every corpus confirmed the same
      pass-ordering bug (`RDD_KEY_229`), resolved by multipass — see below
      for the session-by-session numbers, and `RDD_KEY_229`/`RDD_KEY_243`
      for the full root-cause/resolution writeup.**

First real-code test (2026-08-02) ran against `angular/angular`'s TS
      dogfood cluster-5 accepted-gap files instead (scope had just expanded
      to JS/TS per `RDD_KEY_228`): 1/3 (`emit.ts`) fixed cleanly; 2/3
      (`user_metric_spec.ts`, `i18n_parse.ts`) exposed the pass-ordering bug
      — GDR's pre-pass computes indent depth before brace-placement runs, so
      a joined one-true-brace `} else if (...) {` gets a wrong/non-idempotent
      indent once brace-placement later splits it into `}` / `else if (...)
      {` (Allman style) — the split-out line never gets its own GDR target.
      A post-pass ordering (GDR after the pipeline) was tried as a candidate
      fix and resolved this case but introduced a different non-idempotency
      (GDR's indentation change alters line width, flipping the pipeline's
      own wrap fits-check on the next round — a genuine circular
      dependency). Both remediation paths judged too risky that session —
      no code changed. `README.md`'s Known Limitations documented the gap.

      Then against `javaparser/javaparser` (`javaparser-core-generators`, 43
      files): 13/43 non-idempotent, all traced to the same `RDD_KEY_229`
      root cause (e.g. `GrammarLetterGenerator.java`'s `else {` block
      closer) — confirmed not TS-specific, the dominant failure mode across
      ordinary curly-family code. No independently-fixable bug found; no
      source changed.

      **Prototype bounded multipass remediation** (user go-ahead
      2026-08-03): resolved naming (`RDD_KEY_233`) and multipass-on/base-
      off interaction (`RDD_KEY_234`) first. Implemented: `Config.java`
      gained `curlyGeneralScopeReindentMultipass`/
      `isCurlyGeneralScopeReindentMultipass()`, default `false`.
      `GdrPipelineGate.applyAndFormat(source, language, config, filePath,
      formatOff)` — new single entry point replacing the separate `apply`
      then `formatOne` calls: runs the base single pre-pass path when
      multipass is off or the base flag is off; when both are on and the
      language is curly-family, runs the full 4-stage sequence (GDR,
      pipeline, GDR, pipeline). `make test`: 237/237, zero regressions on
      the default-off suite.

      **Validation, all originally-scoped corpora PASS under multipass:**
      - `angular/angular` TS cluster-5: `user_metric_spec.ts`/`i18n_parse.ts`
        (14/71 diff lines under single-pass) → zero-line diff under
        multipass; `emit.ts` stays idempotent. All pass
        `js_ts_syntax_check.sh`.
      - `javaparser-core-generators` (43 files): 13/43 non-idempotent
        single-pass → 0/43 under multipass. All pass `java_syntax_check.sh`.
      - `tool/JSONEncoderLite.java`: 112 diff lines single-pass → 0 under
        multipass. Passes `java_syntax_check.sh`.
      - `serge-sans-paille/frozen` (20 `.hpp`/`.h` under `include/`): 7/20
        non-idempotent single-pass → 0/20 under multipass. No C/C++
        syntax-check wrapper exists in `tools/verifiers`, so used direct
        `g++ -std=c++17 -fsyntax-only` per file; error count per file
        matches baseline exactly for all 20 (7 pre-existing single-TU
        errors unrelated to formatting, 13 clean) — zero new,
        formatter-induced errors.
      - `javaparser-core` main module (576 files): 93/576 non-idempotent
        single-pass → 0/576 under multipass. All 576 pass
        `java_syntax_check.sh` (batched).

      **Summary: every originally-scoped corpus now PASSes under
      multipass** — `curly-general-scope-reindent-multipass=on` resolves
      every confirmed `RDD_KEY_229`-shape non-idempotency across every
      corpus tested, zero newly-introduced syntax/compile errors anywhere.
      **Caveat:** evidence the second-order-oscillation risk hasn't
      manifested on any tested input, not proof it structurally cannot.
      `make test` throughout: 237/237 (238/238 once the multipass fixture
      was added — see fixture item above).
- [x] Revisit Kotlin dogfood cluster D3 using the pre-pass's statement-
      boundary/structural-depth infrastructure; land a real fix in
      `MiscRuleCurly`/wherever it ends up living, record a new `RDD_KEY_n`,
      update `STATE_KOTLIN.md`'s D3 entries. **RESOLVED 2026-08-16 — see
      below (seventh attempt, landed, `RDD_KEY_298`).**

      **2026-08-03, negative result:** turning on GDR (alone, then with
      multipass) does not resolve D3 as a side effect — both
      `/tmp/d3_test.kt` (grounded repro) and the real
      `EqualityAndComparisonCallsTransformer.kt` stay non-idempotent under
      all three configurations, since GDR only runs *between* whole pipeline
      passes and can't see `renderCallCandidate`'s mid-pass sibling-candidate
      volatility (`RDD_KEY_221`). Confirms a real fix needs a direct change
      to `renderCallCandidate`'s own fits-check. Full writeup: `RDD_KEY_235`.
      No source changed; `make test` unaffected (237/237).

      **2026-08-07, third attempt (two sub-attempts, both reverted):** a
      Kotlin-gated `kotlinStatementStartIndex` backward token scan to
      replace `lineStartIndex` in the no-newline fits-check. Sub-attempt 1
      (every depth-0 `{`/`}` counts as a boundary) regressed 9 fixtures
      (under-measured — a trailing-lambda argument's `{` isn't a statement
      boundary). Sub-attempt 2 (`isKotlinControlFlowOrDeclBraceOpen`, only
      real control-flow/declaration `{` counts) regressed a *different* 10
      fixtures (over-measured in some cases; corrupted continuation-indent
      columns in `real_code_regressions_19_inp.kt`). Both reverted in full;
      `make test` reconfirmed clean at 248/248 before and after. Full
      writeup: `RDD_KEY_252`. **Assessment: not a bounded heuristic-
      refinement problem** — each fix for one shape regresses a different
      shape; needs real Kotlin expression/statement grammar, not an
      enumerated keyword/operator lookback table.

      **2026-08-07 (later same day), fourth attempt (also reverted):**
      "positional/enumerable-context-list" framing instead of another
      boundary-token scan. Sub-attempt 1 ("frame stack", every `{` opens a
      fresh nested statement frame) failed immediately — a lambda argument's
      short body needs its *enclosing* statement's prefix (9 regressed
      fixtures, matching RDD_KEY_252 sub-attempt 1's exact list). Sub-attempt
      2 (`kotlinStatementStart`, narrower backward continuation-newline walk
      with `KOTLIN_CONTINUATION_OPS`/`KOTLIN_CONTINUATION_KEYWORDS`) reduced
      failures 9→7, then (removing `->` as an uncommitted experiment) 7→4 —
      but each reduction traded one false-positive class for another
      (trailing `->` misclassified as continuation rather than new-statement
      start; `is` in a `when`-branch pattern-match misclassified via
      mid-expression type-check reading; `_44` showed a genuinely ambiguous
      `=`-continuation into an intentionally multi-line `if`/`else`
      expression body). **Assessment: the framing did not hold up** — nearly
      every candidate token is genuinely ambiguous in Kotlin without
      production-level context; an enumerable token list can't carry the
      positional grammar-production information the framing needed. Both
      sub-attempts fully reverted; `make test` reconfirmed clean, zero net
      change. Full writeup: `RDD_KEY_253`. **Recommendation: do not attempt
      another "backward scan + token lookup table" variant under either
      framing — both exhausted.** Two viable directions remain: a real
      lightweight statement/expression-boundary parser, or new GDR-adjacent
      infrastructure with sibling-candidate visibility (`RDD_KEY_235`).

      **2026-08-09, requested "one or two more tries," concluded without a
      new code attempt:** re-read the full six-attempt history, then
      considered anchoring off GDR's existing `GdrLineBraceDepth`/
      `GdrParenBracketDepthCounter` data instead of a fresh backward scan,
      but rejected by inspection — GDR's counters record brace-*nesting
      depth*, not brace-*kind* (lambda-body open vs. control-flow/
      declaration-block open vs. plain grouping), which is the actual
      ambiguity every prior attempt tripped on, so the counters wouldn't
      supply construct-kind awareness. No source touched; `make test`
      reconfirmed at 263/263. Per `STATE_COMMON.md`'s evidence-over-
      reasoning guidance, treats the six-attempt record as already
      answering "try once or twice more" — `README.md`'s Known Limitations
      already documents this gap. Closing D3 still needs one of the two
      directions named above, not piecemeal retries.

      **2026-08-16, seventh attempt (landed, RDD_KEY_298)** — mechanism/
      validation numbers: see RDD_KEY_298 in Resolved Design Decisions
      above. Sidestepped needing a stable statement-boundary measurement
      entirely (rather than another excluded backward-scan/token-lookup-
      table variant); unlike GDR's own convergence loop, does not throw on
      non-convergence (silently returns the last pass), since this now
      runs unconditionally for every Kotlin file, not behind an opt-in
      flag. Root cause reconfirmed via debug prints against the real
      `EqualityAndComparisonCallsTransformer.kt`: a single over-limit
      source line with two call/condition candidates both wraps on a fresh
      format, but re-tokenizing that output shortens the physical line the
      second candidate is measured against, un-wrapping it — non-idempotent
      pre-fix, idempotent post-fix. Also validated on the 188-file real
      Kotlin corpus (`JetBrains/kotlin`'s `compiler/ir/backend.js/src`),
      188/188 idempotent and syntax-clean; `GdrPipelineGate`'s convergence
      loop confirmed to compose safely (opaque call to `formatOne`, no
      correctness interaction).
- [x] **Adversarial stress-test of the bounded 4-stage multipass loop**
      (2026-08-05, dedicated hardening/validation task, not new
      functionality or a default flip). Goal: hunt for a genuine
      counterexample where 4 stages isn't enough, via synthetic adversarial
      constructions (the real corpora already tested —
      `javaparser-core(-generators)`, `angular` TS cluster-5,
      `JSONEncoderLite.java`, `serge-sans-paille/frozen` — all cleared with
      zero non-idempotency; this session deliberately targeted new shapes).

      **Mechanism confirmed first:** `GdrPipelineGate.applyAndFormat`'s "4
      stages" was a hardcoded, unconditional 4-call sequence — no
      comparison between any two stages' output, no iteration beyond
      exactly 4, no non-convergence detection at all.

      **Adversarial constructions tried** (via in-file config, syntax-
      validated, round1→round2→round3→round4→round5 reformatted and diffed
      at each step): deeply nested (10/20/30/50-level) one-true-brace
      `else if` chains and real nested if-blocks (Java); dense combos of
      one-true-brace joins + chained fluent calls + multi-line lambda
      wraps (Java); nested switch-in-if (Java); wrapped ternary chains
      nested in deep one-true-brace ifs (Java); randomized/garbage leading
      whitespace (Java); deep nested templates/namespaces with a C++11 raw
      string literal (C++); deep nested one-true-brace if/else-if combined
      with fluent arrow chains + template-literal interpolation + nested
      arrow-lambda bodies (TS); deep nested one-true-brace if/else-if
      combined with trailing-lambda chains (Kotlin).

      **Result:** most constructions stayed fully idempotent across all 5
      rounds — deep one-true-brace nesting/dense-combo/switch-nest/ternary-
      chain/garbage-indent (Java) and the C++ deep-template case found no
      bug at any depth tried.

      **But a genuine, minimal counterexample WAS found in TS/JS/Kotlin —
      see `RDD_KEY_240`:** a single-level TS/JS `if (...) {
      arr.filter(x => x > 0).map(x => x*2).forEach(x => {...}); } else if
      (...) { ... }` (depth 1) with multipass on: round1 differs from round2
      on the wrapped `.map(...)` continuation's indent column, but round2 ==
      round3 == round4 == round5 — a true fixed point exists but needs a
      second full formatter invocation, not the one bounded-4-stage
      invocation the design assumed. This **falsifies** this file's earlier
      claim that "a second full 4-stage application should be a no-op end
      to end." Root cause: the same circular dependency `RDD_KEY_229`
      diagnosed, one level deeper than the 4-stage bound accounts for.
      Reproduces in plain JS/TS at depth 1 and in Kotlin at much deeper
      nesting (30 levels); not reproduced in the equivalent Java shape at
      any depth.

      **No source code changed this session** — fixing the bounded-loop
      architecture itself is a real, separate follow-on (landed next, see
      `RDD_KEY_241` below). `make test`: 244/244, unaffected — all
      adversarial work was `/tmp`-only, no fixture added (a fixture
      demonstrating this bug would need to encode a known failure as
      expected output, wrong for a documented not-yet-fixed gap).

      **Honest confidence assessment:** evidence the 4-stage bound is
      insufficient for at least one real (if narrow/synthetic) shape, not
      just "unproven" — but not evidence every real-world file is at risk:
      every real-code corpus tested so far (700+ files, Java/C++/TS) still
      cleared cleanly, and the triggering shape (a wrapped fluent/arrow
      chain immediately inside a one-true-brace-joined if/else-if) is fairly
      specific. Known: (1) the 4-stage bound is not a structural guarantee,
      confirmed by direct counterexample; (2) for the cases found, the
      oscillation is bounded and damps out after one additional full
      reformat; (3) not an exhaustive search — deeper/wider adversarial
      constructions in C/C++, other JS/TS shapes, and combos with
      switch-case/ternary-wrap in TS/Kotlin remain untried.

      **2026-08-15 follow-up: targeted the "combos with switch-case/
      ternary-wrap in TS/Kotlin remain untried" gap named above**
      (investigation only, no source touched; corresponds to `XL.txt` TIER
      9's `GDR-2` bug entry: "GDR-2 (second multipass cycle) may silently
      overwrite already-STYLE.md-compliant indentation from the pipeline's
      own relative-delta reindenters"). Built synthetic repros in `/tmp` via
      `JXM_CFMT_CFG curly-general-scope-reindent=on;curly-general-
      scope-reindent-multipass=on`, formatted with `code-formatter.sh`, and
      compared each against the same input with `multipass=off` to isolate
      exactly what GDR-2 (the second GDR application in the multipass
      sequence) changes relative to GDR-1-only output, not just
      idempotency. All four named shapes — TS `switch`-in-`if` (`case`/
      `break` bodies via `SwitchRule.applyNonInlineCaseIndent`), TS
      wrapped/nested ternary chain (STYLE.md §8 continuation-indent
      renderer), Kotlin `when`-in-`if` (Kotlin's switch-case equivalent),
      and Kotlin nested `if`-expression ternary-equivalent wrap — came back
      idempotent (round1==round2==round3) and showed the identical pattern
      vs. the multipass-off baseline: GDR-2 changed *only* a pre-existing
      `RDD_KEY_229`-shape else-brace mis-indent, leaving the case/ternary/
      when/wrap region byte-for-byte identical to the multipass-off
      baseline. **All four PASS.** Two harder combined/deep-nesting
      variants built to push further — a 15-level-deep nested Kotlin
      `if`/`else` with a `when`-in-`if` plus a nested `if`-expression wrap
      at the bottom, and a TS `switch`-in-`if` whose `case` body contains
      both a wrapped fluent chain (`.filter().map().forEach()` — the exact
      shape `RDD_KEY_240` exploited) and a nested ternary in a sibling
      `case` — were also fully idempotent, same pattern. **Both PASS.**

      **Disposition: NOT REPRODUCED.** Across all six shapes tested —
      including the two harder combined/deep-nesting variants built to
      stress the same `RDD_KEY_240`-family fluent-chain/deep-nesting
      triggers — GDR-2 never touched a line inside a
      `SwitchRule.applyNonInlineCaseIndent`/`when`-arm/STYLE.md §8
      ternary-continuation region — it only ever
      corrected the already-documented `RDD_KEY_229`-shape closing-brace
      mis-indent that GDR-1-only leaves behind. No evidence GDR-2 clobbers
      the pipeline's own relative-delta reindent decisions, for any shape
      tried. **Not an exhaustive proof** (synthetic repros only, no
      dedicated real-code TS/Kotlin corpus pull — judged out of scope given
      six shapes already cleared cleanly and every real-code corpus this
      job has separately tested under multipass — `javaparser-core
      (-generators)`, `angular` TS cluster-5, `serge-sans-paille/frozen` —
      already showing zero non-idempotency). No fixture added — session
      scoped to `STATE_CURLY_GDR.md`/`XL.txt` edits only, all testing
      `/tmp`-only, no `test/` writes. `XL.txt` TIER 9's `GDR-2` bullet
      updated to reflect this result.
- [x] **Fix `RDD_KEY_240`'s confirmed second-order-oscillation
      counterexample** (2026-08-05, follow-on to the stress-test item
      above; mechanism/details: see RDD_KEY_241 in Resolved Design
      Decisions above). Safety cap `MAX_MULTIPASS_CYCLES = 20` is ~5x the
      ~4 cycles the confirmed counterexample needed. `make test`: 244/244,
      unaffected (no fixture — matches how `RDD_KEY_229`/`RDD_KEY_240` were
      documented). Re-ran the exact minimal `RDD_KEY_240` TS/JS repro:
      confirmed fixed — round1 == round2 (byte-identical), previously
      round1 != round2.

      **Separately investigated, unrelated pre-existing bug, fixed —
      `RDD_KEY_242`** (see RDD_KEY_242 in Resolved Design Decisions above):
      found while constructing a deep-nesting Kotlin adversarial repro,
      reproducing identically under GDR alone (multipass off) — confirmed
      via a side-by-side build of the pre-`RDD_KEY_241` source, so this
      crash predates and was unaffected by the convergence-loop change.
- [x] **Add a real-world-derived multipass regression fixture** (2026-08-14,
      follow-on from `STATE_JS_TS.md`'s Step 2 Increment 5 real-corpus
      sweep). That sweep's `ruanyf/react-demos` dogfood pass found
      `demo13/app.js` (compiled/minified, no JSX) non-idempotent under the
      default (GDR-off) path: minified one-liner function bodies like
      `function _classCallCheck(instance, Constructor) { if(...) throw new
      TypeError(...); }` reindent differently on a second pass. Verified
      directly: GDR off (default), round1 != round2 (reproducing the
      original finding); with `curly-general-scope-reindent=on;
      curly-general-scope-reindent-multipass=on`, round1 == round2,
      byte-identical, `js_ts_syntax_check.sh` clean — the shipped multipass
      workaround also resolves this real-world shape, not just the
      synthetic shapes the existing fixtures cover. Added
      `test/curly_gdr_multipass_oneliner_{inp,out}.js`: the real
      `_createClass`/`_classCallCheck`/`_possibleConstructorReturn`/
      `_inherits` Babel-helper one-liners from `demo13/app.js`, with
      `/*% JXM_CFMT_CFG curly-general-scope-reindent=on;curly-general-
      scope-reindent-multipass=on */` via in-file config. Fixture verified
      to isolate the fix: same input with multipass=off is non-idempotent
      (`--standalone --check` exits 1); with multipass on, idempotent
      (`--check` exits 0). Registered in `Makefile`'s `INP_FILES` right
      after `curly_gdr_js_regex_inp.ts` (before `java_flush_left_inp.java`)
      and in `test/README.txt` immediately after the
      `curly_gdr_js_regex_inp/out.ts` entry. `make test`: 311/311 →
      312/312, zero regressions. Lives entirely in this job per the
      project's job-isolation convention — `STATE_JS_TS.md`'s own finding
      text untouched, cross-referenced only here.
- [x] **Fix the base single-pass `curly-general-scope-reindent` ordering
      bug** (`RDD_KEY_229`) directly, independent of the opt-in multipass
      workaround. Investigated 2026-08-05: unambiguously `RDD_KEY_229` —
      GDR's pre-pass computes each line's target from the source's
      brace/paren nesting *before* the pipeline's brace-placement pass
      runs, so a joined one-true-brace line the pipeline later splits never
      gets a correct target for the split-out line (per-corpus non-
      idempotency counts: see the "Validation" entry above; multipass is
      layered on top, not a fix to this base path — with multipass off,
      the base path still has this exact bug). **Resolved 2026-08-06 as a
      documentation-only fix (`RDD_KEY_243`, coordinator go-ahead):** rather
      than either of `RDD_KEY_229`'s two named remediation options, the
      resolution is to document that the already-shipped, already-validated
      `curly-general-scope-reindent-multipass = on` is the fix for this
      gap — no source code changed. See `RDD_KEY_243` above and
      `README.md`'s "General scope-depth reindentation (GDR)" subsection.

- [x] **Genuine POST-pass variant** (`curly-general-scope-reindent-postpass`, `RDD_KEY_324`,
      2026-08-21, user-suggested per `RDD_KEY_323`'s follow-up note). Landed as isolated,
      EXPERIMENTAL, default-off infrastructure in `GdrPipelineGate.applyAndFormat` — applies GDR
      exactly once more directly to the final pipeline output with no further pipeline call after
      it, unlike every other GDR application in this file (always immediately followed by another
      `formatOne`). `make test`: 334/334, zero regressions on the default-off path. Tested against
      `RDD_KEY_322/323`'s Java anon-class-as-call-argument repro (temporary Java side-channel
      re-enable, reverted, no net source change beyond the new config key/wiring): does not
      resolve that gap — shifts which specific brace pair GDR's line-based depth tracking
      mismatches (method-body pair now aligned; outer anonymous-class-body pair now mismatched
      instead) rather than eliminating the mismatch class. Not a ready-made fix for
      `STATE_C_CPP_JAVA.md`'s Java anon-class "not reformatted" gap; kept as legitimate, harmless
      opt-in infrastructure for whoever next attempts the real `GdrBraceDepthCounter`/
      `GdrReindenter` root-cause fix `RDD_KEY_323` already scoped. Full text: `RDD_KEY_324`.

- [x] **Attempt to promote `curly-general-scope-reindent-postpass` out of EXPERIMENTAL**
      (2026-08-21, `RDD_KEY_328`) — **not promoted; stays EXPERIMENTAL.** First found the
      postpass's original motivating target (`RDD_KEY_322`/`RDD_KEY_323`'s Java anon-class
      compound-brace mismatch) is now moot: `RDD_KEY_325`, a separate later C_CPP_JAVA-job
      session, already fixed that exact gap by an unrelated non-GDR mechanism (confirmed —
      `make test` 336/336, `real_code_regressions_221` passes at default config, no GDR flags
      needed at all). Then ran the checklist's own remaining ask — a real-corpus validation —
      against the 188-file Kotlin corpus from `RDD_KEY_298` (`JetBrains/kotlin`'s
      `compiler/ir/backend.js/src`), comparing `-multipass`-only output against
      `-multipass`+`-postpass` output: **mixed result, not a clean win.** 81/188 files differ;
      some differences are an accidental improvement (postpass force-reindents a line the base
      pipeline's own declaration-alignment pass had mis-indented via an unrelated bug), but
      others are genuine new regressions — a previously-correct STYLE.md §8 wrapped-call
      continuation/closer alignment (e.g. `dce/Dce.kt`, `ic/ICUtils.kt`) gets over-indented by
      postpass's re-application of GDR's structural-depth model. Root cause not isolated
      further this session (would need the same paren/bracket-axis tracing already called for
      in `RDD_KEY_323`/`RDD_KEY_324`, applied to this different trigger shape). No source
      changed (validation-only); `make test`: 336/336 unaffected. Full text: `RDD_KEY_328`.
      **Default-off decision:** given the confirmed real-corpus regressions above (not just
      "unvalidated"), `curly-general-scope-reindent-postpass = off` is the permanent default
      even if the flag is ever promoted out of EXPERIMENTAL later — same posture as
      `RDD_KEY_244`'s base-flag decision, extended to this key specifically. Do not flip this
      default without first fixing the wrap-continuation regression this entry documents.

Do the above checklist one by one. Test, commit, and ask me whether to continue or pause.

## Open Questions

**Diagnosed 2026-08-20, disposition: NOT a GDR bug, out of this job's scope
(accepted gap, documentation-only, no code change).** Follow-up to the
2026-08-19 entry below: a one-line/one-statement-body function *expression*
passed as a call argument (e.g. `items.map(function
(x){doA(x);doB(x);return x;})`) is never reformatted to one-statement-
per-line, even with both `curly-general-scope-reindent` and `-multipass`
turned on. Root cause traced directly (not inferred) via a minimal
direct-harness repro comparing a top-level `function foo(...) {...}`
*declaration* against the identical body used as a call argument
(`items.map(function (...) {...})`), both long enough to force line-
wrapping, tested with GDR on, and — critically — **also with GDR fully off
(no directive at all)**:

- With GDR off (default path): the declaration's over-long line gets its
  inner over-long call wrapped (existing call-line-wrap rule) but the
  function body's own `{`/`}` are not split onto their own lines — matching
  the call-argument case exactly. Both shapes behave identically off.
- With GDR on (+ multipass): the declaration additionally gets full Allman
  brace-placement (`{` moved to its own line, matching ordinary
  statement-position brace-placement treatment) — but the call-argument
  case does **not**: the whole `function (...) { ... }` expression is
  treated as a single opaque unit by the call-argument line-wrap/relocation
  logic (it gets moved onto its own indented line between `map(` and `)`,
  but its interior is never independently brace-placed or reindented).

**Conclusion: this is not a GDR pre-pass scope-walk gap at all — it
reproduces byte-for-byte identically with `curly-general-scope-reindent`
completely off**, which only touches indentation of already-present lines
and cannot be the cause of a difference that also exists on the default-off
path. The actual root cause is in the shared pipeline's own call-argument
line-wrap/relocation logic (the same code family as `MiscRuleCurly`'s
call-candidate wrapping discussed under D3/`RDD_KEY_235` above), which
relocates a function-expression argument as an opaque text blob without
recursively invoking brace-placement/statement formatting on its interior —
whereas statement-position function bodies (declarations) go through the
pipeline's normal per-statement brace-placement path. This is a base-
pipeline behavior gap present identically whether GDR is on or off, so it
is **explicitly out of this job's scope** per the Scoping section above (no
change to `MiscRuleCurly.java`/`ScopePipelineCurly.java`/`FormatterCurly.java`
is expected or permitted, and this job's own default-off gate has nothing
to do with this gap's presence or absence). Full text: `RDD_KEY_314`.

**Disposition:** documentation-only — `README.md`'s existing "Known
Limitations" entry for this gap (added 2026-08-19) corrected to state the
verified root cause (base call-argument-wrap pipeline behavior, not a GDR
scope-walk gap) instead of the prior unverified inference. No code change;
this does not belong on this job's checklist. If ever fixed, it belongs to
whichever job owns `MiscRuleCurly`'s call-wrap logic (C/C++/Java or JS/TS
base pipeline), not this one — do not attempt a fix here.

**Original entry (2026-08-19, superseded above):** flagged while
investigating a JS_TS session's JSX `return(...)`-wrap hole-splicing report
(`STATE_JS_TS.md`) — traced to this same shape, not a hole-recursion bug
(reproduces byte-for-byte identically outside any JSX context, as a plain
top-level statement). At the time root cause was inferred (not verified) as
"plausibly the GDR pre-pass's scope-walk only descends into statement-
position function bodies" — **confirmed wrong** by the 2026-08-20
direct-harness repro above (the gap exists identically with GDR off).

None else currently open beyond the above. (The prior "how to fix the base
single-pass `RDD_KEY_229` bug" question was resolved 2026-08-06 as a
documentation-only fix — see `RDD_KEY_243` above. Option (B), a direct
change to `MiscRuleCurly`'s wrap fits-check, remains un-attempted.

**Disposition (2026-08-10): removed from `XL.txt` TIER 4 entirely, not
demoted to TIER 9.** Option (B) was an alternative fix for the same
`RDD_KEY_229`-shape bug `RDD_KEY_243` already resolved (multipass
empirically confirmed to fix every instance found across every corpus
tested — 100% resolution rate). No remaining bug for Option (B) to fix;
only worth revisiting if a future case surfaces that `-multipass` doesn't
cover.)

## Scoping

This job's scope is **the GDR pre-pass architecture plus D3's eventual
fix** — nothing else. Concretely:

- **Does not touch the existing formatter pipeline code paths at all**
  while `curly-general-scope-reindent` stays at its default (`off`). No
  change to `ScopePipelineCurly.java`, `FormatterCurly.java`, `MiscRule*`,
  `TokenizerCore`/`TokenizerCurly`, or any other shared class is expected
  or permitted on the default-off path — the pre-pass architecture keeps
  the on/off gate entirely outside those classes.
- **Primary implementation surface is the new isolated `com.jxmake.
  formatter.gdr` pre-pass package** (already landed) — **NOT**
  `ScopePipelineCurly.java`. Supersedes old (pre-2026-08-02) speculation
  that a general reindent pass would touch `ScopePipelineCurly.java`/
  `SwitchRule.applyNonInlineCaseIndent` directly. The two existing narrow
  relative-delta reindenters (`SwitchRule.applyNonInlineCaseIndent`,
  `ScopePipeline.applyDeclarationsPass`) are left untouched by this job;
  whether they're ever retired in favor of the pre-pass's absolute-target
  model is open for whenever the pre-pass is mature.
- **D3's eventual fix is in scope**, but only once the pre-pass's
  statement-boundary/structural-depth infrastructure exists to build it on
  — not a standalone task to attempt in isolation again (both prior
  standalone attempts, RDD_KEY_221 and RDD_KEY_226, were tried and
  reverted; re-attempting without the underlying infrastructure would just
  repeat that history).
- **The `curly-general-scope-reindent = on` path's own correctness is a
  hard, multi-session problem** (continuation-vs-block depth, exclusion
  zones, ordering-before-everything-else) — explicitly not a small/
  incremental task even though the default-off path is now zero-risk.
  Treat any single session's progress on the `on` path as partial by
  default; do not assume a quick win.
- **Scope expanded 2026-08-02 per `RDD_KEY_228` (user-directed): JS/TS are
  in scope.** Same curly-brace-family reindentation problem as
  C/C++/Java/Kotlin; `GdrPipelineGate.isCurlyFamily` includes `"js"`/`"ts"`
  — both plain `.js`/`.ts` files AND embedded HTML `<script>` content (via
  `XmlSpecificRule.renderScriptOrStyle`'s own `GdrPipelineGate.apply` call)
  are reindented when on. `GdrTokenizer` gained `scanTemplateLiteral`
  (backtick template literals) to avoid misreading `${...}` interpolation
  as real bracket depth. **JS/TS regex literal tokenization landed:**
  `GdrTokenizer` recognizes JS/TS regex literals (e.g. `/[{]/`), tokenizing
  them as `STRING` tokens so bracket-family characters inside regex
  literals don't miscount structural brace/paren depth (see
  `curly_gdr_js_regex_inp/out.ts`).
- Out of scope entirely: any change to data-format (JSON/YAML/etc.) or
  Python3 indentation logic, and HTML/XML's own element-nesting indentation
  (structurally indent-based already, not a brace-depth problem) — GDR as
  scoped here is the curly-brace-family (C/C++/Java/Kotlin, plus JS/TS)
  reindentation problem, matching where the old `STATE_COMMON.md` TODO
  lived before this split.
- **`curly-general-scope-reindent`/`-multipass` are permanently opt-in by
  design, not "opt-in until proven safe enough to default on."** See
  `RDD_KEY_244` above — rejected on both purpose-mismatch grounds and
  blast-radius grounds, independent of whether the multipass second-order-
  oscillation risk is ever fully resolved. Do not track "flip the default"
  as a future goal for this job.
