# STYLE_JXMAKE.md — JxMakeFile Scripting Language Rules

This file defines formatting rules for JxMakeFile scripts: the literal
filename `JxMakeFile` and any `*.jxm` file. Like the tooling family
(`STYLE_TOOLING.md`: E-INI, Makefile, Bash, PowerShell), this is **narrow,
beautification-only scope** — a short fixed list of specific transforms.
Any construct not explicitly covered below must be left byte-identical to
the source. JxMake scripting is partially context-dependent (macros are
raw text substitution, multiline strings are opaque payloads), so there is
deliberately no general reindentation/re-wrapping fallback beyond the four
rules below.

Read together with `STYLE.md` only where a section below explicitly says
so; otherwise `STYLE.md`'s general rules do not apply — JxMakeFile is not
a curly-brace-family language.

Full grammar reference: `../../docs/txt/JxMake-Grammar.txt`. Prose manual:
`../../docs/txt/en_US/01-*.txt` through `12-*.txt`.

**Requires a real tokenizer.** None of the rules below may fire on a
construct that only looks like a match inside: a raw string (`` `...` ``),
single-quoted string (`'...'`), double-quoted string (`"..."`, including
its combining `."..."` / flattening `:"..."` prefixed forms), a multiline
string (`[[" ... "]]`), a block comment (`(* ... *)`), or a line comment
(`#...`). The tokenizer must also recognize macro-use (`.$name`),
compiler directives (`:::pragma`, `:::include`, etc. — untouched, out of
scope), and `@`/`-@`/`+@`/`-+@`/`?@` shell-exec line markers (the command
text after the marker is left byte-identical, same as a raw string).

---

## 1. Comments

**Line comments** (`#` to end of physical line) are normalized: first-
letter capitalization of the comment body, and stripping a sole trailing
`.` — reusing the exact same `ToolingCommentNormalizer` used for Makefile/
Bash/PowerShell/E-INI, gated by the existing global
`normalize-comment-start-case`/`normalize-comment-end-period` config keys.
Contiguous standalone `#` comment lines are chain-grouped and normalized
as one unit (same semantics as the Makefile/E-INI comment chain rule) —
not normalized independently per line.

```
# fix the build path
```
->
```
# Fix the build path
```

**Indentation of standalone `#` comment chains.** A standalone `#` line
comment (or contiguous chain of them) takes the indent depth of the next
non-blank, non-comment code line that follows it — the comment attaches
to what it comments on, so it always tracks that line's depth under rule
2's forced reindent, even when the comment's own line didn't itself
change block nesting. If the comment is separated from the next code line
by a blank line, or is the last line of a file/block with no following
code line at all, it falls back to the depth rule 2 would already assign
a code line at that same position (i.e. the current depth at the point
the comment appears), rather than searching further ahead.

