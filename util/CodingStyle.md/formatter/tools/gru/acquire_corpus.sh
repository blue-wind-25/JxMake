#!/usr/bin/env bash
# Automates Step 3 GRU training-data ACQUISITION + EXTRACTION only (STATE_AI.md
# open item "a larger real corpus"), per RDD_EXT_16's source policy (own
# dogfooded repos first, then a vetted MIT/BSD-3-Clause/Apache-2.0 public-repo
# list). Deliberately stops at "Pool A/Pool B candidates ready for hand
# labeling" -- assigning the YES/NO ground-truth label to each candidate is a
# human judgment call (RDD_EXT_20's labeling methodology), not something this
# script attempts, so its output is an intermediate, not a finished corpus.
#
# For each configured source (local dogfood path, or a public repo shallow-
# cloned to a scratch dir), runs the same three steps a human would run by
# hand: extract_comments.py over the source tree, then `make
# gru-extract-pool-a`/`gru-extract-pool-b` against that extraction. Public
# repos are cloned with --depth 1 and removed again after extraction (only the
# extracted text is needed downstream) unless --keep-clones is passed.
#
# Per RDD_EXT_19, none of this script's *output* is ever committed -- it
# writes only under --out-dir (default /tmp/gru_corpus), same posture as every
# other real-corpus-touching tool in this directory. The script itself is
# ordinary tooling (like extract_comments.py/add_target_index.py), safe to
# check in.
#
# Usage:
#   tools/gru/acquire_corpus.sh [--out-dir DIR] [--keep-clones] [--only NAME,...]
#
# Run from the formatter/ directory (same convention as `make`).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FORMATTER_DIR="$(cd "$SCRIPT_DIR/.." && cd .. && pwd)"

OUT_DIR="/tmp/gru_corpus"
KEEP_CLONES=0
ONLY=""

while [ $# -gt 0 ]; do
    case "$1" in
        --out-dir) OUT_DIR="$2"; shift 2 ;;
        --keep-clones) KEEP_CLONES=1; shift ;;
        --only) ONLY="$2"; shift 2 ;;
        *) echo "acquire_corpus.sh: unknown argument: $1" >&2; exit 2 ;;
    esac
done

mkdir -p "$OUT_DIR"
CLONE_DIR="$OUT_DIR/clones"
mkdir -p "$CLONE_DIR"

# name|kind|location -- kind is "local" (already-owned dogfood path, ~ expanded)
# or "github" (org/repo, shallow-cloned). Sourced from STATE_AI.md's own
# run-by-run log (RDD_EXT_16's own-repos-first batch, plus item 9's vetted
# MIT/BSD-3-Clause/Apache-2.0 public-repo list) -- add new entries here as
# more sources get vetted, don't invent new ones without checking a license.
SOURCES=(
    "eCxx|local|~/Projects/eCxx"
    "SusterCaller|local|~/Projects/SusterCaller"
    "VMA-GIT|local|~/Projects/VMA-GIT"
    "TTGO_VGA32_Lite|local|~/Projects/TTGO_VGA32_Lite"
    "RobotCoding|local|~/Projects/RobotCoding"
    "microsoft-proxy|github|microsoft/proxy"
    "NVIDIA-stdexec|github|NVIDIA/stdexec"
    "arrow-kt-arrow|github|arrow-kt/arrow"
    "expressjs-express|github|expressjs/express"
    "zurb-foundation-sites|github|zurb/foundation-sites"
    "json5-json5|github|json5/json5"
    "pallets-flask|github|pallets/flask"
    "stephenberry-glaze|github|stephenberry/glaze"
    "google-google-java-format|github|google/google-java-format"
    "apache-ant|github|apache/ant"
    "actions-starter-workflows|github|actions/starter-workflows"
)

echo "acquire_corpus: building jar once up front"
(cd "$FORMATTER_DIR" && make --no-print-directory build >/dev/null)

printf "%-28s %10s %10s %10s\n" "source" "comments" "pool_a" "pool_b"

for entry in "${SOURCES[@]}"; do
    name="${entry%%|*}"
    rest="${entry#*|}"
    kind="${rest%%|*}"
    location="${rest#*|}"

    if [ -n "$ONLY" ]; then
        case ",$ONLY," in
            *",$name,"*) ;;
            *) continue ;;
        esac
    fi

    if [ "$kind" = "local" ]; then
        src_dir="${location/#\~/$HOME}"
        if [ ! -d "$src_dir" ]; then
            echo "acquire_corpus: skipping $name -- local path not found: $src_dir" >&2
            continue
        fi
    else
        src_dir="$CLONE_DIR/$name"
        if [ ! -d "$src_dir" ]; then
            if ! git clone --depth 1 "https://github.com/$location.git" "$src_dir" >/dev/null 2>&1; then
                echo "acquire_corpus: skipping $name -- clone of $location failed" >&2
                continue
            fi
        fi
    fi

    comments_out="$OUT_DIR/comments_${name}.txt"
    pool_a_out="$OUT_DIR/pool_a_${name}.txt"
    pool_b_out="$OUT_DIR/pool_b_${name}.txt"

    python3 "$SCRIPT_DIR/extract_comments.py" "$src_dir" --out "$comments_out" >/dev/null

    (cd "$FORMATTER_DIR" && make --no-print-directory gru-extract-pool-a \
        GRU_ABSTAIN_INPUT="$comments_out" GRU_POOL_A_OUT="$pool_a_out" >/dev/null)
    (cd "$FORMATTER_DIR" && make --no-print-directory gru-extract-pool-b \
        GRU_ABSTAIN_INPUT="$comments_out" GRU_POOL_B_OUT="$pool_b_out" >/dev/null)

    comment_count=$(wc -l < "$comments_out" | tr -d ' ')
    pool_a_count=$(wc -l < "$pool_a_out" | tr -d ' ')
    pool_b_count=$(wc -l < "$pool_b_out" | tr -d ' ')
    printf "%-28s %10s %10s %10s\n" "$name" "$comment_count" "$pool_a_count" "$pool_b_count"

    if [ "$kind" = "github" ] && [ "$KEEP_CLONES" -eq 0 ]; then
        rm -rf "$src_dir"
    fi
done

echo
echo "acquire_corpus: done. Pool A/Pool B candidate files are under $OUT_DIR/ --"
echo "next step is hand labeling (RDD_EXT_20 schema), same as the first batch;"
echo "nothing here has been labeled or committed."
