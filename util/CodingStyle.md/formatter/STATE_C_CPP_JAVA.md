# STATE_C_CPP_JAVA.md — C/C++/Java Formatter Implementation Tracker

Read `STATE_COMMON.md` first — shared commit/ambiguity/testing conventions this file
assumes. `STATE_KOTLIN.md` is a separate job's file, not required reading here.
Dogfood corpus status: see `STATE_DOGFOOD.md`.

---

## Project Layout

```
util/CodingStyle.md/formatter/
  STATE_C_CPP_JAVA.md        ← this file
  RDD_LOG.md           ← full Resolved Design Decisions text (do not read in full)
  STATE_AI.md           ← deferred AI-assist design and NOT FEASIBLE rationale
  README.md
  Makefile
  LICENSE
  src/
    com/jxmake/formatter/
      Main.java
      Config.java
      ServerMode.java
      Formatter.java (FormatterCore + FormatterCurly, curly logic in FormatterCurly)
      IndentationDetector.java
      ScopePipeline.java (ScopePipelineCore + ScopePipelineCurly)
      tokenizer/
        TokenizerCore.java (TokenizerCore + TokenizerCurly)
      grid/
        ColumnGrid.java
        ModifierPriority.java
        CppModifierPriority.java
        JavaModifierPriority.java
      evaluator/
        ComplexityPaddingEvaluator.java
      rules/
        DeclarationAlignmentRule.java (DeclarationAlignmentRuleCore + DeclarationAlignmentRuleCurly)
        BlockStructureRule.java
        SwitchRule.java
        GetterSetterRule.java (GetterSetterRuleCore + GetterSetterRuleCurly)
        MiscRule.java (MiscRuleCore + MiscRuleCurly)
        CppSpecificRule.java
        JavaSpecificRule.java
```

---

## Resolved Design Decisions

