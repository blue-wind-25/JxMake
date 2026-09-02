/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake program, see LICENSE file for the license details
 */


import java.io.*;
import java.lang.management.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.tools.*;


/*
 * Persistent Java compilation daemon.
 *
 * Protocol (per connection, newline-delimited):
 *   Client sends:  arg1\narg2\n...\n\u001FENDINP\u001F\n
 *   Server replies:
 *     \u001FSTDOUT\u001F\n
 *     stdout lines - or nothing when stdout is empty
 *     \u001FSTDERR\u001F\n
 *     stderr lines - or nothing when stderr is empty
 *     \u001FEXTCOD\u001F\n
 *     exit-code\n
 *
 * Sentinel lines contain actual Unit Separator bytes (U+001F, ASCII 31)
 * before and after the tag word, making them unambiguous framing tokens
 * that never appear in normal javac output.
 *
 * If port argument is 0, the listening port is derived from the Java
 * major version: major * 1000
 * (JDK 8->8000, JDK 11->11000, JDK 17->17000, JDK 21->21000).
 *
 * Self-terminates after IDLE_TIMEOUT_MS of no compilation activity.
 */
public class CompileServer {

    static final long IDLE_TIMEOUT_MS = 3L* 60* 60* 1000; // 3 hours

    // US-wrapped sentinel tags.
    // \u001F is the Java Unicode escape for U+001F (the Unit Separator character).
    // These escapes are resolved before tokenisation, so at runtime each
    // constant begins and ends with an actual US byte (0x1F).
    static final String SENTINEL_ENDINP = "\u001FENDINP\u001F";
    static final String SENTINEL_STDOUT = "\u001FSTDOUT\u001F";
    static final String SENTINEL_STDERR = "\u001FSTDERR\u001F";
    static final String SENTINEL_EXTCOD = "\u001FEXTCOD\u001F";

    public static void main(final String[] args) throws Exception
    {
        if(args.length < 1) {
            System.err.println("Usage: java -jar compile-server.jar <port>");
            System.exit(1);
        }

        final int port;
        try {
            port = resolvePort( Integer.parseInt( args[0] ) );
        }
        catch(final NumberFormatException e) {
            System.err.println( "ERROR: Invalid port '" + args[0] + "'. Expected an integer." );
            System.exit(1);
            return; // Unreachable - satisfies definite assignment
        }

        // Write our own log file directly rather than relying on the parent
        // process piping our stdout/stderr: this daemon outlives whatever
        // process spawned it, and on Windows, a child process started with
        // any std handle redirected inherits *every* inheritable handle its
        // parent holds (not just the ones explicitly passed) - including,
        // several process hops up, the pipe a CI runner uses to capture a
        // build step's own output. A long-lived daemon holding that pipe's
        // write end open means the runner's reader never sees EOF and hangs
        // forever, even after every real command has finished. Writing our
        // own log file sidesteps the need for that redirection entirely.
        final String      logFile   = System.getProperty(
            "java.io.tmpdir"
        ) + "/javac-daemon-" + port + ".log";
        final PrintStream logStream = new PrintStream(
            new FileOutputStream(logFile, true), true, "UTF-8"
        );
        System.setOut(logStream);
        System.setErr(logStream);

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        if(compiler == null) {
            System.err.println(
                "ERROR: No JavaCompiler available. Make sure you are running a JDK, not a JRE."
            );
            System.exit(2);
        }

        final String pidFile = System.getProperty(
            "java.io.tmpdir"
        ) + "/javac-daemon-" + port + ".pid";
        final long   pid     = detectPid();

        try(
            final PrintWriter pw = new PrintWriter(pidFile)
        ) {
            pw.println(pid);
        }
        catch(final Exception e) {
            System.err.println( "Warning: could not write PID file: " + e.getMessage() );
        }
        if(pid == -1) System.err.println(
            "Warning: could not determine PID, wrote -1 to " + pidFile
        );
        else System.err.println("PID " + pid + " written to " + pidFile);

        try(
            final ServerSocket server = new ServerSocket()
        ) {
            server.setReuseAddress(true);
            server.bind( new InetSocketAddress(port) );

            System.err.println("javac daemon ready");
            System.err.println( "  JDK : " + System.getProperty("java.home") );
            System.err.println("  Port: " + port);
            System.err.println( "  Idle timeout: " + (IDLE_TIMEOUT_MS / 3_600_000L) + " hours" );

            // Named ThreadFactory so worker threads appear as "javac-worker-N" in thread dumps
            final AtomicInteger   workerCount   = new AtomicInteger(0);
            final ThreadFactory   workerFactory = r->new Thread(
                r, "javac-worker-" + workerCount.incrementAndGet()
            );
            final ExecutorService pool          = Executors.newCachedThreadPool(workerFactory);
            final AtomicLong      lastActivity  = new AtomicLong( System.currentTimeMillis() );

            // Idle watchdog - shuts down after IDLE_TIMEOUT_MS with no activity
            final ScheduledExecutorService watchdogExec = Executors.newSingleThreadScheduledExecutor(
                r -> {
                    final Thread t = new Thread(r, "javac-watchdog");
                    t.setDaemon(true);
                    return t;
                }
            );
            watchdogExec.scheduleAtFixedRate(
                () -> {
                    final long idle = System.currentTimeMillis() - lastActivity.get();
                    if(idle >= IDLE_TIMEOUT_MS) {
                        System.err.println("Idle timeout reached, shutting down.");
                        System.exit(0);
                    }
                },
                60, 60, TimeUnit.SECONDS
            );

            Runtime.getRuntime().addShutdownHook(
                new Thread( () -> new File(pidFile).delete(), "javac-shutdown-hook" )
            );

            while(true) {

                try {
                    final Socket conn = server.accept();
                    lastActivity.set( System.currentTimeMillis() );
                    pool.submit( () -> {
                        try {
                            handleConnection(compiler, conn);
                        }
                        catch(final Exception e) {
                            System.err.println( "Connection error: " + e.getMessage() );
                            e.printStackTrace(System.err);
                        }
                        finally {
                            lastActivity.set( System.currentTimeMillis() );
                            try {
                                conn.close();
                            }
                            catch(final Exception ignored) {}
                        }
                    } );
                }
                catch(final Exception e) {
                    System.err.println( "Accept error: " + e.getMessage() );
                    e.printStackTrace(System.err);
                }

            } // while
        }
    }

