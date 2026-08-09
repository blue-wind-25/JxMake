# Derived weights (RDD_KEY_97 / RDD_KEY_98)

Derived over the 221 labeled examples in `examples_c.md`, `examples_cpp.md`, `examples_java.md`,
`examples_kotlin.md`, `examples_js.md`, `examples_ts.md` (62 as of 2026-07-30 + 63 added
2026-07-31 + 48 added 2026-08-01 + 48 more added 2026-08-01, see "2026-07-31
re-derivation"/"2026-08-01 re-derivation" below and `STATE_AI.md`'s corresponding sections) by
`derive_weights.py` (L2-regularized logistic
regression, run with `python3 tools/classifier_weights/derive_weights.py` — no dependencies).
`DATASET` is parsed directly from the `examples_*.md` tables' own feature columns as of
2026-08-01 (previously a hand-transcribed mirror — see `STATE_AI.md`'s "`derive_weights.py`'s
`DATASET` made auto-extending" session), so adding rows to those files is now sufficient on its
own; nothing else needs updating before the next re-run. Two separate linear formulas exist in
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
`L2_LAMBDA` for why that matters. Current output (`lr=0.5`, `5000` epochs, 125 examples):

```
KEYWORD_BIAS                 = -0.08711
KEYWORD_WEIGHT_PAREN         = -3.08818
KEYWORD_WEIGHT_ARROW         = -1.57140
KEYWORD_WEIGHT_SEMICOLON     = -3.57490
KEYWORD_WEIGHT_URL_OR_NUMBER = -0.93665
KEYWORD_THRESHOLD            =  0.0        (fixed sigmoid decision boundary, not trained)
```

Result: 82/125 examples classified as labeled. All 43 mismatches are the accepted asymmetric-risk
tradeoff (RDD_KEY_98): with the bias negative, a zero-signal keyword-led comment defaults to
ABSTAIN (skip normalization) rather than YES. This means the rare "keyword used as plain English
adjective, no other signal" examples (`static analysis caught a null deref here`, `void of any
real logic, this is a stub`, etc.) now resolve to ABSTAIN instead of their labeled YES — a false
skip, zero-cost per the design philosophy, and the correct tradeoff given real-code zero-signal
keyword-led comments are overwhelmingly genuine code references, not prose (see the real
regression examples added to `examples_cpp.md`/`examples_java.md` and `STATE_AI.md`'s 2026-07-30
section).

### 2026-07-31 re-derivation

Extending the example sets for the "extend classifier_weights" session (adding `KEYWORDS_JS`/
`KEYWORDS_TS` coverage plus a handful more rows each to the original four files, 62 → 113
examples) and re-deriving immediately flipped `KEYWORD_BIAS` positive again (`+0.21890`) —
reopening the exact 2026-07-30 regression. Root cause: the new `examples_js.md`/`examples_ts.md`
rows leaned heavily on zero-mechanical-feature YES examples (illustrating ordinary prose) without
a matching count of zero-feature NO false-friend examples, so the two new files alone shifted the
dataset's zero-signal split from balanced back to YES-heavy. `derive_weights.py` has no notion of
real-world class-frequency prior — it fits whatever ratio is literally present in `DATASET` — so
this is a property of the curated example set, not the underlying language. Fixed the same way as
2026-07-30: added 6 more zero-feature NO rows to each of `examples_js.md` (rows 19-24) and
`examples_ts.md` (rows 19-24), bringing the total to 125 examples and the bias back negative
(`-0.08711`). `make test` re-confirmed passing after the `CommentClassifierWeights.java` update
(see `STATE_AI.md`'s 2026-07-31 section for the full count).

### 2026-08-01 re-derivation

Grew the hand-labeled hard-case corpus by 48 rows (8 per file, all zero-mechanical-feature,
targeting keywords with previously zero example coverage — `case`/`const`/`for`/`return` in
`examples_c.md`, `catch`/`override`/`public`/`protected` in `examples_cpp.md`, and so on across
all six files; see `STATE_AI.md`'s "grew the hand-labeled hard-case set" session for the full
per-file/per-keyword breakdown), bringing the total to 173 examples. Re-ran `derive_weights.py`
(now auto-parsing `DATASET` from the `.md` files directly, no manual transcription step):

```
KEYWORD_BIAS                 = -0.05634
KEYWORD_WEIGHT_PAREN         = -3.10644
KEYWORD_WEIGHT_ARROW         = -1.55819
KEYWORD_WEIGHT_SEMICOLON     = -3.59572
KEYWORD_WEIGHT_URL_OR_NUMBER = -0.96329
KEYWORD_THRESHOLD            =  0.0        (fixed sigmoid decision boundary, not trained)
```

Result: 106/173 examples classified as labeled (61.3%, down from 82/125 = 65.6% — expected, not a
regression: the 48 new rows are deliberately the hardest zero-mechanical-feature shape, diluting
the fraction correctly resolved by this 4-feature linear model without changing anything about
how well it does on the original 125). Bias stayed negative (still the correct asymmetric-risk
tradeoff — zero-signal keyword-led comments default to ABSTAIN, not YES), and all four feature
weights stayed within a few percent of their 2026-07-31 values, so the added rows reinforced the
existing decision boundary rather than shifting it. `CommentClassifierWeights.java` updated to
match; `make test`: 225/225 forward, 225/225 idempotency, no regressions.

### 2026-08-01 re-derivation (second growth pass, same day)

Grew the corpus by another 48 rows (8 per file: `if`/`long`/`else`/`switch` in `examples_c.md`;
`friend`/`throw`/`try`/`using` in `examples_cpp.md`; `break`/`catch`/`finally`/`package` in
`examples_java.md`; `break`/`do`/`else`/`in` in `examples_kotlin.md`; `break`/`catch`/`if`/`return`
in `examples_js.md`; `declare`/`is`/`protected`/`string` in `examples_ts.md`), bringing the total
to 221 examples — same balance discipline as prior passes (each new keyword got a matched
zero-feature YES/NO pair so the batch can't skew `KEYWORD_BIAS`; see `STATE_AI.md`'s corresponding
session for the full per-file breakdown). Re-ran `derive_weights.py`:

```
KEYWORD_BIAS                 = -0.04180
KEYWORD_WEIGHT_PAREN         = -3.10833
KEYWORD_WEIGHT_ARROW         = -1.52024
KEYWORD_WEIGHT_SEMICOLON     = -3.60170
KEYWORD_WEIGHT_URL_OR_NUMBER = -0.97619
KEYWORD_THRESHOLD            =  0.0        (fixed sigmoid decision boundary, not trained)
```

Result: 130/221 examples classified as labeled (58.8%, down from 106/173 = 61.3% — same expected
dilution pattern as every prior growth pass, not a regression). Bias stayed negative and all four
feature weights stayed within a few percent of their 2026-08-01-morning values, so the decision
boundary is stable across four consecutive growth passes now. `CommentClassifierWeights.java`
updated to match; `make test` re-run pending as part of this same follow-up (see `STATE_AI.md`).

### 2026-08-03 re-derivation (522-example set)

The hand-labeled hard-case set grew past 221 (up through the 2026-08-02 growth passes recorded in
`STATE_AI.md`) to 522 rows across `examples_{c,cpp,java,kotlin,js,ts}.md`, but `derive_weights.py`
hadn't been re-run against the grown set yet. Re-ran it:

```
KEYWORD_BIAS                 = -1.18218
KEYWORD_WEIGHT_PAREN         = -2.17830
KEYWORD_WEIGHT_ARROW         = -0.64725
KEYWORD_WEIGHT_SEMICOLON     = -2.66553
KEYWORD_WEIGHT_URL_OR_NUMBER = -0.03338
KEYWORD_THRESHOLD            =  0.0        (fixed sigmoid decision boundary, not trained)
```

Result: 407/522 examples classified as labeled (77.97%), all mismatches the same accepted
asymmetric-risk tradeoff as every prior pass. Bias stayed negative. `CommentClassifierWeights.java`
updated to match.

### 2026-08-10 re-derivation (594-example set, python3 added)

Confirmed (STATE_AI.md's investigation this session) that python3 is the only one of
{json5, css, yaml, toml, xml, html5, js, ts, python3, makefile, bash, powershell} whose comment
normalization actually reaches `KeywordAmbiguityGate` (`MiscRuleIndent`'s `#`-comment path, wired
2026-08-08) -- the rest have either no comment-classifier wiring at all, or (yaml/toml/xml/
makefile/bash/powershell) use `ToolingCommentNormalizer`'s deliberately classifier-free ad hoc
capitalization instead. Added `examples_python3.md` (48 rows, `KeywordAmbiguityGate.KEYWORDS_PYTHON`
+ `lang.isPython3` dispatch branch added alongside, same bug shape as the original JS/TS
wrong-`KEYWORDS_C`-fallback fix) plus 4 new zero-feature NO rows each to `examples_{c,cpp,java,
kotlin,js,ts}.md` (a "more NO samples" pass, no new keywords -- naturalistic-phrasing NO coverage
for keywords already present). New total: 594 rows across all 7 `examples_*.md` files. Re-ran
`derive_weights.py`:

```
KEYWORD_BIAS                 = -1.14719
KEYWORD_WEIGHT_PAREN         = -2.31089
KEYWORD_WEIGHT_ARROW         = -0.61513
KEYWORD_WEIGHT_SEMICOLON     = -2.63047
KEYWORD_WEIGHT_URL_OR_NUMBER = -0.06490
KEYWORD_THRESHOLD            =  0.0        (fixed sigmoid decision boundary, not trained)
```

Result: 459/594 examples classified as labeled (77.27%), same accepted asymmetric-risk mismatch
pattern as every prior pass (essentially unchanged from the 522-row set's 77.97% -- the added rows
were mostly zero-feature NO, which the four-feature linear model can't separate from zero-feature
YES any better than before; expected, not a regression). Bias stayed negative.
`CommentClassifierWeights.java` updated to match. `make test`: unchanged from before this session's
changes (see STATE_AI.md).

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
`python3 tools/classifier_weights/derive_weights.py` and copied the new constants above into
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
4. Re-run `python3 tools/classifier_weights/derive_weights.py`, copy the printed constants into
   `CommentClassifierWeights.java`, and update this file's numbers to match.
5. `make test` must stay unchanged (classifier defaults to `off`); re-run the `/tmp` smoke test
   with the classifier `on` to confirm the new feature fires as expected.

## Extending this

To add a labeled example without a new feature: add rows to the per-language example files and to
`derive_weights.py`'s `DATASET`, re-run `python3 tools/classifier_weights/derive_weights.py`, then copy the printed
constants into `CommentClassifierWeights.java`. Keep this file's numbers in sync with whatever
constants ship. To add a new feature entirely, see "Adding a feature" above.