Lookup convention in `STATE_COMMON.md`. Index below (topic only, full text in `RDD_LOG.md`):

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
| RDD_KEY_16 | `.jxmake-code-formatter` inheritance |
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
| RDD_KEY_79 | `IndentationDetector.java` design (`indent-style = auto`) |
| RDD_KEY_80 | `ServerMode.java` idempotency check on a Java 8 build target -- `ProcessHandle` via reflection |
| RDD_KEY_81 | Allman-brace render-loop infinite loop when `)`/`{` are already adjacent (`CppSpecificRule.java`/`JavaSpecificRule.java`) |
| RDD_KEY_82 | Phase ordering reversed -- `Main.java`/`README.md`/dogfood test deferred until after Phase 2 |
| RDD_KEY_83 | `JavaModifierPriority` column order for `abstract`/`sealed`/`non-sealed`/`final`/`volatile` -- declaration-kind-specific orderings merged into one map |
| RDD_KEY_84 | `record` named-construct detection through component list / `implements` clause / compact constructor |
| RDD_KEY_85 | C++ concepts/`requires` clause implementation in `CppSpecificRule.java` |
| RDD_KEY_86 | `MiscRule.java` call/declaration line-breaking architecture -- option 2 must bypass `parseSignature`, option 1 reuses it + new `renderDropped` |
| RDD_KEY_87 | `MiscRule.enforceCallLineBreaking` implementation scope decisions (nesting, comment bail-out, call-vs-declaration classification, new preserve-groups grid) + `collapseTokensToOneLine` bugfix |
| RDD_KEY_88 | `Main.java` implementation (Step 1.5) -- CLI parsing, config resolution, indent-style temp-cache, server auto-connect/delegate, `--server`/`--stop`, output modes, exit codes |
| RDD_KEY_89 | `combined_inp.java` -- §15 consecutive-`//`-comment grouping, enum constant-list `;` separation, `throws`-clause function-body detection |
| RDD_KEY_90 | Task A (`JXM_CFMT_DIS`/`ENA`) -- rejected split-file-into-tmp-dirs approach in favor of in-memory token masking |
| RDD_KEY_167 | `JXM_CFMT_CFG` top-of-file placement semantics -- own separate comment required, "before first non-comment/non-blank token" not literal line 1 |
| RDD_KEY_168 | `in_file_config_*.hpp` fixture -- `header-guard-rename` untestable via this harness (guard name derives from invocation path, `_inp`/`_out` always differ); swapped for `format-macros=off`, which also proves override of the `test` target's own `FORMAT_MACROS=on` env var |
| RDD_KEY_169 | range-v3 item 20 bug (a) RESOLVED -- `BlockStructureRule.enforceKAndRBraceStyle` glued a named construct's `{` onto a preceding bare `#endif` line, which a later retokenize then swallowed whole into the `#endif` PREPROCESSOR token, permanently dropping that brace from every downstream scope-depth/frame-stack pass and desyncing both the closing-comment indentation and (as a downstream side effect, not a separate bug) angle-bracket classification; fixed by skipping the K&R glue when the preceding real token is a PREPROCESSOR directive |
| RDD_KEY_170 | microsoft/proxy dogfood: 3 bugs in `CppSpecificRule.enforceRequiresClausePlacement` -- (a)/(b) baseIndent/fit-check derived from the trailing `requires` clause's unstable-across-passes closing-paren line instead of the parameter list's own opening-paren line (with chained-specifier unwinding for `noexcept(...)`); (c) a preprocessor directive inside the clause's own constraint expression got spliced mid-line, producing invalid C++ -- fixed by leaving any clause containing a `PREPROCESSOR` token untouched |
| RDD_KEY_171 | Local `src/jxm` dogfood: `TokenizerCore.reclassifyAngleBrackets` had no case for a literal `>>>` token (triple-nested generics), only `>`/`>>` -- round2 re-lexed round1's tight `>>>` as one token, fell through to the generic-safe-token fallback, invalidated the whole open-`<` stack, spaced the generics out. Fixed by adding an explicit `>>>` case generalizing the existing `>>` split to 3 nesting levels plus its 2/1-leftover-`>` partial-match variants. |
| RDD_KEY_172 | Local `src/jxm` dogfood: `JavaSpecificRule.isSingleLineBody`'s fits-under-limit prediction omitted the line's leading indentation and any trailing same-line `//` comment, both of which `MiscRule.enforceCallLineBreaking`'s own fit-check counts -- caused a K&R-vs-Allman flip-flop when indent+comment alone pushed an otherwise-fitting one-liner over the limit. Fixed by including both, whitespace-collapsed the same way `collapseToOneLine` does. |
| RDD_KEY_178 | Local `src/jxm` dogfood: two related bugs in `MiscRule`'s STYLE.md §8 multi-line parameter-list renderer (`render` and its near-duplicate multi-line-declaration renderer) around a standalone `//` banner comment used as a section divider between parameter groups (found in `SWDFlashLoader.Specifier`'s constructor and `STM32QSPI.newQSPICmd`). (1) A leading `//` line comment was inlined as a text prefix on the same physical output line as the following parameter's type+name, silently swallowing that parameter's declaration (and, once re-tokenized, the next one too) into the comment -- compile-breaking. Fixed by emitting a leading `//` line comment on its own separate line; a self-terminating `/* ... */` block comment still inlines as before. (2) The shared column-width used to align type/name (`typeColWidth`, from `maxTypeLen`) is computed only over params with no leading comment at all, so a param preceded by a line comment -- excluded from that computation -- could have a `typeText` as long as or longer than `typeColWidth`, making `padRight` a no-op and merging type+name with zero space (`InstModeinstMode`) on the next reformat. Fixed by never padding to less than `typeText.length() + 1`. |
| RDD_KEY_201 | `alignCommentSeparators` false-positive narrowing attempt, reverted -- tried a fixed allowlist (`—:–|~`) instead of RDD_KEY_50's "any non-alphanumeric char"; backfired (157/162, was 162/162) by disabling the old rule's incidental 2+-candidate disqualifier that had been protecting `:`-heavy prose comments; fully reverted, 162/162 restored; design question remains open (see "Known Gaps -- Open"). |
| RDD_KEY_202 | `alignCommentSeparators` false-positive -- FIXED via `MiscRuleCore.looksCodeLike`, a structural code-likeness check (word count/length/stopword list) on each candidate line's label/rest; see "Known Gaps -- Fixed" for full detail. |
| RDD_KEY_203 | `GetterSetterRuleCurly.parseOneLinerMember`'s breakable-width pre-check gated only on `isDefinition`, `filesystem.hpp` `recursive_directory_iterator` assignment-alignment shape -- FIXED via a new `hasBreakableParams` check alongside the existing `hasBreakableCall` check; see "Known Gaps -- Fixed" for full detail. |
| RDD_KEY_222 | `MiscRuleCore.computeLineCommentGroups`'s §15 consecutive-`//`-comment grouping (RDD_KEY_89) capitalized every group member's line independently, wrongly capitalizing continuation lines of a genuine multi-line `//` comment (unlike the `/* */` path, which correctly capitalizes only content line 0 via `stripSoleTrailingPeriodAcrossLines` + one `capitalizeFirstLetter` call). FIXED by capitalizing only `contents.get(0)`, matching the block-comment path. Applies to all Curly-family languages (`Lang.isCurly` = C/C++/Java/Kotlin/JS/TS); Python3/data-formats/XML/HTML5 have no §15 pass, so unaffected. Surfaced a second latent bug in the same grouping logic: `nextCommentChainLinkIfAdjacent` wrongly chained a trailing end-of-line comment onto the next line's standalone `//` comment as one prose block (found via `test/js_comments_inp.js` and `test/ts_comments_inp.ts` regressing after the first fix). FIXED with a new `isStandaloneCommentLine` helper (true iff alone on its line back to `NEWLINE`/start-of-tokens); `nextCommentChainLinkIfAdjacent` now returns -1 for a non-standalone token, so a trailing comment is still capitalized/period-stripped alone (size-1 group) but never chains onto the next line. 26 pre-existing `*_out` fixtures (Java/C/C++/Kotlin/TS: `real_code_regressions_*`, `c_cpp_decl_gaps`, `java_format_toggle`, `java_preprocessor_method`, `js_comments`, `ts_comments`) had the old buggy per-line capitalization hand-authored as "expected"; updated to match. `make test`: 219/219 forward + idempotency after both fixes, zero unexpected diffs. |
| RDD_KEY_225 | `jenkinsci/jenkins` `hudson/PluginManager.java` (`doPluginsSearch`'s `sitePlugins` stream-chain declaration) -- root cause was `ScopePipelineCurly.applyDeclarationsPass` -> `DeclarationAlignmentRuleCore.renderInitTokens` (runs before `MiscRuleCurly.enforceCallLineBreaking`) unconditionally flattening a declaration's entire initializer, including an embedded multi-statement lambda body, onto one physical line with no line-length check, producing a real ~1992-char line no later pass could re-wrap. FIXED via a new pre-flight bail-out in `DeclarationAlignmentRuleCurly.parseDeclaration` (new `rawSliceBetweenUnfiltered`/`containsMultilineBraceBody` helpers): if any brace pair in the initializer originally spanned more than one physical source line, leave the statement untouched. See "Known Gaps -- Fixed" for full detail, including the 3 pre-existing fixtures (`real_code_regressions_57`/`129`/`130`) updated to match. New fixture `real_code_regressions_176`. `make test`: 224/224 -> 225/225 forward + idempotency, zero regressions. |
| RDD_KEY_231 | User-improved `java_combined_inp.java` fixture -- 2 bugs, both cross-language (C/C++/Java/JS/TS): (a) `DeclarationAlignmentRuleCore.needsSpaceBetween` had no unary-vs-binary `+`/`-` awareness (`int aaa = +1;` -> `= + 1`), the exact C/C++/Java/JS-TS gap `KotlinDeclarationAlignmentRule`'s own earlier Kotlin-scoped fix had flagged as still-open in its javadoc -- fixed by promoting `isUnaryMinusOperand` to `DeclarationAlignmentRuleCore` (shared by `renderTokens` and `renderInitTokens`; Kotlin's now-redundant override removed). (b) Independent: `ScopePipelineCurly.applyDeclarationsPass`'s idempotency-strip heuristic couldn't distinguish a genuine re-format's self-padding from a first-time format whose true indent coincidentally exceeded the modifier-column pad width, silently eating an indent level -- fixed by only accepting the strip when its result is already indentWidth-aligned. `make test`: 228/228, zero regressions. |
| RDD_KEY_238 | Self-hosting dogfood bug: `DeclarationAlignmentRuleCore.isTightToken`/`MiscRuleCore.isTightToken`'s `Token.isRepOp(t, '&')` tight-join rule (C/C++ pointer/reference declarator sigil, already gated off for Kotlin/JS/TS) was never gated off for Java, even though Java has no such construct -- wrongly collapsed a Java logical-AND's leading space (`x >= 2&& y`) wherever an expression rendered through either shared join point, found via `XmlSpecificRule.java`'s own `shouldFosterParent`-adjacent `fostered` declaration. FIXED by adding `!lang.isJava` around the `&`-half of both conditions (the `*`-half untouched, no observed bug). `||` confirmed unaffected. `make test`: 244/244, unchanged. Full self-format dogfood-and-adopt re-run: 168 real occurrences fixed in `src/`, 0 in `tools/*` (already clean, `java_content_diff.sh`/`python_content_diff.sh` clean on every file), 0 `||` occurrences anywhere. Surfaced an unrelated, out-of-scope gap, now fixed via `RDD_KEY_239`: 4 `&&`-missing-space occurrences remained in `src/` in a plain-assignment (non-declaration) ternary shape the formatter's general statement rewrite never touched at all. |
| RDD_KEY_239 | `RDD_KEY_238`'s follow-up gap: general (non-declaration) expression-statement operator spacing was never re-derived (plain-assignment RHS rendered via pure `joinVerbatim`). 3 confirmed real sub-bugs fixed: missing space before `&&`, extra space after unary `!`, `- 1` vs `-1`. See `RDD_KEY_239`'s full entry in `RDD_LOG.md` for complete detail, including 3 regressions found/fixed along the way (C pointer-dereference, Java cast spacing, binary-`*`-after-`]`) and one unrelated pre-existing `]`-then-`(` tight-join gap also fixed. Also centralized duplicated `isUnaryMinusOperand` as `Token.isUnaryMinusOperand`. `make test`: 244/244, unchanged. 21 files adopted back in `src/` (`tools/*` unaffected). |
| RDD_KEY_251 | Seventh session, RESOLVED the nested-switch-in-switch failure mode of "Non-idempotent switch-case re-indent on internally-inconsistent generated source" -- `SwitchRule.applyNonInlineCaseIndent` now derives each case-body line's absolute target indent from its own brace-nesting depth (`applyDepthDerivedBodyIndent`, replacing the old single-relative-delta `shiftLines` body-shift), with a nested switch's entire token span treated as opaque (fully owned by its own independent `SwitchBlock` pass, never touched by the outer switch's depth-derived scan) rather than two independent recomputations disagreeing forever. A second approach (thread one shared depth accumulator recursively through nested switches) was also implemented and rejected -- it hung on even the minimal repro because the nested switch's own independent pass still ran and disagreed with the recursive writes; a correct version of that approach would need much more invasive engine changes. See "Known Gaps -- Fixed" for full detail, `real_code_regressions_181` fixture, and `RDD_KEY_251` in `RDD_LOG.md` for the complete two-approach writeup. `make test`: 247/247 -> 248/248 forward + idempotency, zero regressions. |

---

## Open Questions

- **range-v3 real-code-testing item 20, bug (a): RESOLVED.** Idempotency divergence in
  `utility/any.hpp`, `iterator/common_iterator.hpp`, `meta.hpp`. Root cause/fix: entry (20) in
  "Finished dogfood / real-code testing" below. Full narrative: `RDD_KEY_169` in `RDD_LOG.md`.

- **[Shared with STATE_JS_TS.md family] Java assignment-alignment trailing-comment padding vs.
  `enforceCallLineBreaking` ordering — 2026-08-08 investigation session, no code change landed
  (reverted after regression found).** Same architectural bug already tracked above (the
  `ScopePipelineCurly.processScope` outer-first-then-recurse double-pass entry) and the same
  concrete mechanism RDD_KEY_248/RDD_KEY_270 already fixed for JS/TS via
  `FormatterCurly.format`'s `if(lang.isJs || lang.isTs) scopePipeline.
  reapplyClosingBraceAndDeclarationsPass(text)` narrow re-run — but this session found the
  identical bug shape occurring for **Java**, not JS/TS. Found via the formatter's own
  self-format dogfood (`src/com/jxmake/formatter/rules/PowerShellSpecificRule.java`'s `format()`
  method): a run of `s = someCall(s); // §N.n comment` assignment statements forms an
  `applyAssignmentsPass` alignment group; when one sibling's call name is long enough that
  `enforceCallLineBreaking` (which runs AFTER `applyAssignmentsPass`, inside `processScope`)
  wraps that one call across lines, every *other* sibling's trailing-comment column was already
  padded (during `applyAssignmentsPass`) against the pre-wrap single-line width of the widest
  member — round1 keeps that stale wide padding, round2 (fed round1's own now-multi-line input)
  recomputes the group differently and collapses the padding to one space — non-idempotent.
  Minimal repro (not committed, see below): a small class with 7 `s = applyX(s); // comment`
  lines, one calling a deliberately long method name so `enforceCallLineBreaking` wraps only that
  one; `diff -ru` between round1 and round2 output confirmed the exact padding-then-collapse
  shape byte-for-byte matching the `PowerShellSpecificRule.java` symptom.

  **Attempted fix 1 (reverted): widen `FormatterCurly.format`'s existing JS/TS-only
  `if(lang.isJs || lang.isTs)` gate around `reapplyClosingBraceAndDeclarationsPass` to
  `lang.isCurly`** (run for every curly-family language, on the theory the mechanism is already
  proven language-agnostic since RDD_KEY_270 added `applyAssignmentsPass` to it for JS/TS). Broke
  8 pre-existing fixtures (`hpp_core`, `cpp_core`, `hpp_combined`, `cpp_combined`, `java_core`,
  `java_combined`, `cpp26_reflection`, `real_code_regressions_58`) — re-running
  `applyOversizedAggregateInitClosingBracePass` a second time in the same round for C/C++/Java
  re-collapsed already-correct, unrelated output (exactly the failure mode RDD_KEY_246's own
  doc comment warns "Attempt 2" hit: "re-running unrelated passes a second time in the same round
  risks silently re-collapsing already-correct output").

  **Attempted fix 2 (reverted): narrow the gate to `lang.isJs || lang.isTs || lang.isJava` only,
  and additionally skip `applyOversizedAggregateInitClosingBracePass` specifically for Java**
  (Java has no oversized-aggregate-init-closing-brace construct that pass is meant to fix, so
  skipping it for Java looked lossless). This narrowed the regression from 8 files to 3
  (`java_core`, `java_combined`, `real_code_regressions_58`), all the same symptom: Java's
  enum-constant-list `;`-separation (`JavaSpecificRule.separateEnumConstantListTerminator`,
  RDD_KEY_89 -- detaches the constant-list-terminating `;` onto its own line with a blank line
  above it) got re-collapsed back onto the constant list (`ACTIVE, INACTIVE, PENDING\n\n;` →
  `ACTIVE, INACTIVE, PENDING;`) by the re-run. Since `applyOversizedAggregateInitClosingBracePass`
  was already excluded, the culprit re-run pass is `applyDeclarationsPass` and/or
  `applyAssignmentsPass` themselves re-collapsing the separated `;` line back — not investigated
  further to find which one or why (would need isolating each pass individually inside the
  re-run, which starts to approach the same "re-running unrelated passes regresses unrelated
  output" risk class RDD_KEY_246 already flagged, now for Java's own enum-separation pass rather
  than JS/TS's dangling-`}` shape).

  **Disposition:** both attempts fully reverted (`FormatterCurly.java`/`ScopePipelineCurly.java`
  back to their pre-session state, confirmed via `git diff` showing no residual changes to either
  file); `make test` re-confirmed clean at the pre-existing baseline (261/261 forward +
  idempotency) after the revert. No fixture added (no fix landed). Left OPEN, same as the sibling
  JS/TS-family entry above — a future session picking this up should either (a) find exactly
  which of `applyDeclarationsPass`/`applyAssignmentsPass` re-collapses the enum `;` separator
  when re-run a second time for Java and make that specific behavior idempotent/order-safe rather
  than skipping the whole pass, or (b) find a narrower re-run than the shared
  `reapplyClosingBraceAndDeclarationsPass` three-pass bundle that only re-derives
  `applyAssignmentsPass`'s trailing-comment/`=`-column width for Java without touching whatever
  re-collapses the enum separator.

  **TODO (2026-08-08, later same day) — instance worked around, root cause still open.** The
  concrete trigger instance in `rules/PowerShellSpecificRule.java` was NOT fixed at the formatter-
  source level (both attempts above remain reverted) — instead it was sidestepped by manually
  inserting a blank line between each `s = applyX(s); // comment` statement in that file's
  `format()` method, which breaks `applyAssignmentsPass`'s alignment-group membership per
  RDD_KEY_254's "blank line breaks the group" rule, so the group that was triggering the stale-
  padding-then-collapse behavior no longer forms at all. This is a source-layout workaround in one
  call site, not a fix to the formatter — the underlying `applyAssignmentsPass`-vs-
  `enforceCallLineBreaking` ordering bug documented above is unchanged and still applies to any
  other curly-family file (C/C++/Java) with a similar run of `x = call(x); // comment` assignment
  statements where one sibling's call is long enough to get wrapped by `enforceCallLineBreaking`.
  A future session should still pursue (a) or (b) above for a real fix; until then, be aware this
  bug can resurface anywhere in the codebase (including future edits to
  `PowerShellSpecificRule.java` itself, if the blank lines are ever removed) and is not something
  `make test`'s existing 261-fixture baseline will catch, since no fixture reproducing it has been
  registered (the repro used for investigation was a scratch file, not committed to `test/`).

  **Repro fixture (blank-line-separated, non-buggy shape) now committed**: `test/
  java_combined_inp.java`'s `evaluateAt2` method (end of the file, after the `aaa`/`b`/`ccccc`
  declarations) has the same 7-line `s = applyX(s); // §N.n comment` call chain from
  `PowerShellSpecificRule.java`, blank-line-separated into 4 sub-groups exactly like the
  post-workaround file — this is the *working* shape (`java_combined_out.java` shows it formats
  stably, no wrap). It's a convenient anchor for a future session: to reproduce the actual bug,
  remove the blank lines between the 7 statements (re-forming one contiguous `applyAssignmentsPass`
  group) and re-run round1/round2 — this should still diverge (stale comment-column padding vs. two
  lines getting wrapped by `enforceCallLineBreaking`, per the shape recorded above) until (a)/(b) or
  the idea below lands.

  **New fix idea (2026-08-08, not yet attempted) — make `enforceCallLineBreaking`'s fits-check
  ignore alignment padding.** Distinct from attempts 1/2 above (which both tried *re-running*
  `applyAssignmentsPass` after wrapping); this instead targets the wrap *decision* itself. Right
  now `enforceCallLineBreaking` measures a line's length using whatever comment-column padding
  `applyAssignmentsPass` already inserted — so the wrap decision is a function of alignment width,
  which itself depends on group membership, which shifts once something wraps. If the fits-check
  instead measured the line as if the comment gap were a single canonical space (i.e. ignoring
  alignment padding entirely, the same class of fix as RDD_KEY_172's `isSingleLineBody` fits-check
  correction — there it was *missing* indent+comment width, here it's *including a variable* padding
  width it shouldn't), the wrap/no-wrap decision would be stable across rounds by construction,
  since it would no longer depend on a value that the wrap itself invalidates. This is a narrower,
  more surgical change than attempts 1/2 (a measurement change, not a pass re-run) and might avoid
  their specific regressions (aggregate-init closing braces, Java enum `;` separation), but it still
  touches a method shared by every curly-family language (~15 languages, 260+ fixtures via the
  shared curly pipeline) and needs verification that no other pass or fixture actually relies on
  `enforceCallLineBreaking` seeing the true padded width for a correct wrap (e.g. a line that's
  genuinely too wide for the reader only once alignment padding is included). Investigate this idea
  first, in isolation, before touching `FormatterCurly.java`'s re-run gate again.

  **2026-08-09 session — approach 1 (the "new fix idea" above) tried, reverted; insufficient by
  itself.** Implemented: `MiscRuleCurly.flushCollapseGap` (used by `collapseToOneLine`, which
  backs `enforceCallLineBreaking`'s own whole-line fits-check at line ~1391) gained a special
  case collapsing any gap immediately before a trailing `COMMENT_LINE`/`COMMENT_BLOCK` token to a
  single canonical space, even when it's pure horizontal whitespace with no `NEWLINE` — leaving
  the existing verbatim-preserve behavior untouched for mid-statement alignment padding (e.g. a
  declaration grid's `=`-column, the reason that verbatim-preserve rule exists in the first
  place per its own doc comment, from the vuejs/core `scripts/release.js` bug). `make test`:
  261/261 forward + idempotency, zero regressions — the change alone is safe. **However, a fresh
  minimal repro (7-line `s = applyX(s); // comment` chain, one sibling given a deliberately long
  method name so `enforceCallLineBreaking` wraps only that one) still reproduced the bug
  byte-for-byte after this change**: round1 kept wide stale padding on the non-wrapped siblings
  (`s = applyX(s);                                                                   // §1
  comment`), round2 collapsed it to one space, non-idempotent. Root cause: this fix only changes
  what `enforceCallLineBreaking` measures when deciding whether *its own* line needs to wrap — it
  does not touch `ScopePipelineCurly.applyAssignmentsPass`, which is the pass that actually
  computes and commits each sibling's trailing-comment-column padding, and which never re-runs or
  re-derives that padding after `enforceCallLineBreaking` (a later pass in the same round) changes
  one sibling's line shape. Stabilizing the wrap *decision* was necessary but not sufficient; the
  stale-padding-on-siblings problem lives entirely in a different pass this approach never
  touched. **Reverted** (`git checkout -- src/com/jxmake/formatter/rules/MiscRuleCurly.java`,
  confirmed via `git diff` showing no residual change to that file).

  Approaches (a)/(b) from the disposition note above (find which pass re-collapses the enum `;`
  separator when re-run for Java; or find a narrower re-run than the shared three-pass bundle)
  were NOT attempted this session — given this session's evidence that the real bug lives in
  `applyAssignmentsPass` not being re-derived post-wrap, a future session's most promising next
  step is likely a fourth idea, not yet attempted: make `applyAssignmentsPass` itself run (or its
  comment-column-width computation alone re-run) *after* `enforceCallLineBreaking` in
  `ScopePipelineCurly.processScope`'s pass order for the specific case of a group containing a
  member `enforceCallLineBreaking` just wrapped — i.e. treat the group's true single-line width as
  unknowable until call-wrapping has already been decided, rather than trying to make the
  fits-check ignore a value computed too early. Left OPEN; documented as an accepted gap in
  `README.md`'s Known Limitations (curly-brace family, item 5) per this session's disposition —
  matches the pattern of the two already-reverted attempts, no fix landed.

- **NOT REPRODUCED, 2026-08-03 — closed as unconfirmed/stale, not conflated with the above.**
  Ran every registered `test/*_out.cpp`/`test/*_out.hpp` fixture (37 files) through both
  `g++ -std=c++20 -fsyntax-only` and `clang++ -std=c++23 -fsyntax-only` (tools (2)/(3), incl.
  `-stdlib=libc++`) — zero mismatches; every fixture passing gcc also passes clang. The
  `-stdlib=libc++`-only failures found (standalone header-fragment fixtures missing full context,
  e.g. `platform.hpp`) are expected snippet noise, not a version-mismatch signal — none pass gcc
  either. No other finished-dogfood entry records a clang/gcc discrepancy (item 22
  `microsoft/proxy` used `clang++ -std=c++23 -stdlib=libc++` and matched gcc cleanly). The
  adjacent item (20) `range-v3` corpus (likely origin of this report) was only ever verified
  against gcc, never clang, and its `/tmp` checkout is now empty (unusable for re-verification,
  no filenames recorded). Closed as not reproducible against the current fixture set; re-open
  with concrete filenames if it resurfaces against a live corpus.

---

## Config Keys and Defaults

See `STATE_COMMON.md` → **Config Keys and Defaults** (moved there since the
table is shared across all four supported languages, not C/C++/Java-specific).

---

## Java File Header

Every `.java` source file must begin with this copyright block, before the `package` declaration:

```java
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */
```

## Java Coding Convention — `final` Locals and Parameters

Mark every local variable and method/constructor parameter `final` whenever it is
never reassigned after its initial assignment (i.e., whenever the compiler would
accept `final` there). Applies to all `.java` files under `src/`.

---

## Checklist — Phase 3

**Step 1 — Deterministic extensions (complete):**

**Step 1.5 — Dogfood checkpoint (in progress):**

**Critical rules for this step:**
- User may name a specific `*_inp.*` file to run next — run only that one unless told to run
  all remaining; do not assume sequential order.
- Run test files one at a time, including the self-dogfood pass (formatting the formatter's own
  source). On mismatch vs. `*_out` (or unexpected changes), **stop and ask the user** before
  fixing — the mismatch may be a hand-authored error in the expectation, not a formatter bug.
- After each file test — pass or fail — update the checklist item inline with `(PASS)`/`(FAIL)`/
  `(SKIP)` and commit immediately (no batching).
- Never remove `[x]`/`(PASS)` entries — a later fix could regress a previously-passing file, and
  the user may ask to re-run any entry at any time.
- Apply STATE_COMMON.md's "evidence over reasoning" rule strictly here to limit quota usage and
  avoid regressing `(PASS)` tests/prior fixes.

`Main.java` standalone-mode cache note: `IndentationDetector` results are cached at
`/tmp/jxmake-code-formatter-indent-<sha256-of-boundary-dir>.cache`, content = detected style + `\n`
+ boundary dir `lastModified` epoch ms; invalidated automatically on an mtime mismatch
(RDD_KEY_88).

- [x] CLI arg parsing (`--server`, `--stop`, `--standalone`, `--diff`, `--check`,
      `--out DIR`, `--port N`, file paths); unknown flags / bad usage → exit 2 (RDD_KEY_88)
- [x] `--lang c|cpp|java` (2026-07-06): explicit language override for files whose
      extension `inferLanguage` can't recognize; one flag per invocation (no per-file
      override), validated against exactly `c`/`cpp`/`java` (exit 2 otherwise), threaded
      through `processFile` ahead of the extension-based `inferLanguage` fallback;
      `--server`/`--stop` reject `--lang`. The `/format` HTTP endpoint already accepted an
      optional `lang` query param taking priority over its own path-extension guess
      (`Main.delegateToServer` already sent it), so no protocol change was needed — only
      server-side validation added (`ServerMode.FormatHandler` now 400s on an unrecognized
      `lang`). `README.md` updated. `make test` 25/25, no regressions.
- [x] Four output modes: in-place (default), `--diff` (self-written unified diff,
      single hunk with clamped context), `--check`, `--out DIR` (RDD_KEY_88)
- [x] Exit codes: 0 = success/no changes, 1 = would-change (`--check`) or formatting
      error, 2 = usage error (RDD_KEY_88)
- [x] `README.md` update for Phase 1 + Phase 2 (added `auto` to `indent-style`
      comment; all other Phase 1+2 items already present)

All file-pair tests below PASS (forward + idempotency), zero known regressions. Full
bug-by-bug root-cause narratives have been compacted out of this file — available via
`git log`/`git show` on the commits noted per entry.

- [x] `h_core` / `c_core` / `hpp_core` (PASS, no bugs)
- [x] `cpp_core` (PASS) — 6 bugs: named-construct/attribute/modifier detection, `::`-qualified
  spacing, constructor Allman close-paren, getter/setter padding, trailing-return-type,
  if/else-if chain collapsing.
- [x] `java_core` (PASS) — 8 bugs: header-spacing, `this`/`super` as LHS, stale closing-comment,
  `throws` Allman conversion, inline-switch padding, `catch`/`finally` placement, `@Annotation`
  skip.
- [x] `cpp_modern` (PASS) — 11 bugs: comment no-capitalize set, named-construct one-liners,
  `template`/`requires` signatures, operator-overload detection, coroutine promise_type
  grouping, brace-init/structured-binding spacing, tight cast-template brackets, namespace
  closing-comment chain, + an idempotency fix (column padding double-counted as indentation).
- [x] `java_modern` (PASS) — 5 bugs: empty named-construct bodies, one-liner-call getter/setter
  rejection, multi-statement one-liners left unsplit, RDD_KEY_75 adjacency heuristic removed,
  blank line before leading comment.
- [x] `combined.h` (PASS) — 3 bugs: `format-macros` alignment, `extern "C"` fixture correction,
  enum-alias closing comment + `#if`-guard depth. Committed `efeb6df`.
- [x] `combined.c` (PASS) — 4 bugs: struct member indentation strip safety, flat
  brace-aggregate initializers + C-style cast rejoining, parameter inline comments preserved,
  switch closing-comment idempotency.
- [x] `combined.hpp` (PASS) — 2 bugs: constructor/destructor/operator-overload one-liner
  exclusion, `template<...>` prefix recognition; 1 not feasible (mid-word-dot vs. sentence-period
  needs semantic understanding — Tier-3 AI-assist candidate in `STATE_AI.md`, fixture hand-edited).
- [x] `combined.cpp` (PASS) — 4 bugs: destructor `~` vs. return type, class-template member
  tight brackets + layout, structured-binding misparse, trailing comment duplication on
  group-gap trim.
- [x] `combined.java` (PASS) — 5 bugs: multi-line `//` prose period-stripping, enum
  constant-list `;` detached, blank line before final `return` in a `throws`-clause method,
  `InFileConfig`'s `JXM_CFMT_CFG` directive regex unanchored (false-triggered on directive syntax
  merely mentioned in comment prose); `DeclarationAlignmentRuleCurly.parseDeclaration`'s
  bitfield-colon scan not stopping at first top-level `=` (ternary `:` inside initializer
  misrouted as C++ bitfield → e.g. `Foo.BAR` → `Foo. BAR`); found via GRU-trainer dogfood,
  fixture hand-edited by user. Follow-up: anchored regex still matched directive-looking text
  starting a line INSIDE an already-open block comment (`/* */`/`<!-- -->`) — violates
  RDD_KEY_167 ("never recognized if pasted inside the interior of another comment"). Fixed by
  making `DIRECTIVE` a single sequential-scan pattern with plain-comment fallback per style so
  `Matcher.find()` non-overlapping resume skips block-comment interiors.
- [x] `c_comments` (PASS) — 6 bugs: mid-param `//` comment reattachment (brace-depth desync),
  compound-assignment misparse, one-param-per-line padding, `hasCommentBefore` group-break
  guard, last-param comment alignment, `#define` trailing-comment capitalization. 1
  fixture-only correction.
- [x] `cpp_comments` (PASS) — 5 bugs: forward-declaration comment isolation, stale-closing-
  comment guard narrowing, namespace-body/template-argument/keyword spacing, `requires`-clause
  signature pull, blank line after trailing-comment member, structured-binding/template-argument
  comment spacing.
- [x] `java_comments` (PASS) — 4 bugs: multi-line param comment reattachment, per-language
  no-capitalize keyword sets, switch-case comment blank-line preservation, flat-aggregate
  per-element comments untouched.

**If any file-pair test above shows a mismatch: stop, report the full diff to the user, and
wait for instruction. Do not fix either the formatter or the `*_out` file without explicit
user direction — `*_out` files are hand-authored and may themselves contain errors.**

**After all file-pair tests above pass (or are resolved — ask the user first):**
- [x] Dogfood self-format pass: run formatter on all `src/**/*.java`, write
      to `target/dogfood-src/`
- [x] Dogfood self-format compile: `javac` the `target/dogfood-src/` tree;
      must compile with zero errors — first run surfaced a real compile-breaking bug (see
      "Other findings outside the candidate list" below), now fixed; verified clean compile
      after the fix.
- [~] Dogfood self-format idempotency / declaration count: superseded by the real-code
      testing approach below, which found and fixed the actual bugs underlying this failure.
      Not re-run standalone against `target/dogfood-src/` since; if revisited, expect it to be
      much closer to passing given the pass-ordering fix, but there may be other Java-only
      convergence bugs the C++ testing below wouldn't have exercised.

**Real-code testing (pivoted from synthetic dogfooding — found bugs faster):** see
STATE_COMMON.md's "Real-code testing methodology" for the repeatable round1/round2/compile
recipe and fixture-registration convention. Full bug-by-bug root-cause narratives for
completed candidates have been compacted out of this file into the "Finished" list below —
still available via `git log`/`git show` on the noted commits/fixtures.

**Tools/compiler used**
(1) `gcc -std=gnu99 -fsyntax-only <file>.c` (used for `tongsuo-mini`)
(2) `g++ -std=c++20 -fsyntax-only <file>` — usually `/opt/gcc-12.2.0/bin/g++`; PEGTL,
    stdexec, and mp11 additionally need `LD_LIBRARY_PATH=/opt/isl-0.16.1/lib` with this
    toolchain
(3) `clang++ -std=c++23 -fsyntax-only <file>.cpp` (with/without `-stdlib=libc++`) at
    `~/xsdk/clang22/LLVM-22.1.8-Linux-X64/bin/clang++` — pipe stderr through
    `grep -v 'no version information available'` to filter a harmless libstdc++
    symbol-versioning warning (not a compile error); `/opt/glibc-2.41/` is available if a
    genuine glibc-mismatch/patchelf issue is ever hit with some other prebuilt binary
(4) `javac` — installs used so far: `/opt/openjdk-25_linux-x64_bin`,
    `/opt/openjdk-21_linux-x64_bin/jdk-21/bin/javac -d ... -cp . --release 8` (matches a
    project's own JDK8-source/JDK21-`javac` Makefile convention)
(5) `pcpp-java-1.30.jar` (JxMake's own C-preprocessor-for-Java tool) —
    `java -jar pcpp-java-1.30.jar <input> -o <output>`; compare token streams before/after
    format with `#line` directives stripped first (they legitimately shift with line-count
    changes); plain `gcc -E`/`cpp` does NOT work as a substitute (hard-errors on real `##`
    token-pasting tricks)
(6) `java_syntax_check` — AST-based syntax-only checker. Used when a full javac is not wanted/needed
    (dependency problem) — catches parse errors only, weaker confidence than (4) (no semantic/type
    checking). Run (see STATE_COMMON.md's "Verifier toolchain paths" for the `$JDK` env setup and
    build command):
```bash
"$JDK/bin/java" java_syntax_check <file.java> [file2.java ...]
```

**Dogfood Output Validation — `java_content_diff`.** A content-preservation
checker for Java, complementing `java_syntax_check` (which only proves "still
parses", same `css_content_diff.py`/`xml_content_diff.py` precedent from
`STATE_DATA_FORMATS.md`). Reuses `java_syntax_check`'s `JavacTask.parse()`
infrastructure (no new dependency) but keeps the `CompilationUnitTree`
instead of only scanning diagnostics. Since this formatter *intentionally*
reorders/transforms some Java content (`java-import-order` sorting,
declaration-alignment whitespace, `normalize-comment-start-case`), a naive
text/token diff would false-positive on all of that, so the comparison is
split by content family:
- **imports** — compared as a multiset (sorted qualified-identifier
  strings, `static` flag included) since reordering here is legitimate.
- **package declaration + every top-level type declaration** — compared
  **in original relative order**, each via javac's own pretty-printer
  (`Tree.toString()`), whitespace-normalized. The pretty-printer encodes
  structure/identifiers/literal values but not original whitespace or
  comments, so pure reindentation/alignment-padding differences canonicalize
  to identical text while a dropped/added declaration, renamed identifier,
  or changed literal value still shows up as a text difference.
- **comments** — extracted separately via a raw-text scan (skips
  string/char literals so a `//`/`/*` inside a literal is never
  mistaken for a comment start; the pretty-printer drops comments
  entirely), compared as a multiset, whitespace-normalized **and**
  lowercased — a case-only change is expected
  (`normalize-comment-start-case`) so it must not be flagged, but a
  dropped or otherwise-corrupted comment still is.

Exit 0 if content is preserved, 1 with a description of each mismatch
otherwise, 2 if either file fails to parse. Run (see STATE_COMMON.md's
"Verifier toolchain paths" for the `$JDK` env setup and build command):
```bash
"$JDK/bin/java" java_content_diff <original.java> <formatted.java>
```
Verified against a hand-crafted good pair (reindentation + import sort +
one comment recapitalization — passes clean) and two bad pairs, a dropped
statement (correctly flagged as "top-level declaration #0 structure/content
differs") and a corrupted comment (correctly flagged as a comment present in
one file's set but not the other's) — all three cases caught correctly.
Test fixtures kept in `/tmp` only (hand-crafted verification pairs, not
registered as permanent `test/` fixtures).

**Finished dogfood / real-code testing** (one line each; full narratives via `git log`/`git show`
on the noted commits/fixtures)
(1) `blake-madden/tinyexpr-plusplus` (C++20) — 3 bugs, `MiscRule` multi-line call/decl
    rendering + pass ordering. Verified (2). Fixture: `real_code_regressions_1`.
(2) RobotCoding `gui_frontend` (71 `.java`) — 4 pass-ordering idempotency bugs (`>>>`
    mistokenization, getter/setter padding order). Verified (4). Fixture: `real_code_regressions_2`.
(3) Self-dogfood (formatter's own `src/`, 20 files) — 1 pass-ordering bug in
    `MiscRule.parseAssignment`. Verified (4). Fixture: `real_code_regressions_3`.
(4) `martinus/nanobench` (`nanobench.h` as `.hpp`) — 2 bugs: raw-string-literal tokenizer gap,
    dropped `template<...>` on forward decl. Verified (2). Fixture: `real_code_regressions_4`.
(5) User-reported `} // while` indentation bug — `ScopePipeline` closing-brace gap not
    re-derived from depth. Verified (2). Fixture: `real_code_regressions_5`.
(6) Local `pcpp_java` tool (41 `.java`) — 2 idempotency bugs: `SwitchRule` inline-alignment
    overflow, one-liner raw-newline misfire. Verified (4). Fixtures: `_9`, `_10`.
(7) C17 `Tongsuo-Project/tongsuo-mini` (56 files) — 1 bug: unbounded flat aggregate-init line
    length, no `lineLengthLimit` check. Verified (1). Fixture: `real_code_regressions_11`.
(8) C++20 `serge-sans-paille/frozen` (44 `.hpp` + `catch.hpp`) — 10 bugs across
    `ScopePipeline`/`DeclarationAlignmentRule`/`TokenizerCore`/`CppSpecificRule` (parent-indent,
    struct depth, brace-init, getter padding, K&R/Allman flapping, backslash-continued
    preprocessor corruption, namespace indent fallback, ObjC/attribute mistokenization). Verified
    via idempotency (156-file tree clean). Fixtures: `_12`–`_16`.
(9) C++20 `fmtlib/fmt` — idempotent at default; **at real 2-space convention (`indent-size = 2`)
    found `SwitchRule.deriveUnit`'s hardcoded 4-space fallback** — the only known indent-size-2
    exception case in this codebase. No-op at default, no fixture.
(10) C++20 `taocpp/PEGTL` (355 `.hpp`) — 1 bug: `reclassifyAngleBrackets`'s `>>`-split
     duplicated a char via `retype()`. Verified (2). Fixture: `real_code_regressions_28`. Also a
     no-op found (`normalizeIndent` non-declaration rounding gap, invisible at default indent).
(11) C++17/20 `foonathan/lexy` (121 `.hpp`) — no bug found, idempotent at default. Verified (2)
     on all 9 examples.
(12) C++20 `NVIDIA/stdexec` (192 files) — 4 bugs across 3 sessions: requires-expression `}`
     misidentified as scope-close; `#if`/`#endif` guard dropped by `splitStatements`;
     `tryCollapse` absorbing text past a `//` comment; already-collapsed one-liner misparsed as
     declaration. Verified (2). Fixtures: `_34`, `_35`, `_36`.
(13) C++11 `boostorg/mp11` (34 `.hpp`) — no bug found, idempotent at default. Verified (2).
(14) C++23 `basvas-jkj/cpp_modules` (7 files) — no bug found, idempotent. Verified (3)
     (pre-existing environment failures only, identical before/after).
(15) `google/google-java-format` (84 `.java`) — 3 bugs: `ensureBlankLineInGap` splitting a
     trailing comment (fixture `_6`); `Doc.java` divergence resolved by the config-key wiring
     audit below (no code change); `applyArrowAlignment` joining arrow-case with no
     line-length check (fixture `_7`); `findNameBeforeParen` misparsing `case`/`default` arrow
     arms as one-liner members (fixture `_8`). Verified (4).
(16) MEDIUM `javaparser/javaparser` (1997 `.java` files, 7 modules) — 6 idempotency bugs found via
     full-tree round1/round2 + `make test`, all fixed: braceless `if (cond) throw/return ...`
     misparsed as one-liner getter/setter; comment's sole trailing `.` stripped w/o separating
     whitespace (`_54`); `else`/`catch`/`finally` force-reindent dropping real blank line before
     `}` (`_55`); `enforceCallLineBreaking` fits-check before `formatNonInlineSwitches` case-body
     reindent — ordering fix (`_56`); `isCStyleCastClose` missing control-flow exclusion, misread
     `if(node instanceof RecordPatternExpr)` as cast (`_57`); Java enum-constant-list merging into
     adjacent field alignment group + drifting indent (`_58`).

     1 gap ACCEPTED not fixed: `ASTParser.java` (JavaCC-generated, ~5500 lines) has one
     switch-case body with internally-inconsistent source indentation causing one non-idempotent
     re-indent — same root cause as "Known Gaps — Open" (1/1997 files). `javac` compile-check not
     run (gated on fully-clean idempotency); accepted Finished per user decision — see
     `README.md`'s "Known Limitations".
(17) HUGE `openrewrite/rewrite` — DONE. Full-tree forward pass (default config): 0 errors.
     Round1/round2 idempotency (original 6-cluster investigation): 34 differing files, 6 clusters,
     all now fixed:
     - Cluster 1 (~20 files, ANTLR-generated, incl. `tree/J.java`) — 2 sub-bugs, same root-cause
       shape (fits-check made before a later width-growing pass ran): (a) `isSingleLineBody`
       measured tab-indent via raw `String.length()` not expanded width, fixed via
       `expandedIndentWidth`; (b) `enforceInitializerBraceSpacing` ran after
       `enforceCallLineBreaking` had already decided not to wrap, fixed by pulling it earlier.
       Fixture: `real_code_regressions_128`.
     - Cluster 2 (8 files, dense lambda/if-else bodies) — `appendChainNewlineBeforeElse` only
       fired as a side effect of collapsing a *braced* if/else-if; an already-braceless body fed
       back on round1 left nothing to re-collapse, fusing the chain on round2. Fixed via
       `findBracelessStatementEnd`. Fixture: `_129`.
     - Cluster 3 (`AdaptiveRadixTreeTest.java`, pre-increment spacing) — prefix `++`/`--` before
       an identifier had no tight-join case in `needsSpaceBetween`/its duplicate (`++ i`). Fixed
       by adding the tight-join case to both. Fixture: `_130`.
     - Cluster 4 (`ReloadableJava*ParserVisitor.java` x5, trailing-comment column drift) —
       `parseAssignment`'s verbatim fallback kept embedded `NEWLINE`s in `valueTokens`, so
       `ColumnGrid` measured wrapped-call text via plain `String.length()` (non-idempotent column
       width). Fixed via `valueSpansMultipleLines` exclusion. Fixture: `_131`.
     - Cluster 6 (closing-brace indent drift on a still-K&R `else`/`catch`/`finally`) — fixed via
       `ScopePipelineCurly.findParentIndent`. Fixture: `_132`.
     - Cluster 5 (alignment-group padding collapse, `rewrite-kotlin/.../K.java`'s
       `ExpressionStatement.withType`) — root cause (NOT `GetterSetterRuleCurly`):
       `DeclarationAlignmentRuleCurly.parseDeclaration`'s function-pointer-declarator detection
       (`Type (*name)(params);`) misread `return (T)(cond ? a : b);` as a declaration, lacking a
       leading-keyword exclusion — merged into an adjacent alignment group, collapsing padding
       across rounds. Fixed via a new `STATEMENT_LEADING_KEYWORDS` guard at the
       function-pointer-detection call site. Verified: real `K.java` round1/round2
       byte-identical, `make test` 220/220 (up from 219/219). Fixture: `real_code_regressions_171`.
     `make test` after fixes: 220/220 forward + idempotency, zero regressions.

     **Full-tree round1/round2 re-run + syntax-check, 2026-08-09 (this was the deferred
     re-verification, now run).** Corpus re-cloned fresh (`git clone --depth 1
     https://github.com/openrewrite/rewrite` into `/tmp/rewrite` — the prior session's `/tmp/rewrite`
     checkout on disk was a stale/incomplete sparse-checkout skeleton, only 1 file/4.6M, not the real
     tree; a plain full clone was simplest per this session's task). Repo has grown since the
     original 3373-file count — fresh clone has 3510 `.java` files; used as the ground truth for this
     run. Batched per top-level module subdirectory (`for d in /tmp/rewrite/*/`) per
     `STATE_COMMON.md`'s batching guidance, `--out`/`--preserve-tree`/`--root`.

     Round1/round2 diff: 4 residual idempotency diffs found, all cosmetic (no invalid-syntax risk),
     left **open/undiagnosed** as newly-found Known Gaps (see below) rather than blind-fixed, given
     each matches an already-documented deep/risky architectural bug family in this file (indent-
     width-decided-before-a-later-pass-grows-it, alignment-padding-collapse, switch-arrow-brace
     pass-ordering) — same judgment call already exercised for the open `PowerShellSpecificRule.java`
     self-format bug in `STATE_COMMON.md`'s "Formatter self-formatting" section:
     - `rewrite-java-test/.../ModerneWebsiteExampleTest.java`: a switch-arrow braceless `if/else`
       body followed by the arm's own closing `}` on the same physical line
       (`else b.append(c); }`) gets its `}` pulled onto its own line only on round2.
     - `rewrite-kotlin/.../TabsAndIndentsVisitor.java` and `rewrite-yaml/.../YamlParser.java`: a
       wrapped call argument's closing-paren continuation line gains 4 extra indent spaces between
       round1 and round2 (indent-width-decided-before-a-later-pass-grows-it family).
     - `rewrite-python/.../Pep508RequirementTest.java`: a `List<String>` declaration's
       alignment-group padding (3 spaces) collapses to 1 space on round2 (alignment-padding-collapse
       family, same shape as the already-fixed Cluster 5 but a different trigger site).

     One transient, non-reproducible `NoClassDefFoundError: MiscRuleCore$SepMatch` crash was hit
     mid-batch on the first round2 attempt (JVM classloader/resource-pressure hiccup during the very
     large batch loop, not a formatter bug) — confirmed non-reproducible by re-running just the
     affected subdirectory (`rewrite-gradle`, 241 files) in isolation, which completed cleanly with
     no diff against the rest of the tree.

     `javac` compile of the whole tree was judged impractical standalone (Gradle multi-module project
     with ANTLR-generated sources and real inter-module dependencies, per this item's own long-
     standing note) — used the `java_syntax_check` fallback instead (same pattern as items 25/26).
     Baseline (original unformatted 3510 files): 3510/3510 OK, 0 syntax errors. Round1 (first pass,
     before the fix below): **1 new syntax error**, not present in baseline —
     `rewrite-java-25/.../ReloadableJava25ParserVisitor.java:764: variable declaration not allowed
     here`. Root cause: `BlockStructureRule.isSingleStatementBody`'s declaration guard only refused
     collapse for a `final`/`const`-qualified leading token (`final boolean ignored = ...;`) — an
     un-qualified primitive-type declaration (`int saveCursor = cursor;`, no `final`/`const`) was
     never caught, so `if (...) { int saveCursor = cursor; }` got collapsed to the illegal braceless
     `if (...) int saveCursor = cursor;`. Fixed by adding a new `PRIMITIVE_TYPE_KEYWORDS` set
     (mirrors `DeclarationAlignmentRuleCurly`'s own `TYPE_KEYWORDS_C`/`TYPE_KEYWORDS_JAVA`) and a
     sibling guard alongside the existing `final`/`const` check: refuse collapse when the leading
     token is a primitive/built-in type keyword directly followed by an identifier. Verified: `make
     test` 264/264 (up from 263/263, new fixture `real_code_regressions_187`); full-tree re-run after
     the fix — round1 syntax-check 3510/3510 OK (matches baseline exactly); round1/round2 idempotency
     diff unchanged at the same 4 residual files above (confirms the fix didn't touch that unrelated
     bug family); the earlier transient crash did not recur.

     **Disposition: DONE.** Full-tree idempotency and syntax-check baselines are now established for
     the first time; the one real (syntax-breaking) bug found is fixed and fixtured
     (`real_code_regressions_187`). The 4 residual cosmetic idempotency diffs are recorded as new
     Known Gaps below rather than chased further this session.
(18) Local `VMA-GIT/anemonesoft/` (82 `.java`) — 1 bug: `renderCallCandidate` swallowed a
     multi-line brace-bodied trailing argument. Verified (4). Fixture: `real_code_regressions_29`.
(19) Local `ARMCortexMThumbC.java.in` (PCPP template) — no bug found; verified (5), 0-line
     token-stream diff.
(20) C++20 `ericniebler/range-v3` (311 `.hpp`) — 2 compile-breaking bugs from its
     concept-emulation-macro convention: `template(...)` macro wrongly pulled onto a declarator
     line (fixed by gating on `<`); `CPP_ret(void)(...)` mis-rewritten to `CPP_ret()(...)`,
     deleting the macro's real argument. Verified (2) + full-tree idempotency. Fixture:
     `real_code_regressions_50`. Follow-up bug (b): multi-line `//`-banner-commented deletion
     declaration collapsed by function-pointer-detection misfiring on macro-call shape; fixed
     with narrow `COMMENT_LINE`-scan guard. Verified (2) + `make test` 70/70 + full 318-file
     tree idempotency. Fixture: `_51`. Bug (a) (item-20 idempotency bug) tracked/resolved
     separately — see Open Questions / `RDD_KEY_169`.
(21) C++20 `boost-ext/ut` (44 files) — 1 idempotency bug: a deduction-guide statement's
     close-paren misidentified by `findCloseParenBeforeTrailingReturnType`'s backward scan as an
     unrelated following struct's close paren (scan didn't stop at a depth-0 `;`). Verified with
     minimal repro, `make test` 72/72, full-tree idempotency, (2)/(3) compile checks matching
     baseline. Fixture: `real_code_regressions_52`.
(22) C++20/23 `microsoft/proxy` (28 `.h`/`.cpp`) — 3 bugs in
     `CppSpecificRule.enforceRequiresClausePlacement` (RDD_KEY_170): unstable baseIndent/fit-check
     from closing-paren's line instead of parameter list's opening-paren line; preprocessor
     directive inside clause constraint expression spliced mid-line — fixed by leaving any clause
     containing a `PREPROCESSOR` token untouched. Verified `clang++ -std=c++23 -stdlib=libc++
     -fsyntax-only` (0-error baseline unchanged), full round1/round2 idempotency, `make test`
     77/77. Fixture: `real_code_regressions_53`.
(23) Local `../../../src/jxm` (~272 files: real `.java` + PCPP `.java.in`/`.java.inc`) — 3
     plain-Java bugs, none PCPP-specific: (a) `reclassifyAngleBrackets` had no `>>>` case
     (RDD_KEY_171); (b) `isSingleLineBody` fit-prediction omitted leading indent + trailing
     comment width (RDD_KEY_172); (c) §8 multi-line param-list renderer inlined leading `//` as
     same-line prefix and column-width calc excluded such params → `padRight` no-op merging
     type+name (RDD_KEY_178). 1 known gap unfixed: second occurrence of accepted
     switch-case-reindent gap, `tool/JSONEncoderLite.java` (see "Known Gaps — Open"). Verified:
     full-tree round1/round2 idempotency (clean except accepted gap); `.java.in` via `pcpp_java`;
     `java_syntax_check` (32 pre-existing errors, all pristine U+200B zero-width-space, not
     formatter-introduced); `make test` 90/90. Fixtures: `real_code_regressions_65` (a+b),
     `_66` (c).
(24) Local `../../../src` minus `jxm` (item 23) — vendored third-party Java under `src/com/`/
     `src/org/` (173 files, plain `.java`, no PCPP). 2 bugs, same "raw source indent measured
     before conversion to target indent-style" pattern, only on tab-indented source:
     (a) `enforceCommentStyle` reindented block-comment continuation lines to raw not-yet-
     converted indent — fixed via `renderIndent` normalization first. (b)
     `enforceCallLineBreaking` fits-checks measured tab-indent via `String.length()`, understating
     width — fixed via `expandedIndentWidth`. Verified: round1/round2 over 173 files (down to 6
     pre-existing/deferred diffs, left undisturbed); `make test` 145/145; `javac` (100
     pre-existing errors, all in untouched `jxm/` sibling, zero inside `com`/`org`);
     `java_syntax_check` 173/173 clean. Fixture: `real_code_regressions_95`.

(25) **DONE** — `github.com/jenkinsci/jenkins` (1929 `.java` files, plain Java, no PCPP). Baseline
     `java_syntax_check`: 0 pre-existing errors, full-tree round1 clean. 3 bugs found, 2 fixed:
     (a) `findArrowCases` brace-depth-0 label scan never skipped past a just-found arrow →
     multi-value arrow labels (`case null, default -> ...`) re-matched/duplicated each round —
     fixed by advancing scan past found arrow. (b) `needsSpaceBetween` only tight-joined Kotlin
     `@`; Java `@NonNull String id` → `@ NonNull` — fixed by extending to `lang.isJava`.
     Verified: minimal repros, `make test` 162/162 (up from 161/161); targeted full-tree re-run
     idempotency diff 20→15 files. Fixture: `real_code_regressions_113`. Remaining 15-file diff
     architectural: 13 "non-idempotent reindent on internally-inconsistent source", 1
     (`IdStrategy.java`) `alignCommentSeparators` false-positive, 1 (`PluginManager.java`)
     low-priority line-wrap instability (both then "Known Gaps — Open"). `javac` not attempted
     (Jenkins Maven heavy deps); `java_syntax_check` + idempotency load-bearing.
     `java_content_diff` spot-check: content preserved. **Session closed with remaining 2 gaps
     explicitly accepted, not fixed** — user decision to mark DONE; permanent known limitations
     (`alignCommentSeparators` also in `README.md`).

(26) **DONE — no open gaps** (2026-08-09: re-checked; the "documented open gaps" this item's
     header used to claim were the 3 gaps below, all already marked FIXED — stale header wording,
     not an actual remaining gap. Full-tree re-run not attempted, ~350kloc not worth re-cloning
     just to confirm already-fixed bugs stay fixed) — `github.com/microsoft/STL` (`stl/inc/`+`stl/src/`,
     289 files ~9MB, extensionless headers copied to `.hpp` first; excluded `.ixx` module units).
     Full-tree round1: all 289 formatted, no crashes. Round1/round2: 110/289 differed initially.
     `clang++` compile not attempted (needs STL's own CMake+MSVC harness); full-tree idempotency
     is the load-bearing check.

     2 bugs fixed: (a) `applyLineEndings` default (`lf`) fast path skipped `\r` stripping — false
     on CRLF input (tokenizer preserves `\r` in untouched WHITESPACE); fixed by always
     normalizing to clean LF first — alone resolved 99/110 diffs (STL is CRLF throughout). (b)
     Duplicated `collapseToOneLine`/`flushCollapseGap` (`MiscRuleCurly.java`,
     `CppSpecificRule.java`) inserted a space rejoining newline-spanning gaps with no tight-join
     awareness (`other. _Outer`); sibling `collapseTokensToOneLine` already had the JS/TS guard —
     mirrored to both. `make test` 168/168 (up from 166/166); 110 diffing files → 11. Fixtures:
     `real_code_regressions_118` (a), `_119` (b).

     3 additional gaps, all fixed (see "Known Gaps — Fixed"): constructor signature parameter-
     wrap misapplied to member-initializer-list entry (mutex.hpp, shared_mutex.hpp,
     filesystem.cpp); macro-then-statement line-merge (`_TRY_IO_BEGIN`/`_TRY_BEGIN`/`_BEGIN_LOCK`
     glued to following `if(...)` — istream.hpp, stacktrace.hpp, xlocale.hpp); two
     declaration-alignment column-padding non-idempotency shapes — `ranges.hpp`/`_Range`
     (`ScopePipelineCore.trailingIndent` sweeping same-line comment into per-line indent) and
     `filesystem.hpp` `recursive_directory_iterator` (separate mechanism, also algorithm.hpp).

(27) **DONE** — `github.com/apache/ant` `src/` tree (item 9). Plain `.java`, no PCPP, 1337 files
     (`src/main` + `src/tests`). Full-tree round1/round2 (`--preserve-tree --root DIR --out DIR`):
     `java_syntax_check` baseline 1337/1337 clean (incl. intentionally malformed
     `tests/antunit/taskdefs/javac-dir/bad-src/Bad.java`, identical failure baseline and round1).
     1 bug fixed: `BlockStructureRule.tryCollapse` §10 braceless-body collapse had no check for
     local-variable-declaration body — `FileUtils.java`'s
     `if (!f.canWrite() && ON_WINDOWS) { final boolean ignored = f.setWritable(true); }` collapsed
     to braceless `if` with bare-declaration body (javac rejects). Fixed: refuse collapse in
     `isSingleStatementBody` when body's first token is `final`/`const`. Verified: round1
     syntax-check 1337/1337 clean post-fix. Fixture: `real_code_regressions_126`. Full
     `javac`/self-bootstrap not attempted (Ant multi-step bootstrap); `java_syntax_check` +
     idempotency load-bearing.

     2 idempotency diffs found this session, both the already-documented ACCEPTED "Non-idempotent
     ... re-indent on internally-inconsistent generated source" gap (see "Known Gaps — Open";
     root-cause narrative is switch-case-specific but the pattern recurs on plain `if`/`else`
     bodies too): `JikesOutputParser.java` (`else` misindented relative to `if`) and
     `PathTest.java` (closing `}` at column 9 vs surrounding block column 8). Both pre-date this
     session (original repo source, not formatter-introduced); same "general scope-depth
     reindentation not started" architectural bucket in `STATE_COMMON.md` — no fixture added
     (indistinguishable from that already-tracked class).

     **2026-08-09 single-file re-check (one-off, not a full-tree re-run):** `PathTest.java` no
     longer reproduces — round1/round2 idempotency diff is now empty (fixed as a side effect of
     unrelated work since the original run, not investigated further). `JikesOutputParser.java`
     still reproduces the identical shape byte-for-byte, including with
     `curly-general-scope-reindent` turned on (GDR changes the specific bytes touched but the
     round1/round2 diff is still non-empty — GDR does not currently close this gap either). Only
     these 2 files were re-checked (sparse-fetched individually, not a full-tree clone); no other
     part of the 1337-file corpus was re-verified.

**Not started dogfood / real-code testing**
(3) `github.com/llvm/llvm-project` — LLVM/Clang monorepo; enormous, likely only a
    partial/targeted subtree run is practical (e.g. `clang/lib/Format/` or
    `llvm/include/llvm/ADT/`). Try to exercise C++23 features specifically. Would verify with
    (2)/(3). (NOT STARTED)
(4) `github.com/gcc-mirror/gcc` — GCC monorepo; similarly enormous, and GCC's own source may
    target an older/conservative C++ dialect in parts (bootstrapping), so may exercise less
    modern-C++ surface than its size suggests — lowest priority of the four for
    modern-feature testing specifically. Try to exercise C++23 features specifically. Would
    verify with (2)/(3). (NOT STARTED)
Priority order for the C/C++ queue unless the user redirects: `llvm-project` →
`gcc-mirror` (`mp11`/`lexy`/`stdexec`/`range-v3`/`boost-ext/ut`/`microsoft/proxy`/`STL` already DONE —
`mp11` was smallest/narrowest, `lexy` next for operator-overloading/concepts/CRTP/dense
declaration-alignment in one small tree, `stdexec` for concepts/`requires`/deep metaprogramming,
`range-v3` for its `template(...)`/`CPP_ret`-style concept-emulation-macro convention). For any
C/C++ candidate under a `.h`/`.hpp` extension, confirm the actual language before testing — copy
to `.hpp` first if really C++.

*(`stdexec`, `mp11` reached DONE with no open gaps. javaparser/javaparser's (15b) full narrative —
including a spurious "26 files differing" reading later found not to reproduce (stale
pre-rebuild jar) — is compacted into entry (16) above and "Known Gaps — Open" below; nothing
still-open or unrecorded was removed.)*

When a test completes, remove/compact its entry from "Not started" (or its "In progress"
detail block here) and add it to "Finished dogfood / real-code testing" above — and to
"Tools/compiler used" too, if it introduces a genuinely new tool not already listed there.

**Other findings outside the candidate list**

**Config-key wiring audit (2026-07-06)** — `Doc.java` divergence: `MiscRule.INDENT_WIDTH`/
`LINE_LENGTH_LIMIT` were dead `static final` constants disconnected from `Config` (only
`line-length`/`indent-size` unwired). Fixed via instance fields through rule constructors.
No-op at default; verified at `indent-size = 2`. Same-day: hardcoded
`DEFAULT_INDENT_UNIT = "    "` fallback (same bug class) fixed in `MiscRule`,
`JavaSpecificRule`, `CppSpecificRule`. Same-day removal: `header-guard-style` (silently-dead
config surface) removed from `Config.java`, `README.md`, and this file's sample config.

**Dogfood-compile-check bug** (predates round1/round2 methodology): `MiscRule` call/
declaration preserve-group renderers reset paren/bracket/angle depth to 0 at each physical line
start, corrupting multi-line nested calls (incl. own `TokenizerCore.java`). Fixed via
`groupByOriginalLine` tracking depth cumulatively across the slice.

**Known pre-existing gaps** (found during `Main.java` smoke-testing, left unfixed as out of
scope, flagged to user): `ServerMode.FormatHandler` doesn't resolve `indent-style = auto` before
calling `Formatter.formatOne` (masked in practice by `Main`'s fallback-to-standalone-on-
delegation-failure behavior); `Config.lineEndings()` is applied by `Main.applyLineEndings()` for
standalone/in-process formatting but not yet by `ServerMode.FormatHandler`. Full detail:
RDD_KEY_88.

**Step 2 — AI integration: NOT FEASIBLE (deferred) — see `STATE_AI.md`.**

---

## Known Gaps — Open

- **[Shared with STATE_JS_TS.md] Call-wrap/collapse vs. declaration-alignment/padding fits-check
  ordering — 2026-08-05 investigation session, no code change landed.** Full write-up lives in
  `STATE_JS_TS.md`'s Open Questions section (cluster #3 sibling entry) since the concrete repro
  (`microsoft/TypeScript`'s `commandLineParser.ts`) and all debugging happened there; recorded
  here only as a cross-reference per this bug's "shared root cause" framing. Summary: the real
  root cause is NOT a single method's fits-check but `ScopePipelineCurly.processScope`'s
  outer-first-then-recurse-into-child-spans architecture running the same declaration/assignment/
  signature/getter-setter passes twice over overlapping token ranges within one `format()` call —
  shared infrastructure across every curly-family language (C/C++/Java/Kotlin/JS/TS), not JS/TS-
  specific, which is why this key lives in both jobs' state files. A narrower, verified-safe,
  no-regression refinement to `JsTsDeclarationAlignmentRule.spansMultipleLines`'s bail condition
  was prototyped and reverted (didn't fix the cited bug alone — see `STATE_JS_TS.md` for the
  exact diff shape, easily re-derived if wanted independently). No attempt was made to change
  `processScope`'s outer/inner double-pass architecture itself — same risk class as
  `STATE_CURLY_GDR.md`/`RDD_KEY_229`'s pre-pass-vs-post-pass GDR investigation (a genuine
  circular dependency between an outer pass's decisions and an inner pass's re-derivation of the
  same span from different intermediate text). Left OPEN.
  **2026-08-06/2026-08-07 update:** the `commandLineParser.ts` declaration-alignment instance of
  this bug was fixed by `RDD_KEY_248` (`ScopePipelineCurly.reapplyClosingBraceAndDeclarationsPass`,
  a narrower JS/TS-gated re-run of just the closing-brace + declarations passes, not a change to
  `processScope`'s outer/inner double-pass architecture itself). A 2026-08-07 root-cause-only
  session found a still-open sibling instance of the same family in a pass `RDD_KEY_248` does not
  re-run — `ScopePipelineCurly.applyAssignmentsPass` (bare-assignment alignment, distinct from
  `applyDeclarationsPass`), repro `microsoft/TypeScript`'s `harness/collectionsImpl.ts` — see
  `STATE_JS_TS.md`'s Category 2 cluster #3 2026-08-07 note for the full write-up and candidate fix
  approach (not attempted). Still shared, curly-family-wide infrastructure; still Left OPEN.

- **Non-idempotent switch-case re-indent on internally-inconsistent generated source**
  (`SwitchRule.applyNonInlineCaseIndent`) — RESOLVED 2026-08-07 for both the single-switch
  internally-inconsistent-indentation shape and the nested-switch-in-switch shape (see "Known Gaps
  — Fixed" below, `RDD_KEY_251`). The disjoint **second occurrence** below (a different root
  cause entirely — `ScopePipeline.applyDeclarationsPass`, not `SwitchRule` — found on a lone
  declaration inside a switch body, not a switch-case re-indent) remains ACCEPTED, not fixed;
  original write-up retained for context:

  **Second occurrence** — local `src/jxm` dogfood (candidate 23): `tool/JSONEncoderLite.java` has
  a lone declaration inside a deeply/inconsistently hand-indented `switch default` block whose
  indentation drifts by 1 space per round. Root-caused via a minimal synthetic repro (not
  committed) to the same architectural bug class: `ScopePipeline.applyDeclarationsPass`'s
  raw-source-derived indent diverges from scope-depth-derived indent when the original source's
  raw indentation for that line is inconsistent with the block's structural depth — same shape as
  the (now-fixed) switch-case gap, triggered via the declarations pass instead. Same disposition:
  ACCEPTED, not fixed, single occurrence. No fixture.

- **`openrewrite/rewrite` full-tree re-verification (2026-08-09), 4 residual idempotency diffs —
  ACCEPTED, not fixed.** Found during item (17)'s deferred full-tree round1/round2 re-run (see that
  entry for the run's own detail — this only records the 4 unresolved diffs left over after that
  session's one real bug, the primitive-type-declaration collapse, was fixed and fixtured as
  `real_code_regressions_187`). All 4 are cosmetic (idempotency-only, no invalid-syntax risk — the
  `java_syntax_check` full-tree baseline stayed 3510/3510 clean both before and after), and each
  matches an already-documented deep/risky architectural bug family elsewhere in this file rather
  than being a novel shape — judged not worth a blind fix at this session's scope, same call already
  made for the open `PowerShellSpecificRule.java` self-format bug in `STATE_COMMON.md`'s "Formatter
  self-formatting" section. No fixture registered (root cause not isolated to a minimal repro this
  session). Left OPEN for a future session:
  - `rewrite-java-test/src/test/java/org/openrewrite/java/ModerneWebsiteExampleTest.java`: a
    switch-arrow braceless `if/else` body followed by the arm's own closing `}` on the same physical
    line (`default -> { if(...) ...; else b.append(c); }`) keeps the `}` attached to the `else`
    branch's body on round1, but gets it pulled onto its own line on round2 — switch-arrow-brace
    pass-ordering family, same shape as the original Cluster 2 fix
    (`appendChainNewlineBeforeElse`)/`findBracelessStatementEnd` but a different trigger site
    (switch-arrow body, not a plain `if/else` chain).
  - `rewrite-kotlin/src/main/java/org/openrewrite/kotlin/format/TabsAndIndentsVisitor.java` and
    `rewrite-yaml/src/main/java/org/openrewrite/yaml/YamlParser.java`: a wrapped call argument's
    closing-paren continuation line gains 4 extra indent spaces between round1 and round2 —
    indent-width-decided-before-a-later-pass-grows-it family, same root-cause shape as the original
    Cluster 1 fix (`isSingleLineBody`/`expandedIndentWidth`) and the still-open
    `PowerShellSpecificRule.java` self-format bug, but a different trigger site (a wrapped-call
    continuation line's own indent, not a comment-column-alignment width).
  - `rewrite-python/src/test/java/org/openrewrite/python/internal/pep508/Pep508RequirementTest.java`:
    a `List<String>` declaration's alignment-group padding (3 spaces before the variable name)
    collapses to 1 space on round2 — alignment-padding-collapse family, same shape as the original
    (now-fixed) Cluster 5 (`DeclarationAlignmentRuleCurly.parseDeclaration`'s function-pointer-
    declarator misdetection), but a different trigger site not yet isolated.


## Known Gaps — Fixed

- **[Shared with STATE_JS_TS.md] `formatNonInlineSwitches` vs. `alignInlineSwitches`/call-wrap
  ordering gap (switch-case fallthrough non-idempotency) — FIXED 2026-08-07 (RDD_KEY_263).**
  Full write-up (root cause, real-file repro, fix) lives in `STATE_JS_TS.md`'s formerly-"Known
  open issues" section since the concrete repro (`vuejs/core`'s `utils.ts`, `lodash/lodash`'s
  `lodash.js` `initCloneByTag`) and all debugging happened there; recorded here only as a
  cross-reference since the fix landed in shared `SwitchRule`/`FormatterCurly` code (curly
  family: C/C++/Java/Kotlin/JS/TS), not JS/TS-specific. Summary: `FormatterCurly.format`'s first
  `switchRule.formatNonInlineSwitches` call decided STYLE.md §13's blank-line-around-multiline-
  case-body treatment before `alignInlineSwitches`'s case-grid collapse and the call-wrap passes
  could turn an over-width grid-aligned case multi-line — fixed by re-running
  `formatNonInlineSwitches` a second time near the end of Phase 4, after those passes settle.
  `test/real_code_regressions_183_{inp,out}.js` fixture. `make test`: 253/253 forward +
  idempotency.

Previously-recorded low-priority gaps, now resolved. One-line summaries only — full
before/after detail available via `git log`/`git show`.

- **Non-idempotent switch-case re-indent, both the single-switch internally-inconsistent-source
  shape and the nested-switch-in-switch shape** (`SwitchRule.applyNonInlineCaseIndent`) — RESOLVED
  2026-08-07, seventh session on this gap. See the `RDD_KEY_251` index row above for the landed
  fix summary (`applyDepthDerivedBodyIndent`, boundary + paren/bracket-continuation guards,
  nested-switch-opaque approach (a) chosen over the rejected shared-accumulator approach (b)).
  Full two-approach writeup, including the rejected approach's exact failure mode, lives in
  `RDD_KEY_251` in `RDD_LOG.md`. Verified against the originally-cited production file
  (`javaparser/javaparser`'s `ASTParser.java`, ~5500 lines): previously a confirmed non-converging
  2-cycle, now terminates with idempotency diff dropped from 369 lines to 7 (all 7 pre-existing,
  unrelated to switch-case). `make test`: 247/247 → 248/248 forward + idempotency, zero
  regressions. New fixture: `test/real_code_regressions_181_{inp,out}.java`. `README.md`'s "Known
  Limitations" bullet for this gap removed.

- **Self-hosting dogfood bug: Java `&&`/`&` lost their leading space at a declaration-
  initializer/expression tight join** — FIXED, see `RDD_KEY_238` (index above) for full detail.
  `make test`: 244/244, unchanged. 168 real occurrences fixed project-wide across `src/` via the
  self-format dogfood-and-adopt re-run, 0 in `tools/*`.

- **`RDD_KEY_238` follow-up: general (non-declaration) expression-statement operator spacing was
  never re-derived at all** — FIXED, see `RDD_KEY_239` (index above) for full detail (3 confirmed
  sub-bugs, 3 regressions found/fixed along the way, `isUnaryMinusOperand` centralized). `make
  test`: 244/244 forward + idempotency throughout, zero regressions, no new fixture (user-agreed
  simplified scope). 21 files in `src/` affected and adopted back; `tools/*` unaffected.

- **Extremely long pre-existing single-physical-line statement wraps differently each round** —
  FIXED, see `RDD_KEY_225` (index above) for full root cause/fix detail. Re-verified durable
  against a `jenkinsci/jenkins` `hudson/PluginManager.java` repro (external copy since deleted,
  permanently covered by the fixture below): round1 == round2 == round3. 3 pre-existing fixtures
  (`real_code_regressions_57`/`129`/`130`) updated per `test/README.txt` and RDD_KEY_222
  precedent. New fixture `test/real_code_regressions_176_{inp,out}.java`. `make test`: 224/224 ->
  225/225, zero regressions.
- **`* const` cosmetic gap in mixed declaration groups** (`DeclarationAlignmentRule`) — FIXED.
  `splitCppType` now always returns `postConst = ""`, folding the whole type+star+const text
  into one uniformly-padded column. East-const (`char const*`) intentionally not normalized.
- **`typedef` declarations not aligned** — FIXED. `typedef` added as a rank-0
  `CppModifierPriority` keyword so it parses through the normal declaration path.
- **Direct function-pointer declarations not aligned** — FIXED. `parseDeclaration` now detects
  `Type (*name)(params)` directly and folds `(*name)` into the name cell (including
  multi-star `(**cb)`, via `Token.isRepOp`).
- **`#ifdef`/`#elif`/`#else`/`#endif` interleaved with declarations dropped every branch but
  the first** — FIXED, affects C/C++ (not Java-specific despite being found while
  implementing Task C below). `hasCommentBefore`'s group-break guard didn't check for
  `PREPROCESSOR`/`MACRO_DEF` tokens, so a directive mid-group never forced a group boundary
  and got silently discarded by `render(group)`. Fixed by adding those token types to the same
  guard.
- **`using` alias declarations not aligned — NOT SCHEDULED (design decision)**. Inverted
  grammar (`using Foo = Type;`) doesn't fit the existing `typeTokens`/`name` model; passes
  through unchanged (no corruption), not a bug. If picked up later: align at `=`, needs its
  own parsing branch and column layout keyed on `=` position.
- **Preprocessor directive glued onto a following Java method definition** — FIXED,
  genuinely Java-specific (C++'s `applySignaturePass` branch incidentally routes around it via
  a separate line-rescan). `leadStart`/`sigLeadStart` landed directly on a leading
  `PREPROCESSOR`/`MACRO_DEF` token instead of skipping past it, dropping the directive's text
  from the preserved leading gap. Fixed by walking forward past any leading directive run
  before computing the signature's real first token. Fixture:
  `test/java_preprocessor_method_inp/out.java`.
- **Macro-then-statement line-merge instability across formatting rounds** — FIXED. Found in
  `microsoft/STL` (item 26, `istream.hpp`/`stacktrace.hpp`/`xlocale.hpp`): bare
  macro-invocation-as-statement (`_TRY_IO_BEGIN`/`_TRY_BEGIN`/`_BEGIN_LOCK`, no trailing `;`)
  followed by `if (...) { ... }` that `collapseSingleExpressionBlocks` flattens got glued onto
  the macro line next round. Root cause: `splitStatements` merges macro + following `if` into one
  "statement"; `DeclarationAlignmentRuleCurly.parseDeclaration`'s collapsed-control-statement
  guard checked only first token (macro IDENTIFIER) and misparsed as `Type name = init;`. Fixed:
  depth-tracked scan of whole merged statement for top-level `if`/`while`/`for`/`switch`/`do`/
  `else`. Verified real STL tree (3 files idempotent), `make test` 169/169. Fixture:
  `real_code_regressions_120`.

  **2026-07-31 re-verification — STALE TRACKER ITEM, no code change needed.** A separate tracker
  item wrongly described this exact bug as still open. Re-verified against a fresh `/tmp/STL`
  checkout (`istream`/`stacktrace`/`xlocale`, current JAR): round1/round2 byte-identical, no
  residual case. No new fixture/changes — confirmation-only, to stop a future reader re-opening a
  closed bug.

- **Wrapped constructor signature's parameter-render logic misapplied to its own following
  member-initializer-list entry** — FIXED. Found in `microsoft/STL` (item 26, `mutex.hpp`/
  `shared_mutex.hpp`/`filesystem.cpp`): wrapped ctor corrupted `_Other._Pmtx` → `_Other. _Pmtx`
  (forward-pass, wrong on round1). Root cause: `MiscRuleCurly.enforceCallLineBreaking` treated
  `_Pmtx(_Other._Pmtx)` as call → `parseSignature`/`parseParam` mis-sliced `_Other._Pmtx` as
  `Type name` declarator → declaration-style column renderer inserted space after `.`. Fixed:
  `parseParam` rejects (null) any param whose `typeTokens` end in `.`/`->` (never a real C++
  type), falling back to plain-call rendering. Verified real STL (`mutex.hpp`/`shared_mutex.hpp`,
  idempotent, corruption-free), `make test` 169/169. Fixture: `real_code_regressions_121`.

- **`alignCommentSeparators` false-positives on ordinary English prose** — FIXED (third attempt;
  RDD_KEY_201's two prior attempts — fixed-character-set narrowing, then a 3+-consecutive-line
  threshold — tried and reverted; see RDD_KEY_201). Root cause: RDD_KEY_50's purely lexical rule
  can't distinguish §15 label/value from prose with incidental punctuation (`jenkinsci/jenkins`
  `IdStrategy.java`). Fixed via `MiscRuleCore.looksCodeLike(String)`: ≤4 words, ≤24 chars, no
  whole-word match in `PROSE_STOPWORDS`. Verified: `make test` 172/172 (up from 171/171), incl.
  `hpp_core_inp.hpp` legitimate 2-line §15 still padded. Fixture: `real_code_regressions_123`.
  See RDD_KEY_202.

- **`GetterSetterRuleCurly.parseOneLinerMember`'s breakable-width pre-check gated only on
  `isDefinition`** (`filesystem.hpp` `recursive_directory_iterator` shape) — FIXED (second,
  independent "Declaration-alignment column-padding non-idempotency" shape, after
  `ranges.hpp`/`_Range` `trailingIndent` fix below). Root cause: width-exceeds-`lineLengthLimit`
  exclusion gated `isDefinition && hasBreakableCall(...)`, but non-definition `(params)` can also
  be wrapped by `enforceCallLineBreaking` → `=` column pads round1 / shrinks round2. Fixed:
  `hasBreakableParams = !isDefinition && paramsFrom < paramsTo` alongside `hasBreakableCall`.
  Verified fixture 124: round1 == round2. `make test` 172/172. Constructed fixture only (no local
  `microsoft/STL` checkout reachable). See RDD_KEY_203.

- **`ScopePipelineCore.trailingIndent` sweeping a same-line leading comment into a declaration/
  assignment group's per-line indent** — FIXED (partial fix for "Declaration-alignment
  column-padding non-idempotency", `ranges.hpp`/`_Range` shape only). Found in `microsoft/STL`
  (item 26, `ranges.hpp` `chunk_view`/etc.): same-line leading comment on group's first member
  duplicated onto every sibling next round. Root cause: `trailingIndent(gap)` returned text after
  last `\n` as-is with no pure-whitespace check, sweeping comment into per-line join separator
  used by `applyDeclarationsPass`/`applyAssignmentsPass`/
  `applyOversizedAggregateInitClosingBracePass`. Fixed: truncate at first non-space/non-tab.
  Verified real STL (`ranges.hpp`, all 4 occurrences idempotent), `make test` 170/170. Fixture:
  `real_code_regressions_122`.

---

## TODO — All Tasks DONE

Implementation order used: C, B, D, E, A (F folded into each as it landed, plus a final F
sweep after A) — smallest/lowest-risk first, the large cross-cutting frozen-span feature (A)
last. One-line summaries below; full detail via `git log`/`git show`.

### A — Enable/disable formatting via markers/CLI flag (DONE)
`Token.frozen` + `TokenizerCore.markFrozenSpans` (scans `//% JXM_CFMT_DIS`/`ENA` and block
equivalents, in-memory token masking not fragment/tmp-dir splitting — RDD_KEY_90); every rule
class given a frozen-span guard; `--format-off` CLI flag. Two bugs fixed with
`format_toggle_inp/out.java` fixture (leading-gap vs. first-real-token frozen check;
child-scope re-tokenize losing frozen state → `startFrozen` param). `README.md` documents
marker syntax. `make test` 16/16 PASS.

### B — New config entries: `normalize-comment-start-case`/`normalize-comment-end-period` (DONE)
Two new `Config.java` keys (default `on`/`on`), following the `format-macros` pattern.
`make test` 15/15 PASS.

### C — Don't damage C-preprocessor macros embedded in Java source (DONE)
`TokenizerCore.isPreprocessorLanguage()` now returns `true` unconditionally, so `#`-directives
in `.java` lex as opaque tokens like C/C++. Surfaced two pre-existing bugs, fixed — see "Known
Gaps — Fixed" above. `make test` 15/15 PASS.

### D — Multi-file smoke test + benchmarking (DONE)
New `bench` Makefile target times all-at-once vs. one-by-one formatting in both standalone and
client-server mode.

### E — Code cleanups (DONE)
New `Lang` class centralizes `isC`/`isCpp`/`isJava` per file (replacing scattered string
checks); five null-safe token helpers centralized on `TokenizerCore.Token`, duplicate copies
removed from rule classes.

### F — Add more tests (DONE)
`test/c_cpp_decl_gaps_inp/out.c` added, covering the three `DeclarationAlignmentRule` fixes
under "Known Gaps — Fixed". `make test` 17/17 PASS.

### G — Verify `AI_PREAMBLE_FULL.md`'s `### Edge Case` sections against actual JAR behavior (DONE)
Both edge cases (`else`/`else if` closing comments; `type* const` in a mixed declaration group)
confirmed already correctly handled by the JAR via live `--standalone` runs, not genuine gaps.
Both sections removed from `AI_PREAMBLE_FULL.md` as redundant.

### H — Comment-grammar classifier accuracy upgrade (DONE)
Formerly its own `STATE_COMMENT_GRAMMAR.md` (deleted once complete — full design history in
`RDD_LOG.md`'s `RDD_KEY_94`–`RDD_KEY_98`). Optional classifier-backed path for
comment-normalization keys behind `comment-normalization-classifier` (default `off`, zero
behavior change when off). New `com.jxmake.formatter.classifier` package (feature extraction,
non-Latin-script gate, keyword-ambiguity gate, weighted `YES`/`NO`/`ABSTAIN` classifier — never
guesses). Weights from 40 labeled examples under `tools/classifier_weights/`
(`tools/classifier_weights/derive_weights.py`, see
`tools/classifier_weights/README.md`/`tools/classifier_weights/weights.md`). `make test` 70/70
PASS unchanged (default `off`); classifier `on` smoke-tested, 39/40 on labeled set.

**TODO (still open):** the 40-example `tools/classifier_weights/` set is synthetic; growing it with real comments
would firm up weight magnitudes and might surface new feature-set gaps. Independent of the
formatter's own `make test` suite — classifier defaults to `off`, ships no runtime AI
dependency; this is about `tools/classifier_weights/`'s example quality only.

### I — C/C++/Java braceless else-if chain collapse + alignment (DONE)
Extended Kotlin's braceless if/else-if/else collapse + column alignment (RDD_KEY_124/127/128)
to C/C++/Java, only when every branch in the chain qualifies (RDD_KEY_129) — mixed/braced chain
left untouched. New `BlockStructureRule.chainAllBranchesCollapsible` +
`alignBracelessElseIfChain` (moved from `KotlinSpecificRule.java` into shared rule class, runs
for all languages from `Formatter.java` Phase 4). Two follow-up defects fixed immediately after
(RDD_KEY_130): idempotency bug in render loop (leading-space stripping grew alignment spacing
each pass) and K&R `} else` collapsing whole chain onto one line instead of Allman-style column
alignment.

`make test` full suite green after RDD_KEY_129 and RDD_KEY_130. Fixtures updated:
`test/c_combined_out.c`, `test/cpp_modern_out.cpp`, `test/java_combined_out.java`,
`test/java_core_out.java`, `test/java_modern_out.java`,
`test/real_code_regressions_15_out.hpp`.
