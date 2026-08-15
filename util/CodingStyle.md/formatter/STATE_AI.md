# STATE_AI.md — AI-Assist Design Reference and GRU Job State

This file documents the background/architecture for the JAR's built-in
`ai-assist` feature. (No dogfood corpus for this job — see
`STATE_DOGFOOD.md`'s note.)

- **Step 2** (argument-layout / getter-setter-grouping) — **NOT FEASIBLE**,
  permanently, reference-only, no active work. No tractable grouping-intent
  signal exists for the JAR to hand an LLM, at any model size tested.
- **Step 3** (comment-classifier abstain-case resolution) — **FEASIBLE, GRU
  only**, the active tracked job per `CLAUDE.md`'s job table
  (`com.jxmake.formatter.classifier.gru`), following the same
  `STATE_COMMON.md` conventions as every other job. A narrow classification
  decision, not a layout-authorship judgment call — feasible only via a
  purpose-trained bidirectional GRU; the small-instruction-tuned-LLM variant
  is **NOT FEASIBLE** (tested and failed; see below).

---

## Step 2 — AI Integration: NOT FEASIBLE (deferred)

The JAR cannot distinguish meaningful author-expressed argument grouping from
arbitrary line breaks — the core prerequisite for reliable AI candidate
selection — so a small on-device model (3B–7B) has no reliable basis for
choosing between candidates. Permanent behavior when inline exceeds 100
chars is therefore the mechanical fallback (dropped form if args fit on one
indented line, one-per-line otherwise). The architecture (grammar-constrained
single-token response via `/v1/chat/completions`, candidate layout
generation, fail-safe fallback) stays valid/reusable if a grouping-intent
heuristic or a larger model (7B+) is proven reliable in future. Tier-3
aesthetic decisions (argument layout, non-standard getter/setter grouping)
are instead handled by the capable-AI workflow in `README.txt` /
`AI_PREAMBLE_AESTHETIC.md`.

Checklist — Step 2 (all NOT FEASIBLE, no implementation needed): `Config.java`
ai-assist keys, `AiDecisionClient.java`, `AI_DECISION_PROMPT.md`,
`MiscRule.java` Tier-3 AI hooks — all NOT FEASIBLE. `README.md` ai-assist
section — DONE (removed/replaced).

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
  template automatically — portable across llama.cpp/Ollama/vLLM/LM Studio),
  `temperature = 0.0`.
- Model never rewrites source text — JAR executes the chosen layout
  mechanically, invoking AI only when a genuine multi-candidate choice exists.

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
or part of a token) that a small **purpose-trained** classifier (the GRU's
~500k-parameter footprint) can plausibly handle, unlike a small
instruction-tuned LLM (confirmed NOT FEASIBLE, see below). Builds on the
already-implemented rule-based comment-grammar classifier (Task H in
`STATE.md`, `RDD_KEY_94`–98): `CommentFeatureExtractor`/`CommentFeatureVector`,
`NonLatinScriptGate`, `KeywordAmbiguityGate`, `CommentClassifier`/
`CommentClassifierWeights` (`YES`/`NO`/`ABSTAIN`), gated behind
`comment-normalization-classifier` (defaults `on` since the 2026-07-30
KEYWORD_BIAS fix, see below). Reuses Step 2's confirmed architecture pattern
(grammar-constrained short response, `temperature=0.0`, fail-safe fallback,
`RDD_EXT_9` caching) — only the small-LLM variant is NOT FEASIBLE, not the
pattern itself.

**Small-LLM classifier fallback: NOT FEASIBLE (confirmed by testing).** Small
instruction-tuned models (1B–3B class) cannot reliably tell whether a word at
the start of a sentence is plain English prose or a language keyword —
exactly the `KeywordAmbiguityGate`/Step 3 task. Tested and failed: Qwen
(1B–3B), Qwen2.5-Coder (1B–3B), Gemma (1B–3B); not tested but not expected to
fare better: Llama 3B. **A small on-device LLM will not be used for Step 3,
full stop** — not as v1, not as a fallback behind the GRU, not for
non-Latin-comment routing. The bidirectional GRU is the only Step 3 approach
going forward. Doesn't reopen Step 2, doesn't rule out a larger model (7B+)
— untested, no such path currently designed.

**Model size determination:** a bidirectional GRU with ~500k parameters is
the best accuracy/latency/footprint balance for this narrow classification
decision. Bidirectional because the full comment text is available upfront
(not streamed) — only ~2x encoding compute, no autoregressive-latency
downside. Pipeline: rules first (high confidence → done; abstain → GRU
classifier → final decision). If GRU accuracy proves insufficient, next step
is a fresh design discussion (larger model/different hyperparameters), not a
revival of the rejected small-LLM fallback.

