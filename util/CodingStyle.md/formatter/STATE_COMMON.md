# STATE_COMMON.md — Shared Process Conventions

Read this file first, no matter which job you're picking up. It holds every
process convention identical across jobs — commit workflow, ambiguity
handling, file exclusions, testing methodology, RDD_LOG.md lookup
discipline — which the per-job file assumes and does not restate; that file
only contains what's specific to that job (Project Layout, Resolved Design
Decisions index, Open Questions, Checklist).

**Do NOT read `README.md`** unless the user explicitly asks. All decisions
relevant to implementation are recorded in each job file's own **Resolved
Design Decisions** index (full text in `RDD_LOG.md` — see the lookup
convention below).

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
across all jobs (numbering is one continuous sequence — never restart it
per job). **Do not read `RDD_LOG.md` in full.** Look up one key at a time:

```
grep -Fm1 'RDD_KEY_n' util/CodingStyle.md/formatter/RDD_LOG.md
```

**Never add the `-A` parameter to this `grep`** — the lines in `RDD_LOG.md`
are very long, and `-A` context will flood output unnecessarily.

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
- Never let implemented files and the state file drift out of sync — the
  state file must always reflect true current state at every commit.
- Never modify `test/*_inp.*` unless they contain syntax errors. Never
  modify `test/*_out.*` unless explicitly asked.
- `XL.txt` is the user's own personal tracker, not part of this job's
  process. Ignore it unless the user points at a specific item in it. Never
  reference `XL.txt` from any `STATE_*.md` file (including this note) — each
  must stand alone for a future session with no `XL.txt` context.
- When registering a new local fixture pair that did **not** come from
  real-code testing, add its entry in both `test/README.txt` and the
  `Makefile`'s `INP_FILES`, **before** the `Real-code regressions:` entries.
- Do not commit `code-formatter-1.0.0.jar`, unless the user explicitly ask.
- **New fixtures are authored directly in `formatter/test/`** — no staging
  step (`../FUTURE_TEST_FIXTURES.md` is historical/empty, don't draft new
  ones there). For a language with no real formatter logic yet, register
  commented-out in `INP_FILES` until logic lands; otherwise verify against
  the actual JAR before registering active.
- Use `/tmp` for temporary smoke-test/mini-test files. NEVER perform a
  filesystem-wide find; search `/tmp/claude-1000` or the project root
  first, else ask.
- Prefer evidence over reasoning when diagnosing a bug or regression: keep
  static analysis minimal (just enough to place debug prints), use debug
  prints + `make test` to diagnose/validate. Remove debug prints once a fix
  is verified, then commit only the files actually modified. Do not put
  `RDD_KEY_*` text in a `test/README.txt` fixture-group title. If unsure,
  ask.

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
   (next `RDD_KEY_n` number, continuing the shared sequence), add the key +
   topic to the **Resolved Design Decisions** index in the job's own state
   file, remove from **Open Questions**, unblock the checklist item, then
   continue.

## Session end

- Always leave the state file committed and up to date before ending the
  session.
- The next session resumes from the first unchecked item in the current
  checklist.

---

## Real-code testing methodology

Preferred over synthetic dogfooding — finds concrete, fixable bugs faster:

1. Clone a real, compiling third-party project — search `/tmp` first for a
   prior-session checkout and reuse if found, else re-clone. **Never a
   filesystem-wide search** (e.g. `find /`) — only `/tmp`/scratchpad, or ask.
2. Format it once (round1).
3. Format round1's output again (round2).
4. `diff round1 round2` must be empty (idempotency).
5. Compile round1 with the appropriate toolchain — must succeed with the
   same error count as the unmodified original (no new formatter-induced
   errors).

Use `tools/verifiers` to syntax check. If there are many errors, work in
batch, store the rest in the corresponding state file.

