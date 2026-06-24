# STATE_NEXT_EXT.md — Phase 3: JAR ai-assist Integration

> **DO NOT READ OR IMPLEMENT AGAINST THIS FILE YET.**
> This file is gated until `STATE_NEXT.md`'s End Goal (Phase 2) milestone (the
> `AI_PREAMBLE.md` trim item) is marked complete. If you are a Claude CLI session
> and you have arrived here before that milestone is checked off, stop — return
> to `STATE_NEXT.md` instead.
>
> **Note:** `Main.java`, `README.md`, and the Dogfood test (originally
> `STATE.md`'s End Goal, then `STATE_NEXT.md`'s "End Goal (Phase 1)") now live in
> this file's Checklist — Phase 3, positioned just before "Step 2 — AI
> integration" rather than at the very start of Phase 3. Reason: "Step 1 —
> Deterministic extensions" below lands new branches inside already-COMPLETE
> rule classes (`MiscRule.java`'s call/declaration line-breaking) — same kind of
> risk to existing behavior as the Java 17+/C++20+ work in `STATE_NEXT.md`. The
> dogfood checkpoint needs to run *after* Step 1, not before it, so it actually
> catches any regression Step 1 introduces — running it any earlier would miss
> exactly the risk it exists to catch.

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

- The JAR generates N candidate layouts for a Tier-3 decision point
- A selection prompt is sent to the local model asking it to pick the best
  option by number
- A grammar constraint (`root ::= "0" | "1" | ... | "N"`) forces a
  single-token response — no prose, no reasoning output
- `temperature = 0.0` for deterministic selection
- The JAR uses the OpenAI-compatible `/v1/chat/completions` endpoint —
  llama.cpp applies the model's chat template (ChatML for Qwen, Llama-3
  format for Llama, etc.) automatically from the GGUF metadata, so no
  model-specific prompt tokens (`<|im_start|>` etc.) are needed in the JAR.
  Portable across backends (llama.cpp, Ollama, vLLM, LM Studio, etc.)
- The model never rewrites source text — the JAR executes the chosen layout
  mechanically using existing token-level rules
- AI is only invoked when there is a genuine choice between candidates —
  single-candidate cases are handled mechanically with no endpoint call

**Reference tools and models:**
- llama.cpp: https://github.com/ggml-org/llama.cpp
- Qwen2.5-Coder-3B-Instruct-GGUF: https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF
- Tested GGUF: `qwen2.5-coder-3b-instruct-q4_k_m.gguf`

---

## Function Call and Declaration Line-Breaking — Candidate Forms

> **Note:** This section describes the full design for §8 function *call*
> and *declaration* line-breaking (distinct from function *signature*
> line-breaking, which is already fully deterministic and unchanged).
> STYLE.md §8 currently only documents signature breaking. The CLI must
> update STYLE.md §8 to add these forms **before** implementing them —
> see the checklist below.

### The four candidate forms

**Option 0 — Inline:** all args on one line.
```cpp
myfunc(a, b, c, d, e)
```

**Option 1 — Dropped:** args stay on one line but dropped below `(`;
`)` on its own line. Only offered as a candidate when inline would exceed
the 100-char limit (i.e. option 0 is not viable).
```cpp
myfunc(
    a, b, c, d, e
)
```

**Option 2 — Preserve groups + align:** keep existing line breaks exactly,
ensure `)` is on its own line. Only offered when source is already
multi-line. This option is **fully deterministic — no AI involvement**.
The JAR applies it mechanically whenever the source is already grouped.

Alignment within each preserved group line differs by context:
- **Calls** — normalize spacing around `,` and between token expressions
- **Declarations** — apply the existing §5 column grid (modifier columns,
  type column, name column, comment column) across params within each group
  line, reusing `DeclarationAlignmentRule`/`ColumnGrid`/`ModifierPriority`
  infrastructure — no new alignment machinery needed

```cpp
// call — comma-spacing normalized within each group line
myfunc(
    a,        b,
    c,        d,
    e
)

// declaration — §5 grid applied within each group line
void myfunc(
    int      a, SomeType b,
    uint8_t  c, int      d, // related output params
    bool     e
)
```

