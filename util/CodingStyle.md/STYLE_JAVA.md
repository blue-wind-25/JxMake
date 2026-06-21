# STYLE_JAVA.md — Java Specific Style Rules

Read [STYLE.md](STYLE.md) first. This file extends and overrides it for Java.

---

## 1. Empty Parameter Lists

Omit `void` — use empty parens: `void foo()`

---

## 2. Method Brace Style

**Method definitions only** use **Allman style** — opening `{` on its own line:

```java
void process()
{
    doSomething();
    doMore();
}
```

Class, interface, and enum body braces, as well as all control-flow blocks (`if`, `for`,
`while`, `switch`, `try`, etc.), use **K&R style** — opening `{` on the same line. See
STYLE.md §11.

```java
public class Foo {

    void process()
    {
        if(ready) {
            doSomething();
        }
    }

} // class Foo
```

One-liner methods follow the getter/setter group rule from STYLE.md §14 when
they appear as part of an aligned group. Standalone one-liners:

```java
void reset()
{ _x = 0; }
```

**Lambda expressions** are an exception to Allman: like other non-function blocks
(STYLE.md §11), a block-bodied lambda's `{` stays on the same line as its parameter
list and `->`:

```java
Runnable r = () -> {
    doSomething();
};

list.sort( (a, b) -> {
    return a.compareTo(b);
} );
```

---

## 3. `if` Spacing When Body is `{}`

When an `if` condition is followed by a `{}` block on the same line,
apply loose padding to the condition — even if the condition alone would be tight:

```java
if( list.get(i) ) {} // call inside () — pad
if(a == 1) {}        // simple — no pad (condition is tight, no call/nesting)
```

Rule: the presence of `{}` on the same line does **not** by itself trigger padding —
the condition complexity rule (STYLE.md §3.1) still applies. Only a call or nested
expression inside the condition triggers padding.

---

## 4. `{}` Initializer Spacing

Same rules as STYLE.md §3.3, with Java array-declaration syntax (`int[] x` not `int x[]`):

```java
int[]   x  = { 1, 2, 3 };            // single-level — pad
int[][] xy = { { 1, 2 }, { 3, 4 } }; // nested       — both levels pad
int[]   z  = {};                     // empty        — tight
```

**Double-brace initialization** — leave tight; padding would cause visual confusion:
```java
Map<String, Integer> m = new HashMap<>() {{ put("a", 1); }};
```

---

## 5. Getter/Setter/Checker Group Alignment

Applies to methods inside `class`, `interface`, `enum` bodies.
Same rules as STYLE.md §14.

```java
public void    setX   (int x) { _x = x;        }
public int     getX   (     ) { return _x;     }
public boolean isValid(     ) { return _x > 0; }
```

Alignment spans access modifier, return type, method name, parameters, body, and `}`.

---

## 6. Closing Comments

Same rules as STYLE.md §7, applied to Java constructs:

```java
} // class MyClass
} // interface Runnable
} // enum Color
```

Anonymous classes — no label:
```java
} // class
```

---

## 7. Import Ordering

Imports are arranged in groups separated by **exactly 1 blank line**.
Within each group, imports are sorted alphabetically (configurable).

**Default group order:**

```java
import java.*;              // Group 1: java.* and javax.*
import javax.*;

import com.*;               // Group 2: com.*

import org.*;               // Group 3: org.*

import <other>.*;           // Group 4: <other>.*

import mycompany.myproject; // Group 5: local / in-project imports

import static ...;          // Group 6: static imports
```

**Local import detection:** the project's root package is read from the `package`
declaration at the top of the file being formatted. The top two package components
(e.g. `com.mycompany` from `package com.mycompany.myproject.audio;`) define the
local prefix. All imports sharing that prefix are placed in group 5.

Configurable:
```
java-import-order       = static, java, org, com, local   # group order
java-import-sort        = on                              # alphabetical within group
java-import-depth       = 2                               # components defining "local"
java-import-blank-lines = 1                               # blank lines between groups
```

**Unused imports** — the formatter does not remove unused imports. That is the
responsibility of the IDE or a separate lint tool.

---

## 8. Unresolved / Preserve-As-Is Cases

- `else`/`else if` closing comment threshold: same judgment-call rule as C/C++ —
  apply when the branch is long and contains deeply nested `if`s inside.
