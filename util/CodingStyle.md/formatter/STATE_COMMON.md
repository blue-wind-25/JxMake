# STATE_COMMON.md — Shared Process Conventions

Read this file first, no matter which job (`STATE_C_CPP_JAVA.md` or
`STATE_KOTLIN.md`) you're picking up. It holds every process convention
identical across both jobs — commit workflow, ambiguity handling, file
exclusions, testing methodology, RDD_LOG.md lookup discipline. The per-job
file assumes all of this and does not restate it; it only contains what's
specific to that job (Project Layout, Resolved Design Decisions index, Open
Questions, Checklist).

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
- Small related items within a section may be grouped into one commit if
  trivially connected — use judgment based on the ~50-line threshold.
- Never let implemented files and the state file drift out of sync — the
  state file must always reflect true current state at every commit.
- Never modify `util/CodingStyle.md/formatter/test/*_inp.*` unless they
  contain syntax errors (they are the test input files).
- Never modify `util/CodingStyle.md/formatter/test/*_out.*` unless
  explicitly asked (they are the reference output files).
- Ignore `XL.txt` — that is the user tracker file.
- When registering a new local test fixture pair that did **not** come from
  real-code testing (e.g. a hand-authored dogfood pair), add its entry in
  both `test/README.txt` and the `Makefile`'s `INP_FILES` **before** the
  `Real-code regressions:` section/entries — the same "ahead of
  `real_code_regressions_*`" ordering the Real-code testing methodology
  section below uses for bug-fix fixtures.
- **New local test fixtures are authored directly in `formatter/test/`** —
  there is no staging step. (Historically, `../FUTURE_TEST_FIXTURES.md` held
  hand-drafted pairs for languages ahead of their real implementation; every
  pair it ever held has since been extracted and registered, and that file
  is now historical/empty — don't add new drafts there.) When authoring a
  fixture pair for a language with no real formatter logic yet, register it
  commented-out in the Makefile's `INP_FILES` (same pattern CPP26/JS/TS/
  HTML5/Python3 used) until real logic lands; for a language with real
  logic, verify the pair against the actual JAR before registering it
  active.
- Use `/tmp` for temporary smoke-test and mini-test files.
- NEVER perform a filesystem-wide find; search first in `/tmp/claude-1000`
  or the project root. If still not found, ask the user.
- Prefer evidence over reasoning when diagnosing a bug or checking for a
  regression. Keep static analysis minimal — only enough to identify where
  to insert debug prints. Use debug prints and `make test` to diagnose and
  validate fixes, not static analysis as the primary method. After a fix is
  verified with `make test`, remove all debug prints, then commit only the
  files you actually modified. Do not add `RDD_KEY_*` text in a test fixture
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
   filesystem-wide search** (e.g. `find /`) to locate it — search only
   within `/tmp`/the scratchpad dir, or ask the user, per the
   no-filesystem-wide-find rule elsewhere in this file.
2. Format it once (round1).
3. Format round1's output again (round2).
4. `diff round1 round2` must be empty (idempotency).
5. Compile round1 with the appropriate toolchain — must succeed with the
   same error count as the unmodified original (no new, formatter-induced
   errors).

Use `tools/verifiers` to syntax check.

If there are many errors, work in batch, store the rest in the corresponding
state file.

**Do NOT use `git stash`.** Back up any files you need to preserve to a
temporary location, revert your changes for testing, and restore the
backed-up files if needed. This avoids leaving work hidden in a stash that
may be forgotten after context compaction or an interrupted session (a prior
session left several dangling stash commits this way — cleaned up via `git
fsck --unreachable` + `git gc --prune=now` once confirmed superseded by
already-committed work). **The system Git does NOT support worktree** — do
not suggest or use `git worktree` as an alternative either.

