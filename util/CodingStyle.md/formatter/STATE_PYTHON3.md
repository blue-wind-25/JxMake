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
- [~] Implement §2–9 rule-by-rule, each its own checkpoint commit, per
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
- [x] Author local test fixture pairs per `FUTURE_TEST_FIXTURES.md`'s
      "Python3" section and register in the Makefile's `INP_FILES` /
      `test/README.txt`. Done: `py_combined_inp/out.py` and
      `py_comments_inp/out.py` extracted to `test/`, registered
      commented-out in the Makefile (real logic not yet implemented),
      documented in `test/README.txt`.
- [ ] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      `STYLE_PYTHON3.md`'s listed test-fixture repos (`python/cpython`,
      `pallets/flask`, `django/django`, `psf/black`, `pallets/click`).
