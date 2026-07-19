# STATE_COMMON.md — Shared Process Conventions

Read this file first, no matter which job (`STATE_C_CPP_JAVA.md` or
`STATE_KOTLIN.md`) you're picking up. It holds every process convention that
is identical across both jobs — commit workflow, ambiguity handling, file
exclusions, testing methodology, RDD_LOG.md lookup discipline. The per-job
file assumes all of this and does not restate it; it only contains what's
specific to that job (Project Layout, Resolved Design Decisions index,
Open Questions, Checklist).

**Do NOT read `README.md`** unless the user explicitly asks. All decisions
relevant to implementation are recorded in each job file's own **Resolved
Design Decisions** index (full text in `RDD_LOG.md` — see the lookup
convention below).

**ONLY** read the source file you are currently implementing or directly
modifying. Do NOT read other source files unless a specific checklist item
or ambiguity requires it.

---

## RDD_LOG.md lookup convention

Full decision text for every `RDD_KEY_n` lives in `RDD_LOG.md`, shared
across both jobs (Kotlin continues the same numbering sequence as
C/C++/Java — never restart it). **Do not read `RDD_LOG.md` in full.** Look
up one key at a time:

```
grep -Fm1 'RDD_KEY_n' util/CodingStyle.md/formatter/RDD_LOG.md
```

**Never add the `-A` parameter to this `grep`** — the lines in `RDD_LOG.md`
are very long, and `-A` context will flood output unnecessarily.

---

## During implementation

- Implement one checklist section at a time.
- After completing a section (or when the cumulative diff across all changed
  files exceeds ~50 lines, whichever comes first), do a checkpoint commit:
  1. Update the job's state file — check off completed items and update the
     active checklist.
  2. `git add util/CodingStyle.md/formatter/` (the entire formatter directory)
  3. `git reset util/CodingStyle.md/formatter/target/` (exclude build output)
  4. `git commit -m "<message>"` — short descriptive message, no strict
     format required, trailer ending with
     `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`.
- Small related items within a section may be grouped into one commit if they
  are trivially connected — use judgment based on line count (~50 lines
  threshold).
- Never let implemented files and the state file drift out of sync — the
  state file must always reflect the true current state at every commit.
- Never modify the files `util/CodingStyle.md/formatter/test/*_inp.*` unless
  they contain syntax errors (they are the test input files).
- Never modify the files `util/CodingStyle.md/formatter/test/*_out.*` unless
  explicitly asked (they are the reference output files that show the
  expected results).
- Ignore `XL.txt` — that is the user tracker file.
- When registering a new local test fixture pair that did **not** come from
  real-code testing (e.g. a hand-authored dogfood pair), add its entry in
  both `test/README.txt` and the `Makefile`'s `INP_FILES` **before** the
  `Real-code regressions:` section/entries — the same "ahead of
  `real_code_regressions_*`" ordering the Real-code testing methodology
  section below already uses for bug-fix fixtures.
- **New local test fixtures are authored directly in `formatter/test/`** —
  there is no staging step. (Historically, `../FUTURE_TEST_FIXTURES.md` held
  hand-drafted pairs for languages ahead of their real implementation; every
  pair it ever held has since been extracted and registered, and that file
  is now historical/empty of live drafts — don't add new ones there.) When
  authoring a fixture pair for a language with no real formatter logic yet,
  register it commented-out in the Makefile's `INP_FILES` (same pattern
  CPP26/JS/TS/HTML5/Python3 used) until real logic lands; for a language
  with real logic, verify the pair against the actual JAR before registering
  it active.
- Use `/tmp` for temporary smoke-test and mini-test files.
- NEVER perform a filesystem-wide find; search first in `/tmp/claude-1000`
  or the project root. If still not found, ask the user.
- Prefer evidence over reasoning when diagnosing a bug or checking for a
  regression. Keep static analysis minimal — only enough to identify where
  to insert debug prints. Use debug prints and `make test` to diagnose and
  validate fixes, not static analysis as the primary method. After a fix is
  verified with `make test`, remove all debug prints, then commit only the
  files you actually modified. Do not add `RDD_KEY_*` text in test fixture
  group title in `test/README.txt`. If unsure, ask.

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

