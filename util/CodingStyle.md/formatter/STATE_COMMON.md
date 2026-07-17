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
  real-code testing (e.g. a hand-authored dogfood pair moved out of
  `FUTURE_TEST_FIXTURES.md`), add its entry in both `test/README.txt` and the
  `Makefile`'s `INP_FILES` **before** the `Real-code regressions:` section/
  entries — the same "ahead of `real_code_regressions_*`" ordering the
  Real-code testing methodology section below already uses for bug-fix
  fixtures.
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

## Class Refactor (curly/indent/tags split)

**Purpose:** `TokenizerCore`, `Formatter`, `ScopePipeline`,
`DeclarationAlignmentRule`, `GetterSetterRule`, `MiscRule` currently contain
only curly-brace-family (C/C++/Java/Kotlin) logic — zero indent/tag branching
exists in them today. Before any Python3/data-format/JS-TS job lands real
logic in these files, split each into a slim `*Core` base plus family
siblings, so each future job gets a clean landing file instead of adding to
already-large, already-entangled classes. This is a mechanical rename/move,
not a behavior change or a disentangling of mixed logic — every method's
existing internal branching (including any Kotlin-vs-C/C++/Java checks)
moves unchanged into whichever sibling it lands in.

`DeclarationAlignmentRule`/`GetterSetterRule` get **Core+Curly(+Indent
skeleton)** only, no `Tags` sibling — XML/HTML have no declaration/
getter-setter concept. `TokenizerCore`/`Formatter`/`ScopePipeline`/
`MiscRule` get the full **Core+Curly+Indent+Tags** 4-way split.
`ComplexityPaddingEvaluator.java` is not split — extend in place when a new
job needs it.

Execute as its own checkpoint-committed sequence, one file group per commit,
`make test` green (78/78 forward + 78/78 idempotency, or current live count,
zero regressions) before moving to the next group.

### Lang.java — add family predicates first (everything else depends on this)
- [x] Add `isCurly` (`isC||isCpp||isJava||isKotlin||isJs||isTs`),
      `isIndentBased` (`isPython3`), `isTagBased` (`isXml||isHtml5`) to
      `Lang.java` as instance fields (consistent with existing
      `isC`/`isCpp`/etc. style). JSON/JSON5/YAML/TOML/CSS are none of the
      three (neither brace-block, indent-block, nor tag-nested) — left
      ungated by these predicates; scope note only, not a blocker.
      `make test` 90/90 forward + 90/90 idempotency, zero regressions.

