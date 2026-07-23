# formatter/ — working rules

This directory has several independent tracked jobs. All share one common
process file plus their own job-specific file. Before doing any work,
identify which job the current task belongs to, then read `STATE_COMMON.md`
first, followed by **only** that job's own file — do not read any other
job's file.

| Job | Read first | Then read |
|---|---|---|
| C/C++/Java formatter (existing, most work happens here) | `STATE_COMMON.md` | `STATE_C_CPP_JAVA.md` |
| Kotlin JAR support | `STATE_COMMON.md` | `STATE_KOTLIN.md` |
| C++26 rule coverage (not a separate language — lands directly in the existing "cpp" pipeline) | `STATE_COMMON.md` | `STATE_CPP26.md` |
| Data-format support: JSON/JSON5/YAML/TOML/etc. (JSON/JSON5/CSS/YAML/TOML/XML/HTML5 implemented, including HTML5's `<script>` dispatch) | `STATE_COMMON.md` | `STATE_DATA_FORMATS.md` |
| JS/TS support (implemented; JSX/TSX still need their own future embedding-aware dispatcher) | `STATE_COMMON.md` | `STATE_JS_TS.md` |
| Python3 support (implemented) | `STATE_COMMON.md` | `STATE_PYTHON3.md` |
| AI-assist Step 3: GRU comment-classifier abstain resolution (skeleton started — `com.jxmake.formatter.classifier.gru` package) | `STATE_COMMON.md` | `STATE_AI.md` |

`STATE_COMMON.md` holds the shared commit workflow, ambiguity-handling
protocol, file-exclusion rules, and real-code-testing methodology used by
every job. `STATE_C_CPP_JAVA.md` is the authoritative source for the
C/C++/Java job's progress, implementation protocol, and "Resolved Design
Decisions" table — it supersedes general guesswork for that job.
`STATE_KOTLIN.md`, `STATE_CPP26.md`, `STATE_DATA_FORMATS.md`,
`STATE_JS_TS.md`, `STATE_PYTHON3.md`, and `STATE_AI.md` are each job's
own equivalent. Do not cross-reference between any two job state files
unless a state file's own text says otherwise.

All four newer jobs (data formats, JS/TS, Python3, and the AI-assist
skeleton aside) started scaffold-only at kickoff: wired into `Lang.java`/
`Config.java`/`Main.java`/`ServerMode.java` for detection/dispatch, with
every unimplemented language throwing a clear "not yet implemented" error
rather than silently passing text through or attempting real formatting.
Data formats, JS/TS, and Python3 have since all landed real logic (data
formats: JSON/JSON5/CSS/YAML/TOML/XML/HTML5, including HTML5's `<script>`
dispatch — see `XmlSpecificRule.renderScriptOrStyle`; JS/TS:
`JsTsSpecificRule`/`JsTsDeclarationAlignmentRule`, JSX/TSX still excluded
pending their own future embedding-aware dispatcher; Python3:
`FormatterIndent`/`ScopePipelineIndent` for §1-9 of `STYLE_PYTHON3.md` —
see `STATE_PYTHON3.md`). Per `Lang.SCAFFOLD_ONLY_LANGUAGES` (now an empty
string, kept only for documentation/compatibility), no language this
codebase recognizes is scaffold-only any more. C++26 is different: it has
no separate language identity or scaffold entry at all — it's future
incremental rule coverage on the existing, already-implemented `"cpp"`
pipeline, same as C++20 was folded in with no separate selector (see
`STATE_CPP26.md`'s Resolved Design Decisions). `README.md`/`../README.txt`
described Python3 as implemented ahead of the actual code per explicit
user instruction (2026-07-21); that framing was true only while Python3's
implementation was still landing and no longer applies now that it has
shipped for real — both docs' present-tense claims should track actual
code state like every other implemented language. This file and the
`STATE_*.md` files track true current code state and must NOT be updated
to match any doc's aspirational status ahead of what's actually landed.

(The comment-grammar classifier accuracy upgrade, formerly tracked in its
own `STATE_COMMENT_GRAMMAR.md`, shipped and was folded into
`STATE_C_CPP_JAVA.md`'s "H" section once its checklist completed — see that
section and `RDD_LOG.md`'s `RDD_KEY_94`–`RDD_KEY_98` for its history.)

After any `/compact` or context summarization, re-read the relevant state
file in full before continuing. A summary of prior conversation may omit or
compress details (exact resolved decisions, exact checklist state) that only
that file's current on-disk content can be trusted to have right.

If the current task's job is unclear from context, ask before reading any
state file.
