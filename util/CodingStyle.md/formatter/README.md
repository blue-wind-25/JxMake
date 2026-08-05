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

Produces `code-formatter-1.0.0.jar` in the `formatter/` directory (version
number matches `VERSION` in the Makefile — replace `1.0.0` with your built
version in the commands below).

---

## Usage

### Single file

```sh
java -jar code-formatter-1.0.0.jar File.java
java -jar code-formatter-1.0.0.jar src/Utils.c
java -jar code-formatter-1.0.0.jar include/Module.h
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
java -jar code-formatter-1.0.0.jar --lang java Template.java.in
java -jar code-formatter-1.0.0.jar --lang cpp Module.inc
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
java -jar code-formatter-1.0.0.jar --in-place File.java  # in-place edit (overwrites File.java)
java -jar code-formatter-1.0.0.jar --diff File.java      # print unified diff, do not edit
java -jar code-formatter-1.0.0.jar --check File.java     # exit 1 if file would change (CI)
java -jar code-formatter-1.0.0.jar --out DIR File.java   # write to DIR/File.java instead
java -jar code-formatter-1.0.0.jar --out DIR \           # write to DIR/sub/File.java, preserving
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
java -jar code-formatter-1.0.0.jar --server       # start server in background
java -jar code-formatter-1.0.0.jar File.java      # auto-connects to running server
java -jar code-formatter-1.0.0.jar --stop         # stop server
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

For HTML/XML, a multi-line `<!-- -->` comment (its raw interior contains a newline —
e.g. a copyright-header block) is now automatically preserved byte-for-byte, same as
PI/CDATA content, with no marker needed — `JXM_CFMT_DIS`/`ENA` remains available and
useful for other cases (e.g. freezing a region of tags/attributes).

To disable formatting for an entire file from the command line (as if `JXM_CFMT_DIS`
were present at the very top), without editing the file itself:

```sh
java -jar code-formatter-1.0.0.jar --format-off File.java
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

### GDR in-file directive

When `curly-general-scope-reindent = on` (see Configuration below), a source file can
disable and re-enable the GDR reindentation pre-pass for specific regions via a
`JXM_CFMT_GDR` directive — distinct from, and unrelated in syntax to, `JXM_CFMT_CFG`
above:

```java
//% JXM_CFMT_GDR 0
//% JXM_CFMT_GDR 1
```

The block-comment form works the same way:

```c
/*% JXM_CFMT_GDR 0 */
/*% JXM_CFMT_GDR 1 */
```

`0` disables GDR reindentation for the region following the directive; `1` re-enables
it. Unlike `JXM_CFMT_CFG`, this directive is not limited to a top-of-file preamble — it
can appear anywhere, any number of times, to bracket exactly the region that needs
manual/inconsistent indentation preserved (e.g. a hand-indented block deliberately kept
shallower than its ancestors) without a whole-file config flip. The line the directive
itself appears on is excluded from reindentation the same as the region it controls.

It is a **flat toggle, not a nesting counter**: a single `1` always re-enables
regardless of how many `0`s preceded it, and a redundant `0` while already disabled is a
harmless no-op. An unmatched trailing `0` at end of file is not an error — there is
nothing left in that file to reindent either way, and the next file (if any) starts
fresh from its own config, unaffected by a prior file's unclosed directive. The
directive parses without error even when `curly-general-scope-reindent` is off
globally, so a file can be prepared for GDR ahead of a project-wide flag flip.

### Makefile integration

```makefile
fmt:
    java -jar util/CodingStyle.md/formatter/code-formatter-1.0.0.jar --server
    java -jar util/CodingStyle.md/formatter/code-formatter-1.0.0.jar $(SRCS)

fmt-check:
    java -jar util/CodingStyle.md/formatter/code-formatter-1.0.0.jar --check $(SRCS)
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

**Server mode note on tiers 2/3:** server mode is `localhost`-only (see "Server Wire Protocol"
below) — the server process reads its own `~/.config/jxmake-code-formatter/config` (tier 2) and
its own process environment (tier 3), not the CLI-invoking client's. For tier 2 this is
transparent as long as client and server run as the same OS user on the same machine (the
intended, and only supported, deployment shape), since the server re-reads that file fresh from
disk on every request. Tier 3 is different: a JVM's environment variables are fixed at process
start, so a long-running server's env-var tier can go stale relative to the client's *current*
shell environment if the client's env changed after the server was launched. To avoid this, the
bundled CLI's `delegateToServer` path forwards its own live `JXMAKE_CODE_FORMATTER_*` env-var
snapshot to the server as inline query-param overrides (tier 6) on every delegated request, so
the effective result matches what a fresh standalone run in the client's own environment would
have produced, regardless of how long the server has been running or what it originally started
with.

### Config file format

```properties
# ── Structural constants ──────────────────────────────────────────────────────
server-port                            = 17173

