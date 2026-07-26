# STATE_PYTHON3.md — Python 3 JAR Support Tracker

Read `STATE_COMMON.md` first — it has the shared commit/ambiguity/testing
conventions this file assumes. `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` are
NOT required reading for this job.

---

## Purpose

Tracks implementation of Python 3 support in the deterministic JAR formatter
(`util/CodingStyle.md/formatter/`), per `STYLE_PYTHON3.md`. Python's
imperative surface differs enough from every currently-supported
brace-delimited language (significant whitespace, several bracket-content
categories with no C-family analog) that most rules are new, not inherited.

---

## Scope

`STYLE_PYTHON3.md` covers latest supported Python 3 (currently 3.15+);
Python 2 is not supported. **Indentation note carried from the style doc:**
unlike every other supported language, Python's indentation is semantically
load-bearing — the formatter must never change indentation depth in a way
that alters block membership. This constrains every rule below.

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
`ServerMode.java`/`Config.java`, per the routing `CLAUDE.md` table. This
job's own rule class, `rules/PythonSpecificRule.java`, exists only as a
boilerplate stub (constructor throws `UnsupportedOperationException`) — no
real logic yet.

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

`python_content_diff.py` — content-preservation checker for real-code testing,
modeled on `STATE_DATA_FORMATS.md`'s `*_content_diff.py` scripts (own
equivalent here, not that job's file, since Python has a real parser in its
own stdlib). Lives in `tools/verifiers/python_content_diff.py` (committed,
licensed project tooling, alongside the other jobs' checkers). Parses both
original and formatted files with stdlib `ast` and compares
`ast.dump(tree, include_attributes=False)` for structural equality
(position attributes stripped since formatting legitimately changes those).
Exit 0 if identical, 1 with a first-mismatch line printed if not, 2 if
either file fails to parse. Usage:
`python3 python_content_diff.py <original.py> <formatted.py>`.

**Known false-positive shape, triage manually, do not treat as a bug
without checking:** §3's import-sort pass legitimately reorders `from X
import name` sibling statements, which changes AST statement order (a real,
intended difference) even though the imported names themselves are
unchanged — confirmed during the `pallets/flask` run (9 of 9 initial
AST-diff mismatches were this shape). Before treating any AST-diff
mismatch as a bug, check whether it's solely an import-statement reordering
by comparing each file's own set of `(module, name, asname)` import tuples
pre/post format (order-independent) — if that set is unchanged, it's §3
working as intended, not corruption.

## Class Scoping (post Core/Curly/Indent/Tags refactor)

Python3 is the first real implementation to land in the `*Indent` skeleton
classes created by the refactor: `TokenizerIndent`, `FormatterIndent`,
`ScopePipelineIndent`, and `MiscRuleIndent` (all currently throw
`UnsupportedOperationException` pointing back at this file) are Python3's
landing spot — fill them in in place rather than creating new top-level
classes.

`DeclarationAlignmentRuleIndent`/`GetterSetterRuleIndent` skeletons also
exist and are available if Python3's own alignment-grid work (§1 bracket
complexity feeding §2 assignment alignment, §6 signature wrapping, §7
`match`/`case` compact-form alignment) turns out to overlap enough with the
curly-family shape to reuse. This is optional reuse, not a requirement —
Python's `match`/`case`, indentation-as-scope, and bracket-complexity work
may end up entirely bespoke inside the `*Indent` classes instead. Decide
per-rule as each is implemented, not up front.

Implementation order is unchanged by the refactor: tokenizer pass
(indentation-as-load-bearing architecture decision) → statement/indentation
skeleton → §1 bracket-complexity → §2–9 → fixtures → real-code testing.

## Open Questions

`STYLE_PYTHON3.md`'s own "Known Open Items" (§10) states its prior open
items (decorators, f-strings, type-hint signature wrapping) were already
resolved via Q&A and folded into §4–§6; nothing is left unresolved in the
style doc itself as of this session. One open item remains at the
implementation-architecture level, below.

**Indent-size/style conversion is per-block, not whole-file.** Unlike
Curly-family languages, Python's indentation is the only signal of block
depth (no braces to re-derive it from), so general scope-depth
reindentation is architecturally unavailable here, not merely hard — there's
nothing independent to recompute *from*. Indent-size/style conversion (the
Python analog of `MiscRuleCore.convertIndentation`) must therefore operate
per-block: rescale a block's indentation if its width is a clean multiple of
the presumed original unit, leave that block's lines untouched otherwise —
never reject the whole file for one inconsistent block, since CPython itself
only requires per-block internal consistency, not file-wide uniformity.
**Open:** exact block-boundary granularity (each `def`/`class`/
control-structure body independently vs. the whole contiguous indent-run at
a given depth) is undecided — resolve against real-world drift patterns in
the `psf/black`/`django` fixture repos once `FormatterIndent`/
`MiscRuleIndent` are actually implemented.

---

## Checklist

- [x] Tokenizer support pass (5 slices, all landed; class
      `TokenizerIndent`, not shared `TokenizerCore`). Covers whitespace,
      newlines, `#` comments, numbers, identifiers/keywords (own
      `KEYWORDS_PYTHON` set excluding context-sensitive soft keywords
      `match`/`case`/`_`/`type`), string literals, generic operator/punct
      fallback (Slice 1); triple-quoted strings as one opaque `STRING`
      token, satisfying RDD_KEY_186 at the tokenizer level (Slice 2); `:=`
      walrus as one `OP` token (Slice 3); f-string interpolation
      sub-tokenization (`FSTRING_START`/`MIDDLE`/`END`/`FORMAT_SPEC` token
      types, recursive field expression scan, `!conversion` handling) —
      **gap: a nested replacement field *within* a format spec
      (`f"{x:{width}}"`) is not recursively sub-tokenized**, only the outer
      field is (Slice 4); INDENT/DEDENT synthesis (CPython-style indent-
      width stack, merged bracket-nesting counter suppresses significance
      inside brackets/backslash-continuations) — no tabs/spaces consistency
      validation, assumes syntactically valid input (Slice 5). Verified
      each slice via smoke test + full `make test` (114/114 forward +
      idempotency, zero regressions — compile/link-health only until wired
      into live dispatch).
