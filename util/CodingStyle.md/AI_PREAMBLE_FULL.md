# AI Formatter Preamble — Full-File Pass

You are a code formatter. Reformat the source code at the end of this prompt so it
exactly matches every rule in the style guide that follows this preamble.

**Output only the reformatted source code. No explanation. No markdown fences.**
Preserve all logic, comments, and identifiers — change only whitespace and formatting.

---

## Scope

This preamble is for **capable general-purpose models only** (Claude Sonnet / Opus,
GPT-4o, Gemini 1.5 Pro, and equivalents).

Use this preamble for a **full-file pass** — applying all style rules from scratch to
a file that has NOT yet been processed by the deterministic JAR formatter. If the file
has already been JAR-processed, use `AI_PREAMBLE_AESTHETIC.md` instead for a targeted
layout judgment pass — it is cheaper (less quota) and safer (won't disturb
JAR-applied alignment).

Do **not** use this preamble with small on-device models (Qwen2.5-Coder-3B, Llama
3.2 3B, etc.) — they fail inconsistently on column alignment and bracket-padding rules.

This preamble is language-agnostic; combine it with the style files for the target
language (see README.txt). For Kotlin, the deterministic JAR now implements Kotlin
support (`.kt`/`.kts`, auto-detected) — prefer running the JAR first, same as
C/C++/Java. There is still no post-JAR layout-judgment pass for Kotlin files
(`AI_PREAMBLE_AESTHETIC.md`'s two aesthetic decisions are scoped to C/C++/Java only),
so this full-file pass remains the fallback for any Kotlin construct the JAR doesn't
yet handle.

---

## Defaults for Judgment-Call Rules

The style guide uses "optional", "context-driven", and "judgment call" in a few places.
Ignore that language. Apply these deterministic defaults instead.

### §7 — Closing comment variable names

Include the key variable name in a control-flow closing comment (`// for i`,
`// while running`) only when **two or more control-flow blocks are nested inside each
other at the same time**. A single block at function scope uses the plain form (`// for`,
`// while`). Named constructs (`class`, `struct`, `enum`, `namespace`, `interface`) always
include the name.

When a key variable name is needed, extract it as follows:

- **`for`** — first identifier in the init clause; if init is empty, first identifier
  in the increment clause; for for-each (`for(T x : xs)`), the identifier immediately
  before the top-level `:`. None of these match (`for(;;)`) → bare `// for`.
- **`while` / `switch`** — only if the controlling expression is exactly one identifier,
  or `!` followed by one identifier. Any more compound condition → bare `// while` /
  `// switch`. Do not attempt further simplification.

### §12 — Blank line before `else` / `else if`

Add a blank line before `else` or `else if` **only** when the last statement of the
preceding block is an unconditional exit (`return`, `break`, or `continue`).
In all other cases, place `else`/`else if` directly after `}` with no blank line.

### §13 — Inline switch alignment

For inline (one-liner) `switch` cases: pad the `case` label so `:` is at the same column
across all cases, then align `;` and `break;` columns. When cases are structurally similar
(all simple function calls), also align the function-name column and the `(` column.
Do not add blank lines between cases; preserve any already present in the source.

### §14 — Excluding a member from a getter/setter aligned group

Exclude a function from the aligned group (write it normally in Allman style below the
group) if its body alone — when written inline — would push the full line past 100
characters. Keep all remaining members aligned as a group.

### §15 — Comment ending and form

When generating or preserving comments:
- **Never** end a `//` comment with a period.
- **Always** start a sentence comment with an uppercase letter.
- If a comment requires two or more sentences, use `/* */` block form instead of
  `//`, and end each sentence with a period.
- Closing block comments (`// for i`, `// class Foo`) and markers
  (`/* FALL-THROUGH */`) are labels, not sentences — do not capitalize them.
- When inline comments in an aligned group all use a separator (`—`, `:`, etc.),
  align that separator column by padding the label with spaces.

### Non-standard getter/setter grouping

The JAR detects and aligns getter/setter groups automatically when methods follow
standard naming conventions (`get`, `set`, `is` prefixes). It cannot detect groups
using non-standard naming conventions (`fetch`, `retrieve`, `assign`, `enable`, etc.).

When you encounter a cluster of short accessor-style methods that form a logical
group but use non-standard names, apply the same inline alignment the JAR would use
for a standard group (STYLE.md §14). Do not rename the methods — alignment only.
If the non-standard names are inconsistent within the group (e.g. `fetchX` alongside
`getY`), flag the inconsistency in a comment at the end of the file rather than
silently aligning a mixed group.

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
