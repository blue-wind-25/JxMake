# Kotlin keyword-ambiguity examples (KEYWORDS_KOTLIN set)

Kotlin's keyword set is intentionally small (soft keywords like `by`, `to`, `get`/`set` are
excluded — see `KeywordAmbiguityGate.KEYWORDS_KOTLIN`), so the false-friend surface here is
smaller than C/C++/Java's, but the same paren/arrow/semicolon/url-or-number signals apply.

| # | Comment text | paren? | arrow? | semi? | url/num? | Label | Why |
|---|---|---|---|---|---|---|---|
| 1 | `is this the right approach for pagination?` | no | no | no | no | YES | "is" as an ordinary English verb opening a question, prose. |
| 2 | `is Foo -> handle(foo)` | no | yes | no | no | NO | `->` after "Foo" (a `when`-branch shape) is the signal here — `nextCharIsOpenParen` doesn't fire since "Foo" intervenes between the target word and the paren, but `nextTokenIsArrow` scans the whole tail after the target word and catches the arrow regardless of what's between. |
| 3 | `object of this exercise is to minimize allocations` | no | no | no | no | YES | "object" as a regular noun, prose. |
| 4 | `object Registry { fun get() = instance; }` | yes | no | yes | no | NO | Both paren and semicolon signals fire from the body. |
| 5 | `val is what you want here, not var` | no | no | no | no | YES | Prose comparing two keywords in English sentence structure; no signal fires. |
| 6 | `val cache: MutableMap<String, Int> = mutableMapOf();` | no | no | yes | no | NO | Declaration restated, semicolon fires. |
| 7 | `for now this is disabled pending review` | no | no | no | no | YES | "for" as a preposition, prose. |
| 8 | `for (item in list) process(item);` | yes | no | yes | no | NO | Loop-shaped, both signals fire. |

Row 2 was originally a documented gap (no feature fired, so the linear model resolved it to
ABSTAIN — safe but low-coverage) until `CommentFeatureVector.nextTokenIsArrow` was added
specifically to close it (see `tools/classifier_weights/weights.md`'s "Adding a feature" section for the full story).
It's kept in the table as the worked example that motivated that feature, not as a residual gap.

Rows 9-10 added 2026-07-30, hand-authored analogues of the real `cpp`/`java` zero-feature NO
regressions found the same day (see `examples_cpp.md`/`examples_java.md` and `STATE_AI.md`'s
2026-07-30 section) — no Kotlin fixture happened to trip the bug, but the same false-friend
shape applies to Kotlin's own keyword set.

| 9 | `this function is called from the UI thread` | no | no | no | no | NO | "this" referring to the surrounding function itself (a code reference), not the English demonstrative pronoun opening ordinary prose. |
| 10 | `var holds the mutable reference` | no | no | no | no | NO | "var" naming the actual declaration keyword being described, not an English noun. |
| 11 | `when branch order matters here` | no | no | no | no | NO | "when" naming the actual `when` construct being explained, not the English conjunction. |
| 12 | `object declaration creates a singleton` | no | no | no | no | NO | "object" naming the actual declaration keyword, code reference despite zero mechanical feature. |

Rows 13-15 added 2026-07-31 (STATE_AI.md's "extend classifier_weights" session), covering a few
`KEYWORDS_KOTLIN` members that previously had zero example rows in this file.

| 13 | `class of problems this solves is input validation` | no | no | no | no | YES | "class" used loosely as a noun ("the class of problems"), prose. |
| 14 | `class Repository(private val api: Api)` | yes | no | no | no | NO | Real primary-constructor declaration, paren fires. |
| 15 | `interface segregation keeps this contract small` | no | no | no | no | NO | "interface" naming the actual declaration keyword being described, zero mechanical feature fires — same false-friend shape as rows 9-12. |

Rows 16-23 added 2026-08-01 (STATE_AI.md's "grow hand-labeled hard-case set" session), covering
`KEYWORDS_KOTLIN` members that previously had zero example rows in this file: `as`, `fun`, `if`,
`return`.

| 16 | `as usual, this workaround only masks the symptom` | no | no | no | no | YES | "as" as an ordinary English conjunction ("as usual"), prose. |
| 17 | `as expression here converts safely between the two types` | no | no | no | no | NO | "as" naming the actual cast/conversion keyword being described, code reference despite zero mechanical feature. |
| 18 | `fun fact, this bug has been here since the first commit` | no | no | no | no | YES | "fun" as an ordinary English noun ("fun fact"), prose. |
| 19 | `fun declaration here is only ever called from tests` | no | no | no | no | NO | "fun" naming the actual function-declaration keyword being described, code reference despite zero mechanical feature. |
| 20 | `if anything, this refactor made the callback harder to follow` | no | no | no | no | YES | "if" as an ordinary English conjunction ("if anything"), prose. |
| 21 | `if branch here only runs on the first launch` | no | no | no | no | NO | "if" naming the actual conditional construct being described, code reference despite zero mechanical feature. |
| 22 | `return on investment for this migration was pretty low` | no | no | no | no | YES | "return" used loosely as a noun ("return on investment"), prose. |
| 23 | `return value here is only valid inside the lambda` | no | no | no | no | NO | "return" naming the actual keyword/value being described, code reference despite zero mechanical feature. |
