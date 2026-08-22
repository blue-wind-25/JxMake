tools/verifiers/ -- formatter verification scripts
===================================================

This directory holds two kinds of checker, one pair per supported language:

  * <lang>_syntax_check.<ext>  -- "is this file still syntactically valid?"
  * <lang>_content_diff.<ext>  -- "did formatting change what the file
                                   actually means (as opposed to just its
                                   whitespace/layout)?"

Both kinds are meant to be run against real-code test fixtures: syntax_check on a single file,
content_diff on an original file compared against its formatted output. Neither kind reformats
anything itself -- they are read-only checks used to validate the formatter's output.

Every *_syntax_check.sh / *_content_diff.sh entry point is a thin bash wrapper. Depending on the
language, the wrapper either runs a system compiler directly (C/C++) or hands off to a small
supporting program written in Python, JavaScript (Node.js), or Java, invoked through one of the
shared "_exec_*" launcher scripts described below.


Shared "_exec_*" launcher scripts
----------------------------------

Files whose name starts with "_exec_" are not run directly. They are common plumbing, sourced or
exec'd by the per-language wrapper scripts, and exist so that runtime setup (compiler/interpreter
paths, library paths, module checks, compile-on-demand caching) is written once instead of once
per language:

  _exec_c_cpp_env.sh   Compiler-location env vars for the C/C++ family. Sourced by _exec_c_cpp.sh.

  _exec_c_cpp.sh       Shared launcher for all c*/cpp*_syntax_check.sh and c*/cpp*_content_diff.sh
                       scripts. Runs GCC/Clang directly for a syntax check (-fsyntax-only), or
                       dispatches to _exec_c_cpp.py for a content diff (AST/tree-dump comparison).

  _exec_c_cpp.py       Shared compare engine used by the C/C++ family's content_diff scripts (via
                       _exec_c_cpp.sh). Not meant to be invoked directly.

  _exec_java.sh        Shared launcher for the Java and Kotlin syntax_check/content_diff scripts.
                       Compiles the matching .java helper on demand (if missing or stale) and then
                       runs it.

  _exec_node_env.sh    Common Node.js runtime environment (install location, library path, PATH/
                       NODE_PATH setup). Sourced by _exec_nodejs.sh, and directly by any
                       Python-based script that also needs to shell out to Node.js.

  _exec_nodejs.sh      Shared launcher for Node.js-based helper scripts. Optionally checks if the
                       required npm packages are installed before running.

  _exec_python.sh      Shared launcher for Python-based helper scripts. Optionally sources the
                       Node.js environment first (for scripts that shell out to Node), and
                       optionally checks that required Python modules are installed before running.

User-configurable environment variables
----------------------------------------