Note: for calls, "align" means normalizing spacing around `,` and between tokens
within each preserved group line — not applying the §5 column grid (which is for
typed declarations only). The two lines above illustrate the difference: the call
example pads token spacing within each line; the declaration example aligns type,
name, and comment columns across params on the same line.

**Option 3 — One-per-line:** each arg on its own line, column-aligned;
`)` on its own line. Always a candidate.
```cpp
myfunc(
    a,
    b,
    c,
    d,
    e
)
```

### Candidate availability matrix

| Source form | Options offered | AI needed? |
|---|---|---|
| Inline, fits in 100 chars | 0 only | No |
| Inline, exceeds 100 chars | 1, 3 | Yes (`"0" \| "1"`) |
| Multi-line, inline fits | 0, 2, 3 | Yes (`"0" \| "1" \| "2"`) |
| Multi-line, inline too long | 1, 2, 3 | Yes (`"0" \| "1" \| "2"`) |

### Semantic grouping — explicitly out of scope

Grouping by parameter type similarity, name prefix/suffix, or any other
semantic signal is **never attempted** by the JAR or the local AI model.
Option 2 only preserves grouping the author already expressed via line
breaks — it never creates new groupings. Any semantic grouping is a
human judgment call, outside the scope of this tool at any phase.

### Comment handling within argument lists

- **Trailing comment after an arg** (`myfunc(a, b // note`) — align
  normally per §15 comment alignment rules within the group line.
- **Comment-only line between arg groups** — treat as opaque: preserve
  in place, do not reflow around it. Only compatible with option 2
  (preserve groups); options 0, 1, and 3 migrate it to trailing position
  on the arg it follows.
- **Inline block comment between args** (`myfunc(a, /* note */ b)`) —
  treat as opaque: normalize spaces around it, do not move it.
- **Leading preamble comment above first arg** (comment with no preceding
  arg on any line) — disqualifies options 0, 1, and 3; only option 2
  is offered. This is a strong signal the author wants the layout preserved.

### Distinction from signature breaking

Function *signature* breaking (declarations/definitions with a body `{`)
is already fully deterministic — inline if ≤ 100 chars, one-per-line
otherwise, per the existing §8 implementation. No AI is involved for
signatures. The candidate forms above apply to function *calls* and
*forward declarations / prototype params* — i.e. any parameter list not
directly followed by a body `{`.

---

## File Status

| File | Status |
|---|---|
| `STYLE.md` (add call line-breaking forms to §8) | NOT STARTED |
| `MiscRule.java` (option 1 dropped form + option 2 preserve-groups+align, for both calls and declarations) | NOT STARTED |
| `Config.java` (ai-assist, ai-endpoint, ai-model, ai-retry-interval keys) | NOT FEASIBLE (Step 2 deferred — see note) |
| `AiDecisionClient.java` (OpenAI-compatible `/v1/chat/completions` caller) | NOT FEASIBLE |
| `AI_DECISION_PROMPT.md` (prompt template — separate from AI_PREAMBLE.md) | NOT FEASIBLE |
| `MiscRule.java` (Tier-3 AI decision hooks) | NOT FEASIBLE |
| `README.md` (update ai-assist section with final config details) | PARTIALLY DONE (ai-assist section removed in chat session; `[~] NOT FEASIBLE` below covers remaining item) |
| `FORMATTER_DISCUSSION.md` (add Step 2 NOT FEASIBLE decision to Key Decisions table) | NOT STARTED |

---

## Checklist — Phase 3

**Step 1 — Deterministic extensions (no AI, implement first):**

- [ ] Update `STYLE.md` §8 — add the four call line-breaking candidate forms
      and the comment-handling rules documented in the Background section
      above. Do this before writing any code so the spec and implementation
      stay in sync.

