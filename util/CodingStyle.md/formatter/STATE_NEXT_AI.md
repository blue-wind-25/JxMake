# STATE_NEXT_AI.md — AI-Assist Design Reference and GRU Job State

This file documents the background and architecture for the JAR's built-in
`ai-assist` feature. Step 2 (argument-layout/getter-setter-grouping) is
permanently NOT FEASIBLE and is reference-only — no active work there. Step 3
(the GRU comment-classifier abstain resolution) is now an active tracked job
per `CLAUDE.md`'s job table (`com.jxmake.formatter.classifier.gru`,
skeleton started) and follows the same `STATE_COMMON.md` process conventions
as every other job in that table.

Two separate determinations live here, for two different decision points — they are
not in tension with each other:

- **Step 2** (argument-layout / getter-setter-grouping candidate selection) —
  **NOT FEASIBLE**. No tractable grouping-intent signal exists for the JAR to hand an
  LLM, at any model size tested.
- **Step 3** (comment-classifier abstain-case resolution) — **FEASIBLE, GRU only**.
  This is a narrow classification decision, not a layout-authorship judgment call,
  and reuses Step 2's confirmed infrastructure pattern retargeted at a different
  decision point — but only via a purpose-trained bidirectional GRU (see "GRU
  implementation design" below). The small-instruction-tuned-LLM-as-classifier
  variant of Step 3 is **NOT FEASIBLE** — see "Small-LLM classifier fallback:
  NOT FEASIBLE" below.

---

## Step 2 — AI Integration: NOT FEASIBLE (deferred)

> The JAR cannot distinguish meaningful author-expressed argument grouping from
> arbitrary line breaks — this is the core prerequisite for reliable AI candidate
> selection, and no tractable heuristic exists for it. Without that signal, a small
> on-device model (3B–7B) has no reliable basis for choosing between candidates and
> produces inconsistent results. The mechanical fallback (dropped form if args fit on
> one indented line, one-per-line otherwise) is therefore the permanent behavior when
> inline exceeds 100 chars.
>
> The architecture (grammar-constrained single-token response via `/v1/chat/completions`,
> candidate layout generation, fail-safe fallback) remains documented here as a valid
> design. If a grouping-intent heuristic is developed in the future, or if a larger
> model (7B+) proves reliable enough without one, Step 2 can be revisited without
> redesigning the infrastructure.
>
> Tier-3 aesthetic decisions (argument layout, non-standard getter/setter grouping) are
> handled by the capable-AI workflow in `README.txt` / `AI_PREAMBLE_AESTHETIC.md` instead.

Checklist status — Step 2 (all NOT FEASIBLE — no implementation needed):

- [~] `Config.java` ai-assist keys — NOT FEASIBLE
- [~] `AiDecisionClient.java` — NOT FEASIBLE
- [~] `AI_DECISION_PROMPT.md` — NOT FEASIBLE
- [~] `MiscRule.java` Tier-3 AI hooks — NOT FEASIBLE
- [~] `README.md` ai-assist section — DONE (AI section removed and replaced in chat session)
- [~] `FORMATTER_DISCUSSION.md` — update Key Decisions table to record this decision (NOT STARTED)

Checklist status — Step 3 (FEASIBLE via GRU only, design-only — see full section
below; nothing started, this is a design note, not scoped implementation work yet):

- [~] Search Hugging Face for a current small instruction-tuned model per hardware
      tier (Pi CM5 / Core i5 CPU-only / <1GB VRAM GPU / 1–2GB VRAM GPU) —
      **NOT FEASIBLE, superseded** — see "Small-LLM classifier fallback: NOT
      FEASIBLE" below; no longer applicable, GRU is the only Step 3 approach
- [~] `Config.java` — new keys for enabling the LLM abstain-fallback and pointing at
      an endpoint — **NOT FEASIBLE, superseded** — no LLM fallback exists to
      configure; GRU has its own weights-file config surface instead (see "GRU
      implementation design" below)
- [~] Wire the LLM fallback into the existing `CommentClassifier` ABSTAIN path —
      **NOT FEASIBLE, superseded** — ABSTAIN now routes to the GRU classifier only
      (see "Fail-safe" note in "GRU implementation design" below)
- [~] `com.jxmake.formatter.classifier.gru` package — GRU now determined to be the
      preferred v1 approach (see "Model size determination" below), supersedes the
      earlier LLM-for-v1 lean — SKELETON ONLY: `GruClassifier.java` (`tokenize`/`hashBucket`/
      `HASH_BUCKETS` implemented per RDD_EXT_12/13, and made `public` — not just for
      `tools/gru/GruTokenizerSelfTest.java` below, but because RDD_EXT_13 requires the
      training side to call the exact same hash/tokenizer as the runtime, and the trainer
      lives in a different package outside `src/`; `classify` is an unimplemented throwing
      stub) and
      `GruWeights.java` (`load` now hand-parses the flat scalar schema via regex --
      no external JSON library exists in this project and the schema has no nested
      arrays yet; validates `schemaVersion` per RDD_EXT_14, hard-errors on mismatch or
      missing field; smoke-tested against a hand-written sample weights file). The
      embedding table, GRU weight matrices, and dense-head weights are not represented
      in `GruWeights` yet -- those need the training pipeline to produce real numbers
      first. `tools/gru/GruTrainer.java`
      added as a skeleton `main()` entry point (non-shipped, outside `src/`, per the "Files"
      section below) — throws immediately, actual training loop blocked on open items 3/4/9/10
      (hyperparameters, evaluation target, measured ABSTAIN rate, licensing check).
      `tools/gru/GruTokenizerSelfTest.java` added: a plain-`main()` assertion-based self-check
      for `tokenize`/`hashBucket` (no JUnit or other test framework exists anywhere in this
      project — the formatter's own testing methodology is the `_inp`/`_out` fixture-diffing in
      `test/`, which doesn't apply to internal classifier logic — so this follows the project's
      existing zero-dependency style). Covers punctuation-splitting, camelCase/snake_case
      wholeness, empty/whitespace-only input, hash determinism, and hash range; run via
      `java GruTokenizerSelfTest` after compiling — all checks currently pass. No weights
      file format finalized/written yet, no wiring into `CommentClassifier`'s ABSTAIN path yet.

---

## Background and Architecture (ai-assist)

Tracks the design for the JAR's built-in `ai-assist` feature — local on-device AI
for Tier-3 judgment-call formatting decisions.

**Hard constraint:** this work is purely additive. No existing Tier-1/Tier-2 rule
behavior may change.

Confirmed working design (tested with Qwen2.5-Coder-3B-Instruct-Q4_K_M via
llama.cpp on Raspberry Pi CM5):

- The JAR generates N candidate layouts for a Tier-3 decision point
- A selection prompt is sent to the local model asking it to pick the best
  option by number
- A grammar constraint (`root ::= "0" | "1" | ... | "N"`) forces a
  single-token response — no prose, no reasoning output
- `temperature = 0.0` for deterministic selection
- The JAR uses the OpenAI-compatible `/v1/chat/completions` endpoint —
  llama.cpp applies the model's chat template automatically from the GGUF metadata,
  so no model-specific prompt tokens are needed in the JAR. Portable across backends
  (llama.cpp, Ollama, vLLM, LM Studio, etc.)
- The model never rewrites source text — the JAR executes the chosen layout
  mechanically using existing token-level rules
- AI is only invoked when there is a genuine choice between candidates —
  single-candidate cases are handled mechanically with no endpoint call

**Reference tools and models:**
- llama.cpp: https://github.com/ggml-org/llama.cpp
- Qwen2.5-Coder-3B-Instruct-GGUF: https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF
- Tested GGUF: `qwen2.5-coder-3b-instruct-q4_k_m.gguf`

---

## RDD_EXT entries (AI-assist architecture, not in RDD_LOG.md)

These decisions were never externally logged — they have no entry in `RDD_LOG.md`
and no collision risk with RDD_KEY_n numbering. The related `RDD_KEY_86`/`87`/`88`
decisions that *are* externally logged appear in the main index in `STATE.md`.

| Key | Topic |
|---|---|
| RDD_EXT_1 | Selection prompt + grammar constraint confirmed working for Qwen2.5-Coder-3B on llama.cpp; `/v1/chat/completions` used (not `/v1/completions` or native `/completion`) — llama.cpp applies model chat template automatically from GGUF metadata, no model-specific prompt tokens needed in JAR code |
| RDD_EXT_2 | Model never rewrites source; JAR executes chosen candidate mechanically |
| RDD_EXT_3 | Fail-safe on unreachable endpoint: fall back to option 0, log warning, continue |
| RDD_EXT_4 | Four candidate forms for function call and declaration line-breaking (inline, dropped, preserve-groups+align, one-per-line); option 1 only when inline exceeds 100 chars; option 2 only when source already multi-line; AI only invoked when multiple candidates exist; option 2 uses comma-spacing normalization for calls and existing §5 column grid (DeclarationAlignmentRule/ColumnGrid/ModifierPriority) for declarations |
| RDD_EXT_5 | Semantic grouping (by type/name similarity) explicitly out of scope — option 2 preserves existing author-expressed grouping only, never creates new groupings |
| RDD_EXT_6 | Comment handling: trailing comments align normally; comment-only lines between groups are opaque (option 2 only, others migrate to trailing); inline block comments normalized in place; leading preamble comment disqualifies options 0/1/3 |
| RDD_EXT_7 | Call/declaration breaking is distinct from signature breaking — signatures (param list directly followed by `{`) remain fully deterministic (existing §8 implementation unchanged); candidate forms apply to calls and forward declarations/prototypes |
| RDD_EXT_8 | No-AI fallback for line-breaking when ai-assist is off or endpoint unavailable: attempt dropped (option 1) — if params-only line fits ≤ 100 chars when indented → use dropped; if still exceeds → one-per-line (option 3). No ratio or threshold check — fit check is the sole criterion. Applies to both calls and forward declarations |
| RDD_EXT_9 | Endpoint unavailability cache: standalone mode — static `endpointDead` boolean, skip all AI calls for process lifetime after first failure, single warning log; server mode — static `lastFailedAt` timestamp, skip AI calls for `ai-retry-interval` seconds (default 60) then retry once; connect timeout 500ms; fail-safe always falls back to mechanical result, never aborts formatting |
| RDD_EXT_10 | Step 3 GRU output classes: same `YES`/`NO`/`ABSTAIN` as the existing rule-based classifier, no more granular intermediate class — nothing downstream consumes a finer signal |
| RDD_EXT_11 | Step 3 GRU can abstain on low softmax confidence (default cutoff 0.5, stored in the weights file, tunable), falling through to mechanical default — same posture as the missing-weights-file fail-safe |
| RDD_EXT_12 | Step 3 GRU tokenization: trailing/attached punctuation splits into its own token (`matrix.` → `matrix` + `.`); camelCase/snake_case identifiers stay whole, not sub-tokenized |
| RDD_EXT_13 | Step 3 GRU OOV hashing: FNV-1a (32-bit) mod 1024 for the bucket index — deterministic, no external dependency, trivial to reimplement identically on the training and runtime sides |
| RDD_EXT_14 | Step 3 GRU weights file: top-level `"schemaVersion"` integer field (starts at 1), `GruWeights.java`'s loader throws a clear mismatch error rather than silently misparsing; the abstain-threshold (RDD_EXT_11) also lives in this file, not hardcoded |
| RDD_EXT_15 | Step 3 GRU Pool B (period-ambiguity) extraction: grep-based recall-favoring filter — comments with 2+ `.` where one is whitespace-surrounded, or containing an abbreviation-adjacent token (`etc.`/`vs.`/`approx.`/single-capital-dot) not followed by more lowercase text; false positives discarded during existing frontier-model labeling, not filtered here |

---

## Step 3 — Comment-Classifier Abstain Resolution: FEASIBLE (via purpose-trained GRU only — see "Small-LLM classifier fallback: NOT FEASIBLE" further down)

Unlike Step 2, this is not a layout-authorship judgment call — it's a narrow
classification decision (does this word function as a keyword or as prose here; is
this trailing dot a sentence-ender or part of a token) that a small **purpose-trained**
classifier can plausibly handle, not a generator. "Small" here means the GRU's
~500k-parameter footprint, not a small instruction-tuned LLM — testing confirmed the
latter fails at exactly this task (see below); the two are different kinds of "small
model" and this section's feasibility claim applies only to the former. Builds on
the already-implemented rule-based comment-grammar classifier (Task H in
`STATE.md`, `RDD_KEY_94`–`98`):
`CommentFeatureExtractor`/`CommentFeatureVector`, `NonLatinScriptGate`,
`KeywordAmbiguityGate`, `CommentClassifier`/`CommentClassifierWeights`
(`YES`/`NO`/`ABSTAIN`), gated behind `comment-normalization-classifier` (default `off`).

The originally proposed pipeline routed `ABSTAIN` to a small instruction-tuned LLM
classifier — confirmed NOT FEASIBLE (see below), superseded by the GRU-only pipeline
in "GRU implementation design" below.

Reuses Step 2's already-confirmed architecture pattern rather than reinventing it:
grammar-constrained short response, `temperature = 0.0`, `/v1/chat/completions` via
llama.cpp/Ollama/vLLM/LM Studio, fail-safe fallback to `ABSTAIN`-equivalent behavior
(classifier `off`) on an unreachable endpoint, and the same endpoint-unavailability
caching described in `RDD_EXT_9`. Only the target decision changes — a class label
instead of a layout-candidate index. **This architecture pattern is confirmed sound
(Step 2 validated it end-to-end) — what's confirmed NOT FEASIBLE is small models'
accuracy at this specific classification task, not the plumbing around them.**

### Small-LLM classifier fallback: NOT FEASIBLE (confirmed by testing)

> Small instruction-tuned models (1B–3B class) cannot reliably tell whether a word
> at the start of a sentence is being used as plain English prose or as a
> language keyword — exactly the `KeywordAmbiguityGate`/Step 3 classification task
> this fallback was designed for. **Tested and failed:** Qwen (1B–3B), Qwen2.5-Coder
> (1B–3B), Gemma (1B–3B). **Not tested, but not expected to fare better:** Llama 3B —
> same parameter-count class as the three tested families, no reason to expect a
> different outcome, so it is not being carried forward as an open question.
>
> This supersedes the "Earlier reasoning... favored a small instruction-tuned LLM"
> discussion below — that reasoning was correct about the *advantages* (no training
> pipeline, existing multi-language/programming-terminology understanding) but wrong
> about small models being *accurate enough* to cash in those advantages for this
> specific task. **A small on-device LLM will not be used for Step 3, full stop —**
> not as the v1 approach, not as a fallback behind the GRU, not for the non-Latin-
> comment routing option floated further below. The bidirectional GRU (see "GRU
> implementation design" below) is the only Step 3 approach going forward.
>
> This does not reopen Step 2's determination (Step 2 was already NOT FEASIBLE for
> an unrelated reason — no tractable grouping-intent signal exists at any model
> size, small or large) — the two determinations remain independent, as the file
> intro says. This also does not by itself rule out a *larger* model (7B+) for this
> task; no such test has been run. But no larger-model path is being designed here
> either — the GRU already covers v1, and revisiting an LLM approach of any size
> for Step 3 would need its own fresh justification and its own stop-and-ask,
> same as reopening Step 2 would.

### Model size determination — GRU is the only v1 approach (small-LLM fallback removed)

Earlier reasoning in this doc favored a small instruction-tuned LLM for v1, since it
needs no training set and already understands programming terminology and multiple
languages out of the box, while a GRU/LSTM/MLP would need a training pipeline and a
labeled dataset built from scratch.

Further research changes this determination: a **bidirectional GRU with ~500k
parameters is the best balance** of accuracy, latency, and footprint for this
narrow classification decision (not open-ended generation), and is preferred over
an LLM fallback for v1. Bidirectional is chosen because the full comment text is
available upfront at inference time (not streamed token-by-token), so there is no
autoregressive-latency downside — only roughly 2x encoding compute for the added
backward pass, which should be affordable at this parameter count. This is a
design-only determination — nothing scoped or started yet; see "GRU implementation
design" below for the finalized architecture and the training-set sizing approach
(training-set size is deliberately not pinned to a number here — an earlier ~5M
estimate was superseded by a measure-first, two-pool approach; see that section).

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

The small-LLM fallback design above is kept in this document **only as a historical
record of a rejected approach**, not as a valid fallback option — see "Small-LLM
classifier fallback: NOT FEASIBLE" above. If GRU accuracy proves insufficient in
practice, the next step is not "fall back to a small LLM" (confirmed not to work);
it would need to be a fresh design discussion (larger model? different GRU
hyperparameters/training set? something else entirely), not a revival of this
rejected fallback.

### Non-Latin comments

`RDD_KEY_95`'s `NonLatinScriptGate` currently disables the rule-based classifier
entirely (equivalent to `ABSTAIN`) for any comment containing a non-Latin codepoint,
deferring those comments to the full-file AI pass instead. **This open option is
now closed, not just unstarted:** it depended on the small-LLM fallback's
multi-language understanding to route some non-Latin/mixed-language ABSTAIN cases
away from the full-file pass — since that fallback is confirmed NOT FEASIBLE (see
"Small-LLM classifier fallback: NOT FEASIBLE" above), there is no Step 3 LLM branch
left to route them to. `RDD_KEY_95`'s established behavior (full-file AI pass for
any non-Latin-containing comment) stands unchanged. A GRU-based path for this case
would be a distinct, unexplored idea — training a classifier on non-Latin/mixed-
language examples specifically — not something this document currently designs;
raise as its own topic if it's worth pursuing later, rather than assuming it falls
out of the existing GRU design above.

### GRU implementation design (v1 target)

Design layout for when this is picked up. Architecture finalized via Q&A
(session-refined, superseding the earlier bare "~500k param bidirectional GRU"
placeholder — the ~500k param budget itself was already correct, this section now
fills in the specific config that hits it):

**Architecture:**

| Component | Value |
|---|---|
| Input | word-level tokens, case-preserved (case is a real signal — `Return` reads less like a keyword than `return`), whitespace/punctuation split |
| Explicit vocab | ~3.5k: every keyword across every supported/planned language gets a guaranteed slot (never left to the hash bucket — a keyword is definitionally what triggers `KeywordAmbiguityGate`, so a hash collision landing it next to a random identifier would hurt the one thing this model must get right), plus ~3k common English comment-corpus words filling the rest |
| OOV handling | 1024-bucket hashing, not a single shared `<UNK>` — different unknown identifiers stay distinguishable from each other even if noisy, rather than collapsing to one meaningless vector |
| Embedding init | trained from scratch, not pretrained (GloVe/fastText vocabularies are 400k+ words at 100–300 dim — even pruned and frozen, they blow past the whole param budget; training from scratch also sidesteps the licensing/provenance question bulk-sourced embeddings would reopen) |
| Embedding dim | 16 (kept small deliberately — context modeling capacity, i.e. GRU hidden size, matters more for this task than word-identity richness, since the whole point is resolving *ambiguous* usage from surrounding words) |
| Sequence cap | ~64 tokens per comment (truncate/pad) |
| GRU | single-layer bidirectional, hidden=224 |
| Target-word handling | index into the target word's own biGRU output (concat forward+backward) as the classification feature — no marker token. A biGRU's per-timestep output is already a full contextualized representation of that word given its surroundings; a marker token would add a vocab slot, an embedding, and sequence length to solve a problem the architecture already solves natively |
| Head | concat(448) → dense(64, ReLU) → softmax(N classes) |
| **Total params** | **~425k** (~75k headroom under the 500k budget — deliberately conservative over squeezing the ceiling, since headroom absorbs formula-vs-reality gaps like bias terms and possible later additions, e.g. word-shape features for camelCase/snake_case/ALL_CAPS) |

**Files** (new `com.jxmake.formatter.classifier.gru` package, parallel to the
existing `com.jxmake.formatter.classifier` package):
- `GruClassifier.java` — inference-only runtime code, shipped in the JAR. Loads a
  trained weights file at startup; never contains literal weight arrays in source
  (unlike `CommentClassifierWeights`'s baked-in linear-model constants — a neural
  net's weight count isn't hand-editable the same way, and retraining shouldn't
  require a JAR rebuild).
- `GruWeights.java` — loader/schema for the external weights file (JSON or a flat
  binary tensor dump; JSON preferred for v1 for easy diffing/inspection over a
  binary format).
- A `main()` training entry point in a **separate, non-shipped** location — e.g.
  `tools/gru/GruTrainer.java` or a `cwg/`-sibling directory, not under `src/`, so
  the runtime JAR never bundles training code or a training-only ML dependency.
  Takes a labeled example set path + hyperparameters as arguments, writes the
  trained weights file `GruClassifier` reads — the trainer **writes a weights
  file for the Java classifier to read at runtime; it does not overwrite or
  generate `.java` source**, so a retrain is a resource-file swap, not a
  recompile.

**Training-set acquisition, verification, fixing** (extends the existing `cwg/`
pattern from `RDD_KEY_97`, which is already frontier-model-assisted rather than
corpus-trained; the design below replaces the earlier flat "~5M examples" estimate
with a measure-first, two-pool approach — session-refined, see rationale in each
step):

- **Don't pre-commit to a total size before measuring.** The earlier "~5M examples"
  figure was a blanket guess made before this refinement pass. First step is to run
  the *existing* rule-based classifier (`CommentClassifier`, already implemented)
  over a large local+GitHub comment sample — no labeling yet, just counting — to
  find the real ABSTAIN rate. That number, plus the hit rate of the targeted
  extraction below, determines how much raw extraction actually gets you to a
  usable pool size, rather than assuming 5M up front.

- **Pre-filter through the existing classifier before labeling anything.** Every
  extracted comment gets run through `CommentClassifier` first:
  - **High-confidence YES/NO** → already resolved correctly for free — the rule-based
    classifier's own output *is* the label, no labeling cost. Keep a modest sample
    of these in the training set too (so the GRU also sees easy/unambiguous cases
    and doesn't develop a bias toward assuming everything is ambiguous), but the
    bulk of labeling effort should not go here.
  - **ABSTAIN** → this is the actual target, and where labeling effort concentrates.
    Labeling 5M *random* comments would spend full labeling cost on millions of
    cases the rule-based classifier already gets right for free; pre-filtering to
    ABSTAIN-only keeps the expensive part (frontier-model labeling + spot-check)
    focused on a much smaller, denser, more useful set.

- **Two separate pools, not one uniform corpus** — the two ambiguity classes this
  targets have different shapes and need different acquisition strategies:
  - **Pool A (keyword-ambiguity)** — the large pool. Observed pattern: ABSTAIN
    clusters around *short* comments (roughly ≤6-8 words) containing a known
    keyword with too little surrounding context to disambiguate — e.g. `// for i`
    or `// for matrix` (genuinely ambiguous) vs. `// for matrix below` or
    `// for error message handler` (clearly English once one more word of context
    is present). Extraction should therefore be **targeted, not random**: filter
    toward short comments containing a keyword from any supported/planned
    language, rather than sampling broadly across all comments (which would mostly
    pull in long unambiguous prose and waste extraction volume).
  - **Pool B (period-ambiguity)** — a separate, much smaller pool. This is not
    about brevity the way Pool A is — it's comments that discuss punctuation
    itself (`// The variable dot . is used...`) or unusual abbreviation patterns
    beyond the already-handled `e.g.`/`i.e.`/file-extension cases
    (`MiscRule.stripSoleTrailingPeriod`, see the mid-word-dot TODO elsewhere in
    this file). Expected to occur naturally at a low rate — likely low
    hundreds-to-thousands of examples via a targeted grep-and-review pass, not a
    bulk-extraction effort.
  - This split can be refined further once real data is in hand (e.g. if
    period-ambiguity turns out to occur at a high-enough natural rate inside Pool
    A's own extraction, a separate Pool B extraction pass may prove unnecessary) —
    noted here as the current best guess, not a final commitment.

- **Acquisition sources (both pools):** primary source is public web/search data
  (e.g. sourced via Google search or similar large-scale crawling) of real code
  comments across languages, supplemented with real comments pulled from this
  codebase and the `test/` fixtures (per the open TODO already in `cwg/`'s own
  notes). Licensing/provenance of any bulk-sourced data needs checking before use —
  not addressed yet, flagged as open work for implementation time.

- **Labeling — Pool A:** primary approach is a free frontier model labeling every
  extracted example (same precedent as `RDD_KEY_97`), not blind acceptance —
  spot-check review covers both a random baseline sample (~1-2%, to catch
  *confidently wrong* systematic blind spots that a confidence signal alone
  wouldn't flag) and every example the frontier model itself flags as
  low-confidence/hedged (cheap, high-yield on top of the random baseline). If free
  frontier-model access can't sustain the needed volume, fall back to a cheaper
  heuristic first pass (e.g. simple word-position rules) and only send what that
  heuristic can't confidently resolve to the frontier model — reduces call volume
  at some cost to catching the heuristic's own blind spots.

- **Labeling — Pool B:** no frontier-model pipeline needed — given its expected
  small size and that period-ambiguity is an easy call for a human to make at a
  glance, label by hand directly. Frontier-model assistance is still useful for the
  *extraction* step (finding candidate comments worth reviewing), just not for the
  labeling decision itself.

- **Verification:** flag likely-mislabeled examples via disagreement between the
  existing rule-based classifier's confidence and the assigned label, and via
  held-out accuracy regressions when a new batch is added — not just eyeballing.

- **Fixing:** reuses the existing `cwg/derive_weights.py`/`cwg/weights.md`
  reproducible-and-versioned pattern already established for the linear
  classifier's weights — when a spot-check finds a mislabeled example, correct it
  in place and document *why* it was wrong (which pattern, what the frontier model
  got confused by), not just that it was wrong. Over successive batches those notes
  become a working record of the frontier model's actual failure modes, useful for
  triaging future batches faster.

**Fail-safe:** a missing or unreadable weights file makes `GruClassifier` behave as
`ABSTAIN` — i.e. classifier `off` for that comment, falling back to whatever
Tier-1/Tier-2 mechanical behavior applies without it (there is no further LLM
fallback to fall through to — see "Small-LLM classifier fallback: NOT FEASIBLE"
above), matching the fail-safe posture everywhere else in this design — never
blocks formatting.

### Open refinement items (v1 target)

Architecture (embedding/vocab/hidden-size/target-word-handling) and the
training-data pipeline shape (measure-first sizing, two-pool split, labeling/
verification/fixing approach) are settled above via session Q&A. Items 1, 2, 5, 6,
7, 8 below were pure judgment calls with no data dependency, so they're now
resolved (RDD_EXT_10–15, added to the index above). Items 3, 4, 9, 10 remain open
— each needs a real measurement, training run, or external lookup that hasn't
happened yet, not just a decision:

3. **Training hyperparameters** — loss function, learning rate, batch size, epoch
   count, dropout/regularization, train/val/test split ratios. None chosen yet —
   deferred to implementation time, once a real training set exists to tune
   against (picking these on paper now, before any data, would be guesswork).
4. **Evaluation target** — what accuracy/precision-recall bar makes this "good
   enough" to ship, and against which held-out set. Deferred until item 9's
   measured ABSTAIN rate and a real held-out set exist — a target set against no
   baseline data is arbitrary.
9. **Real ABSTAIN-rate measurement** — the "run the existing rule-based classifier
   over a large sample first, measure before committing to a training-set size"
   step is planned but not yet executed; current pool-size estimates are
   directional, not measured.
10. **Licensing/provenance check** for bulk-sourced GitHub comment data — flagged
    open in multiple places above (acquisition, `cwg/`'s own notes), not
    investigated yet.

Resolved this session (design-only, no code — GRU implementation itself remains
NOT STARTED per the checklist above):

- **Output classes (was item 1):** same `YES`/`NO`/`ABSTAIN` as the existing
  rule-based classifier — no more granular intermediate class (`KEYWORD`/`PROSE`/
  `IDENTIFIER`). Nothing downstream of the classifier consumes anything finer than
  `YES`/`NO`/`ABSTAIN` (the `ABSTAIN`-fallback wiring, `MiscRule.stripSoleTrailingPeriod`,
  etc.), so a granular head would add vocabulary/training-label complexity (a
  four-way labeling scheme instead of three) to feed a mapping step whose only
  output is the same three classes — no consumer benefits from the extra
  granularity. See RDD_EXT_10.
- **GRU's own abstain threshold (was item 2):** yes, the GRU can abstain — a
  softmax confidence check below a cutoff falls through to mechanical default,
  same posture as the missing-weights-file fail-safe. Default cutoff: 0.5 (i.e.
  requires the top class to hold a clear plurality over the other two combined,
  not just be the argmax) — a starting default, tunable, stored as a field in the
  weights file itself (see RDD_EXT_14) so retraining can adjust it without a code
  change. See RDD_EXT_11.
- **Tokenization edge cases (was item 5):** trailing/attached punctuation splits
  off into its own token (`matrix.` → `matrix` + `.`) — consistent with the
  existing rule-based classifier's own `dotCount`-based reasoning, which already
  treats dots as separable signal rather than part of the word. camelCase/
  snake_case identifiers stay whole as a single vocab/hash entry, not sub-
  tokenized — sub-word splitting would need its own segmentation scheme and adds
  complexity with no clear benefit for this task (identifiers are typically OOV
  either way and fall into the hash buckets; the classification signal comes from
  surrounding context words, not from decomposing the identifier itself). See
  RDD_EXT_12.
- **Hash function choice (was item 6):** FNV-1a (32-bit), result taken mod 1024
  for the bucket index — simple, well-known, single-pass, no external dependency,
  and trivially identical to reimplement on both the training side and in
  `GruClassifier.java` since both are just "hash this UTF-8 string the same way."
  See RDD_EXT_13.
- **Weights file schema/versioning (was item 7):** the JSON weights file carries
  a top-level `"schemaVersion"` integer field (starting at `1`). `GruWeights.java`'s
  loader checks it explicitly and throws a clear error naming the expected vs.
  found version on any mismatch or missing field, rather than attempting to parse
  a shape it wasn't written for. The abstain-threshold value from the item above
  also lives in this same weights file (not hardcoded in `GruClassifier.java`),
  so a retrain can ship a new threshold alongside new weights in one file. See
  RDD_EXT_14.
- **Pool B's concrete extraction method (was item 8):** grep-based candidate
  extraction over the comment corpus, keyed on either (a) a comment containing
  two or more `.` characters where at least one is surrounded by whitespace on
  both sides (the punctuation-discussion case, e.g. `the dot . here`), or (b) a
  comment matching a short list of known abbreviation-adjacent tokens beyond the
  already-handled `e.g.`/`i.e.` (`etc.`, `vs.`, `approx.`, single-capital-letter-
  dot patterns like `extern C.`) not immediately followed by more lowercase
  sentence text. This is a recall-favoring first-pass filter, not a precise
  classifier — expected false positives get discarded during the existing
  frontier-model labeling step, not filtered out here. See RDD_EXT_15.



## TODO: Comment sentence-boundary detection defeated by mid-word dots (now FEASIBLE — Step 3 candidate)

`MiscRule.stripSoleTrailingPeriod` (§15) strips a comment's trailing `.` only when it
is the *sole* `.` in the comment text — a deliberately conservative heuristic to avoid
mangling an ellipsis or an abbreviation followed by more sentence text. This
misfires whenever the comment legitimately contains an unrelated dot earlier in the
sentence that is *not* an end-of-sentence period. Example, using C++'s `//` form
(the same problem class applies to any comment syntax under the general
single-sentence-comments-never-end-in-a-period principle — Python's `#`, CSS/XML/
HTML5's block-only `/* */`/`<!-- -->` forms, per AI_PREAMBLE_FULL.md §15's note that
the mechanism varies by language but the underlying rule doesn't):

```
// Combined .hpp test: pragma once, concepts, templates, classes, extern C.
```

Here `.hpp` (a file extension) and the trailing `C.` both count as dots, so
`dotCount != 1` and the genuinely-sentence-ending trailing period is left in place
(expected: stripped — the general rule above should still apply regardless of which
language's comment syntax is in play; treat the snippet above as one worked example,
not the scope of the problem).

Distinguishing a mid-word/mid-token dot (file extensions, `e.g.`, `i.e.`, `v1.0`,
single-letter abbreviations like `extern C.`) from a true sentence-ending dot is a
natural-language judgment call, not a mechanical token-shape rule — no tractable
heuristic was found within Tier-1/Tier-2 mechanical rules alone. **This is exactly
the class of ambiguous, ABSTAIN-worthy case Step 3 targets**: the existing
rule-based classifier's `dotCount != 1` case would ABSTAIN here rather than guess,
and the GRU classifier (see "GRU implementation design" above — Step 3's only
feasible approach, now that the small-LLM classifier fallback is confirmed NOT
FEASIBLE) would resolve it, provided its training set includes enough mid-word-dot
examples to learn the distinction. No longer blanket NOT FEASIBLE — feasible via
Step 3's GRU once that pipeline is implemented; until then, remains an accepted
mechanical-rule limitation (`dotCount != 1` → leave as-is).
