# STATE_rdd_log.md — Resolved Design Decisions Log


All resolved design decisions for the jxmake-code-formatter formatter.
Each row is tagged with a unique key for grep-based lookup.

**CLI usage:** to read a specific decision, run:
```
grep -Fm1 'RDD_KEY_n' util/CodingStyle.md/formatter/STATE_rdd_log.md
```
Use `-A 0` (default) since every decision is a single pipe-delimited line.

**Do NOT read this file in full** during a CLI session.
Look up only the specific key(s) referenced in STATE.md's RDD index.

---
| RDD_KEY_1 | Tokenizer | Fresh Java tokenizer, no external lexer library. |
| RDD_KEY_2 | Rule engine | Direct Java methods, grouped into logical rule classes (not one class per rule). |
| RDD_KEY_3 | Shared grid | Single `ColumnGrid` class shared across all rule classes. |
| RDD_KEY_4 | Modifier priority | Per-language priority maps (`CppModifierPriority`, `JavaModifierPriority`). |
| RDD_KEY_5 | Constants | All magic values as `private static final` in owning class. |
| RDD_KEY_6 | Java parsing | Tokenizer + recursive descent on bounded token slices; no AST. |
| RDD_KEY_7 | AI dependency | None — all Tier-1/Tier-2 rules deterministic via grid + recursion. |
| RDD_KEY_8 | JAR target | Single JAR, Java 8 source/target for broadest JVM compatibility. |
| RDD_KEY_9 | Server mode | Localhost HTTP + lockfile PID; amortizes JVM startup across batch. |
| RDD_KEY_10 | Server idempotency | Check lockfile first; if PID not alive treat as stale, delete and start fresh. |
| RDD_KEY_11 | Port | Default 17173, configurable; lockfile carries actual port used. |
| RDD_KEY_12 | Path separator | Use `File.separator` / `Path` API throughout — never hardcode `/`. |
| RDD_KEY_13 | Lockfile location | `~/.config/jxmake-code-formatter/server.lock` |
| RDD_KEY_14 | Line endings | `Config.lineEndings()` controls output; default `lf`; `preserve` keeps original. |
| RDD_KEY_15 | Config precedence | defaults → global → env → project → subdir → CLI. |
| RDD_KEY_16 | `.jxmake-code-formatter` inheritance | Full inheritance; child keys override parent. |
| RDD_KEY_17 | Multi-module Java imports | `java-import-depth = 2`; top-N components of `package` declaration. |
| RDD_KEY_18 | Windows support | Best-effort; PID liveness via `ProcessHandle` reflection, `kill -0` fallback on Unix only. |
| RDD_KEY_19 | Output modes | In-place (default), `--diff`, `--check`, `--out DIR`. |
| RDD_KEY_20 | Build | `Makefile` with `javac`, manifest, `jar`; no Maven/Gradle. |
| RDD_KEY_21 | `ColumnGrid` flush API | `flush()` returns rendered lines; `reset()` clears state for next group. |
| RDD_KEY_22 | §3.1 complexity padding algorithm | Recursive descent on token slice; `isLoose` propagates outward. |
| RDD_KEY_23 | Declaration-statement detection | Lead token must be a type KEYWORD or IDENTIFIER; OP lead (e.g. `++`) rejects. |
| RDD_KEY_24 | Column grid rendering | `max(len)` per column across group; pad each cell to column width. |
| RDD_KEY_25 | Static reorder vs. STYLE.md §5's worked example | Trust worked example; statics move to top of group unless name-dependency found in intervening tokens. |
| RDD_KEY_26 | §10 Single-expression block eligibility | Block must contain exactly one statement; lambda/anonymous-class bodies excluded. |
| RDD_KEY_27 | §11 K&R brace style detection | Control-flow keywords (`if`/`while`/`for`/`switch`) get K&R; function definitions get Allman. |
| RDD_KEY_28 | §11 lambda bodies also use K&R | Lambda `->` preceding `{` → K&R, same as control-flow. |
| RDD_KEY_29 | §12 else/else-if placement | `else`/`else if` go on their own line after `}`. |
| RDD_KEY_30 | C/C++ bitfield column (`STYLE_C_CPP.md` §6) | Bitfield width (`:<N>`) occupies its own column after the name column. |
| RDD_KEY_31 | §7 closing comments — key variable on nesting | Closing comment threshold is line count between matching braces. |
| RDD_KEY_32 | §7 closing comments — engine structure | Name stack push on `{`, pop on `}`; emit comment when line count ≥ threshold. |
| RDD_KEY_33 | §7 closing comments — named-construct blank lines | Force one blank line after `{` and before `}` for named constructs (class/struct/enum). |
| RDD_KEY_34 | §13 non-inline case brace wrapping | Non-inline case body gets its own `{`/`}` wrapping. |
| RDD_KEY_35 | §13 nested switch processing order | Inner switches processed before outer to avoid double-formatting. |
| RDD_KEY_36 | §13 inline switch row classification | Row is inline if it fits on one line within 100 chars. |
| RDD_KEY_37 | §13 fallthrough marking | `/* FALL-THROUGH */` inserted after last statement before next `case`/`default`. |
| RDD_KEY_38 | §14 getter/setter rendering | One-liner group rendered as adjacent lines; blank line separates groups. |
| RDD_KEY_39 | §14 getter/setter group detection | Adjacent one-liners, broken by blank line or comment. |
| RDD_KEY_40 | §3.2 keyword spacing | No space between control-flow keyword and `(`; `if(` → `if (`. |
| RDD_KEY_41 | §3.3 initializer brace spacing | `{` tight for empty `{}`; single-element `{ x }` loose; multi-element loose. |
| RDD_KEY_42 | §4 pre-increment rewrite | `i++` → `++i` unless post-increment semantics are required (value of expression used). |
| RDD_KEY_43 | §1 indentation scope | `indent-style = auto` cross-file detection deferred to `IndentationDetector.java`. |
| RDD_KEY_44 | §6 grouping and rendering | Assignment alignment groups broken by blank line; `=`/compound-op column aligned. |
| RDD_KEY_45 | §8 signature scope and rendering | Signature breaking applies to function definitions only (param list followed by `{`). |
| RDD_KEY_46 | §9 function-body detection and return scoping | Blank line before `return` only at function scope (depth 1), only in multi-line bodies. |
| RDD_KEY_47 | §15 comment scope and sentence detection | Comment rules apply to `//` and `/* */`; sentence detection via trailing `.`/`!`/`?`. |
| RDD_KEY_48 | §15 partial-implementation split | `enforceCommentStyle` split into sub-passes to allow targeted application. |
| RDD_KEY_49 | §15 multi-line block comment banner reformatting | `/*` banner lines normalized; content lines left verbatim. |
| RDD_KEY_50 | §15 separator alignment | `////...` dividers normalized to exactly `MIN_DIVIDER_SLASHES` slashes. |
| RDD_KEY_51 | §6 multi-line right-hand sides | Multi-line RHS preserves internal formatting; only the `=` column is aligned. |
| RDD_KEY_52 | §1 empty parameter list (`CppSpecificRule.java`) | `(void)` → `()` in C++; `()` → `(void)` in C. |
| RDD_KEY_53 | §2 one-liner scope (`CppSpecificRule.java`) | One-liner detection applies only to function definitions, not declarations. |
| RDD_KEY_54 | §9 section dividers are non-actionable | `////...` lines passed through unchanged; no content rules applied inside. |
| RDD_KEY_55 | §4 pointer/const spacing already satisfied | Tokenizer already separates `*`/`&`/`const` tokens; no additional spacing pass needed. |
| RDD_KEY_56 | §3 template angle-bracket spacing (`CppSpecificRule.java`) | Nested template `<>` gets loose padding when depth > 1. |
| RDD_KEY_57 | §10 header file structure (`CppSpecificRule.java`) | Zones: copyright → guard → includes → body; 2-blank-line separation enforced. |
| RDD_KEY_58 | §11 dropped from `CppSpecificRule.java` scope | No §11 "Include Ordering" section exists in STYLE_C_CPP.md; dropped from scope. |
| RDD_KEY_59 | `JavaSpecificRule.java` scoping | Implements Java §2 (Allman brace), §4 (double-brace init), §7 (import ordering). |
| RDD_KEY_60 | §2 Allman-conversion vs. getter/setter one-liner groups -- left unguarded | Allman pass ran before getter/setter grouping, destroying already-formatted groups. Left as-is pending RDD_KEY_75. |
| RDD_KEY_61 | §3.1 condition-interior padding -- wiring decision | `enforceConditionComplexityPadding` wired as a whole-file pass in `Formatter.java`. |
| RDD_KEY_62 | §3.1 condition-interior padding -- implementation | `ComplexityPaddingEvaluator.isLoose()` recursive descent; pad `( ... )` loose when loose. |
| RDD_KEY_63 | §2 method-definition Allman conversion (`JavaSpecificRule.java`) | Method definitions (param list + `{`) converted to Allman; constructors included. |
| RDD_KEY_64 | §4 array-declaration syntax parenthetical -- non-actionable | `int[] a` vs `int a[]` is a style preference not enforced by the formatter. |
| RDD_KEY_65 | §7 import group order/count contradiction | Trust worked example: `java, com, org, other, local, static` — 6 fixed buckets. |
| RDD_KEY_66 | `Main.java` orchestration architecture | Thin CLI entry point; delegates grouping to `ScopePipeline`, then calls whole-file enforceX passes directly. |
| RDD_KEY_67 | STYLE.md §5/§6 scope -- anywhere in code, recursively | Declaration and assignment alignment apply inside any block, recursively. |
| RDD_KEY_68 | `DeclarationAlignmentRule.splitStatements` depth-awareness fix | `splitStatements` must track brace/paren depth to avoid splitting inside nested blocks. |
| RDD_KEY_69 | §7 import ordering implementation (`JavaSpecificRule.java`) | Sort within each group alphabetically; emit groups in configured order with blank lines. |
| RDD_KEY_70 | `Config.java` file format | Properties file (`key = value`); `#` comments; unknown keys silently ignored. |
| RDD_KEY_71 | `Config.java` resolution scope | `resolve(Path file)` walks upward to find nearest `.jxmake-code-formatter`, merging with global config. |
| RDD_KEY_72 | `Formatter.java` orchestration architecture | `formatOne(source, config, lang)` runs `ScopePipeline.process` then sequential whole-file enforceX passes; called by both `Main` and `ServerMode`. |
| RDD_KEY_73 | `ServerMode.java` wire protocol | HTTP POST `/format` with JSON body `{path, source, lang}`; response JSON `{result}`; `/shutdown` for stop. |
| RDD_KEY_74 | `Formatter.java` whole-file pass order | Phase 0 (ScopePipeline) → Phase 1 (structural: Allman, empty-params, permits, call-breaking) → Phase 2 (spacing: complexity-padding, keyword-spacing, brace-spacing) → Phase 3 (switch) → Phase 4 (comments, blank-lines, pre-increment, header-guard). |
| RDD_KEY_75 | Supersedes RDD_KEY_60 -- Allman pass actually destroys §14 grouping, ordering alone insufficient | Fixed by adding one-liner adjacency heuristic in `CppSpecificRule`/`JavaSpecificRule` to detect and skip already-formatted getter/setter one-liner groups before Allman conversion. |
| RDD_KEY_76 | `DeclarationAlignmentRule` misparses a bare `++j;`/`--j;` statement as a fake field declaration | Fixed by also rejecting any lead token that is neither a valid type KEYWORD nor an IDENTIFIER in both `parseDeclaration` and `parseBitfield`. |
| RDD_KEY_77 | `MiscRule.enforceCommentStyle` relied on pipeline ordering (not detection) to skip closing-comment labels, breaking idempotency -- same flawed pattern as superseded RDD_KEY_60 | Fixed by adding `isClosingBraceLabelComment` structural detection; `enforceCommentStyle` skips `applyCommentTextRules` for `}` / `};`-preceded comment lines. |
| RDD_KEY_78 | `ScopePipeline.splitTopLevelSpans` never closed a span at a C++ access-specifier label (`public:`/`private:`/`protected:`), merging it into the following member | Fixed by adding `isAccessSpecifierLabel` check: span ends at a depth-0 `:` when the span's tokens-so-far are exactly one significant token equal to `public`/`private`/`protected`. |
| RDD_KEY_79 | `IndentationDetector.java` design (`indent-style = auto`) | Two public entry points: `detect(Path fileDir, Map<Path,String> cache)` and `detectFromContent(String source)`. Sampling: first indented line per file = one vote; cap at 10 files; majority vote; ties → `Config.DEFAULT_INDENT_STYLE`. Walk boundary: upward to first `.jxmake-code-formatter` / `.git` / `.hg`, ceiling at `user.home`. Caller-owned two-layer cache: server = in-memory `Map<Path,String>`; standalone = temp-file with `lastModified` freshness check. |
| RDD_KEY_80 | `ServerMode.java` idempotency check on a Java 8 build target -- `ProcessHandle` (Java 9+) is not available | Call `ProcessHandle` via reflection; fall back to `RuntimeMXBean.getName()` PID parse + `/proc/<pid>` check (Linux) or `kill -0` shell-out (other Unix) when running on Java 8. |
| RDD_KEY_81 | Allman-brace render-loop infinite loop when `)`/`{` are already adjacent (`CppSpecificRule.java`/`JavaSpecificRule.java`) | Fixed by inlining the brace's own append into the gap-hit branch (`out.append(tokens.get(braceIdx).text); i = braceIdx + 1;`), removing the dependency on a second loop pass to consume the brace. |
| RDD_KEY_82 | Phase ordering reversed -- `Main.java`/`README.md`/dogfood test deferred until after Phase 2 | Resolved by the user directly: dogfooding once, after both Tier-1/Tier-2 core and Phase-2 construct support exist, avoids running the self-dogfood pass twice and re-discovering/re-fixing the same class of latent bug twice. |
| RDD_KEY_83 | `JavaModifierPriority` column order for `abstract`/`sealed`/`non-sealed`/`final`/`volatile` | Single merged map satisfies all declaration kinds: `public/private/protected=0, static=1, abstract/final/sealed=2, volatile=3`. `non-sealed` deferred (lexes as three tokens). |
| RDD_KEY_84 | `record` named-construct detection through component list / `implements` clause / compact constructor | `bracketNameStack`/`pendingRecordName` captures record name when `(` immediately follows `record IDENTIFIER`, hands it to next `{`; `findRecordComponentListClose` handles `classifyNamed` lookback; `isCompactConstructorBrace` for compact canonical constructors. |
| RDD_KEY_85 | C++ concepts/`requires` clause implementation in `CppSpecificRule.java` | `concept` K&R brace already correct (KEYWORD before `{` fails `isCandidateSignatureName`). Added `"requires"`/`"concept"` to `KEYWORDS_CPP`; `"concept"` to `NAMED_CONSTRUCT_CPP`; `pendingConceptName` + `computeConceptHeaderName` for brace tagging; `isConceptRequiresExpressionBody` for `classifyNamed`; new `enforceRequiresClausePlacement` in `CppSpecificRule` wired into `Formatter.formatOne`. |
| RDD_KEY_86 | `MiscRule.java` call/declaration line-breaking (options 1 and 2) -- architecture from source inspection | `parseSignature` calls `significantOnly()`, stripping comments silently — safe for option 1, but option 2 MUST bypass `parseSignature` entirely and operate on the raw token stream. Option 1 reuses `parseSignature` + new `renderDropped`; option 2 uses new `enforceCallGroupPreservation` operating on raw tokens. Both wired in a single new `enforceCallLineBreaking` pass. |
| RDD_KEY_87 | `MiscRule.enforceCallLineBreaking` implementation scope decisions (nesting, comment bail-out, call-vs-declaration classification, new preserve-groups grid) + `collapseTokensToOneLine` bugfix | (1) Nesting: once `(` claimed, entire interior skipped for rest of scan. (2) Comments: any comment token between `(` and `)` disqualifies the whole candidate — left byte-for-byte untouched. (3) Call vs declaration: `parseSignature` success = forward declaration; failure = plain call. (4) Declaration preserve-groups grid: new `renderDeclarationPreserveGroups` groups params by original physical line, aligns by slot via `ColumnGrid`. Bugfix: `renderTokens` spread nested call parens apart (`bar ( 1, 2 )`); fixed by new `collapseTokensToOneLine` helper (whitespace-run-collapsing) used for all call-argument renderers; sibling args joined with normalized `", "`; each arg's internal tokens reproduced with original spacing preserved. |
| RDD_KEY_88 | `Main.java` implementation (Step 1.5) -- CLI parsing, config resolution, standalone indent-style temp-cache, server auto-connect/delegate, `--server`/`--stop`, output modes, exit codes | Temp-file cache: SHA-256 hex of boundary dir path, `/tmp/jxmake-code-formatter-indent-<hash>.cache`, content = style + `\n` + boundary dir `lastModified` ms. Three additive changes to already-COMPLETE files: `IndentationDetector.findBoundaryDir` widened to `public static`; new `ServerMode.findRunningServerPort()` and `ServerMode.stop()`. Real bug fixed: `ServerMode.start()` return type changed from `void` to `boolean` (true = freshly started, keep alive; false = already running or failed); `Main.run()` returns sentinel `SERVER_STARTED_KEEP_ALIVE = -1` to skip `System.exit`. `--diff` output: self-contained LCS diff (`computeDiffRuns`, O(n·m) DP), single hunk per file with context clamped to 3 lines. Two pre-existing gaps left unfixed (out of scope): `ServerMode.FormatHandler` doesn't resolve `indent-style = auto`; `ServerMode.FormatHandler` doesn't apply `Config.lineEndings()`. |
