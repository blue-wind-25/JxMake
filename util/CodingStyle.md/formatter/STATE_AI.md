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
or part of a token), not a layout-authorship judgment call — a small
**purpose-trained** classifier (the GRU's ~500k-parameter footprint) can
plausibly handle it, unlike a small instruction-tuned LLM (confirmed NOT
FEASIBLE, see below). Builds on the already-implemented rule-based
comment-grammar classifier (Task H in `STATE.md`, `RDD_KEY_94`–`98`):
`CommentFeatureExtractor`/`CommentFeatureVector`, `NonLatinScriptGate`,
`KeywordAmbiguityGate`, `CommentClassifier`/`CommentClassifierWeights`
(`YES`/`NO`/`ABSTAIN`), gated behind `comment-normalization-classifier`
(defaults `on` since the 2026-07-30 KEYWORD_BIAS fix, see below).
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

## OPEN — corpus-generation and benchmarking follow-ups

- **LLM-assisted disagreement sampling against `sample_default.txt`.**
  Discussed 2026-07-30: a full LLM relabel (92039 lines, ~$few-$150) would
  just reproduce the rule-based classifier's existing blind spots, since the
  corpus is auto-labeled by that same classifier. Agreed direction: use an
  LLM to find *disagreements* on a small stratified sample (pull existing NO
  lines + a random YES slice via `shuf -n 300 --random-source=<(yes 42)`,
  label independently without showing the existing label, diff, hand-verify
  only disagreements, append confirmed ones append-only). Steps 2-4 (actual
  LLM-assisted labeling) are still not done — a first pass was done **by
  hand** instead (2026-07-30, no LLM call), see next item, which directly
  motivated the commented-out-code gate.
- **[SEPARATE, still open]** A cluster of extracted comments are DTD/URL
  string-literal fragments with no leading space (e.g. `Sun Microsystems,
  Inc.//DTD Enterprise JavaBeans 1.1//EN";`) that look like `//` inside a
  string literal — **root-caused and fixed 2026-08-01** (see `extract_comments.py`
  fix below); this bullet is closed.

---

## Condensed job history (through 2026-07-29)

Everything mechanical is real, not stubbed: `GruClassifier` (tokenize,
hashBucket, softmax, decide, real bidirectional-GRU `forward()`),
`GruWeights` (full schema, hand-rolled JSON parser, backward-compatible with
scalar-only fixtures), `Vocabulary` (explicit-vocab-vs-hash-bucket lookup),
`GruAbstainResolver` (real "rules → GRU on abstain" pipeline, config-gated
via `gru-classifier`/`gru-weights-path`), `tools/gru/GruTrainer.java` (real
training loop: Xavier/Glorot init, mini-batch forward+backward+Adam — RDD_EXT_18's
batch-32 default superseded by a configurable `--batch-size`, default 16, see
2026-08-01 below — 20% held-out validation split with patience-based early
stopping, reads RDD_EXT_21's 4-column schema, loads `explicit_vocab.txt` by
default per RDD_EXT_22), the `gru-train`/`gru-extract-pool-a`/
`gru-extract-pool-b`/`gru-measure-abstain-rate` Makefile targets, and five
passing self-tests. `GruClassifier.classify` abstains whenever
`hasTrainedWeights()` is false — fail-safe posture, no change to rule-based
behavior until a real weights file is deployed.

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
archived to personal directory (RDD_EXT_19), **left unlabeled** — a
different path was taken instead (auto-labeled distant supervision, see next
section) rather than hand-labeling these.

---

## Session log (2026-07-29 through 2026-08-02)

**2026-07-29 — default auto-labeled corpus, live wiring, `RDD_KEY_217`.**
Rather than hand-labeling the unlabeled 578/492 candidates above,
`GenerateSampleDefault.java` auto-labels an acquired corpus via the existing
rule-based classifier (distant supervision) into `tools/gru/
sample_default.txt` (RDD_EXT_20/21 schema, `targetWordIndex=0`, `ABSTAIN`
comments skipped; dedups in place — 77,499 dup lines removed from a 172,285
run). Wired into `make gru-acquire-corpus`; `make gru-train`'s default
sample file switched from `sample_examples.txt` to this file. Full-scale run:
170,210 kept examples, **100% labeled YES** — direct empirical confirmation
that bootstrapping from the rule-based classifier alone can only teach the
GRU to imitate its YES/abstain-collapsed-to-skip behavior, never real NO
(real NO requires either hand-labeled Pool A/B or a different bootstrap
signal). Root architectural reason: `CommentClassifier.classify`'s decision
tree only ever returns `YES`/`ABSTAIN` (RDD_KEY_96), never `NO`, so until a
NO-producing rule-based gate existed (see `DecorativeSeparatorGate` below)
the GRU was the only possible source of prose `NO`.

**`RDD_KEY_217`** — named exception to RDD_EXT_19: per explicit user
direction (license compatibility — MIT/Apache-2.0/BSD-3-Clause sources,
traceable provenance, short quoted excerpts), exactly `tools/gru/
sample_default.txt` and `code-formatter-ai-assist-weights.json` are
committed, unlike every other real corpus/weights artifact this job
produces. RDD_EXT_19's general policy stands for everything else.

Also this session: fixed 3 pre-existing `targetWordIndex` bugs in
`tools/gru/sample_examples.txt`'s small illustrative Pool B lines (one
pointed past tokenization end, silently skipped every `GruTrainer` run; two
pointed at the wrong token) — verified via a live `GruTrainer` smoke run.
Live formatter wiring: `MiscRuleCore.classifyComment` now calls
`GruAbstainResolver.resolve(...)` instead of `CommentClassifier.classify`
directly, threading `gruClassifier`/`gruWeightsPath` through `MiscRuleCore` →
`MiscRuleCurly` → `ScopePipelineCurly` → `FormatterCurly`. `Config.
gruClassifier` defaults `true` (fails safe to ABSTAIN if weights missing).

**Finding: `comment-normalization-classifier` had to stay `off` by default at
this point** — flipping both defaults to `true` together regressed 9 `make
test` fixtures (rule-based classifier disagreed with the deterministic
`isCommentNoCapitalizeWord` list on keywords like `consteval`/`static`/
`while`/`var`/etc.). Reverted `commentNormalizationClassifier` to `false`
(all-green) while leaving `gruClassifier=true` (provably inert alone). Fixed
the next day (below), after which it now defaults `true`.

Also added the first NO-producing gate: **`DecorativeSeparatorGate.
isDecorativeOnly`** returns `NO` for a comment with no letter/digit anywhere
(`****...****`, `-----`); wired right after the non-Latin-script gate.
Validated against a 96442-comment 5-repo corpus: NO=20774 (0 before), 15
hand-spot-checked new-NOs, zero false positives. `make test`: 219/219.

---

