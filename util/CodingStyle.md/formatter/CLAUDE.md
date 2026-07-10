# formatter/ — working rules

This directory has two independent tracked jobs. Both share one common
process file plus their own job-specific file. Before doing any work,
identify which job the current task belongs to, then read `STATE_COMMON.md`
first, followed by **only** that job's own file — do not read the other
job's file.

| Job | Read first | Then read |
|---|---|---|
| C/C++/Java formatter (existing, most work happens here) | `STATE_COMMON.md` | `STATE_C_CPP_JAVA.md` |
| Kotlin JAR support | `STATE_COMMON.md` | `STATE_KOTLIN.md` |

`STATE_COMMON.md` holds the shared commit workflow, ambiguity-handling
protocol, file-exclusion rules, and real-code-testing methodology used by
both jobs. `STATE_C_CPP_JAVA.md` is the authoritative source for the
C/C++/Java job's progress, implementation protocol, and "Resolved Design
Decisions" table — it supersedes general guesswork for that job.
`STATE_KOTLIN.md` is the Kotlin job's own equivalent. Do not cross-reference
between `STATE_C_CPP_JAVA.md` and `STATE_KOTLIN.md` unless a state file's own
text says otherwise.

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
