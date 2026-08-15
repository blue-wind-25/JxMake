# STATE_TOOLING.md — Build/Dev-Tooling Script Formatter Tracker (Makefile, Bash, PowerShell)

Read `STATE_COMMON.md` first — shared commit/ambiguity/testing conventions
this file assumes; no other job's `STATE_*.md` is required. Dogfood corpus
status: see `STATE_DOGFOOD.md` (candidates registered `NOT STARTED`; no
real-code run yet).

---

## Purpose

Tracks three narrow, beautification-only formatters — Makefile, Bash, and
PowerShell — per `STYLE_TOOLING.md`. Grouped into one job since none is
large enough to warrant its own file (unlike Kotlin or JS/TS), same as
`STATE_DATA_FORMATS.md` grouping seven data formats.

**All three implemented:** Makefile (`Lang.isMakefile`, `FormatterMakefile`,
`MakefileSpecificRule`); Bash (`Lang.isBash`, `FormatterBash`,
`BashSpecificRule`, all five §2 rules); PowerShell (`Lang.isPowerShell`,
`.ps1`/`.psm1` infer, `FormatterPowerShell`, `PowerShellSpecificRule`, all
six §3 rules) — see checklist below. `Lang.SCAFFOLD_ONLY_LANGUAGES` stays
empty; each was added directly to `Lang.SUPPORTED_LANGUAGES` as it landed.

**Canonical language order** for any documentation/help-string/`--lang`
enumeration is recorded in `CLAUDE.md` (search "Canonical language order")
— `makefile`, `bash`, `powershell` come last, after `python3`. `README.md`,
`../README.txt`, and `CLAUDE.md` all list the three as JAR-implemented
(doc-sync checklist item below).

---

## Scope

Each language has its own `STYLE_TOOLING.md` section (§1 Makefile, §2 Bash,
§3 PowerShell). What distinguishes this job from every other language job:
a short **fixed list** of specific transforms plus an explicit "leave
everything else byte-identical" rule — no general-purpose
reindentation/re-wrapping fallback like curly-brace or data-format languages
have. The main risk is getting that "don't touch anything else" boundary
right via a real tokenizer per language, not naive text substitution.
Relative difficulty: Makefile is easiest (line-oriented, only needs to
distinguish tab-prefixed recipe lines from everything else, no tokenizer
needed); Bash and PowerShell are comparable, each needing a small real
tokenizer (quoting, heredocs/here-strings, comments) so fixed-rule passes
never fire inside a string/comment/heredoc — bounded scope, not a full
grammar.

