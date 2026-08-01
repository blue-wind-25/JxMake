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

Rows 24-31 added 2026-08-01 (second corpus-growth pass this session), covering `KEYWORDS_KOTLIN`
members that still had zero example rows after rows 16-23 landed: `break`, `do`, `else`, `in`.

| 24 | `break it down for me, why does this only fail on Android?` | no | no | no | no | YES | "break" in the ordinary English idiom ("break it down"), prose. |
| 25 | `break here only exits the innermost when block` | no | no | no | no | NO | "break" naming the actual break statement being described, code reference despite zero mechanical feature. |
| 26 | `do yourself a favor and read the migration guide first` | no | no | no | no | YES | "do" as an ordinary English verb ("do yourself a favor"), prose. |
| 27 | `do block here always executes once before checking the condition` | no | no | no | no | NO | "do" naming the actual do-while construct being described, code reference despite zero mechanical feature. |
| 28 | `else the whole pipeline falls over on the next deploy` | no | no | no | no | YES | "else" as an ordinary English conjunction, prose. |
| 29 | `else branch here only runs when the cache lookup misses` | no | no | no | no | NO | "else" naming the actual conditional branch being described, code reference despite zero mechanical feature. |
| 30 | `in short, this workaround should be temporary` | no | no | no | no | YES | "in" as an ordinary English preposition ("in short"), prose. |
| 31 | `in operator here checks membership without allocating a new list` | no | no | no | no | NO | "in" naming the actual `in` operator being described, code reference despite zero mechanical feature. |

Rows 32-67 added 2026-08-02 (GRU misclassification-driven corpus growth, same session as
`examples_c.md`'s rows 38-76 — see that file's note and `/tmp/gru_misclassified.txt`). Rows 32-55
add paraphrased variants of the keywords the GRU model got wrong on this exact zero-feature NO
shape (`is`, `this`, `var`, `when`, `object`, `class`, `interface`, `as`, `fun`, `break`, `else`,
`in`); rows 56-67 cover `KEYWORDS_KOTLIN` members that still had zero example rows in this file.

| 32 | `is check here narrows the sealed type before the branch below runs` | no | no | no | no | NO | "is" naming the actual type-check keyword being described, code reference despite zero mechanical feature. |
| 33 | `is here smart-casts the variable for the rest of this block` | no | no | no | no | NO | "is" naming the actual operator being described, not the English verb. |
| 34 | `this reference here is captured by the closure passed to launch` | no | no | no | no | NO | "this" referring to the language's actual `this` reference, code reference despite zero mechanical feature. |
| 35 | `this receiver here refers to the outer class, not the lambda` | no | no | no | no | NO | "this" naming the actual receiver being described, not the English demonstrative. |
| 36 | `var here is only needed because the loop counter is reassigned` | no | no | no | no | NO | "var" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 37 | `var property here triggers the custom setter on every assignment` | no | no | no | no | NO | "var" naming the actual keyword being described, not an English noun. |
| 38 | `when here was rewritten as an expression to drop the fallthrough bug` | no | no | no | no | NO | "when" naming the actual construct being described, code reference despite zero mechanical feature. |
| 39 | `when statement here has no else arm, which is intentional` | no | no | no | no | NO | "when" naming the actual construct being described, not the English conjunction. |
| 40 | `object here is lazily initialized on first access, not eagerly` | no | no | no | no | NO | "object" naming the actual singleton declaration being described, code reference despite zero mechanical feature. |
| 41 | `object expression here creates a one-off anonymous implementation` | no | no | no | no | NO | "object" naming the actual keyword being described, not an English noun. |
| 42 | `class here intentionally has no public constructor` | no | no | no | no | NO | "class" naming the actual type keyword being described, code reference despite zero mechanical feature. |
| 43 | `class here is open so a test double can extend it` | no | no | no | no | NO | "class" naming the actual declaration keyword being described, not an English noun. |
| 44 | `interface here declares only the contract, no default implementation` | no | no | no | no | NO | "interface" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 45 | `interface here is implemented by three different adapters` | no | no | no | no | NO | "interface" naming the actual declaration keyword being described, not an English noun. |
| 46 | `as here fails fast instead of silently returning null on mismatch` | no | no | no | no | NO | "as" naming the actual cast keyword being described, code reference despite zero mechanical feature. |
| 47 | `as cast here was changed to the safe variant after a crash report` | no | no | no | no | NO | "as" naming the actual keyword being contrasted, not the English conjunction. |
| 48 | `fun here is marked inline to avoid the lambda allocation overhead` | no | no | no | no | NO | "fun" naming the actual function-declaration keyword being described, code reference despite zero mechanical feature. |
| 49 | `fun signature here changed to accept a nullable parameter` | no | no | no | no | NO | "fun" naming the actual keyword being described, not the English noun. |
| 50 | `break here only exits the enclosing for loop, not the outer when` | no | no | no | no | NO | "break" naming the actual break statement being described, code reference despite zero mechanical feature. |
| 51 | `break here was labeled to escape the nested loop directly` | no | no | no | no | NO | "break" naming the actual keyword being described, not the English idiom. |
| 52 | `else here only runs when none of the earlier when branches matched` | no | no | no | no | NO | "else" naming the actual conditional branch being described, code reference despite zero mechanical feature. |
| 53 | `else clause here was added later to handle the timeout case` | no | no | no | no | NO | "else" naming the actual branch keyword being described, not the English conjunction. |
| 54 | `in here iterates the range without ever allocating a list` | no | no | no | no | NO | "in" naming the actual operator being described, code reference despite zero mechanical feature. |
| 55 | `in check here is evaluated once before entering the loop body` | no | no | no | no | NO | "in" naming the actual `in` operator being described, not the English preposition. |
| 56 | `continue here skips straight to the next iteration's condition check` | no | no | no | no | NO | "continue" naming the actual loop-control keyword being described, code reference despite zero mechanical feature. |
| 57 | `false here is the safe default before the first health check completes` | no | no | no | no | NO | "false" naming the actual literal being described, not an English adjective. |
| 58 | `for here iterates the map's entries directly to avoid boxing keys twice` | no | no | no | no | NO | "for" naming the actual loop construct being described, code reference despite zero mechanical feature. |
| 59 | `null here is the sentinel returned when the lookup finds nothing` | no | no | no | no | NO | "null" naming the actual literal being described, not an English adjective. |
| 60 | `package here must match this file's directory exactly` | no | no | no | no | NO | "package" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 61 | `super here calls the base class's constructor before this one runs` | no | no | no | no | NO | "super" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 62 | `throw here only fires once the retry budget is exhausted` | no | no | no | no | NO | "throw" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 63 | `true here is the default until the feature flag is flipped off` | no | no | no | no | NO | "true" naming the actual literal being described, not an English adjective. |
| 64 | `try here wraps only the risky call, not the whole function body` | no | no | no | no | NO | "try" naming the actual block keyword being described, code reference despite zero mechanical feature. |
| 65 | `typealias here shortens the long generic type used throughout this file` | no | no | no | no | NO | "typealias" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 66 | `typeof here is only valid inside a reified generic function` | no | no | no | no | NO | "typeof" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 67 | `val here is preferred over var whenever the reference never changes` | no | no | no | no | NO | "val" naming the actual immutable-declaration keyword being described, code reference despite zero mechanical feature. |