Each variable below is declared with a bash "default-if-unset" pattern (": ${VAR:=default}"), so
it can be overridden in your shell or CI environment without editing any script -- useful if your
machine has these tools installed somewhere other than the paths baked in here.

  GXX                    Defined in : _exec_c_cpp_env.sh
                         Default    : /opt/gcc-12.2.0/bin/g++

                         Path to the g++ binary used by every C/C++ syntax_check/content_diff script
                         that runs GCC.

  CLANGXX                Defined in : _exec_c_cpp_env.sh
                         Default    : $HOME/xsdk/clang22/LLVM-22.1.8-Linux-X64/bin/clang++

                         Path to the clang++ binary used by every C/C++ syntax_check/content_diff
                         script that runs Clang (needed for standards GCC 12.2.0 doesn't support
                         literally, e.g. C23/C++26).

  USER_LD_LIBRARY_PATH   Defined in                : _exec_c_cpp_env.sh AND _exec_node_env.sh
                         Default (C/C++ scripts  ) : /opt/isl-0.16.1/lib
                         Default (Node.js scripts) : /opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib

                         Extra shared-library search path exported into  LD_LIBRARY_PATH before
                         running the compiler or Node.js, so their runtime dependencies (e.g. libisl)
                         can be found. Which default applies depends on which _exec_*_env.sh script
                         is in effect for the wrapper you're running.

  JDK                    Defined in : _exec_java.sh
                         Default    : /opt/openjdk-21_linux-x64_bin/jdk-21

                         Path to the JDK 21 install used to compile and run the Java-based helper
                         programs (java_*, kotlin_*).

  KLIB                   Defined in : _exec_java.sh
                         Default    : $HOME/xsdk/kotlin-compiler-2.4.0/kotlinc/lib

                         Path to the Kotlin compiler's lib directory, needed on the classpath only
                         for the kotlin_* helper programs (kotlin-compiler.jar, kotlin-stdlib.jar).

  NODE_HOME              Defined in : _exec_node_env.sh
                         Default    : /opt/node-v24.14.0-linux-x64

                         Path to the Node.js install used to run every Node.js-based helper script.

  USER_NPM_HOME          Defined in : _exec_node_env.sh
                         Default    : $HOME/mynpm

                         Location where required npm packages (postcss, json5, js-yaml, smol-toml,
                         @xmldom/xmldom, parse5, etc.) are expected to be installed, and where the
                         "install it with..." hint from a missing-module error points.

  PYTHON                 Defined in : _exec_python.sh
                         Default    : python3.12

                         Python interpreter used to run every Python-based helper script.

Note: several of the env vars above are declared in more than one file under the same name but with
different defaults (USER_LD_LIBRARY_PATH in particular). Setting one in your environment overrides
both declarations, using whichever default would otherwise have applied to the script you're running.


C/C++ family: c, c20, cpp, cpp20, cpp26
---------------------------------------