**Block comments** (`(* ... *)`, may span many lines) are **not**
normalized — their interior text is always left byte-identical. They are,
however, subject to rule 2's indentation tracking as a single unit: the
entire comment (every physical line inside it, including blank lines)
shifts by a uniform delta equal to `(new indent level of the opening `(*`
line) - (old indent level of the opening `(*` line)`, so any hand-aligned
interior content (e.g. ASCII-art borders, aligned `*` columns) keeps its
relative alignment intact:

```
function f()
(*
 * Explanation.
 *)
    ...
endfunction
```
If `function`'s body moves from indent level 1 to level 2 for any reason,
every line of the `(* ... *)` block — the opener, the `*` line, and the
closer — shifts right by the same one indent-size worth of spaces. Nothing
inside the comment other than each line's leading whitespace is touched.

`%` is never a comment marker for JxMakeFile (reserved for this codebase's
own `JXM_CFMT_CFG` in-file directive, e.g. `#% JXM_CFMT_CFG ...`).

---

## 2. Indentation (Block-Keyword Nesting Depth)

Every non-string-literal-interior physical line is reindented to `depth *
indent-size` spaces of leading whitespace, where `depth` is the current
block-nesting depth computed purely from the fixed list of block-
delimiting keywords below (reuses the shared global `indent-size` config
key, default 4 — same key the curly/indent-based language families
already use). Standalone `#` line comments are covered too, but take
their depth from the next code line per rule 1's "Indentation of
standalone `#` comment chains" above rather than from their own line's
keyword classification (they have none); block `(* ... *)` comments are
covered by rule 1's own uniform-shift-by-delta rule instead. This is a
**full forced reindent** of
every statement line (not a "preserve author's own indent, only snap"
rule like Bash's `snapIndent`) — safe here because every JxMake block
construct is unambiguously keyword-delimited (unlike Bash's braceless
`if`/`for`), so there is no non-keyword construct whose depth is
ambiguous.

**Block openers** (increase depth by one for the lines that follow, until
the matching closer, which itself renders at the opener's own depth):

| Opener | Closer |
|---|---|
| `function name(...) [modifiers]` | `endfunction` |
| `target name-expr [: prereqs]` | `endtarget` |
| `if condition` (block form only — see below) | `endif` |
| `for [local] var := ... to ... step ...` | `endfor` |
| `foreach [local] [idx,] var in ...` | `endforeach` |
| `while condition` | `endwhile` |
| `do` | `whilst condition` |
| `repeat` | `until condition` |
| `loop` | `endloop` |
| `.macro name` | `.endmacro` |

`elif condition` and `else` render at the same depth as their owning
`if`; the body following each renders one level deeper, same as the
`if`'s own body. `do`/`whilst` and `repeat`/`until` are a matched
opener/closer pair like any other — `whilst condition`/`until condition`
carry a trailing condition but are still full closers with no further
body after them.

**One-liner `if` is not a block opener.** `if condition : oneliner-stmt`
(Section 11.1 of the grammar) is a single leaf statement, not tracked as
a nesting level, and has no matching `endif`. Distinguish the two forms
by scanning the logical line (already joined across any `\`
continuations) after the `if` keyword and its condition for a `:` token
at bracket/paren depth 0, outside any string/comment: if found, it's a
one-liner (render as one ordinary statement line at the current depth,
do not open a new nesting level); if absent, it's a block `if` (opens a
level, expects a matching `endif`).

**`if`/`elif`/`else` keyword right-alignment.** Within one
`if`-`elif`-...-`else` chain, the keyword itself (`if`, `elif`, `else`) is
right-justified to the width of the widest keyword used in that chain
(`elif`/`else` are both 4 characters vs. `if`'s 2), by padding with extra
leading spaces before the keyword — added on top of the normal
depth-based indent — so that whatever follows the keyword begins at the
same column on every line of the chain:

```
  if ${halign} &== 'l' ; halign := 2
elif ${halign} &== 'r' ; halign := 4
elif ${halign} &== 'c' ; halign := 0
else                     halign := 1
endif
```

This applies **only** when every branch of the chain inlines its body on
the same physical line via a trailing `;` (the grammar treats `;` as
fully equivalent to a physical newline, so "condition, `;`, body" all on
one physical line is a normal, existing usage in this codebase's own
`.jxm` library files — see `src/0-JxMake/lib/BasicPlatformUtil.jxm`'s
`verSpecStr +:= ...` if/else). If even one branch in the chain instead
puts its body on following separate lines (the ordinary block form), the
whole chain is left at plain depth-based indent with no keyword
right-alignment — this is a grouping rule, not an unconditional one: a
chain only gets this treatment when it looks like a single visually
grouped block of one-line branches, exactly the shape in the example.

**Multi-statement lines** (any line containing a `;` statement separator)
are reindented as a whole to the depth of the *first* statement on that
line — the same forced-reindent rule applies to the line's leading
whitespace only; nothing after the first non-whitespace column is
touched by this rule.

**Continuation lines** (the 2nd and later physical lines of a logical
line joined by a trailing `\`) are **not** independently depth-reindented
by this rule — see rule 3, which governs them instead.

---

## 3. Line-Continuation Alignment (`\`)

A backslash-continued logical line's wrapped physical lines align under
the first physical line's own content, using the Makefile §1.2 / E-INI
§4.3 mechanism:

- For an assignment statement (`direct-assignment-stmt` or
  `indirect-assignment-stmt`) whose first line is continued, every
  wrapped continuation line aligns under the column where the value
  (the first token after the assignment operator) begins on the first
  line:

  ```
  SRC_FILES = a.jxm \
              b.jxm \
              c.jxm
  ```

- For any other continued statement (no clear "value column" exists —
  e.g. a continued `echo`/function-call/target-prerequisite line),
  continuation lines instead indent one level deeper than the
  statement's own line (`(depth + 1) * indent-size`), the same fallback
  Rule 2 already uses for ordinary body lines. This is an intentional,
  resolved design choice (not an open question) — JxMake statements
  outside assignments have no single well-defined anchor column the way
  a Makefile/E-INI `key = value` line does.

An empty physical line still terminates an in-progress `\` continuation
(per the grammar) and is never itself reindented as a continuation.

---

## 4. Assignment Operator Alignment (`=`, `:=`, `+=`, `:+=`/`+:=`, `?=`, `:?=`/`?:=`)

Contiguous single-statement assignment lines at the same nesting depth
are aligned as a field table, not just on the operator: direct assignment
(`[modifier] var-name assign-op term+`, where `modifier` is `local`,
`const`, or `local const` in that order per the grammar — never
`const local`) and indirect assignment (`^var-name assign-op term+`) both
participate. Each line is split into left-justified fields — an optional
`local` field, an optional `const` field, then the var-name (`^`-prefixed
for indirect) — and each field is padded to the widest occurrence of that
field within the contiguous group (a line missing a field still reserves
that field's column width, so later columns stay aligned). One space
separates fields. The assignment operator itself is placed with a single
space after the var-name field and is **not** further padded to a common
width (`=` and `:=` and `+:=` keep their own natural length); the value
(`term+`) follows the operator with a single space and is **not** aligned
into a column either — only the fields left of the operator are aligned.

A blank line, a depth change, a comment line, or any non-matching line
(including any line containing a `;` statement separator, which is
treated as non-matching for this rule regardless of what the first
statement is) breaks the group — the next group starts a fresh alignment
column, same group-boundary semantics as Makefile §1.1/E-INI §4.1/
PowerShell §3.2.

```
local CC     = 'gcc'
local CFLAGS = '-O2 -Wall'

LDFLAGS  += '-lm'
LDFLAGS +:= '...
^RefA    := 20

local       AA = '...'
local const BB = '...'
      const CC = '...'
```

---

## Config

No JxMakeFile-specific config keys are introduced. Rule 1 reuses the
existing global `normalize-comment-start-case`/`normalize-comment-end-period`
keys; rule 2 reuses the existing global `indent-size` key. Revisit only if
a future need for an enable/disable gate or other user-facing option
arises (mirrors `STYLE_TOOLING.md`'s "no config keys" precedent).
