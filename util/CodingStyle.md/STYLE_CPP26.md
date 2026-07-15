# STYLE_CPP26.md — C++26 Construct Rules

Read [STYLE.md](STYLE.md), [STYLE_C_CPP.md](STYLE_C_CPP.md), and
[STYLE_CPP20.md](STYLE_CPP20.md) first. This file extends all three for C++26
constructs not covered there. `STYLE_CPP20.md` remains frozen as the C++17/20/23
baseline (its constructs are fully implemented and cross-checked against actual JAR
behavior) — kept in its own file rather than extended, the same reasoning
STATE_KOTLIN.md gives for staying self-contained rather than folded into STATE.md,
so the riskier/newer C++26 work here doesn't mix into a file that's otherwise done.

C++26 shipped/finalized 28 March 2026. Covers only finalized C++26 constructs — no
C++29 content (see FUTURE_FEATURE_DISCUSSION.md for why: C++29 only began its first
WG21 meeting in June 2026, and "adopted into draft" this early isn't the same as
frozen — C++26 itself lost trivial relocatability during its own finalization).

---

## 1. Pack Indexing (`T...[i]`)

Falls under the existing array-index bracket rules (STYLE.md §3.1) with no new
padding logic — a constant or simple-op index stays tight, an index containing a
call or nested bracket goes loose, same as any other `[]`:

```cpp
template<typename... T>
using Nth = T...[N];              // constant index — tight

template<typename... T>
using Selected = T...[computeIndex()];   // call inside index — loose (per §3.1)
```

No space between the pack name, `...`, and `[`; `...` binds tight to the pack name
the same way STYLE_CPP20.md treats other pack-expansion ellipses.

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
- `contract_assert(cond)` inside a function body is formatted like any other
  function-call-shaped statement — STYLE.md §3.1's complexity rule decides
  tight/loose for its argument.

```cpp
void process(int x) {
    contract_assert(x >= 0);
    // ...
}
```

- If all contract clauses plus the signature fit within the line-length limit on one
  line, they may stay inline, same overflow-triggered-wrap logic as
  STYLE_CPP20.md's `requires` handling.

---

## 5. Reflection (`^^`, `[:`, `:]`)

**Status: tokenizer-support pass required before trusting any rule below — not
inferred from the standard's grammar alone.** These are new tokens the existing
tokenizer does not recognize at all (not new keywords slotted into an existing
grammar shape), comparable in kind to the Kotlin Step 0 tokenizer work: new
`MULTI_CHAR_OPS` entries, longest-prefix-first ordering, and a real risk of surfacing
latent tokenizer bugs the way that session found one in number-literal lexing.

The rules below are a **provisional starting point**, evidence-tested only against
the test-fixture repos listed in FUTURE_FEATURE_DISCUSSION.md, not yet validated the
way STYLE_CPP20.md's constructs were (JAR cross-check). Treat as draft until that
validation pass happens.

```cpp
constexpr auto refl   = ^^SomeType;              // reflection operator
constexpr auto splice = [:refl:];                // splice brackets — tight, bare value
constexpr auto computed = [: computeRefl(x) :];  // splice brackets — loose, contains a call
```

- `^^` binds tight to the operand, no space, same as C++'s existing unary operators
  (STYLE_C_CPP.md's `*`/`&` spacing). It is not a type, so it sticks to the name or
  expression that follows it, not the other way around.
- `[:` / `:]` follow the same tight/loose complexity rule as `[[ ]]`
  (STYLE_CPP20.md §4.4), applied to splice brackets the same way: simple content
  (a bare reflection value, e.g. `[:refl:]`) stays tight, content containing a call or
  nested bracket goes loose (`[: computeRefl(x) :]`). This mirrors the existing
  JAR-verified `[[ assume(a >= 0) ]]` case — a call inside a double-bracket construct
  triggers looseness the same way it does anywhere else.
- No alignment rule is defined yet for reflection-heavy declarations (e.g. a run of
  `constexpr auto x = ^^...;` statements) — revisit once the tokenizer pass confirms
  the tokens are handled correctly at all.

**Test-fixture repos** (same list as FUTURE_FEATURE_DISCUSSION.md, carried here since
this is the section that actually needs them for validation):
- `bloomberg/clang-p2996`
- `wrocpp/cpp26-reflection-examples`
- `simdjson/experimental_json_builder`
- `stephenberry/glaze`

---

## 6. Config

No new config keys. §1–4 reuse existing STYLE.md/STYLE_CPP20.md logic with no
toggle-able behavior; §5 (Reflection) is still provisional and pending the tokenizer
validation pass, so it's premature to define config for it — any knob added now would
be speculation about a rule set that isn't trusted yet. Revisit once §5 graduates out
of draft status.

## 7. Test Fixtures (Local)

Planned local dogfood pairs (unlike §5's external-repo list above, which is for
corpus-scale reflection validation) are staged in **FUTURE_TEST_FIXTURES.md**, under
its "CPP26" section — not written here, so this file doesn't carry fixture-status
content that isn't actually a style rule. See that file for the pair list and what
each covers.

**Not a style reference — must move at implementation time.** Like the rest of this
section's content, the cross-reference to FUTURE_TEST_FIXTURES.md itself is
implementation-tracker information, not a style rule. When `STATE_CPP26.md` is
created (§8 below), this pointer moves there too, same as the rest of this section's
content — FUTURE_TEST_FIXTURES.md's "CPP26" section is then emptied out (its pairs
having been authored and registered in `formatter/test/README.txt` per that file's
own instructions).

## 8. Implementation Note

This file is style rules only — it intentionally carries no implementation-tracker
content (open questions, commit history, ambiguity log), matching STATE_C_CPP_JAVA.md's
separation of concerns for the existing languages.

When actual JAR implementation of C++26 support begins, create `STATE_CPP26.md`:
copy `formatter/STATE_C_CPP_JAVA.md`, strip everything not relevant to C++26, and fold
in the non-style content from this file (open items, provisional/draft flags like §5's
tokenizer-gate, the §7 FUTURE_TEST_FIXTURES.md pointer) as tracker entries. This file
then goes back to being pure style rules, same as STYLE_C_CPP.md/STYLE_CPP20.md are
today.

The same extract-copy-modify step applies when implementation begins for
STYLE_JS_TS.md, STYLE_DATA_FORMATS.md, and STYLE_PYTHON3.md — each spins off its own
`STATE_*.md` at that point, copied from `STATE_C_CPP_JAVA.md` and adapted the same way,
including moving their own FUTURE_TEST_FIXTURES.md pointer into their new STATE file.
