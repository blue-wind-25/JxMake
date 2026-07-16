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
   its rules aren't trusted.
7. Test fixtures — planned pairs live in `FUTURE_TEST_FIXTURES.md`'s "CPP26"
   section (not yet moved here).

No `src/` files yet — scaffold dispatch lives in the shared
`Lang.java`/`Main.java`/`ServerMode.java`/`Config.java`, described in the
routing `CLAUDE.md` table; this job's own rule classes (a future
`CppSpecificRule.java` extension or new C++26-specific class) do not exist
yet.

---

## Resolved Design Decisions

Full text of each decision lives in `RDD_LOG.md` (shared with
`STATE_C_CPP_JAVA.md`/`STATE_KOTLIN.md` — continue the existing `RDD_KEY_n`
numbering, do not restart). See `STATE_COMMON.md`'s lookup convention
(`grep -Fm1`, no `-A`).

| Key | Topic |
|---|---|
| RDD_KEY_179 | Language-selection mechanism — C++26 is explicit-only via `--lang cpp26` / `lang=cpp26` (never auto-`Lang.infer`red from `.cpp`/`.hpp`, which stay `"cpp"` as before) |

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
- [x] **Language-selection mechanism resolved (RDD_KEY_179), this session.**
      Both C++26 and C++20 (and earlier) share the same `.cpp`/`.hpp`
      extensions, so `Lang.infer` deliberately never returns `"cpp26"` —
      extension-based detection still resolves to `"cpp"` (C++20/17
      baseline) exactly as before, unchanged. C++26 mode is explicit-only:
      `--lang cpp26` (CLI, extends the existing `--lang` escape valve
      already used for other extension ambiguities, e.g. `.h`) or
      `lang=cpp26` (server query param). Both currently dispatch straight to
      `UnsupportedLanguageException` (scaffold-only), verified via
      `./code-formatter.sh --standalone --lang cpp26 --diff <file>.cpp`
      smoke test.
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
      `simdjson/experimental_json_builder`, `stephenberry/glaze`) once §5
      is implemented.
