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

Rows 23-30 added 2026-08-01 (STATE_AI.md's "grow hand-labeled hard-case set" session), covering
`KEYWORDS_JAVA` members that previously had zero example rows in this file: `case`, `if`,
`public`, `record`.

| 23 | `case in point, this workaround predates the real fix` | no | no | no | YES | "case" as an ordinary English noun ("case in point"), prose. |
| 24 | `case label here falls through to the next one on purpose` | no | no | no | NO | "case" naming the actual `switch`/`case` label being described, code reference despite zero mechanical feature. |
| 25 | `if anything, this change makes the bug more visible` | no | no | no | YES | "if" as an ordinary English conjunction ("if anything"), prose. |
| 26 | `if block here is only reachable in debug builds` | no | no | no | NO | "if" naming the actual conditional construct being described, code reference despite zero mechanical feature. |
| 27 | `public opinion on this deprecation has been mixed` | no | no | no | YES | "public" used as an ordinary noun ("public opinion"), prose. |
| 28 | `public constructor here is only meant for framework use` | no | no | no | NO | "public" naming the actual access modifier being described, code reference despite zero mechanical feature. |
| 29 | `record label released this album decades before streaming` | no | no | no | YES | "record" used as an ordinary noun ("record label"), prose false-friend case. |
| 30 | `record type here is only used to carry parsed config values` | no | no | no | NO | "record" naming the actual `record` type keyword being described, code reference despite zero mechanical feature. |

Rows 31-38 added 2026-08-01 (second corpus-growth pass this session), covering `KEYWORDS_JAVA`
members that still had zero example rows after rows 23-30 landed: `break`, `catch`, `finally`,
`package`.

| 31 | `break in on this conversation for a second, that assumption is wrong` | no | no | no | YES | "break" in the ordinary English idiom ("break in on"), prose. |
| 32 | `break here only exits the inner loop, not the outer one` | no | no | no | NO | "break" naming the actual break statement being described, code reference despite zero mechanical feature. |
| 33 | `catch me up on why this workaround still exists` | no | no | no | YES | "catch" as an ordinary English verb ("catch me up"), prose. |
| 34 | `catch block here logs the error before rethrowing it` | no | no | no | NO | "catch" naming the actual catch-block construct being described, code reference despite zero mechanical feature. |
| 35 | `finally got this build green after three days of chasing flaky tests` | no | no | no | YES | "finally" as an ordinary English adverb, prose. |
| 36 | `finally block here always runs, even when the try returns early` | no | no | no | NO | "finally" naming the actual finally-block construct being described, code reference despite zero mechanical feature. |
| 37 | `package deal includes both the client and server libraries` | no | no | no | YES | "package" used as an ordinary noun ("package deal"), prose. |
| 38 | `package declaration here must match the directory structure exactly` | no | no | no | NO | "package" naming the actual package-declaration keyword being described, code reference despite zero mechanical feature. |

Rows 39-92 added 2026-08-02 (GRU misclassification-driven corpus growth, same session as
`examples_c.md`'s rows 38-76 — see that file's note and `/tmp/gru_misclassified.txt`). Rows 39-58
add paraphrased variants of the keywords the GRU model got wrong on this exact zero-feature NO
shape (`transient`, `while`, `do`, `var`, `switch`, `this`, `native`, `abstract`, `record`,
`package`); rows 59-92 cover `KEYWORDS_JAVA` members that still had zero example rows in this
file.

| 39 | `transient marker here tells the JVM to skip this field during serialization` | no | no | no | NO | "transient" naming the actual field modifier being described, code reference despite zero mechanical feature. |
| 40 | `transient here was added after a NotSerializableException in production` | no | no | no | NO | "transient" naming the actual keyword being described, not the English adjective. |
| 41 | `while here re-checks the queue size before pulling the next item` | no | no | no | NO | "while" naming the actual loop construct being described, code reference despite zero mechanical feature. |
| 42 | `while loop here was chosen over a stream to avoid boxing overhead` | no | no | no | NO | "while" naming the actual loop construct being contrasted, not the English conjunction. |
| 43 | `do block here always executes once before the condition is tested` | no | no | no | NO | "do" naming the actual `do`/`while` construct being described, code reference despite zero mechanical feature. |
| 44 | `do here pairs with the while at the bottom of this loop` | no | no | no | NO | "do" naming the actual keyword being explained, not the English auxiliary verb. |
| 45 | `var here lets the compiler infer the long generic type on the right` | no | no | no | NO | "var" naming the actual local-variable-inference keyword being described, code reference despite zero mechanical feature. |
| 46 | `var inference here only works because the right-hand side is unambiguous` | no | no | no | NO | "var" naming the actual keyword being described, not an English noun. |
| 47 | `switch here was rewritten as an expression to drop the fallthrough bug` | no | no | no | NO | "switch" naming the actual construct being described, code reference despite zero mechanical feature. |
| 48 | `switch statement here has no default arm, which is intentional` | no | no | no | NO | "switch" naming the actual construct being described, not the English verb. |
| 49 | `this reference here escapes through the listener registered in the constructor` | no | no | no | NO | "this" referring to the language's actual `this` reference, code reference despite zero mechanical feature. |
| 50 | `this field here is only ever mutated from the event-dispatch thread` | no | no | no | NO | "this" naming the actual field-qualification usage being described, not free English prose. |
| 51 | `native library here is loaded once in a static initializer` | no | no | no | NO | "native" naming the actual method modifier being described, code reference despite zero mechanical feature. |
| 52 | `native call here crosses into the platform's C implementation` | no | no | no | NO | "native" naming the actual keyword being described, not an English adjective. |
| 53 | `abstract class here defines the shared template method for every subclass` | no | no | no | NO | "abstract" naming the actual class modifier being described, code reference despite zero mechanical feature. |
| 54 | `abstract here forces every subclass to supply its own implementation` | no | no | no | NO | "abstract" naming the actual keyword being described, not an English adjective. |
| 55 | `record here is only used to carry the parsed response, nothing else` | no | no | no | NO | "record" naming the actual `record` type keyword being described, code reference despite zero mechanical feature. |
| 56 | `record component here is validated in the compact constructor` | no | no | no | NO | "record" naming the actual type keyword being described, not the English noun. |
| 57 | `package here must exactly match this file's directory on disk` | no | no | no | NO | "package" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 58 | `package visibility here is the default when no modifier is written` | no | no | no | NO | "package" naming the actual access-level keyword being described, not the English noun. |
| 59 | `assert here only runs when assertions are enabled with -ea` | no | no | no | NO | "assert" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 60 | `boolean here defaults to false when the field is left unset` | no | no | no | NO | "boolean" naming the actual primitive type being described, not an English noun. |
| 61 | `byte here is the smallest signed integer type available` | no | no | no | NO | "byte" naming the actual primitive type being described, code reference despite zero mechanical feature. |
| 62 | `char here holds a single UTF-16 code unit, not a full character` | no | no | no | NO | "char" naming the actual primitive type being described, not an English noun. |
| 63 | `class here intentionally has no public constructor` | no | no | no | NO | "class" naming the actual type keyword being described, code reference despite zero mechanical feature. |
| 64 | `const here is a reserved word but was never actually implemented` | no | no | no | NO | "const" naming the actual reserved keyword being described, code reference despite zero mechanical feature. |
| 65 | `continue here skips straight to the next iteration's condition check` | no | no | no | NO | "continue" naming the actual loop-control keyword being described, code reference despite zero mechanical feature. |
| 66 | `default here is only reached when none of the case labels match` | no | no | no | NO | "default" naming the actual switch label being described, code reference despite zero mechanical feature. |
| 67 | `else here only runs when the cache lookup misses` | no | no | no | NO | "else" naming the actual conditional branch being described, code reference despite zero mechanical feature. |
| 68 | `enum here lists every valid status the workflow can be in` | no | no | no | NO | "enum" naming the actual enumeration type being described, code reference despite zero mechanical feature. |
| 69 | `extends here pulls in the base class's protected helper methods` | no | no | no | NO | "extends" naming the actual inheritance keyword being described, code reference despite zero mechanical feature. |
| 70 | `final here prevents this reference from ever being reassigned` | no | no | no | NO | "final" naming the actual modifier keyword being described, not an English adjective. |
| 71 | `for here iterates the map's entry set directly to avoid boxing keys twice` | no | no | no | NO | "for" naming the actual loop construct being described, code reference despite zero mechanical feature. |
| 72 | `goto here is a reserved word but has never been usable in Java` | no | no | no | NO | "goto" naming the actual reserved keyword being described, code reference despite zero mechanical feature. |
| 73 | `implements here brings in the interface's abstract methods to fill in` | no | no | no | NO | "implements" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 74 | `import here is only needed because the class lives in another package` | no | no | no | NO | "import" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 75 | `instanceof here replaces the older cast-and-check pattern` | no | no | no | NO | "instanceof" naming the actual operator being described, code reference despite zero mechanical feature. |
| 76 | `interface here declares only the contract, no implementation` | no | no | no | NO | "interface" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 77 | `permits here restricts which classes may extend this sealed type` | no | no | no | NO | "permits" naming the actual sealed-class keyword being described, code reference despite zero mechanical feature. |
| 78 | `private here keeps this helper hidden from the rest of the package` | no | no | no | NO | "private" naming the actual access modifier being described, not an English adjective. |
| 79 | `protected here lets subclasses override this without exposing it publicly` | no | no | no | NO | "protected" naming the actual access modifier being described, code reference despite zero mechanical feature. |
| 80 | `sealed here restricts which classes may extend this type at all` | no | no | no | NO | "sealed" naming the actual class modifier being described, code reference despite zero mechanical feature. |
| 81 | `static here means this field is shared across every instance` | no | no | no | NO | "static" naming the actual modifier keyword being described, not an English adjective. |
| 82 | `strictfp here forces identical floating-point results across platforms` | no | no | no | NO | "strictfp" naming the actual modifier keyword being described, code reference despite zero mechanical feature. |
| 83 | `super here calls the base class's constructor before this one runs` | no | no | no | NO | "super" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 84 | `synchronized here blocks any other thread from entering this method` | no | no | no | NO | "synchronized" naming the actual modifier keyword being described, code reference despite zero mechanical feature. |
| 85 | `throw here only fires once the retry budget is exhausted` | no | no | no | NO | "throw" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 86 | `throws here declares the checked exception this method can propagate` | no | no | no | NO | "throws" naming the actual declaration keyword being described, code reference despite zero mechanical feature. |
| 87 | `try here wraps only the risky call, not the whole method body` | no | no | no | NO | "try" naming the actual block keyword being described, code reference despite zero mechanical feature. |
| 88 | `void here means this method returns nothing to the caller` | no | no | no | NO | "void" naming the actual return-type keyword being described, not an English adjective. |
| 89 | `volatile here guarantees every thread sees the latest write` | no | no | no | NO | "volatile" naming the actual modifier keyword being described, code reference despite zero mechanical feature. |
| 90 | `yield here produces the switch expression's value for this branch` | no | no | no | NO | "yield" naming the actual keyword being described, code reference despite zero mechanical feature. |
| 91 | `null here is the sentinel returned when the lookup finds nothing` | no | no | no | NO | "null" naming the actual literal being described, not an English adjective. |
| 92 | `true here is the default before the health check runs the first time` | no | no | no | NO | "true" naming the actual literal being described, not an English adjective. |
| 93 | `this reference here escapes the constructor before the object is fully built` | no | no | no | NO | "this" naming the actual `this` reference being described in flowing prose, code reference despite zero mechanical feature. |
| 94 | `this builder was refactored last sprint to avoid the mutable-state bug` | no | no | no | NO | "this" naming the specific builder class under discussion, a code-referencing usage despite reading as an ordinary demonstrative pronoun. |
| 95 | `this is the third time this exact race condition has come up in review` | no | no | no | YES | "this" as an ordinary English demonstrative pronoun opening a sentence, prose. |
| 96 | `this module needs its own changelog entry before the release goes out` | no | no | no | YES | "this" as an ordinary English demonstrative pronoun, prose about the module, not a code construct. |
| 97 | `static field here is initialized once per classloader, not per instance` | no | no | no | NO | "static" naming the actual field-storage keyword being described, code reference despite zero mechanical feature. |
| 98 | `static was picked here deliberately to avoid a per-request allocation` | no | no | no | NO | "static" naming the actual keyword under retrospective discussion, code reference despite zero mechanical feature. |
| 99 | `static analysis flagged this resource leak during the nightly build` | no | no | no | YES | "static" as part of the ordinary English phrase "static analysis", not the modifier keyword. |
| 100 | `static electricity kept tripping the sensor, not anything in this code` | no | no | no | YES | "static" as an ordinary English noun (static electricity), prose unrelated to the keyword. |
