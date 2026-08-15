# STATE_CPP26.md — C++26 JAR Support Tracker

Read `STATE_COMMON.md` first — shared commit/ambiguity/testing conventions
this file assumes. `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` not required.
Dogfood corpus status: see `STATE_DOGFOOD.md`.

---

## Purpose

Tracks C++26 support in the deterministic JAR formatter
(`util/CodingStyle.md/formatter/`), per `STYLE_CPP26.md` (builds on
`STYLE.md`, `STYLE_C_CPP.md`, `STYLE_CPP20.md`).

**Toolchain note (found during the `glaze` session):** this system also has
a modern `clang++ 22.1.8` at
`~/xsdk/clang22/LLVM-22.1.8-Linux-X64/bin/clang++`, capable of real
`-std=c++23 -fsyntax-only` compile validation including reflection syntax —
requires `-stdlib=libc++` for standard headers to resolve; pipe stderr
through `grep -v 'no version information available'` (harmless
`libstdc++.so.6` symbol-versioning warning). `/opt/glibc-2.41/` is available
for a glibc-mismatch/patchelf issue with some other prebuilt binary.
Supersedes the older `g++ 4.8.5`/`clang++ 3.7.1` toolchain behind
"Compilation not attempted" in earlier checklist entries — prefer
`clang++ 22.1.8` for any future compile-check step over falling back to
idempotency-only validation.

---

## Scope

`STYLE_CPP26.md` covers finalized C++26 constructs only (shipped 28 March
2026; no C++29 content). Extends existing C/C++ support (`STYLE_C_CPP.md`,
frozen C++17/20/23 baseline in `STYLE_CPP20.md`) with:

1. Pack indexing (`T...[i]`) — falls under existing array-index bracket
   rules, no new padding logic.
2. `= delete("reason")` — trivial, ordinary function-call-argument spacing.
3. Placeholder `_` — ordinary identifier, no new rule.
4. Contracts (`pre`/`post`/`contract_assert`) — comparable to existing
   trailing-`requires`-clause handling; each clause own line, overflow-
   triggered wrap like `requires`.
5. Reflection (`^^`, `[:`, `:]`) — **STALE, 2026-08-10: already implemented**
   (tokenizer support, tight/loose padding, `enforceReflectionOperatorSpacing`
   — see Checklist items under "Tokenizer support pass for §5 Reflection" /
   "§5 tight/loose padding rules implemented" below). This "provisional/draft"
   label predates that work landing; do not re-scope as a TODO without
   re-checking the Checklist first.
6. Config — no new config keys for §1–4; §5 deliberately has none yet
   since its rules aren't trusted. See "Config" below.
7. Test fixtures — authored and registered in `formatter/test/`. See "Test
   Fixtures (Local)" below.

**C++26 is NOT a separately selectable language.** No `Lang.isCpp26` flag,
no `--lang cpp26`/`lang=cpp26` selector, no `SCAFFOLD_ONLY_LANGUAGES`
entry — `.cpp`/`.hpp` files resolve to the existing `"cpp"` pipeline as
always. C++26 rule coverage lands directly in that pipeline's existing
rule classes (primarily `CppSpecificRule.java`), same as C++20 was folded
in additively with no separate `isCpp20`/`--lang cpp20` selector
(`STYLE_CPP20.md` extends `STYLE_C_CPP.md` in place). See RDD_KEY_180.

No `src/` files yet specific to this job — coverage (extending
`CppSpecificRule.java` or similar) doesn't exist yet; when it lands it's
gated on `isCpp`, not a new flag.

---

## Resolved Design Decisions

Full text lives in `RDD_LOG.md` (shared with `STATE_C_CPP_JAVA.md`/
`STATE_KOTLIN.md` — continue existing `RDD_KEY_n` numbering, don't
restart). See `STATE_COMMON.md`'s lookup convention (`grep -Fm1`, no `-A`).

