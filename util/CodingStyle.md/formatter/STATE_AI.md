# STATE_AI.md — AI-Assist Design Reference and GRU Job State

This file documents the background and architecture for the JAR's built-in
`ai-assist` feature. Step 2 (argument-layout/getter-setter-grouping) is
permanently NOT FEASIBLE and is reference-only — no active work there. Step 3
(the GRU comment-classifier abstain resolution) is the active tracked job per
`CLAUDE.md`'s job table (`com.jxmake.formatter.classifier.gru`) and follows
the same `STATE_COMMON.md` process conventions as every other job.

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

> The JAR cannot distinguish meaningful author-expressed argument grouping
> from arbitrary line breaks — the core prerequisite for reliable AI candidate
> selection — and no tractable heuristic exists for it. A small on-device
> model (3B–7B) has no reliable basis for choosing between candidates without
> that signal. The mechanical fallback (dropped form if args fit on one
> indented line, one-per-line otherwise) is therefore permanent behavior when
> inline exceeds 100 chars.
>
> The architecture (grammar-constrained single-token response via
> `/v1/chat/completions`, candidate layout generation, fail-safe fallback)
> remains documented here as a valid design, reusable if a grouping-intent
> heuristic or a larger model (7B+) is proven reliable in the future.
>
> Tier-3 aesthetic decisions (argument layout, non-standard getter/setter
> grouping) are handled by the capable-AI workflow in `README.txt` /
> `AI_PREAMBLE_AESTHETIC.md` instead.

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

**Reference tools/models:** llama.cpp (https://github.com/ggml-org/llama.cpp);
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
| RDD_EXT_19 | Step 3 Pool A/Pool B corpus storage: real extracted/labeled corpora and any derived real trained-weights artifacts are **never committed** — stay under `/tmp`/session scratchpad (or the user's own personal directory, `~/Projects/JxMake/0_excluded_directory/personal/GruArtifacts/`, when explicitly directed — see `tools/gru/README.txt`'s backup section for the exact commands). `tools/gru/sample_examples.txt` (checked in) holds only small, clearly-fake illustrative lines |
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
FEASIBLE, see below — different kind of "small model"). Builds on the
already-implemented rule-based comment-grammar classifier (Task H in
`STATE.md`, `RDD_KEY_94`–`98`): `CommentFeatureExtractor`/
`CommentFeatureVector`, `NonLatinScriptGate`, `KeywordAmbiguityGate`,
`CommentClassifier`/`CommentClassifierWeights` (`YES`/`NO`/`ABSTAIN`), gated
behind `comment-normalization-classifier` (default `off`).

Reuses Step 2's confirmed architecture pattern (grammar-constrained short
response, `temperature=0.0`, fail-safe fallback, `RDD_EXT_9` caching) — only
the small-LLM variant of that pattern is what's NOT FEASIBLE, not the pattern
itself (Step 2 validated the plumbing end-to-end).

### Small-LLM classifier fallback: NOT FEASIBLE (confirmed by testing)

> Small instruction-tuned models (1B–3B class) cannot reliably tell whether a
> word at the start of a sentence is plain English prose or a language
> keyword — exactly the `KeywordAmbiguityGate`/Step 3 task. **Tested and
> failed:** Qwen (1B–3B), Qwen2.5-Coder (1B–3B), Gemma (1B–3B). **Not tested,
> not expected to fare better:** Llama 3B (same param-count class).
>
> **A small on-device LLM will not be used for Step 3, full stop** — not as
> v1, not as a fallback behind the GRU, not for non-Latin-comment routing. The
> bidirectional GRU is the only Step 3 approach going forward. Does not
> reopen Step 2 (independent determination, different reason). Doesn't rule
> out a *larger* model (7B+) — untested, but no such path is being designed
> here; would need its own fresh justification.

### Model size determination — GRU is the only v1 approach

