# STATE_PYTHON3.md — Python 3 JAR Support Tracker

Read `STATE_COMMON.md` first (shared commit/ambiguity/testing conventions this
file assumes); `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` are NOT required
reading for this job. Dogfood corpus status: see `STATE_DOGFOOD.md`.

---

## Purpose

Tracks implementation of Python 3 support in the deterministic JAR formatter
(`util/CodingStyle.md/formatter/`), per `STYLE_PYTHON3.md`. Python's
imperative surface differs enough from every currently-supported
brace-delimited language (significant whitespace, several bracket-content
categories with no C-family analog) that most rules are new, not inherited.

**Status: fully implemented.** §1–§9 all landed, local fixtures active,
real-code dogfood done across all 5 listed test-fixture repos (see below).

---

## Scope

`STYLE_PYTHON3.md` covers latest supported Python 3 (currently 3.15+); Python
2 is unsupported. **Indentation note (from the style doc):** unlike every
other supported language, Python's indentation is semantically load-bearing —
the formatter must never change indentation depth in a way that alters block
membership. This constrains every rule below.

Sections:

1. Bracket complexity detector — extends the existing tight/loose heuristic
   (atoms/simple ops tight, call or nested bracket loose) with Python-only
   categories: comprehensions (always loose, own bucket, not
   atom/call/nested-bracket), slicing (`:` inside `[]` never padded,
   evaluated as its own sub-expression for the outer tight/loose call),
   star-unpacking (`*args`/`**kwargs`, tight, doesn't itself force
   looseness), dict-vs-set literal disambiguation (top-level `:` present →
   dict rules; none → set/list rules; empty `{}` → dict).
2. Assignment alignment — `=` (and augmented `+=`/`-=`/etc.) column-aligned
   across adjacent simple assignments, same group/break rules as the
   existing declaration-alignment mechanism; multi-line RHS continuation
   alignment target unchanged (break-before/after-operator).
3. Import ordering — deliberately simpler than PEP 8/`isort`'s
   stdlib/third-party/local tiers: **no such classification at all.** Sort
   key (import-before-from, then alphabetical), grouping (any non-import
   statement breaks the group, including nested-block imports forming their
   own separate group), `from __future__ import ...` promoted to the top of
   its own group. Config: `python-import-sort`, `python-import-blank-lines`.
4. Decorators — always own-line by grammar (no placement ambiguity), `()`
   content reuses §1's bracket rules, tight `@`; overflow wraps the call's
   argument list. `@property`/`@x.setter` get no special getter/setter-group
   alignment (unlike JS/TS/C-family) since Python function bodies never
   compact.
5. F-strings — expression portion inside `{...}` gets normal expression
   spacing; everything from `!conversion`/`:format_spec` onward is opaque,
   preserved exactly as written.
6. Function signature wrapping — existing inline-vs-one-per-line rule
   applies as-is; alignment target is `:` (and `=` when a default is
   present) rather than a type column, since Python's declaration order is
   `name: type = default`. Return-type arrow stays fixed on the closing `)`
   line.
7. Structural pattern matching (`match`/`case`) — derives from Java 17's
   switch-expression pattern matching by citation; always block-body shape
   (no single-line arrow form); `:` column alignment for a contiguous
   all-compact-form run of cases (all-or-nothing); or-patterns (`|`)
   ordinary binary-operator spacing; deconstruction/sequence/mapping
   patterns reuse §1's bracket rules.
8. Single-statement bodies — extends the existing single-expression-body
   compactness idea to `if`/`elif`/`else`/`while`/`for`/`case`; **never**
   applies to `def`/`class`/`try`-`except`-`finally`/`with` (always a full
   block); overflow expands to a normal indented block.
9. Control-flow blank lines — blank line before a function-scope `return`
   in a multi-line body (not nested); blank line before `elif`/`else` only
   when the preceding block's last statement is `return`/`break`/`continue`
   (not `raise`).

Scaffold dispatch lives in the shared `Lang.java`/`Main.java`/
`ServerMode.java`/`Config.java`, per the routing `CLAUDE.md` table.

---

## Resolved Design Decisions

Full text of each decision lives in `RDD_LOG.md` (shared with
`STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` — continue the existing `RDD_KEY_n`
numbering, do not restart). See `STATE_COMMON.md`'s lookup convention
(`grep -Fm1`, no `-A`).

