# STYLE.md — Code Style Guide (Entry Point)

This file defines **common formatting rules** that apply across all supported languages.
Language-specific rules are in separate files — read them after this one:

- C and C++: see [STYLE_C_CPP.md](STYLE_C_CPP.md)
- Java:      see [STYLE_JAVA.md](STYLE_JAVA.md)

When a language-specific rule conflicts with a common rule, the language-specific rule wins.

---

## 1. Indentation

- **4 spaces** per indent level.
- **Tab display size**: 4 spaces.
- **Match the project**: if the project or most files in it already use tabs, continue using tabs.
  When a project mixes tabs and spaces, use whichever style the **majority of files** in
  that project use.
- **Do not convert** existing indent style (tabs↔spaces) unless the file is inconsistent
  with surrounding files in the same module/directory. If conversion is needed, convert
  the entire file, not just the lines being edited.

---

## 2. Line Length

- Soft limit: **100 characters**.
- Function signatures that exceed 100 chars when written inline must be broken —
  see §8 (Function Signatures).

---

## 3. Bracket and Parenthesis Spacing

### 3.1 Complexity-based padding rule

The decision to pad inside `()`, `[]`, or `{}` depends on the **complexity of the content**,
evaluated bottom-up from the innermost expression.

| Content inside bracket        | Spacing  | Example                        |
|-------------------------------|----------|--------------------------------|
| Atoms, simple binary ops      | tight    | `if(a && b)`                   |
| Long chain of simple ops      | tight    | `if(a > 0 && b > 0 && c > 0)`  |
| Contains a function call      | loose    | `if( isReady(x) )`             |
| Contains nested `()` or `[]`  | loose    | `if( (a == 1) \|\| (b > 2) )`  |
| Array index with expression   | loose    | `a[ countItems(x, y) ]`        |
| Array index with nested `[]`  | loose    | `a[ b[i] ]`                    |
| Array index with constant     | tight    | `a[10]`                        |
| Array index with simple ops   | tight    | `a[a + 10]`                    |

Nesting propagates outward: if an inner call makes its bracket loose, the outer bracket
that contains it is also loose.

### 3.2 Keyword spacing

No space between control-flow keyword and `(`:

```c
if(x)
while(x)
for(...)
switch(x)
```

### 3.3 `{}` initializer / block spacing

Single-level with content — pad:
```c
int x[] = { 1, 2, 3 }
```

Nested `{}` — both levels pad:
```c
int x[][] = { { 1, 2 }, { 3, 4 } }
```

Empty braces — tight:
```c
int x[] = {}
```

---

## 4. Pre/Post Increment and Decrement

Always use **pre-increment / pre-decrement** (`++i`, `--i`) except when post-increment
semantics are required by the surrounding expression (e.g. `arr[i++]`, `return i--`).

---

## 5. Variable Declaration Alignment

Declarations in the same logical group are column-aligned across:
`[modifiers] [type*] [name[size]]`

```c
static volatile       uint8_t        buffer[64]; /* A Comment */
static volatile       uint8_t*       buffer;
static volatile const uint8_t* const buffer;
static                uint16_t       timeout;    /* Another Comment */
                      uint8_t        flags;
static          const char*          name;
                      char           label[MAX];
```

```java
public  static volatile int[]  buffer;     // A Comment
public  static volatile String data;
private static volatile char   mode;
private static          long   timeout;    // Another Comment
                        int    flags;
private static          String name;
                        char[] label;
```

Rules:
- For pointer and `const` placement in C/C++, see STYLE_C_CPP.md §4.
- **`static` declarations come first** in a group, unless a non-static is needed
  as a size or value dependency for a static — in that case keep the dependency
  immediately before the static that uses it.
- If reordering safety is unclear, **preserve relative order**.
- `//` and `/* .. */`are aligned after the field name and array size.
- A blank line between declaration groups **resets alignment** — each group aligns
  independently.

---

## 6. Assignment and Compound Operator Alignment

Align the `=` column across assignments in the same logical group.
Compound operators (`|=`, `&=`, `>>=`, etc.) align their `=` with the group:

```c
flags    = 0x01;
flags   |= 0x02;
flags   &= ~0x04;
flags  >>= 2;
timeout  = 100;
```

**Grouping rules:**
- Variables that are semantically related belong in the same alignment group.
- Unrelated variables: insert a blank line and start a new alignment group.
- A lone variable with no group neighbors: no forced blank line, align trivially
  with itself — do not leave it awkwardly padded.