A **bidirectional GRU with ~500k parameters** is the best balance of
accuracy/latency/footprint for this narrow classification decision (not
open-ended generation). Bidirectional because the full comment text is
available upfront (not streamed), so only ~2x encoding compute, no
autoregressive-latency downside.

```text
Rules
   │
   ├── High confidence
   │
   └── Abstain
         │
      Bidirectional GRU classifier (~500k params) — v1
         │
      Final decision
```

If GRU accuracy proves insufficient in practice, the next step is a fresh
design discussion (larger model? different hyperparameters/training set?) —
not a revival of the rejected small-LLM fallback.

### Non-Latin comments

`RDD_KEY_95`'s `NonLatinScriptGate` disables the rule-based classifier
entirely (≡ `ABSTAIN`) for any comment with a non-Latin codepoint, deferring
to the full-file AI pass. **Closed, not unstarted:** depended on the
small-LLM fallback's multi-language understanding, which is NOT FEASIBLE —
no Step 3 LLM branch exists to route to. `RDD_KEY_95`'s behavior stands
unchanged. A GRU trained specifically on non-Latin/mixed-language examples
would be a distinct, unexplored idea — raise separately if worth pursuing.

### GRU implementation design (v1 target) — architecture finalized

**Architecture:**

| Component | Value |
|---|---|
| Input | word-level tokens, case-preserved (case is a real signal — `Return` reads less like a keyword than `return`), whitespace/punctuation split |
| Explicit vocab | ~3.5k: every keyword across every supported/planned language gets a guaranteed slot (never left to a hash bucket, since a keyword is exactly what triggers `KeywordAmbiguityGate`), plus ~3k common comment-corpus words |
| OOV handling | 1024-bucket hashing, not a shared `<UNK>` — distinguishable unknown identifiers |
| Embedding init | trained from scratch (pretrained vocabularies like GloVe/fastText blow past the param budget and reopen licensing/provenance questions) |
| Embedding dim | 16 (kept small — context modeling capacity, i.e. GRU hidden size, matters more than word-identity richness for resolving *ambiguous* usage) |
| Sequence cap | ~64 tokens per comment (truncate/pad) |
| GRU | single-layer bidirectional, hidden=224 |
| Target-word handling | index into the target word's own biGRU output (concat forward+backward) — no marker token; a biGRU timestep output is already a contextualized representation |
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
extends the `cwg/` pattern from `RDD_KEY_97`):

- Don't pre-commit to a total corpus size before measuring — run the existing
  `CommentClassifier` over a large sample first to find the real ABSTAIN
  rate (done, see item 9 below), then size acquisition off that.
- Pre-filter every extracted comment through `CommentClassifier` first:
  high-confidence YES/NO already resolved for free (keep a modest sample so
  the GRU also sees easy cases); `ABSTAIN` is the real labeling target.
- **Two pools, different shapes:** Pool A (keyword-ambiguity) — large pool,
  targeted extraction toward short comments (≤6-8 words) containing a known
  keyword (`// for i` ambiguous vs. `// for matrix below` not). Pool B
  (period-ambiguity) — small pool, punctuation-discussion comments and
  abbreviation patterns beyond `e.g.`/`i.e.` (RDD_EXT_15's grep filter).
- **Sources:** own dogfooded repos first (RDD_EXT_16), then vetted permissive
  public repos.
- **Labeling:** Pool A via frontier-model labeling + spot-check (random
  baseline + every low-confidence flag); Pool B by hand directly (small,
  easy human call).
- **Verification/fixing:** flag mislabels via rule-based-classifier/label
  disagreement and held-out regressions; correct in place with a note on
  *why* it was wrong, same pattern as `cwg/derive_weights.py`.

**Fail-safe:** missing/unreadable weights file → `GruClassifier` behaves as
`ABSTAIN` (classifier `off` for that comment) — no further LLM fallback,
matches the fail-safe posture everywhere else — never blocks formatting.

### Open refinement items — all resolved

Items 1, 2, 5, 6, 7, 8 (output classes, abstain threshold, tokenization,
hash function, weights schema, Pool B extraction method) were pure judgment
calls, resolved as RDD_EXT_10–15 above. Items 3, 4, 10 (hyperparameters,
eval target, licensing) resolved as RDD_EXT_16–18 (starting points,
revisitable once real measurements exist). **Item 9 (real ABSTAIN-rate
measurement) is CLOSED** — see "Remaining blocked open items" below for the
full measurement and conclusion.

---

## TODO: Comment sentence-boundary detection defeated by mid-word dots (FEASIBLE — Step 3 candidate)

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
stripped).

