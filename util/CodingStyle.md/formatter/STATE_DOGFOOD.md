# STATE_DOGFOOD.md — Dogfood Corpus Master Index

Master index of every dogfood corpus run (or planned) across all jobs, so a
session can check status at a glance without re-reading every `STATE_*.md`.
Not a substitute for a job's own `STATE_*.md` — full bug detail, root
causes, fixes, and `make test` counts live only there. Update this file's
row whenever a dogfood run starts, finishes, or is rejected, alongside the
job file's own detail.

## Status legend

| Status | Meaning |
|---|---|
| `NOT STARTED` | Planned/named as a future target, never actually run |
| `IN PROGRESS` | In progress |
| `DONE` | Run to completion, no known open bugs from this pass |
| `DONE - PARTIAL FIX` | Bug(s) found; at least one root cause still open, deferred, or attempted-and-reverted |
| `DONE - OPEN Q` | Run clean of bugs, but left a non-blocking design question unresolved |

`UNSUITABLE` corpora (rejected as unfit for use, not a completed-run status)
are listed separately at the bottom, not mixed into the main table.

**Table formatting note**: cells are intentionally *not* pipe-aligned (single
space around `|`, no column-width padding) — aligning by hand would force
every row to pad out to match the longest `Note` cell, and GitHub-flavored
markdown doesn't need aligned pipes to render correctly. Keep new rows
unaligned; don't re-align the whole table when adding one.

## Main table

Sorted by Language, then Status (DONE, DONE - PARTIAL FIX, DONE - OPEN Q, NOT STARTED).