- [x] Basic statement/indentation formatting skeleton. **Landed:**
      `ScopePipelineIndent.process` tokenizes via `TokenizerIndent` and
      renders the token stream back to source verbatim (identity pass;
      `render` skips zero-text INDENT/DEDENT markers). `FormatterIndent
      .formatOne` delegates to it. Deliberately does NOT flip `python3` out
      of `Lang.SCAFFOLD_ONLY_LANGUAGES` at this stage (an identity pass
      isn't substantive rule logic yet). Verified via smoke test against
      `test/py_combined_inp.py`/`py_comments_inp.py` (byte-identical
      output) + full `make test` (114/114, zero regressions).
- [x] §1 (bracket complexity detector). **Landed:** new, self-contained
      `evaluator/PythonBracketComplexityEvaluator.java` (does not delegate
      to `ComplexityPaddingEvaluator` — that class has no dict/set-as-
      complexity-signal or Python bracket-kind handling). Three entry
      points: `isLooseParen` (§1.1/§1.2, generator-expr-as-argument),
      `isLooseBracket` (§1.1-§1.4, comprehension always loose, `:` slice
      segments evaluated independently, star-unpacking needs no special
      case since `*`/`**` are `OP` tokens), `isLooseBrace`/`classifyBrace`
      (§1.5, non-empty `{}` always loose, empty always tight/`DICT`,
      classified `DICT`/`SET` by top-level `:` presence). Own local
      bracket-depth counter (operates on an already-extracted slice, not
      the full token stream). Verified via an 18-case smoke test (every
      STYLE_PYTHON3.md §1.1-§1.5 worked example) + full `make test`
      (114/114, zero regressions — no caller wires this in yet; that lands
      with §2/§6/§7).
- [x] §2–9 rule-by-rule (each its own checkpoint commit):

      **§2 (Assignment Alignment).** Fixed a tokenizer gap found while
      implementing this: `TokenizerIndent.emitOperator` only consumed one
      char, so compound-assignment operators (`+=` etc.) came out as
      multiple tokens, invisible earlier but fatal to §2's grouping — fixed
      via new `MULTI_CHAR_OPS` array (mirrors `TokenizerCurly`'s). New
      `MiscRuleIndent.PyAssignment`/`renderPyGroup` (padded `name (op)=
      value`, no trailing-comment alignment per STYLE_PYTHON3.md §2) plus
      `ScopePipelineIndent.applyAssignmentAlignment`, a from-scratch
      NEWLINE/INDENT/DEDENT-aware logical-line splitter; groups break on
      blank line, comment, depth change, or unrecognized statement.
      **Gap:** multi-line RHS (bracket/backslash continuation) never a
      candidate; bare-IDENTIFIER-target-only (matches C-family's own
      exclusion of `self.x = 1`/tuple-assignment). Verified via 5-case
      smoke test + full `make test` (114/114, zero regressions).

      **§3 (Import Ordering).** New `MiscRuleIndent.PyImport` (`Kind` enum
      `FUTURE < IMPORT < FROM`) plus `ScopePipelineIndent.applyImportSort`,
      reusing a generalized shared `RawLine`/`splitRawLines` (shared with
      §2). Groups break on blank line, comment, depth change, or any
      non-import statement. **Discovered mid-implementation:** §3.1's own
      worked example requires within-clause name sorting too —
      `flushImportGroup` rebuilds a `FROM` import's own name-list span when
      out of order, even in an otherwise-unchanged/singleton group.
      **Gaps:** multi-physical-line import untouched; parenthesized
      `from X import (...)` rejected entirely; multi-module `import a, b`
      rejected/deferred (only single-module `import a.b.c[ as alias]`
      recognized). Verified via 6-case smoke test + full `make test`
      (114/114, zero regressions).

      **§4 (Decorators).** New `ScopePipelineIndent.applyDecoratorSpacing`
      + helpers (`applyBracketPadding`/`classifyLoose`/`isOpenBracketText`/
      `matchBracket`/`prevSignificant`/`normalizeGap`). For each
      single-physical-line `@` line: gap between `@` and the next token
      collapsed to zero; every `(`/`[`/`{` pair in the decorator's own
      expression (recursively) gets its delimiter gap normalized per
      `PythonBracketComplexityEvaluator`'s loose/tight verdict. A bare
      decorator (`@dataclass`, `@x.setter`) never enters bracket-padding,
      so `@property`/`@x.setter` get no special treatment. Multi-physical-
      line decorators completely skipped. **Gap: decorator-call overflow/
      line-wrapping not implemented** — no general line-length-based
      call-argument-wrapping mechanism exists anywhere yet (C-family's
      `enforceCallLineBreaking` is Curly-only, not ported). One bug fixed
      during verification: `normalizeGap` wrongly no-op'd on
      already-tight `from == to`, skipping the loose case's needed
      zero-width insertion; fixed to only guard on `from > to`. Verified
      via 7-case smoke test + full `make test` (114/114, zero regressions).

      **§5 (F-Strings).** New `ScopePipelineIndent.applyFStringSpacing` +
      helpers (`processFString`/`processField`/`isFStringConversion`/
      `addBraceTrim`/`isCloseBracketText`) — operates over the full token
      stream, not per-`RawLine`. `processField` tracks local bracket depth
      and recurses into nested f-strings. `addBraceTrim` unconditionally
      trims the gap after `{`; trims the gap before the close ONLY when no
      `!conversion`/format-spec tail follows (`f"{value !r}"` must keep
      that gap). **Gap: internal expression re-spacing (`f"{x  +  1}"` →
      `f"{x + 1}"`) out of scope** — the only inherited token-joining
      primitive (`MiscRuleCore#renderTokens`/`needsSpaceBetween`) is a
      C-family declarator-spacing helper that would wrongly collapse
      Python's `*` multiplication and has no notion of `**`/`//`/`:=`/
      `and`/`or`/`not`/comprehension `for`/`if`. **Also discovered:** when
      an f-string sits inside a span another pass (e.g. §2's assignment
      RHS) already fully replaces, that pass's wider, earlier-`start`
      replacement wins and this pass's narrower one is silently dropped,
      not corrupted. Verified via 8-case smoke test + full `make test`
      (114/114, zero regressions).

      **§6 (Function Signature Wrapping) — alignment-only slice.** The
      inline-vs-one-per-line *decision* has no home anywhere in
      `*Indent`/`*Curly` (same gap as §4's decorator overflow). New
      `MiscRuleIndent.PyParam` (name/type/default triples, trailing-comma
      flag) + `renderPySignatureGroup` (name column padded to widest; `=`
      column padded only across params with BOTH a type hint and a
      default). A param with no type hint skips the `:` segment entirely
      (documented partial-row shape, not a bug). New
      `ScopePipelineIndent.applySignatureAlignment`/`trySignatureGroup`/
      `classifySignatureParam`/`trimEndIdx` — requires the `def`'s
      parameter list already one-parameter-per-line; any deviation (inline
      first param, multiple params per line, per-param trailing comment, a
      param spanning multiple lines) returns null and leaves the *whole*
      signature untouched. `classifySignatureParam`'s `:`/`=` search
      tracks local bracket depth so nested type hints (`List[Dict[str,
      int]]`) work correctly. Return-type arrow untouched by construction.
      **Gap: inline-vs-one-per-line decision not implemented** (blocked on
      same missing wrap-decision infra as §4). Verified via 5-case smoke
      test + full `make test` (114/114, zero regressions).

      **§8 (Single-Statement Bodies) — a join operation** (unlike §2-§7,
      which never join/split lines). Read `BlockStructureRule
      .collapseSingleExpressionBlocks`/`isSingleStatementBody` (STYLE.md
      §10, the C-family precedent) first. `ScopePipelineIndent` gained a
      `lineLength` constructor param (new `Config.DEFAULT_LINE_LENGTH =
      100`) plus `applySingleStatementBody`/
      `classifySingleStatementHeaderColon`/`bodyOpensNewBlock`/
      `physicalLineLength`/`containsComment` and
      `SINGLE_STMT_HEADER_KEYWORDS` (`if`/`elif`/`else`/`while`/`for`;
      `case` delegates to §7's `classifyCaseLine`; `def`/`class`/`try`/
      `except`/`finally`/`with` never members, satisfying §8's "never
      applies" list). A header qualifies only when single physical line,
      first token a qualifying keyword, genuinely block-form. Body line
      must be one depth deeper, single-line, non-blank/non-comment, no
      trailing comment, not itself open a further nested block; the
      following line must sit at a shallower depth. Replacement spans
      `[header.start, bodyContentEnd)`, excluding the body's own trailing
      NEWLINE. Overflow leaves block form untouched. **Ambiguity resolved
      conservatively:** a body containing `lambda` is always treated as
      "opens a new block" (never joined), since lambda-aware depth
      tracking wasn't built. **A nested compound statement's own header
      still independently gets its own join opportunity** (e.g. `if x:\n
      if y:\n  return 1` → `if x:\n if y: return 1`). Verified via a
      17-case smoke test + full `make test` (114/114, zero regressions).
      **Gaps:** header/body spanning multiple physical lines never a
      candidate; body with own trailing comment conservatively skipped;
      never expands an already-compact line back to block form; no
      `;`-chaining ever produced. Same cross-pass render-merge safety
      as §5's documented interaction, not independently re-verified.

      **§7 (Structural Pattern Matching) — `:` column alignment-only
      slice.** New `ScopePipelineIndent.CaseLine`/
      `applyCaseColonAlignment`/`classifyCaseLine`/`flushCaseGroup` —
      reuses §4-§6's `normalizeGap`/`verbatimLineText`/`isOpenBracketText`/
      `isCloseBracketText`/`trimEndIdx`. `classifyCaseLine` checks literal
      text `"case"` (a context-sensitive soft keyword tokenized as plain
      `IDENTIFIER`). The header `:` is found via a bracket-depth-0 scan
      after `case`, correctly skipping a mapping pattern's own colon and
      including any guard clause. `compact` reflects the body's existing
      shape as written (never decided). Grouping mirrors §2/§3.
      **All-or-nothing** per §7: `flushCaseGroup` emits zero replacements
      for the whole group if any member is block-form; for an all-compact
      group, only the gap before `:` is padded. **Verified as pre-existing
      gaps, not new:** or-pattern `|`/guard-clause spacing rely on the
      same "no general expression-respacing" gap §5 documented;
      deconstruction/sequence/mapping pattern content not repadded via
      `PythonBracketComplexityEvaluator` (exists but not wired into
      case-pattern rendering) — narrowing to `:`-alignment-only was this
      checkpoint's pre-agreed scope. **Closing-comment mechanism confirmed
      absent, not guessed** — grepped the whole tree, zero Python3
      support, building it was out of scope. Verified via 8-case smoke
      test + full `make test` (114/114, zero regressions).

      **§9 (Control Flow Blank Lines) — last §2-9 sub-item, checklist item
      now complete.** New `ScopePipelineIndent.ControlFlowFrame`/
      `applyControlFlowBlankLines`/`isDefHeaderLine`/`previousContentLine`/
      `isUnconditionalExitLine`/`insertBlankLineBefore`. §9.1 ported
      faithfully from `MiscRuleCurly#insertBlankLineBeforeReturn`: blank
      line before a `return` that's the first token of its logical line,
      when the innermost enclosing frame is a function body that has
      already seen a statement (does NOT verify `return` is the body's
      textually final statement, matching the C-family reference).
      **§9.2 could not be ported — no C-family mechanism exists to port**
      (`BlockStructureRule.placeElseOnOwnLine` only ever *preserves*
      blank-line state); implemented directly from STYLE_PYTHON3.md §9.2's
      text: blank line before any `elif`/`else` whose nearest preceding
      non-blank/non-comment logical line has `return`/`break`/`continue`
      (never `raise`) as its first token. Both halves only ever ADD a
      missing blank line, never remove one. A comment-only line
      immediately preceding a qualifying line is conservatively left
      untouched. **Gaps:** multi-physical-line `def` header never
      recognized as function-body-opening; semicolon-chained statements
      never recognized by either half; a §8-compact preceding block ending
      in return/break/continue never recognized by §9.2; `try`/`except`/
      `finally` blank-line placement entirely out of scope. Verified via a
      14-case smoke test + full `make test` (114/114 forward + idempotency,
      zero regressions).

- [x] Local test fixtures authored and registered: `py_combined_inp/out.py`
      and `py_comments_inp/out.py` in `test/`, documented in
      `test/README.txt`. **Activated (2026-07-22):** both verified against
      the actual JAR and uncommented in the Makefile's `INP_FILES` (no
      longer scaffold-placeholder-only). Each pair's `_out.py` (originally
      hand-authored speculatively before real logic existed) was updated to
      match actual current-scope output — every diff traced back to an
      already-documented gap above (no general expression/operator-
      respacing outside decorator calls/signatures/case colons; no
      inline-vs-one-per-line overflow-wrap decision; no automatic blank-line
      insertion between import groups; no comment capitalization/period-
      normalization pass for Python3 at all, unlike the C-family) — not a
      formatter bug, confirmed via direct source inspection. Both fixtures
      pass forward + idempotency under `make test` (116/116 forward +
      116/116 idempotency, zero regressions).
- [~] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      `STYLE_PYTHON3.md`'s listed test-fixture repos (`python/cpython`,
      `pallets/flask`, `django/django`, `psf/black`, `pallets/click`).
      **`pallets/flask` — DONE (first Python3 dogfood run).** 83 `.py` files
      (24 `src/flask/`, 41 `tests/`, 18 `examples/`/`docs/`, full tree, no
      exclusions). Zero crashes on forward pass.

      **Four real bugs found via non-idempotency (`diff -r round1 round2`),
      none via `py_compile`/AST-diff (both clean once fixed):**
      1. `ScopePipelineIndent.render`'s replacement-merge loop advanced its
         cursor `r` only on exact `start == i`; two legitimately
         overlapping replacements (§8 join + §2 alignment on the same
         nested statement) left a stale entry that permanently stalled `r`,
         silently dropping every later replacement in the file. Fixed by
         skipping stale entries instead of stalling. Fixture
         `real_code_regressions_78_{inp,out}.py`.
      2. §6 `trySignatureGroup` split params on raw `NEWLINE` only, not
         bracket-depth-aware — a type hint spanning multiple physical
         lines via a still-open nested bracket was misclassified as bogus
         parameters instead of triggering the "leave whole signature
         untouched" gap, producing non-convergent trailing whitespace.
         Fixed by only splitting at depth-0 `NEWLINE`s.
      3. §9.2's zero-width blank-line insertion and §8's join could both
         start at the same token index; stable sort left the zero-width
         entry second, so §8's wider replacement jumped over it, silently
         dropping the blank line (a forward-pass bug, not just
         idempotency). Fixed by sorting equal-`start` zero-width entries
         first. Bugs 2+3 combined into fixture
         `real_code_regressions_79_{inp,out}.py`.

      **Final numbers (after all four fixes, full 83-file corpus):**
      zero crashes; `diff -r round1 round2` empty (83/83); `python3.12 -m
      py_compile` clean on all 83 (python3.6 NOT viable — flask uses
      walrus/`from __future__ import annotations`, post-3.6 syntax); new
      `python_content_diff.py` (see "Dogfood Tooling" below) clean on all
      83 after manually triaging 9 initial reports, all §3 import-reorder
      false positives (verified via each file's `(module, name, asname)`
      tuple set being unchanged) — zero true AST-shape mismatches. `make
      test`: 128/128 forward + 128/128 idempotency.

      **`pallets/click` — DONE.** 78 `.py` files (fresh clone `/tmp/click`).
      Zero crashes on forward pass.

      **One bug found via non-idempotency:** `ScopePipelineIndent
      .applyBracketPadding` (§4) couldn't distinguish an f-string field's
      own `{`/`}` from an actual dict/set literal, padding an f-string
      nested in a decorator's lambda default arg (`tests/test_basic.py`'s
      `@click.custom_version_option(lambda ctx: f"{ctx.info_name} 1.0")` →
      `f"{ ctx.info_name }"` on forward pass; §5 trimmed it back next
      round, so it only surfaced as non-idempotency). Fixed by skipping a
      `{`/`}` pair whenever `{` is immediately preceded by
      `FSTRING_START`/`FSTRING_MIDDLE`. Fixture
      `real_code_regressions_80_{inp,out}.py`.

      **Final numbers:** zero crashes; idempotency empty (78/78) after fix;
      `python3.12 -m py_compile` clean (same as 78 unmodified originals);
      formatted package imports cleanly (`import click`, version `8.4.0`);
      a representative pytest subset (`test_basic.py` — where the bug was
      found — plus `test_arguments.py`/`test_options.py`) passed 857/857
      (full `tests/` run not completed, timed out on an unrelated
      interactive test, not chased further). `make test`: 129/129 forward
      + 129/129 idempotency.

      **`psf/black` — DONE.** Fresh clone (`/tmp/black`), 338 `.py` files
      (`src/`, `tests/` incl. `tests/data/`'s curated edge-case corpus,
      `scripts/`).

      **Forward pass: 1 crash (337/338) — FIXED.**
      `tests/data/cases/pep_701.py` threw `IndexOutOfBoundsException` from
      `ScopePipelineIndent.processField`/`applyFStringSpacing`. Minimal
      repro: `f"{1}\{{"`. Root cause: `TokenizerIndent.emitFString`'s
      backslash-escape handling always skipped 2 chars even when next was
      `{`/`}`, orphaning the second `{` of a doubled-brace escape right
      after a field close. Verified against real CPython semantics
      (`f"\{y}"` → real field; `f"{1}\{{"` → `'1\{'`, no dangling field).
      Fixed: only skip the backslash itself when followed by `{`/`}`, so
      that char is re-evaluated fresh. `make test` 163/163 forward +
      163/163 idempotency. Fixture: `real_code_regressions_114_{inp,out}.py`
      (identity-pass).

      **Round2 idempotency (337 files): 3/337 differed — 2 bugs, both
      fixed in a follow-up session:**
      1. **§7/§8 join-then-align ordering.** Block-form `match`/`case`
         skips §7's colon-alignment on round1, then §8 joins each case
         body onto its header; round2 sees the now-compact form and
         applies column alignment round1 never had. Affected:
         `tests/data/cases/pattern_matching_simple.py`,
         `tests/data/line_ranges_formatted/pattern_matching.py`. FIXED:
         `classifyCaseLine` gained `tryQualifyJoinBody` to predict within
         the same pass whether a case will qualify for §8's join, so
         `flushCaseGroup` bakes correct `:`-column padding up front;
         `applyCaseColonAlignment`'s grouping must skip each virtualJoin
         case's own body line; `applySingleStatementBody` skips headers
         already handled by §7; if §7's padding would push past
         `line-length`, the group falls back to §8's plain unpadded join.
         Fixture `real_code_regressions_115_{inp,out}.py`. `make test`:
         164/164 forward + 164/164 idempotency.
      2. **§6 multi-line union-type-hint gap violated + unbounded
         trailing-whitespace growth.** `tests/data/cases/
         pep604_union_types_line_breaks.py`: a `|`-union type wrapped
         across lines with no enclosing bracket had each `| TypeN`
         continuation misclassified as its own parameter, growing padding
         every round (confirmed non-convergent over 3 rounds). FIXED:
         `classifySignatureParam` now rejects any segment whose first
         token isn't a valid parameter start (`IDENTIFIER`, or
         `*`/`**`/`/`); a leading `|` means continuation not parameter, so
         the whole signature is left untouched per §6's documented gap.
         Fixture `real_code_regressions_116_{inp,out}.py` (identity-pass).
         `make test`: 165/165 forward + 165/165 idempotency.

      **`python3.12 -m py_compile` on all 337 round1 files:** clean except
      one pre-existing failure (`tests/data/cases/
      trailing_comma_optional_parens3.py`, present identically on the
      unmodified original — a deliberately-invalid black test fixture).

      **`python_content_diff.py` on all 337 round1 files: 22 mismatches** —
      13 are the §3 import-reorder false positive; 8 are `rc=2` parse
      failures on deliberately-invalid/post-3.12-syntax fixtures (expected,
      not re-confirmed per-file); **2 are genuine content-corruption bugs,
      both in `applyFStringSpacing`'s `addBraceTrim` (§5) — FIXED
      (follow-up session):**
      - **(a) Nested-brace field fusion.** A field followed by a nested
        `{` (e.g. `f"{ {a for a in (1, 2, 3)}}"`) had its close-gap trimmed
        to zero, fusing with the following literal `{{` and silently
        deleting the nested expression (`ast.dump` confirmed the node is
        gone). Repro: `tests/data/cases/fstring.py` line 8. Fixed:
        `addBraceTrim` normalizes that gap to exactly one space whenever
        the next significant token's text is `{`.
      - **(b) Self-documenting `{expr=}` debug fields.** The leading gap
        was trimmed even though Python reproduces `expr`'s exact original
        whitespace verbatim at runtime for a `=`-suffixed field. Repro:
        `tests/data/cases/preview_long_strings.py` line 327. Fixed:
        `addBraceTrim` detects a bare trailing `=` OP token as the field's
        last significant token and skips all gap-trimming for that field.

      Both verified via `python_content_diff.py` and 2-round idempotency.
      Combined into `real_code_regressions_117_{inp,out}.py`
      (identity-pass, both bugs live in the same method). `make test`:
      166/166 forward + 166/166 idempotency.

      **All four `psf/black` bugs now fixed;** a full 338-file corpus
      re-run was deliberately deferred (each fix independently verified
      against its own repro plus AST-diff/idempotency; `make test` stayed
      green throughout) — a future session may still re-run the full
      corpus for further confidence.

      **`django/django` — DONE.** Reused existing checkout at `/tmp/django`
      (2927 `.py` files, full tree). Batch-formatted in one `xargs`
      invocation per round (`--preserve-tree --root /tmp/django --out
      <scratch>/round1`, then `--root <scratch>/round1 --out
      <scratch>/round2`). Zero crashes on forward pass.

      **One bug found via non-idempotency, but actual content corruption on
      the forward pass itself:** `django/utils/json.py`'s `case
      Sequence():  # str and bytes were already handled.` — a §8
      single-statement-body `match`/`case` header carrying its own
      trailing comment still qualified for the join (only a *body*
      trailing comment was guarded against, not a *header* one).
      `applySingleStatementBody`'s `headerText` construction stops at the
      header's own `:`, silently deleting the comment on the forward pass
      itself (genuine data loss). Root cause:
      `classifySingleStatementHeaderColon`'s loop and `classifyCaseLine`'s
      compact/`virtualJoin` computation both explicitly permitted a
      trailing comment after the header colon to still qualify, with no
      corresponding "don't lose it" step at join time. Fixed:
      `classifySingleStatementHeaderColon` now returns `-1` immediately on
      any header trailing comment; `classifyCaseLine` tracks
      `headerHasTrailingComment` separately from `compact` and never sets
      `virtualJoin` when true; the `case` delegation now additionally
      requires `c.virtualJoin` before returning a joinable `colonIdx` —
      mirrors the join's existing conservative posture for a *body*
      trailing comment (`containsComment` in `tryQualifyJoinBody`), just
      applied on the header side where it had been missed. Fixture
      `real_code_regressions_127_{inp,out}.py` (also guards against
      disabling joining entirely). `make test`: 176/176 forward + 176/176
      idempotency.

      **Final numbers (after the fix, full 2927-file corpus, freshly
      re-run):** zero crashes; idempotency empty (2927/2927);
      `python3.12 -m py_compile`: exactly one syntax error
      (`tests/test_runner_apps/tagged/tests_syntax_error.py`, present
      identically on the unmodified original — deliberately-invalid test
      fixture, not formatter-induced); `python_content_diff.py`: 1 `rc=2`
      (same deliberately-invalid fixture) and 41 `rc=1` AST-diff
      mismatches, all confirmed via the documented triage method to be
      solely §3's import-sort reordering — zero true AST-shape mismatches
      remaining after the one fix above.

      **`python/cpython` — dogfood run DONE, categorized; clusters 1-3
      FIXED, cluster 4 (idempotency-only) NOT YET FIXED.**
      Fresh shallow clone `/tmp/cpython` (`--depth 1`), 2343 `.py` files,
      batched per top-level subdir (`Doc`/`Lib`/`Mac`/`Misc`/`Modules`/
      `Parser`/`PC`/`PCbuild`/`Platforms`/`Programs`/`Tools`) through
      `--preserve-tree --root /tmp/cpython --out <scratch>/round1`, then
      round2 from round1's output. `python3.12 -m py_compile` used as the
      compile-check (python3.6 not viable against modern cpython syntax).

      Stats: 1 crash / 2343 files; 19 idempotency mismatches; `py_compile`
      errors identical before/after (1 in both — `Lib/traceback.py:21`'s
      `lazy import _colorize`, a pre-existing not-yet-standard-Python
      syntax experiment in cpython's own dev tree, present identically in
      the unformatted original — not formatter-induced). Zero new syntax
      errors introduced by formatting.

      No fixes attempted yet — clusters below are triage only, sorted
      **most-valuable-to-fix first** (value = criticality weighed against
      estimated difficulty):

      1. **[CRITICAL] f-string nested-format-spec crash — FIXED.**
         (`IndexOutOfBoundsException`, `ScopePipelineIndent.processField`
         line ~802) — `Lib/test/test_fstring.py` crashed outright (whole
         file skipped, no output at all) on cases like
         `f'{2:{"{"}>10}'`/`f'{3:{"}"}>10}'`/
         `f'{10:#{3 != {4:5} and width}x}'` (`test_format_specifier_
         expressions`). **Actual root cause was narrower than the initial
         triage guess** — not the general "nested replacement field inside
         a format spec isn't recursively sub-tokenized" gap (that
         remains a real, separate, still-open limitation), but specifically:
         `TokenizerIndent.emitFStringFormatSpec`'s brace-depth counter
         scanned raw characters without skipping quoted-string content, so
         a literal `{`/`}` *inside a nested field's own string-literal
         expression* (e.g. the `"{"` in `{2:{"{"}>10}`) miscounted nesting
         depth — the nested field's real closing `}` then only decremented
         the phantom depth instead of closing it, so the scan for the
         whole spec's true closing `}` ran past the actual field end,
         producing a single `FSTRING_FORMAT_SPEC` token spanning to EOF
         with no `FSTRING_END` ever emitted, which is what crashed
         `processField` downstream. **Fixed:** added
         `skipNestedStringLiteral` — skips a quoted string's content
         (`\`-escapes, triple-quotes honored, mirroring
         `emitSimpleString`/`emitTripleQuotedString`'s own scanning)
         whenever a quote is seen at `depth > 0` inside
         `emitFStringFormatSpec`'s brace counter, so embedded braces in a
         nested field's string literal never reach the depth count; at
         `depth == 0` a quote is just literal format-spec text (not Python
         syntax) so needs no special handling. Verified against all 5
         crashing minimal cases plus the two multi-level-nesting cases
         from `Lib/test/test_fstring.py` (all now format without crashing,
         round1==round2, valid Python per `python3.12 -m py_compile` —
         the file's one remaining `py_compile` error, `Sorry: ValueError:
         field 'value' is required for Constant`, reproduces identically
         on the unformatted original, a pre-existing `python3.12`/AST-tool
         quirk unrelated to this fix). Fixture
         `real_code_regressions_133_{inp,out}.py`. `make test`: 182/182
         forward + 182/182 idempotency, zero regressions.
      2. **[IDEMPOTENCY] [FIXED] §3 import-sort: same-module multi-statement group
         order unstable on first pass** (16 files: `Lib/random.py` lines
         53-56 confirmed as the clean minimal case — four separate `from
         math import ...` statements; also `Lib/ssl.py`, `statistics.py`,
         `typing.py`, `turtle.py`, `xml/sax/expatreader.py`,
         `ctypes/__init__.py`, and 9 `Lib/test/*`/`idlelib`/`Mac/Tools`
         files). Round1 doesn't fully alphabetize inter-statement order
         when one group has multiple `from X import ...` lines for the
         *same* `X` (each statement's own within-clause name sort IS
         already correct); round2 (reformatting round1's output)
         self-corrects. Root cause guess: `MiscRuleIndent.PyImport
         .compareTo` (line ~124) is keyed on `this.names`/`other.names`,
         populated from the as-parsed (pre within-clause-sort) name order
         at `applyImportSort`/`flushImportGroup`'s construction site —
         only after one round-trip (names already alphabetized in the
         emitted text) does re-parsing yield a `PyImport.names` matching
         the final sorted form. Reproduced identically at
         `indent-size=2` (not indent-sensitive — pure sort-key bug).
         Likely a **narrow, one-comparator-key fix**: sort `names` (or use
         the already-sorted rendered form) before using it as the
         group-order comparator key, rather than after.

         **FIXED**: root cause confirmed exactly as guessed.
         `PyImport.compareTo` now sorts a copy of each side's `names`
         before the element-by-element comparison (leaving `names` itself
         untouched, in source order, for `sortedNameUnits`'s own separate
         within-clause-rebuild use) — matches §3.1 point 3's "sort by the
         first imported name" read as "the first name after within-clause
         alphabetization," so a same-module multi-statement group now
         sorts correctly on the very first pass instead of needing a
         round-trip. Verified against `Lib/random.py:53-56` directly from
         the `/tmp/cpython` checkout (correctly ordered and idempotent on
         the first format) plus a minimal repro at `indent-size=2` too
         (config-insensitive, as expected). Fixture:
         `real_code_regressions_137`. `make test`: 186/186 forward +
         186/186 idempotency.
      3. **[IDEMPOTENCY] [FIXED] §7/§8 join-then-align ordering, recurrence
         adjacent to a preceding block-form `case`** (2 files:
         `Lib/turtle.py` ~line 3930, `Lib/typing.py` ~line 2974). A run of
         already-compact `case X: stmt` lines stays unpadded on round1
         (matching the original's own unaligned form) but gets `:`-column-
         aligned on round2 — both valid Python, cosmetic-only. This is a
         second occurrence of the bug class already fixed once for
         `psf/black` (fixture `real_code_regressions_115`,
         `classifyCaseLine`'s `tryQualifyJoinBody`); the new trigger in
         both files is a **preceding multi-physical-line block-form `case
         (...)`** immediately before the compact run, which the existing
         fix's group-boundary computation doesn't appear to fully cover.
         Reproduced identically at `indent-size=2` (not indent-sensitive).
         Likely a **narrow extension** of the existing fix
         (`classifyCaseLine`/`flushCaseGroup`/`applyCaseColonAlignment`),
         not a new mechanism.

         **FIXED**: root cause was actually one step removed from the
         initial "preceding block-form `case (...)`" guess -- the
         preceding block-form header correctly breaks the group boundary
         (`classifyCaseLine` already returns `null` for it, since it's
         `multiPhysicalLine`) and has no bearing on the bug. The real
         cause: every member AFTER it (`VAR_POSITIONAL`/`KEYWORD_ONLY`/
         `VAR_KEYWORD`/`_`) is itself `virtualJoin`-eligible (each is
         block-form-as-written with a one-statement body), and `case _:`'s
         very short pattern needs enough padding to match its much longer
         `VAR_POSITIONAL`-length sibling that the padded+joined line
         overflows `line-length` -- correctly making round1's `§7`
         abandon alignment for the whole group (all-or-nothing), leaving
         `§8` to join each member individually, unaligned. A naive round2
         then saw those now-already-compact members and realigned them,
         because `flushCaseGroup`'s pre-commit length-budget check only
         ever examined `virtualJoin` members, never already-compact ones
         -- so the very over-length padding round1 correctly rejected got
         silently applied on round2. Fixed by extending that check
         uniformly to every group member regardless of shape (added a
         `lineEnd` field to `CaseLine` so a compact member's own
         post-alignment full-line length can be computed the same way a
         virtualJoin member's already was). Verified against both cited
         files directly from the `/tmp/cpython` checkout (idempotent,
         `python3.12 -m py_compile`-clean save for `typing.py`'s
         pre-existing unrelated `lazy import` syntax-experiment error,
         confirmed identical on the unformatted original). Fixture:
         `real_code_regressions_138`. `make test`: 187/187 forward +
         187/187 idempotency.
      4. **[IDEMPOTENCY] §4/§5 decorator-call bracket-padding leaks into a
         nested f-string field's own braces** (1 file:
         `Lib/test/test_ctypes/test_generated_structs.py` lines 278, 284).
         `@register(f'Struct331_{signedness}{n}', set_name=True)` (no
         internal spaces in the original) gets the f-string field
         loose-padded to `{ n }` on round1 alongside the outer call parens
         (`@register( f'...', set_name=True )`); round2's §5
         `addBraceTrim` then trims the f-string field back to `{n}` but
         leaves the outer paren padding — non-idempotent (both rounds stay
         valid Python). Same bug class already fixed for `pallets/click`
         (fixture `real_code_regressions_80` — `applyBracketPadding`
         couldn't distinguish an f-string field's own `{`/`}` from a real
         dict/set literal), but that fix was scoped to the decorator's own
         **top-level** call arguments; here the f-string is a call
         argument **one level deeper** inside the decorator call, where
         the existing FSTRING_START/MIDDLE-adjacency guard evidently isn't
         reached. Likely **narrow** — apply the same guard one level
         deeper in `applyBracketPadding`'s recursive descent into
         call-argument lists. Lowest priority of the four: narrowest
         trigger (f-string literal as a decorator-call argument),
         cosmetic-only impact observed.

      Next free fixture number unaffected (no fixtures added yet — none of
      the four clusters above has been fixed). Full corpus re-run deferred
      until fixes land, same pattern as every prior dogfood entry in this
      file.
