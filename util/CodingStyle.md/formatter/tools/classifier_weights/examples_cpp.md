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

Rows 28-35 added 2026-08-01 (second corpus-growth pass this session), covering `KEYWORDS_CPP`
members that still had zero example rows after rows 20-27 landed: `friend`, `throw`, `try`,
`using`.

| 28 | `friend of mine hit the exact same crash last week` | no | no | no | YES | "friend" as an ordinary English noun, prose. |
| 29 | `friend declaration here grants access to the private internals` | no | no | no | NO | "friend" naming the actual friend-declaration keyword being described, code reference despite zero mechanical feature. |
| 30 | `throw in a fallback here just in case the primary call fails` | no | no | no | YES | "throw" in the ordinary English idiom ("throw in"), prose. |
| 31 | `throw specification here was removed in C++11 and later` | no | no | no | NO | "throw" naming the actual (now-removed) throw-specification keyword being described, code reference despite zero mechanical feature. |
| 32 | `try to reproduce this on a clean checkout before filing a bug` | no | no | no | YES | "try" as an ordinary English verb, prose. |
| 33 | `try block here only wraps the risky allocation, not the whole function` | no | no | no | NO | "try" naming the actual try-block construct being described, code reference despite zero mechanical feature. |
| 34 | `using a workaround for now until the real fix lands upstream` | no | no | no | YES | "using" as an ordinary English verb ("using a workaround"), prose. |
| 35 | `using declaration here pulls the base class overloads into scope` | no | no | no | NO | "using" naming the actual using-declaration keyword being described, code reference despite zero mechanical feature. |

Rows 36-90 added 2026-08-02 (GRU misclassification-driven corpus growth, same session as
`examples_c.md`'s rows 38-76 — see that file's note and `/tmp/gru_misclassified.txt`). Rows 36-57
add paraphrased variants of the keywords the GRU model got wrong on this exact zero-feature NO
shape (`explicit`, `new`, `true`, `static`, `consteval`, `virtual`, `private`, `this`, `override`,
`throw`, `using`); rows 58-90 cover `KEYWORDS_CPP` members that still had zero example rows in
this file.

