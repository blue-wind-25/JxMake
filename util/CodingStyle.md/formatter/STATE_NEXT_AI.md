# STATE_NEXT_AI.md — Deferred AI-Assist Design Reference

This file documents the background and architecture for the JAR's built-in
`ai-assist` feature. It is **not part of the active implementation tracker** — do
not read this file during a normal CLI session. It is reference material for any
future revisit of AI-assisted Tier-3 formatting.

Two separate determinations live here, for two different decision points — they are
not in tension with each other:

- **Step 2** (argument-layout / getter-setter-grouping candidate selection) —
  **NOT FEASIBLE**. No tractable grouping-intent signal exists for the JAR to hand an
  LLM, at any model size tested.
- **Step 3** (comment-classifier abstain-case resolution) — **FEASIBLE**. This is a
  narrow classification decision, not a layout-authorship judgment call, and reuses
  Step 2's confirmed infrastructure pattern retargeted at a different decision point.

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

Checklist status — Step 3 (FEASIBLE, design-only — see full section below; nothing
started, this is a design note, not scoped implementation work yet):

- [ ] Search Hugging Face for a current small instruction-tuned model per hardware
      tier (Pi CM5 / Core i5 CPU-only / <1GB VRAM GPU / 1–2GB VRAM GPU) — NOT STARTED,
      deliberately deferred to implementation time (see Step 3 below)
- [ ] `Config.java` — new keys for enabling the LLM abstain-fallback and pointing at
      an endpoint — NOT STARTED
- [ ] Wire the LLM fallback into the existing `CommentClassifier` ABSTAIN path —
      NOT STARTED
- [ ] `com.jxmake.formatter.classifier.gru` package — GRU now determined to be the
      preferred v1 approach (see "Model size determination" below), supersedes the
      earlier LLM-for-v1 lean — NOT STARTED

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

---

## Step 3 — Comment-Classifier Abstain Resolution: FEASIBLE

Unlike Step 2, this is not a layout-authorship judgment call — it's a narrow
classification decision (does this word function as a keyword or as prose here; is
this trailing dot a sentence-ender or part of a token) that a small model can plausibly
handle as a pure classifier, not a generator. Builds on the already-implemented
rule-based comment-grammar classifier (Task H in `STATE.md`, `RDD_KEY_94`–`98`):
`CommentFeatureExtractor`/`CommentFeatureVector`, `NonLatinScriptGate`,
`KeywordAmbiguityGate`, `CommentClassifier`/`CommentClassifierWeights`
(`YES`/`NO`/`ABSTAIN`), gated behind `comment-normalization-classifier` (default `off`).

### Proposed pipeline

```text
Rule-based classifier (already implemented, Task H)
        │
        ├── High confidence (YES/NO) → use classifier result
        │
        └── ABSTAIN
                    │
             Small instruction-tuned LLM, used purely as a classifier
             (single-class output, not generation — e.g. "is 'return' used
             as a programming keyword, an English word, or an identifier
             here? Return only the class.")
                    │
               Final decision
```

Reuses Step 2's already-confirmed architecture pattern rather than reinventing it:
grammar-constrained short response, `temperature = 0.0`, `/v1/chat/completions` via
llama.cpp/Ollama/vLLM/LM Studio, fail-safe fallback to `ABSTAIN`-equivalent behavior
(classifier `off`) on an unreachable endpoint, and the same endpoint-unavailability
caching described in `RDD_EXT_9`. Only the target decision changes — a class label
instead of a layout-candidate index.

### Model size determination — GRU now preferred for v1 (supersedes earlier LLM-for-v1 lean)

Earlier reasoning in this doc favored a small instruction-tuned LLM for v1, since it
needs no training set and already understands programming terminology and multiple
languages out of the box, while a GRU/LSTM/MLP would need a training pipeline and a
labeled dataset built from scratch.

Further research changes this determination: a **bidirectional GRU with ~500k
parameters, trained on ~5M examples, is the best balance** of accuracy, latency, and
footprint for this narrow classification decision (not open-ended generation), and is
preferred over an LLM fallback for v1. Bidirectional is chosen because the full
comment text is available upfront at inference time (not streamed token-by-token),
so there is no autoregressive-latency downside — only roughly 2x encoding compute
for the added backward pass, which should be affordable at this parameter count.
This is a design-only determination — nothing scoped or started yet; see "GRU
implementation design" below for the layout this targets.

```text
Rules
   │
   ├── High confidence
   │
   └── Abstain
         │
      Bidirectional GRU classifier (~500k params, ~5M training examples) — v1
         │
      Final decision
```

The small-LLM fallback design above (grammar-constrained selection,
`/v1/chat/completions`, fail-safe caching) remains documented as a valid architecture
and is kept as a fallback option if the GRU's accuracy proves insufficient in
practice — but GRU is now the v1 target, not a future-only option.

### Non-Latin comments