These five language "scopes" share one implementation (_exec_c_cpp.sh / _exec_c_cpp.py); each
*_syntax_check.sh / *_content_diff.sh wrapper just sets a few variables (which compiler, which
-std, which comparison mode) before sourcing the shared launcher. Naming note: "c20"/"cpp20" name
the scope up to C++23-equivalent standards, matching this project's STYLE_*.md naming (see
c20_syntax_check.sh's own comments for the full explanation); it is not a typo for "C++20"
specifically.

  c_syntax_check.sh
      Legacy C (through C17/18) syntax check. Runs `g++ -x c -std=gnu99 -fsyntax-only` on each
      input file via GXX.

      Usage      : c_syntax_check.sh <file.c> [file2.c ...]
      Depends on : _exec_c_cpp.sh, _exec_c_cpp_env.sh.

  c_content_diff.sh
      Legacy C content-preservation check via GCC's -fdump-tree-original AST-ish dump (-std=gnu99),
      compared textually.

      Usage      : c_content_diff.sh <original.c> <formatted.c>
                   c_content_diff.sh <original_base_dir> <formatted_base_dir> <c_rel_path_file_list.txt>
      Depends on : _exec_c_cpp.sh, _exec_c_cpp_env.sh, _exec_c_cpp.py.

  c20_syntax_check.sh
      C23-scope syntax check. Runs Clang with `-std=c23` (the accurate check) via CLANGXX, plus
      GCC's `-std=c2x` as a best-effort secondary (GCC 12.2.0 has no literal C23 mode).

      Usage      : c20_syntax_check.sh <file.c> [file2.c ...]
      Depends on : _exec_c_cpp.sh, _exec_c_cpp_env.sh.

  c20_content_diff.sh
      C23-scope content-preservation check via Clang's -Xclang -ast-dump with -std=c23 (text-mode
      AST dump, terser than the JSON mode used for cpp26).

      Usage      : c20_content_diff.sh <original.c> <formatted.c>
                   c20_content_diff.sh <original_base_dir> <formatted_base_dir> <c_rel_path_file_list.txt>
      Depends on : _exec_c_cpp.sh, _exec_c_cpp_env.sh, _exec_c_cpp.py.

  cpp_syntax_check.sh
      Legacy C++ (through C++17) syntax check. Runs `g++ -std=gnu++17 -fsyntax-only` via GXX.

      Usage      : cpp_syntax_check.sh <file.cpp> [file2.cpp ...]
      Depends on : _exec_c_cpp.sh, _exec_c_cpp_env.sh.

  cpp_content_diff.sh
      Legacy C++ content-preservation check via GCC's -fdump-tree-original, -std=gnu++17.

      Usage      : cpp_content_diff.sh <original.cpp> <formatted.cpp>
                   cpp_content_diff.sh <original_base_dir> <formatted_base_dir> <cpp_rel_path_file_list.txt>
      Depends on : _exec_c_cpp.sh, _exec_c_cpp_env.sh, _exec_c_cpp.py.

  cpp20_syntax_check.sh
      C++20-until-C++23 syntax check. Runs both GCC and Clang with literal `-std=c++20`.

      Usage      : cpp20_syntax_check.sh <file.cpp> [file2.cpp ...]
      Depends on : _exec_c_cpp.sh, _exec_c_cpp_env.sh.

  cpp20_content_diff.sh
      C++20-until-C++23 content-preservation check via GCC's -fdump-tree-original, -std=c++20.

      Usage      : cpp20_content_diff.sh <original.cpp> <formatted.cpp>
                   cpp20_content_diff.sh <original_base_dir> <formatted_base_dir> <cpp_rel_path_file_list.txt>
      Depends on : _exec_c_cpp.sh, _exec_c_cpp_env.sh, _exec_c_cpp.py.

  cpp26_syntax_check.sh
      C++26 syntax check. Runs Clang with literal `-std=c++26` via CLANGXX (the accurate check),
      plus GCC's `-std=c++2b` as a best-effort secondary (GCC 12.2.0 has no literal C++26 mode).

      Usage      : cpp26_syntax_check.sh <file.cpp> [file2.cpp ...]
      Depends on : _exec_c_cpp.sh, _exec_c_cpp_env.sh.

  cpp26_content_diff.sh
      C++26 content-preservation check via Clang's -Xclang -ast-dump=json -std=c++26. Known
      limitation: this JSON dump embeds source-location fields (line/col/file) that legitimately
      shift with pure reformatting, so it is a stricter, location-sensitive check than the other
      content_diff scripts -- use c_content_diff.sh's or c20_content_diff.sh's mode instead if
      a location-insensitive check is wanted.

      Usage      : cpp26_content_diff.sh <original.cpp> <formatted.cpp>
                   cpp26_content_diff.sh <original_base_dir> <formatted_base_dir> <cpp_rel_path_file_list.txt>
      Depends on : _exec_c_cpp.sh, _exec_c_cpp_env.sh, _exec_c_cpp.py.

For all ten scripts above, extra compiler options (e.g. -Iinclude/dir) may be passed: for a syntax
check, before the file arguments; for a content diff, after a literal "--" following the
positional arguments.


Java/Kotlin
------------

  java_syntax_check.sh
      Syntax checker for Java source files, using javax.tools' in-process  compiler API (parse-only,
      no class files retained for the input).

      Usage      : java_syntax_check.sh <file.java> [file2.java ...]
      Depends on : java_syntax_check.java (compiled on demand), _exec_java.sh.

  java_content_diff.sh
      Content-preservation checker for Java, comparing parsed structure between an original and
      formatted file.

      Usage      : java_content_diff.sh <original.java> <formatted.java>
      Depends on : java_content_diff.java (compiled on demand), _exec_java.sh.

  kotlin_syntax_check.sh
      Syntax checker for Kotlin source files, using the Kotlin compiler's embeddable PSI front end.

      Usage      : kotlin_syntax_check.sh <file.kt> [file2.kt ...]
      Depends on : kotlin_syntax_check.java (compiled on demand), _exec_java.sh (adds KLIB's
                   kotlin-compiler.jar/kotlin-stdlib.jar to the classpath for kotlin_* programs).

  kotlin_content_diff.sh
      Content-preservation checker for Kotlin, comparing PSI structure between an original and
      formatted file.

      Usage      : kotlin_content_diff.sh <original.kt> <formatted.kt>
      Depends on : kotlin_content_diff.java (compiled on demand), _exec_java.sh.