### TokenizerCore.java → TokenizerCore + TokenizerCurly + TokenizerIndent + TokenizerTags
- [x] New slim `TokenizerCore.java`: `Token`/`TokenType` (generic members
      only), `markFrozenSpans`, generic char/number/whitespace/newline
      emitters, `peek`, shared static keyword-set-selection *pattern* (not
      curly's concrete sets) — whatever a subclass constructor needs to call.
- [x] `TokenizerCurly.java` (new file, extends `TokenizerCore`): everything
      else in today's `TokenizerCore.java` — constructor's C/Cpp/Java/Kotlin
      switch, `tokenize`, `trackSignificant`, brace/bracket name-stack emit
      methods, preprocessor/raw-string/text-block/Kotlin-string helpers,
      `reclassifyAngleBrackets`. No behavior change — mechanical move only.
      Existing Kotlin-vs-C/C++/Java branches inside these methods stay
      exactly as they are today (out of scope for this refactor).
- [x] `TokenizerIndent.java` (new, skeleton): extends `TokenizerCore`, throws
      `UnsupportedOperationException` until Python3 job fills it in.
- [x] `TokenizerTags.java` (new, skeleton): same skeleton pattern for
      XML/HTML5.
- [x] Update every caller (`Formatter.java`, `ScopePipeline.java`,
      `rules/SwitchRule.java`) that directly instantiated `TokenizerCore` for
      a curly language to instantiate `TokenizerCurly` instead. (`Main.java`/
      `ServerMode.java`/tests never instantiated `TokenizerCore` directly —
      confirmed via grep, nothing else to update.)
- [x] `make test`: 90/90 forward + 90/90 idempotency, zero regressions.

### Formatter.java → FormatterCore + FormatterCurly + FormatterIndent + FormatterTags
- [x] `FormatterCore`: thin abstract class holding `lang` + the abstract
      `formatOne(content, filePath, config, formatOff)` contract, plus a
      static `forLanguage(String)` factory that constructs a `Lang` and
      dispatches to `FormatterCurly`/`FormatterIndent`/`FormatterTags` by
      family predicate -- this is the "dispatcher lives in the formatter
      class" placement, so `Main.java`/`ServerMode.java` never need their
      own if/else on language.
- [x] `FormatterCurly.java`: today's entire `Formatter.formatOne` body,
      renamed, unchanged logic (all of it is curly-only already); old static
      `Formatter.java` deleted.
- [x] `FormatterIndent.java` / `FormatterTags.java`: skeleton classes
      implementing `FormatterCore`'s contract, throwing
      `UnsupportedOperationException` until their jobs start.
- [x] Update `Main.java`/`ServerMode.java` call sites: both now call
      `FormatterCore.forLanguage(language).formatOne(...)` instead of
      constructing `Formatter` directly.
- [x] `make test`: 90/90 forward + 90/90 idempotency, zero regressions.

### ScopePipeline.java → ScopePipelineCore + ScopePipelineCurly + ScopePipelineIndent + ScopePipelineTags
- [x] `ScopePipelineCore.java`: abstract class holding `Span`/`Replacement`,
      splice/indent/whitespace primitives, `buildIndexMap`, and every
      zero-language-gating helper (`indentUnit`, `isWhitespaceOrNewline`,
      `hasTopLevelNewline`, `anyFrozen`, `trailingGapHasComment`,
      `prevSignificantIndex`/`nextSignificantIndex`,
      `matchParenForward/Backward`, `matchBraceForward`, `splice`,
      `joinText`, `trailingIndent`, `leadingSpaceCount`,
      `stripTrailingSpaces`, `normalizeIndent`, `normalizeLeadingGap`,
      `trimTrailingWhitespace`, `trailingRunNewlineCount`,
      `findSpanContaining`). `normalizeIndent`/`indentUnit` used to read
      `miscRule.indentWidth` (a curly-only field) -- Core now holds its own
      `protected final int indentWidth` set via constructor, same value,
      no behavior change. `process(String)` is Core's one abstract entry
      point, matching the `FormatterCore.forLanguage`-style dispatcher
      shape (though nothing currently needs a `ScopePipelineCore` factory,
      since `ScopePipelineCurly` is only ever constructed directly by
      `FormatterCurly`, never dispatched-to from `Main`/`ServerMode`).
- [x] `ScopePipelineCurly.java`: `process()` entry point plus all four
      rule-driving passes (`applyDeclarationsPass`,
      `applyOversizedAggregateInitClosingBracePass`, `applyAssignmentsPass`,
      `applySignaturePass`, `applyGetterSetterPass`) and their
      Kotlin-vs-C/C++/Java internal branches, unchanged -- mechanical move
      via a Python brace-matching script (not hand-retyped) to guarantee
      byte-identical method bodies; only the class header/fields/
      constructors were hand-edited (extends `ScopePipelineCore`, calls
      `super(indentWidth)`, drops the now-inherited `indentWidth` field).
- [x] `ScopePipelineIndent.java` / `ScopePipelineTags.java`: skeletons,
      constructor takes `indentWidth` (passed to `super`), `process` throws
      `UnsupportedOperationException` referencing STATE_PYTHON3.md /
      STATE_DATA_FORMATS.md respectively.
- [x] Update callers: `FormatterCurly.java`'s `new ScopePipeline(...)` →
      `new ScopePipelineCurly(...)`. Confirmed via grep this was the only
      real caller (no test file constructs `ScopePipeline` directly).
      Old `ScopePipeline.java` removed via `git rm` (plain `rm` + `git add`
      does NOT stage a working-tree deletion on this system's git version --
      see the `Formatter.java` removal note above, same fix reused).
- [x] `make test`: 90/90 forward + 90/90 idempotency, zero regressions.

### DeclarationAlignmentRule.java → DeclarationAlignmentRuleCore + DeclarationAlignmentRuleCurly + DeclarationAlignmentRuleIndent (no Tags)
- [x] Discovered mid-refactor: `KotlinDeclarationAlignmentRule extends
      DeclarationAlignmentRule` (not called out explicitly in the original
      plan text). Resolved by having it extend `DeclarationAlignmentRuleCurly`
      instead of `Core` post-split, since it directly reuses several
      protected members (`hasBlankLineBefore`, `hasCommentBefore`,
      `significantOnly`, the inherited `needsSpaceBetween`, and the
      `lineLengthLimit` field) that only make sense together on the Curly
      side, not as bare Core primitives.
- [x] The plan's literal per-method Core/Curly split does not compile as
      written: it assigns `renderInitTokens` to Core but `isCStyleCastClose`
      (which `renderInitTokens` calls) to Curly. Resolved pragmatically by
      keeping `renderTokens`/`renderInitTokens`/`needsSpaceBetween`/
      `isTightToken`/`isCStyleCastClose` (plus the `CONTROL_FLOW_KEYWORDS`
      constant they depend on) together in Core instead — deviates from the
      plan's literal method list but preserves its intent and the
      "no behavior change, mechanical move only" mandate.
- [x] `DeclarationAlignmentRuleCore.java`: `lang`/`lineLengthLimit` fields +
      constructor, `setOf` helper, `CONTROL_FLOW_KEYWORDS` constant,
      `renderTokens`/`renderInitTokens`/`isCStyleCastClose`/
      `needsSpaceBetween`/`isTightToken`, `splitStatements`,
      `pullTrailingSameLine`, `isAccessSpecifierColon`, `hasCommentBefore`,
      `hasBlankLineBefore`, `lastSignificantIdx`, `findTrailingComment`,
      `significantOnly`. Used the same Python brace-matching extraction
      script technique as the `ScopePipeline` split (proven reliable there)
      rather than hand-retyping; the script's naive brace counter needed one
      fix this time — prose comments with an unbalanced single backtick-brace
      (e.g. "before the `{`") threw off simple char-by-char counting, so the
      script was extended to mask out comment/string/char-literal spans
      before counting braces.
- [x] `DeclarationAlignmentRuleCurly.java`: constructors (call
      `super(lang, lineLengthLimit)`), `groupDeclarations`, `reorderStatics`,
      `parseDeclaration`, `render`, all C/C++/Java-specific helpers
      (`splitCppType`, `isJavaEnumConstantListShape`), `union` helper,
      unchanged. Class stays non-final (`KotlinDeclarationAlignmentRule`
      extends it). Note the existing stray `lang.isKotlin` check in
      `needsSpaceBetween` (now on Core) — left as-is (out of scope; this
      class is dispatched-around for Kotlin already via
      `KotlinDeclarationAlignmentRule`, do not investigate/remove the
      vestigial check as part of this refactor).
- [x] `DeclarationAlignmentRuleIndent.java`: skeleton for future Python3
      assignment-alignment reuse (optional, not required — Python3's own
      alignment-grid work may end up entirely bespoke instead).
- [x] Updated `KotlinDeclarationAlignmentRule.java`'s
      `extends DeclarationAlignmentRule` → `extends DeclarationAlignmentRuleCurly`.
- [x] Updated `ScopePipelineCurly`'s `applyDeclarationsPass` caller (import,
      field type, and constructor call all changed
      `DeclarationAlignmentRule` → `DeclarationAlignmentRuleCurly`). Grepped
      all 10 other files referencing `DeclarationAlignmentRule` by name and
      confirmed every other hit is a comment/javadoc mention only, no code
      change needed (`KotlinGetterSetterRule.java`, `KotlinSignatureRule.java`,
      `JavaSpecificRule.java`, `GetterSetterRule.java`, `CppSpecificRule.java`,
      `MiscRule.java`, `KotlinSpecificRule.java`, `BlockStructureRule.java`).
