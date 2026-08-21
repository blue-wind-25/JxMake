# Design Notes

Rationale behind a handful of formatter decisions that aren't obvious from
usage alone. This is a reader-facing distillation, not a change log or an
implementation-progress tracker — it has no dates and isn't updated per
commit.

## GRU comment classifier: why `abstainThreshold = 0.76`

The comment-normalization classifier (`gru-classifier`, see `README.md`'s
"Comment classifier (GRU)" section) abstains below a softmax confidence
cutoff rather than forcing a low-confidence guess. `0.7` was chosen over
the GRU trainer's raw default of `0.5` via a held-out cross-validation
sweep: it roughly halves the false-positive rate (wrongly capitalizing a
comment that was actually a real code reference, e.g. `return x;`) at
little cost to how many genuinely ambiguous comments get resolved.

Raised to `0.76` after a further sweep (0.68/0.72/0.76) prompted by an
unexpectedly high 5-round full-corpus CV precision figure: against each CV
round's own held-out test split, 0.76 monotonically beat 0.72 and 0.70 on
both the false-negative and false-positive rate (no threshold in the swept
range made either worse). A separate, label-free check against a real,
unrelated codebase (out-of-distribution comments the model never trained
on) found 0.68/0.72/0.76 behaved almost identically to 0.70 — no evidence
any of the three regressed decisiveness on unseen text.

## HTML5 tree-construction gap levels: why they're narrow approximations

`html5-tc-gap-level` (see `README.md`'s "HTML5 tree-construction gap
levels" section) intentionally implements narrow approximations of the
HTML5 spec's tree-construction algorithms rather than the full spec
algorithms, because this formatter's whole design is preserve-as-written
rather than parse-into-a-real-DOM-and-reprint. Each level trades spec
completeness for staying close to that preserve-as-written model:

- **Level 2**'s foster-parenting check (`isInTableInsertionMode()`) is a
  single-level "direct child of an open `<table>`" test rather than a full
  ancestor scan. A full ancestor scan was tried first and rejected: it
  incorrectly re-evaluates a fostered element's own already-relocated
  descendants, double-processing content that a single-level check
  handles correctly.
- **Level 4**'s adoption-agency approximation tracks only the single most
  recently orphaned formatting element, not the spec's full "list of
  active formatting elements" + "furthest block" + "bookmark" algorithm.
  This covers the common case (one misnested `<b>`/`<i>`-style element)
  without the complexity of the full algorithm, at the cost of not
  reconstructing a second, simultaneous misnesting under the same
  ancestor.

## JS/TS import ordering: why bundler path-mapped imports aren't detected

Local-import detection for JS/TS (`js-import-order`, see `README.md`'s
"JS/TS import groups" section) is purely syntactic: an import is `local`
iff its specifier starts with `./` or `../`. This is a deliberate
simplification — resolving a bundler or tsconfig `baseUrl`/`paths`
mapping (e.g. treating `import { Widget } from "components/Widget"` as
local because it resolves to the project's own source tree) would require
this formatter to understand `tsconfig.json`/bundler config, which is out
of scope for a tokenizer-based formatter with no project-graph awareness.
No source-root config key is planned to close this gap.

## GDR joined-brace-style gap: why multipass is opt-in rather than default

`curly-general-scope-reindent-multipass` (see `README.md`'s "General
scope-depth reindentation (GDR)" section) fixes a real non-idempotency on
one-true-brace-style source by running a bounded convergence loop instead
of a single pre-pass-then-pipeline pass. It stays a separate, off-by-default
key rather than folding into `curly-general-scope-reindent` itself because
it changes the cost profile of formatting (multiple pre-pass-plus-pipeline
cycles per file instead of exactly one) — a tradeoff only worth paying for
source that actually exercises the gap, not something to impose on every
GDR-enabled file by default.

## GDR postpass: why it only touches block-structure indentation, not continuation lines

The `curly-general-scope-reindent-postpass` key (see `README.md`'s
Configuration section) reuses GDR's own reindenter as a genuine
post-pass, re-deriving indentation directly from the already-finished output
rather than from source ahead of the pipeline. GDR's model for continuation
indentation (a wrapped call or condition spanning multiple lines) is a
simple "one indent level per open bracket" rule, while the main pipeline's
own continuation-indent logic is more nuanced. Re-deriving a wrapped line's
indentation from GDR's simpler model, on top of output the pipeline already
settled on, could silently disagree with a placement the pipeline had
already gotten right. Rather than trying to make GDR's continuation model
match the pipeline's exactly, the postpass instead leaves any line that's
part of such a wrap alone entirely, and only ever re-targets plain
block-structure indentation that sits outside any wrap — the narrower rule
is easier to reason about and to keep correct than chasing feature parity
between two independent indentation models.