| Language | Parent State File | Dogfood Name | Status | Note |
|---|---|---|---|---|
| C/C++ | STATE_C_CPP_JAVA.md | basvas-jkj/cpp_modules | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | blake-madden/tinyexpr-plusplus | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | boost-ext/ut | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | boostorg/mp11 | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | ericniebler/range-v3 | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | fmtlib/fmt | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | foonathan/lexy | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | martinus/nanobench | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | microsoft/proxy | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | microsoft/STL | DONE | all found gaps fixed; re-checked 2026-08-09, stale "open gaps" wording fixed |
| C/C++ | STATE_C_CPP_JAVA.md | NVIDIA/stdexec | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | serge-sans-paille/frozen | DONE | |
| C/C++ | STATE_CURLY_GDR.md | serge-sans-paille/frozen (GDR multipass) | DONE | multipass=on: 7/20 single-pass non-idempotent files → 0/20 |
| C/C++ | STATE_C_CPP_JAVA.md | taocpp/PEGTL | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | Tongsuo-Project/tongsuo-mini | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | user-reported `} // while` case | DONE | fixture: real_code_regressions_5 |
| C++26 | STATE_CPP26.md | ryanjk5.github.io (rjk-duck post) | DONE | 1 out-of-scope finding, not this job's |
| C++26 | STATE_CPP26.md | simdjson/experimental_json_builder | DONE | 1 bug found+fixed |
| C++26 | STATE_CPP26.md | stephenberry/glaze | DONE | 1 in-scope bug found+fixed (RDD_KEY_285, `json_patch_test.cpp` idempotency, `ScopePipelineCurly.applyOversizedAggregateInitClosingBracePass`); other findings out-of-scope C/C++/Java-job |
| C++26 | STATE_CPP26.md | wrocpp/cpp26-reflection-examples | DONE | |
| Java | STATE_C_CPP_JAVA.md | ../../../src minus jxm (vendored 3rd-party) | DONE | |
| Java | STATE_C_CPP_JAVA.md | ../../../src/jxm (JxMake's own Java tree) | DONE | |
| Java | STATE_C_CPP_JAVA.md | apache/ant | DONE | `PathTest.java` re-checked 2026-08-09, clean; `JikesOutputParser.java`'s reindent gap CLOSED 2026-08-16 -- idempotent/`javac`-clean with `curly-general-scope-reindent=on` AND `curly-general-scope-reindent-multipass=on` together (RDD_KEY_299) |
| Java | STATE_C_CPP_JAVA.md | ARMCortexMThumbC.java.in (local) | DONE | |
| Java | STATE_C_CPP_JAVA.md | google/google-java-format | DONE | |
| Java | STATE_C_CPP_JAVA.md | javaparser/javaparser | DONE | switch-case reindent bug fixed (RDD_KEY_251, nested-switch-in-switch, `ASTParser.java`); idempotency diff non-converging → 369 lines pre-fix → 7 post-fix. `ASTParser.java`'s remaining non-idempotent if/else reindent gap CLOSED 2026-08-16 (RDD_KEY_301, documentation-only — needs `curly-general-scope-reindent=on` AND `curly-general-scope-reindent-multipass=on` together, same resolution shape as RDD_KEY_299's `JikesOutputParser.java`) |
| Java | STATE_CURLY_GDR.md | javaparser/javaparser (javaparser-core, GDR multipass) | DONE | multipass=on: 93/576 single-pass non-idempotent files → 0/576, all pass java_syntax_check |
| Java | STATE_CURLY_GDR.md | javaparser/javaparser (javaparser-core-generators, GDR multipass) | DONE | multipass=on: 13/43 single-pass non-idempotent files → 0/43 |
| Java | STATE_CURLY_GDR.md | tool/JSONEncoderLite.java (local, GDR multipass) | DONE | multipass=on: 112-line single-pass non-idempotency diff → 0 |
| Java | STATE_C_CPP_JAVA.md | jenkinsci/jenkins | DONE | PluginManager.java fixed (RDD_KEY_225, pre-flight bail-out in DeclarationAlignmentRuleCurly.parseDeclaration); make test 224→225/225 |
| Java | STATE_C_CPP_JAVA.md | openrewrite/rewrite | DONE | all 6 idempotency clusters fixed (cluster 5 via STATEMENT_LEADING_KEYWORDS guard); full-tree re-run+syntax-check 2026-08-09 (fresh clone, 3510 files) found+fixed 1 new syntax-breaking bug (primitive-type declaration collapse, fixture real_code_regressions_187); 4 residual cosmetic idempotency diffs found — all 4 now fixed 2026-08-15 (RDD_KEY_290/291/293; the 4th, ScopePipelineCurly.applyDeclarationsPass's JSONEncoderLite.java drift, closed as no-longer-reproducing, RDD_KEY_292); fixtures real_code_regressions_206/207/208 |
| Java | STATE_C_CPP_JAVA.md | pcpp_java tool (local) | DONE | |
| Java | STATE_C_CPP_JAVA.md | RobotCoding gui_frontend | DONE | |
| Java | STATE_C_CPP_JAVA.md | self-dogfood (formatter's own src/) | DONE | |
| Java | STATE_COMMON.md | dogfood-and-adopt (formatter's own src/, real adoption, 2026-08-04) | DONE | round1/round2 idempotent, fixed point vs. original confirmed; round1 adopted over real `src/` (8 files, cosmetic-only diff); rebuilt JAR re-passed make test/test-server (244/244) |
| Java | STATE_COMMON.md | re-run: dogfood-and-adopt (formatter's own `src/`, simplified, 2026-08-08) | NOT ADOPTED -- BUG FOUND | round1/round2 idempotency NOT clean: `rules/PowerShellSpecificRule.java`, a group-aligned trailing `//` comment after a call wrapped by call-line-breaking keeps stale wide padding through round1, collapses to one space in round2 — same pass-ordering bug family as `JavaSpecificRule.isSingleLineBody`. Root-caused, not fixed this session (too risky to patch blind). 24/25 changed files content-diffed clean; round1 compiled and passed make test 261/261. **Not adopted** — real `src/` left untouched pending fix |
| Java | STATE_COMMON.md | re-run 2: dogfood-and-adopt (formatter's own `src/`, simplified, 2026-08-08, after manual workaround) | DONE | Two formatter-source fix attempts for the row above's trigger tried and reverted (too risky, see STATE_C_CPP_JAVA.md Open Questions). Instead, blank lines manually inserted between each `s = applyX(s); // comment` statement in `PowerShellSpecificRule.java`, breaking `applyAssignmentsPass`'s alignment-group membership (RDD_KEY_254) and sidestepping the trigger. Full re-run: round1/round2 empty diff; content-diff clean on all 26 changed files; adopted; make test 261/261, make test-server passed. **Underlying ordering bug remained open at the formatter-source level** — only this one trigger instance was removed |
| Java | STATE_COMMON.md | recurring self-format pass (src/**, XL.txt TIER 0 item 2, 2026-08-12) | DONE - BUG FOUND, FIXED | 93 files; round1/round2 idempotent. 13/93 differed from real `src/`; content-diffed all 13 — 9 OK, 4 flagged mismatches manually verified as known false-positive classes (brace-collapse, FALL-THROUGH switch-case comment), no real content loss. Found and fixed a real bug: `BlockStructureRule.indentBefore` returned `""` for a `catch`/`finally` on its own line after a `}` sitting mid-line, producing flush-left `catch`/`finally` — rewrote to walk back to the containing line's actual start; see `test/README.txt`'s `real_code_regressions_203` entry and `real_code_regressions_168_out.kt` correction. Round1 (fixed jar) adopted; make test 291/291 and make test-server both pass |
| Java | STATE_C_CPP_JAVA.md | VMA-GIT/anemonesoft (local) | DONE | |
| Kotlin | STATE_KOTLIN.md | arrow-kt/arrow | DONE | |
| Kotlin | STATE_KOTLIN.md | gui_frontend_android | DONE | |
| Kotlin | STATE_KOTLIN.md | JetBrains/kotlin | DONE | D3 fixed 2026-08-16 (RDD_KEY_298, see STATE_CURLY_GDR.md) — `FormatterCurly.formatOne` re-runs itself for Kotlin only until two consecutive passes converge; re-validated on the same 188-file `compiler/ir/backend.js/src` subtree the repro came from, 188/188 idempotent and syntax-clean. 2026-08-11: full 16153-file content-diff pass completed (15583 OK, 570 MISMATCH — all sampled as checker tolerance gaps on already-documented legitimate transforms, no new bug); RDD_KEY_278/279 validated at full-corpus scale |
| Kotlin | STATE_KOTLIN.md | kotlinx.coroutines | DONE | |
| Kotlin | STATE_KOTLIN.md | square/kotlinpoet | DONE | |
| Kotlin | STATE_KOTLIN.md | square/okio | DONE | |
| JSON/JSON5 | STATE_DATA_FORMATS.md | babel/babel | DONE | |
| JSON/JSON5 | STATE_DATA_FORMATS.md | eslint/eslint | DONE | |
| JSON/JSON5 | STATE_DATA_FORMATS.md | json5/json5 | DONE | |
| JSON/JSON5 | STATE_DATA_FORMATS.md | microsoft/vscode | DONE | |
| CSS | STATE_DATA_FORMATS.md | necolas/normalize.css | DONE | |
| CSS | STATE_DATA_FORMATS.md | primer/css | DONE | |
| CSS | STATE_DATA_FORMATS.md | twbs/bootstrap | DONE | |
| YAML | STATE_DATA_FORMATS.md | actions/starter-workflows | DONE | |
| YAML | STATE_DATA_FORMATS.md | ansible/ansible | DONE | |
| YAML | STATE_DATA_FORMATS.md | docker/compose | DONE | |
| YAML | STATE_DATA_FORMATS.md | home-assistant/core | DONE | |
| YAML | STATE_DATA_FORMATS.md | kubernetes/kubernetes | DONE | |
| YAML | STATE_DATA_FORMATS.md | prometheus/prometheus | DONE | |
| TOML | STATE_DATA_FORMATS.md | pola-rs/polars | DONE | |
| TOML | STATE_DATA_FORMATS.md | python-poetry/poetry | DONE | |
| TOML | STATE_DATA_FORMATS.md | rust-lang/cargo | DONE | |
| TOML | STATE_DATA_FORMATS.md | toml-lang/toml | DONE | |
| XML | STATE_DATA_FORMATS.md | apache/ant | DONE | |
| XML | STATE_DATA_FORMATS.md | apache/maven | DONE | |
| XML | STATE_DATA_FORMATS.md | jenkinsci/jenkins | DONE | |
| XML | STATE_DATA_FORMATS.md | w3c/svgwg | DONE | |
| HTML5 | STATE_DATA_FORMATS.md | apache/ant manual/ | DONE | running.html's `<p>`-loss gap fixed (RDD_KEY_236, 2026-08-03); re-verified clean of new tc-gap regressions 2026-08-02 (STATE_HTML5_TCG.md checklist item 1) |
| HTML5 | STATE_HTML5_TCG.md | alexandersandberg/html5-elements-tester | DONE | fully clean end-to-end (forward/round2/idempotency/syntax/content-diff), 2026-08-02 |
| HTML5 | STATE_DATA_FORMATS.md | web-platform-tests/wpt | DONE - PARTIAL FIX | deep tree-construction gaps deferred to STATE_HTML5_TCG.md; no new regression confirmed 2026-08-02 |
| HTML5 | STATE_DATA_FORMATS.md | wordpress/wordpress-develop | DONE | magic-comment capitalization question RESOLVED 2026-07-30 (`isSingleWordDirective`, see STATE_DATA_FORMATS.md); re-run as superset (303 files) by tc gap job 2026-08-02, no new tc-gap regressions |
| JS | STATE_JS_TS.md | expressjs/express | DONE | |
| JS | STATE_JS_TS.md | lodash/lodash | DONE | |
| TS | STATE_JS_TS.md | angular/angular | DONE | all clusters fixed: 1-3 fixed, 5 RESOLVED 2026-08-05 (3/3 files idempotent via curly-general-scope-reindent(-multipass), opt-in recommendation only), 4 fully fixed (RDD_KEY_248 resolved 7/9 residual files 2026-08-07; final 4 — `shared.ts`/`directive_outputs.ts` via RDD_KEY_269, `web_animations_player_spec.ts`/`input_transform.ts` via RDD_KEY_271) |
| TS | STATE_CURLY_GDR.md | angular/angular (cluster 5 files, GDR multipass) | DONE - FULL FIX | multipass=on: fixes user_metric_spec.ts + i18n_parse.ts (previously non-idempotent under single-pass GDR); emit.ts already idempotent single-pass, stays idempotent under multipass. Re-confirmed fresh 2026-08-05 (0-line diff on all 3, js_ts_syntax_check.sh exit 0) |
| TS | STATE_JS_TS.md | microsoft/TypeScript | DONE | 4/4 named clusters fixed, incl. `harness/collectionsImpl.ts` (RDD_KEY_270, fixture real_code_regressions_185). 2026-08-09 reconfirmation against a fresh clone (14 flagged files): 6/14 already clean; `compiler/watchPublic.ts`'s nested-array-literal `;`-corruption FIXED (fixture 194); remaining 7 split into 4 shapes, ALL FIXED: if/else body-padding CRLF-staleness (RDD_KEY_273, fixture 195), declaration `:`-column padding group-splitting (RDD_KEY_274, fixture 196), non-idempotent closing-brace `// if` comment (RDD_KEY_275, fixed as a side effect of RDD_KEY_273, fixture 197), interface field named `class` misclassifying its own nested object-type brace (RDD_KEY_276, fixture 198). XL.txt Tier 3 now "NONE FOR NOW" |
| TS | STATE_JS_TS.md | nestjs/nest | DONE | |
| TS | STATE_JS_TS.md | vuejs/core | DONE | switch-fallthrough idempotency bug FIXED 2026-08-07 (RDD_KEY_263) |
| JSX | STATE_JS_TS.md | taniarascia/react-tutorial | DONE - FULL FIX | Real corpus is 5 `.js` files with embedded JSX, zero real `.jsx`. Original finding: as-shipped `.js` extension did NOT trigger the boundary-finding pre-pass, causing content corruption/truncation (4/5 files, incl. `Api.js`'s `{entry}`→`{entry;}`). FIXED 2026-08-13 (`Lang.isJsxSyntaxPath` widened to `.js`/`.mjs`/`.cjs`, see STATE_JS_TS.md). Re-dogfooded: 5/5 clean, round1/round2 idempotent, every diff a legitimate style transform, no JSX content lost (checker itself still mis-flags 4/5, known non-awareness limitation, cross-checked manually) |
| JSX | STATE_JS_TS.md | ruanyf/react-demos | DONE | Step 2 Increment 5 (2026-08-14): real JSX lives almost entirely in `.html` `text/babel` `<script>` blocks (out of scope by design). Of standalone `.js` files, only `demo13/src/{browser,app}.js` have real JSX — idempotent, syntax-check clean. Found 1 unrelated pre-existing non-JSX bug (`demo13/app.js`, compiled/minified one-liner bodies non-idempotent under default GDR-off path) — resolved via STATE_CURLY_GDR.md's new multipass fixture (see row below) |
| JSX | STATE_CURLY_GDR.md | ruanyf/react-demos (demo13/app.js, GDR multipass) | DONE - FULL FIX | multipass=on fixes the compiled/minified one-liner Babel-helper bodies (`_createClass`, `_classCallCheck`, `_possibleConstructorReturn`, `_inherits`) found non-idempotent under single-pass GDR; fixture `test/curly_gdr_multipass_oneliner_{inp,out}.js` (in-file directive enabling both flags) |
| JSX | STATE_JS_TS.md | reactstrap/reactstrap | DONE - FULL FIX | Step 2 Increment 5 (2026-08-14): full 108-file `.js` set. Found+fixed a real content-corruption bug — JSX fragment shorthand (`<>...</>`) wasn't recognized as JSX (`parseJsxTag` required a tag-name unconditionally), letting its `{...}` fall through to statement formatting and gain a stray `;` (`DropdownToggle.js`). Fixed via an empty-string tagName sentinel; fixture `test/jsx_tsx_fragment_shorthand_{inp,out}.tsx`. Post-fix: full 108-file re-sweep idempotent, zero errors. `index.js`'s legacy Babel re-export syntax also fixed 2026-08-14 via a checker-only rewrite in `js_ts_syntax_check.js`; full 197-file `src/` sweep now 0/197 syntax-check failures |
| TSX | STATE_JS_TS.md | microsoft/TypeScript-React-Starter | DONE | 10 real `.tsx` files (`--preserve-tree` to avoid basename collisions). Idempotent (0-diff), content-diff 10/10 OK, syntax-check 10/10 clean, diffs cosmetic-only. Re-run 2026-08-14: same result |
| TSX | STATE_JS_TS.md | Lemoncode/react-typescript-samples | DONE | Step 2 Increment 5 (2026-08-14): 329-file corpus, sampled 15. All 15 idempotent, syntax-check 15/15 clean; 2 multi-line one-attribute-per-line JSX files confirmed pre-existing author formatting, not wrap-logic output |
| TSX | STATE_JS_TS.md | excalidraw/excalidraw | DONE - FULL FIX | Step 2 Increment 5 (2026-08-14): 303-file corpus, sampled 17. Found+fixed a second content-corruption bug — `enforceSemicolonInsertion`'s depth counter didn't track `TEMPLATE_HOLE_OPEN`/`CLOSE` (a template literal's `${...}` hole), so a multi-line hole (wrapped ternary) got a stray `;` right after `${` (`SearchMenu.tsx`). Fixed by treating hole boundaries like `(`/`)`; fixture `test/jsx_tsx_template_hole_wrap_{inp,out}.tsx`. Also fixed a `js_ts_syntax_check.sh` tooling gap misreporting 6 other files as failures. Post-fix: full 17-file sample idempotent, 0/17 failures |
| Python3 | STATE_PYTHON3.md | django/django | DONE | |
| Python3 | STATE_PYTHON3.md | pallets/click | DONE | |
| Python3 | STATE_PYTHON3.md | pallets/flask | DONE | |
| Python3 | STATE_PYTHON3.md | psf/black | DONE | |
| Python3 | STATE_PYTHON3.md | python/cpython | DONE | |
| JxMakeFile | STATE_JXMAKE.md | ../../../src/0-JxMake/lib/*.jxm | DONE | idempotent whole-corpus; 5 pre-existing standalone-compile failures reproduce identically unformatted |
| JxMakeFile | STATE_JXMAKE.md | ../../../{test,util/STM32Spec,hardware/**/Firmware/*}/{*.jxm,JxMakeFile} (2026-08-17, 80 files) | DONE | idempotent whole-corpus; original-vs-round1 `--__compile__` output byte-identical on all 80 files; several confirmed pre-existing/intentional non-clean files (GUI/X11-dependent, syntax-highlighter fixture, missing-include fixture, deprecation-warning fixture) — see STATE_JXMAKE.md Checklist for the breakdown |
| Makefile | STATE_TOOLING.md | serge-sans-paille/frozen | DONE | round1/round2 idempotent; `make -n` spot-check shows only pre-existing environment failures, same on originals — no formatter-induced breakage |
| Makefile | STATE_TOOLING.md | fmtlib/fmt | DONE | `support/Android.mk` run with frozen+PEGTL in the same batch; idempotent, no issue |
| Makefile | STATE_TOOLING.md | ericniebler/range-v3 | DONE | user re-cloned fresh 2026-08-09; repo genuinely has zero Makefile/makefile/*.mk (header-only CMake) — nothing to dogfood, not a gap |
| Makefile | STATE_TOOLING.md | taocpp/PEGTL | DONE | round1/round2 idempotent; `make -n` spot-check shows only pre-existing environment failures, same on originals |
| Makefile | STATE_TOOLING.md | python/cpython | DONE | user re-cloned fresh 2026-08-09, ran round1/round2 on *Makefile/*makefile/*.mk; diff -r empty, no bug |
| Makefile | STATE_TOOLING.md | local `mk.list` (user's personal repos, 226 Makefiles) | DONE | 2026-08-09, copied to `/tmp/mk_dogfood`; round1/round2 via --preserve-tree, diff -rq empty, no bug |
| Bash | STATE_TOOLING.md | javaparser/javaparser | DONE | 7 *.sh release/generator scripts; idempotent, bash -n clean, matching originals |
| Bash | STATE_TOOLING.md | jenkinsci/jenkins | DONE | `ath.sh` + 2 test-resource .sh; idempotent, bash -n clean, matching originals |
| Bash | STATE_TOOLING.md | wordpress/wordpress-develop | DONE | 3 tools/.devcontainer .sh; idempotent, bash -n clean, matching originals |
| Bash | STATE_TOOLING.md | nvm-sh/nvm | DONE | idempotent across all 5 .sh files (5766 lines total); bash -n clean, matching originals. Confirmed the naive brace-depth body reindent (STYLE_TOOLING.md 2.3) intentionally doesn't track if/then/else/fi/case nesting — documented scope, not a bug |
| Bash | STATE_TOOLING.md | acmesh-official/acme.sh | DONE | 276 .sh files; idempotent, bash -n clean, matching originals |
| Bash | STATE_TOOLING.md | ohmyzsh/ohmyzsh | DONE | 17 *.sh/.bash; found+fixed 3 real bugs (case-arm escaped-paren mismatch, root-mode `\'` mis-tokenizing, nested case...esac non-idempotency — fixtures real_code_regressions_188-189) plus 1 syntax-corruption bug (`>|` noclobber redirect split by pipe-spacing — fixture 190). After fixes: idempotent across all 17. `bash -n` still shows 10 pre-existing errors on original and round1 (5 files use zsh-only syntax under .sh/.bash, already invalid bash pre-formatting) — unchanged, out of scope. The one remaining real bug (pipe-spacing rule inserting a space inside a zsh extended-glob alternation, e.g. `(|.git)`) is now fixed 2026-08-16 via a shebang-based skip-gate (`FormatterBash.isBashCompatibleShebang`, `STATE_TOOLING.md`): all 4 known zsh-shebang files, including the one with the original misfire, are now skipped entirely rather than misformatted. Fixture `test/real_code_regressions_213_inp/out.sh`. Residual accepted gap: a shebang-less file using genuine zsh-only syntax is still not caught (deliberately permissive fallback) |
| PowerShell | STATE_TOOLING.md | PowerShell/PowerShell | DONE | round1/round2 clean except 1 idempotency bug: `.ForEach(` misdetected as `foreach` keyword, spurious space before `(`; fixed 2026-08-09 (lookbehind excludes preceding `.`), fixture real_code_regressions_191 |
| PowerShell | STATE_TOOLING.md | PowerShell/PSScriptAnalyzer | DONE | 2 idempotency/correctness bugs found+fixed (arm-vs-pipeline misclassification + pass ordering; bareword `/` path corruption); round1/round2 empty diff after fix, make test 252/252; fixture real_code_regressions_182 |
| PowerShell | STATE_TOOLING.md | actions/runner-images | DONE | round1/round2 clean, no diff, no fix needed |
| PowerShell | STATE_TOOLING.md | microsoft/azure-pipelines-tasks | DONE - FIXED | fresh full download (1123 .ps1) 2026-08-09; found 1 idempotency bug (KEYWORD_PAREN/kind[] misalignment after a standalone comment placeholder), fixed (RunBuffer tracks kind aligned to its own output, ChainCollector.resolveKind splices placeholders in lockstep); re-ran full corpus, diff empty; fixture real_code_regressions_192; make test 269/269 |
| Java/Python3 | STATE_COMMON.md | dogfood-and-adopt (../../JCS, ../../MDXplorer, ../../../3rd_party/tools/pcpp_java, ../../../3rd_party/tools/colordiff/colordiff.py, 2026-08-14) | DONE | 51 files copied back (of 55 total, .java/.py/.sh across 4 external corpora outside `formatter/`); round1/round2 idempotent; content-diff flagged some Java/Python files as MISMATCH (import reordering, declaration-alignment group recomputation, brace-presence changes), all manually reviewed line-by-line and confirmed harmless re-styling, no token/string/comment-meaning loss; bash files syntax-check clean. Adopted and committed in JxMake repo root (commit 8739f2e), outside `formatter/`'s own history |
| Java/Python3/JS | STATE_COMMON.md | dogfood-and-adopt (tools/* — classifier_weights, gru, verifiers; simplified, 2026-08-08) | DONE | 40 files (13 .java, 18 .py, 9 .js); round1/round2 idempotent; content-diff clean on all 40; round1 adopted (8 files changed, trailing-period comment normalization only); make test unaffected (261/261) |
| Java/Python3/JS | STATE_COMMON.md | recurring self-format pass (tools/verifiers/* + formatter's own src/**, 2026-08-14) | DONE - BUG FOUND, FIXED | round1/round2 idempotent on all files after fixes; content-diff MISMATCHes manually reviewed as legitimate re-styling (brace-presence/AST-order changes: one-lining, import sort, alignment-group recomputation), no real corruption. Found+fixed 3 real bugs: (1) `BlockStructureRule.extractSingleIdentifier` dropped a leading `!` on negated conditions, corrupting hand-written trailing comments (RDD_KEY_289); (2) `BlockStructureRule.alignBracelessElseIfChain` misaligned a mixed braceless-if/braced-else-if chain via pure text-prefix matching with no braceless-body check (RDD_KEY_289); (3) `ScopePipelineIndent.applySingleStatementBody`'s "already compact" branch measured raw/padded text instead of normalized form, causing non-idempotent one-liner if/elif/else chains (RDD_KEY_288). Fixtures: real_code_regressions_204 (Python), real_code_regressions_205 (Java). Round1 adopted over both `tools/verifiers/*` and `src/**/*.java`; make test 314/314 forward+idempotency (up from 312/312 baseline) |
| Java/Python3/JS/Bash | STATE_COMMON.md | recurring self-format pass (tools/*, XL.txt TIER 0 item 2, 2026-08-12) | DONE - BUG FOUND, FIXED | 69 files (.java/.py/.js/.sh); round1/round2 idempotent; content-diff clean on all 6 files with actual changes; syntax-checked clean. Surfaced a real formatter bug in `tools/gru/FilterAbstain.java`'s output (flush-left `catch`, see src/** row below); after fix, re-ran and re-adopted that one file. Round1 adopted over real `tools/*` |

**Note on `microsoft/TypeScript`'s status**: cluster #3's shared
braceless-collapse root cause (same as `angular/angular` cluster 4) is
fixed, as is the `applyAssignmentsPass` vs. `enforceCallLineBreaking`
ordering issue that was the residual cause here — shared-curly-pipeline
scope, not JS/TS-specific (see STATE_C_CPP_JAVA.md Open Questions,
`ScopePipelineCurly.reapplyAssignmentsPassOnly`). The 2026-08-09
reconfirmation found 5 residual shapes, all now fixed: `watchPublic.ts`'s
nested-array-literal corruption plus the 4 Tier-3 shapes (RDD_KEY_273-276)
— see the table row above and `STATE_JS_TS.md`'s "Dogfood:
microsoft/TypeScript" section for per-shape detail.

Corpus scope: `src/` only (601 real `.ts` files, 379045 lines) — excluded
`tests/cases/**` (20089 hand-authored compiler fixtures, incl.
deliberately-invalid-syntax cases) and `tests/baselines/**`
(auto-generated) as non-representative, same exclusion class as other jobs'
`built/`/`lib/` skips.

**AI (STATE_AI.md)**: N/A — this job's corpus concept is comment-text
training/labeling data (Pool A/Pool B ABSTAIN measurement), not a
source-formatting dogfood run. No comparable rows.

## Unsuitable (rejected as dogfood candidates)

| Language | Parent State File | Dogfood Name | Reason |
|---|---|---|---|
| C/C++ | STATE_C_CPP_JAVA.md | gcc-mirror/gcc | too massive to count LOC (ghloc.vercel.app) |
| C/C++ | STATE_C_CPP_JAVA.md | llvm/llvm-project | too massive to count LOC (ghloc.vercel.app) |
| C++26 | STATE_CPP26.md | bloomberg/clang-p2996 | repo empty/unusable |
| CSS | STATE_DATA_FORMATS.md | foundation/foundation-sites | all `.scss`, no plain CSS |
| HTML5 | STATE_DATA_FORMATS.md | kangax/html-minifier | rejected candidate — see parent file |
| HTML5 | STATE_DATA_FORMATS.md | mdn/content | rejected candidate — see parent file |
| HTML5 | STATE_DATA_FORMATS.md | twbs/bootstrap (docs) | rejected candidate — see parent file |
| HTML5 | STATE_DATA_FORMATS.md | whatwg/html | rejected candidate — see parent file |
| JS | STATE_JS_TS.md | nodejs/node | too massive to count LOC (ghloc.vercel.app) |
