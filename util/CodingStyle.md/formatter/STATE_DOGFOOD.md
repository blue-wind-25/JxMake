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
| Java | STATE_COMMON.md | re-run: dogfood-and-adopt (formatter's own `src/`, simplified, 2026-08-08) | NOT ADOPTED -- BUG FOUND | round1/round2 not clean: `rules/PowerShellSpecificRule.java`'s group-aligned trailing `//` comment keeps stale wide padding through round1, collapses to one space on round2 — same pass-ordering family as `JavaSpecificRule.isSingleLineBody`. Root-caused but not fixed this session (too risky to patch blind). 24/25 changed files content-diffed clean, round1 passed make test 261/261. **Not adopted**, real `src/` left untouched |
| Java | STATE_COMMON.md | re-run 2: dogfood-and-adopt (formatter's own `src/`, simplified, 2026-08-08, after manual workaround) | DONE | Two formatter-source fixes for the row above's trigger were tried and reverted (too risky, see STATE_C_CPP_JAVA.md Open Questions). Worked around instead: manually inserted blank lines between each `s = applyX(s); // comment` statement in `PowerShellSpecificRule.java`, breaking `applyAssignmentsPass`'s alignment-group membership (RDD_KEY_254) and sidestepping the trigger. Full re-run: round1/round2 empty diff, content-diff clean on all 26 changed files, adopted; make test 261/261, make test-server passed. **Underlying ordering bug stays open at the formatter-source level** — only this one trigger instance was removed |
| Java | STATE_COMMON.md | recurring self-format pass (src/**, XL.txt TIER 0 item 2, 2026-08-12) | DONE - BUG FOUND, FIXED | 93 files, round1/round2 idempotent. 13/93 differed from real `src/`; content-diffed all 13 — 9 OK, 4 flagged mismatches manually verified as known false-positive classes (brace-collapse, FALL-THROUGH switch-case comment), no real content loss. Found+fixed a real bug: `BlockStructureRule.indentBefore` returned `""` for a `catch`/`finally` on its own line after a `}` sitting mid-line (flush-left output) — rewrote to walk back to the containing line's actual start; see `real_code_regressions_203`/`real_code_regressions_168_out.kt`. Round1 adopted; make test 291/291 and make test-server both pass |
| Java | STATE_C_CPP_JAVA.md | VMA-GIT/anemonesoft (local) | DONE | |
| Kotlin | STATE_KOTLIN.md | arrow-kt/arrow | DONE | |
| Kotlin | STATE_KOTLIN.md | gui_frontend_android | DONE | |
| Kotlin | STATE_KOTLIN.md | JetBrains/kotlin | DONE | D3 fixed 2026-08-16 (RDD_KEY_298, see STATE_CURLY_GDR.md) — `FormatterCurly.formatOne` re-runs itself for Kotlin only until two consecutive passes converge; re-validated on the same 188-file `compiler/ir/backend.js/src` subtree the repro came from, 188/188 idempotent and syntax-clean. 2026-08-11: full 16153-file content-diff (15583 OK, 570 MISMATCH, all sampled as checker tolerance gaps on already-documented legitimate transforms, no new bug); RDD_KEY_278/279 validated at full-corpus scale |
| Kotlin | STATE_KOTLIN.md | kotlinx.coroutines | DONE | |
| Kotlin | STATE_KOTLIN.md | square/kotlinpoet | DONE | |
| Kotlin | STATE_KOTLIN.md | square/okio | DONE | |
| Kotlin | STATE_LINE_SPLIT_OP.md | square/okio (line-split-operator-priority elvis/tier1/tier3 sample, flag forced on) | DONE - PARTIAL FIX | 21-file sample; 1 bug found and fixed (array-subscript operator false-positive, D9/RDD_KEY_347); elvis/nullable-type safety and tier-1/tier-3 correctness confirmed on real code; 1 new gap documented not fixed (Kotlin return/assignment split never fires, time-boxed) |
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
| TS | STATE_LINE_SPLIT_OP.md | angular/angular (line-split-operator-priority optional-chaining/nullish/ternary/tier1/tier3 sample, flag forced on) | DONE - PARTIAL FIX | First TS-specific real-code dogfood for this feature (fresh shallow clone, 1788 real `.ts` files under `packages/*/src/**`, 27-file hand-selected sample at the tool's own default `line-length=100`, no adaptation needed); confirmed `?.`/`??`/optional-param safety and tier-1/tier-2 (genuine ternary) correctness on real code, tier-3 confirmed via a synthetic repro; found+fixed 1 real split-point bug, a TS-specific landmine (bare `:` with no preceding real ternary `?` mistaken for an else-branch, fixture real_code_regressions_241); round1/round2 idempotent except 1 already-known flap class recurring in this new corpus (not chased, see STATE_LINE_SPLIT_OP.md); js_ts_syntax_check.sh 26/27 clean, the 1 flagged file's errors confirmed pre-existing and flag-independent (unrelated bug, out of scope) |
| JSX | STATE_JS_TS.md | taniarascia/react-tutorial | DONE - FULL FIX | Real corpus is 5 `.js` files with embedded JSX, zero real `.jsx`. Original finding: as-shipped `.js` extension didn't trigger the boundary-finding pre-pass, corrupting/truncating content (4/5 files, incl. `Api.js`'s `{entry}`→`{entry;}`). FIXED 2026-08-13 (`Lang.isJsxSyntaxPath` widened to `.js`/`.mjs`/`.cjs`). Re-dogfooded: 5/5 clean, idempotent, no JSX content lost (checker itself still mis-flags 4/5, known non-awareness limitation, cross-checked manually) |
| JSX | STATE_JS_TS.md | ruanyf/react-demos | DONE | Step 2 Increment 5 (2026-08-14): real JSX lives almost entirely in `.html` `text/babel` `<script>` blocks (out of scope by design). Of standalone `.js` files, only `demo13/src/{browser,app}.js` have real JSX — idempotent, syntax-check clean. Found 1 unrelated pre-existing non-JSX bug (`demo13/app.js`, compiled/minified one-liner bodies non-idempotent under default GDR-off path), resolved via STATE_CURLY_GDR.md's multipass fixture (see row below) |
| JSX | STATE_CURLY_GDR.md | ruanyf/react-demos (demo13/app.js, GDR multipass) | DONE - FULL FIX | multipass=on fixes the compiled/minified one-liner Babel-helper bodies (`_createClass`, `_classCallCheck`, `_possibleConstructorReturn`, `_inherits`) found non-idempotent under single-pass GDR; fixture `test/curly_gdr_multipass_oneliner_{inp,out}.js` (in-file directive enabling both flags) |
| JSX | STATE_JS_TS.md | reactstrap/reactstrap | DONE - FULL FIX | Step 2 Increment 5 (2026-08-14): full 108-file `.js` set. Found+fixed a real content-corruption bug — JSX fragment shorthand (`<>...</>`) wasn't recognized as JSX (`parseJsxTag` required a tag-name unconditionally), letting its `{...}` fall through to statement formatting and gain a stray `;` (`DropdownToggle.js`). Fixed via an empty-string tagName sentinel; fixture `test/jsx_tsx_fragment_shorthand_{inp,out}.tsx`; full 108-file re-sweep idempotent, zero errors. `index.js`'s legacy Babel re-export syntax also fixed 2026-08-14 via a `js_ts_syntax_check.js` checker rewrite; full 197-file `src/` sweep 0/197 failures. **Re-dogfooded 2026-08-20 (RDD_KEY_313 follow-up):** residual round1/round2 diffs confirmed (disable-and-retest) as pre-existing hole-recursion instability, unrelated to the new pass; no regression |
| TSX | STATE_JS_TS.md | microsoft/TypeScript-React-Starter | DONE | 10 real `.tsx` files (`--preserve-tree` to avoid basename collisions). Idempotent (0-diff), content-diff 10/10 OK, syntax-check 10/10 clean, diffs cosmetic-only. Re-run 2026-08-14: same result. Re-run 2026-08-20 (RDD_KEY_313): still clean |
| TSX | STATE_JS_TS.md | lemoncode/react-typescript-samples | DONE - FULL FIX | RDD_KEY_313 (2026-08-20): full corpus round1/round2 idempotency sweep (JSX child-indentation parser follow-up). Found+fixed a real bug — `old_class_components_samples/15 Lazy Loading/.../memberForm.tsx` (and its `16 Custom Middleware/` sibling) has a JSX root tag with pre-existing tab+space-mixed indentation; `rewriteJsxChildIndentation` read it verbatim as `baseIndent` and baked it into every child line as literal tabs, while a separate later pass renormalized only the root line — mismatched parent/child indent styles, non-idempotent. Fixed via a tab-in-`baseIndent` bail; full one-pass idempotency for this narrow shape stays an accepted residual gap (converges after 2 passes) — see STATE_JS_TS.md's RDD_KEY_313 entry. No fixture (would itself fail the suite's idempotency check by design) |
| TSX | STATE_JS_TS.md | Lemoncode/react-typescript-samples | DONE | Step 2 Increment 5 (2026-08-14): 329-file corpus, sampled 15. All 15 idempotent, syntax-check 15/15 clean; 2 multi-line one-attribute-per-line JSX files confirmed pre-existing author formatting, not wrap-logic output |
| TSX | STATE_JS_TS.md | excalidraw/excalidraw | DONE - FULL FIX | Step 2 Increment 5 (2026-08-14): 303-file corpus, sampled 17. Found+fixed a second content-corruption bug — `enforceSemicolonInsertion`'s depth counter didn't track `TEMPLATE_HOLE_OPEN`/`CLOSE` (a template literal's `${...}` hole), so a multi-line hole (wrapped ternary) got a stray `;` right after `${` (`SearchMenu.tsx`). Fixed by treating hole boundaries like `(`/`)`; fixture `test/jsx_tsx_template_hole_wrap_{inp,out}.tsx`. Also fixed a `js_ts_syntax_check.sh` gap misreporting 6 other files as failures; post-fix full 17-file sample idempotent, 0/17 failures. **Re-dogfooded 2026-08-20 (RDD_KEY_313 follow-up, full 303-file corpus):** ~48 files diff round1/round2, same pre-existing hole-recursion instability as the `reactstrap` row (2 touch tag-boundary lines); unrelated to the new pass, no regression |
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
| Java | STATE_COMMON.md | recurring self-format pass (src/**, 2026-08-21) | DONE - NO BUG | 99 files, round1/round2 idempotent. 9/99 differed from committed `src/` (recently-touched files) — all hand-reviewed as ordinary cosmetic re-style (if/else collapse, call-wrap width, declaration-alignment column width, trailing-period comment stripping), zero content loss, zero flushed-left/dedented lines. Trial JAR from round1: `make _test_serial` 335/337 — the 2 failures (`test/cpp_comments_inp.cpp`, `test/real_code_regressions_217_inp.java`) confirmed pre-existing GRU-classifier/`gru-sync-weights` drift (byte-identical `--diff` vs. a JAR built from unmodified `src/`), same drift class as the 2026-08-16/2026-08-20 entries. round1b/round2b fixed-point check empty on all three. Adopted; `make clean && make test` 337/337, `make test-server` passed |
| Java | STATE_COMMON.md | recurring self-format pass (src/**, 2026-08-24) | DONE | 100 files, round1/round2 idempotent, round1b/round2b fixed-point empty. 22/100 differed from committed `src/` (files touched since the last pass) — all hand-reviewed, plus an explicit `detect_flushed_left_lines.py` scan (0 hits): ordinary cosmetic re-style (Javadoc trailing-period stripping, declaration-alignment column re-width, paren spacing, line-wrap reflow, a pre-existing multi-line-call-argument lambda-arrow-spacing quirk unrelated to this session), zero content loss. Trial JAR (bare `javac`+`jar`, no `gru-sync-weights`): `_test_serial` 340/347 — the 7 failures all comment-classification-dependent fixtures, confirmed as the established gru-weights-resolution artifact (same properly-built `target/` JAR passed 347/347 on the identical fixture set). Adopted; `make clean && make test` **347/347 forward + idempotency**, `make test-server` passed |
| Java | STATE_COMMON.md | recurring self-format pass (src/**, 2026-08-25, flag forced off) | DONE | 100 files (`*.java` only), round1/round2 idempotent. 6/100 differed from committed `src/` (exactly this session's own `line-split-by-operator-priority` rename/static-import/dedup edits) — all hand-reviewed: ordinary cosmetic re-style (paren/brace spacing, declaration-alignment column re-width, call-wrap reflow, a sentence-initial comment-capitalization quirk on two method names, closing-brace annotations), zero content loss, `detect_flushed_left_lines.py` 0 hits. `java_syntax_check.sh` 100/100 clean. Adopted; `make clean && make test` **363/363 forward + idempotency**. `tools/gru/*.java` (13 files) compiled cleanly against the rebuilt classes |
| Java/Python3 | STATE_COMMON.md | dogfood-and-adopt (../../JCS, ../../MDXplorer, ../../../3rd_party/tools/pcpp_java, ../../../3rd_party/tools/colordiff/colordiff.py, 2026-08-14) | DONE | 51 of 55 files copied back (.java/.py/.sh across 4 external corpora outside `formatter/`); round1/round2 idempotent; content-diff MISMATCHes on some Java/Python files (import reordering, declaration-alignment group recomputation, brace-presence changes) reviewed line-by-line and confirmed harmless re-styling, no token/string/comment-meaning loss; bash files syntax-check clean. Adopted and committed in JxMake repo root (commit 8739f2e), outside `formatter/`'s own history |
| Java/Python3/JS | STATE_COMMON.md | dogfood-and-adopt (tools/* — classifier_weights, gru, verifiers; simplified, 2026-08-08) | DONE | 40 files (13 .java, 18 .py, 9 .js); round1/round2 idempotent; content-diff clean on all 40; round1 adopted (8 files changed, trailing-period comment normalization only); make test unaffected (261/261) |
| Java/Python3/JS | STATE_COMMON.md | recurring self-format pass (tools/verifiers/* + formatter's own src/**, 2026-08-14) | DONE - BUG FOUND, FIXED | round1/round2 idempotent after fixes; content-diff MISMATCHes reviewed as legitimate re-styling (one-lining, import sort, alignment-group recomputation), no real corruption. Found+fixed 3 real bugs: (1) `BlockStructureRule.extractSingleIdentifier` dropped a leading `!` on negated conditions, corrupting hand-written trailing comments (RDD_KEY_289); (2) `BlockStructureRule.alignBracelessElseIfChain` misaligned a mixed braceless-if/braced-else-if chain via pure text-prefix matching with no braceless-body check (RDD_KEY_289); (3) `ScopePipelineIndent.applySingleStatementBody`'s "already compact" branch measured raw/padded text instead of normalized form, causing non-idempotent one-liner if/elif/else chains (RDD_KEY_288). Fixtures: real_code_regressions_204 (Python), real_code_regressions_205 (Java). Round1 adopted over both `tools/verifiers/*` and `src/**/*.java`; make test 314/314 (up from 312/312) |
| Java/Python3/JS/Bash | STATE_COMMON.md | recurring self-format pass (tools/*, XL.txt TIER 0 item 2, 2026-08-12) | DONE - BUG FOUND, FIXED | 69 files (.java/.py/.js/.sh); round1/round2 idempotent; content-diff clean on all 6 files with actual changes; syntax-checked clean. Surfaced a real formatter bug in `tools/gru/FilterAbstain.java`'s output (flush-left `catch`, see src/** row below); after fix, re-ran and re-adopted that one file. Round1 adopted over real `tools/*` |
| Java/Python3/JS/Bash | STATE_COMMON.md | recurring self-format pass (tools/*, 2026-08-21) | DONE - NO BUG | 82 files (.java/.py/.js/.sh); round1/round2 idempotent; content-diff clean; only 1 file differed from committed `tools/*` (`verifiers/js_ts_content_diff.js`, a single re-indented line matching its surrounding block, no content change). Round1 adopted. No flushed-left/dedented lines found |
| Java/Python3/JS/Bash | STATE_COMMON.md | recurring self-format pass (tools/*, 2026-08-24) | DONE - BUG FOUND, FIXED | 89 files (.java/.py/.js/.sh); round1/round2 idempotent; content-diff clean on all 46 Java/Python/JS files, bash syntax-check clean on all 43 `.sh` files. Content-diff surfaced a real JS/TS formatter bug in `tools/verifiers/json5_content_diff.js`'s `timestampNow()`: `enforceSemicolonInsertion` inserted a stray semicolon splitting a `return` of two template-literal halves joined by a leading `+` on the continuation line into two statements, discarding the second half. Root cause + fix: `LEADING_CONTINUATION_OPS` wrongly excluded bare `+`/`-`; real JS ASI never splits there (only postfix `++`/`--` is a genuine restricted production) — see RDD_KEY_339. Fixture `real_code_regressions_230`. 2 files adopted (the fixed `json5_content_diff.js`, plus a cosmetic Bash re-style in `makefile_content_diff.sh`). No flushed-left/dedented lines found. `make clean && make test` 346/346 -> 347/347 |
| Java/Python3/Bash | STATE_COMMON.md | dogfood-and-adopt (../../JCS, ../../MDXplorer, ../../../3rd_party/tools/pcpp_java, ../../../3rd_party/tools/colordiff/colordiff.py, 2026-08-21) | DONE - NO CHANGE | 61 files (.java/.py/.sh/.ps1; `.cmd` skipped, no formatter language support for Windows batch files); round1/round2 idempotent; every formatted file byte-identical (md5-verified sample) to its committed original — already at a fixed point from the 2026-08-14 adopt, no copy-back needed |
| Java/XML/Makefile | STATE_COMMON.md | dogfood-and-adopt (../../JCS, ../../MDXplorer, ../../../3rd_party/tools/pcpp_java, ../../../3rd_party/tools/colordiff/colordiff.py, 2026-08-22) | DONE | 61 files scanned (.java/.py/.sh/.ps1/.xml/Makefile; `.cmd` skipped, no formatter language support); round1/round2 idempotent; JCS/MDXplorer/colordiff.py still at the fixed point from prior runs, unchanged; pcpp_java had 4 files change: `Makefile`/`pom.xml` alignment column re-width, one raw BOM char literal made explicit, one comment's zero-width non-joiner bracketed — content-diff/syntax-check clean, adopted |
| Java/XML/Python3/Makefile | STATE_COMMON.md | dogfood-and-adopt (../../JCS, ../../MDXplorer, ../../../3rd_party/tools/pcpp_java, ../../../3rd_party/tools/colordiff/colordiff.py, 2026-08-24) | DONE - NO CHANGE | 63 files scanned (.java/.py/.sh/.ps1/.xml/Makefile; `.cmd`/`.jar`/`.md`/`.txt` skipped, no formatter language support or not source); round1/round2 idempotent; Java/Python/XML/Makefile content-diff clean, bash syntax-check clean; `detect_flushed_left_lines.py` clean (44 curly-family files scanned); every formatted file byte-identical to its committed original across all 4 repos — already at the fixed point from the 2026-08-22 adopt, no copy-back needed |
| Java/XML/Python3/Makefile | STATE_LINE_SPLIT_OP.md | Pass 1 Leg C: dogfood-and-adopt (../../JCS, ../../MDXplorer, ../../../3rd_party/tools/pcpp_java, ../../../3rd_party/tools/colordiff/colordiff.py, 2026-08-25, `line-split-operator-priority` flag off) | DONE - NO CHANGE | 62 files scanned (.java/.py/.xml/Makefile; `.cmd`/`.sh`/`.ps1` present-zero in this scan pass); round1/round2 idempotent; every formatted file byte-identical to its committed original across all 4 repos — still at the fixed point from the 2026-08-24 adopt, confirming the new operator-split feature is a true no-op with the flag at its default (off) |
| C/C++ | STATE_LINE_SPLIT_OP.md | fmtlib/fmt (line-split-operator-priority tier1/2/3 sample, flag forced on) | DONE - PARTIAL FIX | First C/C++-specific real-code dogfood for this feature (fresh clone, 17-file sample covering all 3 tiers + a real `for(...)` header split + the unary-`*` landmine, `line-length` lowered to 60 since fmt's own source is pre-wrapped under the tool's default 100-col limit); found+fixed 2 real split-point bugs (multi-declarator comma-list interleaving, pointer-type-before-`>` mistaken for multiplication, fixture real_code_regressions_239); round1/round2 idempotent except 2 already-known flap classes recurring in this new corpus (not chased, see STATE_LINE_SPLIT_OP.md); cpp_syntax_check.sh clean on all 17 files before and after |

**Note on `microsoft/TypeScript`'s status**: cluster #3's shared
braceless-collapse root cause (same as `angular/angular` cluster 4) and the
residual `applyAssignmentsPass` vs. `enforceCallLineBreaking` ordering
issue are both fixed — shared-curly-pipeline scope, not JS/TS-specific
(see STATE_C_CPP_JAVA.md Open Questions,
`ScopePipelineCurly.reapplyAssignmentsPassOnly`). The 2026-08-09
reconfirmation's 5 residual shapes (`watchPublic.ts` plus the 4 Tier-3
shapes, RDD_KEY_273-276) are all fixed too — see the table row above and
`STATE_JS_TS.md`'s "Dogfood: microsoft/TypeScript" section for per-shape
detail.

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
