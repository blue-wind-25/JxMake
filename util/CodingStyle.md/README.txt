Using AI to Apply the Coding Style Guide
=========================================

Files in this directory
-----------------------
  STYLE.md                  Common rules for all languages (read this first)
  STYLE_C_CPP.md            C and C++ extensions/overrides (baseline)
  STYLE_CPP20.md            C++ extensions/overrides (newer constructs, read after STYLE_C_CPP.md)
  STYLE_CPP26.md            C++26 extensions/overrides (read after STYLE_CPP20.md)
  STYLE_JAVA.md             Java extensions/overrides (baseline)
  STYLE_JAVA17.md           Java extensions/overrides (newer constructs, read after STYLE_JAVA.md)
  STYLE_KOTLIN.md           Kotlin extensions/overrides (baseline)
  STYLE_KOTLIN2.md          Kotlin extensions/overrides (newer constructs, read after STYLE_KOTLIN.md)
  STYLE_JS_TS.md            JavaScript/TypeScript rules (derives from STYLE_JAVA.md/STYLE_KOTLIN.md)
  STYLE_DATA_FORMATS.md     JSON/JSON5, XML, CSS, HTML5 rules (borrows from STYLE.md)
  STYLE_PYTHON3.md          Python 3 rules
  AI_PREAMBLE_FULL.md       Preamble for full-file pass (un-JAR-processed files)
  AI_PREAMBLE_AESTHETIC.md  Preamble for layout judgment pass (post-JAR files)
  README.txt                This file

  NOTE — STYLE_CPP26.md, STYLE_JS_TS.md, STYLE_DATA_FORMATS.md, and
  STYLE_PYTHON3.md are drafted but not yet implemented in the deterministic
  JAR — the JAR still only handles C, C++, Java, and Kotlin (per
  STYLE_C_CPP.md/STYLE_CPP20.md, STYLE_JAVA.md/STYLE_JAVA17.md, and
  STYLE_KOTLIN.md/STYLE_KOTLIN2.md). The four newer files are usable today
  only via the full-file AI pass below, which is LLM-driven and doesn't
  depend on JAR support.

  The deterministic JAR formatter (formatter/code-formatter-1.00.jar, replace
  1.00 with your built version) handles all Tier-1 and Tier-2 rules mechanically
  for C, C++, Java, and Kotlin. Run it first for those languages. The AI workflows
  described here cover the remaining Tier-3 aesthetic decisions the JAR
  intentionally leaves untouched:
    - Function argument list layout (when the source is already multi-line and
      the author's grouping intent should be preserved or improved)
    - Getter/setter groups with non-standard naming conventions

  NOTE — Kotlin: the deterministic JAR's Kotlin support (STYLE_KOTLIN.md /
  STYLE_KOTLIN2.md, `.kt`/`.kts` auto-detected) is on the same footing as
  C/C++/Java — same dogfood-testing process, same regression-fix cadence (see
  formatter/STATE_KOTLIN.md and formatter/RDD_LOG.md). Both AI passes below
  apply to Kotlin the same way they apply to C/C++/Java. The one real
  exception: three specific property-accessor shapes — block-bodied accessors
  (`get() {...}`/`set(v) {...}`), a property pairing a getter with a setter,
  and a property with both an initializer and a custom accessor — are left
  preserved-as-written by the JAR rather than grouped/aligned. This is a
  deliberate scope boundary (these shapes are considerably more complex to
  group correctly than the plain one-liner cases the JAR already handles),
  not an immaturity gap — see formatter/STATE_KOTLIN.md's Open Questions.
  AI_PREAMBLE_AESTHETIC.md's Rule 2 covers aligning these three shapes
  manually when you hit one.

  There are two AI passes, described below. Use only one per file per run.


