# STATE_COMMON.md — Shared Process Conventions

Read this file first, no matter which job you're picking up. It holds every
process convention identical across jobs — commit workflow, ambiguity
handling, file exclusions, testing methodology, RDD_LOG.md lookup
discipline — which the per-job file assumes and does not restate; that file
only contains what's specific to that job (Project Layout, Resolved Design
Decisions index, Open Questions, Checklist).

**Do NOT read `README.md`** unless the user explicitly asks. All decisions
relevant to implementation are recorded in each job file's own **Resolved
Design Decisions** index (full text in `RDD_LOG.md` — see lookup convention
below).

**ONLY** read the source file you are currently implementing or directly
modifying. Do NOT read other source files unless a specific checklist item
or ambiguity requires it.

**Dogfood corpus status**: `STATE_DOGFOOD.md` is the cross-job master index
of every dogfood corpus run/planned/rejected (done/partial-fix/not-started/
unsuitable). Check it before re-running a dogfood pass, and update its row
whenever a job's own state file gains, finishes, or rejects one.

---

## RDD_LOG.md lookup convention

Full decision text for every `RDD_KEY_n` lives in `RDD_LOG.md`, shared
across all jobs (one continuous numbering sequence — never restart it per
job). **Do not read `RDD_LOG.md` in full.** Look up one key at a time, and
**never add `-A`** (lines are very long, `-A` floods output):

```
grep -Fm1 'RDD_KEY_n' util/CodingStyle.md/formatter/RDD_LOG.md
```

---

## During implementation

- Implement one checklist section at a time.
- Checkpoint commit after completing a section, or when the cumulative diff
  exceeds ~50 lines (whichever first): (1) update the job's state file —
  check off items, update the active checklist; (2) `git add
  util/CodingStyle.md/formatter/`; (3) `git reset
  util/CodingStyle.md/formatter/target/` (exclude build output); (4)
  `git commit -m "<message>"` — short descriptive message, trailer ending
  `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`.
- Small trivially-related items may share one commit — use judgment against
  the ~50-line threshold.
- Never let implemented files and the state file drift out of sync.
- Never modify `test/*_inp.*` unless they contain syntax errors. Never
  modify `test/*_out.*` unless explicitly asked.
- `XL.txt` is the user's own personal tracker, not part of this process.
  Ignore it unless the user points at a specific item. Never reference
  `XL.txt` from any `STATE_*.md` file (including this note) — each must
  stand alone for a future session with no `XL.txt` context.
- New local fixture pair not from real-code testing: register in both
  `test/README.txt` and the `Makefile`'s `INP_FILES`, **before** the
  `Real-code regressions:` entries.
- Do not commit `code-formatter-1.0.0.jar` unless explicitly asked.
- **New fixtures are authored directly in `formatter/test/`** — no staging
  step (`../FUTURE_TEST_FIXTURES.md` is historical/empty). For a language
  with no real formatter logic yet, register commented-out in `INP_FILES`
  until logic lands; otherwise verify against the actual JAR before
  registering active.
- Use `/tmp` for temporary smoke-test/mini-test files. NEVER a
  filesystem-wide find; search `/tmp/claude-1000` or the project root
  first, else ask.
- Prefer evidence over reasoning when diagnosing a bug/regression: keep
  static analysis minimal (just enough to place debug prints), use debug
  prints + `make test` to diagnose/validate, remove prints once verified,
  commit only files actually modified. Do not put `RDD_KEY_*` text in a
  `test/README.txt` fixture-group title. If unsure, ask.

## When a file reaches COMPLETE

1. Update the relevant checklist in the job's state file.
2. Commit the state file together with the completed source file.

## When hitting an ambiguity or open question

1. **Stop coding immediately** — do not guess or proceed past the ambiguity.
2. Update the job's state file: add the question to **Open Questions**, mark
   the blocked checklist item with `[~]` and a note.
3. Commit the state file only.
4. Ask the user and wait for an answer before continuing.
5. Once resolved: append the full decision as a new row to `RDD_LOG.md`
   (next `RDD_KEY_n`, continuing the shared sequence), add the key + topic
   to the **Resolved Design Decisions** index in the job's own state file,
   remove from **Open Questions**, unblock the checklist item, then continue.

## Session end

- Always leave the state file committed and up to date before ending.
- The next session resumes from the first unchecked item in the current
  checklist.

---

## Real-code testing methodology

Preferred over synthetic dogfooding — finds concrete, fixable bugs faster:

1. Clone a real, compiling third-party project — search `/tmp` first for a
   prior-session checkout and reuse if found, else re-clone. Never a
   filesystem-wide search (e.g. `find /`) — only `/tmp`/scratchpad, or ask.
2. Format it once (round1).
3. Format round1's output again (round2).
4. `diff round1 round2` must be empty (idempotency).
5. Compile round1 with the appropriate toolchain — must succeed with the
   same error count as the unmodified original (no new formatter-induced
   errors).

Use `tools/verifiers` to syntax check. If there are many errors, work in
batch, store the rest in the corresponding state file.

