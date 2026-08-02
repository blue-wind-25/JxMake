# STATE_DOGFOOD.md — Dogfood Corpus Master Index

Master reference of every real-world dogfood corpus run (or planned) across
all jobs. Not a substitute for a job's own `STATE_*.md` — full bug detail,
root causes, fixes, and `make test` counts still live only there. This file
exists so a future session (or the user) can look up, at a glance, what's
already been run/fixed/rejected/still pending, without re-reading every
`STATE_*.md` in full. When starting, finishing, or rejecting a dogfood run,
update this file's row alongside the job file's own detail.

## Status legend

| Status               | Meaning                                                                               |
|----------------------|---------------------------------------------------------------------------------------|
| `NOT STARTED`        | Planned/named as a future target, never actually run                                  |
| `IN PROGRESS`        | In progress                                                                           |
| `DONE`               | Run to completion, no known open bugs from this pass                                  |
| `DONE - PARTIAL FIX` | Bug(s) found; at least one root cause still open, deferred, or attempted-and-reverted |
| `DONE - OPEN Q`      | Run clean of bugs, but left a non-blocking design question unresolved                 |

`UNSUITABLE` corpora (rejected as unfit for use, not a status of a completed
run) are listed in the separate table at the bottom, not mixed into the
main table.

## Main table

Sorted by Language, then Status (DONE, DONE - PARTIAL FIX, DONE - OPEN Q, NOT STARTED).

