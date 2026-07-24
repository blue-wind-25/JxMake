# jxmake-code-formatter — Code Formatter

A deterministic code formatter for C, C++, Java, Kotlin, JSON/JSON5, CSS,
YAML, TOML, XML, HTML5, JavaScript, TypeScript, and Python 3, implementing
the [CodingStyle.md](../STYLE.md) style guide. No AI, no AST — tokenizer
plus recursive descent on bounded token slices.

---

## Requirements

- Java 8 or later (runtime)
- Java 8 SDK + `make` (build)

---

## Building

```sh
cd util/CodingStyle.md/formatter
make
```

Produces `code-formatter-1.00.jar` in the `formatter/` directory (version
number matches `VERSION` in the Makefile — replace `1.00` with your built
version in the commands below).

---

## Usage

### Single file

```sh
java -jar code-formatter-1.00.jar File.java
java -jar code-formatter-1.00.jar src/Utils.c
java -jar code-formatter-1.00.jar include/Module.h
```

Language is detected from the file extension (`.c` → C, `.h` → C, `.cpp`/`.cc`/`.cxx` → C++,
`.java` → Java, `.kt`/`.kts` → Kotlin, `.json`/`.json5` → JSON/JSON5, `.css` → CSS,
`.yaml`/`.yml` → YAML, `.toml` → TOML, `.xml` → XML, `.html`/`.htm` → HTML5,
`.js`/`.jsx`/`.mjs`/`.cjs` → JavaScript, `.ts`/`.tsx` → TypeScript, `.py` → Python 3). `.jsx`/
`.tsx` are dispatched to the same JS/TS pipeline as `.js`/`.ts` — no JSX/TSX-syntax-aware
formatting exists (out of scope per `STYLE_JS_TS.md`), so a `.jsx`/`.tsx` file is only safe to
run through the formatter if it contains no actual JSX tag syntax.

For a file with a non-standard extension (e.g. `.java.in`, `.txt`, no extension at all),
override detection with `--lang`:

```sh
java -jar code-formatter-1.00.jar --lang java Template.java.in
java -jar code-formatter-1.00.jar --lang cpp Module.inc
```

`--lang` accepts exactly one of `c`, `cpp`, `java`, `kotlin`, `json`, `json5`, `css`, `yaml`,
`toml`, `xml`, `html5`, `js`, `ts`, `python3`, and applies to every file given on that command
line (mixing file types with a single forced `--lang` in one invocation isn't supported — run
the formatter once per language instead).
Without `--lang`, a file whose extension can't be recognized is an error. `--lang` also works
with server mode (below) — the client sends the chosen language to the server, which uses it
in place of its own extension-based guess for that request.

### Output modes

```sh
java -jar code-formatter-1.00.jar --in-place File.java  # in-place edit (overwrites File.java)
java -jar code-formatter-1.00.jar --diff File.java      # print unified diff, do not edit
java -jar code-formatter-1.00.jar --check File.java     # exit 1 if file would change (CI)
java -jar code-formatter-1.00.jar --out DIR File.java   # write to DIR/File.java instead
java -jar code-formatter-1.00.jar --out DIR \           # write to DIR/sub/File.java, preserving
    --preserve-tree --root ROOT sub/File.java           # ROOT-relative subdirectory structure

```

One of `--in-place`, `--diff`, `--check`, or `--out DIR` is required — there is no implicit
default output mode, specifically so that running the formatter without thinking about output
mode can never silently overwrite an input file. Use `--in-place` when you deliberately want
that behavior.

`--out DIR` alone flattens every input file to its basename under `DIR` — two input files
with the same name in different source directories will collide and overwrite each other.
`--preserve-tree` (requires both `--out DIR` and `--root DIR`) avoids this by rebasing each
input file's path relative to `--root DIR` onto `--out DIR` instead, preserving subdirectory
structure. `--root DIR` has no effect and is a usage error without `--preserve-tree`; an
input file that doesn't resolve under `--root DIR` is a per-file error. Opt-in only — omitting
`--preserve-tree` keeps the original flattening behavior unchanged.

### Server mode (faster for batch)

```sh
java -jar code-formatter-1.00.jar --server       # start server in background
java -jar code-formatter-1.00.jar File.java      # auto-connects to running server
java -jar code-formatter-1.00.jar --stop         # stop server
```

