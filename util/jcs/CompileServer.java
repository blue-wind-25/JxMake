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
 *   Client sends:  <arg1>\n<arg2>\n...<argN>\nEND\n
 *   Server replies: <compiler stderr/stdout output lines>\nEXIT:<code>\n
 *
 * Self-terminates after IDLE_TIMEOUT_MS of no compilation activity.
 */
public class CompileServer {

    static final long IDLE_TIMEOUT_MS = 3L * 60 * 60 * 1000; // 3 hours

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java -jar compile-server.jar <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("ERROR: No JavaCompiler available. " +
                "Make sure you are running a JDK, not a JRE.");
            System.exit(2);
        }

        // Write PID file so kill script can find us
        String pidFile = System.getProperty("java.io.tmpdir") +
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

        // Idle watchdog thread — shuts down if no compilation for IDLE_TIMEOUT_MS
        Thread watchdog = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(60_000); // check every minute
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

        // Register shutdown hook to clean up PID file on any exit
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
     * Determine PID using ProcessHandle (Java 9+) via reflection,
     * falling back to RuntimeMXBean (Java 8). Returns -1 if unavailable.
     */
    private static long detectPid() {
        // Java 9+: ProcessHandle via reflection to avoid compile-time dependency
        try {
            Class<?> cls = Class.forName("java.lang.ProcessHandle");
            Object current = cls.getMethod("current").invoke(null);
            return (Long) cls.getMethod("pid").invoke(current);
        } catch (Exception ignored) {}
        // Java 8 fallback: RuntimeMXBean name is "pid@hostname"
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
            // Read javac arguments until END sentinel
            List<String> javacArgs = new ArrayList<String>();
            String line;
            while ((line = in.readLine()) != null && !line.equals("END")) {
                javacArgs.add(line);
            }

            if (javacArgs.isEmpty()) {
                out.println("EXIT:0");
                return;
            }

            // Each connection gets its own FileManager — required for thread safety
            ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
            ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
            PrintStream outStream = new PrintStream(outBuf, true, "UTF-8");
            PrintStream errStream = new PrintStream(errBuf, true, "UTF-8");

            int exitCode;
            try (StandardJavaFileManager fm =
                    compiler.getStandardFileManager(null, null, null)) {

                exitCode = compiler.run(
                    null,       // stdin  (unused)
                    outStream,  // stdout
                    errStream,  // stderr
                    javacArgs.toArray(new String[0])
                );
            }

            // Flush and send output
            outStream.flush();
            errStream.flush();

            String outStr = outBuf.toString("UTF-8");
            String errStr = errBuf.toString("UTF-8");

            if (!outStr.isEmpty()) out.print(outStr);
            if (!errStr.isEmpty()) out.print(errStr);

            out.println("EXIT:" + exitCode);
        }
    }
}
