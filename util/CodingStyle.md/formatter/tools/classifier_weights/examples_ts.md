# TS-only keyword-ambiguity examples (KEYWORDS_TS set)

Same relationship as `examples_cpp.md` to `examples_c.md`: TS files are gated through
`KEYWORDS_JS || KEYWORDS_TS` in `KeywordAmbiguityGate.hasLeadingKeywordMatch`, so these are the
TS-specific additions on top of `examples_js.md`'s shared JS keyword cases — types/interfaces/
modifiers that don't exist as keywords in plain JS. Same feature definitions as `examples_js.md`
(paren/semi/url-or-number, no arrow column).

| # | Comment text | paren? | semi? | url/num? | Label | Why |
|---|---|---|---|---|---|---|
| 1 | `interface between the two subsystems is documented separately` | no | no | no | YES | "interface" as an ordinary noun ("the interface between..."), prose. |
| 2 | `interface UserRecord { id: string; name: string; }` | no | yes | no | NO | Real type declaration, semicolons fire. |
| 3 | `type of error this throws depends on the input` | no | no | no | YES | "type" as an ordinary noun ("the type of error"), prose. |
| 4 | `type UserId = string;` | no | yes | no | NO | Type-alias declaration. |
| 5 | `readonly access is enforced by convention, not the compiler, in this legacy file` | no | no | no | YES | "readonly" used loosely as an adjective in a sentence, prose. |
| 6 | `readonly items: ReadonlyArray<string>;` | no | yes | no | NO | Field declaration, semicolon fires. |
| 7 | `private conversation aside, the real bug is elsewhere` | no | no | no | YES | "private" as an ordinary adjective ("private conversation"), classic false-friend case. |
| 8 | `private constructor() {}` | yes | no | no | NO | Constructor declaration, paren fires. |
| 9 | `enum of possible states is defined below` | no | no | no | YES | "enum" used loosely as a noun in a sentence, prose. |
| 10 | `enum Status { Active, Inactive }` | no | no | no | NO | Declaration-shaped but zero mechanical feature fires — same zero-signal shape `examples_c.md` rows 13-17 exist for; "enum" naming the actual construct. |
| 11 | `namespace pollution is a real risk with this pattern` | no | no | no | YES | "namespace" used loosely as a noun, prose. |
| 12 | `namespace Utils { export function clamp(x: number) {} }` | yes | no | no | NO | Namespace declaration, paren fires. |
| 13 | `unknown factors could explain the flaky test` | no | no | no | YES | "unknown" as an ordinary adjective ("unknown factors"), prose. |
| 14 | `unknown extends Record<string, unknown> ? true : false;` | no | yes | no | NO | Conditional-type expression, semicolon fires. |
| 15 | `abstract enough that every subclass must fill in the details` | no | no | no | YES | "abstract" used loosely as an adjective in a sentence, prose. |
| 16 | `abstract class Shape { abstract area(): number; }` | yes | no | no | NO | Class declaration, paren fires from `area(`. |
| 17 | `as usual, the edge case is the empty array` | no | no | no | YES | "as" as an ordinary English conjunction ("as usual"), prose. |
| 18 | `as unknown as UserRecord;` | no | yes | no | NO | Type-assertion chain, semicolon fires. |

Rows 19-24 added 2026-07-31 (re-derivation follow-up, same session): same rebalancing motivation
as `examples_js.md`'s rows 19-24 — this file's zero-mechanical-feature rows skewed heavily YES
(9 zero-signal YES vs. only 1 zero-signal NO, `#10`), which contributed to `KEYWORD_BIAS` flipping
positive on re-derivation. These add zero-signal NO false-friend cases for keywords not yet
covered by one in this file.

| 19 | `type parameter here is inferred by the compiler, not written explicitly` | no | no | no | NO | "type" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 20 | `readonly modifier prevents reassignment after construction` | no | no | no | NO | "readonly" naming the actual modifier keyword. |
| 21 | `private helper lives below the public API surface of this class` | no | no | no | NO | "private" naming the actual access modifier, not the English adjective. |
| 22 | `namespace groups these utility functions under one qualified name` | no | no | no | NO | "namespace" naming the actual declaration keyword being described. |
| 23 | `unknown here forces an explicit narrowing check before use` | no | no | no | NO | "unknown" naming the actual type keyword, not the English adjective. |
| 24 | `abstract base class defines the shared contract for every shape` | no | no | no | NO | "abstract" naming the actual class modifier, code reference despite zero mechanical feature. |

Rows 25-32 added 2026-08-01 (STATE_AI.md's "grow hand-labeled hard-case set" session), covering
`KEYWORDS_TS` members that previously had zero example rows in this file: `any`, `never`,
`number`, `public`.

| 25 | `any concerns about this API shape should go in the review thread` | no | no | no | YES | "any" as an ordinary English adjective ("any concerns"), prose. |
| 26 | `any here widens the type and defeats the point of this check` | no | no | no | NO | "any" naming the actual escape-hatch type keyword being described, code reference despite zero mechanical feature. |
| 27 | `never mind the extra logging, it's only on in debug builds` | no | no | no | YES | "never" as an ordinary English adverb ("never mind"), prose. |
| 28 | `never here signals this branch is unreachable by construction` | no | no | no | NO | "never" naming the actual bottom-type keyword being described, code reference despite zero mechanical feature. |
| 29 | `number of retries here was picked arbitrarily and should be tuned` | no | no | no | YES | "number" as an ordinary English noun ("number of retries"), prose. |
| 30 | `number here widens too easily and should probably be a literal type` | no | no | no | NO | "number" naming the actual primitive type keyword being described, code reference despite zero mechanical feature. |
| 31 | `public opinion on this API design keeps shifting` | no | no | no | YES | "public" used as an ordinary noun ("public opinion"), prose. |
| 32 | `public members here are intentionally minimal` | no | no | no | NO | "public" naming the actual access modifier being described, code reference despite zero mechanical feature. |

Rows 33-40 added 2026-08-01 (second corpus-growth pass this session), covering `KEYWORDS_TS`
members that still had zero example rows after rows 25-32 landed: `declare`, `is`, `protected`,
`string`.

| 33 | `declare victory too early and the regression tests will bite you` | no | no | no | YES | "declare" in the ordinary English idiom ("declare victory"), prose. |
| 34 | `declare statement here only exists to satisfy the ambient type checker` | no | no | no | NO | "declare" naming the actual ambient-declaration keyword being described, code reference despite zero mechanical feature. |
| 35 | `is this actually safe to call before the module finishes loading?` | no | no | no | YES | "is" as an ordinary English verb opening a question, prose. |
| 36 | `is check here narrows the union before the property access below` | no | no | no | NO | "is" naming the actual type-guard keyword being described, code reference despite zero mechanical feature. |
| 37 | `protected against every edge case we could think of during review` | no | no | no | YES | "protected" used as an ordinary English verb, prose. |
| 38 | `protected member here is only meant to be used by subclasses` | no | no | no | NO | "protected" naming the actual access modifier being described, code reference despite zero mechanical feature. |
| 39 | `string together a few small fixes before the next release` | no | no | no | YES | "string" in the ordinary English idiom ("string together"), prose. |
| 40 | `string here should really be a template literal type instead` | no | no | no | NO | "string" naming the actual primitive type keyword being described, code reference despite zero mechanical feature. |
