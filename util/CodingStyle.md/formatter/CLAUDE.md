# formatter/ — working rules

This directory has three independent tracked jobs, each with its own
self-contained state file. Before doing any work, identify which job the
current task belongs to and read **only** that file — do not read the other
two.

| Job | State file |
|---|---|
| C/C++/Java formatter (existing, most work happens here) | `STATE.md` |
| Kotlin JAR support | `STATE_KOTLIN.md` |
| Comment-grammar classifier | `STATE_COMMENT_GRAMMAR.md` |

`STATE.md` is the authoritative source for the C/C++/Java job's progress,
implementation protocol, and "Resolved Design Decisions" table — it
supersedes general guesswork for that job. `STATE_KOTLIN.md` and
`STATE_COMMENT_GRAMMAR.md` are each self-contained and independent of
`STATE.md`; do not cross-reference between the three unless a state file's
own text says otherwise.

After any `/compact` or context summarization, re-read the relevant state
file in full before continuing. A summary of prior conversation may omit or
compress details (exact resolved decisions, exact checklist state) that only
that file's current on-disk content can be trusted to have right.

If the current task's job is unclear from context, ask before reading any
state file.