The server amortizes JVM startup across a batch of files. If no server is running,
the JAR falls back to standalone mode silently. `--server` is idempotent — safe to
call from a Makefile target even if the server is already running.

**After SIGKILL or manual lockfile deletion:** the next invocation detects the stale
lockfile (PID no longer alive), cleans it up, and starts fresh automatically.

### Disabling formatting for part or all of a file

To keep a region of code exactly as written — untouched by any formatting rule — wrap it
in a marker comment pair:

```java
//% JXM_CFMT_DIS
… code left byte-for-byte untouched …
//% JXM_CFMT_ENA
```

The block-comment form works the same way, useful where a line comment isn't available
(e.g. inside a macro or a single-line context):

```c
/*% JXM_CFMT_DIS */
… code left byte-for-byte untouched …
/*% JXM_CFMT_ENA */
```

Formatting resumes immediately after `JXM_CFMT_ENA`. The marker comments themselves are
never modified or removed by a later run, so this is safe to leave in checked-in source
permanently, and idempotent across repeated formatting passes.

To disable formatting for an entire file from the command line (as if `JXM_CFMT_DIS`
were present at the very top), without editing the file itself:

```sh
java -jar code-formatter-1.00.jar --format-off File.java
```

The file stays completely untouched unless it contains its own `JXM_CFMT_ENA` marker,
at which point formatting resumes from that point onward, same as the in-code marker
pair above.

### In-file config overrides

A single source file can override any per-file config key for itself only, via a
top-of-file `JXM_CFMT_CFG` directive:

```java
//% JXM_CFMT_CFG indent-size=2;line-length=80
```

The block-comment form works the same way:

```c
/*% JXM_CFMT_CFG indent-size=2;line-length=80 */
```

Entries are `key=value` pairs separated by `;`. Any key valid in a
`.jxmake-code-formatter` file is valid here, except `server-port` (a process-wide
property that cannot be set per-file). Values set by this directive are the
highest-priority config layer — they override the project's `.jxmake-code-formatter`
files, environment variables, CLI flags, and (in server mode) the request's own inline
query-param config, all for the same key.

The directive must appear before the first non-comment/non-blank line of the file —
i.e. somewhere within the leading run of blank lines and whole comments at the top of
the file, not necessarily on line 1 itself. It must also be its own separate comment:
it is **not** recognized when merged into another comment's prose, such as inside a
copyright header. For example, this is *not* detected:

```c
/*
 * Copyright (C) 2024 Example Corp.
 * JXM_CFMT_CFG indent-size=2
 */
```

Only one `JXM_CFMT_CFG` directive is allowed per file; a second occurrence is a hard
error (nonzero exit, no output), not a "last one wins" merge.

### Makefile integration

```makefile
fmt:
    java -jar util/CodingStyle.md/formatter/code-formatter-1.00.jar --server
    java -jar util/CodingStyle.md/formatter/code-formatter-1.00.jar $(SRCS)

fmt-check:
    java -jar util/CodingStyle.md/formatter/code-formatter-1.00.jar --check $(SRCS)
```

---

## Configuration

The formatter reads configuration from the following sources, in order of increasing
precedence (later sources override earlier ones):

1. Built-in class defaults
2. `~/.config/jxmake-code-formatter/config` — user global config
3. `JXMAKE_CODE_FORMATTER_*` environment variables
4. `.jxmake-code-formatter` in the project root — per-project config (commit this to the repo)
5. `.jxmake-code-formatter` in the source subdirectory — inherits from parent, overrides specific keys
6. CLI flags / server request's inline query-param config
7. A file's own `JXM_CFMT_CFG` directive (see [In-file config overrides](#in-file-config-overrides)) — always wins

### Config file format

