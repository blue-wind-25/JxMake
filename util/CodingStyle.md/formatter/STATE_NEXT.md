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
| `CppSpecificRule.java` (structured bindings) | NOT STARTED |
| `CppSpecificRule.java` (concepts/requires) | NOT STARTED |

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
- [ ] Structured bindings — atomic name-cell in existing §5 grid, plus internal
      `[a, b, c]` spacing rule (STYLE_CPP20.md §1).
- [ ] Concepts / `requires` clauses — K&R brace style; `requires` trails `)`
      always, wraps only past 100 chars; nested compound requirements untouched
      (resolved — see STYLE_CPP20.md §2 and §5 resolved decisions table).
- [ ] `<=>`, coroutines, init-statement `if`/`switch` — all resolved as needing
      zero new rules (see STYLE_CPP20.md §4 and §5 resolved decisions table).
      Verify no-op assumption holds during implementation.

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

---

## End Goal (Phase 2)

> `Main.java`, `README.md`, and the Dogfood test (originally tracked here as "End Goal
> (Phase 1)", moved from `STATE.md` per RDD_KEY_82) have moved again, to
> `STATE_NEXT_EXT.md`'s Phase 3 checklist, just before its "Step 2 — AI integration" —
> see that file's gate note for why. Only the AI_PREAMBLE trim item remains here.

- [ ] Trim `AI_PREAMBLE.md` back to Tier-3-only content (function-call
      line-breaking, non-standard getter/setter naming — the genuinely
      AI-only judgment calls per `FORMATTER_DISCUSSION.md`'s "Future:
      AI-Assisted Formatting" section). Everything now resolved by the JAR
      (§7 nesting, §12 blank line, §13 inline alignment, §14 outlier exclusion,
      §15 capitalization, and this phase's Java17+/C++20+ additions) should be
      removed from `AI_PREAMBLE.md` since it is no longer ambiguous —
      it is documented, implemented behavior.

---

## After Phase 2

Once End Goal (Phase 2) above is checked off, continue with
[`STATE_NEXT_EXT.md`](STATE_NEXT_EXT.md) for:
- Phase 3 — JAR `ai-assist` integration (local on-device AI for Tier-3 judgment calls)
  (also now owns `Main.java`, `README.md`, and the Dogfood test — see that file)
- Post-phase-3 cleanup — `JXMAKE_` / `jxmake_` prefix rename for all env vars and
  config keys

**Do not read `STATE_NEXT_EXT.md` until End Goal (Phase 2) above is checked off.**
