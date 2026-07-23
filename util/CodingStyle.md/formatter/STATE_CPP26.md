# STATE_CPP26.md — C++26 JAR Support Tracker

Read `STATE_COMMON.md` first — shared commit/ambiguity/testing conventions
this file assumes. `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` not required.

---

## Purpose

Tracks C++26 support in the deterministic JAR formatter
(`util/CodingStyle.md/formatter/`), per `STYLE_CPP26.md` (builds on
`STYLE.md`, `STYLE_C_CPP.md`, `STYLE_CPP20.md`).

**Toolchain note (discovered during the `glaze` session):** this system
also has a modern `clang++ 22.1.8` at
`~/xsdk/clang22/LLVM-22.1.8-Linux-X64/bin/clang++`, capable of real
`-std=c++23 -fsyntax-only` compile validation including reflection syntax
— use `-stdlib=libc++` (required for standard headers to resolve) and pipe
stderr through `grep -v 'no version information available'` (a harmless
`libstdc++.so.6` symbol-versioning warning, not a compile error).
`/opt/glibc-2.41/` is available if a glibc-mismatch/patchelf issue is ever
hit with some other prebuilt binary. This supersedes the older `g++
4.8.5`/`clang++ 3.7.1` toolchain the "Compilation not attempted" notes in
earlier checklist entries below refer to — prefer `clang++ 22.1.8` for any
future compile-check step in this job rather than falling back to
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
5. Reflection (`^^`, `[:`, `:]`) — **provisional/draft**, explicitly
   flagged in the style doc as needing a tokenizer-support pass before any
   rule is trusted (new `MULTI_CHAR_OPS` entries, longest-prefix-first
   ordering; same risk category as Kotlin's Step 0 tokenizer work). Not
   validated against real JAR behavior yet.
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

All four named candidates above are now complete. No further named
candidates remain queued — a future session sourcing more §5 real-code
coverage would need to pick a new candidate first.

## Test Fixtures (Local)

Local dogfood pairs (distinct from the external-repo corpus-scale list
above) authored and registered in `formatter/test/` — see
`test/README.txt` for the pair list and coverage. The reflection pair
(`cpp26_reflection_inp/out.cpp`) was extracted ahead of its original
promotion gate (§5's external-repo tokenizer validation still pending),
per explicit instruction — see "Done:" note on the checklist below.

**Promoted to active `make test` this session** (Makefile `INP_FILES`),
after §5 padding rules landed and the fixture was run against the real
JAR to verify it (every `^^`/`[[ ]]`/`[: :]` diff matched expected output
with zero changes needed). Two fixture-authoring issues surfaced and were
fixed with explicit user confirmation, both unrelated to the new §5 rules:
- Input's body statements (`reflectMember`, `useSplice`, `checkReflected`)
  had zero leading indentation while expected output assumed 4-space
  indentation — formatter does not reindent (preserves source indentation
  verbatim, see `STATE_COMMON.md`'s Architectural TODOs), so input needed
  correct indentation already, same convention as `cpp26_core_inp.cpp`.
  Fixed by indenting the input's body statements.
- `auto v = [:r:];` gets column-aligned under `constexpr auto r = ^^int;`'s
  `=` by the pre-existing (unrelated, C/C++/Java job's) declaration-
  alignment rule — reproduced with plain non-reflection code
  (`constexpr auto r = 1; auto v = 2;`) to confirm it's not a §5-rule
  artifact. Fixed by updating expected output to the real aligned column,
  since the alignment itself is correct, pre-existing behavior.

Still NOT cross-checked against the STYLE_CPP26.md §5 external-corpus
fixture repos (`bloomberg/clang-p2996` etc.) — only against this
formatter's own actual JAR output. That corpus-scale validation pass
remains open (see Scope §5 / Test Fixtures (External, corpus-scale)).

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

None recorded yet. Note: `STYLE_CPP26.md` §5 (Reflection) is explicitly
marked provisional/draft pending a tokenizer validation pass — a known gap
in the style doc itself, not yet elevated to a formal blocked Open
Question here since real implementation hasn't started.

---

## Checklist

- [ ] Diff `STYLE_CPP26.md` against `STYLE_CPP20.md`/`STYLE_C_CPP.md` to
      enumerate exactly which rules are C++26-specific vs. already covered
      (§1–3 look like they may already be structurally covered; §4/§5 look
      genuinely new — confirm by diffing, don't assume).
- [x] **Language-selection mechanism resolved (RDD_KEY_180, reversing
      RDD_KEY_179), this session.** C++26 is NOT a separate selectable
      language — no `Lang.isCpp26` flag, no `--lang cpp26`/`lang=cpp26`
      selector, no `SCAFFOLD_ONLY_LANGUAGES` entry. `.cpp`/`.hpp` files
      resolve to `"cpp"` exactly as before; C++26 rule coverage lands
      directly inside the existing `"cpp"`-gated pipeline
      (`CppSpecificRule.java` etc.), matching how C++20 was folded in with
      no separate selector. (An earlier session had introduced
      `--lang cpp26` as an explicit-only selector under RDD_KEY_179 —
      reverted this session as an unnecessary departure from precedent.)
- [x] Tokenizer smoke check done (this session, via sub-agent, read-only —
      no source/state files touched). Confirmed via `TokenizerCurly.java`'s
      `MULTI_CHAR_OPS` array (lines 114-120): `^^`, `[:`, `:]` are absent,
      so they mis-split into single-char constituents (`^^` → `^`+`^`,
      `[:` → `[`+`:`, `:]` → `:`+`]`) — no crash, no swallowing. CLI run
      against a hand-written `/tmp` snippet confirmed the mis-split is
      actively **corrupting**, not inert passthrough: `^^SomeType`
      (no-space input) rendered as `^ ^ SomeType`, and `[:r:]` vs.
      `[: computeRefl(x) :]` — the same splice-bracket construct — got
      inconsistent spacing (`[: r :]` vs. `[ : computeRefl(x) : ]`)
      depending on interior content, since `[`/`:` are each independently
      re-spaced by ordinary bracket/colon rules rather than treated as one
      splice-bracket unit. No secondary/cascading corruption found beyond
      this spacing inconsistency. Assessment: expected, contained finding
      — safe to proceed to the real tokenizer pass whenever picked up.
- [x] Tokenizer support pass for §5 Reflection done: `TokenizerCurly.java`'s
      `MULTI_CHAR_OPS` array gained `"^^"`, `"[:"`, `":]"` entries (none a
      strict prefix of any other existing entry, so no new ordering
      constraint). `":]"` reaches `emitOperator()` via the dispatch loop's
      existing default case (unhandled leading char falls there), no
      dispatch change needed; `"[:"` needed a new dispatch branch
      (`c == '[' && peek(1) == ':' && lang.isCpp`) placed before the
      generic open-bracket branch, since a leading `[` is otherwise always
      intercepted into `emitOpenBracket` first — gated on `lang.isCpp` only
      (not shared with C/Java/Kotlin) since this branch changes
      tokenization for every leading `[`, unlike the two
      `MULTI_CHAR_OPS`-only additions which are inert no-ops for other
      languages (`^^`/`:]` as adjacent chars essentially never occur in
      real C/Java/Kotlin source). Verified against the same `/tmp` smoke
      snippet: `^^SomeType` no longer splits into `^ ^ SomeType`;
      `[:r:]` and `[: computeRefl(x) :]` now render with the *same*
      consistent interior-padding pattern instead of diverging. `make
      test`: 102/102 forward + idempotency, zero regressions. Note:
      tokenizer support only — no tight/loose padding rule or
      `^^`-binds-tight-to-operand rule implemented for §5 yet (still
      deliberately provisional/draft per Scope §5; snippet still renders
      `^^ SomeType` with a space, splice brackets still get generic loose
      padding regardless of content complexity) — that rule-implementation
      step is separate, not-yet-started, gated on this tokenizer pass
      (now done).
- [x] §5 tight/loose padding rules implemented this session, in
      `CppSpecificRule.java` + `FormatterCurly.java`'s Phase 4 block
      (`lang.isCpp`-gated), with one correction to STYLE_CPP26.md itself.
      Before writing code, empirically tested current JAR behavior on
      `[[assume(a>=0)]]`/`[[nodiscard]]` and found STYLE_CPP26.md §5's
      claim that splice-bracket padding "mirrors the existing JAR-verified
      `[[ assume(a >= 0) ]]` case" was **false** — `[[ ]]` attributes were
      left completely verbatim/unformatted, no tight/loose rule existed at
      all (confirmed with user via `AskUserQuestion`: the coincidental
      interior padding seen in an earlier smoke test came from the
      unrelated generic paren-complexity rule firing on `assume(...)`'s own
      `(...)`, not attribute-aware logic). Per user's explicit decision
      (implement both together since the precedent was coincidental, not
      truly pre-existing), added:
      - `CppSpecificRule.enforceAttributeAndSpliceBracketPadding` — new
        method handling both `[[ ]]` and `[: :]` with one shared
        implementation: forward-scans for matched OP-token pairs (small
        stack keyed on exact open/close text so `[[`/`]]` and `[:`/`:]`
        pairs are never cross-matched), then for each pair reuses
        `ComplexityPaddingEvaluator.isLoose` unmodified on the interior's
        significant tokens to decide tight (no space) vs. loose (single
        space) — `isLoose` already only checks for nested PUNCT `(`/`[`
        tokens, and a call's own `(` is exactly that, so no OP-vs-PUNCT
        change to the evaluator needed. Only the immediate boundary gap on
        each side is rewritten; everything else inside is left verbatim.
        Pairs spanning multiple physical lines, containing a comment, or
        touching a frozen token are skipped entirely, same guard posture
        as every other rewrite in this file.
      - `CppSpecificRule.enforceReflectionOperatorSpacing` — new method,
        same gap-buffering technique as `enforcePackIndexingSpacing`:
        collapses the gap after every `^^` OP token to zero width (subject
        to the same comment/newline/frozen guards), giving `^^SomeType` no
        space between operator and operand.
      Verified against hand-written snippets:
      `[[assume(a>=0)]]`/`[[ assume(a>=0) ]]` → `[[ assume(a>=0) ]]` (loose,
      call inside); `[[nodiscard]]`/`[[ nodiscard ]]` → `[[nodiscard]]`
      (tight, bare); `[:refl:]` stays tight; `[:computeRefl(x):]`/
      `[: computeRefl(x) :]` → `[: computeRefl(x) :]` (loose, call inside);
      `[:arr[0]:]` → `[: arr[0] :]` (loose, nested bracket); `^^SomeType`
      stays tight (no space introduced or removed). `make test`: 102/102
      forward + idempotency, zero regressions.
      `CppSpecificRule.enforcePackIndexingSpacing` (new method, called from
      `FormatterCurly`'s Phase 4 cosmetic-spacing block, `lang.isCpp`-gated)
      collapses the gaps on both sides of an `...` token whenever
      immediately followed by `[` (scoped to that exact adjacency only, so
      ordinary variadic `Args...)`/`Args...,` uses are untouched). `make
      test`: 101/101 forward + idempotency, zero regressions. §2
      (`= delete("reason")`) and §3 (placeholder `_`) confirmed to need no
      new code — both already format correctly via existing ordinary
      call-argument/identifier handling (verified against
      `cpp26_core_inp/out.cpp`).
- [x] §4 Contracts implemented: `CppSpecificRule.enforceContractClausePlacement`
      (placement: `pre`/`post` are plain identifiers, not tokenizer
      keywords, so detection is positional — an identifier `pre`/`post`
      whose previous significant token is `)` (own or a preceding clause's)
      begins/continues a group; each clause's own `(...)` content is
      re-spaced as a plain expression via new `spaceExpressionTokens`
      helper, with `post`'s top-level `:` split out for
      tight-before/space-after rendering) and
      `CppSpecificRule.enforceContractAssertSpacing` (reuses
      `spaceExpressionTokens` for `contract_assert(cond)`'s argument —
      decided this session to treat it as always-normalized despite the
      spec text's "like any other function-call-shaped statement" wording,
      since ordinary call-statement arguments are otherwise left verbatim
      elsewhere in this codebase; `STYLE_CPP26.md` updated to describe this
      explicitly). **Design decision (this session, user-confirmed):** a
      lone single clause follows overflow-based inline/wrap like
      `requires`; a group of **2+** clauses always wraps one-per-line
      regardless of fit — multiple contract clauses are always easier to
      read one per line. `STYLE_CPP26.md` §4 updated accordingly. Both new
      methods wired into `FormatterCurly` (contract clause placement in
      Phase 1 `isCOrCpp`+`lang.isCpp` block right after
      `enforceRequiresClausePlacement`; contract_assert spacing in Phase 4
      cosmetic-spacing `lang.isCpp` block alongside
      `enforcePackIndexingSpacing`). `make test`: 101/101 forward +
      idempotency, zero regressions, `cpp26_core_inp/out.cpp` promoted to
      active in the Makefile (uncommented from `INP_FILES`).
      `cpp26_comments_inp.cpp` deliberately NOT yet promoted — exercises a
      comment between the signature's `)` and the first `pre` clause,
      which `enforceContractClausePlacement`'s replaced span would
      currently silently drop (not yet handled); left as a known gap in
      the Makefile's comment for a future session.
- [x] `cpp26_comments` comment-drop gap fixed, this session.
      `enforceContractClausePlacement` now tracks each clause's own keyword
      token index (`clauseKeywordIdx`) and, for each clause, pulls any
      `COMMENT_LINE`/`COMMENT_BLOCK` tokens sitting in the gap before it
      (via new helper `collectComments`) out of the span about to be
      overwritten, re-inserting them on their own line at the clause's
      indent instead of silently dropping them. A multi-line block comment
      is reindented via the existing, previously-CSS/JSON-only
      `FormatterSimpleBraced.reindentBlockComment` (now also called from
      `CppSpecificRule.java`) so continuation lines shift by the same
      amount as the first line. A leading comment now also forces wrapped
      (one-clause-per-line) rendering even for an otherwise-inlinable lone
      clause, since there's nowhere sensible to put a comment on an
      inlined line.

      Verifying the fixture against the real JAR surfaced several *other*,
      unrelated mismatches between the fixture's expected output and
      actual behavior — none bugs in this fix, confirmed by reproducing
      each with plain non-C++26 snippets:
      - Blank lines the fixture inserted between consecutive `using`
        declarations — no such rule exists; fixed expected output to match
        (no blank lines).
      - `if(init; cond) { ... }` staying K&R-brace (`) {` same line) is
        correct, established behavior (`cpp_core_out.cpp` already shows
        plain `if(...) {` the same way) — fixed expected output, which had
        wrongly assumed Allman conversion here.
      - Input's `divide`/`clamp`/`process` function bodies lacked the
        4-space indentation the expected output assumed — formatter
        doesn't reindent (see `STATE_COMMON.md`'s Architectural TODOs), so
        fixed the input to already carry the indentation, same convention
        as `cpp26_core_inp.cpp`/`cpp26_reflection_inp.cpp`.
      - **Investigated, not a bug:** structured bindings collapsing a
        trailing same-line comment's original gap to a single space
        (`auto [_, count] = getResult();  // comment` -> one space out),
        via `DeclarationAlignmentRuleCurly.java`'s declaration-alignment
        grid rendering path. Initially looked like a one-line bug since
        plain statements (e.g. `using X = ...;  // comment`) preserve
        original spacing verbatim outside that path. Attempted a fix
        (widen to two spaces / preserve original gap) and reverted it: it
        broke five already-passing fixtures (`c_comments`, `cpp_comments`,
        `java_comments`, `cpp_combined`, plus this one) that all expect
        exactly one space before a trailing comment in this grid path
        regardless of source gap width — confirming single-space
        normalization here is established, intentional behavior, not a
        defect. No code change; fixture's expected output stays
        single-spaced to match correct real behavior.
      Promoted to active in the Makefile. `make test`: 104/104 forward +
      idempotency, zero regressions.
- [x] Author local test fixture pairs per `FUTURE_TEST_FIXTURES.md`'s
      "CPP26" section (reflection pair sequenced after the §5 tokenizer
      validation pass, per that section's own note) and register in the
      Makefile's `INP_FILES` / `test/README.txt`. Done: `cpp26_core_inp/out.cpp`,
      `cpp26_comments_inp/out.cpp`, `cpp26_reflection_inp/out.cpp` all
      extracted to `test/`, registered commented-out in the Makefile (real
      §1–4/§5 rule coverage not yet implemented at authoring time),
      documented in `test/README.txt`. `cpp26_reflection`'s promotion gate
      (external-corpus cross-check for §5) was explicitly overridden per
      user instruction, since the pair was needed to seed the initial
      tokenizer test for `^^`/`[:`/`:]` — its expected output still isn't
      validated against that cross-check.
- [x] Real-code testing pass against `wrocpp/cpp26-reflection-examples`
      done this session (fresh shallow clone under `/tmp`, not previously
      present). 103 `.cpp`/`.hpp`/`.h`/`.cc` files (curated reflection blog
      examples repo), 51 of which exercise `^^`/`[: :]`/`template for`
      syntax. Formatted in one batch via `--out`/`--preserve-tree`/`--root`
      — zero crashes/exceptions. round1 -> round2 `diff -r` empty (103/103
      idempotent). No bugs found — zero fixtures added. Compilation not
      attempted: system's only available compilers are `g++ 4.8.5` and
      `clang++ 3.7.1`, both far too old for any P2996/reflection support
      (predates even C++20), so full compile verification isn't possible on
      this system; idempotency + manual inspection used as the documented
      fallback. Manually spot-checked several files with reflection syntax
      (`hello_reflection.cpp`, `posts/03-splicing/examples/
      splice_basics.cpp`, `posts/20-reflect-arbitrary/examples/
      arbitrary.cpp`): `^^int`/`^^T`/`^^Point` stay tight as designed;
      `[: r_int :]`/`obj.[: m :]` collapse to tight (`[:r_int:]`/
      `obj.[:m:]`) when interior is a bare identifier, stay loose
      (`[: std::meta::type_of(m) :]`) when interior contains a nested
      call/`::`-qualified expression — consistent with the existing
      `isLoose` bracket-complexity rule, no corruption in either case;
      `template for(...)` renders with the same no-space-before-paren
      style as ordinary `for(...)` in this corpus's config, and a
      single-statement body collapses to inline exactly like an ordinary
      single-statement `for` loop does (confirmed this is pre-existing
      general behavior, not reflection-specific or a new bug, by
      reproducing with a plain non-`template` `for` snippet in `/tmp`); a
      multi-statement `template for` body correctly keeps its braces. This
      now provides real external corpus validation for §5 previously noted
      as pending (see Scope §5 / Test Fixtures (External, corpus-scale)).
      `bloomberg/clang-p2996` confirmed empty/unusable (see that section).
      `simdjson/experimental_json_builder`, `stephenberry/glaze` remain
      not-started.
- [x] Real-code testing pass against `simdjson/experimental_json_builder`
      done this session (fresh shallow clone under scratchpad, not
      previously present). Repo confirmed genuinely small as expected: 27
      `.cpp`/`.hpp`/`.h`/`.cc` files, 3.9k total lines, no bundled/amalgamated
      simdjson single-header blob present (largest file, `apple_arm_events.h`
      at 1104 lines, is a plain perf-counter-ID header, not vendored
      simdjson). Formatted in one batch via `--out`/`--preserve-tree`/
      `--root` -- zero crashes/exceptions on the initial pass.

      **One real idempotency bug found and fixed.** round1 -> round2 `diff
      -r` was NOT initially empty:
      `benchmarks/simpleparser/from_json.hpp`'s
      `[:simdjson::json_builder::expand(std::meta::nonstatic_data_members_of(^T)):] >>`
      line rendered as one (102-char, over the 100-char limit) line on
      round1 but wrapped onto three lines on round2. Root cause:
      `FormatterCurly`'s Phase ordering had
      `CppSpecificRule.enforceAttributeAndSpliceBracketPadding` (which can
      grow a line's width via its loose `[: expr :]` padding) running in
      Phase 4, *after* `MiscRuleCurly.enforceCallLineBreaking`'s
      "does it fit in `LINE_LENGTH_LIMIT`" measurement in Phase 1 had already
      run and decided not to wrap -- so a fresh format measured the
      pre-padding (98-char, fits) width and stayed one line, while
      reformatting that already-padded output measured the post-padding
      (102-char, over limit) width and wrapped. Exactly the same bug shape
      already documented and fixed once for `enforceComplexityPadding`
      (see the comment at `FormatterCurly.java`'s Phase 1/enforceComplexity
      Padding call) and flagged as a generic risk in `STATE_COMMON.md`'s
      Architectural TODOs ("Ordering interacts with every other pass").
      Fixed the same way: pulled `enforceAttributeAndSpliceBracketPadding`
      forward to run right before `enforceCallLineBreaking`, alongside
      `enforceComplexityPadding` (both `lang.isCpp`-gated at that point
      instead of down in Phase 4). `enforcePackIndexingSpacing`/
      `enforceReflectionOperatorSpacing` were deliberately left in Phase 4 --
      both only ever tighten (remove) spacing, never grow a line's width, so
      they can't trigger this class of bug. Added fixture pair
      `test/real_code_regressions_76_{inp,out}.hpp` (minimal repro of the
      exact construct), registered in the Makefile's `INP_FILES` and
      `test/README.txt`. `make test`: 125/125 forward + idempotency, zero
      regressions.

      **Final full-corpus re-run after the fix:** all 27 files reformatted
      again -- zero crashes, round1 -> round2 `diff -r` fully empty
      (27/27 idempotent). Compilation not attempted: same `g++ 4.8.5`/
      `clang++ 3.7.1` toolchain limitation already documented for the
      `wrocpp` session (both far too old for P2996/reflection); idempotency +
      manual inspection used as the same documented fallback.

      Manually spot-checked every file containing `^^`/`[: :]` syntax
      (`examples/demo.cpp`, `examples/example2.cpp`, `examples/example3.cpp`,
      `tests/user_profile_tests.cpp`, `benchmarks/simpleparser/
      from_json.hpp`, plus the two `.h` files below): `^^T`/`^^Z` stay tight;
      `[:expand(...):]`/`[: json_builder::expand(...) :]` correctly go loose
      (nested call inside) with consistent padding on both the `[:`/`:]`
      boundary and the call's own parens; `t.[:dm:]`/`t.[:mem:]` (member
      splice, bare identifier interior) stay tight; no corruption found in
      any reflection construct.

      **Separate finding, confirmed out of scope for this job, not fixed
      here:** `include/simdjson/json_builder/json_builder.h` and
      `universal_formatter.h` both use `^^`/`[: :]` reflection syntax but are
      `.h`-extensioned -- `Lang.infer` maps `.h` to plain `"c"`, not `"cpp"`
      (pre-existing, `.h`-is-ambiguous-defaults-to-C design predating this
      job, in `Lang.java`, C/C++/Java job's territory, not `CppSpecificRule`/
      Curly-family code this job owns). Since every §5 rule in this job is
      `lang.isCpp`-gated, these two files silently get zero §5 rules applied
      -- no crash, no corruption, just complete non-application (confirmed by
      isolating: identical content reformats loose/§5-aware under a `.hpp`
      filename but tight/§5-unaware under a `.h` filename, in the same
      directory, same invocation). Not this job's file/scope to change
      (`Lang.infer`'s extension table is shared, general-purpose language
      detection, not C++26-specific), and not raised as a blocked Open
      Question since it isn't an ambiguity *within* this job's own rule set --
      documented here purely as a real-corpus finding for whoever next
      touches `Lang.infer`'s `.h`-handling decision.

- [x] Real-code testing pass against `ryanjk5.github.io/posts/rjk-duck` (blog
      post, not a repo) done this session. No local copy found under `/tmp`
      (checked first per methodology); fetched via WebFetch and extracted all
      26 C++ code samples from the post into one file,
      `/tmp/dogfood/rjk-duck/duck_samples.cpp` (samples wrapped in enough
      surrounding scaffolding — e.g. dummy bodies for `{ ... }` placeholder
      elisions, renamed a few duplicate type names across samples like
      `MyTrait`/`Container`/`vtable_wrapper` so the concatenated file doesn't
      redefine the same symbol twice — to be syntactically parseable by the
      tokenizer even though several samples were never meant to compile
      standalone, e.g. `[: /* find perf_options trait, or fall back to
      default */ :]` placeholder specializations).

      Formatted once (round1), reformatted round1's output (round2): `diff`
      fully empty (idempotent), zero crashes/exceptions on either pass.
      Compilation not attempted — same `g++ 4.8.5`/`clang++ 3.7.1` toolchain
      limitation already documented for the `wrocpp`/`simdjson` sessions
      (both far too old for P2996/reflection); idempotency + manual
      inspection used as the same documented fallback (no `syntax_checker`
      entry exists for C++ either — that tool only covers the data-format/
      Java/Kotlin jobs).

      Manually spot-checked every `^^`/`[: :]` occurrence in round1 output:
      `^^MyType`/`^^trait`/`^^has_fn`/`^^Traits...`/`^^void*`/`^^vtable`/
      `^^T`/`^^candidate_wrapper`/`^^overload_set`/`^^vtable_function2`/
      `^^vtable_function_wrapper`/`^^inlined_functions` all stay tight to
      their operand as designed; `[: slots[index] :]` (nested bracket
      interior) renders loose, `[:VtableMember:]`/`[:Member:]` (bare
      identifier interior) render tight — consistent with the existing
      `isLoose` bracket-complexity rule, no corruption in any occurrence.

      **One finding, confirmed out of scope for this job, not fixed here:**
      Sample 10's multi-statement lambda body inside a `std::views::transform(
      [=](...) { ...; ...; ...; return ...; })` pipe-chain argument collapses
      onto one very long line with statements joined by `;` instead of
      staying multi-line, e.g. body statements
      `self_type`/`params`/`args`/`return substitute(...)` all end up on one
      source line. Reproduced with a plain non-C++26 snippet
      (`/tmp/dogfood/rjk-duck/repro.cpp` — no `^^`/`[: :]`/`template for`
      anywhere in it) to confirm this is pre-existing general lambda-body/
      call-line-breaking behavior, not a §5 (or any C++26 §1-4) rule
      artifact — every §5 rule this job owns is narrowly gap-buffering-only
      (`enforceReflectionOperatorSpacing`/
      `enforceAttributeAndSpliceBracketPadding`), none of them touch
      statement-level line breaking inside a lambda body. Belongs to the
      general call-line-breaking/lambda-body logic
      (`MiscRuleCurly.enforceCallLineBreaking` per this file's own earlier
      references), C/C++/Java job's territory not `CppSpecificRule`/
      C++26-owned code — not raised as a blocked Open Question since it
      isn't an ambiguity within this job's own rule set, documented here
      purely as a real-corpus finding for whoever next touches that method,
      same posture as the earlier `.h`-vs-`.hpp` `Lang.infer` finding from
      the `simdjson` session.

      No fixtures added (no bug within this job's scope found). This
      completes the `ryanjk5.github.io/posts/rjk-duck` entry in "Test
      Fixtures (External, corpus-scale)" — it was always noted there as
      "useful extra source" alongside the repo-scale candidates, not a
      substitute for them.

      `stephenberry/glaze` remains not-started.
- [x] Real-code testing pass against `stephenberry/glaze` done this session.
      Reused an existing checkout found under `/tmp/glaze` (checked first per
      methodology — not a fresh clone). 414 `.hpp`/`.cpp`/`.h` files.
      Formatted in one batch pass grouped by subdirectory via `--out`/
      `--preserve-tree`/`--root` (one JVM invocation per subdirectory hit a
      transient `SIGBUS` JVM crash in `libzip.so` on one directory, tracked
      to this system's `/tmp` filesystem sitting at 99% full at the time —
      confirmed environmental, not a formatter bug, since a bare retry of the
      same 5 remaining directories succeeded with exit 0 every time). All
      414 files formatted with zero crashes/exceptions once retried.

      round1 -> round2 (single-invocation re-format, all 414 files in one
      JAR call — no crash that time) surfaced 37 files with a non-empty
      `diff`. **Investigated each; none involve any C++26 construct** (no
      `^^`/`[:`/`:]`/`contract_assert`/pack-indexing `...[` in any of the 37
      — confirmed by grepping each diffed file). Root causes identified by
      inspection, all pre-existing and outside this job's scope
      (`CppSpecificRule.java`/§1-5 rules), matching the general-reindentation
      gap class `STATE_COMMON.md`'s Architectural TODOs already documents:
      - Most (33/37) are the known switch/case relative-delta reindentation
        drift (`SwitchRule.applyNonInlineCaseIndent`) on internally
        inconsistent source — same bug shape as the already-ACCEPTED
        `javaparser`/`JSONEncoderLite.java` gaps, not new.
      - A handful (`glaze_asio.hpp`, `ordered_map_test.cpp`) showed member-
        initializer-list line-wrapping inserting a stray space after `.`
        (`other.index` -> `other. index` -> `other. ec`) when a long
        single-line init-list gets wrapped one-member-per-line — a C/C++/Java
        job bug in the general call/init-list wrapping logic, not
        `CppSpecificRule`.
      - One (`json_perf_common.hpp`/`json_performance.cpp`) showed `**`
        (pointer-to-pointer-looking multiply, `iterations**binary_byte_length`)
        gaining an inconsistent space on reformat — again general C/C++
        operator-spacing logic, not a C++26 rule.
      - One (`json_patch_test.cpp`) showed a long initializer-list line
        wrapped differently between round1/round2 — general line-breaking
        logic, not C++26.
      All confirmed out-of-scope the same way prior sessions confirmed the
      `.h`-vs-`.hpp` (`simdjson` session) and lambda-body (`rjk-duck` session)
      findings: none of this job's owned methods
      (`enforceReflectionOperatorSpacing`/
      `enforceAttributeAndSpliceBracketPadding`/
      `enforcePackIndexingSpacing`/`enforceContractClausePlacement`/
      `enforceContractAssertSpacing`) touch switch/case indentation,
      initializer-list wrapping, or `*`/`**` operator spacing. Not raised as
      a blocked Open Question (not an ambiguity within this job's own rule
      set) — documented here purely as real-corpus findings for whoever next
      touches `SwitchRule`/init-list wrapping/operator-spacing in the
      C/C++/Java job.

      **Every file containing actual C++26 reflection syntax was separately
      verified idempotent and correctly formatted**, isolated from the 37
      failures above: `include/glaze/reflection/get_name.hpp`,
      `include/glaze/reflection/to_tuple.hpp`,
      `tests/networking_tests/http_server_test/
      http_server_headers_validation_test.cpp`, `tests/jsonrpc_test/
      jsonrpc_test.cpp`, `tests/yaml_conformance/yaml_conformance.cpp` all
      round-trip with an empty `diff`. Manually spot-checked `^^T`/`^^E`/
      `^^std::remove_cvref_t<T>` (stay tight), `[:Enums:][I]`/`[:Enums:]`/
      `obj.[:member:]` (bare-identifier interior, tight) and
      `enumerators_of(^^E)` nested-call interiors rendering loose padding
      where the source itself already had a nested call — all consistent
      with the existing `isLoose` bracket-complexity rule, no corruption.
      `template for(constexpr std::meta::info I : [:Enums:])` renders with
      the same no-space-before-paren convention as ordinary `for(...)`,
      consistent with the `wrocpp` session's finding that this is general,
      not reflection-specific.

      **Toolchain upgrade, this session:** a modern `clang++ 22.1.8`
      (`~/xsdk/clang22/LLVM-22.1.8-Linux-X64/bin/clang++`) was pointed out as
      available on this system mid-session, superseding the `g++ 4.8.5`/
      `clang++ 3.7.1` too-old-for-reflection limitation documented in every
      prior C++26 session. `-std=c++23 -stdlib=libc++ -fsyntax-only`
      (`-stdlib=libc++` required — without it, `<string_view>` etc. aren't
      found; stderr piped through `grep -v 'no version information
      available'` to filter a harmless `libstdc++.so.6` symbol-versioning
      warning, not a compile error) gave **genuine compile validation**,
      not just idempotency + manual inspection:
      - `include/glaze/reflection/get_name.hpp` +
        `include/glaze/reflection/to_tuple.hpp` (the two files with actual
        `^^`/`[: :]` reflection syntax): compile clean, zero diagnostics,
        both unmodified and round1-formatted.
      - **Full `include/glaze/glaze.hpp` umbrella header (254 headers
        transitively, the entire `include/glaze` tree) compiles clean with
        zero diagnostics, both unmodified and against the full
        round1-formatted `include/` tree** — the strongest validation any
        C++26 dogfood session has achieved so far, covering every header in
        the corpus rather than a handful of spot-checked files.
      - Two `tests/*.cpp` files (`jsonrpc_test.cpp`, `yaml_conformance.cpp`)
        could not be compiled standalone — missing vendored test-only
        dependency `ut/ut.hpp` (a testing framework, not part of this
        checkout) — unrelated to formatting, not pursued further given the
        umbrella-header result already covers the same reflection code
        those tests exercise via `include/glaze`.
      Idempotency + manual inspection is retained as documentation of the
      prior fallback approach but is no longer the only validation used
      this session — actual `-fsyntax-only` compilation confirms zero
      formatter-induced compile regressions across the entire `include/`
      tree.

      **No fixtures added — zero bugs found within this job's scope
      (`CppSpecificRule.java`/§1-5 C++26 rules).** All 37 idempotency
      mismatches are pre-existing, non-C++26, C/C++/Java-job-owned gaps.

      This completes all four named external-corpus candidates in "Test
      Fixtures (External, corpus-scale)"
      (`wrocpp/cpp26-reflection-examples`, `simdjson/experimental_json_builder`,
      `stephenberry/glaze`, plus the `rjk-duck` blog-post extra source) —
      `bloomberg/clang-p2996` was confirmed empty/unusable in an earlier
      session (see that section). No further named candidates remain on the
      list; a future session would need to source a new candidate before
      continuing this line of work.