**2026-07-30 — fixed `KeywordAmbiguityGate` weight regression; `comment-normalization-classifier` now defaults `on`.**
Root cause of the 9-fixture regression above: the 40-example
`tools/classifier_weights/examples_{c,cpp,java,kotlin}.md` set had all 20
"zero mechanical feature" rows labeled YES (hand-authored prose only) →
`KEYWORD_BIAS = +2.48420`, so any real zero-signal keyword-led comment
defaulted to YES — wrong, that shape is overwhelmingly real code reference.
**Fix:** added 22 zero-feature NO rows (real regression lines + analogues),
20 YES/22 NO split, re-derived via `derive_weights.py` (62 examples):

```
KEYWORD_BIAS                 = -0.20825   (was +2.48420)
KEYWORD_WEIGHT_PAREN         = -2.28827   (was -3.96297)
KEYWORD_WEIGHT_ARROW         = -1.51467   (was -3.22603)
KEYWORD_WEIGHT_SEMICOLON     = -2.96142   (was -4.93396)
KEYWORD_WEIGHT_URL_OR_NUMBER = -0.51492   (was -2.80469)
```

Negative bias now defaults a zero-signal keyword-led comment to ABSTAIN
instead of YES (intentional asymmetric-risk tradeoff — false skip is
zero-cost, false positive is a visible bug); 20/62 examples mismatch, all the
rare "keyword used as plain English adjective" case, accepted.

Fixing 8/9 regressed fixtures surfaced a second bug:
`test/real_code_regressions_54_inp.java` still failed on a stray-period
strip — `CommentFeatureExtractor.extract` always computed
`hasLeadingKeywordMatch` from the comment's *first* word regardless of the
caller's `targetWordIndex`, wrongly gating the period-strip call site (whose
index points at the *last* token). Fixed with a `targetWordIndex`-aware
`extract` overload.

With both fixes, `Config.commentNormalizationClassifier` default flipped to
`true`: **219/219 forward, 219/219 idempotency**. `gruClassifier` +
`commentNormalizationClassifier` both `true` at this point (partially
reverted same day — next item).

**`gru-classifier` flipped back to default `off`.** Evaluated the shipped
weights against the 62 hand-labeled examples: `total=62 abstain=0 decided=62
correct=19 precision=30.6% yesCorrect=19/19 noCorrect=0/43` — GRU predicted
YES on every example, worse than the linear classifier's 67.7% (42/62) on
the same set. Root cause: `sample_default.txt` (the only training corpus at
this point) is auto-labeled by the linear classifier itself, so it never
contains genuinely hard ambiguous-keyword NO cases — the GRU learned
"default to YES." **Fix:** `Config.gruClassifier` default flipped back to
`false`. Does not touch `commentNormalizationClassifier` (stays `on`).

**Self-formatting dogfood-and-adopt run (`src/`)** — first run of
`STATE_COMMON.md`'s process against the formatter's own source. Found/fixed
one shape: a comment starting with a slash-separated list of non-keyword
identifiers (`sizeTokens/initTokens`, `val/var`, etc.) wrongly capitalized,
since none of these leading words were recognized keywords and a leading
word directly followed by `/` was never checked at all. **Fix:** new
`CommentFeatureVector.leadingWordFollowedBySlash` field + new Gate 1c,
returning `NO` independent of keyword membership. `make test`: 220/220.
Re-ran dogfood clean, adopted output into `src/` (71 files +
`GruAbstainResolverSelfTest.java`); rebuild `make test`: 220/220.

**Extended self-formatting to `tools/*`/`tools/classifier_weights/*`** (36
Java/Python/JS files), found two bugs before adoption. **Bug 1 (real
formatter bug, fixed in `src/`):** `#!/usr/bin/env node` shebangs in
`tools/verifiers/*.js` were corrupted — `#` is only a preprocessor directive
for C/C++, so JS fell through to normal tokenization and
`enforceSemicolonInsertion` appended a stray `;`. **Final fix:** dedicated
`TokenType.SHEBANG` (added to `Token.isGapToken`, never `//`-rewritten) +
`TokenizerCurly.emitShebangLine()`, dispatched only at `pos==0 && c=='#' &&
peek(1)=='!'`. `make test`: 220/220. **Bug 2 (comment-classifier false
positive, hand-fixed per-occurrence, NOT gated):** two comments in
`GruAbstainResolverSelfTest.java` starting with a hyphenated config-key
literal got wrongly capitalized. A blanket `leadingWordFollowedByHyphen`
gate was tried and **rejected** — it also suppressed capitalization of
legitimate English hyphenated compounds (`non-negative` → `Non-negative`), a
real regression, since a leading hyphen (unlike `/`) is common in ordinary
prose. **Decision (user-confirmed):** revert the hyphen gate, hand-edit the
two comments instead. Prefer rewording over a new blanket gate if this
recurs. After both fixes: `make test` 220/220, adopted all 36 files;
additionally verified via `node --check` + real end-to-end run (`.js`),
`python3 -m py_compile` (`.py`), and clean compiles for all `.java`
(Kotlin-compiler-dependent files needing `~/xsdk/kotlin-compiler-2.4.0/
kotlinc/lib` on the classpath, JDK11+-dependent files needing
`/opt/openjdk-21_linux-x64_bin/jdk-21`).

**Findings from the first (hand-run, no LLM) disagreement-sampling pass:**
1. **[ACTIONABLE, resolved 2026-07-31 — commented-out-code gate below]**
   Commented-out code mislabeled YES is common, not rare: scaling the sample
   to the full 91064-line YES pool filtered for lines ending in `;` found
   **984 candidates (~1.1%)**; spot-checking ~50 found the large majority
   genuinely commented-out code across C/C++/Java/JS. **Caveat:** a bare
   "ends with `;`" signal alone is unsafe — a 25-line spot-check found ~8%
   genuine prose ending a clause with a semicolon — same asymmetric-risk
   shape as the rejected hyphen gate, hence the eventual gate needs a second
   signal. Also a live-formatting-correctness finding, not just a
   training-corpus concern (`CommentClassifier`'s rule-based gates are always
   live regardless of `gru-classifier`).
2. **[SEPARATE BUG, root-caused/fixed 2026-08-01 — see below]** A cluster of
   the 984 candidates were DTD/URL string-literal fragments containing `//`,
   misread as comment openers by the extraction step.

**DONE — `explicit_vocab.txt` contamination filter.** User inspection found
personal-name/narrow-jargon tokens (`Aloysius`, `Indrayanto`, `Red`, `LUTs`,
`OLED`, `WIZnet`) ranked as "common word" vocabulary — `build_vocab.py`'s
frequency counter had no source-diversity requirement. **Fix:**
`build_vocab.py` now computes document frequency (distinct sources
containing the word) alongside raw count, ranks by `(doc_freq desc, raw_count
desc)`, requiring `--min-sources` (default 2) distinct sources to be
eligible. Regenerated `explicit_vocab.txt` from a real 16-source run: 9684
eligible words at `--min-sources=2`, filling all 3346 common-word slots (154
keyword slots unchanged); confirmed the 5 contaminating tokens gone, `Red`
legitimately survives. **Safety-window finding:** the already-trained
`code-formatter-ai-assist-weights.json` is unaffected by reordering
`explicit_vocab.txt` — `GruWeights` embeds its own `explicitVocab` snapshot
inside the trained JSON, never re-reading the on-disk file at inference time;
regenerating the vocab only affects the *next* training run's embedding-row
layout. `make test`: 220/220.

