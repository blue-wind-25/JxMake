/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public final class ServerMode {
    private static final String LOCKFILE_NAME = "server.lock";
    private static final int DEFAULT_PORT = 17173;

    private ServerMode() {
    }

    private static Path lockfilePath() {
        return Paths.get(System.getProperty("user.home"), ".config/jxmake-code-formatter", LOCKFILE_NAME);
    }

    /**
     * Returns {@code true} if this call actually started a new server (its HTTP listener
     * threads are now live and the caller's process must stay alive -- not call
     * {@code System.exit} -- until {@code /shutdown} terminates it). Returns {@code false} if
     * there was nothing to keep alive: a server was already running, or startup failed; the
     * caller's process can exit immediately in either case.
     */
    public static boolean start(final Config config) {
        final Path lockfilePath = lockfilePath();

        if (Files.isRegularFile(lockfilePath)) {
            final long existingPid = readLockfilePid(lockfilePath);
            if (existingPid > 0 && isProcessAlive(existingPid)) {
                System.out.println("jxmake-code-formatter: server already running (pid " + existingPid + ")");
                return false;
            }
            try {
                Files.deleteIfExists(lockfilePath);
            } catch (final IOException e) {
                System.err.println("jxmake-code-formatter: warning: could not delete stale lockfile: " + e.getMessage());
            }
        }

        final int port = config.serverPort() > 0 ? config.serverPort() : DEFAULT_PORT;
        final HttpServer httpServer;
        try {
            httpServer = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        } catch (final IOException e) {
            System.err.println("jxmake-code-formatter: error: could not bind server: " + e.getMessage());
            return false;
        }

        try {
            Files.createDirectories(lockfilePath.getParent());
            final String lockContent = currentPid() + "\n" + port + "\n";
            Files.write(lockfilePath, lockContent.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            System.err.println("jxmake-code-formatter: error: could not write lockfile: " + e.getMessage());
            return false;
        }

        httpServer.createContext("/format", new FormatHandler());
        httpServer.createContext("/shutdown", new ShutdownHandler(httpServer, lockfilePath));
        httpServer.start();
        System.out.println("jxmake-code-formatter: server listening on port " + port);
        return true;
    }

    /**
     * Uses {@code java.lang.ProcessHandle} (Java 9+) via reflection when available, since the
     * project's Java 8 build target predates that class. Falls back to parsing the JVM name
     * reported by {@code RuntimeMXBean} ({@code "<pid>@<host>"}, available since Java 5) on
     * Java 8.
     */
    private static long currentPid() {
        try {
            final Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
            final Object current = processHandleClass.getMethod("current").invoke(null);
            return (Long) processHandleClass.getMethod("pid").invoke(current);
        } catch (final ReflectiveOperationException e) {
            final String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            final int at = jvmName.indexOf('@');
            return Long.parseLong(at > 0 ? jvmName.substring(0, at) : jvmName);
        }
    }

    /**
     * Uses {@code ProcessHandle.of(pid).isPresent()} (Java 9+) via reflection when available.
     * Falls back, on Java 8, to checking {@code /proc/<pid>} on Linux, or shelling out to
     * {@code kill -0 <pid>} on other Unix-likes; this fallback is not portable to Windows
     * (ties to RDD_KEY_18 -- best-effort only, same disposition as the forceful-shutdown path
     * documented in RDD_KEY_73).
     */
    private static boolean isProcessAlive(final long pid) {
        try {
            final Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
            final Object optional = processHandleClass.getMethod("of", long.class).invoke(null, pid);
            return (Boolean) optional.getClass().getMethod("isPresent").invoke(optional);
        } catch (final ReflectiveOperationException e) {
            final Path procDir = Paths.get("/proc");
            if (Files.isDirectory(procDir)) {
                return Files.isDirectory(procDir.resolve(String.valueOf(pid)));
            }
            try {
                final Process killProbe = new ProcessBuilder("kill", "-0", String.valueOf(pid))
                        .redirectErrorStream(true).start();
                return killProbe.waitFor() == 0;
            } catch (final IOException | InterruptedException probeFailure) {
                return false;
            }
        }
    }

    private static long readLockfilePid(final Path lockfilePath) {
        try {
            final List<String> lines = Files.readAllLines(lockfilePath);
            if (lines.isEmpty()) {
                return -1;
            }
            return Long.parseLong(lines.get(0).trim());
        } catch (final IOException | NumberFormatException e) {
            return -1;
        }
    }

    private static int readLockfilePort(final Path lockfilePath) {
        try {
            final List<String> lines = Files.readAllLines(lockfilePath);
            if (lines.size() < 2) {
                return -1;
            }
            return Integer.parseInt(lines.get(1).trim());
        } catch (final IOException | NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Returns the port of a live running server (lockfile present and its recorded PID alive),
     * or {@code -1} if no server is currently running. Read-only -- does not delete a stale
     * lockfile it finds; {@link #start} owns that cleanup, on an actual start attempt.
     */
    public static int findRunningServerPort() {
        final Path lockfilePath = lockfilePath();
        if (!Files.isRegularFile(lockfilePath)) {
            return -1;
        }
        final long pid = readLockfilePid(lockfilePath);
        if (pid <= 0 || !isProcessAlive(pid)) {
            return -1;
        }
        return readLockfilePort(lockfilePath);
    }

    /**
     * Implements RDD_KEY_73's stop protocol: read the lockfile for PID+port, POST
     * {@code /shutdown} with a short timeout, poll briefly for the lockfile to disappear, and
     * fall back to forceful termination if the server doesn't exit in time. Forceful
     * termination is best-effort (ties to RDD_KEY_18 / RDD_KEY_80) -- not guaranteed on all
     * platforms. Returns {@code true} if no server was running, or the running server was
     * confirmed stopped (gracefully or forcefully); {@code false} only if a live server was
     * found but could not be confirmed stopped.
     */
    public static boolean stop() {
        final Path lockfilePath = lockfilePath();
        if (!Files.isRegularFile(lockfilePath)) {
            System.out.println("jxmake-code-formatter: no server running");
            return true;
        }

        final long pid = readLockfilePid(lockfilePath);
        if (pid <= 0 || !isProcessAlive(pid)) {
            deleteLockfileQuietly(lockfilePath);
            System.out.println("jxmake-code-formatter: no server running (stale lockfile removed)");
            return true;
        }

        final int port = readLockfilePort(lockfilePath);
        if (port > 0) {
            try {
                final java.net.URL url = new java.net.URL("http://localhost:" + port + "/shutdown");
                final java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(500);
                connection.setReadTimeout(500);
                connection.getResponseCode();
                connection.disconnect();
            } catch (final IOException e) {
                // server may already be unresponsive -- fall through to polling/forceful kill
            }
        }

        for (int i = 0; i < 20; i++) {
            if (!Files.isRegularFile(lockfilePath)) {
                System.out.println("jxmake-code-formatter: server stopped");
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!isProcessAlive(pid)) {
            deleteLockfileQuietly(lockfilePath);
            System.out.println("jxmake-code-formatter: server stopped");
            return true;
        }

        System.err.println("jxmake-code-formatter: warning: server did not exit gracefully, forcing termination (pid " + pid
                + ")");
        final boolean killed = forceKill(pid);
        deleteLockfileQuietly(lockfilePath);
        if (killed) {
            System.out.println("jxmake-code-formatter: server stopped (forced)");
        } else {
            System.err.println("jxmake-code-formatter: error: could not stop server (pid " + pid + ") -- forceful "
                    + "termination is best-effort and not guaranteed on all platforms");
        }
        return killed;
    }

    private static void deleteLockfileQuietly(final Path lockfilePath) {
        try {
            Files.deleteIfExists(lockfilePath);
        } catch (final IOException e) {
            System.err.println("jxmake-code-formatter: warning: could not delete lockfile: " + e.getMessage());
        }
    }

    /**
     * Uses {@code ProcessHandle.destroyForcibly()} (Java 9+) via reflection when available.
     * Falls back, on Java 8, to shelling out to {@code kill -9 <pid>} -- same not-portable-to-
     * Windows disposition already documented on {@link #isProcessAlive}.
     */
    private static boolean forceKill(final long pid) {
        try {
            final Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
            final Object optional = processHandleClass.getMethod("of", long.class).invoke(null, pid);
            if (!(Boolean) optional.getClass().getMethod("isPresent").invoke(optional)) {
                return true;
            }
            final Object handle = optional.getClass().getMethod("get").invoke(optional);
            return (Boolean) handle.getClass().getMethod("destroyForcibly").invoke(handle);
        } catch (final ReflectiveOperationException e) {
            try {
                final Process killProcess = new ProcessBuilder("kill", "-9", String.valueOf(pid))
                        .redirectErrorStream(true).start();
                return killProcess.waitFor() == 0;
            } catch (final IOException | InterruptedException probeFailure) {
                return false;
            }
        }
    }

    private static final class FormatHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            try {
                final Map<String, String> params = parseQuery(exchange.getRequestURI());

                final Map<String, String> inlineConfig = new java.util.LinkedHashMap<String, String>();
                for (final Map.Entry<String, String> entry : params.entrySet()) {
                    final String key = entry.getKey();
                    if ("path".equals(key) || "lang".equals(key) || "format-off".equals(key)) {
                        continue;
                    }
                    if (!Config.isKnownKey(key)) {
                        respond(exchange, 400, "unrecognized query parameter: " + key);
                        return;
                    }
                    inlineConfig.put(key, entry.getValue());
                }

                final String path = params.get("path");
                String language = params.get("lang");

                if (path == null) {
                    if (language == null || inlineConfig.isEmpty()) {
                        respond(exchange, 400, "missing required query parameter 'path'");
                        return;
                    }
                } else if (language == null) {
                    language = Main.inferLanguage(path);
                    if (language == null) {
                        respond(exchange, 400, "could not infer language from path extension: " + path
                                + " (client should pass an explicit 'lang' query parameter)");
                        return;
                    }
                }

                if (language != null && !Main.isSupportedLanguage(language)) {
                    respond(exchange, 400, "'lang' query parameter must be one of: " + Main.SUPPORTED_LANGUAGES
                            + " (got: " + language + ")");
                    return;
                }

                final boolean formatOff = "true".equals(params.get("format-off"));
                final String content = readBody(exchange.getRequestBody());
                final Path targetFile = path == null ? null : Paths.get(path);
                final Config config = Config.resolve(targetFile, inlineConfig.isEmpty() ? null : inlineConfig);
                final String formatted = Formatter.formatOne(content, language, path == null ? "" : path, config, formatOff);
                respond(exchange, 200, formatted);
            } catch (final Exception e) {
                respond(exchange, 500, e.getMessage() != null ? e.getMessage() : e.toString());
            }
        }
    }

    private static final class ShutdownHandler implements HttpHandler {
        private final HttpServer httpServer;
        private final Path lockfilePath;

        ShutdownHandler(final HttpServer httpServer, final Path lockfilePath) {
            this.httpServer = httpServer;
            this.lockfilePath = lockfilePath;
        }

        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            respond(exchange, 200, "shutting down");
            final Thread shutdownThread = new Thread(() -> {
                httpServer.stop(1);
                try {
                    Files.deleteIfExists(lockfilePath);
                } catch (final IOException e) {
                    System.err.println("jxmake-code-formatter: warning: could not delete lockfile: " + e.getMessage());
                }
                System.exit(0);
            });
            shutdownThread.setDaemon(true);
            shutdownThread.start();
        }
    }

    private static Map<String, String> parseQuery(final URI uri) {
        final Map<String, String> result = new java.util.LinkedHashMap<String, String>();
        final String query = uri.getRawQuery();
        if (query == null || query.isEmpty()) {
            return result;
        }
        for (final String pair : query.split("&")) {
            final int eq = pair.indexOf('=');
            if (eq < 0) {
                continue;
            }
            final String key = urlDecode(pair.substring(0, eq));
            final String value = urlDecode(pair.substring(eq + 1));
            result.put(key, value);
        }
        return result;
    }

    private static String urlDecode(final String value) {
        try {
            return java.net.URLDecoder.decode(value, "UTF-8");
        } catch (final java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String readBody(final InputStream in) throws IOException {
        final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        final byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void respond(final HttpExchange exchange, final int statusCode, final String body)
            throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (final OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
