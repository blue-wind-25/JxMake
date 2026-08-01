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
| RDD_EXT_18 | Step 3 GRU training hyperparameters (starting defaults): Adam, lr~1e-3, batch size 32 (superseded in practice — trainer uses batch size 1, see below), 20-50 epochs with early stopping on validation loss, dropout 0.2-0.3 |
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
(now defaults `on` — see the 2026-07-30 KEYWORD_BIAS-fix session below).
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

---

## Condensed job history (through 2026-07-29)

Everything mechanical is real, not stubbed: `GruClassifier` (tokenize,
hashBucket, softmax, decide, real bidirectional-GRU `forward()`),
`GruWeights` (full schema, hand-rolled JSON parser, backward-compatible with
scalar-only fixtures), `Vocabulary` (explicit-vocab-vs-hash-bucket lookup),
`GruAbstainResolver` (real "rules → GRU on abstain" pipeline, config-gated
via `gru-classifier`/`gru-weights-path`), `tools/gru/GruTrainer.java` (real
training loop: Xavier/Glorot init, per-example forward+backward+Adam at
batch size 1 — a deliberate simplification of RDD_EXT_18's batch-32 default
— 20% held-out validation split with patience-based early stopping, reads
RDD_EXT_21's 4-column schema, loads `explicit_vocab.txt` by default per
RDD_EXT_22), the `gru-train`/`gru-extract-pool-a`/`gru-extract-pool-b`/
`gru-measure-abstain-rate` Makefile targets, and five passing self-tests.
`GruClassifier.classify` abstains whenever `hasTrainedWeights()` is false —
fail-safe posture, no change to rule-based behavior until a real weights
file is deployed.

**Real ABSTAIN-rate measurement (item 9, CLOSED):** `tools/gru/
extract_comments.py` + `tools/gru/CommentAbstainTally.java` measured
**0.0%-0.6%** ABSTAIN across 14 corpora (~199,000 comments, own repos + 11
vetted MIT/Apache-2.0/BSD-3-Clause public repos), with two explained
outliers (vendored bitmap-font/zlib/libjpeg content; one too-small 103-line
corpus). Confirms random sampling is impractical for training-set
acquisition — targeted extraction (RDD_EXT_15) is required.

**Pool A/B extraction, labeling, first production run:** `ExtractPoolA.java`
/`extract_pool_b.py` extract from `extract_comments.py`'s corpus format. A
genuine `extract_comments.py` bug (a literal `/*` inside a `//` line comment
swallowing unrelated later text) was found and fixed with a single
left-to-right scanner treating `//` and `/* */` as mutually exclusive spans.
Post-fix: 57974 comments → Pool A 167 / Pool B 241 candidates, hand-labeled
per RDD_EXT_20 (Pool A: 45 YES/122 NO; Pool B: 41 YES/200 NO via a documented
rule-based fallback). First production training run (408 examples, 80/20
split, `--epochs=40 --patience=6`): early-stopped epoch 15, held-out
precision 97.96%. After curating the permanent explicit vocab (RDD_EXT_22)
and retraining on the same split, held-out precision became **93.88%**
(current baseline; 97.96% is pre-RDD_EXT_22 and historical) — still clears
RDD_EXT_17's 90% bar. Qualitative spot-check confirmed the model
differentiates on surrounding context, not the target token alone (e.g.
`"for the sake of clarity"` → YES vs. `"for (int i..."` → NO on the same
leading token).

**Tooling for corpus growth/cross-validation:** `acquire_corpus.sh`
(automates acquisition+extraction across 16 hardcoded sources — 5 local
dogfood repos + 11 vetted public repos — stopping before labeling, a human
call per RDD_EXT_20), `GruEval.java` (precision/abstain-rate against an
RDD_EXT_21-schema file), `cross_validate.py` (repeated Monte Carlo
cross-validation). A real (non-smoke) 5-round cross-validation run against
the 408-example corpus gave **precision mean=92.40%, stdev=3.00%,
min=89.80%, max=96.49%**, confirming the single-split 93.88% sits within
normal variance. None of these scripts' working files (splits, weights,
clones) are ever committed (RDD_EXT_19); see `tools/gru/README.txt` for
invocation syntax.

**Optional synthetic-augmentation tooling** (`gen_synthetic_prompt.py`/
`regroup_synthetic.py`, chat-LLM assisted): pads Pool A/B with
copy-pasted-chat-prompt-generated examples. Explicitly **not** a substitute
for the real acquire_corpus.sh + hand-labeling pipeline (RDD_EXT_19/20/23) —
kept as a distinct, clearly-labeled source, never silently merged into the
real corpus. `sp_gemini.txt` predates the request-tracking sidecar
mechanism — treat it as vocab-only-checked legacy data, don't try to
reconstruct its request history.

**Full-scale acquire_corpus.sh run (16 sources):** 172,285 comments →
578 Pool A + 492 Pool B candidates (~3.5x the earlier hand-labeled batch),
archived to personal directory (RDD_EXT_19), **left unlabeled** — hand-
labeling them remains an available future option (the only source of real
ground-truth prose `NO` examples) but a different path was taken instead
(see next section).

---

## 2026-07-29 session: default auto-labeled corpus, live wiring, RDD_KEY_217

Rather than hand-labeling the 578/492 candidates above, this session used
`GenerateSampleDefault.java` to auto-label a large corpus via the existing
rule-based classifier (distant supervision), and wired the GRU path live
into the formatter.

**Why the rule-based classifier only ever returns YES/ABSTAIN, never NO (and
why NO can only come from the GRU):** `CommentClassifier.classify` is
architecturally incapable of returning `NO` — its decision tree only ever
returns `YES`/`ABSTAIN` (RDD_KEY_96); `GruAbstainResolver.resolve` only
calls the GRU on `ABSTAIN`, so `NO` could only ever come from the GRU stage.
Until this session the GRU stage was never reached in the live formatter at
all (config off, no shipped weights, and the default corpus turned out
100% YES), so `NO` never appeared. Later this same session a first
NO-producing rule-based gate (`DecorativeSeparatorGate`, below) was added,
partially superseding this finding — genuine prose/code NO still has no
rule-based gate at that point; the GRU remained the only source of prose NO.

**`tools/gru/GenerateSampleDefault.java`** runs an acquired corpus through
the real rule-based classifier to bootstrap `tools/gru/sample_default.txt`
(RDD_EXT_20/21 schema, `targetWordIndex=0`, `ABSTAIN` comments skipped).
Wired into `make gru-acquire-corpus`; dedups in place (77,499 duplicate
lines removed from a 172,285-comment run, mostly repeated license
headers). `make gru-train`'s default sample file switched from
`sample_examples.txt` to `sample_default.txt`. Full-scale run: **170,210
kept examples, 100% labeled YES** — direct empirical confirmation that
bootstrapping from the rule-based classifier alone can only teach the GRU to
imitate that classifier's YES/abstain-collapsed-to-skip behavior, never real
NO. Getting real NO signal still requires either hand-labeled Pool A/B or a
different bootstrapping signal.

**RDD_KEY_217** — named exception to RDD_EXT_19: per explicit user direction
(license compatibility — MIT/Apache-2.0/BSD-3-Clause sources, traceable
provenance, short quoted excerpts), exactly `tools/gru/sample_default.txt`
and `code-formatter-ai-assist-weights.json` are committed, unlike every
other real corpus/weights artifact this job produces. RDD_EXT_19's general
policy stands for everything else (hand-labeled Pool A/B, cross-validation
working files, any other derived artifact).

**`tools/gru/sample_examples.txt` bug fix:** found/fixed 3 pre-existing
`targetWordIndex` bugs in the small illustrative Pool B lines (one pointed
past the end of its tokenization, silently skipped by `GruTrainer` every
run; two pointed at the wrong token). Fixed in place, verified via a live
`GruTrainer` smoke run.

**Live formatter wiring:** `MiscRuleCore.classifyComment` now calls
`GruAbstainResolver.resolve(...)` instead of `CommentClassifier.classify`
directly — the GRU stage became genuinely reachable from the live pipeline.
`gruClassifier`/`gruWeightsPath` threaded through `MiscRuleCore` →
`MiscRuleCurly` → `ScopePipelineCurly` → `FormatterCurly` (backward-
compatible delegating overloads). `Config.gruClassifier` defaults `true`
(fails safe to ABSTAIN if the weights file is missing).

**Important finding — `comment-normalization-classifier` (the other,
prerequisite gate) had to stay `off` by default at this point.** Flipping
both defaults to `true` together regressed 9 `make test` fixtures
(`c_comments`, `cpp_modern`, `cpp_combined`, `cpp_comments`, `java_core`,
`java_combined`, `java_comments`, `real_code_regressions_22.kt`,
`real_code_regressions_54.java`). Root cause: the rule-based classifier
disagreed with the deterministic `isCommentNoCapitalizeWord` keyword list
on common real-code cases (wrongly capitalized comments starting with
`consteval`/`static`/`while`/`do-while`/`var`/`this`/`const`/`explicit`/
`public`/`switch`/etc.). Reverted `commentNormalizationClassifier` to
`false` (all-green `make test`) while leaving `gruClassifier=true` (provably
inert alone). **This was fixed the next day — see the 2026-07-30
KEYWORD_BIAS session below, after which `comment-normalization-classifier`
now defaults `true`.**

**User feedback on `GruTrainer` performance** from a real run
(`vocabSize=3500, trainExamples=74169, validationExamples=18542`, epoch 1
~1207s): `avgTrainLoss=0.0000` looked suspicious. Requested improvements
(bias-correction computed once per step, mini-batch size 32, cache
tokenization across epochs, fix early-stopping `bestWeights` to be a true
deep copy) — the bias-correction and tokenization-caching items were
confirmed done in the 2026-07-31 checkpointing session below; mini-batch and
the deep-copy fix status should be checked directly against
`GruTrainer.java`/commit history before assuming done.

