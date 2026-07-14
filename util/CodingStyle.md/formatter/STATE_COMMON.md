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
- Use `/tmp` for temporary smoke-test and mini-test files.
- NEVER perform a filesystem-wide find; search first in `/tmp/claude-1000`
  or the project root. If still not found, ask the user.
- Prefer evidence over reasoning when diagnosing a bug or checking for a
  regression. Keep static analysis minimal — only enough to identify where
  to insert debug prints. Use debug prints and `make test` to diagnose and
  validate fixes, not static analysis as the primary method. After a fix is
  verified with `make test`, remove all debug prints, then commit only the
  files you actually modified. If unsure, ask.

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
at a non-default `indent-size`.

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

**DONE — In-file `JXM_CFMT_CFG` config directive.** Core mechanism landed
(RDD_KEY_167): new `InFileConfig.parse(source)` (raw-text regex scan, top-of-file
preamble detection, duplicate/misplaced/invalid-key hard errors, all wired as an
ordinary per-file `IOException`), a new `Config.resolve(Path, Map, Map)` overload
taking the in-file layer as highest priority, wired into both `Main.formatStandalone`
and `ServerMode.FormatHandler` (so it also overrides the server's own inline
query-param config).

Test fixtures landed (RDD_KEY_168 records the `.hpp` fixture's design pivot away from
`header-guard-rename`, which is untestable via the `_inp`/`_out` diff convention since
the guard name derives from the invocation path, to `format-macros=off` instead, which
also proves override precedence over the Makefile `test:` target's own
`FORMAT_MACROS=on` env var): `test/in_file_config_{inp,out}.hpp`/`.java`/`.kt` (one
directive setting every per-file-applicable key, `indent-size=2` proven via 1-space raw
indentation, `.java`/`.kt` each proving their reversed `*-import-order`) and
`test/in_file_config_error_{inp,out}.hpp` (registered but commented out in the
Makefile — a hard-erroring input has no formatted result to diff, so it's excluded from
`make test`'s loop and exercised manually instead). Both blocks registered in the
Makefile's `INP_FILES` and in `test/README.txt`, ahead of the `real_code_regressions_*`
block. `make test`: 75/75 forward + 75/75 idempotency, zero regressions.

`README.md` updated with a new "In-file config overrides" section (including
RDD_KEY_167's placement-semantics answer: the directive must be its own separate
comment, never merged into another comment's prose such as a copyright header; must
appear "before the first non-comment/non-blank line", not literal line 1) and the
Configuration precedence list extended to a 7th tier for this directive.

**Original design note below is now historical** — it discusses design tradeoffs
already resolved above (kept for the remaining implementation detail it still
documents: per-key applicability list, `indent-style = auto` carve-out, hard-error
posture). A top-of-file
comment directive, e.g. `//% JXM_CFMT_CFG line-length=100;indent-size=2` or,
using a block comment, `/*% JXM_CFMT_CFG line-length=100;indent-size=2 */`,
letting a single source file override any **Config Keys and Defaults** key
(above) for itself only, same spirit as the existing
`JXM_CFMT_DIS`/`JXM_CFMT_ENA` markers (`TokenizerCore.markFrozenSpans`) but
detected via an earlier raw-text scan (before `Config.resolve`/`Formatter`
construction, since config values are baked into rule objects before
tokenizing even starts — the DIS/ENA post-tokenization detection point is
too late to reuse directly). Note the marker syntax above (`//`, `/* */`) is
specific to the languages currently supported (C/C++/Java/Kotlin); future
languages on the **On the horizon** roadmap will need their own comment
syntax for the same directive (e.g. `#` for a hash-comment language), and at
least one planned target (JSON, strictly) has no comment syntax at all —
that language simply won't be able to carry this directive and must fall
back to file-based/CLI/env config only. Design notes for whoever picks this
up:

- Almost every config key is a simple additive map-layer override at the
  existing `Config.fromRawMap`/`Main.formatStandalone` choke point —
  `server-port` is the one key that's NOT applicable (a `--server` process
  property, not a per-file one); `indent-style = auto` specifically can't be
  resolved this way either (its whole point is scanning sibling files in the
  directory via `IndentationDetector`) — forcing a concrete `spaces`/`tabs`
  value via the directive is still fine, only the `auto` inference itself
  doesn't apply per-file.
