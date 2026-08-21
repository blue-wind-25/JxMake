# jxmake-code-formatter — Code Formatter

A deterministic code formatter for C, C++, Java, Kotlin, JSON/JSON5, CSS,
YAML, TOML, XML, HTML5, JavaScript, TypeScript, Python 3, E-INI (Extended
INI), JxMakeFile, Makefile, Bash, and PowerShell. Implements the common
rules in [`STYLE.md`](../STYLE.md) plus each language's own derivative
style guide (e.g. C/C++ also follows `STYLE_C_CPP.md`, JS/TS follows
`STYLE_JS_TS.md`), E-INI/Makefile/Bash/PowerShell follow
[`STYLE_TOOLING.md`](../STYLE_TOOLING.md), and JxMakeFile follows
[`STYLE_JXMAKE.md`](../STYLE_JXMAKE.md) — see [Style Guide
Reference](#style-guide-reference) below for the full per-language list.
No AI, no AST — tokenizer plus recursive descent on bounded token slices.

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

Produces `code-formatter-1.0.1.jar` in the `formatter/` directory (version
number matches `VERSION` in the Makefile — replace `1.0.1` with your built
version in the commands below).

---

## Usage

### Single file

```sh
java -jar code-formatter-1.0.1.jar File.java
java -jar code-formatter-1.0.1.jar src/Utils.c
java -jar code-formatter-1.0.1.jar include/Module.h
```

Language is detected from the file extension (`.c` → C, `.h` → C, `.cpp`/`.cc`/`.cxx` → C++,
`.java` → Java, `.kt`/`.kts` → Kotlin, `.json`/`.json5` → JSON/JSON5, `.css` → CSS,
`.yaml`/`.yml` → YAML, `.toml` → TOML, `.xml` → XML, `.html`/`.htm` → HTML5,
`.js`/`.jsx`/`.mjs`/`.cjs` → JavaScript, `.ts`/`.tsx` → TypeScript, `.py` → Python 3,
`.ini` → E-INI (Extended INI), `JxMakeFile`/`.jxm` → JxMakeFile,
`Makefile`/`GNUmakefile`/`.mk` → Makefile, `.sh`/`.bash` → Bash, `.ps1`/`.psm1` → PowerShell).
Makefile detection is also basename-based (`Makefile`, `GNUmakefile`) for extensionless Make
files.

`.jsx`/`.tsx` are dispatched to the same JS/TS pipeline as `.js`/`.ts`. A boundary-finding
pre-pass detects JSX/TSX tag trees and preserves them byte-for-byte as opaque, unformatted spans,
so a file containing real JSX tag syntax is safe to run through the formatter — the JSX/TSX
portions round-trip unchanged while surrounding plain JS/TS gets normal formatting, except that any
top-level `{...}` expression hole — whether in a JSX tree's *children* (e.g.
`{items.map(x => <li>{x}</li>)}`) or in an opening tag's *attribute value* (e.g.
`onClick={handler}`, `className={cond ? "a" : "b"}`) — is recursively formatted through the same
JS/TS pipeline and spliced back in; spread attributes (`{...props}`) are left untouched, as are any
other bytes of the span outside a hole.