**`CommentClassifier` first real NO-producing path — decorative-separator
gate:** `DecorativeSeparatorGate.isDecorativeOnly` (new file) returns true
for a comment with no letter/digit anywhere (e.g. `****...****`, `-----`).
Wired as a new gate right after the non-Latin-script gate, returning `NO`.
Presence-based, narrow — no commented-out-code or license-block detection
(left for later, see gates below). Structurally mutually exclusive with the
leading-keyword gate (no letters ⇒ can't also match a keyword). Validated
against a 96442-comment 5-repo corpus: **NO=20774** (0 before), ABSTAIN=2.1%
(fully explained by the two known vendored-content outlier repos, not a
regression); 15 hand-spot-checked new-NO comments, zero false positives.
`make test` clean (219/219 forward+idempotency) throughout. This means
`sample_default.txt` auto-labeling is now only *mostly* "never produces NO"
— decorative-only comments now do. Real prose NO ground truth still needs
the hand-labeled Pool A/B path.

**Still outstanding at end of session:** commented-out-code and multi-line
license-block NO gates (both later DONE — see the two 2026-07-31 sessions
below).

---

## 2026-07-30 session: fixed `KeywordAmbiguityGate` weight regression — `comment-normalization-classifier` now defaults `on`

Root cause of the 9-fixture 2026-07-29 regression: the 40-example
`tools/classifier_weights/examples_{c,cpp,java,kotlin}.md` set had all 20
"zero mechanical feature" rows (no paren/arrow/semicolon/url-or-number
signal) labeled YES — hand-authored "keyword-used-as-English-word" prose
only. That produced `KEYWORD_BIAS = +2.48420`, so any real keyword-led
comment with none of those four signals defaulted to YES (capitalize) —
wrong, since that shape is overwhelmingly a genuine code reference in real
code (confirmed real lines from `test/cpp_modern_inp.cpp`,
`test/cpp_combined_inp.cpp`, `test/java_core_inp.java`,
`test/java_combined_inp.java`, `test/java_comments_inp.java`).

**Fix:** added 22 new zero-feature NO-labeled rows (real regression lines +
hand-authored analogues), bringing the zero-feature split to 20 YES / 22 NO.
Re-derived via `derive_weights.py` (62 examples):

```
KEYWORD_BIAS                 = -0.20825   (was +2.48420)
KEYWORD_WEIGHT_PAREN         = -2.28827   (was -3.96297)
KEYWORD_WEIGHT_ARROW         = -1.51467   (was -3.22603)
KEYWORD_WEIGHT_SEMICOLON     = -2.96142   (was -4.93396)
KEYWORD_WEIGHT_URL_OR_NUMBER = -0.51492   (was -2.80469)
```

The now-negative bias means a zero-signal keyword-led comment defaults to
ABSTAIN instead of YES — the intentional asymmetric-risk tradeoff already
documented on `KeywordAmbiguityGate` ("a false skip is zero-cost, a false
positive is a visible bug"); 20 of 62 examples mismatch, all the rare
"keyword used as plain English adjective, zero mechanical signal" case,
accepted.

Fixing 8 of 9 regressed fixtures surfaced a second, distinct bug:
`test/real_code_regressions_54_inp.java` still failed on a stray-trailing-
period strip. `CommentFeatureExtractor.extract` always computed
`hasLeadingKeywordMatch` from the comment's *first* word regardless of the
caller's `targetWordIndex`, so the period-strip call site (whose
`targetWordIndex` correctly points at the *last* token) was wrongly gated by
leading-word ambiguity anyway. Fixed with a `targetWordIndex`-aware
`extract` overload that only sets `hasLeadingKeywordMatch` when
`targetWordIndex == 0`, wired through `MiscRuleCore.classifyComment`. A
real, narrow architectural gap independent of the weight fix.

With both fixes, flipped `Config.commentNormalizationClassifier`'s default
to `true` (`STATE_COMMON.md`'s config-key line updated to match):
**219/219 forward, 219/219 idempotency** — all 9 originally-regressed
fixtures pass, no new regressions. `gruClassifier` + `commentNormalization
Classifier` both `true` by default at this point (later partially reverted
— see the same-day "`gru-classifier` flipped back to `off`" session below).

### OPEN — GruTrainer follow-ups (deferred, not yet scheduled)

Fallback-write-on-failure, a gradient-checking tool, and confusion-
matrix/precision/recall/F1 reporting were implemented immediately (see
commit history). These remain deferred, each changing training numerics,
output format, or runtime classifier behavior enough to need its own design
pass and explicit sign-off:

- **Break/resume support** — implemented in the 2026-07-31 session below;
  this item is now DONE, kept here only as a pointer.
- **Mini-batch training (16-32)** — implemented in the 2026-08-01 session
  below (user-commissioned); this item is now DONE, kept here only as a
  pointer.
- **Dropout before dense layer.** Needs a train/eval-mode switch — dropout
  must be disabled at inference in `GruClassifier`, not just training.
- **Learning-rate warmup + cosine decay.** Needs new hyperparameters
  (warmup steps, decay shape); changes training numerics/output.
- **Automatic abstain-threshold tuning.** Needs a labeled validation slice
  and a chosen objective (max F1? fixed precision target?); touches the
  classifier's runtime abstain logic, not just the trainer.

### `acquire_corpus.sh` secret redaction + earlier dedup (2026-07-30)

Added `tools/gru/redact_secrets.py`: scrubs likely API keys/tokens (Google,
AWS access-key-id, GitHub, Stripe, OpenAI/Anthropic, Slack named-prefix
patterns, plus a narrow generic high-entropy `key=`/`secret=`/`token=`/
`password=`/`access_key=`/`auth=` fallback) from comment text, replacing
matches with `[REDACTED]`. Wired into `acquire_corpus.sh` right after
`extract_comments.py`, before Pool A/B extraction — so a leaked secret never
reaches any corpus file. Smoke-tested against synthetic examples of each
format (correctly redacted) and a plain-English/commit-hash control
(correctly left alone).

Also moved exact-duplicate-line dedup from the Makefile's end-of-pipeline
step into `acquire_corpus.sh` itself, running per-source right after
redaction. Smoke run (`--only eCxx`): 45357 raw comments → 27710 after
dedup, 117 Pool A / 189 Pool B candidates.

### DONE — further `CommentClassifier` NO-producing gates

Both deferred items from this TODO are now done: commented-out code (2026-
07-31 session below) and multi-line license/copyright blocks (2026-07-31
session below).

---

## 2026-07-31 session: commented-out-code `CommentClassifier` NO-gate

A comment ending in `;` combined with a second, independent code-shape
signal now resolves `NO` (Gate 1d, between Gate 1c's slash-list gate and
Gate 2's keyword-ambiguity gate). A bare trailing `;` alone was confirmed
unsafe by the earlier disagreement-sampling pass (~8% false-positive rate on
real prose ending a clause with a semicolon) — the same asymmetric-risk
shape as the rejected leading-hyphen gate (2026-07-30 "Bug 2" note below) —
so this gate requires a second signal.

**`CommentedOutCodeGate.looksLikeCommentedOutCode(String)`** (new file,
mirrors `DecorativeSeparatorGate`'s presence-based shape) returns true only
when the text ends with `;` (trailing whitespace ignored) **and** at least
one of four regex sub-patterns matches: **call-shape** (identifier,
optionally dotted, immediately followed by `(`, no space — real English
essentially never writes "word(" with no space); **assignment-shape**
(identifier, optionally array-indexed, followed by a bare `=`, excluding
`==`/`!=`/`<=`/`>=`); **increment/decrement-shape** (`++`/`--` directly
adjacent to a word character on at least one side, narrower than a bare
`--` so a spaced prose em-dash doesn't match); **typed-declaration-shape** (a
type-looking word — known primitive/common-type keyword, sized-integer
alias, or capitalized identifier — followed by a plain identifier then `=`
or `;`, e.g. `Event event;`, `uint16_t ctr = 0;`; requires the *first* word
to look like a type specifically to avoid matching prose endings like
"...4-digit form;").

New `CommentFeatureVector.looksLikeCommentedOutCode` field (13th constructor
arg, computed in `CommentFeatureExtractor`, not `targetWordIndex`-scoped —
a whole-comment shape signal), both call sites updated.

**Testing:** `make test` 220/220 forward+idempotency, unchanged. 13
hand-picked smoke cases: all 9 real commented-out-code shapes from the
disagreement-sampling pass correctly resolve `NO`, both documented real-
prose false positives correctly resolve `YES`, 2 prose sanity checks
unaffected. Real-corpus check against `sample_default.txt` (RDD_KEY_217):
of 91064 YES-labeled lines, 1055 end in `;`; new gate fires `NO` on 739
(70%); ~200 manually inspected across Java/JS/CSS-embedded markup, zero
real-prose false positives. `GruAbstainResolverSelfTest.java` (JDK 21
recompile, standalone — not wired into `make test`): all checks passed.

**Not addressed by this session:** a separate corpus-generation bug where
string-literal fragments containing `//` get mis-extracted as comment text
(e.g. `Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";`-shaped
lines) — not yet root-caused to `extract_comments.py` or
`GenerateSampleDefault.java`; a corpus-generation concern, not a
`CommentClassifier` gap. Still open.

---

## 2026-07-31 session: multi-line license/copyright-block `CommentClassifier` NO-gate

Same overall pattern as the commented-out-code gate above: new gate class,
new `CommentFeatureVector` field, wired as Gate 1e (between 1d and Gate 2).

`tools/gru/README.txt`'s hand-labeling section had a worked example labeling
a multi-line license header NO under "spans 2+ newlines, a license block not
a single sentence → NO" — flagged as too blunt to port verbatim (would
misfire on ordinary multi-line prose paragraphs, common in this codebase's
own Javadoc style).

**`LicenseBlockGate.looksLikeLicenseBlock(String)`** requires **both**: (1)
**primary signal** — spans 2+ newlines *and* its last non-whitespace
character is not sentence-ending punctuation (`.`/`!`/`?`) — not used alone,
since some real license headers (including this project's own file header)
do end in a period, an accepted false-skip; (2) **confirming signal** —
explicit copyright/license vocabulary anywhere in the text (`Copyright`,
`(C)`, `SPDX-License-Identifier`, `Licensed under`, `All rights reserved`,
`Redistribution and use`, `Permission is hereby granted`,
`WITHOUT WARRANTIES`/`WARRANTY`). A decorative-border-line confirming signal
was tried first and **rejected** — real-corpus testing found hundreds of
ordinary decorative section-banner comments (e.g. `apache/ant`'s
`===...===`-framed XML headers, no license content) sharing the border shape
without being license blocks, the same over-broad-single-signal failure
mode the leading-hyphen gate was rejected for. License vocabulary alone
proved narrow enough.

New `CommentFeatureVector.looksLikeLicenseBlock` field (14th constructor
arg, same not-`targetWordIndex`-scoped treatment), both call sites updated.

**Testing:** `make test` 220/220 forward+idempotency, unchanged. 9
hand-picked smoke cases: 4 real license-block shapes correctly `NO`
(README.txt's GNU-LGPL example, `STATE_COMMON.md`'s own fixture header, an
"All rights reserved" block, a border-plus-"Licensed under" block); this
project's own file header (ends in a period) correctly falls through to
`YES` (accepted false-skip, intentional); prose/TODO-block/single-line-
mention/`?`-ending cases all correctly unaffected. Real-corpus check: an
initial vocabulary-or-decorative-border design fired on 599 of 91064 YES
lines with hundreds of false positives (decorative banners); dropping the
border branch reduced the fired count to 375, and manual inspection of the
first ~60 (spanning css/java/xml/js/c/cpp) found zero real-prose false
positives — every fired example genuine license/copyright text.
`GruAbstainResolverSelfTest.java` (JDK 21 recompile): all checks passed.

Closes both items from the "further `CommentClassifier` NO-producing gates"
TODO (commented-out code and license/copyright blocks) — now DONE.

---

## 2026-07-30 session: `gru-classifier` flipped back to default `off`

Evaluated the shipped `code-formatter-ai-assist-weights.json` against the 62
hand-labeled keyword-ambiguity examples in `tools/classifier_weights/
examples_{c,cpp,java,kotlin}.md` (converted to `GruEval`'s RDD_EXT_21
schema):

```
total=62 abstain=0 decided=62 correct=19 precision=30.6%
yesCorrect=19/19  noCorrect=0/43
```

The GRU predicted **YES on every example**, including all 43 that should be
NO — worse than the linear classifier's own 67.7% (42/62) on this identical
set. Root cause: `sample_default.txt` (the GRU's only training corpus at
that point) is auto-labeled *by the linear classifier itself*, so it never
contains the genuinely hard ambiguous-keyword-led NO cases (exactly the
ABSTAIN-path comments the linear classifier won't auto-label, and per
RDD_EXT_19 the hand-labeled examples set is deliberately never merged into
`sample_default.txt`). The GRU learned "default to YES."

**Fix:** `Config.gruClassifier` default flipped back to `false`;
`README.md`/`STATE_COMMON.md` updated to `off` with a pointer here. Does not
touch `commentNormalizationClassifier` (stays `on` — linear classifier path
only, unaffected by this finding).

**Still outstanding at the time:** teaching the GRU the hard cases requires
training data that actually contains them. Partially addressed by a later
same-day session (below), which improved precision to 50% but still below
the 67.7% baseline — `gru-classifier` remains `off` as of the most recent
session in this file.

## 2026-07-30 session: self-formatting dogfood-and-adopt run — found/fixed a `CommentClassifier` false positive (slash-separated lists)

First run of `STATE_COMMON.md`'s dogfood-and-adopt process against the
formatter's own `src/` tree. Spot-check found 5 wrong capitalizations, all
sharing one shape — a comment starting with a slash-separated list of code
identifiers/keywords not in the language's keyword set (`sizeTokens/
initTokens`, `open/final/abstract/sealed`, `val/var`,
`constexpr/consteval/constinit`, `wx/uh/az/ar/ah`). Root cause: none of
these leading words are recognized keywords, so `hasLeadingKeywordMatch` was
false and every one fell into `CommentClassifier`'s "main path"
(`BIAS=1.0`, always YES) — a leading word directly followed by `/` (no
whitespace) was never checked anywhere, keyword or not.

**Fix:** new `CommentFeatureVector.leadingWordFollowedBySlash` field
(`targetWordIndex == 0` scoped) and a new **Gate 1c** (between the
decorative-separator gate and the keyword-ambiguity gate) returning `NO`
whenever it fires — independent of keyword membership, so it catches
non-keyword identifier lists too. Accepted false-skip risk: genuine English
"a/b" constructs (`and/or`, `km/h`) at a comment's start are rare enough
that occasionally leaving one lowercase costs nothing. `make test`: 220/220
forward+idempotency, unchanged.

After the fix, re-ran the dogfood process from step 1 (clean fixed-point
both rounds, zero false positives) and adopted the output into the real
`src/` tree (71 files + `GruAbstainResolverSelfTest.java` for the new
constructor arg); rebuild `make test`: 220/220. See git history for the
diff.

### 2026-07-30 session: extended self-formatting to `tools/*`/`tools/classifier_weights/*`, found/fixed a JS shebang-mangling bug

Ran the same process against the 36 supported-language files under
`tools/*`/`tools/classifier_weights/*` (Java/Python/JS). Spot-check surfaced
two bugs before adoption:

**Bug 1 (real formatter bug, fixed in `src/`):** `#!/usr/bin/env node`
shebangs in `tools/verifiers/*.js` were corrupted, because `#` is only
treated as a preprocessor directive for C/C++, so JS fell through to normal
tokenization and `enforceSemicolonInsertion` appended a stray `;` at line
end, breaking the shebang. A first fix (emitting it as `COMMENT_LINE`)
introduced a second bug (`MiscRuleCore.enforceCommentStyle` assumes every
`COMMENT_LINE` starts with literal `//` and mangled it). **Final fix:** a
dedicated `TokenType.SHEBANG` (added to `Token.isGapToken`, skipped like any
comment token but never `//`-prefix-rewritten) plus
`TokenizerCurly.emitShebangLine()`, dispatched only at `pos==0 && c=='#' &&
peek(1)=='!'`. `make test`: 220/220, unchanged.

**Bug 2 (comment-classifier false positive, hand-fixed per-occurrence, NOT
gated):** two comments in `GruAbstainResolverSelfTest.java` started with a
hyphenated config-key literal (`gru-classifier`, `gru-weights-path`) that
got wrongly capitalized. A blanket `leadingWordFollowedByHyphen` gate was
tried and **rejected** — it also suppressed capitalization of legitimate
English hyphenated compounds (e.g. `non-negative` → `Non-negative` in
`test/c_comments_inp.c`), a real regression since a leading hyphen (unlike
`/`) is common in ordinary English prose. **Decision (user-confirmed):**
revert the hyphen gate entirely, hand-edit the two comments instead
(reworded so the config-key literal doesn't start the comment). No
classifier change survived — if a similar case recurs, prefer rewording over
a new blanket gate.

After both fixes, re-ran the process (clean, `make test` 220/220) and
adopted all 36 files. Additionally verified beyond the standard process
(these files aren't exercised by `src/`'s `make test`): all `.js` files via
`node --check` plus a real end-to-end run, all `.py` via `python3 -m
py_compile`, all `.java` including the Kotlin-compiler-dependent files
(needing `~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib` on the classpath) and
the JDK11+-dependent files (needing `/opt/openjdk-21_linux-x64_bin/jdk-21`)
— all compiled cleanly.

### OPEN — LLM-assisted disagreement sampling against `sample_default.txt`

Discussed 2026-07-30: a full LLM relabel of `sample_default.txt` (92039
lines, ~$few-$150 depending on batching) would just reproduce the rule-based
classifier's existing blind spots, since the corpus is auto-labeled by that
same classifier. Agreed direction: use an LLM to find *disagreements* on a
small stratified sample, not relabel everything. Plan for whoever picks
this up:

1. **Sample** — pull a stratified few hundred lines (all/most of the
   existing NO lines + a random YES slice):
   ```bash
   grep -v '^#' tools/gru/sample_default.txt | awk -F'\t' '$2=="NO"' > /tmp/sample_no.tsv
   grep -v '^#' tools/gru/sample_default.txt | awk -F'\t' '$2=="YES"' | shuf -n 300 --random-source=<(yes 42) > /tmp/sample_yes.tsv
   cat /tmp/sample_no.tsv /tmp/sample_yes.tsv > /tmp/sample_for_llm.tsv
   ```
2. **Label independently via LLM** — batch many lines per call (RDD_EXT_21
   schema), ask for an independent YES/NO judgment without showing the
   existing label (avoid anchoring); keep batches small so one bad
   completion doesn't corrupt the run.
3. **Diff against existing labels** — any disagreement is a candidate
   genuinely-hard case; should be a small set (tens, not thousands).
4. **Hand-verify only the disagreements**, then append confirmed ones as new
   hard examples (append-only, never bulk-overwrite the existing labels).

**First pass already run by hand (2026-07-30, self-judged, no LLM API call
needed)** — see findings below, which directly motivated the
commented-out-code gate. Steps 2-4 above (actual LLM-assisted labeling) are
still not done.

### Findings from the first disagreement-sampling pass (2026-07-30)

Two distinct findings:

1. **[ACTIONABLE, since resolved — see the 2026-07-31 commented-out-code gate
   above] Commented-out code mislabeled YES is common, not rare.** An
   initial small stratified sample (100 NO + 100 YES) found only one
   disagreement (0.5%) — weak evidence on its own. Scaling to the full
   91064-line YES pool filtered for lines ending in `;` found **984
   candidates (~1.1%)**; spot-checking ~50 found the large majority
   genuinely commented-out code (`fmap[j] = a;`, `blockNo++;`, `ch = 0;`,
   `System.out.println(...)`, `assertTrue(...)`, `uint16_t ctr = 0;`,
   `Event event;`), spanning C/C++/Java/JS. **Important caveat confirmed by
   this pass:** a bare "ends with `;`" signal alone is unsafe — a 25-line
   spot-check found ~8% genuine English prose ending a clause with a
   semicolon (e.g. "...cannot be expressed in RFC 3339's 4-digit form;") —
   the same asymmetric-risk shape as the rejected hyphen gate, which is why
   the eventual gate requires a second shape signal. Also a
   **live-formatting-correctness finding**, not just a training-corpus
   concern, since `CommentClassifier`'s rule-based gates are always live
   regardless of `gru-classifier`'s value.
2. **[SEPARATE BUG, still open — a corpus-generation issue, not a
   `CommentClassifier` concern]** A cluster of the 984 candidates are
   DTD/URL string-literal fragments with no leading space (e.g. `Sun
   Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";`) that look like
   pieces of ordinary string literals containing a `//` substring,
   misread as a line-comment start by the comment-extraction step
   (`extract_comments.py` and/or `GenerateSampleDefault.java`'s scan — not
   yet root-caused to which one). Not yet investigated further.

### DONE (2026-07-31) — `explicit_vocab.txt` contamination filter

User inspection of `tools/gru/explicit_vocab.txt` found personal-name and
narrow-domain-jargon tokens (`Aloysius`, `Indrayanto`, `Red`, `LUTs`,
`OLED`, `WIZnet`) ranked as "common word" vocabulary. Root cause:
`build_vocab.py`'s frequency counter had no source-diversity requirement —
a token common across virtually all 16 acquired sources (`value`, `buffer`)
scored the same as one that's merely *locally* frequent within one or two
of the user's own repos.

**Fix implemented and verified same session:** `build_vocab.py` now takes a
directory of per-source `comments_<name>.txt` files, computes document
frequency (distinct sources containing the word) alongside raw count, and
ranks eligible common words by `(doc_freq desc, raw_count desc)` — a word
needs `--min-sources` (default 2) distinct sources to be eligible at all.
Ran a real 16-source `acquire_corpus.sh` and regenerated
`explicit_vocab.txt`: 9684 eligible words at `--min-sources=2`, comfortably
filling all 3346 common-word slots (154 keyword slots unchanged); verified
the 5 contaminating tokens are gone, and `Red` legitimately survives
(genuine 2+-source cross-source hit).

**Safety-window finding:** confirmed the *already-trained*
`code-formatter-ai-assist-weights.json` is unaffected by reordering
`explicit_vocab.txt` regardless of `gru-classifier`'s config value —
`GruWeights` embeds its own `explicitVocab` snapshot inside the trained JSON
(`GruClassifier.java:95`), never re-reading the on-disk vocab file at
inference time. So regenerating the vocab file only affects the *next*
`make gru-train` run's embedding-row layout — safe unconditionally, not just
during a `gru-classifier=off` window. `make test`: 220/220, unchanged (vocab
contents don't affect the rule-based path).

---

## 2026-07-31 session: `GruTrainer` break/resume checkpointing

Implements the "Break/resume support" item deferred above. Before this,
`bestWeightsJson` lived only in memory until the run's end; a kill mid-run
lost all progress with no resume path.

**Two binary checkpoint files**, derived from `--out`'s path
(`<out>.ckpt-current.bin` / `<out>.ckpt-best.bin`):
- **Current-weights checkpoint** — overwritten once per epoch. Full
  resumable state: weights, vocab, Adam optimizer's first/second-moment
  accumulator arrays (needed so resume doesn't restart momentum from
  scratch), and scalar run state (`epoch`, `epochsSinceImprovement`,
  `bestValidationLoss`, `learningRate`/`maxEpochs`/`patience`, RNG `seed`,
  Adam `step` counter — needed for bias-correction continuity). This is the
  file `--resume=<path>` expects.
- **Best-weights checkpoint** — overwritten only on validation-loss
  improvement. Weights+vocab only, no Adam/run state; resume reads it as a
  sibling to recover the true best-so-far weights (the current-weights
  checkpoint only ever holds the latest epoch's weights).

**Binary format:** `DataOutputStream`/`DataInputStream` over buffered
streams (no external library — zero-third-party-dependency convention),
chosen over JSON for I/O speed on every-epoch writes of a multi-MB blob.
Header (`magic=0x47525543`, `formatVersion=1`, `kind` 0=best/1=current) +
shared weights block + (current-only) scalar run-state block + Adam-moments
block. All arrays are shape-prefixed and validated on read, so a
truncated/corrupt checkpoint fails loudly rather than silently misreading.
Written via temp-file-then-atomic-rename (`Files.move` +
`REPLACE_EXISTING`) so a kill mid-write can never leave a corrupt
checkpoint.

Both files are a resume/recovery safety net only, never a persistent
artifact — deleted via `Files.deleteIfExists` on normal successful
completion. Added to `.gitignore`: `*.ckpt-current.bin`,
`*.ckpt-current.bin.tmp`, `*.ckpt-best.bin`, `*.ckpt-best.bin.tmp`.

**`--resume=<checkpoint-path>` — implemented, not deferred**, since it
turned out not disproportionately more work than the checkpoint I/O itself.
Loaded early in `main`, before RDD_EXT_18 hyperparameter defaults are
computed, so `--lr`/`--epochs`/`--patience`/`--seed` fall back to the
checkpoint's recorded values unless explicitly overridden (epochs/patience
can still be raised to extend a resumed run). The vocab always comes from
the checkpoint's own embedded snapshot, never re-derived, so resume can
never shift embedding-row indices out from under the resumed weights.
`random` is re-seeded from the checkpoint's own `seed`, so **the train/
validation split itself is reproduced exactly** on resume. Epoch loop bound
changes to `startEpoch = resumed.epoch + 1`.

**Documented non-bit-reproducibility caveat:** only the RNG *seed* is
persisted, not `java.util.Random`'s internal state, so while the initial
split is exactly reproduced, the *per-epoch* shuffle order from that point
onward diverges from an uninterrupted run. Documented as accepted/
deliberate, not a bug — resume still trains validly (same data, same
optimizer state, same architecture).

**Testing performed:** `make test` 220/220 unchanged (change confined to
non-shipped `tools/gru/GruTrainer.java`); `javac -source 8 -target 8`
compiles clean. Normal-run test confirmed checkpoint files exist during a
run and are cleaned up after. Kill-and-resume test (`kill -9` mid-epoch 3):
both checkpoint files survived, resume recovered `epoch=2,
epochsSinceImprovement=0, bestValidationLoss=0.0604511`, training loss
continued decreasing smoothly across the resume boundary (confirming both
weights and Adam moments were faithfully restored), final weights file had
a sane confusion matrix (`tp=79 fp=0 tn=1 fn=0 precision=1.00000`),
checkpoints cleaned up after. Full cycle confirmed working end to end.

**2026-08-01 follow-up: `make gru-train` auto-resume.** `--resume=<path>`
above only worked when invoked manually — the `gru-train` Makefile target
never passed it, so a checkpoint left by an interrupted `make gru-train` run
sat unused; the next `make gru-train` silently started over from scratch.
Fixed: the target now checks for `$(GRU_WEIGHTS_OUT).ckpt-current.bin`
before invoking `GruTrainer` and, if present, adds
`--resume=$(GRU_WEIGHTS_OUT).ckpt-current.bin` automatically (printing
`gru-train: found ..., resuming` first); `GRU_TRAIN_ARGS` values still
override the checkpoint's stored hyperparameters (`GruTrainer`'s
`getOrDefault(key, resumed-value)` pattern is override-priority regardless
of flag order). No flag change needed for a normal fresh run — absent a
checkpoint file, behavior is unchanged. Verified end to end (user): killed
a `make gru-train` run mid-epoch, re-ran `make gru-train`, confirmed it
detected the checkpoint and resumed. Documented in `tools/gru/README.txt`'s
`GruTrainer.java` section. Also folded in unrelated cosmetic print-format
tweaks to `GruTrainer.java`'s progress/summary log lines (comma-separated
fields, `%2d` epoch padding, quoted output path) made by the user in the
same session.

**Files changed:** `tools/gru/GruTrainer.java` only (checkpoint constants,
binary I/O helpers, `ResumeState`/`LoadedWeights` holders, `--resume` CLI
flag) + `.gitignore` (4 new patterns). No `src/` file touched.

**Not attempted this session** (still open from the GruTrainer-follow-ups
list above): mini-batch training, dropout, LR warmup/decay, automatic
abstain-threshold tuning. The Adam bias-correction and tokenize-once-cache
items from that list were confirmed already done before this session.

---

## 2026-07-31 session: real-corpus GRU retrain re-evaluated — improved (30.6% → 50.0%) but still below the 67.7% baseline

After this session's `explicit_vocab.txt` fix, the new `CommentedOutCodeGate`/
`LicenseBlockGate` NO-gates, and merging `classifier_weights/examples_*.md`
into `sample_default.txt` (92046 → 92308 lines, NO rows 975 → 3069), a real
training pass (`--threads=3 --epochs=3 --patience=2`, 73841 train/18460
validation) converged fast with correct early stopping (val loss bottomed
epoch 1 at 0.0393061, worse both subsequent epochs). Final validation
confusion matrix (majority-YES-dominated): `tp=17763 fp=95 tn=525 fn=77
precision=0.99468` — looks excellent but mostly reflects "predicts YES on
ordinary prose correctly," not resolution of the hard cases.

Re-ran the same 62-example hard-case benchmark:

```
total=62 abstain=0 decided=62 correct=31 precision=0.5
yesCorrect=20/20  noCorrect=11/42
```

| | precision | YES correct | NO correct |
|---|---|---|---|
| Linear classifier (baseline) | 67.7% (42/62) | — | — |
| GRU, 2026-07-30 (pre-fix corpus) | 30.6% (19/62) | 19/19 | 0/43 |
| **GRU, this session (post-fix corpus)** | **50.0% (31/62)** | 20/20 | 11/42 |

Real progress (no longer degenerate all-YES), but still below the linear
classifier's 67.7% — `gru-classifier` stays `off`. `Config.gruClassifier`
unchanged (`false`); `commentNormalizationClassifier` unchanged (`true`).

**Why 50% and not higher, and how to improve it further (still open, no
work done yet on any of these):**

1. **62 hand-labeled examples is tiny relative to 73841 auto-labeled
   examples (~0.08%)** — online SGD sees each of the 62 once per epoch,
   easily swamped by the majority-YES gradient. Highest-leverage fix:
   **grow the hand-labeled hard-case set itself**
   (`tools/classifier_weights/examples_*.md`), not training mechanics.
2. **Consider oversampling/upweighting the hand-labeled hard cases** —
   repeat each of the 62 rows N times, or add a per-example loss weight
   (`GruTrainer` has no such notion today — a small trainer change).
3. **The 62-example benchmark has no held-out split** — all 62 are used for
   both training and evaluation, so 50%/67.7% measures training-fit, not
   generalization (the linear classifier's 67.7% baseline has the same
   caveat, so the comparison is apples-to-apples but neither number is a
   trustworthy generalization estimate).
4. **The rule-based NO-gates (commented-out-code, license-block,
   decorative-separator, slash-list) generalize the "obviously not a
   sentence" end of the NO spectrum, not the "ambiguous leading keyword"
   end this 62-example benchmark targets** — growing `sample_default.txt`
   via more source repos won't move this specific benchmark much further.
5. **Architecture/training-mechanics changes (mini-batch, dropout, LR
   schedule, more epochs) are unlikely to be the bottleneck** — fast
   single-epoch convergence + immediate overfit point at a data-
   representation problem (1-3 above), not an optimization one. Not
   recommended as the next thing to try before growing/reweighting the
   hard-case corpus.

## 2026-07-31 session: extended `classifier_weights` to every language reaching `KeywordAmbiguityGate`, regenerated `sample_default.txt`

Extended `tools/classifier_weights/*.md` with a new file per supported
language reaching the gate, extended the 4 existing files, regenerated
`sample_default.txt` — explicitly **no retraining** this session.

**Investigation (reachability):** traced the only call path into
`classifyComment`/`KeywordAmbiguityGate`: `MiscRuleCore.enforceCommentStyle`,
whose only call site is `FormatterCurly.java:272`, only instantiated for
curly-brace-family languages (`Lang.isCurly = isC || isCpp || isJava ||
isKotlin || isJs || isTs`). `MiscRuleIndent` (Python3) and `MiscRuleTags`
(XML/HTML5) never call it. **Conclusion: only c, cpp, java, kotlin, js, ts
ever reach `KeywordAmbiguityGate`** — json/json5/css/yaml/toml/xml/html5/
python3 are structurally unreachable; no `examples_<lang>.md` work is
meaningful for any of them.

**`KeywordAmbiguityGate.java`:** js/ts previously had no dispatch branch and
silently fell through to `KEYWORDS_C` (wrong — JS shares almost no keyword
vocabulary with C). Added `KEYWORDS_JS` (39 real JS keywords) and
`KEYWORDS_TS` (20 TS-only additions layered on top, same pattern as the
existing `isCpp` branch), plus dispatch branches (`isTs` checked before
`isJs`).

**New `examples_js.md`/`examples_ts.md`** (18 rows each, same
format/reasoning depth as the existing 4 files, both include the
zero-mechanical-feature-NO shape established as essential coverage by the
2026-07-30 KEYWORD_BIAS session). **Extended the 4 existing files**: `c`
+4, `cpp` +4, `java` +4, `kotlin` +3 — targeting `KEYWORDS_*` members with
zero prior example-row coverage. Total: 51 new/changed rows across 6 files
plus 2 brand-new files. `convert_classifier_weights_examples.py`'s
`LANG_BY_STEM` extended with the two new stems (without this a new
`examples_<lang>.md` is silently skipped by `glob.glob`).

**Golden-fixture fallout (expected, not a bug):** `test/js_comments_inp.js`'s
`// class-level implementation note` previously capitalized (JS had no
"class" keyword recognized at all); now correctly resolves to lowercase per
the same asymmetric-risk `KEYWORD_BIAS` design already accepted for other
languages. Updated the golden fixture to match. `make test`: **221/221
forward, 221/221 idempotency** after the update.

**`make gru-acquire-corpus` regeneration:** full 16-source run,
`convert_classifier_weights_examples.py` picked up all 6 `examples_*.md`
files (up from 4), wrote 113 hand-labeled examples (was 62) into
`sample_default.txt` (92336 lines: 89590 YES / 3081 NO auto-labeled from
92671 kept comments). Per-language counts confirm js/ts landed (`js`=2465,
`ts`=72). Committed per RDD_KEY_217. **No training run performed.**

**Next step, if pursued:** grow `examples_*.md` further, re-run the
`GruEval` 62/125-example comparison, watch `noCorrect` for climbing past
21/42 (where the GRU would start beating the linear classifier's absolute
NO-correct count) before reconsidering `gru-classifier=on`.

## 2026-07-31 session: linear classifier weights re-derived from the extended example sets

Follow-up: re-derived `CommentClassifierWeights`'s constants from the new
125-example set (was 62).

**First attempt regressed `KEYWORD_BIAS` back positive** (`+0.21890`) —
reopened the 2026-07-30 zero-signal-bias bug. Root cause: the new
`examples_js.md`/`examples_ts.md` rows leaned heavily on zero-feature YES
prose without a matching zero-feature NO count, shifting the combined
zero-signal split back toward YES-heavy (`derive_weights.py` has no
real-world class-frequency prior — it fits whatever ratio is present in
`DATASET`).

**Fix:** added 6 more zero-feature NO rows to each of `examples_js.md` and
`examples_ts.md` (real keyword usage, not English prose) — 125 total
examples. Re-derived:

```
KEYWORD_BIAS                 = -0.08711
KEYWORD_WEIGHT_PAREN         = -3.08818
KEYWORD_WEIGHT_ARROW         = -1.57140
KEYWORD_WEIGHT_SEMICOLON     = -3.57490
KEYWORD_WEIGHT_URL_OR_NUMBER = -0.93665
```

82/125 classified as labeled; all 43 mismatches are the same accepted
asymmetric-risk tradeoff. Copied into `CommentClassifierWeights.java`;
`weights.md` updated. `make jar` + `make test`: 221/221 forward,
221/221 idempotency (classifier defaults `off` so this alone couldn't have
broken `make test`, but confirms no build breakage).

**Not re-run (still open if picked up):** `make gru-acquire-corpus` (the
newly-added rows would need folding into `sample_default.txt` again via
`classifier_weights_examples.tsv` if the GRU corpus should reflect them
too) and no GRU training (out of scope for this linear-weights-only task).

**Current overall state as of this entry:** `gru-classifier` defaults
`off` (GRU precision 50% vs. linear classifier's 67.7% on the 125-example
hard-case benchmark — see the session above); `comment-normalization-
classifier` defaults `on` (linear rule-based classifier path, unaffected).

---

## 2026-08-01 session: `GruTrainer` mini-batch training (user-commissioned, item 40/41)

Implements the "Mini-batch training (16-32)" item deferred above, per
explicit user sign-off. Replaces per-example forward+backward+immediate-
Adam-step with real mini-batching: gradients from a configurable batch of
examples are averaged, then one Adam step is applied per batch.

**Threads-vs-batching composition:** the two axes compose orthogonally.
`--batch-size=N` (new, default 16) controls how many examples' gradients
get averaged before one Adam update; `--threads=N` (pre-existing) controls
how many of *one batch's* forward/backward computations run in parallel,
reusing the same `computeBatch`/`ExecutorService` machinery that
previously computed `threads`-sized chunks directly — only change needed
was chunking the epoch's example list by `batchSize` instead of `threads`,
then averaging the batch's gradients instead of applying each one's Adam
step individually.

**Implementation:** new `averageGradients(List<ComputedGradient>)` picks
the first non-null entry's `GruClassifier.Gradients` as an in-place
accumulator (its constructor is package-private, not callable from
`GruTrainer`'s default package, so mutating an existing instance via
public fields is the workaround), adds remaining non-null entries in
(`addGradientsInto`/`addInto`, mirroring `clipGradients`/`scale`'s
field-walk shape), then divides every field by the non-null count
(`scaleGradients`). Entries skipped for out-of-range `targetWordIndex` are
excluded from both sum and divisor — this is also what makes a partial
final batch average correctly (divisor is the actual non-null count, never
a hardcoded `batchSize`). Returns `null` (no Adam step) only if every
example in the batch was skipped.

**Adam `step` counter:** now increments once per batch (right before
`adam.apply`), not once per example — required for correct bias-correction
under mini-batch Adam.

**Gradient clipping unchanged deliberately:** `computeGradient` (per-
example) still clips each example's gradient before `averageGradients` —
clipping stays pre-average, per example. This is why `--batch-size=1` is
numerically close to, but not bit-identical to, the old per-example
behavior (documented in README.txt).

**`--batch-size` is a resumable hyperparameter**, same override-if-
specified-else-checkpoint-value pattern as `--lr`/`--epochs`/`--patience`/
`--seed` — new scalar in the checkpoint's run-state block
(`ResumeState.batchSize`). Binary-format change: `CHECKPOINT_FORMAT_VERSION`
bumped 1→2 (a leftover version-1 checkpoint is rejected by the version
check rather than silently misread — acceptable since checkpoints are
ephemeral, never committed, always deleted on normal completion).

**Validation** (all against `/tmp` copies compiled with `javac -encoding
UTF-8 -source 8 -target 8` against the real read-only `target/classes` on
the classpath — never touched the live `make gru-train` process/
checkpoints/`target/`): compile clean; `--check-gradients=5` against
`sample_examples.txt` → `maxRelativeError=0.000001 (PASS)` (confirms
`backward` untouched); tiny training runs (12 train/2 validation,
`--epochs=5 --seed=1`) at batch-size 1/8/16 all completed with decreasing
loss each epoch (1: 1.126→0.515; 8: 1.123→0.869; 16: 1.120→0.939 —
batch-size=1 fastest per-epoch drop, consistent with degenerating toward
old per-example-Adam-step behavior); kill/resume smoke test
(`--epochs=500 --patience=500 --batch-size=4 --seed=7`, `kill -9` mid-
epoch-4) recovered `epoch=3, epochsSinceImprovement=0,
bestValidationLoss=1.0161895` and correctly restored `batchSize=4` from
the checkpoint without `--batch-size` on the resume command line — loss
continued decreasing smoothly (0.857→0.626 over epochs 4-8).

**Isolation:** change confined to `tools/gru/GruTrainer.java` (+
`README.txt` + this file); no `src/` touched; `make test` not run (not
part of its fixture suite), same scoping precedent as the 2026-07-31
checkpointing session.

**Files changed:** `tools/gru/GruTrainer.java` (new `--batch-size` flag,
`averageGradients`/`addGradientsInto`/`addInto`/`scaleGradients` helpers,
checkpoint version bump + `batchSize` field, javadoc) + `tools/gru/
README.txt` + this file.

**Not attempted this session:** dropout before the dense layer, LR
warmup/cosine decay, automatic abstain-threshold tuning. Per the
2026-07-31 "50%" session's finding #5, none of these (nor mini-batching
itself) were expected to move hard-case precision — numerics/perf change
only, no accuracy claim.

---

## 2026-08-01 session: `GruTrainer` learning-rate warmup + cosine decay (user-commissioned, item 41)

Implements the "Learning-rate warmup + cosine decay" item deferred above,
per explicit user sign-off. New CLI flags `--warmup-steps=N` (default 0)
and `--lr-min=N` (default 0.0), both resumable following the
`--batch-size` precedent (override-if-CLI-specified-else-checkpoint-value).

**Step-granularity:** driven by the same 1-based Adam `step` counter used
for bias-correction (one step per mini-batch), not by epoch number —
smoother/more correct under mini-batching than an epoch-granular ramp.

**Decay horizon:** reuses `--epochs` (`stepsPerEpoch * maxEpochs`,
`stepsPerEpoch = ceil(trainExamples / batchSize)`) rather than a third
duration concept. Not persisted separately — recomputes identically on
resume from the already-resumable `maxEpochs`/`batchSize`/`trainExamples`.
Overriding `--epochs` on `--resume` shifts the decay horizon accordingly,
same as raising `--epochs` already extends a resumed run.

**Gating:** `computeScheduledLr` returns `baseLr` unconditionally whenever
`warmupSteps <= 0` (the default) — unmodified invocation is byte-for-byte
the pre-existing flat-lr behavior. `--lr-min` only matters once
`--warmup-steps > 0`. Formula for step `s` in `(warmupSteps, totalSteps]`:
`lr = lrMin + 0.5*(baseLr-lrMin)*(1+cos(pi*progress))`,
`progress = clamp((s-warmupSteps)/(totalSteps-warmupSteps), 0, 1)` — the
clamp means a schedule that never reaches `totalSteps` (e.g. early
stopping) is cut off mid-curve, same posture as `maxEpochs` as an upper
bound today.

**Resumability:** `warmupSteps`/`lrMin` added to the checkpoint's run-state
block, same call sites as every other scalar. Binary-format change:
`CHECKPOINT_FORMAT_VERSION` bumped 2→3 (same low-cost-break precedent as
the 1→2 bump for `batchSize`).

**Observability:** epoch-summary line gained `lr=%9.7f` (last-applied LR
that epoch); start-of-run line gained `warmupSteps=%d, lrMin=%9.7f`.

**Validation** (same `/tmp`-copy/`javac -source 8 -target 8`-against-real-
`target/classes` methodology as the mini-batch session; never touched the
live `make gru-train` process/checkpoints/`target/`): compile clean;
`--check-gradients=5` → `maxRelativeError=0.000001 (PASS)` (confirms
`backward` untouched, no Adam/LR involvement in gradient-check mode);
backward-compat run (no `--warmup-steps`) held LR flat at `0.0010000`
across 5 epochs, confirming true no-op; schedule-enabled run (`--lr=0.01
--warmup-steps=3 --lr-min=0.0001 --batch-size=2 --epochs=10`, 12 examples
→ `stepsPerEpoch=6`, `totalSteps=60`) matched hand-computed formula values
closely at every checkpoint (step 3 = `baseLr` exactly; step 6 hand-calc
`0.009933` vs. printed `0.0099325`; step 12 hand-calc `0.009403` vs.
`0.0094034`; step 60 = `lrMin=0.0001` vs. printed `0.0001000`) — confirms
ramp-up, mid-decay, and floor-at-horizon all match intended shape; kill/
resume smoke test (`--epochs=200 --patience=200 --batch-size=3 --lr=0.02
--warmup-steps=5 --lr-min=0.001 --seed=7`, `kill -9` mid-epoch-6) recovered
`epoch=5, epochsSinceImprovement=4, bestValidationLoss=0.9171171` and
continued the decay curve smoothly (`lr=0.0122256` at first post-resume
epoch, monotonically decreasing after) rather than restarting warmup.

**Isolation:** confined to `tools/gru/GruTrainer.java` (+ `README.txt` +
this file); no `src/` touched; `make test` not run (not in its fixture
suite), same precedent as the two most recent `GruTrainer` sessions.

**Files changed:** `tools/gru/GruTrainer.java` (new `--warmup-steps`/
`--lr-min` flags, `computeScheduledLr` helper, epoch-loop wiring,
checkpoint version bump + two new persisted scalars, javadoc, print lines)
+ `tools/gru/README.txt` + this file. `Makefile`'s `GRU_TRAIN_ARGS`
deliberately left untouched (doesn't set `--warmup-steps`, schedule stays
off by default there too; file had unrelated pre-existing local changes
outside scope).

**Not attempted this session:** dropout before the dense layer, automatic
abstain-threshold tuning. Per the 2026-07-31 "50%" session's finding #5,
an LR schedule (like mini-batching) isn't expected to move hard-case
precision on its own — numerics/observability change only.

---

## 2026-08-01 session: real-corpus GRU retrain re-evaluated on the 125-example benchmark — improved (50.0% → 56.0%) but still below the 67.7% baseline

Real training pass with the post-mini-batch, post-schedule `GruTrainer`
(`--threads=3 --epochs=5 --patience=3 --progress-every=1000`, schedule off
i.e. `--warmup-steps=0`; interrupted mid-epoch-2 and resumed from checkpoint,
confirming resume still works under mini-batching): 73873 train/18468
validation. Best-validation-loss checkpoint landed at epoch 3
(`validationLoss=0.0356914`); epochs 4-5 got strictly worse
(0.0479, 0.0595) and were correctly discarded by best-checkpoint selection
(the written weights file's `bestValidationLoss` matches epoch 3 exactly)
-- `--epochs=5` simply ran out before `--patience=3` could fire on its own
(only 2 consecutive non-improving epochs happened, not 3).

Re-ran the hard-case benchmark via `GruEval` against the new weights, using
`convert_classifier_weights_examples.py` to regenerate the 125-row
RDD_EXT_21 tsv fresh from `tools/classifier_weights/examples_*.md` (not
reusing a stale copy):

```
total=125 abstain=0 decided=125 correct=70 precision=0.56 yesCorrect=40/43 noCorrect=30/82
```

| | precision | YES correct | NO correct |
|---|---|---|---|
| Linear classifier (baseline) | 67.7% (82/125) | — | — |
| GRU, 2026-07-31 (62-example benchmark, pre-mini-batch/schedule) | 50.0% (31/62) | 20/20 | 11/42 |
| **GRU, this session (125-example benchmark, post-mini-batch+schedule trainer)** | **56.0% (70/125)** | 40/43 | 30/82 |

Real progress: `noCorrect` climbed from 11/42 (26%) to 30/82 (37%), and
precision from 50% to 56%. Still below the linear classifier's 67.7% --
`gru-classifier` stays `off`; `Config.gruClassifier` unchanged (`false`).
The 62-vs-125 benchmark size differs from the prior comparison (the
125-example set is the current one, folded into `sample_default.txt` since
the 2026-07-31 js/ts session), so the 50%→56% delta is directionally
informative but not a clean apples-to-apples control for what specifically
moved it -- more hand-labeled rows in the training corpus itself is at
least as plausible an explanation as the mini-batch/schedule trainer
changes, and the two weren't isolated from each other in this run.

**Held-out-split caveat still applies unchanged**: this is the same
125-example set folded into the GRU's own training corpus via
`sample_default.txt`, so 56%/67.7% is still training-fit, not a verified
generalization estimate (same open caveat as the 2026-07-31 62-example
session, item 3 in that session's list).

**Decision:** per user instruction, next step is growing the hand-labeled
hard-case corpus itself (`tools/classifier_weights/examples_*.md`, item 1
in the 2026-07-31 session's list) rather than further trainer-mechanics
work -- see that session below for tracking once new rows land.

---

## 2026-08-01 session: root-caused and fixed the string-in-comment `extract_comments.py` bug

Root-caused the corpus-generation bug flagged open twice above (2026-07-31
sessions, "not yet root-caused to `extract_comments.py` or
`GenerateSampleDefault.java`"): DTD/URL string-literal fragments containing
a `//` substring (e.g. `Sun Microsystems, Inc.//DTD Enterprise JavaBeans
1.1//EN";`) were mis-extracted as comment text.

**Root cause, isolated to `extract_comments.py`'s `extract_c_style_comments`
(`GenerateSampleDefault.java` exonerated)**: the function's single left-to-
right scan recognizes `"//"`/`"/*"` as comment openers anywhere in the raw
source text, with no string/char-literal awareness at all -- a `//`
substring inside a `"..."` string literal was indistinguishable to the
scanner from a real line-comment opener.

**Fix:** added a third mutually-exclusive span type to the same single-pass
scan -- `"`/`'`-delimited literals, consuming to the matching unescaped
quote (or end of line, since no `C_STYLE_LANGS` language has a real string
literal spanning a newline) -- skipped over without checking for `//`/`/*`
inside. Backslash-escaped quotes (`\"`) are honored via a 2-char skip.
Same non-regex single-scan structure as the existing "//"/"/* */" mutual-
exclusion reasoning already documented in the function's own comment (a
`//`/`/*` found while inside a string span was already consumed as literal
text, so it can't be reinterpreted as a comment opener) -- this is that
same principle extended to a third span kind, not a new mechanism.

**Verified via a standalone smoke test** (scratch dir, not part of `make
test` -- `extract_comments.py` has no wired-in test suite): a
`Test.java` fixture combining the exact reported DTD-string shape, a `'/'`
char literal, a `"https://..."` string followed by a real trailing `//`
comment, a block comment containing a quoted `"// string"`, and an escaped-
quote string all extracted exactly the 4 real comments with zero string-
literal leakage. Also re-verified the function's own pre-existing edge
case (`///*mlen = n;`, a `//` comment containing a literal `/*`) still
resolves as one `//`-comment, unaffected by the new literal-skipping logic.

**Not yet done:** `sample_default.txt` was regenerated earlier this session
(before this fix landed) via `make gru-acquire-corpus`, so it still
contains whatever string-literal-leakage rows this bug produced from the
16-source corpus; re-running `make gru-acquire-corpus` now that the fix is
in would pick up the corrected extraction. No GRU retrain has happened
against a fix-applied corpus yet.

**Files changed:** `tools/gru/extract_comments.py` only (`extract_c_style_
comments`'s scan loop) + this file.

---

## 2026-08-01 session: grew the hand-labeled hard-case set (125 → 173 rows)

Direct follow-up to the "next step" decision above. `KeywordAmbiguityGate`'s
six per-language keyword lists (`KEYWORDS_C`/`KEYWORDS_CPP`/`KEYWORDS_JAVA`/
`KEYWORDS_KOTLIN`/`KEYWORDS_JS`/`KEYWORDS_TS`) were audited against each
`tools/classifier_weights/examples_*.md` file's existing rows to find
keywords with zero example coverage (the auto-labeled bulk corpus can never
contain these hard cases, since it's labeled by the same rule-based
classifier being evaluated -- only the hand-labeled files are real signal,
per this file's Background section). Many zero-coverage keywords exist per
language (e.g. C alone has ~24: `auto`, `case`, `char`, `const`, `double`,
`else`, `extern`, `float`, `for`, `goto`, `if`, `inline`, `int`, `long`,
`restrict`, `return`, `signed`, `sizeof`, `switch`, `typedef`, `union`,
`unsigned`, `volatile`, plus others already covered); this session targeted
the highest-value subset per file -- keywords that are also ordinary,
frequently-used English words (so a genuine prose-vs-code ambiguity exists)
-- rather than attempting exhaustive coverage in one pass.

**8 new rows per file (48 total, 125 → 173)**, each a YES-prose/NO-code-
reference pair per targeted keyword, following the zero-mechanical-feature
NO shape established since the 2026-07-30 KEYWORD_BIAS regression (no
paren/semi/url-num signal fires on either row of a pair -- the classifier
must resolve these from context alone, not a mechanical feature):

| File | Keywords targeted (previously zero rows) | Rows added |
|---|---|---|
| `examples_c.md` | `case`, `const`, `for`, `return` | 22-29 |
| `examples_cpp.md` | `catch`, `override`, `public`, `protected` | 20-27 |
| `examples_java.md` | `case`, `if`, `public`, `record` | 23-30 |
| `examples_kotlin.md` | `as`, `fun`, `if`, `return` | 16-23 |
| `examples_js.md` | `case`, `delete`, `throw`, `while` | 25-32 |
| `examples_ts.md` | `any`, `never`, `number`, `public` | 25-32 |

Existing rows were not renumbered or otherwise touched. Regenerated the
RDD_EXT_21 tsv via `python3 tools/gru/convert_classifier_weights_examples.py
tools/classifier_weights --out <scratch-path>` and confirmed a clean parse:
`wrote 173 hand-labeled example(s)` -- matches 125 + 48 exactly, no silent
skips, `LANG_BY_STEM` already covered all six stems.

**Not done this session (explicitly out of scope per instruction):**
`make gru-acquire-corpus` was not run, the GRU was not retrained, `sample_
default.txt`/`code-formatter-ai-assist-weights.json` were not touched, and
`weights.md`'s linear-classifier constants were not re-derived. Folding
these 48 rows into the training corpus, retraining, and re-benchmarking
against the (now 173-example) hard-case set is the next step for whoever
picks this up next -- expect the benchmark's `total`/`decided` denominators
to move from 125 to 173 once that happens, so a future comparison against
this session's 56.0%-on-125 number needs to account for the larger,
not-directly-comparable denominator.

---

## 2026-08-01 session: `derive_weights.py`'s `DATASET` made auto-extending from `examples_*.md`

Growing the hand-labeled hard-case set (previous session) exposed a latent
sync bug: `tools/classifier_weights/derive_weights.py`'s `DATASET` was a
hand-transcribed Python mirror of each `examples_*.md` row's feature
vector, not parsed from the files themselves -- the 48 new rows landed in
the `.md` files but had no `DATASET` entries at all, silently invisible to
the next re-derivation until someone remembered to transcribe them by hand.

**Fix:** replaced the literal `DATASET` list with `load_dataset()`, which
parses `examples_*.md` directly using the same header-name-column-lookup
convention already established by `tools/gru/convert_classifier_weights_
examples.py` (`LANG_BY_STEM` stem-to-language mapping duplicated rather
than imported, matching every other tool under `tools/`'s self-contained-
script precedent). Locates `paren?`/`arrow?`/`semi?`/`url/num?`/`Label`
columns by header name; `arrow?` defaults to `0` when the column is absent
(only `examples_kotlin.md` has it -- no other supported language has a
`->` branch-arrow shape). Row number and YES/NO cells map directly to the
existing `(source, index, paren, arrow, semi, urlnum, label)` tuple shape,
so `train()`/`report()` needed no changes.

**Verified:** `load_dataset()` reproduces every one of the previous 125
hand-transcribed rows identically (spot-checked `c #1`-`#21` and `kotlin
#2`'s `arrow=1`), and picks up all 173 current rows with zero manual
transcription. Re-running the script end to end now trains on the full
173-row set: precision 106/173 (61.3%, down from the old 125-row-set's
82/125 = 65.6% -- expected, the 48 new rows are deliberately the hardest
zero-mechanical-feature shape and this is a 4-feature linear model, not a
regression in the parser). New constants: `KEYWORD_BIAS=-0.05634`,
`KEYWORD_WEIGHT_PAREN=-3.10644`, `KEYWORD_WEIGHT_ARROW=-1.55819`,
`KEYWORD_WEIGHT_SEMICOLON=-3.59572`, `KEYWORD_WEIGHT_URL_OR_NUMBER=
-0.96329`.

**Not done this session:** copying these new constants into
`CommentClassifierWeights.java`/`weights.md`, and the `make gru-acquire-
corpus` rerun (now that the `extract_comments.py` fix above has landed)
are both immediate next steps, tracked separately below.

**Files changed:** `tools/classifier_weights/derive_weights.py` only + this
file.

---

## 2026-08-01 session: `weights.md`/`CommentClassifierWeights.java` re-derived, `sample_default.txt` regenerated with the `extract_comments.py` fix

Two remaining follow-ups from the sessions above, done together:

**1. Copied `derive_weights.py`'s new 173-example constants** into
`CommentClassifierWeights.java` and added a "2026-08-01 re-derivation"
section to `weights.md` following the existing 2026-07-31 section's
format. `make test`: **225/225 forward, 225/225 idempotency**, no
regressions from the weight change.

**2. Re-ran `make gru-acquire-corpus`**, now that the `extract_comments.py`
string-literal fix has landed. Confirms the fix's effect on the real
16-source corpus: 96836 → 96695 raw comments read (141 fewer spurious
extractions), auto-labeled NO rows 3103 → 3023 (80 fewer -- consistent
with the leaked DTD/URL string fragments, which skewed heavily NO given
their code-like shape, now correctly excluded). `sample_default.txt`:
92348 → 92952 lines. Confirmed zero remaining instances of the originally-
reported leakage pattern (`grep -c "DTD Enterprise JavaBeans"` → 0, was
present before the fix). 173 hand-labeled rows folded in correctly
(unchanged from the corpus-growing session, this run just regenerates the
auto-labeled bulk around them).

**Not done this session:** no GRU retrain against this corrected corpus,
no re-run of the 173-example hard-case benchmark against a fix-applied-
corpus model. That's the natural next step once picked up.

**Files changed:** `src/com/jxmake/formatter/classifier/
CommentClassifierWeights.java`, `tools/classifier_weights/weights.md`,
`tools/gru/sample_default.txt` + this file.

---

## 2026-08-01 session: grew the hand-labeled hard-case set further (173 → 221 rows)

Second corpus-growth pass, direct continuation of the "125 → 173" session
above. Re-audited `KeywordAmbiguityGate`'s six per-language `KEYWORDS_*`
sets against each `tools/classifier_weights/examples_*.md` file's *current*
rows (i.e. after the 173-row pass already landed) to find the next batch of
zero-coverage keywords, prioritizing (same criterion as last time) keywords
that are also ordinary, frequently-used English words so a genuine
prose-vs-code ambiguity exists.

**8 new rows per file (48 total, 173 → 221)**, each a YES-prose/NO-code-
reference zero-mechanical-feature pair per targeted keyword (no paren/semi/
url-num signal fires on either row of a pair, same shape as both prior
growth sessions):

| File | Keywords targeted (zero rows as of the 173-row set) | Rows added |
|---|---|---|
| `examples_c.md` | `if`, `long`, `else`, `switch` | 30-37 |
| `examples_cpp.md` | `friend`, `throw`, `try`, `using` | 28-35 |
| `examples_java.md` | `break`, `catch`, `finally`, `package` | 31-38 |
| `examples_kotlin.md` | `break`, `do`, `else`, `in` | 24-31 |
| `examples_js.md` | `break`, `catch`, `if`, `return` | 33-40 |
| `examples_ts.md` | `declare`, `is`, `protected`, `string` | 33-40 |

**Balance check:** each file's new batch is exactly 4 zero-feature YES rows
paired with 4 zero-feature NO rows (one pair per targeted keyword) — the
same 50/50 discipline the 2026-07-30 KEYWORD_BIAS session and the
2026-07-31 js/ts re-derivation session both needed to retrofit after a
YES-heavy batch flipped `KEYWORD_BIAS` positive; this batch can't repeat
that failure mode on its own since it introduces no skew per file. Existing
rows were not renumbered or otherwise touched.

Regenerated the RDD_EXT_21 tsv via `python3 tools/gru/
convert_classifier_weights_examples.py tools/classifier_weights --out
<scratch-path>` and confirmed a clean parse: `wrote 221 hand-labeled
example(s)` — matches 173 + 48 exactly, no silent skips.

**Not done this session (explicitly out of scope per instruction):**
`derive_weights.py` was not re-run, `CommentClassifierWeights.java`/
`weights.md` were not re-derived, `make gru-acquire-corpus` was not run,
the GRU was not retrained, and `sample_default.txt`/
`code-formatter-ai-assist-weights.json` were not touched. Folding these 48
rows into the linear-weights re-derivation and the GRU training corpus,
then re-benchmarking against the (now 221-example) hard-case set, is the
next step for whoever picks this up next — expect the benchmark's
`total`/`decided` denominators to move from 173 to 221 once that happens.

**Files changed:** `tools/classifier_weights/examples_{c,cpp,java,kotlin,
js,ts}.md` + this file.

---

## 2026-08-01 session: re-derived weights and regenerated the corpus for the 221-row set

Direct follow-up to the "173 → 221 rows" session above — the three steps
left explicitly out of scope there.

Re-ran `python3 tools/classifier_weights/derive_weights.py`: `KEYWORD_BIAS`
stayed negative (`-0.04180`, vs `-0.05634` at 173 rows), all four feature
weights stayed within a few percent of their 173-row values, decision
boundary stable across four consecutive growth passes now. 130/221
examples classified as labeled (58.8%, down from 106/173 = 61.3% — same
expected per-pass dilution as every prior growth session, not a
regression). Updated `CommentClassifierWeights.java` and `weights.md`'s
"2026-08-01 re-derivation (second growth pass, same day)" section to
match.

`make test`: 225/225 forward, 225/225 idempotency, no regressions.

Reran `make gru-acquire-corpus` (extraction pipeline unchanged since the
last run, so the Pool A/B counts and auto-labeled 93096 YES=90073/NO=3023
split are the same shape as before): wrote `tools/gru/sample_default.txt`
at 92809 lines, now including all 221 hand-labeled hard-case rows.

**Not done this session:** no GRU retrain against this corpus, no re-run of
the hard-case benchmark against a retrained model — both remain the
natural next step.

**Files changed:** `src/com/jxmake/formatter/classifier/
CommentClassifierWeights.java`, `tools/classifier_weights/weights.md`,
`tools/gru/sample_default.txt` + this file.

---

## 2026-08-02 session: hot-path flat-array/fused-gate refactor benchmarked (no measurable speedup); float vs double evaluated and REJECTED

**Context:** user-commissioned perf investigation. `GruTrainer.java`'s own
per-example loop only orchestrates batching/threading; the actual forward/
backward math lives in the shared `src/com/jxmake/formatter/classifier/gru/
GruClassifier.java` (`forward`/`backward`, used identically by both the
trainer and the shipped inference runtime). Auditing it found it was already
almost entirely flat `double[]`/`double[][]` (no `List<Double>`, no boxed
`Double`, no per-timestep objects) — `matVecInto`-style dot-product loops
already existed for the trainer's per-token scratch sums. So "Step 2" (flat-
array conversion) had little left to convert; the only real remaining waste
was that each GRU gate (`z`, `r`, `hTilde`) was computed via a 4-call chain
(`matVecInto` twice + `addVecInto` + `sigmoidVec`/`tanhVec`), each allocating
or looping over the full hidden dimension separately.

**Benchmark methodology (Step 0/1/3):** synthetic 288-train/72-validation-
example dataset (session-scratch only, `/tmp/.../gru_bench_small.tsv`, never
committed, never touches `sample_default.txt`), RDD_EXT_21 schema, 6
languages, mix of prose (YES) and code-shaped (NO) lines templated around 25
`KeywordAmbiguityGate` keywords. Ran `GruTrainer` directly (not via `make
gru-train`) with `--threads=1 --batch-size=1 --epochs=8 --patience=8
--seed=1` (single-threaded, single-example-per-step, to get a low-noise
per-example timing without thread-pool/batch-averaging variance), reporting
each epoch's `epochSeconds`.

**Baseline (pre-refactor):** epoch 1 (JIT warmup) 3.9s, epochs 2-8 steady at
3.6-3.7s/epoch (288 examples/epoch ⇒ ~12.5ms/example steady-state); total
run 32.8s (epochs 1-8).

**Step 2 refactor:** new `GruClassifier.gateInto(W, x, U, hPrev, b, out,
useSigmoid)` — one flat, straight-line, non-aliased loop per output row
(`out[i] = activation(dot(W[i],x) + dot(U[i],hPrev) + b[i])`), replacing the
4-call chain and its `wx`/`uh`/`az`/`ar`/`ah` scratch buffers entirely.
Deliberately preserves the exact original per-element operation order (Wx
row-dot, then Uh row-dot, then +bias, then activation — matching the old
`matVecInto`+`matVecInto`+`addVecInto`+`sigmoidVec`/`tanhVec` chain exactly)
so results are bit-identical, not a reassociation. Applied to both the
forward and backward biGRU passes' three gates. Dead code removed:
`matVecInto`, `addVecInto`, `sigmoidVec`, `tanhVec` (no longer called
anywhere after the fusion; `matVec`/`matTVec`/`addVec`/`hadamard` remain,
still used elsewhere).

**Correctness verified two ways:** (1) `--check-gradients=8` against the
same synthetic dataset: `maxRelativeError=0.000000 (PASS)` on all 8 sampled
parameters (embedding rows), confirming `backward` (unchanged) still agrees
with the refactored `forward` via numerical differentiation. (2) Re-ran the
exact same baseline training command post-refactor: every epoch's
`trainLoss`/`validationLoss` matched the baseline run to all printed digits
(e.g. epoch 8 `validationLoss=0.0000007` both runs, final confusion matrix
`tp=27 fp=0 tn=45 fn=0` identical) — confirms bit-identical numerics, not
just "close enough."

**Step 3 result — no measurable speedup:** post-refactor epochs 2-8 ran
3.6-3.7s/epoch, same 32.8s total as baseline. **Speedup ratio ≈1.00x.**
Root cause, diagnosed from the refactor itself: at this architecture's size
(hidden=224, embedding=16), the O(h²) `Uh` and O(h·e) `Wx` dot-product
inner loops already dominate wall-clock cost, and those loops were already
flat/branch-free/non-aliased before this session (`matVecInto` already did
exactly that). The 4-call-chain waste this session removed was array
allocation/pass overhead on the *cheaper* O(h) bias-add/activation step, not
the dominant O(h²) compute — real but small relative to total per-token
cost, and apparently within measurement noise at 8-epoch/288-example scale.
**Kept anyway**: fewer allocations, one loop instead of four per gate, and
it's still a real (if here-unmeasurable) improvement with zero behavior
risk (bit-identical, gradient-check-verified) — not reverted.

**`make test`: 225/225 forward, 225/225 idempotency, unchanged** (GRU
package is off the main formatter pipeline, confirmed not to be, per
`STATE_COMMON.md`'s testing methodology, rather than assumed).

**Committed:** `src/com/jxmake/formatter/classifier/gru/GruClassifier.java`
(the fused-gate refactor) — see commit log for hash.