```properties
# ── Structural constants ──────────────────────────────────────────────────────
line-length                      = 100
indent-size                      = 4
indent-style                     = spaces      # spaces | tabs | auto
server-port                      = 17173

# ── Behavior ──────────────────────────────────────────────────────────────────
line-endings                     = lf          # lf | crlf | preserve
normalize-comment-start-case     = on          # on | off
normalize-comment-end-period     = on          # on | off
comment-normalization-classifier = off         # off | on
closing-comment-min-lines        = 5

# ── C/C++ ─────────────────────────────────────────────────────────────────────
header-guard-rename              = off         # off | on (warn only by default)
format-macros                    = off         # off | on

# ── Java ──────────────────────────────────────────────────────────────────────
java-import-order                = java, com, org, other, local, static
java-import-sort                 = on
java-import-depth                = 2
java-import-blank-lines          = 1

# ── Kotlin ────────────────────────────────────────────────────────────────────
kotlin-import-order              = kotlin, java, android, com, org, other, local
kotlin-import-sort               = on
kotlin-import-depth              = 2
kotlin-import-blank-lines        = 1

# ── JS/TS ─────────────────────────────────────────────────────────────────────
js-import-order                  = builtin, third-party, local
js-import-sort                   = on
js-import-blank-lines            = 1

# ── Python 3 ──────────────────────────────────────────────────────────────────
python-import-sort               = on
python-import-blank-lines        = 1

# ── AI-assist (GRU) ───────────────────────────────────────────────────────────
gru-classifier                   = off         # off | on (opt-in, no trained model ships yet)
gru-weights-path                 = target/gru/weights.json
```

### `.jxmake-code-formatter` inheritance

A `.jxmake-code-formatter` file in a subdirectory fully inherits from the nearest `.jxmake-code-formatter`
found in any parent directory, overriding only the keys it explicitly specifies.

### Java import groups and module detection

`java-import-depth = 2` means the top 2 components of the file's own `package`
declaration define the local prefix (e.g. `package com.mycompany.audio` →
local prefix is `com.mycompany`).

For pre-Java-9 projects without module descriptors, the same `package` declaration
is used — no module system involvement required.

### Kotlin import groups

`kotlin-import-depth` works the same way as `java-import-depth`, reading the local
prefix from the file's own `package` declaration. There is no `static` group — Kotlin
has no `import static` keyword; a companion-object-member or top-level-function import
uses the exact same `import a.b.c` syntax as any other import, so "this is a static
import" isn't lexically detectable the way Java's `import static` is. A leading `kotlin`
group (for `kotlin.*` stdlib imports) takes its place in the default order. Aliased
imports (`import foo.Bar as Baz`) and wildcard imports sort/group by their original
qualified name, not the alias.

### JS/TS import groups and local-import classification

There is no `js-import-depth` key — unlike Java/Kotlin's `package`-declaration-derived
local prefix, JS/TS has no equivalent package concept to read a prefix from. Local-import
detection is purely syntactic: an import specifier is classified `local` iff it starts
with `./` or `../`. See "Known Limitations" below for what this means for
bundler/tsconfig path-mapped absolute imports.

### C-preprocessor directives in Java source

Some projects run `.java` files through a C-macro preprocessor (e.g. PCPP-style)
before compilation, as a poor man's template mechanism. `#define`/`#ifdef`/`#endif`/etc.
lines are recognized and passed through untouched, same as in `.c`/`.cpp`/`.h` files,
including a directive placed immediately before a method definition.

---

## Line Endings

| Setting | Behavior |
|---|---|
| `lf` (default) | Normalize all line endings to `\n` on write — recommended for GitHub / GitLab |
| `crlf` | Normalize to `\r\n` — for Windows-native projects |
| `preserve` | Detect dominant line ending in each file and keep it |

---

## Windows Notes

The formatter runs on Windows (JVM is cross-platform, `/` paths are normalized by
the JVM internally). The following limitations apply on Windows:

- `~/.config/jxmake-code-formatter/` resolves via `System.getProperty("user.home")` which on
  Windows typically maps to `C:\Users\<name>` — this works correctly
- Environment variable overrides (`JXMAKE_CODE_FORMATTER_*`) work normally
- Some path overrides in `.jxmake-code-formatter` using Unix-style absolute paths
  (e.g. `/etc/...`) will not resolve correctly on Windows — use relative paths
  or `user.home`-relative paths in config files intended for cross-platform use
- Server mode PID liveness check uses `ProcessHandle` (Java 9+) which is
  cross-platform — no `kill -0` shell-out is used

---

## AI Workflow for Tier-3 Aesthetic Decisions

The JAR handles all deterministic formatting. A small class of aesthetic decisions
— function argument list layout and non-standard getter/setter grouping — can be
handled by a capable AI model (Claude Sonnet / Opus, GPT-4o, etc.) in a separate
pass. The JAR may be extended with built-in AI assist in a future version.

