# STATE_COMMON.md — Shared Process Conventions

Read this file first, no matter which job you're picking up. It holds every
process convention identical across jobs — commit workflow, ambiguity
handling, file exclusions, testing methodology, RDD_LOG.md lookup
discipline. The per-job file assumes all of this and does not restate it; it
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
stderr immediately before each file, so a hang shows exactly which file
it's stuck on (added 2026-08-04 after a switch-case-reindent fix attempt
caused an infinite loop mid-`make test` with no way to tell which fixture
was stuck — see STATE_C_CPP_JAVA.md's Tier-4-escalation entry). Plain
unconditional stderr trace, not gated behind a flag — stderr isn't diffed
by `make test`, so it can't affect pass/fail.

**Diagnosing a hung server (`--server`/`make test-server`)**:
`ServerMode.FormatHandler.handle` similarly prints
`jxmake-code-formatter: processing <path>` (or `(no path, lang=<lang>)` for
inline-content requests) to stderr right before calling
`GdrPipelineGate.applyAndFormat`, added the same day for the same reason.
Matters more than the batch case: `HttpServer` is created with no explicit
executor (`HttpServer.create(...)`'s default), so requests dispatch on a
single internal thread — one hung request blocks every subsequent request
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
```
Jobs should invoke the appropriate wrapper script instead of directly
executing `javac`, `java`, `node`, or `python3`.

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
boundary, so mixing directories in one invocation is safe). Looping
per-file re-pays JVM startup each time, dominating wall-clock time on a
large tree. Collect the file list first and pass it to one invocation:

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

## Improving Server Protocol: Inline Config Support — DONE

`POST /format` accepts any `STATE_C_CPP_JAVA.md` → **Config Keys and
Defaults** key as an optional query parameter, taking priority over
file-based `.jxmake-code-formatter` config for the same keys; `path` is
optional exactly when `lang` plus at least one inline config param are
present; unrecognized config-shaped query keys get HTTP 400. Body format is
unchanged (no JSON — query-string only, decided design, do not
re-litigate). Covered by `make test-server`, documented in `README.md`'s
Server Wire Protocol section.

**Client env-var forwarding on delegation — DONE.** In server mode, tiers 2
(`~/.config/...`)/3 (env vars) of the precedence chain were resolved by the
server process rather than the client, risking staleness for tier 3 (a
JVM's env is fixed at process start and can drift from the client's current
shell env). Fixed via `Config.clientEnvOverrides()` (public wrapper around
`collectEnvVars()`); `Main.delegateToServer` forwards its own live
`JXMAKE_CODE_FORMATTER_*` snapshot as inline query-param overrides on every
delegated request. `README.md`'s Configuration section documents this in a
"Server mode note on tiers 2/3" paragraph.

---

## Config Keys and Defaults

`README.md`'s `### Config file format` section is the authoritative, full list of every config
key with its default and allowed values (also queryable live at runtime via the server's
`/properties` endpoint, see README.md's "Server Wire Protocol" section — backed by
`Config.describeAll()`, `Config.java` itself being the runtime source of truth). Do not
hand-maintain a second full copy of that list here; it drifts. This section only holds
maintainer-facing notes not appropriate for README.md's user-facing doc:

- `comment-normalization-classifier`: flipped on 2026-07-30 after fixing the
  `KeywordAmbiguityGate` weight regression — see `STATE_AI.md`.
- `curly-general-scope-reindent` / `curly-general-scope-reindent-multipass`: see
  `STATE_CURLY_GDR.md`, `RDD_KEY_233`/`RDD_KEY_234`.
- `html5-tc-gap-level`: cumulative 0-4, levels 1-4 implemented — see `STATE_HTML5_TCG.md`.
- `gru-classifier` / `gru-weights-path`: default on since 2026-08-02 (held-out cross-validation
  confirmed `abstainThreshold=0.7` keeps the NO false-positive rate low enough to trust) — see
  `STATE_AI.md`.

For every added, deleted, or modified configuration item,
synchronize it with `README.md` and the implementation of
*In‑file Config Support* (the `JXM_CFMT_CFG` directive, below).

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
`rm` + `git add`) needed to stage a deletion on this system's old git
version. Watch for extraction scripts dropping a `public/private static`
modifier prefix when a marker starts mid-declaration — verify each
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

---