Repeatable methodology for testing the formatter against real, third-party
code (preferred over synthetic dogfooding — it finds concrete, fixable bugs
faster):

1. Clone a real, compiling third-party project. First search `/tmp` for an
   existing checkout from a prior session (this candidate's name/org may
   already be present under `/tmp` or a scratchpad dir from earlier work) —
   reuse it if found. If not found, re-clone fresh. **Never perform a
   filesystem-wide search** (e.g. `find /`) to locate it — search only within
   `/tmp`/the scratchpad dir, or ask the user, per the no-filesystem-wide-find
   rule elsewhere in this file.
2. Format it once (round1).
3. Format round1's output again (round2).
4. `diff round1 round2` must be empty (idempotency).
5. Compile round1 with the appropriate toolchain — must succeed with the
   same error count as the unmodified original (no new, formatter-induced
   errors).

**DONE — `--preserve-tree` + `--root DIR` fix `--out DIR` basename-flattening
collisions.** `Main.java` gained two new CLI flags: `--preserve-tree`
(boolean) and `--root DIR` (String). When both are given alongside `--out
DIR`, each input file's output path is computed as
`outDir.resolve(rootDir.relativize(inputPath))` (`Main.processFile`'s
`OUT_DIR` case), preserving subdirectory structure instead of the previous
`Paths.get(outDir).resolve(path.getFileName())` basename-only flattening —
two input files with the same basename in different source directories no
longer collide. Validation (usage error, exit 2, via `usageError`):
`--preserve-tree` requires `--out DIR`; `--preserve-tree` requires `--root
DIR`; `--root DIR` given without `--preserve-tree` is also a usage error. An
input file that doesn't resolve under `--root DIR` is a per-file
`IOException`, surfaced the same way every other per-file processing error
already is (caught in `run()`'s per-file loop, printed, contributes to a
non-zero overall exit code). Fully opt-in and backward-compatible — omitting
`--preserve-tree` leaves the original flattening behavior byte-for-byte
unchanged. `README.md`'s Output modes section documents both new flags.
`make test` 78/78 forward + 78/78 idempotency, no regressions. Manually
smoke-tested: preserve-tree avoids collision on duplicate basenames across
subdirectories; flattening still collides as before when `--preserve-tree`
is omitted (regression check); all three new validation errors exit 2; a
file outside `--root DIR` surfaces as a per-file error.

**Invoke the formatter JAR once per batch, not once per file.** `Main.run()`
accepts any number of positional file-path arguments and formats them all in
one JVM process (each file independently resolves its own
`.jxmake-code-formatter` boundary via `Config.resolve`, so mixing files from
different directories in one invocation is safe). Looping
`code-formatter.sh <file>` per file re-pays JVM startup for every single
file, which dominates wall-clock time on a large candidate tree. Instead
collect the file list first (e.g. `find <dir> -name '*.hpp' -o -name
'*.cpp'`) and pass it to one invocation, e.g.:

```bash
find <candidate-dir> \( -name '*.hpp' -o -name '*.cpp' -o -name '*.h' \) -print0 \
  | xargs -0 <path-to>/code-formatter.sh --out /tmp/round1
```

If the file count is large enough to risk hitting the shell/`exec` argv
length limit, group by subdirectory (one invocation per top-level
subdirectory under the candidate tree) rather than falling back to one
invocation per file — `xargs` (without `-n1`) already chunks automatically if
needed, so this is mainly a concern for a manually-constructed argument list.
Same applies to round2 and to any `--diff`/`--check` verification pass.

**Run one candidate at a time, via one sub-agent — never launch multiple
real-code-testing sub-agents concurrently.** Wait for one to finish (or stop
it) before starting the next.

When an idempotency (or forward-pass) failure doesn't reproduce at the
default config, try re-testing with a `.jxmake-code-formatter` overriding
`indent-size`, `indent-style`, etc. to match the candidate's own actual
convention before concluding "no bug" — some real bugs are only observable
at a non-default `indent-size` (e.g, `indent-size = 2`).

