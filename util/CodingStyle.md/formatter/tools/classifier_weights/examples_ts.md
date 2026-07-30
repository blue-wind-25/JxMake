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
