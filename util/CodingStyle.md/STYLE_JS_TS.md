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

---

## 1. Baseline — Directly Inherited

The following carry over unchanged; no new rule needed:

- **Bracket/parenthesis complexity padding** — same as STYLE.md §3.1.
- **Keyword spacing** (`if(`, `for(`, `while(`, `switch(`) — same as STYLE.md §3.2.
- **`{}` initializer/block spacing** — same as STYLE.md §3.3.
- **Closing comments on blocks** — same as STYLE.md §7, applied to JS/TS constructs
  (`} // function foo`, `} // class Widget`, `} // interface Props`).
- **Blank line before `return`** — same as STYLE.md §9.
- **`else`/`else if` placement** — same as STYLE.md §12.
- **`switch` formatting** — same as STYLE.md §13.

---

## 2. Destructuring and Spread

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
  nested pattern or default value goes loose if it would otherwise overflow §2's
  line-length limit, same complexity signal as any other bracket content.

## 3. Template Literals

No direct analog to Kotlin's string templates (STYLE_KOTLIN.md §19) in terms of
existing tooling, but the same principle applies: preserve the literal's content
exactly as written, including internal whitespace and newlines — the formatter never
reflows or reindents text inside a template literal.

```javascript
const msg = `Hello, ${user.name}!`;
```

`${...}` interpolation expressions follow normal expression spacing internally
(STYLE.md §3.1), same as Kotlin's `${...}` string-template interpolation.

## 4. Arrow Functions

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
- Block body — braces required, same brace style as STYLE_JAVA.md §2 (Method Brace
  Style).
- Parameter parens: keep even for a single untyped parameter (`(n) => ...`, not
  `n => ...`) for alignment consistency with multi-parameter arrows in the same group.

## 5. Optional Chaining / Nullish Coalescing

Direct analog to Kotlin's null-safety operators (STYLE_KOTLIN.md §5). Same treatment:
`?.` is tight (no surrounding space, same as Kotlin's `?.`), `??` is spaced like a
normal binary operator (same as Kotlin's `?:`).

```typescript
const len    = str?.length ?? 0;
const name   = user?.profile?.name;
```

## 6. `async` / `await`

No direct Java/Kotlin analog (closest conceptually is Kotlin's `suspend`, but the
formatting is unaffected either way — both are ordinary keyword-before-expression
tokens). Treat `await` as a unary prefix operator: tight against its operand, one
space after the keyword itself:

```javascript
const data = await fetchData();
async function load() {
    const result = await api.get(url);
    return result;
}
```

`async` before a function/arrow declaration follows the same single-space-after-
keyword rule as any other modifier keyword (STYLE_JAVA.md's modifier spacing, §2).

## 7. TypeScript Type Annotations

Colon spacing after a parameter or return type is a direct analog to
STYLE_KOTLIN.md §6's `: type` tail handling (visibility/modifiers/val|var/name `:` type
slot order) — no space before `:`, one space after:

```typescript
function process(data: string, count: number): Promise<Result> {
    return doWork(data, count);
}

const handler: (event: Event) => void = (event) => { ... };
```

Variable/property declarations with a type annotation align the same way STYLE.md §5
and STYLE_KOTLIN.md §6 align declaration groups — same group/group-break rules (blank
line or comment breaks the group).

## 8. Generics (`<T>`)

Reuses the same bracket-complexity approach as C++/Java generics (STYLE.md §3.1's
tight/loose signal, applied to `<>` the same way it's applied to `()`/`[]`): a simple
type parameter list stays tight, one containing a nested generic or a complex
constraint goes loose.

```typescript
function identity<T>(value: T): T { return value; }
class Container<T extends Comparable<T>> { ... }
```

## 9. `interface` / `type` Alias Declarations

Structurally closest to a Kotlin `data class` or Java `record` for alignment purposes —
member/property lists inside an `interface` or object-shaped `type` alias align their
`:` the same way §7 above aligns parameter/variable type annotations:

```typescript
interface Props {
    id:       string;
    label:    string;
    onSelect: (id: string) => void;
}

type Point = {
    x: number;
    y: number;
};
```

Brace style for the declaration itself follows STYLE_JAVA.md §2 (Method Brace Style),
applied to `interface`/`type`/`class` bodies the same way it already applies to Java
class bodies.

## 10. Import Ordering

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
js-import-blank-lines = 1                              # blank lines between groups
```

**Local import detection:** an import path is "local" if it's relative (starts with
`./` or `../`) or resolves within the project's configured source root; everything
else resolvable from `node_modules` is third-party; anything matching Node's built-in
module list (or prefixed `node:`) is built-in. Same "not yet in the real config
schema" caveat as the config-properties notes in FUTURE_FEATURE_DISCUSSION.md — the
built-in/third-party/local classification needs the same kind of resolution logic as
Python3's stdlib-vs-third-party question there, tracked as an open item.

**Unused imports** — not removed by the formatter, same as STYLE_JAVA.md §7 — that's
the responsibility of the IDE or a separate lint tool (e.g. ESLint).

---

## Test-Fixture Repos

- `nodejs/node` — large, real, mixed-style JS codebase (core + tooling).
- `expressjs/express` — smaller, idiomatic, widely-read real-world JS.
- `lodash/lodash` — dense functional-style JS, good stress test for complexity-based
  bracket padding (STYLE.md §3.1) on chained calls.
- `microsoft/TypeScript` — the compiler itself; canonical, heavily-typed real-world TS
  at scale; also doubles as a JS fixture.
- `angular/angular` — large, idiomatic, decorator-heavy real TS.
- `nestjs/nest` — decorator- and generic-heavy backend TS, good coverage of the
  type-annotation-alignment cases (§7, §9).
- `vuejs/core` — modern TS with heavy generics and type-level code.