**Diagnosing a hung `make test` / batch run**: `Main.main`'s per-file loop
(`--standalone` batch mode used by `make test`, processes every fixture in
one JVM invocation) prints `jxmake-code-formatter: processing <file>` to
stderr immediately before each file, pinpointing which file a hang is stuck
on (added 2026-08-04 after a switch-case-reindent fix attempt caused an
infinite loop mid-`make test` with no way to tell which fixture was stuck —
see STATE_C_CPP_JAVA.md's Tier-4-escalation entry). Unconditional, not
gated behind a flag — stderr isn't diffed by `make test`, so it can't
affect pass/fail.

**Diagnosing a hung server (`--server`/`make test-server`)**:
`ServerMode.FormatHandler.handle` similarly prints
`jxmake-code-formatter: processing <path>` (or `(no path, lang=<lang>)` for
inline-content requests) to stderr right before calling
`GdrPipelineGate.applyAndFormat`, added the same day for the same reason —
it matters more here because `HttpServer` is created with no explicit
executor (`HttpServer.create(...)`'s default), so requests dispatch on a
single internal thread and one hung request blocks every subsequent request
to that server instance indefinitely. Verified via `make test-server`
(still passes, trace lines visible per request) and `make test` (still
243/243 — unrelated code path).

**Verifier toolchain** — needed to build/run `tools/verifiers/*` and
`tools/gru/*`; shared across every job that touches those tools. Invoke via
their wrapper scripts rather than calling `java`, `node`, or `python3`
directly — the wrappers encapsulate required toolchain paths, runtime
environment, dependency checks, and (for Java) on-demand compilation.

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
Jobs should invoke the appropriate wrapper script instead of directly
executing `javac`, `java`, `node`, or `python3`.

**Every `*_content_diff.*` tool above supports both single-pair and batch
mode** (2026-08-14, extended to all — previously only `java_content_diff.java`/
`kotlin_content_diff.java`/`js_ts_content_diff.js` had it): a single-pair
invocation takes `<original> <formatted>`; a batch invocation takes
`<original_base_dir> <formatted_base_dir> <rel_path_file_list.txt>` (one
relative path per line, resolved against both base dirs) and runs every pair
in one process invocation, avoiding a process-restart-per-file cost on a
large dogfood corpus. Batch mode prints a `[yyyy-MM-dd HH:mm:ss.SSS]
<relative path>` line before each pair (pinpoints a hang/slow file), treats a
rel-path missing from either base dir as a warning-and-skip rather than a
crash, wraps each pair's compare in a try/catch so one bad file doesn't
abort the run, and ends with a `SUMMARY: N OK, N MISMATCH/ERROR, N MISSING`
line whose counts drive the exit code. Use batch mode for any dogfood run
over more than a handful of files.

**Do NOT use `git stash`.** Back up any files you need to preserve to a
temporary location, revert your changes for testing, and restore the
backed-up files if needed — avoids leaving work hidden in a stash that gets
forgotten after context compaction. **The system Git does NOT support
`git worktree`** — do not suggest or use it either.

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
boundary, so mixing directories in one invocation is safe). Per-file looping
re-pays JVM startup each time, dominating wall-clock time on a large tree.
Collect the file list first and pass it to one invocation:

```bash
find <candidate-dir> \( -name '*.hpp' -o -name '*.cpp' -o -name '*.h' \) -print0 \
  | xargs -0 <path-to>/code-formatter.sh --out /tmp/round1
```

If the file count is large enough to risk hitting the shell/`exec` argv
length limit, group by subdirectory (one invocation per top-level
subdirectory) rather than falling back to one invocation per file — `xargs`
(without `-n1`) already chunks automatically if needed. Same applies to
round2 and to any `--diff`/`--check` verification pass.

**`indent-size` fallback rule:** when an idempotency (or forward-pass)
failure doesn't reproduce at the default config, try re-testing with a
`.jxmake-code-formatter` overriding `indent-size`, `indent-style`, etc. to
match the candidate's own actual convention before concluding "no bug" —
some real bugs are only observable at a non-default `indent-size` (e.g.
`indent-size = 2`).

**When a bug is found and fixed, add a new permanent fixture pair:**
`test/real_code_regressions_N_{inp,out}.<ext>` (next available `N`)
reproducing it minimally, then register it in the `Makefile`'s `INP_FILES`
and document it in `test/README.txt` — unless the bug is a no-op at the test
harness's own default config (in which case document the fix and its
non-default-config verification in the state file instead, without adding a
fixture indistinguishable from a no-op at default settings). Try to combine
multiple bugs in the same text fixture if possible.

Use this standard copyright header on every new test fixture file:

```
/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
```

Use in-file config in the text fixture as needed, e.g.:
```
/*% JXM_CFMT_CFG indent-size=2 */
```

---

## Server Protocol: Inline Config Support — DONE

`POST /format` accepts any config key as an optional query parameter,
taking priority over file-based `.jxmake-code-formatter` config for the
same keys; `path` is optional exactly when `lang` plus at least one inline
config param are present; unrecognized config-shaped query keys get HTTP
400. Body format is unchanged (no JSON — query-string only, decided design,
do not re-litigate). Covered by `make test-server`, documented in
`README.md`'s Server Wire Protocol section.

**Client env-var forwarding on delegation — DONE.** In server mode, tiers 2
(`~/.config/...`)/3 (env vars) of the precedence chain were resolved by the
server process rather than the client, risking staleness for tier 3 (a JVM's
env is fixed at process start and can drift from the client's current shell
env). Fixed via `Config.clientEnvOverrides()` (public wrapper around
`collectEnvVars()`); `Main.delegateToServer` forwards its own live
`JXMAKE_CODE_FORMATTER_*` snapshot as inline query-param overrides on every
delegated request. `README.md`'s Configuration section documents this in a
"Server mode note on tiers 2/3" paragraph.

**3rd endpoint exposing config properties — DONE** (2026-08-06).
`Config.describeAll()` returns a `List<ConfigProperty>`
(`group`/`key`/`defaultValue`/`allowedValues`, `null` for free-form values,
`{"on","off"}` for booleans, `INDENT_STYLE_CHOICES`/`LINE_ENDINGS_CHOICES`
for the two enum-like keys). `group` mirrors README.md's `### Config file
format` order (`Structural constants`, `Behavior`, `C/C++`, `Java`,
`Kotlin`, `JS/TS`, `HTML5`, `Python 3`, `AI-assist (GRU)`) via a `GROUPS`
ordered-map that `describeAll()` asserts covers precisely `ALL_KEYS` (throws
if they drift apart). `ServerMode.java` gained a `GET /properties` handler
(self-contained JSON writer — no existing general-purpose object-serializer
to reuse). Verified end-to-end against a real server; all groups/keys
present in README order with correct `allowedValues`. `make test` and
`make test-server` stayed green. README.md documents the new endpoint
alongside `/format`/`/shutdown`.

---

## Config Keys and Defaults

`README.md`'s `### Config file format` section is the authoritative, full
list of every config key with its default and allowed values (also
queryable live at runtime via the server's `/properties` endpoint, backed
by `Config.describeAll()`). Do not hand-maintain a second full copy of that
list here; it drifts. This section only holds maintainer-facing notes not
appropriate for README.md's user-facing doc:

- `comment-normalization-classifier`: flipped on 2026-07-30 after fixing the
  `KeywordAmbiguityGate` weight regression — see `STATE_AI.md`.
- `curly-general-scope-reindent` / `curly-general-scope-reindent-multipass`: see
  `STATE_CURLY_GDR.md`, `RDD_KEY_233`/`RDD_KEY_234`.
