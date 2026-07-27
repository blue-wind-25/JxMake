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
| `DONE`               | Run to completion, no known open bugs from this pass                                  |
| `DONE - PARTIAL FIX` | Bug(s) found; at least one root cause still open, deferred, or attempted-and-reverted |
| `DONE - OPEN Q`      | Run clean of bugs, but left a non-blocking design question unresolved                 |

`UNSUITABLE` corpora (rejected as unfit for use, not a status of a completed
run) are listed in the separate table at the bottom, not mixed into the
main table.

## Main table

Sorted by Language, then Status (DONE, DONE - PARTIAL FIX, DONE - OPEN Q, NOT STARTED).

| Language   | Parent State File     | Dogfood Name                                | Status              | Note                                                   |
|------------|-----------------------|---------------------------------------------|---------------------|--------------------------------------------------------|
| C/C++      | STATE_C_CPP_JAVA.md   | basvas-jkj/cpp_modules                      | DONE                |                                                        |
| C/C++      | STATE_C_CPP_JAVA.md   | blake-madden/tinyexpr-plusplus              | DONE                |                                                        |
| C/C++      | STATE_C_CPP_JAVA.md   | boost-ext/ut                                | DONE                |                                                        |
| C/C++      | STATE_C_CPP_JAVA.md   | boostorg/mp11                               | DONE                |                                                        |
| C/C++      | STATE_C_CPP_JAVA.md   | ericniebler/range-v3                        | DONE                |                                                        |
| C/C++      | STATE_C_CPP_JAVA.md   | fmtlib/fmt                                  | DONE                |                                                        |
| C/C++      | STATE_C_CPP_JAVA.md   | foonathan/lexy                              | DONE                |                                                        |
| C/C++      | STATE_C_CPP_JAVA.md   | gcc-mirror/gcc                              | NOT STARTED         | TOO MASSIVE                                            |
| C/C++      | STATE_C_CPP_JAVA.md   | llvm/llvm-project                           | NOT STARTED         | TOO MASSIVE                                            |
| C/C++      | STATE_C_CPP_JAVA.md   | martinus/nanobench                          | DONE                |                                                        |
| C/C++      | STATE_C_CPP_JAVA.md   | microsoft/proxy                             | DONE                |                                                        |
| C/C++      | STATE_C_CPP_JAVA.md   | microsoft/STL                               | DONE - PARTIAL FIX  | documented open gaps                                |
| C/C++      | STATE_C_CPP_JAVA.md   | NVIDIA/stdexec                              | DONE                |                                                        |
| C/C++      | STATE_C_CPP_JAVA.md   | serge-sans-paille/frozen                    | DONE                |                                                        |
| C/C++      | STATE_C_CPP_JAVA.md   | taocpp/PEGTL                                | DONE                |                                                        |
| C/C++      | STATE_C_CPP_JAVA.md   | Tongsuo-Project/tongsuo-mini                | DONE                |                                                        |
| C/C++      | STATE_C_CPP_JAVA.md   | user-reported `} // while` case             | DONE                | Fixture: real_code_regressions_5                  |
| C++26      | STATE_CPP26.md        | ryanjk5.github.io (rjk-duck post)           | DONE                | 1 out-of-scope finding, not this job's            |
| C++26      | STATE_CPP26.md        | simdjson/experimental_json_builder          | DONE                | 1 bug found+fixed                              |
| C++26      | STATE_CPP26.md        | stephenberry/glaze                          | DONE                | zero in-scope bugs                                     |
| C++26      | STATE_CPP26.md        | wrocpp/cpp26-reflection-examples            | DONE                |                                                        |
| Java       | STATE_C_CPP_JAVA.md   | ../../../src minus jxm (vendored 3rd-party) | DONE                |                                                        |
| Java       | STATE_C_CPP_JAVA.md   | ../../../src/jxm (JxMake's own Java tree)   | DONE                |                                                        |
| Java       | STATE_C_CPP_JAVA.md   | apache/ant                                  | DONE - PARTIAL FIX  | 2 files hit accepted reindent gap                    |
| Java       | STATE_C_CPP_JAVA.md   | ARMCortexMThumbC.java.in (local)            | DONE                |                                                        |
| Java       | STATE_C_CPP_JAVA.md   | google/google-java-format                   | DONE                |                                                        |
| Java       | STATE_C_CPP_JAVA.md   | javaparser/javaparser                       | DONE                |                                                        |
| Java       | STATE_C_CPP_JAVA.md   | jenkinsci/jenkins                           | DONE - PARTIAL FIX  | PluginManager.java line-wrap instability |
| Java       | STATE_C_CPP_JAVA.md   | openrewrite/rewrite                         | DONE - PARTIAL FIX  | cluster 5 still open                               |
| Java       | STATE_C_CPP_JAVA.md   | pcpp_java tool (local)                      | DONE                |                                                        |
| Java       | STATE_C_CPP_JAVA.md   | RobotCoding gui_frontend                    | DONE                |                                                        |
| Java       | STATE_C_CPP_JAVA.md   | self-dogfood (formatter's own src/)         | DONE                |                                                        |
| Java       | STATE_C_CPP_JAVA.md   | VMA-GIT/anemonesoft (local)                 | DONE                |                                                        |
| Kotlin     | STATE_KOTLIN.md       | arrow-kt/arrow                              | DONE                |                                                        |
| Kotlin     | STATE_KOTLIN.md       | gui_frontend_android                        | DONE                |                                                        |
| Kotlin     | STATE_KOTLIN.md       | JetBrains/kotlin                            | NOT STARTED         | huge, ~3000 kLOC                                     |
| Kotlin     | STATE_KOTLIN.md       | kotlinx.coroutines                          | DONE                |                                                        |
| Kotlin     | STATE_KOTLIN.md       | square/kotlinpoet                           | DONE                |                                                        |
| Kotlin     | STATE_KOTLIN.md       | square/okio                                 | DONE                |                                                        |
| JSON/JSON5 | STATE_DATA_FORMATS.md | babel/babel                                 | DONE                |                                                        |
| JSON/JSON5 | STATE_DATA_FORMATS.md | eslint/eslint                               | DONE                |                                                        |
| JSON/JSON5 | STATE_DATA_FORMATS.md | json5/json5                                 | DONE                |                                                        |
| JSON/JSON5 | STATE_DATA_FORMATS.md | microsoft/vscode                            | DONE                |                                                        |
| CSS        | STATE_DATA_FORMATS.md | necolas/normalize.css                       | DONE                |                                                        |
| CSS        | STATE_DATA_FORMATS.md | primer/css                                  | DONE                |                                                        |
| CSS        | STATE_DATA_FORMATS.md | twbs/bootstrap                              | DONE                |                                                        |
| YAML       | STATE_DATA_FORMATS.md | actions/starter-workflows                   | DONE                |                                                        |
| YAML       | STATE_DATA_FORMATS.md | ansible/ansible                             | DONE                |                                                        |
| YAML       | STATE_DATA_FORMATS.md | docker/compose                              | DONE                |                                                        |
| YAML       | STATE_DATA_FORMATS.md | home-assistant/core                         | DONE                |                                                        |
| YAML       | STATE_DATA_FORMATS.md | kubernetes/kubernetes                       | DONE                |                                                        |
| YAML       | STATE_DATA_FORMATS.md | prometheus/prometheus                       | DONE                |                                                        |
| TOML       | STATE_DATA_FORMATS.md | pola-rs/polars                              | DONE                |                                                        |
| TOML       | STATE_DATA_FORMATS.md | python-poetry/poetry                        | DONE                |                                                        |
| TOML       | STATE_DATA_FORMATS.md | rust-lang/cargo                             | DONE                |                                                        |
| TOML       | STATE_DATA_FORMATS.md | toml-lang/toml                              | DONE                |                                                        |
| XML        | STATE_DATA_FORMATS.md | apache/ant                                  | DONE                |                                                        |
| XML        | STATE_DATA_FORMATS.md | apache/maven                                | DONE                |                                                        |
| XML        | STATE_DATA_FORMATS.md | jenkinsci/jenkins                           | DONE                |                                                        |
| XML        | STATE_DATA_FORMATS.md | w3c/svgwg                                   | DONE                |                                                        |
| HTML5      | STATE_DATA_FORMATS.md | apache/ant manual/                          | DONE - PARTIAL FIX  | 1 unfixed gap: running.html loses a `<p>`          |
| HTML5      | STATE_DATA_FORMATS.md | web-platform-tests/wpt                      | DONE - PARTIAL FIX  | deep tree-construction gaps deferred               |
| HTML5      | STATE_DATA_FORMATS.md | wordpress/wordpress-develop                 | DONE - OPEN Q       | magic-comment capitalization question                  |
| JS         | STATE_JS_TS.md        | expressjs/express                           | DONE                |                                                        |
| JS         | STATE_JS_TS.md        | lodash/lodash                               | DONE                |                                                        |
| JS         | STATE_JS_TS.md        | nodejs/node                                 | NOT STARTED         | TOO MASSIVE                                            |
| TS         | STATE_JS_TS.md        | angular/angular                             | DONE - PARTIAL FIX  | cluster 4 #3 reverted; cluster 5 open                 |
| TS         | STATE_JS_TS.md        | microsoft/TypeScript                        | NOT STARTED         | huge, ~1490 kLOC                                     |
| TS         | STATE_JS_TS.md        | nestjs/nest                                 | DONE                |                                                        |
| TS         | STATE_JS_TS.md        | vuejs/core                                  | DONE - PARTIAL FIX  | switch-fallthrough idempotency bug open         |
| Python3    | STATE_PYTHON3.md      | django/django                               | DONE                |                                                        |
| Python3    | STATE_PYTHON3.md      | pallets/click                               | DONE                |                                                        |
| Python3    | STATE_PYTHON3.md      | pallets/flask                               | DONE                |                                                        |
| Python3    | STATE_PYTHON3.md      | psf/black                                   | DONE                |                                                        |
| Python3    | STATE_PYTHON3.md      | python/cpython                              | DONE                |                                                        |

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
