# C++-only keyword-ambiguity examples (KEYWORDS_CPP set)

Same feature definitions as `examples_c.md`; C++ files are gated through `KEYWORDS_C ||
KEYWORDS_CPP` in `KeywordAmbiguityGate.hasLeadingKeywordMatch`, so these are the C++-specific
additions on top of that file's shared C keyword cases.

| # | Comment text | paren? | semi? | url/num? | Label | Why |
|---|---|---|---|---|---|---|
| 1 | `final review before merging this branch` | no | no | no | YES | "final" as an English adjective, ordinary prose. |
| 2 | `final int x = compute();` | yes | yes | no | NO | Both strong negative signals present; clearly code-shaped. |
| 3 | `explicit about what this function does not guarantee` | no | no | no | YES | "explicit" as an adjective, prose. |
| 4 | `explicit(bool) constructors need C++20` | yes | no | yes | NO | Paren directly after target word — signature-shaped, plus version number reinforces code context. |
| 5 | `new behavior added in this release` | no | no | no | YES | "new" as an adjective ("new behavior"), prose. |
| 6 | `new Foo(); // leaked, see TODO` | yes | yes | no | NO | Textbook code-in-comment case; both paren and semicolon fire. |
| 7 | `virtual functions add vtable overhead here` | no | no | no | YES | Prose explaining a design tradeoff. |
| 8 | `virtual ~Base();` | yes | yes | no | NO | Declaration fragment. |
| 9 | `true story, this bug took three days to find` | no | no | no | YES | "true" as a colloquial English intensifier, prose (deliberately included as a case a keyword-blind rule would get wrong but the classifier's job is exactly this ambiguity). |
| 10 | `true == 1 in this codebase's convention` | no | no | yes | NO | Comparison-shaped statement; the digit and `==` pattern read as code even without a semicolon (url/num feature catches the `1`). |
| 11 | `static operator()` | no | no | no | NO | Real regression, `test/cpp_modern_inp.cpp` — names an actual `static operator()` overload, not adjective prose; zero mechanical feature fires. Added 2026-07-30, see `STATE_AI.md`'s 2026-07-30 section. |
| 12 | `consteval utility` | no | no | no | NO | Real regression, `test/cpp_combined_inp.cpp` — labels a `consteval`-tagged helper, code reference despite no feature signal. |
| 13 | `consteval and constinit` | no | no | no | NO | Real regression, `test/cpp_combined_inp.cpp` — section-banner comment naming two keywords being demonstrated below it, not prose. |
| 14 | `final specifier here prevents further inheritance` | no | no | no | NO | "final" naming the actual specifier being explained, code reference despite zero mechanical feature. |
| 15 | `virtual keyword adds a vtable pointer` | no | no | no | NO | "virtual" naming the actual keyword being explained. |
