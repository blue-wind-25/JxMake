# STATE_JXMAKE.md — JxMakeFile Formatter Tracker

Read `STATE_COMMON.md` first — shared commit/ambiguity/testing conventions
this file assumes; no other job's `STATE_*.md` is required. Dogfood corpus
status: real `.jxm` library files under
`/home/aloysius/Projects/JxMake/src/0-JxMake/lib/*.jxm` (JxMake's own
project) — round1/round2 idempotent across the whole corpus, syntax-checked
via `dist_build/jxmake --__compile__` (the 5 files that fail also fail
identically on the unformatted originals — pre-existing standalone-compile
limitations unrelated to this formatter). **Extended 2026-08-17**: also
covers `test/*.jxm`, `test/src/*/JxMakeFile`,
`test/src/cpp_atmega/JxMake-{Extra,Console}.jxm`,
`util/STM32Spec/*.jxm`, and two hardware-project `JxMakeFile`s (80 files) —
see the Checklist entry below for the full breakdown, including which
`JxMakeScriptEditor_Test_*`/`JxMakeTokenMaker_Test`/`SHFTest`/`DeprTest`
files are confirmed pre-existing/intentional non-clean cases vs. normal
testable files.

---

## Purpose

Tracks JxMakeFile (`--lang jxmake`): JxMake's own build-scripting language,
detected from the literal basename `JxMakeFile` or the `.jxm` extension.
Narrow, beautification-only scope per `STYLE_JXMAKE.md` — not a full-language
pipeline, modeled on the E-INI/Makefile/Bash/PowerShell "tooling" family
shape but requiring a real character-level tokenizer (raw/single/double/
multiline strings, non-nesting block comments, shell-exec lines) rather than
E-INI's simpler per-line quote scanner. **Fully implemented**, all 4
STYLE_JXMAKE.md rules landed, `make test` green.

Canonical language order (`CLAUDE.md`, "Canonical language order") places
`jxmake` between `eini` and `makefile`; `README.md`/`../README.txt`/
`CLAUDE.md` all list it as JAR-implemented. `Lang.SCAFFOLD_ONLY_LANGUAGES`
stays empty — `jxmake` was added directly to `Lang.SUPPORTED_LANGUAGES`.

---

## Project Layout

- `src/com/jxmake/formatter/FormatterJxMake.java` — dispatch class, mirrors
  `FormatterEini.java`. Constructs `JxMakeSpecificRule` from `Config`
  accessors, calls `.format(content)`; returns content unchanged when
  `formatOff` is true.
- `src/com/jxmake/formatter/rules/JxMakeSpecificRule.java` — all rule logic.
  - Char-level scanning helpers (`scanForComment`, `maskStringsOnly`,
    `findAssignOp`, `firstToken`, `firstNonWs`) classify raw strings
    (`` `...` ``), single-quoted (`\\`/`\'` escapes only), double-quoted
    (full escape set), multiline strings (`[[" ... "]]`, opening/closing
    tokens alone on their own line, interior 100% verbatim, carried via
    `inMlString` state across the main loop), non-nesting block comments
    (`(* ... *)`, carried via `inBlockComment`/`blockCommentDelta`), and
    `@`/`-@`/`+@`/`-+@`/`?@` shell-exec lines (rest of line opaque up to an
    unquoted `#`).
  - Block-keyword depth tracking (`OPENERS`/`CLOSERS`/`ELIF_ELSE` sets) plus
    `isOneLinerIf` (scans the logical line, joined across `\`
    continuations, for a top-level `:` outside strings/comments, stopping at
    the first top-level `;` and excluding `:` immediately followed by
    `=`/`+`/`?` so a second inlined statement's `:=`/`:+=`/`:?=` assign-op
    colon is never mistaken for the one-liner marker — see RDD_KEY_308).
  - `if`/`elif`/`else` keyword right-alignment (STYLE_JXMAKE.md §2) is a
    second pass layered on the main loop via a `Deque<IfChain>` stack —
    `IfChain` accumulates `{outIndex, keywordLen, baseIndentLen}` per
    branch and an `hasInlineBody`-derived `uniformOneLiner` flag; padding is
    only applied (`applyIfChainAlignment`) at the matching `endif` if every
    branch in the chain inlined its body via `;` (see RDD_KEY_309).
  - Rule 1 (comment normalization) reuses `ToolingCommentNormalizer` for
    `#` line comments (chain-grouped, standalone comment lines keep their
    original leading whitespace — not reindented); block comments shift as
    a unit by the opening `(*` line's indent delta, interior untouched.
  - Rule 3 (continuation alignment) tracks `pendingContinuation`/
    `contIsAssignment`/`contValueCol`/`contFallbackDepth` across loop
    iterations; assignment continuations align under the value's start
    column, everything else falls back to `(depth+1)*indent-size`.
  - Rule 4 (assignment alignment) uses a field-table approach, not a
    single-column approach: `parsePrefixFields` splits the collapsed
    `[local] [const] var-name` (or indirect `^var-name`) prefix into
    `localTok`/`constTok`/`varTok`, and `flushGroup` pads each field
    independently to the group's widest occurrence of that field (a
    zero-width column, e.g. no `local` anywhere in the group, is omitted
    entirely rather than emitting a spurious blank field) — see
    RDD_KEY_307.
  - `repeatChar`/`indent`/`endsWithContinuation`/`stripContinuation`
    delegate to the existing `rules/ToolingSharedRule.java` (no
    JxMake-specific change needed there).