- `html5-tc-gap-level`: cumulative 0-4, levels 1-4 implemented — see `STATE_HTML5_TCG.md`.
- `gru-classifier` / `gru-weights-path`: default on since 2026-08-02 (held-out cross-validation
  confirmed `abstainThreshold=0.7` keeps the NO false-positive rate low enough to trust) — see
  `STATE_AI.md`.
- `python-import-sort` / `python-import-blank-lines`: wired into `Config.java`'s `ALL_KEYS`
  2026-08-06 (RDD_KEY_247) — see `STATE_PYTHON3.md`.
- `line-length-with-comment`: added 2026-08-09, default `120`. Scopes the "code + comment" width
  fits-check (wherever the formatter measures a candidate line's width *including* a trailing
  same-line `//`/`/* */` comment) separately from the code-only `line-length` limit. Wired into the
  curly-brace family only: `MiscRuleCurly.enforceCallLineBreaking`'s whole-line fits-check and
  `JavaSpecificRule.isSingleLineBody`'s fits-prediction (RDD_KEY_172 requires these two to agree),
  each gated on a trailing comment actually being present (`hasCommentBetween`) — a plain code-only
  line still uses `lineLengthLimit`. NOT wired into any other language/pipeline: not because those
  formats lack comment syntax (JSON5/YAML/TOML/XML/HTML5/CSS all have comments; only strict JSON
  doesn't), but because none of their existing fits-checks measure comment-inclusive width at all
  yet, so there's nothing to redirect without building a new fits-check from scratch — out of
  scope here. A future data-format or indent-based-language session building a real
  comment-inclusive fits-check should reuse this key rather than inventing a new one. `make test`
  after landing: 2 pre-existing fixtures (`real_code_regressions_65_out.java`,
  `real_code_regressions_124_out.hpp`) now fail because their code+comment width falls between the
  old 100-char `line-length` limit and the new 120-char default — intended effect, left for the
  user to review/update those two fixtures per this job's fixture-touching rule.

For every added, deleted, or modified configuration item, synchronize it
with `README.md` and the implementation of *In-file Config Support* (the
`JXM_CFMT_CFG` directive, below).

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
(registered but commented out in the Makefile — a hard-erroring input has
no formatted result to diff, so it's exercised manually, not via `make
test`). Both registered in the Makefile's `INP_FILES` and
`test/README.txt`, ahead of `real_code_regressions_*`. RDD_KEY_168 records
the `.hpp` fixture's design pivot away from `header-guard-rename`
(untestable via `_inp`/`_out` diffing since the guard name derives from the
invocation path) to `format-macros=off` instead, which also proves override
precedence over the Makefile `test:` target's own `FORMAT_MACROS=on` env
var.

`README.md` has an "In-file config overrides" section (placement semantics:
the directive must be its own separate comment, never merged into another
comment's prose such as a copyright header; must appear "before the first
non-comment/non-blank line", not literal line 1) and the Configuration
precedence list has a 7th tier for this directive. Full narrative:
`RDD_KEY_167` (core mechanism/precedence/hard-error rules) and `RDD_KEY_168`
(fixture design pivot) in `RDD_LOG.md`.

**`--lang` pseudo-key — DONE (2026-08-12, RDD_KEY_286).** The directive also
accepts `--lang=<language>` (e.g. `//% JXM_CFMT_CFG --lang=cpp`) as a
per-file language override, same name/values as the CLI `--lang` flag /
server `lang` query param, same highest-priority precedence as every other
directive entry (wins over CLI `--lang`/server `lang` too). Not a `Config`
key — `InFileConfig.parse` special-cases the literal key `"--lang"`,
validates it against `Lang.isRecognized` instead of `Config.isKnownKey`,
and leaves it in the returned map for the caller to `.remove("--lang")`
before passing the rest to `Config.resolve`. `Main.processFile`/
`ServerMode.FormatHandler` both reordered to read the file/request body
(needed to parse the directive) before deciding the file's language.
Two real use cases: a `.h` file that's actually C++ (see
`STATE_C_CPP_JAVA.md`'s `.h`-defaults-to-C Open Question), and a templated
source file whose extension can't be inferred at all (`.java.in`/
`.java.inc`). Fixture: `test/in_file_config_lang_{inp,out}.h`. `README.md`
updated (new subsection under "In-file config overrides", cross-reference
from the CLI `--lang` section, Configuration precedence list, and Known
Limitations curly-brace-family item 6 broadened from C++26-reflection-only
to the whole C++ pipeline).

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
rejects the subclass import form). Bulk `private`→`protected` fixes are
needed wherever a Core method is now called from a Curly sibling. `git rm`
(not `rm` + `git add`) is needed to stage a deletion on this system's old
git version. Watch for extraction scripts dropping a `public/private
static` modifier prefix when a marker starts mid-declaration — verify each
extracted nested class's modifiers against the original.

**Result:** every file group landed as its own checkpoint commit, `make
test` green (90/90 forward + 90/90 idempotency) after each.

**2026-07-28 cleanup-pass follow-up:** swept every `*Curly`/`*Indent`/
`*Tags` sibling for independently re-derived helpers. Found one
byte-identical case: `TokenizerCurly`/`TokenizerIndent` had each defined
their own private `setOf(String...)` — promoted to `TokenizerCore` as
`protected static`, duplicates deleted. Left three other same-named `setOf`
copies alone (`DeclarationAlignmentRuleCore`, `MiscRuleCore`,
`BlockStructureRule`, `KeywordAmbiguityGate`) — unrelated hierarchies, no
common ancestor short of a bigger, riskier shared-utility-class move. No
other duplicated helper found worth promoting.