line-length                            = 100
indent-size                            = 4
indent-style                           = spaces      # spaces | tabs | auto

# ── Behavior ──────────────────────────────────────────────────────────────────
line-endings                           = lf          # lf | crlf | preserve

normalize-comment-start-case           = on          # on | off
normalize-comment-end-period           = on          # on | off
comment-normalization-classifier       = on          # on | off
closing-comment-min-lines              = 5

curly-general-scope-reindent           = off         # off | on
curly-general-scope-reindent-multipass = off         # off | on, only takes effect when the above is also on

# ── C/C++ ─────────────────────────────────────────────────────────────────────
header-guard-rename                    = off         # off | on
format-macros                          = off         # off | on

# ── Java ──────────────────────────────────────────────────────────────────────
java-import-order                      = java, com, org, other, local, static
java-import-sort                       = on
java-import-depth                      = 2
java-import-blank-lines                = 1

# ── Kotlin ────────────────────────────────────────────────────────────────────
kotlin-import-order                    = kotlin, java, android, com, org, other, local
kotlin-import-sort                     = on
kotlin-import-depth                    = 2
kotlin-import-blank-lines              = 1

# ── JS/TS ─────────────────────────────────────────────────────────────────────
js-import-order                        = builtin, third-party, local
js-import-sort                         = on
js-import-blank-lines                  = 1

# ── Python 3 ──────────────────────────────────────────────────────────────────
python-import-sort                     = on
python-import-blank-lines              = 1

# ── HTML5 ─────────────────────────────────────────────────────────────────────
html5-tc-gap-level                     = 0           # 0 | 1 | 2 | 3 | 4, cumulative

