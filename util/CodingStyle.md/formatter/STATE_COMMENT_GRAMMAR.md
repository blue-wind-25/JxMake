# STATE_COMMENT_GRAMMAR.md — Comment Grammar Classifier Tracker

*Self-contained. Does not require STATE.md to have been read first.*

## Purpose

This tracks an **accuracy upgrade** to the existing deterministic comment-normalization
rules (`normalize-comment-start-case` / `normalize-comment-end-period`, `### B — New config entries`
in STATE.md, already DONE). It is not a new feature — those two config keys already exist, default `on`,
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
  Once resolved: append the full decision to `RDD_LOG.md` (next
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

Extends the `### B — New config entries` config family (`normalize-comment-start-case`,
`normalize-comment-end-period`, default `on`/`on`).

```properties
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

Full entries live in `RDD_LOG.md` (shared RDD_KEY sequence). This tracker only lists
which keys apply here, for quick reference:

| RDD_KEY | One-line summary |
|---|---|
| RDD_KEY_94 | Deterministic rules vs. classifier split — rules own safe-skip cases; classifier's only job is the ambiguous-but-normalizable prose tail; trailing-comment position is not a reliable skip signal |
| RDD_KEY_95 | Non-Latin script gate is presence-based (any non-Latin codepoint → skip), not ratio-based — mixed-token comments defeat whole-string language-ID; out of scope permanently, not a threshold to tune |
| RDD_KEY_96 | Per-language keyword lists (no shared list across C/C++/Java/Kotlin) + two-stage check: cheap membership test first, contextual scoring only on actual keyword matches |
| RDD_KEY_97 | Weight determination for v1 is frontier-model-assisted (offline, one-time, baked into JAR as constants) — not hand-tuned, not corpus-trained; corpus training is a future swappable upgrade |
| RDD_KEY_98 | Accuracy/coverage target: 99% precision on the positive decision, threshold-set from data, coverage measured not targeted; ABSTAIN is zero-cost, false positive is a visible bug — asymmetric risk drives the default |

**Action for CLI:** append full RDD_KEY_94–98 entries to `RDD_LOG.md` following its
existing entry format (see RDD_KEY_90 as the most recent example of the expected level of
detail) before or during implementation. This tracker should not duplicate that content —
if the two ever disagree, `RDD_LOG.md` is authoritative.

## Handoff note (for the future session that implements this)

Suggested order:
1. Implement feature extraction + both gates (non-Latin presence, per-language keyword
   two-stage check) as pure functions — testable with zero weights.
2. Wire `comment-normalization-classifier` config key into `MiscRule`'s two existing funnel
   points, `off` by default, ABSTAIN-equivalent-to-`off` fallback.
3. Only then: generate weights (frontier-model-assisted per RDD_KEY_97) using a real example
   set per language, and set the threshold from RDD_KEY_98's precision target.
4. `make test` must pass with the new key `off` (default) showing zero behavior change from
   current `### B — New config entries` output, before touching weight generation.
5. README.md config entry added at the end, once the feature is real.

## Checklist

- [x] Boilerplate package `com.jxmake.formatter.classifier` created with empty/stubbed class
      files, each `throw new UnsupportedOperationException(...)` on unimplemented methods and a
      `TODO(comment-grammar)` pointing at the relevant step below. Not wired into `MiscRule` or
      `Config` — zero behavior change, `make test` re-run clean (68/68 PASS lines, "All tests
      passed").
  - `CommentDecision` — the `YES`/`NO`/`ABSTAIN` enum (this one is fully implemented, it's just
    a data type with no logic).
  - `CommentFeatureVector` — placeholder data holder, fields TODO.
  - `CommentFeatureExtractor` — stub for step 1's feature-extraction half.
  - `NonLatinScriptGate` — stub for step 1's RDD_KEY_95 gate.
  - `KeywordAmbiguityGate` — stub for step 1's RDD_KEY_96 two-stage gate.
  - `CommentClassifierWeights` — `BIAS`/`THRESHOLD` constants, currently `0.0` placeholders per
    RDD_KEY_97/98; real values are a later step, not part of this scaffolding.
  - `CommentClassifier` — stub entry point (`classify(CommentFeatureVector) -> CommentDecision`).
- [~] Step 1: implement `CommentFeatureExtractor`, `NonLatinScriptGate`,
      `KeywordAmbiguityGate` for real, as pure functions, unit-testable with zero weights.
  - [x] `NonLatinScriptGate.containsNonLatinScript` — per-codepoint `Character.UnicodeScript`
        check, excluding LATIN/COMMON/INHERITED (so digits, punctuation, and emoji don't
        false-trigger). Smoke-tested (ASCII, accented Latin, Cyrillic, emoji, empty string) in
        `/tmp`; `make test` 70/70 PASS unchanged.
  - [x] `KeywordAmbiguityGate.hasLeadingKeywordMatch` (stage 1) — own per-language keyword sets
        (C/C++/Java mirror `MiscRule`'s `COMMENT_NO_CAPITALIZE_*` per RDD_KEY_96's "no shared
        list" requirement; added a Kotlin set, previously absent from any comment-normalization
        keyword list). Leading-word extraction mirrors `MiscRule.capitalizeFirstLetter`'s. Smoke
        tested per-language incl. Kotlin and empty-string; `make test` 70/70 PASS unchanged.
  - [ ] `KeywordAmbiguityGate.resolveAmbiguousKeyword` (stage 2) — `CommentFeatureVector` no
        longer blocks this (see below, now implemented), but real contextual scoring needs
        actual weights, which is blocked on RDD_KEY_97/98 per "Scope split" above. Do not
        implement with invented/placeholder per-feature weights; leave as the stub until real
        weight generation happens.
  - [x] `CommentFeatureVector` fields (`targetWord`/`previousWord`/`nextWord`,
        `nextCharIsOpenParen`, `containsSemicolon`, `containsUrlOrFilenameOrNumber`,
        `commentType`, `hasNonLatinScript`, `hasLeadingKeywordMatch`) +
        `CommentFeatureExtractor.extract`. `targetWord` is always the comment's leading word
        (the only funnel-point ambiguity in scope today); `previousWord` is therefore always ""
        by construction, documented in the class javadoc rather than left unexplained.
        Smoke-tested (keyword leading word, next-word extraction, next-char-is-`(`, semicolon,
        URL/filename/number detection, non-Latin propagation, empty string, default-overload
        comment type) in `/tmp`; `make test` 70/70 PASS unchanged.
- [x] Step 2: wired `comment-normalization-classifier` config key into `Config.java` and
      `MiscRule`'s two funnel points (`capitalizeFirstLetter`, `stripSoleTrailingPeriod` /
      `stripSoleTrailingPeriodAcrossLines`), `off` by default.
  - `Config.java`: new key added to `ALL_KEYS`, `commentNormalizationClassifier` field (default
    `false`), `isCommentNormalizationClassifier()` getter, parsed in `fromRawMap`.
  - `MiscRule`: new `commentNormalizationClassifier` field threaded through a new most-specific
    constructor overload (old 5-arg overload now delegates with `false`, so no other caller
    breaks). When on, each funnel point calls a new private `classifyComment(content)` helper
    (`CommentFeatureExtractor.extract` + `CommentClassifier.classify`) instead of its
    deterministic logic; anything other than `CommentDecision.YES` behaves as `off` for that one
    comment, per the hard architectural constraint. The two funnel points classify
    independently (own content each), not a single shared decision -- matches how they're
    literally two separate "funnel points" per this file's own intro, and some callers invoke
    only one of the two (e.g. `capitalizePreprocessorTrailingComment` only capitalizes).
  - `CommentClassifier.classify` implemented (was a stub, now a live call path): applies the
    non-Latin gate, then the keyword stage-1 gate (falling straight to ABSTAIN on a stage-1 match
    since stage 2 is still a stub -- see Step 1's note above), then the score/threshold compare.
    With placeholder zero weights this always resolves to ABSTAIN, exactly as designed.
  - `ScopePipeline`/`Formatter.java`: threaded the new flag through the one production call
    chain (`Formatter.formatOne` -> `new MiscRule(...)` and `new ScopePipeline(...)`) via a new
    most-specific `ScopePipeline` constructor overload, old overloads delegate with `false`.
  - Verified end-to-end in `/tmp`: `off` still capitalizes/strips periods exactly as before;
    `on` leaves both alone (classifier ABSTAINs on placeholder weights, no crash). `make test`
    70/70 PASS, unchanged (default is `off`).
- [ ] Step 3: generate real weights (RDD_KEY_97) and threshold (RDD_KEY_98) — blocked until
      then, do not implement early per "Scope split" above.
- [x] Step 4: README.md config entry added (`### Config file format`'s `# ── Behavior ──` block),
      `off | on`, default `off` -- realigned that block's `=` column to fit the new key's longer
      name.

Do not attempt to expand this to non-Latin-script grammar support (e.g. a German- or
Japanese-aware classifier) — that was explicitly scoped out (RDD_KEY_95) as a separate,
much larger effort, not a natural extension of this one.
