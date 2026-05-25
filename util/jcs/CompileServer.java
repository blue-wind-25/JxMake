import javax.tools.*;
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
        try {
            long pid = ProcessHandle.current().pid();
            try (PrintWriter pw = new PrintWriter(pidFile)) {
                pw.println(pid);
            }
            System.err.println("PID " + pid + " written to " + pidFile);
        } catch (Exception e) {
            System.err.println("Warning: could not write PID file: " + e.getMessage());
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
        Thread watchdog = new Thread(() -> {
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
        });
        watchdog.setDaemon(true);
        watchdog.start();

        // Register shutdown hook to clean up PID file on any exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            new File(pidFile).delete();
        }));

        while (true) {
            try {
                Socket conn = server.accept();
                lastActivity.set(System.currentTimeMillis());
                pool.submit(() -> {
                    try {
                        handleConnection(compiler, conn);
                    } catch (Exception e) {
                        System.err.println("Connection error: " + e.getMessage());
                    } finally {
                        lastActivity.set(System.currentTimeMillis());
                    }
                });
            } catch (Exception e) {
                System.err.println("Accept error: " + e.getMessage());
            }
        }
    }

    static void handleConnection(JavaCompiler compiler, Socket conn) throws Exception {
        try (
            BufferedReader in  = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            PrintWriter out = new PrintWriter(
                new OutputStreamWriter(conn.getOutputStream()), true)
        ) {
            // Read javac arguments until END sentinel
            List<String> javacArgs = new ArrayList<>();
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
            PrintStream outStream = new PrintStream(outBuf);
            PrintStream errStream = new PrintStream(errBuf);

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

            String outStr = outBuf.toString();
            String errStr = errBuf.toString();

            if (!outStr.isEmpty()) out.print(outStr);
            if (!errStr.isEmpty()) out.print(errStr);

            out.println("EXIT:" + exitCode);
        }
    }
}