# ── AI-assist (GRU) ───────────────────────────────────────────────────────────
gru-classifier                         = on          # on | off
gru-weights-path                       =             # empty = derive from program dir (code-formatter-ai-assist-weights.json)
```

### Comment classifier (GRU)

`gru-classifier = on` (default) resolves the rule-based comment classifier's
`ABSTAIN` cases — an ambiguous leading keyword (e.g. does `return` start a
sentence or introduce a real code reference?) or an ambiguous trailing
period — via a small purpose-trained bidirectional GRU
(`code-formatter-ai-assist-weights.json`, ~425k parameters, loaded once at
startup from `gru-weights-path`, or the program directory if unset). The
GRU only ever runs after the deterministic rules already fired and
abstained; it never overrides a rule-based `YES`/`NO`. If the weights file
is missing or unreadable, the classifier fails safe to `ABSTAIN`
(equivalent to `gru-classifier = off` for that comment) — formatting is
never blocked on it.

An `ABSTAIN` on a leading-keyword or trailing-period comment leaves that
comment untouched, so whether the GRU is actually active changes real
output: with it active, more ambiguous comments get resolved to a
capitalize-first-letter/strip-trailing-period `YES` than with it inactive.
`make test`'s fixtures assume the weights file is reachable (deployed next
to `$(JAR_FILE)`, e.g. by `_test_serial`'s auto-copy from the repo-root
`code-formatter-ai-assist-weights.json` into `$(BUILD_DIR)` — see the
Makefile); running against a jar with no weights file next to it silently
falls back to the less-aggressive `ABSTAIN`-only behavior instead of
failing, so a missing-weights setup won't show up as a test failure on its
own.

`abstainThreshold = 0.7` is baked into the shipped weights file (not a
separate config key): the GRU itself abstains below this softmax
confidence cutoff rather than forcing a low-confidence guess. See
[`DESIGN_NOTES.md`](DESIGN_NOTES.md) for why `0.7` was chosen over a lower
threshold.

### General scope-depth reindentation (GDR) (`curly-general-scope-reindent`)

`curly-general-scope-reindent` (default `off`) opts curly-family languages
(C/C++/Java/Kotlin/JavaScript/TypeScript, including embedded JavaScript inside HTML5
`<script>` tags) into an isolated pre-pass, run ahead of the normal formatting pipeline, that
derives each line's indentation from absolute brace/paren/bracket nesting depth instead of the
default behavior (preserve original whitespace except where a specific recognized rewrite
requires touching it). This lets badly indented machine-generated code, obfuscated code, or code
copy-pasted from emails/forums that lost its indentation — including fully flush-left
(zero-indentation) input — get reindented correctly, which the base pipeline's relative-delta
reindentation cannot do on its own. Does **not** apply to JSON/JSON5/CSS/YAML/TOML/XML/HTML5,
which already parse into a real tree and print indentation fresh from structural nesting depth
regardless of source formatting, independent of this key.

**Known gap and its workaround (`curly-general-scope-reindent-multipass`):** the pre-pass
measures each line's depth from the source *before* the pipeline's own brace-placement pass
runs. A source line joining a clause onto the closing brace, one-true-brace style —

```java
if (a) {
    foo();
} else if (b) {   // joined onto the previous closing brace, one physical line
    bar();
}
```

— gets its depth measured for that single joined line as it exists at pre-pass time; if this
formatter's own brace-placement pass later needs to split it into separate `}` / `else if (b) {`
lines to match its Allman style, the newly split-out line never gets its own pre-pass target,
and the result can differ between a first format and reformatting that already-formatted output
again (non-idempotent). Reindented output is correct once the input already uses
one-clause-per-line bracing; the gap is specific to sources using the joined `} else`-style form.
Setting `curly-general-scope-reindent-multipass = on` (default `off`, only takes effect when
`curly-general-scope-reindent` is also `on`) resolves this: instead of one pre-pass-then-pipeline
pass, it runs a bounded convergence loop (pre-pass + full pipeline, repeated and compared cycle
to cycle, until two consecutive cycles are byte-identical, capped at a safety limit) — at the
cost of extra formatting passes for any file that enables it.

### HTML5 tree-construction gap levels (`html5-tc-gap-level`)

`html5-tc-gap-level` (default `0`, integer `0`-`4`) enables an increasing, cumulative set of
narrow, formatter-appropriate approximations of specific HTML5 spec tree-construction
algorithms. Each level's code path is guarded independently on `config.html5TcGapLevel() >= N`
and only has effect when `lang.isHtml5` is already true (no separate opt-in needed beyond this
key). Default `0`: current, strictly preserve-as-written HTML5 parsing, unchanged.

- **Level `1`** — implicit `<body>` start-tag insertion: a document with no explicit `<body>`
  start tag anywhere gets one synthesized around the first non-head content and everything after
  it — the first fabricated-node path in this otherwise preserve-as-written formatter. Known
  gap: the synthesis point is a simplified heuristic (first non-whitespace/non-comment/
  non-DOCTYPE/non-`<head>` sibling), not a true "head insertion mode closed" transition — can
  misfire on documents with no explicit `<head>` either (wraps `<meta>`/`<title>`/`<script>` into
  the synthesized `<body>` too early), and fires even on bare markup *fragments* (no full
  document structure at all), wrapping them in a `<body>` too.
- **Level `2`** (+ level 1) — foster-parenting: content the spec requires relocated out of an
  open `<table>` and inserted immediately before it, rather than nested inside where the source
  text placed it. Known gap: `isInTableInsertionMode()` is implemented as a single-level "direct
  child of an open `<table>`" check, not a full ancestor scan (a full ancestor scan incorrectly
  re-evaluates a fostered element's own already-relocated descendants).
- **Level `3`** (+ levels 1-2) — misnested `<form>` reconstruction inside `<template>`: a
  single-slot "form element pointer," scoped per `<template>` boundary via plain Java
  call-stack local-variable save/restore. No known gap.
- **Level `4`** (+ levels 1-3) — adoption agency algorithm: reconstructs a misnested formatting
  element (e.g. `<b>`/`<i>`, as in `<b>1<i>2</b>3</i>`) as a next-sibling clone once its
  misnesting ancestor's own close tag is matched. Known gap: tracks only the single
  most-recently-orphaned formatting element at a time, not the spec's full "list of active
  formatting elements" + "furthest block" + "bookmark" algorithm. Correctly handles the classic
  single-level misnesting case; a second, simultaneous misnesting under the same ancestor is not
  reconstructed — only the innermost/most-recently-orphaned one is (the plain field gets
  overwritten, not queued).

See [`DESIGN_NOTES.md`](DESIGN_NOTES.md) for the design history behind these approximations.

### Comment capitalization exceptions (`normalize-comment-start-case`)

`normalize-comment-start-case = on` (default) skips capitalizing any HTML/XML comment whose
entire (trimmed) body is a single word with no interior whitespace, e.g.:

```html
<!--more-->
```

This is deliberately broad (any single word, not just a known allow-list), because a single-word
HTML/XML comment is often a content-splitting directive a third-party tool parses literally and
must never be rewritten (the motivating case: WordPress's magic comments like `<!--more-->`,
`<!--nextpage-->`, `<!--noteaser-->`). A real-corpus check across multiple real-world HTML5
dogfood trees found zero genuine one-word English prose comments that this rule would wrongly
leave lowercase, so it was accepted as-is rather than built as a maintained allow-list. See
"Known Limitations" below for the accepted risk this leaves.

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
- [`../STYLE_CPP20.md`](../STYLE_CPP20.md) — C++17/20/23 (structured bindings,
  concepts/`requires`, `consteval`/`constinit`)
- [`../STYLE_CPP26.md`](../STYLE_CPP26.md) — C++26 rule coverage (lands
  directly in the existing C/C++ pipeline, no separate language identity)
- [`../STYLE_JAVA.md`](../STYLE_JAVA.md) — Java extensions
- [`../STYLE_JAVA17.md`](../STYLE_JAVA17.md) — Java 17+ (`record`, sealed
  classes, switch expressions, text blocks, pattern matching)
- [`../STYLE_KOTLIN.md`](../STYLE_KOTLIN.md) — Kotlin extensions (baseline)
- [`../STYLE_KOTLIN2.md`](../STYLE_KOTLIN2.md) — Kotlin 2.0/2.1 constructs
  (guard conditions, `data object`), read after `STYLE_KOTLIN.md`
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

Grouped by which language family each limitation affects (curly-brace family, then
tag-based/markup family, then indent-based family), then by effect size within each group.

### Curly-brace family (C/C++/Java/Kotlin/JS/TS)

1. **General scope-depth reindentation is opt-in, not the default, and has a known joined-brace
   non-idempotency gap.** See "Config file format" → [General scope-depth reindentation
   (GDR)](#general-scope-depth-reindentation-gdr-curly-general-scope-reindent) above for what the
   `curly-general-scope-reindent` key does, its scope, the joined-one-true-brace-style gap, and
   the `curly-general-scope-reindent-multipass` workaround.

2. **Multi-line-call/condition wrap decision can flap across repeated formatting passes,
   affecting C/C++/Java/Kotlin/JS/TS.** Whether a small call or condition nested inside a
   longer expression stays on one line or gets wrapped is, in one code path, decided by
   measuring the length of its entire surrounding source line rather than just the
   candidate's own rendered content. For a short nested call sitting inside a long
   enclosing line, this can wrap it unnecessarily. Because the measurement is based on the
   surrounding line's physical layout rather than the candidate's own logical content, the
   same input can format differently the first time versus if the already-formatted output
   is fed back in and formatted again — i.e. formatting is not always idempotent for this
   narrow shape. A fix was attempted (measuring from the candidate's own position instead
   of its enclosing line's start) but caused numerous unrelated formatting regressions
   elsewhere, because a candidate's line-prefix content (e.g. `return `, `if (`, `val x =
   `) legitimately needs to count toward the line-length limit in most other cases. The fix
   was reverted rather than landed narrowly or accompanied by a wide, unreviewed
   regression sweep. This is a known, currently-unresolved gap — no workaround exists
   short of avoiding deeply nested short calls inside very long lines.

3. **JS/TS braceless if/else collapse can still be non-idempotent when the rescuing
   call-wrap doesn't shrink the line enough.** Before collapsing a braceless `if`/`else`
   body onto one line, the formatter checks whether a later call-wrapping pass could
   still rescue an over-limit result (`hasBreakableCall`) — but that check only asks
   "does a rescuable call exist," not "will wrapping it actually bring the line under
   the length limit." For a collapsed candidate long enough that wrapping its one
   breakable call's arguments doesn't shrink the joined line far enough (e.g. a long
   string-concatenation chain where only one of several calls gets wrapped, or wrapping
   it still leaves the line long), the collapse proceeds anyway, and a second formatting
   pass can disagree with the first — i.e. formatting is not always idempotent for this
   narrow shape. This is a known, accepted limitation of the cheap heuristic used (a full
   two-pass simulation of the later call-wrap pass would close this gap but is a bigger,
   separately-scoped lift). No workaround exists short of manually keeping such lines
   braced.

4. **JS/TS import ordering (§15) misclassifies bundler/tsconfig path-mapped absolute
   imports as third-party.** Local-import detection is syntactic only: an import specifier
   is `local` iff it starts with `./` or `../`. A genuinely first-party import resolved via
   a bundler or tsconfig `baseUrl`/`paths` mechanism (e.g. `import { Widget } from
   "components/Widget"` pointing at the project's own source tree, not a `node_modules`
   package) is classified `third-party` instead, since this formatter has no config concept
   for a project's source root and no `tsconfig.json`/bundler-config resolution logic. This
   is a known, accepted simplification — no source-root config key is planned.

5. **Non-idempotent reindent on internally-inconsistent generated source, for any pass using
   a relative-delta technique.** Two known call sites share this root cause:
   `SwitchRule.applyNonInlineCaseIndent` (`case` bodies) and
   `ScopePipeline.applyDeclarationsPass` (declarations) — each shifts a block's lines by one
   delta computed from a single reference line rather than deriving each line's target from
   its own brace-nesting depth, which assumes the block's original indentation was internally
   consistent. On generated sources (e.g. JavaCC/ANTLR-style parser output) whose *own* output
   has inconsistent per-line indentation within a single block — a generator quirk, not
   something realistic hand-written code exhibits — one reformat pass can land a line one
   indent level off from its true target. For example, a `switch` body where a generator's own
   output already mixes indent widths within one `case`:

   ```java
   switch (kind) {
       case TOKEN:
           consume();
         emit(kind);  // one column short of `consume()` above, in the same case body
           break;
   }
   ```

   reformatting that output a second time (an idempotency check: format once, then format the
   result again and compare) converges it to a different value than either the first pass or
   the original source — i.e. two formatting passes are not guaranteed to produce
   byte-identical output on such input. Observed rarely in real-code testing, out of thousands
   of real-world files tested across many candidates. A real fix would need both passes to
   derive each line's target from structural depth rather than a relative delta from one
   reference line — a nontrivial rework with regression risk to existing behavior, not planned
   unless a broader pattern of real-world impact emerges.

### Tag-based family (XML/HTML5)

6. **HTML5 deep tree-construction gap coverage (`html5-tc-gap-level`) is a narrow, documented
   approximation of each corresponding HTML5 spec algorithm, not a full spec-faithful
   implementation.** See "Config file format" → [HTML5 tree-construction gap
   levels](#html5-tree-construction-gap-levels-html5-tc-gap-level) above for what the key is,
   what each level enables, and each level's own documented gap.

7. **HTML/XML single-word comments are never capitalized, even when they're genuine one-word
   prose.** See "Config file format" → [Comment capitalization
   exceptions](#comment-capitalization-exceptions-normalize-comment-start-case) above for what
   `normalize-comment-start-case` skips and why (e.g. `<!--more-->`-style directive comments).
   The accepted risk: a codebase with a real one-word prose comment (e.g. `<!--fixme-->`,
   `<!--todo-->`) will keep it lowercase instead of capitalizing it — a false negative, not a
   false positive (no comment is ever wrongly rewritten by this rule, only possibly left
   as-is).

### Indent-based family (Python 3)

No known limitations currently documented for indent-based languages.

---

## Design Notes

See [`DESIGN_NOTES.md`](DESIGN_NOTES.md) for the rationale behind specific formatter
decisions (why certain config defaults/approximations were chosen).

---

## License

Apache License, Version 2.0 — see [LICENSE.txt](LICENSE.txt)
