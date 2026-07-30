# cwg — Comment-grammar classifier Weight Generation

Reusable inputs for RDD_KEY_97 (frontier-model-assisted weight generation) and RDD_KEY_98
(threshold-from-precision-target). See `STATE_COMMENT_GRAMMAR.md` for the full architecture;
this directory only holds the labeled example sets and the derivation notes used to produce
`CommentClassifierWeights`' constants.

Only one decision is actually ambiguous at runtime (per RDD_KEY_94/96): whether a comment's
**leading word**, when it happens to also be a language keyword, is being used as ordinary
English prose (normalize — capitalize / strip trailing period) or as a literal reference to
code (skip). All other comments that reach the classifier (no non-Latin script, no leading
keyword match) are the "safe" case — the classifier's job there is just to say YES, matching
the old deterministic behavior once the two gates have already ruled out the risky cases.

Files:
- `examples_c.md`, `examples_cpp.md`, `examples_java.md`, `examples_kotlin.md` — per-language
  (RDD_KEY_96: no shared list) labeled examples of leading-keyword-ambiguous comments, each with
  its feature values and a YES/NO label plus the reasoning.
- `weights.md` — the derived weight constants for `CommentClassifierWeights`, with rationale
  tying each value back to the example sets and the 99%-precision target (RDD_KEY_98).

To regenerate/extend: add more labeled examples to the per-language files (same table format),
re-derive weights in `weights.md`, then update the constants in
`src/com/jxmake/formatter/classifier/CommentClassifierWeights.java` to match. Corpus-trained
weights (a future upgrade per RDD_KEY_97) would replace the derivation method, not this file
format — the example sets stay reusable either way.
