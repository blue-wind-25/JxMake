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
**Current status is scaffold-only:** dispatch exists only as a "not yet
implemented" error thrown for Python constructs; no real formatting logic
yet.

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

- [x] Tokenizer support pass: survey `STYLE_PYTHON3.md` for every
      token/construct not already lexed correctly (f-string interpolation
      boundaries, `:=` walrus if present, significant-whitespace/indent
      tracking as a first-class tokenizer concern rather than braces,
      `match`/`case` soft keywords). Lands in the new `TokenizerIndent`
      skeleton class, not the shared `TokenizerCore` that
      C/C++/Java/Kotlin/JS/TS extend — so the risk isn't corrupting an
      existing language's token lexing, it's getting the shared dispatch
      plumbing wrong (`Lang.java`'s family predicates,
      `FormatterCore.forLanguage` routing). Re-run the full regression suite
      and confirm zero regressions before any rule-level work begins.
      Indentation being semantically load-bearing (not just cosmetic) is
      the single biggest structural difference from every existing
      language and is resolved architecturally in this step.

      **Slice 1:** `TokenizerIndent` no longer throws — its constructor + a
      real `tokenize(String)` handle whitespace, newlines, `#` line
      comments, numbers (reusing `TokenizerCore.emitNumber`),
      identifiers-vs-keywords (own `KEYWORDS_PYTHON` set, deliberately
      excluding `match`/`case`/`_`/`type` since those are context-sensitive
      soft keywords, not unconditional — misclassifying them would break
      their use as ordinary identifiers), single-line single/double-quoted
      string literals, and a generic operator/punctuation fallback
      (`(`/`)`/`[`/`]`/`{`/`}`/`,`/`:`/`;` as `PUNCT`, everything else as a
      one-char `OP`). Verified via a smoke test (`def foo(x, y=1):` +
      comment + `return x + y  # trail` tokenized correctly) and a full
      `make test` run (114/114 forward + 114/114 idempotency, zero
      regressions — this class isn't wired into any live dispatch path yet,
      so "zero regressions" here means compile/link health, not behavior
      change).

      **Slice 2:** `emitTripleQuotedString` added (dispatched from
      `tokenize()` when the current char and next two all match the same
      quote character, checked before the single-line-string branch).
      Consumes through the matching closing `"""`/`'''`, correctly spanning
      embedded newlines and singly/doubly-embedded instances of the other
      quote character, and honors backslash escapes the same way
      `emitSimpleString` does. Emits one `STRING` token for the whole body —
      satisfies RDD_KEY_186's "opaque, preserved verbatim beyond the
      opening line" requirement at the tokenizer level (no rule pass needs
      its own docstring-preservation logic). Verified via a smoke test (a
      multi-line docstring with an embedded `"quote"`, a `'''...'''`
      string, and adjacent single-line `"a"`/`'b'` strings all tokenized
      correctly as distinct, correctly-bounded tokens) and a full
      `make test` run (114/114 forward + idempotency, zero regressions).

      **Slice 3:** `emitWalrus` added — the `:=` walrus operator (PEP 572)
      is now dispatched (in `tokenize()`, checked before the general punct
      branch since `:` is otherwise claimed by `emitPunct`) as a single
      `OP` token instead of separate `:` PUNCT + `=` OP. Verified via a
      smoke test (`if (n := len(a)) > 10:` tokenized with `:=` as one `OP`
      token and the trailing statement `:` still correctly a separate
      `PUNCT`) and a full `make test` run (114/114 forward + idempotency,
      zero regressions).

      **Slice 4:** f-string interpolation-boundary sub-tokenization.
      Follows CPython 3.12+'s own FSTRING_START/MIDDLE/END scheme (new
      shared `TokenType` constants `FSTRING_START`/`FSTRING_MIDDLE`/
      `FSTRING_END`/`FSTRING_FORMAT_SPEC`, added to `TokenizerCore`,
      Python3-only). The per-character dispatch previously inlined in
      `tokenize()` was factored into `dispatchToken(List<Token>)` so it can
      be shared with the new `emitFStringField`'s expression sub-scan
      (nested strings/f-strings/brackets/numbers inside a `{...}` field all
      tokenize normally, recursively). `emitFString` emits FSTRING_START,
      alternates FSTRING_MIDDLE literal-text tokens with `{...}` fields
      (escaped `{{`/`}}` stays in the literal text), then FSTRING_END.
      `emitFStringField` tracks a local bracket depth so a nested `:`/`}`
      (dict literal, slice) isn't mistaken for the field's own format-spec
      separator or closing brace; at depth 0 a `!r`/`!s`/`!a` conversion
      (only recognized when immediately followed by `:` or `}`) becomes an
      opaque OP token and a `:` starts `emitFStringFormatSpec`, which
      brace-balances to find the field's true end (STYLE_PYTHON3.md §5:
      format spec is opaque, literal, not code) but does NOT recursively
      sub-tokenize a nested `{expr}` *within* the spec (`f"{x:{width}}"` — a
      known, documented limitation of this slice). Verified via 8
      smoke-test cases (basic interpolation, conversion+spec, plain spec,
      escaped braces, nested spec field, multi-line triple-quoted
      f-string, nested dict/slice/call expressions inside fields, and
      confirming plain non-f prefixed strings like `rb'raw'` are
      unaffected) and a full `make test` run (114/114 forward +
      idempotency, zero regressions). **Explicitly NOT yet covered:**
      recursive sub-tokenization of a nested replacement field inside a
      format spec (see above).

      **Slice 5:** INDENT/DEDENT synthesis. `(`/`[`/`{` now track a single
      merged bracket-nesting counter via the inherited `parenDepth` field
      (new `emitOpenBracket`/`emitCloseBracket`, dispatched from
      `dispatchToken` in place of the old generic `emitPunct` for those six
      chars) — Python attaches no separate scope meaning to `{` the way the
      curly family does, so all three bracket kinds suppress logical-line
      significance identically. New `synthesizeIndentation(List<Token>)`
      runs as a post-pass at the end of `tokenize()` (CPython-style
      indent-width stack: spaces +1, tabs to next multiple of 8, form-feed
      resets to 0), inserting zero-width `INDENT`/`DEDENT` marker tokens
      (new `TokenType` entries, Python3-only; `text` carries the new width
      as a string for any later rule pass) at the start of each logical
      line whose indentation width differs from the enclosing block's. A
      physical line is skipped for indent purposes (no marker emitted, but
      still copied through verbatim) when blank, comment-only, inside an
      open bracket (`NEWLINE` token's own `parenDepth > 0`), or when the
      prior line ended in a backslash continuation
      (`isBackslashContinuation`: the token immediately before the
      `NEWLINE` is a `\` OP). Trailing `DEDENT`s are emitted at EOF to
      close any still-open blocks. No `TabError`-style tabs/spaces
      consistency validation — this tokenizer assumes syntactically valid
      input, same posture as every other language here; the Open
      Questions section's per-block indent-size/style rescaling design
      still needs to be built on top of this width data, not assumed
      already present. Verified via a smoke-test harness (nested `if`
      inside `def`, a blank line, a comment-only line, a parenthesized
      multi-line call, a backslash-continued assignment, and a second
      top-level `class`/`def` nesting) — INDENT/DEDENT markers, suppressed
      markers inside the open call's parens, no marker across the
      backslash continuation, and correct DEDENT-to-0 at EOF all confirmed
      — plus a full `make test` run (114/114 forward + idempotency, zero
      regressions; still compile/link-health only, not wired into any live
      dispatch path yet).

      **Tokenizer support pass is now considered complete** except for the
      one documented format-spec sub-tokenization limitation above, which
      is deferred (opaque format specs are explicitly allowed not to
      recurse per STYLE_PYTHON3.md §5 — only a *nested replacement field
      within* a spec is unhandled, a narrow, rare case). Next checklist
      item is the statement/indentation formatting skeleton.
- [x] Implement basic statement/indentation formatting first (the Python
      analog of a "get the skeleton right" starting point, since there are
      no braces to reuse the existing block-structure rule against) —
      confirm indentation depth is never altered in a way that changes
      block membership before any cosmetic rule (alignment, spacing,
      compaction) is layered on top.
      **Landed:** `ScopePipelineIndent.process` tokenizes via
      `TokenizerIndent` and renders the token stream straight back to
      source text with a new `render` helper — a deliberate identity pass
      (every token's `text` appended verbatim, except the synthesized
      zero-text `INDENT`/`DEDENT` markers, which are skipped).
      `FormatterIndent.formatOne` now delegates to it (mirrors
      `FormatterJson`'s whole-file `formatOff` short-circuit — Python has
      no per-region frozen-span mechanism decided/implemented yet).
      `ScopePipelineIndent`'s constructor gained a `Lang` parameter (needed
      to construct `TokenizerIndent`; had no external callers yet, so free
      to change). This intentionally does NOT flip `python3` out of
      `Lang.SCAFFOLD_ONLY_LANGUAGES` — every other language in this
      codebase left that list only once it had real, substantive rule
      logic, not a lossless skeleton; `Main.run`/`ServerMode` still gate on
      `Lang.isScaffoldOnly` and throw `UnsupportedLanguageException` before
      ever reaching this class for a real `--lang python3` invocation, so
      `FormatterIndent`/`ScopePipelineIndent` are reachable only via
      direct construction/test harnesses for now (same posture as the
      tokenizer-only slices before it). Verified via a smoke-test harness
      calling `FormatterCore.forLanguage("python3").formatOne` directly
      against the two registered local fixtures' `_inp.py` files
      (`test/py_combined_inp.py`, `test/py_comments_inp.py`) — output
      byte-identical to input for both — plus a full `make test` run
      (114/114 forward + idempotency, zero regressions; still
      compile/link-health only for this class, since it isn't reachable
      from the CLI/server dispatch paths yet).
- [x] Implement §1 (bracket complexity detector, including its four
      Python-only sub-categories) as the next foundational piece, since
      §2/§6/§7 all depend on its alignment/padding primitives.
      **Landed:** new `evaluator/PythonBracketComplexityEvaluator.java`,
      deliberately self-contained rather than delegating to the existing
      `ComplexityPaddingEvaluator` (that class never treats a nested `{` as
      a complexity signal — a non-issue for C-family since `{}` is only
      ever an initializer there, but Python's dict/set literals are common
      call-argument/index content; and its Kotlin receiver-function-type
      carve-out has no Python analog). Three entry points per bracket kind:
      `isLooseParen` (§1.1 baseline + §1.2 — a top-level `for` makes a
      generator-expression argument loose the same as any other
      comprehension), `isLooseBracket` (§1.1-§1.4 — a top-level
      comprehension is unconditionally loose; otherwise splits on top-level
      `:` per §1.3 and evaluates each slice segment independently, so
      `a[i+1:(j*k)-1]` goes loose from its second segment's nested `()`
      while the `:` itself is never a signal; §1.4 star-unpacking needed no
      special case since `*`/`**` are `OP` tokens, never bracket
      punctuation), `isLooseBrace`/`classifyBrace` (§1.5 — non-empty `{}`
      unconditionally loose regardless of content shape, empty `{}`
      unconditionally tight and classified `DICT`; non-empty classified
      `DICT`/`SET` by top-level `:` presence). All depth tracking is a
      shared private `(`/`[`/`{` counter, not delegated to the tokenizer's
      own `parenDepth` field (this evaluator only ever sees an
      already-extracted content slice, not the full token stream, so it
      needs its own local notion of depth-0-relative-to-this-slice).
      Verified via an 18-case smoke-test harness covering every example
      given in STYLE_PYTHON3.md §1.1-§1.5 verbatim (all 18 passed) and a
      full `make test` run (114/114 forward + idempotency, zero
      regressions; still compile/link-health only — no caller wires this
      evaluator in yet, that lands with §2/§6/§7's own padding/alignment
      rules).
- [x] Implement §2–9 rule-by-rule, each its own checkpoint commit, per
      `STATE_COMMON.md`'s workflow.

      **§2 (Assignment Alignment) landed.** `TokenizerIndent.emitOperator`
      had a real gap discovered while implementing this: it only ever
      consumed a single character, so every compound-assignment operator
      (`+=`, `|=`, `**=`, `//=`, etc.) came out as two-plus separate `OP`
      tokens instead of one — invisible to the identity-pass/§1 work landed
      so far since neither cares about operator identity, but fatal to
      §2's alignment grouping. Fixed by giving `TokenizerIndent` its own
      `MULTI_CHAR_OPS` longest-first array (mirroring
      `TokenizerCurly.MULTI_CHAR_OPS`'s precedent), covering Python's set:
      `**=`, `//=`, `<<=`, `>>=`, `->`, `**`, `//`, `<<`, `>>`, `<=`, `>=`,
      `==`, `!=`, `+=`, `-=`, `*=`, `/=`, `%=`, `&=`, `|=`, `^=`, `@=` (`:=`
      already had its own dedicated `emitWalrus` dispatch, unaffected).
      §2 itself: new `MiscRuleIndent.PyAssignment`/`renderPyGroup`
      (target/operator/valueTokens triple; padded `name (op)= value`
      rendering, no trailing `;`, no comment-column alignment — nothing in
      STYLE_PYTHON3.md §2 calls for aligning trailing comments, unlike the
      C-family's `ColumnGrid` usage) plus a new
      `ScopePipelineIndent.applyAssignmentAlignment` pass: a from-scratch
      NEWLINE/INDENT/DEDENT-aware logical-line splitter (no `;`/`{}` to
      split on, unlike `MiscRuleCore.splitAssignmentStatements`) that
      classifies each logical line as a single-physical-line `identifier
      (op)= value` candidate or not, tracks indentation depth via the
      synthesized `INDENT`/`DEDENT` markers, and groups consecutive
      same-depth candidates (blank line, comment-only line, depth change,
      or any unrecognized statement all break the group, matching
      STYLE_PYTHON3.md §2's explicit "a blank line or a comment breaks the
      group" text — confirmed this is a genuine Python-specific addition
      versus STYLE.md §6's C-family text, which only names blank lines; not
      a discrepancy to resolve, just two specs each stating their own
      rule). Each grouped assignment gets one `Replacement` covering only
      its own `target...value` token span — indentation, surrounding
      blank/comment lines, and any trailing same-line comment are left
      completely untouched, so no gap-text reconstruction was needed at all
      (simpler than `ScopePipelineCurly.applyAssignmentsPass`, which must
      rewrite whole multi-statement spans since curly's rendering can
      change line count). **Explicitly NOT yet covered by this slice:**
      multi-line right-hand sides (STYLE_PYTHON3.md §2's two continuation
      examples) — a logical line whose token range spans more than one
      `NEWLINE` token (bracket or backslash continuation) is never
      classified as a candidate, left completely untouched; a lone
      bare-IDENTIFIER-target restriction is kept (same restriction
      `MiscRuleCore.parseAssignment` already applies to the C-family, so
      `self.x = 1`/`a[0] = 1`/tuple-assignment all correctly fall through
      unrecognized, no target-rendering machinery needed). Verified via a
      5-case smoke-test harness (basic group + comment-break per
      STYLE_PYTHON3.md §2's own worked example, depth breaking a group,
      blank-line self-check, attribute-target exclusion, multi-line-RHS
      left untouched) — all 5 passed — plus a full `make test` run
      (114/114 forward + idempotency, zero regressions; still
      compile/link-health only, `python3` stays in
      `SCAFFOLD_ONLY_LANGUAGES`).

      **§3 (Import Ordering) landed.** New `MiscRuleIndent.PyImport`
      (`Kind` enum `FUTURE < IMPORT < FROM`, implementing `Comparable`
      directly as the full §3.1/§3.3 sort key: kind, then `moduleName`
      alphabetically — relative-import leading `.`/`..` sort correctly for
      free since ASCII `.` precedes any letter, no special-casing needed —
      then per-name lexicographic comparison; plus
      `nameListStart`/`nameListEnd`/`nameUnitTexts` fields letting a
      `FROM` import's own comma-separated name list be rebuilt in sorted
      order independently of whole-statement reordering) plus a new
      `ScopePipelineIndent.applyImportSort` pass. The old §2-only
      line-splitter was generalized into a shared `RawLine`/
      `splitRawLines` (same NEWLINE/INDENT/DEDENT/continuation-aware
      algorithm, now reused by both §2 and §3 — `process()` now runs both
      passes off one shared `splitRawLines` call, merges their
      `Replacement`s, and sorts by `start` before a single `render`, since
      the two passes' replacement spans can never overlap by
      construction). Grouping: consecutive same-depth `import`/`from`
      statements form one sortable group; a blank line, a comment-only
      line, a depth change, or any non-import statement (including one
      that itself contains a nested import, e.g. an `if`-guarded `import
      winreg`) all break the group — the blank/comment-as-breaker
      extension goes beyond STYLE_PYTHON3.md §3's literal text (which only
      names "non-import statement"), a deliberate conservative choice
      since the spec is silent on how a blank line or attached comment
      should move when statements are physically reordered around it.
      `classifyImport` recognizes `import dotted.name[ as alias]` and
      `from [.[.[...]]][dotted.name] import (name[ as alias][, ...] | *)`.
      **Discovered mid-implementation, not just an inter-statement
      tie-breaker:** STYLE_PYTHON3.md §3.1's own worked example (`from os
      import path, sep`) requires the names *within* a single `from`
      clause to themselves be alphabetized, not merely used to break ties
      between statements — `flushImportGroup` now independently rebuilds
      just a `FROM` import's own name-list span (via
      `nameListStart`/`nameListEnd`/`nameUnitTexts`) when its names are out
      of order, even inside an otherwise-unchanged or singleton group; a
      group flush is now skipped only when neither the statement order nor
      any single statement's own name list needs changing (no longer gated
      on `group.size() < 2` alone, since a lone `from` statement can still
      need its own names re-sorted). **Explicitly NOT yet covered by this
      slice** (each a documented gap, not a guess): any multi-physical-line
      import (bracket or backslash continuation) is left untouched;
      parenthesized `from X import (...)` is rejected entirely, even a
      single-physical-line form, since reliably distinguishing it from the
      simpler comma-list parsing wasn't judged worth the risk this slice;
      multi-module `import a, b` on one line is rejected/deferred (only
      single-module `import a.b.c[ as alias]` is recognized) — no worked
      example covers that shape, and it's structurally similar to but
      riskier than the from-clause name-list problem already solved.
      Verified via a 6-case smoke-test harness (§3.1's full worked example
      including within-clause name sorting, §3.2 grouping with a blank
      line and a nested-import `if` both acting as breakers, §3.3
      future-import promotion to group top, a lone import left untouched,
      a parenthesized from-import left untouched, and an aliased import
      sorting by its own module name ignoring the alias) — all 6 passed —
      plus a full `make test` run (114/114 forward + idempotency, zero
      regressions).

      **§4 (Decorators) landed.** New `ScopePipelineIndent.applyDecoratorSpacing`
      (plus its helpers `applyBracketPadding`/`classifyLoose`/`isOpenBracketText`/
      `matchBracket`/`prevSignificant`/`normalizeGap`) — no new `MiscRuleIndent`
      method needed, unlike §2/§3, since this pass only ever rewrites tiny
      delimiter-gap spans, never rebuilds a whole rendered line. For each
      single-physical-line raw line whose first significant token is the `@` OP:
      (1) any whitespace between `@` and the following token is collapsed to zero
      width (tight bind, STYLE_PYTHON3.md §4's "no space" rule — covers a stray
      `@  app.route(...)`, the ambiguity flagged in the task prompt, resolved as
      "any whitespace on the same logical line is removed"); (2) every `(`/`[`/`{`
      pair anywhere in the decorator's own expression (recursively, at every
      nesting depth — e.g. a `methods=[...]` kwarg list nested inside the outer
      call) gets its own immediate delimiter gap (right after the opener, right
      before the closer) normalized to one space if
      `PythonBracketComplexityEvaluator.isLooseParen`/`isLooseBracket`/
      `isLooseBrace` (picked by the bracket's own kind) says loose, zero if tight
      — deliberately delimiter-gap-only, exactly `MiscRuleCore#enforceComplexityPadding`'s
      division of responsibility for the C-family (comma/operator spacing *inside*
      the content is not this pass's concern and is left untouched). A decorator
      with no call (`@dataclass`, bare `@x.setter`) never enters the bracket-padding
      helper at all (no `(` found), so `@property`/`@x.setter` get zero special
      treatment beyond the shared tight-`@` rule, per §4's explicit "no special
      getter/setter alignment" text. A multi-physical-line decorator (wrapped call
      spanning a bracket continuation) is completely skipped (`RawLine.multiPhysicalLine`
      check), same "documented gap, not a guess" precedent as §2/§3.
      **Explicitly NOT covered by this slice, and why:** overflow/line-wrapping of
      a too-long decorator call's argument list (STYLE_PYTHON3.md §4's own
      "Overflow" paragraph, one-arg-per-line wrap) — surveyed the existing `*Indent`
      classes and `PythonBracketComplexityEvaluator` before writing any code; no
      general line-length-based call-argument-wrapping mechanism exists anywhere
      in this codebase yet (the C-family's own `enforceCallLineBreaking` is a
      `*Curly`-only mechanism, out of scope to port here per the task's explicit
      instruction not to invent fresh wrapping infra from scratch). Scoped this
      checkpoint to spacing/padding normalization only, consistent with how §2/§3
      each documented their own uncovered shapes (multi-line RHS, parenthesized
      `from`-import, multi-module `import a, b`) rather than guessing at machinery
      that doesn't exist yet. Verified via a 7-case smoke-test harness
      (`FormatterCore.forLanguage("python3").formatOne` direct construction, same
      pattern as the statement/indentation-skeleton slice's own harness): the
      style doc's own `@app.route("/users/<int:user_id>")` example, a bare
      `@dataclass`, a `@property`/`@x.setter` pair, `@app.route( "/x" )` extra
      internal padding collapsing to tight (no nested bracket, so `isLooseParen`
      is false), a stray-space `@  app.route("/x")` collapsing to tight-`@`, a
      call with a nested `methods=["GET", "POST"]` list forcing the outer call's
      parens loose while the inner list itself stays tight (no nested bracket
      inside the list), and the same already-loose/already-tight input round-
      tripping unchanged (idempotency at the unit level) — all 7 passed. One real
      bug found and fixed during this verification: the first `normalizeGap`
      draft treated an already-tight `(` `x` `)` gap (`from == to`, no existing
      whitespace token at all) as "nothing to do" and returned `null`
      unconditionally whenever `from >= to`, which silently skipped the loose
      case's needed zero-width insertion — fixed by only treating `from > to` as
      the invalid/no-op guard and allowing `from == to` to fall through to a
      zero-width `Replacement` insertion when `desired` is non-empty. Full
      `make test` run: 114/114 forward + idempotency, zero regressions (still
      compile/link-health only — `python3` stays in `Lang.SCAFFOLD_ONLY_LANGUAGES`,
      this pass is reachable only via direct construction/test harnesses, same
      posture as every prior Python3 slice).

      **§5 (F-Strings) landed.** New `ScopePipelineIndent.applyFStringSpacing` (plus helpers
      `processFString`/`processField`/`isFStringConversion`/`addBraceTrim`/`isCloseBracketText`)
      -- unlike §2/§3/§4, this pass operates directly over the full token stream, not per
      `RawLine`, since a field's own brace/expression tokens never carry a `NEWLINE` and a
      triple-quoted f-string's multi-physical-line literal text is irrelevant to it. Confirmed
      first (per the task's own instruction) exactly what `TokenizerIndent.emitFStringField`
      already produces: the `{...}` expression portion is tokenized as ordinary
      IDENTIFIER/OP/NUMBER/etc tokens (via the shared `dispatchToken`, same as any other Python
      expression), not opaque text -- confirming there is real per-token structure to work with,
      not just a raw string span. `processFString` walks one f-string's FSTRING_START/MIDDLE/
      field/END sequence; `processField` walks one field from its opening `{`, tracking a local
      `(`/`[`/`{` depth (mirroring the tokenizer's own field-scan depth) so a nested bracket's own
      `}` isn't mistaken for the field's close, and recursing into any nested f-string found in
      the expression (`f"{f'{a}'}"`) via `processFString` again so its own fields get their own
      independent trim without affecting the outer field's depth count. `exprEnd` is the first
      depth-0 `!conversion` OP / `FSTRING_FORMAT_SPEC` token, or (absent either) the field's own
      closing `}`. `addBraceTrim` unconditionally trims the gap right after the opening `{` to
      zero-width (the expression's own leading whitespace is never significant), and trims the gap
      right before `exprEnd` to zero-width ONLY when `exprEnd` is itself the closing `}` (i.e. no
      conversion/format-spec present) -- discovered mid-implementation via the smoke test that
      STYLE_PYTHON3.md §5's own worked example `f"{value !r}"` (listed as "never touched") means
      the whitespace immediately before an opaque conversion/format-spec tail is itself left alone,
      not just the tail's own text; the first draft trimmed that boundary gap unconditionally and
      failed this exact worked example, corrected by gating the closing-side trim on
      `exprEnd`-is-the-actual-`}` (a `directClose` boolean) rather than always trimming.
      **Explicitly NOT covered by this slice** (a scope-boundary call per the task's own explicit
      instruction, not a guess): re-spacing the expression's OWN internal operator/operand spacing
      (e.g. `f"{x  +  1}"` -> `f"{x + 1}"`) is out of scope -- surveyed `MiscRuleCore`/
      `MiscRuleIndent`/`PythonBracketComplexityEvaluator` first; the only inherited token-joining
      primitive (`MiscRuleCore#renderTokens`/`needsSpaceBetween`/`isTightToken`) is a C-family
      declarator-spacing helper (hardcodes `*`/`&` as tight pointer/reference sigils, which would
      wrongly collapse Python multiplication `a * b` -> `a* b`; has no notion of Python-only
      operators `**`/`//`/`:=`/`and`/`or`/`not`/comprehension `for`/`if`), not a general
      expression-spacing primitive -- building one from scratch would be a large scope increase
      beyond §5's narrow "braces are tight" ask, deferred to future general-expression-formatting
      work (same posture as §2's "multi-line RHS"/§3's "multi-physical-line import" gaps). Every
      worked example in STYLE_PYTHON3.md §5 itself already has correctly spaced internal
      expression text, so this narrower scope satisfies the style doc's own examples.
      **Also discovered, not guessed, and explicitly left as-is:** when an f-string containing a
      field appears inside a span another pass in the same `process()` call already fully rewrites
      -- concretely, a §2-recognized assignment's own RHS (`x = f"{ y }"`) -- that other pass's
      own wider `Replacement` (covering the whole `target...value` span, sorted first since its
      `start` is smaller) wins, and `ScopePipelineIndent.render`'s "first match at a given token
      position wins; a later, already-passed-over nested replacement is silently never reached"
      behavior means this pass's own narrower replacement for that specific occurrence is dropped
      (not corrupted -- confirmed via a dedicated smoke-test case that the original untrimmed text
      survives unchanged for that one occurrence). An f-string NOT nested inside another pass's own
      replaced span (bare expression statement, function-call argument, any unrecognized-shape
      line) is unaffected and trims normally -- confirmed via the "standalone call-argument
      f-string trims" smoke case. Verified via an 8-case smoke-test harness (direct
      `FormatterCore.forLanguage("python3").formatOne` construction, same pattern as §4's own
      harness): basic `{ x + 1 }` brace trim, format-spec left opaque, conversion-with-space left
      opaque (`{value !r}`, the worked example that caught the bug above), expression-before-format
      -spec trimmed at the open brace only, a plain non-f string with literal `{`/`}` left
      untouched, an already-normalized f-string round-tripping unchanged (idempotency), a
      standalone call-argument f-string trimming normally, and the assignment-RHS interaction
      case documented above -- all 8 passed -- plus a full `make test` run (114/114 forward +
      idempotency, zero regressions; still compile/link-health only, `python3` stays in
      `Lang.SCAFFOLD_ONLY_LANGUAGES`, this pass reachable only via direct construction/test
      harnesses, same posture as every prior Python3 slice).

      **§6 (Function Signature Wrapping) landed -- alignment-only slice.** Confirmed first (per the
      task's own instruction) that STYLE_PYTHON3.md §6's inline-vs-one-per-line *decision* still has
      no home anywhere in the `*Indent`/`*Curly` family (grepped for any line-length-triggered
      breaking mechanism; the only such thing, the C-family's `enforceCallLineBreaking`, is
      Curly-only) -- same documented gap §4 already found for decorator-call overflow, not
      reinvented here. New `MiscRuleIndent.PyParam` (`nameTokens`/`typeTokens`/`defaultTokens`
      triple, `typeTokens`/`defaultTokens` empty-not-null when absent, `trailingComma` flag) plus
      `renderPySignatureGroup` (name column padded to the group's widest name, mirroring
      `renderPyGroup`'s "pad to widest, one gap before the marker" shape for `:`; `=` column padded
      only across the subset of parameters that have BOTH a type hint and a default, so a
      typed-but-defaultless parameter's own type text is never padded and never leaves trailing
      whitespace before its comma -- confirmed against STYLE_PYTHON3.md §6's own worked example,
      whose `y : "List[int]"` row has no default). A parameter with no type hint at all skips the
      `:`-segment entirely per §6's "padded as if its `:` column were empty" text, read literally as
      "omitted" rather than "fake-padded" -- its own `=`, if present, is not forced to align with
      typed rows' `=` column; this is the grid's own documented partial-row shape, not a bug.

      New `ScopePipelineIndent.applySignatureAlignment`/`trySignatureGroup`/
      `classifySignatureParam`/`trimEndIdx`. Unlike §2-§5 (all of which explicitly skip any
      `multiPhysicalLine` `RawLine`), this is the first pass to deliberately target one: a `def`
      (optionally `async def`) statement's parameter list is required to already be written
      one-parameter-per-line in the source -- taking that human-authored line-breaking as given,
      the same posture §2 takes toward an already-single-line assignment candidate (never itself
      decides to break/join a line). `trySignatureGroup` requires the opening `(` to have nothing
      but a `NEWLINE` on its own line, the closing `)` to stand alone on its own line (only its own
      leading indentation before it), and every line strictly between them to classify as exactly
      one parameter via `classifySignatureParam` -- any deviation (an inline first parameter sharing
      `(`'s own line, multiple parameters on one line, a per-parameter trailing comment, a parameter
      itself spanning more than one physical line) returns `null` and the *whole* signature is left
      completely untouched, same "documented gap, not a guess" precedent as every prior §2-§5 slice
      -- this was verified to be a scope-boundary call, not a genuine STATE_COMMON.md ambiguity,
      since STYLE_PYTHON3.md §6 itself only ever describes the already-one-per-line shape aligned
      here. `classifySignatureParam`'s top-level `:`/`=` search tracks the parameter's own local
      bracket depth (starting fresh at 0 per parameter line, mirroring `classifyLoose`/`matchBracket`'s
      own depth-tracking shape elsewhere in this class) so a nested-bracket type hint like
      `List[Dict[str, int]]` never has its own internal `:`/`=` mistaken for the parameter's own
      annotation/default separator -- confirmed this handles the task's own flagged nested-type-hint
      concern without needing to treat it as an open ambiguity. The return-type arrow (`-> Optional
      [str]`) is untouched by construction -- nothing after `closeIdx` is ever scanned or replaced.
      An inline (already-one-line) signature is untouched by construction too, since it's never
      `multiPhysicalLine` and so never reaches `trySignatureGroup` at all.

      Verified via a 5-case smoke-test harness (`FormatterCore.forLanguage("python3").formatOne`
      direct construction, same pattern as every prior slice's own harness): STYLE_PYTHON3.md §6's
      own `process` worked example (4 params, mixed type-hint widths incl. a quoted forward-ref
      type, one with a default), a bare-name/`name=default` parameter participating correctly in
      the padding grid (`alpha` untouched, `beta = 2` aligned with `gamma : int = 3`'s own `=`
      landing in a different column since `beta` has no type segment -- the documented partial-row
      shape above), the fixed-position return-type arrow surviving byte-for-byte including internal
      extra spacing (`->    Optional[int]:`), an inline signature under the line-length limit left
      completely untouched, and an idempotency case (the worked example's own already-aligned
      output round-tripping unchanged) -- all 5 passed -- plus a full `make test` run (114/114
      forward + idempotency, zero regressions; still compile/link-health only, `python3` stays in
      `Lang.SCAFFOLD_ONLY_LANGUAGES`, this pass reachable only via direct construction/test
      harnesses, same posture as every prior Python3 slice).

      **Explicitly NOT yet covered by this slice** (a scope-boundary call, not a guess, per the
      task's own explicit instruction): the inline-vs-one-per-line *decision* itself (STYLE_PYTHON3.md
      §6's opening sentence) is NOT implemented -- this pass never decides to break an inline
      signature into one-per-line form, nor to join an already-broken one back to one line, even
      when the line-length limit would call for it either way. This is blocked on the same missing
      general inline/break-decision infra §4 already documented as absent for decorator-call
      overflow wrapping (no general line-length-triggered call/signature-wrapping mechanism exists
      anywhere in the `*Indent` family; the C-family's `enforceCallLineBreaking` is Curly-only and
      not reusable here) -- not a new gap, the same one, now confirmed to also block §6. Also NOT
      covered, each a deliberate narrow-scope exclusion mirroring §2-§5's own precedent: a signature
      with an inline first parameter sharing `(`'s own line; multiple parameters written on one
      physical line inside an otherwise-broken signature; a per-parameter trailing comment; a
      parameter whose own default value or type hint itself spans more than one physical line
      (bracket continuation nested inside the outer signature's own continuation) -- each of these
      shapes causes the *whole* signature to be left untouched rather than partially aligned.

      **§8 (Single-Statement Bodies) landed.** Unlike §2-§7 (all of which take the human's existing
      line-structure as given and never themselves decide to join/split a line), §8 is a genuine
      join operation, per the task's own framing: it collapses a block already written as `header:`
      followed by exactly one indented simple-statement line back onto the header's own line
      (`header: statement`), when the joined line fits within `line-length` -- STYLE_PYTHON3.md §8
      is a real, precedented feature (the Python analog of STYLE.md §10's C-family single-
      expression-body compaction, whose reference implementation, `BlockStructureRule
      .collapseSingleExpressionBlocks`/`isSingleStatementBody`, was read first as the working
      design precedent), not new architecture.

      `ScopePipelineIndent` gained a third constructor parameter, `lineLength` (new
      `Config.DEFAULT_LINE_LENGTH = 100` constant factored out of the previously-inline literal in
      `Config.java`; `FormatterIndent.formatOne` now passes `config.lineLength()` through), plus new
      `applySingleStatementBody`/`classifySingleStatementHeaderColon`/`bodyOpensNewBlock`/
      `physicalLineLength`/`containsComment` and a `SINGLE_STMT_HEADER_KEYWORDS` set (`if`/`elif`/
      `else`/`while`/`for` -- `case` is handled separately by delegating to the already-landed §7
      `classifyCaseLine`, since `case` is a context-sensitive soft keyword tokenized as a plain
      `IDENTIFIER`, not a member of this set; `def`/`class`/`try`/`except`/`finally`/`with` are
      deliberately never members, satisfying §8's explicit "never applies" list by construction, not
      a runtime check). For each `RawLine`, a header qualifies only when: single physical line, first
      significant token is a qualifying keyword (or `case` via `classifyCaseLine` with
      `compact == false`), and nothing but an optional trailing comment follows its own header-
      terminating `:` (i.e. genuinely block-form, not already compact -- STYLE_PYTHON3.md's own
      `if x: return y`/`while x: x -= 1` worked examples are recognized as already compact by this
      exact check and correctly left alone, never re-derived to a different compact spelling). The
      immediately following `RawLine` must exist, sit exactly one depth deeper, not be
      `multiPhysicalLine`, not be blank/comment-only, not itself carry a trailing comment
      (conservative skip, no worked example either way for this shape), and not itself open a
      further nested block (`bodyOpensNewBlock` -- a depth-0 `:` inside the body's own token span
      means it's itself a compound-statement header, e.g. nested `if`/`for`/`while`/`with`/`match`/
      `def`/`class`, and must keep its own indented block, never qualifies as the "simple statement"
      being joined). The line after the body (if any) must sit at a shallower depth than the body --
      a sibling line still at the body's own depth means the block held more than one statement (or
      a trailing blank/comment line), and the whole join is skipped, mirroring §2/§3's own
      conservative group-boundary posture. On success, one `Replacement` spans
      `[header.start, bodyContentEnd)` (deliberately NOT through the body's own trailing `NEWLINE`
      token -- that token is left alone so the line rendered afterward still terminates correctly;
      an earlier draft that included it in the replaced span produced a corrupted single line with
      no `\n` before whatever followed, caught immediately by the smoke test's very first case)
      with the joined `headerText + " " + bodyText` text; overflow (joined length exceeds
      `line-length`) leaves the block form completely untouched, per §8's own explicit rule.

      **Ambiguity resolved conservatively, not guessed, and confirmed via a dedicated smoke case:**
      a body statement containing `lambda` (e.g. `f = lambda: 1`) has its own top-level `:` that
      does not open a block, but reliably distinguishing that from a genuine nested-compound-
      statement `:` would need lambda-parameter-list-aware depth tracking this slice does not build.
      `bodyOpensNewBlock` (and the header's own condition-colon search in
      `classifySingleStatementHeaderColon`, for the same reason) conservatively treats any `lambda`
      keyword found anywhere in the relevant span as "opens a new block" -- i.e. never joined --
      sidestepping the ambiguity by leaving that narrow shape's block form untouched rather than
      risking a wrong join, per the task's own explicit "safe conservative default, not a genuine
      ambiguity requiring a stop" framing for exactly this shape. `:=` was never at risk of being
      mistaken for either kind of `:` search, since `TokenizerIndent.emitWalrus` already merges it
      into a single pre-tokenized `OP` token.

      **A nested compound statement's own header still legitimately gets its own independent join
      opportunity** -- discovered while writing the smoke test, not a bug: STYLE_PYTHON3.md §8's
      "never applies to a nested compound body" rule disqualifies the *outer* header from joining
      with a nested-if/for/etc. body, but the *inner* header, evaluated independently against the
      same original `RawLines`, can still qualify for its own join if its own sole body statement is
      itself simple (e.g. `if x:\n    if y:\n        return 1` correctly becomes
      `if x:\n    if y: return 1`, not left fully untouched) -- the first smoke-test draft asserted
      the wrong expectation here (assumed no joins at all) and was corrected once the actual,
      correct output was traced back to the rule's own wording.

      Verified via a 17-case smoke-test harness (`FormatterCore.forLanguage("python3").formatOne`
      direct construction, same pattern as every prior slice's own harness): STYLE_PYTHON3.md §8's
      own two already-compact worked examples left untouched, block-form `if`/`while`/`for` each
      joining correctly, a full `if`/`elif`/`else` chain joining each branch independently, the
      style doc's own `match`/`case` worked example (starting from block form) joining each `case`,
      the overflow worked example staying a block, `def`/`class`/`try`-`except`-`finally`/`with`
      each never joining even though grammatically permitted, a nested-compound-statement body
      (single nested `if`/`for`) leaving the *outer* header untouched while the *inner* header still
      independently joins, a nested compound body whose own inner block holds two statements leaving
      everything fully untouched, the `lambda`-body conservative skip, and idempotency of an
      already-joined line -- all 17 passed -- plus a full `make test` run (114/114 forward +
      idempotency, zero regressions; still compile/link-health only, `python3` stays in
      `Lang.SCAFFOLD_ONLY_LANGUAGES`, this pass reachable only via direct construction/test
      harnesses, same posture as every prior Python3 slice).

      **Explicitly NOT yet covered by this slice** (documented scope boundaries, not guesses): a
      header or body spanning more than one physical line (bracket/backslash continuation) is never
      a candidate, same "documented gap" precedent as every prior §2-§7 slice; a body carrying its
      own trailing same-line comment is conservatively skipped rather than carried onto the joined
      line; this pass never decides to *expand* an already-compact one-line form back into a block
      (STYLE_PYTHON3.md §8 names no such rule, and the task's own scope guidance explicitly excluded
      building it); no `;`-chaining is ever produced, moot since only exactly one statement is ever
      joined, per §8's own text. **A real, discovered (not guessed) interaction, same posture as
      §5's own documented one:** this pass's `Replacement` for a qualifying header+body span starts
      earlier than any inner replacement another pass (e.g. §2's assignment alignment, if the body
      statement is itself a simple assignment) might produce for a sub-span nested inside it --
      `render`'s "first match at this position wins, a later nested replacement is silently never
      reached" behavior means the §8 join always wins whole for that occurrence, and the inner
      pass's own narrower replacement is simply never applied there (not corrupted) -- not
      independently re-verified via its own dedicated smoke case this slice, since §5's own
      analogous case already established the render-merge behavior is safe.
      **§7 (Structural Pattern Matching, `match`/`case`) landed -- `:` column alignment-only
      slice.** New `ScopePipelineIndent.CaseLine`/`applyCaseColonAlignment`/`classifyCaseLine`/
      `flushCaseGroup` -- no new `MiscRuleIndent` method needed (unlike §2/§3/§6), since this pass
      only ever pads the gap between a case's own trimmed pattern text and its `:`, never rebuilds a
      whole rendered line; reuses the existing `normalizeGap`/`verbatimLineText`/`isOpenBracketText`/
      `isCloseBracketText`/`trimEndIdx` helpers already landed for §4-§6 rather than adding parallel
      ones. `classifyCaseLine` checks the line's first significant token's own literal text against
      `"case"` rather than `TokenType.KEYWORD`, since `case` (like `match`/`_`/`type`) is a
      context-sensitive soft keyword the tokenizer deliberately leaves as a plain `IDENTIFIER`
      (`STATE_PYTHON3.md`'s own tokenizer-slice note). The header-terminating `:` is found via a
      bracket-depth-0 scan starting right after `case` -- correctly skips a mapping pattern's own
      `{"action": action}` colon (inside `{}`, depth > 0) without any special-casing, and a guard
      clause's own `if x == y` is included in the pattern span by construction (the guard has no `:`
      of its own to stop the scan early, and `:=` is already a single pre-merged `OP` token per the
      tokenizer's own `emitWalrus`, so it was never at risk of being mistaken for the header colon
      either). `compact` is true iff at least one significant, non-comment token follows the `:` on
      the same physical line -- reads the body's existing same-line-vs-block shape exactly as
      written, never decides to compact or expand it, same posture §6 takes toward an
      already-broken-out signature. Grouping mirrors §2/§3's own shape: contiguous same-depth `case`
      lines form one group, broken by a blank line, a comment-only line, a depth change, or any
      non-`case` statement (including the enclosing `match subject:` header itself, which never
      classifies as a `case` line and so naturally closes out any group before it). **All-or-nothing**
      (STYLE_PYTHON3.md §7's own explicit rule): `flushCaseGroup` emits zero replacements for the
      entire group the moment any member is block-form, even for that group's own compact members --
      checked before any padding-width computation, not filtered case-by-case. For an all-compact
      group, each member's replacement covers only `[patternEnd, colonIdx)` (a whitespace-only gap,
      via the shared `normalizeGap`), padded with `maxLen - thisLen` spaces so every pattern's
      trimmed text lines up flush before an aligned `:` column -- the pattern/body text itself is
      never touched, only the gap immediately before the colon.

      **Points 4-7 of the task's own scope guidance verified as smoke-tested, not newly built** (or
      pre-existing documented gaps, not something this slice papers over): or-pattern `|` spacing and
      guard-clause `if`/`==` spacing both survive already-correct source untouched (verified via
      dedicated smoke cases) because there is still no general expression/operator-respacing pass for
      Python3 anywhere in the `*Indent` family -- the same gap §5's own slice already documented (its
      own "no general expression-spacing primitive exists" note) -- so this is confirmed pre-existing,
      not introduced or newly deferred by §7. Wildcard `_` is an ordinary `IDENTIFIER` token with no
      new rule needed, confirmed via a standalone smoke case, and already participates correctly in
      compact-group alignment (the varying-pattern-width smoke case includes a `case _:` member).
      Deconstruction/sequence/mapping pattern content (`Point(x=0, y=0)`, `[1, 2, *rest]`, `{"k": v}`)
      is not repadded by any bracket-complexity-driven rewrite in this slice -- `PythonBracketComplexityEvaluator`
      exists and *could* classify such content per §1, but nothing wires it into case-pattern
      rendering the way §4 wires it into decorator-call padding; this slice's own bracket-depth scan
      in `classifyCaseLine` only uses bracket nesting to find the header colon safely, never to
      repad the pattern's own internal spacing. Not treated as a new gap requiring documentation as
      a checklist blocker -- narrowing to `:`-column alignment only was this checkpoint's explicit,
      pre-agreed scope, and every worked example in STYLE_PYTHON3.md §7 itself already has correctly
      spaced pattern content, so nothing in the style doc's own examples is left unsatisfied.

      **Point 8 (closing comment) explicitly deferred, confirmed absent rather than guessed.** Grepped
      the whole `src/com/jxmake/formatter/` tree for `closing-comment`/`closingComment`/
      `ClosingComment` before writing any code: every hit lives in `Config.java` (the shared config
      key itself), `FormatterCurly.java`, `ScopePipelineCurly.java`, `SwitchRule.java`,
      `CppSpecificRule.java`, `KotlinSpecificRule.java`, `MiscRuleCore.java`, `BlockStructureRule.java`,
      `JsTsSpecificRule.java` -- entirely Curly-family (and JS/TS's own `Tags`-adjacent equivalent),
      zero hits anywhere in the `*Indent` family for any Python3 construct, not even `def`/`class`.
      Building the mechanism from scratch as a side effect of `match`'s own closing comment would be
      exactly the "large amount of missing supporting infra, not a narrow addition" case the task's
      own guidance said to defer on -- left uncovered here, consistent with that guidance, not folded
      into this checkpoint.

      Verified via an 8-case smoke-test harness (`FormatterCore.forLanguage("python3").formatOne`
      direct construction, same pattern as every prior slice's own harness): STYLE_PYTHON3.md §7's own
      block-body worked example left byte-for-byte untouched, its own compact-form worked example
      round-tripping unchanged (idempotency, all three patterns equal width already), a compact group
      with varying pattern widths (`Point(x=0, y=0)` vs `_`) correctly `:`-aligned, a mixed
      compact/block group abandoning alignment entirely (all-or-nothing), or-pattern (`1 | 2 | 3`) and
      guard-clause (`if x == y`) spacing both surviving untouched, a standalone wildcard `case _:`
      block left untouched, and idempotency of the varying-width case's own aligned output -- all 8
      passed -- plus a full `make test` run (114/114 forward + idempotency, zero regressions; still
      compile/link-health only, `python3` stays in `Lang.SCAFFOLD_ONLY_LANGUAGES`, this pass reachable
      only via direct construction/test harnesses, same posture as every prior Python3 slice).

      **§9 (Control Flow Blank Lines) landed -- last §2-9 sub-item, checklist item now
      complete.** New `ScopePipelineIndent.ControlFlowFrame`/`applyControlFlowBlankLines`/
      `isDefHeaderLine`/`previousContentLine`/`isUnconditionalExitLine`/`insertBlankLineBefore`.
      Read `MiscRuleCurly#insertBlankLineBeforeReturn` (STYLE.md §9) first as §9.1's C-family
      reference -- ported faithfully: a blank line is inserted directly before a `return` when it
      is the first significant token of its own logical line AND the innermost enclosing frame is
      a function body (opened by a `def`/`async def` header, tracked via a `ControlFlowFrame`
      stack keyed by `RawLine.depth`, mirroring `FuncFrame`'s own `isFunctionBody`/`sawContent`
      shape) that has already seen at least one statement. Like the C-family reference, this does
      NOT separately re-verify the return is the body's textually final statement (the reference
      implementation never adds that lookahead either, despite STYLE.md's own prose describing the
      common case) -- a faithful port of the actual code, not the prose. The §8 compact-form
      carve-out (`if x: return y` must never get an inner blank line) needs no special-case code at
      all: a compact line's own first significant token is the header keyword (`if`), never
      `return`, so it is never classified as a return statement in the first place -- confirmed via
      a dedicated smoke case, not assumed.

      **§9.2 could NOT be ported from a C-family mechanism, because none exists to port --
      discovered, not guessed, during the required pre-implementation grep.** The task's own
      nominal C-family reference for elif/else blank-line placement, `BlockStructureRule
      .placeElseOnOwnLine` (STYLE.md §12), was read in full and turned out to only ever *preserve*
      whatever blank-line state already exists directly before `else` -- its own javadoc states
      "§12 treats that blank line as an optional, context-driven separator... that this method
      must never add or remove on its own." There is no last-statement-content check anywhere in
      the C-family source (`return`/`break`/`continue` string literals do not appear together
      anywhere relevant). Since STYLE_PYTHON3.md §9.2's own text is unambiguous ("add a blank line
      before elif/else only when...") and gives a fully worked rule with no missing information,
      this was treated as a documented implementation-detail discrepancy between the task's framing
      and actual C-family behavior -- not a STATE_COMMON.md-blocking ambiguity -- and §9.2 was
      implemented directly from the style doc's own text: a blank line is inserted before any
      `elif`/`else` (any `else`, not just `if`-`else` -- Python's `for`/`while`/`try` `else` is the
      same keyword and the style doc draws no distinction) whose nearest preceding non-blank,
      non-comment logical line (`previousContentLine`, which walks backward past intervening
      blank/comment lines regardless of their own depth) has `return`/`break`/`continue` -- never
      `raise`, matching the C-family list's own existing omission -- as its own first significant
      token. Walking past blank/comment lines rather than requiring strict adjacency is what finds
      the deepest last leaf statement of the preceding block with no depth bookkeeping of its own:
      Python's strict block nesting guarantees the last non-blank/non-comment line before
      elif/else is always that block's own deepest last leaf statement, however many levels of
      nested compound statements deep -- confirmed by construction via the smoke harness's own
      nested `for`/`if`/`elif` case, not guessed.

      Both halves only ever ADD a missing blank line, never remove one -- mirrors
      `insertBlankLineBeforeReturn`'s own "an already-blank gap is left untouched" posture exactly
      (verified via a dedicated smoke case: a 2-blank-line gap before a qualifying `return` is left
      at 2, never collapsed to 1). A `return`/`elif`/`else` whose immediately preceding line is a
      comment-only line (no blank line already there) is conservatively left untouched -- same
      "no worked example to guess a relocation from" posture the C-family reference itself uses for
      Java/C++ (as opposed to Kotlin's own separately-carved-out comment-relocation behavior, which
      has no Python analog here).

      **Explicitly NOT covered by this slice** (documented scope boundaries, not guesses, same
      precedent as every prior §2-§8 slice): a `def` header itself written as a multi-physical-line
      (wrapped) parameter list is never recognized as function-body-opening by `isDefHeaderLine`
      (conservatively returns false for any `multiPhysicalLine` header), silently disabling §9.1
      for such a function's own `return` statements -- same posture as §6's own already-documented
      multi-physical-line-signature gaps. A semicolon-chained statement (`x = 1; return y` on one
      physical line) is never recognized as a return/exit statement by either half of this rule,
      since only a logical line's own first significant token is ever inspected -- no
      STYLE_PYTHON3.md worked example exercises semicolon-chaining anywhere in this job. A §8-compact
      preceding block whose own sole statement happens to be `return`/`break`/`continue` (e.g. an
      already-compact `if x: return y` as the block immediately preceding an `elif`/`else`) is never
      recognized by §9.2 as ending in an exit, by the same by-construction reasoning as §9.1's own
      compact-form exclusion (that line's own leading token is `if`, not `return`) -- a real,
      deliberate consequence, not independently re-verified via its own dedicated smoke case this
      slice (documented here rather than silently guessed). `try`/`except`/`finally` blank-line
      placement is entirely out of scope -- STYLE_PYTHON3.md §9.2's own text names only
      `elif`/`else`.

      Verified via a 14-case smoke-test harness (`FormatterCore.forLanguage("python3").formatOne`
      direct construction, same pattern as every prior slice's own harness): STYLE_PYTHON3.md §9.1's
      own `process` worked example (idempotent) and a variant with the blank line missing (inserted
      correctly), STYLE_PYTHON3.md §9.2's own `check` worked example (idempotent -- this example, on
      inspection, has no `elif`/`else` in it at all and mainly exercises §9.1's own final-return
      case, not §9.2, since the style doc's own two named sections happen to share one combined
      example), a nested return never qualifying, a single-statement function body never forcing a
      blank line, the §8 compact-form interaction (inner compact return untouched, following
      real return still qualifies), `elif`/`else` after a block ending in `raise` (no blank,
      excluded), after a block NOT ending in an exit (no blank), after a block ending in `return`/
      `break`/`continue` respectively (blank inserted in each case, including a nested `for`/`if`/
      `elif` case proving the deepest-leaf-statement lookup), idempotency of an already-correct
      elif/else case, an extraneous 2-blank-line gap left uncollapsed (never-remove posture), and a
      comment directly preceding a qualifying `return` left untouched (conservative skip) -- all 14
      passed -- plus a full `make test` run (114/114 forward + idempotency, zero regressions; still
      compile/link-health only, `python3` stays in `Lang.SCAFFOLD_ONLY_LANGUAGES`, this pass
      reachable only via direct construction/test harnesses, same posture as every prior Python3
      slice).
- [x] Author local test fixture pairs per `FUTURE_TEST_FIXTURES.md`'s
      "Python3" section and register in the Makefile's `INP_FILES` /
      `test/README.txt`. Done: `py_combined_inp/out.py` and
      `py_comments_inp/out.py` extracted to `test/`, registered
      commented-out in the Makefile (real logic not yet implemented),
      documented in `test/README.txt`.
- [ ] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      `STYLE_PYTHON3.md`'s listed test-fixture repos (`python/cpython`,
      `pallets/flask`, `django/django`, `psf/black`, `pallets/click`).
