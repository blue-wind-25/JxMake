# STATE_TOOLING.md — Build/Dev-Tooling Script Formatter Tracker (Makefile, Bash, PowerShell)

Read `STATE_COMMON.md` first — shared commit/ambiguity/testing conventions
this file assumes. No other job's `STATE_*.md` is required reading.
Dogfood corpus status: see `STATE_DOGFOOD.md` (candidates registered as
`NOT STARTED`; nothing in this job has reached a real-code run yet).

---

## Purpose

Tracks three separate, narrow, beautification-only formatters — Makefile,
Bash, and PowerShell — per `STYLE_TOOLING.md`. Grouped into one job because
none of the three is large enough to warrant its own file (unlike, say,
Kotlin or JS/TS), the same way `STATE_DATA_FORMATS.md` groups seven data
formats.

**Makefile, Bash, and PowerShell all implemented.** Makefile landed
real logic (`Lang.isMakefile`, `FormatterMakefile`, `MakefileSpecificRule`);
Bash landed real logic (`Lang.isBash`, `FormatterBash`, `BashSpecificRule`,
all five §2 rules); PowerShell landed real logic (`Lang.isPowerShell`,
`.ps1`/`.psm1` infer, `FormatterPowerShell`, `PowerShellSpecificRule`, all
six §3 rules) — see the checklist below. `Lang.SCAFFOLD_ONLY_LANGUAGES`
remains unaffected (still empty) — makefile/bash/powershell were added
directly to `Lang.SUPPORTED_LANGUAGES` as each landed.

**Canonical language order** for any documentation/help-string/`--lang`
enumeration this job's languages join is recorded in `CLAUDE.md` (search
"Canonical language order") — `makefile`, `bash`, `powershell` come last,
after `python3`. `README.md`, `../README.txt`, and `CLAUDE.md` all list
the three as JAR-implemented (doc-sync checklist item below).

---

## Scope

Each of the three languages has its own section in `STYLE_TOOLING.md`
(§1 Makefile, §2 Bash, §3 PowerShell). Common thread distinguishing this
job from every other language job in this repo: a short **fixed list** of
specific transforms, with an explicit "leave everything else
byte-identical" rule — no general-purpose reindentation/re-wrapping
fallback like curly-brace or data-format languages have. Getting the
"don't touch anything else" boundary right (via a real tokenizer per
language, not naive text substitution) is the main risk for Bash and
PowerShell; Makefile is line-oriented and only needs to distinguish
tab-prefixed recipe lines from everything else — no tokenizer needed.
Relative difficulty: Makefile easiest (pure line/regex); Bash and
PowerShell comparable, each needing a small real tokenizer (quoting,
heredocs/here-strings, comments) so fixed-rule passes never fire inside a
string/comment/heredoc — bounded scope, not a full grammar.