**DONE — `--preserve-tree` + `--root DIR` fix `--out DIR` basename-flattening
collisions.** `Main.java`'s two new CLI flags: `--preserve-tree` (boolean),
`--root DIR` (String). With both given alongside `--out DIR`, output path =
`outDir.resolve(rootDir.relativize(inputPath))` (`Main.processFile`'s
`OUT_DIR` case), preserving subdirectory structure instead of the previous
basename-only flattening. Validation (exit 2 via `usageError`):
`--preserve-tree` requires both `--out DIR` and `--root DIR`; `--root DIR`
without `--preserve-tree` is also a usage error. A file not under `--root
DIR` is a per-file `IOException` (same handling as any other per-file
error). Fully opt-in/backward-compatible. `make test` 78/78 forward + 78/78
idempotency.

**Invoke the formatter JAR once per batch, not once per file.** `Main.run()`
accepts any number of positional file-path arguments in one JVM process
(each file independently resolves its own `.jxmake-code-formatter` boundary,
so mixing directories in one invocation is safe). Looping per-file re-pays
JVM startup each time, dominating wall-clock time on a large tree. Collect
the file list first and pass it to one invocation, e.g.:

```bash
find <candidate-dir> \( -name '*.hpp' -o -name '*.cpp' -o -name '*.h' \) -print0 \
  | xargs -0 <path-to>/code-formatter.sh --out /tmp/round1
```

If the file count is large enough to risk hitting the shell/`exec` argv
length limit, group by subdirectory (one invocation per top-level
subdirectory under the candidate tree) rather than falling back to one
invocation per file — `xargs` (without `-n1`) already chunks automatically
if needed, so this is mainly a concern for a manually-constructed argument
list. Same applies to round2 and to any `--diff`/`--check` verification
pass.

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

**Follow-up — client env-var forwarding on delegation (DONE).** Design question: in server mode,
tiers 2 (`~/.config/...`)/3 (env vars) of the precedence chain are resolved by the *server*
process, not the client — inconsistent since the client holds the source-of-truth environment.
Tier 2 is fine (client/server always run as the same user on the same `localhost`-only machine);
tier 3 is a real staleness risk for a long-running server, since a JVM's env is fixed at process
start and can drift from the client's current shell env. Fixed by adding
`Config.clientEnvOverrides()` (public wrapper around the existing private `collectEnvVars()`) and
having `Main.delegateToServer` forward its own live `JXMAKE_CODE_FORMATTER_*` snapshot as inline
query-param overrides on every delegated request — verified via manual smoke test
(`JXMAKE_CODE_FORMATTER_LINE_LENGTH` override changes a delegated request's wrap decision
identically to a standalone run). `README.md`'s Configuration section gained a "Server mode note
on tiers 2/3" paragraph. `make test` 169/169 forward + 169/169 idempotency, `make test-server` all
PASS, zero regressions (no config-key or wire-protocol shape change, just a new client-side
sender).

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
comment-normalization-classifier = off         # off | on (tried on 2026-07-29, regressed 9 fixtures vs deterministic keyword list, see STATE_AI.md)
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

# ── JS/TS ─────────────────────────────────────────────────────────────────────
js-import-order                  = builtin, third-party, local
js-import-sort                   = on
js-import-blank-lines            = 1

# ── Python 3 ──────────────────────────────────────────────────────────────────
python-import-sort               = on
python-import-blank-lines        = 1

# ── AI-assist (GRU) ───────────────────────────────────────────────────────────
gru-classifier                   = on          # off | on (a real trained weights file now ships, see STATE_AI.md)
gru-weights-path                 =             # empty = derive from program dir, see STATE_AI.md
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
`DeclarationAlignmentRule`, `GetterSetterRule`, `MiscRule` held only
curly-brace-family (C/C++/Java/Kotlin) logic. Ahead of Python3/data-format/
JS-TS jobs landing real logic, each was split into a slim `*Core` base plus
family siblings (`*Curly`, and skeletons for `*Indent`/`*Tags`) so each
future job gets a clean landing file. Mechanical rename/move only, no
behavior change — internal branching (incl. Kotlin-vs-C/C++/Java checks)
moved unchanged into whichever sibling it landed in.

