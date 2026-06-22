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
asks. All decisions relevant to implementation are recorded in the
**Resolved Design Decisions** index below (full text in `STATE_rdd_log.md`).
`FORMATTER_DISCUSSION.md` is design history and future planning only — large, and
contains nothing the implementer needs beyond what is already indexed here.

> ⛔ **PHASE-2 GATE — DO NOT READ:**
> `STYLE_JAVA17.md`, `STYLE_CPP20.md`, `STATE_NEXT.md`, and `STATE_rdd_log.md` (in full).
> These are off-limits until the End Goal dogfood-test milestone is checked off.
> `STATE_rdd_log.md` may only be accessed via `grep -Fm1 'RDD_KEY_n'` for a specific key.
> Violation of this gate wastes context and risks importing out-of-scope constraints.

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
5. Once resolved: append the full decision as a new row to `STATE_rdd_log.md`
   (next `RDD_KEY_n` number), add the key + topic to the **Resolved Design
   Decisions** index in this file, remove from **Open Questions**, unblock
   the checklist item, then continue

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
      Formatter.java            ← shared per-file pipeline (Config.resolve + ScopePipeline.process +
                                   whole-file enforceX passes, in order) called by both Main.java and
                                   ServerMode.java -- see "Formatter.java orchestration architecture"
                                   in Resolved Design Decisions
      IndentationDetector.java  ← whole-project dominant-indent-style walker (for `indent-style = keep`)
      ScopePipeline.java        ← recursive scope/signature discovery + group-render-splice engine
                                   for DeclarationAlignmentRule/GetterSetterRule/MiscRule's grouping
                                   rules (STYLE.md §5/§6/§8/§14) -- see "Main.java orchestration
                                   architecture" in Resolved Design Decisions
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

Full decision text lives in `STATE_rdd_log.md` — **do not read that file in full**.
To look up a specific decision during implementation:
```
grep -Fm1 'RDD_KEY_n' util/CodingStyle.md/formatter/STATE_rdd_log.md
```

