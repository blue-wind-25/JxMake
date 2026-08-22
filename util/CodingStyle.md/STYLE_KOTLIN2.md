# STYLE_KOTLIN2.md — Kotlin 2.0+ Construct Rules

Read [STYLE.md](STYLE.md) and [STYLE_KOTLIN.md](STYLE_KOTLIN.md) first. This file
extends both for Kotlin 2.0+ constructs not covered there. `STYLE_KOTLIN.md`
remains the 1.0–1.9 baseline; nothing here overrides it except where explicitly
noted. This file is intentionally short — most 2.0+ additions extend existing
rules rather than introducing new ones.

---

## 1. Guard Conditions in `when` (2.0)

An extra `if` condition after the branch pattern, before `->`:

```kotlin
when(x) {

    is String if x.isNotEmpty() -> foo()
    else                        -> bar()

} // when x
```

No new rule — extends STYLE_KOTLIN.md §4 as-is. The `if <condition>` clause is
part of the branch's label content for alignment purposes; `->` still aligns
across all branches in the same column, including branches with a guard.

---

## 2. `data object` (1.9, bucketed here)

Technically a 1.9 addition, but commonly seen alongside 2.0-era code. Formatted
exactly like a regular `object` declaration — STYLE_KOTLIN.md §3.1 applies
unchanged (K&R brace, always-blank-lines, always closing comment):

```kotlin
data object EmptyState {

    val label = "empty"

} // data object EmptyState
```

---

## 3. Other 2.0/2.1 Features — No New Formatting Rules

The following are language/compiler capabilities with no formatting impact beyond
rules already established in STYLE_KOTLIN.md:

- **K2 compiler** — implementation detail, no syntax change.
- **Non-local `break`/`continue` in lambdas** (2.1) — no new tokens to format.
- **Multi-dollar string interpolation** (`$$`, 2.1) — falls under
  STYLE_KOTLIN.md §19 (string templates preserved exactly as written).
- **Context parameters** (experimental, 2.1+) — not yet in scope; revisit once
  the feature stabilizes and appears in actual project code.
- **Unnamed placeholder `_`** in a lambda parameter list (`{ _, value -> ... }`) —
  formatted identically to any other lambda parameter for spacing purposes
  (STYLE_KOTLIN.md §17.1's arrow spacing, normal comma spacing). See
  STYLE_KOTLIN.md §12 for the destructuring-declaration form of the same
  placeholder.
