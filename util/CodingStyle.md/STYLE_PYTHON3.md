# STYLE_PYTHON3.md — Python 3 Rules

Read [STYLE.md](STYLE.md) first. Python has real imperative-language surface
(functions, control flow, classes) different enough from the brace-delimited
languages that this gets its own file, same as C, Java, and Kotlin each did, rather
than a "borrowed sections" fold-in like STYLE_DATA_FORMATS.md. Where a rule reuses a
STYLE.md mechanism directly, it says so; Python's significant whitespace and several
bracket-content categories with no C-family analog mean most rules here are new,
not inherited.

Supports: latest supported Python 3 (currently 3.15+). Python 2 is not supported.

**Indentation note:** unlike every other currently-supported language, Python's
indentation is semantically load-bearing, not purely cosmetic — the formatter must
never change indentation depth in a way that would alter which block a statement
belongs to. This constrains every rule below, even where not called out explicitly.

---

## 1. Bracket Complexity Detector

Extends STYLE.md §3.1's tight/loose heuristic (atoms/simple ops tight, a call or
nested bracket loose, nesting propagates outward) — this is an **extension, not a
straight port**, since Python has bracket-content categories the C-family heuristic
has no bucket for.

### 1.1 Baseline (inherited from STYLE.md §3.1)

```python
if(a and b):            # atoms, simple ops — tight
if(isReady(x)):         # NOT this — see below, actually loose
if( isReady(x) ):       # contains a call — loose
a[10]                   # constant index — tight
a[ callSomething(x) ]   # call inside index — loose
```

### 1.2 Comprehensions

A fifth content category — a `for` clause plus optional `if` filters, possibly
nested — not "atom," "call," or "nested bracket." Comprehensions get their own rule:
the enclosing bracket is always loose, regardless of how simple the expression or
filter is, since a comprehension is never a bare atom/simple-op case:

```python
squares = [ x * x for x in range(10) ]
evens   = [ x for x in range(10) if x % 2 == 0 ]
pairs   = { (x, y) for x in range(3) for y in range(3) }
lookup  = { k: v for k, v in items.items() }
```

A nested comprehension (comprehension inside a comprehension) stays loose at every
level, same nesting-propagation principle as STYLE.md §3.1.

### 1.3 Slicing

`:` inside `[]` has no analog in C/Java/Kotlin indexing, and does not itself trigger
looseness — it is punctuation between slice parts, not an operator, and is never
padded regardless of whether the enclosing bracket is tight or loose:

```python
a[1:2:3]           # simple slice — tight
a[::2]             # simple slice — tight
a[i+1:j-1]         # simple ops on either side of : — still tight
a[ i+1:(j*k)-1 ]   # nested () triggers outer looseness — colon stays tight
```

