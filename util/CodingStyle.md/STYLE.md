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
int x[] = { 1, 2, 3 };
```

Nested `{}` — both levels pad:
```c
int x[][2] = { { 1, 2 }, { 3, 4 } };
```

Empty braces — tight:
```c
int x[] = {};
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
static volatile       uint8_t*       ptr;
static volatile const uint8_t* const cptr;
static                uint16_t       timeout;    /* Another Comment */
                      uint8_t        flags;
static          const char*          name;
                      char           label[MAX];
```

```java
public  static volatile int[]  buffer;  // A Comment
public  static volatile String data;
private static volatile char   mode;
private static          long   timeout; // Another Comment
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
- `//` and `/* .. */` are aligned after the field name and array size.
- A blank line between declaration groups **resets alignment** — each group aligns
  independently.

---

## 6. Assignment and Compound Operator Alignment

Align the `=` column across assignments in the same logical group.
Compound operators (`|=`, `&=`, `>>=`, etc.) align their `=` with the group:

```c
flags     = 0x01;
flags    |= 0x02;
flags    &= ~0x04;
flags   >>= 2;
timeout   = 100;
```

**Grouping rules:**
- Variables that are semantically related belong in the same alignment group.
- Unrelated variables: insert a blank line and start a new alignment group.
- A lone variable with no group neighbors: no forced blank line, align trivially
  with itself — do not leave it awkwardly padded.

**Multi-line right-hand sides:**

When an assignment's right-hand side is too long to fit on one line and continues
onto the next, the continuation aligns to wherever the equivalent token would have
started had the whole expression fit on line 1:

Breaking **before** an operator — the operator aligns to the `=` column:
```c
int a = something
      + else;
```

Breaking **after** an operator — the next operand aligns to the column immediately
after `=` (where the first operand began):
```c
int a = something +
        else;
```

This applies in both C/C++ and Java. Ordinary statements do not require a trailing `\`
to continue onto the next line — only macro bodies do. The same alignment target
applies regardless of language or whether `\` happens to be present.

---

## 7. Closing Comments on Blocks

Add a closing comment after `}` when the **content of the block exceeds the closing
comment threshold** (default: 5 lines, configurable via `closing-comment-min-lines`).

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

**Named constructs** (`class`, `struct`, `enum`, `enum class`, `namespace`, `interface`, etc.)
always receive a blank line after `{` and before `}`, regardless of content length:

```cpp
class Foo {

