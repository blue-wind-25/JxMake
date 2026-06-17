[//]: # (Copyright (C) 2022-2026 Aloysius Indrayanto)
[//]: # (This file is part of the JxMake build system and is distributed under the MIT License.)
[//]: # (See the LICENSE file in the formatter root directory for the full MIT license text.)

# style-fmt — Code Formatter

A deterministic code formatter for C, C++, and Java implementing the
[CodingStyle.md](../STYLE.md) style guide. No AI, no AST — tokenizer plus
recursive descent on bounded token slices.

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

Produces `formatter-1.00.jar` in the `formatter/` directory.

---

## Usage

### Single file

```sh
java -jar formatter-1.00.jar File.java
java -jar formatter-1.00.jar src/Utils.c
java -jar formatter-1.00.jar include/Module.h
```

Language is detected from the file extension (`.c` → C, `.h` → C, `.cpp`/`.cc`/`.cxx` → C++,
`.java` → Java).

### Output modes

```sh
java -jar formatter-1.00.jar File.java              # in-place edit (default)
java -jar formatter-1.00.jar --diff File.java       # print unified diff, do not edit
java -jar formatter-1.00.jar --check File.java      # exit 1 if file would change (CI)
java -jar formatter-1.00.jar --out DIR File.java    # write to DIR/File.java instead
```

### Server mode (faster for batch)

```sh
java -jar formatter-1.00.jar --server       # start server in background
java -jar formatter-1.00.jar File.java      # auto-connects to running server
java -jar formatter-1.00.jar --stop         # stop server
```

The server amortizes JVM startup across a batch of files. If no server is running,
the JAR falls back to standalone mode silently. `--server` is idempotent — safe to
call from a Makefile target even if the server is already running.

**After SIGKILL or manual lockfile deletion:** the next invocation detects the stale
lockfile (PID no longer alive), cleans it up, and starts fresh automatically.

### Makefile integration

```makefile
fmt:
    java -jar util/CodingStyle.md/formatter/formatter-1.00.jar --server
    java -jar util/CodingStyle.md/formatter/formatter-1.00.jar $(SRCS)

fmt-check:
    java -jar util/CodingStyle.md/formatter/formatter-1.00.jar --check $(SRCS)
```

---

## Configuration

The formatter reads configuration from the following sources, in order of increasing
precedence (later sources override earlier ones):

1. Built-in class defaults
2. `~/.config/style-fmt/config` — user global config
3. `STYLEFMT_*` environment variables
4. `.style-fmt` in the project root — per-project config (commit this to the repo)
5. `.style-fmt` in the source subdirectory — inherits from parent, overrides specific keys
6. CLI flags — always win

### Config file format

```properties
# ── Structural constants ──────────────────────────────────────────────────────
line-length                = 100
indent-size                = 4
indent-style               = spaces          # spaces | tabs
server-port                = 17173

# ── Behavior ──────────────────────────────────────────────────────────────────
closing-comment-min-lines  = 5
format-macros              = off             # off | on
line-endings               = lf             # lf | crlf | preserve

# ── C/C++ ─────────────────────────────────────────────────────────────────────
include-sort               = off             # off | on
header-guard-rename        = off             # off | on (warn only by default)
header-guard-style         = preserve        # preserve | ifndef | pragma-once

# ── Java ──────────────────────────────────────────────────────────────────────
java-import-order          = static, java, org, com, local
java-import-sort           = on
java-import-depth          = 2
java-import-blank-lines    = 1
```

### `.style-fmt` inheritance

A `.style-fmt` file in a subdirectory fully inherits from the nearest `.style-fmt`
found in any parent directory, overriding only the keys it explicitly specifies.

### Java import groups and module detection

`java-import-depth = 2` means the top 2 components of the file's own `package`
declaration define the local prefix (e.g. `package com.mycompany.audio` →
local prefix is `com.mycompany`).

For pre-Java-9 projects without module descriptors, the same `package` declaration
is used — no module system involvement required.

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

- `~/.config/style-fmt/` resolves via `System.getProperty("user.home")` which on
  Windows typically maps to `C:\Users\<name>` — this works correctly
- Environment variable overrides (`STYLEFMT_*`) work normally
- Some path overrides in `.style-fmt` using Unix-style absolute paths
  (e.g. `/etc/...`) will not resolve correctly on Windows — use relative paths
  or `user.home`-relative paths in config files intended for cross-platform use
- Server mode PID liveness check uses `ProcessHandle` (Java 9+) which is
  cross-platform — no `kill -0` shell-out is used

---

## AI-Assisted Formatting (long files / style migration)

For one-off style migration of large existing files (> 500 lines), the separate
Python script `reformat_chunks.py` uses the Anthropic API to apply the style guide
via Claude. This is complementary to the JAR formatter — the JAR is the day-to-day
tool; the Python script is for bulk migration.

See [`../README.txt`](../README.txt) for usage.

---

## Style Guide Reference

- [`../STYLE.md`](../STYLE.md) — common rules (all languages)
- [`../STYLE_C_CPP.md`](../STYLE_C_CPP.md) — C and C++ extensions
- [`../STYLE_JAVA.md`](../STYLE_JAVA.md) — Java extensions
- [`FORMATTER_DISCUSSION.md`](FORMATTER_DISCUSSION.md) — design rationale
- [`STATE.md`](STATE.md) — implementation progress tracker

---

## License

MIT — see [LICENSE](LICENSE)

> **Note:** The LICENSE file year should read `2022-2026`, matching the JxMake project origin year.