Several `STYLE_TOOLING.md` open questions (marked inline, "resolve via RDD
before implementing") must be resolved before implementing the affected
rule — do not guess a default and implement against it.

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
| RDD_KEY_267 | `#`-comment chain-grouping (2026-08-08 brief #3, curly/RDD_KEY_265/RDD_KEY_266 parity): consecutive standalone `#` comment lines now normalize as one unit via `ToolingCommentNormalizer.normalizeChain` instead of independently; fixed a latent bug stripping a mid-chain sole `.` not on the chain's last comment |
| RDD_KEY_272 | Pure refactor: RDD_KEY_267's deferred-placeholder chain mechanism (duplicated in `BashSpecificRule`/`PowerShellSpecificRule`) extracted into shared `ToolingCommentNormalizer.ChainCollector` (both files hold one field instead of separate `ChainEntry`/`resolveChainEntries` copies); Makefile untouched (simple lookahead, no deferred mechanism). Byte-identical output, `make test` 261/261 unchanged |
| RDD_KEY_296 | PowerShell §3.1 `applyBraceIndent` bug fix (dogfood against `util/JCS/*.ps1`): scope depth generalized from brace-only to also track `(`/`[`/`)`/`]` (fixes `param(...)`-block content flushed to enclosing brace depth); new backtick-continuation-aware indent bump (`lineEndsWithBacktick`) fixes inconsistent reindent of `` ` ``-continued lines. New fixture `real_code_regressions_210` |

---

## Config

No language-specific config keys — the five/six fixed-rule-list transforms
per language are unconditional (mirrors the "no gate" precedent already set
by the rest of this job; none of RDD_KEY_254–RDD_KEY_258 asked for one).
Comment normalization (RDD_KEY_261) reuses the existing global
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
Makefile §1.1 and PowerShell §3.2 instances of the same alignment-group
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
      Smoke-tested manually (diff, idempotency, `--lang makefile`,
      extensionless `Makefile`, `.mk` detection). **STALE, 2026-08-10**:
      "no local test fixture pair registered yet" — not needed, already done
      via the "Author local test fixture pairs" item below
      (`makefile_combined_{inp,out}.mk`, `[x]`). Comments remain untouched
      (out of scope, STYLE_TOOLING.md §0).
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
      double-quoted string is still processed (§2.5); nested inside
      `$(...)`/backticks it stays opaque, consistent with leaving
      command-substitution content untouched. Smoke-tested manually (§2
      combined example, pipe-in-string/comment safety,
      heredoc/backtick/`$(...)` safety — byte-identical + idempotent). `make
      test` clean: 248/248 forward, 248/248 idempotency — purely additive.
      **STALE, 2026-08-10**: "no local fixture pair yet" — done
      (`bash_combined_{inp,out}.sh`, `[x]` below).
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
      248/248 forward, 248/248 idempotency — purely additive. **STALE,
      2026-08-10**: "no local fixture pair yet" — done
      (`powershell_combined_{inp,out}.ps1`, `[x]` below).
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
      sparse-checkout, so large trees used selective raw download of
      `*.ps1`/`*.psm1` or clone+strip). User aborted the last oversized pull
      (`azure-pipelines-tasks`); rest not fetched. All rows stay
      `NOT STARTED` in `STATE_DOGFOOD.md` until a real dogfood *run*.

      **Available under `/tmp` now (dir name = repo basename convention):**
      - Makefile: `/tmp/frozen` (prior), `/tmp/fmt`, `/tmp/PEGTL`
      - Bash: `/tmp/javaparser_gdr`, `/tmp/jenkins_scope`,
        `/tmp/wordpress-develop` (prior); `/tmp/nvm`, `/tmp/acme.sh`,
        `/tmp/ohmyzsh` (17 `*.sh` after strip)
      - PowerShell: `/tmp/PSScriptAnalyzer` (full shallow),
        `/tmp/PowerShell` (505 `*.ps1`/`*.psm1` selective),
        `/tmp/runner-images` (247 `*.ps1`/`*.psm1` selective)

      **2026-08-07 skip list, since resolved (2026-08-09):** at materialize time,
      `microsoft/azure-pipelines-tasks` (download aborted mid-way, ~567/1145),
      `ericniebler/range-v3` (`/tmp/range-v3` present but empty of Make files --
      broken/incomplete prior checkout), and `python/cpython` (`/tmp/cpython`
      present but Make-sparse) were all left un-dogfooded. All three were
      re-cloned fresh and actually run 2026-08-09 -- see the dogfood-pass
      writeups below and `STATE_DOGFOOD.md` for final per-repo status
      (`azure-pipelines-tasks`: DONE - OPEN Q, new Tier 4 gap found;
      `range-v3`: DONE, genuinely has zero Make files; `cpython`: DONE, clean).

      No dogfood *run* yet — listing + materialize only.
- [x] Run a real-code dogfood pass, one language at a time (Makefile, then
      Bash, then PowerShell).
      **Bash — DONE, 4 bugs found and fixed (3 idempotency, 1 syntax
      corruption).** Batched 5 corpora already materialized from prior
      sessions through round1/round2: `javaparser/javaparser`
      (`/tmp/javaparser_gdr`, 7 `.sh`), `jenkinsci/jenkins`
      (`/tmp/jenkins_scope`, 3 `.sh`), `wordpress/wordpress-develop`
      (`/tmp/wordpress-develop`, 3 `.sh`), `acmesh-official/acme.sh`
      (`/tmp/acme.sh`, full shallow clone, 276 `.sh`), `ohmyzsh/ohmyzsh`
      (`/tmp/ohmyzsh`, stripped to 17 `.sh`/`.bash`). First four came back
      clean (idempotent, `bash -n` matching originals); `ohmyzsh` had a
      non-empty round1/round2 diff, bisected to a minimal repro
      (evidence-over-reasoning). Four independent root causes, all in
      `src/com/jxmake/formatter/rules/BashSpecificRule.java`:
      (1) `emitCaseBody`'s case-arm boundary regex (`CASE_ARM`) found the
      pattern's terminating `)` via first-match with no backslash-escape
      awareness, so an escaped paren pair like `\(\))` had its *escaped* `)`
      mistaken for the real terminator, splitting the arm mid-pattern —
      fixed with a char-by-char `matchCaseArm` scan that skips
      `\`-escaped characters.
      (2) `runPassA`'s root/code-mode tokenizer had no backslash-escape
      handling, so a `\'` case-arm pattern (e.g. `\'*)`) fell through to the
      plain `'` branch, opening a real single-quote string frame that stayed
      open until an unrelated later `'` closed it, corrupting brace-depth
      indentation downstream (visible only as a round1/round2 shape
      difference) — fixed by adding a root-context `c == '\\'` branch
      (mirrors existing escape handling in the `D`/`Q`/`B` frame types) that
      consumes the backslash and next character as plain code before any
      quote-opening check runs.
      (3) `emitCaseBody` had no concept of a nested `case ... in` as an
      outer arm's body — a nested case's own terminating `esac` was only
      recognized as exactly `esac`, so a combined `esac ;;` line (closing
      both the nested case and the enclosing arm) fell through to the
      generic body-line fallback, corrupting indentation from there on —
      fixed by splitting `emitCaseBody` into a wrapper plus recursive
      `emitCaseBodyInner` (new `CaseBodyEnd` result tracking next index +
      whether the terminator closed an enclosing arm); terminator check now
      accepts `esac`, `esac ;;`, or `esac;;`.
      (4) Surfaced via `tools/verifiers/bash_syntax_check.sh` after fixes
      1-2 (not idempotency — `plugins/wd/wd.sh` parsed clean originally but
      its round1 output did not): `pipeSpacing`'s (§2.2) lone-`|` detector
      excluded `||`/`|&` but not the noclobber-override redirect `>|`
      (`cmd >| file`), splitting it into `> |`, a genuine `bash -n` syntax
      error — fixed by also excluding a `|` immediately preceded by `>`.
      After all four fixes: round1/round2 diff empty across all 5 corpora;
      `bash -n` on `ohmyzsh` shows the same 10 pre-existing error lines on
      both original and round1 (5 files use zsh-only syntax under a
      `.sh`/`.bash` extension — extended-glob alternation, `${(kv)...}`,
      `always {}` blocks — already invalid bash before formatting, out of
      scope). **Known accepted gap, not fixed:** one of those already-
      invalid-under-bash files (`tools/upgrade.sh`) also has `pipeSpacing`
      insert a space inside a zsh extended-glob alternation indistinguishable
      from a real pipe (`(|.git)` -> `( | .git)`) — not a new breakage class,
      since the file already failed `bash -n` before formatting;
      dialect-detecting `.sh`-extension-but-actually-zsh content is out of
      scope (same "no general grammar, fixed transform list" boundary as
      every other accepted gap here). **Disposition (2026-08-10):** documented
      in `README.md`'s Known Limitations → new "Build/dev-tooling scripts
      (Makefile/Bash/PowerShell)" family section, removed from `XL.txt`
      TIER 9 (permanent, not a live TODO). `make test`: 267/267 forward +
      idempotency (was 264/264 -- 3 new fixtures: `real_code_regressions_188`-
      `190`). See `STATE_DOGFOOD.md` for per-repo rows.
      **PowerShell — DONE, 1 bug found and fixed.** User ran round1/round2
      manually (`--preserve-tree`) on `PowerShell/PowerShell`
      (`/tmp/PowerShell`, 505 `*.ps1`/`*.psm1`) and `actions/runner-images`
      (`/tmp/runner-images`, 247 `*.ps1`/`*.psm1`) 2026-08-09.
      `runner-images` came back with an empty `diff -r`. `PowerShell/
      PowerShell` had one non-empty diff: round1 had `("a").ForEach( { $_ })`,
      round2 turned it into `("a").ForEach ( { $_ })` (spurious space before
      `(`). Root cause: `PowerShellSpecificRule.KEYWORD_PAREN`'s
      case-insensitive `foreach` match used a negative lookbehind that only
      excluded preceding word chars (`(?<![A-Za-z0-9_])`), so `.ForEach(`
      (preceded by `.`) was misdetected as the `foreach` keyword. Fixed by
      adding `.` to the lookbehind's exclusion set. Fixture
      `real_code_regressions_191` (nested `.ForEach( { ... })` inside a
      pipeline) confirms both the no-space-inserted output and
      round1/round2 idempotency. `make test`: 268/268 forward +
      idempotency (was 267/267). See `STATE_DOGFOOD.md` for per-repo rows.
      **PowerShell — `microsoft/azure-pipelines-tasks`, DONE - FIXED (2026-08-09,
      follow-up session).** Root cause: `runPassA`'s returned `PassAResult.kind`
      array was sized/indexed to the *original* `content` string's positions,
      but every consumer (`applyBraceIndent`, `applyOperatorSpacing`,
      `applyPipelineSplit`, `applyAssignAlignment`, `applySwitchArmAlignment`,
      `applyKeywordParenSpacing`/`KEYWORD_PAREN`, `applyBraceSpacing` — all
      seven, confirmed via `grep passA.kind`) reads it against
      `passA.transformed`, which diverges in length from `content` once a
      standalone `#` comment's `ChainCollector.defer()` placeholder is
      substituted for a different-length final comment text. Fixed by having
      `RunBuffer` accumulate a parallel `kindOut` string in lockstep with its
      own `out` (new `kindResult()`, appended per real output character on
      every `flush()`), so `kind` is built aligned to `RunBuffer`'s actual
      emitted output, not re-derived from `content` positions. Remaining gap:
      the placeholder-substitution step. `ChainCollector.resolve()`'s
      textual `String.replace(placeholder, finalText)` on `transformed`
      can't be reused for the kind string (all `'C'`/`'O'` characters, never
      literally containing the placeholder marker text), so a companion
      `ChainCollector.resolveKind(preResolveTransformed, preResolveKind)`
      locates each placeholder's position in the pre-substitution
      `transformed` string via `indexOf`, then splices a run of `'O'` of the
      same length as that entry's resolved final text (`resolve()` now
      records `resolvedLength` per entry) into the kind string at the
      matching offset, keeping `kind` aligned with `resolve()`'s return
      value; `resolveKind()` must run after `resolve()`. Verified via a
      minimal repro (standalone `#` comment followed by `if($x -eq $null)`),
      the original `Tasks/Common/VstsAzureHelpers_/Utility.ps1` (diff now
      empty), and the full corpus (all 1123 `.ps1` files under
      `/tmp/azure-pipelines-tasks`, diff empty, zero formatter errors). New
      permanent fixture `test/real_code_regressions_192_{inp,out}.ps1`
      (registered in `Makefile` `INP_FILES` and `test/README.txt`). `make
      test`: 269/269 forward + idempotency (was 268/268 before this fix).
      **Accepted loose end:** the original content-indexed `char[] kind`
      local inside `runPassA` is now dead write-only code (every
      `kind[i] = ...` assignment is never read after this fix) — left in
      place rather than stripped (~50 scattered dead-store lines judged
      higher-risk to remove than to leave); a future cleanup pass may strip
      it. See `STATE_DOGFOOD.md`'s `microsoft/azure-pipelines-tasks` row
      (updated from "OPEN Q" to fixed).
      **Resolved, 2026-08-11 (Tier1 cleanup):** the dead-write-only
      `char[] kind` local was removed from `runPassA` entirely — the
      declaration plus all ~32 now-unread `kind[i] = ...`/
      `kind[i] = kind[i + 1] = ...` assignments (verified via `grep -n
      "kind\["` that every one fell inside `runPassA`'s own body, and that
      `PassAResult.kind` is built solely from `chainCollector.resolveKind(...)`,
      never from this local — every write was genuinely unread). Every other
      `kind[...]` reference in the file (`computeLinePurity`,
      `applyOperatorSpacing`, `applyPipelineSplit`, etc.) reads a *different*
      `kind`/`char[]` — either a method parameter or `passA.kind` — and was
      left untouched. Pure deletion, no behavior change: `make test`
      278/278 forward + idempotency (unchanged pass count); `powershell_combined_{inp,out}.ps1`
      and `real_code_regressions_{182,191,192}_out.ps1` all reformat
      byte-identical to their committed `_out` fixtures before and after.
      **Makefile — DONE.** Batched `/tmp/PEGTL/Makefile`,
      `/tmp/frozen/tests/Makefile`, `/tmp/frozen/benchmarks/Makefile`, and
      `/tmp/fmt/support/Android.mk` (211 lines total) through round1/round2:
      `diff -ru` empty (idempotent). Spot-checked round1 with
      `make -n -f <file>`; exit codes differed from originals only because
      this run copies just the `Makefile`/`*.mk` (not its sibling source
      tree) into a different relative path — the failures are "No such
      file"/"no rule" for sibling sources and `-std=c++20` rejected by this
      sandbox's old g++, same failure class as the unmodified originals run
      from their real location. No formatter-induced breakage; content diff
      showed only the intended §1.1-§1.4 + RDD_KEY_261 transforms; no bug
      found. See `STATE_DOGFOOD.md` for per-repo rows.
      **Makefile — `ericniebler/range-v3` + `python/cpython`, DONE (2026-08-09,
      fresh re-clones after the 2026-08-07 checkouts were found
      broken/incomplete).** `range-v3` genuinely has zero `Makefile`/
      `makefile`/`*.mk` files anywhere in the tree (header-only, CMake-built) —
      confirmed via `find`; closed as DONE (nothing to dogfood) rather than
      left NOT STARTED. `cpython` does have real Make files; round1/round2
      `diff -r` empty (idempotent), no bug found.
      **Bash — DONE.** Ran `nvm-sh/nvm`'s 5 `.sh` files (5766 lines:
      `nvm.sh`, `install.sh`, `test/common.sh`, `rename_test.sh`,
      `update_test_mocks.sh`) through round1/round2: `diff -ru` empty
      (idempotent), `bash -n` clean on every round1 file same as originals.
      `install.sh`'s diff confirmed the brace-depth body reindent (§2.3
      note "byproduct of brace-depth counting") only tracks literal `{`/`}`
      chars, not `if`/`then`/`else`/`fi` keywords — an `if`/`else`/`fi`
      block inside a function body renders at one flat indent level,
      matching PowerShell §3.1's documented "naive" (not context-aware)
      brace-depth semantics. Already-decided scope, not a new bug. See
      `STATE_DOGFOOD.md` for the row.
      **PowerShell — DONE, 2 bugs found and fixed.** Ran all 228
      `.ps1`/`.psm1` files (24151 lines) under `PowerShell/PSScriptAnalyzer`
      through round1/round2; found a non-empty diff (real idempotency bug),
      investigated first per protocol. Root causes
      (`src/com/jxmake/formatter/rules/PowerShellSpecificRule.java`):
      (1) `applySwitchArmAlignment`'s `parseArm` scans a line for its first
      depth-0 `{` and treats everything before it as a switch-arm pattern
      whenever the prefix isn't a control-flow header, with no check for a
      depth-0 `|` in that prefix — a still-unsplit pipeline line
      (`$x = ... | Where-Object {...}`, before `applyPipelineSplit` runs)
      read as one giant arm pattern, while the same line post-split (a bare
      `Where-Object {...}` continuation) did not, so round1 vs round2 fed
      different shapes into the same alignment passes. Fixed two ways:
      `parseArm` now rejects any line with a depth-0 `|` before the `{`;
      `format()`'s pass order moved `applyPipelineSplit` ahead of
      `applyAssignAlignment`/`applySwitchArmAlignment` so both always see an
      already-split, stable shape.
      (2) `applyOperatorSpacing` treated bare `/` (not just `/=`) as binary
      division, unaware of PowerShell's command-argument parsing mode — real
      bareword paths/URLs as command arguments (`Copy-Item -Force
      $profileDir/* $targetProfileDir`, an unquoted
      `https://api.nuget.org/v3/index.json`) got corrupted into extra,
      wrongly-split arguments — real content corruption, not a style nit.
      Fixed by dropping bare `/` from the binary-operator set entirely (kept
      unambiguous `/=`); corpus-wide re-scan found zero remaining corruption
      and zero genuine-division use going unspaced. After both fixes:
      round1/round2 diff empty across all 228 files, `make test` 252/252
      forward + idempotency (was 251/251). No PowerShell
      interpreter/`Invoke-ScriptAnalyzer`/`PSParser` available in this
      sandbox (`which pwsh`/`which powershell` both absent), so validity
      relied on round1/round2 idempotency plus manual reading of
      representative diffs — STYLE_TOOLING.md's "availability unconfirmed"
      caveat now resolved as **not available**. Fixture pair
      `test/real_code_regressions_182_{inp,out}.ps1` reproduces both bugs
      minimally, distilled from the three real files diffed above.
