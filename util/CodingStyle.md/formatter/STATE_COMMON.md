# STATE_COMMON.md — Shared Process Conventions

Read this file first, no matter which job (`STATE_C_CPP_JAVA.md` or
`STATE_KOTLIN.md`) you're picking up. It holds every process convention that
is identical across both jobs — commit workflow, ambiguity handling, file
exclusions, testing methodology, RDD_LOG.md lookup discipline. The per-job
file assumes all of this and does not restate it; it only contains what's
specific to that job (Project Layout, Resolved Design Decisions index,
Open Questions, Checklist).

**Do NOT read `README.md`** unless the user explicitly asks. All decisions
relevant to implementation are recorded in each job file's own **Resolved
Design Decisions** index (full text in `RDD_LOG.md` — see the lookup
convention below).

**ONLY** read the source file you are currently implementing or directly
modifying. Do NOT read other source files unless a specific checklist item
or ambiguity requires it.

---

## RDD_LOG.md lookup convention

Full decision text for every `RDD_KEY_n` lives in `RDD_LOG.md`, shared
across both jobs (Kotlin continues the same numbering sequence as
C/C++/Java — never restart it). **Do not read `RDD_LOG.md` in full.** Look
up one key at a time:

```
grep -Fm1 'RDD_KEY_n' util/CodingStyle.md/formatter/RDD_LOG.md
```

**Never add the `-A` parameter to this `grep`** — the lines in `RDD_LOG.md`
are very long, and `-A` context will flood output unnecessarily.

---

## During implementation

- Implement one checklist section at a time.
- After completing a section (or when the cumulative diff across all changed
  files exceeds ~50 lines, whichever comes first), do a checkpoint commit:
  1. Update the job's state file — check off completed items and update the
     active checklist.
  2. `git add util/CodingStyle.md/formatter/` (the entire formatter directory)
  3. `git reset util/CodingStyle.md/formatter/target/` (exclude build output)
  4. `git commit -m "<message>"` — short descriptive message, no strict
     format required, trailer ending with
     `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`
- Small related items within a section may be grouped into one commit if they
  are trivially connected — use judgment based on line count (~50 lines
  threshold).
- Never let implemented files and the state file drift out of sync — the
  state file must always reflect the true current state at every commit.
- Never modify the files `util/CodingStyle.md/formatter/test/*_inp.*` unless
  they contain syntax errors (they are the test input files).
- Never modify the files `util/CodingStyle.md/formatter/test/*_out.*` unless
  explicitly asked (they are the reference output files that show the
  expected results).
- Ignore `XL.txt` — that is the user tracker file.
- Use `/tmp` for temporary smoke-test and mini-test files.
- NEVER perform a filesystem-wide find; search first in `/tmp/claude-1000`
  or the project root. If still not found, ask the user.
- Prefer evidence over reasoning when diagnosing a bug or checking for a
  regression. Keep static analysis minimal — only enough to identify where
  to insert debug prints. Use debug prints and `make test` to diagnose and
  validate fixes, not static analysis as the primary method. After a fix is
  verified with `make test`, remove all debug prints, then commit only the
  files you actually modified. If unsure, ask.

## When a file reaches COMPLETE

1. Update the relevant checklist in the job's state file.
2. Commit the state file together with the completed source file.

## When hitting an ambiguity or open question

1. **Stop coding immediately** — do not guess or proceed past the ambiguity.
2. Update the job's state file: add the question to **Open Questions**, mark
   the blocked checklist item with `[~]` and a note.
3. Commit the state file only.
4. Ask the user and wait for an answer before continuing.
5. Once resolved: append the full decision as a new row to `RDD_LOG.md`
   (next `RDD_KEY_n` number, continuing the shared sequence), add the key +
   topic to the **Resolved Design Decisions** index in the job's own state
   file, remove from **Open Questions**, unblock the checklist item, then
   continue.

## Session end

- Always leave the state file committed and up to date before ending the
  session.
- The next session resumes from the first unchecked item in the current
  checklist.