**Diagnosing a hung `make test`/batch run**: `Main.main`'s per-file loop
(`--standalone` batch mode, one JVM invocation) prints
`jxmake-code-formatter: processing <file>` to stderr immediately before each
file, pinpointing a stuck file (added 2026-08-04 after a switch-case-reindent
fix attempt caused an infinite loop mid-`make test` with no way to tell which
fixture was stuck — see `STATE_C_CPP_JAVA.md`'s Tier-4-escalation entry).
Unconditional (stderr isn't diffed by `make test`).

**Diagnosing a hung server (`--server`/`make test-server`)**:
`ServerMode.FormatHandler.handle` similarly prints
`jxmake-code-formatter: processing <path>` (or `(no path, lang=<lang>)` for
inline-content requests) right before `GdrPipelineGate.applyAndFormat` —
matters more here because `HttpServer` has no explicit executor by default,
so one hung request blocks every subsequent request to that server instance
indefinitely. Verified via `make test-server` and `make test` (unrelated code
path, unaffected).

**Verifier toolchain** — needed to build/run `tools/verifiers/*` and
`tools/gru/*`; shared across every job touching those tools. Invoke via
their wrapper scripts, not `java`/`node`/`python3` directly — wrappers
encapsulate toolchain paths, runtime env, dependency checks, and (Java)
on-demand compilation.

Available wrappers:

```
_exec_java.sh
_exec_node_env.sh
_exec_nodejs.sh
_exec_python.sh

java_syntax_check.sh
java_content_diff.sh

kotlin_syntax_check.sh
kotlin_content_diff.sh

json_syntax_check.sh
json5_syntax_check.sh

css_syntax_check.sh
css_content_diff.sh

yaml_syntax_check.sh
yaml_content_diff.sh

toml_syntax_check.sh
toml_content_diff.sh

xml_syntax_check.sh
xml_content_diff.sh

html_syntax_check.sh
html_content_diff.sh

js_ts_syntax_check.sh
js_ts_content_diff.sh

python_syntax_check.sh
python_content_diff.sh

makefile_syntax_check.sh

bash_syntax_check.sh
```

**Every `*_content_diff.*` tool supports both single-pair and batch mode**
(2026-08-14, extended to all): single-pair takes `<original> <formatted>`;
batch takes `<original_base_dir> <formatted_base_dir>
<rel_path_file_list.txt>` (one relative path per line, resolved against both
base dirs), runs every pair in one process invocation (avoids per-file
process-restart cost on a large corpus). Batch mode prints a timestamped
`<relative path>` line before each pair (pinpoints a hang/slow file), treats
a rel-path missing from either base dir as warning-and-skip, wraps each
pair's compare in try/catch (one bad file can't abort the run), and ends
with `SUMMARY: N OK, N MISMATCH/ERROR, N MISSING` driving the exit code. Use
batch mode for any dogfood run over more than a handful of files.

**Do NOT use `git stash`.** Back up files to a temporary location, revert
for testing, restore afterward — avoids work hidden in a stash forgotten
after context compaction. **The system Git does NOT support `git
worktree`** — do not suggest or use it either.

**`--preserve-tree` + `--root DIR`** fix `--out DIR` basename-flattening
collisions: with both given alongside `--out DIR`, output path =
`outDir.resolve(rootDir.relativize(inputPath))`, preserving subdirectory
structure. Validation (exit 2 via `usageError`): `--preserve-tree` requires
both `--out DIR` and `--root DIR`; `--root DIR` without `--preserve-tree` is
also a usage error. A file not under `--root DIR` is a per-file
`IOException`. Fully opt-in/backward-compatible.

**Invoke the formatter JAR once per batch, not once per file.** `Main.run()`
accepts any number of positional file-path arguments in one JVM process
(each file independently resolves its own `.jxmake-code-formatter`
boundary, so mixing directories in one invocation is safe) — per-file
looping re-pays JVM startup each time. Collect the file list first:

```bash
find <candidate-dir> \( -name '*.hpp' -o -name '*.cpp' -o -name '*.h' \) -print0 \
  | xargs -0 <path-to>/code-formatter.sh --out /tmp/round1
```

If file count risks the shell/`exec` argv length limit, group by
subdirectory (one invocation per top-level subdirectory) rather than
falling back to one invocation per file — `xargs` (without `-n1`) already
chunks automatically if needed. Same applies to round2 and any
`--diff`/`--check` verification pass.

**`indent-size` fallback rule:** when an idempotency (or forward-pass)
failure doesn't reproduce at the default config, retest with a
`.jxmake-code-formatter` overriding `indent-size`/`indent-style`/etc. to
match the candidate's own convention before concluding "no bug" — some real
bugs are only observable at a non-default `indent-size` (e.g. `= 2`).

**When a bug is found and fixed, add a new permanent fixture pair:**
`test/real_code_regressions_N_{inp,out}.<ext>` (next available `N`)
reproducing it minimally, register in the `Makefile`'s `INP_FILES` and
`test/README.txt` — unless the bug is a no-op at the test harness's own
default config (then document the fix + non-default-config verification in
the state file instead, without a fixture indistinguishable from a no-op at
default settings). Try to combine multiple bugs in one fixture if possible.

Standard copyright header for every new test fixture file:

```
/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
```

In-file config as needed, e.g.:
```
/*% JXM_CFMT_CFG indent-size=2 */
```

---

## Server Protocol: Inline Config Support — DONE

`POST /format` accepts any config key as an optional query parameter, taking
priority over file-based `.jxmake-code-formatter` config for the same keys;
`path` is optional exactly when `lang` plus at least one inline config param
are present; unrecognized config-shaped query keys get HTTP 400. Body format
unchanged (no JSON — query-string only, decided design, do not re-litigate).
Covered by `make test-server`, documented in `README.md`'s Server Wire
Protocol section.

**Client env-var forwarding on delegation — DONE.** Tiers 2/3 of the
precedence chain were resolved by the server process (risking staleness for
tier 3, since a JVM's env is fixed at process start). Fixed via
`Config.clientEnvOverrides()`; `Main.delegateToServer` forwards its own live
`JXMAKE_CODE_FORMATTER_*` snapshot as inline query-param overrides on every
delegated request. Documented in `README.md`'s Configuration section
("Server mode note on tiers 2/3").

**3rd endpoint exposing config properties — DONE** (2026-08-06).
`Config.describeAll()` returns `List<ConfigProperty>`
(`group`/`key`/`defaultValue`/`allowedValues`, `null` for free-form,
`{"on","off"}` for booleans, `INDENT_STYLE_CHOICES`/`LINE_ENDINGS_CHOICES`
for the two enum-like keys). `group` mirrors README.md's `### Config file
format` order via a `GROUPS` ordered-map that `describeAll()` asserts covers
precisely `ALL_KEYS`. `ServerMode.java` gained `GET /properties`
(self-contained JSON writer — no existing serializer to reuse). Verified
end-to-end; `make test`/`make test-server` stayed green. README.md
documents the endpoint alongside `/format`/`/shutdown`.

---

## Config Keys and Defaults

`README.md`'s `### Config file format` section is the authoritative, full
list of every config key with default/allowed values (also queryable live
via the server's `/properties` endpoint). Do not hand-maintain a second full
copy here; it drifts. This section only holds maintainer-facing notes:

- `comment-normalization-classifier`: flipped on 2026-07-30 after fixing the
  `KeywordAmbiguityGate` weight regression — see `STATE_AI.md`.
- `curly-general-scope-reindent` / `curly-general-scope-reindent-multipass`:
  see `STATE_CURLY_GDR.md`, `RDD_KEY_233`/`RDD_KEY_234`.
- `html5-tc-gap-level`: cumulative 0-4, levels 1-4 implemented — see
  `STATE_HTML5_TCG.md`.
- `gru-classifier` / `gru-weights-path`: default on since 2026-08-02 (held-out
  cross-validation confirmed `abstainThreshold=0.7` keeps the NO
  false-positive rate low enough to trust) — see `STATE_AI.md`.
- `python-import-sort` / `python-import-blank-lines`: wired into
  `Config.java`'s `ALL_KEYS` 2026-08-06 (RDD_KEY_247) — see `STATE_PYTHON3.md`.
- `line-length-with-comment`: added 2026-08-09, default `120`. Scopes the
  "code + comment" width fits-check separately from the code-only
  `line-length` limit. Wired into the curly-brace family only:
  `MiscRuleCurly.enforceCallLineBreaking`'s whole-line fits-check and
  `JavaSpecificRule.isSingleLineBody`'s fits-prediction (RDD_KEY_172 requires
  these two to agree), each gated on a trailing comment actually being
  present (`hasCommentBetween`). NOT wired into any other language/pipeline
  — not lack of comment syntax (most data formats have comments), but none
  of their fits-checks measure comment-inclusive width at all yet; out of
  scope here, a future session building a real comment-inclusive fits-check
  for another pipeline should reuse this key rather than invent a new one.
  Landing this caused 2 pre-existing fixtures
  (`real_code_regressions_65_out.java`, `real_code_regressions_124_out.hpp`)
  to fail (code+comment width falls between the old 100-char limit and new
  120-char default) — intended, left for the user to review/update.

For every added, deleted, or modified configuration item, synchronize with
`README.md` and the *In-file Config Support* implementation
(`JXM_CFMT_CFG` directive, below).

---

## In-file Config Support — DONE

Core mechanism (RDD_KEY_167): `InFileConfig.parse(source)` (raw-text regex
scan, top-of-file preamble detection, duplicate/misplaced/invalid-key hard
errors as an ordinary per-file `IOException`), a new `Config.resolve(Path,
Map, Map)` overload with the in-file layer as highest priority, wired into
both `Main.formatStandalone` and `ServerMode.FormatHandler` (overrides the
server's own inline query-param config too).

Fixtures: `test/in_file_config_{inp,out}.hpp`/`.java`/`.kt` (one directive
setting every per-file-applicable key; `.java`/`.kt` each prove their
reversed `*-import-order`) and `test/in_file_config_error_{inp,out}.hpp`
(registered but commented out — a hard-erroring input has no formatted
result to diff, exercised manually not via `make test`). Both registered in
the Makefile's `INP_FILES` and `test/README.txt`, ahead of
`real_code_regressions_*`. RDD_KEY_168 records the `.hpp` fixture's design
pivot away from `header-guard-rename` (untestable via `_inp`/`_out` diffing)
to `format-macros=off` instead, which also proves override precedence over
the Makefile `test:` target's own `FORMAT_MACROS=on` env var.

`README.md` has an "In-file config overrides" section (directive must be its
own separate comment, never merged into another comment's prose; must
appear "before the first non-comment/non-blank line") and the Configuration
precedence list has a 7th tier for this directive. Full narrative:
`RDD_KEY_167` (core mechanism/precedence/hard-error rules) and `RDD_KEY_168`
(fixture design pivot) in `RDD_LOG.md`.

**`--lang` pseudo-key — DONE (2026-08-12, RDD_KEY_286).** The directive also
accepts `--lang=<language>` (e.g. `//% JXM_CFMT_CFG --lang=cpp`) as a
per-file language override, same values as the CLI `--lang` flag/server
`lang` query param, same highest-priority precedence (wins over CLI/server
`lang` too). Not a `Config` key — `InFileConfig.parse` special-cases the
literal key `"--lang"`, validates against `Lang.isRecognized` instead of
`Config.isKnownKey`, leaves it in the returned map for the caller to
`.remove("--lang")` before passing the rest to `Config.resolve`.
`Main.processFile`/`ServerMode.FormatHandler` both reordered to read the
file/request body (needed to parse the directive) before deciding the
file's language. Two real use cases: a `.h` file that's actually C++ (see
`STATE_C_CPP_JAVA.md`'s `.h`-defaults-to-C Open Question), and a templated
source file whose extension can't be inferred at all (`.java.in`/
`.java.inc`). Fixture: `test/in_file_config_lang_{inp,out}.h`. `README.md`
updated accordingly.

---

## Class Refactor (curly/indent/tags split) — DONE

**Purpose:** `TokenizerCore`, `Formatter`, `ScopePipeline`,
`DeclarationAlignmentRule`, `GetterSetterRule`, `MiscRule` held only
curly-brace-family (C/C++/Java/Kotlin) logic. Ahead of Python3/data-format/
JS-TS jobs landing real logic, each was split into a slim `*Core` base plus
family siblings (`*Curly`, and skeletons for `*Indent`/`*Tags`) so each
future job gets a clean landing file. Mechanical rename/move only, no
behavior change. `Lang.java` gained `isCurly`/`isIndentBased`/`isTagBased`
predicates first; `FormatterCore.forLanguage(String)` is the static
dispatcher factory (picks `Curly`/`Indent`/`Tags` by family) —
`Main.java`/`ServerMode.java` need no if/else on language.

**Scoping:** `DeclarationAlignmentRule`/`GetterSetterRule` got
Core+Curly(+Indent skeleton) only, no `Tags` — XML/HTML have no
declaration/getter-setter concept. `TokenizerCore`/`Formatter`/
`ScopePipeline`/`MiscRule` got the full Core+Curly+Indent+Tags split.
`ComplexityPaddingEvaluator.java` not split (extend in place when needed).

**Plan deviations** (when a "Core" method called a "Curly" one, the callee
moved to Core alongside its caller instead): `DeclarationAlignmentRule`'s
`renderTokens`/`renderInitTokens`/`needsSpaceBetween`/`isTightToken`/
`isCStyleCastClose` (+ `CONTROL_FLOW_KEYWORDS`) stayed together in Core;
`MiscRule`'s `needsSpaceBetween`/`isTightToken`, `capitalizeFirstLetter`/
`isCommentNoCapitalizeWord` (+ language-specific no-capitalize sets),
`renderTokens`/`templateAngleTokens`, and generic scan helpers
(`matchParenForward/Backward`, `next/prevSignificantIndex`, `anyFrozen`,
`significantOnly`/`significantWithComments`) all stayed in Core
(`splitTopLevelCommas` moved to Curly despite its generic name — only used
by Curly's signature/call-rendering). `KotlinDeclarationAlignmentRule`/
`KotlinGetterSetterRule`/`KotlinSignatureRule` extend `*Curly` (not `Core`)
— they reuse Curly-side protected members.

**Reusable gotchas for future similar splits:** a Python script masking
`//`/`/* */`/string/char-literal spans before brace-counting mechanically
extracted method bodies into Core vs Curly files (byte-identical) — scale
marker count to file size. An inherited static nested class must be
imported via its declaring class's canonical name, not the subclass (javac
rejects the subclass import form). Bulk `private`→`protected` fixes needed
wherever a Core method is now called from a Curly sibling. `git rm` (not
`rm` + `git add`) is needed to stage a deletion on this system's old git
version. Watch for extraction scripts dropping a `public/private static`
modifier prefix when a marker starts mid-declaration.

**Result:** every file group landed as its own checkpoint commit, `make
test` green (90/90 forward + 90/90 idempotency) after each.

**2026-07-28 cleanup-pass follow-up:** swept every `*Curly`/`*Indent`/
`*Tags` sibling for independently re-derived helpers. Found one
byte-identical case: `TokenizerCurly`/`TokenizerIndent` each defined their
own private `setOf(String...)` — promoted to `TokenizerCore` as `protected
static`, duplicates deleted. Left three other same-named `setOf` copies
alone (`DeclarationAlignmentRuleCore`, `MiscRuleCore`, `BlockStructureRule`,
`KeywordAmbiguityGate`) — unrelated hierarchies, no common ancestor short of
a bigger, riskier shared-utility-class move.

**2026-08-10 cleanup-pass follow-up:** re-swept `FormatterCurly`/
`FormatterIndent`/`FormatterTags`, `ScopePipelineCurly`/`ScopePipelineIndent`/
`ScopePipelineTags`, `MiscRuleCurly`/`MiscRuleIndent`/`MiscRuleTags`,
`IndentationDetector`, and `gdr/` for unused private methods/fields/imports
(repo-wide grep, not just single-file). Found and removed one dead method:
`MiscRuleCurly.lineEndIndex` (RDD_KEY_163 already documents its former call
site was replaced by `effectiveLineEndIndex`; the superseded method was left
behind unused). Removed its now-unused `HashSet`/`Set`/`CommentDecision`
imports (double-checked zero remaining references file-wide). Two low-
reference-count hits investigated and left alone as false positives:
`MiscRuleIndent.isCommentChainLink`/`isCommentRewritable` are legitimate
`@Override`s of `MiscRuleCore` methods called polymorphically. No other
unused private methods/constants found; no new safe duplication-
consolidation candidates found. `make test`: confirmed the single
pre-existing `test/cpp_comments_inp.cpp` failure (GRU comment-classifier
capitalization verdict differs from a prior committed run) was already
present before this session's edits (verified by reverting
`MiscRuleCurly.java` and re-running) — left uninvestigated as out of scope
for a dead-code sweep; a future AI-assist session should check
`STATE_AI.md`/classifier weight provenance for why a checked-in fixture
doesn't match a fresh build deterministically.

---

## Architectural TODOs

### Server concurrency + client read-ahead — IMPLEMENTED (2026-08-09)

Bench (2026-08-08): standalone all-at-once 2629mS vs. client-server
all-at-once 1747mS (gap ≈ one-time JVM startup a warm server skips; batch
throughput already near-optimal). One-by-one: standalone 48450mS vs.
client-server 36722mS, dominated by per-request round-trip serialized
through `HttpServer`'s single thread. Considered `server-concurrency = N`
+ `client read-ahead = M` together (server-side concurrency alone does
nothing without client pipelining) and initially deferred pending a
concurrent-multi-client use case.

**Landed:** `server-concurrency` (default `1`, `Config.java`/
`ServerMode.start` → `HttpServer.setExecutor(Executors.newFixedThreadPool(N))`
when `N > 1`) and `client-read-ahead` (default `1`, `Main.java`'s
`runFilesWithReadAhead` pipelines up to `M` concurrent `processFile` calls
via a fixed thread pool + sliding-window `Future` deque when a live server
is found and `M > 1`). Both process/server-invocation-scoped config keys
(same category as `server-port`), excluded from `JXM_CFMT_CFG`
(`InFileConfig.SERVER_SCOPED_KEYS`).

**Thread-safety audit** (required before shipping `server-concurrency > 1`):
grepped every `.java` under `src/com/jxmake/formatter/` for a non-`final`
`static` field — zero hits reachable from `ServerMode`/`FormatHandler`/
`Config`/`GdrPipelineGate`/the GRU classifier stack. Two hazards checked by
hand, confirmed safe: `IndentationDetector.detect` takes its cache as a
per-call parameter; `GruAbstainResolver.CLASSIFIER_CACHE` is a `static final
ConcurrentHashMap`. `Main.java`'s standalone-mode indent cache file
(CLI-side only) is self-healing under concurrent access. No hazard required
a code change.

**Verification:** `make test`/`make test-server` stayed 263/263 green at
shipped defaults (both keys `1`, byte-for-byte unchanged). New `make
test-server-concurrent` starts a server at `server-concurrency = 4`, fires
80 concurrent HTTP requests (2 distinct Java inputs × 40, interleaved),
diffs every response byte-for-byte against the single-threaded reference —
80/80 matched, no hang. `make bench`'s `client-server, all-at-once` scenario
updated to `server-concurrency = $(nproc)` / `client-read-ahead =
$(nproc)+2`; showed no further speedup there (expected — already one client
call, one JVM invocation, thread-pool overhead dominates at this file
count). `test-server-concurrent`'s 80-simultaneous-request run is the real
evidence the feature works; a future multi-client throughput bench should
add a fifth `make bench` scenario with several independent client processes.

Documented in `README.md`'s "Server mode" section and "Config file
format"/"In-file config overrides" sections.

**2026-08-09 follow-up (real many-clients bench):** user's own run
(`server-concurrency=3`/`client-read-ahead=5` via env vars) confirms the
feature's actual target scenario: client-server all-at-once concurrent
1143mS vs. non-concurrent 1912mS (45.85x vs. 27.42x standalone-baseline
speedup). Also noted: server-vs-standalone gap narrows session-to-session if
the server process isn't restarted between benches — JIT warm-up, not a
formatter bug.

### Multi-sentence comment capitalization — landed, off by default

**Outcome:** landed behind `normalize-comment-start-case-multiline` (default
`off`), following the `curly-general-scope-reindent`/`html5-tc-gap-level`
pattern. Wired into both `MiscRuleCore` (curly: C/C++/Java/Kotlin/JS/TS) and
`ToolingCommentNormalizer` (yaml/toml/makefile/bash/powershell). `make test`
263/263 clean both with the flag off (shipping default) and forced on.
Documented in `README.md`'s Config file format section and Known
Limitations item 6 (cross-family).

**Design:** join a comment group's lines into one combined text stream
(same grouping as `computeLineCommentGroups`/
`ToolingCommentNormalizer.normalizeChain`), find every `[.!?]\s+[a-z]`
boundary, offer each to the *exact same* per-word decision already used for
a group's first word (mechanical/linear/GRU classifier stack when
`comment-normalization-classifier` is on, `isCommentNoCapitalizeWord`
keyword-exception set when off) — no new dedicated gate, no retraining,
out-of-distribution risk explicitly accepted. Capitalized positions tracked
against the synthetic combined string via per-line offset tracking, mapped
back onto the original per-line array.

**Mechanical pre-filter** (`isEligibleSentenceBoundary` in `MiscRuleCore`,
shared with the tooling family via a `protected static` cross-call): rejects
shapes no classifier should be asked about — punctuation not directly
attached to a preceding letter/digit, a preceding word that's a single
letter or known abbreviation (`vs.`, `etc.`, `e.g.`, `i.e.`, `al.`, `cf.`,
...), a following word with an internal uppercase letter (camelCase/dotted
identifiers), and a following word immediately followed by `:` with no
trailing space (URL schemes/directive comments). Built in two passes: first
(ellipsis/symbol/abbreviation/camelCase) fixed all 4 `make test` regressions
found with the flag forced on; second (colon/single-letter-abbreviation)
followed a real-code dogfood against `/tmp/angular` (300 `.ts` files) that
found `https://...`/`ftp://...`/`tslint:...`/`e.g.`/`i.e.` self-
capitalization bugs, taking that dogfood pass from 16 differing files to 7
(6 legitimate, 1 accepted known limitation below).

**Accepted known limitation:** with `comment-normalization-classifier = on`,
the keyword-exception list is never consulted — mirrors
`capitalizeFirstLetter`'s pre-existing sentence-1 behavior (inherited, not
new). E.g. `// import './rxjs/rxjs.spec';` mid-group can be capitalized to
`// Import '...'` if the classifier judges it plausible. Confirmed by the
user as acceptable, documented in README.md rather than chased with another
mechanical rule.

