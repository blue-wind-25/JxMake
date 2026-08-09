# Python3 keyword-ambiguity examples (KEYWORDS_PYTHON set)

Added 2026-08-10 (`STATE_AI.md`'s "grow hand-labeled hard-case corpus" session, user-directed
after confirming python3 is the only one of {json5, css, yaml, toml, xml, html5, js, ts, python3,
makefile, bash, powershell} that actually reaches `KeywordAmbiguityGate`). Python3's `#`-comment
normalization (`MiscRuleIndent.computeHashCommentGroups`, wired 2026-08-08 per `STATE_PYTHON3.md`)
routes through the same `classifyComment`/`CommentClassifier`/`KeywordAmbiguityGate` pipeline
curly uses -- `KeywordAmbiguityGate.hasLeadingKeywordMatch` had no Python branch at all and
silently fell through to the wrong `KEYWORDS_C` default (same bug shape originally found for
JS/TS in `examples_js.md`): Python shares some keywords with C (`if`/`for`/`while`/`return`/
`else`/`break`/`continue`/`class`) but not most of its real false-friend surface (`def`/`elif`/
`except`/`lambda`/`yield`/`with`/`import`/`from`/`None`/`True`/`False`/`pass`/`raise`/`assert`/
`global`/`nonlocal`/`match`/`case`/... aren't in `KEYWORDS_C`). Fixed alongside these examples:
`KeywordAmbiguityGate.KEYWORDS_PYTHON` (full CPython `keyword.kwlist` + soft keywords `match`/
`case`) + a `lang.isPython3` dispatch branch. Same feature definitions as `examples_c.md`/
`examples_js.md` (paren/semi/url-or-number; no arrow column -- Python's `->` return-type
annotation is a type-hint construct, not a branch/match shape like Kotlin's `->`, and none of
these rows discuss it).

Rows below are deliberately balanced (equal zero-mechanical-feature YES/NO) from the start,
per the `examples_c.md`/`examples_js.md` `KEYWORD_BIAS`-flip lesson (STATE_AI.md's 2026-07-30
section) -- growing a new language's file all-YES-first and rebalancing later is avoidable now
that the lesson is known up front.

| # | Comment text | paren? | semi? | url/num? | Label | Why |
|---|---|---|---|---|---|---|
| 1 | `def not worry about the edge case, the caller already validates this` | no | no | no | YES | "def" as informal internet slang for "definitely" ("def not worry"), prose false-friend case. |
| 2 | `def load_config(path): return json.load(open(path))` | yes | no | no | NO | Real function declaration; paren fires. |
| 3 | `class of problems this helper solves is input validation` | no | no | no | YES | "class" as an ordinary noun ("class of problems"), prose false-friend case. |
| 4 | `class Widget(Base): pass` | no | no | no | NO | Declaration-shaped, zero mechanical feature fires (no paren/semi/url-num) -- code reference despite the classifier's mechanical signals staying silent. |
| 5 | `return trip to this module always reveals something new` | no | no | no | YES | "return" used loosely as a noun ("return trip"), prose. |
| 6 | `return statement here is unreachable after the early exit above` | no | no | no | NO | "return" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 7 | `if only this error message were more descriptive` | no | no | no | YES | "if" as an ordinary English conjunction ("if only"), prose. |
| 8 | `if block here only runs once the feature flag is enabled` | no | no | no | NO | "if" naming the actual conditional construct being described, code reference despite zero mechanical feature. |
| 9 | `for the sake of clarity, this helper was split into two functions` | no | no | no | YES | "for" as an ordinary English preposition ("for the sake of"), prose. |
| 10 | `for loop here intentionally iterates the list backwards` | no | no | no | NO | "for" naming the actual loop construct being described, code reference despite zero mechanical feature. |
| 11 | `while we're at it, let's also clean up the imports` | no | no | no | YES | "while" as an ordinary English conjunction ("while we're at it"), prose. |
| 12 | `while loop here intentionally spins until the flag flips` | no | no | no | NO | "while" naming the actual loop construct being described, code reference despite zero mechanical feature. |
| 13 | `try to keep this helper under fifty lines if you can` | no | no | no | YES | "try" as an ordinary English verb ("try to"), prose. |
| 14 | `try block here only wraps the network call, not the parsing` | no | no | no | NO | "try" naming the actual exception-handling block being described, code reference despite zero mechanical feature. |
| 15 | `except for this one caller, nobody else still uses the old signature` | no | no | no | YES | "except" as an ordinary English preposition ("except for"), prose. |
| 16 | `except clause here only catches the specific timeout error` | no | no | no | NO | "except" naming the actual exception-handling keyword being described, code reference despite zero mechanical feature. |
| 17 | `import here is only needed because this helper lives in another module` | no | no | no | NO | "import" naming the actual statement being described, code reference despite zero mechanical feature. |
| 18 | `import numpy as np` | no | no | no | NO | Real import statement, zero mechanical feature fires -- code reference despite the classifier's mechanical signals staying silent. |
| 19 | `with any luck this refactor lands before the release cutoff` | no | no | no | YES | "with" as an ordinary English preposition ("with any luck"), prose. |
| 20 | `with statement here ensures the file handle always gets closed` | no | no | no | NO | "with" naming the actual context-manager keyword being described, code reference despite zero mechanical feature. |
| 21 | `in this case the callback fires twice` | no | no | no | YES | "in" as an ordinary preposition opening a sentence, prose. |
| 22 | `in here checks membership, not identity, unlike the check below` | no | no | no | NO | "in" naming the actual membership operator being described, code reference despite zero mechanical feature. |
| 23 | `lambda calculus is not something most readers of this codebase know` | no | no | no | YES | "lambda" as part of an ordinary English technical-noun phrase ("lambda calculus"), prose about a math concept, not this language's keyword. |
| 24 | `lambda here is only used because a full function felt like overkill` | no | no | no | NO | "lambda" naming the actual anonymous-function keyword being described, code reference despite zero mechanical feature. |
| 25 | `yield to the caller before continuing the loop` | no | no | no | YES | "yield" as an ordinary English verb ("yield to"), prose. |
| 26 | `yield here pauses the generator and hands control back to the caller` | no | no | no | NO | "yield" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 27 | `pass this along to the next reviewer once you're done` | no | no | no | YES | "pass" as an ordinary English verb ("pass this along"), prose. |
| 28 | `pass here is a deliberate no-op placeholder until the handler ships` | no | no | no | NO | "pass" naming the actual no-op statement being described, code reference despite zero mechanical feature. |
| 29 | `raise your hand in the next review if this looks wrong` | no | no | no | YES | "raise" as an ordinary English verb ("raise your hand"), prose. |
| 30 | `raise here only fires once every retry has already failed` | no | no | no | NO | "raise" naming the actual exception-raising keyword being described, code reference despite zero mechanical feature. |
| 31 | `assert yourself here and just reject the malformed input outright` | no | no | no | YES | "assert" used loosely as an ordinary English verb ("assert yourself"), prose. |
| 32 | `assert statement here only runs when the debug flag is set` | no | no | no | NO | "assert" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 33 | `global outage last week is why this retry logic exists at all` | no | no | no | YES | "global" as an ordinary English adjective ("global outage"), prose. |
| 34 | `global keyword here is needed since the counter is mutated in place` | no | no | no | NO | "global" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 35 | `match this pattern against every row before filtering the results` | no | no | no | YES | "match" as an ordinary English verb, prose about the general concept, not this language's `match` statement. |
| 36 | `match statement here replaces what used to be a long if/elif chain` | no | no | no | NO | "match" naming the actual structural pattern-matching keyword being described, code reference despite zero mechanical feature. |
| 37 | `case in point, this hack has outlived three rewrites` | no | no | no | YES | "case" as an ordinary English noun ("case in point"), prose. |
| 38 | `case here only matches when the payload has exactly two fields` | no | no | no | NO | "case" naming the actual pattern-matching branch being described, code reference despite zero mechanical feature. |
| 39 | `None of this logic runs unless the feature flag is enabled` | no | no | no | YES | "None" used loosely as the ordinary English quantifier ("none of"), prose, despite the capital letter matching the literal's spelling. |
| 40 | `None here is the sentinel returned when the lookup finds nothing` | no | no | no | NO | "None" naming the actual literal being described, code reference despite zero mechanical feature. |
| 41 | `True enough, this workaround is uglier than the original bug` | no | no | no | YES | "True" used loosely as the ordinary English interjection ("true enough"), prose, despite the capital letter matching the literal's spelling. |
| 42 | `True here is the default until the feature flag is flipped off` | no | no | no | NO | "True" naming the actual literal being described, code reference despite zero mechanical feature. |
| 43 | `elif here only runs when the first two branches both miss` | no | no | no | NO | "elif" naming the actual keyword being described, code reference despite zero mechanical feature; no natural zero-feature English prose reading exists for "elif" (not an ordinary word), so this keyword has no paired YES row. |
| 44 | `async here means this function always returns a coroutine` | no | no | no | NO | "async" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 45 | `async def fetch_user(user_id): return await db.get(user_id)` | yes | no | no | NO | Real coroutine-function declaration; paren fires. |
| 46 | `await here suspends execution until the coroutine resolves` | no | no | no | NO | "await" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 47 | `del here only removes the local reference, not the underlying object` | no | no | no | NO | "del" naming the actual deletion statement being described, code reference despite zero mechanical feature. |
| 48 | `nonlocal here is needed since the inner closure mutates the counter` | no | no | no | NO | "nonlocal" naming the actual keyword being described, code reference despite zero mechanical feature. |
