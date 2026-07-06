# STATE_COMMENT_GRAMMAR.md — Comment Grammar Classifier Tracker

*Self-contained. Does not require STATE.md to have been read first.*

## Purpose

This tracks an **accuracy upgrade** to the existing deterministic comment-normalization
rules (`normalize-comment-start-case` / `normalize-comment-end-period`, Task B in STATE.md,
already DONE). It is not a new feature — those two config keys already exist, default `on`,
and are gated in `MiscRule` at the two shared comment-normalization call sites every comment
call site funnels through.

This tracker covers replacing/augmenting the *decision* those two gates currently make
(purely deterministic) with an optional classifier-backed decision, without touching how the
formatter actually mutates text.

### During implementation
- Implement one checklist section at a time
- After completing a section (or when the cumulative diff across all changed files
  exceeds ~50 lines, whichever comes first), do a checkpoint commit:
  1. Update STATE_COMMENT_GRAMMAR.md — check off completed items and update the active checklist.
  2. `git add util/CodingStyle.md/formatter/` (the entire formatter directory)
  3. `git reset util/CodingStyle.md/formatter/target/` (exclude build output)
  4. `git commit -m "<message>"` — short descriptive message, no strict format required,
     trailer ending with `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`
- Small related items within a section may be grouped into one commit if they
  are trivially connected — use judgment based on line count (~50 lines threshold)
- Never let implemented files and STATE_COMMENT_GRAMMAR.md drift out of sync — STATE_COMMENT_GRAMMAR.md must
  always reflect the true current state at every commit
- Never modify the files `util/CodingStyle.md/formatter/test/*_inp.*` unless they contain
  syntax errors (they are the test input files).
- Never modify the files `util/CodingStyle.md/formatter/test/*_out.*` unless explicitly
  asked (they are the reference output files that show the expected results).
- Ignore `XL.txt`, that is the user tracker file.
- Use `/tmp` for temporary smoke-test and mini-test files.
- NEVER perform filesystem-wide find; search first in `/tmp/claude-1000` or the project root.
  If still not found, ask me.
- Do not use static analysis as the primary method of bug diagnosis or regression checking.
  Prefer evidence over reasoning (using debug prints). Keep static analysis minimal—only
  enough to identify where to insert debug prints.

## Commit Workflow

Same discipline as `STATE.md`'s own (restated, not cross-referenced, per the
self-contained requirement above):

- Implement one checklist section at a time.
- Checkpoint commit after each section or when the cumulative diff exceeds
  ~50 lines, whichever comes first: update this file's checklist, then
  `git add`/commit the formatter directory (excluding `target/`).
- Trailer: `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`.
- **On any ambiguity:** stop, add the question to Open Questions below, mark
  the checklist item `[~]`, commit this file only, and wait for an answer.
  Once resolved: append the full decision to `STATE_rdd_log.md` (next
  `RDD_KEY_n`, continuing the shared sequence — do not restart numbering for
  Kotlin), add the key + topic to this file's own Resolved Design Decisions
  index below, then continue.
- **On any shared-class change:** re-run the full existing C/C++/Java test
  suite before committing, per the Hard Constraint above. Record the
  before/after test count in the commit message.

## Hard architectural constraint

**The classifier decides. It does not format.**

- Classifier signature: `(feature vector) -> YES/NO/ABSTAIN`. Nothing else.
- All text mutation stays in `MiscRule`'s two existing shared comment-normalization methods.
- The classifier is called from those same two funnel points as an alternate decision path
  behind a new config key — it must not introduce a third call site or touch any other rule
  class. This is what keeps the blast radius contained to comment normalization only.
- On ABSTAIN (low confidence) or any gate rejection (see below): behave exactly as if the
  relevant `normalize-comment-*` key were `off` for that one comment. Never guess.

## New config key

Extends the Task B config family (`normalize-comment-start-case`, `normalize-comment-end-period`,
default `on`/`on`).