All four wrappers just set PROGRAM=<name> and source _exec_java.sh, which compiles the matching
<name>.java file on demand (only if its .class is missing or the .java source is newer) and then
runs it, passing through all arguments.


JSON/JSON5
------------

  json_syntax_check.sh
      Syntax checker for JSON source files, using the built-in JSON.parse.

      Usage      : json_syntax_check.sh <file.json> [file2.json ...]
      Depends on : json_syntax_check.js, _exec_nodejs.sh.

  json_content_diff.sh
      Content-preservation checker for JSON. Parses both files with the Python standard library's
      `json` module and compares the resulting values. JSON has no comment syntax, so unlike the
      YAML/TOML content-diff checkers there is no separate comment-preservation side-channel.

      Usage      : json_content_diff.sh <original.json> <formatted.json>
      Depends on : json_content_diff.py, _exec_python.sh.

  json5_syntax_check.sh
      Syntax checker for JSON5 source files, using the `json5` npm package.

      Usage      : json5_syntax_check.sh <file.json5> [file2.json5 ...]
      Depends on : json5_syntax_check.js, _exec_nodejs.sh (requires the "json5" npm module).

  json5_content_diff.sh
      Content-preservation checker for JSON5, using the `json5` npm package. Compares parsed
      values between an original and formatted file, and separately scans `//` line comments and
      `/* */` block comments as an informational-only signal (does not fail the check by itself).

      Usage      : json5_content_diff.sh <original.json5> <formatted.json5>
      Depends on : json5_content_diff.js, _exec_nodejs.sh (requires the "json5" npm module).


CSS
---

  css_syntax_check.sh
      Syntax checker for CSS source files, using the `postcss` npm package.

      Usage      : css_syntax_check.sh <file.css> [file2.css ...]
      Depends on : css_syntax_check.js, _exec_nodejs.sh (requires the "postcss" npm module).

  css_content_diff.sh
      Content-preservation checker for CSS. Compares comments and parsed rule structure between
      an original and formatted file.

      Usage      : css_content_diff.sh <original.css> <formatted.css>
      Depends on : css_content_diff.py, _exec_python.sh.


YAML
----

  yaml_syntax_check.sh
      Syntax checker for YAML source files, using the `js-yaml` npm package's loadAll() (checks
      every document in a multi-document stream).

      Usage      : yaml_syntax_check.sh <file.yaml> [file2.yaml ...]
      Depends on : yaml_syntax_check.js, _exec_nodejs.sh (requires the "js-yaml" npm module).

  yaml_content_diff.sh
      Content-preservation checker for YAML. Parses both files with PyYAML's `yaml.safe_load_all`
      and compares the resulting data structures document-by-document. Does not catch comment-only
      changes.

      Usage       : yaml_content_diff.sh <original.yaml> <formatted.yaml>
      Depends on  : yaml_content_diff.py, _exec_python.sh (requires the Python "yaml" module).


TOML
----

  toml_syntax_check.sh
      Syntax checker for TOML source files, using the `smol-toml` npm package.

      Usage      : toml_syntax_check.sh <file.toml> [file2.toml ...]
      Depends on : toml_syntax_check.js, _exec_nodejs.sh (requires the "smol-toml" npm module).

  toml_content_diff.sh
      Content-preservation checker for TOML. Runs as Python but shells out to a Node.js helper
      internally, so it needs the Node.js environment set up too.

      Usage      : toml_content_diff.sh <original.toml> <formatted.toml>
      Depends on : toml_content_diff.py, _exec_python.sh (with NODE_ENV=1, which also sources
                   _exec_node_env.sh).

