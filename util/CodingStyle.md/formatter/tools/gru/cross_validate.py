#!/usr/bin/env python3
"""Bounds the variance on a single held-out-split precision estimate (STATE_AI.md open item
"no cross-validation or repeated splits have been run") via repeated Monte Carlo cross-validation:
reshuffles the combined RDD_EXT_21-schema labeled-examples file with a fresh seed each round,
splits 80/20, retrains GruTrainer from scratch on the 80% (which itself carves its own 20%
validation split out for early stopping, unrelated to this script's held-out 20%), evaluates
precision on the untouched 20% via GruEval, and reports the spread across rounds instead of one
number.

This only orchestrates existing pieces (GruTrainer, GruEval) -- it doesn't reimplement any
training/eval math itself, same reasoning as add_target_index.py/build_vocab.py's own tokenize()
mirrors: staying a thin driver around the real Java code keeps this script from silently drifting
from what the real classifier does.

Per RDD_EXT_19, this script's own working files (per-round train/test splits, trained weights)
are written under --work-dir (default /tmp/gru_cv) and never committed -- only the script itself,
which is ordinary tooling like every other checked-in tools/gru/*.py file.

Usage:
    python3 cross_validate.py <combined-rdd-ext-21-labeled-file> [--rounds 5] [--work-dir /tmp/gru_cv]
                               [--epochs 40] [--patience 6] [--vocab tools/gru/explicit_vocab.txt]
"""

import argparse
import random
import re
import statistics
import subprocess
import sys
from pathlib import Path

PRECISION_RE = re.compile(r"precision=([0-9.]+)")


def split(lines, seed, train_fraction=0.8):
    shuffled = list(lines)
    random.Random(seed).shuffle(shuffled)
    cut = int(len(shuffled) * train_fraction)
    return shuffled[:cut], shuffled[cut:]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("examples", help="combined RDD_EXT_21-schema labeled-examples file")
    parser.add_argument("--rounds", type=int, default=5)
    parser.add_argument("--work-dir", default="/tmp/gru_cv")
    parser.add_argument("--epochs", default="40")
    parser.add_argument("--patience", default="6")
    parser.add_argument("--vocab", default=None, help="passed through to GruTrainer's --vocab=, omit to use its default")
    args = parser.parse_args()

    formatter_dir = Path(__file__).resolve().parent.parent.parent
    work_dir = Path(args.work_dir)
    work_dir.mkdir(parents=True, exist_ok=True)
    classes_dir = work_dir / "classes"
    classes_dir.mkdir(exist_ok=True)

    with open(args.examples, "r", encoding="utf-8") as f:
        lines = [line.rstrip("\n") for line in f if line.strip()]
    if len(lines) < 10:
        print(f"cross_validate: only {len(lines)} example(s), too few for a meaningful split", file=sys.stderr)
        sys.exit(2)

    print("cross_validate: building jar + compiling GruTrainer/GruEval once", file=sys.stderr)
    subprocess.run(["make", "--no-print-directory", "build"], cwd=formatter_dir, check=True,
                    stdout=subprocess.DEVNULL)
    class_dir = formatter_dir / "target" / "classes"
    for tool in ("GruTrainer.java", "GruEval.java"):
        subprocess.run(
            ["javac", "-encoding", "UTF-8", "-source", "8", "-target", "8",
             "-cp", str(class_dir), "-d", str(classes_dir), str(formatter_dir / "tools" / "gru" / tool)],
            check=True,
        )
    classpath = f"{class_dir}:{classes_dir}"

    precisions = []
    for round_index in range(args.rounds):
        seed = 1000 + round_index
        train_lines, test_lines = split(lines, seed)
        train_path = work_dir / f"train_round{round_index}.txt"
        test_path = work_dir / f"test_round{round_index}.txt"
        weights_path = work_dir / f"weights_round{round_index}.json"
        train_path.write_text("\n".join(train_lines) + "\n", encoding="utf-8")
        test_path.write_text("\n".join(test_lines) + "\n", encoding="utf-8")

        train_cmd = ["java", "-cp", classpath, "GruTrainer", str(train_path), str(weights_path),
                     f"--epochs={args.epochs}", f"--patience={args.patience}", f"--seed={seed}"]
        if args.vocab is not None:
            train_cmd.append(f"--vocab={args.vocab}")
        subprocess.run(train_cmd, cwd=formatter_dir, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

        eval_result = subprocess.run(
            ["java", "-cp", classpath, "GruEval", str(weights_path), str(test_path)],
            cwd=formatter_dir, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True,
        )
        match = PRECISION_RE.search(eval_result.stdout)
        if not match:
            print(f"cross_validate: round {round_index}: could not parse precision from: "
                  f"{eval_result.stdout.strip()}", file=sys.stderr)
            sys.exit(1)
        precision = float(match.group(1))
        precisions.append(precision)
        print(f"round {round_index} (seed={seed}, train={len(train_lines)}, test={len(test_lines)}): "
              f"{eval_result.stdout.strip()}")

    mean = statistics.mean(precisions)
    stdev = statistics.stdev(precisions) if len(precisions) > 1 else 0.0
    print(f"\ncross_validate: {args.rounds} round(s) -- precision mean={mean:.4f} stdev={stdev:.4f} "
          f"min={min(precisions):.4f} max={max(precisions):.4f}")


if __name__ == "__main__":
    main()
