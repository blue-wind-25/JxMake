#!/usr/bin/env python3
"""
reformat_chunks.py — Reformat a long C/C++/Java file by splitting at section
dividers so no single AI call exceeds the recommended 500-line limit.

Split strategy
--------------
The file is split at every `////...` divider block (single or triple).  The
divider block is placed at the END of the preceding chunk.  This means:
  - No divider is ever split across two chunks.
  - Each chunk the AI receives is syntactically self-contained.
  - Reassembly is a plain concatenation — no divider is duplicated or lost.

If a resulting chunk is still above TARGET_LINES (no internal dividers), it is
further split at function-boundary blank lines (a `}` at column 0 followed by
a blank line).  That heuristic is imperfect — review those chunks carefully.

Usage
-----
    export ANTHROPIC_API_KEY="sk-ant-..."
    python3 reformat_chunks.py <source_file> <lang: c|cpp|java> [--model <id>]

Output
------
    <source_file>.reformatted   — reassembled result
    Review with:  diff <source_file> <source_file>.reformatted
"""

import sys, re, pathlib, textwrap
import anthropic

# ---- configuration ----------------------------------------------------------

STYLE_DIR   = pathlib.Path(__file__).parent
TARGET_LINES = 500          # chunks larger than this trigger the fallback split
DEFAULT_MODEL = "claude-sonnet-4-6"

DIVIDER_RE = re.compile(r'^/{60,}\s*$')  # 60+ forward-slashes = a divider line

# ---- style rules loader -----------------------------------------------------

def load_rules(lang: str) -> str:
    preamble = STYLE_DIR / "AI_PREAMBLE.md"
    common   = STYLE_DIR / "STYLE.md"
    if lang in ("c", "cpp"):
        extra = STYLE_DIR / "STYLE_C_CPP.md"
    elif lang == "java":
        extra = STYLE_DIR / "STYLE_JAVA.md"
    else:
        raise ValueError(f"Unknown language '{lang}'. Use: c, cpp, java")
    return "\n\n".join(p.read_text() for p in [preamble, common, extra])

# ---- splitting --------------------------------------------------------------

def _collect_divider_block(lines: list[str], start: int) -> tuple[list[str], int]:
    """Return (divider_lines, next_index) for the contiguous block at `start`."""
    block = []
    i = start
    while i < len(lines) and DIVIDER_RE.match(lines[i]):
        block.append(lines[i])
        i += 1
    return block, i

def split_at_dividers(lines: list[str]) -> list[list[str]]:
    """
    Primary split: cut at every divider block.
    The divider block is appended to the END of the preceding chunk.
    """
    chunks: list[list[str]] = []
    current: list[str] = []
    i = 0
    while i < len(lines):
        if DIVIDER_RE.match(lines[i]):
            block, i = _collect_divider_block(lines, i)
            current.extend(block)
            chunks.append(current)
            current = []
        else:
            current.append(lines[i])
            i += 1
    if current:
        chunks.append(current)
    return chunks

def split_at_function_boundaries(lines: list[str], target: int) -> list[list[str]]:
    """
    Fallback split for chunks that are still over `target` lines.
    Cuts at blank lines that immediately follow a `}` at column 0.
    """
    if len(lines) <= target:
        return [lines]

    # Collect candidate split positions: blank line after a `}` at col 0
    candidates = []
    for i in range(1, len(lines)):
        if lines[i].strip() == '' and lines[i-1].rstrip() == '}':
            candidates.append(i + 1)  # start of next chunk = line after blank

    if not candidates:
        return [lines]  # can't safely split further

    # Pick split points that keep chunks near TARGET_LINES
    chunks = []
    start = 0
    next_target = target
    for pos in candidates:
        if pos >= next_target:
            chunks.append(lines[start:pos])
            start = pos
            next_target = pos + target
    if start < len(lines):
        chunks.append(lines[start:])
    return [c for c in chunks if c]  # drop empty

def make_chunks(lines: list[str]) -> list[list[str]]:
    """Split by dividers, then apply fallback for oversized chunks."""
    primary = split_at_dividers(lines)
    result = []
    for chunk in primary:
        result.extend(split_at_function_boundaries(chunk, TARGET_LINES))
    return result

# ---- AI reformatting --------------------------------------------------------

def reformat_chunk(
    chunk_lines: list[str],
    rules: str,
    filename: str,
    chunk_num: int,
    total: int,
    model: str,
) -> str:
    chunk_text = "".join(chunk_lines)
    client = anthropic.Anthropic()
    msg = client.messages.create(
        model=model,
        max_tokens=8192,
        messages=[{
            "role": "user",
            "content": (
                f"{rules}\n\n"
                f"=== SOURCE FILE: {filename} (chunk {chunk_num}/{total}) ===\n"
                f"{chunk_text}\n"
                f"=== END SOURCE ==="
            ),
        }],
    )
    return msg.content[0].text

# ---- reassembly -------------------------------------------------------------

def reassemble(parts: list[str]) -> str:
    """Join chunks; ensure exactly one newline between them."""
    out = ""
    for part in parts:
        if out and not out.endswith("\n"):
            out += "\n"
        out += part
    return out

# ---- main -------------------------------------------------------------------

def parse_args():
    import argparse
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("source_file")
    ap.add_argument("lang", choices=["c", "cpp", "java"])
    ap.add_argument("--model", default=DEFAULT_MODEL)
    return ap.parse_args()

def main():
    args = parse_args()
    src  = pathlib.Path(args.source_file)
    if not src.exists():
        sys.exit(f"File not found: {src}")

    rules  = load_rules(args.lang)
    lines  = src.read_text().splitlines(keepends=True)
    chunks = make_chunks(lines)
    total  = len(chunks)

    print(f"{src.name}: {len(lines)} lines → {total} chunk(s) (model: {args.model})")
    for i, chunk in enumerate(chunks, 1):
        flag = "  [fallback split — review carefully]" if (
            not DIVIDER_RE.match(chunk[0]) and i > 1
        ) else ""
        print(f"  Chunk {i}/{total}: {len(chunk)} lines{flag}")

    reformatted = []
    for i, chunk in enumerate(chunks, 1):
        print(f"  Reformatting chunk {i}/{total}...", end=" ", flush=True)
        result = reformat_chunk(chunk, rules, src.name, i, total, args.model)
        reformatted.append(result)
        print("done")

    out = src.with_suffix(src.suffix + ".reformatted")
    out.write_text(reassemble(reformatted))
    print(f"\nWritten: {out}")
    print(f"Review:  diff {src} {out}")

if __name__ == "__main__":
    main()