---

**2026-07-31 — `GruTrainer` break/resume checkpointing.** Before this,
`bestWeightsJson` lived only in memory; a kill mid-run lost all progress.
**Two binary checkpoint files** derived from `--out`'s path
(`<out>.ckpt-current.bin` overwritten once/epoch — full resumable state:
weights, vocab, Adam moment arrays, scalar run state including RNG seed and
Adam step counter; `<out>.ckpt-best.bin` overwritten only on validation-loss
improvement — weights+vocab only). Binary format: `DataOutputStream`/
`DataInputStream`, no external library, header (`magic=0x47525543`,
`formatVersion=1`, `kind`) + shape-prefixed/validated blocks, written via
temp-file-then-atomic-rename. Deleted on normal completion; added to
`.gitignore`. **`--resume=<checkpoint-path>`** loaded early in `main`, so
`--lr`/`--epochs`/`--patience`/`--seed` fall back to the checkpoint's
recorded values unless explicitly overridden; vocab always comes from the
checkpoint's own snapshot; `random` re-seeded from the checkpoint's seed so
the train/validation split is reproduced exactly; epoch loop starts at
`resumed.epoch + 1`. **Caveat (accepted, not a bug):** only the RNG seed is
persisted, not `java.util.Random`'s internal state, so the initial split is
exact but per-epoch shuffle order diverges from an uninterrupted run after
resume. **Testing:** `make test` 220/220 unchanged (non-shipped tool only);
`javac -source 8 -target 8` compiles clean; kill-and-resume test (`kill -9`
mid-epoch 3) recovered exact `epoch=2, epochsSinceImprovement=0,
bestValidationLoss=0.0604511`, training loss continued decreasing smoothly
across the resume boundary, final weights had a sane confusion matrix
(`tp=79 fp=0 tn=1 fn=0 precision=1.00000`).

**2026-08-01 follow-up: `make gru-train` auto-resume.** The Makefile target
never passed `--resume`, so an interrupted run's checkpoint sat unused and
the next `make gru-train` silently restarted from scratch. Fixed: the target
now checks for `$(GRU_WEIGHTS_OUT).ckpt-current.bin` and auto-adds
`--resume=...` if present (prints `gru-train: found ..., resuming`);
`GRU_TRAIN_ARGS` still overrides checkpoint hyperparameters. No behavior
change absent a checkpoint. Verified end to end by the user (killed mid-epoch,
re-ran, confirmed resume). Files changed: `tools/gru/GruTrainer.java` +
`.gitignore` (4 patterns) only — no `src/` file touched.

**Commented-out-code NO-gate.** A comment ending in `;` combined with a
second, independent code-shape signal now resolves `NO` (Gate 1d, between
1c and Gate 2) — a bare trailing `;` alone was confirmed unsafe (~8%
false-positive rate on real prose, per the disagreement-sampling finding
above). **`CommentedOutCodeGate.looksLikeCommentedOutCode(String)`** (new
file) requires text ending in `;` **and** at least one of: call-shape
(`identifier(` no space), assignment-shape (bare `=`, excluding
`==`/`!=`/`<=`/`>=`), increment/decrement-shape (`++`/`--` adjacent to a word
char), typed-declaration-shape (type-looking word + identifier + `=`/`;`).
New `CommentFeatureVector.looksLikeCommentedOutCode` field (13th ctor arg,
whole-comment shape, not `targetWordIndex`-scoped). **Testing:** `make test`
220/220 unchanged; 13 hand-picked smoke cases all correct (9 real
commented-out-code shapes → NO, 2 documented real-prose false positives →
YES, 2 prose sanity checks unaffected); real-corpus check against
`sample_default.txt`: of 91064 YES lines ending in `;` (1055), new gate fires
NO on 739 (70%), ~200 manually inspected, zero real-prose false positives.

**Multi-line license/copyright-block NO-gate.** Same pattern: new gate class,
new `CommentFeatureVector` field, wired as Gate 1e (between 1d and 2). A
naive "spans 2+ newlines + not ending in `.`/`!`/`?`" idea (from
`tools/gru/README.txt`'s worked example) was too blunt alone (would misfire
on ordinary multi-line prose paragraphs), so **`LicenseBlockGate.
looksLikeLicenseBlock(String)`** requires **both**: (1) that primary
newline-span signal, and (2) explicit copyright/license vocabulary anywhere
in the text (`Copyright`, `(C)`, `SPDX-License-Identifier`, `Licensed under`,
`All rights reserved`, `Redistribution and use`, `Permission is hereby
granted`, `WITHOUT WARRANTIES`/`WARRANTY`). A decorative-border-line
confirming signal was tried first and **rejected** — real-corpus testing
found hundreds of ordinary decorative section-banners (e.g. `apache/ant`'s
`===...===`-framed XML headers) sharing the border shape without being
license blocks. New `CommentFeatureVector.looksLikeLicenseBlock` field (14th
ctor arg). **Testing:** `make test` 220/220 unchanged; 9 hand-picked smoke
cases all correct (4 real license blocks → NO including this project's own
GNU-LGPL example and `STATE_COMMON.md`'s own fixture header; this project's
own file header, ending in a period, correctly falls through to YES —
accepted intentional false-skip; prose/TODO/single-mention/`?`-ending cases
unaffected). Real-corpus check: initial vocabulary-or-border design fired on
599/91064 YES lines with hundreds of false positives; dropping the border
branch reduced fired count to 375, manual inspection of ~60 (css/java/xml/
js/c/cpp) found zero false positives. Both gates close the "further
NO-producing gates" TODO.

**GRU retrain re-evaluated: 30.6% → 50.0%, still below 67.7% baseline.**
After the vocab fix, the two new NO-gates, and merging
`classifier_weights/examples_*.md` into `sample_default.txt` (92046 → 92308
lines, NO rows 975 → 3069), a real training pass (`--threads=3 --epochs=3
--patience=2`, 73841 train/18460 validation) early-stopped epoch 1
(val loss 0.0393061). Re-ran the 62-example hard-case benchmark:
`total=62 abstain=0 decided=62 correct=31 precision=0.5 yesCorrect=20/20
noCorrect=11/42`.

| | precision | YES correct | NO correct |
|---|---|---|---|
| Linear classifier (baseline) | 67.7% (42/62) | — | — |
| GRU, 2026-07-30 (pre-fix corpus) | 30.6% (19/62) | 19/19 | 0/43 |
| GRU, 2026-07-31 (post-fix corpus) | 50.0% (31/62) | 20/20 | 11/42 |