Two AI Passes
-------------

  FULL-FILE PASS  (AI_PREAMBLE_FULL.md)
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  Applies all style rules to a file that has NOT yet been processed by the JAR.
  Useful for one-off migration of legacy files. More expensive in time and quota
  since the entire file is reformatted, including rules the JAR handles better
  and more reliably. Review every diff carefully — capable models make mistakes
  on column alignment and bracket-padding, especially in large declaration groups.

  Recommended only when:
    - The file has never been touched by the JAR, and
    - You want a single-pass result without running the JAR separately.

  For ongoing work, prefer running the JAR first and using the layout judgment
  pass below — it is cheaper, faster, and less likely to disturb correct output.

  LAYOUT JUDGMENT PASS  (AI_PREAMBLE_AESTHETIC.md)
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  Applies only the two aesthetic decisions the JAR cannot make:
    1. Function argument list layout (calls and forward declarations)
    2. Non-standard getter/setter group detection and alignment

  Use this after the JAR has already formatted the file. The preamble instructs
  the AI to treat all JAR-applied alignment as opaque — it will not touch
  declaration groups, switch alignment, or signature line-breaking.

  This is the recommended pass for ongoing use. Feed the JAR output directly
  to the AI; review only the argument-list and getter/setter sections of the diff.


Model Selection
---------------
Do NOT use small/fast models (Claude Haiku, Gemini Flash, GPT-4o-mini, etc.).

These rules include tasks that small models fail at inconsistently:
  - Exact character counting for column alignment (full-file pass)
  - Complexity-based bracket/parenthesis padding (full-file pass)
  - Distinguishing meaningful argument grouping from arbitrary line breaks
    (layout judgment pass)

Recommended models (minimum):
  Claude Sonnet 4.6  (claude-sonnet-4-6)  — good balance of quality and speed
  Claude Opus 4.8    (claude-opus-4-8)    — best for large or complex files

Non-Anthropic equivalents at the same tier:
  Gemini 1.5 Pro / 2.0 Pro, GPT-4o (not 4o-mini)

Context note: each `claude -p` call in a shell loop is a completely independent
process — context does NOT accumulate between iterations. Each invocation sees
only the preamble, the style rules, and the current file. Process one file at a
time — do not pipe multiple files into a single prompt.


Preparing the Prompt
---------------------
Combine the relevant preamble with the style files for the target language:

  FULL-FILE PASS — C files:
    cat AI_PREAMBLE_FULL.md STYLE.md STYLE_C_CPP.md > /tmp/style_c_full.txt

  FULL-FILE PASS — C++ files:
    cat AI_PREAMBLE_FULL.md STYLE.md STYLE_C_CPP.md STYLE_CPP20.md > /tmp/style_cpp_full.txt

  FULL-FILE PASS — C++ files (including C++26 constructs):
    cat AI_PREAMBLE_FULL.md STYLE.md STYLE_C_CPP.md STYLE_CPP20.md STYLE_CPP26.md > /tmp/style_cpp26_full.txt

  FULL-FILE PASS — Java files:
    cat AI_PREAMBLE_FULL.md STYLE.md STYLE_JAVA.md STYLE_JAVA17 > /tmp/style_java_full.txt

  FULL-FILE PASS — Kotlin files:
    cat AI_PREAMBLE_FULL.md STYLE.md STYLE_KOTLIN.md STYLE_KOTLIN2.md > /tmp/style_kotlin_full.txt

  LAYOUT JUDGMENT PASS — any language:
    cat AI_PREAMBLE_AESTHETIC.md > /tmp/style_aesthetic.txt
    (no style files needed — the preamble is self-contained for this pass)

Store the combined file once and reuse it across multiple calls.


CLI Mode  (using the `claude` command-line tool)
------------------------------------------------
Non-interactive, single-file reformatting. The output is written to a staging
file for review; apply it in-place only after diffing.

  STYLE_DIR=/path/to/CodingStyle.md
  RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_C_CPP.md)
  SOURCE=$(cat /path/to/file.c)

  claude -p --model claude-sonnet-4-6 "$RULES

=== SOURCE FILE ===
$SOURCE
=== END SOURCE ===" > /path/to/file.c.reformatted

  diff /path/to/file.c /path/to/file.c.reformatted

For the layout judgment pass, substitute AI_PREAMBLE_AESTHETIC.md and omit the
style files (the preamble is self-contained):

  RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_AESTHETIC.md)