`RDD_KEY_95`'s `NonLatinScriptGate` currently disables the rule-based classifier
entirely (equivalent to `ABSTAIN`) for any comment containing a non-Latin codepoint,
deferring those comments to the full-file AI pass instead. The small LLM's
multi-language understanding means some non-Latin/mixed-language ABSTAIN cases could
now route to the same Step 3 LLM fallback rather than falling all the way back to a
full-file pass. **This is flagged as an open option, not a decision** — changing
`RDD_KEY_95`'s established behavior needs its own stop-and-ask per `STATE.md`'s
ambiguity process before implementation, since it changes already-shipped classifier
behavior rather than adding new behavior behind a new gate.

### Model selection — search at implementation time, not now

Do not pin a specific Hugging Face model in this document — open-weight model
availability and quality shift too quickly for a name written today to still be the
right pick later. When this is actually implemented, search Hugging Face at that time
and recommend a model against these criteria, evaluated **separately per target
hardware tier** since each has different constraints:

- **Raspberry Pi CM5** (ARM, no GPU) — must stay compatible with the same llama.cpp
  runtime path Step 2 already validated on this hardware; since this is now a
  secondary abstain-fallback rather than the primary decision-maker, prefer something
  smaller than Step 2's 3B reference point if quality allows.
- **Core i5, no dedicated GPU** — CPU-only inference via llama.cpp; more RAM headroom
  than the Pi, but still latency-sensitive since this sits in the formatter's hot
  path — check realistic tokens/sec for a single-class response, not just that it
  loads.
- **Cheap dedicated GPU, VRAM < 1GB** — forces a heavily quantized ~0.5B-class model;
  confirm a GGUF/quantized variant actually exists at this footprint before assuming
  the model qualifies.
- **Cheap dedicated GPU, VRAM 1–2GB** — opens up more of the small instruction-tuned
  range; still confirm quantized (Q4/Q5) VRAM footprint against the 2GB ceiling, not
  just the unquantized parameter count.

For each tier, check: instruction-tuned (not base), a maintained GGUF/quantized
release exists, and realistic single-token/short-response latency on that hardware —
then document the chosen model(s) here the same way Step 2 documents its tested
Qwen2.5-Coder-3B reference.

### GRU implementation design (v1 target)

Design layout for when this is picked up (~500k param bidirectional GRU, ~5M
training examples, per the determination above):

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
corpus-trained; reaching ~5M examples is a materially larger acquisition effort than
`cwg/`'s current 40-example set):
- **Acquisition:** primary source is public web/search data (e.g. sourced via Google
  search or similar large-scale crawling) of real code comments across languages,
  supplemented with real comments pulled from this codebase and the `test/` fixtures
  (per the open TODO already in `cwg/`'s own notes). Licensing/provenance of any
  bulk-sourced data needs checking before use — not addressed yet, flagged as open
  work for implementation time.
- **Labeling:** frontier-model-assisted labeling (same precedent as `RDD_KEY_97`),
  with spot-check review rather than blind acceptance — at 5M-example scale this
  spot-check needs to be sampling-based rather than exhaustive.
- **Verification:** flag likely-mislabeled examples via disagreement between the
  existing rule-based classifier's confidence and the assigned label, and via
  held-out accuracy regressions when a new batch is added — not just eyeballing.
- **Fixing:** relabeling/removal follows the same reproducible-and-versioned pattern
  `cwg/derive_weights.py`/`cwg/weights.md` already established for the linear
  classifier's weights — document each addition/correction and re-derive.

**Fail-safe:** a missing or unreadable weights file makes `GruClassifier` behave as
`ABSTAIN` (falls through to the LLM fallback, or classifier `off` if that is also
unavailable), matching the fail-safe posture everywhere else in this design — never
blocks formatting.

---

## TODO: `//` comment sentence-boundary detection defeated by mid-word dots (now FEASIBLE — Step 3 candidate)

`MiscRule.stripSoleTrailingPeriod` (§15) strips a comment's trailing `.` only when it
is the *sole* `.` in the comment text — a deliberately conservative heuristic to avoid
mangling an ellipsis or an abbreviation followed by more sentence text. This
misfires whenever the comment legitimately contains an unrelated dot earlier in the
sentence that is *not* an end-of-sentence period, e.g.:

```
// Combined .hpp test: pragma once, concepts, templates, classes, extern C.
```

Here `.hpp` (a file extension) and the trailing `C.` both count as dots, so
`dotCount != 1` and the genuinely-sentence-ending trailing period is left in place
(expected: stripped, since STYLE.md's single-sentence-never-ends-in-a-period rule
should still apply).

Distinguishing a mid-word/mid-token dot (file extensions, `e.g.`, `i.e.`, `v1.0`,
single-letter abbreviations like `extern C.`) from a true sentence-ending dot is a
natural-language judgment call, not a mechanical token-shape rule — no tractable
heuristic was found within Tier-1/Tier-2 mechanical rules alone. **This is exactly
the class of ambiguous, ABSTAIN-worthy case Step 3's hybrid pipeline above targets**:
the existing rule-based classifier's `dotCount != 1` case would ABSTAIN here rather
than guess, and the Step 3 LLM fallback would resolve it with real language
understanding. No longer blanket NOT FEASIBLE — feasible via Step 3 once that
pipeline is implemented; until then, remains an accepted mechanical-rule limitation
(`dotCount != 1` → leave as-is).