Several open questions in `STYLE_TOOLING.md` (marked inline, "resolve via
RDD before implementing") must be resolved before implementation starts
on the affected rule — do not guess a default and implement against it.

---

## Resolved Design Decisions

Full text lives in `RDD_LOG.md` (shared sequence across all jobs — see
`STATE_COMMON.md`'s lookup convention, `grep -Fm1`, no `-A`). All five
below were decided in one scoping session before any implementation
started (docs-only, no code landed yet).

| Key | Topic |
|---|---|
| RDD_KEY_254 | Alignment-group boundary (Makefile §1.1, PowerShell §3.2): blank line OR any non-matching line breaks the group |
| RDD_KEY_255 | Makefile §1.3 target spacing: one space after `:`, single spaces between prerequisites |
| RDD_KEY_256 | PowerShell §3.3: inline pipeline-stage scriptblock always stays single-line, never brace-depth-indented |
| RDD_KEY_257 | PowerShell §3.4: single-line hashtable literal left as-is, never forced multi-line |
| RDD_KEY_258 | PowerShell §3.6: `{`/`}` spacing applies everywhere, including single-line scriptblocks |
| RDD_KEY_259 | (**REVERSED by RDD_KEY_260** — no longer in effect) Comment normalization for all three reuses the shared comment-classifier pipeline — premise was factually wrong, see RDD_KEY_260 |
| RDD_KEY_260 | **REVERSES RDD_KEY_259** — the shared classifier pipeline is curly-family-only (`MiscRuleCore`/`FormatterCurly`), not language-agnostic; Makefile/Bash/PowerShell comment normalization, if added, follows the simpler TOML-style ad hoc pattern instead |
| RDD_KEY_261 | Comment normalization landed (refines, doesn't reverse, RDD_KEY_260): shared `ToolingCommentNormalizer` (start-case + end-period, reusing the existing global config keys) wired into all three; Bash alone gets a Unix-tool-name no-capitalize word list, Makefile/PowerShell get plain cap only |
| RDD_KEY_267 | `#`-comment chain-grouping (2026-08-08 brief decision #3, parity with curly/RDD_KEY_265/RDD_KEY_266): consecutive standalone `#` comment lines now normalize as one unit via `ToolingCommentNormalizer.normalizeChain`, instead of each comment independently; fixed a latent bug where a mid-chain sole `.` (not on the chain's last comment) was incorrectly stripped by the old per-comment-only logic |

---

## Config

No language-specific config keys — the five/six fixed-rule-list transforms
per language are unconditional (mirrors the "no gate" precedent already set
by the rest of this job, since none of RDD_KEY_254–RDD_KEY_258 asked for
one). Comment normalization (RDD_KEY_261) reuses the existing global
`normalize-comment-start-case`/`normalize-comment-end-period` keys already
defined in `Config.java` for JSON/YAML/CSS/TOML/XML — no new keys added.

## Test Fixtures (Local)

| Pair | Covers |
|---|---|
| `makefile_combined_{inp,out}.mk` | STYLE_TOOLING.md §1.1–§1.4 + tab-recipe leave-alone + comment group-break |
| `bash_combined_{inp,out}.sh` | STYLE_TOOLING.md §2.1–§2.5 + string/comment/heredoc/`$(...)` safety |
| `powershell_combined_{inp,out}.ps1` | STYLE_TOOLING.md §3.1–§3.6 + string/comment/here-string safety |

Registered active in `Makefile` `INP_FILES` (before curly-GDR locals / real-code
regressions) and documented in `test/README.txt`.

## Test Fixtures (External, corpus-scale)

Candidates registered in `STATE_DOGFOOD.md` as `NOT STARTED` (see checklist
item below for the full list + reuse notes). No dogfood run started yet.

## Tools/compiler used

Candidates to evaluate when a dogfood run starts: `shellcheck` (Bash syntax
validation, not formatting comparison), `make -n`/`make -q` (Makefile
syntax sanity), PowerShell's own parser (`[System.Management.Automation.
PSParser]::Tokenize` or `Invoke-ScriptAnalyzer` if available in this
sandbox — availability unconfirmed, check before relying on it).

---

## Open Questions

None remaining — all six original scoping questions were resolved in one
session (RDD_KEY_254–RDD_KEY_258 above; RDD_KEY_254 covers both the
Makefile §1.1 and PowerShell §3.2 instances of the same alignment-group-
boundary question). See `STYLE_TOOLING.md` for the resolved rule text.

---

## Checklist

- [x] Resolve all six original scoping open questions (Makefile §1.1/§1.3,
      PowerShell §3.2/§3.3/§3.4/§3.6) via `AskUserQuestion`; recorded as
      RDD_KEY_254–RDD_KEY_258, `STYLE_TOOLING.md` updated to state the
      resolved rules inline (no more "TBD"/open-question markers).
- [x] Resolve comment-normalization scope decision (RDD_KEY_259, later
      corrected by RDD_KEY_260 after the premise was found factually
      wrong on re-check): comments are out of scope for the original
      5-rule lists; *if* normalization is added later it follows the
      simpler TOML-style ad hoc pattern (config-gated first-letter
      capitalization, no classifier dependency), not the curly-only
      `CommentClassifier`/GRU pipeline. `STYLE_TOOLING.md` §0 updated.
- [x] Implement Makefile §1.1 Assignment Alignment.
- [x] Implement Makefile §1.2 Continuation-Line Alignment.
- [x] Implement Makefile §1.3 Target Spacing.
- [x] Implement Makefile §1.4 Conditional Indentation.
      Landed as `Lang.isMakefile`/`Lang.infer` basename+`.mk` detection,
      `FormatterCore.forLanguage` dispatch, `FormatterMakefile` (standalone,
      `FormatterToml`-style, not part of any existing family), and
      `MakefileSpecificRule` (line-oriented, no tokenizer needed — only
      distinguishes tab-prefixed recipe lines, which are never touched).
      Smoke-tested manually (diff + idempotency + `--lang makefile` +
      extensionless `Makefile` + `.mk` detection all verified); no local
      test fixture pair registered yet (that's still the separate,
      unchecked "Author local test fixture pairs" item below). Comments
      remain untouched (out of scope, STYLE_TOOLING.md §0).
- [x] Bash: build/extend a tokenizer sufficient to safely skip quoting,
      heredocs, comments, command substitution, and arithmetic contexts.
- [x] Implement Bash §2.1 `if`/`then` merge.
- [x] Implement Bash §2.2 pipe spacing.
- [x] Implement Bash §2.3 function brace placement.
- [x] Implement Bash §2.4 `case` formatting.
- [x] Implement Bash §2.5 arithmetic operator spacing.
      Landed as `Lang.isBash`/`Lang.infer` `.sh`/`.bash` extension detection,
      `FormatterCore.forLanguage` dispatch, `FormatterBash` (standalone,
      `FormatterMakefile`-style), and `BashSpecificRule`: two-pass design —
      pass A is a character-level state-machine tokenizer (quotes, `$'...'`,
      backticks, `$(...)`, `$((...))`, `#` comments, `<<`/`<<-` heredocs incl.
      quoted/bareword delimiters) that also applies the two token-level rules
      (§2.2, §2.5) inline via a `RunBuffer` flushing on every kind change;
      pass B is line-oriented (§2.1/§2.3/§2.4), guarded by a per-line purity
      flag (first non-whitespace char is code). Arithmetic nested inside a
      double-quoted string is still processed (per the §2.5 example), but
      nested inside `$(...)`/backticks stays opaque, consistent with leaving
      nested command-substitution content untouched. Smoke-tested manually
      (STYLE_TOOLING.md §2 combined example, pipe-in-string/comment safety,
      heredoc/backtick/`$(...)` safety — all byte-identical + idempotent).
      `make test` clean after landing: 248/248 forward, 248/248 idempotency —
      purely additive. No local fixture pair yet (deferred at user's
      explicit request — separate unchecked item below).
- [x] PowerShell: build/extend a tokenizer sufficient to safely skip
      string literals, here-strings, and comments.
      Landed as `Lang.isPowerShell`/`Lang.infer` `.ps1`/`.psm1` extension
      detection, `FormatterCore.forLanguage` dispatch, `FormatterPowerShell`
      (standalone, `FormatterBash`-style), and `PowerShellSpecificRule`
      tokenizer: character-level state machine covering single-quoted
      strings (`''` escape), double-quoted strings (backtick escape,
      `$(...)` subexpressions re-enter code mode so brace-depth can see
      real scriptblock braces, `${...}` stays opaque), expandable
      here-strings `@"..."@` and literal here-strings `@'...'@` (terminator
      only at column 0; open requires quote-then-optional-WS-then-newline,
      so `@{` hashtables aren't mistaken for here-strings), line comments
      `#`, and nestable block comments `<# ... #>`. Top-level backtick
      escape keeps the next character from starting a string. Pass A
      classifies every character 'C'/'O' and re-emits identity via a
      Bash-style `RunBuffer` (token-level §3.x transforms plug in on 'C'
      flushes later); `computeLinePurity` mirrors Bash so structural rules
      can refuse here-string-body / full-comment lines. Smoke-tested:
      kind map for all construct types, purity, multi-construct identity,
      `--lang powershell` + extension infer, idempotent. `make test` clean:
      248/248 forward, 248/248 idempotency — purely additive. No local
      fixture pair yet (deferred with the shared fixtures checklist item).
      §3.1–§3.6 transforms landed in subsequent checklist items.
- [x] Implement PowerShell §3.1 brace-depth indentation.
      Naive brace-depth reindent over code-kind `{`/`}` only (opaque
      strings/here-strings/comments never contribute). Pure-code lines are
      stripped of leading whitespace and re-emitted at
      `depth - leadingCloses`; non-pure lines (here-string bodies, full-line
      comments) keep original leading whitespace byte-identical. Smoke:
      nested if bodies, multi-line `@{` hashtable body indent, here-string
      body braces untouched, `# comment {` does not affect depth; idempotent.
      Full `make test` left for the §3.2–§3.6 series end (additive path).
- [x] Implement PowerShell §3.2 operator spacing + `=` alignment.
      Kind-aware spacing around `=`/`+=`/`-=`/`*=`/`/=`/`%=` and binary
      `+/*/%` (bare `-` left alone so `-gt`/`-eq`/`-Path` stay intact).
      Block-scoped `=` alignment (RDD_KEY_254) on consecutive pure
      assignment lines, broken by blank or non-assignment; first depth-0
      code-kind assignment op on the line. Order: spacing → §3.1 indent →
      alignment. Smoke: `$a=1`/`$bb=$a+2` align; nested under if; multi-line
      `@{` entries align; `"a=b"` string untouched; `-gt` intact; comment
      breaks group; idempotent.
- [x] Implement PowerShell §3.3 pipeline split/align.
      Depth-0 code-kind `|` split (one segment/line after the first), `|`
      at a shared absolute column (min one space before `|`), continuations
      at base+1 indent. Pipes inside `()`/`[]`/`{}` stay unsplit so inline
      scriptblock args remain single-line (RDD_KEY_256). Already-split
      pipelines re-joined via trailing depth-0 `|` before re-split
      (empty segments dropped) for idempotency. Runs after §3.1 indent so
      base indent is correct inside blocks. Smoke: STYLE 3-stage pipeline
      pipe columns match; nested `{$_|...}` unsplit; string `"a|b|c"` safe;
      inside-if indent; idempotent.
- [x] Implement PowerShell §3.4 hashtable spacing.
      No separate pass: multi-line `@{` bodies get §3.1 indent + §3.2
      entry `=` alignment (per-line depth starts at 0 so entry `=` is
      depth-0); single-line hashtables are never force-expanded
      (RDD_KEY_257) -- operator spacing may still tidy interior `=`.
      Smoke: STYLE multi-line Name/Age align; `$h=@{Name="John";Age=20}`
      stays one line; idempotent.
- [x] Implement PowerShell §3.5 `switch` formatting.
      Keyword-paren spacing (`switch($x)` → `switch ($x)`, also `if`/
      `while`/`for`/`foreach`/…) kind-aware; switch-arm `{` column
      alignment on consecutive pure arm-like lines (same indent; blank/
      non-arm breaks group -- RDD_KEY_254), skipping control-flow headers
      (`if`/`function`/…). Indent via §3.1. Smoke: `1`/`22` arms align;
      `default` aligns; `if ($x)` spacing; idempotent. Interior
      `{ "…" }` spaces arrive with §3.6.
- [x] Implement PowerShell §3.6 `{`/`}` spacing.
      Kind-aware: one space before `{` except after `@` (keeps `@{` tight);
      non-empty same-line bodies get a space after `{` and before `}`; empty
      `{}` stays tight. Applies everywhere including single-line scriptblock
      args (RDD_KEY_258). Placed before indent/align so later passes see
      spaced braces. Fixed switch-arm parser to not treat `@{` as an arm.
      Smoke: all five STYLE_TOOLING.md §3 examples match byte-for-byte +
      idempotent (3.1 nested if, 3.2 align, 3.3 pipeline pipe-col 40, 3.4
      hashtable, 3.5 switch arms); single-line `@{...}` not expanded.
      Full `make test` re-run after landing.
- [x] Remove all `RDD*` references from `STYLE_TOOLING.md`. A style file
      must not reference implementation states.
      Stripped every `RDD_KEY_*` citation from `../STYLE_TOOLING.md` and
      rewrote §0 (comments) plus the Bash "added via an RDD" clause and the
      Config footer so the style file states rules only — no classifier/
      `MiscRuleCore`/`FormatterCurly` implementation narrative, no "landed
      implementation" status language. Rule text itself unchanged
      (alignment-group boundary, target spacing, pipeline scriptblock
      single-line, single-line hashtable left as-is, `{`/`}` spacing
      everywhere).
- [x] Author local test fixture pairs per each language's rule set,
      register in `test/README.txt` / Makefile `INP_FILES` before
      the regression fixtures.
      One combined pair per language, covering that language's full
      STYLE_TOOLING.md rule list plus tokenizer safety cases:
      `makefile_combined_{inp,out}.mk` (§1.1–§1.4 + tab-recipe leave-alone
      + comment group-break), `bash_combined_{inp,out}.sh` (§2.1–§2.5 +
      string/comment/heredoc/`$(...)` safety), `powershell_combined_{inp,
      out}.ps1` (§3.1–§3.6 + string/comment/here-string safety). Expected
      outs generated from the live JAR and re-checked for idempotency
      before registration. Active in `Makefile` `INP_FILES` immediately
      before the curly-GDR local fixtures (still ahead of `Real-code
      regressions:`); documented under a new "Makefile / Bash / PowerShell
      (STYLE_TOOLING.md)" heading in `test/README.txt`.
- [x] Source dogfood corpus candidates for Bash and PowerShell (real
      shell scripts / real `.ps1` scripts) once local fixtures pass;
      register in `STATE_DOGFOOD.md`. Makefile corpus candidates: real
      `Makefile`s from existing dogfood repos already cloned for other
      jobs may be reusable — check before cloning anything new.
      Sourced 2026-08-07 after a `/tmp` walk of existing dogfood checkouts;
      missing repos cloned same day (git 1.8 has no partial-clone/
      sparse-checkout — large trees used selective raw download of
      `*.ps1`/`*.psm1` or clone+strip). User aborted the last oversized pull
      (`azure-pipelines-tasks`); remaining not fetched. All rows stay
      `NOT STARTED` in `STATE_DOGFOOD.md` until a real dogfood *run*.

      **Available under `/tmp` now (dir name = repo basename convention):**
      - Makefile: `/tmp/frozen` (prior), `/tmp/fmt`, `/tmp/PEGTL`
      - Bash: `/tmp/javaparser_gdr`, `/tmp/jenkins_scope`,
        `/tmp/wordpress-develop` (prior); `/tmp/nvm`, `/tmp/acme.sh`,
        `/tmp/ohmyzsh` (17 `*.sh` after strip)
      - PowerShell: `/tmp/PSScriptAnalyzer` (full shallow),
        `/tmp/PowerShell` (505 `*.ps1`/`*.psm1` selective),
        `/tmp/runner-images` (247 `*.ps1`/`*.psm1` selective)

      **Skipped / not usable this session:**
      - `microsoft/azure-pipelines-tasks` — download aborted mid-way
        (~567/1145); partial tree removed
      - `ericniebler/range-v3` — `/tmp/range-v3` present but empty of
        Make files (broken/incomplete prior checkout); Makefile-only
        restore not completed
      - `python/cpython` — `/tmp/cpython` present but Make-sparse;
        Makefile-only restore not completed

      No dogfood *run* yet — listing + materialize only.
- [x] Run a real-code dogfood pass, one language at a time (Makefile, then
      Bash, then PowerShell).
      **Makefile — DONE.** Batched `/tmp/PEGTL/Makefile`,
      `/tmp/frozen/tests/Makefile`, `/tmp/frozen/benchmarks/Makefile`, and
      `/tmp/fmt/support/Android.mk` (211 lines total) through round1/round2:
      `diff -ru` empty (idempotent). Spot-checked round1 with
      `make -n -f <file>`; exit codes differed from originals only because
      this run copies just the `Makefile`/`*.mk` (not its sibling source
      tree) into a different relative path — confirmed the failures are
      "No such file"/"no rule" for sibling sources and `-std=c++20` rejected
      by this sandbox's old g++, same failure class as the unmodified
      originals run from their real location. No formatter-induced
      breakage. Content diff showed only the intended §1.1-§1.4 +
      RDD_KEY_261 transforms; no bug found. See `STATE_DOGFOOD.md` for
      per-repo rows.
      **Bash — DONE.** Ran `nvm-sh/nvm`'s 5 `.sh` files (5766 lines:
      `nvm.sh`, `install.sh`, `test/common.sh`, `rename_test.sh`,
      `update_test_mocks.sh`) through round1/round2: `diff -ru` empty
      (idempotent), `bash -n` clean on every round1 file same as originals.
      `install.sh`'s diff confirmed the brace-depth body reindent (§2.3
      note "byproduct of brace-depth counting") only tracks literal `{`/`}`
      chars, not `if`/`then`/`else`/`fi` keywords — a function body
      containing an `if`/`else`/`fi` block renders at one flat indent
      level, matching PowerShell §3.1's documented "naive" (not
      context-aware) brace-depth semantics. Already-decided scope, not a
      new bug. See `STATE_DOGFOOD.md` for the row.
      **PowerShell — DONE, 2 bugs found and fixed.** Ran all 228
      `.ps1`/`.psm1` files (24151 lines) under `PowerShell/PSScriptAnalyzer`
      through round1/round2; first pass found a non-empty diff (real
      idempotency bug), so implementation stopped and investigated first
      per protocol. Root causes
      (`src/com/jxmake/formatter/rules/PowerShellSpecificRule.java`):
      (1) `applySwitchArmAlignment`'s `parseArm` scans a line for its first
      depth-0 `{` and treats everything before it as a switch-arm pattern
      whenever the prefix isn't a control-flow header, with no check for a
      depth-0 `|` in that prefix — a still-unsplit pipeline line
      (`$x = ... | Where-Object {...}`, before `applyPipelineSplit` runs)
      read as one giant arm pattern, while the *same* line post-split (a
      bare `Where-Object {...}` continuation) did not. Since `format()`'s
      original order ran both alignment passes *before*
      `applyPipelineSplit`, round1 (fresh pipeline) and round2 (already
      split) fed different shapes into the same passes, producing
      different padding each round. Fixed two ways: `parseArm` now rejects
      any line with a depth-0 `|` before the `{`; `format()`'s pass order
      moved `applyPipelineSplit` ahead of `applyAssignAlignment`/
      `applySwitchArmAlignment` so both always see an already-split, stable
      shape. (2) `applyOperatorSpacing` treated bare `/` (not just `/=`) as
      binary division with no awareness of PowerShell's command-argument
      parsing mode — real bareword paths/URLs as command arguments
      (`Copy-Item -Force $profileDir/* $targetProfileDir`,
      `-LiteralPath $ruleDocDirectory/README.md`, an unquoted
      `https://api.nuget.org/v3/index.json`) got corrupted into extra,
      wrongly-split arguments — real content corruption, not just a style
      nit. Fixed by dropping bare `/` from the binary-operator set entirely
      (kept unambiguous `/=`); corpus-wide re-scan after the fix found zero
      remaining corruption and zero genuine-division use going unspaced.
      After both fixes: round1/round2 diff empty across all 228 files,
      `make test` 252/252 forward + idempotency (was 251/251 before this
      session — new fixture added). No PowerShell interpreter/
      `Invoke-ScriptAnalyzer`/`PSParser` available in this sandbox
      (confirmed via `which pwsh`/`which powershell`, both absent), so
      validity relied on round1/round2 idempotency plus manual reading of
      representative diffs (`build.psm1`, `AvoidOneChar.tests.ps1`,
      `RuleDocumentation.tests.ps1`) — no corpus-scale automated syntax
      check was possible; STYLE_TOOLING.md's "availability unconfirmed"
      caveat now resolved as **not available**. New permanent fixture pair
      `test/real_code_regressions_182_{inp,out}.ps1` (registered in
      `Makefile` `INP_FILES` and `test/README.txt`) reproduces both bugs
      minimally, distilled from the three real files above.
