# STYLE_KOTLIN.md — Kotlin Specific Style Rules (1.0–1.9 baseline)

Read [STYLE.md](STYLE.md) first. This file extends and overrides it for Kotlin.
Rules here apply to any Kotlin source compiled against language level 1.0 through
1.9 — nothing here depends on a 2.0+ compiler or stdlib feature. See
[STYLE_KOTLIN2.md](STYLE_KOTLIN2.md) for 2.0+ additions.

Where a construct has a direct Java analog (classes, methods, generics, modifiers,
alignment), this file states "same as STYLE_JAVA.md §N" rather than re-deriving the
rule. Sections below are Kotlin-specific or meaningfully different from Java.

---

## 1. Semicolons

Kotlin statement-terminating `;` is optional in almost all positions. The formatter
normalizes: **strip all optional `;`**. Since the formatter already enforces
one-statement-per-line (STYLE.md's line-length/splitting rules), there is virtually
no legitimate case where `;` is needed to separate statements.

`;` is **kept** only where the language requires it:

- Enum entries followed by member declarations (§2 below).
- Multiple statements deliberately kept on one line (rare; formatter's own
  splitting makes this effectively moot in practice).

```kotlin
val x = 1        // ";" stripped
val y = 2

fun foo() {
    doSomething() // ";" stripped
}
```

---

## 2. `enum class` with Members

Direct analog of the C/Java `enum { ... ; ... }` pattern (STYLE.md §13 spirit,
STYLE_JAVA.md §6). When enum entries are followed by member declarations, the `;`
separating them is mandatory in Kotlin — kept, and given the same blank-line
emphasis formatting as Java:

```kotlin
enum class XXX {

    AAA, BBB, CCC

    ;

    fun whatever()
    {
        ...
    }

} // enum class XXX
```

**Trailing comma** after the last entry (`AAA, BBB, CCC,`) is preserved exactly
as written — the formatter neither adds nor removes it. It is a separate,
independent detail from the mandatory `;`; the two are not interchangeable
(`,` separates entries from each other, `;` separates the entry list from members).

Enum body brace is K&R, same as `class`/`interface` (§4), and always receives the
closing comment (STYLE.md §7), same as Java.

---

## 3. Brace Style

Same split as STYLE_JAVA.md §2:

- **Function/method bodies only** — Allman, `{` on its own line.
- **Everything else** — `class`, `object`, `companion object`, `interface`,
  `enum class` body braces, and all control-flow blocks (`if`, `else`, `for`,
  `while`, `when`, `try`, `catch`, `finally`) — K&R, `{` on the same line.

```kotlin
fun whatever(x: Int): Int
{
    if(x > 0) {
        return x
    }
    else {
        return -x
    }
}
```

**Lambda expressions** are always K&R regardless of position — same exception as
STYLE_JAVA.md §2's lambda note. This includes trailing-lambda call syntax
(`launch { }`, `apply { }`, `also { }`) and delegate initializers (`by lazy { }`):

```kotlin
launch {
    doWork()
}

val data: String by lazy {
    computeExpensiveValue()
}
```

### 3.1 Class / Object / Companion Object Bodies

`class`, `object`, and `companion object` body braces are K&R, always receive a
blank line after `{` and before `}` regardless of content length, and always
receive the closing comment (STYLE.md §7's named-construct rule) — not gated by
the `closing-comment-min-lines` threshold, same as `class` in Java/C++:

```kotlin
class Foo {

    companion object {

        const val MAX = 100

    } // companion object

} // class Foo
```

Anonymous `object : Interface { }` expressions follow the same K&R rule; closing
comment omits the name, same as Java's anonymous-class convention
(STYLE_JAVA.md §6): `} // object`.

### 3.2 `catch`/`for`/`while`/`when` — No Space Before `(`

Consistent with `if(...)` (STYLE.md), all control-flow keywords with a parenthesized
head drop the space before `(`: `if(...)`, `when(...)`, `for(...)`, `while(...)`,
`catch(...)`.

```kotlin
try {
    doWork()
}
catch(e: IOException) {
    handleError(e)
}
finally {
    cleanup()
}
```

---

## 4. `when` Expression

Structural analog of Java's arrow-form `switch` expression (STYLE_JAVA17.md §3.1),
brought into the 1.9 baseline since Kotlin's `when` has always used `->`.

```kotlin
when(x) {

    1    -> foo()
    2    -> bar()
    else -> qux()

} // when x
```

- `->` is column-aligned across branches, same spirit as STYLE.md's alignment
  rules and STYLE_JAVA17.md §3.1.
- Blank line inserted after `when(x) {` before the first branch, and before the
  closing `}`, matching Java's `switch` blank-line treatment.
- Closing comment `// when x` follows STYLE.md §7's construct-labeling rule.
- **Branch bodies never get a closing comment**, regardless of length — matches
  Java: only the top-level construct is labeled, not individual `case`/branch
  blocks.
- A branch needing a block body still opens `->` K&R-style, same line:

```kotlin
when(x) {

    1    -> foo()
    2    -> {
        bar()
        baz()
    }
    else -> qux()

} // when x
```

---

## 5. Null-Safety Operators

`?.` and `!!` are tight (no surrounding space), same treatment as C/C++ `*`/`&`
(STYLE.md's pointer/reference spacing). `?:` is spaced, treated as a normal binary
operator (like `&&`, `+`):

```kotlin
val len    = str?.length ?: 0
val name   = user!!.name
val active = true
```

```kotlin
// NOT preferred — do not space ?. or !!
val len = str ?. length
val name = user !! .name
```

---

## 6. Variable / Property Declaration Alignment

Same column-alignment grid as STYLE.md §5, extended for Kotlin's declaration shape.
Slot order (left to right):

```
[visibility] [modifiers: open/final/abstract/sealed/const/override/lateinit/...] [val|var] [name] : [type]
```

`val`/`var` are mutually exclusive, so they share **one** column slot (not two).
`:` before the type gets a leading space when part of an aligned group, so the `:`
column itself lines up, detached from the variable name:

```kotlin
public  open     var x : Int
private          val y : String
         override var z : Long
```

Assignment `=` aligns the same way as Java/C++ (STYLE.md §5) whenever a group of
consecutive declarations/assignments share the construct:

```kotlin
val len    = str?.length ?: 0
val name   = user!!.name
val active = true
```

---

## 7. Constructor / Function Parameter Lists

Same line-breaking rule as function signatures (STYLE.md §8): inline if it fits
within the 100-char soft limit; otherwise break one parameter per line with the
closing `)` on its own line (STYLE.md's "ensure `)` is on its own line" rule).

**Short — stays inline:**
```kotlin
data class Point(val x: Int, val y: Int)
```

**Long — breaks, `:` aligned with a leading space so the column detaches from the
name, same as §6:**
```kotlin
data class User(
    val id    : Long,
    val name  : String,
    val email : String,
    val age   : Int
)
```

### 7.1 Named / Default Arguments

`=` in default parameter values and named-argument call sites is spaced, consistent
with every other `=` in this style (not tight):

```kotlin
fun foo(x: Int = 10, y: Int = 20) { }

foo(x = 1, y = 2)
```

When a named-argument call breaks past 100 chars, `=` aligns across the broken
lines, same as §6:

```kotlin
foo(
    x = 1,
    y = 2,
    z = 3
)
```

---

## 8. Property Accessors (`get`/`set`)

Accessors have exactly two legal forms in Kotlin — expression body or block body —
there is no third "bare statement" form (unlike `if(x) return y;` in STYLE.md §10,
which does not apply here). The formatter preserves whichever form is present; it
does not convert between them.

**Expression body** — no braces, stays inline:
```kotlin
val isValid: Boolean
    get() = value > 0
```

**Block body** — always keeps `{}`, even for a single statement, K&R brace
(same as §3):
```kotlin
var count: Int = 0
    set(v) {
        field = v.coerceAtLeast(0)
    }
```

Standalone one-liner methods/accessors as part of an aligned group follow the
getter/setter group alignment rule (STYLE.md §14 / STYLE_JAVA.md §5).

---

## 9. `for` Loops and Ranges

`for(...)` follows the same tight/loose complexity-padding detector as `if`/`while`
conditions (STYLE.md §3.1), extended with the range keywords `in`, `until`,
`downTo`, `step` as recognized operator tokens:

```kotlin
for(i in 1..10) { ... }
for(i in 1 until 10 step 2) { ... }
for(i in 10 downTo 1) { ... }
```

`..` (range operator) stays tight; `until`, `downTo`, `step`, and `in` are
word-operators and get normal spacing — same tight/loose rule applies once nested
`()`/`[]` appear inside the loop head.

---

## 10. Destructuring Declarations

```kotlin
val (a, b) = pair
val (id, name, _) = user
```

Comma spacing matches function parameter lists (space after, none before). `=`
aligns across a group of consecutive destructuring/assignment declarations, same
as §6, unless there's an outlier breaking the group.

**No forced line-break past 100 chars.** Unlike constructor params (§7), a
destructuring list has no type annotations to anchor a column grid, so a forced
one-per-line break gains no readability. This is a deliberate, documented
exception to the general >100-char breaking rule: destructuring lists are allowed
to overflow.

---

## 11. Generics: Variance (`in`/`out`)

Normal generic-type spacing (tight against `<`), with `in`/`out` inserted as a
token before the type parameter:

```kotlin
class Box<out T>
class Consumer<in T>
interface Function<in P, out R>
```

Nested `<>` get spacing for readability, same as the existing Java/C++
nested-generic rule.

---

## 12. Infix Functions

`infix` is a modifier token occupying the same slot-handling as `suspend`/
`inline`/etc. (§6). Call-site usage is a word-operator, spaced like `until`/
`downTo` (§9):

```kotlin
infix fun Int.times(str: String): String = ...

val result = 3 times "abc"
```

---

## 13. Annotation Use-Site Targets

`@field:`, `@get:`, `@param:`, `@set:`, etc. — `:` is tight between the annotation
name and its use-site target. Annotation placement (own line vs. inline) follows
whatever convention is already established for Java annotations (`@Override`, etc.):

```kotlin
@field:JvmField
val x: Int = 0
```

---

## 14. Lambda-with-Receiver / Function Types

`Type.(Params) -> ReturnType` is recognized as a single function-type token, not
a nested-paren construct — exempt from the tight/loose nesting detector the same
way `int[]` is exempt in Java (STYLE_JAVA.md), regardless of whether its own
parameter list is empty or populated:

```kotlin
fun build(block: StringBuilder.() -> Unit) { }
fun build(block: StringBuilder.(Int, String) -> Unit) { }
```

`->` is spaced, same as `when`'s arrow (§4) and normal function-type arrows.

**Known gap:** a function type nested as a *parameter* of another function type
(e.g. `((Int) -> String, Boolean) -> Unit`) is genuine nesting and is not yet
handled by the exemption above — falls back to default spacing, may not look
ideal. See STYLE.md Known Gaps section.

---

## 15. `vararg`

Modifier token on a parameter, same slot-position handling as Java varargs
(`Object... args`), just spelled as a leading keyword instead of trailing `...`:

```kotlin
fun foo(vararg args: Int) { }
```

---

## 16. String Templates

`"$x"` (bare) vs `"${x}"` (braced) — **preserved exactly as the user wrote it.**
No normalization in either direction. Same "don't impose an opinion the input
didn't have" posture as enum trailing-comma handling (§2).

---

## 17. Sealed Classes / Interfaces

Subtypes (nested or file-level `class`/`object` declarations) follow normal
`class`/`object` K&R rules (§3, §3.1) — no special layout beyond that.

---

## 18. Type Aliases

`typealias Foo = ...` is a single-line declaration; treated like any other
single-line `val`-style statement for spacing (`=` spaced, normal token spacing).

---

## 19. Extension Functions

`fun String.foo()` — `fun` is just a keyword/modifier token like `static`; no
special treatment for the receiver-type prefix beyond normal token spacing.

---

## 20. Known Gaps

- Doubly-nested function types as parameters of an outer function type
  (§14) — not yet detected/exempted correctly by the tight/loose detector.

---

## 21. Resolved Design Decisions (Q&A session)

| Topic | Decision |
|---|---|
| Kotlin version split | Baseline file covers 1.0–1.9 as a single bucket (no 1.8/1.9 sub-split); STYLE_KOTLIN2.md covers 2.0+ only |
| Semicolons | Stripped except where mandatory (enum + members) |
| Enum trailing comma | Preserved as-is, independent of the mandatory `;` |
| Brace style | Allman for function/method bodies only; K&R everywhere else, matching STYLE_JAVA.md exactly |
| `catch`/`for`/`while`/`when` head spacing | No space before `(`, matching `if(...)` |
| `when` | Same arrow-alignment posture as Java's switch expression; branch bodies never get closing comments |
| `?.` / `!!` | Tight, like `*`/`&` |
| `?:` | Spaced, like a normal binary operator |
| `val`/`var` alignment | Share one column slot (mutually exclusive) |
| `:` alignment | Leading space before `:` when aligned, so the column detaches from the name |
| Constructor param breaking | Same as function signatures — one-per-line, `)` on own line, `:` aligned |
| Named/default arg `=` | Spaced, aligns across broken lines |
| Property accessors | Two legal forms only (expression body / block body); formatter preserves whichever is present, never collapses a block to bare-statement form |
| Destructuring | No forced break past 100 chars — allowed to overflow, since there's no alignment anchor |
| String templates | Preserved exactly as written, no normalization |
| Lambda-with-receiver `()` | Exempt from nesting detector as a function-type token; doubly-nested case flagged as a known gap |
