tools/gru/ — GRU comment-classifier training/evaluation tooling
=================================================================

This directory holds the offline tooling for Step 3 (GRU comment-classifier
abstain resolution) — see ../../STATE_AI.md for the full design/decision
history. None of the tools here are shipped in the runtime JAR; they produce
or consume training data and weights files under /tmp (RDD_EXT_19: real
extracted/labeled corpora and trained weights are never committed).

Run every command below from the formatter/ directory (same convention as
`make`), unless noted otherwise.

Pipeline order
--------------
1. Extract raw comments from a source tree           -> extract_comments.py
2. Measure the rule-based classifier's ABSTAIN rate  -> CommentAbstainTally.java
3. Extract Pool A / Pool B ABSTAIN candidates        -> ExtractPoolA.java / extract_pool_b.py
4. Hand-label each candidate (YES/NO)                -> manual, RDD_EXT_20 schema
5. Add the targetWordIndex column                    -> add_target_index.py
6. (Re)build the explicit vocab                      -> build_vocab.py
7. Train                                             -> GruTrainer.java (make gru-train)
8. Evaluate precision against a held-out set         -> GruEval.java
9. Bound precision variance via repeated splits      -> cross_validate.py

Steps 1+3 for many sources at once, minus the labeling, are automated by
acquire_corpus.sh.

Tools
-----