    // ... members ...

} // class Foo
```

For **control-flow blocks** (`for`, `while`, `if`, `switch`): do not add or remove blank lines
inside the block — preserve them as-is. They count toward the closing comment threshold.

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
- `else` / `else if` — never, regardless of length or nesting depth

---

## 8. Function Signatures

**Inline** if the full signature fits within 100 chars:
```c
void foo(int x, uint8_t flags, const char* name)
```

**Break to one-per-line** otherwise, with parameters column-aligned:
```c
void reallyLongFunctionNameHere(
    const char* name,
    uint8_t     flags,
    uint16_t    timeout
)
```

Parameter alignment follows the same declaration alignment rules (§5).

The closing `)` goes on its own line, indented to match the **first character of the
function signature itself** (not indented further to match the parameters):

```c
void reallyLongFunctionNameHere(
    const char* name,
    uint8_t     flags,
    uint16_t    timeout
)
```

Here `)` lines up under `void`, the start of the line the signature begins on.

### Function Calls and Forward Declarations

The rules above apply to **function/method signatures** (parameter lists directly
followed by a body `{`). Function **calls** and **forward declarations** (prototype
parameter lists not followed by a body `{`) follow a simpler rule:

**Inline** if the full call/declaration fits within 100 chars — leave it as-is.

**Otherwise:** attempt the dropped form — all args on one indented line below `(`,
with `)` on its own line at the call's indentation level:

```c
someFunction(
    arg1, arg2, arg3
);
```

If the dropped form still exceeds 100 chars, fall back to **one-per-line**:

```c
someFunction(
    arg1,
    arg2,
    arg3
);
```

**Multi-line source:** if the source already has the args on multiple lines, preserve
the existing line breaks (option 2 — preserve groups). Normalize spacing around `,`
and between tokens within each group line; ensure `)` is on its own line. For forward
declarations, apply the §5 column grid within each group line. For calls, normalize
spacing only — no column grid.

**Comments within arg lists:**
- Trailing comment after an arg — preserve in place.
- Comment-only line between arg groups — preserve in place (incompatible with
  inline/dropped/one-per-line; keeps multi-line grouped form).
- Inline block comment between args — normalize spaces around it, do not move it.
- Leading preamble comment above first arg — preserve entire arg list untouched.

### Alignment of Return Types in Forward Declarations

In C/C++, forward declarations of free functions in the same logical group are
column-aligned across:

```c
static volatile       uint8_t        function1(/* Any */);
static volatile       uint8_t*       func2(/* Any */);
static volatile const uint8_t* const function3(/* Any */);
static                uint16_t       myFunction(/* Any */);
                      uint8_t        theFunction(/* Any */);
static          const char*          aFunction(/* Any */);
                      char           funcX(/* Any */);
```

Java does not have free functions, and thus only governed by §14
(Getter/Setter/Checker Group Alignment).

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
`try`, `catch`, `finally`, and class/interface/enum body braces — use **K&R style**: opening
`{` on the same line as the keyword or declaration, not on its own line.

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

```java
try {
    ...
}
catch(NullPointerException e) {
    ...
}
finally {
    ...
}
```

Contrast with **function definitions**, which use Allman style (opening `{` on its own line) —
see the language-specific files.

**Named constructs** (`class`, `interface`, `enum`, etc.) additionally receive a blank line
after `{` and before `}` regardless of content length — see §7.

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

**Non-inline** — when any case has a multi-line body.

Blank line after the switch's opening `{`, after each `break;` (between cases), and before
the switch's closing `}`. Case body indented **two levels** inside the `case` label (one for
the case `{` block, one for the body inside it); `}` and `break;` share the intermediate level:

```c
switch(cmd) {

    case CMD_READ: {
            status   = readData(buf, len);
            bytesRx += len;
        }
        break;

    case CMD_WRITE: {
            status   = writeData(buf, len);
            bytesTx += len;
        }
        break;

    case CMD_RESET: {
            resetDevice();
            status = STATUS_OK;
        }
        break;

} // switch cmd
```

**Inline** — when every case fits on one line.

No blank lines between cases (preserve any already present in the original). When cases are
structurally similar (all function calls, or all assignments), align: `case` label padded so
`:` is at the same column, then function-name column, `(` column, `)` column, `;` column,
and `break;` column. Add a closing comment when the total line count exceeds the threshold
(see §7):

```c
switch(state) {
    case A : doA    (    )       ; break;
    case B : doLongB(    )       ; break;
    case C : doC    (d, e)       ; break;
    case D : x = funcMath(z) + 10; break;
} // switch state
```

**Fallthrough** — mark explicitly, same indentation level as the next case. In inline
switches the `:` is aligned as above; in non-inline switches no space before `:`:

```c
// Inline:
    case A : /* FALL-THROUGH */
    case B : doB(); break;

// Non-inline:
    case A: /* FALL-THROUGH */
    case B:
        doB();
        break;
```

---

## 14. Getter/Setter/Checker Group Alignment

Short functions in a class, struct, or enum body that form a logical group
(getters, setters, checkers, etc.) may be written inline as an aligned group:

- Align: `)` column, `{` column, body, `}` column
- Pad empty parameter lists with spaces to match the widest signature
- If one function body — when written inline — would push the full line past 100
  characters, **exclude it from the group** — write it normally in Allman style
  below the group. Do not let it distort the alignment of the others
- The right `}` of all group members must also align

```cpp
void setX   (int x) { _x = x;        }
int  getX   (     ) { return _x;     }
bool isValid(     ) { return _x > 0; }
```

---

## 15. Comment Style

Single-line (`//`) and inline comments that form a sentence must start with an
**uppercase** letter and must **not** end with a period:

```c
// Select the endpoint address
// Wait until the endpoint is ready
Endpoint_SelectEndpoint(addr); // Select endpoint
```

**Labels, closing comments, and markers** are not sentences — leave their case as-is:

```c
} // for i
} // class Foo
    case A: /* FALL-THROUGH */
```

When a comment requires **multiple sentences** — forming a paragraph — switch to
block comment form (`/* */`) and end each sentence with a period:

```c
/*
 * Resets the device to its default state.
 * Must not be called while a transmission is in progress.
 */
```

The `/* */` form is triggered by multiple sentences, not by the presence of a
period in a fragment. A sentence that happens to reference an abbreviation ending
in `.` still uses `//` if it stands alone.

**Separator alignment**: when inline comments in an aligned group all use the same
separator character (`—`, `:`, etc.), align that separator across the group by padding
the label with spaces:

```java
int[]   x  = { 1, 2, 3 };            // single-level — pad
int[][] xy = { { 1, 2 }, { 3, 4 } }; // nested       — both levels pad
int[]   z  = {};                     // empty        — tight
```
