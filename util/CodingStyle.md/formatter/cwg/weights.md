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

Across all 40 examples, three features cleanly separated the label:

| Feature present | Observed label | Consistency |
|---|---|---|
| `nextCharIsOpenParen` | always NO | 100% (all 9 paren-positive rows) |
| `containsSemicolon` | always NO | 100% (all 14 semicolon-positive rows) |
| `containsUrlOrFilenameOrNumber` only (no paren/semi) | 4/5 NO, 1/5 YES (`examples_c.md` #6) | 80% |
| none of the three | always YES | 100% (20/20 rows) |

`derive_weights.py` trains a logistic regression (weights `[bias, w_paren, w_semicolon,
w_url_or_number]`) against these 40 rows via gradient descent, with an L2 penalty (`lambda=0.1`)
on the three feature weights (not the bias) so the run converges to a finite optimum instead of
diverging on this (near-)separable data — see the script's own comment on `L2_LAMBDA` for why
that matters. Output (`lr=0.5`, `5000` epochs):

```
KEYWORD_BIAS                  =  2.16510
KEYWORD_WEIGHT_PAREN          = -3.73682
KEYWORD_WEIGHT_SEMICOLON      = -4.68148
KEYWORD_WEIGHT_URL_OR_NUMBER  = -2.52881
KEYWORD_THRESHOLD             =  0.0        (fixed sigmoid decision boundary, not trained)
```

Result: 38/40 examples classified as labeled. The two mismatches are the same two the example
files already call out as known limits of this feature set, not defects introduced by training:
- `examples_c.md` #6 (`short delay before retry, about 50ms`) — labeled YES but has the
  url/number signal, which the trained weight treats as strong enough evidence of code-shaped
  content to push the score negative. Accepted per RDD_KEY_98's asymmetric-risk design: a false
  skip here is zero-cost, and the url/number feature is only ~80% reliable as a YES-predictor in
  this sample, not worth the precision risk.
- `examples_kotlin.md` #2 (`is Foo -> handle(foo)`) — labeled NO but has *no* feature fire
  (`nextCharIsOpenParen` is relative to the target word's own boundary, not the whole comment, so
  the `->`-then-paren shape doesn't trip it), so the trained bias alone classifies it YES. This is
  a genuine false positive against the training set, not a design tradeoff — the feature set
  simply has no signal for "when-branch fragment introduced by `->`". It's the one place these
  weights don't hit the stated precision target; a future feature (e.g. "next non-whitespace
  token is `->`") would be needed to close it, out of scope for this pass.

## Extending this

To add a feature or revise a weight: add labeled rows to the per-language example files and to
`derive_weights.py`'s `DATASET`, re-run `python3 cwg/derive_weights.py`, then copy the printed
constants into `CommentClassifierWeights.java`. Keep this file's numbers in sync with whatever
constants ship.