- `server-port` (or any other key that isn't a valid, per-file-applicable
  **Config Keys and Defaults** entry) appearing inside `JXM_CFMT_CFG` is a
  hard error, same bucket as the duplicate-directive and
  not-at-top-of-file cases below — one consistent rule: anything wrong with
  the directive fails loudly, never a silent partial-apply or ignore.
- **`JXM_CFMT_CFG`, when present, must override every other config source for
  that file — file-based `.jxmake-code-formatter` (any directory level),
  env vars, CLI flags, AND the server's own inline query-param config
  (the "Improving Server Protocol" feature directly above this TODO) for the
  same request.** It is the highest-priority layer, full stop — do not make
  it lose to CLI/query-param overrides the way `.jxmake-code-formatter` does.
- **Multiple `JXM_CFMT_CFG` occurrences anywhere in the same file: hard
  error, not a warn-and-ignore.** Deliberately diverges from DIS/ENA's silent
  idempotent-toggle precedent — unlike a repeated DIS/ENA toggle, a second
  conflicting CFG line would silently change formatting output depending on
  which one "wins," which is a real footgun in a merge/copy-paste scenario;
  there's no legitimate reason to want two, so fail loudly instead of
  guessing. Same treatment for a `JXM_CFMT_CFG` found anywhere past the
  file's leading comment/blank-line preamble (i.e. not honored as a top-of-
  file directive) — hard error telling the user to move it, not a silent
  partial-apply or ignore. Wire both as an ordinary per-file `IOException`
  through `Main.run`'s existing per-file `catch`/stderr-report/exit-1 path —
  no new error-handling machinery needed.
- Decide and document exact top-of-file placement semantics before
  implementing: literal line 1, or "before the first non-comment/non-blank
  token" (so a copyright header above it doesn't count as a violation) — lean
  toward the latter to match how other header-comment-aware passes already
  work in this formatter, but confirm with the user first if genuinely
  ambiguous, per this file's usual ambiguity-handling protocol.

**Required alongside implementation — new test fixtures covering this
directive.** These are deliberately minimal, synthetic fixtures (a bare-bones
class/header, not real-code samples) — their only job is to prove the
`JXM_CFMT_CFG`-set values are actually read and honored, not to exercise
language features (that's already covered elsewhere):
- `test/in_file_config_{inp,out}.hpp`, `.java`, `.kt` — one fixture per
  language (C and C++ share the single `.hpp` fixture). Each input's
  top-of-file directive sets every **Config Keys and Defaults** key that is
  valid to set per-file (i.e. all of them except `server-port`, which is not
  per-file-applicable — see hard-error note above), with `indent-size`
  overridden to `2`. Additionally: the `.hpp` fixture turns on
  `header-guard-rename`; the `.java` and `.kt` fixtures set their respective
  `*-import-order` to the reverse of the default list. Each input file's
  body must actually contain something the corresponding overridden key
  would visibly change — i.e. a header guard whose name the rename would
  alter, and imports whose relative order would change under the
  reversed-order key — so the expected output only matches if the directive
  was truly applied. Same fixture files double as the server-mode proof
  (posted to `/format` and asserted the same way); server-mode exercise is
  not driven through the Makefile, so no separate server-mode fixture is
  needed.
- `test/in_file_config_error_{inp,out}.hpp` — proves the hard-error path
  (e.g. a duplicate `JXM_CFMT_CFG` occurrence, or one placed past the
  top-of-file preamble). Registered in the Makefile but **commented out by
  default**, since a deliberately-erroring fixture would print an error and
  make a normal `make test` run look broken; uncomment to exercise it
  manually.
- Register the `in_file_config_*` pairs in the `Makefile`'s `INP_FILES`
  list, inserted **before** the existing `real_code_regressions_*` entries
  (own clearly-grouped block ahead of the general regression-fixture block,
  not interleaved into it).
- Document the same pairs in `test/README.txt`, inserted **before** that
  file's existing `Real-code regressions:` section header (own clearly-
  grouped block ahead of it, matching the Makefile ordering above).
- Update `README.md` after this feature is fully implemented and tested.
