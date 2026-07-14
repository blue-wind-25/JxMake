# Strategy: range-v3 Idempotency Bug (item 20, bug (a))

Status: open, non-reproducible in minimal repros. This doc is a playbook, not a diagnosis —
treat every "hypothesis" below as unproven until logs confirm it.

## Recap

Two symptoms, both only reproducing in the full real files (`utility/any.hpp`,
`iterator/common_iterator.hpp`, `meta.hpp`), never in a minimal repro:

1. A nested template-argument angle bracket (e.g. `meta::if_c<std::is_reference<T>() ||
   copyable<T>, T>`) fails to converge tight/loose spacing between round1 and round2.
2. A closing-brace-plus-trailing-comment line (`}; // namespace ranges`, etc.) renders at a
   different indentation level between round1 and round2.

Already tried and reverted: adding `||`/`&&`/`!` to `TokenizerCore.isGenericSafeToken`'s OP
whitelist. This changed *which* round showed tight vs. loose spacing but did not make them
converge. Do not retry this blind — if revisited, it should be alongside the instrumentation
below, not instead of it.

## Why minimal repros keep failing

"Only the full file triggers it" means the bug depends on state that accumulates across many
declarations/scopes before the failing construct is reached — stack depth, some counter, or
token identity that differs depending on what came before. Shrinking the file naively removes
that accumulated state along with the noise. Two ways around this:

## Plan A — Instrumented dual-run diff (try first)

Rationale: round2's input *is* round1's output. If they diverge, something differs between
"tokens as read from the pristine source" and "tokens as read from round1's own output" —
and that's true by definition, not a hypothesis. Find the first point of disagreement.

1. Add temporary logging (same pattern as RDD_KEY_136's `System.err` span-indent prints,
   removed before commit) to:
   - `TokenizerCore.reclassifyAngleBrackets` — log every `invalidateAll(openStack)` call site
     (token index, triggering token, and full openStack contents at that moment).
   - Whichever pass computes brace/comment indentation (`ScopePipeline`/`BlockStructureRule`) —
     log per-frame indent decisions (`spanIndent`, `braceLineIndent`, `effectiveSpanIndent`,
     whatever is live) keyed by source line or token index.
2. Run round1 on the real failing file, capture the log.
3. Run round2 (format round1's output), capture the log.
4. Diff the two logs by token-index/line-number correspondence, not raw line number (round1
   changes line numbers). The **first index where the logs disagree** is the root cause,
   almost by construction — no further guessing needed once found.
5. Fix should target that exact divergence point, not the symptom's surface shape.

Stop condition: you have a concrete "token N was classified as X in round1's tokenization and
Y in round2's tokenization, because <specific mechanism>" — not "we think it's related to
angle brackets in general."

## Plan B — Delta debugging (fallback if Plan A's logs are too noisy to read by hand)

1. Script a pure idempotency check: `diverges(file) := format(format(file)) != format(file)`
   restricted to a byte-range or line-range of interest (the specific failing construct).
2. Run automated bisection (ddmin-style, or C-Reduce if it handles this token shape well
   enough) against the actual failing file, using `diverges()` as the reduction predicate.
3. Because ddmin doesn't assume where the accumulated state lives, it can find a smaller
   input that still reproduces the divergence even though manual attempts couldn't — the
   state carrier gets preserved automatically since removal that breaks the divergence is
   rejected by the predicate.
4. Once a smaller repro exists, apply Plan A's instrumentation to *that* — much easier to read.

Use Plan B if Plan A's dual-run diff is unreadable (too many divergent entries with no clear
first cause) or if it's unclear which pass to instrument at all.

## Guardrails

- Symptom (1) and symptom (2) may or may not share a root cause ("both symptoms share the
  general shape of round1-output-vs-pristine-input divergence" is a structural similarity,
  not evidence of a shared bug). Diagnose independently; don't assume a single fix covers both.
- Any fix must be re-verified against the full `make test` suite plus full-tree round1/round2
  idempotency on all 311+ files, not just the three known offenders (per existing project
  convention).
- If a fix is found but its correctness can't be fully proven (as with the reverted attempt),
  document it as tentative in STATE_C_CPP_JAVA.md rather than committing it silently — matches
  existing project practice of reverting unproven tokenizer changes.
- Don't let this doc's framing (Plan A first) override better judgment once inside the actual
  codebase with real tooling access — if the CLI session finds a faster path, take it.
