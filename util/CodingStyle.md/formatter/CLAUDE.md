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
| JS/TS support (implemented) | `STATE_COMMON.md` | `STATE_JS_TS.md` |
| Python3 support (implemented) | `STATE_COMMON.md` | `STATE_PYTHON3.md` |
| AI-assist Step 3: GRU comment-classifier abstain resolution (implemented and shipped — `gru-classifier = on` default since 2026-08-02, `com.jxmake.formatter.classifier.gru` package) | `STATE_COMMON.md` | `STATE_AI.md` |
| Operator-priority line splitting (curly-family `line-split-by-operator-priority`, default off — implemented) | `STATE_COMMON.md` | `STATE_LINE_SPLIT_OP.md` |
| General scope-depth reindentation (curly reindent job; pre-pass architecture landed, default off, behind `curly-general-scope-reindent = on` — high risk, a real pass-ordering bug was found during real-code validation, read `STATE_CURLY_GDR.md` before attempting) | `STATE_COMMON.md` | `STATE_CURLY_GDR.md` |
| HTML5 deep tree-construction gaps (tc gap job; all four levels (1-4) landed and full-suite dogfood re-validated with zero regression, still off by default behind `html5-tc-gap-level = 0`, opt-in cumulative — read `STATE_HTML5_TCG.md` for each level's implementation notes/known limitations before changing) | `STATE_COMMON.md` | `STATE_HTML5_TCG.md` |
| INI-like key-value config formatter (E-INI, Extended INI; implemented — narrow beautification-only rule list per `STYLE_TOOLING.md` §4) | `STATE_COMMON.md` | `STATE_EINI.md` |
| JxMakeFile support (implemented — narrow beautification-only rule list per `STYLE_JXMAKE.md`) | `STATE_COMMON.md` | `STATE_JXMAKE.md` |
| Build/dev-tooling script formatters: Makefile, Bash, PowerShell (implemented — narrow beautification-only rule lists per `STYLE_TOOLING.md`) | `STATE_COMMON.md` | `STATE_TOOLING.md` |

`STATE_COMMON.md` holds the shared commit workflow, ambiguity-handling
protocol, file-exclusion rules, and real-code-testing methodology used by
every job. `STATE_C_CPP_JAVA.md` is authoritative for the C/C++/Java job's
progress, implementation protocol, and "Resolved Design Decisions" table.
`STATE_KOTLIN.md`, `STATE_CPP26.md`, `STATE_DATA_FORMATS.md`,
`STATE_JS_TS.md`, `STATE_PYTHON3.md`, `STATE_AI.md`, `STATE_LINE_SPLIT_OP.md`,
`STATE_CURLY_GDR.md`, `STATE_HTML5_TCG.md`, `STATE_EINI.md`,
`STATE_JXMAKE.md`, and `STATE_TOOLING.md` are each job's own equivalent.

**Current implementation status:** no language in this codebase is
scaffold-only any longer (`Lang.SCAFFOLD_ONLY_LANGUAGES` is now an empty
string, kept only for documentation/compatibility). Data formats (JSON,
JSON5, CSS, YAML, TOML, XML, HTML5 incl. `<script>` dispatch via
`XmlSpecificRule.renderScriptOrStyle`), JS/TS (`JsTsSpecificRule`/
`JsTsDeclarationAlignmentRule`), Python3 (`FormatterIndent`/
`ScopePipelineIndent` for STYLE_PYTHON3.md §1-9), E-INI
(`FormatterEini`/`EiniSpecificRule`, STYLE_TOOLING.md §4), JxMakeFile
(`FormatterJxMake`/`JxMakeSpecificRule`, STYLE_JXMAKE.md), and the three
tooling languages — Makefile (`FormatterMakefile`/`MakefileSpecificRule`),
Bash (`FormatterBash`/`BashSpecificRule`, STYLE_TOOLING.md §2), PowerShell
(`FormatterPowerShell`/`PowerShellSpecificRule`, STYLE_TOOLING.md §3) — all
have real logic landed. C++26 has no separate language identity or
scaffold entry — it's future incremental rule coverage on the existing,
already-implemented `"cpp"` pipeline (same as C++20 — see
`STATE_CPP26.md`'s Resolved Design Decisions). `README.md`/`../README.txt`
should describe every implemented language as implemented, matching actual
code state. This file and the `STATE_*.md` files track true current code
state — never update them to match a doc's aspirational status ahead of
what's actually landed.

(The comment-grammar classifier accuracy upgrade — formerly tracked in its
own `STATE_COMMENT_GRAMMAR.md` — shipped, folded into
`STATE_C_CPP_JAVA.md`'s "H" section; see that section and `RDD_LOG.md`'s
`RDD_KEY_94`–`RDD_KEY_98` for history.)

**`README.md` is strictly user-facing, for every job.** It must never leak this directory's
internal process/implementation vocabulary: no `RDD_KEY_n`, no `STATE_*.md`/`RDD_LOG.md`
references, no `test/` fixture names, no internal Java class/method names (e.g. `Config.java`,
`FormatterCurly.formatOne`, `ServerMode.findRunningServerPort()`), and no internal build/test-
process detail (`make test`, Makefile target names, `_test_serial`). Describe only observable
behavior, in plain language a reader with no access to this directory could follow; use an inline
code snippet instead of naming an internal type/method when illustrating a limitation (see
`STATE_COMMON.md`'s rule on updating `README.md` alongside any user-visible fix). Before finishing
any task that touches `README.md`, grep it for these patterns rather than assuming a small,
localized edit couldn't have introduced one — a 2026-08-20 session found and fixed a dozen such
leaks that had silently accumulated across prior sessions despite this rule already existing in
`STATE_COMMON.md`; this file states it again, prominently, since `STATE_COMMON.md`'s version alone
wasn't enough to keep it from recurring.

After any `/compact` or context summarization, re-read the relevant state
file in full before continuing — a summary may omit or compress exact
resolved-decision or checklist details that only the on-disk file has
right.

**Canonical language order (documentation, help strings, `--lang`/`lang=`
enumerations, etc.), for this and every future session:**

```
c, cpp, java, kotlin, json, json5, css, yaml, toml, xml, html5, js, ts,
python3, eini, jxmake, makefile, bash, powershell
```

Apply this order whenever authoring or editing a language list anywhere in
this directory or its docs. `eini`/`jxmake`/`makefile`/`bash`/`powershell`
are fully implemented (see `STATE_EINI.md`/`STATE_JXMAKE.md`/
`STATE_TOOLING.md` / `STYLE_TOOLING.md`) and belong in every list that
asserts *current* capability (`README.md`'s
`--lang` accepted-values list, its `.ext →` / basename language mapping,
`../README.txt`'s JAR-implemented-languages list, server `lang=` enums,
etc.), matching actual code state.