**Non-Latin comments:** `RDD_KEY_95`'s `NonLatinScriptGate` disables the
rule-based classifier entirely (≡ `ABSTAIN`) for any comment with a
non-Latin codepoint, deferring to the full-file AI pass. Closed, not
unstarted: depended on the small-LLM fallback's multi-language
understanding, which is NOT FEASIBLE — no Step 3 LLM branch exists to route
to; `RDD_KEY_95`'s behavior stands unchanged. A GRU trained specifically on
non-Latin/mixed-language examples would be a distinct, unexplored idea.
**Disposition (2026-08-10): assessed, not planned.** A dedicated non-Latin
GRU would need its own training corpus (no dogfood corpus for any non-Latin
script exists in this project), its own weights file, and a second model to
load/maintain at runtime — cost disproportionate to the benefit, since the
underlying decision (leading-keyword/trailing-period English-prose-vs-code
ambiguity) mostly doesn't apply to non-Latin text anyway. Documented in
`README.md`'s Known Limitations → new "AI-assist (GRU)" family section.

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
extends the `tools/classifier_weights/` pattern from `RDD_KEY_97`): don't
pre-commit to a total corpus size before measuring real ABSTAIN rate (done,
see below). Pre-filter every extracted comment through `CommentClassifier`
first — high-confidence YES/NO resolved for free, `ABSTAIN` is the real
labeling target. **Two pools:** Pool A (keyword-ambiguity) — large pool,
targeted extraction toward short comments (≤6-8 words) containing a known
keyword. Pool B (period-ambiguity) — small pool, punctuation-discussion
comments and abbreviation patterns (RDD_EXT_15's grep filter). Sources: own
dogfooded repos first (RDD_EXT_16), then vetted permissive public repos.
Labeling: Pool A via frontier-model labeling + spot-check; Pool B by hand
(small, easy call). Verification: flag mislabels via rule-based/label
disagreement and held-out regressions, correct in place with a why-note.

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

## CANCELED — Comment sentence-boundary detection defeated by mid-word dots (Step 3 candidate)

**2026-08-11 — permanently closed, decided against (not deferred).** Owner
decision: the task-separation path (a second model/weights file, or a
task/schema field added to the shared abstain-resolution pipeline) will NOT
be pursued. Reason: `MiscRuleCore.classifyComment`
(capitalize-first-letter / strip-trailing-period) routes through the same
`GruAbstainResolver.resolve`/trained weights as the main Step 3 "is this a
real explanatory comment" job — no separate model or `task` dimension exists
in the RDD_EXT_20/21 schema (`lang`/`label`/`targetWordIndex`/`escaped-text`)
to distinguish the two. Training "does this trailing dot end a sentence, vs.
sit mid-token (`.hpp`, `e.g.`, `v1.0`)?" into the same label column the main
job uses for "is this comment substantive prose vs. noise?" risks degrading
the main job's 92.4%+ mean held-out precision — that cross-cutting
correctness risk outweighs the value of closing this narrow a gap. Item is
dead; do not revisit without a new explicit owner decision. The mechanical
rule limitation (`dotCount != 1` → leave as-is) remains permanent behavior.
Original problem statement preserved below for reference.

`MiscRule.stripSoleTrailingPeriod` (§15) strips a comment's trailing `.` only
when it's the *sole* `.` in the text — conservative, to avoid mangling an
ellipsis or an abbreviation followed by more text. Misfires when the comment
has an unrelated earlier dot that isn't a sentence-ender, e.g. (C++ `//`
form; same problem class applies to any comment syntax, per
AI_PREAMBLE_FULL.md §15):

```
// Combined .hpp test: pragma once, concepts, templates, classes, extern C.
```

`.hpp` and the trailing `C.` both count as dots, so `dotCount != 1`, leaving
the genuinely sentence-ending period in place (expected: stripped).
Distinguishing a mid-word/mid-token dot from a true sentence-ending dot is a
natural-language judgment call with no tractable mechanical heuristic —
permanently accepted as this limitation (`dotCount != 1` → leave as-is); see
the 2026-08-11 disposition above for why the GRU task-separation fix isn't
being pursued.

---

## OPEN — corpus-generation and benchmarking follow-ups

