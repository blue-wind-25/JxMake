# Java Compile Server (JCS)

A persistent `javac` daemon that keeps the JVM warm between compilations,
eliminating JVM startup cost (~200–500 ms per build) from your Makefile workflow.

Uses `javax.tools` (standard JDK API) — no external dependencies.
Self-terminates after **3 hours of inactivity**.
Supports **multiple JDK versions** simultaneously (one daemon per JDK major
version, ports auto-derived).

Runs on **Linux/macOS** (Bash) and **Windows** (PowerShell 5.1+, PowerShell 7+).

---

## Files

| File | Purpose |
|---|---|
| `CompileServer.java` | Daemon server source |
| `build-server.sh` | Compiles `CompileServer.java` → `compile-server.jar` |
| `start-compile-server.sh` | Starts the daemon (Bash) |
| `start-compile-server.ps1` | Starts the daemon (PowerShell) |
| `start-compile-server.cmd` | Invokes the `.ps1` from cmd.exe |
| `stop-compile-server.sh` | Stops a daemon by port or stops all (Bash) |
| `stop-compile-server.ps1` | Stops a daemon by port or stops all (PowerShell) |
| `stop-compile-server.cmd` | Invokes the `.ps1` from cmd.exe |
| `javac-client.sh` | Makefile `JAVAC=` replacement — explicit port, no auto-start (Bash) |
| `javac-client.ps1` | Makefile `JAVAC=` replacement (PowerShell) |
| `javac-client.cmd` | Invokes the `.ps1` from cmd.exe |
| `javac-daemon-wrapper.sh` | Transparent wrapper — auto-starts daemon, auto-derives port (Bash) |
| `javac-daemon-wrapper.ps1` | Transparent wrapper (PowerShell) |
| `javac-daemon-wrapper.cmd` | Invokes the `.ps1` from cmd.exe |

---

## Protocol

Each client connection uses a newline-delimited framing with Unit Separator
(U+001F, 0x1F) wrapped sentinel tokens:

**Client → Server**
```
arg1
arg2
...
\u001FENDINP\u001F
```

**Server → Client**
```
\u001FSTDOUT\u001F
<javac stdout lines — section omitted when empty>
\u001FSTDERR\u001F
<javac stderr lines — section omitted when empty>
\u001FEXTCOD\u001F
<exit code>
```

`\u001F` is the Unit Separator character (U+001F, decimal 31).  It never
appears in normal javac output, making framing unambiguous without escaping.

**Bash parsing**: `awk` matches sentinel lines via the `\037` octal literal
(0x1F) directly from the raw response; no preprocessing is required.

**PowerShell parsing**: `[char]0x1F` produces the Unit Separator natively;
sentinel comparison uses exact string equality.

### Path handling

All path arguments are absolutized by the client before being sent to the
server.  The server runs in a fixed working directory (wherever it was first
started), so relative paths would otherwise fail when `make` runs from a
different directory.  The client resolves:

- Source files (bare arguments) — absolutized against the client's CWD
- Output / generated-source dirs (`-d`, `-s`, `-h`) — absolutized
- Classpath-style flags (`-cp`, `-classpath`, `-sourcepath`, `-modulepath`,
  etc.) — each `:` (Linux/macOS) or `;` (Windows) separated entry absolutized
- `@argfile` paths — absolutized

Flag-only arguments (e.g. `-verbose`, `-source`, `-target`) pass through
unchanged.  This makes all client scripts work correctly from any directory
regardless of where the daemon was started.

---

## Auto-derived ports

Pass port `0` anywhere a port is required.  The port is derived from the
JDK major version:

| JDK major | Port  |
|-----------|-------|
| 8         | 8000  |
| 11        | 11000 |
| 17        | 17000 |
| 21        | 21000 |
| 25        | 25000 |

Formula: `port = major_version × 1000`.

---

## Quick Start

### 1. Build the jar (once, any JDK ≥ 8)

```bash
cd util/jcs
bash build-server.sh
# → produces compile-server.jar
```

### 2. Choose your usage style

---

#### Style A — Explicit `JAVAC=` in your Makefile (recommended, no JDK surgery)

