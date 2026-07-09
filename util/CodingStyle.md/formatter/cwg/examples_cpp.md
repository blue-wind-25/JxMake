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
