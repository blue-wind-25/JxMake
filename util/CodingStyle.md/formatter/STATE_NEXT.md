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
| `JavaSpecificRule.java` (record) | COMPLETE (see RDD_KEY_2; `TokenizerCore.java` and `BlockStructureRule.java` also touched) |
| `JavaSpecificRule.java` (switch expressions) | COMPLETE (new `enforceSwitchExpressionArrowAlignment`, wired into `Formatter.java`; no RDD needed -- STYLE_JAVA17.md §7 already pre-resolved the only design question, the block-body all-or-nothing bail-out) |
| `TokenizerCore.java` (text blocks) | COMPLETE (new `isTextBlockOpener`/`emitTextBlock`, opaque `STRING` token spanning the whole block, mirrors `emitBlockComment`'s internal-newline pattern; no RDD needed) |
| `DeclarationAlignmentRule.java` (`var`) | COMPLETE (added `"var"` to `TYPE_KEYWORDS_JAVA`; confirmed-not-no-op, see below) |
| `JavaSpecificRule.java` (pattern matching) | NOT STARTED |
| `CppModifierPriority.java` (consteval/constinit addition) | NOT STARTED |
| `CppSpecificRule.java` (structured bindings) | NOT STARTED |
| `CppSpecificRule.java` (concepts/requires) | NOT STARTED |

---

## Checklist — Java 17+

- [x] `record` — treat as `class` for brace style, closing comment, forced blank
      lines (STYLE_JAVA17.md §1). Component list follows §8 signature rules.
      `TokenizerCore` named-construct stamping extended to see through a record's
      `(...)` component list and an optional trailing `implements` clause;
      `BlockStructureRule`/`JavaSpecificRule` also touched for the same shapes plus
      the compact canonical constructor -- see RDD_KEY_2.
- [x] `sealed` / `non-sealed` / `permits` — new `JavaModifierPriority` column;
      order is `abstract → sealed → non-sealed → final` (resolved — see
      STYLE_JAVA17.md §2 resolved decisions table). Column order required a
      stop-and-ask against the Hard Constraint (the map is flat, no per-kind
      context) -- resolved, see RDD_KEY_1. `permits` clause line-breaking
      implemented as `JavaSpecificRule.enforcePermitsClauseLineBreaking`.
- [x] Switch expressions (`->` form) — new alignment pass, distinct from STYLE.md
      §13's `:`-based switch statement handling (STYLE_JAVA17.md §3). Block-body
      case breaks entire group's `->` alignment — all-or-nothing, no outlier
      exclusion (resolved — see STYLE_JAVA17.md §7 resolved decisions table).
      Implemented as `JavaSpecificRule.enforceSwitchExpressionArrowAlignment`:
      independent switch/case discovery (own helper methods, does not touch or
      reuse `SwitchRule`'s private colon-form machinery) that bails (leaves the
      switch untouched) the instant a label's terminator is `:` instead of `->`,
      so arrow-form and colon-form switches are never confused. Only the label
      span up to and including `-> ` is ever rewritten; body content (including
      a block body's own K&R brace, already handled by
      `BlockStructureRule.isLambdaBrace`'s existing `->`-preceded-brace branch)
      is untouched. Verified: STYLE_JAVA17.md's worked examples (basic alignment,
      block-body bail-out, pattern-matching/record-deconstruction/`default`
      alignment), colon-form switches confirmed byte-for-byte unaffected via a
      pristine-baseline diff, idempotency, and a nested switch-expression-inside-
      a-block-body case.
- [x] Text blocks (`"""`) — tokenizer change only: recognize as one opaque
      multi-line token, contents never touched (STYLE_JAVA17.md §4). NOT a no-op:
      the pre-existing single-line `emitString()` would mis-lex the opening/closing
      `"""` (empty-string token + stray quote), exposing the block's multi-line
      content -- braces, indentation -- to every downstream rule. Fixed with new
      `TokenizerCore.isTextBlockOpener()`/`emitTextBlock()`, reusing `TokenType.STRING`
      and mirroring `emitBlockComment()`'s "one token, internal newlines embedded in
      `.text`" pattern, which is exactly what makes "preserved exactly as written"
      fall out for free (NEWLINE-scanning passes never see inside; `MiscRule`'s
      indentation conversion never reaches in). Verified: STYLE_JAVA17.md worked
      example (content + indentation preserved exactly), idempotency, no
      closing-comment/blank-line leak into block content, statements after the
      block survive, braces inside content untouched, escaped `\"""` inside content
      doesn't break delimiter matching, and a pristine-baseline diff confirming zero
      impact on ordinary string/char literal tokenization.
- [x] `var` — confirmed NOT a no-op: `DeclarationAlignmentRule.TYPE_KEYWORDS_JAVA`
      lacked `"var"` even though `TokenizerCore.KEYWORDS_JAVA` already had it as a
      full keyword (from the sealed/permits work), so `parseDeclaration` hit its
      `KEYWORD not in typeKeywords -> return null` branch and silently excluded every
      `var`-declared local/field from §5 grid alignment. Confirmed via harness: a
      `var`/`int`/`String` group rendered with `var` left at a single space while
      `int`/`String` aligned together, pre-fix. Fixed by adding `"var"` to
      `TYPE_KEYWORDS_JAVA` (mirrors `TYPE_KEYWORDS_CPP` already including `"auto"`
      for the same reason). `GetterSetterRule.java` checked -- no analogous
      type-keyword set there, so no parallel change needed. Verified post-fix: the
      same group now aligns `var`/`int`/`String` together in one grid (STYLE_JAVA17.md
      §5), and the switch-expression and text-block harnesses still pass with zero
      regressions.
- [ ] Pattern matching (`instanceof`, switch patterns, record deconstruction) —
      §3.1 complexity padding should already handle condition content; switch
      patterns reuse the new §3 arrow-form alignment (STYLE_JAVA17.md §6).

## Checklist — C++17/20/23

- [ ] `auto` (as data type), `consteval` / `constinit` — new `CppModifierPriority`
      columns, order `constexpr → consteval → constinit`; verify `constexpr`
      already present before adding (resolved — see STYLE_CPP20.md §3 and §5).
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