- **LLM-assisted disagreement sampling against `sample_default.txt`.**
  2026-07-30: rejected a full LLM relabel (92039 lines, ~$few-$150) as
  circular — the corpus is auto-labeled by the same rule-based classifier an
  LLM relabel would just re-agree/disagree with in the same blind spots.
  Agreed direction instead: use an LLM only to find *disagreements* on a
  small stratified sample, hand-verify only those, append confirmed
  corrections append-only. A first pass was done **by hand** that day (no LLM
  call), which directly motivated the commented-out-code gate below.

  **2026-08-04 — persistence plumbing built (user-commissioned)** so
  confirmed corrections survive `make gru-acquire-corpus` regenerating
  `sample_default.txt` from scratch every run. New committed file
  `tools/gru/disagreement_corrections.txt` (named exception to RDD_EXT_19,
  same footing as `sample_default.txt`/`code-formatter-ai-assist-weights.json`
  per RDD_KEY_217), empty until a disagreement-sampling pass produces
  confirmed rows. New `tools/gru/apply_disagreement_corrections.py`, wired
  into `gru-acquire-corpus` right after the `classifier_weights_examples.tsv`
  append and before final dedup: *override* merge keyed on
  `<lang>/<targetWordIndex>/<escaped-comment-text>` (everything but
  `<label>`), dropping the conflicting auto-labeled row instead of leaving
  both present (plain-append dedup can't collapse a same-text/different-label
  pair). Smoke-tested standalone (override/new-row/empty-file no-op) against
  synthetic scratch files.

  **2026-08-04 — the LLM disagreement pass itself, executed via the `grok`
  CLI (xAI Grok-4.3, headless).** Sampled 150 unique YES + 150 unique NO rows
  (`random.Random(42)`) from `sample_default.txt` (89305 YES / 3292 NO unique
  pools). Grok labeled all 300 blind to the existing label ($0.057, no
  truncation); 74/300 (24.7%) disagreed — high against a 92.4%-precision
  classifier, so flagged to the user rather than trusted. User hand-verified
  every disagreement: 44 confirmed genuine corrections (43 YES, 1 NO)
  appended to `disagreement_corrections.txt`; the other 30 were too ambiguous
  or confirmed the original label. Applying the 44 against the real corpus
  verified clean (44/44 matched, 0 mismatches). Production already clears
  both bars (92.4% mean held-out CV precision at `abstainThreshold=0.7`, 2.7%
  NO FP rate), so this pass was optional polish — measuring the resulting
  precision delta was left as a next step.

  **2026-08-04 — retrained on the corrections, checked, then reverted pending
  a real corpus-level CV.** After `make gru-acquire-corpus` (44/44 applied)
  and a user retrain: (1) `GruEval` training-fit on the 522-row bench went
  99.8%→99.4% (1→3 NO FPs) — noise from a 44-row/93k-line perturbation, not a
  real signal; (2) a CV run was accidentally pointed at
  `classifier_weights_examples.tsv` (the 522-row bench itself, never touching
  `sample_default.txt`/corrections) — it only re-measured the bench's own
  held-out generalization at its now-522-row size (mean=88.78%, stdev=4.81%,
  down from the 474-row 92.4%±2.1%, i.e. the grown bench is
  harder/higher-variance) — unrelated to the corrections. Neither check
  isolates the 44 corrections' effect; that needs a CV run against the full
  `sample_default.txt` (~2700-4050s/round). Added `make gru-cv-corpus`
  (`GRU_CV_ROUNDS`/`GRU_CV_WORK_DIR`/`GRU_CV_LOG`/`GRU_CV_ARGS`) for the user
  to run unattended on CM5. **Reverted the retrained production artifacts**
  (`code-formatter-ai-assist-weights.json`, `tools/gru/sample_default.txt` —
  `git checkout --`'d) pending that measurement, preserved as untracked
  snapshots for restore if CM5 showed improvement:
  `code-formatter-ai-assist-weights.2026-08-04-grok-corrections.json`,
  `tools/gru/sample_default.2026-08-04-grok-corrections.txt`.

  **2026-08-05 — CLOSED via the CM5 `gru-cv-corpus` run.** Confirmed the CM5
  run used the pre-corrections corpus (a known correction's row still showed
  the old `NO` label in every round file, matching the reverted committed
  `sample_default.txt`). Real 5-round full-corpus CV (`--eval-threshold 0.7`,
  74793 train / 18699 test/round):

  ```
  round 0: precision=99.32% (18472/18598, 101 abstain)
  round 1: precision=99.26% (18464/18602, 97 abstain)
  round 2: precision=99.36% (18483/18602, 97 abstain)
  round 3: precision=99.30% (18469/18600, 99 abstain)
  round 4: precision=99.40% (18489/18600, 96 abstain)
  mean=99.33%  stdev=0.06%  min=99.26%  max=99.40%
  ```

  Far above every prior figure (which measured the small, all-ambiguous
  hand-labeled bench, not real corpus distribution) — confirms the shipped,
  without-Grok-corrections weights/corpus already clears production bars by a
  wide margin. Also swept `abstainThreshold` 0.7/0.75/0.8 against the same
  cached weights (no retrain): NO FP rate only drifted
  12.43%→12.22%→11.94% while abstains grew ~48% — flatter trade-off than the
  hand-labeled-bench sweep, so `abstainThreshold` stays `0.7`.

  **Decision: do not adopt the Grok-corrections weights/corpus** — the
  un-corrected corpus already clearing bars answers the question this thread
  was blocked on (is the 44-row batch a real improvement), with no reason to
  pull in the reverted snapshot. Moved the two `*-2026-08-04-grok-corrections.*`
  snapshots and the archived 44-row batch
  (`tools/gru/unused/disagreement_corrections.2026-08-04-grok.txt`) to
  `tools/gru/unused/` (see its README); live
  `tools/gru/disagreement_corrections.txt` emptied back to header-only
  (confirmed byte-identical, true-no-op regeneration). The correction
  mechanism (script + Makefile wiring) stays live for future passes. Also
  retired to `tools/gru/unused/` (never wired into any Makefile target):
  `gen_synthetic_prompt.py`, `regroup_synthetic.py` (+ sidecars),
  `tools/synthetic_out_grok.txt`. CV scratch files (`cvc.zip` etc.) were
  never committed, per `RDD_EXT_19`.

- **[SEPARATE, closed]** A cluster of extracted comments are DTD/URL
  string-literal fragments with no leading space (e.g. `Sun Microsystems,
  Inc.//DTD Enterprise JavaBeans 1.1//EN";`) that look like `//` inside a
  string literal — root-caused and fixed 2026-08-01 (see `extract_comments.py`
  fix in the condensed history below).

---

## Condensed job history (through 2026-07-29)

Everything mechanical is real, not stubbed: `GruClassifier` (tokenize,
hashBucket, softmax, decide, real bidirectional-GRU `forward()`),
`GruWeights` (full schema, hand-rolled JSON parser, backward-compatible with
scalar-only fixtures), `Vocabulary` (explicit-vocab-vs-hash-bucket lookup),
`GruAbstainResolver` (real "rules → GRU on abstain" pipeline, config-gated
via `gru-classifier`/`gru-weights-path`), `tools/gru/GruTrainer.java` (real
training loop: Xavier/Glorot init, mini-batch forward+backward+Adam —
RDD_EXT_18's batch-32 default superseded by configurable `--batch-size`,
default 16, see 2026-08-01 — 20% held-out validation split with
patience-based early stopping, reads RDD_EXT_21's 4-column schema, loads
`explicit_vocab.txt` by default per RDD_EXT_22), the `gru-train`/
`gru-extract-pool-a`/`gru-extract-pool-b`/`gru-measure-abstain-rate` Makefile
targets, and five passing self-tests. `GruClassifier.classify` abstains
whenever `hasTrainedWeights()` is false — fail-safe posture.

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

Also fixed 3 `targetWordIndex` bugs in `tools/gru/sample_examples.txt` Pool B
lines (one past tokenization end → silent skip every `GruTrainer` run; two
wrong token) — verified via live smoke run. Live wiring:
`MiscRuleCore.classifyComment` → `GruAbstainResolver.resolve(...)` (was
`CommentClassifier.classify` direct), threading `gruClassifier`/
`gruWeightsPath` through `MiscRuleCore` → `MiscRuleCurly` →
`ScopePipelineCurly` → `FormatterCurly`. `Config.gruClassifier` defaults
`true` (fails safe to ABSTAIN if weights missing).

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
Root cause of the 9-fixture regression: 40-example
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
   semicolon — same asymmetric-risk shape as the rejected hyphen gate, hence
   the eventual gate needs a second signal. Live-formatting-correctness
   finding too (`CommentClassifier` gates always live regardless of
   `gru-classifier`).
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

**2026-08-01 follow-up: `make gru-train` auto-resume.** Makefile never
passed `--resume` — interrupted checkpoint sat unused, next run restarted
scratch. Fixed: target checks `$(GRU_WEIGHTS_OUT).ckpt-current.bin`,
auto-adds `--resume=...` if present (`gru-train: found ..., resuming`);
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
Naive "2+ newlines + not ending `.`/`!`/`?`" too blunt alone.
**`LicenseBlockGate.looksLikeLicenseBlock(String)`** requires **both**: (1)
primary newline-span signal, (2) copyright/license vocab anywhere
(`Copyright`, `(C)`, `SPDX-License-Identifier`, `Licensed under`, `All
rights reserved`, `Redistribution and use`, `Permission is hereby granted`,
`WITHOUT WARRANTIES`/`WARRANTY`). Decorative-border confirming signal
**rejected** — real corpus had hundreds of ordinary section-banners (e.g.
`apache/ant` `===...===`-framed XML headers). New
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
O(h) chain overhead within noise. **Kept anyway** (fewer allocs, zero behavior
risk, bit-identical, gradient-check-verified) — not reverted. `make test`:
225/225. Committed: `src/com/jxmake/formatter/classifier/gru/GruClassifier.java`.

**Float vs double — evaluated and REJECTED (kept double end-to-end).** Baseline
committed weights on 221-row hard-case, double: precision 53.85%
(119/221, 88 yesCorrect/3 yesIncorrect, 31 noCorrect/99 noIncorrect).
Converted `GruWeights.java`/`GruClassifier.java` weight *storage* to float
(narrow at array construction, JSON parse unchanged): float-typed `GruEval`
was **byte-identical** decision-for-decision to double — zero accuracy
impact from float32 storage. But trainer-to-float surfaced coupling
(`GruClassifier.Gradients` reuses `GruWeights.DirectionWeights` as field
type — converting forces mini-batch grad-accum/Adam math into float too,
needing a split into float storage + double grad-accumulator structs across
every trainer call site). **Decision: reverted float conversion in full,
kept double end-to-end** — scope/risk call, not accuracy (impact exactly
zero); "just the trainer" not bounded; float storage alone revisitable later
as a narrow task. No commit for float conversion. `make test` 225/225 after
revert. `code-formatter-ai-assist-weights.json` byte-unchanged.

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
5-round CV.** 7 misses on 474-row on-training eval, no single fixable
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
Followed the open item above: swept `GruEval` optional threshold args
(0.5/0.6/0.7/0.8/0.9) against 5 already-trained CV rounds' held-out sets (no
retrain — `GruEval` caches forward-pass probs, re-evaluates boundary free):

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
**Disposition (2026-08-10):** the accepted 2.7% residual NO false-positive
rate at this threshold is documented in `README.md`'s Known Limitations →
"AI-assist (GRU)"; removed from `XL.txt` TIER 9 (accepted, not further
reduced, not a live TODO).

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

