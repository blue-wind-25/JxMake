# STATE_NEXT.md — Phase 2 Tracker (Java 17+ / C++20+ Constructs)

> **Active (RDD_KEY_82 in `STATE_rdd_log.md`).** Phase ordering was reversed: this file's
> Java 17+/C++20+ checklists are now implemented *before* `STATE.md`'s original End Goal
> (`Main.java`, `README.md`, the Tier 1/Tier 2 self-dogfood test), which has moved here —
> see "End Goal (Phase 1)" below, placed just before "End Goal (Phase 2)". Rationale: this
> file's own "Hard constraint" below already means Phase 2 work lands as new branches
> inside the same already-COMPLETE rule classes, so dogfooding once after both phases land
> avoids re-validating the same dogfood pass twice. `STATE_NEXT_EXT.md` remains gated until
> this file's own End Goal (Phase 2) milestone (the last item below) is checked off.

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
| `JavaSpecificRule.java` (record, switch expressions, text blocks, var, pattern matching) | NOT STARTED |
| `CppModifierPriority.java` (consteval/constinit addition) | NOT STARTED |
| `CppSpecificRule.java` (structured bindings) | NOT STARTED |
| `CppSpecificRule.java` (concepts/requires) | NOT STARTED |

---

## Checklist — Java 17+

- [ ] `record` — treat as `class` for brace style, closing comment, forced blank
      lines (STYLE_JAVA17.md §1). Component list follows §8 signature rules.
- [x] `sealed` / `non-sealed` / `permits` — new `JavaModifierPriority` column;
      order is `abstract → sealed → non-sealed → final` (resolved — see
      STYLE_JAVA17.md §2 resolved decisions table). Column order required a
      stop-and-ask against the Hard Constraint (the map is flat, no per-kind
      context) -- resolved, see RDD_KEY_1. `permits` clause line-breaking
      implemented as `JavaSpecificRule.enforcePermitsClauseLineBreaking`.
- [ ] Switch expressions (`->` form) — new alignment pass, distinct from STYLE.md
      §13's `:`-based switch statement handling (STYLE_JAVA17.md §3). Block-body
      case breaks entire group's `->` alignment — all-or-nothing, no outlier
      exclusion (resolved — see STYLE_JAVA17.md §7 resolved decisions table).
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

- [ ] `auto` keyword
- [ ] Structured bindings — atomic name-cell in existing §5 grid, plus internal
      `[a, b, c]` spacing rule (STYLE_CPP20.md §1).
- [ ] Concepts / `requires` clauses — K&R brace style; `requires` trails `)`
      always, wraps only past 100 chars; nested compound requirements untouched
      (resolved — see STYLE_CPP20.md §2 and §5 resolved decisions table).
- [ ] `consteval` / `constinit` — new `CppModifierPriority` columns, order
      `constexpr → consteval → constinit`; verify `constexpr` already present
      before adding (resolved — see STYLE_CPP20.md §3 and §5).
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

---

## End Goal (Phase 1)

Moved here from `STATE.md` (RDD_KEY_82) — done *after* the Java 17+/C++20+ checklists
above, so the dogfood pass below covers both phases at once.

### File Status

| File | Status |
|---|---|
| `Main.java` | NOT STARTED |
| `README.md` (for both phase 1 and phase 2; defer until just before Dogfood) | NOT STARTED |

**`Main.java` note:** owns the temp-file cache layer for `IndentationDetector.detect()` in
standalone mode -- key = SHA hash of boundary dir absolute path string, stored as
`/tmp/style-fmt-indent-<hash>.cache`, content = detected style + `\n` + boundary dir
`lastModified` epoch ms. On read: if the file exists and its stored `lastModified` matches
current `Files.getLastModifiedTime(boundaryDir)`, return the cached style; otherwise delete
and rescan. `IndentationDetector` itself is unaware of this -- `Main` calls `detect()` with
a pre-populated single-entry map on a temp-cache hit, bypassing the scan entirely.

### Checklist

- [ ] Dogfood test — run formatter on its own `src/` tree, verify style compliance and that
      `make` still succeeds after

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

---

## After Phase 2

Once End Goal (Phase 2) above is checked off, continue with
[`STATE_NEXT_EXT.md`](STATE_NEXT_EXT.md) for:
- Phase 3 — JAR `ai-assist` integration (local on-device AI for Tier-3 judgment calls)
- Post-phase-3 cleanup — `JXMAKE_` / `jxmake_` prefix rename for all env vars and
  config keys

**Do not read `STATE_NEXT_EXT.md` until End Goal (Phase 2) above is checked off.**