- `test/jxmake_combined_{inp,out}.jxm` — combined fixture covering all 4
  rules: chain-grouped `#` comments, a shifted `(* ... *)` block comment, a
  one-liner `if ... : stmt`, an ordinary block-form `if`/`elif`/`else`/
  `endif` chain (left at plain depth indent — not uniformly one-liner) and
  a second uniform-one-liner chain (right-aligned), a `;`-multi-statement
  line, `@`/`-@` shell-exec lines, backslash continuation (both assignment
  and non-assignment forms), a `[[" ... "]]` multiline string, and rule-4
  field-table alignment across `local`/`const`/plain/indirect assignment
  groups. Registered in `Makefile`'s `INP_FILES` (immediately before
  `makefile_combined_inp.mk`) and `test/README.txt`.
- `test/real_code_regressions_215_{inp,out}.jxm` — the `XMLFrame.jxm`
  `isOneLinerIf`/`;`-inlined-branch bug fix (RDD_KEY_308), doubling as a
  real-corpus proof of the keyword right-alignment sub-rule (RDD_KEY_309).
  Registered in `Makefile`'s `INP_FILES` and `test/README.txt`.

---

## Config

No JxMake-specific keys — all 4 rules are unconditional (mirrors the rest
of the tooling family's "no gate" precedent). Comment normalization reuses
the existing global `normalize-comment-start-case`/
`normalize-comment-end-period`/`normalize-comment-start-case-multiline`
keys already defined in `Config.java`. `indent-size` is reused for the
block-keyword-depth indent width.

---

## Resolved Design Decisions

Full text lives in `RDD_LOG.md` (shared sequence across all jobs — see
`STATE_COMMON.md`'s lookup convention, `grep -Fm1`, no `-A`).

| Key | Topic |
|---|---|
| RDD_KEY_307 | Rule-4 (assignment-operator alignment) field-table implementation: `local`/`const`/var-name are three independently-aligned columns, each padded to the widest occurrence of that field in the group, not one combined key column. Verified against two of STYLE_JXMAKE.md's own worked examples by hand-deriving the padding formula; a third example's internally-inconsistent spacing (between its own two identical-prefix rows) was judged a hand-typed illustrative slip, not a literal byte-exact target. |
| RDD_KEY_308 | Real bug found via dogfooding `XMLFrame.jxm`: `isOneLinerIf` mistook a `;`-inlined branch's `:=` assign-op colon for the one-liner marker, collapsing a block `if`'s `elif`/`elif`/`else`/`endif` to column 0. Fixed by stopping the scan at the first top-level `;` and excluding `:` followed by `=`/`+`/`?`. |
| RDD_KEY_309 | `if`/`elif`/`else` keyword right-alignment sub-rule implementation: a `Deque<IfChain>`-based second pass, applied only when every branch in the chain inlines its body via `;` (`IfChain.uniformOneLiner`). Verified against both real-code reference shapes named in the correction (`XMLFrame.jxm`, `BasicPlatformUtil.jxm`). |
| RDD_KEY_310 | User-reported bug: a standalone `#` comment chain directly above a block-opening statement (e.g. `function`) kept its own stale original indent instead of following the block's new depth. Fixed: a chain now takes the depth of the next non-blank, non-comment code line (`nextCodeLineDepth` helper), falling back to the chain's own current-context depth when separated from that line by a blank line or at end of file/block. STYLE_JXMAKE.md rule 1 gained a new "Indentation of standalone `#` comment chains" paragraph; rule 2's preamble no longer blanket-excludes comment lines. |

---

## Tools/compiler used

Syntax-check command for this job (mirrors `STATE_TOOLING.md`'s own
"Tools/compiler used" section):

```
/home/aloysius/Projects/JxMake/dist_build/jxmake --__compile__ -f <filename>.jxm
```

**If `dist_build/` is missing in a future session, ask the user to rebuild
it — do not attempt to rebuild it yourself** (explicit user instruction).

---

## Open Questions

None. All syntax/rule details (including two mid-implementation
corrections to STYLE_JXMAKE.md — the rule-4 field-table alignment and the
`if`/`elif`/`else` keyword right-alignment sub-rule, later narrowed to the
uniform-one-liner-chain case) were resolved before/during implementation;
RDD_KEY_307/308/309 are implementation-mechanics judgment calls and a real
bug fix within an already-resolved spec, not genuine ambiguities requiring
a stop.

---

## Checklist

- [x] Wire `Lang.java` (`isJxMake`, `JxMakeFile`/`.jxm` inference,
      `SUPPORTED_LANGUAGES`, canonical order placement between `eini` and
      `makefile`) and `FormatterCore.forLanguage` dispatch.
- [x] Implement `FormatterJxMake.java` + `rules/JxMakeSpecificRule.java`
      (tokenizer + all 4 STYLE_JXMAKE.md rules); confirmed
      `Main.java`/`ServerMode.java` need no direct string-literal changes
      (both consume `Lang.SUPPORTED_LANGUAGES`/`Lang.isSupported`/
      `Lang.infer` generically) and `InFileConfig.java`'s `%JXM_CFMT_CFG`
      directive works for `.jxm` files (`#%` marker) with no additional
      wiring.
- [x] Author `jxmake_combined_{inp,out}.jxm`, verify against the live JAR
      (round1==round2 empty diff), register in `Makefile` `INP_FILES` and
      `test/README.txt`.
- [x] Real-code dogfood pass against `src/0-JxMake/lib/*.jxm`: round1/round2
      fully idempotent (0 diffs) across the whole corpus; syntax-checked via
      `dist_build/jxmake --__compile__` (5 pre-existing failures reproduce
      identically on the unformatted originals — unrelated).
- [x] Found and fixed one real bug during dogfood (RDD_KEY_308); added
      `real_code_regressions_215_{inp,out}.jxm`, registered in `Makefile`
      `INP_FILES` and `test/README.txt`.
- [x] Implemented `if`/`elif`/`else` keyword right-alignment sub-rule
      (RDD_KEY_309), added after initial rule-2 landing, later narrowed to
      the uniform-one-liner-chain case; re-verified all fixtures + dogfood
      corpus after each correction.
- [x] `make test` green: 326/326 forward + idempotency.
- [x] **2026-08-17 extended dogfood pass** — 80 more files across
      `test/*.jxm`, `test/src/*/JxMakeFile` (incl. real `extradep` cases),
      `test/src/cpp_atmega/JxMake-{Extra,Console}.jxm`,
      `util/STM32Spec/*.jxm`, two hardware-project `JxMakeFile`s. Round1/
      round2 `diff -rq` fully empty (idempotent, all 80). Syntax-checked via
      `dist_build/jxmake --__compile__ -f <abs-path>.jxm` (path must be
      absolute — resolved relative to the binary's own dir, not caller's
      cwd); original-vs-round1 output byte-identical on all 80 — zero
      formatter-induced regressions. Non-clean files all confirmed
      pre-existing/unrelated to formatting: `JxMakeScriptEditor_Test.jxm`/
      `_B`/`_B1` (X11-less sandbox GUI crash), `JxMakeTokenMaker_Test.jxm`
      (genuine pre-existing compile error, syntax-highlighter fixture),
      `SHFTest.jxm` (deliberate missing-include test), `DeprTest.jxm`
      (intentional deprecation-warning RC=0 case). The other 7
      `JxMakeScriptEditor_Test_*` variants compile cleanly before/after —
      normal testable files. No bug found; `make test` unaffected
      (326/326, no source changes).
- [x] Update docs: `README.md` (new "JxMakeFile" section, extension/basename
      mapping, `--lang`/`lang=` enumerations, Known Limitations entry),
      `../README.txt` (file list, JAR-implemented-languages list, "five
      tooling languages" wording), `CLAUDE.md` (job table row, canonical-
      order block, implementation-status paragraph). Confirmed
      `AI_PREAMBLE_FULL.md`/`AI_PREAMBLE_AESTHETIC.md` need no change (no
      language-enumeration lists there).
- [x] Add `RDD_KEY_307`/`RDD_KEY_308`/`RDD_KEY_309` to `RDD_LOG.md`, index
      them above; create this file.
- [x] **2026-08-17 (later) user-reported bug fix**: standalone `#` comment
      chain indentation (RDD_KEY_310, see index above). Added
      `test/real_code_regressions_216_{inp,out}.jxm`, updated
      `test/jxmake_combined_out.jxm` (its pre-existing hand-indented comment
      pair now correctly renders at depth 0, a genuine expected-output
      change), registered both in `Makefile`/`test/README.txt`. `make test`
      327/327 forward + idempotency. Re-ran round1/round2 idempotency across
      the full 130-file previously-dogfooded corpus — empty diff, confirming
      no bad interaction with block-comment shifting or comment-chain
      grouping elsewhere.