The bracket's tight/loose decision is made the same way as any other `[]` content
(STYLE.md §3.1's nesting/call signal) — evaluate what's on either side of each `:` as
its own sub-expression; if any sub-expression contains a call or nested bracket, the
outer `[]` goes loose, but the `:` itself is never spaced.

### 1.4 Star-Unpacking

`*args`, `**kwargs`, `[*a, *b]` — no direct analog in the bracket contents of any
currently-supported language. No space between `*`/`**` and the name/expression it
applies to, whether in a call, a definition, or a literal:

```python
def merge(*args, **kwargs): ...
result = combine(*first, *second)
merged = [*a, *b, *c]
config = { **defaults, **overrides }
```

Presence of `*`/`**` unpacking inside `[]`/`{}` does not by itself force looseness —
it's evaluated as any other content atom under §1.1/§1.2's rules (a plain
`[*a, *b]` stays tight; `[*get_items(), *b]` goes loose because `get_items()` is a
call). A non-empty `{}` (dict or set, whether or not it contains `**`/`*` unpacking)
always gets §3.3's mandatory padding regardless — see §1.5, which applies uniformly
with no unpacking-only carve-out (RDD_KEY_184).

### 1.5 Dict vs. Set Literal Disambiguation

`{}` is an empty dict, `{1, 2}` is a set, `{1: 2}` is a dict — none of the
brace-delimited languages have this dual meaning. The formatter disambiguates by
presence of `:` at the top level of the `{}` content before applying padding:

- Any top-level `:` present → dict — same padding rules as §1.1/§3.3.
- No top-level `:` → set — same padding rules as a list/array literal.
- Empty `{}` → dict (Python's own semantics — an empty set has no literal form and
  must be written `set()`), tight, same as STYLE.md §3.3's empty-braces rule.
- **Any non-empty `{}` is always loose**, per STYLE.md §3.3's "always pad non-empty
  `{}`" rule, applied uniformly regardless of content shape — including a dict/set
  literal whose only content is `*`/`**` unpacking (e.g. `{ **defaults, **overrides }`,
  see §1.4). There is no tight carve-out for unpacking-only literals (RDD_KEY_184).

```python
empty_dict = {}
empty_set  = set()
a_set      = { 1, 2, 3 }
a_dict     = { "a": 1, "b": 2 }
```

---

## 2. Assignment Alignment

Align `=` across adjacent simple assignment statements, mirroring STYLE.md §6's
compound-operator alignment. Same group/break rules as §6: a blank line or a comment
breaks the group; an augmented-assignment operator (`+=`, `-=`, etc.) aligns in the
same group as `=`, same as §6 already does for C-family languages.

```python
flags    = 0x01
flags   |= 0x02
timeout  = 100
retries  = 3
# a comment breaks the group
name = "worker"
```

Multi-line right-hand sides follow the same continuation-alignment target as
STYLE.md §6 (break-before-operator aligns the operator to the `=` column;
break-after-operator aligns the next operand to the column after `=`) — Python has
no `\`-free ambiguity here since parenthesized continuations are the norm, but the
alignment target is unchanged either way:

```python
total = something
      + something_else

total = (
    something +
    something_else
)
```

---

## 3. Import Ordering

Python import order has no runtime-correctness requirement the way, say, a C header
guard does — imports execute top-to-bottom like any other statement, so ordering here
is purely a formatting convention, not a semantics-preserving one. This is a
deliberately simpler rule than PEP 8/`isort`'s
four-tier (stdlib/third-party/local) convention — **no stdlib-vs-third-party
classification is needed at all.**

### 3.1 Sort Key

Within a group (see §3.2 for what forms a group), sort alphabetically with priority:

1. `import` statements before `from` statements.
2. Within each keyword, sort alphabetically by module name.
3. For a `from X import a, b, c` clause, sort by the first imported name, then the
   second, and so on, if module names tie.

```python
import json
import os
import sys

from . import sibling
from .. import parent
from .helpers import util
from os import path, sep
from sys import argv
```

Relative imports (`from .`/`from ..`) sort before absolute `from` imports
naturally — `.` (0x2E) precedes any letter in plain ASCII ordering, so no
special-casing is needed; they fall out of the same alphabetical sort as regular
`from` imports.

### 3.2 Grouping — Split on Non-Import Statements

A contiguous run of import statements at the same block level forms one sortable
group. **Any non-import statement breaks the group** — including a statement that
itself contains imports nested inside it (an `if`, `try`, function body, etc.).
Imports nested inside such a block form their own separate group, sorted only among
themselves, and are never merged with imports outside that block. This is a hard
rule, not a preference: moving an import across a group boundary can change *when*
it executes relative to surrounding code, which is a real behavioral risk, not just
a cosmetic one.

```python
import os
import sys

if platform.system() == "Windows":
    import winreg          # own group — sorted alone, not merged with anything outside

import json                # new group — starts fresh, does not merge with the top pair
import re
```

### 3.3 `from __future__ import ...`

Must legally be the first executable statement in the file (a hard Python syntax
rule, not a style choice). The formatter moves it to the top **of its own group**
(not necessarily the top of the file) if it isn't already there — this is safe
specifically because it's already required to be first, so promoting it within its
existing group can't introduce the cross-group reordering risk §3.2 warns against.

```python
from __future__ import annotations

import os
import sys
```

---

## 4. Decorators

A decorator (`@app.route("/")`, `@dataclass`, `@property`) is always its own
statement-level line by grammar — there's no inline-vs-own-line placement ambiguity
to resolve, so no placement rule is needed. Structurally it's just `@` plus a name or
a call, so the `()` content reuses §1's existing bracket complexity/padding rules
directly, no extension needed. `@` binds tight to the decorator name, no space, same
as any other unary prefix.

```python
@app.route("/users/<int:user_id>")
def get_user(user_id: int) -> User:
    ...

@dataclass
class Point:
    x : int
    y : int
```

**Overflow:** since a decorator is already on its own line, there's no separate
placement-adjustment step needed — the only overflow case is the decorator's own
call exceeding STYLE.md §2's line-length limit, which wraps its argument list per
§1's normal call-argument overflow rules (one-per-line inside the decorator's `()`),
same as any other overflowing call.

```python
@app.route(
    "/users/<int:user_id>/orders/<int:order_id>/items",
    methods=["GET", "POST"],
)
def get_user_order_items(user_id: int, order_id: int):
    ...
```

**`@property` / `@x.setter`:** no special getter/setter alignment rule, unlike
STYLE.md §14's group alignment for C/Java/Kotlin or STYLE_JS_TS.md §8's `get`/`set`
accessor grouping. Those languages can compact a short accessor onto one line and
align a group of them; Python function bodies never compact (§8 explicitly excludes
`def`), so there's nothing short to align — a `@property` getter and its
`@x.setter` are just two ordinary decorated method definitions, formatted with this
section's decorator rules plus normal function-def block-body formatting, no
different from any other pair of methods that happen to share a name via decorator.

```python
@property
def x(self) -> int:
    return self._x

@x.setter
def x(self, value: int) -> None:
    self._x = value
```

---

## 5. F-Strings

An f-string's `{...}` interpolation holds an expression, not literal text — whitespace
directly inside the braces is never part of the printed output (`f"{ x + 1 }"` and
`f"{x+1}"` produce identical results), so the expression portion follows §1's normal
expression-spacing rules:

```python
name  = f"{first} {last}"
total = f"Total: {price * quantity}"
```

**Format spec is opaque.** An f-string field can carry an optional `!conversion`
(`!r`, `!s`, `!a`) and/or `:format_spec` after the expression
(`f"{value:.2f}"`, `f"{x:>10}"`) — unlike the expression itself, the format-spec
portion is a literal spec string, not code, and its characters (including whitespace,
e.g. alignment/fill specifiers) are significant to the output. Only the expression
before `!`/`:` gets spacing rules applied; everything from `!` or `:` onward is
preserved exactly as written.

```python
f"{value:.2f}"        # ".2f" is a literal spec — never touched
f"{value !r}"         # conversion marker — never touched
f"{x + 1:>{width}}"   # expression spacing applies to "x + 1";
                      # ">{width}" (including its own nested field) stays opaque
```

---

## 6. Function Signature Wrapping

STYLE.md §8's inline-vs-break-to-one-per-line rule applies as-is (inline if the full
signature fits within the line-length limit, one-per-line otherwise, closing `)`
indented to match the first character of the signature). The per-parameter alignment
target differs from C, though: C's declaration order is `type name`, so §8 aligns the
*type* column; Python's order is `name: type = default`, so the alignment target is
the `:` and, when a default value is present, the `=` — both column-aligned across
the broken-out parameter group, same alignment shape as §2's assignment alignment:

```python
def process(
    x    : int,
    y    : "List[int]",
    name : str = "default",
    desc : str = "default"
) -> Optional[str]:
    ...
