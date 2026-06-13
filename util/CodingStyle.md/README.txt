Using AI to Apply the Coding Style Guide
=========================================

Files in this directory
-----------------------
  STYLE.md        Common rules for all languages (read this first)
  STYLE_C_CPP.md  C and C++ extensions/overrides
  STYLE_JAVA.md   Java extensions/overrides
  README.txt      This file


Model Selection
---------------
Do NOT use small/fast models (Claude Haiku, Gemini Flash, GPT-4o-mini, etc.).

These rules include several tasks that small models fail at inconsistently:
  - Complexity-based bracket/parenthesis padding (requires reasoning about
    expression nesting and function-call depth)
  - Column alignment across declaration groups (requires exact character counting)
  - Conditional closing-comment rules (line-count threshold + nesting depth)

Recommended models (minimum):
  Claude Sonnet 4.6  (claude-sonnet-4-6)  — good balance of quality and speed
  Claude Opus 4.8    (claude-opus-4-8)    — best for large or complex files

Non-Anthropic equivalents at the same tier:
  Gemini 1.5 Pro / 2.0 Pro, GPT-4o (not 4o-mini)

Context note: each `claude -p` call in a shell loop is a completely independent
process — context does NOT accumulate between iterations. Each invocation sees
only the style rules and the current file. The batch script below already handles
this correctly. "One file at a time" only matters if you try to feed multiple
files in a single prompt (e.g. `cat *.c | claude -p "..."`), which you should
avoid.


Preparing the System Prompt
----------------------------
The style rules must be included in the prompt. Build a combined prompt by
concatenating the relevant style files for the target language:

  For C files:
    cat STYLE.md STYLE_C_CPP.md > /tmp/style_c.txt

  For C++ files:
    cat STYLE.md STYLE_C_CPP.md > /tmp/style_cpp.txt

  For Java files:
    cat STYLE.md STYLE_JAVA.md > /tmp/style_java.txt

Store the combined file once and reuse it across multiple reformatting calls.


CLI Mode  (using the `claude` command-line tool)
------------------------------------------------
Non-interactive, single-file reformatting:

  claude -p --model claude-sonnet-4-6 "
  You are a code formatter. Apply the coding style rules below EXACTLY.
  Output ONLY the reformatted source code — no explanations, no markdown
  fences, no commentary.

  === STYLE RULES ===
  $(cat /path/to/STYLE.md)
  $(cat /path/to/STYLE_C_CPP.md)
  === END RULES ===

  === SOURCE FILE ===
  $(cat /path/to/file.c)
  === END SOURCE ===
  " > /path/to/file.c.reformatted

Review the diff before accepting:

  diff /path/to/file.c /path/to/file.c.reformatted

Batch reformatting a directory (shell script):

  #!/usr/bin/env bash
  set -euo pipefail

  STYLE_RULES=$(cat /path/to/STYLE.md /path/to/STYLE_C_CPP.md)
  SRC_DIR="$1"          # directory to reformat
  OUT_DIR="${2:-$SRC_DIR/reformatted}"
  mkdir -p "$OUT_DIR"

  for f in "$SRC_DIR"/*.{c,h}; do
      [ -f "$f" ] || continue
      base=$(basename "$f")
      echo "Reformatting $base..."
      claude -p --model claude-sonnet-4-6 "
  You are a code formatter. Apply the coding style rules below EXACTLY.
  Output ONLY the reformatted source code — no explanations, no markdown
  fences, no commentary.

  === STYLE RULES ===
  $STYLE_RULES
  === END RULES ===

  === SOURCE FILE: $base ===
  $(cat "$f")
  === END SOURCE ===
  " > "$OUT_DIR/$base"
      echo "  -> $OUT_DIR/$base"
  done

  echo "Done. Review diffs with:"
  echo "  diff -r $SRC_DIR $OUT_DIR --exclude='*.reformatted'"

Save as reformat.sh, make executable (chmod +x reformat.sh), then run:
  ./reformat.sh src/mymodule/


API / Bot Mode  (Anthropic Python SDK)
---------------------------------------
Install the SDK:
  pip install anthropic

Script (reformat_file.py):

  #!/usr/bin/env python3
  import sys, pathlib, anthropic

  def load_rules(*paths):
      return "\n\n".join(pathlib.Path(p).read_text() for p in paths)

  def reformat(source_path, rules_text, model="claude-sonnet-4-6"):
      source = pathlib.Path(source_path).read_text()
      client = anthropic.Anthropic()   # reads ANTHROPIC_API_KEY from env
      msg = client.messages.create(
          model=model,
          max_tokens=8192,
          messages=[{
              "role": "user",
              "content": (
                  "You are a code formatter. Apply the coding style rules below "
                  "EXACTLY. Output ONLY the reformatted source code — no "
                  "explanations, no markdown fences, no commentary.\n\n"
                  f"=== STYLE RULES ===\n{rules_text}\n=== END RULES ===\n\n"
                  f"=== SOURCE FILE: {source_path} ===\n{source}\n=== END SOURCE ==="
              )
          }]
      )
      return msg.content[0].text

  if __name__ == "__main__":
      if len(sys.argv) < 3:
          print(f"Usage: {sys.argv[0]} <source_file> <lang: c|cpp|java>")
          sys.exit(1)

      src, lang = sys.argv[1], sys.argv[2]
      style_dir = pathlib.Path(__file__).parent

      if lang in ("c", "cpp"):
          rules = load_rules(style_dir/"STYLE.md", style_dir/"STYLE_C_CPP.md")
      elif lang == "java":
          rules = load_rules(style_dir/"STYLE.md", style_dir/"STYLE_JAVA.md")
      else:
          print(f"Unknown language: {lang}"); sys.exit(1)

      result = reformat(src, rules)
      out = pathlib.Path(src).with_suffix(pathlib.Path(src).suffix + ".reformatted")
      out.write_text(result)
      print(f"Written: {out}")
      print(f"Review:  diff {src} {out}")

Usage:
  export ANTHROPIC_API_KEY="sk-ant-..."
  python3 reformat_file.py src/Utils.c c
  python3 reformat_file.py src/MyClass.java java


Tips and Limitations
---------------------
1. Review every diff manually. The model makes mistakes, especially on:
   - Large declaration groups requiring precise column alignment (STYLE.md §5, §6)
   - Getter/setter aligned groups (STYLE.md §14)
   - Complex bracket-padding decisions near the boundary of the rules (§3.1)

2. Process files that are already mostly correct. AI reformatting works best
   as a consistency pass, not a from-scratch transformation.

3. For very long files (> 500 lines), consider splitting into logical sections
   (one struct, one class, one group of related functions) and reformatting
   each section separately.

4. The model may silently drop or alter comments. Always diff against the
   original to catch unintended content changes, not just formatting changes.

5. Alignment in getter/setter groups (STYLE.md §14) is the hardest rule for
   models to apply correctly. Manually verify those sections.
