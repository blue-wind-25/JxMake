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

Enum body brace is K&R, same as `class`/`interface` (§3), and always receives the
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

### 3.3 Secondary Constructors

A secondary constructor's body is a function-definition body — same treatment as
any other function body (this section's lead rule): **Allman**, `{` on its own
line, regardless of whether it delegates to the primary constructor via
`: this(...)` or to a superclass via `: super(...)`. The delegation clause stays
attached to the constructor's own parameter list, same posture as a Java
constructor's own signature line — it does not get its own line unless the
signature itself needs to break per §7:

```kotlin
class Foo(val x: Int, val y: Int) {

    constructor(x: Int) : this(x, 0)
    {
        // ...
    }

} // class Foo
```

A constructor with no body (`constructor(x: Int) : this(x, 0)`, no trailing `{}`)
needs no brace-style decision at all — it is a single-line delegation-only
declaration.

### 3.4 `init` Blocks

`init { }` is treated as a named construct (STYLE.md §7's named-construct rule) —
K&R brace, always a blank line after `{` and before `}` regardless of content
length, always a closing comment (`// init`), same posture as §3.1's
class/object/companion-object treatment, not gated by
`closing-comment-min-lines`:

```kotlin
class Foo(x: Int) {

    init {

        require(x > 0)

    } // init

} // class Foo
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
- **All-or-nothing**, same as STYLE_JAVA17.md §3.1: if any branch uses a block
  body, `->` alignment is abandoned for the entire `when` — no branch gets
  aligned. A branch needing a block body still opens `->` K&R-style, same line:

```kotlin
when(x) {

    1 -> foo()
    2 -> {
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
val len  = str ?. length
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

### 7.2 Trailing Comma

A trailing comma after the last parameter/argument in a broken (one-per-line)
constructor, function, or call parameter list — `val age : Int,` before the closing
`)` — is **preserved exactly as written**, same "don't impose an opinion the input
didn't have" posture already established for the enum trailing comma (§2). The
formatter neither adds nor removes it in any parameter-list context, not only
enum entries.

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

## 9. Expression-Bodied Functions

`fun foo(): Int = x` is the function-level analog of §8's expression-body
accessor — same preserve-as-is posture: the formatter never converts an
expression body to a block body or vice versa.

**Standalone, fits inline** — left exactly as written:
```kotlin
fun square(x: Int): Int = x * x
```

**Adjacent to other one-liner members** (other expression-bodied functions,
block-bodied one-liners, or accessors) — participates in the same §14/STYLE.md
getter/setter-style aligned group §8 already defers to. An expression-bodied
function is just another one-liner shape for that grouping, not a separate
mechanism.

**Doesn't fit in 100 chars** — break the parameter list first, per §7's existing
rule. If `) : ReturnType = expr` still doesn't fit once the parameter list is
broken, wrap `= expr` onto its own line, indented one level, mirroring how §7.1's
named-argument `=` wraps rather than inventing a new break rule:

```kotlin
fun reallyLongFunctionName(
    x: Int,
    y: Int
): Int =
    x * x + y * y
```

---

## 10. `for` Loops and Ranges

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

## 11. Labeled Jumps

`return@label`, `break@loop`, `continue@loop` — the `@label`/`@loop` part is
spaced like a normal keyword followed by an identifier, not tight:

```kotlin
listOf(1, 2, 3).forEach {
    if(it == 2) return@forEach

    println(it)
}

outer@ for(i in 1..10) {
    for(j in 1..10) {
        if(j == 5) break@outer
    }
}
```

The label declaration itself (`outer@`) is likewise spaced from what follows it.
A jump's value expression, when present (`return@label value`), is spaced from
the label the same way a normal `return value` is spaced from `return`.

---

## 12. Destructuring Declarations

```kotlin
val (a, b)        = pair
val (id, name, _) = user
```

Comma spacing matches function parameter lists (space after, none before). `=`
aligns across a group of consecutive destructuring/assignment declarations, same
as §6, unless there's an outlier breaking the group.

**No forced line-break past 100 chars.** Unlike constructor params (§7), a
destructuring list has no type annotations to anchor a column grid, so a forced
one-per-line break would gain no readability. This is a deliberate, documented
exception to the general >100-char breaking rule: destructuring lists are allowed
to overflow.

The unnamed placeholder `_` (skipping a component) is just another identifier for
spacing purposes — same comma/alignment treatment as any other destructured name,
no special-case needed. See STYLE_KOTLIN2.md §3 for the lambda-parameter form of
the same placeholder.

---

## 13. Generics: Variance (`in`/`out`)

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

## 14. Generic `where` Clause

Multiple upper bounds on a type parameter (`where T : BoundA, T : BoundB`) are a
trailing qualifier on the function signature — same "trailing qualifier attaches
to the signature, breaks only at its own natural token, never restructured
further than necessary" posture already established elsewhere in this style
(e.g. a C++ trailing `requires` clause).

**Fits inline** — `where` stays on the same line as the signature's return type:

```kotlin
fun <T> merge(x: T, y: T): T where T : Comparable<T>, T : Serializable {
    ...
}
```

**Doesn't fit** — `where` drops to its own line, indented one level under `fun`;
bounds break one-per-line at the comma (never at the bound's own `:`, which stays
glued to its type parameter same as every other `:` in this style), column-aligned
under the first bound's type parameter:

```kotlin
fun <T> merge(x: T, y: T): T
    where T : Comparable<T>,
          T : Serializable
{
    ...
}
```

**A single bound line still doesn't fit** — allowed to overflow, same deliberate
exception already established for destructuring lists (§12). There is no
further, finer-grained break point below one-bound-per-line that this style
defines a rule for:

```kotlin
fun <T> merge(x: T, y: T): T
    where T : Comparable<T>,
          T : SuperVeryVeryLongLongNameClass<T>
{
    ...
}
```

---

## 15. Infix Functions

`infix` is a modifier token occupying the same slot-handling as `suspend`/
`inline`/etc. (§6). Call-site usage is a word-operator, spaced like `until`/
`downTo` (§10):

```kotlin
infix fun Int.times(str: String): String = ...

val result = 3 times "abc"
```

---

## 16. Annotation Use-Site Targets

`@field:`, `@get:`, `@param:`, `@set:`, etc. — `:` is tight between the annotation
name and its use-site target. Annotation placement (own line vs. inline) follows
whatever convention is already established for Java annotations (`@Override`, etc.):

```kotlin
@field:JvmField
val x: Int = 0
```

---

## 17. Lambda-with-Receiver / Function Types

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
handled by the exemption above — it falls back to default spacing, which may not
look ideal. See STYLE.md's Known Gaps section.

### 17.1 Lambda Parameter Arrow Spacing

A lambda's own parameter-list arrow (`{ x, y -> x + y }`) is spaced on both
sides, same treatment as the function-type arrow above and `when`'s arrow (§4) —
one consistent arrow-spacing rule across all three constructs, not a
lambda-specific exception:

```kotlin
list.map { x, y -> x + y }

list.map { item ->
    item.transform()
}
```

This applies whether the lambda is single-line or the body spans multiple lines
(K&R brace per §3's lambda exception either way).

---

## 18. `vararg`

Modifier token on a parameter, same slot-position handling as Java varargs
(`Object... args`), just spelled as a leading keyword instead of trailing `...`:

```kotlin
fun foo(vararg args: Int) { }
```

---

## 19. String Templates

`"$x"` (bare) vs `"${x}"` (braced) — **preserved exactly as the user wrote it.**
No normalization in either direction. Same "don't impose an opinion the input
didn't have" posture as enum trailing-comma handling (§2).

---

## 20. Sealed Classes / Interfaces

Subtypes (nested or file-level `class`/`object` declarations) follow normal
`class`/`object` K&R rules (§3, §3.1) — no special layout beyond that.

---

## 21. Type Aliases

`typealias Foo = ...` is a single-line declaration; treated like any other
single-line `val`-style statement for spacing (`=` spaced, normal token spacing).

---

## 22. Extension Functions

`fun String.foo()` — `fun` is just a keyword/modifier token like `static`; no
special treatment for the receiver-type prefix beyond normal token spacing.

---

## 23. Known Gaps

- Doubly-nested function types as parameters of an outer function type
  (§17) — not yet detected/exempted correctly by the tight/loose detector.

---

## 24. Import Ordering

Imports are arranged in groups separated by **exactly 1 blank line**. Within
each group, imports are sorted alphabetically (configurable). Derived directly
from STYLE_JAVA.md §7, with one Kotlin-specific difference: **there is no
`static` group**, because Kotlin has no `import static` keyword — an import of
a companion object member or a top-level function is written with the exact
same `import a.b.c` syntax as any other import, so "this is a static import"
isn't something the tokenizer can tell apart from an ordinary one. Instead, a
new leading group is added for Kotlin's own standard library.

**Default group order:**

```kotlin
import kotlin.*              // Group 1: kotlin.* (Kotlin stdlib)

import java.*                // Group 2: java.* and javax.* (JVM interop)
import javax.*

import com.*                 // Group 3: com.*

import org.*                 // Group 4: org.*

import <other>.*             // Group 5: <other>.*

import mycompany.myproject   // Group 6: local / in-project imports
```

**Local import detection:** identical mechanism to STYLE_JAVA.md §7 — the
project's root package is read from the `package` declaration at the top of
the file being formatted, and the top two package components (e.g.
`com.mycompany` from `package com.mycompany.myproject.audio`) define the local
prefix. All imports sharing that prefix are placed in the local group.

Configurable:
```
kotlin-import-order       = kotlin, java, com, org, other, local   # group order
kotlin-import-sort        = on                                     # alphabetical within group
kotlin-import-depth       = 2                                       # components defining "local"
kotlin-import-blank-lines = 1                                       # blank lines between groups
```

**Unused imports** — the formatter does not remove unused imports, same
posture as STYLE_JAVA.md §7. That is the responsibility of the IDE or a
separate lint tool.

**Import aliases** (`import foo.Bar as Baz`) and wildcard imports
(`import foo.bar.*`) sort and group by their original (pre-alias) qualified
name, not the aliased name — consistent with treating the import statement's
own textual identity, not its usage-site name, as what group/sort order acts
on.