---

## Real-code testing methodology

Repeatable methodology for testing the formatter against real, third-party
code (preferred over synthetic dogfooding — it finds concrete, fixable bugs
faster):

1. Clone a real, compiling third-party project.
2. Format it once (round1).
3. Format round1's output again (round2).
4. `diff round1 round2` must be empty (idempotency).
5. Compile round1 with the appropriate toolchain — must succeed with the
   same error count as the unmodified original (no new, formatter-induced
   errors).

When an idempotency (or forward-pass) failure doesn't reproduce at the
default config, try re-testing with a `.jxmake-code-formatter` overriding
`indent-size`, `indent-style`, etc. to match the candidate's own actual
convention before concluding "no bug" — some real bugs are only observable
at a non-default `indent-size`.

**When a bug is found and fixed, add a new permanent fixture pair:**
`test/real_code_regressions_N_{inp,out}.<ext>` (next available `N`)
reproducing it minimally, then register it in the `Makefile`'s `INP_FILES`
and document it in `test/README.txt` — unless the bug is a no-op at the test
harness's own default config (in which case document the fix and its
non-default-config verification in the state file instead, without adding a
fixture that would be indistinguishable from a no-op at default settings).
Try to combine multiple bugs in the same text fixture if possible.

Use this standard copyright header on every new test fixture file:

```
/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
```

---

## Shared TODO — Server Protocol: Inline Config Support

**Status:** COMPLETE. Implemented, tested (`make test-server`, plus `make
test` regression), and documented. Lives here (not in
`STATE_C_CPP_JAVA.md` or `STATE_KOTLIN.md`) because it's server/protocol
infrastructure shared by every job, not a per-language formatting rule.

**Motivation:** `POST /format` currently resolves config by looking for a
`.jxmake-code-formatter` file on disk near the `path` query param. A browser
client formatting a pasted/in-memory snippet has no real file path and
nothing on disk for the server to find — it needs a way to hand the server a
complete config directly in the request.

**Decided design** (resolved via user Q&A across this and a prior session —
do not re-litigate, treat as settled unless a new ambiguity is found during
implementation). **Revision note:** an earlier version of this task specified
a JSON request body (`{"content": ..., "config": {...}}`). That's been
dropped — see rationale below — in favor of extending the existing query
string, so **the body stays exactly as it is today: raw file bytes, no
wrapping, no parsing changes to it at all.**

