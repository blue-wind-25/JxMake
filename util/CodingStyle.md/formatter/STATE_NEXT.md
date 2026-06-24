# STATE_NEXT.md — Phase 2 Tracker (Java 17+ / C++20+ Constructs)

> **Active (RDD_KEY_82 in `STATE_rdd_log.md`).** Phase ordering was reversed: this file's
> Java 17+/C++20+ checklists are implemented *before* `STATE.md`'s original End Goal
> (`Main.java`, `README.md`, the Tier 1/Tier 2 self-dogfood test). That End Goal has since
> moved again, past this file entirely, into `STATE_NEXT_EXT.md`'s Phase 3 checklist,
> positioned just before its "Step 2 — AI integration" — because Phase 3's "Step 1 —
> Deterministic extensions" also lands new branches inside already-COMPLETE rule classes
> (`MiscRule.java`'s call/declaration line-breaking), so the dogfood checkpoint needs to sit
> *after* Step 1 too, to catch regressions from Phase 2 and Step 1 in one combined pass
> before the riskier AI-integration work begins. This file's own gate to `STATE_NEXT_EXT.md`
> is unchanged: still controlled by this file's End Goal (Phase 2) milestone below.

---

## Purpose

Tracks implementation of [`STYLE_JAVA17.md`](../STYLE_JAVA17.md) and
[`STYLE_CPP20.md`](../STYLE_CPP20.md) — newer-language-construct support added
**after** the core formatter (Tier 1 + Tier 2, all of `STYLE.md` /
`STYLE_C_CPP.md` / `STYLE_JAVA.md`) is complete and dogfood-verified.

**Hard constraint:** none of this work may break the existing, already-complete
implementation. Every item below must be additive — new branches in existing rule
classes, new modifier-priority entries, new rule classes where a construct doesn't
fit an existing one — never a rewrite of already-COMPLETE logic. If an item turns
out to require changing existing behavior, stop and ask before proceeding, same
ambiguity protocol as `STATE.md`.

---

## File Status

| File | Status |
|---|---|
| `JavaModifierPriority.java` (sealed/non-sealed addition) | COMPLETE (see RDD_KEY_1; `TokenizerCore.java` and `JavaSpecificRule.java` also touched -- new keywords, new `enforcePermitsClauseLineBreaking` pass) |
| `JavaSpecificRule.java` (record) | COMPLETE (see RDD_KEY_2; `TokenizerCore.java` and `BlockStructureRule.java` also touched) |
| `JavaSpecificRule.java` (switch expressions) | COMPLETE (new `enforceSwitchExpressionArrowAlignment`, wired into `Formatter.java`; no RDD needed -- STYLE_JAVA17.md §7 already pre-resolved the only design question, the block-body all-or-nothing bail-out) |
| `TokenizerCore.java` (text blocks) | COMPLETE (new `isTextBlockOpener`/`emitTextBlock`, opaque `STRING` token spanning the whole block, mirrors `emitBlockComment`'s internal-newline pattern; no RDD needed) |
| `DeclarationAlignmentRule.java` (`var`) | COMPLETE (added `"var"` to `TYPE_KEYWORDS_JAVA`; confirmed-not-no-op, see below) |
| `JavaSpecificRule.java` (pattern matching) | COMPLETE (confirmed true no-op, zero code changes; see below) |
| `CppModifierPriority.java` (consteval/constinit addition) | COMPLETE (see below) |
| `DeclarationAlignmentRule.java` (structured bindings) | COMPLETE (see below; landed here, not in a new `CppSpecificRule.java`, since `parseDeclaration`/`render`'s existing machinery already covered it additively) |
| `CppSpecificRule.java` (concepts/requires) | COMPLETE (see RDD_KEY_3; `TokenizerCore.java` and `BlockStructureRule.java` also touched -- new keywords, `pendingConceptName`, `isConceptRequiresExpressionBody`. Also fixed in the same pass: `<=>` tokenization and `co_await`/`co_return`/`co_yield` keywords for the `<=>`/coroutines/init-statement checklist item below -- both pre-existing TokenizerCore gaps, no RDD needed) |

---

## Checklist — C++17/20/23

- [x] `consteval` / `constinit` — new `CppModifierPriority` columns, order
      `constexpr → consteval → constinit` (resolved — see STYLE_CPP20.md §3 and
      §5). Verified `constexpr` was NOT already present in `CppModifierPriority`
      (the "unlikely given phase-1 work" caveat turned out true) -- added it
      alongside `consteval`/`constinit`, all three sharing one rank (mutually
      exclusive, same shared-column precedent as `JavaModifierPriority`'s
      `abstract`/`final`/`sealed`, RDD_KEY_1), placed between `static` and the
      existing `volatile`/`const` ranks, which were renumbered up by one to make
      room. `consteval`/`constinit` were also missing from `TokenizerCore`'s
      `KEYWORDS_CPP` (only `constexpr` was present) -- added both. Renumbering
      `volatile`/`const` is safe because `DeclarationAlignmentRule.render`
      already omits any column inactive for the whole group rather than
      rendering dead padding -- confirmed via a pristine-baseline diff showing
      byte-for-byte identical output for a mixed static/const/volatile/pointer/
      bitfield group, plus a worked-example harness for `constexpr`/`constinit`
      declarations aligning correctly and idempotently.
      **`auto` (as data type) alignment** deferred to the structured-bindings
      item below -- `auto` is already a recognized type keyword
      (`TYPE_KEYWORDS_CPP`) from prior work; STYLE_CPP20.md §1 is where its
      grid behavior is actually specified.
- [x] Structured bindings — atomic name-cell in existing §5 grid, plus internal
      `[a, b, c]` spacing rule (STYLE_CPP20.md §1). Implemented as a new
      `parseStructuredBinding` helper called from `parseDeclaration` (cpp only,
      right after the modifiers loop and before the bitfield `colonIdx` scan --
      required ordering, since that loop scans the whole body for `:` with no
      `=`/end limit and would wrongly match a `:` inside a ternary in the
      binding's initializer). Detection: scan forward from the first non-modifier
      token for the first top-level `[`; abort (fall through to normal parsing)
      if an IDENTIFIER or top-level `=` is seen first, since legal structured-
      binding type prefixes are only `auto`/`const`/`volatile`/`&`/`&&` -- never
      a real name -- so this can't false-positive on an ordinary `Type name[n]`
      array declaration. Reused `renderTokens()` unmodified for the bracket-list
      spacing (it already produces canonical `[a, b, c]` comma/bracket spacing
      with zero new code, confirmed by trace). Splice-back anchoring (see
      `ScopePipeline.applyDeclarationsPass`) requires `Declaration.name` to be a
      *real* token instance from the input for its identity-keyed index map --
      first attempt synthesized a brand-new `Token` for the whole bracket text
      and hit an NPE there (not in `Declaration`/`render` themselves). Fixed by
      keeping `name` = the real `[` token and folding the interior tokens +
      closing `]` into `sizeTokens` (exactly like a real array-size suffix
      would), so `renderNameCell`'s existing `name.text + renderTokens(sizeTokens)`
      concatenation produces the atomic `[a, b, c]` cell with no changes to
      `renderNameCell`/`render`. Verified via a 6-case harness: atomic name-cell
      alongside plain declarations, idempotency, internal spacing
      normalization, `auto&`/`const auto&` qualified bindings, trailing-comment
      column alignment still correct with `[a, b, c]` as the name cell, and two
      regressions confirmed unaffected (`int arr[3] = {...}` real array decl,
      and bracket-less `auto x = 1;`). Also reran all prior Phase-2 harnesses
      (var, pattern matching, switch expressions, text blocks, consteval/
      constinit) with zero failures.
- [x] Concepts / `requires` clauses — K&R brace style confirmed a true no-op;
      `requires` trails `)` always, wraps only past 100 chars; nested compound
      requirements untouched (see STYLE_CPP20.md §2 and §5 resolved decisions
      table; implementation in RDD_KEY_3, updated with two discovered gaps).
      Implemented: `"requires"`/`"concept"` added to `KEYWORDS_CPP`; `"concept"`
      added to `NAMED_CONSTRUCT_CPP`; new `CppSpecificRule.enforceRequiresClausePlacement`
      wired after `enforceEmptyParameterList`. Two additive gaps found and fixed
      beyond the original design (both documented in RDD_KEY_3): (1)
      `TokenizerCore`'s 2-token construct-name lookback can't see `concept Name =
      requires(...) {` across the parameter list — fixed with `pendingConceptName`,
      mirroring `record`'s `pendingRecordName`. (2) `BlockStructureRule.classifyNamed`
      had the same gap for closing-comment labels — fixed with
      `isConceptRequiresExpressionBody`, checked *before* `findRecordComponentListClose`
      (which would otherwise intercept the same `)`-before-`{` shape first).
      Verified via a 7-case harness (K&R no-op, inline-fits, wrap-then-collapse,
      too-long wrap, requires-expression-body untouched, prototype semicolon,
      comment-blocks-rewrite) plus closing-comment/blank-line cases, all idempotent.
- [x] `<=>`, coroutines, init-statement `if`/`switch` — confirmed all three need
      zero new formatting rules (see STYLE_CPP20.md §4 and §5 resolved decisions
      table), but verification surfaced two missing-keyword gaps, both fixed:
      `<=>` was tokenized as `<=` + `>` because `MULTI_CHAR_OPS` lacked it
      entirely — added, ordered *before* `<=` (a strict prefix) since
      `emitOperator` is first-match-wins. `co_await`/`co_return`/`co_yield` were
      not in `KEYWORDS_CPP`, so `DeclarationAlignmentRule` misparsed
      `co_yield value;` as a fake field declaration — added all three as
      keywords. init-statement `if`/`switch` needed no code at all, confirmed
      via smoke test. Verification also surfaced a pre-existing, unrelated bug
      (`auto x = regularFunc();` renders as `auto x = regularFunc ( );`,
      reproduced on the pristine pre-session build with no coroutine involved at
      all) — out of scope for this checklist item, left unfixed and undocumented
      beyond this note; a future Tier-1 `DeclarationAlignmentRule` pass should
      pick it up.

---

## Resolved Design Decisions

Full decision text lives in `STATE_NEXT_rdd_log.md` — **do not read that file in full**.
To look up a specific decision during implementation:
```
grep -Fm1 'RDD_KEY_n' util/CodingStyle.md/formatter/STATE_NEXT_rdd_log.md
```

| Key | Topic |
|---|---|
| RDD_KEY_1 | `JavaModifierPriority` column order for `abstract`/`sealed`/`non-sealed`/`final`/`volatile` -- declaration-kind-specific orderings merged into one map |
| RDD_KEY_2 | `record` named-construct detection through component list / `implements` clause / compact constructor -- three additive lookback extensions, one regression caught and fixed during verification |
| RDD_KEY_3 | concepts/`requires` implementation in `CppSpecificRule.java` -- `concept` K&R likely no-op (verify), new `enforceRequiresClausePlacement` using `)` predecessor to distinguish trailing clause from requirements expression body, `concept` added to `NAMED_CONSTRUCT_CPP` |

---

## End Goal (Phase 2)

> `Main.java`, `README.md`, and the Dogfood test (originally tracked here as "End Goal
> (Phase 1)", moved from `STATE.md` per RDD_KEY_82) have moved again, to
> `STATE_NEXT_EXT.md`'s Phase 3 checklist, just before its "Step 2 — AI integration" —
> see that file's gate note for why. Only the AI_PREAMBLE trim item remains here.

- [ ] Verify `AI_PREAMBLE_FULL.md` and `AI_PREAMBLE_AESTHETIC.md` are present
      in `util/CodingStyle.md/` alongside `STYLE.md`, and that `README.txt`
      references both filenames correctly. If `AI_PREAMBLE.md` still exists,
      delete it (superseded). No other action needed.

---

## After Phase 2

Once End Goal (Phase 2) above is checked off, continue with
[`STATE_NEXT_EXT.md`](STATE_NEXT_EXT.md) for:
- Phase 3 — JAR `ai-assist` integration (local on-device AI for Tier-3 judgment calls)
  (also now owns `Main.java`, `README.md`, and the Dogfood test — see that file)
- Post-phase-3 cleanup — `JXMAKE_` / `jxmake_` prefix rename for all env vars and
  config keys

**Do not read `STATE_NEXT_EXT.md` until End Goal (Phase 2) above is checked off.**