- [ ] Implement option 1 (dropped form) and option 2 (preserve groups) in
      `MiscRule.java` — see RDD_KEY_4 for full architecture. Summary:
      new `enforceCallLineBreaking` whole-file pass; option 2 applied first
      (multi-line source, raw token stream, no `parseSignature`); option 1
      as fallback for inline-exceeds-100 (reuses `parseSignature` + new
      `renderDropped` method alongside existing `render`). Wire after
      `enforceEmptyParameterList`/`enforcePermitsClauseLineBreaking` in
      `Formatter.formatOne` Phase 1, before `formatNonInlineSwitches`.

      **No-AI fallback rule (applies when ai-assist is off or endpoint
      unavailable):** after the fit check determines inline is not viable,
      attempt dropped — render all args on one line indented one level and
      measure the result. If that line fits ≤ 100 chars → use dropped, done.
      If it still exceeds 100 chars → fall back to one-per-line (option 3).
      No ratio check or threshold needed — the fit check is the sole
      criterion. This applies to both calls and forward declarations.

- [x] Verify `parseSignature` bails on comment tokens between params:
      RESOLVED via source inspection (RDD_KEY_4) — `parseSignature` calls
      `significantOnly()` which strips all COMMENT tokens before parsing, so
      it silently ignores rather than bails on comments. This is safe for
      option 1. Option 2 must NOT use `parseSignature` at all (see RDD_KEY_4).

- [~] Implement option 2 (preserve groups + align) in `MiscRule.java`:
      Merged into the option 1 item above — both implemented in one new
      `enforceCallLineBreaking` pass per RDD_KEY_4.

- [ ] Verify options 0 (inline) and 3 (one-per-line) already work correctly
      for function *calls* (not just signatures) — they may need minor
      adaptation since the existing §8 pass targets signatures only.

**Step 1.5 — Dogfood checkpoint (regression gate before AI integration):**

