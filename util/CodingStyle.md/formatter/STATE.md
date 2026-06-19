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
| §15 comment scope and sentence detection | Asked the user two scope questions, then re-checked against STYLE.md §15's actual current text (which is richer than the checklist's original framing) and corrected accordingly. Resolved (1): the capitalize/no-trailing-period rule applies to every `//` comment this pass sees -- but STYLE.md's pre-existing "labels, closing comments, and markers are not sentences" exemption (`// for i`, `// class Foo`, `/* FALL-THROUGH */`) is kept exactly as written there, achieved via **pipeline ordering**, not an in-rule heuristic: this §15 pass must run before `BlockStructureRule`'s §7 closing-comment-insertion pass and `SwitchRule`'s §13 fallthrough-marking pass in `Main.java`'s eventual orchestration, so label/marker comments simply don't exist in the token stream yet when this pass runs -- nothing to detect or exempt. Recorded as a hard constraint for whoever wires up `Main.java`'s pass order. Resolved (2), refined by the user beyond the original yes/no framing: period-stripping specifically (not capitalization, which always applies) depends on sentence count -- a comment is single-sentence (period stripped) iff the only `.` in its text is its own last non-whitespace character; if a `.` appears anywhere earlier followed by more text, the comment has 2+ sentences and its trailing period is left untouched. (`// Increment index.` → strip; `// Increment index. This index is incremented for XXX.` → leave both periods). `...`/ellipsis is still never touched, per the original checklist note; this whole branch is defensive handling of already-existing non-compliant `//` comments, since STYLE.md's own prescriptive guidance is to write multi-sentence content as a `/* */` block in the first place. Resolved (3): `COMMENT_BLOCK` (`/* ... */`) is in scope for the same capitalize/sentence-aware-period rule, plus a structural precondition the user added: a block comment that already spans multiple lines must have its `/*` and `*/` forced onto their own lines (moved if not already there), matching the shape of STYLE.md's own multi-sentence worked example. Asked a follow-up on how far that structural rule reaches: resolved that only already-multi-line block comments get this banner treatment -- a short single-line `/* note */` (including inline/trailing ones) is left alone, never force-exploded into a 3-line banner, since that would be a large structural change with no STYLE.md worked example to justify it. Resolved (4), newly discovered while re-reading STYLE.md §15 (this rule was entirely missing from the original checklist): STYLE.md also documents a **separator alignment** rule -- when inline trailing `//` comments across an aligned group share a separator character (`—`, `:`, etc.), that separator is aligned across the group by padding the label text before it, per STYLE.md's `int[] x`/`xy`/`z` worked example. Added as a new checklist item; its exact grouping-detection mechanics (reuse §5/§6 groups vs. compute independently) are deferred until that item is implemented. Evaluated whether any of this needed to be written into `STYLE.md` itself (per the user's "add them to STYLE.md too if you think it is necessary, otherwise commit" instruction) and concluded no: the label exemption and separator-alignment rule are already fully documented in STYLE.md (only this checklist was stale/incomplete relative to it), and the pipeline-ordering mechanism plus the §6 rendering algorithm are pure implementation detail, consistent with the §14 precedent of keeping such algorithms out of STYLE.md |

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
| `MiscRule.java` | IN PROGRESS |
| `CppSpecificRule.java` | NOT STARTED |
| `JavaSpecificRule.java` | NOT STARTED |
| `README.md` | NOT STARTED |

---

