# STATE.md — Formatter Implementation Tracker

---

## Instructions for Claude CLI

**Read this section first, every session, before doing anything else.**

### Session start
1. Read this entire file to understand current state
2. Check the **File Status** table to find the current file (`IN PROGRESS` first,
   then the first `NOT STARTED`)
3. Check the **Current File** checklist for unchecked items — that is where to resume
4. If anything in this file is ambiguous, stop and ask before writing any code

**Do NOT read `FORMATTER_DISCUSSION.md` or `README.md`** unless the user explicitly
asks. All decisions relevant to implementation are recorded in the
**Resolved Design Decisions** index below (full text in `STATE_rdd_log.md`).
`FORMATTER_DISCUSSION.md` is design history and future planning only — large, and
contains nothing the implementer needs beyond what is already indexed here.

> ⛔ **PHASE-2 GATE — DO NOT READ:**
> `STYLE_JAVA17.md`, `STYLE_CPP20.md`, `STATE_NEXT.md`, and `STATE_rdd_log.md` (in full).
> These are off-limits until the End Goal dogfood-test milestone is checked off.
> `STATE_rdd_log.md` may only be accessed via `grep -Fm1 'RDD_KEY_n'` for a specific key.
> Violation of this gate wastes context and risks importing out-of-scope constraints.

**ONLY** read the Java source file you are currently implementing or directly modifying. Do NOT read other source files unless a specific checklist item or ambiguity requires it.

### During implementation
- Implement one checklist section at a time
- After completing a section (or when the cumulative diff across all changed files
  exceeds ~50 lines, whichever comes first), do a checkpoint commit:
  1. Update STATE.md — check off completed items, update File Status table
  2. `git add util/CodingStyle.md/formatter/` (the entire formatter directory)
  3. `git reset util/CodingStyle.md/formatter/target/` (exclude build output)
  4. `git commit -m "<message>"` — short descriptive message, no strict format required,
     trailer ending with `Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>`
- Small related items within a section may be grouped into one commit if they
  are trivially connected — use judgment based on line count (~50 lines threshold)
- Never let implemented files and STATE.md drift out of sync — STATE.md must
  always reflect the true current state at every commit

### `.gitignore` — add these lines if not already present
```
# style-fmt build output
target/
*.jar
```

### When hitting an ambiguity or open question
1. **Stop coding immediately** — do not guess or proceed past the ambiguity
2. Update STATE.md: add the question to **Open Questions**, mark the blocked
   checklist item with `[~]` and a note
3. Commit STATE.md only:
   ```
   git add util/CodingStyle.md/formatter/STATE.md
   git commit -m "$(cat <<'EOF'
style-fmt: block on <question summary>

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
   )"
   ```
4. Ask the user and wait for an answer before continuing
5. Once resolved: append the full decision as a new row to `STATE_rdd_log.md`
   (next `RDD_KEY_n` number), add the key + topic to the **Resolved Design
   Decisions** index in this file, remove from **Open Questions**, unblock
   the checklist item, then continue

### When a file reaches COMPLETE
1. Mark it `COMPLETE` in the File Status table
2. Replace the **Current File** checklist with the checklist for the next file
3. Commit STATE.md together with the completed source file

### Session end
- Always leave STATE.md committed and up to date before ending the session
- The next session will resume from the first unchecked item in the Current File checklist

---

## Project Layout

```
util/CodingStyle.md/formatter/
  STATE.md                  ← this file
  README.md
  FORMATTER_DISCUSSION.md
  Makefile
  LICENSE
  src/
    com/jxmake/formatter/
      Main.java
      Config.java
      ServerMode.java
      Formatter.java            ← shared per-file pipeline (Config.resolve + ScopePipeline.process +
                                   whole-file enforceX passes, in order) called by both Main.java and
                                   ServerMode.java -- see "Formatter.java orchestration architecture"
                                   in Resolved Design Decisions
      IndentationDetector.java  ← whole-project dominant-indent-style walker (for `indent-style = keep`)
      ScopePipeline.java        ← recursive scope/signature discovery + group-render-splice engine
                                   for DeclarationAlignmentRule/GetterSetterRule/MiscRule's grouping
                                   rules (STYLE.md §5/§6/§8/§14) -- see "Main.java orchestration
                                   architecture" in Resolved Design Decisions
      tokenizer/
        TokenizerCore.java
      grid/
        ColumnGrid.java
        ModifierPriority.java
        CppModifierPriority.java
        JavaModifierPriority.java
      evaluator/
        ComplexityPaddingEvaluator.java
      rules/
        DeclarationAlignmentRule.java
        BlockStructureRule.java
        SwitchRule.java
        GetterSetterRule.java
        MiscRule.java
        CppSpecificRule.java
        JavaSpecificRule.java
  target/
```

