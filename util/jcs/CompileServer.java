import javax.tools.*;
import java.lang.management.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Persistent Java compilation daemon.
 *
 * Protocol (per connection, newline-delimited):
 *   Client sends:  arg1\narg2\n...\n\u0000ENDINP\u0000\n
 *   Server replies:
 *     \u0000STDOUT\u0000\n
 *     stdout lines  -- or one empty line when stdout is empty
 *     \u0000STDERR\u0000\n
 *     stderr lines  -- or one empty line when stderr is empty
 *     \u0000EXTCOD\u0000\n
 *     exit-code\n
 *
 * Sentinel lines contain actual NUL bytes (U+0000) before and after the
 * tag word, making them unambiguous framing tokens that never appear in
 * normal javac output.
 *
 * If port argument is 0, the listening port is derived from the Java
 * major version: major * 1000
 * (JDK 8->8000, JDK 11->11000, JDK 17->17000, JDK 21->21000).
 *
 * Self-terminates after IDLE_TIMEOUT_MS of no compilation activity.
 */
public class CompileServer {

    static final long IDLE_TIMEOUT_MS = 3L * 60 * 60 * 1000; // 3 hours

    // NUL-wrapped sentinel tags.
    // \u0000 is the Java Unicode escape for U+0000 (the NUL character).
    // These escapes are resolved before tokenisation, so at runtime each
    // constant begins and ends with an actual NUL byte (0x00).
    static final String SENTINEL_ENDINP = "\u0000ENDINP\u0000";
    static final String SENTINEL_STDOUT = "\u0000STDOUT\u0000";
    static final String SENTINEL_STDERR = "\u0000STDERR\u0000";
    static final String SENTINEL_EXTCOD = "\u0000EXTCOD\u0000";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java -jar compile-server.jar <port>");
            System.exit(1);
        }

        // resolvePort handles port==0 by deriving from the Java major version
        final int port = resolvePort(Integer.parseInt(args[0]));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("ERROR: No JavaCompiler available. " +
                "Make sure you are running a JDK, not a JRE.");
            System.exit(2);
        }

        final String pidFile = System.getProperty("java.io.tmpdir") +
            "/javac-daemon-" + port + ".pid";
        long pid = detectPid();
        try (PrintWriter pw = new PrintWriter(pidFile)) {
            pw.println(pid);
        } catch (Exception e) {
            System.err.println("Warning: could not write PID file: " + e.getMessage());
        }
        if (pid == -1) {
            System.err.println("Warning: could not determine PID, wrote -1 to " + pidFile);
        } else {
            System.err.println("PID " + pid + " written to " + pidFile);
        }

        ServerSocket server = new ServerSocket(port);
        server.setReuseAddress(true);

        System.err.println("javac daemon ready");
        System.err.println("  JDK : " + System.getProperty("java.home"));
        System.err.println("  Port: " + port);
        System.err.println("  Idle timeout: 3 hours");

        ExecutorService pool = Executors.newCachedThreadPool();
        AtomicLong lastActivity = new AtomicLong(System.currentTimeMillis());

        // Idle watchdog -- shuts down after IDLE_TIMEOUT_MS with no activity
        Thread watchdog = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(60_000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    long idle = System.currentTimeMillis() - lastActivity.get();
                    if (idle >= IDLE_TIMEOUT_MS) {
                        System.err.println("Idle timeout reached, shutting down.");
                        new File(pidFile).delete();
                        System.exit(0);
                    }
                }
            }
        });
        watchdog.setDaemon(true);
        watchdog.start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                new File(pidFile).delete();
            }
        }));

        while (true) {
            try {
                Socket conn = server.accept();
                lastActivity.set(System.currentTimeMillis());
                pool.submit(new Runnable() {
                    public void run() {
                        try {
                            handleConnection(compiler, conn);
                        } catch (Exception e) {
                            System.err.println("Connection error: " + e.getMessage());
                        } finally {
                            lastActivity.set(System.currentTimeMillis());
                            try { conn.close(); } catch (Exception ignored) {}
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("Accept error: " + e.getMessage());
            }
        }
    }

    /**
     * Returns requestedPort unchanged, or derives a port from the JDK major
     * version when requestedPort == 0: major * 1000.
     * java.specification.version is "1.8" for JDK 8; bare number for JDK 9+.
     */
    static int resolvePort(int requestedPort) {
        if (requestedPort != 0) return requestedPort;
        String spec = System.getProperty("java.specification.version", "8");
        int major;
        if (spec.startsWith("1.")) {
            major = Integer.parseInt(spec.substring(2));
        } else {
            try {
                major = Integer.parseInt(spec.split("\\.")[0]);
            } catch (NumberFormatException e) {
                major = 8;
            }
        }
        int derived = major * 1000;
        System.err.println("Port 0 requested -- auto-derived " + derived +
            " from Java major version " + major);
        return derived;
    }

    /**
     * Determine PID via ProcessHandle (Java 9+, reflected to avoid a
     * compile-time dependency) or RuntimeMXBean name parsing (Java 8).
     */
    private static long detectPid() {
        try {
            Class<?> cls = Class.forName("java.lang.ProcessHandle");
            Object current = cls.getMethod("current").invoke(null);
            return (Long) cls.getMethod("pid").invoke(current);
        } catch (Exception ignored) {}
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            return Long.parseLong(name.split("@")[0]);
        } catch (Exception ignored) {}
        return -1;
    }

    static void handleConnection(JavaCompiler compiler, Socket conn) throws Exception {
        try (
            BufferedReader in  = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(
                new OutputStreamWriter(conn.getOutputStream(), "UTF-8"), true)
        ) {
            // Read javac args until ENDINP sentinel (or connection close)
            List<String> javacArgs = new ArrayList<String>();
            String line;
            while ((line = in.readLine()) != null
                    && !line.equals(SENTINEL_ENDINP)) {
                javacArgs.add(line);
            }

            if (javacArgs.isEmpty()) {
                sendFramedResponse(out, "", "", 0);
                return;
            }

            // Each connection gets its own FileManager -- required for thread safety
            ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
            ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
            PrintStream outStream = new PrintStream(outBuf, true, "UTF-8");
            PrintStream errStream = new PrintStream(errBuf, true, "UTF-8");

            int exitCode;
            try (StandardJavaFileManager fm =
                    compiler.getStandardFileManager(null, null, null)) {
                exitCode = compiler.run(
                    null,
                    outStream,
                    errStream,
                    javacArgs.toArray(new String[0])
                );
            }
            outStream.flush();
            errStream.flush();

            sendFramedResponse(
                out,
                outBuf.toString("UTF-8"),
                errBuf.toString("UTF-8"),
                exitCode
            );
        }
    }

    /**
     * Write a complete framed response to the client.
     * Each section is preceded by its NUL-wrapped sentinel tag.
     * An empty section emits one blank line to prevent client parser hangs.
     */
    private static void sendFramedResponse(PrintWriter out,
            String outStr, String errStr, int exitCode) {
        // stdout section
        out.println(SENTINEL_STDOUT);
        if (outStr.isEmpty()) {
            out.println(); // dummy empty line -- prevents hang when section is empty
        } else {
            out.print(outStr);
            if (!outStr.endsWith("\n")) out.println();
        }

        // stderr section
        out.println(SENTINEL_STDERR);
        if (errStr.isEmpty()) {
            out.println(); // dummy empty line -- prevents hang when section is empty
        } else {
            out.print(errStr);
            if (!errStr.endsWith("\n")) out.println();
        }

        // exit code section
        out.println(SENTINEL_EXTCOD);
        out.println(exitCode);
    }
}