| Key | Topic |
|---|---|
| RDD_KEY_179 | (**REVERSED by RDD_KEY_180** — no longer in effect) Language-selection mechanism — C++26 was made explicit-only via `--lang cpp26` / `lang=cpp26` |
| RDD_KEY_180 | **REVERSES RDD_KEY_179** — C++26 is not a separate selectable language; it is future incremental rule coverage on the existing `"cpp"` pipeline, same pattern as C++20 |
| RDD_KEY_181 | §1 pack indexing — call-containing index (`T...[ computeIndex() ]`) is loose per the ordinary `[]` tight/loose rule; fixed stale tight example |
| RDD_KEY_285 | `glaze` dogfood `json_patch_test.cpp` round1/round2 idempotency fix — `ScopePipelineCurly.applyOversizedAggregateInitClosingBracePass`'s newline-detection scoped to `lang.isCpp`-only paren/bracket depth 0, mirroring `ScopePipelineCore.hasTopLevelNewline` |

---

## Config

No new config keys. §1–4 reuse existing STYLE.md/STYLE_CPP20.md logic,
nothing toggle-able. §5 (Reflection) still provisional pending the
tokenizer validation pass — premature to define config for an untrusted
rule set. Revisit once §5 graduates out of draft status.

## Test Fixtures (External, corpus-scale)

For §5 Reflection tokenizer/rule validation (see Scope §5 and Checklist
below), real-code testing against:
- `bloomberg/clang-p2996` — checked in an earlier session: repo is
  empty/unusable, no further attempts needed
- `wrocpp/cpp26-reflection-examples` — DONE (see Checklist)
- `simdjson/experimental_json_builder` — DONE (see Checklist; one real bug
  found and fixed, `real_code_regressions_76`)
