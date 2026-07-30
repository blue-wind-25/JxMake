# Derived weights (RDD_KEY_97 / RDD_KEY_98)

Derived over the 62 labeled examples in `examples_c.md`, `examples_cpp.md`, `examples_java.md`,
`examples_kotlin.md` (40 original + 22 added 2026-07-30, see "2026-07-30 re-derivation" below and
`STATE_AI.md`'s 2026-07-30 section) by `derive_weights.py` (L2-regularized logistic regression,
run with `python3 cwg/derive_weights.py` — no dependencies). Two separate linear formulas exist
in `CommentClassifier`/`CommentClassifierWeights`:

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

Across the original 40 examples, four features cleanly separated the label:

| Feature present | Observed label | Consistency |
|---|---|---|
| `nextCharIsOpenParen` | always NO | 100% (all 9 paren-positive rows) |
| `nextTokenIsArrow` | always NO | 100% (the 1 arrow-positive row, `examples_kotlin.md` #2) |
| `containsSemicolon` | always NO | 100% (all 14 semicolon-positive rows) |
| `containsUrlOrFilenameOrNumber` only (no paren/arrow/semi) | 4/5 NO, 1/5 YES (`examples_c.md` #6) | 80% |
| none of the four | always YES | 100% (20/20 rows) |

That last row was the bug: all 20 "zero-signal" examples were hand-authored YES prose
(`static analysis caught a null deref here`, `void of any real logic, this is a stub`, etc.),
with zero real-world zero-signal NO examples to balance them — see "2026-07-30 re-derivation"
below for the fix.

`derive_weights.py` trains a logistic regression (weights `[bias, w_paren, w_arrow,
w_semicolon, w_url_or_number]`) against the labeled rows via gradient descent, with an L2 penalty
(`lambda=0.1`) on the four feature weights (not the bias) so the run converges to a finite
optimum instead of diverging on this (near-)separable data — see the script's own comment on
`L2_LAMBDA` for why that matters. Current output (`lr=0.5`, `5000` epochs, 62 examples):

```
KEYWORD_BIAS                  = -0.20825
KEYWORD_WEIGHT_PAREN          = -2.28827
KEYWORD_WEIGHT_ARROW          = -1.51467
KEYWORD_WEIGHT_SEMICOLON      = -2.96142
KEYWORD_WEIGHT_URL_OR_NUMBER  = -0.51492
KEYWORD_THRESHOLD             =  0.0        (fixed sigmoid decision boundary, not trained)
```

Result: 42/62 examples classified as labeled. All 20 mismatches are the accepted asymmetric-risk
tradeoff (RDD_KEY_98): with the bias now negative, a zero-signal keyword-led comment defaults to
ABSTAIN (skip normalization) rather than YES. This means the rare "keyword used as plain English
adjective, no other signal" examples (`static analysis caught a null deref here`, `void of any
real logic, this is a stub`, etc.) now resolve to ABSTAIN instead of their labeled YES — a false
skip, zero-cost per the design philosophy, and the correct tradeoff given real-code zero-signal
keyword-led comments are overwhelmingly genuine code references, not prose (see the real
regression examples added to `examples_cpp.md`/`examples_java.md` and `STATE_AI.md`'s 2026-07-30
section).

### 2026-07-30 re-derivation

The original 40-example set had no zero-feature NO example at all, so the bias trained
positive and every real zero-signal keyword-led comment (e.g. `static operator()`, `consteval
utility`, `while loop`, `do-while`, `var usage`) got wrongly capitalized — a concrete regression
found via `make test` (9 fixtures) once `comment-normalization-classifier` was tried at its
default-`on` setting. Fixed by adding 22 new zero-feature NO rows: real lines pulled from the
failing fixtures (`test/cpp_modern_inp.cpp`, `test/cpp_combined_inp.cpp`,
`test/java_core_inp.java`, `test/java_combined_inp.java`, `test/java_comments_inp.java`) plus
hand-authored analogues for keywords/languages that didn't happen to have a failing fixture,
bringing the zero-feature split from 20 YES / 0 NO to 20 YES / 22 NO. Re-ran
`python3 cwg/derive_weights.py` and copied the new constants above into
`CommentClassifierWeights.java`. `make test`: 219/219 forward, 219/219 idempotency, with
`comment-normalization-classifier` now defaulting `on`.

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