---

## Resolved Design Decisions

Full decision text lives in `STATE_rdd_log.md` — **do not read that file in full**.
To look up a specific decision during implementation:
```
grep -Fm1 'RDD_KEY_n' util/CodingStyle.md/formatter/STATE_rdd_log.md
```

| Key | Topic |
|---|---|
| RDD_KEY_1 | Tokenizer |
| RDD_KEY_2 | Rule engine |
| RDD_KEY_3 | Shared grid |
| RDD_KEY_4 | Modifier priority |
| RDD_KEY_5 | Constants |
| RDD_KEY_6 | Java parsing |
| RDD_KEY_7 | AI dependency |
| RDD_KEY_8 | JAR target |
| RDD_KEY_9 | Server mode |
| RDD_KEY_10 | Server idempotency |
| RDD_KEY_11 | Port |
| RDD_KEY_12 | Path separator |
| RDD_KEY_13 | Lockfile location |
| RDD_KEY_14 | Line endings |
| RDD_KEY_15 | Config precedence |
| RDD_KEY_16 | `.style-fmt` inheritance |
| RDD_KEY_17 | Multi-module Java imports |
| RDD_KEY_18 | Windows support |
| RDD_KEY_19 | Output modes |
| RDD_KEY_20 | Build |
| RDD_KEY_21 | `ColumnGrid` flush API |
| RDD_KEY_22 | §3.1 complexity padding algorithm |
| RDD_KEY_23 | Declaration-statement detection |
| RDD_KEY_24 | Column grid rendering |
| RDD_KEY_25 | Static reorder vs. STYLE.md §5's worked example |
| RDD_KEY_26 | §10 Single-expression block eligibility |
| RDD_KEY_27 | §11 K&R brace style detection |
| RDD_KEY_28 | §11 lambda bodies also use K&R |
| RDD_KEY_29 | §12 else/else-if placement |
| RDD_KEY_30 | C/C++ bitfield column (`STYLE_C_CPP.md` §6) |
| RDD_KEY_31 | §7 closing comments — key variable on nesting |
| RDD_KEY_32 | §7 closing comments — engine structure |
| RDD_KEY_33 | §7 closing comments — named-construct blank lines |
| RDD_KEY_34 | §13 non-inline case brace wrapping |
| RDD_KEY_35 | §13 nested switch processing order |
| RDD_KEY_36 | §13 inline switch row classification |
| RDD_KEY_37 | §13 fallthrough marking |
| RDD_KEY_38 | §14 getter/setter rendering |
| RDD_KEY_39 | §14 getter/setter group detection |
| RDD_KEY_40 | §3.2 keyword spacing |
| RDD_KEY_41 | §3.3 initializer brace spacing |
| RDD_KEY_42 | §4 pre-increment rewrite |
| RDD_KEY_43 | §1 indentation scope |
| RDD_KEY_44 | §6 grouping and rendering |
| RDD_KEY_45 | §8 signature scope and rendering |
| RDD_KEY_46 | §9 function-body detection and return scoping |
| RDD_KEY_47 | §15 comment scope and sentence detection |
| RDD_KEY_48 | §15 partial-implementation split |
| RDD_KEY_49 | §15 multi-line block comment banner reformatting |
| RDD_KEY_50 | §15 separator alignment |
| RDD_KEY_51 | §6 multi-line right-hand sides |
| RDD_KEY_52 | §1 empty parameter list (`CppSpecificRule.java`) |
| RDD_KEY_53 | §2 one-liner scope (`CppSpecificRule.java`) |
| RDD_KEY_54 | §9 section dividers are non-actionable |
| RDD_KEY_55 | §4 pointer/const spacing already satisfied |
| RDD_KEY_56 | §3 template angle-bracket spacing (`CppSpecificRule.java`) |
| RDD_KEY_57 | §10 header file structure (`CppSpecificRule.java`) |
| RDD_KEY_58 | §11 dropped from `CppSpecificRule.java` scope |
| RDD_KEY_59 | `JavaSpecificRule.java` scoping |
| RDD_KEY_60 | §2 Allman-conversion vs. getter/setter one-liner groups -- left unguarded |
| RDD_KEY_61 | §3.1 condition-interior padding -- wiring decision |
| RDD_KEY_62 | §3.1 condition-interior padding -- implementation |
| RDD_KEY_63 | §2 method-definition Allman conversion (`JavaSpecificRule.java`) |
| RDD_KEY_64 | §4 array-declaration syntax parenthetical -- non-actionable |
| RDD_KEY_65 | §7 import group order/count contradiction |
| RDD_KEY_66 | `Main.java` orchestration architecture |
| RDD_KEY_67 | STYLE.md §5/§6 scope -- anywhere in code, recursively |
| RDD_KEY_68 | `DeclarationAlignmentRule.splitStatements` depth-awareness fix |
| RDD_KEY_69 | §7 import ordering implementation (`JavaSpecificRule.java`) |
| RDD_KEY_70 | `Config.java` file format |
| RDD_KEY_71 | `Config.java` resolution scope |
| RDD_KEY_72 | `Formatter.java` orchestration architecture |
| RDD_KEY_73 | `ServerMode.java` wire protocol |
| RDD_KEY_74 | `Formatter.java` whole-file pass order |
| RDD_KEY_75 | Supersedes RDD_KEY_60 -- Allman pass actually destroys §14 grouping, ordering alone insufficient |
| RDD_KEY_76 | `DeclarationAlignmentRule` misparses a bare `++j;`/`--j;` statement as a fake field declaration |
| RDD_KEY_77 | `MiscRule.enforceCommentStyle` relied on pipeline ordering (not detection) to skip closing-comment labels, breaking idempotency |
| RDD_KEY_78 | `ScopePipeline.splitTopLevelSpans` never closed a span at a C++ access-specifier label, merging it into the following member |

