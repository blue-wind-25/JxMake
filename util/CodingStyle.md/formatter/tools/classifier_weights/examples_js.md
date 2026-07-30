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