- [x] Old `DeclarationAlignmentRule.java` removed via `git rm` (not plain
      `rm`, per the established fix for this system's old git version not
      staging working-tree deletions via `git add`).
- [x] `make test`: 90/90 forward + 90/90 idempotency, zero regressions.

### GetterSetterRule.java → GetterSetterRuleCore + GetterSetterRuleCurly + GetterSetterRuleIndent (no Tags)
- [x] `GetterSetterRuleCore.java`: `lang`/`indentWidth`/`lineLengthLimit`
      fields + constructor, the `Member` nested class (needed here since
      `bodyWidth` takes one), `bodyWidth`, `padRight`, `cellText`,
      `splitMembers`, `consumeTrailingSameLine`, `hasBreakableCall`,
      `findNameBeforeParen`, `matchBracket`, significance/whitespace helpers
      (`isInsignificant`, `firstSignificantIndex`, `nextSignificant`,
      `trimLeadingWs`, `trimTrailingWs`, `hasNewlineBetween`,
      `hasBlankLineRun`, `prevSignificant`). Used the same Python
      comment/string-masked brace-matching extraction script as the
      `DeclarationAlignmentRule` split (proven reliable there).
- [x] `GetterSetterRuleCurly.java`: `STATEMENT_KEYWORDS` constant,
      `modifierPriority` field, constructors (call
      `super(lang, indentWidth, lineLengthLimit)`), `groupOneLiners`,
      `OUTLIER_RATIO`, `excludeOutliers`, `render`, `parseOneLinerMember`,
      `isPostParenQualifier`, `hasAccessSpecifier`, unchanged.
