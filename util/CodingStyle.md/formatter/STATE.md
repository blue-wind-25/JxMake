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

**Do NOT read `FORMATTER_DISCUSSION.md` or `README.md`** unless the user explicitly
asks. All decisions relevant to implementation are already recorded in the
**Resolved Design Decisions** table in this file. `FORMATTER_DISCUSSION.md` is design
history and future planning only — it is large and contains nothing the implementer
needs that is not already summarized here.

**ONLY** read the Java source file you are currently implementing or directly modifying. Do NOT read other source files unless a specific checklist item or ambiguity requires it.

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
| §14 getter/setter rendering | `GetterSetterRule.render` reproduces STYLE.md §14's/STYLE_JAVA.md §5's worked examples via one outer `ColumnGrid` pass with cells `[modifier-columns(Java only)..., returnType, name(params), "{", body, "}", trailingComment?]` joined with `" "` -- verified byte-for-byte that this single-space-join + per-column padRight reproduces the doc's worked spacing exactly, including the closing `}` column, which needs no separate pass since it's just another fixed cell. The `name(params)` cell is built from a *nested* `ColumnGrid` over `[name, params, ""]` rows (trailing `""` to dodge the ragged-last-cell skip when params is empty), the same precedent as `SwitchRule.applyInlineAlignment`'s call-shaped case rows. Return type, params, and body text are taken as literal verbatim slices of the original tokens (`cellText` -- raw concatenation, not `renderTokens`-style re-spacing), since these are pre-existing single-line source the rule must not re-space, only pad/align as whole cells. Modifier columns reuse `JavaModifierPriority`/`ModifierPriority.columnCount()` exactly as `DeclarationAlignmentRule` does (only active columns emitted); C/C++ has no modifier column per STATE.md's explicit "Java only," so any leading C/C++ keyword (e.g. `static`) is simply left as part of the literal `returnType` cell text, not parsed into a separate field. Outlier exclusion (`excludeOutliers`) compares `cellText` length of the body slice, removing the current widest while it exceeds `2x` the *current* next-widest among what remains, iterating since removing one outlier can reveal another; if the result drops below 2 members the whole list is discarded (empty result) signaling "not a group," consistent with `groupOneLiners` never emitting size-1 runs. Verified against STYLE.md §14's and STYLE_JAVA.md §5's worked 3-method examples byte-for-byte (Java with `public` modifier column, C++ with no modifier column), plus an outlier-exclusion case and a lone-one-liner (size-1 run, never grouped) case, via a throwaway smoke harness (not committed) |
| §14 getter/setter group detection | Asked the user since STYLE.md/STYLE_JAVA.md never define what makes a contiguous run of one-liner methods count as one "logical group", nor a numeric meaning for "significantly longer". Resolved, three answers: (1) Grouping signal — any maximal run of 2+ textually adjacent single-statement one-liner methods counts as a group, regardless of which field(s) they touch or whether they mix getter/setter/checker shapes; broken by a blank line, a comment, or any non-one-liner member in between. The author's choice to write them adjacently is treated as the grouping signal (no field-matching or marker-comment requirement). (2) Minimum size — 2; a run of length 1 is left standalone/Allman, never treated as a one-member "group". (3) Outlier exclusion — a member is excluded from a group's alignment if its body width is more than 2x the next-widest *remaining* member's body width, applied iteratively (exclude, recompute the remaining widths, re-check) so that removing one outlier can correctly reveal and exclude another; an excluded member renders standalone/Allman like a non-grouped one-liner, and a group that drops to 1 member after exclusion stops being a group |

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
| `GetterSetterRule.java` | COMPLETE |
| `MiscRule.java` | NOT STARTED |
| `CppSpecificRule.java` | NOT STARTED |
| `JavaSpecificRule.java` | NOT STARTED |
| `README.md` | NOT STARTED |

---

## Current File: `MiscRule.java` — NOT STARTED

