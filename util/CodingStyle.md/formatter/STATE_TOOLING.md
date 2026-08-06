# STATE_TOOLING.md — Build/Dev-Tooling Script Formatter Tracker (Makefile, Bash, PowerShell)

Read `STATE_COMMON.md` first — shared commit/ambiguity/testing conventions
this file assumes. No other job's `STATE_*.md` is required reading.
Dogfood corpus status: see `STATE_DOGFOOD.md` (no rows yet — nothing in
this job has reached real-code testing).

---

## Purpose

Tracks three separate, narrow, beautification-only formatters — Makefile,
Bash, and PowerShell — per `STYLE_TOOLING.md`. Grouped into one job because
none of the three is large enough to warrant its own file (unlike, say,
Kotlin or JS/TS), the same way `STATE_DATA_FORMATS.md` groups seven data
formats.

**Makefile implemented; Bash and PowerShell not yet.** Makefile landed real
logic (`Lang.isMakefile`, `FormatterMakefile`, `MakefileSpecificRule`) —
see the checklist below. `Lang.SCAFFOLD_ONLY_LANGUAGES` remains unaffected
(still empty) since Makefile is a fully-supported language now, not a
scaffold entry — it was added directly to `Lang.SUPPORTED_LANGUAGES`. Bash
and PowerShell aren't recognized languages at all yet (no `Lang.infer`
extension, no CLI selector) until their own checklist items land.

**Canonical language order** for any documentation/help-string/`--lang`
enumeration this job's languages join, once implemented, is recorded in
`CLAUDE.md` (search "Canonical language order") — `makefile`, `bash`,
`powershell` come last, after `python3`. Do not add them to any list
asserting current capability (`README.md`'s `--lang` values, `.ext →`
mapping, `../README.txt`'s implemented-languages list) until real logic
actually lands for the language in question — see this file's own
doc-sync checklist item.

---

## Scope

Each of the three languages has its own section in `STYLE_TOOLING.md`
(§1 Makefile, §2 Bash, §3 PowerShell). Common thread across all three,
distinguishing this job from every other language job in this repo: a
short **fixed list** of specific transforms, with an explicit "leave
everything else byte-identical" rule — there is no general-purpose
reindentation/re-wrapping fallback the way there is for curly-brace or
data-format languages. Getting the "don't touch anything else" boundary
right (via a real tokenizer per language, not naive text substitution) is
the main implementation risk for Bash and PowerShell; Makefile is
line-oriented and doesn't need a tokenizer beyond distinguishing
tab-prefixed recipe lines from everything else.

Relative difficulty (see conversation history that scoped this job):
Makefile easiest (pure line/regex work); Bash and PowerShell roughly
comparable to each other, each needing a small real tokenizer (quoting,
heredocs/here-strings, comments) so the fixed-rule passes never fire
inside a string/comment/heredoc — bounded scope, not a full grammar.

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

---

## Config

No config keys yet — nothing implemented. Each language will likely need
at minimum an enable/disable gate before landing real logic (mirrors how
other high-risk/new-scope work in this codebase ships default-off behind
a flag, e.g. `curly-general-scope-reindent`, `html5-tc-gap-level`) —
confirm with the user whether that precedent should apply here, don't
assume.

## Test Fixtures (Local)

None yet.

## Test Fixtures (External, corpus-scale)

None yet. No dogfood candidates identified.

## Tools/compiler used

None yet. Candidates to evaluate once implementation starts: `shellcheck`
(Bash syntax validation, not formatting comparison), `make -n`/`make -q`
(Makefile syntax sanity), PowerShell's own parser (`[System.Management.
Automation.PSParser]::Tokenize` or `Invoke-ScriptAnalyzer` if available in
this sandbox — availability unconfirmed, check before relying on it).

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
      `FormatterMakefile`-style), and `BashSpecificRule`: a two-pass design
      -- pass A is a character-level state-machine tokenizer (quotes,
      `$'...'`, backticks, `$(...)`, `$((...))`, `#` comments, `<<`/`<<-`
      heredocs incl. quoted/bareword delimiters) that also applies the two
      token-level rules (§2.2, §2.5) inline via a `RunBuffer` that flushes
      on every kind change; pass B is line-oriented (applies §2.1/§2.3/§2.4)
      guarded by a per-line purity flag (first non-whitespace char is code).
      Arithmetic nested inside a double-quoted string is still processed
      (per the STYLE_TOOLING.md §2.5 example) but arithmetic nested inside
      `$(...)`/backticks stays opaque, consistent with leaving nested
      command-substitution content untouched entirely. Smoke-tested
      manually against the STYLE_TOOLING.md §2 combined example (all 5
      rules verified matching spec output + idempotent), a pipe-in-string/
      comment safety test, and a heredoc/backtick/`$(...)` safety test
      (heredoc body, backtick, and `$(...)` content all left byte-identical
      + idempotent). Full `make test` suite re-run clean after landing:
      248/248 forward, 248/248 idempotency -- purely additive, no other
      language's path touched. No local test fixture pair registered yet
      (deferred at user's explicit request -- separate unchecked item
      below).
- [ ] PowerShell: build/extend a tokenizer sufficient to safely skip
      string literals, here-strings, and comments.
- [ ] Implement PowerShell §3.1 brace-depth indentation.
- [ ] Implement PowerShell §3.2 operator spacing + `=` alignment.
- [ ] Implement PowerShell §3.3 pipeline split/align.
- [ ] Implement PowerShell §3.4 hashtable spacing.
- [ ] Implement PowerShell §3.5 `switch` formatting.
- [ ] Implement PowerShell §3.6 `{`/`}` spacing.
- [ ] Remove all `RDD*` references from `STYLE_TOOLING.md`. A style file
      must nor reference implementation states.
- [ ] Author local test fixture pairs per each language's rule set,
      register in `test/README.txt` / Makefile `INP_FILES` before
      the regression fixtures.
- [ ] Source dogfood corpus candidates for Bash and PowerShell (real
      shell scripts / real `.ps1` scripts) once local fixtures pass;
      register in `STATE_DOGFOOD.md`. Makefile corpus candidates: real
      `Makefile`s from existing dogfood repos already cloned for other
      jobs may be reusable — check before cloning anything new.
- [ ] Update `CLAUDE.md`'s implementation-status paragraph, `README.md`,
      `../README.txt` once any of the three moves from scaffold to real
      logic (do not update ahead of actual landed code — see
      `STATE_COMMON.md`'s doc-sync convention).