| Key | Topic |
|---|---|
| RDD_KEY_184 | §1.4/§1.5 non-empty `{}` (dict/set) is always loose per §3.3, no unpacking-only carve-out; fixed stale tight example |
| RDD_KEY_186 | New §10 — triple-quoted docstrings/multiline strings are opaque, preserved verbatim beyond the opening line (extends §4's precedent) |
| RDD_KEY_237 | Indent-size/style conversion (Python analog of `MiscRuleCore#convertIndentation`) — granularity resolved per real statement line via the tokenizer's own INDENT/DEDENT depth, not per-block width-guessing; see "Indent-Size/Style Conversion" section below |
| RDD_KEY_247 | `python-import-sort`/`python-import-blank-lines` wired into `Config.java` (previously documented but not recognized keys); `python-import-blank-lines` given real new behavior (blank-line-count normalization between same-depth adjacent import groups separated only by blank lines) per coordinator decision after an initial ambiguity stop — see "Config Keys Wiring" section below |
| RDD_KEY_268 | `normalize-comment-start-case`/`normalize-comment-end-period` implemented from scratch for python3's `#` comments, plus chain-grouping — reuses the existing classifier/GRU-backed decision path (`MiscRuleCore#capitalizeFirstLetter`/`#stripSoleTrailingPeriodAcrossLines`/`#classifyComment` were already family-agnostic) rather than a parallel ad hoc mechanism — see "Comment Normalization" section below |
| RDD_KEY_282 | §9 gap-closing: 3 of 4 documented §9 gaps closed (multi-physical-line `def` header now recognized; semicolon-chained statements now recognized by both §9.1/§9.2 via `lineHasTopLevelReturnSegment`/`lastSemicolonSegmentStart`; a §8-compact preceding block now recognized by §9.2 via `classifyCompactSingleStatementHeaderColon` delegation); `try`/`except`/`finally` left as a scoped-out follow-up (STYLE_PYTHON3.md §9.2 names only `elif`/`else`) — see "§9 Gap Fixes" section below |
| RDD_KEY_287 | §8/§7 join-threshold non-idempotency root-caused and fixed: the join fits-check in `applySingleStatementBody`/`classifyCaseLine`/`flushCaseGroup` undercounted a retained body trailing comment's width (excluded via `trimEndIdx`'s gap-token treatment of `COMMENT_LINE`, even though the comment survives untouched past the join's own replacement span) — genuine non-convergent flip-flop, not cosmetic; confirmed pre-existing (not session-introduced) via A/B build from commit `9eb1bd4`; fixed by measuring `joined + trailingSuffix` at all 3 call sites — see "§8/§7 join-threshold non-idempotency" section below |
| RDD_KEY_288 | §8 compact-branch overflow-check non-idempotency root-caused and fixed (found via self-hosting dogfood on `tools/verifiers/{html,python,toml}_content_diff.py`'s hand-column-aligned `if`/`elif`/`else` chains): `applySingleStatementBody`'s already-compact branch measured the RAW verbatim single-physical-line text (including stale alignment padding between `:` and the body) against `lineLength`, instead of the normalized single-space form — a padded but actually-fitting `elif`/`else` was wrongly forced to expand on round1, then correctly re-joined on round2; genuine flip-flop, same bug family as RDD_KEY_287. Fixed by measuring `headerText + " " + bodyText` and collapsing stale padding to a single space even when no expansion is needed — see "§8 compact-branch overflow-check non-idempotency" section below |

---

## Config

- `python-import-sort` (on/off) — toggles §3.1's alphabetical sort.
- `python-import-blank-lines` — blank lines between import groups (mirrors
  `java-import-blank-lines`'s shape in `STYLE_JAVA.md` §7 / README.md's
  config table).

No `python-import-stdlib-list` / `python-import-first-party-packages` keys
are needed — §3's simplified sort rule has no tier classification to
configure.

---

## Test-Fixture Repos

- `python/cpython` — the reference implementation's own standard library;
  large, disciplined, real-world Python at scale.
- `pallets/flask` — small-to-medium, idiomatic, widely-read real Python.
- `django/django` — large real-world Python with heavy decorator/class-based-
  pattern and dict/list literal density (good §1.5 dict-vs-set stress test).
- `psf/black` — worth including specifically because it's a formatter
  itself: its own source is real Python, and its test-fixture corpus
  (`tests/data`) is itself a curated set of formatting edge cases that may
  be directly reusable.
- `pallets/click` — dense decorator and nested-call-argument use, good
  additional stress test for the §1.1 tight/loose bracket heuristic on call
  sites.

---

## Test Fixtures (Local)

Local dogfood pairs (distinct from the external-repo list above, which is
for corpus-scale validation) have been authored and registered in
`formatter/test/` — see `test/README.txt` for the pair list and what each
covers.

---

## Tools/compiler used

Use:
    `PYTHONDONTWRITEBYTECODE=1 python3.12 -m py_compile <file.py> [file2.py ...]`
or:
    `PYTHONDONTWRITEBYTECODE=1 python3.6 -m py_compile <file.py> [file2.py ...]`

---

## Dogfood Tooling

`python_content_diff.py` (`tools/verifiers/`) — content-preservation
checker for real-code testing, modeled on `STATE_DATA_FORMATS.md`'s
`*_content_diff.py` scripts (Python has a real parser in its own stdlib).
Parses original and formatted files with stdlib `ast`, compares
`ast.dump(tree, include_attributes=False)` for structural equality
(position attributes stripped since formatting legitimately changes those).
Exit 0 identical, 1 with first-mismatch line if not, 2 if either file fails
to parse. Usage: `python3 python_content_diff.py <original.py> <formatted.py>`.

**Known false-positive, triage manually before calling it a bug:** §3's
import-sort pass legitimately reorders `from X import name` sibling
statements, changing AST statement order without changing the imported
names — confirmed during `pallets/flask` (9/9 initial AST-diff mismatches
were this shape). Check by comparing each file's set of
`(module, name, asname)` import tuples pre/post format (order-independent);
unchanged set = §3 working as intended, not corruption.

## Class Scoping (post Core/Curly/Indent/Tags refactor)

Python3 was the first real implementation to land in the `*Indent` skeleton
classes created by the refactor: `TokenizerIndent`, `FormatterIndent`,
`ScopePipelineIndent`, and `MiscRuleIndent` hold all real Python3 logic.

`DeclarationAlignmentRuleIndent`/`GetterSetterRuleIndent` skeletons existed
as optional-reuse scaffolding for Python3's alignment-grid work (§1 feeding
§2, §6, §7) in case it overlapped the curly-family shape. Never needed —
`match`/`case`, indentation-as-scope, and bracket-complexity work ended up
entirely bespoke inside the `*Indent` classes. **Removed as dead scaffold
code in the 2026-07-28 cleanup pass** (STATE_COMMON.md's "Project
refactoring/cleanup pass") — confirmed zero references anywhere in
`src/`/`Makefile`/docs beyond the two files' own declarations.

Implementation order: tokenizer pass (indentation-as-load-bearing
architecture decision) → statement/indentation skeleton → §1
bracket-complexity → §2–9 → fixtures → real-code testing.

## Open Questions

None remain. `STYLE_PYTHON3.md`'s own "Known Open Items" (§10) states its
prior open items (decorators, f-strings, type-hint signature wrapping) were
already resolved via Q&A and folded into §4–§6. The former
implementation-architecture-level open item (indent-size/style conversion
granularity) is resolved — see "Indent-Size/Style Conversion" below. The
former `python-import-sort`/`python-import-blank-lines` wiring gap (see
"Config Keys Wiring — DONE (RDD_KEY_247)" below) was resolved 2026-08-06.

---

## Config Keys Wiring — DONE (RDD_KEY_247)

`python-import-sort`/`python-import-blank-lines` were documented in README.md
and this file's Config section but never wired into `Config.java` (not in
`ALL_KEYS`, no fields/getters, no CLI/file parsing, not in `GROUPS`) —
noticed while building the server's `/properties` endpoint
(`STATE_COMMON.md`'s "Server mode: 3rd endpoint"). Fixed 2026-08-06.
`python-import-sort` was unambiguous to wire — gates
`ScopePipelineIndent.applyImportSort` exactly like `java-import-sort`/
`js-import-sort` gate their own passes (off = the whole §3 pass is a
no-op). `python-import-blank-lines` had nothing to wire into:
`flushImportGroup` only replaced a group's own `[start,end)` range, never
the gap *between* groups — unlike JS/TS's `enforceImportOrdering`, whose
`blankLines` param threads into `renderImportSegment`. That half was
stopped per `STATE_COMMON.md`'s ambiguity protocol and asked of the user
as an Open Question — resolved same day: coordinator decided to implement
it for real, mirroring JS/TS's `enforceImportOrdering`/`renderImportSegment`
blank-line-insertion shape. New `ScopePipelineIndent
.applyImportGroupBlankLines`/`isBlankLine`, scoped to the one unambiguous
case for Python's bucket-less, adjacency-based grouping: two consecutive
recognized import groups at the **same depth**, separated **only** by
blank physical line(s) (no comment, no depth change, no other statement
between), get their blank-line count normalized to `pythonImportBlankLines`.
A gap containing a comment or spanning a depth change is left untouched —
neither shape is documented by the key or STYLE_PYTHON3.md's worked
example. `applyImportSort` restructured to track each flushed group's own
`[startIdx, endIdx, depth)` range in `rawLines` (`groupRanges`) so the new
pass can find each inter-group gap precisely.

`ScopePipelineIndent` gained a 5-arg constructor
(`lang, indentWidth, lineLength, pythonImportSort, pythonImportBlankLines`)
threading both new `Config` getters through from `FormatterIndent
.formatOne`; existing 2-/3-arg constructors default to `true`/`1` (backward
compatible). `Config.java` gained `pythonImportSort`/`pythonImportBlankLines`
fields+getters, both keys in `ALL_KEYS`, CLI/file parsing
(`parseBoolean`/`parseInt`, matching `java-import-sort`/
`java-import-blank-lines`), a new `Python 3` group in `GROUPS` (between
`JS/TS` and `HTML5`, matching README.md's order), and `describeOne` cases.
Removed a stale `Config.java` comment calling these keys "pre-existing, not
introduced by this grouping."

**Validation:** `make test` 245/245 forward + idempotency (244 pre-existing +
1 new). New fixture `test/py_import_blank_lines_{inp,out}.py` (registered
alongside `py_combined`/`py_comments`): 2 adjacent same-depth import groups
separated by a 2-blank-line gap collapse to 1 (default
`python-import-blank-lines`), each group also sorted (default
`python-import-sort=on`); a *different* 2-blank-line gap (between last
import group and a following `def`) left untouched — proves scoping to
import-group boundaries only. Manual smoke test
(`JXMAKE_CODE_FORMATTER_PYTHON_IMPORT_SORT`/`_PYTHON_IMPORT_BLANK_LINES` env
overrides): `python-import-sort=off` reproduced fully unsorted output with
blank lines untouched; default sort + `python-import-blank-lines=0`
collapsed the inter-group gap to zero while leaving the pre-`def` gap at 2.
`/properties` verified via live server + `curl`: `"Python 3"` group present
between `"JS/TS"` and `"HTML5"`, `python-import-sort` → default `"on"`,
allowed `["on","off"]`; `python-import-blank-lines` → default `"1"`, allowed
`null` (free-form int) — matches README.md's defaults exactly.

---

## Indent-Size/Style Conversion — DONE (RDD_KEY_237)

Python analog of `MiscRuleCore#convertIndentation`, implemented 2026-08-04.
**Granularity decision (resolved against real evidence, superseding the
prior "per-block, rescale if clean multiple" hypothesis):** real-code
checks across `/tmp/black`/`/tmp/django`/`/tmp/cpython` found **zero
in-code indentation drift** in `django/django` or `python/cpython`; the
only 3 tab-indented files found (`psf/black`) were entirely inside
already-opaque triple-quoted docstrings, never real block indentation.
Disciplined Python essentially never has intra-file indent-style/size
drift, so per-block width-guessing solved a hypothetical that doesn't occur
in practice. Implemented instead: reconstruct each **real statement line's**
indentation directly from `TokenizerIndent#synthesizeIndentation`'s
already-authoritative INDENT/DEDENT depth stack (the same mechanism
Python's grammar uses to decide block membership — internally consistent by
the fact the file tokenized at all) rather than re-deriving depth by
guessing from raw per-line width — sidesteps the block-boundary-granularity
question entirely, since depth is already known per line.

New `MiscRuleIndent#convertIndentation` (mirrors `MiscRuleCore
#convertIndentation`'s name/shape/signature) walks the token stream with a
running `depth` counter incremented/decremented by each INDENT/DEDENT
marker; each real statement line's leading whitespace renders as `depth`
indent units in the target style. A blank/comment-only line (no
INDENT/DEDENT of its own — e.g. a comment deliberately dedented early to
visually group with a shallower block, confirmed via `test/py_comments_inp.py`)
is never depth-rewritten (true depth ambiguous), but its width is still
safely re-styled via the inherited `MiscRuleCore#renderIndent`/
`expandedIndentWidth` (changes no width, only character choice — safe
regardless of true depth). An interior continuation line of a
multi-physical-line statement (bracket/backslash continuation) is left
completely untouched, mirroring `TokenizerIndent#synthesizeIndentation`'s
own `insideBrackets`/backslash check for what counts as a new logical line.

Wired into `FormatterIndent#formatOne` as a final phase (re-tokenize the
fully-formatted text via `TokenizerIndent`, then convert) — mirrors
`FormatterCurly`'s "Phase 6: final whitespace normalization, last." No new
config key: reuses the existing shared `indent-style` key (already resolved
from `auto` to a concrete `spaces`/`tabs` choice upstream in
`Main.formatStandalone`/`ServerMode` before either family's formatter
runs).

Two real bugs found and fixed (same real-code idempotency methodology used
throughout this job):
1. A `match`/`case`-adjacent comment line got rewritten to the carried-over
   (deeper) depth instead of its own written (shallower) width. Repro:
   `test/py_comments_inp.py`. Fixed by exempting blank/comment-only lines
   from depth-based rewriting (width-convert only, per above).
2. A synthesized end-of-file DEDENT-run token's `text` field (a literal
   width number for internal use, per `TokenizerIndent
   #synthesizeIndentation`'s javadoc) was appended verbatim as real source
   text whenever it landed outside the `atLineStart` branch (a file with no
   trailing newline on its last line) — corrupted `psf/black`'s
   `tests/data/cases/comments3.py`/`annotations.py` with a stray trailing
   digit, growing further each idempotency round. Fixed by explicitly
   skipping INDENT/DEDENT token text in the general (non-`atLineStart`)
   append branch.

New fixture `test/real_code_regressions_178_{inp,out}.py`: tab-indented
`match`/`case`/comment source (pattern modeled on `py_comments`, confirmed
absent as real in-code drift per the corpus check above) converted to the
default `indent-style = spaces` target — exercises depth-based statement
rewriting, comment-width-only conversion, and the EOF-no-trailing-newline
DEDENT fix together.

**Final validation:** `make test` 244/244 forward + idempotency. Full
corpus re-run after both fixes: `psf/black` (338 files), `django/django`
(2927 files), `python/cpython` (`Lib/`, ~1500 files) — zero crashes, zero
non-idempotency, zero new `python3.12 -m py_compile` errors (only
pre-existing, formatter-unrelated failures reproduced identically on
unformatted originals, e.g. `annotationlib.py`'s t-string syntax not yet
supported by python3.12).

---

## Comment Normalization — DONE (RDD_KEY_268)

`normalize-comment-start-case`/`normalize-comment-end-period` were wired up for python3 in this
2026-08-08 session — the last piece of the cross-job comment chain-grouping brief, already landed
for curly (pre-existing), xml/html5 (`9d2312b`), json/json5/css (`b9aa770`/`22a031f`), yaml/toml
(`c064018`/`0e8da9e`), and makefile/bash/powershell (`847d45f`). Full text: `RDD_KEY_268` in
`RDD_LOG.md`.

**Classifier/GRU reuse (key architectural requirement):** `MiscRuleCore`'s
`capitalizeFirstLetter(String)`/`stripSoleTrailingPeriod(String)`/
`stripSoleTrailingPeriodAcrossLines`/`classifyComment` were already family-agnostic (gated only on
the shared `normalizeCommentStartCase`/`normalizeCommentEndPeriod`/`commentNormalizationClassifier`/
`gruClassifier`/`gruWeightsPath` fields every `MiscRuleCore` subclass carries), so `MiscRuleIndent`
needed only a new 8-arg constructor (mirrors `MiscRuleCurly`'s) threading `gruClassifier`/
`gruWeightsPath` through to the inherited `MiscRuleCore` constructor — no new classifier-integration
code.

**New in `MiscRuleIndent`:**
- `COMMENT_NO_CAPITALIZE_PYTHON` — python's hard/soft keywords, `self`/`cls`, and lowercase
  magic-comment directive words (`noqa`/`type`/`pragma`/`coding`/`fmt`/`isort`/`pylint`/`mypy`/
  `flake8`/`nosec`), consulted by `isCommentNoCapitalizeWord` only when
  `comment-normalization-classifier=off` (same as every other language; the classifier path never
  consults it — confirmed via manual diff, not a python-specific gap).
- `isCommentChainLink`/`isCommentRewritable` overridden to unconditionally `true` — python has
  neither a closing-brace-label-comment concept (`isClosingBraceLabelComment` checks for a
  preceding `}`, meaningless for a `#` chain) nor a separator-alignment-comment concept
  (`parseSeparatorComment` hardcodes `substring(2)`/`"//"`, would misparse a 1-char `#` body); §7
  already established neither applies here ("no closing-comment mechanism exists, confirmed via
  full-tree grep").
- `computeHashCommentGroups` — a `#`-prefixed analog of `MiscRuleCore#computeLineCommentGroups` (not
  directly reusable: that method hardcodes `substring(2)`/`"//"`). Reuses the already
  family-agnostic `isStandaloneCommentLine`/`nextCommentChainLinkIfAdjacent`/
  `stripSoleTrailingPeriodAcrossLines` unchanged.

**New in `ScopePipelineIndent`:** `applyCommentNormalization` pass (added to `process`'s replacement
list, after `applyControlFlowBlankLines`) turns each changed `COMMENT_LINE` token from
`computeHashCommentGroups` into a single-token `Replacement`. New 10-arg constructor threads the 5
comment-normalization config values through to `MiscRuleIndent`; the pre-existing 5-arg constructor
now delegates to it with all 5 defaulted off (unchanged behavior for callers that don't need them).
`FormatterIndent#formatOne` passes the real `Config` getters (`isNormalizeCommentStartCase`/
`isNormalizeCommentEndPeriod`/`isCommentNormalizationClassifier`/`isGruClassifier`/
`gruWeightsPath`) through.

**Fixture:** `test/py_comments_normalization_{inp,out}.py` (registered in `test/README.txt`/
`Makefile`'s `INP_FILES`), using a `#% JXM_CFMT_CFG comment-normalization-classifier=off` in-file
directive to exercise the deterministic no-capitalize-word-list path (the default classifier/GRU
path was manually confirmed to normalize `noqa`/`type`-leading comments anyway, since it never
consults the word list). Covers: a 3-line standalone chain (only first line's start capitalized;
sole trailing period — the only `.` in the chain — stripped only from the last line); `noqa`/`type`
staying lowercase; a trailing (non-standalone) comment as its own singleton group; an ordinary
standalone single-comment capitalization.

**Existing-fixture regressions:** `test/py_combined_{inp,out}.py`, `test/py_comments_{inp,out}.py`,
`test/real_code_regressions_127_{inp,out}.py`, `test/real_code_regressions_178_{inp,out}.py` each
needed their `_out.py` updated for the newly-wired-up pass (every diff traced to a `#` comment
gaining start-case capitalization and/or sole-trailing-period stripping, now firing where
§2/§3/§7/§9's worked examples happened to contain ordinary-prose `#` comments). Per this job's
fallback instruction, left unfixed by the implementing agent for the project owner —
**owner reviewed and fixed all four fixtures directly this session**, confirmed correct, `make
test` 258/258 forward + idempotency (257 pre-existing + 1 new fixture).

No real-code dogfood corpus re-run performed this session — the five-repo dogfood list
(`pallets/flask`, `pallets/click`, `psf/black`, `django/django`, `python/cpython`) was all done for
the pre-existing rule surface (§1-9, indent-size/style conversion); a future session may re-run it
to check for classifier-path comment-normalization regressions/false positives, same as every other
language's dogfood precedent for a newly-landed rule.

---

## Checklist

- [x] Tokenizer support pass (5 slices, all landed; class `TokenizerIndent`,
      not shared `TokenizerCore`). Whitespace, newlines, `#` comments,
      numbers, identifiers/keywords (own `KEYWORDS_PYTHON` set excluding
      context-sensitive soft keywords `match`/`case`/`_`/`type`), string
      literals, generic operator/punct fallback (Slice 1); triple-quoted
      strings as one opaque `STRING` token, satisfying RDD_KEY_186 at
      tokenizer level (Slice 2); `:=` walrus as one `OP` token (Slice 3);
      f-string interpolation sub-tokenization (`FSTRING_START`/`MIDDLE`/
      `END`/`FORMAT_SPEC` types, recursive field expression scan,
      `!conversion` handling) — **known limitation, DEFERRED 2026-08-10
      (not planned): a nested replacement field *within* a format spec**
      (`f"{x:{width}}"`) **is not recursively sub-tokenized**, only the
      outer field is (Slice 4). Cost/value assessed: the full CPython
      dogfood (2343 files, incl. `Lib/test/test_fstring.py`) found zero
      real instances mattering — cosmetic-only (untouched, never
      corrupted), and nested format-spec fields are almost always bare
      identifiers with no internal whitespace to normalize. Documented in
      `README.md`'s Known Limitations → "Indent-based family (Python 3)".
      Revisit only if a real corpus surfaces a concrete case;
      INDENT/DEDENT synthesis (CPython-style indent-width stack, merged
      bracket-nesting counter suppresses significance inside
      brackets/backslash-continuations) — no tabs/spaces consistency
      validation, assumes syntactically valid input (Slice 5). `make test`
      114/114 forward + idempotency, zero regressions (compile/link-health
      only until wired into live dispatch).
- [x] Basic statement/indentation formatting skeleton. `ScopePipelineIndent
      .process` tokenizes via `TokenizerIndent` and renders the token
      stream back verbatim (identity pass; `render` skips zero-text
      INDENT/DEDENT markers). `FormatterIndent.formatOne` delegates to it.
      Smoke test byte-identical + `make test` 114/114, zero regressions.
- [x] §1 (bracket complexity detector). Self-contained
      `evaluator/PythonBracketComplexityEvaluator.java` (does not delegate
      to `ComplexityPaddingEvaluator` — no dict/set-as-complexity-signal or
      Python bracket-kind handling there). Three entry points:
      `isLooseParen` (§1.1/§1.2, generator-expr-as-argument), `isLooseBracket`
      (§1.1-§1.4, comprehension always loose, `:` slice segments evaluated
      independently, star-unpacking needs no special case since `*`/`**`
      are `OP` tokens), `isLooseBrace`/`classifyBrace` (§1.5, non-empty `{}`
      always loose, empty always tight/`DICT`, classified `DICT`/`SET` by
      top-level `:` presence). Own local bracket-depth counter. 18-case
      smoke test (every STYLE_PYTHON3.md §1.1-§1.5 worked example) +
      `make test` 114/114, zero regressions — not yet wired into any caller
      at this point; that lands with §2/§6/§7.
- [x] §2–9 rule-by-rule (each its own checkpoint commit):

      **§2 (Assignment Alignment).** Fixed tokenizer gap:
      `TokenizerIndent.emitOperator` only consumed one char, splitting
      compound assignment (`+=` etc.) into multiple tokens — fixed via
      `MULTI_CHAR_OPS` (mirrors `TokenizerCurly`). New
      `MiscRuleIndent.PyAssignment`/`renderPyGroup` (padded `name (op)=
      value`, no trailing-comment alignment per STYLE_PYTHON3.md §2) plus
      `ScopePipelineIndent.applyAssignmentAlignment`, a from-scratch
      NEWLINE/INDENT/DEDENT-aware logical-line splitter; groups break on
      blank line, comment, depth change, or unrecognized statement. **Gap:**
      multi-line RHS (bracket/backslash continuation) never a candidate;
      bare-IDENTIFIER-target-only (matches C-family's exclusion of
      `self.x = 1`/tuple-assignment). 5-case smoke + `make test` 114/114.
      **Disposition (2026-08-10):** not Python3-specific — grep confirmed
      the curly-family's own declaration/assignment-alignment rules
      (`JsTsDeclarationAlignmentRule.spansMultipleLines`, 4 call sites) bail
      out identically once the RHS spans multiple lines, itself undocumented
      anywhere. Matches established family-wide by-design behavior, not a
      real gap — no README entry, no `XL.txt` TIER 9 entry added.

      **§3 (Import Ordering).** New `MiscRuleIndent.PyImport` (`Kind` enum
      `FUTURE < IMPORT < FROM`) plus `ScopePipelineIndent.applyImportSort`,
      reusing shared `RawLine`/`splitRawLines` (shared with §2). Groups
      break on blank line, comment, depth change, or any non-import
      statement. §3.1's worked example required within-clause name sorting
      too — `flushImportGroup` rebuilds a `FROM` import's own name-list
      span when out of order, even in an otherwise-unchanged/singleton
      group. **Gaps:** multi-physical-line import untouched; parenthesized
      `from X import (...)` rejected entirely; multi-module `import a, b`
      rejected/deferred (only single-module `import a.b.c[ as alias]`
      recognized). 6-case smoke + `make test` 114/114.
      **Disposition (2026-08-10):** unlike §2's gap, this one is safe
      (excluded line breaks the group boundary, never corrupted) but a
      genuine coverage gap on common real-world Python style — parenthesized
      multi-line imports and multi-module `import a, b` are frequent.
      Plausible to fix by extending `classifyImport`/`flushImportGroup` to
      parse across the parenthesized span and comma-list. Judged worth
      keeping open: added to `XL.txt` TIER 9 (not README Known Limitations).

      **§3 gap closed (2026-08-10, RDD_KEY_277 — XL.txt TIER 9 item).**
      `classifyImport` extended to recognize all three previously-rejected
      shapes: single-line multi-module `import a, b` (the `import` branch
      loops a comma-separated dotted-name list, each optionally `as alias`,
      via `readDottedName`/`advancePastDottedName`); parenthesized `from X
      import (...)` possibly spanning many physical lines (the `from`
      branch detects a leading `(` via `matchBracket`, bounds the name-list
      scan to the parenthesized span, tolerates only a lone trailing comma
      before the close paren); and backslash-continued `import a, \` /
      `b, c` (new `nextSignificantSkipBackslash` helper — `isGapToken` does
      not treat a literal `\` OP token as transparently skippable, unlike
      WHITESPACE/NEWLINE/COMMENT). Multi-module `import a, b` was a
      genuinely separate gap from the `multiPhysicalLine` call-site gate:
      its rejection was an internal `classifyImport` comma-check, so even a
      plain single-physical-line `import a, b` was rejected before this
      fix. A parenthesized clause carrying any comment inside its span
      (checked via `containsComment`, now scanned from the opening `(`
      itself rather than the first name — a real `django` file had a
      comment right after `(`, before any name, which a narrower scan
      missed) disables only that clause's own within-clause resort
      (`nameListStart`/`nameListEnd` set to -1, falling back to
      `flushImportGroup`'s verbatim reproduction for that statement); the
      statement still participates in cross-statement group
      classification/movement, since that's unconditionally safe. Two
      safety bugs found and fixed via real-code testing (full detail in
      RDD_KEY_277): an off-by-one where `advancePastDottedName`'s `-1`
      "last token on the line" return value defaulted to `line.end`,
      splicing a RawLine's terminating NEWLINE token's text into the last
      comma-separated unit and corrupting the rebuild; and the
      comment-scan-range bug above, found via a 2927-file `/tmp/django`
      dogfood pass, which had corrupted a real `# isort:skip` import block
      before the fix. Verified: `make test` 275/275 forward + idempotency
      unchanged; new fixture `test/py_import_multiline_inp/out.py` covers
      all three shapes plus a per-name-commented parenthesized clause; full
      `/tmp/django` corpus dogfooded twice (before/after the
      comment-scan-range fix), zero crashes both times. A final post-fix
      re-run found exactly 2 remaining non-idempotent files, both isolated
      via A/B testing to **pre-existing, unrelated
      comment-normalization-classifier non-determinism**, not this change:
      a `# isort:skip` capitalization flip reproducing identically on the
      unmodified pre-change codebase, and a
      `# RemovedInDjango70Warning.` trailing-period-stripping flip that only
      reproduces post-change because that clause was previously
      frozen/untouched by the old reject-outright behavior and is now
      correctly exposed to the separate (already-flaky, out-of-scope)
      comment-normalization pass for the first time — both differ only in
      comment styling, no content lost/reordered/corrupted. §3's gap list
      above is now historical, superseded by this entry — `XL.txt` TIER 9
      item resolved.

      **§4 (Decorators).** New `ScopePipelineIndent.applyDecoratorSpacing` +
      bracket-padding helpers. For each single-physical-line `@` line: gap
      between `@` and the next token collapsed to zero; every `(`/`[`/`{`
      pair in the decorator's expression (recursively) gets its delimiter
      gap normalized per `PythonBracketComplexityEvaluator`'s loose/tight
      verdict. A bare decorator (`@dataclass`, `@x.setter`) never enters
      bracket-padding; multi-physical-line decorators skipped entirely.
      Bug fixed: `normalizeGap` wrongly no-op'd on already-tight `from ==
      to`, skipping the loose case's needed zero-width insertion; fixed to
      only guard on `from > to`. 7-case smoke + `make test` 114/114.

      **Gap closed (2026-08-12): decorator-call overflow/line-wrapping.** New
      `tryWrapDecoratorCall` + `splitTopLevelArgs`/`addTrimmedArg`/
      `renderSpan` in `ScopePipelineIndent`, wired into `applyDecoratorSpacing`
      — a narrow, decorator-scoped equivalent of the C-family's
      `enforceCallLineBreaking` (not general/ported; §6's own signature-wrap
      inline-vs-one-per-line decision remains a separate gap, only the
      decorator-call case is closed here). For a single-physical-line
      `@`-decorator whose outermost trailing call exceeds `lineLength` after
      §4's own bracket-padding: splits the call's top-level
      (bracket-depth-0) arguments, renders each indented one level past the
      `@` line with a trailing comma, closing `)` back at the `@` line's own
      indent — matches STYLE_PYTHON3.md §4's "Overflow" worked example
      exactly. Bails (leaves untouched) on: no trailing call at all
      (`@dataclass`/`@x.setter`); a zero-arg call; a comment anywhere inside
      the call's parens or trailing after the closing `)` on the same line
      (same posture as `enforceCallLineBreaking`); or the call not being the
      very last thing on the line. A wrapped decorator becomes
      `multiPhysicalLine` on the next pass, so `applyDecoratorSpacing`'s
      existing multi-physical-line skip makes round2 a no-op — idempotent.

      **Known-risk interaction (nested f-string fields / lambda defaults)
      verified safe.** Per-argument rendering reuses `applyBracketPadding`'s
      recursive bracket-padding, rescoped strictly inside the call's own
      parens (`renderSpan`'s doc comment explains why the outer pair's
      open/close gap entries must be excluded — a real bug during
      implementation had the outer call's loose-open-gap replacement `start`
      coincide with the first argument's first token, double-applying as a
      spurious leading space, `@app.route( "..."`; fixed by re-deriving
      padding over `[openIdx + 1, lastSigOnLine)`). Since the same
      already-battle-tested `applyBracketPadding` (with its f-string-field
      adjacency awareness from the §4/§5 idempotency fix below) drives each
      argument, `f'Struct331_{signedness}{n}'`-shaped adjacent fields and a
      lambda-default argument with its own f-string field both wrap correctly
      with no brace corruption — covered by fixture
      `test/py_decorator_overflow_{inp,out}.py`. Verified via debug run +
      empty round1/round2 diff + `python_syntax_check.sh`; `make test` green
      afterward (`py_combined_out.py`'s already-overflowing `@app.route(...)`
      line updated to its new wrapped expected output — intended).

      **§5 (F-Strings).** New `ScopePipelineIndent.applyFStringSpacing` +
      helpers (`processFString`/`processField`/`addBraceTrim`) — operates
      over the full token stream, not per-`RawLine`. `processField` tracks
      local bracket depth and recurses into nested f-strings. `addBraceTrim`
      unconditionally trims the gap after `{`; trims the gap before the
      close ONLY when no `!conversion`/format-spec tail follows
      (`f"{value !r}"` must keep that gap). **Gap: internal expression
      re-spacing (`f"{x  +  1}"` → `f"{x + 1}"`) out of scope** — the only
      inherited token-joining primitive (`MiscRuleCore#renderTokens`/
      `needsSpaceBetween`) is a C-family declarator-spacing helper that
      would wrongly collapse Python's `*` multiplication and has no notion
      of `**`/`//`/`:=`/`and`/`or`/`not`/comprehension `for`/`if`. When an
      f-string sits inside a span another pass (e.g. §2's assignment RHS)
      already fully replaces, that pass's wider, earlier-`start`
      replacement wins and this pass's narrower one is silently dropped,
      not corrupted. 8-case smoke + `make test` 114/114.

      **DECIDED CLOSED [2026-08-12], not a future job — do not re-add to a
      checklist/XL.txt tier that implies revisiting.** Matches `black`/`ruff
      format` convention (both also leave f-string interiors alone), so the
      gap is intentional scope. Fixing it for real would need a genuine
      Python-expression tokenizer/spacer — reusing the C-family
      `needsSpaceBetween` risks silent semantic corruption, not just a style
      miss. See `XL.txt` TIER X: Dead.

      **§6 (Function Signature Wrapping) — alignment slice.** New
      `MiscRuleIndent.PyParam` (name/type/default triples, trailing-comma
      flag) + `renderPySignatureGroup` (name column padded to widest; `=`
      column padded only across params with BOTH a type hint and a
      default). A param with no type hint skips the `:` segment entirely
      (documented partial-row shape, not a bug). New
      `ScopePipelineIndent.applySignatureAlignment`/`trySignatureGroup`/
      `classifySignatureParam` — requires the `def`'s parameter list
      already one-parameter-per-line; any deviation (inline first param,
      multiple params per line, per-param trailing comment, a param
      spanning multiple lines) returns null, leaving the *whole* signature
      untouched. `classifySignatureParam`'s `:`/`=` search tracks local
      bracket depth so nested type hints (`List[Dict[str, int]]`) work
      correctly. Return-type arrow untouched by construction. 5-case
      smoke + `make test` 114/114.

      **§6 inline-vs-one-per-line decision — CLOSED [2026-08-12], not a
      future job.** New `ScopePipelineIndent.applySignatureWrapping`/
      `tryWrapDefSignature`, run immediately before the alignment slice
      above, is §4-`tryWrapDecoratorCall`-shaped: a single-physical-line
      `def`/`async def` header whose full line still exceeds `lineLength`
      after locating its header-terminating `:` (depth-tracked past a
      `-> Dict[str, int]:` return-type annotation) wraps its top-level
      (bracket-depth-tracked, so `Dict[str, int] = {}` defaults split
      correctly) parameter list one-per-line, rendered **already
      `:`/`=`-column-aligned** via the same `renderPySignatureGroup` the
      alignment slice above uses, rather than left plain for a later pass.
      Unlike §4's decorator wrap (always trailing-comma), §6's worked
      example shows no trailing comma after the last parameter, so none is
      synthesized. Returns `null` (leave inline) on: a comment anywhere
      inside the parens or between `)` and the header `:`; no header `:`
      found on the same physical line; zero parameters; any parameter
      segment failing `classifySignatureParam`'s own per-parameter gap; or
      the line already fitting.

      **Pass-ordering (no fight, no double-processing):** the new pass and
      the alignment slice are mutually exclusive by construction —
      `applySignatureWrapping` only fires on a `RawLine` that is NOT
      `multiPhysicalLine` (still inline as written), while
      `applySignatureAlignment` only fires on one that IS. Both passes
      iterate `rawLines` computed once, up front, from the *original*
      pre-wrap token stream, so the wrap pass's own synthesized multi-line
      text is never re-discovered by a later pass in the same `process()`
      call — hence the wrap pass aligns inline itself. Verified via a
      dedicated adjacent-pass-ordering fixture
      (`test/py_signature_wrap_inp.py`'s `already_wrapped` case: an
      already-one-per-line signature is picked up by the alignment slice,
      not this new pass).

      **One real, pre-existing bug found via `psf/black` dogfood re-run,**
      unrelated to the new wrap pass (reproducible by feeding an
      already-one-per-line signature straight to the pre-existing code): a
      parameter with neither type hint nor default (e.g. bare `**kwargs`
      last parameter) still got its name right-padded to `maxNameLen` with
      nothing to absorb the padding; `trySignatureGroup`'s per-parameter
      replacement span stopped at the last significant token (`trimEndIdx`)
      rather than the segment's own trailing `NEWLINE`, so a previous
      round's padding was left behind as orphaned whitespace and stacked on
      each round, growing unboundedly. Fixed: each parameter's span now
      always extends through its segment's own `NEWLINE`. The remaining ~10
      of the original 13 `psf/black` idempotency mismatches were confirmed
      unrelated (pre-existing comment-classifier non-determinism, and the
      §8 join-threshold bug, RDD_KEY_287 below). New fixture
      `test/real_code_regressions_201_{inp,out}.py`.

      Local fixtures: `test/py_signature_wrap_{inp,out}.py`
      (already-fitting left untouched; plain overflow wrap; a
      `Dict[str, int] = {}` default needing depth-tracked comma
      splitting; a return-type-annotation case; the already-wrapped
      adjacent-pass-ordering check above) plus `test/py_combined_out.py`
      updated in place (its own `def process(...)` line now legitimately
      wraps, matching STYLE_PYTHON3.md §6's worked example exactly).
      `make test`: 288/288 (pre-existing) → 289/289 (regression fixture
      above), all green including idempotency.

      **§8 (Single-Statement Bodies) — a join operation** (unlike §2-§7,
      which never join/split lines). Precedent: `BlockStructureRule
      .collapseSingleExpressionBlocks`/`isSingleStatementBody` (C-family,
      STYLE.md §10). `ScopePipelineIndent` gained a `lineLength`
      constructor param (new `Config.DEFAULT_LINE_LENGTH = 100`) plus
      `applySingleStatementBody`/`classifySingleStatementHeaderColon`/
      `bodyOpensNewBlock` and `SINGLE_STMT_HEADER_KEYWORDS`
      (`if`/`elif`/`else`/`while`/`for`; `case` delegates to §7's
      `classifyCaseLine`; `def`/`class`/`try`/`except`/`finally`/`with`
      never members). A header qualifies when genuinely block-form; a
      multi-physical-line header/body is retained without flattening its
      internal layout. Body line must be one depth deeper,
      non-blank/non-comment, not itself open a further nested block; the
      following line must sit shallower. **Ambiguity resolved
      conservatively:** a body containing `lambda` is always treated as
      "opens a new block" (never joined); a nested compound statement's own
      header still independently gets its own join opportunity. 17-case
      smoke + `make test` 114/114. **Extended 2026-08-11:** body trailing
      comments are retained; an over-limit compact body expands to a normal
      indented block; and semicolon-containing bodies remain untouched so
      the pass never creates or extends a `;` chain. Local fixture:
      `test/py_single_statement_body_ext_{inp,out}.py`.

      **§8/§7 join-threshold non-idempotency — root-caused and FIXED
      (RDD_KEY_287)** (root cause/fix mechanism: see RDD_KEY_287 table entry
      above). Previously just a one-line symptom name, seen but not
      diagnosed during `psf/black`/`click`/`flask`/`django` round1->round2
      dogfood re-runs. Confirmed via minimal repro (`pallets/click`'s
      `utils.py`) and an A/B build from commit `9eb1bd4` (predating this
      session's/the recent §4/§6 work) reproducing the identical flip-flop,
      proving pre-existing. Fixed in `ScopePipelineIndent.java`, 3 call
      sites: `applySingleStatementBody`'s join branch, `classifyCaseLine`'s
      virtualJoin computation (new `bodyLineEnd` field on `CaseLine`), and
      `flushCaseGroup`'s per-member pre-commit length-budget loop. Re-ran
      dogfood against `/tmp/click`+`/tmp/flask` (161 files) and
      `/tmp/django` (2927 files) post-fix: previously flip-flopping files
      (`click/src/click/parser.py`, `click/src/click/utils.py`,
      `flask/src/flask/app.py`, `flask/src/flask/helpers.py`, 3 django
      files) now idempotent; remaining round1/round2 diffs (2 click/flask,
      14 django) are the separately-tracked out-of-scope comment-classifier
      trailing-period drift. New identity-pass fixture
      `test/real_code_regressions_202_{inp,out}.py` (covers both the plain
      `if`/`for` join path and the `case` virtualJoin path). `make test`:
      289/289 -> 290/290 forward + idempotency, zero regressions.

      **§8 compact-branch overflow-check non-idempotency — root-caused and
      FIXED (RDD_KEY_288)** (root cause/fix mechanism: see RDD_KEY_288
      table entry above; same bug family as RDD_KEY_287, fits-check
      measuring the wrong text state). Found via self-hosting dogfood
      (round1/round2 re-format of this repo's own
      `tools/verifiers/{html,python,toml}_content_diff.py`, each with a
      hand-column-aligned `if`/`elif`/`else` one-liner chain — §8's
      compactness decision is per-branch/independent, unlike §7's
      all-or-nothing rule). Verified against the real triggering files:
      `tools/verifiers/{html,python,toml}_content_diff.py` all
      round1/round2-idempotent after the fix. New fixture
      `test/real_code_regressions_204_{inp,out}.py`. `make test`: 312/312 ->
      313/313 forward + idempotency, zero regressions (one unrelated
      pre-existing failure on `test/real_code_regressions_172_inp.ts`,
      confirmed caused by a concurrent session's in-progress
      `BlockStructureRule.java` edit, not this fix). The three triggering
      `tools/verifiers/*.py` files left unreformatted in `tools/` per the
      ground rule that bulk-adopting a reformat is the user's call.

      **§7 (Structural Pattern Matching) — `:` column alignment-only
      slice.** New `ScopePipelineIndent.CaseLine`/`applyCaseColonAlignment`/
      `classifyCaseLine`/`flushCaseGroup` — reuses §4-§6's bracket/gap
      helpers. `classifyCaseLine` checks literal text `"case"` (a
      context-sensitive soft keyword tokenized as plain `IDENTIFIER`); the
      header `:` is found via a bracket-depth-0 scan after `case`, correctly
      skipping a mapping pattern's own colon and including any guard
      clause; `compact` reflects the body's existing shape as written
      (never decided). **All-or-nothing** per §7: `flushCaseGroup` emits
      zero replacements for the whole group if any member is block-form;
      for an all-compact group, only the gap before `:` is padded. Known
      gaps (pre-existing, not new): or-pattern `|`/guard-clause spacing rely
      on the same "no general expression-respacing" gap §5 documented;
      deconstruction/sequence/mapping pattern content not repadded via
      `PythonBracketComplexityEvaluator` (exists but not wired into
      case-pattern rendering) — narrowing to `:`-alignment-only was this
      checkpoint's pre-agreed scope. No closing-comment mechanism exists
      (confirmed via full-tree grep, out of scope). 8-case smoke + `make
      test` 114/114.

      **§9 (Control Flow Blank Lines).** New `ScopePipelineIndent
      .ControlFlowFrame`/`applyControlFlowBlankLines`. §9.1 ported
      faithfully from `MiscRuleCurly#insertBlankLineBeforeReturn`: blank
      line before a `return` that's the first token of its logical line,
      when the innermost enclosing frame is a function body that has
      already seen a statement (does NOT verify `return` is the body's
      textually final statement, matching the C-family reference). **§9.2
      had no C-family mechanism to port** (`BlockStructureRule
      .placeElseOnOwnLine` only ever *preserves* blank-line state);
      implemented directly from STYLE_PYTHON3.md §9.2's text: blank line
      before any `elif`/`else` whose nearest preceding non-blank/non-comment
      logical line has `return`/`break`/`continue` (never `raise`) as its
      first token. Both halves only ever ADD a missing blank line, never
      remove one; a comment-only line immediately preceding a qualifying
      line is conservatively left untouched. **Gaps:** multi-physical-line
      `def` header never recognized as function-body-opening;
      semicolon-chained statements never recognized by either half; a
      §8-compact preceding block ending in return/break/continue never
      recognized by §9.2; `try`/`except`/`finally` blank-line placement
      entirely out of scope. 14-case smoke + `make test` 114/114 forward +
      idempotency.

      **§9 Gap Fixes (2026-08-11, RDD_KEY_282).** 3 of the 4 gaps closed
      (enumeration/fix mechanism: see RDD_KEY_282 table entry above —
      multi-physical-line `def` header, semicolon-chained statements via
      `lineHasTopLevelReturnSegment`/`lastSemicolonSegmentStart`, and a
      §8-compact preceding block via `classifyCompactSingleStatementHeaderColon`
      delegation). `try`/`except`/`finally` deliberately left scoped out:
      STYLE_PYTHON3.md §9.2's text names only `elif`/`else`; extending to
      `except`/`finally` needs a new design decision (what "preceding
      block's last statement" means for a `try` body whose normal-exit path
      never reaches `except`), not a mechanical extension. One real bug
      caught via `make test` (not just static reasoning):
      `lastSemicolonSegmentStart`'s "last semicolon seen" var initialized to
      `-1` unconditionally, so a no-semicolon line whose own span didn't
      start at absolute token index 0 incorrectly scanned from index 0 —
      caught by `real_code_regressions_79_out.py` losing its already-correct
      blank line before `elif e: continue`; fixed by initializing to
      `start - 1`. `make test`: 282/282 → 283/283 forward + idempotency.
      New fixture: `test/py_control_flow_blank_line_gaps_{inp,out}.py`.

- [x] Local test fixtures authored and registered: `py_combined_inp/out.py`
      and `py_comments_inp/out.py` in `test/`, documented in
      `test/README.txt`. **Activated (2026-07-22):** both verified against
      the actual JAR and uncommented in the Makefile's `INP_FILES`. Each
      pair's `_out.py` was updated to match actual current-scope output —
      every diff traced back to an already-documented gap above (no general
      expression/operator-respacing outside decorator calls/signatures/
      case colons; no inline-vs-one-per-line overflow-wrap decision; no
      automatic blank-line insertion between import groups; no comment
      capitalization/period-normalization pass for Python3 at all, unlike
      the C-family) — not a formatter bug, confirmed via direct source
      inspection. Both fixtures pass forward + idempotency under
      `make test` (116/116 forward + 116/116 idempotency, zero
      regressions).
- [x] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      `STYLE_PYTHON3.md`'s listed test-fixture repos. **All five DONE:**

      **`pallets/flask` — DONE (first Python3 dogfood run).** 83 `.py`
      files (24 `src/flask/`, 41 `tests/`, 18 `examples/`/`docs/`, full
      tree). Zero crashes on forward pass. Four bugs found via
      non-idempotency (`diff -r round1 round2`), none via `py_compile`/
      AST-diff:
      1. `ScopePipelineIndent.render`'s replacement-merge loop advanced
         cursor `r` only on exact `start == i`; overlapping replacements
         (§8 join + §2 alignment) stalled `r`, dropping later replacements.
         Fixed by skipping stale entries instead of stalling. Fixture
         `real_code_regressions_78_{inp,out}.py`.
      2. §6 `trySignatureGroup` split params on raw `NEWLINE` only, not
         bracket-depth-aware — multi-line type hints via open nested
         brackets misclassified instead of "leave whole signature
         untouched," producing non-convergent trailing whitespace. Fixed by
         splitting only at depth-0 `NEWLINE`s.
      3. §9.2 zero-width blank-line insertion and §8 join could share the
         same start index; stable sort left zero-width second, so §8's wider
         replacement jumped over it (a forward-pass bug, not just
         idempotency). Fixed by sorting equal-`start` zero-width entries
         first. Bugs 2+3 combined into fixture
         `real_code_regressions_79_{inp,out}.py`.

      Final: zero crashes; idempotency empty (83/83); `python3.12 -m
      py_compile` clean on all 83 (python3.6 not viable — flask uses
      walrus/`from __future__ import annotations`); `python_content_diff.py`
      clean on all 83 after triaging 9 initial reports, all §3 import-reorder
      false positives. `make test`: 128/128 forward + 128/128 idempotency.

      **`pallets/click` — DONE.** 78 `.py` files (fresh clone `/tmp/click`).
      Zero crashes on forward pass. One bug via non-idempotency:
      `ScopePipelineIndent.applyBracketPadding`
      (§4) couldn't distinguish an f-string field's `{`/`}` from a dict/set
      literal, padding an f-string nested in a decorator's lambda default
      arg (`tests/test_basic.py`'s
      `@click.custom_version_option(lambda ctx: f"{ctx.info_name} 1.0")` →
      `f"{ ctx.info_name }"` on forward; §5 trimmed it back next round).
      Fixed: skip a `{`/`}` pair whenever `{` is immediately preceded by
      `FSTRING_START`/`FSTRING_MIDDLE`. Fixture
      `real_code_regressions_80_{inp,out}.py`.

      Final: zero crashes; idempotency empty (78/78) after fix;
      `python3.12 -m py_compile` clean; formatted package imports cleanly
      (`import click`, version `8.4.0`); representative pytest subset
      (`test_basic.py`, `test_arguments.py`, `test_options.py`) passed
      857/857 (full `tests/` run timed out on an unrelated interactive
      test). `make test`: 129/129 forward + 129/129 idempotency.

      **`psf/black` — DONE.** Fresh clone (`/tmp/black`), 338 `.py` files
      (`src/`, `tests/` incl. `tests/data/`'s curated edge-case corpus,
      `scripts/`). Forward pass: 1 crash (337/338) — FIXED. `tests/data/cases/
      pep_701.py`: `IndexOutOfBoundsException` from `ScopePipelineIndent
      .processField`/`applyFStringSpacing`. Minimal repro: `f"{1}\{{"`.
      Cause: `TokenizerIndent.emitFString`'s backslash-escape always skipped
      2 chars even when next was `{`/`}`, orphaning the second `{` of a
      doubled-brace escape right after a field close. Fixed by skipping
      only the backslash itself when followed by `{`/`}`. Fixture
      `real_code_regressions_114_{inp,out}.py` (identity-pass). `make test`
      163/163.

      Round2 idempotency (337 files): 3/337 differed — 2 bugs, both fixed
      in a follow-up session:
      1. **§7/§8 join-then-align ordering.** Block-form `match`/`case`
         skips §7 colon-alignment on round1; §8 joins each case body onto
         its header; round2 sees compact form and applies alignment round1
         never had. Affected: `tests/data/cases/pattern_matching_simple.py`,
         `tests/data/line_ranges_formatted/pattern_matching.py`. FIXED:
         `classifyCaseLine` gained `tryQualifyJoinBody` to predict, within
         the same pass, whether a case will qualify for §8's join, so
         `flushCaseGroup` bakes correct `:`-column padding up front;
         `applyCaseColonAlignment`'s grouping skips each virtualJoin case's
         body line; `applySingleStatementBody` skips headers already
         handled by §7; if §7's padding would push past `line-length`, the
         group falls back to §8's plain unpadded join. Fixture
         `real_code_regressions_115_{inp,out}.py`. `make test`: 164/164.
      2. **§6 multi-line union-type-hint gap violated + unbounded
         trailing-whitespace growth.** `tests/data/cases/
         pep604_union_types_line_breaks.py`: a `|`-union type wrapped
         across lines with no enclosing bracket had each `| TypeN`
         continuation misclassified as its own parameter, growing padding
         every round (non-convergent over 3 rounds). FIXED:
         `classifySignatureParam` now rejects any segment whose first token
         isn't a valid parameter start (`IDENTIFIER`, or `*`/`**`/`/`); a
         leading `|` means continuation not parameter, so the whole
         signature is left untouched per §6's documented gap. Fixture
         `real_code_regressions_116_{inp,out}.py` (identity-pass). `make
         test`: 165/165.

`python3.12 -m py_compile` on all 337 round1 files: clean except one
      pre-existing failure (`tests/data/cases/
      trailing_comma_optional_parens3.py`, identical on the unmodified
      original — a deliberately-invalid black test fixture).
      `python_content_diff.py` on all 337 round1 files: 22 mismatches — 13
      are the §3 import-reorder false positive; 8 are `rc=2` parse failures
      on deliberately-invalid/post-3.12-syntax fixtures (expected); **2 are
      genuine content-corruption bugs, both in `applyFStringSpacing`'s
      `addBraceTrim` (§5) — FIXED (follow-up session):**
      - (a) **Nested-brace field fusion.** A field followed by a nested `{`
        (`f"{ {a for a in (1, 2, 3)}}"`) had its close-gap trimmed to zero,
        fusing with the following literal `{{` and silently deleting the
        nested expression (`ast.dump` confirmed the node gone). Repro:
        `tests/data/cases/fstring.py` line 8. Fixed: normalize that gap to
        exactly one space whenever the next significant token's text is `{`.
      - (b) **Self-documenting `{expr=}` debug fields.** Leading gap was
        trimmed even though Python reproduces `expr`'s exact original
        whitespace verbatim at runtime for a `=`-suffixed field. Repro:
        `tests/data/cases/preview_long_strings.py` line 327. Fixed: detect a
        bare trailing `=` OP token as the field's last significant token and
        skip all gap-trimming for that field.

      Both combined into fixture `real_code_regressions_117_{inp,out}.py`
      (identity-pass, same method). `make test`: 166/166 forward + 166/166
      idempotency.

      All four `psf/black` bugs fixed; a full 338-file corpus re-run was
      deliberately deferred (each fix independently verified against its
      own repro plus AST-diff/idempotency; `make test` stayed green
      throughout).

      **`django/django` — DONE.** Reused existing checkout at `/tmp/django`
      (2927 `.py` files, full tree). Batch-formatted per round. Zero crashes
      on forward pass. One bug found via non-idempotency, but actual
      content corruption on the forward pass itself: `django/utils/json.py`'s
      `case Sequence(): # str and bytes were already handled.` — a §8
      single-statement-body `match`/`case` header carrying its own trailing
      comment still qualified for the join (only a *body* trailing comment
      was guarded against, not a *header* one), so
      `applySingleStatementBody`'s `headerText` silently deleted the comment
      (genuine data loss). Root cause: `classifySingleStatementHeaderColon`
      and `classifyCaseLine`'s compact/`virtualJoin` both permitted a
      trailing comment after the header colon to still qualify, with no
      "don't lose it" step at join time. Fixed: `classifySingleStatementHeaderColon`
      returns `-1` immediately on any header trailing comment;
      `classifyCaseLine` tracks `headerHasTrailingComment` separately from
      `compact` and never sets `virtualJoin` when true; the `case` delegation
      additionally requires `c.virtualJoin` before returning a joinable
      `colonIdx` — mirrors the join's existing conservative posture for a
      *body* trailing comment. Fixture `real_code_regressions_127_{inp,out}.py`.
      `make test`: 176/176 forward + 176/176 idempotency.

      Final (full 2927-file corpus, re-run after the fix): zero crashes;
      idempotency empty (2927/2927); `python3.12 -m py_compile`: exactly one
      syntax error (`tests/test_runner_apps/tagged/tests_syntax_error.py`,
      identical on the unmodified original); `python_content_diff.py`: 1
      `rc=2` (same fixture) and 41 `rc=1` AST-diff mismatches, all confirmed
      via the documented triage method to be solely §3's import-sort
      reordering.

      **`python/cpython` — DONE, categorized; all 4 clusters FIXED.** Fresh
      shallow clone `/tmp/cpython` (`--depth 1`), 2343 `.py` files, batched
      per top-level subdir through `--preserve-tree`, round2 from round1's
      output. `python3.12 -m py_compile` used as compile-check (python3.6
      not viable against modern cpython syntax). Stats: 1 crash / 2343
      files; 19 idempotency mismatches; `py_compile` errors identical
      before/after (1 in both — `Lib/traceback.py:21`'s `lazy import
      _colorize`, a pre-existing not-yet-standard-Python syntax experiment
      in cpython's own dev tree, present identically in the unformatted
      original — not formatter-induced). Zero new syntax errors from
      formatting.

      1. **[CRITICAL, FIXED] f-string nested-format-spec crash**
         (`IndexOutOfBoundsException`, `ScopePipelineIndent.processField`).
         `Lib/test/test_fstring.py` crashed on cases like
         `f'{2:{"{"}>10}'`/`f'{3:{"}"}>10}'`/
         `f'{10:#{3 != {4:5} and width}x}'`
         (`test_format_specifier_expressions`). Narrower root cause than the
         initial "nested field in format spec not sub-tokenized" gap (still
         real and separately open): `TokenizerIndent.emitFStringFormatSpec`'s
         brace-depth counter scanned raw characters without skipping
         quoted-string content, so a literal `{`/`}` inside a nested field's
         own string-literal expression miscounted nesting depth, running the
         spec scan past the real field end and never emitting `FSTRING_END`.
         Fixed by adding `skipNestedStringLiteral`, skipping a quoted
         string's content whenever a quote is seen at `depth > 0` inside the
         brace counter. Verified against all 5 crashing minimal cases plus
         two multi-level-nesting cases. Fixture
         `real_code_regressions_133_{inp,out}.py`. `make test`: 182/182.
      2. **[IDEMPOTENCY, FIXED] §3 import-sort: same-module multi-statement
         group order unstable on first pass** (16 files, e.g. `Lib/random.py`
         lines 53-56 — four separate `from math import ...` statements).
         Round1 didn't fully alphabetize inter-statement order when one
         group had multiple `from X import ...` lines for the same `X`;
         round2 self-corrected. Root cause: `MiscRuleIndent.PyImport
         .compareTo` keyed on `this.names`/`other.names` from as-parsed
         (pre within-clause-sort) order — a pure sort-key bug, not
         indent-sensitive. Fixed: `PyImport.compareTo` now sorts a copy of
         each side's `names` before comparison (leaving `names` untouched in
         source order for `sortedNameUnits`'s within-clause-rebuild use).
         Fixture: `real_code_regressions_137`. `make test`: 186/186.
      3. **[IDEMPOTENCY, FIXED] §7/§8 join-then-align ordering, recurrence
         adjacent to a preceding block-form `case`** (2 files:
         `Lib/turtle.py` ~line 3930, `Lib/typing.py` ~line 2974) — second
         occurrence of the bug class already fixed for `psf/black`
         (`real_code_regressions_115`). Cause: every member after the
         block-form case is itself `virtualJoin`-eligible, and `case _:`'s
         very short pattern needed enough padding to match its longer
         sibling that the padded+joined line overflowed `line-length` —
         correctly made round1's §7 abandon alignment for the whole group
         (all-or-nothing), leaving §8 to join each member individually,
         unaligned. Round2 then realigned those now-compact members, because
         `flushCaseGroup`'s pre-commit length-budget check only examined
         `virtualJoin` members, never already-compact ones. Fixed by
         extending that check uniformly to every group member (`lineEnd`
         field on `CaseLine`). Fixture: `real_code_regressions_138`.
         `make test`: 187/187.
      4. **[IDEMPOTENCY, FIXED] §4/§5 decorator-call bracket-padding leaks
         into a nested f-string field's own braces** (1 file:
         `Lib/test/test_ctypes/test_generated_structs.py` lines 278, 284).
         `@register(f'Struct331_{signedness}{n}', set_name=True)` got the
         f-string field loose-padded to `{ n }` on round1 alongside the
         outer call parens; round2's §5 `addBraceTrim` trimmed the field
         back but left the outer paren padding — non-idempotent. Same bug
         class already fixed for `pallets/click` (`real_code_regressions_80`),
         but root cause here was field **adjacency**, not nesting depth:
         `{signedness}{n}` has no literal text between the two fields, so
         `TokenizerIndent.emitFString` never emits an FSTRING_MIDDLE token
         between them, leaving the second field's opening `{` directly
         preceded by the first field's closing `}` (plain PUNCT) —
         `applyBracketPadding`'s adjacency guard only checked for
         FSTRING_START/MIDDLE, missing this case. Fixed by tracking the
         previous f-string field's close position across
         `applyBracketPadding`'s loop, treating a `{` immediately following
         it as another field open. Fixture: `real_code_regressions_139`.
         `make test`: 188/188.

      All four clusters fixed; full corpus re-run deferred until requested,
      same pattern as every prior dogfood entry in this file.

      **2026-08-04 — deferred full-corpus re-run done** (closes out XL.txt's
      Tier 1 item 1; first full-repo re-run since the 4-cluster fix — the
      earlier indent-conversion validation only covered `Lib/`). Round1:
      2343/2343 processed, zero crashes. Round2: empty, 0/2343 differ.
      `python3.12 -m py_compile`: 73 failures on round1 vs. 74 on the
      unformatted original — **73 identical** (same `lazy import`/t-string
      bleeding-edge syntax experiments not yet standard Python, e.g.
      `Lib/traceback.py:21`; simply more of them now since this is
      cpython's own moving dev branch, not a formatter regression).

      **One file's compile status changed, via a pre-existing lossy-read
      quirk unrelated to any Python3 rule:** `Lib/test/tokenizedata/
      badsyntax_pep3120.py` is a deliberately invalid-UTF-8-encoded test
      fixture (raw Latin-1 `ö` byte, `0xf6` — the test asserts the tokenizer
      raises `SyntaxError` on it). The formatter's file-reading path decoded
      with a lossy/replacement-character fallback rather than failing
      loudly, then wrote valid UTF-8 back out (invalid byte becomes U+FFFD
      `�`), so the formatted copy compiled cleanly where the original
      didn't — silently changing byte content on truly invalid UTF-8 input.
      Narrowest possible edge case (only instance across 2343 cpython files
      plus every other corpus dogfooded in this file). **FIXED** (shared
      `Main.java` IO layer, not Python3-specific — `readFile` now decodes
      with a `CharsetDecoder` set to `REPORT` on malformed/unmappable input
      instead of `String(byte[], Charset)`'s silent-replace default; throws
      `IOException`, caught by the existing per-file error handling in
      `runOneFile` — fails loudly instead of silently corrupting the file).
      `make test` 278/278 unaffected.
- [x] Indent-size/style conversion (Python analog of `MiscRuleCore
      #convertIndentation`) — see "Indent-Size/Style Conversion — DONE
      (RDD_KEY_237)" section above for the full design-decision/
      implementation narrative, bug fixes, and corpus validation.
- [x] Comment normalization (`normalize-comment-start-case`/
      `normalize-comment-end-period` for `#` comments, plus chain-grouping)
      — see "Comment Normalization — DONE (RDD_KEY_268)" section above.
