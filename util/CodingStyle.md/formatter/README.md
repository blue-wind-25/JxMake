# jxmake-code-formatter — Code Formatter

A deterministic code formatter for C, C++, and Java implementing the
[CodingStyle.md](../STYLE.md) style guide. No AI, no AST — tokenizer plus
recursive descent on bounded token slices.

Kotlin (`.kt`/`.kts`) support also exists end-to-end (auto-detected by
extension; see [`../STYLE_KOTLIN.md`](../STYLE_KOTLIN.md) /
[`../STYLE_KOTLIN2.md`](../STYLE_KOTLIN2.md)), but is newer than the C/C++/Java
support and has not yet been through the same real-world dogfood testing —
see `STATE_KOTLIN.md` for current status before relying on it for a large
existing codebase.

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
`.java` → Java, `.kt`/`.kts` → Kotlin).

For a file with a non-standard extension (e.g. `.java.in`, `.txt`, no extension at all),
override detection with `--lang`:

```sh
java -jar code-formatter-1.00.jar --lang java Template.java.in
java -jar code-formatter-1.00.jar --lang cpp Module.inc
```

`--lang` accepts exactly one of `c`, `cpp`, `java`, and applies to every file given on that
command line (mixing file types with a single forced `--lang` in one invocation isn't
supported — run the formatter once per language instead). There is no `--lang kotlin`
override — Kotlin files must be recognized by their `.kt`/`.kts` extension. Without
`--lang`, a file whose extension can't be recognized is an error. `--lang` also works with
server mode (below) — the client sends the chosen language to the server, which uses it in
place of its own extension-based guess for that request.

### Output modes

```sh
java -jar code-formatter-1.00.jar File.java              # in-place edit (default)
java -jar code-formatter-1.00.jar --diff File.java       # print unified diff, do not edit
java -jar code-formatter-1.00.jar --check File.java      # exit 1 if file would change (CI)
java -jar code-formatter-1.00.jar --out DIR File.java    # write to DIR/File.java instead
```

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
6. CLI flags — always win

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
format-macros                    = off         # off | on

# ── C/C++ ─────────────────────────────────────────────────────────────────────
header-guard-rename              = off         # off | on (warn only by default)

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

### C-preprocessor directives in Java source

Some projects run `.java` files through a C-macro preprocessor (e.g. PCPP-style)
before compilation, as a poor man's template mechanism. `#define`/`#ifdef`/`#endif`/etc.
lines are recognized and passed through untouched, same as in `.c`/`.cpp`/`.h` files.
Note: a preprocessor directive placed immediately before a method definition is
currently known to get glued onto the same output line — avoid that specific
placement until it's fixed.

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

See `STATE.md`'s Phase Status / End Goal sections for current progress, and
[`STATE_KOTLIN.md`](STATE_KOTLIN.md) for the Kotlin support tracker specifically.

---

## Server Wire Protocol

The server (`--server`) exposes two plain-HTTP endpoints on `localhost:<port>` (default
`17173`, override with `--port N` / `server-port` config key):

- `POST /format?path=<abs-path>&lang=<c|cpp|java>[&format-off=true]` — request body is the
  file's raw content (UTF-8); response body is the formatted content (UTF-8), HTTP 200. The
  `lang` parameter is required by the client and always takes priority over any
  extension-based guess the server could make from `path` — this is how `--lang` (above)
  reaches the server, and is also why the server itself never needs its own `--lang`-style
  flag. An unrecognized `lang` value, or a request missing `path`, gets HTTP 400 with a plain
  text error body. Any other failure (e.g. a malformed file) gets HTTP 500.
- `POST /shutdown` — asks the server to stop; responds `200 shutting down` immediately, then
  terminates the process shortly after (deleting its lockfile first). Used by `--stop`.

Clients are expected to auto-detect a running server via the lockfile at
`~/.config/jxmake-code-formatter/server.lock` (PID on line 1, port on line 2) rather than
talking to a hardcoded port — see `ServerMode.findRunningServerPort()`. This is the same
protocol the bundled CLI's own auto-connect logic (`--standalone` to disable it) uses; a
third-party client only needs to speak this HTTP protocol, not link against the JAR.

---

## License

MIT — see [LICENSE](LICENSE)

> **Note:** The LICENSE file year should read `2022-2026`, matching the JxMake project origin year.