Batch reformatting a directory (shell script):

  #!/usr/bin/env bash
  set -euo pipefail

  STYLE_DIR="$(dirname "$0")"   # assumes script lives next to the style files
  RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_C_CPP.md)
  SRC_DIR="$1"
  OUT_DIR="${2:-$SRC_DIR/reformatted}"
  mkdir -p "$OUT_DIR"

  for f in "$SRC_DIR"/*.{c,h}; do
      [ -f "$f" ] || continue
      base=$(basename "$f")
      echo "Reformatting $base..."
      source_text=$(cat "$f")
      claude -p --model claude-sonnet-4-6 "$RULES

=== SOURCE FILE: $base ===
$source_text
=== END SOURCE ===" > "$OUT_DIR/$base"
      echo "  -> $OUT_DIR/$base"
  done

  echo "Done. Review diffs with:"
  echo "  diff -r $SRC_DIR $OUT_DIR"

Save as reformat.sh next to the style files, make executable (chmod +x reformat.sh):
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
                  f"{rules_text}\n\n"
                  f"=== SOURCE FILE: {source_path} ===\n{source}\n=== END SOURCE ==="
              )
          }]
      )
      return msg.content[0].text

  if __name__ == "__main__":
      if len(sys.argv) < 3:
          print(f"Usage: {sys.argv[0]} <source_file> <lang: c|cpp|java|kotlin> [pass: full|aesthetic]")
          sys.exit(1)

      src, lang = sys.argv[1], sys.argv[2]
      mode = sys.argv[3] if len(sys.argv) > 3 else "full"
      style_dir = pathlib.Path(__file__).parent

      if mode == "aesthetic":
          rules = load_rules(style_dir / "AI_PREAMBLE_AESTHETIC.md")
      elif lang in ("c", "cpp"):
          rules = load_rules(style_dir / "AI_PREAMBLE_FULL.md", style_dir / "STYLE.md",
                             style_dir / "STYLE_C_CPP.md", style_dir / "STYLE_CPP20.md")
      elif lang == "java":
          rules = load_rules(style_dir / "AI_PREAMBLE_FULL.md", style_dir / "STYLE.md",
                             style_dir / "STYLE_JAVA.md", style_dir / "STYLE_JAVA17.md")
      elif lang == "kotlin":
          rules = load_rules(style_dir / "AI_PREAMBLE_FULL.md", style_dir / "STYLE.md",
                             style_dir / "STYLE_KOTLIN.md", style_dir / "STYLE_KOTLIN2.md")
      else:
          print(f"Unknown language: {lang}"); sys.exit(1)

      result = reformat(src, rules)
      # API mode: write to staging file; the caller diffs and applies
      out = pathlib.Path(src).with_suffix(pathlib.Path(src).suffix + ".reformatted")
      out.write_text(result)
      print(f"Written: {out}")
      print(f"Review:  diff {src} {out}")

Usage:
  export ANTHROPIC_API_KEY="sk-ant-..."

  # Full-file pass (un-JAR-processed file):
  python3 reformat_file.py src/Utils.c c

  # Layout judgment pass (post-JAR file):
  python3 reformat_file.py src/Utils.c c aesthetic

  # Kotlin full-file pass (fallback if the JAR doesn't handle a construct yet):
  python3 reformat_file.py src/Utils.kt kotlin


Tips and Limitations
---------------------
1. Review every diff manually. The model makes mistakes, especially on:
   - Large declaration groups requiring precise column alignment (full-file pass)
   - Getter/setter aligned groups (STYLE.md §14)
   - Complex bracket-padding decisions near the rule boundary (§3.1)

2. For the full-file pass, process files that are already mostly correct. AI
   reformatting works best as a consistency pass, not a from-scratch transformation.

3. For the layout judgment pass, check that the AI has not disturbed JAR-applied
   alignment — any change outside argument lists and getter/setter groups is a bug.

4. Some comment changes in a full-file pass diff are intentional: the model applies
   §15 (removes trailing periods from // comments; converts multi-sentence comments
   to /* */ form). Watch for unintentional changes — dropped comments or altered
   wording — which are bugs, not style fixes.
