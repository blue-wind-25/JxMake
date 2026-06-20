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
int    a     = xxx;
auto [b, c] = somePair;
```

`a` and `[b, c]` start in the same column (the grid's name column); `auto` and `int`
are both padded as the type column; `=` aligns across both rows, same as any other
group per STYLE.md §6.

**Internal spacing of the binding list** — one space after each `,`, no space
directly after `[` or before `]`, consistent with the rest of the style guide's
general comma/bracket spacing:

```cpp
auto [a, b, c] = triple;     // correct
auto [ a,b,c ] = triple;     // incorrect — no padding inside [], no missing space after comma
```

This list is treated as one opaque name-cell for *alignment* purposes (§5's grid),
but its own internal contents still follow this spacing rule independently — the two
are not in conflict, since alignment operates on the cell as a whole while spacing
operates on the cell's own rendering.

---

## 2. Concepts and `requires` Clauses

**Status: TBD.** No worked example agreed yet. Open questions to resolve before
implementation:

- [ ] Brace style for a `concept` definition — same K&R-vs-Allman split as
      class/struct (Allman, since it's a named top-level definition), or does a
      `concept`'s `= requires { ... };` body (an expression, not a block) follow
      different rules entirely?
- [ ] Trailing `requires` clause on a function/template signature — when present
      and the signature is otherwise short, does `requires(...)` force a line break
      even under the 100-char limit, or only when the combined length exceeds it
      (consistent with §8's existing trigger)?
- [ ] Line-breaking and indentation of a multi-line `requires { ... }` expression
      body (the "compound requirement" form) — no existing rule shape to fall back
      on, since this isn't a `{}` initializer (§3.3) or a function/control-flow
      block (§11).

---

## 3. `consteval`, `constinit`, `constexpr` (as it appears post-C++17)

**Status: TBD.**

- [ ] `consteval` and `constinit` are new modifier keywords analogous to `sealed`
      in STYLE_JAVA17.md — they need a column in `CppModifierPriority`. Exact
      ordering relative to existing modifiers (`static`, `volatile`, `const`,
      `constexpr` already present in some capacity) needs to be pinned against the
      full existing priority list before implementation, not assumed.
- [ ] Confirm `constexpr` (pre-existing keyword, but increasingly common on more
      constructs in C++17/20) doesn't already have an entry in
      `CppModifierPriority` that this section is duplicating — check the real
      implementation before adding anything.

---

## 4. Other Candidates Not Yet Scoped

Flagged for future discussion, no rules drafted yet — listed so they aren't
forgotten, not because a decision has been made:

- [ ] Three-way comparison operator `<=>` — spacing only question (is it tight or
      padded like other binary operators?); likely falls under existing binary-op
      spacing with no new rule needed, but not yet confirmed against STYLE.md §3.1's
      table.
- [ ] Coroutines (`co_await`, `co_yield`, `co_return`) — likely no special rule
      beyond existing keyword + expression spacing, but unconfirmed.
- [ ] Modules (`import`/`export module`) — likely needs its own include-ordering-
      style section analogous to STYLE_C_CPP.md §11, not yet drafted.
- [ ] `if`/`switch` with init-statement (`if(auto x = f(); x > 0)`) — existing §3.1
      complexity padding likely already handles the parenthesized content correctly
      since it's just nested expression content, but not yet verified against a
      worked example.

---

## 5. Open Questions Summary

- [ ] §1 structured bindings: confirm the atomic-name-cell reading against a second
      worked example with a 4+ element binding list and a trailing comment, to make
      sure STYLE.md §5's comment-alignment column still lines up correctly.
- [ ] §2 concepts/requires: needs a full design discussion before any checklist
      item is actionable — currently just a list of unresolved questions, not a
      spec.
- [ ] §3 consteval/constinit: needs the real current `CppModifierPriority` ordering
      checked first; do not guess a column position without it.