Distinguishing a mid-word/mid-token dot (file extensions, `e.g.`, `i.e.`,
`v1.0`, single-letter abbreviations) from a true sentence-ending dot is a
natural-language judgment call — no tractable mechanical heuristic exists.
**Exactly the class of ABSTAIN-worthy case Step 3 targets:** the rule-based
classifier's `dotCount != 1` case would ABSTAIN here, and the GRU classifier
would resolve it given enough mid-word-dot training examples. No longer
blanket NOT FEASIBLE — feasible via Step 3's GRU once trained; until then,
remains an accepted mechanical-rule limitation (`dotCount != 1` → leave
as-is).

---

## Remaining blocked open items / current job state

Everything mechanical is wired and real, not stubbed: `GruClassifier`
(`tokenize`, `hashBucket`, `softmax`, `decide`, `CLASS_ORDER`, and a real
`forward()` pass — embedding lookup, bidirectional GRU recurrence computed
only across the ranges that affect the target word's output, dense ReLU
head, softmax), `GruWeights` (full schema: explicit vocab, embedding table,
both GRU directions' gate matrices/biases, dense head, output layer — hand-
rolled recursive-descent JSON parser, no external library, backward-
compatible with the old scalar-only fixture files), `Vocabulary`
(explicit-vocab-vs-hash-bucket lookup), `GruAbstainResolver` (real "rules →
GRU on abstain" pipeline, config-gated via `gru-classifier`/
`gru-weights-path`, empty-path falls back to program-directory-relative
resolution), `tools/gru/GruTrainer.java` (real training loop: Xavier/Glorot
init, per-example forward+backward+Adam step at batch size 1 — a deliberate
simplification of RDD_EXT_18's batch-32 default, revisit if a production run
shows it matters — 20% held-out validation split with patience-based early
stopping, reads RDD_EXT_21's 4-column schema, loads `explicit_vocab.txt` by
default per RDD_EXT_22), the `gru-train`/`gru-extract-pool-a`/
`gru-extract-pool-b`/`gru-measure-abstain-rate` Makefile targets, and five
self-tests (`GruTokenizerSelfTest`, `GruWeightsSelfTest`, `GruSoftmaxSelfTest`,
`GruAbstainResolverSelfTest`, all passing; `make test` 116/116 forward +
116/116 idempotency, zero regressions throughout).

`GruClassifier.classify` still abstains whenever `hasTrainedWeights()` is
false — same fail-safe posture, no change to existing rule-based behavior
until a real weights file is deployed.

### Item 9 — real ABSTAIN-rate measurement: CLOSED

