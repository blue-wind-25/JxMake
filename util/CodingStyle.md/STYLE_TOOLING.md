# STYLE_TOOLING.md — Build/Dev-Tooling Script Rules (E-INI, Makefile, Bash, PowerShell)

This file defines formatting rules for build-and-glue scripting/config: E-INI,
Makefile, Bash, and PowerShell. Unlike the full-language jobs (C/C++/Java,
Kotlin, JS/TS, Python3, the data formats), each of these four is **narrow,
beautification-only scope**: a short fixed list of specific transforms.
Any construct not explicitly covered below must be left byte-identical to
the source — there is no general reindentation/re-wrapping fallback for
anything outside these rules.

Read together with `STYLE.md` only where a section below explicitly says
so; otherwise `STYLE.md`'s general rules do not apply here (these four are
not curly-brace-family languages in the same sense C/C++/Java/Kotlin are).

---

## 0. Comments (shared across all four)

Makefile/Bash/PowerShell use `#` line comments (PowerShell also has
`<# ... #>` block comments, left untouched — out of scope); E-INI uses `#`,
`;`, `@`, `//`, and a triple-same-quote (`'''`/`"""`) marker — see §4.
Comment normalization is **not** part of the fixed rule lists in §1–§4; it
is a separate, optional pass applied to line comments only: first-letter
capitalization of the comment body, and stripping a sole trailing `.` (the
same ad hoc pattern used for TOML and other non-curly languages) — not the
curly-brace-family comment-classifier pipeline. Bash additionally skips
capitalization when the comment opens with a common Unix tool name (e.g.
`grep`, `awk`, `sed`); PowerShell skips it for a narrower list of common
cross-platform CLI tool names (e.g. `java`, `git`, `node`) rather than its
own much broader keyword/cmdlet surface — Makefile and E-INI capitalize
unconditionally.

**`%` is never a comment marker for any of these four languages** —
reserved for this formatter's own `JXM_CFMT_CFG` in-file directive (e.g.
`#% JXM_CFMT_CFG ...`). E-INI's spec explicitly calls this out since its
comment-marker list is otherwise fairly permissive (four punctuation-based
markers plus triple-quote); the other three simply never use `%` for
anything else either.

---

## 1. Makefile

Scope: assignment alignment, continuation-line alignment, target spacing,
conditional-directive indentation. **Recipe lines (leading tab) are never
touched** — copied byte-identical, since Make is whitespace-sensitive there
(a space instead of a tab breaks the recipe).

### 1.1 Assignment Alignment