| Language   | Parent State File     | Dogfood Name                                | Status             | Note                                                                                                                                      |
|------------|-----------------------|---------------------------------------------|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| C/C++      | STATE_C_CPP_JAVA.md   | basvas-jkj/cpp_modules                      | DONE               |                                                                                                                                           |
| C/C++      | STATE_C_CPP_JAVA.md   | blake-madden/tinyexpr-plusplus              | DONE               |                                                                                                                                           |
| C/C++      | STATE_C_CPP_JAVA.md   | boost-ext/ut                                | DONE               |                                                                                                                                           |
| C/C++      | STATE_C_CPP_JAVA.md   | boostorg/mp11                               | DONE               |                                                                                                                                           |
| C/C++      | STATE_C_CPP_JAVA.md   | ericniebler/range-v3                        | DONE               |                                                                                                                                           |
| C/C++      | STATE_C_CPP_JAVA.md   | fmtlib/fmt                                  | DONE               |                                                                                                                                           |
| C/C++      | STATE_C_CPP_JAVA.md   | foonathan/lexy                              | DONE               |                                                                                                                                           |
| C/C++      | STATE_C_CPP_JAVA.md   | gcc-mirror/gcc                              | NOT STARTED        | TOO MASSIVE                                                                                                                               |
| C/C++      | STATE_C_CPP_JAVA.md   | llvm/llvm-project                           | NOT STARTED        | TOO MASSIVE                                                                                                                               |
| C/C++      | STATE_C_CPP_JAVA.md   | martinus/nanobench                          | DONE               |                                                                                                                                           |
| C/C++      | STATE_C_CPP_JAVA.md   | microsoft/proxy                             | DONE               |                                                                                                                                           |
| C/C++      | STATE_C_CPP_JAVA.md   | microsoft/STL                               | DONE - PARTIAL FIX | documented open gaps                                                                                                                      |
| C/C++      | STATE_C_CPP_JAVA.md   | NVIDIA/stdexec                              | DONE               |                                                                                                                                           |
| C/C++      | STATE_C_CPP_JAVA.md   | serge-sans-paille/frozen                    | DONE               |                                                                                                                                           |
| C/C++      | STATE_CURLY_GDR.md    | serge-sans-paille/frozen (GDR multipass)    | DONE               | curly-general-scope-reindent-multipass=on: 7/20 single-pass non-idempotent files → 0/20                                                  |
| C/C++      | STATE_C_CPP_JAVA.md   | taocpp/PEGTL                                | DONE               |                                                                                                                                           |
| C/C++      | STATE_C_CPP_JAVA.md   | Tongsuo-Project/tongsuo-mini                | DONE               |                                                                                                                                           |
| C/C++      | STATE_C_CPP_JAVA.md   | user-reported `} // while` case             | DONE               | Fixture: real_code_regressions_5                                                                                                          |
| C++26      | STATE_CPP26.md        | ryanjk5.github.io (rjk-duck post)           | DONE               | 1 out-of-scope finding, not this job's                                                                                                    |
| C++26      | STATE_CPP26.md        | simdjson/experimental_json_builder          | DONE               | 1 bug found+fixed                                                                                                                         |
| C++26      | STATE_CPP26.md        | stephenberry/glaze                          | DONE               | zero in-scope bugs                                                                                                                        |
| C++26      | STATE_CPP26.md        | wrocpp/cpp26-reflection-examples            | DONE               |                                                                                                                                           |
| Java       | STATE_C_CPP_JAVA.md   | ../../../src minus jxm (vendored 3rd-party) | DONE               |                                                                                                                                           |
| Java       | STATE_C_CPP_JAVA.md   | ../../../src/jxm (JxMake's own Java tree)   | DONE               |                                                                                                                                           |
| Java       | STATE_C_CPP_JAVA.md   | apache/ant                                  | DONE - PARTIAL FIX | 2 files hit accepted reindent gap                                                                                                         |
| Java       | STATE_C_CPP_JAVA.md   | ARMCortexMThumbC.java.in (local)            | DONE               |                                                                                                                                           |
| Java       | STATE_C_CPP_JAVA.md   | google/google-java-format                   | DONE               |                                                                                                                                           |
| Java       | STATE_C_CPP_JAVA.md   | javaparser/javaparser                       | DONE               |                                                                                                                                           |
| Java       | STATE_CURLY_GDR.md    | javaparser/javaparser (javaparser-core, GDR multipass) | DONE    | curly-general-scope-reindent-multipass=on: 93/576 single-pass non-idempotent files → 0/576, all pass java_syntax_check                   |
| Java       | STATE_CURLY_GDR.md    | javaparser/javaparser (javaparser-core-generators, GDR multipass) | DONE | curly-general-scope-reindent-multipass=on: 13/43 single-pass non-idempotent files → 0/43                                                 |
| Java       | STATE_CURLY_GDR.md    | tool/JSONEncoderLite.java (local, GDR multipass) | DONE         | curly-general-scope-reindent-multipass=on: 112-line single-pass non-idempotency diff → 0                                                 |
| Java       | STATE_C_CPP_JAVA.md   | jenkinsci/jenkins                           | DONE - PARTIAL FIX | PluginManager.java: reproducible (corrected 2026-07-31), now a stable ~1992-char unwrapped line, not a round-flap; root cause undiagnosed |
| Java       | STATE_C_CPP_JAVA.md   | openrewrite/rewrite                         | DONE - PARTIAL FIX | cluster 5 still open                                                                                                                      |
| Java       | STATE_C_CPP_JAVA.md   | pcpp_java tool (local)                      | DONE               |                                                                                                                                           |
| Java       | STATE_C_CPP_JAVA.md   | RobotCoding gui_frontend                    | DONE               |                                                                                                                                           |
| Java       | STATE_C_CPP_JAVA.md   | self-dogfood (formatter's own src/)         | DONE               |                                                                                                                                           |
| Java       | STATE_C_CPP_JAVA.md   | VMA-GIT/anemonesoft (local)                 | DONE               |                                                                                                                                           |
| Kotlin     | STATE_KOTLIN.md       | arrow-kt/arrow                              | DONE               |                                                                                                                                           |
| Kotlin     | STATE_KOTLIN.md       | gui_frontend_android                        | DONE               |                                                                                                                                           |
| Kotlin     | STATE_KOTLIN.md       | JetBrains/kotlin                            | DONE - PARTIAL FIX | D3 still open — root cause confirmed, attempted fix reverted (28 fixture regressions); RDD_KEY_235 confirmed GDR/multipass does not resolve it (see STATE_CURLY_GDR.md) |
| Kotlin     | STATE_KOTLIN.md       | kotlinx.coroutines                          | DONE               |                                                                                                                                           |
| Kotlin     | STATE_KOTLIN.md       | square/kotlinpoet                           | DONE               |                                                                                                                                           |
| Kotlin     | STATE_KOTLIN.md       | square/okio                                 | DONE               |                                                                                                                                           |
| JSON/JSON5 | STATE_DATA_FORMATS.md | babel/babel                                 | DONE               |                                                                                                                                           |
| JSON/JSON5 | STATE_DATA_FORMATS.md | eslint/eslint                               | DONE               |                                                                                                                                           |
| JSON/JSON5 | STATE_DATA_FORMATS.md | json5/json5                                 | DONE               |                                                                                                                                           |
| JSON/JSON5 | STATE_DATA_FORMATS.md | microsoft/vscode                            | DONE               |                                                                                                                                           |
| CSS        | STATE_DATA_FORMATS.md | necolas/normalize.css                       | DONE               |                                                                                                                                           |
| CSS        | STATE_DATA_FORMATS.md | primer/css                                  | DONE               |                                                                                                                                           |
| CSS        | STATE_DATA_FORMATS.md | twbs/bootstrap                              | DONE               |                                                                                                                                           |
| YAML       | STATE_DATA_FORMATS.md | actions/starter-workflows                   | DONE               |                                                                                                                                           |
| YAML       | STATE_DATA_FORMATS.md | ansible/ansible                             | DONE               |                                                                                                                                           |
| YAML       | STATE_DATA_FORMATS.md | docker/compose                              | DONE               |                                                                                                                                           |
| YAML       | STATE_DATA_FORMATS.md | home-assistant/core                         | DONE               |                                                                                                                                           |
| YAML       | STATE_DATA_FORMATS.md | kubernetes/kubernetes                       | DONE               |                                                                                                                                           |
| YAML       | STATE_DATA_FORMATS.md | prometheus/prometheus                       | DONE               |                                                                                                                                           |
| TOML       | STATE_DATA_FORMATS.md | pola-rs/polars                              | DONE               |                                                                                                                                           |
| TOML       | STATE_DATA_FORMATS.md | python-poetry/poetry                        | DONE               |                                                                                                                                           |
| TOML       | STATE_DATA_FORMATS.md | rust-lang/cargo                             | DONE               |                                                                                                                                           |
| TOML       | STATE_DATA_FORMATS.md | toml-lang/toml                              | DONE               |                                                                                                                                           |
| XML        | STATE_DATA_FORMATS.md | apache/ant                                  | DONE               |                                                                                                                                           |
| XML        | STATE_DATA_FORMATS.md | apache/maven                                | DONE               |                                                                                                                                           |
| XML        | STATE_DATA_FORMATS.md | jenkinsci/jenkins                           | DONE               |                                                                                                                                           |
| XML        | STATE_DATA_FORMATS.md | w3c/svgwg                                   | DONE               |                                                                                                                                           |
| HTML5      | STATE_DATA_FORMATS.md | apache/ant manual/                          | DONE - PARTIAL FIX | 1 unfixed gap: running.html loses a `<p>`; re-verified clean of new tc-gap regressions 2026-08-02 (STATE_HTML5_TCG.md checklist item 1)   |
| HTML5      | STATE_HTML5_TCG.md    | alexandersandberg/html5-elements-tester     | DONE               | fully clean end-to-end (forward/round2/idempotency/syntax/content-diff), 2026-08-02                                                       |
| HTML5      | STATE_DATA_FORMATS.md | web-platform-tests/wpt                      | DONE - PARTIAL FIX | deep tree-construction gaps deferred to STATE_HTML5_TCG.md; no new regression confirmed 2026-08-02                                        |
| HTML5      | STATE_DATA_FORMATS.md | wordpress/wordpress-develop                 | DONE - OPEN Q      | magic-comment capitalization question; re-run as superset (303 files) by tc gap job 2026-08-02, no new tc-gap regressions                 |
| JS         | STATE_JS_TS.md        | expressjs/express                           | DONE               |                                                                                                                                           |
| JS         | STATE_JS_TS.md        | lodash/lodash                               | DONE               |                                                                                                                                           |
| JS         | STATE_JS_TS.md        | nodejs/node                                 | NOT STARTED        | TOO MASSIVE                                                                                                                               |
| TS         | STATE_JS_TS.md        | angular/angular                             | DONE - PARTIAL FIX | clusters 1-3 fixed; cluster 4 landed but residual files remain; cluster 5 accepted gap, open                                              |
| TS         | STATE_CURLY_GDR.md    | angular/angular (cluster 5 files, GDR multipass) | DONE - PARTIAL FIX | curly-general-scope-reindent-multipass=on: fixes user_metric_spec.ts + i18n_parse.ts (previously non-idempotent under single-pass GDR); emit.ts already idempotent under single-pass, stays idempotent under multipass |
| TS         | STATE_JS_TS.md        | microsoft/TypeScript                        | DONE - PARTIAL FIX | 3/4 clusters fixed; #3's shared shape fixed but most files are a separate, un-root-caused sibling issue                                   |
| TS         | STATE_JS_TS.md        | nestjs/nest                                 | DONE               |                                                                                                                                           |
| TS         | STATE_JS_TS.md        | vuejs/core                                  | DONE - PARTIAL FIX | switch-fallthrough idempotency bug open                                                                                                   |
| Python3    | STATE_PYTHON3.md      | django/django                               | DONE               |                                                                                                                                           |
| Python3    | STATE_PYTHON3.md      | pallets/click                               | DONE               |                                                                                                                                           |
| Python3    | STATE_PYTHON3.md      | pallets/flask                               | DONE               |                                                                                                                                           |
| Python3    | STATE_PYTHON3.md      | psf/black                                   | DONE               |                                                                                                                                           |
| Python3    | STATE_PYTHON3.md      | python/cpython                              | DONE               |                                                                                                                                           |

**Note on `microsoft/TypeScript`'s status**: cluster #3's shared braceless-
collapse root cause (same as `angular/angular` cluster 4) is now fixed, but
most of this corpus's affected files turned out to be a separate,
not-yet-root-caused sibling issue — see `STATE_JS_TS.md`'s "Dogfood:
microsoft/TypeScript" section for the full cluster list/ranking.

Corpus scope: `src/` only (601
real `.ts` files, 379045 lines) — `tests/cases/**` (20089 files,
hand-authored compiler test fixtures including deliberately-invalid-syntax
cases) and `tests/baselines/**` (auto-generated) were excluded as
non-representative test fixtures, same exclusion class as other jobs'
`built/`/`lib/` skips.

**AI (STATE_AI.md)**: N/A — this job's corpus concept is comment-text
training/labeling data (Pool A/Pool B ABSTAIN measurement), not a
source-formatting dogfood run. No comparable rows.

## Unsuitable (rejected as dogfood candidates)

| Language | Parent State File     | Dogfood Name                | Reason                               |
|----------|-----------------------|-----------------------------|--------------------------------------|
| C++26    | STATE_CPP26.md        | bloomberg/clang-p2996       | repo empty/unusable                  |
| CSS      | STATE_DATA_FORMATS.md | foundation/foundation-sites | all `.scss`, no plain CSS            |
| HTML5    | STATE_DATA_FORMATS.md | kangax/html-minifier        | rejected candidate — see parent file |
| HTML5    | STATE_DATA_FORMATS.md | mdn/content                 | rejected candidate — see parent file |
| HTML5    | STATE_DATA_FORMATS.md | twbs/bootstrap (docs)       | rejected candidate — see parent file |
| HTML5    | STATE_DATA_FORMATS.md | whatwg/html                 | rejected candidate — see parent file |