**Scoping:** `DeclarationAlignmentRule`/`GetterSetterRule` got
Core+Curly(+Indent skeleton) only, no `Tags` — XML/HTML have no
declaration/getter-setter concept. `TokenizerCore`/`Formatter`/
`ScopePipeline`/`MiscRule` got the full Core+Curly+Indent+Tags split.
`ComplexityPaddingEvaluator.java` not split (extend in place when needed).
`Lang.java` gained `isCurly`/`isIndentBased`/`isTagBased` predicates first.
`FormatterCore.forLanguage(String)` is the static dispatcher factory (picks
`Curly`/`Indent`/`Tags` by family) — `Main.java`/`ServerMode.java` need no
if/else on language.

**Plan deviations** (when a "Core" method called a "Curly" one, the callee
moved to Core alongside its caller instead):
- `DeclarationAlignmentRule`: `renderTokens`/`renderInitTokens`/
  `needsSpaceBetween`/`isTightToken`/`isCStyleCastClose` (+
  `CONTROL_FLOW_KEYWORDS`) kept together in Core.
- `MiscRule`: `needsSpaceBetween`/`isTightToken`, `capitalizeFirstLetter`/
  `isCommentNoCapitalizeWord` (+ `COMMENT_NO_CAPITALIZE_C/CPP/JAVA`),
  `renderTokens`/`templateAngleTokens`, and generic scan helpers
  (`matchParenForward/Backward`, `next/prevSignificantIndex`, `anyFrozen`,
  `significantOnly`/`significantWithComments`) all kept in Core (each has a
  Core caller). `splitTopLevelCommas` moved to Curly despite its generic
  name — used only by Curly's signature/call-rendering methods.
- `KotlinDeclarationAlignmentRule`/`KotlinGetterSetterRule`/
  `KotlinSignatureRule` extend `*Curly` (not `Core`) — they reuse
  Curly-side protected members.

**Reusable gotchas for future similar splits:**
- A Python script masking `//`/`/* */`/string/char-literal spans before
  brace-counting mechanically extracted method bodies into Core vs Curly
  files (byte-identical); scale marker count to file size (MiscRule needed
  113 markers for 3353 lines).
- An inherited static nested class must be imported via its declaring
  class's canonical name, not the subclass (e.g. `MiscRuleCore.Assignment`
  not `MiscRuleCurly.Assignment`) — javac rejects the subclass import form.
- Bulk `private`→`protected` fixes needed wherever a Core method is now
  called from a Curly sibling.
- `git rm` (not `rm` + `git add`) needed to stage a deletion on this
  system's old git version.
- Watch for extraction scripts starting a marker mid-declaration and
  dropping a `public/private static` modifier prefix — verify each
  extracted nested class's modifiers against the original.

**Result:** every file group landed as its own checkpoint commit, `make
test` green (90/90 forward + 90/90 idempotency) after each. Job-file
class-name references updated to `*Curly` (commit `9cce1a5`); stale
pre-refactor class-name mentions swept later (commit `949b7a9`).

