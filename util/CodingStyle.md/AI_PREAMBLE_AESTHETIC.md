# AI Formatter Preamble — Layout Judgment Pass

You are reviewing source code that has already been processed by the code-formatter
deterministic JAR formatter. Your task is a **layout judgment pass** — a targeted
second pass that handles the small class of aesthetic decisions the JAR intentionally
leaves untouched.

**Output only the full reformatted source file. No explanation. No markdown fences.**
Preserve all logic, comments, and identifiers.

---

## Scope

This preamble is for **capable general-purpose models only** (Claude Sonnet / Opus,
GPT-4o, Gemini 1.5 Pro, and equivalents). Do not use it with small on-device models.

The JAR has already applied all deterministic formatting rules, for C, C++, Java, and
Kotlin alike — Kotlin support is on the same footing as the other three (same
dogfood-testing process, same regression-fix cadence; see
`formatter/STATE_KOTLIN.md`/`formatter/RDD_LOG.md`), with one narrow, deliberate
exception covered in Rule 2 below. Your job covers exactly two things, language-agnostic
across all four:

1. **Function argument list layout** — reflow arg lists that are aesthetically poor
   (see rules below).
2. **Non-standard getter/setter grouping** — identify and align accessor clusters the
   JAR could not detect (see rules below).

Everything else is already correct. Do not touch it.

**Does not apply to C++26 additions, JSON/JSON5/XML/CSS/HTML5, JavaScript/
TypeScript, or Python3.** This entire preamble is premised on a JAR-processed input
file — none of these four have JAR support at all (see AI_PREAMBLE_FULL.md's Scope
section), so there is no deterministic baseline for a layout judgment pass to sit on
top of. Use AI_PREAMBLE_FULL.md exclusively for these languages until JAR support
exists; this file's rules below are C/C++/Java/Kotlin-only.

---

## Hard Constraints — What You Must Never Change

The following have been formatted by the JAR and must be treated as **opaque** —
do not reformat, realign, or reflow them under any circumstances:

- Column-aligned declaration groups (STYLE.md §5/§6)
- Getter/setter aligned groups (STYLE.md §14) — except the non-standard grouping
  case described below, which you are explicitly asked to fix
- Switch case alignment (STYLE.md §13)
- Function/method signature line-breaking (already deterministic — inline if ≤ 100
  chars, one-per-line otherwise)
- Preprocessor directives, string literals, text blocks, lambda bodies
- Any comment content — you may reposition trailing comments to align with a group,
  but never alter comment wording

If you are uncertain whether a block has been JAR-formatted, leave it untouched.

---

## Rule 1 — Function Argument List Layout

This applies to function/method **calls** and forward **declarations** (prototype
parameter lists). It does not apply to function/method definitions — those are
already handled deterministically by the JAR.

### When to reflow

Only reflow an arg list if it meets one of these conditions:

- It is on a single line and exceeds 100 characters — reflow to multi-line.
- It is already multi-line but the layout looks arbitrary (no clear grouping rationale,
  ragged line lengths with no semantic reason) — reflow to one-per-line.

Leave multi-line arg lists that have a clear grouping rationale (related args on the
same line, consistent group sizes) exactly as they are — the author expressed intent
through that layout.

### How to reflow

**If a single line exceeds 100 chars:** try dropping all args to one indented line
below `(`, with `)` on its own line at the call's indentation level. If that line
still exceeds 100 chars, use one-per-line instead.

```c
// dropped form (args fit on one indented line)
someFunction(
    arg1, arg2, arg3
);

// one-per-line (args do not fit on one indented line)
someFunction(
    arg1,
    arg2,
    arg3
);
```

**If already multi-line with arbitrary layout:** reflow to one-per-line.

**If already multi-line with clear grouping:** leave untouched.

### Comments within arg lists

- Trailing comment after an arg — leave in place, align to group column.
- Comment-only line between arg groups — leave in place, do not reflow around it.
  If the surrounding layout must change, migrate the comment to trail the arg it follows.
- Leading preamble comment above the first arg — leave the entire arg list untouched;
  this is a strong signal the author wants the layout preserved.

---

## Rule 2 — Non-Standard Getter/Setter Grouping

The JAR aligns getter/setter groups automatically when methods use standard naming
prefixes (`get`, `set`, `is`), for all four languages. It cannot detect groups using
non-standard names, in any of them.

When you find a cluster of short accessor-style methods that form a logical group but
use non-standard prefixes (`fetch`, `retrieve`, `assign`, `enable`, `toggle`, etc.),
apply the same inline alignment the JAR uses for standard groups (STYLE.md §14):
methods on one line each, columns aligned across the group.

Do **not** rename the methods — alignment only. If the names are inconsistent within
the group (e.g. `fetchX` alongside `getY`), flag the inconsistency in a comment
appended at the very end of the file rather than silently aligning a mixed group.

**Kotlin exception — three residual accessor shapes.** The JAR already groups/aligns
the common Kotlin one-liner-accessor shapes automatically (expression-bodied functions
and plain no-initializer `get() = expr` properties), same as it does for C/C++/Java's
standard-prefix groups. A narrower, deliberate gap remains — not an immaturity issue,
just considerably more complex to group correctly than the one-liner cases — covering
three shapes, all left preserved-as-written by the JAR rather than grouped: block-bodied
accessors (`get() {...}`/`set(v) {...}`), a property pairing a getter with a setter, and
a property with both an initializer and a custom accessor (see
`formatter/STATE_KOTLIN.md`'s Open Questions). For Kotlin files, treat only *these three
shapes* as if they were non-standard groups: align them per the rule above. Leave any
already-aligned expression-bodied/`get() = expr` group the JAR produced untouched.
