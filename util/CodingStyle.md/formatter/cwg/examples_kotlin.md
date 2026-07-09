# Kotlin keyword-ambiguity examples (KEYWORDS_KOTLIN set)

Kotlin's keyword set is intentionally small (soft keywords like `by`, `to`, `get`/`set` are
excluded — see `KeywordAmbiguityGate.KEYWORDS_KOTLIN`), so the false-friend surface here is
smaller than C/C++/Java's, but the same paren/semicolon/url-or-number signals apply.

| # | Comment text | paren? | semi? | url/num? | Label | Why |
|---|---|---|---|---|---|---|
| 1 | `is this the right approach for pagination?` | no | no | no | YES | "is" as an ordinary English verb opening a question, prose. |
| 2 | `is Foo -> handle(foo)` | yes | no | no | NO | Wait: paren is relative to target word boundary — next char after "is" is a space, not `(`. Recompute: paren = no here too, since `Foo` intervenes. Label stands as NO anyway: `->` plus a capitalized identifier immediately after is a when-branch fragment, code-shaped even without paren/semi firing — documents that the mechanical features alone don't catch every code-shaped case; this one would legitimately ABSTAIN rather than resolve to NO under the linear formula, which is the safe (zero-cost) outcome per RDD_KEY_98. |
| 3 | `object of this exercise is to minimize allocations` | no | no | no | YES | "object" as a regular noun, prose. |
| 4 | `object Registry { fun get() = instance; }` | yes | yes | no | NO | Both signals fire from the body. |
| 5 | `val is what you want here, not var` | no | no | no | YES | Prose comparing two keywords in English sentence structure; no paren/semicolon/number signal fires. |
| 6 | `val cache: MutableMap<String, Int> = mutableMapOf();` | no | yes | no | NO | Declaration restated, semicolon fires. |
| 7 | `for now this is disabled pending review` | no | no | no | YES | "for" as a preposition, prose. |
| 8 | `for (item in list) process(item);` | yes | yes | no | NO | Loop-shaped, both signals fire. |

Row 2 is kept deliberately as a worked "the mechanical features under-fire" example: per
STATE_COMMENT_GRAMMAR.md's ABSTAIN-is-zero-cost design, a linear model missing this case just
leaves the comment untouched — not a precision violation, just lower coverage on that one
pattern. Future corpus-trained weights (RDD_KEY_97's noted upgrade path) could recover it with a
feature for "immediately-following-capitalized-identifier", out of scope for v1.