```

A parameter with no type hint (bare `name` or `name=default`) still participates in
the group — it's simply padded as if its `:` column were empty, same as any
partial-row case in a STYLE.md §5/§2-style alignment grid.

The return-type arrow (`-> Optional[str]`) stays on the closing `)` line, immediately
after `)`, followed by `:` — this is a fixed position, not part of the per-parameter
alignment grid.

---

## 7. Structural Pattern Matching (`match` / `case`)

Derives from STYLE_JAVA17.md §3/§6's switch-expression pattern matching (type
patterns, record deconstruction, arrow-column alignment) by citation, not copied
content — the underlying idea (align the case-separator column across a group of
cases, one pattern per case) transfers directly. The one structural difference:
Python's `case pattern:` always takes an indented block body — there is no
single-line arrow-expression form the way Java's `->` allows — so this is closer to
Java's block-body case shape (STYLE_JAVA17.md §3.2) than its arrow-expression form.

```python
match command.split():
    case [action]:
        run(action)
    case [action, obj]:
        run(action, obj)
    case Point(x=0, y=0):
        print("Origin")
    case Point(x=x, y=y) if x == y:
        print("Diagonal")
    case [1, 2, *rest]:
        handle(rest)
    case {"action": action, **rest}:
        handle(action, rest)
    case _:
        unknown()