The pre-pass runs unconditionally on `.jsx`/`.tsx` **and** on plain `.js`/`.mjs`/`.cjs`
(mirrors Prettier's own default — real-world `.js` files with embedded JSX, e.g. older
Create-React-App projects, are common enough that gating on extension alone caused genuine content
corruption). `.ts` deliberately stays gated off by
default — a `.ts` file's legacy `<Type>expr` angle-bracket cast syntax collides with a JSX open
tag, the same reasoning `tsc`/Prettier use to gate `.ts` separately from `.tsx` — but a `.ts` file
that genuinely embeds JSX can opt in per-file with the `jsx-in-ts` [`JXM_CFMT_CFG`
directive](#in-file-config-overrides) (or the equivalent CLI flag/env var/config-file key).

Beyond children-position hole recursion and one specific attribute-wrap rule, JSX/TSX-syntax-*aware*
reformatting (e.g. reflowing/reordering attributes, HTML5-tree-construction-aware child parsing)
does not exist yet — see `STYLE_JS_TS.md`.

For a file with a non-standard extension (e.g. `.java.in`, `.txt`, no extension at all),
override detection with `--lang`:

```sh
java -jar code-formatter-1.0.1.jar --lang java Template.java.in
java -jar code-formatter-1.0.1.jar --lang cpp Module.inc
```

`--lang` accepts exactly one of `c`, `cpp`, `java`, `kotlin`, `json`, `json5`, `css`, `yaml`,
`toml`, `xml`, `html5`, `js`, `ts`, `python3`, `eini`, `jxmake`, `makefile`, `bash`, `powershell`, and applies to
every file given on that command line (mixing file types with a single forced `--lang` in one
invocation isn't supported — run the formatter once per language instead). Without `--lang`, a
file whose extension can't be recognized is an error.

`--lang` also works with server mode (below) — the client sends the chosen language to the server,
which uses it in place of its own extension-based guess for that request. There is no separate
`jsx`/`tsx` `--lang` value — both are covered by `js`/`ts`.

However, whether the JSX/TSX boundary-finding pre-pass runs is decided independently of `--lang`,
purely from the actual filename's extension (`.jsx`/`.tsx` vs. plain `.js`/`.ts`) plus, for `.ts`
only, the `jsx-in-ts` opt-in described above; forcing `--lang js`/`--lang ts` on a file whose name
doesn't match one of those rules selects the JS/TS pipeline but does not itself enable JSX detection.

For a per-file override instead of a per-invocation one (so mixed-language file lists and
templated sources like `.java.in`/`.java.inc` don't need a separate invocation each), see the
`--lang` form of the in-file [`JXM_CFMT_CFG` directive](#in-file-config-overrides) below.

### Output modes

```sh
java -jar code-formatter-1.0.1.jar --in-place File.java  # in-place edit (overwrites File.java)
java -jar code-formatter-1.0.1.jar --diff File.java      # print unified diff, do not edit
java -jar code-formatter-1.0.1.jar --check File.java     # exit 1 if file would change (CI)
java -jar code-formatter-1.0.1.jar --out DIR File.java   # write to DIR/File.java instead
java -jar code-formatter-1.0.1.jar --out DIR \           # write to DIR/sub/File.java, preserving
    --preserve-tree --root ROOT sub/File.java            # ROOT-relative subdirectory structure

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
java -jar code-formatter-1.0.1.jar --server       # start server in background
java -jar code-formatter-1.0.1.jar File.java      # auto-connects to running server
java -jar code-formatter-1.0.1.jar --stop         # stop server
```

The server amortizes JVM startup across a batch of files. If no server is running,
the JAR falls back to standalone mode silently. `--server` is idempotent — safe to
call from a Makefile target even if the server is already running.

**After SIGKILL or manual lockfile deletion:** the next invocation detects the stale
lockfile (PID no longer alive), cleans it up, and starts fresh automatically.

**Concurrency for many small requests (`server-concurrency` / `client-read-ahead`):**
by default the server handles one request at a time (`server-concurrency = 1`, today's
behavior, unchanged) and the CLI's one-by-one batch-invocation path (many files passed
to a single `java -jar ... file1 file2 ...` invocation, each delegated to the server as
its own HTTP request) sends them strictly one at a time (`client-read-ahead = 1`). This
matters for a scenario like an editor plugin issuing one format request per
keystroke-triggered save, or many independent files formatted in one invocation — not
for the already-near-optimal single-call batch path. Both settings are opt-in and
independent of each other:

```sh
JXMAKE_CODE_FORMATTER_SERVER_CONCURRENCY=4 java -jar code-formatter-1.0.1.jar --server
JXMAKE_CODE_FORMATTER_CLIENT_READ_AHEAD=6  java -jar code-formatter-1.0.1.jar file1.java file2.java ...
```

or via a config file:

```properties
server-concurrency = 4
client-read-ahead  = 6
```

`server-concurrency` controls the thread-pool size the server's own HTTP executor uses;
when raising it, `Runtime.getRuntime().availableProcessors()` (the number of CPU cores
available to the JVM) is a reasonable value to start from.

`client-read-ahead` controls how many requests the CLI keeps in flight at once instead of
waiting for each response before sending the next; it is read and applied by whichever process
is doing the delegating, independently of what `server-concurrency` the server it's talking to
is running with (you may not control that server's own setting).

As a tuning guideline only — not an enforced relationship, and each is a genuinely independent
config value — a `client-read-ahead` of roughly `server-concurrency + 2` keeps a couple of
requests queued beyond what the server can immediately work on in parallel, so its thread pool
stays continuously fed instead of idling between bursts.

Both are process/server-invocation-scoped settings, same category as `server-port` — see the
In-file config overrides section below for why they cannot be set per-file via `JXM_CFMT_CFG`.

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
java -jar code-formatter-1.0.1.jar --format-off File.java
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
`.jxmake-code-formatter` file is valid here, except the process/server-invocation-scoped
keys `server-port`, `server-concurrency`, and `client-read-ahead` (none can be set
per-file). Values set by this directive are the
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

**Language override (`--lang`):** alongside ordinary `key=value` config entries, the
directive also accepts a `--lang` pseudo-key, matching the CLI flag of the same name
and accepting the same values (`c`, `cpp`, `java`, `kotlin`, `json`, `json5`, `css`,
`yaml`, `toml`, `xml`, `html5`, `js`, `ts`, `python3`, `eini`, `jxmake`, `makefile`, `bash`,
`powershell`):

```java
//% JXM_CFMT_CFG --lang=cpp

/*% JXM_CFMT_CFG --lang=cpp */
```

This is a per-file alternative to passing `--lang` on the command line (or the
server's `lang` query parameter) every invocation — useful for a file whose extension
either can't be recognized at all (a templated source file such as `Template.java.in`
or `Module.java.inc`) or is recognized but resolves to the wrong language for that
particular file (e.g. a `.h` file that is actually C++, not C — see "Known
Limitations" below). Like every other `JXM_CFMT_CFG` entry, `--lang` is the
highest-priority layer: it wins over an explicit CLI `--lang`/server `lang` param as
well as extension-based inference. It can be combined with ordinary config entries in
the same directive, separated by `;` (order doesn't matter):

```java
//% JXM_CFMT_CFG --lang=cpp;indent-size=2
```

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
    java -jar util/CodingStyle.md/formatter/code-formatter-1.0.1.jar --server
    java -jar util/CodingStyle.md/formatter/code-formatter-1.0.1.jar $(SRCS)

fmt-check:
    java -jar util/CodingStyle.md/formatter/code-formatter-1.0.1.jar --check $(SRCS)
```

---

## E-INI (Extended INI)

E-INI (`--lang eini`) is a simple INI-like key-value config format with grouping, detected from
the `.ini` extension (any other extension needs an explicit `--lang eini` or the
in-file `--lang=eini` directive). It is a narrow, beautification-only pipeline
(see [`../STYLE_TOOLING.md`](../STYLE_TOOLING.md) §4) — anything not covered by
the five rules below is left byte-identical, with no general reindent/rewrap
fallback.

Recognized syntax:

- **Key-value lines**: `key = value` or `key: value` — whichever separator
  (`=` or `:`) appears first outside quotes on the line. An unquoted key has its
  leading/trailing whitespace stripped and internal whitespace runs collapsed to
  a single space; a quoted key (`'...'`/`"..."`) keeps its interior whitespace
  exactly as written. A value has leading/trailing whitespace stripped unless
  quoted, in which case its interior is preserved verbatim (never collapsed).
- **Group headers**: `[name]`, `{name}`, `<name>`, `(name)`, or a bare/plain
  line with no wrapping marker at all (any non-comment line with no key-value
  separator). Same trim/collapse-outside-quotes rule as an unquoted key.
- **Comments**: a line (or trailing portion of a line) starting with `#`, `;`,
  `@`, or `//` outside any quotes, or three of the same quote character in a
  row (`'''`/`"""`) outside any quoting context. `%` is reserved for this
  formatter's own `JXM_CFMT_CFG` in-file directive and is never treated as a
  comment marker.

Formatting applied:

1. **Separator alignment** — a contiguous run of key-value lines has its
   `=`/`:` separators padded into one column; a blank line or any
   non-key-value line breaks the group, and the next group starts a fresh
   column.
2. **Indentation snapping** — each line's leading indentation is rounded up to
   the nearest `indent-size` multiple (E-INI has no braces/nesting of its own
   to derive a depth from).
3. **Line-continuation alignment** — a `\`-continued value's wrapped lines
   align under the first line's value-start column (same mechanism as
   Makefile's continuation-line alignment).
4. **Comment normalization** — first-letter capitalization and stripping a
   sole trailing `.`, applied unconditionally (no tool-name skip list).
5. **No long-line breaking** — values/headers/comments always stay on one
   line, regardless of length.

Example:

```ini
[Server Config]
host = localhost
port : 8080
name = 'John Doe'

; Comment about the timeout
timeout = 30
retries = 3
```

---

## JxMakeFile

JxMakeFile (`--lang jxmake`) is JxMake's own build-scripting language, detected from
the literal basename `JxMakeFile` or the `.jxm` extension. It is a narrow,
beautification-only pipeline (see [`../STYLE_JXMAKE.md`](../STYLE_JXMAKE.md)) —
anything not covered by the four rules below is left byte-identical, with no
general reindent/rewrap fallback: multiline strings (`[[" ... "]]`), raw/single/
double-quoted string interiors, `@`-shell-exec command text, compiler directives
(`:::...`), and macro-use (`.$name`) all pass through untouched.

Formatting applied:

1. **Line-comment normalization** — `#...` line comments are chain-grouped and
   normalized via the same first-letter-capitalization / sole-trailing-period
   rules as every other language. A standalone `#` comment chain takes the
   indentation depth of the next non-blank, non-comment code line that follows
   it — the comment attaches to what it comments on, so it tracks that line's
   depth under rule 2's forced reindent even when the comment's own line
   didn't itself change block nesting (falling back to the depth already in
   effect at that point if no code line follows, e.g. separated by a blank
   line or at end of file/block). Block comments (`(* ... *)`) are not
   normalized — their interior is byte-identical — but shift as one unit by the
   opening `(*` line's indent delta, preserving any hand-alignment inside.
2. **Forced reindentation** by block-keyword nesting depth: `function`/
   `endfunction`, `target`/`endtarget`, `if`/`endif` (block form only — a
   one-liner `if condition : stmt` is a single leaf statement, not a nesting
   level), `for`/`endfor`, `foreach`/`endforeach`, `while`/`endwhile`,
   `do`/`whilst`, `repeat`/`until`, `loop`/`endloop`, `.macro`/`.endmacro`.
   `elif`/`else` render at the same depth as their owning `if`. Within one
   `if`-`elif`-...-`else` chain, if (and only if) every branch inlines its body
   on the same physical line via a trailing `;` (a normal pattern in this
   codebase's own `.jxm` library files), the `if`/`elif`/`else` keyword itself
   is right-justified to the widest keyword used in the chain, added on top of
   the normal depth indent, so the condition/body lines up on every branch — if
   even one branch instead puts its body on separate following lines, the whole
   chain stays at plain depth indent. A `;`-separated multi-statement line
   reindents as a whole to the depth of its first statement only.
3. **Backslash continuation-line alignment** — for assignment statements, a
   `\`-continued value's wrapped lines align under the value's start column
   (same mechanism as E-INI/Makefile continuation alignment); for anything
   else, `(depth + 1) * indent-size`.
4. **Assignment-operator alignment** — a contiguous run of same-depth,
   single-statement assignment lines (direct `[local] [const] var-name
   assign-op term+`, or indirect `^var-name assign-op term+`) has its
   `local`/`const`/var-name fields each padded to the group's widest
   occurrence of that field (the operator and value are never padded). A group
   breaks on a blank line, a depth change, a comment line, or any line
   containing `;`.

Example:

```
local CC     = 'gcc'
local CFLAGS = '-O2 -Wall'

function build(target)
    if target == 'release'
        CC = 'gcc'
    else
        CC = 'cc'
    endif
endfunction
```

---

## Configuration

The formatter reads configuration from the following sources, numbered here in order of
increasing precedence (later sources override earlier ones):

1. Built-in class defaults
2. `~/.config/jxmake-code-formatter/config` — user global config
3. `JXMAKE_CODE_FORMATTER_*` environment variables
4. `.jxmake-code-formatter` in the project root — per-project config (commit this to the repo)
5. `.jxmake-code-formatter` in the source subdirectory — inherits from parent, overrides specific keys
6. CLI flags / server request's inline query-param config
7. A file's own `JXM_CFMT_CFG` directive (see [In-file config overrides](#in-file-config-overrides)) — always wins,
   including its `--lang` pseudo-key, which overrides the CLI `--lang` flag / server `lang` param too

**Server mode note on sources 2/3:** server mode is `localhost`-only (see "Server Wire Protocol"
below) — the server process reads its own `~/.config/jxmake-code-formatter/config` (source 2) and
its own process environment (source 3), not the CLI-invoking client's.

For source 2 this is transparent as long as client and server run as the same OS user on the
same machine (the intended, and only supported, deployment shape), since the server re-reads
that file fresh from disk on every request.

Source 3 is different: a JVM's environment variables are fixed at process start, so a
long-running server's env-var source can go stale relative to the client's *current* shell
environment if the client's env changed after the server was launched.

To avoid this, the bundled CLI's `delegateToServer` path forwards its own live
`JXMAKE_CODE_FORMATTER_*` env-var snapshot to the server as inline query-param overrides
(source 6) on every delegated request, so the effective result matches what a fresh standalone
run in the client's own environment would have produced, regardless of how long the server has been running
or what it originally started with.

**Server mode note on `indent-style = auto`:** resolved server-side too, the same way as
standalone mode (below) — the server samples the *target file's own directory tree* on disk
(`path` query param), not the client's. A no-path inline-content request has no directory to
sample and falls back to the default indent style.

### Config file format

```properties
# ── Server configurations ─────────────────────────────────────────────────────
server-port                            = 17173
server-concurrency                     = 1           # server-only, see "Server mode" below
client-read-ahead                      = 1           # client-only, see "Server mode" below

# ── Structural constants ──────────────────────────────────────────────────────
line-length                            = 100
line-length-with-comment               = 120         # code+comment fits-check width -- curly-brace family only
indent-size                            = 4
indent-style                           = spaces      # spaces | tabs | auto

# ── Behavior ──────────────────────────────────────────────────────────────────
line-endings                           = lf          # lf | crlf | preserve

normalize-comment-start-case           = on          # on | off
normalize-comment-start-case-multiline = off         # off | on
normalize-comment-end-period           = on          # on | off
comment-normalization-classifier       = on          # on | off
closing-comment-min-lines              = 5

curly-general-scope-reindent           = off         # off | on
curly-general-scope-reindent-multipass = off         # off | on, only takes effect when the above is also on
curly-general-scope-reindent-postpass  = off         # off | on, only takes effect when the base flag is also on

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
jsx-in-ts                              = off         # off | on -- per-file opt-in for JSX embedded in a plain `.ts` file (`.tsx`/`.jsx`/`.js`/`.mjs`/`.cjs` detect JSX unconditionally and are unaffected by this key), see below

# ── Python 3 ──────────────────────────────────────────────────────────────────
python-import-sort                     = on
python-import-blank-lines              = 1

# ── HTML5 ─────────────────────────────────────────────────────────────────────
html5-tc-gap-level                     = 0           # 0 | 1 | 2 | 3 | 4, cumulative

# ── AI-assist (GRU) ───────────────────────────────────────────────────────────
gru-classifier                         = on          # on | off
gru-weights-path                       =             # empty = derive from program dir (code-formatter-ai-assist-weights.json)
```

**`indent-size` / `indent-style` semantics.** `indent-size` means different
things depending on `indent-style`:
- `indent-style = spaces`: `indent-size` is the number of space characters
  per indent level.
- `indent-style = tabs`: one indent level is always exactly **one tab
  character**, regardless of `indent-size` — `indent-size` instead sets the
  tab's *visual width* (columns) used when measuring a line against
  `line-length`/`line-length-with-comment` or computing alignment-column
  positions, since a tab's rendered width isn't 1 column.
- `indent-style = auto`: detects `spaces` vs. `tabs` by sampling up to 10
  sibling C/C++/Java-family source files (by extension) in the target
  file's directory tree, walking up to the nearest `.jxmake-code-formatter`/
  `.git`/`.hg` boundary (or the user's home directory) if none are found
  locally; each sampled file votes based on its first indented line's
  leading whitespace character, and the majority vote wins (a tie, or no
  files found, falls back to the default indent style). `indent-size` plays
  no role in this detection — it only applies afterward, once a concrete
  style is chosen. In server mode, this detection runs against the *target
  file's own path* on the server's filesystem (see "Server mode note on
  `indent-style = auto`" above), not the client's.

**Per-language `line-length` / `line-length-with-comment` usage.** Not every
language has a line-length-driven wrap decision, and among the ones that do,
only the curly-brace family has a decision point that separately measures a
trailing same-line comment's width, so only that family reads
`line-length-with-comment`:

| Language(s) | `line-length` | `line-length-with-comment` |
|---|---|---|
| C, C++, Java, Kotlin, JS, TS (curly-brace family) | used (code-only wrap decisions) | used (wrap decisions on a line carrying a trailing same-line comment) |
| JSON, JSON5 | used (array/object tight-vs-loose decision) | not used -- a node with a trailing comment is excluded from tight-candidacy before any width is measured |
| CSS | not used -- `STYLE_DATA_FORMATS.md` §3 defines no line-length-driven wrap rule for CSS at all | not used |
| YAML | used (flow-vs-block conversion) | not used -- the flow-fit check only ever measures a node's own flow-tight rendering, never a trailing comment |
| TOML | not used -- `STYLE_DATA_FORMATS.md` §6.3/§6.4 define array/inline-table (un)wrapping by content type and grammar constraint, not by length | not used |
| XML, HTML5 | used (tight-element/attribute-wrap decision) | not used -- width is measured before a trailing comment is appended, so the comment never enters the measured span |
| Python3 | used (signature/case/single-statement-body join decisions) | not used -- a line carrying a trailing comment is conservatively left unjoined rather than measured, so there is no comment-inclusive decision point to plug into |
| E-INI, Makefile, Bash, PowerShell | not used -- `STYLE_TOOLING.md` scopes these four to a fixed, narrow beautification list with "no general reindentation/re-wrapping fallback" of any kind | not used |
| JxMakeFile | not used -- `STYLE_JXMAKE.md` scopes it to a fixed, narrow beautification list with the same "no general reindentation/re-wrapping fallback" as the tooling family above | not used |

This reflects genuinely different architectures per language, not gaps to be
filled in: CSS/TOML/E-INI/JxMakeFile/Makefile/Bash/PowerShell have no line-length-driven wrap
rule specified at all, and JSON5/YAML/XML/HTML5/Python3 each structurally
exclude a trailing comment from their own wrap decision (skip the decision
entirely, or measure before the comment is appended) rather than folding it
into the measured width the way the curly-brace family's shared pipeline
does. A future session adding a genuine comment-inclusive wrap decision to
any of these should reuse `line-length-with-comment` rather than inventing a
new key.

**`--lang` vs. `jsx-in-ts`.** These two look similar (both influence JS/TS-family
language handling) but sit at different points in the precedence list above. `--lang` is *not*
a config-file key at all — it's a CLI flag / server `lang` query param, with a matching
`JXM_CFMT_CFG` pseudo-key as its only in-file form; it has no env var and
cannot appear in `.jxmake-code-formatter`. `jsx-in-ts` (JS/TS group above) is
an ordinary config key like any other in this list, so it works through every
source: built-in default (`off`), `~/.config/jxmake-code-formatter/config`,
`JXMAKE_CODE_FORMATTER_JSX_IN_TS`, `.jxmake-code-formatter`, CLI flag / server
query param, and `JXM_CFMT_CFG` — see "Configuration" above for the full
precedence order.

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
Running the formatter against a copy of the jar with no weights file next to
it (and no `gru-weights-path` override) silently falls back to the
less-aggressive `ABSTAIN`-only behavior instead of failing — a missing
weights file degrades gracefully rather than blocking formatting.

`abstainThreshold = 0.76` is baked into the shipped weights file (not a
separate config key): the GRU itself abstains below this softmax
confidence cutoff rather than forcing a low-confidence guess. See
[`DESIGN_NOTES.md`](DESIGN_NOTES.md) for why `0.76` was chosen over a lower
threshold.

### Multi-sentence comment capitalization (`normalize-comment-start-case-multiline`)

`normalize-comment-start-case-multiline` (default `off`) extends
`normalize-comment-start-case` beyond a comment group's very first word: with it on, every line
comment group (the same consecutive-`//`/`#`-line grouping already used elsewhere) has its lines
joined into one combined text, and every internal `.`/`!`/`?` + whitespace + lowercase-letter
boundary found in that text — not just the group's first word — is offered to the exact same
capitalization decision (mechanical/linear/GRU classifier stack when
`comment-normalization-classifier` is on, or the plain keyword-exception list when it's off) already
used for the first word. A candidate that the decision doesn't clearly approve is left untouched,
same as today's first-word-only behavior when it abstains or says no. Applies to both the
curly-brace family (C/C++/Java/Kotlin/JS/TS) and the `#`-comment tooling family
(E-INI/JxMakeFile/Makefile/Bash/PowerShell) as well as YAML/TOML.

Before offering a candidate boundary to that decision at all, a narrow mechanical pre-filter
rejects shapes that are structurally never a real sentence start regardless of what the
classifier might say: a run of 2+ punctuation marks (`...`, `?!`), a standalone symbol not
attached to a preceding word (`` `! is` ``), a preceding word that's a single letter or a known
abbreviation (`e.g.`, `i.e.`, `vs.`, `etc.`, `cf.`, `al.`), a following word with an internal
uppercase letter (a camelCase/dotted code identifier, e.g. `doSomething`), and a following word
immediately followed by `:` with no trailing space (a URL scheme or directive comment, e.g.
`https:`, `ftp:`, `tslint:`).

**Known risk when this key is on:** the classifier stack is reused completely as-is — it was
trained to judge "is this leading word safe to capitalize," not "is this a sentence boundary" —
so internal boundaries it wasn't trained on carry explicit out-of-distribution risk beyond what
the mechanical pre-filter above catches. One concrete gap found during real-code validation: the
keyword-exception list (which excludes words like `import` from capitalization) is only consulted
in the `comment-normalization-classifier = off` path — with the classifier on, this key inherits
the same limitation `normalize-comment-start-case` already has for a comment's first word, so a
mid-comment line of commented-out code such as

```
// TODO: re-enable this test
// import './rxjs/rxjs.spec';
```

can have its `import` line capitalized to `// Import './rxjs/rxjs.spec';` if the classifier judges
it a plausible sentence start. Leave this key off for codebases where commented-out code inside
multi-line comment groups is common, or accept spot-checking after enabling it.

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

**`curly-general-scope-reindent-postpass`:** default `off`, only takes effect when
`curly-general-scope-reindent` is also `on`. Runs one extra reindent pass directly on the final
formatted output, with no further formatting pass after it. This pass leaves any line that's
part of a wrapped, multi-line call, condition, or lambda/scope-function chain completely alone —
it never re-derives that line's indentation, including a closing bracket that mixes a paren-close
and a brace-close on one line, a `}.apply { ... }`-style reopened chain, or a wrapped
expression-bodied declaration's continuation and its own nested closing braces — and only
re-targets plain block-structure indentation that sits outside any such wrap. This pass has been
validated against a large real-world Kotlin codebase (roughly 190 files) with no remaining
instances of pushing an already-correctly-aligned wrapped continuation line, or its closer, to the
wrong depth; it stays opt-in (off by default) since it is still a narrower, more aggressive
reformat than the base pass, not because it is considered unproven.

**When to reach for it:** the base pass (with `-multipass`) already reindents ordinary
block-structure drift present in the *original* source, so leave `-postpass` off for routine use.
Its distinguishing trait is running strictly after every other formatting decision in a file is
already final — brace placement, line-wrapping, declaration alignment, all of it — so it can catch
a plain block-depth mistake (a declaration or closing brace at the wrong depth relative to its own
siblings) no matter which step produced it, including one that the base pass's earlier vantage
point can't see because it runs before the rest of the formatter has finished. Reach for it as an
extra, one-off consistency check on a specific file you already suspect has a leftover
block-indentation slip somewhere, not as a routine addition. Because it never touches a wrapped
call/condition/chain continuation line, it's safe to turn on surgically for just the file (or
region) that needs the check rather than as a project-wide setting. Combine it with the GDR
in-file directive above to keep a specific region completely untouched either way — for example, a
block generated by another tool that intentionally uses a different indentation style, sitting
next to a plain hand-written method you want reindented:

```java
/*% JXM_CFMT_CFG curly-general-scope-reindent=on;curly-general-scope-reindent-postpass=on */

public class ImportedFromLegacyTool {

    //% JXM_CFMT_GDR 0
    // Left exactly as the old tool generated it -- excluded from the cleanup below.
    public void generatedSection() {
      doSomething();
          doSomethingElse();
    }
    //% JXM_CFMT_GDR 1

    public void handMergedSection() {
            // A merge-conflict resolution left this block one level too deep; the postpass
            // corrects it without touching anything else in the file.
            fixThisIndentation();
    }

}
```

### HTML5 tree-construction gap levels (`html5-tc-gap-level`)

`html5-tc-gap-level` (default `0`, integer `0`-`4`) enables an increasing, cumulative set of
narrow, formatter-appropriate approximations of specific HTML5 spec tree-construction
algorithms. Each level's code path is guarded independently on `config.html5TcGapLevel() >= N`
and only has effect when `lang.isHtml5` is already true (no separate opt-in needed beyond this
key). Default `0`: current, strictly preserve-as-written HTML5 parsing, unchanged.

- **Level `1`** — implicit `<body>` start-tag insertion: a document with no explicit `<body>`
  start tag anywhere gets one synthesized around the first non-head content and everything after
  it — the first fabricated-node path in this otherwise preserve-as-written formatter. Tracks a
  real "head insertion mode closed" transition (`headInsertionModeClosed`) rather than a sibling
  heuristic, so `<meta>`/`<title>`/`<script>` siblings are correctly left out of the synthesized
  `<body>` even on a document with no explicit `<head>` element at all. Known gap: still fires
  even on bare markup *fragments* (no full document structure at all), wrapping them in a `<body>`
  too.
- **Level `2`** (+ level 1) — foster-parenting: content the spec requires relocated out of an
  open `<table>` and inserted immediately before it, rather than nested inside where the source
  text placed it. Known gap: the "are we inside an open `<table>`" check only looks at the
  direct parent, not the full ancestor chain (a full ancestor scan would incorrectly re-evaluate
  a fostered element's own already-relocated descendants).
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

"Tier" here classifies how confidently a formatting decision can be automated: Tier-1 is plain
deterministic rule application; Tier-2 is also fully built into the JAR, but resolves a handful
of ambiguous comment-capitalization cases via the on-device linear-classifier + GRU stack (see
"Comment classifier (GRU)" above); Tier-3 is the small remaining class of aesthetic decisions —
function argument list layout and non-standard getter/setter grouping — genuinely left for a
separate pass with a capable external AI model (Claude Sonnet / Opus, GPT-4o, etc.), since the
JAR doesn't attempt them itself. The JAR may be extended with its own built-in AI for Tier-3
decisions too in a future version.

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
  TOML/XML/HTML5, including HTML5's `<script>` dispatch to JS/TS
- [`../STYLE_JS_TS.md`](../STYLE_JS_TS.md) — JavaScript/TypeScript (JSX/TSX tag
  trees are preserved byte-for-byte, not JSX-aware-reformatted, see Usage above)
- [`../STYLE_PYTHON3.md`](../STYLE_PYTHON3.md) — Python 3
- [`../STYLE_TOOLING.md`](../STYLE_TOOLING.md) — E-INI, Makefile, Bash, and
  PowerShell (narrow beautification-only rule lists — recipe lines,
  quoting/heredocs/here-strings/comments are left byte-identical outside
  each language's fixed transforms)
- [`../STYLE_JXMAKE.md`](../STYLE_JXMAKE.md) — JxMakeFile, JxMake's own
  build-scripting language (narrow beautification-only rule list — see
  [JxMakeFile](#jxmakefile) above)

---

## Server Wire Protocol

The server (`--server`) exposes two plain-HTTP endpoints on `localhost:<port>` (default
`17173`, override with `--port N` / `server-port` config key):

- `POST /format?path=<abs-path>&lang=<c|cpp|java|kotlin|json|json5|css|yaml|toml|xml|html5|js|ts|python3|eini|jxmake|makefile|bash|powershell>[&format-off=true][&<config-key>=<value>...]`
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
- `GET /properties` — no request parameters, empty request body. Response body (HTTP 200) is a
  JSON array of section-group objects, grouped and ordered exactly like the `### Config file
  format` list below (`Structural constants`, `Behavior`, `C/C++`, `Java`, `Kotlin`, `JS/TS`,
  `Python 3`, `HTML5`, `AI-assist (GRU)`): `[{"group": "<section-name>",
  "properties": [{"key": "<config-key>", "default": "<default-value>", "allowedValues":
  ["choice1", "choice2", ...] | null}, ...]}, ...]`. `default` is always the value's raw string
  form (as it would appear in a config file/query param/env var), even for integer/boolean keys.
  `allowedValues` is a fixed list for `on`/`off` boolean keys and the few enum-like keys
  (`indent-style`, `line-endings`); `null` for free-form values (integers, paths, comma-separated
  import-order lists). Lets tooling introspect the formatter's config surface without parsing this
  README. Always reflects the formatter's own actual live configuration, so it cannot drift from
  actual behavior the way a hand-maintained doc list can.

Clients are expected to auto-detect a running server via the lockfile at
`~/.config/jxmake-code-formatter/server.lock` (PID on line 1, port on line 2) rather than
talking to a hardcoded port. This is the same
protocol the bundled CLI's own auto-connect logic (`--standalone` to disable it) uses; a
third-party client only needs to speak this HTTP protocol, not link against the JAR.

---

## Known Limitations

Grouped by which language family each limitation affects (curly-brace family, then
tag-based/markup family, then indent-based Python 3, then E-INI, then JxMakeFile, then
build/dev-tooling scripts, then AI-assist), then by effect size within each group. A family with
no currently documented limitations is still given its own heading with an explicit "no known
limitations" note rather than being omitted, whenever that itself is useful information — e.g. a
narrow, fixed-rule-scope language where anything outside its rules is left untouched by design,
not a gap that happens to be empty today.

Each item below follows the same shape: a title, a short description, a code example of the
limitation, a workaround where one exists, and any further explanation of the *why* that doesn't
fit in the first four.

### Curly-brace family (C/C++/Java/Kotlin/JS/TS)

#### 1. General scope-depth reindentation is opt-in and has a known joined-brace non-idempotency gap

`curly-general-scope-reindent` (default `off`) is required to correctly reindent badly-indented
or flush-left source — see "Config file format" → [General scope-depth reindentation
(GDR)](#general-scope-depth-reindentation-gdr-curly-general-scope-reindent) above. With it off,
minified/compiled JS with one-liner function bodies is not idempotent:

```js
function foo(a,b){if(a){return b;}else{return a;}}
```

**Workaround:** turn on both `curly-general-scope-reindent` and
`curly-general-scope-reindent-multipass` — the example above then round-trips cleanly. See
[General scope-depth reindentation (GDR)](#general-scope-depth-reindentation-gdr-curly-general-scope-reindent)
above for what the joined-one-true-brace-style gap is and why multipass fixes it.

A related but distinct gap (found 2026-08-19, diagnosed 2026-08-20): a function *expression*
passed as a call argument, with a body already spanning multiple physical lines, was **not**
reformatted the same way an identical body at declaration/statement position already is:

```js
items.map( function (x) {
    doA(x);
    doB(x);
    return x;
} );
```

The root cause is structural: the formatter's scope-recursion pass only treats a `{`/`}` pair as a
formattable child scope when it sits at the top level of its enclosing statement; a
function-expression body passed inside a call's own parentheses sits one level deeper, so it was
never recognized as a scope at all and none of the normal brace-placement/statement-splitting
logic ever ran on it — unlike a top-level `function foo(a,b){...}` *declaration*, which is a
top-level scope and goes through that logic normally.

**Fixed for JavaScript/TypeScript** (2026-08-20): a call-argument function-expression body like
the example above is now recursed into and reformatted the same as an identical body at
declaration/assignment-statement position. A body that is still entirely on one physical line
(e.g. `items.map(function (x) { doA(x); doB(x); return x; });` with no line breaks in the body)
is deliberately left as-is, matching the same one-liner-stays-compact behavior an identical body
already has at declaration position. A function expression with an explicit TypeScript return-type
annotation immediately before its body (`function (x): number { ... }`) is not yet recognized by
this fix and keeps the old, unrecursed-into behavior. One further known limitation even where the
fix applies: only declaration statements inside the reformatted body get their indentation
normalized; a plain non-declaration statement line (e.g. a fluent method-chain continuation) keeps
whatever indentation it already had in the source.

**Fixed for C/C++ and Kotlin** (2026-08-20): the same structural shape — a lambda passed as a call
argument in C/C++ (`std::sort(v.begin(), v.end(), [](int a, int b) {...});`), or a lambda literal
passed as a non-trailing call argument in Kotlin (`bar(1, { x -> ... });`) — is now recursed into
and reformatted the same way. Kotlin's ordinary trailing-lambda call syntax
(`items.forEach { ... }`) was never affected by this gap in the first place. Same narrowing and
same known limitation as the JS/TS fix above: a C/C++ lambda with a trailing `mutable`/`noexcept`/
return-type specifier between its parameter list and its body is not yet recognized, and only
declaration statements inside a reformatted body get their indentation normalized — a plain
non-declaration statement line keeps whatever indentation it already had.

**Fixed for Java** (2026-08-21): an anonymous class passed as a call argument
(`run(new Runnable() { public void run() { ... } });`) is now recursed into and reformatted the
same way as the other languages' equivalents above — each statement in its body onto its own line,
with correct depth-based indentation and brace placement, however many anonymous classes or
members are nested inside one another. (A separate bug that could make this shape's body collapse
onto one garbled line instead of reformatting correctly — an unrelated declaration-parsing issue
that also affected some C++ lambda-as-call-argument shapes with no preceding `.`/`->` in the call
chain — was fixed earlier, 2026-08-20.)

#### 2. `curly-general-scope-reindent-postpass`: validated at real-code scale

`curly-general-scope-reindent-postpass` (default `off`, only takes effect when
`curly-general-scope-reindent` is also `on`) runs one extra reindentation pass directly on the
final formatted output, with no further formatting pass afterward — unlike
`curly-general-scope-reindent-multipass`, which always alternates a reindent pass with a full
formatting pass. Earlier testing found this extra pass could push a previously-correct wrapped
call's continuation line and closing bracket a level deeper than they should be, in shapes such as:

```kotlin
if( a?.b?.isSomething(
    context
) == true ) { ... }
```

and, more subtly, a closing bracket that mixes a paren-close with a brace-close on one line, a
`}.apply { ... }`/`}.also { ... }`-style reopened lambda chain, and a wrapped expression-bodied
declaration's continuation (including that continuation's own nested closing braces). All of
these have now been fixed: this pass leaves every line belonging to any such wrap or chain
completely alone, so its indentation exactly matches whatever the rest of the formatter already
decided. Validated by running this pass against a large real-world Kotlin codebase (roughly 190
files): re-formatting is idempotent, introduces no new syntax errors, and no instance of any of
the above over/under-indentation shapes remains anywhere in the result. The pass still keeps its
positive behavior too: it can *fix* a genuine indentation mistake the base pass leaves behind,
such as a declaration or closing brace that an earlier step left mis-indented relative to its own
siblings.

One separate, narrower gap turned up during that same validation, unrelated to the wrap-alignment
fix above, and has since been fixed: a Kotlin documentation comment (`/** ... */`) that itself
contains an example of a *nested* comment marker (e.g. illustrating `/*static*/` as part of a code
sample inside the doc) could have its content past that point mis-indented, because
comment-boundary detection previously did not account for Kotlin's own comments-can-nest rule.
Kotlin doc comments containing a nested comment marker in their own example text now reindent
correctly.

#### 3. Multi-line-call/condition wrap decisions can flap across repeated formatting passes (C/C++/Java/JS/TS)

Whether a small call or condition nested inside a longer expression stays on one line or gets
wrapped is, in one code path, decided by measuring the length of its entire surrounding source
line rather than just the candidate's own rendered content:

```java
// Short nested call, but the enclosing line is long -- may wrap unnecessarily:
someVeryLongVariableNameHere = anotherLongIdentifier + shortCall(a, b);
```

Because the measurement is based on the surrounding line's physical layout rather than the
candidate's own logical content, the same input can format differently the first time versus if
the already-formatted output is fed back in and formatted again — i.e. formatting is not always
idempotent for this narrow shape. No workaround exists short of avoiding deeply nested short calls
inside very long lines.

**Kotlin is no longer affected**: as of 2026-08-16, formatting a Kotlin file internally re-runs
itself (up to 5 passes) until two consecutive passes produce byte-identical output, converging on
a fixed point instead of flapping. This is a known, currently-unresolved gap
for C/C++/Java/JS/TS only.

#### 4. `.ts` files with embedded JSX need the explicit `jsx-in-ts` opt-in

The JSX/TSX boundary-finding pre-pass runs unconditionally on `.jsx`/`.tsx`/`.js`/`.mjs`/`.cjs` but
deliberately stays off by default on plain `.ts` (see "Single file" above for why) —
a legacy `.ts` file with real embedded JSX and no `jsx-in-ts` directive will have its JSX
mis-tokenized as ordinary angle-bracket/comparison syntax rather than preserved as an opaque span.

**Workaround:** opt in per file with the `jsx-in-ts` [`JXM_CFMT_CFG`
directive](#in-file-config-overrides) (or the equivalent CLI flag/env var/config-file key).

The `.js`/`.mjs`/`.cjs`/`.jsx`/`.tsx` pre-pass and downstream formatting rules have been validated
end-to-end against six real-world corpora spanning both JSX-in-`.js` and TSX
(`taniarascia/react-tutorial`, `ruanyf/react-demos`, `reactstrap/reactstrap`,
`microsoft/TypeScript-React-Starter`, `Lemoncode/react-typescript-samples`,
`excalidraw/excalidraw`), including JSX fragment shorthand (`<>...</>`) and multi-line
template-literal `${...}` holes; a residual risk of an unseen JSX-adjacent edge case not exercised
by these corpora remains, as with any real-world testing.

#### 5. JSX/TSX-syntax-aware reformatting is limited to a few targeted rules

Real JSX tag trees are located by the boundary-finding pre-pass and preserved byte-for-byte as
opaque, unformatted spans, with a small number of content-aware transforms layered on top:
wrapping an overlong JSX opening tag's attribute list; recursively reformatting each top-level
`{...}` expression hole (in both *children* position and an opening tag's *attribute value*
position — spread attributes, `{...props}`, are left untouched) through the normal JS/TS
pipeline, spliced back in place; and re-deriving the leading indentation of each direct child
tag/fragment's own opening line from its JSX nesting depth (fragments, self-closing tags, and
nested components are all tracked). That last pass only ever rewrites a tag-opening line's
leading whitespace — it never touches text runs, `{...}` hole interiors, string/template-literal
contents, or any other inline content, so it cannot change what a JSX tree renders, only how a
tag-opening line the author placed on its own line is indented. Beyond these, there is no
reflowing of text runs onto/off of their own lines, no attribute reordering, and no other
JSX-specific line-breaking — everything else about a JSX span's internal whitespace/line breaks is
whatever the author wrote, unchanged.

A hole whose interior contains deeply-nested, inconsistently-hand-indented object/array literals
can retain a non-idempotent relative indentation on the deepest line(s) across repeated format
rounds — the same already-tracked general-scope-depth-reindentation gap (item 1 above), not
specific to JSX holes.

A JSX tree whose root tag's own leading indentation mixes tabs and spaces is intentionally left
untouched by the child-indentation pass (it bails out rather than rewriting children against an
ambiguous base indent), so such a file may still need two formatting passes instead of one to
reach a stable result, rather than converging in a single pass like every other case.

#### 6. JS/TS import ordering misclassifies bundler/tsconfig path-mapped absolute imports as third-party

Local-import detection (§15) is syntactic only: an import specifier is `local` iff it starts with
`./` or `../`.

```js
import { Widget } from "components/Widget";   // resolves to the project's own source tree via
                                              // tsconfig `baseUrl`/`paths`, but is classified
                                              // "third-party", not "local"
```

No workaround: this formatter has no config concept for a project's source root and no
`tsconfig.json`/bundler-config resolution logic. Known, accepted simplification — no
source-root config key is planned.

#### 7. `.h` files default to C inference, so C++-only rules never apply unless overridden

The `.h` extension maps to `"c"` by default (C is by far the more common real-world
case for a bare `.h` file), so every C++-specific behavior across the whole `cpp` pipeline — not
just C++26 §5 reflection rules (`^^`, `[: :]` splice brackets), but empty-parameter-list
rendering, `template`/`requires` handling, and every other C++20/C++23/C++26 rule — never applies
to a `.h` file's content by default:

```cpp
// Module.h -- inferred as C by default, so `^^`/`[: :]` splice syntax is left untouched
auto r = ^^int;
```

**Workaround:** pass an explicit language override — `--lang cpp` on the CLI, the `lang=cpp` query
parameter in server mode, or a per-file directive that doesn't need to be remembered on every
invocation:

```cpp
//% JXM_CFMT_CFG --lang=cpp
auto r = ^^int;   // now correctly recognized and reformatted as C++
```

This is a deliberate design decision, not an oversight: blanket-treating every `.h` as C++ would
risk misapplying C++-only rules to genuine C headers, and content-sniffing heuristics to
auto-detect C vs. C++ were judged too fragile to trust for a correctness-sensitive rewrite like
this. The override takes priority over extension-based inference for `.h` files specifically — see
[In-file config overrides](#in-file-config-overrides) above.

#### 8. `normalize-comment-start-case-multiline` can capitalize commented-out code inside a multi-line comment group

Affects the curly-brace family (C/C++/Java/Kotlin/JS/TS), the `#`-comment tooling family
(E-INI/JxMakeFile/Makefile/Bash/PowerShell), and YAML/TOML. See "Config file format" → [Multi-sentence
comment capitalization](#multi-sentence-comment-capitalization-normalize-comment-start-case-multiline)
above for the full mechanism and its mechanical pre-filter.

```
// TODO: re-enable this test
// import './rxjs/rxjs.spec';
```

With `normalize-comment-start-case-multiline = on`, this becomes:

```
// TODO: re-enable this test
// Import './rxjs/rxjs.spec';
```

**Workaround:** leave this key off (the default) for codebases that keep a lot of commented-out
code inside otherwise-prose comment groups; enabling it elsewhere is a judgment call.

### Tag-based family (XML/HTML5)

#### 1. HTML5 deep tree-construction gap coverage is a narrow, documented approximation, not a full spec-faithful implementation

`html5-tc-gap-level` (default `0`) enables an increasing, cumulative set of narrow approximations
of the HTML5 spec's tree-construction algorithms. See "Config file format" → [HTML5
tree-construction gap levels](#html5-tree-construction-gap-levels-html5-tc-gap-level) above for
what each level enables and each level's own documented gap.

#### 2. HTML/XML single-word comments are left byte-for-byte untouched, even when they're genuine one-word prose

```html
<!--more-->
```

A single-word HTML/XML comment renders exactly as written — no capitalization, and no interior
padding around the `<!--`/`-->` markers either (an ordinary multi-word comment does get one space
of padding on each side, e.g. `<!-- some comment -->`). This is deliberate: a single-word HTML/XML
comment is often a content-splitting directive a third-party tool parses literally and must never
be rewritten in any way (the motivating case: WordPress's magic comments like `<!--more-->`,
`<!--nextpage-->`, `<!--noteaser-->`). See "Config file format" → [Comment capitalization
exceptions](#comment-capitalization-exceptions-normalize-comment-start-case) above.

No workaround: the accepted risk is that a codebase with a real one-word prose comment (e.g.
`<!--fixme-->`, `<!--todo-->`) keeps it lowercase instead of being capitalized — a false negative,
not a false positive (no comment is ever wrongly rewritten by this rule, only possibly left
as-is).

#### 3. XML has no text reflow — only attribute wrapping

A long text node's own content is never rewrapped/reflowed onto multiple lines; only an element's
attribute list wraps when the element's own line exceeds the width limit. Intentionally out of
scope: XML text content (unlike HTML5 prose) commonly carries meaningful whitespace, so reflowing
it would risk changing document meaning, not just layout. Not planned.

#### 4. `<script>`/`<style>` CDATA re-wrap can be broken by a literal `]]>` in the formatted output

An XHTML-compatibility `<![CDATA[ ... ]]>`-wrapped `<script>` or `<style>` block has its CDATA
markers unwrapped, its interior dispatched to the JS/TS or CSS pipeline, and the result re-wrapped
in fresh CDATA markers:

```html
<style><![CDATA[
  .a { content: "x"; }
]]></style>
```

No workaround: if the *formatted* interior happens to contain the literal three-character sequence
`]]>` (e.g. inside a string literal or comment), re-wrapping it prematurely terminates the CDATA
section at that point, since CDATA has no escape mechanism of its own for its own closing
delimiter. Accepted as an extremely rare edge case, not worth building escaping machinery for.

### Indent-based family (Python 3)

#### 1. A replacement field nested inside an f-string's format spec is not recursively sub-tokenized

```python
f"{x:{ width }}"
```

The outer `{x:...}` field is tokenized and spacing-normalized normally, but the tokenizer emits
the entire `:{ width }` format-spec tail as one opaque token rather than recursively
re-tokenizing the nested `{ width }` field into its own sub-tokens, so the extra interior spaces
above are left exactly as written — never corrupted, just not normalized (§5 f-string spacing
normalization never reaches it).

No workaround needed in practice: a full CPython dogfood run (2343 files, including
`Lib/test/test_fstring.py`, which specifically exercises this shape) found zero real instances of
this actually mattering — nested format-spec fields are almost always bare identifiers with no
internal whitespace to normalize. Not planned unless a real corpus turns up a concrete case.

### E-INI (Extended INI)

No known limitations beyond the fixed five-rule scope itself: any construct
not one of the five formatting rules (see the [E-INI](#e-ini-extended-ini)
section above) is left byte-identical by design, not as a gap.

### JxMakeFile

No known limitations beyond the fixed four-rule scope itself: any construct
not one of the four formatting rules (see the [JxMakeFile](#jxmakefile)
section above) is left byte-identical by design, not as a gap.

### Build/dev-tooling scripts (Makefile/Bash/PowerShell)

#### 1. Bash's pipe-spacing rule can't distinguish a real pipe from a zsh extended-glob alternation

```
(|.git)
```

This formatter has one fixed bash-grammar transform list, not a general zsh dialect parser, so
under `.sh`/`.bash` it would otherwise misread the zsh alternation above as a pipe and rewrite it
to `( | .git)`.

**Workaround (already applied by default):** a file is skipped entirely (left byte-for-byte
unchanged) when its shebang names an interpreter other than `bash`/`sh`/`dash`/`ksh`, including
the `env` indirection form:

```sh
#!/usr/bin/env zsh   # skipped: not a bash-compatible interpreter
#!/bin/zsh           # skipped: not a bash-compatible interpreter
#!/bin/bash          # formatted normally
```

Files with no shebang at all fall through and are formatted as bash, deliberately permissive.
Residual accepted gap: a shebang-less file using genuine zsh-only syntax (rare — sourced helper
scripts are typically shebang-less) is still not caught by this method, since the fallback for
no-shebang content is deliberately permissive rather than content-sniffed; known, accepted, not
planned to be closed.

#### 2. A Makefile recipe's `\` line continuations are never touched, including their alignment

```makefile
build:
	gcc -c a.c \
	       -o a.o
```

Recipe lines (any line starting with a literal tab) are excluded from formatting entirely, even
where §1.2 would otherwise realign an equivalent assignment-value continuation. No workaround: Make
is whitespace-sensitive there (a line not starting with a literal tab isn't a recipe at all), and a
recipe's exact whitespace can affect the invoked shell's own parsing (e.g. an embedded heredoc), so
reformatting risks changing program behavior, not just layout. Deliberate design choice, not
planned to change.

### AI-assist (GRU)

#### 1. Non-Latin/mixed-language comments always `ABSTAIN` from the rule-based classifier and never reach the GRU

```java
// 修复构建路径
```

A dedicated check disables classification entirely for any comment containing a
non-Latin codepoint, leaving it untouched rather than attempting a capitalize/trailing-period
decision. No crash/malformed output, just no normalization on these comments.

No workaround: a dedicated GRU trained specifically on non-Latin/mixed-language examples is a
distinct, unexplored idea, but not planned — it would need its own training corpus (a
language/script this project has no dogfood corpus for), its own weights file, and a second model
to load/maintain, a cost disproportionate to the benefit, since the affected decision
(leading-keyword/trailing-period ambiguity) is an English-prose-vs-code-keyword distinction that
mostly doesn't apply to non-Latin text in the first place.

#### 2. A trailing period next to more than one `.` in the same comment always survives — but capitalization still applies independently

```java
// see the .hpp file, e.g. widget.hpp.
```

becomes

```java
// See the .hpp file, e.g. widget.hpp.
```

The trailing-period stripper calls the classifier first, then discards its result via a mechanical
bail-out whenever a comment contains more than one `.` — a file extension, an abbreviation like
`e.g.`, or an ellipsis — even when the GRU already ran and produced a real answer. This bail only
affects period-stripping: comment-start capitalization is a separate step and still runs
normally, so the comment above is not left fully as-is — only its trailing period survives.

No workaround: distinguishing a mid-word/mid-token dot from a true sentence-ending dot is a
separate judgment call the shared model was never trained on (no `task` dimension in the training
schema, to keep it from degrading the model's main "is this substantive prose" job). Canceled, not
merely deferred — reliably telling the two apart would need a second, separately-trained model
(its own corpus and weights file) dedicated to that one narrow judgment call, which isn't planned
given the limited benefit.

#### 3. The GRU's residual false-positive rate on `NO` cases is accepted, not further reduced

Lowering `abstainThreshold` below its shipped `0.76` recovers more `YES` resolutions (ambiguous
comments the classifier confidently capitalizes) but raises the rate of wrongly capitalizing a
comment that was actually a real code reference. `0.76` was chosen as the best trade-off found so
far — see [`DESIGN_NOTES.md`](DESIGN_NOTES.md). No workaround: not planned to change further
unless a future corpus expansion or held-out measurement moves the curve.

---

## Design Notes

See [`DESIGN_NOTES.md`](DESIGN_NOTES.md) for the rationale behind specific formatter
decisions (why certain config defaults/approximations were chosen).

---

## License

Apache License, Version 2.0 — see [LICENSE_APACHEv2.txt](../../../LICENSE_APACHEv2.txt )