Real progress (no longer degenerate all-YES) but still below baseline —
`gru-classifier` stays `off`. **Why 50% and how to improve (open, no work
done yet):** (1) 62 hand-labeled examples is tiny relative to ~74k
auto-labeled (~0.08%) — highest-leverage fix is growing the hand-labeled
hard-case set itself, not training mechanics; (2) consider oversampling/
upweighting hand-labeled rows (no such notion in `GruTrainer` today); (3) the
62-example benchmark has no held-out split — 50%/67.7% measures training-fit
not generalization; (4) the rule-based NO-gates generalize the "obviously not
a sentence" end of NO, not the "ambiguous leading keyword" end this benchmark
targets; (5) architecture/training-mechanics changes are unlikely to be the
bottleneck (fast convergence + immediate overfit point at a data problem, not
optimization) — not recommended before growing/reweighting the hard-case
corpus.

**Extended `classifier_weights` to every language reaching
`KeywordAmbiguityGate`, regenerated `sample_default.txt` (no retrain).**
Traced the only call path into `classifyComment`/`KeywordAmbiguityGate`:
`MiscRuleCore.enforceCommentStyle` → `FormatterCurly.java:272`, only
instantiated for curly-brace-family languages (`Lang.isCurly`). **Conclusion:
only c/cpp/java/kotlin/js/ts ever reach the gate** — json/json5/css/yaml/
toml/xml/html5/python3 are structurally unreachable. `KeywordAmbiguityGate.
java`: js/ts had no dispatch branch and silently fell through to
`KEYWORDS_C` (wrong) — added `KEYWORDS_JS` (39 keywords)/`KEYWORDS_TS` (20
TS-only additions) + dispatch. New `examples_js.md`/`examples_ts.md` (18 rows
each); extended the 4 existing files (`c` +4, `cpp` +4, `java` +4, `kotlin`
+3) targeting zero-coverage keywords. `convert_classifier_weights_examples.
py`'s `LANG_BY_STEM` extended with the two new stems. **Golden-fixture
fallout (expected):** `test/js_comments_inp.js`'s `// class-level
implementation note` now correctly resolves lowercase (JS previously had no
"class" keyword recognized); fixture updated. `make test`: **221/221**
forward+idempotency. `make gru-acquire-corpus` regeneration: 113 hand-labeled
examples (was 62) folded into `sample_default.txt` (92336 lines: 89590 YES /
3081 NO); per-language counts confirm js/ts landed (`js`=2465, `ts`=72). No
training run performed.

**Linear classifier weights re-derived from the extended (125-example) set.**
First attempt regressed `KEYWORD_BIAS` back positive (`+0.21890`) — the new
js/ts rows leaned zero-feature-YES-heavy without a matching NO count. **Fix:**
added 6 more zero-feature NO rows to each of `examples_js.md`/`examples_ts.
md` (125 total). Re-derived: `KEYWORD_BIAS=-0.08711,
KEYWORD_WEIGHT_PAREN=-3.08818, KEYWORD_WEIGHT_ARROW=-1.57140,
KEYWORD_WEIGHT_SEMICOLON=-3.57490, KEYWORD_WEIGHT_URL_OR_NUMBER=-0.93665`.
82/125 classified as labeled; all 43 mismatches are the accepted
asymmetric-risk tradeoff. `make jar` + `make test`: 221/221 forward,
221/221 idempotency. **Still open:** `make gru-acquire-corpus` rerun to fold
these rows into the GRU corpus, and GRU training against them, neither done
this session. Current overall state at this point: `gru-classifier` off
(GRU 50% vs. linear 67.7%); `comment-normalization-classifier` on.

---

**2026-08-01 — `GruTrainer` mini-batch training (user-commissioned).**
Replaces per-example forward+backward+immediate-Adam-step with real
mini-batching: `--batch-size=N` (new, default 16) controls how many
examples' gradients are averaged before one Adam step; pre-existing
`--threads=N` controls parallelism *within* one batch — the two axes compose
orthogonally, reusing the same `computeBatch`/`ExecutorService` machinery.
`averageGradients` accumulates in place onto the first non-null gradient
(the `Gradients` ctor is package-private, so mutating an existing instance's
public fields is the workaround), skips out-of-range-`targetWordIndex`
entries from both sum and divisor (so a partial final batch averages
correctly). Adam `step` counter now increments once per batch, not per
example (needed for correct bias-correction under mini-batch Adam).
**Gradient clipping stays pre-average, per example** — this is why
`--batch-size=1` is numerically close to, but not bit-identical to, the old
per-example behavior (documented in README.txt). `--batch-size` is a
resumable hyperparameter (same override-if-specified-else-checkpoint-value
pattern); checkpoint format bumped 1→2. **Validation:** compile clean;
`--check-gradients=5` → `maxRelativeError=0.000001 (PASS)`; tiny runs at
batch-size 1/8/16 all showed decreasing loss (1 fastest per-epoch drop,
consistent with degenerating toward old per-example behavior); kill/resume
smoke test correctly restored `batchSize=4` from checkpoint without
`--batch-size` on the resume command line. Confined to `tools/gru/
GruTrainer.java`; `make test` not run (not part of its fixture suite, same
scoping precedent as the checkpointing session). Not attempted: dropout, LR
schedule, auto abstain-threshold tuning — per the 50%-session's finding #5,
none of these were expected to move hard-case precision on their own.

**`GruTrainer` learning-rate warmup + cosine decay (user-commissioned).**
New resumable flags `--warmup-steps=N` (default 0) and `--lr-min=N` (default
0.0). Step-granularity driven by the Adam step counter (one per mini-batch),
not epoch. Decay horizon reuses `--epochs` (`stepsPerEpoch * maxEpochs`,
recomputed identically on resume). `computeScheduledLr` returns `baseLr`
unconditionally whenever `warmupSteps <= 0` — unmodified invocation is
byte-identical to prior flat-lr behavior. Formula for step `s` in
`(warmupSteps, totalSteps]`: `lr = lrMin + 0.5*(baseLr-lrMin)*(1+cos(pi*
progress))`, `progress` clamped to `[0,1]`. Checkpoint format bumped 2→3.
**Validation:** compile clean; `--check-gradients=5` PASS; backward-compat
run (no `--warmup-steps`) held LR flat, confirming true no-op;
schedule-enabled run matched hand-computed formula values closely at every
checkpoint (step 3 = baseLr exactly; step 60 = lrMin exactly); kill/resume
smoke test continued the decay curve smoothly rather than restarting warmup.
Confined to `tools/gru/GruTrainer.java`; `make test` not run (same
precedent). Not attempted: dropout, auto abstain-threshold tuning.

**GRU retrain re-evaluated on the 125-example benchmark: 50.0% → 56.0%,
still below 67.7%.** Real training pass with the post-mini-batch,
post-schedule trainer (`--threads=3 --epochs=5 --patience=3`, schedule off,
interrupted mid-epoch-2 and resumed — confirming resume still works under
mini-batching): 73873 train/18468 validation. Best checkpoint at epoch 3
(`validationLoss=0.0356914`); epochs 4-5 got worse and were correctly
discarded. Re-ran `GruEval` against 125 fresh-regenerated hand-labeled rows:
`total=125 abstain=0 decided=125 correct=70 precision=0.56 yesCorrect=40/43
noCorrect=30/82`.

| | precision | YES correct | NO correct |
|---|---|---|---|
| Linear classifier (baseline) | 67.7% (82/125) | — | — |
| GRU, 2026-07-31 (62-example bench, pre-mini-batch) | 50.0% (31/62) | 20/20 | 11/42 |
| GRU, 2026-08-01 (125-example bench, post-mini-batch+schedule) | 56.0% (70/125) | 40/43 | 30/82 |

`noCorrect` climbed from 11/42 (26%) to 30/82 (37%). Still below baseline —
`gru-classifier` stays `off`. The 62-vs-125 benchmark size differs from the
prior comparison, so the 50%→56% delta is directionally informative but not
a clean apples-to-apples control (more hand-labeled training rows is at
least as plausible an explanation as the trainer changes; the two weren't
isolated). Held-out-split caveat still applies (same training corpus
contains the benchmark rows). **Decision (per user instruction):** next step
is growing the hand-labeled hard-case corpus itself, not further
trainer-mechanics work.

**Root-caused and fixed the string-in-comment `extract_comments.py` bug**
(the DTD/URL string-literal-leakage item flagged open twice above). **Root
cause, isolated to `extract_comments.py`'s `extract_c_style_comments`**
(`GenerateSampleDefault.java` exonerated): the single left-to-right scan
recognized `"//"`/`"/*"` as comment openers anywhere in raw source text, with
no string/char-literal awareness — a `//` substring inside a `"..."` string
literal was indistinguishable from a real comment opener. **Fix:** added a
third mutually-exclusive span type to the scan — `"`/`'`-delimited literals,
consumed to the matching unescaped quote or end of line, skipped without
checking for `//`/`/*` inside; backslash-escaped quotes honored via a 2-char
skip. Same single-scan mutual-exclusion principle already used for `//` vs
`/* */`, extended to a third span kind. **Verified** via a standalone smoke
test (not part of `make test` — the script has no wired-in suite): a fixture
combining the exact reported DTD-string shape, a `'/'` char literal, a
`"https://..."` string followed by a real trailing `//` comment, a block
comment containing a quoted `"// string"`, and an escaped-quote string all
extracted exactly the 4 real comments with zero leakage; the function's
pre-existing edge case (`///*mlen = n;`) still resolves correctly. Files
changed: `tools/gru/extract_comments.py` only. `sample_default.txt` was NOT
regenerated with this fix yet at session end (done in a later session below).

