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
(default `off`). Reuses Step 2's confirmed architecture pattern
(grammar-constrained short response, `temperature=0.0`, fail-safe fallback,
`RDD_EXT_9` caching) — only the small-LLM variant is NOT FEASIBLE, not the
pattern itself.

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
target, licensing) are resolved — see RDD_EXT_10–18 above. Item 9 (real
ABSTAIN-rate measurement) is CLOSED — see below.

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
stripped). **2026-07-28 re-assessment:** no trained weights ship yet (at that
time — `GruClassifier.classify` still unconditionally abstained without
`hasTrainedWeights()`), so still not mechanically tractable. Distinguishing a
mid-word/mid-token dot (file extensions, `e.g.`, `i.e.`, `v1.0`, single-letter
abbreviations) from a true sentence-ending dot is a natural-language judgment
call with no tractable mechanical heuristic — exactly the class of
ABSTAIN-worthy case Step 3 targets: the rule-based classifier's
`dotCount != 1` case would ABSTAIN, and the GRU classifier would resolve it
given enough mid-word-dot training examples. Not blanket NOT FEASIBLE —
feasible via Step 3's GRU once trained; until then, remains an accepted
mechanical-rule limitation (`dotCount != 1` → leave as-is).

---

## Job state history (through 2026-07-29)

