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

Rows 16-19 added 2026-07-31 (STATE_AI.md's "extend classifier_weights" session), covering a few
`KEYWORDS_CPP` members that previously had zero example rows in this file.

| 16 | `class of bugs this pattern eliminates is null dereferences` | no | no | no | YES | "class" used loosely as a noun ("the class of bugs"), prose. |
| 17 | `class Widget final : public Base {};` | no | yes | no | NO | Declaration restated, semicolon fires. |
| 18 | `private members should not be exposed through this accessor` | no | no | no | NO | "private" naming the actual access-specifier keyword being described, zero mechanical feature fires — same false-friend shape as rows 11-15. |
| 19 | `this pointer is invalid after the object is destroyed` | no | no | no | NO | "this" referring to the language's actual `this` pointer, a code reference despite zero mechanical feature — not the English demonstrative pronoun opening ordinary prose. |

Rows 20-27 added 2026-08-01 (STATE_AI.md's "grow hand-labeled hard-case set" session), covering
`KEYWORDS_CPP` members that previously had zero example rows in this file: `catch`, `override`,
`public`, `protected`.

| 20 | `catch me if you can, this race condition is rare` | no | no | no | YES | "catch" as an ordinary English verb ("catch me"), prose. |
| 21 | `catch block here intentionally swallows the exception` | no | no | no | NO | "catch" naming the actual exception-handling construct being described, code reference despite zero mechanical feature. |
| 22 | `override my objection, the client insisted on this API shape` | no | no | no | YES | "override" as an ordinary English verb, prose. |
| 23 | `override here changes the default rounding behavior` | no | no | no | NO | "override" naming the actual virtual-function override being described, code reference despite zero mechanical feature. |
| 24 | `public opinion on this API design keeps shifting` | no | no | no | YES | "public" used as an ordinary noun ("public opinion"), prose. |
| 25 | `public members here are intentionally minimal` | no | no | no | NO | "public" naming the actual access-specifier keyword being described, code reference despite zero mechanical feature. |
| 26 | `protected under the new policy, this endpoint requires auth` | no | no | no | YES | "protected" used as an ordinary adjective ("protected under..."), prose. |
| 27 | `protected members here are visible to every derived class` | no | no | no | NO | "protected" naming the actual access-specifier keyword being described, code reference despite zero mechanical feature. |