See [`../README.txt`](../README.txt) for the full workflow, including two pass modes:
- **Layout judgment pass** (recommended) — post-JAR, targets only aesthetic decisions
- **Full-file pass** — for files not yet processed by the JAR

---

## Style Guide Reference

- [`../STYLE.md`](../STYLE.md) — common rules (all languages)
- [`../STYLE_C_CPP.md`](../STYLE_C_CPP.md) — C and C++ extensions
- [`../STYLE_JAVA.md`](../STYLE_JAVA.md) — Java extensions
- [`../STYLE_KOTLIN.md`](../STYLE_KOTLIN.md) — Kotlin extensions (baseline)
- [`FORMATTER_DISCUSSION.md`](FORMATTER_DISCUSSION.md) — design rationale
- [`STATE.md`](STATE.md) — implementation progress tracker (all phases, including
  Java 17+/C++20+ support and the call/declaration line-breaking work)

Newer-language-construct support:
- [`../STYLE_JAVA17.md`](../STYLE_JAVA17.md) — Java 17+ (`record`, sealed
  classes, switch expressions, text blocks, pattern matching)
- [`../STYLE_CPP20.md`](../STYLE_CPP20.md) — C++17/20/23 (structured bindings,
  concepts/`requires`, `consteval`/`constinit`)
- [`../STYLE_KOTLIN2.md`](../STYLE_KOTLIN2.md) — Kotlin 2.0/2.1 constructs
  (guard conditions, `data object`), read after `STYLE_KOTLIN.md`
- [`../STYLE_CPP26.md`](../STYLE_CPP26.md) — C++26 rule coverage (lands
  directly in the existing C/C++ pipeline, no separate language identity)
- [`../STYLE_DATA_FORMATS.md`](../STYLE_DATA_FORMATS.md) — JSON/JSON5/CSS/YAML/
  TOML/XML/HTML5 (all implemented, including HTML5's `<script>` dispatch to
  JS/TS)
- [`../STYLE_JS_TS.md`](../STYLE_JS_TS.md) — JavaScript/TypeScript (implemented;
  JSX/TSX are out of scope, see Usage above)
- [`../STYLE_PYTHON3.md`](../STYLE_PYTHON3.md) — Python 3

---

## Server Wire Protocol

The server (`--server`) exposes two plain-HTTP endpoints on `localhost:<port>` (default
`17173`, override with `--port N` / `server-port` config key):

- `POST /format?path=<abs-path>&lang=<c|cpp|java|kotlin|json|json5|css|yaml|toml|xml|html5|js|ts|python3>[&format-off=true][&<config-key>=<value>...]`
  — request body is the file's raw content (UTF-8); response body is the formatted content
  (UTF-8), HTTP 200. The `lang` parameter is required by the client and always takes priority
  over any extension-based guess the server could make from `path` — this is how `--lang`
  (above) reaches the server, and is also why the server itself never needs its own
  `--lang`-style flag. An unrecognized `lang` value gets HTTP 400 with a plain text error body.
  Any other failure (e.g. a malformed file) gets HTTP 500.
  - **Inline config.** Any config key from the formatter's config-file property set (e.g.
    `indent-size`, `line-length`, `java-import-order`) may be passed as its own query
    parameter, letting a client (e.g. a browser formatting a pasted/in-memory snippet with no
    real file on disk) hand the server a complete config directly in the request instead of
    relying on a `.jxmake-code-formatter` file near `path`. Any query key other than
    `path`/`lang`/`format-off` that isn't a recognized config key gets HTTP 400 (a typo'd key
    fails loudly rather than being silently ignored).
  - **`path` becomes optional** exactly when at least one inline config parameter and `lang`
    are both present in the request; it stays required otherwise. A request missing `path`
    with no inline config (or no `lang`) gets HTTP 400.
  - File-based (`.jxmake-code-formatter` near `path`) and inline config are mutually exclusive
    by client contract, but if a request supplies both, **inline config wins** — it is applied
    on top of, and overrides, any file-based config for the same keys.
- `POST /shutdown` — asks the server to stop; responds `200 shutting down` immediately, then
  terminates the process shortly after (deleting its lockfile first). Used by `--stop`.