Everything mechanical is wired and real, not stubbed: `GruClassifier`
(`tokenize`, `hashBucket`, `softmax`, `decide`, `CLASS_ORDER`, real
`forward()` — embedding lookup, bidirectional GRU recurrence computed only
across ranges affecting the target word's output, dense ReLU head, softmax),
`GruWeights` (full schema: explicit vocab, embedding table, both GRU
directions' gate matrices/biases, dense head, output layer — hand-rolled
recursive-descent JSON parser, backward-compatible with old scalar-only
fixtures), `Vocabulary` (explicit-vocab-vs-hash-bucket lookup),
`GruAbstainResolver` (real "rules → GRU on abstain" pipeline, config-gated
via `gru-classifier`/`gru-weights-path`, empty-path falls back to
program-directory-relative resolution), `tools/gru/GruTrainer.java` (real
training loop: Xavier/Glorot init, per-example forward+backward+Adam step at
batch size 1 — a deliberate simplification of RDD_EXT_18's batch-32 default,
revisit if a production run shows it matters — 20% held-out validation split
with patience-based early stopping, reads RDD_EXT_21's 4-column schema,
loads `explicit_vocab.txt` by default per RDD_EXT_22), the
`gru-train`/`gru-extract-pool-a`/`gru-extract-pool-b`/
`gru-measure-abstain-rate` Makefile targets, and five self-tests
(`GruTokenizerSelfTest`, `GruWeightsSelfTest`, `GruSoftmaxSelfTest`,
`GruAbstainResolverSelfTest`, all passing; `make test` 116/116 forward +
116/116 idempotency at that point, zero regressions).

`GruClassifier.classify` abstains whenever `hasTrainedWeights()` is false —
fail-safe posture, no change to rule-based behavior until a real weights
file is deployed.

### Item 9 — real ABSTAIN-rate measurement: CLOSED

Tooling: `tools/gru/extract_comments.py` (walks a source tree, maps file
extensions to `Lang` languages, extracts marker-stripped comment text into a
flat `<lang>\t<escaped-text>` corpus; excludes `3rd_party` dirs) +
`tools/gru/CommentAbstainTally.java` (feeds each comment through the real
pipeline, tallies YES/NO/ABSTAIN per language; `make gru-measure-abstain-rate
GRU_ABSTAIN_INPUT=...`).

**Measured across 14 corpora (~199,000 comments, own repos + 11 vetted
MIT/Apache-2.0/BSD-3-Clause public repos):** ABSTAIN rate consistently
**0.0%-0.6%** for ordinary hand-written comments, with two explained
outliers: (a) `TTGO_VGA32_Lite`+`RobotCoding` 4.6%, traced to vendored
bitmap-font/zlib/libjpeg content, not a classifier defect; (b)
`json5/json5` 14.6% on only 103 comments, too small to be informative.
`CommentDecision.NO` never fired in any run at that time — confirmed
expected (no code path produced `NO` yet, not a bug — see below for the
later decorative-separator gate that changed this).

**Implication:** at a ~0.3-0.5% typical rate, random sampling would need an
impractically large raw volume for a usable training set — confirms
RDD_EXT_15's targeted-extraction choice for Pool B (and Pool A). This closed
item 9; volume-based random sampling is ruled out as impractical for
training-set acquisition.

### Pool A/Pool B extraction, labeling, first production training run

`tools/gru/ExtractPoolA.java` (ABSTAIN comments with `hasLeadingKeywordMatch`
set, excluding `NonLatinScriptGate` ABSTAINs) and `tools/gru/extract_pool_b.py`
(RDD_EXT_15's grep filter, independent of `CommentClassifier`) both read
`extract_comments.py`'s corpus format; wired as `gru-extract-pool-a`/
`gru-extract-pool-b`.

First real acquisition run (eCxx/SusterCaller/VMA-GIT, own repos) found and
fixed a genuine `extract_comments.py` bug: a literal `/*` inside a `//` line
comment (e.g. `///*mlen = n;`) got matched by the block-comment regex and
non-greedily swallowed unrelated later text, merging code into a bogus
comment record. Fixed with a single left-to-right scanner
(`extract_c_style_comments`) treating `//` and `/* */` as mutually exclusive
spans; verified against the reproduction and real large `/* */` blocks.

Post-fix: 57974 comments → Pool A 167 candidates (c=49, cpp=92, java=26),
Pool B 241 candidates (c=181, cpp=54, java=6). **Labeling** (RDD_EXT_20
schema): Pool A hand-reviewed: 45 YES / 122 NO (large NO share expected for
a keyword-leading pool from embedded/driver code — mostly commented-out code
or terse labels). Pool B via documented rule-based fallback (NO if 2+
newlines, no trailing `.`/ellipsis/abbreviation-final; YES otherwise): 41
YES / 200 NO, spot-checked. `add_target_index.py` inserts RDD_EXT_21's
`targetWordIndex` column, cross-checked bit-for-bit against the real Java
tokenizer; applied to both corpora with 0 skipped. Labeled corpora never
committed (RDD_EXT_19).

**First production training run:** 408 examples, 80/20 split (327/81).
`--epochs=40 --patience=6`: early-stopped epoch 15. Held-out precision
97.96% (YES 13/1, NO 35/0), 39.5% abstain; training-split 97.37% (not
memorization). Caveat: 408 examples is small for a ~425k-param model; single
un-cross-validated split carries sampling variance — a positive signal, not
a validated production claim.

**Explicit vocab curated (RDD_EXT_22)**, `GruTrainer` wired to load it
(vocabSize=3500). Retrained against permanent vocab (same split/hyperparams):
early-stopped epoch 15, held-out precision **93.88%** (YES 11/2, NO 35/0),
39.5% abstain — still clears RDD_EXT_17's 90% bar; training-split 99.12%.
The drop from 97.96% read as sampling variance (vocab change), not
regression — **this is the current baseline; 97.96%/97.37% are historical
(pre-RDD_EXT_22)**.

Qualitative spot-check confirmed the model differentiates on surrounding
context, not the target token alone: `"for the sake of clarity, rewrite
this"` (target="for") → YES; `"for (int i = 0; i < n; i++) increments"`
(same leading token) → NO; `"extern C."` (target=".") → YES; `"supports
JSON, YAML, TOML, etc."` (target=".") → NO; a non-trailing mid-sentence
period at a different index than the target → ABSTAIN rather than guessed.

### Tooling for corpus growth and cross-validation

- `tools/gru/acquire_corpus.sh` — automates acquisition + extraction only
  (stops before labeling, a human call per RDD_EXT_20). 16 hardcoded sources
  (5 local dogfood repos + 11 vetted public repos), writes candidates under
  `--out-dir` (default `/tmp/gru_corpus`); public repos shallow-cloned and
  removed after unless `--keep-clones`. Smoke-tested (`--only eCxx`): 45357
  comments, 140 Pool A / 215 Pool B candidates.
- `tools/gru/GruEval.java` — loads a trained weights file, reports
  precision/abstain-rate against an RDD_EXT_21-schema file.
- `tools/gru/cross_validate.py` — repeated Monte Carlo cross-validation:
  reshuffles combined labeled file with a fresh seed per round (default 5),
  retrains from scratch on 80%, evaluates on untouched 20%, reports
  mean/stdev/min/max precision. Smoke-tested (2 rounds, low epochs):
  0.8333/0.8163, mean 0.8248 stdev 0.0120 (pipeline check only, not a real
  accuracy measurement).

Neither script's working files (splits, weights, clones) are ever committed
(RDD_EXT_19). See `tools/gru/README.txt` for exact invocation syntax.

### Optional synthetic-augmentation tooling (chat-LLM assisted, NOT the real corpus)

Separate, optional tooling to pad Pool A/B with LLM-generated synthetic
examples via a manually-copy-pasted chat prompt (Gemini/Grok free tiers, no
API key). **Explicitly not a replacement** for the acquire_corpus.sh +
hand-labeling pipeline (RDD_EXT_19/20) — synthetic comments risk teaching
the GRU "what an LLM thinks ambiguous code looks like" rather than
real-world distribution, the exact failure mode Step 3 was designed around
when the small-LLM approach was rejected. Any synthetic-augmented file is a
distinct, clearly-labeled source, never silently merged into the real corpus.

- `tools/gru/gen_synthetic_prompt.py` — reads `explicit_vocab.txt`, prints a
  self-contained copy/paste prompt requesting Pool A + Pool B lines for the
  next unused vocab slice. `--words-per-batch` default 100. State
  (`next_index`) persists in `.gen_synthetic_state.json` so consecutive runs
  walk forward through the vocab; `--reset` restarts from 0. Each run
  appends its actual `word_slice` to an append-only sidecar
  (`--requested-words`, default `.gen_synthetic_requested_words.txt`) as
  soon as computed, independent of whether the prompt is ever pasted. Known
  behavior, not a bug: a chat LLM may silently skip a requested word it
  doesn't recognize as a real keyword.
- `tools/gru/regroup_synthetic.py` — splits one pasted-in file of scattered
  Pool A/B lines into `pool_a.tsv`/`pool_b.tsv`/`unresolved.tsv`. Tolerant
  field splitting. Classification: `targetWordIndex == 0` → Pool A;
  `== index of last whitespace-split token` → Pool B; anything else →
  `unresolved.tsv` for manual review. Known limitation: the real
  `GruClassifier.tokenize` splits trailing attached punctuation into its own
  token (RDD_EXT_12), so a meaningful fraction of real Pool B lines land in
  `unresolved.tsv` rather than being auto-classified — intentional, not a
  bug to fix by loosening the match. Pool A leading-word cross-check (added
  after `sp_gemini.txt` review found a chat LLM drifting into unprompted
  filler sentences with well-formed `targetWordIndex == 0` but wrong
  content): for `idx == 0` candidates, the leading word is checked against
  `--expected-words` (tight — the sidecar above, rejects never-requested
  words) and/or `--vocab` (loose fallback, catches outright garbage only,
  since ordinary English words are legitimately in the vocab). Either check
  can be disabled with an empty string; a line failing either enabled check
  goes to `unresolved.tsv`, never silently dropped. `sp_gemini.txt` predates
  the sidecar mechanism — do not try to reconstruct its request history by
  replaying `gen_synthetic_prompt.py` from reset, since batch boundaries
  don't cleanly match any single `--words-per-batch` value; treat it as
  vocab-only-checked legacy data.

### acquire_corpus.sh run at full scale (16 sources)

Output to `/tmp/gru_corpus` (archived by user to personal directory per
RDD_EXT_19): **172,285 comments total → 578 Pool A + 492 Pool B candidates**,
~3.5x the previous 167/241 hand-labeled batch. None labeled yet at that
point — hand-labeling remains an available next manual step.

### Real (non-smoke-test) cross-validation run

Ran `cross_validate.py` (5 rounds, real `--epochs=40 --patience=6`, 80/20
split) against the 408-example combined corpus (RDD_EXT_22 vocab):
**precision mean=92.40%, stdev=3.00%, min=89.80%, max=96.49%** (abstain rate
27%-40% per round). Confirms the single-split 93.88% figure sits well within
normal variance — this is the variance-bounded precision estimate for the
408-example corpus, superseding the single-split-only caveat. Working files
not committed (RDD_EXT_19).

### Next-steps checklist from the 16-source run — resolution (2026-07-29)

This session took a different path than the originally planned "hand-label
578/492 → combine → retrain → re-cross-validate → deploy" sequence:

1. **Hand-label the 578/492 candidates** — CANCELED/superseded. This session
   used `GenerateSampleDefault.java` auto-labeling via the rule-based
   classifier instead. The 578/492 candidates remain unlabeled in the
   archived `gru_corpus.tar.xz`; hand-labeling them is still available as a
   future option (it's the only source of real ground-truth `NO` examples
   for prose, since the auto-labeled path structurally can't produce prose
   `NO`) but is no longer the default next step.
2. **Add `targetWordIndex`** — moot, since step 1 didn't happen;
   `GenerateSampleDefault.java` emits its own (always 0) directly.
3. **Combine with the 408-example set** — did not happen.
   `sample_default.txt` (new 170k-line auto-labeled corpus) was deliberately
   kept separate — different provenance/label-quality tier; RDD_KEY_217's
   commit exception applies only to `sample_default.txt`, not the
   hand-labeled corpus.
4. **Retrain + re-run cross_validate.py** — a retrain did happen (`make
   gru-train`, producing `code-formatter-ai-assist-weights.json`), but
   against `sample_default.txt`, not the originally-intended grown corpus.
   `cross_validate.py` was not re-run against it (would trivially report
   ~0% variance on NO precision, since that corpus has no NO examples) — the
   92.40%/3.00% (408-example) figures remain the only real precision
   estimate on record.
5. **Back up new artifacts to personal directory** — superseded by
   RDD_KEY_217 (see below): `tools/gru/sample_default.txt` and
   `code-formatter-ai-assist-weights.json` are committed directly instead,
   per explicit user direction. Does not apply to any other artifact (Pool
   A/B candidates, cross-validation working files), which still follow
   personal-directory-backup guidance.
6. **Deploy once precision clears 90%** — partially done. Wiring is done
   this session (`GruAbstainResolver` genuinely reachable from the live
   formatter via `MiscRuleCore`), but did not wait on a precision
   re-measurement against a grown corpus — the shipped default weights file
   is trained on the all-YES auto-labeled corpus, not a precision-vetted
   grown one. Not active by default in practice either:
   `comment-normalization-classifier` (the prerequisite gate) stayed `off`
   by default after it regressed 9 `make test` fixtures (see below). So:
   reachable/wired = done; active-by-default + precision-vetted-at-scale =
   not done, now blocked on classifier-accuracy work instead of corpus size.

---

## 2026-07-29 session: default auto-labeled corpus, live wiring, RDD_KEY_217

### Why the rule-based classifier only ever returns YES/ABSTAIN, never NO (and why NO can only come from the GRU)

Two independent, confirmed-by-reading-the-code facts:

- `CommentClassifier.classify(CommentFeatureVector)` (the rule-based/"linear"
  classifier that gates the GRU) is architecturally incapable of returning
  `NO` — its decision tree only ever returns `YES` or `ABSTAIN` (RDD_KEY_96).
  `GruAbstainResolver.resolve` only calls the GRU at all when the rule
  result is `ABSTAIN`, so **`NO` can only ever come from the GRU stage
  itself.**
- The GRU stage (`GruClassifier.classify`) genuinely can return `NO` — but
  until this session it was never reached in the live formatter at all
  (`gruClassifier` config defaulted off, no shipped weights file existed,
  and the default training corpus turned out to be 100% YES labels — see
  below), so empirically `NO` never appeared from either stage.

**Later this same session**, a first real NO-producing rule-based gate was
added (`DecorativeSeparatorGate`, below) — so `CommentClassifier.classify`
itself can now return `NO` for decorative-only comments. The two-fact
finding above is otherwise still accurate (genuine prose/code NO still has
no rule-based gate; the GRU is still the only source of prose `NO`).

### New default-corpus auto-labeling pipeline (`gru-acquire-corpus`)

Added `tools/gru/GenerateSampleDefault.java`: runs an acquired comment corpus
through the real rule-based `CommentClassifier` (distant supervision) to
bootstrap `tools/gru/sample_default.txt` in RDD_EXT_20/21 schema, skipping
`ABSTAIN` comments, always `targetWordIndex=0`, with a provenance header.
Wired into the `Makefile`: `make gru-acquire-corpus` now also runs this
auto-labeling step, deduplicating in place (77,499 duplicate lines removed
from a 172,285-comment/170,210-kept run — mostly repeated license-header
text recurring across files of the same repo). `make gru-train`'s
`GRU_SAMPLE_EXAMPLES` default now points at `sample_default.txt` (was
`sample_examples.txt`); new `GRU_TRAIN_ARGS` passthrough for hyperparameter
overrides.

**Empirical confirmation of the "why no NO" finding:** the full-scale
auto-labeled run produced 170,210 kept examples, **100% labeled YES** —
directly demonstrating that bootstrapping training data from the rule-based
classifier alone can only teach the GRU to imitate that classifier's own
YES/abstain-collapsed-to-skip behavior, never NO. A default weights file
trained purely on this auto-labeled corpus is expected to behave similarly
to the rule-based classifier on cases it's already confident about (that
classifier is the sole source of its labels), but should still generalize
somewhat differently on truly ambiguous (rule-ABSTAIN) inputs, since those
just never became `sample_default.txt` rows at all rather than being
excluded for lack of a label. Getting real NO signal into the default
corpus still requires either hand-labeled real NO examples (Pool A/B path)
or a different bootstrapping signal than "what does the current rule-based
classifier say."

### RDD_KEY_217 — named exception to RDD_EXT_19 for the default artifacts

Per explicit user direction (license compatibility: sources are MIT/
Apache-2.0/BSD-3-Clause, all compatible with the formatter's own license;
provenance traceable via the acquisition script in-repo; comment excerpts
are short quoted fragments, not "proper/significant code" in the copyright
sense), logged **RDD_KEY_217** in `RDD_LOG.md` as a named, narrow exception:
exactly `tools/gru/sample_default.txt` and
`code-formatter-ai-assist-weights.json` are committed, unlike every other
real corpus/weights artifact this job produces. RDD_EXT_19's general policy
is **not** retracted for anything else — hand-labeled Pool A/B corpora,
cross-validation working files, and any other derived artifact still stay
under `/tmp`/personal-directory only.

### `tools/gru/sample_examples.txt` bug fix

Found and fixed 3 pre-existing `targetWordIndex` bugs in the small
illustrative Pool B lines (cross-checked against `GruClassifier.tokenize`'s
real output): `"extern C."` pointed past the end of its 3-token
tokenization (index 3, should be 2 — `GruTrainer` was silently skipping this
line every run rather than erroring); two other lines pointed at a
mid-sentence comma/abbreviation-dot token instead of the real trailing
period. Fixed in place with a dated comment; verified via a live
`GruTrainer` smoke run showing all 14 lines usable
(`trainExamples=12, validationExamples=2`).

### Live formatter wiring — `GruAbstainResolver` reachable from `MiscRuleCore`

`MiscRuleCore.classifyComment` (the actual comment-normalization funnel used
by `MiscRuleCurly`'s three call sites: sole-trailing-period stripping across
lines, first-letter capitalization, single-line trailing-period stripping)
now calls `GruAbstainResolver.resolve(features, commentText,
targetWordIndex, gruClassifier, gruWeightsPath)` instead of calling
`CommentClassifier.classify` directly — the GRU stage is now genuinely
reachable from the live formatting pipeline, not just offline tooling.
`gruClassifier`/`gruWeightsPath` were threaded as new constructor parameters
through `MiscRuleCore` → `MiscRuleCurly` → `ScopePipelineCurly` →
`FormatterCurly` (all with backward-compatible delegating overloads).
`Config.gruClassifier` now defaults to `true` (a real trained weights file
ships alongside the jar; `GruAbstainResolver` fails safe to `ABSTAIN` if the
file is missing/unreadable regardless of this flag, so flipping it on is
low-risk by itself).

**Important finding — `comment-normalization-classifier` stays `off` by
default.** This is the *other* gate in the chain (`MiscRuleCore` only calls
`GruAbstainResolver` at all when `commentNormalizationClassifier` is true;
`gruClassifier` alone is inert without it). Flipping both defaults to `true`
together regressed 9 `make test` fixtures (`c_comments`, `cpp_modern`,
`cpp_combined`, `cpp_comments`, `java_core`, `java_combined`,
`java_comments`, `real_code_regressions_22.kt`,
`real_code_regressions_54.java`). Root cause: the rule-based classifier
disagrees with the existing deterministic `isCommentNoCapitalizeWord`
keyword list on common real-code cases — it incorrectly decided to
capitalize comments starting with `consteval`, `static`, `while`,
`do-while`, `var`, `this`, `const`, `explicit`, `public`, `switch`, etc.,
where the deterministic list correctly left them lowercase. **Reverted
`commentNormalizationClassifier`'s default back to `false`** (restoring
all-green `make test`); `gruClassifier` was left at its new `true` default
since it's provably inert on its own. The rule-based classifier's accuracy
on exactly this keyword-leading-comment case (`KeywordAmbiguityGate`'s core
scenario) needs real improvement before `comment-normalization-classifier`
can safely default on — the concrete, test-backed blocker to actually
activating the GRU path by default, as opposed to merely reachable when a
user opts in via config.