- [x] Implement comment normalization for Makefile/Bash/PowerShell
      (RDD_KEY_261) -- previously untouched entirely (STYLE_TOOLING.md §0).
      New shared `ToolingCommentNormalizer` (first-letter capitalization +
      sole-trailing-period stripping, no classifier/GRU dependency) wired
      into all three via the existing global `normalize-comment-start-case`/
      `normalize-comment-end-period` config keys (no new keys). Bash's
      `BashSpecificRule` gets an additional `NO_CAPITALIZE_TOOLS`
      word-exception list (~30 common Unix tool names: grep, awk, sed, head,
      tail, etc.) so a comment opening with a bare tool name stays lowercase
      — Makefile/PowerShell get plain cap only (user-confirmed, RDD_KEY_261).
      `test/{bash,makefile,powershell}_combined_out.*` regenerated from the
      live JAR and reverified idempotent. `make test` clean: 251/251
      forward + idempotency.
- [x] Update `CLAUDE.md`'s implementation-status paragraph, `README.md`,
      `../README.txt` once any of the three moves from scaffold to real
      logic (do not update ahead of actual landed code — see
      `STATE_COMMON.md`'s doc-sync convention).
      `README.md`/`../README.txt`: Makefile/Bash/PowerShell as
      JAR-implemented (`--lang`/`lang=` values, extension/basename mapping,
      `STYLE_TOOLING.md` style-guide link, AI full-file fallback +
      layout-judgment exclusion, shell/`reformat_file.py` dispatch).
      `CLAUDE.md`: job table row marked implemented; Current implementation
      status names `FormatterMakefile`/`FormatterBash`/`FormatterPowerShell`
      (+ rule classes); canonical-order note now requires the three in
      every current-capability list (no longer "reserved/not-yet").
- [x] Implement `#`-comment chain-grouping (RDD_KEY_267, 2026-08-08 brief
      decision #3, curly-parity companion to RDD_KEY_265/RDD_KEY_266).
      `MakefileSpecificRule.parseBlock`'s `COMMENT_LINE` branch now scans
      forward collecting every immediately-following comment line before
      normalizing the whole run via `ToolingCommentNormalizer.normalizeChain`
      (was: normalized each comment line independently). Bash/PowerShell
      (`runPassA`, character-level tokenizers) needed a deferred-placeholder
      approach since they normalize+emit inline as each comment's closing
      newline is hit, with no lookahead: a new `Frame.standalone`/`lineNo`
      pair (computed when a `#` frame is pushed) drives a new path where a
      standalone comment's raw body is stored in a `ChainEntry` list behind
      a unique placeholder marker emitted in its place; a trailing
      (non-standalone) comment still normalizes immediately, never deferred.
      After the pass-A scan, `resolveChainEntries` groups entries into
      chains wherever `lineNo` is strictly consecutive (any gap -- blank
      line, code line, or an intervening trailing comment -- breaks the
      chain), normalizes each chain, and substitutes placeholders for final
      text. Found and fixed a real latent bug in RDD_KEY_261's original
      per-comment-only logic in the process: existing fixtures'
      `# Copyright (C) 2024 Example Corp.` line (part of a 4-line standalone
      block also containing a period-free SPDX line and two blank `#`
      lines) had its trailing `.` wrongly stripped because each line's own
      period count was checked in isolation; chain-grouped, the strip step
      only ever touches the chain's *last* comment (the trailing blank
      line, which has no `.` to strip), so the mid-chain period now
      correctly survives -- exactly curly's own semantics.
      `makefile_combined_out.mk`, `bash_combined_out.sh`,
      `powershell_combined_out.ps1`, `real_code_regressions_182_out.ps1`
      regenerated from the live JAR to reflect the fix (user regenerated/
      verified). `make test`: 257/257 forward + idempotency, zero
      regressions (no new fixtures needed this round). Closes out decision
      #3 of the 2026-08-08 brief.