**When a bug is found and fixed, add a new permanent fixture pair:**
`test/real_code_regressions_N_{inp,out}.<ext>` (next available `N`)
reproducing it minimally, then register it in the `Makefile`'s `INP_FILES`
and document it in `test/README.txt` — unless the bug is a no-op at the test
harness's own default config (in which case document the fix and its
non-default-config verification in the state file instead, without adding a
fixture that would be indistinguishable from a no-op at default settings).
Try to combine multiple bugs in the same text fixture if possible.

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

## Improving Server Protocol: Inline Config Support

**Status:** COMPLETE (commit `2d13ca5`). `POST /format` accepts any
`STATE_C_CPP_JAVA.md` → **Config Keys and Defaults** key as an optional
query parameter, taking priority over file-based `.jxmake-code-formatter`
config for the same keys; `path` is optional exactly when `lang` plus at
least one inline config param are present; unrecognized config-shaped
query keys get HTTP 400. Body format is unchanged (no JSON — query-string
only, decided design, do not re-litigate). Covered by the `make
test-server` Makefile target and documented in `README.md`'s Server Wire
Protocol section. No follow-up work remains; the bundled CLI exposing
inline config via its own flags is optional, not required.

---

## Config Keys and Defaults

Configurable values with their in-class defaults. All overridable via config file or CLI.

```properties
# ── Structural constants ──────────────────────────────────────────────────────
line-length                      = 100
indent-size                      = 4
indent-style                     = spaces      # spaces | tabs | auto
server-port                      = 17173

# ── Behavior ──────────────────────────────────────────────────────────────────
line-endings                     = lf          # lf | crlf | preserve
normalize-comment-start-case     = on          # on | off
normalize-comment-end-period     = on          # on | off
comment-normalization-classifier = off         # off | on
closing-comment-min-lines        = 5
format-macros                    = off         # off | on

# ── C/C++ ─────────────────────────────────────────────────────────────────────
header-guard-rename              = off         # off | on (warn only by default)

# ── Java ──────────────────────────────────────────────────────────────────────
java-import-order                = java, com, org, other, local, static
java-import-sort                 = on
java-import-depth                = 2
java-import-blank-lines          = 1

# ── Kotlin ────────────────────────────────────────────────────────────────────
kotlin-import-order              = kotlin, java, android, com, org, other, local
kotlin-import-sort               = on
kotlin-import-depth              = 2
kotlin-import-blank-lines        = 1
```

For every added, deleted, or modified configuration item,
synchronize it with `README.md` and the implementation of
*In‑file Config Support* (the `JXM_CFMT_CFG` directive,below).

---

## In-file Config Support

**DONE — In-file `JXM_CFMT_CFG` config directive.** Core mechanism (RDD_KEY_167): new
`InFileConfig.parse(source)` (raw-text regex scan, top-of-file preamble detection,
duplicate/misplaced/invalid-key hard errors as an ordinary per-file `IOException`), a new
`Config.resolve(Path, Map, Map)` overload with the in-file layer as highest priority, wired into
both `Main.formatStandalone` and `ServerMode.FormatHandler` (overrides the server's own inline
query-param config too).

Fixtures: `test/in_file_config_{inp,out}.hpp`/`.java`/`.kt` (one directive setting every
per-file-applicable key; `indent-size=2` proven via 1-space raw indentation; `.java`/`.kt` each
prove their reversed `*-import-order`) and `test/in_file_config_error_{inp,out}.hpp` (registered
but commented out in the Makefile — a hard-erroring input has no formatted result to diff, so
it's excluded from `make test`'s loop and exercised manually). Both registered in the Makefile's
`INP_FILES` and `test/README.txt`, ahead of `real_code_regressions_*`. `make test`: 75/75
forward + 75/75 idempotency, zero regressions. RDD_KEY_168 records the `.hpp` fixture's design
pivot away from `header-guard-rename` (untestable via `_inp`/`_out` diffing since the guard name
derives from the invocation path) to `format-macros=off` instead, which also proves override
precedence over the Makefile `test:` target's own `FORMAT_MACROS=on` env var.

