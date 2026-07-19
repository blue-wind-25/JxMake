# STYLE_JS_TS.md — JavaScript / TypeScript Rules

Read [STYLE.md](STYLE.md), [STYLE_JAVA.md](STYLE_JAVA.md), and
[STYLE_KOTLIN.md](STYLE_KOTLIN.md) first. JS/TS is close to Java/Kotlin in shape
(C-family brace/paren/statement structure), so this file derives its rules directly
from those two rather than restating them — most constructs are "same as
STYLE_JAVA.md §N" or "same as STYLE_KOTLIN.md §N." Only constructs with no direct
analog get a new rule here. JavaScript and TypeScript share one file (rather than a
separate file each) since TypeScript is a syntactic superset and splitting them would
mean mostly duplicated content — the same reasoning that keeps STYLE_C_CPP.md as one
file for C and C++.

Supports: latest ECMAScript (currently ES2024+) and latest TypeScript (currently 5.x).

**Out of scope: JSX/TSX.** JSX embeds XML/HTML-like tag syntax directly inside
expression position — a compound-language situation like HTML5's `<script>`/`<style>`
dispatch, not a same-file JS/TS extension — so it does not belong in this file even
once scoped; it would need its own file plan (likely a `STYLE_JSX.md` dispatching to
this file for expression content and something XML/tag-shaped for the markup itself).
The new tag-syntax tokens are also a tokenizer-support risk comparable to C++26's
reflection tokens (`STYLE_CPP26.md` §5) — new tokens the tokenizer doesn't recognize
at all, not new keywords slotted into an existing grammar shape. Currently not
spec'd, not started.

---

## 1. Baseline — Directly Inherited

The following carry over unchanged; no new rule needed:

- **Bracket/parenthesis complexity padding** — same as STYLE.md §3.1.
- **Keyword spacing** (`if(`, `for(`, `while(`, `switch(`) — same as STYLE.md §3.2.
- **`{}` initializer/block spacing** — same as STYLE.md §3.3.
- **Closing comments on blocks** — same as STYLE.md §7, applied to JS/TS constructs.
  §7 is universal, not per-language: named constructs (`class`, `enum`, `interface`,
  etc.) and control-flow blocks (`for`, `while`, `switch`, etc.) are already covered
  by this inheritance, e.g. `} // function foo`, `} // class Widget`,
  `} // interface Props`, `} // enum Status`, `} // for i` — not a new rule per
  construct, just this one inherited section applied wherever the construct exists
  in JS/TS.
- **Blank line before `return`** — same as STYLE.md §9.
- **`else`/`else if` placement** — same as STYLE.md §12.
- **`switch` formatting** — same as STYLE.md §13.

---

## 2. Statement Termination (Semicolons)

Unlike Kotlin, where omitting semicolons is a clean, unambiguous language design,
JavaScript's Automatic Semicolon Insertion (ASI) is an error-recovery mechanism, not
a design goal — it has known hazards (a bare `return` followed by a newline silently
terminates the statement before the next line's expression; a line starting with `(`
or `[` can glue onto the previous statement instead of starting fresh). Because there
is no clean no-semicolon behavior to mirror the way there is in Kotlin, the formatter
takes the opposite default: **always insert explicit semicolons, never rely on ASI.**

```typescript
const x = 1;
function f()
{
    return {
        value: x,
    };
}
```

This applies uniformly to both JS and TS; no config toggle — always-explicit avoids
the ASI hazard class entirely rather than trading one style risk for another.

---

## 3. Destructuring and Spread

No direct STYLE_JAVA.md/STYLE_KOTLIN.md analog (Kotlin's destructuring declarations,
STYLE_KOTLIN.md §12, are the closest relative but only cover `component1()`/`component2()`
tuple-style unpacking, not object/array pattern destructuring or spread).

```javascript
const { id, name, ...rest } = user;
const [first, second, ...others] = items;
const merged = { ...defaults, ...overrides };
```

- No space after `...` (spread/rest) — tight against the identifier or expression.
- Destructuring patterns follow the same `{}`/`[]` padding as STYLE.md §3.1/§3.3:
  simple destructuring stays tight (`{ id, name }`), a destructuring pattern with a
  nested pattern or default value goes loose if it would otherwise overflow STYLE.md
  §2's line-length limit, same complexity signal as any other bracket content.
