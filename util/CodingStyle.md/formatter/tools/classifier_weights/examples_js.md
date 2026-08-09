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

Rows 41-81 added 2026-08-02 (GRU misclassification-driven corpus growth, same session as
`examples_c.md`'s rows 38-76 — see that file's note and `/tmp/gru_misclassified.txt`). Rows 41-64
add paraphrased variants of the keywords the GRU model got wrong on this exact zero-feature NO
shape (`class`, `static`, `default`, `in`, `void`, `function`, `let`, `yield`, `import`, `export`,
`delete`, `throw`); rows 65-81 cover `KEYWORDS_JS` members that still had zero example rows in
this file.

| 41 | `class here intentionally has no exported default constructor` | no | no | no | NO | "class" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 42 | `class here is only ever instantiated by the factory function below` | no | no | no | NO | "class" naming the actual keyword being described, not an English noun. |
| 43 | `static factory method here replaces the removed public constructor` | no | no | no | NO | "static" naming the actual property/method modifier being described, code reference despite zero mechanical feature. |
| 44 | `static block here runs once when the class is first loaded` | no | no | no | NO | "static" naming the actual keyword being described, not an English adjective. |
| 45 | `default here is only reached when the switch matches nothing else` | no | no | no | NO | "default" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 46 | `default parameter here only applies when the argument is omitted entirely` | no | no | no | NO | "default" naming the actual keyword being described, not the English adjective. |
| 47 | `in here checks the prototype chain, not just the object's own keys` | no | no | no | NO | "in" naming the actual `in` operator being described, code reference despite zero mechanical feature. |
| 48 | `in here was swapped for hasOwnProperty to avoid inherited matches` | no | no | no | NO | "in" naming the actual operator being contrasted, not the English preposition. |
| 49 | `void here explicitly discards the promise instead of awaiting it` | no | no | no | NO | "void" naming the actual operator being described, code reference despite zero mechanical feature. |
| 50 | `void here is the classic way to guarantee an undefined result` | no | no | no | NO | "void" naming the actual operator being described, not the English adjective. |
| 51 | `function here is hoisted, unlike the arrow version below` | no | no | no | NO | "function" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 52 | `function expression here is used instead of an arrow to bind its own this` | no | no | no | NO | "function" naming the actual keyword being contrasted, not English prose. |
| 53 | `let here is scoped to just this block, unlike var above it` | no | no | no | NO | "let" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 54 | `let here was chosen over const because the accumulator gets reassigned` | no | no | no | NO | "let" naming the actual keyword being contrasted, not the English verb. |
| 55 | `yield here pauses the generator and hands control back to the caller` | no | no | no | NO | "yield" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 56 | `yield here resumes with whatever value the caller passed to next` | no | no | no | NO | "yield" naming the actual keyword being described, not the English verb. |
| 57 | `import here is only needed because this helper lives in another module` | no | no | no | NO | "import" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 58 | `import statement here was reordered to satisfy the lint rule` | no | no | no | NO | "import" naming the actual keyword being described, not an English noun. |
| 59 | `export here makes this the module's single public entry point` | no | no | no | NO | "export" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 60 | `export declaration here was added so tests can reach this helper` | no | no | no | NO | "export" naming the actual keyword being described, not the English verb. |
| 61 | `delete here only removes the property, it does not free the memory` | no | no | no | NO | "delete" naming the actual operator being described, code reference despite zero mechanical feature. |
| 62 | `delete here is slow, so it should not run inside a hot loop` | no | no | no | NO | "delete" naming the actual operator being described, not the English verb. |
| 63 | `throw here only fires after every retry has already failed` | no | no | no | NO | "throw" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 64 | `throw statement here rethrows the original error with extra context` | no | no | no | NO | "throw" naming the actual keyword being described, not the English verb. |
| 65 | `async here means this function always returns a promise` | no | no | no | NO | "async" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 66 | `await here suspends execution until the promise settles` | no | no | no | NO | "await" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 67 | `debugger statement here was left in by mistake and should be removed` | no | no | no | NO | "debugger" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 68 | `extends here pulls in the base class's prototype methods` | no | no | no | NO | "extends" naming the actual inheritance keyword being described, code reference despite zero mechanical feature. |
| 69 | `false here is the safe default before the first check completes` | no | no | no | NO | "false" naming the actual literal being described, not an English adjective. |
| 70 | `finally block here always runs, even when the try returns early` | no | no | no | NO | "finally" naming the actual block keyword being described, code reference despite zero mechanical feature. |
| 71 | `for here iterates the array backwards to simplify the removal logic` | no | no | no | NO | "for" naming the actual loop construct being described, code reference despite zero mechanical feature. |
| 72 | `instanceof here checks the prototype chain, not just the constructor name` | no | no | no | NO | "instanceof" naming the actual operator being described, code reference despite zero mechanical feature. |
| 73 | `new here always goes through the overridden constructor for this class` | no | no | no | NO | "new" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 74 | `null here is the sentinel returned when the lookup finds nothing` | no | no | no | NO | "null" naming the actual literal being described, not an English adjective. |
| 75 | `super here calls the base class's constructor before this one runs` | no | no | no | NO | "super" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 76 | `this here refers to the object the method was actually called on` | no | no | no | NO | "this" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 77 | `true here is the default until the feature flag is flipped off` | no | no | no | NO | "true" naming the actual literal being described, not an English adjective. |
| 78 | `try here wraps only the risky call, not the whole function body` | no | no | no | NO | "try" naming the actual block keyword being described, code reference despite zero mechanical feature. |
| 79 | `typeof here returns a string, not the actual constructor reference` | no | no | no | NO | "typeof" naming the actual operator being described, code reference despite zero mechanical feature. |
| 80 | `var here leaks out of the block, unlike let in the loop above` | no | no | no | NO | "var" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 81 | `with here was avoided entirely since it breaks static analysis` | no | no | no | NO | "with" naming the actual (discouraged) statement keyword being described, code reference despite zero mechanical feature. |
| 82 | `this binding here is why the callback needed an arrow function instead` | no | no | no | NO | "this" naming the actual dynamic-binding keyword being described in flowing prose, code reference despite zero mechanical feature. |
| 83 | `this context was lost here the moment the method got passed as a callback` | no | no | no | NO | "this" naming the actual runtime binding under retrospective discussion, code reference despite zero mechanical feature. |
| 84 | `this is probably the trickiest part of the whole module to get right` | no | no | no | YES | "this" as an ordinary English demonstrative pronoun opening a sentence, prose. |
| 85 | `this component re-renders far more often than it really needs to` | no | no | no | YES | "this" as an ordinary English demonstrative pronoun, prose about the component, not a code construct. |
| 86 | `class field here is shared across every instance created from this constructor` | no | no | no | NO | "class" naming the actual field-declaration construct being described, code reference despite zero mechanical feature. |
| 87 | `class was chosen here over a plain object to get private fields for free` | no | no | no | NO | "class" naming the actual language construct under retrospective discussion, code reference despite zero mechanical feature. |
| 88 | `class act on the reviewer's part, catching this edge case before merge` | no | no | no | YES | "class" as part of the ordinary English idiom ("class act"), prose. |
| 89 | `class of bugs like this one keeps slipping through the review process` | no | no | no | YES | "class" as an ordinary English noun ("class of bugs"), prose unrelated to the keyword. |

Rows 90-93 added 2026-08-10 (STATE_AI.md's "grow hand-labeled hard-case corpus" session, "more
NO samples" pass) -- more zero-mechanical-feature NO coverage for keywords already present but
thin on naturalistic-phrasing NO rows.

| 90 | `switch here was picked over a chain of else-if once the case count grew past four` | no | no | no | NO | "switch" naming the actual control-flow keyword being described, code reference despite zero mechanical feature. |
| 91 | `instanceof here was avoided since it breaks across realms in an iframe` | no | no | no | NO | "instanceof" naming the actual operator being described, code reference despite zero mechanical feature. |
| 92 | `extends here pulls in the shared error-handling logic from the base class` | no | no | no | NO | "extends" naming the actual inheritance keyword being described, code reference despite zero mechanical feature. |
| 93 | `super here calls the parent constructor before this subclass sets its own fields` | no | no | no | NO | "super" naming the actual keyword being described, code reference despite zero mechanical feature. |
