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
| §12 else/else-if placement | `BlockStructureRule.placeElseOnOwnLine` only acts when `else` is *directly* preceded (skipping a whitespace/newline-only gap) by a `}` -- if any other significant token sits between them, or the gap contains a comment, the occurrence is left untouched. When the gap contains no `NEWLINE` token at all (i.e. `}`/`else` share a line, in any spacing from `}else` to `} else`), it is replaced with one newline plus the `}`'s own line-leading indentation (computed by `indentBefore`, walking backward from the `}` to the nearest preceding `NEWLINE`, or "" if `}` isn't first on its line) so `else` lands directly under `}`. Any gap that already contains a `NEWLINE` -- including a deliberate blank line -- is passed through byte-for-byte unchanged, since STYLE.md §12 says that blank line is an optional, context-driven choice (e.g. the preceding branch exits unconditionally) that must never be mechanically added or removed; this method makes no attempt to detect that condition, by design. This method only repositions `else`; it does not touch brace spacing (`§11`/`enforceKAndRBraceStyle`'s job) -- chaining the two (re-tokenizing between passes) produces fully STYLE.md-compliant output, verified by hand for `if(x){...}else{...}` and Allman-style `if(x)\n{...}\nelse\n{...}` inputs |
| C/C++ bitfield column (`STYLE_C_CPP.md` §6) | Measured STYLE_C_CPP.md §6's worked example character-by-character: the `:` is aligned only against other bitfield names in the same group (`bitfieldNameWidth` = max name length among declarations that have a bitfield width), not against the full group's name+size column — e.g. `reserved`(8) sets the colon column even though `buffer[64]`/`label[MAX]` are longer. Non-bitfield rows in the same group are unaffected by `bitfieldNameWidth` and keep the existing tight `name[size];` cell. Implementation: `parseDeclaration` scans for a top-level `:` OP token before checking for `=`; if found, delegates to `parseBitfield`, which takes everything before the name as `typeTokens`, the IDENTIFIER immediately before `:` as `name`, and everything after `:` (rendered via the existing `renderTokens`) as the new `Declaration.bitfieldWidth` field (empty list when not a bitfield). `render()` computes `bitfieldNameWidth` once per group (max name length over declarations with a non-empty `bitfieldWidth`) and passes it into `renderNameCell`, which branches: bitfield rows render `name` padded to `bitfieldNameWidth` + `" : "` + width + `;`; non-bitfield rows are unchanged. No `ColumnGrid` changes were needed — the trailing comment column's alignment falls out for free from `ColumnGrid`'s existing per-column max-width-except-ragged-last-cell behavior once the name cell's content is fixed. Verified byte-for-byte against STYLE_C_CPP.md §6's full worked example (`buffer`/`timeout`/`flags : 4`/`mode : 2`/`reserved : 2`/`label` all in one group) |
| §7 closing comments — key variable on nesting | Asked the user since STYLE.md never defines the extraction algorithm and never shows an `if`/`switch` example. Resolved: the variable is only ever appended when a control-flow block is nested inside another block **of the same kind** (`for` in `for`, `while` in `while`, `switch` in `switch`) — never for a lone block, and never for `if` at all (always bare `// if`). `for`: "simply use the var name" — `extractForVariable` takes the first IDENTIFIER in the init clause, falling back to the first IDENTIFIER in the increment clause if init is empty, or (Java/C++ for-each `for(T x : xs)`) the IDENTIFIER immediately before a top-level `:`; null (bare `// for`) if none of those shapes match (`for(;;)`). `while`/`switch`: "use the var name for one bare identifier... if not just write `// while`" — `extractSingleIdentifier` only matches when the controlling expression's significant tokens are exactly one IDENTIFIER, or `!` + one IDENTIFIER; any more compound condition (`while(i < n)`, `switch(opcode & 0xFF)`) is treated as "not possible to simplify" and falls back to the bare label, per the user's explicit fallback instruction — no further "simplification" heuristic is implemented since none was concretely specified beyond that fallback |
| §7 closing comments — engine structure | `BlockStructureRule.addClosingComments` does a single forward pass with a `Deque<Frame>` stack (one frame pushed per `{`, popped per `}`, so nesting is tracked for free). `classifyBrace` assigns each `{` a `Kind`: `NAMED` (`Token.name != null`, plus Java anonymous classes — `new Identifier(args) {` / `new Identifier<T>(args) {`, detected the same bounded-lookback way as `isLambdaBrace` above, qualified names like `new pkg.Identifier()` not recognized — out of scope), `FOR`/`WHILE`/`IF`/`SWITCH` (`)` whose matching `(` is preceded by that keyword), `EXCLUDED` (bare `else`, or `else if` — detected by checking the token before the `if` keyword — STYLE_C_CPP.md/STYLE_JAVA.md both flag the "long branch with deeply nested ifs" exception as itself unresolved/preserve-as-is, so the simple always-bare reading is the only actionable one), or `OTHER` (everything else: function bodies, `do`/`try`/`catch`/`finally`, naked `{ }` blocks, `case` blocks, unnamed `namespace { }` since `Token.name` is null there too — none of these ever get a comment). Named-construct labels (`class Foo`, `enum class State`, `extern "C"`) are reconstructed by walking backward from the `{` over the identifier and its keyword(s), since `Token.name` only carries the bare identifier (`extern "C"` is the one exception — it is already a complete, space-containing label, detected via `name.indexOf(' ') >= 0`). A C/C++ trailing `;` right after `}` (struct/class/enum/union definitions) is walked over so the comment lands after it, not before — landing before it would comment out the `;` and break the statement. As a general safety net, `safeToCommentAfter` skips adding a comment entirely (rather than guessing a safe insertion point) whenever anything other than whitespace precedes the next newline after the chosen insertion point — this is what correctly defers commenting an `if` block whose `}` shares a line with `} else {` until `placeElseOnOwnLine` has run, and what prevents corrupting a chained anonymous-class expression (`new Runnable() { ... }.start();`) or re-duplicating an already-present trailing comment on re-runs |
| §7 closing comments — named-construct blank lines | `BlockStructureRule.insertNamedConstructBlankLines` is a separate pass (chained via re-tokenizing, same precedent as §11/§12 above) that forces exactly one blank line after `{` and before `}` for every brace tagged `Token.name != null`, regardless of body length — using the same gap-buffering technique as `enforceKAndRBraceStyle`/`placeElseOnOwnLine`. A gap with 2+ existing newlines is untouched; exactly 1 gets a second one inserted right after it; 0 (a same-line `{}`) gets `"\n\n"` prepended. A comment in the gap blocks the insertion, consistent with the rest of this file. Anonymous Java classes are intentionally excluded from forced blank lines (only from the "always commented" rule) — `Token.name` is null for them, and STYLE.md's blank-line rule is textually scoped to "named constructs", not anonymous ones. This must run, and be re-tokenized, before `addClosingComments`, since forced blank lines change an enclosing control-flow block's own line count for its threshold check |
| §13 non-inline case brace wrapping | Asked the user since STYLE.md's only non-inline worked example wraps every multi-statement case body in its own `{ }`, and the two-level/one-level indentation rule textually presupposes that shape. Resolved: `SwitchRule` never adds or removes a case's `{ }` wrapper — it only normalizes the two-level (body) / one-level (`}` + trailing `break;`) indentation when a case body is *already* wrapped in `{ }`. A bare (brace-less) multi-statement case body is left completely untouched, same conservative posture as the rest of the formatter (never restructure, only normalize formatting within whatever shape is already present) |
| §13 nested switch processing order | A nested `switch` inside an outer `case`'s body cannot be fixed by computing one shared `overrides` map across all switches found in a single token-list pass: each switch's body/tail delta-shift is computed from the *original* unshifted token text, so an outer switch's body-shift and an inner switch's own case-shift independently overwrite the same `overrides` map entries for lines inside the inner switch, instead of composing. `SwitchRule.formatNonInlineSwitches` instead loops: each iteration finds all switches, picks the smallest-span (innermost) one that still `needsWork` (`pickInnermostNeedingWork`/`needsWork` — span size is a reliable depth proxy since a nested switch's `[openBrace,closeBrace)` is always strictly contained in its enclosing switch's), fixes only that one switch, renders, and **re-tokenizes** before the next iteration (same re-tokenize-between-passes precedent as §11/§12). Fixing innermost-first means an enclosing switch's later uniform body-shift carries the already-correct nested switch along with it without disturbing its internal correctness. Verified idempotent and correct (byte-exact expected indentation) for a 2-level nested switch via smoke test |
| §13 inline switch row classification | STYLE.md's worked example mixes a call-shaped row (`doA()`) and a non-call assignment row (`x = funcMath(z) + 10`) in the *same* aligned group, so "structurally similar" is read per-row (is this one case's body a recognizable single statement?), not as a whole-group shape requirement. `SwitchRule.classify` recognizes a case body as alignable only if it is empty (fallthrough), exactly `break;`, or exactly one top-level-`;`-terminated statement optionally followed by `break;` and nothing else; if ANY case in the switch fails this (multiple statements, a brace-wrapped body, a trailing comment), the entire switch is left byte-for-byte untouched — same conservative all-or-nothing posture as the rest of this rule, since STYLE.md gives no worked example for those shapes. A call-shaped statement (`name(args);`, single IDENTIFIER immediately followed by a matching `(...)` with nothing else before the `;`) gets bonus name/`(`/args/`)` sub-column alignment via a nested `ColumnGrid`; everything else contributes its literal statement text as one opaque cell, still aligned against the call-shaped rows' assembled width via the outer `ColumnGrid`. The label cell always carries one baked-in trailing space before grid padding, since `ColumnGrid` only pads cells shorter than the column's widest entry — without it, the single widest label would render with no gap before its `:`. A bare `break;` with no preceding statement (`hasContent=false`, `hasBreak=true`) must still flow through the content+terminator columns (not the label-only branch), otherwise the break is silently dropped — caught via smoke test and fixed |
| §13 fallthrough marking | `SwitchRule.markFallthrough` only marks a case whose body is completely empty (no real content) and which is not the switch's last case (nothing to fall through into); it inserts `/* FALL-THROUGH */` directly after the case's `:` token, so it works unchanged for both non-inline (no space before `:`) and inline (handled by a later `alignInlineSwitches` pass) switches. Idempotency and "don't clobber an unrelated comment" both reuse the single `findFallthroughMarker` helper (also used by `classify`), which returns -1 for "no comment", an index for "exactly our own marker" (safe to recognize/regenerate), or -2 for "some other comment is present" (bail, leave that case alone) — `markFallthrough` skips already-marked or unrelated-commented cases via the same -1/-2 contract. This also fixed a latent bug: any comment inside a case body would previously have been silently dropped by `classify`'s literal-slice reconstruction; now any non-marker comment forces the whole switch to be left untouched. Verified via smoke test: matches STYLE.md's exact marked+aligned worked example, idempotent on a second pass, and correctly does not mark an empty last case |

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
| `BlockStructureRule.java` | COMPLETE |
| `SwitchRule.java` | COMPLETE |
| `GetterSetterRule.java` | NOT STARTED |
| `MiscRule.java` | NOT STARTED |
| `CppSpecificRule.java` | NOT STARTED |
| `JavaSpecificRule.java` | NOT STARTED |
| `README.md` | NOT STARTED |

