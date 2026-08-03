# STATE_AI.md — AI-Assist Design Reference and GRU Job State

This file documents the background and architecture for the JAR's built-in
`ai-assist` feature. Step 2 (argument-layout/getter-setter-grouping) is
permanently NOT FEASIBLE and is reference-only — no active work there. Step 3
(the GRU comment-classifier abstain resolution) is the active tracked job per
`CLAUDE.md`'s job table (`com.jxmake.formatter.classifier.gru`) and follows
the same `STATE_COMMON.md` process conventions as every other job.
(No dogfood corpus for this job — see `STATE_DOGFOOD.md`'s note.)

- **Step 2** (argument-layout / getter-setter-grouping) — **NOT FEASIBLE**. No
  tractable grouping-intent signal exists for the JAR to hand an LLM, at any
  model size tested.
- **Step 3** (comment-classifier abstain-case resolution) — **FEASIBLE, GRU
  only**. A narrow classification decision, not a layout-authorship judgment
  call. Only feasible via a purpose-trained bidirectional GRU — the
  small-instruction-tuned-LLM variant is **NOT FEASIBLE** (tested and failed;
  see below).

---

## Step 2 — AI Integration: NOT FEASIBLE (deferred)

The JAR cannot distinguish meaningful author-expressed argument grouping from
arbitrary line breaks — the core prerequisite for reliable AI candidate
selection — and no tractable heuristic exists for it. A small on-device model
(3B–7B) has no reliable basis for choosing between candidates without that
signal. The mechanical fallback (dropped form if args fit on one indented
line, one-per-line otherwise) is therefore permanent behavior when inline
exceeds 100 chars. The architecture (grammar-constrained single-token
response via `/v1/chat/completions`, candidate layout generation, fail-safe
fallback) remains valid and reusable if a grouping-intent heuristic or a
larger model (7B+) is proven reliable in future. Tier-3 aesthetic decisions
(argument layout, non-standard getter/setter grouping) are instead handled by
the capable-AI workflow in `README.txt` / `AI_PREAMBLE_AESTHETIC.md`.

Checklist — Step 2 (all NOT FEASIBLE, no implementation needed): `Config.java`
ai-assist keys, `AiDecisionClient.java`, `AI_DECISION_PROMPT.md`,
`MiscRule.java` Tier-3 AI hooks — all NOT FEASIBLE. `README.md` ai-assist
section — DONE (removed/replaced). `FORMATTER_DISCUSSION.md` Key Decisions
table update — NOT STARTED (low priority, reference-only decision).

---

## Background and Architecture (ai-assist)

Local on-device AI for Tier-3 judgment-call formatting decisions. **Hard
constraint:** purely additive — no existing Tier-1/Tier-2 rule behavior may
change.

Confirmed working design (tested with Qwen2.5-Coder-3B-Instruct-Q4_K_M via
llama.cpp on Raspberry Pi CM5), reused as Step 3's architecture pattern:

- JAR generates N candidate layouts for a Tier-3 decision point.
- Grammar constraint (`root ::= "0" | "1" | ... | "N"`) forces a single-token
  response via `/v1/chat/completions` (llama.cpp applies the model's chat
  template automatically — portable across llama.cpp/Ollama/vLLM/LM Studio).
  `temperature = 0.0`.
- Model never rewrites source text — JAR executes the chosen layout
  mechanically. AI only invoked when there's a genuine multi-candidate choice.

**Tools/compiler used:** llama.cpp (https://github.com/ggml-org/llama.cpp);
Qwen2.5-Coder-3B-Instruct-GGUF (tested: `qwen2.5-coder-3b-instruct-q4_k_m.gguf`).

---

## RDD_EXT entries (AI-assist architecture, not in RDD_LOG.md)

Never externally logged — no `RDD_LOG.md` entry, no collision risk with
`RDD_KEY_n`. Related `RDD_KEY_86`/`87`/`88` (externally logged) are in
`STATE.md`'s main index.

| Key | Topic |
|---|---|
| RDD_EXT_1 | Selection prompt + grammar constraint confirmed working for Qwen2.5-Coder-3B on llama.cpp; `/v1/chat/completions` used, not `/v1/completions`/native `/completion` |
| RDD_EXT_2 | Model never rewrites source; JAR executes chosen candidate mechanically |
| RDD_EXT_3 | Fail-safe on unreachable endpoint: fall back to option 0, log warning, continue |
| RDD_EXT_4 | Four candidate forms for call/declaration line-breaking (inline, dropped, preserve-groups+align, one-per-line); option 1 only when inline exceeds 100 chars; option 2 only when source already multi-line; option 2 uses comma-spacing normalization (calls) / existing §5 column grid (declarations) |
| RDD_EXT_5 | Semantic grouping (by type/name similarity) explicitly out of scope — option 2 preserves existing author-expressed grouping only |
| RDD_EXT_6 | Comment handling: trailing comments align normally; comment-only lines between groups opaque (option 2 only); inline block comments normalized in place; leading preamble comment disqualifies options 0/1/3 |
| RDD_EXT_7 | Call/declaration breaking distinct from signature breaking — signatures remain fully deterministic (existing §8 unchanged) |
| RDD_EXT_8 | No-AI fallback for line-breaking: attempt dropped (option 1) — fits ≤100 chars indented → dropped, else one-per-line (option 3). Fit check is the sole criterion. Applies to calls and forward declarations |
| RDD_EXT_9 | Endpoint unavailability cache: standalone — static `endpointDead` boolean, skip AI for process lifetime after first failure; server — static `lastFailedAt`, skip for `ai-retry-interval`s (default 60) then retry once; connect timeout 500ms; always fails safe to mechanical result |
| RDD_EXT_10 | Step 3 GRU output classes: same `YES`/`NO`/`ABSTAIN` as existing rule-based classifier, no finer intermediate class |
| RDD_EXT_11 | Step 3 GRU abstains below softmax confidence cutoff (default 0.5, stored in weights file, tunable), falls through to mechanical default |
| RDD_EXT_12 | Step 3 GRU tokenization: trailing/attached punctuation splits into its own token (`matrix.` → `matrix` + `.`); camelCase/snake_case stays whole |
| RDD_EXT_13 | Step 3 GRU OOV hashing: FNV-1a (32-bit) mod 1024 bucket index — deterministic, trivially identical on training and runtime sides |
| RDD_EXT_14 | Step 3 GRU weights file: top-level `"schemaVersion"` int (starts at 1), loader throws clear mismatch error; abstain-threshold also lives in this file |
| RDD_EXT_15 | Step 3 GRU Pool B (period-ambiguity) extraction: grep-based recall-favoring filter — 2+ `.` with one whitespace-surrounded, or an abbreviation-adjacent token (`etc.`/`vs.`/`approx.`/single-capital-dot) not followed by more lowercase text; false positives discarded during labeling |
| RDD_EXT_16 | Step 3 GRU training-data source policy: own dogfooded repos first (clearly owned/licensed), extend later with a vetted list of permissively-licensed (MIT/Apache-2.0/BSD-3-Clause) public repos once the pipeline is proven |
| RDD_EXT_17 | Step 3 GRU evaluation target: 90% precision bar to resolve ABSTAIN→YES/NO; below the bar, GRU itself abstains. Starting number, revisit once real measurement exists |
| RDD_EXT_18 | Step 3 GRU training hyperparameters (starting defaults): Adam, lr~1e-3, batch size 32 (superseded in practice — trainer uses configurable mini-batching, default 16, see below), 20-50 epochs with early stopping on validation loss, dropout 0.2-0.3 |
| RDD_EXT_19 | Step 3 Pool A/Pool B corpus storage: real extracted/labeled corpora and any derived real trained-weights artifacts are **never committed** — stay under `/tmp`/session scratchpad (or the user's own personal directory, when explicitly directed — see `tools/gru/README.txt`'s backup section for the exact commands). `tools/gru/sample_examples.txt` (checked in) holds only small, clearly-fake illustrative lines. **Named exception**: see `RDD_KEY_217` in `RDD_LOG.md` (shared numbering) — `tools/gru/sample_default.txt` and `code-formatter-ai-assist-weights.json` are committed, user-directed, license-compatibility rationale; this exception does not extend to any other real corpus/weights artifact |
| RDD_EXT_20 | Step 3 labeled-corpus schema: `<lang>\t<label:YES\|NO>\t<escaped-comment-text>` — label is binary ground truth (`ABSTAIN` is the GRU's own below-threshold behavior, never a ground-truth class) |
| RDD_EXT_21 | Step 3 labeled-corpus schema extension: 4th column `targetWordIndex` — `<lang>\t<label:YES\|NO>\t<targetWordIndex>\t<escaped-comment-text>`. 0-based index (after `GruClassifier.tokenize`) of the ambiguous word the label is about: leading keyword for Pool A, last token for Pool B |
| RDD_EXT_22 | The ~3.5k-word explicit vocab is a **permanent, checked-in** resource, not licensing-sensitive like real corpora/weights (RDD_EXT_19 doesn't apply): individual words/keywords aren't copyrightable subject matter (Feist v. Rural), and a word-frequency list reproduces no protected expression regardless of source corpus license. `tools/gru/explicit_vocab.txt` (3500 words: 154 keyword slots across every `Lang.java` language + 3346 frequency-derived common words) + generator `tools/gru/build_vocab.py`. Append-only once trained against — reordering/removing lines would shift embedding-row indices and corrupt existing weights files. `GruTrainer` loads it by default (`--vocab=` override) |
| RDD_EXT_23 | Step 3 optional synthetic-augmentation tooling (`gen_synthetic_prompt.py`/`regroup_synthetic.py`): chat-LLM-generated Pool A/B padding, explicitly NOT a substitute for the real `acquire_corpus.sh` + hand-labeling pipeline (RDD_EXT_19/20) — kept as a distinct, clearly-labeled source, never silently merged into the real combined corpus, to avoid teaching the GRU an LLM's idea of ambiguity rather than real-world distribution |

---

## Step 3 — Comment-Classifier Abstain Resolution: FEASIBLE (via purpose-trained GRU only)

Unlike Step 2, this is a narrow classification decision (does this word
function as a keyword or as prose here; is this trailing dot a sentence-ender
or part of a token), not a layout-authorship judgment call — a small
**purpose-trained** classifier (the GRU's ~500k-parameter footprint) can
plausibly handle it, unlike a small instruction-tuned LLM (confirmed NOT
FEASIBLE, see below). Builds on the already-implemented rule-based
comment-grammar classifier (Task H in `STATE.md`, `RDD_KEY_94`–`98`):
`CommentFeatureExtractor`/`CommentFeatureVector`, `NonLatinScriptGate`,
`KeywordAmbiguityGate`, `CommentClassifier`/`CommentClassifierWeights`
(`YES`/`NO`/`ABSTAIN`), gated behind `comment-normalization-classifier`
(defaults `on` since the 2026-07-30 KEYWORD_BIAS fix, see below).
Reuses Step 2's confirmed architecture pattern (grammar-constrained short
response, `temperature=0.0`, fail-safe fallback, `RDD_EXT_9` caching) — only
the small-LLM variant is NOT FEASIBLE, not the pattern itself.

**Small-LLM classifier fallback: NOT FEASIBLE (confirmed by testing).** Small
instruction-tuned models (1B–3B class) cannot reliably tell whether a word at
the start of a sentence is plain English prose or a language keyword —
exactly the `KeywordAmbiguityGate`/Step 3 task. Tested and failed: Qwen
(1B–3B), Qwen2.5-Coder (1B–3B), Gemma (1B–3B). Not tested, not expected to
fare better: Llama 3B. **A small on-device LLM will not be used for Step 3,
full stop** — not as v1, not as a fallback behind the GRU, not for
non-Latin-comment routing. The bidirectional GRU is the only Step 3 approach
going forward. Does not reopen Step 2. Doesn't rule out a larger model (7B+)
— untested, no such path currently designed.

**Model size determination:** a bidirectional GRU with ~500k parameters is
the best accuracy/latency/footprint balance for this narrow classification
decision. Bidirectional because the full comment text is available upfront
(not streamed) — only ~2x encoding compute, no autoregressive-latency
downside. Pipeline: rules first (high confidence → done; abstain → GRU
classifier → final decision). If GRU accuracy proves insufficient, the next
step is a fresh design discussion (larger model/different hyperparameters),
not a revival of the rejected small-LLM fallback.

**Non-Latin comments:** `RDD_KEY_95`'s `NonLatinScriptGate` disables the
rule-based classifier entirely (≡ `ABSTAIN`) for any comment with a
non-Latin codepoint, deferring to the full-file AI pass. Closed, not
unstarted: depended on the small-LLM fallback's multi-language
understanding, which is NOT FEASIBLE — no Step 3 LLM branch exists to route
to. `RDD_KEY_95`'s behavior stands unchanged. A GRU trained specifically on
non-Latin/mixed-language examples would be a distinct, unexplored idea.

### GRU implementation design (v1 target) — architecture finalized

| Component | Value |
|---|---|
| Input | word-level tokens, case-preserved (case is a real signal — `Return` reads less like a keyword than `return`), whitespace/punctuation split |
| Explicit vocab | ~3.5k: every keyword across every supported/planned language gets a guaranteed slot (never left to a hash bucket, since a keyword is exactly what triggers `KeywordAmbiguityGate`), plus ~3k common comment-corpus words |
| OOV handling | 1024-bucket hashing, not a shared `<UNK>` — distinguishable unknown identifiers |
| Embedding init | trained from scratch (pretrained vocabularies like GloVe/fastText blow past the param budget and reopen licensing/provenance questions) |
| Embedding dim | 16 (context modeling capacity, i.e. GRU hidden size, matters more than word-identity richness for resolving ambiguous usage) |
| Sequence cap | ~64 tokens per comment (truncate/pad) |
| GRU | single-layer bidirectional, hidden=224 |
| Target-word handling | index into the target word's own biGRU output (concat forward+backward) — no marker token |
| Head | concat(448) → dense(64, ReLU) → softmax(3 classes) |
| **Total params** | **~425k** (~75k headroom under the 500k budget) |

**Files** (new `com.jxmake.formatter.classifier.gru` package):
- `GruClassifier.java` — inference-only runtime code, shipped in the JAR.
  Loads a trained weights file at startup; no literal weight arrays in source.
- `GruWeights.java` — loader/schema for the external JSON weights file.
- `tools/gru/GruTrainer.java` — separate, non-shipped `main()` training entry
  point outside `src/`. Writes a weights file for the runtime classifier to
  read; never overwrites/generates `.java` source, so retraining is a
  resource-file swap.

**Training-set acquisition approach** (measure-first, two-pool design,
extends the `tools/classifier_weights/` pattern from `RDD_KEY_97`): don't pre-commit to a total
corpus size before measuring real ABSTAIN rate (done, see below). Pre-filter
every extracted comment through `CommentClassifier` first — high-confidence
YES/NO resolved for free, `ABSTAIN` is the real labeling target. **Two
pools:** Pool A (keyword-ambiguity) — large pool, targeted extraction toward
short comments (≤6-8 words) containing a known keyword. Pool B
(period-ambiguity) — small pool, punctuation-discussion comments and
abbreviation patterns (RDD_EXT_15's grep filter). Sources: own dogfooded
repos first (RDD_EXT_16), then vetted permissive public repos. Labeling:
Pool A via frontier-model labeling + spot-check; Pool B by hand (small, easy
call). Verification: flag mislabels via rule-based/label disagreement and
held-out regressions, correct in place with a why-note.

**Fail-safe:** missing/unreadable weights file → `GruClassifier` behaves as
`ABSTAIN` (classifier `off` for that comment) — no further LLM fallback,
never blocks formatting.

All "Open refinement items" (output classes, abstain threshold, tokenization,
hash function, weights schema, Pool B extraction, hyperparameters, eval
target, licensing) are resolved — see RDD_EXT_10–18 above. Real ABSTAIN-rate
measurement was also closed early (see condensed history below): 0.0-0.6%
typical rate across 14 corpora (~199k comments), confirming targeted (not
random-sample) extraction for Pool A/B.

---

## OPEN — Comment sentence-boundary detection defeated by mid-word dots (Step 3 candidate)

`MiscRule.stripSoleTrailingPeriod` (§15) strips a comment's trailing `.` only
when it's the *sole* `.` in the text — conservative, to avoid mangling an
ellipsis or an abbreviation followed by more text. Misfires when the comment
has an unrelated earlier dot that isn't a sentence-ender, e.g. (C++ `//`
form; same problem class applies to any comment syntax, per
AI_PREAMBLE_FULL.md §15):

```
// Combined .hpp test: pragma once, concepts, templates, classes, extern C.
```

`.hpp` and the trailing `C.` both count as dots, so `dotCount != 1` and the
genuinely sentence-ending trailing period is left in place (expected:
stripped). Distinguishing a mid-word/mid-token dot (file extensions, `e.g.`,
`i.e.`, `v1.0`, single-letter abbreviations) from a true sentence-ending dot
is a natural-language judgment call with no tractable mechanical heuristic —
exactly the class of ABSTAIN-worthy case Step 3 targets: the rule-based
classifier's `dotCount != 1` case would ABSTAIN, and the GRU classifier would
resolve it given enough mid-word-dot training examples. Not blanket NOT
FEASIBLE — feasible via Step 3's GRU once trained on this shape; until then,
remains an accepted mechanical-rule limitation (`dotCount != 1` → leave
as-is). Still open — no GRU work has targeted this shape specifically yet.

**TODO before attempting (2026-08-03):** unlike the keyword-ambiguity growth
passes (which add balanced rows to an already-wired path,
`KeywordAmbiguityGate`), it is not yet confirmed that `MiscRule.
stripSoleTrailingPeriod`'s `dotCount != 1` case actually calls
`GruAbstainResolver`/routes through the GRU at all, vs. unconditionally
leaving the period alone. **First step: confirm/add that wiring** (a real
gate/feature change, same weight-class as `CommentedOutCodeGate`/
`LicenseBlockGate`'s own design+real-corpus-false-positive-check process, not
a drop-in corpus-growth pass) before hand-labeling any mid-word-dot training
rows. Once wired, follow the established balanced-YES/NO-row growth-pass
pattern (see the 125→522-row session-log entries above), and re-run
`cross_validate.py` specifically checking this shape's held-out accuracy
(not just the aggregate number) — the training-fit-vs-held-out gap already
found on 2026-08-02 (98.7% vs 86.3%, traced to `GRU_HAND_LABELED_REPEAT`
oversampling) is a real risk for a new pattern with only a few examples.

---

## OPEN — corpus-generation and benchmarking follow-ups

- **LLM-assisted disagreement sampling against `sample_default.txt`.**
  Discussed 2026-07-30: a full LLM relabel (92039 lines, ~$few-$150) would
  just reproduce the rule-based classifier's existing blind spots, since the
  corpus is auto-labeled by that same classifier. Agreed direction: use an
  LLM to find *disagreements* on a small stratified sample (pull existing NO
  lines + a random YES slice via `shuf -n 300 --random-source=<(yes 42)`,
  label independently without showing the existing label, diff, hand-verify
  only disagreements, append confirmed ones append-only). Steps 2-4 (actual
  LLM-assisted labeling) are still not done — a first pass was done **by
  hand** instead (2026-07-30, no LLM call), see next item, which directly
  motivated the commented-out-code gate.
  **2026-08-04 — persistence plumbing built (user-commissioned), so
  confirmed corrections survive `make gru-acquire-corpus` regenerating
  `sample_default.txt` from scratch every run (previously, a hand-edited
  correction would have been silently wiped out by the very next run —
  flagged before any real corrections existed, so nothing was lost).** New
  committed file `tools/gru/disagreement_corrections.txt` (named exception to
  RDD_EXT_19, same footing as `sample_default.txt`/
  `code-formatter-ai-assist-weights.json` per RDD_KEY_217 — user confirmed
  this placement over a scratch-only alternative), empty until the
  disagreement-sampling process actually produces confirmed rows. New
  `tools/gru/apply_disagreement_corrections.py`, wired into
  `gru-acquire-corpus` right after the existing
  `classifier_weights_examples.tsv` append and before the final
  exact-duplicate-line dedup: does an *override* merge keyed on
  `<lang>/<targetWordIndex>/<escaped-comment-text>` (everything but
  `<label>`) — a plain append wouldn't work here since a correction and the
  auto-labeled row it corrects share the same comment text and differ only
  in `<label>`, so exact-line dedup can't collapse them; this script drops
  the conflicting auto-labeled row instead of leaving both present. Smoke-
  tested standalone (override case, new-row case, empty-file no-op case)
  against synthetic scratch files, not `make test` (Python tool, no `src/`
  change).
  **2026-08-04 — steps 2-4 actually executed, via the `grok` CLI (xAI Grok,
  headless `-p` mode + `--json-schema`/`--output-format json`/`--no-plan`
  /`--disable-web-search`/`--permission-mode dontAsk`) instead of the
  literal "existing NO lines + `shuf -n 300`" wording above: sampled 150
  unique-text YES + 150 unique-text NO rows (`random.Random(42)`) from
  `sample_default.txt`'s full unique-by-text pools (89305 YES / 3292 NO),
  since NO rows are now produced almost entirely by the three high-precision
  explicit gates added after the 2026-07-30 design note, making "all unique
  NO lines" low marginal value relative to cost. `grok-4.3` labeled all 300
  blind to the existing label (cost $0.057, no truncation). 74/300 (24.7%)
  disagreed with the existing auto-label — a high rate against a
  92.4%-precision classifier, flagged to the user rather than treated as
  ground truth. Every disagreement was hand-verified by the user
  (worksheet-based: skip/YES/NO per row, `TEXT=` newline-rendered for
  multi-line comments). Result: 44 confirmed genuine corrections (43 YES, 1
  NO) appended to `tools/gru/disagreement_corrections.txt`; the other 30
  disagreements were either skipped as too ambiguous or the user confirmed
  the original auto-label was already right (Grok wrong) — those never
  touch the corrections file. Applying the 44 corrections via
  `apply_disagreement_corrections.py` against the real, current
  `sample_default.txt` was verified clean (44/44 keys matched, 44 conflicting
  auto-labeled rows overridden, 0 mismatches). Not yet re-run through
  `make gru-acquire-corpus`/cross-validation to measure the resulting
  precision delta — that's the natural next step if this job is picked up
  again, but 44 corrections against a 93492-line corpus is a small enough
  perturbation that a follow-up CV run is optional polish, not a blocker.
  Given current production numbers already clear both bars (92.4% mean
  held-out CV precision at `abstainThreshold=0.7`, 2.7% NO false-positive
  rate — see the 2026-08-02 threshold-sweep entry below), actually running
  steps 2-4 to populate this file is now optional polish, not a blocker.
- **[SEPARATE, still open]** A cluster of extracted comments are DTD/URL
  string-literal fragments with no leading space (e.g. `Sun Microsystems,
  Inc.//DTD Enterprise JavaBeans 1.1//EN";`) that look like `//` inside a
  string literal — **root-caused and fixed 2026-08-01** (see `extract_comments.py`
  fix below); this bullet is closed.

---

## Condensed job history (through 2026-07-29)

Everything mechanical is real, not stubbed: `GruClassifier` (tokenize,
hashBucket, softmax, decide, real bidirectional-GRU `forward()`),
`GruWeights` (full schema, hand-rolled JSON parser, backward-compatible with
scalar-only fixtures), `Vocabulary` (explicit-vocab-vs-hash-bucket lookup),
`GruAbstainResolver` (real "rules → GRU on abstain" pipeline, config-gated
via `gru-classifier`/`gru-weights-path`), `tools/gru/GruTrainer.java` (real
training loop: Xavier/Glorot init, mini-batch forward+backward+Adam — RDD_EXT_18's
batch-32 default superseded by configurable `--batch-size`, default 16, see
2026-08-01 — 20% held-out validation split with patience-based early
stopping, reads RDD_EXT_21's 4-column schema, loads `explicit_vocab.txt` by
default per RDD_EXT_22), the `gru-train`/`gru-extract-pool-a`/
`gru-extract-pool-b`/`gru-measure-abstain-rate` Makefile targets, and five
passing self-tests. `GruClassifier.classify` abstains whenever
`hasTrainedWeights()` is false — fail-safe posture.

**Real ABSTAIN-rate measurement (item 9, CLOSED):** `tools/gru/
extract_comments.py` + `tools/gru/CommentAbstainTally.java` measured
**0.0%-0.6%** ABSTAIN across 14 corpora (~199,000 comments, own repos + 11
vetted MIT/Apache-2.0/BSD-3-Clause public repos), two explained outliers
(vendored bitmap-font/zlib/libjpeg content; one too-small 103-line corpus).
Random sampling impractical — targeted extraction (RDD_EXT_15) required.

**Pool A/B extraction, labeling, first production run:** `ExtractPoolA.java`/
`extract_pool_b.py`. Fixed `extract_comments.py` bug: literal `/*` inside a
`//` line comment swallowed later text — single left-to-right scanner now
treats `//` and `/* */` as mutually exclusive spans. Post-fix: 57974 comments
→ Pool A 167 / Pool B 241 candidates, hand-labeled per RDD_EXT_20 (Pool A:
45 YES/122 NO; Pool B: 41 YES/200 NO via documented rule-based fallback).
First production training (408 examples, 80/20, `--epochs=40 --patience=6`):
early-stopped epoch 15, held-out precision 97.96%. After permanent explicit
vocab (RDD_EXT_22) + retrain on same split: **93.88%** (current baseline;
97.96% is pre-RDD_EXT_22 historical) — clears RDD_EXT_17's 90% bar.
Spot-check: model uses surrounding context, not target token alone
(`"for the sake of clarity"` → YES vs `"for (int i..."` → NO).

**Tooling:** `acquire_corpus.sh` (16 hardcoded sources — 5 local dogfood + 11
vetted public — stops before labeling per RDD_EXT_20), `GruEval.java`
(precision/abstain-rate against an RDD_EXT_21-schema file),
`cross_validate.py`. Real 5-round CV on 408-example corpus: **precision
mean=92.40%, stdev=3.00%, min=89.80%, max=96.49%**. Working files never
committed (RDD_EXT_19); see `tools/gru/README.txt`.

**Optional synthetic-augmentation** (`gen_synthetic_prompt.py`/
`regroup_synthetic.py`): chat-LLM Pool A/B padding. Explicitly **not** a
substitute for real acquire_corpus.sh + hand-labeling (RDD_EXT_19/20/23) —
distinct source, never silently merged. `sp_gemini.txt` is vocab-only-checked
legacy data (predates request-tracking sidecar) — don't reconstruct its
request history.

**Full-scale acquire_corpus.sh (16 sources):** 172,285 comments → 578 Pool A
+ 492 Pool B candidates (~3.5x earlier batch), archived personal dir
(RDD_EXT_19), **left unlabeled** — auto-labeled distant supervision taken
instead (next section).

---

## Session log (2026-07-29 through 2026-08-02)

**2026-07-29 — default auto-labeled corpus, live wiring, `RDD_KEY_217`.**
`GenerateSampleDefault.java` auto-labels acquired corpus via rule-based
classifier (distant supervision) → `tools/gru/sample_default.txt`
(RDD_EXT_20/21 schema, `targetWordIndex=0`, `ABSTAIN` skipped; dedups —
77,499 dups removed from 172,285 run). Wired into `make gru-acquire-corpus`;
`make gru-train` default sample switched from `sample_examples.txt` to this
file. Full-scale: 170,210 kept, **100% labeled YES** — bootstrapping from
rule-based classifier alone teaches only YES/abstain-collapsed-to-skip, never
real NO (needs hand-labeled Pool A/B or different bootstrap). Root:
`CommentClassifier.classify` only returns `YES`/`ABSTAIN` (RDD_KEY_96), never
`NO`, until a NO-producing gate existed (`DecorativeSeparatorGate` below).

**`RDD_KEY_217`** — named exception to RDD_EXT_19: per explicit user direction
(license compatibility — MIT/Apache-2.0/BSD-3-Clause, traceable provenance,
short quoted excerpts), exactly `tools/gru/sample_default.txt` and
`code-formatter-ai-assist-weights.json` are committed. RDD_EXT_19 stands for
everything else.

Also: fixed 3 `targetWordIndex` bugs in `tools/gru/sample_examples.txt` Pool B
lines (one past tokenization end → silent skip every `GruTrainer` run; two
wrong token) — verified via live smoke run. Live wiring:
`MiscRuleCore.classifyComment` → `GruAbstainResolver.resolve(...)` (was
`CommentClassifier.classify` direct), threading `gruClassifier`/`gruWeightsPath`
through `MiscRuleCore` → `MiscRuleCurly` → `ScopePipelineCurly` →
`FormatterCurly`. `Config.gruClassifier` defaults `true` (fails safe to
ABSTAIN if weights missing).

**Finding: `comment-normalization-classifier` had to stay `off` by default at
this point** — both defaults `true` regressed 9 `make test` fixtures
(rule-based disagreed with `isCommentNoCapitalizeWord` on `consteval`/
`static`/`while`/`var`/etc.). Reverted `commentNormalizationClassifier` to
`false` (all-green); left `gruClassifier=true` (inert alone). Fixed next day
→ now defaults `true`.

First NO-producing gate: **`DecorativeSeparatorGate.isDecorativeOnly`**
returns `NO` for comments with no letter/digit (`****...****`, `-----`);
wired after non-Latin-script gate. Validated 96442-comment 5-repo corpus:
NO=20774 (0 before), 15 hand-spot-checked new-NOs, zero false positives.
`make test`: 219/219.

---

**2026-07-30 — fixed `KeywordAmbiguityGate` weight regression; `comment-normalization-classifier` now defaults `on`.**
Root cause of 9-fixture regression: 40-example
`tools/classifier_weights/examples_{c,cpp,java,kotlin}.md` had all 20
"zero mechanical feature" rows labeled YES → `KEYWORD_BIAS = +2.48420`, so
zero-signal keyword-led comments defaulted YES (wrong — overwhelmingly real
code reference). **Fix:** +22 zero-feature NO rows, 20 YES/22 NO, re-derived
via `derive_weights.py` (62 examples):

```
KEYWORD_BIAS                 = -0.20825   (was +2.48420)
KEYWORD_WEIGHT_PAREN         = -2.28827   (was -3.96297)
KEYWORD_WEIGHT_ARROW         = -1.51467   (was -3.22603)
KEYWORD_WEIGHT_SEMICOLON     = -2.96142   (was -4.93396)
KEYWORD_WEIGHT_URL_OR_NUMBER = -0.51492   (was -2.80469)
```

Negative bias defaults zero-signal keyword-led comment to ABSTAIN not YES
(asymmetric-risk: false skip zero-cost, false positive visible bug); 20/62
mismatch, all rare "keyword as plain English adjective", accepted.

Second bug (fixing 8/9 fixtures): `test/real_code_regressions_54_inp.java`
stray-period strip — `CommentFeatureExtractor.extract` always computed
`hasLeadingKeywordMatch` from comment's *first* word regardless of caller's
`targetWordIndex` (period-strip points at *last* token). Fixed with
`targetWordIndex`-aware `extract` overload.

Both fixes → `Config.commentNormalizationClassifier` default `true`:
**219/219 forward, 219/219 idempotency**. Both classifiers `true` briefly
(partially reverted same day).

**`gru-classifier` flipped back to default `off`.** Shipped weights on 62
hand-labeled: `total=62 abstain=0 decided=62 correct=19 precision=30.6%
yesCorrect=19/19 noCorrect=0/43` — all-YES, worse than linear 67.7% (42/62).
Root: `sample_default.txt` auto-labeled by linear classifier, no hard
ambiguous-keyword NO cases. **Fix:** `Config.gruClassifier` → `false`.
`commentNormalizationClassifier` stays `on`.

**Self-formatting dogfood-and-adopt (`src/`)** — first `STATE_COMMON.md`
process against formatter's own source. Found: comments starting with
slash-separated non-keyword identifiers (`sizeTokens/initTokens`, `val/var`)
wrongly capitalized (leading word + `/` never checked). **Fix:**
`CommentFeatureVector.leadingWordFollowedBySlash` + Gate 1c, returns `NO`
independent of keyword membership. `make test`: 220/220. Dogfood clean,
adopted `src/` (71 files + `GruAbstainResolverSelfTest.java`); rebuild
220/220.

**Extended self-formatting to `tools/*`/`tools/classifier_weights/*`** (36
Java/Python/JS files). **Bug 1 (formatter, fixed in `src/`):**
`#!/usr/bin/env node` shebangs in `tools/verifiers/*.js` corrupted — `#` only
preprocessor for C/C++, JS fell through and `enforceSemicolonInsertion`
appended `;`. **Final fix:** `TokenType.SHEBANG` (in `Token.isGapToken`, never
`//`-rewritten) + `TokenizerCurly.emitShebangLine()`, only at
`pos==0 && c=='#' && peek(1)=='!'`. 220/220. **Bug 2 (comment-classifier
false positive, hand-fixed, NOT gated):** two `GruAbstainResolverSelfTest.java`
comments starting with hyphenated config-key got wrongly capitalized.
Blanket `leadingWordFollowedByHyphen` gate **rejected** — also suppressed
legitimate English compounds (`non-negative` → `Non-negative`). **Decision
(user-confirmed):** revert hyphen gate, hand-edit the two comments. Prefer
rewording over new blanket gate if recurs. After both: 220/220, adopted all
36 files; verified `node --check` + e2e (`.js`), `python3 -m py_compile`
(`.py`), clean compiles for `.java` (Kotlin-compiler-dependent need
`~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib`; JDK11+ need
`/opt/openjdk-21_linux-x64_bin/jdk-21`).

**Findings from first (hand-run, no LLM) disagreement-sampling pass:**
1. **[ACTIONABLE, resolved 2026-07-31 — commented-out-code gate below]**
   Commented-out code mislabeled YES is common: full 91064-line YES pool
   ending in `;` → **984 candidates (~1.1%)**; ~50 spot-checked mostly real
   commented-out code (C/C++/Java/JS). **Caveat:** bare trailing `;` alone
   unsafe — 25-line spot-check ~8% genuine prose with clause-ending
   semicolon — same asymmetric-risk shape as the rejected hyphen gate,
   hence the eventual gate needs a second signal. Live-formatting-correctness finding too
   (`CommentClassifier` gates always live regardless of `gru-classifier`).
2. **[SEPARATE BUG, root-caused/fixed 2026-08-01 — see below]** Cluster of
   the 984 were DTD/URL string-literal fragments containing `//`, misread as
   comment openers by extraction.

**DONE — `explicit_vocab.txt` contamination filter.** User found personal/
jargon tokens (`Aloysius`, `Indrayanto`, `Red`, `LUTs`, `OLED`, `WIZnet`) as
"common word" vocab — `build_vocab.py` had no source-diversity requirement.
**Fix:** document frequency + raw count, rank `(doc_freq desc, raw_count
desc)`, `--min-sources` (default 2). Regenerated from 16-source run: 9684
eligible at `--min-sources=2`, fills 3346 common-word slots (154 keyword
slots unchanged); 5 contaminants gone, `Red` legitimately survives.
**Safety-window:** already-trained `code-formatter-ai-assist-weights.json`
unaffected by reordering `explicit_vocab.txt` — `GruWeights` embeds its own
`explicitVocab` snapshot in trained JSON, never re-reads on-disk file at
inference; regen only affects *next* training run's embedding-row layout.
`make test`: 220/220.

---

**2026-07-31 — `GruTrainer` break/resume checkpointing.** Before:
`bestWeightsJson` in-memory only; kill lost progress. **Two binary
checkpoints** from `--out` path: `<out>.ckpt-current.bin` (once/epoch — full
resumable: weights, vocab, Adam moments, scalar run state incl. RNG seed +
Adam step); `<out>.ckpt-best.bin` (on val-loss improvement — weights+vocab
only). Format: `DataOutputStream`/`DataInputStream`, header
(`magic=0x47525543`, `formatVersion=1`, `kind`) + shape-prefixed blocks,
temp-file-then-atomic-rename. Deleted on normal completion; `.gitignore`.
**`--resume=<checkpoint-path>`** early in `main`: `--lr`/`--epochs`/
`--patience`/`--seed` fall back to checkpoint unless overridden; vocab from
checkpoint snapshot; `random` re-seeded from checkpoint seed (exact
train/val split); epoch loop at `resumed.epoch + 1`. **Caveat (accepted, not
a bug):** only RNG seed persisted, not `java.util.Random` internal state —
initial split exact, per-epoch shuffle diverges after resume. **Testing:**
220/220 unchanged (non-shipped); `javac -source 8 -target 8` clean;
kill-and-resume (`kill -9` mid-epoch 3) recovered `epoch=2,
epochsSinceImprovement=0, bestValidationLoss=0.0604511`; loss continued
smoothly; final confusion `tp=79 fp=0 tn=1 fn=0 precision=1.00000`.

**2026-08-01 follow-up: `make gru-train` auto-resume.** Makefile never passed
`--resume` — interrupted checkpoint sat unused, next run restarted scratch.
Fixed: target checks `$(GRU_WEIGHTS_OUT).ckpt-current.bin`, auto-adds
`--resume=...` if present (`gru-train: found ..., resuming`);
`GRU_TRAIN_ARGS` still overrides checkpoint hyperparameters. No behavior
change absent checkpoint. User-verified end-to-end. Files:
`tools/gru/GruTrainer.java` + `.gitignore` (4 patterns); no `src/`.

**Commented-out-code NO-gate.** Trailing `;` + second independent code-shape
signal → `NO` (Gate 1d, between 1c and Gate 2); bare `;` alone unsafe (~8%
FP on prose). **`CommentedOutCodeGate.looksLikeCommentedOutCode(String)`**
requires ending `;` **and** ≥1 of: call-shape (`identifier(` no space),
assignment-shape (bare `=`, excl. `==`/`!=`/`<=`/`>=`), inc/dec-shape
(`++`/`--` adjacent word char), typed-declaration-shape (type-looking word +
identifier + `=`/`;`). New `CommentFeatureVector.looksLikeCommentedOutCode`
(13th ctor arg, whole-comment, not `targetWordIndex`-scoped). **Testing:**
220/220; 13 smoke cases correct (9 code → NO, 2 documented prose FP → YES, 2
prose unaffected); `sample_default.txt`: of 91064 YES ending `;` (1055),
gate fires NO on 739 (70%), ~200 inspected, zero real-prose FPs.

**Multi-line license/copyright-block NO-gate.** Gate 1e (between 1d and 2).
Naive "2+ newlines + not ending `.`/`!`/`?`" too blunt alone. **`LicenseBlockGate.
looksLikeLicenseBlock(String)`** requires **both**: (1) primary newline-span
signal, (2) copyright/license vocab anywhere (`Copyright`, `(C)`,
`SPDX-License-Identifier`, `Licensed under`, `All rights reserved`,
`Redistribution and use`, `Permission is hereby granted`, `WITHOUT
WARRANTIES`/`WARRANTY`). Decorative-border confirming signal **rejected** —
real corpus had hundreds of ordinary section-banners (e.g. `apache/ant`
`===...===`-framed XML headers). New
`CommentFeatureVector.looksLikeLicenseBlock` (14th ctor arg). **Testing:**
220/220; 9 smoke correct (4 real license → NO incl. project GNU-LGPL +
`STATE_COMMON.md` fixture header; project file header ending in period
correctly falls through YES — accepted intentional false-skip;
prose/TODO/single-mention/`?`-ending unaffected). Real corpus: vocab-or-
border fired 599/91064 YES with hundreds FPs; drop border → 375, ~60
inspected (css/java/xml/js/c/cpp) zero FPs. Both gates close "further
NO-producing gates" TODO.

**GRU retrain: 30.6% → 50.0%, still below 67.7% baseline.** After vocab fix,
two NO-gates, merge `classifier_weights/examples_*.md` into
`sample_default.txt` (92046 → 92308 lines, NO 975 → 3069), train
(`--threads=3 --epochs=3 --patience=2`, 73841 train/18460 val) early-stop
epoch 1 (val loss 0.0393061). 62-example hard-case bench:
`total=62 abstain=0 decided=62 correct=31 precision=0.5 yesCorrect=20/20
noCorrect=11/42`.

| | precision | YES correct | NO correct |
|---|---|---|---|
| Linear classifier (baseline) | 67.7% (42/62) | — | — |
| GRU, 2026-07-30 (pre-fix corpus) | 30.6% (19/62) | 19/19 | 0/43 |
| GRU, 2026-07-31 (post-fix corpus) | 50.0% (31/62) | 20/20 | 11/42 |

Progress (not all-YES) but below baseline — `gru-classifier` stays `off`.
**Why 50% / how to improve (open, no work done yet):** (1) 62 hand-labeled
tiny vs ~74k auto-labeled (~0.08%) — highest leverage is growing hand-labeled
hard-case set, not training mechanics; (2) consider oversampling/upweighting
hand-labeled rows (no such notion in `GruTrainer` today); (3) 62-example
bench has no held-out split — 50%/67.7% is training-fit not generalization;
(4) rule-based NO-gates cover "obviously not a sentence" end of NO, not
"ambiguous leading keyword" this bench targets; (5) architecture/training-
mechanics changes unlikely bottleneck (fast convergence + immediate overfit =
data problem) — not recommended before growing/reweighting hard-case corpus.

**Extended `classifier_weights` to every language reaching
`KeywordAmbiguityGate`; regenerated `sample_default.txt` (no retrain).**
Only call path: `MiscRuleCore.enforceCommentStyle` →
`FormatterCurly.java:272`, only curly-brace-family (`Lang.isCurly`).
**Conclusion: only c/cpp/java/kotlin/js/ts ever reach the gate** —
json/json5/css/yaml/toml/xml/html5/python3 structurally unreachable.
`KeywordAmbiguityGate.java`: js/ts had no dispatch, fell through to
`KEYWORDS_C` (wrong) — added `KEYWORDS_JS` (39)/`KEYWORDS_TS` (20 TS-only) +
dispatch. New `examples_js.md`/`examples_ts.md` (18 rows each); extended 4
existing (`c` +4, `cpp` +4, `java` +4, `kotlin` +3) for zero-coverage
keywords. `convert_classifier_weights_examples.py` `LANG_BY_STEM` + two
stems. **Golden-fixture fallout (expected):** `test/js_comments_inp.js`
`// class-level implementation note` now correctly lowercase (JS previously
no "class" keyword); fixture updated. `make test`: **221/221**
forward+idempotency. `make gru-acquire-corpus`: 113 hand-labeled (was 62)
folded into `sample_default.txt` (92336 lines: 89590 YES / 3081 NO); js/ts
landed (`js`=2465, `ts`=72). No training run.

**Linear classifier weights re-derived from extended (125-example) set.**
First attempt: `KEYWORD_BIAS` back positive (`+0.21890`) — new js/ts rows
zero-feature-YES-heavy. **Fix:** +6 zero-feature NO each to
`examples_js.md`/`examples_ts.md` (125 total). Re-derived:
`KEYWORD_BIAS=-0.08711, KEYWORD_WEIGHT_PAREN=-3.08818,
KEYWORD_WEIGHT_ARROW=-1.57140, KEYWORD_WEIGHT_SEMICOLON=-3.57490,
KEYWORD_WEIGHT_URL_OR_NUMBER=-0.93665`. 82/125 classified as labeled; 43
mismatches = accepted asymmetric-risk. `make jar` + `make test`: 221/221
forward+idempotency. **Still open (at this point):** `make gru-acquire-corpus`
rerun + GRU training against them, neither done this session. State then:
`gru-classifier` off (GRU 50% vs linear 67.7%); `comment-normalization-
classifier` on.

---

**2026-08-01 — `GruTrainer` mini-batch training (user-commissioned).**
Real mini-batching: `--batch-size=N` (new, default 16) averages N examples'
gradients before one Adam step; pre-existing `--threads=N` = parallelism
*within* one batch — orthogonal, same `computeBatch`/`ExecutorService`.
`averageGradients` accumulates onto first non-null gradient (`Gradients` ctor
package-private → mutate public fields), skips out-of-range-`targetWordIndex`
from sum and divisor. Adam `step` increments once per batch (bias-correction).
**Gradient clipping stays pre-average, per example** — why `--batch-size=1`
close to but not bit-identical to old per-example (documented README.txt).
`--batch-size` resumable hyperparameter; checkpoint format 1→2.
**Validation:** compile clean; `--check-gradients=5` →
`maxRelativeError=0.000001 (PASS)`; batch-size 1/8/16 all decreasing loss;
kill/resume restored `batchSize=4` without `--batch-size` on CLI. Confined
`tools/gru/GruTrainer.java`; `make test` not run (same scoping as
checkpointing). Not attempted: dropout, LR schedule, auto abstain-threshold
tuning — per 50%-session finding #5.

**`GruTrainer` learning-rate warmup + cosine decay (user-commissioned).**
Resumable `--warmup-steps=N` (default 0), `--lr-min=N` (default 0.0).
Step-granularity via Adam step counter (per mini-batch), not epoch. Decay
horizon reuses `--epochs` (`stepsPerEpoch * maxEpochs`, same on resume).
`computeScheduledLr` returns `baseLr` when `warmupSteps <= 0` — unmodified
invocation byte-identical to flat-lr. Formula step `s` in
`(warmupSteps, totalSteps]`: `lr = lrMin + 0.5*(baseLr-lrMin)*(1+cos(pi*
progress))`, `progress` clamped `[0,1]`. Checkpoint format 2→3.
**Validation:** compile clean; `--check-gradients=5` PASS; no
`--warmup-steps` held LR flat (true no-op); schedule-enabled matched
hand-computed values (step 3 = baseLr; step 60 = lrMin); kill/resume
continued decay smoothly. Confined `tools/gru/GruTrainer.java`; `make test`
not run. Not attempted: dropout, auto abstain-threshold tuning.

**GRU retrain on 125-example bench: 50.0% → 56.0%, still below 67.7%.**
Post-mini-batch/post-schedule train (`--threads=3 --epochs=5 --patience=3`,
schedule off, interrupted mid-epoch-2 + resumed): 73873 train/18468 val.
Best epoch 3 (`validationLoss=0.0356914`); epochs 4-5 discarded. `GruEval`
on 125 hand-labeled: `total=125 abstain=0 decided=125 correct=70
precision=0.56 yesCorrect=40/43 noCorrect=30/82`.

| | precision | YES correct | NO correct |
|---|---|---|---|
| Linear classifier (baseline) | 67.7% (82/125) | — | — |
| GRU, 2026-07-31 (62-example bench, pre-mini-batch) | 50.0% (31/62) | 20/20 | 11/42 |
| GRU, 2026-08-01 (125-example bench, post-mini-batch+schedule) | 56.0% (70/125) | 40/43 | 30/82 |

`noCorrect` 11/42 (26%) → 30/82 (37%). Still below baseline —
`gru-classifier` stays `off`. 62-vs-125 bench size differs — 50%→56%
directionally informative not clean control (more hand-labeled rows as
plausible as trainer changes; not isolated). Held-out caveat still applies.
**Decision (per user):** next = grow hand-labeled hard-case corpus, not
further trainer-mechanics.

**Root-caused/fixed string-in-comment `extract_comments.py` bug** (DTD/URL
leakage flagged open twice above). **Root cause, isolated to
`extract_comments.py`'s `extract_c_style_comments`**
(`GenerateSampleDefault.java` exonerated): scan treated `"//"`/`"/*"` as
openers anywhere in raw source, no string/char-literal awareness. **Fix:**
third mutually-exclusive span — `"`/`'`-delimited literals, consumed to
matching unescaped quote or EOL, skipped without `//`/`/*` check inside;
backslash-escaped quotes via 2-char skip. **Verified** standalone smoke (not
in `make test`): fixture with exact DTD-string shape, `'/'` char lit,
`"https://..."` + real trailing `//`, block comment with quoted `"// string"`,
escaped-quote string — extracted exactly 4 real comments, zero leakage;
pre-existing `///*mlen = n;` still correct. Files:
`tools/gru/extract_comments.py` only. `sample_default.txt` NOT regenerated
yet at session end (done later below).

**Grew hand-labeled hard-case set: 125 → 173 rows.** Audited
`KeywordAmbiguityGate` six per-language keyword lists vs each
`examples_*.md` for zero-coverage keywords (also ordinary frequent English).
**8 new rows/file (48 total)**, zero-mechanical-feature YES-prose/NO-code
pairs:

| File | Keywords targeted | Rows added |
|---|---|---|
| `examples_c.md` | `case`, `const`, `for`, `return` | 22-29 |
| `examples_cpp.md` | `catch`, `override`, `public`, `protected` | 20-27 |
| `examples_java.md` | `case`, `if`, `public`, `record` | 23-30 |
| `examples_kotlin.md` | `as`, `fun`, `if`, `return` | 16-23 |
| `examples_js.md` | `case`, `delete`, `throw`, `while` | 25-32 |
| `examples_ts.md` | `any`, `never`, `number`, `public` | 25-32 |

TSV: `wrote 173 hand-labeled example(s)` — 125+48 exact. **Not done this
session (out of scope):** `derive_weights.py`, `CommentClassifierWeights.java`/
`weights.md` re-derivation, `make gru-acquire-corpus`, GRU retrain — done next
session.

**`derive_weights.py`'s `DATASET` auto-extending from `examples_*.md`.** Latent
sync bug: `DATASET` was hand-transcribed Python mirror, not parsed from files
— 48 new rows had no entries. **Fix:** `load_dataset()` parsing
`examples_*.md` (same header-column-lookup as
`convert_classifier_weights_examples.py`; `LANG_BY_STEM` duplicated not
imported). **Verified:** all 125 prior rows identical (spot-check); all 173
current with zero manual transcription. Full 173-row: precision 106/173
(61.3%, down from 82/125=65.6% — expected dilution from harder rows, not
parser regression). New constants: `KEYWORD_BIAS=-0.05634,
KEYWORD_WEIGHT_PAREN=-3.10644, KEYWORD_WEIGHT_ARROW=-1.55819,
KEYWORD_WEIGHT_SEMICOLON=-3.59572, KEYWORD_WEIGHT_URL_OR_NUMBER=-0.96329`.
Files: `tools/classifier_weights/derive_weights.py` only.

**`weights.md`/`CommentClassifierWeights.java` re-derived; `sample_default.txt`
regenerated with `extract_comments.py` fix.** (1) 173-example constants →
`CommentClassifierWeights.java` + "2026-08-01 re-derivation" in `weights.md`.
`make test`: **225/225** forward+idempotency. (2) `make gru-acquire-corpus`
with string-literal fix: 96836 → 96695 raw comments (141 fewer spurious),
auto-labeled NO 3103 → 3023 (80 fewer, leaked DTD/URL now excluded).
`sample_default.txt`: 92348 → 92952 lines; zero remaining leakage
(`grep -c "DTD Enterprise JavaBeans"` → 0). 173 hand-labeled folded
unchanged. **Not done:** GRU retrain / hard-case bench re-run.

**Grew hand-labeled further: 173 → 221 rows.** Same process, next
zero-coverage keywords:

| File | Keywords targeted | Rows added |
|---|---|---|
| `examples_c.md` | `if`, `long`, `else`, `switch` | 30-37 |
| `examples_cpp.md` | `friend`, `throw`, `try`, `using` | 28-35 |
| `examples_java.md` | `break`, `catch`, `finally`, `package` | 31-38 |
| `examples_kotlin.md` | `break`, `do`, `else`, `in` | 24-31 |
| `examples_js.md` | `break`, `catch`, `if`, `return` | 33-40 |
| `examples_ts.md` | `declare`, `is`, `protected`, `string` | 33-40 |

Each batch exactly 4 zero-feature YES + 4 zero-feature NO (balanced — can't
repeat KEYWORD_BIAS-flip alone). TSV: `wrote 221 hand-labeled example(s)` —
173+48 exact. **Not done this session (out of scope):** `derive_weights.py`,
`make gru-acquire-corpus`, GRU retrain — done next session.

**Re-derived weights + regenerated corpus for 221-row set.** `KEYWORD_BIAS`
stayed negative (`-0.04180` vs `-0.05634` at 173); four feature weights within
few percent of 173-row — boundary stable across four growth passes. 130/221
classified as labeled (58.8%, down from 61.3% at 173 — expected dilution).
Updated `CommentClassifierWeights.java`/`weights.md` "second growth pass,
same day". `make test`: 225/225. `make gru-acquire-corpus`:
`sample_default.txt` 92809 lines, all 221 hand-labeled. **Not done:** GRU
retrain / hard-case bench (done 2026-08-02 below).

---

**2026-08-02 — hot-path fused-gate refactor (no measurable speedup); float vs
double REJECTED.** User-commissioned perf. `GruClassifier.java`
`forward`/`backward` already almost entirely flat `double[]`/`double[][]` —
remaining waste: each GRU gate (`z`, `r`, `hTilde`) via 4-call chain
(`matVecInto` x2 + `addVecInto` + `sigmoidVec`/`tanhVec`).

**Benchmark:** synthetic 288-train/72-val (session-scratch only, never
committed, never touches `sample_default.txt`), 6 languages,
`--threads=1 --batch-size=1 --epochs=8 --patience=8 --seed=1`. **Baseline:**
epoch 1 (JIT) 3.9s, epochs 2-8 steady 3.6-3.7s/epoch (~12.5ms/example);
total 32.8s.

**Fused-gate:** new `GruClassifier.gateInto(...)` — one flat loop per output
row, exact original per-element op order (bit-identical). Applied both
forward/backward biGRU three gates; removed dead `matVecInto`, `addVecInto`,
`sigmoidVec`, `tanhVec`. **Correctness:** `--check-gradients=8` →
`maxRelativeError=0.000000 (PASS)`; identical baseline command — every epoch
loss matched all printed digits (final `tp=27 fp=0 tn=45 fn=0` both runs).
**Result: no measurable speedup** — epochs 2-8 still 3.6-3.7s, 32.8s total
(≈1.00x). At hidden=224/embedding=16, O(h²)/O(h·e) already dominated; removed
O(h) chain overhead within noise. **Kept anyway** (fewer allocs, zero behavior risk,
bit-identical, gradient-check-verified) — not reverted. `make test`: 225/225. Committed:
`src/com/jxmake/formatter/classifier/gru/GruClassifier.java`.

**Float vs double — evaluated and REJECTED (kept double end-to-end).** (a)
Baseline `GruEval` committed weights on 221-row hard-case, double:
`total=221 abstain=0 decided=221 correct=119 precision=0.5384615384615384
yesCorrect=88 yesIncorrect=3 noCorrect=31 noIncorrect=99`. (b) Converted
`GruWeights.java`/`GruClassifier.java` weight *storage* to float (JSON
`Double.parseDouble` scan unchanged — narrow at array-construction). (c)
Identical `GruEval` float-typed: **byte-identical** decision-for-decision to
double — zero accuracy impact float32 storage. (d)/(e) Trainer-to-float
surfaced coupling: `GruClassifier.Gradients` reuses
`GruWeights.DirectionWeights` as field type — converting forces mini-batch
grad-accum/Adam math into float, requiring split into float storage + double
grad-accumulator structs and updating every trainer call site (Adam moments,
grad clip, mini-batch avg, `--check-gradients`, checkpoint version bump).
**Decision: reverted float conversion in full, kept double end-to-end**
(`GruWeights.java`/`GruClassifier.java` restored — `git diff` byte-identical
pre-session). Scope/risk call not accuracy (impact exactly zero); "just the
trainer" not bounded; float storage alone revisitable later as narrow task.
No commit for float conversion. `make test` 225/225 after revert.
`code-formatter-ai-assist-weights.json` byte-unchanged (only read via
`GruEval`).

**`make gru-train` re-run (default `GRU_TRAIN_ARGS`), 221-example bench:
56.0% → 65.2%, still below 67.7%.** Defaults (`--threads=3 --epochs=9
--patience=3 --batch-size=16 --progress-every=1000`) on `sample_default.txt`
(74280 train/18570 val, vocabSize=3500). Early-stop epoch 5, best epoch 2
(`trainLoss=0.0368411, validationLoss=0.0322115`). Val confusion
(auto-labeled split, not hard-case):
`tp=17913 fp=105 tn=512 fn=40 precision=0.99417 recall=0.99777 f1=0.99597`.
`GruEval` on 221-row hand-labeled:

| weights | precision | correct/total | yesIncorrect | noIncorrect |
|---|---|---|---|---|
| previously committed (pre-retrain baseline) | 53.8% | 119/221 | 3 | 99 |
| freshly retrained (this run) | **65.2%** | 144/221 | 0 | 77 |

Best 221-example precision so far (progression: 30.6% → 50.0% → 56.0% →
65.2%). `yesIncorrect` → 0; `noIncorrect` (77/91 NO→YES) still dominant gap
vs 67.7% linear baseline (RDD_EXT_17's bar). No code change — corpus/hyper
re-run only. `gru-classifier` stays `off`. Weights overwritten by
`make gru-train` (user action); pre-retrain copy + 221-row bench tsv
scratch-only per RDD_EXT_19.

**User-retrained weights: 65.2% → 98.7%, clears 67.7%/90% bars.** User re-ran
`make gru-train` (weights changed per `git diff` — fresh retrain). `GruEval`
on current `make gru-hand-labeled-examples` bench (now 474 rows — set grew
past 221 interim; not strict apples-to-apples vs 65.2%/221):
`threshold=0.5 total=474 abstain=1 decided=473 correct=467
precision=98.73% yesCorrect=91/91 noCorrect=376/382`. First measurement to
clear both RDD_EXT_17's 90% bar and 67.7% linear baseline. Decision: flip
`gru-classifier` default `on` + `make test` (next).

**`Config.gruClassifier` default flipped `false` → `true`.** `make build` +
`make test`: **228/228 forward, 228/228 idempotency** — clean. `gru-classifier`
now `on` by default alongside `comment-normalization-classifier` (on since
2026-07-30). Weights = user-retrained file above (98.7%/474 rows).

**REVERTED same day — 98.7% was training-fit, not held-out; corrected via
5-round CV.** 7 misses on 474-row on-training eval: no single fixable
mechanical shape (all 6 wrong-decisions `NO`→`YES`; 4/7 clustered on Kotlin
`this`/`object`/`is` meta-keyword-discussion sentences — genuine semantic
ambiguity, not gate-able). Ran `tools/gru/cross_validate.py` (5 rounds,
80/20, retrain from scratch per round on 474-row hand-labeled only,
`--epochs=40 --patience=6`):

```
round 0: precision=87.2% (82/94, 1 abstain)
round 1: precision=84.8% (78/92, 3 abstain)
round 2: precision=88.4% (84/95, 0 abstain)
round 3: precision=85.9% (79/92, 3 abstain)
round 4: precision=85.1% (80/94, 1 abstain)
mean=86.3%  stdev=1.5%  min=84.8%  max=88.4%
```

**True held-out precision ~86.3%, below RDD_EXT_17's 90% bar** — 98.7%
overstated generalization (production corpus folds same hand-labeled rows
in directly). 86.3% beats 67.7% forced-linear baseline and naive always-`NO`
(~80% raw on this class balance; trivially safe: 0% FP, 100% missed YES) on
raw precision — but risk-relevant number is FP rate, not raw precision
(asymmetric-risk: false skip = zero cost, false positive = visible bug,
RDD_EXT_11). Aggregate 5 rounds: `yesCorrect=61/99 (62%) yesIncorrect=38/99
noCorrect=342/368 noIncorrect=26/368 (7.1%)` — GRU-on resolves 62% of
genuinely ambiguous YES/prose correctly at **7.1% false-positive rate** on NO
cases, vs 0% under always-abstain-through default. **Decision: reverted
`Config.gruClassifier` back to `false`.** Not rejection of progress (62%
YES-resolution from 0% is real) — 90% bar is conservative proxy for
"FP rate low enough to trust automatically," and 7.1% doesn't clear it.
`make build` + `make test`: 228/228 after revert.
**Threshold sweep, same day — re-enabled at `abstainThreshold=0.7`.**
Followed the open item above: swept `GruEval` optional threshold args (0.5/0.6/0.7/0.8/0.9) against 5
already-trained CV rounds' held-out sets (no retrain — `GruEval` caches
forward-pass probs, re-evaluates boundary free):

| threshold | YES resolved (of decided) | NO false-positive rate |
|---|---|---|
| 0.5 | 61/99 = 61.6% | 26/368 = 7.1% |
| 0.6 | 51/77 = 66.2% | 17/354 = 4.8% |
| **0.7** | **43/65 = 66.2%** | **9/336 = 2.7%** |
| 0.8 | 29/47 = 61.7% | 4/320 = 1.3% |
| 0.9 | 19/27 = 70.4% | 2/304 = 0.7% |

0.7 cuts FP 7.1%→2.7% at essentially no YES-resolution cost (still 66.2%) —
better trade-off than trained default 0.5; pure inference-time cutoff, no
retrain. **Applied:** `code-formatter-ai-assist-weights.json`
`abstainThreshold` `0.5` → `0.7` (only that field; weight arrays untouched —
pre-change copy scratch-only per RDD_EXT_19). Re-verified `GruEval`
production weights + 474-row: `abstain=4 decided=470 correct=467
precision=99.4% noIncorrect=3` (down from `noIncorrect=6` at 0.5; non-held-
out, directionally consistent). **`Config.gruClassifier` flipped back
`false` → `true`** now that FP at this threshold materially better than 7.1%.
`make build` + `make test`: **228/228 forward, 228/228 idempotency**.
`gru-classifier` now `on` by default at `abstainThreshold=0.7`, alongside
`comment-normalization-classifier` (on since 2026-07-30).
**Confirmed same day — fresh from-scratch CV at 0.7.** Added
`cross_validate.py --eval-threshold` (fixed threshold through each round's
`GruEval` instead of freshly-trained weights' own `abstainThreshold`); full
5-round from scratch (`--work-dir /tmp/gru_cv_07`, `--eval-threshold 0.7`,
same deterministic seeds — fresh `GruTrainer` runs, not reuse of earlier
weights):

```
round 0: precision=92.7% (76/82, 13 abstain)
round 1: precision=92.0% (69/75, 20 abstain)
round 2: precision=91.2% (83/91, 4 abstain)
round 3: precision=95.8% (68/71, 24 abstain)
round 4: precision=90.2% (74/82, 13 abstain)
mean=92.4%  stdev=2.1%  min=90.2%  max=95.8%
```

Matches earlier reused-probabilities sweep exactly (aggregate:
`yesCorrect=43/65=66.2% noIncorrect=9/336=2.7%`) — 0.7 pick generalizes to
genuine from-scratch retrain. `abstainThreshold=0.7` stands as production
default. `tools/gru/README.txt` documents `--eval-threshold`.

---

**2026-08-02 (later) — grew hand-labeled hard-case set targeting
miss-inspection pattern: 474 → 522 rows.** Every held-out error was known
`NO`→`YES` mode, clustered on meta-keyword-discussion sentences (describing
a keyword's code meaning while reading as fluent non-templated English —
`this`/`object` in Kotlin worst, 4/7 misses). Prior growth covered mainly via
repeated `"<keyword> here <verb>..."` template; this pass targeted
**naturalistic, non-templated phrasing**, 8 rows/file (4 NO meta-discussion /
4 YES ordinary-English, balanced anti-KEYWORD_BIAS-flip), 48 total across six
`examples_*.md`:

| File | Keywords targeted | Rows added |
|---|---|---|
| `examples_c.md` | `static`, `return` | 77-84 |
| `examples_cpp.md` | `this`, `static` | 91-98 |
| `examples_java.md` | `this`, `static` | 93-100 |
| `examples_kotlin.md` | `this`, `object` | 68-75 |
| `examples_js.md` | `this`, `class` | 82-89 |
| `examples_ts.md` | `this`, `interface` | 69-76 |

`make gru-hand-labeled-examples`: `wrote 522 hand-labeled example(s)` —
474+48 exact. **Not done this session (explicitly out of scope, per user):**
`derive_weights.py` re-run, `CommentClassifierWeights.java`/`weights.md`
re-derivation, `make gru-acquire-corpus`, GRU retrain — deferred later
session/user's own retrain.

**`GRU_TRAIN_ARGS` updated: `--epochs=9 --patience=3` → `--epochs=20
--patience=5`.** 2026-08-01 production run (65.2%→98.7%→86.3% held-out
progression above) early-stopped epoch 6 without exhausting budget —
patience=3 tight enough to cut off runs that might improve past short
plateau. Widened: more epochs headroom (9→20) + more patience (3→5),
consistent with RDD_EXT_18's original 20-50-epoch starting guidance.
`Makefile` only; no training this session per user ("do not retrain, I will
do that later").

**`GRU_HAND_LABELED_REPEAT` left at 3, not increased.** User asked whether to
raise alongside epochs/patience. Recommendation: no — training-fit/held-out
gap this session (98.7% training vs 86.3% held-out, same hand-labeled rows)
is direct symptom of over-weighting those rows; raising repeat would fit exact
hand-labeled sentences harder, not generalize pattern, risks widening gap.
Leave at 3 unless future CV (after this corpus growth folded in) still misses
this pattern — then revisit.

**User re-ran `make gru-acquire-corpus`** to fold grown 522-row hand-labeled
into training corpus; retrain deferred to user's session tomorrow
(2026-08-03). No other changes this turn.

---

**2026-08-03 — linear classifier re-derived against 522-row set; compared
against user's fresh GRU retrain.** `derive_weights.py` not re-run since
221-row set (2026-08-02); re-ran against current 522 rows across six
`examples_*.md`:

```
KEYWORD_BIAS                  = -1.18218
KEYWORD_WEIGHT_PAREN          = -2.17830
KEYWORD_WEIGHT_ARROW          = -0.64725
KEYWORD_WEIGHT_SEMICOLON      = -2.66553
KEYWORD_WEIGHT_URL_OR_NUMBER  = -0.03338
```

407/522 (77.97%) classified as labeled, same accepted asymmetric-risk
mismatch pattern. Copied into `CommentClassifierWeights.java` +
`tools/classifier_weights/weights.md`. `make jar` + `make test`: **228/228
forward, 228/228 idempotency**.

**GRU comparison.** User trained overnight on CM5 (`gru_log.txt`,
`GRU_TRAIN_ARGS` back to `--epochs=9 --patience=3` per user's own Makefile
edit this session — prior runs incl. 2026-08-02 widened `--epochs=20
--patience=5` consistently plateaued by epoch 3; confirmed again:
`bestValidationLoss=0.0321558` at epoch 3, early-stopped epoch 8 after 5
epochs no improvement). `GruEval` (`tools/gru/GruEval.java`, compiled ad
hoc, not part of `make test`) on current 522-row
`make gru-hand-labeled-examples` bench:

| | precision | correct/total |
|---|---|---|
| Linear classifier (freshly re-derived, 522 rows) | 77.97% | 407/522 |
| GRU, fresh retrain, threshold=0.5 | 99.43% | 519/522 |
| GRU, fresh retrain, threshold=0.7 | 99.81% | 518/519 (3 abstain) |

**Caveat (same as every prior on-benchmark GRU figure):** training-fit, not
held-out — 522 hand-labeled rows folded directly into `sample_default.txt`
(with repeat oversampling) that GRU just trained on, same shape as
98.7%-vs-86.3% gap on 2026-08-02. Not directly comparable to linear's
77.97%, which *is* genuine same-set fit (linear isn't trained against
`sample_default.txt`). Fair GRU-vs-linear needs fresh `cross_validate.py` on
grown 522-row set — **not done this session** (not requested; 5-round CV at
this corpus size runs many hours per `gru_log.txt` epoch timings,
~792s/epoch). `code-formatter-ai-assist-weights.json`'s `abstainThreshold`
reset to `0.5` by this retrain (previously-applied `0.7` override from
2026-08-02 threshold sweep not carried over — flagged in case user wants to
reapply before weights go live).

**`abstainThreshold` restored to `0.7`, both in trainer default and committed
weights file.** Confirmed (per user question): `abstainThreshold` is pure
inference-time metadata — `GruTrainer` training loop (loss/gradients/
early-stopping) never reads it; written once verbatim into output JSON
(`GruTrainer.java` build call around line 1396). So 0.5-trained and
0.7-trained with same seed produce byte-identical weight/embedding arrays,
differing only that one field — no retrain needed to change it.
`GruTrainer.java`'s `ABSTAIN_THRESHOLD` constant `0.5` → `0.7` (future
`make gru-train` bakes in `0.7` by default); committed
`code-formatter-ai-assist-weights.json` `abstainThreshold` hand-edited
`0.5` → `0.7` (single-line `sed`, diffed — no other byte changed).
`make jar` + `make test`: 228/228 unchanged. `GruEval` with no explicit
threshold arg (reads file's own trained value): `threshold=0.7 total=522
abstain=3 decided=519 correct=518 precision=99.81%` — file round-trips at
0.7 correctly.

**README.md gained "Comment classifier (GRU)" prose subsection** (between
Configuration properties table and "`.jxmake-code-formatter` inheritance") —
feature previously only had config-table comment line. Explains rules-then-
GRU pipeline, fail-safe posture, states `abstainThreshold = 0.7` explicitly
with FP-rate rationale (links this file for full CV history).
`gru_classifier = on` already shipped default since 2026-08-02 — no code
change to "make it live", only this documentation gap.

**`gru_log.txt` deleted** (`git rm`'d — had been committed by user alongside
prior weights update, outside normal workflow; RDD_EXT_19: real training
artifacts/logs never committed, scratch-only).

**Formatter self-formatting (dogfood-and-adopt) re-run**, prompted by user
flagging `CommentClassifierWeights.java` hand-edited negative literals
(`= - 1.18218`) as legacy unformatted style. Debugging why self-formatted
trial jar disagreed with `target/code-formatter-1.00.jar` on 6
comment-classifier-sensitive fixtures (`c_comments`, `cpp_modern`,
`cpp_comments`, `java_comments`, `js_comments`, `real_code_regressions_54`):
**root cause:** `GruAbstainResolver` resolves default weights path to *jar's
own parent directory* (`target/`, since `JAR_FILE =
target/code-formatter-1.00.jar`), but `make gru-train` only ever copied
trained weights to `$(CLASS_DIR)` (`target/classes`) and repo root — never
`target/` itself. So `make test`/`_test_serial` against real shipped jar had
been silently running with GRU permanently fail-safe-ABSTAINed (rule-based-
only fallback); 6 fixtures last hand-verified against GRU-inactive behavior.
**Fix:** auto-copy step on `_test_serial` in `Makefile` (copies repo-root
`code-formatter-ai-assist-weights.json` into `$(BUILD_DIR)` if not already
there) so `make test` exercises GRU for real. Re-verified 6 disagreeing
fixtures' new (GRU-active) output by hand — all legitimate sentence-start
capitalizations GRU newly resolves correctly (e.g. `/* inline on case */` →
`/* Inline on case */`, `// default case` → `// Default case`,
`// if constexpr` → `// If constexpr`) — updated those 6 `test/*_out.*`
fixtures. `make test`: 228/228 clean with GRU genuinely active. Documented
GRU-active-vs-inactive behavioral difference (and auto-copy) in README.md
"Comment classifier (GRU)" subsection so future missing-weights setup doesn't
silently pass `make test` while under-testing GRU path again.

With corrected baseline, completed full self-formatting dogfood-and-adopt
end to end: round1/round2 idempotent, trial jar from round1 passed
`make _test_serial` 228/228, round1b/round2b fixed-point check against
original `src/` confirmed (round1≡round1b, round2≡round2b), round1 adopted
into `src/` (37 files — all diffs spot-checked cosmetic: unary-minus spacing,
missing binary-operator spacing, declaration-alignment padding, line-wrap
reflow, comment capitalization from now-active GRU), rebuilt, and
`make test` / `make test-server` / `make bench` all clean against adopted
tree. Also parallel `tools/gru`/`tools/verifiers` self-formatting
(round1/round2 idempotent, `java_syntax_check` clean on orig/r1/r2,
`java_content_diff` confirmed AST-equivalent content orig-vs-r1 and r1-vs-r2
for all 9 `tools/gru` + 4 `tools/verifiers` files); adopted round1 into both
dirs (2 of 9 `tools/gru` and 2 of 4 `tools/verifiers` actually changed; rest
already formatter-clean).

---

**GRU weights loaded once per process, not once per comment (user-commissioned
efficiency fix).** `GruAbstainResolver.resolve` previously called
`GruClassifier.load(weightsPath)` -- a fresh read+parse of the weights JSON --
on every single rule-based-ABSTAIN comment/target-word, in both multi-file
batch runs and server mode. **Fix:** `GruAbstainResolver.CLASSIFIER_CACHE`, a
`ConcurrentHashMap<Path, Optional<GruClassifier>>` keyed by the resolved
weights path; `loadCached` uses `computeIfAbsent` so the weights file is
loaded at most once per distinct path for the process's lifetime, shared
safely across concurrent server-mode requests (`computeIfAbsent` makes the
single load thread-safe without an explicit lock). A failed load (missing/
corrupt file) is cached too (as `Optional.empty()`), so it isn't retried per
comment -- same "attempt once, fail-safe ABSTAIN forever after" posture as
`RDD_EXT_9`'s AI-decision-client endpoint-dead cache. **Design note (why not a
literal `GruClassifier` singleton):** `gru-weights-path` is a per-invocation/
per-request config override, so the cache is keyed by weights path rather
than making `GruClassifier` itself a single global instance -- a real
classic singleton would either ignore path overrides or need the same
keyed-cache machinery anyway, just relocated. `GruClassifier` itself stays an
ordinary immutable class; nothing about its own code needed to change.
Confined to `GruAbstainResolver.java`. `make test`: 238/238 forward +
238/238 idempotency, unchanged.
