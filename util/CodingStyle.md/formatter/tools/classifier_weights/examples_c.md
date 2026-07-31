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

Rows 18-21 added 2026-07-31 (STATE_AI.md's "extend classifier_weights" session), covering a few
`KEYWORDS_C` members that previously had zero example rows in this file.

| 18 | `enum of every supported color is listed in the docs` | no | no | no | YES | "enum" used loosely as a noun in a sentence ("the enum of..."), prose. |
| 19 | `enum Color { RED, GREEN, BLUE };` | no | yes | no | NO | Declaration restated, semicolon fires. |
| 20 | `struct fields are laid out in declaration order here` | no | no | no | NO | "struct" naming the actual construct being described, zero mechanical feature fires — same false-friend shape as rows 13-17. |
| 21 | `break early once the target is found` | no | no | no | YES | "break" as an ordinary English verb ("break early"), prose. |

Rows 22-29 added 2026-08-01 (STATE_AI.md's "grow hand-labeled hard-case set" session), covering
`KEYWORDS_C` members that previously had zero example rows in this file: `case`, `const`, `for`,
`return`.

| 22 | `case in point, this bug reproduces every time` | no | no | no | YES | "case" as an ordinary English noun ("case in point"), prose. |
| 23 | `case label falls through here on purpose` | no | no | no | NO | "case" naming the actual `switch`/`case` label being described, code reference despite zero mechanical feature. |
| 24 | `const correctness matters a lot in this codebase's public API` | no | no | no | YES | "const" used loosely as an adjective phrase ("const correctness"), prose. |
| 25 | `const pointer here cannot be reseated after initialization` | no | no | no | NO | "const" naming the actual qualifier being described, not an adjective in ordinary prose. |
| 26 | `for the record, this workaround predates the real fix` | no | no | no | YES | "for" as an ordinary English preposition ("for the record"), prose. |
| 27 | `for loop here intentionally skips the first element` | no | no | no | NO | "for" naming the actual loop construct being described, code reference despite zero mechanical feature. |
| 28 | `return on investment for this refactor was pretty low` | no | no | no | YES | "return" used loosely as a noun ("return on investment"), prose. |
| 29 | `return value here is only valid until the next call` | no | no | no | NO | "return" naming the actual keyword/value being described, code reference despite zero mechanical feature. |

Rows 30-37 added 2026-08-01 (second corpus-growth pass this session), covering `KEYWORDS_C`
members that still had zero example rows after rows 22-29 landed: `if`, `long`, `else`, `switch`.

| 30 | `if only we had caught this bug earlier` | no | no | no | YES | "if" as an ordinary English conjunction ("if only"), prose. |
| 31 | `if block below never executes on release builds` | no | no | no | NO | "if" naming the actual conditional construct being described, code reference despite zero mechanical feature. |
| 32 | `long story short, this hack has stuck around for years` | no | no | no | YES | "long" used in an ordinary English idiom ("long story short"), prose. |
| 33 | `long here is needed to avoid overflow on 32-bit platforms` | no | no | no | NO | "long" naming the actual integer type keyword being described, code reference despite zero mechanical feature. |
| 34 | `else the caller has to handle this itself` | no | no | no | YES | "else" as an ordinary English conjunction, prose. |
| 35 | `else branch here only runs when the cache misses` | no | no | no | NO | "else" naming the actual conditional branch being described, code reference despite zero mechanical feature. |
| 36 | `switch to the new API once the migration lands` | no | no | no | YES | "switch" as an ordinary English verb ("switch to"), prose. |
| 37 | `switch statement here has no default case on purpose` | no | no | no | NO | "switch" naming the actual construct being described, code reference despite zero mechanical feature. |