---

## Current File: `GetterSetterRule.java` — NOT STARTED

> Replace this checklist when this file reaches COMPLETE.
> Implements STYLE.md §14 (Getter/Setter/Checker Group Alignment) and its Java extension
> in STYLE_JAVA.md §5. `SwitchRule.java` is now COMPLETE (§13, all three sections).
> `GetterSetterRule.java` does not exist yet — create it from scratch, following the
> existing rule classes' shape (constructor takes `language`; public entry-point method(s)
> taking `List<Token>` and returning the rendered `String`; reuse `ColumnGrid` for column
> alignment, same as `DeclarationAlignmentRule`/`SwitchRule`'s inline-case alignment).
> Implement and checkpoint-commit one section below at a time.
>
> **Open design question to resolve with the user before/while implementing** (genuine
> ambiguity, not a guessable detail): STYLE.md §14 says short getter/setter/checker methods
> "may be written inline as an aligned group" but never defines what makes a contiguous run
> of one-liner methods count as one "logical group" — e.g. must they be textually adjacent
> with no blank line or other member between them? Is a minimum group size of 2 required?
> Does mixing a getter and a setter for different fields still count as one group, or only
> get/set/is-pairs for the *same* field? STYLE_JAVA.md §3's "one-liner methods follow the
> group rule ... when they appear as part of an aligned group" implies grouping is something
> the *author* has already chosen (by writing them inline/adjacently) rather than something
> this rule should detect/impose by itself — but that reading should be confirmed, not
> assumed, given this project's "ask, don't guess" protocol for ambiguous STYLE.md rules.

