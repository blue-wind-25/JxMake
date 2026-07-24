# STYLE_CPP26.md — C++26 Construct Rules

Read [STYLE.md](STYLE.md), [STYLE_C_CPP.md](STYLE_C_CPP.md), and
[STYLE_CPP20.md](STYLE_CPP20.md) first. This file extends all three for C++26
constructs not covered there. `STYLE_CPP20.md` remains frozen as the C++17/20/23
baseline (its constructs are fully implemented and cross-checked against actual JAR
behavior) — kept in its own file rather than extended, so the riskier/newer C++26
work here doesn't mix into a file that's otherwise done.

C++26 shipped/finalized 28 March 2026. Covers only finalized C++26 constructs — no
C++29 content: C++29 only began its first WG21 meeting in June 2026, and "adopted
into draft" this early isn't the same as frozen — C++26 itself lost trivial
relocatability during its own finalization. Revisit only once C++29 itself ships.

---

## 1. Pack Indexing (`T...[i]`)

Falls under the existing array-index bracket rules (STYLE.md §3.1) with no new
padding logic — a constant or simple-op index stays tight, an index containing a
call or nested bracket goes loose, same as any other `[]`:

```cpp
template<typename... T>
using Nth = T...[N];                       // constant index — tight

template<typename... T>
using Selected = T...[ computeIndex() ];   // call inside index — loose (per §3.1)
```

No space between the pack name, `...`, and `[`; `...` binds tight to the pack name
the same way STYLE_CPP20.md treats other pack-expansion ellipses. The interior
padding itself follows the ordinary tight/loose bracket-complexity rule like any
other `[]` — a call-containing index (`computeIndex()`) gets interior spaces the
same way a loose array index would anywhere else in the project (RDD_KEY_181).

---

## 2. `= delete("reason")`

Trivial — a string literal inside an existing construct. No new spacing rule; the
string argument follows ordinary function-call-argument spacing:

```cpp
void oldApi() = delete("use newApi() instead");
```

---

## 3. Placeholder `_`

An ordinary identifier for formatting purposes — no new rule. It participates in
STYLE.md §5's declaration-alignment grid, comma spacing, etc. exactly like any other
identifier:

```cpp
auto [_, count] = getResult();
if (auto _ = acquireLock(); true) { ... }
```

---

## 4. Contracts (`pre`/`post`/`contract_assert`)

Comparable in shape to STYLE_CPP20.md's trailing-`requires`-clause handling (a clause
trailing the `)`, wrapping to its own line if the combined line overflows STYLE.md
§2's line-length limit):

```cpp
int divide(int a, int b)
    pre(b != 0)
    post(r: r * b == a)
{
    return a / b;
}
```

- Each contract clause (`pre`/`post`) gets its own line, indented one level from the
  function signature, same as a wrapped `requires` clause.
- `post(r: ...)` — the result-binding identifier (`r` here) and `:` follow normal
  identifier/colon spacing, no special padding.
- `contract_assert(cond)` inside a function body gets the same
  expression-operator spacing as a `pre`/`post` clause's argument (`x>=0` →
  `x >= 0`) — unlike an ordinary call-statement argument (which this
  project's formatter otherwise leaves verbatim), a contract condition is
  always treated as a plain expression worth normalizing.

```cpp
void process(int x) {
    contract_assert(x >= 0);
    // ...
}
```

- A lone single clause may stay inline if it fits within the line-length limit
  together with the signature, same overflow-triggered-wrap logic as
  STYLE_CPP20.md's `requires` handling. A group of **two or more** clauses
  always wraps, one clause per line, regardless of whether the combined line
  would fit — multiple contract clauses are always easier to read one per
  line than packed onto a single line, unlike a single clause or a single
  `requires` clause.

---

## 5. Reflection (`^^`, `[:`, `:]`)

**Status: tokenizer-support pass required before trusting any rule below — not
inferred from the standard's grammar alone.** These are new tokens the existing
tokenizer does not recognize at all (not new keywords slotted into an existing
grammar shape), comparable in kind to the Kotlin Step 0 tokenizer work: new
`MULTI_CHAR_OPS` entries, longest-prefix-first ordering, and a real risk of surfacing
latent tokenizer bugs the way that session found one in number-literal lexing.

The rules below are a **provisional starting point**, evidence-tested only against
the test-fixture repos below, not yet validated the way STYLE_CPP20.md's constructs
were (JAR cross-check). Treat as draft until that validation pass happens.

**Test-fixture repos** (real reflection code to validate the tokenizer pass
against, since this is the one piece that can't be trusted from the standard's
grammar alone):
- `bloomberg/clang-p2996` — Bloomberg's experimental Clang fork implementing
  P2996 reflection; the most complete open-source implementation, includes
  its own test suite under the compiler's test tree.
- `wrocpp/cpp26-reflection-examples` — blog-series source, small runnable
  `.cpp` files per post, each independently verified to compile against a
  pinned `clang-p2996` build; good bite-sized fixtures.
- `simdjson/experimental_json_builder` — a real library experimenting with
  P2996-based reflection for JSON (de)serialization, non-trivial real usage
  rather than toy examples.
- `stephenberry/glaze` — production JSON/BEVE serialization library with an
  opt-in P2996 reflection backend; larger, more idiomatic real-world usage.

```cpp
constexpr auto refl     = ^^SomeType;             // reflection operator
constexpr auto splice   = [:refl:];               // splice brackets — tight, bare value
constexpr auto computed = [: computeRefl(x) :];   // splice brackets — loose, contains a call
```

- `^^` binds tight to the operand, no space, same as C++'s existing unary operators
  (STYLE_C_CPP.md's `*`/`&` spacing). It is not a type, so it sticks to the name or
  expression that follows it, not the other way around.
- `[:` / `:]` follow the same tight/loose complexity rule as `[[ ]]`
  (STYLE_CPP20.md §4.4), applied to splice brackets the same way: simple content
  (a bare reflection value, e.g. `[:refl:]`) stays tight, content containing a call or
  nested bracket goes loose (`[: computeRefl(x) :]`). A call inside a double-bracket
  construct triggers looseness the same way it does anywhere else.
- No alignment rule is defined yet for reflection-heavy declarations (e.g. a run of
  `constexpr auto x = ^^...;` statements) — revisit once the tokenizer pass confirms
  the tokens are handled correctly at all.