```

- One pattern per `case`. Whether the body stays on the same line as the pattern or
  drops to an indented block follows §8's single-statement-body rule below — same
  compactness principle as `if`/`while`/`for`, not a match-specific exception.
- **Column alignment for compact cases:** when a contiguous run of `case` lines all
  use the compact one-line form (§8), the `:` column aligns across the group, same
  spirit as STYLE_JAVA17.md §3.1's `->` alignment for Java's arrow-form switch
  expressions:
  ```python
  match command:
      case 1: return "one"
      case 2: return "two"
      case _: return "unknown"
  ```
  **All-or-nothing:** if any case in the group drops to a block body, alignment for
  that group is abandoned — no case in the group gets `:`-aligned. Same conservative
  posture as Java's own all-or-nothing rule.
- Multi-value patterns use `|` (or-pattern: `case 1 | 2 | 3:`), spaced as an
  ordinary binary operator (STYLE.md §3.1).
- A guard clause (`case Point(x, y) if x == y:`) — the `if` and its condition follow
  ordinary keyword-spacing (STYLE.md §3.2), no special treatment.
- Class deconstruction (`case Point(x=0, y=0):`), sequence (`case [1, 2, *rest]:`),
  and mapping (`case {"action": action, **rest}:`) patterns all reuse §1's existing
  bracket-complexity and star-unpacking rules directly — a pattern is just another
  bracket-content shape, not a new category.
- Wildcard `_` pattern — ordinary identifier for formatting purposes, no new rule,
  same treatment C++26's placeholder `_` gets (an unrelated language, same
  reasoning: it's just a name token).

**Closing comment:** `match` is a control-flow block for STYLE.md §7's purposes,
same category as `while`/`switch` — see AI_PREAMBLE_FULL.md §7's Defaults for the
subject-expression extraction rule (`match obj:` → `# match obj` when the subject is
exactly one identifier).

---

## 8. Single-Statement Bodies

Extends STYLE.md §10's principle (single-expression bodies stay compact, not
expanded to a block) to Python's own syntax. C-family omits `{}` for a single
statement (`if(x) return y;`); Python has no braces to omit, but the same
compactness idea maps directly onto keeping a single *simple* statement on the same
line after `:` rather than dropping to an indented block:

```python
if x: return y
while x: x -= 1

match command:
    case 1: return "one"
    case 2: return "two"
```

- Applies to `if`/`elif`/`else`, `while`, `for`, and each `case` in a `match` block —
  the constructs STYLE.md §10 already has a direct C-family analog for.
- **Never applies to `def`, `class`, `try`/`except`/`finally`, or `with`** — even
  though Python's grammar permits a single-line simple-statement suite for these
  too, each always expands to a full indented block, matching how every other
  language in this project treats them: function/method bodies are always a block
  (C/Java functions have no compact form at all; even Kotlin's compact function
  syntax, STYLE_KOTLIN.md §9, is a distinct opt-in expression-body feature, not the
  same mechanism as its `if`/`while` handling), class bodies always get STYLE.md
  §7's blank-line-and-closing-comment treatment, and `try`/`with` have no C-family
  compact analog to extend at all. A one-off exception just for `def` would look
  inconsistent against every other block in the same file, so the line is drawn at
  the exact set of constructs STYLE.md §10 already covers, not extended by Python's
  grammar simply permitting more cases.