- A destructuring pattern on the LHS of `const`/`let` is an ordinary declaration for
  STYLE.md §5's alignment-grid purposes — it joins an adjacent run of `const`/`let`
  declarations into the same `=`-aligned group like any other declaration shape,
  regardless of whether the LHS is a plain identifier or a destructuring pattern
  (RDD_KEY_182).

## 4. Template Literals

No direct analog to Kotlin's string templates (STYLE_KOTLIN.md §19) in terms of
existing tooling, but the same principle applies: preserve the literal's content
exactly as written, including internal whitespace and newlines — the formatter never
reflows or reindents text inside a template literal.

```javascript
const msg = `Hello, ${user.name}!`;
```

`${...}` interpolation expressions follow normal expression spacing internally
(STYLE.md §3.1), same as Kotlin's `${...}` string-template interpolation.

## 5. Function / Method Declaration Brace Style

Named function declarations (`function foo() {}`) and class methods use **Allman**
style, matching STYLE_JAVA.md §2's general Method Brace Style rule (opening `{` on
its own line) — this applies whether the function is a top-level declaration or a
method inside a class, same as Java draws the line between named methods (Allman)
and lambdas (K&R, §6 below):

```typescript
function process(data: string, count: number): Promise<Result>
{
    return doWork(data, count);
}

class Widget {
    render()
    {
        return this.template;
    }
}
```

**Exceptions that stay K&R / one-liner, not Allman:**
- Arrow function block bodies (§6) — JS/TS's lambda equivalent, same lambda
  exception STYLE_JAVA.md §2 and STYLE_KOTLIN.md §3 already carve out for Java/
  Kotlin lambdas, not a JS/TS-specific divergence.
- Getter/setter one-liner groups (§7) and other short one-liner methods that fit
  STYLE.md §14's squeeze-onto-one-line shape — a distinct mechanism from Allman's
  multi-line body case, same as Java's own `void setX(int x) { _x = x; }` example.
- An empty-body method or constructor (`constructor(...) {}`) — nothing to put on
  its own line, same reasoning as any zero-content block.

## 6. Arrow Functions

Closest analog is Kotlin's lambda-with-receiver / function types (STYLE_KOTLIN.md §17,
§17.1 for arrow spacing). Arrow spacing rule carries over directly: space before and
after `=>`, same as Kotlin's `->`.

```javascript
const add     = (a, b) => a + b;
const isEven  = (n) => n % 2 === 0;
const process = (data) => {
    return transform(data);
};
```

- Single-expression body — same-line, no braces (mirrors STYLE.md §10,
  Single-Expression Blocks).
- Block body — braces required, **K&R style** (`{` on the same line as `=>`) — an
  arrow function is JS/TS's lambda equivalent, so it follows STYLE_JAVA.md §2's
  lambda exception and STYLE_KOTLIN.md §3's matching exception, not the general
  Allman rule §5 above applies to named function declarations.
- Parameter parens: keep even for a single untyped parameter (`(n) => ...`, not
  `n => ...`) for alignment consistency with multi-parameter arrows in the same group.

## 7. Optional Chaining / Nullish Coalescing

