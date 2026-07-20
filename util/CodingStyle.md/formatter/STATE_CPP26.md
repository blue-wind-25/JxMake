# STATE_CPP26.md — C++26 JAR Support Tracker

Read `STATE_COMMON.md` first — it has the shared commit/ambiguity/testing
conventions this file assumes. `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` (the
other jobs' files) are NOT required reading for this one — only
`STATE_COMMON.md` is.

---

## Purpose

Tracks implementation of C++26 support in the deterministic JAR formatter
(`util/CodingStyle.md/formatter/`), per `STYLE_CPP26.md` (which itself builds
on `STYLE.md`, `STYLE_C_CPP.md`, and `STYLE_CPP20.md`).

---

## Scope

`STYLE_CPP26.md` covers finalized C++26 constructs only (C++26 shipped/
finalized 28 March 2026; no C++29 content). It extends the existing,
already-implemented C/C++ support (`STYLE_C_CPP.md`, frozen C++17/20/23
baseline in `STYLE_CPP20.md`) with:

1. Pack indexing (`T...[i]`) — falls under existing array-index bracket
   rules, no new padding logic.
2. `= delete("reason")` — trivial, ordinary function-call-argument spacing.
3. Placeholder `_` — ordinary identifier, no new rule.
4. Contracts (`pre`/`post`/`contract_assert`) — comparable to the existing
   trailing-`requires`-clause handling; each clause gets its own line,
   overflow-triggered wrap like `requires`.
5. Reflection (`^^`, `[:`, `:]`) — **provisional/draft**, explicitly flagged
   in the style doc as needing a tokenizer-support pass before any rule is
   trusted (new `MULTI_CHAR_OPS` entries, longest-prefix-first ordering,
   real risk of latent tokenizer bugs surfacing, same category of risk as
   Kotlin's Step 0 tokenizer work). Not validated against real JAR behavior
   yet — style doc treats this section as draft.
6. Config — no new config keys for §1–4; §5 deliberately has none yet since
   its rules aren't trusted. See "Config" below.
7. Test fixtures — authored and registered in `formatter/test/`. See "Test
   Fixtures (Local)" below.

**C++26 is NOT a separately selectable language.** It has no `Lang.isCpp26`
flag, no `--lang cpp26` / `lang=cpp26` selector, and no `SCAFFOLD_ONLY_LANGUAGES`
entry — `.cpp`/`.hpp` files resolve to the existing, already-implemented
`"cpp"` pipeline exactly as they always have. C++26 rule coverage lands
directly inside that pipeline's existing rule classes (primarily
`CppSpecificRule.java`) when implemented, the same way C++20 support was
folded additively into `"cpp"` with no separate `isCpp20`/`--lang cpp20`
selector (`STYLE_CPP20.md` extends `STYLE_C_CPP.md` in place). See RDD_KEY_180.

No `src/` files yet — this job's own rule coverage (extending
`CppSpecificRule.java` or similar) does not exist yet; when it lands it is
gated on `isCpp`, not a new flag.

---

## Resolved Design Decisions

Full text of each decision lives in `RDD_LOG.md` (shared with
`STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` — continue the existing `RDD_KEY_n`
numbering, do not restart). See `STATE_COMMON.md`'s lookup convention
(`grep -Fm1`, no `-A`).

| Key | Topic |
|---|---|
| RDD_KEY_179 | (**REVERSED by RDD_KEY_180** — no longer in effect) Language-selection mechanism — C++26 was made explicit-only via `--lang cpp26` / `lang=cpp26` |
| RDD_KEY_180 | **REVERSES RDD_KEY_179** — C++26 is not a separate selectable language; it is future incremental rule coverage on the existing `"cpp"` pipeline, same pattern as C++20 |
| RDD_KEY_181 | §1 pack indexing — call-containing index (`T...[ computeIndex() ]`) is loose per the ordinary `[]` tight/loose rule; fixed stale tight example |

---

## Config

No new config keys. §1–4 reuse existing STYLE.md/STYLE_CPP20.md logic with
no toggle-able behavior; §5 (Reflection) is still provisional and pending
the tokenizer validation pass, so it's premature to define config for it —
any knob added now would be speculation about a rule set that isn't
trusted yet. Revisit once §5 graduates out of draft status.

