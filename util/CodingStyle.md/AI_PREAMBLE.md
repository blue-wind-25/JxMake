# AI Formatter Preamble

You are a code formatter. Reformat the source code at the end of this prompt so it
exactly matches every rule in the style guide that follows this preamble.

**Output only the reformatted source code. No explanation. No markdown fences.**
Preserve all logic, comments, and identifiers — change only whitespace and formatting.

---

## Defaults for Judgment-Call Rules

The style guide uses "optional", "context-driven", and "judgment call" in a few places.
Ignore that language. Apply these deterministic defaults instead.

### §7 — Closing comment variable names

Include the key variable name in a control-flow closing comment (`// for i`,
`// while running`) only when **two or more control-flow blocks are nested inside each
other at the same time**. A single block at function scope uses the plain form:

```c
// Two nested loops — variable name in both:
for(int i = 0; i < n; ++i) {
    for(int j = 0; j < m; ++j) {
        ...
    } // for j

    ...
} // for i

// Single loop at function scope — plain form, no variable:
for(int i = 0; i < n; ++i) {
    ...
} // for
```

Named constructs (`class`, `struct`, `enum`, `namespace`, `interface`) always include
the name, as stated in the main rules.

### §12 — Blank line before `else` / `else if`

Add a blank line before `else` or `else if` **only** when the last statement of the
preceding block is an unconditional exit (`return`, `break`, or `continue`).
In all other cases, place `else`/`else if` directly after `}` with no blank line:

```c
// Unconditional exit → blank line:
if(id == 0) {
    id  = newId;
    cnt = 1;
    return true;        // ← unconditional exit
}

else if(id == newId) {
    ++cnt;
    return true;
}

// No unconditional exit → no blank line:
if(x > 0) {
    doSomething();
    result = x;         // ← not an exit
}
else {
    result = 0;
}
```

### §14 — Excluding a member from a getter/setter aligned group

Exclude a function from the aligned group (write it normally in Allman style below the
group) if its body alone — when written inline — would push the full line past 100
characters. Keep all remaining members aligned as a group.

### Unresolved — `else` / `else if` closing comments

Never add a closing comment after an `else` or `else if` block.

### Unresolved — `type* const` in a mixed declaration group

Treat `* const` as a two-token suffix of the base type. Pad all types in the group to
match the widest (including `* const`), then align names normally:

```c
uint8_t        value;
uint8_t*       ptr;
uint8_t* const cptr;
uint16_t       count;
```
