[//]: # (Copyright (C) 2022-2026 Aloysius Indrayanto)
[//]: # (This file is part of the JxMake build system and is distributed under the MIT License.)
[//]: # (See the LICENSE file in the formatter root directory for the full MIT license text.)

# STATE.md — Formatter Implementation Tracker

---

## Instructions for Claude CLI

**Read this section first, every session, before doing anything else.**

### Session start
1. Read this entire file to understand current state
2. Check the **File Status** table to find the current file (`IN PROGRESS` first,
   then the first `NOT STARTED`)
3. Check the **Current File** checklist for unchecked items — that is where to resume
4. If anything in this file is ambiguous, stop and ask before writing any code

### During implementation
- Implement one checklist section at a time
- After completing a section (or when the cumulative diff across all changed files
  exceeds ~50 lines, whichever comes first), do a checkpoint commit:
  1. Update STATE.md — check off completed items, update File Status table
  2. `git add util/CodingStyle.md/formatter/` (the entire formatter directory)
  3. `git reset util/CodingStyle.md/formatter/target/` (exclude build output)
  4. `git commit -m "<message>"` — short descriptive message, no strict format required,
     trailer ending with `Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>`
- Small related items within a section may be grouped into one commit if they
  are trivially connected — use judgment based on line count (~50 lines threshold)
- Never let implemented files and STATE.md drift out of sync — STATE.md must
  always reflect the true current state at every commit

### `.gitignore` — add these lines if not already present
```
# style-fmt build output
target/
*.jar
```

### When hitting an ambiguity or open question
1. **Stop coding immediately** — do not guess or proceed past the ambiguity
2. Update STATE.md: add the question to **Open Questions**, mark the blocked
   checklist item with `[~]` and a note
3. Commit STATE.md only:
   ```
   git add util/CodingStyle.md/formatter/STATE.md
   git commit -m "$(cat <<'EOF'
style-fmt: block on <question summary>

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
   )"
   ```
4. Ask the user and wait for an answer before continuing
5. Once resolved: record the decision in **Resolved Design Decisions**, remove
   from **Open Questions**, unblock the checklist item, then continue

### When a file reaches COMPLETE
1. Mark it `COMPLETE` in the File Status table
2. Replace the **Current File** checklist with the checklist for the next file
3. Commit STATE.md together with the completed source file

### Session end
- Always leave STATE.md committed and up to date before ending the session
- The next session will resume from the first unchecked item in the Current File checklist

---

## Project Layout

```
util/CodingStyle.md/formatter/
  STATE.md                  ← this file
  README.md
  FORMATTER_DISCUSSION.md
  Makefile
  LICENSE
  src/
    com/jxmake/formatter/
      Main.java
      Config.java
      ServerMode.java
      tokenizer/
        TokenizerCore.java
      grid/
        ColumnGrid.java
        ModifierPriority.java
        CppModifierPriority.java
        JavaModifierPriority.java
      evaluator/
        ComplexityPaddingEvaluator.java
      rules/
        DeclarationAlignmentRule.java
        BlockStructureRule.java
        SwitchRule.java
        GetterSetterRule.java
        MiscRule.java
        CppSpecificRule.java
        JavaSpecificRule.java
  target/
```

---

## Resolved Design Decisions

Decisions settled during design discussion — carried here so implementation does not
re-open them.

| Topic | Decision |
|---|---|
| Tokenizer | Write fresh in Java — no external lexer library |
| Rule engine | Direct Java methods, grouped into logical rule classes (not one class per rule) |
| Shared grid | `ColumnGrid` is its own class, used by declaration, getter/setter, switch, and signature rules |
| Modifier priority | Abstract `ModifierPriority` base with `CppModifierPriority` and `JavaModifierPriority` subclasses |
| Constants | Fixed (non-configurable) values → `private static final` at top of owning class. Configurable values → instance field with default, overridden by config file |
| Java parsing | Tokenizer + recursive descent — no AST, no tree-sitter, no Eclipse JDT |
| AI dependency | None — all rules are deterministic |
| JAR target | Java 8 bytecode (see Makefile), runs on JVM 8+ |
| Server mode | Localhost HTTP + lockfile (`~/.config/style-fmt/server.lock`) |
| Server idempotency | Check lockfile first; if PID in lockfile is not alive (`ProcessHandle.of(pid).isPresent()`), treat as stale, delete lockfile, start fresh. Handles SIGKILL and manual lockfile deletion gracefully |
| Port | Default `17173`, configurable; lockfile carries actual port used |
| Path separator | Use `/` throughout Java code — JVM normalizes on all platforms including Windows |
| Lockfile location | `System.getProperty("user.home")` + `/.config/style-fmt/` |
| Line endings | Default `lf`; configurable: `lf \| crlf \| preserve`. `preserve` detects dominant ending per file |
| Config precedence | built-in class defaults → `~/.config/style-fmt/config` → `STYLEFMT_*` env vars → `.style-fmt` project root → `.style-fmt` subdir → CLI flags |
| `.style-fmt` inheritance | Full inheritance from parent `.style-fmt` with child keys overriding — standard cascade |
| Multi-module Java imports | Use `java-import-depth` for Java 9+ modules; fall back to top-N components of `package` declaration for pre-Java-9 module-less projects |
| Windows support | Best-effort — `ProcessHandle` and `/` paths work; some path overrides in config may not. Documented in README.md |
| Output modes | In-place (default), `--diff`, `--check` (CI), `--out DIR` |
| Build | Makefile (already in project) |
| `ColumnGrid` flush API | Caller-driven: `addRow(String[] cells)` buffers; explicit `flush()` computes max-width per column, pads, returns padded rows, clears buffer. `ColumnGrid` never inspects tokens/blank lines itself — each rule class (declaration, getter/setter, switch, signature) decides its own group-boundary policy and calls `flush()` accordingly. Ragged rows: a cell is only padded if a later column exists in that same row, so no trailing whitespace is introduced |
| §3.1 complexity padding algorithm | `isLoose(contentTokens)` = true iff `contentTokens` (the significant tokens strictly between one matching bracket pair, as extracted by the caller) contains any `(` or `[` PUNCT token. This single flat check subsumes the "function call" trigger (a call always introduces a `(` token) and is the only reading consistent with every row in STYLE.md §3.1's table — including `if( (a == 1) \|\| (b > 2) )`, which is loose purely because nested parens are structurally present, not because `a == 1` or `b > 2` would themselves be loose in isolation. FORMATTER_DISCUSSION.md's recursive "propagate only if the inner result is loose" pseudocode would mark that example tight, contradicting the STYLE.md table, so it is treated as informal discussion notes rather than the spec. No recursion is needed inside the evaluator itself — each bracket pair's own looseness is independent and the caller invokes `isLoose` once per pair (bottom-up, if it chooses) using whatever slice it extracts |
| Declaration-statement detection | No AST is available, so a statement (tokens up to a top-level `;`, within a single already-extracted scope slice) is recognized as a variable/field declaration only if: (1) zero or more leading KEYWORD tokens are recognized modifiers — reused directly from `ModifierPriority.isModifier(...)` so the modifier set has one owner, not a duplicated list; (2) the remaining tokens up to the name are non-empty and, if the first one is a KEYWORD, it must be in a small per-language positive list of type-introducing keywords (`void/char/int/.../struct/enum/union` for C, `+bool/auto/class/wchar_t/...` for C++, Java primitives for Java) — this rejects `return x;`, `throw e;`, `assert ready;`, `break label;`, etc., which would otherwise match the same `KEYWORD IDENTIFIER ;` shape; (3) the token immediately before any trailing `[size]` groups / `= init` / `;` is exactly one IDENTIFIER (the name) — this rejects function prototypes/definitions (`void foo();`) and call statements (`doSomething(x);`), both of which end in `)`, not an identifier. A same-line trailing `//` or `/* */` comment is reattached to the statement that precedes it (it would otherwise become the next statement's leading token). Anything that doesn't match this shape is left untouched and breaks the current alignment group, same as a blank line |
| Column grid rendering | `ModifierPriority` gained a `columnCount()` helper (max rank + 1) so `DeclarationAlignmentRule.render(group)` knows how many fixed modifier columns the language model has. Per group, a column (each modifier rank, and the C/C++ post-pointer `const` column) is only emitted if at least one declaration in that group actually uses it — an all-unmodified group renders with zero leading padding rather than dead blank columns. The `[name][size]` and trailing `;` are concatenated into one tight cell (no AST exists to model them separately, and STYLE.md's worked example shows them touching, e.g. `buffer[64];`); the optional `= init` is appended before the `;` with plain `" = "` spacing — aligning the `=` column itself is STYLE.md §6 (Assignment alignment), out of scope here since no §5 worked example includes an initializer. The trailing comment is added as an extra cell only on rows that have one, which combined with `ColumnGrid`'s existing "pad only if not last in row" rule reproduces per-row raggedness for free. A generic `renderTokens`/`needsSpaceBetween` token-joiner (no AST) handles tight-attachment punctuation: no space before/after `*`, `&`, `::`, generics `< >`, and no space before `,`/`[`/`]` or after `[` — verified to reproduce STYLE.md §5's full C and Java worked examples byte-for-byte at the time this was written (before `reorderStatics` was wired in; see the next row) |
| Static reorder vs. STYLE.md §5's worked example | STYLE.md's worked C and Java examples place a non-static (`flags`) between two statics (`timeout` and `name`) with no dependency, which directly contradicts the same section's rule text ("static declarations come first in a group"). Asked the user, who chose to trust the rule text: the formatter reorders statics to the front of each group (preserving relative order within the statics block and within the leftover non-statics block), so running the formatter on input matching that exact worked example no longer reproduces it byte-for-byte — it moves `name` ahead of `flags`/`label`. The worked example is treated as an alignment-format illustration only, not a reorder demonstration. `reorderStatics(group)`: walks the group once, holding non-statics in a `pending` buffer; on each static, if any pending declaration's name is referenced in that static's `sizeTokens`/`initTokens`, flush all of `pending` (in order) before placing the static (keeps the whole accumulated dependency run immediately before it, per STYLE.md's "if reordering safety is unclear, preserve relative order"); otherwise the static is placed immediately and `pending` stays held for a possible later flush. Remaining `pending` is flushed at the end. `render()` now calls `reorderStatics` before building the grid |
| §10 Single-expression block eligibility | `BlockStructureRule.collapseSingleExpressionBlocks` only collapses a braced `if`/`while`/`for` body when it holds exactly one top-level `;`-terminated statement with no interleaved comment before it (a single same-line trailing comment after the `;` is allowed) and whose first token is not itself a compound-construct keyword (`if`/`while`/`for`/`switch`/`do`/`try`). The compound-keyword exclusion is a judgment call, not directly stated in STYLE.md §10: collapsing `if(x) { if(y) foo(); }` to `if(x) if(y) foo();` would introduce a dangling-construct ambiguity that none of §10's worked examples (`return`/`continue`/`break`) exercise, and §10's own title is "Single-**Expression** Blocks" -- an `if`-statement is not an expression. Already brace-less bodies, multi-statement bodies, and empty/comment-only bodies are left untouched (verbatim, byte-for-byte). Bracket/brace matching uses local depth counting on the token slice (same approach as `DeclarationAlignmentRule`'s `[`/`]` matching), not the tokenizer's running depth fields, so the method works on any bounded slice independent of its absolute nesting position |
| §11 K&R brace style detection | `BlockStructureRule.enforceKAndRBraceStyle` classifies a `{` as K&R-eligible (move onto the previous line, exactly one space before it) in three ways: (1) the tokenizer already tagged it with a construct name (`Token.name != null` -- class/struct/enum/enum class/namespace/interface/`extern "C"`); (2) its nearest preceding significant token is a bare K&R keyword (`else`/`do`/`try`/`finally`) or a `)` whose matching `(` is preceded by a paren-K&R keyword (`if`/`while`/`for`/`switch`/`catch`); (3) it is a lambda body (see next row). Anything else -- in particular a `)` whose matching `(` is preceded by an identifier, i.e. a named function/method definition -- is left completely untouched, since Allman placement for those is a different (not-yet-implemented) Tier-1 rule. A comment sitting in the gap between the preceding token and `{` also blocks the rewrite for that occurrence, since relocating it unambiguously is out of scope |
| §11 lambda bodies also use K&R | User-requested addition (not originally in STYLE.md/STYLE_C_CPP.md/STYLE_JAVA.md, added to both during this work): a lambda is a value embedded in a larger declaration/call, not a standalone definition, so its body brace follows K&R like other non-function blocks rather than Allman. Detection in `isLambdaBrace`: Java -- preceding significant token is the `->` operator (unambiguous; Java has no other use of bare `->` before `{`). C++ -- preceding token is `]` (capture-only, no params), or `)` whose matching `(` is immediately preceded by `]` (capture list before params), or either of those followed by a trailing `-> Type` (walked backward via `isCppTrailingReturnLambda`, bounded to `MAX_RETURN_TYPE_TOKENS` to avoid runaway scans on unrelated code). The `]`-before-params check is exactly what distinguishes a lambda from a same-shaped trailing-return-type **function** definition (`auto foo() -> int { ... }`, where the token before the param list's `(` is the identifier `foo`, not `]`) -- the latter correctly stays Allman and untouched. STYLE_C_CPP.md §2 and STYLE_JAVA.md §2 were updated with worked examples; per STYLE.md §3.1, `std::sort(...)`/`list.sort(...)` calls wrapping a lambda argument are themselves loose (nested call/paren), so their outer parens are padded in those examples |
| C/C++ bitfield column (`STYLE_C_CPP.md` §6) | Measured STYLE_C_CPP.md §6's worked example character-by-character: the `:` is aligned only against other bitfield names in the same group (`bitfieldNameWidth` = max name length among declarations that have a bitfield width), not against the full group's name+size column — e.g. `reserved`(8) sets the colon column even though `buffer[64]`/`label[MAX]` are longer. Non-bitfield rows in the same group are unaffected by `bitfieldNameWidth` and keep the existing tight `name[size];` cell. Implementation: `parseDeclaration` scans for a top-level `:` OP token before checking for `=`; if found, delegates to `parseBitfield`, which takes everything before the name as `typeTokens`, the IDENTIFIER immediately before `:` as `name`, and everything after `:` (rendered via the existing `renderTokens`) as the new `Declaration.bitfieldWidth` field (empty list when not a bitfield). `render()` computes `bitfieldNameWidth` once per group (max name length over declarations with a non-empty `bitfieldWidth`) and passes it into `renderNameCell`, which branches: bitfield rows render `name` padded to `bitfieldNameWidth` + `" : "` + width + `;`; non-bitfield rows are unchanged. No `ColumnGrid` changes were needed — the trailing comment column's alignment falls out for free from `ColumnGrid`'s existing per-column max-width-except-ragged-last-cell behavior once the name cell's content is fixed. Verified byte-for-byte against STYLE_C_CPP.md §6's full worked example (`buffer`/`timeout`/`flags : 4`/`mode : 2`/`reserved : 2`/`label` all in one group) |

---

## Open Questions

- [ ] Rule engine grouping — confirm `MiscRule` does not grow too large; split into
      `WhitespaceRule` + `BraceStyleRule` if needed during implementation
- [ ] `reformat_chunks.py` — keep as-is (AI-based, for long files) alongside the
      new JAR, or deprecate once JAR handles long files natively?

---

## File Status

| File | Status |
|---|---|
| `Main.java` | NOT STARTED |
| `Config.java` | NOT STARTED |
| `ServerMode.java` | NOT STARTED |
| `TokenizerCore.java` | COMPLETE |
| `ColumnGrid.java` | COMPLETE |
| `ModifierPriority.java` | COMPLETE |
| `CppModifierPriority.java` | COMPLETE |
| `JavaModifierPriority.java` | COMPLETE |
| `ComplexityPaddingEvaluator.java` | COMPLETE |
| `DeclarationAlignmentRule.java` | COMPLETE |
| `BlockStructureRule.java` | IN PROGRESS |
| `SwitchRule.java` | NOT STARTED |
| `GetterSetterRule.java` | NOT STARTED |
| `MiscRule.java` | NOT STARTED |
| `CppSpecificRule.java` | NOT STARTED |
| `JavaSpecificRule.java` | NOT STARTED |
| `README.md` | NOT STARTED |

---

## Current File: `BlockStructureRule.java` — NOT STARTED

> Replace this checklist when this file reaches COMPLETE.
> Implements STYLE.md §7, §10, §11, §12 — closing comments, single-expression block
> brace omission, K&R brace style for non-function blocks, and `else`/`else if`
> placement. Function-definition brace style (Allman) and §13 `switch` formatting are
> out of scope here (the latter is `SwitchRule.java`). Implement and
> checkpoint-commit one section below at a time.

### Single-expression blocks (STYLE.md §10)
- [x] Omit `{}` when the controlled body of `if`/`while`/`for` is a single statement
      (`if(x) return y;`, `if(x) continue;`, `if(x) break;`)

### Non-function block brace style (STYLE.md §11)
- [x] K&R style for `if`/`else`/`else if`/`for`/`while`/`do`/`switch`/`try`/`catch`/
      `finally` and class/interface/enum body braces: opening `{` stays on the same
      line as the keyword/declaration
- [x] Leave function-definition brace style (Allman) untouched here — that's handled
      elsewhere (Tier 1, language-specific files)
- [x] Lambda bodies (Java `(params) -> {`, C++ `[capture](params) {` and the
      `-> Type {` trailing-return-type form) also use K&R, same as other non-function
      blocks — added per user request; see STYLE_C_CPP.md §2 / STYLE_JAVA.md §2 and
      the Resolved Design Decisions row below

### `else` / `else if` placement (STYLE.md §12)
- [ ] `else`/`else if` on its own line directly after the preceding block's `}`
- [ ] Preserve (don't mechanically add/remove) an optional blank line between `}` and
      `else`/`else if` when the preceding branch exits unconditionally
      (`return`/`break`/`continue`) — context-driven, not automatic

### Closing comments on blocks (STYLE.md §7)
- [ ] Brace counter + name stack: push a name on `class`/`struct`/`enum`/
      `enum class`/`namespace`/`interface { ... }` and on named control-flow blocks
      (`for`, `while`, `if`, `switch`); pop on the matching `}`
- [ ] Add `// block-name` after `}` when block content exceeds the configurable
      `closing-comment-min-lines` threshold (default 5); named constructs always get
      a closing comment regardless of length
- [ ] Include the key variable in the comment when multiple control-flow blocks of
      the same kind are nested simultaneously (`// for i`, `// while running`)
- [ ] Named constructs always get a blank line after `{` and before `}` regardless of
      content length; control-flow blocks preserve existing blank lines as-is (do not
      add/remove) — blank lines count toward the closing-comment threshold
- [ ] Never add closing comments on `case` labels, naked compound `{ ... }` blocks, or
      `else`/`else if` (unless the branch is long *and* contains deeply nested `if`s)

---

## Config Keys and Defaults

Configurable values with their in-class defaults. All overridable via config file or CLI.

```properties
# ── Structural constants ──────────────────────────────────────────────────────
line-length                = 100
indent-size                = 4
indent-style               = spaces          # spaces | tabs
server-port                = 17173

# ── Behavior ──────────────────────────────────────────────────────────────────
closing-comment-min-lines  = 5
format-macros              = off             # off | on
line-endings               = lf             # lf | crlf | preserve

# ── C/C++ ─────────────────────────────────────────────────────────────────────
include-sort               = off             # off | on
header-guard-rename        = off             # off | on
header-guard-style         = preserve        # preserve | ifndef | pragma-once

# ── Java ──────────────────────────────────────────────────────────────────────
java-import-order          = static, java, org, com, local
java-import-sort           = on
java-import-depth          = 2
java-import-blank-lines    = 1
```

---

## Fixed Constants (non-configurable)

These must appear as `private static final` in their owning class, never as raw literals.

| Constant | Value | Owner class |
|---|---|---|
| `MIN_DIVIDER_SLASHES` | `60` | `CppSpecificRule` |
| `HEADER_ZONE_BLANK_LINES` | `2` | `CppSpecificRule` |
| `INCLUDE_GROUP_BLANK_LINES` | `1` | `CppSpecificRule` |
| `EXTERN_C_LABEL` | `"extern \"C\""` | `CppSpecificRule` |
| `APP_NAME` | `"style-fmt"` | `Config` |
| `LOCKFILE_NAME` | `"server.lock"` | `ServerMode` |
| `CONFIG_DIR` | `".config/style-fmt"` | `Config` |
| `CONFIG_FILE` | `"config"` | `Config` |
| `DEFAULT_PORT` | `17173` | `ServerMode` |
| `MANIFEST_FILE` | `"MANIFEST.MF"` | _(build only, Makefile)_ |

---

## Java File Header

Every `.java` source file must begin with this copyright block, before the `package` declaration:

```java
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */
```

## Java Coding Convention — `final` Locals and Parameters

Mark every local variable and method/constructor parameter `final` whenever it is
never reassigned after its initial assignment (i.e., whenever the compiler would
accept `final` there). This applies to all `.java` files under `src/` in this
project, including ones already marked COMPLETE — when editing an existing file
for any reason, bring touched declarations into compliance opportunistically;
a dedicated pass is not required unless asked. Loop counters and other variables
that are genuinely reassigned (e.g. a `for` loop's `i`, an accumulator) must NOT
be marked `final` — let `javac` be the check: if marking something `final` fails
to compile, it was actually being reassigned, so leave it without `final`.

## End Goal
- [ ] Dogfood test — run formatter on its own `src/` tree, verify style compliance and that `make` still succeeds after