`README.md` updated with a new "In-file config overrides" section (incl. RDD_KEY_167's
placement-semantics answer: the directive must be its own separate comment, never merged into
another comment's prose such as a copyright header; must appear "before the first
non-comment/non-blank line", not literal line 1) and the Configuration precedence list extended
to a 7th tier for this directive.

**Historical design tradeoffs** (per-key applicability list, `indent-style =
auto` carve-out, hard-error posture, placement semantics, test fixture
design) are resolved and implemented as described above. Full narrative:
`RDD_KEY_167` (core mechanism/precedence/hard-error rules) and `RDD_KEY_168`
(fixture design pivot) in `RDD_LOG.md`.

---

## Class Refactor (curly/indent/tags split) — DONE

**Purpose:** `TokenizerCore`, `Formatter`, `ScopePipeline`,
`DeclarationAlignmentRule`, `GetterSetterRule`, `MiscRule` contained only
curly-brace-family (C/C++/Java/Kotlin) logic. Before Python3/data-format/
JS-TS jobs land real logic, each was split into a slim `*Core` base plus
family siblings (`*Curly`, and skeletons for `*Indent`/`*Tags`), so each
future job gets a clean landing file instead of adding to already-large,
already-entangled classes. Mechanical rename/move only — no behavior change;
every method's existing internal branching (including Kotlin-vs-C/C++/Java
checks) moved unchanged into whichever sibling it landed in.

**Scoping:** `DeclarationAlignmentRule`/`GetterSetterRule` got
**Core+Curly(+Indent skeleton)** only, no `Tags` sibling — XML/HTML have no
declaration/getter-setter concept. `TokenizerCore`/`Formatter`/
`ScopePipeline`/`MiscRule` got the full **Core+Curly+Indent+Tags** 4-way
split. `ComplexityPaddingEvaluator.java` was not split (extend in place when
a new job needs it). `Lang.java` gained `isCurly`/`isIndentBased`/
`isTagBased` family predicates first, since everything else depends on them.
`FormatterCore.forLanguage(String)` is the static dispatcher factory (picks
`FormatterCurly`/`Indent`/`Tags` by family), so `Main.java`/`ServerMode.java`
never need their own if/else on language.

**Key plan deviations** (the original plan's literal per-method Core/Curly
assignment didn't always compile as written — when a "Core" method called a
"Curly" one, the callee moved to Core alongside its caller instead):
- `DeclarationAlignmentRule`: `renderTokens`/`renderInitTokens`/
  `needsSpaceBetween`/`isTightToken`/`isCStyleCastClose` (+
  `CONTROL_FLOW_KEYWORDS`) all kept together in Core.
- `MiscRule`: `needsSpaceBetween`/`isTightToken` moved to Core (Core's
  `renderTokens` calls `needsSpaceBetween` directly); `capitalizeFirstLetter`/
  `isCommentNoCapitalizeWord` (+ `COMMENT_NO_CAPITALIZE_C/CPP/JAVA`
  constants) moved to Core (three Core comment-formatting methods call them
  directly); `renderTokens`/`templateAngleTokens` kept in Core (also used by
  Core's `parseAssignment`); generic scan helpers (`matchParenForward`/
  `Backward`, `nextSignificantIndex`/`prevSignificantIndex`, `anyFrozen`,
  `significantOnly`/`significantWithComments`) kept in Core. Conversely,
  `splitTopLevelCommas` moved to Curly despite its generic name — used
  exclusively by Curly's Signature/call-rendering methods.
- `KotlinDeclarationAlignmentRule`/`KotlinGetterSetterRule`/
  `KotlinSignatureRule` all ended up extending the `*Curly` sibling (not
  `Core`), since they reuse protected members that only make sense together
  on the Curly side.