**2026-08-10 cleanup-pass follow-up:** re-swept `FormatterCurly`/
`FormatterIndent`/`FormatterTags`, `ScopePipelineCurly`/`ScopePipelineIndent`/
`ScopePipelineTags`, `MiscRuleCurly`/`MiscRuleIndent`/`MiscRuleTags`,
`IndentationDetector`, and the `gdr/` package for unused private methods/
fields and unused imports (grep-verified zero call sites anywhere in
`src/`/`test/`/`tools/`, not just single-file scope). Found and removed one
genuinely dead method: `MiscRuleCurly.lineEndIndex` (a NEWLINE-scanning
line-end finder) — RDD_KEY_163 already documents its one former call site
was replaced by the depth-aware `effectiveLineEndIndex` to fix a Kotlin
round1/round2 idempotency bug, but the superseded method was left behind
unused; `JsTsSpecificRule` has its own separate same-named private method,
unaffected. Removed its now-unused `HashSet`/`Set`/`CommentDecision`
imports from the same file (the latter two only ever referenced by that
dead method's neighbors — double-checked each has zero remaining
references file-wide before removing). Two other low-reference-count hits
investigated and left alone as false positives:
`MiscRuleIndent.isCommentChainLink`/`isCommentRewritable` looked unused by
same-file grep but are legitimate `@Override`s of `MiscRuleCore` methods
that `MiscRuleCore` calls polymorphically (Python's own doc comment on the
override explains why it always returns `true`) — correctly not dead. No
unused private methods found in any other swept file this round (all had
either zero or >1 same-file references); no unused `static final`
constants found either. No new safe duplication-consolidation candidates
found beyond the 2026-07-28 `setOf` promotion. `make test`: confirmed the
single pre-existing `test/cpp_comments_inp.cpp` failure (GRU
comment-classifier capitalization verdict differs from a prior committed
run — not caused by, or related to, this session's edits) was already
present identically before this session's two edits (verified by
reverting `MiscRuleCurly.java` to its committed `HEAD` version and
re-running `make clean && make test`, same single failure); left
uninvestigated as out of scope for a dead-code sweep — a future
AI-assist-job session should check `STATE_AI.md`/classifier weight
provenance for why a checked-in fixture doesn't match a fresh build
deterministically.

---

## Architectural TODOs

### Server concurrency + client read-ahead (deliberately not started)

Bench numbers (2026-08-08): standalone all-at-once 2629mS vs. client-server
all-at-once 1747mS — the gap is basically standalone's one-time JVM startup,
which a warm server skips; batch throughput is already near-optimal and
concurrency can't improve it further (one call, nothing to parallelize). The
real gap is one-by-one: standalone 48450mS vs. client-server 36722mS —
dominated by per-request round-trip/dispatch overhead serialized through
`HttpServer`'s single internal thread (see "Diagnosing a hung server" above
re: one hung request blocking every subsequent request on that instance).

Considered adding `server concurrency = N` (executor with N threads on the
server) and `client read-ahead = M` (client keeps M requests in flight
instead of strict request/response/request). **Decided not to implement
yet**: the two changes are only useful together — server-side concurrency
alone does nothing if the client still sends strictly one-by-one, since only
one request is ever in flight for the added threads to work on. Nobody
chasing throughput uses one-by-one anyway (see "Invoke the formatter JAR
once per batch, not once per file" above); real clients that would benefit
are genuinely independent concurrent callers (e.g. multiple editor
instances or CI jobs hitting one shared server), not a single client
pipelining its own requests. Worth reconsidering if a concurrent-multi-
client scenario materializes — at that point the motivation is availability
(a single slow/hung request blocks every other client indefinitely) more
than the raw one-by-one benchmark number.

**2026-08-09: implemented.** Landed `server-concurrency` (default `1`,
`Config.java`/`ServerMode.start` -- `HttpServer.setExecutor(Executors.newFixedThreadPool(N))`
when `N > 1`, otherwise the prior implicit single-threaded executor unchanged) and
`client-read-ahead` (default `1`, `Main.java`'s file loop --
`runFilesWithReadAhead` pipelines up to `M` `processFile` calls concurrently
via a fixed thread pool + sliding-window `Future` deque when a live server
is found and `M > 1`, otherwise the original strictly-serial `runOneFile`
loop unchanged). Both process/server-invocation-scoped config keys, same
category as `server-port` -- added to `Config.ALL_KEYS`/`GROUPS`/
`describeAll()`'s switch/`fromRawMap`, excluded from `JXM_CFMT_CFG` the
same way `server-port` already was (`InFileConfig.SERVER_SCOPED_KEYS`).

**Thread-safety audit** (required before shipping `server-concurrency > 1`):
grepped every `.java` file under `src/com/jxmake/formatter/` for a
non-`final` `static` field -- zero hits reachable from
`ServerMode`/`FormatHandler`/`Config`/`GdrPipelineGate`/the GRU classifier
stack. Two hazards checked by hand and confirmed safe:
`IndentationDetector.detect(Path, Map)` takes its cache as a per-call
parameter (no cross-request sharing); `GruAbstainResolver.CLASSIFIER_CACHE`
is a `static final ConcurrentHashMap` (safe by construction). `Main.java`'s
standalone-mode indent cache file (CLI-side only, server requests never
touch it) is self-healing under concurrent access (a corrupted/partial read
falls through to delete-and-rescan). No hazard required a code change --
"already safe," not "made safe."

**Verification:** `make test`/`make test-server` stayed 263/263 green at
the shipped defaults (both keys at `1`, byte-for-byte unchanged). New
`make test-server-concurrent` Makefile target starts a server with
`server-concurrency = 4`, fires 80 concurrent HTTP requests (2 distinct Java
inputs × 40 each, interleaved) and diffs every response byte-for-byte
against the single-threaded reference output (not just "no exception") --
80/80 matched, no hang. `make bench`'s `client-server, all-at-once` scenario
updated to `server-concurrency = $(nproc)` / `client-read-ahead =
$(nproc)+2`; the other three scenarios untouched. That scenario showed no
further speedup from concurrency/read-ahead (this run: 1708mS -> 2405mS,
within machine variance of the other scenarios' own before/after drift) --
expected, since it's already one client call processing every file in one
JVM invocation with the per-file HTTP loop already fast relative to
thread-pool overhead at this file count. The feature's actual target (many
independent concurrent clients/editor instances hitting one shared server)
isn't represented by any of the four bench scenarios;
`test-server-concurrent`'s 80-simultaneous-request run is the real evidence
this feature works. A future session wanting a real multi-client throughput
bench should add a fifth scenario starting several independent client
processes concurrently.
**NOT NEEDED, 2026-08-10**: already done via another method — see the
2026-08-09 follow-up immediately below (user's own manual multi-client run).

Documented in `README.md`'s "Server mode" section (with the
`server-concurrency + 2` client-read-ahead guidance) and "Config file
format"/"In-file config overrides" sections.

**2026-08-09 follow-up:** the "no further speedup" note above was measured
with one client hitting the server serially; a real many-clients bench
(user's own run, `server-concurrency=3`/`client-read-ahead=5` via
`JXMAKE_CODE_FORMATTER_SERVER_CONCURRENCY`/`JXMAKE_CODE_FORMATTER_CLIENT_READ_AHEAD`
env vars) does show it: client-server all-at-once concurrent 1143mS vs.
non-concurrent 1912mS (45.85x vs. 27.42x standalone-baseline speedup) --
confirms the feature's actual target scenario. Also noted: server-vs-
standalone gap narrows session-to-session if the server process isn't
restarted between benches -- JIT warm-up (JVM tiered compilation), not a
formatter bug.

### Multi-sentence comment capitalization (landed, off by default)

**Outcome:** landed behind `normalize-comment-start-case-multiline` (default
`off`), following the exact `curly-general-scope-reindent`/
`html5-tc-gap-level` pattern. Wired into both `MiscRuleCore` (curly:
C/C++/Java/Kotlin/JS/TS) and `ToolingCommentNormalizer`
(yaml/toml/makefile/bash/powershell). `make test` is 263/263 clean both
with the flag off (shipping default) and forced on via
`JXMAKE_CODE_FORMATTER_NORMALIZE_COMMENT_MULTI_SENTENCE_CASE=on`.
Documented in `README.md`'s `### Config file format` section (own
subsection, "Multi-sentence comment capitalization") and as Known
Limitations item 6 under the curly-brace family (applies cross-family, not
curly-only).

