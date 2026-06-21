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

**Do NOT read `STYLE_JAVA17.md`, `STYLE_CPP20.md`, or `STATE_NEXT.md`** under any
circumstances unless the End Goal dogfood-test milestone below is already checked off.
These are gated, post-dogfood phase-2 work — out of scope for every session until that
milestone is complete, regardless of what checklist item is currently in progress.

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
      IndentationDetector.java  ← whole-project dominant-indent-style walker (for `indent-style = keep`)
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
| §3.2 keyword spacing | `MiscRule.enforceKeywordSpacing` does a single forward gap-buffering pass over the whole token list (same technique as `BlockStructureRule`'s brace-style passes): whitespace/newline/comment tokens accumulate in a `gap` buffer; on reaching a `(` PUNCT token, the gap is collapsed to zero width only if the immediately preceding significant token is a KEYWORD in the fixed `TIGHT_PAREN_KEYWORDS` set (`if`/`while`/`for`/`switch`, exactly STYLE.md §3.2's four) AND the gap contains no comment/newline token; otherwise the gap is emitted verbatim. `(...)`-interior padding (§3.1) is explicitly out of scope for this method, per the checklist note. Verified via a throwaway smoke harness (not committed): collapses `if (x)`/`while   (x)`/`for (...)`/`switch (x)` to tight form, leaves `catch (...)` (not one of the four keywords) and an identifier named `iffy` untouched, and leaves both a comment-in-gap (`if /* c */ (x)`) and a newline-in-gap (`if\n(x)`) case completely unrewritten |
| §3.3 initializer brace spacing | `MiscRule.enforceInitializerBraceSpacing` tracks a `Deque<Boolean>` (`initStack`) of one entry per currently-open `{`, classifying each as initializer or not when pushed: `isInit` iff the immediately preceding significant token is `=`, or is `{`/`,` while the (still-open) enclosing frame on top of the stack is itself `true` -- exactly the structural signal the checklist proposed. Confirmed against `BlockStructureRule.qualifiesForKAndR` (reviewed, not modified) that an initializer brace's preceding token (`=`/`{`/`,`) never satisfies that method's K&R/lambda criteria, so the two rules never classify the same brace. Rendering is a single forward gap-buffering pass: the gap immediately after an initializer-open `{` and immediately before its matching initializer-close `}` are independently collapsed to exactly one space, unless that side's own gap contains a comment/`NEWLINE` (blocks only that side, not the whole pair) or unless the open and close are directly adjacent (`lastSignificant=="{"` and current token is `}`), which collapses to zero width (empty-braces case) regardless of either side's normal rule. Verified via a throwaway smoke harness (not committed): STYLE.md §3.3's single-level, nested, and empty worked examples byte-for-byte; `{  }` (whitespace-only) also collapses to `{}`; control-flow (`if(x) {...}`) and class-body (`class Foo {...}`) braces are left completely untouched; a comment just inside the open brace blocks only that side while the close side still gets padded independently; a multi-line initializer (`NEWLINE` in the gap) is left untouched on the blocked side |
| §4 pre-increment rewrite | `MiscRule.enforcePreIncrement` collects a `Map<Integer,Integer>` of `identIdx -> opIdx` spans to swap, from two independent finders, then renders by walking the token list once and substituting `op.text + ident.text` (tight, no gap) wherever a span starts, jumping straight to `opIdx + 1` (discarding the original gap between identifier and operator -- regenerated tight, consistent with `DeclarationAlignmentRule`'s "restructured content is regenerated, not preserved verbatim" precedent, since this rewrite reorders tokens rather than just padding an existing shape). Finder 1 (`collectBareStatementSpans`): a global paren/bracket-depth counter (incremented/decremented on any `(`/`[`/`)`/`]`, regardless of nesting construct) excludes any candidate inside an unclosed call/index/for-header; at depth 0, an IDENTIFIER immediately preceded by a statement boundary (`;`, `{`, `}`, or start-of-scope) and immediately followed by `++`/`--` then `;` qualifies. A brace-less single-statement control-flow body (`if(x) i++;`, preceded by `)` rather than a boundary token) is a documented, deliberate gap -- recognizing arbitrary statement-start positions without an AST was judged out of scope of STYLE.md's own "i++; at statement level" framing, not guessed at. Finder 2 (`collectForIncrementSpans`): locates each `for` keyword's matching parens (`matchParenForward`, local depth counting) and the two top-level `;` inside (local depth-0 scan, same precedent as `BlockStructureRule.extractForVariable`'s clause-splitting); the third clause is added as a span only when it is exactly one IDENTIFIER followed by `++`/`--` and nothing else (`afterOp == closeParen`) -- a comma-separated multi-increment clause or any other shape is left untouched. Both finders require zero comment/`NEWLINE` tokens between the identifier and operator (`noBlockerBetween`) before adding a span. Verified via a throwaway smoke harness (not committed): bare `i++;`/`i--;` at statement level (including directly after `{`), `for(...; ...; i++)` rewritten to `++i`, all value-used exclusions (`arr[i++]`, `return i--`, `x = i++`, `foo(i++)`), a multi-increment for-clause left untouched, the documented brace-less-if-body gap left untouched, and a comment between identifier and operator blocking the rewrite |
| §1 indentation scope | Asked the user since STYLE.md's "match the project / majority of files" rule is inherently a multi-file decision a single-file token rule cannot make alone. Resolved: three `indent-style` modes, not two. `spaces`/`tabs` are simple, single-file mechanical conversions implemented directly in `MiscRule.java`. `keep` requires a new dedicated file-walking/detection class (name TBD, not yet created -- explicitly NOT `Main.java`/`Config.java` directly, "so not to clutter Main/Config" per the user) that scans the whole project once, determines the dominant existing style, and calls into `MiscRule.java`'s plain converter with that already-resolved choice -- `MiscRule.java` itself never has to interpret "keep". `Config Keys and Defaults`' `indent-style` updated to `spaces \| tabs \| keep`. The new detector class is added to `Project Layout`/`File Status` as `NOT STARTED`; its exact design (e.g. caching, what counts as "majority") is deferred until `Main.java`/`Config.java` orchestration work begins, not blocking `MiscRule.java`'s own `spaces`/`tabs` conversion work now |
| §6 grouping and rendering | Asked the user for an operational definition of "semantically related" grouping. Resolved: same textually-adjacent-run grouping signal as §14 (any maximal run of assignment statements -- any compound-assignment operator counts as one, regardless of which variable(s) are involved -- broken by a blank line, a comment, or a non-assignment statement). Separately, hand-verified against STYLE.md §6's worked example (`flags = 0x01;` / `flags \|= 0x02;` / `flags &= ~0x04;` / `flags >>= 2;` / `timeout = 100;`, all one group despite two different variable names, confirming the textually-adjacent-run signal is right even across variables) that the rendering is NOT a single `ColumnGrid` left-pad column on the concatenated "name+operator" text -- character-offset analysis shows the `=` of every row lands on the same column only when name and operator-prefix are padded as two *separately* fixed-width fields: `padRight(name, maxNameLen) + padLeft(operatorPrefix, maxPrefixLen) + "=" + " " + value + ";"`, where `operatorPrefix` is the operator's text minus its trailing `=` (empty for plain `=`). `ColumnGrid`'s existing contract is left-justify-only, so this rule renders manually rather than extending `ColumnGrid` with a right-justify mode for one rule's one column. A lone ungrouped variable needs no special case -- with group size 1, both maxes simply equal that row's own widths |
| §8 signature scope and rendering | `MiscRule.parseSignature` takes `sigTokens` already isolated by the caller -- from the signature's first lead token (first modifier, or the return type if none) through and including the parameter list's closing `)`, and nothing past it (no `throws`, no `{`/`;`) -- same granularity precedent as `DeclarationAlignmentRule.parseDeclaration`'s pre-split `stmt` contract; this rule's job is inline-vs-broken rendering and param alignment, not discovering where a signature starts in arbitrary source. The name is the IDENTIFIER immediately before the first depth-0 `(` (depth tracked over Java generics via the tokenizer's `ANGLE_BRACKET_OPEN`/`_CLOSE` token types, so `Map<String, Integer> get(...)` doesn't misidentify `Integer` as the name); everything before it (modifiers + return type) is kept as one opaque `leadTokens` list -- no per-modifier columns the way §5's grid has, since §8 has no alignment group across multiple signatures to align modifier columns within. Each parameter is `[typeTokens] name [sizeTokens]` (the C array-param suffix), parsed via the same depth-tracked trailing-`[size]`-peel precedent as `DeclarationAlignmentRule`'s own `sizeTokens` loop. Any parameter containing a top-level `=` (a C++ default value) bails the *entire* signature, left completely untouched -- no STYLE.md worked example covers default values, varargs, or a `throws` clause, so none of those are guessed at. Rendering's broken-form param-type column width is `maxTypeLen + 1` (unconditional, not a floor) followed by the standard single-space join -- hand-derived the same way as §6's `maxPrefixLen + 1`, by measuring STYLE.md §8's own worked example character-by-character and finding every row's gap is one space wider than a plain `ColumnGrid`-style `maxTypeLen`-width column would produce. Discovered while verifying: STYLE.md §8's own broken-form worked example (`reallyLongFunctionNameHere`, 27-char name) is only 82 characters as a single line -- it never actually exceeds the 100-char limit it's illustrating, so it's read as illustrative shape-only, not a literal threshold trigger, same precedent already established for §5's statics-reordering worked example |
| §9 function-body detection and return scoping | `MiscRule.insertBlankLineBeforeReturn` detects a "function body" `{` with the same minimal structural signal already noted (for a different purpose) in "§11 K&R brace style detection" above: a `{` whose immediately preceding significant token is a `)` whose matching `(` is itself preceded by an IDENTIFIER. This alone excludes every control-flow brace (`if`/`while`/`for`/`switch`/`catch` are preceded there by a KEYWORD, never an IDENTIFIER) and every lambda (preceded by `->`, never `)`) with no per-keyword exclusion list needed. One extra guard beyond that precedent: if the identifier is itself preceded by `new`, this is a constructor call / anonymous-class instantiation, not a method definition, and is excluded so an anonymous class body is never misclassified as the function body of whatever constructor created it. A `Deque<FuncFrame>` stack (one frame per open `{`, `isFunctionBody`/`multiLine`/`sawContent` flags) tracks, for whichever frame is on top when a `return` is reached, whether it sits directly in a multi-line function body with at least one statement already before it (`sawContent`) -- "multi-line" is decided once per function body at the brace that opens it, by scanning forward for any `NEWLINE` token before the matching `}` (`spansMultipleLines`). STYLE.md §9's only documented exclusion is the brace-less `if(x) return y;` shape (§10); generalized here to brace-less `while`/`for`/`switch` controlled bodies too (detected the same way: `return` immediately preceded by `)` whose matching `(` is preceded by one of `TIGHT_PAREN_KEYWORDS`), since the underlying reasoning STYLE.md states for the exclusion -- "not inside a nested block" -- applies identically to those even without literal braces. A `return` whose immediately preceding gap contains a comment, or contains zero newlines (shares a source line with the previous statement), is left untouched -- neither shape has a worked example to justify guessing where the blank line, or (for the zero-newline case) a brand-new line break that doesn't yet exist, should go. Known, documented gap: a C++ method with a trailing qualifier between `)` and `{` (`void foo() const { ... }`) is not recognized as a function body (the brace is misclassified as not-a-function-body) -- this rule then simply does nothing there, never anything actively wrong, and no STYLE.md worked example forces resolving it now |
| §15 comment scope and sentence detection | Asked the user two scope questions, then re-checked against STYLE.md §15's actual current text (which is richer than the checklist's original framing) and corrected accordingly. Resolved (1): the capitalize/no-trailing-period rule applies to every `//` comment this pass sees -- but STYLE.md's pre-existing "labels, closing comments, and markers are not sentences" exemption (`// for i`, `// class Foo`, `/* FALL-THROUGH */`) is kept exactly as written there, achieved via **pipeline ordering**, not an in-rule heuristic: this §15 pass must run before `BlockStructureRule`'s §7 closing-comment-insertion pass and `SwitchRule`'s §13 fallthrough-marking pass in `Main.java`'s eventual orchestration, so label/marker comments simply don't exist in the token stream yet when this pass runs -- nothing to detect or exempt. Recorded as a hard constraint for whoever wires up `Main.java`'s pass order. Resolved (2), refined by the user beyond the original yes/no framing: period-stripping specifically (not capitalization, which always applies) depends on sentence count -- a comment is single-sentence (period stripped) iff the only `.` in its text is its own last non-whitespace character; if a `.` appears anywhere earlier followed by more text, the comment has 2+ sentences and its trailing period is left untouched. (`// Increment index.` → strip; `// Increment index. This index is incremented for XXX.` → leave both periods). `...`/ellipsis is still never touched, per the original checklist note; this whole branch is defensive handling of already-existing non-compliant `//` comments, since STYLE.md's own prescriptive guidance is to write multi-sentence content as a `/* */` block in the first place. Resolved (3): `COMMENT_BLOCK` (`/* ... */`) is in scope for the same capitalize/sentence-aware-period rule, plus a structural precondition the user added: a block comment that already spans multiple lines must have its `/*` and `*/` forced onto their own lines (moved if not already there), matching the shape of STYLE.md's own multi-sentence worked example. Asked a follow-up on how far that structural rule reaches: resolved that only already-multi-line block comments get this banner treatment -- a short single-line `/* note */` (including inline/trailing ones) is left alone, never force-exploded into a 3-line banner, since that would be a large structural change with no STYLE.md worked example to justify it. Resolved (4), newly discovered while re-reading STYLE.md §15 (this rule was entirely missing from the original checklist): STYLE.md also documents a **separator alignment** rule -- when inline trailing `//` comments across an aligned group share a separator character (`—`, `:`, etc.), that separator is aligned across the group by padding the label text before it, per STYLE.md's `int[] x`/`xy`/`z` worked example. Added as a new checklist item; its exact grouping-detection mechanics (reuse §5/§6 groups vs. compute independently) are deferred until that item is implemented. Evaluated whether any of this needed to be written into `STYLE.md` itself (per the user's "add them to STYLE.md too if you think it is necessary, otherwise commit" instruction) and concluded no: the label exemption and separator-alignment rule are already fully documented in STYLE.md (only this checklist was stale/incomplete relative to it), and the pipeline-ordering mechanism plus the §6 rendering algorithm are pure implementation detail, consistent with the §14 precedent of keeping such algorithms out of STYLE.md |
| §15 partial-implementation split | User directed: implement §15 fully if simple enough, otherwise implement the simple parts and defer the rest. Split along where per-line ` * `-continuation-marker parsing becomes necessary: implemented now (`MiscRule.enforceCommentStyle`) are (a) every `//` comment, and (b) a `COMMENT_BLOCK` that is **already single-line** (no `\n`/`\r` in its token text) -- both get the resolved capitalize-first-letter + strip-sole-trailing-period rule, operating on the text between the comment delimiters. One reading judgment call beyond the original "§15 comment scope" decision's literal wording: that decision's resolution (3) said a short single-line `/* note */` is "left alone, never force-exploded into a banner" -- re-read as exempting it only from the *structural* banner-forcing, not from the capitalize/period *text* rule, since the decision's lead sentence frames `COMMENT_BLOCK` as in-scope for "the same capitalize/sentence-aware-period rule" before introducing the banner precondition as a separate, additional clause ("PLUS a structural precondition"). Deferred: a `COMMENT_BLOCK` that already spans multiple lines is left completely untouched, for both halves of resolution (3) -- the capitalize/period text rule (needs marker-aware parsing to find the first content letter past a leading ` * ` and the last content character before a trailing ` * `/`*/` line without disturbing the markers) and the `/*`/`*/` own-line forcing (STYLE.md only shows the already-correct target shape, never a "before" example, so the exact relocation mechanics for malformed input are a design choice still to be made). Also deferred: §15's separator-alignment item, which already carries its own unresolved grouping-detection sub-question recorded in resolution (4) above |
| §15 multi-line block comment banner reformatting | Asked the user since the prior session's deferred item explicitly flagged the relocation mechanics as an open design choice, not a guess to take: a multi-line `COMMENT_BLOCK` might already use the conventional ` * `-per-line continuation-marker banner (just needing `/*`/`*/` relocated onto their own lines), or might be raw wrapped prose or commented-out code with no markers at all. The user chose the conservative scope: **only** normalize a multi-line block comment when every continuation line (line 2 onward), after stripping its leading whitespace, already starts with `*` -- i.e. it already follows the marker convention. Anything else is left completely untouched, consistent with the formatter's existing "only normalize within an already-recognizable shape, never restructure" posture used throughout (§11 brace style, §13 non-inline switch case bodies, etc.) -- this is what correctly skips commented-out code blocks and ASCII art, which have no STYLE.md worked example sanctioning a rewrite. `MiscRule.reformatMultiLineBlockComment` implements this: each continuation line's content is extracted via `stripLeadingWhitespace` + `afterLeadingStarMarker` (drops the leading `*` and at most one following space); the closing line additionally has its trailing `*/` peeled first (recognizing both a bare `*/`-only closer and a `* content */` closer in one line). The first and last physical lines (which carry `/*`/`*/` themselves) only contribute a content line if what remains after trimming is non-empty; a genuinely blank *middle* line (bare `*`, nothing else) is preserved as an intentional blank paragraph separator rather than collapsed. Text rules then generalize the existing single-line capitalize/period logic across the whole extracted content: the first content line's leading letter is always capitalized (`capitalizeFirstLetter`, reused unchanged); the trailing period on the very last content line is stripped only if it is the sole `.` across every content line (`stripSoleTrailingPeriodAcrossLines`, a multi-line generalization of `stripSoleTrailingPeriod`'s "only strip if the sole dot is also the trailing character" rule) -- this means a single sentence that merely got wrapped across physical lines inside a `/* */` (not because it has multiple sentences) still loses its trailing period, same as a single-sentence `//` comment would; a genuinely multi-sentence paragraph (2+ dots) keeps every period untouched, per STYLE.md's "end each sentence with a period" directive for the paragraph form. Output is re-indented from scratch using `indentBefore` (token-index variant of `BlockStructureRule.indentBefore`'s same line-leading-whitespace lookup) -- each regenerated line is `indent + " *" [+ " " + content]`, landing every line's `*` directly under the opening `/*`'s own `*` column, and the comment's *own* `/*`-line indentation is left untouched for free since it's a separate, pre-existing `WHITESPACE` token this rule never touches. Verified via a throwaway smoke harness (not committed): STYLE.md's own §15 worked example byte-for-byte (after fixing a leading-whitespace bug caught by the harness -- the first line's content needs full `.trim()`, not only a trailing trim, since the original code only trimmed trailing whitespace and left a stray leading space before the relocated first content line); blank-middle-line preservation; raw-wrapped-prose and commented-out-code rejection; ellipsis and mid-sentence-abbreviation non-stripping; tight (no-space) and bare (`*/`-only) closer variants; non-line-leading comments (mid-statement) correctly getting no indent; CRLF-sourced input; and round-trip idempotency |
| §15 separator alignment | Asked the user two design questions before writing any code, since STATE.md flagged this item's grouping mechanics as unresolved. (1) Grouping signal: **compute independently from comment text alone** -- `MiscRule.alignCommentSeparators` scans the flat token stream for trailing `//` comments on physically-adjacent lines, regardless of what statement (if any) precedes them, rather than depending on another rule's `Declaration`/`Assignment` group objects. This matches every other pass in the file (each operates on retokenized text, never another rule's internal data) and is forced by the pipeline's "chained via re-tokenizing" architecture -- by the time this pass runs, an earlier rule's grouping has already collapsed into plain text. (2) Separator-character recognition, refined by the user beyond the original two-option framing: a candidate separator is any single character that is not alphanumeric **with Unicode letters/digits counting as alphanumeric** (`!Character.isLetterOrDigit(c)`, so accented letters like `ü` can never be mistaken for a separator), flanked by a literal space on both sides. A comment qualifies only if exactly one such candidate exists in its text (zero or 2+ candidates -- e.g. `// foo - bar - baz` -- means "not recognized", not "pick the first/last"); the candidate splits the comment into a trimmed `label` and trimmed `rest`, both of which must be non-empty. `parseSeparatorComment` implements this once and is reused by both the detection step and the rendering step. `findTrailingSeparatorComment` defines a "line" as the token span between two `NEWLINE`s (or list start/end) and only matches when that line's last significant token (skipping `WHITESPACE`) is a qualifying `COMMENT_LINE`. `alignCommentSeparators` then walks the per-line results once, forming maximal runs of consecutive qualifying lines sharing the same separator character -- a blank line, a non-qualifying comment, or a differing separator character all simply fail to extend the run, the same "doesn't match, breaks the group" posture used throughout this file -- and requires a run length of 2+ before rewriting anything (a lone qualifying line, e.g. `// single-level — pad` with no neighbors, is left byte-for-byte untouched, same minimum-group-size precedent as §14's getter/setter grouping). Each rewritten line becomes `"// " + padRight(label, maxLabelLen) + " " + sep + " " + rest`. **New conflict discovered and resolved before coding**: STYLE.md's worked example keeps labels lowercase (`single-level`, `nested`, `empty`), but `enforceCommentStyle`'s capitalize-first-letter rule was applying unconditionally to every `//` comment, which would have corrupted them (`Single-level`). Asked the user, who chose to treat any comment matching the separator-alignment shape as a label/fragment -- the same category as STYLE.md's pre-existing "labels... are not sentences" exemption -- so `enforceCommentStyle` now calls `parseSeparatorComment` first and passes such comments through completely unchanged (no capitalize, no period-stripping), leaving them for `alignCommentSeparators` (a later pass, after re-tokenizing) to pad. Verified via a throwaway smoke harness (not committed): STYLE.md's exact worked example byte-for-byte, a lone qualifying line left untouched, differing-separator/non-comment-line/blank-line run breaks, the 2-candidate ambiguous-comment rejection, and the full `enforceCommentStyle` → re-tokenize → `alignCommentSeparators` pipeline producing the correct final padded output without capitalization corruption |
| §6 multi-line right-hand sides | `MiscRule.parseAssignment` now accepts a value spanning exactly two physical lines: after locating `valueFrom`/`valueTo` as before, a single forward scan over that range counts `NEWLINE` tokens and rejects outright (returns null, untouched) on any comment found or on 2+ `NEWLINE`s -- no STYLE.md worked example covers either. With exactly one `NEWLINE`, the surrounding whitespace is trimmed outward (mirroring the existing trailing-gap trim before `;`) to find `line1`'s last real token and `line2`'s first, and `classifyMultiLineBreak` decides which of STYLE.md's two documented shapes applies: `line2.get(0)` is an operator → "breaking before an operator" (`breakBeforeOperator=true`); else `line1`'s last token is an operator → "breaking after" (`false`); else neither (a break unrelated to any operator, e.g. mid-operand) → reject, same conservative posture as everywhere else in this rule. `Assignment` gained a `multiLine` flag plus `firstLineValueTokens`/`secondLineValueTokens` (via new `Assignment.singleLine(...)`/`Assignment.multiLine(...)` static factories replacing the old public constructor) since a multi-line row's value can no longer be one flat `valueTokens` list. Rendering (`render`/`renderMultiLine`): a multi-line row is excluded from the group's `ColumnGrid` pass entirely -- its `value+";"` cell would only ever hold the first physical line, so any comment-column padding computed from it would be wrong -- and is rendered separately as exactly two lines, then spliced back into the group's output at its original position. Line 1 is `lhs + " " + firstLineValueTokens`, identical in shape to the single-line case. Line 2 is pure indentation (no copied original whitespace) followed by `secondLineValueTokens` + `;` (+ trailing comment if present, appended directly, not column-aligned -- no worked example combines this with `ColumnGrid` comment alignment): indent length `lhsWidth - 1` for the before-operator case (lands the operator on the `=` column, since `lhs` is exactly `lhsWidth` characters and ends in `=`) or `lhsWidth + 1` for the after-operator case (lands the next operand one column past `=`, i.e. where the first operand began on line 1). Both targets are computed from the whole group's `lhsWidth` (shared `maxNameLen`/`maxPrefixLen`), not the row's own unpadded width, so a multi-line row still lines up correctly inside a group with longer-named neighbors. Verified via a throwaway smoke harness (not committed) against both of STYLE.md §6's worked examples byte-for-byte, plus a mixed group (one single-line + one multi-line-with-trailing-comment row, confirming `ColumnGrid`'s name/`=`-column padding and the multi-line row's independent indent both come out correct together) and the two/3+-newline and comment-inside-value rejection cases |

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
| `IndentationDetector.java` | NOT STARTED |
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
| `MiscRule.java` | COMPLETE (§1 `indent-style=keep` cross-file integration deferred to `IndentationDetector.java` -- see Resolved Design Decisions: "§1 indentation scope") |
| `CppSpecificRule.java` | NOT STARTED |
| `JavaSpecificRule.java` | NOT STARTED |
| `README.md` | NOT STARTED |

---

## Current File: `CppSpecificRule.java` — IN PROGRESS

> `MiscRule.java` is COMPLETE (its one remaining item, §1 `indent-style=keep`, is non-blocking --
> see the File Status table and Resolved Design Decisions: "§1 indentation scope"). Asked the
> user how to scope `CppSpecificRule.java` (it had no pre-seeded checklist, unlike the original
> per-rule files): of STYLE_C_CPP.md's 11 sections, §5 (pre/post increment) and §6 (bitfields) are
> already fully implemented elsewhere (`MiscRule`/`DeclarationAlignmentRule`), §7's additional
> C/C++ closing-comment cases are already handled by `BlockStructureRule`'s `NAMED`/`EXTERN_C`
> classification and unnamed-namespace exclusion, the lambda-K&R part of §2 is already done
> (`BlockStructureRule.isLambdaBrace`), and §8 is explicitly "preserve as-is" with nothing to
> implement. That leaves 7 sections of genuinely new work: §1, §2 (Allman conversion only --
> lambda K&R is already done), §3, §4, §9, §10, §11. Resolved: **one file, all 7, document order**
> -- including §10/§11 despite needing filename context, by having `CppSpecificRule`'s entry
> point(s) accept the filename as an extra parameter rather than spinning up a new file-walking
> class for them. `CppSpecificRule.java` does not exist yet -- create it from scratch, following
> the existing rule classes' shape (constructor takes `language`; public entry-point method(s)
> taking `List<Token>` and returning the rendered `String`, plus a filename parameter for
> §10/§11; reuse `ColumnGrid` where applicable). Implement and checkpoint-commit one section
> below at a time, in the order listed.

### §1 Empty Parameter Lists
- [ ] C always writes `void` explicitly (`void foo(void)`); C++ omits it (`void foo()`).
      Branches on the existing `language` field (`"c"` vs `"cpp"`, per
      `TokenizerCore`'s language switch) -- detect an empty `(...)` parameter list on a function
      *definition or declaration* (not a call) and insert/remove the bare `void` token
      accordingly. Needs its own definition-vs-call detection, similar in spirit to
      `MiscRule.parseSignature`'s pre-isolated `sigTokens` contract but for a fresh scan since no
      caller pre-isolates signatures here

### §2 Function Brace Style (Allman conversion)
- [ ] Convert a C/C++ **function definition's** own brace to Allman (own line) when it is
      currently K&R/same-line -- the inverse of `BlockStructureRule.enforceKAndRBraceStyle`,
      which deliberately leaves a `)` preceded by an IDENTIFIER (a named function/method
      definition) untouched today, flagged there as "a different (not-yet-implemented) Tier-1
      rule." Lambda bodies must stay K&R (already correctly excluded by
      `BlockStructureRule.isLambdaBrace`'s detection, reused/not duplicated here) and must not be
      reclassified as Allman by this pass
- [ ] One-liner exception: a function whose entire body is a single statement (or short tightly
      related sequence) renders as `{` and `}` together on the second line (`{ _x = 0; }` /
      `{ _done = true; return y; }`) rather than full Allman expansion -- needs an operational
      definition of "short tightly related sequence" (STYLE.md gives only single- and
      two-statement examples); likely needs an `AskUserQuestion` when this item is reached

### §3 C++ Template Angle Brackets `<>`
- [ ] Single-level template params stay tight (`vector<int>`); any `<>` that directly contains
      another `<>` at any depth gets padded on both sides of the outer `<>` (`vector< vector<int> >`).
      This is flagged as a **correctness rule** (avoids `>>` parse errors in older C++ standards),
      not just style -- so it cannot be skipped/left ambiguous the way some other sections are.
      Tokenizer already distinguishes `ANGLE_BRACKET_OPEN`/`_CLOSE` from plain `<`/`>` operators
      (used by `MiscRule.parseSignature`'s generic-depth tracking) -- reuse that depth tracking to
      find nesting

### §4 Pointer and Const Qualifier Style
- [ ] `*` attaches to the type (`char* p`, not `char *p`); `const` before `*` attaches to type
      (`const char* p`); `const` after `*` stays in place (`uint8_t* const p`,
      `uint8_t* const* pp`). STYLE.md §5's declaration-alignment rule text already says "For
      pointer and `const` placement in C/C++, see STYLE_C_CPP.md §4" -- meaning
      `DeclarationAlignmentRule`'s current rendering takes `*`/`const` spacing verbatim from
      input tokens today, and this section is what's supposed to normalize it. Likely needs
      `DeclarationAlignmentRule`'s `renderTokens`/`needsSpaceBetween` tight-attachment join to be
      reused or extended here rather than re-implemented, since the spacing primitives already
      exist there

### §9 Section Dividers
- [ ] Two strengths of full-width (100-char) `/`-divider lines: single (ordinary section
      boundary) and triple (stronger boundary, e.g. before a large `#endif` or an
      attribution/origin change). STYLE.md explicitly says "use sparingly... should feel
      significant, not routine" -- this reads as **human-authored**, not something the formatter
      mechanically inserts. Needs an `AskUserQuestion` when this item is reached to confirm scope:
      does the formatter do anything at all here (e.g. normalize an existing divider comment's
      length to exactly 100 `/` chars if it's already recognizable as one), or is this section
      entirely non-actionable/documentation-only for human authors

### §10 Header File Structure
- [ ] Fixed zone layout (copyright block / header guard / body / closing `#endif`) with exactly
      2 blank lines between zones. Guard name derived from filename (uppercase, `.`/path
      separators → `_`); warn (default) or rename (`header-guard-rename` config key, already
      defined in Config Keys and Defaults) if the existing guard doesn't match. `#pragma once`
      files preserve that form unless `header-guard-style` is explicitly configured (also already
      defined). Needs the filename passed in (per this file's scoping decision above) and needs
      `Config.java` to exist to actually read `header-guard-rename`/`header-guard-style` --
      likely implementable against hardcoded defaults now and wired to real `Config` once that
      class exists, same precedent as other sections that don't block on `Config.java`

### §11 Include Ordering
- [ ] Two groups (angle-bracket system headers, then quoted local headers) separated by exactly
      1 blank line; grouping is always enforced regardless of `include-sort` (config key already
      defined, default off -- alphabetical sort within each group is opt-in since include order
      can affect behavior via macro dependencies). The file's own corresponding header (e.g.
      `Foo.cpp` → `Foo.h`) always goes first in group 2, regardless of `include-sort`. Needs the
      filename passed in (per this file's scoping decision above) to identify "the file's own
      header"

---

## Config Keys and Defaults

Configurable values with their in-class defaults. All overridable via config file or CLI.

```properties
# ── Structural constants ──────────────────────────────────────────────────────
line-length                = 100
indent-size                = 4
indent-style               = spaces          # spaces | tabs | keep
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

Once the above is checked off, the formatter's core (Tier 1 + Tier 2, STYLE.md /
STYLE_C_CPP.md / STYLE_JAVA.md) is considered complete. Phase 2 — Java 17+ and
C++20+ construct support — begins at that point, tracked separately in
`STATE_NEXT.md` (which also covers trimming `AI_PREAMBLE.md` down to its
post-JAR Tier-3-only scope). Do not open or read `STATE_NEXT.md`,
`STYLE_JAVA17.md`, or `STYLE_CPP20.md` before this milestone is checked off.
