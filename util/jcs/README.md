# Java Compile Server (`javacompileserver`)

A persistent `javac` daemon that keeps the JVM warm between compilations,
eliminating JVM startup cost (~200–500 ms per build) from your Makefile workflow.

Uses `javax.tools` (standard JDK API) — no external dependencies.
Self-terminates after **3 hours of inactivity**.
Supports **multiple JDK versions** simultaneously (one daemon per JDK, ports auto-derived).

---

## Files

| File | Purpose |
|---|---|
| `CompileServer.java` | The daemon server source |
| `build-server.sh` | Compiles `CompileServer.java` → `compile-server.jar` |
| `start-compile-server.sh` | Starts the daemon (explicit port + optional java binary) |
| `stop-compile-server.sh` | Stops a daemon by port (or all daemons) |
| `javac-client.sh` | Makefile `JAVAC=` replacement — explicit port, no auto-start |
| `javac-daemon-wrapper.sh` | Transparent wrapper — auto-starts daemon, auto-derives port from JDK path |

---

## Quick Start

### 1. Build the jar (once)
```bash
cd util/javacompileserver
bash build-server.sh
# → produces compile-server.jar
```

### 2. Choose your usage style

---

#### Style A — Explicit `JAVAC=` in your Makefile (recommended, no JDK surgery)

Start the daemon manually:
```bash
bash start-compile-server.sh 62650
# or with a specific JDK:
bash start-compile-server.sh 62650 /usr/lib/jvm/java-21/bin/java
```

In your Makefile:
```makefile
JAVAC = /path/to/util/javacompileserver/javac-client.sh 62650

build:
    $(JAVAC) -cp libs/*.jar -d build/classes src/Main.java
```

Stop it when done:
```bash
bash stop-compile-server.sh 62650
# or stop all:
bash stop-compile-server.sh
```

---

#### Style B — Transparent wrapper (auto-starts, auto-derives port)

No manual daemon management needed. The wrapper starts the daemon on first use
and derives the port from the JDK path, so different JDKs never collide.

In your Makefile:
```makefile
JAVAC     = /path/to/util/javacompileserver/javac-daemon-wrapper.sh
JAVA_HOME = /usr/lib/jvm/java-21

build:
    $(JAVAC) -cp libs/*.jar -d build/classes src/Main.java
```

Or, for completely transparent use (replaces `javac` in the JDK bin dir):
```bash
mv $JAVA_HOME/bin/javac $JAVA_HOME/bin/javac.real
cp javac-daemon-wrapper.sh $JAVA_HOME/bin/javac
chmod +x $JAVA_HOME/bin/javac
# compile-server.jar must be in the same directory
cp compile-server.jar $JAVA_HOME/bin/
```
After this, any `javac` invocation in that JDK is automatically routed through
the daemon. The wrapper falls back to `javac.real` if the daemon fails to start.

---

## Multiple JDK Versions

Each JDK gets its own daemon. With Style B, ports are derived automatically from
the JDK bin path — no configuration needed:

```makefile
# Project A — Java 21
JAVAC     = /path/to/javac-daemon-wrapper.sh
JAVA_HOME = /usr/lib/jvm/java-21

# Project B — Java 11
JAVAC     = /path/to/javac-daemon-wrapper.sh
JAVA_HOME = /usr/lib/jvm/java-11
```

Two daemons start automatically on different ports.

With Style A, just start two servers on different ports:
```bash
bash start-compile-server.sh 62650 /usr/lib/jvm/java-21/bin/java
bash start-compile-server.sh 62651 /usr/lib/jvm/java-11/bin/java
```

---

## Parallel Builds (`make -j`)

The server handles concurrent compilations safely. Each connection gets its own
`StandardJavaFileManager` instance (required for thread safety). The thread pool
grows as needed (`Executors.newCachedThreadPool`).

---

## PID and Log Files

| File | Contents |
|---|---|
| `/tmp/javac-daemon-<port>.pid` | Daemon PID (auto-deleted on exit) |
| `/tmp/javac-daemon-<port>.log` | Daemon stdout/stderr log |

---

## Requirements

- JDK 11 or newer (needs `javax.tools`, `ProcessHandle`)
- `nc` (netcat) on the client side
- `nohup`, `cksum`, `awk` (standard Unix tools)