**Naming:** user asked mid-task whether a different key name would be
better than `normalize-comment-start-case-multiline`; left as-is (matches
the pre-approved design), flagged in the final report, not unilaterally
renamed. Revisit only if the user says they'd prefer the rename.

<details>
<summary>Original pre-implementation plan (superseded, kept for history)</summary>

Today's comment-start capitalization only touches the first word of a
line-comment chain/group. Full multi-sentence support needed: (1) joining a
comment group's lines into one logical text stream, re-splitting after
transforming; (2) real sentence-boundary detection (genuinely ambiguous —
abbreviations, decimals, version strings, inline code fragments defeat a
naive `.`+whitespace+letter heuristic); the existing GRU classifier answers
"is this leading word safe to capitalize," not "is this a sentence
boundary" — out-of-distribution, would need retraining or its own
heuristic/gate. Blast radius wide (~15 languages, 260+ fixtures). If picked
up: land behind its own flag, off by default, validate against several
real-code dogfood corpora before flipping the default — its own tracked
job-sized effort.

</details>

### Project refactoring/cleanup pass

After `angular/angular` and `python/cpython` dogfood runs (the last cheap-ish
novel-shape corpora before the rest of each job's test-fixture-repo list
crosses ~1000 kLOC), consider a dedicated cleanup pass across jobs rather
than immediately starting the next >1000 kLOC candidate. Candidate scope:
sweep each job's "Known Gaps" for ACCEPTED-not-fixed items that may now be
cheaper to fix; check for unused boilerplate files; check for duplicated
helper logic accreted across the `*Curly`/`*Indent`/`*Tags` split now that
several jobs have landed real `*Indent`/`*Tags` logic; re-read each job's
STATE_*.md for stale/contradictory notes post-compaction; update and fix
`CLAUDE.md`, `README.md`, `../README.txt`, `../AI_PREAMBLE_FULL.md`,
`../AI_PREAMBLE_AESTHETIC.md`. Intentionally scoped as housekeeping, not a
rewrite — do not let it grow into a separate, riskier architectural job.