## Test Fixtures (External, corpus-scale)

For §5 Reflection tokenizer/rule validation (see Scope §5 and the Checklist
below), real-code testing against:
- `bloomberg/clang-p2996`
- `wrocpp/cpp26-reflection-examples`
- `simdjson/experimental_json_builder`
- `stephenberry/glaze`
- `ryanjk5.github.io/posts/rjk-duck` (blog post, not a repo — `^^`/`[:`/`:]`/
  `template for` examples covering vtable generation and member-pointer
  substitution; formatting-wise the examples are plain, no unusual
  whitespace/nesting, so it's useful as an extra real-world source but
  doesn't substitute for the repos above)

## Test Fixtures (Local)

Local dogfood pairs (distinct from the external-repo list above, which is for
corpus-scale reflection validation — see Scope §5 above) have been authored
and registered in `formatter/test/` — see `test/README.txt` for the pair
list and what each covers. The reflection pair
(`cpp26_reflection_inp/out.cpp`) was extracted ahead of its original
promotion gate (§5's external-repo tokenizer validation still pending), per
explicit instruction — see the "Done:" note on this file's checklist below
for that history.

**Promoted to active `make test` this session** (Makefile `INP_FILES`),
after the §5 padding rules landed and the fixture was run against the real
JAR to verify it. Two fixture-authoring issues surfaced and were fixed with
explicit user confirmation, both unrelated to the new §5 rules themselves
(every `^^`/`[[ ]]`/`[: :]` diff matched expected output with zero changes
needed):
- The input's body statements (`reflectMember`, `useSplice`,
  `checkReflected`) had zero leading indentation while the expected output
  assumed 4-space indentation — this formatter does not reindent
  (preserves source indentation verbatim, see `STATE_COMMON.md`'s
  Architectural TODOs), so the input needed to already be authored with
  correct indentation, same convention as `cpp26_core_inp.cpp`. Fixed by
  indenting the input's body statements.
- `auto v = [:r:];` gets column-aligned under `constexpr auto r = ^^int;`'s
  `=` by the pre-existing (unrelated, C/C++/Java job's) declaration-
  alignment rule — reproduced with plain non-reflection code
  (`constexpr auto r = 1; auto v = 2;`) to confirm it's not a §5-rule
  artifact. Fixed by updating the expected output to the real aligned
  column, since the alignment itself is correct, pre-existing behavior.
Still NOT cross-checked against the STYLE_CPP26.md §5 external-corpus
fixture repos (`bloomberg/clang-p2996` etc.) — only against this
formatter's own actual JAR output. That corpus-scale validation pass
remains open (see Scope §5 / Test Fixtures (External, corpus-scale) above).

---

## Class Scoping (post Core/Curly/Indent/Tags refactor)

No new classes. C++26 rule coverage lands entirely inside the existing
`isCpp`-gated curly-family classes: `TokenizerCurly.java`,
`FormatterCurly.java`, `ScopePipelineCurly.java`, `MiscRuleCurly.java`, and
`CppSpecificRule.java`. "Tokenizer support pass" in the Checklist below
means editing `TokenizerCurly.java` specifically (not `TokenizerCore.java`,
which is family-generic and has no C++-specific branching to extend).

## Open Questions

None recorded yet in this file. Note: `STYLE_CPP26.md` §5 (Reflection) is
explicitly marked provisional/draft pending a tokenizer validation pass —
this is a known gap in the style doc itself, not yet elevated to a
formal blocked Open Question here since real implementation hasn't started.

---

## Checklist

- [ ] Diff `STYLE_CPP26.md` against `STYLE_CPP20.md`/`STYLE_C_CPP.md` to
      enumerate exactly which rules are C++26-specific vs. already covered
      by existing C++ support (§1–3 look like they may already be
      structurally covered; §4/§5 look genuinely new — confirm by diffing,
      don't assume).
- [x] **Language-selection mechanism resolved (RDD_KEY_180, reversing
      RDD_KEY_179), this session.** C++26 is NOT a separate selectable
      language — no `Lang.isCpp26` flag, no `--lang cpp26`/`lang=cpp26`
      selector, no `SCAFFOLD_ONLY_LANGUAGES` entry. `.cpp`/`.hpp` files
      resolve to `"cpp"` exactly as before; C++26 rule coverage lands
      directly inside the existing `"cpp"`-gated pipeline
      (`CppSpecificRule.java` etc.) when implemented, matching how C++20
      was folded in with no separate selector. (An earlier session had
      introduced `--lang cpp26` as an explicit-only selector under
      RDD_KEY_179 — reverted this session as an unnecessary departure from
      that precedent.)
- [x] Tokenizer smoke check done (this session, via sub-agent, read-only —
      no source/state files touched). Confirmed via `TokenizerCurly.java`'s
      `MULTI_CHAR_OPS` array (lines 114-120): `^^`, `[:`, `:]` are absent, so
      they currently mis-split into single-char constituents (`^^` → `^`+`^`,
      `[:` → `[`+`:`, `:]` → `:`+`]`) — no crash, no swallowing. CLI run
      against a hand-written `/tmp` snippet confirmed this mis-split is
      actively **corrupting**, not just inert passthrough: `^^SomeType`
      (no-space input) rendered as `^ ^ SomeType` (space inserted between the
      two `^` tokens), and `[:r:]` vs. `[: computeRefl(x) :]` — the same
      splice-bracket construct — got inconsistent spacing (`[: r :]` vs.
      `[ : computeRefl(x) : ]`) depending on interior content, since `[`/`:`
      are each independently re-spaced by ordinary bracket/colon rules
      rather than treated as one splice-bracket unit. No secondary/cascading
      corruption found beyond this spacing inconsistency (no evidence of
      bracket-depth tracking itself going wrong and corrupting unrelated
      braces). Assessment: exactly the expected, contained finding this
      smoke check exists to catch — safe to proceed to the real tokenizer
      pass below whenever it's picked up; nothing here needs escalating
      beyond what that pass already targets.
- [x] Tokenizer support pass for §5 Reflection done: `TokenizerCurly.java`'s
      `MULTI_CHAR_OPS` array gained `"^^"`, `"[:"`, `":]"` entries (none is a
      strict prefix of any other existing entry, so no new ordering
      constraint among them). `":]"` reaches `emitOperator()` via the
      dispatch loop's existing default case (any unhandled leading char
      falls there) with no dispatch change needed; `"[:"` needed a new
      dispatch branch (`c == '[' && peek(1) == ':' && lang.isCpp`) placed
      before the generic open-bracket branch, since a leading `[` is
      otherwise always intercepted into `emitOpenBracket` before ever
      reaching `emitOperator()` — gated on `lang.isCpp` only (not shared
      with C/Java/Kotlin) since this dispatch branch changes tokenization
      behavior for every leading `[`, unlike the two `MULTI_CHAR_OPS`-only
      additions which are inert no-ops in practice for other languages
      (`^^`/`:]` as adjacent characters essentially never occur in real
      C/Java/Kotlin source). Verified fixed against the same `/tmp`
      smoke snippet from the prior smoke-check entry above: `^^SomeType` no
      longer splits into `^ ^ SomeType`; `[:r:]` and `[: computeRefl(x) :]`
      now render with the *same* consistent interior-padding pattern instead
      of diverging. `make test`: 102/102 forward + idempotency, zero
      regressions. Note: this is tokenizer support only — no tight/loose
      padding rule or `^^`-binds-tight-to-operand rule has been implemented
      for §5 yet (still deliberately provisional/draft per Scope §5; the
      snippet above still renders `^^ SomeType` with a space, and splice
      brackets still get generic loose padding regardless of content
      complexity) — that rule-implementation step remains a separate,
      not-yet-started piece of work, gated on this tokenizer pass being
      done, which it now is.
- [x] §5 tight/loose padding rules implemented this session, in
      `CppSpecificRule.java` + `FormatterCurly.java`'s Phase 4 block
      (`lang.isCpp`-gated), with one correction to STYLE_CPP26.md itself
      along the way. Before writing any code, empirically tested current
      JAR behavior on `[[assume(a>=0)]]`/`[[nodiscard]]` and found
      STYLE_CPP26.md §5's claim that splice-bracket padding "mirrors the
      existing JAR-verified `[[ assume(a >= 0) ]]` case" was **false** —
      `[[ ]]` attributes were left completely verbatim/unformatted, no
      tight/loose rule existed for them at all (confirmed with the user via
      `AskUserQuestion`: the coincidental appearance of interior padding in
      an earlier smoke test came from the unrelated generic paren-complexity
      rule firing on `assume(...)`'s own `(...)`, not from any
      attribute-aware logic). Per the user's explicit decision (implement
      both together if the precedent was coincidental, i.e. not truly
      pre-existing), added:
      - `CppSpecificRule.enforceAttributeAndSpliceBracketPadding` — new
        method handling both `[[ ]]` and `[: :]` with one shared
        implementation: forward-scans for matched OP-token pairs (a small
        stack keyed on exact open/close text so `[[`/`]]` and `[:`/`:]`
        pairs are never cross-matched), then for each pair reuses
        `ComplexityPaddingEvaluator.isLoose` unmodified on the interior's
        significant tokens to decide tight (no space) vs. loose (single
        space) — `isLoose` already only checks for nested PUNCT `(`/`[`
        tokens, and a call's own `(` is exactly that, so no OP-vs-PUNCT
        change to the evaluator was needed. Only the immediate boundary gap
        on each side is rewritten; everything else inside (e.g. the call's
        own interior spacing) is left verbatim to whatever other rule
        already governs it. Pairs spanning multiple physical lines,
        containing a comment, or touching a frozen token are skipped
        entirely (untouched), same guard posture as every other rewrite in
        this file.
      - `CppSpecificRule.enforceReflectionOperatorSpacing` — new method,
        same gap-buffering technique as `enforcePackIndexingSpacing`:
        collapses the gap after every `^^` OP token to zero width (subject
        to the same comment/newline/frozen guards), giving `^^SomeType` no
        space between the operator and its operand.
      Verified against hand-written snippets:
      `[[assume(a>=0)]]`/`[[ assume(a>=0) ]]` → `[[ assume(a>=0) ]]` (loose,
      call inside); `[[nodiscard]]`/`[[ nodiscard ]]` → `[[nodiscard]]`
      (tight, bare); `[:refl:]` stays tight; `[:computeRefl(x):]`/
      `[: computeRefl(x) :]` → `[: computeRefl(x) :]` (loose, call inside);
      `[:arr[0]:]` → `[: arr[0] :]` (loose, nested bracket); `^^SomeType`
      stays tight (no space introduced or removed). `make test`: 102/102
      forward + idempotency, zero regressions. `CppSpecificRule.enforcePackIndexingSpacing`
      (new method, called from `FormatterCurly`'s Phase 4 cosmetic-spacing
      block, `lang.isCpp`-gated), collapses the gaps on both sides of an
      `...` token whenever it is immediately followed by `[` (scoped to that
      exact adjacency only, so ordinary variadic `Args...)`/`Args...,` uses
      are untouched). `make test`: 101/101 forward + idempotency, zero
      regressions. §2 (`= delete("reason")`) and §3 (placeholder `_`)
      confirmed to need no new code — both already format correctly via
      existing ordinary call-argument/identifier handling (verified against
      `cpp26_core_inp/out.cpp`).
- [x] §4 Contracts implemented: `CppSpecificRule.enforceContractClausePlacement`
      (placement: `pre`/`post` are plain identifiers, not tokenizer keywords,
      so detection is positional — an identifier `pre`/`post` whose previous
      significant token is `)` (own or a preceding clause's) begins/continues
      a group; each clause's own `(...)` content is re-spaced as a plain
      expression via the new `spaceExpressionTokens` helper, with `post`'s
      top-level `:` split out for tight-before/space-after rendering) and
      `CppSpecificRule.enforceContractAssertSpacing` (reuses
      `spaceExpressionTokens` for `contract_assert(cond)`'s argument — decided
      this session to treat it as always-normalized despite the spec text's
      "like any other function-call-shaped statement" wording, since ordinary
      call-statement arguments are otherwise left verbatim elsewhere in this
      codebase; `STYLE_CPP26.md` updated to describe this explicitly).
      **Design decision (this session, user-confirmed):** a lone single
      clause follows overflow-based inline/wrap like `requires`; a group of
      **2+** clauses always wraps one-per-line regardless of fit — multiple
      contract clauses are always easier to read one per line.
      `STYLE_CPP26.md` §4 updated accordingly. Both new methods wired into
      `FormatterCurly` (contract clause placement in the Phase 1 `isCOrCpp`+
      `lang.isCpp` block right after `enforceRequiresClausePlacement`;
      contract_assert spacing in the Phase 4 cosmetic-spacing `lang.isCpp`
      block alongside `enforcePackIndexingSpacing`). `make test`: 101/101
      forward + idempotency, zero regressions, `cpp26_core_inp/out.cpp`
      promoted to active in the Makefile (uncommented from `INP_FILES`).
      `cpp26_comments_inp.cpp` deliberately NOT yet promoted — it exercises
      a comment sitting between the signature's `)` and the first `pre`
      clause, which `enforceContractClausePlacement`'s replaced span would
      currently silently drop (not yet handled); left as a known gap in the
      Makefile's comment for a future session.
- [x] `cpp26_comments` comment-drop gap fixed, this session.
      `enforceContractClausePlacement` now tracks each clause's own keyword
      token index (`clauseKeywordIdx`) and, for each clause, pulls any
      `COMMENT_LINE`/`COMMENT_BLOCK` tokens sitting in the gap before it
      (via new helper `collectComments`) out of the span about to be
      overwritten, re-inserting them on their own line at the clause's
      indent instead of silently dropping them. A multi-line block comment
      is reindented via the existing, previously-CSS/JSON-only
      `FormatterSimpleBraced.reindentBlockComment` (now also called from
      `CppSpecificRule.java`) so its continuation lines shift by the same
      amount as its first line. A leading comment now also forces the
      wrapped (one-clause-per-line) rendering even for an otherwise-inlinable
      lone clause, since there's nowhere sensible to put a comment on an
      inlined line.

      Verifying the fixture against the real JAR surfaced several *other*,
      unrelated mismatches between the fixture's expected output and actual
      behavior — none of them bugs in this fix, confirmed by reproducing
      each with plain non-C++26 snippets:
      - Blank lines the fixture inserted between consecutive `using`
        declarations — no such rule exists; fixed the expected output to
        match (no blank lines).
      - `if(init; cond) { ... }` staying K&R-brace (`) {` same line) is
        correct, established behavior (`cpp_core_out.cpp` already shows
        plain `if(...) {` the same way) — fixed the expected output, which
        had wrongly assumed Allman conversion for this case.
      - The input's `divide`/`clamp`/`process` function bodies lacked the
        4-space indentation the expected output assumed — this formatter
        doesn't reindent (see `STATE_COMMON.md`'s Architectural TODOs), so
        fixed the input to already carry the indentation, same convention
        as `cpp26_core_inp.cpp`/`cpp26_reflection_inp.cpp`.
      - **Investigated, not a bug:** structured bindings collapsing a
        trailing same-line comment's original gap to a single space
        (`auto [_, count] = getResult();  // comment` -> one space out),
        via `DeclarationAlignmentRuleCurly.java`'s Declaration-alignment
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
      `cpp26_comments_inp/out.cpp`, and `cpp26_reflection_inp/out.cpp` all
      extracted to `test/`, registered commented-out in the Makefile (real
      §1–4/§5 rule coverage not yet implemented), documented in
      `test/README.txt`. `cpp26_reflection`'s promotion gate (external-corpus
      cross-check for §5) was explicitly overridden per user instruction, since
      the pair is needed to seed the initial tokenizer test for `^^`/`[:`/`:]` --
      its expected output still isn't validated against that cross-check.
- [ ] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      this file's "Test Fixtures (External, corpus-scale)" list, once §5
      is implemented.
