# STYLE_JAVA.md — Java Specific Style Rules

Read [STYLE.md](STYLE.md) first. This file extends and overrides it for Java.

---

## 1. Empty Parameter Lists

Omit `void` — use empty parens: `void foo()`

---

## 2. Function / Method Brace Style

Function definitions always use **Allman style** — opening `{` on its own line

```java
void process()
{
    doSomething();
    doMore();
}
```

One-liner methods follow the getter/setter group rule from STYLE.md §12 when
they appear as part of an aligned group. Standalone one-liners:

```java
void reset()
{ _x = 0; }
```

---

## 3. `if` Spacing When Body is `{}`

When an `if` condition is followed by a `{}` block on the same line,
apply loose padding to the condition — even if the condition alone would be tight:

```java
if( list.get(i) ) {}     // call inside () — pad
if(a == 1) {}            // simple — no pad (condition is tight, no call/nesting)
```

Rule: the presence of `{}` on the same line does **not** by itself trigger padding —
the condition complexity rule (STYLE.md §3.1) still applies. Only a call or nested
expression inside the condition triggers padding.

---

## 4. `{}` Initializer Spacing

Single-level with content — pad:
```java
int[] x = { 1, 2, 3 };
```

Nested — both levels pad:
```java
int[][] x = { { 1, 2 }, { 3, 4 } };
```

Empty — tight:
```java
int[] x = {};
```

**Double-brace initialization** — leave tight, padding would cause visual confusion:
```java
Map<String, Integer> m = new HashMap() {{ put("a", 1); }};
```

---

## 5. Getter/Setter/Checker Group Alignment

Applies to methods inside `class`, `interface`, `enum` bodies.
Same rules as STYLE.md §12.

```java
public void setX(int x)  { _x = x;       }
public int  getX(      ) { return _x;     }
public bool isValid(   ) { return _x > 0; }
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
}
```

---

## 7. Unresolved / Preserve-As-Is Cases

- `else`/`else if` closing comment threshold: same judgment-call rule as C/C++ —
  apply when the branch is long and contains deeply nested `if`s inside.