**Design actually used**, matching the original plan below closely: join a
comment group's lines into one combined text stream (same grouping as
`computeLineCommentGroups`/`ToolingCommentNormalizer.normalizeChain`
already use), find every `[.!?]\s+[a-z]` boundary, offer each one to the
*exact same* per-word decision already used for a group's first word
(mechanical/linear/GRU classifier stack when
`comment-normalization-classifier` is on, `isCommentNoCapitalizeWord`
keyword-exception set when it's off) — no new dedicated gate, no
retraining, out-of-distribution risk explicitly accepted as the original
plan called for. Capitalized character positions are tracked against the
synthetic combined string via per-line offset tracking, then mapped back
onto the original per-line array (never re-splitting transformed text).

**Mechanical pre-filter added on top** (`isEligibleSentenceBoundary` in
`MiscRuleCore`, shared with the tooling family via a `protected static`
cross-call): runs before a candidate boundary is offered to the classifier,
rejecting shapes no classifier should need to be asked about — punctuation
not directly attached to a preceding letter/digit (ellipsis/multi-punct
runs, standalone symbols), a preceding word that's a single letter or a
known abbreviation (`vs.`, `etc.`, `e.g.`, `i.e.`, `al.`, `cf.`, ...), a
following word with an internal uppercase letter (camelCase/dotted
identifiers), and a following word immediately followed by `:` with no
trailing space (URL schemes/directive comments — `https:`, `ftp:`,
`tslint:`). Built in two passes: the first (ellipsis/symbol/abbreviation/
camelCase checks) fixed all 4 `make test` regressions found with the flag
forced on; the second (colon/single-letter-abbreviation checks) followed a
real-code dogfood against `/tmp/angular` (angular/angular, 300 `.ts` files)
that found `https://...`/`ftp://...`/`tslint:...`/`e.g.`/`i.e.`
self-capitalization bugs the fixture suite hadn't covered, taking that
dogfood pass from 16 differing files to 7 (6 legitimate, 1 accepted known
limitation, below).

**Accepted known limitation, not fixed further:** with
`comment-normalization-classifier = on`, the keyword-exception list
(`isCommentNoCapitalizeWord`, excludes words like `import`) is never
consulted — mirrors `capitalizeFirstLetter`'s pre-existing sentence-1
behavior exactly (classifier-on path trusts the classifier alone and skips
the keyword list), an inherited, not new, limitation. Concretely, a
mid-comment-group line of commented-out code such as
`// import './rxjs/rxjs.spec';` can be capitalized to `// Import '...'` if
the classifier judges it plausible. Confirmed by the user as an acceptable
documented risk; documented in README.md rather than chased with another
mechanical rule, to avoid deviating from "exact same rule/exception-sets as
sentence 1" per the original design.

**Naming:** the user asked mid-task whether
`normalize-comment-start-case-multiline` would be a better key name than
`normalize-comment-start-case-multiline`. Left as-is since the task's
original pre-approved design specified the exact key name; flagged back to
the user in the final report rather than unilaterally renamed. Revisit if
the user says they'd prefer the rename (not done as of this entry).

<details>
<summary>Original pre-implementation plan (superseded by the above, kept
for history)</summary>

Today's comment-start capitalization only ever touches the first word of a
line-comment chain/group (`computeLineCommentGroups`/`enforceCommentStyle`
in `MiscRuleCore` for curly; `ToolingCommentNormalizer.normalizeChain`'s
`#`-comment equivalent for yaml/toml/makefile/bash/powershell) — sentence
2+ within the same group is never capitalized after a `.`/`!`/`?`. Full
multi-sentence support would need: (1) joining a comment group's lines into
one logical text stream so a sentence can be detected even across a
line-wrap boundary, then re-splitting back onto the original line breaks
after transforming; (2) real sentence-boundary detection, which is
genuinely ambiguous — `.` followed by whitespace+letter is only a
heuristic, defeated by abbreviations (`e.g.`, `etc.`), decimals, version
strings, and inline code fragments (`obj.method()`); the existing GRU
classifier answers "is this specific leading word safe to capitalize," not
"is this a sentence boundary," so it is out-of-distribution for this job
and would need retraining or its own heuristic/gate (likely a
`KeywordAmbiguityGate`-shaped approach). Blast radius is wide — this logic
is now shared across roughly 15 languages and 260+ fixtures (curly,
xml/html5, json/json5/css, yaml/toml, tooling, python3). If picked up,
treat it like `curly-general-scope-reindent`/`html5-tc-gap-level`: land
behind its own config flag (e.g. `normalize-comment-start-case-multiline`),
off by default, and validate against several real-code dogfood corpora
before ever flipping the default — this is its own tracked job-sized
effort, not a same-session drop-in.

</details>

### Project refactoring/cleanup pass

After `angular/angular` and `python/cpython` dogfood runs (the last
remaining "cheap-ish" novel-shape corpora before the rest of each job's
test-fixture-repo list crosses ~1000 kLOC and real-code-testing cycle time
grows substantially), consider a dedicated cleanup pass across jobs rather
than immediately starting the next >1000 kLOC candidate. Candidate scope:

- Sweep each job's "Known Gaps" sections for ACCEPTED-not-fixed items that
  may now be cheaper to actually fix given everything learned since — do
  not treat "accepted" as permanent without re-checking.
- Check for unused files containing only boilerplate, never used.
- Check for duplicated helper logic that accreted independently across the
  `*Curly`/`*Indent`/`*Tags` class split now that several jobs (Python3,
  JS/TS, data formats) have each landed real logic in their own
  `*Indent`/`*Tags` classes — some bespoke per-job code may now warrant
  promotion to a shared helper.
- Re-read each job's STATE_*.md for stale/contradictory notes now that
  compaction passes have happened — verify compacted prose didn't silently
  drop a still-relevant caveat.
- Update and fix `CLAUDE.md`, `README.md`, `../README.txt`,
  `../AI_PREAMBLE_FULL.md`, and `../AI_PREAMBLE_AESTHETIC.md`.
  **2026-07-28: checked, none needed a fix** — all five already track
  actual shipped code state.
  **2026-08-03 (tc gap job doc cleanup, post-completion): re-checked all
  five for staleness now that `html5-tc-gap-level` levels 1-4 landed.**
  `README.md` needed a fix — its `html5-tc-gap-level` explanation (key,
  levels, cumulative meaning) had been placed entirely under "Known
  Limitations" instead of Configuration; moved the config-key
  documentation to a new Configuration subsection (mirroring the GRU
  classifier's subsection pattern) and trimmed "Known Limitations" to only
  the genuine accepted-gap caveats for levels 1, 2, and 4 (level 3 has
  none) — see `README.md`'s Configuration section and `STATE_HTML5_TCG.md`
  for source material. `CLAUDE.md`, `../README.txt`,
  `../AI_PREAMBLE_FULL.md`, and `../AI_PREAMBLE_AESTHETIC.md` needed no
  change — none make an HTML5-tree-construction-specific completeness
  claim that the tc gap job's opt-in, off-by-default levels made stale;
  their general "HTML5 is JAR-implemented" statements remain accurate.

This is intentionally scoped as housekeeping, not a rewrite — do not let it
grow into an attempt at any separate, dedicated, much riskier architectural
job.

**2026-08-12 cleanup-pass follow-up (XL.txt TIER 0 item 1):** re-swept all of
`src/` for unused private methods/fields (per-file grep-count script over
every `private (static)? ... name(` and `private ... name;` declaration,
flagging any with a same-file reference count ≤1, i.e. only the declaration
itself) and re-checked the already-promoted `setOf` consolidation from the
2026-07-28 pass. Found and removed one genuinely dead method:
`JsonSpecificRule.isSignificant` (a WHITESPACE/NEWLINE/COMMENT significance
check with zero call sites anywhere in `src/`, `test/`, or `tools/` —
verified by repo-wide grep, not just same-file). No unused fields found
(the only ≤1-reference hit, `UnsupportedLanguageException.serialVersionUID`,
is a normal `Serializable` field referenced implicitly by the JVM, correctly
left alone). Re-confirmed `TokenizerCurly`/`TokenizerIndent`'s `setOf` calls
correctly route through the `TokenizerCore.setOf` promotion from
2026-07-28 (no re-duplication crept back in); the three other same-named
`setOf` copies (`DeclarationAlignmentRuleCore`, `MiscRuleCore`,
`BlockStructureRule`, `KeywordAmbiguityGate`) remain intentionally
unconsolidated — unrelated hierarchies, no common ancestor short of a
bigger, riskier shared-utility-class move (unchanged conclusion from prior
passes). Checked the `Lang.SCAFFOLD_ONLY_LANGUAGES`-guarded conditionals in
`FormatterIndent.java`/`InFileConfig.java`/`Main.java`/`ServerMode.java`
(all `SCAFFOLD_ONLY_LANGUAGES.isEmpty() ? "" : ...` ternaries) — these are
already-dead-but-safe now that the constant is `""`, but CLAUDE.md
explicitly says the constant is "kept only for documentation/compatibility",
so the guarded call sites were left alone as intentional, not swept as
cruft. Did not attempt any new `*Curly`/`*Indent`/`*Tags` cross-family
consolidation this pass (e.g. token-rendering primitives, bracket-depth
tracking) — a full survey of `rules/`, `tokenizer/`, and the data-format/
JS-TS/Python-specific rule files for that class of duplication was not
completed this session; a future pass should pick that up specifically
(scope not covered here: `*SpecificRule` files across json/json5/css/yaml/
toml/xml/html5/js-ts/python3/tooling — none flagged as unused, but not
compared to each other for near-identical logic). `make test`: 290/290
forward + idempotency, clean (no pre-existing failures observed this run).

### Formatter self-formatting (dogfood-and-adopt) process

A dedicated procedure for actually reformatting the formatter's own Java
source tree (`src/`) with itself and adopting the result — distinct from
the routine dogfood *testing* described elsewhere in this file (which only
checks compile-cleanliness/idempotency/declaration-count against a
temporary formatted copy and never touches the real `src/` tree). Run this
only when explicitly asked; do not run it as a byproduct of an unrelated
task.

1. Copy the formatter's own Java source files (`src/`) to `/tmp/fmt_ref`.
   Apply a round1 format (fresh) and a round2 format (format round1's
   output again) to that copy. Fix any bug before continuing; do not
   proceed past a failing round1/round2 diff.
2. Once round1/round2 is clean, build the round2 Java source files into a
   JAR (separate from the currently-committed
   `target/code-formatter-1.00.jar` — do not overwrite real build output
   with this trial JAR). Use that JAR to run `make test`'s forward and
   idempotency fixtures. Fix any bug before continuing.
3. Use that same round2 JAR to format the *original* (unformatted)
   `/tmp/fmt_ref` copy again, producing round1b and round2b. round1 must be
   byte-identical to round1b, and round2 to round2b — confirms a fixed
   point, not just idempotent output. Fix any bug before continuing.
4. Copy the formatted source files from round1 back over the formatter's
   real `src/` tree, overwriting the committed source. Spot-check a sample
   of changed files for correctness.
5. Rebuild from this newly-adopted `src/` and run `make test` again
   against the real build.
6. If clean, proceed with the normal commit workflow (see above) — do not
   skip it just because this is a self-referential change.

**Tools/compiler used** and exact commands (formatter + GRU tools source):

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

for orig in /tmp/gru_tools/*.java; do \
    filename=$(basename "$orig"); \
    fmt="/tmp/gru_tools_r1/$filename"; \
    if [ -f "$fmt" ]; then \
        echo "=== Comparing: $filename ==="; \
        ../verifiers/java_content_diff.sh "$orig" "$fmt"; \
    fi; \
done
for orig in /tmp/gru_tools_r1/*.java; do \
    filename=$(basename "$orig"); \
    fmt="/tmp/gru_tools_r2/$filename"; \
    if [ -f "$fmt" ]; then \
        echo "=== Comparing: $filename ==="; \
        ../verifiers/java_content_diff.sh "$orig" "$fmt"; \
    fi; \
done

for orig in /tmp/gru_tools/*.py; do \
    filename=$(basename "$orig"); \
    fmt="/tmp/gru_tools_r1/$filename"; \
    if [ -f "$fmt" ]; then \
        echo "=== Comparing: $filename ==="; \
        ../verifiers/python_content_diff.sh "$orig" "$fmt"; \
    fi; \
done
for orig in /tmp/gru_tools_r1/*.py; do \
    filename=$(basename "$orig"); \
    fmt="/tmp/gru_tools_r2/$filename"; \
    if [ -f "$fmt" ]; then \
        echo "=== Comparing: $filename ==="; \
        ../verifiers/python_content_diff.sh java_content_diff "$orig" "$fmt"; \
    fi; \
done

cp -vf /tmp/gru_tools_r1/*.{java,py} .
```

No syntax errors found; AST differed only in comments. `java_content_diff`
initially flagged **INCORRECT COMMENT NORMALIZATION** on `tools/gru/*.java`,
**resolved (2026-07-29) as a false alarm in `java_content_diff.java` itself**
(not a formatter bug) — three expected behaviors it hadn't accounted for
yet: reflowed single-line Javadoc openers tripping its naive
whitespace-collapse on the doubled `*` continuation marker; new
closing-brace annotations (`} // while`) flagged as suspect additions;
`normalize-comment-end-period` (STYLE.md #15) legitimately stripping a sole
trailing `.`. Fixed in `java_content_diff.java` (strips the `* ` marker
before collapsing whitespace, strips a sole trailing `.` on both sides,
exempts closing-brace annotations from the "unexplained addition" check);
re-ran clean, all 9 `tools/gru` files zero comment mismatches — no
classifier fix needed.

**2026-08-08: broadened to all of `tools/*` (simplified process, no
round2-JAR fixed-point check).** Widened from the `tools/gru`-only example
above to every `.java`/`.py`/`.js` file under `tools/*` (40 files), using
the already-built JAR rather than a fresh trial build. Idempotency empty
diff; content-diff clean on all 40 files. 8 files had an actual diff, all
trailing-period comment normalization (STYLE.md #15), not a bug. Adopted
over real `tools/*`; `make test` unaffected (261/261). See
`STATE_DOGFOOD.md` for the summary row.

**2026-08-08: re-ran against the formatter's own `src/` (same
simplification), result not adopted.** `diff -ru` between a fresh round1
and round2 format of real `src/` was not empty — `rules/PowerShellSpecificRule.java`:
a group-aligned trailing `//` comment after a call that
`enforceCallLineBreaking` wraps onto its own line keeps stale wide
alignment padding through round1, then collapses to one space in round2 —
same pass-ordering bug family as `JavaSpecificRule.isSingleLineBody`
(comment-column width computed independently of `enforceCallLineBreaking`'s
own verdict). Root-caused but not fixed this session (too risky to patch
blind at this scope). Round1 compiled clean, passed `make test` 261/261,
and 24/25 changed files content-diffed clean (legitimate cosmetic changes).
Real `src/` left untouched.

**2026-08-08 (later): re-ran after a manual source-layout workaround, this
time adopted.** A formatter-source fix for the `PowerShellSpecificRule.java`
trigger (two attempts, both reverted — see `STATE_C_CPP_JAVA.md`'s Open
Questions entry) was abandoned as too risky. Instead, blank lines were
manually inserted between each `s = applyX(s); // comment` statement in
that file's `format()` method, breaking `applyAssignmentsPass`'s
alignment-group membership (RDD_KEY_254's "blank line breaks the group"
rule) — sidesteps the trigger without touching formatter source. Verified
in isolation (empty diff), then the full `src/` dogfood-and-adopt re-run
end to end: round1/round2 diff over all of real `src/` empty; 26 changed
files content-diffed clean (same cosmetic classes as before, no real
content loss); adopted; `make clean && make test` 261/261 forward +
idempotency; `make test-server` passed. The underlying
`applyAssignmentsPass`-vs-`enforceCallLineBreaking` ordering bug for the
non-JS/TS curly languages was, at the time, still open at the
formatter-source level — this adoption only removed the one known trigger
instance from `src/` via a source-layout change. **Superseded 2026-08-09:**
the underlying bug is now fixed at the formatter-source level — see
`STATE_C_CPP_JAVA.md`'s Open Questions entry (`applyAssignmentsPass` narrow
re-run, RDD_KEY_193 fixture). The blank-line workaround in
`PowerShellSpecificRule.java` is no longer structurally required but was
left in place as optional future cleanup.

**2026-08-10: re-ran against `tools/*` (added `.sh` to scope; 67 files
total), adopted.** Round1/round2 idempotency diff empty. `diff -rq` against
originals found 3 files with actual changes: `tools/gru/acquire_corpus.sh`
(cosmetic case-block reindent + a benign comment capitalization),
`tools/verifiers/js_ts_content_diff.js` (byte-identical to its already-
adopted state, no new diff), `tools/verifiers/kotlin_content_diff.java`.
The last surfaced a real, non-cosmetic bug: the GRU comment classifier
(retrained earlier the same session — see `STATE_AI.md`) capitalized a
comment starting with a real method reference, `// getName() defaults...`
-> `// GetName() defaults...`, changing its meaning (implies a
differently-cased method). Root-caused as a genuine mechanical-gate gap,
not a training/weights problem: `nextCharIsOpenParen` already existed as a
feature but was only ever consulted inside `KeywordAmbiguityGate`'s scoring
(gated behind `hasLeadingKeywordMatch`, only for language-keyword leading
words) — no equivalent of the existing Gate 1c
(`leadingWordFollowedBySlash`) existed for the plain call-shape case. Fixed
by adding `leadingWordFollowedByParen` (`CommentFeatureVector`/
`CommentFeatureExtractor`) and a new Gate 1c-2 in
`CommentClassifier.classify` mirroring Gate 1c exactly, guarded by
`!hasLeadingKeywordMatch` so it can't preempt the already-tuned
keyword-ambiguity scoring path. Updated `GruAbstainResolverSelfTest.java`'s
two `CommentFeatureVector` call sites for the new constructor param.
`kotlin_content_diff.java`'s remaining reported content-diff mismatch (a
single-statement `if` body losing its braces) is a known, intentional,
documented Java brace-collapse feature (`BlockStructureRule.tryCollapse`,
see `STATE_C_CPP_JAVA.md`) that `java_content_diff.java`'s AST-based
declaration comparator doesn't tolerate — a checker limitation, not a
formatter bug; confirmed by comparing javac's raw parsed-tree `toString()`
output directly. All three files adopted over real `tools/*` after the
mechanical-gate fix landed; `make test` 276/276 forward + idempotency, both
before and after.

**2026-08-10 (later, same session): re-ran against the formatter's own
`src/` (same simplification, already-built JAR, no round2-JAR fixed-point
check), adopted.** Round1/round2 idempotency diff empty. 18 files differed
from real `src/`; content-diffed all 18. 14 were `OK: content preserved`.
4 (`Main.java`, `MiscRuleCore.java`, `ToolingCommentNormalizer.java`,
`ScopePipelineIndent.java`) reported a "top-level declaration structure
differs" mismatch, each root-caused individually as one of two already-
understood, non-bug classes: (a) `java_content_diff.java`'s AST-based
declaration comparator doesn't tolerate the documented single-statement
brace-collapse feature (`BlockStructureRule.tryCollapse`), the same
limitation noted for `kotlin_content_diff.java` above; (b) `src/` simply
predated the existing closing-brace loop-variable-naming feature (`// for`
-> `// for i`/`// for e`, see `BlockStructureRule`'s loop-variable-name
logic) and a few ordinary sentence-initial comment capitalizations on
identifiers not immediately followed by `(` (e.g. `advancePastDottedName
returns...` -> `AdvancePastDottedName returns...`) -- correctly outside the
new Gate 1c-2's narrow scope (only slash/paren-adjacent/keyword shapes are
exempted from capitalization, not arbitrary identifiers, per this
classifier's asymmetric-risk design). No real content loss found in any of
the 18. Adopted over real `src/`; `make clean && make test` 276/276 forward
+ idempotency, `make test-server` all passed.
