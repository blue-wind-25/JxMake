# STYLE_TOOLING.md — Build/Dev-Tooling Script Rules (Makefile, Bash, PowerShell)

This file defines formatting rules for build-and-glue scripting: Makefile,
Bash, and PowerShell. Unlike the full-language jobs (C/C++/Java, Kotlin,
JS/TS, Python3, the data formats), each of these three is **narrow,
beautification-only scope**: a short fixed list of specific transforms.
Any construct not explicitly covered below must be left byte-identical to
the source — there is no general reindentation/re-wrapping fallback for
anything outside these rules.

Read together with `STYLE.md` only where a section below explicitly says
so; otherwise `STYLE.md`'s general rules do not apply here (these three are
not curly-brace-family languages in the same sense C/C++/Java/Kotlin are).

---

## 0. Comments (shared across all three)

All three languages use `#` line comments. Comment-text normalization
(grammar/capitalization/decorative-separator/license-block classification)
reuses the existing shared comment-classifier pipeline
(`com.jxmake.formatter.classifier.CommentClassifier`/
`GruAbstainResolver`/`GruClassifier`) — not a new bespoke path. This
includes both the linear classifier and GRU abstain-resolution
automatically: the classifier is already wired as language-agnostic
infrastructure (every implemented family routes through it), and GRU
specifically is gated only by the single global `config.isGruClassifier()`
flag, not per-language — see RDD_KEY_259.

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
breaks the group — the next group starts a fresh alignment column
(RDD_KEY_254).

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
prerequisites; no space before `:` (RDD_KEY_255):

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
explicitly added to this list via an RDD.

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
(Body indentation here is a byproduct of brace-depth counting — see 2.5.)

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
a blank line or any non-matching line breaks the group (RDD_KEY_254, same
decision as Makefile §1.1).

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
pipeline wrapping — never brace-depth-indented per 3.1 (RDD_KEY_256).
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
source get alignment applied (RDD_KEY_257).

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
braces (if/function/switch/hashtable) covered by 3.1's brace-depth indent
(RDD_KEY_258).

---

## Config

No config keys defined yet for any of the three languages in this file —
none have landed implementation. Revisit once a language's rules move out
of draft status (mirrors `STYLE_CPP26.md` §5's provisional-status
precedent).