### Column alignment (STYLE.md §14, STYLE_JAVA.md §5)
- [ ] Resolve the open design question above (what counts as a "group") via `AskUserQuestion`
- [ ] For an already-identified group of one-line methods, align (left-to-right): access
      modifier (Java only) → return type → method name → `(` → parameters → `)` → `{` →
      body → `}` — each column padded to its group's widest entry (worked examples in both
      STYLE.md §14 and STYLE_JAVA.md §5 show empty parameter lists padded with spaces to
      match the widest signature, e.g. `getX   (     )` lining up with `setX   (int x)`)
- [ ] If one group member's body is "significantly longer than the rest", exclude that one
      method from the group (render it normally/standalone) rather than letting it distort
      the others' alignment — STYLE.md §14 does not define "significantly longer"
      numerically; likely needs the same ask-don't-guess treatment as the group-detection
      question above, unless a simple proportional/absolute-length heuristic is agreed first
- [ ] The closing `}` of every group member must align in the same column (this is really
      the last step of the single left-to-right alignment pass above, called out separately
      in STYLE.md §14's bullet list — confirm it falls out for free rather than needing a
      second pass)

### Standalone one-liners (STYLE_JAVA.md §3, cross-referenced from §5)
- [ ] A one-liner method that is NOT part of a group keeps normal Allman brace style
      (`{` on its own line) — i.e. this rule must only act on methods already identified as
      group members; it must never collapse an unrelated standalone one-liner onto one line
- [ ] Confirm interaction with `BlockStructureRule`'s already-COMPLETE K&R/Allman brace
      enforcement (§11) — that rule must not fight this one over a group member's brace
      placement; likely resolved by running `GetterSetterRule` and re-tokenizing before any
      brace-style pass touches these methods, same re-tokenize-between-passes precedent used
      throughout `BlockStructureRule`/`SwitchRule`

### Fallthrough (STYLE.md §13)
- [x] Mark explicitly (`/* FALL-THROUGH */`), same indentation level as the next case
- [x] Inline switches: `:` aligned same as other inline case labels
- [x] Non-inline switches: no space before `:`

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
line-endings               = lf              # lf | crlf | preserve

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