| Key | Topic |
|---|---|
| RDD_KEY_1 | Tokenizer |
| RDD_KEY_2 | Rule engine |
| RDD_KEY_3 | Shared grid |
| RDD_KEY_4 | Modifier priority |
| RDD_KEY_5 | Constants |
| RDD_KEY_6 | Java parsing |
| RDD_KEY_7 | AI dependency |
| RDD_KEY_8 | JAR target |
| RDD_KEY_9 | Server mode |
| RDD_KEY_10 | Server idempotency |
| RDD_KEY_11 | Port |
| RDD_KEY_12 | Path separator |
| RDD_KEY_13 | Lockfile location |
| RDD_KEY_14 | Line endings |
| RDD_KEY_15 | Config precedence |
| RDD_KEY_16 | `.style-fmt` inheritance |
| RDD_KEY_17 | Multi-module Java imports |
| RDD_KEY_18 | Windows support |
| RDD_KEY_19 | Output modes |
| RDD_KEY_20 | Build |
| RDD_KEY_21 | `ColumnGrid` flush API |
| RDD_KEY_22 | §3.1 complexity padding algorithm |
| RDD_KEY_23 | Declaration-statement detection |
| RDD_KEY_24 | Column grid rendering |
| RDD_KEY_25 | Static reorder vs. STYLE.md §5's worked example |
| RDD_KEY_26 | §10 Single-expression block eligibility |
| RDD_KEY_27 | §11 K&R brace style detection |
| RDD_KEY_28 | §11 lambda bodies also use K&R |
| RDD_KEY_29 | §12 else/else-if placement |
| RDD_KEY_30 | C/C++ bitfield column (`STYLE_C_CPP.md` §6) |
| RDD_KEY_31 | §7 closing comments — key variable on nesting |
| RDD_KEY_32 | §7 closing comments — engine structure |
| RDD_KEY_33 | §7 closing comments — named-construct blank lines |
| RDD_KEY_34 | §13 non-inline case brace wrapping |
| RDD_KEY_35 | §13 nested switch processing order |
| RDD_KEY_36 | §13 inline switch row classification |
| RDD_KEY_37 | §13 fallthrough marking |
| RDD_KEY_38 | §14 getter/setter rendering |
| RDD_KEY_39 | §14 getter/setter group detection |
| RDD_KEY_40 | §3.2 keyword spacing |
| RDD_KEY_41 | §3.3 initializer brace spacing |
| RDD_KEY_42 | §4 pre-increment rewrite |
| RDD_KEY_43 | §1 indentation scope |
| RDD_KEY_44 | §6 grouping and rendering |
| RDD_KEY_45 | §8 signature scope and rendering |
| RDD_KEY_46 | §9 function-body detection and return scoping |
| RDD_KEY_47 | §15 comment scope and sentence detection |
| RDD_KEY_48 | §15 partial-implementation split |
| RDD_KEY_49 | §15 multi-line block comment banner reformatting |
| RDD_KEY_50 | §15 separator alignment |
| RDD_KEY_51 | §6 multi-line right-hand sides |
| RDD_KEY_52 | §1 empty parameter list (`CppSpecificRule.java`) |
| RDD_KEY_53 | §2 one-liner scope (`CppSpecificRule.java`) |
| RDD_KEY_54 | §9 section dividers are non-actionable |
| RDD_KEY_55 | §4 pointer/const spacing already satisfied |
| RDD_KEY_56 | §3 template angle-bracket spacing (`CppSpecificRule.java`) |
| RDD_KEY_57 | §10 header file structure (`CppSpecificRule.java`) |
| RDD_KEY_58 | §11 dropped from `CppSpecificRule.java` scope |
| RDD_KEY_59 | `JavaSpecificRule.java` scoping |
| RDD_KEY_60 | §2 Allman-conversion vs. getter/setter one-liner groups -- left unguarded |
| RDD_KEY_61 | §3.1 condition-interior padding -- wiring decision |
| RDD_KEY_62 | §3.1 condition-interior padding -- implementation |
| RDD_KEY_63 | §2 method-definition Allman conversion (`JavaSpecificRule.java`) |
| RDD_KEY_64 | §4 array-declaration syntax parenthetical -- non-actionable |
| RDD_KEY_65 | §7 import group order/count contradiction |
| RDD_KEY_66 | `Main.java` orchestration architecture |
| RDD_KEY_67 | STYLE.md §5/§6 scope -- anywhere in code, recursively |
| RDD_KEY_68 | `DeclarationAlignmentRule.splitStatements` depth-awareness fix |
| RDD_KEY_69 | §7 import ordering implementation (`JavaSpecificRule.java`) |
| RDD_KEY_70 | `Config.java` file format |
| RDD_KEY_71 | `Config.java` resolution scope |
| RDD_KEY_72 | `Formatter.java` orchestration architecture |
| RDD_KEY_73 | `ServerMode.java` wire protocol |
| RDD_KEY_74 | `Formatter.java` whole-file pass order |
| RDD_KEY_75 | Supersedes RDD_KEY_60 -- Allman pass actually destroys §14 grouping, ordering alone insufficient |
| RDD_KEY_76 | `DeclarationAlignmentRule` misparses a bare `++j;`/`--j;` statement as a fake field declaration |
| RDD_KEY_77 | `MiscRule.enforceCommentStyle` relied on pipeline ordering (not detection) to skip closing-comment labels, breaking idempotency |
| RDD_KEY_78 | `ScopePipeline.splitTopLevelSpans` never closed a span at a C++ access-specifier label, merging it into the following member |

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
| `Config.java` | COMPLETE |
| `ServerMode.java` | NOT STARTED |
| `Formatter.java` | COMPLETE |
| `IndentationDetector.java` | NOT STARTED |
| `ScopePipeline.java` | COMPLETE (reopened during `Formatter.java` smoke-testing to fix a C++ access-specifier-label span bug -- see Resolved Design Decisions: "`ScopePipeline.splitTopLevelSpans` never closed a span at a C++ access-specifier label, merging it into the following member") |
| `TokenizerCore.java` | COMPLETE |
| `ColumnGrid.java` | COMPLETE |
| `ModifierPriority.java` | COMPLETE |
| `CppModifierPriority.java` | COMPLETE |
| `JavaModifierPriority.java` | COMPLETE |
| `ComplexityPaddingEvaluator.java` | COMPLETE |
| `DeclarationAlignmentRule.java` | COMPLETE (`splitStatements` made depth-aware -- see Resolved Design Decisions: "`DeclarationAlignmentRule.splitStatements` depth-awareness fix"; reopened during `Formatter.java` smoke-testing to reject a non-keyword, non-identifier type lead -- see Resolved Design Decisions: "`DeclarationAlignmentRule` misparses a bare `++j;`/`--j;` statement as a fake field declaration") |
| `BlockStructureRule.java` | COMPLETE |
| `SwitchRule.java` | COMPLETE |
| `GetterSetterRule.java` | COMPLETE |
| `MiscRule.java` | COMPLETE (§1 `indent-style=keep` cross-file integration deferred to `IndentationDetector.java` -- see Resolved Design Decisions: "§1 indentation scope"; §3.1 condition-interior padding added -- see Resolved Design Decisions: "§3.1 condition-interior padding -- implementation"; reopened during `Formatter.java` smoke-testing to add structural detection for closing-comment labels -- see Resolved Design Decisions: "`MiscRule.enforceCommentStyle` relied on pipeline ordering (not detection) to skip closing-comment labels, breaking idempotency") |
| `CppSpecificRule.java` | COMPLETE (§11 "Include Ordering" dropped from scope -- no such section exists in STYLE_C_CPP.md; see Resolved Design Decisions: "§11 dropped from `CppSpecificRule.java` scope"; reopened during `Formatter.java` smoke-testing to add the §14 one-liner adjacency heuristic -- see Resolved Design Decisions: "Supersedes RDD_KEY_60 -- Allman pass actually destroys §14 grouping, ordering alone insufficient") |
| `JavaSpecificRule.java` | COMPLETE (reopened during `Formatter.java` smoke-testing to add the §14 one-liner adjacency heuristic -- see Resolved Design Decisions: "Supersedes RDD_KEY_60 -- Allman pass actually destroys §14 grouping, ordering alone insufficient") |
| `README.md` (defer until just before Dogfood) | NOT STARTED |