```
comment-normalization-classifier = off   (default)
```

- `off`: current deterministic-only behavior, unchanged (this is the existing shipped behavior).
- `on`: the two existing keys' decisions are made via the classifier + gates below, instead of
  the current purely-deterministic logic.
- README.md entry for this key is added by CLI once implemented — no doc changes needed now.

## Scope split — mechanical (implementable now) vs. future (blocked on training data)

**Mechanical (deterministic, implementable independently, no training data needed):**
- Non-Latin script presence gate (see below)
- Per-language keyword list + two-stage ambiguity check (see below)
- Feature extraction plumbing (previous/next word, next-char-is-`(`, contains semicolon,
  contains URL/filename/number, parser-provided comment type, etc. — per the source doc)
- Scoring formula (`score = w·x + bias`, threshold compare) — this is just arithmetic, weights
  are a separate concern
- Config key wiring and ABSTAIN → `off`-equivalent fallback behavior

**Blocked on future decisions (do NOT implement until resolved):**
- Actual weight values (see "Weight determination" below)
- Per-language training/example sets used to derive those weights
- Threshold value that achieves the target precision (see "Accuracy target" below)

A future session can implement the entire mechanical half — feature extraction, gates,
scoring plumbing, config wiring — with placeholder/zero weights, and it will safely no-op
(everything scores below threshold, defaults to ABSTAIN) until real weights are supplied.
This is intentional: it lets implementation proceed without blocking on the weight-generation
step.

## Resolved design decisions

Full entries live in `STATE_rdd_log.md` (shared RDD_KEY sequence). This tracker only lists
which keys apply here, for quick reference:

| RDD_KEY | One-line summary |
|---|---|
| RDD_KEY_94 | Deterministic rules vs. classifier split — rules own safe-skip cases; classifier's only job is the ambiguous-but-normalizable prose tail; trailing-comment position is not a reliable skip signal |
| RDD_KEY_95 | Non-Latin script gate is presence-based (any non-Latin codepoint → skip), not ratio-based — mixed-token comments defeat whole-string language-ID; out of scope permanently, not a threshold to tune |
| RDD_KEY_96 | Per-language keyword lists (no shared list across C/C++/Java/Kotlin) + two-stage check: cheap membership test first, contextual scoring only on actual keyword matches |
| RDD_KEY_97 | Weight determination for v1 is frontier-model-assisted (offline, one-time, baked into JAR as constants) — not hand-tuned, not corpus-trained; corpus training is a future swappable upgrade |
| RDD_KEY_98 | Accuracy/coverage target: 99% precision on the positive decision, threshold-set from data, coverage measured not targeted; ABSTAIN is zero-cost, false positive is a visible bug — asymmetric risk drives the default |

**Action for CLI:** append full RDD_KEY_94–98 entries to `STATE_rdd_log.md` following its
existing entry format (see RDD_KEY_90 as the most recent example of the expected level of
detail) before or during implementation. This tracker should not duplicate that content —
if the two ever disagree, `STATE_rdd_log.md` is authoritative.

## Handoff note (for the future session that implements this)

Suggested order:
1. Implement feature extraction + both gates (non-Latin presence, per-language keyword
   two-stage check) as pure functions — testable with zero weights.
2. Wire `comment-normalization-classifier` config key into `MiscRule`'s two existing funnel
   points, `off` by default, ABSTAIN-equivalent-to-`off` fallback.
3. Only then: generate weights (frontier-model-assisted per RDD_KEY_97) using a real example
   set per language, and set the threshold from RDD_KEY_98's precision target.
4. `make test` must pass with the new key `off` (default) showing zero behavior change from
   current Task B output, before touching weight generation.
5. README.md config entry added at the end, once the feature is real.

Do not attempt to expand this to non-Latin-script grammar support (e.g. a German- or
Japanese-aware classifier) — that was explicitly scoped out (RDD_KEY_95) as a separate,
much larger effort, not a natural extension of this one.
