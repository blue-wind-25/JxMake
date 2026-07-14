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
if(isReady(x)):          # NOT this — see below, actually loose
if( isReady(x) ):        # contains a call — loose
a[10]                     # constant index — tight
a[ callSomething(x) ]    # call inside index — loose
```

### 1.2 Comprehensions

A fifth content category — a `for` clause plus optional `if` filters, possibly
nested — not "atom," "call," or "nested bracket." Comprehensions get their own rule:
the enclosing bracket is always loose, regardless of how simple the expression or
filter is, since a comprehension is never a bare atom/simple-op case:

```python
squares    = [ x * x for x in range(10) ]
evens      = [ x for x in range(10) if x % 2 == 0 ]
pairs      = { (x, y) for x in range(3) for y in range(3) }
lookup     = { k: v for k, v in items.items() }
```

A nested comprehension (comprehension inside a comprehension) stays loose at every
level, same nesting-propagation principle as STYLE.md §3.1.

### 1.3 Slicing

`:` inside `[]` has no analog in C/Java/Kotlin indexing, and does not itself trigger
looseness — it is punctuation between slice parts, not an operator, and is never
padded regardless of whether the enclosing bracket is tight or loose:

```python
a[1:2:3]                 # simple slice — tight
a[::2]                    # simple slice — tight
a[i+1:j-1]                # simple ops on either side of : — still tight
a[ i+1:(j*k)-1 ]          # nested () triggers outer looseness — colon stays tight
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
config  = {**defaults, **overrides}
```

Presence of `*`/`**` unpacking inside `[]`/`{}` does not by itself force looseness —
it's evaluated as any other content atom under §1.1/§1.2's rules (a plain
`[*a, *b]` stays tight; `[*get_items(), *b]` goes loose because `get_items()` is a
call).

### 1.5 Dict vs. Set Literal Disambiguation

`{}` is an empty dict, `{1, 2}` is a set, `{1: 2}` is a dict — none of the
brace-delimited languages have this dual meaning. The formatter disambiguates by
presence of `:` at the top level of the `{}` content before applying padding:

- Any top-level `:` present → dict — same padding rules as §1.1/§3.3.
- No top-level `:` → set — same padding rules as a list/array literal.
- Empty `{}` → dict (Python's own semantics — an empty set has no literal form and
  must be written `set()`), tight, same as STYLE.md §3.3's empty-braces rule.

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
flags      = 0x01
flags     |= 0x02
timeout    = 100
retries    = 3
# a comment breaks the group
name       = "worker"
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
is purely a formatting convention, not semantics-preserving in the sense STATE_*.md
uses that term elsewhere. This is a deliberately simpler rule than PEP 8/`isort`'s
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

import json                 # new group — starts fresh, does not merge with the top pair
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

### 3.4 Config

- `python-import-sort` (on/off) — toggles §3.1's alphabetical sort.
- `python-import-blank-lines` — blank lines between groups (mirrors
  `java-import-blank-lines`'s shape in STYLE_JAVA.md §7 / README.md's config table).

No `python-import-stdlib-list` / `python-import-first-party-packages` keys are
needed — the simplified sort rule above has no tier classification to configure.

---

## 4. Known Open Items

Not yet designed, deliberately deferred (see FUTURE_FEATURE_DISCUSSION.md if this
ever needs revisiting):

- Decorator formatting conventions beyond "preserve as written."
- f-string internal expression formatting (`f"{x + 1}"`) — likely mirrors §1's
  general expression-spacing rules, but not explicitly Q&A'd yet.
- Type-hint-heavy signatures (`def f(x: int, y: "List[int]") -> Optional[str]:`)
  interaction with STYLE.md §8 (Function Signatures) line-wrapping.

---

## Test-Fixture Repos

- `python/cpython` — the reference implementation's own standard library; large,
  disciplined, real-world Python at scale.
- `pallets/flask` — small-to-medium, idiomatic, widely-read real Python.
- `django/django` — large real-world Python with heavy decorator/class-based-pattern
  and dict/list literal density (good §1.5 dict-vs-set stress test).
- `psf/black` — worth including specifically because it's a formatter itself: its
  own source is real Python, and its test-fixture corpus (`tests/data`) is itself a
  curated set of formatting edge cases that may be directly reusable.
- `pallets/click` — dense decorator and nested-call-argument use, good additional
  stress test for the §1.1 tight/loose bracket heuristic on call sites.