**2026-07-28: checked, none of the five docs needed a fix.**
**2026-08-03 (tc gap job doc cleanup):** re-checked all five now that
`html5-tc-gap-level` levels 1-4 landed. `README.md` needed a fix — its
`html5-tc-gap-level` explanation had been placed entirely under "Known
Limitations" instead of Configuration; moved to a new Configuration
subsection, trimmed "Known Limitations" to genuine accepted-gap caveats
(levels 1, 2, 4; level 3 has none) — see `README.md`'s Configuration section
and `STATE_HTML5_TCG.md`. The other four needed no change.

**2026-08-12 cleanup-pass follow-up (XL.txt TIER 0 item 1):** re-swept all
of `src/` for unused private methods/fields (per-file grep-count script,
flagging same-file reference count ≤1) and re-checked the 2026-07-28 `setOf`
consolidation. Found and removed one dead method: `JsonSpecificRule.
isSignificant` (zero call sites repo-wide). No unused fields found (the only
≤1-reference hit, `UnsupportedLanguageException.serialVersionUID`, is a
normal `Serializable` field, correctly left alone). Re-confirmed
`TokenizerCurly`/`TokenizerIndent`'s `setOf` calls correctly route through
the 2026-07-28 promotion; the three other same-named `setOf` copies remain
intentionally unconsolidated (unchanged conclusion). Checked the
`Lang.SCAFFOLD_ONLY_LANGUAGES`-guarded conditionals across `FormatterIndent.
java`/`InFileConfig.java`/`Main.java`/`ServerMode.java` — already-dead-but-
safe now the constant is `""`, but CLAUDE.md says it's "kept only for
documentation/compatibility", so left alone as intentional. Did not attempt
a new `*Curly`/`*Indent`/`*Tags` cross-family consolidation this pass (token-
rendering primitives, bracket-depth tracking) — scope not covered this
session (across json/json5/css/yaml/toml/xml/html5/js-ts/python3/tooling
`*SpecificRule` files, none flagged unused but not compared to each other
for near-identical logic); a future pass should pick this up specifically.
`make test`: 290/290 forward + idempotency, clean.

