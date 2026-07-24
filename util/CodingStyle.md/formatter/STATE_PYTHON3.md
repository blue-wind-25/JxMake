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

`python_ast_diff.py` — content-preservation checker for real-code testing,
modeled on `STATE_DATA_FORMATS.md`'s `*_content_diff.py` scripts (own
equivalent here, not that job's file, since Python has a real parser in its
own stdlib). Lives in `tools/syntax_checker/python_ast_diff.py` (committed,
licensed project tooling, alongside the other jobs' checkers). Parses both
original and formatted files with stdlib `ast` and compares
`ast.dump(tree, include_attributes=False)` for structural equality
(position attributes stripped since formatting legitimately changes those).
Exit 0 if identical, 1 with a first-mismatch line printed if not, 2 if
either file fails to parse. Usage:
`python3 python_ast_diff.py <original.py> <formatted.py>`.

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

      **§2 (Assignment Alignment).** Fixed a real tokenizer gap found while
      implementing this: `TokenizerIndent.emitOperator` only consumed one
      char, so compound-assignment operators (`+=` etc.) came out as
      multiple tokens — invisible to earlier work but fatal to §2's
      grouping. Fixed via a new `MULTI_CHAR_OPS` array (mirrors
      `TokenizerCurly`'s). New `MiscRuleIndent.PyAssignment`/
      `renderPyGroup` (padded `name (op)= value`, no trailing-comment
      column alignment — not called for in STYLE_PYTHON3.md §2) plus
      `ScopePipelineIndent.applyAssignmentAlignment`, a from-scratch
      NEWLINE/INDENT/DEDENT-aware logical-line splitter. Groups break on
      blank line, comment, depth change, or unrecognized statement.
      Replacements cover only the `target...value` span, leaving
      indentation/comments untouched. **Gap: multi-line RHS (bracket or
      backslash continuation) is never classified as a candidate** and is
      left completely untouched; bare-IDENTIFIER-target-only restriction
      matches the C-family's own (`self.x = 1`/tuple-assignment correctly
      excluded). Verified via 5-case smoke test + full `make test`
      (114/114, zero regressions; `python3` still scaffold-only).

      **§3 (Import Ordering).** New `MiscRuleIndent.PyImport` (`Kind` enum
      `FUTURE < IMPORT < FROM`, sort key = kind then module name then
      per-name comparison) plus `ScopePipelineIndent.applyImportSort`,
      reusing a generalized shared `RawLine`/`splitRawLines` (now shared
      with §2). Groups break on blank line, comment, depth change, or any
      non-import statement. **Discovered mid-implementation:** §3.1's own
      worked example requires within-clause name sorting too (not just
      inter-statement order) — `flushImportGroup` independently rebuilds a
      `FROM` import's own name-list span when out of order, even in an
      otherwise-unchanged/singleton group. **Gaps:** any multi-physical-line
      import (bracket/backslash continuation) is left untouched;
      parenthesized `from X import (...)` is rejected entirely (even
      single-physical-line); multi-module `import a, b` on one line is
      rejected/deferred (only single-module `import a.b.c[ as alias]`
      recognized). Verified via 6-case smoke test + full `make test`
      (114/114, zero regressions).

      **§4 (Decorators).** New `ScopePipelineIndent.applyDecoratorSpacing`
      + helpers (`applyBracketPadding`/`classifyLoose`/`isOpenBracketText`/
      `matchBracket`/`prevSignificant`/`normalizeGap`) — no new
      `MiscRuleIndent` method needed. For each single-physical-line `@`
      line: whitespace between `@` and the next token collapsed to zero
      (tight bind, resolves the "stray-space" ambiguity as "any whitespace
      removed"); every `(`/`[`/`{` pair anywhere in the decorator's own
      expression (recursively) gets its immediate delimiter gap normalized
      per `PythonBracketComplexityEvaluator`'s loose/tight verdict —
      delimiter-gap-only, content spacing untouched. A bare decorator
      (`@dataclass`, `@x.setter`) never enters bracket-padding, so
      `@property`/`@x.setter` get no special treatment, per §4's "no
      special getter/setter alignment" text. Multi-physical-line decorators
      are completely skipped. **Gap: decorator-call overflow/line-wrapping
      is not implemented** — no general line-length-based call-argument-
      wrapping mechanism exists anywhere in this codebase yet (the
      C-family's `enforceCallLineBreaking` is Curly-only, not ported).
      One real bug found+fixed during verification: `normalizeGap`'s first
      draft treated `from == to` (already-tight, no existing whitespace) as
      always a no-op, which wrongly skipped the loose case's needed
      zero-width insertion; fixed to only guard on `from > to`. Verified
      via 7-case smoke test + full `make test` (114/114, zero regressions).

      **§5 (F-Strings).** New `ScopePipelineIndent.applyFStringSpacing` +
      helpers (`processFString`/`processField`/`isFStringConversion`/
      `addBraceTrim`/`isCloseBracketText`) — operates over the full token
      stream, not per-`RawLine`. Confirmed first that
      `TokenizerIndent.emitFStringField`'s `{...}` content already
      tokenizes as ordinary expression tokens. `processField` tracks local
      bracket depth (mirrors the tokenizer's own field-scan depth) and
      recurses into nested f-strings. `addBraceTrim` unconditionally trims
      the gap after `{`; trims the gap before the close ONLY when there's
      no `!conversion`/format-spec tail — discovered via the worked example
      `f"{value !r}"`, which requires that boundary gap be left alone when
      a conversion/spec follows (first draft trimmed it unconditionally and
      failed this case). **Gap: internal expression re-spacing (e.g.
      `f"{x  +  1}"` → `f"{x + 1}"`) is out of scope** — the only inherited
      token-joining primitive (`MiscRuleCore#renderTokens`/
      `needsSpaceBetween`) is a C-family declarator-spacing helper that
      would wrongly collapse Python's `*` multiplication and has no notion
      of `**`/`//`/`:=`/`and`/`or`/`not`/comprehension `for`/`if`; building
      a general primitive was judged out of §5's narrow scope. **Also
      discovered:** when an f-string sits inside a span another pass (e.g.
      §2's assignment RHS) already fully replaces, that pass's wider,
      earlier-`start` replacement wins and this pass's own narrower one for
      that occurrence is silently dropped (not corrupted) — confirmed via a
      dedicated smoke case; an f-string NOT nested in another pass's span
      trims normally. Verified via 8-case smoke test + full `make test`
      (114/114, zero regressions).

      **§6 (Function Signature Wrapping) — alignment-only slice.**
      Confirmed first that the inline-vs-one-per-line *decision* has no
      home anywhere in `*Indent`/`*Curly` (same gap §4 found for decorator
      overflow — the C-family's `enforceCallLineBreaking` is Curly-only).
      New `MiscRuleIndent.PyParam` (name/type/default token triples,
      trailing-comma flag) + `renderPySignatureGroup` (name column padded
      to widest; `=` column padded only across params that have BOTH a
      type hint and a default — a typed-but-defaultless param's type is
      never padded, per the worked example). A param with no type hint
      skips the `:` segment entirely (read literally as "omitted", not
      fake-padded) — a documented partial-row shape, not a bug. New
      `ScopePipelineIndent.applySignatureAlignment`/`trySignatureGroup`/
      `classifySignatureParam`/`trimEndIdx` — the first pass to target a
      `multiPhysicalLine` `RawLine` deliberately: requires the `def`'s
      parameter list to already be one-parameter-per-line (`(` alone on
      its line, `)` alone on its line, every line between exactly one
      param); any deviation (inline first param, multiple params per line,
      per-param trailing comment, a param itself spanning multiple lines)
      returns null and leaves the *whole* signature untouched.
      `classifySignatureParam`'s `:`/`=` search tracks local bracket depth
      so a nested type hint like `List[Dict[str, int]]` is handled
      correctly. Return-type arrow is untouched by construction (nothing
      after the closing `)` is scanned). **Gap: the inline-vs-one-per-line
      decision itself is not implemented** (blocked on the same missing
      general wrap-decision infra as §4). **Also excluded, mirroring
      §2-§5's precedent:** inline first parameter sharing `(`'s line;
      multiple params on one physical line inside an otherwise-broken
      signature; per-parameter trailing comments; a parameter whose default
      or type hint itself spans multiple physical lines — each causes the
      whole signature to be left untouched. Verified via 5-case smoke test
      + full `make test` (114/114, zero regressions).

      **§8 (Single-Statement Bodies) — a join operation** (unlike §2-§7,
      which never join/split lines). Read `BlockStructureRule
      .collapseSingleExpressionBlocks`/`isSingleStatementBody` (STYLE.md
      §10, the C-family precedent) first. `ScopePipelineIndent` gained a
      `lineLength` constructor param (new `Config.DEFAULT_LINE_LENGTH =
      100` constant) plus `applySingleStatementBody`/
      `classifySingleStatementHeaderColon`/`bodyOpensNewBlock`/
      `physicalLineLength`/`containsComment` and a
      `SINGLE_STMT_HEADER_KEYWORDS` set (`if`/`elif`/`else`/`while`/`for`;
      `case` delegates to §7's `classifyCaseLine`; `def`/`class`/`try`/
      `except`/`finally`/`with` are never members, satisfying §8's "never
      applies" list by construction). A header qualifies only when single
      physical line, first token a qualifying keyword, and genuinely
      block-form (already-compact lines like `if x: return y` are left
      alone). The body line must exist one depth deeper, be single-line,
      non-blank/non-comment, have no trailing comment, and not itself open
      a further nested block (`bodyOpensNewBlock`); the line after the
      body must sit at a shallower depth (else the block held >1
      statement, and the join is skipped). Replacement spans
      `[header.start, bodyContentEnd)`, deliberately excluding the body's
      own trailing NEWLINE (an earlier draft that included it corrupted
      output, caught by the first smoke case). Overflow leaves the block
      form untouched. **Ambiguity resolved conservatively:** a body
      containing `lambda` (which has its own non-block-opening `:`) is
      conservatively always treated as "opens a new block" (never joined),
      since lambda-parameter-aware depth tracking wasn't built. **A nested
      compound statement's own header still independently gets its own
      join opportunity** — e.g. `if x:\n if y:\n  return 1` becomes
      `if x:\n if y: return 1`, not left fully untouched (discovered while
      writing the smoke test, not a bug). Verified via a 17-case smoke test
      (block-form joins, `if`/`elif`/`else` chains, `match`/`case`,
      overflow staying block, `def`/`class`/`try`/`with` never joining,
      nested-compound interaction, lambda-body skip, idempotency) + full
      `make test` (114/114, zero regressions). **Gaps:** header/body
      spanning multiple physical lines never a candidate; a body with its
      own trailing comment conservatively skipped; never expands an
      already-compact line back to block form (no such rule in the style
      doc); no `;`-chaining is ever produced. **Discovered interaction**
      (same posture as §5's own documented one): if this pass's join spans
      a sub-range another pass would also replace (e.g. §2 on the body
      statement), this pass's wider, earlier replacement wins and the
      inner one is silently dropped, not corrupted — not independently
      re-verified this slice since §5 already established the render-merge
      behavior is safe.

      **§7 (Structural Pattern Matching) — `:` column alignment-only
      slice.** New `ScopePipelineIndent.CaseLine`/
      `applyCaseColonAlignment`/`classifyCaseLine`/`flushCaseGroup` — no
      new `MiscRuleIndent` method needed, reuses §4-§6's `normalizeGap`/
      `verbatimLineText`/`isOpenBracketText`/`isCloseBracketText`/
      `trimEndIdx`. `classifyCaseLine` checks literal text `"case"` (a
      context-sensitive soft keyword tokenized as plain `IDENTIFIER`). The
      header `:` is found via a bracket-depth-0 scan after `case`, correctly
      skipping a mapping pattern's own `{"action": action}` colon and
      including any guard clause. `compact` reflects the body's existing
      same-line-vs-block shape as written (never decided). Grouping mirrors
      §2/§3 (contiguous same-depth `case` lines, broken by blank/comment/
      depth-change/non-case statement, including the enclosing `match`
      header). **All-or-nothing** per §7: `flushCaseGroup` emits zero
      replacements for the whole group if any member is block-form. For an
      all-compact group, only the gap immediately before `:` is padded to
      align the column. **Verified as pre-existing gaps, not newly
      introduced:** or-pattern `|` and guard-clause spacing both rely on
      the same "no general expression-respacing pass" gap §5 already
      documented; wildcard `_` needs no special handling (ordinary
      identifier); deconstruction/sequence/mapping pattern content is not
      repadded via `PythonBracketComplexityEvaluator` (it exists but isn't
      wired into case-pattern rendering, unlike §4's decorator wiring) —
      not treated as a new gap since narrowing to `:`-alignment-only was
      this checkpoint's pre-agreed scope. **Point 8 (closing-comment
      mechanism) confirmed absent, not guessed** — grepped the whole tree;
      every hit is Curly-family/JS-TS-Tags-adjacent, zero Python3 support;
      building it from scratch was out of scope. Verified via 8-case smoke
      test (block/compact worked examples, varying-width alignment,
      mixed-form all-or-nothing abandonment, or-pattern/guard survival,
      wildcard, idempotency) + full `make test` (114/114, zero
      regressions).

      **§9 (Control Flow Blank Lines) — last §2-9 sub-item, checklist item
      now complete.** New `ScopePipelineIndent.ControlFlowFrame`/
      `applyControlFlowBlankLines`/`isDefHeaderLine`/`previousContentLine`/
      `isUnconditionalExitLine`/`insertBlankLineBefore`. §9.1 ported
      faithfully from `MiscRuleCurly#insertBlankLineBeforeReturn`: inserts
      a blank line before a `return` that's the first token of its logical
      line, when the innermost enclosing frame is a function body (tracked
      via a depth-keyed `ControlFlowFrame` stack) that has already seen a
      statement — does NOT verify `return` is the body's textually final
      statement (a faithful port; the C-family reference doesn't either).
      §8's compact-form carve-out needs no special case (a compact line's
      first token is the header keyword, never `return`).
      **§9.2 could not be ported — no C-family mechanism exists to port,
      discovered via required pre-implementation grep.** The nominal
      reference, `BlockStructureRule.placeElseOnOwnLine`, only ever
      *preserves* existing blank-line state, never adds one on last-
      statement-content; implemented directly from STYLE_PYTHON3.md §9.2's
      own unambiguous text instead: inserts a blank line before any
      `elif`/`else` whose nearest preceding non-blank/non-comment logical
      line (walking back past intervening blank/comment lines, `depth`-
      agnostic) has `return`/`break`/`continue` (never `raise`, matching
      the C-family list's own omission) as its first token. Both halves
      only ever ADD a missing blank line, never remove one (verified: a
      2-blank gap stays 2). A comment-only line immediately preceding a
      qualifying `return`/`elif`/`else` is conservatively left untouched
      (no worked example to guess a relocation from). **Gaps:** a
      multi-physical-line `def` header is never recognized as function-
      body-opening (same posture as §6's own multi-physical-line gaps);
      semicolon-chained statements (`x = 1; return y`) never recognized by
      either half; a §8-compact preceding block ending in `return`/`break`/
      `continue` is never recognized by §9.2 (same by-construction reason
      as §9.1's own compact exclusion — not independently re-verified this
      slice, documented rather than guessed); `try`/`except`/`finally`
      blank-line placement entirely out of scope (§9.2's text names only
      `elif`/`else`). Verified via a 14-case smoke test (both worked
      examples, missing-blank insertion, nested-return non-qualification,
      §8 compact interaction, `raise`-exclusion, each of return/break/
      continue triggering §9.2 including a nested-depth case, idempotency,
      never-remove posture, comment-adjacency skip) + full `make test`
      (114/114 forward + idempotency, zero regressions).

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
      processed (24 `src/flask/`, 41 `tests/`, 18 `examples/`/`docs/` — the
      full tree, no exclusions needed; "not large" confirmed, 3.5M total).
      Zero crashes on the forward pass.

      **Four real bugs found and fixed, all via non-idempotency
      (`diff -r round1 round2`), none via `py_compile`/AST-diff (both came
      back clean once the four fixes landed — see below):**
      1. `ScopePipelineIndent.render`'s replacement-merge loop advanced its
         cursor `r` only on an exact `start == i` match; two legitimately
         overlapping token-range replacements (§8's single-statement-body
         join and §2's own trivial assignment-alignment group for the same
         nested statement) left a stale entry that permanently stalled `r`,
         silently dropping every later replacement in the entire file — a
         single nested nullary-effect assignment group inside a
         `while`/`try` block could disable all downstream assignment-
         alignment padding. Fixed by skipping stale entries instead of
         stalling. Fixture `real_code_regressions_78_{inp,out}.py`.
      2. §6 `trySignatureGroup` split a signature's interior into parameter
         segments on raw `NEWLINE` tokens only, not bracket-depth-aware — a
         parameter's type hint spanning multiple physical lines via a
         still-open nested bracket had its continuation lines misclassified
         as bogus parameters instead of triggering the documented "leave
         whole signature untouched" gap, producing non-convergent trailing
         whitespace across idempotency rounds. Fixed by only splitting at
         depth-0 `NEWLINE`s and rejecting any segment with an embedded one.
      3. §9.2's blank-line-before-`elif`/`else` zero-width insertion and
         §8's single-statement join could both start at the same token
         index; the stable sort left the zero-width entry second, so §8's
         wider replacement jumped over it, silently dropping the blank line
         — a forward-pass bug, not just idempotency (confirmed present on
         `debughelpers.py`'s round1 already). Fixed by sorting equal-`start`
         replacements with zero-width entries first. Bugs 2+3 combined into
         fixture `real_code_regressions_79_{inp,out}.py`.

      **Final numbers (after all four fixes, full 83-file corpus,
      re-cloned-fresh directories to avoid a `/tmp/round1`-name collision
      with a concurrently-running JS/TS dogfood session — always use a
      dedicated scratchpad subdir, never the bare name other jobs also
      use):** forward pass zero crashes; `diff -r round1 round2` **empty**
      (clean idempotency, 83/83); `python3.12 -m py_compile` clean on all 83
      round1 files (python3.6 is NOT viable for this repo — flask's own
      source uses the walrus operator and `from __future__ import
      annotations`, both post-3.6 syntax, so python3.6 falsely reports
      syntax errors unrelated to the formatter); AST-diff (new
      `python_ast_diff.py`, see below) clean on all 83 files **after manual
      triage of 9 initial reports** — every one of those 9 was the §3
      import-sort pass legitimately reordering `from X import name` sibling
      statements (verified by comparing each file's own set of imported
      `(module, name, asname)` tuples pre/post format — identical sets, only
      statement order differs, which is §3's documented, intentional
      behavior), not corruption; zero true AST-shape mismatches (i.e. zero
      indentation/scoping corruption) found. `make test`: 128/128 forward +
      128/128 idempotency after the final fixture additions.

      New tool: `python_ast_diff.py` (see "Dogfood Tooling" below).

      **`pallets/click` — DONE.** 78 `.py` files processed (fresh clone,
      `/tmp/click`; no existing checkout found in `/tmp`/scratchpad from a
      prior session). Zero crashes on the forward pass.

      **One real bug found and fixed, via non-idempotency
      (`diff -rq round1 round2`):** `ScopePipelineIndent
      .applyBracketPadding` (§4's decorator recursive bracket-padding)
      couldn't distinguish an f-string field's own `{`/`}` (plain PUNCT,
      same token shape as a dict/set literal brace) from an actual dict/set
      literal, padding an f-string field nested in a decorator's lambda
      default argument as an always-loose brace pair — found in
      `tests/test_basic.py`'s `@click.custom_version_option(lambda ctx:
      f"{ctx.info_name} 1.0")`, which became `f"{ ctx.info_name }"` on the
      forward pass; §5's own f-string spacing pass then trimmed that gap
      back on the *next* round, so the bug only surfaced as
      non-idempotency. Fixed by skipping a `{`/`}` pair in
      `applyBracketPadding` whenever `{` is immediately preceded by
      `FSTRING_START`/`FSTRING_MIDDLE`. Fixture
      `real_code_regressions_80_{inp,out}.py`.

      **Final numbers:** forward pass zero crashes; `diff -rq round1 round2`
      **empty** (clean idempotency, 78/78) after the fix; `python3.12 -m
      py_compile` clean on all 78 round1 files (same as the 78 unmodified
      originals — zero new syntax errors); semantic sanity check beyond
      syntax: the formatted package imports cleanly
      (`PYTHONPATH=.../src python3.12 -c "import click"`, prints version
      `8.4.0` with only click's own unrelated deprecation warning), and a
      representative slice of click's own pytest suite run against the
      formatted package (`tests/test_basic.py` — the exact file the bug was
      found in — plus `test_arguments.py`/`test_options.py`) passed 857/857;
      the full `tests/` run was not completed (timed out past 120s on what
      looks like an interactive/terminal-waiting test, unrelated to this
      job's formatting output) and was not chased further per the "don't be
      a time sink" guidance — the targeted subset already exercises the
      exact regression found. `make test`: 129/129 forward + 129/129
      idempotency after the fixture addition.

      **`psf/black` — IN PROGRESS (dogfood run, bugs found, NOT fixed this
      session — deferred to a future session per explicit scope limit).**
      Fresh clone (`/tmp/black`, no existing checkout found in `/tmp`/
      scratchpad), 338 `.py` files (`src/`, `tests/` incl. `tests/data/`'s
      own curated formatting-edge-case corpus, `scripts/`). No Python3
      syntax-checker beyond `python3.12 -m py_compile` and this job's own
      `python_ast_diff.py` (both already documented above) — no separate
      "syntax_checker" tool needed beyond those two.

      **Forward pass (round1): one crash, 337/338 files formatted.**
      `./tests/data/cases/pep_701.py` throws
      `java.lang.IndexOutOfBoundsException` from
      `ScopePipelineIndent.processField` (via `processFString` /
      `applyFStringSpacing`), caught per-file (batch continued, did not
      abort the whole run). **Minimal repro:** a single-line file containing
      only `f"{1}\{{"` (an f-string whose field is followed by a literal
      escaped-brace `\{{` sequence) reproduces the crash standalone —
      `IndexOutOfBoundsException: Index: 9, Size: 9` in that minimal case,
      `Index: 1625, Size: 1625` in the full `pep_701.py` file. Root cause not
      investigated beyond the stack trace (per this session's
      documentation-only scope) — likely `processField`'s bracket/index scan
      walks past the token list's end when a field is immediately followed
      by an escaped `{{`/`}}` pair rather than plain literal text or another
      field.

      **Round2 (idempotency, run over the 337 successfully-formatted round1
      files): zero crashes, but 3/337 files differ from round1 — two
      distinct non-idempotency bugs:**

      1. **§7/§8 ordering non-idempotency (case colon alignment).** A
         `match`/`case` block that is still block-form (`case Point():` /
         body on the next line) is correctly skipped by §7's colon-alignment
         (all-or-nothing, block-form present) on round1; §8 then joins each
         case's single-statement body onto the header line
         (`case Point(): print(...)`) later in the same pass. On round2, §7
         now sees the already-compact form left by round1's own §8 join and
         *this* time applies column alignment across the group, padding
         extra spaces before `:` that were never present in round1's output.
         Minimal repro (3 files affected in the corpus:
         `tests/data/cases/pattern_matching_simple.py`,
         `tests/data/line_ranges_formatted/pattern_matching.py`):
         ```python
         match x:
             case Point():
                 print("Somewhere else")
             case _:
                 print("Not a point")
         ```
         Round1 output: `case Point(): print("Somewhere else")` /
         `case _: print("Not a point")` (unaligned, §7 skipped due to
         block-form input). Round2 re-formats round1's own output and
         produces `case _      : print("Not a point")` (colon-aligned) —
         differs from round1, non-idempotent.

      2. **§6 multi-physical-line type-hint gap violated + unbounded
         trailing-whitespace growth (separate bug, same file:
         `tests/data/cases/pep604_union_types_line_breaks.py`).**
         §6's own documented gap says a parameter whose type hint spans
         multiple physical lines (e.g. a `|`-union type broken across lines)
         should leave the *whole signature* untouched, but
         `trySignatureGroup`/`classifySignatureParam` instead treats each
         continuation `| Type` line as if it were its own parameter,
         producing alignment padding it shouldn't attempt at all — and the
         trailing-whitespace padding after each continuation line's
         (nonexistent) `:`/`=` column **grows by more spaces on every
         successive round, never converging** (confirmed 3 rounds:
         width increases round1→round2→round3, did not stabilize). Minimal
         repro:
         ```python
         def foo(
             i: int,
             x: Loooooooooooooooooooooooong
             | Looooooooooooooooong
             | Looooooooooooooooooooong
             | Looooooong,
         ):
             pass
         ```
         Round1 pads `| Looooooooooooooooong` with trailing spaces; round2
         re-pads with *more* trailing spaces than round1; round3 more still.
         This is worse than ordinary non-idempotency (a stable wrong answer)
         — it's unbounded growth, confirmed to not stabilize within 3
         rounds.

      **`python3.12 -m py_compile` on all 337 round1 files: clean except one
      pre-existing (not formatter-induced) failure** —
      `tests/data/cases/trailing_comma_optional_parens3.py` fails
      `SyntaxError: 'return' outside function` on **both** the unmodified
      original and the round1 output (verified identically against the raw
      clone) — this is one of black's own intentionally-invalid test
      fixtures, not a formatter bug.

      **`python_ast_diff.py` on all 337 round1 files: 22 mismatches
      reported, triaged as follows:**
      - **13 are the already-documented §3 import-reorder false-positive
        shape** (`tests/optional.py`, `tests/test_docs.py`,
        `scripts/generate_schema.py`, `src/black/files.py`,
        `src/black/nodes.py`, `src/black/parsing.py`, `src/black/lines.py`,
        `src/black/__init__.py`, `src/blib2to3/pgen2/pgen.py`,
        `src/blib2to3/pgen2/parse.py` reordering `alias(name=...)` import
        entries; `tests/data/cases/import_spacing.py` reordering plain
        imports) — not re-verified tuple-by-tuple this session (pattern
        matches the `pallets/flask` run's already-established false
        positive exactly: reordered `ast.alias`/import nodes, same names).
      - **8 are `rc=2` (parse failure on one side)** — all in
        `tests/data/cases/`/`tests/data/miscellaneous/` fixture files that
        are themselves deliberately either syntactically invalid
        (`pattern_matching_invalid.py`, `invalid_header.py`,
        `python2_detection.py`) or use syntax newer than what stdlib
        `ast`/`python3.12` accepts (`python315.py`, `pep_572_do_not_remove_
        parens.py`, `remove_except_types_parens.py`, `pep_750.py`,
        `type_param_defaults.py`, `async_as_identifier.py`) — expected,
        pre-existing parse limitations of the checker/interpreter version,
        not formatter corruption (not independently re-verified per-file
        against the original also failing to parse, given the session's
        time budget — flagged here for a future session to confirm each one
        the same way `trailing_comma_optional_parens3.py` was confirmed
        above, rather than assumed).
      - **2 are genuine content-corruption bugs, both in `ScopePipelineIndent
        .applyFStringSpacing`'s `addBraceTrim` (§5), both distinct from the
        pep_701.py crash above:**
        - **(a) A field immediately followed by a nested `{` (e.g. a
          set/dict comprehension as the field's own expression) gets its
          field-close brace fused with the literal `{{` that follows,
          turning a real expression field into an escaped-literal-brace
          run.** Minimal repro (`tests/data/cases/fstring.py`, line 8/22):
          ```python
          f"space between opening braces: { {a for a in (1, 2, 3)}}"
          ```
          formats to:
          ```python
          f"space between opening braces: {{a for a in (1, 2, 3)}}"
          ```
          — `{{` is an escaped literal `{`, not a field open, so the
          set-comprehension expression is silently deleted from the
          program's semantics (this is not merely a spacing change; the
          `ast.dump` shows the `FormattedValue`/set-comprehension node is
          gone entirely, replaced by literal string text). Root cause not
          fixed this session; likely `addBraceTrim` trims the gap after the
          field's opening `{` without checking whether the immediately
          preceding character sequence would make the result ambiguous with
          an escaped `{{`.
        - **(b) Self-documenting f-string fields (the `{expr=}` debug
          syntax) have their leading gap trimmed even though the exact
          source text of `expr` (including its original whitespace) is
          supposed to be reproduced verbatim in the runtime output** for a
          `=`-suffixed field. Minimal repro
          (`tests/data/cases/preview_long_strings.py`, line 327):
          ```python
          log.info(f'Skipping: {  longer_name   =  :  .3f }')
          ```
          formats to:
          ```python
          log.info(f'Skipping: {longer_name   =  :  .3f }')
          ```
          — trims the gap right after `{`, which changes the verbatim text
          that Python prints before `=` at runtime for a self-documenting
          field (a real behavior change, not just cosmetic). §5's
          `addBraceTrim` has no carve-out for detecting a trailing `=`
          debug specifier before deciding to trim the opening gap.

      **Summary: 1 crash (pep_701.py, per-file caught, does not abort
      batch), 2 non-idempotency bugs (§7/§8 join-then-align ordering; §6
      unbounded-growth trailing whitespace on multi-line union-type hints),
      2 content-corruption bugs (both in §5's `addBraceTrim` — nested-brace
      field fusion deletes an expression; self-documenting `{expr=}` fields
      lose verbatim leading whitespace). All five are documented here only;
      none fixed this session per explicit task scope — a future session
      should fix each, add permanent `real_code_regressions_*` fixtures per
      `STATE_COMMON.md`'s convention, and only then consider re-running the
      full `psf/black` corpus to confirm.**

      `python/cpython`, `django/django` — still not started.