---

## 7. Closing Comments on Blocks

Add a closing comment after `}` when the **content of the block exceeds 5 lines**.

Format:
- Default: `// block-name` (e.g. `// for`, `// while`, `// MyClass`)
- When multiple control-flow blocks are nested simultaneously, include the key variable
  to make clear which block is closing — `// for i`, `// while running`

```c
for(int i = 0; i < n; ++i) {
    for(int j = 0; j < m; ++j) {
        ...
        ...
        ...
        ...
        ...
        ...
    } // for j

    ...
    ...
    ...
    ...
    ...
    ...
} // for i
```

**Always** include the name for named constructs regardless of nesting depth.
`class` and `enum` are universal. See language-specific files for additional
constructs (`struct`, `namespace` in C/C++; `interface` in Java).

```c
} // class MyClass
} // enum Color
```

**Never** add closing comments on:
- `case` labels
- Naked compound blocks `{ ... }`
- `else` / `else if` — unless the branch is long *and* contains deeply nested `if`s inside

---

## 8. Function Signatures

**Inline** if the full signature fits within 100 chars:
```c
void foo(int x, uint8_t flags, const char* name)
```

**Break to one-per-line** otherwise, with parameters column-aligned:
```c
void reallyLongFunctionNameHere(
    const char*  name,
    uint8_t      flags,
    uint16_t     timeout
)
```

Parameter alignment follows the same declaration alignment rules (§5).

---

## 9. Blank Line Before `return`

Add a blank line before `return` when:
- The function body is multi-line, **and**
- The `return` is at function scope (not inside a nested block)

Do **not** add a blank line before `return` in:
- One-liner functions: `void f() { _done = true; return y; }`
- Single-expression `if`: `if(x) return y;`

---

## 10. Single-Expression Blocks

Omit `{}` when the controlled body is a single expression:
```c
if(x) return y;
if(x) continue;
if(x) break;
```

---

## 11. Non-function Block Brace Style

Control-flow and container blocks — `if`, `else`, `else if`, `for`, `while`, `do`, `switch`,
`try`, `catch`, and class/interface/enum body braces — use **K&R style**: opening `{` on the
same line as the keyword or declaration, not on its own line.

```c
if(x) {
    doSomething();
    doMore();
}

for(int i = 0; i < n; ++i) {
    ...
}

switch(state) {
    ...
}
```

Contrast with **function definitions**, which use Allman style (opening `{` on its own line) —
see the language-specific files.

---

## 12. `else` / `else if` Placement

`else` and `else if` go on their own line, directly after the closing `}` of the preceding block:

```c
if(x) {
    doSomething();
}
else if(y) {
    doOther();
}
else {
    doDefault();
}
```

A **blank line** may be inserted between `}` and `else`/`else if` to separate logically
distinct branches — for example when the preceding branch exits unconditionally (`return`,
`break`, `continue`) and the next branch opens a clearly separate logical path:

```c
if(id == 0) {
    id  = newId;
    cnt = 1;
    return true;
}

else if(id == newId) {
    ++cnt;
    return true;
}

return false;
```

This blank-line grouping is optional and context-driven; do not apply it mechanically.

---

## 13. `switch` Formatting

**Multi-line cases** — blank line after opening `{`, before closing `}`, and between cases:

```c
switch(state) {

    case A: {
            doSomething();
            doMore();
        }
        break;

    case B: {
            ...
        }
        break;

} // switch state
```

**All one-liner cases** — align `:` and `break;` columns, no blank lines:

```c
switch(state) {
    case A: doA(); break;
    case B: doB(); break;
    case C: doC(); break;
}
```

**Fallthrough** — mark explicitly, same indentation level as the next case:

```c
    case A: /* FALL-THROUGH */
    case B: doB(); break;
```

---

## 14. Getter/Setter/Checker Group Alignment

Short functions in a class, struct, or enum body that form a logical group
(getters, setters, checkers) may be written inline as an aligned group:

- Align: `)` column, `{` column, body, `}` column
- Pad empty parameter lists with spaces to match the widest signature
- If one function body is significantly longer than the rest, **exclude it from
  the group** — write it normally, do not let it distort the alignment of the others
- The right `}` of all group members must also align

```cpp
void setX   (int x) { _x = x;        }
int  getX   (     ) { return _x;     }
bool isValid(     ) { return _x > 0; }
```