### Formatter self-formatting (dogfood-and-adopt) process

A dedicated procedure for reformatting the formatter's own Java source tree
(`src/`) with itself and adopting the result — distinct from routine dogfood
*testing* (which only checks against a temporary formatted copy, never
touches real `src/`). Run only when explicitly asked; not as a byproduct of
an unrelated task.

1. Copy `src/` to `/tmp/fmt_ref`. Round1 format, round2 format round1's
   output. Fix any bug before continuing; round1/round2 diff must be clean.
2. Build round2's Java sources into a JAR (separate from the committed
   `target/code-formatter-1.00.jar` — never overwrite real build output with
   a trial JAR). Run `make test`'s forward and idempotency fixtures against
   it. Fix any bug before continuing.
3. Use that JAR to format the *original* `/tmp/fmt_ref` copy again, producing
   round1b/round2b. round1 must be byte-identical to round1b, round2 to
   round2b — confirms a fixed point. Fix any bug before continuing.
4. Copy round1's formatted files back over the real `src/` tree. Spot-check
   a sample of changed files.
5. Rebuild from the newly-adopted `src/`, run `make test` again.
6. If clean, proceed with the normal commit workflow — don't skip it just
   because this is self-referential.

**Tools/compiler used** (formatter + GRU tools source), exact commands:

```bash
### Formatter
rm -rvf /tmp/fmt_r1 /tmp/fmt_r2
find src         -type f -print0 | xargs -0 ./code-formatter.sh --out /tmp/fmt_r1 --preserve-tree --root src
find /tmp/fmt_r1 -type f -print0 | xargs -0 ./code-formatter.sh --out /tmp/fmt_r2 --preserve-tree --root /tmp/fmt_r1
diff -ru /tmp/fmt_r1 /tmp/fmt_r2

JAVA_VERSION=8
CLASS_DIR=/tmp/classes
JAR_FILE=/tmp/output.jar
MANIFEST=/tmp/manifest.txt
MAIN_CLASS=com.jxmake.formatter.Main

mkdir -p "$CLASS_DIR"
printf 'Main-Class: %s\n' $MAIN_CLASS > $MANIFEST
find /tmp/fmt_r1 -type f -name "*.java" -print0 | xargs -0 javac -encoding UTF-8 -source "$JAVA_VERSION" -target "$JAVA_VERSION" -d "$CLASS_DIR"
jar cfm "$JAR_FILE" "$MANIFEST" -C "$CLASS_DIR" .

make _test_serial JAR_FILE=$JAR_FILE

rm -rvf /tmp/fmt_r1b /tmp/fmt_r2b
find src          -type f -print0 | xargs -0 java -jar $JAR_FILE --out /tmp/fmt_r1b --preserve-tree --root src
find /tmp/fmt_r1b -type f -print0 | xargs -0 java -jar $JAR_FILE --out /tmp/fmt_r2b --preserve-tree --root /tmp/fmt_r1b
diff -ru /tmp/fmt_r1b /tmp/fmt_r2b
diff -ru /tmp/fmt_r1  /tmp/fmt_r1b
diff -ru /tmp/fmt_r2  /tmp/fmt_r2b

make clean
cp -Rvf /tmp/fmt_r1/* src
make test
make test-server
make bench

### GRU tools (verifiers tools should also use the same method)
cd tools/gru
rm -rvf /tmp/gru_tools
mkdir /tmp/gru_tools
cp *.{java,py} /tmp/gru_tools
find /tmp/gru_tools    -type f -print0 | xargs -0 ../../code-formatter.sh --out /tmp/gru_tools_r1
find /tmp/gru_tools_r1 -type f -print0 | xargs -0 ../../code-formatter.sh --out /tmp/gru_tools_r2
diff -ru /tmp/gru_tools_r1 /tmp/gru_tools_r2

find /tmp/gru_tools    -type f -name '*.java' -print0 | xargs -0 ../verifiers/java_syntax_check.sh
find /tmp/gru_tools_r1 -type f -name '*.java' -print0 | xargs -0 ../verifiers/java_syntax_check.sh
find /tmp/gru_tools_r2 -type f -name '*.java' -print0 | xargs -0 ../verifiers/java_syntax_check.sh

find /tmp/gru_tools    -type f -name '*.py' -print0 | xargs -0 ../verifiers/python_syntax_check.sh
find /tmp/gru_tools_r1 -type f -name '*.py' -print0 | xargs -0 ../verifiers/python_syntax_check.sh
find /tmp/gru_tools_r2 -type f -name '*.py' -print0 | xargs -0 ../verifiers/python_syntax_check.sh

# Compare each stage's *.java and *.py content against the previous stage via
# ../verifiers/java_content_diff.sh / python_content_diff.sh, one file at a time
# (orig vs r1, then r1 vs r2), then adopt:
cp -vf /tmp/gru_tools_r1/*.{java,py} .
```

