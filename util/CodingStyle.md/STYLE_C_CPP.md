# STYLE_C_CPP.md — C and C++ Specific Style Rules

Read [STYLE.md](STYLE.md) first. This file extends and overrides it for C and C++.

---

## 1. Empty Parameter Lists

- **C**: always write `void` explicitly: `void foo(void)`
- **C++**: omit `void` — empty parens are unambiguous: `void foo()`

---

## 2. Function Brace Style

**Function definitions only** use **Allman style** — opening `{` on its own line:

```c
void process()
{
    doSomething();
    doMore();
}
```

All other blocks (`if`, `else`, `for`, `while`, `switch`, etc.) use **K&R style** — opening
`{` on the same line as the keyword. See STYLE.md §11.

**One-liner exception**: if the entire function body is a single statement (or a short sequence
of tightly related statements), write on two lines with `{` and `}` on the second line:

```c
void reset()
{ _x = 0; }
```

Multiple statements on one line — same rule, semicolons separate them:

```c
void f()
{ _done = true; return y; }
```

Note: function bodies do **not** get the blank-line-after-`{` / blank-line-before-`}`
treatment described in STYLE.md §7, even when the body exceeds 5 lines.

---

## 3. C++ Template Angle Brackets `<>`

**Single-level** template parameters — tight, no padding:

```cpp
vector<int>
map<string, int>
```

**Nested** templates — always pad the outer `<>` to prevent `>>` parse errors
in older C++ standards:

```cpp
vector< vector<int> >
map< string, vector<int> >
map< vector<string>, int >
map< string, map< string, vector<int> > >
```

Rule: any `<>` that directly contains another `<>` at any depth gets padded.
This is a **correctness rule**, not just style.

---

## 4. Pointer and Const Qualifier Style

- `*` attaches to the type: `char* p` not `char *p`
- `const` before `*` attaches to type: `const char* p`
- `const` after `*` stays in place: `uint8_t* const p`
- Pointer to const pointer: `uint8_t* const* pp`

---

## 5. Pre/Post Increment

Same as STYLE.md §4 — use `++i` / `--i` unless post semantics are required.
This is especially relevant in `for` loop increments:

```c
for(int i = 0; i < n; ++i)
```

---

## 6. Bitfields Declaration Alignment

```cpp
struct DeviceState {
    static volatile uint8_t  buffer[64];
                    uint16_t timeout;
                    uint8_t  flags    : 4; // Status flags
                    uint8_t  mode     : 2; // Mode bits
                    uint8_t  reserved : 2;
                    char     label[MAX];
}; // struct DeviceState
```

Rules:
- `:` are aligned after the field name.
- `//` and `/* .. */` are aligned after the field name and array/bitfield size.

---

## 7. Closing Comments — Additional C/C++ Cases

Namespace closing comments — named namespaces include the name; unnamed namespaces get
no closing comment at all (the `}` stands alone):
```cpp
} // namespace audio

}
```

`extern "C"` blocks — always include the literal text:
```cpp
} // extern "C"
```

Class and struct:
```cpp
} // class MyClass
} // struct Point
```

Enum:
```cpp
} // enum Color
} // enum class State (C++)
```

---

## 8. Unresolved / Preserve-As-Is Cases

- `uint8_t* const` within a mixed declaration group: align at type column,
  treat `const` as a post-type modifier — exact column behavior is left to
  judgment based on surrounding context.
- `else`/`else if` closing comment threshold: apply when the branch is long
  **and** contains multiple levels of nested `if`s — exact line count is a
  judgment call.

---

## 9. Section Dividers

Two strengths of divider are used, both full-width at the line-length limit (100 chars).

**Single line** — separates ordinary logical sections within the same context:

```c
////////////////////////////////////////////////////////////////////////////////////////////////////
```

**Triple line** — marks a stronger boundary: either immediately before a `#endif` that closes
a large conditional block, or between parts of a file with fundamentally different attribution
or origin (different subsystem, different third-party source):

```c
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
#endif // SOME_LARGE_CONDITIONAL_BLOCK
```

```c
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

/*
 * Based on "..." — different attribution / origin section follows
 */
```

Use sparingly in both cases — the triple should feel significant, not routine.

---

## 10. Header File Structure

Header files follow a fixed top-level layout. Each zone is separated from the next
by **exactly 2 blank lines**.

```cpp
/*
 * Copyright (c) ...
 * ...
 */


#ifndef MY_MODULE_H
#define MY_MODULE_H


// ... body: includes, declarations, definitions ...


#endif // MY_MODULE_H
```

**Zones:**
1. Copyright block (`/* */` form)
2. Header guard (`#ifndef` / `#define` pair)
3. Body
4. Closing `#endif`

**Guard name:** derived from the filename — uppercase, with `.` and path separators
replaced by `_` (e.g. `src/audio/Codec.h` → `AUDIO_CODEC_H`). The formatter warns
if the existing guard name does not match. Renaming is opt-in:
```
header-guard-rename = off   # default: warn only, do not rename
```

**`#pragma once` alternative:** if the file uses `#pragma once` instead of an
`#ifndef` guard, preserve it as-is. Do not convert between the two forms unless
explicitly configured:
```
header-guard-style = ifndef   # ifndef | pragma-once (default: preserve existing)
```

The 2-blank-line rule above applies to `#ifndef` guards. For `#pragma once` files,
the same zone separation applies with `#pragma once` in place of the guard block.

---

## 11. Include Ordering

Includes are arranged in two groups, separated by **exactly 1 blank line**:

```c
#include <system_header>    // Group 1: system / stdlib (angle brackets)
#include <another_system>

#include "local_header.h"   // Group 2: project / local (quotes)
#include "another_local.h"
```

**Within each group:** alphabetical order. Sorting is opt-in (default off) because
include order in C/C++ can affect behavior via macro dependencies:
```
include-sort = off   # default: preserve order within group, only enforce grouping
```

**Grouping** (angle bracket vs quote) is always enforced regardless of `include-sort`.
Mixed groups are split and reordered into the two-group layout.

**Source files** (`.c` / `.cpp`): same two-group rule applies. No header guard.

**The corresponding own header** (e.g. `Foo.cpp` including `Foo.h`) goes at the top
of group 2, before other local includes — this catches missing self-contained includes
early. This placement is always enforced, regardless of `include-sort`.
