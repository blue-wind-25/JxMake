# STATE_AI.md — AI-Assist Design Reference and GRU Job State

This file documents the background and architecture for the JAR's built-in
`ai-assist` feature. Step 2 (argument-layout/getter-setter-grouping) is
permanently NOT FEASIBLE and is reference-only — no active work there. Step 3
(the GRU comment-classifier abstain resolution) is now an active tracked job
per `CLAUDE.md`'s job table (`com.jxmake.formatter.classifier.gru`, skeleton
started) and follows the same `STATE_COMMON.md` process conventions as every
other job in that table.

Two separate determinations live here, for two different decision points —
they are not in tension with each other:

- **Step 2** (argument-layout / getter-setter-grouping candidate selection) —
  **NOT FEASIBLE**. No tractable grouping-intent signal exists for the JAR to
  hand an LLM, at any model size tested.
- **Step 3** (comment-classifier abstain-case resolution) — **FEASIBLE, GRU
  only**. This is a narrow classification decision, not a layout-authorship
  judgment call, and reuses Step 2's confirmed infrastructure pattern
  retargeted at a different decision point — but only via a purpose-trained
  bidirectional GRU (see "GRU implementation design" below). The
  small-instruction-tuned-LLM-as-classifier variant of Step 3 is **NOT
  FEASIBLE** — see "Small-LLM classifier fallback: NOT FEASIBLE" below.

---

## Step 2 — AI Integration: NOT FEASIBLE (deferred)

> The JAR cannot distinguish meaningful author-expressed argument grouping
> from arbitrary line breaks — this is the core prerequisite for reliable AI
> candidate selection, and no tractable heuristic exists for it. Without that
> signal, a small on-device model (3B–7B) has no reliable basis for choosing
> between candidates and produces inconsistent results. The mechanical
> fallback (dropped form if args fit on one indented line, one-per-line
> otherwise) is therefore the permanent behavior when inline exceeds 100
> chars.
>
> The architecture (grammar-constrained single-token response via
> `/v1/chat/completions`, candidate layout generation, fail-safe fallback)
> remains documented here as a valid design. If a grouping-intent heuristic
> is developed in the future, or if a larger model (7B+) proves reliable
> enough without one, Step 2 can be revisited without redesigning the
> infrastructure.
>
> Tier-3 aesthetic decisions (argument layout, non-standard getter/setter
> grouping) are handled by the capable-AI workflow in `README.txt` /
> `AI_PREAMBLE_AESTHETIC.md` instead.

Checklist status — Step 2 (all NOT FEASIBLE — no implementation needed):

- [~] `Config.java` ai-assist keys — NOT FEASIBLE
- [~] `AiDecisionClient.java` — NOT FEASIBLE
- [~] `AI_DECISION_PROMPT.md` — NOT FEASIBLE
- [~] `MiscRule.java` Tier-3 AI hooks — NOT FEASIBLE
- [~] `README.md` ai-assist section — DONE (AI section removed and replaced in chat session)
- [~] `FORMATTER_DISCUSSION.md` — update Key Decisions table to record this decision (NOT STARTED)

Checklist status — Step 3 (FEASIBLE via GRU only, design-only — see full
section below; nothing started, this is a design note, not scoped
implementation work yet):

- [~] Search Hugging Face for a current small instruction-tuned model per
      hardware tier (Pi CM5 / Core i5 CPU-only / <1GB VRAM GPU / 1–2GB VRAM
      GPU) — **NOT FEASIBLE, superseded** — see "Small-LLM classifier
      fallback: NOT FEASIBLE" below; no longer applicable, GRU is the only
      Step 3 approach
- [~] `Config.java` — new keys for enabling the LLM abstain-fallback and
      pointing at an endpoint — **NOT FEASIBLE, superseded** — no LLM
      fallback exists to configure; GRU has its own weights-file config
      surface instead (see "GRU implementation design" below)
- [~] Wire the LLM fallback into the existing `CommentClassifier` ABSTAIN
      path — **NOT FEASIBLE, superseded** — ABSTAIN now routes to the GRU
      classifier only (see "Fail-safe" note in "GRU implementation design"
      below)
- [~] `com.jxmake.formatter.classifier.gru` package — GRU now determined to
      be the preferred v1 approach (see "Model size determination" below),
      supersedes the earlier LLM-for-v1 lean — SKELETON ONLY:
      `GruClassifier.java` (`tokenize`/`hashBucket`/`HASH_BUCKETS`
      implemented per RDD_EXT_12/13, and made `public` — not just for
      `tools/gru/GruTokenizerSelfTest.java` below, but because RDD_EXT_13
      requires the training side to call the exact same hash/tokenizer as
      the runtime, and the trainer lives in a different package outside
      `src/`; `classify` is an unimplemented throwing stub) and
      `GruWeights.java` (`load` hand-parses the flat scalar schema via regex
      — no external JSON library exists in this project and the schema has
      no nested arrays yet; validates `schemaVersion` per RDD_EXT_14,
      hard-errors on mismatch or missing field; smoke-tested against a
      hand-written sample weights file). The embedding table, GRU weight
      matrices, and dense-head weights are not represented in `GruWeights`
      yet — those need the training pipeline to produce real numbers first.
      `tools/gru/GruTrainer.java` added as a skeleton `main()` entry point
      (non-shipped, outside `src/`, per the "Files" section below). CLI arg
      parsing/validation is real and unblocked (pure plumbing, no
      hyperparameter names/values decided by it): validates the two
      positional args (readable labeled-examples file; output-weights-path
      whose parent directory exists), collects any further `--key=value`
      args generically into an ordered map without interpreting specific
      keys, exits 2 with a usage message on any parsing failure. Only past
      that point does it throw, naming the parsed args in the error — actual
      training loop still blocked on open items 3/4/9/10 (hyperparameters,
      evaluation target, measured ABSTAIN rate, licensing check).
      `tools/gru/GruTokenizerSelfTest.java` added: a plain-`main()`
      assertion-based self-check for `tokenize`/`hashBucket` (no JUnit or
      other test framework exists anywhere in this project — the
      formatter's own testing methodology is the `_inp`/`_out`
      fixture-diffing in `test/`, which doesn't apply to internal classifier
      logic — so this follows the project's existing zero-dependency
      style). Covers punctuation-splitting, camelCase/snake_case wholeness,
      empty/whitespace-only input, hash determinism, and hash range; run via
      `java GruTokenizerSelfTest` after compiling — all checks currently
      pass. `tools/gru/GruWeightsSelfTest.java` added: same pattern, covers
      `GruWeights.load`'s happy path plus each error path (missing field,
      wrong schema version, malformed number, unreadable/nonexistent file)
      using small temp JSON files under the system temp dir (cleaned up
      after each check); run via `java GruWeightsSelfTest` — all checks
      currently pass. `load` also rejects a non-positive dimension field
      (`vocabSize`/`hashBuckets`/`embeddingDim`/`hiddenSize`/`sequenceCap`/
      `numClasses`) and an `abstainThreshold` outside `[0.0, 1.0]` as hard
      errors, instead of silently accepting a nonsensical shape; covered by
      two more `GruWeightsSelfTest.java` checks.
      `GruClassifier.softmax(double[])` (numerically-stable, subtracts max
      logit before exponentiating) and `GruClassifier.decide(double[],
      double)` (RDD_EXT_11's abstain-threshold mapping — top class must
      strictly clear the threshold, not just be the argmax, else `ABSTAIN`)
      added, both pure math/logic usable without a real forward pass.
      Introduced a fixed `CLASS_ORDER = {YES, NO, ABSTAIN}` constant as the
      softmax-output-index convention (an encoding choice, not one of the
      still-open items — any future training pipeline must emit its 3-way
      output in this order). Covered by `tools/gru/GruSoftmaxSelfTest.java`
      (sum-to-one, uniform on equal logits, numerical stability on large
      logits, highest-logit-ranks-highest, empty input, above/below/
      exactly-at threshold, wrong-length rejection) — all checks currently
      pass. `Vocabulary.java` added: the explicit-vocab-vs-hash-bucket
      lookup mechanism only (`lookup(word)` returns an explicit index if
      present, else `explicitVocabSize + hashBucket(word)`), case-preserved,
      no dedup surprises. The actual ~3.5k-word explicit vocab content
      (which keywords, which common words) is training-data curation, not
      an architectural decision, so it's a constructor argument, not
      hardcoded — currently unseeded. Smoke-tested with a small hand-written
      word list. No weights file format finalized/written yet, no wiring
      into `CommentClassifier`'s ABSTAIN path yet.