No syntax errors found; AST differed only in comments.
`java_content_diff.java` initially flagged **INCORRECT COMMENT
NORMALIZATION** on `tools/gru/*.java`, **resolved (2026-07-29) as a false
alarm in the checker itself** — three unaccounted-for behaviors: reflowed
Javadoc openers tripping the naive `* ` whitespace-collapse, new closing-
brace annotations (`} // while`) flagged as suspect additions,
`normalize-comment-end-period` legitimately stripping a sole trailing `.`.
Fixed in `java_content_diff.java`; re-ran clean, all 9 `tools/gru` files
zero mismatches — no classifier fix needed.

**2026-08-08: broadened to all of `tools/*` (simplified, no round2-JAR
fixed-point check).** 40 files (`.java`/`.py`/`.js`), already-built JAR.
Idempotency empty diff; content-diff clean on all 40. 8 files had an actual
diff, all trailing-period comment normalization, not a bug. Adopted; `make
test` unaffected (261/261). See `STATE_DOGFOOD.md` for the summary row.

**2026-08-08: re-ran against real `src/` (same simplification), not
adopted.** round1/round2 `diff -ru` not empty — `rules/
PowerShellSpecificRule.java`: a group-aligned trailing `//` comment after a
call `enforceCallLineBreaking` wraps onto its own line keeps stale wide
alignment padding through round1, then collapses to one space in round2 —
same pass-ordering bug family as `JavaSpecificRule.isSingleLineBody`
(comment-column width computed independently of `enforceCallLineBreaking`'s
verdict). Root-caused, not fixed (too risky to patch blind at this scope).
Round1 compiled clean, passed `make test` 261/261, 24/25 changed files
content-diffed clean. Real `src/` left untouched.

