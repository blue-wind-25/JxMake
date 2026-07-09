# Derived weights (RDD_KEY_97 / RDD_KEY_98)

Derived over the 40 labeled examples in `examples_c.md`, `examples_cpp.md`, `examples_java.md`,
`examples_kotlin.md` by `derive_weights.py` (L2-regularized logistic regression, run with
`python3 cwg/derive_weights.py` — no dependencies). Two separate linear formulas exist in
`CommentClassifier`/`CommentClassifierWeights`:

## Main path (no leading-keyword ambiguity; both gates already cleared)

By construction, everything reaching this path has already passed the non-Latin gate and the
keyword-match gate — i.e. it's exactly the case the old deterministic logic always normalized
(no `isCommentNoCapitalizeWord` skip applied). So the formula only needs a constant that clears
the threshold:

- `BIAS = 1.0`, `THRESHOLD = 0.0` → `score = 1.0 > 0.0` → always `YES`.

This restores the pre-classifier deterministic behavior for the non-ambiguous majority case,
which is what "classifier on" is supposed to preserve per STATE_COMMENT_GRAMMAR.md's intro
(an accuracy *upgrade*, not a stricter gate on cases that were never ambiguous).

## Keyword-ambiguity path (`KeywordAmbiguityGate.resolveAmbiguousKeyword`, stage 2)

Across all 40 examples, four features cleanly separated the label:

| Feature present | Observed label | Consistency |
|---|---|---|
| `nextCharIsOpenParen` | always NO | 100% (all 9 paren-positive rows) |
| `nextTokenIsArrow` | always NO | 100% (the 1 arrow-positive row, `examples_kotlin.md` #2) |
| `containsSemicolon` | always NO | 100% (all 14 semicolon-positive rows) |
| `containsUrlOrFilenameOrNumber` only (no paren/arrow/semi) | 4/5 NO, 1/5 YES (`examples_c.md` #6) | 80% |
| none of the four | always YES | 100% (20/20 rows) |

`derive_weights.py` trains a logistic regression (weights `[bias, w_paren, w_arrow,
w_semicolon, w_url_or_number]`) against these 40 rows via gradient descent, with an L2 penalty
(`lambda=0.1`) on the four feature weights (not the bias) so the run converges to a finite
optimum instead of diverging on this (near-)separable data — see the script's own comment on
`L2_LAMBDA` for why that matters. Output (`lr=0.5`, `5000` epochs):

```
KEYWORD_BIAS                  =  2.48420
KEYWORD_WEIGHT_PAREN          = -3.96297
KEYWORD_WEIGHT_ARROW          = -3.22603
KEYWORD_WEIGHT_SEMICOLON      = -4.93396
KEYWORD_WEIGHT_URL_OR_NUMBER  = -2.80469
KEYWORD_THRESHOLD             =  0.0        (fixed sigmoid decision boundary, not trained)
```

Result: 39/40 examples classified as labeled. The one remaining mismatch is an accepted tradeoff,
not a defect:
- `examples_c.md` #6 (`short delay before retry, about 50ms`) — labeled YES but has the
  url/number signal, which the trained weight treats as strong enough evidence of code-shaped
  content to push the score negative. Accepted per RDD_KEY_98's asymmetric-risk design: a false
  skip here is zero-cost, and the url/number feature is only ~80% reliable as a YES-predictor in
  this sample, not worth the precision risk.

### Adding a feature (worked example: `nextTokenIsArrow`)

Before this feature existed, `examples_kotlin.md` #2 (`is Foo -> handle(foo)`, labeled NO) had no
feature fire at all, so the trained bias alone classified it YES — a genuine false positive
against the training set, not a tradeoff. The fix was a new `CommentFeatureVector.nextTokenIsArrow`
field: `CommentFeatureExtractor` scans the comment tail after the target word's end for `"->"`
(deliberately permissive, same philosophy as the URL/number regex — a false-positive arrow match
only ever costs an ABSTAIN, never a wrong YES), and `KeywordAmbiguityGate.resolveAmbiguousKeyword`
applies `KEYWORD_WEIGHT_ARROW` when it fires. The general recipe for adding a feature:

1. Add the field to `CommentFeatureVector` and populate it in `CommentFeatureExtractor`.
2. Add a weight constant to `CommentClassifierWeights` and consume it in
   `KeywordAmbiguityGate.resolveAmbiguousKeyword`.
3. Add the new column to every row of `derive_weights.py`'s `DATASET` (0 unless the example's
   comment text actually has the new signal) and to the per-language example tables.
4. Re-run `python3 cwg/derive_weights.py`, copy the printed constants into
   `CommentClassifierWeights.java`, and update this file's numbers to match.
5. `make test` must stay unchanged (classifier defaults to `off`); re-run the `/tmp` smoke test
   with the classifier `on` to confirm the new feature fires as expected.

## Extending this

To add a labeled example without a new feature: add rows to the per-language example files and to
`derive_weights.py`'s `DATASET`, re-run `python3 cwg/derive_weights.py`, then copy the printed
constants into `CommentClassifierWeights.java`. Keep this file's numbers in sync with whatever
constants ship. To add a new feature entirely, see "Adding a feature" above.
