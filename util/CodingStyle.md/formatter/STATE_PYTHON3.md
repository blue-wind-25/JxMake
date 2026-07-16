# STATE_PYTHON3.md — Python 3 JAR Support Tracker

Read `STATE_COMMON.md` first — it has the shared commit/ambiguity/testing
conventions this file assumes. `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` (the
other jobs' files) are NOT required reading for this one — only
`STATE_COMMON.md` is.

---

## Purpose

Tracks implementation of Python 3 support in the deterministic JAR formatter
(`util/CodingStyle.md/formatter/`), per `STYLE_PYTHON3.md`. Python has real
imperative-language surface different enough from every currently-supported
brace-delimited language (significant whitespace, several bracket-content
categories with no C-family analog) that most of its rules are new, not
inherited. **Current status is scaffold-only: dispatch exists only as a
"not yet implemented" error thrown for Python constructs, no real
formatting logic exists yet.**

---

## Scope

`STYLE_PYTHON3.md` covers latest supported Python 3 (currently 3.15+);
Python 2 is not supported. **Indentation note carried from the style doc:**
unlike every other currently-supported language, Python's indentation is
semantically load-bearing — the formatter must never change indentation
depth in a way that would alter which block a statement belongs to. This
constrains every rule below.

Sections:

1. Bracket complexity detector — extends the existing tight/loose
   heuristic (atoms/simple ops tight, call or nested bracket loose) with
   Python-only categories: comprehensions (always loose, own bucket, not
   atom/call/nested-bracket), slicing (`:` inside `[]` never padded,
   evaluated as its own sub-expression for the outer tight/loose call),
   star-unpacking (`*args`/`**kwargs`, tight, doesn't itself force
   looseness), dict-vs-set literal disambiguation (top-level `:` present →
   dict rules; none → set/list rules; empty `{}` → dict).
2. Assignment alignment — `=` (and augmented `+=`/`-=`/etc.) column-aligned
   across adjacent simple assignments, same group/break rules as the
   existing declaration-alignment mechanism; multi-line RHS continuation
   alignment target unchanged (break-before/after-operator).
3. Import ordering — deliberately simpler than PEP 8/`isort`'s stdlib/
   third-party/local tiers: **no such classification at all.** Sort key
   (import-before-from, then alphabetical), grouping (any non-import
   statement breaks the group, including nested-block imports forming
   their own separate group), `from __future__ import ...` promoted to
   the top of its own group. Config: `python-import-sort`,
   `python-import-blank-lines`.
4. Decorators — always own-line by grammar (no placement ambiguity), `()`
   content reuses §1's bracket rules, tight `@`; overflow wraps the call's
   argument list. `@property`/`@x.setter` get no special
   getter/setter-group alignment (unlike JS/TS/C-family) since Python
   function bodies never compact.
5. F-strings — expression portion inside `{...}` gets normal expression
   spacing; everything from `!conversion`/`:format_spec` onward is opaque,
   preserved exactly as written.
6. Function signature wrapping — existing inline-vs-one-per-line rule
   applies as-is; alignment target is `:` (and `=` when a default is
   present) rather than a type column, since Python's declaration order is
   `name: type = default`. Return-type arrow stays fixed on the closing
   `)` line.
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
   when the preceding block's last statement is `return`/`break`/
   `continue` (not `raise`).

No `src/` files yet — scaffold dispatch lives in the shared
`Lang.java`/`Main.java`/`ServerMode.java`/`Config.java`, described in the
routing `CLAUDE.md` table; this job's own rule classes (a future
`PythonSpecificRule.java` or similar) do not exist yet.

---

## Resolved Design Decisions

Full text of each decision lives in `RDD_LOG.md` (shared with
`STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` — continue the existing `RDD_KEY_n`
numbering, do not restart). See `STATE_COMMON.md`'s lookup convention
(`grep -Fm1`, no `-A`).

| Key | Topic |
|---|---|
| — | none yet |

---

## Open Questions

None currently — `STYLE_PYTHON3.md`'s own "Known Open Items" (§10) states
its prior open items (decorators, f-strings, type-hint signature wrapping)
were already resolved via Q&A and folded into §4–§6; nothing is left
unresolved in the style doc itself as of this session.

---

## Checklist

- [ ] Tokenizer support pass: survey `STYLE_PYTHON3.md` for every token/
      construct not already lexed correctly (f-string interpolation
      boundaries, `:=` walrus if present, significant-whitespace/indent
      tracking as a first-class tokenizer concern rather than braces,
      `match`/`case` soft keywords) — additive only, re-run the full
      existing C/C++/Java/Kotlin regression suite for zero regressions
      before moving on (same discipline `STATE_KOTLIN.md` Step 0 used).
      Indentation being semantically load-bearing (not just cosmetic) is
      the single biggest structural difference from every existing
      language and should be resolved architecturally in this step, before
      any statement-level rule work begins.
- [ ] Implement basic statement/indentation formatting first (the Python
      analog of a "get the skeleton right" starting point, since there are
      no braces to reuse the existing block-structure rule against) —
      confirm indentation depth is never altered in a way that changes
      block membership before any cosmetic rule (alignment, spacing,
      compaction) is layered on top.
- [ ] Implement §1 (bracket complexity detector, including its four
      Python-only sub-categories) as the next foundational piece, since
      §2/§6/§7 all depend on its alignment/padding primitives.
- [ ] Implement §2–9 rule-by-rule, each its own checkpoint commit, per
      `STATE_COMMON.md`'s workflow.
- [ ] Author local test fixture pairs per `FUTURE_TEST_FIXTURES.md`'s
      "Python3" section and register in the Makefile's `INP_FILES` /
      `test/README.txt`.
- [ ] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      `STYLE_PYTHON3.md`'s listed test-fixture repos (`python/cpython`,
      `pallets/flask`, `django/django`, `psf/black`, `pallets/click`).
