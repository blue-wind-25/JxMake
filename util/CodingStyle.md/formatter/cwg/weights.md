# Derived weights (RDD_KEY_97 / RDD_KEY_98)

Derived by inspection over the 40 labeled examples in `examples_c.md`, `examples_cpp.md`,
`examples_java.md`, `examples_kotlin.md`. Two separate linear formulas exist in
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

Weights chosen so paren or semicolon alone is enough to force the score below threshold
regardless of bias, and url/number alone is *also* enough to force ABSTAIN — even though that
sacrifices one observed YES case (`examples_c.md` #6, `short delay before retry, about 50ms`) —
because RDD_KEY_98's asymmetric-risk design makes a false skip free and a false positive a
visible bug; a signal that's only 80% reliable in this sample isn't worth the precision risk at
the 99% target.

```
KEYWORD_BIAS                  =  1.0
KEYWORD_WEIGHT_PAREN          = -2.5
KEYWORD_WEIGHT_SEMICOLON      = -2.5
KEYWORD_WEIGHT_URL_OR_NUMBER  = -1.5
KEYWORD_THRESHOLD             =  0.0
```

Verification against all 40 examples (score = KEYWORD_BIAS + sum of weights for features
present, compared to KEYWORD_THRESHOLD):
- No signal present → `1.0 > 0.0` → YES. Matches all 20 no-signal rows.
- Paren present (any combo) → `1.0 - 2.5 = -1.5 ≤ 0.0` → ABSTAIN. Matches all 9 paren rows (all
  labeled NO).
- Semicolon present, no paren → `1.0 - 2.5 = -1.5 ≤ 0.0` → ABSTAIN. Matches remaining semicolon
  rows (all labeled NO).
- URL/number present alone → `1.0 - 1.5 = -0.5 ≤ 0.0` → ABSTAIN. Matches 4/5; the 5th
  (`examples_c.md` #6) is an intentional false-skip, zero-cost per the design.

No example in the set produces a false positive (a NO-labeled row that the formula scores YES)
— that's the property that matters for the 99%-precision target; coverage loss on ambiguous
weak signals (the url/number case) is the accepted tradeoff, not a defect.

## Extending this

To add a feature or revise a weight: add labeled rows to the per-language example files first,
re-run the verification table above by hand (or script it — the format is regular enough), then
update `CommentClassifierWeights.java` to match. Keep this file's verification table in sync
with whatever constants ship.