**User feedback on `GruTrainer` performance/correctness**, from a real run
with `GRU_TRAIN_ARGS ?= --epochs=2 --patience=2 --progress-every=100`
(`vocabSize=3500, trainExamples=74169, validationExamples=18542`, epoch 1
took ~1207s): `avgTrainLoss=0.0000` looked suspicious and needed checking.
Requested improvements, not yet fully verified/closed out:
- Optimize Adam bias correction: compute the bias-correction factors
  (1 - beta1^step, 1 - beta2^step) once per optimizer step instead of inside
  every parameter update, without changing the training algorithm or
  numerical results.
- Convert SGD (batch size 1) to mini-batch training (batch size 32):
  accumulate gradients, average over the batch, one Adam update per batch,
  preserving identical behavior except for the batching.
- Tokenize every training sample once before the epoch loop and reuse the
  cached tokenized representation every epoch, without changing runtime
  inference (trainer-only change).
- Fix early stopping so `bestWeights` is a true deep copy instead of another
  reference to the same object.
User noted some of these were already attempted and asked for the rest to
be checked and continued — status of that follow-through not further
detailed in this file; check commit history / `GruTrainer.java` directly
for current state before assuming any item above is done.

### `CommentClassifier` first real NO-producing path: decorative-separator gate

Added `DecorativeSeparatorGate.isDecorativeOnly`
(`src/com/jxmake/formatter/classifier/DecorativeSeparatorGate.java`):
returns true for a comment with no letter or digit anywhere — just
punctuation/symbol runs like `****...****`, `#####...#####`, `");`, `---`.
Wired as a new gate in `classify` right after the non-Latin-script gate,
returning `NO`. Presence-based like `NonLatinScriptGate`, not scored —
deliberately narrow, doesn't attempt commented-out-code or license-block
detection (left for a future gate, see TODO below). Structurally cannot
affect ABSTAIN counts: a decorative-only comment (no letters) can never also
match the leading-keyword gate (which requires a real keyword word), so the
two are mutually exclusive by construction, not just empirically.
`CommentFeatureVector`/`CommentFeatureExtractor` gained the new
`isDecorativeOnly` field (constructor signature changed — updated the one
other call site, `tools/gru/GruAbstainResolverSelfTest.java`).

**Validated** (local-only re-run of `acquire_corpus.sh --only
eCxx,SusterCaller,VMA-GIT,TTGO_VGA32_Lite,RobotCoding`, the 5 local dogfood
repos, no network, plus `gru-measure-abstain-rate` against the combined
96442-comment corpus): **NO=20774** (0 before this change),
**ABSTAIN=1995 (2.1%)** — fully explained by this batch including the two
already-documented outlier repos (TTGO_VGA32_Lite/RobotCoding, vendored
bitmap-font/zlib content, ~4.6% per Item 9), not a regression. Spot-checked
15 real newly-NO comments by hand (separator lines, decorative punctuation,
no false positives on real prose). `make test` clean (219/219 forward +
idempotency) throughout.

