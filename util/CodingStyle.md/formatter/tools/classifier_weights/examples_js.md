# JS/TS shared keyword-ambiguity examples (KEYWORDS_JS set)

Added 2026-07-31 (`STATE_AI.md`'s "extend classifier_weights" session): JS/TS route through the
same curly-brace `MiscRuleCurly.enforceCommentStyle` call path as C/C++/Java/Kotlin (`Lang
.isCurly` includes `isJs`/`isTs`), but `KeywordAmbiguityGate.hasLeadingKeywordMatch` previously
had no JS-specific branch at all and silently fell through to the wrong `KEYWORDS_C` default —
JS shares almost no keywords with C (`static`/`switch`/`typedef`/... aren't JS's real false-friend
surface; `function`/`const`/`let`/`yield`/`await`/... are). Same feature definitions as
`examples_c.md` (paren/semi/url-or-number; JS/TS have no `->` operator, so no arrow column).

| # | Comment text | paren? | semi? | url/num? | Label | Why |
|---|---|---|---|---|---|---|
| 1 | `let me know if this needs a second review` | no | no | no | YES | "let" as an ordinary English verb ("let me know"), prose. |
| 2 | `let cache = new Map();` | yes | yes | no | NO | Declaration statement, both signals fire. |
| 3 | `const environment before you deploy this` | no | no | no | YES | "const" used loosely as a verb-ish imperative in a sentence (informal but real English usage in review comments), prose. |
| 4 | `const MAX_RETRIES = 3;` | no | yes | yes | NO | Declaration with a numeric literal and semicolon. |
| 5 | `yield to the caller before continuing the loop` | no | no | no | YES | "yield" as an English verb ("yield to"), prose. |
| 6 | `yield fetchData();` | yes | yes | no | NO | Generator-function statement, both signals fire. |
| 7 | `async work happens off the main thread here` | no | no | no | YES | "async" as an adjective, prose. |
| 8 | `async function loadUser(id) { return db.get(id); }` | yes | yes | no | NO | Real function declaration; paren/semi both fire. |
| 9 | `class of problems this solves is input validation` | no | no | no | YES | "class" as an ordinary noun ("class of problems"), prose false-friend case. |
| 10 | `class Widget extends Base {}` | no | no | no | NO | Declaration-shaped, but note zero mechanical feature fires here (no paren/semi/url-num) — this is exactly the zero-signal shape `examples_c.md`'s rows 13-17 exist to cover; "class" naming the actual construct, not an English noun. |
| 11 | `static helper functions live in this module` | no | no | no | NO | "static" naming the real property-modifier keyword being described, code reference despite zero mechanical feature — same false-friend shape as the C/C++/Java sets, confirming JS needs its own zero-feature NO coverage too, not just borrowed C rows. |
| 12 | `default export pattern used throughout this repo` | no | no | no | NO | "default" naming the actual `export default` construct being described. |
| 13 | `new behavior added for empty arrays` | no | no | no | YES | "new" as an adjective ("new behavior"), prose. |
| 14 | `new Promise((resolve, reject) => { resolve(1); });` | yes | yes | yes | NO | Constructor call, all three signals fire. |
| 15 | `in this case the callback fires twice` | no | no | no | YES | "in" as an ordinary preposition opening a sentence, prose. |
| 16 | `in obj checks own and inherited keys` | no | no | no | NO | "in" naming the actual `in` operator being explained, code reference despite zero mechanical feature. |
| 17 | `void of any side effects, this helper is pure` | no | no | no | YES | "void" used as the English adjective ("void of"), mirrors `examples_c.md` row 11's false-friend shape. |
| 18 | `void 0 is the classic undefined idiom` | no | no | yes | NO | "void" naming the actual `void` operator idiom; the digit `0` fires the url/num feature. |

Rows 19-24 added 2026-07-31 (re-derivation follow-up, same session): the zero-mechanical-feature
rows above skew heavily YES (8 zero-signal YES vs. only 4 zero-signal NO), which flipped
`KEYWORD_BIAS` back positive when re-deriving weights across all languages together — reopening
the exact regression `examples_c.md` rows 13-17 exist to prevent (see that file's note and
`STATE_AI.md`'s 2026-07-30 section). These add more zero-signal NO false-friend cases across
keywords not yet covered by a zero-signal NO row in this file, to rebalance.

| 19 | `function keyword declares this the traditional way, not as an arrow` | no | no | no | NO | "function" naming the actual declaration keyword being contrasted, code reference despite zero mechanical feature. |
| 20 | `let binding shadows the outer variable of the same name` | no | no | no | NO | "let" naming the actual declaration keyword, not the English verb "let". |
| 21 | `const binding here is never reassigned after this point` | no | no | no | NO | "const" naming the actual declaration keyword being described. |
| 22 | `yield keyword pauses the generator until resumed` | no | no | no | NO | "yield" naming the actual keyword, code reference despite zero mechanical feature. |
| 23 | `import statement pulls in the utility module used below` | no | no | no | NO | "import" naming the actual statement being described. |
| 24 | `export keyword makes this function part of the public API` | no | no | no | NO | "export" naming the actual keyword, not an English verb. |

Rows 25-32 added 2026-08-01 (STATE_AI.md's "grow hand-labeled hard-case set" session), covering
`KEYWORDS_JS` members that previously had zero example rows in this file: `case`, `delete`,
`throw`, `while`.

| 25 | `case in point, this hack has outlived three rewrites` | no | no | no | YES | "case" as an ordinary English noun ("case in point"), prose. |
| 26 | `case label here falls through to the default on purpose` | no | no | no | NO | "case" naming the actual `switch`/`case` label being described, code reference despite zero mechanical feature. |
| 27 | `delete this comment once the workaround is no longer needed` | no | no | no | YES | "delete" as an ordinary English verb, prose. |
| 28 | `delete here only removes the key, not the underlying value` | no | no | no | NO | "delete" naming the actual `delete` operator being described, code reference despite zero mechanical feature. |
| 29 | `throw caution to the wind and ship this on a Friday` | no | no | no | YES | "throw" as an ordinary English verb ("throw caution to the wind"), prose. |
| 30 | `throw here only fires once the retry budget is exhausted` | no | no | no | NO | "throw" naming the actual exception-raising keyword being described, code reference despite zero mechanical feature. |
| 31 | `while we're at it, let's also clean up the imports` | no | no | no | YES | "while" as an ordinary English conjunction ("while we're at it"), prose. |
| 32 | `while loop here intentionally spins until the flag flips` | no | no | no | NO | "while" naming the actual loop construct being described, code reference despite zero mechanical feature. |

Rows 33-40 added 2026-08-01 (second corpus-growth pass this session), covering `KEYWORDS_JS`
members that still had zero example rows after rows 25-32 landed: `break`, `catch`, `if`,
`return`.

| 33 | `break it to them gently, this API is going away next release` | no | no | no | YES | "break" in the ordinary English idiom ("break it to them"), prose. |
| 34 | `break here only exits the switch, not the surrounding loop` | no | no | no | NO | "break" naming the actual break statement being described, code reference despite zero mechanical feature. |
| 35 | `catch you later, this refactor can wait until next sprint` | no | no | no | YES | "catch" in the ordinary English idiom ("catch you later"), prose. |
| 36 | `catch block here only handles the network-timeout case` | no | no | no | NO | "catch" naming the actual catch-block construct being described, code reference despite zero mechanical feature. |
| 37 | `if only this error message were more descriptive` | no | no | no | YES | "if" as an ordinary English conjunction ("if only"), prose. |
| 38 | `if block here only runs once the feature flag is enabled` | no | no | no | NO | "if" naming the actual conditional construct being described, code reference despite zero mechanical feature. |
| 39 | `return trip to this codebase always reveals something new` | no | no | no | YES | "return" used loosely as a noun ("return trip"), prose. |
| 40 | `return statement here is unreachable after the early exit above` | no | no | no | NO | "return" naming the actual keyword being described, code reference despite zero mechanical feature. |