With corrected baseline, completed full self-formatting dogfood-and-adopt end
to end: round1/round2 idempotent, trial jar from round1 passed
`make _test_serial` 228/228, round1b/round2b fixed-point check against
original `src/` confirmed (round1≡round1b, round2≡round2b), round1 adopted
into `src/` (37 files — all diffs spot-checked cosmetic: unary-minus spacing,
missing binary-operator spacing, declaration-alignment padding, line-wrap
reflow, comment capitalization from now-active GRU), rebuilt clean against
`make test` / `make test-server` / `make bench`. Parallel `tools/gru`/
`tools/verifiers` self-formatting also completed (round1/round2 idempotent,
`java_syntax_check` clean on orig/r1/r2, `java_content_diff` confirmed
AST-equivalent content orig-vs-r1 and r1-vs-r2 for all 9 `tools/gru` + 4
`tools/verifiers` files); adopted round1 into both dirs (2 of 9 `tools/gru`
and 2 of 4 `tools/verifiers` actually changed; rest already formatter-clean).

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

---

**2026-08-10 — grew hand-labeled hard-case corpus; confirmed python3 is the only new-reachable
language among a broader candidate list.** User asked to add `examples_*.md` files for json5,
css, yaml, toml, xml, html5, js, ts, python3, makefile, bash, powershell (js/ts already existed)
and more NO samples, then re-run `derive_weights.py`. Investigated actual call paths (per
`STATE_COMMON.md`'s file-exclusion/ambiguity discipline) rather than trusting the 2026-08-01
"only c/cpp/java/kotlin/js/ts reach the gate" note verbatim, since python3 comment normalization
landed 2026-08-08, after that note was written:

- `KeywordAmbiguityGate`/`classifyComment` is only called from `MiscRuleCore` (curly:
  c/cpp/java/kotlin/js/ts) and `MiscRuleIndent` (python3, `#`-comment normalization wired
  2026-08-08 per `STATE_PYTHON3.md`).
- `ToolingCommentNormalizer.java`'s own doc comment states yaml/toml/makefile/bash/powershell
  (and xml, which reuses the same chain-grouping) use "No classifier/GRU dependency" — a separate
  ad hoc capitalization rule (STYLE_TOOLING.md §0 pattern) with no keyword-ambiguity concept.
- json5/css/html5 have no comment-normalization wiring calling `classifyComment` at all.

So of the requested list, only **python3** actually feeds this classifier; the other 9 languages
would get an inert `examples_*.md` file (never affecting runtime behavior). Flagged this to the
user via `AskUserQuestion` before proceeding; user chose "python3 only + fix keyword-set bug
first".

**Fix:** `KeywordAmbiguityGate.hasLeadingKeywordMatch` had no python3 branch (same bug shape
originally found for JS/TS) and silently fell through to the wrong `KEYWORDS_C` default. Added
`KEYWORDS_PYTHON` (full CPython `keyword.kwlist` + soft keywords `match`/`case`) + a
`lang.isPython3` dispatch branch. Updated the stale "python3 never reaches this gate" comment in
the same file to reflect the 2026-08-08 wiring change.

**New `tools/classifier_weights/examples_python3.md`** (48 rows, balanced zero-feature YES/NO
from the start per the `KEYWORD_BIAS`-flip lesson) + 4 new zero-feature NO rows each to
`examples_{c,cpp,java,kotlin,js,ts}.md` (naturalistic-phrasing NO coverage for keywords already
present, no new keywords — the "more NO samples" part of the ask). Registered the new stem in
both `tools/classifier_weights/derive_weights.py`'s and `tools/gru/convert_classifier_weights_examples.py`'s
`LANG_BY_STEM` maps (both needed it, same caveat each file's own comment already documents).

**Re-derived weights** (594 rows total across 7 files):
`KEYWORD_BIAS=-1.14719, KEYWORD_WEIGHT_PAREN=-2.31089, KEYWORD_WEIGHT_ARROW=-0.61513,
KEYWORD_WEIGHT_SEMICOLON=-2.63047, KEYWORD_WEIGHT_URL_OR_NUMBER=-0.06490`. 459/594 (77.27%)
classified as labeled — essentially unchanged from the 522-row set's 77.97% (added rows were
mostly zero-feature NO, which a 4-feature linear model can't separate from zero-feature YES any
better than before; expected, not a regression). Copied into `CommentClassifierWeights.java` +
`tools/classifier_weights/weights.md`. `make jar` + `make test`: **275/275 forward, 275/275
idempotency** — clean (python3's `KeywordAmbiguityGate` fix didn't move any existing fixture,
since no test fixture currently exercises a python3 comment starting with a Python-only keyword
under `comment-normalization-classifier=on`).

**Not done this session (out of scope, not requested):** `make gru-acquire-corpus` /
`GruTrainer` retrain against the grown corpus — GRU-side work is a separate, much larger effort
(hours-per-round CV per the 2026-08-02/2026-08-03 session log above) and wasn't asked for; the
linear-classifier re-derivation above is the only classifier this session's ask targets.

**2026-08-10 note:** `GruTrainer` retrain against the grown 594-row corpus and a fresh
`cross_validate.py` GRU-vs-linear CV are both **recurring** work, already covered by TIER 0's
"[GRU]" job in `XL.txt` (grow corpus → `gru-acquire-corpus` → user retrains via `gru-train` from
another console → check GRU %/CV) — not separate one-off TODOs. `gru-acquire-corpus` was re-run
2026-08-10 (`tools/gru/sample_default.txt`, 119641 lines, includes the grown hand-labeled corpus);
retrain/CV deliberately left for the user to run themselves.

**2026-08-10 — confirmed: `MiscRuleCore.stripSoleTrailingPeriod`'s `dotCount != 1` path DOES
route through `GruAbstainResolver`.** Read the method body directly (line ~2713): the
`commentNormalizationClassifier`-gated `classifyComment(content, lastTokenIndex(content))` call
(which reaches `GruAbstainResolver.resolve` unconditionally when the classifier is on) runs
*before* `dotCount` is even computed — the trailing-char/`dotCount` checks are pure mechanical
bail-outs applied after the classifier already ran. So for a comment ending in `.` with
`dotCount != 1` (e.g. `.hpp`, `e.g.`, an ellipsis), `GruAbstainResolver` is invoked and its result
computed, then discarded by the mechanical dot-count bail-out regardless of what it returned —
a real but harmless (correctness-wise) wasted classifier call, not a wiring gap. No fix needed;
this only clarifies TIER 4's "mid-word-dot sentence-boundary detection" item (`XL.txt`) — that
item is about teaching the *classifier* to handle multi-dot content, not about routing, which
was never broken.
**Disposition (2026-08-10):** documented in `README.md`'s Known Limitations →
"AI-assist (GRU)"; removed from `XL.txt` TIER 9 (canceled, not a live TODO,
per the 2026-08-04 CANCELED entry above).

**2026-08-10 — `make gru-train` re-run by user against the grown
`sample_default.txt` (119641 lines, includes the grown hand-labeled corpus,
per the entry above).** `trainExamples=97490` (594 hand-labeled rows
oversampled x3 = 1782 extra rows folded in), `validationExamples=23926`.
Early-stopped at epoch 7 (no validation improvement for 3 epochs, patience
3), best weights from epoch 4: `trainLoss=0.0171390,
validationLoss=0.0217870` (lowest of the run — validationLoss rose again at
epochs 5-7: 0.0263591, 0.0360400, 0.0377724, classic overfit-after-best-
epoch shape). Epoch 4 validation confusion matrix (positive=YES):
`tp=23134, fp=60, tn=658, fn=74, precision=0.99741, recall=0.99681,
f1=0.99711` — this is validation-split precision from training, not a
GruEval held-out figure. Wrote `target/gru/code-formatter-ai-assist-
weights.json`.

**Quick on-bench `GruEval` check (user explicitly asked for just this, not
a fresh multi-hour `cross_validate.py` CV run) against the 594-row
hand-labeled bench (`target/gru/classifier_weights_examples.tsv`, same
convert script as always):** `threshold=0.7 total=594 abstain=0 decided=594
correct=594 precision=100.0% (yesCorrect=135/135, noCorrect=459/459)`.
**Same caveat as every prior on-bench figure:** this 594-row set is
oversampled directly into training data, so 100% is training-fit, not a
true generalization measure — same shape as the 98.7%-vs-86.3% gap on
2026-08-02. A genuine held-out number needs `cross_validate.py`
(`make gru-cv-corpus`), which the user deferred for time. Weights not yet
promoted to `$(CLASS_DIR)`/repo root/`code-formatter-ai-assist-weights.json`
as of this entry — still sitting in `target/gru/`.

---

**2026-08-11 — `cross_validate.py` round-loop resumability.** User added a
`--progress-every` CLI param by hand (threaded through to each round's
`GruTrainer` invocation, already trainer-supported); the round-loop's own
skip-if-already-done logic, found absent, was implemented. Each round now
checks `weights_round{N}.json` existence AND absence of its sibling
checkpoint (`GruTrainer`'s own `CHECKPOINT_CURRENT_SUFFIX`, appended to the
`--out` path given, i.e. `weights_round{N}.json.ckpt-current.bin`, not a
same-stem sibling) before treating a round as complete — mirroring the
trainer's own resumability semantics (checkpoint deleted only on normal
completion; present means a prior run crashed mid-round, so that round runs
(re-)normally, not skipped). A skipped round still runs `GruEval` against
the existing `weights_round{N}.json`, reading its precision back fresh into
the same in-memory `precisions` list every round contributes to — nothing
cached/assumed. `train_path`/`test_path` are rewritten every round
regardless of skip (deterministic given the fixed per-round seed, cheap,
and needed for `GruEval`'s test split either way). `args.rounds` growing
across a resumed run works for free — rounds are addressed by index/seed,
so done low indices skip and new higher indices just run, no
special-casing needed. Also fixed a pre-existing cosmetic double-space in
the `--progress-every` f-string.

**Validation (no full CV run — multi-hour per this file's own prior CV
timing notes):** a standalone Python snippet (scratch-only) exercised
`weights_path.exists() and not checkpoint_path.exists()` against a temp dir
through all 3 states — nothing on disk, weights only, weights+checkpoint
(simulating a mid-round crash), then checkpoint removed again (simulating
completion) — all 4 assertions passed. `python3 -m py_compile
tools/gru/cross_validate.py` clean. Static review confirmed the aggregation
path (`eval_cmd`/`PRECISION_RE` parsing/`precisions.append`) is
unconditional after the skip/train branch, so a skipped round's precision
appends identically to a fresh one's — only the branch guarding the
expensive `GruTrainer` subprocess call itself needed changing.

**2026-08-11 (same day, follow-up) — user-reported "GRU_CV_ARGS not
honored" + "no progress printed", both root-caused to one bug, fixed.**

1. `gru-cv-corpus`'s Makefile recipe (line 290) does reference
   `$(GRU_CV_ARGS)` on `cross_validate.py`'s command line — not stale/dead,
   ruled out.
2. `cross_validate.py`'s argparse declares and threads `--epochs`/
   `--patience`/`--eval-threshold`/`--progress-every` into `train_cmd`/
   `eval_cmd` correctly, no hardcoded override found — confirmed by a real
   run (below): `GruTrainer`'s startup line echoed `maxEpochs=2, patience=1`
   exactly matching what was passed.
3. **Root cause:** the round loop's `subprocess.run(train_cmd,
   cwd=formatter_dir, check=True, stdout=subprocess.DEVNULL,
   stderr=subprocess.DEVNULL)` (introduced when mini-batch/checkpoint
   support landed, predates this session) discards the `GruTrainer`
   subprocess's stdout/stderr entirely, silently dropping every
   `--progress-every`-gated `GruTrainer: epoch ...` line regardless of the
   flag — explaining symptom 2 directly, and symptom 1's *appearance*: with
   output suppressed there was no way to see whether `--epochs`/
   `--patience` were taking effect (only the final `GruEval` precision line
   was ever visible) — the args were wired correctly all along; only
   visibility was missing.
4. `GruTrainer.java`'s `printProgress` (~line 1216) writes via
   `System.out.println` unconditionally when called, gated only by
   `progressEvery > 0 && examplesSeen % progressEvery == 0` (~line 567) —
   not itself broken or behind a separate verbosity flag.
5. **Fix:** removed `stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL`
   from the training subprocess call so it inherits `cross_validate.py`'s
   own stdout/stderr — flows through to the console for a manual run, or to
   `GRU_CV_LOG` under `make gru-cv-corpus`'s existing `> $(GRU_CV_LOG) 2>&1`
   redirection, no Makefile change needed. The `GruEval` subprocess call
   (`stdout=subprocess.PIPE`) is intentionally left as-is — its output is
   parsed via `PRECISION_RE`, not just logged.

**Validated with a real tiny end-to-end run** (not just static review, since
this is runtime plumbing): 12-line synthetic examples file, `--rounds 1
--epochs 2 --patience 1 --progress-every=1 --eval-threshold 0.7`. Before the
fix: zero `GruTrainer:` lines printed. After: full progress output
appeared, including `GruTrainer: starting -- ... maxEpochs=2, patience=1
...` (confirming `--epochs`/`--patience` were honored all along) and a
`GruTrainer: epoch N, progress M/8 (...)` line per training example per
epoch (matching `--progress-every=1`), through to the final `GruEval`
precision line, unaffected. `python3 -m py_compile tools/gru/cross_validate.py`
clean after the fix.

---

**2026-08-12 — user-flagged "too good to be true" 5-round full-corpus CV
result investigated; out-of-distribution GRU check added and run.** User's
2026-08-11 `make gru-cv-corpus` run (`Zcv_corpus.zip`, seed 1000-1004, full
`tools/gru/sample_default.txt` — 119641 lines, train=95712/test=23929 per
round) reported `mean=0.9933 stdev=0.0008 min=0.9920 max=0.9939`
(`gru_cv_corpus.out`, inspected directly). **Diagnosis: same
training-fit-vs-held-out shape as the 2026-08-02 98.7%-vs-86.3% gap, not a
new bug.** This CV run splits `sample_default.txt` itself, overwhelmingly
the `GenerateSampleDefault.java` auto-labeled majority (easy, already
rule-labeled YES/NO), not the 594-row hand-labeled hard-case set —
per-round confusion matrices confirm the skew directly (`tn+fn` ~600-700 vs
`tp+fp` ~23000 per round). The genuinely-hard, hand-labeled-only CV
(474/522-row sets, 2026-08-02/2026-08-02-later) is the number that actually
bounds real generalization (86.3% mean at the time); this run instead
measures near-in-distribution accuracy on a corpus dominated by
already-easy examples, never a substitute for it — 99.33% is real but not
informative about hard cases.

**New out-of-distribution check, no ground truth needed.** Built two new
permanent tools (`tools/gru/FilterAbstain.java`,
`tools/gru/GruRealCorpusTally.java` — Makefile targets
`gru-filter-abstain`/`gru-real-corpus-tally`) to sanity-check the shipped
weights against a real, unrelated codebase without hand-labeling: extract
comments from `../../../../Shadow/Pt/*.{h,hpp,tpp,c,cpp}` (external repo,
687 candidate files, `extract_comments.py` → 188709 raw comments), filter
down to the 4461 the rule-based `CommentClassifier` itself ABSTAINs on (the
only lines the GRU stage ever reaches in production — a raw random sample
is mostly non-ambiguous lines the GRU never sees, so sampling *before* this
filter under-exercises the GRU the same way the full-corpus CV above does),
then feed a fixed-seed 200-line sample (user's explicit hard cap) through
`GruRealCorpusTally` at `abstainThreshold` = 0.5 / 0.68 / 0.70 / 0.72 /
0.76 / 0.90 (shared forward-pass probabilities per line, one pass per
threshold, same technique as `GruEval`'s sweep):

```
threshold=0.50  GRU-YES=183 GRU-NO=17 GRU-ABSTAIN=0   gru-decide-rate=100.0%
threshold=0.68  GRU-YES=180 GRU-NO=14 GRU-ABSTAIN=6   gru-decide-rate=97.0%
threshold=0.70  GRU-YES=180 GRU-NO=14 GRU-ABSTAIN=6   gru-decide-rate=97.0%
threshold=0.72  GRU-YES=180 GRU-NO=13 GRU-ABSTAIN=7   gru-decide-rate=96.5%
threshold=0.76  GRU-YES=180 GRU-NO=13 GRU-ABSTAIN=7   gru-decide-rate=96.5%
threshold=0.90  GRU-YES=166 GRU-NO=7  GRU-ABSTAIN=27  gru-decide-rate=86.5%
```

**Reading:** on genuinely out-of-distribution text, 0.68/0.72/0.76 all
behave almost identically to the shipped 0.7 (96.5-97.0% decide-rate,
~13x YES:NO skew) — none of the three requested points meaningfully changes
behavior versus the current default; the classifier still resolves nearly
everything confidently even on unseen code, and only 0.90 (well outside the
requested range) meaningfully raises the abstain rate (13.5%). 15-line
manual spot-check of the sampled rule-ABSTAIN lines: most are genuine
fluent English keyword-led sentences (`"do current buffer contents need
written?"`, `"else RGB order"`, `"if no instance is selected yet"`) —
plausible-looking YES calls, not obvious garbage/majority-class collapse —
but one sampled line was a multi-line commented-out code block
(`BIO_set_cipher_ctx`) that should mechanically be NO; not checked whether
GRU got that specific one right (no ground truth in this pass). **No
precision number is claimable from this pass** (Shadow/Pt has no hand
labels) — this only answers "does the abstain rate/skew look realistic on
truly unseen text," not "is it correct." **Disposition: 0.68/0.72/0.76 do
not look more realistic than 0.70 by this measure — no threshold change
made.** `abstainThreshold` stays `0.7` in
`code-formatter-ai-assist-weights.json`; no weights/trainer/tools code
changed. A genuine precision check against Shadow/Pt-class text would need
hand-labeling a Pool-A-shaped subset (see `add_target_index.py`) of
`FilterAbstain`'s output and running it through `GruEval` (not
`GruRealCorpusTally`, which is decisiveness-only) — not done this session
(out of scope; user said "we decide more as needed" after just these 3
threshold points).

`make jar` + `make test`: **286/286 forward, 286/286 idempotency** — clean
(new tools live under `tools/gru/`, outside `src/`, don't affect the shipped
jar's behavior). `Zcv_corpus.zip` (real training/test splits + trained
per-round weights, 40MB) and all `Shadow/Pt`-derived extraction/sample files
stay under `/tmp`/scratchpad per RDD_EXT_19 — not committed, not left in the
repo root.

**Follow-up (same day) — ran the 3 threshold candidates against genuinely
held-out data, then adopted `abstainThreshold = 0.76`.** Two further checks
before deciding, both against real held-out/unlabeled data — not the
594-row bench, which is training-fit (reran it anyway as a sanity check:
100% precision at all four thresholds, confirming, as expected, that it
can't discriminate between them):

1. **Per-CV-round held-out sweep.** Ran `GruEval` at 0.68/0.70/0.72/0.76
   against each of `Zcv_corpus.zip`'s 5 rounds' own `test_round{N}.txt`,
   using that round's own `weights_round{N}.json` (no train/test leakage).
   Aggregated across all 5 rounds:

   | threshold | FN (missed YES) rate | FP (NO→YES) rate | abstain (of 119642) |
   |---|---|---|---|
   | 0.68 | 372/115685 = 0.32% | 449/3504 = 12.82% | 453 (0.38%) |
   | 0.70 | 362/115662 = 0.31% | 437/3472 = 12.59% | 508 (0.42%) |
   | 0.72 | 344/115631 = 0.30% | 432/3453 = 12.51% | 558 (0.47%) |
   | 0.76 | 293/115524 = 0.25% | 419/3401 = 12.32% | 717 (0.60%) |

   Both FN and FP rates fall monotonically as the threshold rises — 0.76
   strictly beats 0.72 and 0.70 here. Caveat: this is the same
   easy-example-dominated corpus flagged as "too good to be true" above
   (~12.5% FP rate here vs. the ~2.7% FP rate the 2026-08-02 hard-case-only
   CV found at 0.7 — not the same population), so the improvement reflects
   mostly-easy examples getting marginally easier to abstain-correctly on,
   not necessarily hard-case generalization.
2. **Shadow/Pt out-of-distribution check** (previous entry): 0.68/0.72/0.76
   all behaved almost identically to 0.70 (96.5-97.0% decide-rate) — no
   evidence any of the three regressed decisiveness on unseen text.

**Considered and rejected: using one of the 5 CV rounds' own trained
weights (e.g. `weights_round3.json`, which had the lowest FP count at
0.76) as the production weights file**, instead of a full-corpus retrain.
Rejected on two grounds: (1) round-to-round differences are noise at this
scale (the CV's own `stdev=0.0008` on precision says so directly; round3
has the fewest FPs at 0.76 but not the fewest FNs — a different column
picks a different "best" round), and (2) each round trains on only 95712
of 119641 rows (80%, since 20% is deliberately held out for that round's
own test) — strictly less data than a full-corpus retrain for no benefit.
CV's job (estimating generalization, validating a threshold choice) was
already done; shipping a fold's intermediate weights would throw away data
and chase noise.

**Decision: keep the full-corpus-trained weights, raise `abstainThreshold`
0.7 → 0.76 only** (same "pure inference-time metadata, no retrain needed"
mechanism confirmed 2026-08-03 — `GruTrainer` writes the field verbatim and
never reads it back for training). Changed:
- `code-formatter-ai-assist-weights.json`: `abstainThreshold` `0.7` → `0.76`
  (single-line edit, verified via `grep -o '"abstainThreshold": [0-9.]*'`
  and a comma-split `cmp` that only that field's line differed — the file
  is one giant JSON line, too large to diff normally).
- `tools/gru/GruTrainer.java`: `ABSTAIN_THRESHOLD` constant `0.7` → `0.76`
  (future `make gru-train` bakes in `0.76` by default).
- `Makefile`: `GRU_CV_ARGS`'s `--eval-threshold 0.7` → `0.76`;
  `GRU_TALLY_THRESHOLDS` default `0.7` → `0.76`.
- `README.md`: both `abstainThreshold = 0.7` references updated; the
  "~2.7% at 0.7" FP-rate figure reworded to avoid restating a number not
  re-measured at 0.76 on the hard-case-only population specifically.
- `DESIGN_NOTES.md`: heading + added a paragraph summarizing this
  investigation's numbers.
- `tools/gru/README.txt`: checked, no change needed — its only `0.7`
  references are either generic sweep examples (`0.5 0.6 0.7 0.8 0.9`) or a
  historical note about a specific past CV run, not a "current default"
  statement.
- `../README.txt`: checked, no GRU/`abstainThreshold` mention exists there
  at all — no change needed.

`make jar` + `make test`: **286/286 forward, 286/286 idempotency** — clean.
