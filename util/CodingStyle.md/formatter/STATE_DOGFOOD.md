# STATE_DOGFOOD.md — Dogfood Corpus Master Index

Master index of every dogfood corpus run (or planned) across all jobs — lets
a session check status at a glance without re-reading every `STATE_*.md`.
Not a substitute for a job's own `STATE_*.md`: full bug detail, root causes,
fixes, and `make test` counts live only there. Update this file's row
whenever a dogfood run starts, finishes, or is rejected — alongside the job
file's own detail.

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

**Table formatting note**: cells below are intentionally *not* pipe-aligned
(single space around `|`, no column-width padding) — hand-alignment would
force every row to pad out to match one long `Note` cell, growing over time.
GitHub-flavored markdown doesn't require aligned pipes to render correctly.
Keep new rows unaligned; don't re-align the whole table when adding one.

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
| C/C++ | STATE_C_CPP_JAVA.md | microsoft/STL | DONE | all found gaps fixed; re-checked 2026-08-09, "open gaps" header wording was stale |
| C/C++ | STATE_C_CPP_JAVA.md | NVIDIA/stdexec | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | serge-sans-paille/frozen | DONE | |
| C/C++ | STATE_CURLY_GDR.md | serge-sans-paille/frozen (GDR multipass) | DONE | curly-general-scope-reindent-multipass=on: 7/20 single-pass non-idempotent files → 0/20 |
| C/C++ | STATE_C_CPP_JAVA.md | taocpp/PEGTL | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | Tongsuo-Project/tongsuo-mini | DONE | |
| C/C++ | STATE_C_CPP_JAVA.md | user-reported `} // while` case | DONE | Fixture: real_code_regressions_5 |
| C++26 | STATE_CPP26.md | ryanjk5.github.io (rjk-duck post) | DONE | 1 out-of-scope finding, not this job's |
| C++26 | STATE_CPP26.md | simdjson/experimental_json_builder | DONE | 1 bug found+fixed |
| C++26 | STATE_CPP26.md | stephenberry/glaze | DONE | 1 in-scope bug found+fixed (RDD_KEY_285, `json_patch_test.cpp` round1/round2 idempotency, `ScopePipelineCurly.applyOversizedAggregateInitClosingBracePass`); several other findings out-of-scope C/C++/Java-job |
| C++26 | STATE_CPP26.md | wrocpp/cpp26-reflection-examples | DONE | |
| Java | STATE_C_CPP_JAVA.md | ../../../src minus jxm (vendored 3rd-party) | DONE | |
| Java | STATE_C_CPP_JAVA.md | ../../../src/jxm (JxMake's own Java tree) | DONE | |
| Java | STATE_C_CPP_JAVA.md | apache/ant | DONE - PARTIAL FIX | 1 file (`JikesOutputParser.java`) still hits accepted reindent gap; `PathTest.java` re-checked 2026-08-09, now clean |
| Java | STATE_C_CPP_JAVA.md | ARMCortexMThumbC.java.in (local) | DONE | |
| Java | STATE_C_CPP_JAVA.md | google/google-java-format | DONE | |
| Java | STATE_C_CPP_JAVA.md | javaparser/javaparser | DONE | switch-case reindent bug fixed (RDD_KEY_251, nested-switch-in-switch, `ASTParser.java`); idempotency diff: non-converging → 369 lines pre-fix → 7 post-fix, all 7 confirmed pre-existing/unrelated |
| Java | STATE_CURLY_GDR.md | javaparser/javaparser (javaparser-core, GDR multipass) | DONE | curly-general-scope-reindent-multipass=on: 93/576 single-pass non-idempotent files → 0/576, all pass java_syntax_check |
| Java | STATE_CURLY_GDR.md | javaparser/javaparser (javaparser-core-generators, GDR multipass) | DONE | curly-general-scope-reindent-multipass=on: 13/43 single-pass non-idempotent files → 0/43 |
| Java | STATE_CURLY_GDR.md | tool/JSONEncoderLite.java (local, GDR multipass) | DONE | curly-general-scope-reindent-multipass=on: 112-line single-pass non-idempotency diff → 0 |
| Java | STATE_C_CPP_JAVA.md | jenkinsci/jenkins | DONE | PluginManager.java fixed (RDD_KEY_225, pre-flight bail-out in DeclarationAlignmentRuleCurly.parseDeclaration); make test 224→225/225 |
| Java | STATE_C_CPP_JAVA.md | openrewrite/rewrite | DONE | all 6 idempotency clusters fixed (cluster 5 via STATEMENT_LEADING_KEYWORDS guard); full-tree re-run + syntax-check completed 2026-08-09 (fresh clone, 3510 files) — found and fixed 1 new syntax-breaking bug (primitive-type declaration collapse, fixture `real_code_regressions_187`); 4 residual cosmetic idempotency diffs left as accepted Known Gaps (no syntax/compile risk) |
| Java | STATE_C_CPP_JAVA.md | pcpp_java tool (local) | DONE | |
| Java | STATE_C_CPP_JAVA.md | RobotCoding gui_frontend | DONE | |
| Java | STATE_C_CPP_JAVA.md | self-dogfood (formatter's own src/) | DONE | |
| Java | STATE_COMMON.md | dogfood-and-adopt (formatter's own src/, real adoption, 2026-08-04) | DONE | round1/round2 idempotent, fixed point vs. original confirmed; round1 adopted over real `src/` (8 files, cosmetic-only diff); rebuilt JAR re-passed `make test`/`make test-server` (244/244) |
| Java/Python3/JS | STATE_COMMON.md | dogfood-and-adopt (tools/* -- classifier_weights, gru, verifiers; simplified, no round2-JAR fixed-point check, 2026-08-08) | DONE | 40 files (13 .java, 18 .py, 9 .js); round1/round2 idempotent (empty diff); content-diff clean on all 40 via java_content_diff.sh/python_content_diff.sh/js_ts_content_diff.sh; round1 adopted over real `tools/*` (8 files changed, trailing-period comment normalization only, STYLE.md #15); `make test` unaffected (261/261 before and after) |
| Java | STATE_COMMON.md | re-run: dogfood-and-adopt (formatter's own `src/`, simplified, no round2-JAR fixed-point check, 2026-08-08) | NOT ADOPTED -- BUG FOUND | round1/round2 idempotency check on real `src/` was NOT clean: 1 file, `rules/PowerShellSpecificRule.java`, 2 hunks -- a group-aligned trailing `//` comment (e.g. `); // §3.6 ...`) keeps its original wide alignment padding through round1 (call args got wrapped onto their own line by call-line-breaking, but the padding computed for the pre-wrap single-line shape isn't recomputed after the wrap) then collapses to a single space in round2 once the input already shows the multi-line shape -- same pass-ordering bug family as the documented `isSingleLineBody` note in `JavaSpecificRule.java` (comment-column alignment computed before/independent of `enforceCallLineBreaking`'s own line-length verdict). Root cause narrowed to the curly-family call-line-breaking / trailing-comment-alignment pass interaction but not fixed at this session's effort level (touches core alignment/line-breaking sequencing shared by C/C++/Java -- judged too risky to patch blind without dedicated investigation, per STATE_COMMON.md's own guidance on this class of bug). 24 of 25 changed files content-diffed clean (legitimate comment-capitalization/trailing-period-strip/line-wrap changes only, verified via `java_content_diff.sh`); round1 compiled clean and its rebuilt JAR passed `make test` 261/261 forward+idempotency regardless. **Not adopted** -- real `src/` left untouched pending a fix for the idempotency bug. |
| Java | STATE_COMMON.md | re-run 2: dogfood-and-adopt (formatter's own `src/`, simplified, no round2-JAR fixed-point check, 2026-08-08, after manual workaround) | DONE | Two formatter-source fix attempts for the row above's trigger were tried and reverted (too risky -- see `STATE_C_CPP_JAVA.md` Open Questions). Instead, blank lines were manually inserted between each `s = applyX(s); // comment` statement in `rules/PowerShellSpecificRule.java`'s `format()`, breaking `applyAssignmentsPass`'s alignment-group membership (RDD_KEY_254) and sidestepping the trigger shape without a formatter-source change. Verified idempotent in isolation first, then the full process re-run: round1/round2 `diff -ru` over all of real `src/` was empty; `java_content_diff.sh` on all 26 changed files showed only already-documented cosmetic classes (closing-brace annotation rewording, trailing-period stripping, list reflow); adopted over real `src/`; `make clean && make test` 261/261 forward+idempotency; `make test-server` all passed. **Underlying bug (`applyAssignmentsPass` vs. `enforceCallLineBreaking` ordering, non-JS/TS curly languages) remains open/unfixed at the formatter-source level** -- this only removes the one known trigger instance from `src/`; any other curly-family file with the same statement-chain shape could still trigger it. |
| Java/Python3/JS/Bash | STATE_COMMON.md | recurring self-format pass (tools/*, XL.txt TIER 0 item 2, 2026-08-12) | DONE - BUG FOUND, FIXED | 69 files (.java/.py/.js/.sh) formatted to `/tmp/tools_round1`/`/tmp/tools_round2`; round1/round2 idempotent (empty diff); content-diff clean on all 6 files with actual changes via the appropriate `*_content_diff.sh`; syntax-checked (`java_syntax_check.sh`/`python_syntax_check.sh`/`bash_syntax_check.sh`/`js_ts_syntax_check.sh`) clean. First pass surfaced a real formatter bug (see `src/**` row below) in `tools/gru/FilterAbstain.java`'s output (flush-left `catch`); after the formatter-source fix, re-ran and re-adopted just that one file. Round1 adopted over real `tools/*`. |
| Java | STATE_COMMON.md | recurring self-format pass (src/**, XL.txt TIER 0 item 2, 2026-08-12) | DONE - BUG FOUND, FIXED | 93 files formatted to `/tmp/fmt_round1`/`/tmp/fmt_round2`; round1/round2 idempotent (empty diff). 13 of 93 files differed from real `src/`; content-diffed all 13 via `java_content_diff.sh` -- 9 `OK: content preserved`, 4 flagged mismatches manually verified as known false-positive classes (single-statement brace-collapse, FALL-THROUGH switch-case comment annotation), no real content loss. `java_syntax_check.sh` clean on all 93. Found and fixed a real bug during this pass: `BlockStructureRule.indentBefore` returned `""` (no indent) for a `catch`/`finally` placed onto its own line after a `}` that itself sat mid-line (e.g. a single-line collapsed `try { ... } catch (...) { ... }`), producing a flush-left `catch`/`finally` -- rewrote it to walk back to the actual containing line's start rather than requiring the reference token to be line-first; see `test/README.txt`'s `real_code_regressions_203` entry and fixture `real_code_regressions_168_out.kt` correction (a pre-existing fixture had baked in the bug as its expected output). Round1 (built from the fixed jar) adopted over real `src/`; rebuilt (`make clean && make jar`) and confirmed `make test` (291/291 forward+idempotency) and `make test-server` both pass against the newly-adopted, self-formatted source. |
| Java | STATE_C_CPP_JAVA.md | VMA-GIT/anemonesoft (local) | DONE | |
| Kotlin | STATE_KOTLIN.md | arrow-kt/arrow | DONE | |
| Kotlin | STATE_KOTLIN.md | gui_frontend_android | DONE | |
| Kotlin | STATE_KOTLIN.md | JetBrains/kotlin | DONE - PARTIAL FIX | D3 still open — root cause confirmed, fix attempt reverted (28 fixture regressions); RDD_KEY_235 confirmed GDR/multipass doesn't resolve it (see STATE_CURLY_GDR.md). 2026-08-11: first full 16153-file content-diff pass completed end-to-end (15583 OK, 570 MISMATCH — all sampled as checker tolerance gaps on already-documented legitimate transforms, no new bug found); RDD_KEY_278/279 validated at full-corpus scale |
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
| TS | STATE_CURLY_GDR.md | angular/angular (cluster 5 files, GDR multipass) | DONE - FULL FIX | curly-general-scope-reindent-multipass=on: fixes user_metric_spec.ts + i18n_parse.ts (previously non-idempotent under single-pass GDR); emit.ts already idempotent under single-pass, stays idempotent under multipass. Re-confirmed fresh 2026-08-05 (0-line round1/round2 diff on all 3, js_ts_syntax_check.sh exit 0) |
| TS | STATE_JS_TS.md | microsoft/TypeScript | DONE | 4/4 named clusters fixed, incl. `harness/collectionsImpl.ts` (RDD_KEY_270, fixture real_code_regressions_185). 2026-08-09 reconfirmation against a fresh clone (14 previously-flagged files): 6/14 were already clean (fixed as side effects of other work); `compiler/watchPublic.ts`'s nested-array-literal `;`-corruption FIXED (fixture real_code_regressions_194); the remaining 7 split into 4 distinct shapes, ALL NOW FIXED: if/else body-padding CRLF-staleness (RDD_KEY_273, fixture 195), declaration `:`-column padding group-splitting (RDD_KEY_274, fixture 196), a non-idempotent closing-brace `// if` comment (RDD_KEY_275, fixed as a verified side effect of RDD_KEY_273, fixture 197), and an interface field named `class` misclassifying its own nested object-type brace (RDD_KEY_276, fixture 198). XL.txt Tier 3 now reads "NONE FOR NOW" |
| TS | STATE_JS_TS.md | nestjs/nest | DONE | |
| TS | STATE_JS_TS.md | vuejs/core | DONE | switch-fallthrough idempotency bug FIXED 2026-08-07 (RDD_KEY_263) |
| JSX | STATE_JS_TS.md | taniarascia/react-tutorial | DONE - FULL FIX | Real corpus is 5 `.js` files with embedded JSX, zero real `.jsx` files. Original finding: as-shipped `.js` extension did NOT trigger the boundary-finding pre-pass (extension-gated by design), causing genuine content corruption/truncation (`js_ts_syntax_check.sh` failed 4/5 files, incl. `Api.js`'s `{entry}`→`{entry;}` truncation). FIXED 2026-08-13 (STATE_JS_TS.md's "2026-08-13 implementation session — JSX-in-`.js`/`.ts` detection (LANDED)"): `Lang.isJsxSyntaxPath` widened unconditionally to `.js`/`.mjs`/`.cjs`. Re-dogfooded against a fresh clone: all 5 `.js` files now `js_ts_syntax_check.sh` 5/5 clean, round1/round2 idempotent (0-line diff), manual whitespace-stripped diff confirms every difference from the original is a legitimate expected style transform (arrow-param parens, semicolon insertion, closing comments) with no JSX content lost/garbled (checker tool itself still mis-flags 4/5 as MISMATCH — known JSX-non-awareness limitation, not a formatter bug, cross-checked manually). No template-literal-with-JSX content in this corpus (item 10 untested here). |
| JSX | STATE_JS_TS.md | ruanyf/react-demos | DONE | Step 2 Increment 5 (2026-08-14): real JSX lives almost entirely in `.html` `text/babel` `<script>` blocks (out of scope by design, `XmlSpecificRule.JS_SCRIPT_TYPES`); of the standalone `.js` files, only `demo13/src/{browser,app}.js` have real JSX — round1/round2 idempotent, `js_ts_syntax_check.sh` clean. Found 1 unrelated pre-existing non-JSX bug (`demo13/app.js`, compiled/minified one-liner bodies non-idempotent under default GDR-off path) — resolved via `STATE_CURLY_GDR.md`'s new real-world multipass fixture (`curly-general-scope-reindent-multipass=on` fixes it; see that job's row below) |
| JSX | STATE_CURLY_GDR.md | ruanyf/react-demos (demo13/app.js, GDR multipass) | DONE - FULL FIX | curly-general-scope-reindent-multipass=on fixes the compiled/minified one-liner Babel-helper bodies (`_createClass`, `_classCallCheck`, `_possibleConstructorReturn`, `_inherits`) found non-idempotent under single-pass GDR; real-world-derived fixture `test/curly_gdr_multipass_oneliner_{inp,out}.js` (in-file `JXM_CFMT_CFG` directive enabling both flags) |
| JSX | STATE_JS_TS.md | reactstrap/reactstrap | DONE - FULL FIX | Step 2 Increment 5 (2026-08-14): full 108-file `.js` set. Found and fixed a real content-corruption bug — JSX fragment shorthand (`<>...</>`) wasn't recognized as JSX at all (`parseJsxTag` required a tag-name IDENTIFIER unconditionally), letting its `{...}` hole fall through to ordinary JS statement formatting and get a stray `;` inserted (`DropdownToggle.js`). Fixed by giving fragments an empty-string tagName sentinel; fixture `test/jsx_tsx_fragment_shorthand_{inp,out}.tsx`. Post-fix: full 108-file re-sweep idempotent, zero errors. `index.js`'s `export X from 'Y'` legacy Babel re-export syntax (confirmed present on the original, not a formatter bug) also fixed 2026-08-14 via a checker-only rewrite in `js_ts_syntax_check.js` (see `STATE_JS_TS.md`); full 197-file `src/` sweep now 0/197 syntax-check failures |
| TSX | STATE_JS_TS.md | microsoft/TypeScript-React-Starter | DONE | 10 real `.tsx` files (incl. subdirectories, `--preserve-tree` used to avoid basename collisions). Round1/round2 idempotent (0-diff). `js_ts_content_diff.js` batch mode 10/10 OK. `js_ts_syntax_check.sh` 10/10 clean. Diffs are cosmetic-only (brace style, closing-brace comments, interface member `:` alignment). No template-literal-with-JSX content in this corpus (item 10 untested here). Re-run 2026-08-14 (Step 2 Increment 5) against a fresh clone: same result, no wrap-triggering line in this corpus |
| TSX | STATE_JS_TS.md | Lemoncode/react-typescript-samples | DONE | Step 2 Increment 5 (2026-08-14): 329-file corpus, sampled 15 across `hooks/`/`old_class_components_samples/` (seeded `shuf`). All 15 round1/round2 idempotent, `js_ts_syntax_check.sh` 15/15 clean; 2 files with multi-line one-attribute-per-line JSX confirmed via diff to be pre-existing author formatting, not wrap-logic output |
| TSX | STATE_JS_TS.md | excalidraw/excalidraw | DONE - FULL FIX | Step 2 Increment 5 (2026-08-14): 303-file corpus, sampled 17 (7 wrap-trigger candidates + 10 random). Found and fixed a second real content-corruption bug — `enforceSemicolonInsertion`'s depth counter didn't track `TEMPLATE_HOLE_OPEN`/`TEMPLATE_HOLE_CLOSE` (a template literal's `${...}` hole), so a multi-line hole (wrapped ternary) got a stray `;` inserted right after `${` (`SearchMenu.tsx`). Fixed by treating the hole boundaries like `(`/`)` in the depth counter; fixture `test/jsx_tsx_template_hole_wrap_{inp,out}.tsx`. Also found and fixed a `js_ts_syntax_check.sh` tooling gap (below) that was misreporting 6 other files in this sample as failures. Post-fix: full 17-file sample idempotent, 0/17 syntax-check failures |
| Python3 | STATE_PYTHON3.md | django/django | DONE | |
| Python3 | STATE_PYTHON3.md | pallets/click | DONE | |
| Python3 | STATE_PYTHON3.md | pallets/flask | DONE | |
| Python3 | STATE_PYTHON3.md | psf/black | DONE | |
| Python3 | STATE_PYTHON3.md | python/cpython | DONE | |
| Makefile | STATE_TOOLING.md | serge-sans-paille/frozen | DONE | round1/round2 idempotent (empty diff); `make -n` spot-check shows only pre-existing environment failures (missing sources/old compiler), same on unmodified originals -- no formatter-induced syntax breakage |
| Makefile | STATE_TOOLING.md | fmtlib/fmt | DONE | `support/Android.mk` run together with frozen+PEGTL in the same batch; idempotent, no formatter-induced issue |
| Makefile | STATE_TOOLING.md | ericniebler/range-v3 | DONE | user re-cloned fresh 2026-08-09; repo genuinely has zero `Makefile`/`makefile`/`*.mk` files (header-only CMake project) -- nothing to dogfood, not a tooling gap |
| Makefile | STATE_TOOLING.md | taocpp/PEGTL | DONE | round1/round2 idempotent (empty diff); `make -n` spot-check shows only pre-existing environment failures (missing sources/old compiler), same on unmodified originals -- no formatter-induced syntax breakage |
| Makefile | STATE_TOOLING.md | python/cpython | DONE | user re-cloned fresh 2026-08-09 and ran round1/round2 on its `*Makefile`/`*makefile`/`*.mk` files; `diff -r` empty (idempotent), no bug found |
| Makefile | STATE_TOOLING.md | local `mk.list` (user's personal repos, 226 Makefiles across TTGO_VGA32_Lite/eCxx/Business/JxMake/Shadow/etc.) | DONE | 2026-08-09, copied to `/tmp/mk_dogfood` (originals untouched) per `mk.list`; round1/round2 via `--preserve-tree`, `diff -rq` empty (idempotent), no bug found |
| Bash | STATE_TOOLING.md | javaparser/javaparser | DONE | 7 `*.sh` release/generator scripts, `/tmp/javaparser_gdr`; round1/round2 idempotent (empty diff), `bash -n` clean, matching originals -- no formatter-induced breakage |
| Bash | STATE_TOOLING.md | jenkinsci/jenkins | DONE | `ath.sh` + 2 test-resource `.sh`, `/tmp/jenkins_scope`; round1/round2 idempotent (empty diff), `bash -n` clean, matching originals -- no formatter-induced breakage |
| Bash | STATE_TOOLING.md | wordpress/wordpress-develop | DONE | 3 `tools/`/`.devcontainer/` `.sh`, `/tmp/wordpress-develop`; round1/round2 idempotent (empty diff), `bash -n` clean, matching originals -- no formatter-induced breakage |
| Bash | STATE_TOOLING.md | nvm-sh/nvm | DONE | round1/round2 idempotent (empty diff) across all 5 `.sh` files (nvm.sh, install.sh, test/common.sh, etc., 5766 lines total); `bash -n` clean on every round1 file, matching the unmodified originals -- no formatter-induced syntax breakage. Confirmed the naive brace-depth body reindent (STYLE_TOOLING.md 2.3 note) intentionally does not track `if`/`then`/`else`/`fi`/`case` nesting (only literal `{`/`}`), so e.g. an `if...else...fi` block inside a function body renders at the same indent as its own body lines -- this is the documented "byproduct of brace-depth counting" scope, not a bug |
| Bash | STATE_TOOLING.md | acmesh-official/acme.sh | DONE | full shallow clone, `/tmp/acme.sh`, 276 `.sh` files; round1/round2 idempotent (empty diff), `bash -n` clean, matching originals -- no formatter-induced breakage |
| Bash | STATE_TOOLING.md | ohmyzsh/ohmyzsh | DONE - PARTIAL FIX | shallow clone stripped to 17 `*.sh`/`.bash`, `/tmp/ohmyzsh`; found and fixed 3 real bugs (case-arm escaped-paren mismatch, root-mode `\'` backslash-escape mis-tokenizing as a string open, nested `case...esac` non-idempotency -- see `STATE_TOOLING.md`, fixtures `real_code_regressions_188`-`189`) plus 1 real syntax-corruption bug (`>|` noclobber redirect split into `> |` by pipe-spacing -- fixture `real_code_regressions_190`). After all four fixes: round1/round2 idempotent (empty diff) across all 17 files. `bash -n` shows 10 pre-existing error lines on both original and round1 (5 files use zsh-only syntax under a `.sh`/`.bash` extension -- extended-glob alternation, `${(kv)...}`, `always {}` blocks -- already invalid bash before any formatting); one of those files (`tools/upgrade.sh`) has the same pipe-spacing rule insert a space inside a zsh extended-glob alternation `(|.git)` -> `( | .git)` it can't distinguish from a real pipe, but since the file was never valid bash to begin with this isn't a new class of breakage -- documented as an accepted known gap (dialect detection out of scope) in `STATE_TOOLING.md` |
| PowerShell | STATE_TOOLING.md | PowerShell/PowerShell | DONE | round1/round2 clean except 1 idempotency bug: `.ForEach(` method call misdetected as `foreach` keyword by `KEYWORD_PAREN`, gained a spurious space before `(`; fixed 2026-08-09 (lookbehind now excludes preceding `.`), fixture `real_code_regressions_191` |
| PowerShell | STATE_TOOLING.md | PowerShell/PSScriptAnalyzer | DONE | 2 idempotency/correctness bugs found and fully fixed (arm-vs-pipeline misclassification + pass ordering; bareword `/` path corruption) -- round1/round2 empty diff after fix, `make test` 252/252; see STATE_TOOLING.md, fixture real_code_regressions_182 |
| PowerShell | STATE_TOOLING.md | actions/runner-images | DONE | round1/round2 clean, no diff, no fix needed |
| PowerShell | STATE_TOOLING.md | microsoft/azure-pipelines-tasks | DONE - FIXED | user completed a fresh full download (1123 `.ps1` files) 2026-08-09 and ran round1/round2; found 1 real idempotency bug (`KEYWORD_PAREN`/`kind[]` misalignment after a standalone comment placeholder). Follow-up session fixed it: `RunBuffer` now tracks `kind` aligned to its own output, `ChainCollector.resolveKind` splices placeholder substitutions in lockstep (see STATE_TOOLING.md); re-ran round1/round2 on the full 1123-file corpus, diff empty; `real_code_regressions_192` fixture added; `make test` 269/269 |

**Note on `microsoft/TypeScript`'s status**: cluster #3's shared braceless-
collapse root cause (same as `angular/angular` cluster 4) is fixed.
`applyAssignmentsPass` vs. `enforceCallLineBreaking` ordering (formerly the
residual cause here) is also now fixed — shared-curly-pipeline scope, not
JS/TS-specific (see STATE_C_CPP_JAVA.md Open Questions,
`ScopePipelineCurly.reapplyAssignmentsPassOnly`). The 2026-08-09
reconfirmation found 5 residual shapes, ALL now fixed: `watchPublic.ts`'s
nested-array-literal corruption plus the 4 Tier-3 shapes (RDD_KEY_273-276)
— see table row above and `STATE_JS_TS.md`'s "Dogfood: microsoft/TypeScript"
section for per-shape detail.

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
| C++26 | STATE_CPP26.md | bloomberg/clang-p2996 | repo empty/unusable |
| C/C++ | STATE_C_CPP_JAVA.md | gcc-mirror/gcc | too massive to count LOC (ghloc.vercel.app) |
| C/C++ | STATE_C_CPP_JAVA.md | llvm/llvm-project | too massive to count LOC (ghloc.vercel.app) |
| JS | STATE_JS_TS.md | nodejs/node | too massive to count LOC (ghloc.vercel.app) |
| CSS | STATE_DATA_FORMATS.md | foundation/foundation-sites | all `.scss`, no plain CSS |
| HTML5 | STATE_DATA_FORMATS.md | kangax/html-minifier | rejected candidate — see parent file |
| HTML5 | STATE_DATA_FORMATS.md | mdn/content | rejected candidate — see parent file |
| HTML5 | STATE_DATA_FORMATS.md | twbs/bootstrap (docs) | rejected candidate — see parent file |
| HTML5 | STATE_DATA_FORMATS.md | whatwg/html | rejected candidate — see parent file |