| 36 | `explicit constructor here blocks accidental implicit conversions` | no | no | no | NO | "explicit" naming the actual constructor specifier being described, code reference despite zero mechanical feature. |
| 37 | `explicit conversion operator here was added to avoid ambiguous overload resolution` | no | no | no | NO | "explicit" naming the actual keyword being described, not the English adjective. |
| 38 | `new here always goes through the overridden allocator for this class` | no | no | no | NO | "new" naming the actual allocation keyword being described, code reference despite zero mechanical feature. |
| 39 | `new expression here is paired with a matching delete further down` | no | no | no | NO | "new" naming the actual expression keyword being described, not the English adjective. |
| 40 | `true branch here is the common case and should stay first` | no | no | no | NO | "true" naming the actual boolean literal being described, code reference despite zero mechanical feature. |
| 41 | `true here is the default when the flag is left unset` | no | no | no | NO | "true" naming the actual literal value being described, not the English adjective. |
| 42 | `static member here is shared across every instance of the class` | no | no | no | NO | "static" naming the actual member-storage keyword being described, code reference despite zero mechanical feature. |
| 43 | `static assertion here only runs at compile time, not runtime` | no | no | no | NO | "static" naming the actual `static_assert` family keyword being described, not an English adjective. |
| 44 | `consteval function here can only ever run at compile time` | no | no | no | NO | "consteval" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 45 | `consteval here is stricter than constexpr since it forbids runtime evaluation` | no | no | no | NO | "consteval" naming the actual keyword being contrasted, not English prose. |
| 46 | `virtual destructor here prevents undefined behavior on polymorphic deletion` | no | no | no | NO | "virtual" naming the actual destructor specifier being described, code reference despite zero mechanical feature. |
| 47 | `virtual dispatch here is resolved through the vtable at runtime` | no | no | no | NO | "virtual" naming the actual dispatch mechanism being described, not an English adjective. |
| 48 | `private inheritance here hides the base class's public interface` | no | no | no | NO | "private" naming the actual inheritance specifier being described, code reference despite zero mechanical feature. |
| 49 | `private constructor here forces callers through the factory method instead` | no | no | no | NO | "private" naming the actual access specifier being described, not the English adjective. |
| 50 | `this reference here is captured by the lambda for later use` | no | no | no | NO | "this" referring to the language's actual `this` pointer, code reference despite zero mechanical feature. |
| 51 | `this parameter here is implicitly passed to every member function` | no | no | no | NO | "this" naming the actual implicit parameter being described, not the English demonstrative. |
| 52 | `override here is required because the base method is pure virtual` | no | no | no | NO | "override" naming the actual specifier being described, code reference despite zero mechanical feature. |
| 53 | `override specifier here catches a signature mismatch at compile time` | no | no | no | NO | "override" naming the actual keyword being described, not the English verb. |
| 54 | `throw expression here propagates the error up to the caller` | no | no | no | NO | "throw" naming the actual expression keyword being described, code reference despite zero mechanical feature. |
| 55 | `throw here is only reached if the precondition check fails` | no | no | no | NO | "throw" naming the actual keyword being described, not the English verb. |
| 56 | `using alias here shortens the long template instantiation below` | no | no | no | NO | "using" naming the actual alias-declaration keyword being described, code reference despite zero mechanical feature. |
| 57 | `using namespace here is scoped to this function only, not the whole file` | no | no | no | NO | "using" naming the actual directive keyword being described, not the English verb. |
| 58 | `alignas here forces this buffer onto a cache-line boundary` | no | no | no | NO | "alignas" naming the actual alignment specifier being described, code reference despite zero mechanical feature. |
| 59 | `alignof here reports the alignment requirement, not the size` | no | no | no | NO | "alignof" naming the actual operator being described, code reference despite zero mechanical feature. |
| 60 | `asm block here drops into raw assembly for the hot loop` | no | no | no | NO | "asm" naming the actual inline-assembly keyword being described, code reference despite zero mechanical feature. |
| 61 | `bool here is stored as a full byte, not a single bit` | no | no | no | NO | "bool" naming the actual type keyword being described, not an English noun. |
| 62 | `char16_t here represents a single UTF-16 code unit, not a full character` | no | no | no | NO | "char16_t" naming the actual character type being described, code reference despite zero mechanical feature. |
| 63 | `char32_t here always holds one full Unicode code point` | no | no | no | NO | "char32_t" naming the actual character type being described, code reference despite zero mechanical feature. |
| 64 | `class here intentionally has no public constructor` | no | no | no | NO | "class" naming the actual type keyword being described, code reference despite zero mechanical feature. |
| 65 | `co_await here suspends the coroutine until the future resolves` | no | no | no | NO | "co_await" naming the actual coroutine keyword being described, code reference despite zero mechanical feature. |
| 66 | `co_return here ends the coroutine without producing a value` | no | no | no | NO | "co_return" naming the actual coroutine keyword being described, code reference despite zero mechanical feature. |
| 67 | `co_yield here hands control back to the caller with a value` | no | no | no | NO | "co_yield" naming the actual coroutine keyword being described, code reference despite zero mechanical feature. |
| 68 | `concept here constrains the template parameter to arithmetic types only` | no | no | no | NO | "concept" naming the actual C++20 keyword being described, code reference despite zero mechanical feature. |
| 69 | `constexpr function here can run at either compile time or runtime` | no | no | no | NO | "constexpr" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 70 | `constinit here guarantees static initialization, not dynamic` | no | no | no | NO | "constinit" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 71 | `const_cast here strips constness that the API never should have added` | no | no | no | NO | "const_cast" naming the actual cast keyword being described, code reference despite zero mechanical feature. |
| 72 | `decltype here deduces the exact return type of the expression` | no | no | no | NO | "decltype" naming the actual operator being described, code reference despite zero mechanical feature. |
| 73 | `delete here marks this overload as explicitly unusable` | no | no | no | NO | "delete" naming the actual specifier keyword being described, code reference despite zero mechanical feature. |
| 74 | `dynamic_cast here safely downcasts and returns null on failure` | no | no | no | NO | "dynamic_cast" naming the actual cast keyword being described, code reference despite zero mechanical feature. |
| 75 | `export here was part of the old, now-removed template export model` | no | no | no | NO | "export" naming the actual (largely obsolete) keyword being described, code reference despite zero mechanical feature. |
| 76 | `false here is the safe default until the feature flag flips` | no | no | no | NO | "false" naming the actual boolean literal being described, not an English adjective. |
| 77 | `mutable here lets this member change even inside a const method` | no | no | no | NO | "mutable" naming the actual specifier keyword being described, code reference despite zero mechanical feature. |
| 78 | `namespace here groups every helper used only within this translation unit` | no | no | no | NO | "namespace" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 79 | `noexcept here promises the caller this function never throws` | no | no | no | NO | "noexcept" naming the actual specifier keyword being described, code reference despite zero mechanical feature. |
| 80 | `nullptr here replaces the old NULL macro throughout this file` | no | no | no | NO | "nullptr" naming the actual literal keyword being described, code reference despite zero mechanical feature. |
| 81 | `operator here is overloaded so this class can be compared directly` | no | no | no | NO | "operator" naming the actual overload keyword being described, code reference despite zero mechanical feature. |
| 82 | `reinterpret_cast here reinterprets the raw bytes without any conversion` | no | no | no | NO | "reinterpret_cast" naming the actual cast keyword being described, code reference despite zero mechanical feature. |
| 83 | `requires clause here constrains this template to numeric types` | no | no | no | NO | "requires" naming the actual constraint keyword being described, code reference despite zero mechanical feature. |
| 84 | `static_assert here fails the build if the assumption ever breaks` | no | no | no | NO | "static_assert" naming the actual compile-time check keyword being described, code reference despite zero mechanical feature. |
| 85 | `static_cast here narrows the value without any runtime check` | no | no | no | NO | "static_cast" naming the actual cast keyword being described, code reference despite zero mechanical feature. |
| 86 | `template parameter here defaults to the platform's native allocator` | no | no | no | NO | "template" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 87 | `thread_local here gives every thread its own independent copy` | no | no | no | NO | "thread_local" naming the actual storage-duration keyword being described, code reference despite zero mechanical feature. |
| 88 | `typeid here returns the actual runtime type, not the static one` | no | no | no | NO | "typeid" naming the actual operator being described, code reference despite zero mechanical feature. |
| 89 | `typename here disambiguates a dependent name inside the template` | no | no | no | NO | "typename" naming the actual disambiguation keyword being described, code reference despite zero mechanical feature. |
| 90 | `wchar_t here is platform-dependent and best avoided in portable code` | no | no | no | NO | "wchar_t" naming the actual wide-character type being described, code reference despite zero mechanical feature. |
