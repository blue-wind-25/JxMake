# STATE_CPP26.md — C++26 JAR Support Tracker

Read `STATE_COMMON.md` first — it has the shared commit/ambiguity/testing
conventions this file assumes. `STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` (the
other jobs' files) are NOT required reading for this one — only
`STATE_COMMON.md` is.

---

## Purpose

Tracks implementation of C++26 support in the deterministic JAR formatter
(`util/CodingStyle.md/formatter/`), per `STYLE_CPP26.md` (which itself builds
on `STYLE.md`, `STYLE_C_CPP.md`, and `STYLE_CPP20.md`). **Current status is
scaffold-only: dispatch exists only as a "not yet implemented" error thrown
for C++26 constructs, no real formatting logic exists yet.**

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
7. Test fixtures — planned pairs live in `FUTURE_TEST_FIXTURES.md`'s "CPP26"
   section (not yet moved here). See "Test Fixtures (Local)" below.

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

---

## Config

No new config keys. §1–4 reuse existing STYLE.md/STYLE_CPP20.md logic with
no toggle-able behavior; §5 (Reflection) is still provisional and pending
the tokenizer validation pass, so it's premature to define config for it —
any knob added now would be speculation about a rule set that isn't
trusted yet. Revisit once §5 graduates out of draft status.

## Test Fixtures (Local)

Planned local dogfood pairs (distinct from §5's external-repo list, which
is for corpus-scale reflection validation — see Scope §5 above) are staged
in **FUTURE_TEST_FIXTURES.md**, under its "CPP26" section — not duplicated
here. See that file for the pair list and what each covers. Its reflection
pair (`cpp_26_reflection_inp/out.cpp`) is sequenced *after* §5's
external-repo tokenizer validation, not alongside the other two pairs — see
that file for why. Once authored, register pairs in the Makefile's
`INP_FILES` / `test/README.txt`, and empty out FUTURE_TEST_FIXTURES.md's
"CPP26" section accordingly.

---

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
- [ ] Before the tokenizer support pass below: run the tokenizer against a
      small hand-written local `^^`/`[:`/`:]` snippet (not a real fixture,
      no expected-output pair — just enough source to see whether it
      crashes, mis-splits the tokens, or silently swallows them). This is a
      cheap smoke check to catch gross tokenizer breakage before spending
      time on the full external-repo pass below, not a substitute for it.
- [ ] Tokenizer support pass for §5 Reflection (`^^`, `[:`, `:]`) — new
      `MULTI_CHAR_OPS` entries, longest-prefix-first ordering, full
      existing C/C++/Java/Kotlin regression suite re-run for zero
      regressions, before trusting any §5 rule.
- [ ] Implement §1–4 rule-by-rule, each its own checkpoint commit, per
      `STATE_COMMON.md`'s workflow.
- [ ] Author local test fixture pairs per `FUTURE_TEST_FIXTURES.md`'s
      "CPP26" section (reflection pair sequenced after the §5 tokenizer
      validation pass, per that section's own note) and register in the
      Makefile's `INP_FILES` / `test/README.txt`.
- [ ] Real-code testing pass per `STATE_COMMON.md`'s methodology against
      `STYLE_CPP26.md` §5's listed test-fixture repos
      (`bloomberg/clang-p2996`, `wrocpp/cpp26-reflection-examples`,
      `simdjson/experimental_json_builder`, `stephenberry/glaze`,
      `ryanjk5.github.io/posts/rjk-duck`) once §5 is implemented.