- [x] `GetterSetterRuleIndent.java`: skeleton (Python3 `@property`/
      `@x.setter` pair handling, if that job ever wants to reuse this shape —
      not committed, just a landing spot).
- [x] Updated `KotlinGetterSetterRule.java`'s
      `extends GetterSetterRule` → `extends GetterSetterRuleCurly`.
- [x] Updated `ScopePipelineCurly`'s `applyGetterSetterPass` caller (import,
      field type, constructor call). Note: the `Member` inner-class import
      must name its actual declaring class (`GetterSetterRuleCore.Member`),
      not the subclass (`GetterSetterRuleCurly.Member`) — javac rejects an
      import of an inherited nested type via the subclass's canonical name
      ("import requires canonical name"), even though plain code references
      to `GetterSetterRuleCurly.Member` work fine via inheritance.
- [x] Grepped all other files referencing `GetterSetterRule` by name and
      confirmed every other hit is a comment/javadoc mention only
      (`FormatterCurly.java`, `JavaSpecificRule.java`,
      `KotlinDeclarationAlignmentRule.java`, `MiscRule.java`,
      `KotlinSpecificRule.java`).
- [x] Old `GetterSetterRule.java` removed via `git rm`.
- [x] `make test`: 90/90 forward + 90/90 idempotency, zero regressions.

### MiscRule.java → MiscRuleCore + MiscRuleCurly + MiscRuleIndent + MiscRuleTags
- [ ] `MiscRuleCore.java`: the fully-generic passes and helpers —
      `convertIndentation`, `enforceKeywordSpacing`,
      `enforceComplexityPadding`, `enforceInitializerBraceSpacing`,
      `groupAssignments`/`render(Assignment)`, `enforceCommentStyle`/
      `alignCommentSeparators` (the two tiny Java/Cpp-gated spots move with
      their enclosing method into Curly instead, not Core), generic
      token-scan utilities.
- [ ] `MiscRuleCurly.java`: `enforcePreIncrement`+helpers,
      `parseSignature`/`render(Signature,...)`,
      `insertBlankLineBeforeReturn`+helpers (incl. Kotlin-only islands),
      `enforceCallLineBreaking`+render helpers, `templateAngleTokens`,
      `isTightToken`, and the two Java/Cpp-gated comment-capitalization
      spots — unchanged internal branching.
- [ ] `MiscRuleIndent.java` / `MiscRuleTags.java`: skeletons.
- [ ] Update `FormatterCurly` caller.
- [ ] `make test` full pass, zero regressions.

### ComplexityPaddingEvaluator.java — no split
- [ ] Confirmed: extend in place with new functions as new languages need
      them. No action now.

### Wrap-up
- [ ] Update `STATE_C_CPP_JAVA.md` and `STATE_KOTLIN.md`'s own file/class
      references (e.g. any prose naming `TokenizerCore.java`/
      `Formatter.java`/etc. that now needs the `*Curly` suffix) so they
      don't go stale.
- [ ] Full `make test` + a real-code regression re-run (per this file's
      Real-code testing methodology) against the same candidate(s) already
      used for C/C++/Java/Kotlin, to confirm the rename/split introduced
      zero behavior change.
- [ ] Series of checkpoint commits per this file's ~50-line-diff convention
      (this refactor needs many commits — one group of file(s) per commit,
      e.g. Tokenizer split as one commit, Formatter split as the next,
      etc.), each with `make test` green before committing.

---

## Future Cleanup TODOs

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
