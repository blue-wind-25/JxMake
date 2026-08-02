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

Rows 38-76 added 2026-08-02 (GRU misclassification-driven corpus growth: the retrained GRU model
scored 65.2% precision on this 221-row hand-labeled set with all 77 errors being false positives
on exactly this zero-mechanical-feature NO shape — see `/tmp/gru_misclassified.txt`). Rows 38-57
add paraphrased variants of the keywords the model got wrong (`short`, `void`, `static`, `while`,
`do`, `default`, `struct`, `case`, `for`, `else`); rows 58-76 cover `KEYWORDS_C` members that still
had zero example rows in this file after rows 22-37.

| 38 | `short here holds the packet length field on the wire` | no | no | no | NO | "short" naming the actual integer type keyword being used, code reference despite zero mechanical feature. |
| 39 | `short saves memory here compared to a full int for this counter` | no | no | no | NO | "short" naming the actual type keyword chosen for this field, not an English adjective. |
| 40 | `void return type here means the caller cannot get a result back` | no | no | no | NO | "void" naming the actual return-type keyword being described, code reference despite zero mechanical feature. |
| 41 | `void pointer here is cast to the real type before use` | no | no | no | NO | "void" naming the actual `void *` type being described, not the English adjective sense. |
| 42 | `static linkage here keeps this symbol out of the exported table` | no | no | no | NO | "static" naming the actual linkage specifier being described, code reference despite zero mechanical feature. |
| 43 | `static storage duration means this variable keeps its value between calls` | no | no | no | NO | "static" naming the actual storage-duration keyword being described, not an English adjective. |
| 44 | `while condition here is re-checked before every iteration` | no | no | no | NO | "while" naming the actual loop condition being described, code reference despite zero mechanical feature. |
| 45 | `while here was chosen over recursion to avoid stack growth` | no | no | no | NO | "while" naming the actual loop construct being contrasted with recursion, not the English conjunction. |
| 46 | `do block here always runs once even if the condition starts false` | no | no | no | NO | "do" naming the actual `do`/`while` construct being described, code reference despite zero mechanical feature. |
| 47 | `do keyword here pairs with the while at the bottom of the loop` | no | no | no | NO | "do" naming the actual keyword being explained, not the English auxiliary verb. |
| 48 | `default here is only reached when none of the case labels match` | no | no | no | NO | "default" naming the actual switch label being described, code reference despite zero mechanical feature. |
| 49 | `default branch here logs an error before returning` | no | no | no | NO | "default" naming the actual switch branch being described, not the English adjective. |
| 50 | `struct here is packed to avoid padding between the fields` | no | no | no | NO | "struct" naming the actual type being described, code reference despite zero mechanical feature. |
| 51 | `struct definition here mirrors the on-disk record layout` | no | no | no | NO | "struct" naming the actual construct being described, not an English noun. |
| 52 | `case here intentionally omits a break to fall through to the next` | no | no | no | NO | "case" naming the actual switch label being described, code reference despite zero mechanical feature. |
| 53 | `case block here only runs for the error path` | no | no | no | NO | "case" naming the actual switch case being described, not the English noun. |
| 54 | `for here iterates backwards to simplify the removal logic` | no | no | no | NO | "for" naming the actual loop construct being described, code reference despite zero mechanical feature. |
| 55 | `for loop here was unrolled once for a small speedup` | no | no | no | NO | "for" naming the actual loop construct being described, not the English preposition. |
| 56 | `else here only fires when the lookup table has no match` | no | no | no | NO | "else" naming the actual conditional branch being described, code reference despite zero mechanical feature. |
| 57 | `else clause here was added later to handle the timeout case` | no | no | no | NO | "else" naming the actual conditional branch being described, not the English conjunction. |
| 58 | `auto here lets the compiler deduce the iterator's real type` | no | no | no | NO | "auto" naming the actual type-deduction keyword being described, code reference despite zero mechanical feature. |
| 59 | `char here holds a single byte, not a full Unicode code point` | no | no | no | NO | "char" naming the actual type keyword being described, not an English noun. |
| 60 | `continue here skips straight to the next iteration's increment` | no | no | no | NO | "continue" naming the actual loop-control keyword being described, code reference despite zero mechanical feature. |
| 61 | `double here gives enough precision for the accumulated total` | no | no | no | NO | "double" naming the actual floating-point type keyword being described, not an English adjective. |
| 62 | `enum here lists every valid state the machine can be in` | no | no | no | NO | "enum" naming the actual enumeration type being described, code reference despite zero mechanical feature. |
| 63 | `extern here declares the symbol without defining it in this file` | no | no | no | NO | "extern" naming the actual linkage keyword being described, code reference despite zero mechanical feature. |
| 64 | `float here loses precision that double would have kept` | no | no | no | NO | "float" naming the actual type keyword being described, not an English adjective. |
| 65 | `goto here jumps straight to the cleanup label on error` | no | no | no | NO | "goto" naming the actual jump keyword being described, code reference despite zero mechanical feature. |
| 66 | `inline here is only a hint, the compiler can still ignore it` | no | no | no | NO | "inline" naming the actual keyword being described, not an English adjective. |
| 67 | `int here defaults to the platform's native word size` | no | no | no | NO | "int" naming the actual type keyword being described, code reference despite zero mechanical feature. |
| 68 | `register here is a hint the compiler is free to ignore today` | no | no | no | NO | "register" naming the actual storage-class keyword being described, code reference despite zero mechanical feature (contrast with row 8's "register your callback", the English-verb sense). |
| 69 | `restrict here promises the compiler these pointers never alias` | no | no | no | NO | "restrict" naming the actual qualifier keyword being described, code reference despite zero mechanical feature. |
| 70 | `signed here allows the counter to go negative during underflow` | no | no | no | NO | "signed" naming the actual type qualifier being described, not an English adjective. |
| 71 | `sizeof here returns the padded size, not the sum of the fields` | no | no | no | NO | "sizeof" naming the actual operator being described, code reference despite zero mechanical feature. |
| 72 | `typedef here gives the anonymous struct a usable name` | no | no | no | NO | "typedef" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 73 | `union here overlays the two representations in the same memory` | no | no | no | NO | "union" naming the actual type keyword being described, not an English noun. |
| 74 | `unsigned here means this counter can never go negative` | no | no | no | NO | "unsigned" naming the actual type qualifier being described, code reference despite zero mechanical feature. |
| 75 | `volatile here tells the compiler this value can change outside its control` | no | no | no | NO | "volatile" naming the actual qualifier keyword being described, code reference despite zero mechanical feature. |
| 76 | `break here only exits the innermost loop, not the outer one` | no | no | no | NO | "break" naming the actual break statement being described, code reference despite zero mechanical feature. |
| 77 | `static here is only visible within this translation unit, not exported` | no | no | no | NO | "static" naming the actual linkage-restricting keyword being described, code reference despite zero mechanical feature. |
| 78 | `static was the wrong choice here since other files needed the symbol` | no | no | no | NO | "static" naming the actual linkage keyword being described in a natural-sounding retrospective sentence, code reference despite zero mechanical feature. |
| 79 | `static analysis flagged this function for a possible buffer overrun` | no | no | no | YES | "static" as part of the ordinary English phrase "static analysis" (a tool/process), not the storage-class keyword. |
| 80 | `static electricity was the actual cause of the sensor glitch, not this code` | no | no | no | YES | "static" as an ordinary English adjective (electricity), prose unrelated to the keyword. |
| 81 | `return here skips the cleanup block entirely, which is intentional` | no | no | no | NO | "return" naming the actual control-flow keyword being described in flowing prose, code reference despite zero mechanical feature. |
| 82 | `return was added late in review after the original bug report` | no | no | no | NO | "return" naming the actual keyword being discussed retrospectively, code reference despite zero mechanical feature. |
| 83 | `return to this file once the dependency issue upstream is resolved` | no | no | no | YES | "return" as an ordinary English verb (come back to), prose. |
| 84 | `return on this investment was never clearly measured by the team` | no | no | no | YES | "return" as an ordinary English noun (an investment return), prose unrelated to the keyword. |
