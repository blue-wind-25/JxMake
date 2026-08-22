Using AI to Apply the Coding Style Guide
=========================================

Files in this directory
-----------------------
  STYLE.md                  Common rules for all languages (read this first)
  STYLE_C_CPP.md            C and C++ extensions/overrides (baseline)
  STYLE_CPP20.md            C++17/20/23 extensions/overrides (read after STYLE_C_CPP.md)
  STYLE_CPP26.md            C++26 extensions/overrides (read after STYLE_CPP20.md)
  STYLE_JAVA.md             Java extensions/overrides (baseline)
  STYLE_JAVA17.md           Java extensions/overrides (newer constructs, read after STYLE_JAVA.md)
  STYLE_KOTLIN.md           Kotlin extensions/overrides (baseline)
  STYLE_KOTLIN2.md          Kotlin extensions/overrides (newer constructs, read after STYLE_KOTLIN.md)
  STYLE_DATA_FORMATS.md     JSON/JSON5, CSS, YAML, TOML, XML, HTML5 rules (borrows from STYLE.md)
  STYLE_JS_TS.md            JavaScript/TypeScript rules (derives from STYLE_JAVA.md/STYLE_KOTLIN.md)
  STYLE_PYTHON3.md          Python 3 rules
  STYLE_TOOLING.md          E-INI, Makefile, Bash, and PowerShell rules (narrow beautification-only)
  STYLE_JXMAKE.md           JxMakeFile rules (narrow beautification-only)
  AI_PREAMBLE_FULL.md       Preamble for full-file pass (un-JAR-processed files)
  AI_PREAMBLE_AESTHETIC.md  Preamble for layout judgment pass (post-JAR files)
  README.txt                This file

  The deterministic JAR formatter (formatter/code-formatter-1.0.1.jar, replace
  1.0.1 with your built version) handles all Tier-1 and Tier-2 rules
  mechanically for C, C++, Java, Kotlin, JSON/JSON5, CSS, YAML, TOML, XML,
  HTML5, JavaScript, TypeScript, Python 3, E-INI, JxMakeFile, Makefile, Bash, and PowerShell.
  (Tier-1 is plain deterministic rule application; Tier-2 is also fully built
  into the JAR, but resolves a handful of ambiguous comment-capitalization
  cases via an on-device linear-classifier + GRU stack -- see formatter/
  README.md's "Comment classifier (GRU)" section.)
  Run it first for those languages. The AI workflows described here cover the
  remaining Tier-3 aesthetic decisions the JAR intentionally leaves untouched
  (data formats and the five narrow-beautification-only languages -- E-INI,
  JxMakeFile, Makefile, Bash, PowerShell -- have no equivalent layout-judgment
  gap, see the Layout Judgment Pass note below; applies to
  every other language — C, C++, Java, Kotlin, JavaScript, TypeScript,
  Python 3):
    - Function argument list layout (when the source is already multi-line and
      the author's grouping intent should be preserved or improved)
    - Getter/setter groups with non-standard naming conventions (Python's
      `@property`/`@x.setter` accessors never compact per STYLE_PYTHON3.md
      §4, so this decision doesn't arise there — see AI_PREAMBLE_AESTHETIC.md's
      Scope section)

  NOTE — Kotlin: Both AI passes below apply to Kotlin the same way they apply
  to C/C++/Java. The one real exception: three specific property-accessor shapes
  — block-bodied accessors (`get() {...}`/`set(v) {...}`), a property pairing
  a getter with a setter, and a property with both an initializer and a custom
  accessor — are left preserved-as-written by the JAR rather than grouped/aligned.
  This is a deliberate scope boundary (these shapes are considerably more complex
  to group correctly than the plain one-liner cases the JAR already handles), not
  an immaturity gap.
  AI_PREAMBLE_AESTHETIC.md's Rule 2 covers aligning these three shapes manually
  when you hit one.

  There are two AI passes, described below. Use only one per file per run.


Two AI Passes
-------------

  FULL-FILE PASS  (AI_PREAMBLE_FULL.md)
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  Applies all style rules to a file that has NOT yet been processed by the JAR.
  Useful for one-off migration of legacy files. More expensive in time and quota
  since the entire file is reformatted, including rules the JAR handles better
  and more reliably. AI_PREAMBLE_FULL.md's "Don't Eyeball Whitespace" section
  instructs the model to compute column alignment and padding with a script
  rather than by visual judgment, which reduces (but does not eliminate) the
  mistakes this used to cause — still review every diff carefully, especially
  in large declaration groups.

  For every language the JAR now implements (C, C++, Java, Kotlin, JSON/JSON5,
  CSS, YAML, TOML, XML, HTML5, JavaScript, TypeScript, Python 3, E-INI,
  JxMakeFile, Makefile, Bash, PowerShell — see the NOTE above), prefer running
  the JAR directly
  instead; this pass is now only needed for a one-off migration of a legacy
  file, or a construct the JAR doesn't yet handle for a given language.

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

Recommended models (minimum), as of mid-2026:
  Claude Sonnet 5   (claude-sonnet-5)   — good balance of quality and speed
  Claude Opus 4.8   (claude-opus-4-8)   — best for large or complex files
  Claude Fable 5                        — Anthropic's top-end coding model

Non-Anthropic equivalents at the same tier:
  GPT-5.5 / GPT-5.6, Gemini 3.1 Pro / 3.5 Pro, Grok 4.3 / 4.5

This list will go stale — labs are shipping flagship updates every 4-8 weeks
at this point. The bar that matters is the one above (Do NOT use small/fast
models), not any specific model name: don't use a small/fast tier (Haiku,
Flash, GPT-mini, etc.) for these tasks regardless of how the flagship-tier
name has changed since this file was last updated.

Effort level: the Anthropic API's `effort` parameter (`output_config.effort`
in the Messages API, `--effort` as a `claude -p` flag) defaults to `high` if
you don't set it — but for these reformatting passes, explicitly set
`medium` instead. The task is a single, well-specified, deterministic
transform, not open-ended reasoning, so `high`'s extra token spend buys
little here. Step up to `high` or `xhigh` for an unusually large or complex
file, or down to `low` for a small/simple one where you're optimizing for
speed. See `reformat_file.py`'s Usage examples below for the CLI shape.

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
    cat AI_PREAMBLE_FULL.md STYLE.md STYLE_JAVA.md STYLE_JAVA17.md > /tmp/style_java_full.txt

  FULL-FILE PASS — Kotlin files:
    cat AI_PREAMBLE_FULL.md STYLE.md STYLE_KOTLIN.md STYLE_KOTLIN2.md > /tmp/style_kotlin_full.txt

  FULL-FILE PASS — JSON/JSON5, CSS, YAML, TOML, XML, or HTML5 files:
    cat AI_PREAMBLE_FULL.md STYLE.md STYLE_DATA_FORMATS.md > /tmp/style_data_formats_full.txt
    (only needed now for a one-off migration — the JAR itself now handles
    these languages directly, including HTML5's <script> dispatch to JS/TS,
    see the NOTE near the top of this file. For an HTML5 file whose <script>
    content needs its own full-file pass, add STYLE_JS_TS.md's own
    combination below to the same cat)

  FULL-FILE PASS — JavaScript/TypeScript files:
    cat AI_PREAMBLE_FULL.md STYLE.md STYLE_JAVA.md STYLE_KOTLIN.md STYLE_JS_TS.md > /tmp/style_js_ts_full.txt
    (STYLE_JAVA.md/STYLE_KOTLIN.md included because STYLE_JS_TS.md derives most of
    its rules from them by section-number citation rather than restating content)

  FULL-FILE PASS — Python3 files:
    cat AI_PREAMBLE_FULL.md STYLE.md STYLE_PYTHON3.md > /tmp/style_python3_full.txt

  FULL-FILE PASS — E-INI, Makefile, Bash, or PowerShell files:
    cat AI_PREAMBLE_FULL.md STYLE.md STYLE_TOOLING.md > /tmp/style_tooling_full.txt
    (only needed now for a one-off migration — the JAR itself now handles
    these languages directly per STYLE_TOOLING.md's fixed rule lists)

  FULL-FILE PASS — JxMakeFile files:
    cat AI_PREAMBLE_FULL.md STYLE.md STYLE_JXMAKE.md > /tmp/style_jxmake_full.txt
    (only needed now for a one-off migration — the JAR itself now handles
    JxMakeFile directly per STYLE_JXMAKE.md's fixed rule list)

  LAYOUT JUDGMENT PASS — C, C++, Java, Kotlin, JavaScript, TypeScript, or
  Python 3 (language-agnostic preamble, see AI_PREAMBLE_AESTHETIC.md's Scope
  section):
    cat AI_PREAMBLE_AESTHETIC.md > /tmp/style_aesthetic.txt
    (no style files needed — the preamble is self-contained for this pass)
    Does NOT apply to JSON/JSON5/CSS/YAML/TOML/XML/HTML5, E-INI, JxMakeFile,
    Makefile, Bash, or PowerShell (C++26 is rule coverage on the existing
    C/C++ pipeline, not a separate language, so it's covered by the C/C++ case
    above). The data formats and narrow-beautification-only languages have no
    equivalent layout-judgment gap to begin with (no function-argument-list or
    getter/setter-group concept), so there is nothing for this pass to add on
    top of their JAR output.

Store the combined file once and reuse it across multiple calls.


CLI Mode  (using the `claude` command-line tool)
------------------------------------------------
Non-interactive, single-file reformatting. The output is written to a staging
file for review; apply it in-place only after diffing.

  STYLE_DIR=/path/to/CodingStyle.md
  RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_C_CPP.md)
  SOURCE=$(cat /path/to/file.c)

  claude -p --model sonnet --effort medium "$RULES

=== SOURCE FILE ===
$SOURCE
=== END SOURCE ===" > /path/to/file.c.reformatted

  diff /path/to/file.c /path/to/file.c.reformatted

For the layout judgment pass, substitute AI_PREAMBLE_AESTHETIC.md and omit the
style files (the preamble is self-contained):

  RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_AESTHETIC.md)