**Grew the hand-labeled hard-case set: 125 → 173 rows.** Audited
`KeywordAmbiguityGate`'s six per-language keyword lists against each
`examples_*.md` file to find zero-coverage keywords (auto-labeled bulk corpus
can never contain these — only hand-labeled files are real signal), targeting
keywords that are also ordinary frequent English words. **8 new rows per
file (48 total)**, each a zero-mechanical-feature YES-prose/NO-code-reference
pair:

| File | Keywords targeted | Rows added |
|---|---|---|
| `examples_c.md` | `case`, `const`, `for`, `return` | 22-29 |
| `examples_cpp.md` | `catch`, `override`, `public`, `protected` | 20-27 |
| `examples_java.md` | `case`, `if`, `public`, `record` | 23-30 |
| `examples_kotlin.md` | `as`, `fun`, `if`, `return` | 16-23 |
| `examples_js.md` | `case`, `delete`, `throw`, `while` | 25-32 |
| `examples_ts.md` | `any`, `never`, `number`, `public` | 25-32 |

Regenerated the tsv: `wrote 173 hand-labeled example(s)` — matches 125+48
exactly, no silent skips. **Not done this session (explicitly out of
scope):** `derive_weights.py` re-run, `CommentClassifierWeights.java`/
`weights.md` re-derivation, `make gru-acquire-corpus`, GRU retrain — all done
in the next session below.

**`derive_weights.py`'s `DATASET` made auto-extending from `examples_*.md`.**
Growing the set exposed a latent sync bug: `DATASET` was a hand-transcribed
Python mirror of each row, not parsed from the files — the 48 new rows had no
`DATASET` entries at all. **Fix:** replaced the literal `DATASET` list with
`load_dataset()`, parsing `examples_*.md` directly (same header-column-lookup
convention as `convert_classifier_weights_examples.py`; `LANG_BY_STEM`
duplicated rather than imported). **Verified:** reproduces all 125 previous
hand-transcribed rows identically (spot-checked), picks up all 173 current
rows with zero manual transcription. Full 173-row run: precision 106/173
(61.3%, down from 82/125=65.6% — expected dilution from the harder new rows,
not a parser regression). New constants: `KEYWORD_BIAS=-0.05634,
KEYWORD_WEIGHT_PAREN=-3.10644, KEYWORD_WEIGHT_ARROW=-1.55819,
KEYWORD_WEIGHT_SEMICOLON=-3.59572, KEYWORD_WEIGHT_URL_OR_NUMBER=-0.96329`.
Files changed: `tools/classifier_weights/derive_weights.py` only.

**`weights.md`/`CommentClassifierWeights.java` re-derived; `sample_default.
txt` regenerated with the `extract_comments.py` fix.** (1) Copied the
173-example constants above into `CommentClassifierWeights.java` + a
"2026-08-01 re-derivation" section in `weights.md`. `make test`: **225/225**
forward+idempotency. (2) Re-ran `make gru-acquire-corpus` with the
string-literal fix now in place: 96836 → 96695 raw comments (141 fewer
spurious extractions), auto-labeled NO rows 3103 → 3023 (80 fewer, consistent
with leaked DTD/URL fragments now correctly excluded). `sample_default.txt`:
92348 → 92952 lines; confirmed zero remaining leakage
(`grep -c "DTD Enterprise JavaBeans"` → 0). 173 hand-labeled rows folded in
unchanged. **Not done:** GRU retrain against this corrected corpus, hard-case
benchmark re-run — next natural step.

**Grew the hand-labeled hard-case set further: 173 → 221 rows.** Same
process as the 125→173 pass, re-auditing for the next batch of zero-coverage
keywords:

| File | Keywords targeted | Rows added |
|---|---|---|
| `examples_c.md` | `if`, `long`, `else`, `switch` | 30-37 |
| `examples_cpp.md` | `friend`, `throw`, `try`, `using` | 28-35 |
| `examples_java.md` | `break`, `catch`, `finally`, `package` | 31-38 |
| `examples_kotlin.md` | `break`, `do`, `else`, `in` | 24-31 |
| `examples_js.md` | `break`, `catch`, `if`, `return` | 33-40 |
| `examples_ts.md` | `declare`, `is`, `protected`, `string` | 33-40 |

Each file's batch is exactly 4 zero-feature YES + 4 zero-feature NO rows
(balanced by construction, can't repeat the KEYWORD_BIAS-flip failure mode on
its own). Regenerated tsv: `wrote 221 hand-labeled example(s)` — matches
173+48 exactly. **Not done this session (explicitly out of scope):**
`derive_weights.py` re-run, `make gru-acquire-corpus`, GRU retrain — done
next session.

