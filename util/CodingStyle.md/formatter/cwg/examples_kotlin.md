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
specifically to close it (see `cwg/weights.md`'s "Adding a feature" section for the full story).
It's kept in the table as the worked example that motivated that feature, not as a residual gap.