Clients are expected to auto-detect a running server via the lockfile at
`~/.config/jxmake-code-formatter/server.lock` (PID on line 1, port on line 2) rather than
talking to a hardcoded port — see `ServerMode.findRunningServerPort()`. This is the same
protocol the bundled CLI's own auto-connect logic (`--standalone` to disable it) uses; a
third-party client only needs to speak this HTTP protocol, not link against the JAR.

---

## Known Limitations

- **General scope‑depth reindentation is not supported for curly-family languages (C/C++/
  Java/Kotlin/JavaScript/TypeScript).** These languages preserve the original whitespace by
  default (only narrow, targeted passes, reindent anything — see below); therefore,
  **jxmake‑code‑formatter** cannot properly format badly indented machine‑generated code,
  obfuscated code, or code copy‑pasted from emails and forums that lost their indentation.
  This does **not** apply to JSON/JSON5/CSS/YAML/TOML/XML/HTML5, which parse into a real
  tree and print indentation fresh from structural nesting depth regardless of source
  formatting (however it **does** apply to embedded JavaScript in HTML5).

- **Non-idempotent reindent on internally-inconsistent generated source, for any pass using
  a relative-delta technique.** Two known call sites share this root cause:
  `SwitchRule.applyNonInlineCaseIndent` (`case` bodies) and
  `ScopePipeline.applyDeclarationsPass` (declarations) — each shifts a block's lines by one
  delta computed from a single reference line rather than deriving each line's target from
  its own brace-nesting depth, which assumes the block's original indentation was internally
  consistent. On JavaCC/ANTLR-style generated sources whose *own* output has inconsistent
  per-line indentation within a single block (a generator quirk, not something realistic
  hand-written code exhibits), one reformat pass can land a line one indent level off from
  its true target, and reformatting that output a second time (an idempotency check: format
  once, then format the result again and compare) converges it to a different value than
  either the first pass or the original source — i.e. two formatting passes are not
  guaranteed to produce byte-identical output on such input. Observed once each in the
  `javaparser/javaparser`
  real-code-testing candidate (`ASTParser.java`, its JavaCC-generated parser) and the local
  dogfood tree (`tool/JSONEncoderLite.java`), out of thousands of real-world files tested
  across all candidates; no other file in either tree or any other tested candidate exhibits
  it. A real fix would need both passes to derive each line's target from structural depth
  rather than a relative delta from one reference line — a nontrivial rework with regression
  risk to existing behavior, not planned unless a broader pattern of real-world impact
  emerges.

- **JS/TS import ordering (§15) misclassifies bundler/tsconfig path-mapped absolute
  imports as third-party.** Local-import detection is syntactic only: an import specifier
  is `local` iff it starts with `./` or `../`. A genuinely first-party import resolved via
  a bundler or tsconfig `baseUrl`/`paths` mechanism (e.g. `import { Widget } from
  "components/Widget"` pointing at the project's own source tree, not a `node_modules`
  package) is classified `third-party` instead, since this formatter has no config concept
  for a project's source root and no `tsconfig.json`/bundler-config resolution logic. This
  is a known, accepted simplification (RDD_KEY_195) — no source-root config key is planned.

- **Java/Kotlin comment-separator alignment (§15) can false-positive on ordinary prose that
  incidentally matches its recognition shape.** `alignCommentSeparators` treats a run of 2+
  adjacent trailing `//` comments as a separator-alignment group when each comment contains
  exactly one non-alphanumeric character flanked by spaces on both sides (RDD_KEY_50's
  deliberately broad, user-chosen recognition rule). Ordinary prose can coincidentally
  satisfy this shape (e.g. `// The @ can be used...` next to `// => the last @ is...`),
  causing the formatter to pad spaces mid-comment to "align" the coincidental character —
  not idempotent, and can push a line over `line-length`. Found during the
  `jenkinsci/jenkins` Java dogfood session; a follow-up attempt to narrow the qualifying-
  character set to a fixed allowlist was tried and reverted (it broke existing Kotlin
  doc-comment fixtures that rely on the current rule's "2+ qualifying candidates
  disqualifies" behavior to stay unrecognized — see `RDD_KEY_201`). No fix is currently
  planned; narrowing this rule safely needs a new design decision, not just a smaller
  character set. Affects roughly 1 file per ~2000 real-world files tested so far.

---

## License

Apache License, Version 2.0 — see [LICENSE.txt](LICENSE.txt)

> **Note:** The LICENSE file year should read `2022-2026`, matching the JxMake project origin year.