---

## Open Questions

*(none)*

---

## File Status

| File | Status |
|---|---|
| `Main.java` | NOT STARTED |
| `Config.java` | COMPLETE |
| `ServerMode.java` | IN PROGRESS |
| `Formatter.java` | COMPLETE |
| `IndentationDetector.java` | NOT STARTED |
| `ScopePipeline.java` | COMPLETE (reopened during `Formatter.java` smoke-testing to fix a C++ access-specifier-label span bug -- see Resolved Design Decisions: "`ScopePipeline.splitTopLevelSpans` never closed a span at a C++ access-specifier label, merging it into the following member") |
| `TokenizerCore.java` | COMPLETE |
| `ColumnGrid.java` | COMPLETE |
| `ModifierPriority.java` | COMPLETE |
| `CppModifierPriority.java` | COMPLETE |
| `JavaModifierPriority.java` | COMPLETE |
| `ComplexityPaddingEvaluator.java` | COMPLETE |
| `DeclarationAlignmentRule.java` | COMPLETE (`splitStatements` made depth-aware -- see Resolved Design Decisions: "`DeclarationAlignmentRule.splitStatements` depth-awareness fix"; reopened during `Formatter.java` smoke-testing to reject a non-keyword, non-identifier type lead -- see Resolved Design Decisions: "`DeclarationAlignmentRule` misparses a bare `++j;`/`--j;` statement as a fake field declaration") |
| `BlockStructureRule.java` | COMPLETE |
| `SwitchRule.java` | COMPLETE |
| `GetterSetterRule.java` | COMPLETE |
| `MiscRule.java` | COMPLETE (§1 `indent-style=keep` cross-file integration deferred to `IndentationDetector.java` -- see Resolved Design Decisions: "§1 indentation scope"; §3.1 condition-interior padding added -- see Resolved Design Decisions: "§3.1 condition-interior padding -- implementation"; reopened during `Formatter.java` smoke-testing to add structural detection for closing-comment labels -- see Resolved Design Decisions: "`MiscRule.enforceCommentStyle` relied on pipeline ordering (not detection) to skip closing-comment labels, breaking idempotency") |
| `CppSpecificRule.java` | COMPLETE (§11 "Include Ordering" dropped from scope -- no such section exists in STYLE_C_CPP.md; see Resolved Design Decisions: "§11 dropped from `CppSpecificRule.java` scope"; reopened during `Formatter.java` smoke-testing to add the §14 one-liner adjacency heuristic -- see Resolved Design Decisions: "Supersedes RDD_KEY_60 -- Allman pass actually destroys §14 grouping, ordering alone insufficient") |
| `JavaSpecificRule.java` | COMPLETE (reopened during `Formatter.java` smoke-testing to add the §14 one-liner adjacency heuristic -- see Resolved Design Decisions: "Supersedes RDD_KEY_60 -- Allman pass actually destroys §14 grouping, ordering alone insufficient") |
| `README.md` (defer until just before Dogfood) | NOT STARTED |

