# STATE_NEXT.md — Phase 2 Tracker (Java 17+ / C++20+ Constructs)

> **DO NOT READ OR IMPLEMENT AGAINST THIS FILE YET.**
> This file is gated until `STATE.md`'s End Goal dogfood-test milestone is marked
> complete. If you are a Claude CLI session and you have arrived here before that
> milestone is checked off, stop — return to `STATE.md` instead.

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
| `JavaModifierPriority.java` (sealed/non-sealed addition) | NOT STARTED |
| `JavaSpecificRule.java` (record, switch expressions, text blocks, var, pattern matching) | NOT STARTED |
| `CppModifierPriority.java` (consteval/constinit addition) | NOT STARTED |
| `CppSpecificRule.java` (structured bindings) | NOT STARTED |
| `CppSpecificRule.java` (concepts/requires) | NOT STARTED — blocked, see Open Questions |
| `README.md` (update) | NOT STARTED |

---

## Checklist — Java 17+

- [ ] `record` — treat as `class` for brace style, closing comment, forced blank
      lines (STYLE_JAVA17.md §1). Component list follows §8 signature rules.
- [ ] `sealed` / `non-sealed` / `permits` — new `JavaModifierPriority` column;
      pin exact ordering against the real current priority list first
      (STYLE_JAVA17.md §2 — flagged open question, do not guess).
- [ ] Switch expressions (`->` form) — new alignment pass, distinct from STYLE.md
      §13's `:`-based switch statement handling (STYLE_JAVA17.md §3). Resolve the
      block-body-outlier open question before implementing the alignment group
      logic.
- [ ] Text blocks (`"""`) — tokenizer change only: recognize as one opaque
      multi-line token, contents never touched (STYLE_JAVA17.md §4). Verify
      current `TokenizerCore.java` doesn't already mis-tokenize these before
      assuming this is purely additive.
- [ ] `var` — confirm it needs zero code changes (already just another type-column
      token in the existing §5 grid) — likely a no-op verification item, not new
      code (STYLE_JAVA17.md §5).
- [ ] Pattern matching (`instanceof`, switch patterns, record deconstruction) —
      §3.1 complexity padding should already handle condition content; switch
      patterns reuse the new §3 arrow-form alignment (STYLE_JAVA17.md §6).

## Checklist — C++17/20/23

- [ ] Structured bindings — atomic name-cell in existing §5 grid, plus internal
      `[a, b, c]` spacing rule (STYLE_CPP20.md §1).
- [ ] Concepts / `requires` clauses — **blocked**, no worked examples agreed yet.
      Needs a design discussion before any checklist item is actionable
      (STYLE_CPP20.md §2).
- [ ] `consteval` / `constinit` — new `CppModifierPriority` column; check whether
      `constexpr` already has an entry before adding anything (STYLE_CPP20.md §3).
- [ ] Other candidates not yet scoped (`<=>`, coroutines, modules, init-statement
      `if`/`switch`) — no rules drafted, listed only so they aren't forgotten
      (STYLE_CPP20.md §4).

---

## End Goal (Phase 2)

- [ ] Dogfood test — formatter applied to a Java 17+ / C++20+ sample set
      exercising every construct above, verify style compliance
- [ ] Trim `AI_PREAMBLE.md` back to Tier-3-only content (function-call
      line-breaking, non-standard getter/setter naming — the genuinely
      AI-only judgment calls per `FORMATTER_DISCUSSION.md`'s "Future:
      AI-Assisted Formatting" section). Everything now resolved by the JAR
      (§7 nesting, §12 blank line, §13 inline alignment, §14 outlier exclusion,
      §15 capitalization, and this phase's Java17+/C++20+ additions) should be
      removed from `AI_PREAMBLE.md` since it is no longer ambiguous —
      it is documented, implemented behavior.
