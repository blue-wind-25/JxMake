# STYLE_C_CPP.md — C and C++ Specific Style Rules

Read [STYLE.md](STYLE.md) first. This file extends and overrides it for C and C++.

---

## 1. Empty Parameter Lists

- **C**: always write `void` explicitly: `void foo(void)`
- **C++**: omit `void` — empty parens are unambiguous: `void foo()`

---

## 2. Function Brace Style

Function definitions always use **Allman style** — opening `{` on its own line:

```c
void process()
{
    doSomething();
    doMore();
}
```

**One-liner exception**: if the entire body is a single statement, write on two lines
with `{` and `}` on the second line:

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

```c
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
- `//` and `/* .. */`are aligned after the field name and array/bitfield size.

---

## 7. Closing Comments — Additional C/C++ Cases

Namespace closing comments:
```cpp
} // namespace audio
} // namespace — unnamed : no label
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