- [x] Implement comment normalization for Makefile/Bash/PowerShell
      (RDD_KEY_261) -- previously untouched (STYLE_TOOLING.md §0). New
      shared `ToolingCommentNormalizer` (first-letter capitalization +
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
      logic (not ahead of actual landed code — see `STATE_COMMON.md`'s
      doc-sync convention).
      `README.md`/`../README.txt`: Makefile/Bash/PowerShell now
      JAR-implemented (`--lang`/`lang=` values, extension/basename mapping,
      `STYLE_TOOLING.md` style-guide link, AI full-file fallback +
      layout-judgment exclusion, shell/`reformat_file.py` dispatch).
      `CLAUDE.md`: job table row marked implemented; current implementation
      status names `FormatterMakefile`/`FormatterBash`/`FormatterPowerShell`
      (+ rule classes); canonical-order note now requires the three in every
      current-capability list (no longer "reserved/not-yet").
- [x] Implement `#`-comment chain-grouping (RDD_KEY_267, 2026-08-08 brief
      decision #3, curly-parity companion to RDD_KEY_265/RDD_KEY_266).
      `MakefileSpecificRule.parseBlock`'s `COMMENT_LINE` branch now scans
      forward collecting every immediately-following comment line before
      normalizing the whole run via `ToolingCommentNormalizer.normalizeChain`
      (was: normalized each line independently). Bash/PowerShell
      (`runPassA`, character-level tokenizers) needed a deferred-placeholder
      approach since they normalize+emit inline with no lookahead: a new
      `Frame.standalone`/`lineNo` pair (set when a `#` frame is pushed)
      stores a standalone comment's raw body in a `ChainEntry` list behind a
      unique placeholder marker emitted in its place; a trailing
      (non-standalone) comment still normalizes immediately, never deferred.
      After the pass-A scan, `resolveChainEntries` groups entries into
      chains wherever `lineNo` is strictly consecutive (any gap — blank
      line, code line, or an intervening trailing comment — breaks the
      chain), normalizes each chain, and substitutes placeholders for final
      text. Found and fixed a real latent bug in RDD_KEY_261's original
      per-comment-only logic: existing fixtures' `# Copyright (C) 2024
      Example Corp.` line (part of a 4-line standalone block also
      containing a period-free SPDX line and two blank `#` lines) had its
      trailing `.` wrongly stripped because each line's own period count
      was checked in isolation; chain-grouped, the strip step only touches
      the chain's *last* comment (the trailing blank line, which has no `.`
      to strip), so the mid-chain period now correctly survives — exactly
      curly's own semantics. `makefile_combined_out.mk`,
      `bash_combined_out.sh`, `powershell_combined_out.ps1`,
      `real_code_regressions_182_out.ps1` regenerated from the live JAR
      (user regenerated/verified). `make test`: 257/257 forward +
      idempotency, zero regressions (no new fixtures needed). Closes
      decision #3 of the 2026-08-08 brief.
- [x] **PowerShell -- `util/JCS/*.ps1` (6 files), DONE, 1 bug found and
      fixed (RDD_KEY_296, 2026-08-15).** A prior dogfood pass over
      `util/JCS/*.ps1` flagged all 6 files with formatting diffs suggesting
      a real bug (`param(...)` block contents flushed to column 0; backtick
      line-continuations re-indented inconsistently) but left
      unfixed/unadopted (no content-diff verifier for PowerShell, round1
      discarded). Repro'd with a synthetic `.ps1` (indented `param(...)` + a
      backtick-continued statement) before touching code. Root cause:
      `PowerShellSpecificRule.applyBraceIndent` tracked scope depth via
      `{`/`}` only, so a multi-line paren/bracket-delimited construct (a
      `param(...)` list) had its interior flattened to the enclosing brace
      depth; backtick continuations had no depth signal at all. Fixed by
      generalizing `countLeadingCloses`/`countCodeChar` (now varargs) to
      also count `(`/`[`/`)`/`]`, and adding a `lineEndsWithBacktick`-driven
      `contLine` flag that bumps a continuation line's indent by one level,
      chaining across consecutive backtick-continued lines, reset on a
      blank or non-continuation line. New fixture
      `test/real_code_regressions_210_{inp,out}.ps1` (combined param-block +
      backtick-continuation repro). `make test`: 319/319 forward +
      idempotency (was 318/318). Verified against the real trigger files:
      all 6 `util/JCS/*.ps1` files reformatted, `param(...)` blocks and
      backtick continuations both confirmed correct, round1/round2 diff
      empty (idempotent) across all 6. Remaining diffs against originals are
      pre-existing, unrelated tooling-job behaviors (operator/`=` alignment,
      comment normalization) already covered by earlier RDD_KEYs -- not
      touched by this fix. See `RDD_KEY_296` for full narrative.
