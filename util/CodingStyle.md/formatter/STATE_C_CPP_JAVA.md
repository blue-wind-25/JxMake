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
| RDD_KEY_169 | range-v3 item 20 bug (a) RESOLVED -- `BlockStructureRule.enforceKAndRBraceStyle` glued a named construct's `{` onto a preceding bare `#endif` line; a later retokenize swallowed it whole into the `#endif` PREPROCESSOR token, permanently dropping that brace from every downstream scope-depth/frame-stack pass and desyncing closing-comment indentation and (downstream side effect) angle-bracket classification. Fixed by skipping the K&R glue when the preceding real token is a PREPROCESSOR directive |
| RDD_KEY_170 | microsoft/proxy dogfood: 3 bugs in `CppSpecificRule.enforceRequiresClausePlacement` -- (a)/(b) baseIndent/fit-check derived from the trailing `requires` clause's unstable-across-passes closing-paren line instead of the parameter list's own opening-paren line (with chained-specifier unwinding for `noexcept(...)`); (c) a preprocessor directive inside the clause's constraint expression got spliced mid-line, producing invalid C++. Fixed by leaving any clause containing a `PREPROCESSOR` token untouched |
| RDD_KEY_171 | Local `src/jxm` dogfood: `TokenizerCore.reclassifyAngleBrackets` had no case for a literal `>>>` token (triple-nested generics), only `>`/`>>` -- round2 re-lexed round1's tight `>>>` as one token, fell through to the generic-safe-token fallback, invalidated the open-`<` stack, spaced the generics out. Fixed by adding an explicit `>>>` case generalizing the `>>` split to 3 nesting levels plus its 2/1-leftover-`>` partial-match variants |
| RDD_KEY_172 | Local `src/jxm` dogfood: `JavaSpecificRule.isSingleLineBody`'s fits-under-limit prediction omitted leading indentation and any trailing same-line `//` comment, both counted by `MiscRule.enforceCallLineBreaking`'s own fit-check -- caused a K&R-vs-Allman flip-flop when indent+comment alone pushed an otherwise-fitting one-liner over the limit. Fixed by including both, whitespace-collapsed like `collapseToOneLine` |
| RDD_KEY_178 | Local `src/jxm` dogfood: two bugs in `MiscRule`'s STYLE.md §8 multi-line parameter-list renderer (`render` and its near-duplicate multi-line-declaration renderer) around a standalone `//` banner comment dividing parameter groups (`SWDFlashLoader.Specifier`'s constructor, `STM32QSPI.newQSPICmd`). (1) A leading `//` line comment was inlined onto the same output line as the next parameter's type+name, swallowing that declaration (and, once re-tokenized, the next one too) into the comment -- compile-breaking; fixed by emitting a leading `//` line comment on its own line (a self-terminating `/* ... */` still inlines). (2) The shared type/name column width (`typeColWidth`, from `maxTypeLen`) excluded params with a leading comment from its computation, so such a param's `typeText` could reach or exceed `typeColWidth`, making `padRight` a no-op and merging type+name with zero space (`InstModeinstMode`) next reformat; fixed by never padding to less than `typeText.length() + 1` |
| RDD_KEY_201 | `alignCommentSeparators` false-positive narrowing attempt, reverted -- tried a fixed allowlist (`—:–|~`) instead of RDD_KEY_50's "any non-alphanumeric char"; backfired (157/162, was 162/162) by disabling the old rule's incidental 2+-candidate disqualifier protecting `:`-heavy prose comments; fully reverted, 162/162 restored. **STALE, 2026-08-10**: "design question remains open" no longer true -- RDD_KEY_202 fixed this via `MiscRuleCore.looksCodeLike`; not in "Known Gaps -- Open" any more |
| RDD_KEY_202 | `alignCommentSeparators` false-positive -- FIXED via `MiscRuleCore.looksCodeLike`, a structural code-likeness check (word count/length/stopword list) on each candidate line's label/rest; see "Known Gaps -- Fixed" |
| RDD_KEY_203 | `GetterSetterRuleCurly.parseOneLinerMember`'s breakable-width pre-check gated only on `isDefinition`, `filesystem.hpp` `recursive_directory_iterator` assignment-alignment shape -- FIXED via a new `hasBreakableParams` check alongside `hasBreakableCall`; see "Known Gaps -- Fixed" |
| RDD_KEY_222 | `MiscRuleCore.computeLineCommentGroups`'s §15 consecutive-`//`-comment grouping (RDD_KEY_89) capitalized every group member's line independently, wrongly capitalizing continuation lines of a genuine multi-line `//` comment (unlike `/* */`, which correctly capitalizes only content line 0 via `stripSoleTrailingPeriodAcrossLines` + one `capitalizeFirstLetter` call). FIXED by capitalizing only `contents.get(0)`, matching the block-comment path. Applies to all Curly-family languages (`Lang.isCurly` = C/C++/Java/Kotlin/JS/TS); Python3/data-formats/XML/HTML5 have no §15 pass, unaffected. Surfaced a second latent bug: `nextCommentChainLinkIfAdjacent` wrongly chained a trailing end-of-line comment onto the next line's standalone `//` comment as one prose block (found via `test/js_comments_inp.js`/`test/ts_comments_inp.ts` regressing after the first fix). FIXED with a new `isStandaloneCommentLine` helper (true iff alone on its line back to `NEWLINE`/start-of-tokens); `nextCommentChainLinkIfAdjacent` now returns -1 for a non-standalone token, so a trailing comment is still capitalized/period-stripped alone (size-1 group) but never chains onward. 26 pre-existing `*_out` fixtures (Java/C/C++/Kotlin/TS: `real_code_regressions_*`, `c_cpp_decl_gaps`, `java_format_toggle`, `java_preprocessor_method`, `js_comments`, `ts_comments`) had the old buggy per-line capitalization hand-authored as "expected"; updated to match. `make test`: 219/219 forward + idempotency after both fixes, zero unexpected diffs |
| RDD_KEY_225 | `jenkinsci/jenkins` `hudson/PluginManager.java` (`doPluginsSearch`'s `sitePlugins` stream-chain declaration) -- root cause: `ScopePipelineCurly.applyDeclarationsPass` -> `DeclarationAlignmentRuleCore.renderInitTokens` (runs before `MiscRuleCurly.enforceCallLineBreaking`) unconditionally flattened a declaration's entire initializer, including an embedded multi-statement lambda body, onto one physical line with no line-length check, producing a real ~1992-char line no later pass could re-wrap. FIXED via a new pre-flight bail-out in `DeclarationAlignmentRuleCurly.parseDeclaration` (new `rawSliceBetweenUnfiltered`/`containsMultilineBraceBody` helpers): if any brace pair in the initializer originally spanned more than one physical source line, leave the statement untouched. See "Known Gaps -- Fixed" for detail, incl. 3 pre-existing fixtures (`real_code_regressions_57`/`129`/`130`) updated to match. New fixture `real_code_regressions_176`. `make test`: 224/224 -> 225/225 forward + idempotency, zero regressions |
| RDD_KEY_231 | User-improved `java_combined_inp.java` fixture -- 2 bugs, both cross-language (C/C++/Java/JS/TS): (a) `DeclarationAlignmentRuleCore.needsSpaceBetween` had no unary-vs-binary `+`/`-` awareness (`int aaa = +1;` -> `= + 1`), the exact gap `KotlinDeclarationAlignmentRule`'s own earlier Kotlin-scoped fix had flagged as still-open in its javadoc -- fixed by promoting `isUnaryMinusOperand` to `DeclarationAlignmentRuleCore` (shared by `renderTokens`/`renderInitTokens`; Kotlin's now-redundant override removed). (b) Independent: `ScopePipelineCurly.applyDeclarationsPass`'s idempotency-strip heuristic couldn't distinguish a genuine re-format's self-padding from a first-time format whose true indent coincidentally exceeded the modifier-column pad width, silently eating an indent level -- fixed by only accepting the strip when its result is already indentWidth-aligned. `make test`: 228/228, zero regressions |
| RDD_KEY_238 | Self-hosting dogfood bug: `DeclarationAlignmentRuleCore.isTightToken`/`MiscRuleCore.isTightToken`'s `Token.isRepOp(t, '&')` tight-join rule (C/C++ pointer/reference declarator sigil, already gated off for Kotlin/JS/TS) was never gated off for Java, though Java has no such construct -- wrongly collapsed a Java logical-AND's leading space (`x >= 2&& y`), found via `XmlSpecificRule.java`'s `shouldFosterParent`-adjacent `fostered` declaration. FIXED by adding `!lang.isJava` around the `&`-half of both conditions (`*`-half untouched, no observed bug). `||` confirmed unaffected. `make test`: 244/244, unchanged. Full self-format dogfood-and-adopt re-run: 168 real occurrences fixed in `src/`, 0 in `tools/*` (already clean, `java_content_diff.sh`/`python_content_diff.sh` clean on every file), 0 `||` occurrences anywhere. Surfaced an unrelated gap, fixed via RDD_KEY_239: 4 `&&`-missing-space occurrences remained in `src/` in a plain-assignment ternary shape never touched by the general statement rewrite |
| RDD_KEY_239 | RDD_KEY_238 follow-up: general (non-declaration) expression-statement operator spacing was never re-derived (plain-assignment RHS rendered via pure `joinVerbatim`). 3 confirmed sub-bugs fixed: missing space before `&&`, extra space after unary `!`, `- 1` vs `-1`. Full detail in `RDD_LOG.md`, incl. 3 regressions found/fixed along the way (C pointer-dereference, Java cast spacing, binary-`*`-after-`]`) and one unrelated pre-existing `]`-then-`(` tight-join gap also fixed. Also centralized duplicated `isUnaryMinusOperand` as `Token.isUnaryMinusOperand`. `make test`: 244/244, unchanged. 21 files adopted back in `src/` (`tools/*` unaffected) |
| RDD_KEY_251 | Seventh session, RESOLVED the nested-switch-in-switch failure mode of "Non-idempotent switch-case re-indent on internally-inconsistent generated source" -- `SwitchRule.applyNonInlineCaseIndent` now derives each case-body line's absolute target indent from its own brace-nesting depth (`applyDepthDerivedBodyIndent`, replacing the old single-relative-delta `shiftLines` body-shift), with a nested switch's entire token span treated as opaque (owned solely by its own independent `SwitchBlock` pass) rather than two independent recomputations disagreeing forever. A second approach (one shared depth accumulator recursed through nested switches) was implemented and rejected -- hung on the minimal repro because the nested switch's own independent pass still ran and disagreed with the recursive writes; a correct version would need far more invasive engine changes. See "Known Gaps -- Fixed", `real_code_regressions_181` fixture, and `RDD_KEY_251` in `RDD_LOG.md` for the full two-approach writeup. `make test`: 247/247 -> 248/248 forward + idempotency, zero regressions |
| RDD_KEY_281 | Tier2 backlog re-verification: `SwitchRule.deriveUnit`'s "hardcoded 4-space fallback" (real-code-testing item (9), `fmtlib/fmt` at `indent-size = 2`) was already fixed by `c0a2305` (2026-07-06) before this item's narrative sentence gained a disposition marker -- `deriveUnit` already returns `defaultIndentUnit`, built from `config.indentSize()` via `FormatterCurly`'s one `new SwitchRule(lang, lineLengthLimit, indentWidth)` call site; `MiscRuleCore.DEFAULT_INDENT_WIDTH` (literal `4`) is dead at runtime, reachable only via the unused 2-arg constructor overload. No source change made. Re-verified via a fresh flush-case-label `.cpp` scratch fixture at `indent-size = 2` (and `= 8`): idempotent, unit correctly scales with configured indent-size. `make test` unchanged, 278/278. |
| RDD_KEY_283 | `using` alias declarations (C++11+) column-aligned on `=` via new isolated `parseUsingAlias`/`renderUsingAliasGroup` path in `DeclarationAlignmentRuleCurly`; bails out untouched on any `...` token to avoid colliding with C++26 pack-indexing/variadic-template spacing rules. `make test` 283/283 -> 284/284. Fixture: `test/cpp_using_alias_{inp,out}.cpp`. See narrative entry in "Known Gaps -- Fixed" for full detail. |
| RDD_KEY_284 | Follow-up to RDD_KEY_283: pack-indexing/variadic-template `using` aliases (`template<typename... T> using Name = T...[N];`) are now ALSO aligned, via new `Declaration.aliasRawTypeText`/`templatePrefixRawText` verbatim-raw-text fields instead of bailing -- `enforcePackIndexingSpacing` runs after declaration alignment regardless of which pass emitted the line, so only the aliased type needed this; the `template<...>` prefix has no such downstream fix-up pass, so it's rendered verbatim too rather than regenerated, sidestepping the question. Also fixed: a standalone comment between `template<...>` and `using` has no home in the `Declaration` model and was being silently dropped -- now detected and bailed on (leave untouched); and a same-line trailing-comment spacing bug (single space instead of the codebase's two-space convention). `make test` unchanged at 284/284 (refines the existing `cpp_using_alias`/`cpp26_comments` fixtures, no new fixture). See narrative entry for full detail. |
| RDD_KEY_289 | Self-hosting dogfood (`JsTsSpecificRule.java`/`TokenizerCurly.java`): 2 bugs, combined fixture `real_code_regressions_205`. (a) `BlockStructureRule.extractSingleIdentifier`'s negated-single-identifier case (`while(!closed)`) dropped the leading `!` when building a same-kind-nested-loop closing-comment label, inverting an existing `} // while !closed` comment's meaning to `} // while closed` -- fixed to preserve the `!`. (b) `alignBracelessElseIfChain`'s chain detection is purely line-text-pattern-based with no check that a matched `if`/`else if` member's body is actually braceless -- a hand-written mixed chain (braceless `if` immediately followed by a genuinely braced `else if(...) { ... }`) got its `if` keyword left-padded by the keyword-length delta anyway, corrupting a correctly flush-aligned pair's indentation every reformat -- fixed by bailing the whole chain untouched, before any mutation, the moment any non-bare-else member's body is braced (bare terminal `else { ... }` remains supported, per `real_code_regressions_172_out.ts`). `make test`: 313/313 -> 314/314, zero regressions. |
| RDD_KEY_286 | `JXM_CFMT_CFG` directive gained a `--lang` pseudo-key (`//% JXM_CFMT_CFG --lang=cpp`) for a per-file language override, highest-priority (wins over CLI `--lang`/server `lang` param too) -- addresses the `.h`-defaults-to-C Open Question below (an explicit per-file fix, not a change to the default) and templated sources (`.java.in`/`.java.inc`) whose extension can't be inferred at all. `InFileConfig.parse` special-cases the key, validates via `Lang.isRecognized`; `Main.processFile`/`ServerMode.FormatHandler` reordered to read the file/body before deciding language. New fixture `in_file_config_lang_{inp,out}.h`. `make test`/`make test-server`: 285/285 -> 286/286, zero regressions. `README.md` updated (new subsection, precedence list, Known Limitations item 6 broadened from C++26-only to the whole C++ pipeline). |
| RDD_KEY_299 | Re-confirmed and closed `apache/ant`'s `JikesOutputParser.java` non-idempotent if/else reindent gap (documentation-only, same resolution shape as `RDD_KEY_243`) -- a prior recheck only tested `curly-general-scope-reindent=on` alone; with BOTH that flag AND `curly-general-scope-reindent-multipass=on` on, round1==round2 (idempotent) and round1 compiles clean under `javac`. See "Known Gaps -- Fixed", `test/real_code_regressions_214` fixture, `RDD_KEY_299` in `RDD_LOG.md` for full detail. `make test`: 322/322 -> 323/323 forward + idempotency, zero regressions. |
| RDD_KEY_315 | Deeper root-cause + accepted-gap disposition for RDD_KEY_314's call-argument function-expression body gap -- `ScopePipelineCurly.splitTopLevelSpans` only records a `{` as a recursable child-scope-owning brace when hit at `depth == 0`; a `function(...) { ... }` call argument's `{` is always hit at `depth >= 1` (inside the call's own parens), so it's never recorded as a span at all -- the whole call statement is one opaque top-level span, invisible to `processScope`'s recursion from the start (not merely "treated as opaque" during later call-wrap relocation, as RDD_KEY_314 framed it). A real fix needs `splitTopLevelSpans` (or an equivalent pre-pass) to detect and independently recurse into a `depth > 0` function-expression `{`, a genuine architectural extension shared by the whole curly family -- judged too risky to attempt speculatively this session; left as an accepted gap for a future session with explicit go-ahead. No source changed, `make test` not re-run. See "Known Gaps -- Open". |
| RDD_KEY_316 | JS/TS-only fix for RDD_KEY_315's call-argument function-expression body gap, without touching `splitTopLevelSpans`'s shared span model. `processScope`'s main span loop gained a `lang.isJs \|\| lang.isTs`-gated side channel: for a span with `openBraceIdx < 0`, `findNestedFunctionExpressionBraces` scans the span's own token range for a `depth > 0` `{` headed by `function [NAME] (...)` (direct `)` -> `{` adjacency only, no TS return-type tail) and recurses into each match the same way an ordinary child scope recurses (indent via `braceLineIndent` + one `indentUnit()`, same trailing-gap force-reindent logic), splicing the result back via the existing `Replacement`/`splice` mechanism. Same `hasTopLevelNewline` one-liner-stays-K&R gate as every other recursed scope, so a still-single-line call-argument body is deliberately left untouched. C/C++/Java/Kotlin's own `splitTopLevelSpans`/`processScope` paths are byte-for-byte unchanged (new code fully gated, cannot execute for other languages). One pre-existing fixture, `test/real_code_regressions_77_out.js`, regressed and was updated -- a call-argument body's interior `var app = ...` declaration line, previously left at its stale unnormalized 2sp indent (body was invisible to recursion), now correctly reindents to 4sp via the same `applyDeclarationsPass` every other recursed declaration gets; judged a genuine bug fix, not a regression. New fixture `test/real_code_regressions_218_{inp,out}.js`. **Residual, explicitly NOT fixed:** a plain non-declaration statement line inside a newly-recursed body is still passed through verbatim (not force-reindented) -- pre-existing general recursion behavior, newly exposed to this shape rather than introduced by this fix; documented as its own follow-on gap. C/C++/Java/Kotlin remain unaffected/still-accepted (RDD_KEY_315 unchanged for those languages). `make test`: 329/329 -> 330/330 forward + idempotency, zero non-JS/TS regressions. See "Known Gaps -- Open" (updated in place, not moved to "Fixed" since the C/C++/Java/Kotlin portion is still open). |

---

## Open Questions

- **`.h` -> C++26 §5 reflection rules (`^^`/`[: :]`) gap: VERIFIED, not a bug, documented.**
  `Lang.infer` maps `.h` to `"c"` by default, so `FormatterCurly`'s `lang.isCpp`-gated
  `enforceReflectionOperatorSpacing`/`enforceAttributeAndSpliceBracketPadding` never run for a
  `.h` file under default inference. Project owner's decision: keep default unchanged (blanket
  `.h` -> C++ risks misapplying C++-only rules to genuine C headers; content-sniffing too fragile)
  — require explicit `--lang cpp` override instead. Verified 2026-08-11: a `.h` fixture with
  `^^ int`/`[:refl:]` left `^^` untouched under default inference but tightened to `^^int` under
  `--lang cpp` (also flips empty-param rendering C `foo(void)` -> C++ `foo()`, confirming the whole
  `cpp` pipeline engages). `Main.java`'s `--lang` already takes priority over `inferLanguage`, no
  bug, no source change. Documented in `README.md` Known Limitations, curly-brace family, item 6.
  `make test`: 278/278 unchanged.

  **2026-08-12 follow-up:** per-file alternative now exists — `JXM_CFMT_CFG`'s new `--lang`
  pseudo-key (RDD_KEY_286). Item 6 broadened from "C++26 §5 only" to the whole C++-only rule
  surface (every C++-only rule was equally affected). Default `.h` -> `"c"` inference itself
  unchanged.

