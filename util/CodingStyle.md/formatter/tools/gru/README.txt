tools/gru/ — GRU comment-classifier training/evaluation tooling
=================================================================

This directory holds the offline tooling for Step 3 (GRU comment-classifier
abstain resolution) — see ../../STATE_AI.md for the full design/decision
history. None of the tools here are shipped in the runtime JAR; they produce
or consume training data and weights files under /tmp (RDD_EXT_19: real
extracted/labeled corpora and trained weights are never committed) — with
one named exception, `sample_default.txt` and
`code-formatter-ai-assist-weights.json` (RDD_KEY_217, see Notes below).

Run every command below from the formatter/ directory (same convention as
`make`), unless noted otherwise.

Pipeline order
--------------
1. Extract raw comments from a source tree          -> extract_comments.py
2. Measure the rule-based classifier's ABSTAIN rate -> CommentAbstainTally.java
3. Extract Pool A / Pool B ABSTAIN candidates       -> ExtractPoolA.java / extract_pool_b.py
4. Hand-label each candidate (YES/NO)               -> manual, RDD_EXT_20 schema
5. Add the targetWordIndex column                   -> add_target_index.py
6. (Re)build the explicit vocab                     -> build_vocab.py
7. Train                                            -> GruTrainer.java (make gru-train)
8. Evaluate precision against a held-out set        -> GruEval.java
9. Bound precision variance via repeated splits     -> cross_validate.py

Steps 1+3 for many sources at once, minus the labeling, are automated by
acquire_corpus.sh.

