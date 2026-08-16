# STATE_EINI.md — E-INI (Extended INI) Formatter Tracker

Read `STATE_COMMON.md` first — shared commit/ambiguity/testing conventions
this file assumes; no other job's `STATE_*.md` is required. Dogfood corpus
status: see `STATE_DOGFOOD.md` (not yet registered — no dogfood run started).

---

## Purpose

Tracks E-INI (`--lang eini`, Extended INI): a simple INI-like key-value
config format with grouping. Narrow, beautification-only scope per
`STYLE_TOOLING.md` §4, modeled directly on the Makefile/Bash/PowerShell
"tooling" family (`STATE_TOOLING.md`) — not a full-language pipeline.

**Implemented:** `Lang.isEini`, `.ini` extension inference (`Lang.infer`),
`FormatterCore.forLanguage` dispatch, `FormatterEini` (standalone,
`FormatterMakefile`-style), `EiniSpecificRule` (all 5 `STYLE_TOOLING.md` §4
rules: separator alignment, indentation snapping, line-continuation
alignment, comment normalization, no long-line breaking).
`Lang.SCAFFOLD_ONLY_LANGUAGES` stays empty — `eini` was added directly to
`Lang.SUPPORTED_LANGUAGES`.

**Canonical language order** recorded in `CLAUDE.md` (search "Canonical
language order") — `eini` sits between `python3` and `makefile`.
`README.md`, `../README.txt`, and `CLAUDE.md` all list it as
JAR-implemented.

---

## Project Layout

- `src/com/jxmake/formatter/FormatterEini.java` — dispatch class, mirrors
  `FormatterMakefile`. Constructs `EiniSpecificRule` from `Config`
  accessors, calls `.format(content)`; returns content unchanged when
  `formatOff` is true.
- `src/com/jxmake/formatter/rules/EiniSpecificRule.java` — all rule logic.
  Per-line scanner (`LineScan`/`scanLine`) tracks quote state to find the
  first unquoted key-value separator (`=`/`:`) and first unquoted comment
  marker (`#`/`;`/`@`/`//`/triple-same-quote). `collapseOutsideQuotes`
  handles key/header whitespace collapsing. Group-based rendering
  (`KvItem`/`flushGroup`) mirrors `MakefileSpecificRule`'s
  `AsgnItem`/assignment-alignment shape, adapted for E-INI's single-char
  separator (continuation column offset is `keyWidth + 3`, vs. Makefile's
  `width + 1`, since Makefile operators can be multi-char). Comment-only
  lines are chain-grouped (contiguous runs) via
  `ToolingCommentNormalizer.normalizeChain`, not normalized independently
  per line — see RDD_KEY_303.
- `rules/ToolingSharedRule.java` — shared clamping `repeatChar`/`indent`
  helpers, reused as-is (no E-INI-specific change needed).
- `test/eini_combined_{inp,out}.ini` — combined fixture covering all 4
  group-header marker styles (`[]`/`{}`/`<>`/`()`) plus bare/plain headers,
  quoted/unquoted keys+values, `=`/`:` separators, all 4 comment markers +
  triple-quote, a `\`-continuation, and separator alignment. Registered in
  `Makefile`'s `INP_FILES` (immediately before the `makefile_combined_inp.mk`
  line) and `test/README.txt`.

---

## Resolved Design Decisions

Full text lives in `RDD_LOG.md` (shared sequence across all jobs — see
`STATE_COMMON.md`'s lookup convention, `grep -Fm1`, no `-A`).

| Key | Topic |
|---|---|
| RDD_KEY_303 | E-INI implementation, four minor non-blocking judgment calls within the already-resolved spec: (1) comment-only lines must chain-group via `ToolingCommentNormalizer.normalizeChain()`, not normalize per-line (a real bug — the copyright-header fixture's trailing period was wrongly stripped by naive per-line normalization — caught and fixed during smoke-testing); (2) indentation snapping (rule 2) applies per physical line independently, since E-INI has no nesting/depth concept to anchor a group-relative round against; (3) continuation-line column offset is `keyWidth + 3` (vs. Makefile's `width + 1`), since E-INI's separator is always exactly one character; (4) group-header bracket-vs-bare detection checks the four wrapper pairs first, falling through to the bare-word-sequence path only if none match. |

---

## Config

No E-INI-specific config keys — the 5 fixed-rule-list transforms are
unconditional (mirrors the "no gate" precedent set by the rest of the
tooling family). Comment normalization reuses the existing global
`normalize-comment-start-case`/`normalize-comment-end-period` keys already
defined in `Config.java` — no new keys added.

## Test Fixtures (Local)

| Pair | Covers |
|---|---|
| `eini_combined_{inp,out}.ini` | STYLE_TOOLING.md §4.1–§4.5 + all 4 group-header marker styles + bare header + quoted key/value + all comment markers + `\`-continuation |

Registered active in `Makefile` `INP_FILES` (immediately before the
Makefile fixture block) and documented in `test/README.txt`.

## Test Fixtures (External, corpus-scale)

None yet — no dogfood corpus identified/run for E-INI (it's a codebase-local
format, not modeled on any widely-used real-world file type the way
Makefile/Bash/PowerShell are). Revisit if a real-world `.ini`-adjacent
corpus with this exact grammar surfaces; otherwise local fixture coverage
is considered sufficient given the narrow, fixed 5-rule scope.

## Tools/compiler used

None — no external validator applies (E-INI is not a real external format
with its own parser/linter to check against). Correctness relies on the
local fixture pair plus round1/round2 idempotency, same bar as the rest of
the tooling family absent a dogfood corpus.

---

## Open Questions

None. All syntax/rule details were fully resolved before implementation
started (see the task's original resolved spec); the four items in
RDD_KEY_303 above are implementation-mechanics judgment calls within that
already-resolved spec, not genuine ambiguities requiring a stop.

---

## Checklist

- [x] Wire `Lang.java`: `isEini` flag, `.ini` extension inference,
      `isSupported`, `SUPPORTED_LANGUAGES`, canonical order placement
      (between `python3` and `makefile`).
- [x] Wire `FormatterCore.forLanguage` dispatch.
- [x] Implement `FormatterEini.java` + `rules/EiniSpecificRule.java`
      (all 5 `STYLE_TOOLING.md` §4 rules).
- [x] Confirm `InFileConfig.java`'s `%JXM_CFMT_CFG` directive works for
      `.ini` files with no additional wiring — the directive regex already
      generically supports a `#`-prefixed form usable by any language.
- [x] Confirm `Main.java`/`ServerMode.java` need no direct string-literal
      changes — both consume `Lang.SUPPORTED_LANGUAGES`/`Lang.isSupported`/
      `Lang.infer` generically.
- [x] Fix chain-grouping comment-normalization bug (RDD_KEY_303) found via
      smoke-testing the copyright-header fixture.
- [x] Author local test fixture pair `eini_combined_{inp,out}.ini`,
      register in `Makefile` `INP_FILES` and `test/README.txt`. Verified
      against the live JAR (round1==round2 empty diff) before registering.
- [x] Update `README.md` (user-facing — new "E-INI (Extended INI)" section,
      extension mapping, `--lang`/`lang=` enumerations, config table,
      Known Limitations subsection — none currently known beyond the fixed
      5-rule scope).
- [x] Update `../README.txt` (JAR-implemented-languages list, tooling-file
      listing line).
- [x] Update `../STYLE_TOOLING.md` (title/intro language count, §0 comment
      marker list + `%`-reservation note, new §4 E-INI section with
      before/after code-fence examples for all 5 rules).
- [x] Update `CLAUDE.md` (job-routing table row, canonical-order code
      block, "Current implementation status" paragraph naming
      `FormatterEini.java`/`EiniSpecificRule.java`).
- [x] Add `RDD_KEY_303` to `RDD_LOG.md`, index it here.
- [x] Create this file (`STATE_EINI.md`).
- [x] Confirmed `AI_PREAMBLE_FULL.md`/`AI_PREAMBLE_AESTHETIC.md` contain no
      language-enumeration lists requiring an E-INI addition.
- [ ] (Future, not blocking) Source a dogfood corpus if a real-world E-INI-
      shaped format surfaces; register in `STATE_DOGFOOD.md` when it does.