    /*
     * Returns requestedPort unchanged, or derives a port from the JDK major
     * version when requestedPort == 0: major * 1000.
     * java.specification.version is "1.8" for JDK 8; bare number for JDK 9+.
     */
    static int resolvePort(final int requestedPort)
    {
        if(requestedPort != 0) return requestedPort;

        final String spec = System.getProperty("java.specification.version", "8");
              int    major;

        if( spec.startsWith("1.") ) {
            major = Integer.parseInt( spec.substring(2) );
        }
        else {
            try {
                major = Integer.parseInt( spec.split("\\.")[0] );
            }
            catch(final NumberFormatException e) {
                major = 8;
            }
        }

        final int derived = major * 1000;
        System.err.println(
            "Port 0 requested - auto-derived " + derived + " from Java major version " + major
        );

        return derived;
    }

    /*
     * Determine PID via ProcessHandle (Java 9+, reflected to avoid a
     * compile-time dependency) or RuntimeMXBean name parsing (Java 8)
     */
    private static long detectPid()
    {
        try {
            final Class<?> cls     = Class.forName("java.lang.ProcessHandle");
            final Object   current = cls.getMethod("current").invoke(null);
            return (Long) cls.getMethod("pid").invoke(current);
        }
        catch(final Exception ignored) {}

        try {
            final String name = ManagementFactory.getRuntimeMXBean().getName();
            return Long.parseLong( name.split("@")[0] );
        }
        catch(final Exception ignored) {}

        return -1;
    }

    static void handleConnection(final JavaCompiler compiler, final Socket conn) throws Exception
    {
        try (
            final BufferedReader in  = new BufferedReader(
                new InputStreamReader( conn.getInputStream(), StandardCharsets.UTF_8 )
            );
            final PrintWriter    out = new PrintWriter(
                new OutputStreamWriter( conn.getOutputStream(), StandardCharsets.UTF_8 ), true
            );
        ) {
            // Read javac args until ENDINP sentinel (or connection close)
            final List<String> javacArgs = new ArrayList<>();
                  String       line;
            while( ( line = in.readLine() ) != null && !line.equals(
                SENTINEL_ENDINP
            ) ) javacArgs.add(
                line
            );

            if( javacArgs.isEmpty() ) {
                sendFramedResponse(out, "", "", 0);
                return;
            }

            // Each connection gets its own FileManager - required for thread safety
            final ByteArrayOutputStream outBuf    = new ByteArrayOutputStream();
            final ByteArrayOutputStream errBuf    = new ByteArrayOutputStream();
            final PrintStream           outStream = new PrintStream(outBuf, true, "UTF-8");
            final PrintStream           errStream = new PrintStream(errBuf, true, "UTF-8");
                  int                   exitCode;

            // Catch anything compiler.run() itself throws (as opposed to
            // reporting via diagnostics on errStream) and still send a
            // complete framed response: letting an exception escape here
            // would close the connection before EXTCOD is ever sent, and
            // the client has no way to tell that apart from a genuine
            // compiler exit code, other than logging an ambiguous
            // "connection closed early" message
            try(
                final StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)
            ) {
                exitCode = compiler.run(
                    null, outStream, errStream, javacArgs.toArray( new String[0] )
                );
            }
            catch(final Throwable t) {
                t.printStackTrace(errStream);
                exitCode = 1;
            }

            sendFramedResponse( out, outBuf.toString("UTF-8"), errBuf.toString("UTF-8"), exitCode );
        }
    }

    /*
     * Write a complete framed response to the client.
     * Each section is preceded by its US-wrapped sentinel tag.
     * An empty section emits no content lines.
     */
    private static void sendFramedResponse(
        final PrintWriter out,
        final String      outStr,
        final String      errStr,
        final int         exitCode
    )
    {
        out.println(SENTINEL_STDOUT);
        if( !outStr.isEmpty() ) {
            out.print(outStr);
            if( !outStr.endsWith("\n") ) out.println();
        }

        out.println(SENTINEL_STDERR);
        if( !errStr.isEmpty() ) {
            out.print(errStr);
            if( !errStr.endsWith("\n") ) out.println();
        }

        out.println(SENTINEL_EXTCOD);
        out.println(exitCode);
    }

} // class CompileServer
