# formatter/ — working rules

This directory has several independent tracked jobs, each sharing one
common process file plus its own job-specific file. Before doing any work,
identify the job, read `STATE_COMMON.md` first, then **only** that job's
own file — do not read any other job's file, and do not cross-reference
between job state files unless a state file's own text says otherwise.

If the current task's job is unclear from context, ask before reading any
state file.

| Job | Read first | Then read |
|---|---|---|
| C/C++/Java formatter (existing, most work happens here) | `STATE_COMMON.md` | `STATE_C_CPP_JAVA.md` |
| Kotlin JAR support | `STATE_COMMON.md` | `STATE_KOTLIN.md` |
| C++26 rule coverage (not a separate language — lands directly in the existing "cpp" pipeline) | `STATE_COMMON.md` | `STATE_CPP26.md` |
| Data-format support: JSON/JSON5/YAML/TOML/etc. (JSON/JSON5/CSS/YAML/TOML/XML/HTML5 implemented, including HTML5's `<script>` dispatch) | `STATE_COMMON.md` | `STATE_DATA_FORMATS.md` |
| JS/TS support (implemented; JSX/TSX still need their own future embedding-aware dispatcher) | `STATE_COMMON.md` | `STATE_JS_TS.md` |
| Python3 support (implemented) | `STATE_COMMON.md` | `STATE_PYTHON3.md` |
| AI-assist Step 3: GRU comment-classifier abstain resolution (skeleton started — `com.jxmake.formatter.classifier.gru` package) | `STATE_COMMON.md` | `STATE_AI.md` |
| General scope-depth reindentation (curly reindent job; pre-pass architecture landed, default off, behind `curly-general-scope-reindent = on` — high risk, a real pass-ordering bug was found during real-code validation, read `STATE_CURLY_GDR.md` before attempting) | `STATE_COMMON.md` | `STATE_CURLY_GDR.md` |
| HTML5 deep tree-construction gaps (tc gap job; level 1 — implicit `<body>` insertion — landed, levels 2-4 not yet implemented — high risk, read `STATE_HTML5_TCG.md` before attempting) | `STATE_COMMON.md` | `STATE_HTML5_TCG.md` |

`STATE_COMMON.md` holds the shared commit workflow, ambiguity-handling
protocol, file-exclusion rules, and real-code-testing methodology used by
every job. `STATE_C_CPP_JAVA.md` is authoritative for the C/C++/Java job's
progress, implementation protocol, and "Resolved Design Decisions" table.
`STATE_KOTLIN.md`, `STATE_CPP26.md`, `STATE_DATA_FORMATS.md`,
`STATE_JS_TS.md`, `STATE_PYTHON3.md`, `STATE_AI.md`, `STATE_CURLY_GDR.md`,
and `STATE_HTML5_TCG.md` are each job's own equivalent.

**Current implementation status:** no language this codebase recognizes is
scaffold-only any more (`Lang.SCAFFOLD_ONLY_LANGUAGES` is now an empty
string, kept only for documentation/compatibility). Data formats (JSON,
JSON5, CSS, YAML, TOML, XML, HTML5 incl. `<script>` dispatch via
`XmlSpecificRule.renderScriptOrStyle`), JS/TS (`JsTsSpecificRule` /
`JsTsDeclarationAlignmentRule`; JSX/TSX still excluded pending a future
embedding-aware dispatcher), and Python3 (`FormatterIndent` /
`ScopePipelineIndent` for STYLE_PYTHON3.md §1-9) have all landed real
logic. C++26 has no separate language identity or scaffold entry at all —
it's future incremental rule coverage on the existing, already-implemented
`"cpp"` pipeline (same as C++20 — see `STATE_CPP26.md`'s Resolved Design
Decisions). `README.md`/`../README.txt` should describe every implemented
language, including Python3, as implemented — matching actual code state.
This file and the `STATE_*.md` files track true current code state and
must NOT be updated to match any doc's aspirational status ahead of what's
actually landed.

(The comment-grammar classifier accuracy upgrade, formerly tracked in its
own `STATE_COMMENT_GRAMMAR.md`, shipped and was folded into
`STATE_C_CPP_JAVA.md`'s "H" section — see that section and `RDD_LOG.md`'s
`RDD_KEY_94`–`RDD_KEY_98` for its history.)

After any `/compact` or context summarization, re-read the relevant state
file in full before continuing — a summary may omit or compress exact
resolved-decision or checklist details that only the on-disk file has
right.