**2026-07-28 cleanup-pass follow-up:** swept every `*Curly`/`*Indent`/
`*Tags` sibling for helpers independently re-derived per job (the scenario
this section's own text flagged as a risk). Found one genuinely
byte-identical case fitting the "clearly mechanical, low-risk" bar:
`TokenizerCurly` and `TokenizerIndent` had each defined their own private
`setOf(String...)` (build a `Set<String>` from varargs) — both already
extend `TokenizerCore`, so it was promoted there as `protected static` and
both duplicates deleted (commit follows). Left three other same-named
`setOf` copies alone (`DeclarationAlignmentRuleCore`, `MiscRuleCore`,
`BlockStructureRule`, `KeywordAmbiguityGate`) — those live in unrelated
class hierarchies with no common ancestor short of introducing a brand-new
shared utility class touched by many unrelated files, a bigger and riskier
move than this housekeeping pass's mechanical-promotion bar. No other
duplicated helper found worth promoting this pass.

---

## Architectural TODOs

### General scope-depth reindentation (not started — high risk, read before attempting)

**Current state** (confirmed by direct testing, C++26 session): the formatter
does not reindent ordinary body statements from scratch — original
whitespace is preserved except for specific recognized rewrites (brace
placement, spacing, alignment). Only `SwitchRule.applyNonInlineCaseIndent`
and `ScopePipeline.applyDeclarationsPass` reindent anything, and both apply
one **relative delta** from a single reference line, not an absolute target
derived from brace-nesting depth. `STATE_C_CPP_JAVA.md`'s "Known Gaps — Open"
documents two real bugs from this shape (`ASTParser.java` in
`javaparser/javaparser`; local `tool/JSONEncoderLite.java`) — non-idempotent
reindentation on internally-inconsistent source, both ACCEPTED-not-fixed:
the real fix (derive each line's absolute target from structural depth, not
a raw-source delta) is nontrivial with real regression risk for a narrow
shape.

**Why a *general* version (every line, not just switch-case/declarations) is
much harder/riskier than those two narrow passes:**
- **Blast radius inversion.** Current invariant: don't touch indentation
  unless a specific construct requires it — why every real-code bug found so
  far (~20+ external repos, "Finished dogfood" list) has been narrow/
  isolated. A general pass makes every line in every file a candidate for a
  wrong result (currently rare, 1/~2000 files in `javaparser`) — would
  become the default risk surface for the whole corpus.
- **Continuation vs. block depth is a second axis, not a free extension.**
  Brace/paren/bracket depth alone isn't enough — wrapped expressions,
  chained calls, multi-line initializers each have their own
  continuation-indent conventions (STYLE.md §2) that don't reduce to "one
  level per `{`". Any real implementation must merge two indent models
  without them fighting — exactly what the two existing narrow passes get
  subtly wrong today.
- **Content that must never be touched.** Raw string literals, block-comment
  interior lines, preprocessor directives (column-0 regardless of depth,
  own continuation rules), and `frozen` spans all need exclusion — each has
  already been a real bug source (backslash-continued preprocessor
  corruption, raw-string tokenizer gap, "Finished dogfood" list) under the
  current narrower passes; a general pass multiplies where these exclusions
  must be reapplied.
- **Ordering interacts with every other pass.** Brace-placement (Allman),
  line-wrapping (`enforceCallLineBreaking`), switch-case handling all run at
  specific `FormatterCurly` phase points because their output affects what
  "correct" indentation even is afterward (see the
  `formatNonInlineSwitches`/`enforceCallLineBreaking` ordering bug, fixture
  `_56`). A general reindent pass needs to run after every line-count/brace
  decision is final; an ordering bug here produces plausible-looking-wrong
  output, not a crash — hard to catch by inspection.

**If ever attempted:**
- Treat as its own dedicated multi-session job with its own `STATE_*.md` —
  do not fold into an existing job's file. Likely touches
  `ScopePipelineCurly.java` primarily, potentially subsuming/replacing
  `SwitchRule.applyNonInlineCaseIndent`'s relative-delta logic (would retire
  the two open Known Gaps above as a side effect).
- `make test`'s fixture corpus is a floor, not a substitute, for validation
  — fixtures were tuned under the current indentation-preserving model, so
  passing them only proves "didn't break already-exercised lines," not the
  much larger space of newly-reindented ordinary lines. Re-run real-code
  testing against at least `javaparser/javaparser`, local
  `tool/JSONEncoderLite.java`, `serge-sans-paille/frozen` (where the
  existing indent bugs surfaced), plus a fresh untested large corpus
  (full-tree idempotency, not `--out DIR`) — neither open gap was caught by
  `make test` alone, both came from one-off real-code-testing sessions.
- Expect this to be the single riskiest change ever made to this
  formatter's core; budget accordingly, not as an incremental fix.

### Project refactoring/cleanup pass

After `angular/angular` and `python/cpython` dogfood runs (the last
remaining "cheap-ish" novel-shape corpora before the rest of each job's
test-fixture-repo list crosses ~1000 kLOC and real-code-testing cycle time
grows substantially), consider a dedicated cleanup pass across jobs rather
than immediately starting the next >1000 kLOC candidate. Candidate scope:

