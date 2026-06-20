# STYLE_JAVA17.md — Java 17+ Construct Rules (Phase 2 — NOT YET IMPLEMENTED)

> **Implementation gate:** these rules are scoped for **after** the formatter's
> dogfood-test milestone (see `formatter/STATE.md`'s End Goal section) succeeds.
> Claude CLI sessions must not read or implement against this file until that
> milestone is marked complete — see `formatter/STATE.md` and `formatter/STATE_NEXT.md`.

Read [STYLE.md](STYLE.md) and [STYLE_JAVA.md](STYLE_JAVA.md) first. This file extends
both for Java 17+ constructs not covered there. `STYLE_JAVA.md` remains the Java 8
baseline; nothing here overrides it except where explicitly noted.

---

## 1. `record`

A `record` declaration is treated exactly like a `class` for every existing rule:

- Brace style: K&R, same as class/interface/enum body braces (STYLE.md §11).
- Closing comment: always included, same as a named construct (STYLE.md §7,
  STYLE_JAVA.md §6) — `} // record Point`.
- Forced blank line after `{` and before `}` (STYLE.md §7's named-construct rule)
  applies the same as it does to `class`.

```java
public record Point(int x, int y) {

    // ... compact constructor, additional members, etc. ...

} // record Point
```

**Component list** (the `(int x, int y)` part) follows the same line-breaking and
column-alignment rules as a function signature (STYLE.md §8) — inline if it fits
within the 100-char soft limit, otherwise broken one-component-per-line with the
same closing-`)` placement rule.

**Compact canonical constructor** (`public Point { ... }` — no parameter list) is a
method-shaped body and follows ordinary method body rules (STYLE_JAVA.md §2), not
the getter/setter one-liner rules, since it is rarely a single expression.

---

## 2. `sealed` / `non-sealed` / `permits`

`sealed`, `non-sealed`, and `permits` are new modifier/clause keywords on `class` and
`interface` declarations:

```java
public sealed interface Shape permits Circle, Square, Triangle {
    ...
}

public final class Circle implements Shape { ... }
public non-sealed class Square implements Shape { ... }
```

**Modifier priority column:** `sealed` / `non-sealed` take a column in
`JavaModifierPriority`, ordered immediately after `final`/`abstract` and before the
type keyword (`class`/`interface`) — i.e. they occupy the same conceptual slot as
`final`, since exactly one of `final` / `sealed` / `non-sealed` / (none) applies to
a given type.

**`permits` clause:** treated like an `implements`/`extends` clause for line-breaking
purposes — inline if the full declaration line fits within 100 chars; if not, break
with one permitted type per line, indented one level, comma-trailing:

```java
public sealed interface Shape
    permits Circle,
            Square,
            Triangle {
    ...
}
```

---

## 3. Switch Expressions

Switch *expressions* (`->` arrow form, multi-label cases, `yield`) are a distinct
construct from the switch *statement* covered in STYLE.md §13 — that section's
`:`-based inline/non-inline alignment rules do not apply here.

### 3.1 Arrow form — always one case per line, never inline-aligned like §13

```java
String result = switch(day) {
    case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "Weekday";
    case SATURDAY, SUNDAY                              -> "Weekend";
};
```

The `->` column is aligned across all cases in the switch, same spirit as STYLE.md
§13's inline `:` alignment — pad each case's label (including multi-value
comma-separated labels) so `->` lands in the same column.

### 3.2 Block bodies with `yield`

When a case's right-hand side is not a single expression, use a block body with
explicit `yield`:

```java
String result = switch(day) {
    case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> {
        logWeekday(day);
        yield "Weekday";
    }
    case SATURDAY, SUNDAY -> "Weekend";
};
```

The block `{` uses K&R (same line as `->`), consistent with STYLE.md §11's general
non-function-block rule. No closing comment is added to this `}` — it is not a
named construct and not one of STYLE.md §7's control-flow cases.

### 3.3 Exhaustiveness / `default`

No special formatting rule beyond ordinary case alignment — a `default ->` case
aligns into the same `->` column as the others.

---

## 4. Text Blocks (`"""`)

Text blocks are left **completely untouched** — formatter does not modify
indentation, content, or delimiter placement inside a `"""..."""` block under any
circumstance. Treated as an opaque token by the tokenizer, the same way a regular
string literal is opaque, just spanning multiple lines.

```java
String json = """
    {
        "key": "value"
    }
    """;
```

The above indentation (or any indentation) inside the text block is preserved
exactly as written, regardless of the surrounding code's indent level.

---

## 5. `var` (local-variable type inference)

No special alignment rule — `var` occupies the type column in STYLE.md §5's
declaration-alignment grid exactly like any other type name. If a group mixes `var`
and explicit types, they align normally (column width = widest type token, `var`
included).

```java
var    count   = 0;
int    total   = computeTotal();
String label   = "done";
```

---

## 6. Pattern Matching (`instanceof`, switch patterns)

### 6.1 Pattern matching for `instanceof`

```java
if(obj instanceof String s) {
    ...
}
```

No special rule — the pattern variable (`s`) is just part of the condition token
stream; STYLE.md §3.1 complexity padding applies to the `instanceof` expression as
it would to any other condition content.

### 6.2 Pattern matching in switch (type patterns, record deconstruction)

```java
String describe(Object obj) {
    return switch(obj) {
        case Integer i      -> "int " + i;
        case String s       -> "string " + s;
        case Point(int x, int y) -> "point " + x + "," + y;
        default             -> "unknown";
    };
}
```

Same §3 arrow-form alignment rules apply — the `->` column aligns across cases
regardless of whether the label is a type pattern, a record deconstruction
pattern, or a plain value.

---

## 7. Open Questions (deferred to implementation time)

- [ ] Switch-expression `->` alignment: does a case with a block body (§3.2) get
      excluded from the `->` column-alignment group the same way GetterSetterRule
      excludes oversized outliers (STYLE.md §14), or does block-bodied presence
      break the whole group's alignment?
- [ ] Record component list alignment when broken (§1): confirm against a worked
      multi-component example that this exactly matches STYLE.md §8's existing
      closing-`)` placement rule with no record-specific deviation.
- [ ] `sealed`/`non-sealed` modifier-priority exact column index relative to every
      other existing `JavaModifierPriority` entry — needs to be pinned against the
      full existing priority list, not just "near final/abstract," before
      implementation.