**Reusable technical gotchas** (apply to future similar splits):
- A Python script that masks out `//`/`/* */`/string/char-literal spans
  before brace-counting was used to mechanically extract method bodies into
  Core vs Curly files (byte-identical, not hand-retyped) — proven reliable
  across all four splits; scale to the target file's marker-list size
  (MiscRule's needed 113 markers for its 3353-line source).
- An inherited static nested class must be imported via its actual
  declaring class's canonical name, not the subclass — e.g.
  `GetterSetterRuleCore.Member`, `MiscRuleCore.Assignment`, not
  `GetterSetterRuleCurly.Member`/`MiscRuleCurly.Assignment` — javac rejects
  the subclass form ("import requires canonical name") even though plain
  code references to the subclass name work fine via inheritance.
- Bulk `private`→`protected` visibility fixes are needed wherever a Core
  method (originally private) is now called from a Curly sibling.
- `git rm` (not plain `rm` + `git add`) is required to stage a working-tree
  deletion on this system's old git version.
- Watch for extraction scripts starting a method-boundary marker
  mid-declaration and silently dropping a `public static`/`private static`
  modifier prefix — verify each extracted nested class's modifiers against
  the original.

**Result:** every file group (Lang.java, Tokenizer, Formatter, ScopePipeline,
DeclarationAlignmentRule, GetterSetterRule, MiscRule) landed as its own
checkpoint commit, `make test` green (90/90 forward + 90/90 idempotency,
zero regressions) after each. `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md`'s own
file/class references were updated to the new `*Curly` names (commit
`9cce1a5`). Full regression re-run confirmed zero behavior change. All
stale pre-refactor class-name mentions in comments/javadoc were later swept
and fixed (commit `949b7a9`).

---

## Future Cleanup TODOs

- `../FUTURE_FEATURE_DISCUSSION.md` is now safe to `git rm` — all of its
  load-bearing content has been folded inline into the `STYLE_*.md`/
  `STATE_*.md` files that referenced it (C++29-exclusion reasoning in
  `STYLE_CPP26.md`, JSX/TSX scoping rationale in `STYLE_JS_TS.md`, the
  C++26-reflection test-fixture repo list in `STYLE_CPP26.md` §5, the
  data-formats implementation-status pointer in
  `formatter/STATE_DATA_FORMATS.md`), and `grep -rn
  FUTURE_FEATURE_DISCUSSION.md STYLE_*.md formatter/STATE_*.md
  formatter/RDD_LOG.md` from `util/CodingStyle.md/` now returns zero hits
  outside this note itself. Not yet deleted — do it next time this file is
  touched, or on explicit request.

- `../FUTURE_TEST_FIXTURES.md` is now also safe to `git rm` — its draft
  content was already fully spent, and the live "staging area" dependency on
  it (this file and each job `STATE_*.md`'s "Test Fixtures (Local)" section)
  has since been rewritten to describe fixtures as authored directly in
  `formatter/test/`, no staging step. Only historical/past-tense mentions
  remain (completed `- [x]` checklist notes in `STATE_CPP26.md`,
  `STATE_JS_TS.md`, `STATE_PYTHON3.md`, `STATE_DATA_FORMATS.md`, and
  `RDD_LOG.md`'s RDD_KEY_181–186/193 entries) — these are accurate records
  of past process and should stay as-is; they don't depend on the file
  still existing. Not yet deleted — do it next time this file is touched,
  or on explicit request.

- Some `STYLE_*.md` files carry a closing pointer sentence along the lines of
  "Implementation-tracker content (config keys, test-fixture repos, local
  test fixtures) for this file lives in `formatter/STATE_*.md`, not here —
  see that file." That sentence is itself tracker metadata, not a style
  rule, so it doesn't strictly belong in a STYLE file either — it exists as
  a breadcrumb for jobs whose STYLE file used to hold that content directly
  before being split out. Low priority (one line, doesn't cause drift the
  way stale duplicated content would), but worth removing from whichever
  `STYLE_*.md` files still have it next time that job's STYLE file is
  touched for an unrelated reason — don't hunt for it standalone. Check
  `grep -l "Implementation-tracker content" STYLE_*.md` at that time rather
  than trusting any specific file list recorded here, since new jobs may add
  their own copy of the sentence later.