**Linux/macOS**

```bash
bash start-compile-server.sh 0           # auto port from active JDK
# or explicit:
bash start-compile-server.sh 21000 /usr/lib/jvm/java-21/bin/java
```

In your Makefile:
```makefile
JAVAC = /path/to/util/jcs/javac-client.sh 0
# or with explicit port:
JAVAC = /path/to/util/jcs/javac-client.sh 21000
```

**Windows**

```batch
start-compile-server.cmd 0
```

In your Makefile / build script:
```makefile
JAVAC = C:\path\to\util\jcs\javac-client.cmd 0
```

Stop when done:
```bash
bash stop-compile-server.sh 0    # or explicit port
bash stop-compile-server.sh      # stop all
```

---

#### Style B — Transparent wrapper (auto-starts, auto-derives port)

No manual daemon management.  The wrapper starts the daemon on first use.

**Linux/macOS (no JDK surgery)**

```makefile
JAVAC     = /path/to/util/jcs/javac-daemon-wrapper.sh
JAVA_HOME = /usr/lib/jvm/java-21
```

**Linux/macOS (drop-in, replaces `javac` in JDK bin)**

```bash
mv $JAVA_HOME/bin/javac $JAVA_HOME/bin/javac.real
cp javac-daemon-wrapper.sh $JAVA_HOME/bin/javac
chmod +x $JAVA_HOME/bin/javac
cp compile-server.jar $JAVA_HOME/bin/
```

After this, every `javac` call is automatically proxied.

**Windows (drop-in, replaces `javac.exe` in JDK bin)**

> Because `cmd.exe` resolves `.cmd` before `.exe` on PATH, renaming the
> real compiler and placing the `.cmd` launcher in the same directory is
> sufficient.  No PATH editing is needed.

```batch
REM 1. Rename the real compiler
rename "%JAVA_HOME%\bin\javac.exe" javac.real.exe

REM 2. Copy the wrapper files into the JDK bin directory
copy javac-daemon-wrapper.cmd "%JAVA_HOME%\bin\"
copy javac-daemon-wrapper.ps1 "%JAVA_HOME%\bin\"
copy compile-server.jar       "%JAVA_HOME%\bin\"
```

After this, every `javac` call is automatically proxied through the daemon,
which falls back to `javac.real.exe` if the daemon cannot start.

---

## Multiple JDK Versions

Each JDK major version gets its own daemon.  With Style B, ports are
auto-derived — no configuration needed:

```makefile
# Project A — Java 21 (port 21000)
JAVA_HOME = /usr/lib/jvm/java-21
JAVAC     = /path/to/javac-daemon-wrapper.sh

# Project B — Java 17 (port 17000)
JAVA_HOME = /usr/lib/jvm/java-17
JAVAC     = /path/to/javac-daemon-wrapper.sh
```

With Style A, start servers on different ports explicitly:

```bash
bash start-compile-server.sh 0 /usr/lib/jvm/java-21/bin/java
bash start-compile-server.sh 0 /usr/lib/jvm/java-17/bin/java
```

---

## Parallel Builds (`make -j`)

The server handles concurrent compilations safely.  Each connection gets its
own `StandardJavaFileManager` instance.  The thread pool grows on demand
(`Executors.newCachedThreadPool`).

---

## PID and Log Files

| File | Contents |
|---|---|
| `$TMPDIR/javac-daemon-<port>.pid` | Daemon PID (auto-deleted on exit) |
| `$TMPDIR/javac-daemon-<port>.log` | Daemon stdout/stderr log |

`$TMPDIR` is `/tmp` on Linux/macOS and `%TEMP%` on Windows.

---

## Requirements

### All platforms
- JDK 8 or newer (needs `javax.tools`)
- `compile-server.jar` (built by `build-server.sh`)

### Linux / macOS
- Bash 4.0+ (CentOS 7 and newer)
- `nc` (netcat), `tr`, `awk` (GNU awk), `nohup` — standard on all distros

### Windows
- PowerShell 5.1+ (Windows 10 built-in) or PowerShell 7.x
- No additional tools required (uses .NET `System.Net.Sockets`)
