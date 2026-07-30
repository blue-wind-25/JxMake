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
| 8 | `register your callback before calling init()` | no | no | no | YES | `register` immediately followed by "your" is ordinary prose; the comment's *next* open-paren (`init(`) is unrelated to the leading word — `nextCharIsOpenParen` is scoped to the target word's own end boundary, so paren=no here. See the row-8 note below for the correction history. |
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

Rows 13-15 added 2026-07-30 to fix the KEYWORD_BIAS regression: the original 12-row set had
zero NO-labeled examples with all four features off, so the trained bias defaulted heavily
toward YES on that all-zero vector — wrong for real code, where a zero-signal keyword-led
comment is overwhelmingly a code reference, not adjective-style prose (see the real
`cpp`/`java` fixture regressions added to `examples_cpp.md`/`examples_java.md` the same day,
and `STATE_AI.md`'s 2026-07-30 section for the full analysis). These three are hand-authored
C analogues of that same real-world shape, since no C fixture happened to trip the bug.

| 13 | `static helper used internally, not part of the public interface` | no | no | no | NO | "static" as the real keyword describing internal linkage — a code reference, not an adjective, despite firing no mechanical feature. |
| 14 | `while loop retries the connection three times` | no | no | no | NO | "while" naming the actual loop construct being described, not the English conjunction. |
| 15 | `do-while guarantees the body executes at least once` | no | no | no | NO | "do" naming the actual `do`/`while` construct, not the English auxiliary verb. |
| 16 | `static keyword restricts this symbol to file scope` | no | no | no | NO | "static" naming the actual keyword being explained, a code reference despite zero mechanical feature. |
| 17 | `default label handles unmatched values` | no | no | no | NO | "default" naming the actual `switch`/`case` label being described, not the English adjective. |