Tooling: `tools/gru/extract_comments.py` (walks a source tree, maps file
extensions to `Lang` languages, extracts marker-stripped comment text into a
flat `<lang>\t<escaped-text>` corpus; excludes `3rd_party` dirs since a
single vendored non-code comment file can dominate a language's count) +
`tools/gru/CommentAbstainTally.java` (feeds each comment through the real
`CommentFeatureExtractor`/`CommentClassifier` pipeline, tallies YES/NO/
ABSTAIN per language; wired as `make gru-measure-abstain-rate
GRU_ABSTAIN_INPUT=...`).

**Measured across 14 corpora (~199,000 comments, own repos + 11 vetted
MIT/Apache-2.0/BSD-3-Clause public repos spanning every job's dogfood/
test-fixture list — full run-by-run counts recoverable via `git log`/`git
show` on this file's history):** ABSTAIN rate is consistently **0.0%-0.6%**
for ordinary hand-written comments, regardless of language or repo, with two
explained outliers: (a) `TTGO_VGA32_Lite`+`RobotCoding` 4.6%, traced to
vendored bitmap-font glyph-table files and embedded third-party zlib/libjpeg
source, not a classifier defect; (b) `json5/json5` 14.6% on only 103
comments, too small to be informative, flagged as unexplained rather than
root-caused. `CommentDecision.NO` never fires in any run — confirmed
expected (traced into `CommentClassifier.classify`: no code path currently
produces `NO`, not a bug).

**Implication:** at a ~0.3-0.5% typical rate, random sampling would need an
impractically large raw volume for a usable training set — confirms
RDD_EXT_15's targeted-extraction choice for Pool B, and the same principle
applies to Pool A. This closed item 9; volume-based random sampling is ruled
out as impractical for training-set acquisition.

### Pool A/Pool B extraction, labeling, and first production training run

`tools/gru/ExtractPoolA.java` (Pool A: ABSTAIN comments with
`hasLeadingKeywordMatch` set, excluding `NonLatinScriptGate` ABSTAINs) and
`tools/gru/extract_pool_b.py` (Pool B: RDD_EXT_15's grep filter,
independent of `CommentClassifier`) both read `extract_comments.py`'s
corpus format; wired as `gru-extract-pool-a`/`gru-extract-pool-b`.

**First real acquisition run (eCxx/SusterCaller/VMA-GIT, own repos per
RDD_EXT_16)** found and fixed a genuine `extract_comments.py` bug: the old
two-regex approach let a literal `/*` inside a `//` line comment (e.g.
`///*mlen = n;`) get matched by the block-comment regex and non-greedily
swallow unrelated later text, merging code into a bogus comment record
(reproduced at `TweetNacl.java:2354-2364`). Fixed with a single left-to-right
scanner (`extract_c_style_comments`) treating `//` and `/* */` as mutually
exclusive consumed spans. Verified against the reproduction and against real
large `/* */` blocks (BearSSL/esp8266) that now extract correctly as single
comments.

Post-fix: 57974 comments → **Pool A 167 candidates** (c=49, cpp=92, java=26),
**Pool B 241 candidates** (c=181, cpp=54, java=6).

**Labeling (RDD_EXT_20 schema):** Pool A hand-reviewed per-example: **45
YES / 122 NO** (c: 25/24, cpp: 19/73, java: 1/25) — large NO share matches
expectations for a keyword-leading pool drawn from embedded/driver code
(mostly commented-out code or terse labels, not full sentences). Pool B
labeled via a documented rule-based fallback (NO if 2+ newlines, no trailing
`.`, ellipsis, or abbreviation-final; YES otherwise): **41 YES / 200 NO**
(c: 37/144, cpp: 3/51, java: 1/5), spot-checked for correctness.

`add_target_index.py` (checked in) inserts RDD_EXT_21's `targetWordIndex`
column (index 0 for Pool A's leading keyword, last-token index for Pool B) —
its own `tokenize()` cross-checked bit-for-bit against the real Java
tokenizer before trusting it. Applied to both real corpora: 167/167 Pool A
and 241/241 Pool B converted, 0 skipped. Labeled corpora live in the session
scratchpad only, never committed (RDD_EXT_19).

**First production training run:** 408 examples (167+241), 80/20 split
(327 train / 81 held-out test, kept fully separate from `GruTrainer`'s own
internal 20% early-stopping split). `--epochs=40 --patience=6`: train loss
0.57→~0, validation bottomed at epoch 9 (0.156), early-stopped at epoch 15.
Held-out precision: 48/49 decided correct = **97.96%** (YES 13/1, NO 35/0),
39.5% abstain rate; training-split sanity check 97.37% (not suspiciously
perfect vs. held-out, i.e. not memorization). **Caveat:** 408 examples is a
very small corpus for a ~425k-param model, and a single un-cross-validated
80/20 split carries real sampling variance — read as "a real positive
signal," not a validated production accuracy claim.

**Explicit vocab curated (RDD_EXT_22)** and `GruTrainer` wired to load it by
default (verified `vocabSize=3500` in written weights). **Retrained against
the permanent vocab** (same 327/81 split, same hyperparameters): validation
bottomed at epoch 9 (0.062), early-stopped at epoch 15. Held-out precision:
46/49 decided correct = **93.88%** (YES 11/2, NO 35/0), 39.5% abstain rate —
still clears RDD_EXT_17's 90% bar. Training-split sanity check: 99.12%
(226/228), 30.3% abstain. The drop from 97.96% is read as sampling variance
(vocab, not just size, changed between runs), not a regression — **this is
now the current baseline; the 97.96%/97.37% figures are superseded/
historical (pre-RDD_EXT_22)**.

Qualitative spot-check on hand-written inputs (not part of either corpus)
confirmed the model differentiates on surrounding context rather than the
target token alone: `"for the sake of clarity, rewrite this"` (target="for")
→ YES; `"for (int i = 0; i < n; i++) increments"` (same leading token) → NO;
`"extern C."` (target=".") → YES; `"supports JSON, YAML, TOML, etc."`
(target=".") → NO; a non-trailing mid-sentence period at a different index
than the target → ABSTAIN rather than guessed.

### Tooling for corpus growth and cross-validation (added, not yet run at scale)

- `tools/gru/acquire_corpus.sh` — automates acquisition + extraction only
  (deliberately stops before labeling, a human judgment call per RDD_EXT_20).
  For each of 16 hardcoded sources (5 local dogfood repos under `~` + 11
  vetted MIT/BSD-3-Clause/Apache-2.0 public repos, sourced from item 9's own
  run log — extend by hand as new sources get vetted, never add unvetted
  ones), runs `extract_comments.py` then `gru-extract-pool-a`/
  `gru-extract-pool-b`, writing candidates under `--out-dir` (default
  `/tmp/gru_corpus`); public repos are shallow-cloned and removed after
  extraction unless `--keep-clones`. Smoke-tested (`--only eCxx`): 45357
  comments, 140 Pool A / 215 Pool B candidates.
- `tools/gru/GruEval.java` (checked in) — loads a trained weights file,
  reports precision/abstain-rate against an RDD_EXT_21-schema file; replaces
  the earlier throwaway `/tmp/GruEval.java` harness.
- `tools/gru/cross_validate.py` — repeated Monte Carlo cross-validation on
  top of `GruEval`: reshuffles a combined labeled file with a fresh seed per
  round (default 5), retrains `GruTrainer` from scratch on an 80% split,
  evaluates on the untouched 20%, reports mean/stdev/min/max precision.
  Smoke-tested (2 rounds, `--epochs=5 --patience=2`): 0.8333/0.8163, mean
  0.8248 stdev 0.0120 — confirms the pipeline runs end to end (low epoch
  count, not a real accuracy measurement).

Neither script's working files (splits, weights, clones) are ever committed,
per RDD_EXT_19. See `tools/gru/README.txt` for exact invocation syntax for
every tool in this directory.

### Optional synthetic-augmentation tooling (chat-LLM assisted, NOT the real corpus)

Separate, optional tooling to pad Pool A/B with LLM-generated synthetic
examples via a manually-copy-pasted chat prompt (Gemini/Grok free tiers,
no API key). This is **explicitly not a replacement** for the
acquire_corpus.sh + hand-labeling pipeline (RDD_EXT_19/20) — synthetic
comments risk teaching the GRU "what an LLM thinks ambiguous code looks
like" rather than real-world distribution, which is the exact failure
mode Step 3 was designed around when the small-instruction-tuned-LLM
approach was rejected. Treat any synthetic-augmented file as a distinct,
clearly-labeled source, not silently merged into the real combined corpus.

- `tools/gru/gen_synthetic_prompt.py` — reads `explicit_vocab.txt`,
  prints a self-contained copy/paste prompt (RDD_EXT_20/21 schema spelled
  out inline, no file upload needed) requesting Pool A + Pool B lines for
  the next unused slice of the vocab. `--words-per-batch` is configurable
  (default **100**, producing `2 * words_per_batch` total lines — raised
  from an earlier default of 20). State (`next_index` into the vocab)
  persists in `.gen_synthetic_state.json` next to the script, so
  consecutive daily runs walk forward through the 3500-word vocab instead
  of repeating words; wraps back to index 0 once exhausted. `--reset`
  restarts from 0. Each run also appends its actual `word_slice` (the
  words really requested that run, one per line) to a persistent,
  append-only sidecar, `--requested-words` (default
  `.gen_synthetic_requested_words.txt` next to the script) — this happens
  as soon as the slice is computed, independent of whether the printed
  prompt is ever actually pasted to a chat LLM. Pass an empty string to
  skip writing it.
  **Known behavior, not a bug:** a chat LLM (observed with Gemini) may
  silently skip a requested word it doesn't recognize as a real language
  keyword rather than emitting a line for it — expected, and the reason
  `regroup_synthetic.py`'s `--expected-words` check below exists.
- `tools/gru/regroup_synthetic.py` — takes one pasted-in file containing
  scattered Pool A/B lines (e.g. concatenated output from multiple chat
  responses/models) and splits it into `pool_a.tsv` / `pool_b.tsv` /
  `unresolved.tsv`. Field splitting tolerates inconsistent space/tab
  spacing (any whitespace run counts as one separator). Classification
  is deliberately conservative: `targetWordIndex == 0` → Pool A;
  `targetWordIndex == index of the last whitespace-split token` → Pool B;
  anything else (including malformed labels/indices) → `unresolved.tsv`
  for manual review, never silently discarded.
  **Known limitation:** the real `GruClassifier.tokenize` splits trailing
  attached punctuation into its own token (RDD_EXT_12), so the true
  target token is not always the last *raw* whitespace-split word (see
  the `extern C.` / `uses a vs. b comparison` examples in
  `sample_examples.txt`). Neither this script nor the chat LLM generating
  the data can reconstruct that tokenizer's exact behavior without its
  source, so a meaningful fraction of real Pool B lines are expected to
  land in `unresolved.tsv` rather than being auto/mis-classified — this
  is intentional, not a bug to fix by loosening the match.
  **Pool A leading-word cross-check (added after a real-data review of
  `sp_gemini.txt` found a chat LLM drifting into unprompted, unrelated
  filler sentences once it ran past its requested word list — still
  well-formed `targetWordIndex == 0` lines, just not answering the actual
  word that was asked for):** for `idx == 0` candidates, the leading
  word (trailing punctuation stripped) is checked against two optional
  sources, either of which can be disabled with an empty string:
  - `--expected-words` (default the `gen_synthetic_prompt.py` sidecar
    above) — the tight check: rejects any leading word that was never
    actually requested by any run. Preferred once the sidecar exists.
  - `--vocab` (default `explicit_vocab.txt`) — a looser fallback that
    only catches outright garbage (e.g. a leading token like
    `sizeof(int)` with code glued on with no space), since the vocab
    also legitimately contains ordinary English words (`the`, `a`,
    `to`, `on`, `by`, …) that are valid Pool A entries in general but
    may not be the specific word a given batch asked for.
  A line failing either enabled check goes to `unresolved.tsv`, never
  silently dropped, same as every other unresolved case.
  **`sp_gemini.txt` predates the sidecar mechanism** (its
  `.gen_synthetic_state.json`/`.gen_synthetic_requested_words.txt` history
  wasn't tracked when it was generated) — do not try to reconstruct its
  request history by replaying `gen_synthetic_prompt.py`
  from a reset state, since the file's actual batch boundaries don't
  cleanly match any single `--words-per-batch` value and a replay would
  very likely diverge from what was actually sent to the LLM, silently
  mis-filtering good lines. Treat `sp_gemini.txt` as vocab-only-checked
  legacy data; `--expected-words` becomes fully reliable only for batches
  generated after this mechanism was added.

---

### acquire_corpus.sh run at full scale (16 sources)

Run for real (not a smoke test) against all 16 configured sources, output to
`/tmp/gru_corpus` (archived by the user to
`0_excluded_directory/personal/GruArtifacts/gru_corpus.tar.xz`, per RDD_EXT_19
— personal directory, not the repo): **172,285 comments total → 578 Pool A +
492 Pool B candidates**, roughly 3.5x the previous 167/241 hand-labeled batch.
None of it is labeled yet — hand-labeling (RDD_EXT_20) remains the next
manual step before any of it can be added to training.

### Real (non-smoke-test) cross-validation run

Ran `cross_validate.py` (5 rounds, real `--epochs=40 --patience=6`, default
80/20 split) against the existing 408-example combined corpus
(`explicit_vocab.txt` vocab, same as the RDD_EXT_22 retrain baseline):
**precision mean=92.40%, stdev=3.00%, min=89.80%, max=96.49%** across the 5
rounds (abstain rate ranged 27%-40% per round, consistent with prior single-
split runs). Confirms the single-split 93.88% figure from the RDD_EXT_22
retrain sits well within normal split-to-split variance, not a lucky or
unlucky draw — this is now the variance-bounded precision estimate for the
408-example corpus, superseding the single-split-only caveat above. Working
files (per-round splits/weights) were not committed, per RDD_EXT_19.

### Not yet done

Hand-labeling the newly acquired 578 Pool A / 492 Pool B candidates (from the
full-scale `acquire_corpus.sh` run above) and folding them into a larger
combined corpus would substantially grow the training set beyond 408
examples — this is the next actionable step for further de-risking precision,
now that both acquisition-at-scale and cross-validation tooling have real
run data behind them.

---

## Next steps (as of the 16-source acquisition + 5-round cross-validation run)

1. **Hand-label the 578 Pool A / 492 Pool B candidates** from
   `~/Projects/JxMake/0_excluded_directory/personal/GruArtifacts/gru_corpus.tar.xz`
   (extract it back under `/tmp` first) per RDD_EXT_20's schema — the one
   step that can't be automated away.
2. **Add the `targetWordIndex` column** to the newly labeled candidates via
   `tools/gru/add_target_index.py` (RDD_EXT_21), same as the first batch.
3. **Combine** the new labeled corpora with the existing 408-example set into
   one larger combined file.
4. **Retrain** against the grown corpus (`make gru-train`, or
   `GruTrainer` directly) and **re-run `cross_validate.py`** against it to get
   an updated, larger-sample precision/variance estimate, superseding the
   current 92.40%/3.00% (408-example) figures above.
5. **Back up** any new real corpus/weights artifacts worth keeping to
   `~/Projects/JxMake/0_excluded_directory/personal/GruArtifacts/` per
   `tools/gru/README.txt`'s backup section (never into the repo, per
   RDD_EXT_19).
6. Once precision on the grown corpus is judged good enough against
   RDD_EXT_17's 90% bar, the resulting weights file becomes a candidate for
   actual deployment (wiring a real weights file into
   `GruAbstainResolver`'s `gru-weights-path` for a live run) — not yet
   scoped in detail, revisit once step 4's numbers are in.