- `stephenberry/glaze` — DONE (see Checklist; zero bugs found within this
  job's scope, several out-of-scope C/C++/Java-job findings documented)
- `ryanjk5.github.io/posts/rjk-duck` (blog post, not a repo) — DONE (see
  Checklist; useful extra source, zero bugs within this job's scope)

All four named candidates above are complete. No further named candidates
remain queued — a future session sourcing more §5 real-code coverage would
need a new candidate first.

## Test Fixtures (Local)

Local dogfood pairs (distinct from the external-repo corpus-scale list
above) authored and registered in `formatter/test/` — see
`test/README.txt` for the pair list and coverage. The reflection pair
(`cpp26_reflection_inp/out.cpp`) was extracted ahead of its original
promotion gate (§5's external-repo tokenizer validation still pending),
per explicit instruction — see "Done:" note on the checklist below.

**Promoted to active `make test`** (Makefile `INP_FILES`), after §5
padding rules landed and the fixture was run against the real JAR to
verify it (every `^^`/`[[ ]]`/`[: :]` diff matched expected output with
zero changes needed). Two fixture-authoring issues surfaced and were
fixed with explicit user confirmation, both unrelated to the new §5 rules:
formatter doesn't reindent (preserves source indentation verbatim, see
`STATE_COMMON.md`'s Architectural TODOs), so the input's body statements
needed correct indentation already added (same convention as
`cpp26_core_inp.cpp`); and `auto v = [:r:];` gets column-aligned under
`constexpr auto r = ^^int;`'s `=` by the pre-existing (unrelated,
C/C++/Java job's) declaration-alignment rule — confirmed correct,
pre-existing behavior with a plain non-reflection repro, expected output
updated to match.

**STALE, superseded — do not re-flag.** This paragraph is a leftover from
before the corpus-scale pass ran; see "Test Fixtures (External,
corpus-scale)" above — all four named candidates (`bloomberg/clang-p2996`
found empty/unusable, `wrocpp/cpp26-reflection-examples`,
`simdjson/experimental_json_builder`, `stephenberry/glaze`,
`ryanjk5.github.io/posts/rjk-duck`) are DONE and that section's own text
says so explicitly. Not an open item; a future session sourcing more §5
real-code coverage would need to name a brand-new candidate first, which
is a fresh future task, not this one.

## Tools/compiler used
(1) `g++ -std=c++20 -fsyntax-only <file>` — usually `/opt/gcc-12.2.0/bin/g++`; PEGTL,
    stdexec, and mp11 additionally need `LD_LIBRARY_PATH=/opt/isl-0.16.1/lib` with this
    toolchain
(2) `clang++ -std=c++23 -fsyntax-only <file>.cpp` (with/without `-stdlib=libc++`) at
    `~/xsdk/clang22/LLVM-22.1.8-Linux-X64/bin/clang++` — pipe stderr through
    `grep -v 'no version information available'` to filter a harmless libstdc++
    symbol-versioning warning (not a compile error); `/opt/glibc-2.41/` is available if a
    genuine glibc-mismatch/patchelf issue is ever hit with some other prebuilt binary

---

## Class Scoping (post Core/Curly/Indent/Tags refactor)

No new classes. C++26 rule coverage lands entirely inside existing
`isCpp`-gated curly-family classes: `TokenizerCurly.java`,
`FormatterCurly.java`, `ScopePipelineCurly.java`, `MiscRuleCurly.java`,
`CppSpecificRule.java`. "Tokenizer support pass" in the Checklist below
means editing `TokenizerCurly.java` specifically (not `TokenizerCore.java`,
which is family-generic, no C++-specific branching to extend).

## Open Questions

None recorded. **STALE, 2026-08-10**: this section (and Scope §5 above)
previously claimed §5 Reflection real implementation "hasn't started" —
false; see the "Tokenizer support pass for §5 Reflection" /
tight-loose-padding / `enforceReflectionOperatorSpacing` Checklist items
below (all landed). No open question remains for §5.

---

## Checklist

- [x] Diff `STYLE_CPP26.md` against `STYLE_CPP20.md`/`STYLE_C_CPP.md` to
      enumerate which rules are C++26-specific vs. already covered (§1–3
      look possibly already covered; §4/§5 look genuinely new — confirm by
      diffing, don't assume).

      **Confirmed 2026-08-03** (direct read-only diff of all three files,
      no edits): §1–3 are thin wrappers over pre-existing baseline rules
      (`STYLE.md` §3.1 bracket tight/loose, §5 declaration-alignment grid,
      ordinary call-argument spacing) — no overlap with
      `STYLE_CPP20.md`/`STYLE_C_CPP.md`. §4/§5 confirmed genuinely new
      (neither `pre`/`post`/`contract_assert` nor `^^`/`[:`/`:]` appear
      elsewhere; they only reuse other files' `requires`-wrap shape /
      bracket-complexity mechanism, not duplicated text). No stale
      section-numbering cross-refs found. No doc edits made.
- [x] **Language-selection mechanism resolved (RDD_KEY_180, reversing
      RDD_KEY_179)** — same resolution as in Scope above: no
      `Lang.isCpp26` flag/selector, `.cpp`/`.hpp` resolve to `"cpp"` as
      before, C++26 rules land inside the existing pipeline. (An earlier
      session had introduced `--lang cpp26` as an explicit-only selector
      under RDD_KEY_179 — reverted as an unnecessary departure from
      precedent.)
- [x] Tokenizer smoke check done (via sub-agent, read-only). Confirmed via
      `TokenizerCurly.java`'s `MULTI_CHAR_OPS` array (lines 114-120): `^^`,
      `[:`, `:]` were absent, mis-splitting into single chars (`^^` →
      `^`+`^`, `[:` → `[`+`:`, `:]` → `:`+`]`) — no crash, but a `/tmp` CLI
      snippet confirmed active **corruption** (`^^SomeType` →
      `^ ^ SomeType`; inconsistent interior spacing on `[: :]`), not inert
      passthrough. No secondary cascading corruption found.
- [x] Tokenizer support pass for §5 Reflection done: `TokenizerCurly.java`'s
      `MULTI_CHAR_OPS` array gained `"^^"`, `"[:"`, `":]"` entries (none a
      strict prefix of another, no new ordering constraint). `":]"` reaches
      `emitOperator()` via the existing dispatch default case; `"[:"`
      needed a new dispatch branch (`c == '[' && peek(1) == ':' &&
      lang.isCpp`) placed before the generic open-bracket branch (a leading
      `[` otherwise always goes to `emitOpenBracket` first) — gated on
      `lang.isCpp` since this branch affects every leading `[`, unlike the
      two `MULTI_CHAR_OPS`-only additions which are inert elsewhere.
      Verified against the same `/tmp` snippet: mis-split gone, consistent
      interior padding. `make test`: 102/102 forward + idempotency, zero
      regressions. Tokenizer support only — no tight/loose padding or
      `^^`-binds-tight rule yet (still provisional/draft per Scope §5) —
      separate, later step.
- [x] §5 tight/loose padding rules implemented, in `CppSpecificRule.java` +
      `FormatterCurly.java`'s Phase 4 block (`lang.isCpp`-gated). Correction
      to `STYLE_CPP26.md` itself: its claim that splice-bracket padding
      "mirrors the existing JAR-verified `[[ assume(a >= 0) ]]` case" was
      **false** — `[[ ]]` attributes had no tight/loose rule at all
      (confirmed with user via `AskUserQuestion`: earlier apparent padding
      was actually the unrelated generic paren-complexity rule firing on
      `assume(...)`'s own `(...)`). Per user decision, implemented both
      together:
      - `CppSpecificRule.enforceAttributeAndSpliceBracketPadding` — one
        shared implementation for both `[[ ]]` and `[: :]`: forward-scans
        matched OP-token pairs (stack keyed on exact open/close text so the
        two bracket kinds never cross-match), reuses
        `ComplexityPaddingEvaluator.isLoose` unmodified on each pair's
        interior to decide tight vs. loose (no evaluator change needed — it
        already treats a nested call's `(` as the loose signal it needs).
        Only the immediate boundary gap each side is rewritten. Pairs
        spanning multiple lines, containing a comment, or touching a
        frozen token are skipped, same guard posture as the rest of the
        file.
      - `CppSpecificRule.enforceReflectionOperatorSpacing` — same
        gap-buffering technique as `enforcePackIndexingSpacing`: collapses
        the gap after every `^^` OP token to zero width (same guards).
      Verified against hand-written snippets (bare vs. call/nested-bracket
      interior for both bracket kinds, `^^SomeType` tight). `make test`:
      102/102 forward + idempotency, zero regressions.
      `CppSpecificRule.enforcePackIndexingSpacing` (called from
      `FormatterCurly`'s Phase 4 cosmetic-spacing block, `lang.isCpp`-gated)
      collapses the gaps on both sides of an `...` token whenever
      immediately followed by `[` (scoped to that exact adjacency, so
      ordinary variadic `Args...)`/`Args...,` uses are untouched). `make
      test`: 101/101 forward + idempotency, zero regressions. §2
      (`= delete("reason")`) and §3 (placeholder `_`) confirmed to need no
      new code — both already format correctly via existing ordinary
      call-argument/identifier handling (verified against
      `cpp26_core_inp/out.cpp`).
- [x] §4 Contracts implemented: `CppSpecificRule.enforceContractClausePlacement`
      (`pre`/`post` are plain identifiers, not tokenizer keywords, so
      detection is positional — an identifier `pre`/`post` whose previous
      significant token is `)` begins/continues a group; each clause's
      `(...)` content is re-spaced via new `spaceExpressionTokens` helper,
      `post`'s top-level `:` split out for tight-before/space-after) and
      `CppSpecificRule.enforceContractAssertSpacing` (reuses
      `spaceExpressionTokens` for `contract_assert(cond)`'s argument —
      treated as always-normalized rather than left verbatim like ordinary
      call-statement arguments elsewhere in this codebase, despite the spec
      text's "like any other function-call-shaped statement" wording;
      `STYLE_CPP26.md` updated to say so explicitly). **Design decision
      (user-confirmed):** a lone clause follows overflow-based inline/wrap
      like `requires`; a group of **2+** clauses always wraps one-per-line
      regardless of fit. `STYLE_CPP26.md` §4 updated accordingly. Both
      methods wired into `FormatterCurly` (clause placement in Phase 1
      `isCOrCpp`+`lang.isCpp` block right after
      `enforceRequiresClausePlacement`; assert spacing in Phase 4 alongside
      `enforcePackIndexingSpacing`). `make test`: 101/101 forward +
      idempotency, zero regressions, `cpp26_core_inp/out.cpp` promoted to
      active in the Makefile. `cpp26_comments_inp.cpp` deliberately NOT yet
      promoted — a comment between the signature's `)` and the first `pre`
      clause was silently dropped by the replaced span; left as a known gap
      (fixed in the next item).
- [x] `cpp26_comments` comment-drop gap fixed. `enforceContractClausePlacement`
      now tracks each clause's own keyword token index (`clauseKeywordIdx`)
      and pulls any `COMMENT_LINE`/`COMMENT_BLOCK` tokens sitting in the
      gap before each clause (via new helper `collectComments`) out of the
      span before it's overwritten, re-inserting them on their own line at
      the clause's indent instead of dropping them. A multi-line block
      comment is reindented via the existing, previously-CSS/JSON-only
      `FormatterSimpleBraced.reindentBlockComment` (now also called from
      `CppSpecificRule.java`). A leading comment now also forces wrapped
      (one-clause-per-line) rendering even for an otherwise-inlinable lone
      clause.

      Verifying against the real JAR surfaced unrelated fixture-authoring
      mistakes, not bugs (each reproduced with plain non-C++26 snippets):
      wrongly-inserted blank lines between consecutive `using` declarations,
      a wrong Allman-brace assumption for `if(init; cond) { ... }` (K&R is
      correct/established), and missing body indentation (formatter doesn't
      reindent). **Investigated, not a bug:** structured bindings collapse
      a trailing same-line comment's gap to a single space via
      `DeclarationAlignmentRuleCurly.java`'s grid path — attempted fix
      reverted, since it broke five already-passing fixtures expecting this
      single-space normalization; confirmed established, intentional
      behavior, no code change. Promoted to active in the Makefile. `make
      test`: 104/104 forward + idempotency, zero regressions.
- [x] Author local test fixture pairs per `FUTURE_TEST_FIXTURES.md`'s
      "CPP26" section and register in the Makefile's `INP_FILES` /
      `test/README.txt`. Done: `cpp26_core_inp/out.cpp`,
      `cpp26_comments_inp/out.cpp`, `cpp26_reflection_inp/out.cpp` all
      extracted to `test/`, registered commented-out in the Makefile
      initially, documented in `test/README.txt`. `cpp26_reflection`'s
      promotion gate (external-corpus cross-check for §5) was explicitly
      overridden per user instruction, since the pair was needed to seed
      the initial tokenizer test for `^^`/`[:`/`:]` — its expected output
      still isn't validated against that cross-check.
- [x] Real-code testing pass against `wrocpp/cpp26-reflection-examples`
      done (fresh clone, `/tmp`). 103 `.cpp`/`.hpp`/`.h`/`.cc` files (51
      exercise `^^`/`[: :]`/`template for`). Zero crashes; round1→round2
      `diff -r` empty (103/103 idempotent). No bugs found, zero fixtures
      added. Compilation not attempted (only compilers available then,
      `g++ 4.8.5`/`clang++ 3.7.1`, predate P2996/C++20 support) —
      idempotency + manual inspection used as fallback (applies to
      `simdjson`/`rjk-duck` below too, until the toolchain upgrade under
      `glaze`). Manual spot-check: `^^int`/`^^T`/`^^Point` stay tight;
      bare-identifier splice interior tight, nested-call/`::`-qualified
      interior loose — consistent with `isLoose`, no corruption. `template
      for(...)` uses the same no-space-before-paren + single-statement
      inline collapse as ordinary `for` (confirmed general via a plain
      non-`template` repro). This is the external-corpus validation for §5
      previously pending (Scope §5). `bloomberg/clang-p2996` confirmed
      empty/unusable (see that section).
- [x] Real-code testing pass against `simdjson/experimental_json_builder`
      done (fresh shallow clone, scratchpad). 27 `.cpp`/`.hpp`/`.h`/`.cc`
      files, 3.9k lines. Zero crashes on the initial pass.

      **One idempotency bug found+fixed.** round1→round2 `diff -r`
      non-empty: `benchmarks/simpleparser/from_json.hpp`'s
      `[:simdjson::json_builder::expand(std::meta::nonstatic_data_members_of(^T)):] >>`
      line rendered one-line (102-char, over 100-char limit) on round1 but
      wrapped on round2. Root cause: `FormatterCurly`'s phase ordering ran
      `CppSpecificRule.enforceAttributeAndSpliceBracketPadding` (can grow a
      line via its loose `[: expr :]` padding) in Phase 4, *after*
      `MiscRuleCurly.enforceCallLineBreaking`'s fits-check in Phase 1 had
      already decided not to wrap — same bug shape already fixed once for
      `enforceComplexityPadding` (flagged generically in
      `STATE_COMMON.md`'s Architectural TODOs, "Ordering interacts with
      every other pass"). Fixed by pulling
      `enforceAttributeAndSpliceBracketPadding` forward to run right before
      `enforceCallLineBreaking`, alongside `enforceComplexityPadding` (both
      `lang.isCpp`-gated there instead of Phase 4).
      `enforcePackIndexingSpacing`/`enforceReflectionOperatorSpacing` stayed
      in Phase 4 — they only tighten spacing, can't trigger this bug class.
      Fixture `test/real_code_regressions_76_{inp,out}.hpp`. `make test`:
      125/125 forward + idempotency, zero regressions.

      **Final full-corpus re-run:** all 27 files, zero crashes,
      round1→round2 `diff -r` empty (27/27 idempotent). Compilation not
      attempted (same too-old-compiler limitation as `wrocpp`); idempotency
      + manual inspection used as fallback. Spot-checked every `^^`/`[: :]`
      file: tight bare identifiers, correctly loose nested calls, no
      corruption.

      **Separate finding, out of scope, not fixed:**
      `include/simdjson/json_builder/json_builder.h` and
      `universal_formatter.h` use `^^`/`[: :]` but are `.h`-extensioned —
      `Lang.infer` maps `.h` to `"c"`, not `"cpp"` (pre-existing
      C/C++/Java-job design), so every §5 rule (`lang.isCpp`-gated)
      silently doesn't apply — no crash/corruption, just non-application
      (confirmed: identical content reformats §5-aware under `.hpp` but not
      `.h`). Not a blocked Open Question — noted for whoever next touches
      `Lang.infer`'s `.h`-handling. **2026-07-28 re-assessment:** unchanged,
      still not this job's territory. **CLOSED 2026-08-11** — accepted as a
      documented limitation (owner decision): default `.h`->`"c"`
      inference is intentionally kept as-is because
      auto-detecting/flipping it risks misapplying C++-only rules to
      genuine C headers; users with C++-content `.h` files must explicitly
      pass the language override (see README Known Limitations).
- [x] Real-code testing pass against `ryanjk5.github.io/posts/rjk-duck`
      (blog post, not a repo) done. No local copy found; fetched via
      WebFetch, extracted all 26 C++ code samples into one file
      (`/tmp/dogfood/rjk-duck/duck_samples.cpp`, minimal scaffolding to
      make the concatenation parseable).

      Formatted once (round1), reformatted (round2): `diff` empty
      (idempotent), zero crashes. Compilation not attempted (same
      too-old-compiler limitation as `wrocpp`/`simdjson`; no C++
      `verifiers` entry exists); idempotency + manual inspection used as
      fallback. Spot-checked every `^^`/`[: :]` occurrence in round1
      output: all 12 distinct `^^operand` forms stay tight; nested-bracket
      interior renders loose, bare identifier tight — no corruption.

      **One finding, out of scope, not fixed:** Sample 10's multi-statement
      lambda body inside a `std::views::transform([=](...) { ... })`
      pipe-chain argument collapses onto one very long `;`-joined line.
      Reproduced with a plain non-C++26 snippet — confirmed pre-existing
      general lambda-body/call-line-breaking behavior
      (`MiscRuleCurly.enforceCallLineBreaking`), not a §1-5 artifact (every
      §5 rule this job owns only gap-buffers spacing, none touch
      statement-level line breaking). C/C++/Java job's territory, not
      raised as a blocked Open Question. **2026-07-28 re-assessment:**
      unchanged, no action taken.

      No fixtures added (no in-scope bug found). Completes the
      `ryanjk5.github.io/posts/rjk-duck` entry in "Test Fixtures (External,
      corpus-scale)" (a "useful extra source", not a repo-scale substitute).
- [x] Real-code testing pass against `stephenberry/glaze` done. Reused
      existing checkout at `/tmp/glaze` (not a fresh clone). 414
      `.hpp`/`.cpp`/`.h` files, formatted in one batch pass grouped by
      subdirectory (one transient `SIGBUS` JVM crash in `libzip.so`,
      traced to `/tmp` at 99% full — environmental, not a formatter bug;
      retry succeeded, zero crashes).

      round1 -> round2 surfaced 37 files with a non-empty `diff`. **None
      involve any C++26 construct** (grepped for `^^`/`[:`/`:]`/
      `contract_assert`/pack-indexing `...[` — none found). All
      root-caused to pre-existing, out-of-this-job's-scope C/C++/Java
      gaps:
      - 33/37: known switch/case relative-delta reindentation drift
        (`SwitchRule.applyNonInlineCaseIndent`) on internally inconsistent
        source — same shape as the then-ACCEPTED `javaparser`/
        `JSONEncoderLite.java` gaps. **Status note (2026-08-16 cleanup
        pass):** the general switch-case-on-internally-inconsistent-source
        shape was fixed 2026-08-07 (`RDD_KEY_251`, see `STATE_C_CPP_JAVA.md`)
        and both `javaparser`/`JSONEncoderLite.java` gaps referenced here are
        now closed too (`RDD_KEY_292`/`RDD_KEY_301`) — not independently
        re-verified against these specific 33 `glaze` files this pass (the
        original `/tmp/glaze` checkout is gone per the 2026-08-15 re-check
        note below), but plausibly also fixed incidentally; re-open in
        `STATE_C_CPP_JAVA.md`'s Known Gaps if a fresh `glaze` clone
        surfaces the same drift again.
      - `glaze_asio.hpp`/`ordered_map_test.cpp`: member-initializer-list
        wrapping inserts a stray space after `.` (`other.index` ->
        `other. index`) on wrap — general init-list-wrapping bug, not
        `CppSpecificRule`.
      - `json_perf_common.hpp`/`json_performance.cpp`: `**` gains an
        inconsistent space on reformat — general operator-spacing logic.
      - `json_patch_test.cpp`: a long initializer-list line wraps
        differently between round1/round2 — general line-breaking logic.
      None of this job's owned methods touch switch/case indentation,
      init-list wrapping, or `*`/`**` spacing — not a blocked Open
      Question, documented for whoever next touches
      `SwitchRule`/init-list-wrapping/operator-spacing in the C/C++/Java
      job.

      **2026-08-15 XL.txt sweep re-check: likely already resolved, not
      re-added to any backlog.** The original `/tmp/glaze` checkout is
      gone (system-cleaned), so the exact files couldn't be re-diffed, but
      hand-built repros matching each described shape (long-wrapped ctor
      with a member-initializer-list referencing `other.field`; a
      declaration-alignment group with a wrapped call argument) all came
      back byte-identical round1/round2 against the current JAR — no
      dot-space corruption, no drift. Plausible cause: several general
      pass-ordering fixes landed in `STATE_C_CPP_JAVA.md` since this entry
      (RDD_KEY_193 assignment-pass re-run, RDD_KEY_290/291/293 openrewrite
      cluster fixes) incidentally cover the same mechanism. Not proven
      since the original corpus files are unavailable — if a fresh `glaze`
      clone surfaces the same diffs again, re-open in
      `STATE_C_CPP_JAVA.md`'s Known Gaps, not here (this job's scope is
      C++26 §1-5 only).

      **2026-08-11: `json_patch_test.cpp` idempotency mismatch FIXED
      (RDD_KEY_285).** Re-cloned `glaze` fresh (the `/tmp/glaze` checkout
      above had been cleaned by the system since); reproduced the exact
      round1/round2 diff on the real
      `tests/json_test/json_patch_test.cpp` and on a minimal local repro
      (`glz::patch_document ops = {{glz::patch_op_type::add, "/b",
      glz::generic(2.0), std::nullopt}};`). Root cause:
      `ScopePipelineCurly.applyOversizedAggregateInitClosingBracePass`'s
      `hasNewlineInside` scan (intended to detect a genuinely oversized
      aggregate init, e.g. a byte/word table spanning many source lines,
      and move its dangling `}` onto its own line) counted *any* NEWLINE
      token inside the outer `{...}` at any brace depth — including one
      strictly inside a nested call's own already-wrapped argument list
      (e.g. `glz::generic(\n  2.0\n)`, wrapped by
      `MiscRuleCurly.enforceCallLineBreaking` elsewhere). A fresh
      single-line source has no such newline yet when this pass runs
      (round1 correctly skipped it, leaving `}` inline), but re-formatting
      round1's own output (round2) saw the call's own wrapped newline and
      wrongly treated the whole init as oversized — classic pass-ordering
      bug (decision made before a later pass introduces the shape it needs
      to already account for), same family as
      `real_code_regressions_76`'s `enforceAttributeAndSpliceBracketPadding`
      fix and the `PowerShellSpecificRule.java`/`applyAssignmentsPass`
      ordering bug in `STATE_COMMON.md`.

      **Fix:** gate the newline check on paren/bracket depth (only a
      NEWLINE at paren/bracket depth 0 counts), mirroring the existing
      `ScopePipelineCore.hasTopLevelNewline` technique used elsewhere in
      this codebase for the identical "is this newline from the
      statement's own multi-line shape, or from a nested call's own wrap"
      distinction. **First attempt applied this unconditionally and
      regressed `test/real_code_regressions_179_inp.ts`** (a JS/TS object
      literal whose expected output relies on the pre-existing behavior of
      moving `}` onto its own line even though its only newline comes from
      a nested wrapped call, `pathOptions = { ...,
      getNormalizedAbsolutePath(\n  ...\n), ... };`) — structurally
      indistinguishable from the glaze shape to a language-agnostic
      paren/bracket-depth gate. Per the user's own suggestion, re-scoped
      the whole paren/bracket-depth-tracking branch to `lang.isCpp` only
      (every other language's `parenDepth` stays permanently `0`, so
      `parenDepth == 0` is unconditionally true there — behavior for
      JS/TS/Java/Kotlin is byte-for-byte unchanged). Verified: closed both
      the minimal repro and the full `json_patch_test.cpp` round1/round2
      diff cleanly (idempotent); re-ran round1/round2 over the entire
      `tests/json_test/` directory (21 files) with no further diffs.
      `make test`: 284/284 forward + 284/284 idempotency, zero regressions
      (`real_code_regressions_179` unaffected once the fix was scoped to
      C++ only). New fixture `test/cpp26_nested_call_wrap_{inp,out}.cpp`
      reproduces the minimal repro's shape.

      **Every file with actual C++26 reflection syntax verified idempotent
      and correctly formatted**, isolated from the 37 failures (5
      spot-checked, all round-trip empty-`diff`). Tight `^^T`/`^^E`/
      `^^std::remove_cvref_t<T>`, tight bare-identifier splice, loose
      nested-call interiors — no corruption. `template for(...)` uses the
      same no-space-before-paren convention as ordinary `for(...)`,
      consistent with the `wrocpp` finding that this is general.

      **Toolchain upgrade:** a modern `clang++ 22.1.8`
      (`~/xsdk/clang22/LLVM-22.1.8-Linux-X64/bin/clang++`) was found
      available mid-session, superseding the `g++ 4.8.5`/`clang++ 3.7.1`
      too-old-for-reflection limitation of every prior C++26 session.
      `-std=c++23 -stdlib=libc++ -fsyntax-only` (`-stdlib=libc++` required
      for `<string_view>` etc. to resolve; stderr piped through
      `grep -v 'no version information available'`) gave **genuine compile
      validation**, not just idempotency + inspection:
      - `get_name.hpp` + `to_tuple.hpp` (the two files with actual `^^`/
        `[: :]` syntax): compile clean, zero diagnostics, unmodified and
        round1-formatted alike.
      - **Full `include/glaze/glaze.hpp` umbrella header (254 headers
        transitively) compiles clean with zero diagnostics, unmodified and
        against the full round1-formatted `include/` tree** — the
        strongest validation any C++26 dogfood session has achieved,
        covering every header rather than a handful of spot-checked files.
      - `jsonrpc_test.cpp`/`yaml_conformance.cpp` could not compile
        standalone (missing vendored test-only `ut/ut.hpp`, unrelated to
        formatting) — not pursued, since the umbrella-header result
        already covers the same reflection code.
      `-fsyntax-only` compilation is now the primary validation
      (idempotency + inspection remains the documented fallback),
      confirming zero formatter-induced compile regressions across the
      entire `include/` tree.

      **No fixtures added — zero bugs found within this job's scope
      (`CppSpecificRule.java`/§1-5 C++26 rules).** All 37 idempotency
      mismatches are pre-existing, non-C++26, C/C++/Java-job-owned gaps.

      This completes all four named external-corpus candidates in "Test
      Fixtures (External, corpus-scale)" above. No further named
      candidates remain on the list; a future session would need to
      source a new one before continuing this line of work.
