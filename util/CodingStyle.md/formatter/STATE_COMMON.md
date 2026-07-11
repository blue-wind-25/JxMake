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

**Status:** not started. Scoped and ready to pick up. Lives here (not in
`STATE_C_CPP_JAVA.md` or `STATE_KOTLIN.md`) because it's server/protocol
infrastructure shared by every job, not a per-language formatting rule.

**Motivation:** `POST /format` currently resolves config by looking for a
`.jxmake-code-formatter` file on disk near the `path` query param. A browser
client formatting a pasted/in-memory snippet has no real file path and
nothing on disk for the server to find — it needs a way to hand the server a
complete config directly in the request.

**Decided design** (resolved via user Q&A this session — do not re-litigate
these, treat as settled unless a new ambiguity is found during implementation):

- **Request body becomes JSON**, replacing the current raw-file-content body:
  ```json
  { "content": "<raw file text>", "config": { "indent-size": 2, "...": "..." } }
  ```
  `config` is optional. When present, its keys/values follow exactly the
  property names in `STATE_C_CPP_JAVA.md`'s **Config Keys and Defaults**
  table — this is the single source of truth for valid keys; do not invent a
  separate schema. That table already spans every currently-supported
  language (structural/behavior keys, `header-guard-rename` for C/C++,
  `java-import-*` for Java, `kotlin-import-*` for Kotlin) — the validator
  must accept the full current table, not just a C/C++/Java subset. It must
  also be kept in sync going forward: any future language support (JSON,
  XML, JavaScript, TypeScript, CSS, HTML5, Python3 — see
  `FUTURE_FEATURE_DISCUSSION.md`) that adds its own config keys (e.g. the
  `json-colon-align`, `css-colon-align`, and `python-*` properties already
  flagged there) must add them to this same table and this same validator in
  the same change, not as a follow-up — do not hardcode today's key list as
  if it were permanent.
- **`path` becomes optional** when both `config` and `lang` are present in
  the request (client sends a real path, or omits it, or sends a placeholder
  purely for logging — the server must not attempt disk-based config lookup
  or extension-based `lang` fallback in this case, since `lang` is already
  required and always wins per the existing rule). `path` stays required in
  the no-inline-config case, unchanged from current behavior.
- **File-path config lookup and inline `config` are mutually exclusive by
  client contract** (a well-behaved client sends one or the other, never
  both) — but defensively, if a request somehow supplies both a resolvable
  `path`-based config *and* an inline `config`, **inline wins** rather than
  erroring.
- **Unknown keys in `config` → HTTP 400** with a plain-text error body,
  consistent with the existing strict `lang`/`path` validation. Malformed
  JSON in the request body is also HTTP 400.
- **Stateless, per-request only.** No session/handshake; every `/format`
  call that wants inline config must include it every time. Do not add any
  connection-level or cookie-based config state.

**Breaking-change note:** this replaces the `/format` body contract (raw
bytes → JSON envelope) for *all* clients, not just new browser ones —
including the bundled CLI's own auto-connect client. Since both sides of
this protocol live in the same repo, treat this as an atomic change: update
the server, the bundled CLI's `ServerMode` client call, and `test/README.txt`
/ any server-mode test fixtures together in one pass, not as a
soft-deprecate-old-format transition. Do not attempt Content-Type-based
negotiation between the old raw-body and new JSON-body forms unless a real
external third-party client is known to depend on the old form — none is
known as of this note.

**Required changes:**
- [ ] Update `/format` request parsing to accept the new JSON body shape
      (`content` + optional `config`), replacing raw-body parsing.
- [ ] Wire inline `config` into whatever config-resolution path the server
      already uses for file-based `.jxmake-code-formatter` lookup, with
      inline taking priority per the rule above.
- [ ] Validate `config` keys against the canonical set in `STATE_C_CPP_JAVA.md`
      → **Config Keys and Defaults**; HTTP 400 on any unrecognized key or
      malformed JSON. Confirm this covers C/C++/Java *and* Kotlin keys (all
      already in that one table) — and re-check this validator whenever a
      future language's config keys land, per the note above.
- [ ] Make `path` optional exactly when `config` + `lang` are both present;
      required otherwise (unchanged).
- [ ] Update the bundled CLI's own server-client call site to send the new
      JSON body shape (it becomes a client of its own new protocol).
- [ ] Update `README.md`'s **Server Wire Protocol** section to document the
      new JSON body shape, the optional/required `path` rule, the
      unknown-key-→-400 behavior, and the mutual-exclusivity-with-inline-wins
      rule — this section currently describes the old raw-body contract and
      will be actively wrong once this lands.
- [ ] Add/update a server-mode test fixture (or extend the existing
      multi-file smoke test from Task D) covering: inline config with no
      `path`, inline config overriding a would-be file-based config, and the
      unknown-key-→-400 case.
- [ ] Follow this file's own **ambiguity-handling** convention above for
      anything not already resolved here — stop, record in the picking-up
      job's **Open Questions**, ask, don't guess.