---

## Current File: `ServerMode.java` — IN PROGRESS

**Resolved (already settled by prior RDD entries, just citing them here):**
- Wire protocol: `POST /format?path=<path>&lang=<java|cpp, optional>` with raw content as the
  request body, formatted content as the 200 response body, non-2xx + error message on failure;
  `POST /shutdown` for graceful exit -- RDD_KEY_73.
- Lockfile: `$HOME/.config/style-fmt/server.lock` (RDD_KEY_13), written by the server on
  successful bind, deleted on graceful shutdown.
- Idempotency: on start, if the lockfile's PID is alive (`ProcessHandle.of(pid).isPresent()`),
  another server is already running -- treat as a harmless no-op (log and return), do not bind a
  second socket. If the PID is not alive, the lockfile is stale -- delete it and bind fresh --
  RDD_KEY_10.
- Port: default `17173` (`DEFAULT_PORT`), overridable via `Config.serverPort()`; lockfile records
  the actual bound port -- RDD_KEY_9/RDD_KEY_11.
- HTTP transport: `com.sun.net.httpserver.HttpServer` (JDK-bundled since 6, no external
  dependency) -- the only reasonable choice given the Makefile's plain `javac`+`jar` build with no
  dependency manager of any kind; not treated as a new design fork.

**Mechanical fill-in (no design fork, decided here and documented for consistency):**
- Lockfile content format: two lines, PID then port (`"<pid>\n<port>\n"`) -- RDD_KEY_9/11/13 say
  *what* it carries (PID + port) but not the exact byte format.
- `/format` language inference when `lang` is absent: by `path`'s extension --
  `.java` → `"java"`; `.c`/`.cc`/`.cpp`/`.cxx`/`.h`/`.hh`/`.hpp`/`.hxx` → `"cpp"`; anything else →
  400 with an error body. Consistent with `Formatter.java`'s existing two-language gate
  (`isCpp`/`isJava`).
- `/format` resolves config fresh per request via `Config.resolve(Paths.get(path), null)` --
  RDD_KEY_71 already establishes per-file resolution with no CLI-override layer for server
  requests (the wire protocol in RDD_KEY_73 carries no override parameters).
- `HttpServer`'s executor is left as the JDK default (sequential dispatch on the server's own
  thread) -- this is a local dev-loop formatting daemon, not a concurrent web service; revisit only
  if real usage proves this insufficient, same "adjustable later if wrong in practice" disposition
  as RDD_KEY_74.
- `/shutdown` responds `200` first (so the calling CLI's POST doesn't hang), then spawns a
  daemon thread that calls `httpServer.stop(1)`, deletes the lockfile, and `System.exit(0)` --
  stopping the server from within its own request-handler thread would block that same handler
  on its own in-flight exchange.

### Checklist

- [ ] **Skeleton** -- `public static void start(Config config)`; resolve `DEFAULT_PORT` from
      `config.serverPort()`, lockfile path from `System.getProperty("user.home")` +
      `/.config/style-fmt/server.lock` (mirroring `Config.java`'s own `CONFIG_DIR`/`CONFIG_FILE`
      constant pattern).
- [ ] **Idempotency check** -- read lockfile if present; live PID → log + return; stale/missing →
      delete if present, continue to bind.
- [ ] **Bind + lockfile write** -- `HttpServer.create(new InetSocketAddress("localhost", port), 0)`;
      on successful bind, write `"<pid>\n<port>\n"` to the lockfile (creating
      `~/.config/style-fmt/` if missing).
