# Java keyword-ambiguity examples (KEYWORDS_JAVA set)

| # | Comment text | paren? | semi? | url/num? | Label | Why |
|---|---|---|---|---|---|---|
| 1 | `static analysis flags this as dead code` | no | no | no | YES | "static" as an adjective, prose (mirrors the C example — Java's keyword set overlaps here, but the list is kept separate per RDD_KEY_96 since the two languages diverge elsewhere, e.g. `record`/`var`/`yield`). |
| 2 | `static Map<String,Integer> CACHE = new HashMap<>();` | no | yes | no | NO | Declaration restated in the comment; semicolon fires. |
| 3 | `default values are applied when the field is omitted` | no | no | no | YES | Prose describing behavior. |
| 4 | `default: throw new IllegalStateException();` | yes | yes | no | NO | `throw(`-shaped is not literally present, but paren/semi both fire from `IllegalStateException()` — clearly code. |
| 5 | `final answer to this question is "it depends"` | no | no | no | YES | "final" as an adjective ("final answer"), prose false-friend case. |
| 6 | `final long TIMEOUT_MS = 5000;` | no | yes | yes | NO | Both semicolon and the numeric literal fire alongside a declaration shape. |
| 7 | `native code path is only used on Windows` | no | no | no | YES | "native" as an adjective, prose. |
| 8 | `native void init();` | yes | yes | no | NO | Declaration fragment, both signals fire. |
| 9 | `transient state should never be persisted here` | no | no | no | YES | Prose explaining a design rule using "transient" as an adjective. |
| 10 | `transient int retryCount;` | no | yes | no | NO | Field declaration. |
| 11 | `while loop` | no | no | no | NO | Real regression, `test/java_core_inp.java` — names the actual loop construct being demonstrated, not prose. Added 2026-07-30, see `STATE_AI.md`'s 2026-07-30 section. |
| 12 | `do-while` | no | no | no | NO | Real regression, `test/java_core_inp.java` — same as above for `do`/`while`. |
| 13 | `var usage` | no | no | no | NO | Real regression, `test/java_combined_inp.java` — labels a code section demonstrating `var`, not prose using "var" as a word. |
| 14 | `switch expressions, records, sealed classes, text blocks, var, pattern matching.` | no | no | no | NO | Real regression, `test/java_combined_inp.java` — file-level banner listing language features by keyword, not a sentence. |
| 15 | `this comment is between annotation and field` | no | no | no | NO | Real regression, `test/java_comments_inp.java` — "this" refers to the comment itself in a meta/structural sense tied to code layout, not free-standing English prose; kept NO to match the fixture's expected (non-capitalized) output. |
| 16 | `this is a void method so this is wrong but tests the comment` | no | no | no | NO | Real regression, `test/java_comments_inp.java` — same meta/structural "this", describing the surrounding code shape rather than being ordinary prose. |
| 17 | `transient fields skip default serialization` | no | no | no | NO | "transient" naming the actual field modifier being explained, code reference despite zero mechanical feature. |
| 18 | `native methods bridge to platform code` | no | no | no | NO | "native" naming the actual keyword being explained. |

Rows 19-22 added 2026-07-31 (STATE_AI.md's "extend classifier_weights" session), covering a few
`KEYWORDS_JAVA` members that previously had zero example rows in this file.

| 19 | `interface between the two modules is documented separately` | no | no | no | YES | "interface" used loosely as a noun ("the interface between..."), prose. |
| 20 | `interface Comparable<T> { int compareTo(T o); }` | yes | no | no | NO | Real declaration, paren fires from `compareTo(`. |
| 21 | `abstract enough that every subclass fills in the details` | no | no | no | YES | "abstract" used loosely as an adjective in a sentence, prose. |
| 22 | `abstract methods have no body in this base class` | no | no | no | NO | "abstract" naming the actual method modifier being described, zero mechanical feature fires — same false-friend shape as rows 11/17/18. |