- "Simple statement" means one that doesn't itself open a new block — `return`,
  `continue`, `break`, `pass`, a single assignment, a single call. A nested compound
  statement (another `if`, `for`, `while`, `with`, `match`) never qualifies — it
  always drops to its own indented block, since it needs one regardless.
- **Overflow:** if the one-line form would exceed STYLE.md §2's line-length limit,
  expand to a normal indented block instead — same overflow-triggered pattern used
  throughout this file (decorator overflow in §4, function signature wrapping in
  §6):
  ```python
  if some_long_condition_that_is_already_quite_verbose_and_overflows_the_limit:
      do_something()
  ```
- Python's `;`-separated multi-statement lines (`if x: y; z`) are never produced by
  the formatter regardless of length — only a single simple statement qualifies for
  the compact form; anything requiring `;` to chain statements always expands to a
  block, same "don't chain multiple statements onto one line" posture C-family
  already has (STYLE.md §10 only covers one statement per omitted-brace body, not a
  comma/semicolon-chained sequence).

---

## 9. Control Flow Blank Lines

Two related STYLE.md/AI_PREAMBLE_FULL.md rules, both un-addressed for Python until
now — neither is exclusive to brace-delimited syntax, both transfer directly since
they're about blank-line placement, not braces themselves.

### 9.1 Blank line before `return`

Same as STYLE.md §9: add a blank line before `return` when the function body is
multi-line **and** the `return` is at function scope — the final statement of the
function body, not nested inside an `if`/`while`/`for`/`match`/`with`/`try` block.
A `return` inside a nested block never gets a blank line before it under this rule.

```python
def process(data):
    result = transform(data)
    validate(result)

    return result
```

Does not apply to a compact one-line body (§8's `if x: return y` form) — same
exclusion STYLE.md §9 already states for C-family one-liner functions/single-
expression `if`.

### 9.2 Blank line before `elif` / `else`

Same as AI_PREAMBLE_FULL.md §12's default: add a blank line before `elif`/`else`
**only** when the last statement of the preceding block is an unconditional exit
(`return`, `break`, or `continue`) — note this list does not currently include
`raise`, matching the existing C-family list exactly rather than extending it (the
existing list's omission of `throw`/exception-raising applies equally to C++/Java/
Kotlin, not something introduced here for Python).

```python
def check(x):
    if x < 0:
        return None

    if x == 0:
        return 0

    return x * 2
```

In all other cases, place `elif`/`else` directly after the preceding block's last
line with no blank line, same as the C-family default.

---

## 10. Docstrings / Triple-Quoted Multiline Strings

A triple-quoted string (`"""..."""`/`'''...'''`), whether used as a docstring
immediately following a `def`/`class`/module header or as an ordinary multiline
string literal elsewhere, is opaque — its content is preserved exactly as written,
byte-for-byte, beyond the opening `"""`/`'''` itself. This extends §4's template-
literal/f-string opaque-content principle by analogy: the formatter never reflows,
reindents, or otherwise rewrites text inside the string, including any inconsistent
internal indentation the original author left in place.

```python
def status():
    """
    Health check endpoint.
        Always returns "ok" for now.
    """
    return "ok"
```

Here, the docstring's own internal indentation (`Health check endpoint.` at one
depth, `Always returns "ok" for now.` at a deeper, inconsistent depth) is left
exactly as written — the formatter only ensures the opening `"""` line itself sits
at the correct structural indent for its enclosing block, same as any other
statement (RDD_KEY_186).

---

## 11. Known Open Items

Not yet designed, deliberately deferred:

None currently — decorators, f-strings, and type-hint-heavy signature wrapping
(previously listed here) were resolved via Q&A and moved into §4–§6 above.
Structural pattern matching (`match`/`case`, §7), single-statement compound-body
compactness (§8), and control-flow blank-line placement (§9) were separate gaps
found during the AI_PREAMBLE_FULL.md review pass — not originally listed here — and
resolved the same way. Section kept for future use if new gaps surface.