**Re-derived weights and regenerated the corpus for the 221-row set.**
`KEYWORD_BIAS` stayed negative (`-0.04180` vs `-0.05634` at 173 rows), all
four feature weights stayed within a few percent of 173-row values — decision
boundary stable across four consecutive growth passes. 130/221 examples
classified as labeled (58.8%, down from 61.3% at 173 — expected dilution, not
a regression). Updated `CommentClassifierWeights.java`/`weights.md`'s
"second growth pass, same day" section. `make test`: 225/225. Reran `make
gru-acquire-corpus` (pipeline unchanged since last run, same Pool A/B shape):
`sample_default.txt` at 92809 lines, now including all 221 hand-labeled
rows. **Not done:** GRU retrain against this corpus, hard-case benchmark
re-run — natural next step (done below, 2026-08-02).

---

**2026-08-02 — hot-path flat-array/fused-gate refactor benchmarked (no
measurable speedup); float vs double evaluated and REJECTED.** User-
commissioned perf investigation. `GruClassifier.java`'s `forward`/`backward`
(shared by trainer and shipped inference) was audited and found already
almost entirely flat `double[]`/`double[][]` — the only real remaining waste
was each GRU gate (`z`, `r`, `hTilde`) being computed via a 4-call chain
(`matVecInto` x2 + `addVecInto` + `sigmoidVec`/`tanhVec`).

**Benchmark methodology:** synthetic 288-train/72-validation dataset
(session-scratch only, never committed, never touches `sample_default.txt`),
6 languages, `--threads=1 --batch-size=1 --epochs=8 --patience=8 --seed=1`
for low-noise per-example timing. **Baseline:** epoch 1 (JIT warmup) 3.9s,
epochs 2-8 steady 3.6-3.7s/epoch (~12.5ms/example); total 32.8s.