- **range-v3 real-code-testing item 20, bug (a): RESOLVED.** Idempotency divergence in
  `utility/any.hpp`, `iterator/common_iterator.hpp`, `meta.hpp`. Root cause/fix: entry (20) in
  "Finished dogfood / real-code testing" below. Full narrative: `RDD_KEY_169` in `RDD_LOG.md`.

- **[Shared with STATE_JS_TS.md family] Java assignment-alignment trailing-comment padding vs.
  `enforceCallLineBreaking` ordering — FIXED 2026-08-09 (four sessions/attempts).** Same
  architectural bug family as above (`ScopePipelineCurly.processScope`'s outer-first-then-recurse
  double-pass), same mechanism RDD_KEY_248/RDD_KEY_270 fixed for JS/TS via a narrow
  `reapplyClosingBraceAndDeclarationsPass` re-run — here for **Java**. Found via self-format
  dogfood on `rules/PowerShellSpecificRule.java`'s `format()`: a run of
  `s = someCall(s); // §N.n comment` assignments forms an `applyAssignmentsPass` alignment group;
  when `enforceCallLineBreaking` (runs after) wraps one sibling's call, every other sibling's
  trailing-comment column stays padded to the pre-wrap width — round1 keeps stale wide padding,
  round2 recomputes and collapses to one space — non-idempotent.

  **Attempt 1 (reverted):** widen the JS/TS-only re-run gate to all curly languages. Broke 8
  fixtures — re-running `applyOversizedAggregateInitClosingBracePass` a second time re-collapsed
  already-correct C/C++/Java output (RDD_KEY_246's "Attempt 2" failure mode).

  **Attempt 2 (reverted):** narrow the gate to JS/TS/Java, also skip
  `applyOversizedAggregateInitClosingBracePass` for Java. Narrowed the regression to 3 fixtures,
  but `applyDeclarationsPass`/`applyAssignmentsPass` still re-collapsed Java's enum-constant-list
  `;`-separator (RDD_KEY_89) — not isolated further.

  **Attempt 3 (reverted):** target the wrap *decision* instead of re-running passes —
  `MiscRuleCurly.flushCollapseGap` collapses the gap before a trailing comment to one canonical
  space so `enforceCallLineBreaking`'s fits-check ignores alignment padding. Safe alone (261/261
  clean) but insufficient — stabilizes the wrap decision, not `applyAssignmentsPass`'s stale
  padding on non-wrapped siblings; repro still diverged.

  **Attempt 4 (LANDED, FIXED):** new, narrower re-run mode re-deriving ONLY
  `applyAssignmentsPass`'s output instead of the whole three-pass bundle (attempts 1/2's failure
  mode). `ScopePipelineCurly.processScope`'s old `closingBraceAndDeclarationsOnly boolean`
  generalized to `int reRunMode` (`RERUN_MODE_FULL`,
  `RERUN_MODE_CLOSING_BRACE_AND_DECLARATIONS` = existing JS/TS bundle unchanged, new
  `RERUN_MODE_ASSIGNMENTS_ONLY`). New entry point
  `ScopePipelineCurly.reapplyAssignmentsPassOnly(String)`; `FormatterCurly.format` gained an
  `else if(lang.isJava)` branch. `applyAssignmentsPass` only touches genuine assignment groups
  (`MiscRuleCurly.groupAssignments`), so re-running it alone structurally can't touch an
  aggregate-init closing brace or enum `;`-separator — attempts 1/2's collateral damage.

  **Verification:** `make test` 269/269 → 270/270 (new fixture) forward + idempotency, zero
  regressions. Manual repro (7-line `s = applyX(s); // comment` chain, one sibling with a long
  method name so only it wraps): round1 == round2 byte-for-byte. Fixture:
  `test/real_code_regressions_193_{inp,out}.java`. `README.md`'s Known Limitations item 5
  (curly-brace family) removed as no longer applicable. Also resolves the
  `PowerShellSpecificRule.java` self-format trigger (its 2026-08-08 blank-line workaround no
  longer required but left in place).

  **2026-08-10: workaround removed, confirmed safe.** Blank lines between the `format()` method's
  `s = applyX(s); // comment` chain in `PowerShellSpecificRule.java` deleted; manual round1/round2
  `--out` re-format (`--lang` auto-inferred java) round1 == round2 byte-for-byte (confirms Attempt
  4's fix holds). `make jar` + `make test`: 275/275, clean. Not adopted back over `src/` as a
  formal dogfood-and-adopt pass — only the manual workaround was removed.

- **NOT REPRODUCED, 2026-08-03 — closed as unconfirmed/stale.** Ran every registered
  `test/*_out.cpp`/`test/*_out.hpp` fixture (37 files) through `g++ -std=c++20 -fsyntax-only` and
  `clang++ -std=c++23 -fsyntax-only` (tools (2)/(3), incl. `-stdlib=libc++`) — zero mismatches;
  every gcc-passing fixture also passes clang. The `-stdlib=libc++`-only failures (standalone
  header-fragment fixtures missing full context, e.g. `platform.hpp`) are expected snippet noise —
  none pass gcc either. No other finished-dogfood entry records a clang/gcc discrepancy (item 22
  `microsoft/proxy` matched gcc cleanly with `clang++ -std=c++23 -stdlib=libc++`). The adjacent
  item (20) `range-v3` corpus (likely origin of this report) was only ever verified against gcc,
  never clang, and its `/tmp` checkout is now empty (no filenames recorded). Closed as not
  reproducible against the current fixture set; re-open with concrete filenames if it resurfaces.

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
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
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
- [x] `--lang c|cpp|java` (2026-07-06): explicit language override for files whose extension
      `inferLanguage` can't recognize; one flag per invocation (no per-file override), validated
      against exactly `c`/`cpp`/`java` (exit 2 otherwise), threaded through `processFile` ahead of
      the extension-based `inferLanguage` fallback; `--server`/`--stop` reject `--lang`. The
      `/format` HTTP endpoint already accepted an optional `lang` query param taking priority over
      its own path-extension guess (`Main.delegateToServer` already sent it), so only server-side
      validation was added (`ServerMode.FormatHandler` now 400s on an unrecognized `lang`).
      `README.md` updated. `make test` 25/25, no regressions.
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
completed candidates have been compacted out of this file into the "Finished" list below,
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

**Dogfood Output Validation — `java_content_diff`.** Content-preservation checker for Java,
complementing `java_syntax_check` (proves only "still parses" — same
`css_content_diff.py`/`xml_content_diff.py` precedent from `STATE_DATA_FORMATS.md`). Reuses
`java_syntax_check`'s `JavacTask.parse()` infrastructure (no new dependency) but keeps the
`CompilationUnitTree` instead of only scanning diagnostics. The formatter *intentionally*
reorders/transforms some Java content (`java-import-order` sorting, declaration-alignment
whitespace, `normalize-comment-start-case`), so a naive text/token diff would false-positive —
comparison is split by content family:
- **imports** — compared as a multiset (sorted qualified-identifier strings, `static` flag
  included) since reordering here is legitimate.
- **package declaration + every top-level type declaration** — compared **in original relative
  order**, each via javac's own pretty-printer (`Tree.toString()`), whitespace-normalized. The
  pretty-printer encodes structure/identifiers/literal values but not original whitespace or
  comments, so pure reindentation/alignment-padding differences canonicalize to identical text
  while a dropped/added declaration, renamed identifier, or changed literal value still shows up.
- **comments** — extracted separately via a raw-text scan (skips string/char literals so a
  `//`/`/*` inside a literal is never mistaken for a comment start; the pretty-printer drops
  comments entirely), compared as a multiset, whitespace-normalized **and** lowercased — a
  case-only change is expected (`normalize-comment-start-case`) so it must not be flagged, but a
  dropped or corrupted comment still is.

Exit 0 if content preserved, 1 with a description of each mismatch otherwise, 2 if either file
fails to parse. Run (see STATE_COMMON.md's "Verifier toolchain paths" for `$JDK` env setup/build):
```bash
"$JDK/bin/java" java_content_diff <original.java> <formatted.java>
```
Verified against a hand-crafted good pair (reindentation + import sort + one comment
recapitalization — passes clean) and two bad pairs: a dropped statement (flagged "top-level
declaration #0 structure/content differs") and a corrupted comment (flagged as present in one
file's set but not the other's) — all three caught correctly. Fixtures kept in `/tmp` only, not
registered as permanent `test/` fixtures.

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
    exception case in this codebase. No-op at default, no fixture. **RESOLVED** — already fixed by
    `c0a2305` (2026-07-06, predates this narrative sentence gaining a disposition marker);
    re-verified 2026-08-11 with no source change needed (RDD_KEY_281): `deriveUnit` already returns
    `defaultIndentUnit`, built from `config.indentSize()` via `FormatterCurly`'s one `new
    SwitchRule(lang, lineLengthLimit, indentWidth)` call site — `MiscRuleCore.DEFAULT_INDENT_WIDTH`
    (the literal `4`) is dead at runtime, only reachable via the unused 2-arg constructor overload.
    Confirmed correct at `indent-size = 2` (and `= 8` as a second data point) via a fresh flush-case-
    label `.cpp` scratch fixture: idempotent, unit scales with the configured indent-size. `make
    test` unchanged at 278/278.
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
(16) MEDIUM `javaparser/javaparser` (1997 `.java` files, 7 modules) — 6 idempotency bugs, all fixed:
     braceless `if (cond) throw/return ...` misparsed as one-liner getter/setter; comment's sole
     trailing `.` stripped w/o separating whitespace (`_54`); `else`/`catch`/`finally`
     force-reindent dropping real blank line before `}` (`_55`); `enforceCallLineBreaking`
     fits-check before `formatNonInlineSwitches` case-body reindent — ordering fix (`_56`);
     `isCStyleCastClose` missing control-flow exclusion, misread
     `if(node instanceof RecordPatternExpr)` as cast (`_57`); Java enum-constant-list merging into
     adjacent field alignment group + drifting indent (`_58`).

     1 gap, `ASTParser.java` (JavaCC-generated, ~5500 lines) internally-inconsistent-source-
     indentation non-idempotency — **CLOSED 2026-08-16 (documentation-only, `RDD_KEY_301`, same
     resolution shape as `RDD_KEY_299`'s `JikesOutputParser.java`).** See "Known Gaps — Fixed".
(17) HUGE `openrewrite/rewrite` — DONE. Full-tree forward pass (default config): 0 errors.
     Round1/round2 idempotency (original 6-cluster investigation): 34 differing files, 6 clusters,
     all fixed:
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

     **Full-tree round1/round2 re-run + syntax-check, 2026-08-09 (deferred re-verification).**
     Corpus re-cloned fresh, grown to 3510 `.java` files (from 3373); batched per top-level module
     subdirectory. `javac` of the whole tree still impractical (Gradle multi-module + ANTLR
     sources); used `java_syntax_check` as before.

     Round1/round2 diff: 4 residual idempotency diffs, all cosmetic (no invalid-syntax risk), left
     open/undiagnosed as new Known Gaps (below) — each matches an already-documented architectural
     bug family (indent-width-decided-before-a-later-pass-grows-it; alignment-padding-collapse;
     switch-arrow-brace pass-ordering), same judgment call as the open `PowerShellSpecificRule.java`
     self-format bug. Files: `rewrite-java-test/.../ModerneWebsiteExampleTest.java` (switch-arrow
     braceless if/else body's closing `}` moves between rounds), `rewrite-kotlin/.../
     TabsAndIndentsVisitor.java` + `rewrite-yaml/.../YamlParser.java` (wrapped call continuation
     line gains 4 indent spaces between rounds), `rewrite-python/.../Pep508RequirementTest.java`
     (`List<String>` alignment padding collapses 3→1 space on round2). One transient,
     non-reproducible `NoClassDefFoundError: MiscRuleCore$SepMatch` crash hit mid-batch (JVM
     classloader hiccup, not a formatter bug); re-running the affected subdirectory alone
     (`rewrite-gradle`, 241 files) was clean.

     Baseline (unformatted 3510 files): 3510/3510 OK. Round1 (before fix): **1 new syntax error**
     — `rewrite-java-25/.../ReloadableJava25ParserVisitor.java:764: variable declaration not
     allowed here`. Root cause: `BlockStructureRule.isSingleStatementBody`'s declaration guard only
     refused collapse for a `final`/`const`-qualified leading token; an unqualified primitive-type
     declaration (`int saveCursor = cursor;`) wasn't caught, so `if (...) { int saveCursor =
     cursor; }` collapsed to illegal braceless `if (...) int saveCursor = cursor;`. Fixed via a new
     `PRIMITIVE_TYPE_KEYWORDS` set (mirrors `DeclarationAlignmentRuleCurly`'s
     `TYPE_KEYWORDS_C`/`TYPE_KEYWORDS_JAVA`) and a sibling guard refusing collapse when the leading
     token is a primitive/built-in type keyword followed by an identifier. Verified: `make test`
     264/264 (new fixture `real_code_regressions_187`);
     full-tree re-run — round1 syntax-check 3510/3510 OK, idempotency diff unchanged at the same
     4 residual files (confirms no interaction), crash did not recur.

     **Disposition: DONE.** Full-tree idempotency/syntax-check baselines established for the
     first time; the one syntax-breaking bug found is fixed and fixtured
     (`real_code_regressions_187`). The 4 residual cosmetic diffs are recorded as new Known Gaps
     below.
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
     `@`; Java `@NonNull String id` → `@ NonNull` — fixed by extending to `lang.isJava`. Verified:
     minimal repros, `make test` 162/162 (up from 161/161); targeted full-tree re-run idempotency
     diff 20→15 files. Fixture: `real_code_regressions_113`. Remaining 15-file diff architectural:
     13 "non-idempotent reindent on internally-inconsistent source", 1 (`IdStrategy.java`)
     `alignCommentSeparators` false-positive, 1 (`PluginManager.java`) low-priority line-wrap
     instability (both then "Known Gaps — Open"). `javac` not attempted (Jenkins Maven heavy deps);
     `java_syntax_check` + idempotency load-bearing; `java_content_diff` spot-check clean. **Closed
     with the remaining 2 gaps explicitly accepted** — user decision to mark DONE; permanent known
     limitations (`alignCommentSeparators` also in `README.md`).

(26) **DONE — no open gaps** (2026-08-09: re-checked, all 3 gaps below already FIXED — stale
     header wording. Full-tree re-run not attempted, ~350kloc not worth re-cloning just to confirm
     already-fixed bugs stay fixed) — `github.com/microsoft/STL` (`stl/inc/`+`stl/src/`, 289 files
     ~9MB, extensionless headers copied to `.hpp` first; excluded `.ixx` module units). Full-tree
     round1: all 289 formatted, no crashes; round1/round2 initially differed on 110/289. `clang++`
     compile not attempted (needs STL's own CMake+MSVC harness); full-tree idempotency load-bearing.

     2 bugs fixed: (a) `applyLineEndings` default (`lf`) fast path skipped `\r` stripping — false
     on CRLF input (tokenizer preserves `\r` in untouched WHITESPACE); fixed by always normalizing
     to clean LF first — alone resolved 99/110 diffs (STL is CRLF throughout). (b) Duplicated
     `collapseToOneLine`/`flushCollapseGap` (`MiscRuleCurly.java`, `CppSpecificRule.java`) inserted
     a space rejoining newline-spanning gaps with no tight-join awareness (`other. _Outer`);
     sibling `collapseTokensToOneLine` already had the JS/TS guard — mirrored to both. `make test`
     168/168 (up from 166/166); 110 diffing files → 11. Fixtures: `real_code_regressions_118` (a),
     `_119` (b).

     3 additional gaps, all fixed (see "Known Gaps — Fixed"): constructor signature parameter-wrap
     misapplied to member-initializer-list entry (mutex.hpp, shared_mutex.hpp, filesystem.cpp);
     macro-then-statement line-merge (`_TRY_IO_BEGIN`/`_TRY_BEGIN`/`_BEGIN_LOCK` glued to following
     `if(...)` — istream.hpp, stacktrace.hpp, xlocale.hpp); two declaration-alignment
     column-padding non-idempotency shapes — `ranges.hpp`/`_Range`
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
     root-cause narrative is switch-case-specific but recurs on plain `if`/`else` bodies too):
     `JikesOutputParser.java` (`else` misindented relative to `if`) and `PathTest.java` (closing
     `}` at column 9 vs surrounding block column 8). Both pre-date this session (original repo
     source, not formatter-introduced); same "general scope-depth reindentation not started"
     bucket in `STATE_COMMON.md` — no fixture added.

     **2026-08-09 single-file re-check:** `PathTest.java` no longer reproduces — idempotency diff
     now empty (fixed as a side effect of unrelated work, not investigated further).
     `JikesOutputParser.java` still reproduces identically, including with
     `curly-general-scope-reindent` on (GDR changes the bytes touched but the diff stays
     non-empty — doesn't close this gap either). **Disposition (2026-08-10):** this `apache/ant`
     finding is just a real-corpus instance of the already-documented "Non-idempotent reindent on
     internally-inconsistent generated source" gap — `README.md`'s Known Limitations → "Curly-brace
     family" item 4; redundant standalone `XL.txt` TIER 9 entry removed. Only these 2 files were
     re-checked (not a full-tree re-clone).

     **CLOSED 2026-08-16 (RDD_KEY_299):** re-verified with BOTH
     `curly-general-scope-reindent=on` AND `curly-general-scope-reindent-multipass=on` (the earlier
     2026-08-10 recheck above only tested the base flag alone). Ran the formatter on
     `JikesOutputParser.java` twice with both flags on: round1 byte-identical to round2 (empty
     `diff`), confirming idempotency. Compiled round1's output with `javac` against the file's real
     sibling classes (`org.apache.tools.ant.Task`/`Project`/`taskdefs.ExecuteStreamHandler`, etc.,
     pulled from the same `apache/ant` checkout) — compiled clean, zero errors. Same disposition as
     `RDD_KEY_243`: this is the already-shipped `curly-general-scope-reindent-multipass=on`
     workaround, just not previously re-tested in combination for this specific corpus finding.
     Minimal repro fixture: `test/real_code_regressions_214_{inp,out}.java` (isolates the exact
     shape — an internally-inconsistently-indented `} else {` in an if/else-if/else chain — gated
     behind both flags via in-file config, since GDR remains off by default project-wide). See
     "Known Gaps — Fixed" for the formal closure entry.

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
  ordering — 2026-08-05 investigation session, no code change landed.** Full write-up in
  `STATE_JS_TS.md`'s Open Questions (cluster #3 sibling entry) — concrete repro
  (`microsoft/TypeScript`'s `commandLineParser.ts`) and all debugging happened there; cross-ref
  only here. Root cause is NOT a single method's fits-check but
  `ScopePipelineCurly.processScope`'s outer-first-then-recurse-into-child-spans architecture,
  running the same declaration/assignment/signature/getter-setter passes twice over overlapping
  token ranges within one `format()` call — shared curly-family infrastructure (C/C++/Java/
  Kotlin/JS/TS), hence tracked in both jobs' files. A narrower, verified-safe refinement to
  `JsTsDeclarationAlignmentRule.spansMultipleLines`'s bail condition was prototyped and reverted
  (didn't fix the cited bug alone). No attempt to change `processScope`'s outer/inner double-pass
  architecture itself — same risk class as `STATE_CURLY_GDR.md`/`RDD_KEY_229`'s pre-pass-vs-
  post-pass GDR investigation (a genuine circular dependency between an outer pass's decisions and
  an inner pass's re-derivation of the same span from different intermediate text). Left OPEN.
  **2026-08-06/2026-08-07 update:** the `commandLineParser.ts` instance fixed by `RDD_KEY_248`
  (`ScopePipelineCurly.reapplyClosingBraceAndDeclarationsPass`, a narrower JS/TS-gated re-run of
  just closing-brace + declarations passes, not a `processScope` architecture change). A
  2026-08-07 session found a sibling instance in a pass `RDD_KEY_248` doesn't re-run —
  `ScopePipelineCurly.applyAssignmentsPass` (bare-assignment alignment), repro
  `microsoft/TypeScript`'s `harness/collectionsImpl.ts`. **FIXED, RDD_KEY_270**: added
  `applyAssignmentsPass` as a third pass inside the existing `closingBraceAndDeclarationsOnly`
  re-run mode, same gate as `RDD_KEY_248`; `make test` 259/259 -> 260/260, zero regressions.

- **Non-idempotent switch-case re-indent on internally-inconsistent generated source**
  (`SwitchRule.applyNonInlineCaseIndent`) — RESOLVED 2026-08-07 for both the single-switch and
  nested-switch-in-switch shapes (see "Known Gaps — Fixed" below, `RDD_KEY_251`). The disjoint
  **second occurrence** below (different root cause — `ScopePipeline.applyDeclarationsPass`, not
  `SwitchRule` — a lone declaration inside a switch body, not a case re-indent) — **CLOSED
  2026-08-15, no longer reproduces (RDD_KEY_292).** Original write-up retained for context:

  **Second occurrence** — local `src/jxm` dogfood (candidate 23): `tool/JSONEncoderLite.java` had
  a lone declaration inside a deeply/inconsistently hand-indented `switch default` block, drifting
  1 space/round. Root-caused (no committed repro) to the same bug class:
  `ScopePipeline.applyDeclarationsPass`'s raw-source-derived indent diverging from
  scope-depth-derived indent when the source's raw indentation is inconsistent with structural
  depth — same shape as the (now-fixed) switch-case gap, via the declarations pass instead.

  **2026-08-15 re-check:** a fix attempt against the real file (default config, `indent-size =
  2`/`3`) plus several synthetic repros matching the description all converged idempotently — no
  drift reproduces any more. Likely incidentally fixed by unrelated `applyDeclarationsPass`/
  indent-normalization work since (e.g. RDD_KEY_231), not proven since the original repro was
  never committed. Closed as no-longer-reproducing; reopen if a fresh instance surfaces. No
  fixture (none ever existed for this occurrence).

- **`openrewrite/rewrite` full-tree re-verification (2026-08-09), 4 residual idempotency diffs —
  1 remains ACCEPTED, not fixed; 3 FIXED (2026-08-15, see "Known Gaps — Fixed",
  `RDD_KEY_290`/`RDD_KEY_291`/`RDD_KEY_293`).** Found during item (17)'s deferred full-tree
  round1/round2 re-run (see that entry for the run's own detail — this only records the 4
  unresolved diffs left over after that session's one real bug, the primitive-type-declaration
  collapse, was fixed and fixtured as `real_code_regressions_187`). All 4 are cosmetic
  (idempotency-only, no invalid-syntax risk — the `java_syntax_check` full-tree baseline stayed
  3510/3510 clean both before and after).


- **Call-argument function-expression body never split to one-statement-per-line/Allman
  placement (RDD_KEY_314/RDD_KEY_315), FIXED for JS/TS 2026-08-20 (RDD_KEY_316); still ACCEPTED,
  not fixed for C/C++/Java/Kotlin.** A `function (x) { doA(x); doB(x); return x; }` passed as a
  call argument (e.g. `items.map(function (x) {...})`), with an already-multi-line body, never
  got recursed into -- unlike the identical body at declaration/statement position. Root cause
  (RDD_KEY_315), unchanged: `ScopePipelineCurly.splitTopLevelSpans` only records a `{` as a
  recursable child-scope-owning `braceIdx` at `depth == 0`; a call argument's function-expression
  `{` is always at `depth >= 1` (inside the call's own parens), so the entire call statement is
  captured as one opaque top-level span with no child scope.
  **JS/TS fix (RDD_KEY_316):** rather than teaching `splitTopLevelSpans` a second `braceIdx` per
  span (a change that would touch every curly-family language's shared span model),
  `processScope`'s main span loop gained a JS/TS-gated (`lang.isJs || lang.isTs`) side-channel:
  for a span with `openBraceIdx < 0`, a new `findNestedFunctionExpressionBraces` scans the span's
  own token range for a `depth > 0` `{` headed by `function [NAME] (...)` and recurses into each
  match the same way an ordinary child scope is recursed into (see full write-up in RDD_KEY_316).
  C/C++/Java/Kotlin's `ScopePipelineCurly.splitTopLevelSpans`/`processScope` code paths are
  byte-for-byte unchanged (the new code is fully gated behind `lang.isJs || lang.isTs` and cannot
  execute for any other language) -- confirmed via `make test`: 330/330 forward + idempotency,
  zero C/C++/Java/Kotlin fixture diffs. **Residual gap even for JS/TS, not fixed, flagged
  explicitly:** within a newly-recursed call-argument body, only declaration-statement lines get
  reindented (`applyDeclarationsPass`, as always); a plain non-declaration statement line is
  passed through verbatim, so a body with pre-existing inconsistent indentation can render with a
  visually inconsistent mix of reindented and untouched lines (see `test/real_code_regressions_77_out.js`,
  whose `var app = ...` line now reindents to 4sp while the following `request(app).get(...)`
  chain lines stay at their original 2sp) -- this is pre-existing general recursion behavior
  (identical for a standalone `const foo = function (x) {...};`), newly exposed to the
  call-argument shape rather than introduced by this fix; left as its own accepted, documented
  follow-on gap for a future session. **C/C++/Java/Kotlin: still ACCEPTED, not fixed** -- a real
  fix requires extending `splitTopLevelSpans` (or an equivalent pre-pass) to detect and
  independently recurse into a `depth > 0` function-expression/lambda/anonymous-class `{` for
  those languages too; not attempted (a C/C++ lambda-as-call-argument or Java
  anonymous-class-as-call-argument equivalent was not separately confirmed to exist as a distinct
  repro). Left for a future session with explicit go-ahead, same posture as `RDD_KEY_235`'s
  `renderCallCandidate` fits-check gap.

## Known Gaps — Fixed

- **`MiscRuleCore.groupAssignments` (STYLE.md §6 consecutive bare-assignment alignment) silently
  deleted an own-line comment sitting between two grouped assignment statements — FIXED 2026-08-19
  (`RDD_KEY_311`).** Found while dogfooding the formatter's own today-changed `FormatterCurly.java`:
  a real leading comment between two `jsTsRule.*(tokenizer.apply(text));` statements was deleted
  outright, not just reflowed. Root cause: `groupAssignments` only broke an alignment group on a
  blank line or an unparseable statement, never on a comment-only gap, despite its own doc comment
  already claiming that case was handled. `ScopePipelineCurly.applyAssignmentsPass` only preserves
  the *first* row's leading gap for a group's replacement text — every interior row is joined with a
  hardcoded `"\n" + indent`, so any leading comment on a non-first row had nowhere to survive. Fixed
  by adding `MiscRuleCore.hasCommentBeforeStmt` (mirrors `DeclarationAlignmentRuleCore.hasCommentBefore`'s
  exact precedent) and wiring it into `groupAssignments`'s existing `blankBefore`-triggered
  group-break alongside `blankBefore` itself. New fixture `real_code_regressions_217`. `make test`
  329/329 forward + idempotency, zero regressions.

- **`javaparser/javaparser`'s `ASTParser.java` non-idempotent if/else reindent — CLOSED 2026-08-16
  (documentation-only, `RDD_KEY_301`).** Same resolution shape as `RDD_KEY_299`/`RDD_KEY_243`: no
  source change. The item (16) dogfood session's "1 gap ACCEPTED not fixed" was re-tested this
  cleanup pass with BOTH `curly-general-scope-reindent=on` AND
  `curly-general-scope-reindent-multipass=on` together (the original acceptance predated a
  combined-flags recheck, same stale-recheck situation `RDD_KEY_299` found for `apache/ant` in the
  same session). With both flags: round1 byte-identical to round2 (empty diff); `java_syntax_check.sh`
  clean on both the original file and round1's output. No fixture added (no source touched, and the
  existing generic GDR-gap bullet in `README.md`'s Known Limitations already covers this class of
  gap without naming a specific corpus file).

- **`apache/ant`'s `JikesOutputParser.java` non-idempotent if/else reindent — CLOSED 2026-08-16
  (documentation-only, `RDD_KEY_299`).** Same resolution shape as `RDD_KEY_243`: no source change.
  Re-verified with BOTH `curly-general-scope-reindent=on` AND
  `curly-general-scope-reindent-multipass=on` together (a prior 2026-08-10 recheck had only tested
  the base flag alone and wrongly concluded GDR "doesn't fix it either"). With both flags: round1
  byte-identical to round2 (empty diff), and round1 compiles clean under `javac` against the file's
  real sibling classes. Fixture: `test/real_code_regressions_214_{inp,out}.java`. See the full
  narrative in item (27)'s dogfood entry above.

- **`openrewrite/rewrite` full-tree re-verification residual gaps, wrapped-call continuation-indent
  shape — FIXED 2026-08-15 (`RDD_KEY_293`).** Repro:
  `rewrite-kotlin/src/main/java/org/openrewrite/kotlin/format/TabsAndIndentsVisitor.java` and
  `rewrite-yaml/src/main/java/org/openrewrite/yaml/YamlParser.java` — a wrapped call argument's
  closing-paren continuation line gained 4 extra indent spaces between round1 and round2. Same
  root-cause family as the original Cluster 1 fix (`isSingleLineBody`/`expandedIndentWidth`) and
  the `PowerShellSpecificRule.java` self-format bug (`ScopePipelineCurly.reapplyAssignmentsPassOnly`),
  different trigger site. Root cause: `SwitchRule.formatNonInlineSwitches` (runs between the two
  `MiscRuleCurly.enforceCallLineBreaking` call sites in `FormatterCurly.format`) shifts a
  switch-case body statement's own leading indent (+4) without touching an already-wrapped
  single-argument call's continuation/closing lines nested inside it. Combined with
  `MiscRuleCurly.renderCallCandidate`'s blanket `topLevelArgs.size() <= 1` bail (added to protect
  against a genuine comma-misdetection risk when a lone argument's own content spans multiple
  physical lines), an already-wrapped single-argument call was permanently frozen at whatever
  indent it happened to have, producing round1's stale shape; round2 self-corrected only because
  `applyDeclarationsPass` (lacking RDD_KEY_225's brace-only bail applicability to a paren-only
  initializer) flattened the initializer back to one line first, letting the by-then-correct indent
  be recomputed fresh.

  Fixed by narrowing the bail to only fire when the sole argument's own content genuinely spans
  multiple physical lines (new `containsInternalNewline` helper check), letting a
  single-physical-line argument's wrap be safely re-derived from its current physical-line indent
  on every pass — C/C++/Java only. Kotlin/JS/TS keep the ORIGINAL blanket `topLevelArgs.size() <= 1`
  bail unconditionally: widening the narrowed check to those languages regressed
  `real_code_regressions_43.kt` (a genuine `topLevelArgs.size() == 2` candidate whose rendering
  shape turned out to depend on this bail's exact scope, reason not fully traced) and
  `curly_gdr_multipass_oneliner.js` (architectural conflict with
  `curly-general-scope-reindent-multipass`'s deliberately deeper one-liner-body indentation — see
  `STATE_CURLY_GDR.md`, out of scope here). Two prior attempts rejected: (1) unscoped
  `containsInternalNewline`-only narrowing (no language gate) regressed those same 2 fixtures; (2) a
  flat `lang.isKotlin || lang.isJs || lang.isTs || ...` top-level OR was WRONG — unconditionally
  bailed regardless of `topLevelArgs.size()`, silently changing size-`>1` behavior (4 regressions
  instead of 2, incl. 2 new TS failures). Landed version nests the language gate strictly inside the
  existing `topLevelArgs.size() <= 1` branch, size-`>1` fallthrough unchanged for every language.

  Verified via minimal isolated repro (nested if/switch/case chain, braceless-`else` branch with a
  wrapped single-argument call) — non-idempotent before, byte-identical round1/round2 after. New
  fixture `test/real_code_regressions_208_{inp,out}.java`. Both real triggering files
  (`/tmp/rewrite` corpus copy) also now byte-identical round1/round2. `make test`: 316/316, zero
  regressions. Corpus files left unreformatted — bulk-adopting a corpus reformat out of scope.

- **Self-hosting dogfood (`src/**/*.java` formatted with itself), 2 bugs found comparing `src/`
  against a fresh format of `src/` — FIXED, see `RDD_KEY_289` index row above for the (a)/(b) bug
  detail.** Bug (b) only reachable after its own method signature wraps to multi-line (that's what
  surfaced it), though the root cause is unrelated to signature-wrapping. `make test`: 313/313 ->
  314/314, zero regressions. New fixture `test/real_code_regressions_205_{inp,out}.java` (combines
  both bugs). `src/`'s two affected files left untouched (no bulk-reformat/adopt performed).

- **`openrewrite/rewrite` full-tree re-verification residual gaps, Cluster 2 shape (switch-arrow
  braceless if/else closing-brace instability) — FIXED 2026-08-15 (`RDD_KEY_290`).** Repro:
  `rewrite-java-test/src/test/java/org/openrewrite/java/ModerneWebsiteExampleTest.java`'s
  switch-arrow arm `default -> { if(c < 0x20) b.append(...); else b.append(c); }` — the arm's own
  closing `}` sits on the same physical line as the braceless `else`'s statement. Root cause: a
  pass-ordering disagreement between `ScopePipelineCurly.processScope`'s one-liner-vs-has-newline
  branch decision (controls whether a scope's trailing gap before `}` gets force-reindented onto
  its own line) and `BlockStructureRule`'s bare-`else` handling, which forced a newline before
  `else` but had no symmetric fixup for a `}` trailing the `else`'s own body on the same line —
  round1 (body still one physical line when `ScopePipelineCurly` runs) left the `}` attached;
  round2 (body already split by round1) force-reindented it onto its own line. Fixed by adding a
  new `else if` branch in `BlockStructureRule`'s bare-`else` handling: when a braceless `else`
  body's statement end is immediately followed, same physical line, nothing frozen in between, by a
  `}`, force that `}` onto its own new line at the `else`'s resolved indent — matching what round2
  would otherwise have produced, so both rounds converge. Verified via minimal repro and the real
  `ModerneWebsiteExampleTest.java` (`/tmp/rewrite` corpus copy) — non-idempotent before,
  byte-identical round1/round2 after both. New fixture
  `test/real_code_regressions_206_{inp,out}.java`. `make test`: 314/314 -> 316/316 (fixtures 206 and
  207 registered together), zero regressions. See `RDD_KEY_290` in `RDD_LOG.md` for full detail.

- **`openrewrite/rewrite` full-tree re-verification residual gaps, Cluster 5 shape (alignment
  padding collapse) — FIXED 2026-08-15 (`RDD_KEY_291`).** Repro:
  `rewrite-python/src/test/java/org/openrewrite/python/internal/pep508/Pep508RequirementTest.java`'s
  `List<String[]> urlSpecs = Arrays.asList(new String[]{...}, ...)` declaration, grouped with
  following `List<String> markers`/`whitespaces` declarations for column alignment. Root cause:
  `DeclarationAlignmentRuleCurly.containsMultilineBraceBody` (added for `RDD_KEY_225`) bailed
  `parseDeclaration` out of the alignment group whenever the initializer contained ANY multi-line
  brace pair, at any nesting depth — over-broad, since it also caught a SAFE shape: a flat,
  comma-separated array-literal argument list (no embedded statement) that only became multi-line
  because round1's own `enforceCallLineBreaking` wrapped it one-argument-per-line. `urlSpecs`
  joined its alignment group on round1 (still all on short lines) but dropped out on round2 (now
  sees round1's own multi-line wrapping and bails), narrowing the group and collapsing
  `markers`/`whitespaces`' shared padding from 3 spaces to 1. Fixed by narrowing
  `containsMultilineBraceBody` to only bail when a multi-line brace pair's interior ALSO contains a
  top-level `;` (a genuine multi-statement lambda/anonymous-class body, the original `RDD_KEY_225`
  concern) — via two parallel per-open-brace-level flag stacks (`sawNewline`/`sawSemi`), mirroring
  the `;`-presence check `isFlatAggregateInit` already uses for the same distinction. A flat
  array-literal list has no `;` inside its braces, so it no longer bails regardless of how many
  lines a later pass wraps it onto. Verified via a minimal repro extracted verbatim from the real
  declarations and against the real `Pep508RequirementTest.java` (same `/tmp/rewrite` corpus copy)
  — non-idempotent before, byte-identical round1/round2 after both. New fixture
  `test/real_code_regressions_207_{inp,out}.java`. `make test`: 314/314 -> 316/316 (fixtures 206 and
  207 registered together), zero regressions. See `RDD_KEY_291` in `RDD_LOG.md` for full detail.

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
  2026-08-07, seventh session on this gap (`applyDepthDerivedBodyIndent`, boundary +
  paren/bracket-continuation guards, nested-switch-opaque approach chosen over a rejected
  shared-accumulator approach — full two-approach writeup incl. rejected approach's failure mode
  in `RDD_KEY_251`, `RDD_LOG.md`). Verified against the originally-cited production file
  (`javaparser/javaparser`'s `ASTParser.java`, ~5500 lines): previously a confirmed non-converging
  2-cycle, now terminates with idempotency diff dropped from 369 lines to 7 (all 7 pre-existing,
  unrelated). `make test`: 247/247 → 248/248, zero regressions. New fixture:
  `test/real_code_regressions_181_{inp,out}.java`. `README.md`'s "Known Limitations" bullet
  removed.

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
- **`using` alias declarations not aligned** — FIXED (RDD_KEY_283), C++-only. Generic
  `parseDeclaration` rejected `using Name = Type;` (inverted grammar doesn't fit the forward `Type
  name = init;` model) — added isolated `Declaration.isUsingAlias` + `parseUsingAlias` path and a
  dedicated `renderUsingAliasGroup` (3-column grid aligning `=`), bailing out untouched on any
  `...` token to avoid colliding with C++26 pack-indexing/variadic-template spacing. `make test`
  283/283 -> 284/284, zero regressions. Fixture: `test/cpp_using_alias_{inp,out}.cpp`. Full
  root-cause/fix detail in RDD_LOG.md's `RDD_KEY_283`.
- **Pack-indexing/variadic-template `using` aliases still left unaligned** — FIXED (RDD_KEY_284,
  follow-up), C++-only. RDD_KEY_283's `...`-bail-out was broader than necessary; narrowed via new
  `Declaration.aliasRawTypeText`/`templatePrefixRawText` verbatim fields so only the truly
  unsafe prefix half stays unregenerated. Two more real bugs found via `make test` against
  `cpp26_comments`: a standalone comment between `template<...>` and `using` was silently dropped
  (now detected and bailed), and a trailing-comment spacing bug (one space instead of the
  codebase's two-space convention). `make test` unchanged at 284/284; `cpp26_comments` fixture
  updated in place. Full root-cause/fix detail in RDD_LOG.md's `RDD_KEY_284`.
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
  `real_code_regressions_120`. **2026-07-31 re-verification:** a separate tracker item wrongly
  described this bug as still open; re-verified against a fresh `/tmp/STL` checkout, byte-identical
  round1/round2, no residual case — confirmation-only, no code change.

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
  independent "Declaration-alignment column-padding non-idempotency" shape, after the
  `ranges.hpp`/`_Range` `trailingIndent` fix below). Root cause: width-exceeds-`lineLengthLimit`
  exclusion gated `isDefinition && hasBreakableCall(...)`, but non-definition `(params)` can also
  be wrapped by `enforceCallLineBreaking` → `=` column pads round1 / shrinks round2. Fixed:
  `hasBreakableParams = !isDefinition && paramsFrom < paramsTo` alongside `hasBreakableCall`.
  Verified fixture 124: round1 == round2. `make test` 172/172. Constructed fixture only. See
  RDD_KEY_203.

- **`ScopePipelineCore.trailingIndent` sweeping a same-line leading comment into a declaration/
  assignment group's per-line indent** — FIXED (partial fix for "Declaration-alignment
  column-padding non-idempotency", `ranges.hpp`/`_Range` shape only). Found in `microsoft/STL`
  (item 26, `ranges.hpp` `chunk_view`/etc.): same-line leading comment on group's first member
  duplicated onto every sibling next round. Root cause: `trailingIndent(gap)` returned text after
  last `\n` as-is with no pure-whitespace check, sweeping comment into the per-line join separator
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