- Sweep each job's "Known Gaps" sections (`STATE_C_CPP_JAVA.md`,
  `STATE_PYTHON3.md`, etc.) for ACCEPTED-not-fixed items that may now be
  cheaper to actually fix given everything learned since — do not treat
  "accepted" as permanent without re-checking.
- Check for unused file that contains only boilerplate code and never used.
- Check for duplicated helper logic that accreted independently across the
  `*Curly`/`*Indent`/`*Tags` class split (see this file's "Class Refactor"
  section) now that several jobs (Python3, JS/TS, data formats) have each
  landed real logic in their own `*Indent`/`*Tags` classes — some of what
  was written bespoke per-job may now warrant promotion to a shared
  helper, the same way `TokenizerCurly`'s `MULTI_CHAR_OPS` pattern was
  independently re-derived per job.
- Re-read each job's STATE_*.md for stale/contradictory notes now that
  compaction passes have happened (e.g. 2026-07-26's compaction of
  `STATE_C_CPP_JAVA.md`/`STATE_PYTHON3.md`/`test/README.txt`) — verify
  compacted prose didn't silently drop a still-relevant caveat.
- Update and fix `CLAUDE.md`, `README.md`, `../README.txt`,
  `../AI_PREAMBLE_FULL.md`, and `../AI_PREAMBLE_AESTHETIC.md`.
  **2026-07-28: checked, none needed a fix.** All five already track
  actual shipped code state (all six data formats + JS/TS + Python3 real,
  JSX/TSX correctly still called out as out-of-scope, HTML5's `<script>`
  dispatch correctly described as implemented in both `README.md` and
  `../README.txt`). The one stale `<script>`-dispatch claim found this
  pass was in `STATE_DATA_FORMATS.md`, not any of these five — already
  fixed separately (see this section's own sibling entries above/commit
  history). No doc edits made.

This is intentionally scoped as housekeeping, not a rewrite — do not let it
grow into an attempt at the "General scope-depth reindentation" item above;
that stays its own separate, dedicated, much riskier future job.

### Formatter self-formatting (dogfood-and-adopt) process

A dedicated procedure for actually reformatting the formatter's own Java
source tree (`src/`) with itself and adopting the result — distinct from
the routine dogfood *testing* already described elsewhere in this file
(which only checks compile-cleanliness/idempotency/declaration-count
against a temporary formatted copy and never touches the real `src/`
tree). Run this only when explicitly asked; do not run it as a byproduct
of an unrelated task.

1. Copy the formatter's own Java source files (`src/`) to `/tmp/fmt_ref`.
   Apply a round1 format (fresh) and a round2 format (format round1's
   output again) to that copy — the same forward-then-idempotency
   dogfood methodology used elsewhere. Fix any bug before continuing;
   do not proceed past a failing round1/round2 diff.
2. Once round1/round2 is clean, build the round2 Java source files into
   a JAR (a separate build from the formatter's currently-committed
   `target/code-formatter-1.00.jar` — do not overwrite the real build
   output with this trial JAR). Use that JAR to run the forward and
   idempotency test fixtures listed in the Makefile's `INP_FILES`. Fix
   any bug before continuing. Create additional test fixtures if needed.
3. Use that same round2 JAR to format the *original* (unformatted)
   `/tmp/fmt_ref` copy again, producing round1b and round2b. If
   everything is consistent, round1 must be byte-identical to round1b,
   and round2 byte-identical to round2b — this confirms that rebuilding
   the formatter from its own freshly-formatted source doesn't change
   its own formatting behavior (a fixed point, not just idempotent
   output). Fix any bug before continuing.
4. Once all of the above holds, copy the formatted source files from
   round1 (round1 and round2 should already be identical at this point,
   per step 1's own exit condition, so either is equivalent) back over
   the formatter's real `src/` tree, overwriting the currently-committed
   source with the formatted version. Spot-check a sample of changed
   files for correctness before proceeding.
5. Rebuild the formatter from this newly-adopted `src/` and run
   `make test` again against the real build.
6. If `make test` is clean, the formatter has successfully formatted
   itself and the change is ready for the normal commit workflow (see
   this file's own commit-workflow section above) — do not skip that
   workflow just because this is a self-referential change.