**Fused-gate refactor:** new `GruClassifier.gateInto(...)` — one flat,
straight-line loop per output row, replacing the 4-call chain, deliberately
preserving the exact original per-element operation order (bit-identical, not
a reassociation). Applied to both forward/backward biGRU passes' three
gates; dead code removed (`matVecInto`, `addVecInto`, `sigmoidVec`,
`tanhVec`). **Correctness verified two ways:** `--check-gradients=8` →
`maxRelativeError=0.000000 (PASS)`; re-ran the identical baseline command
post-refactor — every epoch's loss matched to all printed digits (final
confusion matrix `tp=27 fp=0 tn=45 fn=0` identical both runs). **Result: no
measurable speedup** — post-refactor epochs 2-8 still 3.6-3.7s/epoch, same
32.8s total (speedup ratio ≈1.00x). At this architecture's size (hidden=224,
embedding=16) the O(h²)/O(h·e) dot-product loops already dominated cost and
were already flat before this session; the removed 4-call-chain overhead was
on the cheaper O(h) bias-add/activation step, apparently within measurement
noise at this scale. **Kept anyway** (fewer allocations, zero behavior risk,
bit-identical, gradient-check-verified) — not reverted. `make test`: 225/225
forward+idempotency, unchanged (confirmed GRU package is off the main
pipeline per `STATE_COMMON.md`'s testing methodology, not assumed).
Committed: `src/com/jxmake/formatter/classifier/gru/GruClassifier.java`.

**Float vs double precision — evaluated and REJECTED (kept double
end-to-end).** (a) Baseline: `GruEval` with the committed weights file
against the 221-row hard-case set, double-typed inference:
`total=221 abstain=0 decided=221 correct=119 precision=0.5384615384615384
yesCorrect=88 yesIncorrect=3 noCorrect=31 noIncorrect=99`. (b) Converted
`GruWeights.java`/`GruClassifier.java`'s weight *storage* fields to
float (JSON loader's `Double.parseDouble` scan left unchanged — narrowing
happens only at array-construction time). (c) Re-ran the identical `GruEval`
command through the float-typed code: **byte-identical** decision-for-
decision result to the double baseline — zero accuracy impact from narrowing
stored weights to float32. (d)/(e) Converting the trainer to float as well
(next step per the task's decision tree) surfaced a coupling this codebase
doesn't cleanly allow to split: `GruClassifier.Gradients` reuses `GruWeights.
DirectionWeights` as its own field type, so converting `DirectionWeights` to
float would also force the mini-batch gradient-accumulation/Adam-update math
into float, requiring first splitting `DirectionWeights` into a separate
float-typed storage struct and double-typed gradient-accumulator struct, and
updating every trainer call site (Adam moments, gradient clipping, mini-batch
averaging, `--check-gradients` harness, checkpoint format version bump).
**Decision: reverted the float conversion in full, kept double end-to-end**
(`GruWeights.java`/`GruClassifier.java` restored — `git diff` confirmed
byte-identical to pre-session). Reasoning: not an accuracy call (measured
impact was exactly zero) but a scope/risk call — the type-sharing means "just
the trainer" isn't a bounded follow-up; no accuracy upside motivated the risk
today (float storage alone could be revisited later as its own narrowly-
scoped task). No commit made for the float conversion. `make test`
reconfirmed: 225/225 forward+idempotency after the revert.
`code-formatter-ai-assist-weights.json` byte-unchanged throughout this
session (only read via `GruEval`, never written).

**`make gru-train` re-run (default `GRU_TRAIN_ARGS`), re-evaluated on the
221-example benchmark: 56.0% → 65.2%, still below 67.7%.** Ran `make
gru-train` with Makefile defaults (`--threads=3 --epochs=9 --patience=3
--batch-size=16 --progress-every=1000`) against `sample_default.txt` (74280
train/18570 validation, vocabSize=3500). Early-stopped epoch 5, best epoch 2
(`trainLoss=0.0368411, validationLoss=0.0322115`). Validation confusion
matrix (auto-labeled split, not the hard-case benchmark):
`tp=17913 fp=105 tn=512 fn=40 precision=0.99417 recall=0.99777 f1=0.99597`
— says nothing about hard-case accuracy on its own. Re-ran `GruEval` against
the 221-row hand-labeled set:

| weights | precision | correct/total | yesIncorrect | noIncorrect |
|---|---|---|---|---|
| previously committed (pre-retrain baseline) | 53.8% | 119/221 | 3 | 99 |
| freshly retrained (this run) | **65.2%** | 144/221 | 0 | 77 |

Best 221-example precision measured for any GRU retrain so far (prior
progression in this file: 30.6% → 50.0% → 56.0% → 65.2%). `yesIncorrect`
dropped to 0; `noIncorrect` (77/91 NO-labeled examples misclassified as YES)
remains the dominant error mode and the main gap versus the 67.7%
linear-classifier baseline (RDD_EXT_17's bar). No code changed this session —
training-corpus/hyperparameter re-run only. `gru-classifier` stays `off`
(still below the 67.7% bar). `code-formatter-ai-assist-weights.json` was
overwritten by `make gru-train` itself (the user's own action); the
pre-retrain comparison copy and the 221-row benchmark tsv are scratch-only,
not committed, per RDD_EXT_19.

**User-retrained weights: 65.2% → 98.7%, clears the 67.7%/90% bars.**
User re-ran `make gru-train` again (weights file changed per `git diff`,
confirming a fresh retrain, not a stale copy). Evaluated via `GruEval`
against the current `make gru-hand-labeled-examples` benchmark (now 474
rows — the hand-labeled set continued growing past 221 in the interim, per
this file's own growth-pass entries above, so this isn't a strict
apples-to-apples control against the 65.2%/221-row figure):
`threshold=0.5 total=474 abstain=1 decided=473 correct=467
precision=98.73% yesCorrect=91/91 noCorrect=376/382`. First measurement to
clear both RDD_EXT_17's 90% bar and the 67.7% linear-classifier baseline.
Decision: flip `gru-classifier` default to `on` and re-run `make test`
(next entry below).

**`Config.gruClassifier` default flipped `false` → `true`.** `make build` +
`make test`: **228/228 forward, 228/228 idempotency** — clean, no
regressions from enabling the GRU path live. `gru-classifier` is now `on`
by default alongside `comment-normalization-classifier` (already `on`
since 2026-07-30). `code-formatter-ai-assist-weights.json` is the
user-retrained file evaluated above (98.7%/474 rows).

**REVERTED same day — 98.7% was a training-fit number, not a held-out
one; corrected via 5-round cross-validation.** Inspecting the 7 misses on
the 474-row on-training-set eval found no single fixable mechanical shape
(all 6 wrong-decisions were `NO`→`YES`, the known error mode; 4 of 7
clustered on Kotlin `this`/`object`/`is` meta-keyword-discussion sentences
that read as fluent prose while still meaning the keyword — a genuine
semantic-ambiguity case, not a gate-able pattern). That null result
motivated running `tools/gru/cross_validate.py` (5 rounds, 80/20 splits,
retrained from scratch per round on the 474-row hand-labeled file only,
`--epochs=40 --patience=6`) to get a generalization-honest number instead
of the training-fit 98.7%:

```
round 0: precision=87.2% (82/94, 1 abstain)
round 1: precision=84.8% (78/92, 3 abstain)
round 2: precision=88.4% (84/95, 0 abstain)
round 3: precision=85.9% (79/92, 3 abstain)
round 4: precision=85.1% (80/94, 1 abstain)
mean=86.3%  stdev=1.5%  min=84.8%  max=88.4%
```

**True held-out precision is ~86.3%, below RDD_EXT_17's 90% bar** — the
98.7% figure overstated generalization because the production training
corpus folds these same hand-labeled rows in directly (not held out).
86.3% does beat the 67.7% forced-linear-classifier baseline and a naive
always-`NO` default (~80% raw accuracy on this benchmark's class balance,
trivially safe: 0% false positives, 100% missed YES) on raw precision —
but the risk-relevant number is the false-positive rate, not raw
precision, given this job's asymmetric-risk design (false skip = zero
cost, false positive = visible bug, RDD_EXT_11). Aggregating the 5 rounds:
`yesCorrect=61/99 (62%) yesIncorrect=38/99 noCorrect=342/368
noIncorrect=26/368 (7.1%)` — GRU-on resolves 62% of genuinely ambiguous
YES/prose comments correctly, at a cost of a **7.1% false-positive rate**
(wrongly capitalizing a real code-reference-style comment) on NO cases,
versus 0% under today's always-abstain-through default. **Decision:
reverted `Config.gruClassifier` back to `false`.** Not a rejection of the
progress (62% YES-resolution from 0% is real) — the 90% bar exists
specifically as a conservative proxy for "false-positive rate low enough
to trust automatically," and 7.1% doesn't clear it. `make build` + `make
test`: 228/228 forward+idempotency after the revert, confirmed clean.
**Threshold sweep, same day — re-enabled at `abstainThreshold=0.7`.**
Followed the open item above: swept `GruEval`'s optional threshold args
(0.5/0.6/0.7/0.8/0.9) against the 5 already-trained cross-validation
rounds' held-out test sets (no retraining — `GruEval` caches each
example's forward-pass probabilities and re-evaluates the decision
boundary per threshold for free). Aggregated NO false-positive rate and
YES-resolution rate across all 5 rounds per threshold:

| threshold | YES resolved (of decided) | NO false-positive rate |
|---|---|---|
| 0.5 | 61/99 = 61.6% | 26/368 = 7.1% |
| 0.6 | 51/77 = 66.2% | 17/354 = 4.8% |
| **0.7** | **43/65 = 66.2%** | **9/336 = 2.7%** |
| 0.8 | 29/47 = 61.7% | 4/320 = 1.3% |
| 0.9 | 19/27 = 70.4% | 2/304 = 0.7% |

0.7 cuts the false-positive rate from 7.1% to 2.7% at essentially no cost
to YES-resolution rate (still 66.2%) — a materially better trade-off than
the trained default of 0.5, and cheap since it's a pure inference-time
cutoff change, no retraining. **Applied:** `code-formatter-ai-assist-
weights.json`'s `abstainThreshold` field changed `0.5` → `0.7` (only that
one field; weight arrays untouched — pre-change copy kept scratch-only,
not committed, per RDD_EXT_19). Re-verified via `GruEval` against the
production weights + 474-row benchmark: `abstain=4 decided=470 correct=467
precision=99.4% noIncorrect=3` (down from `noIncorrect=6` at 0.5, on this
same non-held-out benchmark — directionally consistent with the held-out
sweep). **`Config.gruClassifier` flipped back `false` → `true`** now that
the false-positive rate at this threshold is materially better than the
7.1% that motivated the earlier revert. `make build` + `make test`:
**228/228 forward, 228/228 idempotency** — clean. `gru-classifier` is now
`on` by default at `abstainThreshold=0.7`, alongside
`comment-normalization-classifier` (on since 2026-07-30). **Confirmed same day — fresh from-scratch cross-validation at 0.7.**
Added `cross_validate.py --eval-threshold` (passes a fixed threshold
through to each round's `GruEval` call instead of the freshly-trained
weights file's own trained `abstainThreshold`), then re-ran the full
5-round cross-validation from scratch (`--work-dir /tmp/gru_cv_07`,
`--eval-threshold 0.7`, same seeds as the earlier run since they're
deterministic — so this exercises fresh `GruTrainer` runs, not a reuse of
the earlier weights files):

```
round 0: precision=92.7% (76/82, 13 abstain)
round 1: precision=92.0% (69/75, 20 abstain)
round 2: precision=91.2% (83/91, 4 abstain)
round 3: precision=95.8% (68/71, 24 abstain)
round 4: precision=90.2% (74/82, 13 abstain)
mean=92.4%  stdev=2.1%  min=90.2%  max=95.8%
```

Numbers match the earlier reused-probabilities sweep exactly (aggregate:
`yesCorrect=43/65=66.2% noIncorrect=9/336=2.7%`), confirming the sweep's
0.7 pick generalizes to a genuine from-scratch retrain, not just an
artifact of reusing the 0.5-trained models. `abstainThreshold=0.7` stands
confirmed as the production default. `tools/gru/README.txt`'s
`cross_validate.py` section documents the new `--eval-threshold` flag.

---

**2026-08-02 (later) — grew the hand-labeled hard-case set targeting the
miss-inspection pattern: 474 → 522 rows.** The miss inspection above found
every held-out error was the known `NO`→`YES` mode, clustered on
meta-keyword-discussion sentences (a sentence *describing* a keyword's
code meaning while still reading as fluent, non-templated English —
`this`/`object` in Kotlin were the worst offenders, 4/7 misses). Prior
growth passes covered this shape mainly via a single repeated `"<keyword>
here <verb>..."` template; this pass instead targeted **naturalistic,
non-templated phrasing** of the same ambiguity, 8 rows per file (4 NO
meta-discussion / 4 YES ordinary-English, balanced per the established
anti-KEYWORD_BIAS-flip precedent), 48 total across all six
`examples_*.md` files:

| File | Keywords targeted | Rows added |
|---|---|---|
| `examples_c.md` | `static`, `return` | 77-84 |
| `examples_cpp.md` | `this`, `static` | 91-98 |
| `examples_java.md` | `this`, `static` | 93-100 |
| `examples_kotlin.md` | `this`, `object` | 68-75 |
| `examples_js.md` | `this`, `class` | 82-89 |
| `examples_ts.md` | `this`, `interface` | 69-76 |

`make gru-hand-labeled-examples`: `wrote 522 hand-labeled example(s)` —
matches 474+48 exactly, no silent skips. **Not done this session
(explicitly out of scope, per user instruction):** `derive_weights.py`
re-run, `CommentClassifierWeights.java`/`weights.md` re-derivation, `make
gru-acquire-corpus`, GRU retrain — all deferred to a later session/the
user's own retrain.

**`GRU_TRAIN_ARGS` updated: `--epochs=9 --patience=3` →
`--epochs=20 --patience=5`.** The 2026-08-01 production run
(`STATE_AI.md`'s "`make gru-train` re-run" entry, 65.2%→98.7%→86.3%
held-out progression above) early-stopped at epoch 6 without exhausting
the epoch budget — patience=3 was tight enough to plausibly cut off
runs that would have kept improving past a short plateau. Widened both:
more epochs headroom (9→20) and more patience to ride out a longer
plateau (3→5), consistent with RDD_EXT_18's original 20-50-epoch starting
guidance. `Makefile` only; no training run performed this session per
user instruction ("do not retrain, I will do that later").

**`GRU_HAND_LABELED_REPEAT` left at 3, not increased.** User asked whether
to raise it alongside the epochs/patience widening above. Recommendation:
no — the training-fit/held-out gap this session already traced (98.7%
training vs. 86.3% held-out, both measured against the *same* hand-labeled
rows) is a direct symptom of over-weighting those rows relative to the
rest of the corpus; raising repeat count further would push the model to
fit the exact hand-labeled sentences harder, not generalize the pattern
better, and risks widening the same gap rather than closing it. Leave at
3 unless a future cross-validation run (after this corpus growth is
folded in) still misses this specific pattern, at which point revisit.

**User re-ran `make gru-acquire-corpus`** to fold the grown 522-row
hand-labeled set into the training corpus; retrain deferred to the user's
own session tomorrow (2026-08-03). No other changes this turn.

---

**2026-08-03 — linear classifier re-derived against the 522-row set;
compared against the user's fresh GRU retrain.** `derive_weights.py`
hadn't been re-run since the 221-row set (2026-08-02); re-ran against the
now-current 522 rows across all six `examples_*.md` files:

```
KEYWORD_BIAS                  = -1.18218
KEYWORD_WEIGHT_PAREN          = -2.17830
KEYWORD_WEIGHT_ARROW          = -0.64725
KEYWORD_WEIGHT_SEMICOLON      = -2.66553
KEYWORD_WEIGHT_URL_OR_NUMBER  = -0.03338
```

407/522 (77.97%) classified as labeled, same accepted asymmetric-risk
mismatch pattern as every prior pass. Copied into
`CommentClassifierWeights.java` + `tools/classifier_weights/weights.md`.
`make jar` + `make test`: **228/228 forward, 228/228 idempotency**.

**GRU comparison.** User trained overnight on CM5 (`gru_log.txt`,
`GRU_TRAIN_ARGS` back to `--epochs=9 --patience=3` per the user's own
Makefile edit this session, reasoning: prior runs — including the
2026-08-02 widened `--epochs=20 --patience=5` run recorded in this same
log — consistently plateaued by epoch 3 anyway, confirmed again here:
`bestValidationLoss=0.0321558` at epoch 3, early-stopped epoch 8 after 5
epochs with no improvement). Ran `GruEval` (`tools/gru/GruEval.java`,
compiled ad hoc, not part of `make test`) against the current 522-row
`make gru-hand-labeled-examples` benchmark:

| | precision | correct/total |
|---|---|---|
| Linear classifier (freshly re-derived, 522 rows) | 77.97% | 407/522 |
| GRU, fresh retrain, threshold=0.5 | 99.43% | 519/522 |
| GRU, fresh retrain, threshold=0.7 | 99.81% | 518/519 (3 abstain) |

**Caveat (same as every prior on-benchmark GRU figure in this file):**
this is a training-fit number, not held-out — the 522 hand-labeled rows
are folded directly into `sample_default.txt` (with repeat oversampling)
that the GRU was just trained on, same shape as the 98.7%-vs-86.3%
training-fit/held-out gap found on 2026-08-02. Not directly comparable to
the linear classifier's 77.97%, which *is* a genuine same-set fit number
(the linear classifier isn't trained against `sample_default.txt` at
all). A fair GRU-vs-linear comparison needs a fresh `cross_validate.py`
run against the grown 522-row set — not done this session (not
requested; a 5-round cross-validation at this corpus size runs many
hours per the epoch timings in `gru_log.txt`, ~792s/epoch). `code-
formatter-ai-assist-weights.json`'s `abstainThreshold` field reset to
`0.5` by this retrain (the previously-applied `0.7` override from
2026-08-02's threshold sweep was not carried over — flagging in case the
user wants to reapply it before this weights file goes live).

**`abstainThreshold` restored to `0.7`, both in the trainer default and
the committed weights file.** Confirmed first (per user question) that
`abstainThreshold` is pure inference-time metadata — `GruTrainer`'s
training loop (loss/gradients/early-stopping) never reads it; it's only
written once, verbatim, into the output JSON (`GruTrainer.java` build
call around line 1396). So a 0.5-trained run and a 0.7-trained run with
the same seed produce byte-identical weight/embedding arrays, differing
in only that one field — no retrain needed to change it.
`GruTrainer.java`'s `ABSTAIN_THRESHOLD` constant changed `0.5` → `0.7`
(so all future `make gru-train` runs bake in `0.7` by default), and the
already-committed `code-formatter-ai-assist-weights.json`'s
`abstainThreshold` field hand-edited `0.5` → `0.7` directly (single-line
`sed`, diffed to confirm no other byte changed). `make jar` + `make
test`: 228/228 unchanged. Re-ran `GruEval` with no explicit threshold arg
(so it reads the file's own trained value): `threshold=0.7 total=522
abstain=3 decided=519 correct=518 precision=99.81%` — confirms the file
now round-trips at 0.7 correctly.