> `GetterSetterRule.java` is COMPLETE. Asked the user how to scope `MiscRule.java` (it had no
> pre-seeded checklist, unlike every other rule file): resolved as **one file** covering every
> remaining generic STYLE.md section not owned by another rule class (the "all remaining
> generic sections" option, not a `WhitespaceRule`/`BraceStyleRule` split and not a smaller
> subset) -- §1, §2, §3.2, §3.3, §4, §6, §8, §9, §15. `MiscRule.java` does not exist yet --
> create it from scratch, following the existing rule classes' shape (constructor takes
> `language`; public entry-point method(s) taking `List<Token>` and returning the rendered
> `String`; reuse `ColumnGrid` for column alignment where applicable, same as
> `DeclarationAlignmentRule`). Implement and checkpoint-commit one section below at a time, in
> the order listed (later sections build on earlier ones: §8's param alignment reuses §6/§5-style
> column alignment; §9's blank-line insertion reuses the gap-buffering technique from
> `BlockStructureRule`).

### §3.2 Keyword spacing
- [ ] No space between a control-flow keyword (`if`/`while`/`for`/`switch` -- exactly the four
      keywords in STYLE.md §3.2's code block, no others) and its following `(`: collapse any
      gap (spaces only -- a comment or a `NEWLINE` in the gap blocks the rewrite, same
      conservative posture as `BlockStructureRule`'s brace-style passes) down to zero width.
      This rule only ever removes whitespace; it never decides whether the *contents* of the
      `(...)` are padded -- that is §3.1, already implemented in `ComplexityPaddingEvaluator`,
      and wiring it into an actual rewrite pass is a separate, not-yet-assigned piece of work
      (flag this gap rather than silently expanding scope here)

### §3.3 `{}` initializer / block spacing
- [ ] Empty `{}` -- always tight, no padding, regardless of context
- [ ] Single-level `{ ... }` with content -- pad with one space inside both braces
- [ ] Nested `{ { ... }, { ... } }` -- pad at every nesting level, not just the outermost
- [ ] Scope to brace-initializer lists only (array/struct initializers, `= { ... }` contexts) --
      must not touch a control-flow or function/class body `{ }` (those are §11/§7's domain,
      already implemented in `BlockStructureRule`); confirm how to distinguish the two
      structurally (no AST -- likely: an initializer brace's nearest preceding significant
      token is `=`, or another `{`/`,` while already inside a recognized initializer) before
      writing code, and if genuinely ambiguous for some shape, stop and ask rather than guess

### §4 Pre/Post Increment and Decrement
- [ ] Detect `i++`/`i--` (post) used as a bare expression statement (`i++;` at statement level,
      not embedded in a larger expression) and rewrite to `++i;`/`--i;`
- [ ] Leave post-increment/decrement untouched when it is NOT a bare statement -- i.e. whenever
      its value is actually used (`arr[i++]`, `return i--`, `x = i++`, as a function argument,
      etc.) -- since STYLE.md §4 explicitly carves out "post-increment semantics required by the
      surrounding expression" as the exception
- [ ] Confirm `for(...; ...; i++)` loop increment clauses count as "value not used" (the
      increment's result is discarded either way) and should also be rewritten to `++i` --
      STYLE.md's own examples are framed as general "prefer pre" with explicit exceptions only
      for the value-is-used cases, so a `for` increment clause is not an exception

### §1 Indentation
- [ ] 4 spaces per indent level -- for any reformatting this rule or others perform that need to
      *generate* new indentation (e.g. wrapped function signatures in §8), use 4 spaces, tab
      display size 4
- [ ] Existing indentation in untouched code must never be converted (tabs↔spaces) by this rule
      in isolation -- STYLE.md's "match the project / majority of files" detection is inherently
      a multi-file, whole-project decision, not something a single-file token-level rule can
      determine on its own. Decide and record here whether (a) this is out of scope for
      `MiscRule.java` entirely and belongs in `Main.java`/`Config.java`'s file-walking
      orchestration instead, or (b) `MiscRule.java` exposes a pure function that Main.java calls
      with an externally-supplied "dominant style" parameter -- do not silently guess once this
      section is reached; ask if unclear

### §2 Line Length
- [ ] Confirm scope: STYLE.md §2 states a 100-char soft limit and explicitly defers the only
      described mechanical fix (breaking) to §8 (Function Signatures). There is no other
      described mechanical rewrite for an over-length line in STYLE.md. Decide here whether
      `MiscRule.java` needs anything for §2 beyond what §8 already does (e.g. a `--check`-mode
      warning emission, out of scope for this rule class which only renders text) -- likely a
      no-op section beyond documenting the line-length constant for §8 to consume; do not invent
      additional line-breaking behavior beyond §8's explicit scope

### §6 Assignment and Compound Operator Alignment
- [ ] Detect alignment groups of assignment statements (`=`, `|=`, `&=`, `>>=`, etc. -- any
      compound-assignment operator) -- "semantically related" grouping per STYLE.md §6 has no
      mechanical definition given; likely resolve the same way §14's grouping signal was
      resolved (ask the user for a concrete operational definition before implementing, rather
      than guessing what "semantically related" means)
- [ ] Column-align the operator (`=`, `|=`, etc.) across one group via `ColumnGrid`, mirroring
      `DeclarationAlignmentRule`'s architecture
- [ ] A lone variable with no group neighbors aligns trivially with itself (i.e. is simply
      rendered as-is, no padding needed since there's nothing to align against)
- [ ] A blank line between groups resets alignment, same precedent as §5

### §8 Function Signatures
- [ ] Inline when the full signature fits within the 100-char soft limit (§2)
- [ ] Break to one-parameter-per-line when it does not fit, with parameters column-aligned
      following the same declaration alignment rules as §5 (reuse `DeclarationAlignmentRule`'s
      column-alignment approach/helpers where practical rather than re-deriving from scratch)
- [ ] Confirm exact placement of the closing `)` on a broken signature (STYLE.md's own example
      shows it on its own line, un-indented, matching the function's own indentation level --
      verify this against the worked example byte-for-byte)

### §9 Blank Line Before `return`
- [ ] Insert exactly one blank line before a `return` statement when: the enclosing function
      body is multi-line, AND the `return` sits at function scope (not inside a nested
      block/if/for/etc. -- i.e. its brace-depth-relative-to-the-function-body is 0)
- [ ] Do NOT add a blank line before `return` in a one-liner function body (already excluded
      from this rule's scope since one-liners are GetterSetterRule's domain and never reach
      this pass with their body intact in expanded multi-line form) or before a single-expression
      `if(x) return y;` (the `return` there is not at function scope in the relevant sense --
      it's the single statement of an `if` body, never standing alone on the function's own top
      level)
- [ ] Use the same gap-buffering / "blank line already present is left untouched, never removed"
      precedent as `BlockStructureRule.insertNamedConstructBlankLines` -- this rule only ever
      adds a missing blank line, never removes or normalizes an existing one beyond ensuring at
      least one

### §15 Comment Style
- [ ] A single-line (`//`) or inline trailing comment that "forms a sentence" must start with an
      uppercase letter -- capitalize the first letter of the comment's text if it is currently
      lowercase and alphabetic; leave non-alphabetic first characters (e.g. a comment starting
      with a symbol, number, or already-uppercase) untouched
- [ ] Must NOT end with a period -- strip a single trailing `.` if present (only one; do not
      touch `...`/ellipsis or any other trailing punctuation like `?`/`!`/`:`)
- [ ] Confirm scope: does this apply to ALL `//` comments uniformly, or only ones that
      structurally "form a sentence" (STYLE.md's own qualifier) -- e.g. should a comment that is
      just a single identifier/label, or a commented-out code line, be exempt? No worked
      counter-example exists in STYLE.md §15 to settle this -- if it matters once this section is
      reached, ask rather than guess. `COMMENT_BLOCK` (`/* ... */`) handling is unspecified by
      §15's own examples (which are all `//`) -- confirm whether block comments are in or out of
      scope before writing code for them

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
