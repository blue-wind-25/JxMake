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
**Fully implemented**, all 5 §4 rules landed, `make test` green.

Canonical language order (`CLAUDE.md`, "Canonical language order") places
`eini` between `python3` and `makefile`; `README.md`/`../README.txt`/
`CLAUDE.md` all list it as JAR-implemented. `Lang.SCAFFOLD_ONLY_LANGUAGES`
stays empty — `eini` was added directly to `Lang.SUPPORTED_LANGUAGES`.

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
  per line — see RDD_KEY_303. `repeatChar` delegates to the existing
  `rules/ToolingSharedRule.java` (no E-INI-specific change needed there).
- `test/eini_combined_{inp,out}.ini` — combined fixture covering all 4
  group-header marker styles (`[]`/`{}`/`<>`/`()`) plus bare/plain headers,
  quoted/unquoted keys+values, `=`/`:` separators, all 4 comment markers +
  triple-quote, a `\`-continuation, and separator alignment. Registered in
  `Makefile`'s `INP_FILES` (immediately before `makefile_combined_inp.mk`)
  and `test/README.txt`. No dogfood/external corpus — E-INI is a
  codebase-local format with no widely-used real-world referent, so local
  fixture coverage (plus round1/round2 idempotency) is the accepted bar,
  same as the rest of the tooling family absent a corpus. No external
  tool/validator applies for the same reason.

---

## Config

No E-INI-specific keys — all 5 rules are unconditional (mirrors the rest of
the tooling family's "no gate" precedent). Comment normalization reuses the
existing global `normalize-comment-start-case`/`normalize-comment-end-period`
keys already defined in `Config.java`.

---

## Resolved Design Decisions

Full text lives in `RDD_LOG.md` (shared sequence across all jobs — see
`STATE_COMMON.md`'s lookup convention, `grep -Fm1`, no `-A`).

| Key | Topic |
|---|---|
| RDD_KEY_303 | E-INI implementation, four minor non-blocking judgment calls within the already-resolved spec: (1) comment-only lines must chain-group via `ToolingCommentNormalizer.normalizeChain()`, not normalize per-line (a real bug — the copyright-header fixture's trailing period was wrongly stripped by naive per-line normalization — caught and fixed during smoke-testing); (2) indentation snapping (rule 2) applies per physical line independently, since E-INI has no nesting/depth concept to anchor a group-relative round against; (3) continuation-line column offset is `keyWidth + 3` (vs. Makefile's `width + 1`), since E-INI's separator is always exactly one character; (4) group-header bracket-vs-bare detection checks the four wrapper pairs first, falling through to the bare-word-sequence path only if none match. |

---

## Open Questions

None. All syntax/rule details were resolved before implementation started;
RDD_KEY_303's four items are implementation-mechanics judgment calls within
that already-resolved spec, not genuine ambiguities requiring a stop.

---

## Checklist

- [x] Wire `Lang.java` (`isEini`, `.ini` extension inference,
      `SUPPORTED_LANGUAGES`, canonical order placement) and
      `FormatterCore.forLanguage` dispatch.
- [x] Implement `FormatterEini.java` + `rules/EiniSpecificRule.java` (all 5
      `STYLE_TOOLING.md` §4 rules); confirmed `Main.java`/`ServerMode.java`
      need no direct string-literal changes (both consume
      `Lang.SUPPORTED_LANGUAGES`/`Lang.isSupported`/`Lang.infer`
      generically) and `InFileConfig.java`'s `%JXM_CFMT_CFG` directive works
      for `.ini` files with no additional wiring.
- [x] Fix chain-grouping comment-normalization bug found via smoke-testing
      (RDD_KEY_303).
- [x] Author `eini_combined_{inp,out}.ini`, verify against the live JAR
      (round1==round2 empty diff), register in `Makefile` `INP_FILES` and
      `test/README.txt`.
- [x] Update docs: `README.md` (new "E-INI (Extended INI)" section,
      extension mapping, `--lang`/`lang=` enumerations, config table),
      `../README.txt`, `../STYLE_TOOLING.md` (title/intro count, §0 comment
      marker list + `%`-reservation note, new §4), `CLAUDE.md` (job table
      row, canonical-order block, implementation-status paragraph).
      Confirmed `AI_PREAMBLE_FULL.md`/`AI_PREAMBLE_AESTHETIC.md` need no
      change (no language-enumeration lists there).
- [x] Add `RDD_KEY_303` to `RDD_LOG.md`, index it above; create this file.
- [ ] (Future, not blocking) Source a dogfood corpus if a real-world E-INI-
      shaped format surfaces; register in `STATE_DOGFOOD.md` when it does.