Batch reformatting a directory (shell script):

  Mirrors reformat_file.py's language dispatch below — same lang codes, same
  RULES combinations — so the two examples stay consistent instead of the shell
  version only demonstrating one language.

  #!/usr/bin/env bash
  set -euo pipefail

  STYLE_DIR="$(dirname "$0")"   # assumes script lives next to the style files
  LANG="$1"
  SRC_DIR="$2"
  OUT_DIR="${3:-$SRC_DIR/reformatted}"
  mkdir -p "$OUT_DIR"

  case "$LANG" in
    c)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_C_CPP.md)
      GLOBS=("*.c" "*.h") ;;
    cpp)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_C_CPP.md "$STYLE_DIR"/STYLE_CPP20.md)
      GLOBS=("*.cpp" "*.hpp") ;;
    cpp26)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_C_CPP.md "$STYLE_DIR"/STYLE_CPP20.md "$STYLE_DIR"/STYLE_CPP26.md)
      GLOBS=("*.cpp" "*.hpp") ;;
    java)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_JAVA.md "$STYLE_DIR"/STYLE_JAVA17.md)
      GLOBS=("*.java") ;;
    kotlin)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_KOTLIN.md "$STYLE_DIR"/STYLE_KOTLIN2.md)
      GLOBS=("*.kt" "*.kts") ;;
    json|json5|css|yaml|toml|xml|html)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_DATA_FORMATS.md)
      GLOBS=("*.$LANG") ;;
    js)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_JAVA.md "$STYLE_DIR"/STYLE_KOTLIN.md "$STYLE_DIR"/STYLE_JS_TS.md)
      GLOBS=("*.js") ;;
    ts)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_JAVA.md "$STYLE_DIR"/STYLE_KOTLIN.md "$STYLE_DIR"/STYLE_JS_TS.md)
      GLOBS=("*.ts") ;;
    python3)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_PYTHON3.md)
      GLOBS=("*.py") ;;
    eini)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_TOOLING.md)
      GLOBS=("*.ini") ;;
    jxmake)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_JXMAKE.md)
      GLOBS=("JxMakeFile" "*.jxm") ;;
    makefile)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_TOOLING.md)
      GLOBS=("Makefile" "GNUmakefile" "*.mk") ;;
    bash)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_TOOLING.md)
      GLOBS=("*.sh" "*.bash") ;;
    powershell)
      RULES=$(cat "$STYLE_DIR"/AI_PREAMBLE_FULL.md "$STYLE_DIR"/STYLE.md "$STYLE_DIR"/STYLE_TOOLING.md)
      GLOBS=("*.ps1" "*.psm1") ;;
    *)
      echo "Unknown language: $LANG"; exit 1 ;;
  esac

  for pattern in "${GLOBS[@]}"; do
    for f in "$SRC_DIR"/$pattern; do
        [ -f "$f" ] || continue
        base=$(basename "$f")
        echo "Reformatting $base..."
        source_text=$(cat "$f")
        claude -p --model sonnet --effort medium "$RULES