**2026-08-08 (later): re-ran after a manual source-layout workaround,
adopted.** A formatter-source fix for the trigger above (two attempts, both
reverted — see `STATE_C_CPP_JAVA.md`'s Open Questions) was abandoned as too
risky. Instead, blank lines manually inserted between each `s = applyX(s);
// comment` statement in `PowerShellSpecificRule.java`'s `format()`,
breaking `applyAssignmentsPass`'s alignment-group membership (RDD_KEY_254's
"blank line breaks the group" rule) — sidesteps the trigger without
touching formatter source. Full re-run end to end: round1/round2 diff over
all of real `src/` empty; 26 changed files content-diffed clean; adopted;
`make clean && make test` 261/261, `make test-server` passed. **Superseded
2026-08-09:** the underlying `applyAssignmentsPass`-vs-`enforceCallLineBreaking`
ordering bug is now fixed at the formatter-source level — see
`STATE_C_CPP_JAVA.md`'s Open Questions (RDD_KEY_193 fixture). The blank-line
workaround was left in place as optional cleanup, no longer structurally
required.

**2026-08-10: re-ran against `tools/*` (added `.sh`, 67 files total),
adopted.** Round1/round2 idempotency empty. 3 files differed from originals:
`tools/gru/acquire_corpus.sh` (cosmetic), `tools/verifiers/
js_ts_content_diff.js` (no new diff), `tools/verifiers/
kotlin_content_diff.java` — surfaced a real bug: the GRU comment classifier
(retrained earlier this session, see `STATE_AI.md`) capitalized a comment
starting with a real method reference, `// getName() defaults...` →
`// GetName() defaults...`, changing its meaning. Root-caused as a genuine
mechanical-gate gap: `nextCharIsOpenParen` existed as a feature but was only
consulted inside `KeywordAmbiguityGate`'s scoring (gated behind
`hasLeadingKeywordMatch`) — no equivalent of Gate 1c
(`leadingWordFollowedBySlash`) existed for the plain call-shape case. Fixed
by adding `leadingWordFollowedByParen` (`CommentFeatureVector`/
`CommentFeatureExtractor`) and a new Gate 1c-2 in `CommentClassifier.classify`
mirroring Gate 1c, guarded by `!hasLeadingKeywordMatch`. Updated
`GruAbstainResolverSelfTest.java`'s two call sites for the new constructor
param. `kotlin_content_diff.java`'s remaining mismatch (a single-statement
`if` losing its braces) is the known, intentional
`BlockStructureRule.tryCollapse` feature the AST-based checker doesn't
tolerate — a checker limitation, confirmed via javac's raw parsed-tree
`toString()`. All three adopted; `make test` 276/276 before and after.

**2026-08-10 (later, same session): re-ran against real `src/`, adopted.**
Round1/round2 idempotency empty. 18 files differed; content-diffed all 18 —
14 `OK`, 4 (`Main.java`, `MiscRuleCore.java`, `ToolingCommentNormalizer.java`,
`ScopePipelineIndent.java`) flagged "structure differs", each root-caused as
one of two already-understood non-bug classes: (a) the same brace-collapse
checker limitation noted above; (b) `src/` predated the closing-brace loop-
variable-naming feature (`// for` → `// for i`/`// for e`) and some
sentence-initial comment capitalizations outside Gate 1c-2's narrow scope
(identifiers not immediately followed by `(`). No real content loss.
Adopted; `make clean && make test` 276/276, `make test-server` passed.