Direct analog to Kotlin's null-safety operators (STYLE_KOTLIN.md §5). Same treatment:
`?.` is tight (no surrounding space, same as Kotlin's `?.`), `??` is spaced like a
normal binary operator (same as Kotlin's `?:`).

```typescript
const len    = str?.length ?? 0;
const name   = user?.profile?.name;
```

## 8. Getter/Setter Accessors (`get`/`set`)

Unlike Python (which loses this mechanism entirely — see STYLE_PYTHON3.md §8's
exclusion of `def` from its compact-body rule, since Python function bodies are
never single-line), JS/TS class methods always use a real block body `{}` — a short
`get`/`set` accessor is structurally just an ordinary short method with a `get`/`set`
keyword prefix, so it's a direct application of STYLE.md §14's getter/setter group
alignment, same as Java's `void setX(int x) { _x = x; }` one-liner group:

```typescript
class Point {
    get x   ()      { return this._x; }
    set x   (value) { this._x = value; }
    get y   ()      { return this._y; }
    isValid ()      { return this._x > 0 && this._y > 0; }
}
```

Same STYLE.md §14 rules apply directly: align the `)` column, `{` column, body, and
`}` column across the group; pad empty parameter lists to match the widest
signature; exclude a member from the group (write it normally below, Allman style)
if its body alone would push the line past 100 characters.

## 9. Decorators

No direct STYLE_JAVA.md/STYLE_KOTLIN.md analog (Java annotations are visually similar
but always own-line by convention; JS/TS decorators mix own-line and inline usage by
design). Structurally, a decorator is just `@Name` or `@Name(args)` — the `()`/`{}`
content reuses STYLE.md §3.1's existing tight/loose padding directly, no extension
needed. `@` binds tight to the decorator name, no space, same as any other unary
prefix (STYLE.md's general operator-spacing shape).

```typescript
@Component({
    selector: "app-widget",
})
export class Widget {
    @Input() name: string;
    @Output() changed = new EventEmitter<void>();

    constructor(@Inject(TOKEN) private service: Service) {}
}
```

**Placement** — own-line (before a class/method) vs. inline (before a property or
parameter) is preserved exactly as written; the formatter never moves a decorator
from one placement to the other.

**Overflow** — if a decorator plus the target it precedes would exceed STYLE.md §2's
line-length limit on one line, resolve in two steps, same dropped-form/one-per-line
cascade already used for call-argument overflow (AI_PREAMBLE_AESTHETIC.md Rule 1):

1. Drop the decorator to its own line, keeping the target (class/method/property) on
   the next line. If this fits, stop here.
2. If the decorator alone (with its own arguments) still overflows even on its own
   line, wrap its argument list per STYLE.md §3.1's normal call-argument overflow
   rules — one-per-line inside the decorator's `()`.

## 10. `async` / `await`

No direct Java/Kotlin analog (closest conceptually is Kotlin's `suspend`, but the
formatting is unaffected either way — both are ordinary keyword-before-expression
tokens). Treat `await` as a unary prefix operator: tight against its operand, one
space after the keyword itself:

```javascript
const data = await fetchData();
async function load()
{
    const result = await api.get(url);
    return result;
}
```

`async` before a function/arrow declaration follows the same single-space-after-
keyword rule as any other modifier keyword (STYLE_JAVA.md's modifier spacing, §2).

## 11. TypeScript Type Annotations

Colon spacing after a parameter or return type is a direct analog to
STYLE_KOTLIN.md §6's `: type` tail handling (visibility/modifiers/val|var/name `:` type
slot order) — no space before `:`, one space after:

```typescript
function process(data: string, count: number): Promise<Result>
{
    return doWork(data, count);
}

const handler: (event: Event) => void = (event) => { ... };
```

### 11.1 Union / Intersection Types (`|`, `&`)

Ordinary binary-operator spacing — space both sides, same as any other operator
(STYLE.md §3.1):

```typescript
type Status = "active" | "inactive" | "pending";
type Combined = Base & Extra;
```

For a union/intersection that overflows STYLE.md §2's line-length limit and must
wrap, the break-before-operator vs. break-after-operator choice is preserved exactly
as written, same as STYLE_PYTHON3.md §2 preserves the author's continuation-break
choice for assignment right-hand sides — the formatter doesn't rewrite one style into
the other, only aligns to the appropriate column for whichever was used:

```typescript
type X = A |
         B |
         C;

type Y = A
       | B
       | C;
```

Variable/property declarations with a type annotation align the same way STYLE.md §5
and STYLE_KOTLIN.md §6 align declaration groups — same group/group-break rules (blank
line or comment breaks the group). Two or more consecutive `type X = ...` alias
declarations, with nothing separating them, form their own `=`-aligned group the same
way — same alignment grid as any other consecutive declaration run (RDD_KEY_183).

### 11.2 Class Field Modifiers

TS has modifier keywords Java doesn't (`readonly`, `override`, `declare`), so it
can't borrow Java's `JavaModifierPriority` ordering wholesale — it gets its own
priority table, with the TS-only modifiers slotted in relative to Java's existing
order (visibility → `static` → `abstract`):

1. `declare` — TS-only ambient marker; first, since it declares the member itself is
   ambient before anything else applies.
2. `public` / `private` / `protected` (visibility) — same slot as Java.
3. `static` — same slot as Java.
4. `abstract` — same slot as Java.
5. `override` — TS-only; pairs conceptually with `abstract` (both describe a
   member's relationship to a base class), slotted right after it.
6. `readonly` — TS-only; conceptually parallels Java's `final` for fields, so it
   takes the position Java's `final` occupies (last, right before the name).

```typescript
class Widget extends Base {
    declare public static readonly MAX_COUNT: number;
    protected override readonly cache: Map<string, number>;
    private static instance: Widget;
}
```

**Alignment within a group:** same declaration-alignment grid as Java (STYLE.md §5)
— when a group has members with different modifier combinations, the shorter
modifier phrase is padded as one unit so the type column still aligns across the
whole group, same as Java's `private static final int` vs. `private int` grid:

```typescript
class Config {
    private static readonly DEFAULT : string = "en";
    private                 locale  : string;
    protected               count   : number;
}
```

## 12. Enums

TS/JS enums have no direct STYLE_KOTLIN.md analog (Kotlin enum classes can carry
methods; TS enums cannot — they're a flat member list, structurally closer to a
C++ `enum class` than a Java enum). Derives from the C++ `enum class` handling
already implemented in the JAR: members are always **one-per-line**, regardless of
whether they carry explicit values — this differs from Java's plain-enum convention
of packing values-less members onto one comma-separated line, since idiomatic TS
style consistently lists enum members one-per-line either way.

- **No explicit member values** — one member per line, no alignment needed (nothing
  to align against):
  ```typescript
  enum Color {
      Red,
      Green,
      Blue,
  }
  ```
- **Explicit member values present** — one member per line, `NAME = VALUE,`, `=`
  column-aligned across the group, same as C++'s `enum class` values (STYLE.md §6's
  alignment shape):
  ```typescript
  enum Status {
      Active   = 1,
      Inactive = 2,
      Pending  = 3,
  }
  ```
- Closing brace gets a closing comment, same convention as §1's Baseline
  (`} // enum Status`) — no trailing `;` needed, unlike C++'s `enum class`.

## 13. Generics (`<T>`)

Reuses the same bracket-complexity approach as C++/Java generics (STYLE.md §3.1's
tight/loose signal, applied to `<>` the same way it's applied to `()`/`[]`): a simple
type parameter list stays tight, one containing a nested generic or a complex
constraint goes loose.

```typescript
function identity<T>(value: T): T
{
    return value;
}

class Container<T extends Comparable<T>> { ... }
```

## 14. `interface` / `type` Alias Declarations

Structurally closest to a Kotlin `data class` or Java `record` for alignment purposes —
member/property lists inside an `interface` or object-shaped `type` alias align their
`:` the same way §11 above aligns parameter/variable type annotations:

```typescript
interface Props {
    id       : string;
    label    : string;
    onSelect : (id: string) => void;
}

type Point = {
    x : number;
    y : number;
};
```

Brace style for the declaration itself is **K&R** (`{` on the same line as the
declaration) — `interface`/`type`/`class` bodies are container constructs, not
function/method definitions, so they follow STYLE.md §11 (Non-function Block Brace
Style), the same rule Java's own class/interface/enum bodies use, not §5 above's
Allman rule for named functions/methods.

## 15. Import Ordering

TypeScript/JavaScript's `import`/`export` statements are grouped and sorted with the
same group/blank-line shape as STYLE_JAVA.md §7, but the groups themselves differ —
JS/TS has no `java.*`/`javax.*`/`com.*`/`org.*` convention. Default groups:

```typescript
import fs from "fs";                    // Group 1: built-in / node: modules

import express from "express";          // Group 2: third-party (node_modules)
import { debounce } from "lodash";

import { Widget } from "../components"; // Group 3: local / relative imports
import { helper } from "./helper";
```

Configurable, mirroring `java-import-*` shape:
```
js-import-order       = builtin, third-party, local   # group order
js-import-sort        = on                            # alphabetical within group
js-import-blank-lines = 1                             # blank lines between groups
```

**Local import detection:** an import path is "local" if it's relative (starts with
`./` or `../`) or resolves within the project's configured source root; everything
else resolvable from `node_modules` is third-party; anything matching Node's built-in
module list (or prefixed `node:`) is built-in. Not yet in the real config schema —
the resolution logic for this classification is still an open item (§15).

**Unused imports** — not removed by the formatter, same as STYLE_JAVA.md §7 — that's
the responsibility of the IDE or a separate lint tool (e.g. ESLint).

---

## Known Open Items

Not yet designed, deliberately deferred:

- **JSX/TSX** — out of scope entirely, not just deferred within this file; see
  this file's "Out of scope" note in the intro.
- Import-path built-in/third-party/local classification's resolution logic (§15) —
  not yet designed.

Implementation-tracker content (test-fixture repos, local test fixtures,
open design questions) for this file lives in `formatter/STATE_JS_TS.md`,
not here — see that file.