- **No JSON anywhere in this protocol, by design.** The only field that
  would ever need JSON-style escaping is the file content itself (arbitrary
  real-world source: embedded quotes, backslashes, regex literals, unicode
  identifiers, emoji in comments) — a hand-rolled parser for that is a real
  correctness risk (silent corruption on a missed escape case, not a loud
  failure), and pulling in an external JSON library was explicitly rejected
  (external deps update too often for this project's taste). Query strings
  sidestep the problem entirely: the body never has to be escaped/parsed as
  anything, because it never carries config.
- **Each `config` entry becomes its own optional query parameter** on the
  same `POST /format` call, reusing exactly the parsing the server already
  does for `path`/`lang`/`format-off` today — no new parsing code path, just
  more recognized optional keys:
  ```
  POST /format?path=<abs-path>&lang=java&indent-size=2&line-length=120&java-import-order=java,com,org
  ```
  Standard URL query-string encoding (browsers get this for free via
  `URLSearchParams`; the JVM's HTTP server already decodes it) handles
  commas/spaces/special characters in a config *value* safely — this only
  ever has to cover short, bounded values (numbers, `on`/`off`, short
  comma-lists like an import-order list), never arbitrary source code, so
  there is no meaningful risk class here at all, unlike the body.
  Keys/values follow exactly the property names in `STATE_C_CPP_JAVA.md`'s
  **Config Keys and Defaults** table — this is the single source of truth
  for valid keys; do not invent a separate schema. That table already spans
  every currently-supported language (structural/behavior keys,
  `header-guard-rename` for C/C++, `java-import-*` for Java,
  `kotlin-import-*` for Kotlin) — the validator must accept the full current
  table, not just a C/C++/Java subset. It must also be kept in sync going
  forward: any future language support (JSON, XML, JavaScript, TypeScript,
  CSS, HTML5, Python3 — see `FUTURE_FEATURE_DISCUSSION.md`) that adds its
  own config keys (e.g. the `json-colon-align`, `css-colon-align`, and
  `python-*` properties already flagged there) must add them to this same
  table and this same validator in the same change, not as a follow-up — do
  not hardcode today's key list as if it were permanent.
- **`path` becomes optional** when at least one inline config parameter and
  `lang` are both present in the request (client sends a real path, omits
  it, or sends a placeholder purely for logging — the server must not
  attempt disk-based config lookup or extension-based `lang` fallback in
  this case, since `lang` is already required and always wins per the
  existing rule). `path` stays required when no inline config parameters are
  given, unchanged from current behavior.
- **File-path config lookup and inline config query params are mutually
  exclusive by client contract** (a well-behaved client sends one or the
  other, never both) — but defensively, if a request somehow supplies both a
  resolvable `path`-based config *and* one or more inline config params,
  **inline wins** rather than erroring.
- **Unknown config-shaped query keys → HTTP 400** with a plain-text error
  body, consistent with the existing strict `lang`/`path` validation (a
  typo'd key should fail loudly, not be silently ignored or silently treated
  as a no-op override).
- **Stateless, per-request only.** No session/handshake; every `/format`
  call that wants inline config must include it every time. Do not add any
  connection-level or cookie-based config state.

**Backward compatibility:** unlike the JSON-body approach this replaces,
this design is **purely additive** — the body format is untouched, and every
existing caller (including the bundled CLI's own auto-connect client) that
never sends the new query keys keeps working with zero changes. The bundled
CLI's client only needs updating if/when it wants to *expose* inline config
to its own users (e.g. a future CLI flag that forwards to these query
params) — that's optional follow-up work, not a required part of this task.

**Required changes:**
- [x] Extend `/format`'s existing query-string parsing to recognize the
      config keys from `STATE_C_CPP_JAVA.md`'s **Config Keys and Defaults**
      table as additional optional parameters. No body-parsing changes.
- [x] Wire recognized inline config params into whatever config-resolution
      path the server already uses for file-based `.jxmake-code-formatter`
      lookup, with inline taking priority per the rule above.
- [x] Validate query keys that look like config keys (i.e. match a known
      property name pattern, or simply: any query key besides
      `path`/`lang`/`format-off`) against the canonical set in
      `STATE_C_CPP_JAVA.md` → **Config Keys and Defaults**; HTTP 400 on any
      unrecognized key. Confirm this covers C/C++/Java *and* Kotlin keys
      (all already in that one table) — and re-check this validator
      whenever a future language's config keys land, per the note above.
- [x] Make `path` optional exactly when at least one inline config param and
      `lang` are both present; required otherwise (unchanged).
- [x] Update `README.md`'s **Server Wire Protocol** section to document the
      new optional query parameters, the optional/required `path` rule, the
      unknown-key-→-400 behavior, and the mutual-exclusivity-with-inline-
      wins rule — this section currently only documents `path`/`lang`/
      `format-off` and will be incomplete (not wrong, since the body/base
      contract is unchanged) once this lands.
- [x] Add a server-mode test fixture covering: inline config with no `path`,
      inline config overriding a would-be file-based config, and the
      unknown-key-→-400 case. This does **not** belong in `test/README.txt`
      (that file documents the format input/output fixture pairs only, not
      server-mode behavior) — place it wherever existing server-mode
      testing already lives (see Task D's multi-file smoke test in
      `STATE_C_CPP_JAVA.md` for the closest existing precedent), or ask if
      no such location exists yet.
- [x] Follow this file's own **ambiguity-handling** convention above for
      anything not already resolved here — stop, record in the picking-up
      job's **Open Questions**, ask, don't guess.
