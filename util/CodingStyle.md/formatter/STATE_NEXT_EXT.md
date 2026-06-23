# STATE_NEXT_EXT.md — Phase 3: JAR ai-assist Integration

> **DO NOT READ OR IMPLEMENT AGAINST THIS FILE YET.**
> This file is gated until `STATE_NEXT.md`'s End Goal (Phase 2) dogfood-test
> milestone is marked complete. If you are a Claude CLI session and you have
> arrived here before that milestone is checked off, stop — return to
> `STATE_NEXT.md` instead.

---

## Purpose

Tracks implementation of the JAR's built-in `ai-assist` feature — local
on-device AI for Tier-3 judgment-call formatting decisions — and the
post-phase-3 cleanup tasks that follow.

**Hard constraint:** this work is purely additive. No existing Tier-1/Tier-2
rule behavior may change. If an item turns out to require modifying existing
logic, stop and ask before proceeding.

---

## Background and Architecture

Confirmed working design (tested with Qwen2.5-Coder-3B-Instruct-Q4_K_M via
llama.cpp on Raspberry Pi CM5):

- The JAR generates N candidate layouts for a Tier-3 decision point (e.g.
  function call line-breaking: inline vs. split-per-arg vs. split-grouped)
- A selection prompt is sent to the local model asking it to pick the best
  option by number
- A grammar constraint (`root ::= "0" | "1" | ... | "N"`) forces a
  single-token response — no prose, no reasoning output
- `temperature = 0.0` for deterministic selection
- The JAR uses the OpenAI-compatible `/v1/completions` endpoint (not the
  llama.cpp-native `/completion` endpoint) for portability across backends
  (llama.cpp, Ollama, vLLM, LM Studio, etc.)
- The model never rewrites source text — the JAR executes the chosen layout
  mechanically using existing token-level rules

**Reference tools and models:**
- llama.cpp: https://github.com/ggml-org/llama.cpp
- Qwen2.5-Coder-3B-Instruct-GGUF: https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF
- Tested GGUF: `qwen2.5-coder-3b-instruct-q4_k_m.gguf`

---

## File Status

| File | Status |
|---|---|
| `Config.java` (ai-assist, ai-endpoint, ai-model keys) | NOT STARTED |
| `AiDecisionClient.java` (OpenAI-compatible `/v1/completions` caller) | NOT STARTED |
| `AI_DECISION_PROMPT.md` (prompt template — separate from AI_PREAMBLE.md) | NOT STARTED |
| `MiscRule.java` or rule classes (Tier-3 decision hooks) | NOT STARTED |
| `README.md` (update ai-assist section with final config details) | NOT STARTED |
| `FORMATTER_DISCUSSION.md` (close out remaining open questions) | NOT STARTED |

---

## Checklist — Phase 3

- [ ] Add config keys to `Config.java`:
      `ai-assist` (off | local), `ai-endpoint`, `ai-model`
      Env var equivalents: `STYLEFMT_AI_ASSIST`, `STYLEFMT_AI_ENDPOINT`,
      `STYLEFMT_AI_MODEL`

- [ ] Implement `AiDecisionClient.java`:
      POST to `{ai-endpoint}/v1/completions` with prompt, `n_predict = 1` (or
      `max_tokens = 1`), `temperature = 0.0`, and grammar constraint string.
      Parse `choices[0].text` from JSON response. Fail-safe: if the endpoint
      is unreachable or returns an unexpected token, fall back to option 0
      (first candidate) and log a warning — never abort formatting.

- [ ] Design and write `AI_DECISION_PROMPT.md`:
      Prompt template for the selection prompt. Must include: (1) the candidate
      layouts as numbered options, (2) a one-paragraph rule summary (not the
      full style guide), (3) the current line-length budget, (4) the instruction
      to respond with exactly one digit. Keep it minimal — small models degrade
      with long prompts.

- [ ] Wire Tier-3 decision hooks into the relevant rule classes:
      Function call line-breaking (§8/§9 interaction) is the primary target.
      Getter/setter grouping boundary is secondary. Each hook: generate
      candidates mechanically → call `AiDecisionClient` → execute chosen form.

- [ ] Update `README.md` ai-assist section with final config key names and
      grammar constraint format once implementation is stable.

- [ ] Update `FORMATTER_DISCUSSION.md` — close out remaining open items,
      update Key Decisions table.

---

## Checklist — Post-Phase-3 Cleanup

To be done after all Phase 3 items above are complete and the API surface
(config keys, env vars) is frozen.

- [ ] Rename all `STYLEFMT_*` environment variables to `JXMAKE_*` prefix.
      Grep: `grep -r "STYLEFMT_" src/`
- [ ] Rename all `.style-fmt` config file keys from unprefixed names to
      `jxmake-` prefix (e.g. `line-length` → `jxmake-line-length`).
      Update `Config.java` key strings and all docs in one pass.
- [ ] Rename `~/.config/style-fmt/` path to `~/.config/jxmake/` (or decide
      to keep tool-specific path — confirm before implementing).
- [ ] Update `README.md`, `README.txt`, `FORMATTER_DISCUSSION.md`, and any
      other docs referencing old key names or env var names.
- [ ] Verify no stale `STYLEFMT_` or unprefixed key references remain:
      `grep -r "STYLEFMT_\|style-fmt" src/ docs/`

---

## Resolved Design Decisions

| Key | Topic |
|---|---|
| RDD_EXT_1 | Selection prompt + grammar constraint confirmed working for Qwen2.5-Coder-3B on llama.cpp; `/v1/completions` preferred over native `/completion` for portability |
| RDD_EXT_2 | Model never rewrites source; JAR executes chosen candidate mechanically |
| RDD_EXT_3 | Fail-safe on unreachable endpoint: fall back to option 0, log warning, continue |

---

## End Goal (Phase 3)

- [ ] `ai-assist = local` works end-to-end: JAR formats a file with Tier-3
      decisions delegated to the local model, output is correct and stable
      across repeated runs (`temperature = 0.0`)
- [ ] Post-phase-3 cleanup complete: all env vars and config keys use the
      `JXMAKE_` / `jxmake-` prefix; no stale references remain