Contiguous non-blank assignment lines (`=`, `:=`, `+=`, `?=`) are aligned
into one column group. A blank line **or any non-matching line** (a
comment, a target line, or any other line that isn't itself an assignment)
breaks the group — the next group starts a fresh alignment column.

```
CC       = gcc
CFLAGS   = -O2 -Wall
LDFLAGS += -lm
TARGET  := app
```

### 1.2 Continuation-Line Alignment

A backslash-continued value's wrapped lines align under the first line's
value start column:

```
SRC = a.c \
      b.c \
      c.c
```

### 1.3 Target Spacing

Normalize to exactly one space after `:` and single spaces between
prerequisites; no space before `:`:

```
app: main.o util.o
```

### 1.4 Conditional Indentation

`ifdef`/`ifeq`/`ifneq`/`else`/`endif` bodies indent one level (4 spaces)
relative to the directive:

```
ifdef DEBUG
    CFLAGS += -g
else
    CFLAGS += -O2
endif
```

---

## 2. Bash

Scope: five specific transforms, modeled on (not a full reimplementation
of) `shfmt`'s defaults. Everything else — including but not limited to
arrays, `[[ ]]`/`[ ]` tests, `local`/`declare`, command substitution
nesting, `elif`, `while`/`until`, comments — is left untouched until
explicitly added to this list.

**Requires a real tokenizer** (quoting: `'...'`, `"..."` with
interpolation, `$'...'`; heredocs `<<EOF`/`<<-EOF`; comments `#`; command
substitution `$(...)`/backticks; arithmetic `$((...))`) — none of the
rules below may fire on a construct that only looks like a match inside a
string, heredoc, or comment.

### 2.1 `if`/`then` Same-Line Merge

```
if foo
then
    echo hi
fi
```
->
```
if foo; then
    echo hi
fi
```

### 2.2 Pipe Spacing

```
find .|grep foo|sort
```
->
```
find . | grep foo | sort
```

### 2.3 Function Brace Placement

```
foo(){
echo hi
}
```
->
```
foo() {
    echo hi
}
```
(Body indentation here is a byproduct of brace-depth counting, not a
separately configurable rule.)

### 2.4 `case` Formatting

```
case "$x" in
foo)
echo foo;;
esac
```
->
```
case "$x" in
foo)
    echo foo
    ;;
esac
```

### 2.5 Arithmetic Operator Spacing

Inside `$((...))` only:

```
echo "${arr[$((i+1))]}"
```
->
```
echo "${arr[$((i + 1))]}"
```

---

## 3. PowerShell

Scope: brace-depth indentation, block-scoped `=` alignment, pipeline
split/align, and `{`/`}` spacing. Like Bash, requires a real tokenizer
(string literals `'...'`/`"..."` with `$(...)`/`${...}` interpolation,
here-strings `@"..."@`/`@'...'@`, comments `#`/`<# #>`) so brace-counting
and operator detection never fire inside them.

### 3.1 Indentation (Naive Brace-Depth Count)

Not context-aware scope-depth reindentation (unlike the C/C++/Java/Kotlin
curly-reindent job) — every `{` increases depth by one, every `}`
decreases it, regardless of what kind of construct the braces belong to
(`if`, function body, scriptblock, hashtable all treated identically):

```
if($x){
Write-Host "hello"
if($y){
Write-Host "world"
}
}
```
->
```
if ($x) {
    Write-Host "hello"
    if ($y) {
        Write-Host "world"
    }
}
```

### 3.2 Operator Spacing + `=` Alignment

```
$a=1
$bb=$a+2
```
->
```
$a  = 1
$bb = $a + 2
```

Same block-scoped alignment-group mechanism applies inside hashtable
literals and `switch` arms (3.4, 3.5), including the group-boundary rule:
a blank line or any non-matching line breaks the group (same rule as
Makefile §1.1).

### 3.3 Pipeline: Always Split + Right-Align `|`

No collapse/wrap decision — pipelines are unconditionally split one
segment per line (after the first), with `|` right-aligned to the longest
segment on the line above it:

```
Get-ChildItem|Where-Object{$_.Length -gt 10MB}|Sort-Object Name
```
->
```
Get-ChildItem                           |
    Where-Object { $_.Length -gt 10MB } |
    Sort-Object Name
```

A scriptblock argument passed inline to a pipeline stage (e.g.
`Where-Object { ... }` above) always stays single-line, regardless of
pipeline wrapping — never brace-depth-indented per 3.1.
Multi-statement scriptblocks are out of scope (left untouched, same as
any other unlisted construct).

### 3.4 Hashtable Spacing

```
@{
Name="John"
Age=20
}
```
->
```
@{
    Name = "John"
    Age  = 20
}
```

A single-line hashtable literal (`@{ Name = "John" }`) is left as-is,
never forced multi-line — only hashtables already multi-line in the
source get alignment applied.

### 3.5 `switch` Formatting

Indentation per 3.1; arm patterns get the same `=`-style alignment
mechanism (3.2) applied to the gap before `{`:

```
switch($x){
1{"one"}
2{"two"}
}
```
->
```
switch ($x) {
    1  { "one" }
    22 { "two" }
}
```

### 3.6 `{`/`}` Spacing

Ensure exactly one space before an opening `{` and, where on the same
line as trailing content, one space before a closing `}`. Applies
everywhere, including single-line scriptblock arguments (e.g.
`Where-Object { $_.Length -gt 10MB }`) — not limited to the "structural"
braces (if/function/switch/hashtable) covered by 3.1's brace-depth indent.

---

## 4. E-INI (Extended INI)

A simple INI-like key-value config format with grouping. Scope: separator
alignment, indentation snapping, line-continuation alignment, and comment
normalization — a fixed four formatting rules (plus "no long-line
breaking" as an explicit fifth non-rule). No braces/indentation-significant
structure of its own; a "group" here means a contiguous run of alignable
key-value lines, distinct from an INI `[section]` group header.

**Recognized syntax** (requires a real per-line scanner: single/double
quotes `'...'`/`"..."` never span a physical line, so none of the rules
below may fire on a construct inside a quoted span):

- **Key-value separator**: `=` or `:`, whichever appears first outside
  quotes on the line (an independent operator each time — not `:=` or any
  other compound form).
- **Key**: everything left of the separator. Quoted (`'...'`/`"..."`)
  keeps its interior whitespace verbatim; unquoted has outer whitespace
  stripped and internal whitespace runs collapsed to one space.
- **Value**: everything right of the separator, outer whitespace stripped
  unless quoted (quoted interior preserved verbatim, never collapsed).
- **Group header**: `[name]`, `{name}`, `<name>`, `(name)`, or a bare/plain
  line with no wrapping marker at all (any non-comment line with no
  key-value separator). Same trim/collapse rule as an unquoted key; a
  quoted portion inside a header preserves its interior verbatim.
- **Comments**: `#`, `;`, `@`, `//`, each recognized only outside quotes —
  an exact allow-list, not an open-ended "any punctuation" rule. Also,
  three of the same quote character in a row (`'''`/`"""`) outside any
  other quoting context starts a comment to end of line. `%` is never a
  comment marker (see §0).

### 4.1 Separator Alignment

Contiguous key-value lines are aligned into one column group; a blank line
or any non-matching line breaks the group (same semantics as Makefile
§1.1):

```
host = localhost
port : 8080
name = 'John Doe'
```
->
```
host = localhost
port : 8080
name = 'John Doe'
```

### 4.2 Indentation (Round Up to Nearest Multiple)

E-INI has no braces/structural nesting to derive a depth from, so every
line's leading indentation is independently snapped to the nearest
`indent-size` multiple (rounding up):

```
[section]
  key = value
     other = value
```
->
```
[section]
    key   = value
    other = value
```

### 4.3 Line Continuation Alignment

A `\`-continued value's wrapped lines align under the first line's value
start column (same mechanism as Makefile §1.2):

```
long_key = first part \
    second part
```
->
```
long_key = first part \
           second part
```

### 4.4 Comment Normalization

Reuses the Makefile §0 normalizer: first-letter capitalization of the
comment body, and stripping a sole trailing `.` — capitalized
unconditionally, no tool-name skip list:

```
; comment about the timeout
```
->
```
; Comment about the timeout
```

### 4.5 No Long-Line Breaking

Values, headers, and comments always stay on one line regardless of
length — there is no wrapping fallback, unlike the full-language jobs.

---

## Config

No config keys are defined for the four languages in this file. Revisit
when a language needs an enable/disable gate or other user-facing option
(mirrors `STYLE_CPP26.md` §5's provisional-status precedent).
