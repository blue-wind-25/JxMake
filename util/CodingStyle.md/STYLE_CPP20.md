# STYLE_CPP20.md — C++17/20/23 Construct Rules (Phase 2 — NOT YET IMPLEMENTED)

> **Implementation gate:** these rules are scoped for **after** the formatter's
> dogfood-test milestone (see `formatter/STATE.md`'s End Goal section) succeeds.
> Claude CLI sessions must not read or implement against this file until that
> milestone is marked complete — see `formatter/STATE.md` and `formatter/STATE_NEXT.md`.

Read [STYLE.md](STYLE.md) and [STYLE_C_CPP.md](STYLE_C_CPP.md) first. This file extends
both for C++17/20/23 constructs not covered there. `STYLE_C_CPP.md` remains the
baseline; nothing here overrides it except where explicitly noted.

Unlike Java 17+, no construct here requires a *correctness* change to an existing
rule (e.g. nothing breaks tokenization the way Java text blocks could) — these are
additive sections for constructs the existing rules don't yet describe.

---

## 1. Structured Bindings

```cpp
auto [b, c] = somePair;
```

The left-hand side is **not** a normal `[type][name]` declaration shape — `[b, c]` is
a bracketed binding list, not an array or index. It slots into STYLE.md §5's existing
declaration-alignment grid as a single, atomic name-cell occupying the same column a
plain name would:

```cpp
int   a     = xxx;
auto [b, c] = somePair;
```

`a` and `[b, c]` start in the same column (the grid's name column); `auto` and `int`
are both padded as the type column; `=` aligns across both rows, same as any other
group per STYLE.md §6.

**Internal spacing of the binding list** — one space after each `,`, no space
directly after `[` or before `]`, consistent with the rest of the style guide's
general comma/bracket spacing:

```cpp
auto [a, b, c] = triple;   // correct
auto [ a,b,c ] = triple;   // incorrect — no padding inside [], no missing space after comma
```

This list is treated as one opaque name-cell for *alignment* purposes (§5's grid),
but its own internal contents still follow this spacing rule independently — the two
are not in conflict, since alignment operates on the cell as a whole while spacing
operates on the cell's own rendering.

---

## 2. Concepts and `requires` Clauses

### 2.1 `concept` definition — brace style

A `concept`'s `= requires { ... }` body is an expression (assigned via `=`), not a
standalone block definition. It uses **K&R** brace placement, same as lambda bodies,
for the same reason — a `{` that is part of an expression stays on the same line:

```cpp
template<typename T>
concept Drawable = requires(T t) {
    t.draw();
    { t.area() } -> std::convertible_to<double>;
};
```

Nested compound requirements (`{ ... } -> type`) are left **completely untouched**
by the formatter — their interior is opaque, same posture as lambdas with complex
bodies.

### 2.2 Trailing `requires` clause on a function/template signature

The `requires` clause always trails the closing `)` of the parameter list — on the
same line if the `)` is inline, or on the same line as the broken-form `)` if the
signature is already broken per STYLE.md §8:

```cpp
// fits in 100 chars — inline
void draw(T t) requires Drawable<T> { ... }

// signature broken per §8, requires trails the )
void reallyLongFunctionName(
        ParamType param
) requires Drawable<T> && Serializable<T>
{ ... }
```

If the `) requires ...` combined line exceeds the 100-char soft limit, `requires`
wraps to its own line, indented one level under the function name:

```cpp
void reallyLongFunctionName(ParamType param)
        requires Drawable<T> && Serializable<T>
{ ... }
```

### 2.3 Multi-line `requires { }` expression body

Standard block indentation — body contents indented one level, closing `};` on its
own line. Nested `{ ... } -> type` compound requirements are left untouched:

```cpp
template<typename T>
concept Drawable = requires(T t) {
    t.draw();
    {
        t.area()
    } -> std::convertible_to<double>;
};
```

---

## 3. `consteval`, `constinit`, `constexpr` (as it appears post-C++17)

`consteval` and `constinit` are new modifier keywords that take columns in
`CppModifierPriority` adjacent to the existing `constexpr` entry:

```
static / extern / inline
constexpr → consteval → constinit
virtual / override / final
const / volatile
```

Before adding these columns, verify against the real `CppModifierPriority`
implementation that `constexpr` already has an entry there — do not add a
duplicate. If `constexpr` is missing (unlikely given phase-1 work), add it first
in the same slot, then `consteval` and `constinit` immediately after.

---

## 4. Other Constructs

### 4.1 Three-way comparison operator `<=>`

Single space each side, same as all other binary operators:

```cpp
auto result = a <=> b;
```

No inter-line RHS operator alignment — `<=>` inside a value expression is ordinary
content, out of STYLE.md §6's scope (which aligns only the `=` column).

### 4.2 Coroutines (`co_await`, `co_yield`, `co_return`)

Treated identically to `return` / `throw` — keyword followed by an expression, no
special formatting rule.

```cpp
co_return result;
co_yield value;
auto x = co_await asyncOp();
```

### 4.3 `if` / `switch` with init-statement

```cpp
if(auto x = f(); x > 0) { ... }
switch(auto x = compute(); x) { ... }
```

No special rule needed. STYLE.md §3.1's `isLoose` check applies to the full
condition content as usual — the `;` is just another token. A condition like
`auto x = 10; x > 0` containing no `(` or `[` tokens is correctly classified
tight; one like `auto x = f(); x > 0` containing a call `(` is correctly loose.

---

## 5. Resolved Design Decisions (Q&A session)

| Topic | Decision |
|---|---|
| `concept` brace style | K&R — `requires { }` is an expression body, same as lambda |
| `requires` clause line-break trigger | Trails `)` always; wraps to own line only when `) requires ...` exceeds 100 chars |
| Multi-line `requires { }` body | Standard block indent; nested `{ ... } -> type` compound requirements left untouched |
| `constexpr` / `consteval` / `constinit` column order | `constexpr → consteval → constinit` together; verify `constexpr` already present before adding |
| `<=>` spacing | Single space each side; no RHS operator alignment |
| Coroutines | Identical to `return`/`throw` — no special rule |
| `if`/`switch` init-statement | No special rule; `isLoose` already handles correctly via `(` / `[` detection |

**§1 structured bindings implementation note:** verify the atomic-name-cell reading
against a worked example with a trailing comment before implementing, to confirm
STYLE.md §5's comment-alignment column still lines up when the name cell is `[a, b, c]`.
