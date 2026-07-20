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
layout judgment pass — it is cheaper (less quota) and safer (won't disturb JAR-applied
alignment).

Do **not** use this preamble with small on-device models (Qwen2.5-Coder-3B, Llama
3.2 3B, etc.) — they fail inconsistently on column alignment and bracket-padding rules.

This preamble is language-agnostic; combine it with the style files for the target
language (see README.txt). Prefer running the JAR first and use this full-file pass only
as a fallback for a construct the JAR doesn't handle. The relevant `STYLE_*.md` file's
own text is authoritative; nothing below should be assumed to override it.

---

## Don't Eyeball Whitespace — Use a Script

Column alignment and indentation are **arithmetic**, not visual judgment. Manually
counting spaces/columns while reading source text is unreliable even for a capable
model — the padding it produces often *looks* plausible and is wrong by one or two
columns. Never compute or apply padding by eye. Use a script instead: Python if it's
available in your environment; if not, any other scripting tool in reach (node, a
shell one-liner) — anything that counts characters exactly is acceptable, manual
counting is not.

The five snippets below are illustrative, not a library to import verbatim — adapt
the logic to whatever you're actually aligning (declarations, assignments, colon
groups, etc. per the relevant `STYLE*.md` section).

**1. Detect indent size and style** — measure what a file is already using before
converting anything:

```python
from collections import Counter

def detect_indent(lines):
    """Returns ('spaces'|'tabs', width) guessed from the file's own body."""
    widths = Counter()
    style = Counter()
    for line in lines:
        stripped = line.lstrip(' \t')
        indent = line[:len(line) - len(stripped)]
        if not indent or not stripped.strip():
            continue
        style['tabs' if '\t' in indent else 'spaces'] += 1
        if '\t' not in indent:
            widths[len(indent)] += 1
    dominant_style = style.most_common(1)[0][0] if style else 'spaces'
    # Smallest common non-zero width is usually one indent level.
    dominant_width = min(widths, default=4)
    return dominant_style, dominant_width
```

**2. Convert/modify indentation** — rescale to a target width/style without
touching content past the leading whitespace run:

```python
def convert_indent_line(line, old_width, new_width, new_style):
    stripped = line.lstrip(' \t')
    indent = line[:len(line) - len(stripped)]
    col = 0
    for ch in indent:
        col += (old_width - (col % old_width)) if ch == '\t' else 1
    if col % old_width != 0:
        return line  # irregular indent — leave untouched, don't guess
    level = col // old_width
    unit = '\t' if new_style == 'tabs' else ' ' * new_width
    return unit * level + stripped
```

**3. Compute padding for column alignment** — given a group of lines that should
align on some marker (`=`, `:`, a name column, etc.), compute the target column and
each line's needed pad count:

```python
def compute_alignment_padding(entries):
    """entries: list of (prefix_text, ...) where prefix_text is everything
    before the marker on that line. Returns per-entry pad-to-column counts."""
    target_col = max(len(prefix) for prefix, *_ in entries) + 1  # +1 = min 1 space
    return [target_col - len(prefix) for prefix, *_ in entries]
```

**4. Apply the computed padding** — insert exact spaces, never re-derive by eye
after step 3 has already given you the count:

```python
def apply_padding(prefix, pad_count, marker, rest):
    return f"{prefix}{' ' * pad_count}{marker}{rest}"
```

**5. Verify the output** — re-scan what you actually produced; don't trust that
steps 1–4 were applied correctly just because the logic was correct:

```python
def verify_output(lines, indent_width, indent_style, aligned_groups=()):
    problems = []
    for i, line in enumerate(lines, 1):
        if line != line.rstrip():
            problems.append(f"line {i}: trailing whitespace")
        indent = line[:len(line) - len(line.lstrip(' \t'))]
        if ' ' in indent and '\t' in indent:
            problems.append(f"line {i}: mixed tabs/spaces in indent")
        if indent_style == 'spaces' and len(indent) % indent_width != 0 and line.strip():
            problems.append(f"line {i}: indent width not a multiple of {indent_width}")
    for group in aligned_groups:
        # group: list of (line_no, column_of_marker)
        cols = {c for _, c in group}
        if len(cols) > 1:
            problems.append(f"lines {[n for n, _ in group]}: alignment column mismatch {cols}")
    return problems
```

If `verify_output` flags anything, fix the underlying computation and re-run
verification — don't hand-patch the flagged line by eye, since that reintroduces
the exact failure mode this section exists to prevent.

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
  before the top-level `:`. None of these match (`for(;;)`) → bare `// for`. This
  extraction assumes C-style `for(init; cond; incr)` syntax — for languages without
  that shape, use the language-specific extraction below instead:
  - **Python `for x in iterable:`** — the bound identifier(s) immediately after
    `for` and before `in`. For a tuple target (`for k, v in items:`), use the first
    identifier only, same "first identifier wins" principle as C's init-clause rule.
  - **JS/TS `for...of` / `for...in`** — the bound identifier immediately after the
    loop keyword and before `of`/`in`, same position as Python's rule above. A
    C-style `for(init; cond; incr)` in JS/TS still uses the original init-clause
    rule, since JS/TS supports both forms in the same language.
- **`while` / `switch`** — only if the controlling expression is exactly one identifier,
  or `!` followed by one identifier. Any more compound condition → bare `// while` /
  `// switch`. Do not attempt further simplification.
- **Python `match`** — same treatment as `while`/`switch` above: only if the subject
  expression is exactly one identifier. Any more compound subject → bare `// match`.

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

**Python (`#` only, no block-comment form):** the period/capitalization/label rules
above apply unchanged, but the block-form switch does not — Python has no `/* */`
equivalent, so a comment requiring two or more sentences stays as consecutive `#`
lines instead of changing syntax, each sentence still ending with a period (the
underlying "multi-sentence comments get periods, single-line labels don't" principle
is unchanged; only the mechanism for holding multiple sentences differs).

**CSS, XML, HTML5 (`/* */` / `<!-- -->` only, no line-comment form):** the block-form
switch is moot — these languages only ever have the block form, so a multi-sentence
comment already uses it by default; apply the period/capitalization rules directly
without a "switch to block form" step, since there's no other form to switch from.

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