Moved here from `STATE.md` (RDD_KEY_82, originally `STATE_NEXT.md`'s "End Goal
(Phase 1)"). Placed after Step 1 rather than before it: Step 1 above touches
already-COMPLETE `MiscRule.java` logic, so this checkpoint must run after Step 1
lands in order to actually catch any regression it introduces, covering the
core formatter plus the Java 17+/C++20+ additions plus Step 1 in one combined
dogfood pass — before the riskier AI-integration work in Step 2 begins.

| File | Status |
|---|---|
| `Main.java` | NOT STARTED |
| `README.md` (for both phase 1 and phase 2; defer until just before Dogfood) | NOT STARTED |

**`Main.java` note:** owns the temp-file cache layer for `IndentationDetector.detect()` in
standalone mode -- key = SHA hash of boundary dir absolute path string, stored as
`/tmp/style-fmt-indent-<hash>.cache`, content = detected style + `\n` + boundary dir
`lastModified` epoch ms. On read: if the file exists and its stored `lastModified` matches
current `Files.getLastModifiedTime(boundaryDir)`, return the cached style; otherwise delete
and rescan. `IndentationDetector` itself is unaware of this -- `Main` calls `detect()` with
a pre-populated single-entry map on a temp-cache hit, bypassing the scan entirely.

- [ ] Parse CLI args: `--server`, `--stop`, `--standalone`, `--diff`,
      `--check`, `--out DIR`, `--port N`, and one or more file paths.
      Unknown flags → print usage to stderr and exit 2.

- [ ] Config resolution: call `Config.resolve(filePath, cliFlags)` per file.

- [ ] IndentationDetector temp-file cache layer (standalone mode):
      Key = SHA-256 hex of boundary dir absolute path string.
      Cache file = `/tmp/style-fmt-indent-<key>.cache`
      Content = detected style + `\n` + boundary dir `lastModified` epoch ms.
      On read: file exists + stored `lastModified` ==
      `Files.getLastModifiedTime(boundaryDir)` → pre-populate single-entry
      map and pass to `IndentationDetector.detect()` (bypasses scan);
      otherwise delete cache file and rescan normally.

- [ ] Server auto-connect: if no `--standalone` flag, check lockfile; if
      server is alive, delegate to it via HTTP POST `/format` and exit;
      else run in-process via `Formatter.formatOne()`.

- [ ] `--server` mode: delegate to `ServerMode.start()` and exit.

- [ ] `--stop` mode: read lockfile for PID+port, POST `/shutdown` with
      short timeout, poll for lockfile removal, fall back to forceful kill
      on timeout (best-effort, see RDD_KEY_73/RDD_KEY_80).

- [ ] Output modes:
      in-place (default) — overwrite file with formatted content;
      `--diff` — print unified diff to stdout, do not modify file;
      `--check` — exit 1 if file would change, 0 if already formatted;
      `--out DIR` — write formatted output to DIR/<filename> instead.

- [ ] Exit codes: 0 = success / no changes, 1 = would change (`--check`) or
      formatting error, 2 = usage error (bad flags / no files given).

- [ ] Dogfood test — run formatter on its own `src/` tree, verify style compliance and that
      `make` still succeeds after
- [ ] Dogfood test — formatter applied to a Java 17+ / C++20+ sample set
      exercising every construct in `STATE_NEXT.md`, verify style compliance

**Step 2 — AI integration: NOT FEASIBLE (deferred)**

> The JAR cannot distinguish meaningful author-expressed argument grouping from
> arbitrary line breaks — this is the core prerequisite for reliable AI candidate
> selection, and no tractable heuristic exists for it. Without that signal, a small
> on-device model (3B–7B) has no reliable basis for choosing between candidates and
> produces inconsistent results. The mechanical fallback (dropped form if args fit on
> one indented line, one-per-line otherwise) is therefore the permanent behavior when
> inline exceeds 100 chars.
>
> The architecture (grammar-constrained single-token response via `/v1/chat/completions`,
> candidate layout generation, fail-safe fallback) remains documented here and in the
> RDD table as a valid design. If a grouping-intent heuristic is developed in the future,
> or if a larger model (7B+) proves reliable enough without one, Step 2 can be revisited
> without redesigning the infrastructure.
>
> Tier-3 aesthetic decisions (argument layout, non-standard getter/setter grouping) are
> handled by the capable-AI workflow in `README.txt` / `AI_PREAMBLE_AESTHETIC.md` instead.

- [~] All Step 2 items below are NOT FEASIBLE — no implementation needed.
- [~] `Config.java` ai-assist keys — NOT FEASIBLE
- [~] `AiDecisionClient.java` — NOT FEASIBLE
- [~] `AI_DECISION_PROMPT.md` — NOT FEASIBLE
- [~] `MiscRule.java` Tier-3 AI hooks — NOT FEASIBLE
- [~] `README.md` ai-assist section — DONE (AI section removed and replaced in chat session; no further CLI action needed for this item)
- [~] `FORMATTER_DISCUSSION.md` — update Key Decisions table to record this decision

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
- [ ] Rename `indent-style = keep` value to `indent-style = auto` — `keep`
      implies "preserve as-is" but the actual behavior is "detect project
      majority and apply it." Update `Config.java` (`INDENT_STYLE_CHOICES`,
      default), `IndentationDetector.java` (any internal string comparisons),
      docs (`README.md`, `FORMATTER_DISCUSSION.md`), and `.style-fmt` files
      in the repo if any use `keep`.

---

## Resolved Design Decisions

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
| RDD_KEY_4 | `MiscRule.java` call/declaration line-breaking architecture -- `parseSignature` strips comments via `significantOnly()`, so option 2 must bypass it and work on raw token stream; option 1 reuses `parseSignature` + new `renderDropped`; both in one new `enforceCallLineBreaking` pass |
| RDD_EXT_9 | Endpoint unavailability cache: standalone mode — static `endpointDead` boolean, skip all AI calls for process lifetime after first failure, single warning log; server mode — static `lastFailedAt` timestamp, skip AI calls for `ai-retry-interval` seconds (default 60) then retry once; connect timeout 500ms; fail-safe always falls back to mechanical result, never aborts formatting |

---

## End Goal (Phase 3)

- [ ] `STYLE.md` §8 updated with call line-breaking forms
- [ ] Options 1 and 2 implemented deterministically, verified by smoke test
- [~] `ai-assist = local` — NOT FEASIBLE (see Step 2 note above); mechanical
      fallback (dropped-or-one-per-line) is the permanent behavior
- [ ] Post-phase-3 cleanup complete: all env vars and config keys use the
      `JXMAKE_` / `jxmake-` prefix; no stale references remain