Quick default path (replaces steps 2-6, no hand-labeling): `make
gru-acquire-corpus` acquires a corpus (step 1, via acquire_corpus.sh — which
also redacts likely API keys/tokens and dedupes exact-duplicate lines
per-source before Pool A/B extraction, see acquire_corpus.sh below) AND
auto-labels it via GenerateSampleDefault.java (below) straight into
tools/gru/sample_default.txt — bootstrapping labels
from the rule-based CommentClassifier itself (distant supervision) instead
of steps 2-6's ABSTAIN-tally/Pool-extraction/hand-labeling/vocab-build
chain. `make gru-train` then runs step 7 against sample_default.txt by
default. This auto-labeled path can only produce NO for the narrow
decorative-only case (a comment with no letter or digit at all — see
CommentClassifier's DecorativeSeparatorGate); the rule-based classifier it
bootstraps from still cannot emit NO for genuine prose/code cases
(commented-out code, license blocks, etc. — see STATE_AI.md's "why the GRU
only ever returns YES/ABSTAIN" note for what's still unhandled) — it
exists to give the shipped weights file *some* real, license-clean,
non-empty default training data, not to replace the hand-labeled Pool A/B
path (steps 2-6), which remains the only source of real prose/code NO
ground truth.

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
      1. Extract an acquire_corpus.sh archive back under /tmp if needed (from
         wherever you archived it, per RDD_EXT_19's personal-directory
         guidance):
             tar -xJf <path-to-your-archived>/gru_corpus.tar.xz -C /tmp
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
    file: Xavier/Glorot init, per-example forward+backward, mini-batch
    gradient averaging + one Adam step per batch, 20% held-out validation
    split with patience-based early stopping. Loads tools/gru/
    explicit_vocab.txt by default (--vocab= to override; empty/missing path
    falls back to deriving vocab from the training file itself, useful only
    for quick smoke tests). Wired as a Makefile target:

        make gru-train GRU_TRAIN_INPUT=<labeled-file> GRU_WEIGHTS_OUT=<weights-file> \
            [GRU_TRAIN_ARGS="--epochs=40 --patience=6 --vocab=<path> --seed=<n> --threads=<n> --batch-size=<n>"]

    or invoked directly once compiled:

        java -cp target/classes:<gru-tools-classes> GruTrainer <labeled-file> <weights-file> \
            [--epochs=N] [--patience=N] [--vocab=<path>] [--seed=N] [--threads=N] [--batch-size=N] \
            [--warmup-steps=N] [--lr-min=N]

    --batch-size=N (default 16, must be >= 1) is the mini-batch size:
    gradients from N examples are averaged (never summed -- averaging keeps
    the effective step size comparable across different batch sizes, the
    standard mini-batch SGD/Adam definition) before one Adam optimizer step
    is applied. The Adam step counter (used for bias-correction) increments
    once per batch, not once per example, so mini-batching changes the
    bias-correction schedule versus the old per-example-step behavior --
    expected/correct for mini-batch Adam, not a bug. The last, possibly-
    partial batch of an epoch (trainExamples % batchSize != 0) still
    averages correctly over however many examples it actually contains, not
    the full batchSize. --batch-size=1 degenerates to (numerically close
    to, not bit-identical to, since gradient clipping is still applied per
    example before averaging) the old per-example-Adam-step behavior. A
    resumable hyperparameter -- see the checkpointing section below.

    --threads=N (default 1, i.e. plain sequential per-batch compute)
    parallelizes the expensive forward/backward compute for one mini-
    batch's N examples across N worker threads, all against the same pre-
    batch weights snapshot (safe -- forward/backward only read weights,
    never mutate it) -- an axis that composes orthogonally with
    --batch-size, not a competing one: --batch-size decides how many
    examples' gradients get averaged per Adam step, --threads decides how
    many of that batch's forward/backward computations run in parallel.
    Validation-loss computation is also parallelized under --threads=N (in
    its own, separate --threads-sized chunks, unrelated to --batch-size),
    but carries no staleness tradeoff at all: weights are frozen during
    validation, so results there are identical regardless of thread count.
    --threads is left opt-in at 1 rather than defaulting to all cores, so a
    real training run doesn't unexpectedly saturate the machine — pick a
    value that leaves some cores free if you want to keep using the machine
    for other things while training runs.

    --warmup-steps=N (default 0, must be >= 0) and --lr-min=N (default 0.0,
    must be >= 0) opt into a learning-rate warmup + cosine decay schedule --
    OFF BY DEFAULT, backward compatible: --warmup-steps=0 (the default)
    disables the schedule entirely and --lr is used flat throughout
    training, byte-for-byte the same behavior as before this feature
    existed. Passing --warmup-steps=N > 0 enables both phases together (one
    flag gates the whole schedule, --lr-min is only meaningful once
    warmup is on):

      - Warmup: LR ramps linearly from 0 up to --lr over the first N Adam
        steps (step-granular, not epoch-granular -- training already takes
        one Adam step per mini-batch since --batch-size landed, so warming
        up per-step is the smoother, more correct match for that
        granularity than per-epoch would be).
      - Cosine decay: once warmup completes, LR follows a cosine curve
        from --lr down to --lr-min over the remaining steps up to the
        decay horizon, defined as stepsPerEpoch * --epochs (reusing
        --epochs as the schedule's duration rather than inventing a third,
        separate duration concept alongside epochs/patience). Early
        stopping via --patience simply cuts the schedule off early if it
        fires before the horizon is reached -- same as it already does to
        --epochs itself; not a bug.

    Like --batch-size, both --warmup-steps and --lr-min are resumable
    hyperparameters (override-if-CLI-specified-else-checkpoint-value) --
    see the checkpointing section below. Since the decay horizon is
    derived from --epochs/--batch-size/trainExamples rather than stored
    separately, overriding --epochs on a --resume changes the decay
    horizon accordingly, the same way it already changes when maxEpochs
    itself is raised to extend a run.

    The current epoch's last-applied LR is reported on the per-epoch
    summary line (see below) so the schedule's actual shape is directly
    observable during a run, not just trusted from the formula.

    Checkpointing / --resume=<path> (2026-07-31, batch-size persistence
    added 2026-08-01, warmup-steps/lr-min persistence added 2026-08-01):
    every epoch, GruTrainer writes a binary checkpoint
    next to <weights-file> -- <weights-file>.ckpt-current.bin (full
    resumable state: weights, Adam moment arrays, vocab, epoch/patience
    counters, hyperparameters incl. --batch-size/--warmup-steps/--lr-min,
    seed) and, only when
    validation loss improves that epoch,
    <weights-file>.ckpt-best.bin (weights + vocab only). These are binary
    (DataOutputStream, not JSON) purely for per-epoch write speed on a
    multi-MB weights blob, and are pure recovery/resume artifacts, not a
    persistent output: on a normal, uninterrupted run that finishes and
    writes the final JSON weights file, BOTH checkpoint files are deleted
    automatically at the very end. They are also gitignored -- never
    commit them.

    This means an interrupted run (killed, crashed, machine rebooted) is
    the ONLY situation where a checkpoint file survives to be resumed
    from. A short, successfully-completing run -- e.g. the Makefile's own
    default GRU_TRAIN_ARGS ("--threads=3 --epochs=3 --patience=2
    --progress-every=1000"), which finishes in minutes on any real corpus
    -- will never leave a checkpoint behind for you to resume from, by
    design: there is nothing to recover from once the run has already
    finished and written its real output. If you want to actually
    exercise --resume, either let a long real run get interrupted
    (Ctrl-C / kill / crash / reboot) partway through, or deliberately
    kill a run yourself mid-epoch to leave <weights-file>.ckpt-current.bin
    on disk.

    `make gru-train` auto-detects an interrupted run: before invoking
    GruTrainer, it checks for `<weights-file>.ckpt-current.bin` (i.e.
    `$(GRU_WEIGHTS_OUT).ckpt-current.bin`) and, if present, automatically
    adds `--resume=<weights-file>.ckpt-current.bin` to the invocation
    (printing "gru-train: found ..., resuming" first) — no flag needed on
    your part. Values in `GRU_TRAIN_ARGS` (e.g. a raised `--epochs=`/
    `--patience=` to extend the run) still take priority over the
    checkpoint's stored values, same override rule as a manual --resume
    below. If no checkpoint file exists, `make gru-train` runs a normal
    fresh training pass exactly as before.

    To resume an interrupted run manually (e.g. outside the Makefile, or to
    resume a checkpoint at a path other than the Makefile's own
    GRU_WEIGHTS_OUT):

        java -cp target/classes:<gru-tools-classes> GruTrainer <labeled-file> <weights-file> \
            --resume=<weights-file>.ckpt-current.bin [other flags...]

    Resume restores the weights, Adam moment arrays (including the Adam
    step counter, so bias-correction continues seamlessly rather than
    restarting), vocabulary, and hyperparameters from the checkpoint, and
    opportunistically loads the sibling .ckpt-best.bin to recover the true
    best-so-far weights (falls back to the current checkpoint's own
    weights with a warning if the best-checkpoint is missing). The RNG is
    re-seeded from the stored seed, which reproduces the identical initial
    shuffle and train/validation split -- but per-epoch shuffle order
    beyond that point diverges from what an uninterrupted run would have
    done, since java.util.Random's live internal state isn't itself
    serialized, only the seed. This is a documented, accepted limitation,
    not a bug: a resumed run's exact per-step trajectory won't bit-match
    an uninterrupted one, but the training/validation split, weights, and
    optimizer state are all faithfully restored.

    If the final weights write to <weights-file> fails (bad path, full
    disk, permissions), the trainer does not discard the trained weights:
    it falls back to writing them to a timestamped file under the system
    temp directory (java.io.tmpdir) and prints that path loudly to stderr,
    then still exits non-zero so the failure is visible to scripts/CI. The
    fallback file is not cleaned up automatically — move it somewhere
    permanent yourself.

    After a successful write, it also reloads the just-written weights and
    reports a binary confusion matrix (positive class = YES) against the
    held-out validation split, plus precision/recall/F1:

        GruTrainer: validation confusion matrix (positive=YES) tp=.. fp=.. tn=.. fn=.. precision=.. recall=.. f1=..

    --check-gradients=N (absent by default, does not train when present):
    diagnostic-only mode. Picks one random labeled example, runs
    forward+backward once, then for N random entries in each of a
    representative sample of weight arrays (dense layer, output layer, one
    recurrent direction's Wz, and the embedding rows the example actually
    touches) compares GruClassifier.backward()'s analytic gradient against
    a numeric finite-difference estimate, printing each comparison and a
    final maxRelativeError/PASS-FAIL summary (exit code reflects pass/fail).
    Useful for building confidence in backward() before relying on it for
    further changes; adds no runtime cost to normal training since it never
    runs unless explicitly requested. Example:

        java -cp target/classes:<gru-tools-classes> GruTrainer <labeled-file> <weights-file> --check-gradients=20

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

    Per source, right after extract_comments.py and before Pool A/B
    extraction: runs redact_secrets.py (below) over the extracted comments
    file in place, then dedupes exact-duplicate lines in place
    (`awk '!seen[$0]++'` — real-world corpora accumulate many exact-duplicate
    lines, mostly repeated license-header/boilerplate text recurring across
    files of the same source repo; this used to run once at the very end
    against the final combined/auto-labeled sample_default.txt in the
    Makefile — moved here, per-source, so it also applies to Pool A/B
    candidate extraction).

        tools/gru/acquire_corpus.sh [--out-dir DIR] [--keep-clones] [--only name1,name2,...]

    Extend the SOURCES list by hand as new repos get vetted (license check
    first) — don't add unvetted sources.

redact_secrets.py
    Scrubs likely API keys/tokens from an extract_comments.py-format corpus
    file's comment-text column, in place, before it lands in any corpus file
    or gets combined/labeled downstream — so a scraped repo's leaked secret
    never reaches sample_default.txt or anything committed. Covers named
    provider formats (Google, AWS access-key-id, GitHub, Stripe,
    OpenAI/Anthropic, Slack) plus a narrow generic fallback for
    key/secret/token/password/access_key/auth-shaped assignments whose value
    looks high-entropy (mixed case+digit, Shannon entropy >= 3.5) — deliberately
    conservative to avoid mass-redacting ordinary hashes/identifiers that
    happen to appear in comments. Matches are replaced with [REDACTED].
    Called automatically by acquire_corpus.sh; can also be run standalone:

        python3 tools/gru/redact_secrets.py <file> [<file> ...]

GenerateSampleDefault.java
    Auto-labels a comments file (from extract_comments.py / acquire_corpus.sh)
    by running each comment through the real rule-based CommentFeatureExtractor/
    CommentClassifier pipeline (distant supervision): YES verdicts are kept
    as RDD_EXT_20/21-schema rows (always targetWordIndex=0), ABSTAIN verdicts
    are skipped, and a provenance header is written. CommentClassifier can
    never emit NO (RDD_KEY_96), so this always produces an all-YES/skip
    corpus — real NO ground truth still requires hand-labeling Pool A/B.
    Wired into the Makefile rather than run directly:

        make gru-acquire-corpus

    which acquires + extracts Pool A/B (as before, already redacted/deduped
    per-source by acquire_corpus.sh) and additionally runs this auto-labeling
    step into tools/gru/sample_default.txt.

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