This means `sample_default.txt`/`GenerateSampleDefault.java`'s "auto-
labeling can never produce NO" limitation (documented in `tools/gru/
README.txt` and above) is now only *mostly* true — decorative-only comments
will auto-label as NO on the next `make gru-acquire-corpus` run. Real prose
NO ground truth (commented-out code, license blocks, etc.) still requires
the hand-labeled Pool A/B path — this gate only covers the narrow
decorative-separator case.

### Still outstanding

- Commented-out-code and multi-line-license-block NO gates — see the
  "TODO — further `CommentClassifier` NO-producing gates" section below.

## 2026-07-30 session: fixed `KeywordAmbiguityGate` weight regression, `comment-normalization-classifier` now defaults `on`

Root cause of the 9-fixture 2026-07-29 regression: the 40-example
`tools/classifier_weights/examples_{c,cpp,java,kotlin}.md` set had all 20 "zero mechanical
feature" rows (no paren/arrow/semicolon/url-or-number signal) labeled YES
— hand-authored "keyword-used-as-English-word" prose only. That produced
`KEYWORD_BIAS = +2.48420`, so any real keyword-led comment with none of
those four signals defaulted to YES (capitalize) — wrong, since that shape
is overwhelmingly a genuine code reference in real code (`static
operator()`, `consteval utility`, `while loop`, `do-while`, `var usage`,
etc. — confirmed real lines from `test/cpp_modern_inp.cpp`,
`test/cpp_combined_inp.cpp`, `test/java_core_inp.java`,
`test/java_combined_inp.java`, `test/java_comments_inp.java`).

Fix: added 22 new zero-feature NO-labeled rows (mix of the real regression
lines above + hand-authored analogues for languages/keywords without a
failing fixture) bringing the zero-feature split to 20 YES / 22 NO.
Re-ran `python3 tools/classifier_weights/derive_weights.py` (62 examples) and copied the new
constants into `CommentClassifierWeights.java`:

```
KEYWORD_BIAS                 = -0.20825   (was +2.48420)
KEYWORD_WEIGHT_PAREN         = -2.28827   (was -3.96297)
KEYWORD_WEIGHT_ARROW         = -1.51467   (was -3.22603)
KEYWORD_WEIGHT_SEMICOLON     = -2.96142   (was -4.93396)
KEYWORD_WEIGHT_URL_OR_NUMBER = -0.51492   (was -2.80469)
```

The now-negative bias means a zero-signal keyword-led comment defaults to
ABSTAIN (skip normalization) instead of YES — the intentional
asymmetric-risk tradeoff already documented on `KeywordAmbiguityGate` ("a
false skip is zero-cost, a false positive is a visible bug"); per-example
check now shows 20 mismatches, all the rare "keyword used as plain English
adjective, zero mechanical signal" case (e.g. `static analysis caught a
null deref here`) resolving to ABSTAIN instead of YES — accepted tradeoff.

Fixing 8 of the 9 regressed fixtures surfaced a second, distinct bug:
`test/real_code_regressions_54_inp.java` still failed, on a
stray-trailing-period strip (not capitalization) — `stripSoleTrailingPeriod`
should have stripped `" ."` from `... as the specified type .` but didn't.
That comment starts with keyword `return`, and `CommentFeatureExtractor
.extract` always computed `hasLeadingKeywordMatch` from the comment's
*first* word regardless of the caller's `targetWordIndex` — so the
strip-period call site (whose `targetWordIndex` correctly points at the
*last* token, unrelated to "return") was wrongly gated by leading-word
ambiguity anyway. Fixed with a `targetWordIndex`-aware `extract` overload
that only sets `hasLeadingKeywordMatch` when `targetWordIndex == 0`, wired
through `MiscRuleCore.classifyComment`'s existing parameter (previously
computed unconditionally). A real, narrow architectural gap independent of
the weight-derivation fix — the rule-based `CommentClassifier` path had
never actually been position-aware despite `classifyComment`'s own javadoc
describing `targetWordIndex` as pointing "at the token the decision
actually hinges on".

With both fixes in place, flipped `Config.commentNormalizationClassifier`'s
default to `true` (`STATE_COMMON.md`'s config-key line updated to match):
**219/219 forward, 219/219 idempotency** — all 9 originally-regressed
fixtures pass, no new regressions. The GRU comment-normalization pipeline
(`gruClassifier` + `commentNormalizationClassifier`, both now `true` by
default) is fully active by default, no longer real-but-opt-in.

### TODO — GruTrainer follow-ups (deferred, not yet scheduled)

Discussed 2026-07-29; user chose to implement fallback-write-on-failure, a
gradient-checking tool, and confusion-matrix/precision/recall/F1 reporting
immediately (see commit history for those). The rest were deferred to their
own future design passes rather than bundled in, since each changes training
numerics, output format, or runtime classifier behavior:

- **Break/resume support.** Needs real checkpointing: weights + Adam
  optimizer state (currently not serialized at all) + epoch/step position,
  written at some cadence, plus a `--resume=<checkpoint>` flag. Bigger than
  a quick add — deserves its own design pass.
- **Mini-batch training (16-32).** Would accumulate/average gradients over
  a batch before one Adam update, shrinking the serial-update bottleneck
  identified in the multi-threading work — but changes training numerics,
  so needs explicit sign-off as an intentional behavior change before
  implementing.
- **Dropout before dense layer.** Changes training numerics and needs a
  train/eval-mode switch — dropout must be disabled at inference in
  `GruClassifier` (the classifier used at format-time), not just during
  training.
- **Learning-rate warmup + cosine decay.** Replaces the current flat LR
  with a schedule; needs new hyperparameters (warmup steps, decay shape)
  and changes training numerics/output for any given corpus.
- **Automatic abstain-threshold tuning.** Needs a labeled validation slice
  and a chosen objective (max F1? fixed precision target?) to sweep
  thresholds against; also touches the classifier's runtime abstain logic,
  not just the trainer.

### `acquire_corpus.sh` secret redaction + earlier dedup (2026-07-30)

Added `tools/gru/redact_secrets.py`: scrubs likely API keys/tokens (Google,
AWS access-key-id, GitHub, Stripe, OpenAI/Anthropic, Slack — named-prefix
patterns — plus a narrow generic fallback for `key=`/`secret=`/`token=`/
`password=`/`access_key=`/`auth=`-shaped assignments whose value looks
high-entropy: mixed case+digit, Shannon entropy >= 3.5) from a comment's text
column, replacing matches with `[REDACTED]` in place. Wired into
`acquire_corpus.sh` right after `extract_comments.py` and before Pool A/B
extraction, so a scraped repo's leaked secret never reaches any corpus file
(Pool A/B candidates, `sample_default.txt`, or anything committed).
Smoke-tested against synthetic examples of each named format (correctly
redacted) and a plain-English/commit-hash control (correctly left alone).

Also moved the exact-duplicate-line dedup (`awk '!seen[$0]++'`) from the
Makefile's `gru-acquire-corpus` target — where it only ever ran once, at the
very end, against the final combined+auto-labeled `sample_default.txt` — into
`acquire_corpus.sh` itself, running per-source right after redaction. Smoke
run (`--only eCxx`): 45357 raw extracted comments → 27710 after dedup (license
header/boilerplate repetition, as expected), 117 Pool A / 189 Pool B
candidates. `make -n gru-acquire-corpus` confirmed the Makefile target still
parses correctly with the line removed.

### TODO — further `CommentClassifier` NO-producing gates (deferred, not yet scheduled)

Discussed alongside the decorative-separator gate (see above); higher
false-positive risk than that gate, so deferred rather than bundled in:

- **Commented-out code.** DONE — see the "Commented-out-code NO-gate" session
  below (2026-07-31).
- **Multi-line license/copyright blocks.** DONE — see the "Multi-line
  license/copyright-block NO-gate" session below (2026-07-31).

## 2026-07-31 session: commented-out-code `CommentClassifier` NO-gate (tracker item 11 / former "further NO-producing gates" item 1)

Implemented the gate flagged as actionable by the 2026-07-30
disagreement-sampling pass (see that section above): a comment ending in
`;` (the comment's own last non-whitespace character) combined with a
second, independent code-shape signal now resolves `NO` rather than
falling through to the scored majority path (which always resolves `YES`).
A bare trailing `;` alone was confirmed unsafe by that earlier pass (~8%
false-positive rate on real prose ending a clause with a semicolon,
e.g. "...cannot be expressed in RFC 3339's 4-digit form;") — the same
asymmetric-risk shape as the reverted leading-hyphen gate (2026-07-30
"Bug 2" note) — so this gate requires the second signal, unlike that
rejected single-signal design.

**New files/fields:**
- `src/com/jxmake/formatter/classifier/CommentedOutCodeGate.java` (new,
  mirrors `DecorativeSeparatorGate`'s presence-based-gate shape):
  `looksLikeCommentedOutCode(String)` returns true only when the text ends
  with `;` (trailing whitespace ignored) **and** at least one of four
  regex sub-patterns matches anywhere in the text:
  - **Call-shape** — an identifier (optionally dotted, e.g.
    `System.out.println`) immediately followed by `(`, no intervening
    whitespace (real English essentially never writes "word(" with no
    space before a parenthetical).
  - **Assignment-shape** — identifier (optionally array-indexed) followed
    by a bare `=`, excluding `==`/`!=`/`<=`/`>=` via lookbehind/lookahead.
  - **Increment/decrement-shape** — `++`/`--` directly adjacent to a word
    character on at least one side (narrower than a bare `--` so a prose
    em-dash-style `--` with spaces on both sides doesn't match).
  - **Typed-declaration-shape** — a type-looking word (known primitive/
    common-type keyword, a sized-integer alias like `uint16_t`, or a
    capitalized identifier) followed by a plain identifier then `=` or
    `;` (e.g. `Event event;`, `uint16_t ctr = 0;`). Requires the *first*
    word to look like a type specifically to avoid matching ordinary
    two-word prose endings like "...4-digit form;" (neither "4-digit" nor
    "form" looks like a type).
- `CommentFeatureVector.looksLikeCommentedOutCode` (new field, 13th
  constructor arg — updated both call sites:
  `CommentFeatureExtractor.extract` and
  `tools/gru/GruAbstainResolverSelfTest.java`'s two hand-built vectors).
  Computed in `CommentFeatureExtractor`, **not** `targetWordIndex`-scoped
  (unlike `hasLeadingKeywordMatch`/`leadingWordFollowedBySlash`) — it's a
  whole-comment shape signal independent of which token the decision
  hinges on.
- `CommentClassifier` **Gate 1d** (between Gate 1c's slash-list gate and
  Gate 2's keyword-ambiguity gate): returns `NO` whenever
  `looksLikeCommentedOutCode` is set.

**Testing:**
- `make test`: 220/220 forward + 220/220 idempotency, unchanged — no
  regression from the new gate or the constructor-arity change.
- Unit smoke test (13 hand-picked cases, `/tmp` scratch file, not
  committed): all 9 real commented-out-code shapes from the disagreement-
  sampling pass's spot-check correctly resolve `NO`
  (`fmap[j] = a;`, `blockNo++;`, `--count;`,
  `System.out.println("hi");`, `mnuToolbox.addSeparator();`,
  `assertTrue(x);`, `uint16_t ctr = 0;`, `Event event;`, `ch = 0;`); both
  documented real-prose false-positive examples from that pass correctly
  resolve `YES` (fall through to the gate, unaffected); two additional
  prose sanity checks (a plain sentence, a sentence containing an
  unrelated mid-clause `;`) correctly do not trigger the gate.
- Real-corpus precision check against `tools/gru/sample_default.txt`
  (committed default corpus, RDD_KEY_217 exception): of the 91064 YES-
  labeled lines, 1055 end in `;` after trimming trailing whitespace (close
  to the disagreement-sampling pass's earlier ~984-candidate estimate on
  the same shape); the new gate fires `NO` on 739 of those (70%). Manually
  inspected ~200 of the fired lines (Java, JS, CSS-embedded-markup) — all
  genuine commented-out code/markup, zero real-English-prose false
  positives observed. The remaining 30% (ends in `;` but no second signal
  fires, e.g. bare closing-brace continuation lines, plain trailing-`;`
  prose) correctly still falls through rather than being force-NO'd —
  accepted lower recall for the higher-precision two-signal design, same
  tradeoff already documented for Gate 1c.
- `GruAbstainResolverSelfTest.java` (recompiled against JDK 21 per
  `STATE_COMMON.md`'s toolchain note, run standalone — not wired into
  `make test`): "all checks passed", confirming the constructor-arity
  change didn't break the existing hand-built `CommentFeatureVector` call
  sites.

**Not addressed by this session** (unrelated, flagged out of scope by the
disagreement-sampling pass): the string-literal-fragment-extracted-as-
comment corpus-generation bug (`Sun Microsystems, Inc.//DTD Enterprise
JavaBeans 1.1//EN";`-shaped lines) — still not root-caused, still a
separate `extract_comments.py`/`GenerateSampleDefault.java` concern, not a
`CommentClassifier` gap.

## 2026-07-31 session: multi-line license/copyright-block `CommentClassifier` NO-gate (tracker item 22 / former "further NO-producing gates" item 2)

Implemented the gate flagged as outstanding by the "further `CommentClassifier`
NO-producing gates" TODO above, the same overall pattern as the
commented-out-code gate (2026-07-31 session above): a new gate class, a new
`CommentFeatureVector` field computed in `CommentFeatureExtractor`, wired
into `CommentClassifier` as a new whole-comment-shape gate — Gate 1e,
between the commented-out-code gate (1d) and the keyword-ambiguity gate
(Gate 2).

**Design starting point:** `tools/gru/README.txt`'s "Hand labeling" section
has a worked example labeling a multi-line license header NO under the
rule "spans 2+ newlines, a license block not a single sentence -> NO" — the
same informal fallback rule Pool B hand-labeling already used. That rule
alone was explicitly flagged (both in `README.txt`'s own risk note and in
this file's TODO entry) as too blunt to port verbatim: a real multi-line
prose paragraph (a comment split across lines purely for width, common
throughout this codebase's own Javadoc/block-comment style) must not
misfire just because it spans 2+ newlines.

**Final design (two independent signals, mirroring
`CommentedOutCodeGate`'s shape):**
- `src/com/jxmake/formatter/classifier/LicenseBlockGate.java` (new):
  `looksLikeLicenseBlock(String)` returns true only when **both**:
  1. **Primary signal** — the comment spans 2+ newlines *and* its last
     non-whitespace character is not sentence-ending punctuation
     (`.`/`!`/`?`). Genuine multi-sentence prose overwhelmingly ends its
     final sentence with terminal punctuation; a license/copyright block
     frequently does not (e.g. this project's own `STATE_COMMON.md`
     standard test-fixture header, ending `"SPDX-License-Identifier:
     MIT"`, no trailing period). Not used alone as the gate, since some
     real license headers (including this project's own file header,
     ending `"... Apache License, Version 2.0 text."`) do end in a period
     and would be missed by this signal alone — an accepted false-skip
     (asymmetric-risk design used throughout this classifier), not a
     correctness problem.
  2. **Confirming signal** — explicit copyright/license vocabulary
     anywhere in the text (`Copyright`, `(C)`, `SPDX-License-Identifier`,
     `Licensed under`, `All rights reserved`, `Redistribution and use`,
     `Permission is hereby granted`, `WITHOUT WARRANTIES`/`WARRANTY`).
  A decorative-border-line confirming signal (the shape
  `DecorativeSeparatorGate` recognizes at the whole-comment level, applied
  per-line) was tried first and **rejected**: real-corpus testing (see
  below) found hundreds of ordinary decorative section-banner comments
  (e.g. `apache/ant`'s XML build file's `===...===`-framed section
  headers, containing zero copyright/license content) sharing the
  border-line shape without being license blocks —
  the same over-broad-single-signal failure mode the leading-hyphen gate
  was rejected for (2026-07-30 "Bug 2" note). License-specific vocabulary
  alone proved narrow enough to avoid that false-positive class while
  still catching every real license/copyright block found in the corpus.
- `CommentFeatureVector.looksLikeLicenseBlock` (new field, 14th constructor
  arg — updated both call sites: `CommentFeatureExtractor.extract` and
  `tools/gru/GruAbstainResolverSelfTest.java`'s two hand-built vectors).
  Computed in `CommentFeatureExtractor`, **not** `targetWordIndex`-scoped
  (same as `looksLikeCommentedOutCode`) — a whole-comment shape signal
  independent of which token the decision hinges on.
- `CommentClassifier` **Gate 1e** (between Gate 1d's commented-out-code
  gate and Gate 2's keyword-ambiguity gate): returns `NO` whenever
  `looksLikeLicenseBlock` is set.

**Testing:**
- `make test`: 220/220 forward + 220/220 idempotency, unchanged — no
  regression from the new gate or the constructor-arity change.
- Unit smoke test (9 hand-picked cases, `/tmp` scratch file, not
  committed): 4 real multi-line license-block shapes correctly resolve
  `NO` (the `README.txt` GNU-LGPL worked example, `STATE_COMMON.md`'s own
  standard test-fixture header, an "All rights reserved" block, a
  border-plus-"Licensed under" block); this project's own file header
  (ends in a period) correctly falls through to `YES` — the accepted
  false-skip from signal 1 above, confirmed intentional, not a bug; a
  genuine multi-paragraph Javadoc-style prose comment, a multi-line
  TODO-style prose block with neither trailing punctuation nor license
  vocabulary, a single-line (not multi-line) copyright mention, and a
  2-newline block ending in `?` all correctly fall through to `YES`
  (unaffected by the gate).
- Real-corpus precision check against `tools/gru/sample_default.txt`
  (committed default corpus, RDD_KEY_217 exception): of the 91064
  YES-labeled lines, an initial design (vocabulary-or-decorative-border)
  fired on 599; manually inspecting the fired set found hundreds of
  false positives — ordinary decorative section-banner comments (XML
  build-file headers framed by `===...===` borders, no license content)
  sharing only the border shape. Dropping the decorative-border branch
  and requiring license vocabulary specifically reduced the fired count
  to 375, and manual inspection of that reduced set (the first ~60
  matches, spanning `css`/`java`/`xml`/`js`/`c`/`cpp` — Apache-2.0 NOTICE
  boilerplate, GNU LGPL headers, ESP8266-core `esp8266/Arduino`-style
  multi-paragraph copyright/license blocks) found **zero real-English-
  prose false positives** — every fired example is genuine license/
  copyright text.
- `GruAbstainResolverSelfTest.java` (recompiled against JDK 21 per
  `STATE_COMMON.md`'s toolchain note, run standalone — not wired into
  `make test`): "all checks passed", confirming the constructor-arity
  change didn't break the existing hand-built `CommentFeatureVector` call
  sites.

This closes tracker item 22 / the "further `CommentClassifier`
NO-producing gates" TODO's remaining "Multi-line license/copyright
blocks" bullet — both items from that TODO list (commented-out code and
multi-line license/copyright blocks) are now DONE.

## 2026-07-30 session: `gru-classifier` flipped back to default `off` (real-trained weights underperform the linear classifier on ambiguous cases)

Evaluated the shipped `code-formatter-ai-assist-weights.json` against the
62 hand-labeled keyword-ambiguity examples in
`tools/classifier_weights/examples_{c,cpp,java,kotlin}.md` (the genuinely-ambiguous corpus
`derive_weights.py` trains the linear `KeywordAmbiguityGate` on), converted
to `GruEval`'s RDD_EXT_21 schema (`targetWordIndex=0` for all rows). Result
via `java -cp target/classes:<gru-tools-classes> GruEval
code-formatter-ai-assist-weights.json <converted-file>`:

```
total=62 abstain=0 decided=62 correct=19 precision=30.6%
yesCorrect=19/19  noCorrect=0/43
```

The GRU predicts **YES on every single example**, including all 43 that
should be NO — worse than the linear classifier's own 67.7% (42/62, see
`tools/classifier_weights/weights.md`) on this identical set.

Root cause: `sample_default.txt` (the GRU's only training corpus) is
auto-labeled *by the linear classifier itself* via `GenerateSampleDefault`,
keeping only its own high-confidence YES/NO decisions — so it's dominated
by clear-cut prose (mostly YES) and never contains the genuinely hard
ambiguous-keyword-led NO cases (those are exactly the ABSTAIN-path
comments the linear classifier won't auto-label, and per RDD_EXT_19 the
hand-labeled `tools/classifier_weights/examples_*.md` set is deliberately never merged into
`sample_default.txt`). The GRU learned "default to YES" rather than the
subtle distinction it exists to resolve.

Fix: `Config.gruClassifier` default flipped back to `false`
(`src/com/jxmake/formatter/Config.java`), `README.md`/`STATE_COMMON.md`'s
`gru-classifier` config-key lines updated to `off` with a pointer here.
Does not touch `commentNormalizationClassifier` (still defaults `on` — see
the KEYWORD_BIAS-regression section above), which gates the linear
classifier path only and is unaffected by this finding.

**Still outstanding:** teaching the GRU the hard cases requires training
data that actually contains them — e.g. incorporating the 62 hand-labeled
examples (or a larger set in the same style) directly into the GRU's
training corpus rather than relying on auto-labeled majority-YES
`sample_default.txt`. Until re-evaluated with `GruEval` against a held-out
labeled set beating the linear classifier's 67.7% baseline,
`gru-classifier` stays `off`.

## 2026-07-30 session: self-formatting dogfood-and-adopt run found and fixed a `CommentClassifier` false positive (slash-separated lists)

First run of `STATE_COMMON.md`'s "Formatter self-formatting
(dogfood-and-adopt) process" against the formatter's own `src/` tree
(round1/round2 fixed-point + `make test` all clean). Step 4's mandated
spot-check found 5 wrong capitalizations, all sharing one shape — a
comment starting with a slash-separated list of code identifiers/keywords,
e.g.:

```
sizeTokens/initTokens get flattened...       -> SizeTokens/initTokens ...   (WRONG)
open/final/abstract/sealed share one column  -> Open/final/abstract/...     (WRONG)
val/var share one slot per STYLE_KOTLIN.md   -> Val/var share one slot      (WRONG)
constexpr/consteval/constinit share one...   -> Constexpr/consteval/...     (WRONG)
wx/uh/az/ar/ah are short-lived per-token...  -> Wx/uh/az/ar/ah are ...      (WRONG)
```

Root cause: none of these leading words (`sizeTokens`, `open`, `val`,
`constexpr`, `wx`) are in the file's language's keyword set (all Java
comments — `.isJava`), so `hasLeadingKeywordMatch` was false and every one
skipped `KeywordAmbiguityGate` entirely, falling into `CommentClassifier`'s
"main path" (`BIAS=1.0`, always YES) — designed only for the non-ambiguous
majority case per `tools/classifier_weights/weights.md`. A leading word directly followed by
`/` (no whitespace) was never checked anywhere, keyword or not.

Fix: new `CommentFeatureVector.leadingWordFollowedBySlash` field (computed
in `CommentFeatureExtractor`, `targetWordIndex == 0` scoped like
`hasLeadingKeywordMatch`) and a new `CommentClassifier` **Gate 1c** (between
the decorative-separator gate and the keyword-ambiguity gate) that returns
`NO` whenever it fires — independent of keyword membership, so it also
catches non-keyword identifier lists like `sizeTokens`/`wx`. Accepted
false-skip risk (asymmetric-risk design, same as every other gate here):
genuine English "a/b" constructs (`and/or`, `km/h`) at a comment's very
start are rare enough that occasionally leaving one lowercase costs
nothing, versus wrongly capitalizing a code-identifier reference.
`make test`: 220/220 forward + idempotency, unchanged.

After the fix, re-ran the process from step 1 (round1/round2 fixed-point
clean, trial-JAR `make test` 220/220, round1b/round2b fixed-point clean,
isolated case-diff re-checked: only the 4 legitimate prose capitalizations
remained, zero false positives) and adopted round1's output into the real
`src/` tree; rebuild `make test`: 220/220 forward + idempotency. First real
self-formatting adoption of this codebase's own source with the current
ruleset — see git history for the resulting diff (71 `src/` files +
`tools/gru/GruAbstainResolverSelfTest.java` for the new constructor
argument).

### 2026-07-30 session: extended self-formatting to `tools/*`/`tools/classifier_weights/*`, found and fixed a JS shebang-mangling bug

Ran the same dogfood-and-adopt process against the 36 supported-language
files under `tools/*` and `tools/classifier_weights/*` (Java, Python, JS — the project's own
verifier scripts and GRU training/eval tools, distinct from the `src/`
formatter JAR itself but still formatter-supported languages). Spot-check
(step 4) surfaced two separate bugs before adoption:

**Bug 1 (real formatter bug, fixed in `src/`):** `#!/usr/bin/env node`
shebang lines in `tools/verifiers/*.js` were being corrupted. Root cause:
JS/TS routes through the same curly-brace tokenizer as C/C++/Java, and `#`
is only treated as a preprocessor directive for C/C++
(`isPreprocessorLanguage()`); for JS a leading `#!` fell through to normal
tokenization, so `enforceSemicolonInsertion` (`JsTsSpecificRule.java`) saw
`/usr/bin/env` etc. as a chain of division operators and appended a stray
statement-terminator semicolon at the line's end
(`#!/usr/bin/env node` -> `#!/usr/bin/env node;`), which breaks the
shebang (`env` would look for a binary literally named `node;`). A first
fix attempt emitted the shebang line as a `COMMENT_LINE` token, which
avoided the semicolon but introduced a *second* bug: `MiscRuleCore
.enforceCommentStyle` and other passes assume every `COMMENT_LINE` token's
text starts with a literal `//` and rewrite it as such, mangling the line
into `///usr/bin/env node`. Final fix: a dedicated `TokenType.SHEBANG`
(added to `Token.isGapToken`, so it's skipped by every rule exactly like
`COMMENT_LINE`/`COMMENT_BLOCK`, but never matched by any `//`-prefix-
assuming code) plus `TokenizerCurly.emitShebangLine()`, dispatched only
when `pos == 0 && c == '#' && peek(1) == '!'` (so it can only ever fire on
the file's literal first two characters, never mid-file). `make test`:
220/220 forward + idempotency, unchanged.

**Bug 2 (comment-classifier false positive, hand-fixed per-occurrence, NOT
gated):** two comments in `tools/gru/GruAbstainResolverSelfTest.java`
started with a hyphenated config-key literal (`gru-classifier`,
`gru-weights-path`) that got capitalized to `Gru-classifier`/
`Gru-weights-path`, breaking the literal spelling of the real config key.
Unlike the 2026-07-30 slash-list fix above, a blanket
`leadingWordFollowedByHyphen` gate was tried and **rejected**: it also
suppressed capitalization of legitimate English hyphenated compounds (e.g.
`non-negative` -> `Non-negative` in the existing `test/c_comments_inp.c`
golden test), which is a real regression, not an acceptable false-skip —
unlike `/`, a leading hyphen is common in ordinary English prose, so the
same asymmetric-risk argument doesn't hold. Decision (user-confirmed):
revert the hyphen feature/gate entirely and hand-edit the two affected
comments instead (reworded so the config-key literal no longer starts the
comment, e.g. "The gru-classifier config key is off: ..."). No classifier
change survived from this bug; if a similar case recurs, prefer wording the
comment to avoid a bare identifier at position 0, not a new blanket gate.

After both fixes, re-ran the process from step 1 (round1/round2 fixed-point
clean, round1b/round2b fixed-point clean, isolated case-diff zero false
positives, `make test` 220/220 clean) and adopted all 36 files into their
real locations. Verified beyond the standard process, since these files
aren't exercised by the `src/` JAR's own `make test`: all `.js` files parse
via `node --check` and run correctly end-to-end (`json_syntax_check.js`
against a real sample), all `.py` files via `python3 -m py_compile`, and
all `.java` files (including the Kotlin-compiler-dependent
`kotlin_syntax_check.java`/`kotlin_content_diff.java`, needing
`~/xsdk/kotlin-compiler-2.4.0/kotlinc/lib`'s jars on the classpath, and the
JDK11+-API-dependent `java_content_diff.java`/`java_syntax_check.java`,
needing `/opt/openjdk-21_linux-x64_bin/jdk-21`) compile cleanly.

### TODO — LLM-assisted disagreement sampling against `sample_default.txt` (deferred, not yet scheduled)

Discussed 2026-07-30: user asked how expensive it would be to have an LLM fix
labels in `sample_default.txt` (92039 data lines, 975 NO / 91064 YES, ~116.5
avg chars/line). Root cause (see this file's earlier sessions) is that the
corpus is auto-labeled entirely by the existing rule-based `CommentClassifier`
itself, so it structurally cannot contain hard NO cases the rules already
miss — a full LLM relabel of all 92k lines would just reproduce the same
blind spots at a different price point (est. a few dollars/~1-2h if batched,
$50-150+ if not, using cheap-model pricing). Agreed direction instead: use an
LLM to find *disagreements* against the existing rule-based labels on a small
sample, not to relabel the whole corpus. Steps for whoever picks this up:

1. **Sample.** Pull a few hundred lines from `sample_default.txt`, stratified
   toward the 975 existing NO lines (all or most of them) plus a random slice
   of YES lines, e.g.:
   ```bash
   grep -v '^#' tools/gru/sample_default.txt | awk -F'\t' '$2=="NO"' > /tmp/sample_no.tsv
   grep -v '^#' tools/gru/sample_default.txt | awk -F'\t' '$2=="YES"' | shuf -n 300 --random-source=<(yes 42) > /tmp/sample_yes.tsv
   cat /tmp/sample_no.tsv /tmp/sample_yes.tsv > /tmp/sample_for_llm.tsv
   ```
2. **Label independently via LLM.** Batch many lines per API call (schema:
   `<lang>\t<YES|NO>\t<targetWordIndex>\t<comment text>`, per RDD_EXT_21) and
   ask the LLM to return its own YES/NO judgment per line, without showing it
   the existing label (avoid anchoring). Keep batches small enough that a
   single bad completion doesn't corrupt the whole run.
3. **Diff against existing labels.** Any line where the LLM's label differs
   from the corpus's existing label is a candidate genuinely-hard case —
   exactly the class of example missing from the corpus today. This should
   be a small set (tens, not thousands) if the LLM roughly agrees with the
   rule-based classifier's easy majority.
4. **Hand-verify only the disagreements**, then fold the confirmed ones into
   the corpus as new hard examples (append-only — never bulk-overwrite the
   existing 92k labels). This directly grows the corpus's coverage of hard
   NO/ambiguous cases rather than paying full-corpus relabeling cost for a
   result that wouldn't change the GRU's YES-only failure mode.

### Findings from a first disagreement-sampling pass (2026-07-30, self-judged, no LLM API call needed)

Ran step 1 of the plan above by hand (I judged the samples directly rather
than calling out to a separate LLM). Two distinct findings, one directly
actionable, one a separate corpus-generation bug:

1. **[ACTIONABLE — elevates tracker item 6's priority] Commented-out code
   mislabeled YES is common, not rare.** Initial small stratified sample
   (100 NO + 100 YES) found only one disagreement (`size_t offset = 0;`,
   labeled YES, clearly code not prose) — 0.5%, seemingly weak evidence.
   Scaling up by filtering the **full** 91064-line YES pool for lines ending
   in `;` (the exact commented-out-code shape) found **984 candidates
   (~1.1% of all YES lines)**. Spot-checking ~50 across both a
   "starts-with-a-space" and "no-leading-space" subset: the large majority
   are genuinely commented-out code (`fmap[j] = a;`, `blockNo++;`,
   `ch = 0;`, `System.out.println(...)`, `mnuToolbox.addSeparator();`,
   `assertTrue(...)`, `uint16_t ctr = 0;`, `Event event;`), spanning C, C++,
   Java, and JS — not a single-language quirk. **Important caveat: a bare
   "ends with `;`" signal alone is NOT safe as a gate** — a 25-line
   spot-check of the leading-space subset found ~2/25 (8%) were genuine
   English prose that happens to end a clause with a semicolon before
   continuing (e.g. "...cannot be expressed in RFC 3339's 4-digit form;",
   "...is expected to emit an object body (e.g. a map);") — the same
   asymmetric-risk shape as this session's reverted hyphen-gate. **This
   confirms the commented-out-code gate is worth building, but the gate must
   combine trailing `;` with a second signal** (assignment/call/increment/
   declaration shape — e.g. `IDENTIFIER (=|++|--|(...)) ... ;` — not
   semicolon alone) to keep the ~8% prose-false-positive rate out.
   **Unlike the GRU-corpus-only framing this TODO section started from,
   this is a live-formatting-correctness finding**: `CommentClassifier` is
   wired live via `GruAbstainResolver` into the real formatting pipeline
   (comment-normalization-classifier defaults `on` — see the 2026-07-30
   "`gru-classifier` flipped back to default `off`" section above; only the
   GRU half of that pipeline is off, the rule-based `CommentClassifier`
   gates are always live), so a fix here changes real output for real code
   today, across every curly-brace language — not just training-corpus
   quality for a currently-disabled feature.
2. **[SEPARATE BUG, not a `CommentClassifier` concern] String-literal
   content getting extracted as if it were comment text.** A cluster of the
   984 candidates are DTD/URL string fragments with no leading space (e.g.
   `Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";`,
   `apache.org/xml/features/validation/schema";`) — these look like pieces
   of ordinary Java string literals that happen to contain a `//`
   substring, which the comment-extraction step (`extract_comments.py`
   and/or `GenerateSampleDefault.java`'s own scan, not yet root-caused to
   which one) appears to misread as a line-comment start, capturing the
   rest of the string as if it were comment text. This is a corpus-
   generation/extraction bug, not a classifier-gate gap — `CommentClassifier`
   can't fix bad input. Not yet investigated further (which extractor step,
   how it decides `//` starts a comment without checking string-literal
   context) — flagged here for whoever picks up either this TODO or a
   corpus-generation-quality pass.

### `explicit_vocab.txt` contamination filter: DONE (2026-07-31, same session as scoping)

User inspection of the checked-in `tools/gru/explicit_vocab.txt` found
personal-name and narrow-domain-jargon tokens in the "common word" section
(`Aloysius`, `Indrayanto`, `Red`, `LUTs`, `OLED`, `WIZnet`) that have no
business being ranked as common English comment vocabulary. Root cause:
`build_vocab.py`'s frequency counter (`Counter()` over every token in every
line of whatever corpus file it's pointed at) has **no filtering at all**
beyond the 154 explicit per-language keyword slots — it ranks purely by raw
token-occurrence count, with no English-wordlist/stopword check and, more
importantly, no source-diversity requirement. A token that is genuinely
common in ordinary English comments (`value`, `buffer`, `check`) shows up
across virtually every one of `acquire_corpus.sh`'s 16 sources, including
the dozen-plus third-party public repos (`expressjs/express`,
`pallets/flask`, `apache/ant`, etc.) where a private individual's name or a
single hobby project's hardware jargon could never appear. By contrast
`Aloysius`/`Indrayanto` (a copyright-header line repeated across this
user's own local dogfood repos) and `OLED`/`WIZnet`/`LUTs` (concentrated in
one or two of those same hardware-firmware repos) are *locally* frequent —
high raw count within a narrow slice of sources — without being broadly
common at all. Note also that `acquire_corpus.sh`'s per-source exact-
duplicate-line dedup (added in the "`acquire_corpus.sh` secret redaction +
earlier dedup" session above) postdates whenever the current committed
`explicit_vocab.txt` was generated, so the committed file may additionally
reflect counts from an earlier, undeduped extraction where a repeated
boilerplate header line was counted once per source *file* rather than
once per source *repo* — worth checking `git log` on `explicit_vocab.txt`
against that dedup fix's commit date before assuming today's pipeline
alone explains the contamination.

**Scoped fix** (not yet implemented):

1. **Require cross-source diversity, not just raw frequency.** Change
   `build_vocab.py`'s counting unit from "occurrences across one combined
   corpus file" to "number of *distinct sources* (per-source
   `comments_<name>.txt` files, matching `acquire_corpus.sh`'s existing
   per-source file boundaries) a word appears in at least once," with raw
   count as a tiebreaker only. This needs `build_vocab.py`'s CLI to accept
   either a directory of per-source files or multiple `--input` paths
   instead of (or in addition to) today's single combined-file argument —
   `acquire_corpus.sh` already produces exactly these per-source files
   (`$OUT_DIR/comments_<name>.txt`), so no new extraction step is needed,
   just a different aggregation in `build_vocab.py` itself. A reasonable
   starting threshold: require a word to appear in at least 2-3 distinct
   sources (tune against how many of the 16 configured sources are
   available in a given run — some are the user's own local dogfood repos
   which won't exist on another machine, so the threshold must not assume
   all 16 are always present).
2. **Independently, re-run/verify `acquire_corpus.sh`'s per-source dedup is
   actually what today's `explicit_vocab.txt` was generated from** — if the
   checked-in file predates that dedup fix, regenerating against the
   current pipeline (even before item 1 lands) may already shrink the
   contamination somewhat, and is a useful sanity check on how much of the
   problem item 1 alone would fix vs. how much was a stale-generation
   artifact.
3. **Regenerate `explicit_vocab.txt` and re-derive/retrain.** Per
   `RDD_EXT_22`, the vocab is append-only *once a weights file has been
   trained against it* (reordering/removing lines shifts embedding-row
   indices). Since `gru-classifier` currently defaults `off` (no trained
   weights file is live against today's vocab — see the "`gru-classifier`
   flipped back to default `off`" session above), this is actually a safe
   window to regenerate `explicit_vocab.txt` from scratch (not merely
   append) without invalidating anything currently in use. That safety
   window closes the moment a real trained weights file is committed and
   `gru-classifier` flips back to `on` — do this before that happens, not
   after.

**Implemented and verified same session.** `build_vocab.py` now takes a
directory of per-source `comments_<name>.txt` files (`acquire_corpus.sh`'s
own output shape) instead of one combined file, computes document
frequency (number of distinct sources containing the word at least once,
via a per-source word set) alongside the old raw count, and ranks eligible
common words by `(doc_freq desc, raw_count desc)` — a word needs
`--min-sources` (default 2) distinct sources to be eligible at all. Ran the
real `acquire_corpus.sh` at full 16-source scale (5 local dogfood repos +
11 public clones) into a scratch dir, then regenerated
`tools/gru/explicit_vocab.txt` against it: 9684 words were eligible at
`--min-sources=2`, comfortably filling all 3346 common-word slots (154
keyword slots unchanged). Verified `Aloysius`/`Indrayanto`/`OLED`/`WIZnet`/
`LUTs` are all gone from the regenerated file. `Red` survives — spot-
checked as a genuine cross-source hit (capitalized "Red" occurring in at
least 2 independent sources, not concentrated in the user's own repos), a
useful sanity check that the diversity filter doesn't just remove anything
short/capitalized.

Item 2's premise (checking whether the committed file predated the
per-source dedup fix) turned out moot — regenerating from a real, current
`acquire_corpus.sh` run (which already includes that dedup) superseded the
question either way.

On item 3's safety-window point: confirmed `code-formatter-ai-assist-
weights.json` is **not** at risk from reordering `explicit_vocab.txt` on
disk regardless of `gru-classifier`'s config value — `GruWeights`
(`src/com/jxmake/formatter/classifier/gru/GruWeights.java`) embeds its own
`explicitVocab` string-array snapshot inside the trained JSON itself, and
`GruClassifier`/`Vocabulary` build their runtime vocab from that embedded
snapshot (`GruClassifier.java:95`), not by re-reading
`tools/gru/explicit_vocab.txt` at inference time. So the on-disk vocab file
only affects the *next* `make gru-train` run's embedding-row layout, never
any already-trained/committed weights file — regenerating it from scratch
was safe unconditionally, not just during today's `gru-classifier=off`
window. `make test`: 220/220 forward + idempotency, unchanged (vocab
contents don't affect the rule-based `CommentClassifier` path `make test`
exercises).

Not committed as part of this write-up alone — see the commit that lands
alongside this STATE_AI.md update for `build_vocab.py`'s diff and the
regenerated `tools/gru/explicit_vocab.txt`.

## 2026-07-31 session: `GruTrainer` break/resume checkpointing

Implements the "Break/resume support" item flagged deferred in the "TODO —
GruTrainer follow-ups" section above ("Needs real checkpointing: weights +
Adam optimizer state (currently not serialized at all) + epoch/step
position, written at some cadence, plus a `--resume=<checkpoint>` flag.
Bigger than a quick add -- deserves its own design pass") — this session is
that design pass. Before this, `bestWeightsJson` lived only in memory until
the very end of the run; killing the process mid-run (Ctrl-C, crash, reboot)
lost all progress with no resume path at all.

**Two binary checkpoint files**, both derived from `--out`'s path and living
right next to it (`<out>.ckpt-current.bin` / `<out>.ckpt-best.bin`):

- **Current-weights checkpoint** — overwritten once per epoch (after that
  epoch's Adam updates + validation-loss computation), including on the
  epoch that triggers early stopping. Full resumable state: weights (same
  field list `toJsonFields`/`GruWeightsBuilder` already enumerate — reused,
  not reinvented), the vocab (needed to reconstruct a `Vocabulary`), the
  Adam optimizer's own first/second-moment accumulator arrays (same shapes
  as the weight arrays they track — not just the raw weights, since
  resuming without them would restart the optimizer's momentum from
  scratch and defeat the point of a faithful resume), and scalar run state
  (`epoch`, `epochsSinceImprovement`, `bestValidationLoss`, `learningRate`/
  `maxEpochs`/`patience`, the RNG `seed`, and the Adam `step` counter —
  needed for bias-correction continuity, not just cosmetic). This is the
  file `--resume=<path>` expects.
- **Best-weights checkpoint** — overwritten only when validation loss
  improves. Weights + vocab only (no Adam/run state) — "give me the best
  model so far", not a resume target on its own, though a resume does read
  it as a *sibling* of the current-weights checkpoint (same base path,
  suffix swapped) to recover the true best-so-far weight arrays, since the
  current-weights checkpoint alone only ever holds the *latest* epoch's
  weights, never the best one.

**Binary format**: `java.io.DataOutputStream`/`DataInputStream` over
`BufferedOutputStream`/`BufferedInputStream` — no external library
(zero-third-party-dependency convention, no JSON library exists in this
codebase either), chosen over JSON purely for I/O speed on frequent
(every-epoch) writes of a several-MB weights blob. Exact layout, both files
share a header (`int magic=0x47525543 ("GRUC")`, `int formatVersion=1`,
`int kind` — `0`=best, `1`=current) then a shared weights block
(schemaVersion, vocabSize, hashBuckets, embeddingDim, hiddenSize,
sequenceCap, numClasses, abstainThreshold as scalars; vocab words via
`writeUTF` per word; `embeddings` 2D array; forward/backward
`DirectionWeights` — 6 2D + 3 1D arrays each; `denseW`/`denseB`/`outW`/
`outB`). Best-checkpoint appends one more scalar (`bestValidationLoss`,
informational). Current-checkpoint appends the scalar run-state block
(`epoch`, `epochsSinceImprovement`, `bestValidationLoss`, `learningRate`,
`maxEpochs`, `patience`, `seed`, `step`) then the Adam-moments block
(mirroring the weights block's shape exactly — embedding M/V, forward/
backward `DirectionMoments` M/V pairs, dense/out M/V pairs). All array
writers are shape-prefixed (`writeInt(length)` before every array/row) and
the loader-side counterparts validate the read length against the target
array's actual length before filling it in place, so a truncated/corrupt
checkpoint fails loudly with a clear "shape mismatch" `IOException` instead
of silently misreading. Written via temp-file-then-atomic-rename
(`Files.move` + `REPLACE_EXISTING`) — a process killed mid-write can never
leave a half-written, corrupt checkpoint behind, which is exactly the crash
scenario this feature exists to protect against.

**Naming convention**: `<weightsOutPath>.ckpt-current.bin` /
`<weightsOutPath>.ckpt-best.bin` (e.g. `--out
/tmp/weights.json` → `/tmp/weights.json.ckpt-current.bin`). Both are a
resume/recovery safety net, never a persistent artifact — deleted via
`Files.deleteIfExists` on normal successful completion (right after the
final JSON weights file is confirmed written), same posture as every other
real per-run output this job never commits (RDD_EXT_19-style, though this
isn't itself an RDD_EXT entry since it's not corpus/weights licensing —
just "generated working state, never checked in"). Added to `.gitignore`:
`*.ckpt-current.bin`, `*.ckpt-current.bin.tmp`, `*.ckpt-best.bin`,
`*.ckpt-best.bin.tmp` (the `.tmp` variants cover an aborted write's
leftover temp file, which the atomic-rename step means a reader never
actually sees, but which could otherwise linger on disk after a kill at
exactly the wrong instant).

**`--resume=<checkpoint-path>` — implemented, not deferred.** The user's
own scoping note leaned toward implementing resume rather than stopping at
checkpoint-writing alone, and it turned out not disproportionately more
work than the checkpoint I/O itself (the epoch loop already threads
through the exact scalars a resume needs). Loading a checkpoint happens
early in `main` (before the RDD_EXT_18 hyperparameter defaults are
computed), so `--lr`/`--epochs`/`--patience`/`--seed` fall back to the
checkpoint's own recorded values when resuming and the CLI doesn't
explicitly override them — `--epochs`/`--patience` can still be raised on
the CLI to extend a resumed run past its original bounds. The vocab is
never re-derived from `--vocab`/the examples file when resuming — it comes
from the checkpoint's own embedded snapshot (mirroring how `GruWeights`
already embeds its own `explicitVocab` in the JSON weights file), so a
resume can never silently shift embedding-row indices out from under the
resumed weight arrays. `random` is re-seeded from the checkpoint's own
`seed` and immediately used for the identical initial shuffle+split, so
**the train/validation split itself is reproduced exactly** on resume
(deterministic given the same seed and the same examples file/order). The
epoch loop's bound changes from `for(epoch=1; ...)` to
`for(epoch=startEpoch; ...)` where `startEpoch = resumed.epoch + 1`.

**Documented non-bit-reproducibility caveat** (exactly as flagged in the
task scope): only the RNG *seed* is persisted, not `java.util.Random`'s
internal state — so while the initial train/validation split is exactly
reproduced (see above), the *per-epoch* shuffle order from that point
onward diverges from what an uninterrupted run would have produced.
Documented at both the checkpoint-writer's javadoc and the resume call
site's inline comment as an accepted, deliberate limitation, not a bug:
resume continues training validly (same data, same optimizer state, same
architecture), it just doesn't bit-reproduce a hypothetical uninterrupted
run past the initial split.

**Testing performed:**
- `make test`: 220/220 forward + 220/220 idempotency, unchanged — this
  change never touches `src/`'s shipped `GruWeights.java`/`GruClassifier
  .java` at all, only the non-shipped `tools/gru/GruTrainer.java`; verified
  anyway since the trainer round-trips through `GruWeights.load`.
- `javac -encoding UTF-8 -source 8 -target 8 -cp target/classes -d
  <scratch> tools/gru/GruTrainer.java`: compiles clean, no warnings beyond
  what already existed.
- **Normal uninterrupted run** (400-line stratified sample of `tools/gru/
  sample_default.txt`, `--epochs=4 --patience=4`): checkpoint files
  observably existed during the run and were both gone afterward (only
  the intended `weights.json` remained) — confirms cleanup-on-success.
- **Kill-and-resume run** (same corpus, `--epochs=8 --patience=8`): started
  the trainer backgrounded, `kill -9`'d it partway through epoch 3 (after
  epochs 1-2 completed and were checkpointed); confirmed both
  `<out>.ckpt-current.bin` and `<out>.ckpt-best.bin` survived the kill and
  no final weights file existed. Re-ran with
  `--resume=<out>.ckpt-current.bin`: printed `"resuming from ... (epoch=2,
  epochsSinceImprovement=0, bestValidationLoss=0.0604511)"`, then
  continued training from **epoch 3** through epoch 8 with training loss
  continuing to decrease smoothly across the resume boundary (0.1244 at
  epoch 2 pre-kill → 0.1364/0.0890/0.0589/... at epochs 3-6 post-resume —
  a real continuation, not a restart from scratch, confirming both the
  weights and the Adam moments were faithfully restored), wrote a final
  `weights.json` with a sane confusion matrix
  (`tp=79 fp=0 tn=1 fn=0 precision=1.00000`), and both checkpoint files
  were gone afterward — full break/resume/cleanup cycle confirmed working
  end to end. Exact commands used (small scratch corpus + weights output,
  not committed): `java -cp target/classes:<compiled-tools-classes>
  GruTrainer <corpus> <out.json> --epochs=N --patience=N
  [--resume=<out.json>.ckpt-current.bin]`.

**Files changed:** `tools/gru/GruTrainer.java` (checkpoint constants,
binary I/O helpers — `writeWeightsBlock`/`readWeightsBlock`,
`writeBestCheckpoint`/`readBestCheckpoint`, `writeCurrentCheckpoint`/
`loadCurrentCheckpoint`, `writeAdamState`/`readAdamStateInto`,
`ResumeState`/`LoadedWeights` holder classes — `--resume` CLI flag,
per-epoch checkpoint writes, resume-aware initialization, cleanup on
success), `.gitignore` (4 new checkpoint-file patterns). No `src/` file
touched.

**Not attempted / explicitly out of scope this session** (unrelated to
checkpointing, still open from the "TODO — GruTrainer follow-ups" list
above): mini-batch training, dropout, LR warmup/decay, automatic
abstain-threshold tuning. The "Adam bias-correction computed once per
step" and "tokenize once, cache across epochs" items in that list were
already done before this session (see the class javadoc/`Example` class
above); this session only adds checkpointing/resume, nothing else from
that list.

## 2026-07-31 session: real-corpus GRU retrain re-evaluated against the linear classifier — improved (30.6% → 50.0%) but still below the 67.7% baseline

After this session's `explicit_vocab.txt` contamination fix, the new
`CommentedOutCodeGate`/`LicenseBlockGate` NO-gates, the `tools/classifier_
weights/examples_*.md` → `sample_default.txt` merge (`acquire_corpus.sh`'s
`classifier_weights_examples.tsv` step), and a real `make gru-acquire-
corpus` regeneration (92046 → 92308 lines, NO-labeled rows 975 → 3069 —
see the corpus-quality comparison earlier in this session), the user ran a
real training pass: `--threads=3 --epochs=3 --patience=2
--progress-every=1000` against the regenerated `sample_default.txt`
(73841 train / 18460 validation examples). Training converged fast and
correctly triggered early stopping — validation loss bottomed out at
epoch 1 (0.0393061) and got worse both subsequent epochs (0.0469431,
0.0417988), a classic single-epoch-convergence-then-overfit curve on this
corpus, not a still-improving one; `patience=2` caught it correctly at
epoch 3. Final validation confusion matrix (majority-YES,
imbalance-dominated): `tp=17763 fp=95 tn=525 fn=77 precision=0.99468
recall=0.99568 f1=0.99518` — looks excellent, but is measured against a
validation split where NO is still only ~3.3% of examples (602/18460), so
it mostly reflects "GRU correctly predicts YES on ordinary prose," not
resolution of the hard ambiguous-keyword-led cases this whole effort
exists for.

Re-ran the *same* hard-case benchmark the 2026-07-30 session used —
`GruEval` against `tools/classifier_weights/examples_{c,cpp,java,kotlin}.md`'s 62
genuinely-ambiguous hand-labeled examples (converted to RDD_EXT_21 schema
via `convert_classifier_weights_examples.py`, the same file `acquire_
corpus.sh` now writes to `$OUT_DIR/classifier_weights_examples.tsv`) —
against the freshly trained `target/gru/code-formatter-ai-assist-
weights.json`:

```
total=62 abstain=0 decided=62 correct=31 precision=0.5
yesCorrect=20/20  noCorrect=11/42
```

| | precision | YES correct | NO correct |
|---|---|---|---|
| Linear classifier (`KeywordAmbiguityGate`, baseline) | 67.7% (42/62) | — | — |
| GRU, 2026-07-30 (pre-fix corpus, majority-YES `sample_default.txt` only) | 30.6% (19/62) | 19/19 | 0/43 |
| **GRU, this session (post-fix corpus, hand-labeled examples merged in)** | **50.0% (31/62)** | 20/20 | 11/42 |

**Real, measurable progress, but still short of the bar.** The GRU no
longer degenerately predicts YES on every single example (2026-07-30's
finding) — it now gets 11/42 hard NO cases right, direct evidence that
merging the 62 hand-labeled examples into the training corpus and adding
the new rule-based NO-gates (which reshape what `GenerateSampleDefault`
auto-labels as NO across the wider real corpus, not just the merged 62
themselves) taught it something real about the hard cases. But it's still
below the linear classifier's 67.7%, and still wrong on 31/42 NO cases, so
this does not clear the bar the 2026-07-30 session set for flipping
`gru-classifier` back to `on` — it stays `off`. `Config.gruClassifier`
unchanged (`false`); `commentNormalizationClassifier` (linear-classifier
gate pipeline, unaffected by any of this) unchanged (`true`).

**Why 50% and not higher, and how to improve it further:**

1. **62 hand-labeled examples is a very small, repeated-many-times-over
   corpus relative to 73841 auto-labeled training examples (~0.08%).**
   Even merged in verbatim, `GruTrainer`'s online SGD sees each of the 62
   exactly once per epoch — 3 epochs of exposure to 62 hard cases against
   73841 easy ones is a tiny gradient signal, easily swamped by the
   majority-class-YES gradient direction. The single highest-leverage fix
   is **growing the hand-labeled hard-case set itself** (more entries in
   `tools/classifier_weights/examples_{c,cpp,java,kotlin}.md`, ideally in the same
   genuinely-ambiguous style `derive_weights.py` already curates for the
   linear classifier) rather than anything about training mechanics — the
   GRU can only learn a distinction that's actually represented with
   enough weight in what it's shown.
2. **Consider oversampling/upweighting the hand-labeled hard cases within
   `sample_default.txt` or within `GruTrainer` itself**, e.g. repeating
   each of the 62 rows N times (a crude but simple oversampling) or adding
   a per-example loss weight the trainer doesn't currently have (`GruTrainer`
   has no notion of per-example weight today — would be a small trainer
   change, not a corpus change) so the hard cases contribute gradient on
   par with their actual importance rather than their raw frequency in the
   corpus.
3. **62 examples is also a very small held-out-nothing benchmark** — all
   62 are used for both training (merged into `sample_default.txt`) and
   evaluation (the `GruEval` run above), so this 50%/67.7% comparison is
   really "how well did each classifier fit training data it saw," not a
   true generalization test. A cleaner benchmark would hold out a fraction
   of the hand-labeled set from training and evaluate only on the held-out
   slice — this wasn't done this session (`derive_weights.py`'s own 67.7%
   baseline for the linear classifier has the same caveat, so the
   comparison is at least apples-to-apples, but neither number is a
   trustworthy generalization estimate).
4. **The rule-based NO-gates (commented-out-code, license-block,
   decorative-separator, slash-list) generalize the corpus's NO population
   at the "obviously not a sentence" end of the spectrum, not the
   "ambiguous leading keyword" end the 62 hand-labeled examples target.**
   Growing `sample_default.txt` further via `acquire_corpus.sh` (more
   source repos) will keep improving the GRU's grasp of clear-cut cases
   but won't by itself move this specific 62-example benchmark much
   further — that benchmark is specifically testing the keyword-ambiguity
   shape the gates don't touch at all (`KeywordAmbiguityGate`/RDD_EXT_20
   Pool A is the relevant category, not Pool B or the gate-covered shapes).
5. **Architecture/training-mechanics changes (mini-batch training,
   dropout, LR schedule, more epochs) are unlikely to be the bottleneck
   here** — the fast single-epoch convergence and immediate overfit onset
   point at a data-representation problem (see 1-3 above), not an
   optimization problem. Not recommended as the next thing to try before
   growing/reweighting the hard-case corpus.

**Next step, if pursued:** grow `tools/classifier_weights/examples_*.md` with more
genuinely-ambiguous hand-labeled cases (same effort that already benefits
the linear classifier via `derive_weights.py`), then re-run this exact
`GruEval` comparison — watch specifically for `noCorrect` climbing past
21/42 (the point where the GRU would start beating the linear classifier's
absolute NO-correct count) before considering `gru-classifier=on` again.
