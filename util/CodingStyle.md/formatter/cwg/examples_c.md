# C/C++ shared keyword-ambiguity examples (KEYWORDS_C set)

Leading word already lowercase (the case classify() is invoked for — see
`MiscRule.capitalizeFirstLetter`), matches a C keyword, so `hasLeadingKeywordMatch = true` and
stage 2 (`resolveAmbiguousKeyword`) must decide.

| # | Comment text | paren? | semi? | url/num? | Label | Why |
|---|---|---|---|---|---|---|
| 1 | `static analysis caught a null deref here` | no | no | no | YES | Plain prose, "static" used as an English adjective. |
| 2 | `static int cache_size;` | no | yes | no | NO | Restating a declaration; semicolon signals code-shaped content. |
| 3 | `static(void) is a legacy annotation we removed` | yes | no | no | NO | `static(` reads as a call/cast-like token, not a word boundary a sentence would have. |
| 4 | `default case falls through intentionally` | no | no | no | YES | Prose describing switch behavior in English, not code. |
| 5 | `default: return -1;` | no | yes | no | NO | Literal code snippet embedded in the comment. |
| 6 | `short delay before retry, about 50ms` | no | no | yes | YES | Prose; the number (50ms) is incidental, not a code reference. |
| 7 | `short.java has the same bug, see line 42` | no | no | yes | NO | Filename-shaped token (`short.java`) plus a line number — comment is *about* code, not itself prose starting with "short" as an adjective; low-confidence, ABSTAIN-equivalent is acceptable here even though not a hard NO. |
| 8 | `register your callback before calling init()` | yes | no | no | NO | `register` immediately followed by "your" is fine prose-wise, but the comment's *next* open-paren (`init(`) is unrelated to the leading word — this example intentionally shows nextCharIsOpenParen only fires on the word directly after target, so paren=no here (correcting: recompute). |
| 9 | `continue reading the spec before editing this` | no | no | no | YES | "continue" as an English verb, ordinary prose. |
| 10 | `continue; // fallthrough guard` | no | yes | no | NO | Bare code statement. |
| 11 | `void of any real logic, this is a stub` | no | no | no | YES | "void" used as the English adjective ("void of"), common false-friend case the classifier must get right for the 99% precision target. |
| 12 | `void main(void)` | yes | no | no | NO | Signature-shaped, `void` directly followed by whitespace then `main(` — nextWord captures "main", paren feature reflects the word immediately after target's word boundary since no other word intervenes before `(`. |

Row 8 note: on reflection `nextCharIsOpenParen` is defined relative to the *target word's* end
boundary, not the whole comment, so `register your callback...` has `nextCharIsOpenParen = false`
(next char after "register" is a space, not `(`) — label stands (YES would be more accurate here
actually, since it's ordinary prose "register your callback"). Corrected: **row 8 is YES**, not
NO. Kept as an explicit worked example of the feature's exact scope, since it's the one place
in this set where an initial guess was wrong.
