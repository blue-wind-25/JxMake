# STATE_NEXT_AI.md — Deferred AI-Assist Design Reference

This file documents the background, architecture, and NOT FEASIBLE determination for
the JAR's built-in `ai-assist` feature (Step 2). It is **not part of the active
implementation tracker** — do not read this file during a normal CLI session. It is
reference material for any future revisit of AI-assisted Tier-3 formatting.

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

Checklist status (all NOT FEASIBLE — no implementation needed):

- [~] `Config.java` ai-assist keys — NOT FEASIBLE
- [~] `AiDecisionClient.java` — NOT FEASIBLE
- [~] `AI_DECISION_PROMPT.md` — NOT FEASIBLE
- [~] `MiscRule.java` Tier-3 AI hooks — NOT FEASIBLE
- [~] `README.md` ai-assist section — DONE (AI section removed and replaced in chat session)
- [~] `FORMATTER_DISCUSSION.md` — update Key Decisions table to record this decision (NOT STARTED)

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

## RDD_EXT entries (AI-assist architecture, not in STATE_rdd_log.md)

These decisions were never externally logged — they have no entry in `STATE_rdd_log.md`
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
