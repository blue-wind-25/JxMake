# FUTURE_FEATURE_DISCUSSION.md — Future Language/Version Support (Not Scoped)

This file is a standalone discussion note for language or version support that is
**not currently scoped, not started, and not part of any implementation plan.**
It is deliberately **not referenced from README.md, STATE.md, or any other file** —
there is no path by which a normal session would be pointed here, and that is
intentional: this is a place for the user to jot down "worth remembering later"
notes, not a queue for the CLI to discover and act on. If any entry below is ever
actually picked up, its contents should be moved into the normal flow at that
time (a real STATE_NEXT_*.md design file, or checklist items in STATE.md) rather
than treating this file itself as a task list.

No checklist syntax (`[ ]`/`[~]`) is used anywhere in this file, on purpose — an
entry here is a discussion point, not a tracked, scoped piece of work.

---

## C++26

**Status:** exploratory discussion only, nothing scoped, nothing started.

C++26 brings several new constructs. Most look tractable as additive work,
following the same pattern that added C++17/20/23 support in `STYLE_CPP20.md`
(new keyword columns, new operator spacing rules, or — for several C++20
constructs — no new code at all once checked against actual JAR behavior).
One area looks meaningfully harder:

- **Pack indexing (`T...[i]`)** — likely falls under the existing array-index
  bracket rules (STYLE.md §3.1) with little or no new code.
- **`= delete("reason")`** — trivial; a string literal inside an existing
  construct.
- **Placeholder `_`** — ordinary identifier, no new rule anticipated.
- **Contracts (`pre`/`post`/`contract_assert` attached to a signature)** —
  comparable in shape to STYLE_CPP20.md §2.2's trailing-`requires`-clause
  handling (a clause trailing the `)`, wrapping to its own line if the
  combined line overflows). Expected to be tractable as an additive section.
- **Reflection (`^^`, `[:`, `:]` splicing)** — the one genuinely harder piece.
  These are new tokens the tokenizer does not recognize at all, not new
  keywords added to an existing grammar shape — comparable in kind to the
  Kotlin Step 0 tokenizer work (new `MULTI_CHAR_OPS` entries, longest-prefix-
  first ordering, and a real risk of surfacing latent tokenizer bugs the way
  that session found one in number-literal lexing). `[:`/`:]` also look
  bracket-shaped, so a deliberate decision is needed on whether they interact
  with STYLE.md §3.1's complexity-based bracket-padding rule or stay fully
  opaque. This should get its own dedicated tokenizer-support pass, evidence-
  tested against real reflection code, before any style rule for it is
  trusted — not inferred from the standard's grammar alone.

**If this is ever picked up — file structure recommendation:** create a new
`STYLE_CPP26.md`, rather than extending `STYLE_CPP20.md`. Two reasons:
1. Java and Kotlin both already split "baseline" from "newer constructs"
   (`STYLE_JAVA.md`→`STYLE_JAVA17.md`, `STYLE_KOTLIN.md`→`STYLE_KOTLIN2.md`,
   each explicitly "read after" its baseline file). `STYLE_CPP20.md` breaks
   that convention by lumping three revisions (17/20/23) into one file, but
   those were uniformly small, additive features. Reflection and contracts
   are a larger, more structurally novel feature surface — closer to "new
   language subset" than "a few new keywords" — and fit the split-file
   pattern better than the lumped one.
2. `STYLE_CPP20.md`'s constructs are fully implemented and verified (STATE.md
   Task G cross-checked them against actual JAR behavior). Keeping it frozen
   and putting C++26 in its own file avoids mixing the riskier, unverified
   reflection/contracts work into a file that is otherwise done — same
   reasoning STATE_KOTLIN.md gives for staying self-contained rather than
   folded into STATE.md.

Nothing above should be read as a commitment to implement C++26 support —
this is a note for later, not a plan.
