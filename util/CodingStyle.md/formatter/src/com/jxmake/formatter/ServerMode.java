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

    public static void start(final Config config) {
        final Path lockfilePath = Paths.get(System.getProperty("user.home"), ".config/style-fmt", LOCKFILE_NAME);

        if (Files.isRegularFile(lockfilePath)) {
            final long existingPid = readLockfilePid(lockfilePath);
            if (existingPid > 0 && isProcessAlive(existingPid)) {
                System.out.println("style-fmt: server already running (pid " + existingPid + ")");
                return;
            }
            try {
                Files.deleteIfExists(lockfilePath);
            } catch (final IOException e) {
                System.err.println("style-fmt: warning: could not delete stale lockfile: " + e.getMessage());
            }
        }

        final int port = config.serverPort() > 0 ? config.serverPort() : DEFAULT_PORT;
        final HttpServer httpServer;
        try {
            httpServer = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        } catch (final IOException e) {
            System.err.println("style-fmt: error: could not bind server: " + e.getMessage());
            return;
        }

        try {
            Files.createDirectories(lockfilePath.getParent());
            final String lockContent = currentPid() + "\n" + port + "\n";
            Files.write(lockfilePath, lockContent.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            System.err.println("style-fmt: error: could not write lockfile: " + e.getMessage());
            return;
        }

        httpServer.createContext("/format", new FormatHandler());
        httpServer.createContext("/shutdown", new ShutdownHandler(httpServer, lockfilePath));
        httpServer.start();
        System.out.println("style-fmt: server listening on port " + port);
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

    private static final class FormatHandler implements HttpHandler {
        @Override
        public void handle(final HttpExchange exchange) throws IOException {
            try {
                final Map<String, String> params = parseQuery(exchange.getRequestURI());
                final String path = params.get("path");
                if (path == null) {
                    respond(exchange, 400, "missing required query parameter 'path'");
                    return;
                }

                String language = params.get("lang");
                if (language == null) {
                    language = inferLanguage(path);
                    if (language == null) {
                        respond(exchange, 400, "could not infer language from path extension: " + path);
                        return;
                    }
                }

                final String content = readBody(exchange.getRequestBody());
                final Config config = Config.resolve(Paths.get(path), null);
                final String formatted = Formatter.formatOne(content, language, path, config);
                respond(exchange, 200, formatted);
            } catch (final Exception e) {
                respond(exchange, 500, e.getMessage() != null ? e.getMessage() : e.toString());
            }
        }

        private static String inferLanguage(final String path) {
            final String lower = path.toLowerCase(java.util.Locale.ROOT);
            if (lower.endsWith(".java")) {
                return "java";
            }
            if (lower.endsWith(".c") || lower.endsWith(".cc") || lower.endsWith(".cpp") || lower.endsWith(".cxx")
                    || lower.endsWith(".h") || lower.endsWith(".hh") || lower.endsWith(".hpp")
                    || lower.endsWith(".hxx")) {
                return "cpp";
            }
            return null;
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
                    System.err.println("style-fmt: warning: could not delete lockfile: " + e.getMessage());
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