extract_comments.py
    Walks a source tree, maps file extensions to Lang-recognized languages,
    extracts marker-stripped comment text per language's comment syntax into
    a flat corpus file. Excludes 3rd_party directories (a single vendored
    non-code comment file can dominate a language's stats).

        python3 tools/gru/extract_comments.py <root-dir> [<root-dir> ...] --out <comments-file>

CommentAbstainTally.java
    Feeds a comments file (from extract_comments.py) through the real
    CommentFeatureExtractor/CommentClassifier pipeline and tallies
    YES/NO/ABSTAIN counts overall and per language. Wired as a Makefile
    target rather than run directly:

        make gru-measure-abstain-rate GRU_ABSTAIN_INPUT=<comments-file>

ExtractPoolA.java
    Reads a comments file, writes only the ABSTAIN comments with
    hasLeadingKeywordMatch set (Pool A: keyword-ambiguity), excluding
    NonLatinScriptGate ABSTAINs. Wired as a Makefile target:

        make gru-extract-pool-a GRU_ABSTAIN_INPUT=<comments-file> GRU_POOL_A_OUT=<pool-a-file>

extract_pool_b.py
    Reads a comments file, writes candidates matching RDD_EXT_15's
    recall-favoring grep filter (Pool B: period-ambiguity) — independent of
    CommentClassifier. Wired as a Makefile target:

        make gru-extract-pool-b GRU_ABSTAIN_INPUT=<comments-file> GRU_POOL_B_OUT=<pool-b-file>

Hand labeling (no tool — manual step)
    Assign YES/NO ground truth to each Pool A / Pool B candidate per
    RDD_EXT_20's schema: <lang>\t<label:YES|NO>\t<escaped-comment-text>.
    Pool A: is this leading-keyword comment genuine prose (YES) or
    code/commented-out-code/a data label (NO)? Pool B: does the comment's
    one real sentence-ending period need stripping (YES), or does the dot
    belong to an abbreviation/license block/non-sentence text (NO)?

    Practical steps:
      1. Extract an acquire_corpus.sh archive back under /tmp if needed:
             tar -xJf ~/Projects/JxMake/0_excluded_directory/personal/GruArtifacts/gru_corpus.tar.xz -C /tmp
      2. Open a pool_a_<source>.txt or pool_b_<source>.txt file. Each line
         starts as <lang>\t<text> (no label column yet).
      3. For each line, insert the label as the 2nd tab-separated field, so
         <lang>\t<text> becomes <lang>\t<label>\t<text>. Save the result as
         a separate ..._labeled.txt file (keep the unlabeled original as a
         backup until the pass is done).
      4. You don't have to label every candidate in a file — labeling a
         representative subset per source is enough to grow the corpus
         (the first batch labeled 167 Pool A + 241 Pool B this way).

    Worked examples (real lines from pool_a_eCxx.txt / pool_b_eCxx.txt):

        Unlabeled:  cpp\t false = target-serial bridge ; true = JTAG2UPDI-serial bridge
        Labeled:    cpp\tNO\t false = target-serial bridge ; true = JTAG2UPDI-serial bridge
        Reasoning:  leading word "false" here describes a data value, not a sentence -> NO

        Unlabeled:  cpp\t if(pwmState.cnt)
        Labeled:    cpp\tNO\t if(pwmState.cnt)
        Reasoning:  literal commented-out code, not prose -> NO

        Unlabeled:  cpp\t An application base class that contains all common member types/constants/variables/functions/etc.
        Labeled:    cpp\tYES\t An application base class that contains all common member types/constants/variables/functions/etc.
        Reasoning:  one real sentence; the trailing period is a genuine sentence-ender
                    (the mid-sentence "etc." dot is what triggered Pool B's filter, not
                    the trailing one) -> YES

        Unlabeled:  cpp\t\n * ----...----\n * Copyright (C) ... GNU Lesser General
                    Public License ...\n * ----...----\n  (a multi-line license header)
        Labeled:    cpp\tNO\t<same text>
        Reasoning:  spans 2+ newlines, a license block not a single sentence -> NO
                    (same rule the first labeled batch used for these)

    Once labeled, run add_target_index.py (next tool below) on the result.

add_target_index.py
    Inserts RDD_EXT_21's targetWordIndex column into an RDD_EXT_20-schema
    labeled file: index 0 for Pool A (the leading keyword), last-token index
    for Pool B. Its own tokenize() must stay bit-for-bit identical to
    GruClassifier.tokenize — cross-check before trusting a change to it.

        python3 tools/gru/add_target_index.py <pool-a|pool-b> <input-file> --out <output-file>

build_vocab.py
    Generates the permanent, checked-in explicit_vocab.txt (RDD_EXT_22):
    one keyword slot per Lang.java-supported/planned language keyword, plus
    the most frequent remaining words from a real corpus, up to a target
    size. Append-only once any weights file has been trained against it —
    reordering/removing lines shifts embedding-row indices and corrupts
    existing weights files.

        python3 tools/gru/build_vocab.py <corpus-file> --out tools/gru/explicit_vocab.txt --target-size 3500

GruTrainer.java
    Trains a GruClassifier weights file from an RDD_EXT_21-schema labeled
    file: Xavier/Glorot init, per-example forward+backward+Adam step, 20%
    held-out validation split with patience-based early stopping. Loads
    tools/gru/explicit_vocab.txt by default (--vocab= to override; empty/
    missing path falls back to deriving vocab from the training file itself,
    useful only for quick smoke tests). Wired as a Makefile target:

        make gru-train GRU_TRAIN_INPUT=<labeled-file> GRU_WEIGHTS_OUT=<weights-file> \
            [GRU_TRAIN_ARGS="--epochs=40 --patience=6 --vocab=<path> --seed=<n>"]

    or invoked directly once compiled:

        java -cp target/classes:<gru-tools-classes> GruTrainer <labeled-file> <weights-file> \
            [--epochs=N] [--patience=N] [--vocab=<path>] [--seed=N]

GruEval.java
    Loads a trained weights file and reports precision/abstain-rate against
    an RDD_EXT_21-schema examples file (usually a held-out split the
    training run never saw).

        java -cp target/classes:<gru-tools-classes> GruEval <weights-file> <examples-file>

acquire_corpus.sh
    Automates acquisition + extraction only (steps 1+3 above) across a
    hardcoded list of sources: local dogfood repos under ~ plus vetted
    MIT/BSD-3-Clause/Apache-2.0 public GitHub repos. Public repos are
    shallow-cloned to a scratch dir and removed again after extraction
    unless --keep-clones is passed. Deliberately stops before labeling —
    that stays a manual step. Writes per-source comments/pool-a/pool-b files
    under --out-dir (default /tmp/gru_corpus) and prints a summary table.

        tools/gru/acquire_corpus.sh [--out-dir DIR] [--keep-clones] [--only name1,name2,...]

    Extend the SOURCES list by hand as new repos get vetted (license check
    first) — don't add unvetted sources.

cross_validate.py
    Bounds the variance on a single held-out-split precision estimate via
    repeated Monte Carlo cross-validation: reshuffles a combined
    RDD_EXT_21-schema labeled file with a fresh seed each round, splits
    80/20, retrains GruTrainer from scratch on the 80%, evaluates precision
    on the untouched 20% via GruEval, and reports mean/stdev/min/max across
    rounds instead of trusting one split.

        python3 tools/gru/cross_validate.py <combined-labeled-file> [--rounds 5] \
            [--work-dir /tmp/gru_cv] [--epochs 40] [--patience 6] [--vocab <path>]

Optional: chat-LLM synthetic augmentation (NOT the real corpus)
-----------------------------------------------------------------
See STATE_AI.md's "Optional synthetic-augmentation tooling" note for the
rationale/caveats. These two scripts let you pad Pool A/B via a manually
copy-pasted prompt to a free-tier chat LLM (Gemini, Grok, etc.) instead of
the API — no upload needed, the prompt is self-contained. Keep any resulting
file clearly labeled as synthetic; do not merge it into the real combined
corpus without review.

gen_synthetic_prompt.py
    Reads explicit_vocab.txt and prints a copy/paste-ready prompt asking a
    chat LLM for Pool A + Pool B lines in RDD_EXT_20/21 schema, using the
    next unused slice of the vocab as Pool A's leading-keyword list. Tracks
    which word index to resume from in a small state file next to the
    script, so repeated runs walk forward through the vocab (wrapping at
    the end) instead of asking for the same words twice.

        python3 tools/gru/gen_synthetic_prompt.py [--words-per-batch 20] \
            [--vocab <path>] [--state <path>] [--langs c cpp java ...] [--reset] [--out <file>]

    Paste the printed prompt into the chat LLM, then paste its full reply
    (Pool A + Pool B lines together, in any order) into one file for the
    next tool.

regroup_synthetic.py
    Splits a pasted-in file containing scattered Pool A/B lines (including
    concatenated replies from more than one chat/model) back into separate
    pool_a.tsv / pool_b.tsv files. Tolerates inconsistent space/tab spacing
    between fields. Anything it can't confidently classify (bad label,
    non-integer index, or a targetWordIndex it can't match to Pool A's 0 or
    Pool B's last token) goes to unresolved.tsv for manual review rather
    than being dropped or guessed at — see the known tokenizer-mismatch
    caveat in STATE_AI.md before assuming unresolved lines are errors.

        python3 tools/gru/regroup_synthetic.py --input <pasted-file> --outdir <dir>

Backing up real corpora / weights to GruArtifacts
--------------------------------------------------
Per RDD_EXT_19, nothing under /tmp (extracted comments, Pool A/B candidates,
labeled corpora, trained weights) is ever committed to the repo. If you want
to keep a real acquisition run or a trained weights file around longer than
/tmp survives, archive it into the user's personal (non-repo) directory,
~/Projects/JxMake/0_excluded_directory/personal/GruArtifacts/ — never into
the repo itself.

    # a full acquire_corpus.sh output directory
    tar -cJf ~/Projects/JxMake/0_excluded_directory/personal/GruArtifacts/gru_corpus_<date>.tar.xz \
        -C /tmp gru_corpus

    # a single trained weights file
    cp /tmp/<weights-file>.json \
        ~/Projects/JxMake/0_excluded_directory/personal/GruArtifacts/gru_trained_weights_<description>_<date>.json

Suffix filenames with a date and/or a short description (example count,
vocab version, etc.) so multiple backups don't collide or get confused with
each other later.

Notes
-----
- Per RDD_EXT_19, nothing this directory's tools produce from real source
  code (extracted comments, Pool A/B candidates, labeled corpora, trained
  weights files) is ever committed to the repo. Only the tools themselves,
  and the small fake-illustrative tools/gru/sample_examples.txt, are checked
  in.
- This system runs Python 3.6 — avoid post-3.6 stdlib features (e.g.
  subprocess.run(capture_output=True) needs 3.7+; use
  stdout=PIPE, stderr=PIPE, universal_newlines=True instead) in any new
  script here.