=== SOURCE FILE: $base ===
$source_text
=== END SOURCE ===" > "$OUT_DIR/$base"
        echo "  -> $OUT_DIR/$base"
    done
  done

  echo "Done. Review diffs with:"
  echo "  diff -r $SRC_DIR $OUT_DIR"

Save as reformat.sh next to the style files, make executable (chmod +x reformat.sh):
  ./reformat.sh cpp src/mymodule/
  ./reformat.sh python3 src/mymodule/


API / Bot Mode  (Anthropic Python SDK)
---------------------------------------
Install the SDK:
  pip install anthropic

Script (reformat_file.py):

  #!/usr/bin/env python3
  import sys, pathlib, anthropic

  def load_rules(*paths):
      return "\n\n".join(pathlib.Path(p).read_text() for p in paths)

  def reformat(source_path, rules_text, model="claude-sonnet-5", effort="medium"):
      # NOTE: unlike the CLI's `sonnet`/`opus` aliases, the Messages API has no
      # evergreen model alias -- pin an explicit, versioned model ID here and
      # update it periodically. Check https://docs.claude.com/en/about-claude/models
      # for the current recommended string before relying on this in production.
      source = pathlib.Path(source_path).read_text()
      client = anthropic.Anthropic()   # reads ANTHROPIC_API_KEY from env
      msg = client.messages.create(
          model=model,
          max_tokens=8192,
          output_config={"effort": effort},  # low|medium|high(default)|xhigh|max
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
          print(f"Usage: {sys.argv[0]} <source_file> "
                f"<lang: c|cpp|cpp26|java|kotlin|json|json5|css|yaml|toml|xml|html|js|ts|python3|eini|jxmake|makefile|bash|powershell> "
                f"[pass: full|aesthetic] [effort: low|medium|high|xhigh|max]")
          sys.exit(1)

      src, lang = sys.argv[1], sys.argv[2]
      mode = sys.argv[3] if len(sys.argv) > 3 else "full"
      effort = sys.argv[4] if len(sys.argv) > 4 else "medium"
      style_dir = pathlib.Path(__file__).parent

      if mode == "aesthetic":
          # Language-agnostic (not for JSON/JSON5/CSS/YAML/TOML/XML/HTML5,
          # Makefile/Bash/PowerShell) -- see AI_PREAMBLE_AESTHETIC.md's Scope
          # section.
          rules = load_rules(style_dir / "AI_PREAMBLE_AESTHETIC.md")
      elif lang == "cpp":
          rules = load_rules(style_dir / "AI_PREAMBLE_FULL.md", style_dir / "STYLE.md",
                             style_dir / "STYLE_C_CPP.md", style_dir / "STYLE_CPP20.md")
      elif lang == "cpp26":
          rules = load_rules(style_dir / "AI_PREAMBLE_FULL.md", style_dir / "STYLE.md",
                             style_dir / "STYLE_C_CPP.md", style_dir / "STYLE_CPP20.md",
                             style_dir / "STYLE_CPP26.md")
      elif lang == "c":
          rules = load_rules(style_dir / "AI_PREAMBLE_FULL.md", style_dir / "STYLE.md",
                             style_dir / "STYLE_C_CPP.md")
      elif lang == "java":
          rules = load_rules(style_dir / "AI_PREAMBLE_FULL.md", style_dir / "STYLE.md",
                             style_dir / "STYLE_JAVA.md", style_dir / "STYLE_JAVA17.md")
      elif lang == "kotlin":
          rules = load_rules(style_dir / "AI_PREAMBLE_FULL.md", style_dir / "STYLE.md",
                             style_dir / "STYLE_KOTLIN.md", style_dir / "STYLE_KOTLIN2.md")
      elif lang in ("json", "json5", "css", "yaml", "toml", "xml", "html"):
          rules = load_rules(style_dir / "AI_PREAMBLE_FULL.md", style_dir / "STYLE.md",
                             style_dir / "STYLE_DATA_FORMATS.md")
          # HTML files embedding <script> also need the js/ts combination above
          # added to this same rules set — not handled automatically here.
      elif lang in ("js", "ts"):
          rules = load_rules(style_dir / "AI_PREAMBLE_FULL.md", style_dir / "STYLE.md",
                             style_dir / "STYLE_JAVA.md", style_dir / "STYLE_KOTLIN.md",
                             style_dir / "STYLE_JS_TS.md")
      elif lang == "python3":
          rules = load_rules(style_dir / "AI_PREAMBLE_FULL.md", style_dir / "STYLE.md",
                             style_dir / "STYLE_PYTHON3.md")
      elif lang in ("eini", "makefile", "bash", "powershell"):
          rules = load_rules(style_dir / "AI_PREAMBLE_FULL.md", style_dir / "STYLE.md",
                             style_dir / "STYLE_TOOLING.md")
      elif lang == "jxmake":
          rules = load_rules(style_dir / "AI_PREAMBLE_FULL.md", style_dir / "STYLE.md",
                             style_dir / "STYLE_JXMAKE.md")
      else:
          print(f"Unknown language: {lang}"); sys.exit(1)

      result = reformat(src, rules, effort=effort)
      # API mode: write to staging file; the caller diffs and applies
      out = pathlib.Path(src).with_suffix(pathlib.Path(src).suffix + ".reformatted")
      out.write_text(result)
      print(f"Written: {out}")
      print(f"Review:  diff {src} {out}")

Usage:
  export ANTHROPIC_API_KEY="sk-ant-..."

  # Full-file pass (un-JAR-processed file):
  python3 reformat_file.py src/Utils.c c

  # C++ full-file pass (baseline, no C++26 constructs):
  python3 reformat_file.py src/Utils.cpp cpp

  # C++ full-file pass, including C++26 constructs:
  python3 reformat_file.py src/Utils.cpp cpp26

  # Layout judgment pass (post-JAR file; language-agnostic, see
  # AI_PREAMBLE_AESTHETIC.md's Scope section):
  python3 reformat_file.py src/Utils.c c aesthetic
  python3 reformat_file.py src/utils.py python3 aesthetic

  # Kotlin full-file pass (fallback if the JAR doesn't handle a construct yet):
  python3 reformat_file.py src/Utils.kt kotlin

  # Data-format full-file pass (JAR now handles this directly for JSON/JSON5/
  # CSS/YAML/TOML/XML/HTML5, including HTML5's <script> dispatch to JS/TS --
  # only needed for one-off migration, see the NOTE near the top of this file):
  python3 reformat_file.py config.json json

  # JavaScript/TypeScript full-file pass (fallback -- the JAR now handles
  # this directly, see the NOTE near the top of this file):
  python3 reformat_file.py src/utils.ts ts

  # Python3 full-file pass (fallback -- the JAR now handles this directly):
  python3 reformat_file.py src/utils.py python3

  # E-INI full-file pass (fallback -- the JAR now handles this directly per
  # STYLE_TOOLING.md §4):
  python3 reformat_file.py config.ini eini

  # JxMakeFile full-file pass (fallback -- the JAR now handles this directly
  # per STYLE_JXMAKE.md):
  python3 reformat_file.py JxMakeFile jxmake

  # Tooling-script full-file pass (fallback -- the JAR now handles Makefile/
  # Bash/PowerShell directly per STYLE_TOOLING.md):
  python3 reformat_file.py Makefile makefile
  python3 reformat_file.py scripts/build.sh bash
  python3 reformat_file.py tools/deploy.ps1 powershell

  # Same, at explicit low effort (cheaper, for a simple/small file) or xhigh
  # (for a large or unusually complex one -- effort defaults to "medium" if
  # omitted, see Model Selection above):
  python3 reformat_file.py src/utils.py python3 full low
  python3 reformat_file.py src/BigLegacyFile.cpp cpp26 full xhigh


Tips and Limitations
---------------------
1. Review every diff manually. Both AI_PREAMBLE_FULL.md and
   AI_PREAMBLE_AESTHETIC.md now instruct the model to compute column alignment,
   padding, and indent width with a script rather than by eye (see each
   preamble's "Don't Eyeball" section) — this cuts down on, but does not
   eliminate, mistakes in:
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
   wording — which are bugs, not style fixes. §15's mechanism varies by language for
   the newer language families — Python's `#`-only syntax keeps multi-sentence
   comments as consecutive `#` lines rather than switching to a block form;
   YAML/TOML are `#`-only the same way; CSS/XML/HTML5/JSON5 are already
   block-only, so there's no line-to-block switch to look for at all. See
   AI_PREAMBLE_FULL.md §15 for the per-language specifics.