## Current File: `MiscRule.java` — IN PROGRESS

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
- [x] No space between a control-flow keyword (`if`/`while`/`for`/`switch` -- exactly the four
      keywords in STYLE.md §3.2's code block, no others) and its following `(`: collapse any
      gap (spaces only -- a comment or a `NEWLINE` in the gap blocks the rewrite, same
      conservative posture as `BlockStructureRule`'s brace-style passes) down to zero width.
      This rule only ever removes whitespace; it never decides whether the *contents* of the
      `(...)` are padded -- that is §3.1, already implemented in `ComplexityPaddingEvaluator`,
      and wiring it into an actual rewrite pass is a separate, not-yet-assigned piece of work
      (flag this gap rather than silently expanding scope here)

### §3.3 `{}` initializer / block spacing
- [x] Empty `{}` -- always tight, no padding, regardless of context
- [x] Single-level `{ ... }` with content -- pad with one space inside both braces
- [x] Nested `{ { ... }, { ... } }` -- pad at every nesting level, not just the outermost
- [x] Scope to brace-initializer lists only (array/struct initializers, `= { ... }` contexts) --
      must not touch a control-flow or function/class body `{ }` (those are §11/§7's domain,
      already implemented in `BlockStructureRule`); confirm how to distinguish the two
      structurally (no AST -- likely: an initializer brace's nearest preceding significant
      token is `=`, or another `{`/`,` while already inside a recognized initializer) before
      writing code, and if genuinely ambiguous for some shape, stop and ask rather than guess

### §4 Pre/Post Increment and Decrement
- [x] Detect `i++`/`i--` (post) used as a bare expression statement (`i++;` at statement level,
      not embedded in a larger expression) and rewrite to `++i;`/`--i;`
- [x] Leave post-increment/decrement untouched when it is NOT a bare statement -- i.e. whenever
      its value is actually used (`arr[i++]`, `return i--`, `x = i++`, as a function argument,
      etc.) -- since STYLE.md §4 explicitly carves out "post-increment semantics required by the
      surrounding expression" as the exception
- [x] Confirm `for(...; ...; i++)` loop increment clauses count as "value not used" (the
      increment's result is discarded either way) and should also be rewritten to `++i` --
      STYLE.md's own examples are framed as general "prefer pre" with explicit exceptions only
      for the value-is-used cases, so a `for` increment clause is not an exception

### §1 Indentation
- [ ] 4 spaces per indent level -- for any reformatting this rule or others perform that need to
      *generate* new indentation (e.g. wrapped function signatures in §8), use 4 spaces, tab
      display size 4
- [ ] `indent-style = spaces | tabs`: a simple, single-file mechanical conversion of every
      indentation whitespace run to the specified style -- implement directly in `MiscRule.java`
      (or a small helper it owns), no project-wide context needed for these two modes
- [ ] `indent-style = keep` (resolved -- see Resolved Design Decisions: "§1 indentation scope"):
      requires a new dedicated file-walking/detection class (not yet created, not
      `Main.java`/`Config.java` directly) that scans the whole project once to determine the
      dominant existing style, then calls into `MiscRule.java`'s plain spaces/tabs converter with
      that resolved choice -- `MiscRule.java` itself never has to interpret "keep"

### §2 Line Length
- [ ] Confirm scope: STYLE.md §2 states a 100-char soft limit and explicitly defers the only
      described mechanical fix (breaking) to §8 (Function Signatures). There is no other
      described mechanical rewrite for an over-length line in STYLE.md. Decide here whether
      `MiscRule.java` needs anything for §2 beyond what §8 already does (e.g. a `--check`-mode
      warning emission, out of scope for this rule class which only renders text) -- likely a
      no-op section beyond documenting the line-length constant for §8 to consume; do not invent
      additional line-breaking behavior beyond §8's explicit scope

### §6 Assignment and Compound Operator Alignment
(resolved -- see Resolved Design Decisions: "§6 grouping and rendering")
- [ ] Detect alignment groups: any maximal run of textually-adjacent assignment statements
      (`=`, `|=`, `&=`, `>>=`, etc. -- any compound-assignment operator counts), regardless of
      which variable(s) are involved -- same grouping signal as §14, broken by a blank line, a
      comment, or a non-assignment statement
- [ ] Render via two independently-computed fixed widths per group (NOT a single `ColumnGrid`
      left-pad column -- verified by hand against STYLE.md §6's worked example that this does not
      reproduce it): `maxNameLen` = max target-name length in the group; `maxPrefixLen` = max
      length of (operator text minus its trailing `=`) in the group (0 for plain `=`, 1 for
      `|=`/`&=`/etc., 2 for `>>=`/`<<=`). Each row renders as
      `padRight(name, maxNameLen) + padLeft(prefix, maxPrefixLen) + "=" + " " + value + ";"` --
      the `padLeft` on the prefix is what lines up every row's `=` regardless of which operator
      it uses
- [ ] A lone variable with no group neighbors aligns trivially with itself -- falls out for free
      from the same width computation (group of 1 means both maxes equal that row's own widths)
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
(resolved -- see Resolved Design Decisions: "§15 comment scope and sentence detection")
- [ ] Applies to every `//` comment this pass actually sees. STYLE.md's own "labels, closing
      comments, and markers are not sentences" exemption (`// for i`, `// class Foo`,
      `/* FALL-THROUGH */`) is satisfied by **pipeline ordering, not in-rule detection**: this
      §15 pass must run before `BlockStructureRule.addClosingComments`/
      `insertNamedConstructBlankLines` (§7) and `SwitchRule.markFallthrough` (§13) in
      `Main.java`'s eventual pass sequence, so those labels/markers don't exist yet in the token
      stream when this pass runs and there is nothing to exempt. **Constraint for whoever wires
      up `Main.java`'s pass order: §15 (`MiscRule`) before §7/§13 (`BlockStructureRule`/
      `SwitchRule`).** No new "is this a label" heuristic is implemented in `MiscRule.java` itself
- [ ] Capitalize the first letter if it is currently lowercase and alphabetic; leave
      non-alphabetic first characters (symbol, number, already-uppercase) untouched -- this
      applies regardless of single- vs. multi-sentence (see next item)
- [ ] Strip the trailing `.` only when the comment is a single sentence: i.e. when the only `.`
      in the comment's text is its own last non-whitespace character. If a `.` appears anywhere
      else (followed by more text), the comment has 2+ sentences and the trailing period is left
      untouched (do not strip it). `...`/ellipsis still must never be touched regardless. Exact
      mechanical detection (e.g. ellipsis disambiguation) is an implementation-time judgment call,
      not a remaining open question. (STYLE.md's own prescriptive guidance is that multi-sentence
      content should be written as a `/* */` block comment in the first place -- this branch is
      defensive handling for already-existing non-compliant `//` comments, not a contradiction of
      that guidance)
- [ ] `COMMENT_BLOCK` (`/* ... */`) is in scope, with the same capitalize/single-sentence-period
      rule as `//`, PLUS a structural precondition: only a block comment that already spans
      multiple lines gets its `/*` and `*/` forced onto their own lines (moved there if not
      already) -- matching the shape STYLE.md's own multi-sentence worked example already shows.
      A short single-line `/* note */` (including inline/trailing ones) is left completely alone
      -- never force-exploded into a 3-line banner
- [ ] **Separator alignment** (new item -- STYLE.md §15 has this rule with its own worked example;
      it was missing from this checklist entirely): when inline trailing `//` comments across an
      aligned group (e.g. a declaration-alignment or assignment-alignment group from §5/§6) all
      use the same separator character (`—`, `:`, etc.), align that separator column across the
      group by padding the label text before it -- mirrors the `int[] x` / `xy` / `z` worked
      example. Needs its own grouping-detection question (does this reuse §5/§6's groups
      directly, or compute independently from comment text alone?) when this item is reached

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