- [ ] **`/format` handler** -- parse `path`/`lang` query params; read request body fully as
      bytes → `String`; infer language from extension if `lang` absent (400 if unrecognized);
      `Config.resolve` + `Formatter.formatOne`; write formatted content as the `200` body; catch
      any exception and respond non-2xx with the exception message as the body.
- [ ] **`/shutdown` handler** -- respond `200` immediately, then on a daemon thread:
      `httpServer.stop(1)`, delete the lockfile, `System.exit(0)`.
- [ ] **Throwaway smoke test** -- not committed: start the server on an ephemeral port, POST a
      small Java snippet to `/format` and check the response is correctly formatted, POST a C++
      snippet and check the same, verify the lockfile was written with the right PID/port, POST
      `/shutdown` and verify the process's `HttpServer` actually stops and the lockfile is removed.

---

## Config Keys and Defaults

Configurable values with their in-class defaults. All overridable via config file or CLI.

```properties
# ── Structural constants ──────────────────────────────────────────────────────
line-length                = 100
indent-size                = 4
indent-style               = spaces          # spaces | tabs | keep
server-port                = 17173

# ── Behavior ──────────────────────────────────────────────────────────────────
closing-comment-min-lines  = 5
format-macros              = off             # off | on
line-endings               = lf              # lf | crlf | preserve

# ── C/C++ ─────────────────────────────────────────────────────────────────────
include-sort               = off             # off | on
header-guard-rename        = off             # off | on
header-guard-style         = preserve        # preserve | ifndef | pragma-once

# ── Java ──────────────────────────────────────────────────────────────────────
java-import-order          = java, com, org, other, local, static
java-import-sort           = on
java-import-depth          = 2
java-import-blank-lines    = 1
```

---

## Fixed Constants (non-configurable)

These must appear as `private static final` in their owning class, never as raw literals.

| Constant | Value | Owner class |
|---|---|---|
| `MIN_DIVIDER_SLASHES` | `60` | `CppSpecificRule` |
| `HEADER_ZONE_BLANK_LINES` | `2` | `CppSpecificRule` |
| `INCLUDE_GROUP_BLANK_LINES` | `1` | `CppSpecificRule` |
| `EXTERN_C_LABEL` | `"extern \"C\""` | `CppSpecificRule` |
| `APP_NAME` | `"style-fmt"` | `Config` |
| `LOCKFILE_NAME` | `"server.lock"` | `ServerMode` |
| `CONFIG_DIR` | `".config/style-fmt"` | `Config` |
| `CONFIG_FILE` | `"config"` | `Config` |
| `DEFAULT_PORT` | `17173` | `ServerMode` |
| `MANIFEST_FILE` | `"MANIFEST.MF"` | _(build only, Makefile)_ |

---

## Java File Header

Every `.java` source file must begin with this copyright block, before the `package` declaration:

```java
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */
```

## Java Coding Convention — `final` Locals and Parameters

Mark every local variable and method/constructor parameter `final` whenever it is
never reassigned after its initial assignment (i.e., whenever the compiler would
accept `final` there). This applies to all `.java` files under `src/` in this
project, including ones already marked COMPLETE — when editing an existing file
for any reason, bring touched declarations into compliance opportunistically;
a dedicated pass is not required unless asked. Loop counters and other variables
that are genuinely reassigned (e.g. a `for` loop's `i`, an accumulator) must NOT
be marked `final` — let `javac` be the check: if marking something `final` fails
to compile, it was actually being reassigned, so leave it without `final`.

## End Goal
- [ ] Dogfood test — run formatter on its own `src/` tree, verify style compliance and that `make` still succeeds after

Once the above is checked off, the formatter's core (Tier 1 + Tier 2, STYLE.md /
STYLE_C_CPP.md / STYLE_JAVA.md) is considered complete. Phase 2 — Java 17+ and
C++20+ construct support — begins at that point, tracked separately in
`STATE_NEXT.md` (which also covers trimming `AI_PREAMBLE.md` down to its
post-JAR Tier-3-only scope). Do not open or read `STATE_NEXT.md`,
`STYLE_JAVA17.md`, or `STYLE_CPP20.md` before this milestone is checked off.
