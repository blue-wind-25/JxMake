# STATE_NEXT.md — Phase 2 Tracker (Java 17+ / C++20+ Constructs)

> **COMPLETE.** Every checklist below is `[x]` and the End Goal (Phase 2) milestone is
> checked off. There is nothing left to resume in this file — continue in
> [`STATE_NEXT_EXT.md`](STATE_NEXT_EXT.md) (Phase 3) instead. This file is kept as the
> historical record for Phase 2's File Status table and Resolved Design Decisions index;
> do not reopen it unless a Phase-2-era construct needs a fix (see the relevant `RDD_KEY_n`
> first via `grep -Fm1` against `STATE_NEXT_rdd_log.md`).
>
> Phase ordering note (RDD_KEY_82 in `STATE_rdd_log.md`): this file's Java 17+/C++20+
> checklists ran *before* `STATE.md`'s original End Goal (`Main.java`, `README.md`, the
> Tier 1/Tier 2 self-dogfood test), which has since moved again into `STATE_NEXT_EXT.md`'s
> Phase 3 checklist, just before its "Step 2 — AI integration" step.

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
| `DeclarationAlignmentRule.java` (`var`) | COMPLETE (added `"var"` to `TYPE_KEYWORDS_JAVA`; confirmed-not-no-op) |
| `JavaSpecificRule.java` (pattern matching) | COMPLETE (confirmed true no-op, zero code changes) |
| `CppModifierPriority.java` (consteval/constinit addition) | COMPLETE (new shared-rank column for `constexpr`/`consteval`/`constinit` between `static` and `volatile`/`const`; `consteval`/`constinit` also added to `KEYWORDS_CPP` -- `constexpr` alone was already present) |
| `DeclarationAlignmentRule.java` (structured bindings) | COMPLETE (new `parseStructuredBinding` helper in `parseDeclaration`, cpp only; landed here rather than in a new `CppSpecificRule.java` method since `render`'s existing machinery already covered the atomic `[a, b, c]` name-cell additively) |
| `CppSpecificRule.java` (concepts/requires) | COMPLETE (see RDD_KEY_3; `TokenizerCore.java` and `BlockStructureRule.java` also touched -- new keywords, `pendingConceptName`, `isConceptRequiresExpressionBody`. Also fixed in the same pass: `<=>` tokenization and `co_await`/`co_return`/`co_yield` keywords for the `<=>`/coroutines/init-statement checklist item below -- both pre-existing TokenizerCore gaps, no RDD needed) |

---

## Checklist — C++17/20/23 (all complete, see File Status table above for detail)

- [x] `consteval` / `constinit`
- [x] Structured bindings
- [x] Concepts / `requires` clauses (RDD_KEY_3)
- [x] `<=>`, coroutines, init-statement `if`/`switch` -- also fixed two missing-keyword
      tokenizer gaps surfaced by verification (`<=>` in `MULTI_CHAR_OPS`,
      `co_await`/`co_return`/`co_yield` in `KEYWORDS_CPP`). Surfaced but left unfixed as
      out-of-scope: a pre-existing, unrelated bug where `auto x = regularFunc();` renders
      as `auto x = regularFunc ( );` -- reproduces on the pristine pre-Phase-2 build, a
      future Tier-1 `DeclarationAlignmentRule` pass should pick it up.

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

- [x] Verify `AI_PREAMBLE_FULL.md` and `AI_PREAMBLE_AESTHETIC.md` are present
      alongside `STYLE.md`, and `README.txt` references both correctly --
      delete `AI_PREAMBLE.md` if it still exists (superseded). Verified clean:
      both files present, references correct, no stale file to delete.

---

## After Phase 2

Once End Goal (Phase 2) above is checked off, continue with
[`STATE_NEXT_EXT.md`](STATE_NEXT_EXT.md) for:
- Phase 3 — JAR `ai-assist` integration (local on-device AI for Tier-3 judgment calls)
  (also now owns `Main.java`, `README.md`, and the Dogfood test — see that file)
- Post-phase-3 cleanup — `JXMAKE_` / `jxmake_` prefix rename for all env vars and
  config keys

**Do not read `STATE_NEXT_EXT.md` until End Goal (Phase 2) above is checked off.**