---

## `ScopePipeline.java` — COMPLETE

Implemented per the checklist below (all items done, smoke-tested end-to-end: §5/§6/§8/§14 each
verified individually plus nested recursion across method/if-block scopes, the §14
outlier-exclusion non-contiguous-splice path, and round-trip idempotency -- not committed, same
throwaway-smoke-test precedent as the `splitStatements` depth-fix). Design history kept below for
reference.

> While scoping `Main.java`'s checklist, found that `Main.java`'s real prerequisite is a new file,
> `ScopePipeline.java` (see `Project Layout`/`File Status`, inserted before `Main.java`). Several
> grouping rule classes (`DeclarationAlignmentRule.groupDeclarations`, `GetterSetterRule.groupOneLiners`,
> `MiscRule.groupAssignments`, `MiscRule.parseSignature`) explicitly document that the **caller**
> must find scope/signature boundaries in the whole-file token stream and splice rendered group
> output back in -- no code anywhere does this yet. `ScopePipeline.java` is that caller.
> `Main.java`'s own checklist is deferred until `ScopePipeline.java`'s checklist is written and
> implemented -- `Main.java`'s job will then be much thinner (CLI args, file discovery, config
> loading, one call into `ScopePipeline` per file for the grouping rules, then the remaining simple
> whole-file `enforceX` passes called directly, then output-mode handling).
>
> **Resolved so far** (see Resolved Design Decisions for full detail on each):
> - Boundary-finding + splice-back orchestration lives in this new dedicated class, not
>   `Main.java`/`Config.java` -- "`Main.java` orchestration architecture".
> - STYLE.md §5/§6 (declaration/assignment alignment) apply **anywhere in code, recursively** --
>   not just class/struct bodies, but function/method bodies and every nested block too --
>   "STYLE.md §5/§6 scope -- anywhere in code, recursively".
> - Fixed a latent bug this surfaced: `DeclarationAlignmentRule.splitStatements` was not
>   depth-aware and would have corrupt-split on a scope containing a nested `{ }` block. Ported
>   `MiscRule.splitAssignmentStatements`'s depth-tracking algorithm into it directly (small,
>   mechanical, already-verified-by-smoke-test fix to an already-COMPLETE file, not a new design
>   question) -- "`DeclarationAlignmentRule.splitStatements` depth-awareness fix".
>
> **Resolved (pre-session Q&A — all open items closed, checklist ready to write):**
>
> **Boundary contracts (three distinct granularities):**
> - `groupDeclarations` / `groupAssignments` (§5/§6): direct-content-only slice — nested `{ }`
>   block appears as one opaque consumed statement in the slice; inner tokens not included.
> - `groupOneLiners` (§14): full type-body range including nested method-body tokens — confirmed
>   by reading `GetterSetterRule.splitMembers`, which tracks brace depth and consumes nested bodies.
> - `parseSignature` (§8): signature span only — first modifier/return-type token through the
>   closing `)` of the parameter list; not brace-delimited at all.
>
> **§8 signature finder — definitions only:**
> STYLE.md §8's worked examples show only function definitions; no prototype example exists
> anywhere in the spec. Confirmed: `ScopePipeline`'s signature-finder targets only definitions
> (`)` directly followed by `{`, skipping a possible C++ trailing qualifier or Java `throws`
> clause). Bare prototypes (`void foo();`) are left untouched — same deliberate gap as
> `CppSpecificRule.java` §1. No `AskUserQuestion` needed.
>
> **Recursive walk order — outer-first:**
> `ScopePipeline` visits scopes outer-first then recurses. This is safe because each scope
> receives its own extracted direct-content slice — inner splices never affect outer token
> indices. (Inner-first is only needed when passes share one flat token list with overlapping
> spans, as in §13's nested-switch fix — not the case here.)
>
> **Splice-back and internal pipeline:**
> For each scope: extract slice → group → render → splice rendered output back into the slice
> → re-tokenize the slice → recurse into nested scopes on the fresh token list. Chained via
> re-tokenizing between passes, same precedent as §11/§12/§13/§15 throughout this codebase.
>
> **Hard pipeline-ordering constraints (unchanged, carry forward to checklist):**
> - `GetterSetterRule` must run before any Allman-conversion pass (both languages) --
>   RDD_KEY_60.
> - Several `MiscRule`/`BlockStructureRule`/`SwitchRule` passes have ordering requirements
>   relative to each other -- see each section's own RDD entry.
>
> **Splice-back mechanics (mechanical fill-in, not a new design decision -- same category as
> the `splitStatements` depth-awareness fix above):**
> - `GetterSetterRule.Member` already stores `memberFrom`/`memberTo` indices directly -- §14's
>   splice-back needs no extra bookkeeping.
> - `MiscRule.Signature` carries no indices at all, but `ScopePipeline` itself is the one that
>   finds and slices `sigTokens` in the first place (see §8 checklist item below), so it already
>   has the exact `[start, closeParenIdx]` range before calling `parseSignature` -- no recovery
>   needed there either.
> - `DeclarationAlignmentRule.Declaration` and `MiscRule.Assignment` carry neither indices nor a
>   reference to their statement's closing `;`/trailing-comment token. Since `Token` has no
>   `equals`/`hashCode` override (default reference identity), and every `Declaration`/
>   `Assignment` field holds the *same* `Token` instances sliced out of the `scopeTokens` list
>   `ScopePipeline` already has, the fix is: build an `IdentityHashMap<Token,Integer>` once per
>   scope (token instance -> index in `scopeTokens`), then for each returned group, look up an
>   anchor token from its first/last `Declaration`/`Assignment` (e.g. `name`/`target`, or
>   `trailingComment` when present) to find which of `ScopePipeline`'s own independently-computed
>   top-level spans (see checklist below) that statement falls in -- the *span's* `(start, end)`
>   (not the anchor token's own index) is what is actually replaced, since the span already
>   correctly includes the trailing `;`/comment via the identical depth-aware algorithm.
>
> **Per-scope pass design (mechanical fill-in):** since `groupOneLiners`'s "full type-body
> range including nested method-body tokens" and `groupDeclarations`/`groupAssignments`'s
> "direct-content-only slice, nested block opaque" boundary contracts are both already
> satisfied just by handing **the same `scopeTokens` slice** to each call (their own internal
> depth-aware statement/member splitters already treat a nested `{ }` as one opaque unit), no
> separate slice-extraction step is needed per rule -- every pass for one scope operates on that
> scope's own full `(open, close)`-exclusive token range. Four passes run in this fixed order,
> re-tokenizing the scope's text between each (same "chained via re-tokenizing between passes"
> precedent as §11/§12/§13/§15): §5 declarations, §6 assignments, §8 signatures, §14
> getter/setter. §8 before §14 specifically: §8's signature rendering only ever touches the
> `[leadTokens..closeParen]` span, never the body, so it cannot disturb a one-liner body; running
> it first lets §8 normalize/possibly-break long signatures before §14's grid-alignment pass
> re-pads the (by-definition short, one-line) signatures of whatever one-liner run remains.
> §5/§6 are statement-level (`;`-terminated) and structurally disjoint from §8/§14's
> brace-block-terminated members, so their relative order doesn't matter; declarations-then-
> assignments is just the order STYLE.md numbers the sections in.
> Only after all four passes does recursion into child scopes happen (outer-first, per the
> already-resolved walk order).
>
> **Child-scope / signature-candidate discovery (mechanical fill-in):** one depth-aware
> top-level span splitter, `splitTopLevelSpans`, ported a third time (same algorithm already
> duplicated in `DeclarationAlignmentRule.splitStatements` and
> `MiscRule.splitAssignmentStatements`) -- but additionally recording, for any span that closes
> via a top-level `}` rather than a `;`, the matching open-brace index. This one helper serves
> three jobs at once: (1) anchor-token -> span lookup for the §5/§6 splice-back above, (2)
> finding every child scope to recurse into (any `}`-closed span's `(openBraceIdx+1, closeIdx)`
> interior), and (3) finding §8 signature candidates (a `}`-closed span whose open brace is
> directly preceded -- skipping whitespace/comments/newlines -- by a `)` whose matching `(` is
> itself preceded by an IDENTIFIER not preceded by `new`; ported from
> `JavaSpecificRule.isCandidateMethodName`/`CppSpecificRule.isCandidateSignatureName`, plus
> `JavaSpecificRule.isEnumConstantBody`'s exclusion guarded by `"java".equals(language)`). A
> signature candidate's lead span is `[span.start, closeParenIdx]`; its body is the same child
> scope found by (2) for that span -- both are derived from one scan, not two.
>
> **Indentation level for §8 rendering:** `MiscRule.render(Signature, indentLevel, indentStyle)`
> needs the nesting depth of the signature's own scope. `ScopePipeline`'s recursion already
> threads a plain `int depth` parameter (0 at the file root, incremented by 1 each time it
> recurses into a child scope's interior) -- pass that straight through as `indentLevel`, no
> separate computation needed.
>
> All design questions are now resolved; the checklist below is ready to implement directly.

### Checklist

- [x] **Skeleton + shared helpers** -- class fields (`language`, `indentStyle`, a
      `TokenizerCore`, one instance each of `DeclarationAlignmentRule`/`GetterSetterRule`/
      `MiscRule`); ported low-level scanning helpers duplicated per this codebase's
      one-owner-per-class precedent: `isPunct`, `isOp`, `isGapToken`, `prevSignificantIndex`,
      `nextSignificantIndex`, `matchParenForward`, `matchParenBackward`, `matchBraceForward`.
- [x] **`splitTopLevelSpans`** -- the depth-aware span splitter described above (statement- and
      block-terminated spans, contiguous coverage of the input, open-brace index recorded for
      block-terminated spans, same `pullTrailingSameLine`-style same-line-trailing-comment pull
      ported alongside it).
- [x] **§5 pass** -- `groupDeclarations(scopeTokens)` -> `render(group)` per group -> anchor
      each group to a `(start, end)` span range via the `IdentityHashMap` lookup described above
      -> splice all groups' rendered text back into `scopeTokens`' source text in one pass
      (spans not covered by any group pass through verbatim).
- [x] **§6 pass** -- identical shape to §5 using `groupAssignments`/`render` (Assignment's
      anchor token is `target`, or for a `multiLine` row, `firstLineValueTokens.get(0)`).
- [x] **§8 pass** -- signature-candidate scan per `splitTopLevelSpans` (above) -> for each
      candidate, `parseSignature(sigTokens)` (returns `null` => leave untouched, same posture as
      every other unrecognized shape in this codebase) -> `render(sig, depth, indentStyle)` ->
      splice only the `[span.start, closeParenIdx]` range, leaving the body untouched.
- [x] **§14 pass** -- `groupOneLiners(scopeTokens)` -> `excludeOutliers(scopeTokens, group)` ->
      `render(scopeTokens, group)` -> splice using each `Member`'s own `memberFrom`/`memberTo`
      directly (no anchor lookup needed); a group that drops below 2 after exclusion is skipped
      entirely (members render unchanged, per `excludeOutliers`'s own contract).
- [x] **Recursion driver** -- `processScope(tokens, depth)`: run the four passes above in
      fixed order, re-tokenizing the scope's text between each; then, on the final token list,
      use `splitTopLevelSpans` again to find every block-terminated span's child interior,
      recursively call `processScope(childTokens, depth + 1)` on each (outer-first: this scope's
      own four passes complete before any child is touched), splice each child's processed text
      back in place; return the final assembled text for this scope.
- [x] **`process(String source)`** -- public entry point: tokenize the whole file via
      `TokenizerCore`, call `processScope(tokens, 0)`, return the result. This is the one method
      `Main.java` will call once per file.
- [x] **Throwaway smoke test** -- not committed, same precedent as the `splitStatements`
      depth-fix's verification: hand-built source snippets covering one example each of §5
      (declarations inside a method body, not just a class body -- exercises RDD_KEY_67),
      §6, §8 (including a signature long enough to force the broken multi-line form), §14, and
      nested recursion (a declaration group inside an `if` block inside a method inside a
      class), verified to render correctly end-to-end.

---

## `Config.java` — COMPLETE

Implemented per the checklist below (all items done, smoke-tested end-to-end: built-in defaults,
global-config override, `.style-fmt` two-level cascade (subdir overrides project root, project
root overrides global), `STYLEFMT_*` env vars, CLI override beating every other layer, and
fail-soft invalid-value handling -- not committed, same throwaway-smoke-test precedent as
`ScopePipeline.java`'s). Design history kept below for reference.

While scoping `Main.java`'s checklist, confirmed `Config.java` is the next real prerequisite
(see RDD_KEY_66): it's the most foundational of `Main.java`'s three remaining NOT-STARTED
dependencies (`Config.java`, `ServerMode.java`, `IndentationDetector.java`), and unlike
`IndentationDetector.java` (open design question per RDD_KEY_43, deferred), its design is now
fully resolved -- no outstanding ambiguity.

**Resolved (this session — checklist ready to write):**
- Config file format (both `~/.config/style-fmt/config` and `.style-fmt`): hand-rolled
  `key = value` line parser, not `java.util.Properties` — RDD_KEY_70.
- Resolution scope: per-file, via a `Config.resolve(Path targetFile, Map<String,String>
  cliOverrides)` factory that walks `targetFile`'s directory upward collecting `.style-fmt`
  overrides to the filesystem root, rather than one config resolved once per invocation —
  RDD_KEY_71.
- Full precedence chain (already resolved pre-session): RDD_KEY_15. `.style-fmt` cascade
  semantics: RDD_KEY_16. `STYLEFMT_*` env var layer: implied by RDD_KEY_15, key-name mapping
  is mechanical (`line-length` → `STYLEFMT_LINE_LENGTH`: uppercase, `-` → `_`, prefixed).

**Mechanical fill-in (not new design decisions):**
- Each layer (global config file, one `.style-fmt` file, env vars, CLI overrides) parses into
  a raw `Map<String,String>` containing only the keys it explicitly sets — never pre-filled
  with defaults. Layers are merged in RDD_KEY_15 order via simple overlay (later layer's keys
  win); built-in defaults are applied only for keys absent from every layer.
- `.style-fmt` ancestor walk: starting at `targetFile`'s parent directory, check each directory
  for a `.style-fmt` file up to the filesystem root, collecting matches outer-to-inner (root-most
  first) so that closer-to-file always overrides farther-from-file, per RDD_KEY_16's cascade. No
  special "project root" marker (e.g. `.git`) is needed — walking to the filesystem root and
  merging every `.style-fmt` found along the way already produces the correct two-tier (or
  N-tier) cascade RDD_KEY_15/16 describe.
- Typed construction: after the final raw-string merge, each key is parsed into its declared
  type (`int`, boolean-as-`on`/`off`, or a restricted-choice `String`). An invalid/unparseable
  value for a key prints a warning to stderr and falls back to that key's built-in default —
  fail-soft, consistent with this codebase's existing posture elsewhere (e.g. `parseSignature`
  returning `null` rather than crashing on an unrecognized shape). Never throws/exits the run.
- `java-import-order`'s comma-separated value parses into a trimmed `List<String>`; every other
  key is scalar.
- CLI overrides are merged in last (highest precedence per RDD_KEY_15), as a
  `Map<String,String>` parameter `Main.java` passes into `Config.resolve` — `Main.java`'s own
  CLI-flag parsing is out of scope for `Config.java` itself, which only ever receives the
  already-parsed override map.

### Checklist

- [x] **Fixed constants + typed fields** — `APP_NAME`, `CONFIG_DIR`, `CONFIG_FILE` as
      `private static final` (per Fixed Constants table); one typed field per key in the
      Config Keys and Defaults table below, each initialized to its built-in default.
- [x] **`parseConfigFile(Path)` → `Map<String,String>`** — hand-rolled line parser: skip blank
      lines and lines whose first non-whitespace character is `#`; split remaining lines on the
      first `=`, trim both sides; return only the keys actually present.
- [x] **Env var layer** — collect `STYLEFMT_*` environment variables into a
      `Map<String,String>`, converting each config key to its env var name (uppercase, `-` → `_`,
      `STYLEFMT_` prefix) to reverse-match.
- [x] **`.style-fmt` ancestor walk** — given a target file's path, walk from its parent
      directory to the filesystem root, calling `parseConfigFile` on each `.style-fmt` found,
      collecting results outer-to-inner (root-most first).
- [x] **Layered merge** — overlay raw `Map<String,String>` layers in RDD_KEY_15 order (built-in
      defaults are the typed fallback, not a map layer; global config → env vars → `.style-fmt`
      walk results in outer-to-inner order → CLI overrides) into one final raw map.
- [x] **Typed construction** — build the final `Config` instance from the merged raw map: parse
      each key into its declared type, falling back to the built-in default with an stderr
      warning on any invalid/unparseable value; parse `java-import-order` into a `List<String>`.
- [x] **`Config.resolve(Path targetFile, Map<String,String> cliOverrides)`** — public static
      factory tying together all of the above; the one entry point `Main.java` calls once per
      file.
- [x] **Throwaway smoke test** — not committed, same precedent as `ScopePipeline.java`'s: a
      built-in-defaults-only case, a global-config-only override, an env var override, a
      two-level nested `.style-fmt` cascade (subdir overrides one key, inherits the rest from
      project root), and a CLI override beating every other layer — verified end-to-end.

---

## Current File: `Formatter.java` — COMPLETE

While scoping `ServerMode.java`'s wire protocol, found its `/format` handler needs the exact
per-file pipeline `Main.java`'s CLI path will also need -- see "`Formatter.java` orchestration
architecture" (RDD_KEY_72). This file owns that pipeline so neither caller duplicates it.

**Resolved:**
- Public API: `Formatter.formatOne(String content, String language, String filePath, Config
  config)` -- `filePath` was added beyond the original `(content, language, config)` sketch in
  RDD_KEY_72 once `CppSpecificRule.enforceHeaderFileStructure(tokens, filePath, renameGuard)`'s
  existing signature turned out to need a real path string for include-guard-name derivation; a
  mechanical signature refinement, not a new design question.
- Whole-file pass order for the 17 `enforceX` methods beyond ScopePipeline's §5/§6/§8/§14 --
  RDD_KEY_74 (confirmed with the user, adjustable later if a phase ordering turns out wrong in
  practice).

**Mechanical fill-in:**
- One `TokenizerCore` instance (`new TokenizerCore(language)`), re-tokenizing via
  `tokenizer.tokenize(text)` before every pass, same chained-re-tokenize precedent used
  throughout this codebase.
- One instance each of `BlockStructureRule` (via the `(language, closingCommentMinLines)`
  constructor, passing `config.closingCommentMinLines()`), `SwitchRule`, `MiscRule`, and --
  language-conditionally -- `CppSpecificRule` or `JavaSpecificRule`.
- §5/§6/§8/§14 run first via `new ScopePipeline(language, config.indentStyle()).process(content)`,
  before any `enforceX` call -- this alone satisfies RDD_KEY_60 (GetterSetterRule before Allman)
  with no extra bookkeeping.
- Cpp-only calls (`enforceFunctionDefinitionAllmanBraceStyle`, `enforceEmptyParameterList`,
  `enforceTemplateAngleBracketSpacing`, `enforceHeaderFileStructure`) and Java-only calls
  (`enforceMethodDefinitionAllmanBraceStyle`, `enforceImportOrdering`) are gated on
  `"cpp".equals(language)` / `"java".equals(language)` respectively; everything else runs for
  both languages.
- `enforceImportOrdering` is called with `config.javaImportOrder()`, `config.isJavaImportSort()`,
  `config.javaImportDepth()`, `config.javaImportBlankLines()` directly. `enforceHeaderFileStructure`
  is called with `filePath` and `config.isHeaderGuardRename()` directly -- `header-guard-style`
  needs no wiring at all per its own RDD_KEY_57 note ("needs no code at all right now").
- `convertIndentation` is called last with `config.indentStyle()` -- `indent-style=keep`'s
  cross-file majority detection remains deferred to `IndentationDetector.java` per RDD_KEY_43;
  `convertIndentation` itself already documents what it does with `"keep"` as input (unchanged
  from `MiscRule.java`'s existing COMPLETE behavior, not re-decided here).

### Checklist

- [x] **Skeleton** -- `formatOne(String content, String language, String filePath, Config
      config)`; construct one `TokenizerCore`, one `ScopePipeline`, one each of
      `BlockStructureRule`/`SwitchRule`/`MiscRule`, and the language-conditional
      `CppSpecificRule`/`JavaSpecificRule`.
- [x] **Phase 0** -- `scopePipeline.process(content)`.
- [x] **Phase 1 (structural/brace)** -- `collapseSingleExpressionBlocks` →
      `enforceKAndRBraceStyle` → `placeElseOnOwnLine` → `insertNamedConstructBlankLines` →
      language's Allman-conversion method → `enforceEmptyParameterList` (cpp only) →
      `formatNonInlineSwitches` → `insertBlankLineBeforeReturn`, re-tokenizing between each.
- [x] **Phase 2 (comment-style)** -- `enforceCommentStyle` → `alignCommentSeparators`.
- [x] **Phase 3 (comment/marker-generating)** -- `addClosingComments` → `markFallthrough` →
      `alignInlineSwitches`.
- [x] **Phase 4 (cosmetic spacing)** -- `enforceKeywordSpacing` →
      `enforceConditionComplexityPadding` → `enforceInitializerBraceSpacing` →
      `enforcePreIncrement` → `enforceTemplateAngleBracketSpacing` (cpp only).
- [x] **Phase 5 (file-header-level)** -- `enforceHeaderFileStructure` (cpp only) /
      `enforceImportOrdering` (java only).
- [x] **Phase 6 (final whitespace)** -- `convertIndentation`, return the result.
- [x] **Throwaway smoke test** -- not committed, same precedent as `Config.java`'s: one Java
      input and one C++ input, each exercising enough of STYLE.md/STYLE_JAVA.md/STYLE_C_CPP.md
      to touch every phase at least once, verified end-to-end plus idempotency. Surfaced and fixed
      four real bugs along the way (RDD_KEY_75 through RDD_KEY_78): §14 one-liner grouping
      destroyed by the Allman pass, a `++j;`/`--j;` misparsed as a fake declaration, auto-generated
      closing-comment labels wrongly recapitalized on a second pass, and a C++ access-specifier
      label merged into the following member's signature.

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
java-import-order          = java, com, org, other, local, static
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