XML
---

  xml_syntax_check.sh
      Syntax checker for XML source files, using the `@xmldom/xmldom` npm package with a custom
      error handler (xmldom's default handler does not throw on error-level problems).

      Usage      : xml_syntax_check.sh <file.xml> [file2.xml ...]
      Depends on : xml_syntax_check.js, _exec_nodejs.sh (requires the "@xmldom/xmldom" npm module).

  xml_content_diff.sh
      Content-preservation checker for XML. Parses both files with the Python standard library's
      `xml.dom.minidom` and walks both DOMs in parallel, comparing structure.

      Usage      : xml_content_diff.sh <original.xml> <formatted.xml>
      Depends on : xml_content_diff.py, _exec_python.sh.


HTML5
-----

  html_syntax_check.sh
      Syntax checker for HTML5 source files, using the `parse5` npm package (a spec-compliant
      WHATWG HTML5 parser) and its parse-error callback.

      Usage      : html_syntax_check.sh <file.html> [file2.html ...]
      Depends on : html_syntax_check.js, _exec_nodejs.sh (requires the "parse5" npm module).

  html_content_diff.sh
      Content-preservation checker for HTML5. Runs as Python but shells out to a Node.js/parse5
      helper internally, so it needs the Node.js environment set up too.

      Usage      : html_content_diff.sh <original.html> <formatted.html>
      Depends on : html_content_diff.py, _exec_python.sh (with NODE_ENV=1, which also sources
                   _exec_node_env.sh).


JS/TS
-----

  js_ts_syntax_check.sh
      Syntax checker for JavaScript and TypeScript source files, using the TypeScript compiler
      API (one script handles both `.js` and `.ts`).

      Usage      : js_ts_syntax_check.sh <file.js|.ts> [file2.js|.ts ...]
      Depends on : js_ts_syntax_check.js, _exec_nodejs.sh.

  js_ts_content_diff.sh
      Content-preservation checker for JS/TS, comparing TypeScript-compiler AST structure between
      an original and formatted file.

      Usage      : js_ts_content_diff.sh <original.js|.ts> <formatted.js|.ts>
      Depends on : js_ts_content_diff.js, _exec_nodejs.sh.


Python
------

  python_syntax_check.sh
      Syntax checker for Python source files, using the standard library's `ast.parse` (parse-only;
      no .pyc files generated, no code executed).

      Usage      : python_syntax_check.sh <file.py> [file2.py ...]
      Depends on : python_syntax_check.py, _exec_python.sh.

  python_content_diff.sh
      Content-preservation checker for Python. Parses both files with `ast.parse` and compares
      `ast.dump(tree, include_attributes=False)`.

      Usage      : python_content_diff.sh <original.py> <formatted.py>
      Depends on : python_content_diff.py, _exec_python.sh.


Makefile
--------

  makefile_syntax_check.sh
      Syntax checker for Makefile source files, using `make -n` (dry run) against a throwaway
      wrapper Makefile that `include`s the input file and defines its own no-op target, so the
      check does not fail merely because the input file itself defines no targets (normal for
      `.mk` fragments).

      Usage : makefile_syntax_check.sh <Makefile> [file2.mk ...]

      Standalone -- does not depend on any of the _exec_* launchers or any supporting .py/.js/
      .java file.

      There is no makefile_content_diff.sh in this directory as of this writing.


Bash
----

  bash_syntax_check.sh
      Syntax checker for Bash source files, using `bash -n` (parse-only, no execution).

      Usage : bash_syntax_check.sh <file.sh> [file2.sh ...]

      Standalone -- does not depend on any of the _exec_* launchers or any supporting .py/.js/
      .java file.

      There is no bash_content_diff.sh in this directory as of this writing.


Exit codes
-----------

All *_syntax_check.* scripts follow the same convention: exit 0 if every input file parses
successfully, 1 if one or more files contain syntax errors, and 2 if invoked with no file
arguments (usage error).