- [x] **Abstain-routing plumbing (stub GRU, real pipeline wiring)** — DONE, but
      still far from a real trained classifier; items 3/4/9/10 remain exactly
      as blocked as before (see "Remaining blocked open items" below).
      `GruClassifier.classify(String, int)` no longer throws — it still calls
      `tokenize` (future-proofing the tokenization path) but unconditionally
      returns `CommentDecision.ABSTAIN`, with updated javadoc explaining this
      is intentional stubbed behavior (forward pass blocked on a real trained
      weights file with an embedding table/GRU weight matrices, neither of
      which `GruWeights` represents yet), matching the project's fail-safe
      posture (missing/unusable signal → ABSTAIN → mechanical fallback, never
      blocks formatting). New class `com.jxmake.formatter.classifier.gru.
      GruAbstainResolver` (static `resolve(CommentFeatureVector features,
      String commentText, int targetWordIndex, Config config)`) is the real
      "Rules → high confidence / abstain → GRU classifier → final decision"
      pipeline call site: (a) calls `CommentClassifier.classify(features)`
      and returns immediately if non-`ABSTAIN`, touching no filesystem; (b)
      if `ABSTAIN` and `config.isGruClassifier()` is `false`, returns
      `ABSTAIN` immediately, again with no filesystem access (opt-in feature,
      no wasted I/O when off); (c) otherwise attempts `GruClassifier.load` at
      `config.gruWeightsPath()`, catching `IOException` as a fail-safe
      `ABSTAIN`; (d) on successful load, delegates to `GruClassifier.classify`
      and returns whatever results (currently always `ABSTAIN`, per the stub
      above). Does not touch `CommentClassifier.classify`'s pure rule-based
      signature/contract, and is not wired into `MiscRuleCore` (that
      integration remains its own separate, out-of-scope follow-up, same as
      `CommentClassifier`'s own pre-existing "not yet wired" status). Since
      the GRU stage always abstains right now, this cannot change any
      existing behavior — matches the hard "purely additive" constraint —
      but the pipeline code path is now real and exercised.

      Two new `Config.java` keys (added to `ALL_KEYS`, with fields/
      accessors/`fromRawMap` parsing, following the existing `parseBoolean`
      pattern for the first and a new minimal `parseString` helper — no
      choice-list validation — for the second, since no prior config key held
      an unconstrained filesystem path): `gru-classifier` (boolean, default
      `off` — opt-in, since no trained model exists yet) and
      `gru-weights-path` (string, default `Config.DEFAULT_GRU_WEIGHTS_PATH =
      ""`, empty). Both keys registered in `STATE_COMMON.md`'s "Config Keys
      and Defaults" table under a new `# ── AI-assist (GRU) ──` heading.
      `Config.isKnownKey`/env-var collection/in-file `JXM_CFMT_CFG`
      validation/server query-param validation all pick up both keys
      automatically (they iterate `ALL_KEYS`), so no other file needed
      editing.

      **Follow-up (same session): program-directory-relative weights
      resolution.** `gru-weights-path`'s default is empty rather than a fixed
      `target/`-relative path, per explicit user direction — `GruAbstainResolver`
      now derives the weights-file location from the running program's own
      directory when the config value is empty, instead of requiring an
      explicit path. New `GruAbstainResolver.WEIGHTS_FILENAME =
      "code-formatter-ai-assist-weights.json"` constant (the filename the
      top-level `dist_build/jxmake_dist/apps/code-formatter/` distribution
      layout is expected to use) and two new private helpers:
      `resolveWeightsPath(Config)` (returns `config.gruWeightsPath()` as a
      `Path` if explicitly set/non-empty, else `programDirectory().resolve(
      WEIGHTS_FILENAME)`, or `null` if neither is available) and
      `programDirectory()` (resolves the running program's own directory via
      `GruAbstainResolver.class.getProtectionDomain().getCodeSource()
      .getLocation()` — the jar's parent directory when run via `-jar`, or
      the classes directory itself for a dev/test run against `$(CLASS_DIR)`
      since there's no jar to take a parent of in that mode; returns `null`
      on any `URISyntaxException`/`RuntimeException`, treated as "can't
      resolve a default path" by the caller). A `null` from either helper is
      a fail-safe "no path" result — `GruAbstainResolver.resolve` treats it
      exactly like a missing/unreadable weights file, i.e. `ABSTAIN`, never
      throws. Verified via `programDirectory()` (called through reflection,
      since it's private) resolving to `target/classes` when run against
      `-cp target/classes` and to `target/` (the jar's parent) when run
      against the packaged jar — both confirmed by direct manual testing.

      `Makefile`'s `gru-train` target now also copies the generated
      `$(GRU_WEIGHTS_OUT)` into `$(CLASS_DIR)/code-formatter-ai-assist-
      weights.json` (active `cp` line) so `make test`/dev runs against
      `$(CLASS_DIR)` can find it via the same empty-path-derivation logic,
      plus a second, deliberately commented-out `cp $(GRU_WEIGHTS_OUT)
      code-formatter-ai-assist-weights.json` mirroring the pre-existing
      commented-out `@cp $(JAR_FILE) .` line — both are meant to be enabled
      together later for the top-level `dist_build/jxmake_dist` packaging
      step, per explicit user direction; not enabled yet since the jar-copy
      line itself isn't either.

      `GruAbstainResolverSelfTest.java` gained a fifth check,
      `checkEmptyWeightsPath_derivesFromProgramDirectory`, confirming (a)
      the default `gru-weights-path` is indeed empty, (b) resolving with it
      empty doesn't throw and returns a fail-safe `ABSTAIN` (no weights file
      is expected to exist at the derived location in the test environment),
      and (c) `programDirectory()` (invoked via reflection) resolves to a
      real, existing directory rather than `null` or a nonexistent path —
      guarding against a silent resolution bug that a bare `ABSTAIN` check
      alone wouldn't catch (both a resolution failure and a resolution
      success with no file present end in `ABSTAIN`). All five
      `GruAbstainResolverSelfTest` checks pass, alongside the three
      pre-existing GRU self-tests and `make test` (116/116 forward + 116/116
      idempotency, zero regressions, re-confirmed after this follow-up).

      New self-test `tools/gru/GruAbstainResolverSelfTest.java` (same
      zero-framework plain-`main()`-assertion style as the other three GRU
      self-tests), covering all four required cases: rules resolve
      non-`ABSTAIN` (GRU never consulted, weights path pointed at a
      nonexistent file, no error, rule-based result returned unchanged);
      rules `ABSTAIN` + `gru-classifier` off (falls through immediately, no
      load attempt, verified via a nonexistent weights path causing no
      error); rules `ABSTAIN` + `gru-classifier` on + weights file missing
      (fail-safe `ABSTAIN`); rules `ABSTAIN` + `gru-classifier` on + a real
      (small, hand-written temp) weights file present (loads successfully,
      `GruClassifier.classify` stub still returns `ABSTAIN`, final result is
      `ABSTAIN`). All four pass, alongside all three pre-existing GRU
      self-tests (`GruTokenizerSelfTest`, `GruWeightsSelfTest`,
      `GruSoftmaxSelfTest`, unmodified, still passing) and `make test`
      (116/116 forward + 116/116 idempotency, zero regressions — this task
      never touches language-formatting rule code).

- [x] **Trainer scalar-weights-file output + Makefile wiring** — DONE, same
      caveat as above: the real training loop remains exactly as blocked as
      before. `tools/gru/GruTrainer.java`'s `main()` no longer throws after
      CLI parsing — it now writes a weights file containing only the flat
      scalar fields `GruWeights.load` currently parses: `schemaVersion=1`,
      `hashBuckets=1024` (`GruClassifier.HASH_BUCKETS`), `embeddingDim=16`,
      `hiddenSize=224`, `sequenceCap=64` (the same finalized architecture
      constant as `GruClassifier.SEQUENCE_CAP`, written as a literal since
      that field is package-private and `GruTrainer` lives outside `src/` in
      a different package — not accessible from there), `numClasses=3`
      (`GruClassifier.CLASS_ORDER.length`), and `abstainThreshold=0.5`
      (RDD_EXT_11's stated default). `vocabSize` is derived by counting
      distinct whitespace-split tokens in the labeled-examples input file
      (`GruTrainer.countDistinctTokens`) — a natural, non-guessy count from
      real input, not an invented placeholder number. No embedding table, GRU
      weight matrices, or dense-head weights are written — `GruWeights` has
      no fields for them yet, so there is nothing to guess there; this is
      legitimate to implement now precisely because it only writes the
      schema that already exists. The `--key=value` hyperparameters map is
      still collected but not consumed by anything (documented in the
      class's javadoc and in the runtime's own printed summary) — there is no
      training loop yet to feed it to.

      New Makefile target `gru-train` (placed immediately after `clean:` and
      before the `# ── Test target ───...` header, per instruction, and added
      to `.PHONY` alongside the existing targets at line 27), depends on
      `$(JAR_FILE)`, compiles `tools/gru/GruTrainer.java` with `-cp
      $(CLASS_DIR)` into `$(BUILD_DIR)/gru/classes` (it references
      `GruClassifier`/`GruWeights` from `src/`, so needs that classpath), then
      runs it against a new placeholder/dummy labeled-examples file,
      `tools/gru/sample_examples.txt` (checked in, header comment explicitly
      marks it as placeholder/not-real-training-data, per the same open
      items 3/4/9/10 blocking real data), writing output to
      `$(BUILD_DIR)/gru/weights.json` — i.e. `target/gru/weights.json` — then
      also copies that file into `$(CLASS_DIR)/code-formatter-ai-assist-
      weights.json` (see the "Follow-up" note above), so a fresh `make build
      gru-train` run produces a file the resolver's empty-default
      program-directory derivation would actually find when run against
      `$(CLASS_DIR)` (still harmless/fail-safe if absent on a clean checkout
      before this target has run). Verified end-to-end: `make
      build` succeeds unmodified, `make gru-train` runs and produces a real
      `target/gru/weights.json`; its contents (`schemaVersion: 1, vocabSize:
      76, hashBuckets: 1024, embeddingDim: 16, hiddenSize: 224, sequenceCap:
      64, numClasses: 3, abstainThreshold: 0.5`) were confirmed to
      round-trip through `GruWeights.load` without error via
      `GruWeightsSelfTest`'s same parsing logic (not a fresh test case, but
      the identical schema shape already covered by that self-test's happy
      path).

---

## Background and Architecture (ai-assist)

Tracks the design for the JAR's built-in `ai-assist` feature — local
on-device AI for Tier-3 judgment-call formatting decisions.

**Hard constraint:** this work is purely additive. No existing Tier-1/Tier-2
rule behavior may change.

Confirmed working design (tested with Qwen2.5-Coder-3B-Instruct-Q4_K_M via
llama.cpp on Raspberry Pi CM5):

- The JAR generates N candidate layouts for a Tier-3 decision point
- A selection prompt is sent to the local model asking it to pick the best
  option by number
- A grammar constraint (`root ::= "0" | "1" | ... | "N"`) forces a
  single-token response — no prose, no reasoning output
- `temperature = 0.0` for deterministic selection
- The JAR uses the OpenAI-compatible `/v1/chat/completions` endpoint —
  llama.cpp applies the model's chat template automatically from the GGUF
  metadata, so no model-specific prompt tokens are needed in the JAR.
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

## RDD_EXT entries (AI-assist architecture, not in RDD_LOG.md)

These decisions were never externally logged — they have no entry in
`RDD_LOG.md` and no collision risk with RDD_KEY_n numbering. The related
`RDD_KEY_86`/`87`/`88` decisions that *are* externally logged appear in the
main index in `STATE.md`.

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
| RDD_EXT_16 | Step 3 GRU training-data source (was open item 10/licensing): own dogfooded repos first (src/jxm, local dogfood copies already used for real-code testing — clearly owned/licensed), extend later with a vetted list of permissively-licensed public repos once the pipeline itself is proven on the smaller corpus |
| RDD_EXT_17 | Step 3 GRU evaluation target (was open item 4): 90% precision bar for the GRU to resolve a rule-based ABSTAIN to YES/NO; below the bar, GRU itself abstains (RDD_EXT_11's mechanism). Starting number, not fixed — revisit once item 9's real measurement exists, adjust to 85% or 95% if the measured precision/coverage tradeoff calls for it |
| RDD_EXT_18 | Step 3 GRU training hyperparameters (was open item 3): documented starting defaults, not yet validated against real data — Adam optimizer, learning rate ~1e-3, batch size 32, 20-50 epochs with early stopping on validation loss, dropout 0.2-0.3. To be tuned once a real training set exists; these are a starting point, not a final answer |

---

## Step 3 — Comment-Classifier Abstain Resolution: FEASIBLE (via purpose-trained GRU only — see "Small-LLM classifier fallback: NOT FEASIBLE" further down)

Unlike Step 2, this is not a layout-authorship judgment call — it's a narrow
classification decision (does this word function as a keyword or as prose
here; is this trailing dot a sentence-ender or part of a token) that a small
**purpose-trained** classifier can plausibly handle, not a generator.
"Small" here means the GRU's ~500k-parameter footprint, not a small
instruction-tuned LLM — testing confirmed the latter fails at exactly this
task (see below); the two are different kinds of "small model" and this
section's feasibility claim applies only to the former. Builds on the
already-implemented rule-based comment-grammar classifier (Task H in
`STATE.md`, `RDD_KEY_94`–`98`): `CommentFeatureExtractor`/
`CommentFeatureVector`, `NonLatinScriptGate`, `KeywordAmbiguityGate`,
`CommentClassifier`/`CommentClassifierWeights` (`YES`/`NO`/`ABSTAIN`), gated
behind `comment-normalization-classifier` (default `off`).

The originally proposed pipeline routed `ABSTAIN` to a small
instruction-tuned LLM classifier — confirmed NOT FEASIBLE (see below),
superseded by the GRU-only pipeline in "GRU implementation design" below.

Reuses Step 2's already-confirmed architecture pattern rather than
reinventing it: grammar-constrained short response, `temperature = 0.0`,
`/v1/chat/completions` via llama.cpp/Ollama/vLLM/LM Studio, fail-safe
fallback to `ABSTAIN`-equivalent behavior (classifier `off`) on an
unreachable endpoint, and the same endpoint-unavailability caching described
in `RDD_EXT_9`. Only the target decision changes — a class label instead of
a layout-candidate index. **This architecture pattern is confirmed sound
(Step 2 validated it end-to-end) — what's confirmed NOT FEASIBLE is small
models' accuracy at this specific classification task, not the plumbing
around them.**

### Small-LLM classifier fallback: NOT FEASIBLE (confirmed by testing)

> Small instruction-tuned models (1B–3B class) cannot reliably tell whether
> a word at the start of a sentence is being used as plain English prose or
> as a language keyword — exactly the `KeywordAmbiguityGate`/Step 3
> classification task this fallback was designed for. **Tested and failed:**
> Qwen (1B–3B), Qwen2.5-Coder (1B–3B), Gemma (1B–3B). **Not tested, but not
> expected to fare better:** Llama 3B — same parameter-count class as the
> three tested families, no reason to expect a different outcome, so it is
> not being carried forward as an open question.
>
> This supersedes the "Earlier reasoning... favored a small
> instruction-tuned LLM" discussion below — that reasoning was correct about
> the *advantages* (no training pipeline, existing multi-language/
> programming-terminology understanding) but wrong about small models being
> *accurate enough* to cash in those advantages for this specific task. **A
> small on-device LLM will not be used for Step 3, full stop —** not as the
> v1 approach, not as a fallback behind the GRU, not for the non-Latin-
> comment routing option floated further below. The bidirectional GRU (see
> "GRU implementation design" below) is the only Step 3 approach going
> forward.
>
> This does not reopen Step 2's determination (Step 2 was already NOT
> FEASIBLE for an unrelated reason — no tractable grouping-intent signal
> exists at any model size, small or large) — the two determinations remain
> independent, as the file intro says. This also does not by itself rule out
> a *larger* model (7B+) for this task; no such test has been run. But no
> larger-model path is being designed here either — the GRU already covers
> v1, and revisiting an LLM approach of any size for Step 3 would need its
> own fresh justification and its own stop-and-ask, same as reopening Step 2
> would.

### Model size determination — GRU is the only v1 approach (small-LLM fallback removed)

Earlier reasoning in this doc favored a small instruction-tuned LLM for v1,
since it needs no training set and already understands programming
terminology and multiple languages out of the box, while a GRU/LSTM/MLP
would need a training pipeline and a labeled dataset built from scratch.

Further research changes this determination: a **bidirectional GRU with
~500k parameters is the best balance** of accuracy, latency, and footprint
for this narrow classification decision (not open-ended generation), and is
preferred over an LLM fallback for v1. Bidirectional is chosen because the
full comment text is available upfront at inference time (not streamed
token-by-token), so there is no autoregressive-latency downside — only
roughly 2x encoding compute for the added backward pass, which should be
affordable at this parameter count. This is a design-only determination —
nothing scoped or started yet; see "GRU implementation design" below for the
finalized architecture and the training-set sizing approach (training-set
size is deliberately not pinned to a number here — an earlier ~5M estimate
was superseded by a measure-first, two-pool approach; see that section).

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

The small-LLM fallback design above is kept in this document **only as a
historical record of a rejected approach**, not as a valid fallback option —
see "Small-LLM classifier fallback: NOT FEASIBLE" above. If GRU accuracy
proves insufficient in practice, the next step is not "fall back to a small
LLM" (confirmed not to work); it would need to be a fresh design discussion
(larger model? different GRU hyperparameters/training set? something else
entirely), not a revival of this rejected fallback.

### Non-Latin comments

`RDD_KEY_95`'s `NonLatinScriptGate` currently disables the rule-based
classifier entirely (equivalent to `ABSTAIN`) for any comment containing a
non-Latin codepoint, deferring those comments to the full-file AI pass
instead. **This open option is now closed, not just unstarted:** it
depended on the small-LLM fallback's multi-language understanding to route
some non-Latin/mixed-language ABSTAIN cases away from the full-file pass —
since that fallback is confirmed NOT FEASIBLE (see "Small-LLM classifier
fallback: NOT FEASIBLE" above), there is no Step 3 LLM branch left to route
them to. `RDD_KEY_95`'s established behavior (full-file AI pass for any
non-Latin-containing comment) stands unchanged. A GRU-based path for this
case would be a distinct, unexplored idea — training a classifier on
non-Latin/mixed-language examples specifically — not something this
document currently designs; raise as its own topic if it's worth pursuing
later, rather than assuming it falls out of the existing GRU design above.

### GRU implementation design (v1 target)

Design layout for when this is picked up. Architecture finalized via Q&A
(session-refined, superseding the earlier bare "~500k param bidirectional
GRU" placeholder — the ~500k param budget itself was already correct, this
section now fills in the specific config that hits it):

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
- `GruClassifier.java` — inference-only runtime code, shipped in the JAR.
  Loads a trained weights file at startup; never contains literal weight
  arrays in source (unlike `CommentClassifierWeights`'s baked-in
  linear-model constants — a neural net's weight count isn't hand-editable
  the same way, and retraining shouldn't require a JAR rebuild).
- `GruWeights.java` — loader/schema for the external weights file (JSON or
  a flat binary tensor dump; JSON preferred for v1 for easy
  diffing/inspection over a binary format).
- A `main()` training entry point in a **separate, non-shipped** location —
  e.g. `tools/gru/GruTrainer.java` or a `cwg/`-sibling directory, not under
  `src/`, so the runtime JAR never bundles training code or a training-only
  ML dependency. Takes a labeled example set path + hyperparameters as
  arguments, writes the trained weights file `GruClassifier` reads — the
  trainer **writes a weights file for the Java classifier to read at
  runtime; it does not overwrite or generate `.java` source**, so a retrain
  is a resource-file swap, not a recompile.

**Training-set acquisition, verification, fixing** (extends the existing
`cwg/` pattern from `RDD_KEY_97`, which is already frontier-model-assisted
rather than corpus-trained; the design below replaces the earlier flat
"~5M examples" estimate with a measure-first, two-pool approach —
session-refined, see rationale in each step):

- **Don't pre-commit to a total size before measuring.** The earlier "~5M
  examples" figure was a blanket guess made before this refinement pass.
  First step is to run the *existing* rule-based classifier
  (`CommentClassifier`, already implemented) over a large local+GitHub
  comment sample — no labeling yet, just counting — to find the real
  ABSTAIN rate. That number, plus the hit rate of the targeted extraction
  below, determines how much raw extraction actually gets you to a usable
  pool size, rather than assuming 5M up front.

- **Pre-filter through the existing classifier before labeling anything.**
  Every extracted comment gets run through `CommentClassifier` first:
  - **High-confidence YES/NO** → already resolved correctly for free — the
    rule-based classifier's own output *is* the label, no labeling cost.
    Keep a modest sample of these in the training set too (so the GRU also
    sees easy/unambiguous cases and doesn't develop a bias toward assuming
    everything is ambiguous), but the bulk of labeling effort should not go
    here.
  - **ABSTAIN** → this is the actual target, and where labeling effort
    concentrates. Labeling 5M *random* comments would spend full labeling
    cost on millions of cases the rule-based classifier already gets right
    for free; pre-filtering to ABSTAIN-only keeps the expensive part
    (frontier-model labeling + spot-check) focused on a much smaller,
    denser, more useful set.

- **Two separate pools, not one uniform corpus** — the two ambiguity classes
  this targets have different shapes and need different acquisition
  strategies:
  - **Pool A (keyword-ambiguity)** — the large pool. Observed pattern:
    ABSTAIN clusters around *short* comments (roughly ≤6-8 words) containing
    a known keyword with too little surrounding context to disambiguate —
    e.g. `// for i` or `// for matrix` (genuinely ambiguous) vs. `// for
    matrix below` or `// for error message handler` (clearly English once
    one more word of context is present). Extraction should therefore be
    **targeted, not random**: filter toward short comments containing a
    keyword from any supported/planned language, rather than sampling
    broadly across all comments (which would mostly pull in long
    unambiguous prose and waste extraction volume).
  - **Pool B (period-ambiguity)** — a separate, much smaller pool. This is
    not about brevity the way Pool A is — it's comments that discuss
    punctuation itself (`// The variable dot . is used...`) or unusual
    abbreviation patterns beyond the already-handled `e.g.`/`i.e.`/
    file-extension cases (`MiscRule.stripSoleTrailingPeriod`, see the
    mid-word-dot TODO elsewhere in this file). Expected to occur naturally
    at a low rate — likely low hundreds-to-thousands of examples via a
    targeted grep-and-review pass, not a bulk-extraction effort.
  - This split can be refined further once real data is in hand (e.g. if
    period-ambiguity turns out to occur at a high-enough natural rate
    inside Pool A's own extraction, a separate Pool B extraction pass may
    prove unnecessary) — noted here as the current best guess, not a final
    commitment.

- **Acquisition sources (both pools):** primary source is public web/search
  data (e.g. sourced via Google search or similar large-scale crawling) of
  real code comments across languages, supplemented with real comments
  pulled from this codebase and the `test/` fixtures (per the open TODO
  already in `cwg/`'s own notes). Licensing/provenance of any bulk-sourced
  data needs checking before use — not addressed yet, flagged as open work
  for implementation time.

- **Labeling — Pool A:** primary approach is a free frontier model labeling
  every extracted example (same precedent as `RDD_KEY_97`), not blind
  acceptance — spot-check review covers both a random baseline sample
  (~1-2%, to catch *confidently wrong* systematic blind spots that a
  confidence signal alone wouldn't flag) and every example the frontier
  model itself flags as low-confidence/hedged (cheap, high-yield on top of
  the random baseline). If free frontier-model access can't sustain the
  needed volume, fall back to a cheaper heuristic first pass (e.g. simple
  word-position rules) and only send what that heuristic can't confidently
  resolve to the frontier model — reduces call volume at some cost to
  catching the heuristic's own blind spots.

- **Labeling — Pool B:** no frontier-model pipeline needed — given its
  expected small size and that period-ambiguity is an easy call for a human
  to make at a glance, label by hand directly. Frontier-model assistance is
  still useful for the *extraction* step (finding candidate comments worth
  reviewing), just not for the labeling decision itself.

- **Verification:** flag likely-mislabeled examples via disagreement between
  the existing rule-based classifier's confidence and the assigned label,
  and via held-out accuracy regressions when a new batch is added — not
  just eyeballing.

- **Fixing:** reuses the existing `cwg/derive_weights.py`/`cwg/weights.md`
  reproducible-and-versioned pattern already established for the linear
  classifier's weights — when a spot-check finds a mislabeled example,
  correct it in place and document *why* it was wrong (which pattern, what
  the frontier model got confused by), not just that it was wrong. Over
  successive batches those notes become a working record of the frontier
  model's actual failure modes, useful for triaging future batches faster.

**Fail-safe:** a missing or unreadable weights file makes `GruClassifier`
behave as `ABSTAIN` — i.e. classifier `off` for that comment, falling back
to whatever Tier-1/Tier-2 mechanical behavior applies without it (there is
no further LLM fallback to fall through to — see "Small-LLM classifier
fallback: NOT FEASIBLE" above), matching the fail-safe posture everywhere
else in this design — never blocks formatting.

### Open refinement items (v1 target)

Architecture (embedding/vocab/hidden-size/target-word-handling) and the
training-data pipeline shape (measure-first sizing, two-pool split,
labeling/verification/fixing approach) are settled above via session Q&A.
Items 1, 2, 5, 6, 7, 8 below were pure judgment calls with no data
dependency, so they're now resolved (RDD_EXT_10–15, added to the index
above). Items 3, 4, 10 were likewise decidable by judgment call (session
Q&A, RDD_EXT_16–18) even without real data yet — recorded as starting
points, explicitly revisitable once real measurements land. Item 9 remains
genuinely open — it needs an actual measurement run, not a decision:

3. **Training hyperparameters** — resolved as a starting-point default, not
   a final answer. See RDD_EXT_18 (Adam, lr~1e-3, batch 32, 20-50 epochs,
   dropout 0.2-0.3, early stopping on val loss). To be tuned once a real
   training set exists.
4. **Evaluation target** — resolved as a starting-point bar. See RDD_EXT_17
   (90% precision to resolve ABSTAIN→YES/NO, GRU itself abstains below the
   bar). Revisit against item 9's measured rate once available.
9. **Real ABSTAIN-rate measurement** — the "run the existing rule-based
   classifier over a large sample first, measure before committing to a
   training-set size" step is planned but not yet executed; current
   pool-size estimates are directional, not measured. Still genuinely open.
   Tooling to run this measurement now exists (`tools/gru/extract_comments.py`
   + `tools/gru/CommentAbstainTally.java`, `make gru-measure-abstain-rate` —
   see "Remaining blocked open items" below) — the tooling doesn't resolve
   this item by itself, it still needs to actually be pointed at a real,
   sizeable comment corpus and run.
10. **Licensing/provenance check** for bulk-sourced GitHub comment data —
    resolved via data-source choice. See RDD_EXT_16 (own dogfooded repos
    first, vetted permissive public repos as a later extension).

Resolved this session (design-only, no code — GRU implementation itself
remains NOT STARTED per the checklist above):

- **Output classes (was item 1):** same `YES`/`NO`/`ABSTAIN` as the
  existing rule-based classifier — no more granular intermediate class
  (`KEYWORD`/`PROSE`/`IDENTIFIER`). Nothing downstream of the classifier
  consumes anything finer than `YES`/`NO`/`ABSTAIN` (the `ABSTAIN`-fallback
  wiring, `MiscRule.stripSoleTrailingPeriod`, etc.), so a granular head
  would add vocabulary/training-label complexity (a four-way labeling
  scheme instead of three) to feed a mapping step whose only output is the
  same three classes — no consumer benefits from the extra granularity.
  See RDD_EXT_10.
- **GRU's own abstain threshold (was item 2):** yes, the GRU can abstain —
  a softmax confidence check below a cutoff falls through to mechanical
  default, same posture as the missing-weights-file fail-safe. Default
  cutoff: 0.5 (i.e. requires the top class to hold a clear plurality over
  the other two combined, not just be the argmax) — a starting default,
  tunable, stored as a field in the weights file itself (see RDD_EXT_14) so
  retraining can adjust it without a code change. See RDD_EXT_11.
- **Tokenization edge cases (was item 5):** trailing/attached punctuation
  splits off into its own token (`matrix.` → `matrix` + `.`) — consistent
  with the existing rule-based classifier's own `dotCount`-based reasoning,
  which already treats dots as separable signal rather than part of the
  word. camelCase/snake_case identifiers stay whole as a single vocab/hash
  entry, not sub-tokenized — sub-word splitting would need its own
  segmentation scheme and adds complexity with no clear benefit for this
  task (identifiers are typically OOV either way and fall into the hash
  buckets; the classification signal comes from surrounding context words,
  not from decomposing the identifier itself). See RDD_EXT_12.
- **Hash function choice (was item 6):** FNV-1a (32-bit), result taken mod
  1024 for the bucket index — simple, well-known, single-pass, no external
  dependency, and trivially identical to reimplement on both the training
  side and in `GruClassifier.java` since both are just "hash this UTF-8
  string the same way." See RDD_EXT_13.
- **Weights file schema/versioning (was item 7):** the JSON weights file
  carries a top-level `"schemaVersion"` integer field (starting at `1`).
  `GruWeights.java`'s loader checks it explicitly and throws a clear error
  naming the expected vs. found version on any mismatch or missing field,
  rather than attempting to parse a shape it wasn't written for. The
  abstain-threshold value from the item above also lives in this same
  weights file (not hardcoded in `GruClassifier.java`), so a retrain can
  ship a new threshold alongside new weights in one file. See RDD_EXT_14.
- **Pool B's concrete extraction method (was item 8):** grep-based
  candidate extraction over the comment corpus, keyed on either (a) a
  comment containing two or more `.` characters where at least one is
  surrounded by whitespace on both sides (the punctuation-discussion case,
  e.g. `the dot . here`), or (b) a comment matching a short list of known
  abbreviation-adjacent tokens beyond the already-handled `e.g.`/`i.e.`
  (`etc.`, `vs.`, `approx.`, single-capital-letter-dot patterns like
  `extern C.`) not immediately followed by more lowercase sentence text.
  This is a recall-favoring first-pass filter, not a precise classifier —
  expected false positives get discarded during the existing frontier-model
  labeling step, not filtered out here. See RDD_EXT_15.

---

## TODO: Comment sentence-boundary detection defeated by mid-word dots (now FEASIBLE — Step 3 candidate)

`MiscRule.stripSoleTrailingPeriod` (§15) strips a comment's trailing `.`
only when it is the *sole* `.` in the comment text — a deliberately
conservative heuristic to avoid mangling an ellipsis or an abbreviation
followed by more sentence text. This misfires whenever the comment
legitimately contains an unrelated dot earlier in the sentence that is
*not* an end-of-sentence period. Example, using C++'s `//` form (the same
problem class applies to any comment syntax under the general
single-sentence-comments-never-end-in-a-period principle — Python's `#`,
CSS/XML/HTML5's block-only `/* */`/`<!-- -->` forms, per
AI_PREAMBLE_FULL.md §15's note that the mechanism varies by language but
the underlying rule doesn't):

```
// Combined .hpp test: pragma once, concepts, templates, classes, extern C.
```

Here `.hpp` (a file extension) and the trailing `C.` both count as dots, so
`dotCount != 1` and the genuinely-sentence-ending trailing period is left in
place (expected: stripped — the general rule above should still apply
regardless of which language's comment syntax is in play; treat the
snippet above as one worked example, not the scope of the problem).

Distinguishing a mid-word/mid-token dot (file extensions, `e.g.`, `i.e.`,
`v1.0`, single-letter abbreviations like `extern C.`) from a true
sentence-ending dot is a natural-language judgment call, not a mechanical
token-shape rule — no tractable heuristic was found within Tier-1/Tier-2
mechanical rules alone. **This is exactly the class of ambiguous,
ABSTAIN-worthy case Step 3 targets**: the existing rule-based classifier's
`dotCount != 1` case would ABSTAIN here rather than guess, and the GRU
classifier (see "GRU implementation design" above — Step 3's only feasible
approach, now that the small-LLM classifier fallback is confirmed NOT
FEASIBLE) would resolve it, provided its training set includes enough
mid-word-dot examples to learn the distinction. No longer blanket NOT
FEASIBLE — feasible via Step 3's GRU once that pipeline is implemented;
until then, remains an accepted mechanical-rule limitation (`dotCount != 1`
→ leave as-is).

---

## Remaining blocked open items (as of the abstain-routing-plumbing work)

Everything unblocked has been scaffolded/wired: `GruClassifier` (`tokenize`,
`hashBucket`, `softmax`, `decide`, `CLASS_ORDER`, and now `classify` itself —
a real method, but an intentional always-`ABSTAIN` stub, not a real forward
pass), `GruWeights` (flat-schema `load` with schema-version and sanity
validation), `Vocabulary` (explicit-vocab-vs-hash-bucket lookup, unseeded),
the new `GruAbstainResolver` (real "rules then GRU on abstain" pipeline,
config-gated), the two new `Config.java` keys (`gru-classifier`,
`gru-weights-path`), `tools/gru/GruTrainer.java` (CLI arg parsing plus now a
real scalar-architecture-constants-only weights-file writer — still not a
real training loop), its new placeholder `tools/gru/sample_examples.txt`
input, the new `gru-train` Makefile target, and four self-tests
(`GruTokenizerSelfTest`, `GruWeightsSelfTest`, `GruSoftmaxSelfTest`,
`GruAbstainResolverSelfTest`, all passing). **None of this amounts to a real
trained classifier yet** — no embedding table, GRU weight matrices, or
dense-head weights exist anywhere, `GruClassifier.classify` always returns
`ABSTAIN`, and `Vocabulary`'s ~3.5k-word explicit vocab is still unseeded.
Items 3, 4, and 10 from "Open refinement items" above are now resolved as
starting-point decisions via session Q&A (RDD_EXT_16–18 — data source, eval
target, hyperparameters). **Item 9 remains the sole genuinely blocking
item** — it needs an actual measurement run, not a decision:

9. **Real ABSTAIN-rate measurement** — running the existing rule-based
   `CommentClassifier` over a large comment sample to measure the actual
   ABSTAIN rate, before committing to a training-set size, hasn't been
   done yet. **Tooling for this now exists** (not yet run against real data):
   `tools/gru/extract_comments.py` walks a source tree, maps file extensions
   to `Lang`-recognized language strings, and extracts raw comment text
   (marker-stripped) per language's comment syntax into a flat
   `<lang>\t<escaped-text>` corpus file; `tools/gru/CommentAbstainTally.java`
   reads that file and feeds each comment through the actual
   `CommentFeatureExtractor.extract`/`CommentClassifier.classify` pipeline
   (not a reimplementation), tallying YES/NO/ABSTAIN counts overall and per
   language. Wired into the Makefile as `gru-measure-abstain-rate` (requires
   `GRU_ABSTAIN_INPUT=<path from extract_comments.py>`). Smoke-tested against
   this project's own `src/` (3337 comments, 0.5% ABSTAIN) — this project's
   own comments are mostly full sentences already, so that number is not
   representative; the real measurement still needs to run against
   RDD_EXT_16's chosen corpus (own dogfooded repos first).

   **Preliminary real-corpus runs (not yet the final measurement, no
   training-set-size decision made from these numbers yet):**

   - `~/Projects/eCxx` + `~/Projects/SusterCaller` + `~/Projects/VMA-GIT`
     combined: 65754 comments classified, overall ABSTAIN rate **0.4%**
     (c 34191/97 abstain = 0.3%, cpp 21130/114 = 0.5%, java 7032/31 = 0.4%,
     python3 2968/27 = 0.9%, xml/html5/yaml near-zero volume, 0% abstain).
     No kotlin/js/ts/json/json5/toml/css comments turned up (those repos
     have no files of those extensions), so this run doesn't exercise those
     language paths.
   - `JxMake/src/` (the build system's own source, `../../../src` relative
     to this directory): 22857 comments classified, overall ABSTAIN rate
     **1.4%** (java 22521/328 = 1.4%, xml near-zero volume at 0%).
   - **`CommentDecision.NO` never fires, in any run, any language — confirmed
     expected, not a bug.** Traced into `CommentClassifier.classify`
     (`src/com/jxmake/formatter/classifier/CommentClassifier.java:20-40`):
     there is currently no code path that produces `NO` at all -- the main
     path is a fixed `BIAS` constant compared to `THRESHOLD` (always `YES`
     for the non-ambiguous majority), and the keyword-ambiguity path
     (`KeywordAmbiguityGate.resolveAmbiguousKeyword`) only ever resolves to
     `YES` or `ABSTAIN` by design (its own comment: "a stage-2 'not prose'
     result is ABSTAIN, not NO -- no NO-producing path exists yet"). `NO` is
     a declared enum value with zero producers in current logic -- nothing
     to investigate further here.
   - Both runs land well under the earlier ~5M-examples-style planning
     assumption's implied ABSTAIN volume — at a ~0.4-1.4% ABSTAIN rate,
     Pool A's targeted-extraction approach (short comments containing a
     keyword, not random sampling) matters even more than assumed, since
     random sampling at this rate would need a very large raw volume to
     yield a usable labeled set.
   - `~/Projects/TTGO_VGA32_Lite` + `~/Projects/RobotCoding` combined: 67549
     comments classified, overall ABSTAIN rate **14.6%** — notably higher
     than the two runs above (c 47991/9774 = **16.9%**, cpp 5866/78 = 1.3%,
     java 2831/8 = 0.3%, python3 253/3 = 1.2%, kotlin/html5/css/xml/yaml
     near-zero volume at 0%). **Investigated and explained, not a formatter
     bug:** sampled the actual C-language ABSTAIN cases (ad hoc diagnostic,
     not committed) and found 9743 of the 9774 (99.7%) are
     `NonLatinScriptGate` hits (Gate 1), not keyword ambiguity — single-
     character comments (`ヽ`, `ゞ`, `あ`, `か`, ...) from one vendored
     third-party file, `TTGO_VGA32_Lite/3rd_party/tools/bfg/misaki/
     misaki_gothic_non_Kanji_list.h` (a bitmap-font glyph table listing
     Japanese kana one per comment, `.h` extension → classified as C). Only
     ~26-31 of the 9774 are genuine keyword-ambiguity ABSTAINs (e.g.
     `sizeof(TDialogColor)` — leading `sizeof` + open-paren correctly
     triggering `KeywordAmbiguityGate`'s negative paren weight). **Lesson
     for future corpus runs: a single vendored non-code-comment data file
     can dominate a language's ABSTAIN count and isn't representative of
     ordinary hand-written comment style** — future extraction passes
     should consider excluding known third-party/vendored directories (this
     run's `extract_comments.py` invocation did not exclude `3rd_party/`).
   - **`extract_comments.py` now excludes `3rd_party` directories** (added to
     `SKIP_DIR_NAMES` alongside `.git`/`target`/`node_modules`/`__pycache__`),
     per the lesson above. Both runs above were re-run after the fix:
     - `eCxx` + `SusterCaller` + `VMA-GIT`: 58739 comments (down from 65754 —
       the excluded volume was mostly non-comment noise elsewhere), overall
       ABSTAIN **0.4%** (unchanged) -- c 30351/81 = 0.3%, cpp 20732/109 =
       0.5%, java 7032/31 = 0.4% (unchanged, no `3rd_party` java there),
       python3 239/0 = **0.0%** (down from 0.9%/27 -- those abstains were
       apparently all inside a vendored `3rd_party` Python file).
     - `TTGO_VGA32_Lite` + `RobotCoding`: 38870 comments (down from 67549 --
       confirms `3rd_party` was the bulk of the corpus), overall ABSTAIN
       dropped from 14.6% to **4.6%** -- c 27502/1701 = **5.8%** (down from
       16.9%, confirming the misaki font table was almost all of the prior
       spike, though c is still the highest of any language/run measured so
       far), cpp 5752/75 = 1.3%, java 2831/8 = 0.3%, python3 253/3 = 1.2%,
       kotlin/html5/css/xml/yaml near-zero volume at 0%. **The remaining
       elevated C rate (5.8% vs eCxx's 0.3%) is investigated and explained --
       same root cause as before, just not caught by the `3rd_party`
       exclusion this time.** Sampled the 1701 C ABSTAINs (ad hoc diagnostic,
       not committed): 1408 (82.8%) are `NonLatinScriptGate` hits from
       per-glyph-annotation comments (`Character #224 0xE0 U+000003B1 'α'`)
       in bitmap-font header files checked in directly under `src/`, not
       `3rd_party/`:
       `TTGO_VGA32_Lite/src/BitmapFont_8x8_{DefaultExt,MisakiHKS,DefaultCP437}.h`.
       Same category of artifact (auto-generated glyph-table data, not
       hand-written prose) as the earlier misaki file, just not excludable
       by directory name since these copies live at the top level of `src/`.
       Of the remaining 293: most are legitimate keyword-ambiguity ABSTAINs
       on **commented-out real code** (`if (state->window == Z_NULL)`,
       `sizeof(unsigned char)`, `unsigned char h_samp_factor; /* ... */`) --
       looks like vendored zlib/libjpeg source embedded in the tree outside
       any `3rd_party`-named directory -- correctly abstaining, since these
       genuinely are code fragments, not prose; plus one more copy of the
       Misaki font's Japanese license-header block (also legitimately
       non-Latin). **Conclusion: this repo's elevated C rate is not a
       classifier bug and not (this time) a directory-name-excludable
       vendored file -- it's font-glyph data and embedded third-party
       library code checked in under `src/` itself.** Excluding by directory
       name alone can't fully solve this; a more targeted signal (e.g.
       filename patterns like `BitmapFont*`, or a per-repo vendored-file
       allowlist) would be needed, but that's increasingly bespoke per-repo
       cleanup rather than a general `extract_comments.py` improvement --
       not implemented, since it isn't clear it generalizes beyond this one
       repo.
   - **First public-repo runs (RDD_EXT_16's "extend later with a vetted list
     of permissively-licensed public repos" stage), both reused from prior
     `STATE_C_CPP_JAVA.md` dogfood clones already on disk (no re-clone
     needed) — MIT/Apache-2.0 only, per explicit user instruction to be
     careful with licensing on this measurement step (raw-comment-text
     extraction for internal measurement only, not redistributed, still not
     yet a licensing decision for actual training data — that remains
     RDD_EXT_16's own separate check when real acquisition starts):**
     - `microsoft/proxy` (`/tmp/proxy`, MIT — dogfood item 22): only 235
       comments (small repo, 28 files), 0.0% ABSTAIN across cpp/c/python3/yaml.
       Too small a sample to be very informative on its own.
     - `NVIDIA/stdexec` (`/tmp/stdexec`, Apache-2.0 — dogfood item 12): 10115
       comments, overall ABSTAIN **0.4%** (cpp 10014/45 = 0.4%, c/python3/yaml
       near-zero volume at 0%) — consistent with every own-repo run's ~0.3-0.5%
       cpp/c rate so far, no anomaly investigated (no elevated-rate flag like
       the TTGO/RobotCoding runs above).
     - `arrow-kt/arrow` (`/tmp/arrow_orig`, Apache-2.0 — `STATE_KOTLIN.md`'s
       "Finished dogfood / real-code testing" list, item 5, reused as-is):
       2787 comments, overall ABSTAIN **0.5%** (kotlin 2773/13 = 0.5%, toml
       negligible volume at 0%) — first Kotlin-specific public-repo data
       point, in line with the low own-repo Kotlin-adjacent rates seen so
       far, no anomaly.
     - `expressjs/express` (`/tmp/express`, MIT — `STATE_JS_TS.md`'s
       Test-Fixture Repos list; fresh shallow clone, no existing checkout on
       disk): 878 comments, overall ABSTAIN **0.1%** (js 798/1 = 0.1%, yaml
       negligible volume at 0%) — first JS-specific public-repo data point,
       again low, no anomaly.
   - These are still preliminary/directional, not the final item-9
     measurement: coverage is limited to whatever languages/repos happened
     to be scanned, and RDD_EXT_16's full corpus plan (own dogfooded repos
     first, then vetted public repos) hasn't been fully run yet -- these
     public-repo runs are a first step into that second stage, not its
     completion. The spread across runs, post-`3rd_party`-exclusion (0.4% /
     1.4% / 4.6% own-repo, 0.0% / 0.1% / 0.4% / 0.5% public-repo), is
     narrower than before but still shows the ABSTAIN rate is
     corpus-dependent, reinforcing that item 9 needs the full planned
     corpus, not a handful of repos, before sizing a training set.

Everything downstream is blocked on this measurement plus the actual
training-data acquisition it feeds into: acquiring/labeling the Pool A/Pool
B training sets from RDD_EXT_16's chosen sources (own dogfooded repos
first), populating `Vocabulary`'s ~3.5k-word explicit vocab content, adding
the embedding table/GRU weight matrices/dense-head weights to `GruWeights`,
implementing `GruClassifier.classify`'s actual forward pass, implementing
`GruTrainer`'s training loop using RDD_EXT_18's starting hyperparameters,
and writing the first real weights file, evaluated against RDD_EXT_17's 90%
precision starting bar. Do not guess at item 9 to unblock this work —
resolve it per `STATE_COMMON.md`'s ambiguity-handling protocol first (i.e.
actually run the measurement, don't estimate it).