## Architectural TODOs

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
  `README.md` needed a fix — its `html5-tc-gap-level` explanation (what
  the key is, its levels, cumulative meaning) had been placed entirely
  under "Known Limitations" instead of Configuration; moved the
  config-key documentation to a new Configuration subsection (mirroring
  the GRU classifier's subsection pattern) and trimmed "Known
  Limitations" to only the genuine accepted-gap caveats for levels 1, 2,
  and 4 (level 3 has none) — see `README.md`'s Configuration section and
  `STATE_HTML5_TCG.md`'s checklist items 3, 5, 7 for the source material.
  `CLAUDE.md`, `../README.txt`, `../AI_PREAMBLE_FULL.md`, and
  `../AI_PREAMBLE_AESTHETIC.md` needed no change — none of them make any
  HTML5-tree-construction-specific completeness claim that the tc gap
  job's opt-in, off-by-default levels made stale; their general
  "HTML5 is JAR-implemented" statements remain accurate. This job's own
  "Config Keys and Defaults" block (below) and `STATE_HTML5_TCG.md`
  checklist item 2a already carried `html5-tc-gap-level` correctly —
  only `README.md`'s section placement was wrong.

This is intentionally scoped as housekeeping, not a rewrite — do not let it
grow into an attempt at any separate, dedicated, much riskier architectural
job.

### Server mode: 3rd endpoint exposing config properties — DONE

Implemented 2026-08-06. `Config.describeAll()` returns a `List<ConfigProperty>`
(`key`/`defaultValue`/`allowedValues`, the latter `null` for free-form values,
`{"on","off"}` for boolean keys, `INDENT_STYLE_CHOICES`/`LINE_ENDINGS_CHOICES`
for the two enum-like keys), zipped from `ALL_KEYS` via an explicit switch over
a fresh default `Config` instance's fields — mechanical, no new state.
`ServerMode.java` gained a `GET /properties` handler (`PropertiesHandler`,
registered in `start()` alongside `/format`/`/shutdown`) that serializes it
with a small self-contained `propertiesJson()`/`jsonString()` JSON writer (no
existing general-purpose JSON-building helper existed elsewhere in the
codebase to reuse — `FormatterJson.java` etc. are JSON-the-language
formatters, not object serializers). Verified end-to-end: started a real
server, curled `/properties`, confirmed valid JSON with all 27 keys and
correct `allowedValues` (e.g. `indent-style` → `["spaces","tabs","auto"]`).
`make test` (244/244) and `make test-server` (all existing checks) stayed
green. README.md's "Server Wire Protocol" section documents the new endpoint
alongside `/format`/`/shutdown`. This file's "Config Keys and Defaults" block
below was trimmed per the docs-only follow-up (see next paragraph).

Docs-only follow-up (done together): trimmed this file's "Config Keys and
Defaults" block so it stops duplicating README.md's `### Config file format`
section in full — now points to README.md (and to the `/properties` endpoint)
as the authoritative property list, keeping only maintainer-facing notes
(RDD_KEY/STATE_*.md cross-refs) here.

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

### GRU tools
cd tools/gru
rm -rvf /tmp/gru_tools
mkdir /tmp/gru_tools
cp *.java /tmp/gru_tools
find /tmp/gru_tools    -type f -print0 | xargs -0 ../../code-formatter.sh --out /tmp/gru_tools_r1
find /tmp/gru_tools_r1 -type f -print0 | xargs -0 ../../code-formatter.sh --out /tmp/gru_tools_r2
diff -ru /tmp/gru_tools_r1 /tmp/gru_tools_r2

JDK=/opt/openjdk-21_linux-x64_bin/jdk-21
KLIB="$HOME/xsdk/kotlin-compiler-2.4.0/kotlinc/lib"
CP="$CP:$KLIB/kotlin-compiler.jar:$KLIB/kotlin-stdlib.jar"
$JDK/bin/javac -cp $CP ../verifiers/*.java

find /tmp/gru_tools    -type f -print0 | xargs -0 $JDK/bin/java -cp ../verifiers java_syntax_check
find /tmp/gru_tools_r1 -type f -print0 | xargs -0 $JDK/bin/java -cp ../verifiers java_syntax_check
find /tmp/gru_tools_r2 -type f -print0 | xargs -0 $JDK/bin/java -cp ../verifiers java_syntax_check

for orig in /tmp/gru_tools/*.java; do \
    filename=$(basename "$orig"); \
    fmt="/tmp/gru_tools_r1/$filename"; \
    if [ -f "$fmt" ]; then \
        echo "=== Comparing: $filename ==="; \
        "$JDK/bin/java" -cp ../verifiers java_content_diff "$orig" "$fmt"; \
    fi; \
done

for orig in /tmp/gru_tools_r1/*.java; do \
    filename=$(basename "$orig"); \
    fmt="/tmp/gru_tools_r2/$filename"; \
    if [ -f "$fmt" ]; then \
        echo "=== Comparing: $filename ==="; \
        "$JDK/bin/java" -cp ../verifiers java_content_diff "$orig" "$fmt"; \
    fi; \
done

cp -vf /tmp/gru_tools_r1/*.java .
```

No syntax errors found; AST differed only in comments. `java_content_diff`
initially flagged **INCORRECT COMMENT NORMALIZATION** on `tools/gru/*.java`,
**resolved (2026-07-29) as a false alarm in `java_content_diff.java` itself**
(not a formatter bug) — three expected behaviors it didn't yet account for:
reflowed single-line Javadoc openers tripping its naive whitespace-collapse
on the doubled `*` continuation marker; new closing-brace annotations
(`} // while`) flagged as suspect additions; `normalize-comment-end-period`
(STYLE.md #15) legitimately stripping a sole trailing `.`. Fixed in
`java_content_diff.java` (strips the `* ` marker before collapsing
whitespace, strips a sole trailing `.` on both sides, exempts closing-brace
annotations from the "unexplained addition" check); re-ran clean, all 9
`tools/gru` files zero comment mismatches. No classifier fix was needed.
